package stellar.snow.astralis.mixins.core;

import stellar.snow.astralis.patcher.UniversalPatcher;
import net.minecraft.crash.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

/**
 * Mixin for CrashReport - Emergency GPU Resource Release
 * Ensures Vulkan device is properly released on crash
 */
@Mixin(CrashReport.class)
public class MixinCrashReport {

    /**
     * Intercept crash report creation to begin emergency shutdown
     */
    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void onCrashReportCreated(String descriptionIn, Throwable causeThrowable, CallbackInfo ci) {
        System.err.println("[Astralis] Crash detected: " + descriptionIn);
        UniversalPatcher.onCrashDetected(descriptionIn);
    }

    /**
     * Ensures GPU resources are released before crash report is saved
     */
    @Inject(
        method = "saveToFile",
        at = @At("HEAD")
    )
    private void onSaveToFile(File toFile, CallbackInfoReturnable<Boolean> cir) {
        System.out.println("[Astralis] Crash report being saved, ensuring GPU cleanup...");
        UniversalPatcher.abortFrame();
    }

    /**
     * Hook into complete crash report to add Astralis state
     */
    @Inject(
        method = "getCompleteReport",
        at = @At("TAIL")
    )
    private void onGetCompleteReport(CallbackInfoReturnable<String> cir) {
        // Could append Astralis debug info to crash report here
    }
}
