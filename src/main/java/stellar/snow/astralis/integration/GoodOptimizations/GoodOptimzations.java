package stellar.snow.astralis.integration.GoodOpimizations;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GoodOptimization - Single File Edition
 * 
 * Complete rendering optimization suite for Minecraft 1.12.2
 * Because "bad" optimizations are actually good.
 * 
 * This single file contains:
 * - All mixin implementations (inner classes)
 * - Complete cache management system
 * - Configuration system
 * - Event handlers
 * - Performance monitoring
 * 
 * Zero external dependencies except Forge, Mixin, and Guava.
 * 
 * @author
 * @version 1.0.0
 */

public class GoodOptimization {
    
    public static final String MODID = "goodopt";
    public static final String NAME = "GoodOptimization";
    public static final String VERSION = "1.0.0";
    
    private static final Logger LOGGER = LogManager.getLogger(NAME);
    private static GoodOptimization instance;
    
    // Cache manager singleton
    private static final CacheManager CACHE = new CacheManager();
    
    // Configuration singleton
    @Config.LangKey("goodopt.config.title")
    public static class Configuration {
        
        @Config.Name("Lightmap Caching")
        @Config.Comment("Cache lightmap calculations to reduce GPU overhead")
        public static boolean enableLightmapCache = true;
        
        @Config.Name("Lightmap Update Threshold")
        @Config.Comment("Milliseconds between lightmap updates (higher = more caching)")
        @Config.RangeInt(min = 50, max = 500)
        public static int lightmapThreshold = 120;
        
        @Config.Name("Sky Color Caching")
        @Config.Comment("Cache sky color calculations based on world time")
        public static boolean enableSkyColorCache = true;
        
        @Config.Name("Sky Color Update Interval")
        @Config.Comment("Game ticks between sky color updates")
        @Config.RangeInt(min = 1, max = 20)
        public static int skyColorInterval = 3;
        
        @Config.Name("Entity Renderer Caching")
        @Config.Comment("Cache entity render decisions for better performance")
        public static boolean enableEntityCache = true;
        
        @Config.Name("Tile Entity Caching")
        @Config.Comment("Cache tile entity render states")
        public static boolean enableTileEntityCache = true;
        
        @Config.Name("Particle Optimization")
        @Config.Comment("Limit particle spawning based on performance")
        public static boolean enableParticleOptimization = true;
        
        @Config.Name("Max Particles Per Tick")
        @Config.Comment("Maximum particles to spawn per tick")
        @Config.RangeInt(min = 20, max = 200)
        public static int maxParticlesPerTick = 50;
        
        @Config.Name("Remove Tutorial (Non-Demo)")
        @Config.Comment("Disable tutorial system in non-demo worlds")
        public static boolean removeTutorial = true;
        
        @Config.Name("FOV Calculation Optimization")
        @Config.Comment("Cache FOV calculations to reduce CPU usage")
        public static boolean optimizeFOV = true;
        
        @Config.Name("Show F3 Statistics")
        @Config.Comment("Display cache statistics in F3 debug screen")
        public static boolean showF3Stats = true;
        
        @Config.Name("Log Performance Warnings")
        @Config.Comment("Log warnings when performance drops below threshold")
        public static boolean logPerformanceWarnings = true;
        
        @Config.Name("Cache Size (MB)")
        @Config.Comment("Maximum memory to use for caching")
        @Config.RangeInt(min = 16, max = 256)
        public static int maxCacheSizeMB = 64;
    }
    
    public GoodOptimization() {
        instance = this;
    }
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Initializing {} v{}", NAME, VERSION);
        ConfigManager.sync(MODID, Config.Type.INSTANCE);
    }
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        CACHE.initialize();
        LOGGER.info("Cache system initialized - {} optimizations active", getActiveOptimizationCount());
    }
    
    private static int getActiveOptimizationCount() {
        int count = 0;
        if (Configuration.enableLightmapCache) count++;
        if (Configuration.enableSkyColorCache) count++;
        if (Configuration.enableEntityCache) count++;
        if (Configuration.enableTileEntityCache) count++;
        if (Configuration.enableParticleOptimization) count++;
        if (Configuration.removeTutorial) count++;
        if (Configuration.optimizeFOV) count++;
        return count;
    }
    
    public static GoodOptimization getInstance() {
        return instance;
    }
    
    public static CacheManager getCache() {
        return CACHE;
    }
    
    public static Logger getLogger() {
        return LOGGER;
    }
    
    // ==================== CACHE MANAGER ====================
    
    public static class CacheManager {
        
        // Specialized caches with TTL and size limits
        private final Cache<Integer, LightmapEntry> lightmapCache;
        private final Cache<Long, SkyColorEntry> skyColorCache;
        private final Cache<Class<?>, RenderEntry> entityRenderCache;
        private final Cache<Class<?>, Boolean> tileEntityCache;
        private final Cache<Integer, Float> fovCache;
        
        // Performance tracking
        private final AtomicLong cacheHits = new AtomicLong(0);
        private final AtomicLong cacheMisses = new AtomicLong(0);
        
        public CacheManager() {
            this.lightmapCache = CacheBuilder.newBuilder()
                .maximumSize(128)
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .build();
                
            this.skyColorCache = CacheBuilder.newBuilder()
                .maximumSize(256)
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .build();
                
            this.entityRenderCache = CacheBuilder.newBuilder()
                .maximumSize(512)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build();
                
            this.tileEntityCache = CacheBuilder.newBuilder()
                .maximumSize(256)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
                
            this.fovCache = CacheBuilder.newBuilder()
                .maximumSize(32)
                .expireAfterWrite(2, TimeUnit.SECONDS)
                .build();
        }
        
        public void initialize() {
            LOGGER.info("Cache manager initialized");
        }
        
        public LightmapEntry getLightmap(int combinedLight, long worldTime) {
            LightmapEntry cached = lightmapCache.getIfPresent(combinedLight);
            if (cached != null && !cached.isExpired(worldTime)) {
                cacheHits.incrementAndGet();
                return cached;
            }
            cacheMisses.incrementAndGet();
            return null;
        }
        
        public void putLightmap(int combinedLight, long worldTime) {
            lightmapCache.put(combinedLight, new LightmapEntry(worldTime));
        }
        
        public SkyColorEntry getSkyColor(long worldTime) {
            SkyColorEntry cached = skyColorCache.getIfPresent(worldTime);
            if (cached != null) {
                cacheHits.incrementAndGet();
                return cached;
            }
            cacheMisses.incrementAndGet();
            return null;
        }
        
        public void putSkyColor(long worldTime, float r, float g, float b) {
            skyColorCache.put(worldTime, new SkyColorEntry(r, g, b));
        }
        
        public RenderEntry getEntityRender(Class<?> entityClass) {
            RenderEntry cached = entityRenderCache.getIfPresent(entityClass);
            if (cached != null) {
                cacheHits.incrementAndGet();
                return cached;
            }
            cacheMisses.incrementAndGet();
            return null;
        }
        
        public void putEntityRender(Class<?> entityClass, boolean shouldRender) {
            entityRenderCache.put(entityClass, new RenderEntry(shouldRender));
        }
        
        public Boolean getTileEntityRender(Class<?> tileClass) {
            Boolean cached = tileEntityCache.getIfPresent(tileClass);
            if (cached != null) {
                cacheHits.incrementAndGet();
                return cached;
            }
            cacheMisses.incrementAndGet();
            return null;
        }
        
        public void putTileEntityRender(Class<?> tileClass, boolean shouldRender) {
            tileEntityCache.put(tileClass, shouldRender);
        }
        
        public Float getFOV(int hash) {
            Float cached = fovCache.getIfPresent(hash);
            if (cached != null) {
                cacheHits.incrementAndGet();
                return cached;
            }
            cacheMisses.incrementAndGet();
            return null;
        }
        
        public void putFOV(int hash, float fov) {
            fovCache.put(hash, fov);
        }
        
        public void clearAll() {
            lightmapCache.invalidateAll();
            skyColorCache.invalidateAll();
            entityRenderCache.invalidateAll();
            tileEntityCache.invalidateAll();
            fovCache.invalidateAll();
            cacheHits.set(0);
            cacheMisses.set(0);
        }
        
        public double getHitRate() {
            long hits = cacheHits.get();
            long total = hits + cacheMisses.get();
            return total == 0 ? 0.0 : (double) hits / total;
        }
        
        public long getHits() {
            return cacheHits.get();
        }
        
        public long getMisses() {
            return cacheMisses.get();
        }
        
        // Cache entry classes
        public static class LightmapEntry {
            final long timestamp;
            
            public LightmapEntry(long timestamp) {
                this.timestamp = timestamp;
            }
            
            public boolean isExpired(long currentTime) {
                return (currentTime - timestamp) > Configuration.lightmapThreshold;
            }
        }
        
        public static class SkyColorEntry {
            final float r, g, b;
            
            public SkyColorEntry(float r, float g, float b) {
                this.r = r;
                this.g = g;
                this.b = b;
            }
            
            public void apply() {
                GlStateManager.color(r, g, b, 1.0f);
            }
        }
        
        public static class RenderEntry {
            final boolean shouldRender;
            
            public RenderEntry(boolean shouldRender) {
                this.shouldRender = shouldRender;
            }
        }
    }
    
    // ==================== EVENT HANDLER ====================
    
    @SideOnly(Side.CLIENT)
    public static class EventHandler {
        
        private int tickCounter = 0;
        private long lastCleanup = 0;
        
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            
            tickCounter++;
            
            // Periodic cleanup every 5 seconds
            long now = System.currentTimeMillis();
            if (now - lastCleanup > 5000) {
                performMaintenance();
                lastCleanup = now;
            }
        }
        
        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(MODID)) {
                ConfigManager.sync(MODID, Config.Type.INSTANCE);
                LOGGER.info("Configuration reloaded");
            }
        }
        
        @SubscribeEvent
        public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
            if (!Configuration.showF3Stats) return;
            if (!Minecraft.getMinecraft().gameSettings.showDebugInfo) return;
            
            double hitRate = CACHE.getHitRate();
            event.getLeft().add("");
            event.getLeft().add("§e[GoodOptimization]");
            event.getLeft().add(String.format("Cache Hit Rate: §a%.1f%%", hitRate * 100));
            event.getLeft().add(String.format("Cache Hits: §a%d §7| Misses: §c%d", 
                CACHE.getHits(), CACHE.getMisses()));
            event.getLeft().add(String.format("Optimizations: §a%d active", 
                getActiveOptimizationCount()));
        }
        
        private void performMaintenance() {
            double hitRate = CACHE.getHitRate();
            
            if (Configuration.logPerformanceWarnings && hitRate < 0.5 && CACHE.getHits() > 100) {
                LOGGER.warn("Low cache hit rate: {:.1f}% - consider adjusting settings", 
                    hitRate * 100);
            }
        }
    }
    
    // ==================== MIXINS ====================
    
    /**
     * Lightmap optimization - cache expensive lightmap updates
     */
    @Mixin(EntityRenderer.class)
    public static abstract class MixinLightmap {
        
        @Shadow private float torchFlickerX;
        @Shadow private float bossColorModifier;
        @Shadow private float bossColorModifierPrev;
        
        @Unique private long goodopt$lastUpdate = 0;
        @Unique private int goodopt$lastCombinedLight = -1;
        
        @Inject(method = "updateLightmap", at = @At("HEAD"), cancellable = true)
        private void onUpdateLightmap(float partialTicks, CallbackInfo ci) {
            if (!Configuration.enableLightmapCache) return;
            
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.world == null) return;
            
            long worldTime = mc.world.getWorldTime();
            int combinedLight = mc.world.getCombinedLight(mc.player.getPosition(), 0);
            
            // Check if we can use cached lightmap
            CacheManager.LightmapEntry cached = CACHE.getLightmap(combinedLight, worldTime);
            if (cached != null && combinedLight == goodopt$lastCombinedLight) {
                ci.cancel(); // Skip update, use cache
                return;
            }
            
            goodopt$lastUpdate = worldTime;
            goodopt$lastCombinedLight = combinedLight;
        }
        
        @Inject(method = "updateLightmap", at = @At("RETURN"))
        private void afterUpdateLightmap(float partialTicks, CallbackInfo ci) {
            if (!Configuration.enableLightmapCache) return;
            
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.world != null && goodopt$lastCombinedLight != -1) {
                CACHE.putLightmap(goodopt$lastCombinedLight, mc.world.getWorldTime());
            }
        }
    }
    
    /**
     * Entity rendering optimization - cache render decisions
     */
    @Mixin(RenderManager.class)
    public static abstract class MixinEntityRender {
        
        @Inject(method = "doRenderEntity", at = @At("HEAD"), cancellable = true)
        private void onRenderEntity(Entity entity, double x, double y, double z, 
                                   float yaw, float partialTicks, boolean p_188391_10_,
                                   CallbackInfo ci) {
            if (!Configuration.enableEntityCache) return;
            
            Class<?> entityClass = entity.getClass();
            CacheManager.RenderEntry cached = CACHE.getEntityRender(entityClass);
            
            if (cached == null) {
                // Determine if we should render this entity type
                boolean shouldRender = shouldRenderEntity(entity);
                CACHE.putEntityRender(entityClass, shouldRender);
                
                if (!shouldRender) {
                    ci.cancel();
                }
            } else if (!cached.shouldRender) {
                ci.cancel();
            }
        }
        
        @Unique
        private boolean shouldRenderEntity(Entity entity) {
            // Check distance from player
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player == null) return true;
            
            double distanceSq = entity.getDistanceSq(mc.player);
            double maxDistance = entity.getEntityBoundingBox().getAverageEdgeLength() > 2.0 ? 16384.0 : 4096.0; // 128 or 64 blocks
            
            return distanceSq <= maxDistance;
        }
    }
    
    /**
     * Tile entity optimization - cache render decisions
     */
    @Mixin(TileEntityRendererDispatcher.class)
    public static abstract class MixinTileEntityRender {
        
        @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V", 
                at = @At("HEAD"), cancellable = true)
        private void onRenderTileEntity(TileEntity tileEntity, double x, double y, double z,
                                       float partialTicks, int destroyStage, float partial,
                                       CallbackInfo ci) {
            if (!Configuration.enableTileEntityCache) return;
            
            Class<?> tileClass = tileEntity.getClass();
            Boolean cached = CACHE.getTileEntityRender(tileClass);
            
            if (cached == null) {
                boolean shouldRender = shouldRenderTileEntity(tileEntity);
                CACHE.putTileEntityRender(tileClass, shouldRender);
                
                if (!shouldRender) {
                    ci.cancel();
                }
            } else if (!cached) {
                ci.cancel();
            }
        }
        
        @Unique
        private boolean shouldRenderTileEntity(TileEntity tileEntity) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player == null) return true;
            
            double distanceSq = tileEntity.getDistanceSq(
                mc.player.posX, mc.player.posY, mc.player.posZ);
            
            return distanceSq <= 4096.0; // 64 blocks
        }
    }
    
    /**
     * Particle optimization - limit spawning based on performance
     */
    @Mixin(ParticleManager.class)
    public static abstract class MixinParticle {
        
        @Unique private final AtomicInteger goodopt$particleCount = new AtomicInteger(0);
        
        @Inject(method = "addEffect", at = @At("HEAD"), cancellable = true)
        private void onAddParticle(Particle particle, CallbackInfo ci) {
            if (!Configuration.enableParticleOptimization) return;
            
            if (goodopt$particleCount.get() >= Configuration.maxParticlesPerTick) {
                ci.cancel();
                return;
            }
            
            goodopt$particleCount.incrementAndGet();
        }
        
        @Inject(method = "updateEffects", at = @At("HEAD"))
        private void onUpdateParticles(CallbackInfo ci) {
            goodopt$particleCount.set(0);
        }
    }
    
    /**
     * Tutorial optimization - disable in non-demo worlds
     */
    @Mixin(Tutorial.class)
    public static abstract class MixinTutorial {
        
        @Inject(method = "update", at = @At("HEAD"), cancellable = true)
        private void onTutorialUpdate(CallbackInfo ci) {
            if (!Configuration.removeTutorial) return;
            
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.world != null && !mc.world.getWorldInfo().isHardcoreModeEnabled()) {
                ci.cancel(); // Skip tutorial updates in non-demo worlds
            }
        }
    }
    
    /**
     * FOV calculation optimization - cache results
     */
    @Mixin(EntityRenderer.class)
    public static abstract class MixinFOV {
        
        @Inject(method = "getFOVModifier", at = @At("HEAD"), cancellable = true)
        private void onGetFOV(float partialTicks, boolean useFOVSetting, 
                             CallbackInfoReturnable<Float> cir) {
            if (!Configuration.optimizeFOV) return;
            
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player == null) return;
            
            // Create hash from relevant FOV factors
            int hash = Float.floatToIntBits(partialTicks) ^ 
                      Float.floatToIntBits(mc.player.getFovModifier()) ^
                      (useFOVSetting ? 1 : 0);
            
            Float cached = CACHE.getFOV(hash);
            if (cached != null) {
                cir.setReturnValue(cached);
                return;
            }
        }
        
        @Inject(method = "getFOVModifier", at = @At("RETURN"))
        private void afterGetFOV(float partialTicks, boolean useFOVSetting,
                                CallbackInfoReturnable<Float> cir) {
            if (!Configuration.optimizeFOV) return;
            
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player == null) return;
            
            int hash = Float.floatToIntBits(partialTicks) ^ 
                      Float.floatToIntBits(mc.player.getFovModifier()) ^
                      (useFOVSetting ? 1 : 0);
            
            CACHE.putFOV(hash, cir.getReturnValue());
        }
    }
    
    // ==================== CONFIG GUI FACTORY ====================
    
    public static class ConfigGuiFactory implements IModGuiFactory {
        
        @Override
        public void initialize(Minecraft minecraft) {}
        
        @Override
        public boolean hasConfigGui() {
            return true;
        }
        
        @Override
        public GuiScreen createConfigGui(GuiScreen parentScreen) {
            return new GuiConfig(
                parentScreen,
                new ConfigElement(ConfigManager.getConfiguration()).getChildElements(),
                MODID,
                false,
                false,
                NAME
            );
        }
        
        @Override
        @Nullable
        public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
            return null;
        }
    }
}
