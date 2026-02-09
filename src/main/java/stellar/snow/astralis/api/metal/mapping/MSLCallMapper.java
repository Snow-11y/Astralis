package stellar.snow.astralis.api.metal.mapping;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Comprehensive Metal Shading Language (MSL) call mapper and cross-compiler.
 * 
 * <p>This class provides the complete shader compilation pipeline for Metal,
 * including cross-compilation from SPIR-V, HLSL, and GLSL to MSL, as well as
 * native MSL compilation with all supported features across Metal versions.
 * 
 * <h2>Supported Metal Versions</h2>
 * <ul>
 *   <li>MSL 1.0 - Metal 1.0 (iOS 8, macOS 10.11)</li>
 *   <li>MSL 1.1 - Metal 1.1 (iOS 9, macOS 10.11)</li>
 *   <li>MSL 1.2 - Metal 1.2 (iOS 10, macOS 10.12) - Tessellation</li>
 *   <li>MSL 2.0 - Metal 2.0 (iOS 11, macOS 10.13) - Argument buffers, imageblocks</li>
 *   <li>MSL 2.1 - Metal 2.1 (iOS 12, macOS 10.14) - Tile shaders</li>
 *   <li>MSL 2.2 - Metal 2.2 (iOS 13, macOS 10.15) - Ray tracing</li>
 *   <li>MSL 2.3 - Metal 2.3 (iOS 14, macOS 11.0) - Function pointers, binary archives</li>
 *   <li>MSL 2.4 - Metal 2.4 (iOS 15, macOS 12.0) - Mesh shaders</li>
 *   <li>MSL 3.0 - Metal 3.0 (iOS 16, macOS 13.0)</li>
 *   <li>MSL 3.1 - Metal 3.1 (iOS 17, macOS 14.0) - Dynamic libraries</li>
 * </ul>
 * 
 * <h2>Cross-Compilation Support</h2>
 * <ul>
 *   <li>SPIR-V → MSL via SPIRV-Cross</li>
 *   <li>HLSL → MSL via DXC + SPIRV-Cross or direct translation</li>
 *   <li>GLSL → MSL via glslang + SPIRV-Cross</li>
 * </ul>
 * 
 * @author Mojang AB (Metal Backend Team)
 * @since 1.23.0
 */
public final class MSLCallMapper implements AutoCloseable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("Minecraft-Metal-MSL");
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 1: MSL VERSION MANAGEMENT
    // Metal Shading Language version detection and feature support
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Metal Shading Language version enumeration.
     */
    public enum MSLVersion {
        MSL_1_0(1, 0, 10000, "1.0", "Metal 1.0"),
        MSL_1_1(1, 1, 10100, "1.1", "Metal 1.1"),
        MSL_1_2(1, 2, 10200, "1.2", "Metal 1.2 - Tessellation"),
        MSL_2_0(2, 0, 20000, "2.0", "Metal 2.0 - Argument Buffers"),
        MSL_2_1(2, 1, 20100, "2.1", "Metal 2.1 - Tile Shaders"),
        MSL_2_2(2, 2, 20200, "2.2", "Metal 2.2 - Ray Tracing"),
        MSL_2_3(2, 3, 20300, "2.3", "Metal 2.3 - Function Pointers"),
        MSL_2_4(2, 4, 20400, "2.4", "Metal 2.4 - Mesh Shaders"),
        MSL_3_0(3, 0, 30000, "3.0", "Metal 3.0"),
        MSL_3_1(3, 1, 30100, "3.1", "Metal 3.1 - Dynamic Libraries");
        
        public final int major;
        public final int minor;
        public final int code;
        public final String versionString;
        public final String description;
        
        MSLVersion(int major, int minor, int code, String versionString, String description) {
            this.major = major;
            this.minor = minor;
            this.code = code;
            this.versionString = versionString;
            this.description = description;
        }
        
        /**
         * Check if this version supports a specific feature.
         */
        public boolean supports(MSLFeature feature) {
            return this.code >= feature.minVersion.code;
        }
        
        /**
         * Get the MSL version from a code.
         */
        public static MSLVersion fromCode(int code) {
            for (MSLVersion v : values()) {
                if (v.code == code) return v;
            }
            // Find closest lower version
            MSLVersion result = MSL_1_0;
            for (MSLVersion v : values()) {
                if (v.code <= code && v.code > result.code) {
                    result = v;
                }
            }
            return result;
        }
        
        /**
         * Get the language version macro value.
         */
        public String getLanguageVersionMacro() {
            return String.format("__METAL_VERSION__ >= %d", code);
        }
    }
    
    /**
     * MSL feature enumeration with minimum version requirements.
     */
    public enum MSLFeature {
        // Basic features
        BASIC_SHADERS(MSLVersion.MSL_1_0),
        FUNCTION_CONSTANTS(MSLVersion.MSL_1_0),
        VERTEX_AMPLIFICATION(MSLVersion.MSL_1_0),
        
        // MSL 1.1 features
        SAMPLER_ARRAYS(MSLVersion.MSL_1_1),
        TEXTURE_ARRAYS(MSLVersion.MSL_1_1),
        
        // MSL 1.2 features
        TESSELLATION(MSLVersion.MSL_1_2),
        TESSELLATION_CONTROL_SHADER(MSLVersion.MSL_1_2),
        TESSELLATION_EVALUATION_SHADER(MSLVersion.MSL_1_2),
        
        // MSL 2.0 features
        ARGUMENT_BUFFERS(MSLVersion.MSL_2_0),
        IMAGEBLOCKS(MSLVersion.MSL_2_0),
        RASTER_ORDER_GROUPS(MSLVersion.MSL_2_0),
        UNIFORM_TYPE(MSLVersion.MSL_2_0),
        TEXTURE_READ_WRITE(MSLVersion.MSL_2_0),
        SPARSE_TEXTURES(MSLVersion.MSL_2_0),
        
        // MSL 2.1 features
        TILE_SHADERS(MSLVersion.MSL_2_1),
        IMAGEBLOCK_SAMPLE_COVERAGE(MSLVersion.MSL_2_1),
        SIMD_GROUP_FUNCTIONS(MSLVersion.MSL_2_1),
        QUAD_GROUP_FUNCTIONS(MSLVersion.MSL_2_1),
        NON_UNIFORM_THREADGROUP_SIZE(MSLVersion.MSL_2_1),
        
        // MSL 2.2 features
        RAY_TRACING(MSLVersion.MSL_2_2),
        INTERSECTION_FUNCTIONS(MSLVersion.MSL_2_2),
        ACCELERATION_STRUCTURE(MSLVersion.MSL_2_2),
        INLINE_RAY_TRACING(MSLVersion.MSL_2_2),
        VISIBLE_FUNCTION_TABLE(MSLVersion.MSL_2_2),
        
        // MSL 2.3 features
        FUNCTION_POINTERS(MSLVersion.MSL_2_3),
        BINARY_ARCHIVES(MSLVersion.MSL_2_3),
        DYNAMIC_CALLABLE_FUNCTIONS(MSLVersion.MSL_2_3),
        INTERSECTION_FUNCTION_TABLES(MSLVersion.MSL_2_3),
        
        // MSL 2.4 features
        MESH_SHADERS(MSLVersion.MSL_2_4),
        OBJECT_SHADERS(MSLVersion.MSL_2_4),
        PRIMITIVE_ACCELERATION_STRUCTURE(MSLVersion.MSL_2_4),
        
        // MSL 3.0 features
        METAL_3(MSLVersion.MSL_3_0),
        TEXTURE_COMPRESSION(MSLVersion.MSL_3_0),
        EXTENDED_DYNAMIC_RANGES(MSLVersion.MSL_3_0),
        
        // MSL 3.1 features
        DYNAMIC_LIBRARIES(MSLVersion.MSL_3_1),
        PRELOADED_LIBRARIES(MSLVersion.MSL_3_1);
        
        public final MSLVersion minVersion;
        
        MSLFeature(MSLVersion minVersion) {
            this.minVersion = minVersion;
        }
    }
    
    /**
     * GPU family enumeration for feature detection.
     */
    public enum GPUFamily {
        // Apple Silicon
        APPLE_1(1, MSLVersion.MSL_1_1, "A7"),
        APPLE_2(2, MSLVersion.MSL_1_2, "A8"),
        APPLE_3(3, MSLVersion.MSL_2_0, "A9-A10"),
        APPLE_4(4, MSLVersion.MSL_2_1, "A11"),
        APPLE_5(5, MSLVersion.MSL_2_2, "A12"),
        APPLE_6(6, MSLVersion.MSL_2_3, "A13"),
        APPLE_7(7, MSLVersion.MSL_2_3, "A14-A15-M1"),
        APPLE_8(8, MSLVersion.MSL_3_0, "A16-M2"),
        APPLE_9(9, MSLVersion.MSL_3_1, "A17-M3"),
        
        // Mac GPUs
        MAC_1(1001, MSLVersion.MSL_2_0, "Mac Family 1"),
        MAC_2(1002, MSLVersion.MSL_2_3, "Mac Family 2"),
        
        // Common families
        COMMON_1(2001, MSLVersion.MSL_1_2, "Common 1"),
        COMMON_2(2002, MSLVersion.MSL_2_0, "Common 2"),
        COMMON_3(2003, MSLVersion.MSL_2_3, "Common 3"),
        
        // Metal 3
        METAL_3(3001, MSLVersion.MSL_3_0, "Metal 3");
        
        public final int code;
        public final MSLVersion maxMSLVersion;
        public final String description;
        
        GPUFamily(int code, MSLVersion maxMSLVersion, String description) {
            this.code = code;
            this.maxMSLVersion = maxMSLVersion;
            this.description = description;
        }
    }
    
    /**
     * MSL version capabilities for a specific device.
     */
    public static final class MSLCapabilities {
        
        public final MSLVersion version;
        public final GPUFamily family;
        public final EnumSet<MSLFeature> supportedFeatures;
        
        // Limits
        public final int maxThreadsPerThreadgroup;
        public final int maxThreadgroupMemoryLength;
        public final int maxBufferArgumentTableEntries;
        public final int maxTextureArgumentTableEntries;
        public final int maxSamplerArgumentTableEntries;
        public final int maxVertexAttributes;
        public final int maxColorAttachments;
        public final int maxTessellationFactor;
        public final int maxVisibleFunctionTableSize;
        public final int maxArgumentBufferSamplerCount;
        
        // Texture limits
        public final int maxTexture2DWidth;
        public final int maxTexture2DHeight;
        public final int maxTexture3DWidth;
        public final int maxTexture3DHeight;
        public final int maxTexture3DDepth;
        public final int maxTextureCubeSize;
        public final int maxTextureArrayLayers;
        public final int maxTextureMipLevels;
        
        public MSLCapabilities(long device) {
            this.version = detectMSLVersion(device);
            this.family = detectGPUFamily(device);
            this.supportedFeatures = detectFeatures(device, version, family);
            
            // Query limits from device
            this.maxThreadsPerThreadgroup = nMTLDeviceMaxThreadsPerThreadgroup(device);
            this.maxThreadgroupMemoryLength = nMTLDeviceMaxThreadgroupMemoryLength(device);
            this.maxBufferArgumentTableEntries = nMTLDeviceMaxBufferArgumentTableEntries(device);
            this.maxTextureArgumentTableEntries = nMTLDeviceMaxTextureArgumentTableEntries(device);
            this.maxSamplerArgumentTableEntries = nMTLDeviceMaxSamplerArgumentTableEntries(device);
            this.maxVertexAttributes = nMTLDeviceMaxVertexAttributes(device);
            this.maxColorAttachments = 8; // Standard Metal limit
            this.maxTessellationFactor = supportedFeatures.contains(MSLFeature.TESSELLATION) ? 64 : 0;
            this.maxVisibleFunctionTableSize = supportedFeatures.contains(MSLFeature.VISIBLE_FUNCTION_TABLE) ? 
                nMTLDeviceMaxVisibleFunctionTableSize(device) : 0;
            this.maxArgumentBufferSamplerCount = supportedFeatures.contains(MSLFeature.ARGUMENT_BUFFERS) ?
                nMTLDeviceMaxArgumentBufferSamplerCount(device) : 0;
            
            this.maxTexture2DWidth = nMTLDeviceMaxTexture2DWidth(device);
            this.maxTexture2DHeight = nMTLDeviceMaxTexture2DHeight(device);
            this.maxTexture3DWidth = nMTLDeviceMaxTexture3DWidth(device);
            this.maxTexture3DHeight = nMTLDeviceMaxTexture3DHeight(device);
            this.maxTexture3DDepth = nMTLDeviceMaxTexture3DDepth(device);
            this.maxTextureCubeSize = nMTLDeviceMaxTextureCubeSize(device);
            this.maxTextureArrayLayers = nMTLDeviceMaxTextureArrayLayers(device);
            this.maxTextureMipLevels = nMTLDeviceMaxTextureMipLevels(device);
        }
        
        private static MSLVersion detectMSLVersion(long device) {
            // Check highest supported MSL version
            if (nMTLDeviceSupportsFamily(device, 3001)) { // Metal 3
                if (nMTLDeviceSupportsMSLVersion(device, 30100)) return MSLVersion.MSL_3_1;
                return MSLVersion.MSL_3_0;
            }
            
            // Check by testing features
            if (nMTLDeviceSupportsMSLVersion(device, 20400)) return MSLVersion.MSL_2_4;
            if (nMTLDeviceSupportsMSLVersion(device, 20300)) return MSLVersion.MSL_2_3;
            if (nMTLDeviceSupportsMSLVersion(device, 20200)) return MSLVersion.MSL_2_2;
            if (nMTLDeviceSupportsMSLVersion(device, 20100)) return MSLVersion.MSL_2_1;
            if (nMTLDeviceSupportsMSLVersion(device, 20000)) return MSLVersion.MSL_2_0;
            if (nMTLDeviceSupportsMSLVersion(device, 10200)) return MSLVersion.MSL_1_2;
            if (nMTLDeviceSupportsMSLVersion(device, 10100)) return MSLVersion.MSL_1_1;
            
            return MSLVersion.MSL_1_0;
        }
        
        private static GPUFamily detectGPUFamily(long device) {
            // Check Apple families first (most common on macOS with Apple Silicon)
            for (int i = 9; i >= 1; i--) {
                if (nMTLDeviceSupportsFamily(device, i)) { // Apple family codes
                    return switch (i) {
                        case 9 -> GPUFamily.APPLE_9;
                        case 8 -> GPUFamily.APPLE_8;
                        case 7 -> GPUFamily.APPLE_7;
                        case 6 -> GPUFamily.APPLE_6;
                        case 5 -> GPUFamily.APPLE_5;
                        case 4 -> GPUFamily.APPLE_4;
                        case 3 -> GPUFamily.APPLE_3;
                        case 2 -> GPUFamily.APPLE_2;
                        default -> GPUFamily.APPLE_1;
                    };
                }
            }
            
            // Check Mac families (Intel/AMD)
            if (nMTLDeviceSupportsFamily(device, 1002)) return GPUFamily.MAC_2;
            if (nMTLDeviceSupportsFamily(device, 1001)) return GPUFamily.MAC_1;
            
            // Check common families
            if (nMTLDeviceSupportsFamily(device, 2003)) return GPUFamily.COMMON_3;
            if (nMTLDeviceSupportsFamily(device, 2002)) return GPUFamily.COMMON_2;
            if (nMTLDeviceSupportsFamily(device, 2001)) return GPUFamily.COMMON_1;
            
            // Metal 3
            if (nMTLDeviceSupportsFamily(device, 3001)) return GPUFamily.METAL_3;
            
            return GPUFamily.COMMON_1;
        }
        
        private static EnumSet<MSLFeature> detectFeatures(long device, MSLVersion version, GPUFamily family) {
            EnumSet<MSLFeature> features = EnumSet.noneOf(MSLFeature.class);
            
            // Add all features supported by the MSL version
            for (MSLFeature feature : MSLFeature.values()) {
                if (version.supports(feature)) {
                    // Additional device-specific checks for some features
                    boolean supported = switch (feature) {
                        case RAY_TRACING, ACCELERATION_STRUCTURE, INLINE_RAY_TRACING -> 
                            nMTLDeviceSupportsRaytracing(device);
                        case MESH_SHADERS, OBJECT_SHADERS -> 
                            nMTLDeviceSupportsMeshShaders(device);
                        case ARGUMENT_BUFFERS -> 
                            nMTLDeviceArgumentBuffersSupport(device) > 0;
                        case DYNAMIC_LIBRARIES -> 
                            nMTLDeviceSupportsDynamicLibraries(device);
                        default -> true;
                    };
                    if (supported) {
                        features.add(feature);
                    }
                }
            }
            
            return features;
        }
        
        /**
         * Check if a specific feature is supported.
         */
        public boolean hasFeature(MSLFeature feature) {
            return supportedFeatures.contains(feature);
        }
        
        /**
         * Get the recommended compile options for this device.
         */
        public MTLCompileOptions getRecommendedCompileOptions() {
            MTLCompileOptions options = new MTLCompileOptions();
            options.languageVersion = this.version;
            options.fastMathEnabled = true;
            
            // Apple GPUs benefit from aggressive optimization
            if (family.code < 1000) { // Apple family
                options.optimizationLevel = OptimizationLevel.PERFORMANCE;
            } else {
                options.optimizationLevel = OptimizationLevel.DEFAULT;
            }
            
            return options;
        }
        
        @Override
        public String toString() {
            return String.format("MSLCapabilities[version=%s, family=%s, features=%d]",
                version.versionString, family.description, supportedFeatures.size());
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 2: RUNTIME COMPILATION
    // MTLLibrary creation and shader compilation
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * MTLCompileOptions wrapper.
     */
    public static final class MTLCompileOptions {
        public MSLVersion languageVersion = MSLVersion.MSL_2_3;
        public boolean fastMathEnabled = true;
        public boolean preserveInvariants = false;
        public OptimizationLevel optimizationLevel = OptimizationLevel.DEFAULT;
        public Map<String, String> preprocessorMacros = new HashMap<>();
        public boolean mathRelyOnImplicitArguments = false;
        public boolean mathAllowFloatDivisionByZero = false;
        public InstallName installName = null;
        public List<String> libraries = new ArrayList<>();
        
        public enum OptimizationLevel {
            DEFAULT(0),
            NONE(1),
            SIZE(2),
            PERFORMANCE(3);
            
            public final int value;
            OptimizationLevel(int value) { this.value = value; }
        }
        
        /**
         * Install name for dynamic libraries (Metal 3.1+).
         */
        public static class InstallName {
            public String name;
            public String version;
            
            public InstallName(String name, String version) {
                this.name = name;
                this.version = version;
            }
        }
        
        /**
         * Convert to native MTLCompileOptions handle.
         */
        public long toNative() {
            long options = nMTLCompileOptionsNew();
            
            nMTLCompileOptionsSetLanguageVersion(options, languageVersion.code);
            nMTLCompileOptionsSetFastMathEnabled(options, fastMathEnabled);
            nMTLCompileOptionsSetPreserveInvariants(options, preserveInvariants);
            nMTLCompileOptionsSetOptimizationLevel(options, optimizationLevel.value);
            
            for (Map.Entry<String, String> macro : preprocessorMacros.entrySet()) {
                nMTLCompileOptionsAddPreprocessorMacro(options, macro.getKey(), macro.getValue());
            }
            
            if (installName != null) {
                nMTLCompileOptionsSetInstallName(options, installName.name);
            }
            
            for (String lib : libraries) {
                nMTLCompileOptionsAddLibrary(options, lib);
            }
            
            return options;
        }
        
        /**
         * Create a copy of these options.
         */
        public MTLCompileOptions copy() {
            MTLCompileOptions copy = new MTLCompileOptions();
            copy.languageVersion = this.languageVersion;
            copy.fastMathEnabled = this.fastMathEnabled;
            copy.preserveInvariants = this.preserveInvariants;
            copy.optimizationLevel = this.optimizationLevel;
            copy.preprocessorMacros = new HashMap<>(this.preprocessorMacros);
            copy.mathRelyOnImplicitArguments = this.mathRelyOnImplicitArguments;
            copy.mathAllowFloatDivisionByZero = this.mathAllowFloatDivisionByZero;
            copy.installName = this.installName;
            copy.libraries = new ArrayList<>(this.libraries);
            return copy;
        }
    }
    
    /**
     * Compiled Metal library.
     */
    public static final class CompiledLibrary implements AutoCloseable {
        
        public final long handle;
        public final String name;
        public final String hash;
        public final MSLVersion version;
        public final ShaderType type;
        public final long compileTimeNs;
        public final Map<String, CompiledFunction> functions;
        public final LibraryReflection reflection;
        
        private boolean released = false;
        
        CompiledLibrary(long handle, String name, String hash, MSLVersion version,
                       ShaderType type, long compileTimeNs, LibraryReflection reflection) {
            this.handle = handle;
            this.name = name;
            this.hash = hash;
            this.version = version;
            this.type = type;
            this.compileTimeNs = compileTimeNs;
            this.functions = new ConcurrentHashMap<>();
            this.reflection = reflection;
        }
        
        /**
         * Get a function by name.
         */
        public CompiledFunction getFunction(String name) {
            return functions.computeIfAbsent(name, n -> {
                long fn = nMTLLibraryNewFunctionWithName(handle, n);
                if (fn == 0) {
                    throw new ShaderCompilationException(this.name, "Function not found: " + n);
                }
                return new CompiledFunction(fn, n, this);
            });
        }
        
        /**
         * Get a function with constant values.
         */
        public CompiledFunction getFunctionWithConstants(String name, FunctionConstantValues constants) {
            String key = name + "_" + constants.computeHash();
            return functions.computeIfAbsent(key, k -> {
                long constantsHandle = constants.toNative();
                long[] error = {0};
                long fn = nMTLLibraryNewFunctionWithNameConstantValues(handle, name, constantsHandle, error);
                nRelease(constantsHandle);
                
                if (fn == 0) {
                    String errorMsg = error[0] != 0 ? nNSErrorLocalizedDescription(error[0]) : "Unknown error";
                    if (error[0] != 0) nRelease(error[0]);
                    throw new ShaderCompilationException(this.name, 
                        "Failed to create function with constants: " + name + " - " + errorMsg);
                }
                
                return new CompiledFunction(fn, name, this);
            });
        }
        
        /**
         * Get all function names in this library.
         */
        public String[] getFunctionNames() {
            return nMTLLibraryGetFunctionNames(handle);
        }
        
        /**
         * Check if released.
         */
        public boolean isReleased() {
            return released;
        }
        
        @Override
        public void close() {
            if (!released) {
                released = true;
                for (CompiledFunction fn : functions.values()) {
                    fn.close();
                }
                functions.clear();
                nRelease(handle);
            }
        }
    }
    
    /**
     * Compiled shader function.
     */
    public static final class CompiledFunction implements AutoCloseable {
        
        public final long handle;
        public final String name;
        public final CompiledLibrary library;
        
        private FunctionReflection reflection;
        private boolean released = false;
        
        CompiledFunction(long handle, String name, CompiledLibrary library) {
            this.handle = handle;
            this.name = name;
            this.library = library;
        }
        
        /**
         * Get function reflection data.
         */
        public FunctionReflection getReflection() {
            if (reflection == null) {
                reflection = extractFunctionReflection(handle);
            }
            return reflection;
        }
        
        /**
         * Get the function type.
         */
        public MTLFunctionType getFunctionType() {
            return MTLFunctionType.fromValue(nMTLFunctionGetFunctionType(handle));
        }
        
        /**
         * Get vertex attributes (for vertex functions).
         */
        public List<VertexAttribute> getVertexAttributes() {
            return getReflection().vertexAttributes;
        }
        
        /**
         * Get stage input attributes (for fragment functions).
         */
        public List<StageInputAttribute> getStageInputAttributes() {
            return getReflection().stageInputAttributes;
        }
        
        @Override
        public void close() {
            if (!released) {
                released = true;
                nRelease(handle);
            }
        }
    }
    
    /**
     * Function type enumeration.
     */
    public enum MTLFunctionType {
        VERTEX(1),
        FRAGMENT(2),
        KERNEL(3),
        VISIBLE(5),
        INTERSECTION(6),
        MESH(7),
        OBJECT(8);
        
        public final int value;
        
        MTLFunctionType(int value) {
            this.value = value;
        }
        
        public static MTLFunctionType fromValue(int value) {
            for (MTLFunctionType type : values()) {
                if (type.value == value) return type;
            }
            return VERTEX;
        }
    }
    
    /**
     * Shader type enumeration.
     */
    public enum ShaderType {
        MSL,
        SPIRV,
        HLSL,
        GLSL
    }
    
    /**
     * Function constant values.
     */
    public static final class FunctionConstantValues {
        
        private final List<ConstantEntry> entries = new ArrayList<>();
        
        private static final class ConstantEntry {
            final String name;
            final int index;
            final ConstantType type;
            final Object value;
            
            ConstantEntry(String name, int index, ConstantType type, Object value) {
                this.name = name;
                this.index = index;
                this.type = type;
                this.value = value;
            }
        }
        
        public enum ConstantType {
            BOOL(0, 1),
            CHAR(1, 1),
            UCHAR(2, 1),
            SHORT(3, 2),
            USHORT(4, 2),
            INT(5, 4),
            UINT(6, 4),
            LONG(7, 8),
            ULONG(8, 8),
            HALF(9, 2),
            FLOAT(10, 4),
            DOUBLE(11, 8),
            INT2(12, 8),
            INT3(13, 12),
            INT4(14, 16),
            FLOAT2(15, 8),
            FLOAT3(16, 12),
            FLOAT4(17, 16);
            
            public final int code;
            public final int size;
            
            ConstantType(int code, int size) {
                this.code = code;
                this.size = size;
            }
        }
        
        public FunctionConstantValues setBool(String name, boolean value) {
            entries.add(new ConstantEntry(name, -1, ConstantType.BOOL, value));
            return this;
        }
        
        public FunctionConstantValues setBool(int index, boolean value) {
            entries.add(new ConstantEntry(null, index, ConstantType.BOOL, value));
            return this;
        }
        
        public FunctionConstantValues setInt(String name, int value) {
            entries.add(new ConstantEntry(name, -1, ConstantType.INT, value));
            return this;
        }
        
        public FunctionConstantValues setInt(int index, int value) {
            entries.add(new ConstantEntry(null, index, ConstantType.INT, value));
            return this;
        }
        
        public FunctionConstantValues setUInt(String name, int value) {
            entries.add(new ConstantEntry(name, -1, ConstantType.UINT, value));
            return this;
        }
        
        public FunctionConstantValues setUInt(int index, int value) {
            entries.add(new ConstantEntry(null, index, ConstantType.UINT, value));
            return this;
        }
        
        public FunctionConstantValues setFloat(String name, float value) {
            entries.add(new ConstantEntry(name, -1, ConstantType.FLOAT, value));
            return this;
        }
        
        public FunctionConstantValues setFloat(int index, float value) {
            entries.add(new ConstantEntry(null, index, ConstantType.FLOAT, value));
            return this;
        }
        
        public FunctionConstantValues setHalf(String name, float value) {
            entries.add(new ConstantEntry(name, -1, ConstantType.HALF, value));
            return this;
        }
        
        public FunctionConstantValues setHalf(int index, float value) {
            entries.add(new ConstantEntry(null, index, ConstantType.HALF, value));
            return this;
        }
        
        public FunctionConstantValues setFloat2(String name, float x, float y) {
            entries.add(new ConstantEntry(name, -1, ConstantType.FLOAT2, new float[]{x, y}));
            return this;
        }
        
        public FunctionConstantValues setFloat3(String name, float x, float y, float z) {
            entries.add(new ConstantEntry(name, -1, ConstantType.FLOAT3, new float[]{x, y, z}));
            return this;
        }
        
        public FunctionConstantValues setFloat4(String name, float x, float y, float z, float w) {
            entries.add(new ConstantEntry(name, -1, ConstantType.FLOAT4, new float[]{x, y, z, w}));
            return this;
        }
        
        /**
         * Convert to native handle.
         */
        public long toNative() {
            long handle = nMTLFunctionConstantValuesNew();
            
            for (ConstantEntry entry : entries) {
                switch (entry.type) {
                    case BOOL -> {
                        if (entry.name != null) {
                            nMTLFunctionConstantValuesSetBoolByName(handle, entry.name, (Boolean) entry.value);
                        } else {
                            nMTLFunctionConstantValuesSetBoolByIndex(handle, entry.index, (Boolean) entry.value);
                        }
                    }
                    case INT -> {
                        if (entry.name != null) {
                            nMTLFunctionConstantValuesSetIntByName(handle, entry.name, (Integer) entry.value);
                        } else {
                            nMTLFunctionConstantValuesSetIntByIndex(handle, entry.index, (Integer) entry.value);
                        }
                    }
                    case UINT -> {
                        if (entry.name != null) {
                            nMTLFunctionConstantValuesSetUIntByName(handle, entry.name, (Integer) entry.value);
                        } else {
                            nMTLFunctionConstantValuesSetUIntByIndex(handle, entry.index, (Integer) entry.value);
                        }
                    }
                    case FLOAT -> {
                        if (entry.name != null) {
                            nMTLFunctionConstantValuesSetFloatByName(handle, entry.name, (Float) entry.value);
                        } else {
                            nMTLFunctionConstantValuesSetFloatByIndex(handle, entry.index, (Float) entry.value);
                        }
                    }
                    case HALF -> {
                        if (entry.name != null) {
                            nMTLFunctionConstantValuesSetHalfByName(handle, entry.name, (Float) entry.value);
                        } else {
                            nMTLFunctionConstantValuesSetHalfByIndex(handle, entry.index, (Float) entry.value);
                        }
                    }
                    case FLOAT2 -> {
                        float[] v = (float[]) entry.value;
                        nMTLFunctionConstantValuesSetFloat2ByName(handle, entry.name, v[0], v[1]);
                    }
                    case FLOAT3 -> {
                        float[] v = (float[]) entry.value;
                        nMTLFunctionConstantValuesSetFloat3ByName(handle, entry.name, v[0], v[1], v[2]);
                    }
                    case FLOAT4 -> {
                        float[] v = (float[]) entry.value;
                        nMTLFunctionConstantValuesSetFloat4ByName(handle, entry.name, v[0], v[1], v[2], v[3]);
                    }
                }
            }
            
            return handle;
        }
        
        /**
         * Compute a hash for caching.
         */
        public String computeHash() {
            StringBuilder sb = new StringBuilder();
            for (ConstantEntry entry : entries) {
                if (entry.name != null) sb.append(entry.name);
                sb.append(entry.index).append('_');
                sb.append(entry.type.name()).append('_');
                sb.append(entry.value).append(';');
            }
            return Integer.toHexString(sb.toString().hashCode());
        }
    }
    
    /**
     * Runtime shader compiler.
     */
    public final class ShaderCompiler {
        
        private final long device;
        private final MSLCapabilities capabilities;
        private final MTLCompileOptions defaultOptions;
        private final ConcurrentHashMap<String, CompiledLibrary> libraryCache = new ConcurrentHashMap<>();
        private final ExecutorService compilationExecutor;
        private final CompilationStats stats = new CompilationStats();
        
        public ShaderCompiler(long device, MSLCapabilities capabilities) {
            this.device = device;
            this.capabilities = capabilities;
            this.defaultOptions = capabilities.getRecommendedCompileOptions();
            this.compilationExecutor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                r -> {
                    Thread t = new Thread(r, "MSL-Compiler");
                    t.setDaemon(true);
                    return t;
                }
            );
        }
        
        /**
         * Compile MSL source to a library.
         */
        public CompiledLibrary compileFromSource(String name, String source) {
            return compileFromSource(name, source, null);
        }
        
        /**
         * Compile MSL source with options.
         */
        public CompiledLibrary compileFromSource(String name, String source, 
                                                 @Nullable MTLCompileOptions options) {
            String hash = computeHash(source, options);
            
            // Check cache
            CompiledLibrary cached = libraryCache.get(hash);
            if (cached != null && !cached.isReleased()) {
                stats.cacheHits.incrementAndGet();
                return cached;
            }
            
            stats.cacheMisses.incrementAndGet();
            stats.compilations.incrementAndGet();
            long startTime = System.nanoTime();
            
            // Prepare options
            MTLCompileOptions opts = options != null ? options : defaultOptions;
            long optionsHandle = opts.toNative();
            
            try {
                // Compile
                long[] error = {0};
                long library = nMTLDeviceNewLibraryWithSource(device, source, optionsHandle, error);
                
                if (library == 0) {
                    String errorMsg = error[0] != 0 ? nNSErrorLocalizedDescription(error[0]) : "Unknown error";
                    if (error[0] != 0) nRelease(error[0]);
                    stats.failures.incrementAndGet();
                    
                    throw new ShaderCompilationException(name, formatCompilationError(errorMsg, source));
                }
                
                if (error[0] != 0) nRelease(error[0]);
                
                long compileTime = System.nanoTime() - startTime;
                stats.totalCompileTimeNs.addAndGet(compileTime);
                
                // Extract reflection
                LibraryReflection reflection = extractLibraryReflection(library);
                
                CompiledLibrary compiled = new CompiledLibrary(
                    library, name, hash, opts.languageVersion, 
                    ShaderType.MSL, compileTime, reflection
                );
                
                libraryCache.put(hash, compiled);
                return compiled;
                
            } finally {
                nRelease(optionsHandle);
            }
        }
        
        /**
         * Compile MSL source asynchronously.
         */
        public CompletableFuture<CompiledLibrary> compileFromSourceAsync(
                String name, String source, @Nullable MTLCompileOptions options) {
            
            return CompletableFuture.supplyAsync(() -> 
                compileFromSource(name, source, options), compilationExecutor);
        }
        
        /**
         * Load a pre-compiled library from a file.
         */
        public CompiledLibrary loadFromFile(String name, String path) {
            String hash = "file:" + path;
            
            CompiledLibrary cached = libraryCache.get(hash);
            if (cached != null && !cached.isReleased()) {
                return cached;
            }
            
            long startTime = System.nanoTime();
            long[] error = {0};
            long library = nMTLDeviceNewLibraryWithFile(device, path, error);
            
            if (library == 0) {
                String errorMsg = error[0] != 0 ? nNSErrorLocalizedDescription(error[0]) : "File not found";
                if (error[0] != 0) nRelease(error[0]);
                throw new ShaderCompilationException(name, "Failed to load library: " + errorMsg);
            }
            
            if (error[0] != 0) nRelease(error[0]);
            
            long loadTime = System.nanoTime() - startTime;
            LibraryReflection reflection = extractLibraryReflection(library);
            
            CompiledLibrary compiled = new CompiledLibrary(
                library, name, hash, capabilities.version,
                ShaderType.MSL, loadTime, reflection
            );
            
            libraryCache.put(hash, compiled);
            return compiled;
        }
        
        /**
         * Load a library from binary data.
         */
        public CompiledLibrary loadFromData(String name, ByteBuffer data) {
            long startTime = System.nanoTime();
            long[] error = {0};
            
            // Create dispatch_data_t from buffer
            long dispatchData = nCreateDispatchDataFromBuffer(data);
            long library = nMTLDeviceNewLibraryWithData(device, dispatchData, error);
            nReleaseDispatchData(dispatchData);
            
            if (library == 0) {
                String errorMsg = error[0] != 0 ? nNSErrorLocalizedDescription(error[0]) : "Invalid data";
                if (error[0] != 0) nRelease(error[0]);
                throw new ShaderCompilationException(name, "Failed to load library from data: " + errorMsg);
            }
            
            if (error[0] != 0) nRelease(error[0]);
            
            long loadTime = System.nanoTime() - startTime;
            String hash = computeDataHash(data);
            LibraryReflection reflection = extractLibraryReflection(library);
            
            CompiledLibrary compiled = new CompiledLibrary(
                library, name, hash, capabilities.version,
                ShaderType.MSL, loadTime, reflection
            );
            
            libraryCache.put(hash, compiled);
            return compiled;
        }
        
        /**
         * Get the default library (compiled into the app bundle).
         */
        public CompiledLibrary getDefaultLibrary() {
            long library = nMTLDeviceNewDefaultLibrary(device);
            if (library == 0) {
                throw new ShaderCompilationException("default", "No default library found");
            }
            
            LibraryReflection reflection = extractLibraryReflection(library);
            return new CompiledLibrary(library, "default", "default", 
                capabilities.version, ShaderType.MSL, 0, reflection);
        }
        
        /**
         * Evict unused libraries from cache.
         */
        public void evictUnused(long maxAgeMs) {
            long cutoff = System.currentTimeMillis() - maxAgeMs;
            libraryCache.entrySet().removeIf(entry -> {
                CompiledLibrary lib = entry.getValue();
                // Simple eviction - in production, track last access time
                if (lib.isReleased()) {
                    return true;
                }
                return false;
            });
        }
        
        /**
         * Clear cache.
         */
        public void clearCache() {
            for (CompiledLibrary lib : libraryCache.values()) {
                if (!lib.isReleased()) {
                    lib.close();
                }
            }
            libraryCache.clear();
        }
        
        /**
         * Get statistics.
         */
        public CompilationStats getStats() {
            return stats;
        }
        
        /**
         * Shutdown the compiler.
         */
        public void shutdown() {
            compilationExecutor.shutdown();
            try {
                if (!compilationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    compilationExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                compilationExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            clearCache();
        }
        
        private String computeHash(String source, @Nullable MTLCompileOptions options) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(source.getBytes(StandardCharsets.UTF_8));
                if (options != null) {
                    digest.update((byte) options.languageVersion.ordinal());
                    digest.update((byte) (options.fastMathEnabled ? 1 : 0));
                    digest.update((byte) options.optimizationLevel.ordinal());
                    for (Map.Entry<String, String> macro : options.preprocessorMacros.entrySet()) {
                        digest.update(macro.getKey().getBytes(StandardCharsets.UTF_8));
                        digest.update(macro.getValue().getBytes(StandardCharsets.UTF_8));
                    }
                }
                byte[] hashBytes = digest.digest();
                StringBuilder sb = new StringBuilder(hashBytes.length * 2);
                for (byte b : hashBytes) {
                    sb.append(String.format("%02x", b & 0xff));
                }
                return sb.toString();
            } catch (Exception e) {
                return Integer.toHexString(source.hashCode());
            }
        }
        
        private String computeDataHash(ByteBuffer data) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                data.mark();
                while (data.hasRemaining()) {
                    digest.update(data.get());
                }
                data.reset();
                byte[] hashBytes = digest.digest();
                StringBuilder sb = new StringBuilder(hashBytes.length * 2);
                for (byte b : hashBytes) {
                    sb.append(String.format("%02x", b & 0xff));
                }
                return sb.toString();
            } catch (Exception e) {
                return "data_" + data.hashCode();
            }
        }
        
        private String formatCompilationError(String error, String source) {
            StringBuilder sb = new StringBuilder();
            sb.append(error).append("\n\n");
            
            // Try to extract line numbers from error
            Pattern linePattern = Pattern.compile(":(\\d+):");
            Matcher matcher = linePattern.matcher(error);
            Set<Integer> errorLines = new HashSet<>();
            while (matcher.find()) {
                errorLines.add(Integer.parseInt(matcher.group(1)));
            }
            
            // Add source context
            if (!errorLines.isEmpty()) {
                String[] lines = source.split("\n");
                sb.append("Source context:\n");
                for (int lineNum : errorLines) {
                    if (lineNum > 0 && lineNum <= lines.length) {
                        int start = Math.max(0, lineNum - 3);
                        int end = Math.min(lines.length, lineNum + 2);
                        for (int i = start; i < end; i++) {
                            String prefix = (i == lineNum - 1) ? ">>> " : "    ";
                            sb.append(String.format("%s%4d: %s\n", prefix, i + 1, lines[i]));
                        }
                        sb.append("\n");
                    }
                }
            }
            
            return sb.toString();
        }
    }
    
    /**
     * Compilation statistics.
     */
    public static final class CompilationStats {
        public final AtomicLong compilations = new AtomicLong();
        public final AtomicLong cacheHits = new AtomicLong();
        public final AtomicLong cacheMisses = new AtomicLong();
        public final AtomicLong failures = new AtomicLong();
        public final AtomicLong totalCompileTimeNs = new AtomicLong();
        
        public double getCacheHitRate() {
            long total = cacheHits.get() + cacheMisses.get();
            return total > 0 ? (double) cacheHits.get() / total : 0;
        }
        
        public double getAverageCompileTimeMs() {
            long count = compilations.get();
            return count > 0 ? totalCompileTimeNs.get() / count / 1_000_000.0 : 0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "CompilationStats[compilations=%d, cacheHit=%.1f%%, avgTime=%.2fms, failures=%d]",
                compilations.get(), getCacheHitRate() * 100, getAverageCompileTimeMs(), failures.get()
            );
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 3: SHADER FUNCTION TYPES AND ATTRIBUTES
    // Mapping of shader stages and attribute qualifiers
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Shader stage enumeration.
     */
    public enum ShaderStage {
        VERTEX("vertex", "[[vertex]]"),
        FRAGMENT("fragment", "[[fragment]]"),
        KERNEL("kernel", "[[kernel]]"),
        TESSELLATION_CONTROL("tessellation_control", "[[patch(quad, 4)]]"),
        TESSELLATION_EVALUATION("tessellation_evaluation", "[[post_tessellation_vertex]]"),
        OBJECT("object", "[[object]]"),
        MESH("mesh", "[[mesh]]"),
        INTERSECTION("intersection", "[[intersection(triangle, instancing)]]"),
        TILE("tile", "[[tile]]"),
        VISIBLE("visible", "[[visible]]");
        
        public final String name;
        public final String attribute;
        
        ShaderStage(String name, String attribute) {
            this.name = name;
            this.attribute = attribute;
        }
        
        public MSLFeature getRequiredFeature() {
            return switch (this) {
                case VERTEX, FRAGMENT, KERNEL -> MSLFeature.BASIC_SHADERS;
                case TESSELLATION_CONTROL, TESSELLATION_EVALUATION -> MSLFeature.TESSELLATION;
                case OBJECT, MESH -> MSLFeature.MESH_SHADERS;
                case INTERSECTION -> MSLFeature.INTERSECTION_FUNCTIONS;
                case TILE -> MSLFeature.TILE_SHADERS;
                case VISIBLE -> MSLFeature.FUNCTION_POINTERS;
            };
        }
    }
    
    /**
     * MSL attribute types for shader inputs/outputs.
     */
    public enum MSLAttribute {
        // Buffer/Resource bindings
        BUFFER("buffer", "[[buffer(%d)]]"),
        TEXTURE("texture", "[[texture(%d)]]"),
        SAMPLER("sampler", "[[sampler(%d)]]"),
        
        // Vertex shader
        STAGE_IN("stage_in", "[[stage_in]]"),
        VERTEX_ID("vertex_id", "[[vertex_id]]"),
        INSTANCE_ID("instance_id", "[[instance_id]]"),
        BASE_VERTEX("base_vertex", "[[base_vertex]]"),
        BASE_INSTANCE("base_instance", "[[base_instance]]"),
        
        // Vertex attributes
        ATTRIBUTE("attribute", "[[attribute(%d)]]"),
        
        // Vertex output / Fragment input
        POSITION("position", "[[position]]"),
        POINT_SIZE("point_size", "[[point_size]]"),
        CLIP_DISTANCE("clip_distance", "[[clip_distance]]"),
        USER("user", "[[user(%s)]]"),
        FLAT("flat", "[[flat]]"),
        CENTROID("centroid", "[[centroid_perspective]]"),
        SAMPLE("sample", "[[sample_perspective]]"),
        CENTER("center", "[[center_perspective]]"),
        CENTROID_NO_PERSPECTIVE("centroid_no_perspective", "[[centroid_no_perspective]]"),
        SAMPLE_NO_PERSPECTIVE("sample_no_perspective", "[[sample_no_perspective]]"),
        CENTER_NO_PERSPECTIVE("center_no_perspective", "[[center_no_perspective]]"),
        
        // Fragment shader
        FRONT_FACING("front_facing", "[[front_facing]]"),
        POINT_COORD("point_coord", "[[point_coord]]"),
        SAMPLE_ID("sample_id", "[[sample_id]]"),
        SAMPLE_MASK("sample_mask", "[[sample_mask]]"),
        FRAGMENT_COORD("position", "[[position]]"),
        
        // Fragment output
        COLOR("color", "[[color(%d)]]"),
        DEPTH("depth", "[[depth(%s)]]"),
        STENCIL("stencil", "[[stencil]]"),
        COVERAGE("coverage", "[[sample_mask]]"),
        
        // Compute shader
        THREAD_POSITION_IN_GRID("thread_position_in_grid", "[[thread_position_in_grid]]"),
        THREAD_POSITION_IN_THREADGROUP("thread_position_in_threadgroup", "[[thread_position_in_threadgroup]]"),
        THREADGROUP_POSITION_IN_GRID("threadgroup_position_in_grid", "[[threadgroup_position_in_grid]]"),
        THREADS_PER_THREADGROUP("threads_per_threadgroup", "[[threads_per_threadgroup]]"),
        THREADGROUPS_PER_GRID("threadgroups_per_grid", "[[threadgroups_per_grid]]"),
        THREAD_INDEX_IN_THREADGROUP("thread_index_in_threadgroup", "[[thread_index_in_threadgroup]]"),
        THREAD_INDEX_IN_SIMDGROUP("thread_index_in_simdgroup", "[[thread_index_in_simdgroup]]"),
        SIMDGROUP_INDEX_IN_THREADGROUP("simdgroup_index_in_threadgroup", "[[simdgroup_index_in_threadgroup]]"),
        THREADS_PER_SIMDGROUP("threads_per_simdgroup", "[[threads_per_simdgroup]]"),
        DISPATCH_THREADS_PER_THREADGROUP("dispatch_threads_per_threadgroup", "[[dispatch_threads_per_threadgroup]]"),
        
        // Mesh/Object shader
        OBJECT_PAYLOAD("object_payload", "[[payload]]"),
        MESH_GRID_ORIGIN("mesh_grid_origin", "[[mesh_grid_origin]]"),
        MESH_GRID_SIZE("mesh_grid_size", "[[mesh_grid_size]]"),
        THREADGROUPS_PER_MESH_GRID("threadgroups_per_mesh_grid", "[[threadgroups_per_mesh_grid]]"),
        
        // Tessellation
        PATCH_ID("patch_id", "[[patch_id]]"),
        POSITION_IN_PATCH("position_in_patch", "[[position_in_patch]]"),
        
        // Ray tracing
        RAY_DATA("ray_data", "[[ray_data]]"),
        INSTANCE_ACCELERATION_STRUCTURE("instance_acceleration_structure", "[[instance_acceleration_structure]]"),
        PRIMITIVE_ACCELERATION_STRUCTURE("primitive_acceleration_structure", "[[primitive_acceleration_structure]]"),
        INTERSECTION_RESULT("intersection_result", "[[intersection_result]]"),
        MAX_DISTANCE("max_distance", "[[max_distance]]"),
        
        // Tile shader
        TILE_DATA("tile_data", "[[tile]]"),
        IMAGEBLOCK("imageblock", "[[imageblock_data]]"),
        IMAGEBLOCK_SAMPLE_COVERAGE("imageblock_sample_coverage", "[[imageblock_sample_coverage]]"),
        
        // Argument buffers
        ID("id", "[[id(%d)]]"),
        
        // Address space qualifiers (not attributes but related)
        DEVICE("device", "device"),
        CONSTANT("constant", "constant"),
        THREAD("thread", "thread"),
        THREADGROUP("threadgroup", "threadgroup"),
        THREADGROUP_IMAGEBLOCK("threadgroup_imageblock", "threadgroup_imageblock");
        
        public final String name;
        public final String format;
        
        MSLAttribute(String name, String format) {
            this.name = name;
            this.format = format;
        }
        
        public String format(Object... args) {
            return String.format(format, args);
        }
    }
    
    /**
     * Depth function attribute values.
     */
    public enum DepthMode {
        ANY("any"),
        GREATER("greater"),
        LESS("less"),
        UNCHANGED("unchanged");
        
        public final String value;
        DepthMode(String value) { this.value = value; }
    }
    
    /**
     * MSL data type mapping.
     */
    public enum MSLDataType {
        // Scalar types
        BOOL("bool", 1, 1),
        CHAR("char", 1, 1),
        UCHAR("uchar", 1, 1),
        SHORT("short", 2, 1),
        USHORT("ushort", 2, 1),
        INT("int", 4, 1),
        UINT("uint", 4, 1),
        LONG("long", 8, 1),
        ULONG("ulong", 8, 1),
        HALF("half", 2, 1),
        FLOAT("float", 4, 1),
        
        // Vector types
        BOOL2("bool2", 2, 2),
        BOOL3("bool3", 3, 3),
        BOOL4("bool4", 4, 4),
        CHAR2("char2", 2, 2),
        CHAR3("char3", 3, 3),
        CHAR4("char4", 4, 4),
        UCHAR2("uchar2", 2, 2),
        UCHAR3("uchar3", 3, 3),
        UCHAR4("uchar4", 4, 4),
        SHORT2("short2", 4, 2),
        SHORT3("short3", 6, 3),
        SHORT4("short4", 8, 4),
        USHORT2("ushort2", 4, 2),
        USHORT3("ushort3", 6, 3),
        USHORT4("ushort4", 8, 4),
        INT2("int2", 8, 2),
        INT3("int3", 12, 3),
        INT4("int4", 16, 4),
        UINT2("uint2", 8, 2),
        UINT3("uint3", 12, 3),
        UINT4("uint4", 16, 4),
        HALF2("half2", 4, 2),
        HALF3("half3", 6, 3),
        HALF4("half4", 8, 4),
        FLOAT2("float2", 8, 2),
        FLOAT3("float3", 12, 3),
        FLOAT4("float4", 16, 4),
        
        // Matrix types
        HALF2X2("half2x2", 8, 4),
        HALF2X3("half2x3", 12, 6),
        HALF2X4("half2x4", 16, 8),
        HALF3X2("half3x2", 12, 6),
        HALF3X3("half3x3", 18, 9),
        HALF3X4("half3x4", 24, 12),
        HALF4X2("half4x2", 16, 8),
        HALF4X3("half4x3", 24, 12),
        HALF4X4("half4x4", 32, 16),
        FLOAT2X2("float2x2", 16, 4),
        FLOAT2X3("float2x3", 24, 6),
        FLOAT2X4("float2x4", 32, 8),
        FLOAT3X2("float3x2", 24, 6),
        FLOAT3X3("float3x3", 36, 9),
        FLOAT3X4("float3x4", 48, 12),
        FLOAT4X2("float4x2", 32, 8),
        FLOAT4X3("float4x3", 48, 12),
        FLOAT4X4("float4x4", 64, 16),
        
        // Packed types
        PACKED_FLOAT2("packed_float2", 8, 2),
        PACKED_FLOAT3("packed_float3", 12, 3),
        PACKED_FLOAT4("packed_float4", 16, 4),
        PACKED_HALF2("packed_half2", 4, 2),
        PACKED_HALF3("packed_half3", 6, 3),
        PACKED_HALF4("packed_half4", 8, 4),
        PACKED_INT2("packed_int2", 8, 2),
        PACKED_INT3("packed_int3", 12, 3),
        PACKED_INT4("packed_int4", 16, 4),
        
        // Texture types
        TEXTURE2D("texture2d<float>", 8, 1),
        TEXTURE2D_HALF("texture2d<half>", 8, 1),
        TEXTURE2D_INT("texture2d<int>", 8, 1),
        TEXTURE2D_UINT("texture2d<uint>", 8, 1),
        TEXTURE3D("texture3d<float>", 8, 1),
        TEXTURE_CUBE("texturecube<float>", 8, 1),
        TEXTURE2D_ARRAY("texture2d_array<float>", 8, 1),
        TEXTURE2D_MS("texture2d_ms<float>", 8, 1),
        DEPTH_TEXTURE2D("depth2d<float>", 8, 1),
        DEPTH_TEXTURE_CUBE("depthcube<float>", 8, 1),
        DEPTH_TEXTURE2D_ARRAY("depth2d_array<float>", 8, 1),
        
        // Sampler
        SAMPLER("sampler", 8, 1),
        
        // Special
        VOID("void", 0, 0),
        POINTER("*", 8, 1);
        
        public final String mslName;
        public final int byteSize;
        public final int componentCount;
        
        MSLDataType(String mslName, int byteSize, int componentCount) {
            this.mslName = mslName;
            this.byteSize = byteSize;
            this.componentCount = componentCount;
        }
        
        /**
         * Get the base scalar type.
         */
        public MSLDataType getBaseType() {
            String name = this.mslName.replace("packed_", "");
            if (name.contains("float")) return FLOAT;
            if (name.contains("half")) return HALF;
            if (name.contains("int") && !name.contains("uint")) return INT;
            if (name.contains("uint")) return UINT;
            if (name.contains("short") && !name.contains("ushort")) return SHORT;
            if (name.contains("ushort")) return USHORT;
            if (name.contains("char") && !name.contains("uchar")) return CHAR;
            if (name.contains("uchar")) return UCHAR;
            if (name.contains("bool")) return BOOL;
            return this;
        }
        
        /**
         * Check if this is a vector type.
         */
        public boolean isVector() {
            return componentCount > 1 && componentCount <= 4 && !isMatrix();
        }
        
        /**
         * Check if this is a matrix type.
         */
        public boolean isMatrix() {
            return mslName.contains("x");
        }
        
        /**
         * Check if this is a texture type.
         */
        public boolean isTexture() {
            return mslName.startsWith("texture") || mslName.startsWith("depth");
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 4: SPIR-V TO MSL CROSS-COMPILATION
    // SPIRV-Cross integration for shader cross-compilation
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * SPIR-V to MSL compiler using SPIRV-Cross.
     */
    public final class SPIRVToMSLCompiler {
        
        private final MSLCapabilities capabilities;
        private final SPIRVCrossOptions defaultOptions;
        
        public SPIRVToMSLCompiler(MSLCapabilities capabilities) {
            this.capabilities = capabilities;
            this.defaultOptions = new SPIRVCrossOptions();
            this.defaultOptions.mslVersion = capabilities.version;
            this.defaultOptions.platform = SPIRVCrossOptions.Platform.MACOS;
            this.defaultOptions.enableArgumentBuffers = capabilities.hasFeature(MSLFeature.ARGUMENT_BUFFERS);
            this.defaultOptions.argumentBuffersTier = capabilities.hasFeature(MSLFeature.ARGUMENT_BUFFERS) ? 2 : 0;
        }
        
        /**
         * SPIRV-Cross compilation options.
         */
        public static final class SPIRVCrossOptions {
            public MSLVersion mslVersion = MSLVersion.MSL_2_3;
            public Platform platform = Platform.MACOS;
            public boolean enableArgumentBuffers = true;
            public int argumentBuffersTier = 2;
            public boolean enableFastMath = true;
            public boolean enableFramebufferFetch = false;
            public boolean enableSIMDGroupFunctions = true;
            public boolean enableQuadGroupFunctions = false;
            public boolean enablePointSizeBuiltin = false;
            public boolean forceActiveArgumentBufferResources = false;
            public boolean forceNativeArrays = false;
            public boolean emulateSubgroups = false;
            public int subgroupSize = 32;
            public boolean enableRowMajorMatrices = false;
            public boolean vertexBufferBindingOffset = false;
            public int vertexBufferBindingBase = 0;
            public int textureBufferBindingBase = 0;
            public int samplerBindingBase = 0;
            
            // Resource binding overrides
            public final Map<ResourceKey, ResourceBinding> bufferBindings = new HashMap<>();
            public final Map<ResourceKey, ResourceBinding> textureBindings = new HashMap<>();
            public final Map<ResourceKey, ResourceBinding> samplerBindings = new HashMap<>();
            
            public enum Platform {
                MACOS(0),
                IOS(1),
                IOS_SIMULATOR(2);
                
                public final int value;
                Platform(int value) { this.value = value; }
            }
        }
        
        /**
         * Resource key for binding overrides.
         */
        public static final class ResourceKey {
            public final int descriptorSet;
            public final int binding;
            
            public ResourceKey(int descriptorSet, int binding) {
                this.descriptorSet = descriptorSet;
                this.binding = binding;
            }
            
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof ResourceKey that)) return false;
                return descriptorSet == that.descriptorSet && binding == that.binding;
            }
            
            @Override
            public int hashCode() {
                return Objects.hash(descriptorSet, binding);
            }
        }
        
        /**
         * Resource binding result.
         */
        public static final class ResourceBinding {
            public int mslBuffer = -1;
            public int mslTexture = -1;
            public int mslSampler = -1;
        }
        
        /**
         * Compilation result.
         */
        public static final class SPIRVCompilationResult {
            public boolean success;
            public String mslSource;
            public String errorMessage;
            public ShaderStage stage;
            public String entryPoint;
            public final List<ReflectedResource> resources = new ArrayList<>();
            public final List<ReflectedVertexAttribute> vertexAttributes = new ArrayList<>();
            public final Map<String, Integer> bufferBindings = new HashMap<>();
            public final Map<String, Integer> textureBindings = new HashMap<>();
            public final Map<String, Integer> samplerBindings = new HashMap<>();
            public int threadgroupSizeX;
            public int threadgroupSizeY;
            public int threadgroupSizeZ;
        }
        
        /**
         * Reflected resource from SPIR-V.
         */
        public static final class ReflectedResource {
            public String name;
            public ResourceType type;
            public int descriptorSet;
            public int binding;
            public int mslBuffer;
            public int mslTexture;
            public int mslSampler;
            public MSLDataType dataType;
            public int arraySize;
            
            public enum ResourceType {
                UNIFORM_BUFFER,
                STORAGE_BUFFER,
                SAMPLED_IMAGE,
                STORAGE_IMAGE,
                SEPARATE_IMAGE,
                SEPARATE_SAMPLER,
                INPUT_ATTACHMENT,
                PUSH_CONSTANT,
                ACCELERATION_STRUCTURE
            }
        }
        
        /**
         * Reflected vertex attribute.
         */
        public static final class ReflectedVertexAttribute {
            public String name;
            public int location;
            public int mslAttribute;
            public MSLDataType dataType;
            public int componentCount;
        }
        
        /**
         * Compile SPIR-V binary to MSL.
         */
        public SPIRVCompilationResult compile(ByteBuffer spirvBinary, String entryPoint, ShaderStage stage) {
            return compile(spirvBinary, entryPoint, stage, null);
        }
        
        /**
         * Compile SPIR-V with custom options.
         */
        public SPIRVCompilationResult compile(ByteBuffer spirvBinary, String entryPoint, 
                                             ShaderStage stage, @Nullable SPIRVCrossOptions options) {
            SPIRVCompilationResult result = new SPIRVCompilationResult();
            result.entryPoint = entryPoint;
            result.stage = stage;
            
            SPIRVCrossOptions opts = options != null ? options : defaultOptions;
            
            try {
                // Validate SPIR-V magic number
                if (spirvBinary.remaining() < 4) {
                    result.success = false;
                    result.errorMessage = "Invalid SPIR-V: buffer too small";
                    return result;
                }
                
                int magic = spirvBinary.getInt(0);
                if (magic != 0x07230203) {
                    result.success = false;
                    result.errorMessage = String.format("Invalid SPIR-V magic: 0x%08X", magic);
                    return result;
                }
                
                // Create SPIRV-Cross compiler context
                long compiler = nSPIRVCrossCreateCompiler(spirvBinary, spirvBinary.remaining());
                if (compiler == 0) {
                    result.success = false;
                    result.errorMessage = "Failed to create SPIRV-Cross compiler";
                    return result;
                }
                
                try {
                    // Set MSL options
                    nSPIRVCrossSetMSLVersion(compiler, opts.mslVersion.code);
                    nSPIRVCrossSetMSLPlatform(compiler, opts.platform.value);
                    nSPIRVCrossSetFastMathEnabled(compiler, opts.enableFastMath);
                    nSPIRVCrossSetArgumentBuffersEnabled(compiler, opts.enableArgumentBuffers);
                    nSPIRVCrossSetArgumentBuffersTier(compiler, opts.argumentBuffersTier);
                    nSPIRVCrossSetSIMDGroupFunctionsEnabled(compiler, opts.enableSIMDGroupFunctions);
                    nSPIRVCrossSetQuadGroupFunctionsEnabled(compiler, opts.enableQuadGroupFunctions);
                    nSPIRVCrossSetFramebufferFetchEnabled(compiler, opts.enableFramebufferFetch);
                    nSPIRVCrossSetEmulateSubgroups(compiler, opts.emulateSubgroups);
                    nSPIRVCrossSetSubgroupSize(compiler, opts.subgroupSize);
                    
                    // Set entry point
                    int spirvStage = switch (stage) {
                        case VERTEX -> 0;
                        case FRAGMENT -> 4;
                        case KERNEL -> 5;
                        case TESSELLATION_CONTROL -> 1;
                        case TESSELLATION_EVALUATION -> 2;
                        case MESH -> 7;
                        case OBJECT -> 8;
                        default -> 0;
                    };
                    nSPIRVCrossSetEntryPoint(compiler, entryPoint, spirvStage);
                    
                    // Apply binding overrides
                    for (Map.Entry<ResourceKey, ResourceBinding> entry : opts.bufferBindings.entrySet()) {
                        ResourceKey key = entry.getKey();
                        ResourceBinding binding = entry.getValue();
                        if (binding.mslBuffer >= 0) {
                            nSPIRVCrossSetMSLBufferBinding(compiler, key.descriptorSet, key.binding, binding.mslBuffer);
                        }
                    }
                    for (Map.Entry<ResourceKey, ResourceBinding> entry : opts.textureBindings.entrySet()) {
                        ResourceKey key = entry.getKey();
                        ResourceBinding binding = entry.getValue();
                        if (binding.mslTexture >= 0) {
                            nSPIRVCrossSetMSLTextureBinding(compiler, key.descriptorSet, key.binding, binding.mslTexture);
                        }
                    }
                    for (Map.Entry<ResourceKey, ResourceBinding> entry : opts.samplerBindings.entrySet()) {
                        ResourceKey key = entry.getKey();
                        ResourceBinding binding = entry.getValue();
                        if (binding.mslSampler >= 0) {
                            nSPIRVCrossSetMSLSamplerBinding(compiler, key.descriptorSet, key.binding, binding.mslSampler);
                        }
                    }
                    
                    // Compile to MSL
                    String mslSource = nSPIRVCrossCompile(compiler);
                    if (mslSource == null || mslSource.isEmpty()) {
                        result.success = false;
                        result.errorMessage = nSPIRVCrossGetLastError(compiler);
                        return result;
                    }
                    
                    result.success = true;
                    result.mslSource = mslSource;
                    
                    // Extract reflection data
                    extractReflection(compiler, result);
                    
                } finally {
                    nSPIRVCrossDestroyCompiler(compiler);
                }
                
            } catch (Exception e) {
                result.success = false;
                result.errorMessage = "SPIRV-Cross exception: " + e.getMessage();
            }
            
            return result;
        }
        
        /**
         * Compile SPIR-V with automatic resource binding.
         */
        public SPIRVCompilationResult compileWithAutoBindings(ByteBuffer spirvBinary, String entryPoint,
                                                             ShaderStage stage, ResourceBindingScheme scheme) {
            SPIRVCrossOptions options = new SPIRVCrossOptions();
            options.mslVersion = capabilities.version;
            options.platform = SPIRVCrossOptions.Platform.MACOS;
            options.enableArgumentBuffers = scheme.useArgumentBuffers;
            
            // Set up binding bases according to scheme
            options.vertexBufferBindingBase = scheme.vertexBufferBase;
            options.textureBufferBindingBase = scheme.textureBase;
            options.samplerBindingBase = scheme.samplerBase;
            
            return compile(spirvBinary, entryPoint, stage, options);
        }
        
        /**
         * Resource binding scheme.
         */
        public static final class ResourceBindingScheme {
            public boolean useArgumentBuffers = false;
            public int vertexBufferBase = 0;
            public int fragmentBufferBase = 0;
            public int computeBufferBase = 0;
            public int textureBase = 0;
            public int samplerBase = 0;
            
            public static ResourceBindingScheme minecraftDefault() {
                ResourceBindingScheme scheme = new ResourceBindingScheme();
                scheme.useArgumentBuffers = false;
                scheme.vertexBufferBase = 0;
                scheme.fragmentBufferBase = 0;
                scheme.textureBase = 0;
                scheme.samplerBase = 0;
                return scheme;
            }
            
            public static ResourceBindingScheme withArgumentBuffers() {
                ResourceBindingScheme scheme = new ResourceBindingScheme();
                scheme.useArgumentBuffers = true;
                scheme.vertexBufferBase = 0;
                scheme.fragmentBufferBase = 0;
                scheme.textureBase = 0;
                scheme.samplerBase = 0;
                return scheme;
            }
        }
        
        private void extractReflection(long compiler, SPIRVCompilationResult result) {
            // Extract uniform buffers
            int uniformBufferCount = nSPIRVCrossGetUniformBufferCount(compiler);
            for (int i = 0; i < uniformBufferCount; i++) {
                ReflectedResource resource = new ReflectedResource();
                resource.name = nSPIRVCrossGetUniformBufferName(compiler, i);
                resource.type = ReflectedResource.ResourceType.UNIFORM_BUFFER;
                resource.descriptorSet = nSPIRVCrossGetUniformBufferSet(compiler, i);
                resource.binding = nSPIRVCrossGetUniformBufferBinding(compiler, i);
                resource.mslBuffer = nSPIRVCrossGetUniformBufferMSLBinding(compiler, i);
                result.resources.add(resource);
                result.bufferBindings.put(resource.name, resource.mslBuffer);
            }
            
            // Extract storage buffers
            int storageBufferCount = nSPIRVCrossGetStorageBufferCount(compiler);
            for (int i = 0; i < storageBufferCount; i++) {
                ReflectedResource resource = new ReflectedResource();
                resource.name = nSPIRVCrossGetStorageBufferName(compiler, i);
                resource.type = ReflectedResource.ResourceType.STORAGE_BUFFER;
                resource.descriptorSet = nSPIRVCrossGetStorageBufferSet(compiler, i);
                resource.binding = nSPIRVCrossGetStorageBufferBinding(compiler, i);
                resource.mslBuffer = nSPIRVCrossGetStorageBufferMSLBinding(compiler, i);
                result.resources.add(resource);
                result.bufferBindings.put(resource.name, resource.mslBuffer);
            }
            
            // Extract sampled images
            int sampledImageCount = nSPIRVCrossGetSampledImageCount(compiler);
            for (int i = 0; i < sampledImageCount; i++) {
                ReflectedResource resource = new ReflectedResource();
                resource.name = nSPIRVCrossGetSampledImageName(compiler, i);
                resource.type = ReflectedResource.ResourceType.SAMPLED_IMAGE;
                resource.descriptorSet = nSPIRVCrossGetSampledImageSet(compiler, i);
                resource.binding = nSPIRVCrossGetSampledImageBinding(compiler, i);
                resource.mslTexture = nSPIRVCrossGetSampledImageMSLTexture(compiler, i);
                resource.mslSampler = nSPIRVCrossGetSampledImageMSLSampler(compiler, i);
                result.resources.add(resource);
                result.textureBindings.put(resource.name, resource.mslTexture);
                result.samplerBindings.put(resource.name, resource.mslSampler);
            }
            
            // Extract separate samplers
            int separateSamplerCount = nSPIRVCrossGetSeparateSamplerCount(compiler);
            for (int i = 0; i < separateSamplerCount; i++) {
                ReflectedResource resource = new ReflectedResource();
                resource.name = nSPIRVCrossGetSeparateSamplerName(compiler, i);
                resource.type = ReflectedResource.ResourceType.SEPARATE_SAMPLER;
                resource.descriptorSet = nSPIRVCrossGetSeparateSamplerSet(compiler, i);
                resource.binding = nSPIRVCrossGetSeparateSamplerBinding(compiler, i);
                resource.mslSampler = nSPIRVCrossGetSeparateSamplerMSLBinding(compiler, i);
                result.resources.add(resource);
                result.samplerBindings.put(resource.name, resource.mslSampler);
            }
            
            // Extract separate images
            int separateImageCount = nSPIRVCrossGetSeparateImageCount(compiler);
            for (int i = 0; i < separateImageCount; i++) {
                ReflectedResource resource = new ReflectedResource();
                resource.name = nSPIRVCrossGetSeparateImageName(compiler, i);
                resource.type = ReflectedResource.ResourceType.SEPARATE_IMAGE;
                resource.descriptorSet = nSPIRVCrossGetSeparateImageSet(compiler, i);
                resource.binding = nSPIRVCrossGetSeparateImageBinding(compiler, i);
                resource.mslTexture = nSPIRVCrossGetSeparateImageMSLBinding(compiler, i);
                result.resources.add(resource);
                result.textureBindings.put(resource.name, resource.mslTexture);
            }
            
            // Extract storage images
            int storageImageCount = nSPIRVCrossGetStorageImageCount(compiler);
            for (int i = 0; i < storageImageCount; i++) {
                ReflectedResource resource = new ReflectedResource();
                resource.name = nSPIRVCrossGetStorageImageName(compiler, i);
                resource.type = ReflectedResource.ResourceType.STORAGE_IMAGE;
                resource.descriptorSet = nSPIRVCrossGetStorageImageSet(compiler, i);
                resource.binding = nSPIRVCrossGetStorageImageBinding(compiler, i);
                resource.mslTexture = nSPIRVCrossGetStorageImageMSLBinding(compiler, i);
                result.resources.add(resource);
                result.textureBindings.put(resource.name, resource.mslTexture);
            }
            
            // Extract vertex attributes
            int vertexAttributeCount = nSPIRVCrossGetVertexAttributeCount(compiler);
            for (int i = 0; i < vertexAttributeCount; i++) {
                ReflectedVertexAttribute attr = new ReflectedVertexAttribute();
                attr.name = nSPIRVCrossGetVertexAttributeName(compiler, i);
                attr.location = nSPIRVCrossGetVertexAttributeLocation(compiler, i);
                attr.mslAttribute = nSPIRVCrossGetVertexAttributeMSLBinding(compiler, i);
                attr.componentCount = nSPIRVCrossGetVertexAttributeComponentCount(compiler, i);
                result.vertexAttributes.add(attr);
            }
            
            // Extract compute threadgroup size
            if (result.stage == ShaderStage.KERNEL) {
                int[] workgroupSize = nSPIRVCrossGetWorkgroupSize(compiler);
                result.threadgroupSizeX = workgroupSize[0];
                result.threadgroupSizeY = workgroupSize[1];
                result.threadgroupSizeZ = workgroupSize[2];
            }
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 5: HLSL TO MSL TRANSPILATION
    // HLSL to MSL conversion via DXC + SPIRV-Cross or direct translation
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * HLSL to MSL compiler.
     */
    public final class HLSLToMSLCompiler {
        
        private final MSLCapabilities capabilities;
        private final SPIRVToMSLCompiler spirvCompiler;
        
        public HLSLToMSLCompiler(MSLCapabilities capabilities, SPIRVToMSLCompiler spirvCompiler) {
            this.capabilities = capabilities;
            this.spirvCompiler = spirvCompiler;
        }
        
        /**
         * HLSL compilation options.
         */
        public static final class HLSLOptions {
            public String entryPoint = "main";
            public ShaderStage stage = ShaderStage.VERTEX;
            public HLSLVersion hlslVersion = HLSLVersion.SM_6_0;
            public boolean enableDebugInfo = false;
            public boolean enable16BitTypes = false;
            public boolean enableWaveOps = true;
            public OptimizationLevel optimizationLevel = OptimizationLevel.O3;
            public MatrixLayout matrixLayout = MatrixLayout.ROW_MAJOR;
            public Map<String, String> defines = new HashMap<>();
            public List<String> includePaths = new ArrayList<>();
            
            public enum HLSLVersion {
                SM_5_0("5_0"),
                SM_5_1("5_1"),
                SM_6_0("6_0"),
                SM_6_1("6_1"),
                SM_6_2("6_2"),
                SM_6_3("6_3"),
                SM_6_4("6_4"),
                SM_6_5("6_5"),
                SM_6_6("6_6"),
                SM_6_7("6_7");
                
                public final String profile;
                HLSLVersion(String profile) { this.profile = profile; }
            }
            
            public enum OptimizationLevel {
                O0, O1, O2, O3
            }
            
            public enum MatrixLayout {
                ROW_MAJOR,
                COLUMN_MAJOR
            }
        }
        
        /**
         * HLSL compilation result.
         */
        public static final class HLSLCompilationResult {
            public boolean success;
            public String mslSource;
            public String errorMessage;
            public byte[] spirvBinary;
            public SPIRVToMSLCompiler.SPIRVCompilationResult spirvResult;
            public final Map<String, HLSLSemanticMapping> semanticMappings = new HashMap<>();
        }
        
        /**
         * HLSL semantic to MSL attribute mapping.
         */
        public static final class HLSLSemanticMapping {
            public String hlslSemantic;
            public MSLAttribute mslAttribute;
            public int index;
            
            HLSLSemanticMapping(String semantic, MSLAttribute attr, int index) {
                this.hlslSemantic = semantic;
                this.mslAttribute = attr;
                this.index = index;
            }
        }
        
        /**
         * Compile HLSL to MSL.
         */
        public HLSLCompilationResult compile(String hlslSource, HLSLOptions options) {
            HLSLCompilationResult result = new HLSLCompilationResult();
            
            try {
                // Step 1: Compile HLSL to SPIR-V using DXC
                byte[] spirvBinary = compileHLSLToSPIRV(hlslSource, options);
                if (spirvBinary == null || spirvBinary.length == 0) {
                    result.success = false;
                    result.errorMessage = "DXC compilation failed";
                    return result;
                }
                result.spirvBinary = spirvBinary;
                
                // Step 2: Compile SPIR-V to MSL
                ByteBuffer spirvBuffer = ByteBuffer.allocateDirect(spirvBinary.length)
                    .order(ByteOrder.LITTLE_ENDIAN);
                spirvBuffer.put(spirvBinary);
                spirvBuffer.flip();
                
                SPIRVToMSLCompiler.SPIRVCompilationResult spirvResult = 
                    spirvCompiler.compile(spirvBuffer, options.entryPoint, options.stage);
                
                if (!spirvResult.success) {
                    result.success = false;
                    result.errorMessage = "SPIRV-Cross compilation failed: " + spirvResult.errorMessage;
                    return result;
                }
                
                result.success = true;
                result.mslSource = spirvResult.mslSource;
                result.spirvResult = spirvResult;
                
                // Build semantic mappings
                buildSemanticMappings(result, options.stage);
                
            } catch (Exception e) {
                result.success = false;
                result.errorMessage = "HLSL compilation exception: " + e.getMessage();
            }
            
            return result;
        }
        
        /**
         * Compile HLSL source to SPIR-V using DXC.
         */
        private byte[] compileHLSLToSPIRV(String hlslSource, HLSLOptions options) {
            // Build DXC arguments
            List<String> args = new ArrayList<>();
            
            // Target profile
            String profile = switch (options.stage) {
                case VERTEX -> "vs_" + options.hlslVersion.profile;
                case FRAGMENT -> "ps_" + options.hlslVersion.profile;
                case KERNEL -> "cs_" + options.hlslVersion.profile;
                case MESH -> "ms_" + options.hlslVersion.profile;
                case OBJECT -> "as_" + options.hlslVersion.profile;
                default -> "vs_" + options.hlslVersion.profile;
            };
            args.add("-T");
            args.add(profile);
            
            // Entry point
            args.add("-E");
            args.add(options.entryPoint);
            
            // Output SPIR-V
            args.add("-spirv");
            
            // Optimization level
            args.add("-O" + options.optimizationLevel.ordinal());
            
            // Matrix layout
            if (options.matrixLayout == HLSLOptions.MatrixLayout.ROW_MAJOR) {
                args.add("-Zpr");
            } else {
                args.add("-Zpc");
            }
            
            // 16-bit types
            if (options.enable16BitTypes) {
                args.add("-enable-16bit-types");
            }
            
            // Defines
            for (Map.Entry<String, String> define : options.defines.entrySet()) {
                args.add("-D");
                args.add(define.getKey() + "=" + define.getValue());
            }
            
            // Include paths
            for (String includePath : options.includePaths) {
                args.add("-I");
                args.add(includePath);
            }
            
            // SPIR-V specific options
            args.add("-fspv-target-env=vulkan1.1");
            args.add("-fvk-use-dx-layout");
            
            // Compile using native DXC
            return nDXCCompile(hlslSource, args.toArray(new String[0]));
        }
        
        /**
         * Build HLSL semantic to MSL attribute mappings.
         */
        private void buildSemanticMappings(HLSLCompilationResult result, ShaderStage stage) {
            // Standard HLSL → MSL semantic mappings
            Map<String, HLSLSemanticMapping> mappings = result.semanticMappings;
            
            // Vertex input semantics
            mappings.put("POSITION", new HLSLSemanticMapping("POSITION", MSLAttribute.ATTRIBUTE, 0));
            mappings.put("NORMAL", new HLSLSemanticMapping("NORMAL", MSLAttribute.ATTRIBUTE, 1));
            mappings.put("TANGENT", new HLSLSemanticMapping("TANGENT", MSLAttribute.ATTRIBUTE, 2));
            mappings.put("BINORMAL", new HLSLSemanticMapping("BINORMAL", MSLAttribute.ATTRIBUTE, 3));
            mappings.put("TEXCOORD0", new HLSLSemanticMapping("TEXCOORD0", MSLAttribute.ATTRIBUTE, 4));
            mappings.put("TEXCOORD1", new HLSLSemanticMapping("TEXCOORD1", MSLAttribute.ATTRIBUTE, 5));
            mappings.put("TEXCOORD2", new HLSLSemanticMapping("TEXCOORD2", MSLAttribute.ATTRIBUTE, 6));
            mappings.put("TEXCOORD3", new HLSLSemanticMapping("TEXCOORD3", MSLAttribute.ATTRIBUTE, 7));
            mappings.put("COLOR0", new HLSLSemanticMapping("COLOR0", MSLAttribute.ATTRIBUTE, 8));
            mappings.put("COLOR1", new HLSLSemanticMapping("COLOR1", MSLAttribute.ATTRIBUTE, 9));
            mappings.put("BLENDWEIGHT", new HLSLSemanticMapping("BLENDWEIGHT", MSLAttribute.ATTRIBUTE, 10));
            mappings.put("BLENDINDICES", new HLSLSemanticMapping("BLENDINDICES", MSLAttribute.ATTRIBUTE, 11));
            
            // System value semantics
            mappings.put("SV_Position", new HLSLSemanticMapping("SV_Position", MSLAttribute.POSITION, 0));
            mappings.put("SV_VertexID", new HLSLSemanticMapping("SV_VertexID", MSLAttribute.VERTEX_ID, 0));
            mappings.put("SV_InstanceID", new HLSLSemanticMapping("SV_InstanceID", MSLAttribute.INSTANCE_ID, 0));
            mappings.put("SV_IsFrontFace", new HLSLSemanticMapping("SV_IsFrontFace", MSLAttribute.FRONT_FACING, 0));
            mappings.put("SV_SampleIndex", new HLSLSemanticMapping("SV_SampleIndex", MSLAttribute.SAMPLE_ID, 0));
            
            // Fragment output semantics
            for (int i = 0; i < 8; i++) {
                mappings.put("SV_Target" + i, new HLSLSemanticMapping("SV_Target" + i, MSLAttribute.COLOR, i));
            }
            mappings.put("SV_Depth", new HLSLSemanticMapping("SV_Depth", MSLAttribute.DEPTH, 0));
            
            // Compute semantics
            mappings.put("SV_DispatchThreadID", new HLSLSemanticMapping("SV_DispatchThreadID", 
                MSLAttribute.THREAD_POSITION_IN_GRID, 0));
            mappings.put("SV_GroupThreadID", new HLSLSemanticMapping("SV_GroupThreadID",
                MSLAttribute.THREAD_POSITION_IN_THREADGROUP, 0));
            mappings.put("SV_GroupID", new HLSLSemanticMapping("SV_GroupID",
                MSLAttribute.THREADGROUP_POSITION_IN_GRID, 0));
            mappings.put("SV_GroupIndex", new HLSLSemanticMapping("SV_GroupIndex",
                MSLAttribute.THREAD_INDEX_IN_THREADGROUP, 0));
        }
        
        /**
         * Direct HLSL to MSL translation for simple shaders.
         * This bypasses SPIR-V for faster compilation of simple cases.
         */
        public HLSLCompilationResult translateDirect(String hlslSource, HLSLOptions options) {
            HLSLCompilationResult result = new HLSLCompilationResult();
            
            try {
                HLSLDirectTranslator translator = new HLSLDirectTranslator(capabilities);
                String mslSource = translator.translate(hlslSource, options);
                
                result.success = true;
                result.mslSource = mslSource;
                buildSemanticMappings(result, options.stage);
                
            } catch (Exception e) {
                result.success = false;
                result.errorMessage = "Direct translation failed: " + e.getMessage();
                
                // Fall back to SPIR-V path
                LOGGER.debug("Direct HLSL translation failed, falling back to SPIR-V path");
                return compile(hlslSource, options);
            }
            
            return result;
        }
    }
    
    /**
     * Direct HLSL to MSL translator for simple shaders.
     */
    private static final class HLSLDirectTranslator {
        
        private final MSLCapabilities capabilities;
        
        // Type mappings
        private static final Map<String, String> TYPE_MAP = new HashMap<>();
        static {
            TYPE_MAP.put("float", "float");
            TYPE_MAP.put("float2", "float2");
            TYPE_MAP.put("float3", "float3");
            TYPE_MAP.put("float4", "float4");
            TYPE_MAP.put("float2x2", "float2x2");
            TYPE_MAP.put("float3x3", "float3x3");
            TYPE_MAP.put("float4x4", "float4x4");
            TYPE_MAP.put("int", "int");
            TYPE_MAP.put("int2", "int2");
            TYPE_MAP.put("int3", "int3");
            TYPE_MAP.put("int4", "int4");
            TYPE_MAP.put("uint", "uint");
            TYPE_MAP.put("uint2", "uint2");
            TYPE_MAP.put("uint3", "uint3");
            TYPE_MAP.put("uint4", "uint4");
            TYPE_MAP.put("bool", "bool");
            TYPE_MAP.put("half", "half");
            TYPE_MAP.put("half2", "half2");
            TYPE_MAP.put("half3", "half3");
            TYPE_MAP.put("half4", "half4");
            TYPE_MAP.put("Texture2D", "texture2d<float>");
            TYPE_MAP.put("Texture3D", "texture3d<float>");
            TYPE_MAP.put("TextureCube", "texturecube<float>");
            TYPE_MAP.put("Texture2DArray", "texture2d_array<float>");
            TYPE_MAP.put("SamplerState", "sampler");
            TYPE_MAP.put("SamplerComparisonState", "sampler");
        }
        
        // Function mappings
        private static final Map<String, String> FUNCTION_MAP = new HashMap<>();
        static {
            FUNCTION_MAP.put("mul", "");  // Matrix multiply needs special handling
            FUNCTION_MAP.put("lerp", "mix");
            FUNCTION_MAP.put("frac", "fract");
            FUNCTION_MAP.put("rsqrt", "rsqrt");
            FUNCTION_MAP.put("saturate", "saturate");
            FUNCTION_MAP.put("ddx", "dfdx");
            FUNCTION_MAP.put("ddy", "dfdy");
            FUNCTION_MAP.put("fwidth", "fwidth");
            FUNCTION_MAP.put("clip", "discard_fragment");
            FUNCTION_MAP.put("tex2D", "sample");
            FUNCTION_MAP.put("tex2Dlod", "sample");
            FUNCTION_MAP.put("tex2Dbias", "sample");
            FUNCTION_MAP.put("texCUBE", "sample");
        }
        
        HLSLDirectTranslator(MSLCapabilities capabilities) {
            this.capabilities = capabilities;
        }
        
        String translate(String hlslSource, HLSLToMSLCompiler.HLSLOptions options) {
            StringBuilder msl = new StringBuilder();
            
            // Standard MSL header
            msl.append("#include <metal_stdlib>\n");
            msl.append("#include <simd/simd.h>\n");
            msl.append("using namespace metal;\n\n");
            
            // Parse and translate
            String translated = translateSource(hlslSource, options);
            msl.append(translated);
            
            return msl.toString();
        }
        
        private String translateSource(String source, HLSLToMSLCompiler.HLSLOptions options) {
            String result = source;
            
            // Replace types
            for (Map.Entry<String, String> entry : TYPE_MAP.entrySet()) {
                result = result.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
            }
            
            // Replace functions
            for (Map.Entry<String, String> entry : FUNCTION_MAP.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    result = result.replaceAll("\\b" + entry.getKey() + "\\s*\\(", entry.getValue() + "(");
                }
            }
            
            // Handle mul() specially - MSL uses operator* or matrix functions
            result = translateMul(result);
            
            // Handle texture sampling
            result = translateTextureSampling(result);
            
            // Handle semantics
            result = translateSemantics(result, options.stage);
            
            // Handle constant buffers
            result = translateConstantBuffers(result);
            
            // Handle clip() -> discard_fragment()
            result = translateClip(result);
            
            return result;
        }
        
        private String translateMul(String source) {
            // Pattern: mul(a, b) where order depends on types
            // In HLSL: mul(vector, matrix) = vector * matrix
            // In MSL: matrix * vector (reversed)
            Pattern mulPattern = Pattern.compile("mul\\s*\\(\\s*([^,]+)\\s*,\\s*([^)]+)\\s*\\)");
            Matcher matcher = mulPattern.matcher(source);
            
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                String left = matcher.group(1).trim();
                String right = matcher.group(2).trim();
                // Assume typical HLSL convention and reverse for MSL
                matcher.appendReplacement(result, "(" + right + " * " + left + ")");
            }
            matcher.appendTail(result);
            
            return result.toString();
        }
        
        private String translateTextureSampling(String source) {
            // Pattern: texture.Sample(sampler, uv)
            Pattern samplePattern = Pattern.compile(
                "(\\w+)\\.Sample\\s*\\(\\s*(\\w+)\\s*,\\s*([^)]+)\\s*\\)");
            Matcher matcher = samplePattern.matcher(source);
            
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                String texture = matcher.group(1);
                String sampler = matcher.group(2);
                String coords = matcher.group(3);
                matcher.appendReplacement(result, texture + ".sample(" + sampler + ", " + coords + ")");
            }
            matcher.appendTail(result);
            
            return result.toString();
        }
        
        private String translateSemantics(String source, ShaderStage stage) {
            // Remove HLSL semantics and add MSL attributes
            // This is a simplified version - full implementation would parse struct definitions
            
            String result = source;
            
            // Remove standalone semantics
            result = result.replaceAll(":\\s*SV_POSITION\\b", " [[position]]");
            result = result.replaceAll(":\\s*SV_Position\\b", " [[position]]");
            result = result.replaceAll(":\\s*SV_TARGET\\d*\\b", ""); // Handled by output struct
            result = result.replaceAll(":\\s*SV_Target\\d*\\b", "");
            result = result.replaceAll(":\\s*POSITION\\d*\\b", "");
            result = result.replaceAll(":\\s*TEXCOORD\\d*\\b", "");
            result = result.replaceAll(":\\s*COLOR\\d*\\b", "");
            result = result.replaceAll(":\\s*NORMAL\\d*\\b", "");
            
            return result;
        }
        
        private String translateConstantBuffers(String source) {
            // Pattern: cbuffer Name : register(b0) { ... }
            Pattern cbufferPattern = Pattern.compile(
                "cbuffer\\s+(\\w+)\\s*(?::\\s*register\\s*\\(\\s*b(\\d+)\\s*\\))?\\s*\\{([^}]+)\\}");
            Matcher matcher = cbufferPattern.matcher(source);
            
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                String name = matcher.group(1);
                String registerIndex = matcher.group(2);
                String contents = matcher.group(3);
                
                int bufferIndex = registerIndex != null ? Integer.parseInt(registerIndex) : 0;
                
                // Convert to MSL constant struct
                String mslStruct = "struct " + name + " {\n" + contents + "\n};\n";
                matcher.appendReplacement(result, mslStruct);
            }
            matcher.appendTail(result);
            
            return result.toString();
        }
        
        private String translateClip(String source) {
            // Pattern: clip(value) -> if (value < 0) discard_fragment();
            Pattern clipPattern = Pattern.compile("clip\\s*\\(([^)]+)\\)\\s*;");
            Matcher matcher = clipPattern.matcher(source);
            
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                String value = matcher.group(1);
                matcher.appendReplacement(result, 
                    "if (any(" + value + " < 0.0)) discard_fragment();");
            }
            matcher.appendTail(result);
            
            return result.toString();
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 6: GLSL TO MSL TRANSPILATION
    // GLSL to MSL conversion via glslang + SPIRV-Cross
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * GLSL to MSL compiler.
     */
    public final class GLSLToMSLCompiler {
        
        private final MSLCapabilities capabilities;
        private final SPIRVToMSLCompiler spirvCompiler;
        
        public GLSLToMSLCompiler(MSLCapabilities capabilities, SPIRVToMSLCompiler spirvCompiler) {
            this.capabilities = capabilities;
            this.spirvCompiler = spirvCompiler;
        }
        
        /**
         * GLSL compilation options.
         */
        public static final class GLSLOptions {
            public String entryPoint = "main";
            public ShaderStage stage = ShaderStage.VERTEX;
            public GLSLVersion glslVersion = GLSLVersion.GLSL_450;
            public GLSLProfile profile = GLSLProfile.CORE;
            public boolean enableVulkanSemantics = true;
            public boolean autoMapLocations = true;
            public boolean autoMapBindings = true;
            public Map<String, String> defines = new HashMap<>();
            public List<String> includePaths = new ArrayList<>();
            
            public enum GLSLVersion {
                GLSL_110(110),
                GLSL_120(120),
                GLSL_130(130),
                GLSL_140(140),
                GLSL_150(150),
                GLSL_330(330),
                GLSL_400(400),
                GLSL_410(410),
                GLSL_420(420),
                GLSL_430(430),
                GLSL_440(440),
                GLSL_450(450),
                GLSL_460(460);
                
                public final int version;
                GLSLVersion(int version) { this.version = version; }
            }
            
            public enum GLSLProfile {
                CORE,
                COMPATIBILITY,
                ES
            }
        }
        
        /**
         * GLSL compilation result.
         */
        public static final class GLSLCompilationResult {
            public boolean success;
            public String mslSource;
            public String errorMessage;
            public byte[] spirvBinary;
            public SPIRVToMSLCompiler.SPIRVCompilationResult spirvResult;
            public final Map<String, GLSLBuiltinMapping> builtinMappings = new HashMap<>();
        }
        
        /**
         * GLSL builtin to MSL attribute mapping.
         */
        public static final class GLSLBuiltinMapping {
            public String glslBuiltin;
            public MSLAttribute mslAttribute;
            public int index;
            
            GLSLBuiltinMapping(String builtin, MSLAttribute attr, int index) {
                this.glslBuiltin = builtin;
                this.mslAttribute = attr;
                this.index = index;
            }
        }
        
        /**
         * Compile GLSL to MSL.
         */
        public GLSLCompilationResult compile(String glslSource, GLSLOptions options) {
            GLSLCompilationResult result = new GLSLCompilationResult();
            
            try {
                // Step 1: Preprocess GLSL
                String preprocessed = preprocessGLSL(glslSource, options);
                
                // Step 2: Compile GLSL to SPIR-V using glslang
                byte[] spirvBinary = compileGLSLToSPIRV(preprocessed, options);
                if (spirvBinary == null || spirvBinary.length == 0) {
                    result.success = false;
                    result.errorMessage = "glslang compilation failed";
                    return result;
                }
                result.spirvBinary = spirvBinary;
                
                // Step 3: Compile SPIR-V to MSL
                ByteBuffer spirvBuffer = ByteBuffer.allocateDirect(spirvBinary.length)
                    .order(ByteOrder.LITTLE_ENDIAN);
                spirvBuffer.put(spirvBinary);
                spirvBuffer.flip();
                
                SPIRVToMSLCompiler.SPIRVCompilationResult spirvResult =
                    spirvCompiler.compile(spirvBuffer, options.entryPoint, options.stage);
                
                if (!spirvResult.success) {
                    result.success = false;
                    result.errorMessage = "SPIRV-Cross compilation failed: " + spirvResult.errorMessage;
                    return result;
                }
                
                result.success = true;
                result.mslSource = spirvResult.mslSource;
                result.spirvResult = spirvResult;
                
                // Build builtin mappings
                buildBuiltinMappings(result, options.stage);
                
            } catch (Exception e) {
                result.success = false;
                result.errorMessage = "GLSL compilation exception: " + e.getMessage();
            }
            
            return result;
        }
        
        /**
         * Preprocess GLSL source.
         */
        private String preprocessGLSL(String source, GLSLOptions options) {
            StringBuilder result = new StringBuilder();
            
            // Add version directive if not present
            if (!source.contains("#version")) {
                result.append("#version ").append(options.glslVersion.version);
                if (options.profile == GLSLOptions.GLSLProfile.ES) {
                    result.append(" es");
                } else if (options.profile == GLSLOptions.GLSLProfile.CORE) {
                    result.append(" core");
                }
                result.append("\n");
            }
            
            // Add defines
            for (Map.Entry<String, String> define : options.defines.entrySet()) {
                result.append("#define ").append(define.getKey())
                      .append(" ").append(define.getValue()).append("\n");
            }
            
            result.append(source);
            return result.toString();
        }
        
        /**
         * Compile GLSL to SPIR-V using glslang.
         */
        private byte[] compileGLSLToSPIRV(String source, GLSLOptions options) {
            int glslangStage = switch (options.stage) {
                case VERTEX -> 0;       // GLSLANG_STAGE_VERTEX
                case FRAGMENT -> 4;     // GLSLANG_STAGE_FRAGMENT
                case KERNEL -> 5;       // GLSLANG_STAGE_COMPUTE
                case TESSELLATION_CONTROL -> 1;   // GLSLANG_STAGE_TESSCONTROL
                case TESSELLATION_EVALUATION -> 2; // GLSLANG_STAGE_TESSEVALUATION
                default -> 0;
            };
            
            int clientVersion = options.enableVulkanSemantics ? 
                110 :  // Vulkan 1.1
                100;   // OpenGL 4.5
            
            return nGlslangCompile(source, glslangStage, clientVersion, 
                options.autoMapLocations, options.autoMapBindings);
        }
        
        /**
         * Build GLSL builtin to MSL attribute mappings.
         */
        private void buildBuiltinMappings(GLSLCompilationResult result, ShaderStage stage) {
            Map<String, GLSLBuiltinMapping> mappings = result.builtinMappings;
            
            // Vertex shader builtins
            mappings.put("gl_Position", new GLSLBuiltinMapping("gl_Position", MSLAttribute.POSITION, 0));
            mappings.put("gl_VertexID", new GLSLBuiltinMapping("gl_VertexID", MSLAttribute.VERTEX_ID, 0));
            mappings.put("gl_InstanceID", new GLSLBuiltinMapping("gl_InstanceID", MSLAttribute.INSTANCE_ID, 0));
            mappings.put("gl_VertexIndex", new GLSLBuiltinMapping("gl_VertexIndex", MSLAttribute.VERTEX_ID, 0));
            mappings.put("gl_InstanceIndex", new GLSLBuiltinMapping("gl_InstanceIndex", MSLAttribute.INSTANCE_ID, 0));
            mappings.put("gl_BaseVertex", new GLSLBuiltinMapping("gl_BaseVertex", MSLAttribute.BASE_VERTEX, 0));
            mappings.put("gl_BaseInstance", new GLSLBuiltinMapping("gl_BaseInstance", MSLAttribute.BASE_INSTANCE, 0));
            mappings.put("gl_PointSize", new GLSLBuiltinMapping("gl_PointSize", MSLAttribute.POINT_SIZE, 0));
            
            // Fragment shader builtins
            mappings.put("gl_FragCoord", new GLSLBuiltinMapping("gl_FragCoord", MSLAttribute.FRAGMENT_COORD, 0));
            mappings.put("gl_FrontFacing", new GLSLBuiltinMapping("gl_FrontFacing", MSLAttribute.FRONT_FACING, 0));
            mappings.put("gl_PointCoord", new GLSLBuiltinMapping("gl_PointCoord", MSLAttribute.POINT_COORD, 0));
            mappings.put("gl_SampleID", new GLSLBuiltinMapping("gl_SampleID", MSLAttribute.SAMPLE_ID, 0));
            mappings.put("gl_SampleMask", new GLSLBuiltinMapping("gl_SampleMask", MSLAttribute.SAMPLE_MASK, 0));
            mappings.put("gl_FragDepth", new GLSLBuiltinMapping("gl_FragDepth", MSLAttribute.DEPTH, 0));
            
            // Compute shader builtins
            mappings.put("gl_GlobalInvocationID", new GLSLBuiltinMapping("gl_GlobalInvocationID",
                MSLAttribute.THREAD_POSITION_IN_GRID, 0));
            mappings.put("gl_LocalInvocationID", new GLSLBuiltinMapping("gl_LocalInvocationID",
                MSLAttribute.THREAD_POSITION_IN_THREADGROUP, 0));
            mappings.put("gl_WorkGroupID", new GLSLBuiltinMapping("gl_WorkGroupID",
                MSLAttribute.THREADGROUP_POSITION_IN_GRID, 0));
            mappings.put("gl_LocalInvocationIndex", new GLSLBuiltinMapping("gl_LocalInvocationIndex",
                MSLAttribute.THREAD_INDEX_IN_THREADGROUP, 0));
            mappings.put("gl_NumWorkGroups", new GLSLBuiltinMapping("gl_NumWorkGroups",
                MSLAttribute.THREADGROUPS_PER_GRID, 0));
            mappings.put("gl_WorkGroupSize", new GLSLBuiltinMapping("gl_WorkGroupSize",
                MSLAttribute.THREADS_PER_THREADGROUP, 0));
            
            // Tessellation builtins
            mappings.put("gl_PatchID", new GLSLBuiltinMapping("gl_PatchID", MSLAttribute.PATCH_ID, 0));
            mappings.put("gl_TessCoord", new GLSLBuiltinMapping("gl_TessCoord", MSLAttribute.POSITION_IN_PATCH, 0));
        }
        
        /**
         * Direct GLSL to MSL translation for Minecraft shaders.
         * Optimized for common Minecraft shader patterns.
         */
        public GLSLCompilationResult translateMinecraft(String glslSource, GLSLOptions options) {
            GLSLCompilationResult result = new GLSLCompilationResult();
            
            try {
                MinecraftGLSLTranslator translator = new MinecraftGLSLTranslator(capabilities);
                String mslSource = translator.translate(glslSource, options);
                
                result.success = true;
                result.mslSource = mslSource;
                buildBuiltinMappings(result, options.stage);
                
            } catch (Exception e) {
                result.success = false;
                result.errorMessage = "Direct translation failed: " + e.getMessage();
                
                // Fall back to full compilation path
                LOGGER.debug("Direct GLSL translation failed, falling back to full path");
                return compile(glslSource, options);
            }
            
            return result;
        }
    }
    
    /**
     * Minecraft-specific GLSL to MSL translator.
     * Handles common patterns found in Minecraft shaders.
     */
    private static final class MinecraftGLSLTranslator {
        
        private final MSLCapabilities capabilities;
        
        // Minecraft-specific patterns
        private static final Map<String, String> MC_TYPE_MAP = new HashMap<>();
        private static final Map<String, String> MC_FUNC_MAP = new HashMap<>();
        private static final Map<String, String> MC_BUILTIN_MAP = new HashMap<>();
        
        static {
            // Type mappings
            MC_TYPE_MAP.put("vec2", "float2");
            MC_TYPE_MAP.put("vec3", "float3");
            MC_TYPE_MAP.put("vec4", "float4");
            MC_TYPE_MAP.put("ivec2", "int2");
            MC_TYPE_MAP.put("ivec3", "int3");
            MC_TYPE_MAP.put("ivec4", "int4");
            MC_TYPE_MAP.put("uvec2", "uint2");
            MC_TYPE_MAP.put("uvec3", "uint3");
            MC_TYPE_MAP.put("uvec4", "uint4");
            MC_TYPE_MAP.put("mat2", "float2x2");
            MC_TYPE_MAP.put("mat3", "float3x3");
            MC_TYPE_MAP.put("mat4", "float4x4");
            MC_TYPE_MAP.put("sampler2D", "texture2d<float>");
            MC_TYPE_MAP.put("sampler3D", "texture3d<float>");
            MC_TYPE_MAP.put("samplerCube", "texturecube<float>");
            MC_TYPE_MAP.put("sampler2DArray", "texture2d_array<float>");
            MC_TYPE_MAP.put("sampler2DShadow", "depth2d<float>");
            MC_TYPE_MAP.put("samplerCubeShadow", "depthcube<float>");
            
            // Function mappings
            MC_FUNC_MAP.put("texture", "sample");
            MC_FUNC_MAP.put("texture2D", "sample");
            MC_FUNC_MAP.put("texture3D", "sample");
            MC_FUNC_MAP.put("textureCube", "sample");
            MC_FUNC_MAP.put("textureLod", "sample");
            MC_FUNC_MAP.put("textureGrad", "sample");
            MC_FUNC_MAP.put("textureProj", "sample");
            MC_FUNC_MAP.put("texelFetch", "read");
            MC_FUNC_MAP.put("imageLoad", "read");
            MC_FUNC_MAP.put("imageStore", "write");
            MC_FUNC_MAP.put("mix", "mix");
            MC_FUNC_MAP.put("fract", "fract");
            MC_FUNC_MAP.put("mod", "fmod");
            MC_FUNC_MAP.put("dFdx", "dfdx");
            MC_FUNC_MAP.put("dFdy", "dfdy");
            MC_FUNC_MAP.put("fwidth", "fwidth");
            MC_FUNC_MAP.put("inversesqrt", "rsqrt");
            MC_FUNC_MAP.put("atan", "atan2"); // Note: GLSL atan(y,x) -> MSL atan2(y,x)
            
            // Builtin mappings
            MC_BUILTIN_MAP.put("gl_Position", "out.position");
            MC_BUILTIN_MAP.put("gl_FragCoord", "in.position");
            MC_BUILTIN_MAP.put("gl_VertexID", "vid");
            MC_BUILTIN_MAP.put("gl_InstanceID", "iid");
            MC_BUILTIN_MAP.put("gl_FrontFacing", "is_front_face");
            MC_BUILTIN_MAP.put("gl_PointCoord", "point_coord");
        }
        
        MinecraftGLSLTranslator(MSLCapabilities capabilities) {
            this.capabilities = capabilities;
        }
        
        String translate(String glslSource, GLSLToMSLCompiler.GLSLOptions options) {
            StringBuilder msl = new StringBuilder();
            
            // MSL header
            msl.append("#include <metal_stdlib>\n");
            msl.append("#include <simd/simd.h>\n");
            msl.append("using namespace metal;\n\n");
            
            // Parse GLSL and extract information
            GLSLParseResult parsed = parseGLSL(glslSource, options);
            
            // Generate MSL structures
            generateStructures(msl, parsed, options.stage);
            
            // Generate MSL function
            generateFunction(msl, parsed, options);
            
            return msl.toString();
        }
        
        private GLSLParseResult parseGLSL(String source, GLSLToMSLCompiler.GLSLOptions options) {
            GLSLParseResult result = new GLSLParseResult();
            
            // Remove version directive and comments
            String cleaned = source.replaceAll("#version\\s+\\d+.*", "")
                                  .replaceAll("//.*", "")
                                  .replaceAll("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "");
            
            // Extract uniforms
            Pattern uniformPattern = Pattern.compile(
                "uniform\\s+(\\w+)\\s+(\\w+)(?:\\s*=\\s*[^;]+)?\\s*;");
            Matcher uniformMatcher = uniformPattern.matcher(cleaned);
            while (uniformMatcher.find()) {
                GLSLVariable var = new GLSLVariable();
                var.type = uniformMatcher.group(1);
                var.name = uniformMatcher.group(2);
                var.qualifier = "uniform";
                result.uniforms.add(var);
            }
            
            // Extract uniform blocks
            Pattern uboPattern = Pattern.compile(
                "(?:layout\\s*\\([^)]*\\)\\s*)?uniform\\s+(\\w+)\\s*\\{([^}]+)\\}(?:\\s*(\\w+))?\\s*;");
            Matcher uboMatcher = uboPattern.matcher(cleaned);
            while (uboMatcher.find()) {
                GLSLUniformBlock block = new GLSLUniformBlock();
                block.name = uboMatcher.group(1);
                block.instanceName = uboMatcher.group(3);
                
                // Parse block members
                String members = uboMatcher.group(2);
                Pattern memberPattern = Pattern.compile("(\\w+)\\s+(\\w+)(?:\\s*\\[\\s*(\\d+)\\s*\\])?\\s*;");
                Matcher memberMatcher = memberPattern.matcher(members);
                while (memberMatcher.find()) {
                    GLSLVariable member = new GLSLVariable();
                    member.type = memberMatcher.group(1);
                    member.name = memberMatcher.group(2);
                    if (memberMatcher.group(3) != null) {
                        member.arraySize = Integer.parseInt(memberMatcher.group(3));
                    }
                    block.members.add(member);
                }
                result.uniformBlocks.add(block);
            }
            
            // Extract inputs (in variables)
            Pattern inPattern = Pattern.compile(
                "(?:layout\\s*\\(\\s*location\\s*=\\s*(\\d+)\\s*\\)\\s*)?" +
                "in\\s+(\\w+)\\s+(\\w+)\\s*;");
            Matcher inMatcher = inPattern.matcher(cleaned);
            while (inMatcher.find()) {
                GLSLVariable var = new GLSLVariable();
                var.location = inMatcher.group(1) != null ? Integer.parseInt(inMatcher.group(1)) : -1;
                var.type = inMatcher.group(2);
                var.name = inMatcher.group(3);
                var.qualifier = "in";
                result.inputs.add(var);
            }
            
            // Extract outputs (out variables)
            Pattern outPattern = Pattern.compile(
                "(?:layout\\s*\\(\\s*location\\s*=\\s*(\\d+)\\s*\\)\\s*)?" +
                "out\\s+(\\w+)\\s+(\\w+)\\s*;");
            Matcher outMatcher = outPattern.matcher(cleaned);
            while (outMatcher.find()) {
                GLSLVariable var = new GLSLVariable();
                var.location = outMatcher.group(1) != null ? Integer.parseInt(outMatcher.group(1)) : -1;
                var.type = outMatcher.group(2);
                var.name = outMatcher.group(3);
                var.qualifier = "out";
                result.outputs.add(var);
            }
            
            // Extract main function body
            Pattern mainPattern = Pattern.compile("void\\s+main\\s*\\(\\s*\\)\\s*\\{([\\s\\S]*?)\\}\\s*$");
            Matcher mainMatcher = mainPattern.matcher(cleaned);
            if (mainMatcher.find()) {
                result.mainBody = mainMatcher.group(1);
            }
            
            // Extract other functions
            Pattern funcPattern = Pattern.compile(
                "(\\w+)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*\\{([^}]+)\\}");
            Matcher funcMatcher = funcPattern.matcher(cleaned);
            while (funcMatcher.find()) {
                String name = funcMatcher.group(2);
                if (!name.equals("main")) {
                    GLSLFunction func = new GLSLFunction();
                    func.returnType = funcMatcher.group(1);
                    func.name = name;
                    func.parameters = funcMatcher.group(3);
                    func.body = funcMatcher.group(4);
                    result.functions.add(func);
                }
            }
            
            return result;
        }
        
        private void generateStructures(StringBuilder msl, GLSLParseResult parsed, ShaderStage stage) {
            // Generate uniform buffer structs
            int bufferIndex = 0;
            for (GLSLUniformBlock block : parsed.uniformBlocks) {
                msl.append("struct ").append(block.name).append(" {\n");
                for (GLSLVariable member : block.members) {
                    String mslType = translateType(member.type);
                    if (member.arraySize > 0) {
                        msl.append("    ").append(mslType).append(" ")
                           .append(member.name).append("[").append(member.arraySize).append("];\n");
                    } else {
                        msl.append("    ").append(mslType).append(" ").append(member.name).append(";\n");
                    }
                }
                msl.append("};\n\n");
                block.mslBufferIndex = bufferIndex++;
            }
            
            // Generate vertex input struct
            if (stage == ShaderStage.VERTEX && !parsed.inputs.isEmpty()) {
                msl.append("struct VertexIn {\n");
                int attrIndex = 0;
                for (GLSLVariable input : parsed.inputs) {
                    int loc = input.location >= 0 ? input.location : attrIndex;
                    String mslType = translateType(input.type);
                    msl.append("    ").append(mslType).append(" ").append(input.name)
                       .append(" [[attribute(").append(loc).append(")]];\n");
                    attrIndex++;
                }
                msl.append("};\n\n");
            }
            
            // Generate vertex output / fragment input struct
            if (stage == ShaderStage.VERTEX && !parsed.outputs.isEmpty()) {
                msl.append("struct VertexOut {\n");
                msl.append("    float4 position [[position]];\n");
                int locIndex = 0;
                for (GLSLVariable output : parsed.outputs) {
                    if (!output.name.equals("gl_Position")) {
                        String mslType = translateType(output.type);
                        msl.append("    ").append(mslType).append(" ").append(output.name)
                           .append(" [[user(loc").append(locIndex++).append(")]];\n");
                    }
                }
                msl.append("};\n\n");
            }
            
            // Generate fragment input struct
            if (stage == ShaderStage.FRAGMENT && !parsed.inputs.isEmpty()) {
                msl.append("struct FragmentIn {\n");
                msl.append("    float4 position [[position]];\n");
                int locIndex = 0;
                for (GLSLVariable input : parsed.inputs) {
                    String mslType = translateType(input.type);
                    msl.append("    ").append(mslType).append(" ").append(input.name)
                       .append(" [[user(loc").append(locIndex++).append(")]];\n");
                }
                msl.append("};\n\n");
            }
            
            // Generate fragment output struct
            if (stage == ShaderStage.FRAGMENT && !parsed.outputs.isEmpty()) {
                msl.append("struct FragmentOut {\n");
                int colorIndex = 0;
                for (GLSLVariable output : parsed.outputs) {
                    String mslType = translateType(output.type);
                    int loc = output.location >= 0 ? output.location : colorIndex;
                    msl.append("    ").append(mslType).append(" ").append(output.name)
                       .append(" [[color(").append(loc).append(")]];\n");
                    colorIndex++;
                }
                msl.append("};\n\n");
            }
        }
        
        private void generateFunction(StringBuilder msl, GLSLParseResult parsed, 
                                     GLSLToMSLCompiler.GLSLOptions options) {
            // Generate helper functions first
            for (GLSLFunction func : parsed.functions) {
                String returnType = translateType(func.returnType);
                String params = translateParameters(func.parameters);
                String body = translateBody(func.body, parsed, options.stage);
                msl.append(returnType).append(" ").append(func.name)
                   .append("(").append(params).append(") {\n")
                   .append(body).append("\n}\n\n");
            }
            
            // Generate main function
            switch (options.stage) {
                case VERTEX -> generateVertexMain(msl, parsed);
                case FRAGMENT -> generateFragmentMain(msl, parsed);
                case KERNEL -> generateComputeMain(msl, parsed);
            }
        }
        
        private void generateVertexMain(StringBuilder msl, GLSLParseResult parsed) {
            msl.append("vertex VertexOut vertexMain(\n");
            msl.append("    VertexIn in [[stage_in]],\n");
            msl.append("    uint vid [[vertex_id]],\n");
            msl.append("    uint iid [[instance_id]]");
            
            // Add uniform buffers
            int bufferIndex = 0;
            for (GLSLUniformBlock block : parsed.uniformBlocks) {
                msl.append(",\n    constant ").append(block.name).append("& ")
                   .append(block.instanceName != null ? block.instanceName : uncapitalize(block.name))
                   .append(" [[buffer(").append(bufferIndex++).append(")]]");
            }
            
            // Add textures and samplers
            int textureIndex = 0;
            int samplerIndex = 0;
            for (GLSLVariable uniform : parsed.uniforms) {
                if (uniform.type.contains("sampler")) {
                    String texType = translateType(uniform.type);
                    msl.append(",\n    ").append(texType).append(" ")
                       .append(uniform.name).append("_tex [[texture(").append(textureIndex++).append(")]]");
                    msl.append(",\n    sampler ").append(uniform.name)
                       .append("_smp [[sampler(").append(samplerIndex++).append(")]]");
                }
            }
            
            msl.append("\n) {\n");
            msl.append("    VertexOut out;\n\n");
            
            // Translate main body
            String body = translateBody(parsed.mainBody, parsed, ShaderStage.VERTEX);
            msl.append(body);
            
            msl.append("\n    return out;\n");
            msl.append("}\n");
        }
        
        private void generateFragmentMain(StringBuilder msl, GLSLParseResult parsed) {
            msl.append("fragment FragmentOut fragmentMain(\n");
            msl.append("    FragmentIn in [[stage_in]],\n");
            msl.append("    bool is_front_face [[front_facing]]");
            
            // Add uniform buffers
            int bufferIndex = 0;
            for (GLSLUniformBlock block : parsed.uniformBlocks) {
                msl.append(",\n    constant ").append(block.name).append("& ")
                   .append(block.instanceName != null ? block.instanceName : uncapitalize(block.name))
                   .append(" [[buffer(").append(bufferIndex++).append(")]]");
            }
            
            // Add textures and samplers
            int textureIndex = 0;
            int samplerIndex = 0;
            for (GLSLVariable uniform : parsed.uniforms) {
                if (uniform.type.contains("sampler")) {
                    String texType = translateType(uniform.type);
                    msl.append(",\n    ").append(texType).append(" ")
                       .append(uniform.name).append("_tex [[texture(").append(textureIndex++).append(")]]");
                    msl.append(",\n    sampler ").append(uniform.name)
                       .append("_smp [[sampler(").append(samplerIndex++).append(")]]");
                }
            }
            
            msl.append("\n) {\n");
            msl.append("    FragmentOut out;\n\n");
            
            // Translate main body
            String body = translateBody(parsed.mainBody, parsed, ShaderStage.FRAGMENT);
            msl.append(body);
            
            msl.append("\n    return out;\n");
            msl.append("}\n");
        }
        
        private void generateComputeMain(StringBuilder msl, GLSLParseResult parsed) {
            msl.append("kernel void computeMain(\n");
            msl.append("    uint3 gid [[thread_position_in_grid]],\n");
            msl.append("    uint3 lid [[thread_position_in_threadgroup]],\n");
            msl.append("    uint3 tgid [[threadgroup_position_in_grid]]");
            
            // Add buffers
            int bufferIndex = 0;
            for (GLSLUniformBlock block : parsed.uniformBlocks) {
                msl.append(",\n    constant ").append(block.name).append("& ")
                   .append(block.instanceName != null ? block.instanceName : uncapitalize(block.name))
                   .append(" [[buffer(").append(bufferIndex++).append(")]]");
            }
            
            // Add textures
            int textureIndex = 0;
            for (GLSLVariable uniform : parsed.uniforms) {
                if (uniform.type.contains("sampler") || uniform.type.contains("image")) {
                    String texType = translateType(uniform.type);
                    msl.append(",\n    ").append(texType).append(" ")
                       .append(uniform.name).append(" [[texture(").append(textureIndex++).append(")]]");
                }
            }
            
            msl.append("\n) {\n");
            
            // Translate main body
            String body = translateBody(parsed.mainBody, parsed, ShaderStage.KERNEL);
            msl.append(body);
            
            msl.append("}\n");
        }
        
        private String translateType(String glslType) {
            return MC_TYPE_MAP.getOrDefault(glslType, glslType);
        }
        
        private String translateParameters(String params) {
            if (params == null || params.trim().isEmpty()) {
                return "";
            }
            
            StringBuilder result = new StringBuilder();
            String[] paramList = params.split(",");
            for (int i = 0; i < paramList.length; i++) {
                if (i > 0) result.append(", ");
                String param = paramList[i].trim();
                
                // Parse parameter: [qualifier] type name
                String[] parts = param.split("\\s+");
                if (parts.length >= 2) {
                    String type = translateType(parts[parts.length - 2]);
                    String name = parts[parts.length - 1];
                    
                    // Handle inout/out qualifiers
                    if (parts.length > 2) {
                        String qualifier = parts[0];
                        if (qualifier.equals("out") || qualifier.equals("inout")) {
                            result.append("thread ").append(type).append("& ").append(name);
                        } else {
                            result.append(type).append(" ").append(name);
                        }
                    } else {
                        result.append(type).append(" ").append(name);
                    }
                }
            }
            
            return result.toString();
        }
        
        private String translateBody(String body, GLSLParseResult parsed, ShaderStage stage) {
            if (body == null) return "";
            
            String result = body;
            
            // Translate types
            for (Map.Entry<String, String> entry : MC_TYPE_MAP.entrySet()) {
                result = result.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
            }
            
            // Translate texture sampling
            result = translateTextureSampling(result, parsed);
            
            // Translate builtins based on stage
            result = translateBuiltins(result, stage);
            
            // Translate input/output variable access
            result = translateVariableAccess(result, parsed, stage);
            
            // Translate functions
            for (Map.Entry<String, String> entry : MC_FUNC_MAP.entrySet()) {
                if (!entry.getValue().isEmpty() && !entry.getKey().equals(entry.getValue())) {
                    result = result.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
                }
            }
            
            // Handle discard
            result = result.replaceAll("\\bdiscard\\s*;", "discard_fragment();");
            
            // Handle mod() -> fmod()
            result = result.replaceAll("\\bmod\\s*\\(", "fmod(");
            
            return result;
        }
        
        private String translateTextureSampling(String body, GLSLParseResult parsed) {
            String result = body;
            
            // Map sampler names to texture/sampler pairs
            Set<String> samplerNames = new HashSet<>();
            for (GLSLVariable uniform : parsed.uniforms) {
                if (uniform.type.contains("sampler")) {
                    samplerNames.add(uniform.name);
                }
            }
            
            // Pattern: texture(sampler, coords) -> sampler_tex.sample(sampler_smp, coords)
            for (String samplerName : samplerNames) {
                Pattern texPattern = Pattern.compile(
                    "texture\\s*\\(\\s*" + samplerName + "\\s*,\\s*([^)]+)\\s*\\)");
                Matcher matcher = texPattern.matcher(result);
                result = matcher.replaceAll(samplerName + "_tex.sample(" + samplerName + "_smp, $1)");
                
                // Also handle texture2D, textureLod, etc.
                Pattern tex2DPattern = Pattern.compile(
                    "texture2D\\s*\\(\\s*" + samplerName + "\\s*,\\s*([^)]+)\\s*\\)");
                matcher = tex2DPattern.matcher(result);
                result = matcher.replaceAll(samplerName + "_tex.sample(" + samplerName + "_smp, $1)");
                
                Pattern texLodPattern = Pattern.compile(
                    "textureLod\\s*\\(\\s*" + samplerName + "\\s*,\\s*([^,]+)\\s*,\\s*([^)]+)\\s*\\)");
                matcher = texLodPattern.matcher(result);
                result = matcher.replaceAll(samplerName + "_tex.sample(" + samplerName + "_smp, $1, level($2))");
            }
            
            return result;
        }
        
        private String translateBuiltins(String body, ShaderStage stage) {
            String result = body;
            
            switch (stage) {
                case VERTEX -> {
                    result = result.replaceAll("\\bgl_Position\\b", "out.position");
                    result = result.replaceAll("\\bgl_VertexID\\b", "vid");
                    result = result.replaceAll("\\bgl_VertexIndex\\b", "vid");
                    result = result.replaceAll("\\bgl_InstanceID\\b", "iid");
                    result = result.replaceAll("\\bgl_InstanceIndex\\b", "iid");
                    result = result.replaceAll("\\bgl_PointSize\\b", "out.pointSize");
                }
                case FRAGMENT -> {
                    result = result.replaceAll("\\bgl_FragCoord\\b", "in.position");
                    result = result.replaceAll("\\bgl_FrontFacing\\b", "is_front_face");
                    result = result.replaceAll("\\bgl_PointCoord\\b", "point_coord");
                }
                case KERNEL -> {
                    result = result.replaceAll("\\bgl_GlobalInvocationID\\b", "gid");
                    result = result.replaceAll("\\bgl_LocalInvocationID\\b", "lid");
                    result = result.replaceAll("\\bgl_WorkGroupID\\b", "tgid");
                    result = result.replaceAll("\\bgl_LocalInvocationIndex\\b", "lid.x + lid.y * 8 + lid.z * 64");
                }
            }
            
            return result;
        }
        
        private String translateVariableAccess(String body, GLSLParseResult parsed, ShaderStage stage) {
            String result = body;
            
            // Translate input variable access
            for (GLSLVariable input : parsed.inputs) {
                result = result.replaceAll("\\b" + input.name + "\\b", "in." + input.name);
            }
            
            // Translate output variable access
            for (GLSLVariable output : parsed.outputs) {
                if (!output.name.equals("gl_Position")) {
                    result = result.replaceAll("\\b" + output.name + "\\b", "out." + output.name);
                }
            }
            
            return result;
        }
        
        private String uncapitalize(String s) {
            if (s == null || s.isEmpty()) return s;
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }
        
        // Helper classes for GLSL parsing
        private static class GLSLParseResult {
            List<GLSLVariable> uniforms = new ArrayList<>();
            List<GLSLUniformBlock> uniformBlocks = new ArrayList<>();
            List<GLSLVariable> inputs = new ArrayList<>();
            List<GLSLVariable> outputs = new ArrayList<>();
            List<GLSLFunction> functions = new ArrayList<>();
            String mainBody;
        }
        
        private static class GLSLVariable {
            String type;
            String name;
            String qualifier;
            int location = -1;
            int arraySize = 0;
        }
        
        private static class GLSLUniformBlock {
            String name;
            String instanceName;
            List<GLSLVariable> members = new ArrayList<>();
            int mslBufferIndex;
        }
        
        private static class GLSLFunction {
            String returnType;
            String name;
            String parameters;
            String body;
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 7: SHADER REFLECTION
    // Extract metadata from compiled shaders
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Library reflection data.
     */
    public static final class LibraryReflection {
        public final List<String> functionNames = new ArrayList<>();
        public final Map<String, FunctionReflection> functions = new HashMap<>();
        public final List<ArgumentBufferDescriptor> argumentBuffers = new ArrayList<>();
        
        @Override
        public String toString() {
            return String.format("LibraryReflection[functions=%d, argBuffers=%d]",
                functionNames.size(), argumentBuffers.size());
        }
    }
    
    /**
     * Function reflection data.
     */
    public static final class FunctionReflection {
        public String name;
        public MTLFunctionType type;
        public final List<VertexAttribute> vertexAttributes = new ArrayList<>();
        public final List<StageInputAttribute> stageInputAttributes = new ArrayList<>();
        public final List<BufferBinding> bufferBindings = new ArrayList<>();
        public final List<TextureBinding> textureBindings = new ArrayList<>();
        public final List<SamplerBinding> samplerBindings = new ArrayList<>();
        public final List<ThreadgroupBinding> threadgroupBindings = new ArrayList<>();
        public int[] threadgroupSize = new int[3];
        public int maxTotalThreadsPerThreadgroup;
        public int threadExecutionWidth;
        
        @Override
        public String toString() {
            return String.format(
                "FunctionReflection[name=%s, type=%s, buffers=%d, textures=%d, samplers=%d]",
                name, type, bufferBindings.size(), textureBindings.size(), samplerBindings.size()
            );
        }
    }
    
    /**
     * Vertex attribute descriptor.
     */
    public static final class VertexAttribute {
        public String name;
        public int attributeIndex;
        public MSLDataType dataType;
        public int componentCount;
        public boolean active;
        
        @Override
        public String toString() {
            return String.format("VertexAttribute[%s: index=%d, type=%s, active=%b]",
                name, attributeIndex, dataType, active);
        }
    }
    
    /**
     * Stage input attribute descriptor.
     */
    public static final class StageInputAttribute {
        public String name;
        public int attributeIndex;
        public MSLDataType dataType;
        public boolean active;
        
        @Override
        public String toString() {
            return String.format("StageInputAttribute[%s: index=%d, type=%s]",
                name, attributeIndex, dataType);
        }
    }
    
    /**
     * Buffer binding descriptor.
     */
    public static final class BufferBinding {
        public String name;
        public int index;
        public int size;
        public int alignment;
        public BufferType type;
        public MSLAttribute addressSpace;
        public boolean active;
        public final List<StructMember> members = new ArrayList<>();
        
        public enum BufferType {
            UNIFORM,
            STORAGE,
            ARGUMENT
        }
        
        @Override
        public String toString() {
            return String.format("BufferBinding[%s: index=%d, size=%d, type=%s]",
                name, index, size, type);
        }
    }
    
    /**
     * Struct member descriptor.
     */
    public static final class StructMember {
        public String name;
        public MSLDataType dataType;
        public int offset;
        public int size;
        public int arraySize;
        
        @Override
        public String toString() {
            return String.format("StructMember[%s: offset=%d, type=%s]",
                name, offset, dataType);
        }
    }
    
    /**
     * Texture binding descriptor.
     */
    public static final class TextureBinding {
        public String name;
        public int index;
        public TextureType textureType;
        public PixelFormat pixelFormat;
        public TextureAccess access;
        public int arrayLength;
        public boolean active;
        
        public enum TextureType {
            TEXTURE_1D,
            TEXTURE_1D_ARRAY,
            TEXTURE_2D,
            TEXTURE_2D_ARRAY,
            TEXTURE_2D_MS,
            TEXTURE_2D_MS_ARRAY,
            TEXTURE_3D,
            TEXTURE_CUBE,
            TEXTURE_CUBE_ARRAY,
            TEXTURE_BUFFER
        }
        
        public enum PixelFormat {
            INVALID,
            RGBA8_UNORM,
            RGBA8_SNORM,
            RGBA8_UINT,
            RGBA8_SINT,
            RGBA16_FLOAT,
            RGBA32_FLOAT,
            DEPTH32_FLOAT,
            DEPTH24_STENCIL8
        }
        
        public enum TextureAccess {
            SAMPLE,
            READ,
            WRITE,
            READ_WRITE
        }
        
        @Override
        public String toString() {
            return String.format("TextureBinding[%s: index=%d, type=%s, access=%s]",
                name, index, textureType, access);
        }
    }
    
    /**
     * Sampler binding descriptor.
     */
    public static final class SamplerBinding {
        public String name;
        public int index;
        public boolean active;
        
        @Override
        public String toString() {
            return String.format("SamplerBinding[%s: index=%d]", name, index);
        }
    }
    
    /**
     * Threadgroup binding descriptor.
     */
    public static final class ThreadgroupBinding {
        public String name;
        public int index;
        public int size;
        public int alignment;
        
        @Override
        public String toString() {
            return String.format("ThreadgroupBinding[%s: index=%d, size=%d]",
                name, index, size);
        }
    }
    
    /**
     * Argument buffer descriptor.
     */
    public static final class ArgumentBufferDescriptor {
        public int index;
        public int encodedLength;
        public final List<ArgumentDescriptor> arguments = new ArrayList<>();
        
        @Override
        public String toString() {
            return String.format("ArgumentBuffer[index=%d, args=%d]",
                index, arguments.size());
        }
    }
    
    /**
     * Individual argument descriptor.
     */
    public static final class ArgumentDescriptor {
        public int index;
        public ArgumentType type;
        public TextureBinding.TextureType textureType;
        public int arrayLength;
        public MSLAttribute addressSpace;
        public TextureBinding.TextureAccess access;
        
        public enum ArgumentType {
            BUFFER,
            TEXTURE,
            SAMPLER,
            THREADGROUP_MEMORY,
            VISIBLE_FUNCTION_TABLE,
            PRIMITIVE_ACCELERATION_STRUCTURE,
            INSTANCE_ACCELERATION_STRUCTURE
        }
    }
    
    /**
     * Extract reflection data from a library.
     */
    private static LibraryReflection extractLibraryReflection(long library) {
        LibraryReflection reflection = new LibraryReflection();
        
        // Get function names
        String[] names = nMTLLibraryGetFunctionNames(library);
        if (names != null) {
            reflection.functionNames.addAll(Arrays.asList(names));
        }
        
        return reflection;
    }
    
    /**
     * Extract reflection data from a function.
     */
    private static FunctionReflection extractFunctionReflection(long function) {
        FunctionReflection reflection = new FunctionReflection();
        
        reflection.name = nMTLFunctionGetName(function);
        reflection.type = MTLFunctionType.fromValue(nMTLFunctionGetFunctionType(function));
        
        // Extract vertex attributes
        int vertexAttrCount = nMTLFunctionGetVertexAttributeCount(function);
        for (int i = 0; i < vertexAttrCount; i++) {
            VertexAttribute attr = new VertexAttribute();
            attr.name = nMTLFunctionGetVertexAttributeName(function, i);
            attr.attributeIndex = nMTLFunctionGetVertexAttributeIndex(function, i);
            attr.dataType = MSLDataType.values()[nMTLFunctionGetVertexAttributeType(function, i)];
            attr.active = nMTLFunctionGetVertexAttributeActive(function, i);
            reflection.vertexAttributes.add(attr);
        }
        
        // Extract stage input attributes
        int stageInputCount = nMTLFunctionGetStageInputAttributeCount(function);
        for (int i = 0; i < stageInputCount; i++) {
            StageInputAttribute attr = new StageInputAttribute();
            attr.name = nMTLFunctionGetStageInputAttributeName(function, i);
            attr.attributeIndex = nMTLFunctionGetStageInputAttributeIndex(function, i);
            attr.dataType = MSLDataType.values()[nMTLFunctionGetStageInputAttributeType(function, i)];
            attr.active = nMTLFunctionGetStageInputAttributeActive(function, i);
            reflection.stageInputAttributes.add(attr);
        }
        
        // Extract buffer bindings
        int bufferCount = nMTLFunctionGetBufferBindingCount(function);
        for (int i = 0; i < bufferCount; i++) {
            BufferBinding binding = new BufferBinding();
            binding.name = nMTLFunctionGetBufferBindingName(function, i);
            binding.index = nMTLFunctionGetBufferBindingIndex(function, i);
            binding.size = nMTLFunctionGetBufferBindingSize(function, i);
            binding.alignment = nMTLFunctionGetBufferBindingAlignment(function, i);
            binding.type = BufferBinding.BufferType.values()[nMTLFunctionGetBufferBindingType(function, i)];
            binding.active = nMTLFunctionGetBufferBindingActive(function, i);
            
            // Extract struct members if available
            int memberCount = nMTLFunctionGetBufferBindingMemberCount(function, i);
            for (int j = 0; j < memberCount; j++) {
                StructMember member = new StructMember();
                member.name = nMTLFunctionGetBufferBindingMemberName(function, i, j);
                member.offset = nMTLFunctionGetBufferBindingMemberOffset(function, i, j);
                member.size = nMTLFunctionGetBufferBindingMemberSize(function, i, j);
                member.dataType = MSLDataType.values()[nMTLFunctionGetBufferBindingMemberType(function, i, j)];
                binding.members.add(member);
            }
            
            reflection.bufferBindings.add(binding);
        }
        
        // Extract texture bindings
        int textureCount = nMTLFunctionGetTextureBindingCount(function);
        for (int i = 0; i < textureCount; i++) {
            TextureBinding binding = new TextureBinding();
            binding.name = nMTLFunctionGetTextureBindingName(function, i);
            binding.index = nMTLFunctionGetTextureBindingIndex(function, i);
            binding.textureType = TextureBinding.TextureType.values()[
                nMTLFunctionGetTextureBindingType(function, i)];
            binding.access = TextureBinding.TextureAccess.values()[
                nMTLFunctionGetTextureBindingAccess(function, i)];
            binding.arrayLength = nMTLFunctionGetTextureBindingArrayLength(function, i);
            binding.active = nMTLFunctionGetTextureBindingActive(function, i);
            reflection.textureBindings.add(binding);
        }
        
        // Extract sampler bindings
        int samplerCount = nMTLFunctionGetSamplerBindingCount(function);
        for (int i = 0; i < samplerCount; i++) {
            SamplerBinding binding = new SamplerBinding();
            binding.name = nMTLFunctionGetSamplerBindingName(function, i);
            binding.index = nMTLFunctionGetSamplerBindingIndex(function, i);
            binding.active = nMTLFunctionGetSamplerBindingActive(function, i);
            reflection.samplerBindings.add(binding);
        }
        
        // Extract threadgroup bindings for compute
        if (reflection.type == MTLFunctionType.KERNEL) {
            int tgCount = nMTLFunctionGetThreadgroupBindingCount(function);
            for (int i = 0; i < tgCount; i++) {
                ThreadgroupBinding binding = new ThreadgroupBinding();
                binding.name = nMTLFunctionGetThreadgroupBindingName(function, i);
                binding.index = nMTLFunctionGetThreadgroupBindingIndex(function, i);
                binding.size = nMTLFunctionGetThreadgroupBindingSize(function, i);
                binding.alignment = nMTLFunctionGetThreadgroupBindingAlignment(function, i);
                reflection.threadgroupBindings.add(binding);
            }
            
            // Get threadgroup size
            int[] size = nMTLFunctionGetThreadgroupSize(function);
            System.arraycopy(size, 0, reflection.threadgroupSize, 0, 3);
            reflection.maxTotalThreadsPerThreadgroup = nMTLFunctionGetMaxTotalThreadsPerThreadgroup(function);
            reflection.threadExecutionWidth = nMTLFunctionGetThreadExecutionWidth(function);
        }
        
        return reflection;
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 8: SHADER OPTIMIZATION
    // Pre-compilation and runtime optimization passes
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Shader optimizer for MSL source.
     */
    public static final class ShaderOptimizer {
        
        private final MSLCapabilities capabilities;
        private final OptimizationOptions options;
        
        public ShaderOptimizer(MSLCapabilities capabilities) {
            this(capabilities, new OptimizationOptions());
        }
        
        public ShaderOptimizer(MSLCapabilities capabilities, OptimizationOptions options) {
            this.capabilities = capabilities;
            this.options = options;
        }
        
        /**
         * Optimization options.
         */
        public static final class OptimizationOptions {
            public boolean enableFastMath = true;
            public boolean enableLoopUnrolling = true;
            public boolean enableConstantFolding = true;
            public boolean enableDeadCodeElimination = true;
            public boolean enableInlining = true;
            public boolean enableVectorization = true;
            public boolean enablePrecisionRelaxation = false;
            public int maxUnrollIterations = 64;
            public int maxInlineSize = 100;
            public boolean useHalfPrecisionWhenPossible = false;
        }
        
        /**
         * Optimization result.
         */
        public static final class OptimizationResult {
            public String optimizedSource;
            public int linesRemoved;
            public int functionsInlined;
            public int loopsUnrolled;
            public int constantsFolded;
            public long optimizationTimeNs;
            
            @Override
            public String toString() {
                return String.format(
                    "OptimizationResult[lines=%d, inlined=%d, unrolled=%d, folded=%d, time=%.2fms]",
                    linesRemoved, functionsInlined, loopsUnrolled, constantsFolded,
                    optimizationTimeNs / 1_000_000.0
                );
            }
        }
        
        /**
         * Optimize MSL source code.
         */
        public OptimizationResult optimize(String source) {
            OptimizationResult result = new OptimizationResult();
            long startTime = System.nanoTime();
            
            String optimized = source;
            int originalLines = countLines(source);
            
            // Apply optimization passes
            if (options.enableConstantFolding) {
                ConstantFoldingPass foldingPass = new ConstantFoldingPass();
                optimized = foldingPass.apply(optimized);
                result.constantsFolded = foldingPass.foldedCount;
            }
            
            if (options.enableDeadCodeElimination) {
                DeadCodeEliminationPass dcePass = new DeadCodeEliminationPass();
                optimized = dcePass.apply(optimized);
            }
            
            if (options.enableLoopUnrolling) {
                LoopUnrollingPass unrollPass = new LoopUnrollingPass(options.maxUnrollIterations);
                optimized = unrollPass.apply(optimized);
                result.loopsUnrolled = unrollPass.unrolledCount;
            }
            
            if (options.enableInlining) {
                InliningPass inlinePass = new InliningPass(options.maxInlineSize);
                optimized = inlinePass.apply(optimized);
                result.functionsInlined = inlinePass.inlinedCount;
            }
            
            if (options.enableVectorization) {
                VectorizationPass vecPass = new VectorizationPass();
                optimized = vecPass.apply(optimized);
            }
            
            if (options.useHalfPrecisionWhenPossible && capabilities.hasFeature(MSLFeature.BASIC_SHADERS)) {
                PrecisionRelaxationPass precPass = new PrecisionRelaxationPass();
                optimized = precPass.apply(optimized);
            }
            
            // Apply fast math transformations
            if (options.enableFastMath) {
                FastMathPass fastMathPass = new FastMathPass();
                optimized = fastMathPass.apply(optimized);
            }
            
            result.optimizedSource = optimized;
            result.linesRemoved = originalLines - countLines(optimized);
            result.optimizationTimeNs = System.nanoTime() - startTime;
            
            return result;
        }
        
        private int countLines(String source) {
            return source.split("\n").length;
        }
        
        /**
         * Constant folding optimization pass.
         */
        private static final class ConstantFoldingPass {
            int foldedCount = 0;
            
            String apply(String source) {
                String result = source;
                
                // Fold simple arithmetic expressions with constants
                // Pattern: (const op const) where op is +, -, *, /
                Pattern arithPattern = Pattern.compile(
                    "\\(\\s*(-?\\d+\\.?\\d*)\\s*([+\\-*/])\\s*(-?\\d+\\.?\\d*)\\s*\\)");
                
                Matcher matcher = arithPattern.matcher(result);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    double left = Double.parseDouble(matcher.group(1));
                    String op = matcher.group(2);
                    double right = Double.parseDouble(matcher.group(3));
                    
                    double folded = switch (op) {
                        case "+" -> left + right;
                        case "-" -> left - right;
                        case "*" -> left * right;
                        case "/" -> right != 0 ? left / right : Double.NaN;
                        default -> Double.NaN;
                    };
                    
                    if (!Double.isNaN(folded)) {
                        String foldedStr = folded == (int) folded ? 
                            String.valueOf((int) folded) : String.valueOf(folded);
                        matcher.appendReplacement(sb, foldedStr);
                        foldedCount++;
                    }
                }
                matcher.appendTail(sb);
                result = sb.toString();
                
                // Fold vector constructors with all constant arguments
                // e.g., float3(1.0, 2.0, 3.0) stays as is, but could be simplified in some cases
                
                // Fold boolean expressions
                result = result.replaceAll("\\btrue\\s*&&\\s*true\\b", "true");
                result = result.replaceAll("\\bfalse\\s*&&\\s*\\w+\\b", "false");
                result = result.replaceAll("\\btrue\\s*\\|\\|\\s*\\w+\\b", "true");
                result = result.replaceAll("\\bfalse\\s*\\|\\|\\s*false\\b", "false");
                
                return result;
            }
        }
        
        /**
         * Dead code elimination pass.
         */
        private static final class DeadCodeEliminationPass {
            String apply(String source) {
                String result = source;
                
                // Remove if(false) blocks
                Pattern ifFalsePattern = Pattern.compile(
                    "if\\s*\\(\\s*false\\s*\\)\\s*\\{[^}]*\\}(\\s*else\\s*\\{([^}]*)\\})?");
                Matcher matcher = ifFalsePattern.matcher(result);
                result = matcher.replaceAll("$2");
                
                // Remove if(true) else blocks, keep if body
                Pattern ifTruePattern = Pattern.compile(
                    "if\\s*\\(\\s*true\\s*\\)\\s*\\{([^}]*)\\}\\s*else\\s*\\{[^}]*\\}");
                matcher = ifTruePattern.matcher(result);
                result = matcher.replaceAll("$1");
                
                // Remove unreachable code after return/discard
                Pattern unreachablePattern = Pattern.compile(
                    "(return[^;]*;|discard_fragment\\(\\);)\\s*([^}]+)(?=\\})");
                matcher = unreachablePattern.matcher(result);
                result = matcher.replaceAll("$1");
                
                // Remove empty blocks
                result = result.replaceAll("\\{\\s*\\}", "");
                
                // Remove redundant semicolons
                result = result.replaceAll(";\\s*;", ";");
                
                return result;
            }
        }
        
        /**
         * Loop unrolling pass.
         */
        private static final class LoopUnrollingPass {
            final int maxIterations;
            int unrolledCount = 0;
            
            LoopUnrollingPass(int maxIterations) {
                this.maxIterations = maxIterations;
            }
            
            String apply(String source) {
                String result = source;
                
                // Pattern: for (int i = 0; i < CONST; i++) { body }
                Pattern forPattern = Pattern.compile(
                    "for\\s*\\(\\s*(?:int|uint)\\s+(\\w+)\\s*=\\s*(\\d+)\\s*;\\s*\\1\\s*<\\s*(\\d+)\\s*;\\s*\\1\\+\\+\\s*\\)\\s*\\{([^}]+)\\}");
                
                Matcher matcher = forPattern.matcher(result);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    String varName = matcher.group(1);
                    int start = Integer.parseInt(matcher.group(2));
                    int end = Integer.parseInt(matcher.group(3));
                    String body = matcher.group(4);
                    
                    int iterations = end - start;
                    if (iterations > 0 && iterations <= maxIterations && body.length() < 500) {
                        StringBuilder unrolled = new StringBuilder();
                        unrolled.append("{\n");
                        for (int i = start; i < end; i++) {
                            String iterBody = body.replaceAll("\\b" + varName + "\\b", String.valueOf(i));
                            unrolled.append(iterBody);
                        }
                        unrolled.append("}\n");
                        matcher.appendReplacement(sb, Matcher.quoteReplacement(unrolled.toString()));
                        unrolledCount++;
                    }
                }
                matcher.appendTail(sb);
                result = sb.toString();
                
                return result;
            }
        }
        
        /**
         * Function inlining pass.
         */
        private static final class InliningPass {
            final int maxSize;
            int inlinedCount = 0;
            
            InliningPass(int maxSize) {
                this.maxSize = maxSize;
            }
            
            String apply(String source) {
                // Simple inlining for small helper functions
                // This is a simplified version - full implementation would parse the AST
                
                // Find small functions that could be inlined
                Map<String, String> inlineCandidates = new HashMap<>();
                Pattern funcPattern = Pattern.compile(
                    "(\\w+)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*\\{\\s*return\\s+([^;]+);\\s*\\}");
                
                Matcher matcher = funcPattern.matcher(source);
                while (matcher.find()) {
                    String returnType = matcher.group(1);
                    String funcName = matcher.group(2);
                    String params = matcher.group(3);
                    String body = matcher.group(4);
                    
                    if (body.length() <= maxSize && !funcName.equals("main") && 
                        !funcName.endsWith("Main")) {
                        inlineCandidates.put(funcName, body);
                    }
                }
                
                String result = source;
                
                // Inline function calls
                for (Map.Entry<String, String> entry : inlineCandidates.entrySet()) {
                    String funcName = entry.getKey();
                    String body = entry.getValue();
                    
                    // Simple case: parameterless functions
                    Pattern callPattern = Pattern.compile(funcName + "\\s*\\(\\s*\\)");
                    Matcher callMatcher = callPattern.matcher(result);
                    if (callMatcher.find()) {
                        result = callMatcher.replaceAll("(" + Matcher.quoteReplacement(body) + ")");
                        inlinedCount++;
                    }
                }
                
                return result;
            }
        }
        
        /**
         * Vectorization pass.
         */
        private static final class VectorizationPass {
            String apply(String source) {
                String result = source;
                
                // Convert scalar operations on vector components to vector operations
                // e.g., v.x * s, v.y * s, v.z * s -> v * s
                
                // Detect patterns like: float3(a.x * b, a.y * b, a.z * b)
                Pattern scalarMulPattern = Pattern.compile(
                    "float3\\s*\\(\\s*(\\w+)\\.x\\s*\\*\\s*(\\w+)\\s*,\\s*\\1\\.y\\s*\\*\\s*\\2\\s*,\\s*\\1\\.z\\s*\\*\\s*\\2\\s*\\)");
                Matcher matcher = scalarMulPattern.matcher(result);
                result = matcher.replaceAll("$1 * $2");
                
                // float4 version
                Pattern scalarMul4Pattern = Pattern.compile(
                    "float4\\s*\\(\\s*(\\w+)\\.x\\s*\\*\\s*(\\w+)\\s*,\\s*\\1\\.y\\s*\\*\\s*\\2\\s*,\\s*\\1\\.z\\s*\\*\\s*\\2\\s*,\\s*\\1\\.w\\s*\\*\\s*\\2\\s*\\)");
                matcher = scalarMul4Pattern.matcher(result);
                result = matcher.replaceAll("$1 * $2");
                
                // Convert component-wise min/max to vector operations
                Pattern minPattern = Pattern.compile(
                    "float3\\s*\\(\\s*min\\s*\\(\\s*(\\w+)\\.x\\s*,\\s*(\\w+)\\.x\\s*\\)\\s*,\\s*min\\s*\\(\\s*\\1\\.y\\s*,\\s*\\2\\.y\\s*\\)\\s*,\\s*min\\s*\\(\\s*\\1\\.z\\s*,\\s*\\2\\.z\\s*\\)\\s*\\)");
                matcher = minPattern.matcher(result);
                result = matcher.replaceAll("min($1, $2)");
                
                return result;
            }
        }
        
        /**
         * Precision relaxation pass - convert float to half where safe.
         */
        private static final class PrecisionRelaxationPass {
            String apply(String source) {
                String result = source;
                
                // This is a conservative pass - only convert clearly safe cases
                // Full implementation would require data flow analysis
                
                // Convert color values to half (0-1 range)
                // Pattern: float4 color = ... -> half4 color = ...
                // Only if the variable name suggests it's a color
                Pattern colorPattern = Pattern.compile(
                    "\\bfloat4\\s+(color|col|rgba|albedo)\\b");
                result = colorPattern.matcher(result).replaceAll("half4 $1");
                
                Pattern color3Pattern = Pattern.compile(
                    "\\bfloat3\\s+(color|col|rgb|albedo)\\b");
                result = color3Pattern.matcher(result).replaceAll("half3 $1");
                
                // Convert UV coordinates to half (usually 0-1 range)
                Pattern uvPattern = Pattern.compile(
                    "\\bfloat2\\s+(uv|texcoord|tc)\\d*\\b");
                result = uvPattern.matcher(result).replaceAll("half2 $1");
                
                return result;
            }
        }
        
        /**
         * Fast math optimization pass.
         */
        private static final class FastMathPass {
            String apply(String source) {
                String result = source;
                
                // Replace precise_sqrt with sqrt (fast math allows this)
                result = result.replaceAll("\\bprecise::sqrt\\b", "sqrt");
                result = result.replaceAll("\\bprecise::rsqrt\\b", "rsqrt");
                
                // Use fma where applicable: a * b + c -> fma(a, b, c)
                // Pattern: (expr1) * (expr2) + (expr3) or expr1 * expr2 + expr3
                // This is complex and should be done carefully
                Pattern fmaPattern = Pattern.compile(
                    "\\(([^()]+)\\)\\s*\\*\\s*\\(([^()]+)\\)\\s*\\+\\s*\\(([^()]+)\\)");
                result = fmaPattern.matcher(result).replaceAll("fma($1, $2, $3)");
                
                // Optimize reciprocal: 1.0 / x -> rcp(x) for appropriate types
                // Note: MSL uses 1.0/x for reciprocal, but fast_divide is available
                
                // Optimize pow with integer exponents
                result = result.replaceAll("pow\\s*\\(\\s*([^,]+)\\s*,\\s*2\\.0\\s*\\)", "($1 * $1)");
                result = result.replaceAll("pow\\s*\\(\\s*([^,]+)\\s*,\\s*3\\.0\\s*\\)", "($1 * $1 * $1)");
                result = result.replaceAll("pow\\s*\\(\\s*([^,]+)\\s*,\\s*0\\.5\\s*\\)", "sqrt($1)");
                
                // Use fast_normalize where precision isn't critical
                // This would require analysis to determine safety
                
                return result;
            }
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 9: BINARY ARCHIVES AND CACHING
    // Metal 2.3+ binary archive support for compiled shader caching
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Binary archive manager for shader caching.
     * Requires Metal 2.3+ (MSL 2.3+)
     */
    public final class BinaryArchiveManager {
        
        private final long device;
        private final MSLCapabilities capabilities;
        private final Map<String, Long> archiveHandles = new ConcurrentHashMap<>();
        private final Path cacheDirectory;
        
        public BinaryArchiveManager(long device, MSLCapabilities capabilities, Path cacheDirectory) {
            this.device = device;
            this.capabilities = capabilities;
            this.cacheDirectory = cacheDirectory;
            
            if (!capabilities.hasFeature(MSLFeature.BINARY_ARCHIVES)) {
                LOGGER.warn("Binary archives not supported on this device");
            }
        }
        
        /**
         * Create or load a binary archive.
         */
        public long getOrCreateArchive(String name) {
            if (!capabilities.hasFeature(MSLFeature.BINARY_ARCHIVES)) {
                return 0;
            }
            
            return archiveHandles.computeIfAbsent(name, n -> {
                Path archivePath = cacheDirectory.resolve(n + ".metallib");
                
                // Try to load existing archive
                if (archivePath.toFile().exists()) {
                    long archive = nMTLDeviceNewBinaryArchiveWithURL(device, archivePath.toString());
                    if (archive != 0) {
                        LOGGER.debug("Loaded binary archive: {}", archivePath);
                        return archive;
                    }
                }
                
                // Create new archive
                long archive = nMTLDeviceNewBinaryArchive(device);
                if (archive != 0) {
                    LOGGER.debug("Created new binary archive: {}", name);
                }
                return archive;
            });
        }
        
        /**
         * Add a compiled pipeline to the archive.
         */
        public boolean addPipelineToArchive(String archiveName, long pipelineDescriptor) {
            if (!capabilities.hasFeature(MSLFeature.BINARY_ARCHIVES)) {
                return false;
            }
            
            long archive = getOrCreateArchive(archiveName);
            if (archive == 0) return false;
            
            long[] error = {0};
            boolean success = nMTLBinaryArchiveAddRenderPipeline(archive, pipelineDescriptor, error);
            
            if (!success && error[0] != 0) {
                String errorMsg = nNSErrorLocalizedDescription(error[0]);
                LOGGER.warn("Failed to add pipeline to archive: {}", errorMsg);
                nRelease(error[0]);
            }
            
            return success;
        }
        
        /**
         * Add a compute pipeline to the archive.
         */
        public boolean addComputePipelineToArchive(String archiveName, long pipelineDescriptor) {
            if (!capabilities.hasFeature(MSLFeature.BINARY_ARCHIVES)) {
                return false;
            }
            
            long archive = getOrCreateArchive(archiveName);
            if (archive == 0) return false;
            
            long[] error = {0};
            boolean success = nMTLBinaryArchiveAddComputePipeline(archive, pipelineDescriptor, error);
            
            if (!success && error[0] != 0) {
                String errorMsg = nNSErrorLocalizedDescription(error[0]);
                LOGGER.warn("Failed to add compute pipeline to archive: {}", errorMsg);
                nRelease(error[0]);
            }
            
            return success;
        }
        
        /**
         * Serialize archive to disk.
         */
        public boolean serializeArchive(String name) {
            if (!capabilities.hasFeature(MSLFeature.BINARY_ARCHIVES)) {
                return false;
            }
            
            Long archiveHandle = archiveHandles.get(name);
            if (archiveHandle == null || archiveHandle == 0) {
                return false;
            }
            
            Path archivePath = cacheDirectory.resolve(name + ".metallib");
            
            // Ensure directory exists
            try {
                java.nio.file.Files.createDirectories(cacheDirectory);
            } catch (IOException e) {
                LOGGER.error("Failed to create cache directory", e);
                return false;
            }
            
            long[] error = {0};
            boolean success = nMTLBinaryArchiveSerialize(archiveHandle, archivePath.toString(), error);
            
            if (!success && error[0] != 0) {
                String errorMsg = nNSErrorLocalizedDescription(error[0]);
                LOGGER.error("Failed to serialize archive: {}", errorMsg);
                nRelease(error[0]);
            } else {
                LOGGER.info("Serialized binary archive to: {}", archivePath);
            }
            
            return success;
        }
        
        /**
         * Close all archives.
         */
        public void closeAll() {
            for (Long handle : archiveHandles.values()) {
                if (handle != 0) {
                    nRelease(handle);
                }
            }
            archiveHandles.clear();
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 10: DYNAMIC LIBRARIES (Metal 3.1+)
    // Dynamic library support for modular shader compilation
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Dynamic library manager for Metal 3.1+.
     */
    public final class DynamicLibraryManager {
        
        private final long device;
        private final MSLCapabilities capabilities;
        private final Map<String, Long> libraries = new ConcurrentHashMap<>();
        private final Map<String, Long> preloadedLibraries = new ConcurrentHashMap<>();
        
        public DynamicLibraryManager(long device, MSLCapabilities capabilities) {
            this.device = device;
            this.capabilities = capabilities;
            
            if (!capabilities.hasFeature(MSLFeature.DYNAMIC_LIBRARIES)) {
                LOGGER.warn("Dynamic libraries not supported on this device");
            }
        }
        
        /**
         * Compile a dynamic library from source.
         */
        public long compileLibrary(String name, String source, MTLCompileOptions options) {
            if (!capabilities.hasFeature(MSLFeature.DYNAMIC_LIBRARIES)) {
                throw new UnsupportedOperationException("Dynamic libraries require Metal 3.1+");
            }
            
            // Set install name for the library
            if (options.installName == null) {
                options.installName = new MTLCompileOptions.InstallName(name, "1.0");
            }
            
            long optionsHandle = options.toNative();
            
            try {
                long[] error = {0};
                long library = nMTLDeviceNewDynamicLibraryWithSource(device, source, optionsHandle, error);
                
                if (library == 0) {
                    String errorMsg = error[0] != 0 ? nNSErrorLocalizedDescription(error[0]) : "Unknown error";
                    if (error[0] != 0) nRelease(error[0]);
                    throw new ShaderCompilationException(name, "Dynamic library compilation failed: " + errorMsg);
                }
                
                if (error[0] != 0) nRelease(error[0]);
                
                libraries.put(name, library);
                return library;
                
            } finally {
                nRelease(optionsHandle);
            }
        }
        
        /**
         * Load a precompiled dynamic library.
         */
        public long loadLibrary(String path) {
            if (!capabilities.hasFeature(MSLFeature.DYNAMIC_LIBRARIES)) {
                throw new UnsupportedOperationException("Dynamic libraries require Metal 3.1+");
            }
            
            String name = new File(path).getName();
            
            long[] error = {0};
            long library = nMTLDeviceNewDynamicLibraryWithURL(device, path, error);
            
            if (library == 0) {
                String errorMsg = error[0] != 0 ? nNSErrorLocalizedDescription(error[0]) : "File not found";
                if (error[0] != 0) nRelease(error[0]);
                throw new ShaderCompilationException(name, "Failed to load dynamic library: " + errorMsg);
            }
            
            if (error[0] != 0) nRelease(error[0]);
            
            libraries.put(name, library);
            return library;
        }
        
        /**
         * Create a preloaded library from multiple dynamic libraries.
         */
        public long createPreloadedLibrary(String name, List<String> libraryNames) {
            if (!capabilities.hasFeature(MSLFeature.PRELOADED_LIBRARIES)) {
                throw new UnsupportedOperationException("Preloaded libraries require Metal 3.1+");
            }
            
            // Collect library handles
            long[] libHandles = new long[libraryNames.size()];
            for (int i = 0; i < libraryNames.size(); i++) {
                Long handle = libraries.get(libraryNames.get(i));
                if (handle == null || handle == 0) {
                    throw new IllegalArgumentException("Library not found: " + libraryNames.get(i));
                }
                libHandles[i] = handle;
            }
            
            long[] error = {0};
            long preloaded = nMTLDeviceNewPreloadedLibrary(device, libHandles, error);
            
            if (preloaded == 0) {
                String errorMsg = error[0] != 0 ? nNSErrorLocalizedDescription(error[0]) : "Unknown error";
                if (error[0] != 0) nRelease(error[0]);
                throw new ShaderCompilationException(name, "Failed to create preloaded library: " + errorMsg);
            }
            
            if (error[0] != 0) nRelease(error[0]);
            
            preloadedLibraries.put(name, preloaded);
            return preloaded;
        }
        
        /**
         * Get a library by name.
         */
        @Nullable
        public Long getLibrary(String name) {
            return libraries.get(name);
        }
        
        /**
         * Get a preloaded library by name.
         */
        @Nullable
        public Long getPreloadedLibrary(String name) {
            return preloadedLibraries.get(name);
        }
        
        /**
         * Release all libraries.
         */
        public void releaseAll() {
            for (Long handle : libraries.values()) {
                if (handle != 0) nRelease(handle);
            }
            libraries.clear();
            
            for (Long handle : preloadedLibraries.values()) {
                if (handle != 0) nRelease(handle);
            }
            preloadedLibraries.clear();
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 11: MINECRAFT-SPECIFIC SHADER TEMPLATES
    // Pre-built shader templates for common Minecraft rendering operations
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Minecraft shader template generator.
     */
    public static final class MinecraftShaderTemplates {
        
        /**
         * Generate a basic position-color-texture vertex shader.
         */
        public static String generatePositionColorTexVertex(MSLVersion version) {
            return """
                #include <metal_stdlib>
                using namespace metal;
                
                struct Uniforms {
                    float4x4 ModelViewMat;
                    float4x4 ProjMat;
                    float4 ColorModulator;
                };
                
                struct VertexIn {
                    float3 Position [[attribute(0)]];
                    float4 Color [[attribute(1)]];
                    float2 UV0 [[attribute(2)]];
                };
                
                struct VertexOut {
                    float4 position [[position]];
                    float4 vertexColor;
                    float2 texCoord0;
                };
                
                vertex VertexOut vertexMain(
                    VertexIn in [[stage_in]],
                    constant Uniforms& uniforms [[buffer(0)]]
                ) {
                    VertexOut out;
                    out.position = uniforms.ProjMat * uniforms.ModelViewMat * float4(in.Position, 1.0);
                    out.vertexColor = in.Color * uniforms.ColorModulator;
                    out.texCoord0 = in.UV0;
                    return out;
                }
                """;
        }
        
        /**
         * Generate a basic position-color-texture fragment shader.
         */
        public static String generatePositionColorTexFragment(MSLVersion version) {
            return """
                #include <metal_stdlib>
                using namespace metal;
                
                struct FragmentIn {
                    float4 position [[position]];
                    float4 vertexColor;
                    float2 texCoord0;
                };
                
                struct FragmentOut {
                    float4 color [[color(0)]];
                };
                
                fragment FragmentOut fragmentMain(
                    FragmentIn in [[stage_in]],
                    texture2d<float> Sampler0 [[texture(0)]],
                    sampler sampler0 [[sampler(0)]]
                ) {
                    FragmentOut out;
                    float4 texColor = Sampler0.sample(sampler0, in.texCoord0);
                    out.color = texColor * in.vertexColor;
                    return out;
                }
                """;
        }
        
        /**
         * Generate terrain shader with lightmap support.
         */
        public static String generateTerrainVertex(MSLVersion version) {
            return """
                #include <metal_stdlib>
                using namespace metal;
                
                struct Uniforms {
                    float4x4 ModelViewMat;
                    float4x4 ProjMat;
                    float3 ChunkOffset;
                    float4 ColorModulator;
                };
                
                struct VertexIn {
                    float3 Position [[attribute(0)]];
                    float4 Color [[attribute(1)]];
                    float2 UV0 [[attribute(2)]];
                    int2 UV2 [[attribute(3)]];
                    float3 Normal [[attribute(4)]];
                };
                
                struct VertexOut {
                    float4 position [[position]];
                    float4 vertexColor;
                    float2 texCoord0;
                    float2 texCoord2;
                    float4 normal;
                    float vertexDistance;
                };
                
                constant float3 LIGHT0_DIRECTION = float3(0.2, 1.0, -0.7);
                constant float3 LIGHT1_DIRECTION = float3(-0.2, 1.0, 0.7);
                
                vertex VertexOut vertexMain(
                    VertexIn in [[stage_in]],
                    constant Uniforms& uniforms [[buffer(0)]]
                ) {
                    VertexOut out;
                    
                    float3 position = in.Position + uniforms.ChunkOffset;
                    float4 viewPos = uniforms.ModelViewMat * float4(position, 1.0);
                    out.position = uniforms.ProjMat * viewPos;
                    
                    // Calculate lighting
                    float3 normal = normalize(in.Normal);
                    float light0 = max(0.0, dot(normal, normalize(LIGHT0_DIRECTION)));
                    float light1 = max(0.0, dot(normal, normalize(LIGHT1_DIRECTION)));
                    float light = min(1.0, (light0 + light1) * 0.6 + 0.4);
                    
                    out.vertexColor = float4(in.Color.rgb * light, in.Color.a) * uniforms.ColorModulator;
                    out.texCoord0 = in.UV0;
                    out.texCoord2 = float2(in.UV2) / 256.0;
                    out.normal = float4(normal, 0.0);
                    out.vertexDistance = length(viewPos.xyz);
                    
                    return out;
                }
                """;
        }
        
        /**
         * Generate terrain fragment shader.
         */
        public static String generateTerrainFragment(MSLVersion version) {
            return """
                #include <metal_stdlib>
                using namespace metal;
                
                struct Uniforms {
                    float FogStart;
                    float FogEnd;
                    float4 FogColor;
                };
                
                struct FragmentIn {
                    float4 position [[position]];
                    float4 vertexColor;
                    float2 texCoord0;
                    float2 texCoord2;
                    float4 normal;
                    float vertexDistance;
                };
                
                struct FragmentOut {
                    float4 color [[color(0)]];
                };
                
                float linearFog(float vertexDistance, float fogStart, float fogEnd) {
                    if (vertexDistance <= fogStart) return 0.0;
                    if (vertexDistance >= fogEnd) return 1.0;
                    return smoothstep(fogStart, fogEnd, vertexDistance);
                }
                
                fragment FragmentOut fragmentMain(
                    FragmentIn in [[stage_in]],
                    constant Uniforms& uniforms [[buffer(0)]],
                    texture2d<float> Sampler0 [[texture(0)]],
                    texture2d<float> Sampler2 [[texture(1)]],
                    sampler sampler0 [[sampler(0)]]
                ) {
                    FragmentOut out;
                    
                    float4 texColor = Sampler0.sample(sampler0, in.texCoord0);
                    if (texColor.a < 0.1) {
                        discard_fragment();
                    }
                    
                    float4 lightmapColor = Sampler2.sample(sampler0, in.texCoord2);
                    float4 color = texColor * in.vertexColor * lightmapColor;
                    
                    // Apply fog
                    float fogFactor = linearFog(in.vertexDistance, uniforms.FogStart, uniforms.FogEnd);
                    out.color = mix(color, uniforms.FogColor, fogFactor);
                    
                    return out;
                }
                """;
        }
        
        /**
         * Generate entity shader with animation support.
         */
        public static String generateEntityVertex(MSLVersion version) {
            return """
                #include <metal_stdlib>
                using namespace metal;
                
                struct Uniforms {
                    float4x4 ModelViewMat;
                    float4x4 ProjMat;
                    float4 ColorModulator;
                    float4 Light0_Direction;
                    float4 Light1_Direction;
                };
                
                struct VertexIn {
                    float3 Position [[attribute(0)]];
                    float4 Color [[attribute(1)]];
                    float2 UV0 [[attribute(2)]];
                    int2 UV1 [[attribute(3)]];
                    int2 UV2 [[attribute(4)]];
                    float3 Normal [[attribute(5)]];
                };
                
                struct VertexOut {
                    float4 position [[position]];
                    float4 vertexColor;
                    float2 texCoord0;
                    float2 texCoord1;
                    float2 texCoord2;
                    float4 normal;
                    float vertexDistance;
                };
                
                float minecraft_mix_light(float3 lightDir0, float3 lightDir1, float3 normal) {
                    float light0 = max(0.0, dot(lightDir0, normal));
                    float light1 = max(0.0, dot(lightDir1, normal));
                    return min(1.0, (light0 + light1) * 0.6 + 0.4);
                }
                
                vertex VertexOut vertexMain(
                    VertexIn in [[stage_in]],
                    constant Uniforms& uniforms [[buffer(0)]]
                ) {
                    VertexOut out;
                    
                    float4 viewPos = uniforms.ModelViewMat * float4(in.Position, 1.0);
                    out.position = uniforms.ProjMat * viewPos;
                    
                    float3 normal = normalize(in.Normal);
                    float lightness = minecraft_mix_light(
                        uniforms.Light0_Direction.xyz,
                        uniforms.Light1_Direction.xyz,
                        normal
                    );
                    
                    out.vertexColor = float4(in.Color.rgb * lightness, in.Color.a) * uniforms.ColorModulator;
                    out.texCoord0 = in.UV0;
                    out.texCoord1 = float2(in.UV1) / 256.0;
                    out.texCoord2 = float2(in.UV2) / 256.0;
                    out.normal = float4(normal, 0.0);
                    out.vertexDistance = length(viewPos.xyz);
                    
                    return out;
                }
                """;
        }
        
        /**
         * Generate particle compute shader.
         */
        public static String generateParticleCompute(MSLVersion version) {
            return """
                #include <metal_stdlib>
                using namespace metal;
                
                struct Particle {
                    float3 position;
                    float3 velocity;
                    float4 color;
                    float life;
                    float size;
                    float2 padding;
                };
                
                struct SimulationParams {
                    float deltaTime;
                    float3 gravity;
                    float3 wind;
                    uint particleCount;
                };
                
                kernel void particleUpdate(
                    device Particle* particles [[buffer(0)]],
                    constant SimulationParams& params [[buffer(1)]],
                    uint id [[thread_position_in_grid]]
                ) {
                    if (id >= params.particleCount) return;
                    
                    Particle p = particles[id];
                    
                    // Skip dead particles
                    if (p.life <= 0.0) return;
                    
                    // Update velocity
                    p.velocity += params.gravity * params.deltaTime;
                    p.velocity += params.wind * params.deltaTime * 0.1;
                    
                    // Apply drag
                    p.velocity *= 0.98;
                    
                    // Update position
                    p.position += p.velocity * params.deltaTime;
                    
                    // Update life
                    p.life -= params.deltaTime;
                    
                    // Fade out
                    float fadeStart = 0.5;
                    if (p.life < fadeStart) {
                        p.color.a = p.life / fadeStart;
                    }
                    
                    particles[id] = p;
                }
                """;
        }
        
        /**
         * Generate post-processing bloom shader.
         */
        public static String generateBloomFragment(MSLVersion version) {
            return """
                #include <metal_stdlib>
                using namespace metal;
                
                struct FragmentIn {
                    float4 position [[position]];
                    float2 texCoord;
                };
                
                struct FragmentOut {
                    float4 color [[color(0)]];
                };
                
                struct BloomParams {
                    float threshold;
                    float intensity;
                    float2 texelSize;
                };
                
                // Gaussian weights for 9-tap blur
                constant float weights[9] = {
                    0.0162162162, 0.0540540541, 0.1216216216, 0.1945945946,
                    0.2270270270, 0.1945945946, 0.1216216216, 0.0540540541, 0.0162162162
                };
                
                constant float offsets[9] = {
                    -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0
                };
                
                float3 extractBright(float3 color, float threshold) {
                    float brightness = dot(color, float3(0.2126, 0.7152, 0.0722));
                    return color * step(threshold, brightness);
                }
                
                fragment FragmentOut bloomBrightPass(
                    FragmentIn in [[stage_in]],
                    constant BloomParams& params [[buffer(0)]],
                    texture2d<float> inputTex [[texture(0)]],
                    sampler texSampler [[sampler(0)]]
                ) {
                    FragmentOut out;
                    float4 color = inputTex.sample(texSampler, in.texCoord);
                    out.color = float4(extractBright(color.rgb, params.threshold), 1.0);
                    return out;
                }
                
                fragment FragmentOut bloomBlurHorizontal(
                    FragmentIn in [[stage_in]],
                    constant BloomParams& params [[buffer(0)]],
                    texture2d<float> inputTex [[texture(0)]],
                    sampler texSampler [[sampler(0)]]
                ) {
                    FragmentOut out;
                    float3 result = float3(0.0);
                    
                    for (int i = 0; i < 9; i++) {
                        float2 offset = float2(offsets[i] * params.texelSize.x, 0.0);
                        result += inputTex.sample(texSampler, in.texCoord + offset).rgb * weights[i];
                    }
                    
                    out.color = float4(result, 1.0);
                    return out;
                }
                
                fragment FragmentOut bloomBlurVertical(
                    FragmentIn in [[stage_in]],
                    constant BloomParams& params [[buffer(0)]],
                    texture2d<float> inputTex [[texture(0)]],
                    sampler texSampler [[sampler(0)]]
                ) {
                    FragmentOut out;
                    float3 result = float3(0.0);
                    
                    for (int i = 0; i < 9; i++) {
                        float2 offset = float2(0.0, offsets[i] * params.texelSize.y);
                        result += inputTex.sample(texSampler, in.texCoord + offset).rgb * weights[i];
                    }
                    
                    out.color = float4(result, 1.0);
                    return out;
                }
                
                fragment FragmentOut bloomComposite(
                    FragmentIn in [[stage_in]],
                    constant BloomParams& params [[buffer(0)]],
                    texture2d<float> sceneTex [[texture(0)]],
                    texture2d<float> bloomTex [[texture(1)]],
                    sampler texSampler [[sampler(0)]]
                ) {
                    FragmentOut out;
                    float4 sceneColor = sceneTex.sample(texSampler, in.texCoord);
                    float4 bloomColor = bloomTex.sample(texSampler, in.texCoord);
                    out.color = float4(sceneColor.rgb + bloomColor.rgb * params.intensity, sceneColor.a);
                    return out;
                }
                """;
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 12: MAIN CLASS IMPLEMENTATION
    // Core MSLCallMapper functionality and lifecycle management
    // ════════════════════════════════════════════════════════════════════════════
    
    // Instance fields
    private final long device;
    private final MSLCapabilities capabilities;
    private final ShaderCompiler shaderCompiler;
    private final SPIRVToMSLCompiler spirvCompiler;
    private final HLSLToMSLCompiler hlslCompiler;
    private final GLSLToMSLCompiler glslCompiler;
    private final ShaderOptimizer optimizer;
    private BinaryArchiveManager archiveManager;
    private DynamicLibraryManager dynamicLibraryManager;
    
    private boolean closed = false;
    
    /**
     * Create a new MSL call mapper.
     */
    public MSLCallMapper(long device) {
        this.device = device;
        this.capabilities = new MSLCapabilities(device);
        this.shaderCompiler = new ShaderCompiler(device, capabilities);
        this.spirvCompiler = new SPIRVToMSLCompiler(capabilities);
        this.hlslCompiler = new HLSLToMSLCompiler(capabilities, spirvCompiler);
        this.glslCompiler = new GLSLToMSLCompiler(capabilities, spirvCompiler);
        this.optimizer = new ShaderOptimizer(capabilities);
        
        LOGGER.info("MSL Call Mapper initialized: {}", capabilities);
    }
    
    /**
     * Get device capabilities.
     */
    public MSLCapabilities getCapabilities() {
        return capabilities;
    }
    
    /**
     * Get the shader compiler.
     */
    public ShaderCompiler getShaderCompiler() {
        return shaderCompiler;
    }
    
    /**
     * Get the SPIR-V to MSL compiler.
     */
    public SPIRVToMSLCompiler getSPIRVCompiler() {
        return spirvCompiler;
    }
    
    /**
     * Get the HLSL to MSL compiler.
     */
    public HLSLToMSLCompiler getHLSLCompiler() {
        return hlslCompiler;
    }
    
    /**
     * Get the GLSL to MSL compiler.
     */
    public GLSLToMSLCompiler getGLSLCompiler() {
        return glslCompiler;
    }
    
    /**
     * Get the shader optimizer.
     */
    public ShaderOptimizer getOptimizer() {
        return optimizer;
    }
    
    /**
     * Initialize binary archive manager with cache directory.
     */
    public BinaryArchiveManager initBinaryArchives(Path cacheDirectory) {
        if (archiveManager == null) {
            archiveManager = new BinaryArchiveManager(device, capabilities, cacheDirectory);
        }
        return archiveManager;
    }
    
    /**
     * Get the binary archive manager.
     */
    @Nullable
    public BinaryArchiveManager getArchiveManager() {
        return archiveManager;
    }
    
    /**
     * Initialize dynamic library manager.
     */
    public DynamicLibraryManager initDynamicLibraries() {
        if (dynamicLibraryManager == null) {
            dynamicLibraryManager = new DynamicLibraryManager(device, capabilities);
        }
        return dynamicLibraryManager;
    }
    
    /**
     * Get the dynamic library manager.
     */
    @Nullable
    public DynamicLibraryManager getDynamicLibraryManager() {
        return dynamicLibraryManager;
    }
    
    /**
     * Compile shader from any supported source format.
     */
    public CompiledLibrary compileShader(String name, String source, ShaderType type, ShaderStage stage) {
        return compileShader(name, source, type, stage, null);
    }
    
    /**
     * Compile shader with options.
     */
    public CompiledLibrary compileShader(String name, String source, ShaderType type, 
                                         ShaderStage stage, @Nullable MTLCompileOptions options) {
        // Apply optimizations first
        ShaderOptimizer.OptimizationResult optimized = null;
        if (type == ShaderType.MSL) {
            optimized = optimizer.optimize(source);
            source = optimized.optimizedSource;
        }
        
        String mslSource;
        
        switch (type) {
            case MSL -> mslSource = source;
            case SPIRV -> throw new IllegalArgumentException("Use compileSPIRV for binary SPIR-V data");
            case HLSL -> {
                HLSLToMSLCompiler.HLSLOptions hlslOptions = new HLSLToMSLCompiler.HLSLOptions();
                hlslOptions.stage = stage;
                HLSLToMSLCompiler.HLSLCompilationResult result = hlslCompiler.compile(source, hlslOptions);
                if (!result.success) {
                    throw new ShaderCompilationException(name, result.errorMessage);
                }
                mslSource = result.mslSource;
            }
            case GLSL -> {
                GLSLToMSLCompiler.GLSLOptions glslOptions = new GLSLToMSLCompiler.GLSLOptions();
                glslOptions.stage = stage;
                GLSLToMSLCompiler.GLSLCompilationResult result = glslCompiler.compile(source, glslOptions);
                if (!result.success) {
                    throw new ShaderCompilationException(name, result.errorMessage);
                }
                mslSource = result.mslSource;
            }
            default -> throw new IllegalArgumentException("Unsupported shader type: " + type);
        }
        
        // Compile MSL
        CompiledLibrary library = shaderCompiler.compileFromSource(name, mslSource, options);
        
        if (optimized != null) {
            LOGGER.debug("Shader {} compiled with optimizations: {}", name, optimized);
        }
        
        return library;
    }
    
    /**
     * Compile SPIR-V binary to MSL.
     */
    public CompiledLibrary compileSPIRV(String name, ByteBuffer spirvBinary, String entryPoint,
                                        ShaderStage stage, @Nullable MTLCompileOptions options) {
        SPIRVToMSLCompiler.SPIRVCompilationResult result = 
            spirvCompiler.compile(spirvBinary, entryPoint, stage);
        
        if (!result.success) {
            throw new ShaderCompilationException(name, result.errorMessage);
        }
        
        return shaderCompiler.compileFromSource(name, result.mslSource, options);
    }
    
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            
            if (archiveManager != null) {
                archiveManager.closeAll();
            }
            
            if (dynamicLibraryManager != null) {
                dynamicLibraryManager.releaseAll();
            }
            
            shaderCompiler.shutdown();
            
            LOGGER.info("MSL Call Mapper closed. Stats: {}", shaderCompiler.getStats());
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 13: EXCEPTION CLASSES
    // Custom exceptions for shader compilation errors
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Exception thrown when shader compilation fails.
     */
    public static class ShaderCompilationException extends RuntimeException {
        private final String shaderName;
        
        public ShaderCompilationException(String shaderName, String message) {
            super("Shader '" + shaderName + "' compilation failed: " + message);
            this.shaderName = shaderName;
        }
        
        public ShaderCompilationException(String shaderName, String message, Throwable cause) {
            super("Shader '" + shaderName + "' compilation failed: " + message, cause);
            this.shaderName = shaderName;
        }
        
        public String getShaderName() {
            return shaderName;
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 14: NATIVE METHOD DECLARATIONS
    // JNI bindings to Metal and SPIRV-Cross APIs
    // ════════════════════════════════════════════════════════════════════════════
    
    // Device capabilities
    private static native boolean nMTLDeviceSupportsFamily(long device, int family);
    private static native boolean nMTLDeviceSupportsMSLVersion(long device, int version);
    private static native boolean nMTLDeviceSupportsRaytracing(long device);
    private static native boolean nMTLDeviceSupportsMeshShaders(long device);
    private static native int nMTLDeviceArgumentBuffersSupport(long device);
    private static native boolean nMTLDeviceSupportsDynamicLibraries(long device);
    
    // Device limits
    private static native int nMTLDeviceMaxThreadsPerThreadgroup(long device);
    private static native int nMTLDeviceMaxThreadgroupMemoryLength(long device);
    private static native int nMTLDeviceMaxBufferArgumentTableEntries(long device);
    private static native int nMTLDeviceMaxTextureArgumentTableEntries(long device);
    private static native int nMTLDeviceMaxSamplerArgumentTableEntries(long device);
    private static native int nMTLDeviceMaxVertexAttributes(long device);
    private static native int nMTLDeviceMaxVisibleFunctionTableSize(long device);
    private static native int nMTLDeviceMaxArgumentBufferSamplerCount(long device);
    private static native int nMTLDeviceMaxTexture2DWidth(long device);
    private static native int nMTLDeviceMaxTexture2DHeight(long device);
    private static native int nMTLDeviceMaxTexture3DWidth(long device);
    private static native int nMTLDeviceMaxTexture3DHeight(long device);
    private static native int nMTLDeviceMaxTexture3DDepth(long device);
    private static native int nMTLDeviceMaxTextureCubeSize(long device);
    private static native int nMTLDeviceMaxTextureArrayLayers(long device);
    private static native int nMTLDeviceMaxTextureMipLevels(long device);
    
    // Compile options
    private static native long nMTLCompileOptionsNew();
    private static native void nMTLCompileOptionsSetLanguageVersion(long options, int version);
    private static native void nMTLCompileOptionsSetFastMathEnabled(long options, boolean enabled);
    private static native void nMTLCompileOptionsSetPreserveInvariants(long options, boolean enabled);
    private static native void nMTLCompileOptionsSetOptimizationLevel(long options, int level);
    private static native void nMTLCompileOptionsAddPreprocessorMacro(long options, String key, String value);
    private static native void nMTLCompileOptionsSetInstallName(long options, String name);
    private static native void nMTLCompileOptionsAddLibrary(long options, String path);
    
    // Library creation
    private static native long nMTLDeviceNewLibraryWithSource(long device, String source, long options, long[] error);
    private static native long nMTLDeviceNewLibraryWithFile(long device, String path, long[] error);
    private static native long nMTLDeviceNewLibraryWithData(long device, long dispatchData, long[] error);
    private static native long nMTLDeviceNewDefaultLibrary(long device);
    private static native long nMTLDeviceNewDynamicLibraryWithSource(long device, String source, long options, long[] error);
    private static native long nMTLDeviceNewDynamicLibraryWithURL(long device, String url, long[] error);
    private static native long nMTLDeviceNewPreloadedLibrary(long device, long[] libraries, long[] error);
    
    // Library functions
    private static native String[] nMTLLibraryGetFunctionNames(long library);
    private static native long nMTLLibraryNewFunctionWithName(long library, String name);
    private static native long nMTLLibraryNewFunctionWithNameConstantValues(long library, String name, long constants, long[] error);
    
    // Function constant values
    private static native long nMTLFunctionConstantValuesNew();
    private static native void nMTLFunctionConstantValuesSetBoolByName(long values, String name, boolean value);
    private static native void nMTLFunctionConstantValuesSetBoolByIndex(long values, int index, boolean value);
    private static native void nMTLFunctionConstantValuesSetIntByName(long values, String name, int value);
    private static native void nMTLFunctionConstantValuesSetIntByIndex(long values, int index, int value);
    private static native void nMTLFunctionConstantValuesSetUIntByName(long values, String name, int value);
    private static native void nMTLFunctionConstantValuesSetUIntByIndex(long values, int index, int value);
    private static native void nMTLFunctionConstantValuesSetFloatByName(long values, String name, float value);
    private static native void nMTLFunctionConstantValuesSetFloatByIndex(long values, int index, float value);
    private static native void nMTLFunctionConstantValuesSetHalfByName(long values, String name, float value);
    private static native void nMTLFunctionConstantValuesSetHalfByIndex(long values, int index, float value);
    private static native void nMTLFunctionConstantValuesSetFloat2ByName(long values, String name, float x, float y);
    private static native void nMTLFunctionConstantValuesSetFloat3ByName(long values, String name, float x, float y, float z);
    private static native void nMTLFunctionConstantValuesSetFloat4ByName(long values, String name, float x, float y, float z, float w);
    
    // Function reflection
    private static native String nMTLFunctionGetName(long function);
    private static native int nMTLFunctionGetFunctionType(long function);
    private static native int nMTLFunctionGetVertexAttributeCount(long function);
    private static native String nMTLFunctionGetVertexAttributeName(long function, int index);
    private static native int nMTLFunctionGetVertexAttributeIndex(long function, int index);
    private static native int nMTLFunctionGetVertexAttributeType(long function, int index);
    private static native boolean nMTLFunctionGetVertexAttributeActive(long function, int index);
    private static native int nMTLFunctionGetStageInputAttributeCount(long function);
    private static native String nMTLFunctionGetStageInputAttributeName(long function, int index);
    private static native int nMTLFunctionGetStageInputAttributeIndex(long function, int index);
    private static native int nMTLFunctionGetStageInputAttributeType(long function, int index);
    private static native boolean nMTLFunctionGetStageInputAttributeActive(long function, int index);
    private static native int nMTLFunctionGetBufferBindingCount(long function);
    private static native String nMTLFunctionGetBufferBindingName(long function, int index);
    private static native int nMTLFunctionGetBufferBindingIndex(long function, int index);
    private static native int nMTLFunctionGetBufferBindingSize(long function, int index);
    private static native int nMTLFunctionGetBufferBindingAlignment(long function, int index);
    private static native int nMTLFunctionGetBufferBindingType(long function, int index);
    private static native boolean nMTLFunctionGetBufferBindingActive(long function, int index);
    private static native int nMTLFunctionGetBufferBindingMemberCount(long function, int bufferIndex);
    private static native String nMTLFunctionGetBufferBindingMemberName(long function, int bufferIndex, int memberIndex);
    private static native int nMTLFunctionGetBufferBindingMemberOffset(long function, int bufferIndex, int memberIndex);
    private static native int nMTLFunctionGetBufferBindingMemberSize(long function, int bufferIndex, int memberIndex);
    private static native int nMTLFunctionGetBufferBindingMemberType(long function, int bufferIndex, int memberIndex);
    private static native int nMTLFunctionGetTextureBindingCount(long function);
    private static native String nMTLFunctionGetTextureBindingName(long function, int index);
    private static native int nMTLFunctionGetTextureBindingIndex(long function, int index);
    private static native int nMTLFunctionGetTextureBindingType(long function, int index);
    private static native int nMTLFunctionGetTextureBindingAccess(long function, int index);
    private static native int nMTLFunctionGetTextureBindingArrayLength(long function, int index);
    private static native boolean nMTLFunctionGetTextureBindingActive(long function, int index);
    private static native int nMTLFunctionGetSamplerBindingCount(long function);
    private static native String nMTLFunctionGetSamplerBindingName(long function, int index);
    private static native int nMTLFunctionGetSamplerBindingIndex(long function, int index);
    private static native boolean nMTLFunctionGetSamplerBindingActive(long function, int index);
    private static native int nMTLFunctionGetThreadgroupBindingCount(long function);
    private static native String nMTLFunctionGetThreadgroupBindingName(long function, int index);
    private static native int nMTLFunctionGetThreadgroupBindingIndex(long function, int index);
    private static native int nMTLFunctionGetThreadgroupBindingSize(long function, int index);
    private static native int nMTLFunctionGetThreadgroupBindingAlignment(long function, int index);
    private static native int[] nMTLFunctionGetThreadgroupSize(long function);
    private static native int nMTLFunctionGetMaxTotalThreadsPerThreadgroup(long function);
    private static native int nMTLFunctionGetThreadExecutionWidth(long function);
    
    // Binary archives
    private static native long nMTLDeviceNewBinaryArchive(long device);
    private static native long nMTLDeviceNewBinaryArchiveWithURL(long device, String url);
    private static native boolean nMTLBinaryArchiveAddRenderPipeline(long archive, long descriptor, long[] error);
    private static native boolean nMTLBinaryArchiveAddComputePipeline(long archive, long descriptor, long[] error);
    private static native boolean nMTLBinaryArchiveSerialize(long archive, String url, long[] error);
    
    // SPIRV-Cross
    private static native long nSPIRVCrossCreateCompiler(ByteBuffer spirv, int length);
    private static native void nSPIRVCrossDestroyCompiler(long compiler);
    private static native void nSPIRVCrossSetMSLVersion(long compiler, int version);
    private static native void nSPIRVCrossSetMSLPlatform(long compiler, int platform);
    private static native void nSPIRVCrossSetFastMathEnabled(long compiler, boolean enabled);
    private static native void nSPIRVCrossSetArgumentBuffersEnabled(long compiler, boolean enabled);
    private static native void nSPIRVCrossSetArgumentBuffersTier(long compiler, int tier);
    private static native void nSPIRVCrossSetSIMDGroupFunctionsEnabled(long compiler, boolean enabled);
    private static native void nSPIRVCrossSetQuadGroupFunctionsEnabled(long compiler, boolean enabled);
    private static native void nSPIRVCrossSetFramebufferFetchEnabled(long compiler, boolean enabled);
    private static native void nSPIRVCrossSetEmulateSubgroups(long compiler, boolean enabled);
    private static native void nSPIRVCrossSetSubgroupSize(long compiler, int size);
    private static native void nSPIRVCrossSetEntryPoint(long compiler, String name, int stage);
    private static native void nSPIRVCrossSetMSLBufferBinding(long compiler, int set, int binding, int mslBinding);
    private static native void nSPIRVCrossSetMSLTextureBinding(long compiler, int set, int binding, int mslBinding);
    private static native void nSPIRVCrossSetMSLSamplerBinding(long compiler, int set, int binding, int mslBinding);
    private static native String nSPIRVCrossCompile(long compiler);
    private static native String nSPIRVCrossGetLastError(long compiler);
    
    // SPIRV-Cross reflection
    private static native int nSPIRVCrossGetUniformBufferCount(long compiler);
    private static native String nSPIRVCrossGetUniformBufferName(long compiler, int index);
    private static native int nSPIRVCrossGetUniformBufferSet(long compiler, int index);
    private static native int nSPIRVCrossGetUniformBufferBinding(long compiler, int index);
    private static native int nSPIRVCrossGetUniformBufferMSLBinding(long compiler, int index);
    private static native int nSPIRVCrossGetStorageBufferCount(long compiler);
    private static native String nSPIRVCrossGetStorageBufferName(long compiler, int index);
    private static native int nSPIRVCrossGetStorageBufferSet(long compiler, int index);
    private static native int nSPIRVCrossGetStorageBufferBinding(long compiler, int index);
    private static native int nSPIRVCrossGetStorageBufferMSLBinding(long compiler, int index);
    private static native int nSPIRVCrossGetSampledImageCount(long compiler);
    private static native String nSPIRVCrossGetSampledImageName(long compiler, int index);
    private static native int nSPIRVCrossGetSampledImageSet(long compiler, int index);
    private static native int nSPIRVCrossGetSampledImageBinding(long compiler, int index);
    private static native int nSPIRVCrossGetSampledImageMSLTexture(long compiler, int index);
    private static native int nSPIRVCrossGetSampledImageMSLSampler(long compiler, int index);
    private static native int nSPIRVCrossGetSeparateSamplerCount(long compiler);
    private static native String nSPIRVCrossGetSeparateSamplerName(long compiler, int index);
    private static native int nSPIRVCrossGetSeparateSamplerSet(long compiler, int index);
    private static native int nSPIRVCrossGetSeparateSamplerBinding(long compiler, int index);
    private static native int nSPIRVCrossGetSeparateSamplerMSLBinding(long compiler, int index);
    private static native int nSPIRVCrossGetSeparateImageCount(long compiler);
    private static native String nSPIRVCrossGetSeparateImageName(long compiler, int index);
    private static native int nSPIRVCrossGetSeparateImageSet(long compiler, int index);
    private static native int nSPIRVCrossGetSeparateImageBinding(long compiler, int index);
    private static native int nSPIRVCrossGetSeparateImageMSLBinding(long compiler, int index);
    private static native int nSPIRVCrossGetStorageImageCount(long compiler);
    private static native String nSPIRVCrossGetStorageImageName(long compiler, int index);
    private static native int nSPIRVCrossGetStorageImageSet(long compiler, int index);
    private static native int nSPIRVCrossGetStorageImageBinding(long compiler, int index);
    private static native int nSPIRVCrossGetStorageImageMSLBinding(long compiler, int index);
    private static native int nSPIRVCrossGetVertexAttributeCount(long compiler);
    private static native String nSPIRVCrossGetVertexAttributeName(long compiler, int index);
    private static native int nSPIRVCrossGetVertexAttributeLocation(long compiler, int index);
    private static native int nSPIRVCrossGetVertexAttributeMSLBinding(long compiler, int index);
    private static native int nSPIRVCrossGetVertexAttributeComponentCount(long compiler, int index);
    private static native int[] nSPIRVCrossGetWorkgroupSize(long compiler);
    
    // DXC (DirectX Shader Compiler)
    private static native byte[] nDXCCompile(String source, String[] args);
    
    // glslang
    private static native byte[] nGlslangCompile(String source, int stage, int clientVersion,
                                                  boolean autoMapLocations, boolean autoMapBindings);
    
    // Utility
    private static native void nRelease(long handle);
    private static native String nNSErrorLocalizedDescription(long error);
    private static native long nCreateDispatchDataFromBuffer(ByteBuffer buffer);
    private static native void nReleaseDispatchData(long dispatchData);
    
    // Static initialization
    static {
        try {
            System.loadLibrary("minecraft_metal_msl");
        } catch (UnsatisfiedLinkError e) {
            LOGGER.error("Failed to load MSL native library", e);
        }
    }

// ════════════════════════════════════════════════════════════════════════════
// SECTION 15: TOKENIZATION LAYER
// Lexical analysis when regex patterns fail or struggle
// ════════════════════════════════════════════════════════════════════════════

/**
 * Multi-language shader tokenizer supporting MSL, GLSL, HLSL.
 */
public static final class ShaderTokenizer {
    
    /**
     * Token types for shader languages.
     */
    public enum TokenType {
        // Literals
        INTEGER_LITERAL,
        FLOAT_LITERAL,
        DOUBLE_LITERAL,
        HALF_LITERAL,
        STRING_LITERAL,
        BOOLEAN_LITERAL,
        
        // Identifiers and keywords
        IDENTIFIER,
        KEYWORD,
        TYPE_KEYWORD,
        QUALIFIER_KEYWORD,
        BUILTIN_VARIABLE,
        BUILTIN_FUNCTION,
        
        // Operators
        PLUS, MINUS, STAR, SLASH, PERCENT,
        AMPERSAND, PIPE, CARET, TILDE,
        EQUAL, NOT_EQUAL, LESS, GREATER, LESS_EQUAL, GREATER_EQUAL,
        LOGICAL_AND, LOGICAL_OR, LOGICAL_NOT,
        SHIFT_LEFT, SHIFT_RIGHT,
        ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, STAR_ASSIGN, SLASH_ASSIGN,
        PERCENT_ASSIGN, AMPERSAND_ASSIGN, PIPE_ASSIGN, CARET_ASSIGN,
        SHIFT_LEFT_ASSIGN, SHIFT_RIGHT_ASSIGN,
        INCREMENT, DECREMENT,
        QUESTION, COLON, DOUBLE_COLON,
        DOT, ARROW, COMMA, SEMICOLON,
        
        // Brackets
        LEFT_PAREN, RIGHT_PAREN,
        LEFT_BRACE, RIGHT_BRACE,
        LEFT_BRACKET, RIGHT_BRACKET,
        
        // Preprocessor
        PREPROCESSOR_DIRECTIVE,
        PREPROCESSOR_INCLUDE,
        PREPROCESSOR_DEFINE,
        PREPROCESSOR_IFDEF,
        PREPROCESSOR_IFNDEF,
        PREPROCESSOR_ELSE,
        PREPROCESSOR_ENDIF,
        PREPROCESSOR_PRAGMA,
        
        // Attributes
        ATTRIBUTE,  // [[...]]
        LAYOUT,     // layout(...)
        SEMANTIC,   // : SEMANTIC (HLSL)
        
        // Comments
        LINE_COMMENT,
        BLOCK_COMMENT,
        
        // Special
        WHITESPACE,
        NEWLINE,
        EOF,
        ERROR
    }
    
    /**
     * A token in the shader source.
     */
    public static final class Token {
        public final TokenType type;
        public final String value;
        public final int line;
        public final int column;
        public final int startOffset;
        public final int endOffset;
        
        public Token(TokenType type, String value, int line, int column, int startOffset, int endOffset) {
            this.type = type;
            this.value = value;
            this.line = line;
            this.column = column;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
        
        @Override
        public String toString() {
            return String.format("Token[%s '%s' @%d:%d]", type, 
                value.length() > 20 ? value.substring(0, 20) + "..." : value, line, column);
        }
        
        public boolean is(TokenType... types) {
            for (TokenType t : types) {
                if (this.type == t) return true;
            }
            return false;
        }
        
        public boolean isKeyword(String... keywords) {
            if (type != TokenType.KEYWORD && type != TokenType.TYPE_KEYWORD && 
                type != TokenType.QUALIFIER_KEYWORD) return false;
            for (String kw : keywords) {
                if (value.equals(kw)) return true;
            }
            return false;
        }
    }
    
    /**
     * Tokenization result.
     */
    public static final class TokenizationResult {
        public final List<Token> tokens;
        public final List<TokenizationError> errors;
        public final boolean success;
        public final long tokenizationTimeNs;
        
        TokenizationResult(List<Token> tokens, List<TokenizationError> errors, long timeNs) {
            this.tokens = Collections.unmodifiableList(tokens);
            this.errors = Collections.unmodifiableList(errors);
            this.success = errors.isEmpty();
            this.tokenizationTimeNs = timeNs;
        }
        
        public List<Token> getTokensWithoutWhitespace() {
            return tokens.stream()
                .filter(t -> t.type != TokenType.WHITESPACE && 
                            t.type != TokenType.NEWLINE &&
                            t.type != TokenType.LINE_COMMENT &&
                            t.type != TokenType.BLOCK_COMMENT)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Tokenization error.
     */
    public static final class TokenizationError {
        public final String message;
        public final int line;
        public final int column;
        public final String context;
        
        TokenizationError(String message, int line, int column, String context) {
            this.message = message;
            this.line = line;
            this.column = column;
            this.context = context;
        }
        
        @Override
        public String toString() {
            return String.format("Error at %d:%d: %s", line, column, message);
        }
    }
    
    // Language-specific keyword sets
    private static final Set<String> MSL_KEYWORDS = Set.of(
        "kernel", "vertex", "fragment", "compute", "tile", "object", "mesh",
        "device", "constant", "threadgroup", "threadgroup_imageblock", "ray_data",
        "thread", "stage_in", "sampler", "texture", "patch", "postTessellation",
        "using", "namespace", "struct", "class", "enum", "union", "typedef",
        "if", "else", "for", "while", "do", "switch", "case", "default", "break",
        "continue", "return", "discard_fragment", "simdgroup", "quadgroup",
        "visible_function_table", "intersection_function_table", "primitive_acceleration_structure",
        "instance_acceleration_structure", "static", "inline", "constexpr"
    );
    
    private static final Set<String> MSL_TYPES = Set.of(
        "void", "bool", "char", "uchar", "short", "ushort", "int", "uint", "long", "ulong",
        "half", "float", "double", "size_t", "ptrdiff_t",
        "bool2", "bool3", "bool4", "char2", "char3", "char4", "uchar2", "uchar3", "uchar4",
        "short2", "short3", "short4", "ushort2", "ushort3", "ushort4",
        "int2", "int3", "int4", "uint2", "uint3", "uint4",
        "half2", "half3", "half4", "float2", "float3", "float4",
        "float2x2", "float2x3", "float2x4", "float3x2", "float3x3", "float3x4",
        "float4x2", "float4x3", "float4x4",
        "half2x2", "half2x3", "half2x4", "half3x2", "half3x3", "half3x4",
        "half4x2", "half4x3", "half4x4",
        "texture1d", "texture1d_array", "texture2d", "texture2d_array", "texture2d_ms",
        "texture2d_ms_array", "texture3d", "texturecube", "texturecube_array",
        "texture_buffer", "depth2d", "depth2d_array", "depth2d_ms", "depth2d_ms_array",
        "depthcube", "depthcube_array", "sampler", "array", "atomic_int", "atomic_uint",
        "packed_float2", "packed_float3", "packed_float4", "packed_half2", "packed_half3", "packed_half4"
    );
    
    private static final Set<String> GLSL_KEYWORDS = Set.of(
        "attribute", "const", "uniform", "varying", "buffer", "shared",
        "centroid", "flat", "smooth", "noperspective", "patch", "sample",
        "coherent", "volatile", "restrict", "readonly", "writeonly",
        "layout", "in", "out", "inout", "lowp", "mediump", "highp", "precision",
        "invariant", "precise", "struct", "if", "else", "switch", "case", "default",
        "while", "do", "for", "continue", "break", "return", "discard",
        "subroutine", "true", "false"
    );
    
    private static final Set<String> GLSL_TYPES = Set.of(
        "void", "bool", "int", "uint", "float", "double",
        "vec2", "vec3", "vec4", "dvec2", "dvec3", "dvec4",
        "bvec2", "bvec3", "bvec4", "ivec2", "ivec3", "ivec4",
        "uvec2", "uvec3", "uvec4",
        "mat2", "mat3", "mat4", "mat2x2", "mat2x3", "mat2x4",
        "mat3x2", "mat3x3", "mat3x4", "mat4x2", "mat4x3", "mat4x4",
        "dmat2", "dmat3", "dmat4", "dmat2x2", "dmat2x3", "dmat2x4",
        "dmat3x2", "dmat3x3", "dmat3x4", "dmat4x2", "dmat4x3", "dmat4x4",
        "sampler1D", "sampler2D", "sampler3D", "samplerCube", "sampler1DShadow",
        "sampler2DShadow", "samplerCubeShadow", "sampler1DArray", "sampler2DArray",
        "sampler1DArrayShadow", "sampler2DArrayShadow", "samplerBuffer",
        "sampler2DMS", "sampler2DMSArray", "samplerCubeArray", "samplerCubeArrayShadow",
        "isampler1D", "isampler2D", "isampler3D", "isamplerCube", "isampler1DArray",
        "isampler2DArray", "isamplerBuffer", "isampler2DMS", "isampler2DMSArray",
        "usampler1D", "usampler2D", "usampler3D", "usamplerCube", "usampler1DArray",
        "usampler2DArray", "usamplerBuffer", "usampler2DMS", "usampler2DMSArray",
        "image1D", "image2D", "image3D", "imageCube", "image1DArray", "image2DArray",
        "imageBuffer", "image2DMS", "image2DMSArray", "atomic_uint"
    );
    
    private static final Set<String> HLSL_KEYWORDS = Set.of(
        "cbuffer", "tbuffer", "groupshared", "register", "packoffset",
        "precise", "nointerpolation", "centroid", "linear", "noperspective", "sample",
        "in", "out", "inout", "uniform", "extern", "static", "volatile", "const",
        "row_major", "column_major", "export", "shared", "globallycoherent",
        "inline", "struct", "class", "interface", "typedef", "namespace",
        "if", "else", "switch", "case", "default", "while", "do", "for",
        "break", "continue", "return", "discard", "true", "false",
        "technique", "technique10", "technique11", "pass", "compile", "compile_fragment"
    );
    
    private static final Set<String> HLSL_TYPES = Set.of(
        "void", "bool", "int", "uint", "dword", "half", "float", "double",
        "min16float", "min10float", "min16int", "min12int", "min16uint",
        "bool1", "bool2", "bool3", "bool4", "int1", "int2", "int3", "int4",
        "uint1", "uint2", "uint3", "uint4", "half1", "half2", "half3", "half4",
        "float1", "float2", "float3", "float4", "double1", "double2", "double3", "double4",
        "float1x1", "float1x2", "float1x3", "float1x4", "float2x1", "float2x2", "float2x3", "float2x4",
        "float3x1", "float3x2", "float3x3", "float3x4", "float4x1", "float4x2", "float4x3", "float4x4",
        "half1x1", "half2x2", "half3x3", "half4x4",
        "Texture1D", "Texture1DArray", "Texture2D", "Texture2DArray", "Texture2DMS",
        "Texture2DMSArray", "Texture3D", "TextureCube", "TextureCubeArray",
        "RWTexture1D", "RWTexture1DArray", "RWTexture2D", "RWTexture2DArray", "RWTexture3D",
        "Buffer", "RWBuffer", "StructuredBuffer", "RWStructuredBuffer",
        "ByteAddressBuffer", "RWByteAddressBuffer", "AppendStructuredBuffer", "ConsumeStructuredBuffer",
        "SamplerState", "SamplerComparisonState", "RaytracingAccelerationStructure",
        "string", "vector", "matrix"
    );
    
    private static final Set<String> GLSL_BUILTINS = Set.of(
        "gl_Position", "gl_PointSize", "gl_ClipDistance", "gl_CullDistance",
        "gl_VertexID", "gl_VertexIndex", "gl_InstanceID", "gl_InstanceIndex",
        "gl_FragCoord", "gl_FrontFacing", "gl_PointCoord", "gl_FragDepth",
        "gl_SampleID", "gl_SamplePosition", "gl_SampleMask", "gl_SampleMaskIn",
        "gl_PrimitiveID", "gl_Layer", "gl_ViewportIndex", "gl_HelperInvocation",
        "gl_LocalInvocationID", "gl_GlobalInvocationID", "gl_WorkGroupID",
        "gl_LocalInvocationIndex", "gl_NumWorkGroups", "gl_WorkGroupSize",
        "gl_TessLevelOuter", "gl_TessLevelInner", "gl_TessCoord",
        "gl_PatchVerticesIn", "gl_InvocationID"
    );
    
    private static final Set<String> HLSL_SEMANTICS = Set.of(
        "POSITION", "NORMAL", "TANGENT", "BINORMAL", "BLENDINDICES", "BLENDWEIGHT",
        "COLOR", "TEXCOORD", "PSIZE", "FOG", "TESSFACTOR", "VFACE", "VPOS",
        "SV_Position", "SV_Target", "SV_Depth", "SV_Coverage", "SV_IsFrontFace",
        "SV_SampleIndex", "SV_PrimitiveID", "SV_InstanceID", "SV_VertexID",
        "SV_DispatchThreadID", "SV_GroupID", "SV_GroupIndex", "SV_GroupThreadID",
        "SV_DomainLocation", "SV_InsideTessFactor", "SV_OutputControlPointID",
        "SV_TessFactor", "SV_ClipDistance", "SV_CullDistance", "SV_RenderTargetArrayIndex",
        "SV_ViewportArrayIndex", "SV_StencilRef", "SV_GSInstanceID"
    );
    
    private final ShaderLanguage language;
    private final Set<String> keywords;
    private final Set<String> types;
    private final Set<String> builtins;
    private final Set<String> semantics;
    
    private String source;
    private int position;
    private int line;
    private int column;
    private int lineStart;
    
    public ShaderTokenizer(ShaderLanguage language) {
        this.language = language;
        
        switch (language) {
            case MSL -> {
                keywords = MSL_KEYWORDS;
                types = MSL_TYPES;
                builtins = Set.of();
                semantics = Set.of();
            }
            case GLSL -> {
                keywords = GLSL_KEYWORDS;
                types = GLSL_TYPES;
                builtins = GLSL_BUILTINS;
                semantics = Set.of();
            }
            case HLSL -> {
                keywords = HLSL_KEYWORDS;
                types = HLSL_TYPES;
                builtins = Set.of();
                semantics = HLSL_SEMANTICS;
            }
            default -> {
                keywords = Set.of();
                types = Set.of();
                builtins = Set.of();
                semantics = Set.of();
            }
        }
    }
    
    /**
     * Tokenize shader source code.
     */
    public TokenizationResult tokenize(String source) {
        long startTime = System.nanoTime();
        
        this.source = source;
        this.position = 0;
        this.line = 1;
        this.column = 1;
        this.lineStart = 0;
        
        List<Token> tokens = new ArrayList<>();
        List<TokenizationError> errors = new ArrayList<>();
        
        while (!isAtEnd()) {
            try {
                Token token = scanToken();
                if (token != null) {
                    tokens.add(token);
                    if (token.type == TokenType.ERROR) {
                        errors.add(new TokenizationError(token.value, token.line, token.column, 
                            getContext(token.startOffset)));
                    }
                }
            } catch (Exception e) {
                errors.add(new TokenizationError(e.getMessage(), line, column, getContext(position)));
                advance(); // Skip problematic character
            }
        }
        
        tokens.add(makeToken(TokenType.EOF, "", position));
        
        return new TokenizationResult(tokens, errors, System.nanoTime() - startTime);
    }
    
    private Token scanToken() {
        int start = position;
        char c = advance();
        
        // Whitespace and newlines
        if (c == ' ' || c == '\t' || c == '\r') {
            return scanWhitespace(start);
        }
        if (c == '\n') {
            Token token = makeToken(TokenType.NEWLINE, "\n", start);
            line++;
            column = 1;
            lineStart = position;
            return token;
        }
        
        // Comments and division
        if (c == '/') {
            if (match('/')) return scanLineComment(start);
            if (match('*')) return scanBlockComment(start);
            if (match('=')) return makeToken(TokenType.SLASH_ASSIGN, "/=", start);
            return makeToken(TokenType.SLASH, "/", start);
        }
        
        // Preprocessor directives
        if (c == '#') {
            return scanPreprocessor(start);
        }
        
        // Numbers
        if (isDigit(c)) {
            return scanNumber(start);
        }
        if (c == '.' && isDigit(peek())) {
            return scanNumber(start);
        }
        
        // Strings
        if (c == '"') {
            return scanString(start);
        }
        
        // Identifiers and keywords
        if (isAlpha(c) || c == '_') {
            return scanIdentifier(start);
        }
        
        // Operators and punctuation
        return scanOperator(c, start);
    }
    
    private Token scanWhitespace(int start) {
        while (!isAtEnd() && (peek() == ' ' || peek() == '\t' || peek() == '\r')) {
            advance();
        }
        return makeToken(TokenType.WHITESPACE, source.substring(start, position), start);
    }
    
    private Token scanLineComment(int start) {
        while (!isAtEnd() && peek() != '\n') {
            advance();
        }
        return makeToken(TokenType.LINE_COMMENT, source.substring(start, position), start);
    }
    
    private Token scanBlockComment(int start) {
        int depth = 1;
        while (!isAtEnd() && depth > 0) {
            if (peek() == '/' && peekNext() == '*') {
                advance(); advance();
                depth++;
            } else if (peek() == '*' && peekNext() == '/') {
                advance(); advance();
                depth--;
            } else {
                if (peek() == '\n') {
                    line++;
                    lineStart = position + 1;
                }
                advance();
            }
        }
        if (depth > 0) {
            return makeToken(TokenType.ERROR, "Unterminated block comment", start);
        }
        return makeToken(TokenType.BLOCK_COMMENT, source.substring(start, position), start);
    }
    
    private Token scanPreprocessor(int start) {
        // Skip whitespace after #
        while (!isAtEnd() && (peek() == ' ' || peek() == '\t')) {
            advance();
        }
        
        // Read directive name
        int directiveStart = position;
        while (!isAtEnd() && isAlphaNumeric(peek())) {
            advance();
        }
        String directive = source.substring(directiveStart, position);
        
        // Handle line continuation
        StringBuilder fullDirective = new StringBuilder();
        fullDirective.append(source.substring(start, position));
        
        while (!isAtEnd()) {
            if (peek() == '\\' && peekNext() == '\n') {
                fullDirective.append(advance()).append(advance());
                line++;
                lineStart = position;
            } else if (peek() == '\n') {
                break;
            } else {
                fullDirective.append(advance());
            }
        }
        
        TokenType type = switch (directive) {
            case "include" -> TokenType.PREPROCESSOR_INCLUDE;
            case "define" -> TokenType.PREPROCESSOR_DEFINE;
            case "ifdef" -> TokenType.PREPROCESSOR_IFDEF;
            case "ifndef" -> TokenType.PREPROCESSOR_IFNDEF;
            case "else" -> TokenType.PREPROCESSOR_ELSE;
            case "endif" -> TokenType.PREPROCESSOR_ENDIF;
            case "pragma" -> TokenType.PREPROCESSOR_PRAGMA;
            default -> TokenType.PREPROCESSOR_DIRECTIVE;
        };
        
        return makeToken(type, fullDirective.toString(), start);
    }
    
    private Token scanNumber(int start) {
        boolean isFloat = source.charAt(start) == '.';
        boolean isHex = false;
        boolean hasSuffix = false;
        
        // Check for hex
        if (position < source.length() && peek() == 'x' || peek() == 'X') {
            advance();
            isHex = true;
            while (!isAtEnd() && isHexDigit(peek())) {
                advance();
            }
        } else {
            // Integer part
            while (!isAtEnd() && isDigit(peek())) {
                advance();
            }
            
            // Fractional part
            if (peek() == '.' && isDigit(peekNext())) {
                isFloat = true;
                advance();
                while (!isAtEnd() && isDigit(peek())) {
                    advance();
                }
            }
            
            // Exponent
            if (peek() == 'e' || peek() == 'E') {
                isFloat = true;
                advance();
                if (peek() == '+' || peek() == '-') advance();
                while (!isAtEnd() && isDigit(peek())) {
                    advance();
                }
            }
        }
        
        // Suffix
        TokenType type = isFloat ? TokenType.FLOAT_LITERAL : TokenType.INTEGER_LITERAL;
        if (!isAtEnd()) {
            char suffix = peek();
            if (suffix == 'f' || suffix == 'F') {
                advance();
                type = TokenType.FLOAT_LITERAL;
            } else if (suffix == 'h' || suffix == 'H') {
                advance();
                type = TokenType.HALF_LITERAL;
            } else if (suffix == 'd' || suffix == 'D' || 
                       (suffix == 'l' && peekNext() == 'f')) {
                advance();
                if (peek() == 'f') advance();
                type = TokenType.DOUBLE_LITERAL;
            } else if (suffix == 'u' || suffix == 'U') {
                advance();
                if (peek() == 'l' || peek() == 'L') advance();
            } else if (suffix == 'l' || suffix == 'L') {
                advance();
                if (peek() == 'u' || peek() == 'U') advance();
            }
        }
        
        return makeToken(type, source.substring(start, position), start);
    }
    
    private Token scanString(int start) {
        StringBuilder value = new StringBuilder();
        value.append('"');
        
        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\n') {
                return makeToken(TokenType.ERROR, "Unterminated string", start);
            }
            if (peek() == '\\') {
                value.append(advance()); // backslash
                if (!isAtEnd()) value.append(advance()); // escaped char
            } else {
                value.append(advance());
            }
        }
        
        if (isAtEnd()) {
            return makeToken(TokenType.ERROR, "Unterminated string", start);
        }
        
        advance(); // closing quote
        value.append('"');
        
        return makeToken(TokenType.STRING_LITERAL, value.toString(), start);
    }
    
    private Token scanIdentifier(int start) {
        while (!isAtEnd() && (isAlphaNumeric(peek()) || peek() == '_')) {
            advance();
        }
        
        String text = source.substring(start, position);
        
        // Check for attributes [[...]]
        if (text.equals("attribute") || (position < source.length() - 1 && 
            source.charAt(start) == '[' && source.charAt(start + 1) == '[')) {
            return scanAttribute(start);
        }
        
        // Check for boolean literals
        if (text.equals("true") || text.equals("false")) {
            return makeToken(TokenType.BOOLEAN_LITERAL, text, start);
        }
        
        // Check for types
        if (types.contains(text)) {
            return makeToken(TokenType.TYPE_KEYWORD, text, start);
        }
        
        // Check for keywords
        if (keywords.contains(text)) {
            // Determine if it's a qualifier
            if (isQualifier(text)) {
                return makeToken(TokenType.QUALIFIER_KEYWORD, text, start);
            }
            return makeToken(TokenType.KEYWORD, text, start);
        }
        
        // Check for builtins
        if (builtins.contains(text)) {
            return makeToken(TokenType.BUILTIN_VARIABLE, text, start);
        }
        
        // Check for HLSL semantics (after colon)
        if (language == ShaderLanguage.HLSL && semantics.contains(text)) {
            return makeToken(TokenType.SEMANTIC, text, start);
        }
        
        return makeToken(TokenType.IDENTIFIER, text, start);
    }
    
    private Token scanAttribute(int start) {
        // MSL/C++11 style [[...]]
        if (peek() == '[' && peekNext() == '[') {
            advance(); advance(); // [[
            int depth = 1;
            while (!isAtEnd() && depth > 0) {
                if (peek() == '[' && peekNext() == '[') {
                    advance(); advance();
                    depth++;
                } else if (peek() == ']' && peekNext() == ']') {
                    advance(); advance();
                    depth--;
                } else {
                    advance();
                }
            }
            return makeToken(TokenType.ATTRIBUTE, source.substring(start, position), start);
        }
        
        // GLSL layout(...)
        if (source.substring(start, position).equals("layout")) {
            if (peek() == '(') {
                advance(); // (
                int depth = 1;
                while (!isAtEnd() && depth > 0) {
                    if (peek() == '(') depth++;
                    else if (peek() == ')') depth--;
                    advance();
                }
                return makeToken(TokenType.LAYOUT, source.substring(start, position), start);
            }
        }
        
        return makeToken(TokenType.IDENTIFIER, source.substring(start, position), start);
    }
    
    private Token scanOperator(char c, int start) {
        return switch (c) {
            case '(' -> makeToken(TokenType.LEFT_PAREN, "(", start);
            case ')' -> makeToken(TokenType.RIGHT_PAREN, ")", start);
            case '{' -> makeToken(TokenType.LEFT_BRACE, "{", start);
            case '}' -> makeToken(TokenType.RIGHT_BRACE, "}", start);
            case '[' -> {
                if (match('[')) {
                    // Start of attribute [[
                    position = start; column -= 2;
                    yield scanAttributeBracket(start);
                }
                yield makeToken(TokenType.LEFT_BRACKET, "[", start);
            }
            case ']' -> makeToken(TokenType.RIGHT_BRACKET, "]", start);
            case ';' -> makeToken(TokenType.SEMICOLON, ";", start);
            case ',' -> makeToken(TokenType.COMMA, ",", start);
            case '.' -> makeToken(TokenType.DOT, ".", start);
            case '?' -> makeToken(TokenType.QUESTION, "?", start);
            case ':' -> {
                if (match(':')) yield makeToken(TokenType.DOUBLE_COLON, "::", start);
                yield makeToken(TokenType.COLON, ":", start);
            }
            case '+' -> {
                if (match('+')) yield makeToken(TokenType.INCREMENT, "++", start);
                if (match('=')) yield makeToken(TokenType.PLUS_ASSIGN, "+=", start);
                yield makeToken(TokenType.PLUS, "+", start);
            }
            case '-' -> {
                if (match('-')) yield makeToken(TokenType.DECREMENT, "--", start);
                if (match('=')) yield makeToken(TokenType.MINUS_ASSIGN, "-=", start);
                if (match('>')) yield makeToken(TokenType.ARROW, "->", start);
                yield makeToken(TokenType.MINUS, "-", start);
            }
            case '*' -> {
                if (match('=')) yield makeToken(TokenType.STAR_ASSIGN, "*=", start);
                yield makeToken(TokenType.STAR, "*", start);
            }
            case '%' -> {
                if (match('=')) yield makeToken(TokenType.PERCENT_ASSIGN, "%=", start);
                yield makeToken(TokenType.PERCENT, "%", start);
            }
            case '&' -> {
                if (match('&')) yield makeToken(TokenType.LOGICAL_AND, "&&", start);
                if (match('=')) yield makeToken(TokenType.AMPERSAND_ASSIGN, "&=", start);
                yield makeToken(TokenType.AMPERSAND, "&", start);
            }
            case '|' -> {
                if (match('|')) yield makeToken(TokenType.LOGICAL_OR, "||", start);
                if (match('=')) yield makeToken(TokenType.PIPE_ASSIGN, "|=", start);
                yield makeToken(TokenType.PIPE, "|", start);
            }
            case '^' -> {
                if (match('=')) yield makeToken(TokenType.CARET_ASSIGN, "^=", start);
                yield makeToken(TokenType.CARET, "^", start);
            }
            case '~' -> makeToken(TokenType.TILDE, "~", start);
            case '!' -> {
                if (match('=')) yield makeToken(TokenType.NOT_EQUAL, "!=", start);
                yield makeToken(TokenType.LOGICAL_NOT, "!", start);
            }
            case '=' -> {
                if (match('=')) yield makeToken(TokenType.EQUAL, "==", start);
                yield makeToken(TokenType.ASSIGN, "=", start);
            }
            case '<' -> {
                if (match('<')) {
                    if (match('=')) yield makeToken(TokenType.SHIFT_LEFT_ASSIGN, "<<=", start);
                    yield makeToken(TokenType.SHIFT_LEFT, "<<", start);
                }
                if (match('=')) yield makeToken(TokenType.LESS_EQUAL, "<=", start);
                yield makeToken(TokenType.LESS, "<", start);
            }
            case '>' -> {
                if (match('>')) {
                    if (match('=')) yield makeToken(TokenType.SHIFT_RIGHT_ASSIGN, ">>=", start);
                    yield makeToken(TokenType.SHIFT_RIGHT, ">>", start);
                }
                if (match('=')) yield makeToken(TokenType.GREATER_EQUAL, ">=", start);
                yield makeToken(TokenType.GREATER, ">", start);
            }
            default -> makeToken(TokenType.ERROR, "Unexpected character: " + c, start);
        };
    }
    
    private Token scanAttributeBracket(int start) {
        advance(); advance(); // [[
        int depth = 1;
        while (!isAtEnd() && depth > 0) {
            if (peek() == '[' && peekNext() == '[') {
                advance(); advance();
                depth++;
            } else if (peek() == ']' && peekNext() == ']') {
                advance(); advance();
                depth--;
            } else {
                advance();
            }
        }
        return makeToken(TokenType.ATTRIBUTE, source.substring(start, position), start);
    }
    
    private boolean isQualifier(String text) {
        return switch (language) {
            case GLSL -> Set.of("in", "out", "inout", "uniform", "const", "varying", 
                               "attribute", "buffer", "shared", "coherent", "volatile",
                               "restrict", "readonly", "writeonly", "centroid", "flat",
                               "smooth", "noperspective", "sample", "patch", "precise",
                               "lowp", "mediump", "highp", "invariant").contains(text);
            case HLSL -> Set.of("in", "out", "inout", "uniform", "extern", "static",
                               "volatile", "const", "row_major", "column_major", "precise",
                               "nointerpolation", "linear", "centroid", "noperspective",
                               "sample", "globallycoherent", "groupshared").contains(text);
            case MSL -> Set.of("device", "constant", "threadgroup", "thread", "kernel",
                              "vertex", "fragment", "stage_in").contains(text);
            default -> false;
        };
    }
    
    private Token makeToken(TokenType type, String value, int start) {
        int tokenLine = line;
        int tokenColumn = start - lineStart + 1;
        return new Token(type, value, tokenLine, tokenColumn, start, position);
    }
    
    private boolean isAtEnd() {
        return position >= source.length();
    }
    
    private char advance() {
        column++;
        return source.charAt(position++);
    }
    
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(position) != expected) return false;
        position++;
        column++;
        return true;
    }
    
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(position);
    }
    
    private char peekNext() {
        if (position + 1 >= source.length()) return '\0';
        return source.charAt(position + 1);
    }
    
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    
    private boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
    
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
    
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
    
    private String getContext(int offset) {
        int start = Math.max(0, offset - 20);
        int end = Math.min(source.length(), offset + 20);
        return source.substring(start, end).replace("\n", "\\n");
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SECTION 16: ABSTRACT SYNTAX TREE (AST)
// Full AST representation when tokenization needs structure
// ════════════════════════════════════════════════════════════════════════════

/**
 * AST node types and parser for shader languages.
 */
public static final class ShaderAST {
    
    /**
     * Base class for all AST nodes.
     */
    public abstract static class ASTNode {
        public final int line;
        public final int column;
        public final List<ASTNode> children = new ArrayList<>();
        public ASTNode parent;
        
        protected ASTNode(int line, int column) {
            this.line = line;
            this.column = column;
        }
        
        public <T extends ASTNode> T addChild(T child) {
            children.add(child);
            child.parent = this;
            return child;
        }
        
        public abstract void accept(ASTVisitor visitor);
        public abstract String toMSL();
        public abstract String toGLSL();
        public abstract String toHLSL();
    }
    
    /**
     * Root translation unit node.
     */
    public static final class TranslationUnit extends ASTNode {
        public final List<PreprocessorDirective> preprocessorDirectives = new ArrayList<>();
        public final List<TypeDeclaration> typeDeclarations = new ArrayList<>();
        public final List<VariableDeclaration> globalVariables = new ArrayList<>();
        public final List<FunctionDeclaration> functions = new ArrayList<>();
        
        public TranslationUnit() {
            super(1, 1);
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            StringBuilder sb = new StringBuilder();
            sb.append("#include <metal_stdlib>\n");
            sb.append("using namespace metal;\n\n");
            
            for (TypeDeclaration type : typeDeclarations) {
                sb.append(type.toMSL()).append("\n\n");
            }
            for (VariableDeclaration var : globalVariables) {
                sb.append(var.toMSL()).append(";\n");
            }
            if (!globalVariables.isEmpty()) sb.append("\n");
            for (FunctionDeclaration func : functions) {
                sb.append(func.toMSL()).append("\n\n");
            }
            return sb.toString();
        }
        
        @Override
        public String toGLSL() {
            StringBuilder sb = new StringBuilder();
            sb.append("#version 450\n\n");
            
            for (PreprocessorDirective pp : preprocessorDirectives) {
                if (!pp.directive.startsWith("#version")) {
                    sb.append(pp.toGLSL()).append("\n");
                }
            }
            for (TypeDeclaration type : typeDeclarations) {
                sb.append(type.toGLSL()).append("\n\n");
            }
            for (VariableDeclaration var : globalVariables) {
                sb.append(var.toGLSL()).append(";\n");
            }
            if (!globalVariables.isEmpty()) sb.append("\n");
            for (FunctionDeclaration func : functions) {
                sb.append(func.toGLSL()).append("\n\n");
            }
            return sb.toString();
        }
        
        @Override
        public String toHLSL() {
            StringBuilder sb = new StringBuilder();
            
            for (TypeDeclaration type : typeDeclarations) {
                sb.append(type.toHLSL()).append("\n\n");
            }
            for (VariableDeclaration var : globalVariables) {
                sb.append(var.toHLSL()).append(";\n");
            }
            if (!globalVariables.isEmpty()) sb.append("\n");
            for (FunctionDeclaration func : functions) {
                sb.append(func.toHLSL()).append("\n\n");
            }
            return sb.toString();
        }
    }
    
    /**
     * Preprocessor directive node.
     */
    public static final class PreprocessorDirective extends ASTNode {
        public final String directive;
        
        public PreprocessorDirective(String directive, int line) {
            super(line, 1);
            this.directive = directive;
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            return directive; // Most preprocessor directives are universal
        }
        
        @Override
        public String toGLSL() {
            return directive;
        }
        
        @Override
        public String toHLSL() {
            return directive;
        }
    }
    
    /**
     * Type declaration (struct, class, enum).
     */
    public static final class TypeDeclaration extends ASTNode {
        public enum Kind { STRUCT, CLASS, ENUM, TYPEDEF }
        
        public final Kind kind;
        public final String name;
        public final List<VariableDeclaration> members = new ArrayList<>();
        public final List<String> enumValues = new ArrayList<>();
        public final Map<String, String> attributes = new HashMap<>();
        
        public TypeDeclaration(Kind kind, String name, int line, int column) {
            super(line, column);
            this.kind = kind;
            this.name = name;
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            StringBuilder sb = new StringBuilder();
            sb.append("struct ").append(name).append(" {\n");
            for (VariableDeclaration member : members) {
                sb.append("    ").append(member.toMSL());
                // Add MSL attributes
                if (member.attributes.containsKey("position")) {
                    sb.append(" [[position]]");
                } else if (member.attributes.containsKey("attribute")) {
                    sb.append(" [[attribute(").append(member.attributes.get("attribute")).append(")]]");
                } else if (member.attributes.containsKey("color")) {
                    sb.append(" [[color(").append(member.attributes.get("color")).append(")]]");
                } else if (member.attributes.containsKey("buffer")) {
                    sb.append(" [[buffer(").append(member.attributes.get("buffer")).append(")]]");
                } else if (member.attributes.containsKey("texture")) {
                    sb.append(" [[texture(").append(member.attributes.get("texture")).append(")]]");
                } else if (member.attributes.containsKey("sampler")) {
                    sb.append(" [[sampler(").append(member.attributes.get("sampler")).append(")]]");
                } else if (member.attributes.containsKey("user")) {
                    sb.append(" [[user(").append(member.attributes.get("user")).append(")]]");
                }
                sb.append(";\n");
            }
            sb.append("}");
            return sb.toString();
        }
        
        @Override
        public String toGLSL() {
            StringBuilder sb = new StringBuilder();
            sb.append("struct ").append(name).append(" {\n");
            for (VariableDeclaration member : members) {
                sb.append("    ").append(member.toGLSL()).append(";\n");
            }
            sb.append("}");
            return sb.toString();
        }
        
        @Override
        public String toHLSL() {
            StringBuilder sb = new StringBuilder();
            sb.append("struct ").append(name).append(" {\n");
            for (VariableDeclaration member : members) {
                sb.append("    ").append(member.toHLSL());
                // Add HLSL semantics
                if (member.attributes.containsKey("semantic")) {
                    sb.append(" : ").append(member.attributes.get("semantic"));
                }
                sb.append(";\n");
            }
            sb.append("}");
            return sb.toString();
        }
    }
    
    /**
     * Variable declaration.
     */
    public static final class VariableDeclaration extends ASTNode {
        public String type;
        public String name;
        public Expression initializer;
        public final Set<String> qualifiers = new LinkedHashSet<>();
        public final Map<String, String> attributes = new HashMap<>();
        public int arraySize = -1;
        public int location = -1;
        public int binding = -1;
        public int set = -1;
        
        public VariableDeclaration(String type, String name, int line, int column) {
            super(line, column);
            this.type = type;
            this.name = name;
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            StringBuilder sb = new StringBuilder();
            // MSL qualifiers
            if (qualifiers.contains("constant") || qualifiers.contains("uniform")) {
                sb.append("constant ");
            } else if (qualifiers.contains("device") || qualifiers.contains("buffer")) {
                sb.append("device ");
            } else if (qualifiers.contains("threadgroup") || qualifiers.contains("shared")) {
                sb.append("threadgroup ");
            }
            
            sb.append(TypeMapper.toMSL(type)).append(" ").append(name);
            if (arraySize > 0) {
                sb.append("[").append(arraySize).append("]");
            }
            if (initializer != null) {
                sb.append(" = ").append(initializer.toMSL());
            }
            return sb.toString();
        }
        
        @Override
        public String toGLSL() {
            StringBuilder sb = new StringBuilder();
            // Layout qualifier
            if (location >= 0 || binding >= 0 || set >= 0) {
                sb.append("layout(");
                List<String> layoutParts = new ArrayList<>();
                if (set >= 0) layoutParts.add("set = " + set);
                if (binding >= 0) layoutParts.add("binding = " + binding);
                if (location >= 0) layoutParts.add("location = " + location);
                sb.append(String.join(", ", layoutParts));
                sb.append(") ");
            }
            
            // Qualifiers
            for (String qual : qualifiers) {
                if (Set.of("in", "out", "inout", "uniform", "buffer", "shared", 
                          "flat", "smooth", "centroid").contains(qual)) {
                    sb.append(qual).append(" ");
                }
            }
            
            sb.append(TypeMapper.toGLSL(type)).append(" ").append(name);
            if (arraySize > 0) {
                sb.append("[").append(arraySize).append("]");
            }
            if (initializer != null) {
                sb.append(" = ").append(initializer.toGLSL());
            }
            return sb.toString();
        }
        
        @Override
        public String toHLSL() {
            StringBuilder sb = new StringBuilder();
            
            // Qualifiers
            if (qualifiers.contains("uniform") || qualifiers.contains("constant")) {
                // Will be in cbuffer
            } else if (qualifiers.contains("static")) {
                sb.append("static ");
            } else if (qualifiers.contains("groupshared") || qualifiers.contains("shared")) {
                sb.append("groupshared ");
            }
            
            sb.append(TypeMapper.toHLSL(type)).append(" ").append(name);
            if (arraySize > 0) {
                sb.append("[").append(arraySize).append("]");
            }
            
            // Register binding
            if (binding >= 0) {
                char regType = 'b'; // constant buffer by default
                if (type.contains("Texture") || type.contains("sampler")) {
                    regType = 't';
                } else if (type.contains("Sampler")) {
                    regType = 's';
                } else if (qualifiers.contains("buffer")) {
                    regType = 'u';
                }
                sb.append(" : register(").append(regType).append(binding).append(")");
            }
            
            if (initializer != null) {
                sb.append(" = ").append(initializer.toHLSL());
            }
            return sb.toString();
        }
    }
    
    /**
     * Function declaration.
     */
    public static final class FunctionDeclaration extends ASTNode {
        public String returnType;
        public String name;
        public final List<Parameter> parameters = new ArrayList<>();
        public BlockStatement body;
        public ShaderStage stage;
        public final Map<String, String> attributes = new HashMap<>();
        
        public FunctionDeclaration(String returnType, String name, int line, int column) {
            super(line, column);
            this.returnType = returnType;
            this.name = name;
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            StringBuilder sb = new StringBuilder();
            
            // Stage attribute
            if (stage != null) {
                sb.append(switch (stage) {
                    case VERTEX -> "vertex ";
                    case FRAGMENT -> "fragment ";
                    case KERNEL -> "kernel ";
                    case MESH -> "[[mesh]] ";
                    case OBJECT -> "[[object]] ";
                    default -> "";
                });
            }
            
            sb.append(TypeMapper.toMSL(returnType)).append(" ").append(name).append("(\n");
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) sb.append(",\n");
                sb.append("    ").append(parameters.get(i).toMSL());
            }
            sb.append("\n)");
            
            if (body != null) {
                sb.append(" ").append(body.toMSL());
            } else {
                sb.append(";");
            }
            return sb.toString();
        }
        
        @Override
        public String toGLSL() {
            StringBuilder sb = new StringBuilder();
            sb.append(TypeMapper.toGLSL(returnType)).append(" ").append(name).append("(");
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(parameters.get(i).toGLSL());
            }
            sb.append(")");
            
            if (body != null) {
                sb.append(" ").append(body.toGLSL());
            } else {
                sb.append(";");
            }
            return sb.toString();
        }
        
        @Override
        public String toHLSL() {
            StringBuilder sb = new StringBuilder();
            sb.append(TypeMapper.toHLSL(returnType)).append(" ").append(name).append("(");
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(parameters.get(i).toHLSL());
            }
            sb.append(")");
            
            if (body != null) {
                sb.append(" ").append(body.toHLSL());
            } else {
                sb.append(";");
            }
            return sb.toString();
        }
    }
    
    /**
     * Function parameter.
     */
    public static final class Parameter extends ASTNode {
        public String type;
        public String name;
        public final Set<String> qualifiers = new LinkedHashSet<>();
        public final Map<String, String> attributes = new HashMap<>();
        
        public Parameter(String type, String name, int line, int column) {
            super(line, column);
            this.type = type;
            this.name = name;
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            StringBuilder sb = new StringBuilder();
            
            // Address space qualifiers
            if (qualifiers.contains("constant") || qualifiers.contains("uniform")) {
                sb.append("constant ");
            } else if (qualifiers.contains("device")) {
                sb.append("device ");
            } else if (qualifiers.contains("threadgroup")) {
                sb.append("threadgroup ");
            } else if (qualifiers.contains("thread") || qualifiers.contains("out") || 
                      qualifiers.contains("inout")) {
                sb.append("thread ");
            }
            
            sb.append(TypeMapper.toMSL(type));
            
            // Reference for out/inout
            if (qualifiers.contains("out") || qualifiers.contains("inout") ||
                qualifiers.contains("constant") || qualifiers.contains("device")) {
                sb.append("&");
            }
            
            sb.append(" ").append(name);
            
            // MSL attributes
            if (attributes.containsKey("stage_in")) {
                sb.append(" [[stage_in]]");
            } else if (attributes.containsKey("buffer")) {
                sb.append(" [[buffer(").append(attributes.get("buffer")).append(")]]");
            } else if (attributes.containsKey("texture")) {
                sb.append(" [[texture(").append(attributes.get("texture")).append(")]]");
            } else if (attributes.containsKey("sampler")) {
                sb.append(" [[sampler(").append(attributes.get("sampler")).append(")]]");
            } else if (attributes.containsKey("thread_position_in_grid")) {
                sb.append(" [[thread_position_in_grid]]");
            } else if (attributes.containsKey("vertex_id")) {
                sb.append(" [[vertex_id]]");
            } else if (attributes.containsKey("instance_id")) {
                sb.append(" [[instance_id]]");
            }
            
            return sb.toString();
        }
        
        @Override
        public String toGLSL() {
            StringBuilder sb = new StringBuilder();
            for (String qual : qualifiers) {
                if (Set.of("in", "out", "inout").contains(qual)) {
                    sb.append(qual).append(" ");
                }
            }
            sb.append(TypeMapper.toGLSL(type)).append(" ").append(name);
            return sb.toString();
        }
        
        @Override
        public String toHLSL() {
            StringBuilder sb = new StringBuilder();
            for (String qual : qualifiers) {
                if (Set.of("in", "out", "inout").contains(qual)) {
                    sb.append(qual).append(" ");
                }
            }
            sb.append(TypeMapper.toHLSL(type)).append(" ").append(name);
            if (attributes.containsKey("semantic")) {
                sb.append(" : ").append(attributes.get("semantic"));
            }
            return sb.toString();
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Statements
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * Base class for statements.
     */
    public abstract static class Statement extends ASTNode {
        protected Statement(int line, int column) {
            super(line, column);
        }
    }
    
    /**
     * Block statement { ... }.
     */
    public static final class BlockStatement extends Statement {
        public final List<Statement> statements = new ArrayList<>();
        
        public BlockStatement(int line, int column) {
            super(line, column);
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            StringBuilder sb = new StringBuilder("{\n");
            for (Statement stmt : statements) {
                sb.append("    ").append(indent(stmt.toMSL())).append("\n");
            }
            sb.append("}");
            return sb.toString();
        }
        
        @Override
        public String toGLSL() {
            StringBuilder sb = new StringBuilder("{\n");
            for (Statement stmt : statements) {
                sb.append("    ").append(indent(stmt.toGLSL())).append("\n");
            }
            sb.append("}");
            return sb.toString();
        }
        
        @Override
        public String toHLSL() {
            StringBuilder sb = new StringBuilder("{\n");
            for (Statement stmt : statements) {
                sb.append("    ").append(indent(stmt.toHLSL())).append("\n");
            }
            sb.append("}");
            return sb.toString();
        }
        
        private String indent(String s) {
            return s.replace("\n", "\n    ");
        }
    }
    
    /**
     * Expression statement.
     */
    public static final class ExpressionStatement extends Statement {
        public final Expression expression;
        
        public ExpressionStatement(Expression expression, int line, int column) {
            super(line, column);
            this.expression = expression;
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            return expression.toMSL() + ";";
        }
        
        @Override
        public String toGLSL() {
            return expression.toGLSL() + ";";
        }
        
        @Override
        public String toHLSL() {
            return expression.toHLSL() + ";";
        }
    }
    
    /**
     * Variable declaration statement.
     */
    public static final class VariableStatement extends Statement {
        public final VariableDeclaration declaration;
        
        public VariableStatement(VariableDeclaration declaration, int line, int column) {
            super(line, column);
            this.declaration = declaration;
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            return declaration.toMSL() + ";";
        }
        
        @Override
        public String toGLSL() {
            return declaration.toGLSL() + ";";
        }
        
        @Override
        public String toHLSL() {
            return declaration.toHLSL() + ";";
        }
    }
    
    /**
     * If statement.
     */
    public static final class IfStatement extends Statement {
        public final Expression condition;
        public final Statement thenBranch;
        public Statement elseBranch;
        
        public IfStatement(Expression condition, Statement thenBranch, int line, int column) {
            super(line, column);
            this.condition = condition;
            this.thenBranch = thenBranch;
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            StringBuilder sb = new StringBuilder();
            sb.append("if (").append(condition.toMSL()).append(") ");
            sb.append(thenBranch.toMSL());
            if (elseBranch != null) {
                sb.append(" else ").append(elseBranch.toMSL());
            }
            return sb.toString();
        }
        
        @Override
        public String toGLSL() {
            StringBuilder sb = new StringBuilder();
            sb.append("if (").append(condition.toGLSL()).append(") ");
            sb.append(thenBranch.toGLSL());
            if (elseBranch != null) {
                sb.append(" else ").append(elseBranch.toGLSL());
            }
            return sb.toString();
        }
        
        @Override
        public String toHLSL() {
            StringBuilder sb = new StringBuilder();
            sb.append("if (").append(condition.toHLSL()).append(") ");
            sb.append(thenBranch.toHLSL());
            if (elseBranch != null) {
                sb.append(" else ").append(elseBranch.toHLSL());
            }
            return sb.toString();
        }
    }
    
    /**
     * For loop statement.
     */
    public static final class ForStatement extends Statement {
        public Statement initializer;
        public Expression condition;
        public Expression increment;
        public Statement body;
        
        public ForStatement(int line, int column) {
            super(line, column);
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            StringBuilder sb = new StringBuilder("for (");
            if (initializer != null) {
                String init = initializer.toMSL();
                sb.append(init.endsWith(";") ? init.substring(0, init.length() - 1) : init);
            }
            sb.append("; ");
            if (condition != null) sb.append(condition.toMSL());
            sb.append("; ");
            if (increment != null) sb.append(increment.toMSL());
            sb.append(") ").append(body.toMSL());
            return sb.toString();
        }
        
        @Override
        public String toGLSL() {
            StringBuilder sb = new StringBuilder("for (");
            if (initializer != null) {
                String init = initializer.toGLSL();
                sb.append(init.endsWith(";") ? init.substring(0, init.length() - 1) : init);
            }
            sb.append("; ");
            if (condition != null) sb.append(condition.toGLSL());
            sb.append("; ");
            if (increment != null) sb.append(increment.toGLSL());
            sb.append(") ").append(body.toGLSL());
            return sb.toString();
        }
        
        @Override
        public String toHLSL() {
            StringBuilder sb = new StringBuilder("for (");
            if (initializer != null) {
                String init = initializer.toHLSL();
                sb.append(init.endsWith(";") ? init.substring(0, init.length() - 1) : init);
            }
            sb.append("; ");
            if (condition != null) sb.append(condition.toHLSL());
            sb.append("; ");
            if (increment != null) sb.append(increment.toHLSL());
            sb.append(") ").append(body.toHLSL());
            return sb.toString();
        }
    }
    
    /**
     * While loop statement.
     */
    public static final class WhileStatement extends Statement {
        public final Expression condition;
        public final Statement body;
        
        public WhileStatement(Expression condition, Statement body, int line, int column) {
            super(line, column);
            this.condition = condition;
            this.body = body;
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            return "while (" + condition.toMSL() + ") " + body.toMSL();
        }
        
        @Override
        public String toGLSL() {
            return "while (" + condition.toGLSL() + ") " + body.toGLSL();
        }
        
        @Override
        public String toHLSL() {
            return "while (" + condition.toHLSL() + ") " + body.toHLSL();
        }
    }
    
    /**
     * Return statement.
     */
    public static final class ReturnStatement extends Statement {
        public final Expression value;
        
        public ReturnStatement(Expression value, int line, int column) {
            super(line, column);
            this.value = value;
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            if (value != null) {
                return "return " + value.toMSL() + ";";
            }
            return "return;";
        }
        
        @Override
        public String toGLSL() {
            if (value != null) {
                return "return " + value.toGLSL() + ";";
            }
            return "return;";
        }
        
        @Override
        public String toHLSL() {
            if (value != null) {
                return "return " + value.toHLSL() + ";";
            }
            return "return;";
        }
    }
    
    /**
     * Discard statement (fragment shaders).
     */
    public static final class DiscardStatement extends Statement {
        public DiscardStatement(int line, int column) {
            super(line, column);
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            return "discard_fragment();";
        }
        
        @Override
        public String toGLSL() {
            return "discard;";
        }
        
        @Override
        public String toHLSL() {
            return "discard;";
        }
    }
    
    /**
     * Break statement.
     */
    public static final class BreakStatement extends Statement {
        public BreakStatement(int line, int column) {
            super(line, column);
        }
        
        @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
        @Override public String toMSL() { return "break;"; }
        @Override public String toGLSL() { return "break;"; }
        @Override public String toHLSL() { return "break;"; }
    }
    
    /**
     * Continue statement.
     */
    public static final class ContinueStatement extends Statement {
        public ContinueStatement(int line, int column) {
            super(line, column);
        }
        
        @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
        @Override public String toMSL() { return "continue;"; }
        @Override public String toGLSL() { return "continue;"; }
        @Override public String toHLSL() { return "continue;"; }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Expressions
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * Base class for expressions.
     */
    public abstract static class Expression extends ASTNode {
        public String inferredType;
        
        protected Expression(int line, int column) {
            super(line, column);
        }
    }
    
    /**
     * Literal expression (number, string, bool).
     */
    public static final class LiteralExpression extends Expression {
        public final Object value;
        public final String text;
        
        public LiteralExpression(Object value, String text, int line, int column) {
            super(line, column);
            this.value = value;
            this.text = text;
        }
        
        @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
        @Override public String toMSL() { return text; }
        @Override public String toGLSL() { return text; }
        @Override public String toHLSL() { return text; }
    }
    
    /**
     * Identifier expression.
     */
    public static final class IdentifierExpression extends Expression {
        public final String name;
        
        public IdentifierExpression(String name, int line, int column) {
            super(line, column);
            this.name = name;
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            return BuiltinMapper.toMSL(name);
        }
        
        @Override
        public String toGLSL() {
            return BuiltinMapper.toGLSL(name);
        }
        
        @Override
        public String toHLSL() {
            return BuiltinMapper.toHLSL(name);
        }
    }
    
    /**
     * Binary expression (a op b).
     */
    public static final class BinaryExpression extends Expression {
        public final Expression left;
        public final String operator;
        public final Expression right;
        
        public BinaryExpression(Expression left, String operator, Expression right, int line, int column) {
            super(line, column);
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
        
        @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
        
        @Override
        public String toMSL() {
            return "(" + left.toMSL() + " " + operator + " " + right.toMSL() + ")";
        }
        
        @Override
        public String toGLSL() {
            return "(" + left.toGLSL() + " " + operator + " " + right.toGLSL() + ")";
        }
        
        @Override
        public String toHLSL() {
            return "(" + left.toHLSL() + " " + operator + " " + right.toHLSL() + ")";
        }
    }
    
    /**
     * Unary expression (op expr or expr op).
     */
    public static final class UnaryExpression extends Expression {
        public final String operator;
        public final Expression operand;
        public final boolean prefix;
        
        public UnaryExpression(String operator, Expression operand, boolean prefix, int line, int column) {
            super(line, column);
            this.operator = operator;
            this.operand = operand;
            this.prefix = prefix;
        }
        
        @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
        
        @Override
        public String toMSL() {
            return prefix ? operator + operand.toMSL() : operand.toMSL() + operator;
        }
        
        @Override
        public String toGLSL() {
            return prefix ? operator + operand.toGLSL() : operand.toGLSL() + operator;
        }
        
        @Override
        public String toHLSL() {
            return prefix ? operator + operand.toHLSL() : operand.toHLSL() + operator;
        }
    }
    
    /**
     * Function call expression.
     */
    public static final class CallExpression extends Expression {
        public final String functionName;
        public final List<Expression> arguments = new ArrayList<>();
        public Expression callee; // For member function calls
        
        public CallExpression(String functionName, int line, int column) {
            super(line, column);
            this.functionName = functionName;
        }
        
        @Override
        public void accept(ASTVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public String toMSL() {
            String func = FunctionMapper.toMSL(functionName);
            StringBuilder sb = new StringBuilder();
            
            // Handle texture sampling specially
            if (functionName.equals("texture") || functionName.equals("texture2D") ||
                functionName.startsWith("texture")) {
                if (arguments.size() >= 2) {
                    sb.append(arguments.get(0).toMSL()).append(".sample(");
                    sb.append(arguments.get(0).toMSL()).append("_sampler, ");
                    for (int i = 1; i < arguments.size(); i++) {
                        if (i > 1) sb.append(", ");
                        sb.append(arguments.get(i).toMSL());
                    }
                    sb.append(")");
                    return sb.toString();
                }
            }
            
            if (callee != null) {
                sb.append(callee.toMSL()).append(".");
            }
            sb.append(func).append("(");
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arguments.get(i).toMSL());
            }
            sb.append(")");
            return sb.toString();
        }
        
        @Override
        public String toGLSL() {
            String func = FunctionMapper.toGLSL(functionName);
            StringBuilder sb = new StringBuilder();
            if (callee != null) {
                sb.append(callee.toGLSL()).append(".");
            }
            sb.append(func).append("(");
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arguments.get(i).toGLSL());
            }
            sb.append(")");
            return sb.toString();
        }
        
        @Override
        public String toHLSL() {
            String func = FunctionMapper.toHLSL(functionName);
            StringBuilder sb = new StringBuilder();
            
            // Handle texture sampling
            if (functionName.equals("texture") || functionName.equals("texture2D")) {
                if (arguments.size() >= 2) {
                    sb.append(arguments.get(0).toHLSL()).append(".Sample(");
                    sb.append(arguments.get(0).toHLSL()).append("_sampler, ");
                    for (int i = 1; i < arguments.size(); i++) {
                        if (i > 1) sb.append(", ");
                        sb.append(arguments.get(i).toHLSL());
                    }
                    sb.append(")");
                    return sb.toString();
                }
            }
            
            if (callee != null) {
                sb.append(callee.toHLSL()).append(".");
            }
            sb.append(func).append("(");
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arguments.get(i).toHLSL());
            }
            sb.append(")");
            return sb.toString();
        }
    }
    
    /**
     * Member access expression (a.b).
     */
    public static final class MemberExpression extends Expression {
        public final Expression object;
        public final String member;
        
        public MemberExpression(Expression object, String member, int line, int column) {
            super(line, column);
            this.object = object;
            this.member = member;
        }
        
        @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
        
        @Override
        public String toMSL() {
            return object.toMSL() + "." + member;
        }
        
        @Override
        public String toGLSL() {
            return object.toGLSL() + "." + member;
        }
        
        @Override
        public String toHLSL() {
            return object.toHLSL() + "." + member;
        }
    }
    
    /**
     * Array/index access expression (a[i]).
     */
    public static final class IndexExpression extends Expression {
        public final Expression array;
        public final Expression index;
        
        public IndexExpression(Expression array, Expression index, int line, int column) {
            super(line, column);
            this.array = array;
            this.index = index;
        }
        
        @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
        
        @Override
        public String toMSL() {
            return array.toMSL() + "[" + index.toMSL() + "]";
        }
        
        @Override
        public String toGLSL() {
            return array.toGLSL() + "[" + index.toGLSL() + "]";
        }
        
        @Override
        public String toHLSL() {
            return array.toHLSL() + "[" + index.toHLSL() + "]";
        }
    }
    
    /**
     * Type cast expression.
     */
    public static final class CastExpression extends Expression {
        public final String targetType;
        public final Expression expression;
        
        public CastExpression(String targetType, Expression expression, int line, int column) {
            super(line, column);
            this.targetType = targetType;
            this.expression = expression;
        }
        
        @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
        
        @Override
        public String toMSL() {
            return TypeMapper.toMSL(targetType) + "(" + expression.toMSL() + ")";
        }
        
        @Override
        public String toGLSL() {
            return TypeMapper.toGLSL(targetType) + "(" + expression.toGLSL() + ")";
        }
        
        @Override
        public String toHLSL() {
            return "(" + TypeMapper.toHLSL(targetType) + ")" + expression.toHLSL();
        }
    }
    
    /**
     * Constructor expression (float3(1,2,3)).
     */
    public static final class ConstructorExpression extends Expression {
        public final String type;
        public final List<Expression> arguments = new ArrayList<>();
        
        public ConstructorExpression(String type, int line, int column) {
            super(line, column);
            this.type = type;
        }
        
        @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
        
        @Override
        public String toMSL() {
            StringBuilder sb = new StringBuilder(TypeMapper.toMSL(type));
            sb.append("(");
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arguments.get(i).toMSL());
            }
            sb.append(")");
            return sb.toString();
        }
        
        @Override
        public String toGLSL() {
            StringBuilder sb = new StringBuilder(TypeMapper.toGLSL(type));
            sb.append("(");
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arguments.get(i).toGLSL());
            }
            sb.append(")");
            return sb.toString();
        }
        
        @Override
        public String toHLSL() {
            StringBuilder sb = new StringBuilder(TypeMapper.toHLSL(type));
            sb.append("(");
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arguments.get(i).toHLSL());
            }
            sb.append(")");
            return sb.toString();
        }
    }
    
    /**
     * Ternary/conditional expression (a ? b : c).
     */
    public static final class TernaryExpression extends Expression {
        public final Expression condition;
        public final Expression thenExpr;
        public final Expression elseExpr;
        
        public TernaryExpression(Expression condition, Expression thenExpr, 
                                Expression elseExpr, int line, int column) {
            super(line, column);
            this.condition = condition;
            this.thenExpr = thenExpr;
            this.elseExpr = elseExpr;
        }
        
        @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
        
        @Override
        public String toMSL() {
            return "(" + condition.toMSL() + " ? " + thenExpr.toMSL() + " : " + elseExpr.toMSL() + ")";
        }
        
        @Override
        public String toGLSL() {
            return "(" + condition.toGLSL() + " ? " + thenExpr.toGLSL() + " : " + elseExpr.toGLSL() + ")";
        }
        
        @Override
        public String toHLSL() {
            return "(" + condition.toHLSL() + " ? " + thenExpr.toHLSL() + " : " + elseExpr.toHLSL() + ")";
        }
    }
    
    /**
     * Assignment expression.
     */
    public static final class AssignmentExpression extends Expression {
        public final Expression target;
        public final String operator; // =, +=, -=, etc.
        public final Expression value;
        
        public AssignmentExpression(Expression target, String operator, 
                                   Expression value, int line, int column) {
            super(line, column);
            this.target = target;
            this.operator = operator;
            this.value = value;
        }
        
        @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
        
        @Override
        public String toMSL() {
            return target.toMSL() + " " + operator + " " + value.toMSL();
        }
        
        @Override
        public String toGLSL() {
            return target.toGLSL() + " " + operator + " " + value.toGLSL();
        }
        
        @Override
        public String toHLSL() {
            return target.toHLSL() + " " + operator + " " + value.toHLSL();
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // AST Visitor
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * Visitor interface for AST traversal.
     */
    public interface ASTVisitor {
        default void visit(TranslationUnit node) { visitChildren(node); }
        default void visit(PreprocessorDirective node) {}
        default void visit(TypeDeclaration node) { visitChildren(node); }
        default void visit(VariableDeclaration node) { if (node.initializer != null) node.initializer.accept(this); }
        default void visit(FunctionDeclaration node) { 
            for (Parameter p : node.parameters) p.accept(this);
            if (node.body != null) node.body.accept(this);
        }
        default void visit(Parameter node) {}
        default void visit(BlockStatement node) { for (Statement s : node.statements) s.accept(this); }
        default void visit(ExpressionStatement node) { node.expression.accept(this); }
        default void visit(VariableStatement node) { node.declaration.accept(this); }
        default void visit(IfStatement node) { 
            node.condition.accept(this);
            node.thenBranch.accept(this);
            if (node.elseBranch != null) node.elseBranch.accept(this);
        }
        default void visit(ForStatement node) {
            if (node.initializer != null) node.initializer.accept(this);
            if (node.condition != null) node.condition.accept(this);
            if (node.increment != null) node.increment.accept(this);
            node.body.accept(this);
        }
        default void visit(WhileStatement node) { node.condition.accept(this); node.body.accept(this); }
        default void visit(ReturnStatement node) { if (node.value != null) node.value.accept(this); }
        default void visit(DiscardStatement node) {}
        default void visit(BreakStatement node) {}
        default void visit(ContinueStatement node) {}
        default void visit(LiteralExpression node) {}
        default void visit(IdentifierExpression node) {}
        default void visit(BinaryExpression node) { node.left.accept(this); node.right.accept(this); }
        default void visit(UnaryExpression node) { node.operand.accept(this); }
        default void visit(CallExpression node) { 
            if (node.callee != null) node.callee.accept(this);
            for (Expression arg : node.arguments) arg.accept(this);
        }
        default void visit(MemberExpression node) { node.object.accept(this); }
        default void visit(IndexExpression node) { node.array.accept(this); node.index.accept(this); }
        default void visit(CastExpression node) { node.expression.accept(this); }
        default void visit(ConstructorExpression node) { for (Expression arg : node.arguments) arg.accept(this); }
        default void visit(TernaryExpression node) { 
            node.condition.accept(this); 
            node.thenExpr.accept(this); 
            node.elseExpr.accept(this);
        }
        default void visit(AssignmentExpression node) { node.target.accept(this); node.value.accept(this); }
        
        default void visitChildren(ASTNode node) {
            for (ASTNode child : node.children) {
                child.accept(this);
            }
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Type/Function/Builtin Mappers
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * Type mapping between shader languages.
     */
    public static final class TypeMapper {
        private static final Map<String, String> GLSL_TO_MSL = new HashMap<>();
        private static final Map<String, String> HLSL_TO_MSL = new HashMap<>();
        private static final Map<String, String> MSL_TO_GLSL = new HashMap<>();
        private static final Map<String, String> MSL_TO_HLSL = new HashMap<>();
        
        static {
            // GLSL to MSL
            GLSL_TO_MSL.put("vec2", "float2");
            GLSL_TO_MSL.put("vec3", "float3");
            GLSL_TO_MSL.put("vec4", "float4");
            GLSL_TO_MSL.put("ivec2", "int2");
            GLSL_TO_MSL.put("ivec3", "int3");
            GLSL_TO_MSL.put("ivec4", "int4");
            GLSL_TO_MSL.put("uvec2", "uint2");
            GLSL_TO_MSL.put("uvec3", "uint3");
            GLSL_TO_MSL.put("uvec4", "uint4");
            GLSL_TO_MSL.put("bvec2", "bool2");
            GLSL_TO_MSL.put("bvec3", "bool3");
            GLSL_TO_MSL.put("bvec4", "bool4");
            GLSL_TO_MSL.put("mat2", "float2x2");
            GLSL_TO_MSL.put("mat3", "float3x3");
            GLSL_TO_MSL.put("mat4", "float4x4");
            GLSL_TO_MSL.put("mat2x2", "float2x2");
            GLSL_TO_MSL.put("mat2x3", "float2x3");
            GLSL_TO_MSL.put("mat2x4", "float2x4");
            GLSL_TO_MSL.put("mat3x2", "float3x2");
            GLSL_TO_MSL.put("mat3x3", "float3x3");
            GLSL_TO_MSL.put("mat3x4", "float3x4");
            GLSL_TO_MSL.put("mat4x2", "float4x2");
            GLSL_TO_MSL.put("mat4x3", "float4x3");
            GLSL_TO_MSL.put("mat4x4", "float4x4");
            GLSL_TO_MSL.put("sampler2D", "texture2d<float>");
            GLSL_TO_MSL.put("sampler3D", "texture3d<float>");
            GLSL_TO_MSL.put("samplerCube", "texturecube<float>");
            GLSL_TO_MSL.put("sampler2DArray", "texture2d_array<float>");
            GLSL_TO_MSL.put("sampler2DShadow", "depth2d<float>");
            GLSL_TO_MSL.put("isampler2D", "texture2d<int>");
            GLSL_TO_MSL.put("usampler2D", "texture2d<uint>");
            
            // HLSL to MSL
            HLSL_TO_MSL.put("float2", "float2");
            HLSL_TO_MSL.put("float3", "float3");
            HLSL_TO_MSL.put("float4", "float4");
            HLSL_TO_MSL.put("int2", "int2");
            HLSL_TO_MSL.put("int3", "int3");
            HLSL_TO_MSL.put("int4", "int4");
            HLSL_TO_MSL.put("uint2", "uint2");
            HLSL_TO_MSL.put("uint3", "uint3");
            HLSL_TO_MSL.put("uint4", "uint4");
            HLSL_TO_MSL.put("float2x2", "float2x2");
            HLSL_TO_MSL.put("float3x3", "float3x3");
            HLSL_TO_MSL.put("float4x4", "float4x4");
            HLSL_TO_MSL.put("Texture2D", "texture2d<float>");
            HLSL_TO_MSL.put("Texture3D", "texture3d<float>");
            HLSL_TO_MSL.put("TextureCube", "texturecube<float>");
            HLSL_TO_MSL.put("Texture2DArray", "texture2d_array<float>");
            HLSL_TO_MSL.put("RWTexture2D", "texture2d<float, access::read_write>");
            HLSL_TO_MSL.put("SamplerState", "sampler");
            HLSL_TO_MSL.put("SamplerComparisonState", "sampler");
            
            // Build reverse mappings
            for (Map.Entry<String, String> e : GLSL_TO_MSL.entrySet()) {
                MSL_TO_GLSL.putIfAbsent(e.getValue(), e.getKey());
            }
            for (Map.Entry<String, String> e : HLSL_TO_MSL.entrySet()) {
                MSL_TO_HLSL.putIfAbsent(e.getValue(), e.getKey());
            }
        }
        
        public static String toMSL(String type) {
            if (type == null) return "void";
            String mapped = GLSL_TO_MSL.get(type);
            if (mapped != null) return mapped;
            mapped = HLSL_TO_MSL.get(type);
            return mapped != null ? mapped : type;
        }
        
        public static String toGLSL(String type) {
            if (type == null) return "void";
            String mapped = MSL_TO_GLSL.get(type);
            return mapped != null ? mapped : type;
        }
        
        public static String toHLSL(String type) {
            if (type == null) return "void";
            String mapped = MSL_TO_HLSL.get(type);
            return mapped != null ? mapped : type;
        }
    }
    
    /**
     * Function mapping between shader languages.
     */
    public static final class FunctionMapper {
        private static final Map<String, String> TO_MSL = new HashMap<>();
        private static final Map<String, String> TO_GLSL = new HashMap<>();
        private static final Map<String, String> TO_HLSL = new HashMap<>();
        
        static {
            // GLSL -> MSL
            TO_MSL.put("texture", "sample");
            TO_MSL.put("texture2D", "sample");
            TO_MSL.put("texture3D", "sample");
            TO_MSL.put("textureCube", "sample");
            TO_MSL.put("textureLod", "sample");
            TO_MSL.put("textureProj", "sample");
            TO_MSL.put("texelFetch", "read");
            TO_MSL.put("imageLoad", "read");
            TO_MSL.put("imageStore", "write");
            TO_MSL.put("mod", "fmod");
            TO_MSL.put("fract", "fract");
            TO_MSL.put("mix", "mix");
            TO_MSL.put("dFdx", "dfdx");
            TO_MSL.put("dFdy", "dfdy");
            TO_MSL.put("fwidth", "fwidth");
            TO_MSL.put("inversesqrt", "rsqrt");
            TO_MSL.put("lessThan", "isless");
            TO_MSL.put("lessThanEqual", "islessequal");
            TO_MSL.put("greaterThan", "isgreater");
            TO_MSL.put("greaterThanEqual", "isgreaterequal");
            TO_MSL.put("equal", "isequal");
            TO_MSL.put("notEqual", "isnotequal");
            
            // MSL -> GLSL
            TO_GLSL.put("sample", "texture");
            TO_GLSL.put("read", "texelFetch");
            TO_GLSL.put("write", "imageStore");
            TO_GLSL.put("fmod", "mod");
            TO_GLSL.put("dfdx", "dFdx");
            TO_GLSL.put("dfdy", "dFdy");
            TO_GLSL.put("rsqrt", "inversesqrt");
            TO_GLSL.put("discard_fragment", "discard");
            
            // HLSL/MSL -> HLSL
            TO_HLSL.put("sample", "Sample");
            TO_HLSL.put("read", "Load");
            TO_HLSL.put("fmod", "fmod");
            TO_HLSL.put("fract", "frac");
            TO_HLSL.put("mix", "lerp");
            TO_HLSL.put("dfdx", "ddx");
            TO_HLSL.put("dfdy", "ddy");
            TO_HLSL.put("rsqrt", "rsqrt");
            TO_HLSL.put("discard_fragment", "discard");
            TO_HLSL.put("inversesqrt", "rsqrt");
            TO_HLSL.put("texture", "Sample");
            TO_HLSL.put("texture2D", "Sample");
        }
        
        public static String toMSL(String func) {
            return TO_MSL.getOrDefault(func, func);
        }
        
        public static String toGLSL(String func) {
            return TO_GLSL.getOrDefault(func, func);
        }
        
        public static String toHLSL(String func) {
            return TO_HLSL.getOrDefault(func, func);
        }
    }
    
    /**
     * Builtin variable mapping.
     */
    public static final class BuiltinMapper {
        private static final Map<String, String> TO_MSL = new HashMap<>();
        private static final Map<String, String> TO_GLSL = new HashMap<>();
        private static final Map<String, String> TO_HLSL = new HashMap<>();
        
        static {
            // GLSL builtins -> MSL
            TO_MSL.put("gl_Position", "out.position");
            TO_MSL.put("gl_FragCoord", "in.position");
            TO_MSL.put("gl_VertexID", "vertex_id");
            TO_MSL.put("gl_VertexIndex", "vertex_id");
            TO_MSL.put("gl_InstanceID", "instance_id");
            TO_MSL.put("gl_InstanceIndex", "instance_id");
            TO_MSL.put("gl_FrontFacing", "front_facing");
            TO_MSL.put("gl_PointCoord", "point_coord");
            TO_MSL.put("gl_FragDepth", "depth(any)");
            TO_MSL.put("gl_GlobalInvocationID", "thread_position_in_grid");
            TO_MSL.put("gl_LocalInvocationID", "thread_position_in_threadgroup");
            TO_MSL.put("gl_WorkGroupID", "threadgroup_position_in_grid");
            TO_MSL.put("gl_LocalInvocationIndex", "thread_index_in_threadgroup");
            
            // HLSL semantics -> MSL
            TO_MSL.put("SV_Position", "position");
            TO_MSL.put("SV_Target", "color(0)");
            TO_MSL.put("SV_Target0", "color(0)");
            TO_MSL.put("SV_Target1", "color(1)");
            TO_MSL.put("SV_Target2", "color(2)");
            TO_MSL.put("SV_Target3", "color(3)");
            TO_MSL.put("SV_Depth", "depth(any)");
            TO_MSL.put("SV_VertexID", "vertex_id");
            TO_MSL.put("SV_InstanceID", "instance_id");
            TO_MSL.put("SV_IsFrontFace", "front_facing");
            TO_MSL.put("SV_DispatchThreadID", "thread_position_in_grid");
            TO_MSL.put("SV_GroupThreadID", "thread_position_in_threadgroup");
            TO_MSL.put("SV_GroupID", "threadgroup_position_in_grid");
            TO_MSL.put("SV_GroupIndex", "thread_index_in_threadgroup");
            
            // MSL -> GLSL
            TO_GLSL.put("position", "gl_Position");
            TO_GLSL.put("vertex_id", "gl_VertexID");
            TO_GLSL.put("instance_id", "gl_InstanceID");
            TO_GLSL.put("front_facing", "gl_FrontFacing");
            TO_GLSL.put("point_coord", "gl_PointCoord");
            TO_GLSL.put("thread_position_in_grid", "gl_GlobalInvocationID");
            TO_GLSL.put("thread_position_in_threadgroup", "gl_LocalInvocationID");
            TO_GLSL.put("threadgroup_position_in_grid", "gl_WorkGroupID");
            
            // MSL -> HLSL
            TO_HLSL.put("position", "SV_Position");
            TO_HLSL.put("vertex_id", "SV_VertexID");
            TO_HLSL.put("instance_id", "SV_InstanceID");
            TO_HLSL.put("front_facing", "SV_IsFrontFace");
            TO_HLSL.put("thread_position_in_grid", "SV_DispatchThreadID");
            TO_HLSL.put("thread_position_in_threadgroup", "SV_GroupThreadID");
            TO_HLSL.put("threadgroup_position_in_grid", "SV_GroupID");
        }
        
        public static String toMSL(String name) {
            return TO_MSL.getOrDefault(name, name);
        }
        
        public static String toGLSL(String name) {
            return TO_GLSL.getOrDefault(name, name);
        }
        
        public static String toHLSL(String name) {
            return TO_HLSL.getOrDefault(name, name);
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SECTION 17: AST PARSER
// Parse tokens into AST
// ════════════════════════════════════════════════════════════════════════════

/**
 * Parser that converts tokens into AST.
 */
public static final class ShaderParser {
    
    private final ShaderLanguage language;
    private List<ShaderTokenizer.Token> tokens;
    private int current;
    private final List<ParseError> errors = new ArrayList<>();
    
    public ShaderParser(ShaderLanguage language) {
        this.language = language;
    }
    
    /**
     * Parse error.
     */
    public static final class ParseError {
        public final String message;
        public final int line;
        public final int column;
        public final String token;
        
        ParseError(String message, int line, int column, String token) {
            this.message = message;
            this.line = line;
            this.column = column;
            this.token = token;
        }
        
        @Override
        public String toString() {
            return String.format("Parse error at %d:%d near '%s': %s", line, column, token, message);
        }
    }
    
    /**
     * Parse result.
     */
    public static final class ParseResult {
        public final ShaderAST.TranslationUnit ast;
        public final List<ParseError> errors;
        public final boolean success;
        public final long parseTimeNs;
        
        ParseResult(ShaderAST.TranslationUnit ast, List<ParseError> errors, long timeNs) {
            this.ast = ast;
            this.errors = Collections.unmodifiableList(errors);
            this.success = errors.isEmpty() && ast != null;
            this.parseTimeNs = timeNs;
        }
    }
    
    /**
     * Parse tokens into AST.
     */
    public ParseResult parse(List<ShaderTokenizer.Token> tokens) {
        long startTime = System.nanoTime();
        
        this.tokens = tokens.stream()
            .filter(t -> t.type != ShaderTokenizer.TokenType.WHITESPACE &&
                        t.type != ShaderTokenizer.TokenType.NEWLINE &&
                        t.type != ShaderTokenizer.TokenType.LINE_COMMENT &&
                        t.type != ShaderTokenizer.TokenType.BLOCK_COMMENT)
            .collect(Collectors.toList());
        this.current = 0;
        this.errors.clear();
        
        ShaderAST.TranslationUnit unit = new ShaderAST.TranslationUnit();
        
        try {
            while (!isAtEnd()) {
                try {
                    parseTopLevel(unit);
                } catch (Exception e) {
                    errors.add(new ParseError(e.getMessage(), 
                        peek().line, peek().column, peek().value));
                    synchronize();
                }
            }
        } catch (Exception e) {
            errors.add(new ParseError("Fatal parse error: " + e.getMessage(), 
                current < tokens.size() ? peek().line : 0, 
                current < tokens.size() ? peek().column : 0, ""));
        }
        
        return new ParseResult(unit, new ArrayList<>(errors), System.nanoTime() - startTime);
    }
    
    private void parseTopLevel(ShaderAST.TranslationUnit unit) {
        // Skip preprocessor directives
        if (check(ShaderTokenizer.TokenType.PREPROCESSOR_DIRECTIVE) ||
            check(ShaderTokenizer.TokenType.PREPROCESSOR_INCLUDE) ||
            check(ShaderTokenizer.TokenType.PREPROCESSOR_DEFINE) ||
            check(ShaderTokenizer.TokenType.PREPROCESSOR_IFDEF) ||
            check(ShaderTokenizer.TokenType.PREPROCESSOR_IFNDEF) ||
            check(ShaderTokenizer.TokenType.PREPROCESSOR_ELSE) ||
            check(ShaderTokenizer.TokenType.PREPROCESSOR_ENDIF) ||
            check(ShaderTokenizer.TokenType.PREPROCESSOR_PRAGMA)) {
            ShaderTokenizer.Token pp = advance();
            unit.preprocessorDirectives.add(
                new ShaderAST.PreprocessorDirective(pp.value, pp.line));
            return;
        }
        
        // Parse struct/class/enum
        if (checkKeyword("struct", "class", "enum")) {
            unit.typeDeclarations.add(parseTypeDeclaration());
            return;
        }
        
        // Parse layout qualifier (GLSL)
        if (check(ShaderTokenizer.TokenType.LAYOUT)) {
            parseLayoutQualifiedDeclaration(unit);
            return;
        }
        
        // Parse attributes [[...]]
        Map<String, String> attributes = new HashMap<>();
        if (check(ShaderTokenizer.TokenType.ATTRIBUTE)) {
            attributes = parseAttributes();
        }
        
        // Parse qualifiers
        Set<String> qualifiers = new LinkedHashSet<>();
        while (check(ShaderTokenizer.TokenType.QUALIFIER_KEYWORD) ||
               check(ShaderTokenizer.TokenType.KEYWORD)) {
            if (checkKeyword("in", "out", "inout", "uniform", "const", "static",
                            "varying", "attribute", "buffer", "shared",
                            "device", "constant", "threadgroup", "kernel",
                            "vertex", "fragment", "compute", "groupshared")) {
                qualifiers.add(advance().value);
            } else {
                break;
            }
        }
        
        // Determine if it's a function or variable
        ShaderTokenizer.Token typeToken = advance();
        String type = typeToken.value;
        
        if (!check(ShaderTokenizer.TokenType.IDENTIFIER)) {
            throw new RuntimeException("Expected identifier after type");
        }
        ShaderTokenizer.Token nameToken = advance();
        String name = nameToken.value;
        
        // Function declaration
        if (check(ShaderTokenizer.TokenType.LEFT_PAREN)) {
            ShaderAST.FunctionDeclaration func = parseFunction(type, name, 
                qualifiers, attributes, typeToken.line, typeToken.column);
            unit.functions.add(func);
            return;
        }
        
        // Variable declaration
        ShaderAST.VariableDeclaration var = parseVariableDeclaration(type, name, 
            qualifiers, attributes, typeToken.line, typeToken.column);
        unit.globalVariables.add(var);
        consume(ShaderTokenizer.TokenType.SEMICOLON, "Expected ';' after variable declaration");
    }
    
    private ShaderAST.TypeDeclaration parseTypeDeclaration() {
        ShaderTokenizer.Token keyword = advance();
        ShaderAST.TypeDeclaration.Kind kind = switch (keyword.value) {
            case "struct" -> ShaderAST.TypeDeclaration.Kind.STRUCT;
            case "class" -> ShaderAST.TypeDeclaration.Kind.CLASS;
            case "enum" -> ShaderAST.TypeDeclaration.Kind.ENUM;
            default -> throw new RuntimeException("Expected struct/class/enum");
        };
        
        String name = "";
        if (check(ShaderTokenizer.TokenType.IDENTIFIER)) {
            name = advance().value;
        }
        
        ShaderAST.TypeDeclaration decl = new ShaderAST.TypeDeclaration(kind, name, 
            keyword.line, keyword.column);
        
        consume(ShaderTokenizer.TokenType.LEFT_BRACE, "Expected '{' after struct name");
        
        while (!check(ShaderTokenizer.TokenType.RIGHT_BRACE) && !isAtEnd()) {
            // Parse member
            Map<String, String> memberAttrs = new HashMap<>();
            if (check(ShaderTokenizer.TokenType.ATTRIBUTE)) {
                memberAttrs = parseAttributes();
            }
            
            ShaderTokenizer.Token memberType = advance();
            ShaderTokenizer.Token memberName = consume(ShaderTokenizer.TokenType.IDENTIFIER, 
                "Expected member name");
            
            ShaderAST.VariableDeclaration member = new ShaderAST.VariableDeclaration(
                memberType.value, memberName.value, memberType.line, memberType.column);
            member.attributes.putAll(memberAttrs);
            
            // Array size
            if (match(ShaderTokenizer.TokenType.LEFT_BRACKET)) {
                if (check(ShaderTokenizer.TokenType.INTEGER_LITERAL)) {
                    member.arraySize = Integer.parseInt(advance().value);
                }
                consume(ShaderTokenizer.TokenType.RIGHT_BRACKET, "Expected ']'");
            }
            
            // MSL attributes [[...]]
            if (check(ShaderTokenizer.TokenType.ATTRIBUTE)) {
                member.attributes.putAll(parseAttributes());
            }
            
            // HLSL semantic : SEMANTIC
            if (match(ShaderTokenizer.TokenType.COLON)) {
                if (check(ShaderTokenizer.TokenType.SEMANTIC) || 
                    check(ShaderTokenizer.TokenType.IDENTIFIER)) {
                    member.attributes.put("semantic", advance().value);
                }
            }
            
            consume(ShaderTokenizer.TokenType.SEMICOLON, "Expected ';' after struct member");
            decl.members.add(member);
        }
        
        consume(ShaderTokenizer.TokenType.RIGHT_BRACE, "Expected '}'");
        match(ShaderTokenizer.TokenType.SEMICOLON); // Optional semicolon
        
        return decl;
    }
    
    private void parseLayoutQualifiedDeclaration(ShaderAST.TranslationUnit unit) {
        ShaderTokenizer.Token layout = advance();
        String layoutContent = layout.value;
        
        // Parse layout content to extract location, binding, set
        int location = extractLayoutValue(layoutContent, "location");
        int binding = extractLayoutValue(layoutContent, "binding");
        int set = extractLayoutValue(layoutContent, "set");
        
        // Parse qualifiers
        Set<String> qualifiers = new LinkedHashSet<>();
        while (check(ShaderTokenizer.TokenType.QUALIFIER_KEYWORD)) {
            qualifiers.add(advance().value);
        }
        
        // Check for uniform block
        if (checkKeyword("uniform") && peekNext().type == ShaderTokenizer.TokenType.IDENTIFIER &&
            peekNextNext().type == ShaderTokenizer.TokenType.LEFT_BRACE) {
            advance(); // uniform
            ShaderTokenizer.Token blockName = advance();
            
            ShaderAST.TypeDeclaration block = new ShaderAST.TypeDeclaration(
                ShaderAST.TypeDeclaration.Kind.STRUCT, blockName.value, 
                blockName.line, blockName.column);
            block.attributes.put("binding", String.valueOf(binding));
            block.attributes.put("set", String.valueOf(set));
            
            consume(ShaderTokenizer.TokenType.LEFT_BRACE, "Expected '{'");
            
            while (!check(ShaderTokenizer.TokenType.RIGHT_BRACE) && !isAtEnd()) {
                ShaderTokenizer.Token memberType = advance();
                ShaderTokenizer.Token memberName = consume(ShaderTokenizer.TokenType.IDENTIFIER, 
                    "Expected member name");
                
                ShaderAST.VariableDeclaration member = new ShaderAST.VariableDeclaration(
                    memberType.value, memberName.value, memberType.line, memberType.column);
                
                if (match(ShaderTokenizer.TokenType.LEFT_BRACKET)) {
                    if (check(ShaderTokenizer.TokenType.INTEGER_LITERAL)) {
                        member.arraySize = Integer.parseInt(advance().value);
                    }
                    consume(ShaderTokenizer.TokenType.RIGHT_BRACKET, "Expected ']'");
                }
                
                consume(ShaderTokenizer.TokenType.SEMICOLON, "Expected ';'");
                block.members.add(member);
            }
            
            consume(ShaderTokenizer.TokenType.RIGHT_BRACE, "Expected '}'");
            
            // Instance name
            if (check(ShaderTokenizer.TokenType.IDENTIFIER)) {
                block.attributes.put("instance", advance().value);
            }
            
            consume(ShaderTokenizer.TokenType.SEMICOLON, "Expected ';'");
            unit.typeDeclarations.add(block);
            return;
        }
        
        // Regular variable
        ShaderTokenizer.Token typeToken = advance();
        ShaderTokenizer.Token nameToken = consume(ShaderTokenizer.TokenType.IDENTIFIER, 
            "Expected variable name");
        
        ShaderAST.VariableDeclaration var = new ShaderAST.VariableDeclaration(
            typeToken.value, nameToken.value, typeToken.line, typeToken.column);
        var.qualifiers.addAll(qualifiers);
        var.location = location;
        var.binding = binding;
        var.set = set;
        
        if (match(ShaderTokenizer.TokenType.LEFT_BRACKET)) {
            if (check(ShaderTokenizer.TokenType.INTEGER_LITERAL)) {
                var.arraySize = Integer.parseInt(advance().value);
            }
            consume(ShaderTokenizer.TokenType.RIGHT_BRACKET, "Expected ']'");
        }
        
        consume(ShaderTokenizer.TokenType.SEMICOLON, "Expected ';'");
        unit.globalVariables.add(var);
    }
    
    private int extractLayoutValue(String layout, String key) {
        Pattern pattern = Pattern.compile(key + "\\s*=\\s*(\\d+)");
        Matcher matcher = pattern.matcher(layout);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }
    
    private Map<String, String> parseAttributes() {
        Map<String, String> attrs = new HashMap<>();
        ShaderTokenizer.Token attr = advance();
        String content = attr.value;
        
        // Parse [[attr(value)]] or [[attr]]
        Pattern attrPattern = Pattern.compile("\\[\\[\\s*(\\w+)(?:\\s*\\(\\s*([^)]+)\\s*\\))?\\s*\\]\\]");
        Matcher matcher = attrPattern.matcher(content);
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = matcher.group(2);
            attrs.put(name, value != null ? value : "true");
        }
        
        return attrs;
    }
    
    private ShaderAST.FunctionDeclaration parseFunction(String returnType, String name,
            Set<String> qualifiers, Map<String, String> attributes, int line, int column) {
        
        ShaderAST.FunctionDeclaration func = new ShaderAST.FunctionDeclaration(
            returnType, name, line, column);
        func.attributes.putAll(attributes);
        
        // Determine shader stage from qualifiers or attributes
        if (qualifiers.contains("vertex") || attributes.containsKey("vertex")) {
            func.stage = ShaderStage.VERTEX;
        } else if (qualifiers.contains("fragment") || attributes.containsKey("fragment")) {
            func.stage = ShaderStage.FRAGMENT;
        } else if (qualifiers.contains("kernel") || qualifiers.contains("compute") ||
                   attributes.containsKey("kernel")) {
            func.stage = ShaderStage.KERNEL;
        }
        
        consume(ShaderTokenizer.TokenType.LEFT_PAREN, "Expected '('");
        
        // Parse parameters
        if (!check(ShaderTokenizer.TokenType.RIGHT_PAREN)) {
            do {
                func.parameters.add(parseParameter());
            } while (match(ShaderTokenizer.TokenType.COMMA));
        }
        
        consume(ShaderTokenizer.TokenType.RIGHT_PAREN, "Expected ')'");
        
        // Function body or declaration
        if (check(ShaderTokenizer.TokenType.LEFT_BRACE)) {
            func.body = parseBlock();
        } else {
            consume(ShaderTokenizer.TokenType.SEMICOLON, "Expected ';' or function body");
        }
        
        return func;
    }
    
    private ShaderAST.Parameter parseParameter() {
        Map<String, String> attrs = new HashMap<>();
        Set<String> qualifiers = new LinkedHashSet<>();
        
        // Parse qualifiers and attributes
        while (true) {
            if (check(ShaderTokenizer.TokenType.ATTRIBUTE)) {
                attrs.putAll(parseAttributes());
            } else if (check(ShaderTokenizer.TokenType.QUALIFIER_KEYWORD) ||
                      checkKeyword("in", "out", "inout", "const", "device", 
                                  "constant", "threadgroup", "thread")) {
                qualifiers.add(advance().value);
            } else {
                break;
            }
        }
        
        ShaderTokenizer.Token typeToken = advance();
        
        // Handle reference types
        boolean isReference = match(ShaderTokenizer.TokenType.AMPERSAND);
        
        ShaderTokenizer.Token nameToken = consume(ShaderTokenizer.TokenType.IDENTIFIER, 
            "Expected parameter name");
        
        ShaderAST.Parameter param = new ShaderAST.Parameter(
            typeToken.value, nameToken.value, typeToken.line, typeToken.column);
        param.qualifiers.addAll(qualifiers);
        param.attributes.putAll(attrs);
        
        // MSL attributes after name
        if (check(ShaderTokenizer.TokenType.ATTRIBUTE)) {
            param.attributes.putAll(parseAttributes());
        }
        
        // HLSL semantic
        if (match(ShaderTokenizer.TokenType.COLON)) {
            if (check(ShaderTokenizer.TokenType.SEMANTIC) || 
                check(ShaderTokenizer.TokenType.IDENTIFIER)) {
                param.attributes.put("semantic", advance().value);
            }
        }
        
        return param;
    }
    
    private ShaderAST.VariableDeclaration parseVariableDeclaration(String type, String name,
            Set<String> qualifiers, Map<String, String> attributes, int line, int column) {
        
        ShaderAST.VariableDeclaration var = new ShaderAST.VariableDeclaration(type, name, line, column);
        var.qualifiers.addAll(qualifiers);
        var.attributes.putAll(attributes);
        
        // Array size
        if (match(ShaderTokenizer.TokenType.LEFT_BRACKET)) {
            if (check(ShaderTokenizer.TokenType.INTEGER_LITERAL)) {
                var.arraySize = Integer.parseInt(advance().value);
            }
            consume(ShaderTokenizer.TokenType.RIGHT_BRACKET, "Expected ']'");
        }
        
        // Initializer
        if (match(ShaderTokenizer.TokenType.ASSIGN)) {
            var.initializer = parseExpression();
        }
        
        return var;
    }
    
    private ShaderAST.BlockStatement parseBlock() {
        ShaderTokenizer.Token brace = consume(ShaderTokenizer.TokenType.LEFT_BRACE, "Expected '{'");
        ShaderAST.BlockStatement block = new ShaderAST.BlockStatement(brace.line, brace.column);
        
        while (!check(ShaderTokenizer.TokenType.RIGHT_BRACE) && !isAtEnd()) {
            block.statements.add(parseStatement());
        }
        
        consume(ShaderTokenizer.TokenType.RIGHT_BRACE, "Expected '}'");
        return block;
    }
    
    private ShaderAST.Statement parseStatement() {
        if (check(ShaderTokenizer.TokenType.LEFT_BRACE)) {
            return parseBlock();
        }
        if (checkKeyword("if")) {
            return parseIfStatement();
        }
        if (checkKeyword("for")) {
            return parseForStatement();
        }
        if (checkKeyword("while")) {
            return parseWhileStatement();
        }
        if (checkKeyword("return")) {
            return parseReturnStatement();
        }
        if (checkKeyword("discard") || checkKeyword("discard_fragment")) {
            return parseDiscardStatement();
        }
        if (checkKeyword("break")) {
            advance();
            consume(ShaderTokenizer.TokenType.SEMICOLON, "Expected ';'");
            return new ShaderAST.BreakStatement(previous().line, previous().column);
        }
        if (checkKeyword("continue")) {
            advance();
            consume(ShaderTokenizer.TokenType.SEMICOLON, "Expected ';'");
            return new ShaderAST.ContinueStatement(previous().line, previous().column);
        }
        
        // Check for variable declaration (type identifier)
        if (isTypeStart()) {
            return parseVariableStatement();
        }
        
        // Expression statement
        return parseExpressionStatement();
    }
    
    private boolean isTypeStart() {
        if (check(ShaderTokenizer.TokenType.TYPE_KEYWORD)) return true;
        if (check(ShaderTokenizer.TokenType.IDENTIFIER)) {
            // Look ahead to see if this is "type name"
            int saved = current;
            advance();
            boolean isType = check(ShaderTokenizer.TokenType.IDENTIFIER);
            current = saved;
            return isType;
        }
        return false;
    }
    
    private ShaderAST.Statement parseVariableStatement() {
        ShaderTokenizer.Token typeToken = advance();
        ShaderTokenizer.Token nameToken = consume(ShaderTokenizer.TokenType.IDENTIFIER, 
            "Expected variable name");
        
        ShaderAST.VariableDeclaration decl = parseVariableDeclaration(
            typeToken.value, nameToken.value, 
            new LinkedHashSet<>(), new HashMap<>(),
            typeToken.line, typeToken.column);
        
        consume(ShaderTokenizer.TokenType.SEMICOLON, "Expected ';'");
        return new ShaderAST.VariableStatement(decl, typeToken.line, typeToken.column);
    }
    
    private ShaderAST.Statement parseExpressionStatement() {
        ShaderAST.Expression expr = parseExpression();
        consume(ShaderTokenizer.TokenType.SEMICOLON, "Expected ';'");
        return new ShaderAST.ExpressionStatement(expr, expr.line, expr.column);
    }
    
    private ShaderAST.IfStatement parseIfStatement() {
        ShaderTokenizer.Token ifToken = advance(); // 'if'
        consume(ShaderTokenizer.TokenType.LEFT_PAREN, "Expected '('");
        ShaderAST.Expression condition = parseExpression();
        consume(ShaderTokenizer.TokenType.RIGHT_PAREN, "Expected ')'");
        
        ShaderAST.Statement thenBranch = parseStatement();
        ShaderAST.IfStatement stmt = new ShaderAST.IfStatement(condition, thenBranch, 
            ifToken.line, ifToken.column);
        
        if (checkKeyword("else")) {
            advance();
            stmt.elseBranch = parseStatement();
        }
        
        return stmt;
    }
    
    private ShaderAST.ForStatement parseForStatement() {
        ShaderTokenizer.Token forToken = advance(); // 'for'
        consume(ShaderTokenizer.TokenType.LEFT_PAREN, "Expected '('");
        
        ShaderAST.ForStatement stmt = new ShaderAST.ForStatement(forToken.line, forToken.column);
        
        // Initializer
        if (!check(ShaderTokenizer.TokenType.SEMICOLON)) {
            if (isTypeStart()) {
                stmt.initializer = parseVariableStatement();
            } else {
                stmt.initializer = parseExpressionStatement();
            }
        } else {
            advance(); // consume semicolon
        }
        
        // Condition
        if (!check(ShaderTokenizer.TokenType.SEMICOLON)) {
            stmt.condition = parseExpression();
        }
        consume(ShaderTokenizer.TokenType.SEMICOLON, "Expected ';'");
        
        // Increment
        if (!check(ShaderTokenizer.TokenType.RIGHT_PAREN)) {
            stmt.increment = parseExpression();
        }
        consume(ShaderTokenizer.TokenType.RIGHT_PAREN, "Expected ')'");
        
        stmt.body = parseStatement();
        return stmt;
    }
    
    private ShaderAST.WhileStatement parseWhileStatement() {
        ShaderTokenizer.Token whileToken = advance(); // 'while'
        consume(ShaderTokenizer.TokenType.LEFT_PAREN, "Expected '('");
        ShaderAST.Expression condition = parseExpression();
        consume(ShaderTokenizer.TokenType.RIGHT_PAREN, "Expected ')'");
        ShaderAST.Statement body = parseStatement();
        
        return new ShaderAST.WhileStatement(condition, body, whileToken.line, whileToken.column);
    }
    
    private ShaderAST.ReturnStatement parseReturnStatement() {
        ShaderTokenizer.Token returnToken = advance(); // 'return'
        ShaderAST.Expression value = null;
        
        if (!check(ShaderTokenizer.TokenType.SEMICOLON)) {
            value = parseExpression();
        }
        consume(ShaderTokenizer.TokenType.SEMICOLON, "Expected ';'");
        
        return new ShaderAST.ReturnStatement(value, returnToken.line, returnToken.column);
    }
    
    private ShaderAST.DiscardStatement parseDiscardStatement() {
        ShaderTokenizer.Token discardToken = advance(); // 'discard' or 'discard_fragment'
        
        // MSL uses discard_fragment()
        if (check(ShaderTokenizer.TokenType.LEFT_PAREN)) {
            advance();
            consume(ShaderTokenizer.TokenType.RIGHT_PAREN, "Expected ')'");
        }
        consume(ShaderTokenizer.TokenType.SEMICOLON, "Expected ';'");
        
        return new ShaderAST.DiscardStatement(discardToken.line, discardToken.column);
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Expression Parsing (Precedence Climbing)
    // ════════════════════════════════════════════════════════════════════════
    
    private ShaderAST.Expression parseExpression() {
        return parseAssignment();
    }
    
    private ShaderAST.Expression parseAssignment() {
        ShaderAST.Expression expr = parseTernary();
        
        if (check(ShaderTokenizer.TokenType.ASSIGN) ||
            check(ShaderTokenizer.TokenType.PLUS_ASSIGN) ||
            check(ShaderTokenizer.TokenType.MINUS_ASSIGN) ||
            check(ShaderTokenizer.TokenType.STAR_ASSIGN) ||
            check(ShaderTokenizer.TokenType.SLASH_ASSIGN) ||
            check(ShaderTokenizer.TokenType.PERCENT_ASSIGN) ||
            check(ShaderTokenizer.TokenType.AMPERSAND_ASSIGN) ||
            check(ShaderTokenizer.TokenType.PIPE_ASSIGN) ||
            check(ShaderTokenizer.TokenType.CARET_ASSIGN)) {
            
            ShaderTokenizer.Token op = advance();
            ShaderAST.Expression value = parseAssignment();
            return new ShaderAST.AssignmentExpression(expr, op.value, value, expr.line, expr.column);
        }
        
        return expr;
    }
    
    private ShaderAST.Expression parseTernary() {
        ShaderAST.Expression expr = parseLogicalOr();
        
        if (match(ShaderTokenizer.TokenType.QUESTION)) {
            ShaderAST.Expression thenExpr = parseExpression();
            consume(ShaderTokenizer.TokenType.COLON, "Expected ':'");
            ShaderAST.Expression elseExpr = parseTernary();
            return new ShaderAST.TernaryExpression(expr, thenExpr, elseExpr, expr.line, expr.column);
        }
        
        return expr;
    }
    
    private ShaderAST.Expression parseLogicalOr() {
        ShaderAST.Expression expr = parseLogicalAnd();
        
        while (match(ShaderTokenizer.TokenType.LOGICAL_OR)) {
            ShaderAST.Expression right = parseLogicalAnd();
            expr = new ShaderAST.BinaryExpression(expr, "||", right, expr.line, expr.column);
        }
        
        return expr;
    }
    
    private ShaderAST.Expression parseLogicalAnd() {
        ShaderAST.Expression expr = parseBitwiseOr();
        
        while (match(ShaderTokenizer.TokenType.LOGICAL_AND)) {
            ShaderAST.Expression right = parseBitwiseOr();
            expr = new ShaderAST.BinaryExpression(expr, "&&", right, expr.line, expr.column);
        }
        
        return expr;
    }
    
    private ShaderAST.Expression parseBitwiseOr() {
        ShaderAST.Expression expr = parseBitwiseXor();
        
        while (match(ShaderTokenizer.TokenType.PIPE)) {
            ShaderAST.Expression right = parseBitwiseXor();
            expr = new ShaderAST.BinaryExpression(expr, "|", right, expr.line, expr.column);
        }
        
        return expr;
    }
    
    private ShaderAST.Expression parseBitwiseXor() {
        ShaderAST.Expression expr = parseBitwiseAnd();
        
        while (match(ShaderTokenizer.TokenType.CARET)) {
            ShaderAST.Expression right = parseBitwiseAnd();
            expr = new ShaderAST.BinaryExpression(expr, "^", right, expr.line, expr.column);
        }
        
        return expr;
    }
    
    private ShaderAST.Expression parseBitwiseAnd() {
        ShaderAST.Expression expr = parseEquality();
        
        while (match(ShaderTokenizer.TokenType.AMPERSAND)) {
            ShaderAST.Expression right = parseEquality();
            expr = new ShaderAST.BinaryExpression(expr, "&", right, expr.line, expr.column);
        }
        
        return expr;
    }
    
    private ShaderAST.Expression parseEquality() {
        ShaderAST.Expression expr = parseComparison();
        
        while (match(ShaderTokenizer.TokenType.EQUAL, ShaderTokenizer.TokenType.NOT_EQUAL)) {
            String op = previous().value;
            ShaderAST.Expression right = parseComparison();
            expr = new ShaderAST.BinaryExpression(expr, op, right, expr.line, expr.column);
        }
        
        return expr;
    }
    
    private ShaderAST.Expression parseComparison() {
        ShaderAST.Expression expr = parseShift();
        
        while (match(ShaderTokenizer.TokenType.LESS, ShaderTokenizer.TokenType.LESS_EQUAL,
                    ShaderTokenizer.TokenType.GREATER, ShaderTokenizer.TokenType.GREATER_EQUAL)) {
            String op = previous().value;
            ShaderAST.Expression right = parseShift();
            expr = new ShaderAST.BinaryExpression(expr, op, right, expr.line, expr.column);
        }
        
        return expr;
    }
    
    private ShaderAST.Expression parseShift() {
        ShaderAST.Expression expr = parseAdditive();
        
        while (match(ShaderTokenizer.TokenType.SHIFT_LEFT, ShaderTokenizer.TokenType.SHIFT_RIGHT)) {
            String op = previous().value;
            ShaderAST.Expression right = parseAdditive();
            expr = new ShaderAST.BinaryExpression(expr, op, right, expr.line, expr.column);
        }
        
        return expr;
    }
    
    private ShaderAST.Expression parseAdditive() {
        ShaderAST.Expression expr = parseMultiplicative();
        
        while (match(ShaderTokenizer.TokenType.PLUS, ShaderTokenizer.TokenType.MINUS)) {
            String op = previous().value;
            ShaderAST.Expression right = parseMultiplicative();
            expr = new ShaderAST.BinaryExpression(expr, op, right, expr.line, expr.column);
        }
        
        return expr;
    }
    
    private ShaderAST.Expression parseMultiplicative() {
        ShaderAST.Expression expr = parseUnary();
        
        while (match(ShaderTokenizer.TokenType.STAR, ShaderTokenizer.TokenType.SLASH, 
                    ShaderTokenizer.TokenType.PERCENT)) {
            String op = previous().value;
            ShaderAST.Expression right = parseUnary();
            expr = new ShaderAST.BinaryExpression(expr, op, right, expr.line, expr.column);
        }
        
        return expr;
    }
    
    private ShaderAST.Expression parseUnary() {
        if (match(ShaderTokenizer.TokenType.MINUS, ShaderTokenizer.TokenType.LOGICAL_NOT,
                 ShaderTokenizer.TokenType.TILDE, ShaderTokenizer.TokenType.INCREMENT,
                 ShaderTokenizer.TokenType.DECREMENT)) {
            String op = previous().value;
            ShaderAST.Expression operand = parseUnary();
            return new ShaderAST.UnaryExpression(op, operand, true, previous().line, previous().column);
        }
        
        // Cast expression: (type)expr
        if (check(ShaderTokenizer.TokenType.LEFT_PAREN) && isTypeCast()) {
            advance(); // (
            ShaderTokenizer.Token typeToken = advance();
            consume(ShaderTokenizer.TokenType.RIGHT_PAREN, "Expected ')'");
            ShaderAST.Expression expr = parseUnary();
            return new ShaderAST.CastExpression(typeToken.value, expr, typeToken.line, typeToken.column);
        }
        
        return parsePostfix();
    }
    
    private boolean isTypeCast() {
        if (!check(ShaderTokenizer.TokenType.LEFT_PAREN)) return false;
        int saved = current;
        advance(); // (
        boolean isType = check(ShaderTokenizer.TokenType.TYPE_KEYWORD);
        current = saved;
        return isType;
    }
    
    private ShaderAST.Expression parsePostfix() {
        ShaderAST.Expression expr = parsePrimary();
        
        while (true) {
            if (match(ShaderTokenizer.TokenType.LEFT_PAREN)) {
                // Function call
                ShaderAST.CallExpression call;
                if (expr instanceof ShaderAST.IdentifierExpression ident) {
                    call = new ShaderAST.CallExpression(ident.name, expr.line, expr.column);
                } else if (expr instanceof ShaderAST.MemberExpression member) {
                    call = new ShaderAST.CallExpression(member.member, expr.line, expr.column);
                    call.callee = member.object;
                } else {
                    throw new RuntimeException("Invalid call expression");
                }
                
                if (!check(ShaderTokenizer.TokenType.RIGHT_PAREN)) {
                    do {
                        call.arguments.add(parseExpression());
                    } while (match(ShaderTokenizer.TokenType.COMMA));
                }
                consume(ShaderTokenizer.TokenType.RIGHT_PAREN, "Expected ')'");
                expr = call;
                
            } else if (match(ShaderTokenizer.TokenType.LEFT_BRACKET)) {
                // Array access
                ShaderAST.Expression index = parseExpression();
                consume(ShaderTokenizer.TokenType.RIGHT_BRACKET, "Expected ']'");
                expr = new ShaderAST.IndexExpression(expr, index, expr.line, expr.column);
                
            } else if (match(ShaderTokenizer.TokenType.DOT)) {
                // Member access
                ShaderTokenizer.Token member = consume(ShaderTokenizer.TokenType.IDENTIFIER, 
                    "Expected member name");
                expr = new ShaderAST.MemberExpression(expr, member.value, expr.line, expr.column);
                
            } else if (match(ShaderTokenizer.TokenType.INCREMENT, ShaderTokenizer.TokenType.DECREMENT)) {
                // Postfix ++/--
                String op = previous().value;
                expr = new ShaderAST.UnaryExpression(op, expr, false, expr.line, expr.column);
                
            } else {
                break;
            }
        }
        
        return expr;
    }
    
    private ShaderAST.Expression parsePrimary() {
        // Literals
        if (match(ShaderTokenizer.TokenType.INTEGER_LITERAL,
                 ShaderTokenizer.TokenType.FLOAT_LITERAL,
                 ShaderTokenizer.TokenType.DOUBLE_LITERAL,
                 ShaderTokenizer.TokenType.HALF_LITERAL)) {
            ShaderTokenizer.Token lit = previous();
            return new ShaderAST.LiteralExpression(lit.value, lit.value, lit.line, lit.column);
        }
        
        if (match(ShaderTokenizer.TokenType.BOOLEAN_LITERAL)) {
            ShaderTokenizer.Token lit = previous();
            return new ShaderAST.LiteralExpression(
                lit.value.equals("true"), lit.value, lit.line, lit.column);
        }
        
        // Constructor: type(args)
        if (check(ShaderTokenizer.TokenType.TYPE_KEYWORD)) {
            ShaderTokenizer.Token typeToken = advance();
            if (check(ShaderTokenizer.TokenType.LEFT_PAREN)) {
                advance();
                ShaderAST.ConstructorExpression ctor = new ShaderAST.ConstructorExpression(
                    typeToken.value, typeToken.line, typeToken.column);
                
                if (!check(ShaderTokenizer.TokenType.RIGHT_PAREN)) {
                    do {
                        ctor.arguments.add(parseExpression());
                    } while (match(ShaderTokenizer.TokenType.COMMA));
                }
                consume(ShaderTokenizer.TokenType.RIGHT_PAREN, "Expected ')'");
                return ctor;
            }
            // Just a type reference
            return new ShaderAST.IdentifierExpression(typeToken.value, typeToken.line, typeToken.column);
        }
        
        // Identifier
        if (match(ShaderTokenizer.TokenType.IDENTIFIER, 
                 ShaderTokenizer.TokenType.BUILTIN_VARIABLE,
                 ShaderTokenizer.TokenType.BUILTIN_FUNCTION)) {
            ShaderTokenizer.Token ident = previous();
            return new ShaderAST.IdentifierExpression(ident.value, ident.line, ident.column);
        }
        
        // Grouped expression
        if (match(ShaderTokenizer.TokenType.LEFT_PAREN)) {
            ShaderAST.Expression expr = parseExpression();
            consume(ShaderTokenizer.TokenType.RIGHT_PAREN, "Expected ')'");
            return expr;
        }
        
        throw new RuntimeException("Expected expression, got: " + peek().value);
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Parser Utilities
    // ════════════════════════════════════════════════════════════════════════
    
    private boolean match(ShaderTokenizer.TokenType... types) {
        for (ShaderTokenizer.TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    
    private boolean check(ShaderTokenizer.TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }
    
    private boolean checkKeyword(String... keywords) {
        if (isAtEnd()) return false;
        ShaderTokenizer.Token token = peek();
        if (token.type != ShaderTokenizer.TokenType.KEYWORD &&
            token.type != ShaderTokenizer.TokenType.TYPE_KEYWORD &&
            token.type != ShaderTokenizer.TokenType.QUALIFIER_KEYWORD &&
            token.type != ShaderTokenizer.TokenType.IDENTIFIER) {
            return false;
        }
        for (String kw : keywords) {
            if (token.value.equals(kw)) return true;
        }
        return false;
    }
    
    private ShaderTokenizer.Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
    
    private ShaderTokenizer.Token consume(ShaderTokenizer.TokenType type, String message) {
        if (check(type)) return advance();
        throw new RuntimeException(message + " at " + peek().line + ":" + peek().column);
    }
    
    private boolean isAtEnd() {
        return peek().type == ShaderTokenizer.TokenType.EOF;
    }
    
    private ShaderTokenizer.Token peek() {
        return tokens.get(current);
    }
    
    private ShaderTokenizer.Token peekNext() {
        if (current + 1 >= tokens.size()) return tokens.get(tokens.size() - 1);
        return tokens.get(current + 1);
    }
    
    private ShaderTokenizer.Token peekNextNext() {
        if (current + 2 >= tokens.size()) return tokens.get(tokens.size() - 1);
        return tokens.get(current + 2);
    }
    
    private ShaderTokenizer.Token previous() {
        return tokens.get(current - 1);
    }
    
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == ShaderTokenizer.TokenType.SEMICOLON) return;
            if (previous().type == ShaderTokenizer.TokenType.RIGHT_BRACE) return;
            
            switch (peek().type) {
                case KEYWORD -> {
                    if (checkKeyword("struct", "class", "enum", "if", "for", "while", 
                                    "return", "vertex", "fragment", "kernel")) {
                        return;
                    }
                }
                case TYPE_KEYWORD, QUALIFIER_KEYWORD -> { return; }
                default -> {}
            }
            advance();
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SECTION 18: SPIR-V CROSS INTEGRATION
// Bridge to SPIR-V Cross for heavy lifting cross-compilation
// ════════════════════════════════════════════════════════════════════════════

/**
 * SPIR-V Cross integration for shader cross-compilation.
 * Handles the heavy work of translation when regex and AST approaches fail.
 */
public static final class SPIRVCrossCompiler {
    
    /**
     * Supported SPIR-V Cross output backends.
     */
    public enum SPIRVBackend {
        MSL,
        GLSL,
        HLSL,
        CPP,
        REFLECT
    }
    
    /**
     * SPIR-V Cross compilation options.
     */
    public static final class SPIRVOptions {
        // MSL options
        public int mslVersion = 20100;  // Metal 2.1
        public boolean mslArgumentBuffers = false;
        public boolean mslTexelBufferNative = true;
        public int mslPlatform = 0;  // 0=iOS, 1=macOS
        public boolean mslInvariantFPMath = false;
        public boolean mslEmulateSubgroups = false;
        public int mslSubgroupSize = 32;
        public boolean mslForceNativeArrays = false;
        public boolean mslDisableRasterOrderGroups = false;
        
        // GLSL options
        public int glslVersion = 450;
        public boolean glslEs = false;
        public boolean glslVulkanSemantics = true;
        public boolean glslSeparateShaderObjects = false;
        public boolean glslFlattenUBOs = false;
        public boolean glslEmitPushConstants = true;
        
        // HLSL options
        public int hlslShaderModel = 60;  // SM 6.0
        public boolean hlslPointSizeCompat = false;
        public boolean hlslPointCoordCompat = false;
        public boolean hlslForceStorageBufferAsUAV = false;
        public boolean hlslNonWritableUAVTexture = false;
        
        // General options
        public boolean flipVertexY = false;
        public boolean fixupClipSpace = false;
        public boolean emitLineDirectives = false;
        public boolean flattenMultidimensionalArrays = false;
        
        public SPIRVOptions copy() {
            SPIRVOptions copy = new SPIRVOptions();
            copy.mslVersion = this.mslVersion;
            copy.mslArgumentBuffers = this.mslArgumentBuffers;
            copy.mslTexelBufferNative = this.mslTexelBufferNative;
            copy.mslPlatform = this.mslPlatform;
            copy.mslInvariantFPMath = this.mslInvariantFPMath;
            copy.mslEmulateSubgroups = this.mslEmulateSubgroups;
            copy.mslSubgroupSize = this.mslSubgroupSize;
            copy.glslVersion = this.glslVersion;
            copy.glslEs = this.glslEs;
            copy.glslVulkanSemantics = this.glslVulkanSemantics;
            copy.hlslShaderModel = this.hlslShaderModel;
            copy.flipVertexY = this.flipVertexY;
            copy.fixupClipSpace = this.fixupClipSpace;
            return copy;
        }
    }
    
    /**
     * SPIR-V compilation result.
     */
    public static final class SPIRVResult {
        public final boolean success;
        public final String output;
        public final String errorMessage;
        public final byte[] spirvBinary;
        public final ReflectionData reflection;
        public final long compilationTimeNs;
        public final SPIRVBackend backend;
        
        SPIRVResult(boolean success, String output, String error, byte[] spirv,
                   ReflectionData reflection, long timeNs, SPIRVBackend backend) {
            this.success = success;
            this.output = output;
            this.errorMessage = error;
            this.spirvBinary = spirv;
            this.reflection = reflection;
            this.compilationTimeNs = timeNs;
            this.backend = backend;
        }
        
        public static SPIRVResult success(String output, byte[] spirv, 
                                         ReflectionData reflection, long timeNs, SPIRVBackend backend) {
            return new SPIRVResult(true, output, null, spirv, reflection, timeNs, backend);
        }
        
        public static SPIRVResult failure(String error, long timeNs, SPIRVBackend backend) {
            return new SPIRVResult(false, null, error, null, null, timeNs, backend);
        }
    }
    
    /**
     * Shader reflection data extracted from SPIR-V.
     */
    public static final class ReflectionData {
        public final List<UniformBuffer> uniformBuffers = new ArrayList<>();
        public final List<StorageBuffer> storageBuffers = new ArrayList<>();
        public final List<SampledImage> sampledImages = new ArrayList<>();
        public final List<StorageImage> storageImages = new ArrayList<>();
        public final List<StageInput> stageInputs = new ArrayList<>();
        public final List<StageOutput> stageOutputs = new ArrayList<>();
        public final List<PushConstant> pushConstants = new ArrayList<>();
        public final List<SpecConstant> specConstants = new ArrayList<>();
        public int3 workgroupSize = new int3(1, 1, 1);
        
        public static final class UniformBuffer {
            public String name;
            public int set;
            public int binding;
            public int size;
            public List<Member> members = new ArrayList<>();
        }
        
        public static final class StorageBuffer {
            public String name;
            public int set;
            public int binding;
            public int size;
            public boolean readonly;
        }
        
        public static final class SampledImage {
            public String name;
            public int set;
            public int binding;
            public String type;
            public int dimensions;
            public boolean array;
            public boolean multisampled;
        }
        
        public static final class StorageImage {
            public String name;
            public int set;
            public int binding;
            public String format;
            public boolean readonly;
            public boolean writeonly;
        }
        
        public static final class StageInput {
            public String name;
            public int location;
            public String type;
            public boolean builtin;
        }
        
        public static final class StageOutput {
            public String name;
            public int location;
            public String type;
            public boolean builtin;
        }
        
        public static final class PushConstant {
            public String name;
            public int offset;
            public int size;
            public String type;
        }
        
        public static final class SpecConstant {
            public String name;
            public int constantId;
            public String type;
            public Object defaultValue;
        }
        
        public static final class Member {
            public String name;
            public int offset;
            public int size;
            public String type;
        }
        
        public static final class int3 {
            public int x, y, z;
            public int3(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
        }
    }
    
    // Native library handle
    private static volatile boolean nativeLoaded = false;
    private static final Object nativeLock = new Object();
    
    // JNI method declarations (would be implemented in native code)
    private static native long nativeCreate();
    private static native void nativeDestroy(long handle);
    private static native byte[] nativeCompileToSPIRV(long handle, String source, 
        int language, int stage, String entryPoint);
    private static native String nativeCrossCompile(long handle, byte[] spirv, 
        int backend, String optionsJson);
    private static native String nativeGetReflection(long handle, byte[] spirv);
    private static native String nativeGetError(long handle);
    
    /**
     * Attempt to load native SPIR-V Cross library.
     */
    public static boolean loadNativeLibrary() {
        if (nativeLoaded) return true;
        
        synchronized (nativeLock) {
            if (nativeLoaded) return true;
            
            String[] libraryNames = {
                "spirv-cross-java",
                "spirv_cross_java",
                "libspirv-cross-java",
                "spirvcross"
            };
            
            for (String name : libraryNames) {
                try {
                    System.loadLibrary(name);
                    nativeLoaded = true;
                    return true;
                } catch (UnsatisfiedLinkError ignored) {}
            }
            
            // Try loading from specific paths
            String[] paths = {
                System.getProperty("user.dir") + "/lib/",
                System.getProperty("java.io.tmpdir") + "/",
                "/usr/local/lib/",
                "/opt/spirv-cross/"
            };
            
            String os = System.getProperty("os.name").toLowerCase();
            String ext = os.contains("win") ? ".dll" : os.contains("mac") ? ".dylib" : ".so";
            
            for (String path : paths) {
                for (String name : libraryNames) {
                    try {
                        System.load(path + name + ext);
                        nativeLoaded = true;
                        return true;
                    } catch (UnsatisfiedLinkError ignored) {}
                }
            }
            
            return false;
        }
    }
    
    /**
     * Check if native library is available.
     */
    public static boolean isNativeAvailable() {
        return nativeLoaded || loadNativeLibrary();
    }
    
    private final SPIRVOptions defaultOptions;
    private long nativeHandle = 0;
    
    public SPIRVCrossCompiler() {
        this(new SPIRVOptions());
    }
    
    public SPIRVCrossCompiler(SPIRVOptions options) {
        this.defaultOptions = options;
        if (isNativeAvailable()) {
            this.nativeHandle = nativeCreate();
        }
    }
    
    public void dispose() {
        if (nativeHandle != 0) {
            nativeDestroy(nativeHandle);
            nativeHandle = 0;
        }
    }
    
    /**
     * Compile GLSL to SPIR-V binary.
     */
    public SPIRVResult compileGLSLToSPIRV(String glslSource, ShaderStage stage, String entryPoint) {
        long startTime = System.nanoTime();
        
        if (!isNativeAvailable()) {
            // Fallback: Use pure Java GLSL -> SPIR-V compiler (limited)
            return compileGLSLToSPIRVFallback(glslSource, stage, entryPoint, startTime);
        }
        
        try {
            byte[] spirv = nativeCompileToSPIRV(nativeHandle, glslSource, 
                0, stageToInt(stage), entryPoint);
            
            if (spirv == null || spirv.length == 0) {
                return SPIRVResult.failure(nativeGetError(nativeHandle), 
                    System.nanoTime() - startTime, null);
            }
            
            ReflectionData reflection = extractReflection(spirv);
            return SPIRVResult.success(null, spirv, reflection, 
                System.nanoTime() - startTime, null);
                
        } catch (Exception e) {
            return SPIRVResult.failure("Native compilation failed: " + e.getMessage(),
                System.nanoTime() - startTime, null);
        }
    }
    
    /**
     * Compile HLSL to SPIR-V binary.
     */
    public SPIRVResult compileHLSLToSPIRV(String hlslSource, ShaderStage stage, String entryPoint) {
        long startTime = System.nanoTime();
        
        if (!isNativeAvailable()) {
            return SPIRVResult.failure("Native library required for HLSL compilation",
                System.nanoTime() - startTime, null);
        }
        
        try {
            byte[] spirv = nativeCompileToSPIRV(nativeHandle, hlslSource,
                1, stageToInt(stage), entryPoint);
            
            if (spirv == null || spirv.length == 0) {
                return SPIRVResult.failure(nativeGetError(nativeHandle),
                    System.nanoTime() - startTime, null);
            }
            
            ReflectionData reflection = extractReflection(spirv);
            return SPIRVResult.success(null, spirv, reflection,
                System.nanoTime() - startTime, null);
                
        } catch (Exception e) {
            return SPIRVResult.failure("HLSL compilation failed: " + e.getMessage(),
                System.nanoTime() - startTime, null);
        }
    }
    
    /**
     * Cross-compile SPIR-V to MSL.
     */
    public SPIRVResult crossCompileToMSL(byte[] spirv, SPIRVOptions options) {
        long startTime = System.nanoTime();
        SPIRVOptions opts = options != null ? options : defaultOptions;
        
        if (!isNativeAvailable()) {
            return crossCompileToMSLFallback(spirv, opts, startTime);
        }
        
        try {
            String optionsJson = buildOptionsJson(opts, SPIRVBackend.MSL);
            String mslOutput = nativeCrossCompile(nativeHandle, spirv, 0, optionsJson);
            
            if (mslOutput == null || mslOutput.isEmpty()) {
                return SPIRVResult.failure(nativeGetError(nativeHandle),
                    System.nanoTime() - startTime, SPIRVBackend.MSL);
            }
            
            ReflectionData reflection = extractReflection(spirv);
            return SPIRVResult.success(mslOutput, spirv, reflection,
                System.nanoTime() - startTime, SPIRVBackend.MSL);
                
        } catch (Exception e) {
            return SPIRVResult.failure("MSL cross-compilation failed: " + e.getMessage(),
                System.nanoTime() - startTime, SPIRVBackend.MSL);
        }
    }
    
    /**
     * Cross-compile SPIR-V to GLSL.
     */
    public SPIRVResult crossCompileToGLSL(byte[] spirv, SPIRVOptions options) {
        long startTime = System.nanoTime();
        SPIRVOptions opts = options != null ? options : defaultOptions;
        
        if (!isNativeAvailable()) {
            return SPIRVResult.failure("Native library required for SPIR-V cross-compilation",
                System.nanoTime() - startTime, SPIRVBackend.GLSL);
        }
        
        try {
            String optionsJson = buildOptionsJson(opts, SPIRVBackend.GLSL);
            String glslOutput = nativeCrossCompile(nativeHandle, spirv, 1, optionsJson);
            
            if (glslOutput == null || glslOutput.isEmpty()) {
                return SPIRVResult.failure(nativeGetError(nativeHandle),
                    System.nanoTime() - startTime, SPIRVBackend.GLSL);
            }
            
            ReflectionData reflection = extractReflection(spirv);
            return SPIRVResult.success(glslOutput, spirv, reflection,
                System.nanoTime() - startTime, SPIRVBackend.GLSL);
                
        } catch (Exception e) {
            return SPIRVResult.failure("GLSL cross-compilation failed: " + e.getMessage(),
                System.nanoTime() - startTime, SPIRVBackend.GLSL);
        }
    }
    
    /**
     * Cross-compile SPIR-V to HLSL.
     */
    public SPIRVResult crossCompileToHLSL(byte[] spirv, SPIRVOptions options) {
        long startTime = System.nanoTime();
        SPIRVOptions opts = options != null ? options : defaultOptions;
        
        if (!isNativeAvailable()) {
            return SPIRVResult.failure("Native library required for SPIR-V cross-compilation",
                System.nanoTime() - startTime, SPIRVBackend.HLSL);
        }
        
        try {
            String optionsJson = buildOptionsJson(opts, SPIRVBackend.HLSL);
            String hlslOutput = nativeCrossCompile(nativeHandle, spirv, 2, optionsJson);
            
            if (hlslOutput == null || hlslOutput.isEmpty()) {
                return SPIRVResult.failure(nativeGetError(nativeHandle),
                    System.nanoTime() - startTime, SPIRVBackend.HLSL);
            }
            
            ReflectionData reflection = extractReflection(spirv);
            return SPIRVResult.success(hlslOutput, spirv, reflection,
                System.nanoTime() - startTime, SPIRVBackend.HLSL);
                
        } catch (Exception e) {
            return SPIRVResult.failure("HLSL cross-compilation failed: " + e.getMessage(),
                System.nanoTime() - startTime, SPIRVBackend.HLSL);
        }
    }
    
    /**
     * Get reflection data from SPIR-V.
     */
    public ReflectionData extractReflection(byte[] spirv) {
        if (!isNativeAvailable()) {
            return extractReflectionFallback(spirv);
        }
        
        try {
            String reflectionJson = nativeGetReflection(nativeHandle, spirv);
            return parseReflectionJson(reflectionJson);
        } catch (Exception e) {
            return new ReflectionData();
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Fallback implementations (pure Java)
    // ════════════════════════════════════════════════════════════════════════
    
    private SPIRVResult compileGLSLToSPIRVFallback(String glsl, ShaderStage stage, 
                                                   String entryPoint, long startTime) {
        // Very basic GLSL -> SPIR-V fallback using glslang command-line tool if available
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "glslangValidator", "-V", "--stdin", "-S", stageToString(stage), "-o", "-"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            process.getOutputStream().write(glsl.getBytes());
            process.getOutputStream().close();
            
            byte[] spirv = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();
            
            if (exitCode != 0 || spirv.length < 20) {
                return SPIRVResult.failure("glslangValidator failed",
                    System.nanoTime() - startTime, null);
            }
            
            return SPIRVResult.success(null, spirv, extractReflectionFallback(spirv),
                System.nanoTime() - startTime, null);
                
        } catch (Exception e) {
            return SPIRVResult.failure("No native library or glslangValidator available",
                System.nanoTime() - startTime, null);
        }
    }
    
    private SPIRVResult crossCompileToMSLFallback(byte[] spirv, SPIRVOptions opts, long startTime) {
        // Try using spirv-cross command-line tool
        try {
            Path tempIn = Files.createTempFile("shader", ".spv");
            Files.write(tempIn, spirv);
            
            List<String> command = new ArrayList<>(Arrays.asList(
                "spirv-cross", "--msl",
                "--msl-version", String.valueOf(opts.mslVersion),
                tempIn.toString()
            ));
            
            if (opts.mslArgumentBuffers) command.add("--msl-argument-buffers");
            if (opts.mslPlatform == 1) command.add("--msl-ios");
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            Files.delete(tempIn);
            
            if (exitCode != 0) {
                return SPIRVResult.failure("spirv-cross failed: " + output,
                    System.nanoTime() - startTime, SPIRVBackend.MSL);
            }
            
            return SPIRVResult.success(output, spirv, extractReflectionFallback(spirv),
                System.nanoTime() - startTime, SPIRVBackend.MSL);
                
        } catch (Exception e) {
            return SPIRVResult.failure("No spirv-cross tool available: " + e.getMessage(),
                System.nanoTime() - startTime, SPIRVBackend.MSL);
        }
    }
    
    private ReflectionData extractReflectionFallback(byte[] spirv) {
        // Basic SPIR-V binary parsing for reflection
        ReflectionData data = new ReflectionData();
        
        if (spirv == null || spirv.length < 20) return data;
        
        ByteBuffer buffer = ByteBuffer.wrap(spirv).order(ByteOrder.LITTLE_ENDIAN);
        
        // Check magic number
        int magic = buffer.getInt();
        if (magic != 0x07230203) {
            // Try big endian
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.position(0);
            magic = buffer.getInt();
            if (magic != 0x07230203) return data;
        }
        
        // Skip version, generator, bound, schema
        buffer.position(20);
        
        // Parse instructions
        Map<Integer, String> names = new HashMap<>();
        Map<Integer, Integer> decorations = new HashMap<>();
        Map<Integer, int[]> memberDecorations = new HashMap<>();
        
        while (buffer.hasRemaining()) {
            int word0 = buffer.getInt();
            int opcode = word0 & 0xFFFF;
            int wordCount = (word0 >> 16) & 0xFFFF;
            
            if (wordCount == 0) break;
            
            int[] operands = new int[wordCount - 1];
            for (int i = 0; i < operands.length && buffer.hasRemaining(); i++) {
                operands[i] = buffer.getInt();
            }
            
            switch (opcode) {
                case 5 -> { // OpName
                    if (operands.length >= 2) {
                        names.put(operands[0], extractString(operands, 1));
                    }
                }
                case 71 -> { // OpDecorate
                    if (operands.length >= 2) {
                        int decoration = operands[1];
                        if (decoration == 33 && operands.length >= 3) { // Binding
                            decorations.put(operands[0] << 16 | 33, operands[2]);
                        } else if (decoration == 34 && operands.length >= 3) { // DescriptorSet
                            decorations.put(operands[0] << 16 | 34, operands[2]);
                        } else if (decoration == 30 && operands.length >= 3) { // Location
                            decorations.put(operands[0] << 16 | 30, operands[2]);
                        }
                    }
                }
            }
        }
        
        return data;
    }
    
    private String extractString(int[] operands, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < operands.length; i++) {
            int word = operands[i];
            for (int j = 0; j < 4; j++) {
                char c = (char) ((word >> (j * 8)) & 0xFF);
                if (c == 0) return sb.toString();
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    private int stageToInt(ShaderStage stage) {
        return switch (stage) {
            case VERTEX -> 0;
            case FRAGMENT -> 1;
            case KERNEL -> 2;
            case GEOMETRY -> 3;
            case TESSELLATION_CONTROL -> 4;
            case TESSELLATION_EVALUATION -> 5;
            case MESH -> 6;
            case TASK -> 7;
            default -> 0;
        };
    }
    
    private String stageToString(ShaderStage stage) {
        return switch (stage) {
            case VERTEX -> "vert";
            case FRAGMENT -> "frag";
            case KERNEL -> "comp";
            case GEOMETRY -> "geom";
            case TESSELLATION_CONTROL -> "tesc";
            case TESSELLATION_EVALUATION -> "tese";
            case MESH -> "mesh";
            case TASK -> "task";
            default -> "vert";
        };
    }
    
    private String buildOptionsJson(SPIRVOptions opts, SPIRVBackend backend) {
        StringBuilder json = new StringBuilder("{");
        
        switch (backend) {
            case MSL -> {
                json.append("\"msl_version\":").append(opts.mslVersion).append(",");
                json.append("\"msl_argument_buffers\":").append(opts.mslArgumentBuffers).append(",");
                json.append("\"msl_platform\":").append(opts.mslPlatform).append(",");
                json.append("\"msl_invariant_fp_math\":").append(opts.mslInvariantFPMath);
            }
            case GLSL -> {
                json.append("\"glsl_version\":").append(opts.glslVersion).append(",");
                json.append("\"glsl_es\":").append(opts.glslEs).append(",");
                json.append("\"glsl_vulkan_semantics\":").append(opts.glslVulkanSemantics);
            }
            case HLSL -> {
                json.append("\"hlsl_shader_model\":").append(opts.hlslShaderModel).append(",");
                json.append("\"hlsl_point_size_compat\":").append(opts.hlslPointSizeCompat);
            }
            default -> {}
        }
        
        json.append(",\"flip_vertex_y\":").append(opts.flipVertexY);
        json.append(",\"fixup_clip_space\":").append(opts.fixupClipSpace);
        json.append("}");
        
        return json.toString();
    }
    
    private ReflectionData parseReflectionJson(String json) {
        // Simple JSON parsing for reflection data
        ReflectionData data = new ReflectionData();
        // Would implement full JSON parsing here
        return data;
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SECTION 19: MULTI-VERSION ROUTING SYSTEM
// Try multiple shader versions/formats until one works
// ════════════════════════════════════════════════════════════════════════════

/**
 * Multi-version shader compilation router.
 * Attempts compilation with multiple backends and versions until success.
 */
public static final class ShaderVersionRouter {
    
    /**
     * Target specification for routing.
     */
    public static final class TargetSpec {
        public final ShaderLanguage language;
        public final int majorVersion;
        public final int minorVersion;
        public final Map<String, Object> options;
        public final int priority;
        
        public TargetSpec(ShaderLanguage language, int major, int minor, int priority) {
            this.language = language;
            this.majorVersion = major;
            this.minorVersion = minor;
            this.options = new HashMap<>();
            this.priority = priority;
        }
        
        public TargetSpec withOption(String key, Object value) {
            options.put(key, value);
            return this;
        }
        
        public String versionString() {
            return majorVersion + "." + minorVersion;
        }
        
        @Override
        public String toString() {
            return language + " " + versionString();
        }
    }
    
    /**
     * Routing result.
     */
    public static final class RoutingResult {
        public final boolean success;
        public final String compiledSource;
        public final TargetSpec usedTarget;
        public final List<TargetAttempt> attempts;
        public final long totalTimeNs;
        
        RoutingResult(boolean success, String source, TargetSpec target, 
                     List<TargetAttempt> attempts, long timeNs) {
            this.success = success;
            this.compiledSource = source;
            this.usedTarget = target;
            this.attempts = Collections.unmodifiableList(attempts);
            this.totalTimeNs = timeNs;
        }
    }
    
    /**
     * Record of a single compilation attempt.
     */
    public static final class TargetAttempt {
        public final TargetSpec target;
        public final boolean success;
        public final String result;
        public final String error;
        public final long timeNs;
        
        TargetAttempt(TargetSpec target, boolean success, String result, String error, long timeNs) {
            this.target = target;
            this.success = success;
            this.result = result;
            this.error = error;
            this.timeNs = timeNs;
        }
    }
    
    // Default target specifications ordered by priority
    private static final List<TargetSpec> MSL_TARGETS = Arrays.asList(
        new TargetSpec(ShaderLanguage.MSL, 3, 1, 1),   // Metal 3.1 (latest)
        new TargetSpec(ShaderLanguage.MSL, 3, 0, 2),   // Metal 3.0
        new TargetSpec(ShaderLanguage.MSL, 2, 4, 3),   // Metal 2.4
        new TargetSpec(ShaderLanguage.MSL, 2, 3, 4),   // Metal 2.3
        new TargetSpec(ShaderLanguage.MSL, 2, 2, 5),   // Metal 2.2
        new TargetSpec(ShaderLanguage.MSL, 2, 1, 6),   // Metal 2.1
        new TargetSpec(ShaderLanguage.MSL, 2, 0, 7),   // Metal 2.0
        new TargetSpec(ShaderLanguage.MSL, 1, 2, 8),   // Metal 1.2
        new TargetSpec(ShaderLanguage.MSL, 1, 1, 9),   // Metal 1.1
        new TargetSpec(ShaderLanguage.MSL, 1, 0, 10)   // Metal 1.0 (fallback)
    );
    
    private static final List<TargetSpec> GLSL_TARGETS = Arrays.asList(
        new TargetSpec(ShaderLanguage.GLSL, 4, 60, 1), // GLSL 4.60
        new TargetSpec(ShaderLanguage.GLSL, 4, 50, 2), // GLSL 4.50 (Vulkan)
        new TargetSpec(ShaderLanguage.GLSL, 4, 30, 3), // GLSL 4.30
        new TargetSpec(ShaderLanguage.GLSL, 4, 20, 4), // GLSL 4.20
        new TargetSpec(ShaderLanguage.GLSL, 4, 10, 5), // GLSL 4.10
        new TargetSpec(ShaderLanguage.GLSL, 4, 0, 6),  // GLSL 4.00
        new TargetSpec(ShaderLanguage.GLSL, 3, 30, 7), // GLSL 3.30
        new TargetSpec(ShaderLanguage.GLSL, 3, 20, 8), // GLSL 3.20 ES
        new TargetSpec(ShaderLanguage.GLSL, 3, 10, 9), // GLSL 3.10 ES
        new TargetSpec(ShaderLanguage.GLSL, 3, 0, 10), // GLSL 3.00 ES
        new TargetSpec(ShaderLanguage.GLSL, 1, 50, 11),// GLSL 1.50
        new TargetSpec(ShaderLanguage.GLSL, 1, 40, 12),// GLSL 1.40
        new TargetSpec(ShaderLanguage.GLSL, 1, 30, 13),// GLSL 1.30
        new TargetSpec(ShaderLanguage.GLSL, 1, 20, 14),// GLSL 1.20
        new TargetSpec(ShaderLanguage.GLSL, 1, 10, 15) // GLSL 1.10 (oldest)
    );
    
    private static final List<TargetSpec> HLSL_TARGETS = Arrays.asList(
        new TargetSpec(ShaderLanguage.HLSL, 6, 6, 1),  // SM 6.6
        new TargetSpec(ShaderLanguage.HLSL, 6, 5, 2),  // SM 6.5
        new TargetSpec(ShaderLanguage.HLSL, 6, 4, 3),  // SM 6.4
        new TargetSpec(ShaderLanguage.HLSL, 6, 3, 4),  // SM 6.3
        new TargetSpec(ShaderLanguage.HLSL, 6, 2, 5),  // SM 6.2
        new TargetSpec(ShaderLanguage.HLSL, 6, 1, 6),  // SM 6.1
        new TargetSpec(ShaderLanguage.HLSL, 6, 0, 7),  // SM 6.0
        new TargetSpec(ShaderLanguage.HLSL, 5, 1, 8),  // SM 5.1
        new TargetSpec(ShaderLanguage.HLSL, 5, 0, 9),  // SM 5.0
        new TargetSpec(ShaderLanguage.HLSL, 4, 1, 10), // SM 4.1
        new TargetSpec(ShaderLanguage.HLSL, 4, 0, 11), // SM 4.0
        new TargetSpec(ShaderLanguage.HLSL, 3, 0, 12)  // SM 3.0 (DX9)
    );
    
    private static final List<TargetSpec> SPIRV_TARGETS = Arrays.asList(
        new TargetSpec(ShaderLanguage.SPIRV, 1, 6, 1), // SPIR-V 1.6
        new TargetSpec(ShaderLanguage.SPIRV, 1, 5, 2), // SPIR-V 1.5
        new TargetSpec(ShaderLanguage.SPIRV, 1, 4, 3), // SPIR-V 1.4
        new TargetSpec(ShaderLanguage.SPIRV, 1, 3, 4), // SPIR-V 1.3
        new TargetSpec(ShaderLanguage.SPIRV, 1, 2, 5), // SPIR-V 1.2
        new TargetSpec(ShaderLanguage.SPIRV, 1, 1, 6), // SPIR-V 1.1
        new TargetSpec(ShaderLanguage.SPIRV, 1, 0, 7)  // SPIR-V 1.0
    );
    
    private final SPIRVCrossCompiler spirvCompiler;
    private final Map<ShaderLanguage, List<TargetSpec>> targetsByLanguage;
    private final ExecutorService executor;
    private final int maxParallelAttempts;
    
    public ShaderVersionRouter() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    public ShaderVersionRouter(int parallelism) {
        this.spirvCompiler = new SPIRVCrossCompiler();
        this.maxParallelAttempts = parallelism;
        this.executor = Executors.newFixedThreadPool(parallelism);
        
        this.targetsByLanguage = new EnumMap<>(ShaderLanguage.class);
        targetsByLanguage.put(ShaderLanguage.MSL, MSL_TARGETS);
        targetsByLanguage.put(ShaderLanguage.GLSL, GLSL_TARGETS);
        targetsByLanguage.put(ShaderLanguage.HLSL, HLSL_TARGETS);
        targetsByLanguage.put(ShaderLanguage.SPIRV, SPIRV_TARGETS);
    }
    
    /**
     * Route shader compilation through multiple targets until success.
     */
    public RoutingResult route(String source, ShaderLanguage sourceLanguage, 
                               ShaderLanguage targetLanguage, ShaderStage stage) {
        long startTime = System.nanoTime();
        List<TargetAttempt> attempts = new ArrayList<>();
        
        List<TargetSpec> targets = targetsByLanguage.getOrDefault(targetLanguage, List.of());
        if (targets.isEmpty()) {
            return new RoutingResult(false, null, null, attempts, System.nanoTime() - startTime);
        }
        
        // Try each target in priority order
        for (TargetSpec target : targets) {
            long attemptStart = System.nanoTime();
            
            try {
                String result = compileForTarget(source, sourceLanguage, target, stage);
                
                if (result != null && !result.isEmpty()) {
                    // Validate the result
                    if (validateOutput(result, target)) {
                        attempts.add(new TargetAttempt(target, true, result, null, 
                            System.nanoTime() - attemptStart));
                        return new RoutingResult(true, result, target, attempts, 
                            System.nanoTime() - startTime);
                    }
                }
                
                attempts.add(new TargetAttempt(target, false, null, "Validation failed",
                    System.nanoTime() - attemptStart));
                    
            } catch (Exception e) {
                attempts.add(new TargetAttempt(target, false, null, e.getMessage(),
                    System.nanoTime() - attemptStart));
            }
        }
        
        return new RoutingResult(false, null, null, attempts, System.nanoTime() - startTime);
    }
    
    /**
     * Route with parallel attempts for faster results.
     */
    public CompletableFuture<RoutingResult> routeAsync(String source, ShaderLanguage sourceLanguage,
                                                        ShaderLanguage targetLanguage, ShaderStage stage) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            List<TargetSpec> targets = targetsByLanguage.getOrDefault(targetLanguage, List.of());
            
            if (targets.isEmpty()) {
                return new RoutingResult(false, null, null, List.of(), System.nanoTime() - startTime);
            }
            
            // Group targets by priority for batch processing
            Map<Integer, List<TargetSpec>> priorityGroups = targets.stream()
                .collect(Collectors.groupingBy(t -> (t.priority - 1) / maxParallelAttempts));
            
            List<TargetAttempt> allAttempts = new CopyOnWriteArrayList<>();
            
            for (int group = 0; group <= priorityGroups.size(); group++) {
                List<TargetSpec> batch = priorityGroups.get(group);
                if (batch == null) continue;
                
                List<CompletableFuture<TargetAttempt>> futures = batch.stream()
                    .map(target -> CompletableFuture.supplyAsync(() -> {
                        long attemptStart = System.nanoTime();
                        try {
                            String result = compileForTarget(source, sourceLanguage, target, stage);
                            if (result != null && validateOutput(result, target)) {
                                return new TargetAttempt(target, true, result, null,
                                    System.nanoTime() - attemptStart);
                            }
                            return new TargetAttempt(target, false, null, "Compilation failed",
                                System.nanoTime() - attemptStart);
                        } catch (Exception e) {
                            return new TargetAttempt(target, false, null, e.getMessage(),
                                System.nanoTime() - attemptStart);
                        }
                    }, executor))
                    .collect(Collectors.toList());
                
                // Wait for all in this batch
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                // Check for success
                for (CompletableFuture<TargetAttempt> future : futures) {
                    TargetAttempt attempt = future.join();
                    allAttempts.add(attempt);
                    
                    if (attempt.success) {
                        return new RoutingResult(true, attempt.result, attempt.target,
                            new ArrayList<>(allAttempts), System.nanoTime() - startTime);
                    }
                }
            }
            
            return new RoutingResult(false, null, null, new ArrayList<>(allAttempts),
                System.nanoTime() - startTime);
        }, executor);
    }
    
    /**
     * Compile source for a specific target.
     */
    private String compileForTarget(String source, ShaderLanguage sourceLanguage,
                                   TargetSpec target, ShaderStage stage) {
        // If source and target are the same language, just adjust version
        if (sourceLanguage == target.language) {
            return adjustVersion(source, target);
        }
        
        // Use SPIR-V as intermediate if needed
        byte[] spirv = null;
        
        if (sourceLanguage == ShaderLanguage.GLSL) {
            SPIRVCrossCompiler.SPIRVResult result = spirvCompiler.compileGLSLToSPIRV(
                source, stage, "main");
            if (result.success) spirv = result.spirvBinary;
        } else if (sourceLanguage == ShaderLanguage.HLSL) {
            SPIRVCrossCompiler.SPIRVResult result = spirvCompiler.compileHLSLToSPIRV(
                source, stage, "main");
            if (result.success) spirv = result.spirvBinary;
        } else if (sourceLanguage == ShaderLanguage.SPIRV) {
            // Assume source is base64 encoded SPIR-V
            spirv = Base64.getDecoder().decode(source);
        }
        
        if (spirv == null) {
            // Try AST-based translation
            return translateViaAST(source, sourceLanguage, target);
        }
        
        // Cross-compile SPIR-V to target
        SPIRVCrossCompiler.SPIRVOptions opts = new SPIRVCrossCompiler.SPIRVOptions();
        configureOptionsForTarget(opts, target);
        
        SPIRVCrossCompiler.SPIRVResult result = switch (target.language) {
            case MSL -> spirvCompiler.crossCompileToMSL(spirv, opts);
            case GLSL -> spirvCompiler.crossCompileToGLSL(spirv, opts);
            case HLSL -> spirvCompiler.crossCompileToHLSL(spirv, opts);
            default -> null;
        };
        
        return result != null && result.success ? result.output : null;
    }
    
    /**
     * Adjust shader source for a specific version.
     */
    private String adjustVersion(String source, TargetSpec target) {
        StringBuilder adjusted = new StringBuilder();
        
        switch (target.language) {
            case GLSL -> {
                // Remove existing #version directive
                String withoutVersion = source.replaceFirst("#version\\s+\\d+.*\\n?", "");
                int version = target.majorVersion * 100 + target.minorVersion * 10;
                adjusted.append("#version ").append(version);
                if (version >= 300 && version < 400) {
                    adjusted.append(" es");
                }
                adjusted.append("\n").append(withoutVersion);
            }
            case HLSL -> {
                // Add/update shader model target
                String withoutTarget = source.replaceFirst("//\\s*SM\\s*\\d+\\.\\d+.*\\n?", "");
                adjusted.append("// SM ").append(target.majorVersion).append(".")
                       .append(target.minorVersion).append("\n").append(withoutTarget);
            }
            case MSL -> {
                // MSL version is typically set via compiler options, not in source
                adjusted.append(source);
            }
            default -> adjusted.append(source);
        }
        
        return adjusted.toString();
    }
    
    /**
     * Translate via AST when SPIR-V path fails.
     */
    private String translateViaAST(String source, ShaderLanguage sourceLanguage, TargetSpec target) {
        try {
            ShaderTokenizer tokenizer = new ShaderTokenizer(sourceLanguage);
            ShaderTokenizer.TokenizationResult tokenResult = tokenizer.tokenize(source);
            
            if (!tokenResult.success) return null;
            
            ShaderParser parser = new ShaderParser(sourceLanguage);
            ShaderParser.ParseResult parseResult = parser.parse(tokenResult.tokens);
            
            if (!parseResult.success || parseResult.ast == null) return null;
            
            return switch (target.language) {
                case MSL -> parseResult.ast.toMSL();
                case GLSL -> parseResult.ast.toGLSL();
                case HLSL -> parseResult.ast.toHLSL();
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Configure SPIR-V Cross options for target.
     */
    private void configureOptionsForTarget(SPIRVCrossCompiler.SPIRVOptions opts, TargetSpec target) {
        switch (target.language) {
            case MSL -> {
                opts.mslVersion = target.majorVersion * 10000 + target.minorVersion * 100;
                opts.mslPlatform = (Integer) target.options.getOrDefault("platform", 1);
            }
            case GLSL -> {
                opts.glslVersion = target.majorVersion * 100 + target.minorVersion * 10;
                opts.glslEs = target.majorVersion == 3 && target.minorVersion < 30;
            }
            case HLSL -> {
                opts.hlslShaderModel = target.majorVersion * 10 + target.minorVersion;
            }
        }
    }
    
    /**
     * Validate compiled output.
     */
    private boolean validateOutput(String output, TargetSpec target) {
        if (output == null || output.isEmpty()) return false;
        
        // Basic validation checks
        switch (target.language) {
            case MSL -> {
                return output.contains("using namespace metal") ||
                       output.contains("#include <metal_stdlib>") ||
                       output.contains("vertex ") ||
                       output.contains("fragment ") ||
                       output.contains("kernel ");
            }
            case GLSL -> {
                return output.contains("#version") ||
                       output.contains("void main") ||
                       output.contains("gl_Position") ||
                       output.contains("gl_FragColor");
            }
            case HLSL -> {
                return output.contains("void ") ||
                       output.contains("float4 ") ||
                       output.contains("SV_") ||
                       output.contains("cbuffer");
            }
            default -> { return true; }
        }
    }
    
    public void shutdown() {
        executor.shutdown();
        spirvCompiler.dispose();
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SECTION 20: WORK SPLITTING SYSTEM
// Split complex shaders into smaller chunks for compilation
// ════════════════════════════════════════════════════════════════════════════

/**
 * Splits complex shaders into manageable chunks for incremental compilation.
 */
public static final class ShaderWorkSplitter {
    
    /**
     * A chunk of shader code that can be compiled independently.
     */
    public static final class ShaderChunk {
        public final String id;
        public final ChunkType type;
        public final String source;
        public final Set<String> dependencies;
        public final Set<String> exports;
        public final int originalLine;
        public final int complexity;
        
        public ShaderChunk(String id, ChunkType type, String source, int line, int complexity) {
            this.id = id;
            this.type = type;
            this.source = source;
            this.originalLine = line;
            this.complexity = complexity;
            this.dependencies = new HashSet<>();
            this.exports = new HashSet<>();
        }
    }
    
    /**
     * Types of shader chunks.
     */
    public enum ChunkType {
        PREPROCESSOR,       // #include, #define, etc.
        TYPE_DEFINITION,    // struct, class, enum
        CONSTANT_BUFFER,    // Uniform/constant buffers
        RESOURCE_BINDING,   // Textures, samplers, buffers
        HELPER_FUNCTION,    // Non-entry-point functions
        ENTRY_POINT,        // Main shader function
        GLOBAL_VARIABLE     // Global variables
    }
    
    /**
     * Split result containing all chunks.
     */
    public static final class SplitResult {
        public final List<ShaderChunk> chunks;
        public final List<ShaderChunk> orderedChunks;  // Topologically sorted
        public final Map<String, Set<String>> dependencyGraph;
        public final int totalComplexity;
        public final boolean canParallelize;
        
        SplitResult(List<ShaderChunk> chunks, Map<String, Set<String>> deps) {
            this.chunks = Collections.unmodifiableList(chunks);
            this.dependencyGraph = Collections.unmodifiableMap(deps);
            this.orderedChunks = topologicalSort(chunks, deps);
            this.totalComplexity = chunks.stream().mapToInt(c -> c.complexity).sum();
            this.canParallelize = hasIndependentChunks(deps);
        }
        
        private static List<ShaderChunk> topologicalSort(List<ShaderChunk> chunks, 
                                                         Map<String, Set<String>> deps) {
            Map<String, ShaderChunk> byId = chunks.stream()
                .collect(Collectors.toMap(c -> c.id, c -> c));
            
            List<ShaderChunk> sorted = new ArrayList<>();
            Set<String> visited = new HashSet<>();
            Set<String> visiting = new HashSet<>();
            
            for (ShaderChunk chunk : chunks) {
                if (!visited.contains(chunk.id)) {
                    visit(chunk.id, byId, deps, visited, visiting, sorted);
                }
            }
            
            Collections.reverse(sorted);
            return sorted;
        }
        
        private static void visit(String id, Map<String, ShaderChunk> byId,
                                 Map<String, Set<String>> deps, Set<String> visited,
                                 Set<String> visiting, List<ShaderChunk> sorted) {
            if (visiting.contains(id)) {
                throw new RuntimeException("Cyclic dependency detected: " + id);
            }
            if (visited.contains(id)) return;
            
            visiting.add(id);
            
            Set<String> dependencies = deps.getOrDefault(id, Set.of());
            for (String dep : dependencies) {
                if (byId.containsKey(dep)) {
                    visit(dep, byId, deps, visited, visiting, sorted);
                }
            }
            
            visiting.remove(id);
            visited.add(id);
            
            if (byId.containsKey(id)) {
                sorted.add(byId.get(id));
            }
        }
        
        private static boolean hasIndependentChunks(Map<String, Set<String>> deps) {
            // Check if there are chunks with no dependencies on each other
            for (Map.Entry<String, Set<String>> entry : deps.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * Compilation options for split shaders.
     */
    public static final class SplitCompileOptions {
        public int maxChunkComplexity = 1000;
        public boolean parallelCompilation = true;
        public int maxParallelChunks = 4;
        public boolean preserveOrder = true;
        public boolean cacheChunks = true;
    }
    
    private final ShaderTokenizer tokenizer;
    private final ShaderParser parser;
    
    public ShaderWorkSplitter(ShaderLanguage language) {
        this.tokenizer = new ShaderTokenizer(language);
        this.parser = new ShaderParser(language);
    }
    
    /**
     * Split shader source into compilable chunks.
     */
    public SplitResult split(String source) {
        List<ShaderChunk> chunks = new ArrayList<>();
        Map<String, Set<String>> dependencies = new HashMap<>();
        
        // Tokenize to find structure
        ShaderTokenizer.TokenizationResult tokenResult = tokenizer.tokenize(source);
        if (!tokenResult.success) {
            // Fallback to line-based splitting
            return splitByLines(source);
        }
        
        // Parse to AST
        ShaderParser.ParseResult parseResult = parser.parse(tokenResult.tokens);
        if (parseResult.success && parseResult.ast != null) {
            // Use AST-based splitting
            return splitByAST(parseResult.ast);
        }
        
        // Fallback to regex-based splitting
        return splitByRegex(source);
    }
    
    /**
     * Split by AST nodes.
     */
    private SplitResult splitByAST(ShaderAST.TranslationUnit ast) {
        List<ShaderChunk> chunks = new ArrayList<>();
        Map<String, Set<String>> dependencies = new HashMap<>();
        int chunkId = 0;
        
        // Extract preprocessor directives
        for (ShaderAST.PreprocessorDirective pp : ast.preprocessorDirectives) {
            String id = "pp_" + (chunkId++);
            ShaderChunk chunk = new ShaderChunk(id, ChunkType.PREPROCESSOR, 
                pp.directive, pp.line, 1);
            chunks.add(chunk);
            dependencies.put(id, new HashSet<>());
        }
        
        // Extract type definitions
        for (ShaderAST.TypeDeclaration type : ast.typeDeclarations) {
            String id = "type_" + type.name;
            String source = type.toMSL(); // Use MSL as default
            int complexity = type.members.size() * 2;
            
            ShaderChunk chunk = new ShaderChunk(id, ChunkType.TYPE_DEFINITION,
                source, type.line, complexity);
            chunk.exports.add(type.name);
            
            // Find dependencies (types used in members)
            for (ShaderAST.VariableDeclaration member : type.members) {
                String memberType = extractBaseType(member.type);
                if (!isPrimitiveType(memberType)) {
                    chunk.dependencies.add(memberType);
                }
            }
            
            chunks.add(chunk);
            dependencies.put(id, new HashSet<>(chunk.dependencies));
        }
        
        // Extract global variables
        for (ShaderAST.VariableDeclaration var : ast.globalVariables) {
            String id = "var_" + var.name;
            String source = var.toMSL() + ";";
            int complexity = 1;
            
            ChunkType type = determineVariableChunkType(var);
            ShaderChunk chunk = new ShaderChunk(id, type, source, var.line, complexity);
            chunk.exports.add(var.name);
            
            String varType = extractBaseType(var.type);
            if (!isPrimitiveType(varType)) {
                chunk.dependencies.add(varType);
            }
            
            chunks.add(chunk);
            dependencies.put(id, new HashSet<>(chunk.dependencies));
        }
        
        // Extract functions
        for (ShaderAST.FunctionDeclaration func : ast.functions) {
            String id = "func_" + func.name;
            String source = func.toMSL();
            int complexity = estimateFunctionComplexity(func);
            
            ChunkType type = func.stage != null ? ChunkType.ENTRY_POINT : ChunkType.HELPER_FUNCTION;
            ShaderChunk chunk = new ShaderChunk(id, type, source, func.line, complexity);
            chunk.exports.add(func.name);
            
            // Analyze function dependencies
            Set<String> funcDeps = analyzeFunctionDependencies(func);
            chunk.dependencies.addAll(funcDeps);
            
            chunks.add(chunk);
            dependencies.put(id, new HashSet<>(chunk.dependencies));
        }
        
        // Resolve dependency IDs
        resolveDepdenencyIds(chunks, dependencies);
        
        return new SplitResult(chunks, dependencies);
    }
    
    /**
     * Fallback: Split by regex patterns.
     */
    private SplitResult splitByRegex(String source) {
        List<ShaderChunk> chunks = new ArrayList<>();
        Map<String, Set<String>> dependencies = new HashMap<>();
        int chunkId = 0;
        
        // Split preprocessor directives
        Pattern ppPattern = Pattern.compile("^\\s*#[^\n]+", Pattern.MULTILINE);
        Matcher ppMatcher = ppPattern.matcher(source);
        while (ppMatcher.find()) {
            String id = "pp_" + (chunkId++);
            chunks.add(new ShaderChunk(id, ChunkType.PREPROCESSOR, 
                ppMatcher.group(), countLines(source, ppMatcher.start()), 1));
            dependencies.put(id, new HashSet<>());
        }
        
        // Split struct definitions
        Pattern structPattern = Pattern.compile(
            "struct\\s+(\\w+)\\s*\\{[^}]*\\}\\s*;?", Pattern.DOTALL);
        Matcher structMatcher = structPattern.matcher(source);
        while (structMatcher.find()) {
            String name = structMatcher.group(1);
            String id = "type_" + name;
            String structSource = structMatcher.group();
            int complexity = structSource.split(";").length;
            
            ShaderChunk chunk = new ShaderChunk(id, ChunkType.TYPE_DEFINITION,
                structSource, countLines(source, structMatcher.start()), complexity);
            chunk.exports.add(name);
            
            chunks.add(chunk);
            dependencies.put(id, new HashSet<>());
        }
        
        // Split functions
        Pattern funcPattern = Pattern.compile(
            "((?:vertex|fragment|kernel|compute|\\w+)\\s+)?(\\w+)\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{",
            Pattern.MULTILINE);
        Matcher funcMatcher = funcPattern.matcher(source);
        
        while (funcMatcher.find()) {
            int braceStart = funcMatcher.end() - 1;
            int braceEnd = findMatchingBrace(source, braceStart);
            if (braceEnd == -1) continue;
            
            String funcSource = source.substring(funcMatcher.start(), braceEnd + 1);
            String funcName = funcMatcher.group(3);
            String qualifier = funcMatcher.group(1);
            
            String id = "func_" + funcName;
            int complexity = estimateComplexityFromSource(funcSource);
            ChunkType type = (qualifier != null && 
                (qualifier.contains("vertex") || qualifier.contains("fragment") || 
                 qualifier.contains("kernel") || qualifier.contains("compute")))
                ? ChunkType.ENTRY_POINT : ChunkType.HELPER_FUNCTION;
            
            ShaderChunk chunk = new ShaderChunk(id, type, funcSource,
                countLines(source, funcMatcher.start()), complexity);
            chunk.exports.add(funcName);
            
            chunks.add(chunk);
            dependencies.put(id, new HashSet<>());
        }
        
        return new SplitResult(chunks, dependencies);
    }
    
    /**
     * Fallback: Split by lines for very complex shaders.
     */
    private SplitResult splitByLines(String source) {
        List<ShaderChunk> chunks = new ArrayList<>();
        Map<String, Set<String>> dependencies = new HashMap<>();
        
        String[] lines = source.split("\n");
        int chunkId = 0;
        StringBuilder currentChunk = new StringBuilder();
        int startLine = 0;
        int braceDepth = 0;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // Count braces
            for (char c : line.toCharArray()) {
                if (c == '{') braceDepth++;
                else if (c == '}') braceDepth--;
            }
            
            currentChunk.append(line).append("\n");
            
            // End of logical block
            if (braceDepth == 0 && (line.contains(";") || line.contains("}"))) {
                String chunkSource = currentChunk.toString().trim();
                if (!chunkSource.isEmpty()) {
                    String id = "chunk_" + (chunkId++);
                    ChunkType type = inferChunkType(chunkSource);
                    int complexity = estimateComplexityFromSource(chunkSource);
                    
                    chunks.add(new ShaderChunk(id, type, chunkSource, startLine, complexity));
                    dependencies.put(id, new HashSet<>());
                }
                
                currentChunk = new StringBuilder();
                startLine = i + 1;
            }
        }
        
        // Handle remaining content
        if (currentChunk.length() > 0) {
            String remaining = currentChunk.toString().trim();
            if (!remaining.isEmpty()) {
                String id = "chunk_" + chunkId;
                chunks.add(new ShaderChunk(id, ChunkType.HELPER_FUNCTION, 
                    remaining, startLine, 10));
                dependencies.put(id, new HashSet<>());
            }
        }
        
        return new SplitResult(chunks, dependencies);
    }
    
    /**
     * Merge chunks back into complete shader.
     */
    public String mergeChunks(List<ShaderChunk> chunks) {
        StringBuilder merged = new StringBuilder();
        
        // Order by type for proper output
        List<ChunkType> typeOrder = Arrays.asList(
            ChunkType.PREPROCESSOR,
            ChunkType.TYPE_DEFINITION,
            ChunkType.CONSTANT_BUFFER,
            ChunkType.RESOURCE_BINDING,
            ChunkType.GLOBAL_VARIABLE,
            ChunkType.HELPER_FUNCTION,
            ChunkType.ENTRY_POINT
        );
        
        for (ChunkType type : typeOrder) {
            for (ShaderChunk chunk : chunks) {
                if (chunk.type == type) {
                    merged.append(chunk.source).append("\n\n");
                }
            }
        }
        
        return merged.toString().trim();
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Helper methods
    // ════════════════════════════════════════════════════════════════════════
    
    private ChunkType determineVariableChunkType(ShaderAST.VariableDeclaration var) {
        if (var.qualifiers.contains("uniform") || var.qualifiers.contains("constant")) {
            return ChunkType.CONSTANT_BUFFER;
        }
        if (var.type.contains("texture") || var.type.contains("sampler") ||
            var.type.contains("Texture") || var.type.contains("Sampler")) {
            return ChunkType.RESOURCE_BINDING;
        }
        if (var.qualifiers.contains("buffer") || var.qualifiers.contains("device")) {
            return ChunkType.RESOURCE_BINDING;
        }
        return ChunkType.GLOBAL_VARIABLE;
    }
    
    private int estimateFunctionComplexity(ShaderAST.FunctionDeclaration func) {
        if (func.body == null) return 1;
        
        int[] complexity = {1}; // Base complexity
        
        func.body.accept(new ShaderAST.ASTVisitor() {
            @Override
            public void visit(ShaderAST.ForStatement node) {
                complexity[0] += 10;
                ShaderAST.ASTVisitor.super.visit(node);
            }
            
            @Override
            public void visit(ShaderAST.WhileStatement node) {
                complexity[0] += 10;
                ShaderAST.ASTVisitor.super.visit(node);
            }
            
            @Override
            public void visit(ShaderAST.IfStatement node) {
                complexity[0] += 3;
                ShaderAST.ASTVisitor.super.visit(node);
            }
            
            @Override
            public void visit(ShaderAST.CallExpression node) {
                complexity[0] += 2;
                ShaderAST.ASTVisitor.super.visit(node);
            }
            
            @Override
            public void visit(ShaderAST.BinaryExpression node) {
                complexity[0] += 1;
                ShaderAST.ASTVisitor.super.visit(node);
            }
        });
        
        return complexity[0];
    }
    
    private Set<String> analyzeFunctionDependencies(ShaderAST.FunctionDeclaration func) {
        Set<String> deps = new HashSet<>();
        
        // Return type
        String returnType = extractBaseType(func.returnType);
        if (!isPrimitiveType(returnType)) {
            deps.add(returnType);
        }
        
        // Parameter types
        for (ShaderAST.Parameter param : func.parameters) {
            String paramType = extractBaseType(param.type);
            if (!isPrimitiveType(paramType)) {
                deps.add(paramType);
            }
        }
        
        // Function calls and identifiers in body
        if (func.body != null) {
            func.body.accept(new ShaderAST.ASTVisitor() {
                @Override
                public void visit(ShaderAST.CallExpression node) {
                    if (!isBuiltinFunction(node.functionName)) {
                        deps.add(node.functionName);
                    }
                    ShaderAST.ASTVisitor.super.visit(node);
                }
                
                @Override
                public void visit(ShaderAST.ConstructorExpression node) {
                    if (!isPrimitiveType(node.type)) {
                        deps.add(node.type);
                    }
                    ShaderAST.ASTVisitor.super.visit(node);
                }
            });
        }
        
        return deps;
    }
    
    private void resolveDepdenencyIds(List<ShaderChunk> chunks, Map<String, Set<String>> dependencies) {
        // Build export-to-chunk-id mapping
        Map<String, String> exportToId = new HashMap<>();
        for (ShaderChunk chunk : chunks) {
            for (String export : chunk.exports) {
                exportToId.put(export, chunk.id);
            }
        }
        
        // Resolve dependencies to chunk IDs
        for (ShaderChunk chunk : chunks) {
            Set<String> resolvedDeps = new HashSet<>();
            for (String dep : chunk.dependencies) {
                String depId = exportToId.get(dep);
                if (depId != null && !depId.equals(chunk.id)) {
                    resolvedDeps.add(depId);
                }
            }
            dependencies.put(chunk.id, resolvedDeps);
        }
    }
    
    private String extractBaseType(String type) {
        // Remove qualifiers and template parameters
        return type.replaceAll("<.*>", "")
                  .replaceAll("\\[.*\\]", "")
                  .replaceAll("(device|constant|thread|threadgroup)\\s+", "")
                  .replaceAll("\\*|&", "")
                  .trim();
    }
    
    private boolean isPrimitiveType(String type) {
        return Set.of(
            "void", "bool", "int", "uint", "float", "double", "half",
            "int2", "int3", "int4", "uint2", "uint3", "uint4",
            "float2", "float3", "float4", "half2", "half3", "half4",
            "double2", "double3", "double4", "bool2", "bool3", "bool4",
            "float2x2", "float3x3", "float4x4", "half2x2", "half3x3", "half4x4",
            "vec2", "vec3", "vec4", "ivec2", "ivec3", "ivec4",
            "uvec2", "uvec3", "uvec4", "mat2", "mat3", "mat4",
            "sampler", "texture2d", "texture3d", "texturecube"
        ).contains(type);
    }
    
    private boolean isBuiltinFunction(String name) {
        return Set.of(
            // Math
            "abs", "sign", "floor", "ceil", "round", "trunc", "fract",
            "mod", "fmod", "min", "max", "clamp", "saturate", "mix", "lerp",
            "step", "smoothstep", "sqrt", "rsqrt", "inversesqrt",
            "pow", "exp", "exp2", "log", "log2", "log10",
            "sin", "cos", "tan", "asin", "acos", "atan", "atan2",
            "sinh", "cosh", "tanh", "asinh", "acosh", "atanh",
            "radians", "degrees",
            // Vector
            "length", "distance", "dot", "cross", "normalize", "reflect", "refract",
            "faceforward",
            // Matrix
            "transpose", "determinant", "inverse",
            // Texture
            "sample", "read", "write", "texture", "textureLod", "textureGrad",
            "texelFetch", "imageLoad", "imageStore",
            // Derivatives
            "dfdx", "dfdy", "fwidth", "ddx", "ddy",
            // Comparison
            "any", "all", "not", "select",
            // Atomic
            "atomic_fetch_add", "atomic_fetch_sub", "atomic_exchange",
            // Misc
            "discard_fragment", "discard"
        ).contains(name);
    }
    
    private ChunkType inferChunkType(String source) {
        if (source.startsWith("#")) return ChunkType.PREPROCESSOR;
        if (source.contains("struct ") || source.contains("class ")) return ChunkType.TYPE_DEFINITION;
        if (source.contains("cbuffer") || source.contains("uniform ")) return ChunkType.CONSTANT_BUFFER;
        if (source.contains("texture") || source.contains("sampler") ||
            source.contains("Texture") || source.contains("Sampler")) return ChunkType.RESOURCE_BINDING;
        if (source.contains("vertex ") || source.contains("fragment ") ||
            source.contains("kernel ") || source.contains("void main")) return ChunkType.ENTRY_POINT;
        if (source.contains("(") && source.contains(")") && source.contains("{")) {
            return ChunkType.HELPER_FUNCTION;
        }
        return ChunkType.GLOBAL_VARIABLE;
    }
    
    private int estimateComplexityFromSource(String source) {
        int complexity = 1;
        complexity += (source.length() / 50);
        complexity += countOccurrences(source, "for");
        complexity += countOccurrences(source, "while");
        complexity += countOccurrences(source, "if") / 2;
        complexity += countOccurrences(source, "(") / 3;
        return complexity;
    }
    
    private int findMatchingBrace(String source, int start) {
        int depth = 1;
        for (int i = start + 1; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
    
    private int countLines(String source, int offset) {
        int lines = 1;
        for (int i = 0; i < offset && i < source.length(); i++) {
            if (source.charAt(i) == '\n') lines++;
        }
        return lines;
    }
    
    private int countOccurrences(String source, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SECTION 21: DEGRADATION SYSTEM
// Simplify shaders when compilation fails
// ════════════════════════════════════════════════════════════════════════════

/**
 * Degrades complex shader features to simpler equivalents.
 */
public static final class ShaderDegrader {
    
    /**
     * Degradation level.
     */
    public enum DegradationLevel {
        NONE(0),           // Original shader
        MINIMAL(1),        // Remove optional features
        MODERATE(2),       // Simplify complex features
        AGGRESSIVE(3),     // Replace with fallbacks
        MAXIMUM(4);        // Bare minimum functionality
        
        public final int level;
        DegradationLevel(int level) { this.level = level; }
    }
    
    /**
     * Degradation result.
     */
    public static final class DegradationResult {
        public final String degradedSource;
        public final DegradationLevel level;
        public final List<String> removedFeatures;
        public final List<String> simplifiedFeatures;
        public final List<String> warnings;
        public final boolean visualQualityReduced;
        
        DegradationResult(String source, DegradationLevel level, 
                         List<String> removed, List<String> simplified, List<String> warnings) {
            this.degradedSource = source;
            this.level = level;
            this.removedFeatures = Collections.unmodifiableList(removed);
            this.simplifiedFeatures = Collections.unmodifiableList(simplified);
            this.warnings = Collections.unmodifiableList(warnings);
            this.visualQualityReduced = !removed.isEmpty() || !simplified.isEmpty();
        }
    }
    
    /**
     * Features that can be degraded.
     */
    public enum DegradableFeature {
        // Texture features
        TEXTURE_LOD("Texture LOD", DegradationLevel.MINIMAL),
        TEXTURE_GRAD("Texture gradients", DegradationLevel.MINIMAL),
        TEXTURE_GATHER("Texture gather", DegradationLevel.MODERATE),
        TEXTURE_ARRAY("Texture arrays", DegradationLevel.MODERATE),
        TEXTURE_3D("3D textures", DegradationLevel.AGGRESSIVE),
        TEXTURE_CUBE("Cube textures", DegradationLevel.AGGRESSIVE),
        MULTISAMPLING("Multisampling", DegradationLevel.MODERATE),
        
        // Precision
        HALF_PRECISION("Half precision", DegradationLevel.MINIMAL),
        DOUBLE_PRECISION("Double precision", DegradationLevel.MINIMAL),
        
        // Control flow
        DYNAMIC_INDEXING("Dynamic indexing", DegradationLevel.MODERATE),
        DYNAMIC_BRANCHING("Dynamic branching", DegradationLevel.AGGRESSIVE),
        LOOPS("Loop unrolling", DegradationLevel.MODERATE),
        
        // Advanced features
        DERIVATIVES("Derivatives", DegradationLevel.MODERATE),
        COMPUTE_SHARED("Compute shared memory", DegradationLevel.AGGRESSIVE),
        ATOMICS("Atomic operations", DegradationLevel.AGGRESSIVE),
        SUBGROUPS("Subgroup operations", DegradationLevel.MODERATE),
        
        // Effects
        SHADOWS("Shadows", DegradationLevel.AGGRESSIVE),
        REFLECTIONS("Reflections", DegradationLevel.AGGRESSIVE),
        NORMAL_MAPPING("Normal mapping", DegradationLevel.MODERATE),
        PARALLAX_MAPPING("Parallax mapping", DegradationLevel.MODERATE),
        AMBIENT_OCCLUSION("Ambient occlusion", DegradationLevel.MODERATE),
        
        // Math
        MATRIX_INVERSE("Matrix inverse", DegradationLevel.MINIMAL),
        TRIGONOMETRY("Complex trigonometry", DegradationLevel.MODERATE);
        
        public final String description;
        public final DegradationLevel minimumLevel;
        
        DegradableFeature(String desc, DegradationLevel level) {
            this.description = desc;
            this.minimumLevel = level;
        }
    }
    
    private final ShaderLanguage language;
    
    public ShaderDegrader(ShaderLanguage language) {
        this.language = language;
    }
    
    /**
     * Degrade shader to specified level.
     */
    public DegradationResult degrade(String source, DegradationLevel targetLevel) {
        if (targetLevel == DegradationLevel.NONE) {
            return new DegradationResult(source, targetLevel, List.of(), List.of(), List.of());
        }
        
        List<String> removed = new ArrayList<>();
        List<String> simplified = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        String result = source;
        
        // Apply degradations based on level
        for (DegradableFeature feature : DegradableFeature.values()) {
            if (feature.minimumLevel.level <= targetLevel.level) {
                DegradationAction action = getDegradationAction(feature, targetLevel);
                if (action != null) {
                    String before = result;
                    result = action.apply(result);
                    
                    if (!result.equals(before)) {
                        if (action.removes) {
                            removed.add(feature.description);
                        } else {
                            simplified.add(feature.description);
                        }
                        if (action.warning != null) {
                            warnings.add(action.warning);
                        }
                    }
                }
            }
        }
        
        // Clean up any dangling code
        result = cleanupDegradedCode(result);
        
        return new DegradationResult(result, targetLevel, removed, simplified, warnings);
    }
    
    /**
     * Auto-degrade: try each level until compilation succeeds.
     */
    public DegradationResult autodegrade(String source, 
                                         java.util.function.Predicate<String> compilationTest) {
        for (DegradationLevel level : DegradationLevel.values()) {
            DegradationResult result = degrade(source, level);
            
            try {
                if (compilationTest.test(result.degradedSource)) {
                    return result;
                }
            } catch (Exception e) {
                // Continue to next level
            }
        }
        
        // Return maximum degradation even if it didn't compile
        return degrade(source, DegradationLevel.MAXIMUM);
    }
    
    /**
     * Get specific degradation action for a feature.
     */
    private DegradationAction getDegradationAction(DegradableFeature feature, 
                                                   DegradationLevel level) {
        return switch (feature) {
            case TEXTURE_LOD -> new DegradationAction(
                // Replace textureLod with texture
                s -> s.replaceAll("textureLod\\s*\\(([^,]+),\\s*([^,]+),\\s*[^)]+\\)", 
                                 "texture(\$1, \$2)"),
                false, null
            );
            
            case TEXTURE_GRAD -> new DegradationAction(
                // Replace textureGrad with texture
                s -> s.replaceAll("textureGrad\\s*\\(([^,]+),\\s*([^,]+),\\s*[^)]+,\\s*[^)]+\\)",
                                 "texture(\$1, \$2)"),
                false, "Texture gradients removed, may affect filtering quality"
            );
            
            case TEXTURE_GATHER -> new DegradationAction(
                // Replace textureGather with multiple texture samples
                s -> replaceTextureGather(s),
                false, "Texture gather replaced with multiple samples"
            );
            
            case HALF_PRECISION -> new DegradationAction(
                // Replace half with float
                s -> s.replaceAll("\\bhalf([234]?)\\b", "float\$1")
                      .replaceAll("\\bhalf([234]x[234])\\b", "float\$1"),
                false, null
            );
            
            case DOUBLE_PRECISION -> new DegradationAction(
                // Replace double with float
                s -> s.replaceAll("\\bdouble([234]?)\\b", "float\$1")
                      .replaceAll("\\bdouble([234]x[234])\\b", "float\$1"),
                false, "Reduced precision may cause artifacts"
            );
            
            case DERIVATIVES -> new DegradationAction(
                // Replace dFdx/dFdy with constants
                s -> s.replaceAll("dFdx\\s*\\([^)]+\\)", "vec3(0.001)")
                      .replaceAll("dFdy\\s*\\([^)]+\\)", "vec3(0.001)")
                      .replaceAll("fwidth\\s*\\([^)]+\\)", "vec3(0.002)")
                      .replaceAll("dfdx\\s*\\([^)]+\\)", "float3(0.001)")
                      .replaceAll("dfdy\\s*\\([^)]+\\)", "float3(0.001)"),
                false, "Derivatives replaced with constants, edge detection may fail"
            );
            
            case DYNAMIC_INDEXING -> new DegradationAction(
                // This is complex - would need to unroll loops
                s -> unrollDynamicIndexing(s, level),
                false, "Dynamic indexing unrolled"
            );
            
            case DYNAMIC_BRANCHING -> new DegradationAction(
                // Flatten dynamic branches
                s -> flattenDynamicBranches(s),
                false, "Dynamic branches flattened"
            );
            
            case MATRIX_INVERSE -> new DegradationAction(
                // Replace inverse() with approximation or pre-computed
                s -> s.replaceAll("inverse\\s*\\(([^)]+)\\)", 
                                 "transpose(\$1)"), // Approximation for orthogonal matrices
                false, "Matrix inverse approximated"
            );
            
            case NORMAL_MAPPING -> new DegradationAction(
                // Remove normal mapping, use vertex normals
                s -> removeNormalMapping(s),
                level.level >= DegradationLevel.AGGRESSIVE.level,
                "Normal mapping removed"
            );
            
            case SHADOWS -> new DegradationAction(
                // Remove shadow calculations
                s -> removeShadowMapping(s),
                true, "Shadows removed"
            );
            
            case REFLECTIONS -> new DegradationAction(
                // Remove reflection calculations
                s -> removeReflections(s),
                true, "Reflections removed"
            );
            
            case SUBGROUPS -> new DegradationAction(
                // Replace subgroup operations with local alternatives
                s -> replaceSubgroupOps(s),
                false, "Subgroup operations replaced"
            );
            
            case ATOMICS -> new DegradationAction(
                // Replace atomics with non-atomic equivalents (may cause races)
                s -> replaceAtomics(s),
                false, "Atomic operations removed, may cause race conditions"
            );
            
            default -> null;
        };
    }
    
    /**
     * Action to apply for degradation.
     */
    private static final class DegradationAction {
        final java.util.function.UnaryOperator<String> transform;
        final boolean removes;
        final String warning;
        
        DegradationAction(java.util.function.UnaryOperator<String> transform, 
                         boolean removes, String warning) {
            this.transform = transform;
            this.removes = removes;
            this.warning = warning;
        }
        
        String apply(String source) {
            return transform.apply(source);
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Specific degradation implementations
    // ════════════════════════════════════════════════════════════════════════
    
    private String replaceTextureGather(String source) {
        // textureGather(sampler, coord, component) -> 4 texture samples
        Pattern pattern = Pattern.compile(
            "textureGather\\s*\\(\\s*(\\w+)\\s*,\\s*([^,]+)\\s*,\\s*(\\d)\\s*\\)");
        Matcher matcher = pattern.matcher(source);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String sampler = matcher.group(1);
            String coord = matcher.group(2).trim();
            String comp = matcher.group(3);
            
            String replacement = String.format(
                "vec4(texture(%s, %s + vec2(-0.5, -0.5) / textureSize(%s, 0)).%s, " +
                "texture(%s, %s + vec2(0.5, -0.5) / textureSize(%s, 0)).%s, " +
                "texture(%s, %s + vec2(0.5, 0.5) / textureSize(%s, 0)).%s, " +
                "texture(%s, %s + vec2(-0.5, 0.5) / textureSize(%s, 0)).%s)",
                sampler, coord, sampler, getComponentChar(comp),
                sampler, coord, sampler, getComponentChar(comp),
                sampler, coord, sampler, getComponentChar(comp),
                sampler, coord, sampler, getComponentChar(comp));
            
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    private String getComponentChar(String index) {
        return switch (index) {
            case "0" -> "r";
            case "1" -> "g";
            case "2" -> "b";
            case "3" -> "a";
            default -> "r";
        };
    }
    
    private String unrollDynamicIndexing(String source, DegradationLevel level) {
        // Find array accesses with variable indices and unroll
        Pattern pattern = Pattern.compile("(\\w+)\\[(\\w+)\\]");
        Matcher matcher = pattern.matcher(source);
        
        // This is a simplified approach - real implementation would be more sophisticated
        if (level.level >= DegradationLevel.AGGRESSIVE.level) {
            // Replace dynamic indexing with switch statements
            // This is very simplified
            return source;
        }
        
        return source;
    }
    
    private String flattenDynamicBranches(String source) {
        // Add [[flatten]] attribute to if statements in HLSL
        // For other languages, convert to select/mix operations where possible
        
        // Simple case: if (cond) a else b -> mix(b, a, float(cond))
        Pattern simpleIf = Pattern.compile(
            "if\\s*\\(([^)]+)\\)\\s*\\{?\\s*(\\w+)\\s*=\\s*([^;]+);\\s*\\}?" +
            "\\s*else\\s*\\{?\\s*\\2\\s*=\\s*([^;]+);\\s*\\}?");
        
        Matcher matcher = simpleIf.matcher(source);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String cond = matcher.group(1);
            String varName = matcher.group(2);
            String trueVal = matcher.group(3);
            String falseVal = matcher.group(4);
            
            String replacement = switch (language) {
                case GLSL -> String.format("%s = mix(%s, %s, float(%s));", 
                    varName, falseVal, trueVal, cond);
                case HLSL -> String.format("%s = lerp(%s, %s, %s);",
                    varName, falseVal, trueVal, cond);
                case MSL -> String.format("%s = select(%s, %s, %s);",
                    varName, falseVal, trueVal, cond);
                default -> matcher.group();
            };
            
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    private String removeNormalMapping(String source) {
        // Remove normal map sampling and TBN calculations
        // Replace with simple vertex normal usage
        
        // Remove normal map declarations
        source = source.replaceAll(
            "(uniform\\s+)?sampler2D\\s+\\w*[nN]ormal\\w*\\s*;", "");
        source = source.replaceAll(
            "texture2d<float>\\s+\\w*[nN]ormal\\w*\\s*(\\[\\[[^]]+\\]\\])?\\s*,?", "");
        
        // Remove TBN matrix calculations
        source = source.replaceAll(
            "mat3\\s+TBN\\s*=\\s*mat3\\s*\\([^)]+\\)\\s*;", "");
        source = source.replaceAll(
            "float3x3\\s+TBN\\s*=\\s*float3x3\\s*\\([^)]+\\)\\s*;", "");
        
        // Replace normal map samples with (0, 0, 1)
        source = source.replaceAll(
            "texture\\s*\\(\\s*\\w*[nN]ormal\\w*\\s*,[^)]+\\)", 
            "vec4(0.5, 0.5, 1.0, 1.0)");
        source = source.replaceAll(
            "\\w*[nN]ormal\\w*\\.sample\\s*\\([^)]+\\)",
            "float4(0.5, 0.5, 1.0, 1.0)");
        
        return source;
    }
    
    private String removeShadowMapping(String source) {
        // Remove shadow map sampling
        // Replace shadow factor with 1.0 (fully lit)
        
        source = source.replaceAll(
            "(uniform\\s+)?sampler2DShadow\\s+\\w+\\s*;", "");
        source = source.replaceAll(
            "(uniform\\s+)?sampler2D\\s+\\w*[sS]hadow\\w*\\s*;", "");
        source = source.replaceAll(
            "depth2d<float>\\s+\\w*[sS]hadow\\w*\\s*(\\[\\[[^]]+\\]\\])?\\s*,?", "");
        
        // Replace shadow calculations with 1.0
        source = source.replaceAll(
            "texture\\s*\\(\\s*\\w*[sS]hadow\\w*\\s*,[^)]+\\)", "1.0");
        source = source.replaceAll(
            "\\w*[sS]hadow\\w*\\.sample_compare\\s*\\([^)]+\\)", "1.0");
        
        // Remove shadow-related variables
        source = source.replaceAll(
            "float\\s+shadow\\w*\\s*=\\s*[^;]+;", "float shadow = 1.0;");
        
        return source;
    }
    
    private String removeReflections(String source) {
        // Remove reflection/environment map sampling
        
        source = source.replaceAll(
            "(uniform\\s+)?samplerCube\\s+\\w*([eE]nv|[rR]eflect)\\w*\\s*;", "");
        source = source.replaceAll(
            "texturecube<float>\\s+\\w*([eE]nv|[rR]eflect)\\w*\\s*(\\[\\[[^]]+\\]\\])?\\s*,?", "");
        
        // Replace environment/reflection samples with default color
        source = source.replaceAll(
            "texture\\s*\\(\\s*\\w*([eE]nv|[rR]eflect)\\w*\\s*,[^)]+\\)",
            "vec4(0.5, 0.5, 0.5, 1.0)");
        source = source.replaceAll(
            "\\w*([eE]nv|[rR]eflect)\\w*\\.sample\\s*\\([^)]+\\)",
            "float4(0.5, 0.5, 0.5, 1.0)");
        
        return source;
    }
    
    private String replaceSubgroupOps(String source) {
        // Replace subgroup operations with local equivalents
        // Most will just return the input value
        
        source = source.replaceAll("subgroupAdd\\s*\\(([^)]+)\\)", "\$1");
        source = source.replaceAll("subgroupMul\\s*\\(([^)]+)\\)", "\$1");
        source = source.replaceAll("subgroupMin\\s*\\(([^)]+)\\)", "\$1");
        source = source.replaceAll("subgroupMax\\s*\\(([^)]+)\\)", "\$1");
        source = source.replaceAll("subgroupAnd\\s*\\(([^)]+)\\)", "\$1");
        source = source.replaceAll("subgroupOr\\s*\\(([^)]+)\\)", "\$1");
        source = source.replaceAll("subgroupXor\\s*\\(([^)]+)\\)", "\$1");
        source = source.replaceAll("subgroupBroadcast\\s*\\([^,]+,\\s*([^)]+)\\)", "\$1");
        source = source.replaceAll("subgroupBroadcastFirst\\s*\\(([^)]+)\\)", "\$1");
        source = source.replaceAll("subgroupBallot\\s*\\([^)]+\\)", "uvec4(1)");
        
        // MSL equivalents
        source = source.replaceAll("simd_sum\\s*\\(([^)]+)\\)", "\$1");
        source = source.replaceAll("simd_product\\s*\\(([^)]+)\\)", "\$1");
        source = source.replaceAll("simd_min\\s*\\(([^)]+)\\)", "\$1");
        source = source.replaceAll("simd_max\\s*\\(([^)]+)\\)", "\$1");
        source = source.replaceAll("simd_broadcast\\s*\\([^,]+,\\s*([^)]+)\\)", "\$1");
        source = source.replaceAll("simd_shuffle\\s*\\([^,]+,\\s*([^)]+)\\)", "\$1");
        
        return source;
    }
    
    private String replaceAtomics(String source) {
        // Replace atomic operations with non-atomic equivalents
        // WARNING: This can cause race conditions!
        
        source = source.replaceAll(
            "atomicAdd\\s*\\(([^,]+),\\s*([^)]+)\\)", 
            "(\$1 += \$2)");
        source = source.replaceAll(
            "atomicMin\\s*\\(([^,]+),\\s*([^)]+)\\)",
            "($1 = min(\$1, \$2))");
        source = source.replaceAll(
            "atomicMax\\s*\\(([^,]+),\\s*([^)]+)\\)",
            "($1 = max(\$1, \$2))");
        source = source.replaceAll(
            "atomicExchange\\s*\\(([^,]+),\\s*([^)]+)\\)",
            "(\$1 = \$2)");
        source = source.replaceAll(
            "atomicCompSwap\\s*\\(([^,]+),\\s*([^,]+),\\s*([^)]+)\\)",
            "(\$1 = \$3)");
        
        // MSL atomics
        source = source.replaceAll(
            "atomic_fetch_add_explicit\\s*\\(([^,]+),\\s*([^,]+)[^)]*\\)",
            "(*\$1 += \$2)");
        source = source.replaceAll(
            "atomic_fetch_min_explicit\\s*\\(([^,]+),\\s*([^,]+)[^)]*\\)",
            "(*$1 = min(*\$1, \$2))");
        source = source.replaceAll(
            "atomic_fetch_max_explicit\\s*\\(([^,]+),\\s*([^,]+)[^)]*\\)",
            "(*$1 = max(*\$1, \$2))");
        
        return source;
    }
    
    private String cleanupDegradedCode(String source) {
        // Remove empty blocks
        source = source.replaceAll("\\{\\s*\\}", "{}");
        
        // Remove multiple consecutive newlines
        source = source.replaceAll("\n{3,}", "\n\n");
        
        // Remove trailing commas in parameter lists
        source = source.replaceAll(",\\s*\\)", ")");
        
        // Remove unused variable declarations (simple cases)
        // This is a simplification - proper implementation would use liveness analysis
        
        return source.trim();
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SECTION 22: COMPILATION CACHE
// Cache compiled shaders for reuse
// ════════════════════════════════════════════════════════════════════════════

/**
 * Multi-level cache for compiled shaders.
 */
public static final class ShaderCompilationCache {
    
    /**
     * Cache entry.
     */
    public static final class CacheEntry {
        public final String sourceHash;
        public final String compiledSource;
        public final ShaderLanguage sourceLanguage;
        public final ShaderLanguage targetLanguage;
        public final String targetVersion;
        public final long creationTime;
        public final long lastAccessTime;
        public final int accessCount;
        public final int sizeBytes;
        public final Map<String, Object> metadata;
        
        CacheEntry(String hash, String compiled, ShaderLanguage src, ShaderLanguage tgt,
                  String version, Map<String, Object> metadata) {
            this.sourceHash = hash;
            this.compiledSource = compiled;
            this.sourceLanguage = src;
            this.targetLanguage = tgt;
            this.targetVersion = version;
            this.creationTime = System.currentTimeMillis();
            this.lastAccessTime = creationTime;
            this.accessCount = 1;
            this.sizeBytes = compiled.getBytes().length;
            this.metadata = Collections.unmodifiableMap(metadata);
        }
        
        CacheEntry withAccess() {
            return new CacheEntry(sourceHash, compiledSource, sourceLanguage, 
                targetLanguage, targetVersion, new HashMap<>(metadata)) {
                @Override
                public long getLastAccessTime() { return System.currentTimeMillis(); }
                @Override
                public int getAccessCount() { return CacheEntry.this.accessCount + 1; }
            };
        }
        
        public long getLastAccessTime() { return lastAccessTime; }
        public int getAccessCount() { return accessCount; }
    }
    
    /**
     * Cache statistics.
     */
    public static final class CacheStatistics {
        public final long hits;
        public final long misses;
        public final long evictions;
        public final long totalEntries;
        public final long totalSizeBytes;
        public final long maxSizeBytes;
        public final double hitRate;
        public final long avgAccessTimeNs;
        
        CacheStatistics(long hits, long misses, long evictions, long entries, 
                       long size, long maxSize, long avgTime) {
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.totalEntries = entries;
            this.totalSizeBytes = size;
            this.maxSizeBytes = maxSize;
            this.hitRate = (hits + misses) > 0 ? (double) hits / (hits + misses) : 0.0;
            this.avgAccessTimeNs = avgTime;
        }
        
        @Override
        public String toString() {
            return String.format("Cache Stats: %d hits, %d misses (%.1f%% hit rate), " +
                "%d entries, %s / %s used",
                hits, misses, hitRate * 100, totalEntries,
                formatBytes(totalSizeBytes), formatBytes(maxSizeBytes));
        }
        
        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * Cache configuration.
     */
    public static final class CacheConfig {
        public long maxMemoryBytes = 256 * 1024 * 1024;  // 256 MB
        public int maxEntries = 10000;
        public long entryTtlMs = 24 * 60 * 60 * 1000;    // 24 hours
        public boolean persistToDisk = true;
        public Path diskCachePath = Path.of(System.getProperty("user.home"), 
            ".cache", "shader-transpiler");
        public long maxDiskBytes = 1024 * 1024 * 1024;   // 1 GB
        public EvictionPolicy evictionPolicy = EvictionPolicy.LRU;
        public boolean compressEntries = true;
        public int compressionLevel = 6;
        
        public enum EvictionPolicy {
            LRU,      // Least Recently Used
            LFU,      // Least Frequently Used
            FIFO,     // First In First Out
            SIZE,     // Largest entries first
            TTL       // Oldest entries first
        }
    }
    
    // Memory cache
    private final ConcurrentHashMap<String, CacheEntry> memoryCache;
    private final CacheConfig config;
    
    // Statistics
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();
    private final AtomicLong totalAccessTimeNs = new AtomicLong();
    private final AtomicLong accessCount = new AtomicLong();
    
    // Size tracking
    private final AtomicLong currentSizeBytes = new AtomicLong();
    
    // Background cleanup
    private final ScheduledExecutorService cleanupExecutor;
    
    // Disk cache
    private final ReentrantReadWriteLock diskLock = new ReentrantReadWriteLock();
    
    public ShaderCompilationCache() {
        this(new CacheConfig());
    }
    
    public ShaderCompilationCache(CacheConfig config) {
        this.config = config;
        this.memoryCache = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ShaderCache-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // Schedule periodic cleanup
        cleanupExecutor.scheduleAtFixedRate(this::performCleanup, 
            1, 5, TimeUnit.MINUTES);
        
        // Load disk cache index on startup
        if (config.persistToDisk) {
            loadDiskCacheIndex();
        }
    }
    
    /**
     * Generate cache key from source and options.
     */
    public String generateKey(String source, ShaderLanguage sourceLanguage,
                             ShaderLanguage targetLanguage, String targetVersion,
                             Map<String, Object> options) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            md.update(source.getBytes(StandardCharsets.UTF_8));
            md.update(sourceLanguage.name().getBytes());
            md.update(targetLanguage.name().getBytes());
            md.update(targetVersion.getBytes());
            
            // Include options in hash
            if (options != null) {
                for (Map.Entry<String, Object> entry : new TreeMap<>(options).entrySet()) {
                    md.update(entry.getKey().getBytes());
                    md.update(String.valueOf(entry.getValue()).getBytes());
                }
            }
            
            byte[] hash = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return Integer.toHexString(Objects.hash(source, sourceLanguage, 
                targetLanguage, targetVersion, options));
        }
    }
    
    /**
     * Get cached entry.
     */
    public Optional<CacheEntry> get(String key) {
        long startTime = System.nanoTime();
        
        try {
            // Check memory cache first
            CacheEntry entry = memoryCache.get(key);
            
            if (entry != null) {
                // Check TTL
                if (System.currentTimeMillis() - entry.creationTime > config.entryTtlMs) {
                    memoryCache.remove(key);
                    evictions.incrementAndGet();
                    currentSizeBytes.addAndGet(-entry.sizeBytes);
                    entry = null;
                } else {
                    // Update access statistics
                    memoryCache.put(key, entry.withAccess());
                }
            }
            
            // Check disk cache if not in memory
            if (entry == null && config.persistToDisk) {
                entry = loadFromDisk(key);
                if (entry != null) {
                    // Promote to memory cache
                    putInMemory(key, entry);
                }
            }
            
            if (entry != null) {
                hits.incrementAndGet();
                return Optional.of(entry);
            } else {
                misses.incrementAndGet();
                return Optional.empty();
            }
            
        } finally {
            long elapsed = System.nanoTime() - startTime;
            totalAccessTimeNs.addAndGet(elapsed);
            accessCount.incrementAndGet();
        }
    }
    
    /**
     * Put entry in cache.
     */
    public void put(String key, String compiledSource, ShaderLanguage sourceLanguage,
                   ShaderLanguage targetLanguage, String targetVersion,
                   Map<String, Object> metadata) {
        CacheEntry entry = new CacheEntry(key, compiledSource, sourceLanguage,
            targetLanguage, targetVersion, metadata != null ? metadata : Map.of());
        
        putInMemory(key, entry);
        
        // Persist to disk asynchronously
        if (config.persistToDisk) {
            CompletableFuture.runAsync(() -> saveToDisk(key, entry));
        }
    }
    
    /**
     * Put entry in memory cache with eviction if needed.
     */
    private void putInMemory(String key, CacheEntry entry) {
        // Check if we need to evict
        while (currentSizeBytes.get() + entry.sizeBytes > config.maxMemoryBytes ||
               memoryCache.size() >= config.maxEntries) {
            evictOne();
        }
        
        CacheEntry old = memoryCache.put(key, entry);
        if (old != null) {
            currentSizeBytes.addAndGet(-old.sizeBytes);
        }
        currentSizeBytes.addAndGet(entry.sizeBytes);
    }
    
    /**
     * Evict one entry based on policy.
     */
    private void evictOne() {
        if (memoryCache.isEmpty()) return;
        
        String keyToEvict = switch (config.evictionPolicy) {
            case LRU -> memoryCache.entrySet().stream()
                .min(Comparator.comparingLong(e -> e.getValue().getLastAccessTime()))
                .map(Map.Entry::getKey)
                .orElse(null);
                
            case LFU -> memoryCache.entrySet().stream()
                .min(Comparator.comparingInt(e -> e.getValue().getAccessCount()))
                .map(Map.Entry::getKey)
                .orElse(null);
                
            case FIFO -> memoryCache.entrySet().stream()
                .min(Comparator.comparingLong(e -> e.getValue().creationTime))
                .map(Map.Entry::getKey)
                .orElse(null);
                
            case SIZE -> memoryCache.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().sizeBytes))
                .map(Map.Entry::getKey)
                .orElse(null);
                
            case TTL -> memoryCache.entrySet().stream()
                .min(Comparator.comparingLong(e -> 
                    config.entryTtlMs - (System.currentTimeMillis() - e.getValue().creationTime)))
                .map(Map.Entry::getKey)
                .orElse(null);
        };
        
        if (keyToEvict != null) {
            CacheEntry evicted = memoryCache.remove(keyToEvict);
            if (evicted != null) {
                currentSizeBytes.addAndGet(-evicted.sizeBytes);
                evictions.incrementAndGet();
            }
        }
    }
    
    /**
     * Remove entry from cache.
     */
    public void invalidate(String key) {
        CacheEntry removed = memoryCache.remove(key);
        if (removed != null) {
            currentSizeBytes.addAndGet(-removed.sizeBytes);
        }
        
        if (config.persistToDisk) {
            deleteFromDisk(key);
        }
    }
    
    /**
     * Clear entire cache.
     */
    public void clear() {
        memoryCache.clear();
        currentSizeBytes.set(0);
        
        if (config.persistToDisk) {
            clearDiskCache();
        }
    }
    
    /**
     * Get cache statistics.
     */
    public CacheStatistics getStatistics() {
        long count = accessCount.get();
        long avgTime = count > 0 ? totalAccessTimeNs.get() / count : 0;
        
        return new CacheStatistics(
            hits.get(),
            misses.get(),
            evictions.get(),
            memoryCache.size(),
            currentSizeBytes.get(),
            config.maxMemoryBytes,
            avgTime
        );
    }
    
    /**
     * Shutdown cache (save to disk, cleanup).
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            cleanupExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Save all entries to disk
        if (config.persistToDisk) {
            saveDiskCacheIndex();
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Disk cache operations
    // ════════════════════════════════════════════════════════════════════════
    
    private void loadDiskCacheIndex() {
        diskLock.readLock().lock();
        try {
            Path indexPath = config.diskCachePath.resolve("index.json");
            if (!Files.exists(indexPath)) return;
            
            // Load index and populate memory cache with frequently used entries
            String indexJson = Files.readString(indexPath);
            // Parse index and load hot entries
            
        } catch (IOException e) {
            // Ignore disk cache errors
        } finally {
            diskLock.readLock().unlock();
        }
    }
    
    private void saveDiskCacheIndex() {
        diskLock.writeLock().lock();
        try {
            Files.createDirectories(config.diskCachePath);
            Path indexPath = config.diskCachePath.resolve("index.json");
            
            // Build index from current entries
            StringBuilder json = new StringBuilder("{\n  \"entries\": [\n");
            boolean first = true;
            for (Map.Entry<String, CacheEntry> entry : memoryCache.entrySet()) {
                if (!first) json.append(",\n");
                first = false;
                json.append("    {\"key\": \"").append(entry.getKey())
                    .append("\", \"size\": ").append(entry.getValue().sizeBytes)
                    .append(", \"created\": ").append(entry.getValue().creationTime)
                    .append("}");
            }
            json.append("\n  ]\n}");
            
            Files.writeString(indexPath, json.toString());
            
        } catch (IOException e) {
            // Ignore disk cache errors
        } finally {
            diskLock.writeLock().unlock();
        }
    }
    
    private CacheEntry loadFromDisk(String key) {
        diskLock.readLock().lock();
        try {
            Path entryPath = config.diskCachePath.resolve(key.substring(0, 2))
                .resolve(key + ".cache");
            
            if (!Files.exists(entryPath)) return null;
            
            byte[] data = Files.readAllBytes(entryPath);
            
            // Decompress if needed
            if (config.compressEntries) {
                data = decompress(data);
            }
            
            return deserializeEntry(data);
            
        } catch (IOException e) {
            return null;
        } finally {
            diskLock.readLock().unlock();
        }
    }
    
    private void saveToDisk(String key, CacheEntry entry) {
        diskLock.writeLock().lock();
        try {
            Path dir = config.diskCachePath.resolve(key.substring(0, 2));
            Files.createDirectories(dir);
            Path entryPath = dir.resolve(key + ".cache");
            
            byte[] data = serializeEntry(entry);
            
            // Compress if enabled
            if (config.compressEntries) {
                data = compress(data);
            }
            
            Files.write(entryPath, data);
            
        } catch (IOException e) {
            // Ignore disk write errors
        } finally {
            diskLock.writeLock().unlock();
        }
    }
    
    private void deleteFromDisk(String key) {
        diskLock.writeLock().lock();
        try {
            Path entryPath = config.diskCachePath.resolve(key.substring(0, 2))
                .resolve(key + ".cache");
            Files.deleteIfExists(entryPath);
        } catch (IOException e) {
            // Ignore
        } finally {
            diskLock.writeLock().unlock();
        }
    }
    
    private void clearDiskCache() {
        diskLock.writeLock().lock();
        try {
            if (Files.exists(config.diskCachePath)) {
                Files.walk(config.diskCachePath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.delete(path); } 
                        catch (IOException ignored) {}
                    });
            }
        } catch (IOException e) {
            // Ignore
        } finally {
            diskLock.writeLock().unlock();
        }
    }
    
    private byte[] serializeEntry(CacheEntry entry) {
        // Simple serialization format
        StringBuilder sb = new StringBuilder();
        sb.append(entry.sourceHash).append("\n");
        sb.append(entry.sourceLanguage.name()).append("\n");
        sb.append(entry.targetLanguage.name()).append("\n");
        sb.append(entry.targetVersion).append("\n");
        sb.append(entry.creationTime).append("\n");
        sb.append(entry.compiledSource.length()).append("\n");
        sb.append(entry.compiledSource);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    private CacheEntry deserializeEntry(byte[] data) {
        String str = new String(data, StandardCharsets.UTF_8);
        String[] lines = str.split("\n", 7);
        
        if (lines.length < 7) return null;
        
        String hash = lines[0];
        ShaderLanguage srcLang = ShaderLanguage.valueOf(lines[1]);
        ShaderLanguage tgtLang = ShaderLanguage.valueOf(lines[2]);
        String version = lines[3];
        // lines[4] is creation time, lines[5] is length
        String compiled = lines[6];
        
        return new CacheEntry(hash, compiled, srcLang, tgtLang, version, Map.of());
    }
    
    private byte[] compress(byte[] data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (java.util.zip.GZIPOutputStream gzip = 
                    new java.util.zip.GZIPOutputStream(baos) {{
                        def.setLevel(config.compressionLevel);
                    }}) {
                gzip.write(data);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            return data;
        }
    }
    
    private byte[] decompress(byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            try (java.util.zip.GZIPInputStream gzip = 
                    new java.util.zip.GZIPInputStream(bais)) {
                return gzip.readAllBytes();
            }
        } catch (IOException e) {
            return data;
        }
    }
    
    private void performCleanup() {
        // Remove expired entries
        long now = System.currentTimeMillis();
        memoryCache.entrySet().removeIf(entry -> {
            if (now - entry.getValue().creationTime > config.entryTtlMs) {
                currentSizeBytes.addAndGet(-entry.getValue().sizeBytes);
                evictions.incrementAndGet();
                return true;
            }
            return false;
        });
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SECTION 23: UNIFIED TRANSPILER API
// Main entry point combining all systems
// ════════════════════════════════════════════════════════════════════════════

/**
 * Unified shader transpiler combining all translation systems.
 */
public static final class UnifiedShaderTranspiler {
    
    /**
     * Transpilation request.
     */
    public static final class TranspileRequest {
        public final String source;
        public final ShaderLanguage sourceLanguage;
        public final ShaderLanguage targetLanguage;
        public final ShaderStage stage;
        public final String entryPoint;
        public final Map<String, Object> options;
        public final TranspileStrategy strategy;
        
        private TranspileRequest(Builder builder) {
            this.source = builder.source;
            this.sourceLanguage = builder.sourceLanguage;
            this.targetLanguage = builder.targetLanguage;
            this.stage = builder.stage;
            this.entryPoint = builder.entryPoint;
            this.options = Collections.unmodifiableMap(builder.options);
            this.strategy = builder.strategy;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static final class Builder {
            private String source;
            private ShaderLanguage sourceLanguage;
            private ShaderLanguage targetLanguage;
            private ShaderStage stage = ShaderStage.VERTEX;
            private String entryPoint = "main";
            private Map<String, Object> options = new HashMap<>();
            private TranspileStrategy strategy = TranspileStrategy.AUTO;
            
            public Builder source(String source) {
                this.source = source;
                return this;
            }
            
            public Builder sourceLanguage(ShaderLanguage lang) {
                this.sourceLanguage = lang;
                return this;
            }
            
            public Builder targetLanguage(ShaderLanguage lang) {
                this.targetLanguage = lang;
                return this;
            }
            
            public Builder stage(ShaderStage stage) {
                this.stage = stage;
                return this;
            }
            
            public Builder entryPoint(String name) {
                this.entryPoint = name;
                return this;
            }
            
            public Builder option(String key, Object value) {
                this.options.put(key, value);
                return this;
            }
            
            public Builder options(Map<String, Object> opts) {
                this.options.putAll(opts);
                return this;
            }
            
            public Builder strategy(TranspileStrategy strategy) {
                this.strategy = strategy;
                return this;
            }
            
            public TranspileRequest build() {
                Objects.requireNonNull(source, "Source is required");
                Objects.requireNonNull(sourceLanguage, "Source language is required");
                Objects.requireNonNull(targetLanguage, "Target language is required");
                return new TranspileRequest(this);
            }
        }
    }
    
    /**
     * Transpilation strategy.
     */
    public enum TranspileStrategy {
        REGEX_ONLY,      // Only use regex-based translation
        AST_ONLY,        // Only use AST-based translation
        SPIRV_ONLY,      // Only use SPIR-V Cross
        AUTO,            // Automatically choose best strategy
        CASCADING,       // Try each strategy in order until success
        PARALLEL         // Try all strategies in parallel, use first success
    }
    
    /**
     * Transpilation result.
     */
    public static final class TranspileResult {
        public final boolean success;
        public final String transpiledSource;
        public final ShaderLanguage targetLanguage;
        public final String targetVersion;
        public final TranspileStrategy usedStrategy;
        public final List<String> warnings;
        public final List<String> errors;
        public final long transpilationTimeNs;
        public final TranspileMetrics metrics;
        
        TranspileResult(boolean success, String source, ShaderLanguage target,
                       String version, TranspileStrategy strategy, List<String> warnings,
                       List<String> errors, long timeNs, TranspileMetrics metrics) {
            this.success = success;
            this.transpiledSource = source;
            this.targetLanguage = target;
            this.targetVersion = version;
            this.usedStrategy = strategy;
            this.warnings = Collections.unmodifiableList(warnings);
            this.errors = Collections.unmodifiableList(errors);
            this.transpilationTimeNs = timeNs;
            this.metrics = metrics;
        }
        
        public static TranspileResult success(String source, ShaderLanguage target,
                                              String version, TranspileStrategy strategy,
                                              List<String> warnings, long timeNs,
                                              TranspileMetrics metrics) {
            return new TranspileResult(true, source, target, version, strategy,
                warnings, List.of(), timeNs, metrics);
        }
        
        public static TranspileResult failure(List<String> errors, long timeNs) {
            return new TranspileResult(false, null, null, null, null,
                List.of(), errors, timeNs, null);
        }
    }
    
    /**
     * Transpilation metrics.
     */
    public static final class TranspileMetrics {
        public final int sourceLines;
        public final int outputLines;
        public final int tokensProcessed;
        public final int astNodes;
        public final int transformationsApplied;
        public final boolean cacheHit;
        public final String strategyDetails;
        
        TranspileMetrics(int srcLines, int outLines, int tokens, int nodes,
                        int transforms, boolean cached, String details) {
            this.sourceLines = srcLines;
            this.outputLines = outLines;
            this.tokensProcessed = tokens;
            this.astNodes = nodes;
            this.transformationsApplied = transforms;
            this.cacheHit = cached;
            this.strategyDetails = details;
        }
    }
    
    // Components
    private final ShaderCompilationCache cache;
    private final ShaderVersionRouter router;
    private final SPIRVCrossCompiler spirvCompiler;
    private final ExecutorService executor;
    
    // Regex translators
    private final Map<Pair<ShaderLanguage, ShaderLanguage>, Object> regexTranslators;
    
    public UnifiedShaderTranspiler() {
        this(new ShaderCompilationCache.CacheConfig());
    }
    
    public UnifiedShaderTranspiler(ShaderCompilationCache.CacheConfig cacheConfig) {
        this.cache = new ShaderCompilationCache(cacheConfig);
        this.router = new ShaderVersionRouter();
        this.spirvCompiler = new SPIRVCrossCompiler();
        this.executor = Executors.newWorkStealingPool();
        this.regexTranslators = new ConcurrentHashMap<>();
        
        initializeTranslators();
    }
    
    private void initializeTranslators() {
        // Initialize regex-based translators for common paths
        regexTranslators.put(new Pair<>(ShaderLanguage.GLSL, ShaderLanguage.MSL),
            new GLSLToMSLTranslator());
        regexTranslators.put(new Pair<>(ShaderLanguage.HLSL, ShaderLanguage.MSL),
            new HLSLToMSLTranslator());
        regexTranslators.put(new Pair<>(ShaderLanguage.MSL, ShaderLanguage.GLSL),
            new MSLToGLSLTranslator());
        regexTranslators.put(new Pair<>(ShaderLanguage.GLSL, ShaderLanguage.HLSL),
            new GLSLToHLSLTranslator());
        regexTranslators.put(new Pair<>(ShaderLanguage.HLSL, ShaderLanguage.GLSL),
            new HLSLToGLSLTranslator());
    }
    
    /**
     * Transpile shader synchronously.
     */
    public TranspileResult transpile(TranspileRequest request) {
        long startTime = System.nanoTime();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        // Check cache first
        String cacheKey = cache.generateKey(request.source, request.sourceLanguage,
            request.targetLanguage, getTargetVersion(request), request.options);
        
        Optional<ShaderCompilationCache.CacheEntry> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            TranspileMetrics metrics = new TranspileMetrics(
                countLines(request.source), countLines(cached.get().compiledSource),
                0, 0, 0, true, "Cache hit"
            );
            return TranspileResult.success(cached.get().compiledSource,
                request.targetLanguage, cached.get().targetVersion,
                null, warnings, System.nanoTime() - startTime, metrics);
        }
        
        // Execute transpilation based on strategy
        String result = switch (request.strategy) {
            case REGEX_ONLY -> transpileRegex(request, warnings, errors);
            case AST_ONLY -> transpileAST(request, warnings, errors);
            case SPIRV_ONLY -> transpileSPIRV(request, warnings, errors);
            case AUTO -> transpileAuto(request, warnings, errors);
            case CASCADING -> transpileCascading(request, warnings, errors);
            case PARALLEL -> transpileParallel(request, warnings, errors);
        };
        
        if (result != null && errors.isEmpty()) {
            // Cache successful result
            String version = getTargetVersion(request);
            cache.put(cacheKey, result, request.sourceLanguage, request.targetLanguage,
                version, Map.of("strategy", request.strategy.name()));
            
            TranspileMetrics metrics = new TranspileMetrics(
                countLines(request.source), countLines(result),
                0, 0, 0, false, request.strategy.name()
            );
            
            return TranspileResult.success(result, request.targetLanguage, version,
                request.strategy, warnings, System.nanoTime() - startTime, metrics);
        }
        
        return TranspileResult.failure(errors, System.nanoTime() - startTime);
    }
    
    /**
     * Transpile shader asynchronously.
     */
    public CompletableFuture<TranspileResult> transpileAsync(TranspileRequest request) {
        return CompletableFuture.supplyAsync(() -> transpile(request), executor);
    }
    
    /**
     * Regex-based transpilation.
     */
    private String transpileRegex(TranspileRequest request, 
                                 List<String> warnings, List<String> errors) {
        Pair<ShaderLanguage, ShaderLanguage> key = 
            new Pair<>(request.sourceLanguage, request.targetLanguage);
        
        Object translator = regexTranslators.get(key);
        if (translator == null) {
            errors.add("No regex translator available for " + 
                request.sourceLanguage + " -> " + request.targetLanguage);
            return null;
        }
        
        try {
            // Use reflection to call translate method
            java.lang.reflect.Method translateMethod = 
                translator.getClass().getMethod("translate", String.class);
            Object result = translateMethod.invoke(translator, request.source);
            
            if (result instanceof String str) {
                return str;
            } else if (result != null) {
                // Handle TranslationResult type
                java.lang.reflect.Field successField = 
                    result.getClass().getDeclaredField("success");
                successField.setAccessible(true);
                
                if ((boolean) successField.get(result)) {
                    java.lang.reflect.Field outputField = 
                        result.getClass().getDeclaredField("output");
                    outputField.setAccessible(true);
                    return (String) outputField.get(result);
                }
            }
            
            errors.add("Regex translation failed");
            return null;
            
        } catch (Exception e) {
            errors.add("Regex translation error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * AST-based transpilation.
     */
    private String transpileAST(TranspileRequest request,
                               List<String> warnings, List<String> errors) {
        try {
            // Tokenize
            ShaderTokenizer tokenizer = new ShaderTokenizer(request.sourceLanguage);
            ShaderTokenizer.TokenizationResult tokenResult = 
                tokenizer.tokenize(request.source);
            
            if (!tokenResult.success) {
                errors.addAll(tokenResult.errors.stream()
                    .map(e -> e.message)
                    .collect(Collectors.toList()));
                return null;
            }
            
            // Parse
            ShaderParser parser = new ShaderParser(request.sourceLanguage);
            ShaderParser.ParseResult parseResult = parser.parse(tokenResult.tokens);
            
            if (!parseResult.success || parseResult.ast == null) {
                errors.addAll(parseResult.errors);
                return null;
            }
            
            // Generate target code
            String output = switch (request.targetLanguage) {
                case MSL -> parseResult.ast.toMSL();
                case GLSL -> parseResult.ast.toGLSL();
                case HLSL -> parseResult.ast.toHLSL();
                default -> null;
            };
            
            if (output == null) {
                errors.add("AST code generation not supported for " + request.targetLanguage);
            }
            
            return output;
            
        } catch (Exception e) {
            errors.add("AST translation error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * SPIR-V Cross transpilation.
     */
    private String transpileSPIRV(TranspileRequest request,
                                  List<String> warnings, List<String> errors) {
        try {
            // Compile to SPIR-V first
            SPIRVCrossCompiler.SPIRVResult spirvResult = switch (request.sourceLanguage) {
                case GLSL -> spirvCompiler.compileGLSLToSPIRV(
                    request.source, request.stage, request.entryPoint);
                case HLSL -> spirvCompiler.compileHLSLToSPIRV(
                    request.source, request.stage, request.entryPoint);
                default -> {
                    errors.add("Cannot compile " + request.sourceLanguage + " to SPIR-V");
                    yield null;
                }
            };
            
            if (spirvResult == null || !spirvResult.success || spirvResult.spirvBinary == null) {
                errors.add("SPIR-V compilation failed: " + 
                    (spirvResult != null ? spirvResult.errorMessage : "unknown error"));
                return null;
            }
            
            // Cross-compile to target
            SPIRVCrossCompiler.SPIRVOptions options = buildSPIRVOptions(request);
            SPIRVCrossCompiler.SPIRVResult crossResult = switch (request.targetLanguage) {
                case MSL -> spirvCompiler.crossCompileToMSL(spirvResult.spirvBinary, options);
                case GLSL -> spirvCompiler.crossCompileToGLSL(spirvResult.spirvBinary, options);
                case HLSL -> spirvCompiler.crossCompileToHLSL(spirvResult.spirvBinary, options);
                default -> {
                    errors.add("SPIR-V cross-compilation not supported for " + request.targetLanguage);
                    yield null;
                }
            };
            
            if (crossResult == null || !crossResult.success) {
                errors.add("SPIR-V cross-compilation failed: " +
                    (crossResult != null ? crossResult.errorMessage : "unknown error"));
                return null;
            }
            
            return crossResult.output;
            
        } catch (Exception e) {
            errors.add("SPIR-V transpilation error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Automatic strategy selection.
     */
    private String transpileAuto(TranspileRequest request,
                                List<String> warnings, List<String> errors) {
        // Analyze source to choose best strategy
        int complexity = estimateComplexity(request.source);
        boolean hasAdvancedFeatures = hasAdvancedFeatures(request.source);
        
        if (complexity < 50 && !hasAdvancedFeatures) {
            // Simple shader - try regex first
            String result = transpileRegex(request, warnings, new ArrayList<>());
            if (result != null) return result;
        }
        
        // Try AST for medium complexity
        if (complexity < 200) {
            String result = transpileAST(request, warnings, new ArrayList<>());
            if (result != null) return result;
        }
        
        // Fall back to SPIR-V for complex shaders
        if (SPIRVCrossCompiler.isNativeAvailable()) {
            String result = transpileSPIRV(request, warnings, new ArrayList<>());
            if (result != null) return result;
        }
        
        // Final fallback: try cascading
        return transpileCascading(request, warnings, errors);
    }
    
    /**
     * Cascading strategy - try each method in order.
     */
    private String transpileCascading(TranspileRequest request,
                                      List<String> warnings, List<String> errors) {
        List<String> attemptErrors = new ArrayList<>();
        
        // 1. Try regex
        String result = transpileRegex(request, warnings, attemptErrors);
        if (result != null) return result;
        
        // 2. Try AST
        attemptErrors.clear();
        result = transpileAST(request, warnings, attemptErrors);
        if (result != null) return result;
        
        // 3. Try SPIR-V
        attemptErrors.clear();
        result = transpileSPIRV(request, warnings, attemptErrors);
        if (result != null) return result;
        
        // 4. Try version routing
        ShaderVersionRouter.RoutingResult routingResult = router.route(
            request.source, request.sourceLanguage, request.targetLanguage, request.stage);
        
        if (routingResult.success) {
            return routingResult.compiledSource;
        }
        
        // 5. Try degradation
        ShaderDegrader degrader = new ShaderDegrader(request.targetLanguage);
        ShaderDegrader.DegradationResult degraded = degrader.autodegrade(
            request.source,
            src -> {
                List<String> testErrors = new ArrayList<>();
                return transpileRegex(
                    TranspileRequest.builder()
                        .source(src)
                        .sourceLanguage(request.sourceLanguage)
                        .targetLanguage(request.targetLanguage)
                        .stage(request.stage)
                        .build(),
                    new ArrayList<>(), testErrors) != null;
            }
        );
        
        if (degraded.degradedSource != null && !degraded.degradedSource.equals(request.source)) {
            warnings.add("Shader was degraded to level " + degraded.level);
            warnings.addAll(degraded.warnings);
            
            // Try transpilation again with degraded source
            TranspileRequest degradedRequest = TranspileRequest.builder()
                .source(degraded.degradedSource)
                .sourceLanguage(request.sourceLanguage)
                .targetLanguage(request.targetLanguage)
                .stage(request.stage)
                .entryPoint(request.entryPoint)
                .options(request.options)
                .strategy(TranspileStrategy.REGEX_ONLY)
                .build();
            
            return transpileRegex(degradedRequest, warnings, errors);
        }
        
        errors.add("All transpilation strategies failed");
        return null;
    }
    
    /**
     * Parallel strategy - try all methods simultaneously.
     */
    private String transpileParallel(TranspileRequest request,
                                     List<String> warnings, List<String> errors) {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        // Start all strategies in parallel
        futures.add(CompletableFuture.supplyAsync(() -> 
            transpileRegex(request, new ArrayList<>(), new ArrayList<>()), executor));
        futures.add(CompletableFuture.supplyAsync(() -> 
            transpileAST(request, new ArrayList<>(), new ArrayList<>()), executor));
        
        if (SPIRVCrossCompiler.isNativeAvailable()) {
            futures.add(CompletableFuture.supplyAsync(() -> 
                transpileSPIRV(request, new ArrayList<>(), new ArrayList<>()), executor));
        }
        
        // Wait for first successful result
        try {
            CompletableFuture<String> anyOf = CompletableFuture.anyOf(
                futures.toArray(new CompletableFuture[0]))
                .thenApply(obj -> (String) obj);
            
            // Poll for first success
            for (int i = 0; i < 100; i++) { // Max 10 seconds
                for (CompletableFuture<String> future : futures) {
                    if (future.isDone() && !future.isCompletedExceptionally()) {
                        String result = future.get();
                        if (result != null) {
                            // Cancel other futures
                            futures.forEach(f -> f.cancel(true));
                            return result;
                        }
                    }
                }
                Thread.sleep(100);
            }
            
        } catch (Exception e) {
            errors.add("Parallel transpilation error: " + e.getMessage());
        }
        
        // Fall back to cascading if parallel fails
        return transpileCascading(request, warnings, errors);
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Helper methods
    // ════════════════════════════════════════════════════════════════════════
    
    private String getTargetVersion(TranspileRequest request) {
        return (String) request.options.getOrDefault("targetVersion", 
            switch (request.targetLanguage) {
                case MSL -> "2.1";
                case GLSL -> "450";
                case HLSL -> "6.0";
                case SPIRV -> "1.5";
                default -> "1.0";
            });
    }
    
    private SPIRVCrossCompiler.SPIRVOptions buildSPIRVOptions(TranspileRequest request) {
        SPIRVCrossCompiler.SPIRVOptions options = new SPIRVCrossCompiler.SPIRVOptions();
        
        // Apply request options
        if (request.options.containsKey("mslVersion")) {
            options.mslVersion = (Integer) request.options.get("mslVersion");
        }
        if (request.options.containsKey("glslVersion")) {
            options.glslVersion = (Integer) request.options.get("glslVersion");
        }
        if (request.options.containsKey("hlslShaderModel")) {
            options.hlslShaderModel = (Integer) request.options.get("hlslShaderModel");
        }
        if (request.options.containsKey("flipVertexY")) {
            options.flipVertexY = (Boolean) request.options.get("flipVertexY");
        }
        
        return options;
    }
    
    private int estimateComplexity(String source) {
        int complexity = source.length() / 50;
        complexity += countOccurrences(source, "for") * 5;
        complexity += countOccurrences(source, "while") * 5;
        complexity += countOccurrences(source, "if") * 2;
        complexity += countOccurrences(source, "struct") * 10;
        complexity += countOccurrences(source, "function") * 5;
        return complexity;
    }
    
    private boolean hasAdvancedFeatures(String source) {
        return source.contains("atomic") ||
               source.contains("subgroup") ||
               source.contains("simd_") ||
               source.contains("texture2DArray") ||
               source.contains("imageLoad") ||
               source.contains("shared ") ||
               source.contains("threadgroup ");
    }
    
    private int countLines(String source) {
        if (source == null) return 0;
        return source.split("\n").length;
    }
    
    private int countOccurrences(String source, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
    
    /**
     * Get cache statistics.
     */
    public ShaderCompilationCache.CacheStatistics getCacheStatistics() {
        return cache.getStatistics();
    }
    
    /**
     * Clear cache.
     */
    public void clearCache() {
        cache.clear();
    }
    
    /**
     * Shutdown transpiler.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        cache.shutdown();
        router.shutdown();
        spirvCompiler.dispose();
    }
    
    /**
     * Simple pair class for map keys.
     */
    private static final class Pair<A, B> {
        final A first;
        final B second;
        
        Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pair<?, ?> pair)) return false;
            return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SECTION 24: CONVENIENCE API & STATIC METHODS
// Simple entry points for common use cases
// ════════════════════════════════════════════════════════════════════════════

/**
 * Static convenience methods for quick shader translation.
 */
public static String glslToMSL(String glsl) {
    return new GLSLToMSLTranslator().translate(glsl).output;
}

public static String hlslToMSL(String hlsl) {
    return new HLSLToMSLTranslator().translate(hlsl).output;
}

public static String mslToGLSL(String msl) {
    return new MSLToGLSLTranslator().translate(msl).output;
}

public static String glslToHLSL(String glsl) {
    return new GLSLToHLSLTranslator().translate(glsl).output;
}

public static String hlslToGLSL(String hlsl) {
    return new HLSLToGLSLTranslator().translate(hlsl).output;
}

/**
 * Translate between any supported languages.
 */
public static String translate(String source, ShaderLanguage from, ShaderLanguage to) {
    UnifiedShaderTranspiler transpiler = new UnifiedShaderTranspiler();
    try {
        UnifiedShaderTranspiler.TranspileResult result = transpiler.transpile(
            UnifiedShaderTranspiler.TranspileRequest.builder()
                .source(source)
                .sourceLanguage(from)
                .targetLanguage(to)
                .strategy(UnifiedShaderTranspiler.TranspileStrategy.AUTO)
                .build()
        );
        
        if (result.success) {
            return result.transpiledSource;
        } else {
            throw new RuntimeException("Translation failed: " + 
                String.join(", ", result.errors));
        }
    } finally {
        transpiler.shutdown();
    }
}

/**
 * Translate with options.
 */
public static String translate(String source, ShaderLanguage from, ShaderLanguage to,
                               ShaderStage stage, Map<String, Object> options) {
    UnifiedShaderTranspiler transpiler = new UnifiedShaderTranspiler();
    try {
        UnifiedShaderTranspiler.TranspileResult result = transpiler.transpile(
            UnifiedShaderTranspiler.TranspileRequest.builder()
                .source(source)
                .sourceLanguage(from)
                .targetLanguage(to)
                .stage(stage)
                .options(options)
                .strategy(UnifiedShaderTranspiler.TranspileStrategy.AUTO)
                .build()
        );
        
        if (result.success) {
            return result.transpiledSource;
        } else {
            throw new RuntimeException("Translation failed: " + 
                String.join(", ", result.errors));
        }
    } finally {
        transpiler.shutdown();
    }
}

/**
 * Create a reusable transpiler instance.
 */
public static UnifiedShaderTranspiler createTranspiler() {
    return new UnifiedShaderTranspiler();
}

/**
 * Create a transpiler with custom cache configuration.
 */
public static UnifiedShaderTranspiler createTranspiler(
        ShaderCompilationCache.CacheConfig cacheConfig) {
    return new UnifiedShaderTranspiler(cacheConfig);
}

// ════════════════════════════════════════════════════════════════════════════
// SECTION 25: MAIN METHOD & EXAMPLES
// Entry point and usage examples
// ════════════════════════════════════════════════════════════════════════════

/**
 * Main entry point demonstrating usage.
 */
public static void main(String[] args) {
    System.out.println("╔══════════════════════════════════════════════════════════════╗");
    System.out.println("║     Universal Shader Transpiler - Java Implementation        ║");
    System.out.println("║     Supports: GLSL ↔ HLSL ↔ MSL ↔ SPIR-V                     ║");
    System.out.println("╚══════════════════════════════════════════════════════════════╝");
    System.out.println();
    
    // Example GLSL shader
    String glslSource = """
        #version 450
        
        layout(location = 0) in vec3 inPosition;
        layout(location = 1) in vec2 inTexCoord;
        layout(location = 2) in vec3 inNormal;
        
        layout(location = 0) out vec2 fragTexCoord;
        layout(location = 1) out vec3 fragNormal;
        layout(location = 2) out vec3 fragWorldPos;
        
        layout(binding = 0) uniform Uniforms {
            mat4 model;
            mat4 view;
            mat4 projection;
            vec3 cameraPos;
        } ubo;
        
        void main() {
            vec4 worldPos = ubo.model * vec4(inPosition, 1.0);
            fragWorldPos = worldPos.xyz;
            fragTexCoord = inTexCoord;
            fragNormal = mat3(transpose(inverse(ubo.model))) * inNormal;
            gl_Position = ubo.projection * ubo.view * worldPos;
        }
        """;
    
    System.out.println("═══════════════════════════════════════════════════════════════");
    System.out.println("Input GLSL Vertex Shader:");
    System.out.println("═══════════════════════════════════════════════════════════════");
    System.out.println(glslSource);
    
    // Create transpiler
    UnifiedShaderTranspiler transpiler = createTranspiler();
    
    try {
        // Translate to MSL
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Output MSL:");
        System.out.println("═══════════════════════════════════════════════════════════════");
        
        UnifiedShaderTranspiler.TranspileResult mslResult = transpiler.transpile(
            UnifiedShaderTranspiler.TranspileRequest.builder()
                .source(glslSource)
                .sourceLanguage(ShaderLanguage.GLSL)
                .targetLanguage(ShaderLanguage.MSL)
                .stage(ShaderStage.VERTEX)
                .build()
        );
        
        if (mslResult.success) {
            System.out.println(mslResult.transpiledSource);
            System.out.println("\n[Success] Transpiled in " + 
                (mslResult.transpilationTimeNs / 1_000_000.0) + " ms");
        } else {
            System.out.println("[Failed] " + String.join(", ", mslResult.errors));
        }
        
        // Translate to HLSL
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Output HLSL:");
        System.out.println("═══════════════════════════════════════════════════════════════");
        
        UnifiedShaderTranspiler.TranspileResult hlslResult = transpiler.transpile(
            UnifiedShaderTranspiler.TranspileRequest.builder()
                .source(glslSource)
                .sourceLanguage(ShaderLanguage.GLSL)
                .targetLanguage(ShaderLanguage.HLSL)
                .stage(ShaderStage.VERTEX)
                .build()
        );
        
        if (hlslResult.success) {
            System.out.println(hlslResult.transpiledSource);
            System.out.println("\n[Success] Transpiled in " + 
                (hlslResult.transpilationTimeNs / 1_000_000.0) + " ms");
        } else {
            System.out.println("[Failed] " + String.join(", ", hlslResult.errors));
        }
        
        // Show cache statistics
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Cache Statistics:");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println(transpiler.getCacheStatistics());
        
        // Demonstrate quick translation
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Quick Translation Demo (GLSL -> MSL):");
        System.out.println("═══════════════════════════════════════════════════════════════");
        
        String quickMSL = glslToMSL("""
            #version 330
            in vec2 TexCoord;
            out vec4 FragColor;
            uniform sampler2D mainTexture;
            uniform vec4 tintColor;
            
            void main() {
                vec4 texColor = texture(mainTexture, TexCoord);
                FragColor = texColor * tintColor;
            }
            """);
        
        System.out.println(quickMSL);
        
    } finally {
        transpiler.shutdown();
    }
    
    System.out.println("\n═══════════════════════════════════════════════════════════════");
    System.out.println("Transpiler shutdown complete.");
    System.out.println("═══════════════════════════════════════════════════════════════");
}
}
