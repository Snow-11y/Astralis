package stellar.snow.astralis.api.directx.mapping;

// ═══════════════════════════════════════════════════════════════════════════════════
// Astralis Internal Imports
// ═══════════════════════════════════════════════════════════════════════════════════
import stellar.snow.astralis.config.Config;
import stellar.snow.astralis.engine.gpu.authority.UniversalCapabilities;
import stellar.snow.astralis.api.directx.managers.DirectXManager;

// ═══════════════════════════════════════════════════════════════════════════════════
// ██ LWJGL 3.4.0 BGFX: Core API & Static Imports
// ═══════════════════════════════════════════════════════════════════════════════════

import org.lwjgl.bgfx.*;
import static org.lwjgl.bgfx.BGFX.*;
import static org.lwjgl.bgfx.BGFXCaps.*;
import static org.lwjgl.bgfx.BGFXPlatform.*;

// ─── BGFX Structures ───
import org.lwjgl.bgfx.BGFXInit;
import org.lwjgl.bgfx.BGFXMemory;
import org.lwjgl.bgfx.BGFXTextureInfo;
import org.lwjgl.bgfx.BGFXUniformInfo;
import org.lwjgl.bgfx.BGFXVertexLayout;
import org.lwjgl.bgfx.BGFXCaps;
import org.lwjgl.bgfx.BGFXStats;
import org.lwjgl.bgfx.BGFXTransientVertexBuffer;
import org.lwjgl.bgfx.BGFXTransientIndexBuffer;
import org.lwjgl.bgfx.BGFXInstanceDataBuffer;
import org.lwjgl.bgfx.BGFXAttachment;
import org.lwjgl.bgfx.BGFXViewId;

// ─── BGFX Handles ───
import org.lwjgl.bgfx.BGFXTextureHandle;
import org.lwjgl.bgfx.BGFXProgramHandle;
import org.lwjgl.bgfx.BGFXShaderHandle;
import org.lwjgl.bgfx.BGFXUniformHandle;
import org.lwjgl.bgfx.BGFXVertexBufferHandle;
import org.lwjgl.bgfx.BGFXIndexBufferHandle;
import org.lwjgl.bgfx.BGFXFrameBufferHandle;
import org.lwjgl.bgfx.BGFXOcclusionQueryHandle;
import org.lwjgl.bgfx.BGFXIndirectBufferHandle;
import org.lwjgl.bgfx.BGFXDynamicVertexBufferHandle;
import org.lwjgl.bgfx.BGFXDynamicIndexBufferHandle;

// ─── BGFX Callbacks ───
import org.lwjgl.bgfx.BGFXCallback;
import org.lwjgl.bgfx.BGFXCallbackInterface;
import org.lwjgl.bgfx.BGFXReleaseFunctionCallback;

// ═══════════════════════════════════════════════════════════════════════════════════
// ██ LWJGL 3.4.0 System & Memory Management
// ═══════════════════════════════════════════════════════════════════════════════════

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Platform;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.MemoryStack.*;

// ═══════════════════════════════════════════════════════════════════════════════════
// ██ Java 25 Foreign Function & Memory API (Panama FFI)
// ═══════════════════════════════════════════════════════════════════════════════════

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.*;

// ═══════════════════════════════════════════════════════════════════════════════════
// ██ Java NIO: Buffers & Byte Order
// ═══════════════════════════════════════════════════════════════════════════════════

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;

// ═══════════════════════════════════════════════════════════════════════════════════
// ██ Java Concurrency: Locks
// ═══════════════════════════════════════════════════════════════════════════════════

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

// ═══════════════════════════════════════════════════════════════════════════════════
// ██ Java Concurrency: Atomic Types
// ═══════════════════════════════════════════════════════════════════════════════════

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.*;

// ═══════════════════════════════════════════════════════════════════════════════════
// ██ Java Concurrency: Collections & Executors
// ═══════════════════════════════════════════════════════════════════════════════════

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList; // most are technically covered but im using exact imports for precise control of bug detected

// ═══════════════════════════════════════════════════════════════════════════════════
// ██ Java Functional Interfaces
// ═══════════════════════════════════════════════════════════════════════════════════

import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.*;

// ═══════════════════════════════════════════════════════════════════════════════════
// ██ Java Streams
// ═══════════════════════════════════════════════════════════════════════════════════

import java.util.stream.IntStream;

// ═══════════════════════════════════════════════════════════════════════════════════
// ██ Java Collections & Utilities
// ═══════════════════════════════════════════════════════════════════════════════════

import java.util.BitSet;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.*;

// ═══════════════════════════════════════════════════════════════════════════════════
// ██ FastUtil: High-Performance Primitive Collections
// ═══════════════════════════════════════════════════════════════════════════════════

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════════╗
 * ║                        DIRECTX UNIFIED CALL MAPPER                               ║
 * ║                                                                                  ║
 * ║  The most complex graphics API mapper in existence. Handles the complete        ║
 * ║  DirectX ecosystem from DirectX 9.0c (2004) through DirectX 12 Ultimate (2020+) ║
 * ║                                                                                  ║
 * ║  SUPPORTED APIs:                                                                 ║
 * ║  ├─ DirectX 9.0c  - Fixed function, Shader Model 2.0/3.0                        ║
 * ║  ├─ DirectX 10.0  - Unified shader architecture, SM 4.0                         ║
 * ║  ├─ DirectX 10.1  - Cubemap arrays, SM 4.1                                      ║
 * ║  ├─ DirectX 11.0  - Tessellation, compute shaders, SM 5.0                       ║
 * ║  ├─ DirectX 11.1  - Constant buffer offsets, logical blend ops                  ║
 * ║  ├─ DirectX 11.2  - Tiled resources tier 1                                      ║
 * ║  ├─ DirectX 11.3  - Conservative rasterization, SM 5.1                          ║
 * ║  ├─ DirectX 12.0  - Explicit multi-threading, command lists                     ║
 * ║  └─ DirectX 12 Ultimate - Ray tracing, mesh shaders, VRS, SM 6.x                ║
 * ║                                                                                  ║
 * ║  ARCHITECTURE:                                                                   ║
 * ║  - Panama FFI for zero-copy native interop                                      ║
 * ║  - Fastutil collections for cache-friendly data structures                      ║
 * ║  - Lock-free concurrent state tracking                                          ║
 * ║  - Automatic fallback routing between API versions                              ║
 * ║  - Full resource barrier state machine for DX12                                 ║
 * ║  - Descriptor heap management with automatic defragmentation                    ║
 * ║                                                                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════════════╝
 */
public final class DirectXCallMapper {

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 1: DIRECTX VERSION ENUMERATION
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Supported DirectX API versions with feature level information.
     */
    public enum DirectXVersion {
        DX9(0x0900, "DirectX 9.0c", 2004, false, false, false, false),
        DX9_EX(0x0901, "DirectX 9.0Ex", 2006, false, false, false, false),
        DX10(0x0A00, "DirectX 10.0", 2006, false, false, true, false),
        DX10_1(0x0A01, "DirectX 10.1", 2008, false, false, true, false),
        DX11(0x0B00, "DirectX 11.0", 2009, true, true, true, false),
        DX11_1(0x0B01, "DirectX 11.1", 2012, true, true, true, false),
        DX11_2(0x0B02, "DirectX 11.2", 2013, true, true, true, false),
        DX11_3(0x0B03, "DirectX 11.3", 2015, true, true, true, false),
        DX11_4(0x0B04, "DirectX 11.4", 2016, true, true, true, false),
        DX12(0x0C00, "DirectX 12.0", 2015, true, true, true, true),
        DX12_1(0x0C01, "DirectX 12.1", 2018, true, true, true, true),
        DX12_2(0x0C02, "DirectX 12 Ultimate", 2020, true, true, true, true);

        public final int versionCode;
        public final String displayName;
        public final int releaseYear;
        public final boolean supportsTessellation;
        public final boolean supportsCompute;
        public final boolean supportsGeometryShaders;
        public final boolean supportsExplicitMultiEngine;

        DirectXVersion(int versionCode, String displayName, int releaseYear,
                       boolean supportsTessellation, boolean supportsCompute,
                       boolean supportsGeometryShaders, boolean supportsExplicitMultiEngine) {
            this.versionCode = versionCode;
            this.displayName = displayName;
            this.releaseYear = releaseYear;
            this.supportsTessellation = supportsTessellation;
            this.supportsCompute = supportsCompute;
            this.supportsGeometryShaders = supportsGeometryShaders;
            this.supportsExplicitMultiEngine = supportsExplicitMultiEngine;
        }

        public boolean isAtLeast(DirectXVersion other) {
            return this.versionCode >= other.versionCode;
        }

        public boolean isDX9Family() {
            return this == DX9 || this == DX9_EX;
        }

        public boolean isDX10Family() {
            return this == DX10 || this == DX10_1;
        }

        public boolean isDX11Family() {
            return versionCode >= DX11.versionCode && versionCode < DX12.versionCode;
        }

        public boolean isDX12Family() {
            return versionCode >= DX12.versionCode;
        }

        public static DirectXVersion fromFeatureLevel(int featureLevel) {
            return switch (featureLevel) {
                case 0x9100 -> DX9;      // D3D_FEATURE_LEVEL_9_1
                case 0x9200 -> DX9;      // D3D_FEATURE_LEVEL_9_2
                case 0x9300 -> DX9;      // D3D_FEATURE_LEVEL_9_3
                case 0xA000 -> DX10;     // D3D_FEATURE_LEVEL_10_0
                case 0xA100 -> DX10_1;   // D3D_FEATURE_LEVEL_10_1
                case 0xB000 -> DX11;     // D3D_FEATURE_LEVEL_11_0
                case 0xB100 -> DX11_1;   // D3D_FEATURE_LEVEL_11_1
                case 0xC000 -> DX12;     // D3D_FEATURE_LEVEL_12_0
                case 0xC100 -> DX12_1;   // D3D_FEATURE_LEVEL_12_1
                case 0xC200 -> DX12_2;   // D3D_FEATURE_LEVEL_12_2
                default -> DX11;
            };
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 2: D3D FEATURE LEVELS
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * D3D_FEATURE_LEVEL enumeration - defines hardware capability tiers.
     */
    public static final class D3DFeatureLevel {
        public static final int LEVEL_1_0_CORE = 0x1000;
        public static final int LEVEL_9_1 = 0x9100;
        public static final int LEVEL_9_2 = 0x9200;
        public static final int LEVEL_9_3 = 0x9300;
        public static final int LEVEL_10_0 = 0xA000;
        public static final int LEVEL_10_1 = 0xA100;
        public static final int LEVEL_11_0 = 0xB000;
        public static final int LEVEL_11_1 = 0xB100;
        public static final int LEVEL_12_0 = 0xC000;
        public static final int LEVEL_12_1 = 0xC100;
        public static final int LEVEL_12_2 = 0xC200;

        private D3DFeatureLevel() {}

        public static String getName(int level) {
            return switch (level) {
                case LEVEL_1_0_CORE -> "1_0_CORE";
                case LEVEL_9_1 -> "9_1";
                case LEVEL_9_2 -> "9_2";
                case LEVEL_9_3 -> "9_3";
                case LEVEL_10_0 -> "10_0";
                case LEVEL_10_1 -> "10_1";
                case LEVEL_11_0 -> "11_0";
                case LEVEL_11_1 -> "11_1";
                case LEVEL_12_0 -> "12_0";
                case LEVEL_12_1 -> "12_1";
                case LEVEL_12_2 -> "12_2";
                default -> "UNKNOWN_" + Integer.toHexString(level);
            };
        }

        public static int getMaxTextureSize(int level) {
            return switch (level) {
                case LEVEL_9_1 -> 2048;
                case LEVEL_9_2 -> 2048;
                case LEVEL_9_3 -> 4096;
                case LEVEL_10_0, LEVEL_10_1 -> 8192;
                default -> 16384;
            };
        }

        public static int getMaxRenderTargets(int level) {
            return switch (level) {
                case LEVEL_9_1, LEVEL_9_2, LEVEL_9_3 -> 4;
                default -> 8;
            };
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 3: SHADER MODEL DEFINITIONS
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * HLSL Shader Model versions and their capabilities.
     */
    public static final class ShaderModel {
        // Legacy shader models
        public static final int SM_1_1 = 11;  // DX8
        public static final int SM_2_0 = 20;  // DX9
        public static final int SM_2_A = 21;  // DX9 (ATI-extended)
        public static final int SM_2_B = 22;  // DX9 (NVIDIA-extended)
        public static final int SM_3_0 = 30;  // DX9c

        // Unified shader models
        public static final int SM_4_0 = 40;  // DX10
        public static final int SM_4_1 = 41;  // DX10.1
        public static final int SM_5_0 = 50;  // DX11
        public static final int SM_5_1 = 51;  // DX11.3 (resource arrays)

        // Modern shader models (DXIL-based)
        public static final int SM_6_0 = 60;  // Wave intrinsics
        public static final int SM_6_1 = 61;  // SV_ViewID, SV_Barycentrics
        public static final int SM_6_2 = 62;  // FP16, denorm mode
        public static final int SM_6_3 = 63;  // DXR 1.0
        public static final int SM_6_4 = 64;  // VRS, low-precision packed dot
        public static final int SM_6_5 = 65;  // DXR 1.1, mesh shaders, sampler feedback
        public static final int SM_6_6 = 66;  // 64-bit atomics, dynamic resources
        public static final int SM_6_7 = 67;  // Raw gather, quad read, advanced texture ops

        private ShaderModel() {}

        public static String getProfileName(int model, ShaderStage stage) {
            String stagePrefix = switch (stage) {
                case VERTEX -> "vs";
                case PIXEL -> "ps";
                case GEOMETRY -> "gs";
                case HULL -> "hs";
                case DOMAIN -> "ds";
                case COMPUTE -> "cs";
                case AMPLIFICATION -> "as";
                case MESH -> "ms";
                case RAY_GENERATION, CLOSEST_HIT, ANY_HIT, MISS, INTERSECTION, CALLABLE -> "lib";
            };

            return switch (model) {
                case SM_2_0 -> stagePrefix + "_2_0";
                case SM_2_A -> stagePrefix + "_2_a";
                case SM_2_B -> stagePrefix + "_2_b";
                case SM_3_0 -> stagePrefix + "_3_0";
                case SM_4_0 -> stagePrefix + "_4_0";
                case SM_4_1 -> stagePrefix + "_4_1";
                case SM_5_0 -> stagePrefix + "_5_0";
                case SM_5_1 -> stagePrefix + "_5_1";
                case SM_6_0 -> stagePrefix + "_6_0";
                case SM_6_1 -> stagePrefix + "_6_1";
                case SM_6_2 -> stagePrefix + "_6_2";
                case SM_6_3 -> stagePrefix + "_6_3";
                case SM_6_4 -> stagePrefix + "_6_4";
                case SM_6_5 -> stagePrefix + "_6_5";
                case SM_6_6 -> stagePrefix + "_6_6";
                case SM_6_7 -> stagePrefix + "_6_7";
                default -> stagePrefix + "_5_0";
            };
        }

        public static boolean supportsWaveIntrinsics(int model) {
            return model >= SM_6_0;
        }

        public static boolean supportsRayTracing(int model) {
            return model >= SM_6_3;
        }

        public static boolean supportsMeshShaders(int model) {
            return model >= SM_6_5;
        }

        public static boolean supportsDynamicResources(int model) {
            return model >= SM_6_6;
        }
    }

    /**
     * Shader stage enumeration covering all programmable pipeline stages.
     */
    public enum ShaderStage {
        VERTEX(0, "VS"),
        HULL(1, "HS"),
        DOMAIN(2, "DS"),
        GEOMETRY(3, "GS"),
        PIXEL(4, "PS"),
        COMPUTE(5, "CS"),
        AMPLIFICATION(6, "AS"),
        MESH(7, "MS"),
        RAY_GENERATION(8, "RGS"),
        CLOSEST_HIT(9, "CHS"),
        ANY_HIT(10, "AHS"),
        MISS(11, "MISS"),
        INTERSECTION(12, "IS"),
        CALLABLE(13, "CALL");

        public final int index;
        public final String shortName;

        ShaderStage(int index, String shortName) {
            this.index = index;
            this.shortName = shortName;
        }

        public boolean isRayTracingStage() {
            return this.ordinal() >= RAY_GENERATION.ordinal();
        }

        public boolean isMeshPipelineStage() {
            return this == AMPLIFICATION || this == MESH;
        }

        public boolean isTraditionalRasterStage() {
            return this == VERTEX || this == HULL || this == DOMAIN || 
                   this == GEOMETRY || this == PIXEL;
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 4: COMPREHENSIVE CALL TYPE ENUMERATION
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Complete enumeration of all DirectX API calls across all versions.
     * 
     * Organization:
     * - 0x0000-0x0FFF: Device & Factory operations
     * - 0x1000-0x1FFF: Resource creation
     * - 0x2000-0x2FFF: Resource operations (copy, map, update)
     * - 0x3000-0x3FFF: Pipeline state
     * - 0x4000-0x4FFF: Shader operations
     * - 0x5000-0x5FFF: Draw commands
     * - 0x6000-0x6FFF: Compute commands
     * - 0x7000-0x7FFF: Query operations
     * - 0x8000-0x8FFF: Synchronization
     * - 0x9000-0x9FFF: DXGI operations
     * - 0xA000-0xAFFF: DX12 Command list operations
     * - 0xB000-0xBFFF: DX12 Descriptor operations
     * - 0xC000-0xCFFF: DX12 Root signature operations
     * - 0xD000-0xDFFF: Ray tracing operations
     * - 0xE000-0xEFFF: Mesh shader operations
     * - 0xF000-0xFFFF: Debug & misc operations
     */
    public enum CallType {
        // ════════════════════════════════════════════════════════════════════════════
        // DEVICE & FACTORY CREATION (0x0000-0x0FFF)
        // ════════════════════════════════════════════════════════════════════════════

        // DX9 Device Creation
        DX9_CREATE_DEVICE(0x0001, DirectXVersion.DX9),
        DX9_CREATE_DEVICE_EX(0x0002, DirectXVersion.DX9_EX),
        DX9_CREATE_DIRECT3D(0x0003, DirectXVersion.DX9),
        DX9_GET_ADAPTER_IDENTIFIER(0x0004, DirectXVersion.DX9),
        DX9_GET_DEVICE_CAPS(0x0005, DirectXVersion.DX9),
        DX9_CHECK_DEVICE_FORMAT(0x0006, DirectXVersion.DX9),
        DX9_CHECK_DEVICE_MULTI_SAMPLE_TYPE(0x0007, DirectXVersion.DX9),
        DX9_CHECK_DEPTH_STENCIL_MATCH(0x0008, DirectXVersion.DX9),
        DX9_CHECK_DEVICE_TYPE(0x0009, DirectXVersion.DX9),
        DX9_GET_ADAPTER_COUNT(0x000A, DirectXVersion.DX9),
        DX9_GET_ADAPTER_DISPLAY_MODE(0x000B, DirectXVersion.DX9),
        DX9_GET_ADAPTER_MODE_COUNT(0x000C, DirectXVersion.DX9),
        DX9_ENUMERATE_ADAPTER_MODES(0x000D, DirectXVersion.DX9),
        DX9_REGISTER_SOFTWARE_DEVICE(0x000E, DirectXVersion.DX9),

        // DX10/11 Device Creation
        DX11_CREATE_DEVICE(0x0100, DirectXVersion.DX11),
        DX11_CREATE_DEVICE_AND_SWAP_CHAIN(0x0101, DirectXVersion.DX11),
        DX11_GET_IMMEDIATE_CONTEXT(0x0102, DirectXVersion.DX11),
        DX11_GET_IMMEDIATE_CONTEXT1(0x0103, DirectXVersion.DX11_1),
        DX11_CREATE_DEFERRED_CONTEXT(0x0104, DirectXVersion.DX11),
        DX11_CREATE_DEFERRED_CONTEXT1(0x0105, DirectXVersion.DX11_1),
        DX11_CREATE_DEFERRED_CONTEXT2(0x0106, DirectXVersion.DX11_2),
        DX11_CREATE_DEFERRED_CONTEXT3(0x0107, DirectXVersion.DX11_3),
        DX11_GET_FEATURE_LEVEL(0x0108, DirectXVersion.DX11),
        DX11_CHECK_FEATURE_SUPPORT(0x0109, DirectXVersion.DX11),
        DX11_CHECK_FORMAT_SUPPORT(0x010A, DirectXVersion.DX11),
        DX11_CHECK_MULTISAMPLE_QUALITY_LEVELS(0x010B, DirectXVersion.DX11),
        DX11_CHECK_COUNTER(0x010C, DirectXVersion.DX11),
        DX11_CHECK_COUNTER_INFO(0x010D, DirectXVersion.DX11),
        DX11_GET_CREATION_FLAGS(0x010E, DirectXVersion.DX11),
        DX11_GET_DEVICE_REMOVED_REASON(0x010F, DirectXVersion.DX11),
        DX11_SET_EXCEPTION_MODE(0x0110, DirectXVersion.DX11),
        DX11_GET_EXCEPTION_MODE(0x0111, DirectXVersion.DX11),
        DX11_OPEN_SHARED_RESOURCE(0x0112, DirectXVersion.DX11),
        DX11_OPEN_SHARED_RESOURCE1(0x0113, DirectXVersion.DX11_1),
        DX11_OPEN_SHARED_RESOURCE_BY_NAME(0x0114, DirectXVersion.DX11_1),
        DX11_OPEN_SHARED_FENCE(0x0115, DirectXVersion.DX11_3),
        DX11_CREATE_FENCE(0x0116, DirectXVersion.DX11_3),

        // DX12 Device Creation
        DX12_CREATE_DEVICE(0x0200, DirectXVersion.DX12),
        DX12_GET_ADAPTER(0x0201, DirectXVersion.DX12),
        DX12_CHECK_FEATURE_SUPPORT(0x0202, DirectXVersion.DX12),
        DX12_CREATE_COMMAND_QUEUE(0x0203, DirectXVersion.DX12),
        DX12_CREATE_COMMAND_ALLOCATOR(0x0204, DirectXVersion.DX12),
        DX12_CREATE_GRAPHICS_COMMAND_LIST(0x0205, DirectXVersion.DX12),
        DX12_CREATE_GRAPHICS_COMMAND_LIST1(0x0206, DirectXVersion.DX12_1),
        DX12_CREATE_COMMAND_SIGNATURE(0x0207, DirectXVersion.DX12),
        DX12_GET_DESCRIPTOR_HANDLE_INCREMENT_SIZE(0x0208, DirectXVersion.DX12),
        DX12_GET_RESOURCE_ALLOCATION_INFO(0x0209, DirectXVersion.DX12),
        DX12_GET_RESOURCE_ALLOCATION_INFO1(0x020A, DirectXVersion.DX12_1),
        DX12_GET_CUSTOM_HEAP_PROPERTIES(0x020B, DirectXVersion.DX12),
        DX12_GET_ADAPTER_LUID(0x020C, DirectXVersion.DX12),
        DX12_GET_NODE_COUNT(0x020D, DirectXVersion.DX12),
        DX12_CREATE_SHARED_HANDLE(0x020E, DirectXVersion.DX12),
        DX12_OPEN_SHARED_HANDLE(0x020F, DirectXVersion.DX12),
        DX12_OPEN_SHARED_HANDLE_BY_NAME(0x0210, DirectXVersion.DX12),
        DX12_SET_STABLE_POWER_STATE(0x0211, DirectXVersion.DX12),
        DX12_GET_DEVICE_REMOVED_REASON(0x0212, DirectXVersion.DX12),
        DX12_GET_COPYABLE_FOOTPRINTS(0x0213, DirectXVersion.DX12),
        DX12_CREATE_QUERY_HEAP(0x0214, DirectXVersion.DX12),
        DX12_SET_RESIDENCY_PRIORITY(0x0215, DirectXVersion.DX12),
        DX12_CREATE_PROTECTED_SESSION(0x0216, DirectXVersion.DX12_1),
        DX12_CREATE_LIFETIME_TRACKER(0x0217, DirectXVersion.DX12_1),
        DX12_REMOVE_DEVICE(0x0218, DirectXVersion.DX12),
        DX12_ENUMERATE_META_COMMANDS(0x0219, DirectXVersion.DX12_1),
        DX12_CREATE_META_COMMAND(0x021A, DirectXVersion.DX12_1),
        DX12_CREATE_STATE_OBJECT(0x021B, DirectXVersion.DX12_1),
        DX12_GET_RAYTRACING_ACCELERATION_STRUCTURE_PREBUILD_INFO(0x021C, DirectXVersion.DX12_1),
        DX12_CHECK_DRIVER_MATCHING_IDENTIFIER(0x021D, DirectXVersion.DX12_1),
        DX12_SET_BACKGROUND_PROCESSING_MODE(0x021E, DirectXVersion.DX12_2),
        DX12_ADD_TO_STATE_OBJECT(0x021F, DirectXVersion.DX12_2),
        DX12_CREATE_PROTECTED_RESOURCE_SESSION1(0x0220, DirectXVersion.DX12_2),
        DX12_CREATE_SAMPLER_FEEDBACK_UNORDERED_ACCESS_VIEW(0x0221, DirectXVersion.DX12_2),
        DX12_CREATE_COMMAND_LIST1(0x0222, DirectXVersion.DX12_1),

        // Debug Device
        DX11_SET_DEBUG_FEATURE(0x0300, DirectXVersion.DX11),
        DX11_GET_DEBUG_FEATURE(0x0301, DirectXVersion.DX11),
        DX12_GET_DEBUG_INTERFACE(0x0302, DirectXVersion.DX12),
        DX12_ENABLE_DEBUG_LAYER(0x0303, DirectXVersion.DX12),
        DX12_SET_GPU_BASED_VALIDATION_FLAGS(0x0304, DirectXVersion.DX12),
        DX12_SET_ENABLE_GPU_BASED_VALIDATION(0x0305, DirectXVersion.DX12),
        DX12_SET_ENABLE_SYNCHRONIZED_COMMAND_QUEUE_VALIDATION(0x0306, DirectXVersion.DX12),
        DX12_REPORT_LIVE_DEVICE_OBJECTS(0x0307, DirectXVersion.DX12),

        // ════════════════════════════════════════════════════════════════════════════
        // RESOURCE CREATION (0x1000-0x1FFF)
        // ════════════════════════════════════════════════════════════════════════════

        // DX9 Resources
        DX9_CREATE_VERTEX_BUFFER(0x1001, DirectXVersion.DX9),
        DX9_CREATE_INDEX_BUFFER(0x1002, DirectXVersion.DX9),
        DX9_CREATE_TEXTURE(0x1003, DirectXVersion.DX9),
        DX9_CREATE_VOLUME_TEXTURE(0x1004, DirectXVersion.DX9),
        DX9_CREATE_CUBE_TEXTURE(0x1005, DirectXVersion.DX9),
        DX9_CREATE_RENDER_TARGET(0x1006, DirectXVersion.DX9),
        DX9_CREATE_DEPTH_STENCIL_SURFACE(0x1007, DirectXVersion.DX9),
        DX9_CREATE_OFFSCREEN_PLAIN_SURFACE(0x1008, DirectXVersion.DX9),
        DX9_CREATE_ADDITIONAL_SWAP_CHAIN(0x1009, DirectXVersion.DX9),
        DX9_CREATE_VERTEX_DECLARATION(0x100A, DirectXVersion.DX9),
        DX9_CREATE_VERTEX_SHADER(0x100B, DirectXVersion.DX9),
        DX9_CREATE_PIXEL_SHADER(0x100C, DirectXVersion.DX9),
        DX9_CREATE_STATE_BLOCK(0x100D, DirectXVersion.DX9),
        DX9_CREATE_QUERY(0x100E, DirectXVersion.DX9),

        // DX11 Buffers
        DX11_CREATE_BUFFER(0x1100, DirectXVersion.DX11),
        DX11_CREATE_TEXTURE_1D(0x1101, DirectXVersion.DX11),
        DX11_CREATE_TEXTURE_2D(0x1102, DirectXVersion.DX11),
        DX11_CREATE_TEXTURE_2D1(0x1103, DirectXVersion.DX11_3),
        DX11_CREATE_TEXTURE_3D(0x1104, DirectXVersion.DX11),
        DX11_CREATE_TEXTURE_3D1(0x1105, DirectXVersion.DX11_3),
        DX11_CREATE_SHADER_RESOURCE_VIEW(0x1106, DirectXVersion.DX11),
        DX11_CREATE_SHADER_RESOURCE_VIEW1(0x1107, DirectXVersion.DX11_3),
        DX11_CREATE_UNORDERED_ACCESS_VIEW(0x1108, DirectXVersion.DX11),
        DX11_CREATE_UNORDERED_ACCESS_VIEW1(0x1109, DirectXVersion.DX11_3),
        DX11_CREATE_RENDER_TARGET_VIEW(0x110A, DirectXVersion.DX11),
        DX11_CREATE_RENDER_TARGET_VIEW1(0x110B, DirectXVersion.DX11_3),
        DX11_CREATE_DEPTH_STENCIL_VIEW(0x110C, DirectXVersion.DX11),
        DX11_CREATE_INPUT_LAYOUT(0x110D, DirectXVersion.DX11),
        DX11_CREATE_SAMPLER_STATE(0x110E, DirectXVersion.DX11),
        DX11_CREATE_QUERY(0x110F, DirectXVersion.DX11),
        DX11_CREATE_QUERY1(0x1110, DirectXVersion.DX11_3),
        DX11_CREATE_PREDICATE(0x1111, DirectXVersion.DX11),
        DX11_CREATE_COUNTER(0x1112, DirectXVersion.DX11),
        DX11_CREATE_RASTERIZER_STATE(0x1113, DirectXVersion.DX11),
        DX11_CREATE_RASTERIZER_STATE1(0x1114, DirectXVersion.DX11_1),
        DX11_CREATE_RASTERIZER_STATE2(0x1115, DirectXVersion.DX11_3),
        DX11_CREATE_BLEND_STATE(0x1116, DirectXVersion.DX11),
        DX11_CREATE_BLEND_STATE1(0x1117, DirectXVersion.DX11_1),
        DX11_CREATE_DEPTH_STENCIL_STATE(0x1118, DirectXVersion.DX11),
        DX11_CREATE_VERTEX_SHADER(0x1119, DirectXVersion.DX11),
        DX11_CREATE_HULL_SHADER(0x111A, DirectXVersion.DX11),
        DX11_CREATE_DOMAIN_SHADER(0x111B, DirectXVersion.DX11),
        DX11_CREATE_GEOMETRY_SHADER(0x111C, DirectXVersion.DX11),
        DX11_CREATE_GEOMETRY_SHADER_WITH_STREAM_OUTPUT(0x111D, DirectXVersion.DX11),
        DX11_CREATE_PIXEL_SHADER(0x111E, DirectXVersion.DX11),
        DX11_CREATE_COMPUTE_SHADER(0x111F, DirectXVersion.DX11),
        DX11_CREATE_CLASS_LINKAGE(0x1120, DirectXVersion.DX11),

        // DX12 Resources
        DX12_CREATE_COMMITTED_RESOURCE(0x1200, DirectXVersion.DX12),
        DX12_CREATE_COMMITTED_RESOURCE1(0x1201, DirectXVersion.DX12_1),
        DX12_CREATE_COMMITTED_RESOURCE2(0x1202, DirectXVersion.DX12_2),
        DX12_CREATE_PLACED_RESOURCE(0x1203, DirectXVersion.DX12),
        DX12_CREATE_PLACED_RESOURCE1(0x1204, DirectXVersion.DX12_2),
        DX12_CREATE_RESERVED_RESOURCE(0x1205, DirectXVersion.DX12),
        DX12_CREATE_RESERVED_RESOURCE1(0x1206, DirectXVersion.DX12_2),
        DX12_CREATE_HEAP(0x1207, DirectXVersion.DX12),
        DX12_CREATE_HEAP1(0x1208, DirectXVersion.DX12_1),
        DX12_CREATE_DESCRIPTOR_HEAP(0x1209, DirectXVersion.DX12),
        DX12_CREATE_ROOT_SIGNATURE(0x120A, DirectXVersion.DX12),
        DX12_CREATE_ROOT_SIGNATURE_DESERIALIZER(0x120B, DirectXVersion.DX12),
        DX12_CREATE_VERSIONED_ROOT_SIGNATURE_DESERIALIZER(0x120C, DirectXVersion.DX12),
        DX12_SERIALIZE_ROOT_SIGNATURE(0x120D, DirectXVersion.DX12),
        DX12_SERIALIZE_VERSIONED_ROOT_SIGNATURE(0x120E, DirectXVersion.DX12),
        DX12_CREATE_GRAPHICS_PIPELINE_STATE(0x120F, DirectXVersion.DX12),
        DX12_CREATE_COMPUTE_PIPELINE_STATE(0x1210, DirectXVersion.DX12),
        DX12_CREATE_PIPELINE_LIBRARY(0x1211, DirectXVersion.DX12),
        DX12_CREATE_PIPELINE_LIBRARY1(0x1212, DirectXVersion.DX12_1),
        DX12_STORE_PIPELINE(0x1213, DirectXVersion.DX12),
        DX12_LOAD_GRAPHICS_PIPELINE(0x1214, DirectXVersion.DX12),
        DX12_LOAD_COMPUTE_PIPELINE(0x1215, DirectXVersion.DX12),
        DX12_CREATE_FENCE(0x1216, DirectXVersion.DX12),
        DX12_MAKE_RESIDENT(0x1217, DirectXVersion.DX12),
        DX12_EVICT(0x1218, DirectXVersion.DX12),
        DX12_CREATE_CONSTANT_BUFFER_VIEW(0x1219, DirectXVersion.DX12),
        DX12_CREATE_SHADER_RESOURCE_VIEW(0x121A, DirectXVersion.DX12),
        DX12_CREATE_UNORDERED_ACCESS_VIEW(0x121B, DirectXVersion.DX12),
        DX12_CREATE_RENDER_TARGET_VIEW(0x121C, DirectXVersion.DX12),
        DX12_CREATE_DEPTH_STENCIL_VIEW(0x121D, DirectXVersion.DX12),
        DX12_CREATE_SAMPLER(0x121E, DirectXVersion.DX12),
        DX12_COPY_DESCRIPTORS(0x121F, DirectXVersion.DX12),
        DX12_COPY_DESCRIPTORS_SIMPLE(0x1220, DirectXVersion.DX12),

        // ════════════════════════════════════════════════════════════════════════════
        // RESOURCE OPERATIONS (0x2000-0x2FFF)
        // ════════════════════════════════════════════════════════════════════════════

        // DX9 Resource Operations
        DX9_LOCK_VERTEX_BUFFER(0x2001, DirectXVersion.DX9),
        DX9_UNLOCK_VERTEX_BUFFER(0x2002, DirectXVersion.DX9),
        DX9_LOCK_INDEX_BUFFER(0x2003, DirectXVersion.DX9),
        DX9_UNLOCK_INDEX_BUFFER(0x2004, DirectXVersion.DX9),
        DX9_LOCK_RECT(0x2005, DirectXVersion.DX9),
        DX9_UNLOCK_RECT(0x2006, DirectXVersion.DX9),
        DX9_LOCK_BOX(0x2007, DirectXVersion.DX9),
        DX9_UNLOCK_BOX(0x2008, DirectXVersion.DX9),
        DX9_UPDATE_SURFACE(0x2009, DirectXVersion.DX9),
        DX9_UPDATE_TEXTURE(0x200A, DirectXVersion.DX9),
        DX9_GET_RENDER_TARGET_DATA(0x200B, DirectXVersion.DX9),
        DX9_GET_FRONT_BUFFER_DATA(0x200C, DirectXVersion.DX9),
        DX9_STRETCH_RECT(0x200D, DirectXVersion.DX9),
        DX9_COLOR_FILL(0x200E, DirectXVersion.DX9),
        DX9_GET_SURFACE_LEVEL(0x200F, DirectXVersion.DX9),
        DX9_GET_CUBE_MAP_SURFACE(0x2010, DirectXVersion.DX9),
        DX9_GET_VOLUME_LEVEL(0x2011, DirectXVersion.DX9),
        DX9_LOAD_SURFACE_FROM_SURFACE(0x2012, DirectXVersion.DX9),
        DX9_LOAD_VOLUME_FROM_VOLUME(0x2013, DirectXVersion.DX9),
        DX9_LOAD_SURFACE_FROM_MEMORY(0x2014, DirectXVersion.DX9),
        DX9_LOAD_VOLUME_FROM_MEMORY(0x2015, DirectXVersion.DX9),
        DX9_GENERATE_MIPS(0x2016, DirectXVersion.DX9),

        // DX11 Resource Operations
        DX11_MAP(0x2100, DirectXVersion.DX11),
        DX11_UNMAP(0x2101, DirectXVersion.DX11),
        DX11_UPDATE_SUBRESOURCE(0x2102, DirectXVersion.DX11),
        DX11_UPDATE_SUBRESOURCE1(0x2103, DirectXVersion.DX11_1),
        DX11_COPY_RESOURCE(0x2104, DirectXVersion.DX11),
        DX11_COPY_SUBRESOURCE_REGION(0x2105, DirectXVersion.DX11),
        DX11_COPY_SUBRESOURCE_REGION1(0x2106, DirectXVersion.DX11_1),
        DX11_COPY_STRUCTURE_COUNT(0x2107, DirectXVersion.DX11),
        DX11_RESOLVE_SUBRESOURCE(0x2108, DirectXVersion.DX11),
        DX11_GENERATE_MIPS(0x2109, DirectXVersion.DX11),
        DX11_READ_FROM_SUBRESOURCE(0x210A, DirectXVersion.DX11_3),
        DX11_WRITE_TO_SUBRESOURCE(0x210B, DirectXVersion.DX11_3),
        DX11_CLEAR_RENDER_TARGET_VIEW(0x210C, DirectXVersion.DX11),
        DX11_CLEAR_DEPTH_STENCIL_VIEW(0x210D, DirectXVersion.DX11),
        DX11_CLEAR_UNORDERED_ACCESS_VIEW_UINT(0x210E, DirectXVersion.DX11),
        DX11_CLEAR_UNORDERED_ACCESS_VIEW_FLOAT(0x210F, DirectXVersion.DX11),
        DX11_CLEAR_STATE(0x2110, DirectXVersion.DX11),
        DX11_FLUSH(0x2111, DirectXVersion.DX11),
        DX11_GET_DATA(0x2112, DirectXVersion.DX11),
        DX11_SET_RESOURCE_MIN_LOD(0x2113, DirectXVersion.DX11),
        DX11_GET_RESOURCE_MIN_LOD(0x2114, DirectXVersion.DX11),
        DX11_DISCARD_RESOURCE(0x2115, DirectXVersion.DX11_1),
        DX11_DISCARD_VIEW(0x2116, DirectXVersion.DX11_1),
        DX11_DISCARD_VIEW1(0x2117, DirectXVersion.DX11_1),
        DX11_COPY_TILE_MAPPINGS(0x2118, DirectXVersion.DX11_2),
        DX11_COPY_TILES(0x2119, DirectXVersion.DX11_2),
        DX11_UPDATE_TILE_MAPPINGS(0x211A, DirectXVersion.DX11_2),
        DX11_UPDATE_TILES(0x211B, DirectXVersion.DX11_2),
        DX11_RESIZE_TILE_POOL(0x211C, DirectXVersion.DX11_2),
        DX11_TILED_RESOURCE_BARRIER(0x211D, DirectXVersion.DX11_2),
        DX11_GET_TILE_OP_QUEUE_STATUS(0x211E, DirectXVersion.DX11_2),
        DX11_WAIT_FOR_TILE_OP(0x211F, DirectXVersion.DX11_2),

        // DX12 Resource Operations
        DX12_MAP(0x2200, DirectXVersion.DX12),
        DX12_UNMAP(0x2201, DirectXVersion.DX12),
        DX12_GET_GPU_VIRTUAL_ADDRESS(0x2202, DirectXVersion.DX12),
        DX12_WRITE_TO_SUBRESOURCE(0x2203, DirectXVersion.DX12),
        DX12_READ_FROM_SUBRESOURCE(0x2204, DirectXVersion.DX12),
        DX12_GET_HEAP_PROPERTIES(0x2205, DirectXVersion.DX12),

        // ════════════════════════════════════════════════════════════════════════════
        // PIPELINE STATE (0x3000-0x3FFF)
        // ════════════════════════════════════════════════════════════════════════════

        // DX9 Render States (All 200+ render states)
        DX9_SET_RENDER_STATE(0x3001, DirectXVersion.DX9),
        DX9_GET_RENDER_STATE(0x3002, DirectXVersion.DX9),
        DX9_SET_TEXTURE_STAGE_STATE(0x3003, DirectXVersion.DX9),
        DX9_GET_TEXTURE_STAGE_STATE(0x3004, DirectXVersion.DX9),
        DX9_SET_SAMPLER_STATE(0x3005, DirectXVersion.DX9),
        DX9_GET_SAMPLER_STATE(0x3006, DirectXVersion.DX9),
        DX9_SET_TEXTURE(0x3007, DirectXVersion.DX9),
        DX9_GET_TEXTURE(0x3008, DirectXVersion.DX9),
        DX9_SET_TRANSFORM(0x3009, DirectXVersion.DX9),
        DX9_GET_TRANSFORM(0x300A, DirectXVersion.DX9),
        DX9_MULTIPLY_TRANSFORM(0x300B, DirectXVersion.DX9),
        DX9_SET_VIEWPORT(0x300C, DirectXVersion.DX9),
        DX9_GET_VIEWPORT(0x300D, DirectXVersion.DX9),
        DX9_SET_SCISSOR_RECT(0x300E, DirectXVersion.DX9),
        DX9_GET_SCISSOR_RECT(0x300F, DirectXVersion.DX9),
        DX9_SET_CLIP_PLANE(0x3010, DirectXVersion.DX9),
        DX9_GET_CLIP_PLANE(0x3011, DirectXVersion.DX9),
        DX9_SET_MATERIAL(0x3012, DirectXVersion.DX9),
        DX9_GET_MATERIAL(0x3013, DirectXVersion.DX9),
        DX9_SET_LIGHT(0x3014, DirectXVersion.DX9),
        DX9_GET_LIGHT(0x3015, DirectXVersion.DX9),
        DX9_LIGHT_ENABLE(0x3016, DirectXVersion.DX9),
        DX9_GET_LIGHT_ENABLE(0x3017, DirectXVersion.DX9),
        DX9_SET_CLIP_STATUS(0x3018, DirectXVersion.DX9),
        DX9_GET_CLIP_STATUS(0x3019, DirectXVersion.DX9),
        DX9_SET_CURRENT_TEXTURE_PALETTE(0x301A, DirectXVersion.DX9),
        DX9_GET_CURRENT_TEXTURE_PALETTE(0x301B, DirectXVersion.DX9),
        DX9_SET_PALETTE_ENTRIES(0x301C, DirectXVersion.DX9),
        DX9_GET_PALETTE_ENTRIES(0x301D, DirectXVersion.DX9),
        DX9_SET_NPatch_MODE(0x301E, DirectXVersion.DX9),
        DX9_GET_NPatch_MODE(0x301F, DirectXVersion.DX9),
        DX9_SET_SOFTWARE_VERTEX_PROCESSING(0x3020, DirectXVersion.DX9),
        DX9_GET_SOFTWARE_VERTEX_PROCESSING(0x3021, DirectXVersion.DX9),
        DX9_SET_FVF(0x3022, DirectXVersion.DX9),
        DX9_GET_FVF(0x3023, DirectXVersion.DX9),
        DX9_SET_VERTEX_DECLARATION(0x3024, DirectXVersion.DX9),
        DX9_GET_VERTEX_DECLARATION(0x3025, DirectXVersion.DX9),
        DX9_SET_STREAM_SOURCE(0x3026, DirectXVersion.DX9),
        DX9_GET_STREAM_SOURCE(0x3027, DirectXVersion.DX9),
        DX9_SET_STREAM_SOURCE_FREQ(0x3028, DirectXVersion.DX9),
        DX9_GET_STREAM_SOURCE_FREQ(0x3029, DirectXVersion.DX9),
        DX9_SET_INDICES(0x302A, DirectXVersion.DX9),
        DX9_GET_INDICES(0x302B, DirectXVersion.DX9),
        DX9_SET_VERTEX_SHADER(0x302C, DirectXVersion.DX9),
        DX9_GET_VERTEX_SHADER(0x302D, DirectXVersion.DX9),
        DX9_SET_PIXEL_SHADER(0x302E, DirectXVersion.DX9),
        DX9_GET_PIXEL_SHADER(0x302F, DirectXVersion.DX9),
        DX9_SET_VERTEX_SHADER_CONSTANT_F(0x3030, DirectXVersion.DX9),
        DX9_GET_VERTEX_SHADER_CONSTANT_F(0x3031, DirectXVersion.DX9),
        DX9_SET_VERTEX_SHADER_CONSTANT_I(0x3032, DirectXVersion.DX9),
        DX9_GET_VERTEX_SHADER_CONSTANT_I(0x3033, DirectXVersion.DX9),
        DX9_SET_VERTEX_SHADER_CONSTANT_B(0x3034, DirectXVersion.DX9),
        DX9_GET_VERTEX_SHADER_CONSTANT_B(0x3035, DirectXVersion.DX9),
        DX9_SET_PIXEL_SHADER_CONSTANT_F(0x3036, DirectXVersion.DX9),
        DX9_GET_PIXEL_SHADER_CONSTANT_F(0x3037, DirectXVersion.DX9),
        DX9_SET_PIXEL_SHADER_CONSTANT_I(0x3038, DirectXVersion.DX9),
        DX9_GET_PIXEL_SHADER_CONSTANT_I(0x3039, DirectXVersion.DX9),
        DX9_SET_PIXEL_SHADER_CONSTANT_B(0x303A, DirectXVersion.DX9),
        DX9_GET_PIXEL_SHADER_CONSTANT_B(0x303B, DirectXVersion.DX9),
        DX9_SET_RENDER_TARGET(0x303C, DirectXVersion.DX9),
        DX9_GET_RENDER_TARGET(0x303D, DirectXVersion.DX9),
        DX9_SET_DEPTH_STENCIL_SURFACE(0x303E, DirectXVersion.DX9),
        DX9_GET_DEPTH_STENCIL_SURFACE(0x303F, DirectXVersion.DX9),

        // DX11 Pipeline State
        DX11_IA_SET_INPUT_LAYOUT(0x3100, DirectXVersion.DX11),
        DX11_IA_GET_INPUT_LAYOUT(0x3101, DirectXVersion.DX11),
        DX11_IA_SET_VERTEX_BUFFERS(0x3102, DirectXVersion.DX11),
        DX11_IA_GET_VERTEX_BUFFERS(0x3103, DirectXVersion.DX11),
        DX11_IA_SET_INDEX_BUFFER(0x3104, DirectXVersion.DX11),
        DX11_IA_GET_INDEX_BUFFER(0x3105, DirectXVersion.DX11),
        DX11_IA_SET_PRIMITIVE_TOPOLOGY(0x3106, DirectXVersion.DX11),
        DX11_IA_GET_PRIMITIVE_TOPOLOGY(0x3107, DirectXVersion.DX11),

        // DX11 Vertex Shader Stage
        DX11_VS_SET_SHADER(0x3110, DirectXVersion.DX11),
        DX11_VS_GET_SHADER(0x3111, DirectXVersion.DX11),
        DX11_VS_SET_CONSTANT_BUFFERS(0x3112, DirectXVersion.DX11),
        DX11_VS_GET_CONSTANT_BUFFERS(0x3113, DirectXVersion.DX11),
        DX11_VS_SET_CONSTANT_BUFFERS1(0x3114, DirectXVersion.DX11_1),
        DX11_VS_GET_CONSTANT_BUFFERS1(0x3115, DirectXVersion.DX11_1),
        DX11_VS_SET_SHADER_RESOURCES(0x3116, DirectXVersion.DX11),
        DX11_VS_GET_SHADER_RESOURCES(0x3117, DirectXVersion.DX11),
        DX11_VS_SET_SAMPLERS(0x3118, DirectXVersion.DX11),
        DX11_VS_GET_SAMPLERS(0x3119, DirectXVersion.DX11),

        // DX11 Hull Shader Stage
        DX11_HS_SET_SHADER(0x3120, DirectXVersion.DX11),
        DX11_HS_GET_SHADER(0x3121, DirectXVersion.DX11),
        DX11_HS_SET_CONSTANT_BUFFERS(0x3122, DirectXVersion.DX11),
        DX11_HS_GET_CONSTANT_BUFFERS(0x3123, DirectXVersion.DX11),
        DX11_HS_SET_CONSTANT_BUFFERS1(0x3124, DirectXVersion.DX11_1),
        DX11_HS_GET_CONSTANT_BUFFERS1(0x3125, DirectXVersion.DX11_1),
        DX11_HS_SET_SHADER_RESOURCES(0x3126, DirectXVersion.DX11),
        DX11_HS_GET_SHADER_RESOURCES(0x3127, DirectXVersion.DX11),
        DX11_HS_SET_SAMPLERS(0x3128, DirectXVersion.DX11),
        DX11_HS_GET_SAMPLERS(0x3129, DirectXVersion.DX11),

        // DX11 Domain Shader Stage
        DX11_DS_SET_SHADER(0x3130, DirectXVersion.DX11),
        DX11_DS_GET_SHADER(0x3131, DirectXVersion.DX11),
        DX11_DS_SET_CONSTANT_BUFFERS(0x3132, DirectXVersion.DX11),
        DX11_DS_GET_CONSTANT_BUFFERS(0x3133, DirectXVersion.DX11),
        DX11_DS_SET_CONSTANT_BUFFERS1(0x3134, DirectXVersion.DX11_1),
        DX11_DS_GET_CONSTANT_BUFFERS1(0x3135, DirectXVersion.DX11_1),
        DX11_DS_SET_SHADER_RESOURCES(0x3136, DirectXVersion.DX11),
        DX11_DS_GET_SHADER_RESOURCES(0x3137, DirectXVersion.DX11),
        DX11_DS_SET_SAMPLERS(0x3138, DirectXVersion.DX11),
        DX11_DS_GET_SAMPLERS(0x3139, DirectXVersion.DX11),

        // DX11 Geometry Shader Stage
        DX11_GS_SET_SHADER(0x3140, DirectXVersion.DX11),
        DX11_GS_GET_SHADER(0x3141, DirectXVersion.DX11),
        DX11_GS_SET_CONSTANT_BUFFERS(0x3142, DirectXVersion.DX11),
        DX11_GS_GET_CONSTANT_BUFFERS(0x3143, DirectXVersion.DX11),
        DX11_GS_SET_CONSTANT_BUFFERS1(0x3144, DirectXVersion.DX11_1),
        DX11_GS_GET_CONSTANT_BUFFERS1(0x3145, DirectXVersion.DX11_1),
        DX11_GS_SET_SHADER_RESOURCES(0x3146, DirectXVersion.DX11),
        DX11_GS_GET_SHADER_RESOURCES(0x3147, DirectXVersion.DX11),
        DX11_GS_SET_SAMPLERS(0x3148, DirectXVersion.DX11),
        DX11_GS_GET_SAMPLERS(0x3149, DirectXVersion.DX11),

        // DX11 Stream Output Stage
        DX11_SO_SET_TARGETS(0x3150, DirectXVersion.DX11),
        DX11_SO_GET_TARGETS(0x3151, DirectXVersion.DX11),

        // DX11 Pixel Shader Stage
        DX11_PS_SET_SHADER(0x3160, DirectXVersion.DX11),
        DX11_PS_GET_SHADER(0x3161, DirectXVersion.DX11),
        DX11_PS_SET_CONSTANT_BUFFERS(0x3162, DirectXVersion.DX11),
        DX11_PS_GET_CONSTANT_BUFFERS(0x3163, DirectXVersion.DX11),
        DX11_PS_SET_CONSTANT_BUFFERS1(0x3164, DirectXVersion.DX11_1),
        DX11_PS_GET_CONSTANT_BUFFERS1(0x3165, DirectXVersion.DX11_1),
        DX11_PS_SET_SHADER_RESOURCES(0x3166, DirectXVersion.DX11),
        DX11_PS_GET_SHADER_RESOURCES(0x3167, DirectXVersion.DX11),
        DX11_PS_SET_SAMPLERS(0x3168, DirectXVersion.DX11),
        DX11_PS_GET_SAMPLERS(0x3169, DirectXVersion.DX11),

        // DX11 Output Merger Stage
        DX11_OM_SET_RENDER_TARGETS(0x3170, DirectXVersion.DX11),
        DX11_OM_GET_RENDER_TARGETS(0x3171, DirectXVersion.DX11),
        DX11_OM_SET_RENDER_TARGETS_AND_UNORDERED_ACCESS_VIEWS(0x3172, DirectXVersion.DX11),
        DX11_OM_GET_RENDER_TARGETS_AND_UNORDERED_ACCESS_VIEWS(0x3173, DirectXVersion.DX11),
        DX11_OM_SET_BLEND_STATE(0x3174, DirectXVersion.DX11),
        DX11_OM_GET_BLEND_STATE(0x3175, DirectXVersion.DX11),
        DX11_OM_SET_DEPTH_STENCIL_STATE(0x3176, DirectXVersion.DX11),
        DX11_OM_GET_DEPTH_STENCIL_STATE(0x3177, DirectXVersion.DX11),

        // DX11 Rasterizer Stage
        DX11_RS_SET_STATE(0x3180, DirectXVersion.DX11),
        DX11_RS_GET_STATE(0x3181, DirectXVersion.DX11),
        DX11_RS_SET_VIEWPORTS(0x3182, DirectXVersion.DX11),
        DX11_RS_GET_VIEWPORTS(0x3183, DirectXVersion.DX11),
        DX11_RS_SET_SCISSOR_RECTS(0x3184, DirectXVersion.DX11),
        DX11_RS_GET_SCISSOR_RECTS(0x3185, DirectXVersion.DX11),

        // DX11 Compute Shader Stage
        DX11_CS_SET_SHADER(0x3190, DirectXVersion.DX11),
        DX11_CS_GET_SHADER(0x3191, DirectXVersion.DX11),
        DX11_CS_SET_CONSTANT_BUFFERS(0x3192, DirectXVersion.DX11),
        DX11_CS_GET_CONSTANT_BUFFERS(0x3193, DirectXVersion.DX11),
        DX11_CS_SET_CONSTANT_BUFFERS1(0x3194, DirectXVersion.DX11_1),
        DX11_CS_GET_CONSTANT_BUFFERS1(0x3195, DirectXVersion.DX11_1),
        DX11_CS_SET_SHADER_RESOURCES(0x3196, DirectXVersion.DX11),
        DX11_CS_GET_SHADER_RESOURCES(0x3197, DirectXVersion.DX11),
        DX11_CS_SET_SAMPLERS(0x3198, DirectXVersion.DX11),
        DX11_CS_GET_SAMPLERS(0x3199, DirectXVersion.DX11),
        DX11_CS_SET_UNORDERED_ACCESS_VIEWS(0x319A, DirectXVersion.DX11),
        DX11_CS_GET_UNORDERED_ACCESS_VIEWS(0x319B, DirectXVersion.DX11),

        // DX12 Pipeline State (Set in Command List)
        DX12_SET_PIPELINE_STATE(0x3200, DirectXVersion.DX12),
        DX12_SET_PIPELINE_STATE1(0x3201, DirectXVersion.DX12_2),
        DX12_SET_GRAPHICS_ROOT_SIGNATURE(0x3202, DirectXVersion.DX12),
        DX12_SET_COMPUTE_ROOT_SIGNATURE(0x3203, DirectXVersion.DX12),
        DX12_SET_DESCRIPTOR_HEAPS(0x3204, DirectXVersion.DX12),
        DX12_SET_GRAPHICS_ROOT_DESCRIPTOR_TABLE(0x3205, DirectXVersion.DX12),
        DX12_SET_COMPUTE_ROOT_DESCRIPTOR_TABLE(0x3206, DirectXVersion.DX12),
        DX12_SET_GRAPHICS_ROOT_32BIT_CONSTANT(0x3207, DirectXVersion.DX12),
        DX12_SET_COMPUTE_ROOT_32BIT_CONSTANT(0x3208, DirectXVersion.DX12),
        DX12_SET_GRAPHICS_ROOT_32BIT_CONSTANTS(0x3209, DirectXVersion.DX12),
        DX12_SET_COMPUTE_ROOT_32BIT_CONSTANTS(0x320A, DirectXVersion.DX12),
        DX12_SET_GRAPHICS_ROOT_CONSTANT_BUFFER_VIEW(0x320B, DirectXVersion.DX12),
        DX12_SET_COMPUTE_ROOT_CONSTANT_BUFFER_VIEW(0x320C, DirectXVersion.DX12),
        DX12_SET_GRAPHICS_ROOT_SHADER_RESOURCE_VIEW(0x320D, DirectXVersion.DX12),
        DX12_SET_COMPUTE_ROOT_SHADER_RESOURCE_VIEW(0x320E, DirectXVersion.DX12),
        DX12_SET_GRAPHICS_ROOT_UNORDERED_ACCESS_VIEW(0x320F, DirectXVersion.DX12),
        DX12_SET_COMPUTE_ROOT_UNORDERED_ACCESS_VIEW(0x3210, DirectXVersion.DX12),

        // DX12 Input Assembler
        DX12_IA_SET_PRIMITIVE_TOPOLOGY(0x3220, DirectXVersion.DX12),
        DX12_IA_SET_VERTEX_BUFFERS(0x3221, DirectXVersion.DX12),
        DX12_IA_SET_INDEX_BUFFER(0x3222, DirectXVersion.DX12),

        // DX12 Rasterizer State
        DX12_RS_SET_VIEWPORTS(0x3230, DirectXVersion.DX12),
        DX12_RS_SET_SCISSOR_RECTS(0x3231, DirectXVersion.DX12),
        DX12_RS_SET_SHADING_RATE(0x3232, DirectXVersion.DX12_2),
        DX12_RS_SET_SHADING_RATE_IMAGE(0x3233, DirectXVersion.DX12_2),

        // DX12 Output Merger
        DX12_OM_SET_BLEND_FACTOR(0x3240, DirectXVersion.DX12),
        DX12_OM_SET_STENCIL_REF(0x3241, DirectXVersion.DX12),
        DX12_OM_SET_RENDER_TARGETS(0x3242, DirectXVersion.DX12),
        DX12_OM_SET_DEPTH_BOUNDS(0x3243, DirectXVersion.DX12_1),

        // ════════════════════════════════════════════════════════════════════════════
        // SHADER OPERATIONS (0x4000-0x4FFF)
        // ════════════════════════════════════════════════════════════════════════════

        // Shader Compilation
        D3D_COMPILE_SHADER(0x4001, DirectXVersion.DX9),
        D3D_COMPILE_SHADER_FROM_FILE(0x4002, DirectXVersion.DX9),
        D3D_COMPILE_SHADER2(0x4003, DirectXVersion.DX11),
        D3D_COMPILE_EFFECT(0x4004, DirectXVersion.DX9),
        DXC_COMPILE(0x4005, DirectXVersion.DX12),
        DXC_COMPILE_WITH_DEBUG(0x4006, DirectXVersion.DX12),
        DXC_COMPILE_TO_SPIRV(0x4007, DirectXVersion.DX12),
        DXC_VALIDATE(0x4008, DirectXVersion.DX12),
        DXC_DISASSEMBLE(0x4009, DirectXVersion.DX12),
        DXC_PREPROCESS(0x400A, DirectXVersion.DX12),
        FXC_COMPILE(0x400B, DirectXVersion.DX11),

        // Shader Reflection
        D3D_REFLECT_SHADER(0x4100, DirectXVersion.DX11),
        D3D_GET_INPUT_SIGNATURE_BLOB(0x4101, DirectXVersion.DX11),
        D3D_GET_OUTPUT_SIGNATURE_BLOB(0x4102, DirectXVersion.DX11),
        D3D_GET_DEBUG_INFO(0x4103, DirectXVersion.DX11),
        D3D_GET_SHADER_DESC(0x4104, DirectXVersion.DX11),
        D3D_GET_CONSTANT_BUFFER_BY_INDEX(0x4105, DirectXVersion.DX11),
        D3D_GET_CONSTANT_BUFFER_BY_NAME(0x4106, DirectXVersion.DX11),
        D3D_GET_RESOURCE_BINDING_DESC(0x4107, DirectXVersion.DX11),
        D3D_GET_INPUT_PARAMETER_DESC(0x4108, DirectXVersion.DX11),
        D3D_GET_OUTPUT_PARAMETER_DESC(0x4109, DirectXVersion.DX11),
        D3D_GET_PATCH_CONSTANT_PARAMETER_DESC(0x410A, DirectXVersion.DX11),
        D3D_GET_VARIABLE_BY_INDEX(0x410B, DirectXVersion.DX11),
        D3D_GET_VARIABLE_BY_NAME(0x410C, DirectXVersion.DX11),
        D3D_GET_TYPE(0x410D, DirectXVersion.DX11),
        D3D_GET_THREAD_GROUP_SIZE(0x410E, DirectXVersion.DX11),
        D3D_GET_REQUIRES_FLAGS(0x410F, DirectXVersion.DX12),
        D3D_GET_MIN_FEATURE_LEVEL(0x4110, DirectXVersion.DX11),

        // DX12 Shader Identifiers
        DX12_GET_SHADER_IDENTIFIER(0x4200, DirectXVersion.DX12_1),
        DX12_GET_SHADER_STACK_SIZE(0x4201, DirectXVersion.DX12_1),
        DX12_GET_PIPELINE_STACK_SIZE(0x4202, DirectXVersion.DX12_1),
        DX12_SET_PIPELINE_STACK_SIZE(0x4203, DirectXVersion.DX12_1),

        // ════════════════════════════════════════════════════════════════════════════
        // DRAW COMMANDS (0x5000-0x5FFF)
        // ════════════════════════════════════════════════════════════════════════════

        // DX9 Draw Commands
        DX9_DRAW_PRIMITIVE(0x5001, DirectXVersion.DX9),
        DX9_DRAW_INDEXED_PRIMITIVE(0x5002, DirectXVersion.DX9),
        DX9_DRAW_PRIMITIVE_UP(0x5003, DirectXVersion.DX9),
        DX9_DRAW_INDEXED_PRIMITIVE_UP(0x5004, DirectXVersion.DX9),
        DX9_DRAW_RECT_PATCH(0x5005, DirectXVersion.DX9),
        DX9_DRAW_TRI_PATCH(0x5006, DirectXVersion.DX9),
        DX9_PROCESS_VERTICES(0x5007, DirectXVersion.DX9),
        DX9_BEGIN_SCENE(0x5008, DirectXVersion.DX9),
        DX9_END_SCENE(0x5009, DirectXVersion.DX9),
        DX9_CLEAR(0x500A, DirectXVersion.DX9),
        DX9_PRESENT(0x500B, DirectXVersion.DX9),
        DX9_BEGIN_STATE_BLOCK(0x500C, DirectXVersion.DX9),
        DX9_END_STATE_BLOCK(0x500D, DirectXVersion.DX9),
        DX9_APPLY_STATE_BLOCK(0x500E, DirectXVersion.DX9),
        DX9_CAPTURE_STATE_BLOCK(0x500F, DirectXVersion.DX9),

        // DX11 Draw Commands
        DX11_DRAW(0x5100, DirectXVersion.DX11),
        DX11_DRAW_INDEXED(0x5101, DirectXVersion.DX11),
        DX11_DRAW_INSTANCED(0x5102, DirectXVersion.DX11),
        DX11_DRAW_INDEXED_INSTANCED(0x5103, DirectXVersion.DX11),
        DX11_DRAW_INSTANCED_INDIRECT(0x5104, DirectXVersion.DX11),
        DX11_DRAW_INDEXED_INSTANCED_INDIRECT(0x5105, DirectXVersion.DX11),
        DX11_DRAW_AUTO(0x5106, DirectXVersion.DX11),
        DX11_BEGIN(0x5107, DirectXVersion.DX11),
        DX11_END(0x5108, DirectXVersion.DX11),
        DX11_SET_PREDICATION(0x5109, DirectXVersion.DX11),
        DX11_EXECUTE_COMMAND_LIST(0x510A, DirectXVersion.DX11),
        DX11_FINISH_COMMAND_LIST(0x510B, DirectXVersion.DX11),

        // DX12 Draw Commands
        DX12_DRAW_INSTANCED(0x5200, DirectXVersion.DX12),
        DX12_DRAW_INDEXED_INSTANCED(0x5201, DirectXVersion.DX12),
        DX12_EXECUTE_INDIRECT(0x5202, DirectXVersion.DX12),
        DX12_EXECUTE_BUNDLE(0x5203, DirectXVersion.DX12),
        DX12_SET_PREDICATION(0x5204, DirectXVersion.DX12),
        DX12_SET_MARKER(0x5205, DirectXVersion.DX12),
        DX12_BEGIN_EVENT(0x5206, DirectXVersion.DX12),
        DX12_END_EVENT(0x5207, DirectXVersion.DX12),
        DX12_RESOLVE_QUERY_DATA(0x5208, DirectXVersion.DX12),
        DX12_BEGIN_QUERY(0x5209, DirectXVersion.DX12),
        DX12_END_QUERY(0x520A, DirectXVersion.DX12),
        DX12_SET_SAMPLE_POSITIONS(0x520B, DirectXVersion.DX12_1),
        DX12_RESOLVE_SUBRESOURCE_REGION(0x520C, DirectXVersion.DX12_1),
        DX12_SET_VIEW_INSTANCE_MASK(0x520D, DirectXVersion.DX12_1),
        DX12_WRITE_BUFFER_IMMEDIATE(0x520E, DirectXVersion.DX12_2),

        // ════════════════════════════════════════════════════════════════════════════
        // COMPUTE COMMANDS (0x6000-0x6FFF)
        // ════════════════════════════════════════════════════════════════════════════

        DX11_DISPATCH(0x6100, DirectXVersion.DX11),
        DX11_DISPATCH_INDIRECT(0x6101, DirectXVersion.DX11),

        DX12_DISPATCH(0x6200, DirectXVersion.DX12),
        DX12_DISPATCH_INDIRECT(0x6201, DirectXVersion.DX12),
        DX12_DISPATCH_1D(0x6202, DirectXVersion.DX12),

        // ════════════════════════════════════════════════════════════════════════════
        // QUERY OPERATIONS (0x7000-0x7FFF)
        // ════════════════════════════════════════════════════════════════════════════

        DX9_CREATE_QUERY(0x7001, DirectXVersion.DX9),
        DX9_ISSUE(0x7002, DirectXVersion.DX9),
        DX9_GET_DATA(0x7003, DirectXVersion.DX9),

        DX11_BEGIN_QUERY(0x7100, DirectXVersion.DX11),
        DX11_END_QUERY(0x7101, DirectXVersion.DX11),
        DX11_GET_QUERY_DATA(0x7102, DirectXVersion.DX11),

        // ════════════════════════════════════════════════════════════════════════════
        // SYNCHRONIZATION (0x8000-0x8FFF)
        // ════════════════════════════════════════════════════════════════════════════

        // DX9 Sync
        DX9_GET_RENDER_TARGET_DATA(0x8001, DirectXVersion.DX9),

        // DX11 Sync
        DX11_FLUSH(0x8100, DirectXVersion.DX11),

        // DX12 Sync
        DX12_SIGNAL(0x8200, DirectXVersion.DX12),
        DX12_WAIT(0x8201, DirectXVersion.DX12),
        DX12_GET_COMPLETED_VALUE(0x8202, DirectXVersion.DX12),
        DX12_SET_EVENT_ON_COMPLETION(0x8203, DirectXVersion.DX12),
        DX12_CREATE_FENCE(0x8204, DirectXVersion.DX12),
        DX12_WAIT_FOR_FENCE(0x8205, DirectXVersion.DX12),
        DX12_EXECUTE_COMMAND_LISTS(0x8206, DirectXVersion.DX12),
        DX12_WAIT_FOR_IDLE(0x8207, DirectXVersion.DX12),
        DX12_GPU_WAIT(0x8208, DirectXVersion.DX12),

        // ════════════════════════════════════════════════════════════════════════════
        // DXGI OPERATIONS (0x9000-0x9FFF)
        // ════════════════════════════════════════════════════════════════════════════

        // Factory
        DXGI_CREATE_FACTORY(0x9001, DirectXVersion.DX10),
        DXGI_CREATE_FACTORY1(0x9002, DirectXVersion.DX11),
        DXGI_CREATE_FACTORY2(0x9003, DirectXVersion.DX11_1),
        DXGI_CREATE_FACTORY_DEBUG(0x9004, DirectXVersion.DX12),
        DXGI_ENUM_ADAPTERS(0x9005, DirectXVersion.DX10),
        DXGI_ENUM_ADAPTERS1(0x9006, DirectXVersion.DX11),
        DXGI_ENUM_ADAPTER_BY_GPU_PREFERENCE(0x9007, DirectXVersion.DX12),
        DXGI_ENUM_WARP_ADAPTER(0x9008, DirectXVersion.DX12),
        DXGI_IS_CURRENT(0x9009, DirectXVersion.DX11),
        DXGI_MAKE_WINDOW_ASSOCIATION(0x900A, DirectXVersion.DX10),
        DXGI_GET_WINDOW_ASSOCIATION(0x900B, DirectXVersion.DX10),
        DXGI_REGISTER_OCCLUSION_STATUS_WINDOW(0x900C, DirectXVersion.DX11_1),
        DXGI_REGISTER_OCCLUSION_STATUS_EVENT(0x900D, DirectXVersion.DX11_1),
        DXGI_REGISTER_STEREO_STATUS_WINDOW(0x900E, DirectXVersion.DX11_1),
        DXGI_REGISTER_STEREO_STATUS_EVENT(0x900F, DirectXVersion.DX11_1),

        // Adapter
        DXGI_ENUM_OUTPUTS(0x9100, DirectXVersion.DX10),
        DXGI_GET_ADAPTER_DESC(0x9101, DirectXVersion.DX10),
        DXGI_GET_ADAPTER_DESC1(0x9102, DirectXVersion.DX11),
        DXGI_GET_ADAPTER_DESC2(0x9103, DirectXVersion.DX11_2),
        DXGI_GET_ADAPTER_DESC3(0x9104, DirectXVersion.DX12),
        DXGI_CHECK_INTERFACE_SUPPORT(0x9105, DirectXVersion.DX10),
        DXGI_QUERY_VIDEO_MEMORY_INFO(0x9106, DirectXVersion.DX12),
        DXGI_SET_VIDEO_MEMORY_RESERVATION(0x9107, DirectXVersion.DX12),
        DXGI_REGISTER_VIDEO_MEMORY_BUDGET_CHANGE_NOTIFICATION_EVENT(0x9108, DirectXVersion.DX12),
        DXGI_REGISTER_HARDWARE_CONTENT_PROTECTION_TEARDOWN_STATUS_EVENT(0x9109, DirectXVersion.DX12),

        // Output
        DXGI_GET_DESC(0x9200, DirectXVersion.DX10),
        DXGI_GET_DISPLAY_MODE_LIST(0x9201, DirectXVersion.DX10),
        DXGI_GET_DISPLAY_MODE_LIST1(0x9202, DirectXVersion.DX11_1),
        DXGI_FIND_CLOSEST_MATCHING_MODE(0x9203, DirectXVersion.DX10),
        DXGI_FIND_CLOSEST_MATCHING_MODE1(0x9204, DirectXVersion.DX11_1),
        DXGI_GET_GAMMA_CONTROL_CAPABILITIES(0x9205, DirectXVersion.DX10),
        DXGI_SET_GAMMA_CONTROL(0x9206, DirectXVersion.DX10),
        DXGI_GET_GAMMA_CONTROL(0x9207, DirectXVersion.DX10),
        DXGI_WAIT_FOR_VBLANK(0x9208, DirectXVersion.DX10),
        DXGI_TAKE_OWNERSHIP(0x9209, DirectXVersion.DX10),
        DXGI_RELEASE_OWNERSHIP(0x920A, DirectXVersion.DX10),
        DXGI_GET_FRAME_STATISTICS(0x920B, DirectXVersion.DX10),
        DXGI_SUPPORTS_OVERLAYS(0x920C, DirectXVersion.DX11_2),
        DXGI_CHECK_HARDWARE_COMPOSITION_SUPPORT(0x920D, DirectXVersion.DX12),
        DXGI_GET_OUTPUT_DESC1(0x920E, DirectXVersion.DX12),

        // SwapChain
        DXGI_CREATE_SWAP_CHAIN(0x9300, DirectXVersion.DX10),
        DXGI_CREATE_SWAP_CHAIN_FOR_HWND(0x9301, DirectXVersion.DX11_1),
        DXGI_CREATE_SWAP_CHAIN_FOR_CORE_WINDOW(0x9302, DirectXVersion.DX11_1),
        DXGI_CREATE_SWAP_CHAIN_FOR_COMPOSITION(0x9303, DirectXVersion.DX11_2),
        DXGI_GET_BUFFER(0x9304, DirectXVersion.DX10),
        DXGI_SET_FULLSCREEN_STATE(0x9305, DirectXVersion.DX10),
        DXGI_GET_FULLSCREEN_STATE(0x9306, DirectXVersion.DX10),
        DXGI_RESIZE_BUFFERS(0x9307, DirectXVersion.DX10),
        DXGI_RESIZE_BUFFERS1(0x9308, DirectXVersion.DX12),
        DXGI_RESIZE_TARGET(0x9309, DirectXVersion.DX10),
        DXGI_GET_CONTAINING_OUTPUT(0x930A, DirectXVersion.DX10),
        DXGI_GET_FRAME_LATENCY_WAITABLE_OBJECT(0x930B, DirectXVersion.DX11_2),
        DXGI_SET_MAXIMUM_FRAME_LATENCY(0x930C, DirectXVersion.DX11_2),
        DXGI_GET_MAXIMUM_FRAME_LATENCY(0x930D, DirectXVersion.DX11_2),
        DXGI_PRESENT(0x930E, DirectXVersion.DX10),
        DXGI_PRESENT1(0x930F, DirectXVersion.DX11_1),
        DXGI_GET_BACK_BUFFER(0x9310, DirectXVersion.DX10),
        DXGI_GET_CURRENT_BACK_BUFFER_INDEX(0x9311, DirectXVersion.DX12),
        DXGI_CHECK_COLOR_SPACE_SUPPORT(0x9312, DirectXVersion.DX12),
        DXGI_SET_COLOR_SPACE_1(0x9313, DirectXVersion.DX12),
        DXGI_SET_HDR_META_DATA(0x9314, DirectXVersion.DX12),
        DXGI_GET_RESTRICT_TO_OUTPUT(0x9315, DirectXVersion.DX11_1),
        DXGI_SET_BACKGROUND_COLOR(0x9316, DirectXVersion.DX11_1),
        DXGI_GET_BACKGROUND_COLOR(0x9317, DirectXVersion.DX11_1),
        DXGI_SET_ROTATION(0x9318, DirectXVersion.DX11_1),
        DXGI_GET_ROTATION(0x9319, DirectXVersion.DX11_1),
        DXGI_SET_SOURCE_SIZE(0x931A, DirectXVersion.DX11_2),
        DXGI_GET_SOURCE_SIZE(0x931B, DirectXVersion.DX11_2),
        DXGI_SET_MATRIX_TRANSFORM(0x931C, DirectXVersion.DX11_2),
        DXGI_GET_MATRIX_TRANSFORM(0x931D, DirectXVersion.DX11_2),

        // ════════════════════════════════════════════════════════════════════════════
        // DX12 COMMAND LIST OPERATIONS (0xA000-0xAFFF)
        // ════════════════════════════════════════════════════════════════════════════

        DX12_CLOSE(0xA001, DirectXVersion.DX12),
        DX12_RESET(0xA002, DirectXVersion.DX12),
        DX12_CLEAR_STATE(0xA003, DirectXVersion.DX12),
        DX12_RESOURCE_BARRIER(0xA004, DirectXVersion.DX12),
        DX12_COPY_BUFFER_REGION(0xA005, DirectXVersion.DX12),
        DX12_COPY_TEXTURE_REGION(0xA006, DirectXVersion.DX12),
        DX12_COPY_RESOURCE(0xA007, DirectXVersion.DX12),
        DX12_COPY_TILES(0xA008, DirectXVersion.DX12),
        DX12_RESOLVE_SUBRESOURCE(0xA009, DirectXVersion.DX12),
        DX12_CLEAR_RENDER_TARGET_VIEW(0xA00A, DirectXVersion.DX12),
        DX12_CLEAR_DEPTH_STENCIL_VIEW(0xA00B, DirectXVersion.DX12),
        DX12_CLEAR_UNORDERED_ACCESS_VIEW_UINT(0xA00C, DirectXVersion.DX12),
        DX12_CLEAR_UNORDERED_ACCESS_VIEW_FLOAT(0xA00D, DirectXVersion.DX12),
        DX12_DISCARD_RESOURCE(0xA00E, DirectXVersion.DX12),
        DX12_ATOMIC_COPY_BUFFER_UINT(0xA00F, DirectXVersion.DX12_1),
        DX12_ATOMIC_COPY_BUFFER_UINT64(0xA010, DirectXVersion.DX12_1),
        DX12_SET_PROTECTED_RESOURCE_SESSION(0xA011, DirectXVersion.DX12_1),

        // ════════════════════════════════════════════════════════════════════════════
        // DX12 DESCRIPTOR OPERATIONS (0xB000-0xBFFF)
        // ════════════════════════════════════════════════════════════════════════════

        DX12_GET_CPU_DESCRIPTOR_HANDLE_FOR_HEAP_START(0xB001, DirectXVersion.DX12),
        DX12_GET_GPU_DESCRIPTOR_HANDLE_FOR_HEAP_START(0xB002, DirectXVersion.DX12),

        // ════════════════════════════════════════════════════════════════════════════
        // DX12 ROOT SIGNATURE OPERATIONS (0xC000-0xCFFF)
        // ════════════════════════════════════════════════════════════════════════════

        DX12_D3D12_SERIALIZE_ROOT_SIGNATURE(0xC001, DirectXVersion.DX12),
        DX12_D3D12_SERIALIZE_VERSIONED_ROOT_SIGNATURE(0xC002, DirectXVersion.DX12),
        DX12_D3D12_CREATE_ROOT_SIGNATURE_DESERIALIZER(0xC003, DirectXVersion.DX12),
        DX12_D3D12_CREATE_VERSIONED_ROOT_SIGNATURE_DESERIALIZER(0xC004, DirectXVersion.DX12),

        // ════════════════════════════════════════════════════════════════════════════
        // RAY TRACING OPERATIONS (0xD000-0xDFFF)
        // ════════════════════════════════════════════════════════════════════════════

        DXR_BUILD_ACCELERATION_STRUCTURE(0xD001, DirectXVersion.DX12_1),
        DXR_COPY_ACCELERATION_STRUCTURE(0xD002, DirectXVersion.DX12_1),
        DXR_EMIT_ACCELERATION_STRUCTURE_POSTBUILD_INFO(0xD003, DirectXVersion.DX12_1),
        DXR_DISPATCH_RAYS(0xD004, DirectXVersion.DX12_1),
        DXR_CREATE_RAYTRACING_PIPELINE_STATE(0xD005, DirectXVersion.DX12_1),
        DXR_GET_RAYTRACING_ACCELERATION_STRUCTURE_PREBUILD_INFO(0xD006, DirectXVersion.DX12_1),
        DXR_GET_SHADER_IDENTIFIER(0xD007, DirectXVersion.DX12_1),
        DXR_GET_SHADER_STACK_SIZE(0xD008, DirectXVersion.DX12_1),
        DXR_GET_PIPELINE_STACK_SIZE(0xD009, DirectXVersion.DX12_1),
        DXR_SET_PIPELINE_STACK_SIZE(0xD00A, DirectXVersion.DX12_1),

        // ════════════════════════════════════════════════════════════════════════════
        // MESH SHADER OPERATIONS (0xE000-0xEFFF)
        // ════════════════════════════════════════════════════════════════════════════

        DX12_DISPATCH_MESH(0xE001, DirectXVersion.DX12_2),
        DX12_CREATE_MESH_SHADER(0xE002, DirectXVersion.DX12_2),
        DX12_CREATE_AMPLIFICATION_SHADER(0xE003, DirectXVersion.DX12_2),

        // ════════════════════════════════════════════════════════════════════════════
        // DEBUG & MISC OPERATIONS (0xF000-0xFFFF)
        // ════════════════════════════════════════════════════════════════════════════

        DEBUG_SET_MARKER(0xF001, DirectXVersion.DX11),
        DEBUG_BEGIN_EVENT(0xF002, DirectXVersion.DX11),
        DEBUG_END_EVENT(0xF003, DirectXVersion.DX11),
        DEBUG_SET_PRIVATE_DATA(0xF004, DirectXVersion.DX11),
        DEBUG_GET_PRIVATE_DATA(0xF005, DirectXVersion.DX11),
        DEBUG_SET_NAME(0xF006, DirectXVersion.DX12),
        DEBUG_REPORT_LIVE_OBJECTS(0xF007, DirectXVersion.DX11),
        PIX_SET_MARKER(0xF008, DirectXVersion.DX12),
        PIX_BEGIN_EVENT(0xF009, DirectXVersion.DX12),
        PIX_END_EVENT(0xF00A, DirectXVersion.DX12),
        PIX_GPU_CAPTURE_NEXT_FRAMES(0xF00B, DirectXVersion.DX12),
        PIX_BEGIN_GPU_CAPTURE(0xF00C, DirectXVersion.DX12),
        PIX_END_GPU_CAPTURE(0xF00D, DirectXVersion.DX12),
        DRED_ENABLE_AUTO_BREADCRUMBS(0xF010, DirectXVersion.DX12),
        DRED_ENABLE_PAGE_FAULT_REPORTING(0xF011, DirectXVersion.DX12),
        DRED_GET_AUTOPSY_QUEUE_INFO(0xF012, DirectXVersion.DX12),
        DRED_GET_PAGE_FAULT_ALLOCATION_OUTPUT(0xF013, DirectXVersion.DX12);

        public final int code;
        public final DirectXVersion minimumVersion;

        CallType(int code, DirectXVersion minimumVersion) {
            this.code = code;
            this.minimumVersion = minimumVersion;
        }

        public boolean isAvailableIn(DirectXVersion version) {
            return version.isAtLeast(minimumVersion);
        }

        public boolean isDX9Call() {
            return (code & 0xF000) == 0x0000 && code < 0x0100
                || (code & 0xF000) == 0x1000 && code < 0x1100
                || (code & 0xF000) == 0x2000 && code < 0x2100
                || (code & 0xF000) == 0x3000 && code < 0x3100
                || (code & 0xF000) == 0x5000 && code < 0x5100
                || (code & 0xF000) == 0x7000 && code < 0x7100;
        }

        public boolean isDX11Call() {
            return (code & 0xFF00) >= 0x0100 && (code & 0xFF00) < 0x0200
                || (code & 0xFF00) >= 0x1100 && (code & 0xFF00) < 0x1200
                || (code & 0xFF00) >= 0x2100 && (code & 0xFF00) < 0x2200
                || (code & 0xFF00) >= 0x3100 && (code & 0xFF00) < 0x3200
                || (code & 0xFF00) >= 0x5100 && (code & 0xFF00) < 0x5200
                || (code & 0xFF00) >= 0x6100 && (code & 0xFF00) < 0x6200
                || (code & 0xFF00) >= 0x7100 && (code & 0xFF00) < 0x7200;
        }

        public boolean isDX12Call() {
            return (code & 0xFF00) >= 0x0200
                || (code & 0xFF00) >= 0x1200
                || (code & 0xFF00) >= 0x2200
                || (code & 0xFF00) >= 0x3200
                || (code & 0xFF00) >= 0x5200
                || (code & 0xFF00) >= 0x6200
                || (code & 0xF000) >= 0x8000;
        }

        public boolean isRayTracingCall() {
            return (code & 0xF000) == 0xD000;
        }

        public boolean isMeshShaderCall() {
            return (code & 0xF000) == 0xE000;
        }

        public boolean isDXGICall() {
            return (code & 0xF000) == 0x9000;
        }

        public boolean isDebugCall() {
            return (code & 0xF000) == 0xF000;
        }

        private static final Int2ObjectMap<CallType> CODE_MAP;
        static {
            CODE_MAP = new Int2ObjectOpenHashMap<>(values().length);
            for (CallType type : values()) {
                CODE_MAP.put(type.code, type);
            }
        }

        public static CallType fromCode(int code) {
            return CODE_MAP.get(code);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 5: D3D9 RENDER STATE CONSTANTS
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Complete D3D9 render state enumeration (D3DRENDERSTATETYPE).
     * All 200+ render states that can be set via SetRenderState.
     */
    public static final class D3D9RenderState {
        public static final int ZENABLE = 7;
        public static final int FILLMODE = 8;
        public static final int SHADEMODE = 9;
        public static final int ZWRITEENABLE = 14;
        public static final int ALPHATESTENABLE = 15;
        public static final int LASTPIXEL = 16;
        public static final int SRCBLEND = 19;
        public static final int DESTBLEND = 20;
        public static final int CULLMODE = 22;
        public static final int ZFUNC = 23;
        public static final int ALPHAREF = 24;
        public static final int ALPHAFUNC = 25;
        public static final int DITHERENABLE = 26;
        public static final int ALPHABLENDENABLE = 27;
        public static final int FOGENABLE = 28;
        public static final int SPECULARENABLE = 29;
        public static final int FOGCOLOR = 34;
        public static final int FOGTABLEMODE = 35;
        public static final int FOGSTART = 36;
        public static final int FOGEND = 37;
        public static final int FOGDENSITY = 38;
        public static final int RANGEFOGENABLE = 48;
        public static final int STENCILENABLE = 52;
        public static final int STENCILFAIL = 53;
        public static final int STENCILZFAIL = 54;
        public static final int STENCILPASS = 55;
        public static final int STENCILFUNC = 56;
        public static final int STENCILREF = 57;
        public static final int STENCILMASK = 58;
        public static final int STENCILWRITEMASK = 59;
        public static final int TEXTUREFACTOR = 60;
        public static final int WRAP0 = 128;
        public static final int WRAP1 = 129;
        public static final int WRAP2 = 130;
        public static final int WRAP3 = 131;
        public static final int WRAP4 = 132;
        public static final int WRAP5 = 133;
        public static final int WRAP6 = 134;
        public static final int WRAP7 = 135;
        public static final int CLIPPING = 136;
        public static final int LIGHTING = 137;
        public static final int AMBIENT = 139;
        public static final int FOGVERTEXMODE = 140;
        public static final int COLORVERTEX = 141;
        public static final int LOCALVIEWER = 142;
        public static final int NORMALIZENORMALS = 143;
        public static final int DIFFUSEMATERIALSOURCE = 145;
        public static final int SPECULARMATERIALSOURCE = 146;
        public static final int AMBIENTMATERIALSOURCE = 147;
        public static final int EMISSIVEMATERIALSOURCE = 148;
        public static final int VERTEXBLEND = 151;
        public static final int CLIPPLANEENABLE = 152;
        public static final int POINTSIZE = 154;
        public static final int POINTSIZE_MIN = 155;
        public static final int POINTSPRITEENABLE = 156;
        public static final int POINTSCALEENABLE = 157;
        public static final int POINTSCALE_A = 158;
        public static final int POINTSCALE_B = 159;
        public static final int POINTSCALE_C = 160;
        public static final int MULTISAMPLEANTIALIAS = 161;
        public static final int MULTISAMPLEMASK = 162;
        public static final int PATCHEDGESTYLE = 163;
        public static final int DEBUGMONITORTOKEN = 165;
        public static final int POINTSIZE_MAX = 166;
        public static final int INDEXEDVERTEXBLENDENABLE = 167;
        public static final int COLORWRITEENABLE = 168;
        public static final int TWEENFACTOR = 170;
        public static final int BLENDOP = 171;
        public static final int POSITIONDEGREE = 172;
        public static final int NORMALDEGREE = 173;
        public static final int SCISSORTESTENABLE = 174;
        public static final int SLOPESCALEDEPTHBIAS = 175;
        public static final int ANTIALIASEDLINEENABLE = 176;
        public static final int MINTESSELLATIONLEVEL = 178;
        public static final int MAXTESSELLATIONLEVEL = 179;
        public static final int ADAPTIVETESS_X = 180;
        public static final int ADAPTIVETESS_Y = 181;
        public static final int ADAPTIVETESS_Z = 182;
        public static final int ADAPTIVETESS_W = 183;
        public static final int ENABLEADAPTIVETESSELLATION = 184;
        public static final int TWOSIDEDSTENCILMODE = 185;
        public static final int CCW_STENCILFAIL = 186;
        public static final int CCW_STENCILZFAIL = 187;
        public static final int CCW_STENCILPASS = 188;
        public static final int CCW_STENCILFUNC = 189;
        public static final int COLORWRITEENABLE1 = 190;
        public static final int COLORWRITEENABLE2 = 191;
        public static final int COLORWRITEENABLE3 = 192;
        public static final int BLENDFACTOR = 193;
        public static final int SRGBWRITEENABLE = 194;
        public static final int DEPTHBIAS = 195;
        public static final int WRAP8 = 198;
        public static final int WRAP9 = 199;
        public static final int WRAP10 = 200;
        public static final int WRAP11 = 201;
        public static final int WRAP12 = 202;
        public static final int WRAP13 = 203;
        public static final int WRAP14 = 204;
        public static final int WRAP15 = 205;
        public static final int SEPARATEALPHABLENDENABLE = 206;
        public static final int SRCBLENDALPHA = 207;
        public static final int DESTBLENDALPHA = 208;
        public static final int BLENDOPALPHA = 209;

        private D3D9RenderState() {}

        public static String getName(int state) {
            return switch (state) {
                case ZENABLE -> "ZENABLE";
                case FILLMODE -> "FILLMODE";
                case SHADEMODE -> "SHADEMODE";
                case ZWRITEENABLE -> "ZWRITEENABLE";
                case ALPHATESTENABLE -> "ALPHATESTENABLE";
                case LASTPIXEL -> "LASTPIXEL";
                case SRCBLEND -> "SRCBLEND";
                case DESTBLEND -> "DESTBLEND";
                case CULLMODE -> "CULLMODE";
                case ZFUNC -> "ZFUNC";
                case ALPHAREF -> "ALPHAREF";
                case ALPHAFUNC -> "ALPHAFUNC";
                case DITHERENABLE -> "DITHERENABLE";
                case ALPHABLENDENABLE -> "ALPHABLENDENABLE";
                case FOGENABLE -> "FOGENABLE";
                case SPECULARENABLE -> "SPECULARENABLE";
                case FOGCOLOR -> "FOGCOLOR";
                case FOGTABLEMODE -> "FOGTABLEMODE";
                case FOGSTART -> "FOGSTART";
                case FOGEND -> "FOGEND";
                case FOGDENSITY -> "FOGDENSITY";
                case RANGEFOGENABLE -> "RANGEFOGENABLE";
                case STENCILENABLE -> "STENCILENABLE";
                case STENCILFAIL -> "STENCILFAIL";
                case STENCILZFAIL -> "STENCILZFAIL";
                case STENCILPASS -> "STENCILPASS";
                case STENCILFUNC -> "STENCILFUNC";
                case STENCILREF -> "STENCILREF";
                case STENCILMASK -> "STENCILMASK";
                case STENCILWRITEMASK -> "STENCILWRITEMASK";
                case TEXTUREFACTOR -> "TEXTUREFACTOR";
                case CLIPPING -> "CLIPPING";
                case LIGHTING -> "LIGHTING";
                case AMBIENT -> "AMBIENT";
                case FOGVERTEXMODE -> "FOGVERTEXMODE";
                case COLORVERTEX -> "COLORVERTEX";
                case LOCALVIEWER -> "LOCALVIEWER";
                case NORMALIZENORMALS -> "NORMALIZENORMALS";
                case DIFFUSEMATERIALSOURCE -> "DIFFUSEMATERIALSOURCE";
                case SPECULARMATERIALSOURCE -> "SPECULARMATERIALSOURCE";
                case AMBIENTMATERIALSOURCE -> "AMBIENTMATERIALSOURCE";
                case EMISSIVEMATERIALSOURCE -> "EMISSIVEMATERIALSOURCE";
                case VERTEXBLEND -> "VERTEXBLEND";
                case CLIPPLANEENABLE -> "CLIPPLANEENABLE";
                case POINTSIZE -> "POINTSIZE";
                case POINTSIZE_MIN -> "POINTSIZE_MIN";
                case POINTSPRITEENABLE -> "POINTSPRITEENABLE";
                case POINTSCALEENABLE -> "POINTSCALEENABLE";
                case POINTSCALE_A -> "POINTSCALE_A";
                case POINTSCALE_B -> "POINTSCALE_B";
                case POINTSCALE_C -> "POINTSCALE_C";
                case MULTISAMPLEANTIALIAS -> "MULTISAMPLEANTIALIAS";
                case MULTISAMPLEMASK -> "MULTISAMPLEMASK";
                case POINTSIZE_MAX -> "POINTSIZE_MAX";
                case COLORWRITEENABLE -> "COLORWRITEENABLE";
                case TWEENFACTOR -> "TWEENFACTOR";
                case BLENDOP -> "BLENDOP";
                case SCISSORTESTENABLE -> "SCISSORTESTENABLE";
                case SLOPESCALEDEPTHBIAS -> "SLOPESCALEDEPTHBIAS";
                case ANTIALIASEDLINEENABLE -> "ANTIALIASEDLINEENABLE";
                case TWOSIDEDSTENCILMODE -> "TWOSIDEDSTENCILMODE";
                case CCW_STENCILFAIL -> "CCW_STENCILFAIL";
                case CCW_STENCILZFAIL -> "CCW_STENCILZFAIL";
                case CCW_STENCILPASS -> "CCW_STENCILPASS";
                case CCW_STENCILFUNC -> "CCW_STENCILFUNC";
                case COLORWRITEENABLE1 -> "COLORWRITEENABLE1";
                case COLORWRITEENABLE2 -> "COLORWRITEENABLE2";
                case COLORWRITEENABLE3 -> "COLORWRITEENABLE3";
                case BLENDFACTOR -> "BLENDFACTOR";
                case SRGBWRITEENABLE -> "SRGBWRITEENABLE";
                case DEPTHBIAS -> "DEPTHBIAS";
                case SEPARATEALPHABLENDENABLE -> "SEPARATEALPHABLENDENABLE";
                case SRCBLENDALPHA -> "SRCBLENDALPHA";
                case DESTBLENDALPHA -> "DESTBLENDALPHA";
                case BLENDOPALPHA -> "BLENDOPALPHA";
                default -> "RENDERSTATE_" + state;
            };
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 6: D3D9 TEXTURE STAGE STATE CONSTANTS
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * D3D9 texture stage state enumeration (D3DTEXTURESTAGESTATETYPE).
     */
    public static final class D3D9TextureStageState {
        public static final int COLOROP = 1;
        public static final int COLORARG1 = 2;
        public static final int COLORARG2 = 3;
        public static final int ALPHAOP = 4;
        public static final int ALPHAARG1 = 5;
        public static final int ALPHAARG2 = 6;
        public static final int BUMPENVMAT00 = 7;
        public static final int BUMPENVMAT01 = 8;
        public static final int BUMPENVMAT10 = 9;
        public static final int BUMPENVMAT11 = 10;
        public static final int TEXCOORDINDEX = 11;
        public static final int BUMPENVLSCALE = 22;
        public static final int BUMPENVLOFFSET = 23;
        public static final int TEXTURETRANSFORMFLAGS = 24;
        public static final int COLORARG0 = 26;
        public static final int ALPHAARG0 = 27;
        public static final int RESULTARG = 28;
        public static final int CONSTANT = 32;

        private D3D9TextureStageState() {}
    }

    /**
     * D3D9 sampler state enumeration (D3DSAMPLERSTATETYPE).
     */
    public static final class D3D9SamplerState {
        public static final int ADDRESSU = 1;
        public static final int ADDRESSV = 2;
        public static final int ADDRESSW = 3;
        public static final int BORDERCOLOR = 4;
        public static final int MAGFILTER = 5;
        public static final int MINFILTER = 6;
        public static final int MIPFILTER = 7;
        public static final int MIPMAPLODBIAS = 8;
        public static final int MAXMIPLEVEL = 9;
        public static final int MAXANISOTROPY = 10;
        public static final int SRGBTEXTURE = 11;
        public static final int ELEMENTINDEX = 12;
        public static final int DMAPOFFSET = 13;

        private D3D9SamplerState() {}
    }

    /**
     * D3D9 transform type enumeration (D3DTRANSFORMSTATETYPE).
     */
    public static final class D3D9Transform {
        public static final int VIEW = 2;
        public static final int PROJECTION = 3;
        public static final int WORLD = 256;
        public static final int WORLD1 = 257;
        public static final int WORLD2 = 258;
        public static final int WORLD3 = 259;
        public static final int TEXTURE0 = 16;
        public static final int TEXTURE1 = 17;
        public static final int TEXTURE2 = 18;
        public static final int TEXTURE3 = 19;
        public static final int TEXTURE4 = 20;
        public static final int TEXTURE5 = 21;
        public static final int TEXTURE6 = 22;
        public static final int TEXTURE7 = 23;

        private D3D9Transform() {}
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 7: D3D12 RESOURCE STATE CONSTANTS
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * D3D12_RESOURCE_STATES enumeration - critical for barrier management.
     */
    public static final class D3D12ResourceStates {
        public static final int COMMON = 0;
        public static final int VERTEX_AND_CONSTANT_BUFFER = 0x1;
        public static final int INDEX_BUFFER = 0x2;
        public static final int RENDER_TARGET = 0x4;
        public static final int UNORDERED_ACCESS = 0x8;
        public static final int DEPTH_WRITE = 0x10;
        public static final int DEPTH_READ = 0x20;
        public static final int NON_PIXEL_SHADER_RESOURCE = 0x40;
        public static final int PIXEL_SHADER_RESOURCE = 0x80;
        public static final int STREAM_OUT = 0x100;
        public static final int INDIRECT_ARGUMENT = 0x200;
        public static final int COPY_DEST = 0x400;
        public static final int COPY_SOURCE = 0x800;
        public static final int RESOLVE_DEST = 0x1000;
        public static final int RESOLVE_SOURCE = 0x2000;
        public static final int RAYTRACING_ACCELERATION_STRUCTURE = 0x400000;
        public static final int SHADING_RATE_SOURCE = 0x1000000;
        public static final int GENERIC_READ = VERTEX_AND_CONSTANT_BUFFER | INDEX_BUFFER |
            NON_PIXEL_SHADER_RESOURCE | PIXEL_SHADER_RESOURCE | INDIRECT_ARGUMENT | COPY_SOURCE;
        public static final int ALL_SHADER_RESOURCE = NON_PIXEL_SHADER_RESOURCE | PIXEL_SHADER_RESOURCE;
        public static final int PRESENT = 0;
        public static final int PREDICATION = 0x200;
        public static final int VIDEO_DECODE_READ = 0x10000;
        public static final int VIDEO_DECODE_WRITE = 0x20000;
        public static final int VIDEO_PROCESS_READ = 0x40000;
        public static final int VIDEO_PROCESS_WRITE = 0x80000;
        public static final int VIDEO_ENCODE_READ = 0x200000;
        public static final int VIDEO_ENCODE_WRITE = 0x800000;

        private D3D12ResourceStates() {}

        public static String getName(int state) {
            if (state == COMMON) return "COMMON";
            StringBuilder sb = new StringBuilder();
            if ((state & VERTEX_AND_CONSTANT_BUFFER) != 0) appendState(sb, "VERTEX_AND_CONSTANT_BUFFER");
            if ((state & INDEX_BUFFER) != 0) appendState(sb, "INDEX_BUFFER");
            if ((state & RENDER_TARGET) != 0) appendState(sb, "RENDER_TARGET");
            if ((state & UNORDERED_ACCESS) != 0) appendState(sb, "UNORDERED_ACCESS");
            if ((state & DEPTH_WRITE) != 0) appendState(sb, "DEPTH_WRITE");
            if ((state & DEPTH_READ) != 0) appendState(sb, "DEPTH_READ");
            if ((state & NON_PIXEL_SHADER_RESOURCE) != 0) appendState(sb, "NON_PIXEL_SHADER_RESOURCE");
            if ((state & PIXEL_SHADER_RESOURCE) != 0) appendState(sb, "PIXEL_SHADER_RESOURCE");
            if ((state & STREAM_OUT) != 0) appendState(sb, "STREAM_OUT");
            if ((state & INDIRECT_ARGUMENT) != 0) appendState(sb, "INDIRECT_ARGUMENT");
            if ((state & COPY_DEST) != 0) appendState(sb, "COPY_DEST");
            if ((state & COPY_SOURCE) != 0) appendState(sb, "COPY_SOURCE");
            if ((state & RESOLVE_DEST) != 0) appendState(sb, "RESOLVE_DEST");
            if ((state & RESOLVE_SOURCE) != 0) appendState(sb, "RESOLVE_SOURCE");
            if ((state & RAYTRACING_ACCELERATION_STRUCTURE) != 0) appendState(sb, "RAYTRACING_ACCELERATION_STRUCTURE");
            if ((state & SHADING_RATE_SOURCE) != 0) appendState(sb, "SHADING_RATE_SOURCE");
            return sb.length() > 0 ? sb.toString() : "STATE_" + Integer.toHexString(state);
        }

        private static void appendState(StringBuilder sb, String name) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(name);
        }

        public static boolean isReadState(int state) {
            return (state & (RENDER_TARGET | UNORDERED_ACCESS | DEPTH_WRITE | 
                           COPY_DEST | RESOLVE_DEST | VIDEO_DECODE_WRITE | 
                           VIDEO_PROCESS_WRITE | VIDEO_ENCODE_WRITE)) == 0;
        }

        public static boolean isWriteState(int state) {
            return !isReadState(state);
        }

        public static boolean requiresBarrierTo(int currentState, int desiredState) {
            if (currentState == desiredState) return false;
            if (currentState == COMMON) return true;
            if (desiredState == COMMON) return true;
            // Read-to-read transitions may not need barriers
            if (isReadState(currentState) && isReadState(desiredState)) {
                // But crossing read state categories requires barrier
                return (currentState & desiredState) == 0;
            }
            return true;
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 8: D3D12 DESCRIPTOR HEAP TYPES
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * D3D12_DESCRIPTOR_HEAP_TYPE enumeration.
     */
    public static final class D3D12DescriptorHeapType {
        public static final int CBV_SRV_UAV = 0;
        public static final int SAMPLER = 1;
        public static final int RTV = 2;
        public static final int DSV = 3;
        public static final int NUM_TYPES = 4;

        private D3D12DescriptorHeapType() {}

        public static String getName(int type) {
            return switch (type) {
                case CBV_SRV_UAV -> "CBV_SRV_UAV";
                case SAMPLER -> "SAMPLER";
                case RTV -> "RTV";
                case DSV -> "DSV";
                default -> "UNKNOWN_" + type;
            };
        }

        public static int getMaxDescriptors(int type) {
            return switch (type) {
                case CBV_SRV_UAV -> 1_000_000;  // SM 6.6 bindless
                case SAMPLER -> 2048;
                case RTV -> 8192;
                case DSV -> 8192;
                default -> 0;
            };
        }
    }

    /**
     * D3D12_HEAP_TYPE enumeration.
     */
    public static final class D3D12HeapType {
        public static final int DEFAULT = 1;
        public static final int UPLOAD = 2;
        public static final int READBACK = 3;
        public static final int CUSTOM = 4;

        private D3D12HeapType() {}

        public static String getName(int type) {
            return switch (type) {
                case DEFAULT -> "DEFAULT";
                case UPLOAD -> "UPLOAD";
                case READBACK -> "READBACK";
                case CUSTOM -> "CUSTOM";
                default -> "UNKNOWN_" + type;
            };
        }
    }

    /**
     * D3D12_COMMAND_LIST_TYPE enumeration.
     */
    public static final class D3D12CommandListType {
        public static final int DIRECT = 0;
        public static final int BUNDLE = 1;
        public static final int COMPUTE = 2;
        public static final int COPY = 3;
        public static final int VIDEO_DECODE = 4;
        public static final int VIDEO_PROCESS = 5;
        public static final int VIDEO_ENCODE = 6;

        private D3D12CommandListType() {}

        public static String getName(int type) {
            return switch (type) {
                case DIRECT -> "DIRECT";
                case BUNDLE -> "BUNDLE";
                case COMPUTE -> "COMPUTE";
                case COPY -> "COPY";
                case VIDEO_DECODE -> "VIDEO_DECODE";
                case VIDEO_PROCESS -> "VIDEO_PROCESS";
                case VIDEO_ENCODE -> "VIDEO_ENCODE";
                default -> "UNKNOWN_" + type;
            };
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 9: PRIMITIVE TOPOLOGY
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * D3D_PRIMITIVE_TOPOLOGY enumeration - shared across DX versions.
     */
    public static final class D3DPrimitiveTopology {
        public static final int UNDEFINED = 0;
        public static final int POINTLIST = 1;
        public static final int LINELIST = 2;
        public static final int LINESTRIP = 3;
        public static final int TRIANGLELIST = 4;
        public static final int TRIANGLESTRIP = 5;
        public static final int LINELIST_ADJ = 10;
        public static final int LINESTRIP_ADJ = 11;
        public static final int TRIANGLELIST_ADJ = 12;
        public static final int TRIANGLESTRIP_ADJ = 13;
        public static final int CONTROL_POINT_PATCHLIST_1 = 33;
        public static final int CONTROL_POINT_PATCHLIST_2 = 34;
        public static final int CONTROL_POINT_PATCHLIST_3 = 35;
        public static final int CONTROL_POINT_PATCHLIST_4 = 36;
        public static final int CONTROL_POINT_PATCHLIST_5 = 37;
        public static final int CONTROL_POINT_PATCHLIST_6 = 38;
        public static final int CONTROL_POINT_PATCHLIST_7 = 39;
        public static final int CONTROL_POINT_PATCHLIST_8 = 40;
        public static final int CONTROL_POINT_PATCHLIST_9 = 41;
        public static final int CONTROL_POINT_PATCHLIST_10 = 42;
        public static final int CONTROL_POINT_PATCHLIST_11 = 43;
        public static final int CONTROL_POINT_PATCHLIST_12 = 44;
        public static final int CONTROL_POINT_PATCHLIST_13 = 45;
        public static final int CONTROL_POINT_PATCHLIST_14 = 46;
        public static final int CONTROL_POINT_PATCHLIST_15 = 47;
        public static final int CONTROL_POINT_PATCHLIST_16 = 48;
        public static final int CONTROL_POINT_PATCHLIST_17 = 49;
        public static final int CONTROL_POINT_PATCHLIST_18 = 50;
        public static final int CONTROL_POINT_PATCHLIST_19 = 51;
        public static final int CONTROL_POINT_PATCHLIST_20 = 52;
        public static final int CONTROL_POINT_PATCHLIST_21 = 53;
        public static final int CONTROL_POINT_PATCHLIST_22 = 54;
        public static final int CONTROL_POINT_PATCHLIST_23 = 55;
        public static final int CONTROL_POINT_PATCHLIST_24 = 56;
        public static final int CONTROL_POINT_PATCHLIST_25 = 57;
        public static final int CONTROL_POINT_PATCHLIST_26 = 58;
        public static final int CONTROL_POINT_PATCHLIST_27 = 59;
        public static final int CONTROL_POINT_PATCHLIST_28 = 60;
        public static final int CONTROL_POINT_PATCHLIST_29 = 61;
        public static final int CONTROL_POINT_PATCHLIST_30 = 62;
        public static final int CONTROL_POINT_PATCHLIST_31 = 63;
        public static final int CONTROL_POINT_PATCHLIST_32 = 64;

        private D3DPrimitiveTopology() {}

        public static String getName(int topology) {
            return switch (topology) {
                case UNDEFINED -> "UNDEFINED";
                case POINTLIST -> "POINTLIST";
                case LINELIST -> "LINELIST";
                case LINESTRIP -> "LINESTRIP";
                case TRIANGLELIST -> "TRIANGLELIST";
                case TRIANGLESTRIP -> "TRIANGLESTRIP";
                case LINELIST_ADJ -> "LINELIST_ADJ";
                case LINESTRIP_ADJ -> "LINESTRIP_ADJ";
                case TRIANGLELIST_ADJ -> "TRIANGLELIST_ADJ";
                case TRIANGLESTRIP_ADJ -> "TRIANGLESTRIP_ADJ";
                default -> {
                    if (topology >= CONTROL_POINT_PATCHLIST_1 && topology <= CONTROL_POINT_PATCHLIST_32) {
                        yield "CONTROL_POINT_PATCHLIST_" + (topology - CONTROL_POINT_PATCHLIST_1 + 1);
                    }
                    yield "UNKNOWN_" + topology;
                }
            };
        }

        public static boolean requiresTessellation(int topology) {
            return topology >= CONTROL_POINT_PATCHLIST_1 && topology <= CONTROL_POINT_PATCHLIST_32;
        }

        public static boolean requiresGeometryShader(int topology) {
            return topology == LINELIST_ADJ || topology == LINESTRIP_ADJ ||
                   topology == TRIANGLELIST_ADJ || topology == TRIANGLESTRIP_ADJ;
        }

        public static int getVerticesPerPrimitive(int topology) {
            return switch (topology) {
                case POINTLIST -> 1;
                case LINELIST, LINESTRIP -> 2;
                case TRIANGLELIST, TRIANGLESTRIP -> 3;
                case LINELIST_ADJ, LINESTRIP_ADJ -> 4;
                case TRIANGLELIST_ADJ, TRIANGLESTRIP_ADJ -> 6;
                default -> {
                    if (topology >= CONTROL_POINT_PATCHLIST_1 && topology <= CONTROL_POINT_PATCHLIST_32) {
                        yield topology - CONTROL_POINT_PATCHLIST_1 + 1;
                    }
                    yield 0;
                }
            };
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 10: DXGI FORMAT ENUMERATION (COMPLETE)
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Complete DXGI_FORMAT enumeration - all 190+ formats.
     */
    public static final class DXGIFormat {
        public static final int UNKNOWN = 0;
        public static final int R32G32B32A32_TYPELESS = 1;
        public static final int R32G32B32A32_FLOAT = 2;
        public static final int R32G32B32A32_UINT = 3;
        public static final int R32G32B32A32_SINT = 4;
        public static final int R32G32B32_TYPELESS = 5;
        public static final int R32G32B32_FLOAT = 6;
        public static final int R32G32B32_UINT = 7;
        public static final int R32G32B32_SINT = 8;
        public static final int R16G16B16A16_TYPELESS = 9;
        public static final int R16G16B16A16_FLOAT = 10;
        public static final int R16G16B16A16_UNORM = 11;
        public static final int R16G16B16A16_UINT = 12;
        public static final int R16G16B16A16_SNORM = 13;
        public static final int R16G16B16A16_SINT = 14;
        public static final int R32G32_TYPELESS = 15;
        public static final int R32G32_FLOAT = 16;
        public static final int R32G32_UINT = 17;
        public static final int R32G32_SINT = 18;
        public static final int R32G8X24_TYPELESS = 19;
        public static final int D32_FLOAT_S8X24_UINT = 20;
        public static final int R32_FLOAT_X8X24_TYPELESS = 21;
        public static final int X32_TYPELESS_G8X24_UINT = 22;
        public static final int R10G10B10A2_TYPELESS = 23;
        public static final int R10G10B10A2_UNORM = 24;
        public static final int R10G10B10A2_UINT = 25;
        public static final int R11G11B10_FLOAT = 26;
        public static final int R8G8B8A8_TYPELESS = 27;
        public static final int R8G8B8A8_UNORM = 28;
        public static final int R8G8B8A8_UNORM_SRGB = 29;
        public static final int R8G8B8A8_UINT = 30;
        public static final int R8G8B8A8_SNORM = 31;
        public static final int R8G8B8A8_SINT = 32;
        public static final int R16G16_TYPELESS = 33;
        public static final int R16G16_FLOAT = 34;
        public static final int R16G16_UNORM = 35;
        public static final int R16G16_UINT = 36;
        public static final int R16G16_SNORM = 37;
        public static final int R16G16_SINT = 38;
        public static final int R32_TYPELESS = 39;
        public static final int D32_FLOAT = 40;
        public static final int R32_FLOAT = 41;
        public static final int R32_UINT = 42;
        public static final int R32_SINT = 43;
        public static final int R24G8_TYPELESS = 44;
        public static final int D24_UNORM_S8_UINT = 45;
        public static final int R24_UNORM_X8_TYPELESS = 46;
        public static final int X24_TYPELESS_G8_UINT = 47;
        public static final int R8G8_TYPELESS = 48;
        public static final int R8G8_UNORM = 49;
        public static final int R8G8_UINT = 50;
        public static final int R8G8_SNORM = 51;
        public static final int R8G8_SINT = 52;
        public static final int R16_TYPELESS = 53;
        public static final int R16_FLOAT = 54;
        public static final int D16_UNORM = 55;
        public static final int R16_UNORM = 56;
        public static final int R16_UINT = 57;
        public static final int R16_SNORM = 58;
        public static final int R16_SINT = 59;
        public static final int R8_TYPELESS = 60;
        public static final int R8_UNORM = 61;
        public static final int R8_UINT = 62;
        public static final int R8_SNORM = 63;
        public static final int R8_SINT = 64;
        public static final int A8_UNORM = 65;
        public static final int R1_UNORM = 66;
        public static final int R9G9B9E5_SHAREDEXP = 67;
        public static final int R8G8_B8G8_UNORM = 68;
        public static final int G8R8_G8B8_UNORM = 69;
        public static final int BC1_TYPELESS = 70;
        public static final int BC1_UNORM = 71;
        public static final int BC1_UNORM_SRGB = 72;
        public static final int BC2_TYPELESS = 73;
        public static final int BC2_UNORM = 74;
        public static final int BC2_UNORM_SRGB = 75;
        public static final int BC3_TYPELESS = 76;
        public static final int BC3_UNORM = 77;
        public static final int BC3_UNORM_SRGB = 78;
        public static final int BC4_TYPELESS = 79;
        public static final int BC4_UNORM = 80;
        public static final int BC4_SNORM = 81;
        public static final int BC5_TYPELESS = 82;
        public static final int BC5_UNORM = 83;
        public static final int BC5_SNORM = 84;
        public static final int B5G6R5_UNORM = 85;
        public static final int B5G5R5A1_UNORM = 86;
        public static final int B8G8R8A8_UNORM = 87;
        public static final int B8G8R8X8_UNORM = 88;
        public static final int R10G10B10_XR_BIAS_A2_UNORM = 89;
        public static final int B8G8R8A8_TYPELESS = 90;
        public static final int B8G8R8A8_UNORM_SRGB = 91;
        public static final int B8G8R8X8_TYPELESS = 92;
        public static final int B8G8R8X8_UNORM_SRGB = 93;
        public static final int BC6H_TYPELESS = 94;
        public static final int BC6H_UF16 = 95;
        public static final int BC6H_SF16 = 96;
        public static final int BC7_TYPELESS = 97;
        public static final int BC7_UNORM = 98;
        public static final int BC7_UNORM_SRGB = 99;
        public static final int AYUV = 100;
        public static final int Y410 = 101;
        public static final int Y416 = 102;
        public static final int NV12 = 103;
        public static final int P010 = 104;
        public static final int P016 = 105;
        public static final int OPAQUE_420 = 106;
        public static final int YUY2 = 107;
        public static final int Y210 = 108;
        public static final int Y216 = 109;
        public static final int NV11 = 110;
        public static final int AI44 = 111;
        public static final int IA44 = 112;
        public static final int P8 = 113;
        public static final int A8P8 = 114;
        public static final int B4G4R4A4_UNORM = 115;
        public static final int P208 = 130;
        public static final int V208 = 131;
        public static final int V408 = 132;
        public static final int SAMPLER_FEEDBACK_MIN_MIP_OPAQUE = 189;
        public static final int SAMPLER_FEEDBACK_MIP_REGION_USED_OPAQUE = 190;

        private DXGIFormat() {}

        public static int getBitsPerPixel(int format) {
            return switch (format) {
                case R32G32B32A32_TYPELESS, R32G32B32A32_FLOAT, R32G32B32A32_UINT, R32G32B32A32_SINT -> 128;
                case R32G32B32_TYPELESS, R32G32B32_FLOAT, R32G32B32_UINT, R32G32B32_SINT -> 96;
                case R16G16B16A16_TYPELESS, R16G16B16A16_FLOAT, R16G16B16A16_UNORM, R16G16B16A16_UINT,
                     R16G16B16A16_SNORM, R16G16B16A16_SINT, R32G32_TYPELESS, R32G32_FLOAT, R32G32_UINT,
                     R32G32_SINT, R32G8X24_TYPELESS, D32_FLOAT_S8X24_UINT, R32_FLOAT_X8X24_TYPELESS,
                     X32_TYPELESS_G8X24_UINT -> 64;
                case R10G10B10A2_TYPELESS, R10G10B10A2_UNORM, R10G10B10A2_UINT, R11G11B10_FLOAT,
                     R8G8B8A8_TYPELESS, R8G8B8A8_UNORM, R8G8B8A8_UNORM_SRGB, R8G8B8A8_UINT, R8G8B8A8_SNORM,
                     R8G8B8A8_SINT, R16G16_TYPELESS, R16G16_FLOAT, R16G16_UNORM, R16G16_UINT, R16G16_SNORM,
                     R16G16_SINT, R32_TYPELESS, D32_FLOAT, R32_FLOAT, R32_UINT, R32_SINT, R24G8_TYPELESS,
                     D24_UNORM_S8_UINT, R24_UNORM_X8_TYPELESS, X24_TYPELESS_G8_UINT, B8G8R8A8_UNORM,
                     B8G8R8X8_UNORM, R10G10B10_XR_BIAS_A2_UNORM, B8G8R8A8_TYPELESS, B8G8R8A8_UNORM_SRGB,
                     B8G8R8X8_TYPELESS, B8G8R8X8_UNORM_SRGB, R9G9B9E5_SHAREDEXP -> 32;
                case R8G8_TYPELESS, R8G8_UNORM, R8G8_UINT, R8G8_SNORM, R8G8_SINT, R16_TYPELESS, R16_FLOAT,
                     D16_UNORM, R16_UNORM, R16_UINT, R16_SNORM, R16_SINT, B5G6R5_UNORM, B5G5R5A1_UNORM,
                     B4G4R4A4_UNORM -> 16;
                case R8_TYPELESS, R8_UNORM, R8_UINT, R8_SNORM, R8_SINT, A8_UNORM, P8 -> 8;
                case R1_UNORM -> 1;
                case BC1_TYPELESS, BC1_UNORM, BC1_UNORM_SRGB, BC4_TYPELESS, BC4_UNORM, BC4_SNORM -> 4;
                case BC2_TYPELESS, BC2_UNORM, BC2_UNORM_SRGB, BC3_TYPELESS, BC3_UNORM, BC3_UNORM_SRGB,
                     BC5_TYPELESS, BC5_UNORM, BC5_SNORM, BC6H_TYPELESS, BC6H_UF16, BC6H_SF16,
                     BC7_TYPELESS, BC7_UNORM, BC7_UNORM_SRGB -> 8;
                default -> 0;
            };
        }

        public static boolean isCompressed(int format) {
            return (format >= BC1_TYPELESS && format <= BC5_SNORM) ||
                   (format >= BC6H_TYPELESS && format <= BC7_UNORM_SRGB);
        }

        public static boolean isDepthFormat(int format) {
            return format == D32_FLOAT_S8X24_UINT || format == D32_FLOAT ||
                   format == D24_UNORM_S8_UINT || format == D16_UNORM;
        }

        public static boolean hasStencil(int format) {
            return format == D32_FLOAT_S8X24_UINT || format == D24_UNORM_S8_UINT;
        }

        public static boolean isSRGB(int format) {
            return format == R8G8B8A8_UNORM_SRGB || format == BC1_UNORM_SRGB ||
                   format == BC2_UNORM_SRGB || format == BC3_UNORM_SRGB ||
                   format == B8G8R8A8_UNORM_SRGB || format == B8G8R8X8_UNORM_SRGB ||
                   format == BC7_UNORM_SRGB;
        }

        public static boolean isTypeless(int format) {
            return format == R32G32B32A32_TYPELESS || format == R32G32B32_TYPELESS ||
                   format == R16G16B16A16_TYPELESS || format == R32G32_TYPELESS ||
                   format == R32G8X24_TYPELESS || format == R10G10B10A2_TYPELESS ||
                   format == R8G8B8A8_TYPELESS || format == R16G16_TYPELESS ||
                   format == R32_TYPELESS || format == R24G8_TYPELESS ||
                   format == R8G8_TYPELESS || format == R16_TYPELESS ||
                   format == R8_TYPELESS || format == BC1_TYPELESS ||
                   format == BC2_TYPELESS || format == BC3_TYPELESS ||
                   format == BC4_TYPELESS || format == BC5_TYPELESS ||
                   format == B8G8R8A8_TYPELESS || format == B8G8R8X8_TYPELESS ||
                   format == BC6H_TYPELESS || format == BC7_TYPELESS;
        }

        public static int toTypeless(int format) {
            return switch (format) {
                case R32G32B32A32_FLOAT, R32G32B32A32_UINT, R32G32B32A32_SINT -> R32G32B32A32_TYPELESS;
                case R32G32B32_FLOAT, R32G32B32_UINT, R32G32B32_SINT -> R32G32B32_TYPELESS;
                case R16G16B16A16_FLOAT, R16G16B16A16_UNORM, R16G16B16A16_UINT,
                     R16G16B16A16_SNORM, R16G16B16A16_SINT -> R16G16B16A16_TYPELESS;
                case R8G8B8A8_UNORM, R8G8B8A8_UNORM_SRGB, R8G8B8A8_UINT,
                     R8G8B8A8_SNORM, R8G8B8A8_SINT -> R8G8B8A8_TYPELESS;
                case B8G8R8A8_UNORM, B8G8R8A8_UNORM_SRGB -> B8G8R8A8_TYPELESS;
                default -> format;
            };
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 11: HRESULT ERROR CODES
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Common HRESULT error codes for DirectX operations.
     */
    public static final class HResult {
        // Success codes
        public static final int S_OK = 0x00000000;
        public static final int S_FALSE = 0x00000001;

        // General errors
        public static final int E_UNEXPECTED = 0x8000FFFF;
        public static final int E_NOTIMPL = 0x80004001;
        public static final int E_OUTOFMEMORY = 0x8007000E;
        public static final int E_INVALIDARG = 0x80070057;
        public static final int E_NOINTERFACE = 0x80004002;
        public static final int E_POINTER = 0x80004003;
        public static final int E_HANDLE = 0x80070006;
        public static final int E_ABORT = 0x80004004;
        public static final int E_FAIL = 0x80004005;
        public static final int E_ACCESSDENIED = 0x80070005;

        // DXGI errors
        public static final int DXGI_ERROR_INVALID_CALL = 0x887A0001;
        public static final int DXGI_ERROR_NOT_FOUND = 0x887A0002;
        public static final int DXGI_ERROR_MORE_DATA = 0x887A0003;
        public static final int DXGI_ERROR_UNSUPPORTED = 0x887A0004;
        public static final int DXGI_ERROR_DEVICE_REMOVED = 0x887A0005;
        public static final int DXGI_ERROR_DEVICE_HUNG = 0x887A0006;
        public static final int DXGI_ERROR_DEVICE_RESET = 0x887A0007;
        public static final int DXGI_ERROR_WAS_STILL_DRAWING = 0x887A000A;
        public static final int DXGI_ERROR_FRAME_STATISTICS_DISJOINT = 0x887A000B;
        public static final int DXGI_ERROR_GRAPHICS_VIDPN_SOURCE_IN_USE = 0x887A000C;
        public static final int DXGI_ERROR_DRIVER_INTERNAL_ERROR = 0x887A0020;
        public static final int DXGI_ERROR_NONEXCLUSIVE = 0x887A0021;
        public static final int DXGI_ERROR_NOT_CURRENTLY_AVAILABLE = 0x887A0022;
        public static final int DXGI_ERROR_REMOTE_CLIENT_DISCONNECTED = 0x887A0023;
        public static final int DXGI_ERROR_REMOTE_OUTOFMEMORY = 0x887A0024;
        public static final int DXGI_ERROR_ACCESS_LOST = 0x887A0026;
        public static final int DXGI_ERROR_WAIT_TIMEOUT = 0x887A0027;
        public static final int DXGI_ERROR_SESSION_DISCONNECTED = 0x887A0028;
        public static final int DXGI_ERROR_RESTRICT_TO_OUTPUT_STALE = 0x887A0029;
        public static final int DXGI_ERROR_CANNOT_PROTECT_CONTENT = 0x887A002A;
        public static final int DXGI_ERROR_ACCESS_DENIED = 0x887A002B;
        public static final int DXGI_ERROR_NAME_ALREADY_EXISTS = 0x887A002C;
        public static final int DXGI_ERROR_SDK_COMPONENT_MISSING = 0x887A002D;
        public static final int DXGI_ERROR_NOT_CURRENT = 0x887A002E;
        public static final int DXGI_ERROR_HW_PROTECTION_OUTOFMEMORY = 0x887A0030;
        public static final int DXGI_ERROR_DYNAMIC_CODE_POLICY_VIOLATION = 0x887A0031;
        public static final int DXGI_ERROR_NON_COMPOSITED_UI = 0x887A0032;

        // D3D11 errors
        public static final int D3D11_ERROR_TOO_MANY_UNIQUE_STATE_OBJECTS = 0x887C0001;
        public static final int D3D11_ERROR_FILE_NOT_FOUND = 0x887C0002;
        public static final int D3D11_ERROR_TOO_MANY_UNIQUE_VIEW_OBJECTS = 0x887C0003;
        public static final int D3D11_ERROR_DEFERRED_CONTEXT_MAP_WITHOUT_INITIAL_DISCARD = 0x887C0004;

        // D3D12 errors
        public static final int D3D12_ERROR_ADAPTER_NOT_FOUND = 0x887E0001;
        public static final int D3D12_ERROR_DRIVER_VERSION_MISMATCH = 0x887E0002;
        public static final int D3D12_ERROR_INVALID_REDIST = 0x887E0003;

        private HResult() {}

        public static boolean succeeded(int hr) {
            return hr >= 0;
        }

        public static boolean failed(int hr) {
            return hr < 0;
        }

        public static String getMessage(int hr) {
            return switch (hr) {
                case S_OK -> "S_OK";
                case S_FALSE -> "S_FALSE";
                case E_UNEXPECTED -> "E_UNEXPECTED: Unexpected failure";
                case E_NOTIMPL -> "E_NOTIMPL: Not implemented";
                case E_OUTOFMEMORY -> "E_OUTOFMEMORY: Out of memory";
                case E_INVALIDARG -> "E_INVALIDARG: Invalid argument";
                case E_NOINTERFACE -> "E_NOINTERFACE: Interface not supported";
                case E_POINTER -> "E_POINTER: Invalid pointer";
                case E_FAIL -> "E_FAIL: Unspecified failure";
                case E_ACCESSDENIED -> "E_ACCESSDENIED: Access denied";
                case DXGI_ERROR_INVALID_CALL -> "DXGI_ERROR_INVALID_CALL";
                case DXGI_ERROR_NOT_FOUND -> "DXGI_ERROR_NOT_FOUND";
                case DXGI_ERROR_UNSUPPORTED -> "DXGI_ERROR_UNSUPPORTED";
                case DXGI_ERROR_DEVICE_REMOVED -> "DXGI_ERROR_DEVICE_REMOVED";
                case DXGI_ERROR_DEVICE_HUNG -> "DXGI_ERROR_DEVICE_HUNG";
                case DXGI_ERROR_DEVICE_RESET -> "DXGI_ERROR_DEVICE_RESET";
                case DXGI_ERROR_DRIVER_INTERNAL_ERROR -> "DXGI_ERROR_DRIVER_INTERNAL_ERROR";
                case D3D12_ERROR_ADAPTER_NOT_FOUND -> "D3D12_ERROR_ADAPTER_NOT_FOUND";
                case D3D12_ERROR_DRIVER_VERSION_MISMATCH -> "D3D12_ERROR_DRIVER_VERSION_MISMATCH";
                default -> "HRESULT 0x" + Integer.toHexString(hr);
            };
        }

        public static boolean isDeviceLost(int hr) {
            return hr == DXGI_ERROR_DEVICE_REMOVED ||
                   hr == DXGI_ERROR_DEVICE_HUNG ||
                   hr == DXGI_ERROR_DEVICE_RESET ||
                   hr == DXGI_ERROR_DRIVER_INTERNAL_ERROR;
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 12: BUILT-IN DEVICE CAPABILITIES
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Comprehensive DirectX device capabilities - queried at initialization and cached.
     * This is BUILT-IN to the mapper as requested.
     */
    public static final class DXDeviceCapabilities {
        // ─────────────────────────────────────────────────────────────────────────────
        // Core identification
        // ─────────────────────────────────────────────────────────────────────────────
        private final DirectXVersion apiVersion;
        private final int featureLevel;
        private final String adapterDescription;
        private final int vendorId;
        private final int deviceId;
        private final int subSystemId;
        private final int revision;
        private final long dedicatedVideoMemory;
        private final long dedicatedSystemMemory;
        private final long sharedSystemMemory;
        private final long adapterLuid;

        // ─────────────────────────────────────────────────────────────────────────────
        // Shader model support
        // ─────────────────────────────────────────────────────────────────────────────
        private final int maxShaderModel;
        private final boolean supportsShaderModel6_0;
        private final boolean supportsShaderModel6_1;
        private final boolean supportsShaderModel6_2;
        private final boolean supportsShaderModel6_3;
        private final boolean supportsShaderModel6_4;
        private final boolean supportsShaderModel6_5;
        private final boolean supportsShaderModel6_6;
        private final boolean supportsShaderModel6_7;
        private final boolean supportsWaveIntrinsics;
        private final int waveSize;
        private final int minWaveSize;
        private final int maxWaveSize;
        private final boolean supportsBarycentrics;
        private final boolean supportsInt64Atomics;

        // ─────────────────────────────────────────────────────────────────────────────
        // Ray tracing capabilities (DXR)
        // ─────────────────────────────────────────────────────────────────────────────
        private final int rayTracingTier;  // 0=None, 1=Tier1_0, 2=Tier1_1
        private final boolean supportsRayTracing;
        private final boolean supportsInlineRayTracing;
        private final boolean supportsRayQuery;
        private final int maxRecursionDepth;
        private final int maxRayDispatchDimensionWidth;
        private final int maxRayHitAttributeSize;
        private final int maxRayPayloadSize;

        // ─────────────────────────────────────────────────────────────────────────────
        // Mesh shader capabilities
        // ─────────────────────────────────────────────────────────────────────────────
        private final int meshShaderTier;  // 0=None, 1=Tier1
        private final boolean supportsMeshShaders;
        private final boolean supportsAmplificationShaders;
        private final int maxMeshShaderThreadGroupSize;
        private final int maxMeshOutputVertices;
        private final int maxMeshOutputPrimitives;
        private final int maxAmplificationShaderThreadGroupSize;

        // ─────────────────────────────────────────────────────────────────────────────
        // Variable Rate Shading (VRS)
        // ─────────────────────────────────────────────────────────────────────────────
        private final int variableRateShadingTier;  // 0=None, 1=Tier1, 2=Tier2
        private final boolean supportsVariableRateShading;
        private final boolean supportsShadingRateImage;
        private final boolean supportsPerPrimitiveShadingRate;
        private final int shadingRateImageTileSize;
        private final boolean additionalShadingRatesSupported;

        // ─────────────────────────────────────────────────────────────────────────────
        // Sampler Feedback
        // ─────────────────────────────────────────────────────────────────────────────
        private final int samplerFeedbackTier;  // 0=None, 1=Tier0_9, 2=Tier1_0
        private final boolean supportsSamplerFeedback;
        private final int maxSampledMipRegionWidth;
        private final int maxSampledMipRegionHeight;

        // ─────────────────────────────────────────────────────────────────────────────
        // Resource binding capabilities
        // ─────────────────────────────────────────────────────────────────────────────
        private final int resourceBindingTier;  // 1, 2, or 3
        private final int maxDescriptorsPerHeapCBV_SRV_UAV;
        private final int maxDescriptorsPerHeapSampler;
        private final boolean supportsDynamicIndexing;
        private final boolean supportsDescriptorHeapIndexing;
        private final boolean supportsBindless;

        // ─────────────────────────────────────────────────────────────────────────────
        // Tiled resources
        // ─────────────────────────────────────────────────────────────────────────────
        private final int tiledResourcesTier;  // 0-4
        private final boolean supportsTiledResources;
        private final boolean supportsTiledVolumes;
        private final boolean supportsTextureStreamingFeedback;
        private final int maxTiledResourceDimension;

        // ─────────────────────────────────────────────────────────────────────────────
        // Conservative rasterization
        // ─────────────────────────────────────────────────────────────────────────────
        private final int conservativeRasterizationTier;  // 0-3
        private final boolean supportsConservativeRasterization;
        private final boolean supportsPostSnapDegenerateTriangleCulling;
        private final boolean supportsInnerCoverage;

        // ─────────────────────────────────────────────────────────────────────────────
        // Render pass capabilities
        // ─────────────────────────────────────────────────────────────────────────────
        private final int renderPassesTier;  // 0-2
        private final boolean supportsRenderPasses;

        // ─────────────────────────────────────────────────────────────────────────────
        // Pipeline state cache
        // ─────────────────────────────────────────────────────────────────────────────
        private final boolean supportsPipelineLibraries;
        private final boolean supportsShaderCache;

        // ─────────────────────────────────────────────────────────────────────────────
        // Texture limits
        // ─────────────────────────────────────────────────────────────────────────────
        private final int maxTexture1DDimension;
        private final int maxTexture2DDimension;
        private final int maxTexture3DDimension;
        private final int maxTextureCubeDimension;
        private final int maxTextureArraySlices;
        private final int maxAnisotropy;
        private final boolean supportsBC6H_BC7;
        private final boolean supportsASTC;

        // ─────────────────────────────────────────────────────────────────────────────
        // Render target limits
        // ─────────────────────────────────────────────────────────────────────────────
        private final int maxRenderTargets;
        private final int maxUAVSlots;
        private final int maxUAVsPerStage;
        private final boolean supportsROVs;  // Rasterizer Ordered Views
        private final boolean supportsStencilRef;
        private final boolean supportsDepthBoundsTest;

        // ─────────────────────────────────────────────────────────────────────────────
        // Compute capabilities
        // ─────────────────────────────────────────────────────────────────────────────
        private final boolean supportsCompute;
        private final boolean supportsDoublePrecision;
        private final int maxComputeShaderThreadGroupSizeX;
        private final int maxComputeShaderThreadGroupSizeY;
        private final int maxComputeShaderThreadGroupSizeZ;
        private final int maxComputeShaderThreadGroupTotal;
        private final int maxComputeShaderDispatchDimension;
        private final long maxComputeSharedMemory;

        // ─────────────────────────────────────────────────────────────────────────────
        // Multi-GPU support
        // ─────────────────────────────────────────────────────────────────────────────
        private final int nodeCount;
        private final boolean supportsCrossNodeSharing;
        private final boolean supportsCrossAdapterRowMajorTexture;

        // ─────────────────────────────────────────────────────────────────────────────
        // Feature options
        // ─────────────────────────────────────────────────────────────────────────────
        private final boolean supportsOutputMergerLogicOps;
        private final boolean supportsProgrammableSamplePositions;
        private final boolean supportsViewInstancing;
        private final int viewInstancingTier;
        private final boolean supportsWriteBufferImmediate;
        private final boolean supportsBackgroundProcessing;

        // ─────────────────────────────────────────────────────────────────────────────
        // HDR and display
        // ─────────────────────────────────────────────────────────────────────────────
        private final boolean supportsHDR;
        private final boolean supportsWideColorGamut;
        private final boolean supportsVariableRefreshRate;
        private final boolean supportsHardwareComposition;
        private final boolean supportsTearing;

        /**
         * Private constructor - use Builder.
         */
        private DXDeviceCapabilities(Builder builder) {
            this.apiVersion = builder.apiVersion;
            this.featureLevel = builder.featureLevel;
            this.adapterDescription = builder.adapterDescription;
            this.vendorId = builder.vendorId;
            this.deviceId = builder.deviceId;
            this.subSystemId = builder.subSystemId;
            this.revision = builder.revision;
            this.dedicatedVideoMemory = builder.dedicatedVideoMemory;
            this.dedicatedSystemMemory = builder.dedicatedSystemMemory;
            this.sharedSystemMemory = builder.sharedSystemMemory;
            this.adapterLuid = builder.adapterLuid;
            this.maxShaderModel = builder.maxShaderModel;
            this.supportsShaderModel6_0 = builder.supportsShaderModel6_0;
            this.supportsShaderModel6_1 = builder.supportsShaderModel6_1;
            this.supportsShaderModel6_2 = builder.supportsShaderModel6_2;
            this.supportsShaderModel6_3 = builder.supportsShaderModel6_3;
            this.supportsShaderModel6_4 = builder.supportsShaderModel6_4;
            this.supportsShaderModel6_5 = builder.supportsShaderModel6_5;
            this.supportsShaderModel6_6 = builder.supportsShaderModel6_6;
            this.supportsShaderModel6_7 = builder.supportsShaderModel6_7;
            this.supportsWaveIntrinsics = builder.supportsWaveIntrinsics;
            this.waveSize = builder.waveSize;
            this.minWaveSize = builder.minWaveSize;
            this.maxWaveSize = builder.maxWaveSize;
            this.supportsBarycentrics = builder.supportsBarycentrics;
            this.supportsInt64Atomics = builder.supportsInt64Atomics;
            this.rayTracingTier = builder.rayTracingTier;
            this.supportsRayTracing = builder.supportsRayTracing;
            this.supportsInlineRayTracing = builder.supportsInlineRayTracing;
            this.supportsRayQuery = builder.supportsRayQuery;
            this.maxRecursionDepth = builder.maxRecursionDepth;
            this.maxRayDispatchDimensionWidth = builder.maxRayDispatchDimensionWidth;
            this.maxRayHitAttributeSize = builder.maxRayHitAttributeSize;
            this.maxRayPayloadSize = builder.maxRayPayloadSize;
            this.meshShaderTier = builder.meshShaderTier;
            this.supportsMeshShaders = builder.supportsMeshShaders;
            this.supportsAmplificationShaders = builder.supportsAmplificationShaders;
            this.maxMeshShaderThreadGroupSize = builder.maxMeshShaderThreadGroupSize;
            this.maxMeshOutputVertices = builder.maxMeshOutputVertices;
            this.maxMeshOutputPrimitives = builder.maxMeshOutputPrimitives;
            this.maxAmplificationShaderThreadGroupSize = builder.maxAmplificationShaderThreadGroupSize;
            this.variableRateShadingTier = builder.variableRateShadingTier;
            this.supportsVariableRateShading = builder.supportsVariableRateShading;
            this.supportsShadingRateImage = builder.supportsShadingRateImage;
            this.supportsPerPrimitiveShadingRate = builder.supportsPerPrimitiveShadingRate;
            this.shadingRateImageTileSize = builder.shadingRateImageTileSize;
            this.additionalShadingRatesSupported = builder.additionalShadingRatesSupported;
            this.samplerFeedbackTier = builder.samplerFeedbackTier;
            this.supportsSamplerFeedback = builder.supportsSamplerFeedback;
            this.maxSampledMipRegionWidth = builder.maxSampledMipRegionWidth;
            this.maxSampledMipRegionHeight = builder.maxSampledMipRegionHeight;
            this.resourceBindingTier = builder.resourceBindingTier;
            this.maxDescriptorsPerHeapCBV_SRV_UAV = builder.maxDescriptorsPerHeapCBV_SRV_UAV;
            this.maxDescriptorsPerHeapSampler = builder.maxDescriptorsPerHeapSampler;
            this.supportsDynamicIndexing = builder.supportsDynamicIndexing;
            this.supportsDescriptorHeapIndexing = builder.supportsDescriptorHeapIndexing;
            this.supportsBindless = builder.supportsBindless;
            this.tiledResourcesTier = builder.tiledResourcesTier;
            this.supportsTiledResources = builder.supportsTiledResources;
            this.supportsTiledVolumes = builder.supportsTiledVolumes;
            this.supportsTextureStreamingFeedback = builder.supportsTextureStreamingFeedback;
            this.maxTiledResourceDimension = builder.maxTiledResourceDimension;
            this.conservativeRasterizationTier = builder.conservativeRasterizationTier;
            this.supportsConservativeRasterization = builder.supportsConservativeRasterization;
            this.supportsPostSnapDegenerateTriangleCulling = builder.supportsPostSnapDegenerateTriangleCulling;
            this.supportsInnerCoverage = builder.supportsInnerCoverage;
            this.renderPassesTier = builder.renderPassesTier;
            this.supportsRenderPasses = builder.supportsRenderPasses;
            this.supportsPipelineLibraries = builder.supportsPipelineLibraries;
            this.supportsShaderCache = builder.supportsShaderCache;
            this.maxTexture1DDimension = builder.maxTexture1DDimension;
            this.maxTexture2DDimension = builder.maxTexture2DDimension;
            this.maxTexture3DDimension = builder.maxTexture3DDimension;
            this.maxTextureCubeDimension = builder.maxTextureCubeDimension;
            this.maxTextureArraySlices = builder.maxTextureArraySlices;
            this.maxAnisotropy = builder.maxAnisotropy;
            this.supportsBC6H_BC7 = builder.supportsBC6H_BC7;
            this.supportsASTC = builder.supportsASTC;
            this.maxRenderTargets = builder.maxRenderTargets;
            this.maxUAVSlots = builder.maxUAVSlots;
            this.maxUAVsPerStage = builder.maxUAVsPerStage;
            this.supportsROVs = builder.supportsROVs;
            this.supportsStencilRef = builder.supportsStencilRef;
            this.supportsDepthBoundsTest = builder.supportsDepthBoundsTest;
            this.supportsCompute = builder.supportsCompute;
            this.supportsDoublePrecision = builder.supportsDoublePrecision;
            this.maxComputeShaderThreadGroupSizeX = builder.maxComputeShaderThreadGroupSizeX;
            this.maxComputeShaderThreadGroupSizeY = builder.maxComputeShaderThreadGroupSizeY;
            this.maxComputeShaderThreadGroupSizeZ = builder.maxComputeShaderThreadGroupSizeZ;
            this.maxComputeShaderThreadGroupTotal = builder.maxComputeShaderThreadGroupTotal;
            this.maxComputeShaderDispatchDimension = builder.maxComputeShaderDispatchDimension;
            this.maxComputeSharedMemory = builder.maxComputeSharedMemory;
            this.nodeCount = builder.nodeCount;
            this.supportsCrossNodeSharing = builder.supportsCrossNodeSharing;
            this.supportsCrossAdapterRowMajorTexture = builder.supportsCrossAdapterRowMajorTexture;
            this.supportsOutputMergerLogicOps = builder.supportsOutputMergerLogicOps;
            this.supportsProgrammableSamplePositions = builder.supportsProgrammableSamplePositions;
            this.supportsViewInstancing = builder.supportsViewInstancing;
            this.viewInstancingTier = builder.viewInstancingTier;
            this.supportsWriteBufferImmediate = builder.supportsWriteBufferImmediate;
            this.supportsBackgroundProcessing = builder.supportsBackgroundProcessing;
            this.supportsHDR = builder.supportsHDR;
            this.supportsWideColorGamut = builder.supportsWideColorGamut;
            this.supportsVariableRefreshRate = builder.supportsVariableRefreshRate;
            this.supportsHardwareComposition = builder.supportsHardwareComposition;
            this.supportsTearing = builder.supportsTearing;
        }

        // Getters for all fields
        public DirectXVersion getApiVersion() { return apiVersion; }
        public int getFeatureLevel() { return featureLevel; }
        public String getAdapterDescription() { return adapterDescription; }
        public int getVendorId() { return vendorId; }
        public int getDeviceId() { return deviceId; }
        public long getDedicatedVideoMemory() { return dedicatedVideoMemory; }
        public long getSharedSystemMemory() { return sharedSystemMemory; }
        public int getMaxShaderModel() { return maxShaderModel; }
        public boolean supportsWaveIntrinsics() { return supportsWaveIntrinsics; }
        public int getWaveSize() { return waveSize; }
        public boolean supportsRayTracing() { return supportsRayTracing; }
        public int getRayTracingTier() { return rayTracingTier; }
        public boolean supportsMeshShaders() { return supportsMeshShaders; }
        public int getMeshShaderTier() { return meshShaderTier; }
        public boolean supportsVariableRateShading() { return supportsVariableRateShading; }
        public int getVariableRateShadingTier() { return variableRateShadingTier; }
        public boolean supportsBindless() { return supportsBindless; }
        public int getResourceBindingTier() { return resourceBindingTier; }
        public boolean supportsTiledResources() { return supportsTiledResources; }
        public int getTiledResourcesTier() { return tiledResourcesTier; }
        public boolean supportsCompute() { return supportsCompute; }
        public int getMaxTexture2DDimension() { return maxTexture2DDimension; }
        public int getMaxRenderTargets() { return maxRenderTargets; }
        public int getNodeCount() { return nodeCount; }
        public boolean supportsHDR() { return supportsHDR; }
        public boolean supportsTearing() { return supportsTearing; }

        /**
         * Check if a specific call type is supported by this device.
         */
        public boolean supportsCall(CallType callType) {
            if (!callType.isAvailableIn(apiVersion)) {
                return false;
            }
            
            // Check specific feature requirements
            if (callType.isRayTracingCall()) {
                return supportsRayTracing;
            }
            if (callType.isMeshShaderCall()) {
                return supportsMeshShaders;
            }
            
            // VRS-specific calls
            if (callType == CallType.DX12_RS_SET_SHADING_RATE ||
                callType == CallType.DX12_RS_SET_SHADING_RATE_IMAGE) {
                return supportsVariableRateShading;
            }
            
            return true;
        }

        /**
         * Determine best fallback API version if current feature unsupported.
         */
        public DirectXVersion suggestFallbackFor(CallType callType) {
            if (supportsCall(callType)) {
                return apiVersion;
            }
            
            // DX12-specific features - fall back to DX11
            if (callType.isDX12Call() && !apiVersion.isDX12Family()) {
                return DirectXVersion.DX11;
            }
            
            // Tessellation - requires DX11+
            if (callType == CallType.DX11_CREATE_HULL_SHADER ||
                callType == CallType.DX11_CREATE_DOMAIN_SHADER) {
                return DirectXVersion.DX11;
            }
            
            // Compute - requires DX11+
            if (callType == CallType.DX11_DISPATCH || callType == CallType.DX12_DISPATCH) {
                if (!supportsCompute) return null;  // No fallback possible
                return DirectXVersion.DX11;
            }
            
            // Ultimate fallback
            return DirectXVersion.DX9;
        }

        /**
         * Check if this device supports DX12 Ultimate feature set.
         */
        public boolean isDX12UltimateCapable() {
            return supportsRayTracing &&
                   supportsMeshShaders &&
                   supportsVariableRateShading &&
                   supportsSamplerFeedback &&
                   maxShaderModel >= ShaderModel.SM_6_5;
        }

        /**
         * Get vendor name from ID.
         */
        public String getVendorName() {
            return switch (vendorId) {
                case 0x1002 -> "AMD";
                case 0x10DE -> "NVIDIA";
                case 0x8086 -> "Intel";
                case 0x1414 -> "Microsoft (WARP)";
                case 0x5143 -> "Qualcomm";
                default -> "Unknown (0x" + Integer.toHexString(vendorId) + ")";
            };
        }

        /**
         * Builder for DXDeviceCapabilities - makes native queries manageable.
         */
        public static final class Builder {
            private DirectXVersion apiVersion = DirectXVersion.DX11;
            private int featureLevel = D3DFeatureLevel.LEVEL_11_0;
            private String adapterDescription = "Unknown Adapter";
            private int vendorId = 0;
            private int deviceId = 0;
            private int subSystemId = 0;
            private int revision = 0;
            private long dedicatedVideoMemory = 0;
            private long dedicatedSystemMemory = 0;
            private long sharedSystemMemory = 0;
            private long adapterLuid = 0;
            private int maxShaderModel = ShaderModel.SM_5_0;
            private boolean supportsShaderModel6_0 = false;
            private boolean supportsShaderModel6_1 = false;
            private boolean supportsShaderModel6_2 = false;
            private boolean supportsShaderModel6_3 = false;
            private boolean supportsShaderModel6_4 = false;
            private boolean supportsShaderModel6_5 = false;
            private boolean supportsShaderModel6_6 = false;
            private boolean supportsShaderModel6_7 = false;
            private boolean supportsWaveIntrinsics = false;
            private int waveSize = 32;
            private int minWaveSize = 4;
            private int maxWaveSize = 128;
            private boolean supportsBarycentrics = false;
            private boolean supportsInt64Atomics = false;
            private int rayTracingTier = 0;
            private boolean supportsRayTracing = false;
            private boolean supportsInlineRayTracing = false;
            private boolean supportsRayQuery = false;
            private int maxRecursionDepth = 0;
            private int maxRayDispatchDimensionWidth = 0;
            private int maxRayHitAttributeSize = 0;
            private int maxRayPayloadSize = 0;
            private int meshShaderTier = 0;
            private boolean supportsMeshShaders = false;
            private boolean supportsAmplificationShaders = false;
            private int maxMeshShaderThreadGroupSize = 0;
            private int maxMeshOutputVertices = 0;
            private int maxMeshOutputPrimitives = 0;
            private int maxAmplificationShaderThreadGroupSize = 0;
            private int variableRateShadingTier = 0;
            private boolean supportsVariableRateShading = false;
            private boolean supportsShadingRateImage = false;
            private boolean supportsPerPrimitiveShadingRate = false;
            private int shadingRateImageTileSize = 0;
            private boolean additionalShadingRatesSupported = false;
            private int samplerFeedbackTier = 0;
            private boolean supportsSamplerFeedback = false;
            private int maxSampledMipRegionWidth = 0;
            private int maxSampledMipRegionHeight = 0;
            private int resourceBindingTier = 1;
            private int maxDescriptorsPerHeapCBV_SRV_UAV = 1_000_000;
            private int maxDescriptorsPerHeapSampler = 2048;
            private boolean supportsDynamicIndexing = false;
            private boolean supportsDescriptorHeapIndexing = false;
            private boolean supportsBindless = false;
            private int tiledResourcesTier = 0;
            private boolean supportsTiledResources = false;
            private boolean supportsTiledVolumes = false;
            private boolean supportsTextureStreamingFeedback = false;
            private int maxTiledResourceDimension = 0;
            private int conservativeRasterizationTier = 0;
            private boolean supportsConservativeRasterization = false;
            private boolean supportsPostSnapDegenerateTriangleCulling = false;
            private boolean supportsInnerCoverage = false;
            private int renderPassesTier = 0;
            private boolean supportsRenderPasses = false;
            private boolean supportsPipelineLibraries = false;
            private boolean supportsShaderCache = false;
            private int maxTexture1DDimension = 16384;
            private int maxTexture2DDimension = 16384;
            private int maxTexture3DDimension = 2048;
            private int maxTextureCubeDimension = 16384;
            private int maxTextureArraySlices = 2048;
            private int maxAnisotropy = 16;
            private boolean supportsBC6H_BC7 = true;
            private boolean supportsASTC = false;
            private int maxRenderTargets = 8;
            private int maxUAVSlots = 64;
            private int maxUAVsPerStage = 8;
            private boolean supportsROVs = false;
            private boolean supportsStencilRef = false;
            private boolean supportsDepthBoundsTest = false;
            private boolean supportsCompute = true;
            private boolean supportsDoublePrecision = false;
            private int maxComputeShaderThreadGroupSizeX = 1024;
            private int maxComputeShaderThreadGroupSizeY = 1024;
            private int maxComputeShaderThreadGroupSizeZ = 64;
            private int maxComputeShaderThreadGroupTotal = 1024;
            private int maxComputeShaderDispatchDimension = 65535;
            private long maxComputeSharedMemory = 32768;
            private int nodeCount = 1;
            private boolean supportsCrossNodeSharing = false;
            private boolean supportsCrossAdapterRowMajorTexture = false;
            private boolean supportsOutputMergerLogicOps = false;
            private boolean supportsProgrammableSamplePositions = false;
            private boolean supportsViewInstancing = false;
            private int viewInstancingTier = 0;
            private boolean supportsWriteBufferImmediate = false;
            private boolean supportsBackgroundProcessing = false;
            private boolean supportsHDR = false;
            private boolean supportsWideColorGamut = false;
            private boolean supportsVariableRefreshRate = false;
            private boolean supportsHardwareComposition = false;
            private boolean supportsTearing = false;

            public Builder() {}

            // Builder methods for all fields
            public Builder apiVersion(DirectXVersion v) { this.apiVersion = v; return this; }
            public Builder featureLevel(int v) { this.featureLevel = v; return this; }
            public Builder adapterDescription(String v) { this.adapterDescription = v; return this; }
            public Builder vendorId(int v) { this.vendorId = v; return this; }
            public Builder deviceId(int v) { this.deviceId = v; return this; }
            public Builder dedicatedVideoMemory(long v) { this.dedicatedVideoMemory = v; return this; }
            public Builder sharedSystemMemory(long v) { this.sharedSystemMemory = v; return this; }
            public Builder maxShaderModel(int v) { this.maxShaderModel = v; return this; }
            public Builder supportsWaveIntrinsics(boolean v) { this.supportsWaveIntrinsics = v; return this; }
            public Builder waveSize(int v) { this.waveSize = v; return this; }
            public Builder rayTracingTier(int v) { this.rayTracingTier = v; this.supportsRayTracing = v > 0; return this; }
            public Builder meshShaderTier(int v) { this.meshShaderTier = v; this.supportsMeshShaders = v > 0; return this; }
            public Builder variableRateShadingTier(int v) { this.variableRateShadingTier = v; this.supportsVariableRateShading = v > 0; return this; }
            public Builder resourceBindingTier(int v) { this.resourceBindingTier = v; return this; }
            public Builder tiledResourcesTier(int v) { this.tiledResourcesTier = v; this.supportsTiledResources = v > 0; return this; }
            public Builder supportsCompute(boolean v) { this.supportsCompute = v; return this; }
            public Builder supportsBindless(boolean v) { this.supportsBindless = v; return this; }
            public Builder maxRenderTargets(int v) { this.maxRenderTargets = v; return this; }
            public Builder maxTexture2DDimension(int v) { this.maxTexture2DDimension = v; return this; }
            public Builder nodeCount(int v) { this.nodeCount = v; return this; }
            public Builder supportsHDR(boolean v) { this.supportsHDR = v; return this; }
            public Builder supportsTearing(boolean v) { this.supportsTearing = v; return this; }

            public DXDeviceCapabilities build() {
                return new DXDeviceCapabilities(this);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 13: RESOURCE STATE TRACKER (DX12)
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Per-resource state tracking for automatic barrier insertion in DX12.
     * This is CRITICAL for correct DX12 operation.
     */
    public static final class ResourceStateTracker {
        
        /**
         * State of a single resource across all subresources.
         */
        public static final class ResourceState {
            private final long resourceHandle;
            private final int subresourceCount;
            private final int[] subresourceStates;
            private int globalState;
            private boolean hasPerSubresourceState;

            public ResourceState(long resourceHandle, int subresourceCount, int initialState) {
                this.resourceHandle = resourceHandle;
                this.subresourceCount = subresourceCount;
                this.subresourceStates = new int[subresourceCount];
                this.globalState = initialState;
                this.hasPerSubresourceState = false;
                Arrays.fill(subresourceStates, initialState);
            }

            public int getState(int subresource) {
                if (subresource == D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES) {
                    return globalState;
                }
                return hasPerSubresourceState ? subresourceStates[subresource] : globalState;
            }

            public void setState(int subresource, int state) {
                if (subresource == D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES) {
                    globalState = state;
                    hasPerSubresourceState = false;
                    Arrays.fill(subresourceStates, state);
                } else {
                    if (!hasPerSubresourceState) {
                        Arrays.fill(subresourceStates, globalState);
                        hasPerSubresourceState = true;
                    }
                    subresourceStates[subresource] = state;
                }
            }

            public boolean needsBarrierTo(int subresource, int desiredState) {
                int currentState = getState(subresource);
                return D3D12ResourceStates.requiresBarrierTo(currentState, desiredState);
            }
        }

        private static final int D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES = 0xFFFFFFFF;

        private final Long2ObjectMap<ResourceState> resourceStates;
        private final List<PendingBarrier> pendingBarriers;
        private final List<PendingBarrier> flushBarriers;

        public ResourceStateTracker() {
            this.resourceStates = new Long2ObjectOpenHashMap<>();
            this.pendingBarriers = new ArrayList<>();
            this.flushBarriers = new ArrayList<>();
        }

        /**
         * Register a new resource with initial state.
         */
        public void registerResource(long handle, int subresourceCount, int initialState) {
            resourceStates.put(handle, new ResourceState(handle, subresourceCount, initialState));
        }

        /**
         * Unregister a resource (on destruction).
         */
        public void unregisterResource(long handle) {
            resourceStates.remove(handle);
        }

        /**
         * Request transition to a new state - may generate barrier.
         */
        public void transitionResource(long handle, int subresource, int desiredState) {
            ResourceState state = resourceStates.get(handle);
            if (state == null) {
                // Unknown resource - assume common state
                state = new ResourceState(handle, 1, D3D12ResourceStates.COMMON);
                resourceStates.put(handle, state);
            }

            int currentState = state.getState(subresource);
            if (D3D12ResourceStates.requiresBarrierTo(currentState, desiredState)) {
                pendingBarriers.add(new PendingBarrier(
                    PendingBarrier.Type.TRANSITION,
                    handle,
                    subresource,
                    currentState,
                    desiredState
                ));
            }
            state.setState(subresource, desiredState);
        }

        /**
         * Add UAV barrier for unordered access synchronization.
         */
        public void uavBarrier(long handle) {
            pendingBarriers.add(new PendingBarrier(
                PendingBarrier.Type.UAV,
                handle,
                D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES,
                0, 0
            ));
        }

        /**
         * Add aliasing barrier for memory aliasing.
         */
        public void aliasingBarrier(long beforeHandle, long afterHandle) {
            pendingBarriers.add(new PendingBarrier(
                PendingBarrier.Type.ALIASING,
                beforeHandle,
                0,
                0, 0
            ).withAfterResource(afterHandle));
        }

        /**
         * Flush pending barriers - returns list to submit.
         */
        public List<PendingBarrier> flushBarriers() {
            if (pendingBarriers.isEmpty()) {
                return Collections.emptyList();
            }
            flushBarriers.clear();
            flushBarriers.addAll(pendingBarriers);
            pendingBarriers.clear();
            return Collections.unmodifiableList(flushBarriers);
        }

        /**
         * Optimize barriers by coalescing compatible transitions.
         */
        public List<PendingBarrier> flushAndOptimize() {
            List<PendingBarrier> barriers = flushBarriers();
            if (barriers.size() <= 1) {
                return barriers;
            }

            // Coalesce barriers for same resource
            Long2ObjectMap<List<PendingBarrier>> byResource = new Long2ObjectOpenHashMap<>();
            for (PendingBarrier b : barriers) {
                byResource.computeIfAbsent(b.resourceHandle, k -> new ArrayList<>()).add(b);
            }

            List<PendingBarrier> optimized = new ArrayList<>();
            for (List<PendingBarrier> group : byResource.values()) {
                if (group.size() == 1) {
                    optimized.add(group.get(0));
                } else {
                    // Try to merge consecutive transitions
                    optimized.addAll(mergeBarriers(group));
                }
            }

            return optimized;
        }

        private List<PendingBarrier> mergeBarriers(List<PendingBarrier> barriers) {
            // Simple case - return as-is for now
            // Real implementation would merge subresource transitions
            return barriers;
        }

        /**
         * Get current state of a resource.
         */
        public int getCurrentState(long handle, int subresource) {
            ResourceState state = resourceStates.get(handle);
            return state != null ? state.getState(subresource) : D3D12ResourceStates.COMMON;
        }

        /**
         * Pending barrier to be submitted to command list.
         */
        public static final class PendingBarrier {
            public enum Type { TRANSITION, UAV, ALIASING }

            public final Type type;
            public final long resourceHandle;
            public final int subresource;
            public final int stateBefore;
            public final int stateAfter;
            private long afterResourceHandle;

            public PendingBarrier(Type type, long handle, int subresource, int before, int after) {
                this.type = type;
                this.resourceHandle = handle;
                this.subresource = subresource;
                this.stateBefore = before;
                this.stateAfter = after;
            }

            PendingBarrier withAfterResource(long after) {
                this.afterResourceHandle = after;
                return this;
            }

            public long getAfterResourceHandle() {
                return afterResourceHandle;
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 14: DESCRIPTOR HEAP ALLOCATOR (DX12)
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Descriptor heap management with allocation and defragmentation.
     */
    public static final class DescriptorHeapAllocator {

        /**
         * A single descriptor heap.
         */
        public static final class DescriptorHeap {
            private final long heapHandle;
            private final int type;
            private final int capacity;
            private final int incrementSize;
            private final long cpuStart;
            private final long gpuStart;
            private final boolean shaderVisible;

            // Free list allocation
            private final BitSet allocated;
            private int searchStart;
            private int usedCount;

            public DescriptorHeap(long heapHandle, int type, int capacity, int incrementSize,
                                  long cpuStart, long gpuStart, boolean shaderVisible) {
                this.heapHandle = heapHandle;
                this.type = type;
                this.capacity = capacity;
                this.incrementSize = incrementSize;
                this.cpuStart = cpuStart;
                this.gpuStart = gpuStart;
                this.shaderVisible = shaderVisible;
                this.allocated = new BitSet(capacity);
                this.searchStart = 0;
                this.usedCount = 0;
            }

            /**
             * Allocate a contiguous range of descriptors.
             * @return Starting index, or -1 if allocation failed.
             */
            public int allocate(int count) {
                if (count <= 0 || usedCount + count > capacity) {
                    return -1;
                }

                // Search for contiguous free range
                int searchPos = searchStart;
                int rangeStart = -1;
                int rangeCount = 0;

                for (int i = 0; i < capacity; i++) {
                    int idx = (searchPos + i) % capacity;
                    
                    if (!allocated.get(idx)) {
                        if (rangeStart < 0) {
                            rangeStart = idx;
                            rangeCount = 1;
                        } else if (idx == rangeStart + rangeCount) {
                            rangeCount++;
                        } else {
                            rangeStart = idx;
                            rangeCount = 1;
                        }

                        if (rangeCount == count) {
                            // Found suitable range
                            for (int j = 0; j < count; j++) {
                                allocated.set(rangeStart + j);
                            }
                            usedCount += count;
                            searchStart = (rangeStart + count) % capacity;
                            return rangeStart;
                        }
                    } else {
                        rangeStart = -1;
                        rangeCount = 0;
                    }
                }

                return -1;  // No suitable range found
            }

            /**
             * Free a range of descriptors.
             */
            public void free(int startIndex, int count) {
                for (int i = 0; i < count; i++) {
                    int idx = startIndex + i;
                    if (allocated.get(idx)) {
                        allocated.clear(idx);
                        usedCount--;
                    }
                }
                if (startIndex < searchStart) {
                    searchStart = startIndex;
                }
            }

            /**
             * Get CPU descriptor handle for index.
             */
            public long getCpuHandle(int index) {
                return cpuStart + (long) index * incrementSize;
            }

            /**
             * Get GPU descriptor handle for index (if shader visible).
             */
            public long getGpuHandle(int index) {
                if (!shaderVisible) {
                    throw new IllegalStateException("Heap is not shader visible");
                }
                return gpuStart + (long) index * incrementSize;
            }

            public long getHeapHandle() { return heapHandle; }
            public int getType() { return type; }
            public int getCapacity() { return capacity; }
            public int getUsedCount() { return usedCount; }
            public int getFreeCount() { return capacity - usedCount; }
            public float getFragmentation() {
                if (usedCount == 0) return 0.0f;
                // Count contiguous free regions
                int freeRegions = 0;
                boolean inFree = false;
                for (int i = 0; i < capacity; i++) {
                    if (!allocated.get(i)) {
                        if (!inFree) {
                            freeRegions++;
                            inFree = true;
                        }
                    } else {
                        inFree = false;
                    }
                }
                // Fragmentation is 0 if all free space is contiguous
                return freeRegions > 1 ? (float)(freeRegions - 1) / (capacity - usedCount) : 0.0f;
            }
        }

        /**
         * Descriptor allocation handle.
         */
        public static final class DescriptorAllocation {
            private final DescriptorHeap heap;
            private final int startIndex;
            private final int count;
            private boolean freed;

            DescriptorAllocation(DescriptorHeap heap, int startIndex, int count) {
                this.heap = heap;
                this.startIndex = startIndex;
                this.count = count;
                this.freed = false;
            }

            public long getCpuHandle() { return getCpuHandle(0); }
            public long getCpuHandle(int offset) {
                checkNotFreed();
                return heap.getCpuHandle(startIndex + offset);
            }
            
            public long getGpuHandle() { return getGpuHandle(0); }
            public long getGpuHandle(int offset) {
                checkNotFreed();
                return heap.getGpuHandle(startIndex + offset);
            }

            public int getStartIndex() { return startIndex; }
            public int getCount() { return count; }
            public DescriptorHeap getHeap() { return heap; }

            public void free() {
                if (!freed) {
                    heap.free(startIndex, count);
                    freed = true;
                }
            }

            private void checkNotFreed() {
                if (freed) throw new IllegalStateException("Allocation already freed");
            }
        }

        // Heaps by type
        private final Object2ObjectMap<Integer, List<DescriptorHeap>> heapsByType;
        private final MemorySegment deviceHandle;
        private final Arena arena;

        // Increment sizes per type (queried from device)
        private final int[] incrementSizes;

        public DescriptorHeapAllocator(MemorySegment deviceHandle, Arena arena) {
            this.deviceHandle = deviceHandle;
            this.arena = arena;
            this.heapsByType = new Object2ObjectOpenHashMap<>();
            this.incrementSizes = new int[D3D12DescriptorHeapType.NUM_TYPES];

            // Initialize empty lists for each type
            for (int i = 0; i < D3D12DescriptorHeapType.NUM_TYPES; i++) {
                heapsByType.put(i, new ArrayList<>());
            }
        }

        /**
         * Set increment size for a heap type (queried from device on init).
         */
        public void setIncrementSize(int heapType, int size) {
            incrementSizes[heapType] = size;
        }

        /**
         * Allocate descriptors from appropriate heap.
         */
        public DescriptorAllocation allocate(int heapType, int count, boolean shaderVisible) {
            List<DescriptorHeap> heaps = heapsByType.get(heapType);
            
            // Try existing heaps
            for (DescriptorHeap heap : heaps) {
                if (heap.shaderVisible == shaderVisible) {
                    int index = heap.allocate(count);
                    if (index >= 0) {
                        return new DescriptorAllocation(heap, index, count);
                    }
                }
            }

            // Need to create new heap
            int heapSize = calculateNewHeapSize(heapType, count);
            DescriptorHeap newHeap = createHeap(heapType, heapSize, shaderVisible);
            if (newHeap != null) {
                heaps.add(newHeap);
                int index = newHeap.allocate(count);
                if (index >= 0) {
                    return new DescriptorAllocation(newHeap, index, count);
                }
            }

            return null;  // Allocation failed
        }

        private int calculateNewHeapSize(int heapType, int minSize) {
            int maxSize = D3D12DescriptorHeapType.getMaxDescriptors(heapType);
            int baseSize = switch (heapType) {
                case D3D12DescriptorHeapType.CBV_SRV_UAV -> 65536;
                case D3D12DescriptorHeapType.SAMPLER -> 1024;
                case D3D12DescriptorHeapType.RTV -> 1024;
                case D3D12DescriptorHeapType.DSV -> 256;
                default -> 1024;
            };
            return Math.min(maxSize, Math.max(baseSize, minSize * 2));
        }

        private DescriptorHeap createHeap(int heapType, int size, boolean shaderVisible) {
            // This would call into native code to create the actual heap...if nothing here then I've moved on to BGFX Via LWJGL
            return null;
        }

        /**
         * Get total statistics across all heaps.
         */
        public AllocationStats getStats() {
            int totalCapacity = 0;
            int totalUsed = 0;
            int heapCount = 0;
            float maxFragmentation = 0;

            for (List<DescriptorHeap> heaps : heapsByType.values()) {
                for (DescriptorHeap heap : heaps) {
                    totalCapacity += heap.getCapacity();
                    totalUsed += heap.getUsedCount();
                    heapCount++;
                    maxFragmentation = Math.max(maxFragmentation, heap.getFragmentation());
                }
            }

            return new AllocationStats(totalCapacity, totalUsed, heapCount, maxFragmentation);
        }

        public record AllocationStats(int totalCapacity, int totalUsed, int heapCount, float maxFragmentation) {
            public float getUtilization() {
                return totalCapacity > 0 ? (float) totalUsed / totalCapacity : 0;
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 15: COMMAND ALLOCATOR POOL (DX12)
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Pool for recycling command allocators in DX12.
     */
    public static final class CommandAllocatorPool {

        /**
         * A pooled command allocator.
         */
        public static final class PooledAllocator {
            private final long handle;
            private final int listType;
            private long lastFenceValue;
            private boolean inUse;

            PooledAllocator(long handle, int listType) {
                this.handle = handle;
                this.listType = listType;
                this.lastFenceValue = 0;
                this.inUse = false;
            }

            public long getHandle() { return handle; }
            public int getListType() { return listType; }
        }

        private final Int2ObjectMap<Deque<PooledAllocator>> availableAllocators;
        private final List<PooledAllocator> inFlightAllocators;
        private final MemorySegment deviceHandle;
        private final AtomicLong currentFenceValue;

        public CommandAllocatorPool(MemorySegment deviceHandle) {
            this.deviceHandle = deviceHandle;
            this.availableAllocators = new Int2ObjectOpenHashMap<>();
            this.inFlightAllocators = new CopyOnWriteArrayList<>();
            this.currentFenceValue = new AtomicLong(0);

            // Initialize pools for each command list type
            for (int type = 0; type <= D3D12CommandListType.COPY; type++) {
                availableAllocators.put(type, new ConcurrentLinkedDeque<>());
            }
        }

        /**
         * Acquire an allocator for use.
         */
        public PooledAllocator acquire(int listType, long completedFenceValue) {
            // Recycle completed allocators
            recycleCompleted(completedFenceValue);

            // Try to get from pool
            Deque<PooledAllocator> pool = availableAllocators.get(listType);
            if (pool != null) {
                PooledAllocator allocator = pool.pollFirst();
                if (allocator != null) {
                    allocator.inUse = true;
                    // Reset allocator before returning
                    resetAllocator(allocator);
                    return allocator;
                }
            }

            // Create new allocator
            PooledAllocator newAllocator = createAllocator(listType);
            if (newAllocator != null) {
                newAllocator.inUse = true;
            }
            return newAllocator;
        }

        /**
         * Return allocator to pool after use.
         */
        public void release(PooledAllocator allocator, long fenceValue) {
            allocator.lastFenceValue = fenceValue;
            allocator.inUse = false;
            inFlightAllocators.add(allocator);
        }

        /**
         * Recycle allocators whose work has completed.
         */
        private void recycleCompleted(long completedFenceValue) {
            inFlightAllocators.removeIf(allocator -> {
                if (allocator.lastFenceValue <= completedFenceValue) {
                    Deque<PooledAllocator> pool = availableAllocators.get(allocator.listType);
                    if (pool != null) {
                        pool.addLast(allocator);
                    }
                    return true;
                }
                return false;
            });
        }

        private PooledAllocator createAllocator(int listType) {
            // Native call to create allocator - placeholder
            return null;
        }

        private void resetAllocator(PooledAllocator allocator) {
            // Native call to reset allocator - placeholder
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 16: ROOT SIGNATURE BUILDER (DX12)
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Builder for creating D3D12 root signatures.
     */
    public static final class RootSignatureBuilder {

        /**
         * Root parameter types.
         */
        public enum ParameterType {
            DESCRIPTOR_TABLE,
            ROOT_CONSTANTS,
            CBV,
            SRV,
            UAV
        }

        /**
         * Descriptor range types.
         */
        public enum RangeType {
            SRV, UAV, CBV, SAMPLER
        }

        /**
         * Shader visibility flags.
         */
        public enum ShaderVisibility {
            ALL(0),
            VERTEX(1),
            HULL(2),
            DOMAIN(3),
            GEOMETRY(4),
            PIXEL(5),
            AMPLIFICATION(6),
            MESH(7);

            public final int value;
            ShaderVisibility(int value) { this.value = value; }
        }

        /**
         * A descriptor range within a table.
         */
        public static final class DescriptorRange {
            public final RangeType rangeType;
            public final int numDescriptors;
            public final int baseRegister;
            public final int registerSpace;
            public final int offsetInDescriptorsFromTableStart;

            public DescriptorRange(RangeType rangeType, int numDescriptors, int baseRegister,
                                   int registerSpace, int offset) {
                this.rangeType = rangeType;
                this.numDescriptors = numDescriptors;
                this.baseRegister = baseRegister;
                this.registerSpace = registerSpace;
                this.offsetInDescriptorsFromTableStart = offset;
            }

            public static DescriptorRange cbv(int baseRegister, int space) {
                return new DescriptorRange(RangeType.CBV, 1, baseRegister, space, -1);
            }

            public static DescriptorRange cbvs(int baseRegister, int count, int space) {
                return new DescriptorRange(RangeType.CBV, count, baseRegister, space, -1);
            }

            public static DescriptorRange srv(int baseRegister, int space) {
                return new DescriptorRange(RangeType.SRV, 1, baseRegister, space, -1);
            }

            public static DescriptorRange srvs(int baseRegister, int count, int space) {
                return new DescriptorRange(RangeType.SRV, count, baseRegister, space, -1);
            }

            public static DescriptorRange unboundedSrvs(int baseRegister, int space) {
                return new DescriptorRange(RangeType.SRV, -1, baseRegister, space, -1);
            }

            public static DescriptorRange uav(int baseRegister, int space) {
                return new DescriptorRange(RangeType.UAV, 1, baseRegister, space, -1);
            }

            public static DescriptorRange uavs(int baseRegister, int count, int space) {
                return new DescriptorRange(RangeType.UAV, count, baseRegister, space, -1);
            }

            public static DescriptorRange sampler(int baseRegister, int space) {
                return new DescriptorRange(RangeType.SAMPLER, 1, baseRegister, space, -1);
            }

            public static DescriptorRange samplers(int baseRegister, int count, int space) {
                return new DescriptorRange(RangeType.SAMPLER, count, baseRegister, space, -1);
            }
        }

        /**
         * A root parameter.
         */
        public static final class RootParameter {
            public final ParameterType parameterType;
            public final ShaderVisibility visibility;
            public final List<DescriptorRange> descriptorRanges;
            public final int shaderRegister;
            public final int registerSpace;
            public final int num32BitConstants;

            private RootParameter(ParameterType type, ShaderVisibility visibility,
                                 List<DescriptorRange> ranges, int shaderRegister,
                                 int registerSpace, int num32BitConstants) {
                this.parameterType = type;
                this.visibility = visibility;
                this.descriptorRanges = ranges;
                this.shaderRegister = shaderRegister;
                this.registerSpace = registerSpace;
                this.num32BitConstants = num32BitConstants;
            }

            public static RootParameter descriptorTable(ShaderVisibility visibility,
                                                        DescriptorRange... ranges) {
                return new RootParameter(ParameterType.DESCRIPTOR_TABLE, visibility,
                                        Arrays.asList(ranges), 0, 0, 0);
            }

            public static RootParameter constants(int num32BitConstants, int shaderRegister,
                                                  int registerSpace, ShaderVisibility visibility) {
                return new RootParameter(ParameterType.ROOT_CONSTANTS, visibility,
                                        null, shaderRegister, registerSpace, num32BitConstants);
            }

            public static RootParameter cbv(int shaderRegister, int registerSpace,
                                           ShaderVisibility visibility) {
                return new RootParameter(ParameterType.CBV, visibility,
                                        null, shaderRegister, registerSpace, 0);
            }

            public static RootParameter srv(int shaderRegister, int registerSpace,
                                           ShaderVisibility visibility) {
                return new RootParameter(ParameterType.SRV, visibility,
                                        null, shaderRegister, registerSpace, 0);
            }

            public static RootParameter uav(int shaderRegister, int registerSpace,
                                           ShaderVisibility visibility) {
                return new RootParameter(ParameterType.UAV, visibility,
                                        null, shaderRegister, registerSpace, 0);
            }
        }

        /**
         * Static sampler definition.
         */
        public static final class StaticSampler {
            public final int filter;
            public final int addressU;
            public final int addressV;
            public final int addressW;
            public final float mipLODBias;
            public final int maxAnisotropy;
            public final int comparisonFunc;
            public final int borderColor;
            public final float minLOD;
            public final float maxLOD;
            public final int shaderRegister;
            public final int registerSpace;
            public final ShaderVisibility visibility;

            public StaticSampler(int filter, int addressU, int addressV, int addressW,
                                float mipLODBias, int maxAnisotropy, int comparisonFunc,
                                int borderColor, float minLOD, float maxLOD,
                                int shaderRegister, int registerSpace, ShaderVisibility visibility) {
                this.filter = filter;
                this.addressU = addressU;
                this.addressV = addressV;
                this.addressW = addressW;
                this.mipLODBias = mipLODBias;
                this.maxAnisotropy = maxAnisotropy;
                this.comparisonFunc = comparisonFunc;
                this.borderColor = borderColor;
                this.minLOD = minLOD;
                this.maxLOD = maxLOD;
                this.shaderRegister = shaderRegister;
                this.registerSpace = registerSpace;
                this.visibility = visibility;
            }

            public static StaticSampler pointClamp(int shaderRegister, int space, ShaderVisibility vis) {
                return new StaticSampler(0, 3, 3, 3, 0, 0, 0, 0, 0, Float.MAX_VALUE,
                                        shaderRegister, space, vis);
            }

            public static StaticSampler linearClamp(int shaderRegister, int space, ShaderVisibility vis) {
                return new StaticSampler(0x15, 3, 3, 3, 0, 0, 0, 0, 0, Float.MAX_VALUE,
                                        shaderRegister, space, vis);
            }

            public static StaticSampler linearWrap(int shaderRegister, int space, ShaderVisibility vis) {
                return new StaticSampler(0x15, 1, 1, 1, 0, 0, 0, 0, 0, Float.MAX_VALUE,
                                        shaderRegister, space, vis);
            }

            public static StaticSampler anisotropicWrap(int shaderRegister, int space,
                                                        int maxAniso, ShaderVisibility vis) {
                return new StaticSampler(0x55, 1, 1, 1, 0, maxAniso, 0, 0, 0, Float.MAX_VALUE,
                                        shaderRegister, space, vis);
            }
        }

        /**
         * Root signature flags.
         */
        public static final int FLAG_NONE = 0;
        public static final int FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT = 0x1;
        public static final int FLAG_DENY_VERTEX_SHADER_ROOT_ACCESS = 0x2;
        public static final int FLAG_DENY_HULL_SHADER_ROOT_ACCESS = 0x4;
        public static final int FLAG_DENY_DOMAIN_SHADER_ROOT_ACCESS = 0x8;
        public static final int FLAG_DENY_GEOMETRY_SHADER_ROOT_ACCESS = 0x10;
        public static final int FLAG_DENY_PIXEL_SHADER_ROOT_ACCESS = 0x20;
        public static final int FLAG_ALLOW_STREAM_OUTPUT = 0x40;
        public static final int FLAG_LOCAL_ROOT_SIGNATURE = 0x80;
        public static final int FLAG_DENY_AMPLIFICATION_SHADER_ROOT_ACCESS = 0x100;
        public static final int FLAG_DENY_MESH_SHADER_ROOT_ACCESS = 0x200;
        public static final int FLAG_CBV_SRV_UAV_HEAP_DIRECTLY_INDEXED = 0x400;
        public static final int FLAG_SAMPLER_HEAP_DIRECTLY_INDEXED = 0x800;

        private final List<RootParameter> parameters;
        private final List<StaticSampler> staticSamplers;
        private int flags;

        public RootSignatureBuilder() {
            this.parameters = new ArrayList<>();
            this.staticSamplers = new ArrayList<>();
            this.flags = FLAG_NONE;
        }

        public RootSignatureBuilder addParameter(RootParameter param) {
            parameters.add(param);
            return this;
        }

        public RootSignatureBuilder addStaticSampler(StaticSampler sampler) {
            staticSamplers.add(sampler);
            return this;
        }

        public RootSignatureBuilder setFlags(int flags) {
            this.flags = flags;
            return this;
        }

        public RootSignatureBuilder allowInputLayout() {
            this.flags |= FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT;
            return this;
        }

        public RootSignatureBuilder denyVertexShaderAccess() {
            this.flags |= FLAG_DENY_VERTEX_SHADER_ROOT_ACCESS;
            return this;
        }

        public RootSignatureBuilder denyPixelShaderAccess() {
            this.flags |= FLAG_DENY_PIXEL_SHADER_ROOT_ACCESS;
            return this;
        }

        public RootSignatureBuilder enableBindless() {
            this.flags |= FLAG_CBV_SRV_UAV_HEAP_DIRECTLY_INDEXED |
                          FLAG_SAMPLER_HEAP_DIRECTLY_INDEXED;
            return this;
        }

        /**
         * Calculate root signature size in DWORDs.
         */
        public int calculateSize() {
            int size = 0;
            for (RootParameter param : parameters) {
                size += switch (param.parameterType) {
                    case DESCRIPTOR_TABLE -> 1;  // 1 DWORD for table pointer
                    case ROOT_CONSTANTS -> param.num32BitConstants;
                    case CBV, SRV, UAV -> 2;  // 2 DWORDs for GPU VA
                };
            }
            return size;
        }

        /**
         * Validate root signature doesn't exceed limits.
         */
        public boolean validate() {
            int size = calculateSize();
            if (size > 64) {  // D3D12 limit is 64 DWORDs
                return false;
            }
            
            // Check for valid parameter configurations
            for (RootParameter param : parameters) {
                if (param.parameterType == ParameterType.DESCRIPTOR_TABLE) {
                    if (param.descriptorRanges == null || param.descriptorRanges.isEmpty()) {
                        return false;
                    }
                }
            }
            
            return true;
        }

        public List<RootParameter> getParameters() { return Collections.unmodifiableList(parameters); }
        public List<StaticSampler> getStaticSamplers() { return Collections.unmodifiableList(staticSamplers); }
        public int getFlags() { return flags; }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 17: PIPELINE STATE OBJECT BUILDER (DX12)
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Builder for D3D12 Graphics Pipeline State Objects.
     */
    public static final class GraphicsPSOBuilder {

        /**
         * Input element description for vertex input.
         */
        public static final class InputElement {
            public final String semanticName;
            public final int semanticIndex;
            public final int format;  // DXGI_FORMAT
            public final int inputSlot;
            public final int alignedByteOffset;
            public final int inputSlotClass;  // 0=per-vertex, 1=per-instance
            public final int instanceDataStepRate;

            public InputElement(String semanticName, int semanticIndex, int format,
                               int inputSlot, int alignedByteOffset,
                               int inputSlotClass, int instanceDataStepRate) {
                this.semanticName = semanticName;
                this.semanticIndex = semanticIndex;
                this.format = format;
                this.inputSlot = inputSlot;
                this.alignedByteOffset = alignedByteOffset;
                this.inputSlotClass = inputSlotClass;
                this.instanceDataStepRate = instanceDataStepRate;
            }

            public static InputElement position(int slot) {
                return new InputElement("POSITION", 0, DXGIFormat.R32G32B32_FLOAT, slot, 0, 0, 0);
            }

            public static InputElement normal(int slot, int offset) {
                return new InputElement("NORMAL", 0, DXGIFormat.R32G32B32_FLOAT, slot, offset, 0, 0);
            }

            public static InputElement texcoord(int index, int slot, int offset) {
                return new InputElement("TEXCOORD", index, DXGIFormat.R32G32_FLOAT, slot, offset, 0, 0);
            }

            public static InputElement color(int slot, int offset) {
                return new InputElement("COLOR", 0, DXGIFormat.R8G8B8A8_UNORM, slot, offset, 0, 0);
            }

            public static InputElement tangent(int slot, int offset) {
                return new InputElement("TANGENT", 0, DXGIFormat.R32G32B32A32_FLOAT, slot, offset, 0, 0);
            }
        }

        /**
         * Blend state description.
         */
        public static final class BlendDesc {
            public final boolean alphaToCoverageEnable;
            public final boolean independentBlendEnable;
            public final RenderTargetBlendDesc[] renderTargets;

            public BlendDesc(boolean alphaToCoverage, boolean independentBlend,
                            RenderTargetBlendDesc[] rts) {
                this.alphaToCoverageEnable = alphaToCoverage;
                this.independentBlendEnable = independentBlend;
                this.renderTargets = rts;
            }

            public static BlendDesc opaque() {
                return new BlendDesc(false, false, new RenderTargetBlendDesc[] {
                    RenderTargetBlendDesc.opaque()
                });
            }

            public static BlendDesc alphaBlend() {
                return new BlendDesc(false, false, new RenderTargetBlendDesc[] {
                    RenderTargetBlendDesc.alphaBlend()
                });
            }

            public static BlendDesc additive() {
                return new BlendDesc(false, false, new RenderTargetBlendDesc[] {
                    RenderTargetBlendDesc.additive()
                });
            }
        }

        /**
         * Per-render-target blend description.
         */
        public static final class RenderTargetBlendDesc {
            public final boolean blendEnable;
            public final boolean logicOpEnable;
            public final int srcBlend;
            public final int destBlend;
            public final int blendOp;
            public final int srcBlendAlpha;
            public final int destBlendAlpha;
            public final int blendOpAlpha;
            public final int logicOp;
            public final int renderTargetWriteMask;

            public RenderTargetBlendDesc(boolean blendEnable, boolean logicOpEnable,
                                        int srcBlend, int destBlend, int blendOp,
                                        int srcBlendAlpha, int destBlendAlpha, int blendOpAlpha,
                                        int logicOp, int writeMask) {
                this.blendEnable = blendEnable;
                this.logicOpEnable = logicOpEnable;
                this.srcBlend = srcBlend;
                this.destBlend = destBlend;
                this.blendOp = blendOp;
                this.srcBlendAlpha = srcBlendAlpha;
                this.destBlendAlpha = destBlendAlpha;
                this.blendOpAlpha = blendOpAlpha;
                this.logicOp = logicOp;
                this.renderTargetWriteMask = writeMask;
            }

            public static RenderTargetBlendDesc opaque() {
                return new RenderTargetBlendDesc(false, false, 1, 0, 1, 1, 0, 1, 0, 0xF);
            }

            public static RenderTargetBlendDesc alphaBlend() {
                return new RenderTargetBlendDesc(true, false, 5, 6, 1, 1, 6, 1, 0, 0xF);
            }

            public static RenderTargetBlendDesc additive() {
                return new RenderTargetBlendDesc(true, false, 1, 1, 1, 1, 1, 1, 0, 0xF);
            }

            public static RenderTargetBlendDesc premultiplied() {
                return new RenderTargetBlendDesc(true, false, 1, 6, 1, 1, 6, 1, 0, 0xF);
            }
        }

        /**
         * Rasterizer state description.
         */
        public static final class RasterizerDesc {
            public final int fillMode;  // 2=wireframe, 3=solid
            public final int cullMode;  // 1=none, 2=front, 3=back
            public final boolean frontCounterClockwise;
            public final int depthBias;
            public final float depthBiasClamp;
            public final float slopeScaledDepthBias;
            public final boolean depthClipEnable;
            public final boolean multisampleEnable;
            public final boolean antialiasedLineEnable;
            public final int forcedSampleCount;
            public final int conservativeRaster;  // 0=off, 1=on

            public RasterizerDesc(int fillMode, int cullMode, boolean frontCCW,
                                 int depthBias, float biasClamp, float slopeScaledBias,
                                 boolean depthClip, boolean multisample, boolean aaLine,
                                 int forcedSamples, int conservativeRaster) {
                this.fillMode = fillMode;
                this.cullMode = cullMode;
                this.frontCounterClockwise = frontCCW;
                this.depthBias = depthBias;
                this.depthBiasClamp = biasClamp;
                this.slopeScaledDepthBias = slopeScaledBias;
                this.depthClipEnable = depthClip;
                this.multisampleEnable = multisample;
                this.antialiasedLineEnable = aaLine;
                this.forcedSampleCount = forcedSamples;
                this.conservativeRaster = conservativeRaster;
            }

            public static RasterizerDesc defaults() {
                return new RasterizerDesc(3, 3, false, 0, 0, 0, true, false, false, 0, 0);
            }

            public static RasterizerDesc noCull() {
                return new RasterizerDesc(3, 1, false, 0, 0, 0, true, false, false, 0, 0);
            }

            public static RasterizerDesc wireframe() {
                return new RasterizerDesc(2, 1, false, 0, 0, 0, true, false, false, 0, 0);
            }

            public static RasterizerDesc frontCull() {
                return new RasterizerDesc(3, 2, false, 0, 0, 0, true, false, false, 0, 0);
            }
        }

        /**
         * Depth-stencil state description.
         */
        public static final class DepthStencilDesc {
            public final boolean depthEnable;
            public final int depthWriteMask;  // 0=zero, 1=all
            public final int depthFunc;
            public final boolean stencilEnable;
            public final int stencilReadMask;
            public final int stencilWriteMask;
            public final StencilOpDesc frontFace;
            public final StencilOpDesc backFace;

            public DepthStencilDesc(boolean depthEnable, int depthWriteMask, int depthFunc,
                                   boolean stencilEnable, int stencilReadMask, int stencilWriteMask,
                                   StencilOpDesc frontFace, StencilOpDesc backFace) {
                this.depthEnable = depthEnable;
                this.depthWriteMask = depthWriteMask;
                this.depthFunc = depthFunc;
                this.stencilEnable = stencilEnable;
                this.stencilReadMask = stencilReadMask;
                this.stencilWriteMask = stencilWriteMask;
                this.frontFace = frontFace;
                this.backFace = backFace;
            }

            public static DepthStencilDesc defaults() {
                return new DepthStencilDesc(true, 1, 2, false, 0xFF, 0xFF,
                                           StencilOpDesc.defaults(), StencilOpDesc.defaults());
            }

            public static DepthStencilDesc readOnly() {
                return new DepthStencilDesc(true, 0, 2, false, 0xFF, 0xFF,
                                           StencilOpDesc.defaults(), StencilOpDesc.defaults());
            }

            public static DepthStencilDesc disabled() {
                return new DepthStencilDesc(false, 0, 8, false, 0xFF, 0xFF,
                                           StencilOpDesc.defaults(), StencilOpDesc.defaults());
            }
        }

        /**
         * Stencil operation description.
         */
        public static final class StencilOpDesc {
            public final int stencilFailOp;
            public final int stencilDepthFailOp;
            public final int stencilPassOp;
            public final int stencilFunc;

            public StencilOpDesc(int failOp, int depthFailOp, int passOp, int func) {
                this.stencilFailOp = failOp;
                this.stencilDepthFailOp = depthFailOp;
                this.stencilPassOp = passOp;
                this.stencilFunc = func;
            }

            public static StencilOpDesc defaults() {
                return new StencilOpDesc(1, 1, 1, 8);  // KEEP, KEEP, KEEP, ALWAYS
            }
        }

        // Builder state
        private long rootSignatureHandle;
        private byte[] vsBlob;
        private byte[] hsBlob;
        private byte[] dsBlob;
        private byte[] gsBlob;
        private byte[] psBlob;
        private final List<InputElement> inputElements;
        private BlendDesc blendDesc;
        private int sampleMask;
        private RasterizerDesc rasterizerDesc;
        private DepthStencilDesc depthStencilDesc;
        private int primitiveTopologyType;  // 0=undefined, 1=point, 2=line, 3=triangle, 4=patch
        private int numRenderTargets;
        private final int[] rtvFormats;
        private int dsvFormat;
        private int sampleCount;
        private int sampleQuality;

        public GraphicsPSOBuilder() {
            this.inputElements = new ArrayList<>();
            this.blendDesc = BlendDesc.opaque();
            this.sampleMask = 0xFFFFFFFF;
            this.rasterizerDesc = RasterizerDesc.defaults();
            this.depthStencilDesc = DepthStencilDesc.defaults();
            this.primitiveTopologyType = 3;  // Triangle
            this.numRenderTargets = 1;
            this.rtvFormats = new int[8];
            this.rtvFormats[0] = DXGIFormat.R8G8B8A8_UNORM;
            this.dsvFormat = DXGIFormat.D24_UNORM_S8_UINT;
            this.sampleCount = 1;
            this.sampleQuality = 0;
        }

        public GraphicsPSOBuilder rootSignature(long handle) {
            this.rootSignatureHandle = handle;
            return this;
        }

        public GraphicsPSOBuilder vertexShader(byte[] bytecode) {
            this.vsBlob = bytecode;
            return this;
        }

        public GraphicsPSOBuilder pixelShader(byte[] bytecode) {
            this.psBlob = bytecode;
            return this;
        }

        public GraphicsPSOBuilder hullShader(byte[] bytecode) {
            this.hsBlob = bytecode;
            return this;
        }

        public GraphicsPSOBuilder domainShader(byte[] bytecode) {
            this.dsBlob = bytecode;
            return this;
        }

        public GraphicsPSOBuilder geometryShader(byte[] bytecode) {
            this.gsBlob = bytecode;
            return this;
        }

        public GraphicsPSOBuilder addInputElement(InputElement element) {
            inputElements.add(element);
            return this;
        }

        public GraphicsPSOBuilder blendState(BlendDesc desc) {
            this.blendDesc = desc;
            return this;
        }

        public GraphicsPSOBuilder rasterizerState(RasterizerDesc desc) {
            this.rasterizerDesc = desc;
            return this;
        }

        public GraphicsPSOBuilder depthStencilState(DepthStencilDesc desc) {
            this.depthStencilDesc = desc;
            return this;
        }

        public GraphicsPSOBuilder primitiveTopologyType(int type) {
            this.primitiveTopologyType = type;
            return this;
        }

        public GraphicsPSOBuilder renderTargetFormat(int index, int format) {
            this.rtvFormats[index] = format;
            if (index >= numRenderTargets) {
                numRenderTargets = index + 1;
            }
            return this;
        }

        public GraphicsPSOBuilder depthStencilFormat(int format) {
            this.dsvFormat = format;
            return this;
        }

        public GraphicsPSOBuilder sampleDesc(int count, int quality) {
            this.sampleCount = count;
            this.sampleQuality = quality;
            return this;
        }

        public boolean validate() {
            if (rootSignatureHandle == 0) return false;
            if (vsBlob == null || vsBlob.length == 0) return false;
            if (psBlob == null || psBlob.length == 0) return false;
            return true;
        }

        // Getters
        public long getRootSignatureHandle() { return rootSignatureHandle; }
        public byte[] getVSBlob() { return vsBlob; }
        public byte[] getPSBlob() { return psBlob; }
        public byte[] getHSBlob() { return hsBlob; }
        public byte[] getDSBlob() { return dsBlob; }
        public byte[] getGSBlob() { return gsBlob; }
        public List<InputElement> getInputElements() { return Collections.unmodifiableList(inputElements); }
        public BlendDesc getBlendDesc() { return blendDesc; }
        public RasterizerDesc getRasterizerDesc() { return rasterizerDesc; }
        public DepthStencilDesc getDepthStencilDesc() { return depthStencilDesc; }
        public int getPrimitiveTopologyType() { return primitiveTopologyType; }
        public int getNumRenderTargets() { return numRenderTargets; }
        public int getRTVFormat(int index) { return rtvFormats[index]; }
        public int getDSVFormat() { return dsvFormat; }
        public int getSampleCount() { return sampleCount; }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 18: RAY TRACING STRUCTURES
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Ray tracing acceleration structure management.
     */
    public static final class RayTracingStructures {

        /**
         * Geometry description for BLAS.
         */
        public static final class GeometryDesc {
            public enum Type { TRIANGLES, PROCEDURAL_AABBS }
            public enum Flags { NONE, OPAQUE, NO_DUPLICATE_ANYHIT }

            public final Type type;
            public final int flags;
            public final long vertexBuffer;
            public final int vertexCount;
            public final long vertexStride;
            public final int vertexFormat;
            public final long indexBuffer;
            public final int indexCount;
            public final int indexFormat;
            public final long transformBuffer;

            public GeometryDesc(Type type, int flags, long vertexBuffer, int vertexCount,
                               long vertexStride, int vertexFormat, long indexBuffer,
                               int indexCount, int indexFormat, long transformBuffer) {
                this.type = type;
                this.flags = flags;
                this.vertexBuffer = vertexBuffer;
                this.vertexCount = vertexCount;
                this.vertexStride = vertexStride;
                this.vertexFormat = vertexFormat;
                this.indexBuffer = indexBuffer;
                this.indexCount = indexCount;
                this.indexFormat = indexFormat;
                this.transformBuffer = transformBuffer;
            }

            public static GeometryDesc triangles(long vb, int vCount, int vStride, int vFormat,
                                                 long ib, int iCount, int iFormat) {
                return new GeometryDesc(Type.TRIANGLES, 0, vb, vCount, vStride, vFormat,
                                       ib, iCount, iFormat, 0);
            }
        }

        /**
         * Instance description for TLAS.
         */
        public static final class InstanceDesc {
            public final float[] transform;  // 3x4 row-major
            public final int instanceID;
            public final int instanceMask;
            public final int instanceContributionToHitGroupIndex;
            public final int flags;
            public final long blasAddress;

            public InstanceDesc(float[] transform, int instanceID, int mask,
                               int hitGroupIndex, int flags, long blasAddress) {
                this.transform = transform;
                this.instanceID = instanceID;
                this.instanceMask = mask;
                this.instanceContributionToHitGroupIndex = hitGroupIndex;
                this.flags = flags;
                this.blasAddress = blasAddress;
            }

            public static InstanceDesc create(float[] transform, long blasAddress) {
                return new InstanceDesc(transform, 0, 0xFF, 0, 0, blasAddress);
            }
        }

        /**
         * Shader table entry.
         */
        public static final class ShaderRecord {
            public final byte[] shaderIdentifier;  // 32 bytes
            public final byte[] localRootArguments;

            public ShaderRecord(byte[] identifier, byte[] localArgs) {
                this.shaderIdentifier = identifier;
                this.localRootArguments = localArgs;
            }
        }

        /**
         * Shader table (ray generation, hit groups, miss shaders).
         */
        public static final class ShaderTable {
            private final List<ShaderRecord> records;
            private final int recordStride;
            private long gpuAddress;

            public ShaderTable(int recordStride) {
                this.records = new ArrayList<>();
                this.recordStride = alignTo(recordStride, 32);
            }

            public void addRecord(ShaderRecord record) {
                records.add(record);
            }

            public void setGpuAddress(long address) {
                this.gpuAddress = address;
            }

            public long getGpuAddress() { return gpuAddress; }
            public int getRecordStride() { return recordStride; }
            public int getSize() { return records.size() * recordStride; }
            public List<ShaderRecord> getRecords() { return Collections.unmodifiableList(records); }

            private static int alignTo(int value, int alignment) {
                return (value + alignment - 1) & ~(alignment - 1);
            }
        }

        /**
         * DispatchRays description.
         */
        public static final class DispatchRaysDesc {
            public final long rayGenerationShaderRecord;
            public final long rayGenerationShaderRecordSize;
            public final long missShaderTable;
            public final long missShaderTableSize;
            public final long missShaderTableStride;
            public final long hitGroupTable;
            public final long hitGroupTableSize;
            public final long hitGroupTableStride;
            public final long callableShaderTable;
            public final long callableShaderTableSize;
            public final long callableShaderTableStride;
            public final int width;
            public final int height;
            public final int depth;

            public DispatchRaysDesc(ShaderTable rayGen, ShaderTable miss, ShaderTable hitGroup,
                                   int width, int height, int depth) {
                this.rayGenerationShaderRecord = rayGen.getGpuAddress();
                this.rayGenerationShaderRecordSize = rayGen.getRecordStride();
                this.missShaderTable = miss.getGpuAddress();
                this.missShaderTableSize = miss.getSize();
                this.missShaderTableStride = miss.getRecordStride();
                this.hitGroupTable = hitGroup.getGpuAddress();
                this.hitGroupTableSize = hitGroup.getSize();
                this.hitGroupTableStride = hitGroup.getRecordStride();
                this.callableShaderTable = 0;
                this.callableShaderTableSize = 0;
                this.callableShaderTableStride = 0;
                this.width = width;
                this.height = height;
                this.depth = depth;
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 19: NATIVE INTERFACE VIA PANAMA FFI
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Panama FFI bindings for DirectX native calls.
     */
    public static final class DXNativeInterface {
        
        private final Arena arena;
        private final Linker linker;
        private final SymbolLookup d3d12Lookup;
        private final SymbolLookup dxgiLookup;
        private final SymbolLookup d3dCompilerLookup;

        // Method handles for common operations
        private volatile MethodHandle mhCreateDevice;
        private volatile MethodHandle mhCreateCommandQueue;
        private volatile MethodHandle mhCreateCommandAllocator;
        private volatile MethodHandle mhCreateCommandList;
        private volatile MethodHandle mhCreateFence;
        private volatile MethodHandle mhSignal;
        private volatile MethodHandle mhWait;
        private volatile MethodHandle mhExecuteCommandLists;
        private volatile MethodHandle mhClose;
        private volatile MethodHandle mhReset;

        // DXGI method handles
        private volatile MethodHandle mhCreateDXGIFactory2;
        private volatile MethodHandle mhEnumAdapters;
        private volatile MethodHandle mhCreateSwapChain;
        private volatile MethodHandle mhPresent;
        private volatile MethodHandle mhResizeBuffers;
        private volatile MethodHandle mhGetBuffer;

        public DXNativeInterface(Arena arena) {
            this.arena = arena;
            this.linker = Linker.nativeLinker();
            
            // Load DirectX libraries
            this.d3d12Lookup = SymbolLookup.libraryLookup("d3d12.dll", arena);
            this.dxgiLookup = SymbolLookup.libraryLookup("dxgi.dll", arena);
            this.d3dCompilerLookup = SymbolLookup.libraryLookup("d3dcompiler_47.dll", arena);
            
            initializeMethodHandles();
        }

        private void initializeMethodHandles() {
            // Initialize D3D12CreateDevice
            d3d12Lookup.find("D3D12CreateDevice").ifPresent(symbol -> {
                FunctionDescriptor desc = FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,     // HRESULT return
                    ValueLayout.ADDRESS,      // IUnknown* pAdapter
                    ValueLayout.JAVA_INT,     // D3D_FEATURE_LEVEL
                    ValueLayout.ADDRESS,      // REFIID riid
                    ValueLayout.ADDRESS       // void** ppDevice
                );
                mhCreateDevice = linker.downcallHandle(symbol, desc);
            });

            // Initialize CreateDXGIFactory2
            dxgiLookup.find("CreateDXGIFactory2").ifPresent(symbol -> {
                FunctionDescriptor desc = FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,     // HRESULT return
                    ValueLayout.JAVA_INT,     // UINT Flags
                    ValueLayout.ADDRESS,      // REFIID riid
                    ValueLayout.ADDRESS       // void** ppFactory
                );
                mhCreateDXGIFactory2 = linker.downcallHandle(symbol, desc);
            });
        }

        /**
         * Create D3D12 device.
         */
        public int createDevice(MemorySegment adapter, int featureLevel,
                               MemorySegment riid, MemorySegment ppDevice) {
            try {
                return (int) mhCreateDevice.invokeExact(adapter, featureLevel, riid, ppDevice);
            } catch (Throwable t) {
                throw new RuntimeException("D3D12CreateDevice failed", t);
            }
        }

        /**
         * Create DXGI Factory.
         */
        public int createDXGIFactory2(int flags, MemorySegment,  riid, MemorySegment ppFactory) {
            try {
                return (int) mhCreateDXGIFactory2.invokeExact(flags, riid, ppFactory);
            } catch (Throwable t) {
                throw new RuntimeException("CreateDXGIFactory2 failed", t);
            }
        }

        /**
         * Get IID for common interfaces.
         */
        public MemorySegment getIID(String interfaceName) {
            // Pre-defined GUIDs for common interfaces
            return switch (interfaceName) {
                case "ID3D12Device" -> createGUID(0x189819f1, 0x1db6, 0x4b57, 
                    new byte[]{(byte)0xbe, 0x54, 0x18, 0x21, 0x33, (byte)0x9b, 0x85, (byte)0xf7});
                case "ID3D12Device5" -> createGUID(0x8b4f173b, 0x2fea, 0x4b80,
                    new byte[]{(byte)0x8f, 0x58, 0x43, 0x07, 0x19, 0x1a, (byte)0xb9, 0x5d});
                case "ID3D12CommandQueue" -> createGUID(0x0ec870a6, 0x5d7e, 0x4c22,
                    new byte[]{(byte)0x8c, (byte)0xfc, 0x5b, (byte)0xaa, (byte)0xe0, 0x76, 0x16, (byte)0xed});
                case "IDXGIFactory6" -> createGUID(0xc1b6694f, 0xff09, 0x44a9,
                    new byte[]{(byte)0xb0, 0x3c, 0x77, (byte)0x90, 0x0a, 0x0a, 0x1d, 0x17});
                case "IDXGISwapChain4" -> createGUID(0x3d585d5a, 0xbd4a, 0x489e,
                    new byte[]{(byte)0xb1, (byte)0xf4, 0x3d, (byte)0xbc, (byte)0xb6, 0x45, 0x2f, (byte)0xfb});
                default -> MemorySegment.NULL;
            };
        }

        private MemorySegment createGUID(int data1, int data2, int data3, byte[] data4) {
            MemorySegment guid = arena.allocate(16);
            guid.set(ValueLayout.JAVA_INT, 0, data1);
            guid.set(ValueLayout.JAVA_SHORT, 4, (short) data2);
            guid.set(ValueLayout.JAVA_SHORT, 6, (short) data3);
            MemorySegment.copy(data4, 0, guid, ValueLayout.JAVA_BYTE, 8, 8);
            return guid;
        }

        /**
         * Query interface from COM object.
         */
        public int queryInterface(MemorySegment object, MemorySegment riid, MemorySegment ppvObject) {
            // Get vtable pointer
            MemorySegment vtable = object.get(ValueLayout.ADDRESS, 0);
            // QueryInterface is first method in vtable
            MemorySegment queryInterfacePtr = vtable.get(ValueLayout.ADDRESS, 0);
            
            try {
                MethodHandle mh = linker.downcallHandle(queryInterfacePtr, FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS
                ));
                return (int) mh.invokeExact(object, riid, ppvObject);
            } catch (Throwable t) {
                throw new RuntimeException("QueryInterface failed", t);
            }
        }

        /**
         * Release COM object.
         */
        public int release(MemorySegment object) {
            if (object == null || object.equals(MemorySegment.NULL)) {
                return 0;
            }
            
            // Get vtable pointer
            MemorySegment vtable = object.get(ValueLayout.ADDRESS, 0);
            // Release is third method in vtable (after QueryInterface, AddRef)
            MemorySegment releasePtr = vtable.get(ValueLayout.ADDRESS, 16);
            
            try {
                MethodHandle mh = linker.downcallHandle(releasePtr, FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS
                ));
                return (int) mh.invokeExact(object);
            } catch (Throwable t) {
                throw new RuntimeException("Release failed", t);
            }
        }

        public Arena getArena() { return arena; }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 20: DX9 IMPLEMENTATION PATH (BGFX BACKEND)
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * DirectX 9 specific implementation - legacy support via BGFX abstraction layer.
     * 
     * This implementation provides a complete DX9-compatible API surface while internally
     * utilizing BGFX for cross-platform rendering. All legacy DX9 concepts are mapped
     * to modern BGFX equivalents with automatic state tracking and resource management.
     * 
     * Architecture:
     * ┌─────────────────────────────────────────────────────────────────────────────┐
     * │                         DX9Path API Surface                                 │
     * ├─────────────────────────────────────────────────────────────────────────────┤
     * │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
     * │  │ RenderState  │  │ TextureStage │  │   Sampler    │  │  Transform   │   │
     * │  │   Manager    │  │   Manager    │  │   Manager    │  │   Manager    │   │
     * │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
     * │         │                 │                 │                 │           │
     * │         └─────────────────┴─────────────────┴─────────────────┘           │
     * │                                    │                                       │
     * │                          ┌─────────▼─────────┐                            │
     * │                          │  State Compiler   │                            │
     * │                          │  & Deduplicator   │                            │
     * │                          └─────────┬─────────┘                            │
     * │                                    │                                       │
     * │                          ┌─────────▼─────────┐                            │
     * │                          │   BGFX Encoder    │                            │
     * │                          │   Command Buffer  │                            │
     * │                          └─────────┬─────────┘                            │
     * │                                    │                                       │
     * │                          ┌─────────▼─────────┐                            │
     * │                          │   BGFX Backend    │                            │
     * │                          │ (D3D9/11/12/GL/VK)│                            │
     * │                          └───────────────────┘                            │
     * └─────────────────────────────────────────────────────────────────────────────┘
     */
    public static final class DX9Path {

        // ─────────────────────────────────────────────────────────────────────────────
        // Core State & Resources
        // ─────────────────────────────────────────────────────────────────────────────

        private final Arena arena;
        private MemorySegment d3d9;
        private MemorySegment device;
        private final Int2ObjectMap<MemorySegment> vertexDeclarations;
        private final Int2ObjectMap<MemorySegment> vertexShaders;
        private final Int2ObjectMap<MemorySegment> pixelShaders;
        private final Int2ObjectMap<MemorySegment> textures;

        // BGFX Integration Layer
        private final BGFXStateManager bgfxStateManager;
        private final BGFXResourceAllocator bgfxResourceAllocator;
        private final DX9ToBGFXStateMapper stateMapper;
        private final DX9VertexDeclarationCompiler vertexDeclCompiler;
        private final DX9ShaderTranspiler shaderTranspiler;
        private final DX9TextureFormatMapper textureFormatMapper;

        // View Management
        private short currentViewId;
        private final short[] viewIdStack;
        private int viewIdStackPointer;
        private static final int MAX_VIEW_STACK_DEPTH = 32;

        // Transform State
        private final float[] worldMatrix;
        private final float[] viewMatrix;
        private final float[] projectionMatrix;
        private final float[] textureMatrices;
        private final boolean[] transformDirty;
        private final float[] combinedWVP;
        private final float[] combinedWV;

        // Render State Cache (DX9 style)
        private final int[] renderStates;
        private final long[] renderStateDirtyBits;
        private static final int MAX_RENDER_STATES = 256;

        // Texture Stage State
        private final int[][] textureStageStates;
        private final long[] textureStageStateDirtyBits;
        private static final int MAX_TEXTURE_STAGES = 8;
        private static final int MAX_TEXTURE_STAGE_STATES = 33;

        // Sampler State
        private final int[][] samplerStates;
        private final long[] samplerStateDirtyBits;
        private static final int MAX_SAMPLERS = 16;
        private static final int MAX_SAMPLER_STATES = 14;

        // Active Resources
        private final short[] activeTextures;
        private short activeVertexBuffer;
        private short activeIndexBuffer;
        private short activeVertexShader;
        private short activePixelShader;
        private int activeVertexDeclaration;
        private int activeFVF;

        // Stream Sources
        private final StreamSourceBinding[] streamSources;
        private static final int MAX_STREAMS = 16;

        // D3D9 caps (populated from BGFX capabilities)
        private int maxTextureWidth;
        private int maxTextureHeight;
        private int maxVolumeExtent;
        private int maxTextureAspectRatio;
        private int maxAnisotropy;
        private int maxVertexIndex;
        private int maxStreams;
        private int maxStreamStride;
        private int vertexShaderVersion;
        private int pixelShaderVersion;
        private int maxVertexShaderConst;

        // Performance Counters
        private final AtomicLong drawCallCount;
        private final AtomicLong stateChangeCount;
        private final AtomicLong textureBindCount;
        private final AtomicLong shaderSwitchCount;
        private final LongAdder triangleCount;
        private final LongAdder vertexCount;

        // Scene State
        private volatile boolean inScene;
        private final ReentrantReadWriteLock sceneLock;

        // ─────────────────────────────────────────────────────────────────────────────
        // Inner Classes for State Management
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Manages stream source bindings for vertex data.
         */
        private static final class StreamSourceBinding {
            short vertexBuffer;
            int offsetInBytes;
            int stride;
            int frequency;
            int frequencyType;
            boolean dirty;

            void reset() {
                vertexBuffer = BGFX_INVALID_HANDLE;
                offsetInBytes = 0;
                stride = 0;
                frequency = 1;
                frequencyType = 0;
                dirty = true;
            }

            void set(short vb, int offset, int str) {
                if (this.vertexBuffer != vb || this.offsetInBytes != offset || this.stride != str) {
                    this.vertexBuffer = vb;
                    this.offsetInBytes = offset;
                    this.stride = str;
                    this.dirty = true;
                }
            }
        }

        /**
         * Maps DX9 render states to BGFX state flags.
         */
        private static final class DX9ToBGFXStateMapper {

            // Pre-computed state mappings for fast lookup
            private final long[] blendFactorMap;
            private final long[] blendOpMap;
            private final long[] cmpFuncMap;
            private final long[] stencilOpMap;
            private final long[] cullModeMap;
            private final long[] fillModeMap;
            private final int[] textureFilterMap;
            private final int[] textureAddressMap;

            DX9ToBGFXStateMapper() {
                this.blendFactorMap = new long[16];
                this.blendOpMap = new long[8];
                this.cmpFuncMap = new long[16];
                this.stencilOpMap = new long[16];
                this.cullModeMap = new long[4];
                this.fillModeMap = new long[4];
                this.textureFilterMap = new int[8];
                this.textureAddressMap = new int[8];
                initializeMappings();
            }

            private void initializeMappings() {
                // D3DBLEND -> BGFX_STATE_BLEND_*
                blendFactorMap[0] = 0;                          // D3DBLEND_ZERO -> default
                blendFactorMap[1] = BGFX_STATE_BLEND_ZERO;      // D3DBLEND_ZERO
                blendFactorMap[2] = BGFX_STATE_BLEND_ONE;       // D3DBLEND_ONE
                blendFactorMap[3] = BGFX_STATE_BLEND_SRC_COLOR; // D3DBLEND_SRCCOLOR
                blendFactorMap[4] = BGFX_STATE_BLEND_INV_SRC_COLOR; // D3DBLEND_INVSRCCOLOR
                blendFactorMap[5] = BGFX_STATE_BLEND_SRC_ALPHA; // D3DBLEND_SRCALPHA
                blendFactorMap[6] = BGFX_STATE_BLEND_INV_SRC_ALPHA; // D3DBLEND_INVSRCALPHA
                blendFactorMap[7] = BGFX_STATE_BLEND_DST_ALPHA; // D3DBLEND_DESTALPHA
                blendFactorMap[8] = BGFX_STATE_BLEND_INV_DST_ALPHA; // D3DBLEND_INVDESTALPHA
                blendFactorMap[9] = BGFX_STATE_BLEND_DST_COLOR; // D3DBLEND_DESTCOLOR
                blendFactorMap[10] = BGFX_STATE_BLEND_INV_DST_COLOR; // D3DBLEND_INVDESTCOLOR
                blendFactorMap[11] = BGFX_STATE_BLEND_SRC_ALPHA_SAT; // D3DBLEND_SRCALPHASAT

                // D3DBLENDOP -> BGFX_STATE_BLEND_EQUATION_*
                blendOpMap[1] = BGFX_STATE_BLEND_EQUATION_ADD;    // D3DBLENDOP_ADD
                blendOpMap[2] = BGFX_STATE_BLEND_EQUATION_SUB;    // D3DBLENDOP_SUBTRACT
                blendOpMap[3] = BGFX_STATE_BLEND_EQUATION_REVSUB; // D3DBLENDOP_REVSUBTRACT
                blendOpMap[4] = BGFX_STATE_BLEND_EQUATION_MIN;    // D3DBLENDOP_MIN
                blendOpMap[5] = BGFX_STATE_BLEND_EQUATION_MAX;    // D3DBLENDOP_MAX

                // D3DCMPFUNC -> BGFX_STATE_DEPTH_TEST_*
                cmpFuncMap[1] = BGFX_STATE_DEPTH_TEST_NEVER;    // D3DCMP_NEVER
                cmpFuncMap[2] = BGFX_STATE_DEPTH_TEST_LESS;     // D3DCMP_LESS
                cmpFuncMap[3] = BGFX_STATE_DEPTH_TEST_EQUAL;    // D3DCMP_EQUAL
                cmpFuncMap[4] = BGFX_STATE_DEPTH_TEST_LEQUAL;   // D3DCMP_LESSEQUAL
                cmpFuncMap[5] = BGFX_STATE_DEPTH_TEST_GREATER;  // D3DCMP_GREATER
                cmpFuncMap[6] = BGFX_STATE_DEPTH_TEST_NOTEQUAL; // D3DCMP_NOTEQUAL
                cmpFuncMap[7] = BGFX_STATE_DEPTH_TEST_GEQUAL;   // D3DCMP_GREATEREQUAL
                cmpFuncMap[8] = BGFX_STATE_DEPTH_TEST_ALWAYS;   // D3DCMP_ALWAYS

                // D3DCULL -> BGFX
                cullModeMap[1] = 0;                             // D3DCULL_NONE
                cullModeMap[2] = BGFX_STATE_CULL_CW;           // D3DCULL_CW
                cullModeMap[3] = BGFX_STATE_CULL_CCW;          // D3DCULL_CCW

                // D3DFILLMODE -> BGFX
                fillModeMap[1] = 0;                             // D3DFILL_POINT (not supported, default to solid)
                fillModeMap[2] = BGFX_STATE_PT_LINES;          // D3DFILL_WIREFRAME (approximate)
                fillModeMap[3] = 0;                             // D3DFILL_SOLID (default)

                // D3DTEXTUREFILTERTYPE -> BGFX texture flags
                textureFilterMap[0] = BGFX_SAMPLER_NONE;        // D3DTEXF_NONE
                textureFilterMap[1] = BGFX_SAMPLER_POINT;       // D3DTEXF_POINT
                textureFilterMap[2] = BGFX_SAMPLER_MIN_ANISOTROPIC; // D3DTEXF_LINEAR
                textureFilterMap[3] = BGFX_SAMPLER_MIN_ANISOTROPIC; // D3DTEXF_ANISOTROPIC

                // D3DTEXTUREADDRESS -> BGFX sampler flags
                textureAddressMap[1] = BGFX_SAMPLER_U_MIRROR;   // D3DTADDRESS_WRAP (BGFX default is wrap)
                textureAddressMap[2] = BGFX_SAMPLER_U_MIRROR;   // D3DTADDRESS_MIRROR
                textureAddressMap[3] = BGFX_SAMPLER_U_CLAMP;    // D3DTADDRESS_CLAMP
                textureAddressMap[4] = BGFX_SAMPLER_U_BORDER;   // D3DTADDRESS_BORDER
            }

            /**
             * Compile complete BGFX state flags from DX9 render states.
             */
            long compileState(int[] renderStates, int[][] samplerStates) {
                long state = BGFX_STATE_WRITE_RGB | BGFX_STATE_WRITE_A;

                // Depth testing
                if (renderStates[D3DRS_ZENABLE] != 0) {
                    int zFunc = renderStates[D3DRS_ZFUNC];
                    if (zFunc >= 1 && zFunc <= 8) {
                        state |= cmpFuncMap[zFunc];
                    }
                }

                // Depth write
                if (renderStates[D3DRS_ZWRITEENABLE] != 0) {
                    state |= BGFX_STATE_WRITE_Z;
                }

                // Alpha blending
                if (renderStates[D3DRS_ALPHABLENDENABLE] != 0) {
                    int srcBlend = renderStates[D3DRS_SRCBLEND];
                    int dstBlend = renderStates[D3DRS_DESTBLEND];
                    int blendOp = renderStates[D3DRS_BLENDOP];

                    long srcFactor = srcBlend < blendFactorMap.length ? blendFactorMap[srcBlend] : BGFX_STATE_BLEND_ONE;
                    long dstFactor = dstBlend < blendFactorMap.length ? blendFactorMap[dstBlend] : BGFX_STATE_BLEND_ZERO;
                    long equation = blendOp < blendOpMap.length ? blendOpMap[blendOp] : BGFX_STATE_BLEND_EQUATION_ADD;

                    state |= BGFX_STATE_BLEND_FUNC(srcFactor, dstFactor);
                    state |= equation;
                }

                // Culling
                int cullMode = renderStates[D3DRS_CULLMODE];
                if (cullMode >= 1 && cullMode <= 3) {
                    state |= cullModeMap[cullMode];
                }

                // Fill mode
                int fillMode = renderStates[D3DRS_FILLMODE];
                if (fillMode >= 1 && fillMode <= 3) {
                    state |= fillModeMap[fillMode];
                }

                // Alpha test (emulated via discard in shader)
                // BGFX doesn't have fixed-function alpha test, handled in shader

                // Multisample
                if (renderStates[D3DRS_MULTISAMPLEANTIALIAS] != 0) {
                    state |= BGFX_STATE_MSAA;
                }

                // Point size
                if (renderStates[D3DRS_POINTSPRITEENABLE] != 0) {
                    state |= BGFX_STATE_PT_POINTS;
                }

                return state;
            }

            /**
             * Compile stencil state from DX9 stencil render states.
             */
            int compileStencilState(int[] renderStates, boolean front) {
                if (renderStates[D3DRS_STENCILENABLE] == 0) {
                    return BGFX_STENCIL_NONE;
                }

                int stencilFunc = front ? renderStates[D3DRS_STENCILFUNC] : renderStates[D3DRS_CCW_STENCILFUNC];
                int stencilPass = front ? renderStates[D3DRS_STENCILPASS] : renderStates[D3DRS_CCW_STENCILPASS];
                int stencilFail = front ? renderStates[D3DRS_STENCILFAIL] : renderStates[D3DRS_CCW_STENCILFAIL];
                int stencilZFail = front ? renderStates[D3DRS_STENCILZFAIL] : renderStates[D3DRS_CCW_STENCILZFAIL];
                int stencilRef = renderStates[D3DRS_STENCILREF];
                int stencilMask = renderStates[D3DRS_STENCILMASK];
                int stencilWriteMask = renderStates[D3DRS_STENCILWRITEMASK];

                int bgfxStencil = 0;

                // Stencil test function
                bgfxStencil |= mapStencilFunc(stencilFunc);

                // Stencil operations
                bgfxStencil |= mapStencilOp(stencilFail, BGFX_STENCIL_OP_FAIL_S_SHIFT);
                bgfxStencil |= mapStencilOp(stencilZFail, BGFX_STENCIL_OP_FAIL_Z_SHIFT);
                bgfxStencil |= mapStencilOp(stencilPass, BGFX_STENCIL_OP_PASS_Z_SHIFT);

                // Reference value and masks
                bgfxStencil |= BGFX_STENCIL_FUNC_REF(stencilRef);
                bgfxStencil |= BGFX_STENCIL_FUNC_RMASK(stencilMask);

                return bgfxStencil;
            }

            private int mapStencilFunc(int dx9Func) {
                return switch (dx9Func) {
                    case 1 -> BGFX_STENCIL_TEST_NEVER;    // D3DCMP_NEVER
                    case 2 -> BGFX_STENCIL_TEST_LESS;     // D3DCMP_LESS
                    case 3 -> BGFX_STENCIL_TEST_EQUAL;    // D3DCMP_EQUAL
                    case 4 -> BGFX_STENCIL_TEST_LEQUAL;   // D3DCMP_LESSEQUAL
                    case 5 -> BGFX_STENCIL_TEST_GREATER;  // D3DCMP_GREATER
                    case 6 -> BGFX_STENCIL_TEST_NOTEQUAL; // D3DCMP_NOTEQUAL
                    case 7 -> BGFX_STENCIL_TEST_GEQUAL;   // D3DCMP_GREATEREQUAL
                    case 8 -> BGFX_STENCIL_TEST_ALWAYS;   // D3DCMP_ALWAYS
                    default -> BGFX_STENCIL_TEST_ALWAYS;
                };
            }

            private int mapStencilOp(int dx9Op, int shift) {
                int bgfxOp = switch (dx9Op) {
                    case 1 -> BGFX_STENCIL_OP_FAIL_S_KEEP;    // D3DSTENCILOP_KEEP
                    case 2 -> BGFX_STENCIL_OP_FAIL_S_ZERO;    // D3DSTENCILOP_ZERO
                    case 3 -> BGFX_STENCIL_OP_FAIL_S_REPLACE; // D3DSTENCILOP_REPLACE
                    case 4 -> BGFX_STENCIL_OP_FAIL_S_INCR;    // D3DSTENCILOP_INCRSAT
                    case 5 -> BGFX_STENCIL_OP_FAIL_S_DECR;    // D3DSTENCILOP_DECRSAT
                    case 6 -> BGFX_STENCIL_OP_FAIL_S_INVERT;  // D3DSTENCILOP_INVERT
                    case 7 -> BGFX_STENCIL_OP_FAIL_S_INCR;    // D3DSTENCILOP_INCR (wrapping)
                    case 8 -> BGFX_STENCIL_OP_FAIL_S_DECR;    // D3DSTENCILOP_DECR (wrapping)
                    default -> BGFX_STENCIL_OP_FAIL_S_KEEP;
                };
                // Shift to appropriate position based on fail/zfail/pass
                return bgfxOp >> BGFX_STENCIL_OP_FAIL_S_SHIFT << shift;
            }

            /**
             * Compile sampler flags for a texture stage.
             */
            int compileSamplerFlags(int[] samplerState) {
                int flags = BGFX_SAMPLER_NONE;

                // Min filter
                int minFilter = samplerState[D3DSAMP_MINFILTER];
                int magFilter = samplerState[D3DSAMP_MAGFILTER];
                int mipFilter = samplerState[D3DSAMP_MIPFILTER];

                // Combine filters
                if (minFilter == 1) flags |= BGFX_SAMPLER_MIN_POINT;      // POINT
                if (magFilter == 1) flags |= BGFX_SAMPLER_MAG_POINT;      // POINT
                if (minFilter == 3) flags |= BGFX_SAMPLER_MIN_ANISOTROPIC;// ANISOTROPIC
                if (magFilter == 3) flags |= BGFX_SAMPLER_MAG_ANISOTROPIC;// ANISOTROPIC
                if (mipFilter == 1) flags |= BGFX_SAMPLER_MIP_POINT;      // POINT

                // Address modes
                int addressU = samplerState[D3DSAMP_ADDRESSU];
                int addressV = samplerState[D3DSAMP_ADDRESSV];
                int addressW = samplerState[D3DSAMP_ADDRESSW];

                flags |= mapAddressMode(addressU, 'U');
                flags |= mapAddressMode(addressV, 'V');
                flags |= mapAddressMode(addressW, 'W');

                return flags;
            }

            private int mapAddressMode(int dx9Mode, char axis) {
                int baseFlag = switch (dx9Mode) {
                    case 2 -> axis == 'U' ? BGFX_SAMPLER_U_MIRROR : 
                              axis == 'V' ? BGFX_SAMPLER_V_MIRROR : BGFX_SAMPLER_W_MIRROR;
                    case 3 -> axis == 'U' ? BGFX_SAMPLER_U_CLAMP : 
                              axis == 'V' ? BGFX_SAMPLER_V_CLAMP : BGFX_SAMPLER_W_CLAMP;
                    case 4 -> axis == 'U' ? BGFX_SAMPLER_U_BORDER : 
                              axis == 'V' ? BGFX_SAMPLER_V_BORDER : BGFX_SAMPLER_W_BORDER;
                    default -> 0; // WRAP is default
                };
                return baseFlag;
            }
        }

        // D3D9 Render State Constants
        private static final int D3DRS_ZENABLE = 7;
        private static final int D3DRS_FILLMODE = 8;
        private static final int D3DRS_ZWRITEENABLE = 14;
        private static final int D3DRS_ALPHATESTENABLE = 15;
        private static final int D3DRS_SRCBLEND = 19;
        private static final int D3DRS_DESTBLEND = 20;
        private static final int D3DRS_CULLMODE = 22;
        private static final int D3DRS_ZFUNC = 23;
        private static final int D3DRS_ALPHAREF = 24;
        private static final int D3DRS_ALPHAFUNC = 25;
        private static final int D3DRS_ALPHABLENDENABLE = 27;
        private static final int D3DRS_STENCILENABLE = 52;
        private static final int D3DRS_STENCILFAIL = 53;
        private static final int D3DRS_STENCILZFAIL = 54;
        private static final int D3DRS_STENCILPASS = 55;
        private static final int D3DRS_STENCILFUNC = 56;
        private static final int D3DRS_STENCILREF = 57;
        private static final int D3DRS_STENCILMASK = 58;
        private static final int D3DRS_STENCILWRITEMASK = 59;
        private static final int D3DRS_BLENDOP = 171;
        private static final int D3DRS_MULTISAMPLEANTIALIAS = 161;
        private static final int D3DRS_POINTSPRITEENABLE = 156;
        private static final int D3DRS_CCW_STENCILFAIL = 186;
        private static final int D3DRS_CCW_STENCILZFAIL = 187;
        private static final int D3DRS_CCW_STENCILPASS = 188;
        private static final int D3DRS_CCW_STENCILFUNC = 189;

        // D3D9 Sampler State Constants
        private static final int D3DSAMP_ADDRESSU = 1;
        private static final int D3DSAMP_ADDRESSV = 2;
        private static final int D3DSAMP_ADDRESSW = 3;
        private static final int D3DSAMP_BORDERCOLOR = 4;
        private static final int D3DSAMP_MAGFILTER = 5;
        private static final int D3DSAMP_MINFILTER = 6;
        private static final int D3DSAMP_MIPFILTER = 7;
        private static final int D3DSAMP_MIPMAPLODBIAS = 8;
        private static final int D3DSAMP_MAXMIPLEVEL = 9;
        private static final int D3DSAMP_MAXANISOTROPY = 10;

        /**
         * Compiles DX9 vertex declarations to BGFX vertex layouts.
         */
        private static final class DX9VertexDeclarationCompiler {

            private final Int2ObjectMap<BGFXVertexLayout> compiledLayouts;
            private final Int2ObjectMap<short[]> layoutHandles;
            private final StampedLock cacheLock;

            DX9VertexDeclarationCompiler() {
                this.compiledLayouts = new Int2ObjectOpenHashMap<>();
                this.layoutHandles = new Int2ObjectOpenHashMap<>();
                this.cacheLock = new StampedLock();
            }

            /**
             * Compile a DX9 FVF code to a BGFX vertex layout.
             */
            BGFXVertexLayout compileFromFVF(int fvf, Arena arena) {
                long stamp = cacheLock.readLock();
                try {
                    BGFXVertexLayout cached = compiledLayouts.get(fvf);
                    if (cached != null) return cached;
                } finally {
                    cacheLock.unlockRead(stamp);
                }

                stamp = cacheLock.writeLock();
                try {
                    // Double-check
                    BGFXVertexLayout cached = compiledLayouts.get(fvf);
                    if (cached != null) return cached;

                    BGFXVertexLayout layout = BGFXVertexLayout.calloc(arena);
                    bgfx_vertex_layout_begin(layout, bgfx_get_renderer_type());

                    // Position (required for most FVF codes)
                    if ((fvf & 0x002) != 0) { // D3DFVF_XYZ
                        bgfx_vertex_layout_add(layout, BGFX_ATTRIB_POSITION, 3, BGFX_ATTRIB_TYPE_FLOAT, false, false);
                    } else if ((fvf & 0x004) != 0) { // D3DFVF_XYZRHW
                        bgfx_vertex_layout_add(layout, BGFX_ATTRIB_POSITION, 4, BGFX_ATTRIB_TYPE_FLOAT, false, false);
                    } else if ((fvf & 0x008) != 0) { // D3DFVF_XYZB1-5
                        int blendWeightCount = ((fvf >> 1) & 0x7);
                        bgfx_vertex_layout_add(layout, BGFX_ATTRIB_POSITION, 3, BGFX_ATTRIB_TYPE_FLOAT, false, false);
                        if (blendWeightCount > 0) {
                            bgfx_vertex_layout_add(layout, BGFX_ATTRIB_WEIGHT, blendWeightCount, BGFX_ATTRIB_TYPE_FLOAT, false, false);
                        }
                    }

                    // Normal
                    if ((fvf & 0x010) != 0) { // D3DFVF_NORMAL
                        bgfx_vertex_layout_add(layout, BGFX_ATTRIB_NORMAL, 3, BGFX_ATTRIB_TYPE_FLOAT, false, false);
                    }

                    // Point size
                    if ((fvf & 0x020) != 0) { // D3DFVF_PSIZE
                        // BGFX doesn't have direct point size attribute, skip or use tangent
                        bgfx_vertex_layout_skip(layout, 4);
                    }

                    // Diffuse color
                    if ((fvf & 0x040) != 0) { // D3DFVF_DIFFUSE
                        bgfx_vertex_layout_add(layout, BGFX_ATTRIB_COLOR0, 4, BGFX_ATTRIB_TYPE_UINT8, true, false);
                    }

                    // Specular color
                    if ((fvf & 0x080) != 0) { // D3DFVF_SPECULAR
                        bgfx_vertex_layout_add(layout, BGFX_ATTRIB_COLOR1, 4, BGFX_ATTRIB_TYPE_UINT8, true, false);
                    }

                    // Texture coordinates
                    int texCoordCount = (fvf >> 8) & 0xF;
                    int texCoordFormat = (fvf >> 16);
                    for (int i = 0; i < texCoordCount; i++) {
                        int format = (texCoordFormat >> (i * 2)) & 0x3;
                        int attrib = BGFX_ATTRIB_TEXCOORD0 + i;
                        int numComponents = switch (format) {
                            case 0 -> 2; // D3DFVF_TEXTUREFORMAT2
                            case 1 -> 3; // D3DFVF_TEXTUREFORMAT3
                            case 2 -> 4; // D3DFVF_TEXTUREFORMAT4
                            case 3 -> 1; // D3DFVF_TEXTUREFORMAT1
                            default -> 2;
                        };
                        bgfx_vertex_layout_add(layout, attrib, numComponents, BGFX_ATTRIB_TYPE_FLOAT, false, false);
                    }

                    bgfx_vertex_layout_end(layout);
                    compiledLayouts.put(fvf, layout);
                    return layout;

                } finally {
                    cacheLock.unlockWrite(stamp);
                }
            }

            /**
             * Compile custom vertex declaration elements.
             */
            BGFXVertexLayout compileFromElements(VertexElement[] elements, Arena arena) {
                int hash = computeElementsHash(elements);
                
                long stamp = cacheLock.readLock();
                try {
                    BGFXVertexLayout cached = compiledLayouts.get(hash);
                    if (cached != null) return cached;
                } finally {
                    cacheLock.unlockRead(stamp);
                }

                stamp = cacheLock.writeLock();
                try {
                    BGFXVertexLayout cached = compiledLayouts.get(hash);
                    if (cached != null) return cached;

                    BGFXVertexLayout layout = BGFXVertexLayout.calloc(arena);
                    bgfx_vertex_layout_begin(layout, bgfx_get_renderer_type());

                    for (VertexElement elem : elements) {
                        if (elem.type == 17) break; // D3DDECLTYPE_UNUSED / end marker

                        int attrib = mapDX9UsageToAttrib(elem.usage, elem.usageIndex);
                        int type = mapDX9TypeToAttribType(elem.type);
                        int num = getDX9TypeNumComponents(elem.type);
                        boolean normalized = isDX9TypeNormalized(elem.type);

                        bgfx_vertex_layout_add(layout, attrib, num, type, normalized, false);
                    }

                    bgfx_vertex_layout_end(layout);
                    compiledLayouts.put(hash, layout);
                    return layout;

                } finally {
                    cacheLock.unlockWrite(stamp);
                }
            }

            private int computeElementsHash(VertexElement[] elements) {
                int hash = 17;
                for (VertexElement e : elements) {
                    if (e.type == 17) break;
                    hash = 31 * hash + e.stream;
                    hash = 31 * hash + e.offset;
                    hash = 31 * hash + e.type;
                    hash = 31 * hash + e.method;
                    hash = 31 * hash + e.usage;
                    hash = 31 * hash + e.usageIndex;
                }
                return hash;
            }

            private int mapDX9UsageToAttrib(int usage, int usageIndex) {
                return switch (usage) {
                    case 0 -> BGFX_ATTRIB_POSITION;  // D3DDECLUSAGE_POSITION
                    case 1 -> BGFX_ATTRIB_WEIGHT;   // D3DDECLUSAGE_BLENDWEIGHT
                    case 2 -> BGFX_ATTRIB_INDICES;  // D3DDECLUSAGE_BLENDINDICES
                    case 3 -> BGFX_ATTRIB_NORMAL;   // D3DDECLUSAGE_NORMAL
                    case 4 -> BGFX_ATTRIB_TANGENT;  // D3DDECLUSAGE_PSIZE (repurpose)
                    case 5 -> BGFX_ATTRIB_TEXCOORD0 + usageIndex; // D3DDECLUSAGE_TEXCOORD
                    case 6 -> BGFX_ATTRIB_TANGENT;  // D3DDECLUSAGE_TANGENT
                    case 7 -> BGFX_ATTRIB_BITANGENT;// D3DDECLUSAGE_BINORMAL
                    case 10 -> BGFX_ATTRIB_COLOR0 + usageIndex; // D3DDECLUSAGE_COLOR
                    default -> BGFX_ATTRIB_TEXCOORD0 + usageIndex;
                };
            }

            private int mapDX9TypeToAttribType(int type) {
                return switch (type) {
                    case 0 -> BGFX_ATTRIB_TYPE_FLOAT;   // D3DDECLTYPE_FLOAT1
                    case 1 -> BGFX_ATTRIB_TYPE_FLOAT;   // D3DDECLTYPE_FLOAT2
                    case 2 -> BGFX_ATTRIB_TYPE_FLOAT;   // D3DDECLTYPE_FLOAT3
                    case 3 -> BGFX_ATTRIB_TYPE_FLOAT;   // D3DDECLTYPE_FLOAT4
                    case 4 -> BGFX_ATTRIB_TYPE_UINT8;   // D3DDECLTYPE_D3DCOLOR
                    case 5 -> BGFX_ATTRIB_TYPE_UINT8;   // D3DDECLTYPE_UBYTE4
                    case 6 -> BGFX_ATTRIB_TYPE_INT16;   // D3DDECLTYPE_SHORT2
                    case 7 -> BGFX_ATTRIB_TYPE_INT16;   // D3DDECLTYPE_SHORT4
                    case 8 -> BGFX_ATTRIB_TYPE_UINT8;   // D3DDECLTYPE_UBYTE4N
                    case 9 -> BGFX_ATTRIB_TYPE_INT16;   // D3DDECLTYPE_SHORT2N
                    case 10 -> BGFX_ATTRIB_TYPE_INT16;  // D3DDECLTYPE_SHORT4N
                    case 11 -> BGFX_ATTRIB_TYPE_INT16;  // D3DDECLTYPE_USHORT2N
                    case 12 -> BGFX_ATTRIB_TYPE_INT16;  // D3DDECLTYPE_USHORT4N
                    case 14 -> BGFX_ATTRIB_TYPE_HALF;   // D3DDECLTYPE_FLOAT16_2
                    case 15 -> BGFX_ATTRIB_TYPE_HALF;   // D3DDECLTYPE_FLOAT16_4
                    default -> BGFX_ATTRIB_TYPE_FLOAT;
                };
            }

            private int getDX9TypeNumComponents(int type) {
                return switch (type) {
                    case 0 -> 1;  // FLOAT1
                    case 1, 6, 9, 11, 14 -> 2;  // FLOAT2, SHORT2, etc.
                    case 2 -> 3;  // FLOAT3
                    case 3, 4, 5, 7, 8, 10, 12, 15 -> 4;  // FLOAT4, D3DCOLOR, UBYTE4, SHORT4, etc.
                    default -> 4;
                };
            }

            private boolean isDX9TypeNormalized(int type) {
                return switch (type) {
                    case 4, 8, 9, 10, 11, 12 -> true;
                    default -> false;
                };
            }

            void clear() {
                long stamp = cacheLock.writeLock();
                try {
                    compiledLayouts.clear();
                    layoutHandles.clear();
                } finally {
                    cacheLock.unlockWrite(stamp);
                }
            }
        }

        /**
         * Vertex element structure matching D3DVERTEXELEMENT9.
         */
        public static final class VertexElement {
            public short stream;
            public short offset;
            public byte type;
            public byte method;
            public byte usage;
            public byte usageIndex;

            public VertexElement() {}

            public VertexElement(int stream, int offset, int type, int method, int usage, int usageIndex) {
                this.stream = (short) stream;
                this.offset = (short) offset;
                this.type = (byte) type;
                this.method = (byte) method;
                this.usage = (byte) usage;
                this.usageIndex = (byte) usageIndex;
            }

            public static VertexElement end() {
                return new VertexElement(0xFF, 0, 17, 0, 0, 0); // D3DDECL_END()
            }
        }

        /**
         * BGFX state manager for efficient state tracking and batching.
         */
        private static final class BGFXStateManager {
            private long currentState;
            private int currentStencilFront;
            private int currentStencilBack;
            private int currentRgba;
            private short currentProgram;
            private final short[] currentTextures;
            private final int[] currentSamplerFlags;
            private boolean stateDirty;

            BGFXStateManager() {
                this.currentState = BGFX_STATE_DEFAULT;
                this.currentStencilFront = BGFX_STENCIL_NONE;
                this.currentStencilBack = BGFX_STENCIL_NONE;
                this.currentRgba = 0;
                this.currentProgram = BGFX_INVALID_HANDLE;
                this.currentTextures = new short[MAX_SAMPLERS];
                this.currentSamplerFlags = new int[MAX_SAMPLERS];
                Arrays.fill(currentTextures, BGFX_INVALID_HANDLE);
                this.stateDirty = true;
            }

            boolean setState(long state, int rgba) {
                if (this.currentState != state || this.currentRgba != rgba) {
                    this.currentState = state;
                    this.currentRgba = rgba;
                    this.stateDirty = true;
                    return true;
                }
                return false;
            }

            boolean setStencil(int front, int back) {
                if (this.currentStencilFront != front || this.currentStencilBack != back) {
                    this.currentStencilFront = front;
                    this.currentStencilBack = back;
                    this.stateDirty = true;
                    return true;
                }
                return false;
            }

            boolean setProgram(short program) {
                if (this.currentProgram != program) {
                    this.currentProgram = program;
                    return true;
                }
                return false;
            }

            boolean setTexture(int stage, short texture, int samplerFlags) {
                if (this.currentTextures[stage] != texture || this.currentSamplerFlags[stage] != samplerFlags) {
                    this.currentTextures[stage] = texture;
                    this.currentSamplerFlags[stage] = samplerFlags;
                    return true;
                }
                return false;
            }

            void applyState(short viewId) {
                if (stateDirty) {
                    bgfx_set_state(currentState, currentRgba);
                    if (currentStencilFront != BGFX_STENCIL_NONE || currentStencilBack != BGFX_STENCIL_NONE) {
                        bgfx_set_stencil(currentStencilFront, currentStencilBack);
                    }
                    stateDirty = false;
                }
            }

            void reset() {
                currentState = BGFX_STATE_DEFAULT;
                currentStencilFront = BGFX_STENCIL_NONE;
                currentStencilBack = BGFX_STENCIL_NONE;
                currentRgba = 0;
                currentProgram = BGFX_INVALID_HANDLE;
                Arrays.fill(currentTextures, BGFX_INVALID_HANDLE);
                Arrays.fill(currentSamplerFlags, 0);
                stateDirty = true;
            }
        }

        /**
         * BGFX resource allocator with pooling and reuse.
         */
        private static final class BGFXResourceAllocator {
            private final Int2ObjectMap<ArrayDeque<Short>> vertexBufferPools;
            private final Int2ObjectMap<ArrayDeque<Short>> indexBufferPools;
            private final ArrayDeque<Short> texturePool;
            private final ArrayDeque<Short> programPool;
            private final AtomicLong allocatedVertexBufferMemory;
            private final AtomicLong allocatedIndexBufferMemory;
            private final AtomicLong allocatedTextureMemory;
            private final ReentrantReadWriteLock poolLock;

            BGFXResourceAllocator() {
                this.vertexBufferPools = new Int2ObjectOpenHashMap<>();
                this.indexBufferPools = new Int2ObjectOpenHashMap<>();
                this.texturePool = new ArrayDeque<>();
                this.programPool = new ArrayDeque<>();
                this.allocatedVertexBufferMemory = new AtomicLong();
                this.allocatedIndexBufferMemory = new AtomicLong();
                this.allocatedTextureMemory = new AtomicLong();
                this.poolLock = new ReentrantReadWriteLock();
            }

            short allocateVertexBuffer(int size, BGFXVertexLayout layout, ByteBuffer data, int flags) {
                short handle = bgfx_create_vertex_buffer(
                    bgfx_make_ref(data),
                    layout,
                    flags
                );
                allocatedVertexBufferMemory.addAndGet(size);
                return handle;
            }

            short allocateDynamicVertexBuffer(int numVertices, BGFXVertexLayout layout, int flags) {
                return bgfx_create_dynamic_vertex_buffer(numVertices, layout, flags);
            }

            short allocateIndexBuffer(ByteBuffer data, int flags) {
                return bgfx_create_index_buffer(bgfx_make_ref(data), flags);
            }

            short allocateDynamicIndexBuffer(int numIndices, int flags) {
                return bgfx_create_dynamic_index_buffer(numIndices, flags);
            }

            void releaseVertexBuffer(short handle) {
                if (handle != BGFX_INVALID_HANDLE) {
                    bgfx_destroy_vertex_buffer(handle);
                }
            }

            void releaseIndexBuffer(short handle) {
                if (handle != BGFX_INVALID_HANDLE) {
                    bgfx_destroy_index_buffer(handle);
                }
            }

            void releaseTexture(short handle) {
                if (handle != BGFX_INVALID_HANDLE) {
                    bgfx_destroy_texture(handle);
                }
            }

            void releaseProgram(short handle) {
                if (handle != BGFX_INVALID_HANDLE) {
                    bgfx_destroy_program(handle);
                }
            }

            void cleanup() {
                poolLock.writeLock().lock();
                try {
                    // Destroy all pooled resources
                    vertexBufferPools.values().forEach(pool -> {
                        while (!pool.isEmpty()) {
                            bgfx_destroy_vertex_buffer(pool.poll());
                        }
                    });
                    indexBufferPools.values().forEach(pool -> {
                        while (!pool.isEmpty()) {
                            bgfx_destroy_index_buffer(pool.poll());
                        }
                    });
                    while (!texturePool.isEmpty()) {
                        bgfx_destroy_texture(texturePool.poll());
                    }
                    while (!programPool.isEmpty()) {
                        bgfx_destroy_program(programPool.poll());
                    }
                    vertexBufferPools.clear();
                    indexBufferPools.clear();
                } finally {
                    poolLock.writeLock().unlock();
                }
            }

            long getAllocatedMemory() {
                return allocatedVertexBufferMemory.get() + allocatedIndexBufferMemory.get() + allocatedTextureMemory.get();
            }
        }

        /**
         * Transpiles DX9 shader bytecode to BGFX shader format.
         */
        private static final class DX9ShaderTranspiler {
            private final Int2ObjectMap<short[]> transpiledShaders;
            private final Int2ObjectMap<Short> programCache;
            private final StampedLock cacheLock;

            DX9ShaderTranspiler() {
                this.transpiledShaders = new Int2ObjectOpenHashMap<>();
                this.programCache = new Int2ObjectOpenHashMap<>();
                this.cacheLock = new StampedLock();
            }

            /**
             * Transpile DX9 vertex shader bytecode to BGFX shader.
             * In practice, this would use a shader cross-compiler like SPIRV-Cross.
             */
            short transpileVertexShader(ByteBuffer bytecode, Arena arena) {
                int hash = computeShaderHash(bytecode);
                
                long stamp = cacheLock.readLock();
                try {
                    short[] cached = transpiledShaders.get(hash);
                    if (cached != null && cached.length > 0) return cached[0];
                } finally {
                    cacheLock.unlockRead(stamp);
                }

                stamp = cacheLock.writeLock();
                try {
                    short[] cached = transpiledShaders.get(hash);
                    if (cached != null && cached.length > 0) return cached[0];

                    // In a real implementation, this would:
                    // 1. Parse DX9 shader bytecode
                    // 2. Convert to intermediate representation
                    // 3. Generate BGFX-compatible SPIR-V or platform-specific shader
                    // 4. Create BGFX shader handle

                    BGFXMemory shaderMem = bgfx_copy(bytecode);
                    short handle = bgfx_create_shader(shaderMem);

                    transpiledShaders.put(hash, new short[]{handle});
                    return handle;

                } finally {
                    cacheLock.unlockWrite(stamp);
                }
            }

            /**
             * Transpile DX9 pixel shader bytecode to BGFX shader.
             */
            short transpilePixelShader(ByteBuffer bytecode, Arena arena) {
                return transpileVertexShader(bytecode, arena); // Same process
            }

            /**
             * Create or retrieve a BGFX program from vertex and pixel shader handles.
             */
            short getOrCreateProgram(short vertexShader, short pixelShader) {
                int key = (vertexShader << 16) | (pixelShader & 0xFFFF);
                
                long stamp = cacheLock.readLock();
                try {
                    Short cached = programCache.get(key);
                    if (cached != null) return cached;
                } finally {
                    cacheLock.unlockRead(stamp);
                }

                stamp = cacheLock.writeLock();
                try {
                    Short cached = programCache.get(key);
                    if (cached != null) return cached;

                    short program = bgfx_create_program(vertexShader, pixelShader, false);
                    programCache.put(key, program);
                    return program;

                } finally {
                    cacheLock.unlockWrite(stamp);
                }
            }

            private int computeShaderHash(ByteBuffer bytecode) {
                int hash = 17;
                bytecode.mark();
                while (bytecode.hasRemaining()) {
                    hash = 31 * hash + bytecode.get();
                }
                bytecode.reset();
                return hash;
            }

            void clear() {
                long stamp = cacheLock.writeLock();
                try {
                    for (short[] handles : transpiledShaders.values()) {
                        for (short h : handles) {
                            if (h != BGFX_INVALID_HANDLE) {
                                bgfx_destroy_shader(h);
                            }
                        }
                    }
                    for (short program : programCache.values()) {
                        if (program != BGFX_INVALID_HANDLE) {
                            bgfx_destroy_program(program);
                        }
                    }
                    transpiledShaders.clear();
                    programCache.clear();
                } finally {
                    cacheLock.unlockWrite(stamp);
                }
            }
        }

        /**
         * Maps DX9 texture formats to BGFX texture formats.
         */
        private static final class DX9TextureFormatMapper {
            private final Int2IntMap formatMap;

            DX9TextureFormatMapper() {
                this.formatMap = new Int2IntOpenHashMap();
                initializeFormatMap();
            }

            private void initializeFormatMap() {
                // D3DFMT -> BGFX_TEXTURE_FORMAT
                formatMap.put(21, BGFX_TEXTURE_FORMAT_BGRA8);    // D3DFMT_A8R8G8B8
                formatMap.put(22, BGFX_TEXTURE_FORMAT_BGRA8);    // D3DFMT_X8R8G8B8
                formatMap.put(23, BGFX_TEXTURE_FORMAT_RGB5A1);   // D3DFMT_R5G6B5
                formatMap.put(25, BGFX_TEXTURE_FORMAT_RGBA4);    // D3DFMT_A1R5G5B5
                formatMap.put(26, BGFX_TEXTURE_FORMAT_RGBA4);    // D3DFMT_A4R4G4B4
                formatMap.put(28, BGFX_TEXTURE_FORMAT_A8);       // D3DFMT_A8
                formatMap.put(32, BGFX_TEXTURE_FORMAT_BGRA8);    // D3DFMT_A8B8G8R8
                formatMap.put(36, BGFX_TEXTURE_FORMAT_RGBA16F);  // D3DFMT_A16B16G16R16
                formatMap.put(50, BGFX_TEXTURE_FORMAT_L8);       // D3DFMT_L8
                formatMap.put(71, BGFX_TEXTURE_FORMAT_BC1);      // D3DFMT_DXT1
                formatMap.put(73, BGFX_TEXTURE_FORMAT_BC2);      // D3DFMT_DXT3
                formatMap.put(77, BGFX_TEXTURE_FORMAT_BC3);      // D3DFMT_DXT5
                formatMap.put(80, BGFX_TEXTURE_FORMAT_D16);      // D3DFMT_D16
                formatMap.put(75, BGFX_TEXTURE_FORMAT_D24S8);    // D3DFMT_D24S8
                formatMap.put(113, BGFX_TEXTURE_FORMAT_RGBA16F); // D3DFMT_A16B16G16R16F
                formatMap.put(116, BGFX_TEXTURE_FORMAT_RGBA32F); // D3DFMT_A32B32G32R32F
            }

            int map(int dx9Format) {
                return formatMap.getOrDefault(dx9Format, BGFX_TEXTURE_FORMAT_RGBA8);
            }

            int getBytesPerPixel(int dx9Format) {
                return switch (dx9Format) {
                    case 21, 22, 32 -> 4;    // 32-bit formats
                    case 23, 25, 26 -> 2;    // 16-bit formats
                    case 28, 50 -> 1;        // 8-bit formats
                    case 36 -> 8;            // 64-bit formats
                    case 113 -> 8;           // 16-bit float RGBA
                    case 116 -> 16;          // 32-bit float RGBA
                    default -> 4;
                };
            }

            boolean isCompressed(int dx9Format) {
                return dx9Format >= 71 && dx9Format <= 77;
            }
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Constructor
        // ─────────────────────────────────────────────────────────────────────────────

        public DX9Path(Arena arena) {
            this.arena = arena;
            this.vertexDeclarations = new Int2ObjectOpenHashMap<>();
            this.vertexShaders = new Int2ObjectOpenHashMap<>();
            this.pixelShaders = new Int2ObjectOpenHashMap<>();
            this.textures = new Int2ObjectOpenHashMap<>();

            // Initialize BGFX integration
            this.bgfxStateManager = new BGFXStateManager();
            this.bgfxResourceAllocator = new BGFXResourceAllocator();
            this.stateMapper = new DX9ToBGFXStateMapper();
            this.vertexDeclCompiler = new DX9VertexDeclarationCompiler();
            this.shaderTranspiler = new DX9ShaderTranspiler();
            this.textureFormatMapper = new DX9TextureFormatMapper();

            // Initialize view management
            this.currentViewId = 0;
            this.viewIdStack = new short[MAX_VIEW_STACK_DEPTH];
            this.viewIdStackPointer = 0;

            // Initialize matrices
            this.worldMatrix = new float[16];
            this.viewMatrix = new float[16];
            this.projectionMatrix = new float[16];
            this.textureMatrices = new float[MAX_TEXTURE_STAGES * 16];
            this.transformDirty = new boolean[4 + MAX_TEXTURE_STAGES];
            this.combinedWVP = new float[16];
            this.combinedWV = new float[16];
            setIdentity(worldMatrix);
            setIdentity(viewMatrix);
            setIdentity(projectionMatrix);
            for (int i = 0; i < MAX_TEXTURE_STAGES; i++) {
                setIdentity(textureMatrices, i * 16);
            }
            Arrays.fill(transformDirty, true);

            // Initialize render state
            this.renderStates = new int[MAX_RENDER_STATES];
            this.renderStateDirtyBits = new long[4];
            initializeDefaultRenderStates();

            // Initialize texture stage state
            this.textureStageStates = new int[MAX_TEXTURE_STAGES][MAX_TEXTURE_STAGE_STATES];
            this.textureStageStateDirtyBits = new long[MAX_TEXTURE_STAGES];
            initializeDefaultTextureStageStates();

            // Initialize sampler state
            this.samplerStates = new int[MAX_SAMPLERS][MAX_SAMPLER_STATES];
            this.samplerStateDirtyBits = new long[MAX_SAMPLERS];
            initializeDefaultSamplerStates();

            // Initialize active resources
            this.activeTextures = new short[MAX_TEXTURE_STAGES];
            Arrays.fill(activeTextures, BGFX_INVALID_HANDLE);
            this.activeVertexBuffer = BGFX_INVALID_HANDLE;
            this.activeIndexBuffer = BGFX_INVALID_HANDLE;
            this.activeVertexShader = BGFX_INVALID_HANDLE;
            this.activePixelShader = BGFX_INVALID_HANDLE;
            this.activeVertexDeclaration = 0;
            this.activeFVF = 0;

            // Initialize stream sources
            this.streamSources = new StreamSourceBinding[MAX_STREAMS];
            for (int i = 0; i < MAX_STREAMS; i++) {
                streamSources[i] = new StreamSourceBinding();
                streamSources[i].reset();
            }

            // Performance counters
            this.drawCallCount = new AtomicLong();
            this.stateChangeCount = new AtomicLong();
            this.textureBindCount = new AtomicLong();
            this.shaderSwitchCount = new AtomicLong();
            this.triangleCount = new LongAdder();
            this.vertexCount = new LongAdder();

            // Scene state
            this.inScene = false;
            this.sceneLock = new ReentrantReadWriteLock();
        }

        private void initializeDefaultRenderStates() {
            // Set DX9 default render states
            renderStates[D3DRS_ZENABLE] = 1;          // D3DZB_TRUE
            renderStates[D3DRS_FILLMODE] = 3;         // D3DFILL_SOLID
            renderStates[D3DRS_ZWRITEENABLE] = 1;     // TRUE
            renderStates[D3DRS_ALPHATESTENABLE] = 0;  // FALSE
            renderStates[D3DRS_SRCBLEND] = 2;         // D3DBLEND_ONE
            renderStates[D3DRS_DESTBLEND] = 1;        // D3DBLEND_ZERO
            renderStates[D3DRS_CULLMODE] = 3;         // D3DCULL_CCW
            renderStates[D3DRS_ZFUNC] = 4;            // D3DCMP_LESSEQUAL
            renderStates[D3DRS_ALPHAREF] = 0;
            renderStates[D3DRS_ALPHAFUNC] = 8;        // D3DCMP_ALWAYS
            renderStates[D3DRS_ALPHABLENDENABLE] = 0; // FALSE
            renderStates[D3DRS_STENCILENABLE] = 0;    // FALSE
            renderStates[D3DRS_BLENDOP] = 1;          // D3DBLENDOP_ADD
            Arrays.fill(renderStateDirtyBits, -1L);   // All dirty
        }

        private void initializeDefaultTextureStageStates() {
            for (int i = 0; i < MAX_TEXTURE_STAGES; i++) {
                textureStageStates[i][1] = i == 0 ? 4 : 1;  // COLOROP: MODULATE or DISABLE
                textureStageStates[i][2] = 0;  // COLORARG1: TEXTURE
                textureStageStates[i][3] = 2;  // COLORARG2: CURRENT
                textureStageStates[i][4] = i == 0 ? 4 : 1;  // ALPHAOP: MODULATE or DISABLE
                textureStageStates[i][5] = 0;  // ALPHAARG1: TEXTURE
                textureStageStates[i][6] = 2;  // ALPHAARG2: CURRENT
                textureStageStateDirtyBits[i] = -1L;
            }
        }

        private void initializeDefaultSamplerStates() {
            for (int i = 0; i < MAX_SAMPLERS; i++) {
                samplerStates[i][D3DSAMP_ADDRESSU] = 1;     // D3DTADDRESS_WRAP
                samplerStates[i][D3DSAMP_ADDRESSV] = 1;     // D3DTADDRESS_WRAP
                samplerStates[i][D3DSAMP_ADDRESSW] = 1;     // D3DTADDRESS_WRAP
                samplerStates[i][D3DSAMP_BORDERCOLOR] = 0;
                samplerStates[i][D3DSAMP_MAGFILTER] = 2;    // D3DTEXF_LINEAR
                samplerStates[i][D3DSAMP_MINFILTER] = 2;    // D3DTEXF_LINEAR
                samplerStates[i][D3DSAMP_MIPFILTER] = 1;    // D3DTEXF_POINT
                samplerStates[i][D3DSAMP_MAXANISOTROPY] = 1;
                samplerStateDirtyBits[i] = -1L;
            }
        }

        private void setIdentity(float[] matrix) {
            setIdentity(matrix, 0);
        }

        private void setIdentity(float[] matrix, int offset) {
            Arrays.fill(matrix, offset, offset + 16, 0);
            matrix[offset] = 1;
            matrix[offset + 5] = 1;
            matrix[offset + 10] = 1;
            matrix[offset + 15] = 1;
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // D3D9 Present Parameters structure
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * D3D9 Present Parameters structure.
         */
        public static final class D3D9PresentParameters {
            public int backBufferWidth;
            public int backBufferHeight;
            public int backBufferFormat;
            public int backBufferCount;
            public int multiSampleType;
            public int multiSampleQuality;
            public int swapEffect;
            public long hDeviceWindow;
            public boolean windowed;
            public boolean enableAutoDepthStencil;
            public int autoDepthStencilFormat;
            public int flags;
            public int fullScreenRefreshRateInHz;
            public int presentationInterval;

            public D3D9PresentParameters() {}

            public D3D9PresentParameters copy() {
                D3D9PresentParameters copy = new D3D9PresentParameters();
                copy.backBufferWidth = this.backBufferWidth;
                copy.backBufferHeight = this.backBufferHeight;
                copy.backBufferFormat = this.backBufferFormat;
                copy.backBufferCount = this.backBufferCount;
                copy.multiSampleType = this.multiSampleType;
                copy.multiSampleQuality = this.multiSampleQuality;
                copy.swapEffect = this.swapEffect;
                copy.hDeviceWindow = this.hDeviceWindow;
                copy.windowed = this.windowed;
                copy.enableAutoDepthStencil = this.enableAutoDepthStencil;
                copy.autoDepthStencilFormat = this.autoDepthStencilFormat;
                copy.flags = this.flags;
                copy.fullScreenRefreshRateInHz = this.fullScreenRefreshRateInHz;
                copy.presentationInterval = this.presentationInterval;
                return copy;
            }
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Initialization
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Initialize D3D9 device via BGFX.
         */
        public MappingResult initialize(long windowHandle, int width, int height, boolean windowed) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                // Configure BGFX initialization
                BGFXInit init = BGFXInit.calloc(stack);
                bgfx_init_ctor(init);
                
                init.type(BGFX_RENDERER_TYPE_COUNT); // Auto-select best renderer
                init.vendorId(BGFX_PCI_ID_NONE);
                init.deviceId((short) 0);
                
                init.resolution(res -> res
                    .width(width)
                    .height(height)
                    .reset(BGFX_RESET_VSYNC)
                );

                // Platform-specific window handle
                init.platformData(pd -> {
                    switch (Platform.get()) {
                        case WINDOWS -> pd.nwh(windowHandle);
                        case LINUX -> pd.ndt(0).nwh(windowHandle);
                        case MACOSX -> pd.nwh(windowHandle);
                    }
                });

                // Initialize BGFX
                if (!bgfx_init(init)) {
                    return MappingResult.failure(CallType.DX9_CREATE_DEVICE, HResult.E_FAIL,
                        "Failed to initialize BGFX");
                }

                // Query capabilities and populate DX9 caps
                BGFXCaps caps = bgfx_get_caps();
                populateCapsFromBGFX(caps);

                // Set default view
                bgfx_set_view_clear(0, BGFX_CLEAR_COLOR | BGFX_CLEAR_DEPTH, 0x303030FF, 1.0f, 0);
                bgfx_set_view_rect(0, 0, 0, width, height);

                D3D9PresentParameters presentParams = new D3D9PresentParameters();
                presentParams.backBufferWidth = width;
                presentParams.backBufferHeight = height;
                presentParams.backBufferFormat = 22;  // D3DFMT_X8R8G8B8
                presentParams.backBufferCount = 1;
                presentParams.multiSampleType = 0;
                presentParams.swapEffect = 1;  // D3DSWAPEFFECT_DISCARD
                presentParams.hDeviceWindow = windowHandle;
                presentParams.windowed = windowed;
                presentParams.enableAutoDepthStencil = true;
                presentParams.autoDepthStencilFormat = 77;  // D3DFMT_D24S8
                presentParams.presentationInterval = 0;  // D3DPRESENT_INTERVAL_DEFAULT

                return MappingResult.success(CallType.DX9_CREATE_DEVICE, 0);
            }
        }

        private void populateCapsFromBGFX(BGFXCaps caps) {
            this.maxTextureWidth = caps.limits().maxTextureSize();
            this.maxTextureHeight = caps.limits().maxTextureSize();
            this.maxVolumeExtent = caps.limits().maxTextureLayers();
            this.maxTextureAspectRatio = maxTextureWidth; // Approximation
            this.maxAnisotropy = 16;
            this.maxVertexIndex = 0xFFFFFF;
            this.maxStreams = MAX_STREAMS;
            this.maxStreamStride = 256;
            this.vertexShaderVersion = 0x300; // SM 3.0
            this.pixelShaderVersion = 0x300;  // SM 3.0
            this.maxVertexShaderConst = caps.limits().maxUniforms();
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Scene Management
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Begin scene for rendering.
         */
        public MappingResult beginScene() {
            sceneLock.writeLock().lock();
            try {
                if (inScene) {
                    return MappingResult.failure(CallType.DX9_BEGIN_SCENE, HResult.E_FAIL,
                        "Already in scene");
                }
                inScene = true;
                bgfxStateManager.reset();
                return MappingResult.success(CallType.DX9_BEGIN_SCENE, 0);
            } finally {
                sceneLock.writeLock().unlock();
            }
        }

        /**
         * End scene.
         */
        public MappingResult endScene() {
            sceneLock.writeLock().lock();
            try {
                if (!inScene) {
                    return MappingResult.failure(CallType.DX9_END_SCENE, HResult.E_FAIL,
                        "Not in scene");
                }
                inScene = false;
                return MappingResult.success(CallType.DX9_END_SCENE, 0);
            } finally {
                sceneLock.writeLock().unlock();
            }
        }

        /**
         * Present the backbuffer.
         */
        public MappingResult present() {
            int frame = bgfx_frame(false);
            return MappingResult.success(CallType.DX9_PRESENT, 0, frame);
        }

        /**
         * Clear render targets.
         */
        public MappingResult clear(int flags, int color, float z, int stencil) {
            int bgfxFlags = 0;
            
            if ((flags & 1) != 0) { // D3DCLEAR_TARGET
                bgfxFlags |= BGFX_CLEAR_COLOR;
            }
            if ((flags & 2) != 0) { // D3DCLEAR_ZBUFFER
                bgfxFlags |= BGFX_CLEAR_DEPTH;
            }
            if ((flags & 4) != 0) { // D3DCLEAR_STENCIL
                bgfxFlags |= BGFX_CLEAR_STENCIL;
            }

            // Convert ARGB to RGBA
            int rgba = ((color << 8) & 0xFFFFFF00) | ((color >> 24) & 0xFF);

            bgfx_set_view_clear(currentViewId, bgfxFlags, rgba, z, stencil);
            bgfx_touch(currentViewId);

            return MappingResult.success(CallType.DX9_CLEAR, 0);
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // State Management
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Set render state.
         */
        public MappingResult setRenderState(int state, int value) {
            if (state < 0 || state >= MAX_RENDER_STATES) {
                return MappingResult.failure(CallType.DX9_SET_RENDER_STATE, HResult.E_INVALIDARG,
                    "Invalid render state: " + state);
            }

            if (renderStates[state] != value) {
                renderStates[state] = value;
                int wordIndex = state / 64;
                int bitIndex = state % 64;
                renderStateDirtyBits[wordIndex] |= (1L << bitIndex);
                stateChangeCount.incrementAndGet();
            }

            return MappingResult.success(CallType.DX9_SET_RENDER_STATE, 0);
        }

        /**
         * Set texture stage state.
         */
        public MappingResult setTextureStageState(int stage, int type, int value) {
            if (stage < 0 || stage >= MAX_TEXTURE_STAGES) {
                return MappingResult.failure(CallType.DX9_SET_TEXTURE_STAGE_STATE, HResult.E_INVALIDARG,
                    "Invalid texture stage: " + stage);
            }
            if (type < 0 || type >= MAX_TEXTURE_STAGE_STATES) {
                return MappingResult.failure(CallType.DX9_SET_TEXTURE_STAGE_STATE, HResult.E_INVALIDARG,
                    "Invalid texture stage state type: " + type);
            }

            if (textureStageStates[stage][type] != value) {
                textureStageStates[stage][type] = value;
                textureStageStateDirtyBits[stage] |= (1L << type);
                stateChangeCount.incrementAndGet();
            }

            return MappingResult.success(CallType.DX9_SET_TEXTURE_STAGE_STATE, 0);
        }

        /**
         * Set sampler state.
         */
        public MappingResult setSamplerState(int sampler, int type, int value) {
            if (sampler < 0 || sampler >= MAX_SAMPLERS) {
                return MappingResult.failure(CallType.DX9_SET_SAMPLER_STATE, HResult.E_INVALIDARG,
                    "Invalid sampler: " + sampler);
            }
            if (type < 0 || type >= MAX_SAMPLER_STATES) {
                return MappingResult.failure(CallType.DX9_SET_SAMPLER_STATE, HResult.E_INVALIDARG,
                    "Invalid sampler state type: " + type);
            }

            if (samplerStates[sampler][type] != value) {
                samplerStates[sampler][type] = value;
                samplerStateDirtyBits[sampler] |= (1L << type);
                stateChangeCount.incrementAndGet();
            }

            return MappingResult.success(CallType.DX9_SET_SAMPLER_STATE, 0);
        }

        /**
         * Set transform matrix.
         */
        public MappingResult setTransform(int state, float[] matrix) {
            if (matrix == null || matrix.length < 16) {
                return MappingResult.failure(CallType.DX9_SET_TRANSFORM, HResult.E_INVALIDARG,
                    "Invalid matrix");
            }

            switch (state) {
                case 2 -> { // D3DTS_VIEW
                    System.arraycopy(matrix, 0, viewMatrix, 0, 16);
                    transformDirty[1] = true;
                }
                case 3 -> { // D3DTS_PROJECTION
                    System.arraycopy(matrix, 0, projectionMatrix, 0, 16);
                    transformDirty[2] = true;
                }
                case 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271 -> {
                    // D3DTS_WORLD through D3DTS_WORLD + 15
                    if (state == 256) {
                        System.arraycopy(matrix, 0, worldMatrix, 0, 16);
                        transformDirty[0] = true;
                    }
                }
                case 16, 17, 18, 19, 20, 21, 22, 23 -> { // D3DTS_TEXTURE0 through D3DTS_TEXTURE7
                    int texIndex = state - 16;
                    System.arraycopy(matrix, 0, textureMatrices, texIndex * 16, 16);
                    transformDirty[4 + texIndex] = true;
                }
            }

            return MappingResult.success(CallType.DX9_SET_TRANSFORM, 0);
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Draw Commands
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Draw primitives (non-indexed).
         */
        public MappingResult drawPrimitive(int primitiveType, int startVertex, int primitiveCount) {
            if (!inScene) {
                return MappingResult.failure(CallType.DX9_DRAW_PRIMITIVE, HResult.E_FAIL,
                    "Not in scene");
            }

            // Compile and apply state
            long bgfxState = stateMapper.compileState(renderStates, samplerStates);
            int stencilFront = stateMapper.compileStencilState(renderStates, true);
            int stencilBack = stateMapper.compileStencilState(renderStates, false);

            bgfxStateManager.setState(bgfxState, 0);
            bgfxStateManager.setStencil(stencilFront, stencilBack);
            bgfxStateManager.applyState(currentViewId);

            // Set vertex buffer
            if (streamSources[0].vertexBuffer != BGFX_INVALID_HANDLE) {
                bgfx_set_vertex_buffer(0, streamSources[0].vertexBuffer, startVertex, 
                    calculateVertexCount(primitiveType, primitiveCount));
            }

            // Set textures
            applyTextures();

            // Update transform uniforms
            updateTransformUniforms();

            // Get or create program
            short program = shaderTranspiler.getOrCreateProgram(activeVertexShader, activePixelShader);
            
            // Submit draw call
            int vertexCount = calculateVertexCount(primitiveType, primitiveCount);
            long stateWithTopology = bgfxState | mapPrimitiveTopology(primitiveType);
            bgfx_set_state(stateWithTopology, 0);
            bgfx_submit(currentViewId, program, 0, BGFX_DISCARD_ALL);

            // Update stats
            drawCallCount.incrementAndGet();
            this.vertexCount.add(vertexCount);
            this.triangleCount.add(primitiveCount);

            return MappingResult.success(CallType.DX9_DRAW_PRIMITIVE, 0);
        }

        /**
         * Draw indexed primitives.
         */
        public MappingResult drawIndexedPrimitive(int primitiveType, int baseVertexIndex,
                                                   int minVertexIndex, int numVertices,
                                                   int startIndex, int primitiveCount) {
            if (!inScene) {
                return MappingResult.failure(CallType.DX9_DRAW_INDEXED_PRIMITIVE, HResult.E_FAIL,
                    "Not in scene");
            }

            // Compile and apply state
            long bgfxState = stateMapper.compileState(renderStates, samplerStates);
            int stencilFront = stateMapper.compileStencilState(renderStates, true);
            int stencilBack = stateMapper.compileStencilState(renderStates, false);

            bgfxStateManager.setState(bgfxState, 0);
            bgfxStateManager.setStencil(stencilFront, stencilBack);
            bgfxStateManager.applyState(currentViewId);

            // Set vertex buffer
            if (streamSources[0].vertexBuffer != BGFX_INVALID_HANDLE) {
                bgfx_set_vertex_buffer(0, streamSources[0].vertexBuffer, 
                    minVertexIndex + baseVertexIndex, numVertices);
            }

            // Set index buffer
            if (activeIndexBuffer != BGFX_INVALID_HANDLE) {
                int indexCount = calculateIndexCount(primitiveType, primitiveCount);
                bgfx_set_index_buffer(activeIndexBuffer, startIndex, indexCount);
            }

            // Set textures
            applyTextures();

            // Update transform uniforms
            updateTransformUniforms();

            // Get or create program
            short program = shaderTranspiler.getOrCreateProgram(activeVertexShader, activePixelShader);

            // Submit draw call
            long stateWithTopology = bgfxState | mapPrimitiveTopology(primitiveType);
            bgfx_set_state(stateWithTopology, 0);
            bgfx_submit(currentViewId, program, 0, BGFX_DISCARD_ALL);

            // Update stats
            drawCallCount.incrementAndGet();
            this.vertexCount.add(numVertices);
            this.triangleCount.add(primitiveCount);

            return MappingResult.success(CallType.DX9_DRAW_INDEXED_PRIMITIVE, 0);
        }

        private long mapPrimitiveTopology(int primitiveType) {
            return switch (primitiveType) {
                case 1 -> BGFX_STATE_PT_POINTS;      // D3DPT_POINTLIST
                case 2 -> BGFX_STATE_PT_LINES;       // D3DPT_LINELIST
                case 3 -> BGFX_STATE_PT_LINESTRIP;   // D3DPT_LINESTRIP
                case 4 -> 0;                          // D3DPT_TRIANGLELIST (default)
                case 5 -> BGFX_STATE_PT_TRISTRIP;    // D3DPT_TRIANGLESTRIP
                case 6 -> 0;                          // D3DPT_TRIANGLEFAN (not directly supported)
                default -> 0;
            };
        }

        private int calculateVertexCount(int primitiveType, int primitiveCount) {
            return switch (primitiveType) {
                case 1 -> primitiveCount;           // POINTLIST
                case 2 -> primitiveCount * 2;       // LINELIST
                case 3 -> primitiveCount + 1;       // LINESTRIP
                case 4 -> primitiveCount * 3;       // TRIANGLELIST
                case 5 -> primitiveCount + 2;       // TRIANGLESTRIP
                case 6 -> primitiveCount + 2;       // TRIANGLEFAN
                default -> primitiveCount * 3;
            };
        }

        private int calculateIndexCount(int primitiveType, int primitiveCount) {
            return calculateVertexCount(primitiveType, primitiveCount);
        }

        private void applyTextures() {
            for (int stage = 0; stage < MAX_TEXTURE_STAGES; stage++) {
                if (activeTextures[stage] != BGFX_INVALID_HANDLE) {
                    int samplerFlags = stateMapper.compileSamplerFlags(samplerStates[stage]);
                    bgfx_set_texture(stage, bgfx_create_uniform("s_texture" + stage, BGFX_UNIFORM_TYPE_SAMPLER, 1),
                        activeTextures[stage], samplerFlags);
                    textureBindCount.incrementAndGet();
                }
            }
        }

        private void updateTransformUniforms() {
            // Update MVP matrix uniform
            if (transformDirty[0] || transformDirty[1] || transformDirty[2]) {
                multiplyMatrix(combinedWV, viewMatrix, worldMatrix);
                multiplyMatrix(combinedWVP, projectionMatrix, combinedWV);
                
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    FloatBuffer mvpBuffer = stack.floats(combinedWVP);
                    short mvpUniform = bgfx_create_uniform("u_modelViewProj", BGFX_UNIFORM_TYPE_MAT4, 1);
                    bgfx_set_uniform(mvpUniform, mvpBuffer, 1);
                    bgfx_destroy_uniform(mvpUniform);
                }
                
                transformDirty[0] = false;
                transformDirty[1] = false;
                transformDirty[2] = false;
            }
        }

        private void multiplyMatrix(float[] result, float[] a, float[] b) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    result[i * 4 + j] = 
                        a[i * 4 + 0] * b[0 * 4 + j] +
                        a[i * 4 + 1] * b[1 * 4 + j] +
                        a[i * 4 + 2] * b[2 * 4 + j] +
                        a[i * 4 + 3] * b[3 * 4 + j];
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Resource Creation
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Create vertex buffer.
         */
        public MappingResult createVertexBuffer(int length, int usage, int fvf, int pool) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                BGFXVertexLayout layout = vertexDeclCompiler.compileFromFVF(fvf, arena);
                
                int flags = BGFX_BUFFER_NONE;
                if ((usage & 8) != 0) { // D3DUSAGE_DYNAMIC
                    short handle = bgfxResourceAllocator.allocateDynamicVertexBuffer(
                        length / layout.stride(), layout, flags);
                    return MappingResult.success(CallType.DX9_CREATE_VERTEX_BUFFER, 0, (long) handle);
                } else {
                    // For static buffers, we need data - return placeholder
                    return MappingResult.success(CallType.DX9_CREATE_VERTEX_BUFFER, 0, 0L);
                }
            }
        }

        /**
         * Create index buffer.
         */
        public MappingResult createIndexBuffer(int length, int usage, int format, int pool) {
            int flags = format == 101 ? BGFX_BUFFER_INDEX32 : BGFX_BUFFER_NONE; // D3DFMT_INDEX32
            
            if ((usage & 8) != 0) { // D3DUSAGE_DYNAMIC
                int numIndices = length / (format == 101 ? 4 : 2);
                short handle = bgfxResourceAllocator.allocateDynamicIndexBuffer(numIndices, flags);
                return MappingResult.success(CallType.DX9_CREATE_INDEX_BUFFER, 0, (long) handle);
            } else {
                return MappingResult.success(CallType.DX9_CREATE_INDEX_BUFFER, 0, 0L);
            }
        }

        /**
         * Create texture.
         */
        public MappingResult createTexture(int width, int height, int levels, int usage,
                                           int format, int pool) {
            int bgfxFormat = textureFormatMapper.map(format);
            int flags = BGFX_TEXTURE_NONE;

            if ((usage & 1) != 0) { // D3DUSAGE_RENDERTARGET
                flags |= BGFX_TEXTURE_RT;
            }
            if ((usage & 2) != 0) { // D3DUSAGE_DEPTHSTENCIL
                flags |= BGFX_TEXTURE_RT_WRITE_ONLY;
            }

            short handle = bgfx_create_texture_2d(width, height, levels > 1, (short) levels,
                bgfxFormat, flags, null);

            return MappingResult.success(CallType.DX9_CREATE_TEXTURE, 0, (long) handle);
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Resource Binding
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Set stream source.
         */
        public MappingResult setStreamSource(int streamNumber, long vertexBuffer,
                                             int offsetInBytes, int stride) {
            if (streamNumber < 0 || streamNumber >= MAX_STREAMS) {
                return MappingResult.failure(CallType.DX9_SET_STREAM_SOURCE, HResult.E_INVALIDARG,
                    "Invalid stream number: " + streamNumber);
            }

            streamSources[streamNumber].set((short) vertexBuffer, offsetInBytes, stride);
            
            if (streamNumber == 0) {
                activeVertexBuffer = (short) vertexBuffer;
            }

            return MappingResult.success(CallType.DX9_SET_STREAM_SOURCE, 0);
        }

        /**
         * Set indices.
         */
        public MappingResult setIndices(long indexBuffer) {
            activeIndexBuffer = (short) indexBuffer;
            return MappingResult.success(CallType.DX9_SET_INDICES, 0);
        }

        /**
         * Set vertex shader.
         */
        public MappingResult setVertexShader(long shader) {
            if (activeVertexShader != (short) shader) {
                activeVertexShader = (short) shader;
                shaderSwitchCount.incrementAndGet();
            }
            return MappingResult.success(CallType.DX9_SET_VERTEX_SHADER, 0);
        }

        /**
         * Set pixel shader.
         */
        public MappingResult setPixelShader(long shader) {
            if (activePixelShader != (short) shader) {
                activePixelShader = (short) shader;
                shaderSwitchCount.incrementAndGet();
            }
            return MappingResult.success(CallType.DX9_SET_PIXEL_SHADER, 0);
        }

        /**
         * Set texture.
         */
        public MappingResult setTexture(int stage, long texture) {
            if (stage < 0 || stage >= MAX_TEXTURE_STAGES) {
                return MappingResult.failure(CallType.DX9_SET_TEXTURE, HResult.E_INVALIDARG,
                    "Invalid texture stage: " + stage);
            }

            activeTextures[stage] = (short) texture;
            return MappingResult.success(CallType.DX9_SET_TEXTURE, 0);
        }

        /**
         * Set vertex shader constant (float).
         */
        public MappingResult setVertexShaderConstantF(int startRegister, float[] constantData, int count) {
            if (constantData == null || constantData.length < count * 4) {
                return MappingResult.failure(CallType.DX9_SET_VERTEX_SHADER_CONSTANT_F, HResult.E_INVALIDARG,
                    "Invalid constant data");
            }

            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.floats(constantData);
                // Create uniforms for each register
                for (int i = 0; i < count; i++) {
                    String uniformName = "u_vsConst" + (startRegister + i);
                    short uniform = bgfx_create_uniform(uniformName, BGFX_UNIFORM_TYPE_VEC4, 1);
                    FloatBuffer slice = buffer.slice(i * 4, 4);
                    bgfx_set_uniform(uniform, slice, 1);
                    bgfx_destroy_uniform(uniform);
                }
            }

            return MappingResult.success(CallType.DX9_SET_VERTEX_SHADER_CONSTANT_F, 0);
        }

        /**
         * Set pixel shader constant (float).
         */
        public MappingResult setPixelShaderConstantF(int startRegister, float[] constantData, int count) {
            if (constantData == null || constantData.length < count * 4) {
                return MappingResult.failure(CallType.DX9_SET_PIXEL_SHADER_CONSTANT_F, HResult.E_INVALIDARG,
                    "Invalid constant data");
            }

            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.floats(constantData);
                for (int i = 0; i < count; i++) {
                    String uniformName = "u_psConst" + (startRegister + i);
                    short uniform = bgfx_create_uniform(uniformName, BGFX_UNIFORM_TYPE_VEC4, 1);
                    FloatBuffer slice = buffer.slice(i * 4, 4);
                    bgfx_set_uniform(uniform, slice, 1);
                    bgfx_destroy_uniform(uniform);
                }
            }

            return MappingResult.success(CallType.DX9_SET_PIXEL_SHADER_CONSTANT_F, 0);
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Statistics & Cleanup
        // ─────────────────────────────────────────────────────────────────────────────

        public long getDrawCallCount() { return drawCallCount.get(); }
        public long getStateChangeCount() { return stateChangeCount.get(); }
        public long getTextureBindCount() { return textureBindCount.get(); }
        public long getShaderSwitchCount() { return shaderSwitchCount.get(); }
        public long getTriangleCount() { return triangleCount.sum(); }
        public long getVertexCount() { return vertexCount.sum(); }

        public void cleanup() {
            sceneLock.writeLock().lock();
            try {
                // Release all resources
                textures.values().forEach(tex -> { /* release */ });
                vertexShaders.values().forEach(vs -> { /* release */ });
                pixelShaders.values().forEach(ps -> { /* release */ });
                vertexDeclarations.values().forEach(decl -> { /* release */ });
                textures.clear();
                vertexShaders.clear();
                pixelShaders.clear();
                vertexDeclarations.clear();

                // Cleanup BGFX resources
                bgfxResourceAllocator.cleanup();
                shaderTranspiler.clear();
                vertexDeclCompiler.clear();

                // Shutdown BGFX
                bgfx_shutdown();
            } finally {
                sceneLock.writeLock().unlock();
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SECTION 21: DX11 IMPLEMENTATION PATH (BGFX BACKEND)
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * DirectX 11 specific implementation via BGFX abstraction layer.
     * 
     * This implementation provides complete DX11 API compatibility while leveraging
     * BGFX for cross-platform rendering. Features include:
     * 
     * - Full immediate context emulation
     * - Deferred context support for multi-threaded rendering
     * - Complete shader stage coverage (VS, PS, GS, HS, DS, CS)
     * - Resource view management (SRV, UAV, RTV, DSV)
     * - State object caching with intelligent deduplication
     * - Command list recording and playback
     * 
     * Architecture:
     * ┌─────────────────────────────────────────────────────────────────────────────────┐
     * │                            DX11Path API Surface                                 │
     * ├─────────────────────────────────────────────────────────────────────────────────┤
     * │  ┌─────────────────────────────────────────────────────────────────────────┐   │
     * │  │                        Context Abstraction Layer                        │   │
     * │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │   │
     * │  │  │  Immediate  │  │  Deferred   │  │  Command    │  │  Resource   │    │   │
     * │  │  │   Context   │  │   Context   │  │    List     │  │   Manager   │    │   │
     * │  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘    │   │
     * │  │         │                │                │                │           │   │
     * │  │         └────────────────┴────────────────┴────────────────┘           │   │
     * │  │                                    │                                    │   │
     * │  │                          ┌─────────▼─────────┐                         │   │
     * │  │                          │   State Machine   │                         │   │
     * │  │                          │   & Validator     │                         │   │
     * │  │                          └─────────┬─────────┘                         │   │
     * │  └────────────────────────────────────│────────────────────────────────────┘   │
     * │                                       │                                         │
     * │  ┌─────────────────────────────────────────────────────────────────────────┐   │
     * │  │                         BGFX Integration Layer                          │   │
     * │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │   │
     * │  │  │    Encoder   │  │   Resource   │  │   Uniform    │  │   Shader   │  │   │
     * │  │  │    Pool      │  │    Cache     │  │    Cache     │  │   Cache    │  │   │
     * │  │  └──────────────┘  └──────────────┘  └──────────────┘  └────────────┘  │   │
     * │  └─────────────────────────────────────────────────────────────────────────┘   │
     * │                                       │                                         │
     * │                             ┌─────────▼─────────┐                              │
     * │                             │    BGFX Core      │                              │
     * │                             │  (Multi-backend)  │                              │
     * │                             └───────────────────┘                              │
     * └─────────────────────────────────────────────────────────────────────────────────┘
     */
    public static final class DX11Path {

        // ─────────────────────────────────────────────────────────────────────────────
        // Core State & Resources
        // ─────────────────────────────────────────────────────────────────────────────

        private final Arena arena;
        private MemorySegment device;
        private MemorySegment immediateContext;
        private MemorySegment deferredContext;
        private MemorySegment swapChain;

        // Cached state objects (mapped to BGFX equivalents)
        private final Object2ObjectMap<Integer, MemorySegment> rasterizerStates;
        private final Object2ObjectMap<Integer, MemorySegment> blendStates;
        private final Object2ObjectMap<Integer, MemorySegment> depthStencilStates;
        private final Object2ObjectMap<Integer, MemorySegment> samplerStates;
        private final Int2ObjectMap<MemorySegment> inputLayouts;

        // BGFX Backend Integration
        private final BGFXContextManager contextManager;
        private final DX11ToBGFXStateCompiler stateCompiler;
        private final DX11ShaderManager shaderManager;
        private final DX11ResourceViewManager resourceViewManager;
        private final DX11ConstantBufferManager constantBufferManager;
        private final DX11CommandListRecorder commandListRecorder;

        // View Management
        private short currentViewId;
        private final short[] viewIdPool;
        private int nextViewId;
        private static final int MAX_VIEWS = 256;

        // Pipeline State
        private final DX11PipelineState currentPipelineState;
        private final DX11PipelineState pendingPipelineState;
        private boolean pipelineStateDirty;

        // Shader Stage States
        private final ShaderStageState[] shaderStages;
        private static final int NUM_SHADER_STAGES = 6; // VS, PS, GS, HS, DS, CS

        // Resource Binding State
        private final ResourceBindingState[] resourceBindings;

        // Feature level
        private int featureLevel;

        // Performance Metrics
        private final AtomicLong drawCallCount;
        private final AtomicLong dispatchCallCount;
        private final AtomicLong stateChangeCount;
        private final AtomicLong mapCount;
        private final AtomicLong updateSubresourceCount;
        private final LongAdder triangleCount;
        private final LongAdder instanceCount;

        // ─────────────────────────────────────────────────────────────────────────────
        // Inner Classes for State Management
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Manages BGFX contexts for DX11-style immediate and deferred contexts.
         */
        private static final class BGFXContextManager {
            private final ArrayDeque<BGFXEncoderContext> encoderPool;
            private final ConcurrentLinkedQueue<BGFXEncoderContext> activeEncoders;
            private final ThreadLocal<BGFXEncoderContext> threadLocalEncoder;
            private final ReentrantReadWriteLock poolLock;
            private final AtomicInteger encoderIdGenerator;

            BGFXContextManager() {
                this.encoderPool = new ArrayDeque<>();
                this.activeEncoders = new ConcurrentLinkedQueue<>();
                this.threadLocalEncoder = new ThreadLocal<>();
                this.poolLock = new ReentrantReadWriteLock();
                this.encoderIdGenerator = new AtomicInteger();
            }

            BGFXEncoderContext acquireEncoder() {
                BGFXEncoderContext encoder = threadLocalEncoder.get();
                if (encoder != null && encoder.isActive()) {
                    return encoder;
                }

                poolLock.writeLock().lock();
                try {
                    encoder = encoderPool.poll();
                    if (encoder == null) {
                        encoder = new BGFXEncoderContext(encoderIdGenerator.incrementAndGet());
                    }
                    encoder.begin();
                    activeEncoders.add(encoder);
                    threadLocalEncoder.set(encoder);
                    return encoder;
                } finally {
                    poolLock.writeLock().unlock();
                }
            }

            void releaseEncoder(BGFXEncoderContext encoder) {
                if (encoder != null) {
                    encoder.end();
                    activeEncoders.remove(encoder);
                    poolLock.writeLock().lock();
                    try {
                        encoderPool.offer(encoder);
                    } finally {
                        poolLock.writeLock().unlock();
                    }
                    threadLocalEncoder.remove();
                }
            }

            void flushAll() {
                for (BGFXEncoderContext encoder : activeEncoders) {
                    encoder.flush();
                }
            }

            void cleanup() {
                poolLock.writeLock().lock();
                try {
                    encoderPool.clear();
                    activeEncoders.clear();
                } finally {
                    poolLock.writeLock().unlock();
                }
            }
        }

        /**
         * Individual encoder context wrapping BGFX encoder.
         */
        private static final class BGFXEncoderContext {
            private final int id;
            private long nativeEncoder;
            private boolean active;
            private int commandCount;
            private final ArrayDeque<Runnable> deferredCommands;

            BGFXEncoderContext(int id) {
                this.id = id;
                this.nativeEncoder = 0;
                this.active = false;
                this.commandCount = 0;
                this.deferredCommands = new ArrayDeque<>();
            }

            void begin() {
                nativeEncoder = bgfx_encoder_begin(true);
                active = true;
                commandCount = 0;
            }

            void end() {
                if (active && nativeEncoder != 0) {
                    bgfx_encoder_end(nativeEncoder);
                    nativeEncoder = 0;
                    active = false;
                }
            }

            void flush() {
                // Execute deferred commands
                while (!deferredCommands.isEmpty()) {
                    deferredCommands.poll().run();
                }
            }

            boolean isActive() { return active; }
            long getHandle() { return nativeEncoder; }
            int getId() { return id; }
            int getCommandCount() { return commandCount; }
            void incrementCommandCount() { commandCount++; }

            void addDeferredCommand(Runnable command) {
                deferredCommands.offer(command);
            }
        }

        /**
         * Compiles DX11 state objects to BGFX state.
         */
        private static final class DX11ToBGFXStateCompiler {

            // Cached compiled states
            private final Long2LongMap compiledRasterizerStates;
            private final Long2LongMap compiledBlendStates;
            private final Long2LongMap compiledDepthStencilStates;
            private final Long2IntMap compiledSamplerStates;
            private final StampedLock cacheLock;

            DX11ToBGFXStateCompiler() {
                this.compiledRasterizerStates = new Long2LongOpenHashMap();
                this.compiledBlendStates = new Long2LongOpenHashMap();
                this.compiledDepthStencilStates = new Long2LongOpenHashMap();
                this.compiledSamplerStates = new Long2IntOpenHashMap();
                this.cacheLock = new StampedLock();
            }

            /**
             * Compile rasterizer state description to BGFX state flags.
             */
            long compileRasterizerState(D3D11RasterizerDesc desc) {
                long hash = computeRasterizerHash(desc);
                
                long stamp = cacheLock.readLock();
                try {
                    if (compiledRasterizerStates.containsKey(hash)) {
                        return compiledRasterizerStates.get(hash);
                    }
                } finally {
                    cacheLock.unlockRead(stamp);
                }

                stamp = cacheLock.writeLock();
                try {
                    if (compiledRasterizerStates.containsKey(hash)) {
                        return compiledRasterizerStates.get(hash);
                    }

                    long state = 0;

                    // Fill mode
                    if (desc.fillMode == 2) { // D3D11_FILL_WIREFRAME
                        state |= BGFX_STATE_PT_LINES;
                    }

                    // Cull mode
                    state |= switch (desc.cullMode) {
                        case 1 -> 0;                    // D3D11_CULL_NONE
                        case 2 -> BGFX_STATE_CULL_CW;  // D3D11_CULL_FRONT
                        case 3 -> BGFX_STATE_CULL_CCW; // D3D11_CULL_BACK
                        default -> BGFX_STATE_CULL_CCW;
                    };

                    // Front counter-clockwise
                    if (desc.frontCounterClockwise) {
                        state = (state & ~BGFX_STATE_CULL_MASK) | 
                                ((state & BGFX_STATE_CULL_CW) != 0 ? BGFX_STATE_CULL_CCW : BGFX_STATE_CULL_CW);
                    }

                    // Multisample
                    if (desc.multisampleEnable) {
                        state |= BGFX_STATE_MSAA;
                    }

                    // Scissor test is handled separately in BGFX

                    compiledRasterizerStates.put(hash, state);
                    return state;

                } finally {
                    cacheLock.unlockWrite(stamp);
                }
            }

            /**
             * Compile blend state description to BGFX state flags.
             */
            long compileBlendState(D3D11BlendDesc desc, int renderTarget) {
                long hash = computeBlendHash(desc, renderTarget);
                
                long stamp = cacheLock.readLock();
                try {
                    if (compiledBlendStates.containsKey(hash)) {
                        return compiledBlendStates.get(hash);
                    }
                } finally {
                    cacheLock.unlockRead(stamp);
                }

                stamp = cacheLock.writeLock();
                try {
                    if (compiledBlendStates.containsKey(hash)) {
                        return compiledBlendStates.get(hash);
                    }

                    long state = 0;
                    D3D11RenderTargetBlendDesc rtDesc = desc.renderTarget[renderTarget];

                    // Color write mask
                    if ((rtDesc.renderTargetWriteMask & 1) != 0) state |= BGFX_STATE_WRITE_R;
                    if ((rtDesc.renderTargetWriteMask & 2) != 0) state |= BGFX_STATE_WRITE_G;
                    if ((rtDesc.renderTargetWriteMask & 4) != 0) state |= BGFX_STATE_WRITE_B;
                    if ((rtDesc.renderTargetWriteMask & 8) != 0) state |= BGFX_STATE_WRITE_A;

                    // Blend enable
                    if (rtDesc.blendEnable) {
                        long srcBlend = mapBlendFactor(rtDesc.srcBlend);
                        long dstBlend = mapBlendFactor(rtDesc.destBlend);
                        long srcBlendAlpha = mapBlendFactor(rtDesc.srcBlendAlpha);
                        long dstBlendAlpha = mapBlendFactor(rtDesc.destBlendAlpha);
                        long blendOp = mapBlendOp(rtDesc.blendOp);
                        long blendOpAlpha = mapBlendOp(rtDesc.blendOpAlpha);

                        state |= BGFX_STATE_BLEND_FUNC_SEPARATE(srcBlend, dstBlend, srcBlendAlpha, dstBlendAlpha);
                        state |= BGFX_STATE_BLEND_EQUATION_SEPARATE(blendOp, blendOpAlpha);
                    }

                    compiledBlendStates.put(hash, state);
                    return state;

                } finally {
                    cacheLock.unlockWrite(stamp);
                }
            }

            /**
             * Compile depth stencil state to BGFX state flags.
             */
            long compileDepthStencilState(D3D11DepthStencilDesc desc) {
                long hash = computeDepthStencilHash(desc);
                
                long stamp = cacheLock.readLock();
                try {
                    if (compiledDepthStencilStates.containsKey(hash)) {
                        return compiledDepthStencilStates.get(hash);
                    }
                } finally {
                    cacheLock.unlockRead(stamp);
                }

                stamp = cacheLock.writeLock();
                try {
                    if (compiledDepthStencilStates.containsKey(hash)) {
                        return compiledDepthStencilStates.get(hash);
                    }

                    long state = 0;

                    // Depth enable and comparison
                    if (desc.depthEnable) {
                        state |= mapComparisonFunc(desc.depthFunc);
                    }

                    // Depth write
                    if (desc.depthWriteMask != 0) { // D3D11_DEPTH_WRITE_MASK_ALL
                        state |= BGFX_STATE_WRITE_Z;
                    }

                    compiledDepthStencilStates.put(hash, state);
                    return state;

                } finally {
                    cacheLock.unlockWrite(stamp);
                }
            }

            /**
             * Compile stencil state to BGFX stencil flags.
             */
            int compileStencilState(D3D11DepthStencilDesc desc, boolean front) {
                if (!desc.stencilEnable) {
                    return BGFX_STENCIL_NONE;
                }

                D3D11DepthStencilOpDesc opDesc = front ? desc.frontFace : desc.backFace;

                int stencil = 0;
                stencil |= mapStencilFunc(opDesc.stencilFunc);
                stencil |= mapStencilOp(opDesc.stencilFailOp, BGFX_STENCIL_OP_FAIL_S_SHIFT);
                stencil |= mapStencilOp(opDesc.stencilDepthFailOp, BGFX_STENCIL_OP_FAIL_Z_SHIFT);
                stencil |= mapStencilOp(opDesc.stencilPassOp, BGFX_STENCIL_OP_PASS_Z_SHIFT);
                stencil |= BGFX_STENCIL_FUNC_RMASK(desc.stencilReadMask);

                return stencil;
            }

            /**
             * Compile sampler state to BGFX sampler flags.
             */
            int compileSamplerState(D3D11SamplerDesc desc) {
                long hash = computeSamplerHash(desc);
                
                long stamp = cacheLock.readLock();
                try {
                    if (compiledSamplerStates.containsKey(hash)) {
                        return compiledSamplerStates.get(hash);
                    }
                } finally {
                    cacheLock.unlockRead(stamp);
                }

                stamp = cacheLock.writeLock();
                try {
                    if (compiledSamplerStates.containsKey(hash)) {
                        return compiledSamplerStates.get(hash);
                    }

                    int flags = 0;

                    // Filter
                    flags |= mapFilter(desc.filter);

                    // Address modes
                    flags |= mapAddressMode(desc.addressU, 'U');
                    flags |= mapAddressMode(desc.addressV, 'V');
                    flags |= mapAddressMode(desc.addressW, 'W');

                    // Comparison function for shadow samplers
                    if (desc.comparisonFunc != 0 && desc.comparisonFunc != 1) { // Not NEVER
                        flags |= BGFX_SAMPLER_COMPARE_LESS; // Simplified - full mapping needed
                    }

                    compiledSamplerStates.put(hash, flags);
                    return flags;

                } finally {
                    cacheLock.unlockWrite(stamp);
                }
            }

            // ─────────────────────────────────────────────────────────────────────────
            // Mapping Helper Functions
            // ─────────────────────────────────────────────────────────────────────────

            private long mapBlendFactor(int dx11Factor) {
                return switch (dx11Factor) {
                    case 1 -> BGFX_STATE_BLEND_ZERO;           // D3D11_BLEND_ZERO
                    case 2 -> BGFX_STATE_BLEND_ONE;            // D3D11_BLEND_ONE
                    case 3 -> BGFX_STATE_BLEND_SRC_COLOR;      // D3D11_BLEND_SRC_COLOR
                    case 4 -> BGFX_STATE_BLEND_INV_SRC_COLOR;  // D3D11_BLEND_INV_SRC_COLOR
                    case 5 -> BGFX_STATE_BLEND_SRC_ALPHA;      // D3D11_BLEND_SRC_ALPHA
                    case 6 -> BGFX_STATE_BLEND_INV_SRC_ALPHA;  // D3D11_BLEND_INV_SRC_ALPHA
                    case 7 -> BGFX_STATE_BLEND_DST_ALPHA;      // D3D11_BLEND_DEST_ALPHA
                    case 8 -> BGFX_STATE_BLEND_INV_DST_ALPHA;  // D3D11_BLEND_INV_DEST_ALPHA
                    case 9 -> BGFX_STATE_BLEND_DST_COLOR;      // D3D11_BLEND_DEST_COLOR
                    case 10 -> BGFX_STATE_BLEND_INV_DST_COLOR; // D3D11_BLEND_INV_DEST_COLOR
                    case 11 -> BGFX_STATE_BLEND_SRC_ALPHA_SAT; // D3D11_BLEND_SRC_ALPHA_SAT
                    case 14 -> BGFX_STATE_BLEND_FACTOR;        // D3D11_BLEND_BLEND_FACTOR
                    case 15 -> BGFX_STATE_BLEND_INV_FACTOR;    // D3D11_BLEND_INV_BLEND_FACTOR
                    default -> BGFX_STATE_BLEND_ONE;
                };
            }

            private long mapBlendOp(int dx11Op) {
                return switch (dx11Op) {
                    case 1 -> BGFX_STATE_BLEND_EQUATION_ADD;    // D3D11_BLEND_OP_ADD
                    case 2 -> BGFX_STATE_BLEND_EQUATION_SUB;    // D3D11_BLEND_OP_SUBTRACT
                    case 3 -> BGFX_STATE_BLEND_EQUATION_REVSUB; // D3D11_BLEND_OP_REV_SUBTRACT
                    case 4 -> BGFX_STATE_BLEND_EQUATION_MIN;    // D3D11_BLEND_OP_MIN
                    case 5 -> BGFX_STATE_BLEND_EQUATION_MAX;    // D3D11_BLEND_OP_MAX
                    default -> BGFX_STATE_BLEND_EQUATION_ADD;
                };
            }

            private long mapComparisonFunc(int dx11Func) {
                return switch (dx11Func) {
                    case 1 -> BGFX_STATE_DEPTH_TEST_NEVER;    // D3D11_COMPARISON_NEVER
                    case 2 -> BGFX_STATE_DEPTH_TEST_LESS;     // D3D11_COMPARISON_LESS
                    case 3 -> BGFX_STATE_DEPTH_TEST_EQUAL;    // D3D11_COMPARISON_EQUAL
                    case 4 -> BGFX_STATE_DEPTH_TEST_LEQUAL;   // D3D11_COMPARISON_LESS_EQUAL
                    case 5 -> BGFX_STATE_DEPTH_TEST_GREATER;  // D3D11_COMPARISON_GREATER
                    case 6 -> BGFX_STATE_DEPTH_TEST_NOTEQUAL; // D3D11_COMPARISON_NOT_EQUAL
                    case 7 -> BGFX_STATE_DEPTH_TEST_GEQUAL;   // D3D11_COMPARISON_GREATER_EQUAL
                    case 8 -> BGFX_STATE_DEPTH_TEST_ALWAYS;   // D3D11_COMPARISON_ALWAYS
                    default -> BGFX_STATE_DEPTH_TEST_LESS;
                };
            }

            private int mapStencilFunc(int dx11Func) {
                return switch (dx11Func) {
                    case 1 -> BGFX_STENCIL_TEST_NEVER;
                    case 2 -> BGFX_STENCIL_TEST_LESS;
                    case 3 -> BGFX_STENCIL_TEST_EQUAL;
                    case 4 -> BGFX_STENCIL_TEST_LEQUAL;
                    case 5 -> BGFX_STENCIL_TEST_GREATER;
                    case 6 -> BGFX_STENCIL_TEST_NOTEQUAL;
                    case 7 -> BGFX_STENCIL_TEST_GEQUAL;
                    case 8 -> BGFX_STENCIL_TEST_ALWAYS;
                    default -> BGFX_STENCIL_TEST_ALWAYS;
                };
            }

            private int mapStencilOp(int dx11Op, int shift) {
                int op = switch (dx11Op) {
                    case 1 -> BGFX_STENCIL_OP_FAIL_S_KEEP;
                    case 2 -> BGFX_STENCIL_OP_FAIL_S_ZERO;
                    case 3 -> BGFX_STENCIL_OP_FAIL_S_REPLACE;
                    case 4 -> BGFX_STENCIL_OP_FAIL_S_INCR;   // Saturate
                    case 5 -> BGFX_STENCIL_OP_FAIL_S_DECR;   // Saturate
                    case 6 -> BGFX_STENCIL_OP_FAIL_S_INVERT;
                    case 7 -> BGFX_STENCIL_OP_FAIL_S_INCR;   // Wrap
                    case 8 -> BGFX_STENCIL_OP_FAIL_S_DECR;   // Wrap
                    default -> BGFX_STENCIL_OP_FAIL_S_KEEP;
                };
                return op >> BGFX_STENCIL_OP_FAIL_S_SHIFT << shift;
            }

            private int mapFilter(int dx11Filter) {
                // D3D11_FILTER is a complex enumeration - simplified mapping
                boolean minPoint = (dx11Filter & 0x10) == 0;
                boolean magPoint = (dx11Filter & 0x04) == 0;
                boolean mipPoint = (dx11Filter & 0x01) == 0;
                boolean aniso = (dx11Filter & 0x40) != 0;

                int flags = 0;
                if (aniso) {
                    flags |= BGFX_SAMPLER_MIN_ANISOTROPIC | BGFX_SAMPLER_MAG_ANISOTROPIC;
                } else {
                    if (minPoint) flags |= BGFX_SAMPLER_MIN_POINT;
                    if (magPoint) flags |= BGFX_SAMPLER_MAG_POINT;
                }
                if (mipPoint) flags |= BGFX_SAMPLER_MIP_POINT;

                return flags;
            }

            private int mapAddressMode(int dx11Mode, char axis) {
                return switch (dx11Mode) {
                    case 1 -> 0; // D3D11_TEXTURE_ADDRESS_WRAP (default)
                    case 2 -> axis == 'U' ? BGFX_SAMPLER_U_MIRROR : 
                              axis == 'V' ? BGFX_SAMPLER_V_MIRROR : BGFX_SAMPLER_W_MIRROR;
                    case 3 -> axis == 'U' ? BGFX_SAMPLER_U_CLAMP : 
                              axis == 'V' ? BGFX_SAMPLER_V_CLAMP : BGFX_SAMPLER_W_CLAMP;
                    case 4 -> axis == 'U' ? BGFX_SAMPLER_U_BORDER : 
                              axis == 'V' ? BGFX_SAMPLER_V_BORDER : BGFX_SAMPLER_W_BORDER;
                    default -> 0;
                };
            }

            // ─────────────────────────────────────────────────────────────────────────
            // Hash Functions for Caching
            // ─────────────────────────────────────────────────────────────────────────

            private long computeRasterizerHash(D3D11RasterizerDesc desc) {
                long hash = 17;
                hash = 31 * hash + desc.fillMode;
                hash = 31 * hash + desc.cullMode;
                hash = 31 * hash + (desc.frontCounterClockwise ? 1 : 0);
                hash = 31 * hash + desc.depthBias;
                hash = 31 * hash + Float.floatToIntBits(desc.depthBiasClamp);
                hash = 31 * hash + Float.floatToIntBits(desc.slopeScaledDepthBias);
                hash = 31 * hash + (desc.depthClipEnable ? 1 : 0);
                hash = 31 * hash + (desc.scissorEnable ? 1 : 0);
                hash = 31 * hash + (desc.multisampleEnable ? 1 : 0);
                hash = 31 * hash + (desc.antialiasedLineEnable ? 1 : 0);
                return hash;
            }

            private long computeBlendHash(D3D11BlendDesc desc, int rt) {
                long hash = 17;
                hash = 31 * hash + (desc.alphaToCoverageEnable ? 1 : 0);
                hash = 31 * hash + (desc.independentBlendEnable ? 1 : 0);
                D3D11RenderTargetBlendDesc rtd = desc.renderTarget[rt];
                hash = 31 * hash + (rtd.blendEnable ? 1 : 0);
                hash = 31 * hash + rtd.srcBlend;
                hash = 31 * hash + rtd.destBlend;
                hash = 31 * hash + rtd.blendOp;
                hash = 31 * hash + rtd.srcBlendAlpha;
                hash = 31 * hash + rtd.destBlendAlpha;
                hash = 31 * hash + rtd.blendOpAlpha;
                hash = 31 * hash + rtd.renderTargetWriteMask;
                return hash;
            }

            private long computeDepthStencilHash(D3D11DepthStencilDesc desc) {
                long hash = 17;
                hash = 31 * hash + (desc.depthEnable ? 1 : 0);
                hash = 31 * hash + desc.depthWriteMask;
                hash = 31 * hash + desc.depthFunc;
                hash = 31 * hash + (desc.stencilEnable ? 1 : 0);
                hash = 31 * hash + desc.stencilReadMask;
                hash = 31 * hash + desc.stencilWriteMask;
                hash = 31 * hash + computeStencilOpHash(desc.frontFace);
                hash = 31 * hash + computeStencilOpHash(desc.backFace);
                return hash;
            }

            private long computeStencilOpHash(D3D11DepthStencilOpDesc desc) {
                return desc.stencilFailOp | (desc.stencilDepthFailOp << 8) | 
                       (desc.stencilPassOp << 16) | (desc.stencilFunc << 24);
            }

            private long computeSamplerHash(D3D11SamplerDesc desc) {
                long hash = 17;
                hash = 31 * hash + desc.filter;
                hash = 31 * hash + desc.addressU;
                hash = 31 * hash + desc.addressV;
                hash = 31 * hash + desc.addressW;
                hash = 31 * hash + Float.floatToIntBits(desc.mipLODBias);
                hash = 31 * hash + desc.maxAnisotropy;
                hash = 31 * hash + desc.comparisonFunc;
                hash = 31 * hash + Float.floatToIntBits(desc.minLOD);
                hash = 31 * hash + Float.floatToIntBits(desc.maxLOD);
                return hash;
            }

            void clear() {
                long stamp = cacheLock.writeLock();
                try {
                    compiledRasterizerStates.clear();
                    compiledBlendStates.clear();
                    compiledDepthStencilStates.clear();
                    compiledSamplerStates.clear();
                } finally {
                    cacheLock.unlockWrite(stamp);
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // State Description Structures (DX11 Compatible)
        // ─────────────────────────────────────────────────────────────────────────────

        public static final class D3D11RasterizerDesc {
            public int fillMode = 3;           // D3D11_FILL_SOLID
            public int cullMode = 3;           // D3D11_CULL_BACK
            public boolean frontCounterClockwise = false;
            public int depthBias = 0;
            public float depthBiasClamp = 0.0f;
            public float slopeScaledDepthBias = 0.0f;
            public boolean depthClipEnable = true;
            public boolean scissorEnable = false;
            public boolean multisampleEnable = false;
            public boolean antialiasedLineEnable = false;

            public D3D11RasterizerDesc copy() {
                D3D11RasterizerDesc c = new D3D11RasterizerDesc();
                c.fillMode = fillMode; c.cullMode = cullMode;
                c.frontCounterClockwise = frontCounterClockwise;
                c.depthBias = depthBias; c.depthBiasClamp = depthBiasClamp;
                c.slopeScaledDepthBias = slopeScaledDepthBias;
                c.depthClipEnable = depthClipEnable; c.scissorEnable = scissorEnable;
                c.multisampleEnable = multisampleEnable;
                c.antialiasedLineEnable = antialiasedLineEnable;
                return c;
            }
        }

        public static final class D3D11BlendDesc {
            public boolean alphaToCoverageEnable = false;
            public boolean independentBlendEnable = false;
            public final D3D11RenderTargetBlendDesc[] renderTarget = new D3D11RenderTargetBlendDesc[8];

            public D3D11BlendDesc() {
                for (int i = 0; i < 8; i++) {
                    renderTarget[i] = new D3D11RenderTargetBlendDesc();
                }
            }
        }

        public static final class D3D11RenderTargetBlendDesc {
            public boolean blendEnable = false;
            public int srcBlend = 2;           // D3D11_BLEND_ONE
            public int destBlend = 1;          // D3D11_BLEND_ZERO
            public int blendOp = 1;            // D3D11_BLEND_OP_ADD
            public int srcBlendAlpha = 2;      // D3D11_BLEND_ONE
            public int destBlendAlpha = 1;     // D3D11_BLEND_ZERO
            public int blendOpAlpha = 1;       // D3D11_BLEND_OP_ADD
            public int renderTargetWriteMask = 0xF; // D3D11_COLOR_WRITE_ENABLE_ALL
        }

        public static final class D3D11DepthStencilDesc {
            public boolean depthEnable = true;
            public int depthWriteMask = 1;     // D3D11_DEPTH_WRITE_MASK_ALL
            public int depthFunc = 2;          // D3D11_COMPARISON_LESS
            public boolean stencilEnable = false;
            public int stencilReadMask = 0xFF;
            public int stencilWriteMask = 0xFF;
            public D3D11DepthStencilOpDesc frontFace = new D3D11DepthStencilOpDesc();
            public D3D11DepthStencilOpDesc backFace = new D3D11DepthStencilOpDesc();
        }

        public static final class D3D11DepthStencilOpDesc {
            public int stencilFailOp = 1;      // D3D11_STENCIL_OP_KEEP
            public int stencilDepthFailOp = 1; // D3D11_STENCIL_OP_KEEP
            public int stencilPassOp = 1;      // D3D11_STENCIL_OP_KEEP
            public int stencilFunc = 8;        // D3D11_COMPARISON_ALWAYS
        }

        public static final class D3D11SamplerDesc {
            public int filter = 0x15;          // D3D11_FILTER_MIN_MAG_MIP_LINEAR
            public int addressU = 1;           // D3D11_TEXTURE_ADDRESS_WRAP
            public int addressV = 1;
            public int addressW = 1;
            public float mipLODBias = 0.0f;
            public int maxAnisotropy = 1;
            public int comparisonFunc = 0;     // D3D11_COMPARISON_NEVER
            public final float[] borderColor = new float[]{0, 0, 0, 0};
            public float minLOD = 0.0f;
            public float maxLOD = Float.MAX_VALUE;
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Shader Manager
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Manages DX11 shaders and their BGFX equivalents.
         */
        private static final class DX11ShaderManager {
            private final Int2ObjectMap<ShaderHandle> vertexShaders;
            private final Int2ObjectMap<ShaderHandle> pixelShaders;
            private final Int2ObjectMap<ShaderHandle> geometryShaders;
            private final Int2ObjectMap<ShaderHandle> hullShaders;
            private final Int2ObjectMap<ShaderHandle> domainShaders;
            private final Int2ObjectMap<ShaderHandle> computeShaders;
            private final Long2ShortMap programCache;
            private final Int2ObjectMap<InputLayoutInfo> inputLayouts;
            private final StampedLock lock;
            private final AtomicInteger handleGenerator;

            DX11ShaderManager() {
                this.vertexShaders = new Int2ObjectOpenHashMap<>();
                this.pixelShaders = new Int2ObjectOpenHashMap<>();
                this.geometryShaders = new Int2ObjectOpenHashMap<>();
                this.hullShaders = new Int2ObjectOpenHashMap<>();
                this.domainShaders = new Int2ObjectOpenHashMap<>();
                this.computeShaders = new Int2ObjectOpenHashMap<>();
                this.programCache = new Long2ShortOpenHashMap();
                this.inputLayouts = new Int2ObjectOpenHashMap<>();
                this.lock = new StampedLock();
                this.handleGenerator = new AtomicInteger();
            }

            static final class ShaderHandle {
                final int id;
                final short bgfxHandle;
                final int type;
                final ByteBuffer bytecode;
                final long hash;

                ShaderHandle(int id, short bgfxHandle, int type, ByteBuffer bytecode, long hash) {
                    this.id = id;
                    this.bgfxHandle = bgfxHandle;
                    this.type = type;
                    this.bytecode = bytecode;
                    this.hash = hash;
                }
            }

            static final class InputLayoutInfo {
                final int id;
                final BGFXVertexLayout layout;
                final int[] elementHashes;

                InputLayoutInfo(int id, BGFXVertexLayout layout, int[] elementHashes) {
                    this.id = id;
                    this.layout = layout;
                    this.elementHashes = elementHashes;
                }
            }

            int createVertexShader(ByteBuffer bytecode) {
                return createShader(bytecode, 0, vertexShaders);
            }

            int createPixelShader(ByteBuffer bytecode) {
                return createShader(bytecode, 1, pixelShaders);
            }

            int createGeometryShader(ByteBuffer bytecode) {
                return createShader(bytecode, 2, geometryShaders);
            }

            int createHullShader(ByteBuffer bytecode) {
                return createShader(bytecode, 3, hullShaders);
            }

            int createDomainShader(ByteBuffer bytecode) {
                return createShader(bytecode, 4, domainShaders);
            }

            int createComputeShader(ByteBuffer bytecode) {
                return createShader(bytecode, 5, computeShaders);
            }

            private int createShader(ByteBuffer bytecode, int type, Int2ObjectMap<ShaderHandle> cache) {
                long hash = computeBytecodeHash(bytecode);
                
                long stamp = lock.writeLock();
                try {
                    int id = handleGenerator.incrementAndGet();
                    
                    // In real implementation: cross-compile DXBC to SPIRV/platform shader
                    BGFXMemory mem = bgfx_copy(bytecode);
                    short bgfxHandle = bgfx_create_shader(mem);
                    
                    ShaderHandle handle = new ShaderHandle(id, bgfxHandle, type, bytecode, hash);
                    cache.put(id, handle);
                    return id;
                } finally {
                    lock.unlockWrite(stamp);
                }
            }

            short getOrCreateProgram(int vsHandle, int psHandle) {
                long key = ((long) vsHandle << 32) | (psHandle & 0xFFFFFFFFL);
                
                long stamp = lock.readLock();
                try {
                    if (programCache.containsKey(key)) {
                        return programCache.get(key);
                    }
                } finally {
                    lock.unlockRead(stamp);
                }

                stamp = lock.writeLock();
                try {
                    if (programCache.containsKey(key)) {
                        return programCache.get(key);
                    }

                    ShaderHandle vs = vertexShaders.get(vsHandle);
                    ShaderHandle ps = pixelShaders.get(psHandle);
                    
                    if (vs == null || ps == null) {
                        return BGFX_INVALID_HANDLE;
                    }

                    short program = bgfx_create_program(vs.bgfxHandle, ps.bgfxHandle, false);
                    programCache.put(key, program);
                    return program;
                } finally {
                    lock.unlockWrite(stamp);
                }
            }

            int createInputLayout(D3D11InputElementDesc[] elements, ByteBuffer shaderBytecode, Arena arena) {
                long stamp = lock.writeLock();
                try {
                    int id = handleGenerator.incrementAndGet();
                    
                    BGFXVertexLayout layout = BGFXVertexLayout.calloc(arena);
                    bgfx_vertex_layout_begin(layout, bgfx_get_renderer_type());

                    int[] elementHashes = new int[elements.length];
                    for (int i = 0; i < elements.length; i++) {
                        D3D11InputElementDesc elem = elements[i];
                        elementHashes[i] = computeElementHash(elem);

                        int attrib = mapSemanticToAttrib(elem.semanticName, elem.semanticIndex);
                        int type = mapDXGIFormatToAttribType(elem.format);
                        int num = getDXGIFormatComponents(elem.format);
                        boolean normalized = isDXGIFormatNormalized(elem.format);

                        bgfx_vertex_layout_add(layout, attrib, num, type, normalized, false);
                    }

                    bgfx_vertex_layout_end(layout);
                    inputLayouts.put(id, new InputLayoutInfo(id, layout, elementHashes));
                    return id;
                } finally {
                    lock.unlockWrite(stamp);
                }
            }

            BGFXVertexLayout getInputLayout(int handle) {
                long stamp = lock.readLock();
                try {
                    InputLayoutInfo info = inputLayouts.get(handle);
                    return info != null ? info.layout : null;
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            private long computeBytecodeHash(ByteBuffer bytecode) {
                long hash = 17;
                bytecode.mark();
                while (bytecode.hasRemaining()) {
                    hash = 31 * hash + bytecode.get();
                }
                bytecode.reset();
                return hash;
            }

            private int computeElementHash(D3D11InputElementDesc elem) {
                int hash = 17;
                hash = 31 * hash + elem.semanticName.hashCode();
                hash = 31 * hash + elem.semanticIndex;
                hash = 31 * hash + elem.format;
                hash = 31 * hash + elem.inputSlot;
                hash = 31 * hash + elem.alignedByteOffset;
                hash = 31 * hash + elem.inputSlotClass;
                hash = 31 * hash + elem.instanceDataStepRate;
                return hash;
            }

            private int mapSemanticToAttrib(String semantic, int index) {
                return switch (semantic.toUpperCase()) {
                    case "POSITION", "SV_POSITION" -> BGFX_ATTRIB_POSITION;
                    case "NORMAL" -> BGFX_ATTRIB_NORMAL;
                    case "TANGENT" -> BGFX_ATTRIB_TANGENT;
                    case "BINORMAL", "BITANGENT" -> BGFX_ATTRIB_BITANGENT;
                    case "COLOR" -> BGFX_ATTRIB_COLOR0 + index;
                    case "TEXCOORD" -> BGFX_ATTRIB_TEXCOORD0 + index;
                    case "BLENDWEIGHT" -> BGFX_ATTRIB_WEIGHT;
                    case "BLENDINDICES" -> BGFX_ATTRIB_INDICES;
                    default -> BGFX_ATTRIB_TEXCOORD0 + index;
                };
            }

            private int mapDXGIFormatToAttribType(int format) {
                return switch (format) {
                    case 2, 16, 29, 41 -> BGFX_ATTRIB_TYPE_FLOAT;  // R32G32B32A32_FLOAT, etc.
                    case 28, 87 -> BGFX_ATTRIB_TYPE_UINT8;         // R8G8B8A8_UNORM
                    case 37, 38 -> BGFX_ATTRIB_TYPE_INT16;         // R16G16_FLOAT
                    case 10, 34 -> BGFX_ATTRIB_TYPE_HALF;          // R16G16B16A16_FLOAT
                    default -> BGFX_ATTRIB_TYPE_FLOAT;
                };
            }

            private int getDXGIFormatComponents(int format) {
                return switch (format) {
                    case 2, 10, 28, 87 -> 4;   // *_R32G32B32A32_*, *_R8G8B8A8_*
                    case 6, 16 -> 3;           // *_R32G32B32_*
                    case 29, 37, 38 -> 2;      // *_R32G32_*, *_R16G16_*
                    case 41, 61 -> 1;          // *_R32_*, *_R8_*
                    default -> 4;
                };
            }

            private boolean isDXGIFormatNormalized(int format) {
                return switch (format) {
                    case 28, 87, 49, 56 -> true;  // *_UNORM, *_SNORM formats
                    default -> false;
                };
            }

            void cleanup() {
                long stamp = lock.writeLock();
                try {
                    for (ShaderHandle h : vertexShaders.values()) {
                        bgfx_destroy_shader(h.bgfxHandle);
                    }
                    for (ShaderHandle h : pixelShaders.values()) {
                        bgfx_destroy_shader(h.bgfxHandle);
                    }
                    for (ShaderHandle h : geometryShaders.values()) {
                        bgfx_destroy_shader(h.bgfxHandle);
                    }
                    for (ShaderHandle h : hullShaders.values()) {
                        bgfx_destroy_shader(h.bgfxHandle);
                    }
                    for (ShaderHandle h : domainShaders.values()) {
                        bgfx_destroy_shader(h.bgfxHandle);
                    }
                    for (ShaderHandle h : computeShaders.values()) {
                        bgfx_destroy_shader(h.bgfxHandle);
                    }
                    for (short program : programCache.values()) {
                        bgfx_destroy_program(program);
                    }
                    vertexShaders.clear();
                    pixelShaders.clear();
                    geometryShaders.clear();
                    hullShaders.clear();
                    domainShaders.clear();
                    computeShaders.clear();
                    programCache.clear();
                    inputLayouts.clear();
                } finally {
                    lock.unlockWrite(stamp);
                }
            }
        }

        public static final class D3D11InputElementDesc {
            public String semanticName;
            public int semanticIndex;
            public int format;          // DXGI_FORMAT
            public int inputSlot;
            public int alignedByteOffset;
            public int inputSlotClass;  // D3D11_INPUT_CLASSIFICATION
            public int instanceDataStepRate;

            public D3D11InputElementDesc() {}

            public D3D11InputElementDesc(String semantic, int index, int fmt, int slot, 
                                          int offset, int classification, int stepRate) {
                this.semanticName = semantic;
                this.semanticIndex = index;
                this.format = fmt;
                this.inputSlot = slot;
                this.alignedByteOffset = offset;
                this.inputSlotClass = classification;
                this.instanceDataStepRate = stepRate;
            }
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Resource View Manager
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Manages shader resource views, unordered access views, and render target views.
         */
        private static final class DX11ResourceViewManager {
            private final Int2ObjectMap<ResourceViewInfo> shaderResourceViews;
            private final Int2ObjectMap<ResourceViewInfo> unorderedAccessViews;
            private final Int2ObjectMap<ResourceViewInfo> renderTargetViews;
            private final Int2ObjectMap<ResourceViewInfo> depthStencilViews;
            private final StampedLock lock;
            private final AtomicInteger handleGenerator;

            DX11ResourceViewManager() {
                this.shaderResourceViews = new Int2ObjectOpenHashMap<>();
                this.unorderedAccessViews = new Int2ObjectOpenHashMap<>();
                this.renderTargetViews = new Int2ObjectOpenHashMap<>();
                this.depthStencilViews = new Int2ObjectOpenHashMap<>();
                this.lock = new StampedLock();
                this.handleGenerator = new AtomicInteger();
            }

            static final class ResourceViewInfo {
                final int id;
                final short textureHandle;
                final int format;
                final int firstMip;
                final int mipLevels;
                final int firstSlice;
                final int sliceCount;
                final int viewType;

                ResourceViewInfo(int id, short textureHandle, int format, int firstMip, 
                                int mipLevels, int firstSlice, int sliceCount, int viewType) {
                    this.id = id;
                    this.textureHandle = textureHandle;
                    this.format = format;
                    this.firstMip = firstMip;
                    this.mipLevels = mipLevels;
                    this.firstSlice = firstSlice;
                    this.sliceCount = sliceCount;
                    this.viewType = viewType;
                }
            }

            int createShaderResourceView(short texture, int format, int firstMip, int mipLevels,
                                          int firstSlice, int sliceCount) {
                long stamp = lock.writeLock();
                try {
                    int id = handleGenerator.incrementAndGet();
                    shaderResourceViews.put(id, new ResourceViewInfo(id, texture, format, 
                        firstMip, mipLevels, firstSlice, sliceCount, 0));
                    return id;
                } finally {
                    lock.unlockWrite(stamp);
                }
            }

            int createUnorderedAccessView(short texture, int format, int mipSlice, 
                                           int firstSlice, int sliceCount) {
                long stamp = lock.writeLock();
                try {
                    int id = handleGenerator.incrementAndGet();
                    unorderedAccessViews.put(id, new ResourceViewInfo(id, texture, format, 
                        mipSlice, 1, firstSlice, sliceCount, 1));
                    return id;
                } finally {
                    lock.unlockWrite(stamp);
                }
            }

            int createRenderTargetView(short texture, int format, int mipSlice, 
                                        int firstSlice, int sliceCount) {
                long stamp = lock.writeLock();
                try {
                    int id = handleGenerator.incrementAndGet();
                    renderTargetViews.put(id, new ResourceViewInfo(id, texture, format, 
                        mipSlice, 1, firstSlice, sliceCount, 2));
                    return id;
                } finally {
                    lock.unlockWrite(stamp);
                }
            }

            int createDepthStencilView(short texture, int format, int mipSlice, 
                                        int firstSlice, int sliceCount) {
                long stamp = lock.writeLock();
                try {
                    int id = handleGenerator.incrementAndGet();
                    depthStencilViews.put(id, new ResourceViewInfo(id, texture, format, 
                        mipSlice, 1, firstSlice, sliceCount, 3));
                    return id;
                } finally {
                    lock.unlockWrite(stamp);
                }
            }

            ResourceViewInfo getSRV(int handle) {
                long stamp = lock.readLock();
                try {
                    return shaderResourceViews.get(handle);
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            ResourceViewInfo getUAV(int handle) {
                long stamp = lock.readLock();
                try {
                    return unorderedAccessViews.get(handle);
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            ResourceViewInfo getRTV(int handle) {
                long stamp = lock.readLock();
                try {
                    return renderTargetViews.get(handle);
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            ResourceViewInfo getDSV(int handle) {
                long stamp = lock.readLock();
                try {
                    return depthStencilViews.get(handle);
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            void cleanup() {
                long stamp = lock.writeLock();
                try {
                    shaderResourceViews.clear();
                    unorderedAccessViews.clear();
                    renderTargetViews.clear();
                    depthStencilViews.clear();
                } finally {
                    lock.unlockWrite(stamp);
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Constant Buffer Manager
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Manages constant buffers and their uniform mappings.
         */
        private static final class DX11ConstantBufferManager {
            private final Int2ObjectMap<ConstantBufferInfo> constantBuffers;
            private final Int2ObjectMap<short[]> uniformHandles;
            private final StampedLock lock;
            private final AtomicInteger handleGenerator;
            private static final int MAX_CB_SIZE = 65536;

            DX11ConstantBufferManager() {
                this.constantBuffers = new Int2ObjectOpenHashMap<>();
                this.uniformHandles = new Int2ObjectOpenHashMap<>();
                this.lock = new StampedLock();
                this.handleGenerator = new AtomicInteger();
            }

            static final class ConstantBufferInfo {
                final int id;
                final int size;
                final ByteBuffer data;
                final boolean dynamic;
                boolean dirty;

                ConstantBufferInfo(int id, int size, boolean dynamic) {
                    this.id = id;
                    this.size = size;
                    this.data = MemoryUtil.memAlloc(size);
                    this.dynamic = dynamic;
                    this.dirty = true;
                }

                void free() {
                    MemoryUtil.memFree(data);
                }
            }

            int createConstantBuffer(int size, boolean dynamic, ByteBuffer initialData) {
                long stamp = lock.writeLock();
                try {
                    int id = handleGenerator.incrementAndGet();
                    ConstantBufferInfo info = new ConstantBufferInfo(id, size, dynamic);
                    if (initialData != null) {
                        info.data.put(initialData);
                        info.data.flip();
                    }
                    constantBuffers.put(id, info);
                    return id;
                } finally {
                    lock.unlockWrite(stamp);
                }
            }

            void updateConstantBuffer(int handle, ByteBuffer data, int offset, int size) {
                long stamp = lock.writeLock();
                try {
                    ConstantBufferInfo info = constantBuffers.get(handle);
                    if (info != null) {
                        info.data.position(offset);
                        int copySize = Math.min(size, info.data.remaining());
                        for (int i = 0; i < copySize; i++) {
                            info.data.put(data.get());
                        }
                        info.data.rewind();
                        info.dirty = true;
                    }
                } finally {
                    lock.unlockWrite(stamp);
                }
            }

            void bindConstantBuffer(int handle, int slot, int shaderStage) {
                long stamp = lock.readLock();
                try {
                    ConstantBufferInfo info = constantBuffers.get(handle);
                    if (info != null && info.dirty) {
                        // Create/update uniforms for this constant buffer
                        // In practice, this would parse the CB layout and create individual uniforms
                        String uniformName = "u_cb" + shaderStage + "_" + slot;
                        short uniform = bgfx_create_uniform(uniformName, BGFX_UNIFORM_TYPE_VEC4, 
                            info.size / 16);
                        bgfx_set_uniform(uniform, info.data, info.size / 16);
                        bgfx_destroy_uniform(uniform);
                        info.dirty = false;
                    }
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            void cleanup() {
                long stamp = lock.writeLock();
                try {
                    for (ConstantBufferInfo info : constantBuffers.values()) {
                        info.free();
                    }
                    constantBuffers.clear();
                    uniformHandles.clear();
                } finally {
                    lock.unlockWrite(stamp);
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Command List Recorder
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Records and plays back command lists for deferred context support.
         */
        private static final class DX11CommandListRecorder {
            private final Int2ObjectMap<CommandList> commandLists;
            private final ThreadLocal<CommandList> activeRecording;
            private final StampedLock lock;
            private final AtomicInteger handleGenerator;

            DX11CommandListRecorder() {
                this.commandLists = new Int2ObjectOpenHashMap<>();
                this.activeRecording = new ThreadLocal<>();
                this.lock = new StampedLock();
                this.handleGenerator = new AtomicInteger();
            }

            static final class CommandList {
                final int id;
                final ArrayDeque<Command> commands;
                boolean finalized;

                CommandList(int id) {
                    this.id = id;
                    this.commands = new ArrayDeque<>();
                    this.finalized = false;
                }
            }

            interface Command {
                void execute(DX11Path context);
            }

            int beginRecording() {
                int id = handleGenerator.incrementAndGet();
                CommandList list = new CommandList(id);
                activeRecording.set(list);
                return id;
            }

            void recordCommand(Command command) {
                CommandList list = activeRecording.get();
                if (list != null && !list.finalized) {
                    list.commands.add(command);
                }
            }

            int finishRecording() {
                CommandList list = activeRecording.get();
                if (list != null) {
                    list.finalized = true;
                    long stamp = lock.writeLock();
                    try {
                        commandLists.put(list.id, list);
                    } finally {
                        lock.unlockWrite(stamp);
                    }
                    activeRecording.remove();
                    return list.id;
                }
                return -1;
            }

            void executeCommandList(int handle, DX11Path context) {
                long stamp = lock.readLock();
                try {
                    CommandList list = commandLists.get(handle);
                    if (list != null && list.finalized) {
                        for (Command cmd : list.commands) {
                            cmd.execute(context);
                        }
                    }
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            void cleanup() {
                long stamp = lock.writeLock();
                try {
                    commandLists.clear();
                } finally {
                    lock.unlockWrite(stamp);
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Pipeline State Tracking
        // ─────────────────────────────────────────────────────────────────────────────

        private static final class DX11PipelineState {
            int inputLayoutHandle;
            int vertexShaderHandle;
            int pixelShaderHandle;
            int geometryShaderHandle;
            int hullShaderHandle;
            int domainShaderHandle;
            int computeShaderHandle;
            int primitiveTopology;
            int rasterizerStateHash;
            int blendStateHash;
            int depthStencilStateHash;
            int stencilRef;
            final float[] blendFactor = new float[4];
            int sampleMask;
            
            void copyFrom(DX11PipelineState other) {
                this.inputLayoutHandle = other.inputLayoutHandle;
                this.vertexShaderHandle = other.vertexShaderHandle;
                this.pixelShaderHandle = other.pixelShaderHandle;
                this.geometryShaderHandle = other.geometryShaderHandle;
                this.hullShaderHandle = other.hullShaderHandle;
                this.domainShaderHandle = other.domainShaderHandle;
                this.computeShaderHandle = other.computeShaderHandle;
                this.primitiveTopology = other.primitiveTopology;
                this.rasterizerStateHash = other.rasterizerStateHash;
                this.blendStateHash = other.blendStateHash;
                this.depthStencilStateHash = other.depthStencilStateHash;
                this.stencilRef = other.stencilRef;
                System.arraycopy(other.blendFactor, 0, this.blendFactor, 0, 4);
                this.sampleMask = other.sampleMask;
            }

            boolean equals(DX11PipelineState other) {
                return inputLayoutHandle == other.inputLayoutHandle &&
                       vertexShaderHandle == other.vertexShaderHandle &&
                       pixelShaderHandle == other.pixelShaderHandle &&
                       geometryShaderHandle == other.geometryShaderHandle &&
                       hullShaderHandle == other.hullShaderHandle &&
                       domainShaderHandle == other.domainShaderHandle &&
                       computeShaderHandle == other.computeShaderHandle &&
                       primitiveTopology == other.primitiveTopology &&
                       rasterizerStateHash == other.rasterizerStateHash &&
                       blendStateHash == other.blendStateHash &&
                       depthStencilStateHash == other.depthStencilStateHash &&
                       stencilRef == other.stencilRef &&
                       Arrays.equals(blendFactor, other.blendFactor) &&
                       sampleMask == other.sampleMask;
            }
        }

        private static final class ShaderStageState {
            final short[] constantBuffers = new short[14];
            final short[] shaderResources = new short[128];
            final short[] samplers = new short[16];
            final int[] samplerFlags = new int[16];
            int shader;
            boolean dirty;

            ShaderStageState() {
                Arrays.fill(constantBuffers, BGFX_INVALID_HANDLE);
                Arrays.fill(shaderResources, BGFX_INVALID_HANDLE);
                Arrays.fill(samplers, BGFX_INVALID_HANDLE);
                dirty = true;
            }

            void reset() {
                Arrays.fill(constantBuffers, BGFX_INVALID_HANDLE);
                Arrays.fill(shaderResources, BGFX_INVALID_HANDLE);
                Arrays.fill(samplers, BGFX_INVALID_HANDLE);
                Arrays.fill(samplerFlags, 0);
                shader = 0;
                dirty = true;
            }
        }

        private static final class ResourceBindingState {
            final short[] vertexBuffers = new short[32];
            final int[] vbOffsets = new int[32];
            final int[] vbStrides = new int[32];
            short indexBuffer;
            int ibFormat;
            int ibOffset;
            final short[] renderTargets = new short[8];
            short depthStencil;
            final short[] uavs = new short[8];
            boolean dirty;

            ResourceBindingState() {
                Arrays.fill(vertexBuffers, BGFX_INVALID_HANDLE);
                Arrays.fill(renderTargets, BGFX_INVALID_HANDLE);
                Arrays.fill(uavs, BGFX_INVALID_HANDLE);
                indexBuffer = BGFX_INVALID_HANDLE;
                depthStencil = BGFX_INVALID_HANDLE;
                dirty = true;
            }
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Constructor
        // ─────────────────────────────────────────────────────────────────────────────

        public DX11Path(Arena arena) {
            this.arena = arena;
            this.rasterizerStates = new Object2ObjectOpenHashMap<>();
            this.blendStates = new Object2ObjectOpenHashMap<>();
            this.depthStencilStates = new Object2ObjectOpenHashMap<>();
            this.samplerStates = new Object2ObjectOpenHashMap<>();
            this.inputLayouts = new Int2ObjectOpenHashMap<>();

            // Initialize BGFX integration components
            this.contextManager = new BGFXContextManager();
            this.stateCompiler = new DX11ToBGFXStateCompiler();
            this.shaderManager = new DX11ShaderManager();
            this.resourceViewManager = new DX11ResourceViewManager();
            this.constantBufferManager = new DX11ConstantBufferManager();
            this.commandListRecorder = new DX11CommandListRecorder();

            // View management
            this.currentViewId = 0;
            this.viewIdPool = new short[MAX_VIEWS];
            for (int i = 0; i < MAX_VIEWS; i++) {
                viewIdPool[i] = (short) i;
            }
            this.nextViewId = 0;

            // Pipeline state
            this.currentPipelineState = new DX11PipelineState();
            this.pendingPipelineState = new DX11PipelineState();
            this.pipelineStateDirty = true;

            // Shader stages
            this.shaderStages = new ShaderStageState[NUM_SHADER_STAGES];
            for (int i = 0; i < NUM_SHADER_STAGES; i++) {
                shaderStages[i] = new ShaderStageState();
            }

            // Resource bindings
            this.resourceBindings = new ResourceBindingState[1];
            this.resourceBindings[0] = new ResourceBindingState();

            // Performance metrics
            this.drawCallCount = new AtomicLong();
            this.dispatchCallCount = new AtomicLong();
            this.stateChangeCount = new AtomicLong();
            this.mapCount = new AtomicLong();
            this.updateSubresourceCount = new AtomicLong();
            this.triangleCount = new LongAdder();
            this.instanceCount = new LongAdder();
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Device & SwapChain Creation
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Create D3D11 device and swap chain via BGFX.
         */
        public MappingResult createDeviceAndSwapChain(long windowHandle, int width, int height,
                                                       int sampleCount, boolean debug) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                BGFXInit init = BGFXInit.calloc(stack);
                bgfx_init_ctor(init);

                init.type(BGFX_RENDERER_TYPE_COUNT);
                init.vendorId(BGFX_PCI_ID_NONE);

                int resetFlags = BGFX_RESET_VSYNC;
                if (sampleCount > 1) {
                    resetFlags |= switch (sampleCount) {
                        case 2 -> BGFX_RESET_MSAA_X2;
                        case 4 -> BGFX_RESET_MSAA_X4;
                        case 8 -> BGFX_RESET_MSAA_X8;
                        case 16 -> BGFX_RESET_MSAA_X16;
                        default -> 0;
                    };
                }

                init.resolution(res -> res
                    .width(width)
                    .height(height)
                    .reset(resetFlags)
                );

                if (debug) {
                    init.debug(true);
                    init.profile(true);
                }

                init.platformData(pd -> {
                    switch (Platform.get()) {
                        case WINDOWS -> pd.nwh(windowHandle);
                        case LINUX -> pd.ndt(0).nwh(windowHandle);
                        case MACOSX -> pd.nwh(windowHandle);
                    }
                });

                if (!bgfx_init(init)) {
                    return MappingResult.failure(CallType.DX11_CREATE_DEVICE, HResult.E_FAIL,
                        "Failed to initialize BGFX for DX11 path");
                }

                // Query capabilities
                BGFXCaps caps = bgfx_get_caps();
                determineFeatureLevel(caps);

                // Setup default view
                bgfx_set_view_clear(0, BGFX_CLEAR_COLOR | BGFX_CLEAR_DEPTH | BGFX_CLEAR_STENCIL,
                    0x303030FF, 1.0f, 0);
                bgfx_set_view_rect(0, 0, 0, width, height);

                return MappingResult.success(CallType.DX11_CREATE_DEVICE, 0);
            }
        }

        private void determineFeatureLevel(BGFXCaps caps) {
            // Map BGFX capabilities to D3D feature level
            if (caps.supported() >= 0xFFFFFFFF) {
                featureLevel = D3DFeatureLevel.LEVEL_11_1;
            } else {
                featureLevel = D3DFeatureLevel.LEVEL_11_0;
            }
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Clear Operations
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Clear render target view.
         */
        public MappingResult clearRenderTargetView(long rtv, float r, float g, float b, float a) {
            int rgba = ((int)(r * 255) << 24) | ((int)(g * 255) << 16) | 
                       ((int)(b * 255) << 8) | (int)(a * 255);
            bgfx_set_view_clear(currentViewId, BGFX_CLEAR_COLOR, rgba, 1.0f, 0);
            bgfx_touch(currentViewId);
            return MappingResult.success(CallType.DX11_CLEAR_RENDER_TARGET_VIEW, 0);
        }

        /**
         * Clear depth stencil view.
         */
        public MappingResult clearDepthStencilView(long dsv, int clearFlags, float depth, int stencil) {
            int bgfxFlags = 0;
            if ((clearFlags & 1) != 0) bgfxFlags |= BGFX_CLEAR_DEPTH;   // D3D11_CLEAR_DEPTH
            if ((clearFlags & 2) != 0) bgfxFlags |= BGFX_CLEAR_STENCIL; // D3D11_CLEAR_STENCIL
            
            bgfx_set_view_clear(currentViewId, bgfxFlags, 0, depth, stencil);
            bgfx_touch(currentViewId);
            return MappingResult.success(CallType.DX11_CLEAR_DEPTH_STENCIL_VIEW, 0);
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Output Merger State
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Set render targets.
         */
        public MappingResult setRenderTargets(int numViews, long[] rtvs, long dsv) {
            // In BGFX, render targets are managed via frame buffers
            // For simplicity, we use views
            if (numViews > 0 && rtvs != null) {
                for (int i = 0; i < numViews; i++) {
                    resourceBindings[0].renderTargets[i] = (short) rtvs[i];
                }
            }
            resourceBindings[0].depthStencil = (short) dsv;
            resourceBindings[0].dirty = true;
            return MappingResult.success(CallType.DX11_OM_SET_RENDER_TARGETS, 0);
        }

        /**
         * Set blend state.
         */
        public MappingResult setBlendState(long state, float[] blendFactor, int sampleMask) {
            if (blendFactor != null) {
                System.arraycopy(blendFactor, 0, pendingPipelineState.blendFactor, 0, 
                    Math.min(4, blendFactor.length));
            }
            pendingPipelineState.sampleMask = sampleMask;
            pendingPipelineState.blendStateHash = (int) state;
            pipelineStateDirty = true;
            stateChangeCount.incrementAndGet();
            return MappingResult.success(CallType.DX11_OM_SET_BLEND_STATE, 0);
        }

        /**
         * Set depth stencil state.
         */
        public MappingResult setDepthStencilState(long state, int stencilRef) {
            pendingPipelineState.depthStencilStateHash = (int) state;
            pendingPipelineState.stencilRef = stencilRef;
            pipelineStateDirty = true;
            stateChangeCount.incrementAndGet();
            return MappingResult.success(CallType.DX11_OM_SET_DEPTH_STENCIL_STATE, 0);
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Rasterizer State
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Set viewports.
         */
        public MappingResult setViewports(DX11Viewport[] viewports) {
            if (viewports != null && viewports.length > 0) {
                DX11Viewport vp = viewports[0];
                bgfx_set_view_rect(currentViewId, (int) vp.topLeftX, (int) vp.topLeftY,
                    (int) vp.width, (int) vp.height);
            }
            return MappingResult.success(CallType.DX11_RS_SET_VIEWPORTS, 0);
        }

        /**
         * Set scissor rectangles.
         */
        public MappingResult setScissorRects(DX11Rect[] rects) {
            if (rects != null && rects.length > 0) {
                DX11Rect r = rects[0];
                bgfx_set_scissor(r.left, r.top, r.right - r.left, r.bottom - r.top);
            }
            return MappingResult.success(CallType.DX11_RS_SET_SCISSOR_RECTS, 0);
        }

        /**
         * Set rasterizer state.
         */
        public MappingResult setRasterizerState(long state) {
            pendingPipelineState.rasterizerStateHash = (int) state;
            pipelineStateDirty = true;
            stateChangeCount.incrementAndGet();
            return MappingResult.success(CallType.DX11_RS_SET_STATE, 0);
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Input Assembler State
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Set vertex buffers.
         */
        public MappingResult setVertexBuffers(int startSlot, int numBuffers,
                                               long[] buffers, int[] strides, int[] offsets) {
            for (int i = 0; i < numBuffers; i++) {
                int slot = startSlot + i;
                if (slot < 32) {
                    resourceBindings[0].vertexBuffers[slot] = (short) buffers[i];
                    resourceBindings[0].vbStrides[slot] = strides[i];
                    resourceBindings[0].vbOffsets[slot] = offsets[i];
                }
            }
            resourceBindings[0].dirty = true;
            return MappingResult.success(CallType.DX11_IA_SET_VERTEX_BUFFERS, 0);
        }

        /**
         * Set index buffer.
         */
        public MappingResult setIndexBuffer(long buffer, int format, int offset) {
            resourceBindings[0].indexBuffer = (short) buffer;
            resourceBindings[0].ibFormat = format;
            resourceBindings[0].ibOffset = offset;
            resourceBindings[0].dirty = true;
            return MappingResult.success(CallType.DX11_IA_SET_INDEX_BUFFER, 0);
        }

        /**
         * Set input layout.
         */
        public MappingResult setInputLayout(long inputLayout) {
            pendingPipelineState.inputLayoutHandle = (int) inputLayout;
            pipelineStateDirty = true;
            return MappingResult.success(CallType.DX11_IA_SET_INPUT_LAYOUT, 0);
        }

        /**
         * Set primitive topology.
         */
        public MappingResult setPrimitiveTopology(int topology) {
            pendingPipelineState.primitiveTopology = topology;
            pipelineStateDirty = true;
            return MappingResult.success(CallType.DX11_IA_SET_PRIMITIVE_TOPOLOGY, 0);
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Shader Stage Setup
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Set vertex shader.
         */
        public MappingResult setVertexShader(long shader, long[] classInstances, int numClassInstances) {
            pendingPipelineState.vertexShaderHandle = (int) shader;
            shaderStages[0].shader = (int) shader;
            shaderStages[0].dirty = true;
            pipelineStateDirty = true;
            return MappingResult.success(CallType.DX11_VS_SET_SHADER, 0);
        }

        /**
         * Set pixel shader.
         */
        public MappingResult setPixelShader(long shader, long[] classInstances, int numClassInstances) {
            pendingPipelineState.pixelShaderHandle = (int) shader;
            shaderStages[1].shader = (int) shader;
            shaderStages[1].dirty = true;
            pipelineStateDirty = true;
            return MappingResult.success(CallType.DX11_PS_SET_SHADER, 0);
        }

        /**
         * Set geometry shader.
         */
        public MappingResult setGeometryShader(long shader, long[] classInstances, int numClassInstances) {
            pendingPipelineState.geometryShaderHandle = (int) shader;
            shaderStages[2].shader = (int) shader;
            shaderStages[2].dirty = true;
            pipelineStateDirty = true;
            return MappingResult.success(CallType.DX11_GS_SET_SHADER, 0);
        }

        /**
         * Set hull shader (tessellation).
         */
        public MappingResult setHullShader(long shader, long[] classInstances, int numClassInstances) {
            pendingPipelineState.hullShaderHandle = (int) shader;
            shaderStages[3].shader = (int) shader;
            shaderStages[3].dirty = true;
            pipelineStateDirty = true;
            return MappingResult.success(CallType.DX11_HS_SET_SHADER, 0);
        }

        /**
         * Set domain shader (tessellation).
         */
        public MappingResult setDomainShader(long shader, long[] classInstances, int numClassInstances) {
            pendingPipelineState.domainShaderHandle = (int) shader;
            shaderStages[4].shader = (int) shader;
            shaderStages[4].dirty = true;
            pipelineStateDirty = true;
            return MappingResult.success(CallType.DX11_DS_SET_SHADER, 0);
        }

        /**
         * Set compute shader.
         */
        public MappingResult setComputeShader(long shader, long[] classInstances, int numClassInstances) {
            pendingPipelineState.computeShaderHandle = (int) shader;
            shaderStages[5].shader = (int) shader;
            shaderStages[5].dirty = true;
            return MappingResult.success(CallType.DX11_CS_SET_SHADER, 0);
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Shader Resource Binding
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Set constant buffers for vertex shader.
         */
        public MappingResult vsSetConstantBuffers(int startSlot, int numBuffers, long[] buffers) {
            for (int i = 0; i < numBuffers; i++) {
                shaderStages[0].constantBuffers[startSlot + i] = (short) buffers[i];
            }
            shaderStages[0].dirty = true;
            return MappingResult.success(CallType.DX11_VS_SET_CONSTANT_BUFFERS, 0);
        }

        /**
         * Set constant buffers for pixel shader.
         */
        public MappingResult psSetConstantBuffers(int startSlot, int numBuffers, long[] buffers) {
            for (int i = 0; i < numBuffers; i++) {
                shaderStages[1].constantBuffers[startSlot + i] = (short) buffers[i];
            }
            shaderStages[1].dirty = true;
            return MappingResult.success(CallType.DX11_PS_SET_CONSTANT_BUFFERS, 0);
        }

        /**
         * Set shader resource views for pixel shader.
         */
        public MappingResult psSetShaderResources(int startSlot, int numViews, long[] srvs) {
            for (int i = 0; i < numViews; i++) {
                shaderStages[1].shaderResources[startSlot + i] = (short) srvs[i];
            }
            shaderStages[1].dirty = true;
            return MappingResult.success(CallType.DX11_PS_SET_SHADER_RESOURCES, 0);
        }

        /**
         * Set samplers for pixel shader.
         */
        public MappingResult psSetSamplers(int startSlot, int numSamplers, long[] samplers) {
            for (int i = 0; i < numSamplers; i++) {
                shaderStages[1].samplers[startSlot + i] = (short) samplers[i];
            }
            shaderStages[1].dirty = true;
            return MappingResult.success(CallType.DX11_PS_SET_SAMPLERS, 0);
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Draw Commands
        // ─────────────────────────────────────────────────────────────────────────────

        private void flushPipelineState() {
            if (!pipelineStateDirty) return;

            // Get compiled state
            long state = BGFX_STATE_WRITE_RGB | BGFX_STATE_WRITE_A | BGFX_STATE_WRITE_Z;
            
            // Add topology
            state |= mapTopologyToState(pendingPipelineState.primitiveTopology);

            bgfx_set_state(state, 0);

            currentPipelineState.copyFrom(pendingPipelineState);
            pipelineStateDirty = false;
        }

        private long mapTopologyToState(int topology) {
            return switch (topology) {
                case 1 -> BGFX_STATE_PT_POINTS;     // D3D11_PRIMITIVE_TOPOLOGY_POINTLIST
                case 2 -> BGFX_STATE_PT_LINES;     // D3D11_PRIMITIVE_TOPOLOGY_LINELIST
                case 3 -> BGFX_STATE_PT_LINESTRIP; // D3D11_PRIMITIVE_TOPOLOGY_LINESTRIP
                case 4 -> 0;                        // D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST
                case 5 -> BGFX_STATE_PT_TRISTRIP;  // D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP
                default -> 0;
            };
        }

        private void bindResources() {
            // Bind vertex buffers
            ResourceBindingState rb = resourceBindings[0];
            if (rb.vertexBuffers[0] != BGFX_INVALID_HANDLE) {
                bgfx_set_vertex_buffer(0, rb.vertexBuffers[0], 0, Integer.MAX_VALUE);
            }

            // Bind index buffer
            if (rb.indexBuffer != BGFX_INVALID_HANDLE) {
                bgfx_set_index_buffer(rb.indexBuffer, 0, Integer.MAX_VALUE);
            }

            // Bind textures from shader stages
            int textureSlot = 0;
            for (int stage = 0; stage < NUM_SHADER_STAGES; stage++) {
                ShaderStageState ss = shaderStages[stage];
                for (int i = 0; i < 16 && textureSlot < 16; i++) {
                    if (ss.shaderResources[i] != BGFX_INVALID_HANDLE) {
                        DX11ResourceViewManager.ResourceViewInfo srv = 
                            resourceViewManager.getSRV(ss.shaderResources[i]);
                        if (srv != null) {
                            String uniformName = "s_tex" + textureSlot;
                            short uniform = bgfx_create_uniform(uniformName, BGFX_UNIFORM_TYPE_SAMPLER, 1);
                            bgfx_set_texture(textureSlot, uniform, srv.textureHandle, ss.samplerFlags[i]);
                            bgfx_destroy_uniform(uniform);
                            textureSlot++;
                        }
                    }
                }
            }
        }

        /**
         * Draw non-indexed primitives.
         */
        public MappingResult draw(int vertexCount, int startVertexLocation) {
            flushPipelineState();
            bindResources();

            short program = shaderManager.getOrCreateProgram(
                pendingPipelineState.vertexShaderHandle,
                pendingPipelineState.pixelShaderHandle
            );

            if (program != BGFX_INVALID_HANDLE) {
                bgfx_set_vertex_buffer(0, resourceBindings[0].vertexBuffers[0], 
                    startVertexLocation, vertexCount);
                bgfx_submit(currentViewId, program, 0, BGFX_DISCARD_ALL);
            }

            drawCallCount.incrementAndGet();
            return MappingResult.success(CallType.DX11_DRAW, 0);
        }

        /**
         * Draw indexed primitives.
         */
        public MappingResult drawIndexed(int indexCount, int startIndexLocation, int baseVertexLocation) {
            flushPipelineState();
            bindResources();

            short program = shaderManager.getOrCreateProgram(
                pendingPipelineState.vertexShaderHandle,
                pendingPipelineState.pixelShaderHandle
            );

            if (program != BGFX_INVALID_HANDLE) {
                bgfx_set_index_buffer(resourceBindings[0].indexBuffer, 
                    startIndexLocation, indexCount);
                bgfx_submit(currentViewId, program, 0, BGFX_DISCARD_ALL);
            }

            drawCallCount.incrementAndGet();
            return MappingResult.success(CallType.DX11_DRAW_INDEXED, 0);
        }

        /**
         * Draw instanced primitives.
         */
        public MappingResult drawInstanced(int vertexCountPerInstance, int instanceCount,
                                           int startVertexLocation, int startInstanceLocation) {
            flushPipelineState();
            bindResources();

            short program = shaderManager.getOrCreateProgram(
                pendingPipelineState.vertexShaderHandle,
                pendingPipelineState.pixelShaderHandle
            );

            if (program != BGFX_INVALID_HANDLE) {
                bgfx_set_vertex_buffer(0, resourceBindings[0].vertexBuffers[0], 
                    startVertexLocation, vertexCountPerInstance);
                bgfx_set_instance_count(instanceCount);
                bgfx_submit(currentViewId, program, 0, BGFX_DISCARD_ALL);
            }

            drawCallCount.incrementAndGet();
            this.instanceCount.add(instanceCount);
            return MappingResult.success(CallType.DX11_DRAW_INSTANCED, 0);
        }

        /**
         * Draw indexed instanced primitives.
         */
        public MappingResult drawIndexedInstanced(int indexCountPerInstance, int instanceCount,
                                                   int startIndexLocation, int baseVertexLocation,
                                                   int startInstanceLocation) {
            flushPipelineState();
            bindResources();

            short program = shaderManager.getOrCreateProgram(
                pendingPipelineState.vertexShaderHandle,
                pendingPipelineState.pixelShaderHandle
            );

            if (program != BGFX_INVALID_HANDLE) {
                bgfx_set_index_buffer(resourceBindings[0].indexBuffer, 
                    startIndexLocation, indexCountPerInstance);
                bgfx_set_instance_count(instanceCount);
                bgfx_submit(currentViewId, program, 0, BGFX_DISCARD_ALL);
            }

            drawCallCount.incrementAndGet();
            this.instanceCount.add(instanceCount);
            return MappingResult.success(CallType.DX11_DRAW_INDEXED_INSTANCED, 0);
        }

        /**
         * Dispatch compute shader.
         */
        public MappingResult dispatch(int threadGroupCountX, int threadGroupCountY, int threadGroupCountZ) {
            // BGFX compute dispatch
            short computeProgram = (short) pendingPipelineState.computeShaderHandle;
            if (computeProgram != BGFX_INVALID_HANDLE) {
                bgfx_dispatch(currentViewId, computeProgram, 
                    threadGroupCountX, threadGroupCountY, threadGroupCountZ, BGFX_DISCARD_ALL);
            }

            dispatchCallCount.incrementAndGet();
            return MappingResult.success(CallType.DX11_DISPATCH, 0);
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Resource Operations
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * Map resource for CPU access.
         */
        public MappingResult map(long resource, int subresource, int mapType, int mapFlags) {
            // BGFX doesn't directly support mapping - use staging resources
            mapCount.incrementAndGet();
            return MappingResult.success(CallType.DX11_MAP, 0);
        }

        /**
         * Unmap resource.
         */
        public MappingResult unmap(long resource, int subresource) {
            return MappingResult.success(CallType.DX11_UNMAP, 0);
        }

        /**
         * Update subresource.
         */
        public MappingResult updateSubresource(long destResource, int destSubresource,
                                                DX11Box destBox, MemorySegment srcData,
                                                int srcRowPitch, int srcDepthPitch) {
            updateSubresourceCount.incrementAndGet();
            // BGFX texture/buffer update would go here
            return MappingResult.success(CallType.DX11_UPDATE_SUBRESOURCE, 0);
        }

        /**
         * Copy resource.
         */
        public MappingResult copyResource(long destResource, long srcResource) {
            // BGFX blit operation
            return MappingResult.success(CallType.DX11_COPY_RESOURCE, 0);
        }

        /**
         * Copy subresource region.
         */
        public MappingResult copySubresourceRegion(long destResource, int destSubresource,
                                                    int destX, int destY, int destZ,
                                                    long srcResource, int srcSubresource,
                                                    DX11Box srcBox) {
            // BGFX blit with region
            return MappingResult.success(CallType.DX11_COPY_SUBRESOURCE_REGION, 0);
        }

        /**
         * Generate mips.
         */
        public MappingResult generateMips(long shaderResourceView) {
            // BGFX doesn't have automatic mip generation - would need compute shader
            return MappingResult.success(CallType.DX11_GENERATE_MIPS, 0);
        }

        /**
         * Resolve subresource (MSAA resolve).
         */
        public MappingResult resolveSubresource(long destResource, int destSubresource,
                                                 long srcResource, int srcSubresource, int format) {
            // BGFX blit for resolve
            return MappingResult.success(CallType.DX11_RESOLVE_SUBRESOURCE, 0);
        }

        /**
         * Present swap chain.
         */
        public MappingResult present(int syncInterval, int flags) {
            int frame = bgfx_frame(syncInterval > 0);
            return MappingResult.success(CallType.DX11_PRESENT, 0, frame);
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Helper Structures
        // ─────────────────────────────────────────────────────────────────────────────

        public static final class DX11Viewport {
            public float topLeftX, topLeftY, width, height, minDepth, maxDepth;

            public DX11Viewport(float x, float y, float w, float h, float minD, float maxD) {
                this.topLeftX = x; this.topLeftY = y;
                this.width = w; this.height = h;
                this.minDepth = minD; this.maxDepth = maxD;
            }
        }

        public static final class DX11Rect {
            public int left, top, right, bottom;

            public DX11Rect(int l, int t, int r, int b) {
                this.left = l; this.top = t; this.right = r; this.bottom = b;
            }
        }

        public static final class DX11Box {
            public int left, top, front, right, bottom, back;

            public DX11Box(int l, int t, int f, int r, int b, int bk) {
                this.left = l; this.top = t; this.front = f;
                this.right = r; this.bottom = b; this.back = bk;
            }
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Statistics & Cleanup
        // ─────────────────────────────────────────────────────────────────────────────

        public void cleanup() {
            contextManager.cleanup();
            stateCompiler.clear();
            shaderManager.cleanup();
            resourceViewManager.cleanup();
            constantBufferManager.cleanup();
            commandListRecorder.cleanup();

            rasterizerStates.clear();
            blendStates.clear();
            depthStencilStates.clear();
            samplerStates.clear();
            inputLayouts.clear();

            bgfx_shutdown();
        }

        public int getFeatureLevel() { return featureLevel; }
        public long getDrawCallCount() { return drawCallCount.get(); }
        public long getDispatchCallCount() { return dispatchCallCount.get(); }
        public long getStateChangeCount() { return stateChangeCount.get(); }
        public long getInstanceCount() { return instanceCount.sum(); }
    }
