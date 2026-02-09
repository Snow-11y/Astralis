# ✨ Contributing to Astralis 🪐

Welcome! Thanks for considering contributing to Astralis. This document explains how to work with this codebase effectively.

---

## 🌟 Philosophy

Astralis is built on three principles:

1. **Performance First** - Every line of code should justify its existence through measurable impact
2. **Compatibility Above All** - The mod must play nice with others
3. **Beauty Matters** - Code should be elegant, readable, and aesthetically pleasing

If your contribution doesn't align with these, it won't be merged.

### Community First, Always

**This project exists for the community's benefit, not for profit.**

- The project will **NEVER** have Ko-Fi, Patreon, or any sponsorship/donation platforms
- The author will **NEVER** monetize this work
- All contributions are for the betterment of the Minecraft community
- Free and open-source, forever

This commitment ensures Astralis remains focused on quality, performance, and community value rather than financial incentives.

---

## 📋 Code of Conduct

### ✅ DO:

- **Respect the architecture** - Understand the design before changing it
- **Measure everything** - Provide benchmarks for performance claims
- **Write tests** - Especially for critical paths
- **Document your changes** - Explain WHY, not just WHAT
- **Keep it clean** - Follow existing code style
- **Be specific** - "Optimized rendering" means nothing. "Reduced draw calls by 40%" means something.

### ❌ DON'T:

- Submit PRs with "minor fixes" without explaining what they fix
- Break compatibility for marginal gains
- Add dependencies without strong justification
- Ignore failing tests
- Use corporate buzzwords ("leverage," "synergy," "robust")
- Demand changes - suggest and discuss
- Engage in toxicity, bullying, or harassment of any kind
- Provide criticism without proper information or fair reasoning

### 🚫 INSTANT REJECTION:

- AI-generated code without understanding or testing (welcomed if your AI-Generated Code works)
- Copy-pasted solutions from Stack Overflow without attribution
- Breaking changes without migration path
- Performance regressions without justification
- Removing features without consensus
- **Using forbidden dependencies:**
  - MixinBooter (`zone.rong:mixinbooter`) or FermiumBooter - use DeepMix
  - RedCore - use BlueCore
  - Testing with incompatible mods instead of Astralis ecosystem equivalents

---

## 🎯 Priority Framework

Not all contributions are equal. Here's how we prioritize:

### **CRITICAL** (Will be reviewed immediately):
- Crashes, data corruption, security vulnerabilities
- Game-breaking bugs affecting majority of users
- Performance regressions >20%
- Compatibility breaks with popular mods

### **HIGH** (Will be reviewed within days):
- Significant performance improvements (>10% gains)
- New backend support (WebGPU, etc.)
- Features that enable new capabilities
- Compatibility fixes for specific mod conflicts

### **MEDIUM** (Will be reviewed within weeks):
- Quality-of-life improvements
- Code refactoring with measurable benefits
- Documentation improvements
- Minor optimizations (5-10% gains)

### **LOW** (May not be reviewed):
- Aesthetic preferences without functional benefit
- Micro-optimizations (<5% gains)
- "Nice to have" features
- Edge cases affecting <0.1% of users

**"Say Good Or Shut Up"** - Criticism must come with proof, benchmarks, or working alternatives. If something seems impossible but you can't demonstrate why with data, your skepticism isn't helpful. Either show evidence proving it's wrong, provide a better solution, or trust the maintainers who've done the work. For LOW priority items, exceptional justification with measurements is required.

**[CLARIFY]** This means: Be correct **AND NICE**, or don't speak up. Helpful, respectful communication is required - sarcasm and rudeness are not tolerated and will count against you (see Communication Points System below).

---

## 🔧 Technical Requirements

### Prerequisites

**Required:**
- Java 21+ (Java 25 preferred for full feature set)
- Gradle 8.5+
- Git
- Advanced understanding of graphics APIs (OpenGL/Vulkan)
- Ability to read and write English

**Recommended:**
- Experience with Panama FFI (Foreign Function & Memory API)
- Knowledge of SIMD optimization
- Understanding of GPU architecture
- Proficiency with debugging tools (JProfiler, VisualVM)

### Development Setup

```bash
# Clone the repository
git clone https://github.com/yourusername/astralis.git
cd astralis

# Build the project
./gradlew build

# Run tests
./gradlew test

# Run Minecraft with the mod
./gradlew runClient
```

### Required Dependencies & Ecosystem

**REQUIRED - Use These Only:**

**Mixin Provider:**
- ✅ **DeepMix** - The ONLY supported mixin provider
  - Provides MixinExtras 0.5.0
  - Provides Mixin 0.8.7
  - Includes extended features beyond standard mixins
  - ❌ **NEVER use MixinBooter (`zone.rong:mixinbooter`) or FermiumBooter** - DeepMix provides everything they do and more

**Runtime & Libraries:**
- ✅ **MDR (Mini-DirtyRoom)** - Provides latest Java, LWJGL, and libraries for Forge/any loader

**Core Framework:**
- ✅ **BlueCore** - Our core framework
  - ❌ **NEVER rely on RedCore** - BlueCore is superior

**Performance & Rendering Mods (Astralis Ecosystem):**

These mods are part of the Astralis ecosystem. **NOTE:** The mods listed below are **INCOMPATIBLE** with Astralis - use our ecosystem alternatives instead:

| **Incompatible Mod** | **Use Astralis Component** | **Purpose** |
|----------------------|---------------------------|-------------|
| Celerita's Dynamic Lights | **Neon** | Dynamic lighting system |
| Alfheim | **Lumen** | Lighting improvements |
| Lithium | **Fluorine** | General optimizations |
| VintageFix | **Asto** | Fixes and optimizations |
| Valkyrie | **Haku** | Performance improvements |
| OptiFine (partial) | **Lavender** | Visual features & benefits (NOT rendering) |
| Kirino / Nothirium / Celerita / Sodium | **Snowium** | Rendering engine |
| LoliASM | **SnowyASM** | ASM transformations |
| ModernFix | **LegacyFix** | Compatibility fixes |

**IMPORTANT:** When contributing, test compatibility with the Astralis ecosystem mods. Do NOT test with the incompatible mods listed above.

### Environment

- **IDE:** IntelliJ IDEA recommended (Community or Ultimate)
- **JVM Args:** `-XX:+UnlockExperimentalVMOptions --add-modules jdk.incubator.vector`
- **Gradle Args:** `--warning-mode all --stacktrace`

---

## 📐 Code Style

### General Principles

1. **Readability > Cleverness** - Code should be understandable at 3 AM
2. **Explicit > Implicit** - Make your intent clear
3. **Comments explain WHY** - The code shows WHAT
4. **No magic numbers** - Use named constants

### Formatting

```java
// ✅ GOOD: Clear, aligned, documented
private static final int DRAW_COUNT_WARNING   = 2000;  // 2K draws triggers warning
private static final int DRAW_COUNT_CRITICAL  = 5000;  // 5K draws triggers mitigation
private static final int DRAW_COUNT_EMERGENCY = 10000; // 10K draws triggers emergency mode

// ❌ BAD: Unclear, no context
private static final int LIMIT1 = 2000;
private static final int LIMIT2 = 5000;
```

### Naming Conventions

```java
// Classes: PascalCase
public class DrawCallCluster { }

// Methods: camelCase (verb-based)
public void optimizeBatches() { }
public boolean canMergeWith(DrawCommand other) { }

// Constants: SCREAMING_SNAKE_CASE
private static final int MAX_BATCH_SIZE = 1000;

// Variables: camelCase (descriptive)
int vertexCount = 0;          // ✅ Clear
int vc = 0;                   // ❌ Unclear
```

### File Structure

Every major file should follow this pattern:

```java
// ════════════════════════════════════════════════════════════════════════════════
// ██ FILE BANNER WITH ASCII ART
// ════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                            CLASS DOCUMENTATION                             ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  Purpose: What this class does                                            ║
 * ║  Problems Solved: Specific issues addressed                               ║
 * ║  Performance: Measured impact                                             ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */

// ════════════════════════════════════════════════════════════════════════════════
// ██ SECTION 1: IMPORTS
// ════════════════════════════════════════════════════════════════════════════════

// ════════════════════════════════════════════════════════════════════════════════
// ██ SECTION 2: CONSTANTS
// ════════════════════════════════════════════════════════════════════════════════

// ════════════════════════════════════════════════════════════════════════════════
// ██ SECTION 3: FIELDS
// ════════════════════════════════════════════════════════════════════════════════

// ... etc
```

### Documentation Standards

```java
/**
 * Batches compatible draw commands into single indirect draw.
 * 
 * PROBLEM: Submitting 10,000 individual draw calls consumes ~1ms CPU time
 * SOLUTION: Merge compatible draws into single glMultiDrawElementsIndirect call
 * RESULT: 10x-100x reduction in draw call overhead
 * 
 * @param commands List of draw commands to batch
 * @param useIndirect Whether to use indirect drawing (requires GL 4.3+)
 * @return Number of batches created
 */
public int batchCommands(List<DrawCommand> commands, boolean useIndirect) {
    // Implementation
}
```

---

## 🧪 Testing Requirements

### What to Test

**MUST test:**
- Core rendering paths (batching, culling, indirect draws)
- Backend switching (Vulkan ↔ OpenGL ↔ Metal)
- Memory allocation/deallocation (no leaks)
- Compatibility with popular mods (Optifine, Sodium, etc.)
- Performance under stress (10K+ entities, complex scenes)

**SHOULD test:**
- Edge cases (0 draws, MAX_INT draws, null inputs)
- Error recovery (driver crashes, OOM, device loss)
- Platform-specific behavior (Windows/Linux/macOS)

**CAN skip:**
- Framework internals (trust LWJGL, trust Vulkan)
- Obvious getters/setters with no logic

### Testing Standards

```java
@Test
public void testBatchingReducesDrawCalls() {
    // GIVEN: 1000 compatible draw commands
    List<DrawCommand> commands = generateCompatibleCommands(1000);
    DrawPool pool = new DrawPool();
    
    // WHEN: Commands are batched
    commands.forEach(pool::submit);
    pool.optimize();
    
    // THEN: Batches should be <10 (100x reduction)
    int batchCount = pool.getBatchCount();
    assertTrue(batchCount < 10, 
        "Expected <10 batches, got " + batchCount);
}
```

### Performance Testing

**Every optimization PR must include:**

```java
// Benchmark before/after with JMH or manual timing
@Benchmark
public void benchmarkDrawPoolBatching() {
    // Setup
    DrawPool pool = new DrawPool();
    List<DrawCommand> commands = generateCommands(10000);
    
    // Measure
    long start = System.nanoTime();
    commands.forEach(pool::submit);
    pool.optimize();
    long end = System.nanoTime();
    
    // Report
    long durationMicros = (end - start) / 1000;
    System.out.println("Batching 10K commands: " + durationMicros + "µs");
}
```

Include results in PR description:
```
Before: 1200µs
After:  450µs
Improvement: 62% faster
```

---

## 🔀 Git Workflow

### Branch Naming

```
feature/webgpu-backend          # New features
fix/vulkan-memory-leak          # Bug fixes
optimize/simd-batch-sorting     # Performance improvements
refactor/draw-pool-cleanup      # Code cleanup
docs/contributing-guide         # Documentation
```

### Commit Messages

```
✅ GOOD:
"Fix race condition in VulkanMemoryAllocator

Multiple threads could allocate from same memory block simultaneously.
Added StampedLock to synchronize allocations while allowing
concurrent reads of allocation metadata.

Fixes #123"

❌ BAD:
"fix bug"
"update files"
"changes"
```

**Format:**
```
<type>: <summary in imperative mood>

<detailed explanation of what and why>
<performance impact if applicable>
<breaking changes if any>

Fixes #issue-number
```

### Pull Request Template

```markdown
## Description
<!-- What does this PR do? -->

## Motivation
<!-- Why is this change needed? -->

## Testing
<!-- How did you test this? -->
- [ ] Manual testing in development environment
- [ ] Automated tests added/updated
- [ ] Tested with popular mods (Optifine, Sodium, etc.)
- [ ] Performance benchmarks included

## Performance Impact
<!-- Required for optimization PRs -->
Before: X µs/ms
After: Y µs/ms
Improvement: Z%

## Breaking Changes
<!-- List any breaking changes -->
- [ ] None
- [ ] API changes (list them)
- [ ] Configuration changes (list them)

## Checklist
- [ ] Code follows style guide
- [ ] Documentation updated
- [ ] Tests pass
- [ ] No performance regressions
- [ ] Backwards compatible (or migration path provided)
```

---

## 🏗️ Architecture Guidelines

### Adding New Backends

To add support for a new graphics API (e.g., WebGPU):

1. **Create backend structure:**
```
src/main/java/stellar/snow/astralis/api/webgpu/
  ├── managers/
  │   └── WebGPUManager.java
  ├── mapping/
  │   ├── WebGPUCallMapper.java
  │   └── WGSLCallMapper.java
  ├── pipeline/
  │   └── WebGPUPipelineProvider.java
  └── backend/
      └── WebGPUBackend.java
```

2. **Implement `GPUBackend` interface** in `WebGPUBackend.java`

3. **Register in `GPUBackendSelector.java`:**
```java
private void probeWebGPU() {
    try {
        // Initialize WebGPU context
        // Score based on features
        // Add to availableBackends
    } catch (Exception e) {
        // Log and continue to next backend
    }
}
```

4. **Add feature detection** to `UniversalCapabilities.java`

5. **Write tests** for backend switching and feature parity

### Modifying Core Systems

**Before touching:**
- `DrawPool.java` - Read the Javadoc
- `VulkanMemoryAllocator.java` - Understand memory budgeting
- `MeshletRenderer.java` - Understand GPU-driven rendering
- `ECS/World.java` - Understand off-heap entity storage

**Rules:**
1. **Measure first** - Profile to confirm bottleneck
2. **Prototype separately** - Don't modify core until proven
3. **Benchmark thoroughly** - Test against baseline
4. **Document extensively** - Explain design decisions
5. **Maintain compatibility** - Don't break existing users

### Memory Management

**Critical: All GPU-visible memory MUST use `MemorySegment`**

```java
// ✅ GOOD: Off-heap, GC-invisible
Arena arena = Arena.ofConfined();
MemorySegment buffer = arena.allocate(layout, count);

// ❌ BAD: On-heap, triggers GC
ByteBuffer buffer = ByteBuffer.allocate(size);
```

**Always:**
- Use `Arena` for lifecycle management
- Align allocations to cache lines (64 bytes)
- Free resources explicitly (don't rely on GC)
- Track allocation sizes for debugging

### Concurrency

**Lock hierarchy (to prevent deadlocks):**
1. Frame lock (highest)
2. Backend lock
3. Memory allocator lock
4. State cache lock (lowest)

**Prefer lock-free:**
```java
// ✅ GOOD: Lock-free counter
private final LongAdder totalDraws = new LongAdder();
totalDraws.increment();

// ❌ BAD: Synchronized counter
private long totalDraws = 0;
synchronized(this) { totalDraws++; }
```

---

## 📊 Performance Standards

### Benchmarking

**Required for all optimization PRs:**

```java
// 1. Baseline measurement
long baseline = measureDrawPoolPerformance();

// 2. Apply optimization
// ... your changes ...

// 3. Measure again
long optimized = measureDrawPoolPerformance();

// 4. Calculate improvement
double improvement = ((baseline - optimized) / (double)baseline) * 100;

// 5. Include in PR description
System.out.printf("Improvement: %.1f%%\n", improvement);
```

### Acceptance Criteria

**Optimization PRs must:**
- Improve performance by >5% in target scenario
- Not regress other scenarios by >2%
- Not increase memory usage by >10%
- Not reduce code readability significantly

**Example:**
```
Scenario: Batching 10,000 draw commands
Before: 1200µs ± 50µs
After:  450µs ± 20µs
Improvement: 62.5%

Memory: +0.5MB (acceptable, <10% increase)
Readability: Slightly more complex but well-documented
Decision: ✅ ACCEPT
```

### Profiling Tools

**Recommended:**
- JProfiler (CPU, memory, allocations)
- VisualVM (free alternative)
- Java Flight Recorder (built-in)
- GPU profilers (RenderDoc, Nsight, Metal Debugger)

**Profile before claiming optimization:**
```bash
# Run with Flight Recorder
java -XX:StartFlightRecording=duration=60s,filename=profile.jfr

# Analyze with Mission Control
jmc profile.jfr
```

---

## 🐛 Bug Reports

### Information Required

**Minimum:**
```markdown
**Description:** Clear explanation of the bug
**Expected:** What should happen
**Actual:** What actually happens
**Steps to Reproduce:**
1. Launch Minecraft with Astralis
2. ...
3. Bug occurs

**Environment:**
- Astralis Version: 0.1.2
- Minecraft Version: 1.12.2
- Java Version: 21.0.1
- OS: Windows 10 / Linux / macOS
- GPU: NVIDIA RTX 3080 / AMD RX 6800 / etc.
- Driver Version: 537.42
- Other Mods: Optifine, JEI, etc.

**Logs:** (Attach latest.log and crash report if applicable)
```

### Critical Bugs

**Immediate attention required:**
- Crashes on launch
- Data corruption
- Security vulnerabilities
- Complete feature breakage
- Performance regression >50%

**Report via:**
- GitHub Issues (preferred)

---

## 🎨 Aesthetic Guidelines

### Visual Identity

Astralis embraces **cosmic, dreamlike aesthetics**:

```java
// ✅ GOOD: Sparkles, cosmic naming, visual appeal
// ✨🪐【 ASTRALIS RENDER CORE 】🪐✨

// ❌ BAD: Corporate, sterile naming
// RENDER_SYSTEM_V2
```

**Allowed:**
- Star emoji (⭐), sparkles (✨), planets (🪐), snowflakes (❄️)
- Cosmic terminology (Stellar, Astral, Celestial)
- Creative naming (Snowflakes Edition)

**Not allowed:**
- Corporate buzzwords
- Generic technical jargon without personality
- Sterile documentation

### Code Aesthetics

**Banner examples:**

```java
// ═══════════════════════════════════════════════════════════════════════════════
// ██████████████████████████████████████████████████████████████████████████████
// ██                                                                          ██
// ██    ██████╗ ██████╗  █████╗ ██╗    ██╗██████╗  ██████╗  ██████╗ ██╗      ██
// ██    ██╔══██╗██╔══██╗██╔══██╗██║    ██║██╔══██╗██╔═══██╗██╔═══██╗██║      ██
// ██    ██║  ██║██████╔╝███████║██║ █╗ ██║██████╔╝██║   ██║██║   ██║██║      ██
// ██    ██║  ██║██╔══██╗██╔══██║██║███╗██║██╔═══╝ ██║   ██║██║   ██║██║      ██
// ██    ██████╔╝██║  ██║██║  ██║╚███╔███╔╝██║     ╚██████╔╝╚██████╔╝███████╗ ██
// ██    ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝ ╚══╝╚══╝ ╚═╝      ╚═════╝  ╚═════╝ ╚══════╝ ██
// ██                                                                          ██
// ██████████████████████████████████████████████████████████████████████████████
// ═══════════════════════════════════════════════════════════════════════════════
```

---

## 🤝 Communication

### Channels

- **GitHub Issues:** Bug reports, feature requests
- **GitHub Discussions:** General questions, ideas
- **Pull Requests:** Code contributions

### Tone & Behavior

**MUST:**
- Stay helpful and respectful at all times
- Be direct and honest
- Be data-driven (measurements over opinions)
- Be specific (not vague)

**ABSOLUTELY FORBIDDEN:**
- Sarcasm or rudeness in any form
- Condescending behavior
- Passive-aggressive comments
- Dismissive attitudes toward concerns
- Demanding without justification

### Communication Points System

Snow (the maintainer) tracks contributor behavior through a points system:

**Negative Points (-1 to -10 per incident):**
- Sarcasm or rude comments: -5 points
- Dismissive or condescending behavior: -5 points
- Passive-aggressive communication: -3 points
- Unfair criticism without proper information: -5 points
- Toxicity or bullying: -15 points
- Ignoring feedback or being demanding: -3 points

**Positive Points (+1 to +10 per incident):**
- Helpful, constructive feedback: +5 points
- Quality contributions with good documentation: +5 points
- Patient and respectful communication: +3 points
- Helping other contributors: +5 points
- Fixing critical bugs or security issues: +10 points

**Consequences:**
- **75 negative points = Permanent ban from contributing**
- Contributors with higher positive points get prioritized in reviews
- Negative points can be offset by positive contributions over time

This system ensures our community remains helpful, respectful, and productive.

### Response Times

**Maintainer response times:**
- Critical bugs: <24 hours
- High-priority PRs: <3 days
- Medium-priority PRs: <1 week
- Low-priority PRs: When possible

**Contributor expectations:**
- Respond to review comments within 1 week
- Update PRs based on feedback
- Close stale PRs (no activity for 30 days)

---

## 📜 License & Attribution

### License

**[CLARIFY]** The specific license type is currently undecided and left as a placeholder for now. This will be determined and updated in the future.

Astralis is licensed under [LICENSE TYPE]. By contributing, you agree that your contributions will be licensed under the same license.

### Attribution

**Required:**
- Add yourself to `contributors.md`
- Credit significant inspirations or code adaptations
- Link to relevant research papers or articles

**Example:**
```java
/**
 * SIMD-accelerated sort implementation.
 * 
 * Inspired by: "Fast Sorting on Modern CPUs" by Edelkamp & Weiß
 * Adapted from: https://github.com/example/simd-sort
 * License: MIT
 */
```

---

## 🚀 First Contribution?

### Good First Issues

Look for issues labeled:
- `good-first-issue` - Beginner-friendly
- `documentation` - Non-code contributions
- `help-wanted` - Maintainers need assistance

### Learning Path

**New to graphics programming:**
1. Read `docs/architecture.md` (when exists)
2. Explore `api/opengl/` for simpler backend
3. Study `DrawPool.java` for batching concepts
4. Start with documentation improvements

**New to Java 21+ features:**
1. Read [JEP 454](https://openjdk.org/jeps/454) (Foreign Function & Memory API)
2. Read [JEP 448](https://openjdk.org/jeps/448) (Vector API)
3. Explore `VulkanMemoryAllocator.java` for `MemorySegment` usage
4. Explore `DrawPool.java` for SIMD usage

**New to performance optimization:**
1. Profile first, optimize second
2. Measure everything
3. One optimization at a time
4. Document your findings

---

## ⚠️ Common Mistakes

### ❌ Don't:

**1. Optimize without profiling**
```java
// ❌ "I think this is faster"
for (int i = 0; i < array.length; ++i) { } // vs i++

// ✅ "Profiling shows 60% time in X, here's the fix"
// [Profiler screenshot] + benchmarks
```

**2. Break compatibility silently**
```java
// ❌ Changed API without migration path
public void draw(int vbo) // Was: draw(int vbo, int mode)

// ✅ Deprecated old, added new
@Deprecated
public void draw(int vbo) { draw(vbo, GL_TRIANGLES); }
public void draw(int vbo, int mode) { ... }
```

**3. Add dependencies without justification**
```gradle
// ❌ Added library "because it's cool"
implementation 'resource.libraries:fancy-lib:1.0'

// ❌ Using forbidden mixin providers
implementation 'zone.rong:mixinbooter:1.0'          // NEVER - Use DeepMix

// ✅ Explained need and evaluated alternatives
// Need: Efficient sparse matrix operations
// Evaluated: Apache Commons Math, EJML, custom impl
// Chose: EJML (smallest, fastest, maintained)
implementation 'org.ejml:ejml-core:0.43'
```

**4. Ignore test failures**
```
❌ "Tests are broken but my change works"
✅ Fix tests OR explain why test is invalid
```

**5. Submit huge PRs**
```
❌ 5000 lines changed across 50 files
✅ Small, focused PRs (<500 lines, single purpose)
```

---

## 📚 Resources

### Documentation

- **Architecture Guide:** Understanding the system design
- **Performance Guide:** Optimization techniques and benchmarking
- **API Reference:** JavaDoc (auto-generated)
- **Shader Guide:** Writing and optimizing shaders

### External Resources

**Graphics APIs:**
- [Vulkan Tutorial](https://vulkan-tutorial.com/)
- [OpenGL Spec](https://www.khronos.org/opengl/)
- [Metal Best Practices](https://developer.apple.com/metal/)

**Java 21+ Features:**
- [Panama FFI Documentation](https://openjdk.org/jeps/454)
- [Vector API Documentation](https://openjdk.org/jeps/448)
- [Structured Concurrency](https://openjdk.org/jeps/453)

**Performance:**
- [JMH Benchmarking](https://openjdk.org/projects/code-tools/jmh/)
- [GPU Performance Tuning](https://developer.nvidia.com/blog/)

---

## 🎓 Learning from the Codebase

### Recommended Reading Order

**Day 1: High-level architecture**
1. `GPUBackendSelector.java` - How backends are chosen
2. `BackendCoordinator.java` - How backends cooperate
3. `UniversalCapabilities.java` - Feature detection

**Day 2: Core systems**
1. `DrawPool.java` - Batching engine (2000+ lines of documentation)
2. `DrawCallCluster.java` - Draw caching
3. `IndirectDrawManager.java` - GPU-driven rendering

**Day 3: Specific backends**
1. `VulkanBackend.java` - Modern API implementation
2. `OpenGLBackend.java` - Legacy API implementation
3. `VulkanMemoryAllocator.java` - Custom memory management

**Day 4: Bridge & integration**
1. `RenderBridge.java` - Minecraft ↔ Engine connection
2. `MinecraftECSBridge.java` - Entity synchronization
3. `MixinUniversalPatcher.java` - Runtime code injection

### Code Patterns to Study

**Off-heap memory:**
```java
// Study: VulkanMemoryAllocator.java
Arena arena = Arena.ofConfined();
MemorySegment buffer = arena.allocate(LAYOUT, count);
// ... use buffer ...
arena.close(); // Auto-frees all allocations
```

**SIMD acceleration:**
```java
// Study: DrawPool.java -> computeSortKeysSIMD()
LongVector v1 = LongVector.fromArray(SPECIES, data, 0);
LongVector result = v1.lanewise(VectorOperators.ADD, v2);
result.intoArray(output, 0);
```

**Lock-free concurrency:**
```java
// Study: DrawCallCluster.java
private final LongAdder hits = new LongAdder();
hits.increment(); // Thread-safe, no lock
```

**Builder pattern:**
```java
// Study: DrawPool.java -> DrawSubmissionBuilder
pool.draw()
    .triangles()
    .vertices(1024)
    .shader(shaderId)
    .submit();
```

---

## ✨ Final Notes

### What We Value

**In contributions:**
1. Correctness > Speed (but we want both)
2. Compatibility > Features (don't break existing users)
3. Clarity > Cleverness (maintainable code)
4. Measurement > Opinion (data-driven decisions)

**In contributors:**
1. Respect for the craft
2. Willingness to learn
3. Attention to detail
4. Honest communication
5. Patience with review process

### What We Don't Tolerate

- **Toxicity of any kind** - This is a harsh NO. Zero tolerance.
- Disrespectful behavior or bullying
- Sarcasm or rudeness in communication
- Unfair criticism without proper information or reasoning
- Plagiarism or uncredited work
- Deliberate sabotage
- Ignoring maintainer feedback
- Demanding immediate attention
- Using forbidden dependencies (MixinBooter variants, RedCore, etc.)

### Recognition

**Outstanding contributors receive:**
- Mention in release notes
- Addition to contributors list
- Potential collaborator status

**"Collaborator" status granted after:**
- 5+ merged PRs
- Demonstrated understanding of architecture
- Consistent quality contributions
- Respectful communication

---

## 🌠 The Astralis Way

This project is built with:
- **Passion** - We love what we do
- **Precision** - Every detail matters
- **Performance** - Speed is a feature
- **Personality** - Code should have soul

Contributing to Astralis means joining a mission: making Minecraft rendering **fast, beautiful, and magical**.

Welcome to the cosmos. Let's build something incredible.

---

*✨ Built with care by the Snow-11y 🪐*

*Questions? Open a discussion. Found a bug? Open an issue. Ready to contribute? Send a PR.*
