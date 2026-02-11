package stellar.snow.astralis.mixins.accessor;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * IEntityAccessor - Direct field access for Entity class.
 */
@Mixin(Entity.class)
public interface IEntityAccessor {

    @Accessor("isInWeb")
    boolean astralis$isInWeb();

    @Accessor("isInWeb")
    void astralis$setInWeb(boolean value);

    @Accessor("fire")
    int astralis$getFire();

    @Accessor("fire")
    void astralis$setFire(int value);

    @Accessor("firstUpdate")
    boolean astralis$isFirstUpdate();

    @Accessor("entityUniqueID")
    java.util.UUID astralis$getUUID();
}
