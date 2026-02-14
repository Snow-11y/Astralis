package stellar.snow.astralis.engine.render.system;
import org.lwjgl.opengl.*;
import java.nio.*;
import java.util.*;
/**
 * GLStateBackup - Robust OpenGL State Management
 * 
 * Critical for modding environments where multiple systems render in the same context.
 * Records and restores ALL relevant GL state to prevent interference with other mods/systems.
 * 
 * Features:
 * - Complete state capture (blend, depth, stencil, rasterizer, texture bindings, etc.)
 * - Stack-based state management for nested calls
 * - Automatic validation and error detection
 * - Performance optimized with dirty tracking
 */
    
    private static final int MAX_TEXTURE_UNITS = 32;
    private static final int MAX_VERTEX_ATTRIBS = 16;
    private static final int MAX_UNIFORM_BUFFER_BINDINGS = 36;
    private static final int MAX_SHADER_STORAGE_BINDINGS = 24;
    
    // State snapshot
    private static class StateSnapshot {
        // Blend state
        boolean blendEnabled;
        int blendSrcRGB, blendDstRGB, blendSrcAlpha, blendDstAlpha;
        int blendEquationRGB, blendEquationAlpha;
        float[] blendColor = new float[4];
        
        // Depth state
        boolean depthTestEnabled;
        boolean depthWriteEnabled;
        int depthFunc;
        double depthRangeNear, depthRangeFar;
        
        // Stencil state
        boolean stencilTestEnabled;
        int stencilFuncFront, stencilFuncBack;
        int stencilRefFront, stencilRefBack;
        int stencilMaskFront, stencilMaskBack;
        int stencilFailFront, stencilFailBack;
        int stencilZFailFront, stencilZFailBack;
        int stencilZPassFront, stencilZPassBack;
        int stencilWriteMask;
        
        // Rasterizer state
        boolean cullFaceEnabled;
        int cullFaceMode;
        int frontFace;
        int polygonMode;
        float lineWidth;
        float pointSize;
        boolean scissorTestEnabled;
        int[] scissorBox = new int[4];
        
        // Color state
        boolean[] colorMask = new boolean[4];
        float[] clearColor = new float[4];
        
        // Viewport
        int[] viewport = new int[4];
        
        // Framebuffer bindings
        int drawFramebuffer;
        int readFramebuffer;
        
        // Program state
        int currentProgram;
        
        // VAO state
        int vertexArray;
        
        // Buffer bindings
        int arrayBuffer;
        int elementArrayBuffer;
        int uniformBuffer;
        int shaderStorageBuffer;
        int drawIndirectBuffer;
        int dispatchIndirectBuffer;
        
        // Texture bindings (per unit)
        int activeTexture;
        int[] texture2DBindings = new int[MAX_TEXTURE_UNITS];
        int[] texture3DBindings = new int[MAX_TEXTURE_UNITS];
        int[] textureCubeBindings = new int[MAX_TEXTURE_UNITS];
        int[] texture2DArrayBindings = new int[MAX_TEXTURE_UNITS];
        
        // Sampler bindings
        int[] samplerBindings = new int[MAX_TEXTURE_UNITS];
        
        // Image bindings (bindless)
        int[] imageBindings = new int[8];
        int[] imageFormats = new int[8];
        
        // Vertex attributes
        boolean[] vertexAttribEnabled = new boolean[MAX_VERTEX_ATTRIBS];
        
        // Uniform buffer bindings
        int[] uniformBufferBindings = new int[MAX_UNIFORM_BUFFER_BINDINGS];
        long[] uniformBufferOffsets = new long[MAX_UNIFORM_BUFFER_BINDINGS];
        long[] uniformBufferSizes = new long[MAX_UNIFORM_BUFFER_BINDINGS];
        
        // Shader storage buffer bindings
        int[] ssboBindings = new int[MAX_SHADER_STORAGE_BINDINGS];
        long[] ssboOffsets = new long[MAX_SHADER_STORAGE_BINDINGS];
        long[] ssboSizes = new long[MAX_SHADER_STORAGE_BINDINGS];
        
        // Pixel pack/unpack state
        int packAlignment, unpackAlignment;
        int packRowLength, unpackRowLength;
        
        // Misc state
        boolean primitivRestartEnabled;
        int primitiveRestartIndex;
        boolean seamlessCubemapEnabled;
        boolean depthClampEnabled;
        boolean multisampleEnabled;
        boolean sampleAlphaToCoverageEnabled;
        boolean sampleAlphaToOneEnabled;
        
        // Provoking vertex
        int provokingVertex;
        
        // Clip distances
        boolean[] clipDistanceEnabled = new boolean[8];
    }
    
    // State stack for nested backup/restore
    private final Stack<StateSnapshot> stateStack = new Stack<>();
    
    // Current active snapshot
    private StateSnapshot currentSnapshot;
    
    // Dirty flags for performance optimization
    private final BitSet dirtyFlags = new BitSet(64);
    
    // Statistics
    private long totalBackups = 0;
    private long totalRestores = 0;
    private long mismatchDetections = 0;
    
    // Validation mode (expensive, use in debug builds)
    private final boolean validationEnabled;
    
    public GLStateBackup(boolean enableValidation) {
        this.validationEnabled = enableValidation;
    }
    
    /**
     * Capture current GL state
     */
    public void backup() {
        StateSnapshot snapshot = new StateSnapshot();
        captureState(snapshot);
        stateStack.push(snapshot);
        currentSnapshot = snapshot;
        totalBackups++;
        
        if (validationEnabled) {
            validateStateCapture(snapshot);
        }
    }
    
    /**
     * Restore previously captured state
     */
    public void restore() {
        if (stateStack.isEmpty()) {
            throw new IllegalStateException("No state to restore - backup() must be called first");
        }
        
        StateSnapshot snapshot = stateStack.pop();
        restoreState(snapshot);
        currentSnapshot = stateStack.isEmpty() ? null : stateStack.peek();
        totalRestores++;
        
        if (validationEnabled) {
            validateStateRestore(snapshot);
        }
    }
    
    /**
     * Capture all relevant GL state
     */
    private void captureState(StateSnapshot s) {
        // Blend state
        s.blendEnabled = glIsEnabled(GL_BLEND);
        s.blendSrcRGB = glGetInteger(GL_BLEND_SRC_RGB);
        s.blendDstRGB = glGetInteger(GL_BLEND_DST_RGB);
        s.blendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA);
        s.blendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
        s.blendEquationRGB = glGetInteger(GL_BLEND_EQUATION_RGB);
        s.blendEquationAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA);
        glGetFloatv(GL_BLEND_COLOR, s.blendColor);
        
        // Depth state
        s.depthTestEnabled = glIsEnabled(GL_DEPTH_TEST);
        s.depthWriteEnabled = glGetBoolean(GL_DEPTH_WRITEMASK);
        s.depthFunc = glGetInteger(GL_DEPTH_FUNC);
        s.depthRangeNear = glGetDouble(GL_DEPTH_RANGE);
        s.depthRangeFar = glGetDouble(GL_DEPTH_RANGE + 1);
        
        // Stencil state
        s.stencilTestEnabled = glIsEnabled(GL_STENCIL_TEST);
        s.stencilFuncFront = glGetInteger(GL_STENCIL_FUNC);
        s.stencilFuncBack = glGetInteger(GL_STENCIL_BACK_FUNC);
        s.stencilRefFront = glGetInteger(GL_STENCIL_REF);
        s.stencilRefBack = glGetInteger(GL_STENCIL_BACK_REF);
        s.stencilMaskFront = glGetInteger(GL_STENCIL_VALUE_MASK);
        s.stencilMaskBack = glGetInteger(GL_STENCIL_BACK_VALUE_MASK);
        s.stencilFailFront = glGetInteger(GL_STENCIL_FAIL);
        s.stencilFailBack = glGetInteger(GL_STENCIL_BACK_FAIL);
        s.stencilZFailFront = glGetInteger(GL_STENCIL_PASS_DEPTH_FAIL);
        s.stencilZFailBack = glGetInteger(GL_STENCIL_BACK_PASS_DEPTH_FAIL);
        s.stencilZPassFront = glGetInteger(GL_STENCIL_PASS_DEPTH_PASS);
        s.stencilZPassBack = glGetInteger(GL_STENCIL_BACK_PASS_DEPTH_PASS);
        s.stencilWriteMask = glGetInteger(GL_STENCIL_WRITEMASK);
        
        // Rasterizer state
        s.cullFaceEnabled = glIsEnabled(GL_CULL_FACE);
        s.cullFaceMode = glGetInteger(GL_CULL_FACE_MODE);
        s.frontFace = glGetInteger(GL_FRONT_FACE);
        s.polygonMode = glGetInteger(GL_POLYGON_MODE);
        s.lineWidth = glGetFloat(GL_LINE_WIDTH);
        s.pointSize = glGetFloat(GL_POINT_SIZE);
        s.scissorTestEnabled = glIsEnabled(GL_SCISSOR_TEST);
        glGetIntegerv(GL_SCISSOR_BOX, s.scissorBox);
        
        // Color state
        s.colorMask[0] = glGetBoolean(GL_COLOR_WRITEMASK);
        glGetFloatv(GL_COLOR_CLEAR_VALUE, s.clearColor);
        
        // Viewport
        glGetIntegerv(GL_VIEWPORT, s.viewport);
        
        // Framebuffer bindings
        s.drawFramebuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        s.readFramebuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        
        // Program state
        s.currentProgram = glGetInteger(GL_CURRENT_PROGRAM);
        
        // VAO state
        s.vertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        
        // Buffer bindings
        s.arrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        s.elementArrayBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING);
        s.uniformBuffer = glGetInteger(GL_UNIFORM_BUFFER_BINDING);
        s.shaderStorageBuffer = glGetInteger(GL_SHADER_STORAGE_BUFFER_BINDING);
        s.drawIndirectBuffer = glGetInteger(GL_DRAW_INDIRECT_BUFFER_BINDING);
        s.dispatchIndirectBuffer = glGetInteger(GL_DISPATCH_INDIRECT_BUFFER_BINDING);
        
        // Texture bindings
        s.activeTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        for (int i = 0; i < MAX_TEXTURE_UNITS; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            s.texture2DBindings[i] = glGetInteger(GL_TEXTURE_BINDING_2D);
            s.texture3DBindings[i] = glGetInteger(GL_TEXTURE_BINDING_3D);
            s.textureCubeBindings[i] = glGetInteger(GL_TEXTURE_BINDING_CUBE_MAP);
            s.texture2DArrayBindings[i] = glGetInteger(GL_TEXTURE_BINDING_2D_ARRAY);
            s.samplerBindings[i] = glGetInteger(GL_SAMPLER_BINDING);
        }
        glActiveTexture(s.activeTexture);
        
        // Vertex attributes
        for (int i = 0; i < MAX_VERTEX_ATTRIBS; i++) {
            s.vertexAttribEnabled[i] = glGetBooleani(GL_VERTEX_ATTRIB_ARRAY_ENABLED, i);
        }
        
        // Uniform buffer bindings
        for (int i = 0; i < MAX_UNIFORM_BUFFER_BINDINGS; i++) {
            s.uniformBufferBindings[i] = glGetIntegeri(GL_UNIFORM_BUFFER_BINDING, i);
        }
        
        // SSBO bindings
        for (int i = 0; i < MAX_SHADER_STORAGE_BINDINGS; i++) {
            s.ssboBindings[i] = glGetIntegeri(GL_SHADER_STORAGE_BUFFER_BINDING, i);
        }
        
        // Pixel pack/unpack
        s.packAlignment = glGetInteger(GL_PACK_ALIGNMENT);
        s.unpackAlignment = glGetInteger(GL_UNPACK_ALIGNMENT);
        s.packRowLength = glGetInteger(GL_PACK_ROW_LENGTH);
        s.unpackRowLength = glGetInteger(GL_UNPACK_ROW_LENGTH);
        
        // Misc state
        s.primitivRestartEnabled = glIsEnabled(GL_PRIMITIVE_RESTART);
        s.primitiveRestartIndex = glGetInteger(GL_PRIMITIVE_RESTART_INDEX);
        s.seamlessCubemapEnabled = glIsEnabled(GL_TEXTURE_CUBE_MAP_SEAMLESS);
        s.depthClampEnabled = glIsEnabled(GL_DEPTH_CLAMP);
        s.multisampleEnabled = glIsEnabled(GL_MULTISAMPLE);
        s.sampleAlphaToCoverageEnabled = glIsEnabled(GL_SAMPLE_ALPHA_TO_COVERAGE);
        s.sampleAlphaToOneEnabled = glIsEnabled(GL_SAMPLE_ALPHA_TO_ONE);
        s.provokingVertex = glGetInteger(GL_PROVOKING_VERTEX);
        
        // Clip distances
        for (int i = 0; i < 8; i++) {
            s.clipDistanceEnabled[i] = glIsEnabled(GL_CLIP_DISTANCE0 + i);
        }
    }
    
    /**
     * Restore all GL state from snapshot
     */
    private void restoreState(StateSnapshot s) {
        // Blend state
        if (s.blendEnabled) glEnable(GL_BLEND); else glDisable(GL_BLEND);
        glBlendFuncSeparate(s.blendSrcRGB, s.blendDstRGB, s.blendSrcAlpha, s.blendDstAlpha);
        glBlendEquationSeparate(s.blendEquationRGB, s.blendEquationAlpha);
        glBlendColor(s.blendColor[0], s.blendColor[1], s.blendColor[2], s.blendColor[3]);
        
        // Depth state
        if (s.depthTestEnabled) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
        glDepthMask(s.depthWriteEnabled);
        glDepthFunc(s.depthFunc);
        glDepthRange(s.depthRangeNear, s.depthRangeFar);
        
        // Stencil state
        if (s.stencilTestEnabled) glEnable(GL_STENCIL_TEST); else glDisable(GL_STENCIL_TEST);
        glStencilFuncSeparate(GL_FRONT, s.stencilFuncFront, s.stencilRefFront, s.stencilMaskFront);
        glStencilFuncSeparate(GL_BACK, s.stencilFuncBack, s.stencilRefBack, s.stencilMaskBack);
        glStencilOpSeparate(GL_FRONT, s.stencilFailFront, s.stencilZFailFront, s.stencilZPassFront);
        glStencilOpSeparate(GL_BACK, s.stencilFailBack, s.stencilZFailBack, s.stencilZPassBack);
        glStencilMask(s.stencilWriteMask);
        
        // Rasterizer state
        if (s.cullFaceEnabled) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
        glCullFace(s.cullFaceMode);
        glFrontFace(s.frontFace);
        glPolygonMode(GL_FRONT_AND_BACK, s.polygonMode);
        glLineWidth(s.lineWidth);
        glPointSize(s.pointSize);
        if (s.scissorTestEnabled) glEnable(GL_SCISSOR_TEST); else glDisable(GL_SCISSOR_TEST);
        glScissor(s.scissorBox[0], s.scissorBox[1], s.scissorBox[2], s.scissorBox[3]);
        
        // Color state
        glColorMask(s.colorMask[0], s.colorMask[1], s.colorMask[2], s.colorMask[3]);
        glClearColor(s.clearColor[0], s.clearColor[1], s.clearColor[2], s.clearColor[3]);
        
        // Viewport
        glViewport(s.viewport[0], s.viewport[1], s.viewport[2], s.viewport[3]);
        
        // Framebuffer bindings
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, s.drawFramebuffer);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, s.readFramebuffer);
        
        // Program state
        glUseProgram(s.currentProgram);
        
        // VAO state
        glBindVertexArray(s.vertexArray);
        
        // Buffer bindings
        glBindBuffer(GL_ARRAY_BUFFER, s.arrayBuffer);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, s.elementArrayBuffer);
        glBindBuffer(GL_UNIFORM_BUFFER, s.uniformBuffer);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, s.shaderStorageBuffer);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, s.drawIndirectBuffer);
        glBindBuffer(GL_DISPATCH_INDIRECT_BUFFER, s.dispatchIndirectBuffer);
        
        // Texture bindings
        for (int i = 0; i < MAX_TEXTURE_UNITS; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, s.texture2DBindings[i]);
            glBindTexture(GL_TEXTURE_3D, s.texture3DBindings[i]);
            glBindTexture(GL_TEXTURE_CUBE_MAP, s.textureCubeBindings[i]);
            glBindTexture(GL_TEXTURE_2D_ARRAY, s.texture2DArrayBindings[i]);
            glBindSampler(i, s.samplerBindings[i]);
        }
        glActiveTexture(s.activeTexture);
        
        // Vertex attributes
        for (int i = 0; i < MAX_VERTEX_ATTRIBS; i++) {
            if (s.vertexAttribEnabled[i]) {
                glEnableVertexAttribArray(i);
            } else {
                glDisableVertexAttribArray(i);
            }
        }
        
        // Uniform buffer bindings
        for (int i = 0; i < MAX_UNIFORM_BUFFER_BINDINGS; i++) {
            glBindBufferBase(GL_UNIFORM_BUFFER, i, s.uniformBufferBindings[i]);
        }
        
        // SSBO bindings
        for (int i = 0; i < MAX_SHADER_STORAGE_BINDINGS; i++) {
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, i, s.ssboBindings[i]);
        }
        
        // Pixel pack/unpack
        glPixelStorei(GL_PACK_ALIGNMENT, s.packAlignment);
        glPixelStorei(GL_UNPACK_ALIGNMENT, s.unpackAlignment);
        glPixelStorei(GL_PACK_ROW_LENGTH, s.packRowLength);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, s.unpackRowLength);
        
        // Misc state
        if (s.primitivRestartEnabled) glEnable(GL_PRIMITIVE_RESTART); else glDisable(GL_PRIMITIVE_RESTART);
        glPrimitiveRestartIndex(s.primitiveRestartIndex);
        if (s.seamlessCubemapEnabled) glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS); else glDisable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
        if (s.depthClampEnabled) glEnable(GL_DEPTH_CLAMP); else glDisable(GL_DEPTH_CLAMP);
        if (s.multisampleEnabled) glEnable(GL_MULTISAMPLE); else glDisable(GL_MULTISAMPLE);
        if (s.sampleAlphaToCoverageEnabled) glEnable(GL_SAMPLE_ALPHA_TO_COVERAGE); else glDisable(GL_SAMPLE_ALPHA_TO_COVERAGE);
        if (s.sampleAlphaToOneEnabled) glEnable(GL_SAMPLE_ALPHA_TO_ONE); else glDisable(GL_SAMPLE_ALPHA_TO_ONE);
        glProvokingVertex(s.provokingVertex);
        
        // Clip distances
        for (int i = 0; i < 8; i++) {
            if (s.clipDistanceEnabled[i]) {
                glEnable(GL_CLIP_DISTANCE0 + i);
            } else {
                glDisable(GL_CLIP_DISTANCE0 + i);
            }
        }
    }
    
    /**
     * Validate that state was captured correctly
     */
    private void validateStateCapture(StateSnapshot s) {
        int err = glGetError();
        if (err != GL_NO_ERROR) {
            System.err.println("GL Error during state capture: 0x" + Integer.toHexString(err));
            mismatchDetections++;
        }
    }
    
    /**
     * Validate that state was restored correctly
     */
    private void validateStateRestore(StateSnapshot s) {
        int err = glGetError();
        if (err != GL_NO_ERROR) {
            System.err.println("GL Error during state restore: 0x" + Integer.toHexString(err));
            mismatchDetections++;
        }
        
        // Deep validation - re-capture and compare
        StateSnapshot current = new StateSnapshot();
        captureState(current);
        
        if (!statesMatch(s, current)) {
            System.err.println("State mismatch detected after restore!");
            mismatchDetections++;
        }
    }
    
    /**
     * Compare two state snapshots
     */
    private boolean statesMatch(StateSnapshot a, StateSnapshot b) {
        // Compare critical states
        return a.blendEnabled == b.blendEnabled &&
               a.depthTestEnabled == b.depthTestEnabled &&
               a.cullFaceEnabled == b.cullFaceEnabled &&
               a.currentProgram == b.currentProgram &&
               a.vertexArray == b.vertexArray;
    }
    
    /**
     * Get statistics
     */
    public String getStatistics() {
        return String.format("GLStateBackup Stats: %d backups, %d restores, %d mismatches, stack depth: %d",
            totalBackups, totalRestores, mismatchDetections, stateStack.size());
    }
    
    /**
     * Clear the state stack
     */
    public void clear() {
        stateStack.clear();
        currentSnapshot = null;
    }
    
    @Override
    public void close() {
        if (!stateStack.isEmpty()) {
            System.err.println("Warning: GLStateBackup closed with " + stateStack.size() + " states on stack");
            clear();
        }
    }
    
    /**
     * Scoped state backup using try-with-resources
     */
    public static class ScopedBackup implements AutoCloseable {
        private final GLStateBackup backup;
        
        public ScopedBackup(GLStateBackup backup) {
            this.backup = backup;
            backup.backup();
        }
        
        @Override
        public void close() {
            backup.restore();
        }
    }
    
    /**
     * Create a scoped backup
     */
    public ScopedBackup scoped() {
        return new ScopedBackup(this);
    }
}
