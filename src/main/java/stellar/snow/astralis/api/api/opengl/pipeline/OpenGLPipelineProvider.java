package stellar.snow.astralis.api.opengl.pipeline;

import stellar.snow.astralis.Astralis;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static stellar.snow.astralis.engine.render.pipeline.RenderConstants.*;

/**
 * OpenGLPipelineProvider - State machine manager for OpenGL rendering.
 * 
 * <p>While OpenGL doesn't have explicit pipeline objects like Vulkan, this class
 * provides similar functionality by:
 * <ol>
 *   <li>Tracking current GL state to avoid redundant calls</li>
 *   <li>Caching compiled shader programs</li>
 *   <li>Managing VAO configurations</li>
 *   <li>Providing state diff and batch application</li>
 *   <li>Handling version-specific optimizations</li>
 * </ol>
 * </p>
 * 
 * <h2>Optimization Strategies:</h2>
 * <ul>
 *   <li><b>State Shadowing:</b> Track all GL state locally to skip no-op calls</li>
 *   <li><b>State Sorting:</b> Apply expensive state changes first</li>
 *   <li><b>VAO Caching:</b> Reuse vertex array configurations</li>
 *   <li><b>Program Caching:</b> Cache linked shader programs</li>
 *   <li><b>DSA Usage:</b> Use Direct State Access on GL 4.5+ for fewer binds</li>
 * </ul>
 */
public class OpenGLPipelineProvider {

    // GL Version info
    private final GLVersion glVersion;
    private final boolean supportsDSA;
    private final boolean supportsPersistentMapping;
    private final boolean supportsBindlessTextures;
    private final boolean supportsComputeShaders;
    private final boolean supportsSPIRV;

    // State shadow - mirrors current GL state
    private final GLStateShadow currentState;
    private final GLStateShadow pendingState;

    // Caches
    private final ConcurrentHashMap<ProgramKey, Integer> programCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, VAOConfiguration> vaoCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SamplerKey, Integer> samplerCache = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicLong stateChanges = new AtomicLong(0);
    private final AtomicLong redundantChanges = new AtomicLong(0);
    private final AtomicLong drawCalls = new AtomicLong(0);

    // Current bindings (for redundancy elimination)
    private int boundProgram = 0;
    private int boundVAO = 0;
    private int boundFBO = 0;
    private final int[] boundTextures = new int[32];
    private final int[] boundSamplers = new int[32];
    private final int[] boundUBOs = new int[16];
    private final int[] boundSSBOs = new int[16];

    public OpenGLPipelineProvider() {
        this.glVersion = detectGLVersion();
        this.supportsDSA = glVersion.isAtLeast(GLVersion.GL_4_5);
        this.supportsPersistentMapping = glVersion.isAtLeast(GLVersion.GL_4_4);
        this.supportsBindlessTextures = GL.getCapabilities().GL_ARB_bindless_texture;
        this.supportsComputeShaders = glVersion.isAtLeast(GLVersion.GL_4_3);
        this.supportsSPIRV = glVersion.isAtLeast(GLVersion.GL_4_6);

        this.currentState = new GLStateShadow();
        this.pendingState = new GLStateShadow();

        // Initialize to GL defaults
        currentState.initDefaults();
        pendingState.initDefaults();

        Astralis.LOGGER.info("[GLPipelineProvider] Initialized for GL {}.{} (DSA={}, Persistent={}, Bindless={}, Compute={}, SPIRV={})",
            glVersion.major, glVersion.minor, supportsDSA, supportsPersistentMapping, 
            supportsBindlessTextures, supportsComputeShaders, supportsSPIRV);
    }

    private GLVersion detectGLVersion() {
        int major = GL11.glGetInteger(GL30.GL_MAJOR_VERSION);
        int minor = GL11.glGetInteger(GL30.GL_MINOR_VERSION);
        return GLVersion.fromNumbers(major, minor);
    }

    // ========================================================================
    // STATE APPLICATION FROM RenderState
    // ========================================================================

    /**
     * Applies the current RenderState to OpenGL, minimizing redundant calls.
     * 
     * @param state The render state to apply
     */
    public void applyState(RenderState state) {
        MemorySegment mem = state.getStateMemory();

        // Read state from memory segment
        long blendPacked = mem.get(ValueLayout.JAVA_LONG, RenderState.OFF_BLEND);
        long depthPacked = mem.get(ValueLayout.JAVA_LONG, RenderState.OFF_DEPTH);
        long stencilPacked = mem.get(ValueLayout.JAVA_LONG, RenderState.OFF_STENCIL);
        long cullPacked = mem.get(ValueLayout.JAVA_LONG, RenderState.OFF_CULL);
        long colorMask = mem.get(ValueLayout.JAVA_LONG, RenderState.OFF_COLOR_MASK);

        // Apply each state category
        applyBlendState(blendPacked);
        applyDepthState(depthPacked);
        applyStencilState(stencilPacked);
        applyCullState(cullPacked);
        applyColorMask(colorMask);

        // Apply viewport/scissor
        applyViewport(
            mem.get(ValueLayout.JAVA_INT, RenderState.OFF_VIEWPORT_X),
            mem.get(ValueLayout.JAVA_INT, RenderState.OFF_VIEWPORT_Y),
            mem.get(ValueLayout.JAVA_INT, RenderState.OFF_VIEWPORT_W),
            mem.get(ValueLayout.JAVA_INT, RenderState.OFF_VIEWPORT_H)
        );

        boolean scissorEnabled = mem.get(ValueLayout.JAVA_BYTE, RenderState.OFF_SCISSOR_ENABLED) != 0;
        if (scissorEnabled) {
            applyScissor(
                mem.get(ValueLayout.JAVA_INT, RenderState.OFF_SCISSOR_X),
                mem.get(ValueLayout.JAVA_INT, RenderState.OFF_SCISSOR_Y),
                mem.get(ValueLayout.JAVA_INT, RenderState.OFF_SCISSOR_W),
                mem.get(ValueLayout.JAVA_INT, RenderState.OFF_SCISSOR_H)
            );
        } else {
            disableScissor();
        }

        // Apply program
        long programHandle = mem.get(ValueLayout.JAVA_LONG, RenderState.OFF_ACTIVE_PROGRAM);
        if (programHandle != 0) {
            useProgram((int) programHandle);
        }
    }

    // ========================================================================
    // BLEND STATE
    // ========================================================================

    private void applyBlendState(long packed) {
        boolean enabled = (packed & 1) != 0;
        int srcColor = (int) ((packed >> 8) & 0xFF);
        int dstColor = (int) ((packed >> 16) & 0xFF);
        int blendOp = (int) ((packed >> 24) & 0xFF);
        int srcAlpha = (int) ((packed >> 32) & 0xFF);
        int dstAlpha = (int) ((packed >> 40) & 0xFF);
        int alphaOp = (int) ((packed >> 48) & 0xFF);

        if (enabled != currentState.blendEnabled) {
            if (enabled) {
                GL11.glEnable(GL11.GL_BLEND);
            } else {
                GL11.glDisable(GL11.GL_BLEND);
            }
            currentState.blendEnabled = enabled;
            stateChanges.incrementAndGet();
        } else {
            redundantChanges.incrementAndGet();
        }

        if (enabled) {
            // Check if we need separate blend func
            boolean needsSeparate = srcColor != srcAlpha || dstColor != dstAlpha;
            
            if (needsSeparate) {
                if (currentState.srcColorFactor != srcColor || 
                    currentState.dstColorFactor != dstColor ||
                    currentState.srcAlphaFactor != srcAlpha ||
                    currentState.dstAlphaFactor != dstAlpha) {
                    
                    GL14.glBlendFuncSeparate(
                        translateBlendFactor(srcColor),
                        translateBlendFactor(dstColor),
                        translateBlendFactor(srcAlpha),
                        translateBlendFactor(dstAlpha)
                    );
                    currentState.srcColorFactor = srcColor;
                    currentState.dstColorFactor = dstColor;
                    currentState.srcAlphaFactor = srcAlpha;
                    currentState.dstAlphaFactor = dstAlpha;
                    stateChanges.incrementAndGet();
                }
            } else {
                if (currentState.srcColorFactor != srcColor || 
                    currentState.dstColorFactor != dstColor) {
                    
                    GL11.glBlendFunc(
                        translateBlendFactor(srcColor),
                        translateBlendFactor(dstColor)
                    );
                    currentState.srcColorFactor = srcColor;
                    currentState.dstColorFactor = dstColor;
                    currentState.srcAlphaFactor = srcAlpha;
                    currentState.dstAlphaFactor = dstAlpha;
                    stateChanges.incrementAndGet();
                }
            }

            // Blend equation
            boolean needsSeparateOp = blendOp != alphaOp;
            if (needsSeparateOp) {
                if (currentState.blendOpColor != blendOp || currentState.blendOpAlpha != alphaOp) {
                    GL20.glBlendEquationSeparate(
                        translateBlendOp(blendOp),
                        translateBlendOp(alphaOp)
                    );
                    currentState.blendOpColor = blendOp;
                    currentState.blendOpAlpha = alphaOp;
                    stateChanges.incrementAndGet();
                }
            } else {
                if (currentState.blendOpColor != blendOp) {
                    GL14.glBlendEquation(translateBlendOp(blendOp));
                    currentState.blendOpColor = blendOp;
                    currentState.blendOpAlpha = alphaOp;
                    stateChanges.incrementAndGet();
                }
            }
        }
    }

    // ========================================================================
    // DEPTH STATE
    // ========================================================================

    private void applyDepthState(long packed) {
        boolean testEnabled = (packed & 1) != 0;
        boolean writeEnabled = ((packed >> 1) & 1) != 0;
        int func = (int) ((packed >> 8) & 0xFF);
        float rangeNear = Float.intBitsToFloat((int) ((packed >> 16) & 0xFFFF));
        float rangeFar = Float.intBitsToFloat((int) ((packed >> 32) & 0xFFFF));

        // Depth test enable
        if (testEnabled != currentState.depthTestEnabled) {
            if (testEnabled) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            } else {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
            currentState.depthTestEnabled = testEnabled;
            stateChanges.incrementAndGet();
        }

        // Depth write mask
        if (writeEnabled != currentState.depthWriteEnabled) {
            GL11.glDepthMask(writeEnabled);
            currentState.depthWriteEnabled = writeEnabled;
            stateChanges.incrementAndGet();
        }

        // Depth function
        if (func != currentState.depthFunc) {
            GL11.glDepthFunc(translateCompareFunc(func));
            currentState.depthFunc = func;
            stateChanges.incrementAndGet();
        }
    }

    // ========================================================================
    // STENCIL STATE
    // ========================================================================

    private void applyStencilState(long packed) {
        boolean enabled = (packed & 1) != 0;
        int func = (int) ((packed >> 8) & 0xFF);
        int ref = (int) ((packed >> 16) & 0xFF);
        int mask = (int) ((packed >> 24) & 0xFF);
        int sfail = (int) ((packed >> 32) & 0xFF);
        int dpfail = (int) ((packed >> 40) & 0xFF);
        int dppass = (int) ((packed >> 48) & 0xFF);

        if (enabled != currentState.stencilTestEnabled) {
            if (enabled) {
                GL11.glEnable(GL11.GL_STENCIL_TEST);
            } else {
                GL11.glDisable(GL11.GL_STENCIL_TEST);
            }
            currentState.stencilTestEnabled = enabled;
            stateChanges.incrementAndGet();
        }

        if (enabled) {
            if (currentState.stencilFunc != func || 
                currentState.stencilRef != ref || 
                currentState.stencilMask != mask) {
                
                GL11.glStencilFunc(translateCompareFunc(func), ref, mask);
                currentState.stencilFunc = func;
                currentState.stencilRef = ref;
                currentState.stencilMask = mask;
                stateChanges.incrementAndGet();
            }

            if (currentState.stencilSFail != sfail ||
                currentState.stencilDPFail != dpfail ||
                currentState.stencilDPPass != dppass) {
                
                GL11.glStencilOp(
                    translateStencilOp(sfail),
                    translateStencilOp(dpfail),
                    translateStencilOp(dppass)
                );
                currentState.stencilSFail = sfail;
                currentState.stencilDPFail = dpfail;
                currentState.stencilDPPass = dppass;
                stateChanges.incrementAndGet();
            }
        }
    }

    // ========================================================================
    // CULL STATE
    // ========================================================================

    private void applyCullState(long packed) {
        boolean enabled = (packed & 1) != 0;
        int face = (int) ((packed >> 8) & 0xFF);
        int frontFace = (int) ((packed >> 16) & 0xFF);
        int polygonMode = (int) ((packed >> 24) & 0xFF);

        if (enabled != currentState.cullFaceEnabled) {
            if (enabled) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            } else {
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
            currentState.cullFaceEnabled = enabled;
            stateChanges.incrementAndGet();
        }

        if (enabled && face != currentState.cullFace) {
            GL11.glCullFace(translateCullFace(face));
            currentState.cullFace = face;
            stateChanges.incrementAndGet();
        }

        if (frontFace != currentState.frontFace) {
            GL11.glFrontFace(translateFrontFace(frontFace));
            currentState.frontFace = frontFace;
            stateChanges.incrementAndGet();
        }

        if (polygonMode != currentState.polygonMode) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, translatePolygonMode(polygonMode));
            currentState.polygonMode = polygonMode;
            stateChanges.incrementAndGet();
        }
    }

    // ========================================================================
    // COLOR MASK
    // ========================================================================

    private void applyColorMask(long packed) {
        boolean r = (packed & 1) != 0;
        boolean g = ((packed >> 1) & 1) != 0;
        boolean b = ((packed >> 2) & 1) != 0;
        boolean a = ((packed >> 3) & 1) != 0;

        if (r != currentState.colorMaskR ||
            g != currentState.colorMaskG ||
            b != currentState.colorMaskB ||
            a != currentState.colorMaskA) {
            
            GL11.glColorMask(r, g, b, a);
            currentState.colorMaskR = r;
            currentState.colorMaskG = g;
            currentState.colorMaskB = b;
            currentState.colorMaskA = a;
            stateChanges.incrementAndGet();
        }
    }

    // ========================================================================
    // VIEWPORT & SCISSOR
    // ========================================================================

    private void applyViewport(int x, int y, int width, int height) {
        if (x != currentState.viewportX ||
            y != currentState.viewportY ||
            width != currentState.viewportW ||
            height != currentState.viewportH) {
            
            GL11.glViewport(x, y, width, height);
            currentState.viewportX = x;
            currentState.viewportY = y;
            currentState.viewportW = width;
            currentState.viewportH = height;
            stateChanges.incrementAndGet();
        }
    }

    private void applyScissor(int x, int y, int width, int height) {
        if (!currentState.scissorEnabled) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            currentState.scissorEnabled = true;
            stateChanges.incrementAndGet();
        }

        if (x != currentState.scissorX ||
            y != currentState.scissorY ||
            width != currentState.scissorW ||
            height != currentState.scissorH) {
            
            GL11.glScissor(x, y, width, height);
            currentState.scissorX = x;
            currentState.scissorY = y;
            currentState.scissorW = width;
            currentState.scissorH = height;
            stateChanges.incrementAndGet();
        }
    }

    private void disableScissor() {
        if (currentState.scissorEnabled) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            currentState.scissorEnabled = false;
            stateChanges.incrementAndGet();
        }
    }

    // ========================================================================
    // PROGRAM MANAGEMENT
    // ========================================================================

    public void useProgram(int program) {
        if (program != boundProgram) {
            GL20.glUseProgram(program);
            boundProgram = program;
            stateChanges.incrementAndGet();
        } else {
            redundantChanges.incrementAndGet();
        }
    }

    /**
     * Gets or creates a linked program from shader sources.
     */
    public int getProgram(int vertexShader, int fragmentShader) {
        ProgramKey key = new ProgramKey(vertexShader, fragmentShader, 0);
        return programCache.computeIfAbsent(key, k -> linkProgram(vertexShader, fragmentShader, 0));
    }

    public int getProgram(int vertexShader, int fragmentShader, int geometryShader) {
        ProgramKey key = new ProgramKey(vertexShader, fragmentShader, geometryShader);
        return programCache.computeIfAbsent(key, k -> linkProgram(vertexShader, fragmentShader, geometryShader));
    }

    private int linkProgram(int vs, int fs, int gs) {
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);
        if (gs != 0) {
            GL20.glAttachShader(program, gs);
        }
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program);
            GL20.glDeleteProgram(program);
            throw new RuntimeException("Program linking failed: " + log);
        }

        return program;
    }

    // ========================================================================
    // VAO MANAGEMENT
    // ========================================================================

    public void bindVAO(int vao) {
        if (vao != boundVAO) {
            GL30.glBindVertexArray(vao);
            boundVAO = vao;
            stateChanges.incrementAndGet();
        } else {
            redundantChanges.incrementAndGet();
        }
    }

    /**
     * Creates or retrieves a VAO for the given vertex format.
     */
    public int getVAO(int formatHash, VertexFormatDescriptor format) {
        VAOConfiguration cached = vaoCache.get(formatHash);
        if (cached != null) {
            return cached.vao;
        }

        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        // Configure vertex attributes
        for (VertexAttribute attr : format.attributes()) {
            GL20.glEnableVertexAttribArray(attr.location());
            
            if (attr.isInteger()) {
                GL30.glVertexAttribIPointer(
                    attr.location(),
                    attr.count(),
                    translateGLType(attr.type()),
                    format.stride(),
                    attr.offset()
                );
            } else {
                GL20.glVertexAttribPointer(
                    attr.location(),
                    attr.count(),
                    translateGLType(attr.type()),
                    attr.normalized(),
                    format.stride(),
                    attr.offset()
                );
            }

            if (attr.divisor() > 0) {
                GL33.glVertexAttribDivisor(attr.location(), attr.divisor());
            }
        }

        GL30.glBindVertexArray(0);
        vaoCache.put(formatHash, new VAOConfiguration(vao, format));
        boundVAO = 0;

        return vao;
    }

    // ========================================================================
    // TEXTURE BINDING
    // ========================================================================

    public void bindTexture(int unit, int target, int texture) {
        if (boundTextures[unit] != texture) {
            if (supportsDSA) {
                GL45.glBindTextureUnit(unit, texture);
            } else {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
                GL11.glBindTexture(target, texture);
            }
            boundTextures[unit] = texture;
            stateChanges.incrementAndGet();
        } else {
            redundantChanges.incrementAndGet();
        }
    }

    public void bindSampler(int unit, int sampler) {
        if (boundSamplers[unit] != sampler) {
            GL33.glBindSampler(unit, sampler);
            boundSamplers[unit] = sampler;
            stateChanges.incrementAndGet();
        }
    }

    /**
     * Gets or creates a sampler object for the given parameters.
     */
    public int getSampler(int minFilter, int magFilter, int wrapS, int wrapT, int wrapR, float anisotropy) {
        SamplerKey key = new SamplerKey(minFilter, magFilter, wrapS, wrapT, wrapR, anisotropy);
        return samplerCache.computeIfAbsent(key, k -> createSampler(minFilter, magFilter, wrapS, wrapT, wrapR, anisotropy));
    }

    private int createSampler(int minFilter, int magFilter, int wrapS, int wrapT, int wrapR, float anisotropy) {
        int sampler = GL33.glGenSamplers();
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_MIN_FILTER, minFilter);
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_MAG_FILTER, magFilter);
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_WRAP_S, wrapS);
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_WRAP_T, wrapT);
        GL33.glSamplerParameteri(sampler, GL12.GL_TEXTURE_WRAP_R, wrapR);
        
        if (anisotropy > 1.0f && GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
            float maxAniso = GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
            GL33.glSamplerParameterf(sampler, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 
                Math.min(anisotropy, maxAniso));
        }
        
        return sampler;
    }

    // ========================================================================
    // BUFFER BINDING
    // ========================================================================

    public void bindUBO(int bindingPoint, int buffer) {
        if (boundUBOs[bindingPoint] != buffer) {
            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, bindingPoint, buffer);
            boundUBOs[bindingPoint] = buffer;
            stateChanges.incrementAndGet();
        }
    }

    public void bindUBORange(int bindingPoint, int buffer, long offset, long size) {
        // Range bindings always need to be applied (offset may change)
        GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, bindingPoint, buffer, offset, size);
        boundUBOs[bindingPoint] = buffer;
        stateChanges.incrementAndGet();
    }

    public void bindSSBO(int bindingPoint, int buffer) {
        if (!supportsComputeShaders) return;
        
        if (boundSSBOs[bindingPoint] != buffer) {
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, bindingPoint, buffer);
            boundSSBOs[bindingPoint] = buffer;
            stateChanges.incrementAndGet();
        }
    }

    // ========================================================================
    // FRAMEBUFFER
    // ========================================================================

    public void bindFramebuffer(int framebuffer) {
        if (boundFBO != framebuffer) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            boundFBO = framebuffer;
            stateChanges.incrementAndGet();
        }
    }

    public void bindReadFramebuffer(int framebuffer) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebuffer);
        stateChanges.incrementAndGet();
    }

    public void bindDrawFramebuffer(int framebuffer) {
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
        stateChanges.incrementAndGet();
    }

    // ========================================================================
    // CLEAR OPERATIONS
    // ========================================================================

    public void clear(boolean color, boolean depth, boolean stencil) {
        int mask = 0;
        if (color) mask |= GL11.GL_COLOR_BUFFER_BIT;
        if (depth) mask |= GL11.GL_DEPTH_BUFFER_BIT;
        if (stencil) mask |= GL11.GL_STENCIL_BUFFER_BIT;
        
        if (mask != 0) {
            GL11.glClear(mask);
        }
    }

    public void setClearColor(float r, float g, float b, float a) {
        if (r != currentState.clearColorR ||
            g != currentState.clearColorG ||
            b != currentState.clearColorB ||
            a != currentState.clearColorA) {
            
            GL11.glClearColor(r, g, b, a);
            currentState.clearColorR = r;
            currentState.clearColorG = g;
            currentState.clearColorB = b;
            currentState.clearColorA = a;
        }
    }

    public void setClearDepth(float depth) {
        if (depth != currentState.clearDepth) {
            GL11.glClearDepth(depth);
            currentState.clearDepth = depth;
        }
    }

    public void setClearStencil(int stencil) {
        if (stencil != currentState.clearStencil) {
            GL11.glClearStencil(stencil);
            currentState.clearStencil = stencil;
        }
    }

    // ========================================================================
    // DRAW CALLS
    // ========================================================================

    public void drawArrays(int mode, int first, int count) {
        GL11.glDrawArrays(mode, first, count);
        drawCalls.incrementAndGet();
    }

    public void drawElements(int mode, int count, int type, long offset) {
        GL11.glDrawElements(mode, count, type, offset);
        drawCalls.incrementAndGet();
    }

    public void drawArraysInstanced(int mode, int first, int count, int instances) {
        GL31.glDrawArraysInstanced(mode, first, count, instances);
        drawCalls.incrementAndGet();
    }

    public void drawElementsInstanced(int mode, int count, int type, long offset, int instances) {
        GL31.glDrawElementsInstanced(mode, count, type, offset, instances);
        drawCalls.incrementAndGet();
    }

    public void drawArraysIndirect(int mode, long offset) {
        if (glVersion.isAtLeast(GLVersion.GL_4_0)) {
            GL40.glDrawArraysIndirect(mode, offset);
            drawCalls.incrementAndGet();
        }
    }

    public void drawElementsIndirect(int mode, int type, long offset) {
        if (glVersion.isAtLeast(GLVersion.GL_4_0)) {
            GL40.glDrawElementsIndirect(mode, type, offset);
            drawCalls.incrementAndGet();
        }
    }

    public void multiDrawArraysIndirect(int mode, long offset, int drawCount, int stride) {
        if (glVersion.isAtLeast(GLVersion.GL_4_3)) {
            GL43.glMultiDrawArraysIndirect(mode, offset, drawCount, stride);
            drawCalls.addAndGet(drawCount);
        }
    }

    public void multiDrawElementsIndirect(int mode, int type, long offset, int drawCount, int stride) {
        if (glVersion.isAtLeast(GLVersion.GL_4_3)) {
            GL43.glMultiDrawElementsIndirect(mode, type, offset, drawCount, stride);
            drawCalls.addAndGet(drawCount);
        }
    }

    // ========================================================================
    // TRANSLATION HELPERS
    // ========================================================================

    private int translateBlendFactor(int factor) {
        return switch (factor) {
            case 0 -> GL11.GL_ZERO;
            case 1 -> GL11.GL_ONE;
            case 2 -> GL11.GL_SRC_COLOR;
            case 3 -> GL11.GL_ONE_MINUS_SRC_COLOR;
            case 4 -> GL11.GL_DST_COLOR;
            case 5 -> GL11.GL_ONE_MINUS_DST_COLOR;
            case 6 -> GL11.GL_SRC_ALPHA;
            case 7 -> GL11.GL_ONE_MINUS_SRC_ALPHA;
            case 8 -> GL11.GL_DST_ALPHA;
            case 9 -> GL11.GL_ONE_MINUS_DST_ALPHA;
            case 10 -> GL14.GL_CONSTANT_COLOR;
            case 11 -> GL14.GL_ONE_MINUS_CONSTANT_COLOR;
            case 12 -> GL14.GL_CONSTANT_ALPHA;
            case 13 -> GL14.GL_ONE_MINUS_CONSTANT_ALPHA;
            case 14 -> GL11.GL_SRC_ALPHA_SATURATE;
            default -> GL11.GL_ONE;
        };
    }

    private int translateBlendOp(int op) {
        return switch (op) {
            case 0 -> GL14.GL_FUNC_ADD;
            case 1 -> GL14.GL_FUNC_SUBTRACT;
            case 2 -> GL14.GL_FUNC_REVERSE_SUBTRACT;
            case 3 -> GL14.GL_MIN;
            case 4 -> GL14.GL_MAX;
            default -> GL14.GL_FUNC_ADD;
        };
    }

    private int translateCompareFunc(int func) {
        return switch (func) {
            case 0 -> GL11.GL_NEVER;
            case 1 -> GL11.GL_LESS;
            case 2 -> GL11.GL_EQUAL;
            case 3 -> GL11.GL_LEQUAL;
            case 4 -> GL11.GL_GREATER;
            case 5 -> GL11.GL_NOTEQUAL;
            case 6 -> GL11.GL_GEQUAL;
            case 7 -> GL11.GL_ALWAYS;
            default -> GL11.GL_LESS;
        };
    }

    private int translateStencilOp(int op) {
        return switch (op) {
            case 0 -> GL11.GL_KEEP;
            case 1 -> GL11.GL_ZERO;
            case 2 -> GL11.GL_REPLACE;
            case 3 -> GL11.GL_INCR;
            case 4 -> GL14.GL_INCR_WRAP;
            case 5 -> GL11.GL_DECR;
            case 6 -> GL14.GL_DECR_WRAP;
            case 7 -> GL11.GL_INVERT;
            default -> GL11.GL_KEEP;
        };
    }

    private int translateCullFace(int face) {
        return switch (face) {
            case 0 -> GL11.GL_FRONT;
            case 1 -> GL11.GL_BACK;
            case 2 -> GL11.GL_FRONT_AND_BACK;
            default -> GL11.GL_BACK;
        };
    }

    private int translateFrontFace(int face) {
        return switch (face) {
            case 0 -> GL11.GL_CW;
            case 1 -> GL11.GL_CCW;
            default -> GL11.GL_CCW;
        };
    }

    private int translatePolygonMode(int mode) {
        return switch (mode) {
            case 0 -> GL11.GL_POINT;
            case 1 -> GL11.GL_LINE;
            case 2 -> GL11.GL_FILL;
            default -> GL11.GL_FILL;
        };
    }

    private int translateGLType(int type) {
        return switch (type) {
            case 0 -> GL11.GL_BYTE;
            case 1 -> GL11.GL_UNSIGNED_BYTE;
            case 2 -> GL11.GL_SHORT;
            case 3 -> GL11.GL_UNSIGNED_SHORT;
            case 4 -> GL11.GL_INT;
            case 5 -> GL11.GL_UNSIGNED_INT;
            case 6 -> GL11.GL_FLOAT;
            case 7 -> GL11.GL_DOUBLE;
            case 8 -> GL30.GL_HALF_FLOAT;
            default -> GL11.GL_FLOAT;
        };
    }

    // ========================================================================
    // STATE RESET
    // ========================================================================

    /**
     * Resets all tracked state to force re-application.
     */
    public void invalidateState() {
        currentState.invalidate();
        boundProgram = -1;
        boundVAO = -1;
        boundFBO = -1;
        java.util.Arrays.fill(boundTextures, -1);
        java.util.Arrays.fill(boundSamplers, -1);
        java.util.Arrays.fill(boundUBOs, -1);
        java.util.Arrays.fill(boundSSBOs, -1);
    }

    /**
     * Syncs internal state with actual GL state (useful after external GL calls).
     */
    public void syncWithGL() {
        boundProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boundVAO = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        boundFBO = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        
        // Sync other state...
        currentState.depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        currentState.blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        currentState.cullFaceEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        currentState.scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        currentState.stencilTestEnabled = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    public String getStatistics() {
        long total = stateChanges.get() + redundantChanges.get();
        double efficiency = total > 0 ? (redundantChanges.get() * 100.0 / total) : 0;
        return String.format("StateChanges: %d, Redundant: %d (%.1f%% eliminated), DrawCalls: %d, Programs: %d, VAOs: %d",
            stateChanges.get(), redundantChanges.get(), efficiency, drawCalls.get(),
            programCache.size(), vaoCache.size());
    }

    public void resetStatistics() {
        stateChanges.set(0);
        redundantChanges.set(0);
        drawCalls.set(0);
    }

    // ========================================================================
    // CLEANUP
    // ========================================================================

    public void cleanup() {
        // Delete programs
        for (int program : programCache.values()) {
            GL20.glDeleteProgram(program);
        }
        programCache.clear();

        // Delete VAOs
        for (VAOConfiguration config : vaoCache.values()) {
            GL30.glDeleteVertexArrays(config.vao);
        }
        vaoCache.clear();

        // Delete samplers
        for (int sampler : samplerCache.values()) {
            GL33.glDeleteSamplers(sampler);
        }
        samplerCache.clear();

        Astralis.LOGGER.info("[GLPipelineProvider] Cleanup complete. Final stats: {}", getStatistics());
    }

    // ========================================================================
    // GETTERS
    // ========================================================================

    public GLVersion getGLVersion() {
        return glVersion;
    }

    public boolean supportsDSA() {
        return supportsDSA;
    }

    public boolean supportsPersistentMapping() {
        return supportsPersistentMapping;
    }

    public boolean supportsBindlessTextures() {
        return supportsBindlessTextures;
    }

    public boolean supportsComputeShaders() {
        return supportsComputeShaders;
    }

    public boolean supportsSPIRV() {
        return supportsSPIRV;
    }

    // ========================================================================
    // INNER CLASSES
    // ========================================================================

    /**
     * Shadow of current OpenGL state for redundancy elimination.
     */
    private static class GLStateShadow {
        // Blend
        boolean blendEnabled;
        int srcColorFactor, dstColorFactor;
        int srcAlphaFactor, dstAlphaFactor;
        int blendOpColor, blendOpAlpha;

        // Depth
        boolean depthTestEnabled;
        boolean depthWriteEnabled;
        int depthFunc;

        // Stencil
        boolean stencilTestEnabled;
        int stencilFunc, stencilRef, stencilMask;
        int stencilSFail, stencilDPFail, stencilDPPass;

        // Cull
        boolean cullFaceEnabled;
        int cullFace;
        int frontFace;
        int polygonMode;

        // Color mask
        boolean colorMaskR, colorMaskG, colorMaskB, colorMaskA;

        // Viewport/Scissor
        int viewportX, viewportY, viewportW, viewportH;
        boolean scissorEnabled;
        int scissorX, scissorY, scissorW, scissorH;

        // Clear values
        float clearColorR, clearColorG, clearColorB, clearColorA;
        float clearDepth;
        int clearStencil;

        void initDefaults() {
            blendEnabled = false;
            srcColorFactor = 1; // ONE
            dstColorFactor = 0; // ZERO
            srcAlphaFactor = 1;
            dstAlphaFactor = 0;
            blendOpColor = 0; // ADD
            blendOpAlpha = 0;

            depthTestEnabled = false;
            depthWriteEnabled = true;
            depthFunc = 1; // LESS

            stencilTestEnabled = false;
            stencilFunc = 7; // ALWAYS
            stencilRef = 0;
            stencilMask = 0xFF;
            stencilSFail = 0; // KEEP
            stencilDPFail = 0;
            stencilDPPass = 0;

            cullFaceEnabled = false;
            cullFace = 1; // BACK
            frontFace = 1; // CCW
            polygonMode = 2; // FILL

            colorMaskR = colorMaskG = colorMaskB = colorMaskA = true;

            viewportX = viewportY = 0;
            viewportW = viewportH = 0;
            scissorEnabled = false;

            clearColorR = clearColorG = clearColorB = clearColorA = 0;
            clearDepth = 1.0f;
            clearStencil = 0;
        }

        void invalidate() {
            // Set to invalid values to force re-application
            blendEnabled = false;
            srcColorFactor = -1;
            depthTestEnabled = false;
            depthFunc = -1;
            cullFaceEnabled = false;
            cullFace = -1;
            viewportW = -1;
        }
    }

    private record ProgramKey(int vertexShader, int fragmentShader, int geometryShader) {}
    
    private record VAOConfiguration(int vao, VertexFormatDescriptor format) {}
    
    private record SamplerKey(int minFilter, int magFilter, int wrapS, int wrapT, int wrapR, float anisotropy) {}

    public record VertexAttribute(
        int location,
        int count,
        int type,
        boolean normalized,
        int offset,
        int divisor
    ) {
        public boolean isInteger() {
            return type == 0 || type == 1 || type == 2 || type == 3 || type == 4 || type == 5;
        }
    }

    public record VertexFormatDescriptor(VertexAttribute[] attributes, int stride) {}
}
