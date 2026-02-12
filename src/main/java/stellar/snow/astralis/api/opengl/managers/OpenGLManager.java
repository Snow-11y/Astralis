package stellar.snow.astralis.api.opengl.managers;

import stellar.snow.astralis.Config;
import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;
import stellar.snow.astralis.api.opengl.mapping.GLSLCallMapper;
import stellar.snow.astralis.api.opengl.pipeline.OpenGLPipelineProvider;
import stellar.snow.astralis.api.opengl.pipeline.GLSLPipelineProvider;

import stellar.snow.astralis.api.opengl.buffer.GLBufferOps10;
import stellar.snow.astralis.api.opengl.buffer.GLBufferOps11;
import stellar.snow.astralis.api.opengl.buffer.GLBufferOps12;
import stellar.snow.astralis.api.opengl.buffer.GLBufferOps121;
import stellar.snow.astralis.api.opengl.buffer.GLBufferOps15;
import stellar.snow.astralis.api.opengl.buffer.GLBufferOps20;
import stellar.snow.astralis.api.opengl.buffer.GLBufferOps21;
import stellar.snow.astralis.api.opengl.buffer.GLBufferOps30;
import stellar.snow.astralis.api.opengl.buffer.GLBufferOps33;
import stellar.snow.astralis.api.opengl.buffer.GLBufferOps40;
import stellar.snow.astralis.api.opengl.buffer.GLBufferOps42;
import stellar.snow.astralis.api.opengl.buffer.GLBufferOps43;
import stellar.snow.astralis.api.opengl.buffer.GLBufferOps44;
import stellar.snow.astralis.api.opengl.buffer.GLBufferOps45;
import stellar.snow.astralis.api.opengl.buffer.GLBufferOps46;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

// Mixin imports for inner mixin classes
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;/**
 * OpenGLManager
 *
 * The ultra-fast central dispatcher:
 * - Receives GL calls from the game (MC 1.12.2 or any caller)
 * - Chooses the *single* best pipeline at init time
 * - Routes calls to the chosen pipeline with near-zero overhead
 * - Uses OpenGLCallMapper as infrastructure (LWJGL2/3 separation, extensions, translation)
 *
 * HARD RULES IMPLEMENTED:
 * - No per-call GL version branching (only init-time selection)
 * - No per-call allocation
 * - No "emulation" of missing features at manager level
 *   (manager either routes to pipeline/mapper, or throws if strict)
 *
 * IMPORTANT:
 * - This manager assumes the game runs GL on a single render thread.
 * - If vanilla/other mods call GL directly (bypassing manager), call invalidateState()
 *   before your rendering pass to avoid state desync.
 * 
 * CONTEXT LIMITATION:
 * - This manager is designed for single GL context usage (standard for Minecraft).
 * - If multiple GL contexts exist (rare, but possible with some launchers/mods),
 *   each context would need its own manager instance. Current design does not support this.
 */
public final class OpenGLManager {

    // ---------------------------------------------------------------------
    // Safe publication
    // ---------------------------------------------------------------------

    private static volatile OpenGLManager PUBLISHED;
    private static OpenGLManager FAST;

    public static boolean isInitialized() { return PUBLISHED != null; }

    /** Safe accessor (one volatile read). */
    public static OpenGLManager getSafe() {
        OpenGLManager m = PUBLISHED;
        if (m == null) throw new IllegalStateException("OpenGLManager not initialized. Call OpenGLManager.init() after GL context exists.");
        return m;
    }

    /** Fast accessor (no volatile read). Use ONLY after init(). */
    public static OpenGLManager getFast() { return FAST; }

    // ---------------------------------------------------------------------
    // LWJGL detection (manager-level only for info / diagnostics)
    // Mapper does the real separation.
    // ---------------------------------------------------------------------

    public static final int LWJGL_UNKNOWN = 0;
    public static final int LWJGL_2 = 2;
    public static final int LWJGL_3 = 3;

    public static final int LWJGL_VERSION;
    static {
        int v = LWJGL_UNKNOWN;
        try {
            Class.forName("org.lwjgl.Version");
            v = LWJGL_3;
        } catch (Throwable ignored) {
            try {
                Class.forName("org.lwjgl.Sys");
                v = LWJGL_2;
            } catch (Throwable ignored2) {
                v = LWJGL_UNKNOWN;
            }
        }
        LWJGL_VERSION = v;
    }

    // ---------------------------------------------------------------------
    // Minimal config snapshot (no references; init-time only)
    // Uses reflection-friendly getters to avoid hard dependency on exact Config API.
    // ---------------------------------------------------------------------

    private static final class Settings {
        final int maxGL;                 // max GL version to use (e.g. 33, 45, 46)
        final boolean debug;             // debug logging
        final boolean strictNoEmulation; // if true: unsupported advanced calls throw
        final boolean cacheBinds;        // bind caching (huge CPU win)
        final boolean cacheCommonState;  // enable/disable + depth/blend caching
        final boolean enableMultiDrawIndirect; // allow MultiDrawIndirect usage if supported
        final boolean enablePersistentMapping; // allow persistent map usage (pipelines may use)
        final boolean enableDSA;         // allow DSA usage (pipelines may use)
        final boolean throwOnInitFailure; // if true, init() throws instead of returning false

        Settings(Config cfg) {
            this.maxGL = getInt(cfg, "getMaxGLVersion", 46);
            this.debug = getBool(cfg, "getDebugMode", false);

            // Default to your rule: translation only, no emulation.
            this.strictNoEmulation = getBool(cfg, "getStrictNoEmulation", true);

            this.cacheBinds = getBool(cfg, "getCacheBinds", true);
            this.cacheCommonState = getBool(cfg, "getCacheCommonState", true);

            this.enableMultiDrawIndirect = getBool(cfg, "getUseMultiDrawIndirect", true);
            this.enablePersistentMapping = getBool(cfg, "getUsePersistentMapping", true);
            this.enableDSA = getBool(cfg, "getUseDSA", true);
            this.throwOnInitFailure = getBool(cfg, "getThrowOnInitFailure", false);
        }

        private static boolean getBool(Object o, String method, boolean def) {
            try {
                return (Boolean) o.getClass().getMethod(method).invoke(o);
            } catch (Throwable t) {
                return def;
            }
        }

        private static int getInt(Object o, String method, int def) {
            try {
                return (Integer) o.getClass().getMethod(method).invoke(o);
            } catch (Throwable t) {
                return def;
            }
        }
    }

    // ---------------------------------------------------------------------
    // GL constants (no LWJGL dependency)
    // ---------------------------------------------------------------------

    public static final int GL_VERSION = 0x1F02;

    // Buffer targets
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_COPY_READ_BUFFER = 0x8F36;
    public static final int GL_COPY_WRITE_BUFFER = 0x8F37;
    public static final int GL_PIXEL_PACK_BUFFER = 0x88EB;
    public static final int GL_PIXEL_UNPACK_BUFFER = 0x88EC;
    public static final int GL_UNIFORM_BUFFER = 0x8A11;
    public static final int GL_TEXTURE_BUFFER = 0x8C2A;
    public static final int GL_TRANSFORM_FEEDBACK_BUFFER = 0x8C8E;
    public static final int GL_DRAW_INDIRECT_BUFFER = 0x8F3F;
    public static final int GL_ATOMIC_COUNTER_BUFFER = 0x92C0;
    public static final int GL_DISPATCH_INDIRECT_BUFFER = 0x90EE;
    public static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;
    public static final int GL_QUERY_BUFFER = 0x9192;

    // Capabilities
    public static final int GL_DEPTH_TEST = 0x0B71;
    public static final int GL_BLEND = 0x0BE2;
    public static final int GL_CULL_FACE = 0x0B44;
    public static final int GL_SCISSOR_TEST = 0x0C11;
    public static final int GL_STENCIL_TEST = 0x0B90;
    public static final int GL_ALPHA_TEST = 0x0BC0;
    public static final int GL_POLYGON_OFFSET_FILL = 0x8037;
    public static final int GL_MULTISAMPLE = 0x809D;
    public static final int GL_SAMPLE_ALPHA_TO_COVERAGE = 0x809E;
    public static final int GL_LINE_SMOOTH = 0x0B20;
    public static final int GL_POLYGON_SMOOTH = 0x0B41;
    public static final int GL_TEXTURE_2D = 0x0DE1;

    // ---------------------------------------------------------------------
    // Expanded state cache (single-thread assumed)
    // ---------------------------------------------------------------------

    private static final class State {
        // Buffer bindings - using array for arbitrary target caching
        // Index by target ordinal (computed from target constant)
        private static final int BUFFER_TARGET_COUNT = 16;
        final int[] bufferBindings = new int[BUFFER_TARGET_COUNT];
        
        // Capability enable/disable bits
        long enableBits = 0L;

        // Depth state
        int depthFunc = -1;
        boolean depthMask = true;

        // Blend state
        int blendSrcRGB = -1, blendDstRGB = -1, blendSrcA = -1, blendDstA = -1;
        int blendEquationRGB = -1, blendEquationA = -1;

        // Texture bindings (most common units)
        int activeTexture = -1;
        final int[] texture2DBindings = new int[16]; // units 0-15

        // VAO binding (GL 3.0+)
        int boundVAO = -1;
        
        // Program binding
        int boundProgram = -1;
        
        // Framebuffer bindings
        int boundDrawFBO = -1;
        int boundReadFBO = -1;

        // Capability bit indices
        static final int BIT_DEPTH = 0;
        static final int BIT_BLEND = 1;
        static final int BIT_CULL  = 2;
        static final int BIT_SCISS = 3;
        static final int BIT_STEN  = 4;
        static final int BIT_ALPHA = 5;
        static final int BIT_POLY_OFFSET = 6;
        static final int BIT_MULTISAMPLE = 7;
        static final int BIT_SAMPLE_ALPHA = 8;
        static final int BIT_LINE_SMOOTH = 9;
        static final int BIT_POLY_SMOOTH = 10;
        static final int BIT_TEX2D = 11;

        State() {
            invalidate();
        }

        void invalidate() {
            for (int i = 0; i < BUFFER_TARGET_COUNT; i++) {
                bufferBindings[i] = -1;
            }
            enableBits = 0L;
            depthFunc = -1;
            depthMask = true;
            blendSrcRGB = blendDstRGB = blendSrcA = blendDstA = -1;
            blendEquationRGB = blendEquationA = -1;
            activeTexture = -1;
            for (int i = 0; i < texture2DBindings.length; i++) {
                texture2DBindings[i] = -1;
            }
            boundVAO = -1;
            boundProgram = -1;
            boundDrawFBO = -1;
            boundReadFBO = -1;
        }

        /**
         * Maps buffer target constant to cache index.
         * Returns -1 for unknown/uncached targets.
         */
        static int targetToIndex(int target) {
            switch (target) {
                case GL_ARRAY_BUFFER:              return 0;
                case GL_ELEMENT_ARRAY_BUFFER:      return 1;
                case GL_COPY_READ_BUFFER:          return 2;
                case GL_COPY_WRITE_BUFFER:         return 3;
                case GL_PIXEL_PACK_BUFFER:         return 4;
                case GL_PIXEL_UNPACK_BUFFER:       return 5;
                case GL_UNIFORM_BUFFER:            return 6;
                case GL_TEXTURE_BUFFER:            return 7;
                case GL_TRANSFORM_FEEDBACK_BUFFER: return 8;
                case GL_DRAW_INDIRECT_BUFFER:      return 9;
                case GL_ATOMIC_COUNTER_BUFFER:     return 10;
                case GL_DISPATCH_INDIRECT_BUFFER:  return 11;
                case GL_SHADER_STORAGE_BUFFER:     return 12;
                case GL_QUERY_BUFFER:              return 13;
                default:                           return -1;
            }
        }
    }

    // ---------------------------------------------------------------------
    // Buffer pipeline dispatch WITHOUT requiring pipeline classes to implement an interface.
    // Uses MethodHandles bound at init:
    // - No per-call reflection
    // - No per-call branching
    // - Monomorphic invokeExact on constant handles (JIT can optimize)
    // ---------------------------------------------------------------------

    private static final class BufferDispatch {
        final Object impl;

        final MethodHandle mhGenBuffer; // ()int
        final MethodHandle mhGenBuffers; // (int)int[] (optional batch)
        final MethodHandle mhDeleteBuffer; // (int)void
        final MethodHandle mhDeleteBuffers; // (int[])void (optional batch)
        final MethodHandle mhBindBuffer; // (int,int)void

        final MethodHandle mhBufferDataSize; // (int,long,int)void
        final MethodHandle mhBufferDataBB;   // (int,ByteBuffer,int)void
        final MethodHandle mhBufferDataFB;   // (int,FloatBuffer,int)void
        final MethodHandle mhBufferDataIB;   // (int,IntBuffer,int)void

        final MethodHandle mhBufferSubDataBB; // (int,long,ByteBuffer)void
        final MethodHandle mhBufferSubDataFB; // (int,long,FloatBuffer)void
        final MethodHandle mhBufferSubDataIB; // (int,long,IntBuffer)void

        final MethodHandle mhMapBuffer;       // either (int,int,long)ByteBuffer OR (int,int)ByteBuffer
        final boolean mapBufferHasLength;

        final MethodHandle mhMapBufferRange;  // (int,long,long,int)ByteBuffer (optional)
        final boolean hasMapBufferRange;

        final MethodHandle mhUnmapBuffer;     // either (int)boolean OR (int)void
        final boolean unmapReturnsBoolean;

        final MethodHandle mhFlushMappedRange; // (int,long,long)void (optional)
        final boolean hasFlushMappedRange;

        final MethodHandle mhCopyBufferSubData; // (int,int,long,long,long)void (optional)
        final boolean hasCopyBufferSubData;

        final MethodHandle mhInvalidateBufferData; // (int)void (optional, GL 4.3+)
        final boolean hasInvalidateBufferData;

        final MethodHandle mhInvalidateBufferSubData; // (int,long,long)void (optional, GL 4.3+)
        final boolean hasInvalidateBufferSubData;

        final MethodHandle mhGetBufferParameteri; // (int,int)int (optional)
        final boolean hasGetBufferParameteri;

        final MethodHandle mhShutdown; // ()void (optional)
        final boolean hasShutdown;

        final boolean hasGenBuffers;
        final boolean hasDeleteBuffers;

        BufferDispatch(Object impl, MethodHandles.Lookup lookup) {
            this.impl = impl;

            // Required core methods (must exist)
            mhGenBuffer = bindRequired(lookup, impl, "genBuffer", MethodType.methodType(int.class));
            mhDeleteBuffer = bindRequired(lookup, impl, "deleteBuffer", MethodType.methodType(void.class, int.class));
            mhBindBuffer = bindRequired(lookup, impl, "bindBuffer", MethodType.methodType(void.class, int.class, int.class));

            // Optional batch operations
            MethodHandle genBatch = bindOptional(lookup, impl, "genBuffers", MethodType.methodType(int[].class, int.class));
            mhGenBuffers = genBatch;
            hasGenBuffers = (genBatch != null);

            MethodHandle delBatch = bindOptional(lookup, impl, "deleteBuffers", MethodType.methodType(void.class, int[].class));
            mhDeleteBuffers = delBatch;
            hasDeleteBuffers = (delBatch != null);

            mhBufferDataSize = bindRequired(lookup, impl, "bufferData", MethodType.methodType(void.class, int.class, long.class, int.class));
            mhBufferDataBB = bindRequired(lookup, impl, "bufferData", MethodType.methodType(void.class, int.class, ByteBuffer.class, int.class));
            mhBufferDataFB = bindRequired(lookup, impl, "bufferData", MethodType.methodType(void.class, int.class, FloatBuffer.class, int.class));
            mhBufferDataIB = bindRequired(lookup, impl, "bufferData", MethodType.methodType(void.class, int.class, IntBuffer.class, int.class));

            mhBufferSubDataBB = bindRequired(lookup, impl, "bufferSubData", MethodType.methodType(void.class, int.class, long.class, ByteBuffer.class));
            mhBufferSubDataFB = bindRequired(lookup, impl, "bufferSubData", MethodType.methodType(void.class, int.class, long.class, FloatBuffer.class));
            mhBufferSubDataIB = bindRequired(lookup, impl, "bufferSubData", MethodType.methodType(void.class, int.class, long.class, IntBuffer.class));

            // mapBuffer: try (int,int,long)->ByteBuffer, else (int,int)->ByteBuffer
            MethodHandle mapA = bindOptional(lookup, impl, "mapBuffer",
                    MethodType.methodType(ByteBuffer.class, int.class, int.class, long.class));
            if (mapA != null) {
                mhMapBuffer = mapA;
                mapBufferHasLength = true;
            } else {
                MethodHandle mapB = bindOptional(lookup, impl, "mapBuffer",
                        MethodType.methodType(ByteBuffer.class, int.class, int.class));
                mhMapBuffer = requireNonNull(mapB, "mapBuffer");
                mapBufferHasLength = false;
            }

            // mapBufferRange optional (GL 3.0+)
            MethodHandle mbr = bindOptional(lookup, impl, "mapBufferRange",
                    MethodType.methodType(ByteBuffer.class, int.class, long.class, long.class, int.class));
            mhMapBufferRange = mbr;
            hasMapBufferRange = (mbr != null);

            // unmapBuffer: try boolean return, else void
            MethodHandle unA = bindOptional(lookup, impl, "unmapBuffer",
                    MethodType.methodType(boolean.class, int.class));
            if (unA != null) {
                mhUnmapBuffer = unA;
                unmapReturnsBoolean = true;
            } else {
                MethodHandle unB = bindOptional(lookup, impl, "unmapBuffer",
                        MethodType.methodType(void.class, int.class));
                mhUnmapBuffer = requireNonNull(unB, "unmapBuffer");
                unmapReturnsBoolean = false;
            }

            MethodHandle flush = bindOptional(lookup, impl, "flushMappedBufferRange",
                    MethodType.methodType(void.class, int.class, long.class, long.class));
            mhFlushMappedRange = flush;
            hasFlushMappedRange = (flush != null);

            MethodHandle copy = bindOptional(lookup, impl, "copyBufferSubData",
                    MethodType.methodType(void.class, int.class, int.class, long.class, long.class, long.class));
            mhCopyBufferSubData = copy;
            hasCopyBufferSubData = (copy != null);

            // GL 4.3+ invalidation
            MethodHandle invData = bindOptional(lookup, impl, "invalidateBufferData",
                    MethodType.methodType(void.class, int.class));
            mhInvalidateBufferData = invData;
            hasInvalidateBufferData = (invData != null);

            MethodHandle invSub = bindOptional(lookup, impl, "invalidateBufferSubData",
                    MethodType.methodType(void.class, int.class, long.class, long.class));
            mhInvalidateBufferSubData = invSub;
            hasInvalidateBufferSubData = (invSub != null);

            // Query
            MethodHandle getParam = bindOptional(lookup, impl, "getBufferParameteri",
                    MethodType.methodType(int.class, int.class, int.class));
            mhGetBufferParameteri = getParam;
            hasGetBufferParameteri = (getParam != null);

            MethodHandle shut = bindOptional(lookup, impl, "shutdown",
                    MethodType.methodType(void.class));
            mhShutdown = shut;
            hasShutdown = (shut != null);
        }

        int genBuffer() {
            try {
                return (int) mhGenBuffer.invokeExact();
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        int[] genBuffers(int count) {
            if (!hasGenBuffers) {
                // Fallback: generate one at a time
                int[] result = new int[count];
                for (int i = 0; i < count; i++) {
                    result[i] = genBuffer();
                }
                return result;
            }
            try {
                return (int[]) mhGenBuffers.invokeExact(count);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void deleteBuffer(int buffer) {
            try {
                mhDeleteBuffer.invokeExact(buffer);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void deleteBuffers(int[] buffers) {
            if (!hasDeleteBuffers) {
                // Fallback: delete one at a time
                for (int buffer : buffers) {
                    deleteBuffer(buffer);
                }
                return;
            }
            try {
                mhDeleteBuffers.invokeExact(buffers);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void bindBuffer(int target, int buffer) {
            try {
                mhBindBuffer.invokeExact(target, buffer);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void bufferData(int target, long size, int usage) {
            try {
                mhBufferDataSize.invokeExact(target, size, usage);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void bufferData(int target, ByteBuffer data, int usage) {
            try {
                mhBufferDataBB.invokeExact(target, data, usage);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void bufferData(int target, FloatBuffer data, int usage) {
            try {
                mhBufferDataFB.invokeExact(target, data, usage);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void bufferData(int target, IntBuffer data, int usage) {
            try {
                mhBufferDataIB.invokeExact(target, data, usage);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void bufferSubData(int target, long offset, ByteBuffer data) {
            try {
                mhBufferSubDataBB.invokeExact(target, offset, data);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void bufferSubData(int target, long offset, FloatBuffer data) {
            try {
                mhBufferSubDataFB.invokeExact(target, offset, data);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void bufferSubData(int target, long offset, IntBuffer data) {
            try {
                mhBufferSubDataIB.invokeExact(target, offset, data);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        ByteBuffer mapBuffer(int target, int access, long lengthIfNeeded) {
            try {
                if (mapBufferHasLength) {
                    return (ByteBuffer) mhMapBuffer.invokeExact(target, access, lengthIfNeeded);
                }
                return (ByteBuffer) mhMapBuffer.invokeExact(target, access);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        ByteBuffer mapBufferRange(int target, long offset, long length, int accessFlags) {
            if (!hasMapBufferRange) return null;
            try {
                return (ByteBuffer) mhMapBufferRange.invokeExact(target, offset, length, accessFlags);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        boolean unmapBuffer(int target) {
            try {
                if (unmapReturnsBoolean) {
                    return (boolean) mhUnmapBuffer.invokeExact(target);
                }
                mhUnmapBuffer.invokeExact(target);
                return true;
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void flushMappedBufferRange(int target, long offset, long length) {
            if (!hasFlushMappedRange) return;
            try {
                mhFlushMappedRange.invokeExact(target, offset, length);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void copyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
            if (!hasCopyBufferSubData) return;
            try {
                mhCopyBufferSubData.invokeExact(readTarget, writeTarget, readOffset, writeOffset, size);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void invalidateBufferData(int buffer) {
            if (!hasInvalidateBufferData) return;
            try {
                mhInvalidateBufferData.invokeExact(buffer);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void invalidateBufferSubData(int buffer, long offset, long length) {
            if (!hasInvalidateBufferSubData) return;
            try {
                mhInvalidateBufferSubData.invokeExact(buffer, offset, length);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        int getBufferParameteri(int target, int pname) {
            if (!hasGetBufferParameteri) return 0;
            try {
                return (int) mhGetBufferParameteri.invokeExact(target, pname);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        void shutdown() {
            if (!hasShutdown) return;
            try {
                mhShutdown.invokeExact();
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        private static MethodHandle bindRequired(MethodHandles.Lookup lookup, Object impl, String name, MethodType type) {
            MethodHandle mh = bindOptional(lookup, impl, name, type);
            return requireNonNull(mh, name + type);
        }

        private static MethodHandle bindOptional(MethodHandles.Lookup lookup, Object impl, String name, MethodType type) {
            try {
                MethodHandle mh = MethodHandles.publicLookup()
                        .findVirtual(impl.getClass(), name, type)
                        .bindTo(impl);
                return mh;
            } catch (Throwable t) {
                return null;
            }
        }

        private static MethodHandle requireNonNull(MethodHandle mh, String what) {
            if (mh == null) {
                throw new IllegalStateException("Pipeline missing required method: " + what);
            }
            return mh;
        }

        private static RuntimeException rethrow(Throwable t) {
            if (t instanceof RuntimeException) return (RuntimeException) t;
            if (t instanceof Error) throw (Error) t;
            return new RuntimeException(t);
        }
    }

    // ---------------------------------------------------------------------
    // Instance fields (ALL final for JIT friendliness)
    // ---------------------------------------------------------------------

    private final Settings settings;
    private final Thread renderThread;
    private final boolean debug;

    private final int detectedGL;       // e.g., 46, 45, 33, 121
    private final int effectiveGL;      // limited by config max
    private final int bufferOpsVersion; // one of the allowed set

    private final BufferDispatch buffer;
    private final State state;
    
    // Pipeline providers for GLSL and GL calls
    private final GLSLPipelineProvider glslPipeline;
    private final OpenGLPipelineProvider glPipeline;
    
    // Mappers for call routing
    private final GLSLCallMapper glslMapper;
    private final OpenGLCallMapper glMapper;

    // cached capability flags (read once, used for strict checks)
    private final boolean hasMultiDrawIndirect;   // GL 4.3+ / extension
    private final boolean hasPersistentMapping;   // GL 4.4+ / extension
    private final boolean hasDSA;                 // GL 4.5+ / extension

    // Raw version string for diagnostics
    private final String rawVersionString;

    private OpenGLManager(Settings s, int detectedGL, int effectiveGL, int opsVer,
                          BufferDispatch buffer, State state,
                          boolean hasMDI, boolean hasPM, boolean hasDSA,
                          Thread renderThread, String rawVersionString) {
        this.settings = s;
        this.renderThread = renderThread;
        this.debug = s.debug;

        this.detectedGL = detectedGL;
        this.effectiveGL = effectiveGL;
        this.bufferOpsVersion = opsVer;

        this.buffer = buffer;
        this.state = state;
        
        // Initialize pipeline providers
        this.glslPipeline = GLSLPipelineProvider.getInstance();
        this.glPipeline = OpenGLPipelineProvider.getInstance();
        
        // Initialize mappers as backend (NOT MC render backend)
        this.glslMapper = GLSLCallMapper.getInstance();
        this.glMapper = OpenGLCallMapper.getInstance();

        this.hasMultiDrawIndirect = hasMDI;
        this.hasPersistentMapping = hasPM;
        this.hasDSA = hasDSA;

        this.rawVersionString = rawVersionString;
    }

    // ---------------------------------------------------------------------
    // init()
    // ---------------------------------------------------------------------

    /**
     * Initialize (must be called after GL context exists).
     * Creates exactly one buffer pipeline instance:
     * GLBufferOps10/11/12/121/15/20/21/30/33/40/42/43/44/45/46
     * 
     * @return true if initialization succeeded, false otherwise
     * @throws RuntimeException if throwOnInitFailure config is true and init fails
     */
    public static boolean init() {
        if (PUBLISHED != null) return true;

        synchronized (OpenGLManager.class) {
            if (PUBLISHED != null) return true;

            Settings s = null;
            try {
                OpenGLCallMapper.initialize();

                Config cfg = Config.getInstance();
                s = new Settings(cfg);

                String ver = OpenGLCallMapper.getString(GL_VERSION);
                int detected = parseGLVersionCode(ver);
                int effective = clampToMax(detected, s.maxGL);
                int opsVer = chooseBufferOpsVersion(effective);

                Object opsImpl = instantiateBufferOps(opsVer);

                // Detect important advanced capabilities from mapper (infra)
                // If your OpenGLCallMapper doesn't expose these, keep them conservative.
                boolean hasMDI = safeHas(OpenGLCallMapper.class, "hasFeatureMultiDrawIndirect");
                boolean hasPM = safeHas(OpenGLCallMapper.class, "hasFeaturePersistentMapping");
                boolean hasDSA = safeHas(OpenGLCallMapper.class, "hasFeatureDSA");

                BufferDispatch dispatch = new BufferDispatch(opsImpl, MethodHandles.lookup());
                State st = new State();
                st.invalidate();

                Thread rt = Thread.currentThread();

                OpenGLManager mgr = new OpenGLManager(s, detected, effective, opsVer, dispatch, st, hasMDI, hasPM, hasDSA, rt, ver);

                PUBLISHED = mgr;
                FAST = mgr;

                if (s.debug) {
                    System.out.println("[OpenGLManager] init ok" +
                            " LWJGL=" + LWJGL_VERSION +
                            " GL=\"" + ver + "\"" +
                            " detected=" + detected +
                            " effective=" + effective +
                            " bufferOps=" + opsVer +
                            " MDI=" + hasMDI +
                            " PM=" + hasPM +
                            " DSA=" + hasDSA);
                }

                return true;
            } catch (Throwable t) {
                System.err.println("[OpenGLManager] init failed: " + t.getMessage());
                t.printStackTrace();
                if (s != null && s.throwOnInitFailure) {
                    throw new RuntimeException("OpenGLManager initialization failed", t);
                }
                return false;
            }
        }
    }

    /**
     * Initialize with a callback for custom error handling.
     * 
     * @param errorCallback called if initialization fails, receives the exception
     * @return true if initialization succeeded
     */
    public static boolean init(java.util.function.Consumer<Throwable> errorCallback) {
        if (PUBLISHED != null) return true;

        synchronized (OpenGLManager.class) {
            if (PUBLISHED != null) return true;

            try {
                return init();
            } catch (Throwable t) {
                if (errorCallback != null) {
                    errorCallback.accept(t);
                }
                return false;
            }
        }
    }

    // ---------------------------------------------------------------------
    // Buffer pipeline selection (ONLY YOUR OPS)
    // ---------------------------------------------------------------------

    private static Object instantiateBufferOps(int version) {
        switch (version) {
            case 10:  return new GLBufferOps10();
            case 11:  return new GLBufferOps11();
            case 12:  return new GLBufferOps12();
            case 121: return new GLBufferOps121();
            case 15:  return new GLBufferOps15();
            case 20:  return new GLBufferOps20();
            case 21:  return new GLBufferOps21();
            case 30:  return new GLBufferOps30();
            case 33:  return new GLBufferOps33();
            case 40:  return new GLBufferOps40();
            case 42:  return new GLBufferOps42();
            case 43:  return new GLBufferOps43();
            case 44:  return new GLBufferOps44();
            case 45:  return new GLBufferOps45();
            case 46:  return new GLBufferOps46();
            default:  return new GLBufferOps10();
        }
    }

    private static int chooseBufferOpsVersion(int gl) {
        // The only allowed pipelines:
        // 10,11,12,121,15,20,21,30,33,40,42,43,44,45,46
        if (gl == 121) return 121;
        if (gl <= 10) return 10;
        if (gl == 11) return 11;
        if (gl == 12) return 12;

        // 1.3/1.4 not listed -> use 1.2.1 pipeline
        if (gl == 13 || gl == 14) return 121;

        if (gl == 15) return 15;

        if (gl == 20) return 20;
        if (gl == 21) return 21;

        // 3.0-3.2 -> 30 pipeline
        if (gl == 30 || gl == 31 || gl == 32) return 30;
        if (gl == 33) return 33;

        // 4.0-4.1 -> 40 pipeline
        if (gl == 40 || gl == 41) return 40;
        if (gl == 42) return 42;
        if (gl == 43) return 43;
        if (gl == 44) return 44;
        if (gl == 45) return 45;
        if (gl >= 46) return 46;

        return 10;
    }

    // ---------------------------------------------------------------------
    // Robust GL version parsing
    // Handles:
    //   "3.3.0 NVIDIA 545.29.06"
    //   "4.6.0 Compatibility Profile Context 23.11.1"
    //   "OpenGL ES 3.2 Mesa 24.0.0"
    //   "4.6"
    //   "1.2.1"
    // ---------------------------------------------------------------------

    private static int parseGLVersionCode(String versionString) {
        if (versionString == null || versionString.isEmpty()) return 11;

        // Trim and work on the string
        String s = versionString.trim();

        // Skip "OpenGL " or "OpenGL ES " prefix if present
        if (s.startsWith("OpenGL ES ")) {
            s = s.substring(10);
        } else if (s.startsWith("OpenGL ")) {
            s = s.substring(7);
        }

        // Now extract major.minor[.patch] from the beginning
        int major = 1, minor = 1, patch = 0;
        int len = s.length();
        int i = 0;

        // Parse major
        int majorStart = i;
        while (i < len && isDigit(s.charAt(i))) i++;
        if (i > majorStart) {
            major = parseIntFast(s, majorStart, i);
        } else {
            return 11; // No digits found
        }

        // Expect '.'
        if (i < len && s.charAt(i) == '.') {
            i++;
            // Parse minor
            int minorStart = i;
            while (i < len && isDigit(s.charAt(i))) i++;
            if (i > minorStart) {
                minor = parseIntFast(s, minorStart, i);
            }

            // Check for second '.' (patch version)
            if (i < len && s.charAt(i) == '.') {
                i++;
                int patchStart = i;
                while (i < len && isDigit(s.charAt(i))) i++;
                if (i > patchStart) {
                    patch = parseIntFast(s, patchStart, i);
                }
            }
        }

        // Special case: GL 1.2.1
        if (major == 1 && minor == 2 && patch == 1) return 121;

        // Normal encoding: major*10 + minor
        return major * 10 + minor;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static int parseIntFast(String s, int start, int end) {
        int val = 0;
        for (int i = start; i < end; i++) {
            val = val * 10 + (s.charAt(i) - '0');
        }
        return val;
    }

    private static int clampToMax(int detected, int max) {
        // Interpret 1.2.1 as 1.2 for comparison against user max,
        // but preserve 121 if it is the detected version and allowed.
        int d = (detected == 121) ? 12 : detected;
        int m = (max == 121) ? 12 : max;

        return (d <= m) ? detected : max;
    }

    // ---------------------------------------------------------------------
    // Capability detection from OpenGLCallMapper (best-effort).
    // If your mapper has different names, this safely returns false.
    // ---------------------------------------------------------------------

    private static boolean safeHas(Class<?> mapper, String method) {
        try {
            Object v = mapper.getMethod(method).invoke(null);
            return (v instanceof Boolean) && (Boolean) v;
        } catch (Throwable t) {
            return false;
        }
    }

    // ---------------------------------------------------------------------
    // Debug-only render-thread check (zero overhead when debug=false)
    // ---------------------------------------------------------------------

    private void assertRenderThread() {
        if (!debug) return;
        if (Thread.currentThread() != renderThread) {
            throw new IllegalStateException("OpenGLManager used from wrong thread. Expected=" +
                    renderThread.getName() + " got=" + Thread.currentThread().getName());
        }
    }

    // ---------------------------------------------------------------------
    // Public: state desync handling
    // ---------------------------------------------------------------------

    /**
     * If any code calls GL directly bypassing this manager, call invalidateState()
     * before you start your rendering pass.
     */
    public void invalidateState() {
        assertRenderThread();
        state.invalidate();
    }

    /**
     * Invalidate only buffer binding cache.
     * Use if external code modifies buffer bindings but not other state.
     */
    public void invalidateBufferBindings() {
        assertRenderThread();
        for (int i = 0; i < State.BUFFER_TARGET_COUNT; i++) {
            state.bufferBindings[i] = -1;
        }
    }

    /**
     * Invalidate only texture binding cache.
     */
    public void invalidateTextureBindings() {
        assertRenderThread();
        state.activeTexture = -1;
        for (int i = 0; i < state.texture2DBindings.length; i++) {
            state.texture2DBindings[i] = -1;
        }
    }

    // ---------------------------------------------------------------------
    // Public: Buffer routing (fastest hot path)
    // ---------------------------------------------------------------------

    public int genBuffer() {
        assertRenderThread();
        return buffer.genBuffer();
    }

    /**
     * Generate multiple buffers at once (batch operation).
     * More efficient than calling genBuffer() in a loop.
     */
    public int[] genBuffers(int count) {
        assertRenderThread();
        return buffer.genBuffers(count);
    }

    public void deleteBuffer(int id) {
        assertRenderThread();
        buffer.deleteBuffer(id);
        // Invalidate this buffer from any cached binding
        for (int i = 0; i < State.BUFFER_TARGET_COUNT; i++) {
            if (state.bufferBindings[i] == id) {
                state.bufferBindings[i] = -1;
            }
        }
    }

    /**
     * Delete multiple buffers at once (batch operation).
     */
    public void deleteBuffers(int[] buffers) {
        assertRenderThread();
        buffer.deleteBuffers(buffers);
        // Invalidate these buffers from cache
        for (int bufId : buffers) {
            for (int i = 0; i < State.BUFFER_TARGET_COUNT; i++) {
                if (state.bufferBindings[i] == bufId) {
                    state.bufferBindings[i] = -1;
                }
            }
        }
    }

    /**
     * Bind buffer with caching.
     * Buffer binding is heavily cached because it's one of the most frequent GL calls.
     */
    public void bindBuffer(int target, int id) {
        assertRenderThread();

        if (settings.cacheBinds) {
            int idx = State.targetToIndex(target);
            if (idx >= 0) {
                if (state.bufferBindings[idx] == id) return;
                state.bufferBindings[idx] = id;
            }
        }

        buffer.bindBuffer(target, id);
    }

    /**
     * Force bind buffer, bypassing cache.
     * Use when you know external code may have modified the binding.
     */
    public void bindBufferForce(int target, int id) {
        assertRenderThread();
        int idx = State.targetToIndex(target);
        if (idx >= 0) {
            state.bufferBindings[idx] = id;
        }
        buffer.bindBuffer(target, id);
    }

    public void bufferData(int target, long size, int usage) {
        assertRenderThread();
        buffer.bufferData(target, size, usage);
    }

    public void bufferData(int target, ByteBuffer data, int usage) {
        assertRenderThread();
        buffer.bufferData(target, data, usage);
    }

    public void bufferData(int target, FloatBuffer data, int usage) {
        assertRenderThread();
        buffer.bufferData(target, data, usage);
    }

    public void bufferData(int target, IntBuffer data, int usage) {
        assertRenderThread();
        buffer.bufferData(target, data, usage);
    }

    public void bufferSubData(int target, long offset, ByteBuffer data) {
        assertRenderThread();
        buffer.bufferSubData(target, offset, data);
    }

    public void bufferSubData(int target, long offset, FloatBuffer data) {
        assertRenderThread();
        buffer.bufferSubData(target, offset, data);
    }

    public void bufferSubData(int target, long offset, IntBuffer data) {
        assertRenderThread();
        buffer.bufferSubData(target, offset, data);
    }

    /**
     * Map a buffer for CPU access.
     * 
     * <p>The {@code lengthIfNeeded} parameter is used by some pipeline implementations
     * (particularly LWJGL3-based ones like GL 1.5+) that require knowing the buffer size
     * for mapping. Pre-1.5 pipelines and some LWJGL2 paths ignore this parameter.</p>
     * 
     * <p>If you don't know the buffer size, you can query it first with
     * {@link #getBufferParameteri(int, int)} using GL_BUFFER_SIZE, or pass 0
     * if your pipeline doesn't require it.</p>
     *
     * @param target the buffer target (e.g., GL_ARRAY_BUFFER)
     * @param access the access mode (e.g., GL_READ_ONLY, GL_WRITE_ONLY, GL_READ_WRITE)
     * @param lengthIfNeeded buffer size hint, may be ignored by some pipelines
     * @return a ByteBuffer view of the mapped memory, or null if mapping failed
     */
    public ByteBuffer mapBuffer(int target, int access, long lengthIfNeeded) {
        assertRenderThread();
        return buffer.mapBuffer(target, access, lengthIfNeeded);
    }

    /**
     * Map a range of a buffer for CPU access.
     * 
     * <p>This is the preferred mapping method for GL 3.0+ as it allows partial
     * mapping and more control over synchronization via access flags.</p>
     * 
     * <p>Returns null if the pipeline does not support mapBufferRange (pre-GL 3.0).
     * If {@code strictNoEmulation} is enabled, throws instead of returning null.</p>
     *
     * @param target the buffer target
     * @param offset byte offset into the buffer
     * @param length number of bytes to map
     * @param accessFlags bitfield of GL_MAP_READ_BIT, GL_MAP_WRITE_BIT, etc.
     * @return mapped ByteBuffer, or null if unsupported
     * @throws UnsupportedOperationException if strict mode and unsupported
     */
    public ByteBuffer mapBufferRange(int target, long offset, long length, int accessFlags) {
        assertRenderThread();
        ByteBuffer bb = buffer.mapBufferRange(target, offset, length, accessFlags);
        if (bb == null && settings.strictNoEmulation) {
            throw new UnsupportedOperationException("mapBufferRange not supported by pipeline GLBufferOps" + bufferOpsVersion);
        }
        return bb;
    }

    public boolean unmapBuffer(int target) {
        assertRenderThread();
        return buffer.unmapBuffer(target);
    }

    /**
     * Flush a portion of a mapped buffer range.
     * 
     * <p>Only valid after mapping with GL_MAP_FLUSH_EXPLICIT_BIT.
     * No-op if pipeline doesn't support it (pre-GL 3.0).
     * Throws in strict mode if unsupported.</p>
     */
    public void flushMappedBufferRange(int target, long offset, long length) {
        assertRenderThread();
        if (!buffer.hasFlushMappedRange && settings.strictNoEmulation) {
            throw new UnsupportedOperationException("flushMappedBufferRange not supported by pipeline GLBufferOps" + bufferOpsVersion);
        }
        buffer.flushMappedBufferRange(target, offset, length);
    }

    /**
     * Copy data between buffers.
     * 
     * <p>Requires GL 3.1+. No-op if unsupported unless strict mode.</p>
     */
    public void copyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        assertRenderThread();
        if (!buffer.hasCopyBufferSubData && settings.strictNoEmulation) {
            throw new UnsupportedOperationException("copyBufferSubData not supported by pipeline GLBufferOps" + bufferOpsVersion);
        }
        buffer.copyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }

    /**
     * Invalidate entire buffer contents (GL 4.3+).
     * Signals to the driver that previous contents are no longer needed.
     */
    public void invalidateBufferData(int buffer) {
        assertRenderThread();
        if (!this.buffer.hasInvalidateBufferData) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("invalidateBufferData not supported by pipeline GLBufferOps" + bufferOpsVersion);
            }
            return;
        }
        this.buffer.invalidateBufferData(buffer);
    }

    /**
     * Invalidate a range of buffer contents (GL 4.3+).
     */
    public void invalidateBufferSubData(int buffer, long offset, long length) {
        assertRenderThread();
        if (!this.buffer.hasInvalidateBufferSubData) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("invalidateBufferSubData not supported by pipeline GLBufferOps" + bufferOpsVersion);
            }
            return;
        }
        this.buffer.invalidateBufferSubData(buffer, offset, length);
    }

    /**
     * Query buffer parameter.
     * Common pnames: GL_BUFFER_SIZE (0x8764), GL_BUFFER_USAGE (0x8765), GL_BUFFER_MAPPED (0x88BC)
     */
    public int getBufferParameteri(int target, int pname) {
        assertRenderThread();
        return buffer.getBufferParameteri(target, pname);
    }

    // ---------------------------------------------------------------------
    // Public: extreme-perf features that the manager can route (no emulation)
    // (These are optional entrypoints; pipelines + mapper do the heavy lifting.)
    // ---------------------------------------------------------------------

    /**
     * MultiDrawArraysIndirect (GL 4.3+):
     * Manager will NOT emulate by looping unless you explicitly disable strictNoEmulation.
     */
    public void multiDrawArraysIndirect(int mode, long indirect, int drawCount, int stride) {
        assertRenderThread();
        if (!settings.enableMultiDrawIndirect || !hasMultiDrawIndirect) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("MultiDrawArraysIndirect not supported/enabled");
            }
            return;
        }
        OpenGLCallMapper.multiDrawArraysIndirect(mode, indirect, drawCount, stride);
    }

    /**
     * MultiDrawElementsIndirect (GL 4.3+):
     * No emulation here.
     */
    public void multiDrawElementsIndirect(int mode, int type, long indirect, int drawCount, int stride) {
        assertRenderThread();
        if (!settings.enableMultiDrawIndirect || !hasMultiDrawIndirect) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("MultiDrawElementsIndirect not supported/enabled");
            }
            return;
        }
        OpenGLCallMapper.multiDrawElementsIndirect(mode, type, indirect, drawCount, stride);
    }

    // ---------------------------------------------------------------------
    // Public: common state caching (tiny, fast, big win)
    // ---------------------------------------------------------------------

    public void enable(int cap) {
        assertRenderThread();
        if (!settings.cacheCommonState) {
            OpenGLCallMapper.enable(cap);
            return;
        }
        int bit = capToBit(cap);
        if (bit >= 0) {
            long mask = 1L << bit;
            if ((state.enableBits & mask) != 0L) return;
            state.enableBits |= mask;
        }
        OpenGLCallMapper.enable(cap);
    }

    public void disable(int cap) {
        assertRenderThread();
        if (!settings.cacheCommonState) {
            OpenGLCallMapper.disable(cap);
            return;
        }
        int bit = capToBit(cap);
        if (bit >= 0) {
            long mask = 1L << bit;
            if ((state.enableBits & mask) == 0L) return;
            state.enableBits &= ~mask;
        }
        OpenGLCallMapper.disable(cap);
    }

    /**
     * Check if a capability is currently enabled (cached).
     * Returns accurate value only if caching is enabled and state hasn't been
     * modified externally. Returns false for unknown capabilities.
     */
    public boolean isEnabled(int cap) {
        if (!settings.cacheCommonState) {
            return OpenGLCallMapper.isEnabled(cap);
        }
        int bit = capToBit(cap);
        if (bit < 0) {
            return OpenGLCallMapper.isEnabled(cap);
        }
        return (state.enableBits & (1L << bit)) != 0L;
    }

    private static int capToBit(int cap) {
        switch (cap) {
            case GL_DEPTH_TEST:              return State.BIT_DEPTH;
            case GL_BLEND:                   return State.BIT_BLEND;
            case GL_CULL_FACE:               return State.BIT_CULL;
            case GL_SCISSOR_TEST:            return State.BIT_SCISS;
            case GL_STENCIL_TEST:            return State.BIT_STEN;
            case GL_ALPHA_TEST:              return State.BIT_ALPHA;
            case GL_POLYGON_OFFSET_FILL:     return State.BIT_POLY_OFFSET;
            case GL_MULTISAMPLE:             return State.BIT_MULTISAMPLE;
            case GL_SAMPLE_ALPHA_TO_COVERAGE:return State.BIT_SAMPLE_ALPHA;
            case GL_LINE_SMOOTH:             return State.BIT_LINE_SMOOTH;
            case GL_POLYGON_SMOOTH:          return State.BIT_POLY_SMOOTH;
            case GL_TEXTURE_2D:              return State.BIT_TEX2D;
            default:                         return -1;
        }
    }

    public void depthFunc(int func) {
        assertRenderThread();
        if (settings.cacheCommonState && state.depthFunc == func) return;
        state.depthFunc = func;
        OpenGLCallMapper.depthFunc(func);
    }

    public void depthMask(boolean flag) {
        assertRenderThread();
        if (settings.cacheCommonState && state.depthMask == flag) return;
        state.depthMask = flag;
        OpenGLCallMapper.depthMask(flag);
    }

    public void blendFunc(int sfactor, int dfactor) {
        assertRenderThread();
        if (settings.cacheCommonState &&
                state.blendSrcRGB == sfactor && state.blendDstRGB == dfactor &&
                state.blendSrcA == sfactor && state.blendDstA == dfactor) {
            return;
        }
        state.blendSrcRGB = sfactor;
        state.blendDstRGB = dfactor;
        state.blendSrcA = sfactor;
        state.blendDstA = dfactor;
        OpenGLCallMapper.blendFunc(sfactor, dfactor);
    }

    public void blendFuncSeparate(int srcRGB, int dstRGB, int srcA, int dstA) {
        assertRenderThread();
        if (settings.cacheCommonState &&
                state.blendSrcRGB == srcRGB && state.blendDstRGB == dstRGB &&
                state.blendSrcA == srcA && state.blendDstA == dstA) {
            return;
        }
        state.blendSrcRGB = srcRGB;
        state.blendDstRGB = dstRGB;
        state.blendSrcA = srcA;
        state.blendDstA = dstA;
        OpenGLCallMapper.blendFuncSeparate(srcRGB, dstRGB, srcA, dstA);
    }

    public void blendEquation(int mode) {
        assertRenderThread();
        if (settings.cacheCommonState && 
                state.blendEquationRGB == mode && state.blendEquationA == mode) {
            return;
        }
        state.blendEquationRGB = mode;
        state.blendEquationA = mode;
        OpenGLCallMapper.blendEquation(mode);
    }

    public void blendEquationSeparate(int modeRGB, int modeAlpha) {
        assertRenderThread();
        if (settings.cacheCommonState &&
                state.blendEquationRGB == modeRGB && state.blendEquationA == modeAlpha) {
            return;
        }
        state.blendEquationRGB = modeRGB;
        state.blendEquationA = modeAlpha;
        OpenGLCallMapper.blendEquationSeparate(modeRGB, modeAlpha);
    }

    // ---------------------------------------------------------------------
    // Public: VAO binding (GL 3.0+)
    // ---------------------------------------------------------------------

    public void bindVertexArray(int vao) {
        assertRenderThread();
        if (settings.cacheBinds && state.boundVAO == vao) return;
        state.boundVAO = vao;
        OpenGLCallMapper.bindVertexArray(vao);
    }

    // ---------------------------------------------------------------------
    // Public: Program binding
    // ---------------------------------------------------------------------

    public void useProgram(int program) {
        assertRenderThread();
        if (settings.cacheBinds && state.boundProgram == program) return;
        state.boundProgram = program;
        OpenGLCallMapper.useProgram(program);
    }

    // ---------------------------------------------------------------------
    // Public: Framebuffer binding
    // ---------------------------------------------------------------------

    public void bindFramebuffer(int target, int framebuffer) {
        assertRenderThread();
        // 0x8CA8 = GL_READ_FRAMEBUFFER, 0x8CA9 = GL_DRAW_FRAMEBUFFER, 0x8D40 = GL_FRAMEBUFFER
        if (settings.cacheBinds) {
            if (target == 0x8D40) { // GL_FRAMEBUFFER binds both
                if (state.boundDrawFBO == framebuffer && state.boundReadFBO == framebuffer) return;
                state.boundDrawFBO = framebuffer;
                state.boundReadFBO = framebuffer;
            } else if (target == 0x8CA9) { // GL_DRAW_FRAMEBUFFER
                if (state.boundDrawFBO == framebuffer) return;
                state.boundDrawFBO = framebuffer;
            } else if (target == 0x8CA8) { // GL_READ_FRAMEBUFFER
                if (state.boundReadFBO == framebuffer) return;
                state.boundReadFBO = framebuffer;
            }
        }
        OpenGLCallMapper.bindFramebuffer(target, framebuffer);
    }

    // ---------------------------------------------------------------------
    // Public: Active texture unit
    // ---------------------------------------------------------------------

    public void activeTexture(int texture) {
        assertRenderThread();
        if (settings.cacheBinds && state.activeTexture == texture) return;
        state.activeTexture = texture;
        OpenGLCallMapper.activeTexture(texture);
    }

    public void bindTexture(int target, int texture) {
        assertRenderThread();
        // Only cache GL_TEXTURE_2D for the first 16 units
        if (settings.cacheBinds && target == GL_TEXTURE_2D) {
            int unit = state.activeTexture - 0x84C0; // GL_TEXTURE0 = 0x84C0
            if (unit >= 0 && unit < state.texture2DBindings.length) {
                if (state.texture2DBindings[unit] == texture) return;
                state.texture2DBindings[unit] = texture;
            }
        }
        OpenGLCallMapper.bindTexture(target, texture);
    }

    // ---------------------------------------------------------------------
    // Lightweight diagnostics / getters
    // ---------------------------------------------------------------------

    public int getDetectedGLVersion() { return detectedGL; }
    public int getEffectiveGLVersion() { return effectiveGL; }
    public int getBufferOpsVersion() { return bufferOpsVersion; }
    public String getRawVersionString() { return rawVersionString; }

    public boolean isDSAAvailable() { return hasDSA; }
    public boolean isPersistentMappingAvailable() { return hasPersistentMapping; }
    public boolean isMultiDrawIndirectAvailable() { return hasMultiDrawIndirect; }

    public boolean isCacheBindsEnabled() { return settings.cacheBinds; }
    public boolean isCacheCommonStateEnabled() { return settings.cacheCommonState; }
    public boolean isStrictNoEmulation() { return settings.strictNoEmulation; }
    public boolean isDebugEnabled() { return debug; }

    /**
     * Get a human-readable description of the current configuration.
     */
    public String getConfigSummary() {
        return String.format(
            "OpenGLManager[LWJGL=%d, GL=%s, detected=%d, effective=%d, ops=%d, " +
            "MDI=%b, PM=%b, DSA=%b, cacheBinds=%b, cacheState=%b, strict=%b]",
            LWJGL_VERSION, rawVersionString, detectedGL, effectiveGL, bufferOpsVersion,
            hasMultiDrawIndirect, hasPersistentMapping, hasDSA,
            settings.cacheBinds, settings.cacheCommonState, settings.strictNoEmulation
        );
    }

    // ---------------------------------------------------------------------
    // Pipeline and Mapper Access (for game integration via mixins)
    // ---------------------------------------------------------------------
    
    /**
     * Get the GLSL pipeline provider for shader operations.
     * This is used by mixins to route GLSL calls through the pipeline to the game.
     */
    public GLSLPipelineProvider getGLSLPipeline() {
        return glslPipeline;
    }
    
    /**
     * Get the OpenGL pipeline provider for GL operations.
     * This is used by mixins to route GL calls through the pipeline to the game.
     */
    public OpenGLPipelineProvider getGLPipeline() {
        return glPipeline;
    }
    
    /**
     * Get the GLSL call mapper (backend, not MC render backend).
     * This provides low-level GLSL translation capabilities.
     */
    public GLSLCallMapper getGLSLMapper() {
        return glslMapper;
    }
    
    /**
     * Get the OpenGL call mapper (backend, not MC render backend).
     * This provides low-level GL call translation and routing.
     */
    public OpenGLCallMapper getGLMapper() {
        return glMapper;
    }

    // ---------------------------------------------------------------------
    // Shutdown: no leaks (pipelines + mapper caches)
    // 
    // NOTE: This assumes single GL context usage. If multiple contexts exist
    // (rare in MC), calling shutdown() will affect the global state. Each
    // context would require its own manager instance, which is not currently
    // supported.
    // ---------------------------------------------------------------------

    public void shutdown() {
        assertRenderThread();

        // Ask pipeline to cleanup if it owns any internal resources (should be lightweight).
        try {
            buffer.shutdown();
        } catch (Throwable ignored) {}
        
        // Cleanup GLSL pipeline
        try {
            if (glslPipeline != null) {
                glslPipeline.shutdown();
            }
        } catch (Throwable ignored) {}
        
        // Cleanup GL pipeline
        try {
            if (glPipeline != null) {
                glPipeline.shutdown();
            }
        } catch (Throwable ignored) {}

        // Clear mapper caches (shader/program caches, etc.) if present.
        try {
            OpenGLCallMapper.clearAllCaches();
        } catch (Throwable ignored) {}
        
        // Clear GLSL mapper caches
        try {
            if (glslMapper != null) {
                glslMapper.clearAllCaches();
            }
        } catch (Throwable ignored) {}

        state.invalidate();

        synchronized (OpenGLManager.class) {
            PUBLISHED = null;
            FAST = null;
        }
    }

    /**
     * Check if currently on the render thread.
     * Useful for external code that wants to verify thread safety.
     */
    public boolean isRenderThread() {
        return Thread.currentThread() == renderThread;
    }

    /**
     * Get the render thread this manager was initialized on.
     */
    public Thread getRenderThread() {
        return renderThread;
    }
    
    // =====================================================================
    // MIXINS - Game integration layer
    // These are embedded here to route calls through pipelines
    // =====================================================================
    
    /**
     * Mixin for EntityRenderer - hooks game rendering to route through GL pipeline.
     * 
     * Hooks into Minecraft's game renderer to ensure all GL calls go through our pipeline.
     */
    @Mixin(net.minecraft.client.renderer.EntityRenderer.class)
    public static class MixinGameRenderer {
        private static OpenGLManager glManager;
        private static OpenGLPipelineProvider glPipeline;
        
        /**
         * Hook into renderWorld to ensure our pipeline is active.
         */
        @Inject(method = "renderWorld", at = @At("HEAD"))
        private void onRenderWorldStart(float partialTicks, long finishTimeNano, CallbackInfo ci) {
            if (!OpenGLManager.isInitialized()) return;
            
            if (glManager == null) {
                glManager = OpenGLManager.getSafe();
                glPipeline = glManager.getGLPipeline();
            }
            
            glManager.invalidateState();
            glPipeline.beginFrame();
        }
        
        /**
         * Hook into renderWorld end to finalize pipeline state.
         */
        @Inject(method = "renderWorld", at = @At("RETURN"))
        private void onRenderWorldEnd(float partialTicks, long finishTimeNano, CallbackInfo ci) {
            if (glPipeline != null) {
                glPipeline.endFrame();
            }
        }
        
        /**
         * Hook into updateCameraAndRender to ensure pipeline is ready.
         */
        @Inject(method = "updateCameraAndRender", at = @At("HEAD"))
        private void onUpdateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo ci) {
            if (!OpenGLManager.isInitialized()) return;
            
            if (glManager == null) {
                glManager = OpenGLManager.getSafe();
                glPipeline = glManager.getGLPipeline();
            }
        }
    }
    
    /**
     * Mixin for ShaderManager - hooks GLSL operations to route through GLSL pipeline.
     * 
     * Routes all shader compilation and linking through our GLSL pipeline system.
     */
    @Mixin(net.minecraft.client.shader.ShaderManager.class)
    public static class MixinShaderManager {
        private static OpenGLManager glManager;
        private static GLSLPipelineProvider glslPipeline;
        private static GLSLCallMapper glslMapper;
        
        /**
         * Initialize our pipeline references when shader manager is created.
         */
        @Inject(method = "<init>", at = @At("RETURN"))
        private void onInit(CallbackInfo ci) {
            if (OpenGLManager.isInitialized()) {
                glManager = OpenGLManager.getSafe();
                glslPipeline = glManager.getGLSLPipeline();
                glslMapper = glManager.getGLSLMapper();
            }
        }
        
        /**
         * Hook shader compilation to route through our GLSL pipeline.
         */
        @Inject(method = "createShader", at = @At("HEAD"), cancellable = true)
        private static void onCreateShader(int shaderType, String name, String source, 
                                          CallbackInfoReturnable<Integer> cir) {
            if (!OpenGLManager.isInitialized() || glslPipeline == null) {
                return;
            }
            
            try {
                int shaderId = glslPipeline.compileShader(shaderType, name, source);
                cir.setReturnValue(shaderId);
            } catch (Exception e) {
                System.err.println("[MixinShaderManager] Failed to compile shader: " + e.getMessage());
            }
        }
        
        /**
         * Hook shader linking to route through our GLSL pipeline.
         */
        @Inject(method = "linkProgram", at = @At("HEAD"), cancellable = true)
        private static void onLinkProgram(int program, CallbackInfoReturnable<Boolean> cir) {
            if (!OpenGLManager.isInitialized() || glslPipeline == null) {
                return;
            }
            
            try {
                boolean success = glslPipeline.linkProgram(program);
                cir.setReturnValue(success);
            } catch (Exception e) {
                System.err.println("[MixinShaderManager] Failed to link program: " + e.getMessage());
            }
        }
        
        /**
         * Hook uniform location queries to use our cache.
         */
        @Inject(method = "getUniformLocation", at = @At("HEAD"), cancellable = true)
        private void onGetUniformLocation(int program, String name, 
                                          CallbackInfoReturnable<Integer> cir) {
            if (glslPipeline != null) {
                try {
                    int location = glslPipeline.getUniformLocation(program, name);
                    cir.setReturnValue(location);
                } catch (Exception ignored) {
                }
            }
        }
    }
    
    /**
     * Mixin for GlStateManager - hooks all GL state changes to route through OpenGLManager.
     * 
     * Routes all GL state changes through our manager for caching and optimization.
     */
    @Mixin(net.minecraft.client.renderer.GlStateManager.class)
    public static class MixinGlStateManager {
        private static OpenGLManager glManager;
        
        private static OpenGLManager getManager() {
            if (glManager == null && OpenGLManager.isInitialized()) {
                glManager = OpenGLManager.getSafe();
            }
            return glManager;
        }
        
        // Buffer operations
        @Redirect(method = "bindBuffer", at = @At(value = "INVOKE", 
                  target = "Lorg/lwjgl/opengl/GL15;glBindBuffer(II)V"))
        private static void redirectBindBuffer(int target, int buffer) {
            OpenGLManager mgr = getManager();
            if (mgr != null) {
                mgr.bindBuffer(target, buffer);
            } else {
                org.lwjgl.opengl.GL15.glBindBuffer(target, buffer);
            }
        }
        
        // Texture operations
        @Redirect(method = "activeTexture", at = @At(value = "INVOKE",
                  target = "Lorg/lwjgl/opengl/GL13;glActiveTexture(I)V"))
        private static void redirectActiveTexture(int texture) {
            OpenGLManager mgr = getManager();
            if (mgr != null) {
                mgr.activeTexture(texture);
            } else {
                org.lwjgl.opengl.GL13.glActiveTexture(texture);
            }
        }
        
        @Redirect(method = "bindTexture", at = @At(value = "INVOKE",
                  target = "Lorg/lwjgl/opengl/GL11;glBindTexture(II)V"))
        private static void redirectBindTexture(int target, int texture) {
            OpenGLManager mgr = getManager();
            if (mgr != null) {
                mgr.bindTexture(target, texture);
            } else {
                org.lwjgl.opengl.GL11.glBindTexture(target, texture);
            }
        }
        
        // Enable/Disable
        @Redirect(method = "enableBlend", at = @At(value = "INVOKE", 
                  target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V"))
        private static void redirectEnableBlend(int cap) {
            OpenGLManager mgr = getManager();
            if (mgr != null) {
                mgr.enable(cap);
            } else {
                org.lwjgl.opengl.GL11.glEnable(cap);
            }
        }
        
        @Redirect(method = "disableBlend", at = @At(value = "INVOKE",
                  target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V"))
        private static void redirectDisableBlend(int cap) {
            OpenGLManager mgr = getManager();
            if (mgr != null) {
                mgr.disable(cap);
            } else {
                org.lwjgl.opengl.GL11.glDisable(cap);
            }
        }
        
        // Depth operations
        @Redirect(method = "depthMask", at = @At(value = "INVOKE",
                  target = "Lorg/lwjgl/opengl/GL11;glDepthMask(Z)V"))
        private static void redirectDepthMask(boolean flag) {
            OpenGLManager mgr = getManager();
            if (mgr != null) {
                mgr.depthMask(flag);
            } else {
                org.lwjgl.opengl.GL11.glDepthMask(flag);
            }
        }
        
        @Redirect(method = "depthFunc", at = @At(value = "INVOKE",
                  target = "Lorg/lwjgl/opengl/GL11;glDepthFunc(I)V"))
        private static void redirectDepthFunc(int func) {
            OpenGLManager mgr = getManager();
            if (mgr != null) {
                mgr.depthFunc(func);
            } else {
                org.lwjgl.opengl.GL11.glDepthFunc(func);
            }
        }
        
        // Blend operations
        @Redirect(method = "blendFunc", at = @At(value = "INVOKE",
                  target = "Lorg/lwjgl/opengl/GL14;glBlendFunc(II)V"))
        private static void redirectBlendFunc(int sfactor, int dfactor) {
            OpenGLManager mgr = getManager();
            if (mgr != null) {
                mgr.blendFunc(sfactor, dfactor);
            } else {
                org.lwjgl.opengl.GL14.glBlendFunc(sfactor, dfactor);
            }
        }
        
        @Redirect(method = "blendFuncSeparate", at = @At(value = "INVOKE",
                  target = "Lorg/lwjgl/opengl/GL14;glBlendFuncSeparate(IIII)V"))
        private static void redirectBlendFuncSeparate(int srcRGB, int dstRGB, int srcA, int dstA) {
            OpenGLManager mgr = getManager();
            if (mgr != null) {
                mgr.blendFuncSeparate(srcRGB, dstRGB, srcA, dstA);
            } else {
                org.lwjgl.opengl.GL14.glBlendFuncSeparate(srcRGB, dstRGB, srcA, dstA);
            }
        }
        
        // Program operations
        @Redirect(method = "useProgram", at = @At(value = "INVOKE",
                  target = "Lorg/lwjgl/opengl/GL20;glUseProgram(I)V"))
        private static void redirectUseProgram(int program) {
            OpenGLManager mgr = getManager();
            if (mgr != null) {
                mgr.useProgram(program);
            } else {
                org.lwjgl.opengl.GL20.glUseProgram(program);
            }
        }
        
        // VAO operations
        @Redirect(method = "bindVertexArray", at = @At(value = "INVOKE",
                  target = "Lorg/lwjgl/opengl/GL30;glBindVertexArray(I)V"))
        private static void redirectBindVertexArray(int vao) {
            OpenGLManager mgr = getManager();
            if (mgr != null) {
                mgr.bindVertexArray(vao);
            } else {
                org.lwjgl.opengl.GL30.glBindVertexArray(vao);
            }
        }
        
        // Framebuffer operations
        @Redirect(method = "bindFramebuffer", at = @At(value = "INVOKE",
                  target = "Lorg/lwjgl/opengl/GL30;glBindFramebuffer(II)V"))
        private static void redirectBindFramebuffer(int target, int framebuffer) {
            OpenGLManager mgr = getManager();
            if (mgr != null) {
                mgr.bindFramebuffer(target, framebuffer);
            } else {
                org.lwjgl.opengl.GL30.glBindFramebuffer(target, framebuffer);
            }
        }
    }
}
