package stellar.snow.astralis.mixins.core;

import stellar.snow.astralis.bridge.MinecraftECSBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MixinMinecraft - Core game loop integration.
 *
 * <h2>Injection Points:</h2>
 * <ul>
 *   <li>startGame: Initialize ECS bridge</li>
 *   <li>runTick: Drive ECS tick processing</li>
 *   <li>runGameLoop: Handle render tick</li>
 *   <li>shutdownMinecraftApplet: Cleanup bridge</li>
 * </ul>
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Unique
    private static final Logger astralis$LOGGER = Logger.getLogger("Astralis-MixinMinecraft");

    // ========================================================================
    // SHADOWS
    // ========================================================================

    @Shadow public WorldClient theWorld;
    @Shadow @Final public Profiler mcProfiler;
    @Shadow public boolean isGamePaused;
    @Shadow private float renderPartialTicksPaused;

    @Shadow public abstract float getRenderPartialTicks();

    // ========================================================================
    // LIFECYCLE STATE
    // ========================================================================

    @Unique
    private boolean astralis$initialized = false;

    @Unique
    private long astralis$lastTickTime = 0L;

    @Unique
    private float astralis$accumulatedTime = 0.0f;

    // Fixed timestep for physics (50 Hz = 20ms per tick, matching MC)
    @Unique
    private static final float FIXED_TIMESTEP = 0.02f;

    @Unique
    private static final float MAX_FRAME_TIME = 0.25f; // Prevent spiral of death

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    /**
     * Initialize ECS bridge after Minecraft finishes startup.
     * Placed at RETURN to ensure all MC systems are ready.
     */
    @Inject(
        method = "startGame",
        at = @At("RETURN")
    )
    private void astralis$onStartGame(CallbackInfo ci) {
        if (astralis$initialized) return;

        try {
            astralis$LOGGER.info("[Astralis] Initializing ECS Bridge...");
            
            MinecraftECSBridge bridge = MinecraftECSBridge.getInstance();
            bridge.initialize();
            
            astralis$initialized = true;
            astralis$lastTickTime = System.nanoTime();
            
            astralis$LOGGER.info("[Astralis] ECS Bridge initialized successfully");
            
        } catch (Exception e) {
            astralis$LOGGER.log(Level.SEVERE, "[Astralis] Failed to initialize ECS Bridge", e);
            // Don't crash the game, but log the error
        }
    }

    // ========================================================================
    // TICK PROCESSING
    // ========================================================================

    /**
     * Drive ECS tick at the start of MC's tick loop.
     * Uses fixed timestep accumulator for consistent physics.
     */
    @Inject(
        method = "runTick",
        at = @At("HEAD")
    )
    private void astralis$onTickStart(CallbackInfo ci) {
        if (!astralis$initialized || isGamePaused) return;

        MinecraftECSBridge bridge = astralis$getBridgeSafe();
        if (bridge == null) return;

        mcProfiler.startSection("astralis_tick");

        try {
            // Calculate delta time
            long now = System.nanoTime();
            float deltaTime = (now - astralis$lastTickTime) / 1_000_000_000.0f;
            astralis$lastTickTime = now;

            // Clamp to prevent spiral of death
            deltaTime = Math.min(deltaTime, MAX_FRAME_TIME);

            // Accumulate time
            astralis$accumulatedTime += deltaTime;

            // Process fixed timesteps
            int steps = 0;
            while (astralis$accumulatedTime >= FIXED_TIMESTEP && steps < 4) {
                bridge.onClientTick(FIXED_TIMESTEP);
                astralis$accumulatedTime -= FIXED_TIMESTEP;
                steps++;
            }

            // If we're still behind, drain the accumulator to prevent spiral
            if (astralis$accumulatedTime > FIXED_TIMESTEP * 2) {
                astralis$LOGGER.fine("[Astralis] Draining time accumulator: " + astralis$accumulatedTime);
                astralis$accumulatedTime = 0;
            }

        } catch (Exception e) {
            astralis$LOGGER.log(Level.WARNING, "[Astralis] Tick processing error", e);
        } finally {
            mcProfiler.endSection();
        }
    }

    /**
     * Post-tick hook for any cleanup or state validation.
     */
    @Inject(
        method = "runTick",
        at = @At("RETURN")
    )
    private void astralis$onTickEnd(CallbackInfo ci) {
        if (!astralis$initialized) return;

        // Optional: Validate state, process deferred operations
        // This runs after MC has finished its tick
    }

    // ========================================================================
    // RENDER TICK
    // ========================================================================

    /**
     * Hook into the render loop for interpolation updates.
     * This ensures smooth visuals between physics ticks.
     */
    @Inject(
        method = "runGameLoop",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/EntityRenderer;updateCameraAndRender(FJ)V",
            shift = At.Shift.BEFORE
        )
    )
    private void astralis$onPreRender(CallbackInfo ci) {
        if (!astralis$initialized) return;

        MinecraftECSBridge bridge = astralis$getBridgeSafe();
        if (bridge == null) return;

        float partialTicks = isGamePaused ? renderPartialTicksPaused : getRenderPartialTicks();

        mcProfiler.startSection("astralis_render");
        try {
            bridge.onRenderTick(partialTicks);
        } catch (Exception e) {
            astralis$LOGGER.log(Level.FINE, "[Astralis] Render tick error", e);
        } finally {
            mcProfiler.endSection();
        }
    }

    // ========================================================================
    // WORLD LOADING/UNLOADING
    // ========================================================================

    /**
     * Handle world changes (loading new world / disconnecting).
     */
    @Inject(
        method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",
        at = @At("HEAD")
    )
    private void astralis$onWorldLoad(WorldClient world, String message, CallbackInfo ci) {
        if (!astralis$initialized) return;

        MinecraftECSBridge bridge = astralis$getBridgeSafe();
        if (bridge == null) return;

        if (world == null) {
            // World is being unloaded
            astralis$LOGGER.info("[Astralis] World unloading, pausing bridge");
            bridge.pause();
        } else {
            // New world is loading
            astralis$LOGGER.info("[Astralis] World loading, resuming bridge");
            bridge.resume();
        }
    }

    // ========================================================================
    // SHUTDOWN
    // ========================================================================

    /**
     * Clean up ECS bridge on game shutdown.
     */
    @Inject(
        method = "shutdownMinecraftApplet",
        at = @At("HEAD")
    )
    private void astralis$onShutdown(CallbackInfo ci) {
        if (!astralis$initialized) return;

        astralis$LOGGER.info("[Astralis] Shutting down ECS Bridge...");

        try {
            MinecraftECSBridge bridge = MinecraftECSBridge.getInstance();
            bridge.close();
        } catch (Exception e) {
            astralis$LOGGER.log(Level.WARNING, "[Astralis] Error during bridge shutdown", e);
        }

        astralis$initialized = false;
    }

    // ========================================================================
    // UTILITIES
    // ========================================================================

    @Unique
    private MinecraftECSBridge astralis$getBridgeSafe() {
        try {
            MinecraftECSBridge bridge = MinecraftECSBridge.getInstance();
            return bridge.isRunning() ? bridge : null;
        } catch (Exception e) {
            return null;
        }
    }
}
