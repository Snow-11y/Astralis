package stellar.snow.astralis.mixins.accessor;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * IMinecraftAccessor - Direct field access for Minecraft class.
 */
@Mixin(Minecraft.class)
public interface IMinecraftAccessor {

    @Accessor("timer")
    Timer astralis$getTimer();

    @Accessor("fpsCounter")
    int astralis$getFpsCounter();

    @Accessor("debugFPS")
    static int astralis$getDebugFPS() {
        throw new AssertionError();
    }
}
