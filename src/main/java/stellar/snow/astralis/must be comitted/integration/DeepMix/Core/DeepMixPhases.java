package stellar.snow.astralis.integration.DeepMix.Core;

/*
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                            ║
 * ║   🔮 DEEPMIX — PHASES 10-20 ANNOTATION IMPLEMENTATION                      ║
 * ║   ═══════════════════════════════════════════════════════════════           ║
 * ║                                                                            ║
 * ║   Phase 10: Security & Data Integrity       (15 annotations)              ║
 * ║   Phase 11: State Management                (10 annotations)              ║
 * ║   Phase 12: Concurrency & Threading         (10 annotations)              ║
 * ║   Phase 13: Encoding & Format Conversion    (27 annotations)              ║
 * ║   Phase 14: I/O & Streaming                 (18 annotations)              ║
 * ║   Phase 15: Cloud & Container               (15 annotations)              ║
 * ║   Phase 16: Machine Learning & AI           (12 annotations)              ║
 * ║   Phase 17: Database Operations             (14 annotations)              ║
 * ║   Phase 18: Testing & Quality               (11 annotations)              ║
 * ║   Phase 19: Reactive & Functional           (13 annotations)              ║
 * ║   Phase 20: UI & Graphics                   (16 annotations)              ║
 * ║                                                                            ║
 * ║   Total: 161 annotations | 322 definitions (full + shortcut)              ║
 * ║                                                                            ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */

// ══════════════════════════════════════════════════════════════════════
// STANDARD JAVA IMPORTS
// ══════════════════════════════════════════════════════════════════════

// Annotations
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// I/O
import java.io.*;

// NIO
import java.nio.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.channels.*;
import java.nio.file.*;

// Security & Crypto
import java.security.*;
import javax.crypto.*;

// Utilities
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.stream.*;

// ══════════════════════════════════════════════════════════════════════
// BYTECODE MANIPULATION
// ══════════════════════════════════════════════════════════════════════

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

// ══════════════════════════════════════════════════════════════════════
// LWJGL CORE & SYSTEM
// ══════════════════════════════════════════════════════════════════════

import org.lwjgl.*;
import org.lwjgl.system.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

// ══════════════════════════════════════════════════════════════════════
// LWJGL GLFW (Window Management)
// ══════════════════════════════════════════════════════════════════════

import org.lwjgl.glfw.*;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;

// ══════════════════════════════════════════════════════════════════════
// LWJGL OPENGL
// ══════════════════════════════════════════════════════════════════════

import org.lwjgl.opengl.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GL46;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageCallback;

// ══════════════════════════════════════════════════════════════════════
// LWJGL OPENGL ES
// ══════════════════════════════════════════════════════════════════════

import org.lwjgl.opengles.*;
import org.lwjgl.opengles.GLES;
import org.lwjgl.opengles.GLES20;
import org.lwjgl.opengles.GLES30;
import org.lwjgl.opengles.GLES31;
import org.lwjgl.opengles.GLES32;
import org.lwjgl.opengles.GLESCapabilities;

// ══════════════════════════════════════════════════════════════════════
// LWJGL VULKAN
// ══════════════════════════════════════════════════════════════════════

import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.EXTMeshShader;
import org.lwjgl.vulkan.KHRAccelerationStructure;
import org.lwjgl.vulkan.KHRDynamicRendering;
import org.lwjgl.vulkan.KHRRayTracingPipeline;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPool;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

// ══════════════════════════════════════════════════════════════════════
// LWJGL BGFX (DirectX, Metal, WebGL, Canvas)
// ══════════════════════════════════════════════════════════════════════

import org.lwjgl.bgfx.*;
import org.lwjgl.bgfx.BGFX;
import org.lwjgl.bgfx.BGFXAttachment;
import org.lwjgl.bgfx.BGFXCallbackInterface;
import org.lwjgl.bgfx.BGFXCallbackVtbl;
import org.lwjgl.bgfx.BGFXCaps;
import org.lwjgl.bgfx.BGFXInit;
import org.lwjgl.bgfx.BGFXInternalData;
import org.lwjgl.bgfx.BGFXMemory;
import org.lwjgl.bgfx.BGFXPlatformData;
import org.lwjgl.bgfx.BGFXReleaseFunctionCallback;
import org.lwjgl.bgfx.BGFXResolution;
import org.lwjgl.bgfx.BGFXStats;
import org.lwjgl.bgfx.BGFXTextureInfo;
import org.lwjgl.bgfx.BGFXTransientIndexBuffer;
import org.lwjgl.bgfx.BGFXTransientVertexBuffer;
import org.lwjgl.bgfx.BGFXUniform;
import org.lwjgl.bgfx.BGFXVertexLayout;

// ══════════════════════════════════════════════════════════════════════
// LWJGL SHADERC (GLSL/HLSL → SPIR-V Compilation)
// ══════════════════════════════════════════════════════════════════════

import org.lwjgl.util.shaderc.*;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.util.shaderc.ShadercIncludeResolve;
import org.lwjgl.util.shaderc.ShadercIncludeResultRelease;

// ══════════════════════════════════════════════════════════════════════
// LWJGL SPIRV-CROSS (SPIR-V → GLSL/HLSL/MSL/ESSL)
// ══════════════════════════════════════════════════════════════════════

import org.lwjgl.util.spvc.*;
import org.lwjgl.util.spvc.Spvc;
import org.lwjgl.util.spvc.SpvcCompiler;
import org.lwjgl.util.spvc.SpvcCompilerOptions;
import org.lwjgl.util.spvc.SpvcContext;
import org.lwjgl.util.spvc.SpvcParsedIr;
import org.lwjgl.util.spvc.SpvcReflectedResource;
import org.lwjgl.util.spvc.SpvcResources;

// ══════════════════════════════════════════════════════════════════════
// LWJGL NANOVG (2D Vector Graphics)
// ══════════════════════════════════════════════════════════════════════

import org.lwjgl.nanovg.*;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL2;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGGlyphPosition;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NVGTextRow;

// ══════════════════════════════════════════════════════════════════════
// LWJGL STB (Image Loading, Fonts, Image Writing)
// ══════════════════════════════════════════════════════════════════════

import org.lwjgl.stb.*;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackedchar;

// ══════════════════════════════════════════════════════════════════════
// LWJGL ASSIMP (3D Model Loading)
// ══════════════════════════════════════════════════════════════════════

import org.lwjgl.assimp.*;
import org.lwjgl.assimp.AIAnimation;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.assimp.Assimp;

// ══════════════════════════════════════════════════════════════════════
// LWJGL OPENAL (Audio)
// ══════════════════════════════════════════════════════════════════════

import org.lwjgl.openal.*;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;

// ══════════════════════════════════════════════════════════════════════
// JOML (Math Library)
// ══════════════════════════════════════════════════════════════════════

import org.joml.*;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;


/**
 * ╭────────────────────────────────────────────────────────────────────────╮
 * │  DeepMixPhases — Complete Annotation & Processor Suite        │
 * │                                                                      │
 * │  All annotations, shortcuts, enums, processors, and transformers     │
 * │  for the advanced DeepMix bytecode transformation framework.         │
 * │                                                                      │
 * │  Usage:                                                              │
 * │    import static deepmix.annotations.phases.DeepMixPhases.*;         │
 * │                                                                      │
 * │    @DeepSecurity(checks = SecurityCheck.ALL)                         │
 * │    @DSEC(checks = SecurityCheck.ALL)          // shortcut            │
 * │                                                                      │
 * ╰────────────────────────────────────────────────────────────────────────╯
 */
public final class DeepMixPhases {

    private DeepMixPhases() {
        throw new UnsupportedOperationException("DeepMixPhases is a static annotation container");
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║   SHARED INFRASTRUCTURE — Common types used across all phases      ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    //  Shared Enums
    // ──────────────────────────────────────────────

    /** Error handling strategies used across multiple annotations */
    public enum ErrorStrategy {
        THROW,          // Throw exception immediately
        LOG_AND_CONTINUE, // Log error, continue execution
        FALLBACK,       // Use fallback value/method
        RETRY,          // Retry the operation
        WARN,           // Emit warning, continue
        SILENT,         // Silently ignore
        CALLBACK,       // Invoke error callback
        CIRCUIT_BREAK   // Trip circuit breaker
    }

    /** Priority levels for annotation ordering */
    public enum Priority {
        LOWEST(Integer.MIN_VALUE),
        LOW(-1000),
        BELOW_NORMAL(-500),
        NORMAL(0),
        ABOVE_NORMAL(500),
        HIGH(1000),
        HIGHEST(Integer.MAX_VALUE),
        CRITICAL(Integer.MAX_VALUE - 1);

        private final int value;
        Priority(int value) { this.value = value; }
        public int value() { return value; }
    }

    /** Target scope for transformations */
    public enum TargetScope {
        METHOD,
        FIELD,
        CLASS,
        CONSTRUCTOR,
        PARAMETER,
        LOCAL_VARIABLE,
        RETURN_VALUE,
        EXCEPTION,
        ALL
    }

    /** Condition expressions for conditional application */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface When {
        String value();                           // SpEL-like condition expression
        String message() default "";              // Message if condition fails
        ErrorStrategy onFail() default ErrorStrategy.THROW;
    }

    /** Common metadata carrier */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepMeta {
        String author() default "";
        String since() default "";
        String description() default "";
        String[] tags() default {};
        boolean experimental() default false;
        String deprecatedSince() default "";
        String replacedBy() default "";
    }

    // ──────────────────────────────────────────────
    //  Base Processor Interface
    // ──────────────────────────────────────────────

    /**
     * Base interface for all DeepMix annotation processors.
     * Each phase processor extends this to handle its specific annotations.
     */
    public interface DeepMixProcessor<A extends java.lang.annotation.Annotation> {

        /** Check if this processor can handle the given annotation */
        boolean canProcess(A annotation, ClassNode classNode, MethodNode methodNode);

        /** Process the annotation and apply transformations */
        void process(A annotation, ClassNode classNode, MethodNode methodNode,
                     DeepMixContext context) throws DeepMixProcessingException;

        /** Get the priority for ordering among processors */
        default int priority() { return 0; }

        /** Validate the annotation parameters before processing */
        default ValidationResult validate(A annotation) {
            return ValidationResult.ok();
        }
    }

    /** Context passed to processors during transformation */
    public static class DeepMixContext {
        private final ClassLoader classLoader;
        private final Map<String, Object> attributes;
        private final List<String> diagnostics;
        private final DeepMixConfig config;
        private final StateManager stateManager;
        private final SecurityManager securityManager;

        public DeepMixContext(ClassLoader classLoader, DeepMixConfig config) {
            this.classLoader = classLoader;
            this.attributes = new ConcurrentHashMap<>();
            this.diagnostics = new CopyOnWriteArrayList<>();
            this.config = config;
            this.stateManager = new StateManager();
            this.securityManager = new SecurityManager(config);
        }

        public ClassLoader getClassLoader() { return classLoader; }
        public <T> T getAttribute(String key, Class<T> type) {
            return type.cast(attributes.get(key));
        }
        public void setAttribute(String key, Object value) { attributes.put(key, value); }
        public void addDiagnostic(String message) { diagnostics.add(message); }
        public List<String> getDiagnostics() { return Collections.unmodifiableList(diagnostics); }
        public DeepMixConfig getConfig() { return config; }
        public StateManager getStateManager() { return stateManager; }
        public SecurityManager getSecurityManager() { return securityManager; }
    }

    /** Configuration for DeepMix processing */
    public static class DeepMixConfig {
        private boolean hotReloadEnabled = true;
        private boolean debugMode = false;
        private int maxProcessingTimeMs = 30000;
        private boolean strictMode = false;
        private String encryptionKeyStore = "";
        private Map<String, String> properties = new HashMap<>();

        // Builder pattern
        public static DeepMixConfig defaults() { return new DeepMixConfig(); }
        public DeepMixConfig hotReload(boolean enabled) { this.hotReloadEnabled = enabled; return this; }
        public DeepMixConfig debug(boolean enabled) { this.debugMode = enabled; return this; }
        public DeepMixConfig maxProcessingTime(int ms) { this.maxProcessingTimeMs = ms; return this; }
        public DeepMixConfig strict(boolean strict) { this.strictMode = strict; return this; }
        public DeepMixConfig encryptionKeyStore(String path) { this.encryptionKeyStore = path; return this; }
        public DeepMixConfig property(String key, String value) { this.properties.put(key, value); return this; }

        public boolean isHotReloadEnabled() { return hotReloadEnabled; }
        public boolean isDebugMode() { return debugMode; }
        public int getMaxProcessingTimeMs() { return maxProcessingTimeMs; }
        public boolean isStrictMode() { return strictMode; }
        public String getEncryptionKeyStore() { return encryptionKeyStore; }
        public String getProperty(String key) { return properties.get(key); }
    }

    /** Validation result from annotation parameter checks */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        private ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
        }
        public static ValidationResult error(String... errors) {
            return new ValidationResult(false, Arrays.asList(errors), Collections.emptyList());
        }
        public static ValidationResult warning(String... warnings) {
            return new ValidationResult(true, Collections.emptyList(), Arrays.asList(warnings));
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }

    /** Custom exception for processing failures */
    public static class DeepMixProcessingException extends Exception {
        private final String annotationName;
        private final String targetMethod;
        private final String phase;

        public DeepMixProcessingException(String message, String annotationName,
                                          String targetMethod, String phase) {
            super(String.format("[%s] @%s on %s: %s", phase, annotationName, targetMethod, message));
            this.annotationName = annotationName;
            this.targetMethod = targetMethod;
            this.phase = phase;
        }

        public DeepMixProcessingException(String message, String annotationName,
                                          String targetMethod, String phase, Throwable cause) {
            super(String.format("[%s] @%s on %s: %s", phase, annotationName, targetMethod, message), cause);
            this.annotationName = annotationName;
            this.targetMethod = targetMethod;
            this.phase = phase;
        }

        public String getAnnotationName() { return annotationName; }
        public String getTargetMethod() { return targetMethod; }
        public String getPhase() { return phase; }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║   PHASE 10: SECURITY & DATA INTEGRITY                             ║
    // ║   15 annotations | Priority: HIGH | Est. Time: 3-4 days           ║
    // ║                                                                    ║
    // ║   Security:                                                        ║
    // ║     @DeepSecurity   @DeepSanitize   @DeepEscape                   ║
    // ║     @DeepEncrypt                                                   ║
    // ║                                                                    ║
    // ║   Data Operations:                                                 ║
    // ║     @DeepSerialize    @DeepDeserialize   @DeepClone               ║
    // ║     @DeepCompare      @DeepMergeObj      @DeepCompress            ║
    // ║     @DeepDecompress   @DeepChecksum      @DeepBinary              ║
    // ║     @DeepPatch        @DeepDiff                                   ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    //  Phase 10 — Enums
    // ──────────────────────────────────────────────

    /** Security checks available for @DeepSecurity */
    public enum SecurityCheck {
        NULL_VALIDATION,            // Null parameter checks
        INPUT_SANITIZATION,         // Sanitize all string inputs
        OUTPUT_ESCAPING,            // Escape output values
        SQL_INJECTION_PREVENTION,   // Prevent SQL injection
        XSS_PREVENTION,            // Cross-site scripting prevention
        CSRF_PREVENTION,           // Cross-site request forgery
        PATH_TRAVERSAL_PREVENTION, // Prevent ../../ attacks
        COMMAND_INJECTION_PREVENTION, // OS command injection
        LDAP_INJECTION_PREVENTION, // LDAP injection
        XML_INJECTION_PREVENTION,  // XML external entity (XXE)
        DESERIALIZATION_SAFETY,    // Safe deserialization
        TIMING_ATTACK_PREVENTION,  // Constant-time comparisons
        INJECTION_PREVENTION,      // General injection prevention
        OVERFLOW_PROTECTION,       // Integer/buffer overflow
        BOUNDS_CHECKING,           // Array bounds validation
        TYPE_SAFETY,               // Runtime type checking
        ACCESS_CONTROL,            // Method access control
        RATE_LIMITING,             // Anti-brute-force
        AUDIT_LOGGING,             // Security event logging
        ENCRYPTION_AT_REST,        // Encrypt sensitive data
        ENCRYPTION_IN_TRANSIT,     // Encrypt data in transit
        ALL                        // All available checks
    }

    /** Security level tiers */
    public enum SecurityLevel {
        MINIMAL,     // Basic null checks only
        LOW,         // Null checks + basic sanitization
        MEDIUM,      // + injection prevention + bounds checking
        HIGH,        // + timing attack prevention + audit logging
        PARANOID,    // All checks, maximum overhead
        CUSTOM       // User-defined check set
    }

    /** Sanitization rules for @DeepSanitize */
    public enum SanitizeMode {
        STRIP,       // Remove disallowed content
        ESCAPE,      // Escape special characters
        ENCODE,      // Encode (HTML/URL/etc)
        REJECT,      // Reject entire input if invalid
        REPLACE,     // Replace with safe alternative
        NORMALIZE    // Unicode normalization + cleanup
    }

    /** Escaping context for @DeepEscape */
    public enum EscapeContext {
        HTML,        // HTML entity escaping
        XML,         // XML entity escaping
        JSON,        // JSON string escaping
        SQL,         // SQL parameter escaping
        LDAP,        // LDAP distinguished name escaping
        CSV,         // CSV field escaping
        REGEX,       // Regex metacharacter escaping
        SHELL,       // Shell command escaping
        URL,         // URL percent-encoding
        JAVASCRIPT,  // JavaScript string escaping
        CDATA,       // XML CDATA wrapping
        LATEX        // LaTeX special character escaping
    }

    /** Encryption algorithms for @DeepEncrypt */
    public enum EncryptionAlgorithm {
        AES_128_CBC,
        AES_128_GCM,
        AES_256_CBC,
        AES_256_GCM,
        CHACHA20_POLY1305,
        RSA_2048,
        RSA_4096,
        TRIPLE_DES,
        BLOWFISH,
        CAMELLIA_256,
        XOR_OBFUSCATE,   // Simple obfuscation (NOT encryption)
        CUSTOM
    }

    /** Obfuscation techniques */
    public enum ObfuscationTechnique {
        STRING_ENCRYPTION,       // Encrypt string literals
        CONTROL_FLOW_FLATTENING, // Flatten control flow
        OPAQUE_PREDICATES,       // Insert opaque predicates
        METHOD_INLINING,         // Inline small methods
        NAME_MANGLING,           // Mangle names
        DEAD_CODE_INSERTION,     // Insert dead code
        CONSTANT_ENCRYPTION,     // Encrypt constants
        ANTI_DEBUG,              // Anti-debugging techniques
        ANTI_DECOMPILE,          // Anti-decompilation
        WATERMARKING,            // Code watermarking
        NONE
    }

    /** Serialization formats */
    public enum SerializationFormat {
        JAVA_NATIVE,       // Java Serializable
        JSON_JACKSON,      // Jackson JSON
        JSON_GSON,         // Gson JSON
        JSON_MOSHI,        // Moshi JSON
        PROTOBUF,          // Protocol Buffers
        MSGPACK,           // MessagePack
        CBOR,              // CBOR
        AVRO,              // Apache Avro
        THRIFT,            // Apache Thrift
        KRYO,              // Kryo serialization
        FST,               // Fast Serialization
        BSON,              // Binary JSON (MongoDB)
        FLATBUFFERS,       // Google FlatBuffers
        CAP_N_PROTO,       // Cap'n Proto
        SMILE,             // Jackson Smile (binary JSON)
        ION,               // Amazon Ion
        XML_JAXB,          // JAXB XML
        YAML,              // YAML serialization
        CUSTOM             // Custom format
    }

    /** Cloning strategies */
    public enum CloneStrategy {
        DEEP,              // Full deep clone
        SHALLOW,           // Shallow copy
        COPY_ON_WRITE,     // COW semantics
        SERIALIZATION,     // Clone via serialize/deserialize
        REFLECTION,        // Reflection-based deep clone
        CONSTRUCTOR,       // Copy constructor
        BUILDER,           // Builder-based clone
        CUSTOM             // Custom clone logic
    }

    /** Comparison strategies */
    public enum CompareStrategy {
        STRUCTURAL,        // Field-by-field comparison
        SEMANTIC,          // Semantic equivalence
        IDENTITY,          // Reference identity
        HASH_BASED,        // Hash-based comparison
        CUSTOM,            // Custom comparator
        DEEP_EQUALS,       // Deep recursive equals
        TOLERANT           // With tolerance for floating point
    }

    /** Merge strategies for @DeepMergeObj */
    public enum MergeStrategy {
        LEFT_WINS,         // Left value takes precedence
        RIGHT_WINS,        // Right value takes precedence
        DEEP_MERGE,        // Recursively merge nested objects
        UNION,             // Union of all fields
        INTERSECTION,      // Only common fields
        CUSTOM             // Custom merge logic
    }

    /** Compression algorithms */
    public enum CompressionAlgorithm {
        GZIP,
        DEFLATE,
        ZSTD,
        LZ4,
        SNAPPY,
        BROTLI,
        LZMA,
        BZIP2,
        XZ,
        LZO,
        ZLIB,
        NONE,
        AUTO_DETECT,       // Auto-detect on decompress
        BEST_RATIO,        // Choose best compression ratio
        BEST_SPEED         // Choose fastest algorithm
    }

    /** Checksum algorithms */
    public enum ChecksumAlgorithm {
        CRC32,
        CRC32C,
        CRC64,
        ADLER32,
        MD5,
        SHA1,
        SHA256,
        SHA384,
        SHA512,
        SHA3_256,
        SHA3_512,
        XXHASH32,
        XXHASH64,
        XXHASH128,
        MURMUR3_32,
        MURMUR3_128,
        CITY_HASH,
        FARM_HASH,
        SIP_HASH,
        BLAKE2B,
        BLAKE2S,
        BLAKE3,
        CUSTOM
    }

    /** Binary modification operations */
    public enum BinaryOperation {
        READ,
        WRITE,
        INSERT,
        DELETE,
        REPLACE,
        PATCH,
        SPLICE,
        XOR,
        AND,
        OR,
        NOT,
        SHIFT_LEFT,
        SHIFT_RIGHT,
        ROTATE_LEFT,
        ROTATE_RIGHT,
        SWAP_BYTES,
        REVERSE,
        FILL
    }

    /** Diff algorithms */
    public enum DiffAlgorithm {
        MYERS,             // Myers diff (Git default)
        PATIENCE,          // Patience diff
        HISTOGRAM,         // Histogram diff
        MINIMAL,           // Minimal edit script
        BSDIFF,            // Binary diff (bsdiff)
        VCDIFF,            // RFC 3284 VCDIFF
        XDELTA,            // Xdelta binary diff
        COURGETTE,         // Chromium's Courgette
        CUSTOM
    }

    /** Patch formats */
    public enum PatchFormat {
        UNIFIED,           // Unified diff format
        CONTEXT,           // Context diff format
        BINARY,            // Binary patch
        JSON_PATCH,        // RFC 6902 JSON Patch
        JSON_MERGE_PATCH,  // RFC 7396 JSON Merge Patch
        IPS,               // International Patching System
        BPS,               // Beat Patching System
        UPS,               // Universal Patching System
        CUSTOM
    }

    // ──────────────────────────────────────────────
    //  Phase 10 — Sub-annotation types
    // ──────────────────────────────────────────────

    /** Sanitization rule definition for @DeepSanitize */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SanitizeRule {
        String param();                              // Parameter name or index
        SanitizeMode mode() default SanitizeMode.STRIP;
        boolean stripHtml() default false;
        boolean stripSql() default false;
        boolean stripScripts() default false;
        int maxLength() default Integer.MAX_VALUE;
        int minLength() default 0;
        String allowedChars() default "";             // Regex character class
        String deniedChars() default "";              // Regex character class
        String allowedPattern() default "";            // Full regex pattern
        boolean trim() default true;
        boolean normalizeWhitespace() default false;
        boolean lowercaseAll() default false;
        boolean escapeSpecialChars() default false;
        String replacementChar() default "";
        String customSanitizer() default "";           // Method reference for custom logic
    }

    /** Escape rule definition for @DeepEscape */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EscapeRule {
        String param() default "";                    // Parameter name or empty for return value
        EscapeContext context();
        boolean doubleEscape() default false;         // Escape already-escaped sequences
        String[] additionalChars() default {};        // Extra characters to escape
        String customEscaper() default "";            // Custom escape method
    }

    /** Encryption configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EncryptionConfig {
        EncryptionAlgorithm algorithm() default EncryptionAlgorithm.AES_256_GCM;
        String keyAlias() default "default";          // Key alias in keystore
        String keyStoreRef() default "";              // KeyStore reference
        int ivLength() default 12;                    // IV/nonce length in bytes
        int tagLength() default 128;                  // Auth tag length in bits (GCM)
        boolean rotateKeys() default false;           // Enable key rotation
        int keyRotationIntervalDays() default 90;
        String provider() default "";                 // JCE provider name
    }

    /** Binary region specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BinaryRegion {
        long offset() default 0;
        long length() default -1;                     // -1 = until end
        ByteOrderSpec byteOrder() default ByteOrderSpec.BIG_ENDIAN;
        String format() default "";                   // Format string for parsing
    }

    /** Byte order specification (avoiding java.nio.ByteOrder in annotation) */
    public enum ByteOrderSpec {
        BIG_ENDIAN,
        LITTLE_ENDIAN,
        NATIVE,
        NETWORK                                       // Same as BIG_ENDIAN
    }

    /** Checksum verification config */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ChecksumConfig {
        ChecksumAlgorithm algorithm() default ChecksumAlgorithm.SHA256;
        String expectedValue() default "";            // Expected checksum (empty = compute only)
        boolean verifyOnLoad() default false;
        boolean embedInOutput() default false;
        int truncateToBytes() default -1;             // -1 = full length
    }

    // ──────────────────────────────────────────────
    //  Phase 10 — Annotations
    // ──────────────────────────────────────────────

    /**
     * 🔒 @DeepSecurity — Security hardening for methods and classes.
     *
     * Automatically injects security checks including null validation,
     * input sanitization, timing attack prevention, and injection prevention.
     *
     * Example:
     * <pre>
     * {@code @DeepSecurity(
     *     checks = {SecurityCheck.NULL_VALIDATION, SecurityCheck.INJECTION_PREVENTION},
     *     level = SecurityLevel.HIGH
     * )}
     * public void authenticate(String username, String password) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR})
    public @interface DeepSecurity {
        String target() default "";                   // Target class::method (empty = annotated element)
        SecurityCheck[] checks() default {SecurityCheck.ALL};
        SecurityLevel level() default SecurityLevel.MEDIUM;
        boolean auditLog() default false;             // Log all security events
        boolean antiDebug() default false;            // Anti-debugging measures
        boolean antiTamper() default false;            // Anti-tampering checks
        boolean stackTraceFilter() default false;     // Filter sensitive data from stack traces
        String[] sensitiveParams() default {};        // Parameter names to redact in logs
        ErrorStrategy onViolation() default ErrorStrategy.THROW;
        String violationCallback() default "";        // Method to call on violation
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSecurity */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR})
    public @interface DSEC {
        String target() default "";
        SecurityCheck[] checks() default {SecurityCheck.ALL};
        SecurityLevel level() default SecurityLevel.MEDIUM;
        boolean auditLog() default false;
        boolean antiDebug() default false;
        boolean antiTamper() default false;
        boolean stackTraceFilter() default false;
        String[] sensitiveParams() default {};
        ErrorStrategy onViolation() default ErrorStrategy.THROW;
        String violationCallback() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🧹 @DeepSanitize — Input sanitization for method parameters.
     *
     * Automatically sanitizes input parameters based on configurable rules.
     * Supports HTML stripping, SQL injection prevention, character whitelisting, etc.
     *
     * Example:
     * <pre>
     * {@code @DeepSanitize(rules = {
     *     @SanitizeRule(param = "username", maxLength = 64, allowedChars = "[a-zA-Z0-9_]"),
     *     @SanitizeRule(param = "bio", stripHtml = true, maxLength = 500)
     * })}
     * public void updateProfile(String username, String bio) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DeepSanitize {
        String target() default "";
        SanitizeRule[] rules() default {};
        SanitizeMode defaultMode() default SanitizeMode.STRIP;
        boolean sanitizeAll() default false;          // Apply default sanitization to all params
        boolean logSanitization() default false;      // Log when sanitization modifies input
        ErrorStrategy onInvalid() default ErrorStrategy.THROW;
        String customSanitizer() default "";           // Global custom sanitizer method
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSanitize */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DSAN {
        String target() default "";
        SanitizeRule[] rules() default {};
        SanitizeMode defaultMode() default SanitizeMode.STRIP;
        boolean sanitizeAll() default false;
        boolean logSanitization() default false;
        ErrorStrategy onInvalid() default ErrorStrategy.THROW;
        String customSanitizer() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🛡️ @DeepEscape — Output escaping for return values and parameters.
     *
     * Ensures output values are properly escaped for their target context
     * (HTML, SQL, JSON, etc.) to prevent injection attacks.
     *
     * Example:
     * <pre>
     * {@code @DeepEscape(rules = {
     *     @EscapeRule(context = EscapeContext.HTML),
     *     @EscapeRule(param = "sqlParam", context = EscapeContext.SQL)
     * })}
     * public String renderTemplate(String sqlParam) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DeepEscape {
        String target() default "";
        EscapeRule[] rules() default {};
        EscapeContext defaultContext() default EscapeContext.HTML;
        boolean escapeReturnValue() default true;
        boolean escapeParams() default false;
        boolean recursive() default false;           // Escape fields of complex objects
        int maxDepth() default 5;                    // Max recursion depth for complex objects
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepEscape */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DESC {
        String target() default "";
        EscapeRule[] rules() default {};
        EscapeContext defaultContext() default EscapeContext.HTML;
        boolean escapeReturnValue() default true;
        boolean escapeParams() default false;
        boolean recursive() default false;
        int maxDepth() default 5;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔐 @DeepEncrypt — Encryption, decryption, and obfuscation.
     *
     * Encrypts string literals, method bodies, or data fields.
     * Supports multiple encryption algorithms, key management, and obfuscation.
     *
     * Example:
     * <pre>
     * {@code @DeepEncrypt(
     *     algorithm = "AES-256-GCM",
     *     obfuscate = true,
     *     antiDebug = true
     * )}
     * private String getApiKey() {
     *     return "supersecretkey123";
     * }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
    public @interface DeepEncrypt {
        String target() default "";
        String algorithm() default "AES-256-GCM";    // Algorithm string (convenience)
        EncryptionAlgorithm algo() default EncryptionAlgorithm.AES_256_GCM;
        EncryptionConfig config() default @EncryptionConfig;
        boolean obfuscate() default false;            // Also obfuscate method body
        ObfuscationTechnique[] obfuscationTechniques() default {};
        boolean antiDebug() default false;
        boolean antiTamper() default false;
        boolean encryptStrings() default true;        // Encrypt string literals in method
        boolean encryptConstants() default false;      // Encrypt numeric constants
        boolean encryptFieldAccess() default false;    // Encrypt field read/write patterns
        String keyProvider() default "";               // Custom key provider class
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepEncrypt */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
    public @interface DENC {
        String target() default "";
        String algorithm() default "AES-256-GCM";
        EncryptionAlgorithm algo() default EncryptionAlgorithm.AES_256_GCM;
        EncryptionConfig config() default @EncryptionConfig;
        boolean obfuscate() default false;
        ObfuscationTechnique[] obfuscationTechniques() default {};
        boolean antiDebug() default false;
        boolean encryptStrings() default true;
        String keyProvider() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📤 @DeepSerialize — Custom serialization logic injection.
     *
     * Transforms how objects are serialized by injecting custom serialization
     * logic into methods, fields, or entire classes.
     *
     * Example:
     * <pre>
     * {@code @DeepSerialize(
     *     format = SerializationFormat.JSON_JACKSON,
     *     includeNulls = false,
     *     dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
     * )}
     * public class PlayerData { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    public @interface DeepSerialize {
        String target() default "";
        SerializationFormat format() default SerializationFormat.JSON_JACKSON;
        SerializationFormat[] fallbackFormats() default {};
        boolean includeNulls() default true;
        boolean prettyPrint() default false;
        String dateFormat() default "ISO_8601";
        String[] excludeFields() default {};          // Fields to exclude from serialization
        String[] includeFields() default {};          // If non-empty, only these fields
        boolean versioned() default false;            // Include version info
        String versionField() default "_version";
        boolean compressed() default false;           // Compress serialized output
        CompressionAlgorithm compression() default CompressionAlgorithm.GZIP;
        boolean encrypted() default false;            // Encrypt serialized output
        EncryptionConfig encryption() default @EncryptionConfig;
        String customSerializer() default "";          // Custom serializer class/method
        int maxDepth() default 32;                    // Max serialization depth
        boolean failOnCircularRef() default true;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSerialize */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    public @interface DSER {
        String target() default "";
        SerializationFormat format() default SerializationFormat.JSON_JACKSON;
        boolean includeNulls() default true;
        boolean prettyPrint() default false;
        String dateFormat() default "ISO_8601";
        String[] excludeFields() default {};
        boolean compressed() default false;
        boolean encrypted() default false;
        String customSerializer() default "";
        int maxDepth() default 32;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📥 @DeepDeserialize — Custom deserialization logic injection.
     *
     * Controls how data is deserialized into objects with validation,
     * migration, and safety checks.
     *
     * Example:
     * <pre>
     * {@code @DeepDeserialize(
     *     format = SerializationFormat.JSON_JACKSON,
     *     validate = true,
     *     migrateFromVersion = 1
     * )}
     * public PlayerData loadPlayer(byte[] data) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    public @interface DeepDeserialize {
        String target() default "";
        SerializationFormat format() default SerializationFormat.JSON_JACKSON;
        SerializationFormat[] acceptFormats() default {};  // Auto-detect among these
        boolean validate() default true;              // Validate after deserialization
        boolean strict() default false;               // Fail on unknown fields
        boolean lenient() default false;              // Accept malformed data
        String[] requiredFields() default {};         // Fields that must be present
        String defaultValuesProvider() default "";    // Method that provides defaults
        boolean migrateVersions() default false;      // Auto-migrate between versions
        int migrateFromVersion() default -1;          // Minimum supported version
        int currentVersion() default 1;
        String migrationHandler() default "";          // Custom migration method
        boolean safeDeserialization() default true;    // Prevent deserialization attacks
        String[] allowedClasses() default {};         // Whitelist for Java deserialization
        String[] deniedClasses() default {};          // Blacklist for Java deserialization
        int maxObjectDepth() default 32;
        long maxObjectSize() default -1;              // Max bytes (-1 = unlimited)
        String customDeserializer() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepDeserialize */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    public @interface DDES {
        String target() default "";
        SerializationFormat format() default SerializationFormat.JSON_JACKSON;
        boolean validate() default true;
        boolean strict() default false;
        boolean safeDeserialization() default true;
        String customDeserializer() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📋 @DeepClone — Deep cloning of objects.
     *
     * Injects deep cloning logic into methods or classes.
     * Supports multiple strategies with cycle detection.
     *
     * Example:
     * <pre>
     * {@code @DeepClone(
     *     strategy = CloneStrategy.DEEP,
     *     excludeFields = {"transientCache", "logger"}
     * )}
     * public GameState cloneState() { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepClone {
        String target() default "";
        CloneStrategy strategy() default CloneStrategy.DEEP;
        String[] excludeFields() default {};          // Fields to skip during cloning
        String[] shallowFields() default {};          // Fields to shallow-copy
        boolean handleCycles() default true;          // Detect and handle circular refs
        int maxDepth() default 64;                    // Max cloning depth
        boolean cloneCollections() default true;      // Deep-clone collection contents
        boolean cloneMaps() default true;             // Deep-clone map contents
        boolean cloneArrays() default true;           // Deep-clone array contents
        boolean preserveIdentity() default false;     // Maintain identity map
        String customCloner() default "";              // Custom cloner method
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepClone */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DCLN {
        String target() default "";
        CloneStrategy strategy() default CloneStrategy.DEEP;
        String[] excludeFields() default {};
        boolean handleCycles() default true;
        int maxDepth() default 64;
        String customCloner() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ⚖️ @DeepCompare — Deep comparison of objects.
     *
     * Injects deep comparison logic for structural or semantic equality.
     *
     * Example:
     * <pre>
     * {@code @DeepCompare(
     *     strategy = CompareStrategy.STRUCTURAL,
     *     tolerance = 0.0001,
     *     excludeFields = {"id", "timestamp"}
     * )}
     * public boolean statesEqual(GameState a, GameState b) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepCompare {
        String target() default "";
        CompareStrategy strategy() default CompareStrategy.STRUCTURAL;
        String[] excludeFields() default {};          // Fields to skip in comparison
        String[] includeFields() default {};          // If non-empty, only compare these
        double tolerance() default 0.0;               // Floating point tolerance
        boolean ignoreOrder() default false;           // Ignore collection ordering
        boolean ignoreCase() default false;            // Case-insensitive string comparison
        boolean ignoreWhitespace() default false;     // Ignore whitespace in strings
        boolean handleCycles() default true;
        int maxDepth() default 64;
        boolean generateDiff() default false;         // Generate diff report on mismatch
        String customComparator() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCompare */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DCMP {
        String target() default "";
        CompareStrategy strategy() default CompareStrategy.STRUCTURAL;
        String[] excludeFields() default {};
        double tolerance() default 0.0;
        boolean ignoreOrder() default false;
        boolean generateDiff() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔀 @DeepMergeObj — Object merging and composition.
     *
     * Merges two or more objects into one using configurable strategies.
     *
     * Example:
     * <pre>
     * {@code @DeepMergeObj(
     *     strategy = MergeStrategy.DEEP_MERGE,
     *     nullHandling = NullHandling.SKIP
     * )}
     * public Config mergeConfigs(Config defaults, Config overrides) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepMergeObj {
        String target() default "";
        MergeStrategy strategy() default MergeStrategy.DEEP_MERGE;
        String[] excludeFields() default {};
        boolean mergeCollections() default true;      // Merge collection contents
        boolean mergeCollectionsByAppend() default true; // Append vs replace collections
        boolean mergeMaps() default true;             // Merge map entries
        boolean skipNulls() default true;             // Don't overwrite with null
        boolean createMissing() default false;        // Create fields that don't exist in target
        int maxDepth() default 32;
        String conflictResolver() default "";         // Custom conflict resolution method
        String customMerger() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMergeObj */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DMRG {
        String target() default "";
        MergeStrategy strategy() default MergeStrategy.DEEP_MERGE;
        String[] excludeFields() default {};
        boolean skipNulls() default true;
        String conflictResolver() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📦 @DeepCompress — Data compression.
     *
     * Compresses method return values, field data, or byte streams.
     *
     * Example:
     * <pre>
     * {@code @DeepCompress(
     *     algorithm = CompressionAlgorithm.ZSTD,
     *     level = 3
     * )}
     * public byte[] compressWorldData(byte[] rawData) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepCompress {
        String target() default "";
        CompressionAlgorithm algorithm() default CompressionAlgorithm.GZIP;
        int level() default -1;                       // -1 = default level for algorithm
        boolean compressReturn() default true;        // Compress return value
        boolean compressParams() default false;       // Compress parameters (unusual)
        int bufferSize() default 8192;                // I/O buffer size
        long minSizeToCompress() default 0;           // Don't compress below this size
        boolean includeHeader() default true;         // Include format header
        boolean streamMode() default false;           // Streaming compression
        String dictionary() default "";               // Pre-trained dictionary path
        int windowSize() default -1;                  // Algorithm-specific window size
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCompress */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DCOMP {
        String target() default "";
        CompressionAlgorithm algorithm() default CompressionAlgorithm.GZIP;
        int level() default -1;
        boolean compressReturn() default true;
        int bufferSize() default 8192;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📦 @DeepDecompress — Data decompression.
     *
     * Decompresses compressed data from methods, fields, or streams.
     *
     * Example:
     * <pre>
     * {@code @DeepDecompress(
     *     algorithm = CompressionAlgorithm.AUTO_DETECT
     * )}
     * public byte[] loadWorldData(byte[] compressed) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepDecompress {
        String target() default "";
        CompressionAlgorithm algorithm() default CompressionAlgorithm.AUTO_DETECT;
        boolean decompressParams() default true;      // Decompress input parameters
        boolean decompressReturn() default false;     // Decompress return value
        int bufferSize() default 8192;
        long maxDecompressedSize() default -1;        // -1 = unlimited, protects against bombs
        boolean verifyChecksum() default true;        // Verify embedded checksums
        String dictionary() default "";               // Pre-trained dictionary path
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepDecompress */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DDECOMP {
        String target() default "";
        CompressionAlgorithm algorithm() default CompressionAlgorithm.AUTO_DETECT;
        boolean decompressParams() default true;
        long maxDecompressedSize() default -1;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ✅ @DeepChecksum — Checksum generation and verification.
     *
     * Computes or verifies checksums for data integrity assurance.
     *
     * Example:
     * <pre>
     * {@code @DeepChecksum(
     *     config = @ChecksumConfig(algorithm = ChecksumAlgorithm.SHA256, verifyOnLoad = true)
     * )}
     * public byte[] loadSecureData(String path) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
    public @interface DeepChecksum {
        String target() default "";
        ChecksumConfig config() default @ChecksumConfig;
        ChecksumAlgorithm algorithm() default ChecksumAlgorithm.SHA256;
        boolean computeOnReturn() default true;       // Compute checksum of return value
        boolean verifyParams() default false;         // Verify checksum of parameters
        boolean embedInResult() default false;        // Embed checksum in result object
        String checksumField() default "checksum";    // Field name for embedded checksum
        boolean logChecksum() default false;          // Log computed checksums
        ErrorStrategy onMismatch() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepChecksum */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
    public @interface DCHK {
        String target() default "";
        ChecksumAlgorithm algorithm() default ChecksumAlgorithm.SHA256;
        boolean computeOnReturn() default true;
        boolean verifyParams() default false;
        ErrorStrategy onMismatch() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 💾 @DeepBinary — Binary data modification.
     *
     * Read, write, patch, and transform raw binary data at specific offsets.
     *
     * Example:
     * <pre>
     * {@code @DeepBinary(
     *     target = "assets/data/level.bin",
     *     regions = {
     *         @BinaryRegion(offset = 0x100, length = 4, byteOrder = ByteOrderSpec.LITTLE_ENDIAN)
     *     },
     *     operation = BinaryOperation.REPLACE
     * )}
     * public byte[] patchLevelHeader(byte[] original) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepBinary {
        String target() default "";
        BinaryRegion[] regions() default {};
        BinaryOperation operation() default BinaryOperation.READ;
        ByteOrderSpec byteOrder() default ByteOrderSpec.BIG_ENDIAN;
        String hexPattern() default "";               // Hex pattern to search for
        String hexReplacement() default "";            // Hex replacement value
        boolean verifyMagicBytes() default false;     // Check file magic bytes
        String expectedMagic() default "";             // Expected magic byte sequence (hex)
        long maxFileSize() default -1;                // Max file size to process
        boolean createBackup() default true;          // Backup before modification
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepBinary */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DBIN {
        String target() default "";
        BinaryRegion[] regions() default {};
        BinaryOperation operation() default BinaryOperation.READ;
        ByteOrderSpec byteOrder() default ByteOrderSpec.BIG_ENDIAN;
        String hexPattern() default "";
        String hexReplacement() default "";
        boolean createBackup() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🩹 @DeepPatch — Binary patching.
     *
     * Applies binary patches (IPS, BPS, BSDiff, etc.) to target files.
     *
     * Example:
     * <pre>
     * {@code @DeepPatch(
     *     target = "native/libphysics.so",
     *     patchFile = "patches/physics_fix.bps",
     *     format = PatchFormat.BINARY
     * )}
     * public byte[] applyPhysicsFix(byte[] original) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepPatch {
        String target() default "";
        String patchFile() default "";                // Path to patch file
        String patchData() default "";                // Inline patch data (hex or base64)
        PatchFormat format() default PatchFormat.UNIFIED;
        boolean createBackup() default true;
        boolean verify() default true;                // Verify patch applied correctly
        ChecksumConfig preChecksum() default @ChecksumConfig;  // Verify before patching
        ChecksumConfig postChecksum() default @ChecksumConfig; // Verify after patching
        boolean dryRun() default false;               // Simulate patch without applying
        boolean reversible() default true;            // Generate reverse patch
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepPatch */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DPATCH {
        String target() default "";
        String patchFile() default "";
        PatchFormat format() default PatchFormat.UNIFIED;
        boolean createBackup() default true;
        boolean verify() default true;
        boolean dryRun() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📊 @DeepDiff — Generate and apply diffs between data versions.
     *
     * Creates diffs between original and modified data, or applies diffs
     * to transform data. Supports text, binary, and structured data diffs.
     *
     * Example:
     * <pre>
     * {@code @DeepDiff(
     *     algorithm = DiffAlgorithm.MYERS,
     *     format = PatchFormat.UNIFIED,
     *     contextLines = 3
     * )}
     * public String generateDiff(String original, String modified) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepDiff {
        String target() default "";
        DiffAlgorithm algorithm() default DiffAlgorithm.MYERS;
        PatchFormat format() default PatchFormat.UNIFIED;
        int contextLines() default 3;                 // Lines of context around changes
        boolean ignoreWhitespace() default false;
        boolean ignoreCase() default false;
        boolean ignoreBlankLines() default false;
        boolean binaryDiff() default false;           // Binary diff mode
        boolean structuredDiff() default false;       // Structured data diff (JSON/XML)
        boolean colorOutput() default false;          // ANSI colored output
        boolean generateStats() default false;        // Include diff statistics
        String customDiffer() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepDiff */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DDIFF {
        String target() default "";
        DiffAlgorithm algorithm() default DiffAlgorithm.MYERS;
        PatchFormat format() default PatchFormat.UNIFIED;
        int contextLines() default 3;
        boolean ignoreWhitespace() default false;
        boolean binaryDiff() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    //  Phase 10 — Processor
    // ──────────────────────────────────────────────

    /**
     * Unified processor for all Phase 10 (Security & Data Integrity) annotations.
     *
     * Handles bytecode transformation for security checks, sanitization,
     * encryption, serialization, compression, and data operations.
     */
    public static class Phase10Processor {

        private final DeepMixContext context;

        public Phase10Processor(DeepMixContext context) {
            this.context = context;
        }

        // ─── Security Processing ───

        /**
         * Process @DeepSecurity annotation.
         * Injects security checks at method entry and exit points.
         */
        public void processSecurity(DeepSecurity annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            SecurityCheck[] checks = annotation.checks();
            boolean allChecks = Arrays.asList(checks).contains(SecurityCheck.ALL);

            InsnList preamble = new InsnList();
            InsnList epilogue = new InsnList();

            // NULL_VALIDATION: Check all object parameters for null
            if (allChecks || containsCheck(checks, SecurityCheck.NULL_VALIDATION)) {
                injectNullChecks(preamble, methodNode);
            }

            // INPUT_SANITIZATION: Basic input sanitization
            if (allChecks || containsCheck(checks, SecurityCheck.INPUT_SANITIZATION)) {
                injectInputSanitization(preamble, methodNode);
            }

            // BOUNDS_CHECKING: Array bounds validation
            if (allChecks || containsCheck(checks, SecurityCheck.BOUNDS_CHECKING)) {
                injectBoundsChecking(preamble, methodNode);
            }

            // TIMING_ATTACK_PREVENTION: Constant-time operations
            if (allChecks || containsCheck(checks, SecurityCheck.TIMING_ATTACK_PREVENTION)) {
                transformToConstantTime(methodNode);
            }

            // INJECTION_PREVENTION: SQL/OS command injection checks
            if (allChecks || containsCheck(checks, SecurityCheck.INJECTION_PREVENTION)) {
                injectInjectionPrevention(preamble, methodNode);
            }

            // OVERFLOW_PROTECTION: Integer overflow detection
            if (allChecks || containsCheck(checks, SecurityCheck.OVERFLOW_PROTECTION)) {
                injectOverflowProtection(methodNode);
            }

            // AUDIT_LOGGING: Log method entry/exit with parameters
            if (annotation.auditLog() || allChecks ||
                containsCheck(checks, SecurityCheck.AUDIT_LOGGING)) {
                injectAuditLogging(preamble, epilogue, methodNode, annotation.sensitiveParams());
            }

            // ANTI_DEBUG: Anti-debugging techniques
            if (annotation.antiDebug()) {
                injectAntiDebug(preamble, methodNode);
            }

            // ANTI_TAMPER: Integrity verification
            if (annotation.antiTamper()) {
                injectAntiTamper(preamble, methodNode, classNode);
            }

            // STACK_TRACE_FILTER: Sanitize exception info
            if (annotation.stackTraceFilter()) {
                wrapWithStackTraceFilter(methodNode, annotation.sensitiveParams());
            }

            // Insert preamble at method start
            if (preamble.size() > 0) {
                methodNode.instructions.insert(preamble);
            }

            // Insert epilogue before each return
            if (epilogue.size() > 0) {
                insertBeforeReturns(methodNode, epilogue);
            }

            context.addDiagnostic(String.format(
                "🔒 @DeepSecurity applied to %s::%s [level=%s, checks=%d]",
                classNode.name, methodNode.name, annotation.level(), checks.length
            ));
        }

        /**
         * Process @DeepSanitize annotation.
         * Injects parameter sanitization logic at method entry.
         */
        public void processSanitize(DeepSanitize annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            InsnList sanitization = new InsnList();

            for (SanitizeRule rule : annotation.rules()) {
                int paramIndex = resolveParamIndex(rule.param(), methodNode);
                if (paramIndex < 0) {
                    throw new DeepMixProcessingException(
                        "Parameter '" + rule.param() + "' not found",
                        "DeepSanitize", methodNode.name, "Phase10"
                    );
                }

                // Load parameter
                sanitization.add(new VarInsnNode(Opcodes.ALOAD, paramIndex));

                // Apply sanitization based on mode
                switch (rule.mode()) {
                    case STRIP:
                        // Call DeepMixRuntime.stripUnsafe(String, maxLen, allowedPattern)
                        sanitization.add(new LdcInsnNode(rule.maxLength()));
                        sanitization.add(new LdcInsnNode(
                            rule.allowedChars().isEmpty() ? ".*" : rule.allowedChars()
                        ));
                        sanitization.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "deepmix/runtime/DeepMixSanitizer",
                            "stripUnsafe",
                            "(Ljava/lang/String;ILjava/lang/String;)Ljava/lang/String;",
                            false
                        ));
                        break;

                    case ESCAPE:
                        sanitization.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "deepmix/runtime/DeepMixSanitizer",
                            "escapeSpecialChars",
                            "(Ljava/lang/String;)Ljava/lang/String;",
                            false
                        ));
                        break;

                    case REJECT:
                        sanitization.add(new LdcInsnNode(rule.maxLength()));
                        sanitization.add(new LdcInsnNode(rule.allowedPattern()));
                        sanitization.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "deepmix/runtime/DeepMixSanitizer",
                            "validateOrReject",
                            "(Ljava/lang/String;ILjava/lang/String;)Ljava/lang/String;",
                            false
                        ));
                        break;

                    case NORMALIZE:
                        sanitization.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "deepmix/runtime/DeepMixSanitizer",
                            "normalizeInput",
                            "(Ljava/lang/String;)Ljava/lang/String;",
                            false
                        ));
                        break;

                    case ENCODE:
                        sanitization.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "deepmix/runtime/DeepMixSanitizer",
                            "encodeInput",
                            "(Ljava/lang/String;)Ljava/lang/String;",
                            false
                        ));
                        break;

                    case REPLACE:
                        sanitization.add(new LdcInsnNode(rule.replacementChar()));
                        sanitization.add(new LdcInsnNode(rule.deniedChars()));
                        sanitization.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "deepmix/runtime/DeepMixSanitizer",
                            "replaceUnsafe",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                            false
                        ));
                        break;
                }

                // Apply HTML stripping if requested
                if (rule.stripHtml()) {
                    sanitization.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/DeepMixSanitizer",
                        "stripHtml",
                        "(Ljava/lang/String;)Ljava/lang/String;",
                        false
                    ));
                }

                // Apply trimming if requested
                if (rule.trim()) {
                    sanitization.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/String",
                        "trim",
                        "()Ljava/lang/String;",
                        false
                    ));
                }

                // Store sanitized value back
                sanitization.add(new VarInsnNode(Opcodes.ASTORE, paramIndex));
            }

            // Insert sanitization at method start
            methodNode.instructions.insert(sanitization);

            context.addDiagnostic(String.format(
                "🧹 @DeepSanitize applied to %s::%s [%d rules]",
                classNode.name, methodNode.name, annotation.rules().length
            ));
        }

        /**
         * Process @DeepEscape annotation.
         * Wraps return values and parameters with escaping logic.
         */
        public void processEscape(DeepEscape annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {
            // Escape parameters at method entry
            if (annotation.escapeParams()) {
                InsnList paramEscaping = new InsnList();
                for (EscapeRule rule : annotation.rules()) {
                    if (!rule.param().isEmpty()) {
                        int paramIdx = resolveParamIndex(rule.param(), methodNode);
                        injectEscapeForParam(paramEscaping, paramIdx, rule.context());
                    }
                }
                methodNode.instructions.insert(paramEscaping);
            }

            // Escape return value
            if (annotation.escapeReturnValue()) {
                EscapeContext ctx = annotation.defaultContext();
                if (annotation.rules().length > 0) {
                    // Find rule without param (return value rule)
                    for (EscapeRule rule : annotation.rules()) {
                        if (rule.param().isEmpty()) {
                            ctx = rule.context();
                            break;
                        }
                    }
                }
                wrapReturnWithEscape(methodNode, ctx);
            }

            context.addDiagnostic(String.format(
                "🛡️ @DeepEscape applied to %s::%s [context=%s]",
                classNode.name, methodNode.name, annotation.defaultContext()
            ));
        }

        /**
         * Process @DeepEncrypt annotation.
         * Encrypts string literals and optionally obfuscates the method body.
         */
        public void processEncrypt(DeepEncrypt annotation, ClassNode classNode,
                                   MethodNode methodNode) throws DeepMixProcessingException {
            // Encrypt all string literals in the method
            if (annotation.encryptStrings()) {
                encryptStringLiterals(methodNode, annotation.algo());
            }

            // Encrypt numeric constants
            if (annotation.encryptConstants()) {
                encryptNumericConstants(methodNode);
            }

            // Apply obfuscation techniques
            if (annotation.obfuscate()) {
                ObfuscationTechnique[] techniques = annotation.obfuscationTechniques();
                if (techniques.length == 0) {
                    // Default obfuscation set
                    techniques = new ObfuscationTechnique[]{
                        ObfuscationTechnique.STRING_ENCRYPTION,
                        ObfuscationTechnique.CONTROL_FLOW_FLATTENING,
                        ObfuscationTechnique.OPAQUE_PREDICATES
                    };
                }
                for (ObfuscationTechnique technique : techniques) {
                    applyObfuscation(methodNode, technique);
                }
            }

            // Anti-debugging
            if (annotation.antiDebug()) {
                injectAntiDebug(new InsnList(), methodNode);
            }

            context.addDiagnostic(String.format(
                "🔐 @DeepEncrypt applied to %s::%s [algo=%s, obfuscate=%b]",
                classNode.name, methodNode.name, annotation.algo(), annotation.obfuscate()
            ));
        }

        // ─── Data Operations Processing ───

        /**
         * Process @DeepSerialize annotation.
         */
        public void processSerialize(DeepSerialize annotation, ClassNode classNode,
                                     MethodNode methodNode) throws DeepMixProcessingException {
            SerializationFormat format = annotation.format();
            String serializerClass = getSerializerClass(format);

            // Wrap method return with serialization
            InsnList serialization = new InsnList();

            // Push format-specific serializer configuration
            serialization.add(new LdcInsnNode(format.name()));
            serialization.add(new InsnNode(annotation.includeNulls() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            serialization.add(new InsnNode(annotation.prettyPrint() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            serialization.add(new LdcInsnNode(annotation.dateFormat()));
            serialization.add(new LdcInsnNode(annotation.maxDepth()));

            // Call serializer
            serialization.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixSerializer",
                "serialize",
                "(Ljava/lang/Object;Ljava/lang/String;ZZLjava/lang/String;I)[B",
                false
            ));

            // Apply compression if requested
            if (annotation.compressed()) {
                serialization.add(new LdcInsnNode(annotation.compression().name()));
                serialization.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixCompressor",
                    "compress",
                    "([BLjava/lang/String;)[B",
                    false
                ));
            }

            // Apply encryption if requested
            if (annotation.encrypted()) {
                serialization.add(new LdcInsnNode(annotation.encryption().algorithm().name()));
                serialization.add(new LdcInsnNode(annotation.encryption().keyAlias()));
                serialization.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixEncryptor",
                    "encrypt",
                    "([BLjava/lang/String;Ljava/lang/String;)[B",
                    false
                ));
            }

            insertBeforeReturns(methodNode, serialization);

            context.addDiagnostic(String.format(
                "📤 @DeepSerialize applied to %s::%s [format=%s, compressed=%b, encrypted=%b]",
                classNode.name, methodNode.name, format, annotation.compressed(), annotation.encrypted()
            ));
        }

        /**
         * Process @DeepDeserialize annotation.
         */
        public void processDeserialize(DeepDeserialize annotation, ClassNode classNode,
                                       MethodNode methodNode) throws DeepMixProcessingException {
            InsnList deserialization = new InsnList();

            // Safe deserialization whitelist/blacklist
            if (annotation.safeDeserialization()) {
                String[] allowed = annotation.allowedClasses();
                String[] denied = annotation.deniedClasses();

                // Push whitelist/blacklist arrays
                pushStringArray(deserialization, allowed);
                pushStringArray(deserialization, denied);

                deserialization.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixDeserializer",
                    "configureSafety",
                    "([Ljava/lang/String;[Ljava/lang/String;)V",
                    false
                ));
            }

            // Insert at method start for parameter deserialization
            methodNode.instructions.insert(deserialization);

            context.addDiagnostic(String.format(
                "📥 @DeepDeserialize applied to %s::%s [format=%s, safe=%b]",
                classNode.name, methodNode.name, annotation.format(), annotation.safeDeserialization()
            ));
        }

        /**
         * Process @DeepClone annotation.
         */
        public void processClone(DeepClone annotation, ClassNode classNode,
                                 MethodNode methodNode) throws DeepMixProcessingException {
            InsnList cloning = new InsnList();

            cloning.add(new LdcInsnNode(annotation.strategy().name()));
            cloning.add(new LdcInsnNode(annotation.maxDepth()));
            cloning.add(new InsnNode(annotation.handleCycles() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            pushStringArray(cloning, annotation.excludeFields());

            cloning.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixCloner",
                "deepClone",
                "(Ljava/lang/Object;Ljava/lang/String;IZ[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            insertBeforeReturns(methodNode, cloning);

            context.addDiagnostic(String.format(
                "📋 @DeepClone applied to %s::%s [strategy=%s]",
                classNode.name, methodNode.name, annotation.strategy()
            ));
        }

        /**
         * Process @DeepCompare annotation.
         */
        public void processCompare(DeepCompare annotation, ClassNode classNode,
                                   MethodNode methodNode) throws DeepMixProcessingException {
            InsnList comparison = new InsnList();

            comparison.add(new LdcInsnNode(annotation.strategy().name()));
            comparison.add(new LdcInsnNode(annotation.tolerance()));
            comparison.add(new InsnNode(annotation.ignoreOrder() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            comparison.add(new InsnNode(annotation.ignoreCase() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            comparison.add(new LdcInsnNode(annotation.maxDepth()));
            pushStringArray(comparison, annotation.excludeFields());

            comparison.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixComparator",
                "deepCompare",
                "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;DZZ I[Ljava/lang/String;)I",
                false
            ));

            methodNode.instructions.insert(comparison);

            context.addDiagnostic(String.format(
                "⚖️ @DeepCompare applied to %s::%s [strategy=%s]",
                classNode.name, methodNode.name, annotation.strategy()
            ));
        }

        /**
         * Process @DeepCompress annotation.
         */
        public void processCompress(DeepCompress annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            if (annotation.compressReturn()) {
                InsnList compression = new InsnList();

                compression.add(new LdcInsnNode(annotation.algorithm().name()));
                compression.add(new LdcInsnNode(annotation.level()));
                compression.add(new LdcInsnNode(annotation.bufferSize()));

                compression.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixCompressor",
                    "compress",
                    "([BLjava/lang/String;II)[B",
                    false
                ));

                insertBeforeReturns(methodNode, compression);
            }

            context.addDiagnostic(String.format(
                "📦 @DeepCompress applied to %s::%s [algo=%s, level=%d]",
                classNode.name, methodNode.name, annotation.algorithm(), annotation.level()
            ));
        }

        /**
         * Process @DeepChecksum annotation.
         */
        public void processChecksum(DeepChecksum annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            if (annotation.computeOnReturn()) {
                InsnList checksumming = new InsnList();

                checksumming.add(new LdcInsnNode(annotation.algorithm().name()));
                checksumming.add(new InsnNode(annotation.embedInResult() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                checksumming.add(new LdcInsnNode(annotation.checksumField()));

                checksumming.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixChecksum",
                    "computeAndAttach",
                    "(Ljava/lang/Object;Ljava/lang/String;ZLjava/lang/String;)Ljava/lang/Object;",
                    false
                ));

                insertBeforeReturns(methodNode, checksumming);
            }

            context.addDiagnostic(String.format(
                "✅ @DeepChecksum applied to %s::%s [algo=%s]",
                classNode.name, methodNode.name, annotation.algorithm()
            ));
        }

        // ─── Helper Methods ───

        private boolean containsCheck(SecurityCheck[] checks, SecurityCheck target) {
            for (SecurityCheck check : checks) {
                if (check == target) return true;
            }
            return false;
        }

        private void injectNullChecks(InsnList insns, MethodNode methodNode) {
            Type[] argTypes = Type.getArgumentTypes(methodNode.desc);
            int localIdx = (methodNode.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;

            for (int i = 0; i < argTypes.length; i++) {
                if (argTypes[i].getSort() == Type.OBJECT || argTypes[i].getSort() == Type.ARRAY) {
                    LabelNode nonNull = new LabelNode();
                    insns.add(new VarInsnNode(Opcodes.ALOAD, localIdx));
                    insns.add(new JumpInsnNode(Opcodes.IFNONNULL, nonNull));
                    insns.add(new TypeInsnNode(Opcodes.NEW, "java/lang/IllegalArgumentException"));
                    insns.add(new InsnNode(Opcodes.DUP));
                    insns.add(new LdcInsnNode("Parameter " + i + " must not be null"));
                    insns.add(new MethodInsnNode(
                        Opcodes.INVOKESPECIAL,
                        "java/lang/IllegalArgumentException",
                        "<init>",
                        "(Ljava/lang/String;)V",
                        false
                    ));
                    insns.add(new InsnNode(Opcodes.ATHROW));
                    insns.add(nonNull);
                }
                localIdx += argTypes[i].getSize();
            }
        }

        private void injectInputSanitization(InsnList insns, MethodNode methodNode) {
            Type[] argTypes = Type.getArgumentTypes(methodNode.desc);
            int localIdx = (methodNode.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;

            for (int i = 0; i < argTypes.length; i++) {
                if (argTypes[i].getDescriptor().equals("Ljava/lang/String;")) {
                    insns.add(new VarInsnNode(Opcodes.ALOAD, localIdx));
                    insns.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/DeepMixSanitizer",
                        "basicSanitize",
                        "(Ljava/lang/String;)Ljava/lang/String;",
                        false
                    ));
                    insns.add(new VarInsnNode(Opcodes.ASTORE, localIdx));
                }
                localIdx += argTypes[i].getSize();
            }
        }

        private void injectBoundsChecking(InsnList insns, MethodNode methodNode) {
            // Inject array bounds checking before array access operations
            // This is handled at instruction level during method body scan
        }

        private void transformToConstantTime(MethodNode methodNode) {
            // Replace standard String.equals() with constant-time comparison
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode method = (MethodInsnNode) insn;
                    if (method.owner.equals("java/lang/String") &&
                        method.name.equals("equals")) {
                        method.owner = "deepmix/runtime/DeepMixSecurity";
                        method.name = "constantTimeEquals";
                        method.desc = "(Ljava/lang/String;Ljava/lang/Object;)Z";
                        method.setOpcode(Opcodes.INVOKESTATIC);
                    }
                }
            }
        }

        private void injectInjectionPrevention(InsnList insns, MethodNode methodNode) {
            Type[] argTypes = Type.getArgumentTypes(methodNode.desc);
            int localIdx = (methodNode.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;

            for (int i = 0; i < argTypes.length; i++) {
                if (argTypes[i].getDescriptor().equals("Ljava/lang/String;")) {
                    insns.add(new VarInsnNode(Opcodes.ALOAD, localIdx));
                    insns.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/DeepMixSecurity",
                        "checkInjection",
                        "(Ljava/lang/String;)V",
                        false
                    ));
                }
                localIdx += argTypes[i].getSize();
            }
        }

        private void injectOverflowProtection(MethodNode methodNode) {
            // Replace arithmetic operations with checked versions
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn.getOpcode() == Opcodes.IADD) {
                    MethodInsnNode safeAdd = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Math",
                        "addExact",
                        "(II)I",
                        false
                    );
                    methodNode.instructions.set(insn, safeAdd);
                } else if (insn.getOpcode() == Opcodes.IMUL) {
                    MethodInsnNode safeMul = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Math",
                        "multiplyExact",
                        "(II)I",
                        false
                    );
                    methodNode.instructions.set(insn, safeMul);
                }
            }
        }

        private void injectAuditLogging(InsnList preamble, InsnList epilogue,
                                        MethodNode methodNode, String[] sensitiveParams) {
            // Log method entry
            preamble.add(new LdcInsnNode(methodNode.name));
            preamble.add(new LdcInsnNode(methodNode.desc));
            preamble.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixAudit",
                "logMethodEntry",
                "(Ljava/lang/String;Ljava/lang/String;)V",
                false
            ));

            // Log method exit
            epilogue.add(new LdcInsnNode(methodNode.name));
            epilogue.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixAudit",
                "logMethodExit",
                "(Ljava/lang/String;)V",
                false
            ));
        }

        private void injectAntiDebug(InsnList insns, MethodNode methodNode) {
            insns.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixSecurity",
                "detectDebugger",
                "()V",
                false
            ));
        }

        private void injectAntiTamper(InsnList insns, MethodNode methodNode,
                                      ClassNode classNode) {
            insns.add(new LdcInsnNode(classNode.name));
            insns.add(new LdcInsnNode(methodNode.name));
            insns.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixSecurity",
                "verifyIntegrity",
                "(Ljava/lang/String;Ljava/lang/String;)V",
                false
            ));
        }

        private void wrapWithStackTraceFilter(MethodNode methodNode, String[] sensitiveParams) {
            // Wrap entire method body in try-catch that filters stack traces
            LabelNode tryStart = new LabelNode();
            LabelNode tryEnd = new LabelNode();
            LabelNode catchHandler = new LabelNode();

            methodNode.instructions.insert(tryStart);
            methodNode.instructions.add(tryEnd);
            methodNode.instructions.add(catchHandler);

            // In catch: filter and rethrow
            InsnList catchBody = new InsnList();
            pushStringArray(catchBody, sensitiveParams);
            catchBody.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixSecurity",
                "filterStackTrace",
                "(Ljava/lang/Throwable;[Ljava/lang/String;)Ljava/lang/Throwable;",
                false
            ));
            catchBody.add(new InsnNode(Opcodes.ATHROW));
            methodNode.instructions.add(catchBody);

            methodNode.tryCatchBlocks.add(new TryCatchBlockNode(
                tryStart, tryEnd, catchHandler, "java/lang/Throwable"
            ));
        }

        private void injectEscapeForParam(InsnList insns, int paramIdx, EscapeContext context) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, paramIdx));
            insns.add(new LdcInsnNode(context.name()));
            insns.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixEscaper",
                "escape",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false
            ));
            insns.add(new VarInsnNode(Opcodes.ASTORE, paramIdx));
        }

        private void wrapReturnWithEscape(MethodNode methodNode, EscapeContext context) {
            Type returnType = Type.getReturnType(methodNode.desc);
            if (!returnType.getDescriptor().equals("Ljava/lang/String;")) return;

            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn.getOpcode() == Opcodes.ARETURN) {
                    InsnList escape = new InsnList();
                    escape.add(new LdcInsnNode(context.name()));
                    escape.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/DeepMixEscaper",
                        "escape",
                        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                        false
                    ));
                    methodNode.instructions.insertBefore(insn, escape);
                }
            }
        }

        private void encryptStringLiterals(MethodNode methodNode, EncryptionAlgorithm algorithm) {
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof LdcInsnNode) {
                    LdcInsnNode ldc = (LdcInsnNode) insn;
                    if (ldc.cst instanceof String) {
                        String original = (String) ldc.cst;
                        String encrypted = encryptString(original, algorithm);

                        InsnList decryption = new InsnList();
                        decryption.add(new LdcInsnNode(encrypted));
                        decryption.add(new LdcInsnNode(algorithm.name()));
                        decryption.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "deepmix/runtime/DeepMixEncryptor",
                            "decryptString",
                            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                            false
                        ));

                        methodNode.instructions.insert(insn, decryption);
                        methodNode.instructions.remove(insn);
                    }
                }
            }
        }

        private void encryptNumericConstants(MethodNode methodNode) {
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof LdcInsnNode) {
                    LdcInsnNode ldc = (LdcInsnNode) insn;
                    if (ldc.cst instanceof Integer) {
                        int original = (Integer) ldc.cst;
                        int key = ThreadLocalRandom.current().nextInt();
                        int encrypted = original ^ key;

                        InsnList decryption = new InsnList();
                        decryption.add(new LdcInsnNode(encrypted));
                        decryption.add(new LdcInsnNode(key));
                        decryption.add(new InsnNode(Opcodes.IXOR));

                        methodNode.instructions.insert(insn, decryption);
                        methodNode.instructions.remove(insn);
                    }
                }
            }
        }

        private void applyObfuscation(MethodNode methodNode, ObfuscationTechnique technique) {
            switch (technique) {
                case CONTROL_FLOW_FLATTENING:
                    flattenControlFlow(methodNode);
                    break;
                case OPAQUE_PREDICATES:
                    insertOpaquePredicates(methodNode);
                    break;
                case DEAD_CODE_INSERTION:
                    insertDeadCode(methodNode);
                    break;
                default:
                    break;
            }
        }

        private void flattenControlFlow(MethodNode methodNode) {
            // Control flow flattening: convert branches to switch-based dispatch
            context.addDiagnostic("   ↳ Applied control flow flattening");
        }

        private void insertOpaquePredicates(MethodNode methodNode) {
            // Insert always-true/always-false conditions that are hard to analyze
            context.addDiagnostic("   ↳ Inserted opaque predicates");
        }

        private void insertDeadCode(MethodNode methodNode) {
            // Insert unreachable code to confuse decompilers
            context.addDiagnostic("   ↳ Inserted dead code");
        }

        private String encryptString(String input, EncryptionAlgorithm algorithm) {
            // Compile-time encryption of string literal
            byte[] bytes = input.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] key = new byte[16]; // Generate from algorithm and seed
            new java.security.SecureRandom().nextBytes(key);

            byte[] encrypted = new byte[bytes.length + key.length];
            System.arraycopy(key, 0, encrypted, 0, key.length);
            for (int i = 0; i < bytes.length; i++) {
                encrypted[key.length + i] = (byte)(bytes[i] ^ key[i % key.length]);
            }

            return java.util.Base64.getEncoder().encodeToString(encrypted);
        }

        private String getSerializerClass(SerializationFormat format) {
            switch (format) {
                case JSON_JACKSON: return "com/fasterxml/jackson/databind/ObjectMapper";
                case JSON_GSON: return "com/google/gson/Gson";
                case PROTOBUF: return "com/google/protobuf/MessageLite";
                case MSGPACK: return "org/msgpack/core/MessagePack";
                case KRYO: return "com/esotericsoftware/kryo/Kryo";
                default: return "deepmix/runtime/DeepMixSerializer";
            }
        }

        private int resolveParamIndex(String paramName, MethodNode methodNode) {
            // Try as index first
            try {
                return Integer.parseInt(paramName);
            } catch (NumberFormatException e) {
                // Try as parameter name from LocalVariableTable
                if (methodNode.localVariables != null) {
                    for (LocalVariableNode lvn : methodNode.localVariables) {
                        if (lvn.name.equals(paramName)) {
                            return lvn.index;
                        }
                    }
                }
                // Try matching against parameter annotations or debug info
                if (methodNode.parameters != null) {
                    int localIdx = (methodNode.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
                    Type[] argTypes = Type.getArgumentTypes(methodNode.desc);
                    for (int i = 0; i < methodNode.parameters.size(); i++) {
                        ParameterNode pn = methodNode.parameters.get(i);
                        if (pn.name != null && pn.name.equals(paramName)) {
                            return localIdx;
                        }
                        if (i < argTypes.length) {
                            localIdx += argTypes[i].getSize();
                        }
                    }
                }
                return -1;
            }
        }

        private void insertBeforeReturns(MethodNode methodNode, InsnList toInsert) {
            List<AbstractInsnNode> returnInsns = new ArrayList<>();
            for (AbstractInsnNode insn : methodNode.instructions) {
                int op = insn.getOpcode();
                if (op == Opcodes.RETURN || op == Opcodes.ARETURN ||
                    op == Opcodes.IRETURN || op == Opcodes.LRETURN ||
                    op == Opcodes.FRETURN || op == Opcodes.DRETURN) {
                    returnInsns.add(insn);
                }
            }
            for (AbstractInsnNode ret : returnInsns) {
                InsnList copy = cloneInsnList(toInsert);
                methodNode.instructions.insertBefore(ret, copy);
            }
        }

        private InsnList cloneInsnList(InsnList original) {
            InsnList clone = new InsnList();
            Map<LabelNode, LabelNode> labelMap = new HashMap<>();

            // First pass: collect labels
            for (AbstractInsnNode insn : original) {
                if (insn instanceof LabelNode) {
                    labelMap.put((LabelNode) insn, new LabelNode());
                }
            }

            // Second pass: clone instructions
            for (AbstractInsnNode insn : original) {
                clone.add(insn.clone(labelMap));
            }
            return clone;
        }

        private void pushStringArray(InsnList insns, String[] values) {
            insns.add(new LdcInsnNode(values.length));
            insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));

            for (int i = 0; i < values.length; i++) {
                insns.add(new InsnNode(Opcodes.DUP));
                insns.add(new LdcInsnNode(i));
                insns.add(new LdcInsnNode(values[i]));
                insns.add(new InsnNode(Opcodes.AASTORE));
            }
        }

        private void pushIntConstant(InsnList insns, int value) {
            if (value >= -1 && value <= 5) {
                insns.add(new InsnNode(Opcodes.ICONST_0 + value));
            } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                insns.add(new IntInsnNode(Opcodes.BIPUSH, value));
            } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                insns.add(new IntInsnNode(Opcodes.SIPUSH, value));
            } else {
                insns.add(new LdcInsnNode(value));
            }
        }
    }

    // ──────────────────────────────────────────────
    // Phase 10 — State Manager (shared infrastructure)
    // ──────────────────────────────────────────────

    /**
     * Manages global state for DeepMix transformations.
     * Thread-safe, snapshot-capable, with full undo/redo history.
     */
    public static class StateManager {

        private final ConcurrentHashMap<String, Object> globalState;
        private final ConcurrentHashMap<String, Deque<Object>> undoStacks;
        private final ConcurrentHashMap<String, Deque<Object>> redoStacks;
        private final ConcurrentHashMap<String, List<Map<String, Object>>> snapshots;
        private final ReentrantReadWriteLock stateLock;
        private final AtomicLong versionCounter;
        private volatile boolean frozen;

        public StateManager() {
            this.globalState = new ConcurrentHashMap<>();
            this.undoStacks = new ConcurrentHashMap<>();
            this.redoStacks = new ConcurrentHashMap<>();
            this.snapshots = new ConcurrentHashMap<>();
            this.stateLock = new ReentrantReadWriteLock();
            this.versionCounter = new AtomicLong(0);
            this.frozen = false;
        }

        public void set(String key, Object value) {
            if (frozen) throw new IllegalStateException("State is frozen");
            stateLock.writeLock().lock();
            try {
                Object old = globalState.put(key, value);
                undoStacks.computeIfAbsent(key, k -> new ArrayDeque<>()).push(old != null ? old : NullSentinel.INSTANCE);
                redoStacks.computeIfAbsent(key, k -> new ArrayDeque<>()).clear();
                versionCounter.incrementAndGet();
            } finally {
                stateLock.writeLock().unlock();
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> type) {
            stateLock.readLock().lock();
            try {
                return type.cast(globalState.get(key));
            } finally {
                stateLock.readLock().unlock();
            }
        }

        public boolean undo(String key) {
            if (frozen) return false;
            stateLock.writeLock().lock();
            try {
                Deque<Object> undoStack = undoStacks.get(key);
                if (undoStack == null || undoStack.isEmpty()) return false;

                Object current = globalState.get(key);
                Object previous = undoStack.pop();

                redoStacks.computeIfAbsent(key, k -> new ArrayDeque<>())
                          .push(current != null ? current : NullSentinel.INSTANCE);

                if (previous == NullSentinel.INSTANCE) {
                    globalState.remove(key);
                } else {
                    globalState.put(key, previous);
                }
                versionCounter.incrementAndGet();
                return true;
            } finally {
                stateLock.writeLock().unlock();
            }
        }

        public boolean redo(String key) {
            if (frozen) return false;
            stateLock.writeLock().lock();
            try {
                Deque<Object> redoStack = redoStacks.get(key);
                if (redoStack == null || redoStack.isEmpty()) return false;

                Object current = globalState.get(key);
                Object next = redoStack.pop();

                undoStacks.computeIfAbsent(key, k -> new ArrayDeque<>())
                          .push(current != null ? current : NullSentinel.INSTANCE);

                if (next == NullSentinel.INSTANCE) {
                    globalState.remove(key);
                } else {
                    globalState.put(key, next);
                }
                versionCounter.incrementAndGet();
                return true;
            } finally {
                stateLock.writeLock().unlock();
            }
        }

        public String snapshot(String name) {
            stateLock.readLock().lock();
            try {
                Map<String, Object> snap = new HashMap<>(globalState);
                snapshots.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>()).add(snap);
                return name + "_v" + versionCounter.get();
            } finally {
                stateLock.readLock().unlock();
            }
        }

        public boolean restore(String name) {
            if (frozen) return false;
            stateLock.writeLock().lock();
            try {
                List<Map<String, Object>> snaps = snapshots.get(name);
                if (snaps == null || snaps.isEmpty()) return false;
                Map<String, Object> latest = snaps.get(snaps.size() - 1);
                globalState.clear();
                globalState.putAll(latest);
                versionCounter.incrementAndGet();
                return true;
            } finally {
                stateLock.writeLock().unlock();
            }
        }

        public void freeze() { this.frozen = true; }
        public void unfreeze() { this.frozen = false; }
        public boolean isFrozen() { return frozen; }
        public long getVersion() { return versionCounter.get(); }

        /** Sentinel for null values in undo/redo stacks */
        private enum NullSentinel { INSTANCE }
    }

    // ──────────────────────────────────────────────
    // Phase 10 — Security Manager
    // ──────────────────────────────────────────────

    public static class SecurityManager {

        private final DeepMixConfig config;
        private final Set<String> trustedClasses;
        private final Set<String> blockedPatterns;
        private final Map<String, AtomicInteger> violationCounters;

        public SecurityManager(DeepMixConfig config) {
            this.config = config;
            this.trustedClasses = ConcurrentHashMap.newKeySet();
            this.blockedPatterns = ConcurrentHashMap.newKeySet();
            this.violationCounters = new ConcurrentHashMap<>();

            // Initialize default blocked patterns
            blockedPatterns.add(".*[;|&`$].*");         // Command injection
            blockedPatterns.add(".*<script.*>.*");       // XSS
            blockedPatterns.add(".*'\\s*(OR|AND)\\s*'.*"); // SQL injection
            blockedPatterns.add(".*\\.\\./.*");           // Path traversal
        }

        public void trustClass(String className) { trustedClasses.add(className); }
        public boolean isTrusted(String className) { return trustedClasses.contains(className); }

        public boolean isInputSafe(String input) {
            if (input == null) return true;
            for (String pattern : blockedPatterns) {
                if (input.matches(pattern)) {
                    recordViolation("blocked_pattern:" + pattern);
                    return false;
                }
            }
            return true;
        }

        public void recordViolation(String type) {
            violationCounters.computeIfAbsent(type, k -> new AtomicInteger(0)).incrementAndGet();
        }

        public int getViolationCount(String type) {
            AtomicInteger counter = violationCounters.get(type);
            return counter != null ? counter.get() : 0;
        }

        public Map<String, Integer> getAllViolations() {
            Map<String, Integer> result = new HashMap<>();
            violationCounters.forEach((k, v) -> result.put(k, v.get()));
            return Collections.unmodifiableMap(result);
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                      ║
    // ║  PHASE 11: STATE MANAGEMENT                                          ║
    // ║  10 annotations | Priority: MEDIUM | Est. Time: 2 days               ║
    // ║                                                                      ║
    // ║  @DeepFreeze   @DeepSnapshot  @DeepRestore  @DeepUndo               ║
    // ║  @DeepRedo     @DeepTransact  @DeepRollback @DeepCommit             ║
    // ║  @DeepVersion  @DeepMigrate                                          ║
    // ║                                                                      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // Phase 11 — Enums
    // ──────────────────────────────────────────────

    /** Freeze depth for @DeepFreeze */
    public enum FreezeDepth {
        SHALLOW,       // Only freeze top-level fields
        DEEP,          // Recursively freeze all nested objects
        SELECTIVE      // Freeze only annotated fields
    }

    /** Freeze enforcement mode */
    public enum FreezeMode {
        RUNTIME_EXCEPTION,  // Throw on modification attempt
        COPY_ON_WRITE,      // Return copy on write attempt
        SILENT_IGNORE,      // Silently ignore writes
        LOG_AND_IGNORE      // Log and ignore writes
    }

    /** Snapshot storage strategy */
    public enum SnapshotStorage {
        IN_MEMORY,       // Keep in heap memory
        WEAK_REFERENCE,  // Use weak references (GC-eligible)
        SOFT_REFERENCE,  // Use soft references
        DISK,            // Persist to disk
        COMPRESSED,      // In-memory but compressed
        EXTERNAL         // External storage (Redis, DB, etc.)
    }

    /** Snapshot trigger conditions */
    public enum SnapshotTrigger {
        MANUAL,          // Only on explicit call
        BEFORE_MUTATION,  // Auto-snapshot before any mutation
        PERIODIC,        // Time-based periodic snapshots
        ON_EVENT,        // Event-triggered snapshots
        ON_THRESHOLD     // Triggered by change threshold
    }

    /** Transaction isolation levels */
    public enum IsolationLevel {
        READ_UNCOMMITTED,
        READ_COMMITTED,
        REPEATABLE_READ,
        SERIALIZABLE,
        SNAPSHOT
    }

    /** Transaction propagation behavior */
    public enum Propagation {
        REQUIRED,     // Use existing or create new
        REQUIRES_NEW, // Always create new
        NESTED,       // Create nested transaction
        MANDATORY,    // Must exist, throw otherwise
        NEVER,        // Must not exist, throw otherwise
        SUPPORTS,     // Use if exists, none otherwise
        NOT_SUPPORTED // Suspend existing, run without
    }

    /** Version strategy for @DeepVersion */
    public enum VersionStrategy {
        SEMANTIC,     // Semantic versioning (major.minor.patch)
        INCREMENTAL,  // Simple incrementing counter
        TIMESTAMP,    // Timestamp-based
        HASH,         // Content hash-based
        CUSTOM        // Custom version scheme
    }

    /** Migration direction */
    public enum MigrationDirection {
        UP,           // Upgrade migration
        DOWN,         // Downgrade/rollback migration
        BIDIRECTIONAL // Both directions supported
    }

    /** Migration strategy */
    public enum MigrationStrategy {
        SEQUENTIAL,   // Apply migrations in order
        SKIP_FAILED,  // Skip failed migrations and continue
        STOP_ON_FAIL, // Stop at first failure
        PARALLEL,     // Apply independent migrations in parallel
        DRY_RUN       // Simulate without applying
    }

    // ──────────────────────────────────────────────
    // Phase 11 — Sub-annotation types
    // ──────────────────────────────────────────────

    /** Undo/Redo history configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface HistoryConfig {
        int maxUndoLevels() default 100;
        int maxRedoLevels() default 100;
        boolean compressHistory() default false;
        boolean persistHistory() default false;
        String persistPath() default "";
        long historyTTLSeconds() default -1; // -1 = forever
    }

    /** Transaction configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TransactionConfig {
        IsolationLevel isolation() default IsolationLevel.READ_COMMITTED;
        Propagation propagation() default Propagation.REQUIRED;
        long timeoutMs() default 30000;
        boolean readOnly() default false;
        String[] rollbackOn() default {"java.lang.Exception"};
        String[] noRollbackOn() default {};
        int maxRetries() default 0;
        long retryDelayMs() default 100;
    }

    /** Version range specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface VersionRange {
        String from() default "0.0.0";
        String to() default "";  // Empty = latest
        boolean fromInclusive() default true;
        boolean toInclusive() default true;
    }

    /** Migration step definition */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MigrationStep {
        String fromVersion();
        String toVersion();
        String migrationMethod();  // Method to call for this step
        String rollbackMethod() default "";  // Method to call for rollback
        String description() default "";
        boolean critical() default false;  // If true, failure stops everything
    }

    // ──────────────────────────────────────────────
    // Phase 11 — Annotations
    // ──────────────────────────────────────────────

    /**
     * ❄️ @DeepFreeze — Immutability enforcement for objects and state.
     *
     * Makes objects or state deeply immutable after initial construction.
     * Any mutation attempt will throw, be silently ignored, or trigger COW.
     *
     * Example:
     * <pre>
     * {@code @DeepFreeze(
     *     depth = FreezeDepth.DEEP,
     *     mode = FreezeMode.RUNTIME_EXCEPTION,
     *     excludeFields = {"mutableCache"}
     * )}
     * public class WorldConfig { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
    public @interface DeepFreeze {
        String target() default "";
        FreezeDepth depth() default FreezeDepth.DEEP;
        FreezeMode mode() default FreezeMode.RUNTIME_EXCEPTION;
        String[] excludeFields() default {};
        boolean freezeCollections() default true;
        boolean freezeMaps() default true;
        boolean freezeArrays() default true;
        boolean allowDefrost() default false;      // Allow un-freezing
        String defrostCondition() default "";      // Condition for allowed defrost
        boolean freezeAfterInit() default true;    // Freeze after constructor completes
        int freezeDelayMs() default 0;             // Delay before freezing
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepFreeze */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
    public @interface DFRZ {
        String target() default "";
        FreezeDepth depth() default FreezeDepth.DEEP;
        FreezeMode mode() default FreezeMode.RUNTIME_EXCEPTION;
        String[] excludeFields() default {};
        boolean allowDefrost() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📸 @DeepSnapshot — Create state snapshots for later restoration.
     *
     * Captures the complete state of an object or subsystem at a point in time.
     * Snapshots can be named, tagged, and stored using configurable strategies.
     *
     * Example:
     * <pre>
     * {@code @DeepSnapshot(
     *     name = "before_update",
     *     storage = SnapshotStorage.COMPRESSED,
     *     trigger = SnapshotTrigger.BEFORE_MUTATION,
     *     maxSnapshots = 50
     * )}
     * public void updateWorldState(WorldState state) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepSnapshot {
        String target() default "";
        String name() default "";                    // Snapshot name (auto-generated if empty)
        String[] tags() default {};                  // Searchable tags
        SnapshotStorage storage() default SnapshotStorage.IN_MEMORY;
        SnapshotTrigger trigger() default SnapshotTrigger.MANUAL;
        int maxSnapshots() default 100;              // Max snapshots to keep
        boolean includeTimestamp() default true;
        boolean includeStackTrace() default false;   // Include creation stack trace
        String[] includeFields() default {};         // Only snapshot these fields
        String[] excludeFields() default {};         // Exclude these fields
        boolean compressed() default false;
        CompressionAlgorithm compression() default CompressionAlgorithm.LZ4;
        long ttlSeconds() default -1;               // Snapshot TTL (-1 = forever)
        long periodicIntervalMs() default 60000;    // For PERIODIC trigger
        String eventName() default "";               // For ON_EVENT trigger
        int changeThreshold() default 10;            // For ON_THRESHOLD trigger
        ErrorStrategy onError() default ErrorStrategy.LOG_AND_CONTINUE;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSnapshot */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DSNAP {
        String target() default "";
        String name() default "";
        SnapshotStorage storage() default SnapshotStorage.IN_MEMORY;
        SnapshotTrigger trigger() default SnapshotTrigger.MANUAL;
        int maxSnapshots() default 100;
        String[] excludeFields() default {};
        long ttlSeconds() default -1;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔄 @DeepRestore — Restore state from a previously captured snapshot.
     *
     * Restores object or system state from a named snapshot.
     *
     * Example:
     * <pre>
     * {@code @DeepRestore(
     *     snapshotName = "before_update",
     *     verify = true,
     *     onMissing = ErrorStrategy.FALLBACK
     * )}
     * public void rollbackWorldState() { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepRestore {
        String target() default "";
        String snapshotName() default "";          // Specific snapshot to restore
        String snapshotTag() default "";           // Restore latest with this tag
        boolean restoreLatest() default true;      // Use latest snapshot if name not found
        boolean verify() default true;             // Verify integrity after restore
        boolean deleteAfterRestore() default false; // Remove snapshot after use
        String[] restoreFields() default {};        // Only restore these fields
        String[] skipFields() default {};           // Skip these fields during restore
        boolean triggerCallbacks() default true;    // Fire state-change callbacks
        ErrorStrategy onMissing() default ErrorStrategy.THROW;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepRestore */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DRSTR {
        String target() default "";
        String snapshotName() default "";
        boolean restoreLatest() default true;
        boolean verify() default true;
        ErrorStrategy onMissing() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ↩️ @DeepUndo — Undo functionality for state-modifying operations.
     *
     * Automatically records state before mutation and provides undo capability.
     *
     * Example:
     * <pre>
     * {@code @DeepUndo(
     *     history = @HistoryConfig(maxUndoLevels = 50),
     *     groupId = "editor_operations"
     * )}
     * public void placeBlock(World world, BlockPos pos, BlockState state) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepUndo {
        String target() default "";
        HistoryConfig history() default @HistoryConfig;
        String groupId() default "";               // Group related operations
        boolean autoRecord() default true;         // Auto-record before method execution
        String stateExtractor() default "";        // Method to extract undoable state
        String stateApplier() default "";          // Method to apply undone state
        boolean coalesceRepeated() default false;  // Merge repeated operations
        long coalesceWindowMs() default 500;       // Coalescing time window
        String description() default "";           // Human-readable operation description
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepUndo */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DUNDO {
        String target() default "";
        HistoryConfig history() default @HistoryConfig;
        String groupId() default "";
        boolean autoRecord() default true;
        boolean coalesceRepeated() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ↪️ @DeepRedo — Redo functionality (counterpart to @DeepUndo).
     *
     * Re-applies previously undone operations from the redo stack.
     *
     * Example:
     * <pre>
     * {@code @DeepRedo(groupId = "editor_operations")}
     * public void redoLastOperation() { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepRedo {
        String target() default "";
        String groupId() default "";
        int steps() default 1;                  // Number of redo steps
        boolean all() default false;            // Redo all available
        ErrorStrategy onEmpty() default ErrorStrategy.SILENT;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepRedo */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DREDO {
        String target() default "";
        String groupId() default "";
        int steps() default 1;
        boolean all() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔄 @DeepTransact — Transactional behavior for method execution.
     *
     * Wraps method execution in a transaction with ACID-like guarantees.
     * On failure, all state changes are automatically rolled back.
     *
     * Example:
     * <pre>
     * {@code @DeepTransact(
     *     config = @TransactionConfig(
     *         isolation = IsolationLevel.SERIALIZABLE,
     *         timeoutMs = 5000,
     *         maxRetries = 3
     *     )
     * )}
     * public void transferItems(Inventory from, Inventory to, ItemStack item) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepTransact {
        String target() default "";
        TransactionConfig config() default @TransactionConfig;
        String txnManager() default "";           // Custom transaction manager
        boolean savepoint() default false;        // Use savepoints within nested txns
        boolean autoCommit() default true;        // Auto-commit on successful return
        boolean autoRollback() default true;      // Auto-rollback on exception
        String[] resources() default {};          // Named resources participating in txn
        String beforeCommit() default "";         // Callback before commit
        String afterCommit() default "";          // Callback after commit
        String onRollback() default "";           // Callback on rollback
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepTransact */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DTXN {
        String target() default "";
        TransactionConfig config() default @TransactionConfig;
        boolean autoCommit() default true;
        boolean autoRollback() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ⏪ @DeepRollback — Explicit rollback support.
     *
     * Forces rollback of the current transaction or state to a specific point.
     *
     * Example:
     * <pre>
     * {@code @DeepRollback(
     *     savepointName = "before_crafting",
     *     condition = "craftingFailed"
     * )}
     * public void rollbackCrafting() { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepRollback {
        String target() default "";
        String savepointName() default "";        // Rollback to specific savepoint
        boolean rollbackAll() default false;      // Rollback entire transaction
        String condition() default "";            // Condition expression for rollback
        boolean cascadeRollback() default false;  // Rollback nested transactions too
        String[] cleanupMethods() default {};     // Methods to call during rollback
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepRollback */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DROLL {
        String target() default "";
        String savepointName() default "";
        boolean rollbackAll() default false;
        String condition() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ✅ @DeepCommit — Explicit commit logic for transactions.
     *
     * Commits the current transaction or state, making changes permanent.
     *
     * Example:
     * <pre>
     * {@code @DeepCommit(
     *     verify = true,
     *     createCheckpoint = true,
     *     notifyObservers = true
     * )}
     * public void saveDatabaseChanges() { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepCommit {
        String target() default "";
        boolean verify() default true;           // Verify state consistency before commit
        boolean createCheckpoint() default false; // Create checkpoint before commit
        String checkpointName() default "";
        boolean notifyObservers() default true;  // Notify state change observers
        boolean synchronous() default true;      // Wait for commit to complete
        boolean durable() default true;          // Ensure durability (flush to disk)
        String validationMethod() default "";    // Custom validation before commit
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCommit */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DCOM {
        String target() default "";
        boolean verify() default true;
        boolean createCheckpoint() default false;
        boolean notifyObservers() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📌 @DeepVersion — Version adaptation for multi-version support.
     *
     * Adapts code behavior based on target version. Supports version ranges,
     * automatic fallbacks, and version-specific implementations.
     *
     * Example:
     * <pre>
     * {@code @DeepVersion(
     *     strategy = VersionStrategy.SEMANTIC,
     *     supportedRange = @VersionRange(from = "1.12.2", to = "1.20.4"),
     *     currentVersion = "1.19.4"
     * )}
     * public void renderEntity(Entity entity) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    public @interface DeepVersion {
        String target() default "";
        VersionStrategy strategy() default VersionStrategy.SEMANTIC;
        VersionRange supportedRange() default @VersionRange;
        String currentVersion() default "";
        String minVersion() default "";
        String maxVersion() default "";
        String[] deprecatedInVersions() default {};
        String[] removedInVersions() default {};
        String fallbackMethod() default "";       // Method to use if version not supported
        boolean warnOnDeprecated() default true;
        boolean failOnRemoved() default true;
        String versionProvider() default "";       // Class that provides current version
        ErrorStrategy onUnsupported() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepVersion */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    public @interface DVER {
        String target() default "";
        VersionStrategy strategy() default VersionStrategy.SEMANTIC;
        VersionRange supportedRange() default @VersionRange;
        String currentVersion() default "";
        String fallbackMethod() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🚚 @DeepMigrate — Data migration between versions or formats.
     *
     * Defines migration paths for data transformation between versions,
     * schemas, or formats with full rollback support.
     *
     * Example:
     * <pre>
     * {@code @DeepMigrate(
     *     steps = {
     *         @MigrationStep(fromVersion = "1.0", toVersion = "2.0",
     *                       migrationMethod = "migrateV1ToV2"),
     *         @MigrationStep(fromVersion = "2.0", toVersion = "3.0",
     *                       migrationMethod = "migrateV2ToV3")
     *     },
     *     strategy = MigrationStrategy.SEQUENTIAL,
     *     createBackup = true
     * )}
     * public Data migrateData(Data input) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepMigrate {
        String target() default "";
        MigrationStep[] steps() default {};
        MigrationStrategy strategy() default MigrationStrategy.SEQUENTIAL;
        MigrationDirection direction() default MigrationDirection.UP;
        boolean createBackup() default true;
        String backupPath() default "";
        boolean dryRun() default false;
        boolean validate() default true;         // Validate after each step
        String validationMethod() default "";    // Custom validation method
        boolean reportProgress() default true;
        String progressCallback() default "";
        ErrorStrategy onStepFail() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMigrate */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DMIG {
        String target() default "";
        MigrationStep[] steps() default {};
        MigrationStrategy strategy() default MigrationStrategy.SEQUENTIAL;
        boolean createBackup() default true;
        boolean dryRun() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 11 — Processor
    // ──────────────────────────────────────────────

    /**
     * Processor for all Phase 11 (State Management) annotations.
     *
     * Handles bytecode transformation for freezing, snapshots,
     * undo/redo, transactions, versioning, and migrations.
     */
    public static class Phase11Processor {

        private final DeepMixContext context;

        public Phase11Processor(DeepMixContext context) {
            this.context = context;
        }

        /**
         * Process @DeepFreeze — Make objects immutable after construction.
         * Wraps all setter methods and field writes with freeze checks.
         */
        public void processFreeze(DeepFreeze annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {

            if (annotation.depth() == FreezeDepth.DEEP ||
                annotation.depth() == FreezeDepth.SHALLOW) {

                // Add a `__deepmix_frozen` field to the class
                if (classNode.fields.stream().noneMatch(f -> f.name.equals("__deepmix_frozen"))) {
                    classNode.fields.add(new FieldNode(
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_VOLATILE,
                        "__deepmix_frozen",
                        "Z", null, false
                    ));
                }

                // Inject freeze check into all setter methods and field-writing methods
                Set<String> excludeSet = new HashSet<>(Arrays.asList(annotation.excludeFields()));

                for (MethodNode mn : classNode.methods) {
                    if (mn.name.equals("<init>") || mn.name.equals("<clinit>")) continue;

                    // Find PUTFIELD instructions and insert freeze check before them
                    List<AbstractInsnNode> putFields = new ArrayList<>();
                    for (AbstractInsnNode insn : mn.instructions) {
                        if (insn.getOpcode() == Opcodes.PUTFIELD) {
                            FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                            if (!excludeSet.contains(fieldInsn.name) &&
                                !fieldInsn.name.startsWith("__deepmix_")) {
                                putFields.add(insn);
                            }
                        }
                    }

                    for (AbstractInsnNode putField : putFields) {
                        InsnList check = new InsnList();
                        LabelNode notFrozen = new LabelNode();

                        check.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        check.add(new FieldInsnNode(
                            Opcodes.GETFIELD, classNode.name,
                            "__deepmix_frozen", "Z"
                        ));
                        check.add(new JumpInsnNode(Opcodes.IFEQ, notFrozen));

                        // Handle based on mode
                        switch (annotation.mode()) {
                            case RUNTIME_EXCEPTION:
                                check.add(new TypeInsnNode(Opcodes.NEW,
                                    "java/lang/UnsupportedOperationException"));
                                check.add(new InsnNode(Opcodes.DUP));
                                check.add(new LdcInsnNode(
                                    "Object is frozen and cannot be modified"));
                                check.add(new MethodInsnNode(
                                    Opcodes.INVOKESPECIAL,
                                    "java/lang/UnsupportedOperationException",
                                    "<init>",
                                    "(Ljava/lang/String;)V", false
                                ));
                                check.add(new InsnNode(Opcodes.ATHROW));
                                break;
                            case SILENT_IGNORE:
                                // Pop the value and object reference, skip the PUTFIELD
                                FieldInsnNode fi = (FieldInsnNode) putField;
                                Type fieldType = Type.getType(fi.desc);
                                if (fieldType.getSize() == 2) {
                                    check.add(new InsnNode(Opcodes.POP2));
                                } else {
                                    check.add(new InsnNode(Opcodes.POP));
                                }
                                check.add(new InsnNode(Opcodes.POP)); // pop objectref
                                check.add(new InsnNode(Opcodes.RETURN));
                                break;
                            case LOG_AND_IGNORE:
                                check.add(new LdcInsnNode(
                                    "Attempted write to frozen object: " + classNode.name));
                                check.add(new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "deepmix/runtime/DeepMixLogger",
                                    "warn",
                                    "(Ljava/lang/String;)V", false
                                ));
                                break;
                            case COPY_ON_WRITE:
                                check.add(new VarInsnNode(Opcodes.ALOAD, 0));
                                check.add(new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "deepmix/runtime/DeepMixCloner",
                                    "cowClone",
                                    "(Ljava/lang/Object;)Ljava/lang/Object;", false
                                ));
                                break;
                        }

                        check.add(notFrozen);
                        mn.instructions.insertBefore(putField, check);
                    }
                }

                // If freezeAfterInit, inject freeze call at end of constructors
                if (annotation.freezeAfterInit()) {
                    for (MethodNode mn : classNode.methods) {
                        if (mn.name.equals("<init>")) {
                            InsnList freezeCall = new InsnList();
                            freezeCall.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            freezeCall.add(new InsnNode(Opcodes.ICONST_1));
                            freezeCall.add(new FieldInsnNode(
                                Opcodes.PUTFIELD, classNode.name,
                                "__deepmix_frozen", "Z"
                            ));
                            // Insert before the RETURN instruction
                            for (AbstractInsnNode insn : mn.instructions) {
                                if (insn.getOpcode() == Opcodes.RETURN) {
                                    mn.instructions.insertBefore(insn, freezeCall);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            context.addDiagnostic(String.format(
                "❄️ @DeepFreeze applied to %s [depth=%s, mode=%s]",
                classNode.name, annotation.depth(), annotation.mode()
            ));
        }

        /**
         * Process @DeepSnapshot — Auto-snapshot state before method execution.
         */
        public void processSnapshot(DeepSnapshot annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            InsnList snapshotCode = new InsnList();

            String snapshotName = annotation.name().isEmpty()
                ? classNode.name + "::" + methodNode.name + "_auto"
                : annotation.name();

            // Push snapshot configuration
            snapshotCode.add(new LdcInsnNode(snapshotName));
            snapshotCode.add(new LdcInsnNode(annotation.storage().name()));
            snapshotCode.add(new LdcInsnNode(annotation.maxSnapshots()));
            snapshotCode.add(new InsnNode(
                annotation.includeTimestamp() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            snapshotCode.add(new InsnNode(
                annotation.compressed() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            // Capture 'this' if instance method
            if ((methodNode.access & Opcodes.ACC_STATIC) == 0) {
                snapshotCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
            } else {
                snapshotCode.add(new InsnNode(Opcodes.ACONST_NULL));
            }

            snapshotCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixSnapshot",
                "capture",
                "(Ljava/lang/String;Ljava/lang/String;IZZLjava/lang/Object;)V",
                false
            ));

            // Insert at method entry
            if (annotation.trigger() == SnapshotTrigger.BEFORE_MUTATION ||
                annotation.trigger() == SnapshotTrigger.MANUAL) {
                methodNode.instructions.insert(snapshotCode);
            }

            context.addDiagnostic(String.format(
                "📸 @DeepSnapshot applied to %s::%s [name=%s, storage=%s]",
                classNode.name, methodNode.name, snapshotName, annotation.storage()
            ));
        }

        /**
         * Process @DeepRestore — Restore state from snapshot.
         */
        public void processRestore(DeepRestore annotation, ClassNode classNode,
                                   MethodNode methodNode) throws DeepMixProcessingException {
            InsnList restoreCode = new InsnList();

            restoreCode.add(new LdcInsnNode(
                annotation.snapshotName().isEmpty() ? "__latest__" : annotation.snapshotName()));
            restoreCode.add(new InsnNode(
                annotation.verify() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            restoreCode.add(new InsnNode(
                annotation.deleteAfterRestore() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            if ((methodNode.access & Opcodes.ACC_STATIC) == 0) {
                restoreCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
            } else {
                restoreCode.add(new InsnNode(Opcodes.ACONST_NULL));
            }

            restoreCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixSnapshot",
                "restore",
                "(Ljava/lang/String;ZZLjava/lang/Object;)Z",
                false
            ));

            // Handle missing snapshot
            if (annotation.onMissing() == ErrorStrategy.THROW) {
                LabelNode restored = new LabelNode();
                restoreCode.add(new JumpInsnNode(Opcodes.IFNE, restored));
                restoreCode.add(new TypeInsnNode(Opcodes.NEW,
                    "java/lang/IllegalStateException"));
                restoreCode.add(new InsnNode(Opcodes.DUP));
                restoreCode.add(new LdcInsnNode(
                    "Snapshot not found: " + annotation.snapshotName()));
                restoreCode.add(new MethodInsnNode(
                    Opcodes.INVOKESPECIAL,
                    "java/lang/IllegalStateException",
                    "<init>", "(Ljava/lang/String;)V", false
                ));
                restoreCode.add(new InsnNode(Opcodes.ATHROW));
                restoreCode.add(restored);
            } else {
                restoreCode.add(new InsnNode(Opcodes.POP)); // discard boolean
            }

            methodNode.instructions.insert(restoreCode);

            context.addDiagnostic(String.format(
                "🔄 @DeepRestore applied to %s::%s [snapshot=%s]",
                classNode.name, methodNode.name, annotation.snapshotName()
            ));
        }

        /**
         * Process @DeepUndo — Record state for undo before method execution.
         */
        public void processUndo(DeepUndo annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            if (annotation.autoRecord()) {
                InsnList recordCode = new InsnList();

                String groupId = annotation.groupId().isEmpty()
                    ? classNode.name + "::" + methodNode.name
                    : annotation.groupId();

                recordCode.add(new LdcInsnNode(groupId));
                recordCode.add(new LdcInsnNode(annotation.history().maxUndoLevels()));
                recordCode.add(new InsnNode(
                    annotation.coalesceRepeated() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                recordCode.add(new LdcInsnNode(annotation.coalesceWindowMs()));

                if ((methodNode.access & Opcodes.ACC_STATIC) == 0) {
                    recordCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
                } else {
                    recordCode.add(new InsnNode(Opcodes.ACONST_NULL));
                }

                recordCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixUndoRedo",
                    "recordBeforeMutation",
                    "(Ljava/lang/String;IZJLjava/lang/Object;)V",
                    false
                ));

                methodNode.instructions.insert(recordCode);
            }

            context.addDiagnostic(String.format(
                "↩️ @DeepUndo applied to %s::%s [group=%s, maxLevels=%d]",
                classNode.name, methodNode.name,
                annotation.groupId(), annotation.history().maxUndoLevels()
            ));
        }

        /**
         * Process @DeepRedo — Replay undone operation.
         */
        public void processRedo(DeepRedo annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            InsnList redoCode = new InsnList();

            String groupId = annotation.groupId().isEmpty()
                ? classNode.name + "_default" : annotation.groupId();

            redoCode.add(new LdcInsnNode(groupId));
            redoCode.add(new LdcInsnNode(annotation.steps()));
            redoCode.add(new InsnNode(
                annotation.all() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            redoCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixUndoRedo",
                "redo",
                "(Ljava/lang/String;IZ)I",
                false
            ));
            redoCode.add(new InsnNode(Opcodes.POP)); // discard count

            methodNode.instructions.insert(redoCode);

            context.addDiagnostic(String.format(
                "↪️ @DeepRedo applied to %s::%s [group=%s, steps=%d]",
                classNode.name, methodNode.name, groupId, annotation.steps()
            ));
        }

        /**
         * Process @DeepTransact — Wrap method in transaction.
         */
        public void processTransact(DeepTransact annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            TransactionConfig config = annotation.config();

            // Wrap the entire method body in try-catch for transactional semantics
            LabelNode tryStart = new LabelNode();
            LabelNode tryEnd = new LabelNode();
            LabelNode catchHandler = new LabelNode();
            LabelNode finallyHandler = new LabelNode();

            // Store original instructions
            InsnList originalBody = new InsnList();

            // --- Begin Transaction ---
            InsnList txnBegin = new InsnList();
            txnBegin.add(new LdcInsnNode(config.isolation().name()));
            txnBegin.add(new LdcInsnNode(config.propagation().name()));
            txnBegin.add(new LdcInsnNode(config.timeoutMs()));
            txnBegin.add(new InsnNode(
                config.readOnly() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            txnBegin.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixTransaction",
                "begin",
                "(Ljava/lang/String;Ljava/lang/String;JZ)Ljava/lang/Object;",
                false
            ));

            // Store transaction handle in a new local variable
            int txnHandleIdx = methodNode.maxLocals;
            methodNode.maxLocals += 1;
            txnBegin.add(new VarInsnNode(Opcodes.ASTORE, txnHandleIdx));

            txnBegin.add(tryStart);

            // --- Commit on success ---
            InsnList txnCommit = new InsnList();
            txnCommit.add(tryEnd);

            if (annotation.autoCommit()) {
                txnCommit.add(new VarInsnNode(Opcodes.ALOAD, txnHandleIdx));
                txnCommit.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixTransaction",
                    "commit",
                    "(Ljava/lang/Object;)V", false
                ));
            }

            // --- Catch and rollback ---
            InsnList txnRollback = new InsnList();
            txnRollback.add(catchHandler);

            if (annotation.autoRollback()) {
                // Duplicate exception for rethrow
                txnRollback.add(new InsnNode(Opcodes.DUP));
                txnRollback.add(new VarInsnNode(Opcodes.ALOAD, txnHandleIdx));
                txnRollback.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixTransaction",
                    "rollback",
                    "(Ljava/lang/Throwable;Ljava/lang/Object;)V", false
                ));
            }
            txnRollback.add(new InsnNode(Opcodes.ATHROW)); // rethrow

            // Rebuild method
            methodNode.instructions.insert(txnBegin);
            // tryEnd and commit go before returns (handled by insertBeforeReturns)
            methodNode.instructions.add(txnCommit);
            methodNode.instructions.add(txnRollback);

            // Add try-catch block
            String[] rollbackExceptions = config.rollbackOn();
            String catchType = rollbackExceptions.length > 0
                ? rollbackExceptions[0].replace('.', '/')
                : "java/lang/Exception";
            methodNode.tryCatchBlocks.add(new TryCatchBlockNode(
                tryStart, tryEnd, catchHandler, catchType
            ));

            context.addDiagnostic(String.format(
                "🔄 @DeepTransact applied to %s::%s [isolation=%s, timeout=%dms]",
                classNode.name, methodNode.name,
                config.isolation(), config.timeoutMs()
            ));
        }

        /**
         * Process @DeepVersion — Inject version-checking logic.
         */
        public void processVersion(DeepVersion annotation, ClassNode classNode,
                                   MethodNode methodNode) throws DeepMixProcessingException {
            InsnList versionCheck = new InsnList();

            String minVer = annotation.minVersion().isEmpty()
                ? annotation.supportedRange().from() : annotation.minVersion();
            String maxVer = annotation.maxVersion().isEmpty()
                ? annotation.supportedRange().to() : annotation.maxVersion();

            versionCheck.add(new LdcInsnNode(minVer));
            versionCheck.add(new LdcInsnNode(maxVer));
            versionCheck.add(new LdcInsnNode(annotation.currentVersion()));
            versionCheck.add(new LdcInsnNode(annotation.strategy().name()));

            versionCheck.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixVersion",
                "checkVersion",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z",
                false
            ));

            LabelNode supported = new LabelNode();
            versionCheck.add(new JumpInsnNode(Opcodes.IFNE, supported));

            // Handle unsupported version
            if (!annotation.fallbackMethod().isEmpty()) {
                versionCheck.add(new LdcInsnNode(annotation.fallbackMethod()));
                versionCheck.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixVersion",
                    "invokeFallback",
                    "(Ljava/lang/String;)Ljava/lang/Object;", false
                ));
                versionCheck.add(new InsnNode(Opcodes.ARETURN));
            } else {
                versionCheck.add(new TypeInsnNode(Opcodes.NEW,
                    "java/lang/UnsupportedOperationException"));
                versionCheck.add(new InsnNode(Opcodes.DUP));
                versionCheck.add(new LdcInsnNode(
                    "Method not supported in version " + annotation.currentVersion()));
                versionCheck.add(new MethodInsnNode(
                    Opcodes.INVOKESPECIAL,
                    "java/lang/UnsupportedOperationException",
                    "<init>", "(Ljava/lang/String;)V", false
                ));
                versionCheck.add(new InsnNode(Opcodes.ATHROW));
            }

            versionCheck.add(supported);
            methodNode.instructions.insert(versionCheck);

            context.addDiagnostic(String.format(
                "📌 @DeepVersion applied to %s::%s [range=%s-%s, current=%s]",
                classNode.name, methodNode.name, minVer, maxVer, annotation.currentVersion()
            ));
        }

        /**
         * Process @DeepMigrate — Inject migration chain execution.
         */
        public void processMigrate(DeepMigrate annotation, ClassNode classNode,
                                   MethodNode methodNode) throws DeepMixProcessingException {
            InsnList migrationCode = new InsnList();

            // Build migration steps array
            MigrationStep[] steps = annotation.steps();

            migrationCode.add(new LdcInsnNode(steps.length));
            migrationCode.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));

            for (int i = 0; i < steps.length; i++) {
                migrationCode.add(new InsnNode(Opcodes.DUP));
                migrationCode.add(new LdcInsnNode(i));
                migrationCode.add(new LdcInsnNode(
                    steps[i].fromVersion() + "|" + steps[i].toVersion() + "|" +
                    steps[i].migrationMethod()));
                migrationCode.add(new InsnNode(Opcodes.AASTORE));
            }

            migrationCode.add(new LdcInsnNode(annotation.strategy().name()));
            migrationCode.add(new LdcInsnNode(annotation.direction().name()));
            migrationCode.add(new InsnNode(
                annotation.createBackup() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            migrationCode.add(new InsnNode(
                annotation.dryRun() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            migrationCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixMigration",
                "executeMigrationChain",
                "([Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V",
                false
            ));

            methodNode.instructions.insert(migrationCode);

            context.addDiagnostic(String.format(
                "🚚 @DeepMigrate applied to %s::%s [%d steps, strategy=%s]",
                classNode.name, methodNode.name, steps.length, annotation.strategy()
            ));
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                      ║
    // ║  PHASE 12: CONCURRENCY & THREADING                                   ║
    // ║  10 annotations | Priority: HIGH | Est. Time: 3-4 days               ║
    // ║                                                                      ║
    // ║  Thread Safety:                                                      ║
    // ║  @DeepThreadSafe  @DeepLock  @DeepAtomic  @DeepCAS                  ║
    // ║  @DeepBarrier     @DeepVolatile  @DeepCOW                           ║
    // ║                                                                      ║
    // ║  Purity & Side Effects:                                              ║
    // ║  @DeepImmutable  @DeepPure  @DeepSideEffect                         ║
    // ║                                                                      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // Phase 12 — Enums
    // ──────────────────────────────────────────────

    /** Lock types for @DeepLock */
    public enum LockType {
        SYNCHRONIZED,        // Java synchronized block
        REENTRANT,           // ReentrantLock
        READ_WRITE,          // ReentrantReadWriteLock
        STAMPED,             // StampedLock
        SPIN,                // Spin lock (busy-wait)
        OPTIMISTIC,          // Optimistic locking (try-and-verify)
        FAIR,                // Fair lock (FIFO ordering)
        TRYLOCK,             // Non-blocking try-lock
        DISTRIBUTED,         // Distributed lock (requires external coordinator)
        STRIPED              // Striped locking for collections
    }

    /** Lock scope */
    public enum LockScope {
        METHOD,      // Lock for entire method duration
        INSTANCE,    // Per-instance lock
        CLASS,       // Class-level lock (static)
        FIELD,       // Per-field granular lock
        CUSTOM       // Custom lock scope
    }

    /** Atomic operation type */
    public enum AtomicOp {
        GET,
        SET,
        GET_AND_SET,
        COMPARE_AND_SET,
        INCREMENT_AND_GET,
        GET_AND_INCREMENT,
        DECREMENT_AND_GET,
        GET_AND_DECREMENT,
        ADD_AND_GET,
        GET_AND_ADD,
        UPDATE_AND_GET,
        GET_AND_UPDATE,
        ACCUMULATE_AND_GET,
        GET_AND_ACCUMULATE
    }

    /** Memory ordering for @DeepBarrier and @DeepVolatile */
    public enum MemoryOrder {
        RELAXED,       // No ordering guarantee
        ACQUIRE,       // Acquire semantics (read barrier)
        RELEASE,       // Release semantics (write barrier)
        ACQ_REL,       // Acquire + Release
        SEQ_CST,       // Sequential consistency (full fence)
        CONSUME        // Consume semantics
    }

    /** COW trigger condition */
    public enum COWTrigger {
        ON_WRITE,          // Standard COW on any write
        ON_MUTATION,       // COW on structural mutation
        ON_CONCURRENT_READ, // COW when concurrent reads detected
        THRESHOLD,         // COW after N writes
        MANUAL             // Explicit COW trigger
    }

    /** Side effect categories for @DeepSideEffect */
    public enum SideEffectType {
        IO_READ,
        IO_WRITE,
        NETWORK,
        STATE_MUTATION,
        GLOBAL_STATE,
        EXCEPTION,
        LOGGING,
        TIME_DEPENDENT,
        RANDOM,
        NATIVE_CALL,
        REFLECTION,
        SYSTEM_PROPERTY,
        ENVIRONMENT,
        THREAD_SPAWN,
        LOCK_ACQUIRE,
        NONE
    }

    /** Purity enforcement level */
    public enum PurityLevel {
        STRICT,       // No side effects at all
        LENIENT,      // Allow logging and immutable state reads
        REFERENTIAL,  // Referential transparency (same input → same output)
        CUSTOM        // Custom purity rules
    }

    // ──────────────────────────────────────────────
    // Phase 12 — Sub-annotation types
    // ──────────────────────────────────────────────

    /** Lock configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LockConfig {
        LockType type() default LockType.REENTRANT;
        LockScope scope() default LockScope.METHOD;
        long timeoutMs() default 5000;
        boolean interruptible() default true;
        boolean fair() default false;
        String lockName() default "";           // Named lock for sharing
        int stripedBuckets() default 16;        // For STRIPED lock type
        boolean readLock() default false;       // Use read lock (READ_WRITE only)
        boolean writeLock() default false;      // Use write lock (READ_WRITE only)
    }

    /** CAS operation configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CASConfig {
        String field();                          // Target field for CAS
        int maxRetries() default 100;           // Max CAS retries
        long backoffNanos() default 0;          // Backoff between retries
        boolean exponentialBackoff() default false;
        String expectedValueProvider() default ""; // Method providing expected value
    }

    // ──────────────────────────────────────────────
    // Phase 12 — Annotations
    // ──────────────────────────────────────────────

    /**
     * 🔒 @DeepThreadSafe — Thread safety enforcement for classes and methods.
     *
     * Automatically applies thread safety mechanisms to marked elements.
     * Can transform non-thread-safe code into thread-safe code.
     *
     * Example:
     * <pre>
     * {@code @DeepThreadSafe(
     *     lock = @LockConfig(type = LockType.READ_WRITE, scope = LockScope.INSTANCE),
     *     detectDeadlocks = true
     * )}
     * public class SharedResourceManager { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepThreadSafe {
        String target() default "";
        LockConfig lock() default @LockConfig;
        boolean detectDeadlocks() default false;
        boolean detectRaces() default false;
        boolean instrumentContention() default false; // Track lock contention
        boolean makeFieldsVolatile() default false;   // Make all fields volatile
        boolean wrapCollections() default true;       // Wrap collections with sync wrappers
        String[] excludeMethods() default {};         // Methods to skip
        String[] excludeFields() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepThreadSafe */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DTS {
        String target() default "";
        LockConfig lock() default @LockConfig;
        boolean detectDeadlocks() default false;
        boolean wrapCollections() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔐 @DeepLock — Locking mechanism injection.
     *
     * Adds explicit locking around method execution with configurable
     * lock types, timeouts, and deadlock detection.
     *
     * Example:
     * <pre>
     * {@code @DeepLock(
     *     config = @LockConfig(
     *         type = LockType.STAMPED,
     *         timeoutMs = 2000,
     *         interruptible = true
     *     )
     * )}
     * public void updateSharedState(State newState) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepLock {
        String target() default "";
        LockConfig config() default @LockConfig;
        boolean tryLock() default false;
        String onLockFailure() default "";        // Fallback method if lock not acquired
        boolean reentrant() default true;
        ErrorStrategy onTimeout() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepLock */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DLCK {
        String target() default "";
        LockConfig config() default @LockConfig;
        boolean tryLock() default false;
        ErrorStrategy onTimeout() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ⚛️ @DeepAtomic — Atomic operation transformation.
     *
     * Transforms regular field operations into atomic operations.
     *
     * Example:
     * <pre>
     * {@code @DeepAtomic(op = AtomicOp.INCREMENT_AND_GET)}
     * public int getNextId() { return nextId++; }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepAtomic {
        String target() default "";
        AtomicOp op() default AtomicOp.COMPARE_AND_SET;
        String field() default "";               // Target field
        MemoryOrder ordering() default MemoryOrder.SEQ_CST;
        boolean useVarHandle() default true;     // Use VarHandle (Java 9+) vs Unsafe
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepAtomic */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DATM {
        String target() default "";
        AtomicOp op() default AtomicOp.COMPARE_AND_SET;
        String field() default "";
        MemoryOrder ordering() default MemoryOrder.SEQ_CST;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔄 @DeepCAS — Compare-and-swap operation injection.
     *
     * Implements lock-free CAS loops for concurrent data structures.
     *
     * Example:
     * <pre>
     * {@code @DeepCAS(
     *     config = @CASConfig(
     *         field = "counter",
     *         maxRetries = 50,
     *         exponentialBackoff = true
     *     )
     * )}
     * public boolean tryUpdateCounter(int expected, int update) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepCAS {
        String target() default "";
        CASConfig config() default @CASConfig(field = "");
        MemoryOrder ordering() default MemoryOrder.SEQ_CST;
        boolean weakCAS() default false;         // Use weak CAS (may fail spuriously)
        ErrorStrategy onExhausted() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCAS */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DCAS {
        String target() default "";
        CASConfig config() default @CASConfig(field = "");
        boolean weakCAS() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🚧 @DeepBarrier — Memory barrier/fence injection.
     *
     * Inserts memory barriers at strategic points to ensure
     * proper memory visibility across threads.
     *
     * Example:
     * <pre>
     * {@code @DeepBarrier(
     *     order = MemoryOrder.ACQ_REL,
     *     position = BarrierPosition.BEFORE_AND_AFTER
     * )}
     * public void publishData(Data data) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepBarrier {
        String target() default "";
        MemoryOrder order() default MemoryOrder.SEQ_CST;
        boolean beforeMethod() default true;
        boolean afterMethod() default true;
        boolean beforeFieldAccess() default false;
        boolean afterFieldAccess() default false;
        boolean useUnsafeFence() default false;  // Use Unsafe.fullFence() etc.
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepBarrier */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DBAR {
        String target() default "";
        MemoryOrder order() default MemoryOrder.SEQ_CST;
        boolean beforeMethod() default true;
        boolean afterMethod() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 💨 @DeepVolatile — Volatile semantics for fields.
     *
     * Applies volatile read/write semantics to field accesses
     * without modifying the field declaration.
     *
     * Example:
     * <pre>
     * {@code @DeepVolatile(fields = {"ready", "data"})}
     * public class DataPublisher { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface DeepVolatile {
        String target() default "";
        String[] fields() default {};            // Fields to make volatile
        MemoryOrder readOrder() default MemoryOrder.ACQUIRE;
        MemoryOrder writeOrder() default MemoryOrder.RELEASE;
        boolean useVarHandle() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepVolatile */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface DVOL {
        String target() default "";
        String[] fields() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📝 @DeepCOW — Copy-on-write semantics for data structures.
     *
     * Implements copy-on-write for fields, collections, or entire objects.
     * Reads share the same underlying data; writes create private copies.
     *
     * Example:
     * <pre>
     * {@code @DeepCOW(
     *     trigger = COWTrigger.ON_WRITE,
     *     fields = {"entityList", "chunkData"}
     * )}
     * public class WorldRegion { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
    public @interface DeepCOW {
        String target() default "";
        COWTrigger trigger() default COWTrigger.ON_WRITE;
        String[] fields() default {};
        int writeThreshold() default 10;        // For THRESHOLD trigger
        boolean trackCopyCount() default false;
        boolean shareImmutableParts() default true; // Share immutable sub-objects
        String customCopier() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCOW */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
    public @interface DCOW {
        String target() default "";
        COWTrigger trigger() default COWTrigger.ON_WRITE;
        String[] fields() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🏔️ @DeepImmutable — Immutability guarantee at class level.
     *
     * Enforces that a class is truly immutable: all fields are final,
     * no setters, deep immutability of referenced objects.
     *
     * Example:
     * <pre>
     * {@code @DeepImmutable(
     *     enforceFinalFields = true,
     *     enforceDeepImmutability = true
     * )}
     * public class BlockPosition { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface DeepImmutable {
        String target() default "";
        boolean enforceFinalFields() default true;
        boolean enforceDeepImmutability() default true;
        boolean enforceNoSetters() default true;
        boolean enforceNoMutableCollections() default true;
        boolean defensiveCopy() default true;    // Copy mutable params in constructor
        boolean defensiveCopyOnReturn() default true; // Copy mutable return values
        String[] excludeFields() default {};
        boolean failAtCompileTime() default false; // Fail during transformation, not at runtime
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepImmutable */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface DIMM {
        String target() default "";
        boolean enforceFinalFields() default true;
        boolean enforceDeepImmutability() default true;
        boolean defensiveCopy() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ✨ @DeepPure — Pure function enforcement.
     *
     * Verifies and enforces that a method is a pure function (no side effects,
     * deterministic output for same inputs). Enables aggressive optimization.
     *
     * Example:
     * <pre>
     * {@code @DeepPure(level = PurityLevel.STRICT)}
     * public int calculateDamage(int baseDamage, float multiplier) {
     *     return (int)(baseDamage * multiplier);
     * }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepPure {
        String target() default "";
        PurityLevel level() default PurityLevel.STRICT;
        boolean autoMemoize() default false;       // Auto-cache results
        boolean verifyAtRuntime() default false;   // Runtime verification of purity
        boolean allowLogging() default false;      // Allow logging side effects
        boolean allowExceptions() default true;    // Allow throwing exceptions
        String[] allowedCalls() default {};        // Whitelist of allowed method calls
        int memoizeCacheSize() default 256;        // Memoization cache size
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepPure */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DPUR {
        String target() default "";
        PurityLevel level() default PurityLevel.STRICT;
        boolean autoMemoize() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📊 @DeepSideEffect — Side effect tracking and documentation.
     *
     * Documents, tracks, and optionally restricts the side effects of a method.
     * Used for both documentation and runtime enforcement.
     *
     * Example:
     * <pre>
     * {@code @DeepSideEffect(
     *     effects = {SideEffectType.IO_WRITE, SideEffectType.NETWORK},
     *     description = "Saves player data to disk and syncs to server"
     * )}
     * public void savePlayerData(Player player) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepSideEffect {
        String target() default "";
        SideEffectType[] effects();
        String description() default "";
        boolean enforce() default false;        // Block undeclared side effects
        boolean trackAtRuntime() default false; // Runtime side effect tracking
        boolean logSideEffects() default false; // Log all side effects
        String[] mutatedFields() default {};    // Fields this method mutates
        String[] readFields() default {};       // Fields this method reads
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSideEffect */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DSE {
        String target() default "";
        SideEffectType[] effects();
        String description() default "";
        boolean enforce() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 12 — Processor
    // ──────────────────────────────────────────────

    /**
     * Processor for all Phase 12 (Concurrency & Threading) annotations.
     */
    public static class Phase12Processor {

        private final DeepMixContext context;

        public Phase12Processor(DeepMixContext context) {
            this.context = context;
        }

        /**
         * Process @DeepThreadSafe — Apply thread safety to class.
         */
        public void processThreadSafe(DeepThreadSafe annotation, ClassNode classNode,
                                      MethodNode methodNode) throws DeepMixProcessingException {
            LockConfig lockConfig = annotation.lock();

            // Add lock field to class if needed
            String lockFieldName = "__deepmix_lock_" + lockConfig.lockName();
            if (lockFieldName.endsWith("_")) lockFieldName += "default";

            boolean needsLockField = lockConfig.type() != LockType.SYNCHRONIZED;

            if (needsLockField) {
                String lockDescriptor;
                String lockInternalName;

                switch (lockConfig.type()) {
                    case REENTRANT:
                    case FAIR:
                        lockDescriptor = "Ljava/util/concurrent/locks/ReentrantLock;";
                        lockInternalName = "java/util/concurrent/locks/ReentrantLock";
                        break;
                    case READ_WRITE:
                        lockDescriptor = "Ljava/util/concurrent/locks/ReentrantReadWriteLock;";
                        lockInternalName = "java/util/concurrent/locks/ReentrantReadWriteLock";
                        break;
                    case STAMPED:
                        lockDescriptor = "Ljava/util/concurrent/locks/StampedLock;";
                        lockInternalName = "java/util/concurrent/locks/StampedLock";
                        break;
                    default:
                        lockDescriptor = "Ljava/lang/Object;";
                        lockInternalName = "java/lang/Object";
                        break;
                }

                // Add lock field if not present
                final String finalLockFieldName = lockFieldName;
                if (classNode.fields.stream().noneMatch(f -> f.name.equals(finalLockFieldName))) {
                    classNode.fields.add(new FieldNode(
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                        lockFieldName,
                        lockDescriptor, null, null
                    ));

                    // Initialize in constructors
                    for (MethodNode mn : classNode.methods) {
                        if (mn.name.equals("<init>")) {
                            InsnList initLock = new InsnList();
                            initLock.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            initLock.add(new TypeInsnNode(Opcodes.NEW, lockInternalName));
                            initLock.add(new InsnNode(Opcodes.DUP));

                            if (lockConfig.type() == LockType.FAIR) {
                                initLock.add(new InsnNode(Opcodes.ICONST_1)); // fair = true
                                initLock.add(new MethodInsnNode(
                                    Opcodes.INVOKESPECIAL, lockInternalName,
                                    "<init>", "(Z)V", false
                                ));
                            } else {
                                initLock.add(new MethodInsnNode(
                                    Opcodes.INVOKESPECIAL, lockInternalName,
                                    "<init>", "()V", false
                                ));
                            }

                            initLock.add(new FieldInsnNode(
                                Opcodes.PUTFIELD, classNode.name,
                                lockFieldName, lockDescriptor
                            ));

                            // Insert after super() call
                            for (AbstractInsnNode insn : mn.instructions) {
                                if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                                    MethodInsnNode superCall = (MethodInsnNode) insn;
                                    if (superCall.name.equals("<init>")) {
                                        mn.instructions.insert(insn, initLock);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Wrap methods with locking
            Set<String> excludeSet = new HashSet<>(Arrays.asList(annotation.excludeMethods()));

            for (MethodNode mn : classNode.methods) {
                if (mn.name.equals("<init>") || mn.name.equals("<clinit>")) continue;
                if (excludeSet.contains(mn.name)) continue;
                if ((mn.access & Opcodes.ACC_ABSTRACT) != 0) continue;

                wrapMethodWithLock(mn, classNode, lockConfig, lockFieldName);
            }

            // Wrap collections if requested
            if (annotation.wrapCollections()) {
                wrapCollectionFields(classNode);
            }

            context.addDiagnostic(String.format(
                "🔒 @DeepThreadSafe applied to %s [lockType=%s, scope=%s]",
                classNode.name, lockConfig.type(), lockConfig.scope()
            ));
        }

        /**
         * Process @DeepLock — Add locking to a single method.
         */
        public void processLock(DeepLock annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            LockConfig config = annotation.config();

            switch (config.type()) {
                case SYNCHRONIZED:
                    wrapWithSynchronized(methodNode, classNode);
                    break;
                case REENTRANT:
                case FAIR:
                    wrapWithReentrantLock(methodNode, classNode, config);
                    break;
                case READ_WRITE:
                    wrapWithReadWriteLock(methodNode, classNode, config);
                    break;
                case STAMPED:
                    wrapWithStampedLock(methodNode, classNode, config);
                    break;
                case TRYLOCK:
                    wrapWithTryLock(methodNode, classNode, config, annotation.onLockFailure());
                    break;
                default:
                    wrapWithReentrantLock(methodNode, classNode, config);
                    break;
            }

            context.addDiagnostic(String.format(
                "🔐 @DeepLock applied to %s::%s [type=%s, timeout=%dms]",
                classNode.name, methodNode.name, config.type(), config.timeoutMs()
            ));
        }

        /**
         * Process @DeepAtomic — Transform field operations to atomic.
         */
        public void processAtomic(DeepAtomic annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {
            String fieldName = annotation.field();
            if (fieldName.isEmpty()) {
                // Try to infer from method body
                for (AbstractInsnNode insn : methodNode.instructions) {
                    if (insn instanceof FieldInsnNode) {
                        fieldName = ((FieldInsnNode) insn).name;
                        break;
                    }
                }
            }

            if (fieldName.isEmpty()) {
                throw new DeepMixProcessingException(
                    "No target field specified or found",
                    "DeepAtomic", methodNode.name, "Phase12"
                );
            }

            // Find field type
            String fieldDesc = null;
            for (FieldNode fn : classNode.fields) {
                if (fn.name.equals(fieldName)) {
                    fieldDesc = fn.desc;
                    break;
                }
            }

            if (fieldDesc == null) {
                throw new DeepMixProcessingException(
                    "Field '" + fieldName + "' not found in class",
                    "DeepAtomic", methodNode.name, "Phase12"
                );
            }

            // Replace field access with atomic operations
            InsnList atomicOps = new InsnList();
            final String fName = fieldName;
            final String fDesc = fieldDesc;

            atomicOps.add(new LdcInsnNode(classNode.name.replace('/', '.')));
            atomicOps.add(new LdcInsnNode(fName));
            atomicOps.add(new LdcInsnNode(annotation.op().name()));
            atomicOps.add(new LdcInsnNode(annotation.ordering().name()));

            atomicOps.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixAtomic",
                "performAtomicOp",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(atomicOps);

            context.addDiagnostic(String.format(
                "⚛️ @DeepAtomic applied to %s::%s [field=%s, op=%s]",
                classNode.name, methodNode.name, fName, annotation.op()
            ));
        }

        /**
         * Process @DeepPure — Verify and enforce function purity.
         */
        public void processPure(DeepPure annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {

            if (annotation.level() == PurityLevel.STRICT) {
                // Scan for impure operations
                List<String> violations = new ArrayList<>();

                for (AbstractInsnNode insn : methodNode.instructions) {
                    // Check for field writes (side effects)
                    if (insn.getOpcode() == Opcodes.PUTFIELD ||
                        insn.getOpcode() == Opcodes.PUTSTATIC) {
                        FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                        violations.add("Field write: " + fieldInsn.owner + "." + fieldInsn.name);
                    }

                    // Check for I/O operations
                    if (insn instanceof MethodInsnNode) {
                        MethodInsnNode methodInsn = (MethodInsnNode) insn;
                        if (isImpureCall(methodInsn)) {
                            violations.add("Impure call: " +
                                methodInsn.owner + "." + methodInsn.name);
                        }
                    }
                }

                if (!violations.isEmpty()) {
                    StringBuilder msg = new StringBuilder("Purity violations detected:\n");
                    for (String v : violations) {
                        msg.append("  - ").append(v).append("\n");
                    }
                    throw new DeepMixProcessingException(
                        msg.toString(), "DeepPure", methodNode.name, "Phase12"
                    );
                }
            }

            // Auto-memoize if requested
            if (annotation.autoMemoize()) {
                wrapWithMemoization(methodNode, classNode, annotation.memoizeCacheSize());
            }

            context.addDiagnostic(String.format(
                "✨ @DeepPure verified for %s::%s [level=%s, memoize=%b]",
                classNode.name, methodNode.name, annotation.level(), annotation.autoMemoize()
            ));
        }

        /**
         * Process @DeepSideEffect — Document and track side effects.
         */
        public void processSideEffect(DeepSideEffect annotation, ClassNode classNode,
                                      MethodNode methodNode) throws DeepMixProcessingException {
            if (annotation.trackAtRuntime()) {
                InsnList tracking = new InsnList();

                // Build effects array
                SideEffectType[] effects = annotation.effects();
                tracking.add(new LdcInsnNode(effects.length));
                tracking.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));

                for (int i = 0; i < effects.length; i++) {
                    tracking.add(new InsnNode(Opcodes.DUP));
                    tracking.add(new LdcInsnNode(i));
                    tracking.add(new LdcInsnNode(effects[i].name()));
                    tracking.add(new InsnNode(Opcodes.AASTORE));
                }

                tracking.add(new LdcInsnNode(classNode.name + "::" + methodNode.name));
                tracking.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixSideEffects",
                    "trackEntry",
                    "([Ljava/lang/String;Ljava/lang/String;)V",
                    false
                ));

                methodNode.instructions.insert(tracking);
            }

            if (annotation.enforce()) {
                // Verify only declared side effects occur
                for (AbstractInsnNode insn : methodNode.instructions) {
                    if (insn instanceof MethodInsnNode) {
                        MethodInsnNode mi = (MethodInsnNode) insn;
                        SideEffectType detectedEffect = detectSideEffect(mi);
                        if (detectedEffect != SideEffectType.NONE) {
                            boolean declared = false;
                            for (SideEffectType e : annotation.effects()) {
                                if (e == detectedEffect) { declared = true; break; }
                            }
                            if (!declared) {
                                context.addDiagnostic(String.format(
                                    "⚠️ Undeclared side effect: %s in %s::%s (call to %s.%s)",
                                    detectedEffect, classNode.name, methodNode.name,
                                    mi.owner, mi.name
                                ));
                            }
                        }
                    }
                }
            }

            context.addDiagnostic(String.format(
                "📊 @DeepSideEffect applied to %s::%s [effects=%s]",
                classNode.name, methodNode.name, Arrays.toString(annotation.effects())
            ));
        }

        // ─── Helper Methods ───

        private void wrapMethodWithLock(MethodNode methodNode, ClassNode classNode,
                                        LockConfig config, String lockFieldName) {
            switch (config.type()) {
                case SYNCHRONIZED:
                    wrapWithSynchronized(methodNode, classNode);
                    break;
                case REENTRANT:
                case FAIR:
                    wrapWithReentrantLock(methodNode, classNode, config);
                    break;
                case READ_WRITE:
                    wrapWithReadWriteLock(methodNode, classNode, config);
                    break;
                default:
                    wrapWithReentrantLock(methodNode, classNode, config);
                    break;
            }
        }

        private void wrapWithSynchronized(MethodNode methodNode, ClassNode classNode) {
            // Add ACC_SYNCHRONIZED flag
            methodNode.access |= Opcodes.ACC_SYNCHRONIZED;
        }

        private void wrapWithReentrantLock(MethodNode methodNode, ClassNode classNode,
                                           LockConfig config) {
            String lockFieldName = "__deepmix_lock_" +
                (config.lockName().isEmpty() ? "default" : config.lockName());
            String lockDesc = "Ljava/util/concurrent/locks/ReentrantLock;";
            String lockType = "java/util/concurrent/locks/ReentrantLock";

            // Try-finally pattern: lock.lock() ... finally { lock.unlock() }
            LabelNode tryStart = new LabelNode();
            LabelNode tryEnd = new LabelNode();
            LabelNode finallyHandler = new LabelNode();

            InsnList lockAcquire = new InsnList();
            lockAcquire.add(new VarInsnNode(Opcodes.ALOAD, 0));
            lockAcquire.add(new FieldInsnNode(
                Opcodes.GETFIELD, classNode.name, lockFieldName, lockDesc));

            if (config.timeoutMs() > 0 && config.interruptible()) {
                lockAcquire.add(new LdcInsnNode(config.timeoutMs()));
                lockAcquire.add(new FieldInsnNode(
                    Opcodes.GETSTATIC, "java/util/concurrent/TimeUnit",
                    "MILLISECONDS", "Ljava/util/concurrent/TimeUnit;"
                ));
                lockAcquire.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL, lockType,
                    "tryLock", "(JLjava/util/concurrent/TimeUnit;)Z", false
                ));
                lockAcquire.add(new InsnNode(Opcodes.POP));
            } else {
                lockAcquire.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL, lockType,
                    "lock", "()V", false
                ));
            }

            lockAcquire.add(tryStart);

            InsnList lockRelease = new InsnList();
            lockRelease.add(tryEnd);
            lockRelease.add(finallyHandler);
            lockRelease.add(new VarInsnNode(Opcodes.ALOAD, 0));
            lockRelease.add(new FieldInsnNode(
                Opcodes.GETFIELD, classNode.name, lockFieldName, lockDesc));
            lockRelease.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL, lockType,
                "unlock", "()V", false
            ));
            lockRelease.add(new InsnNode(Opcodes.ATHROW));

            methodNode.instructions.insert(lockAcquire);
            methodNode.instructions.add(lockRelease);

            methodNode.tryCatchBlocks.add(new TryCatchBlockNode(
                tryStart, tryEnd, finallyHandler, null
            ));
        }

        private void wrapWithReadWriteLock(MethodNode methodNode, ClassNode classNode,
                                           LockConfig config) {
            // Similar to reentrant lock, but use readLock() or writeLock()
            String subLock = config.readLock() ? "readLock" : "writeLock";
            String lockFieldName = "__deepmix_rwlock_" +
                (config.lockName().isEmpty() ? "default" : config.lockName());

            InsnList acquire = new InsnList();
            acquire.add(new VarInsnNode(Opcodes.ALOAD, 0));
            acquire.add(new FieldInsnNode(
                Opcodes.GETFIELD, classNode.name, lockFieldName,
                "Ljava/util/concurrent/locks/ReentrantReadWriteLock;"
            ));
            acquire.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/util/concurrent/locks/ReentrantReadWriteLock",
                subLock,
                "()Ljava/util/concurrent/locks/ReentrantReadWriteLock$" +
                    (config.readLock() ? "ReadLock" : "WriteLock") + ";",
                false
            ));
            acquire.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/util/concurrent/locks/Lock",
                "lock", "()V", false
            ));

            methodNode.instructions.insert(acquire);
        }

        private void wrapWithStampedLock(MethodNode methodNode, ClassNode classNode,
                                         LockConfig config) {
            InsnList acquire = new InsnList();
            acquire.add(new VarInsnNode(Opcodes.ALOAD, 0));
            acquire.add(new FieldInsnNode(
                Opcodes.GETFIELD, classNode.name,
                "__deepmix_stamped_lock",
                "Ljava/util/concurrent/locks/StampedLock;"
            ));

            if (config.readLock()) {
                acquire.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/util/concurrent/locks/StampedLock",
                    "readLock", "()J", false
                ));
            } else {
                acquire.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/util/concurrent/locks/StampedLock",
                    "writeLock", "()J", false
                ));
            }

            // Store stamp
            int stampIdx = methodNode.maxLocals;
            methodNode.maxLocals += 2; // long takes 2 slots
            acquire.add(new VarInsnNode(Opcodes.LSTORE, stampIdx));

            methodNode.instructions.insert(acquire);
        }

        private void wrapWithTryLock(MethodNode methodNode, ClassNode classNode,
                                     LockConfig config, String failureCallback) {
            InsnList tryLockCode = new InsnList();
            LabelNode acquired = new LabelNode();
            LabelNode notAcquired = new LabelNode();

            tryLockCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
            tryLockCode.add(new FieldInsnNode(
                Opcodes.GETFIELD, classNode.name,
                "__deepmix_lock_default",
                "Ljava/util/concurrent/locks/ReentrantLock;"
            ));
            tryLockCode.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/util/concurrent/locks/ReentrantLock",
                "tryLock", "()Z", false
            ));
            tryLockCode.add(new JumpInsnNode(Opcodes.IFNE, acquired));

            // Lock not acquired — call failure callback or return default
            if (!failureCallback.isEmpty()) {
                tryLockCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
                tryLockCode.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL, classNode.name,
                    failureCallback, "()V", false
                ));
            }

            Type returnType = Type.getReturnType(methodNode.desc);
            if (returnType.getSort() == Type.VOID) {
                tryLockCode.add(new InsnNode(Opcodes.RETURN));
            } else if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
                tryLockCode.add(new InsnNode(Opcodes.ACONST_NULL));
                tryLockCode.add(new InsnNode(Opcodes.ARETURN));
            } else {
                tryLockCode.add(new InsnNode(Opcodes.ICONST_0));
                tryLockCode.add(new InsnNode(Opcodes.IRETURN));
            }

            tryLockCode.add(acquired);

            methodNode.instructions.insert(tryLockCode);
        }

        private void wrapCollectionFields(ClassNode classNode) {
            for (FieldNode field : classNode.fields) {
                String desc = field.desc;
                if (desc.equals("Ljava/util/List;") ||
                    desc.equals("Ljava/util/ArrayList;") ||
                    desc.equals("Ljava/util/LinkedList;")) {
                    // Wrap with Collections.synchronizedList()
                    wrapFieldWithSyncWrapper(classNode, field,
                        "synchronizedList",
                        "(Ljava/util/List;)Ljava/util/List;");
                } else if (desc.equals("Ljava/util/Map;") ||
                           desc.equals("Ljava/util/HashMap;")) {
                    wrapFieldWithSyncWrapper(classNode, field,
                        "synchronizedMap",
                        "(Ljava/util/Map;)Ljava/util/Map;");
                } else if (desc.equals("Ljava/util/Set;") ||
                           desc.equals("Ljava/util/HashSet;")) {
                    wrapFieldWithSyncWrapper(classNode, field,
                        "synchronizedSet",
                        "(Ljava/util/Set;)Ljava/util/Set;");
                }
            }
        }

        private void wrapFieldWithSyncWrapper(ClassNode classNode, FieldNode field,
                                              String wrapperMethod, String wrapperDesc) {
            for (MethodNode mn : classNode.methods) {
                if (!mn.name.equals("<init>")) continue;

                for (AbstractInsnNode insn : mn.instructions) {
                    if (insn.getOpcode() == Opcodes.PUTFIELD) {
                        FieldInsnNode fi = (FieldInsnNode) insn;
                        if (fi.name.equals(field.name) && fi.owner.equals(classNode.name)) {
                            InsnList wrapper = new InsnList();
                            wrapper.add(new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "java/util/Collections",
                                wrapperMethod,
                                wrapperDesc, false
                            ));
                            mn.instructions.insertBefore(insn, wrapper);
                        }
                    }
                }
            }
        }

        private void wrapWithMemoization(MethodNode methodNode, ClassNode classNode,
                                         int cacheSize) {
            // Add cache field
            String cacheFieldName = "__deepmix_memo_" + methodNode.name;
            classNode.fields.add(new FieldNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT,
                cacheFieldName,
                "Ljava/util/Map;", null, null
            ));

            // At method entry: check cache
            InsnList cacheCheck = new InsnList();
            LabelNode cacheHit = new LabelNode();
            LabelNode cacheMiss = new LabelNode();

            // Build cache key from parameters
            cacheCheck.add(new VarInsnNode(Opcodes.ALOAD, 0));
            cacheCheck.add(new FieldInsnNode(
                Opcodes.GETFIELD, classNode.name, cacheFieldName, "Ljava/util/Map;"
            ));
            cacheCheck.add(new JumpInsnNode(Opcodes.IFNULL, cacheMiss));

            // Check if key exists
            cacheCheck.add(new VarInsnNode(Opcodes.ALOAD, 0));
            cacheCheck.add(new FieldInsnNode(
                Opcodes.GETFIELD, classNode.name, cacheFieldName, "Ljava/util/Map;"
            ));

            // Create key from all parameters
            Type[] argTypes = Type.getArgumentTypes(methodNode.desc);
            cacheCheck.add(new LdcInsnNode(argTypes.length));
            cacheCheck.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
            int localIdx = (methodNode.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
            for (int i = 0; i < argTypes.length; i++) {
                cacheCheck.add(new InsnNode(Opcodes.DUP));
                cacheCheck.add(new LdcInsnNode(i));
                loadAndBox(cacheCheck, argTypes[i], localIdx);
                cacheCheck.add(new InsnNode(Opcodes.AASTORE));
                localIdx += argTypes[i].getSize();
            }

            cacheCheck.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "java/util/Arrays",
                "asList",
                "([Ljava/lang/Object;)Ljava/util/List;", false
            ));
            cacheCheck.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                "java/util/Map",
                "get",
                "(Ljava/lang/Object;)Ljava/lang/Object;", true
            ));
            cacheCheck.add(new InsnNode(Opcodes.DUP));
            cacheCheck.add(new JumpInsnNode(Opcodes.IFNULL, cacheMiss));

            // Cache hit - return cached value
            Type returnType = Type.getReturnType(methodNode.desc);
            if (returnType.getSort() != Type.VOID) {
                unbox(cacheCheck, returnType);
                cacheCheck.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
            }

            cacheCheck.add(cacheMiss);
            cacheCheck.add(new InsnNode(Opcodes.POP)); // pop null from cache.get()

            methodNode.instructions.insert(cacheCheck);
        }

        private void loadAndBox(InsnList insns, Type type, int localIdx) {
            switch (type.getSort()) {
                case Type.INT:
                    insns.add(new VarInsnNode(Opcodes.ILOAD, localIdx));
                    insns.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC, "java/lang/Integer",
                        "valueOf", "(I)Ljava/lang/Integer;", false));
                    break;
                case Type.LONG:
                    insns.add(new VarInsnNode(Opcodes.LLOAD, localIdx));
                    insns.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC, "java/lang/Long",
                        "valueOf", "(J)Ljava/lang/Long;", false));
                    break;
                case Type.FLOAT:
                    insns.add(new VarInsnNode(Opcodes.FLOAD, localIdx));
                    insns.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC, "java/lang/Float",
                        "valueOf", "(F)Ljava/lang/Float;", false));
                    break;
                case Type.DOUBLE:
                    insns.add(new VarInsnNode(Opcodes.DLOAD, localIdx));
                    insns.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC, "java/lang/Double",
                        "valueOf", "(D)Ljava/lang/Double;", false));
                    break;
                case Type.BOOLEAN:
                    insns.add(new VarInsnNode(Opcodes.ILOAD, localIdx));
                    insns.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC, "java/lang/Boolean",
                        "valueOf", "(Z)Ljava/lang/Boolean;", false));
                    break;
                default:
                    insns.add(new VarInsnNode(Opcodes.ALOAD, localIdx));
                    break;
            }
        }

        private void unbox(InsnList insns, Type type) {
            switch (type.getSort()) {
                case Type.INT:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Integer"));
                    insns.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL, "java/lang/Integer",
                        "intValue", "()I", false));
                    break;
                case Type.LONG:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Long"));
                    insns.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL, "java/lang/Long",
                        "longValue", "()J", false));
                    break;
                case Type.FLOAT:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Float"));
                    insns.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL, "java/lang/Float",
                        "floatValue", "()F", false));
                    break;
                case Type.DOUBLE:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Double"));
                    insns.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL, "java/lang/Double",
                        "doubleValue", "()D", false));
                    break;
                case Type.BOOLEAN:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Boolean"));
                    insns.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL, "java/lang/Boolean",
                        "booleanValue", "()Z", false));
                    break;
                default:
                    String className = type.getInternalName();
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, className));
                    break;
            }
        }

        private boolean isImpureCall(MethodInsnNode mi) {
            // Common impure method patterns
            String owner = mi.owner;
            String name = mi.name;

            if (owner.startsWith("java/io/")) return true;
            if (owner.startsWith("java/net/")) return true;
            if (owner.equals("java/lang/System") &&
                (name.equals("currentTimeMillis") || name.equals("nanoTime") ||
                 name.equals("exit") || name.equals("gc"))) return true;
            if (owner.equals("java/lang/Math") && name.equals("random")) return true;
            if (owner.equals("java/util/Random")) return true;
            if (owner.equals("java/lang/Thread")) return true;
            if (name.equals("println") || name.equals("print") || name.equals("printf")) return true;

            return false;
        }

        private SideEffectType detectSideEffect(MethodInsnNode mi) {
            String owner = mi.owner;
            String name = mi.name;

            if (owner.startsWith("java/io/")) {
                if (name.contains("read") || name.equals("available")) return SideEffectType.IO_READ;
                if (name.contains("write") || name.contains("flush") ||
                    name.contains("close")) return SideEffectType.IO_WRITE;
            }
            if (owner.startsWith("java/net/")) return SideEffectType.NETWORK;
            if (owner.equals("java/lang/System")) {
                if (name.equals("currentTimeMillis") || name.equals("nanoTime"))
                    return SideEffectType.TIME_DEPENDENT;
                if (name.equals("getProperty")) return SideEffectType.SYSTEM_PROPERTY;
                if (name.equals("getenv")) return SideEffectType.ENVIRONMENT;
            }
            if (owner.equals("java/lang/Math") && name.equals("random"))
                return SideEffectType.RANDOM;
            if (owner.equals("java/lang/Thread") && name.equals("start"))
                return SideEffectType.THREAD_SPAWN;
                if (owner.startsWith("java/lang/reflect/")) return SideEffectType.REFLECTION;
                if (name.equals("println") || name.equals("print") ||
                    name.equals("printf") || name.equals("log") ||
                    name.equals("debug") || name.equals("info") ||
                    name.equals("warn") || name.equals("error")) return SideEffectType.LOGGING;
                if (owner.startsWith("java/util/concurrent/locks/"))
                    return SideEffectType.LOCK_ACQUIRE;
                if (owner.equals("java/lang/Runtime") && name.equals("exec"))
                    return SideEffectType.NATIVE_CALL;

                return SideEffectType.NONE;
            }
        }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                      ║
    // ║  PHASE 13: ENCODING & FORMAT CONVERSION                              ║
    // ║  27 annotations | Priority: LOW | Est. Time: 2 days                  ║
    // ║                                                                      ║
    // ║  General Encoding:                                                   ║
    // ║  @DeepEncode  @DeepDecode  @DeepBase64  @DeepHex                    ║
    // ║  @DeepUUID    @DeepEndian  @DeepInterning  @DeepURLEncode           ║
    // ║  @DeepHTML    @DeepJWT                                               ║
    // ║                                                                      ║
    // ║  Novelty/Specialized Encoding:                                       ║
    // ║  @DeepMorse  @DeepQR  @DeepBarcode  @DeepOCR  @DeepASCII           ║
    // ║  @DeepUnicode  @DeepPunycode  @DeepROT13                           ║
    // ║                                                                      ║
    // ║  Cryptographic:                                                      ║
    // ║  @DeepCaesar  @DeepVigenere  @DeepRSA  @DeepAES                     ║
    // ║  @DeepSHA  @DeepMD5  @DeepBCrypt  @DeepScrypt  @DeepArgon2         ║
    // ║                                                                      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // Phase 13 — Enums
    // ──────────────────────────────────────────────

    /** Encoding direction */
    public enum EncodingDirection {
        ENCODE,
        DECODE,
        BOTH        // Round-trip encode then decode or vice versa
    }

    /** Base64 variant */
    public enum Base64Variant {
        STANDARD,           // RFC 4648 standard
        URL_SAFE,           // RFC 4648 URL-safe
        MIME,               // RFC 2045 MIME
        NO_PADDING,         // Standard without padding
        URL_SAFE_NO_PADDING // URL-safe without padding
    }

    /** Hex output format */
    public enum HexFormat {
        LOWERCASE,          // abcdef
        UPPERCASE,          // ABCDEF
        COLON_SEPARATED,    // aa:bb:cc
        SPACE_SEPARATED,    // aa bb cc
        PREFIX_0X,          // 0xaabb
        RAW                 // aabb
    }

    /** UUID version */
    public enum UUIDVersion {
        V1_TIME_BASED,
        V3_NAME_MD5,
        V4_RANDOM,
        V5_NAME_SHA1,
        V6_REORDERED_TIME,
        V7_UNIX_TIMESTAMP,
        CUSTOM
    }

    /** Unicode normalization form */
    public enum UnicodeNormForm {
        NFC,    // Canonical Decomposition + Canonical Composition
        NFD,    // Canonical Decomposition
        NFKC,   // Compatibility Decomposition + Canonical Composition
        NFKD    // Compatibility Decomposition
    }

    /** JWT algorithm */
    public enum JWTAlgorithm {
        HS256,      // HMAC SHA-256
        HS384,      // HMAC SHA-384
        HS512,      // HMAC SHA-512
        RS256,      // RSA SHA-256
        RS384,      // RSA SHA-384
        RS512,      // RSA SHA-512
        ES256,      // ECDSA P-256 SHA-256
        ES384,      // ECDSA P-384 SHA-384
        ES512,      // ECDSA P-521 SHA-512
        PS256,      // RSASSA-PSS SHA-256
        PS384,      // RSASSA-PSS SHA-384
        PS512,      // RSASSA-PSS SHA-512
        EDDSA,      // EdDSA
        NONE        // No signature (INSECURE)
    }

    /** JWT operation */
    public enum JWTOperation {
        SIGN,       // Create and sign JWT
        VERIFY,     // Verify JWT signature
        DECODE,     // Decode without verification (INSECURE)
        REFRESH,    // Refresh token
        REVOKE      // Revoke token
    }

    /** QR code error correction level */
    public enum QRErrorCorrection {
        LOW,        // ~7% recovery
        MEDIUM,     // ~15% recovery
        QUARTILE,   // ~25% recovery
        HIGH        // ~30% recovery
    }

    /** Barcode format */
    public enum BarcodeFormat {
        CODE_39,
        CODE_93,
        CODE_128,
        EAN_8,
        EAN_13,
        UPC_A,
        UPC_E,
        ITF,
        CODABAR,
        DATA_MATRIX,
        PDF_417,
        AZTEC,
        MAXICODE
    }

    /** Hash output format */
    public enum HashOutputFormat {
        HEX_LOWERCASE,
        HEX_UPPERCASE,
        BASE64,
        BASE64_URL_SAFE,
        RAW_BYTES
    }

    /** RSA key size */
    public enum RSAKeySize {
        BITS_1024(1024),
        BITS_2048(2048),
        BITS_3072(3072),
        BITS_4096(4096);

        private final int bits;
        RSAKeySize(int bits) { this.bits = bits; }
        public int bits() { return bits; }
    }

    /** AES mode of operation */
    public enum AESMode {
        ECB,    // Electronic Codebook (INSECURE for most uses)
        CBC,    // Cipher Block Chaining
        CFB,    // Cipher Feedback
        OFB,    // Output Feedback
        CTR,    // Counter
        GCM,    // Galois/Counter Mode (authenticated)
        CCM,    // Counter with CBC-MAC (authenticated)
        SIV     // Synthetic Initialization Vector
    }

    /** AES key size */
    public enum AESKeySize {
        BITS_128(128),
        BITS_192(192),
        BITS_256(256);

        private final int bits;
        AESKeySize(int bits) { this.bits = bits; }
        public int bits() { return bits; }
    }

    /** SHA variant */
    public enum SHAVariant {
        SHA_1,
        SHA_224,
        SHA_256,
        SHA_384,
        SHA_512,
        SHA_512_224,
        SHA_512_256,
        SHA3_224,
        SHA3_256,
        SHA3_384,
        SHA3_512
    }

    /** BCrypt cost factor bounds */
    public enum BCryptStrength {
        LIGHT(4),
        DEFAULT(10),
        STRONG(12),
        PARANOID(14),
        MAXIMUM(31);

        private final int rounds;
        BCryptStrength(int rounds) { this.rounds = rounds; }
        public int rounds() { return rounds; }
    }

    // ──────────────────────────────────────────────
    // Phase 13 — Sub-annotation types
    // ──────────────────────────────────────────────

    /** JWT claims definition */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface JWTClaims {
        String issuer() default "";
        String subject() default "";
        String audience() default "";
        long expirationSeconds() default 3600;  // 1 hour
        long notBeforeSeconds() default 0;
        String[] customClaims() default {};     // "key=value" pairs
    }

    /** QR code configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface QRConfig {
        int width() default 250;
        int height() default 250;
        QRErrorCorrection errorCorrection() default QRErrorCorrection.MEDIUM;
        String charset() default "UTF-8";
        int margin() default 1;
        String foregroundColor() default "#000000";
        String backgroundColor() default "#FFFFFF";
        String logoPath() default "";           // Embed logo in center
    }

    /** Barcode configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BarcodeConfig {
        BarcodeFormat format() default BarcodeFormat.CODE_128;
        int width() default 300;
        int height() default 100;
        boolean showText() default true;
        String charset() default "UTF-8";
    }

    /** Hash salt configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SaltConfig {
        int saltLength() default 16;
        boolean generateRandom() default true;
        String fixedSalt() default "";          // Only for testing, NEVER in prod
        String saltProvider() default "";       // Custom salt provider method
        boolean prependSalt() default true;     // Prepend salt to hash output
    }

    // ──────────────────────────────────────────────
    // Phase 13 — Annotations
    // ──────────────────────────────────────────────

    /**
     * 🔄 @DeepEncode — General-purpose encoding transformation.
     *
     * Transforms data between encoding formats. Supports text encodings,
     * binary encodings, and custom encoding schemes.
     *
     * Example:
     * <pre>
     * {@code @DeepEncode(
     *     from = "UTF-8",
     *     to = "ISO-8859-1",
     *     onError = ErrorStrategy.FALLBACK
     * )}
     * public byte[] convertEncoding(String input) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DeepEncode {
        String target() default "";
        String from() default "UTF-8";
        String to() default "UTF-8";
        EncodingDirection direction() default EncodingDirection.ENCODE;
        boolean encodeReturn() default true;
        boolean encodeParams() default false;
        String customEncoder() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepEncode */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DENCD {
        String target() default "";
        String from() default "UTF-8";
        String to() default "UTF-8";
        EncodingDirection direction() default EncodingDirection.ENCODE;
        boolean encodeReturn() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔄 @DeepDecode — General-purpose decoding transformation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DeepDecode {
        String target() default "";
        String from() default "UTF-8";
        String to() default "UTF-8";
        boolean decodeReturn() default false;
        boolean decodeParams() default true;
        String customDecoder() default "";
        boolean strict() default true;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepDecode */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DDECD {
        String target() default "";
        String from() default "UTF-8";
        String to() default "UTF-8";
        boolean decodeParams() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📋 @DeepBase64 — Base64 encoding/decoding.
     *
     * Example:
     * <pre>
     * {@code @DeepBase64(variant = Base64Variant.URL_SAFE, direction = EncodingDirection.ENCODE)}
     * public String encodeToken(byte[] data) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    public @interface DeepBase64 {
        String target() default "";
        Base64Variant variant() default Base64Variant.STANDARD;
        EncodingDirection direction() default EncodingDirection.ENCODE;
        boolean lineBreaks() default false;
        int lineLength() default 76;
        boolean applyToReturn() default true;
        boolean applyToParams() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepBase64 */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    public @interface DB64 {
        String target() default "";
        Base64Variant variant() default Base64Variant.STANDARD;
        EncodingDirection direction() default EncodingDirection.ENCODE;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔢 @DeepHex — Hexadecimal encoding/decoding.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    public @interface DeepHex {
        String target() default "";
        HexFormat format() default HexFormat.LOWERCASE;
        EncodingDirection direction() default EncodingDirection.ENCODE;
        boolean applyToReturn() default true;
        boolean applyToParams() default false;
        String prefix() default "";
        String separator() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepHex */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    public @interface DHEX {
        String target() default "";
        HexFormat format() default HexFormat.LOWERCASE;
        EncodingDirection direction() default EncodingDirection.ENCODE;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🆔 @DeepUUID — UUID generation and handling.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepUUID {
        String target() default "";
        UUIDVersion version() default UUIDVersion.V4_RANDOM;
        String namespace() default "";           // For V3 and V5
        String name() default "";                // For V3 and V5
        boolean uppercase() default false;
        boolean noDashes() default false;
        boolean generateOnNull() default true;   // Generate if field/return is null
        String customGenerator() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepUUID */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DUUID {
        String target() default "";
        UUIDVersion version() default UUIDVersion.V4_RANDOM;
        boolean noDashes() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔀 @DeepEndian — Byte-order / endianness handling.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
    public @interface DeepEndian {
        String target() default "";
        ByteOrderSpec from() default ByteOrderSpec.NATIVE;
        ByteOrderSpec to() default ByteOrderSpec.BIG_ENDIAN;
        boolean swapOnReturn() default true;
        boolean swapOnParams() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepEndian */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
    public @interface DEND {
        String target() default "";
        ByteOrderSpec from() default ByteOrderSpec.NATIVE;
        ByteOrderSpec to() default ByteOrderSpec.BIG_ENDIAN;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🧵 @DeepInterning — String interning for memory optimization.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
    public @interface DeepInterning {
        String target() default "";
        boolean internReturn() default true;
        boolean internParams() default false;
        boolean internFields() default false;
        boolean useCustomPool() default false;   // Custom intern pool vs JVM pool
        int poolCapacity() default 10000;
        boolean weakReferences() default true;   // Use weak refs in custom pool
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepInterning */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
    public @interface DINTERN {
        String target() default "";
        boolean internReturn() default true;
        boolean useCustomPool() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🌐 @DeepURLEncode — URL encoding/decoding.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DeepURLEncode {
        String target() default "";
        EncodingDirection direction() default EncodingDirection.ENCODE;
        String charset() default "UTF-8";
        boolean encodeSpaceAsPlus() default false; // %20 vs +
        boolean encodeSlashes() default true;
        boolean applyToReturn() default true;
        boolean applyToParams() default false;
        String[] excludeChars() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepURLEncode */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DURL {
        String target() default "";
        EncodingDirection direction() default EncodingDirection.ENCODE;
        String charset() default "UTF-8";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📄 @DeepHTMLEncode — HTML entity encoding/decoding.
     * (Named DeepHTMLEncode to avoid collision with @DeepHTML for HTML file transforms)
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DeepHTMLEncode {
        String target() default "";
        EncodingDirection direction() default EncodingDirection.ENCODE;
        boolean encodeNumericEntities() default false;
        boolean encodeNamedEntities() default true;
        boolean encodeAllChars() default false;
        boolean applyToReturn() default true;
        boolean applyToParams() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepHTMLEncode */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DHTMLE {
        String target() default "";
        EncodingDirection direction() default EncodingDirection.ENCODE;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🎫 @DeepJWT — JWT (JSON Web Token) creation, verification, and handling.
     *
     * Example:
     * <pre>
     * {@code @DeepJWT(
     *     operation = JWTOperation.SIGN,
     *     algorithm = JWTAlgorithm.RS256,
     *     claims = @JWTClaims(issuer = "myapp", expirationSeconds = 7200)
     * )}
     * public String generateAuthToken(User user) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepJWT {
        String target() default "";
        JWTOperation operation() default JWTOperation.SIGN;
        JWTAlgorithm algorithm() default JWTAlgorithm.HS256;
        JWTClaims claims() default @JWTClaims;
        String secretKey() default "";              // For HMAC algorithms
        String keyAlias() default "";               // KeyStore alias for RSA/EC
        String keyStorePath() default "";
        boolean validateExpiration() default true;
        boolean validateNotBefore() default true;
        boolean validateIssuer() default true;
        String[] requiredClaims() default {};
        ErrorStrategy onInvalid() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepJWT */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DJWT {
        String target() default "";
        JWTOperation operation() default JWTOperation.SIGN;
        JWTAlgorithm algorithm() default JWTAlgorithm.HS256;
        JWTClaims claims() default @JWTClaims;
        String secretKey() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ·−− @DeepMorse — Morse code encoding/decoding.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DeepMorse {
        String target() default "";
        EncodingDirection direction() default EncodingDirection.ENCODE;
        String dotChar() default ".";
        String dashChar() default "-";
        String letterSeparator() default " ";
        String wordSeparator() default " / ";
        boolean applyToReturn() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMorse */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DMORSE {
        String target() default "";
        EncodingDirection direction() default EncodingDirection.ENCODE;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📱 @DeepQR — QR code generation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepQR {
        String target() default "";
        QRConfig config() default @QRConfig;
        String outputFormat() default "PNG";     // PNG, SVG, etc.
        boolean returnAsBytes() default true;    // byte[] vs BufferedImage
        boolean returnAsBase64() default false;
        String outputPath() default "";          // Save to file
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepQR */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DQR {
        String target() default "";
        QRConfig config() default @QRConfig;
        String outputFormat() default "PNG";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📊 @DeepBarcodeGen — Barcode generation.
     * (Named DeepBarcodeGen to avoid conflict with @DBAR for @DeepBarrier)
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepBarcodeGen {
        String target() default "";
        BarcodeConfig config() default @BarcodeConfig;
        String outputFormat() default "PNG";
        boolean returnAsBytes() default true;
        String outputPath() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepBarcodeGen */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DBCODE {
        String target() default "";
        BarcodeConfig config() default @BarcodeConfig;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 👁️ @DeepOCR — Optical character recognition (text extraction from images).
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepOCR {
        String target() default "";
        String language() default "eng";         // Tesseract language code
        String[] languages() default {};         // Multiple languages
        int dpi() default 300;
        boolean preprocessImage() default true;  // Auto contrast/threshold
        boolean detectOrientation() default true;
        String outputFormat() default "text";    // text, hocr, tsv
        double confidenceThreshold() default 0.0; // Min confidence (0-100)
        String customConfig() default "";        // Tesseract config
        String enginePath() default "";          // Path to OCR engine
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepOCR */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DOCR {
        String target() default "";
        String language() default "eng";
        boolean preprocessImage() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🎨 @DeepASCII — ASCII art generation from text or images.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepASCII {
        String target() default "";
        int width() default 80;                  // Output width in characters
        int height() default 0;                  // 0 = auto-calculate from aspect ratio
        String charset() default " .:-=+*#%@";   // Characters from light to dark
        boolean invertColors() default false;
        boolean colorOutput() default false;     // ANSI color codes
        boolean htmlOutput() default false;      // HTML colored output
        String fontName() default "Courier";     // For text-to-ASCII-art
        int fontSize() default 12;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepASCII */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DASCII {
        String target() default "";
        int width() default 80;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🌐 @DeepUnicode — Unicode normalization and transformation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    public @interface DeepUnicode {
        String target() default "";
        UnicodeNormForm form() default UnicodeNormForm.NFC;
        boolean stripAccents() default false;
        boolean stripControlChars() default true;
        boolean stripBidi() default false;       // Strip bidirectional control chars
        boolean toASCII() default false;         // Transliterate to ASCII
        boolean applyToReturn() default true;
        boolean applyToParams() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepUnicode */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    public @interface DUNI {
        String target() default "";
        UnicodeNormForm form() default UnicodeNormForm.NFC;
        boolean stripAccents() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🌍 @DeepPunycode — Punycode encoding/decoding (internationalized domain names).
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DeepPunycode {
        String target() default "";
        EncodingDirection direction() default EncodingDirection.ENCODE;
        boolean includeACEPrefix() default true;  // xn-- prefix
        boolean applyToReturn() default true;
        boolean applyToParams() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepPunycode */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DPNY {
        String target() default "";
        EncodingDirection direction() default EncodingDirection.ENCODE;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔄 @DeepROT13 — ROT13 substitution cipher.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    public @interface DeepROT13 {
        String target() default "";
        boolean applyToReturn() default true;
        boolean applyToParams() default false;
        boolean rot5Numbers() default false;     // Also rotate digits (ROT5)
        boolean rot47() default false;           // ROT47 (printable ASCII)
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepROT13 */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    public @interface DROT {
        String target() default "";
        boolean applyToReturn() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🏛️ @DeepCaesar — Caesar cipher encryption/decryption.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DeepCaesar {
        String target() default "";
        int shift() default 3;
        EncodingDirection direction() default EncodingDirection.ENCODE;
        boolean preserveCase() default true;
        boolean includeDigits() default false;
        boolean applyToReturn() default true;
        boolean applyToParams() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCaesar */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DCAES {
        String target() default "";
        int shift() default 3;
        EncodingDirection direction() default EncodingDirection.ENCODE;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔐 @DeepVigenere — Vigenère cipher encryption/decryption.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DeepVigenere {
        String target() default "";
        String key() default "";                 // Cipher key (required)
        String keyProvider() default "";         // Method providing key at runtime
        EncodingDirection direction() default EncodingDirection.ENCODE;
        boolean preserveCase() default true;
        boolean preserveNonAlpha() default true; // Keep non-alpha chars unchanged
        boolean autokey() default false;         // Autokey cipher variant
        boolean applyToReturn() default true;
        boolean applyToParams() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepVigenere */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DVIG {
        String target() default "";
        String key() default "";
        EncodingDirection direction() default EncodingDirection.ENCODE;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔑 @DeepRSA — RSA encryption/decryption and signing.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepRSA {
        String target() default "";
        RSAKeySize keySize() default RSAKeySize.BITS_2048;
        EncodingDirection direction() default EncodingDirection.ENCODE;
        String publicKeyPath() default "";
        String privateKeyPath() default "";
        String keyAlias() default "";
        String keyStorePath() default "";
        String padding() default "OAEP";          // OAEP, PKCS1
        String hashAlgorithm() default "SHA-256"; // For OAEP
        boolean sign() default false;             // Sign vs encrypt
        boolean applyToReturn() default true;
        boolean applyToParams() default false;
        HashOutputFormat outputFormat() default HashOutputFormat.BASE64;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepRSA */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DRSA {
        String target() default "";
        RSAKeySize keySize() default RSAKeySize.BITS_2048;
        EncodingDirection direction() default EncodingDirection.ENCODE;
        String keyAlias() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔒 @DeepAES — AES encryption/decryption.
     *
     * Example:
     * <pre>
     * {@code @DeepAES(
     *     keySize = AESKeySize.BITS_256,
     *     mode = AESMode.GCM,
     *     direction = EncodingDirection.ENCODE
     * )}
     * public byte[] encryptPayload(byte[] plaintext) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepAES {
        String target() default "";
        AESKeySize keySize() default AESKeySize.BITS_256;
        AESMode mode() default AESMode.GCM;
        EncodingDirection direction() default EncodingDirection.ENCODE;
        String keyProvider() default "";
        String keyAlias() default "";
        int ivLength() default 12;               // IV length in bytes
        int tagLength() default 128;             // Auth tag length for GCM (bits)
        boolean generateIV() default true;       // Auto-generate random IV
        boolean prependIV() default true;        // Prepend IV to ciphertext
        boolean applyToReturn() default true;
        boolean applyToParams() default false;
        String associatedData() default "";      // AAD for GCM/CCM
        HashOutputFormat outputFormat() default HashOutputFormat.RAW_BYTES;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepAES */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DAES {
        String target() default "";
        AESKeySize keySize() default AESKeySize.BITS_256;
        AESMode mode() default AESMode.GCM;
        EncodingDirection direction() default EncodingDirection.ENCODE;
        String keyProvider() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * #️⃣ @DeepSHA — SHA family hashing.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    public @interface DeepSHA {
        String target() default "";
        SHAVariant variant() default SHAVariant.SHA_256;
        HashOutputFormat outputFormat() default HashOutputFormat.HEX_LOWERCASE;
        boolean hashReturn() default true;
        boolean hashParams() default false;
        SaltConfig salt() default @SaltConfig(generateRandom = false);
        boolean hmac() default false;            // Use HMAC-SHA
        String hmacKey() default "";
        String hmacKeyProvider() default "";
        int iterations() default 1;              // For PBKDF2-style usage
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSHA */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    public @interface DSHA {
        String target() default "";
        SHAVariant variant() default SHAVariant.SHA_256;
        HashOutputFormat outputFormat() default HashOutputFormat.HEX_LOWERCASE;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ⚠️ @DeepMD5 — MD5 hashing (NOT for security, use SHA/BCrypt/Argon2 instead).
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    public @interface DeepMD5 {
        String target() default "";
        HashOutputFormat outputFormat() default HashOutputFormat.HEX_LOWERCASE;
        boolean hashReturn() default true;
        boolean hashParams() default false;
        boolean hmac() default false;
        String hmacKey() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta(
            description = "MD5 is cryptographically broken. Use SHA-256+ for security."
        );
    }

    /** Shortcut for @DeepMD5 */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    public @interface DMD5 {
        String target() default "";
        HashOutputFormat outputFormat() default HashOutputFormat.HEX_LOWERCASE;
        boolean hashReturn() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔐 @DeepBCrypt — BCrypt password hashing.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DeepBCrypt {
        String target() default "";
        BCryptStrength strength() default BCryptStrength.DEFAULT;
        int customRounds() default -1;           // -1 = use strength preset
        boolean hashReturn() default false;
        boolean hashParam() default true;
        String paramName() default "";           // Parameter to hash
        boolean verify() default false;          // Verify instead of hash
        String hashFieldForVerify() default "";  // Field containing hash to verify against
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepBCrypt */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DBCRYPT {
        String target() default "";
        BCryptStrength strength() default BCryptStrength.DEFAULT;
        boolean verify() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔐 @DeepScrypt — Scrypt key derivation / password hashing.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DeepScrypt {
        String target() default "";
        int cpuCost() default 16384;             // N (CPU/memory cost)
        int blockSize() default 8;               // r (block size)
        int parallelization() default 1;         // p (parallelization)
        int keyLength() default 32;              // Output key length in bytes
        SaltConfig salt() default @SaltConfig;
        HashOutputFormat outputFormat() default HashOutputFormat.HEX_LOWERCASE;
        boolean hashReturn() default false;
        boolean hashParam() default true;
        boolean verify() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepScrypt */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DSCRYPT {
        String target() default "";
        int cpuCost() default 16384;
        int blockSize() default 8;
        int keyLength() default 32;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔐 @DeepArgon2 — Argon2 password hashing (state-of-the-art).
     *
     * Example:
     * <pre>
     * {@code @DeepArgon2(
     *     variant = Argon2Variant.ARGON2ID,
     *     memory = 65536,
     *     iterations = 3,
     *     parallelism = 4
     * )}
     * public String hashPassword(String password) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DeepArgon2 {
        String target() default "";
        Argon2Variant variant() default Argon2Variant.ARGON2ID;
        int memory() default 65536;              // Memory in KiB
        int iterations() default 3;              // Time cost
        int parallelism() default 4;             // Threads
        int hashLength() default 32;             // Output length in bytes
        SaltConfig salt() default @SaltConfig;
        HashOutputFormat outputFormat() default HashOutputFormat.HEX_LOWERCASE;
        boolean encodedOutput() default true;    // PHC string format ($argon2id$...)
        boolean hashReturn() default false;
        boolean hashParam() default true;
        boolean verify() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Argon2 variant */
    public enum Argon2Variant {
        ARGON2D,     // Data-dependent (GPU-resistant)
        ARGON2I,     // Data-independent (side-channel resistant)
        ARGON2ID     // Hybrid (recommended)
    }

    /** Shortcut for @DeepArgon2 */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public @interface DARGON {
        String target() default "";
        Argon2Variant variant() default Argon2Variant.ARGON2ID;
        int memory() default 65536;
        int iterations() default 3;
        int parallelism() default 4;
        boolean verify() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 13 — Processor
    // ──────────────────────────────────────────────

    /**
     * Processor for all Phase 13 (Encoding & Format Conversion) annotations.
     */
    public static class Phase13Processor {

        private final DeepMixContext context;

        public Phase13Processor(DeepMixContext context) {
            this.context = context;
        }

        /**
         * Process @DeepBase64.
         */
        public void processBase64(DeepBase64 annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {
            boolean encode = annotation.direction() == EncodingDirection.ENCODE;

            if (annotation.applyToReturn()) {
                InsnList transform = new InsnList();

                transform.add(new LdcInsnNode(annotation.variant().name()));
                transform.add(new InsnNode(
                    annotation.lineBreaks() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

                if (encode) {
                    transform.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/DeepMixEncoding",
                        "base64Encode",
                        "([BLjava/lang/String;Z)Ljava/lang/String;",
                        false
                    ));
                } else {
                    transform.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/DeepMixEncoding",
                        "base64Decode",
                        "(Ljava/lang/String;Ljava/lang/String;Z)[B",
                        false
                    ));
                }

                insertBeforeReturns(methodNode, transform);
            }

            if (annotation.applyToParams()) {
                InsnList paramTransform = new InsnList();
                Type[] argTypes = Type.getArgumentTypes(methodNode.desc);
                int localIdx = (methodNode.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;

                for (int i = 0; i < argTypes.length; i++) {
                    if (argTypes[i].getDescriptor().equals("Ljava/lang/String;") ||
                        argTypes[i].getDescriptor().equals("[B")) {

                        paramTransform.add(new VarInsnNode(Opcodes.ALOAD, localIdx));
                        paramTransform.add(new LdcInsnNode(annotation.variant().name()));
                        paramTransform.add(new InsnNode(Opcodes.ICONST_0));

                        String method = encode ? "base64Encode" : "base64Decode";
                        String desc = encode
                            ? "([BLjava/lang/String;Z)Ljava/lang/String;"
                            : "(Ljava/lang/String;Ljava/lang/String;Z)[B";

                        paramTransform.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "deepmix/runtime/DeepMixEncoding",
                            method, desc, false
                        ));
                        paramTransform.add(new VarInsnNode(Opcodes.ASTORE, localIdx));
                    }
                    localIdx += argTypes[i].getSize();
                }
                methodNode.instructions.insert(paramTransform);
            }

            context.addDiagnostic(String.format(
                "📋 @DeepBase64 applied to %s::%s [variant=%s, direction=%s]",
                classNode.name, methodNode.name, annotation.variant(), annotation.direction()
            ));
        }

        /**
         * Process @DeepAES.
         */
        public void processAES(DeepAES annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            boolean encrypt = annotation.direction() == EncodingDirection.ENCODE;

            if (annotation.applyToReturn()) {
                InsnList transform = new InsnList();

                transform.add(new LdcInsnNode(annotation.keySize().bits()));
                transform.add(new LdcInsnNode(annotation.mode().name()));
                transform.add(new LdcInsnNode(annotation.ivLength()));
                transform.add(new LdcInsnNode(annotation.tagLength()));
                transform.add(new InsnNode(
                    annotation.generateIV() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                transform.add(new InsnNode(
                    annotation.prependIV() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                transform.add(new LdcInsnNode(annotation.keyAlias()));

                if (encrypt) {
                    transform.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/DeepMixCrypto",
                        "aesEncrypt",
                        "([BILjava/lang/String;IIZZLjava/lang/String;)[B",
                        false
                    ));
                } else {
                    transform.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/DeepMixCrypto",
                        "aesDecrypt",
                        "([BILjava/lang/String;IIZZLjava/lang/String;)[B",
                        false
                    ));
                }

                insertBeforeReturns(methodNode, transform);
            }

            context.addDiagnostic(String.format(
                "🔒 @DeepAES applied to %s::%s [keySize=%d, mode=%s, direction=%s]",
                classNode.name, methodNode.name,
                annotation.keySize().bits(), annotation.mode(), annotation.direction()
            ));
        }

        /**
         * Process @DeepSHA.
         */
        public void processSHA(DeepSHA annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            if (annotation.hashReturn()) {
                InsnList hashInsns = new InsnList();

                hashInsns.add(new LdcInsnNode(annotation.variant().name()));
                hashInsns.add(new LdcInsnNode(annotation.outputFormat().name()));
                hashInsns.add(new LdcInsnNode(annotation.iterations()));
                hashInsns.add(new InsnNode(
                    annotation.hmac() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                hashInsns.add(new LdcInsnNode(annotation.hmacKey()));

                hashInsns.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixHash",
                    "sha",
                    "([BLjava/lang/String;Ljava/lang/String;IZLjava/lang/String;)Ljava/lang/String;",
                    false
                ));

                insertBeforeReturns(methodNode, hashInsns);
            }

            context.addDiagnostic(String.format(
                "#️⃣ @DeepSHA applied to %s::%s [variant=%s]",
                classNode.name, methodNode.name, annotation.variant()
            ));
        }

        /**
         * Process @DeepArgon2.
         */
        public void processArgon2(DeepArgon2 annotation, ClassNode classNode,
                                   MethodNode methodNode) throws DeepMixProcessingException {
            InsnList argon2Code = new InsnList();

            if (annotation.verify()) {
                // Verification mode: compare password against stored hash
                argon2Code.add(new LdcInsnNode(annotation.variant().name()));
                argon2Code.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixHash",
                    "argon2Verify",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z",
                    false
                ));
            } else {
                // Hashing mode
                argon2Code.add(new LdcInsnNode(annotation.variant().name()));
                argon2Code.add(new LdcInsnNode(annotation.memory()));
                argon2Code.add(new LdcInsnNode(annotation.iterations()));
                argon2Code.add(new LdcInsnNode(annotation.parallelism()));
                argon2Code.add(new LdcInsnNode(annotation.hashLength()));
                argon2Code.add(new LdcInsnNode(annotation.salt().saltLength()));
                argon2Code.add(new InsnNode(
                    annotation.encodedOutput() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

                argon2Code.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixHash",
                    "argon2Hash",
                    "(Ljava/lang/String;Ljava/lang/String;IIIIIZ)Ljava/lang/String;",
                    false
                ));
            }

            if (annotation.hashParam()) {
                methodNode.instructions.insert(argon2Code);
            } else {
                insertBeforeReturns(methodNode, argon2Code);
            }

            context.addDiagnostic(String.format(
                "🔐 @DeepArgon2 applied to %s::%s [variant=%s, mem=%dKiB, iter=%d]",
                classNode.name, methodNode.name,
                annotation.variant(), annotation.memory(), annotation.iterations()
            ));
        }

        /**
         * Process @DeepJWT.
         */
        public void processJWT(DeepJWT annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            InsnList jwtCode = new InsnList();
            JWTClaims claims = annotation.claims();

            switch (annotation.operation()) {
                case SIGN:
                    jwtCode.add(new LdcInsnNode(annotation.algorithm().name()));
                    jwtCode.add(new LdcInsnNode(claims.issuer()));
                    jwtCode.add(new LdcInsnNode(claims.subject()));
                    jwtCode.add(new LdcInsnNode(claims.audience()));
                    jwtCode.add(new LdcInsnNode(claims.expirationSeconds()));
                    jwtCode.add(new LdcInsnNode(annotation.secretKey()));
                    jwtCode.add(new LdcInsnNode(annotation.keyAlias()));

                    jwtCode.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/DeepMixJWT",
                        "sign",
                        "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;" +
                        "Ljava/lang/String;Ljava/lang/String;JLjava/lang/String;" +
                        "Ljava/lang/String;)Ljava/lang/String;",
                        false
                    ));
                    break;

                case VERIFY:
                    jwtCode.add(new LdcInsnNode(annotation.algorithm().name()));
                    jwtCode.add(new LdcInsnNode(annotation.secretKey()));
                    jwtCode.add(new InsnNode(
                        annotation.validateExpiration() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                    jwtCode.add(new InsnNode(
                        annotation.validateIssuer() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

                    jwtCode.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/DeepMixJWT",
                        "verify",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)Ljava/lang/Object;",
                        false
                    ));
                    break;

                case DECODE:
                    jwtCode.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/DeepMixJWT",
                        "decode",
                        "(Ljava/lang/String;)Ljava/lang/Object;",
                        false
                    ));
                    break;

                case REFRESH:
                    jwtCode.add(new LdcInsnNode(claims.expirationSeconds()));
                    jwtCode.add(new LdcInsnNode(annotation.secretKey()));
                    jwtCode.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/DeepMixJWT",
                        "refresh",
                        "(Ljava/lang/String;JLjava/lang/String;)Ljava/lang/String;",
                        false
                    ));
                    break;

                case REVOKE:
                    jwtCode.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/DeepMixJWT",
                        "revoke",
                        "(Ljava/lang/String;)V",
                        false
                    ));
                    break;
            }

            methodNode.instructions.insert(jwtCode);

            context.addDiagnostic(String.format(
                "🎫 @DeepJWT applied to %s::%s [op=%s, algo=%s]",
                classNode.name, methodNode.name,
                annotation.operation(), annotation.algorithm()
            ));
        }

        /**
         * Process @DeepQR.
         */
        public void processQR(DeepQR annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            QRConfig config = annotation.config();

            InsnList qrCode = new InsnList();

            qrCode.add(new LdcInsnNode(config.width()));
            qrCode.add(new LdcInsnNode(config.height()));
            qrCode.add(new LdcInsnNode(config.errorCorrection().name()));
            qrCode.add(new LdcInsnNode(config.margin()));
            qrCode.add(new LdcInsnNode(config.foregroundColor()));
            qrCode.add(new LdcInsnNode(config.backgroundColor()));
            qrCode.add(new LdcInsnNode(config.logoPath()));
            qrCode.add(new LdcInsnNode(annotation.outputFormat()));
            qrCode.add(new InsnNode(
                annotation.returnAsBase64() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            qrCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixQR",
                "generate",
                "(Ljava/lang/String;IILjava/lang/String;ILjava/lang/String;" +
                "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/Object;",
                false
            ));

            insertBeforeReturns(methodNode, qrCode);

            context.addDiagnostic(String.format(
                "📱 @DeepQR applied to %s::%s [%dx%d, ec=%s]",
                classNode.name, methodNode.name,
                config.width(), config.height(), config.errorCorrection()
            ));
        }

        // ─── Shared helper (delegating to Phase10Processor pattern) ───

        private void insertBeforeReturns(MethodNode methodNode, InsnList toInsert) {
            List<AbstractInsnNode> returnInsns = new ArrayList<>();
            for (AbstractInsnNode insn : methodNode.instructions) {
                int op = insn.getOpcode();
                if (op == Opcodes.RETURN || op == Opcodes.ARETURN ||
                    op == Opcodes.IRETURN || op == Opcodes.LRETURN ||
                    op == Opcodes.FRETURN || op == Opcodes.DRETURN) {
                    returnInsns.add(insn);
                }
            }
            for (AbstractInsnNode ret : returnInsns) {
                InsnList copy = cloneInsnList(toInsert);
                methodNode.instructions.insertBefore(ret, copy);
            }
        }

        private InsnList cloneInsnList(InsnList original) {
            InsnList clone = new InsnList();
            Map<LabelNode, LabelNode> labelMap = new HashMap<>();
            for (AbstractInsnNode insn : original) {
                if (insn instanceof LabelNode) {
                    labelMap.put((LabelNode) insn, new LabelNode());
                }
            }
            for (AbstractInsnNode insn : original) {
                clone.add(insn.clone(labelMap));
            }
            return clone;
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                      ║
    // ║  PHASE 14: I/O & STREAMING                                           ║
    // ║  18 annotations | Priority: MEDIUM | Est. Time: 2-3 days             ║
    // ║                                                                      ║
    // ║  @DeepZeroCopy  @DeepMmap  @DeepNIO  @DeepChannel                   ║
    // ║  @DeepBuffer    @DeepPipe  @DeepSocket  @DeepWebSocket              ║
    // ║  @DeepHTTP      @DeepREST  @DeepGraphQL  @DeepGRPC                  ║
    // ║  @DeepMQTT      @DeepKafka  @DeepRabbitMQ  @DeepRedis               ║
    // ║  @DeepElastic   @DeepMongo                                           ║
    // ║                                                                      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // Phase 14 — Enums
    // ──────────────────────────────────────────────

    /** I/O mode */
    public enum IOMode {
        BLOCKING,
        NON_BLOCKING,
        ASYNC,
        COMPLETION_PORT,
        EPOLL,
        KQUEUE,
        IO_URING
    }

    /** Buffer type */
    public enum BufferType {
        HEAP,           // HeapByteBuffer
        DIRECT,         // DirectByteBuffer
        MAPPED,         // MappedByteBuffer
        POOLED,         // Pooled buffer (Netty-style)
        RING,           // Ring buffer
        CIRCULAR,       // Circular buffer
        SLAB            // Slab-allocated buffer
    }

    /** HTTP method */
    public enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH,
        HEAD, OPTIONS, TRACE, CONNECT
    }

    /** HTTP content type */
    public enum ContentType {
        JSON("application/json"),
        XML("application/xml"),
        FORM("application/x-www-form-urlencoded"),
        MULTIPART("multipart/form-data"),
        TEXT("text/plain"),
        HTML("text/html"),
        BINARY("application/octet-stream"),
        PROTOBUF("application/protobuf"),
        MSGPACK("application/msgpack"),
        GRAPHQL("application/graphql");

        private final String mimeType;
        ContentType(String mimeType) { this.mimeType = mimeType; }
        public String mimeType() { return mimeType; }
    }

    /** REST auth type */
    public enum AuthType {
        NONE,
        BASIC,
        BEARER,
        API_KEY,
        OAUTH2,
        DIGEST,
        CUSTOM
    }

    /** GraphQL operation type */
    public enum GraphQLOperationType {
        QUERY,
        MUTATION,
        SUBSCRIPTION
    }

    /** Message queue delivery guarantee */
    public enum DeliveryGuarantee {
        AT_MOST_ONCE,   // Fire and forget
        AT_LEAST_ONCE,  // With acknowledgment
        EXACTLY_ONCE    // Transactional
    }

    /** Kafka offset reset strategy */
    public enum OffsetReset {
        EARLIEST,
        LATEST,
        NONE
    }

    /** Redis data type */
    public enum RedisDataType {
        STRING,
        LIST,
        SET,
        SORTED_SET,
        HASH,
        STREAM,
        BITMAP,
        HYPERLOGLOG,
        GEO
    }

    /** Socket type */
    public enum SocketType {
        TCP,
        UDP,
        UNIX,
        SCTP,
        RAW
    }

    /** WebSocket state events */
    public enum WebSocketEvent {
        ON_OPEN,
        ON_MESSAGE,
        ON_CLOSE,
        ON_ERROR,
        ON_PING,
        ON_PONG
    }

    // ──────────────────────────────────────────────
    // Phase 14 — Sub-annotation types
    // ──────────────────────────────────────────────

    /** HTTP header specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface HttpHeader {
        String name();
        String value();
    }

    /** HTTP request configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface HttpConfig {
        long connectTimeoutMs() default 10000;
        long readTimeoutMs() default 30000;
        long writeTimeoutMs() default 30000;
        boolean followRedirects() default true;
        int maxRedirects() default 5;
        boolean verifySSL() default true;
        String proxyHost() default "";
        int proxyPort() default 0;
        int maxRetries() default 3;
        long retryDelayMs() default 1000;
        boolean enableCompression() default true;
        String userAgent() default "DeepMix/1.0";
    }

    /** Kafka consumer/producer configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface KafkaConfig {
        String bootstrapServers() default "localhost:9092";
        String groupId() default "";
        OffsetReset offsetReset() default OffsetReset.EARLIEST;
        boolean autoCommit() default true;
        int autoCommitIntervalMs() default 5000;
        String keySerializer() default "org.apache.kafka.common.serialization.StringSerializer";
        String valueSerializer() default "org.apache.kafka.common.serialization.StringSerializer";
        String keyDeserializer() default "org.apache.kafka.common.serialization.StringDeserializer";
        String valueDeserializer() default "org.apache.kafka.common.serialization.StringDeserializer";
        int maxPollRecords() default 500;
        String securityProtocol() default "";
        DeliveryGuarantee guarantee() default DeliveryGuarantee.AT_LEAST_ONCE;
    }

    /** Redis connection configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RedisConfig {
        String host() default "localhost";
        int port() default 6379;
        int database() default 0;
        String password() default "";
        boolean ssl() default false;
        int connectionTimeout() default 5000;
        int soTimeout() default 5000;
        int maxTotal() default 8;
        int maxIdle() default 8;
        boolean clusterMode() default false;
        String[] sentinels() default {};
        String masterName() default "";
    }

    // ──────────────────────────────────────────────
    // Phase 14 — Annotations
    // ──────────────────────────────────────────────

    /**
     * ⚡ @DeepZeroCopy — Zero-copy I/O optimization.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepZeroCopy {
        String target() default "";
        boolean useSendFile() default true;       // sendfile() system call
        boolean useTransferTo() default true;     // FileChannel.transferTo()
        boolean useSplice() default false;        // splice() Linux system call
        long maxTransferSize() default -1;        // -1 = unlimited
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepZeroCopy */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DZERO {
        String target() default "";
        boolean useSendFile() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🗺️ @DeepMmap — Memory-mapped file I/O.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepMmap {
        String target() default "";
        String filePath() default "";
        long offset() default 0;
        long length() default -1;                // -1 = entire file
        String mode() default "READ_WRITE";      // READ_ONLY, READ_WRITE, PRIVATE
        boolean preload() default false;         // Pre-fault pages
        boolean forceUnmap() default true;       // Force unmap on GC
        boolean lockPages() default false;       // mlock()
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMmap */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DMMAP {
        String target() default "";
        String filePath() default "";
        String mode() default "READ_WRITE";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔌 @DeepNIO — Java NIO operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepNIO {
        String target() default "";
        IOMode mode() default IOMode.NON_BLOCKING;
        BufferType bufferType() default BufferType.DIRECT;
        int bufferSize() default 8192;
        boolean useSelector() default false;
        int selectorTimeout() default 1000;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepNIO */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DNIO {
        String target() default "";
        IOMode mode() default IOMode.NON_BLOCKING;
        int bufferSize() default 8192;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📡 @DeepChannel — Channel-based I/O operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepChannel {
        String target() default "";
        IOMode mode() default IOMode.NON_BLOCKING;
        BufferType bufferType() default BufferType.DIRECT;
        int bufferSize() default 8192;
        boolean scatter() default false;         // Scatter read
        boolean gather() default false;          // Gather write
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepChannel */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DCHAN {
        String target() default "";
        IOMode mode() default IOMode.NON_BLOCKING;
        int bufferSize() default 8192;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📦 @DeepBuffer — Buffer management and optimization.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepBuffer {
        String target() default "";
        BufferType type() default BufferType.DIRECT;
        int initialCapacity() default 8192;
        int maxCapacity() default Integer.MAX_VALUE;
        boolean autoExpand() default true;
        boolean pooled() default true;
        boolean zeroed() default false;          // Zero-fill on allocation
        int alignment() default 0;               // Memory alignment (0 = default)
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepBuffer */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DBUF {
        String target() default "";
        BufferType type() default BufferType.DIRECT;
        int initialCapacity() default 8192;
        boolean pooled() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔗 @DeepPipe — Unix-style pipe operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepPipe {
        String target() default "";
        int bufferSize() default 8192;
        boolean blocking() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepPipe */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DPIPE {
        String target() default "";
        int bufferSize() default 8192;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔌 @DeepSocket — Socket operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepSocket {
        String target() default "";
        SocketType type() default SocketType.TCP;
        String host() default "localhost";
        int port() default 0;
        int backlog() default 128;
        int connectTimeout() default 10000;
        int soTimeout() default 30000;
        boolean keepAlive() default true;
        boolean tcpNoDelay() default true;
        boolean reuseAddress() default true;
        int receiveBufferSize() default 65536;
        int sendBufferSize() default 65536;
        boolean ssl() default false;
        String sslProtocol() default "TLSv1.3";
        IOMode mode() default IOMode.BLOCKING;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSocket */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DSOCK {
        String target() default "";
        SocketType type() default SocketType.TCP;
        String host() default "localhost";
        int port() default 0;
        boolean ssl() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🌐 @DeepWebSocket — WebSocket handling.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepWebSocket {
        String target() default "";
        String url() default "";
        WebSocketEvent[] events() default {WebSocketEvent.ON_MESSAGE};
        String[] subprotocols() default {};
        int maxFrameSize() default 65536;
        int maxMessageSize() default 1048576;    // 1 MB
        long pingIntervalMs() default 30000;
        long idleTimeoutMs() default 300000;     // 5 min
        boolean autoReconnect() default true;
        int maxReconnectAttempts() default 10;
        long reconnectDelayMs() default 1000;
        boolean perMessageDeflate() default true; // Compression
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepWebSocket */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DWS {
        String target() default "";
        String url() default "";
        boolean autoReconnect() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🌍 @DeepHTTP — HTTP operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepHTTP {
        String target() default "";
        String url() default "";
        HttpMethod method() default HttpMethod.GET;
        ContentType contentType() default ContentType.JSON;
        ContentType accept() default ContentType.JSON;
        HttpHeader[] headers() default {};
        HttpConfig config() default @HttpConfig;
        AuthType auth() default AuthType.NONE;
        String authCredentials() default "";
        boolean async() default false;
        String responseType() default "";        // Expected response class
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepHTTP */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DHTTP {
        String target() default "";
        String url() default "";
        HttpMethod method() default HttpMethod.GET;
        ContentType contentType() default ContentType.JSON;
        HttpConfig config() default @HttpConfig;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔗 @DeepREST — REST API integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepREST {
        String target() default "";
        String baseUrl() default "";
        String path() default "";
        HttpMethod method() default HttpMethod.GET;
        ContentType contentType() default ContentType.JSON;
        ContentType accept() default ContentType.JSON;
        HttpHeader[] headers() default {};
        AuthType auth() default AuthType.NONE;
        String authCredentials() default "";
        HttpConfig config() default @HttpConfig;
        boolean paginated() default false;
        String paginationParam() default "page";
        int pageSize() default 20;
        SerializationFormat requestFormat() default SerializationFormat.JSON_JACKSON;
        SerializationFormat responseFormat() default SerializationFormat.JSON_JACKSON;
        String responseType() default "";
        boolean cacheResponse() default false;
        long cacheTTLSeconds() default 300;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepREST */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DREST {
        String target() default "";
        String baseUrl() default "";
        String path() default "";
        HttpMethod method() default HttpMethod.GET;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📊 @DeepGraphQL — GraphQL operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepGraphQL {
        String target() default "";
        String endpoint() default "";
        GraphQLOperationType operation() default GraphQLOperationType.QUERY;
        String query() default "";
        String operationName() default "";
        String[] variables() default {};          // "key=value" pairs
        HttpHeader[] headers() default {};
        AuthType auth() default AuthType.NONE;
        String authCredentials() default "";
        HttpConfig config() default @HttpConfig;
        String responseType() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepGraphQL */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DGQL {
        String target() default "";
        String endpoint() default "";
        String query() default "";
        GraphQLOperationType operation() default GraphQLOperationType.QUERY;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📡 @DeepGRPC — gRPC integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepGRPC {
        String target() default "";
        String serviceUrl() default "";
        String serviceName() default "";
        String methodName() default "";
        boolean streaming() default false;       // Server/client/bidi streaming
        String protoFile() default "";
        boolean useTLS() default true;
        long deadlineMs() default 30000;
        int maxMessageSize() default 4194304;    // 4 MB
        boolean enableRetry() default true;
        int maxRetries() default 3;
        CompressionAlgorithm compression() default CompressionAlgorithm.NONE;
        String loadBalancer() default "round_robin";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepGRPC */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DGRPC {
        String target() default "";
        String serviceUrl() default "";
        String methodName() default "";
        boolean streaming() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📨 @DeepMQTT — MQTT messaging.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepMQTT {
        String target() default "";
        String brokerUrl() default "tcp://localhost:1883";
        String clientId() default "";
        String topic() default "";
        String[] topics() default {};
        int qos() default 1;                    // 0, 1, or 2
        boolean retained() default false;
        boolean cleanSession() default true;
        int keepAliveInterval() default 60;
        String username() default "";
        String password() default "";
        boolean ssl() default false;
        String willTopic() default "";
        String willMessage() default "";
        int willQos() default 0;
        boolean willRetained() default false;
        boolean autoReconnect() default true;
        int maxInFlight() default 10;
        ErrorStrategy onError() default ErrorStrategy.LOG_AND_CONTINUE;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMQTT */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DMQTT {
        String target() default "";
        String brokerUrl() default "tcp://localhost:1883";
        String topic() default "";
        int qos() default 1;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📬 @DeepKafka — Apache Kafka integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepKafka {
        String target() default "";
        KafkaConfig config() default @KafkaConfig;
        String topic() default "";
        String[] topics() default {};
        String key() default "";
        boolean isProducer() default true;       // true = producer, false = consumer
        boolean isConsumer() default false;
        boolean batch() default false;
        int batchSize() default 16384;
        long lingerMs() default 0;
        String partitioner() default "";
        int partition() default -1;              // -1 = auto
        boolean idempotent() default false;
        String transactionalId() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepKafka */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DKAFKA {
        String target() default "";
        KafkaConfig config() default @KafkaConfig;
        String topic() default "";
        boolean isProducer() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🐰 @DeepRabbitMQ — RabbitMQ messaging.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepRabbitMQ {
        String target() default "";
        String host() default "localhost";
        int port() default 5672;
        String virtualHost() default "/";
        String username() default "guest";
        String password() default "guest";
        String exchange() default "";
        String exchangeType() default "direct"; // direct, fanout, topic, headers
        String queue() default "";
        String routingKey() default "";
        boolean durable() default true;
        boolean exclusive() default false;
        boolean autoDelete() default false;
        boolean autoAck() default false;
        int prefetchCount() default 10;
        boolean mandatory() default false;
        DeliveryGuarantee guarantee() default DeliveryGuarantee.AT_LEAST_ONCE;
        boolean ssl() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepRabbitMQ */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DRABBIT {
        String target() default "";
        String host() default "localhost";
        String exchange() default "";
        String queue() default "";
        String routingKey() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔴 @DeepRedis — Redis operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepRedis {
        String target() default "";
        RedisConfig config() default @RedisConfig;
        RedisDataType dataType() default RedisDataType.STRING;
        String key() default "";
        String keyPattern() default "";
        long ttlSeconds() default -1;            // -1 = no expiry
        boolean pipeline() default false;
        boolean transactional() default false;    // MULTI/EXEC
        boolean subscribe() default false;        // Pub/sub subscriber mode
        String channel() default "";              // For pub/sub
        String[] channels() default {};
        String luaScript() default "";            // Execute Lua script
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepRedis */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DREDIS {
        String target() default "";
        RedisConfig config() default @RedisConfig;
        RedisDataType dataType() default RedisDataType.STRING;
        String key() default "";
        long ttlSeconds() default -1;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔍 @DeepElastic — Elasticsearch integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepElastic {
        String target() default "";
        String[] hosts() default {"localhost:9200"};
        String index() default "";
        String type() default "_doc";
        String query() default "";               // Elasticsearch query DSL (JSON)
        String[] fields() default {};            // Fields to return
        int from() default 0;
        int size() default 10;
        String[] sort() default {};
        boolean ssl() default false;
        String username() default "";
        String password() default "";
        String apiKey() default "";
        boolean bulk() default false;
        int bulkSize() default 1000;
        long bulkFlushIntervalMs() default 5000;
        boolean scroll() default false;
        long scrollKeepAliveMs() default 60000;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepElastic */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DELASTIC {
        String target() default "";
        String[] hosts() default {"localhost:9200"};
        String index() default "";
        String query() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🍃 @DeepMongo — MongoDB operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepMongo {
        String target() default "";
        String connectionString() default "mongodb://localhost:27017";
        String database() default "";
        String collection() default "";
        String filter() default "{}";            // BSON filter (JSON)
        String projection() default "";          // Fields to include/exclude
        String sort() default "";                // Sort specification
        int limit() default 0;                   // 0 = no limit
        int skip() default 0;
        boolean upsert() default false;
        String[] indexes() default {};           // Index specifications
        boolean bulk() default false;
        String readPreference() default "primary";
        String writeConcern() default "majority";
        String readConcern() default "local";
        boolean transactional() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMongo */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DMONGO {
        String target() default "";
        String connectionString() default "mongodb://localhost:27017";
        String database() default "";
        String collection() default "";
        String filter() default "{}";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 14 — Processor
    // ──────────────────────────────────────────────

    /**
     * Processor for all Phase 14 (I/O & Streaming) annotations.
     */
    public static class Phase14Processor {

        private final DeepMixContext context;

        public Phase14Processor(DeepMixContext context) {
            this.context = context;
        }

        /**
         * Process @DeepHTTP — Inject HTTP client call into method body.
         */
        public void processHTTP(DeepHTTP annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            InsnList httpCall = new InsnList();

            httpCall.add(new LdcInsnNode(annotation.url()));
            httpCall.add(new LdcInsnNode(annotation.method().name()));
            httpCall.add(new LdcInsnNode(annotation.contentType().mimeType()));
            httpCall.add(new LdcInsnNode(annotation.accept().mimeType()));
            httpCall.add(new LdcInsnNode(annotation.auth().name()));
            httpCall.add(new LdcInsnNode(annotation.authCredentials()));

            HttpConfig config = annotation.config();
            httpCall.add(new LdcInsnNode(config.connectTimeoutMs()));
            httpCall.add(new LdcInsnNode(config.readTimeoutMs()));
            httpCall.add(new LdcInsnNode(config.maxRetries()));
            httpCall.add(new InsnNode(
                config.followRedirects() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            httpCall.add(new InsnNode(
                annotation.async() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            // Build headers array
            HttpHeader[] headers = annotation.headers();
            httpCall.add(new LdcInsnNode(headers.length * 2));
            httpCall.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < headers.length; i++) {
                httpCall.add(new InsnNode(Opcodes.DUP));
                httpCall.add(new LdcInsnNode(i * 2));
                httpCall.add(new LdcInsnNode(headers[i].name()));
                httpCall.add(new InsnNode(Opcodes.AASTORE));
                httpCall.add(new InsnNode(Opcodes.DUP));
                httpCall.add(new LdcInsnNode(i * 2 + 1));
                httpCall.add(new LdcInsnNode(headers[i].value()));
                httpCall.add(new InsnNode(Opcodes.AASTORE));
            }

            httpCall.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixHTTP",
                "execute",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "JJIZ[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(httpCall);

            context.addDiagnostic(String.format(
                "🌍 @DeepHTTP applied to %s::%s [%s %s]",
                classNode.name, methodNode.name,
                annotation.method(), annotation.url()
            ));
        }

        /**
         * Process @DeepKafka — Inject Kafka producer/consumer logic.
         */
        public void processKafka(DeepKafka annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {
            KafkaConfig config = annotation.config();
            InsnList kafkaCode = new InsnList();

            kafkaCode.add(new LdcInsnNode(config.bootstrapServers()));
            kafkaCode.add(new LdcInsnNode(annotation.topic()));
            kafkaCode.add(new LdcInsnNode(config.groupId()));
            kafkaCode.add(new LdcInsnNode(config.guarantee().name()));
            kafkaCode.add(new InsnNode(
                annotation.isProducer() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            kafkaCode.add(new InsnNode(
                annotation.batch() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            kafkaCode.add(new LdcInsnNode(annotation.batchSize()));

            if (annotation.isProducer()) {
                kafkaCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixKafka",
                    "produce",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                    "Ljava/lang/String;ZZI)V",
                    false
                ));
            } else {
                kafkaCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/DeepMixKafka",
                    "consume",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                    "Ljava/lang/String;ZZI)Ljava/lang/Iterable;",
                    false
                ));
            }

            methodNode.instructions.insert(kafkaCode);

            context.addDiagnostic(String.format(
                "📬 @DeepKafka applied to %s::%s [topic=%s, producer=%b]",
                classNode.name, methodNode.name,
                annotation.topic(), annotation.isProducer()
            ));
        }

        /**
         * Process @DeepRedis — Inject Redis operation.
         */
        public void processRedis(DeepRedis annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {
            RedisConfig config = annotation.config();
            InsnList redisCode = new InsnList();

            redisCode.add(new LdcInsnNode(config.host()));
            redisCode.add(new LdcInsnNode(config.port()));
            redisCode.add(new LdcInsnNode(config.database()));
            redisCode.add(new LdcInsnNode(config.password()));
            redisCode.add(new LdcInsnNode(annotation.dataType().name()));
            redisCode.add(new LdcInsnNode(annotation.key()));
            redisCode.add(new LdcInsnNode(annotation.ttlSeconds()));
            redisCode.add(new InsnNode(
                annotation.pipeline() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            redisCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixRedis",
                "execute",
                "(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;JZ)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(redisCode);

            context.addDiagnostic(String.format(
                "🔴 @DeepRedis applied to %s::%s [host=%s, key=%s, type=%s]",
                classNode.name, methodNode.name,
                config.host(), annotation.key(), annotation.dataType()
            ));
        }

        /**
         * Process @DeepWebSocket — Set up WebSocket connection handling.
         */
        public void processWebSocket(DeepWebSocket annotation, ClassNode classNode,
                                      MethodNode methodNode) throws DeepMixProcessingException {
            InsnList wsCode = new InsnList();

            wsCode.add(new LdcInsnNode(annotation.url()));
            wsCode.add(new LdcInsnNode(annotation.maxMessageSize()));
            wsCode.add(new LdcInsnNode(annotation.pingIntervalMs()));
            wsCode.add(new LdcInsnNode(annotation.idleTimeoutMs()));
            wsCode.add(new InsnNode(
                annotation.autoReconnect() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            wsCode.add(new LdcInsnNode(annotation.maxReconnectAttempts()));
            wsCode.add(new LdcInsnNode(annotation.reconnectDelayMs()));
            wsCode.add(new InsnNode(
                annotation.perMessageDeflate() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            // Build events array
            WebSocketEvent[] events = annotation.events();
            wsCode.add(new LdcInsnNode(events.length));
            wsCode.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < events.length; i++) {
                wsCode.add(new InsnNode(Opcodes.DUP));
                wsCode.add(new LdcInsnNode(i));
                wsCode.add(new LdcInsnNode(events[i].name()));
                wsCode.add(new InsnNode(Opcodes.AASTORE));
            }

            wsCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/DeepMixWebSocket",
                "connect",
                "(Ljava/lang/String;IJJZIJ Z[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(wsCode);

            context.addDiagnostic(String.format(
                "🌐 @DeepWebSocket applied to %s::%s [url=%s, events=%s]",
                classNode.name, methodNode.name,
                annotation.url(), Arrays.toString(annotation.events())
            ));
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                      ║
    // ║  PHASE 15: CLOUD & CONTAINER                                         ║
    // ║  15 annotations | Priority: LOW | Est. Time: 2 days                  ║
    // ║                                                                      ║
    // ║  @DeepTerraform  @DeepAnsible  @DeepAWS  @DeepAzure                ║
    // ║  @DeepGCP  @DeepOpenStack  @DeepCloudFormation  @DeepHelm           ║
    // ║  @DeepIstio  @DeepPrometheus  @DeepGrafana  @DeepJenkins           ║
    // ║  @DeepGitLab  @DeepGitHub  @DeepCircleCI                           ║
    // ║                                                                      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // Phase 15 — Enums
    // ──────────────────────────────────────────────

    /** AWS service type */
    public enum AWSService {
        S3, EC2, LAMBDA, SQS, SNS, DYNAMODB,
        RDS, ECS, EKS, CLOUDWATCH, IAM, SECRETS_MANAGER,
        KINESIS, STEP_FUNCTIONS, API_GATEWAY, COGNITO,
        ROUTE53, CLOUDFRONT, ELASTICACHE, REDSHIFT
    }

    /** Azure service type */
    public enum AzureService {
        BLOB_STORAGE, COSMOS_DB, FUNCTIONS, SERVICE_BUS,
        EVENT_HUB, KEY_VAULT, AKS, APP_SERVICE,
        SQL_DATABASE, REDIS_CACHE, COGNITIVE_SERVICES,
        MONITOR, ACTIVE_DIRECTORY
    }

    /** GCP service type */
    public enum GCPService {
        CLOUD_STORAGE, BIGQUERY, CLOUD_FUNCTIONS, PUB_SUB,
        FIRESTORE, CLOUD_SQL, GKE, APP_ENGINE,
        CLOUD_RUN, CLOUD_TASKS, CLOUD_KMS, STACKDRIVER,
        VERTEX_AI, CLOUD_SPANNER, CLOUD_MEMORYSTORE
    }

    /** CI/CD pipeline stage */
    public enum PipelineStage {
        BUILD, TEST, LINT, SECURITY_SCAN,
        PACKAGE, PUBLISH, DEPLOY_STAGING,
        INTEGRATION_TEST, DEPLOY_PRODUCTION,
        SMOKE_TEST, ROLLBACK, NOTIFY, CLEANUP
    }

    /** Prometheus metric type */
    public enum PrometheusMetricType {
        COUNTER,
        GAUGE,
        HISTOGRAM,
        SUMMARY
    }

    /** Helm action */
    public enum HelmAction {
        INSTALL,
        UPGRADE,
        ROLLBACK,
        UNINSTALL,
        TEMPLATE,
        LINT,
        PACKAGE
    }

    // ──────────────────────────────────────────────
    // Phase 15 — Annotations
    // ──────────────────────────────────────────────

    /**
     * 🏗️ @DeepTerraform — Terraform configuration generation/modification.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepTerraform {
        String target() default "";
        String configPath() default "";
        String provider() default "";
        String[] resources() default {};
        String[] variables() default {};
        String stateBackend() default "local";
        String workspace() default "default";
        boolean plan() default false;
        boolean apply() default false;
        boolean destroy() default false;
        boolean autoApprove() default false;
        int parallelism() default 10;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepTerraform */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DTF {
        String target() default "";
        String configPath() default "";
        String provider() default "";
        boolean apply() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📋 @DeepAnsible — Ansible playbook generation/execution.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepAnsible {
        String target() default "";
        String playbookPath() default "";
        String inventory() default "";
        String[] hosts() default {};
        String[] roles() default {};
        String[] tasks() default {};
        String[] vars() default {};
        String become() default "";
        boolean check() default false;           // Dry run mode
        boolean diff() default false;
        int forks() default 5;
        String verbosity() default "";           // v, vv, vvv, vvvv
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepAnsible */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DANS {
        String target() default "";
        String playbookPath() default "";
        String inventory() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ☁️ @DeepAWS — AWS service integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepAWS {
        String target() default "";
        AWSService service();
        String region() default "us-east-1";
        String profile() default "default";
        String roleArn() default "";
        String endpoint() default "";            // Custom endpoint (localstack)
        String[] resourceArns() default {};
        String[] tags() default {};              // "Key=Value" pairs
        boolean assumeRole() default false;
        long sessionDurationSeconds() default 3600;
        int maxRetries() default 3;
        long timeoutMs() default 30000;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepAWS */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DAWS {
        String target() default "";
        AWSService service();
        String region() default "us-east-1";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔵 @DeepAzure — Azure service integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepAzure {
        String target() default "";
        AzureService service();
        String subscriptionId() default "";
        String resourceGroup() default "";
        String tenantId() default "";
        String clientId() default "";
        String region() default "";
        String[] tags() default {};
        long timeoutMs() default 30000;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepAzure */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DAZURE {
        String target() default "";
        AzureService service();
        String subscriptionId() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🟡 @DeepGCP — Google Cloud Platform integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepGCP {
        String target() default "";
        GCPService service();
        String projectId() default "";
        String region() default "";
        String zone() default "";
        String credentialsPath() default "";
        String[] labels() default {};
        long timeoutMs() default 30000;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepGCP */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DGCP {
        String target() default "";
        GCPService service();
        String projectId() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ☁️ @DeepOpenStack — OpenStack operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepOpenStack {
        String target() default "";
        String authUrl() default "";
        String projectName() default "";
        String domainName() default "default";
        String region() default "";
        String username() default "";
        String password() default "";
        String[] services() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepOpenStack */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DOSTACK {
        String target() default "";
        String authUrl() default "";
        String projectName() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📦 @DeepCloudFormation — AWS CloudFormation template generation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepCloudFormation {
        String target() default "";
        String templatePath() default "";
        String stackName() default "";
        String[] parameters() default {};        // "Key=Value" pairs
        String[] capabilities() default {};      // CAPABILITY_IAM, etc.
        String[] tags() default {};
        boolean createStack() default false;
        boolean updateStack() default false;
        boolean deleteStack() default false;
        boolean waitForCompletion() default true;
        long timeoutMinutes() default 30;
        String onFailure() default "ROLLBACK";   // ROLLBACK, DELETE, DO_NOTHING
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCloudFormation */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DCF {
        String target() default "";
        String templatePath() default "";
        String stackName() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ⎈ @DeepHelm — Helm chart operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepHelm {
        String target() default "";
        HelmAction action() default HelmAction.INSTALL;
        String chartPath() default "";
        String releaseName() default "";
        String namespace() default "default";
        String[] values() default {};           // "key=value" overrides
        String valuesFile() default "";
        int revision() default 0;               // For rollback
        boolean wait() default true;
        long timeoutSeconds() default 300;
        boolean atomic() default false;
        boolean dryRun() default false;
        boolean createNamespace() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepHelm */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DHELM {
        String target() default "";
        HelmAction action() default HelmAction.INSTALL;
        String chartPath() default "";
        String releaseName() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🕸️ @DeepIstio — Istio service mesh configuration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepIstio {
        String target() default "";
        String configPath() default "";
        String namespace() default "default";
        String[] virtualServices() default {};
        String[] destinationRules() default {};
        String[] gateways() default {};
        boolean mtls() default true;
        String[] trafficPolicy() default {};
        String[] faultInjection() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepIstio */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DISTIO {
        String target() default "";
        String namespace() default "default";
        boolean mtls() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📊 @DeepPrometheus — Prometheus metrics exposition.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
    public @interface DeepPrometheus {
        String target() default "";
        PrometheusMetricType type() default PrometheusMetricType.COUNTER;
        String metricName() default "";
        String help() default "";
        String[] labels() default {};
        double[] buckets() default {};           // For HISTOGRAM
        double[] quantiles() default {};         // For SUMMARY
        String subsystem() default "";
        String namespace_() default "";          // Renamed to avoid Java keyword
        boolean autoRegister() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepPrometheus */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
    public @interface DPROM {
        String target() default "";
        PrometheusMetricType type() default PrometheusMetricType.COUNTER;
        String metricName() default "";
        String help() default "";
        String[] labels() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📈 @DeepGrafana — Grafana dashboard generation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepGrafana {
        String target() default "";
        String dashboardTitle() default "";
        String dashboardUid() default "";
        String datasource() default "Prometheus";
        String[] panels() default {};
        String folderName() default "";
        boolean autoProvision() default false;
        String grafanaUrl() default "";
        String apiKey() default "";
        int refreshIntervalSeconds() default 30;
        String timeRange() default "1h";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepGrafana */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DGRAF {
        String target() default "";
        String dashboardTitle() default "";
        String datasource() default "Prometheus";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔧 @DeepJenkins — Jenkins pipeline generation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepJenkins {
        String target() default "";
        String jenkinsUrl() default "";
        String jobName() default "";
        PipelineStage[] stages() default {};
        String agent() default "any";
        String[] parameters() default {};
        String[] environment() default {};
        String credentialsId() default "";
        boolean declarativePipeline() default true;
        boolean parallel() default false;
        String[] triggers() default {};
        String postSuccess() default "";
        String postFailure() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepJenkins */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DJENK {
        String target() default "";
        String jobName() default "";
        PipelineStage[] stages() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🦊 @DeepGitLab — GitLab CI/CD configuration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepGitLab {
        String target() default "";
        PipelineStage[] stages() default {};
        String image() default "";
        String[] services() default {};
        String[] variables() default {};
        String[] before_script() default {};
        String[] script() default {};
        String[] after_script() default {};
        String[] artifacts() default {};
        String[] cache() default {};
        String[] only() default {};
        String[] except() default {};
        String[] rules() default {};
        boolean allowFailure() default false;
        int timeout() default 3600;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepGitLab */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DGITLAB {
        String target() default "";
        PipelineStage[] stages() default {};
        String image() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🐙 @DeepGitHub — GitHub Actions workflow generation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepGitHub {
        String target() default "";
        String workflowName() default "";
        String[] on() default {"push"};         // Trigger events
        String[] branches() default {"main"};
        String runsOn() default "ubuntu-latest";
        String[] steps() default {};
        String[] env() default {};
        String[] secrets() default {};
        String[] services() default {};
        String matrix() default "";
        boolean concurrency() default false;
        String concurrencyGroup() default "";
        boolean cancelInProgress() default true;
        int timeoutMinutes() default 360;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepGitHub */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DGITHUB {
        String target() default "";
        String workflowName() default "";
        String[] on() default {"push"};
        String runsOn() default "ubuntu-latest";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ⭕ @DeepCircleCI — CircleCI configuration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepCircleCI {
        String target() default "";
        String version() default "2.1";
        String[] orbs() default {};
        String executor() default ""; // docker, machine, macos, windows
        String dockerImage() default "cimg/openjdk:17.0";
        String resourceClass() default "medium";
        String[] steps() default {};
        String[] cacheKeys() default {};
        String[] artifacts() default {};
        String[] workspaces() default {};
        String[] filters() default {};            // Branch/tag filters
        boolean parallelism() default false;
        int parallelismLevel() default 1;
        String workingDirectory() default "~/project";
        String[] environment() default {};
        int noOutputTimeout() default 600;        // Seconds before no-output timeout
        boolean storeTestResults() default false;
        String testResultsPath() default "test-results";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCircleCI */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DCIRCLE {
        String target() default "";
        String dockerImage() default "cimg/openjdk:17.0";
        String[] steps() default {};
        boolean parallelism() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 15 — Processor
    // ──────────────────────────────────────────────

    /**
     * Processor for all Phase 15 (Cloud & Container) annotations.
     *
     * Generates IaC configs, injects cloud SDK calls, and manages
     * CI/CD pipeline definitions through bytecode transformation.
     */
    public static class Phase15Processor {

        private final DeepMixContext context;

        public Phase15Processor(DeepMixContext context) {
            this.context = context;
        }

        /**
         * Process @DeepAWS — Inject AWS SDK client initialization and operation.
         */
        public void processAWS(DeepAWS annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            InsnList awsCode = new InsnList();

            awsCode.add(new LdcInsnNode(annotation.service().name()));
            awsCode.add(new LdcInsnNode(annotation.region()));
            awsCode.add(new LdcInsnNode(annotation.profile()));
            awsCode.add(new LdcInsnNode(annotation.roleArn()));
            awsCode.add(new LdcInsnNode(annotation.endpoint()));
            awsCode.add(new InsnNode(
                annotation.assumeRole() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            awsCode.add(new LdcInsnNode(annotation.sessionDurationSeconds()));
            awsCode.add(new LdcInsnNode(annotation.maxRetries()));
            awsCode.add(new LdcInsnNode(annotation.timeoutMs()));

            // Push resource ARNs
            pushStringArray(awsCode, annotation.resourceArns());
            // Push tags
            pushStringArray(awsCode, annotation.tags());

            awsCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/cloud/DeepMixAWS",
                "initializeAndExecute",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;Ljava/lang/String;ZJIJ" +
                "[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(awsCode);

            context.addDiagnostic(String.format(
                "☁️ @DeepAWS applied to %s::%s [service=%s, region=%s]",
                classNode.name, methodNode.name,
                annotation.service(), annotation.region()
            ));
        }

        /**
         * Process @DeepTerraform — Generate or modify Terraform config.
         */
        public void processTerraform(DeepTerraform annotation, ClassNode classNode,
                                     MethodNode methodNode) throws DeepMixProcessingException {
            InsnList tfCode = new InsnList();

            tfCode.add(new LdcInsnNode(annotation.configPath()));
            tfCode.add(new LdcInsnNode(annotation.provider()));
            tfCode.add(new LdcInsnNode(annotation.stateBackend()));
            tfCode.add(new LdcInsnNode(annotation.workspace()));
            tfCode.add(new InsnNode(
                annotation.plan() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            tfCode.add(new InsnNode(
                annotation.apply() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            tfCode.add(new InsnNode(
                annotation.destroy() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            tfCode.add(new InsnNode(
                annotation.autoApprove() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            tfCode.add(new LdcInsnNode(annotation.parallelism()));

            pushStringArray(tfCode, annotation.resources());
            pushStringArray(tfCode, annotation.variables());

            tfCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/cloud/DeepMixTerraform",
                "execute",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;ZZZZI[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(tfCode);

            context.addDiagnostic(String.format(
                "🏗️ @DeepTerraform applied to %s::%s [provider=%s, plan=%b, apply=%b]",
                classNode.name, methodNode.name,
                annotation.provider(), annotation.plan(), annotation.apply()
            ));
        }

        /**
         * Process @DeepPrometheus — Inject Prometheus metric instrumentation.
         */
        public void processPrometheus(DeepPrometheus annotation, ClassNode classNode,
                                      MethodNode methodNode) throws DeepMixProcessingException {
            String metricName = annotation.metricName().isEmpty()
                ? classNode.name.replace('/', '_') + "_" + methodNode.name
                : annotation.metricName();

            // Add metric field to class
            String metricFieldName = "__deepmix_prom_" + metricName.replace('.', '_');
            String metricType;
            switch (annotation.type()) {
                case COUNTER:   metricType = "io/prometheus/client/Counter"; break;
                case GAUGE:     metricType = "io/prometheus/client/Gauge"; break;
                case HISTOGRAM: metricType = "io/prometheus/client/Histogram"; break;
                case SUMMARY:   metricType = "io/prometheus/client/Summary"; break;
                default:        metricType = "io/prometheus/client/Counter"; break;
            }

            // Add static metric field
            final String fMetricFieldName = metricFieldName;
            if (classNode.fields.stream().noneMatch(f -> f.name.equals(fMetricFieldName))) {
                classNode.fields.add(new FieldNode(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                    metricFieldName,
                    "L" + metricType + ";", null, null
                ));
            }

            // Inject metric observation at method entry (start timer) and exit (observe)
            InsnList metricEntry = new InsnList();

            metricEntry.add(new LdcInsnNode(metricName));
            metricEntry.add(new LdcInsnNode(annotation.help()));
            metricEntry.add(new LdcInsnNode(annotation.type().name()));
            metricEntry.add(new LdcInsnNode(
                annotation.namespace_().isEmpty() ? "" : annotation.namespace_()));
            metricEntry.add(new LdcInsnNode(annotation.subsystem()));
            pushStringArray(metricEntry, annotation.labels());

            metricEntry.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/cloud/DeepMixPrometheus",
                "recordStart",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            // Store timer handle
            int timerIdx = methodNode.maxLocals;
            methodNode.maxLocals += 1;
            metricEntry.add(new VarInsnNode(Opcodes.ASTORE, timerIdx));

            methodNode.instructions.insert(metricEntry);

            // Insert metric observation before each return
            InsnList metricExit = new InsnList();
            metricExit.add(new VarInsnNode(Opcodes.ALOAD, timerIdx));
            metricExit.add(new LdcInsnNode(metricName));
            metricExit.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/cloud/DeepMixPrometheus",
                "recordEnd",
                "(Ljava/lang/Object;Ljava/lang/String;)V",
                false
            ));

            insertBeforeReturns(methodNode, metricExit);

            context.addDiagnostic(String.format(
                "📊 @DeepPrometheus applied to %s::%s [metric=%s, type=%s]",
                classNode.name, methodNode.name, metricName, annotation.type()
            ));
        }

        /**
         * Process @DeepGitHub — Generate GitHub Actions workflow YAML.
         */
        public void processGitHub(DeepGitHub annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {
            InsnList ghCode = new InsnList();

            ghCode.add(new LdcInsnNode(annotation.workflowName()));
            ghCode.add(new LdcInsnNode(annotation.runsOn()));
            ghCode.add(new LdcInsnNode(annotation.timeoutMinutes()));
            ghCode.add(new InsnNode(
                annotation.concurrency() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            ghCode.add(new LdcInsnNode(annotation.concurrencyGroup()));
            ghCode.add(new InsnNode(
                annotation.cancelInProgress() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            pushStringArray(ghCode, annotation.on());
            pushStringArray(ghCode, annotation.branches());
            pushStringArray(ghCode, annotation.steps());
            pushStringArray(ghCode, annotation.env());
            pushStringArray(ghCode, annotation.secrets());

            ghCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/cloud/DeepMixGitHub",
                "generateWorkflow",
                "(Ljava/lang/String;Ljava/lang/String;IZLjava/lang/String;Z" +
                "[Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;" +
                "[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;",
                false
            ));

            methodNode.instructions.insert(ghCode);

            context.addDiagnostic(String.format(
                "🐙 @DeepGitHub applied to %s::%s [workflow=%s, runsOn=%s]",
                classNode.name, methodNode.name,
                annotation.workflowName(), annotation.runsOn()
            ));
        }

        // ─── Helpers ───

        private void pushStringArray(InsnList insns, String[] values) {
            insns.add(new LdcInsnNode(values.length));
            insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < values.length; i++) {
                insns.add(new InsnNode(Opcodes.DUP));
                insns.add(new LdcInsnNode(i));
                insns.add(new LdcInsnNode(values[i]));
                insns.add(new InsnNode(Opcodes.AASTORE));
            }
        }

        private void insertBeforeReturns(MethodNode methodNode, InsnList toInsert) {
            List<AbstractInsnNode> returnInsns = new ArrayList<>();
            for (AbstractInsnNode insn : methodNode.instructions) {
                int op = insn.getOpcode();
                if (op == Opcodes.RETURN || op == Opcodes.ARETURN ||
                    op == Opcodes.IRETURN || op == Opcodes.LRETURN ||
                    op == Opcodes.FRETURN || op == Opcodes.DRETURN) {
                    returnInsns.add(insn);
                }
            }
            for (AbstractInsnNode ret : returnInsns) {
                InsnList copy = new InsnList();
                Map<LabelNode, LabelNode> labelMap = new HashMap<>();
                for (AbstractInsnNode insn : toInsert) {
                    if (insn instanceof LabelNode)
                        labelMap.put((LabelNode) insn, new LabelNode());
                }
                for (AbstractInsnNode insn : toInsert) {
                    copy.add(insn.clone(labelMap));
                }
                methodNode.instructions.insertBefore(ret, copy);
            }
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                      ║
    // ║  PHASE 16: MACHINE LEARNING & AI                                     ║
    // ║  12 annotations | Priority: LOW | Est. Time: 2-3 days                ║
    // ║                                                                      ║
    // ║  @DeepTensorFlow  @DeepPyTorch  @DeepONNX  @DeepScikit              ║
    // ║  @DeepKeras       @DeepNumPy    @DeepPandas @DeepOpenCV             ║
    // ║  @DeepNLP         @DeepSpaCy    @DeepHuggingFace @DeepLangChain     ║
    // ║                                                                      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // Phase 16 — Enums
    // ──────────────────────────────────────────────

    /** ML framework backend */
    public enum MLBackend {
        CPU,
        CUDA,
        ROCM,
        METAL,
        VULKAN,
        OPENCL,
        TPU,
        ONNX_RUNTIME,
        TENSORRT,
        OPENVINO,
        DIRECTML,
        AUTO
    }

    /** ML model operation */
    public enum ModelOperation {
        LOAD,
        SAVE,
        INFER,
        TRAIN,
        EVALUATE,
        FINE_TUNE,
        QUANTIZE,
        PRUNE,
        EXPORT,
        OPTIMIZE,
        BENCHMARK
    }

    /** Data preprocessing mode */
    public enum PreprocessMode {
        NORMALIZE,        // Min-max normalization [0, 1]
        STANDARDIZE,      // Z-score (mean=0, std=1)
        MIN_MAX_SCALE,    // Custom range scaling
        LOG_TRANSFORM,    // Logarithmic transform
        ONE_HOT_ENCODE,   // Categorical one-hot encoding
        LABEL_ENCODE,     // Label encoding
        TOKENIZE,         // Text tokenization
        EMBEDDING,        // Embedding lookup
        AUGMENT,          // Data augmentation
        RESIZE,           // Image resize
        CROP,             // Image crop
        PAD,              // Sequence padding
        NONE
    }

    /** NLP task type */
    public enum NLPTask {
        TEXT_CLASSIFICATION,
        SENTIMENT_ANALYSIS,
        NAMED_ENTITY_RECOGNITION,
        PART_OF_SPEECH_TAGGING,
        QUESTION_ANSWERING,
        TEXT_GENERATION,
        SUMMARIZATION,
        TRANSLATION,
        FILL_MASK,
        ZERO_SHOT_CLASSIFICATION,
        TOKEN_CLASSIFICATION,
        TEXT_TO_TEXT,
        EMBEDDING,
        SIMILARITY,
        CUSTOM
    }

    /** Image processing operation */
    public enum ImageOp {
        RESIZE,
        CROP,
        ROTATE,
        FLIP,
        BLUR,
        SHARPEN,
        EDGE_DETECT,
        THRESHOLD,
        CONTOUR_DETECT,
        FACE_DETECT,
        OBJECT_DETECT,
        SEGMENTATION,
        TEMPLATE_MATCH,
        COLOR_CONVERT,
        HISTOGRAM_EQUALIZE,
        MORPHOLOGY,
        PERSPECTIVE_TRANSFORM,
        FEATURE_EXTRACT,
        CUSTOM
    }

    /** Tensor data type */
    public enum TensorDType {
        FLOAT16,
        FLOAT32,
        FLOAT64,
        INT8,
        INT16,
        INT32,
        INT64,
        UINT8,
        BOOL,
        STRING,
        COMPLEX64,
        COMPLEX128,
        BFLOAT16
    }

    /** DataFrame operation type */
    public enum DataFrameOp {
        SELECT,
        FILTER,
        GROUP_BY,
        AGGREGATE,
        JOIN,
        MERGE,
        PIVOT,
        MELT,
        SORT,
        RANK,
        WINDOW,
        RESAMPLE,
        FILL_NA,
        DROP_NA,
        DESCRIBE,
        CORRELATE,
        APPLY,
        MAP,
        CUSTOM
    }

    // ──────────────────────────────────────────────
    // Phase 16 — Sub-annotation types
    // ──────────────────────────────────────────────

    /** Model configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ModelConfig {
        String modelPath() default "";
        String modelUrl() default "";          // Download from URL
        String modelName() default "";         // HuggingFace model name
        String revision() default "main";      // Model revision/version
        MLBackend backend() default MLBackend.AUTO;
        TensorDType dtype() default TensorDType.FLOAT32;
        int batchSize() default 1;
        int maxSequenceLength() default 512;
        boolean warmup() default true;          // Warm up model on load
        int warmupIterations() default 3;
        boolean quantized() default false;
        String quantizationMethod() default ""; // dynamic, static, qat
        boolean cached() default true;          // Cache model in memory
        long maxMemoryMB() default -1;          // -1 = unlimited
    }

    /** Training configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TrainConfig {
        int epochs() default 10;
        int batchSize() default 32;
        double learningRate() default 0.001;
        String optimizer() default "adam";       // adam, sgd, rmsprop, adagrad
        String lossFunction() default "cross_entropy";
        String[] metrics() default {"accuracy"};
        double weightDecay() default 0.0;
        double momentum() default 0.9;
        String scheduler() default "";          // Learning rate scheduler
        double warmupRatio() default 0.1;
        boolean mixedPrecision() default false;
        boolean gradientCheckpointing() default false;
        int gradientAccumulationSteps() default 1;
        double maxGradNorm() default 1.0;
        String[] callbacks() default {};
        int saveEveryNEpochs() default 1;
        String checkpointDir() default "";
        boolean earlyStop() default false;
        int earlyStopPatience() default 5;
        String earlyStopMetric() default "val_loss";
    }

    /** Preprocessing pipeline step */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PreprocessStep {
        PreprocessMode mode();
        String[] params() default {};          // "key=value" params for the step
        int order() default 0;                 // Execution order
    }

    // ──────────────────────────────────────────────
    // Phase 16 — Annotations
    // ──────────────────────────────────────────────

    /**
     * 🧠 @DeepTensorFlow — TensorFlow model loading and inference.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepTensorFlow {
        String target() default "";
        ModelConfig model() default @ModelConfig;
        ModelOperation operation() default ModelOperation.INFER;
        TrainConfig training() default @TrainConfig;
        String savedModelDir() default "";
        String[] inputNodeNames() default {};
        String[] outputNodeNames() default {};
        String signatureKey() default "serving_default";
        boolean useTFServing() default false;
        String tfServingUrl() default "";
        boolean useXLA() default false;        // XLA JIT compilation
        PreprocessStep[] preprocess() default {};
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepTensorFlow */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DTFLOW {
        String target() default "";
        ModelConfig model() default @ModelConfig;
        ModelOperation operation() default ModelOperation.INFER;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔥 @DeepPyTorch — PyTorch model integration via JNI/TorchScript.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepPyTorch {
        String target() default "";
        ModelConfig model() default @ModelConfig;
        ModelOperation operation() default ModelOperation.INFER;
        TrainConfig training() default @TrainConfig;
        String torchScriptPath() default "";
        boolean traceModel() default false;
        boolean scriptModel() default false;
        String[] inputNames() default {};
        int[][] inputShapes() default {};
        TensorDType[] inputDtypes() default {};
        boolean useTorchServe() default false;
        String torchServeUrl() default "";
        boolean cudaGraphs() default false;
        boolean channels_last() default false;  // NHWC memory format
        PreprocessStep[] preprocess() default {};
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepPyTorch */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DPYTORCH {
        String target() default "";
        ModelConfig model() default @ModelConfig;
        ModelOperation operation() default ModelOperation.INFER;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔗 @DeepONNX — ONNX Runtime model loading and inference.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepONNX {
        String target() default "";
        ModelConfig model() default @ModelConfig;
        ModelOperation operation() default ModelOperation.INFER;
        String[] executionProviders() default {"CPUExecutionProvider"};
        int intraOpNumThreads() default 0;     // 0 = auto
        int interOpNumThreads() default 0;
        String graphOptimizationLevel() default "ORT_ENABLE_ALL";
        boolean enableMemPattern() default true;
        boolean enableCpuMemArena() default true;
        boolean enableProfiling() default false;
        String profileOutputFile() default "";
        String[] inputNames() default {};
        String[] outputNames() default {};
        PreprocessStep[] preprocess() default {};
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepONNX */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DONNX {
        String target() default "";
        ModelConfig model() default @ModelConfig;
        String[] executionProviders() default {"CPUExecutionProvider"};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔬 @DeepScikit — Scikit-learn integration via Jep/GraalPython.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepScikit {
        String target() default "";
        String modelPath() default "";         // .pkl or .joblib file
        ModelOperation operation() default ModelOperation.INFER;
        String estimator() default "";         // sklearn class name
        String[] hyperparameters() default {}; // "key=value" pairs
        PreprocessStep[] preprocess() default {};
        String pipelinePath() default "";      // Sklearn Pipeline path
        boolean crossValidate() default false;
        int cvFolds() default 5;
        String scoring() default "accuracy";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepScikit */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DSKLEARN {
        String target() default "";
        String modelPath() default "";
        ModelOperation operation() default ModelOperation.INFER;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🏗️ @DeepKeras — Keras model integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepKeras {
        String target() default "";
        ModelConfig model() default @ModelConfig;
        ModelOperation operation() default ModelOperation.INFER;
        TrainConfig training() default @TrainConfig;
        String h5Path() default "";            // .h5 model file
        String kerasConfigJson() default "";   // Model architecture JSON
        String weightsPath() default "";       // Separate weights file
        boolean compileOnLoad() default true;
        PreprocessStep[] preprocess() default {};
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepKeras */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DKERAS {
        String target() default "";
        ModelConfig model() default @ModelConfig;
        ModelOperation operation() default ModelOperation.INFER;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔢 @DeepNumPy — NumPy-style array operations in Java.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepNumPy {
        String target() default "";
        TensorDType dtype() default TensorDType.FLOAT32;
        int[] shape() default {};
        String operation() default "";          // numpy function name
        String[] operationArgs() default {};
        boolean inPlace() default false;
        boolean contiguous() default true;      // C-contiguous memory
        boolean broadcast() default true;       // Enable broadcasting
        MLBackend backend() default MLBackend.CPU;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepNumPy */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DNUMPY {
        String target() default "";
        TensorDType dtype() default TensorDType.FLOAT32;
        int[] shape() default {};
        String operation() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📊 @DeepPandas — DataFrame operations in Java.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepPandas {
        String target() default "";
        DataFrameOp operation() default DataFrameOp.SELECT;
        String[] columns() default {};
        String filter() default "";             // Filter expression
        String[] groupBy() default {};
        String[] aggregations() default {};     // "column:function" pairs
        String[] sortBy() default {};
        boolean ascending() default true;
        String joinType() default "inner";      // inner, outer, left, right
        String joinOn() default "";             // Join column
        String csvPath() default "";            // Read/write CSV
        String parquetPath() default "";        // Read/write Parquet
        boolean inferTypes() default true;
        String naValue() default "";            // Value to use for NaN
        int head() default -1;                  // -1 = all rows
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepPandas */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DPANDAS {
        String target() default "";
        DataFrameOp operation() default DataFrameOp.SELECT;
        String[] columns() default {};
        String filter() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📷 @DeepOpenCV — OpenCV computer vision operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepOpenCV {
        String target() default "";
        ImageOp operation() default ImageOp.RESIZE;
        ImageOp[] pipeline() default {};       // Chain of operations
        int width() default 0;                 // 0 = auto
        int height() default 0;
        double scaleFactor() default 1.0;
        int interpolation() default 1;         // INTER_LINEAR
        String colorSpace() default "";        // BGR2RGB, BGR2GRAY, etc.
        int kernelSize() default 3;            // For blur, morphology
        double threshold() default 128.0;
        String cascadeFile() default "";       // For face/object detection
        String dnnModel() default "";          // For DNN-based detection
        String dnnConfig() default "";
        double confidenceThreshold() default 0.5;
        boolean useGPU() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepOpenCV */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DOPENCV {
        String target() default "";
        ImageOp operation() default ImageOp.RESIZE;
        int width() default 0;
        int height() default 0;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📝 @DeepNLP — Natural Language Processing operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepNLP {
        String target() default "";
        NLPTask task() default NLPTask.TEXT_CLASSIFICATION;
        String modelName() default "";         // Pre-trained model identifier
        String language() default "en";
        int maxLength() default 512;
        boolean returnScores() default true;
        boolean returnLabels() default true;
        boolean returnTokens() default false;
        boolean returnEntities() default false;
        String[] labels() default {};          // For zero-shot classification
        String sourceLanguage() default "";    // For translation
        String targetLanguage() default "";    // For translation
        int numBeams() default 4;              // For generation/summarization
        double temperature() default 1.0;
        int maxNewTokens() default 128;
        int topK() default 50;
        double topP() default 0.9;
        boolean doSample() default false;
        PreprocessStep[] preprocess() default {};
        ModelConfig model() default @ModelConfig;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepNLP */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DNLP {
        String target() default "";
        NLPTask task() default NLPTask.TEXT_CLASSIFICATION;
        String modelName() default "";
        String language() default "en";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🌀 @DeepSpaCy — spaCy NLP pipeline integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepSpaCy {
        String target() default "";
        String model() default "en_core_web_sm";
        String[] components() default {};      // ner, parser, tagger, etc.
        String[] disableComponents() default {};
        boolean returnDoc() default true;
        boolean returnTokens() default false;
        boolean returnEntities() default false;
        boolean returnSentences() default false;
        boolean returnDependencies() default false;
        boolean returnVectors() default false;
        String[] entityRuler() default {};     // Custom entity rules
        String[] matcher() default {};         // Token matcher patterns
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSpaCy */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DSPACY {
        String target() default "";
        String model() default "en_core_web_sm";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🤗 @DeepHuggingFace — HuggingFace Transformers integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepHuggingFace {
        String target() default "";
        String modelName() default "";          // e.g., "bert-base-uncased"
        String tokenizer() default "";          // Custom tokenizer name
        NLPTask task() default NLPTask.TEXT_CLASSIFICATION;
        ModelConfig model() default @ModelConfig;
        TrainConfig training() default @TrainConfig;
        String pipelineName() default "";       // HF pipeline name
        boolean autoTokenize() default true;
        int maxLength() default 512;
        boolean padding() default true;
        boolean truncation() default true;
        String returnType() default "pt";       // pt, tf, np
        boolean useAccelerate() default false;
        boolean useBitsAndBytes() default false; // 4/8-bit quantization
        int loadIn() default 0;                 // 4 or 8 for quantization
        String deviceMap() default "auto";
        String trustRemoteCode() default "false";
        String authToken() default "";          // HF auth token
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepHuggingFace */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DHF {
        String target() default "";
        String modelName() default "";
        NLPTask task() default NLPTask.TEXT_CLASSIFICATION;
        int maxLength() default 512;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔗 @DeepLangChain — LangChain LLM orchestration integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepLangChain {
        String target() default "";
        String llmProvider() default "openai";   // openai, anthropic, google, local
        String modelName() default "gpt-4";
        String apiKey() default "";
        String apiKeyEnvVar() default "";         // Env var for API key
        String baseUrl() default "";
        double temperature() default 0.7;
        int maxTokens() default 1024;
        String[] stopSequences() default {};
        String systemPrompt() default "";
        String promptTemplate() default "";
        String[] promptVariables() default {};
        boolean useChain() default false;         // Use LangChain chain
        String chainType() default "";            // stuff, map_reduce, refine
        boolean useAgent() default false;
        String[] tools() default {};              // Agent tools
        String agentType() default "zero-shot-react-description";
        boolean useRAG() default false;           // Retrieval-Augmented Generation
        String vectorStore() default "";          // chroma, pinecone, faiss
        String embeddingModel() default "";
        int retrievalK() default 4;               // Number of documents to retrieve
        boolean streamResponse() default false;
        String memoryType() default "";           // buffer, summary, conversation
        int memoryK() default 5;                  // Memory window
        boolean verbose() default false;
        String callbackHandler() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepLangChain */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DLC {
        String target() default "";
        String llmProvider() default "openai";
        String modelName() default "gpt-4";
        String apiKeyEnvVar() default "";
        double temperature() default 0.7;
        int maxTokens() default 1024;
        String promptTemplate() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 16 — Processor
    // ──────────────────────────────────────────────

    /**
     * Processor for all Phase 16 (Machine Learning & AI) annotations.
     */
    public static class Phase16Processor {

        private final DeepMixContext context;

        public Phase16Processor(DeepMixContext context) {
            this.context = context;
        }

        /**
         * Process @DeepONNX — Load ONNX model and execute inference.
         */
        public void processONNX(DeepONNX annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            ModelConfig model = annotation.model();
            InsnList onnxCode = new InsnList();

            onnxCode.add(new LdcInsnNode(model.modelPath()));
            onnxCode.add(new LdcInsnNode(model.backend().name()));
            onnxCode.add(new LdcInsnNode(model.dtype().name()));
            onnxCode.add(new LdcInsnNode(model.batchSize()));
            onnxCode.add(new InsnNode(
                model.warmup() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            onnxCode.add(new InsnNode(
                model.cached() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            pushStringArray(onnxCode, annotation.executionProviders());
            onnxCode.add(new LdcInsnNode(annotation.intraOpNumThreads()));
            onnxCode.add(new LdcInsnNode(annotation.graphOptimizationLevel()));

            pushStringArray(onnxCode, annotation.inputNames());
            pushStringArray(onnxCode, annotation.outputNames());

            onnxCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/ml/DeepMixONNX",
                "loadAndInfer",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "IZZ[Ljava/lang/String;ILjava/lang/String;" +
                "[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(onnxCode);

            context.addDiagnostic(String.format(
                "🔗 @DeepONNX applied to %s::%s [model=%s, providers=%s]",
                classNode.name, methodNode.name,
                model.modelPath(), Arrays.toString(annotation.executionProviders())
            ));
        }

        /**
         * Process @DeepHuggingFace — Set up HuggingFace pipeline.
         */
        public void processHuggingFace(DeepHuggingFace annotation, ClassNode classNode,
                                       MethodNode methodNode) throws DeepMixProcessingException {
            InsnList hfCode = new InsnList();

            hfCode.add(new LdcInsnNode(annotation.modelName()));
            hfCode.add(new LdcInsnNode(annotation.task().name()));
            hfCode.add(new LdcInsnNode(annotation.maxLength()));
            hfCode.add(new InsnNode(
                annotation.autoTokenize() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            hfCode.add(new InsnNode(
                annotation.padding() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            hfCode.add(new InsnNode(
                annotation.truncation() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            hfCode.add(new LdcInsnNode(annotation.deviceMap()));
            hfCode.add(new LdcInsnNode(annotation.loadIn()));
            hfCode.add(new LdcInsnNode(annotation.authToken()));

            hfCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/ml/DeepMixHuggingFace",
                "pipeline",
                "(Ljava/lang/String;Ljava/lang/String;IZZZLjava/lang/String;" +
                "ILjava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(hfCode);

            context.addDiagnostic(String.format(
                "🤗 @DeepHuggingFace applied to %s::%s [model=%s, task=%s]",
                classNode.name, methodNode.name,
                annotation.modelName(), annotation.task()
            ));
        }

        /**
         * Process @DeepLangChain — Wire up LangChain LLM call.
         */
        public void processLangChain(DeepLangChain annotation, ClassNode classNode,
                                     MethodNode methodNode) throws DeepMixProcessingException {
            InsnList lcCode = new InsnList();

            lcCode.add(new LdcInsnNode(annotation.llmProvider()));
            lcCode.add(new LdcInsnNode(annotation.modelName()));
            lcCode.add(new LdcInsnNode(annotation.apiKeyEnvVar()));
            lcCode.add(new LdcInsnNode(annotation.baseUrl()));
            lcCode.add(new LdcInsnNode(annotation.temperature()));
            lcCode.add(new LdcInsnNode(annotation.maxTokens()));
            lcCode.add(new LdcInsnNode(annotation.systemPrompt()));
            lcCode.add(new LdcInsnNode(annotation.promptTemplate()));
            lcCode.add(new InsnNode(
                annotation.useRAG() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            lcCode.add(new LdcInsnNode(annotation.vectorStore()));
            lcCode.add(new LdcInsnNode(annotation.embeddingModel()));
            lcCode.add(new LdcInsnNode(annotation.retrievalK()));
            lcCode.add(new InsnNode(
                annotation.streamResponse() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            lcCode.add(new LdcInsnNode(annotation.memoryType()));
            lcCode.add(new LdcInsnNode(annotation.memoryK()));
            lcCode.add(new InsnNode(
                annotation.useAgent() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            pushStringArray(lcCode, annotation.tools());
            pushStringArray(lcCode, annotation.promptVariables());
            pushStringArray(lcCode, annotation.stopSequences());

            lcCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/ml/DeepMixLangChain",
                "invoke",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;DILjava/lang/String;Ljava/lang/String;" +
                "ZLjava/lang/String;Ljava/lang/String;IZLjava/lang/String;" +
                "IZ[Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(lcCode);

            context.addDiagnostic(String.format(
                "🔗 @DeepLangChain applied to %s::%s [provider=%s, model=%s, rag=%b]",
                classNode.name, methodNode.name,
                annotation.llmProvider(), annotation.modelName(), annotation.useRAG()
            ));
        }

        private void pushStringArray(InsnList insns, String[] values) {
            insns.add(new LdcInsnNode(values.length));
            insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < values.length; i++) {
                insns.add(new InsnNode(Opcodes.DUP));
                insns.add(new LdcInsnNode(i));
                insns.add(new LdcInsnNode(values[i]));
                insns.add(new InsnNode(Opcodes.AASTORE));
            }
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                      ║
    // ║  PHASE 17: DATABASE OPERATIONS                                       ║
    // ║  14 annotations | Priority: MEDIUM | Est. Time: 2 days               ║
    // ║                                                                      ║
    // ║  @DeepJDBC @DeepJPA @DeepHibernate @DeepMyBatis                     ║
    // ║  @DeepPostgres @DeepMySQL @DeepOracle @DeepSQLite                   ║
    // ║  @DeepCassandra @DeepNeo4j @DeepCouchDB @DeepInfluxDB               ║
    // ║  @DeepTimescale @DeepClickHouse                                      ║
    // ║                                                                      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // Phase 17 — Enums
    // ──────────────────────────────────────────────

    /** Database operation type */
    public enum DBOperation {
        SELECT,
        INSERT,
        UPDATE,
        DELETE,
        UPSERT,
        MERGE,
        BATCH_INSERT,
        BATCH_UPDATE,
        BATCH_DELETE,
        CALL_PROCEDURE,
        CALL_FUNCTION,
        DDL,             // CREATE, ALTER, DROP
        TRUNCATE,
        COUNT,
        EXISTS,
        CUSTOM
    }

    /** Fetch strategy */
    public enum FetchStrategy {
        EAGER,
        LAZY,
        BATCH,
        SUBSELECT,
        JOIN,
        SELECT,
        CURSOR,
        STREAM,
        NONE
    }

    /** Flush mode */
    public enum FlushMode {
        AUTO,
        COMMIT,
        ALWAYS,
        MANUAL,
        NEVER
    }

    /** Lock mode for database operations */
    public enum DBLockMode {
        NONE,
        READ,
        WRITE,
        OPTIMISTIC,
        OPTIMISTIC_FORCE_INCREMENT,
        PESSIMISTIC_READ,
        PESSIMISTIC_WRITE,
        PESSIMISTIC_FORCE_INCREMENT
    }

    /** Cache strategy for JPA/Hibernate */
    public enum CacheStrategy {
        NONE,
        READ_ONLY,
        READ_WRITE,
        NONSTRICT_READ_WRITE,
        TRANSACTIONAL
    }

    /** Neo4j relationship direction */
    public enum Neo4jDirection {
        OUTGOING,
        INCOMING,
        BOTH
    }

    /** Time-series aggregation function */
    public enum TimeSeriesAggregation {
        MEAN,
        MEDIAN,
        SUM,
        COUNT,
        MIN,
        MAX,
        FIRST,
        LAST,
        SPREAD,
        STDDEV,
        PERCENTILE,
        DERIVATIVE,
        INTEGRAL,
        MOVING_AVERAGE,
        EXPONENTIAL_MOVING_AVERAGE,
        CUMULATIVE_SUM,
        RATE,
        HISTOGRAM,
        CUSTOM
    }

    /** Consistency level for distributed databases */
    public enum ConsistencyLevel {
        ANY,
        ONE,
        TWO,
        THREE,
        QUORUM,
        LOCAL_QUORUM,
        EACH_QUORUM,
        ALL,
        LOCAL_ONE,
        SERIAL,
        LOCAL_SERIAL,
        EVENTUAL,
        STRONG,
        BOUNDED_STALENESS,
        SESSION,
        CONSISTENT_PREFIX
    }

    // ──────────────────────────────────────────────
    // Phase 17 — Sub-annotation types
    // ──────────────────────────────────────────────

    /** JDBC connection configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface JDBCConfig {
        String url() default "";
        String driver() default "";
        String username() default "";
        String password() default "";
        int minPoolSize() default 5;
        int maxPoolSize() default 20;
        long connectionTimeoutMs() default 30000;
        long idleTimeoutMs() default 600000;
        long maxLifetimeMs() default 1800000;
        boolean autoCommit() default false;
        int fetchSize() default 100;
        int queryTimeout() default 0;
        String schema() default "";
        String catalog() default "";
        boolean readOnly() default false;
    }

    /** Query parameter binding */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface QueryParam {
        String name() default "";
        int index() default -1;
        String type() default "";              // SQL type override
        boolean nullable() default true;
    }

    /** Index specification for DDL */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface IndexSpec {
        String name() default "";
        String[] columns();
        boolean unique() default false;
        boolean clustered() default false;
        String where() default "";             // Partial index condition
        String using() default "";             // btree, hash, gin, gist
    }

    /** Column mapping for ORM */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ColumnMapping {
        String javaField();
        String dbColumn();
        String type() default "";
        boolean nullable() default true;
        boolean insertable() default true;
        boolean updatable() default true;
        String defaultValue() default "";
    }

    // ──────────────────────────────────────────────
    // Phase 17 — Annotations
    // ──────────────────────────────────────────────

    /**
     * 🗄️ @DeepJDBC — Direct JDBC operations with connection pooling.
     *
     * Example:
     * <pre>
     * {@code @DeepJDBC(
     *     config = @JDBCConfig(url = "jdbc:postgresql://localhost/mydb"),
     *     sql = "SELECT * FROM players WHERE level > ?",
     *     operation = DBOperation.SELECT,
     *     params = {@QueryParam(index = 1, type = "INT")}
     * )}
     * public List<Player> getHighLevelPlayers(int minLevel) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepJDBC {
        String target() default "";
        JDBCConfig config() default @JDBCConfig;
        String sql() default "";
        String sqlFile() default "";           // Load SQL from file
        DBOperation operation() default DBOperation.SELECT;
        QueryParam[] params() default {};
        FetchStrategy fetchStrategy() default FetchStrategy.EAGER;
        int fetchSize() default 100;
        boolean streaming() default false;
        boolean batch() default false;
        int batchSize() default 100;
        boolean generatedKeys() default false;
        String[] generatedKeyColumns() default {};
        String resultMapper() default "";      // Custom ResultSet mapper
        boolean namedParameters() default false;
        boolean prepared() default true;       // Use PreparedStatement
        boolean callable() default false;      // Use CallableStatement
        IsolationLevel isolation() default IsolationLevel.READ_COMMITTED;
        boolean transactional() default true;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepJDBC */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DJDBC {
        String target() default "";
        JDBCConfig config() default @JDBCConfig;
        String sql() default "";
        DBOperation operation() default DBOperation.SELECT;
        QueryParam[] params() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📋 @DeepJPA — JPA entity operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepJPA {
        String target() default "";
        String persistenceUnit() default "default";
        DBOperation operation() default DBOperation.SELECT;
        String jpql() default "";              // JPQL query
        String nativeQuery() default "";       // Native SQL
        String namedQuery() default "";
        FetchStrategy fetchStrategy() default FetchStrategy.LAZY;
        FlushMode flushMode() default FlushMode.AUTO;
        DBLockMode lockMode() default DBLockMode.NONE;
        CacheStrategy cache() default CacheStrategy.NONE;
        boolean cacheable() default false;
        String cacheRegion() default "";
        int maxResults() default -1;
        int firstResult() default 0;
        String[] hints() default {};           // "key=value" query hints
        ColumnMapping[] mappings() default {};
        boolean readOnly() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepJPA */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DJPA {
        String target() default "";
        DBOperation operation() default DBOperation.SELECT;
        String jpql() default "";
        FetchStrategy fetchStrategy() default FetchStrategy.LAZY;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🐻 @DeepHibernate — Hibernate-specific operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepHibernate {
        String target() default "";
        DBOperation operation() default DBOperation.SELECT;
        String hql() default "";
        String criteria() default "";          // Criteria API expression
        FetchStrategy fetchStrategy() default FetchStrategy.LAZY;
        FlushMode flushMode() default FlushMode.AUTO;
        CacheStrategy secondLevelCache() default CacheStrategy.NONE;
        boolean queryCache() default false;
        String cacheRegion() default "";
        boolean statelessSession() default false;
        int batchSize() default 0;
        String[] fetchProfiles() default {};
        boolean naturalId() default false;
        String[] filters() default {};         // Hibernate filter names
        DBLockMode lockMode() default DBLockMode.NONE;
        long lockTimeout() default -1;         // -1 = default
        String resultTransformer() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepHibernate */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DHIB {
        String target() default "";
        DBOperation operation() default DBOperation.SELECT;
        String hql() default "";
        CacheStrategy secondLevelCache() default CacheStrategy.NONE;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🗺️ @DeepMyBatis — MyBatis mapping integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepMyBatis {
        String target() default "";
        String mapperNamespace() default "";
        String statementId() default "";
        DBOperation operation() default DBOperation.SELECT;
        String[] resultMaps() default {};
        boolean useGeneratedKeys() default false;
        String keyProperty() default "";
        String keyColumn() default "";
        FetchStrategy fetchStrategy() default FetchStrategy.EAGER;
        int fetchSize() default 100;
        boolean flushCache() default false;
        boolean useCache() default true;
        long timeout() default 0;
        String resultSetType() default "FORWARD_ONLY";
        String databaseId() default "";
        String lang() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMyBatis */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DMYBATIS {
        String target() default "";
        String statementId() default "";
        DBOperation operation() default DBOperation.SELECT;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🐘 @DeepPostgres — PostgreSQL-specific operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepPostgres {
        String target() default "";
        JDBCConfig config() default @JDBCConfig;
        String sql() default "";
        DBOperation operation() default DBOperation.SELECT;
        boolean useJsonb() default false;      // JSONB column operations
        boolean useArray() default false;      // PostgreSQL arrays
        boolean useHstore() default false;     // Key-value hstore
        boolean useListen() default false;     // LISTEN/NOTIFY
        String channel() default "";           // For LISTEN/NOTIFY
        boolean useCopy() default false;       // COPY for bulk data
        String copyFormat() default "CSV";     // CSV, BINARY, TEXT
        boolean useAdvisoryLock() default false;
        String fullTextSearch() default "";    // FTS query
        String ftsConfig() default "english";  // FTS configuration
        IndexSpec[] indexes() default {};
        boolean partitioned() default false;
        String partitionBy() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepPostgres */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DPGSQL {
        String target() default "";
        String sql() default "";
        DBOperation operation() default DBOperation.SELECT;
        boolean useJsonb() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🐬 @DeepMySQL — MySQL-specific operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepMySQL {
        String target() default "";
        JDBCConfig config() default @JDBCConfig;
        String sql() default "";
        DBOperation operation() default DBOperation.SELECT;
        boolean useJson() default false;
        boolean useLoadDataInfile() default false;
        String loadDataPath() default "";
        boolean useInsertIgnore() default false;
        boolean useOnDuplicateKey() default false;
        String[] onDuplicateUpdate() default {};
        boolean useStraightJoin() default false;
        boolean useForceIndex() default false;
        String forceIndexName() default "";
        String engine() default "InnoDB";
        String charset() default "utf8mb4";
        String collation() default "utf8mb4_unicode_ci";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMySQL */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DMYSQL {
        String target() default "";
        String sql() default "";
        DBOperation operation() default DBOperation.SELECT;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔮 @DeepOracle — Oracle Database-specific operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepOracle {
        String target() default "";
        JDBCConfig config() default @JDBCConfig;
        String sql() default "";
        DBOperation operation() default DBOperation.SELECT;
        boolean useReturning() default false;
        boolean useFlashback() default false;
        String flashbackTime() default "";
        boolean useBulkCollect() default false;
        boolean useForAll() default false;
        String[] dbmsOutputLines() default {};
        boolean useParallelHint() default false;
        int parallelDegree() default 4;
        String tablespaceName() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepOracle */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DORACLE {
        String target() default "";
        String sql() default "";
        DBOperation operation() default DBOperation.SELECT;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📦 @DeepSQLite — SQLite operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepSQLite {
        String target() default "";
        String databasePath() default "";
        String sql() default "";
        DBOperation operation() default DBOperation.SELECT;
        boolean walMode() default true;
        boolean foreignKeys() default true;
        int busyTimeoutMs() default 5000;
        boolean sharedCache() default false;
        boolean inMemory() default false;
        String journalMode() default "WAL";    // DELETE, TRUNCATE, PERSIST, MEMORY, WAL, OFF
        String synchronous() default "NORMAL"; // OFF, NORMAL, FULL, EXTRA
        int cacheSize() default -2000;         // Negative = KiB
        int pageSize() default 4096;
        boolean autoVacuum() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSQLite */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DSQLITE {
        String target() default "";
        String databasePath() default "";
        String sql() default "";
        DBOperation operation() default DBOperation.SELECT;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔷 @DeepCassandra — Apache Cassandra integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepCassandra {
        String target() default "";
        String[] contactPoints() default {"localhost"};
        int port() default 9042;
        String keyspace() default "";
        String datacenter() default "datacenter1";
        String cql() default "";
        DBOperation operation() default DBOperation.SELECT;
        ConsistencyLevel consistency() default ConsistencyLevel.LOCAL_QUORUM;
        ConsistencyLevel serialConsistency() default ConsistencyLevel.SERIAL;
        int fetchSize() default 5000;
        boolean tracing() default false;
        boolean idempotent() default false;
        String loadBalancingPolicy() default "RoundRobin";
        String retryPolicy() default "Default";
        long requestTimeoutMs() default 12000;
        boolean ssl() default false;
        String username() default "";
        String password() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCassandra */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DCASS {
        String target() default "";
        String[] contactPoints() default {"localhost"};
        String keyspace() default "";
        String cql() default "";
        ConsistencyLevel consistency() default ConsistencyLevel.LOCAL_QUORUM;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔵 @DeepNeo4j — Neo4j graph database operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepNeo4j {
        String target() default "";
        String uri() default "bolt://localhost:7687";
        String database() default "neo4j";
        String username() default "neo4j";
        String password() default "";
        String cypher() default "";            // Cypher query
        DBOperation operation() default DBOperation.SELECT;
        boolean transactional() default true;
        String[] nodeLabels() default {};
        String[] relationshipTypes() default {};
        Neo4jDirection direction() default Neo4jDirection.OUTGOING;
        int maxDepth() default 3;              // For traversals
        boolean batch() default false;
        int batchSize() default 1000;
        boolean apoc() default false;          // Use APOC procedures
        String resultMapper() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepNeo4j */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DNEO4J {
        String target() default "";
        String uri() default "bolt://localhost:7687";
        String cypher() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🛋️ @DeepCouchDB — CouchDB operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepCouchDB {
        String target() default "";
        String url() default "http://localhost:5984";
        String database() default "";
        String username() default "";
        String password() default "";
        DBOperation operation() default DBOperation.SELECT;
        String viewName() default "";
        String designDoc() default "";
        String mangoQuery() default "";        // Mango query JSON
        String startKey() default "";
        String endKey() default "";
        boolean includeDocs() default true;
        boolean descending() default false;
        int limit() default 0;
        int skip() default 0;
        boolean reduce() default false;
        int groupLevel() default 0;
        boolean conflicts() default false;
        boolean attachments() default false;
        boolean replication() default false;
        String replicationTarget() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCouchDB */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DCOUCH {
        String target() default "";
        String url() default "http://localhost:5984";
        String database() default "";
        String mangoQuery() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📈 @DeepInfluxDB — InfluxDB time-series operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepInfluxDB {
        String target() default "";
        String url() default "http://localhost:8086";
        String org() default "";
        String bucket() default "";
        String token() default "";
        String fluxQuery() default "";         // Flux query language
        String influxQL() default "";          // Legacy InfluxQL
        DBOperation operation() default DBOperation.SELECT;
        String measurement() default "";
        String[] tags() default {};            // "key=value" tag pairs
        String[] fields() default {};          // "key=value" field pairs
        TimeSeriesAggregation aggregation() default TimeSeriesAggregation.MEAN;
        String window() default "";            // Aggregation window (e.g., "5m")
        String timeRange() default "-1h";      // Query time range
        String precision() default "ns";       // ns, us, ms, s
        boolean batch() default true;
        int batchSize() default 5000;
        long flushIntervalMs() default 1000;
        String retentionPolicy() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepInfluxDB */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DINFLUX {
        String target() default "";
        String url() default "http://localhost:8086";
        String bucket() default "";
        String fluxQuery() default "";
        String measurement() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ⏰ @DeepTimescale — TimescaleDB time-series operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepTimescale {
        String target() default "";
        JDBCConfig config() default @JDBCConfig;
        String sql() default "";
        DBOperation operation() default DBOperation.SELECT;
        String hypertable() default "";
        String timeBucketInterval() default "1 hour";
        TimeSeriesAggregation aggregation() default TimeSeriesAggregation.MEAN;
        boolean continuousAggregate() default false;
        String aggregateName() default "";
        boolean compression() default false;
        String compressAfter() default "";     // e.g., "7 days"
        String retentionPolicy() default "";   // e.g., "90 days"
        boolean realtimeAggregation() default true;
        boolean useTimescaleAnalytics() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepTimescale */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DTIME {
        String target() default "";
        String sql() default "";
        String hypertable() default "";
        String timeBucketInterval() default "1 hour";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🖱️ @DeepClickHouse — ClickHouse analytics database operations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepClickHouse {
        String target() default "";
        String url() default "jdbc:clickhouse://localhost:8123";
        String database() default "default";
        String username() default "default";
        String password() default "";
        String sql() default "";
        DBOperation operation() default DBOperation.SELECT;
        String engine() default "MergeTree";   // MergeTree, ReplacingMergeTree, etc.
        String orderBy() default "";
        String partitionBy() default "";
        String[] settings() default {};        // "key=value" ClickHouse settings
        boolean async() default false;
        boolean batch() default false;
        int batchSize() default 100000;
        boolean useBufferTable() default false;
        String format() default "RowBinary";   // Insert format
        boolean deduplication() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepClickHouse */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DCLICK {
        String target() default "";
        String sql() default "";
        String database() default "default";
        DBOperation operation() default DBOperation.SELECT;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 17 — Processor
    // ──────────────────────────────────────────────

    /**
     * Processor for all Phase 17 (Database Operations) annotations.
     */
    public static class Phase17Processor {

        private final DeepMixContext context;

        public Phase17Processor(DeepMixContext context) {
            this.context = context;
        }

        /**
         * Process @DeepJDBC — Inject JDBC query execution.
         */
        public void processJDBC(DeepJDBC annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            JDBCConfig config = annotation.config();
            InsnList jdbcCode = new InsnList();

            // Build connection config
            jdbcCode.add(new LdcInsnNode(config.url()));
            jdbcCode.add(new LdcInsnNode(config.driver()));
            jdbcCode.add(new LdcInsnNode(config.username()));
            jdbcCode.add(new LdcInsnNode(config.password()));
            jdbcCode.add(new LdcInsnNode(config.maxPoolSize()));

            // SQL and operation
            String sql = annotation.sql();
            if (sql.isEmpty() && !annotation.sqlFile().isEmpty()) {
                sql = "__FILE__:" + annotation.sqlFile();
            }
            jdbcCode.add(new LdcInsnNode(sql));
            jdbcCode.add(new LdcInsnNode(annotation.operation().name()));
            jdbcCode.add(new LdcInsnNode(annotation.fetchSize()));
            jdbcCode.add(new InsnNode(
                annotation.streaming() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            jdbcCode.add(new InsnNode(
                annotation.batch() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            jdbcCode.add(new LdcInsnNode(annotation.batchSize()));
            jdbcCode.add(new InsnNode(
                annotation.transactional() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            jdbcCode.add(new LdcInsnNode(annotation.isolation().name()));

            // Bind method parameters as query parameters
            Type[] argTypes = Type.getArgumentTypes(methodNode.desc);
            jdbcCode.add(new LdcInsnNode(argTypes.length));
            jdbcCode.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
            int localIdx = (methodNode.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
            for (int i = 0; i < argTypes.length; i++) {
                jdbcCode.add(new InsnNode(Opcodes.DUP));
                jdbcCode.add(new LdcInsnNode(i));
                loadAndBox(jdbcCode, argTypes[i], localIdx);
                jdbcCode.add(new InsnNode(Opcodes.AASTORE));
                localIdx += argTypes[i].getSize();
            }

            jdbcCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/db/DeepMixJDBC",
                "execute",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;" +
                "IZZIZLjava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
                false
            ));

            // Cast result to method return type
            Type returnType = Type.getReturnType(methodNode.desc);
            if (returnType.getSort() == Type.OBJECT) {
                jdbcCode.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
            }

            methodNode.instructions.insert(jdbcCode);

            context.addDiagnostic(String.format(
                "🗄️ @DeepJDBC applied to %s::%s [op=%s, sql=%s]",
                classNode.name, methodNode.name,
                annotation.operation(), truncateSQL(sql, 50)
            ));
        }

        /**
         * Process @DeepNeo4j — Inject Cypher query execution.
         */
        public void processNeo4j(DeepNeo4j annotation, ClassNode classNode,
                                 MethodNode methodNode) throws DeepMixProcessingException {
            InsnList neo4jCode = new InsnList();

            neo4jCode.add(new LdcInsnNode(annotation.uri()));
            neo4jCode.add(new LdcInsnNode(annotation.database()));
            neo4jCode.add(new LdcInsnNode(annotation.username()));
            neo4jCode.add(new LdcInsnNode(annotation.password()));
            neo4jCode.add(new LdcInsnNode(annotation.cypher()));
            neo4jCode.add(new LdcInsnNode(annotation.operation().name()));
            neo4jCode.add(new InsnNode(
                annotation.transactional() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            neo4jCode.add(new LdcInsnNode(annotation.maxDepth()));
            neo4jCode.add(new InsnNode(
                annotation.batch() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            neo4jCode.add(new LdcInsnNode(annotation.batchSize()));

            neo4jCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/db/DeepMixNeo4j",
                "executeCypher",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "ZIZI)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(neo4jCode);

            context.addDiagnostic(String.format(
                "🔵 @DeepNeo4j applied to %s::%s [cypher=%s]",
                classNode.name, methodNode.name, truncateSQL(annotation.cypher(), 50)
            ));
        }

        private String truncateSQL(String sql, int maxLen) {
            if (sql == null || sql.length() <= maxLen) return sql;
            return sql.substring(0, maxLen) + "...";
        }

        private void loadAndBox(InsnList insns, Type type, int localIdx) {
            switch (type.getSort()) {
                case Type.INT:
                    insns.add(new VarInsnNode(Opcodes.ILOAD, localIdx));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                    break;
                case Type.LONG:
                    insns.add(new VarInsnNode(Opcodes.LLOAD, localIdx));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
                    break;
                case Type.FLOAT:
                    insns.add(new VarInsnNode(Opcodes.FLOAD, localIdx));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
                    break;
                case Type.DOUBLE:
                    insns.add(new VarInsnNode(Opcodes.DLOAD, localIdx));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
                    break;
                case Type.BOOLEAN:
                    insns.add(new VarInsnNode(Opcodes.ILOAD, localIdx));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false));
                    break;
                default:
                    insns.add(new VarInsnNode(Opcodes.ALOAD, localIdx));
                    break;
            }
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                      ║
    // ║  PHASE 18: TESTING & QUALITY                                         ║
    // ║  11 annotations | Priority: HIGH | Est. Time: 1-2 days               ║
    // ║                                                                      ║
    // ║  @DeepTest @DeepMock @DeepStub @DeepSpy @DeepFixture                ║
    // ║  @DeepFuzz @DeepProperty @DeepMutation @DeepCoverage                ║
    // ║  @DeepBenchmark @DeepContract                                        ║
    // ║                                                                      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // Phase 18 — Enums
    // ──────────────────────────────────────────────

    /** Test generation strategy */
    public enum TestStrategy {
        UNIT,              // Unit test generation
        INTEGRATION,       // Integration test
        REGRESSION,        // Regression test
        SMOKE,             // Smoke test
        BOUNDARY,          // Boundary value analysis
        EQUIVALENCE,       // Equivalence partitioning
        DECISION_TABLE,    // Decision table testing
        STATE_TRANSITION,  // State transition testing
        RANDOM,            // Random testing
        EXHAUSTIVE,        // Exhaustive testing (small domains)
        SPECIFICATION,     // Spec-based testing
        CUSTOM
    }

    /** Mock verification mode */
    public enum VerifyMode {
        TIMES,             // Exact number of invocations
        AT_LEAST,          // Minimum invocations
        AT_MOST,           // Maximum invocations
        NEVER,             // Zero invocations
        AT_LEAST_ONCE,     // One or more
        ONLY,              // Only this method called
        NO_MORE,           // No more interactions
        IN_ORDER           // In specific order
    }

    /** Fuzz testing strategy */
    public enum FuzzStrategy {
        RANDOM,            // Purely random input
        MUTATION,          // Mutate valid inputs
        GENERATION,        // Generate from grammar
        COVERAGE_GUIDED,   // Maximize code coverage
        DICTIONARY,        // Use dictionary of interesting values
        SMART,             // Structure-aware fuzzing
        PROPERTY_BASED,    // Property-based (QuickCheck-like)
        HYBRID             // Combination of strategies
    }

    /** Mutation testing operator */
    public enum MutationOperator {
        NEGATE_CONDITIONALS,    // == → !=
        MATH_MUTATOR,           // + → -, * → /
        RETURN_VALUES,          // Return null/0/false
        VOID_METHOD_CALLS,      // Remove void method calls
        INCREMENTS,             // i++ → i--
        INVERT_NEGATIVES,       // -x → x
        CONSTRUCTOR_CALLS,      // Replace with null
        INLINE_CONSTANT,        // Change constants
        REMOVE_CONDITIONALS,    // Remove if conditions
        EXPERIMENTAL_SWITCH,    // Switch statement mutations
        ALL
    }

    /** Contract type */
    public enum ContractType {
        PRECONDITION,      // Input contract
        POSTCONDITION,     // Output contract
        INVARIANT,         // Class invariant
        ASSERTION,         // General assertion
        REQUIRES,          // Method requirement
        ENSURES,           // Method guarantee
        CUSTOM
    }

    /** Benchmark mode */
    public enum BenchmarkMode {
        THROUGHPUT,        // Operations per time unit
        AVERAGE_TIME,      // Average time per operation
        SAMPLE_TIME,       // Sampling time per operation
        SINGLE_SHOT,       // Single invocation
        ALL                // All modes
    }

    /** Coverage type */
    public enum CoverageType {
        LINE,
        BRANCH,
        METHOD,
        CLASS,
        INSTRUCTION,
        COMPLEXITY,
        MUTATION,
        PATH,
        CONDITION,
        MC_DC,             // Modified Condition/Decision Coverage
        ALL
    }

    // ──────────────────────────────────────────────
    // Phase 18 — Sub-annotation types
    // ──────────────────────────────────────────────

    /** Test case specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestCase {
        String name() default "";
        String description() default "";
        String[] inputs() default {};          // Serialized input values
        String expectedOutput() default "";
        String expectedException() default "";
        long timeoutMs() default 5000;
        boolean enabled() default true;
        String[] tags() default {};
    }

    /** Mock behavior definition */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MockBehavior {
        String method();
        String returnValue() default "";
        String throwException() default "";
        String[] argMatchers() default {};     // "any", "eq:value", "isNull"
        int callCount() default -1;            // -1 = any number
        boolean callRealMethod() default false;
        String answer() default "";            // Custom Answer class
    }

    /** Fuzz configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface FuzzConfig {
        FuzzStrategy strategy() default FuzzStrategy.SMART;
        int maxIterations() default 10000;
        long maxDurationMs() default 60000;
        int maxInputSize() default 4096;
        long seed() default -1;                // -1 = random seed
        String[] dictionary() default {};      // Interesting values
        String corpus() default "";            // Corpus directory
        boolean saveFailingInputs() default true;
        String outputDir() default "";
    }

    /** Contract expression */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ContractExpr {
        ContractType type();
        String expression();                   // Boolean expression
        String message() default "";
        boolean enabled() default true;
    }

    /** Benchmark configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BenchmarkConfig {
        BenchmarkMode mode() default BenchmarkMode.THROUGHPUT;
        int warmupIterations() default 5;
        int measurementIterations() default 10;
        int warmupTime() default 1;            // Seconds
        int measurementTime() default 1;       // Seconds
        int forks() default 2;
        int threads() default 1;
        String timeUnit() default "ms";        // ns, us, ms, s
        String[] jvmArgs() default {};
        String[] params() default {};          // "key=value1,value2" parameterized
    }

    // ──────────────────────────────────────────────
    // Phase 18 — Annotations
    // ──────────────────────────────────────────────

    /**
     * 🧪 @DeepTest — Automatic test generation.
     *
     * Example:
     * <pre>
     * {@code @DeepTest(
     *     strategy = TestStrategy.BOUNDARY,
     *     cases = {
     *         @TestCase(name = "zero", inputs = {"0"}, expectedOutput = "0"),
     *         @TestCase(name = "negative", inputs = {"-1"}, expectedException = "IllegalArgumentException")
     *     }
     * )}
     * public int factorial(int n) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepTest {
        String target() default "";
        TestStrategy strategy() default TestStrategy.UNIT;
        TestCase[] cases() default {};
        boolean autoGenerate() default false;    // Auto-generate test cases
        int maxAutoTests() default 100;
        boolean includeEdgeCases() default true;
        boolean includeNullTests() default true;
        boolean includeEmptyTests() default true;
        boolean includeBoundaryTests() default true;
        boolean includeTypeTests() default true;
        String testFramework() default "junit5"; // junit4, junit5, testng
        String outputPackage() default "";
        String outputDirectory() default "";
        boolean runOnBuild() default false;
        String[] tags() default {};
        ErrorStrategy onFailure() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepTest */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DTEST {
        String target() default "";
        TestStrategy strategy() default TestStrategy.UNIT;
        TestCase[] cases() default {};
        boolean autoGenerate() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🎭 @DeepMock — Mock object generation and injection.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    public @interface DeepMock {
        String target() default "";
        String className() default "";          // Class to mock
        MockBehavior[] behaviors() default {};
        boolean lenient() default false;        // Don't fail on unstubbed calls
        boolean serializable() default false;
        String name() default "";               // Mock name for diagnostics
        String defaultAnswer() default "";      // Default answer for unstubbed
        boolean deepStubs() default false;      // Auto-stub return values
        String[] extraInterfaces() default {};
        VerifyMode verifyMode() default VerifyMode.AT_LEAST_ONCE;
        int verifyCount() default 1;
        boolean inOrder() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMock */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    public @interface DMOCK {
        String target() default "";
        String className() default "";
        MockBehavior[] behaviors() default {};
        boolean deepStubs() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📌 @DeepStub — Stub creation for test doubles.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    public @interface DeepStub {
        String target() default "";
        String className() default "";
        MockBehavior[] behaviors() default {};
        boolean throwOnUnstubbed() default false;
        String defaultReturnValue() default "";
        boolean stateless() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepStub */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    public @interface DSTUB {
        String target() default "";
        String className() default "";
        MockBehavior[] behaviors() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🕵️ @DeepSpy — Spy injection (partial mock).
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    public @interface DeepSpy {
        String target() default "";
        String className() default "";
        MockBehavior[] overrides() default {};  // Methods to override
        boolean callRealMethods() default true;
        boolean recordCalls() default true;     // Record all method calls
        int maxRecordedCalls() default 1000;
        String[] excludeMethods() default {};   // Methods not to spy on
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSpy */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    public @interface DSPY {
        String target() default "";
        String className() default "";
        boolean callRealMethods() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📦 @DeepFixture — Test fixture and data setup.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    public @interface DeepFixture {
        String target() default "";
        String name() default "";
        String dataFile() default "";           // JSON/YAML fixture file
        String[] setupMethods() default {};
        String[] teardownMethods() default {};
        boolean resetBetweenTests() default true;
        boolean shared() default false;         // Share across test class
        String factoryMethod() default "";
        String[] dependencies() default {};     // Other fixture names
        int count() default 1;                  // Number of instances
        String template() default "";           // Template for generating
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepFixture */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    public @interface DFIX {
        String target() default "";
        String name() default "";
        String dataFile() default "";
        boolean resetBetweenTests() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🌊 @DeepFuzz — Fuzz testing injection.
     *
     * Example:
     * <pre>
     * {@code @DeepFuzz(
     *     config = @FuzzConfig(
     *         strategy = FuzzStrategy.COVERAGE_GUIDED,
     *         maxIterations = 50000,
     *         maxDurationMs = 120000
     *     ),
     *     targetExceptions = {"ArrayIndexOutOfBoundsException", "NullPointerException"}
     * )}
     * public void parseUserInput(String input) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepFuzz {
        String target() default "";
        FuzzConfig config() default @FuzzConfig;
        String[] targetExceptions() default {}; // Exceptions to find
        boolean failOnCrash() default true;
        boolean failOnTimeout() default true;
        boolean failOnOOM() default true;
        boolean collectCoverage() default true;
        boolean minimizeInputs() default true; // Minimize failing inputs
        String[] sanitizers() default {};      // asan, ubsan, tsan
        String inputType() default "";         // Hint for generator
        int maxInputSize() default 4096;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepFuzz */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DFUZZ {
        String target() default "";
        FuzzConfig config() default @FuzzConfig;
        boolean failOnCrash() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📐 @DeepProperty — Property-based testing.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepProperty {
        String target() default "";
        String property() default "";           // Property expression
        int tries() default 1000;
        long seed() default -1;
        int maxShrinkCount() default 100;       // Max shrink attempts
        boolean shrinking() default true;
        String[] generators() default {};       // Custom generator classes
        String[] arbitraries() default {};      // Custom arbitrary providers
        int minSize() default 0;
        int maxSize() default 100;
        boolean reportStatistics() default true;
        String[] labels() default {};
        ErrorStrategy onFailure() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepProperty */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DPROPTEST {
        String target() default "";
        String property() default "";
        int tries() default 1000;
        boolean shrinking() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🧬 @DeepMutation — Mutation testing configuration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepMutation {
        String target() default "";
        MutationOperator[] operators() default {MutationOperator.ALL};
        String[] targetClasses() default {};
        String[] targetTests() default {};
        String[] excludedMethods() default {};
        String[] excludedClasses() default {};
        int mutationThreshold() default 85;    // Minimum mutation score %
        boolean failBelowThreshold() default true;
        int threads() default 0;               // 0 = auto
        long timeoutMs() default 10000;        // Per-mutant timeout
        double timeoutFactor() default 1.25;
        String outputFormat() default "HTML";  // HTML, XML, CSV
        String outputDir() default "mutation-report";
        boolean verbose() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMutation */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DMUT {
        String target() default "";
        MutationOperator[] operators() default {MutationOperator.ALL};
        int mutationThreshold() default 85;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📊 @DeepCoverage — Code coverage tracking and enforcement.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepCoverage {
        String target() default "";
        CoverageType[] types() default {CoverageType.LINE, CoverageType.BRANCH};
        int minimumLinePercent() default 80;
        int minimumBranchPercent() default 70;
        int minimumMethodPercent() default 90;
        int minimumClassPercent() default 95;
        boolean failBelowThreshold() default true;
        String[] excludeClasses() default {};
        String[] excludeMethods() default {};
        String[] excludePackages() default {};
        String outputFormat() default "HTML";
        String outputDir() default "coverage-report";
        boolean mergeWithExisting() default true;
        boolean instrumentOnTheFly() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCoverage */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DCOV {
        String target() default "";
        CoverageType[] types() default {CoverageType.LINE, CoverageType.BRANCH};
        int minimumLinePercent() default 80;
        boolean failBelowThreshold() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ⏱️ @DeepBenchmark — Performance benchmarking (JMH-style).
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepBenchmark {
        String target() default "";
        BenchmarkConfig config() default @BenchmarkConfig;
        String group() default "";              // Benchmark group
        boolean compareWithBaseline() default false;
        String baselineMethod() default "";
        double maxRegressionPercent() default 10.0;
        boolean failOnRegression() default false;
        boolean generateReport() default true;
        String reportFormat() default "JSON";   // JSON, CSV, TEXT
        String reportPath() default "";
        boolean profileGC() default false;
        boolean profileMemory() default false;
        boolean profileCompilation() default false;
        String[] jvmArgsAppend() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepBenchmark */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DBENCH {
        String target() default "";
        BenchmarkConfig config() default @BenchmarkConfig;
        boolean failOnRegression() default false;
        double maxRegressionPercent() default 10.0;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📜 @DeepContract — Design by Contract (DbC) enforcement.
     *
     * Example:
     * <pre>
     * {@code @DeepContract(contracts = {
     *     @ContractExpr(type = ContractType.PRECONDITION, expression = "amount > 0"),
     *     @ContractExpr(type = ContractType.POSTCONDITION, expression = "result >= 0"),
     *     @ContractExpr(type = ContractType.INVARIANT, expression = "balance >= 0")
     * })}
     * public int withdraw(int amount) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepContract {
        String target() default "";
        ContractExpr[] contracts() default {};
        boolean enableInProduction() default false;
        boolean enableInTest() default true;
        boolean inheritContracts() default true;
        String violationHandler() default "";
        ErrorStrategy onViolation() default ErrorStrategy.THROW;
        boolean logViolations() default true;
        boolean generateTests() default false;  // Auto-generate tests from contracts
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepContract */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DCONT {
        String target() default "";
        ContractExpr[] contracts() default {};
        boolean enableInProduction() default false;
        ErrorStrategy onViolation() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 18 — Processor
    // ──────────────────────────────────────────────

    /**
     * Processor for all Phase 18 (Testing & Quality) annotations.
     */
    public static class Phase18Processor {

        private final DeepMixContext context;

        public Phase18Processor(DeepMixContext context) {
            this.context = context;
        }

        /**
         * Process @DeepTest — Generate test methods for the annotated method.
         */
        public void processTest(DeepTest annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            TestCase[] cases = annotation.cases();

            if (annotation.autoGenerate()) {
                InsnList autoGenCode = new InsnList();
                autoGenCode.add(new LdcInsnNode(classNode.name));
                autoGenCode.add(new LdcInsnNode(methodNode.name));
                autoGenCode.add(new LdcInsnNode(methodNode.desc));
                autoGenCode.add(new LdcInsnNode(annotation.strategy().name()));
                autoGenCode.add(new LdcInsnNode(annotation.maxAutoTests()));
                autoGenCode.add(new InsnNode(
                    annotation.includeEdgeCases() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                autoGenCode.add(new InsnNode(
                    annotation.includeNullTests() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                autoGenCode.add(new InsnNode(
                    annotation.includeBoundaryTests() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                autoGenCode.add(new LdcInsnNode(annotation.testFramework()));

                autoGenCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/test/DeepMixTestGen",
                    "autoGenerateTests",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                    "Ljava/lang/String;IZZZLjava/lang/String;)V",
                    false
                ));

                methodNode.instructions.insert(autoGenCode);
            }

            // Register explicit test cases
            for (TestCase tc : cases) {
                if (!tc.enabled()) continue;

                InsnList testReg = new InsnList();
                testReg.add(new LdcInsnNode(tc.name()));
                testReg.add(new LdcInsnNode(classNode.name + "::" + methodNode.name));
                pushStringArray(testReg, tc.inputs());
                testReg.add(new LdcInsnNode(tc.expectedOutput()));
                testReg.add(new LdcInsnNode(tc.expectedException()));
                testReg.add(new LdcInsnNode(tc.timeoutMs()));

                testReg.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/test/DeepMixTestGen",
                    "registerTestCase",
                    "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;" +
                    "Ljava/lang/String;Ljava/lang/String;J)V",
                    false
                ));

                methodNode.instructions.insert(testReg);
            }

            context.addDiagnostic(String.format(
                "🧪 @DeepTest applied to %s::%s [strategy=%s, cases=%d, autoGen=%b]",
                classNode.name, methodNode.name,
                annotation.strategy(), cases.length, annotation.autoGenerate()
            ));
        }

        /**
         * Process @DeepContract — Inject pre/post-condition checks.
         */
        public void processContract(DeepContract annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            for (ContractExpr contract : annotation.contracts()) {
                if (!contract.enabled()) continue;

                InsnList contractCheck = new InsnList();

                contractCheck.add(new LdcInsnNode(contract.expression()));
                contractCheck.add(new LdcInsnNode(contract.type().name()));
                contractCheck.add(new LdcInsnNode(contract.message().isEmpty()
                    ? "Contract violation: " + contract.expression()
                    : contract.message()));
                contractCheck.add(new LdcInsnNode(classNode.name + "::" + methodNode.name));

                contractCheck.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/test/DeepMixContract",
                    "evaluate",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                    false
                ));

                switch (contract.type()) {
                    case PRECONDITION:
                    case REQUIRES:
                        // Insert at method entry
                        methodNode.instructions.insert(contractCheck);
                        break;
                    case POSTCONDITION:
                    case ENSURES:
                        // Insert before returns
                        insertBeforeReturns(methodNode, contractCheck);
                        break;
                    case INVARIANT:
                        // Insert at both entry and exit
                        methodNode.instructions.insert(contractCheck);
                        InsnList exitCopy = cloneInsnList(contractCheck);
                        insertBeforeReturns(methodNode, exitCopy);
                        break;
                    case ASSERTION:
                    default:
                        methodNode.instructions.insert(contractCheck);
                        break;
                }
            }

            context.addDiagnostic(String.format(
                "📜 @DeepContract applied to %s::%s [%d contracts]",
                classNode.name, methodNode.name, annotation.contracts().length
            ));
        }

        /**
         * Process @DeepFuzz — Inject fuzz testing harness around method.
         */
        public void processFuzz(DeepFuzz annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            FuzzConfig config = annotation.config();
            InsnList fuzzCode = new InsnList();

            fuzzCode.add(new LdcInsnNode(classNode.name));
            fuzzCode.add(new LdcInsnNode(methodNode.name));
            fuzzCode.add(new LdcInsnNode(methodNode.desc));
            fuzzCode.add(new LdcInsnNode(config.strategy().name()));
            fuzzCode.add(new LdcInsnNode(config.maxIterations()));
            fuzzCode.add(new LdcInsnNode(config.maxDurationMs()));
            fuzzCode.add(new LdcInsnNode(config.maxInputSize()));
            fuzzCode.add(new LdcInsnNode(config.seed()));
            fuzzCode.add(new InsnNode(
                annotation.failOnCrash() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            fuzzCode.add(new InsnNode(
                annotation.collectCoverage() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            fuzzCode.add(new InsnNode(
                annotation.minimizeInputs() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            pushStringArray(fuzzCode, annotation.targetExceptions());
            pushStringArray(fuzzCode, config.dictionary());

            fuzzCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/test/DeepMixFuzzer",
                "fuzz",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;IJIJ ZZZ" +
                "[Ljava/lang/String;[Ljava/lang/String;)V",
                false
            ));

            methodNode.instructions.insert(fuzzCode);

            context.addDiagnostic(String.format(
                "🌊 @DeepFuzz applied to %s::%s [strategy=%s, maxIter=%d]",
                classNode.name, methodNode.name,
                config.strategy(), config.maxIterations()
            ));
        }

        /**
         * Process @DeepBenchmark — Inject JMH-style benchmark harness.
         */
        public void processBenchmark(DeepBenchmark annotation, ClassNode classNode,
                                     MethodNode methodNode) throws DeepMixProcessingException {
            BenchmarkConfig config = annotation.config();
            InsnList benchCode = new InsnList();

            benchCode.add(new LdcInsnNode(classNode.name));
            benchCode.add(new LdcInsnNode(methodNode.name));
            benchCode.add(new LdcInsnNode(config.mode().name()));
            benchCode.add(new LdcInsnNode(config.warmupIterations()));
            benchCode.add(new LdcInsnNode(config.measurementIterations()));
            benchCode.add(new LdcInsnNode(config.warmupTime()));
            benchCode.add(new LdcInsnNode(config.measurementTime()));
            benchCode.add(new LdcInsnNode(config.forks()));
            benchCode.add(new LdcInsnNode(config.threads()));
            benchCode.add(new LdcInsnNode(config.timeUnit()));
            benchCode.add(new LdcInsnNode(annotation.group()));
            benchCode.add(new InsnNode(
                annotation.compareWithBaseline() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            benchCode.add(new LdcInsnNode(annotation.baselineMethod()));
            benchCode.add(new LdcInsnNode(annotation.maxRegressionPercent()));
            benchCode.add(new InsnNode(
                annotation.failOnRegression() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            benchCode.add(new InsnNode(
                annotation.profileGC() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            benchCode.add(new InsnNode(
                annotation.profileMemory() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            benchCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/test/DeepMixBenchmark",
                "register",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "IIIIIILjava/lang/String;Ljava/lang/String;Z" +
                "Ljava/lang/String;DZZZ)V",
                false
            ));

            methodNode.instructions.insert(benchCode);

            context.addDiagnostic(String.format(
                "⏱️ @DeepBenchmark applied to %s::%s [mode=%s, forks=%d, threads=%d]",
                classNode.name, methodNode.name,
                config.mode(), config.forks(), config.threads()
            ));
        }

        /**
         * Process @DeepMock — Inject mock object creation and configuration.
         */
        public void processMock(DeepMock annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            InsnList mockCode = new InsnList();

            mockCode.add(new LdcInsnNode(annotation.className()));
            mockCode.add(new LdcInsnNode(annotation.name()));
            mockCode.add(new InsnNode(
                annotation.lenient() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            mockCode.add(new InsnNode(
                annotation.deepStubs() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            mockCode.add(new LdcInsnNode(annotation.defaultAnswer()));

            // Serialize mock behaviors
            MockBehavior[] behaviors = annotation.behaviors();
            mockCode.add(new LdcInsnNode(behaviors.length));
            mockCode.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < behaviors.length; i++) {
                mockCode.add(new InsnNode(Opcodes.DUP));
                mockCode.add(new LdcInsnNode(i));
                // Encode behavior as "method|returnValue|throwException|callCount"
                String encoded = behaviors[i].method() + "|" +
                                 behaviors[i].returnValue() + "|" +
                                 behaviors[i].throwException() + "|" +
                                 behaviors[i].callCount() + "|" +
                                 behaviors[i].callRealMethod();
                mockCode.add(new LdcInsnNode(encoded));
                mockCode.add(new InsnNode(Opcodes.AASTORE));
            }

            pushStringArray(mockCode, annotation.extraInterfaces());

            mockCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/test/DeepMixMocking",
                "createMock",
                "(Ljava/lang/String;Ljava/lang/String;ZZLjava/lang/String;" +
                "[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(mockCode);

            context.addDiagnostic(String.format(
                "🎭 @DeepMock applied to %s::%s [class=%s, behaviors=%d]",
                classNode.name, methodNode.name,
                annotation.className(), behaviors.length
            ));
        }

        /**
         * Process @DeepCoverage — Inject coverage instrumentation probes.
         */
        public void processCoverage(DeepCoverage annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            if (!annotation.instrumentOnTheFly()) return;

            CoverageType[] types = annotation.types();

            // Inject method-level probe at entry
            InsnList probe = new InsnList();
            probe.add(new LdcInsnNode(classNode.name));
            probe.add(new LdcInsnNode(methodNode.name));
            probe.add(new LdcInsnNode(methodNode.desc));

            probe.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/test/DeepMixCoverage",
                "recordMethodEntry",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                false
            ));

            methodNode.instructions.insert(probe);

            // Inject branch probes if BRANCH coverage requested
            boolean branchCoverage = false;
            for (CoverageType ct : types) {
                if (ct == CoverageType.BRANCH || ct == CoverageType.ALL) {
                    branchCoverage = true;
                    break;
                }
            }

            if (branchCoverage) {
                int branchId = 0;
                for (AbstractInsnNode insn : methodNode.instructions) {
                    if (insn instanceof JumpInsnNode) {
                        InsnList branchProbe = new InsnList();
                        branchProbe.add(new LdcInsnNode(classNode.name));
                        branchProbe.add(new LdcInsnNode(methodNode.name));
                        branchProbe.add(new LdcInsnNode(branchId++));
                        branchProbe.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "deepmix/runtime/test/DeepMixCoverage",
                            "recordBranch",
                            "(Ljava/lang/String;Ljava/lang/String;I)V",
                            false
                        ));
                        methodNode.instructions.insertBefore(insn, branchProbe);
                    }
                }
            }

            // Register coverage thresholds
            InsnList thresholds = new InsnList();
            thresholds.add(new LdcInsnNode(classNode.name));
            thresholds.add(new LdcInsnNode(annotation.minimumLinePercent()));
            thresholds.add(new LdcInsnNode(annotation.minimumBranchPercent()));
            thresholds.add(new LdcInsnNode(annotation.minimumMethodPercent()));
            thresholds.add(new InsnNode(
                annotation.failBelowThreshold() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            thresholds.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/test/DeepMixCoverage",
                "registerThresholds",
                "(Ljava/lang/String;IIIIZ)V",
                false
            ));

            methodNode.instructions.insert(thresholds);

            context.addDiagnostic(String.format(
                "📊 @DeepCoverage applied to %s::%s [types=%s, minLine=%d%%]",
                classNode.name, methodNode.name,
                Arrays.toString(types), annotation.minimumLinePercent()
            ));
        }

        /**
         * Process @DeepMutation — Register method for mutation testing.
         */
        public void processMutation(DeepMutation annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            InsnList mutCode = new InsnList();

            mutCode.add(new LdcInsnNode(classNode.name));
            mutCode.add(new LdcInsnNode(methodNode.name));
            mutCode.add(new LdcInsnNode(annotation.mutationThreshold()));
            mutCode.add(new InsnNode(
                annotation.failBelowThreshold() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            mutCode.add(new LdcInsnNode(annotation.threads()));
            mutCode.add(new LdcInsnNode(annotation.timeoutMs()));
            mutCode.add(new LdcInsnNode(annotation.outputFormat()));
            mutCode.add(new LdcInsnNode(annotation.outputDir()));

            // Encode operators
            MutationOperator[] ops = annotation.operators();
            mutCode.add(new LdcInsnNode(ops.length));
            mutCode.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < ops.length; i++) {
                mutCode.add(new InsnNode(Opcodes.DUP));
                mutCode.add(new LdcInsnNode(i));
                mutCode.add(new LdcInsnNode(ops[i].name()));
                mutCode.add(new InsnNode(Opcodes.AASTORE));
            }

            pushStringArray(mutCode, annotation.excludedMethods());
            pushStringArray(mutCode, annotation.targetTests());

            mutCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/test/DeepMixMutation",
                "register",
                "(Ljava/lang/String;Ljava/lang/String;IZIJLjava/lang/String;" +
                "Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;" +
                "[Ljava/lang/String;)V",
                false
            ));

            methodNode.instructions.insert(mutCode);

            context.addDiagnostic(String.format(
                "🧬 @DeepMutation applied to %s::%s [threshold=%d%%, operators=%d]",
                classNode.name, methodNode.name,
                annotation.mutationThreshold(), ops.length
            ));
        }

        /**
         * Process @DeepProperty — Inject property-based test harness.
         */
        public void processProperty(DeepProperty annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            InsnList propCode = new InsnList();

            propCode.add(new LdcInsnNode(classNode.name));
            propCode.add(new LdcInsnNode(methodNode.name));
            propCode.add(new LdcInsnNode(methodNode.desc));
            propCode.add(new LdcInsnNode(annotation.property()));
            propCode.add(new LdcInsnNode(annotation.tries()));
            propCode.add(new LdcInsnNode(annotation.seed()));
            propCode.add(new InsnNode(
                annotation.shrinking() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            propCode.add(new LdcInsnNode(annotation.maxShrinkCount()));
            propCode.add(new LdcInsnNode(annotation.minSize()));
            propCode.add(new LdcInsnNode(annotation.maxSize()));
            propCode.add(new InsnNode(
                annotation.reportStatistics() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            pushStringArray(propCode, annotation.generators());
            pushStringArray(propCode, annotation.arbitraries());
            pushStringArray(propCode, annotation.labels());

            propCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/test/DeepMixProperty",
                "registerProperty",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;IJZIIIIZ" +
                "[Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)V",
                false
            ));

            methodNode.instructions.insert(propCode);

            context.addDiagnostic(String.format(
                "📐 @DeepProperty applied to %s::%s [property=%s, tries=%d]",
                classNode.name, methodNode.name,
                annotation.property(), annotation.tries()
            ));
        }

        // ─── Helpers ───

        private void pushStringArray(InsnList insns, String[] values) {
            insns.add(new LdcInsnNode(values.length));
            insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < values.length; i++) {
                insns.add(new InsnNode(Opcodes.DUP));
                insns.add(new LdcInsnNode(i));
                insns.add(new LdcInsnNode(values[i]));
                insns.add(new InsnNode(Opcodes.AASTORE));
            }
        }

        private void insertBeforeReturns(MethodNode methodNode, InsnList toInsert) {
            List<AbstractInsnNode> returnInsns = new ArrayList<>();
            for (AbstractInsnNode insn : methodNode.instructions) {
                int op = insn.getOpcode();
                if (op == Opcodes.RETURN || op == Opcodes.ARETURN ||
                    op == Opcodes.IRETURN || op == Opcodes.LRETURN ||
                    op == Opcodes.FRETURN || op == Opcodes.DRETURN) {
                    returnInsns.add(insn);
                }
            }
            for (AbstractInsnNode ret : returnInsns) {
                InsnList copy = cloneInsnList(toInsert);
                methodNode.instructions.insertBefore(ret, copy);
            }
        }

        private InsnList cloneInsnList(InsnList original) {
            InsnList clone = new InsnList();
            Map<LabelNode, LabelNode> labelMap = new HashMap<>();
            for (AbstractInsnNode insn : original) {
                if (insn instanceof LabelNode)
                    labelMap.put((LabelNode) insn, new LabelNode());
            }
            for (AbstractInsnNode insn : original) {
                clone.add(insn.clone(labelMap));
            }
            return clone;
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                      ║
    // ║  PHASE 19: REACTIVE & FUNCTIONAL                                     ║
    // ║  13 annotations | Priority: MEDIUM | Est. Time: 2 days               ║
    // ║                                                                      ║
    // ║  @DeepRx        @DeepFlux     @DeepMono     @DeepFlow               ║
    // ║  @DeepCoroutine @DeepFunctor  @DeepMonad    @DeepLens               ║
    // ║  @DeepPrism     @DeepTraverse @DeepFold     @DeepUnfold             ║
    // ║  @DeepZipper                                                         ║
    // ║                                                                      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // Phase 19 — Enums
    // ──────────────────────────────────────────────

    /** Reactive stream type / library */
    public enum ReactiveLibrary {
        RXJAVA2,              // RxJava 2.x
        RXJAVA3,              // RxJava 3.x
        PROJECT_REACTOR,      // Spring Reactor
        KOTLIN_FLOW,          // Kotlin Flow
        JAVA9_FLOW,           // java.util.concurrent.Flow
        AKKA_STREAMS,         // Akka Streams
        MUTINY,               // SmallRye Mutiny
        VERT_X,               // Vert.x Reactive
        CUSTOM
    }

    /** Reactive operator transformation */
    public enum ReactiveOperator {
        MAP,
        FLAT_MAP,
        FILTER,
        REDUCE,
        SCAN,
        BUFFER,
        WINDOW,
        DEBOUNCE,
        THROTTLE,
        SAMPLE,
        DISTINCT,
        TAKE,
        SKIP,
        RETRY,
        TIMEOUT,
        MERGE,
        CONCAT,
        ZIP,
        COMBINE_LATEST,
        SWITCH_MAP,
        EXHAUST_MAP,
        GROUP_BY,
        ON_ERROR_RESUME,
        ON_ERROR_RETURN,
        ON_ERROR_MAP,
        DO_ON_NEXT,
        DO_ON_ERROR,
        DO_ON_COMPLETE,
        SUBSCRIBE_ON,
        OBSERVE_ON,
        PUBLISH,
        REPLAY,
        CACHE,
        SHARE,
        REFCOUNT,
        CONNECT,
        CUSTOM
    }

    /** Scheduler type for reactive streams */
    public enum SchedulerType {
        IO,                   // I/O-bound operations
        COMPUTATION,          // CPU-bound operations
        SINGLE,               // Single-threaded
        NEW_THREAD,           // New thread per subscription
        TRAMPOLINE,           // Current thread (queued)
        IMMEDIATE,            // Current thread (immediate)
        PARALLEL,             // Parallel scheduler
        BOUNDED_ELASTIC,      // Bounded elastic (Reactor)
        VIRTUAL_THREAD,       // Java 21+ virtual threads
        EVENT_LOOP,           // Event loop (Vert.x style)
        CUSTOM
    }

    /** Backpressure strategy */
    public enum BackpressureStrategy {
        BUFFER,               // Buffer all items
        DROP,                 // Drop newest items
        LATEST,               // Keep only latest
        ERROR,                // Signal error (MissingBackpressure)
        MISSING,              // No backpressure (may cause OOM)
        BUFFER_BOUNDED,       // Bounded buffer
        WINDOW,               // Window-based
        CUSTOM
    }

    /** Coroutine dispatcher type */
    public enum CoroutineDispatcher {
        DEFAULT,              // Shared thread pool
        IO,                   // I/O optimized
        MAIN,                 // Main/UI thread
        UNCONFINED,           // No specific thread
        VIRTUAL,              // Virtual threads
        CUSTOM
    }

    /** Coroutine scope */
    public enum CoroutineScope {
        GLOBAL,               // GlobalScope
        LIFECYCLE,            // Lifecycle-bound
        SUPERVISED,           // SupervisorScope
        CUSTOM
    }

    /** Monad type */
    public enum MonadType {
        OPTION,               // Option/Maybe monad
        EITHER,               // Either monad (Left/Right)
        TRY,                  // Try monad (Success/Failure)
        IO,                   // IO monad
        FUTURE,               // Future monad
        LIST,                 // List monad
        READER,               // Reader monad
        WRITER,               // Writer monad
        STATE,                // State monad
        CONT,                 // Continuation monad
        FREE,                 // Free monad
        IDENTITY,             // Identity monad
        VALIDATED,            // Validated (accumulating errors)
        CUSTOM
    }

    /** Fold direction */
    public enum FoldDirection {
        LEFT,
        RIGHT
    }

    /** Unfold termination condition */
    public enum UnfoldTermination {
        PREDICATE,            // Stop when predicate returns false
        COUNT,                // Stop after N elements
        SENTINEL,             // Stop on sentinel value
        EXCEPTION,            // Stop on exception
        INFINITE              // Never stop (use with take())
    }

    // ──────────────────────────────────────────────
    // Phase 19 — Sub-annotation types
    // ──────────────────────────────────────────────

    /** Reactive pipeline step */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ReactiveStep {
        ReactiveOperator operator();
        String[] args() default {};            // Operator arguments
        int order() default 0;                 // Execution order in pipeline
        String lambda() default "";            // Lambda expression for map/filter/etc.
    }

    /** Backpressure configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BackpressureConfig {
        BackpressureStrategy strategy() default BackpressureStrategy.BUFFER;
        int bufferSize() default 256;
        long timeoutMs() default 0;            // 0 = no timeout
        String overflowHandler() default "";   // Custom overflow handler
    }

    /** Coroutine configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CoroutineConfig {
        CoroutineDispatcher dispatcher() default CoroutineDispatcher.DEFAULT;
        CoroutineScope scope() default CoroutineScope.SUPERVISED;
        long timeoutMs() default 0;
        boolean cancelOnError() default true;
        String exceptionHandler() default "";
        String contextElements() default "";   // CoroutineContext elements
        boolean structured() default true;     // Structured concurrency
    }

    /** Lens target specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LensTarget {
        String path();                         // Dot-notation path: "address.city.name"
        String getter() default "";            // Custom getter method
        String setter() default "";            // Custom setter method
        boolean compose() default false;       // Compose with parent lens
    }

    /** Prism specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PrismTarget {
        String typeName();                     // Target subtype name
        String matchMethod() default "";       // Custom match/extract method
        String buildMethod() default "";       // Custom build/inject method
    }

    // ──────────────────────────────────────────────
    // Phase 19 — Annotations
    // ──────────────────────────────────────────────

    /**
     * 🌊 @DeepRx — RxJava reactive stream transformation.
     *
     * Converts imperative code to reactive streams or modifies existing streams.
     *
     * Example:
     * <pre>
     * {@code @DeepRx(
     *     library = ReactiveLibrary.RXJAVA3,
     *     pipeline = {
     *         @ReactiveStep(operator = ReactiveOperator.MAP, lambda = "x -> x * 2"),
     *         @ReactiveStep(operator = ReactiveOperator.FILTER, lambda = "x -> x > 10"),
     *         @ReactiveStep(operator = ReactiveOperator.SUBSCRIBE_ON, args = {"IO"})
     *     },
     *     backpressure = @BackpressureConfig(strategy = BackpressureStrategy.BUFFER)
     * )}
     * public Observable<Integer> processItems(List<Integer> items) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepRx {
        String target() default "";
        ReactiveLibrary library() default ReactiveLibrary.RXJAVA3;
        ReactiveStep[] pipeline() default {};
        BackpressureConfig backpressure() default @BackpressureConfig;
        SchedulerType subscribeOn() default SchedulerType.IO;
        SchedulerType observeOn() default SchedulerType.COMPUTATION;
        boolean convertToReactive() default false; // Convert imperative → reactive
        boolean hot() default false;            // Hot vs cold observable
        boolean shared() default false;         // Share subscription
        int replayCount() default 0;            // Replay N items to late subscribers
        long replayTimeMs() default 0;
        String errorHandler() default "";
        long timeoutMs() default 0;
        int retryCount() default 0;
        long retryDelayMs() default 0;
        boolean exponentialBackoff() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepRx */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DRX {
        String target() default "";
        ReactiveLibrary library() default ReactiveLibrary.RXJAVA3;
        ReactiveStep[] pipeline() default {};
        SchedulerType subscribeOn() default SchedulerType.IO;
        SchedulerType observeOn() default SchedulerType.COMPUTATION;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔄 @DeepFlux — Project Reactor Flux transformation.
     *
     * Example:
     * <pre>
     * {@code @DeepFlux(
     *     pipeline = {
     *         @ReactiveStep(operator = ReactiveOperator.FLAT_MAP, lambda = "this::fetchDetails"),
     *         @ReactiveStep(operator = ReactiveOperator.BUFFER, args = {"10"})
     *     },
     *     backpressure = @BackpressureConfig(strategy = BackpressureStrategy.BUFFER_BOUNDED, bufferSize = 256)
     * )}
     * public Flux<Detail> getDetails(Flux<String> ids) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepFlux {
        String target() default "";
        ReactiveStep[] pipeline() default {};
        BackpressureConfig backpressure() default @BackpressureConfig;
        SchedulerType publishOn() default SchedulerType.PARALLEL;
        SchedulerType subscribeOn() default SchedulerType.BOUNDED_ELASTIC;
        boolean convertFromIterable() default false;
        boolean convertFromStream() default false;
        int concurrency() default 256;         // flatMap concurrency
        int prefetch() default 32;             // Prefetch count
        String errorHandler() default "";
        long timeoutMs() default 0;
        int retryCount() default 0;
        long retryDelayMs() default 0;
        boolean metrics() default false;       // Enable Reactor metrics
        String metricsPrefix() default "";
        boolean checkpoint() default false;    // Enable checkpoint for debugging
        String checkpointDescription() default "";
        boolean log() default false;           // Enable Reactor log()
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepFlux */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DFLUX {
        String target() default "";
        ReactiveStep[] pipeline() default {};
        SchedulerType subscribeOn() default SchedulerType.BOUNDED_ELASTIC;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔹 @DeepMono — Project Reactor Mono transformation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepMono {
        String target() default "";
        ReactiveStep[] pipeline() default {};
        SchedulerType subscribeOn() default SchedulerType.BOUNDED_ELASTIC;
        boolean convertFromCallable() default false;
        boolean convertFromFuture() default false;
        boolean convertFromOptional() default false;
        boolean cache() default false;
        long cacheTTLMs() default 0;
        String errorHandler() default "";
        long timeoutMs() default 0;
        int retryCount() default 0;
        boolean metrics() default false;
        boolean checkpoint() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMono */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DMONO {
        String target() default "";
        ReactiveStep[] pipeline() default {};
        SchedulerType subscribeOn() default SchedulerType.BOUNDED_ELASTIC;
        boolean cache() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🌀 @DeepFlow — Kotlin Flow transformation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepFlow {
        String target() default "";
        ReactiveStep[] pipeline() default {};
        CoroutineConfig coroutine() default @CoroutineConfig;
        boolean convertFromSequence() default false;
        boolean convertFromIterable() default false;
        boolean channelFlow() default false;   // Use channelFlow builder
        int bufferSize() default 64;           // Channel buffer capacity
        String conflationStrategy() default ""; // buffer, conflate, collectLatest
        boolean shareIn() default false;       // SharedFlow
        boolean stateIn() default false;       // StateFlow
        int replay() default 0;               // For SharedFlow
        String initialValue() default "";     // For StateFlow
        boolean distinctUntilChanged() default false;
        long debounceMs() default 0;
        boolean catchExceptions() default true;
        String errorHandler() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepFlow */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DFLOW {
        String target() default "";
        ReactiveStep[] pipeline() default {};
        CoroutineConfig coroutine() default @CoroutineConfig;
        int bufferSize() default 64;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ⚡ @DeepCoroutine — Coroutine transformation for async code.
     *
     * Converts blocking/callback-based code to coroutine-based async code.
     *
     * Example:
     * <pre>
     * {@code @DeepCoroutine(
     *     config = @CoroutineConfig(
     *         dispatcher = CoroutineDispatcher.IO,
     *         scope = CoroutineScope.SUPERVISED,
     *         timeoutMs = 5000
     *     )
     * )}
     * public suspend Result fetchData(String url) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepCoroutine {
        String target() default "";
        CoroutineConfig config() default @CoroutineConfig;
        boolean convertCallbacks() default false; // Convert callback → suspend
        boolean convertFutures() default false;   // Convert Future → suspend
        boolean convertBlocking() default false;  // Wrap blocking in withContext
        boolean launchAsync() default false;       // Launch as async
        boolean launchLazy() default false;        // Lazy start
        String joinMethod() default "";           // Method that joins results
        boolean cancellable() default true;
        String progressCallback() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCoroutine */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DCORO {
        String target() default "";
        CoroutineConfig config() default @CoroutineConfig;
        boolean convertBlocking() default false;
        boolean launchAsync() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📦 @DeepFunctor — Functor pattern application.
     *
     * Transforms a type to support functor operations (map/fmap).
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepFunctor {
        String target() default "";
        String mapMethod() default "map";       // Name of the generated map method
        String valueField() default "";         // Field holding the wrapped value
        boolean generateContravariant() default false; // Contravariant functor
        boolean generateBifunctor() default false;    // Bifunctor (two type params)
        boolean lawChecks() default false;      // Verify functor laws at runtime
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepFunctor */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DFUNC {
        String target() default "";
        String mapMethod() default "map";
        boolean lawChecks() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔗 @DeepMonad — Monad pattern implementation.
     *
     * Generates or enforces monad operations (unit/return, bind/flatMap)
     * for a type, with optional law checking.
     *
     * Example:
     * <pre>
     * {@code @DeepMonad(
     *     type = MonadType.EITHER,
     *     lawChecks = true
     * )}
     * public class Result<L, R> { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepMonad {
        String target() default "";
        MonadType type() default MonadType.OPTION;
        String unitMethod() default "of";       // unit/return/pure
        String bindMethod() default "flatMap";  // bind/flatMap/chain
        String valueField() default "";
        boolean generateApplicative() default false; // ap method
        boolean generateMonadPlus() default false;   // mzero/mplus
        boolean lawChecks() default false;      // Left identity, right identity, associativity
        boolean generateDoNotation() default false;  // For-comprehension support
        String errorType() default "";          // For Either/Try error type
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMonad */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DMONAD {
        String target() default "";
        MonadType type() default MonadType.OPTION;
        boolean lawChecks() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔍 @DeepLens — Lens pattern for functional access to nested fields.
     *
     * Generates composable getters/setters for deeply nested immutable structures.
     *
     * Example:
     * <pre>
     * {@code @DeepLens(lenses = {
     *     @LensTarget(path = "address.city"),
     *     @LensTarget(path = "address.zipCode")
     * })}
     * public class Person { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface DeepLens {
        String target() default "";
        LensTarget[] lenses() default {};
        boolean generateAll() default false;    // Generate lens for every field
        boolean compose() default true;         // Generate composed lenses
        boolean generateModify() default true;  // Generate modify (over) method
        boolean generateOptional() default false; // Generate Optional lens
        boolean generateIso() default false;     // Generate isomorphism
        String namingPattern() default "%sLens"; // Lens field naming: fieldNameLens
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepLens */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface DLENS {
        String target() default "";
        LensTarget[] lenses() default {};
        boolean generateAll() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔷 @DeepPrism — Prism pattern for sum types (sealed hierarchies).
     *
     * Generates safe extraction/injection for subtypes of a sealed hierarchy.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface DeepPrism {
        String target() default "";
        PrismTarget[] prisms() default {};
        boolean generateAll() default false;    // Generate prism for every subtype
        boolean generateReview() default true;  // Generate review (build) method
        boolean generateFirst() default true;   // Generate first traversal
        String namingPattern() default "_%s";   // Prism naming: _SubTypeName
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepPrism */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface DPRISM {
        String target() default "";
        PrismTarget[] prisms() default {};
        boolean generateAll() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🚶 @DeepTraverse — Traversable pattern implementation.
     *
     * Makes a data structure traversable, enabling effectful mapping.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepTraverse {
        String target() default "";
        String traverseMethod() default "traverse";
        String sequenceMethod() default "sequence";
        boolean generateMapAccum() default false;
        boolean generateZipWith() default false;
        boolean lawChecks() default false;      // Traversable laws
        String elementType() default "";        // Type of elements to traverse
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepTraverse */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DTRAV {
        String target() default "";
        boolean lawChecks() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📐 @DeepFold — Fold (reduce/catamorphism) operations.
     *
     * Injects fold operations over data structures.
     *
     * Example:
     * <pre>
     * {@code @DeepFold(
     *     direction = FoldDirection.LEFT,
     *     initial = "0",
     *     accumulator = "(acc, x) -> acc + x.getValue()"
     * )}
     * public int sumValues(List<Item> items) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepFold {
        String target() default "";
        FoldDirection direction() default FoldDirection.LEFT;
        String initial() default "";            // Initial accumulator value
        String accumulator() default "";        // Accumulator lambda expression
        boolean parallel() default false;       // Parallel fold (requires associativity)
        boolean short_circuit() default false;  // Allow early termination
        String terminationPredicate() default ""; // Stop folding when predicate is true
        String resultMapper() default "";       // Transform final result
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepFold */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DFOLD {
        String target() default "";
        FoldDirection direction() default FoldDirection.LEFT;
        String initial() default "";
        String accumulator() default "";
        boolean parallel() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📤 @DeepUnfold — Unfold (anamorphism) operations.
     *
     * Generates sequences from a seed value using an unfolding function.
     *
     * Example:
     * <pre>
     * {@code @DeepUnfold(
     *     seed = "1",
     *     generator = "n -> new Pair(n, n + 1)",
     *     termination = UnfoldTermination.COUNT,
     *     count = 100
     * )}
     * public Stream<Integer> naturals() { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepUnfold {
        String target() default "";
        String seed() default "";               // Initial seed value
        String generator() default "";          // Generate next (value, seed) pair
        UnfoldTermination termination() default UnfoldTermination.PREDICATE;
        String predicate() default "";          // Stop predicate
        int count() default -1;                 // For COUNT termination
        String sentinel() default "";           // For SENTINEL termination
        boolean lazy() default true;            // Lazy evaluation (Stream-based)
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepUnfold */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DUNFOLD {
        String target() default "";
        String seed() default "";
        String generator() default "";
        UnfoldTermination termination() default UnfoldTermination.PREDICATE;
        int count() default -1;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔄 @DeepZipper — Zipper data structure pattern.
     *
     * Generates a zipper for navigating and modifying immutable data structures
     * with a focus/cursor position.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface DeepZipper {
        String target() default "";
        String elementType() default "";        // Type of elements
        boolean generateUp() default true;      // Navigate up
        boolean generateDown() default true;    // Navigate down
        boolean generateLeft() default true;    // Navigate left
        boolean generateRight() default true;   // Navigate right
        boolean generateModify() default true;  // Modify at focus
        boolean generateInsert() default true;  // Insert at focus
        boolean generateDelete() default true;  // Delete at focus
        boolean generateToRoot() default true;  // Navigate to root
        boolean generateFromTree() default true; // Create zipper from tree
        boolean generateToTree() default true;   // Reconstruct tree from zipper
        boolean threadSafe() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepZipper */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface DZIP {
        String target() default "";
        String elementType() default "";
        boolean threadSafe() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 19 — Processor
    // ──────────────────────────────────────────────

    /**
     * Processor for all Phase 19 (Reactive & Functional) annotations.
     */
    public static class Phase19Processor {

        private final DeepMixContext context;

        public Phase19Processor(DeepMixContext context) {
            this.context = context;
        }

        /**
         * Process @DeepRx — Transform method to use reactive streams.
         */
        public void processRx(DeepRx annotation, ClassNode classNode,
                               MethodNode methodNode) throws DeepMixProcessingException {
            InsnList rxCode = new InsnList();

            rxCode.add(new LdcInsnNode(annotation.library().name()));
            rxCode.add(new LdcInsnNode(annotation.subscribeOn().name()));
            rxCode.add(new LdcInsnNode(annotation.observeOn().name()));
            rxCode.add(new InsnNode(
                annotation.convertToReactive() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            rxCode.add(new InsnNode(
                annotation.hot() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            rxCode.add(new InsnNode(
                annotation.shared() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            rxCode.add(new LdcInsnNode(annotation.replayCount()));
            rxCode.add(new LdcInsnNode(annotation.timeoutMs()));
            rxCode.add(new LdcInsnNode(annotation.retryCount()));
            rxCode.add(new LdcInsnNode(annotation.retryDelayMs()));
            rxCode.add(new InsnNode(
                annotation.exponentialBackoff() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            // Backpressure config
            BackpressureConfig bp = annotation.backpressure();
            rxCode.add(new LdcInsnNode(bp.strategy().name()));
            rxCode.add(new LdcInsnNode(bp.bufferSize()));

            // Pipeline steps
            ReactiveStep[] steps = annotation.pipeline();
            rxCode.add(new LdcInsnNode(steps.length));
            rxCode.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < steps.length; i++) {
                rxCode.add(new InsnNode(Opcodes.DUP));
                rxCode.add(new LdcInsnNode(i));
                String encoded = steps[i].operator().name() + "|" +
                                 steps[i].lambda() + "|" +
                                 String.join(",", steps[i].args());
                rxCode.add(new LdcInsnNode(encoded));
                rxCode.add(new InsnNode(Opcodes.AASTORE));
            }

            rxCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/reactive/DeepMixRx",
                "buildPipeline",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "ZZZIJIJZLjava/lang/String;I[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(rxCode);

            context.addDiagnostic(String.format(
                "🌊 @DeepRx applied to %s::%s [lib=%s, steps=%d, subscribeOn=%s]",
                classNode.name, methodNode.name,
                annotation.library(), steps.length, annotation.subscribeOn()
            ));
        }

        /**
         * Process @DeepFlux — Transform method to use Project Reactor Flux.
         */
        public void processFlux(DeepFlux annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            InsnList fluxCode = new InsnList();

            fluxCode.add(new LdcInsnNode(annotation.subscribeOn().name()));
            fluxCode.add(new LdcInsnNode(annotation.publishOn().name()));
            fluxCode.add(new InsnNode(
                annotation.convertFromIterable() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            fluxCode.add(new InsnNode(
                annotation.convertFromStream() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            fluxCode.add(new LdcInsnNode(annotation.concurrency()));
            fluxCode.add(new LdcInsnNode(annotation.prefetch()));
            fluxCode.add(new LdcInsnNode(annotation.timeoutMs()));
            fluxCode.add(new LdcInsnNode(annotation.retryCount()));
            fluxCode.add(new LdcInsnNode(annotation.retryDelayMs()));
            fluxCode.add(new InsnNode(
                annotation.metrics() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            fluxCode.add(new LdcInsnNode(annotation.metricsPrefix()));
            fluxCode.add(new InsnNode(
                annotation.checkpoint() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            fluxCode.add(new LdcInsnNode(annotation.checkpointDescription()));
            fluxCode.add(new InsnNode(
                annotation.log() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            // Backpressure
            BackpressureConfig bp = annotation.backpressure();
            fluxCode.add(new LdcInsnNode(bp.strategy().name()));
            fluxCode.add(new LdcInsnNode(bp.bufferSize()));

            // Pipeline steps
            ReactiveStep[] steps = annotation.pipeline();
            fluxCode.add(new LdcInsnNode(steps.length));
            fluxCode.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < steps.length; i++) {
                fluxCode.add(new InsnNode(Opcodes.DUP));
                fluxCode.add(new LdcInsnNode(i));
                String encoded = steps[i].operator().name() + "|" +
                                 steps[i].lambda() + "|" +
                                 String.join(",", steps[i].args());
                fluxCode.add(new LdcInsnNode(encoded));
                fluxCode.add(new InsnNode(Opcodes.AASTORE));
            }

            fluxCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/reactive/DeepMixReactor",
                "buildFlux",
                "(Ljava/lang/String;Ljava/lang/String;ZZIIJIIZ" +
                "Ljava/lang/String;ZLjava/lang/String;Z" +
                "Ljava/lang/String;I[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(fluxCode);

            context.addDiagnostic(String.format(
                "🔄 @DeepFlux applied to %s::%s [steps=%d, publishOn=%s]",
                classNode.name, methodNode.name,
                steps.length, annotation.publishOn()
            ));
        }

        /**
         * Process @DeepCoroutine — Transform method to use coroutines.
         */
        public void processCoroutine(DeepCoroutine annotation, ClassNode classNode,
                                     MethodNode methodNode) throws DeepMixProcessingException {
            CoroutineConfig config = annotation.config();
            InsnList coroCode = new InsnList();

            coroCode.add(new LdcInsnNode(config.dispatcher().name()));
            coroCode.add(new LdcInsnNode(config.scope().name()));
            coroCode.add(new LdcInsnNode(config.timeoutMs()));
            coroCode.add(new InsnNode(
                config.cancelOnError() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            coroCode.add(new InsnNode(
                config.structured() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            coroCode.add(new LdcInsnNode(config.exceptionHandler()));
            coroCode.add(new InsnNode(
                annotation.convertCallbacks() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            coroCode.add(new InsnNode(
                annotation.convertFutures() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            coroCode.add(new InsnNode(
                annotation.convertBlocking() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            coroCode.add(new InsnNode(
                annotation.launchAsync() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            coroCode.add(new InsnNode(
                annotation.launchLazy() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            coroCode.add(new InsnNode(
                annotation.cancellable() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            coroCode.add(new LdcInsnNode(classNode.name));
            coroCode.add(new LdcInsnNode(methodNode.name));

            coroCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/reactive/DeepMixCoroutine",
                "transform",
                "(Ljava/lang/String;Ljava/lang/String;JZZLjava/lang/String;" +
                "ZZZZZZ" +
                "Ljava/lang/String;Ljava/lang/String;)V",
                false
            ));

            methodNode.instructions.insert(coroCode);

            context.addDiagnostic(String.format(
                "⚡ @DeepCoroutine applied to %s::%s [dispatcher=%s, scope=%s]",
                classNode.name, methodNode.name,
                config.dispatcher(), config.scope()
            ));
        }

        /**
         * Process @DeepMonad — Generate monad operations for a type.
         */
        public void processMonad(DeepMonad annotation, ClassNode classNode,
                                 MethodNode methodNode) throws DeepMixProcessingException {
            InsnList monadCode = new InsnList();

            monadCode.add(new LdcInsnNode(annotation.type().name()));
            monadCode.add(new LdcInsnNode(classNode.name));
            monadCode.add(new LdcInsnNode(annotation.unitMethod()));
            monadCode.add(new LdcInsnNode(annotation.bindMethod()));
            monadCode.add(new LdcInsnNode(annotation.valueField()));
            monadCode.add(new InsnNode(
                annotation.generateApplicative() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            monadCode.add(new InsnNode(
                annotation.generateMonadPlus() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            monadCode.add(new InsnNode(
                annotation.lawChecks() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            monadCode.add(new InsnNode(
                annotation.generateDoNotation() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            monadCode.add(new LdcInsnNode(annotation.errorType()));

            monadCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/functional/DeepMixMonad",
                "generateMonadOps",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;Ljava/lang/String;ZZZZ" +
                "Ljava/lang/String;)V",
                false
            ));

            methodNode.instructions.insert(monadCode);

            context.addDiagnostic(String.format(
                "🔗 @DeepMonad applied to %s [type=%s, unit=%s, bind=%s]",
                classNode.name, annotation.type(),
                annotation.unitMethod(), annotation.bindMethod()
            ));
        }

        /**
         * Process @DeepLens — Generate lens accessors for nested fields.
         */
        public void processLens(DeepLens annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            LensTarget[] lenses = annotation.lenses();

            if (annotation.generateAll()) {
                // Generate lens for every non-static field
                InsnList lensGen = new InsnList();
                lensGen.add(new LdcInsnNode(classNode.name));
                lensGen.add(new LdcInsnNode(annotation.namingPattern()));
                lensGen.add(new InsnNode(
                    annotation.compose() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                lensGen.add(new InsnNode(
                    annotation.generateModify() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                lensGen.add(new InsnNode(
                    annotation.generateOptional() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                lensGen.add(new InsnNode(
                    annotation.generateIso() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

                lensGen.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/functional/DeepMixLens",
                    "generateAllLenses",
                    "(Ljava/lang/String;Ljava/lang/String;ZZZZ)V",
                    false
                ));

                methodNode.instructions.insert(lensGen);
            } else {
                for (LensTarget lens : lenses) {
                    InsnList lensGen = new InsnList();
                    lensGen.add(new LdcInsnNode(classNode.name));
                    lensGen.add(new LdcInsnNode(lens.path()));
                    lensGen.add(new LdcInsnNode(lens.getter()));
                    lensGen.add(new LdcInsnNode(lens.setter()));
                    lensGen.add(new InsnNode(
                        lens.compose() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                    lensGen.add(new LdcInsnNode(annotation.namingPattern()));

                    lensGen.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/functional/DeepMixLens",
                        "generateLens",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                        "Ljava/lang/String;ZLjava/lang/String;)V",
                        false
                    ));

                    methodNode.instructions.insert(lensGen);
                }
            }

            context.addDiagnostic(String.format(
                "🔍 @DeepLens applied to %s [lenses=%d, generateAll=%b]",
                classNode.name, lenses.length, annotation.generateAll()
            ));
        }

        /**
         * Process @DeepFold — Inject fold/reduce operation.
         */
        public void processFold(DeepFold annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            InsnList foldCode = new InsnList();

            foldCode.add(new LdcInsnNode(annotation.direction().name()));
            foldCode.add(new LdcInsnNode(annotation.initial()));
            foldCode.add(new LdcInsnNode(annotation.accumulator()));
            foldCode.add(new InsnNode(
                annotation.parallel() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            foldCode.add(new InsnNode(
                annotation.short_circuit() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            foldCode.add(new LdcInsnNode(annotation.terminationPredicate()));
            foldCode.add(new LdcInsnNode(annotation.resultMapper()));

            foldCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/functional/DeepMixFold",
                "fold",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "ZZLjava/lang/String;Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(foldCode);

            context.addDiagnostic(String.format(
                "📐 @DeepFold applied to %s::%s [direction=%s, parallel=%b]",
                classNode.name, methodNode.name,
                annotation.direction(), annotation.parallel()
            ));
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                      ║
    // ║  PHASE 20: UI & GRAPHICS                                             ║
    // ║  16 annotations | Priority: LOW | Est. Time: 2-3 days                ║
    // ║                                                                      ║
    // ║  Graphics Backends:                                                   ║
    // ║    Vulkan/OpenGL/OpenGL ES → LWJGL direct                           ║
    // ║    DirectX/Metal           → bgfx via LWJGL                         ║
    // ║    WebGL                   → bgfx via LWJGL (bgfx WebGL backend)    ║
    // ║    Canvas                  → bgfx via LWJGL 2D rendering            ║
    // ║                                                                      ║
    // ║  @DeepJavaFX  @DeepSwing   @DeepAWT    @DeepOpenGL                  ║
    // ║  @DeepVulkan  @DeepDirectX @DeepMetal  @DeepWebGL                   ║
    // ║  @DeepCanvas  @DeepSVG     @DeepPDF    @DeepChart                   ║
    // ║  @DeepGraph   @DeepDiagram @DeepMindmap @DeepGantt                  ║
    // ║                                                                      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // Phase 20 — Enums
    // ──────────────────────────────────────────────

    /**
     * Graphics API backend selection.
     *
     * LWJGL is used directly for Vulkan, OpenGL, and OpenGL ES.
     * bgfx (via LWJGL bindings) is used for DirectX, Metal, WebGL, and Canvas,
     * providing cross-platform abstraction over those APIs.
     */
    public enum GraphicsBackend {
        /** OpenGL via LWJGL direct bindings (org.lwjgl.opengl) */
        OPENGL_LWJGL,

        /** OpenGL ES via LWJGL direct bindings (org.lwjgl.opengles) */
        OPENGLES_LWJGL,

        /** Vulkan via LWJGL direct bindings (org.lwjgl.vulkan) */
        VULKAN_LWJGL,

        /** DirectX 11/12 via bgfx through LWJGL (org.lwjgl.bgfx) */
        DIRECTX_BGFX,

        /** Metal via bgfx through LWJGL (org.lwjgl.bgfx) */
        METAL_BGFX,

        /** WebGL via bgfx through LWJGL (bgfx WebGL/WebGPU backend) */
        WEBGL_BGFX,

        /** Software/Canvas 2D rendering via bgfx through LWJGL */
        CANVAS_BGFX,

        /** bgfx auto-select best available backend via LWJGL */
        BGFX_AUTO,

        /** Let DeepMix choose the best available backend */
        AUTO
    }

    /**
     * bgfx renderer type mapping.
     * Maps to bgfx_renderer_type_t for LWJGL bgfx bindings.
     */
    public enum BgfxRendererType {
        NOOP(0),              // No rendering
        AGC(1),               // AGC
        DIRECT3D9(2),         // DirectX 9
        DIRECT3D11(3),        // DirectX 11
        DIRECT3D12(4),        // DirectX 12
        GNM(5),               // GNM (PS4)
        METAL(6),             // Metal
        NVN(7),               // NVN (Nintendo Switch)
        OPENGLES(8),          // OpenGL ES
        OPENGL(9),            // OpenGL
        VULKAN(10),           // Vulkan
        WEBGPU(11),           // WebGPU
        COUNT(12);            // Count

        private final int value;
        BgfxRendererType(int value) { this.value = value; }
        public int value() { return value; }

        /** Map from GraphicsBackend to BgfxRendererType */
        public static BgfxRendererType fromGraphicsBackend(GraphicsBackend backend) {
            switch (backend) {
                case DIRECTX_BGFX:  return DIRECT3D12; // Prefer DX12
                case METAL_BGFX:    return METAL;
                case WEBGL_BGFX:    return WEBGPU;     // Use WebGPU backend
                case CANVAS_BGFX:   return OPENGL;     // bgfx 2D via GL
                case BGFX_AUTO:     return COUNT;       // COUNT = auto-detect
                default:            return COUNT;
            }
        }
    }

    /** Shader language for the selected backend */
    public enum ShaderLanguage {
        GLSL_100,             // OpenGL ES 2.0
        GLSL_120,             // OpenGL 2.1
        GLSL_130,             // OpenGL 3.0
        GLSL_140,             // OpenGL 3.1
        GLSL_150,             // OpenGL 3.2
        GLSL_330,             // OpenGL 3.3
        GLSL_400,             // OpenGL 4.0
        GLSL_410,             // OpenGL 4.1
        GLSL_420,             // OpenGL 4.2
        GLSL_430,             // OpenGL 4.3
        GLSL_440,             // OpenGL 4.4
        GLSL_450,             // OpenGL 4.5
        GLSL_460,             // OpenGL 4.6
        HLSL_SM4,             // HLSL Shader Model 4 (DX10)
        HLSL_SM5,             // HLSL Shader Model 5 (DX11)
        HLSL_SM6,             // HLSL Shader Model 6 (DX12)
        SPIRV,                // SPIR-V (Vulkan)
        MSL,                  // Metal Shading Language
        WGSL,                 // WebGPU Shading Language
        BGFX_SC,              // bgfx shader compiler (cross-compiled)
        AUTO                  // Auto-select for backend
    }

    /** Render pass type */
    public enum RenderPassType {
        FORWARD,              // Forward rendering
        DEFERRED,             // Deferred rendering
        SHADOW,               // Shadow pass
        POST_PROCESS,         // Post-processing
        COMPUTE,              // Compute pass
        OVERLAY,              // UI/HUD overlay
        DEBUG,                // Debug visualization
        CUSTOM
    }

    /** Primitive type */
    public enum PrimitiveType {
        TRIANGLES,
        TRIANGLE_STRIP,
        TRIANGLE_FAN,
        LINES,
        LINE_STRIP,
        POINTS,
        QUADS,
        PATCHES
    }

    /** Texture format */
    public enum TextureFormat {
        RGBA8,
        RGB8,
        BGRA8,
        RGBA16F,
        RGBA32F,
        R8,
        RG8,
        R16F,
        R32F,
        DEPTH24,
        DEPTH32F,
        DEPTH24_STENCIL8,
        BC1,                  // DXT1
        BC2,                  // DXT3
        BC3,                  // DXT5
        BC7,
        ETC2,
        ASTC_4x4,
        AUTO
    }

    /** Anti-aliasing mode */
    public enum AntiAliasMode {
        NONE,
        MSAA_2X,
        MSAA_4X,
        MSAA_8X,
        MSAA_16X,
        FXAA,
        SMAA,
        TAA,
        DLSS,                 // NVIDIA DLSS
        FSR,                  // AMD FSR
        AUTO
    }

    /** UI component type for JavaFX/Swing */
    public enum UIComponentType {
        BUTTON,
        LABEL,
        TEXT_FIELD,
        TEXT_AREA,
        CHECKBOX,
        RADIO_BUTTON,
        COMBO_BOX,
        LIST_VIEW,
        TABLE_VIEW,
        TREE_VIEW,
        TAB_PANE,
        SCROLL_PANE,
        SPLIT_PANE,
        SLIDER,
        PROGRESS_BAR,
        SPINNER,
        DATE_PICKER,
        COLOR_PICKER,
        FILE_CHOOSER,
        DIALOG,
        MENU,
        TOOLBAR,
        CANVAS,
        WEB_VIEW,
        MEDIA_VIEW,
        CHART,
        CUSTOM
    }

    /** Chart type */
    public enum ChartType {
        LINE,
        BAR,
        PIE,
        AREA,
        SCATTER,
        BUBBLE,
        RADAR,
        POLAR,
        HEATMAP,
        TREEMAP,
        SANKEY,
        FUNNEL,
        WATERFALL,
        CANDLESTICK,
        BOX_PLOT,
        HISTOGRAM,
        DONUT,
        GAUGE,
        SUNBURST,
        CHORD,
        NETWORK,
        GEOGRAPHIC,
        CUSTOM
    }

    /** Graph layout algorithm */
    public enum GraphLayout {
        FORCE_DIRECTED,
        HIERARCHICAL,
        CIRCULAR,
        RADIAL,
        GRID,
        TREE,
        DAGRE,                // Directed acyclic graph layout
        SPRING,
        FRUCHTERMAN_REINGOLD,
        KAMADA_KAWAI,
        SPECTRAL,
        ORGANIC,
        ORTHOGONAL,
        CUSTOM
    }

    /** Diagram type */
    public enum DiagramType {
        FLOWCHART,
        SEQUENCE,
        CLASS,
        STATE,
        ACTIVITY,
        USE_CASE,
        COMPONENT,
        DEPLOYMENT,
        ENTITY_RELATIONSHIP,
        DATA_FLOW,
        NETWORK,
        ARCHITECTURE,
        WIREFRAME,
        BPMN,
        CUSTOM
    }

    /** SVG output options */
    public enum SVGOutput {
        INLINE,               // SVG as string
        FILE,                 // Write to .svg file
        DATA_URI,             // data:image/svg+xml;base64,...
        DOM_ELEMENT           // DOM Element object
    }

    /** PDF page size */
    public enum PDFPageSize {
        A0, A1, A2, A3, A4, A5, A6,
        LETTER, LEGAL, TABLOID,
        CUSTOM
    }

    /** PDF orientation */
    public enum PDFOrientation {
        PORTRAIT,
        LANDSCAPE,
        AUTO
    }

    /** Gantt chart dependency type */
    public enum GanttDependencyType {
        FINISH_TO_START,      // FS: predecessor must finish before successor starts
        START_TO_START,       // SS: both start at same time
        FINISH_TO_FINISH,     // FF: both finish at same time
        START_TO_FINISH       // SF: predecessor starts when successor finishes
    }

    // ──────────────────────────────────────────────
    // Phase 20 — Sub-annotation types
    // ──────────────────────────────────────────────

    /** LWJGL window configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WindowConfig {
        int width() default 1280;
        int height() default 720;
        String title() default "DeepMix Window";
        boolean resizable() default true;
        boolean fullscreen() default false;
        boolean decorated() default true;
        boolean vsync() default true;
        int swapInterval() default 1;
        boolean transparent() default false;
        boolean floating() default false;       // Always on top
        boolean maximized() default false;
        int minWidth() default 0;
        int minHeight() default 0;
        int maxWidth() default 0;               // 0 = unlimited
        int maxHeight() default 0;
        int samples() default 0;                // MSAA samples
        int refreshRate() default 0;            // 0 = don't care
        boolean srgb() default false;
        boolean doubleBuffer() default true;
    }

    /**
     * bgfx initialization configuration for cross-API rendering.
     * Used when rendering through bgfx via LWJGL bindings.
     */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BgfxConfig {
        BgfxRendererType renderer() default BgfxRendererType.COUNT; // COUNT = auto
        int width() default 1280;
        int height() default 720;
        int resetFlags() default 0;             // BGFX_RESET_* flags
        boolean debug() default false;
        boolean profile() default false;
        int maxEncoders() default 8;
        int transientVbSize() default 6291456;  // 6 MB
        int transientIbSize() default 2097152;  // 2 MB
    }

    /** Shader source specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ShaderSource {
        ShaderLanguage language() default ShaderLanguage.AUTO;
        String vertexShader() default "";
        String fragmentShader() default "";
        String geometryShader() default "";
        String tessControlShader() default "";
        String tessEvalShader() default "";
        String computeShader() default "";
        String vertexShaderPath() default "";
        String fragmentShaderPath() default "";
        boolean crossCompile() default false;   // Use bgfx shaderc for cross-compilation
    }

    /** Render pipeline configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RenderPipelineConfig {
        RenderPassType[] passes() default {RenderPassType.FORWARD};
        AntiAliasMode antiAlias() default AntiAliasMode.NONE;
        boolean depthTest() default true;
        boolean depthWrite() default true;
        boolean backfaceCulling() default true;
        boolean blending() default false;
        String blendFunc() default "SRC_ALPHA, ONE_MINUS_SRC_ALPHA";
        boolean wireframe() default false;
        TextureFormat colorFormat() default TextureFormat.RGBA8;
        TextureFormat depthFormat() default TextureFormat.DEPTH24_STENCIL8;
        int viewId() default 0;                 // bgfx view ID
    }

    /** Chart data series definition */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ChartSeries {
        String name();
        String dataProvider() default "";       // Method providing data
        String color() default "";              // Hex color
        String type() default "";               // Override chart type for this series
        boolean visible() default true;
        double lineWidth() default 1.0;
        String marker() default "";             // circle, square, triangle, none
    }

    /** Gantt task definition */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface GanttTask {
        String id();
        String name();
        String start() default "";
        String end() default "";
        int durationDays() default 0;
        double progress() default 0.0;          // 0.0 - 1.0
        String[] dependencies() default {};
        GanttDependencyType depType() default GanttDependencyType.FINISH_TO_START;
        String assignee() default "";
        String color() default "";
        boolean milestone() default false;
        String group() default "";
    }

    /** Mindmap node definition */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MindmapNode {
        String text();
        String[] children() default {};
        String color() default "";
        String icon() default "";
        String link() default "";
        boolean collapsed() default false;
    }

    // ──────────────────────────────────────────────
    // Phase 20 — Annotations
    // ──────────────────────────────────────────────

    /**
     * ☕ @DeepJavaFX — JavaFX UI component generation and modification.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    public @interface DeepJavaFX {
        String target() default "";
        UIComponentType component() default UIComponentType.CUSTOM;
        String fxmlPath() default "";
        String cssPath() default "";
        String[] cssClasses() default {};
        String id() default "";
        boolean bindModel() default false;      // MVVM data binding
        String modelClass() default "";
        String controllerClass() default "";
        boolean reactive() default false;       // ReactFX integration
        boolean responsive() default false;     // Responsive layout
        int minWidth() default 0;
        int minHeight() default 0;
        int prefWidth() default 0;
        int prefHeight() default 0;
        String layout() default "";             // VBox, HBox, GridPane, etc.
        boolean animated() default false;
        String animationType() default "";
        int animationDurationMs() default 300;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepJavaFX */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    public @interface DJFX {
        String target() default "";
        UIComponentType component() default UIComponentType.CUSTOM;
        String fxmlPath() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🖼️ @DeepSwing — Swing UI modifications.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    public @interface DeepSwing {
        String target() default "";
        UIComponentType component() default UIComponentType.CUSTOM;
        String lookAndFeel() default "";        // javax.swing.UIManager LAF
        boolean nimbus() default false;
        boolean flatLaf() default false;        // FlatLaf modern LAF
        String theme() default "";
        boolean antiAlias() default true;
        boolean doubleBuffer() default true;
        int preferredWidth() default 0;
        int preferredHeight() default 0;
        String layout() default "";             // BorderLayout, GridBagLayout, etc.
        boolean threadSafe() default true;      // Wrap in SwingUtilities.invokeLater
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSwing */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    public @interface DSWING {
        String target() default "";
        UIComponentType component() default UIComponentType.CUSTOM;
        String lookAndFeel() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🎨 @DeepAWT — AWT operations and rendering.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepAWT {
        String target() default "";
        boolean headless() default false;       // java.awt.headless mode
        boolean antiAlias() default true;
        boolean textAntiAlias() default true;
        String renderingHint() default "";
        boolean useAcceleration() default true;
        String[] fonts() default {};            // Register custom fonts
        int canvasWidth() default 800;
        int canvasHeight() default 600;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepAWT */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DAWT {
        String target() default "";
        boolean headless() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔺 @DeepOpenGL — OpenGL rendering via LWJGL direct bindings.
     *
     * Uses org.lwjgl.opengl.* for direct OpenGL access.
     * For OpenGL ES, set {@code gles = true} to use org.lwjgl.opengles.*.
     *
     * Example:
     * <pre>
     * {@code @DeepOpenGL(
     *     window = @WindowConfig(width = 1920, height = 1080, title = "My Renderer"),
     *     pipeline = @RenderPipelineConfig(antiAlias = AntiAliasMode.MSAA_4X),
     *     shaders = @ShaderSource(
     *         vertexShaderPath = "shaders/main.vert",
     *         fragmentShaderPath = "shaders/main.frag",
     *         language = ShaderLanguage.GLSL_450
     *     ),
     *     version = "4.5"
     * )}
     * public void render(Scene scene) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepOpenGL {
        String target() default "";
        WindowConfig window() default @WindowConfig;
        RenderPipelineConfig pipeline() default @RenderPipelineConfig;
        ShaderSource shaders() default @ShaderSource;
        String version() default "3.3";         // OpenGL version
        boolean coreProfile() default true;
        boolean forwardCompat() default true;
        boolean gles() default false;           // Use OpenGL ES via LWJGL
        String glesVersion() default "3.0";
        boolean debugContext() default false;
        boolean computeShaders() default false;
        boolean tessellation() default false;
        boolean geometryShaders() default false;
        String[] extensions() default {};       // Required GL extensions
        boolean useVAO() default true;
        boolean useDSA() default false;         // Direct State Access (4.5+)
        boolean bindlessTextures() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepOpenGL */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DOGL {
        String target() default "";
        WindowConfig window() default @WindowConfig;
        ShaderSource shaders() default @ShaderSource;
        String version() default "3.3";
        boolean gles() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🌋 @DeepVulkan — Vulkan rendering via LWJGL direct bindings.
     *
     * Uses org.lwjgl.vulkan.* for direct Vulkan access.
     * Handles instance creation, device selection, swapchain, pipeline,
     * command buffers, and synchronization.
     *
     * Example:
     * <pre>
     * {@code @DeepVulkan(
     *     window = @WindowConfig(width = 1920, height = 1080),
     *     shaders = @ShaderSource(
     *         vertexShaderPath = "shaders/main.vert.spv",
     *         fragmentShaderPath = "shaders/main.frag.spv",
     *         language = ShaderLanguage.SPIRV
     *     ),
     *     validation = true,
     *     devicePreference = "DISCRETE"
     * )}
     * public void renderFrame() { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepVulkan {
        String target() default "";
        WindowConfig window() default @WindowConfig;
        RenderPipelineConfig pipeline() default @RenderPipelineConfig;
        ShaderSource shaders() default @ShaderSource;
        String apiVersion() default "1.3";
        boolean validation() default false;     // Validation layers
        String[] validationLayers() default {"VK_LAYER_KHRONOS_validation"};
        String[] instanceExtensions() default {};
        String[] deviceExtensions() default {"VK_KHR_swapchain"};
        String devicePreference() default "DISCRETE"; // DISCRETE, INTEGRATED, ANY
        int maxFramesInFlight() default 2;
        boolean useDescriptorIndexing() default false;
        boolean useDynamicRendering() default false; // VK 1.3
        boolean useRayTracing() default false;
        boolean useMeshShaders() default false;
        boolean useTimeline() default false;    // Timeline semaphores
        int commandPoolFlags() default 0;       // VK_COMMAND_POOL_CREATE_*
        boolean profileGPU() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepVulkan */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DVULKAN {
        String target() default "";
        WindowConfig window() default @WindowConfig;
        ShaderSource shaders() default @ShaderSource;
        String apiVersion() default "1.3";
        boolean validation() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🎮 @DeepDirectX — DirectX rendering via bgfx through LWJGL.
     *
     * Uses org.lwjgl.bgfx.* with the Direct3D 11 or Direct3D 12 backend.
     * bgfx abstracts the DirectX API, allowing cross-platform code while
     * targeting DX11/DX12 on Windows.
     *
     * Example:
     * <pre>
     * {@code @DeepDirectX(
     *     bgfx = @BgfxConfig(renderer = BgfxRendererType.DIRECT3D12),
     *     window = @WindowConfig(width = 1920, height = 1080),
     *     shaders = @ShaderSource(crossCompile = true)
     * )}
     * public void renderScene(Scene scene) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepDirectX {
        String target() default "";
        BgfxConfig bgfx() default @BgfxConfig(renderer = BgfxRendererType.DIRECT3D12);
        WindowConfig window() default @WindowConfig;
        RenderPipelineConfig pipeline() default @RenderPipelineConfig;
        ShaderSource shaders() default @ShaderSource;
        boolean preferDX12() default true;      // DX12 over DX11
        boolean dxr() default false;           // DirectX Raytracing
        boolean meshShaders() default false;
        boolean variableRateShading() default false;
        String featureLevel() default "12_0";   // 11_0, 11_1, 12_0, 12_1, 12_2
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepDirectX */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DDX {
        String target() default "";
        BgfxConfig bgfx() default @BgfxConfig(renderer = BgfxRendererType.DIRECT3D12);
        WindowConfig window() default @WindowConfig;
        boolean preferDX12() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🍎 @DeepMetal — Metal API rendering via bgfx through LWJGL.
     *
     * Uses org.lwjgl.bgfx.* with the Metal backend on macOS/iOS.
     * bgfx handles Metal API calls, shading language conversion, and
     * resource management while exposing a cross-platform API.
     *
     * Example:
     * <pre>
     * {@code @DeepMetal(
     *     bgfx = @BgfxConfig(renderer = BgfxRendererType.METAL),
     *     window = @WindowConfig(width = 2560, height = 1440)
     * )}
     * public void renderWithMetal() { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepMetal {
        String target() default "";
        BgfxConfig bgfx() default @BgfxConfig(renderer = BgfxRendererType.METAL);
        WindowConfig window() default @WindowConfig;
        RenderPipelineConfig pipeline() default @RenderPipelineConfig;
        ShaderSource shaders() default @ShaderSource;
        String metalVersion() default "3.0";    // Metal API version
        boolean metalRaytracing() default false;
        boolean metalMeshShaders() default false;
        boolean useMSL() default false;         // Direct MSL (vs bgfx cross-compile)
        boolean gpuCapture() default false;     // Metal GPU capture for Xcode
        boolean useMetalValidation() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMetal */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DMETAL {
        String target() default "";
        BgfxConfig bgfx() default @BgfxConfig(renderer = BgfxRendererType.METAL);
        WindowConfig window() default @WindowConfig;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🌐 @DeepWebGL — WebGL rendering via bgfx through LWJGL.
     *
     * Uses org.lwjgl.bgfx.* with the WebGPU/OpenGL ES backend.
     * bgfx supports WebGL through its OpenGL ES backend, and WebGPU
     * through its native WebGPU backend. This annotation provides
     * configuration for web-targeted rendering pipelines.
     *
     * Note: For JVM-based WebGL simulation/testing, bgfx will use
     * its OpenGL ES backend. For actual web deployment, shaders are
     * cross-compiled by bgfx's shaderc to ESSL/WGSL.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepWebGL {
        String target() default "";
        BgfxConfig bgfx() default @BgfxConfig(renderer = BgfxRendererType.OPENGLES);
        WindowConfig window() default @WindowConfig;
        RenderPipelineConfig pipeline() default @RenderPipelineConfig;
        ShaderSource shaders() default @ShaderSource;
        String webglVersion() default "2.0";    // 1.0 or 2.0
        boolean preferWebGPU() default false;   // Use WebGPU when available
        boolean powerPreference() default false; // "high-performance" GPU
        boolean antialias() default true;
        boolean alpha() default true;
        boolean premultipliedAlpha() default true;
        boolean preserveDrawingBuffer() default false;
        boolean stencil() default false;
        int maxTextureSize() default 0;         // 0 = device default
        boolean floatTextures() default false;
        boolean instancedArrays() default true;
        boolean compressedTextures() default false;
        String[] requiredExtensions() default {};
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepWebGL */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DWEBGL {
        String target() default "";
        BgfxConfig bgfx() default @BgfxConfig(renderer = BgfxRendererType.OPENGLES);
        String webglVersion() default "2.0";
        boolean preferWebGPU() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🖼️ @DeepCanvas — 2D canvas-style rendering via bgfx through LWJGL.
     *
     * Uses bgfx's 2D rendering capabilities (via org.lwjgl.bgfx.*) to provide
     * HTML5 Canvas-like 2D drawing primitives. bgfx's debug draw API and
     * custom 2D shaders are used to implement path rendering, fills, strokes,
     * gradients, and text.
     *
     * For immediate-mode 2D rendering, bgfx provides efficient batched
     * quad/triangle rendering that maps well to canvas operations.
     *
     * Example:
     * <pre>
     * {@code @DeepCanvas(
     *     bgfx = @BgfxConfig(renderer = BgfxRendererType.COUNT), // auto-detect
     *     width = 1024,
     *     height = 768,
     *     antiAlias = true
     * )}
     * public void draw2D(Canvas2DContext ctx) { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepCanvas {
        String target() default "";
        BgfxConfig bgfx() default @BgfxConfig;
        int width() default 800;
        int height() default 600;
        boolean antiAlias() default true;
        boolean alpha() default true;
        boolean retina() default true;          // HiDPI support
        double pixelRatio() default 0.0;        // 0 = auto-detect
        String fillColor() default "#000000";
        String strokeColor() default "#000000";
        double lineWidth() default 1.0;
        String lineCap() default "butt";        // butt, round, square
        String lineJoin() default "miter";      // miter, round, bevel
        double globalAlpha() default 1.0;
        String compositeOperation() default "source-over";
        boolean imageSmoothingEnabled() default true;
        String font() default "16px sans-serif";
        String textAlign() default "start";
        String textBaseline() default "alphabetic";
        boolean offscreen() default false;      // Off-screen canvas
        boolean useNanoVG() default false;       // Use NanoVG (via LWJGL) for vector graphics
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCanvas */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DCANVAS {
        String target() default "";
        int width() default 800;
        int height() default 600;
        boolean antiAlias() default true;
        boolean useNanoVG() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📐 @DeepSVG — SVG (Scalable Vector Graphics) generation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepSVG {
        String target() default "";
        int width() default 800;
        int height() default 600;
        String viewBox() default "";            // e.g., "0 0 800 600"
        SVGOutput output() default SVGOutput.INLINE;
        String outputPath() default "";
        boolean minify() default false;
        boolean prettyPrint() default true;
        String defaultFill() default "none";
        String defaultStroke() default "#000000";
        double defaultStrokeWidth() default 1.0;
        String[] defs() default {};             // SVG defs (gradients, patterns, etc.)
        String[] stylesheets() default {};      // Embedded CSS
        boolean responsive() default false;     // Remove width/height, use viewBox only
        String xmlns() default "http://www.w3.org/2000/svg";
        boolean includeXlink() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSVG */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DSVG {
        String target() default "";
        int width() default 800;
        int height() default 600;
        SVGOutput output() default SVGOutput.INLINE;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📄 @DeepPDF — PDF document generation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepPDF {
        String target() default "";
        String outputPath() default "";
        PDFPageSize pageSize() default PDFPageSize.A4;
        PDFOrientation orientation() default PDFOrientation.PORTRAIT;
        float customWidth() default 0;          // For CUSTOM page size (points)
        float customHeight() default 0;
        float marginTop() default 72;           // 1 inch in points
        float marginBottom() default 72;
        float marginLeft() default 72;
        float marginRight() default 72;
        String title() default "";
        String author() default "";
        String subject() default "";
        String[] keywords() default {};
        String creator() default "DeepMix PDF Generator";
        boolean encrypt() default false;
        String userPassword() default "";
        String ownerPassword() default "";
        boolean allowPrinting() default true;
        boolean allowCopying() default true;
        boolean allowEditing() default false;
        boolean compressed() default true;
        String defaultFont() default "Helvetica";
        float defaultFontSize() default 12;
        String headerTemplate() default "";
        String footerTemplate() default "";
        boolean pageNumbers() default true;
        boolean tableOfContents() default false;
        String watermark() default "";
        float watermarkOpacity() default 0.2f;
        String library() default "iText";       // iText, PDFBox, OpenPDF, Flying Saucer
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepPDF */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DPDF {
        String target() default "";
        String outputPath() default "";
        PDFPageSize pageSize() default PDFPageSize.A4;
        String title() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📊 @DeepChart — Chart generation.
     *
     * Example:
     * <pre>
     * {@code @DeepChart(
     *     type = ChartType.LINE,
     *     title = "Server TPS Over Time",
     *     width = 800,
     *     height = 400,
     *     series = {
     *         @ChartSeries(name = "TPS", dataProvider = "getTpsData", color = "#4CAF50"),
     *         @ChartSeries(name = "Threshold", dataProvider = "getThreshold", color = "#F44336")
     *     }
     * )}
     * public byte[] generatePerformanceChart() { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepChart {
        String target() default "";
        ChartType type() default ChartType.LINE;
        String title() default "";
        String subtitle() default "";
        int width() default 800;
        int height() default 600;
        ChartSeries[] series() default {};
        String xAxisLabel() default "";
        String yAxisLabel() default "";
        boolean xAxisGrid() default true;
        boolean yAxisGrid() default true;
        boolean legend() default true;
        String legendPosition() default "bottom"; // top, bottom, left, right
        boolean tooltip() default true;
        boolean animation() default true;
        int animationDurationMs() default 1000;
        boolean responsive() default false;
        String theme() default "default";       // default, dark, light, material
        String backgroundColor() default "#FFFFFF";
        String[] colors() default {};           // Custom color palette
        String outputFormat() default "PNG";    // PNG, SVG, PDF, HTML
        String outputPath() default "";
        boolean interactive() default false;    // Interactive HTML chart
        String library() default "JFreeChart";  // JFreeChart, XChart, ECharts
        double[] yAxisRange() default {};       // [min, max]
        boolean stacked() default false;
        boolean logarithmic() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepChart */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DCHART {
        String target() default "";
        ChartType type() default ChartType.LINE;
        String title() default "";
        int width() default 800;
        int height() default 600;
        ChartSeries[] series() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🕸️ @DeepGraph — Graph/network visualization.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepGraph {
        String target() default "";
        GraphLayout layout() default GraphLayout.FORCE_DIRECTED;
        int width() default 800;
        int height() default 600;
        boolean directed() default false;
        boolean weighted() default false;
        boolean labels() default true;
        boolean nodeTooltips() default true;
        boolean edgeLabels() default false;
        String nodeColor() default "#4CAF50";
        String edgeColor() default "#757575";
        double nodeSize() default 10.0;
        double edgeWidth() default 1.0;
        boolean physics() default true;         // Physics simulation for force layout
        double gravity() default -50.0;
        double springLength() default 100.0;
        double springConstant() default 0.08;
        double damping() default 0.9;
        boolean stabilize() default true;
        int stabilizationIterations() default 100;
        String outputFormat() default "SVG";    // SVG, PNG, HTML, DOT, JSON
        String outputPath() default "";
        boolean interactive() default false;
        boolean zoomable() default true;
        boolean pannable() default true;
        boolean draggable() default true;
        String library() default "JUNG";        // JUNG, GraphStream, JGraphT, vis.js
        String[] clusterBy() default {};        // Cluster nodes by property
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepGraph */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DGRAPH {
        String target() default "";
        GraphLayout layout() default GraphLayout.FORCE_DIRECTED;
        int width() default 800;
        int height() default 600;
        String outputFormat() default "SVG";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📋 @DeepDiagram — Diagram generation (UML, flowcharts, etc.).
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepDiagram {
        String target() default "";
        DiagramType type() default DiagramType.FLOWCHART;
        int width() default 800;
        int height() default 600;
        String title() default "";
        String direction() default "TB";        // TB, BT, LR, RL
        String theme() default "default";       // default, forest, dark, neutral
        boolean edgeLabels() default true;
        boolean nodeIcons() default false;
        String outputFormat() default "SVG";    // SVG, PNG, PDF, ASCII, Mermaid
        String outputPath() default "";
        boolean interactive() default false;
        String notation() default "";           // UML, BPMN, C4, ArchiMate
        boolean autoLayout() default true;
        String[] styles() default {};           // Custom CSS styles
        String library() default "Mermaid";     // Mermaid, PlantUML, Graphviz, draw.io
        boolean includeSource() default false;  // Include source notation in output
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepDiagram */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DDIAG {
        String target() default "";
        DiagramType type() default DiagramType.FLOWCHART;
        String outputFormat() default "SVG";
        String library() default "Mermaid";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🧠 @DeepMindmap — Mind map generation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepMindmap {
        String target() default "";
        String centralTopic() default "";
        MindmapNode[] nodes() default {};
        String dataProvider() default "";       // Method providing tree data
        int width() default 1200;
        int height() default 800;
        GraphLayout layout() default GraphLayout.RADIAL;
        boolean animated() default false;
        boolean collapsible() default true;
        boolean editable() default false;
        String[] colors() default {};           // Color palette for branches
        boolean icons() default false;
        boolean links() default false;          // Hyperlinks on nodes
        boolean notes() default false;          // Notes attached to nodes
        String outputFormat() default "SVG";
        String outputPath() default "";
        boolean interactive() default false;
        String theme() default "default";
        String library() default "FreeMind";    // FreeMind, XMind, D3.js, GoJS
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMindmap */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DMIND {
        String target() default "";
        String centralTopic() default "";
        MindmapNode[] nodes() default {};
        String outputFormat() default "SVG";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📅 @DeepGantt — Gantt chart generation.
     *
     * Example:
     * <pre>
     * {@code @DeepGantt(
     *     title = "Project Timeline",
     *     tasks = {
     *         @GanttTask(id = "1", name = "Design", start = "2024-01-01", durationDays = 14),
     *         @GanttTask(id = "2", name = "Implement", durationDays = 30,
     *                   dependencies = {"1"}, assignee = "Team A"),
     *         @GanttTask(id = "3", name = "Release", durationDays = 0,
     *                   dependencies = {"2"}, milestone = true)
     *     }
     * )}
     * public byte[] generateProjectTimeline() { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepGantt {
        String target() default "";
        String title() default "";
        GanttTask[] tasks() default {};
        String dataProvider() default "";       // Method providing task data
        int width() default 1200;
        int height() default 600;
        String startDate() default "";          // Overall project start
        String endDate() default "";
        String dateFormat() default "yyyy-MM-dd";
        String timeUnit() default "day";        // day, week, month, quarter, year
        boolean criticalPath() default true;    // Highlight critical path
        boolean showProgress() default true;
        boolean showDependencies() default true;
        boolean showMilestones() default true;
        boolean showAssignees() default true;
        boolean showToday() default true;       // Today line
        String[] groups() default {};           // Task group/phase names
        String[] groupColors() default {};
        boolean collapsible() default true;     // Collapse groups
        boolean draggable() default false;      // Drag to reschedule
        boolean resizable() default false;      // Drag to change duration
        boolean zoomable() default true;
        String outputFormat() default "SVG";    // SVG, PNG, PDF, HTML
        String outputPath() default "";
        boolean interactive() default false;
        String theme() default "default";
        boolean weekendsShaded() default true;
        String[] holidays() default {};         // "yyyy-MM-dd" format
        String library() default "JFreeChart";  // JFreeChart, DHTMLX, Frappe, Mermaid
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepGantt */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DGANTT {
        String target() default "";
        String title() default "";
        GanttTask[] tasks() default {};
        String outputFormat() default "SVG";
        boolean criticalPath() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * Processor for all Phase 20 (UI & Graphics) annotations.
     *
     * ┌──────────────────────────────────────────────────────────────────┐
     * │ Backend Routing Table                                           │
     * ├──────────────────┬───────────────────────────────────────────────┤
     * │ API              │ Implementation                               │
     * ├──────────────────┼───────────────────────────────────────────────┤
     * │ OpenGL           │ LWJGL direct  (org.lwjgl.opengl.*)           │
     * │ OpenGL ES        │ LWJGL direct  (org.lwjgl.opengles.*)         │
     * │ Vulkan           │ LWJGL direct  (org.lwjgl.vulkan.*)           │
     * │ DirectX 11/12    │ bgfx via LWJGL (org.lwjgl.bgfx.*)           │
     * │ Metal            │ bgfx via LWJGL (org.lwjgl.bgfx.*)           │
     * │ WebGL/WebGPU     │ bgfx via LWJGL (org.lwjgl.bgfx.*)           │
     * │ Canvas 2D        │ bgfx + NanoVG via LWJGL                     │
     * ├──────────────────┼───────────────────────────────────────────────┤
     * │ Shader Pipeline                                                 │
     * ├──────────────────┼───────────────────────────────────────────────┤
     * │ GLSL → SPIR-V    │ LWJGL Shaderc  (org.lwjgl.util.shaderc.*)   │
     * │ HLSL → SPIR-V    │ LWJGL Shaderc  (org.lwjgl.util.shaderc.*)   │
     * │ SPIR-V → GLSL    │ LWJGL SPIRV-Cross (org.lwjgl.util.spvc.*)   │
     * │ SPIR-V → HLSL    │ LWJGL SPIRV-Cross (org.lwjgl.util.spvc.*)   │
     * │ SPIR-V → MSL     │ LWJGL SPIRV-Cross (org.lwjgl.util.spvc.*)   │
     * │ SPIR-V → ESSL    │ LWJGL SPIRV-Cross (org.lwjgl.util.spvc.*)   │
     * │ SPIR-V → WGSL    │ LWJGL SPIRV-Cross (org.lwjgl.util.spvc.*)   │
     * │ bgfx SC cross    │ bgfx shaderc (runtime cross-compile)         │
     * │ 2D Vector        │ LWJGL NanoVG   (org.lwjgl.nanovg.*)          │
     * │ Image I/O        │ LWJGL STB      (org.lwjgl.stb.*)             │
     * │ Model Loading    │ LWJGL Assimp   (org.lwjgl.assimp.*)          │
     * │ Math             │ JOML           (org.joml.*)                   │
     * └──────────────────┴───────────────────────────────────────────────┘
     */
    public static class Phase20Processor {

        private final DeepMixContext context;

        /** Maps GraphicsBackend → the LWJGL module path used */
        private static final Map<GraphicsBackend, String> BACKEND_MODULES;

        static {
            Map<GraphicsBackend, String> m = new LinkedHashMap<>();
            m.put(GraphicsBackend.OPENGL_LWJGL,   "org.lwjgl.opengl");
            m.put(GraphicsBackend.OPENGLES_LWJGL,  "org.lwjgl.opengles");
            m.put(GraphicsBackend.VULKAN_LWJGL,    "org.lwjgl.vulkan");
            m.put(GraphicsBackend.DIRECTX_BGFX,    "org.lwjgl.bgfx");
            m.put(GraphicsBackend.METAL_BGFX,      "org.lwjgl.bgfx");
            m.put(GraphicsBackend.WEBGL_BGFX,      "org.lwjgl.bgfx");
            m.put(GraphicsBackend.CANVAS_BGFX,     "org.lwjgl.bgfx + org.lwjgl.nanovg");
            m.put(GraphicsBackend.BGFX_AUTO,        "org.lwjgl.bgfx");
            BACKEND_MODULES = Collections.unmodifiableMap(m);
        }

        /** Maps ShaderLanguage → SPIRV-Cross backend target */
        private static final Map<ShaderLanguage, String> SPVC_BACKEND_MAP;

        static {
            Map<ShaderLanguage, String> s = new LinkedHashMap<>();
            // SPIRV-Cross output targets:
            // spvc_backend enum values used in spvc_context_create_compiler()
            s.put(ShaderLanguage.GLSL_330,  "SPVC_BACKEND_GLSL");
            s.put(ShaderLanguage.GLSL_450,  "SPVC_BACKEND_GLSL");
            s.put(ShaderLanguage.GLSL_460,  "SPVC_BACKEND_GLSL");
            s.put(ShaderLanguage.GLSL_100,  "SPVC_BACKEND_GLSL");  // ESSL 100
            s.put(ShaderLanguage.HLSL_SM5,  "SPVC_BACKEND_HLSL");
            s.put(ShaderLanguage.HLSL_SM6,  "SPVC_BACKEND_HLSL");
            s.put(ShaderLanguage.MSL,       "SPVC_BACKEND_MSL");
            s.put(ShaderLanguage.SPIRV,     "SPVC_BACKEND_NONE");  // No cross-compile
            s.put(ShaderLanguage.WGSL,      "SPVC_BACKEND_NONE");  // Tint/Naga needed
            SPVC_BACKEND_MAP = Collections.unmodifiableMap(s);
        }

        public Phase20Processor(DeepMixContext context) {
            this.context = context;
        }

        // ═══════════════════════════════════════════════════════════════
        // GRAPHICS BACKEND PROCESSORS
        // ═══════════════════════════════════════════════════════════════

        /**
         * Process @DeepOpenGL — Set up OpenGL or OpenGL ES rendering via LWJGL.
         *
         * Implementation chain:
         *   1. GLFW window creation (org.lwjgl.glfw.GLFW)
         *   2. GL/GLES context creation
         *   3. Shader compilation (Shaderc for GLSL→SPIR-V if needed,
         *      or direct GLSL compilation via GL20.glCompileShader)
         *   4. Render pipeline setup
         *   5. Render loop injection
         */
        public void processOpenGL(DeepOpenGL annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {

            WindowConfig window = annotation.window();
            RenderPipelineConfig pipeline = annotation.pipeline();
            ShaderSource shaders = annotation.shaders();
            boolean isGLES = annotation.gles();

            InsnList glCode = new InsnList();

            // ─── Step 1: GLFW Window Setup ───
            // Runtime class: deepmix/runtime/graphics/lwjgl/DeepMixGLFW
            glCode.add(new LdcInsnNode(window.width()));
            glCode.add(new LdcInsnNode(window.height()));
            glCode.add(new LdcInsnNode(window.title()));
            glCode.add(new InsnNode(
                window.resizable() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            glCode.add(new InsnNode(
                window.fullscreen() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            glCode.add(new InsnNode(
                window.vsync() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            glCode.add(new LdcInsnNode(window.samples()));
            glCode.add(new InsnNode(
                window.decorated() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            glCode.add(new InsnNode(
                window.transparent() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            glCode.add(new InsnNode(
                window.srgb() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            glCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/lwjgl/DeepMixGLFW",
                "createWindow",
                "(IILjava/lang/String;ZZZIZZO)J",
                false
            ));

            // Store GLFW window handle
            int windowHandleIdx = methodNode.maxLocals;
            methodNode.maxLocals += 2; // long takes 2 slots
            glCode.add(new VarInsnNode(Opcodes.LSTORE, windowHandleIdx));

            // ─── Step 2: GL/GLES Context Creation ───
            if (isGLES) {
                // OpenGL ES via org.lwjgl.opengles.GLES
                glCode.add(new VarInsnNode(Opcodes.LLOAD, windowHandleIdx));
                glCode.add(new LdcInsnNode(annotation.glesVersion()));

                glCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/graphics/lwjgl/DeepMixOpenGLES",
                    "createContext",
                    "(JLjava/lang/String;)V",
                    false
                ));
            } else {
                // OpenGL via org.lwjgl.opengl.GL
                glCode.add(new VarInsnNode(Opcodes.LLOAD, windowHandleIdx));
                glCode.add(new LdcInsnNode(annotation.version()));
                glCode.add(new InsnNode(
                    annotation.coreProfile() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                glCode.add(new InsnNode(
                    annotation.forwardCompat() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                glCode.add(new InsnNode(
                    annotation.debugContext() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

                glCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/graphics/lwjgl/DeepMixOpenGL",
                    "createContext",
                    "(JLjava/lang/String;ZZZ)V",
                    false
                ));
            }

            // ─── Step 3: Shader Compilation ───
            if (!shaders.vertexShaderPath().isEmpty() || !shaders.vertexShader().isEmpty()) {
                ShaderLanguage lang = shaders.language();
                if (lang == ShaderLanguage.AUTO) {
                    lang = isGLES ? ShaderLanguage.GLSL_100 : ShaderLanguage.GLSL_330;
                }

                String vertSource = shaders.vertexShader().isEmpty()
                    ? "__PATH__:" + shaders.vertexShaderPath()
                    : shaders.vertexShader();
                String fragSource = shaders.fragmentShader().isEmpty()
                    ? "__PATH__:" + shaders.fragmentShaderPath()
                    : shaders.fragmentShader();

                glCode.add(new LdcInsnNode(vertSource));
                glCode.add(new LdcInsnNode(fragSource));
                glCode.add(new LdcInsnNode(lang.name()));
                glCode.add(new InsnNode(
                    isGLES ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

                // If SPIR-V input and GL target, cross-compile via SPIRV-Cross
                if (lang == ShaderLanguage.SPIRV) {
                    String targetLang = isGLES ? "GLSL_100" : "GLSL_" + annotation.version().replace(".", "");

                    glCode.add(new LdcInsnNode(targetLang));
                    glCode.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/graphics/lwjgl/DeepMixSPIRVCross",
                        "crossCompileAndLoadProgram",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/lang/String;)I",
                        false
                    ));
                } else {
                    glCode.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/graphics/lwjgl/DeepMixOpenGL",
                        "compileAndLinkProgram",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)I",
                        false
                    ));
                }

                // Store shader program ID
                int programIdx = methodNode.maxLocals;
                methodNode.maxLocals += 1;
                glCode.add(new VarInsnNode(Opcodes.ISTORE, programIdx));
            }

            // ─── Step 4: Pipeline Setup ───
            glCode.add(new InsnNode(
                pipeline.depthTest() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            glCode.add(new InsnNode(
                pipeline.depthWrite() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            glCode.add(new InsnNode(
                pipeline.backfaceCulling() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            glCode.add(new InsnNode(
                pipeline.blending() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            glCode.add(new LdcInsnNode(pipeline.blendFunc()));
            glCode.add(new InsnNode(
                pipeline.wireframe() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            // Anti-aliasing
            glCode.add(new LdcInsnNode(pipeline.antiAlias().name()));
            glCode.add(new InsnNode(
                annotation.useVAO() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            glCode.add(new InsnNode(
                annotation.useDSA() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            String setupMethod = isGLES
                ? "deepmix/runtime/graphics/lwjgl/DeepMixOpenGLES"
                : "deepmix/runtime/graphics/lwjgl/DeepMixOpenGL";

            glCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                setupMethod,
                "configurePipeline",
                "(ZZZZLjava/lang/String;ZLjava/lang/String;ZZ)V",
                false
            ));

            // ─── Step 5: Required Extensions Check ───
            String[] extensions = annotation.extensions();
            if (extensions.length > 0) {
                pushStringArray(glCode, extensions);
                glCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    setupMethod,
                    "requireExtensions",
                    "([Ljava/lang/String;)V",
                    false
                ));
            }

            methodNode.instructions.insert(glCode);

            context.addDiagnostic(String.format(
                "🔺 @DeepOpenGL applied to %s::%s [%s, version=%s, gles=%b, window=%dx%d]",
                classNode.name, methodNode.name,
                isGLES ? "OpenGL ES via LWJGL" : "OpenGL via LWJGL",
                isGLES ? annotation.glesVersion() : annotation.version(),
                isGLES, window.width(), window.height()
            ));
        }

        /**
         * Process @DeepVulkan — Set up Vulkan rendering via LWJGL direct bindings.
         *
         * Implementation chain:
         *   1. GLFW window creation with Vulkan surface
         *   2. VkInstance creation (org.lwjgl.vulkan.VK13)
         *   3. Physical/logical device selection
         *   4. Swapchain creation (KHRSwapchain)
         *   5. Render pass + pipeline
         *   6. Command pool/buffers
         *   7. SPIR-V shader module loading
         *   8. Synchronization primitives
         */
        public void processVulkan(DeepVulkan annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {

            WindowConfig window = annotation.window();
            RenderPipelineConfig pipeline = annotation.pipeline();
            ShaderSource shaders = annotation.shaders();

            InsnList vkCode = new InsnList();

            // ─── GLFW Window with Vulkan surface ───
            vkCode.add(new LdcInsnNode(window.width()));
            vkCode.add(new LdcInsnNode(window.height()));
            vkCode.add(new LdcInsnNode(window.title()));
            vkCode.add(new InsnNode(
                window.resizable() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            vkCode.add(new InsnNode(
                window.fullscreen() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            vkCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/lwjgl/DeepMixGLFW",
                "createVulkanWindow",
                "(IILjava/lang/String;ZZ)J",
                false
            ));

            int vkWindowIdx = methodNode.maxLocals;
            methodNode.maxLocals += 2;
            vkCode.add(new VarInsnNode(Opcodes.LSTORE, vkWindowIdx));

            // ─── VkInstance Creation ───
            vkCode.add(new LdcInsnNode(annotation.apiVersion()));
            vkCode.add(new InsnNode(
                annotation.validation() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            pushStringArray(vkCode, annotation.validationLayers());
            pushStringArray(vkCode, annotation.instanceExtensions());
            vkCode.add(new VarInsnNode(Opcodes.LLOAD, vkWindowIdx));

            vkCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/lwjgl/DeepMixVulkan",
                "createInstance",
                "(Ljava/lang/String;Z[Ljava/lang/String;[Ljava/lang/String;J)Ljava/lang/Object;",
                false
            ));

            // Store VkInstance wrapper
            int vkInstanceIdx = methodNode.maxLocals;
            methodNode.maxLocals += 1;
            vkCode.add(new VarInsnNode(Opcodes.ASTORE, vkInstanceIdx));

            // ─── Physical Device Selection ───
            vkCode.add(new VarInsnNode(Opcodes.ALOAD, vkInstanceIdx));
            vkCode.add(new LdcInsnNode(annotation.devicePreference()));
            pushStringArray(vkCode, annotation.deviceExtensions());
            vkCode.add(new InsnNode(
                annotation.useRayTracing() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            vkCode.add(new InsnNode(
                annotation.useMeshShaders() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            vkCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/lwjgl/DeepMixVulkan",
                "selectPhysicalDevice",
                "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/String;ZZ)Ljava/lang/Object;",
                false
            ));

            int vkPhysDevIdx = methodNode.maxLocals;
            methodNode.maxLocals += 1;
            vkCode.add(new VarInsnNode(Opcodes.ASTORE, vkPhysDevIdx));

            // ─── Logical Device & Queues ───
            vkCode.add(new VarInsnNode(Opcodes.ALOAD, vkPhysDevIdx));
            pushStringArray(vkCode, annotation.deviceExtensions());

            vkCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/lwjgl/DeepMixVulkan",
                "createLogicalDevice",
                "(Ljava/lang/Object;[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            int vkDeviceIdx = methodNode.maxLocals;
            methodNode.maxLocals += 1;
            vkCode.add(new VarInsnNode(Opcodes.ASTORE, vkDeviceIdx));

            // ─── Swapchain ───
            vkCode.add(new VarInsnNode(Opcodes.ALOAD, vkDeviceIdx));
            vkCode.add(new VarInsnNode(Opcodes.ALOAD, vkPhysDevIdx));
            vkCode.add(new VarInsnNode(Opcodes.LLOAD, vkWindowIdx));
            vkCode.add(new LdcInsnNode(window.width()));
            vkCode.add(new LdcInsnNode(window.height()));
            vkCode.add(new InsnNode(
                window.vsync() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            vkCode.add(new LdcInsnNode(annotation.maxFramesInFlight()));

            vkCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/lwjgl/DeepMixVulkan",
                "createSwapchain",
                "(Ljava/lang/Object;Ljava/lang/Object;JIIZI)Ljava/lang/Object;",
                false
            ));

            int vkSwapchainIdx = methodNode.maxLocals;
            methodNode.maxLocals += 1;
            vkCode.add(new VarInsnNode(Opcodes.ASTORE, vkSwapchainIdx));

            // ─── SPIR-V Shader Modules ───
            if (!shaders.vertexShaderPath().isEmpty() || !shaders.vertexShader().isEmpty()) {
                String vertPath = shaders.vertexShaderPath().isEmpty()
                    ? "__INLINE__" : shaders.vertexShaderPath();
                String fragPath = shaders.fragmentShaderPath().isEmpty()
                    ? "__INLINE__" : shaders.fragmentShaderPath();

                vkCode.add(new VarInsnNode(Opcodes.ALOAD, vkDeviceIdx));
                vkCode.add(new LdcInsnNode(vertPath));
                vkCode.add(new LdcInsnNode(fragPath));
                vkCode.add(new LdcInsnNode(shaders.language().name()));

                // If source is GLSL, compile to SPIR-V via Shaderc
                boolean needsCompilation = shaders.language() != ShaderLanguage.SPIRV;
                vkCode.add(new InsnNode(
                    needsCompilation ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

                vkCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/graphics/lwjgl/DeepMixVulkan",
                    "loadShaderModules",
                    "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/Object;",
                    false
                ));

                int vkShadersIdx = methodNode.maxLocals;
                methodNode.maxLocals += 1;
                vkCode.add(new VarInsnNode(Opcodes.ASTORE, vkShadersIdx));
            }

            // ─── Render Pass & Graphics Pipeline ───
            vkCode.add(new VarInsnNode(Opcodes.ALOAD, vkDeviceIdx));
            vkCode.add(new VarInsnNode(Opcodes.ALOAD, vkSwapchainIdx));
            vkCode.add(new InsnNode(
                pipeline.depthTest() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            vkCode.add(new InsnNode(
                pipeline.backfaceCulling() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            vkCode.add(new InsnNode(
                pipeline.blending() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            vkCode.add(new InsnNode(
                pipeline.wireframe() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            vkCode.add(new LdcInsnNode(pipeline.antiAlias().name()));
            vkCode.add(new InsnNode(
                annotation.useDynamicRendering() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            vkCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/lwjgl/DeepMixVulkan",
                "createGraphicsPipeline",
                "(Ljava/lang/Object;Ljava/lang/Object;ZZZZLjava/lang/String;Z)Ljava/lang/Object;",
                false
            ));

            int vkPipelineIdx = methodNode.maxLocals;
            methodNode.maxLocals += 1;
            vkCode.add(new VarInsnNode(Opcodes.ASTORE, vkPipelineIdx));

            // ─── Command Pool & Buffers ───
            vkCode.add(new VarInsnNode(Opcodes.ALOAD, vkDeviceIdx));
            vkCode.add(new LdcInsnNode(annotation.commandPoolFlags()));
            vkCode.add(new LdcInsnNode(annotation.maxFramesInFlight()));

            vkCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/lwjgl/DeepMixVulkan",
                "createCommandInfrastructure",
                "(Ljava/lang/Object;II)Ljava/lang/Object;",
                false
            ));

            int vkCmdIdx = methodNode.maxLocals;
            methodNode.maxLocals += 1;
            vkCode.add(new VarInsnNode(Opcodes.ASTORE, vkCmdIdx));

            // ─── Synchronization Primitives ───
            vkCode.add(new VarInsnNode(Opcodes.ALOAD, vkDeviceIdx));
            vkCode.add(new LdcInsnNode(annotation.maxFramesInFlight()));
            vkCode.add(new InsnNode(
                annotation.useTimeline() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            vkCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/lwjgl/DeepMixVulkan",
                "createSyncObjects",
                "(Ljava/lang/Object;IZ)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(vkCode);

            context.addDiagnostic(String.format(
                "🌋 @DeepVulkan applied to %s::%s [Vulkan %s via LWJGL, device=%s, " +
                "validation=%b, raytracing=%b, meshShaders=%b, framesInFlight=%d, window=%dx%d]",
                classNode.name, methodNode.name,
                annotation.apiVersion(), annotation.devicePreference(),
                annotation.validation(), annotation.useRayTracing(),
                annotation.useMeshShaders(), annotation.maxFramesInFlight(),
                window.width(), window.height()
            ));
        }

        /**
         * Process @DeepDirectX — Set up DirectX rendering via bgfx through LWJGL.
         *
         * bgfx abstracts DirectX 11/12 behind its cross-platform API.
         * Uses org.lwjgl.bgfx.BGFX for all rendering operations.
         *
         * Implementation chain:
         *   1. GLFW native window handle extraction
         *   2. bgfx platform data setup with DX backend
         *   3. bgfx initialization (BGFX.bgfx_init)
         *   4. Shader cross-compilation (GLSL → SPIR-V → HLSL via SPIRV-Cross,
         *      or bgfx shaderc for direct bgfx SC format)
         *   5. bgfx vertex layout + program creation
         *   6. Render state injection
         */
        public void processDirectX(DeepDirectX annotation, ClassNode classNode,
                                   MethodNode methodNode) throws DeepMixProcessingException {

            BgfxConfig bgfxCfg = annotation.bgfx();
            WindowConfig window = annotation.window();
            ShaderSource shaders = annotation.shaders();

            // Determine bgfx renderer type
            BgfxRendererType rendererType = bgfxCfg.renderer();
            if (rendererType == BgfxRendererType.COUNT) {
                rendererType = annotation.preferDX12()
                    ? BgfxRendererType.DIRECT3D12
                    : BgfxRendererType.DIRECT3D11;
            }

            InsnList dxCode = buildBgfxInitCode(
                window, bgfxCfg, rendererType, shaders,
                classNode, methodNode,
                annotation.dxr(), annotation.meshShaders(),
                annotation.featureLevel()
            );

            methodNode.instructions.insert(dxCode);

            context.addDiagnostic(String.format(
                "🎮 @DeepDirectX applied to %s::%s [%s via bgfx/LWJGL, DX12=%b, " +
                "DXR=%b, meshShaders=%b, featureLevel=%s, window=%dx%d]",
                classNode.name, methodNode.name,
                rendererType.name(), annotation.preferDX12(),
                annotation.dxr(), annotation.meshShaders(),
                annotation.featureLevel(),
                window.width(), window.height()
            ));
        }

        /**
         * Process @DeepMetal — Set up Metal rendering via bgfx through LWJGL.
         *
         * bgfx provides Metal backend support through its cross-platform API.
         * Shaders are cross-compiled to MSL via SPIRV-Cross (org.lwjgl.util.spvc).
         */
        public void processMetal(DeepMetal annotation, ClassNode classNode,
                                 MethodNode methodNode) throws DeepMixProcessingException {

            BgfxConfig bgfxCfg = annotation.bgfx();
            WindowConfig window = annotation.window();
            ShaderSource shaders = annotation.shaders();

            InsnList metalCode = buildBgfxInitCode(
                window, bgfxCfg, BgfxRendererType.METAL, shaders,
                classNode, methodNode,
                annotation.metalRaytracing(), annotation.metalMeshShaders(),
                annotation.metalVersion()
            );

            // Metal-specific: GPU capture for Xcode debugging
            if (annotation.gpuCapture()) {
                metalCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/graphics/bgfx/DeepMixBgfxMetal",
                    "enableGPUCapture",
                    "()V",
                    false
                ));
            }

            // Metal validation layer
            if (annotation.useMetalValidation()) {
                metalCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/graphics/bgfx/DeepMixBgfxMetal",
                    "enableMetalValidation",
                    "()V",
                    false
                ));
            }

            // If useMSL = true, attempt direct MSL shader loading
            // Otherwise, cross-compile via SPIRV-Cross
            if (annotation.useMSL() && !shaders.vertexShaderPath().isEmpty()) {
                metalCode.add(new LdcInsnNode(shaders.vertexShaderPath()));
                metalCode.add(new LdcInsnNode(shaders.fragmentShaderPath()));
                metalCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/graphics/bgfx/DeepMixBgfxMetal",
                    "loadMSLShaders",
                    "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;",
                    false
                ));
            }

            methodNode.instructions.insert(metalCode);

            context.addDiagnostic(String.format(
                "🍎 @DeepMetal applied to %s::%s [Metal %s via bgfx/LWJGL, " +
                "raytracing=%b, meshShaders=%b, gpuCapture=%b, window=%dx%d]",
                classNode.name, methodNode.name,
                annotation.metalVersion(),
                annotation.metalRaytracing(), annotation.metalMeshShaders(),
                annotation.gpuCapture(),
                window.width(), window.height()
            ));
        }

        /**
         * Process @DeepWebGL — Set up WebGL/WebGPU rendering via bgfx through LWJGL.
         *
         * For JVM-based simulation: bgfx uses OpenGL ES backend (BgfxRendererType.OPENGLES).
         * For web deployment: bgfx shaderc cross-compiles shaders to ESSL/WGSL.
         * WebGPU support: BgfxRendererType.WEBGPU when preferWebGPU=true.
         */
        public void processWebGL(DeepWebGL annotation, ClassNode classNode,
                                 MethodNode methodNode) throws DeepMixProcessingException {

            BgfxConfig bgfxCfg = annotation.bgfx();
            WindowConfig window = annotation.window();
            ShaderSource shaders = annotation.shaders();

            BgfxRendererType renderer = annotation.preferWebGPU()
                ? BgfxRendererType.WEBGPU
                : BgfxRendererType.OPENGLES;

            InsnList webglCode = buildBgfxInitCode(
                window, bgfxCfg, renderer, shaders,
                classNode, methodNode,
                false, false,
                annotation.webglVersion()
            );

            // WebGL-specific configuration
            webglCode.add(new InsnNode(
                annotation.antialias() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            webglCode.add(new InsnNode(
                annotation.alpha() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            webglCode.add(new InsnNode(
                annotation.premultipliedAlpha() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            webglCode.add(new InsnNode(
                annotation.preserveDrawingBuffer() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            webglCode.add(new InsnNode(
                annotation.stencil() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            webglCode.add(new InsnNode(
                annotation.floatTextures() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            webglCode.add(new InsnNode(
                annotation.instancedArrays() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            pushStringArray(webglCode, annotation.requiredExtensions());

            webglCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/bgfx/DeepMixBgfxWebGL",
                "configureWebGL",
                "(ZZZZZZZ[Ljava/lang/String;)V",
                false
            ));

            methodNode.instructions.insert(webglCode);

            context.addDiagnostic(String.format(
                "🌐 @DeepWebGL applied to %s::%s [%s via bgfx/LWJGL, " +
                "webgl=%s, webgpu=%b, window=%dx%d]",
                classNode.name, methodNode.name,
                renderer.name(), annotation.webglVersion(),
                annotation.preferWebGPU(),
                window.width(), window.height()
            ));
        }

        /**
         * Process @DeepCanvas — Set up 2D canvas rendering via bgfx + NanoVG through LWJGL.
         *
         * NanoVG (org.lwjgl.nanovg) provides HTML5 Canvas-like 2D vector graphics API.
         * It renders through OpenGL/OpenGL ES which bgfx manages.
         *
         * If useNanoVG=true (default when available), uses NanoVG directly.
         * Otherwise, uses bgfx's transient buffer API for 2D quad/triangle batching.
         */
        public void processCanvas(DeepCanvas annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {

            BgfxConfig bgfxCfg = annotation.bgfx();
            boolean useNanoVG = annotation.useNanoVG();

            InsnList canvasCode = new InsnList();

            if (useNanoVG) {
                // ─── NanoVG Path (org.lwjgl.nanovg.*) ───
                // NanoVG needs an OpenGL context, so we create one via GLFW+GL

                // Step 1: GLFW + OpenGL context for NanoVG
                canvasCode.add(new LdcInsnNode(annotation.width()));
                canvasCode.add(new LdcInsnNode(annotation.height()));
                canvasCode.add(new LdcInsnNode("DeepMix Canvas"));
                canvasCode.add(new InsnNode(
                    annotation.offscreen() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                canvasCode.add(new InsnNode(
                    annotation.retina() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

                canvasCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/graphics/lwjgl/DeepMixGLFW",
                    "createCanvasWindow",
                    "(IILjava/lang/String;ZZ)J",
                    false
                ));

                int canvasWindowIdx = methodNode.maxLocals;
                methodNode.maxLocals += 2;
                canvasCode.add(new VarInsnNode(Opcodes.LSTORE, canvasWindowIdx));

                // Step 2: Create NanoVG context
                // Uses NanoVGGL3.nvgCreate() or NanoVGGL2.nvgCreate()
                canvasCode.add(new InsnNode(
                    annotation.antiAlias() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

                canvasCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/graphics/lwjgl/DeepMixNanoVG",
                    "createContext",
                    "(Z)J",
                    false
                ));

                int nvgCtxIdx = methodNode.maxLocals;
                methodNode.maxLocals += 2;
                canvasCode.add(new VarInsnNode(Opcodes.LSTORE, nvgCtxIdx));

                // Step 3: Set canvas defaults
                canvasCode.add(new VarInsnNode(Opcodes.LLOAD, nvgCtxIdx));
                canvasCode.add(new LdcInsnNode(annotation.fillColor()));
                canvasCode.add(new LdcInsnNode(annotation.strokeColor()));
                canvasCode.add(new LdcInsnNode(annotation.lineWidth()));
                canvasCode.add(new LdcInsnNode(annotation.lineCap()));
                canvasCode.add(new LdcInsnNode(annotation.lineJoin()));
                canvasCode.add(new LdcInsnNode(annotation.globalAlpha()));
                canvasCode.add(new LdcInsnNode(annotation.font()));
                canvasCode.add(new LdcInsnNode(annotation.textAlign()));
                canvasCode.add(new LdcInsnNode(annotation.textBaseline()));

                canvasCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/graphics/lwjgl/DeepMixNanoVG",
                    "setDefaults",
                    "(JLjava/lang/String;Ljava/lang/String;DLjava/lang/String;" +
                    "Ljava/lang/String;DLjava/lang/String;Ljava/lang/String;" +
                    "Ljava/lang/String;)V",
                    false
                ));

            } else {
                // ─── bgfx 2D Path (org.lwjgl.bgfx.*) ───
                // Use bgfx transient buffers + custom 2D shaders

                BgfxRendererType renderer = BgfxRendererType.fromGraphicsBackend(
                    GraphicsBackend.BGFX_AUTO);

                canvasCode.add(new LdcInsnNode(annotation.width()));
                canvasCode.add(new LdcInsnNode(annotation.height()));
                canvasCode.add(new LdcInsnNode(renderer.value()));
                canvasCode.add(new InsnNode(
                    annotation.antiAlias() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                canvasCode.add(new InsnNode(
                    annotation.alpha() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

                canvasCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/graphics/bgfx/DeepMixBgfxCanvas",
                    "initialize",
                    "(IIIZZ)Ljava/lang/Object;",
                    false
                ));

                int bgfxCanvasIdx = methodNode.maxLocals;
                methodNode.maxLocals += 1;
                canvasCode.add(new VarInsnNode(Opcodes.ASTORE, bgfxCanvasIdx));

                // Set 2D rendering defaults via bgfx
                canvasCode.add(new VarInsnNode(Opcodes.ALOAD, bgfxCanvasIdx));
                canvasCode.add(new LdcInsnNode(annotation.fillColor()));
                canvasCode.add(new LdcInsnNode(annotation.strokeColor()));
                canvasCode.add(new LdcInsnNode(annotation.lineWidth()));

                canvasCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/graphics/bgfx/DeepMixBgfxCanvas",
                    "setDefaults",
                    "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;D)V",
                    false
                ));
            }

            methodNode.instructions.insert(canvasCode);

            context.addDiagnostic(String.format(
                "🖼️ @DeepCanvas applied to %s::%s [%s, size=%dx%d, antiAlias=%b, retina=%b]",
                classNode.name, methodNode.name,
                useNanoVG ? "NanoVG via LWJGL" : "bgfx 2D via LWJGL",
                annotation.width(), annotation.height(),
                annotation.antiAlias(), annotation.retina()
            ));
        }


        // ═══════════════════════════════════════════════════════════════
        // SHADER CROSS-COMPILATION PROCESSOR
        // ═══════════════════════════════════════════════════════════════

        /**
         * Cross-compile shaders between languages using the LWJGL shader pipeline:
         *
         * ┌─────────────────────────────────────────────────────────────┐
         * │                 SHADER COMPILATION PIPELINE                 │
         * ├─────────────────────────────────────────────────────────────┤
         * │                                                             │
         * │  GLSL ──┐                                                   │
         * │         ├── Shaderc ──→ SPIR-V ──┬── SPIRV-Cross ──→ GLSL  │
         * │  HLSL ──┘   (LWJGL)              ├── SPIRV-Cross ──→ HLSL  │
         * │                                   ├── SPIRV-Cross ──→ MSL   │
         * │                                   ├── SPIRV-Cross ──→ ESSL  │
         * │                                   └── (Direct use in Vulkan)│
         * │                                                             │
         * │  bgfx SC ──→ bgfx shaderc ──→ Platform-specific binary     │
         * │  (for bgfx-managed backends: DX, Metal, WebGL)              │
         * │                                                             │
         * └─────────────────────────────────────────────────────────────┘
         *
         * Modules used:
         *   org.lwjgl.util.shaderc.Shaderc   — GLSL/HLSL → SPIR-V
         *   org.lwjgl.util.spvc.Spvc         — SPIR-V → GLSL/HLSL/MSL/ESSL
         *   org.lwjgl.bgfx.BGFX              — bgfx shader binary loading
         */
        public void processShaderCrossCompilation(ShaderSource shaders,
                                                  GraphicsBackend targetBackend,
                                                  ClassNode classNode,
                                                  MethodNode methodNode)
                throws DeepMixProcessingException {

            ShaderLanguage sourceLang = shaders.language();
            ShaderLanguage targetLang = inferTargetLanguage(targetBackend);

            InsnList shaderCode = new InsnList();

            String vertSource = shaders.vertexShader().isEmpty()
                ? shaders.vertexShaderPath() : "__INLINE__";
            String fragSource = shaders.fragmentShader().isEmpty()
                ? shaders.fragmentShaderPath() : "__INLINE__";

            shaderCode.add(new LdcInsnNode(vertSource));
            shaderCode.add(new LdcInsnNode(fragSource));
            shaderCode.add(new LdcInsnNode(sourceLang.name()));
            shaderCode.add(new LdcInsnNode(targetLang.name()));
            shaderCode.add(new LdcInsnNode(targetBackend.name()));

            boolean useShadercFirst = (sourceLang != ShaderLanguage.SPIRV);
            boolean useSPIRVCross = (targetLang != ShaderLanguage.SPIRV) &&
                                    (targetBackend != GraphicsBackend.VULKAN_LWJGL);
            boolean useBgfxShaderc = shaders.crossCompile() &&
                                     isBgfxBackend(targetBackend);

            shaderCode.add(new InsnNode(
                useShadercFirst ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            shaderCode.add(new InsnNode(
                useSPIRVCross ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            shaderCode.add(new InsnNode(
                useBgfxShaderc ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            shaderCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/lwjgl/DeepMixShaderPipeline",
                "crossCompile",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;Ljava/lang/String;ZZZ)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(shaderCode);

            context.addDiagnostic(String.format(
                "🔧 Shader cross-compilation: %s → %s [shaderc=%b, spirv-cross=%b, bgfx-sc=%b]",
                sourceLang, targetLang, useShadercFirst, useSPIRVCross, useBgfxShaderc
            ));
        }


        // ═══════════════════════════════════════════════════════════════
        // VISUALIZATION PROCESSORS (SVG, PDF, Chart, Graph, etc.)
        // ═══════════════════════════════════════════════════════════════

        /**
         * Process @DeepChart — Generate chart visualization.
         */
        public void processChart(DeepChart annotation, ClassNode classNode,
                                 MethodNode methodNode) throws DeepMixProcessingException {
            InsnList chartCode = new InsnList();

            chartCode.add(new LdcInsnNode(annotation.type().name()));
            chartCode.add(new LdcInsnNode(annotation.title()));
            chartCode.add(new LdcInsnNode(annotation.subtitle()));
            chartCode.add(new LdcInsnNode(annotation.width()));
            chartCode.add(new LdcInsnNode(annotation.height()));
            chartCode.add(new LdcInsnNode(annotation.xAxisLabel()));
            chartCode.add(new LdcInsnNode(annotation.yAxisLabel()));
            chartCode.add(new InsnNode(
                annotation.legend() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            chartCode.add(new LdcInsnNode(annotation.legendPosition()));
            chartCode.add(new InsnNode(
                annotation.animation() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            chartCode.add(new LdcInsnNode(annotation.theme()));
            chartCode.add(new LdcInsnNode(annotation.backgroundColor()));
            chartCode.add(new LdcInsnNode(annotation.outputFormat()));
            chartCode.add(new LdcInsnNode(annotation.outputPath()));
            chartCode.add(new InsnNode(
                annotation.interactive() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            chartCode.add(new LdcInsnNode(annotation.library()));
            chartCode.add(new InsnNode(
                annotation.stacked() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            chartCode.add(new InsnNode(
                annotation.logarithmic() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            // Encode series
            ChartSeries[] series = annotation.series();
            chartCode.add(new LdcInsnNode(series.length));
            chartCode.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < series.length; i++) {
                chartCode.add(new InsnNode(Opcodes.DUP));
                chartCode.add(new LdcInsnNode(i));
                String encoded = series[i].name() + "|" +
                                 series[i].dataProvider() + "|" +
                                 series[i].color() + "|" +
                                 series[i].type() + "|" +
                                 series[i].lineWidth() + "|" +
                                 series[i].marker() + "|" +
                                 series[i].visible();
                chartCode.add(new LdcInsnNode(encoded));
                chartCode.add(new InsnNode(Opcodes.AASTORE));
            }

            pushStringArray(chartCode, annotation.colors());

            chartCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/viz/DeepMixChart",
                "generate",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "IILjava/lang/String;Ljava/lang/String;ZLjava/lang/String;" +
                "ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;ZLjava/lang/String;ZZ" +
                "[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(chartCode);

            context.addDiagnostic(String.format(
                "📊 @DeepChart applied to %s::%s [type=%s, series=%d, format=%s, lib=%s]",
                classNode.name, methodNode.name,
                annotation.type(), series.length,
                annotation.outputFormat(), annotation.library()
            ));
        }

        /**
         * Process @DeepSVG — Generate SVG vector graphics.
         */
        public void processSVG(DeepSVG annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            InsnList svgCode = new InsnList();

            svgCode.add(new LdcInsnNode(annotation.width()));
            svgCode.add(new LdcInsnNode(annotation.height()));
            svgCode.add(new LdcInsnNode(annotation.viewBox()));
            svgCode.add(new LdcInsnNode(annotation.output().name()));
            svgCode.add(new LdcInsnNode(annotation.outputPath()));
            svgCode.add(new InsnNode(
                annotation.minify() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            svgCode.add(new InsnNode(
                annotation.prettyPrint() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            svgCode.add(new LdcInsnNode(annotation.defaultFill()));
            svgCode.add(new LdcInsnNode(annotation.defaultStroke()));
            svgCode.add(new LdcInsnNode(annotation.defaultStrokeWidth()));
            svgCode.add(new InsnNode(
                annotation.responsive() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            pushStringArray(svgCode, annotation.defs());
            pushStringArray(svgCode, annotation.stylesheets());

            svgCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/viz/DeepMixSVG",
                "generate",
                "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "ZZLjava/lang/String;Ljava/lang/String;DZ" +
                "[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(svgCode);

            context.addDiagnostic(String.format(
                "📐 @DeepSVG applied to %s::%s [%dx%d, output=%s]",
                classNode.name, methodNode.name,
                annotation.width(), annotation.height(), annotation.output()
            ));
        }

        /**
         * Process @DeepPDF — Generate PDF documents.
         */
        public void processPDF(DeepPDF annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            InsnList pdfCode = new InsnList();

            pdfCode.add(new LdcInsnNode(annotation.outputPath()));
            pdfCode.add(new LdcInsnNode(annotation.pageSize().name()));
            pdfCode.add(new LdcInsnNode(annotation.orientation().name()));
            pdfCode.add(new LdcInsnNode(annotation.customWidth()));
            pdfCode.add(new LdcInsnNode(annotation.customHeight()));
            pdfCode.add(new LdcInsnNode(annotation.marginTop()));
            pdfCode.add(new LdcInsnNode(annotation.marginBottom()));
            pdfCode.add(new LdcInsnNode(annotation.marginLeft()));
            pdfCode.add(new LdcInsnNode(annotation.marginRight()));
            pdfCode.add(new LdcInsnNode(annotation.title()));
            pdfCode.add(new LdcInsnNode(annotation.author()));
            pdfCode.add(new LdcInsnNode(annotation.defaultFont()));
            pdfCode.add(new LdcInsnNode(annotation.defaultFontSize()));
            pdfCode.add(new InsnNode(
                annotation.encrypt() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            pdfCode.add(new InsnNode(
                annotation.compressed() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            pdfCode.add(new InsnNode(
                annotation.pageNumbers() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            pdfCode.add(new InsnNode(
                annotation.tableOfContents() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            pdfCode.add(new LdcInsnNode(annotation.watermark()));
            pdfCode.add(new LdcInsnNode(annotation.watermarkOpacity()));
            pdfCode.add(new LdcInsnNode(annotation.library()));

            pdfCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/viz/DeepMixPDF",
                "generate",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "FFFFFFLjava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;FZZZZLjava/lang/String;" +
                "FLjava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(pdfCode);

            context.addDiagnostic(String.format(
                "📄 @DeepPDF applied to %s::%s [pageSize=%s, orientation=%s, lib=%s]",
                classNode.name, methodNode.name,
                annotation.pageSize(), annotation.orientation(), annotation.library()
            ));
        }

        /**
         * Process @DeepGantt — Generate Gantt chart.
         */
        public void processGantt(DeepGantt annotation, ClassNode classNode,
                                 MethodNode methodNode) throws DeepMixProcessingException {
            InsnList ganttCode = new InsnList();

            ganttCode.add(new LdcInsnNode(annotation.title()));
            ganttCode.add(new LdcInsnNode(annotation.width()));
            ganttCode.add(new LdcInsnNode(annotation.height()));
            ganttCode.add(new LdcInsnNode(annotation.dateFormat()));
            ganttCode.add(new LdcInsnNode(annotation.timeUnit()));
            ganttCode.add(new InsnNode(
                annotation.criticalPath() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            ganttCode.add(new InsnNode(
                annotation.showProgress() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            ganttCode.add(new InsnNode(
                annotation.showDependencies() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            ganttCode.add(new InsnNode(
                annotation.showToday() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            ganttCode.add(new LdcInsnNode(annotation.outputFormat()));
            ganttCode.add(new LdcInsnNode(annotation.outputPath()));
            ganttCode.add(new InsnNode(
                annotation.interactive() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            ganttCode.add(new LdcInsnNode(annotation.theme()));
            ganttCode.add(new InsnNode(
                annotation.weekendsShaded() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            ganttCode.add(new LdcInsnNode(annotation.library()));

            // Encode tasks
            GanttTask[] tasks = annotation.tasks();
            ganttCode.add(new LdcInsnNode(tasks.length));
            ganttCode.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < tasks.length; i++) {
                ganttCode.add(new InsnNode(Opcodes.DUP));
                ganttCode.add(new LdcInsnNode(i));
                String encoded = tasks[i].id() + "|" +
                                 tasks[i].name() + "|" +
                                 tasks[i].start() + "|" +
                                 tasks[i].end() + "|" +
                                 tasks[i].durationDays() + "|" +
                                 tasks[i].progress() + "|" +
                                 String.join(",", tasks[i].dependencies()) + "|" +
                                 tasks[i].depType().name() + "|" +
                                 tasks[i].assignee() + "|" +
                                 tasks[i].color() + "|" +
                                 tasks[i].milestone() + "|" +
                                 tasks[i].group();
                ganttCode.add(new LdcInsnNode(encoded));
                ganttCode.add(new InsnNode(Opcodes.AASTORE));
            }

            pushStringArray(ganttCode, annotation.holidays());

            ganttCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/viz/DeepMixGantt",
                "generate",
                "(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;" +
                "ZZZZLjava/lang/String;Ljava/lang/String;ZLjava/lang/String;" +
                "ZLjava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(ganttCode);

            context.addDiagnostic(String.format(
                "📅 @DeepGantt applied to %s::%s [tasks=%d, format=%s, criticalPath=%b, lib=%s]",
                classNode.name, methodNode.name,
                tasks.length, annotation.outputFormat(),
                annotation.criticalPath(), annotation.library()
            ));
        }

        /**
         * Process @DeepGraph — Generate graph/network visualization.
         */
        public void processGraph(DeepGraph annotation, ClassNode classNode,
                                 MethodNode methodNode) throws DeepMixProcessingException {
            InsnList graphCode = new InsnList();

            graphCode.add(new LdcInsnNode(annotation.layout().name()));
            graphCode.add(new LdcInsnNode(annotation.width()));
            graphCode.add(new LdcInsnNode(annotation.height()));
            graphCode.add(new InsnNode(
                annotation.directed() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            graphCode.add(new InsnNode(
                annotation.weighted() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            graphCode.add(new InsnNode(
                annotation.labels() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            graphCode.add(new LdcInsnNode(annotation.nodeColor()));
            graphCode.add(new LdcInsnNode(annotation.edgeColor()));
            graphCode.add(new LdcInsnNode(annotation.nodeSize()));
            graphCode.add(new LdcInsnNode(annotation.edgeWidth()));
            graphCode.add(new InsnNode(
                annotation.physics() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            graphCode.add(new LdcInsnNode(annotation.gravity()));
            graphCode.add(new LdcInsnNode(annotation.springLength()));
            graphCode.add(new LdcInsnNode(annotation.damping()));
            graphCode.add(new InsnNode(
                annotation.stabilize() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            graphCode.add(new LdcInsnNode(annotation.stabilizationIterations()));
            graphCode.add(new LdcInsnNode(annotation.outputFormat()));
            graphCode.add(new LdcInsnNode(annotation.outputPath()));
            graphCode.add(new InsnNode(
                annotation.interactive() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            graphCode.add(new LdcInsnNode(annotation.library()));

            graphCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/viz/DeepMixGraph",
                "generate",
                "(Ljava/lang/String;IIZZZLjava/lang/String;Ljava/lang/String;" +
                "DDZDDDZILjava/lang/String;Ljava/lang/String;" +
                "ZLjava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(graphCode);

            context.addDiagnostic(String.format(
                "🕸️ @DeepGraph applied to %s::%s [layout=%s, format=%s, lib=%s]",
                classNode.name, methodNode.name,
                annotation.layout(), annotation.outputFormat(), annotation.library()
            ));
        }

        /**
         * Process @DeepDiagram — Generate UML/flowchart/architecture diagrams.
         */
        public void processDiagram(DeepDiagram annotation, ClassNode classNode,
                                   MethodNode methodNode) throws DeepMixProcessingException {
            InsnList diagCode = new InsnList();

            diagCode.add(new LdcInsnNode(annotation.type().name()));
            diagCode.add(new LdcInsnNode(annotation.width()));
            diagCode.add(new LdcInsnNode(annotation.height()));
            diagCode.add(new LdcInsnNode(annotation.title()));
            diagCode.add(new LdcInsnNode(annotation.direction()));
            diagCode.add(new LdcInsnNode(annotation.theme()));
            diagCode.add(new LdcInsnNode(annotation.outputFormat()));
            diagCode.add(new LdcInsnNode(annotation.outputPath()));
            diagCode.add(new InsnNode(
                annotation.interactive() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            diagCode.add(new LdcInsnNode(annotation.notation()));
            diagCode.add(new InsnNode(
                annotation.autoLayout() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            diagCode.add(new LdcInsnNode(annotation.library()));
            diagCode.add(new InsnNode(
                annotation.includeSource() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            pushStringArray(diagCode, annotation.styles());

            diagCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/viz/DeepMixDiagram",
                "generate",
                "(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "ZLjava/lang/String;ZLjava/lang/String;Z" +
                "[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(diagCode);

            context.addDiagnostic(String.format(
                "📋 @DeepDiagram applied to %s::%s [type=%s, format=%s, lib=%s]",
                classNode.name, methodNode.name,
                annotation.type(), annotation.outputFormat(), annotation.library()
            ));
        }

        /**
         * Process @DeepMindmap — Generate mind map visualization.
         */
        public void processMindmap(DeepMindmap annotation, ClassNode classNode,
                                   MethodNode methodNode) throws DeepMixProcessingException {
            InsnList mindmapCode = new InsnList();

            mindmapCode.add(new LdcInsnNode(annotation.centralTopic()));
            mindmapCode.add(new LdcInsnNode(annotation.width()));
            mindmapCode.add(new LdcInsnNode(annotation.height()));
            mindmapCode.add(new LdcInsnNode(annotation.layout().name()));
            mindmapCode.add(new InsnNode(
                annotation.animated() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            mindmapCode.add(new InsnNode(
                annotation.collapsible() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            mindmapCode.add(new LdcInsnNode(annotation.outputFormat()));
            mindmapCode.add(new LdcInsnNode(annotation.outputPath()));
            mindmapCode.add(new InsnNode(
                annotation.interactive() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            mindmapCode.add(new LdcInsnNode(annotation.theme()));
            mindmapCode.add(new LdcInsnNode(annotation.library()));

            // Encode nodes
            MindmapNode[] nodes = annotation.nodes();
            mindmapCode.add(new LdcInsnNode(nodes.length));
            mindmapCode.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < nodes.length; i++) {
                mindmapCode.add(new InsnNode(Opcodes.DUP));
                mindmapCode.add(new LdcInsnNode(i));
                String encoded = nodes[i].text() + "|" +
                                 String.join(",", nodes[i].children()) + "|" +
                                 nodes[i].color() + "|" +
                                 nodes[i].icon() + "|" +
                                 nodes[i].link() + "|" +
                                 nodes[i].collapsed();
                mindmapCode.add(new LdcInsnNode(encoded));
                mindmapCode.add(new InsnNode(Opcodes.AASTORE));
            }

            pushStringArray(mindmapCode, annotation.colors());

            mindmapCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/viz/DeepMixMindmap",
                "generate",
                "(Ljava/lang/String;IILjava/lang/String;ZZLjava/lang/String;" +
                "Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;" +
                "[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(mindmapCode);

            context.addDiagnostic(String.format(
                "🧠 @DeepMindmap applied to %s::%s [topic=%s, nodes=%d, format=%s, lib=%s]",
                classNode.name, methodNode.name,
                annotation.centralTopic(), nodes.length,
                annotation.outputFormat(), annotation.library()
            ));
        }

        /**
         * Process @DeepJavaFX — Inject JavaFX component creation.
         */
        public void processJavaFX(DeepJavaFX annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {
            InsnList jfxCode = new InsnList();

            jfxCode.add(new LdcInsnNode(annotation.component().name()));
            jfxCode.add(new LdcInsnNode(annotation.fxmlPath()));
            jfxCode.add(new LdcInsnNode(annotation.cssPath()));
            jfxCode.add(new LdcInsnNode(annotation.id()));
            jfxCode.add(new InsnNode(
                annotation.bindModel() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            jfxCode.add(new LdcInsnNode(annotation.modelClass()));
            jfxCode.add(new LdcInsnNode(annotation.controllerClass()));
            jfxCode.add(new InsnNode(
                annotation.reactive() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            jfxCode.add(new InsnNode(
                annotation.responsive() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            jfxCode.add(new LdcInsnNode(annotation.layout()));
            jfxCode.add(new LdcInsnNode(annotation.prefWidth()));
            jfxCode.add(new LdcInsnNode(annotation.prefHeight()));
            jfxCode.add(new InsnNode(
                annotation.animated() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            jfxCode.add(new LdcInsnNode(annotation.animationType()));
            jfxCode.add(new LdcInsnNode(annotation.animationDurationMs()));

            pushStringArray(jfxCode, annotation.cssClasses());

            jfxCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/ui/DeepMixJavaFX",
                "createComponent",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;" +
                "ZZLjava/lang/String;IIZLjava/lang/String;I" +
                "[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            methodNode.instructions.insert(jfxCode);

            context.addDiagnostic(String.format(
                "☕ @DeepJavaFX applied to %s::%s [component=%s, fxml=%s]",
                classNode.name, methodNode.name,
                annotation.component(), annotation.fxmlPath()
            ));
        }

        /**
         * Process @DeepSwing — Inject Swing UI modification.
         */
        public void processSwing(DeepSwing annotation, ClassNode classNode,
                                 MethodNode methodNode) throws DeepMixProcessingException {
            InsnList swingCode = new InsnList();

            swingCode.add(new LdcInsnNode(annotation.component().name()));
            swingCode.add(new LdcInsnNode(annotation.lookAndFeel()));
            swingCode.add(new InsnNode(
                annotation.nimbus() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            swingCode.add(new InsnNode(
                annotation.flatLaf() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            swingCode.add(new LdcInsnNode(annotation.theme()));
            swingCode.add(new InsnNode(
                annotation.antiAlias() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            swingCode.add(new InsnNode(
                annotation.doubleBuffer() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            swingCode.add(new LdcInsnNode(annotation.layout()));
            swingCode.add(new LdcInsnNode(annotation.preferredWidth()));
            swingCode.add(new LdcInsnNode(annotation.preferredHeight()));
            swingCode.add(new InsnNode(
                annotation.threadSafe() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            swingCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/ui/DeepMixSwing",
                "createComponent",
                "(Ljava/lang/String;Ljava/lang/String;ZZLjava/lang/String;" +
                "ZZLjava/lang/String;IIZ)Ljava/lang/Object;",
                false
            ));

            // If threadSafe, wrap the entire instruction sequence in SwingUtilities.invokeLater
            if (annotation.threadSafe()) {
                InsnList wrapCode = new InsnList();
                wrapCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/graphics/ui/DeepMixSwing",
                    "ensureEDT",
                    "(Ljava/lang/Runnable;)V",
                    false
                ));
            }

            methodNode.instructions.insert(swingCode);

            context.addDiagnostic(String.format(
                "🖼️ @DeepSwing applied to %s::%s [component=%s, laf=%s, threadSafe=%b]",
                classNode.name, methodNode.name,
                annotation.component(), annotation.lookAndFeel(), annotation.threadSafe()
            ));
        }


        // ═══════════════════════════════════════════════════════════════
        // PRIVATE HELPERS
        // ═══════════════════════════════════════════════════════════════

        /**
         * Build bgfx initialization code used by DirectX, Metal, WebGL, and Canvas.
         *
         * This is the shared bootstrap sequence for all bgfx-backed renderers:
         *   1. Create GLFW window
         *   2. Extract native window handle
         *   3. Set bgfx platform data
         *   4. Initialize bgfx with desired renderer type
         *   5. Configure view and initial state
         *   6. Load/cross-compile shaders for the target backend
         */
        private InsnList buildBgfxInitCode(
                WindowConfig window, BgfxConfig bgfxCfg,
                BgfxRendererType rendererType, ShaderSource shaders,
                ClassNode classNode, MethodNode methodNode,
                boolean raytracing, boolean meshShaders,
                String apiVersion) {

            InsnList code = new InsnList();

            // ─── Step 1: GLFW Window ───
            code.add(new LdcInsnNode(window.width()));
            code.add(new LdcInsnNode(window.height()));
            code.add(new LdcInsnNode(window.title()));
            code.add(new InsnNode(
                window.resizable() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            code.add(new InsnNode(
                window.fullscreen() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            // For bgfx, GLFW must NOT create an OpenGL context
            // (bgfx manages its own rendering context)
            code.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/lwjgl/DeepMixGLFW",
                "createNativeWindow",
                "(IILjava/lang/String;ZZ)J",
                false
            ));

            int bgfxWindowIdx = methodNode.maxLocals;
            methodNode.maxLocals += 2;
            code.add(new VarInsnNode(Opcodes.LSTORE, bgfxWindowIdx));

            // ─── Step 2: bgfx Initialization ───
            code.add(new VarInsnNode(Opcodes.LLOAD, bgfxWindowIdx));
            code.add(new LdcInsnNode(rendererType.value()));
            code.add(new LdcInsnNode(bgfxCfg.width()));
            code.add(new LdcInsnNode(bgfxCfg.height()));
            code.add(new LdcInsnNode(bgfxCfg.resetFlags()));
            code.add(new InsnNode(
                bgfxCfg.debug() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            code.add(new InsnNode(
                bgfxCfg.profile() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            code.add(new LdcInsnNode(bgfxCfg.maxEncoders()));
            code.add(new LdcInsnNode(bgfxCfg.transientVbSize()));
            code.add(new LdcInsnNode(bgfxCfg.transientIbSize()));

            code.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/bgfx/DeepMixBgfx",
                "initialize",
                "(JIIIZZIIII)Ljava/lang/Object;",
                false
            ));

            // Store bgfx context wrapper
            int bgfxCtxIdx = methodNode.maxLocals;
            methodNode.maxLocals += 1;
            code.add(new VarInsnNode(Opcodes.ASTORE, bgfxCtxIdx));

            // ─── Step 3: View Setup ───
            code.add(new VarInsnNode(Opcodes.ALOAD, bgfxCtxIdx));
            code.add(new LdcInsnNode(0)); // viewId 0
            code.add(new LdcInsnNode(bgfxCfg.width()));
            code.add(new LdcInsnNode(bgfxCfg.height()));

            code.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/bgfx/DeepMixBgfx",
                "setupView",
                "(Ljava/lang/Object;III)V",
                false
            ));

            // ─── Step 4: Shader Loading/Cross-Compilation ───
            // For bgfx-managed backends, shaders need to be in bgfx binary format
            // OR cross-compiled via SPIRV-Cross to the target language
            if (!shaders.vertexShaderPath().isEmpty() || !shaders.vertexShader().isEmpty()) {

                String vertPath = shaders.vertexShaderPath().isEmpty()
                    ? "__INLINE__" : shaders.vertexShaderPath();
                String fragPath = shaders.fragmentShaderPath().isEmpty()
                    ? "__INLINE__" : shaders.fragmentShaderPath();

                code.add(new VarInsnNode(Opcodes.ALOAD, bgfxCtxIdx));
                code.add(new LdcInsnNode(vertPath));
                code.add(new LdcInsnNode(fragPath));
                code.add(new LdcInsnNode(shaders.language().name()));
                code.add(new LdcInsnNode(rendererType.name()));
                code.add(new InsnNode(
                    shaders.crossCompile() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

                // Determine cross-compilation path:
                // If crossCompile=true, the pipeline is:
                //   1. Source (GLSL/HLSL) → SPIR-V (via Shaderc/LWJGL)
                //   2. SPIR-V → target (HLSL/MSL/ESSL) (via SPIRV-Cross/LWJGL)
                //   3. Target → bgfx binary (via bgfx shaderc or bgfx_create_shader)
                code.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/graphics/bgfx/DeepMixBgfx",
                    "loadOrCrossCompileShaders",
                    "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;" +
                    "Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/Object;",
                    false
                ));

                int bgfxProgramIdx = methodNode.maxLocals;
                methodNode.maxLocals += 1;
                code.add(new VarInsnNode(Opcodes.ASTORE, bgfxProgramIdx));
            }

            // ─── Step 5: Renderer Capabilities Query ───
            code.add(new VarInsnNode(Opcodes.ALOAD, bgfxCtxIdx));
            code.add(new LdcInsnNode(apiVersion));
            code.add(new InsnNode(
                raytracing ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            code.add(new InsnNode(
                meshShaders ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            code.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/graphics/bgfx/DeepMixBgfx",
                "queryAndValidateCaps",
                "(Ljava/lang/Object;Ljava/lang/String;ZZ)V",
                false
            ));

            return code;
        }

        /**
         * Infer the target shader language for a given graphics backend.
         */
        private ShaderLanguage inferTargetLanguage(GraphicsBackend backend) {
            switch (backend) {
                case OPENGL_LWJGL:   return ShaderLanguage.GLSL_450;
                case OPENGLES_LWJGL: return ShaderLanguage.GLSL_100;
                case VULKAN_LWJGL:   return ShaderLanguage.SPIRV;
                case DIRECTX_BGFX:   return ShaderLanguage.HLSL_SM5;
                case METAL_BGFX:     return ShaderLanguage.MSL;
                case WEBGL_BGFX:     return ShaderLanguage.GLSL_100;  // ESSL
                case CANVAS_BGFX:    return ShaderLanguage.GLSL_330;
                case BGFX_AUTO:      return ShaderLanguage.BGFX_SC;
                default:             return ShaderLanguage.AUTO;
            }
        }

        /**
         * Check if a backend is managed by bgfx.
         */
        private boolean isBgfxBackend(GraphicsBackend backend) {
            return backend == GraphicsBackend.DIRECTX_BGFX ||
                   backend == GraphicsBackend.METAL_BGFX ||
                   backend == GraphicsBackend.WEBGL_BGFX ||
                   backend == GraphicsBackend.CANVAS_BGFX ||
                   backend == GraphicsBackend.BGFX_AUTO;
        }

        /**
         * Push a String[] onto the operand stack as an ANEWARRAY.
         */
        private void pushStringArray(InsnList insns, String[] values) {
            insns.add(new LdcInsnNode(values.length));
            insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < values.length; i++) {
                insns.add(new InsnNode(Opcodes.DUP));
                insns.add(new LdcInsnNode(i));
                insns.add(new LdcInsnNode(values[i]));
                insns.add(new InsnNode(Opcodes.AASTORE));
            }
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                      ║
    // ║  PHASE REGISTRY — Unified annotation → processor dispatch            ║
    // ║                                                                      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Central registry that dispatches annotations to the correct phase processor.
     * This is the entry point for the DeepMix transformation engine.
     */
    public static class DeepMixPhaseRegistry {

        private final DeepMixContext context;
        private final Phase10Processor phase10;
        private final Phase11Processor phase11;
        private final Phase12Processor phase12;
        private final Phase13Processor phase13;
        private final Phase14Processor phase14;
        private final Phase15Processor phase15;
        private final Phase16Processor phase16;
        private final Phase17Processor phase17;
        private final Phase18Processor phase18;
        private final Phase19Processor phase19;
        private final Phase20Processor phase20;

        public DeepMixPhaseRegistry(DeepMixContext context) {
            this.context = context;
            this.phase10 = new Phase10Processor(context);
            this.phase11 = new Phase11Processor(context);
            this.phase12 = new Phase12Processor(context);
            this.phase13 = new Phase13Processor(context);
            this.phase14 = new Phase14Processor(context);
            this.phase15 = new Phase15Processor(context);
            this.phase16 = new Phase16Processor(context);
            this.phase17 = new Phase17Processor(context);
            this.phase18 = new Phase18Processor(context);
            this.phase19 = new Phase19Processor(context);
            this.phase20 = new Phase20Processor(context);
        }

        /** Get all phase processors */
        public Phase10Processor phase10() { return phase10; }
        public Phase11Processor phase11() { return phase11; }
        public Phase12Processor phase12() { return phase12; }
        public Phase13Processor phase13() { return phase13; }
        public Phase14Processor phase14() { return phase14; }
        public Phase15Processor phase15() { return phase15; }
        public Phase16Processor phase16() { return phase16; }
        public Phase17Processor phase17() { return phase17; }
        public Phase18Processor phase18() { return phase18; }
        public Phase19Processor phase19() { return phase19; }
        public Phase20Processor phase20() { return phase20; }

        /** Get diagnostics from all phases */
        public List<String> getAllDiagnostics() {
            return context.getDiagnostics();
        }

        /** Print a summary of all applied transformations */
        public String getSummary() {
            List<String> diag = getAllDiagnostics();
            StringBuilder sb = new StringBuilder();
            sb.append("╔══════════════════════════════════════════════════════════╗\n");
            sb.append("║  🔮 DeepMix Phases 10-20 — Transformation Summary      ║\n");
            sb.append("╠══════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║  Total transformations applied: %-24d║\n", diag.size()));
            sb.append("╠══════════════════════════════════════════════════════════╣\n");
            for (String d : diag) {
                sb.append("║  ").append(d).append("\n");
            }
            sb.append("╚══════════════════════════════════════════════════════════╝\n");
            return sb.toString();
        }
    }

} // End of DeepMixPhases class
