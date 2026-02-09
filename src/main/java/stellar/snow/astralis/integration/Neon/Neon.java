/*
 * ╔════════════════════════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                                                ║
 * ║     ███╗   ██╗███████╗ ██████╗ ███╗   ██╗    ██████╗ ██╗   ██╗███╗   ██╗ █████╗ ███╗   ███╗   ║
 * ║     ████╗  ██║██╔════╝██╔═══██╗████╗  ██║    ██╔══██╗╚██╗ ██╔╝████╗  ██║██╔══██╗████╗ ████║   ║
 * ║     ██╔██╗ ██║█████╗  ██║   ██║██╔██╗ ██║    ██║  ██║ ╚████╔╝ ██╔██╗ ██║███████║██╔████╔██║   ║
 * ║     ██║╚██╗██║██╔══╝  ██║   ██║██║╚██╗██║    ██║  ██║  ╚██╔╝  ██║╚██╗██║██╔══██║██║╚██╔╝██║   ║
 * ║     ██║ ╚████║███████╗╚██████╔╝██║ ╚████║    ██████╔╝   ██║   ██║ ╚████║██║  ██║██║ ╚═╝ ██║   ║
 * ║     ╚═╝  ╚═══╝╚══════╝ ╚═════╝ ╚═╝  ╚═══╝    ╚═════╝    ╚═╝   ╚═╝  ╚═══╝╚═╝  ╚═╝╚═╝     ╚═╝   ║
 * ║                                                                                                ║
 * ║                        Advanced Dynamic Lighting Engine v2.0                                   ║
 * ║                                                                                                ║
 * ║    Features: Native Memory • Spatial Hashing • Temporal Smoothing • Lumen Integration         ║
 * ║              Batch Processing • Distance LOD • Light Interpolation • Occlusion Hints          ║
 * ║                                                                                                ║
 * ╚════════════════════════════════════════════════════════════════════════════════════════════════╝
 *
 * Single-file implementation for Minecraft 1.12.2 with Forge
 * Requires: Java 21+, Lumen, fermiumbooter
 */

package stellar.snow.astralis.integration.Neon;

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 1: IMPORTS
// ═══════════════════════════════════════════════════════════════════════════════

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntitySpectralArrow;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.fonnymunkey.foniumbooter.api.FermiumRegistryAPI;
import stellar.snow.astralis.integration.Lumen.Lumen;

import java.io.InputStreamReader;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.*;

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 2: MAIN MOD CLASS
// ═══════════════════════════════════════════════════════════════════════════════

public final class Neon {
    
    public static final String MOD_ID = "neon";
    public static final String VERSION = "2.0.0";
    public static final Logger LOG = LogManager.getLogger("Neon");
    
    @Mod.Instance(MOD_ID)
    public static Neon INSTANCE;
    
    // ════════════════════════════════════════════════════════════════════════
    // Core Engine Components
    // ════════════════════════════════════════════════════════════════════════
    
    private NativeLightEngine engine;
    private SpatialLightIndex spatialIndex;
    private TemporalSmoother smoother;
    private LightUpdateScheduler scheduler;
    private LumenBridge lumenBridge;
    private NeonMetrics metrics;
    
    // ════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ════════════════════════════════════════════════════════════════════════
    
    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        if (!event.getSide().isClient()) return;
        
        MinecraftForge.EVENT_BUS.register(new NeonEventHandler());
        
        IReloadableResourceManager rm = (IReloadableResourceManager) 
            Minecraft.getMinecraft().getResourceManager();
        rm.registerReloadListener(ItemLightRegistry::reload);
        
        LOG.info("Neon pre-initialization complete");
    }
    
    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        if (!event.getSide().isClient()) return;
        
        // Initialize components
        this.metrics = new NeonMetrics();
        this.engine = new NativeLightEngine(NeonConfig.maxLightSources, metrics);
        this.spatialIndex = new SpatialLightIndex();
        this.smoother = new TemporalSmoother();
        this.scheduler = new LightUpdateScheduler();
        this.lumenBridge = new LumenBridge(engine);
        
        // Register default handlers
        LightHandlerRegistry.registerDefaults();
        
        LOG.info("Neon initialized - max sources: {}, mode: {}", 
            NeonConfig.maxLightSources, NeonConfig.mode);
    }
    
    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent event) {
        if (!event.getSide().isClient()) return;
        
        if (NeonConfig.lumenIntegration) {
            lumenBridge.initialize();
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Public API
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * Main update loop - called each frame before rendering
     */
    public void updateAll(@NotNull RenderGlobal renderer) {
        if (!NeonConfig.enabled || !NeonConfig.mode.isEnabled()) {
            return;
        }
        
        long startNs = System.nanoTime();
        
        int updated = engine.updateAll(renderer, spatialIndex, smoother, scheduler);
        
        if (NeonConfig.lumenIntegration && lumenBridge.isReady()) {
            lumenBridge.pushUpdates();
        }
        
        metrics.recordUpdate(updated, System.nanoTime() - startNs);
    }
    
    /**
     * Get dynamic light contribution at a block position
     */
    public double getDynamicLightLevel(@NotNull BlockPos pos) {
        if (!NeonConfig.mode.isEnabled()) return 0.0;
        return spatialIndex.queryMaxLight(pos.getX(), pos.getY(), pos.getZ());
    }
    
    /**
     * Apply dynamic lighting to a lightmap value
     */
    public int getLightmapWithDynamicLight(@NotNull BlockPos pos, int lightmap) {
        double dynamic = getDynamicLightLevel(pos);
        return applyDynamicToLightmap(dynamic, lightmap);
    }
    
    /**
     * Apply dynamic lighting for an entity
     */
    public int getLightmapWithDynamicLight(@NotNull Entity entity, int lightmap) {
        double posLight = getDynamicLightLevel(entity.getPosition());
        
        int entityLight = 0;
        if (entity instanceof DynamicLightSource source) {
            entityLight = source.neon$getLuminance();
        }
        
        return applyDynamicToLightmap(Math.max(posLight, entityLight), lightmap);
    }
    
    private int applyDynamicToLightmap(double dynamicLevel, int lightmap) {
        if (dynamicLevel > 0.0) {
            int blockLight = (lightmap >> 4) & 0xF;
            int skyLight = (lightmap >> 20) & 0xF;
            
            int dynamicInt = (int) Math.ceil(dynamicLevel);
            if (dynamicInt > blockLight) {
                blockLight = Math.min(15, dynamicInt);
            }
            
            return (skyLight << 20) | (blockLight << 4);
        }
        return lightmap;
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Light Source Management
    // ════════════════════════════════════════════════════════════════════════
    
    public void addLightSource(@NotNull DynamicLightSource source) {
        World world = source.neon$getWorld();
        if (world == null || !world.isRemote) return;
        if (!NeonConfig.mode.isEnabled()) return;
        if (engine.contains(source)) return;
        
        engine.addSource(source);
        spatialIndex.insert(source);
        metrics.incrementSources();
    }
    
    public void removeLightSource(@NotNull DynamicLightSource source) {
        if (!engine.contains(source)) return;
        
        engine.removeSource(source);
        spatialIndex.remove(source);
        source.neon$scheduleChunkRebuilds(Minecraft.getMinecraft().renderGlobal);
        metrics.decrementSources();
    }
    
    public boolean containsLightSource(@NotNull DynamicLightSource source) {
        return engine.contains(source);
    }
    
    public void clearAllLightSources() {
        engine.forEachSource(source -> {
            source.neon$reset();
            source.neon$scheduleChunkRebuilds(Minecraft.getMinecraft().renderGlobal);
        });
        engine.clear();
        spatialIndex.clear();
        metrics.resetSourceCount();
    }
    
    public void removeLightSourcesIf(@NotNull Predicate<DynamicLightSource> predicate) {
        RenderGlobal renderer = Minecraft.getMinecraft().renderGlobal;
        engine.removeIf(predicate, source -> {
            spatialIndex.remove(source);
            source.neon$reset();
            source.neon$scheduleChunkRebuilds(renderer);
            metrics.decrementSources();
        });
    }
    
    /**
     * Update tracking state for a light source based on its luminance
     */
    public static void updateTracking(@NotNull DynamicLightSource source) {
        boolean isTracked = source.neon$isEnabled();
        int luminance = source.neon$getLuminance();
        
        if (!isTracked && luminance > 0) {
            source.neon$setEnabled(true);
        } else if (isTracked && luminance <= 0) {
            source.neon$setEnabled(false);
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Chunk Rebuild Scheduling
    // ════════════════════════════════════════════════════════════════════════
    
    public static void scheduleChunkRebuild(@NotNull RenderGlobal renderer, @NotNull BlockPos pos) {
        scheduleChunkRebuild(renderer, pos.getX(), pos.getY(), pos.getZ());
    }
    
    public static void scheduleChunkRebuild(@NotNull RenderGlobal renderer, long packedPos) {
        int x = unpackX(packedPos);
        int y = unpackY(packedPos);
        int z = unpackZ(packedPos);
        scheduleChunkRebuild(renderer, x, y, z);
    }
    
    public static void scheduleChunkRebuild(@NotNull RenderGlobal renderer, int chunkX, int chunkY, int chunkZ) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return;
        
        int minX = chunkX << 4;
        int minY = chunkY << 4;
        int minZ = chunkZ << 4;
        
        ((WorldRendererAccessor) renderer).neon$markBlockRangeForRenderUpdate(
            minX, minY, minZ,
            minX + 15, minY + 15, minZ + 15,
            false
        );
    }
    
    public static void updateTrackedChunks(
        @NotNull BlockPos chunkPos,
        @Nullable LongOpenHashSet oldChunks,
        @Nullable LongOpenHashSet newChunks
    ) {
        long packed = packChunkPos(chunkPos.getX(), chunkPos.getY(), chunkPos.getZ());
        if (oldChunks != null) oldChunks.remove(packed);
        if (newChunks != null) newChunks.add(packed);
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Position Packing Utilities
    // ════════════════════════════════════════════════════════════════════════
    
    public static long packChunkPos(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 |
               ((long) y & 0xFFF) << 26 |
               ((long) z & 0x3FFFFFF);
    }
    
    public static int unpackX(long packed) {
        return (int) (packed >> 38);
    }
    
    public static int unpackY(long packed) {
        return (int) ((packed >> 26) & 0xFFF);
    }
    
    public static int unpackZ(long packed) {
        return (int) (packed << 38 >> 38);
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Utility Methods
    // ════════════════════════════════════════════════════════════════════════
    
    public static boolean isSubmergedInFluid(@NotNull EntityLivingBase entity) {
        if (!NeonConfig.waterSensitivity) return false;
        return FluidUtil.isEntitySubmerged(entity);
    }
    
    public static int getLuminanceFromItemStack(@NotNull ItemStack stack, boolean submerged) {
        int luminance = ItemLightRegistry.getLuminance(stack, submerged);
        return Math.min(14, luminance); // Cap at 14 to distinguish from full blocks
    }
    
    public static int getLivingEntityItemLuminance(@NotNull EntityLivingBase entity) {
        boolean submerged = isSubmergedInFluid(entity);
        int maxLuminance = 0;
        
        // Check held items
        for (ItemStack stack : entity.getHeldEquipment()) {
            maxLuminance = Math.max(maxLuminance, getLuminanceFromItemStack(stack, submerged));
        }
        
        // Check armor
        for (ItemStack stack : entity.getArmorInventoryList()) {
            maxLuminance = Math.max(maxLuminance, getLuminanceFromItemStack(stack, submerged));
        }
        
        return maxLuminance;
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Accessors
    // ════════════════════════════════════════════════════════════════════════
    
    public static Neon get() { return INSTANCE; }
    public NativeLightEngine getEngine() { return engine; }
    public SpatialLightIndex getSpatialIndex() { return spatialIndex; }
    public TemporalSmoother getSmoother() { return smoother; }
    public LumenBridge getLumenBridge() { return lumenBridge; }
    public NeonMetrics getMetrics() { return metrics; }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 3: CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════════

@Config(modid = Neon.MOD_ID, name = "neon/neon")
@Config.LangKey("config.neon.title")
final class NeonConfig {
    
    @Config.Comment("Enable Neon dynamic lighting system")
    public static boolean enabled = true;
    
    @Config.Comment("Dynamic lighting update mode")
    public static DynamicLightsMode mode = DynamicLightsMode.REALTIME;
    
    @Config.Comment("Maximum simultaneous light sources")
    @Config.RangeInt(min = 64, max = 8192)
    public static int maxLightSources = 1024;
    
    @Config.Comment("Enable light sources from entities")
    public static boolean entitiesEnabled = true;
    
    @Config.Comment("Enable player's own light source (self-lighting)")
    public static boolean selfLightEnabled = true;
    
    @Config.Comment("Enable light sources from tile entities")
    public static boolean tileEntitiesEnabled = true;
    
    @Config.Comment("Enable water sensitivity (water-sensitive lights turn off in water)")
    public static boolean waterSensitivity = true;
    
    @Config.Comment("Creeper explosion lighting mode")
    public static ExplosiveLightMode creeperLightMode = ExplosiveLightMode.FANCY;
    
    @Config.Comment("TNT explosion lighting mode")
    public static ExplosiveLightMode tntLightMode = ExplosiveLightMode.FANCY;
    
    @Config.Comment("Enable smooth light transitions (reduces flickering)")
    public static boolean smoothLighting = true;
    
    @Config.Comment("Smoothing interpolation factor (higher = faster transitions)")
    @Config.RangeDouble(min = 0.05, max = 1.0)
    public static double smoothingFactor = 0.25;
    
    @Config.Comment("Light falloff type: LINEAR, QUADRATIC, REALISTIC")
    public static LightFalloff falloffType = LightFalloff.LINEAR;
    
    @Config.Comment("Enable distance-based Level of Detail (reduces updates for far lights)")
    public static boolean distanceLOD = true;
    
    @Config.Comment("Enable deep Lumen rendering engine integration")
    public static boolean lumenIntegration = true;
    
    @Config.Comment("Enable debug metrics overlay (F3 menu)")
    public static boolean debugMetrics = false;
    
    public static void sync() {
        ConfigManager.sync(Neon.MOD_ID, Config.Type.INSTANCE);
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 4: ENUMS
// ═══════════════════════════════════════════════════════════════════════════════

enum DynamicLightsMode {
    OFF(0, false),
    SLOW(500, true),
    FAST(250, true),
    REALTIME(0, true);
    
    private final int delayMs;
    private final boolean enabled;
    
    DynamicLightsMode(int delayMs, boolean enabled) {
        this.delayMs = delayMs;
        this.enabled = enabled;
    }
    
    public boolean isEnabled() { return enabled; }
    public boolean hasDelay() { return delayMs > 0; }
    public int getDelayMs() { return delayMs; }
    
    public DynamicLightsMode cycle() {
        DynamicLightsMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}

enum ExplosiveLightMode {
    OFF(false),
    SIMPLE(true),
    FANCY(true);
    
    private final boolean enabled;
    
    ExplosiveLightMode(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() { return enabled; }
    
    public ExplosiveLightMode cycle() {
        ExplosiveLightMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}

enum LightFalloff {
    LINEAR {
        @Override
        public double calculate(double distance, double maxRadius, int luminance) {
            if (distance >= maxRadius) return 0.0;
            return (1.0 - distance / maxRadius) * luminance;
        }
    },
    QUADRATIC {
        @Override
        public double calculate(double distance, double maxRadius, int luminance) {
            if (distance >= maxRadius) return 0.0;
            double factor = 1.0 - distance / maxRadius;
            return factor * factor * luminance;
        }
    },
    REALISTIC {
        @Override
        public double calculate(double distance, double maxRadius, int luminance) {
            if (distance >= maxRadius || distance < 0.5) return distance < 0.5 ? luminance : 0.0;
            // Inverse square with minimum threshold
            return Math.min(luminance, luminance / (distance * distance));
        }
    };
    
    public abstract double calculate(double distance, double maxRadius, int luminance);
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 5: DYNAMIC LIGHT SOURCE INTERFACE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Interface for objects that can emit dynamic light.
 * Implemented via mixin on Entity and TileEntity.
 */
interface DynamicLightSource {
    
    double neon$getX();
    double neon$getY();
    double neon$getZ();
    
    @Nullable World neon$getWorld();
    
    /**
     * @return Current luminance level (0-15)
     */
    int neon$getLuminance();
    
    /**
     * @return Smoothed luminance for interpolation
     */
    default float neon$getSmoothedLuminance() {
        return neon$getLuminance();
    }
    
    /**
     * Called each tick to recalculate light emission
     */
    void neon$tick();
    
    /**
     * Reset light state
     */
    void neon$reset();
    
    /**
     * @return true if currently registered and emitting light
     */
    boolean neon$isEnabled();
    
    /**
     * Enable or disable this light source
     */
    void neon$setEnabled(boolean enabled);
    
    /**
     * @return true if this light is affected by water/fluid submersion
     */
    default boolean neon$isWaterSensitive() {
        return false;
    }
    
    /**
     * Check if an update should occur this frame based on mode timing
     */
    boolean neon$shouldUpdate();
    
    /**
     * Perform a full update including position tracking and chunk scheduling
     */
    boolean neon$performUpdate(@NotNull RenderGlobal renderer);
    
    /**
     * Schedule rebuilds for all tracked affected chunks
     */
    void neon$scheduleChunkRebuilds(@NotNull RenderGlobal renderer);
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 6: LIGHT HANDLER SYSTEM
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Functional interface for calculating light emission from a source
 */
@FunctionalInterface
interface LightHandler<T> {
    
    int getLuminance(T source);
    
    default boolean isWaterSensitive(T source) {
        return false;
    }
    
    // Factory methods
    static <T> LightHandler<T> of(ToIntFunction<T> luminanceFunc) {
        return luminanceFunc::applyAsInt;
    }
    
    static <T> LightHandler<T> of(ToIntFunction<T> luminanceFunc, Predicate<T> waterSensitive) {
        return new LightHandler<>() {
            @Override public int getLuminance(T source) { return luminanceFunc.applyAsInt(source); }
            @Override public boolean isWaterSensitive(T source) { return waterSensitive.test(source); }
        };
    }
    
    static <T> LightHandler<T> constant(int luminance) {
        return source -> luminance;
    }
    
    static <T> LightHandler<T> constant(int luminance, boolean waterSensitive) {
        return new LightHandler<>() {
            @Override public int getLuminance(T source) { return luminance; }
            @Override public boolean isWaterSensitive(T source) { return waterSensitive; }
        };
    }
}

/**
 * Registry for entity and tile entity light handlers
 */
final class LightHandlerRegistry {
    
    private static final Reference2ObjectMap<Class<?>, LightHandler<?>> ENTITY_HANDLERS =
        new Reference2ObjectOpenHashMap<>();
    private static final Reference2ObjectMap<Class<?>, LightHandler<?>> TILE_ENTITY_HANDLERS =
        new Reference2ObjectOpenHashMap<>();
    
    // Handler cache for subclass lookups
    private static final Reference2ObjectMap<Class<?>, LightHandler<?>> ENTITY_HANDLER_CACHE =
        new Reference2ObjectOpenHashMap<>();
    private static final Reference2ObjectMap<Class<?>, LightHandler<?>> TILE_ENTITY_HANDLER_CACHE =
        new Reference2ObjectOpenHashMap<>();
    
    private LightHandlerRegistry() {}
    
    // ════════════════════════════════════════════════════════════════════════
    // Registration
    // ════════════════════════════════════════════════════════════════════════
    
    @SuppressWarnings("unchecked")
    public static <T extends Entity> void registerEntity(Class<T> clazz, LightHandler<T> handler) {
        LightHandler<?> existing = ENTITY_HANDLERS.get(clazz);
        
        LightHandler<T> wrappedHandler;
        if (existing != null) {
            // Combine handlers - take maximum
            LightHandler<T> existingTyped = (LightHandler<T>) existing;
            wrappedHandler = source -> Math.min(14, Math.max(
                existingTyped.getLuminance(source),
                handler.getLuminance(source)
            ));
        } else {
            // Wrap to clamp at 14
            wrappedHandler = source -> Math.min(14, handler.getLuminance(source));
        }
        
        ENTITY_HANDLERS.put(clazz, wrappedHandler);
        ENTITY_HANDLER_CACHE.clear(); // Invalidate cache
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends TileEntity> void registerTileEntity(Class<T> clazz, LightHandler<T> handler) {
        LightHandler<?> existing = TILE_ENTITY_HANDLERS.get(clazz);
        
        LightHandler<T> wrappedHandler;
        if (existing != null) {
            LightHandler<T> existingTyped = (LightHandler<T>) existing;
            wrappedHandler = source -> Math.min(14, Math.max(
                existingTyped.getLuminance(source),
                handler.getLuminance(source)
            ));
        } else {
            wrappedHandler = source -> Math.min(14, handler.getLuminance(source));
        }
        
        TILE_ENTITY_HANDLERS.put(clazz, wrappedHandler);
        TILE_ENTITY_HANDLER_CACHE.clear();
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Lookup with Caching
    // ════════════════════════════════════════════════════════════════════════
    
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends Entity> LightHandler<T> getEntityHandler(T entity) {
        Class<?> clazz = entity.getClass();
        
        // Check cache first
        LightHandler<?> cached = ENTITY_HANDLER_CACHE.get(clazz);
        if (cached != null) {
            return cached == NullHandler.INSTANCE ? null : (LightHandler<T>) cached;
        }
        
        // Walk up class hierarchy
        LightHandler<?> handler = null;
        Class<?> searchClass = clazz;
        while (searchClass != null && Entity.class.isAssignableFrom(searchClass)) {
            handler = ENTITY_HANDLERS.get(searchClass);
            if (handler != null) break;
            searchClass = searchClass.getSuperclass();
        }
        
        // Cache result (including null as NullHandler)
        ENTITY_HANDLER_CACHE.put(clazz, handler != null ? handler : NullHandler.INSTANCE);
        return (LightHandler<T>) handler;
    }
    
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends TileEntity> LightHandler<T> getTileEntityHandler(T tileEntity) {
        Class<?> clazz = tileEntity.getClass();
        
        LightHandler<?> cached = TILE_ENTITY_HANDLER_CACHE.get(clazz);
        if (cached != null) {
            return cached == NullHandler.INSTANCE ? null : (LightHandler<T>) cached;
        }
        
        LightHandler<?> handler = null;
        Class<?> searchClass = clazz;
        while (searchClass != null && TileEntity.class.isAssignableFrom(searchClass)) {
            handler = TILE_ENTITY_HANDLERS.get(searchClass);
            if (handler != null) break;
            searchClass = searchClass.getSuperclass();
        }
        
        TILE_ENTITY_HANDLER_CACHE.put(clazz, handler != null ? handler : NullHandler.INSTANCE);
        return (LightHandler<T>) handler;
    }
    
    // Sentinel for caching null lookups
    private enum NullHandler implements LightHandler<Object> {
        INSTANCE;
        @Override public int getLuminance(Object source) { return 0; }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Luminance Calculation
    // ════════════════════════════════════════════════════════════════════════
    
    public static <T extends Entity> int getLuminance(T entity) {
        if (!NeonConfig.entitiesEnabled) return 0;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (entity == mc.player && !NeonConfig.selfLightEnabled) {
            return 0;
        }
        
        LightHandler<T> handler = getEntityHandler(entity);
        if (handler == null) return 0;
        
        // Water sensitivity check
        if (handler.isWaterSensitive(entity) && entity.world != null) {
            if (entity.isInWater() || entity.isInLava()) {
                return 0;
            }
        }
        
        return handler.getLuminance(entity);
    }
    
    public static <T extends TileEntity> int getLuminance(T tileEntity) {
        if (!NeonConfig.tileEntitiesEnabled) return 0;
        
        LightHandler<T> handler = getTileEntityHandler(tileEntity);
        if (handler == null) return 0;
        
        if (handler.isWaterSensitive(tileEntity) && tileEntity.getWorld() != null) {
            if (FluidUtil.isFluidAt(tileEntity.getWorld(), tileEntity.getPos())) {
                return 0;
            }
        }
        
        return handler.getLuminance(tileEntity);
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Default Handlers
    // ════════════════════════════════════════════════════════════════════════
    
    public static void registerDefaults() {
        // Blaze - constant warm glow, extinguished by water
        registerEntity(EntityBlaze.class, LightHandler.constant(10, true));
        
        // Magma Cube - varies with squish animation
        registerEntity(EntityMagmaCube.class, 
            cube -> cube.squishFactor > 0.6f ? 11 : 8);
        
        // Spectral Arrow - constant glow
        registerEntity(EntitySpectralArrow.class, LightHandler.constant(8));
        
        // Enderman holding light-emitting block
        registerEntity(EntityEnderman.class, enderman -> {
            IBlockState held = enderman.getHeldBlockState();
            return held != null ? held.getLightValue() : 0;
        });
        
        // Dropped items
        registerEntity(EntityItem.class, item -> 
            ItemLightRegistry.getLuminance(item.getItem(), item.isInWater()));
        
        // Item frames
        registerEntity(EntityItemFrame.class, frame ->
            ItemLightRegistry.getLuminance(frame.getDisplayedItem(), FluidUtil.isEntitySubmerged(frame)));
        
        // Creeper - handled specially in mixin, but register base handler
        registerEntity(EntityCreeper.class, creeper -> {
            if (!NeonConfig.creeperLightMode.isEnabled()) return 0;
            
            float flashIntensity = creeper.getCreeperFlashIntensity(0.0f);
            if (flashIntensity <= 0.001f) return 0;
            
            return switch (NeonConfig.creeperLightMode) {
                case SIMPLE -> 10;
                case FANCY -> (int) (flashIntensity * 10.0f);
                default -> 0;
            };
        });
        
        Neon.LOG.info("Registered {} default entity light handlers", ENTITY_HANDLERS.size());
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 7: NATIVE LIGHT ENGINE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * High-performance native memory light engine.
 * 
 * Uses Foreign Function & Memory API for cache-efficient storage.
 * Each light source entry is 64 bytes aligned for optimal cache performance.
 * 
 * Memory Layout (64 bytes per entry):
 * ┌────────────────────────────────────────────────────────────────┐
 * │ Offset │ Size │ Field                                         │
 * ├────────┼──────┼───────────────────────────────────────────────┤
 * │   0    │  8   │ Position X (double)                           │
 * │   8    │  8   │ Position Y (double)                           │
 * │  16    │  8   │ Position Z (double)                           │
 * │  24    │  4   │ Luminance (int, 0-15)                         │
 * │  28    │  4   │ Smoothed Luminance × 1000 (int, fixed-point)  │
 * │  32    │  4   │ Flags (int: enabled, water-sens, dirty, etc)  │
 * │  36    │  4   │ Last Update Tick (int)                        │
 * │  40    │  8   │ Previous X (double, for interpolation)        │
 * │  48    │  8   │ Previous Y (double)                           │
 * │  56    │  8   │ Previous Z (double)                           │
 * └────────┴──────┴───────────────────────────────────────────────┘
 */
final class NativeLightEngine implements AutoCloseable {
    
    // Entry layout constants
    private static final int ENTRY_SIZE = 64;
    private static final int OFFSET_X = 0;
    private static final int OFFSET_Y = 8;
    private static final int OFFSET_Z = 16;
    private static final int OFFSET_LUMINANCE = 24;
    private static final int OFFSET_SMOOTHED = 28;
    private static final int OFFSET_FLAGS = 32;
    private static final int OFFSET_LAST_TICK = 36;
    private static final int OFFSET_PREV_X = 40;
    private static final int OFFSET_PREV_Y = 48;
    private static final int OFFSET_PREV_Z = 56;
    
    // Flag bit masks
    private static final int FLAG_ENABLED = 1;
    private static final int FLAG_WATER_SENSITIVE = 1 << 1;
    private static final int FLAG_DIRTY = 1 << 2;
    private static final int FLAG_INTERPOLATING = 1 << 3;
    
    // Native memory
    private final Arena arena;
    private final MemorySegment buffer;
    private final int capacity;
    
    // Source tracking with StampedLock for optimistic reads
    private final Reference2IntOpenHashMap<DynamicLightSource> sourceToIndex;
    private final Object2ObjectOpenHashMap<DynamicLightSource, DynamicLightSource> sourceSet;
    private final StampedLock lock;
    private final AtomicInteger activeCount;
    
    // Free list for slot allocation
    private final int[] freeSlots;
    private int freeSlotHead;
    
    // VarHandles for native memory access
    private static final VarHandle VH_DOUBLE;
    private static final VarHandle VH_INT;
    
    static {
        VH_DOUBLE = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_DOUBLE);
        VH_INT = MethodHandles.memorySegmentViewVarHandle(ValueLayout.JAVA_INT);
    }
    
    private final NeonMetrics metrics;
    
    NativeLightEngine(int maxSources, NeonMetrics metrics) {
        this.capacity = maxSources;
        this.metrics = metrics;
        this.arena = Arena.ofShared();
        this.buffer = arena.allocate((long) maxSources * ENTRY_SIZE, 64);
        
        this.sourceToIndex = new Reference2IntOpenHashMap<>(maxSources);
        this.sourceToIndex.defaultReturnValue(-1);
        this.sourceSet = new Object2ObjectOpenHashMap<>(maxSources);
        this.lock = new StampedLock();
        this.activeCount = new AtomicInteger(0);
        
        // Initialize free list
        this.freeSlots = new int[maxSources];
        for (int i = 0; i < maxSources; i++) {
            freeSlots[i] = i;
        }
        this.freeSlotHead = 0;
        
        // Zero initialize buffer
        buffer.fill((byte) 0);
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Source Management
    // ════════════════════════════════════════════════════════════════════════
    
    void addSource(@NotNull DynamicLightSource source) {
        long stamp = lock.writeLock();
        try {
            if (sourceSet.containsKey(source)) return;
            if (activeCount.get() >= capacity) {
                Neon.LOG.warn("Light source limit reached: {}", capacity);
                return;
            }
            
            int index = allocateSlot();
            if (index < 0) return;
            
            sourceSet.put(source, source);
            sourceToIndex.put(source, index);
            activeCount.incrementAndGet();
            
            // Initialize entry
            long base = (long) index * ENTRY_SIZE;
            writeDouble(base + OFFSET_X, source.neon$getX());
            writeDouble(base + OFFSET_Y, source.neon$getY());
            writeDouble(base + OFFSET_Z, source.neon$getZ());
            writeDouble(base + OFFSET_PREV_X, source.neon$getX());
            writeDouble(base + OFFSET_PREV_Y, source.neon$getY());
            writeDouble(base + OFFSET_PREV_Z, source.neon$getZ());
            writeInt(base + OFFSET_LUMINANCE, source.neon$getLuminance());
            writeInt(base + OFFSET_SMOOTHED, source.neon$getLuminance() * 1000);
            writeInt(base + OFFSET_FLAGS, FLAG_ENABLED | FLAG_DIRTY);
            writeInt(base + OFFSET_LAST_TICK, 0);
            
        } finally {
            lock.unlockWrite(stamp);
        }
    }
    
    void removeSource(@NotNull DynamicLightSource source) {
        long stamp = lock.writeLock();
        try {
            int index = sourceToIndex.removeInt(source);
            if (index < 0) return;
            
            sourceSet.remove(source);
            activeCount.decrementAndGet();
            
            // Clear entry
            buffer.asSlice((long) index * ENTRY_SIZE, ENTRY_SIZE).fill((byte) 0);
            releaseSlot(index);
            
        } finally {
            lock.unlockWrite(stamp);
        }
    }
    
    boolean contains(@NotNull DynamicLightSource source) {
        long stamp = lock.tryOptimisticRead();
        boolean result = sourceSet.containsKey(source);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = sourceSet.containsKey(source);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }
    
    int getSourceCount() {
        return activeCount.get();
    }
    
    void clear() {
        long stamp = lock.writeLock();
        try {
            sourceSet.clear();
            sourceToIndex.clear();
            activeCount.set(0);
            
            // Reset free list
            for (int i = 0; i < capacity; i++) {
                freeSlots[i] = i;
            }
            freeSlotHead = 0;
            
            buffer.fill((byte) 0);
        } finally {
            lock.unlockWrite(stamp);
        }
    }
    
    void forEachSource(@NotNull Consumer<DynamicLightSource> action) {
        long stamp = lock.readLock();
        try {
            for (DynamicLightSource source : sourceSet.keySet()) {
                action.accept(source);
            }
        } finally {
            lock.unlockRead(stamp);
        }
    }
    
    void removeIf(@NotNull Predicate<DynamicLightSource> predicate, 
                  @NotNull Consumer<DynamicLightSource> onRemove) {
        long stamp = lock.writeLock();
        try {
            var iterator = sourceSet.keySet().iterator();
            while (iterator.hasNext()) {
                DynamicLightSource source = iterator.next();
                if (predicate.test(source)) {
                    int index = sourceToIndex.removeInt(source);
                    if (index >= 0) {
                        buffer.asSlice((long) index * ENTRY_SIZE, ENTRY_SIZE).fill((byte) 0);
                        releaseSlot(index);
                    }
                    iterator.remove();
                    activeCount.decrementAndGet();
                    onRemove.accept(source);
                }
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Update Loop
    // ════════════════════════════════════════════════════════════════════════
    
    int updateAll(
        @NotNull RenderGlobal renderer,
        @NotNull SpatialLightIndex spatialIndex,
        @NotNull TemporalSmoother smoother,
        @NotNull LightUpdateScheduler scheduler
    ) {
        if (!scheduler.shouldRunGlobalUpdate()) {
            return 0;
        }
        
        int updatedCount = 0;
        
        // Collect sources to update (read lock)
        List<DynamicLightSource> toUpdate = new ArrayList<>();
        long stamp = lock.readLock();
        try {
            for (DynamicLightSource source : sourceSet.keySet()) {
                if (source.neon$shouldUpdate()) {
                    toUpdate.add(source);
                }
            }
        } finally {
            lock.unlockRead(stamp);
        }
        
        // Process updates
        for (DynamicLightSource source : toUpdate) {
            if (updateSingleSource(source, renderer, spatialIndex, smoother)) {
                updatedCount++;
            }
        }
        
        return updatedCount;
    }
    
    private boolean updateSingleSource(
        @NotNull DynamicLightSource source,
        @NotNull RenderGlobal renderer,
        @NotNull SpatialLightIndex spatialIndex,
        @NotNull TemporalSmoother smoother
    ) {
        int index = sourceToIndex.getInt(source);
        if (index < 0) return false;
        
        long base = (long) index * ENTRY_SIZE;
        
        // Read previous state
        double prevX = readDouble(base + OFFSET_X);
        double prevY = readDouble(base + OFFSET_Y);
        double prevZ = readDouble(base + OFFSET_Z);
        int prevLuminance = readInt(base + OFFSET_LUMINANCE);
        int prevSmoothedFixed = readInt(base + OFFSET_SMOOTHED);
        
        // Get current state
        double newX = source.neon$getX();
        double newY = source.neon$getY();
        double newZ = source.neon$getZ();
        int newLuminance = source.neon$getLuminance();
        
        // Check for significant changes
        double dx = newX - prevX;
        double dy = newY - prevY;
        double dz = newZ - prevZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        
        boolean positionChanged = distSq > 0.01; // 0.1^2
        boolean luminanceChanged = newLuminance != prevLuminance;
        
        if (positionChanged || luminanceChanged) {
            // Store previous position for interpolation
            writeDouble(base + OFFSET_PREV_X, prevX);
            writeDouble(base + OFFSET_PREV_Y, prevY);
            writeDouble(base + OFFSET_PREV_Z, prevZ);
            
            // Update current position
            writeDouble(base + OFFSET_X, newX);
            writeDouble(base + OFFSET_Y, newY);
            writeDouble(base + OFFSET_Z, newZ);
            writeInt(base + OFFSET_LUMINANCE, newLuminance);
            
            // Apply smoothing
            float prevSmoothed = prevSmoothedFixed / 1000.0f;
            float newSmoothed = smoother.smooth(prevSmoothed, newLuminance);
            writeInt(base + OFFSET_SMOOTHED, (int) (newSmoothed * 1000));
            
            // Mark dirty
            int flags = readInt(base + OFFSET_FLAGS);
            writeInt(base + OFFSET_FLAGS, flags | FLAG_DIRTY | FLAG_INTERPOLATING);
            
            // Update spatial index if position changed significantly
            if (positionChanged) {
                spatialIndex.update(source);
            }
            
            // Trigger chunk rebuilds
            source.neon$performUpdate(renderer);
            
            return true;
        }
        
        return false;
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Queries
    // ════════════════════════════════════════════════════════════════════════
    
    float getSmoothedLuminance(@NotNull DynamicLightSource source) {
        int index = sourceToIndex.getInt(source);
        if (index < 0) return 0;
        return readInt((long) index * ENTRY_SIZE + OFFSET_SMOOTHED) / 1000.0f;
    }
    
    /**
     * Get interpolated position for smooth rendering
     */
    void getInterpolatedPosition(
        @NotNull DynamicLightSource source, 
        float partialTicks,
        double[] outPosition
    ) {
        int index = sourceToIndex.getInt(source);
        if (index < 0) {
            outPosition[0] = source.neon$getX();
            outPosition[1] = source.neon$getY();
            outPosition[2] = source.neon$getZ();
            return;
        }
        
        long base = (long) index * ENTRY_SIZE;
        
        double prevX = readDouble(base + OFFSET_PREV_X);
        double prevY = readDouble(base + OFFSET_PREV_Y);
        double prevZ = readDouble(base + OFFSET_PREV_Z);
        double currX = readDouble(base + OFFSET_X);
        double currY = readDouble(base + OFFSET_Y);
        double currZ = readDouble(base + OFFSET_Z);
        
        outPosition[0] = prevX + (currX - prevX) * partialTicks;
        outPosition[1] = prevY + (currY - prevY) * partialTicks;
        outPosition[2] = prevZ + (currZ - prevZ) * partialTicks;
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Slot Management
    // ════════════════════════════════════════════════════════════════════════
    
    private int allocateSlot() {
        if (freeSlotHead >= capacity) return -1;
        return freeSlots[freeSlotHead++];
    }
    
    private void releaseSlot(int index) {
        if (freeSlotHead > 0) {
            freeSlots[--freeSlotHead] = index;
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Native Memory Access
    // ════════════════════════════════════════════════════════════════════════
    
    private double readDouble(long offset) {
        return (double) VH_DOUBLE.get(buffer, offset);
    }
    
    private void writeDouble(long offset, double value) {
        VH_DOUBLE.set(buffer, offset, value);
    }
    
    private int readInt(long offset) {
        return (int) VH_INT.get(buffer, offset);
    }
    
    private void writeInt(long offset, int value) {
        VH_INT.set(buffer, offset, value);
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ════════════════════════════════════════════════════════════════════════
    
    @Override
    public void close() {
        long stamp = lock.writeLock();
        try {
            clear();
            arena.close();
        } finally {
            lock.unlockWrite(stamp);
        }
    }
    
    MemorySegment getBuffer() { return buffer; }
    int getEntrySize() { return ENTRY_SIZE; }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 8: SPATIAL LIGHT INDEX
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Spatial hash grid for O(1) light source queries.
 * 
 * Divides world into cells of 16×16×16 blocks.
 * Each cell tracks light sources that can affect blocks within it.
 */
final class SpatialLightIndex {
    
    private static final int CELL_SHIFT = 4; // 16 blocks per cell
    private static final int CELL_SIZE = 1 << CELL_SHIFT;
    private static final double MAX_LIGHT_RADIUS = 15.0;
    private static final double MAX_LIGHT_RADIUS_SQ = MAX_LIGHT_RADIUS * MAX_LIGHT_RADIUS;
    
    // Cell -> sources in that cell
    private final Long2ObjectOpenHashMap<ObjectArrayList<DynamicLightSource>> cellToSources;
    
    // Source -> cells it's registered in
    private final Reference2ObjectOpenHashMap<DynamicLightSource, LongArrayList> sourceToCells;
    
    // Pool for LongArrayList to reduce allocations
    private final ConcurrentLinkedQueue<LongArrayList> listPool;
    
    SpatialLightIndex() {
        this.cellToSources = new Long2ObjectOpenHashMap<>();
        this.sourceToCells = new Reference2ObjectOpenHashMap<>();
        this.listPool = new ConcurrentLinkedQueue<>();
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Insertion / Removal / Update
    // ════════════════════════════════════════════════════════════════════════
    
    synchronized void insert(@NotNull DynamicLightSource source) {
        if (sourceToCells.containsKey(source)) {
            update(source);
            return;
        }
        
        int luminance = source.neon$getLuminance();
        if (luminance <= 0) return;
        
        double x = source.neon$getX();
        double y = source.neon$getY();
        double z = source.neon$getZ();
        
        // Calculate affected cell range
        double radius = Math.min(luminance + 1, MAX_LIGHT_RADIUS);
        int minCX = (int) Math.floor((x - radius) / CELL_SIZE);
        int minCY = (int) Math.floor((y - radius) / CELL_SIZE);
        int minCZ = (int) Math.floor((z - radius) / CELL_SIZE);
        int maxCX = (int) Math.floor((x + radius) / CELL_SIZE);
        int maxCY = (int) Math.floor((y + radius) / CELL_SIZE);
        int maxCZ = (int) Math.floor((z + radius) / CELL_SIZE);
        
        LongArrayList affectedCells = acquireList();
        
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cy = minCY; cy <= maxCY; cy++) {
                for (int cz = minCZ; cz <= maxCZ; cz++) {
                    long cellKey = packCellKey(cx, cy, cz);
                    
                    ObjectArrayList<DynamicLightSource> cellSources = 
                        cellToSources.computeIfAbsent(cellKey, k -> new ObjectArrayList<>());
                    
                    if (!cellSources.contains(source)) {
                        cellSources.add(source);
                    }
                    affectedCells.add(cellKey);
                }
            }
        }
        
        sourceToCells.put(source, affectedCells);
    }
    
    synchronized void remove(@NotNull DynamicLightSource source) {
        LongArrayList cells = sourceToCells.remove(source);
        if (cells == null) return;
        
        for (int i = 0; i < cells.size(); i++) {
            long cellKey = cells.getLong(i);
            ObjectArrayList<DynamicLightSource> cellSources = cellToSources.get(cellKey);
            if (cellSources != null) {
                cellSources.remove(source);
                if (cellSources.isEmpty()) {
                    cellToSources.remove(cellKey);
                }
            }
        }
        
        releaseList(cells);
    }
    
    synchronized void update(@NotNull DynamicLightSource source) {
        // Simple remove + insert for now
        // Could be optimized to only update changed cells
        remove(source);
        insert(source);
    }
    
    synchronized void clear() {
        // Return all lists to pool
        for (LongArrayList list : sourceToCells.values()) {
            releaseList(list);
        }
        cellToSources.clear();
        sourceToCells.clear();
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Queries
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * Query maximum dynamic light level at a position.
     * Uses configured falloff type.
     */
    double queryMaxLight(int blockX, int blockY, int blockZ) {
        int cellX = blockX >> CELL_SHIFT;
        int cellY = blockY >> CELL_SHIFT;
        int cellZ = blockZ >> CELL_SHIFT;
        long cellKey = packCellKey(cellX, cellY, cellZ);
        
        ObjectArrayList<DynamicLightSource> sources = cellToSources.get(cellKey);
        if (sources == null || sources.isEmpty()) {
            return 0.0;
        }
        
        double maxLight = 0.0;
        double px = blockX + 0.5;
        double py = blockY + 0.5;
        double pz = blockZ + 0.5;
        LightFalloff falloff = NeonConfig.falloffType;
        
        for (int i = 0; i < sources.size(); i++) {
            DynamicLightSource source = sources.get(i);
            int luminance = source.neon$getLuminance();
            if (luminance <= 0) continue;
            
            double dx = px - source.neon$getX();
            double dy = py - source.neon$getY();
            double dz = pz - source.neon$getZ();
            double distSq = dx * dx + dy * dy + dz * dz;
            
            if (distSq < MAX_LIGHT_RADIUS_SQ) {
                double dist = Math.sqrt(distSq);
                double light = falloff.calculate(dist, MAX_LIGHT_RADIUS, luminance);
                maxLight = Math.max(maxLight, light);
            }
        }
        
        return Math.min(15.0, maxLight);
    }
    
    /**
     * Batch query for multiple positions (SIMD-style optimization).
     */
    void queryBatch(int[] blockX, int[] blockY, int[] blockZ, double[] outLight, int count) {
        LightFalloff falloff = NeonConfig.falloffType;
        
        for (int i = 0; i < count; i++) {
            outLight[i] = 0.0;
            
            int cellX = blockX[i] >> CELL_SHIFT;
            int cellY = blockY[i] >> CELL_SHIFT;
            int cellZ = blockZ[i] >> CELL_SHIFT;
            long cellKey = packCellKey(cellX, cellY, cellZ);
            
            ObjectArrayList<DynamicLightSource> sources = cellToSources.get(cellKey);
            if (sources == null) continue;
            
            double px = blockX[i] + 0.5;
            double py = blockY[i] + 0.5;
            double pz = blockZ[i] + 0.5;
            
            for (int j = 0; j < sources.size(); j++) {
                DynamicLightSource source = sources.get(j);
                int luminance = source.neon$getLuminance();
                if (luminance <= 0) continue;
                
                double dx = px - source.neon$getX();
                double dy = py - source.neon$getY();
                double dz = pz - source.neon$getZ();
                double distSq = dx * dx + dy * dy + dz * dz;
                
                if (distSq < MAX_LIGHT_RADIUS_SQ) {
                    double dist = Math.sqrt(distSq);
                    double light = falloff.calculate(dist, MAX_LIGHT_RADIUS, luminance);
                    outLight[i] = Math.max(outLight[i], light);
                }
            }
            
            outLight[i] = Math.min(15.0, outLight[i]);
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Utilities
    // ════════════════════════════════════════════════════════════════════════
    
    private static long packCellKey(int x, int y, int z) {
        return ((long) (x + 0x100000) & 0x1FFFFF) << 42 |
               ((long) (y + 0x400) & 0xFFF) << 30 |
               ((long) (z + 0x100000) & 0x1FFFFF) << 9;
    }
    
    private LongArrayList acquireList() {
        LongArrayList list = listPool.poll();
        if (list == null) {
            list = new LongArrayList(8);
        }
        return list;
    }
    
    private void releaseList(LongArrayList list) {
        list.clear();
        if (listPool.size() < 256) { // Limit pool size
            listPool.offer(list);
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 9: TEMPORAL SMOOTHER
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Provides smooth interpolation between light levels to reduce flickering.
 */
final class TemporalSmoother {
    
    private static final float MIN_DELTA = 0.01f;
    
    /**
     * Smooth a value towards a target using exponential interpolation.
     */
    float smooth(float current, float target) {
        if (!NeonConfig.smoothLighting) {
            return target;
        }
        
        float delta = target - current;
        if (Math.abs(delta) < MIN_DELTA) {
            return target;
        }
        
        float factor = (float) NeonConfig.smoothingFactor;
        float result = current + delta * factor;
        
        // Snap to target if very close
        if (Math.abs(target - result) < MIN_DELTA) {
            return target;
        }
        
        return result;
    }
    
    /**
     * Smooth with custom factor (for special cases).
     */
    float smooth(float current, float target, float customFactor) {
        if (!NeonConfig.smoothLighting) {
            return target;
        }
        
        float delta = target - current;
        if (Math.abs(delta) < MIN_DELTA) {
            return target;
        }
        
        return current + delta * customFactor;
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 10: UPDATE SCHEDULER
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Schedules light updates based on mode and distance LOD.
 */
final class LightUpdateScheduler {
    
    private final AtomicLong lastGlobalUpdate = new AtomicLong(0);
    
    boolean shouldRunGlobalUpdate() {
        DynamicLightsMode mode = NeonConfig.mode;
        if (!mode.isEnabled()) return false;
        
        long now = System.currentTimeMillis();
        long last = lastGlobalUpdate.get();
        
        if (now >= last + 50) { // 20 TPS minimum
            lastGlobalUpdate.compareAndSet(last, now);
            return true;
        }
        return false;
    }
    
    /**
     * Calculate update interval based on distance to player (LOD).
     */
    int getUpdateInterval(@NotNull DynamicLightSource source) {
        if (!NeonConfig.distanceLOD) {
            return NeonConfig.mode.getDelayMs();
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return NeonConfig.mode.getDelayMs();
        }
        
        double dx = source.neon$getX() - mc.player.posX;
        double dy = source.neon$getY() - mc.player.posY;
        double dz = source.neon$getZ() - mc.player.posZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        
        // Increase interval for distant lights
        if (distSq > 64 * 64) { // > 64 blocks
            return NeonConfig.mode.getDelayMs() * 4;
        } else if (distSq > 32 * 32) { // > 32 blocks
            return NeonConfig.mode.getDelayMs() * 2;
        }
        
        return NeonConfig.mode.getDelayMs();
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 11: ITEM LIGHT REGISTRY
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Registry for items that emit light when held or dropped.
 */
final class ItemLightRegistry {
    
    private static final Reference2ObjectMap<Item, ItemLightSource> SOURCES = 
        new Reference2ObjectOpenHashMap<>();
    private static final Reference2ObjectMap<Item, ItemLightSource> STATIC_SOURCES = 
        new Reference2ObjectOpenHashMap<>();
    
    private static final String[] DEFAULT_CONFIGS = {
        "blaze_powder", "blaze_rod", "fire_charge", "glowstone_dust",
        "lava_bucket", "nether_star", "prismarine_crystals", 
        "redstone_torch", "spectral_arrow", "torch"
    };
    
    private ItemLightRegistry() {}
    
    static void reload(@NotNull IResourceManager resourceManager) {
        SOURCES.clear();
        
        for (String name : DEFAULT_CONFIGS) {
            ResourceLocation location = new ResourceLocation(Neon.MOD_ID, "dynamiclights/item/" + name + ".json");
            try {
                for (IResource resource : resourceManager.getAllResources(location)) {
                    loadResource(location, resource);
                }
            } catch (Exception e) {
                // Config not found - expected for optional items
            }
        }
        
        // Static sources override loaded ones
        SOURCES.putAll(STATIC_SOURCES);
        
        Neon.LOG.info("Loaded {} item light sources", SOURCES.size());
    }
    
    private static void loadResource(ResourceLocation location, IResource resource) {
        ResourceLocation id = new ResourceLocation(
            location.getNamespace(),
            location.getPath().replace("dynamiclights/item/", "").replace(".json", "")
        );
        
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            
            if (!json.has("item") || !json.has("luminance")) return;
            
            ResourceLocation itemId = new ResourceLocation(json.get("item").getAsString());
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) return;
            
            boolean waterSensitive = json.has("water_sensitive") && 
                json.get("water_sensitive").getAsBoolean();
            
            var luminanceElement = json.get("luminance");
            
            ItemLightSource source;
            if (luminanceElement.isJsonPrimitive()) {
                var prim = luminanceElement.getAsJsonPrimitive();
                if (prim.isNumber()) {
                    source = new StaticItemLight(id, item, prim.getAsInt(), waterSensitive);
                } else if (prim.isString() && prim.getAsString().equals("block")) {
                    if (item instanceof ItemBlock blockItem) {
                        source = new BlockItemLight(id, item, blockItem.getBlock().getDefaultState(), waterSensitive);
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
            
            if (!STATIC_SOURCES.containsKey(item)) {
                SOURCES.put(item, source);
            }
        } catch (Exception e) {
            Neon.LOG.warn("Failed to load item light source: {}", id, e);
        }
    }
    
    public static void register(@NotNull ItemLightSource source) {
        STATIC_SOURCES.put(source.item(), source);
    }
    
    public static int getLuminance(@NotNull ItemStack stack, boolean submerged) {
        if (stack.isEmpty()) return 0;
        
        ItemLightSource source = SOURCES.get(stack.getItem());
        if (source != null) {
            return Math.min(14, source.getLuminance(stack, submerged));
        }
        
        // Check if block item
        if (stack.getItem() instanceof ItemBlock blockItem) {
            IBlockState state = blockItem.getBlock().getDefaultState();
            return Math.min(14, state.getLightValue());
        }
        
        return 0;
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Light Source Types
    // ════════════════════════════════════════════════════════════════════════
    
    interface ItemLightSource {
        ResourceLocation id();
        Item item();
        boolean waterSensitive();
        int getLuminance(ItemStack stack, boolean submerged);
    }
    
    record StaticItemLight(
        ResourceLocation id,
        Item item,
        int luminance,
        boolean waterSensitive
    ) implements ItemLightSource {
        @Override
        public int getLuminance(ItemStack stack, boolean submerged) {
            if (waterSensitive && NeonConfig.waterSensitivity && submerged) return 0;
            return luminance;
        }
    }
    
    record BlockItemLight(
        ResourceLocation id,
        Item item,
        IBlockState blockState,
        boolean waterSensitive
    ) implements ItemLightSource {
        @Override
        public int getLuminance(ItemStack stack, boolean submerged) {
            if (waterSensitive && NeonConfig.waterSensitivity && submerged) return 0;
            return blockState.getLightValue();
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 12: LUMEN BRIDGE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Integration bridge with Lumen lighting engine.
 */
final class LumenBridge {
    
    private final NativeLightEngine engine;
    private volatile boolean ready = false;
    private Object lumenInstance;
    
    LumenBridge(NativeLightEngine engine) {
        this.engine = engine;
    }
    
    void initialize() {
        if (!NeonConfig.lumenIntegration) {
            Neon.LOG.info("Lumen integration disabled in config");
            return;
        }
        
        try {
            Class<?> lumenClass = Class.forName("lumen.core.Lumen");
            var getInstance = lumenClass.getMethod("get");
            lumenInstance = getInstance.invoke(null);
            
            if (lumenInstance != null) {
                registerWithLumen();
                ready = true;
                Neon.LOG.info("Lumen integration active");
            }
        } catch (ClassNotFoundException e) {
            Neon.LOG.info("Lumen not present - running standalone");
        } catch (Exception e) {
            Neon.LOG.warn("Lumen integration failed", e);
        }
    }
    
    private void registerWithLumen() throws Exception {
        // Register dynamic light buffer with Lumen's native system
        var lumenEngine = lumenInstance.getClass().getDeclaredField("lightEngine");
        lumenEngine.setAccessible(true);
        Object lightEngine = lumenEngine.get(lumenInstance);
        
        if (lightEngine != null) {
            var registerMethod = lightEngine.getClass().getMethod(
                "registerDynamicLightProvider",
                Supplier.class
            );
            registerMethod.invoke(lightEngine, (Supplier<MemorySegment>) engine::getBuffer);
        }
    }
    
    void pushUpdates() {
        if (!ready || lumenInstance == null) return;
        
        try {
            var notifyMethod = lumenInstance.getClass().getMethod("notifyDynamicLightUpdate");
            notifyMethod.invoke(lumenInstance);
        } catch (Exception ignored) {
            // Lumen may not support this - silently ignore
        }
    }
    
    boolean isReady() { return ready; }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 13: METRICS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Performance metrics and statistics.
 */
final class NeonMetrics {
    
    private final AtomicInteger sourceCount = new AtomicInteger(0);
    private final AtomicInteger lastUpdateCount = new AtomicInteger(0);
    private final AtomicLong lastUpdateTimeNs = new AtomicLong(0);
    private final AtomicLong totalUpdates = new AtomicLong(0);
    private final AtomicLong totalUpdateTimeNs = new AtomicLong(0);
    
    // Rolling average (last 100 updates)
    private final long[] recentUpdateTimes = new long[100];
    private int recentIndex = 0;
    
    void recordUpdate(int count, long timeNs) {
        lastUpdateCount.set(count);
        lastUpdateTimeNs.set(timeNs);
        totalUpdates.incrementAndGet();
        totalUpdateTimeNs.addAndGet(timeNs);
        
        recentUpdateTimes[recentIndex] = timeNs;
        recentIndex = (recentIndex + 1) % recentUpdateTimes.length;
    }
    
    void incrementSources() { sourceCount.incrementAndGet(); }
    void decrementSources() { sourceCount.decrementAndGet(); }
    void resetSourceCount() { sourceCount.set(0); }
    
    int getSourceCount() { return sourceCount.get(); }
    int getLastUpdateCount() { return lastUpdateCount.get(); }
    long getLastUpdateTimeNs() { return lastUpdateTimeNs.get(); }
    
    double getAverageUpdateTimeMs() {
        long total = 0;
        for (long t : recentUpdateTimes) {
            total += t;
        }
        return (total / (double) recentUpdateTimes.length) / 1_000_000.0;
    }
    
    String getDebugString() {
        return String.format(
            "Neon: %d sources, %d updated, %.2fms avg",
            sourceCount.get(),
            lastUpdateCount.get(),
            getAverageUpdateTimeMs()
        );
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 14: UTILITIES
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Fluid detection utilities with mod compatibility.
 */
final class FluidUtil {
    
    private static final boolean FLUIDLOGGED_API = Loader.isModLoaded("fluidlogged_api");
    
    private FluidUtil() {}
    
    static boolean isEntitySubmerged(@NotNull Entity entity) {
        float pt = Minecraft.getMinecraft().getRenderPartialTicks();
        BlockPos eyePos = new BlockPos(entity.getPositionEyes(pt));
        return isFluidAt(entity.world, eyePos);
    }
    
    static boolean isFluidAt(@NotNull IBlockAccess world, @NotNull BlockPos pos) {
        IBlockState state = getFluidState(world, pos);
        Material material = state.getMaterial();
        return material.isLiquid() || material == Material.WATER || material == Material.LAVA;
    }
    
    static IBlockState getFluidState(@NotNull IBlockAccess world, @NotNull BlockPos pos) {
        if (FLUIDLOGGED_API) {
            try {
                Class<?> fluidUtils = Class.forName(
                    "git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils"
                );
                var method = fluidUtils.getMethod("getFluidOrReal", IBlockAccess.class, BlockPos.class);
                return (IBlockState) method.invoke(null, world, pos);
            } catch (Exception ignored) {}
        }
        return world.getBlockState(pos);
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 15: EVENT HANDLER
// ═══════════════════════════════════════════════════════════════════════════════

final class NeonEventHandler {
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            Neon.get().clearAllLightSources();
        }
    }
    
    @SubscribeEvent
    public void onConfigChanged(net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent event) {
        if (Neon.MOD_ID.equals(event.getModID())) {
            NeonConfig.sync();
            Neon.LOG.info("Configuration reloaded");
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 16: COREMOD / MIXIN LOADER
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * FML Loading Plugin for early mixin registration.
 */
final class NeonLoadingPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {
    
    @Override public String[] getASMTransformerClass() { return new String[0]; }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) {}
    @Override public String getAccessTransformerClass() { return null; }
    
    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.neon.json");
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 17: MIXIN ACCESSORS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Accessor for RenderGlobal chunk rebuild scheduling.
 */
interface WorldRendererAccessor {
    void neon$markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2, boolean important);
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 18: MIXINS - ENTITY BASE
// ═══════════════════════════════════════════════════════════════════════════════

@Mixin(Entity.class)
abstract class MixinEntity implements DynamicLightSource {
    
    @Shadow public World world;
    @Shadow public double posX;
    @Shadow public double posY;
    @Shadow public double posZ;
    @Shadow public boolean isDead;
    @Shadow public int chunkCoordX;
    @Shadow public int chunkCoordY;
    @Shadow public int chunkCoordZ;
    
    @Shadow public abstract float getEyeHeight();
    @Shadow public abstract boolean isBurning();
    @Shadow public abstract BlockPos getPosition();
    
    @Unique private int neon$luminance = 0;
    @Unique private int neon$lastLuminance = 0;
    @Unique private long neon$lastUpdateTime = 0;
    @Unique private double neon$prevX, neon$prevY, neon$prevZ;
    @Unique private final LongOpenHashSet neon$trackedChunks = new LongOpenHashSet();
    
    @Override public double neon$getX() { return posX; }
    @Override public double neon$getY() { return posY + getEyeHeight(); }
    @Override public double neon$getZ() { return posZ; }
    @Override public @Nullable World neon$getWorld() { return world; }
    @Override public int neon$getLuminance() { return neon$luminance; }
    
    @Override
    public void neon$tick() {
        // Fire gives light
        neon$luminance = isBurning() ? 14 : 0;
        
        // Check handler
        int handlerLum = LightHandlerRegistry.getLuminance((Entity) (Object) this);
        neon$luminance = Math.max(neon$luminance, handlerLum);
    }
    
    @Override public void neon$reset() { neon$lastLuminance = 0; }
    
    @Override
    public boolean neon$isEnabled() {
        return NeonConfig.mode.isEnabled() && Neon.get().containsLightSource(this);
    }
    
    @Override
    public void neon$setEnabled(boolean enabled) {
        neon$reset();
        if (enabled) {
            Neon.get().addLightSource(this);
        } else {
            Neon.get().removeLightSource(this);
        }
    }
    
    @Override
    public boolean neon$shouldUpdate() {
        DynamicLightsMode mode = NeonConfig.mode;
        if (!mode.isEnabled()) return false;
        
        if (mode.hasDelay()) {
            long now = System.currentTimeMillis();
            if (now < neon$lastUpdateTime + mode.getDelayMs()) {
                return false;
            }
            neon$lastUpdateTime = now;
        }
        return true;
    }
    
    @Override
    public boolean neon$performUpdate(@NotNull RenderGlobal renderer) {
        if (!neon$shouldUpdate()) return false;
        
        double dx = posX - neon$prevX;
        double dy = posY - neon$prevY;
        double dz = posZ - neon$prevZ;
        int luminance = neon$getLuminance();
        
        boolean posChanged = dx * dx + dy * dy + dz * dz > 0.01;
        boolean lumChanged = luminance != neon$lastLuminance;
        
        if (posChanged || lumChanged) {
            neon$prevX = posX;
            neon$prevY = posY;
            neon$prevZ = posZ;
            neon$lastLuminance = luminance;
            
            LongOpenHashSet newChunks = new LongOpenHashSet();
            
            if (luminance > 0) {
                double eyeY = posY + getEyeHeight();
                int chunkY = (int) Math.floor(eyeY) >> 4;
                BlockPos.MutableBlockPos chunkPos = new BlockPos.MutableBlockPos(chunkCoordX, chunkY, chunkCoordZ);
                
                Neon.scheduleChunkRebuild(renderer, chunkPos);
                Neon.updateTrackedChunks(chunkPos, neon$trackedChunks, newChunks);
                
                // Adjacent chunks based on position within chunk
                double localX = posX - Math.floor(posX / 16.0) * 16.0;
                double localY = eyeY - Math.floor(eyeY / 16.0) * 16.0;
                double localZ = posZ - Math.floor(posZ / 16.0) * 16.0;
                
                EnumFacing dirX = localX >= 8.0 ? EnumFacing.EAST : EnumFacing.WEST;
                EnumFacing dirY = localY >= 8.0 ? EnumFacing.UP : EnumFacing.DOWN;
                EnumFacing dirZ = localZ >= 8.0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
                
                for (int i = 0; i < 7; i++) {
                    switch (i % 4) {
                        case 0 -> chunkPos.move(dirX);
                        case 1 -> chunkPos.move(dirZ);
                        case 2 -> chunkPos.move(dirX.getOpposite());
                        case 3 -> { chunkPos.move(dirZ.getOpposite()); chunkPos.move(dirY); }
                    }
                    Neon.scheduleChunkRebuild(renderer, chunkPos);
                    Neon.updateTrackedChunks(chunkPos, neon$trackedChunks, newChunks);
                }
            }
            
            neon$scheduleChunkRebuilds(renderer);
            neon$trackedChunks.clear();
            neon$trackedChunks.addAll(newChunks);
            return true;
        }
        return false;
    }
    
    @Override
    public void neon$scheduleChunkRebuilds(@NotNull RenderGlobal renderer) {
        if (Minecraft.getMinecraft().world == world) {
            LongIterator iter = neon$trackedChunks.iterator();
            while (iter.hasNext()) {
                Neon.scheduleChunkRebuild(renderer, iter.nextLong());
            }
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // Injections
    // ════════════════════════════════════════════════════════════════════════
    
    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void neon$onTick(CallbackInfo ci) {
        if (world != null && world.isRemote) {
            if (isDead) {
                neon$setEnabled(false);
            } else {
                neon$tick();
                Neon.updateTracking(this);
            }
        }
    }
    
    @Inject(method = "getBrightnessForRender", at = @At("RETURN"), cancellable = true)
    private void neon$modifyBrightness(CallbackInfoReturnable<Integer> cir) {
        if (!NeonConfig.mode.isEnabled()) return;
        int lightmap = cir.getReturnValueI();
        cir.setReturnValue(Neon.get().getLightmapWithDynamicLight((Entity) (Object) this, lightmap));
    }
    
    @Inject(method = "setDead", at = @At("HEAD"))
    private void neon$onDeath(CallbackInfo ci) {
        if (world != null && world.isRemote) {
            neon$setEnabled(false);
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 19: MIXINS - LIVING ENTITY
// ═══════════════════════════════════════════════════════════════════════════════

@Mixin(EntityLivingBase.class)
abstract class MixinEntityLivingBase extends Entity implements DynamicLightSource {
    
    @Unique private int neon$livingLuminance = 0;
    
    private MixinEntityLivingBase() { super(null); }
    
    @Shadow public abstract boolean isGlowing();
    
    @Override
    public void neon$tick() {
        EntityLivingBase self = (EntityLivingBase) (Object) this;
        
        // Fire/glowing
        neon$livingLuminance = isBurning() || isGlowing() ? 14 : 0;
        
        // Held items and armor
        int itemLight = Neon.getLivingEntityItemLuminance(self);
        neon$livingLuminance = Math.max(neon$livingLuminance, itemLight);
        
        // Handler
        int handlerLight = LightHandlerRegistry.getLuminance(self);
        neon$livingLuminance = Math.max(neon$livingLuminance, handlerLight);
    }
    
    @Override
    public int neon$getLuminance() {
        return neon$livingLuminance;
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 20: MIXINS - PLAYER ENTITY
// ═══════════════════════════════════════════════════════════════════════════════

@Mixin(EntityPlayer.class)
abstract class MixinEntityPlayer extends EntityLivingBase implements DynamicLightSource {
    
    @Unique private int neon$playerLuminance = 0;
    @Unique private World neon$lastWorld;
    
    private MixinEntityPlayer() { super(null); }
    
    @Shadow public abstract boolean isSpectator();
    
    @Override
    public void neon$tick() {
        EntityPlayer self = (EntityPlayer) (Object) this;
        
        // Spectators don't emit light
        if (isSpectator()) {
            neon$playerLuminance = 0;
            return;
        }
        
        // World change check
        if (neon$lastWorld != world) {
            neon$lastWorld = world;
            neon$playerLuminance = 0;
            return;
        }
        
        // Fire/glowing
        neon$playerLuminance = isBurning() || isGlowing() ? 14 : 0;
        
        // Items
        int itemLight = Neon.getLivingEntityItemLuminance(self);
        neon$playerLuminance = Math.max(neon$playerLuminance, itemLight);
        
        // Handler
        int handlerLight = LightHandlerRegistry.getLuminance(self);
        neon$playerLuminance = Math.max(neon$playerLuminance, handlerLight);
    }
    
    @Override
    public int neon$getLuminance() {
        return neon$playerLuminance;
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 21: MIXINS - TNT ENTITY
// ═══════════════════════════════════════════════════════════════════════════════

@Mixin(EntityTNTPrimed.class)
abstract class MixinEntityTNTPrimed extends Entity implements DynamicLightSource {
    
    @Unique private int neon$tntLuminance = 0;
    @Unique private int neon$initialFuse = 80;
    
    private MixinEntityTNTPrimed() { super(null); }
    
    @Shadow public abstract int getFuse();
    
    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/entity/EntityLivingBase;)V", at = @At("TAIL"))
    private void neon$onInit(World world, double x, double y, double z, EntityLivingBase placer, CallbackInfo ci) {
        neon$initialFuse = getFuse();
    }
    
    @Override
    public void neon$tick() {
        if (!NeonConfig.tntLightMode.isEnabled()) {
            neon$tntLuminance = 0;
            return;
        }
        
        if (isBurning()) {
            neon$tntLuminance = 14;
        } else {
            neon$tntLuminance = switch (NeonConfig.tntLightMode) {
                case SIMPLE -> 10;
                case FANCY -> {
                    float fuseRatio = (float) getFuse() / neon$initialFuse;
                    yield (int) (-(fuseRatio * fuseRatio) * 10.0f) + 10;
                }
                default -> 0;
            };
        }
    }
    
    @Override
    public int neon$getLuminance() {
        return neon$tntLuminance;
    }
    
    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void neon$onTntTick(CallbackInfo ci) {
        if (world != null && world.isRemote) {
            if (isDead) {
                neon$setEnabled(false);
            } else {
                neon$tick();
                Neon.updateTracking(this);
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 22: MIXINS - HANGING ENTITY (ITEM FRAMES)
// ═══════════════════════════════════════════════════════════════════════════════

@Mixin(EntityHanging.class)
abstract class MixinEntityHanging extends Entity implements DynamicLightSource {
    
    private MixinEntityHanging() { super(null); }
    
    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void neon$onHangingTick(CallbackInfo ci) {
        if (world != null && world.isRemote) {
            if (isDead) {
                neon$setEnabled(false);
            } else {
                neon$tick();
                Neon.updateTracking(this);
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 23: MIXINS - TILE ENTITY
// ═══════════════════════════════════════════════════════════════════════════════

@Mixin(TileEntity.class)
abstract class MixinTileEntity implements DynamicLightSource {
    
    @Shadow protected BlockPos pos;
    @Shadow protected World world;
    @Shadow protected boolean tileEntityInvalid;
    
    @Unique private int neon$teLuminance = 0;
    @Unique private int neon$teLastLuminance = 0;
    @Unique private long neon$teLastUpdate = 0;
    @Unique private final LongOpenHashSet neon$teTrackedChunks = new LongOpenHashSet();
    
    @Override public double neon$getX() { return pos.getX() + 0.5; }
    @Override public double neon$getY() { return pos.getY() + 0.5; }
    @Override public double neon$getZ() { return pos.getZ() + 0.5; }
    @Override public @Nullable World neon$getWorld() { return world; }
    @Override public int neon$getLuminance() { return neon$teLuminance; }
    
    @Override
    public void neon$tick() {
        if (world == null || !world.isRemote || tileEntityInvalid) return;
        neon$teLuminance = LightHandlerRegistry.getLuminance((TileEntity) (Object) this);
        Neon.updateTracking(this);
    }
    
    @Override public void neon$reset() { neon$teLastLuminance = 0; }
    
    @Override
    public boolean neon$isEnabled() {
        return NeonConfig.mode.isEnabled() && Neon.get().containsLightSource(this);
    }
    
    @Override
    public void neon$setEnabled(boolean enabled) {
        neon$reset();
        if (enabled) {
            Neon.get().addLightSource(this);
        } else {
            Neon.get().removeLightSource(this);
        }
    }
    
    @Override
    public boolean neon$shouldUpdate() {
        DynamicLightsMode mode = NeonConfig.mode;
        if (!mode.isEnabled()) return false;
        
        if (mode.hasDelay()) {
            long now = System.currentTimeMillis();
            if (now < neon$teLastUpdate + mode.getDelayMs()) return false;
            neon$teLastUpdate = now;
        }
        return true;
    }
    
    @Override
    public boolean neon$performUpdate(@NotNull RenderGlobal renderer) {
        if (!neon$shouldUpdate()) return false;
        
        int luminance = neon$getLuminance();
        if (luminance != neon$teLastLuminance) {
            neon$teLastLuminance = luminance;
            
            if (neon$teTrackedChunks.isEmpty()) {
                BlockPos.MutableBlockPos chunkPos = new BlockPos.MutableBlockPos(
                    Math.floorDiv(pos.getX(), 16),
                    Math.floorDiv(pos.getY(), 16),
                    Math.floorDiv(pos.getZ(), 16)
                );
                
                Neon.updateTrackedChunks(chunkPos, null, neon$teTrackedChunks);
                
                double localX = pos.getX() - Math.floor(pos.getX() / 16.0) * 16.0;
                double localY = pos.getY() - Math.floor(pos.getY() / 16.0) * 16.0;
                double localZ = pos.getZ() - Math.floor(pos.getZ() / 16.0) * 16.0;
                
                EnumFacing dirX = localX >= 8.0 ? EnumFacing.EAST : EnumFacing.WEST;
                EnumFacing dirY = localY >= 8.0 ? EnumFacing.UP : EnumFacing.DOWN;
                EnumFacing dirZ = localZ >= 8.0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
                
                for (int i = 0; i < 7; i++) {
                    switch (i % 4) {
                        case 0 -> chunkPos.move(dirX);
                        case 1 -> chunkPos.move(dirZ);
                        case 2 -> chunkPos.move(dirX.getOpposite());
                        case 3 -> { chunkPos.move(dirZ.getOpposite()); chunkPos.move(dirY); }
                    }
                    Neon.updateTrackedChunks(chunkPos, null, neon$teTrackedChunks);
                }
            }
            
            neon$scheduleChunkRebuilds(renderer);
            return true;
        }
        return false;
    }
    
    @Override
    public void neon$scheduleChunkRebuilds(@NotNull RenderGlobal renderer) {
        if (Minecraft.getMinecraft().world == world) {
            LongIterator iter = neon$teTrackedChunks.iterator();
            while (iter.hasNext()) {
                Neon.scheduleChunkRebuild(renderer, iter.nextLong());
            }
        }
    }
    
    @Inject(method = "invalidate", at = @At("TAIL"))
    private void neon$onInvalidate(CallbackInfo ci) {
        neon$setEnabled(false);
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 24: MIXINS - RENDER GLOBAL
// ═══════════════════════════════════════════════════════════════════════════════

@Mixin(RenderGlobal.class)
abstract class MixinRenderGlobal implements WorldRendererAccessor {
    
    @Shadow
    protected abstract void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2, boolean updateImmediately);
    
    @Override
    public void neon$markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2, boolean important) {
        markBlockRangeForRenderUpdate(x1, y1, z1, x2, y2, z2, important);
    }
    
    @Inject(method = "setupTerrain", at = @At("HEAD"))
    private void neon$beforeSetupTerrain(Entity viewEntity, double partialTicks, 
            net.minecraft.client.renderer.culling.ICamera camera, int frameCount, 
            boolean playerSpectator, CallbackInfo ci) {
        Neon.get().updateAll((RenderGlobal) (Object) this);
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 25: MIXINS - BLOCK STATE
// ═══════════════════════════════════════════════════════════════════════════════

@Mixin(targets = "net.minecraft.block.state.BlockStateContainer$StateImplementation")
abstract class MixinBlockState {
    
    @Inject(method = "getPackedLightmapCoords", at = @At("RETURN"), cancellable = true)
    private void neon$modifyLightmap(IBlockAccess world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (!NeonConfig.mode.isEnabled()) return;
        
        IBlockState self = (IBlockState) (Object) this;
        
        // Skip for AO on opaque blocks
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.ambientOcclusion > 0 && self.isOpaqueCube()) {
            return;
        }
        
        int vanilla = cir.getReturnValueI();
        int modified = Neon.get().getLightmapWithDynamicLight(pos, vanilla);
        cir.setReturnValue(modified);
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 26: MIXINS - WORLD CLIENT
// ═══════════════════════════════════════════════════════════════════════════════

@Mixin(net.minecraft.client.multiplayer.WorldClient.class)
abstract class MixinWorldClient {
    
    @Inject(method = "removeEntityFromWorld", at = @At("HEAD"))
    private void neon$onRemoveEntity(int entityId, CallbackInfoReturnable<Entity> cir) {
        net.minecraft.client.multiplayer.WorldClient world = 
            (net.minecraft.client.multiplayer.WorldClient) (Object) this;
        Entity entity = world.getEntityByID(entityId);
        if (entity instanceof DynamicLightSource source) {
            source.neon$setEnabled(false);
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 27: MIXINS - MINECRAFT
// ═══════════════════════════════════════════════════════════════════════════════

@Mixin(Minecraft.class)
abstract class MixinMinecraft {
    
    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", 
            at = @At("HEAD"))
    private void neon$onLoadWorld(net.minecraft.client.multiplayer.WorldClient world, 
            String loadingMessage, CallbackInfo ci) {
        Neon.get().clearAllLightSources();
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 28: MIXINS - PARTICLE
// ═══════════════════════════════════════════════════════════════════════════════

@Mixin(Particle.class)
abstract class MixinParticle {
    
    @Shadow protected double posX;
    @Shadow protected double posY;
    @Shadow protected double posZ;
    
    @ModifyArg(
        method = "getBrightnessForRender",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getCombinedLight(Lnet/minecraft/util/math/BlockPos;I)I"),
        index = 1
    )
    private int neon$modifyParticleLight(int lightValue) {
        if (!NeonConfig.mode.isEnabled()) return lightValue;
        
        BlockPos pos = new BlockPos(posX, posY, posZ);
        int dynamicLight = (int) Neon.get().getDynamicLightLevel(pos);
        return Math.max(lightValue, dynamicLight);
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 29: MIXINS - ITEM RENDERER (First Person)
// ═══════════════════════════════════════════════════════════════════════════════

@Mixin(ItemRenderer.class)
abstract class MixinItemRenderer {
    
    @Shadow private ItemStack itemStackMainHand;
    @Shadow private ItemStack itemStackOffHand;
    @Shadow private Minecraft mc;
    
    @ModifyArg(
        method = "updateEquippedItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getCombinedLight(Lnet/minecraft/util/math/BlockPos;I)I"),
        index = 1
    )
    private int neon$modifyHeldItemLight(int lightValue) {
        if (!NeonConfig.mode.isEnabled()) return lightValue;
        if (mc.player == null) return lightValue;
        
        boolean submerged = Neon.isSubmergedInFluid(mc.player);
        int mainLight = Neon.getLuminanceFromItemStack(itemStackMainHand, submerged);
        int offLight = Neon.getLuminanceFromItemStack(itemStackOffHand, submerged);
        
        return Math.max(lightValue, Math.max(mainLight, offLight));
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 30: MIXINS - TILE ENTITY RENDERER DISPATCHER
// ═══════════════════════════════════════════════════════════════════════════════

@Mixin(TileEntityRendererDispatcher.class)
abstract class MixinTileEntityRendererDispatcher {
    
    @ModifyArg(
        method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getCombinedLight(Lnet/minecraft/util/math/BlockPos;I)I"),
        index = 1
    )
    private int neon$modifyTileEntityRenderLight(int lightValue) {
        // Light value is already modified at block state level
        // This is a fallback for direct TE rendering
        return lightValue;
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// END OF FILE
// ═══════════════════════════════════════════════════════════════════════════════
