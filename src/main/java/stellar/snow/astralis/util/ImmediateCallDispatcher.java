package com.example.nexus.dispatch;

import com.example.api.common.mapping.OpenGLESCallMapper;
import com.example.api.common.mapping.GLSLCallMapper;
import com.example.api.common.mapping.MetalCallMapper;
import com.example.api.common.mapping.OpenGLCallMapper;
import com.example.api.common.mapping.SPIRVCallMapper;
import com.example.api.common.mapping.VulkanCallMapper;

import java.lang.invoke.*;
import java.lang.foreign.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Ultra-High-Performance Graphics API Call Dispatcher.
 * 
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                         GLCallDispatcher v4.0                                │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
 * │  │   OpenGL     │  │    GLSL      │  │   Vulkan     │  │   SPIR-V     │     │
 * │  │   Mapper     │  │   Mapper     │  │   Mapper     │  │   Mapper     │     │
 * │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
 * │         │                 │                 │                 │             │
 * │  ┌──────┴───────┐  ┌──────┴───────┐         │                 │             │
 * │  │    Metal     │  │    GLES      │         │                 │             │
 * │  │   Mapper     │  │   Mapper     │         │                 │             │
 * │  └──────┬───────┘  └──────┬───────┘         │                 │             │
 * │         └─────────────────┴─────────────────┴─────────────────┘             │
 * │                                    │                                        │
 * │         ┌──────────────────────────┴──────────────────────────┐             │
 * │         │           Lock-Free Fast Path Dispatch              │             │
 * │         └─────────────────────────────────────────────────────┘             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @author Advanced GL Systems
 * @version 4.0.0
 * @since JDK 21+
 */
@SuppressWarnings({"unchecked", "preview"})
public final class GLCallDispatcher {

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static final int MAPPER_COUNT = 6;
    private static final int OPENGL_IDX = 0;
    private static final int GLSL_IDX = 1;
    private static final int VULKAN_IDX = 2;
    private static final int SPIRV_IDX = 3;
    private static final int METAL_IDX = 4;
    private static final int GLES_IDX = 5;
    
    private static final int DISPATCH_TABLE_SIZE = 1 << 14;
    private static final int DISPATCH_TABLE_MASK = DISPATCH_TABLE_SIZE - 1;

    // ═══════════════════════════════════════════════════════════════════════════
    // API TYPE
    // ═══════════════════════════════════════════════════════════════════════════
    
    public enum API {
        OPENGL(OPENGL_IDX), GLSL(GLSL_IDX), VULKAN(VULKAN_IDX), 
        SPIRV(SPIRV_IDX), METAL(METAL_IDX), GLES(GLES_IDX);
        
        final int idx;
        API(int idx) { this.idx = idx; }
        
        private static final API[] BY_IDX = values();
        static API of(int idx) { return BY_IDX[idx & (MAPPER_COUNT - 1)]; }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VARHANDLES
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static final VarHandle STATE_HANDLE;
    private static final VarHandle SEQUENCE_HANDLE;
    
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            STATE_HANDLE = l.findVarHandle(GLCallDispatcher.class, "state", int.class);
            SEQUENCE_HANDLE = l.findVarHandle(GLCallDispatcher.class, "sequence", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INSTANCE FIELDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private volatile int state;
    private volatile long sequence;
    
    // Mapper instances
    private final OpenGLCallMapper openGLMapper;
    private final GLSLCallMapper glslMapper;
    private final VulkanCallMapper vulkanMapper;
    private final SPIRVCallMapper spirvMapper;
    private final MetalCallMapper metalMapper;
    private final GLESCallMapper glesMapper;
    
    // Fast dispatch array
    private final Object[] mappers;
    
    // MethodHandle fast paths
    private final MethodHandle[] dispatchHandles;
    
    // Lambda fast paths (fastest)
    private final BiFunction<Integer, Object[], Object>[] lambdaPaths;
    
    // Function lookup table
    private final long[] dispatchTable; // packed: apiIdx << 32 | functionId

    // ═══════════════════════════════════════════════════════════════════════════
    // SINGLETON
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static volatile GLCallDispatcher INSTANCE;
    
    public static GLCallDispatcher get() {
        GLCallDispatcher i = INSTANCE;
        if (i == null) {
            synchronized (GLCallDispatcher.class) {
                i = INSTANCE;
                if (i == null) {
                    INSTANCE = i = new GLCallDispatcher();
                }
            }
        }
        return i;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════
    
    private GLCallDispatcher() {
        // Create mappers
        this.openGLMapper = new OpenGLCallMapper();
        this.glslMapper = new GLSLCallMapper();
        this.vulkanMapper = new VulkanCallMapper();
        this.spirvMapper = new SPIRVCallMapper();
        this.metalMapper = new MetalCallMapper();
        this.glesMapper = new GLESCallMapper();
        
        // Array for indexed access
        this.mappers = new Object[] {
            openGLMapper, glslMapper, vulkanMapper, 
            spirvMapper, metalMapper, glesMapper
        };
        
        // Initialize fast paths
        this.dispatchHandles = new MethodHandle[MAPPER_COUNT];
        this.lambdaPaths = new BiFunction[MAPPER_COUNT];
        this.dispatchTable = new long[DISPATCH_TABLE_SIZE];
        
        initializeFastPaths();
        buildDispatchTable();
        
        this.state = 1; // Ready
    }
    
    private void initializeFastPaths() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            
            for (int i = 0; i < MAPPER_COUNT; i++) {
                Object mapper = mappers[i];
                Class<?> clazz = mapper.getClass();
                
                MethodHandle mh = lookup.findVirtual(clazz, "dispatch",
                    MethodType.methodType(Object.class, int.class, Object[].class));
                dispatchHandles[i] = mh.bindTo(mapper);
                
                // Create lambda via LambdaMetafactory
                CallSite cs = LambdaMetafactory.metafactory(
                    lookup, "apply",
                    MethodType.methodType(BiFunction.class, clazz),
                    MethodType.methodType(Object.class, Object.class, Object.class),
                    mh,
                    MethodType.methodType(Object.class, Integer.class, Object[].class)
                );
                lambdaPaths[i] = (BiFunction<Integer, Object[], Object>) cs.getTarget().invoke(mapper);
            }
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }
    
    private void buildDispatchTable() {
        // OpenGL
        reg("glBindBuffer", OPENGL_IDX, 0x0001);
        reg("glBufferData", OPENGL_IDX, 0x0002);
        reg("glBufferSubData", OPENGL_IDX, 0x0003);
        reg("glDrawArrays", OPENGL_IDX, 0x0004);
        reg("glDrawElements", OPENGL_IDX, 0x0005);
        reg("glDrawArraysInstanced", OPENGL_IDX, 0x0006);
        reg("glDrawElementsInstanced", OPENGL_IDX, 0x0007);
        reg("glMultiDrawArrays", OPENGL_IDX, 0x0008);
        reg("glMultiDrawElements", OPENGL_IDX, 0x0009);
        reg("glUseProgram", OPENGL_IDX, 0x000A);
        reg("glUniform1f", OPENGL_IDX, 0x000B);
        reg("glUniform2f", OPENGL_IDX, 0x000C);
        reg("glUniform3f", OPENGL_IDX, 0x000D);
        reg("glUniform4f", OPENGL_IDX, 0x000E);
        reg("glUniform1i", OPENGL_IDX, 0x000F);
        reg("glUniform2i", OPENGL_IDX, 0x0010);
        reg("glUniform3i", OPENGL_IDX, 0x0011);
        reg("glUniform4i", OPENGL_IDX, 0x0012);
        reg("glUniformMatrix2fv", OPENGL_IDX, 0x0013);
        reg("glUniformMatrix3fv", OPENGL_IDX, 0x0014);
        reg("glUniformMatrix4fv", OPENGL_IDX, 0x0015);
        reg("glVertexAttribPointer", OPENGL_IDX, 0x0016);
        reg("glEnableVertexAttribArray", OPENGL_IDX, 0x0017);
        reg("glDisableVertexAttribArray", OPENGL_IDX, 0x0018);
        reg("glBindVertexArray", OPENGL_IDX, 0x0019);
        reg("glGenVertexArrays", OPENGL_IDX, 0x001A);
        reg("glDeleteVertexArrays", OPENGL_IDX, 0x001B);
        reg("glGenBuffers", OPENGL_IDX, 0x001C);
        reg("glDeleteBuffers", OPENGL_IDX, 0x001D);
        reg("glBindTexture", OPENGL_IDX, 0x001E);
        reg("glGenTextures", OPENGL_IDX, 0x001F);
        reg("glDeleteTextures", OPENGL_IDX, 0x0020);
        reg("glTexImage2D", OPENGL_IDX, 0x0021);
        reg("glTexSubImage2D", OPENGL_IDX, 0x0022);
        reg("glActiveTexture", OPENGL_IDX, 0x0023);
        reg("glEnable", OPENGL_IDX, 0x0024);
        reg("glDisable", OPENGL_IDX, 0x0025);
        reg("glBlendFunc", OPENGL_IDX, 0x0026);
        reg("glBlendFuncSeparate", OPENGL_IDX, 0x0027);
        reg("glDepthFunc", OPENGL_IDX, 0x0028);
        reg("glDepthMask", OPENGL_IDX, 0x0029);
        reg("glCullFace", OPENGL_IDX, 0x002A);
        reg("glFrontFace", OPENGL_IDX, 0x002B);
        reg("glViewport", OPENGL_IDX, 0x002C);
        reg("glScissor", OPENGL_IDX, 0x002D);
        reg("glClear", OPENGL_IDX, 0x002E);
        reg("glClearColor", OPENGL_IDX, 0x002F);
        reg("glClearDepth", OPENGL_IDX, 0x0030);
        reg("glBindFramebuffer", OPENGL_IDX, 0x0031);
        reg("glGenFramebuffers", OPENGL_IDX, 0x0032);
        reg("glDeleteFramebuffers", OPENGL_IDX, 0x0033);
        reg("glFramebufferTexture2D", OPENGL_IDX, 0x0034);
        reg("glCheckFramebufferStatus", OPENGL_IDX, 0x0035);
        reg("glBindRenderbuffer", OPENGL_IDX, 0x0036);
        reg("glGenRenderbuffers", OPENGL_IDX, 0x0037);
        reg("glRenderbufferStorage", OPENGL_IDX, 0x0038);
        reg("glFramebufferRenderbuffer", OPENGL_IDX, 0x0039);
        reg("glGetError", OPENGL_IDX, 0x003A);
        reg("glFlush", OPENGL_IDX, 0x003B);
        reg("glFinish", OPENGL_IDX, 0x003C);
        reg("glReadPixels", OPENGL_IDX, 0x003D);
        reg("glPixelStorei", OPENGL_IDX, 0x003E);
        reg("glLineWidth", OPENGL_IDX, 0x003F);
        reg("glPointSize", OPENGL_IDX, 0x0040);
        reg("glPolygonMode", OPENGL_IDX, 0x0041);
        reg("glPolygonOffset", OPENGL_IDX, 0x0042);
        reg("glStencilFunc", OPENGL_IDX, 0x0043);
        reg("glStencilMask", OPENGL_IDX, 0x0044);
        reg("glStencilOp", OPENGL_IDX, 0x0045);
        reg("glColorMask", OPENGL_IDX, 0x0046);
        reg("glSampleCoverage", OPENGL_IDX, 0x0047);
        reg("glTexParameteri", OPENGL_IDX, 0x0048);
        reg("glTexParameterf", OPENGL_IDX, 0x0049);
        reg("glGenerateMipmap", OPENGL_IDX, 0x004A);
        reg("glCompressedTexImage2D", OPENGL_IDX, 0x004B);
        reg("glGetIntegerv", OPENGL_IDX, 0x004C);
        reg("glGetFloatv", OPENGL_IDX, 0x004D);
        reg("glGetString", OPENGL_IDX, 0x004E);
        reg("glIsEnabled", OPENGL_IDX, 0x004F);
        
        // GLSL
        reg("glCreateShader", GLSL_IDX, 0x0101);
        reg("glDeleteShader", GLSL_IDX, 0x0102);
        reg("glShaderSource", GLSL_IDX, 0x0103);
        reg("glCompileShader", GLSL_IDX, 0x0104);
        reg("glGetShaderiv", GLSL_IDX, 0x0105);
        reg("glGetShaderInfoLog", GLSL_IDX, 0x0106);
        reg("glCreateProgram", GLSL_IDX, 0x0107);
        reg("glDeleteProgram", GLSL_IDX, 0x0108);
        reg("glAttachShader", GLSL_IDX, 0x0109);
        reg("glDetachShader", GLSL_IDX, 0x010A);
        reg("glLinkProgram", GLSL_IDX, 0x010B);
        reg("glValidateProgram", GLSL_IDX, 0x010C);
        reg("glGetProgramiv", GLSL_IDX, 0x010D);
        reg("glGetProgramInfoLog", GLSL_IDX, 0x010E);
        reg("glGetUniformLocation", GLSL_IDX, 0x010F);
        reg("glGetAttribLocation", GLSL_IDX, 0x0110);
        reg("glBindAttribLocation", GLSL_IDX, 0x0111);
        reg("glGetActiveUniform", GLSL_IDX, 0x0112);
        reg("glGetActiveAttrib", GLSL_IDX, 0x0113);
        reg("glGetUniformBlockIndex", GLSL_IDX, 0x0114);
        reg("glUniformBlockBinding", GLSL_IDX, 0x0115);
        reg("glBindBufferBase", GLSL_IDX, 0x0116);
        reg("glBindBufferRange", GLSL_IDX, 0x0117);
        reg("glGetUniformfv", GLSL_IDX, 0x0118);
        reg("glGetUniformiv", GLSL_IDX, 0x0119);
        reg("glProgramUniform1f", GLSL_IDX, 0x011A);
        reg("glProgramUniform2f", GLSL_IDX, 0x011B);
        reg("glProgramUniform3f", GLSL_IDX, 0x011C);
        reg("glProgramUniform4f", GLSL_IDX, 0x011D);
        reg("glProgramUniformMatrix4fv", GLSL_IDX, 0x011E);
        
        // Vulkan
        reg("vkCreateInstance", VULKAN_IDX, 0x0201);
        reg("vkDestroyInstance", VULKAN_IDX, 0x0202);
        reg("vkEnumeratePhysicalDevices", VULKAN_IDX, 0x0203);
        reg("vkGetPhysicalDeviceProperties", VULKAN_IDX, 0x0204);
        reg("vkGetPhysicalDeviceFeatures", VULKAN_IDX, 0x0205);
        reg("vkCreateDevice", VULKAN_IDX, 0x0206);
        reg("vkDestroyDevice", VULKAN_IDX, 0x0207);
        reg("vkGetDeviceQueue", VULKAN_IDX, 0x0208);
        reg("vkCreateCommandPool", VULKAN_IDX, 0x0209);
        reg("vkDestroyCommandPool", VULKAN_IDX, 0x020A);
        reg("vkAllocateCommandBuffers", VULKAN_IDX, 0x020B);
        reg("vkFreeCommandBuffers", VULKAN_IDX, 0x020C);
        reg("vkBeginCommandBuffer", VULKAN_IDX, 0x020D);
        reg("vkEndCommandBuffer", VULKAN_IDX, 0x020E);
        reg("vkCmdDraw", VULKAN_IDX, 0x020F);
        reg("vkCmdDrawIndexed", VULKAN_IDX, 0x0210);
        reg("vkCmdBindPipeline", VULKAN_IDX, 0x0211);
        reg("vkCmdBindVertexBuffers", VULKAN_IDX, 0x0212);
        reg("vkCmdBindIndexBuffer", VULKAN_IDX, 0x0213);
        reg("vkCmdBindDescriptorSets", VULKAN_IDX, 0x0214);
        reg("vkQueueSubmit", VULKAN_IDX, 0x0215);
        reg("vkQueueWaitIdle", VULKAN_IDX, 0x0216);
        reg("vkDeviceWaitIdle", VULKAN_IDX, 0x0217);
        reg("vkCreateBuffer", VULKAN_IDX, 0x0218);
        reg("vkDestroyBuffer", VULKAN_IDX, 0x0219);
        reg("vkCreateImage", VULKAN_IDX, 0x021A);
        reg("vkDestroyImage", VULKAN_IDX, 0x021B);
        reg("vkAllocateMemory", VULKAN_IDX, 0x021C);
        reg("vkFreeMemory", VULKAN_IDX, 0x021D);
        reg("vkMapMemory", VULKAN_IDX, 0x021E);
        reg("vkUnmapMemory", VULKAN_IDX, 0x021F);
        reg("vkBindBufferMemory", VULKAN_IDX, 0x0220);
        reg("vkBindImageMemory", VULKAN_IDX, 0x0221);
        reg("vkCreateImageView", VULKAN_IDX, 0x0222);
        reg("vkDestroyImageView", VULKAN_IDX, 0x0223);
        reg("vkCreateSampler", VULKAN_IDX, 0x0224);
        reg("vkDestroySampler", VULKAN_IDX, 0x0225);
        reg("vkCreateShaderModule", VULKAN_IDX, 0x0226);
        reg("vkDestroyShaderModule", VULKAN_IDX, 0x0227);
        reg("vkCreatePipelineLayout", VULKAN_IDX, 0x0228);
        reg("vkDestroyPipelineLayout", VULKAN_IDX, 0x0229);
        reg("vkCreateGraphicsPipelines", VULKAN_IDX, 0x022A);
        reg("vkDestroyPipeline", VULKAN_IDX, 0x022B);
        reg("vkCreateRenderPass", VULKAN_IDX, 0x022C);
        reg("vkDestroyRenderPass", VULKAN_IDX, 0x022D);
        reg("vkCreateFramebuffer", VULKAN_IDX, 0x022E);
        reg("vkDestroyFramebuffer", VULKAN_IDX, 0x022F);
        reg("vkCmdBeginRenderPass", VULKAN_IDX, 0x0230);
        reg("vkCmdEndRenderPass", VULKAN_IDX, 0x0231);
        reg("vkCmdSetViewport", VULKAN_IDX, 0x0232);
        reg("vkCmdSetScissor", VULKAN_IDX, 0x0233);
        reg("vkCreateSemaphore", VULKAN_IDX, 0x0234);
        reg("vkDestroySemaphore", VULKAN_IDX, 0x0235);
        reg("vkCreateFence", VULKAN_IDX, 0x0236);
        reg("vkDestroyFence", VULKAN_IDX, 0x0237);
        reg("vkWaitForFences", VULKAN_IDX, 0x0238);
        reg("vkResetFences", VULKAN_IDX, 0x0239);
        reg("vkAcquireNextImageKHR", VULKAN_IDX, 0x023A);
        reg("vkQueuePresentKHR", VULKAN_IDX, 0x023B);
        reg("vkCreateSwapchainKHR", VULKAN_IDX, 0x023C);
        reg("vkDestroySwapchainKHR", VULKAN_IDX, 0x023D);
        reg("vkGetSwapchainImagesKHR", VULKAN_IDX, 0x023E);
        reg("vkCreateDescriptorSetLayout", VULKAN_IDX, 0x023F);
        reg("vkDestroyDescriptorSetLayout", VULKAN_IDX, 0x0240);
        reg("vkCreateDescriptorPool", VULKAN_IDX, 0x0241);
        reg("vkDestroyDescriptorPool", VULKAN_IDX, 0x0242);
        reg("vkAllocateDescriptorSets", VULKAN_IDX, 0x0243);
        reg("vkUpdateDescriptorSets", VULKAN_IDX, 0x0244);
        reg("vkCmdCopyBuffer", VULKAN_IDX, 0x0245);
        reg("vkCmdCopyImage", VULKAN_IDX, 0x0246);
        reg("vkCmdCopyBufferToImage", VULKAN_IDX, 0x0247);
        reg("vkCmdCopyImageToBuffer", VULKAN_IDX, 0x0248);
        reg("vkCmdPipelineBarrier", VULKAN_IDX, 0x0249);
        reg("vkCmdPushConstants", VULKAN_IDX, 0x024A);
        reg("vkCmdDispatch", VULKAN_IDX, 0x024B);
        reg("vkCreateComputePipelines", VULKAN_IDX, 0x024C);
        
        // SPIR-V
        reg("spirvParse", SPIRV_IDX, 0x0301);
        reg("spirvValidate", SPIRV_IDX, 0x0302);
        reg("spirvOptimize", SPIRV_IDX, 0x0303);
        reg("spirvToGLSL", SPIRV_IDX, 0x0304);
        reg("spirvToHLSL", SPIRV_IDX, 0x0305);
        reg("spirvToMSL", SPIRV_IDX, 0x0306);
        reg("spirvReflect", SPIRV_IDX, 0x0307);
        reg("spirvLink", SPIRV_IDX, 0x0308);
        reg("spirvAssemble", SPIRV_IDX, 0x0309);
        reg("spirvDisassemble", SPIRV_IDX, 0x030A);
        reg("spirvCrossCompile", SPIRV_IDX, 0x030B);
        reg("spirvGetCapabilities", SPIRV_IDX, 0x030C);
        reg("spirvGetEntryPoints", SPIRV_IDX, 0x030D);
        reg("spirvGetDecorations", SPIRV_IDX, 0x030E);
        reg("spirvStripDebugInfo", SPIRV_IDX, 0x030F);
        reg("spirvFlatten", SPIRV_IDX, 0x0310);
        reg("spirvInline", SPIRV_IDX, 0x0311);
        reg("spirvDeadCodeElim", SPIRV_IDX, 0x0312);
        reg("spirvMerge", SPIRV_IDX, 0x0313);
        
        // Metal
        reg("mtlCreateDevice", METAL_IDX, 0x0401);
        reg("mtlCreateCommandQueue", METAL_IDX, 0x0402);
        reg("mtlCreateLibrary", METAL_IDX, 0x0403);
        reg("mtlCreateFunction", METAL_IDX, 0x0404);
        reg("mtlCreateRenderPipelineState", METAL_IDX, 0x0405);
        reg("mtlCreateComputePipelineState", METAL_IDX, 0x0406);
        reg("mtlCreateBuffer", METAL_IDX, 0x0407);
        reg("mtlCreateTexture", METAL_IDX, 0x0408);
        reg("mtlCreateSamplerState", METAL_IDX, 0x0409);
        reg("mtlCreateDepthStencilState", METAL_IDX, 0x040A);
        reg("mtlNewCommandBuffer", METAL_IDX, 0x040B);
        reg("mtlRenderCommandEncoder", METAL_IDX, 0x040C);
        reg("mtlComputeCommandEncoder", METAL_IDX, 0x040D);
        reg("mtlBlitCommandEncoder", METAL_IDX, 0x040E);
        reg("mtlDrawPrimitives", METAL_IDX, 0x040F);
        reg("mtlDrawIndexedPrimitives", METAL_IDX, 0x0410);
        reg("mtlDispatchThreadgroups", METAL_IDX, 0x0411);
        reg("mtlEndEncoding", METAL_IDX, 0x0412);
        reg("mtlCommit", METAL_IDX, 0x0413);
        reg("mtlWaitUntilCompleted", METAL_IDX, 0x0414);
        reg("mtlSetVertexBuffer", METAL_IDX, 0x0415);
        reg("mtlSetFragmentBuffer", METAL_IDX, 0x0416);
        reg("mtlSetVertexTexture", METAL_IDX, 0x0417);
        reg("mtlSetFragmentTexture", METAL_IDX, 0x0418);
        reg("mtlSetVertexSamplerState", METAL_IDX, 0x0419);
        reg("mtlSetFragmentSamplerState", METAL_IDX, 0x041A);
        reg("mtlSetRenderPipelineState", METAL_IDX, 0x041B);
        reg("mtlSetDepthStencilState", METAL_IDX, 0x041C);
        reg("mtlSetCullMode", METAL_IDX, 0x041D);
        reg("mtlSetFrontFacingWinding", METAL_IDX, 0x041E);
        reg("mtlSetViewport", METAL_IDX, 0x041F);
        reg("mtlSetScissorRect", METAL_IDX, 0x0420);
        reg("mtlSetBlendColor", METAL_IDX, 0x0421);
        reg("mtlSetStencilReferenceValue", METAL_IDX, 0x0422);
        reg("mtlUseResource", METAL_IDX, 0x0423);
        reg("mtlUseHeap", METAL_IDX, 0x0424);
        reg("mtlMemoryBarrier", METAL_IDX, 0x0425);
        reg("mtlSynchronizeResource", METAL_IDX, 0x0426);
        reg("mtlPresentDrawable", METAL_IDX, 0x0427);
        reg("mtlAddScheduledHandler", METAL_IDX, 0x0428);
        reg("mtlAddCompletedHandler", METAL_IDX, 0x0429);
        
        // GLES
        reg("glesDrawArrays", GLES_IDX, 0x0501);
        reg("glesDrawElements", GLES_IDX, 0x0502);
        reg("glEGLImageTargetTexture2DOES", GLES_IDX, 0x0503);
        reg("glEGLImageTargetRenderbufferStorageOES", GLES_IDX, 0x0504);
        reg("glMapBufferOES", GLES_IDX, 0x0505);
        reg("glUnmapBufferOES", GLES_IDX, 0x0506);
        reg("glBindVertexArrayOES", GLES_IDX, 0x0507);
        reg("glGenVertexArraysOES", GLES_IDX, 0x0508);
        reg("glDeleteVertexArraysOES", GLES_IDX, 0x0509);
        reg("glIsVertexArrayOES", GLES_IDX, 0x050A);
        reg("glRenderbufferStorageMultisampleAPPLE", GLES_IDX, 0x050B);
        reg("glResolveMultisampleFramebufferAPPLE", GLES_IDX, 0x050C);
        reg("glDiscardFramebufferEXT", GLES_IDX, 0x050D);
        reg("glMapBufferRangeEXT", GLES_IDX, 0x050E);
        reg("glFlushMappedBufferRangeEXT", GLES_IDX, 0x050F);
        reg("glTexStorage2DEXT", GLES_IDX, 0x0510);
        reg("glTexStorage3DEXT", GLES_IDX, 0x0511);
        reg("glDrawArraysInstancedEXT", GLES_IDX, 0x0512);
        reg("glDrawElementsInstancedEXT", GLES_IDX, 0x0513);
        reg("glVertexAttribDivisorEXT", GLES_IDX, 0x0514);
        reg("glDrawBuffersEXT", GLES_IDX, 0x0515);
        reg("glReadBufferNV", GLES_IDX, 0x0516);
        reg("glFramebufferTexture2DMultisampleEXT", GLES_IDX, 0x0517);
        reg("glRenderbufferStorageMultisampleEXT", GLES_IDX, 0x0518);
        reg("glBlitFramebufferANGLE", GLES_IDX, 0x0519);
        reg("glBindFragDataLocationEXT", GLES_IDX, 0x051A);
        reg("glGetFragDataLocationEXT", GLES_IDX, 0x051B);
        reg("glProgramBinaryOES", GLES_IDX, 0x051C);
        reg("glGetProgramBinaryOES", GLES_IDX, 0x051D);
        reg("glTexImage3DOES", GLES_IDX, 0x051E);
        reg("glTexSubImage3DOES", GLES_IDX, 0x051F);
        reg("glCopyTexSubImage3DOES", GLES_IDX, 0x0520);
        reg("glCompressedTexImage3DOES", GLES_IDX, 0x0521);
        reg("glCompressedTexSubImage3DOES", GLES_IDX, 0x0522);
        reg("glFramebufferTexture3DOES", GLES_IDX, 0x0523);
        reg("glBindBufferBaseEXT", GLES_IDX, 0x0524);
        reg("glBindBufferRangeEXT", GLES_IDX, 0x0525);
        reg("glTransformFeedbackVaryingsEXT", GLES_IDX, 0x0526);
        reg("glBeginTransformFeedbackEXT", GLES_IDX, 0x0527);
        reg("glEndTransformFeedbackEXT", GLES_IDX, 0x0528);
        reg("glPauseTransformFeedbackNV", GLES_IDX, 0x0529);
        reg("glResumeTransformFeedbackNV", GLES_IDX, 0x052A);
    }
    
    private void reg(String name, int apiIdx, int funcId) {
        int h = hash(name);
        int idx = h & DISPATCH_TABLE_MASK;
        // Linear probe
        while (dispatchTable[idx] != 0) {
            idx = (idx + 1) & DISPATCH_TABLE_MASK;
        }
        dispatchTable[idx] = ((long) apiIdx << 48) | ((long) (h & 0xFFFF) << 32) | funcId;
    }
    
    private static int hash(String s) {
        int h = 0x811c9dc5;
        for (int i = 0, len = s.length(); i < len; i++) {
            h ^= s.charAt(i);
            h *= 0x01000193;
        }
        return h;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DISPATCH - PRIMARY API
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Dispatch by function name - auto-detects API.
     */
    public Object dispatch(String func, Object... args) {
        int h = hash(func);
        int idx = h & DISPATCH_TABLE_MASK;
        long entry;
        
        // Lookup with linear probe
        while ((entry = dispatchTable[idx]) != 0) {
            if (((int) (entry >> 32) & 0xFFFF) == (h & 0xFFFF)) {
                int apiIdx = (int) (entry >> 48);
                int funcId = (int) entry;
                return invoke(apiIdx, funcId, args);
            }
            idx = (idx + 1) & DISPATCH_TABLE_MASK;
        }
        
        // Fallback: infer from prefix
        int apiIdx = inferApi(func);
        return invoke(apiIdx, h, args);
    }
    
    /**
     * Dispatch with explicit API type.
     */
    public Object dispatch(API api, String func, Object... args) {
        return invoke(api.idx, hash(func), args);
    }
    
    /**
     * Dispatch with explicit API type and function ID (fastest).
     */
    public Object dispatch(API api, int funcId, Object... args) {
        return invoke(api.idx, funcId, args);
    }
    
    /**
     * Dispatch with raw indices (maximum performance).
     */
    public Object dispatch(int apiIdx, int funcId, Object... args) {
        return invoke(apiIdx, funcId, args);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DIRECT MAPPER ACCESS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public Object toOpenGL(int funcId, Object... args) {
        return lambdaPaths[OPENGL_IDX].apply(funcId, args);
    }
    
    public Object toGLSL(int funcId, Object... args) {
        return lambdaPaths[GLSL_IDX].apply(funcId, args);
    }
    
    public Object toVulkan(int funcId, Object... args) {
        return lambdaPaths[VULKAN_IDX].apply(funcId, args);
    }
    
    public Object toSPIRV(int funcId, Object... args) {
        return lambdaPaths[SPIRV_IDX].apply(funcId, args);
    }
    
    public Object toMetal(int funcId, Object... args) {
        return lambdaPaths[METAL_IDX].apply(funcId, args);
    }
    
    public Object toGLES(int funcId, Object... args) {
        return lambdaPaths[GLES_IDX].apply(funcId, args);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TYPED MAPPER GETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public OpenGLCallMapper openGL()  { return openGLMapper; }
    public GLSLCallMapper glsl()      { return glslMapper; }
    public VulkanCallMapper vulkan()  { return vulkanMapper; }
    public SPIRVCallMapper spirv()    { return spirvMapper; }
    public MetalCallMapper metal()    { return metalMapper; }
    public GLESCallMapper gles()      { return glesMapper; }

    // ═══════════════════════════════════════════════════════════════════════════
    // INTERNAL - CORE INVOKE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private Object invoke(int apiIdx, int funcId, Object[] args) {
        // Bounds safety: branchless mask
        int safeIdx = apiIdx & (MAPPER_COUNT - 1);
        
        // Sequence for ordering (optional memory fence)
        SEQUENCE_HANDLE.getAndAddAcquire(this, 1L);
        
        // Lambda path is fastest (JIT inlines)
        BiFunction<Integer, Object[], Object> path = lambdaPaths[safeIdx];
        if (path != null) {
            return path.apply(funcId, args);
        }
        
        // MethodHandle fallback
        MethodHandle mh = dispatchHandles[safeIdx];
        if (mh != null) {
            try {
                return mh.invokeExact(funcId, args);
            } catch (Throwable t) {
                throw wrap(t);
            }
        }
        
        // Direct invocation fallback
        return switch (safeIdx) {
            case OPENGL_IDX -> openGLMapper.dispatch(funcId, args);
            case GLSL_IDX -> glslMapper.dispatch(funcId, args);
            case VULKAN_IDX -> vulkanMapper.dispatch(funcId, args);
            case SPIRV_IDX -> spirvMapper.dispatch(funcId, args);
            case METAL_IDX -> metalMapper.dispatch(funcId, args);
            case GLES_IDX -> glesMapper.dispatch(funcId, args);
            default -> throw new IllegalArgumentException("Unknown API index: " + apiIdx);
        };
    }
    
    private static int inferApi(String func) {
        if (func.startsWith("vk")) return VULKAN_IDX;
        if (func.startsWith("mtl")) return METAL_IDX;
        if (func.startsWith("gles")) return GLES_IDX;
        if (func.startsWith("spirv") || func.startsWith("spv")) return SPIRV_IDX;
        if (func.contains("Shader") || func.contains("Program") || 
            func.contains("Uniform") || func.contains("Attrib")) return GLSL_IDX;
        return OPENGL_IDX;
    }
    
    private static RuntimeException wrap(Throwable t) {
        if (t instanceof RuntimeException re) return re;
        if (t instanceof Error e) throw e;
        return new RuntimeException(t);
    }
}
