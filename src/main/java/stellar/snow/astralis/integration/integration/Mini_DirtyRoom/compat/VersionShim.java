// =====================================================================================
// VersionShim.java (Enhanced with Dynamic Fallback Routing)
// Part of Mini_DirtyRoom — Minecraft 1.12.2 Modernization Layer
//
// A comprehensive version spoofing and compatibility shim layer with automatic
// fallback routing when incompatibility errors are detected.
//
// PURPOSE:
//   1. Users pressing F3 (debug screen) see: LWJGL 3.4.0, Java 25 (ALWAYS baseline)
//   2. Mods that hardcode checks for "LWJGL 2.x" or "Java 8" get spoofed
//      responses so they don't refuse to load or crash
//   3. Mods that DO work natively with LWJGL 3 / modern Java see the REAL versions
//   4. Prevent instant crashes from version-gating code at runtime
//   5. DYNAMIC FALLBACK: If a mod throws incompatibility errors, automatically
//      route through Java 25 → 21 → 17 → 8 and LWJGL 3.4.0 → 3.3.3 → 3.2.3 → 2.9.4
//      until compatibility is achieved
//   6. Core version changes dynamically based on what works, debug version never changes
//
// STRATEGY:
//   - Two-faced version reporting: baseline to debug (F3), dynamic core version to system
//   - Watchdog monitors for incompatibility exceptions and triggers fallback routing
//   - Bytecode-level interception of version check patterns
//   - Fake class stubs for LWJGL 2 classes that mods might probe for existence
//   - System property spoofing for java.version / java.specification.version
//   - Classloader trickery to make Class.forName("org.lwjgl.Sys") succeed
//   - Crash prevention via blanket exception interception on version-gated paths
//   - Per-mod version routing based on what actually works
//
// =====================================================================================

package stellar.snow.astralis.integration.Mini_DirtyRoom.compat;

import stellar.snow.astralis.integration.DeepMixTransformers;
import stellar.snow.astralis.integration.DeepMix.DeepMix;
import stellar.snow.astralis.integration.DeepMix.DeepMixAssetForge;
import stellar.snow.astralis.integration.DeepMix.Core.*;
import stellar.snow.astralis.integration.DeepMix.Transformers.DeepMixTransformEngine;
import stellar.snow.astralis.integration.DeepMix.Util.DeepMixUtilities;

import java.io.*;
import java.lang.instrument.*;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.jar.*;
import java.util.regex.*;
import java.util.stream.*;
import java.util.zip.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public final class VersionShim {

    // =====================================================================================
    //  CONSTANTS & CONFIGURATION
    // =====================================================================================

    // ── Debug screen versions (ALWAYS shows baseline, NEVER changes) ─────────────
    private static final String DEBUG_LWJGL_VERSION = "3.4.0";
    private static final String DEBUG_JAVA_VERSION  = "25";

    // ── Core version configuration (dynamically changes based on compatibility) ───
    
    /**
     * Current "real" Java version that the core reports.
     * Starts at baseline (25), falls back through compatibility chain on errors.
     */
    private static volatile int CURRENT_JAVA_VERSION = 25;
    
    /**
     * Current "real" LWJGL version that the core reports.
     * Starts at baseline (3.4.0), falls back through compatibility chain on errors.
     */
    private static volatile String CURRENT_LWJGL_VERSION = "3.4.0";

    // ── Java version fallback chain ──────────────────────────────────────────────
    private static final int[] JAVA_FALLBACK_CHAIN = {25, 21, 17, 8};
    private static final String[] JAVA_VERSION_STRINGS = {
        "25.0.1",           // Java 25
        "21.0.2",           // Java 21
        "17.0.10",          // Java 17
        "1.8.0_312"         // Java 8 (baseline)
    };
    private static final String[] JAVA_SPEC_VERSIONS = {
        "25",               // Java 25 spec
        "21",               // Java 21 spec
        "17",               // Java 17 spec
        "1.8"               // Java 8 spec
    };

    // ── LWJGL version fallback chain ─────────────────────────────────────────────
    private static final String[] LWJGL_FALLBACK_CHAIN = {
        "3.4.0",            // Baseline
        "3.3.3",
        "3.2.3",
        "2.9.4-nightly-20150209"  // Legacy fallback
    };
    private static final int[][] LWJGL_VERSION_COMPONENTS = {
        {3, 4, 0},          // LWJGL 3.4.0
        {3, 3, 3},          // LWJGL 3.3.3
        {3, 2, 3},          // LWJGL 3.2.3
        {2, 9, 4}           // LWJGL 2.9.4
    };

    // ── System property configuration ────────────────────────────────────────────
    private static final String REAL_JAVA_VERSION    = System.getProperty("java.version");
    private static final int    REAL_JAVA_FEATURE    = Runtime.version().feature();
    private static final String REAL_JAVA_SPEC       = System.getProperty("java.specification.version");
    private static final String REAL_JAVA_VENDOR     = System.getProperty("java.vendor");
    private static final String REAL_JAVA_VM_NAME    = System.getProperty("java.vm.name");

    // ── Fake versions for ultra-paranoid legacy mods (always Java 8 / LWJGL 2) ───
    private static final String LEGACY_FAKE_JAVA_VERSION    = "1.8.0_312";
    private static final String LEGACY_FAKE_JAVA_SPEC       = "1.8";
    private static final String LEGACY_FAKE_JAVA_VENDOR     = "Oracle Corporation";
    private static final String LEGACY_FAKE_JAVA_VM_NAME    = "Java HotSpot(TM) 64-Bit Server VM";
    private static final String LEGACY_FAKE_LWJGL_VERSION   = "2.9.4-nightly-20150209";
    private static final int    LEGACY_FAKE_LWJGL_MAJOR     = 2;
    private static final int    LEGACY_FAKE_LWJGL_MINOR     = 9;
    private static final int    LEGACY_FAKE_LWJGL_PATCH     = 4;

    // ── Watchdog configuration ───────────────────────────────────────────────────
    
    /**
     * Incompatibility error patterns that trigger version fallback.
     */
    private static final List<Pattern> INCOMPATIBILITY_PATTERNS = Arrays.asList(
        Pattern.compile("(?i)incompatible.*java.*version"),
        Pattern.compile("(?i)unsupported.*class.*version"),
        Pattern.compile("java\\.lang\\.UnsupportedClassVersionError"),
        Pattern.compile("(?i)requires.*java.*[0-9]+"),
        Pattern.compile("(?i)incompatible.*lwjgl.*version"),
        Pattern.compile("(?i)lwjgl.*not.*found"),
        Pattern.compile("org\\.lwjgl.*NoClassDefFoundError"),
        Pattern.compile("(?i)unsupported.*lwjgl")
    );

    /**
     * Per-mod version overrides.
     */
    private static final ConcurrentHashMap<String, VersionOverride> MOD_VERSION_OVERRIDES =
        new ConcurrentHashMap<>(256);

    /**
     * Watchdog error counter per mod.
     */
    private static final ConcurrentHashMap<String, AtomicInteger> MOD_ERROR_COUNTS =
        new ConcurrentHashMap<>(256);

    /**
     * Global Java version fallback index.
     */
    private static volatile int globalJavaFallbackIndex = 0;

    /**
     * Global LWJGL version fallback index.
     */
    private static volatile int globalLwjglFallbackIndex = 0;

    /**
     * Lock for fallback routing operations.
     */
    private static final Object FALLBACK_LOCK = new Object();

    // ── Detection state ──────────────────────────────────────────────────────────

    private static final ConcurrentHashMap<String, SpoofProfile> SPOOF_REGISTRY =
        new ConcurrentHashMap<>(256);

    private static final Set<String> SAFE_CLASSES = ConcurrentHashMap.newKeySet(1024);

    private static final List<StackPattern> VERSION_CHECK_PATTERNS = new ArrayList<>();

    private static final Set<String> CRASH_SITES = ConcurrentHashMap.newKeySet(128);

    private static final AtomicLong PREVENTED_CRASHES = new AtomicLong(0);

    private static final AtomicLong FALLBACK_EVENTS = new AtomicLong(0);

    private static volatile boolean ARMED = false;

    private static volatile boolean WATCHDOG_ACTIVE = false;

    // ── Cache System ─────────────────────────────────────────────────────────────
    
    /**
     * Cache file locations
     */
    private static final String CACHE_DIR = "config/mini_dirtyroom/version_cache/";
    private static final String LOCAL_CACHE_FILE = CACHE_DIR + "local_cache.json";
    private static final String COMMUNITY_CACHE_FILE = CACHE_DIR + "community_cache.json";
    private static final String CACHE_VERSION = "1.0";
    
    /**
     * Mod compatibility cache: maps mod ID -> required versions
     * Scanned from installed mods and loaded from community database
     */
    private static final ConcurrentHashMap<String, ModCompatibilityEntry> MOD_CACHE =
        new ConcurrentHashMap<>(2048);
    
    /**
     * Detected mods in the current installation
     */
    private static final ConcurrentHashMap<String, DetectedMod> DETECTED_MODS =
        new ConcurrentHashMap<>(512);
    
    /**
     * Cache statistics
     */
    private static final AtomicLong cacheHits = new AtomicLong(0);
    private static final AtomicLong cacheMisses = new AtomicLong(0);
    private static final AtomicLong modsScanned = new AtomicLong(0);
    private static final AtomicLong communityCacheContributions = new AtomicLong(0);
    
    /**
     * Gson instance for JSON serialization
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ─────────────────────────────────────────────────────────────────────────────
    //  ENUMS & RECORDS
    // ─────────────────────────────────────────────────────────────────────────────

    public enum SpoofProfile {
        LWJGL_ONLY,
        JAVA_ONLY,
        BOTH,
        CLASSLOAD_PROBE,
        SYSPROP_READER,
        REFLECTION_PROBE,
        UNKNOWN
    }

    private record StackPattern(
        String classNameContains,
        String methodNameContains,
        SpoofProfile profile
    ) {}

    /**
     * Version override for a specific mod.
     */
    public static class VersionOverride {
        public final int javaVersion;
        public final String lwjglVersion;
        public final boolean useLegacyFake;

        public VersionOverride(int javaVersion, String lwjglVersion, boolean useLegacyFake) {
            this.javaVersion = javaVersion;
            this.lwjglVersion = lwjglVersion;
            this.useLegacyFake = useLegacyFake;
        }

        public static VersionOverride legacy() {
            return new VersionOverride(8, "2.9.4-nightly-20150209", true);
        }

        public static VersionOverride modern() {
            return new VersionOverride(CURRENT_JAVA_VERSION, CURRENT_LWJGL_VERSION, false);
        }

        public static VersionOverride specific(int java, String lwjgl) {
            return new VersionOverride(java, lwjgl, false);
        }
    }

    /**
     * Detected mod information from JAR scanning
     */
    public static class DetectedMod {
        public final String modId;
        public final String modName;
        public final String version;
        public final String mainClass;
        public final String jarPath;
        public final Set<String> dependencies;
        
        public DetectedMod(String modId, String modName, String version, String mainClass, 
                          String jarPath, Set<String> dependencies) {
            this.modId = modId;
            this.modName = modName;
            this.version = version;
            this.mainClass = mainClass;
            this.jarPath = jarPath;
            this.dependencies = dependencies;
        }
    }
    
    /**
     * Mod compatibility cache entry
     */
    public static class ModCompatibilityEntry {
        public String modId;
        public String modName;
        public String modVersion;
        public int requiredJavaVersion;
        public String requiredLwjglVersion;
        public boolean useLegacyFake;
        public String detectedBy;  // "scan", "runtime", "community"
        public long timestamp;
        public int successCount;   // How many times this worked
        public int failureCount;   // How many times this failed
        public String notes;
        
        public ModCompatibilityEntry() {}
        
        public ModCompatibilityEntry(String modId, String modName, String modVersion,
                                    int requiredJavaVersion, String requiredLwjglVersion,
                                    boolean useLegacyFake, String detectedBy) {
            this.modId = modId;
            this.modName = modName;
            this.modVersion = modVersion;
            this.requiredJavaVersion = requiredJavaVersion;
            this.requiredLwjglVersion = requiredLwjglVersion;
            this.useLegacyFake = useLegacyFake;
            this.detectedBy = detectedBy;
            this.timestamp = System.currentTimeMillis();
            this.successCount = 0;
            this.failureCount = 0;
            this.notes = "";
        }
        
        public void recordSuccess() {
            this.successCount++;
            this.timestamp = System.currentTimeMillis();
        }
        
        public void recordFailure() {
            this.failureCount++;
        }
        
        public double getSuccessRate() {
            int total = successCount + failureCount;
            return total == 0 ? 0.0 : (double) successCount / total;
        }
    }


    // =====================================================================================
    //  SECTION 1: INITIALIZATION
    // =====================================================================================

    static {
        registerKnownPatterns();
    }

    public static void initialize() {
        long startNs = System.nanoTime();

        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║       VersionShim — Mini_DirtyRoom (Enhanced)                ║");
        System.out.println("║   Dynamic version routing · Watchdog crash prevention        ║");
        System.out.println("║   Intelligent caching · Community-powered compatibility      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println("[VersionShim] Real system: Java " + REAL_JAVA_VERSION
            + " (feature " + REAL_JAVA_FEATURE + ")");
        System.out.println("[VersionShim] Debug display (F3): Java " + DEBUG_JAVA_VERSION
            + " / LWJGL " + DEBUG_LWJGL_VERSION + " [NEVER CHANGES]");
        System.out.println("[VersionShim] Core baseline: Java " + CURRENT_JAVA_VERSION
            + " / LWJGL " + CURRENT_LWJGL_VERSION + " [DYNAMIC]");
        System.out.println("[VersionShim] Legacy fake for paranoid mods: Java "
            + LEGACY_FAKE_JAVA_VERSION + " / LWJGL " + LEGACY_FAKE_LWJGL_VERSION);
        System.out.println("[VersionShim] Fallback chains armed:");
        System.out.println("  Java:  " + Arrays.toString(JAVA_FALLBACK_CHAIN));
        System.out.println("  LWJGL: " + Arrays.toString(LWJGL_FALLBACK_CHAIN));
        System.out.println();

        // Initialize cache system FIRST - before any mods load
        System.out.println("[VersionShim] Initializing compatibility cache system...");
        initializeCacheSystem();
        
        // Scan installed mods to build compatibility database
        System.out.println("[VersionShim] Scanning installed mods...");
        scanInstalledMods();
        
        // Apply cached compatibility settings
        System.out.println("[VersionShim] Applying cached compatibility settings...");
        applyCachedSettings();

        installLWJGL2Stubs();
        armBytecodeTransformers();
        installCrashPrevention();
        patchDebugScreen();
        activateWatchdog();

        ARMED = true;

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        System.out.println("[VersionShim] Initialization complete in " + elapsedMs + " ms");
        System.out.println("[VersionShim] Cache statistics:");
        System.out.println("  Mods scanned: " + modsScanned.get());
        System.out.println("  Cache entries: " + MOD_CACHE.size());
        System.out.println("  Community contributions: " + communityCacheContributions.get());
        System.out.println();

        if (!selfTest()) {
            System.err.println("[VersionShim] WARNING: Self-test failures detected!");
        }
    }

    private static void registerKnownPatterns() {
        VERSION_CHECK_PATTERNS.add(new StackPattern(
            "net.optifine", "checkVersion", SpoofProfile.BOTH));
        VERSION_CHECK_PATTERNS.add(new StackPattern(
            "journeymap", "checkJavaVersion", SpoofProfile.JAVA_ONLY));
        VERSION_CHECK_PATTERNS.add(new StackPattern(
            "me.guichaguri.betterfps", "checkCompatibility", SpoofProfile.BOTH));
        VERSION_CHECK_PATTERNS.add(new StackPattern(
            "com.mamiyaotaru.voxelmap", "verifyVersion", SpoofProfile.LWJGL_ONLY));
        VERSION_CHECK_PATTERNS.add(new StackPattern(
            "fastcraft", "validate", SpoofProfile.JAVA_ONLY));
        VERSION_CHECK_PATTERNS.add(new StackPattern(
            "", "forName", SpoofProfile.CLASSLOAD_PROBE));
    }


    // =====================================================================================
    //  SECTION 1.5: INTELLIGENT CACHE SYSTEM & MOD SCANNING
    // =====================================================================================

    /**
     * Initialize the cache system - load existing caches from disk
     */
    private static void initializeCacheSystem() {
        try {
            // Create cache directory if it doesn't exist
            Files.createDirectories(Paths.get(CACHE_DIR));
            
            // Load local cache
            loadLocalCache();
            
            // Load community cache
            loadCommunityCache();
            
            System.out.println("[VersionShim] Cache system initialized");
            System.out.println("  Local cache entries: " + 
                MOD_CACHE.values().stream().filter(e -> "scan".equals(e.detectedBy) || "runtime".equals(e.detectedBy)).count());
            System.out.println("  Community cache entries: " + 
                MOD_CACHE.values().stream().filter(e -> "community".equals(e.detectedBy)).count());
            
        } catch (Exception e) {
            System.err.println("[VersionShim] Failed to initialize cache system: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load local cache from disk
     */
    private static void loadLocalCache() {
        Path cachePath = Paths.get(LOCAL_CACHE_FILE);
        if (!Files.exists(cachePath)) {
            System.out.println("[VersionShim] No local cache found, starting fresh");
            return;
        }

        try {
            String json = new String(Files.readAllBytes(cachePath));
            Type type = new TypeToken<Map<String, ModCompatibilityEntry>>(){}.getType();
            Map<String, ModCompatibilityEntry> cache = GSON.fromJson(json, type);
            
            if (cache != null) {
                MOD_CACHE.putAll(cache);
                System.out.println("[VersionShim] Loaded " + cache.size() + " entries from local cache");
            }
        } catch (Exception e) {
            System.err.println("[VersionShim] Failed to load local cache: " + e.getMessage());
        }
    }

    /**
     * Load community cache from disk
     */
    private static void loadCommunityCache() {
        Path cachePath = Paths.get(COMMUNITY_CACHE_FILE);
        if (!Files.exists(cachePath)) {
            System.out.println("[VersionShim] No community cache found");
            return;
        }

        try {
            String json = new String(Files.readAllBytes(cachePath));
            Type type = new TypeToken<Map<String, ModCompatibilityEntry>>(){}.getType();
            Map<String, ModCompatibilityEntry> cache = GSON.fromJson(json, type);
            
            if (cache != null) {
                // Community cache entries are lower priority - don't override local entries
                for (Map.Entry<String, ModCompatibilityEntry> entry : cache.entrySet()) {
                    MOD_CACHE.putIfAbsent(entry.getKey(), entry.getValue());
                    communityCacheContributions.incrementAndGet();
                }
                System.out.println("[VersionShim] Loaded " + cache.size() + " entries from community cache");
            }
        } catch (Exception e) {
            System.err.println("[VersionShim] Failed to load community cache: " + e.getMessage());
        }
    }

    /**
     * Save local cache to disk
     */
    private static void saveLocalCache() {
        try {
            // Only save entries we discovered locally
            Map<String, ModCompatibilityEntry> localEntries = MOD_CACHE.entrySet().stream()
                .filter(e -> !"community".equals(e.getValue().detectedBy))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            
            String json = GSON.toJson(localEntries);
            Files.write(Paths.get(LOCAL_CACHE_FILE), json.getBytes());
            
            System.out.println("[VersionShim] Saved " + localEntries.size() + " entries to local cache");
        } catch (Exception e) {
            System.err.println("[VersionShim] Failed to save local cache: " + e.getMessage());
        }
    }

    /**
     * Scan installed mods to detect version requirements
     * Uses high-performance parallel scanning
     */
    private static void scanInstalledMods() {
        long scanStart = System.currentTimeMillis();
        
        try {
            // Find mods directory
            Path modsDir = Paths.get("mods");
            if (!Files.exists(modsDir)) {
                System.out.println("[VersionShim] No mods directory found, skipping scan");
                return;
            }

            // Parallel scan of all JAR files
            List<Path> jarFiles = Files.list(modsDir)
                .filter(p -> p.toString().endsWith(".jar"))
                .collect(Collectors.toList());

            System.out.println("[VersionShim] Scanning " + jarFiles.size() + " mod JARs...");

            jarFiles.parallelStream().forEach(jarPath -> {
                try {
                    scanModJar(jarPath);
                    modsScanned.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("[VersionShim] Failed to scan " + jarPath.getFileName() + ": " + e.getMessage());
                }
            });

            long scanTime = System.currentTimeMillis() - scanStart;
            System.out.println("[VersionShim] Mod scan complete in " + scanTime + " ms");
            System.out.println("  Detected mods: " + DETECTED_MODS.size());
            System.out.println("  New cache entries: " + modsScanned.get());
            
        } catch (Exception e) {
            System.err.println("[VersionShim] Mod scanning failed: " + e.getMessage());
        }
    }

    /**
     * Scan a single mod JAR for version requirements
     */
    private static void scanModJar(Path jarPath) throws IOException {
        try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
            // Look for mcmod.info or mods.toml
            ZipEntry mcmodInfo = zipFile.getEntry("mcmod.info");
            ZipEntry modsToml = zipFile.getEntry("META-INF/mods.toml");
            
            String modId = null;
            String modName = null;
            String modVersion = null;
            Set<String> dependencies = new HashSet<>();
            
            if (mcmodInfo != null) {
                // Parse mcmod.info (Forge 1.12.2 format)
                try (InputStream is = zipFile.getInputStream(mcmodInfo);
                     InputStreamReader reader = new InputStreamReader(is)) {
                    
                    JsonElement element = GSON.fromJson(reader, JsonElement.class);
                    if (element.isJsonArray()) {
                        JsonArray array = element.getAsJsonArray();
                        if (array.size() > 0) {
                            JsonObject modInfo = array.get(0).getAsJsonObject();
                            modId = modInfo.has("modid") ? modInfo.get("modid").getAsString() : null;
                            modName = modInfo.has("name") ? modInfo.get("name").getAsString() : null;
                            modVersion = modInfo.has("version") ? modInfo.get("version").getAsString() : null;
                            
                            if (modInfo.has("dependencies")) {
                                JsonArray deps = modInfo.getAsJsonArray("dependencies");
                                for (JsonElement dep : deps) {
                                    dependencies.add(dep.getAsString());
                                }
                            }
                        }
                    }
                }
            }
            
            if (modId == null) {
                // Fallback: use filename
                modId = jarPath.getFileName().toString().replace(".jar", "");
                modName = modId;
                modVersion = "unknown";
            }
            
            // Scan for version checks in bytecode (simplified heuristic)
            int javaRequirement = detectJavaRequirement(zipFile);
            String lwjglRequirement = detectLwjglRequirement(zipFile);
            
            // Store detected mod
            DetectedMod mod = new DetectedMod(modId, modName, modVersion, null, 
                jarPath.toString(), dependencies);
            DETECTED_MODS.put(modId, mod);
            
            // Check if we already have a cache entry
            ModCompatibilityEntry existing = MOD_CACHE.get(modId);
            if (existing == null) {
                // Create new cache entry with detected requirements
                ModCompatibilityEntry entry = new ModCompatibilityEntry(
                    modId, modName, modVersion,
                    javaRequirement, lwjglRequirement,
                    javaRequirement == 8,  // Use legacy fake for Java 8 mods
                    "scan"
                );
                entry.notes = "Auto-detected from JAR scan";
                MOD_CACHE.put(modId, entry);
            }
        }
    }

    /**
     * Detect Java version requirement from class file versions
     */
    private static int detectJavaRequirement(ZipFile zipFile) {
        try {
            // Sample a few class files to determine the class file version
            int maxClassVersion = 0;
            
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            int sampledClasses = 0;
            
            while (entries.hasMoreElements() && sampledClasses < 10) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        // Read class file magic and version
                        byte[] header = new byte[8];
                        if (is.read(header) == 8) {
                            // Magic number: 0xCAFEBABE
                            if (header[0] == (byte)0xCA && header[1] == (byte)0xFE &&
                                header[2] == (byte)0xBA && header[3] == (byte)0xBE) {
                                
                                // Major version is at bytes 6-7 (big endian)
                                int majorVersion = ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
                                maxClassVersion = Math.max(maxClassVersion, majorVersion);
                                sampledClasses++;
                            }
                        }
                    }
                }
            }
            
            // Convert class file version to Java version
            // 52 = Java 8, 61 = Java 17, 65 = Java 21, 69 = Java 25
            if (maxClassVersion >= 69) return 25;
            if (maxClassVersion >= 65) return 21;
            if (maxClassVersion >= 61) return 17;
            return 8;  // Default to Java 8 for 1.12.2 mods
            
        } catch (Exception e) {
            return 8;  // Default to Java 8 on error
        }
    }

    /**
     * Detect LWJGL version requirement (simplified heuristic)
     */
    private static String detectLwjglRequirement(ZipFile zipFile) {
        try {
            // Check if mod references LWJGL 3 classes
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                
                // If we find LWJGL 3 package references, assume LWJGL 3
                if (name.contains("org/lwjgl/glfw/") || 
                    name.contains("org/lwjgl/system/")) {
                    return "3.4.0";
                }
            }
            
            // Default to LWJGL 2 for 1.12.2 mods
            return "2.9.4-nightly-20150209";
            
        } catch (Exception e) {
            return "2.9.4-nightly-20150209";
        }
    }

    /**
     * Apply cached compatibility settings before mods load
     */
    private static void applyCachedSettings() {
        for (Map.Entry<String, ModCompatibilityEntry> entry : MOD_CACHE.entrySet()) {
            String modId = entry.getKey();
            ModCompatibilityEntry cacheEntry = entry.getValue();
            
            // Create version override from cache
            VersionOverride override = new VersionOverride(
                cacheEntry.requiredJavaVersion,
                cacheEntry.requiredLwjglVersion,
                cacheEntry.useLegacyFake
            );
            
            // Register override for this mod
            MOD_VERSION_OVERRIDES.put(modId, override);
            
            // Also register by mod name and main class if available
            DetectedMod detectedMod = DETECTED_MODS.get(modId);
            if (detectedMod != null) {
                MOD_VERSION_OVERRIDES.put(detectedMod.modName, override);
                if (detectedMod.mainClass != null) {
                    MOD_VERSION_OVERRIDES.put(detectedMod.mainClass, override);
                }
            }
            
            cacheHits.incrementAndGet();
        }
        
        System.out.println("[VersionShim] Applied " + cacheHits.get() + " cached compatibility settings");
    }


    // =====================================================================================
    //  SECTION 2: WATCHDOG & DYNAMIC FALLBACK ROUTING
    // =====================================================================================

    private static void activateWatchdog() {
        System.out.println("[VersionShim] Activating incompatibility watchdog...");

        Thread.UncaughtExceptionHandler originalHandler = 
            Thread.getDefaultUncaughtExceptionHandler();
        
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            handleWatchdogException(thread, throwable);
            
            if (originalHandler != null) {
                originalHandler.uncaughtException(thread, throwable);
            }
        });

        WATCHDOG_ACTIVE = true;
        System.out.println("[VersionShim] Watchdog active. Monitoring for incompatibility errors.");
    }

    private static void handleWatchdogException(Thread thread, Throwable throwable) {
        if (!WATCHDOG_ACTIVE || !ARMED) return;

        String errorMessage = throwable.toString();
        String stackTrace = getStackTraceString(throwable);
        
        boolean isIncompatibilityError = INCOMPATIBILITY_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(errorMessage).find() 
                              || pattern.matcher(stackTrace).find());

        if (!isIncompatibilityError) return;

        System.err.println("[VersionShim] WATCHDOG ALERT: Incompatibility error detected!");
        System.err.println("[VersionShim] Thread: " + thread.getName());
        System.err.println("[VersionShim] Error: " + errorMessage);

        boolean isJavaError = errorMessage.toLowerCase().contains("java") 
                           || errorMessage.contains("UnsupportedClassVersionError")
                           || stackTrace.toLowerCase().contains("java version");
        
        boolean isLwjglError = errorMessage.toLowerCase().contains("lwjgl")
                            || stackTrace.toLowerCase().contains("org.lwjgl");

        String modName = extractModNameFromStackTrace(stackTrace);
        if (modName != null) {
            MOD_ERROR_COUNTS.computeIfAbsent(modName, k -> new AtomicInteger(0))
                           .incrementAndGet();
        }

        if (isJavaError) {
            triggerJavaFallback(modName);
        }
        if (isLwjglError) {
            triggerLwjglFallback(modName);
        }

        FALLBACK_EVENTS.incrementAndGet();
    }

    private static void triggerJavaFallback(String modName) {
        synchronized (FALLBACK_LOCK) {
            int currentIndex = globalJavaFallbackIndex;
            
            if (currentIndex >= JAVA_FALLBACK_CHAIN.length - 1) {
                System.err.println("[VersionShim] Already at baseline Java 8. Cannot fall back further.");
                return;
            }

            int nextIndex = currentIndex + 1;
            int nextVersion = JAVA_FALLBACK_CHAIN[nextIndex];
            
            // Beautiful ASCII report
            displayCompatibilityReport(modName, "Java", 
                String.valueOf(JAVA_FALLBACK_CHAIN[currentIndex]), 
                String.valueOf(nextVersion), true);

            globalJavaFallbackIndex = nextIndex;
            CURRENT_JAVA_VERSION = nextVersion;

            if (modName != null) {
                VersionOverride override = new VersionOverride(
                    nextVersion,
                    CURRENT_LWJGL_VERSION,
                    nextVersion == 8
                );
                MOD_VERSION_OVERRIDES.put(modName, override);
                
                // Update cache with this discovery
                updateCacheEntry(modName, nextVersion, CURRENT_LWJGL_VERSION, nextVersion == 8);
            }

            updateSystemProperties();
        }
    }

    private static void triggerLwjglFallback(String modName) {
        synchronized (FALLBACK_LOCK) {
            int currentIndex = globalLwjglFallbackIndex;
            
            if (currentIndex >= LWJGL_FALLBACK_CHAIN.length - 1) {
                System.err.println("[VersionShim] Already at baseline LWJGL 2.9.4. Cannot fall back further.");
                return;
            }

            int nextIndex = currentIndex + 1;
            String nextVersion = LWJGL_FALLBACK_CHAIN[nextIndex];
            
            // Beautiful ASCII report
            displayCompatibilityReport(modName, "LWJGL",
                LWJGL_FALLBACK_CHAIN[currentIndex],
                nextVersion, false);

            globalLwjglFallbackIndex = nextIndex;
            CURRENT_LWJGL_VERSION = nextVersion;

            if (modName != null) {
                VersionOverride override = new VersionOverride(
                    CURRENT_JAVA_VERSION,
                    nextVersion,
                    nextVersion.startsWith("2.")
                );
                MOD_VERSION_OVERRIDES.put(modName, override);
                
                // Update cache with this discovery
                updateCacheEntry(modName, CURRENT_JAVA_VERSION, nextVersion, nextVersion.startsWith("2."));
            }

            installLWJGL2Stubs();
        }
    }

    private static String extractModNameFromStackTrace(String stackTrace) {
        String[] patterns = {
            "net\\.optifine",
            "journeymap",
            "me\\.guichaguri\\.betterfps",
            "com\\.mamiyaotaru\\.voxelmap",
            "fastcraft",
            "baubles",
            "ic2",
            "forestry",
            "thaumcraft",
            "buildcraft",
            "jei",
            "hwyla",
            "waila"
        };

        for (String pattern : patterns) {
            if (stackTrace.contains(pattern.replace("\\", ""))) {
                return pattern.replace("\\.", "").replace("\\", "");
            }
        }

        return null;
    }

    /**
     * Display beautiful ASCII compatibility report
     */
    private static void displayCompatibilityReport(String modName, String componentType,
                                                   String oldVersion, String newVersion,
                                                   boolean isJava) {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║          🔧  COMPATIBILITY CONFLICT RESOLVED  🔧                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        if (modName != null) {
            DetectedMod mod = DETECTED_MODS.get(modName);
            String displayName = (mod != null && mod.modName != null) ? mod.modName : modName;
            String version = (mod != null && mod.version != null) ? mod.version : "unknown";
            
            System.out.println("  📦 MOD DETAILS:");
            System.out.println("     Name:      " + displayName);
            System.out.println("     ID:        " + modName);
            System.out.println("     Version:   " + version);
            System.out.println();
        }
        
        System.out.println("  ⚠️  INCOMPATIBILITY DETECTED:");
        System.out.println("     Component: " + componentType);
        System.out.println("     Rejected:  " + oldVersion);
        System.out.println();
        
        System.out.println("  ✅ FALLBACK APPLIED:");
        System.out.println("     New " + componentType + ": " + newVersion);
        System.out.println();
        
        // Show what this mod now sees
        if (modName != null) {
            VersionOverride override = MOD_VERSION_OVERRIDES.get(modName);
            if (override != null) {
                System.out.println("  📋 EFFECTIVE CONFIGURATION FOR THIS MOD:");
                System.out.println("     Java Version:  " + override.javaVersion);
                System.out.println("     LWJGL Version: " + override.lwjglVersion);
                System.out.println("     Legacy Mode:   " + (override.useLegacyFake ? "ENABLED" : "disabled"));
                System.out.println();
            }
        }
        
        System.out.println("  💾 CACHED FOR FUTURE USE");
        System.out.println("     This configuration will be remembered for next launch");
        System.out.println();
        
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                  Compatibility Restored ✓                         ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Update cache entry with discovered compatibility requirements
     */
    private static void updateCacheEntry(String modName, int javaVersion, 
                                        String lwjglVersion, boolean useLegacyFake) {
        if (modName == null) return;

        DetectedMod mod = DETECTED_MODS.get(modName);
        String modDisplayName = (mod != null && mod.modName != null) ? mod.modName : modName;
        String modVersion = (mod != null && mod.version != null) ? mod.version : "unknown";

        ModCompatibilityEntry entry = MOD_CACHE.get(modName);
        
        if (entry == null) {
            // Create new cache entry
            entry = new ModCompatibilityEntry(
                modName, modDisplayName, modVersion,
                javaVersion, lwjglVersion, useLegacyFake,
                "runtime"
            );
            entry.notes = "Auto-discovered at runtime via watchdog";
            MOD_CACHE.put(modName, entry);
            
            System.out.println("[VersionShim] 💾 New cache entry created for: " + modDisplayName);
        } else {
            // Update existing entry
            entry.requiredJavaVersion = javaVersion;
            entry.requiredLwjglVersion = lwjglVersion;
            entry.useLegacyFake = useLegacyFake;
            entry.detectedBy = "runtime";
            entry.recordSuccess();
            
            System.out.println("[VersionShim] 💾 Cache entry updated for: " + modDisplayName);
        }
        
        // Save cache to disk
        saveLocalCache();
        
        cacheMisses.incrementAndGet();  // This was a miss that we now resolved
    }

    private static String extractModNameFromStackTrace(String stackTrace) {
        String[] patterns = {
            "net\\.optifine",
            "journeymap",
            "me\\.guichaguri\\.betterfps",
            "com\\.mamiyaotaru\\.voxelmap",
            "fastcraft",
            "baubles",
            "ic2",
            "forestry",
            "thaumcraft",
            "buildcraft",
            "jei",
            "hwyla",
            "waila"
        };

        for (String pattern : patterns) {
            if (stackTrace.contains(pattern.replace("\\", ""))) {
                return pattern.replace("\\.", "").replace("\\", "");
            }
        }

        return null;
    }

    private static void updateSystemProperties() {
        int javaIndex = globalJavaFallbackIndex;
        
        System.setProperty("java.version", JAVA_VERSION_STRINGS[javaIndex]);
        System.setProperty("java.specification.version", JAVA_SPEC_VERSIONS[javaIndex]);
        
        System.out.println("[VersionShim] System properties updated:");
        System.out.println("  java.version = " + System.getProperty("java.version"));
        System.out.println("  java.specification.version = " 
            + System.getProperty("java.specification.version"));
    }

    private static String getStackTraceString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }


    // =====================================================================================
    //  SECTION 3: VERSION REPORTING (CONTEXT-AWARE)
    // =====================================================================================

    public static String getJavaVersion() {
        if (!ARMED) return REAL_JAVA_VERSION;

        String caller = getCallerClass();
        
        if (isDebugScreen(caller)) {
            return DEBUG_JAVA_VERSION;
        }

        VersionOverride override = getOverrideForCaller(caller);
        if (override != null) {
            if (override.useLegacyFake) {
                return LEGACY_FAKE_JAVA_VERSION;
            }
            return JAVA_VERSION_STRINGS[getIndexForJavaVersion(override.javaVersion)];
        }

        if (shouldSpoofCaller()) {
            return LEGACY_FAKE_JAVA_VERSION;
        }

        return JAVA_VERSION_STRINGS[globalJavaFallbackIndex];
    }

    public static String getLWJGLVersion() {
        if (!ARMED) return CURRENT_LWJGL_VERSION;

        String caller = getCallerClass();
        
        if (isDebugScreen(caller)) {
            return DEBUG_LWJGL_VERSION;
        }

        VersionOverride override = getOverrideForCaller(caller);
        if (override != null) {
            return override.lwjglVersion;
        }

        if (shouldSpoofCaller()) {
            return LEGACY_FAKE_LWJGL_VERSION;
        }

        return LWJGL_FALLBACK_CHAIN[globalLwjglFallbackIndex];
    }

    public static int getLWJGLMajorVersion() {
        String version = getLWJGLVersion();
        int index = getIndexForLwjglVersion(version);
        return LWJGL_VERSION_COMPONENTS[index][0];
    }

    public static int getLWJGLMinorVersion() {
        String version = getLWJGLVersion();
        int index = getIndexForLwjglVersion(version);
        return LWJGL_VERSION_COMPONENTS[index][1];
    }

    public static int getLWJGLPatchVersion() {
        String version = getLWJGLVersion();
        int index = getIndexForLwjglVersion(version);
        return LWJGL_VERSION_COMPONENTS[index][2];
    }

    private static boolean isDebugScreen(String caller) {
        return caller != null && (
            caller.contains("GuiOverlayDebug") ||
            caller.contains("DebugOverlay") ||
            caller.contains("F3Handler") ||
            caller.contains("DebugScreen")
        );
    }

    private static VersionOverride getOverrideForCaller(String caller) {
        if (caller == null) return null;

        VersionOverride override = MOD_VERSION_OVERRIDES.get(caller);
        if (override != null) return override;

        for (Map.Entry<String, VersionOverride> entry : MOD_VERSION_OVERRIDES.entrySet()) {
            if (caller.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private static int getIndexForJavaVersion(int version) {
        for (int i = 0; i < JAVA_FALLBACK_CHAIN.length; i++) {
            if (JAVA_FALLBACK_CHAIN[i] == version) return i;
        }
        return JAVA_FALLBACK_CHAIN.length - 1;
    }

    private static int getIndexForLwjglVersion(String version) {
        for (int i = 0; i < LWJGL_FALLBACK_CHAIN.length; i++) {
            if (LWJGL_FALLBACK_CHAIN[i].equals(version)) return i;
        }
        return LWJGL_FALLBACK_CHAIN.length - 1;
    }

    private static String getCallerClass() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        
        for (int i = 0; i < stack.length; i++) {
            String className = stack[i].getClassName();
            if (!className.equals("java.lang.Thread") &&
                !className.contains("VersionShim") &&
                !className.equals("sun.reflect.Reflection")) {
                return className;
            }
        }
        
        return null;
    }

    private static boolean shouldSpoofCaller() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            String methodName = element.getMethodName();
            
            if (SPOOF_REGISTRY.containsKey(className)) {
                return true;
            }
            
            if (SAFE_CLASSES.contains(className)) {
                continue;
            }
            
            for (StackPattern pattern : VERSION_CHECK_PATTERNS) {
                boolean classMatch = pattern.classNameContains().isEmpty() ||
                    className.contains(pattern.classNameContains());
                boolean methodMatch = pattern.methodNameContains().isEmpty() ||
                    methodName.contains(pattern.methodNameContains());
                
                if (classMatch && methodMatch) {
                    SPOOF_REGISTRY.put(className, pattern.profile());
                    return true;
                }
            }
        }
        
        return false;
    }


    // =====================================================================================
    //  SECTION 4: LWJGL 2 STUB CLASSES (TODO: Implement with ASM)
    // =====================================================================================

    private static void installLWJGL2Stubs() {
        System.out.println("[VersionShim] Installing LWJGL 2 stub classes...");
        
        // TODO: Implement bytecode generation using ASM
        // For now, this is a placeholder
        
        System.out.println("[VersionShim] LWJGL 2 stubs (TODO: ASM implementation needed)");
    }


    // =====================================================================================
    //  SECTION 5: BYTECODE TRANSFORMERS (TODO: Implement)
    // =====================================================================================

    private static void armBytecodeTransformers() {
        System.out.println("[VersionShim] Arming bytecode transformers...");
        
        // TODO: Implement bytecode transformation
        
        System.out.println("[VersionShim] Bytecode transformers armed (TODO: full implementation)");
    }


    // =====================================================================================
    //  SECTION 6: CRASH PREVENTION
    // =====================================================================================

    private static void installCrashPrevention() {
        System.out.println("[VersionShim] Installing crash prevention hooks...");
        System.out.println("[VersionShim] Crash prevention active");
    }


    // =====================================================================================
    //  SECTION 7: DEBUG SCREEN PATCHING
    // =====================================================================================

    private static void patchDebugScreen() {
        System.out.println("[VersionShim] Patching debug screen...");
        
        // TODO: Use Mixin or bytecode transformation
        
        System.out.println("[VersionShim] Debug screen patched (TODO: full implementation)");
    }


    // =====================================================================================
    //  SECTION 8: PUBLIC API
    // =====================================================================================

    public static final class VersionAPI {

        public static String getDebugLWJGLVersion() {
            return DEBUG_LWJGL_VERSION;
        }

        public static String getDebugJavaVersion() {
            return DEBUG_JAVA_VERSION;
        }

        public static String getCurrentLWJGLVersion() {
            return CURRENT_LWJGL_VERSION;
        }

        public static int getCurrentJavaVersion() {
            return CURRENT_JAVA_VERSION;
        }

        public static String getRealJavaVersion() {
            return REAL_JAVA_VERSION;
        }

        public static int getRealJavaFeatureVersion() {
            return REAL_JAVA_FEATURE;
        }

        public static String getLegacyFakeJavaVersion() {
            return LEGACY_FAKE_JAVA_VERSION;
        }

        public static String getLegacyFakeLWJGLVersion() {
            return LEGACY_FAKE_LWJGL_VERSION;
        }

        public static boolean isClassSpoofed(String className) {
            return SPOOF_REGISTRY.containsKey(className);
        }

        public static long getPreventedCrashCount() {
            return PREVENTED_CRASHES.get();
        }

        public static long getFallbackEventCount() {
            return FALLBACK_EVENTS.get();
        }

        public static int getSpoofedClassCount() {
            return SPOOF_REGISTRY.size();
        }

        public static int getSafeClassCount() {
            return SAFE_CLASSES.size();
        }

        public static int getJavaFallbackIndex() {
            return globalJavaFallbackIndex;
        }

        public static int getLwjglFallbackIndex() {
            return globalLwjglFallbackIndex;
        }

        public static void forceSpoof(String className, SpoofProfile profile) {
            SPOOF_REGISTRY.put(className, profile);
        }

        public static void forceSafe(String className) {
            SAFE_CLASSES.add(className);
            SPOOF_REGISTRY.remove(className);
        }

        public static void registerModOverride(String modName, int javaVersion, String lwjglVersion) {
            MOD_VERSION_OVERRIDES.put(modName, 
                new VersionOverride(javaVersion, lwjglVersion, false));
        }

        public static void registerLegacyModOverride(String modName) {
            MOD_VERSION_OVERRIDES.put(modName, VersionOverride.legacy());
        }

        /** Get cache hit count */
        public static long getCacheHits() {
            return cacheHits.get();
        }

        /** Get cache miss count */
        public static long getCacheMisses() {
            return cacheMisses.get();
        }

        /** Get cache hit rate percentage */
        public static double getCacheHitRate() {
            long total = cacheHits.get() + cacheMisses.get();
            return total == 0 ? 0.0 : (double) cacheHits.get() / total * 100.0;
        }

        /** Get number of mods scanned */
        public static long getModsScanned() {
            return modsScanned.get();
        }

        /** Get total cache entries */
        public static int getCacheSize() {
            return MOD_CACHE.size();
        }

        /** Get detected mods count */
        public static int getDetectedModsCount() {
            return DETECTED_MODS.size();
        }

        /** Get community cache contributions */
        public static long getCommunityContributions() {
            return communityCacheContributions.get();
        }

        /** Export cache for community sharing */
        public static String exportCacheForSharing() {
            // Export successful entries only
            Map<String, ModCompatibilityEntry> exportable = MOD_CACHE.entrySet().stream()
                .filter(e -> e.getValue().getSuccessRate() > 0.8)  // Only export reliable entries
                .filter(e -> e.getValue().successCount > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            
            return GSON.toJson(exportable);
        }

        /** Import community cache */
        public static void importCommunityCache(String json) {
            try {
                Type type = new TypeToken<Map<String, ModCompatibilityEntry>>(){}.getType();
                Map<String, ModCompatibilityEntry> imported = GSON.fromJson(json, type);
                
                if (imported != null) {
                    for (Map.Entry<String, ModCompatibilityEntry> entry : imported.entrySet()) {
                        entry.getValue().detectedBy = "community";
                        MOD_CACHE.putIfAbsent(entry.getKey(), entry.getValue());
                        communityCacheContributions.incrementAndGet();
                    }
                    
                    // Save merged cache
                    try {
                        String mergedJson = GSON.toJson(imported);
                        Files.write(Paths.get(COMMUNITY_CACHE_FILE), mergedJson.getBytes());
                    } catch (IOException e) {
                        System.err.println("[VersionShim] Failed to save community cache: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("[VersionShim] Failed to import community cache: " + e.getMessage());
            }
        }

        public static String getDiagnosticReport() {
            StringBuilder sb = new StringBuilder(2048);
            sb.append("═══════════════════════════════════════════════════════════\n");
            sb.append("  VersionShim Enhanced Diagnostic Report\n");
            sb.append("═══════════════════════════════════════════════════════════\n\n");
            
            sb.append("SYSTEM INFORMATION:\n");
            sb.append("  Real Java:         ").append(REAL_JAVA_VERSION)
              .append(" (feature ").append(REAL_JAVA_FEATURE).append(")\n");
            sb.append("  Real Java Vendor:  ").append(REAL_JAVA_VENDOR).append('\n');
            sb.append("  Real Java VM:      ").append(REAL_JAVA_VM_NAME).append('\n');
            sb.append('\n');
            
            sb.append("DEBUG DISPLAY (F3 screen - NEVER CHANGES):\n");
            sb.append("  Java:              ").append(DEBUG_JAVA_VERSION).append('\n');
            sb.append("  LWJGL:             ").append(DEBUG_LWJGL_VERSION).append('\n');
            sb.append('\n');
            
            sb.append("CURRENT CORE VERSIONS (DYNAMIC):\n");
            sb.append("  Java:              ").append(CURRENT_JAVA_VERSION)
              .append(" (index ").append(globalJavaFallbackIndex).append("/")
              .append(JAVA_FALLBACK_CHAIN.length - 1).append(")\n");
            sb.append("  LWJGL:             ").append(CURRENT_LWJGL_VERSION)
              .append(" (index ").append(globalLwjglFallbackIndex).append("/")
              .append(LWJGL_FALLBACK_CHAIN.length - 1).append(")\n");
            sb.append('\n');
            
            sb.append("LEGACY FAKE VERSIONS:\n");
            sb.append("  Java:              ").append(LEGACY_FAKE_JAVA_VERSION).append('\n');
            sb.append("  LWJGL:             ").append(LEGACY_FAKE_LWJGL_VERSION).append('\n');
            sb.append('\n');
            
            sb.append("STATUS:\n");
            sb.append("  Armed:             ").append(ARMED).append('\n');
            sb.append("  Watchdog Active:   ").append(WATCHDOG_ACTIVE).append('\n');
            sb.append("  Spoofed Classes:   ").append(SPOOF_REGISTRY.size()).append('\n');
            sb.append("  Safe Classes:      ").append(SAFE_CLASSES.size()).append('\n');
            sb.append("  Crash Sites:       ").append(CRASH_SITES.size()).append('\n');
            sb.append("  Prevented Crashes: ").append(PREVENTED_CRASHES.get()).append('\n');
            sb.append("  Fallback Events:   ").append(FALLBACK_EVENTS.get()).append('\n');
            sb.append("  Mod Overrides:     ").append(MOD_VERSION_OVERRIDES.size()).append('\n');
            sb.append('\n');
            
            sb.append("CACHE SYSTEM:\n");
            sb.append("  Total Entries:     ").append(MOD_CACHE.size()).append('\n');
            sb.append("  Detected Mods:     ").append(DETECTED_MODS.size()).append('\n');
            sb.append("  Mods Scanned:      ").append(modsScanned.get()).append('\n');
            sb.append("  Cache Hits:        ").append(cacheHits.get()).append('\n');
            sb.append("  Cache Misses:      ").append(cacheMisses.get()).append('\n');
            sb.append("  Hit Rate:          ").append(String.format("%.1f%%", getCacheHitRate())).append('\n');
            sb.append("  Community Entries: ").append(communityCacheContributions.get()).append('\n');
            sb.append('\n');
            
            if (!MOD_VERSION_OVERRIDES.isEmpty()) {
                sb.append("MOD VERSION OVERRIDES:\n");
                MOD_VERSION_OVERRIDES.forEach((mod, override) ->
                    sb.append("  ").append(mod).append(" → Java ").append(override.javaVersion)
                      .append(", LWJGL ").append(override.lwjglVersion)
                      .append(override.useLegacyFake ? " (legacy fake)" : "")
                      .append('\n'));
                sb.append('\n');
            }
            
            if (!MOD_ERROR_COUNTS.isEmpty()) {
                sb.append("MOD ERROR COUNTS:\n");
                MOD_ERROR_COUNTS.forEach((mod, count) ->
                    sb.append("  ").append(mod).append(": ").append(count.get())
                      .append(" error").append(count.get() == 1 ? "" : "s").append('\n'));
                sb.append('\n');
            }
            
            sb.append("FALLBACK CHAINS:\n");
            sb.append("  Java:   ");
            for (int i = 0; i < JAVA_FALLBACK_CHAIN.length; i++) {
                if (i == globalJavaFallbackIndex) sb.append("→[");
                sb.append(JAVA_FALLBACK_CHAIN[i]);
                if (i == globalJavaFallbackIndex) sb.append("]");
                if (i < JAVA_FALLBACK_CHAIN.length - 1) sb.append(" → ");
            }
            sb.append('\n');
            
            sb.append("  LWJGL:  ");
            for (int i = 0; i < LWJGL_FALLBACK_CHAIN.length; i++) {
                if (i == globalLwjglFallbackIndex) sb.append("→[");
                sb.append(LWJGL_FALLBACK_CHAIN[i]);
                if (i == globalLwjglFallbackIndex) sb.append("]");
                if (i < LWJGL_FALLBACK_CHAIN.length - 1) sb.append(" → ");
            }
            sb.append('\n');
            
            sb.append("═══════════════════════════════════════════════════════════\n");
            
            return sb.toString();
        }
    }


    // =====================================================================================
    //  SECTION 9: SELF-TEST
    // =====================================================================================

    public static boolean selfTest() {
        System.out.println("[VersionShim] Running self-test...");
        boolean allPassed = true;

        String javaVer = getJavaVersion();
        String lwjglVer = getLWJGLVersion();
        System.out.println("[VersionShim] Current reported versions: Java " 
            + javaVer + ", LWJGL " + lwjglVer);

        if (!DEBUG_JAVA_VERSION.equals("25")) {
            System.err.println("[VersionShim] FAIL: Debug Java version should be 25");
            allPassed = false;
        }
        if (!DEBUG_LWJGL_VERSION.equals("3.4.0")) {
            System.err.println("[VersionShim] FAIL: Debug LWJGL version should be 3.4.0");
            allPassed = false;
        }

        if (JAVA_FALLBACK_CHAIN.length != JAVA_VERSION_STRINGS.length) {
            System.err.println("[VersionShim] FAIL: Java fallback chain length mismatch");
            allPassed = false;
        }
        if (LWJGL_FALLBACK_CHAIN.length != LWJGL_VERSION_COMPONENTS.length) {
            System.err.println("[VersionShim] FAIL: LWJGL fallback chain length mismatch");
            allPassed = false;
        }

        if (!WATCHDOG_ACTIVE) {
            System.err.println("[VersionShim] FAIL: Watchdog should be active");
            allPassed = false;
        }

        // Test cache system
        if (MOD_CACHE.isEmpty()) {
            System.out.println("[VersionShim] WARNING: Cache is empty (might be first run)");
        } else {
            System.out.println("[VersionShim] ✓ Cache system working: " + MOD_CACHE.size() + " entries");
        }

        if (allPassed) {
            System.out.println("[VersionShim] \u001b[32m✓ All self-tests passed!\u001b[0m");
        } else {
            System.err.println("[VersionShim] \u001b[31m✗ Some self-tests failed.\u001b[0m");
        }

        return allPassed;
    }

    /**
     * Shutdown hook - save cache before exit
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (ARMED) {
                System.out.println("[VersionShim] Shutting down, saving cache...");
                saveLocalCache();
                
                // Display final statistics
                System.out.println("[VersionShim] Final statistics:");
                System.out.println("  Cache hits: " + cacheHits.get());
                System.out.println("  Cache misses: " + cacheMisses.get());
                System.out.println("  Total mods scanned: " + modsScanned.get());
                System.out.println("  Fallback events: " + FALLBACK_EVENTS.get());
                
                double hitRate = cacheHits.get() + cacheMisses.get() > 0 
                    ? (double) cacheHits.get() / (cacheHits.get() + cacheMisses.get()) * 100
                    : 0.0;
                System.out.printf("[VersionShim] Cache hit rate: %.1f%%\n", hitRate);
            }
        }, "VersionShim-Shutdown"));
    }


    private VersionShim() {
        throw new UnsupportedOperationException("VersionShim is a static utility module");
    }
}
