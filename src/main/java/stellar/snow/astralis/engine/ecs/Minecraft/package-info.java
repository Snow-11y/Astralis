/**
 * Minecraft ECS Integration Module (OPTIONAL)
 * 
 * This package contains Minecraft-specific optimizations and integrations for the
 * Snow's ECS system. The module is completely optional and can be deleted without
 * affecting the core ECS functionality.
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Chunk streaming optimization for entity loading/unloading</li>
 *   <li>Entity batching for Minecraft entity updates</li>
 *   <li>Redstone optimization using ECS patterns</li>
 *   <li>Spatial partitioning for Minecraft world</li>
 *   <li>Bridge between Minecraft entities and ECS entities</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
 * <p>
 * To use this module, ensure Minecraft APIs are available in your classpath.
 * If you're not using Minecraft, you can safely delete this entire folder.
 * </p>
 * 
 * <h2>Module Status:</h2>
 * <p>
 * This module is marked as {@code required=false}, meaning the ECS system will
 * continue to function normally even if this module is missing or if Minecraft
 * APIs are not available.
 * </p>
 * 
 * <h2>Dependencies:</h2>
 * <ul>
 *   <li>Core ECS module (required)</li>
 *   <li>Minecraft server/client APIs (optional)</li>
 * </ul>
 * 
 * <h2>Files in this module:</h2>
 * <ul>
 *   <li>{@code MinecraftChunkStreamer.java} - Chunk-based entity streaming</li>
 *   <li>{@code MinecraftEntityOptimizer.java} - Entity update optimization</li>
 *   <li>{@code MinecraftRedstoneOptimizer.java} - Redstone circuit optimization</li>
 *   <li>{@code MinecraftSpatialOptimizer.java} - Spatial partitioning</li>
 *   <li>{@code MinecraftECSBridge.java} - Bridge between MC and ECS</li>
 * </ul>
 * 
 * @see ecs.Minecraft.MinecraftModule
 * @since 1.0.0
 */
@ecs.Minecraft.MinecraftModule(
    version = "1.0.0",
    description = "Minecraft ECS integration with chunk streaming and entity optimization",
    required = false,
    minecraftVersion = "1.20+"
)
package ecs.Minecraft;
