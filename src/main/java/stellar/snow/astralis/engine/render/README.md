# Astralis Rendering Engine - Modder's Guide

## Introduction

Welcome to the Astralis Rendering Engine! This guide will help you understand and extend the engine safely.

## Architecture Overview

```
Public API (Simple, Safe)
    ↓
Integration Layer (Extensions, Configuration)
    ↓
GPU-Driven Systems (Culling, Indirect Draw)
    ↓
Core Systems (CullingManager, IndirectDrawManager)
    ↓
Debug & Stability (Validation, Profiling)
```

### Key Principle

**You don't need to understand the complex internals.** The public API is simple and hides the complexity.

## Quick Start

### Basic Usage

```java
// Create rendering API
var api = new RenderingAPI();

// Render a frame
api.renderFrame(world, entities, partialTicks);

// Get statistics
api.printStatistics();
```

That's it! The engine handles:
- Entity culling
- GPU-driven rendering
- Debug visualization
- Validation

### Custom Configuration

```java
var config = new RenderingAPI.Configuration.Builder()
    .validationLevel(ValidationLevel.PARANOID)  // Extra safety
    .enableDebugHUD(true)
    .enableProfiling(true)
    .build();

var api = new RenderingAPI(config);
```

## Extending the Engine

### Creating a Custom Render Pass

```java
public class MyWaterEffect extends RenderingAPI.RenderingExtension {
    @Override
    public void onPreRender(World world, List<Entity> entities, float partialTicks) {
        // Setup water rendering
        setupWaterShader();
        bindWaterTextures();
    }
    
    @Override
    public void onPostRender() {
        // Cleanup
        unbindWaterTextures();
    }
}

// Register extension
api.registerExtension(new MyWaterEffect());
```

### Adding Debug Visualizations

```java
var debug = api.getDebugSystem();

// Draw 3D shapes
debug.getGizmosManager().drawSphere(position, radius, color);
debug.getGizmosManager().drawAABB(minPos, maxPos, color);
debug.getGizmosManager().drawLine(start, end, color);

// Add HUD elements
var hudContext = debug.getHUDManager().createContext("water", "Water Effects");
hudContext.addValueWatch("wave_height", "Wave Height", () -> 
    String.format("%.2f", getCurrentWaveHeight()));
```

### Using the Profiler

```java
var profiler = api.getDebugSystem().getProfiler();

// Start profiling
profiler.startProfiling();

// Profile sections
profiler.beginSection("my_expensive_code");
doExpensiveWork();
profiler.endSection("my_expensive_code");

// Get report
profiler.stopProfiling();
System.out.println(profiler.generateReport());
```

## Understanding the Systems

### 1. Culling System

**What it does:** Decides which entities to render based on distance, visibility, etc.

**How to use:**
```java
var cullingSystem = new CullingSystem();
var result = cullingSystem.cullEntities(world, entities);

// Result contains:
// - result.fullDetail: Entities to render with full quality
// - result.reducedDetail: Entities to render with reduced quality
// - result.minimal: Entities to render with minimal quality
// - result.culled: Entities that should not be rendered
```

**Under the hood:** Uses LO's `CullingManager` with temporal smoothing, hysteresis, and FMA precision. You don't need to know this - it just works.

### 2. Indirect Draw Bridge

**What it does:** Manages GPU-driven rendering for high performance.

**How to use:**
```java
var drawBridge = new IndirectDrawBridge();

drawBridge.beginFrame();

// Add instances
int instanceId = drawBridge.addInstance(
    meshId,      // Which mesh to render
    transform,   // Where to render it
    materialId,  // What material to use
    flags        // Rendering flags
);

// Execute GPU rendering
drawBridge.executeCullingAndDraw(cmdBuffer, viewMatrix, projMatrix, cullFlags);

drawBridge.endFrame();
```

**Under the hood:** Uses LO's `IndirectDrawManager` with lock-free allocation, GPU culling, HiZ occlusion. Again, you don't need to know this.

### 3. Debug System

**What it does:** Provides human-centric debugging tools.

**Features:**
- Real-time HUD with live statistics
- 3D gizmos for visual debugging
- Performance profiler with flamegraphs
- Command console for runtime control
- Automatic anomaly detection

**Why this matters:** Kirino wins here by being debuggable. We match that and go further.

### 4. Stability Layer

**What it does:** Catches bugs early before they become "heisenbugs."

**Validation levels:**
- `NONE`: No validation (production)
- `FAST`: Light checks (<1% overhead)
- `NORMAL`: Standard checks (<5% overhead)
- `PARANOID`: Everything (development only)

**What it catches:**
- Invariant violations
- Thread safety issues
- Memory corruption
- Resource leaks
- Invalid state transitions

## Common Patterns

### Pattern 1: Adding a Custom Render Pass

```java
public class BloomEffect extends RenderingAPI.RenderingExtension {
    private long bloomBuffer;
    private long blurBuffer;
    
    @Override
    public void onPostRender() {
        // Render bloom after main scene
        extractBrightPixels();
        blurBrightPixels();
        compositeBloom();
    }
}
```

### Pattern 2: Custom Entity Culling

```java
// Override culling for specific entities
var cullResult = cullingSystem.cullEntities(world, entities);

// Force certain entities to always render
for (Entity important : importantEntities) {
    cullResult.fullDetail.add(important);
    cullResult.culled.remove(important);
}
```

### Pattern 3: Debug Watches

```java
// Watch a value in the debug HUD
api.getDebugSystem().addWatch("player_health", () -> 
    player.getHealth() + "/" + player.getMaxHealth());

// Execute commands
var console = api.getDebugSystem().getConsole();
console.executeCommand("stats");
console.executeCommand("profile_dump");
```

## Performance Tips

### 1. Use Validation Wisely

```java
// Production: No validation
var prodConfig = new Configuration.Builder()
    .validationLevel(ValidationLevel.NONE)
    .build();

// Development: Full validation
var devConfig = new Configuration.Builder()
    .validationLevel(ValidationLevel.PARANOID)
    .build();
```

### 2. Profile Before Optimizing

```java
profiler.startProfiling();

// Your code here

profiler.stopProfiling();
String report = profiler.generateReport();

// Find the actual bottleneck
```

### 3. Let the Culling System Work

Don't try to manually cull entities. The culling system uses:
- Temporal smoothing (prevents flicker)
- Hysteresis (stable tier transitions)
- Cache optimization (>99% hit rate)

Just trust it to work.

## Troubleshooting

### Problem: Low FPS

**Check:**
1. Profiler report - find bottleneck
2. Culling statistics - ensure culling is working
3. Anomaly detector - check for warnings

```java
api.printStatistics();
api.generateDiagnosticReport();
```

### Problem: Entities Flickering

**Cause:** Fighting the culling system.

**Solution:** Don't manually cull. Let the temporal smoothing work.

### Problem: Memory Leak

**Check:**
```java
var validator = api.getValidator();
List<ResourceLeak> leaks = validator.detectLeaks();

for (ResourceLeak leak : leaks) {
    System.out.println(leak.describe());
}
```

### Problem: Validation Failures

**Check:**
```java
var failures = validator.getRecentFailures(10);
for (ValidationFailure failure : failures) {
    System.out.println(failure.describe());
}
```

## Advanced Topics

### Understanding Validation Levels

```
NONE: 
  - No overhead
  - Production use

FAST (<1% overhead):
  - Null checks
  - Positive value checks
  - Basic invariants

NORMAL (<5% overhead):
  - All FAST checks
  - Thread safety validation
  - Memory bounds checking
  - State machine validation

PARANOID (>10% overhead):
  - All NORMAL checks
  - Resource leak detection
  - Full stack traces
  - Development only
```

### Custom Validators

```java
// Add custom validation
validator.checkInvariant(myCondition, "My condition failed");
validator.checkRange(value, 0, 100, "value");
validator.checkNotNull(object, "object");
```

## Comparing to Kirino

### What Kirino Does Better

1. **Simplicity:** Kirino's FSM is simpler than our DAG
   - **Our solution:** Public API hides DAG complexity
   
2. **Debuggability:** Kirino has great HUD
   - **Our solution:** Even better HUD + profiler + console
   
3. **Predictability:** Kirino's FSM is obviously correct
   - **Our solution:** Stability validator catches bugs

### What We Do Better

1. **Performance:** 10-100x faster resource access
2. **Culling:** Temporal smoothing, hysteresis, FMA precision
3. **GPU-Driven:** Indirect draw, HiZ occlusion, lock-free
4. **Validation:** Comprehensive error detection

## FAQ

**Q: Do I need to understand VarHandles and MemorySegments?**  
A: No. Those are internal implementation details.

**Q: Is the validation expensive?**  
A: FAST level is <1% overhead. NORMAL is <5%. Use NONE in production.

**Q: Can I mix this with other rendering mods?**  
A: Yes. The GL state capture/restore makes it compatible.

**Q: How do I report bugs?**  
A: Enable PARANOID validation, collect the diagnostic report, submit it.

**Q: Why is this so complex internally?**  
A: Performance. The complexity is hidden behind simple APIs.

---

**Remember:** You're working with a production-grade engine. Use the simple API, enable validation during development, and trust the systems to work correctly.
