package stellar.snow.astralis.mixins.accessor;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * IWorldAccessor - Direct field access for World class.
 */
@Mixin(World.class)
public interface IWorldAccessor {

    @Accessor("loadedEntityList")
    List<Entity> astralis$getLoadedEntityList();

    @Accessor("unloadedEntityList")
    List<Entity> astralis$getUnloadedEntityList();

    @Accessor("updateLCG")
    int astralis$getUpdateLCG();

    @Accessor("updateLCG")
    void astralis$setUpdateLCG(int value);
}
