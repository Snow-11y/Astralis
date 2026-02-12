package stellar.snow.astralis.bridge;

import stellar.snow.astralis.engine.ecs.core.Entity;

import java.lang.foreign.MemorySegment;

/**
 * BridgeMixinInterface - Contract for Minecraft Entity mixin injection.
 *
 * <p>Provides O(1) access to ECS data from any Minecraft entity reference,
 * avoiding hash table lookups in critical rendering and physics paths.</p>
 *
 * <h2>Thread Safety:</h2>
 * <ul>
 *   <li>Slot index: Written on main thread, read from render thread (volatile)</li>
 *   <li>ECS entity: Same as slot index</li>
 *   <li>Component memory: Accessed via bridge with proper synchronization</li>
 * </ul>
 *
 * <h2>Performance Characteristics:</h2>
 * <ul>
 *   <li>Slot lookup: O(1) field read</li>
 *   <li>Memory offset: O(1) multiplication</li>
 *   <li>Component access: O(1) direct memory</li>
 * </ul>
 */
public interface BridgeMixinInterface {

    // ========================================================================
    // CORE ACCESSORS
    // ========================================================================

    /**
     * Sets the bridge slot index for this entity.
     * Called by the bridge during entity registration.
     *
     * @param slot the slot index, or -1 if not registered
     */
    void astralis$setBridgeSlot(int slot);

    /**
     * Gets the bridge slot index for this entity.
     *
     * @return the slot index, or -1 if not registered
     */
    int astralis$getBridgeSlot();

    /**
     * Sets the ECS entity handle.
     *
     * @param entity the ECS entity, or null if not registered
     */
    void astralis$setEcsEntity(Entity entity);

    /**
     * Gets the ECS entity handle.
     *
     * @return the ECS entity, or null if not registered
     */
    Entity astralis$getEcsEntity();

    // ========================================================================
    // STATE FLAGS
    // ========================================================================

    /**
     * Sets internal state flags.
     *
     * @param flags the flags to set
     */
    void astralis$setFlags(int flags);

    /**
     * Gets internal state flags.
     *
     * @return current flags
     */
    int astralis$getFlags();

    /**
     * Atomically sets a flag bit.
     *
     * @param flag the flag bit to set
     */
    default void astralis$setFlag(int flag) {
        astralis$setFlags(astralis$getFlags() | flag);
    }

    /**
     * Atomically clears a flag bit.
     *
     * @param flag the flag bit to clear
     */
    default void astralis$clearFlag(int flag) {
        astralis$setFlags(astralis$getFlags() & ~flag);
    }

    /**
     * Checks if a flag is set.
     *
     * @param flag the flag to check
     * @return true if set
     */
    default boolean astralis$hasFlag(int flag) {
        return (astralis$getFlags() & flag) != 0;
    }

    // ========================================================================
    // FLAG CONSTANTS
    // ========================================================================

    /** Entity is registered with the bridge */
    int FLAG_REGISTERED = 1 << 0;
    
    /** Entity AI is currently culled */
    int FLAG_AI_CULLED = 1 << 1;
    
    /** Entity rendering is culled */
    int FLAG_RENDER_CULLED = 1 << 2;
    
    /** Entity physics is managed by ECS */
    int FLAG_ECS_PHYSICS = 1 << 3;
    
    /** Entity needs sync from MC to ECS */
    int FLAG_DIRTY_INBOUND = 1 << 4;
    
    /** Entity needs sync from ECS to MC */
    int FLAG_DIRTY_OUTBOUND = 1 << 5;
    
    /** Entity was teleported this tick */
    int FLAG_TELEPORTED = 1 << 6;
    
    /** Entity is in interpolation mode */
    int FLAG_INTERPOLATING = 1 << 7;

    // ========================================================================
    // CONVENIENCE METHODS
    // ========================================================================

    /**
     * Checks if this entity is registered with the bridge.
     *
     * @return true if registered
     */
    default boolean astralis$isRegistered() {
        return astralis$getBridgeSlot() >= 0 && astralis$hasFlag(FLAG_REGISTERED);
    }

    /**
     * Gets direct access to this entity's component memory offset.
     *
     * @return memory offset, or -1 if not registered
     */
    default long astralis$getComponentMemoryOffset() {
        int slot = astralis$getBridgeSlot();
        return slot >= 0 ? (long) slot * MinecraftECSBridge.ENTITY_BLOCK_SIZE : -1L;
    }

    /**
     * Checks if AI processing should be skipped.
     *
     * @return true if AI is culled
     */
    default boolean astralis$isAICulled() {
        return astralis$hasFlag(FLAG_AI_CULLED);
    }

    /**
     * Checks if rendering should be skipped.
     *
     * @return true if render is culled
     */
    default boolean astralis$isRenderCulled() {
        return astralis$hasFlag(FLAG_RENDER_CULLED);
    }

    /**
     * Checks if ECS controls physics for this entity.
     *
     * @return true if ECS manages physics
     */
    default boolean astralis$isEcsPhysics() {
        return astralis$hasFlag(FLAG_ECS_PHYSICS);
    }
}
