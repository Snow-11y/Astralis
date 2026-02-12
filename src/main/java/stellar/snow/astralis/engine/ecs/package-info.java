/**
 * Astralis ECS - High-Performance Entity-Component-System Framework
 * 
 * <h1>Overview</h1>
 * A production-ready ECS implementation featuring:
 * <ul>
 *   <li>Zero-cost abstractions with Java 21+ Foreign Memory API</li>
 *   <li>Lock-free concurrent entity operations</li>
 *   <li>Cache-friendly Structure-of-Arrays (SoA) layout</li>
 *   <li>Automatic parallel system scheduling</li>
 *   <li>Comprehensive component and system library</li>
 * </ul>
 * 
 * <h1>Architecture</h1>
 * 
 * <h2>Core ({@link stellar.snow.astralis.engine.ecs.core})</h2>
 * <ul>
 *   <li>{@link stellar.snow.astralis.engine.ecs.core.World} - Main ECS container</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.core.Entity} - Lightweight entity handle</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.core.Archetype} - Component storage</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.core.SnowySystem} - System base class</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.core.SystemScheduler} - Parallel execution</li>
 * </ul>
 * 
 * <h2>Components ({@link stellar.snow.astralis.engine.ecs.components})</h2>
 * <ul>
 *   <li>{@link stellar.snow.astralis.engine.ecs.components.Transform} - Position, rotation, scale</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.components.Velocity} - Movement and rotation</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.components.Health} - HP and damage</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.components.Lifetime} - Timed destruction</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.components.Tag} - 64-bit categorization</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.components.RenderInfo} - Rendering properties</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.components.Parent} - Entity hierarchy</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.components.Children} - Child tracking</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.components.AABB} - Collision bounds</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.components.Name} - Debug names</li>
 * </ul>
 * 
 * <h2>Systems ({@link stellar.snow.astralis.engine.ecs.systems})</h2>
 * <ul>
 *   <li>{@link stellar.snow.astralis.engine.ecs.systems.PhysicsSystem} - Movement integration</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.systems.LifetimeSystem} - Entity destruction</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.systems.HealthRegenSystem} - Health regeneration</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.systems.HierarchySystem} - Transform propagation</li>
 * </ul>
 * 
 * <h2>Utilities ({@link stellar.snow.astralis.engine.ecs.util})</h2>
 * <ul>
 *   <li>{@link stellar.snow.astralis.engine.ecs.util.EntityBuilder} - Fluent entity creation</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.util.QueryBuilder} - Fluent queries</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.util.ECSProfiler} - Performance monitoring</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.util.EntityCommandBuffer} - Deferred operations</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.util.SpatialIndex} - Spatial queries</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.util.WorldStatistics} - ECS metrics</li>
 * </ul>
 * 
 * <h2>Events ({@link stellar.snow.astralis.engine.ecs.events})</h2>
 * <ul>
 *   <li>{@link stellar.snow.astralis.engine.ecs.events.EventBus} - Decoupled messaging</li>
 * </ul>
 * 
 * <h2>Prefabs ({@link stellar.snow.astralis.engine.ecs.prefab})</h2>
 * <ul>
 *   <li>{@link stellar.snow.astralis.engine.ecs.prefab.PrefabRegistry} - Entity templates</li>
 * </ul>
 * 
 * <h2>Storage ({@link stellar.snow.astralis.engine.ecs.storage})</h2>
 * <ul>
 *   <li>{@link stellar.snow.astralis.engine.ecs.storage.ComponentArray} - Packed component data</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.storage.ComponentRegistry} - Type management</li>
 *   <li>{@link stellar.snow.astralis.engine.ecs.storage.Query} - Entity queries</li>
 * </ul>
 * 
 * <h1>Quick Example</h1>
 * <pre>
 * // Create world
 * World world = new World();
 * world.addSystem(new PhysicsSystem());
 * world.addSystem(new LifetimeSystem());
 * 
 * // Create entity
 * Entity player = EntityBuilder.create(world)
 *     .withTransform(0, 0, 0)
 *     .withVelocity(0, 0, 0)
 *     .withHealth(100, 5.0f)
 *     .withTag(Tag.PLAYER)
 *     .build();
 * 
 * // Game loop
 * while (running) {
 *     world.update(deltaTime);
 * }
 * </pre>
 * 
 * <h1>Performance</h1>
 * <ul>
 *   <li><b>Memory:</b> Compact SoA layout, 8-40 bytes per component</li>
 *   <li><b>Speed:</b> Lock-free operations, parallel systems</li>
 *   <li><b>Cache:</b> Linear iteration, prefetch-friendly</li>
 *   <li><b>Scale:</b> Tested with 100k+ entities</li>
 * </ul>
 * 
 * @author Astralis ECS Team
 * @version 2.0.0
 * @since Java 21
 */
package stellar.snow.astralis.engine.ecs;
