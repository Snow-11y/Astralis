package stellar.snow.astralis.engine.render.pipeline;
import org.lwjgl.system.MemoryUtil;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicLong;
/**
 * RenderState - High-performance GL state tracking with dirty flags.
 *
 * <h2>Memory Layout (256 bytes, cache-aligned):</h2>
 * <pre>
 * [0-7]     depthTest: enabled(1), func(4), write(1), _pad(2)
 * [8-15]    blend: enabled(1), srcRGB(4), dstRGB(4), srcA(4), dstA(4), eqRGB(2), eqA(2)
 * [16-23]   cull: enabled(1), face(4), frontFace(4), _pad(7)
 * [24-31]   alpha: enabled(1), func(4), ref(4), _pad(7)
 * [32-39]   scissor: enabled(1), x(2), y(2), w(2), h(2)
 * [40-47]   stencil: enabled(1), func(4), ref(4), mask(4), _pad(3)
 * [48-55]   polygon: mode(4), offsetFill(1), factor(4), units(4), _pad(3)
 * [56-63]   fog: enabled(1), mode(4), density(4), start(4), end(4), _pad(3)
 * [64-71]   viewport: x(2), y(2), w(2), h(2)
 * [72-79]   clearColor: r(4), g(4), b(4), a(4)
 * [80-87]   clearDepth: depth(8)
 * [88-95]   colorMask: r(1), g(1), b(1), a(1), _pad(4)
 * [96-127]  textureUnits: unit0-7 bindings (4 bytes each)
 * [128-159] boundBuffers: array(4), element(4), uniform(4), ssbo(4), _pad(16)
 * [160-167] activeProgram: handle(4), _pad(4)
 * [168-175] lineWidth: width(4), _pad(4)
 * [176-183] pointSize: size(4), _pad(4)
 * [184-191] capabilities: bitfield(8)
 * [192-199] dirtyFlags: bitfield(8)
 * [200-207] frameCounter: count(8)
 * [208-255] reserved
 * </pre>
 */
    // ========================================================================
    // CONSTANTS
    // ========================================================================
    public static final int STATE_SIZE = 256;
    public static final int MAX_TEXTURE_UNITS = 8;
    // Offset constants
    public static final long OFF_DEPTH = 0L;
    public static final long OFF_BLEND = 8L;
    public static final long OFF_CULL = 16L;
    public static final long OFF_ALPHA = 24L;
    public static final long OFF_SCISSOR = 32L;
    public static final long OFF_STENCIL = 40L;
    public static final long OFF_POLYGON = 48L;
    public static final long OFF_FOG = 56L;
    public static final long OFF_VIEWPORT = 64L;
    public static final long OFF_CLEAR_COLOR = 72L;
    public static final long OFF_CLEAR_DEPTH = 80L;
    public static final long OFF_COLOR_MASK = 88L;
    public static final long OFF_TEXTURE_UNITS = 96L;
    public static final long OFF_BOUND_BUFFERS = 128L;
    public static final long OFF_ACTIVE_PROGRAM = 160L;
    public static final long OFF_LINE_WIDTH = 168L;
    public static final long OFF_POINT_SIZE = 176L;
    public static final long OFF_CAPABILITIES = 184L;
    public static final long OFF_DIRTY_FLAGS = 192L;
    public static final long OFF_FRAME_COUNTER = 200L;
    // Dirty flag bits
    public static final long DIRTY_DEPTH = 1L << 0;
    public static final long DIRTY_BLEND = 1L << 1;
    public static final long DIRTY_CULL = 1L << 2;
    public static final long DIRTY_ALPHA = 1L << 3;
    public static final long DIRTY_SCISSOR = 1L << 4;
    public static final long DIRTY_STENCIL = 1L << 5;
    public static final long DIRTY_POLYGON = 1L << 6;
    public static final long DIRTY_FOG = 1L << 7;
    public static final long DIRTY_VIEWPORT = 1L << 8;
    public static final long DIRTY_CLEAR_COLOR = 1L << 9;
    public static final long DIRTY_COLOR_MASK = 1L << 10;
    public static final long DIRTY_TEXTURES = 1L << 11;
    public static final long DIRTY_BUFFERS = 1L << 12;
    public static final long DIRTY_PROGRAM = 1L << 13;
    public static final long DIRTY_LINE_WIDTH = 1L << 14;
    public static final long DIRTY_POINT_SIZE = 1L << 15;
    public static final long DIRTY_ALL = 0xFFFFL;
    // Capability bits
    public static final long CAP_DEPTH_TEST = 1L << 0;
    public static final long CAP_BLEND = 1L << 1;
    public static final long CAP_CULL_FACE = 1L << 2;
    public static final long CAP_ALPHA_TEST = 1L << 3;
    public static final long CAP_SCISSOR_TEST = 1L << 4;
    public static final long CAP_STENCIL_TEST = 1L << 5;
    public static final long CAP_POLYGON_OFFSET = 1L << 6;
    public static final long CAP_FOG = 1L << 7;
    public static final long CAP_LIGHTING = 1L << 8;
    public static final long CAP_TEXTURE_2D = 1L << 9;
    public static final long CAP_MULTISAMPLE = 1L << 10;
    public static final long CAP_COLOR_LOGIC_OP = 1L << 11;
    // ========================================================================
    // STATE MEMORY
    // ========================================================================
    private final Arena arena;
    private final MemorySegment stateMemory;
    private final long stateAddress;
    // VarHandles for atomic operations
    private static final VarHandle DIRTY_FLAGS_HANDLE;
    private static final VarHandle CAPABILITIES_HANDLE;
    private static final VarHandle FRAME_COUNTER_HANDLE;
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            DIRTY_FLAGS_HANDLE = lookup.findVarHandle(RenderState.class, "dirtyFlagsCache", long.class);
            CAPABILITIES_HANDLE = lookup.findVarHandle(RenderState.class, "capabilitiesCache", long.class);
            FRAME_COUNTER_HANDLE = lookup.findVarHandle(RenderState.class, "frameCounterCache", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    // Cached values for VarHandle access
    @SuppressWarnings("FieldMayBeFinal")
    private volatile long dirtyFlagsCache = 0L;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile long capabilitiesCache = 0L;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile long frameCounterCache = 0L;
    // Active texture unit
    private int activeTextureUnit = 0;
    // ========================================================================
    // CONSTRUCTION
    // ========================================================================
    public RenderState() {
        this.arena = Arena.ofShared();
        this.stateMemory = arena.allocate(STATE_SIZE, 64); // Cache-line aligned
        this.stateAddress = stateMemory.address();
        // Zero-initialize
        stateMemory.fill((byte) 0);
        // Set defaults
        initializeDefaults();
    }
    private void initializeDefaults() {
        // Depth: enabled, LESS, write=true
        setDepthTest(true, RenderConstants.GL_LESS, true);
        // Blend: disabled, SRC_ALPHA, ONE_MINUS_SRC_ALPHA
        setBlend(false, RenderConstants.GL_SRC_ALPHA, RenderConstants.GL_ONE_MINUS_SRC_ALPHA,
                RenderConstants.GL_SRC_ALPHA, RenderConstants.GL_ONE_MINUS_SRC_ALPHA);
        // Cull: enabled, BACK, CCW
        setCullFace(true, RenderConstants.GL_BACK, RenderConstants.GL_CCW);
        // Viewport: full (0,0,1,1 as placeholder)
        setViewport(0, 0, 1, 1);
        // Clear color: black
        setClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        // Color mask: all enabled
        setColorMask(true, true, true, true);
        // Line width and point size
        setLineWidth(1.0f);
        setPointSize(1.0f);
        // Mark all as dirty for first sync
        markAllDirty();
    }
    // ========================================================================
    // DEPTH STATE
    // ========================================================================
    public void setDepthTest(boolean enabled, int func, boolean write) {
        byte enabledByte = (byte) (enabled ? 1 : 0);
        byte writeByte = (byte) (write ? 1 : 0);
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_DEPTH, enabledByte);
        stateMemory.set(ValueLayout.JAVA_INT, OFF_DEPTH + 1, func);
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_DEPTH + 5, writeByte);
        updateCapability(CAP_DEPTH_TEST, enabled);
        markDirty(DIRTY_DEPTH);
    }
    public void enableDepthTest(boolean enabled) {
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_DEPTH, (byte) (enabled ? 1 : 0));
        updateCapability(CAP_DEPTH_TEST, enabled);
        markDirty(DIRTY_DEPTH);
    }
    public void setDepthFunc(int func) {
        stateMemory.set(ValueLayout.JAVA_INT, OFF_DEPTH + 1, func);
        markDirty(DIRTY_DEPTH);
    }
    public void setDepthMask(boolean write) {
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_DEPTH + 5, (byte) (write ? 1 : 0));
        markDirty(DIRTY_DEPTH);
    }
    public boolean isDepthTestEnabled() {
        return stateMemory.get(ValueLayout.JAVA_BYTE, OFF_DEPTH) != 0;
    }
    public int getDepthFunc() {
        return stateMemory.get(ValueLayout.JAVA_INT, OFF_DEPTH + 1);
    }
    public boolean isDepthWriteEnabled() {
        return stateMemory.get(ValueLayout.JAVA_BYTE, OFF_DEPTH + 5) != 0;
    }
    // ========================================================================
    // BLEND STATE
    // ========================================================================
    public void setBlend(boolean enabled, int srcRGB, int dstRGB, int srcA, int dstA) {
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_BLEND, (byte) (enabled ? 1 : 0));
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_BLEND + 1, (short) srcRGB);
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_BLEND + 3, (short) dstRGB);
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_BLEND + 5, (short) srcA);
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_BLEND + 7, (short) dstA);
        updateCapability(CAP_BLEND, enabled);
        markDirty(DIRTY_BLEND);
    }
    public void enableBlend(boolean enabled) {
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_BLEND, (byte) (enabled ? 1 : 0));
        updateCapability(CAP_BLEND, enabled);
        markDirty(DIRTY_BLEND);
    }
    public void setBlendFunc(int src, int dst) {
        setBlendFuncSeparate(src, dst, src, dst);
    }
    public void setBlendFuncSeparate(int srcRGB, int dstRGB, int srcA, int dstA) {
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_BLEND + 1, (short) srcRGB);
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_BLEND + 3, (short) dstRGB);
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_BLEND + 5, (short) srcA);
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_BLEND + 7, (short) dstA);
        markDirty(DIRTY_BLEND);
    }
    public boolean isBlendEnabled() {
        return stateMemory.get(ValueLayout.JAVA_BYTE, OFF_BLEND) != 0;
    }
    public int getBlendSrcRGB() {
        return stateMemory.get(ValueLayout.JAVA_SHORT, OFF_BLEND + 1) & 0xFFFF;
    }
    public int getBlendDstRGB() {
        return stateMemory.get(ValueLayout.JAVA_SHORT, OFF_BLEND + 3) & 0xFFFF;
    }
    public int getBlendSrcAlpha() {
        return stateMemory.get(ValueLayout.JAVA_SHORT, OFF_BLEND + 5) & 0xFFFF;
    }
    public int getBlendDstAlpha() {
        return stateMemory.get(ValueLayout.JAVA_SHORT, OFF_BLEND + 7) & 0xFFFF;
    }
    // ========================================================================
    // CULL STATE
    // ========================================================================
    public void setCullFace(boolean enabled, int face, int frontFace) {
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_CULL, (byte) (enabled ? 1 : 0));
        stateMemory.set(ValueLayout.JAVA_INT, OFF_CULL + 1, face);
        stateMemory.set(ValueLayout.JAVA_INT, OFF_CULL + 5, frontFace);
        updateCapability(CAP_CULL_FACE, enabled);
        markDirty(DIRTY_CULL);
    }
    public void enableCullFace(boolean enabled) {
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_CULL, (byte) (enabled ? 1 : 0));
        updateCapability(CAP_CULL_FACE, enabled);
        markDirty(DIRTY_CULL);
    }
    public void setCullFaceMode(int face) {
        stateMemory.set(ValueLayout.JAVA_INT, OFF_CULL + 1, face);
        markDirty(DIRTY_CULL);
    }
    public boolean isCullFaceEnabled() {
        return stateMemory.get(ValueLayout.JAVA_BYTE, OFF_CULL) != 0;
    }
    public int getCullFace() {
        return stateMemory.get(ValueLayout.JAVA_INT, OFF_CULL + 1);
    }
    public int getFrontFace() {
        return stateMemory.get(ValueLayout.JAVA_INT, OFF_CULL + 5);
    }
    // ========================================================================
    // ALPHA TEST STATE (Legacy, emulated via shader)
    // ========================================================================
    public void setAlphaTest(boolean enabled, int func, float ref) {
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_ALPHA, (byte) (enabled ? 1 : 0));
        stateMemory.set(ValueLayout.JAVA_INT, OFF_ALPHA + 1, func);
        stateMemory.set(ValueLayout.JAVA_FLOAT, OFF_ALPHA + 5, ref);
        updateCapability(CAP_ALPHA_TEST, enabled);
        markDirty(DIRTY_ALPHA);
    }
    public void enableAlphaTest(boolean enabled) {
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_ALPHA, (byte) (enabled ? 1 : 0));
        updateCapability(CAP_ALPHA_TEST, enabled);
        markDirty(DIRTY_ALPHA);
    }
    public boolean isAlphaTestEnabled() {
        return stateMemory.get(ValueLayout.JAVA_BYTE, OFF_ALPHA) != 0;
    }
    public int getAlphaFunc() {
        return stateMemory.get(ValueLayout.JAVA_INT, OFF_ALPHA + 1);
    }
    public float getAlphaRef() {
        return stateMemory.get(ValueLayout.JAVA_FLOAT, OFF_ALPHA + 5);
    }
    // ========================================================================
    // SCISSOR STATE
    // ========================================================================
    public void setScissor(boolean enabled, int x, int y, int width, int height) {
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_SCISSOR, (byte) (enabled ? 1 : 0));
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_SCISSOR + 1, (short) x);
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_SCISSOR + 3, (short) y);
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_SCISSOR + 5, (short) width);
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_SCISSOR + 7, (short) height);
        updateCapability(CAP_SCISSOR_TEST, enabled);
        markDirty(DIRTY_SCISSOR);
    }
    public void enableScissor(boolean enabled) {
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_SCISSOR, (byte) (enabled ? 1 : 0));
        updateCapability(CAP_SCISSOR_TEST, enabled);
        markDirty(DIRTY_SCISSOR);
    }
    // ========================================================================
    // VIEWPORT STATE
    // ========================================================================
    public void setViewport(int x, int y, int width, int height) {
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_VIEWPORT, (short) x);
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_VIEWPORT + 2, (short) y);
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_VIEWPORT + 4, (short) width);
        stateMemory.set(ValueLayout.JAVA_SHORT, OFF_VIEWPORT + 6, (short) height);
        markDirty(DIRTY_VIEWPORT);
    }
    public int getViewportX() {
        return stateMemory.get(ValueLayout.JAVA_SHORT, OFF_VIEWPORT) & 0xFFFF;
    }
    public int getViewportY() {
        return stateMemory.get(ValueLayout.JAVA_SHORT, OFF_VIEWPORT + 2) & 0xFFFF;
    }
    public int getViewportWidth() {
        return stateMemory.get(ValueLayout.JAVA_SHORT, OFF_VIEWPORT + 4) & 0xFFFF;
    }
    public int getViewportHeight() {
        return stateMemory.get(ValueLayout.JAVA_SHORT, OFF_VIEWPORT + 6) & 0xFFFF;
    }
    // ========================================================================
    // CLEAR STATE
    // ========================================================================
    public void setClearColor(float r, float g, float b, float a) {
        stateMemory.set(ValueLayout.JAVA_FLOAT, OFF_CLEAR_COLOR, r);
        stateMemory.set(ValueLayout.JAVA_FLOAT, OFF_CLEAR_COLOR + 4, g);
        stateMemory.set(ValueLayout.JAVA_FLOAT, OFF_CLEAR_COLOR + 8, b);
        stateMemory.set(ValueLayout.JAVA_FLOAT, OFF_CLEAR_COLOR + 12, a);
        markDirty(DIRTY_CLEAR_COLOR);
    }
    public void setClearDepth(double depth) {
        stateMemory.set(ValueLayout.JAVA_DOUBLE, OFF_CLEAR_DEPTH, depth);
    }
    // ========================================================================
    // COLOR MASK
    // ========================================================================
    public void setColorMask(boolean r, boolean g, boolean b, boolean a) {
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_COLOR_MASK, (byte) (r ? 1 : 0));
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_COLOR_MASK + 1, (byte) (g ? 1 : 0));
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_COLOR_MASK + 2, (byte) (b ? 1 : 0));
        stateMemory.set(ValueLayout.JAVA_BYTE, OFF_COLOR_MASK + 3, (byte) (a ? 1 : 0));
        markDirty(DIRTY_COLOR_MASK);
    }
    // ========================================================================
    // TEXTURE BINDINGS
    // ========================================================================
    public void setActiveTextureUnit(int unit) {
        this.activeTextureUnit = Math.min(unit, MAX_TEXTURE_UNITS - 1);
    }
    public int getActiveTextureUnit() {
        return activeTextureUnit;
    }
    public void bindTexture(int unit, int textureId) {
        if (unit >= 0 && unit < MAX_TEXTURE_UNITS) {
            stateMemory.set(ValueLayout.JAVA_INT, OFF_TEXTURE_UNITS + (long) unit * 4, textureId);
            markDirty(DIRTY_TEXTURES);
        }
    }
    public void bindTexture(int textureId) {
        bindTexture(activeTextureUnit, textureId);
    }
    public int getBoundTexture(int unit) {
        if (unit >= 0 && unit < MAX_TEXTURE_UNITS) {
            return stateMemory.get(ValueLayout.JAVA_INT, OFF_TEXTURE_UNITS + (long) unit * 4);
        }
        return 0;
    }
    // ========================================================================
    // BUFFER BINDINGS
    // ========================================================================
    public void bindArrayBuffer(int bufferId) {
        stateMemory.set(ValueLayout.JAVA_INT, OFF_BOUND_BUFFERS, bufferId);
        markDirty(DIRTY_BUFFERS);
    }
    public void bindElementBuffer(int bufferId) {
        stateMemory.set(ValueLayout.JAVA_INT, OFF_BOUND_BUFFERS + 4, bufferId);
        markDirty(DIRTY_BUFFERS);
    }
    public int getBoundArrayBuffer() {
        return stateMemory.get(ValueLayout.JAVA_INT, OFF_BOUND_BUFFERS);
    }
    public int getBoundElementBuffer() {
        return stateMemory.get(ValueLayout.JAVA_INT, OFF_BOUND_BUFFERS + 4);
    }
    // ========================================================================
    // PROGRAM BINDING
    // ========================================================================
    public void useProgram(int programId) {
        stateMemory.set(ValueLayout.JAVA_INT, OFF_ACTIVE_PROGRAM, programId);
        markDirty(DIRTY_PROGRAM);
    }
    public int getActiveProgram() {
        return stateMemory.get(ValueLayout.JAVA_INT, OFF_ACTIVE_PROGRAM);
    }
    // ========================================================================
    // LINE/POINT SIZE
    // ========================================================================
    public void setLineWidth(float width) {
        stateMemory.set(ValueLayout.JAVA_FLOAT, OFF_LINE_WIDTH, width);
        markDirty(DIRTY_LINE_WIDTH);
    }
    public float getLineWidth() {
        return stateMemory.get(ValueLayout.JAVA_FLOAT, OFF_LINE_WIDTH);
    }
    public void setPointSize(float size) {
        stateMemory.set(ValueLayout.JAVA_FLOAT, OFF_POINT_SIZE, size);
        markDirty(DIRTY_POINT_SIZE);
    }
    public float getPointSize() {
        return stateMemory.get(ValueLayout.JAVA_FLOAT, OFF_POINT_SIZE);
    }
    // ========================================================================
    // CAPABILITY MANAGEMENT
    // ========================================================================
    private void updateCapability(long cap, boolean enabled) {
        long current = (long) CAPABILITIES_HANDLE.get(this);
        if (enabled) {
            CAPABILITIES_HANDLE.set(this, current | cap);
        } else {
            CAPABILITIES_HANDLE.set(this, current & ~cap);
        }
        stateMemory.set(ValueLayout.JAVA_LONG, OFF_CAPABILITIES, (long) CAPABILITIES_HANDLE.get(this));
    }
    public boolean hasCapability(long cap) {
        return ((long) CAPABILITIES_HANDLE.get(this) & cap) != 0;
    }
    public long getCapabilities() {
        return (long) CAPABILITIES_HANDLE.get(this);
    }
    // ========================================================================
    // DIRTY FLAG MANAGEMENT
    // ========================================================================
    public void markDirty(long flag) {
        long current = (long) DIRTY_FLAGS_HANDLE.get(this);
        DIRTY_FLAGS_HANDLE.set(this, current | flag);
        stateMemory.set(ValueLayout.JAVA_LONG, OFF_DIRTY_FLAGS, (long) DIRTY_FLAGS_HANDLE.get(this));
    }
    public void markAllDirty() {
        DIRTY_FLAGS_HANDLE.set(this, DIRTY_ALL);
        stateMemory.set(ValueLayout.JAVA_LONG, OFF_DIRTY_FLAGS, DIRTY_ALL);
    }
    public void clearDirty(long flag) {
        long current = (long) DIRTY_FLAGS_HANDLE.get(this);
        DIRTY_FLAGS_HANDLE.set(this, current & ~flag);
        stateMemory.set(ValueLayout.JAVA_LONG, OFF_DIRTY_FLAGS, (long) DIRTY_FLAGS_HANDLE.get(this));
    }
    public void clearAllDirty() {
        DIRTY_FLAGS_HANDLE.set(this, 0L);
        stateMemory.set(ValueLayout.JAVA_LONG, OFF_DIRTY_FLAGS, 0L);
    }
    public boolean isDirty(long flag) {
        return ((long) DIRTY_FLAGS_HANDLE.get(this) & flag) != 0;
    }
    public long getDirtyFlags() {
        return (long) DIRTY_FLAGS_HANDLE.get(this);
    }
    public long getAndClearDirtyFlags() {
        long flags = (long) DIRTY_FLAGS_HANDLE.getAndSet(this, 0L);
        stateMemory.set(ValueLayout.JAVA_LONG, OFF_DIRTY_FLAGS, 0L);
        return flags;
    }
    // ========================================================================
    // FRAME MANAGEMENT
    // ========================================================================
    public void nextFrame() {
        long count = (long) FRAME_COUNTER_HANDLE.getAndAdd(this, 1L);
        stateMemory.set(ValueLayout.JAVA_LONG, OFF_FRAME_COUNTER, count + 1);
    }
    public long getFrameCount() {
        return (long) FRAME_COUNTER_HANDLE.get(this);
    }
    // ========================================================================
    // RAW MEMORY ACCESS
    // ========================================================================
    public MemorySegment getStateMemory() {
        return stateMemory;
    }
    public long getStateAddress() {
        return stateAddress;
    }
    // ========================================================================
    // CLEANUP
    // ========================================================================
    public void close() {
        if (arena.scope().isAlive()) {
            arena.close();
        }
    }
}
