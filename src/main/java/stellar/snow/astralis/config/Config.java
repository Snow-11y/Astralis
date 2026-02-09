/**
 * Config - Astralis Configuration Manager
 *
 * <h2>Purpose:</h2>
 * Central configuration system for Astralis Universal Patcher.
 * Manages all user-configurable settings with runtime validation
 * against hardware capabilities via UniversalCapabilities.
 *
 * <h2>Configuration Categories:</h2>
 * <ul>
 *   <li>Graphics API Selection (OpenGL/GLES/Vulkan)</li>
 *   <li>Shader Engine Selection (GLSL/SPIR-V)</li>
 *   <li>Performance Thresholds</li>
 *   <li>Compatibility Modes</li>
 *   <li>Logging Settings</li>
 * </ul>
 *
 * @author Astralis Team
 * @version 1.0.0 - Java 25 / LWJGL 3.3.6
 * @since Astralis 1.0
 */

package stellar.snow.astralis.nexus.controlpanel;

// ═══════════════════════════════════════════════════════════════════════════════════════
// Astralis Internal Imports
// ═══════════════════════════════════════════════════════════════════════════════════════
import stellar.snow.astralis.engine.gpu.authority.UniversalCapabilities;

// ═══════════════════════════════════════════════════════════════════════════════════════
// Java Imports
// ═══════════════════════════════════════════════════════════════════════════════════════
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Central configuration manager for Astralis
 */
public final class Config {

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 1: CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /** Configuration file name */
    private static final String CONFIG_FILE_NAME = "astralis.cfg";

    /** Configuration directory */
    private static final String CONFIG_DIRECTORY = "config";

    /** Configuration version for migration */
    private static final int CONFIG_VERSION = 1;

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 2: ENUMS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Preferred Graphics API
     */
    public enum PreferredAPI {
        AUTO("Automatic - Best available"),
        OPENGL("OpenGL - Desktop standard"),
        OPENGL_ES("OpenGL ES - Mobile/Embedded"),
        VULKAN("Vulkan - Modern low-level API");

        public final String description;

        PreferredAPI(String description) {
            this.description = description;
        }
    }

    /**
     * Preferred Shader Engine
     */
    public enum ShaderEngine {
        AUTO("Automatic - Best available"),
        GLSL("GLSL - OpenGL Shading Language"),
        SPIRV("SPIR-V - Standard Portable Intermediate Representation");

        public final String description;

        ShaderEngine(String description) {
            this.description = description;
        }
    }

    /**
     * Renderer Override
     */
    public enum RendererOverride {
        AUTO("Automatic detection"),
        VANILLA("Force Vanilla renderer"),
        OPTIFINE("Force Optifine compatibility"),
        SODIUM("Force Sodium compatibility"),
        RUBIDIUM("Force Rubidium compatibility"),
        EMBEDDIUM("Force Embeddium compatibility"),
        CELERITAS("Force Celeritas compatibility"),
        NOTHIRIUM("Force Nothirium compatibility"),
        NEONIUM("Force Neonium compatibility"),
        RELICTIUM("Force Relictium compatibility"),
        VINTAGIUM("Force Vintagium compatibility"),
        KIRINO("Force Kirino compatibility"),
        SNOWIUM("Force Snowium compatibility");

        public final String description;

        RendererOverride(String description) {
            this.description = description;
        }
    }

    /**
     * Logging Level
     */
    public enum LoggingLevel {
        OFF("No logging"),
        MINIMAL("Errors only"),
        NORMAL("Important events"),
        VERBOSE("All calls"),
        DEBUG("Maximum detail");

        public final String description;

        LoggingLevel(String description) {
            this.description = description;
        }
    }

    /**
     * Performance Profile
     */
    public enum PerformanceProfile {
        POTATO("Minimal features, maximum compatibility"),
        LOW("Basic features, high compatibility"),
        BALANCED("Balance of features and performance"),
        HIGH("Modern features, good hardware required"),
        ULTRA("All features, best hardware required"),
        CUSTOM("User-defined settings");

        public final String description;

        PerformanceProfile(String description) {
            this.description = description;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 3: STATE
    // ═══════════════════════════════════════════════════════════════════════════════════

    /** Initialization flag */
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    /** Configuration file path */
    private static Path configPath;

    /** Configuration values storage */
    private static final ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();

    /** Default values */
    private static final Map<String, Object> defaults = new LinkedHashMap<>();

    /** Change listeners */
    private static final ConcurrentHashMap<String, Runnable> changeListeners = new ConcurrentHashMap<>();

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 4: DEFAULT VALUES
    // ═══════════════════════════════════════════════════════════════════════════════════

    static {
        // API Settings
        defaults.put("preferredAPI", PreferredAPI.AUTO);
        defaults.put("shaderEngine", ShaderEngine.AUTO);
        defaults.put("rendererOverride", RendererOverride.AUTO);

        // Performance Profile
        defaults.put("performanceProfile", PerformanceProfile.BALANCED);

        // Thresholds
        defaults.put("teleportThresholdSquared", 64.0);
        defaults.put("maxFrameTime", 0.25);
        defaults.put("fixedTimestep", 0.05); // 1/20
        defaults.put("aiThrottleMask", 1);
        defaults.put("batchSize", 256);

        // State Management
        defaults.put("enableStateCache", true);
        defaults.put("stateValidationInterval", 60);
        defaults.put("syncStateOnExternalChange", true);

        // Exception Safety
        defaults.put("enableExceptionSafety", true);
        defaults.put("maxStateStackDepth", 16);

        // Logging
        defaults.put("loggingLevel", LoggingLevel.NORMAL);
        defaults.put("logBufferSize", 1024);
        defaults.put("logFlushIntervalMs", 5000L);
        defaults.put("logCallDetails", true);
        defaults.put("logFallbacks", true);

        // Compatibility
        defaults.put("allowFallback", true);
        defaults.put("maxFallbackAttempts", 5);
        defaults.put("skipStateManagerOverwrite", false);
        defaults.put("deferStateChanges", false);

        // Engine Version Limits
        defaults.put("minGLMajor", 1);
        defaults.put("minGLMinor", 5);
        defaults.put("maxGLMajor", 4);
        defaults.put("maxGLMinor", 6);
        defaults.put("minGLESMajor", 2);
        defaults.put("minGLESMinor", 0);
        defaults.put("maxGLESMajor", 3);
        defaults.put("maxGLESMinor", 2);
        defaults.put("minVulkanMajor", 1);
        defaults.put("minVulkanMinor", 0);
        defaults.put("maxVulkanMajor", 1);
        defaults.put("maxVulkanMinor", 4);

        // Feature Toggles
        defaults.put("useVBO", true);
        defaults.put("useVAO", true);
        defaults.put("useInstancing", true);
        defaults.put("useDSA", true);
        defaults.put("usePersistentMapping", true);
        defaults.put("useComputeShaders", false);
        defaults.put("useSPIRV", false);

        // Wrapper-specific
        defaults.put("trustDriverVersion", true);
        defaults.put("enableWrapperQuirks", true);

        // Debug
        defaults.put("enableDebugOutput", false);
        defaults.put("validateOnEveryCall", false);
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 5: INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Private constructor - utility class
     */
    private Config() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Initialize configuration system
     */
    public static void initialize() {
        if (initialized.getAndSet(true)) return;

        try {
            // Setup config path
            Path configDir = Paths.get(CONFIG_DIRECTORY);
            Files.createDirectories(configDir);
            configPath = configDir.resolve(CONFIG_FILE_NAME);

            // Load defaults
            values.putAll(defaults);

            // Load from file if exists
            if (Files.exists(configPath)) {
                load();
            } else {
                save();
            }

            // Validate against capabilities
            validateAgainstCapabilities();

        } catch (IOException e) {
            System.err.println("[Astralis Config] Initialization failed: " + e.getMessage());
            values.putAll(defaults);
        }
    }

    /**
     * Load configuration from file
     */
    public static void load() {
        if (!initialized.get()) initialize();

        try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;

                int eqIndex = line.indexOf('=');
                if (eqIndex <= 0) continue;

                String key = line.substring(0, eqIndex).trim();
                String valueStr = line.substring(eqIndex + 1).trim();

                if (defaults.containsKey(key)) {
                    Object defaultValue = defaults.get(key);
                    Object parsed = parseValue(valueStr, defaultValue.getClass());
                    if (parsed != null) {
                        values.put(key, parsed);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Astralis Config] Load failed: " + e.getMessage());
        }
    }

    /**
     * Save configuration to file
     */
    public static void save() {
        if (!initialized.get()) initialize();

        try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            
            writer.write("# Astralis Configuration\n");
            writer.write("# Version: " + CONFIG_VERSION + "\n");
            writer.write("# Generated: " + java.time.Instant.now() + "\n");
            writer.write("\n");

            // Group by category
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# API Settings\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "preferredAPI");
            writeValue(writer, "shaderEngine");
            writeValue(writer, "rendererOverride");
            writer.write("\n");

            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# Performance\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "performanceProfile");
            writeValue(writer, "teleportThresholdSquared");
            writeValue(writer, "maxFrameTime");
            writeValue(writer, "fixedTimestep");
            writeValue(writer, "aiThrottleMask");
            writeValue(writer, "batchSize");
            writer.write("\n");

            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# State Management\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "enableStateCache");
            writeValue(writer, "stateValidationInterval");
            writeValue(writer, "syncStateOnExternalChange");
            writeValue(writer, "enableExceptionSafety");
            writeValue(writer, "maxStateStackDepth");
            writer.write("\n");

            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# Logging\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "loggingLevel");
            writeValue(writer, "logBufferSize");
            writeValue(writer, "logFlushIntervalMs");
            writeValue(writer, "logCallDetails");
            writeValue(writer, "logFallbacks");
            writer.write("\n");

            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# Compatibility\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "allowFallback");
            writeValue(writer, "maxFallbackAttempts");
            writeValue(writer, "skipStateManagerOverwrite");
            writeValue(writer, "deferStateChanges");
            writer.write("\n");

            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# Feature Toggles\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "useVBO");
            writeValue(writer, "useVAO");
            writeValue(writer, "useInstancing");
            writeValue(writer, "useDSA");
            writeValue(writer, "usePersistentMapping");
            writeValue(writer, "useComputeShaders");
            writeValue(writer, "useSPIRV");
            writer.write("\n");

            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# Debug\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "enableDebugOutput");
            writeValue(writer, "validateOnEveryCall");
            writeValue(writer, "trustDriverVersion");
            writeValue(writer, "enableWrapperQuirks");

        } catch (IOException e) {
            System.err.println("[Astralis Config] Save failed: " + e.getMessage());
        }
    }

    private static void writeValue(BufferedWriter writer, String key) throws IOException {
        Object value = values.getOrDefault(key, defaults.get(key));
        String valueStr = value instanceof Enum<?> ? ((Enum<?>) value).name() : String.valueOf(value);
        writer.write(key + "=" + valueStr + "\n");
    }

    /**
     * Parse string value to appropriate type
     */
    @SuppressWarnings("unchecked")
    private static Object parseValue(String str, Class<?> type) {
        try {
            if (type == Boolean.class || type == boolean.class) {
                return Boolean.parseBoolean(str);
            } else if (type == Integer.class || type == int.class) {
                return Integer.parseInt(str);
            } else if (type == Long.class || type == long.class) {
                return Long.parseLong(str);
            } else if (type == Double.class || type == double.class) {
                return Double.parseDouble(str);
            } else if (type == Float.class || type == float.class) {
                return Float.parseFloat(str);
            } else if (type == String.class) {
                return str;
            } else if (type.isEnum()) {
                return Enum.valueOf((Class<Enum>) type, str.toUpperCase());
            }
        } catch (Exception e) {
            // Return null to use default
        }
        return null;
    }

    /**
     * Validate configuration against hardware capabilities
     */
    private static void validateAgainstCapabilities() {
        try {
            if (!UniversalCapabilities.isInitialized()) {
                UniversalCapabilities.detect();
            }

            // Validate API preference
            PreferredAPI api = getPreferredAPI();
            if (api == PreferredAPI.VULKAN && !UniversalCapabilities.Vulkan.isAvailable) {
                values.put("preferredAPI", PreferredAPI.OPENGL);
            }
            if (api == PreferredAPI.OPENGL_ES && !UniversalCapabilities.GLES.isGLESContext) {
                values.put("preferredAPI", PreferredAPI.OPENGL);
            }

            // Validate shader engine
            ShaderEngine shader = getPreferredShaderEngine();
            if (shader == ShaderEngine.SPIRV && !UniversalCapabilities.SPIRV.hasGLSPIRV) {
                values.put("shaderEngine", ShaderEngine.GLSL);
            }

            // Validate feature toggles against capabilities
            if (getBoolean("useDSA") && !UniversalCapabilities.Features.DSA) {
                values.put("useDSA", false);
            }
            if (getBoolean("usePersistentMapping") && !UniversalCapabilities.Features.persistentMapping) {
                values.put("usePersistentMapping", false);
            }
            if (getBoolean("useComputeShaders") && !UniversalCapabilities.Features.computeShaders) {
                values.put("useComputeShaders", false);
            }
            if (getBoolean("useSPIRV") && !UniversalCapabilities.SPIRV.hasGLSPIRV) {
                values.put("useSPIRV", false);
            }

        } catch (Throwable t) {
            System.err.println("[Astralis Config] Capability validation failed: " + t.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 6: GETTERS - API Settings
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static PreferredAPI getPreferredAPI() {
        if (!initialized.get()) initialize();
        return (PreferredAPI) values.getOrDefault("preferredAPI", defaults.get("preferredAPI"));
    }

    public static ShaderEngine getPreferredShaderEngine() {
        if (!initialized.get()) initialize();
        return (ShaderEngine) values.getOrDefault("shaderEngine", defaults.get("shaderEngine"));
    }

    public static RendererOverride getRendererOverride() {
        if (!initialized.get()) initialize();
        return (RendererOverride) values.getOrDefault("rendererOverride", defaults.get("rendererOverride"));
    }

    public static PerformanceProfile getPerformanceProfile() {
        if (!initialized.get()) initialize();
        return (PerformanceProfile) values.getOrDefault("performanceProfile", defaults.get("performanceProfile"));
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 7: GETTERS - Thresholds
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static double getTeleportThresholdSquared() {
        if (!initialized.get()) initialize();
        return getDouble("teleportThresholdSquared");
    }

    public static double getMaxFrameTime() {
        if (!initialized.get()) initialize();
        return getDouble("maxFrameTime");
    }

    public static double getFixedTimestep() {
        if (!initialized.get()) initialize();
        return getDouble("fixedTimestep");
    }

    public static int getAIThrottleMask() {
        if (!initialized.get()) initialize();
        return getInt("aiThrottleMask");
    }

    public static int getBatchSize() {
        if (!initialized.get()) initialize();
        return getInt("batchSize");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 8: GETTERS - State Management
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static boolean isStateCacheEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("enableStateCache");
    }

    public static int getStateValidationInterval() {
        if (!initialized.get()) initialize();
        return getInt("stateValidationInterval");
    }

    public static boolean isSyncStateOnExternalChange() {
        if (!initialized.get()) initialize();
        return getBoolean("syncStateOnExternalChange");
    }

    public static boolean isExceptionSafetyEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("enableExceptionSafety");
    }

    public static int getMaxStateStackDepth() {
        if (!initialized.get()) initialize();
        return getInt("maxStateStackDepth");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 9: GETTERS - Logging
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static LoggingLevel getLoggingLevel() {
        if (!initialized.get()) initialize();
        return (LoggingLevel) values.getOrDefault("loggingLevel", defaults.get("loggingLevel"));
    }

    public static int getLogBufferSize() {
        if (!initialized.get()) initialize();
        return getInt("logBufferSize");
    }

    public static long getLogFlushIntervalMs() {
        if (!initialized.get()) initialize();
        return getLong("logFlushIntervalMs");
    }

    public static boolean isLogCallDetails() {
        if (!initialized.get()) initialize();
        return getBoolean("logCallDetails");
    }

    public static boolean isLogFallbacks() {
        if (!initialized.get()) initialize();
        return getBoolean("logFallbacks");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 10: GETTERS - Compatibility
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static boolean isAllowFallback() {
        if (!initialized.get()) initialize();
        return getBoolean("allowFallback");
    }

    public static int getMaxFallbackAttempts() {
        if (!initialized.get()) initialize();
        return getInt("maxFallbackAttempts");
    }

    public static boolean isSkipStateManagerOverwrite() {
        if (!initialized.get()) initialize();
        return getBoolean("skipStateManagerOverwrite");
    }

    public static boolean isDeferStateChanges() {
        if (!initialized.get()) initialize();
        return getBoolean("deferStateChanges");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 11: GETTERS - Feature Toggles
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static boolean isUseVBO() {
        if (!initialized.get()) initialize();
        return getBoolean("useVBO");
    }

    public static boolean isUseVAO() {
        if (!initialized.get()) initialize();
        return getBoolean("useVAO");
    }

    public static boolean isUseInstancing() {
        if (!initialized.get()) initialize();
        return getBoolean("useInstancing");
    }

    public static boolean isUseDSA() {
        if (!initialized.get()) initialize();
        return getBoolean("useDSA");
    }

    public static boolean isUsePersistentMapping() {
        if (!initialized.get()) initialize();
        return getBoolean("usePersistentMapping");
    }

    public static boolean isUseComputeShaders() {
        if (!initialized.get()) initialize();
        return getBoolean("useComputeShaders");
    }

    public static boolean isUseSPIRV() {
        if (!initialized.get()) initialize();
        return getBoolean("useSPIRV");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 12: GETTERS - Debug
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static boolean isDebugOutputEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("enableDebugOutput");
    }

    public static boolean isValidateOnEveryCall() {
        if (!initialized.get()) initialize();
        return getBoolean("validateOnEveryCall");
    }

    public static boolean isTrustDriverVersion() {
        if (!initialized.get()) initialize();
        return getBoolean("trustDriverVersion");
    }

    public static boolean isEnableWrapperQuirks() {
        if (!initialized.get()) initialize();
        return getBoolean("enableWrapperQuirks");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 13: GENERIC GETTERS
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static boolean getBoolean(String key) {
        Object value = values.getOrDefault(key, defaults.get(key));
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }

    public static int getInt(String key) {
        Object value = values.getOrDefault(key, defaults.get(key));
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return 0; }
    }

    public static long getLong(String key) {
        Object value = values.getOrDefault(key, defaults.get(key));
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (Exception e) { return 0L; }
    }

    public static double getDouble(String key) {
        Object value = values.getOrDefault(key, defaults.get(key));
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); } catch (Exception e) { return 0.0; }
    }

    public static String getString(String key) {
        return String.valueOf(values.getOrDefault(key, defaults.getOrDefault(key, "")));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T getEnum(String key, Class<T> enumClass) {
        Object value = values.getOrDefault(key, defaults.get(key));
        if (enumClass.isInstance(value)) return (T) value;
        try { return Enum.valueOf(enumClass, String.valueOf(value).toUpperCase()); } 
        catch (Exception e) { return null; }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 14: SETTERS
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static void set(String key, Object value) {
        if (!initialized.get()) initialize();
        values.put(key, value);
        notifyChange(key);
    }

    public static void setPreferredAPI(PreferredAPI api) {
        set("preferredAPI", api);
    }

    public static void setPreferredShaderEngine(ShaderEngine engine) {
        set("shaderEngine", engine);
    }

    public static void setPerformanceProfile(PerformanceProfile profile) {
        set("performanceProfile", profile);
        applyPerformanceProfile(profile);
    }

    public static void setLoggingLevel(LoggingLevel level) {
        set("loggingLevel", level);
    }

    /**
     * Apply a performance profile preset
     */
    private static void applyPerformanceProfile(PerformanceProfile profile) {
        switch (profile) {
            case POTATO -> {
                values.put("useVBO", true);
                values.put("useVAO", false);
                values.put("useInstancing", false);
                values.put("useDSA", false);
                values.put("usePersistentMapping", false);
                values.put("useComputeShaders", false);
                values.put("useSPIRV", false);
                values.put("batchSize", 64);
            }
            case LOW -> {
                values.put("useVBO", true);
                values.put("useVAO", true);
                values.put("useInstancing", false);
                values.put("useDSA", false);
                values.put("usePersistentMapping", false);
                values.put("useComputeShaders", false);
                values.put("useSPIRV", false);
                values.put("batchSize", 128);
            }
            case BALANCED -> {
                values.put("useVBO", true);
                values.put("useVAO", true);
                values.put("useInstancing", true);
                values.put("useDSA", false);
                values.put("usePersistentMapping", false);
                values.put("useComputeShaders", false);
                values.put("useSPIRV", false);
                values.put("batchSize", 256);
            }
            case HIGH -> {
                values.put("useVBO", true);
                values.put("useVAO", true);
                values.put("useInstancing", true);
                values.put("useDSA", true);
                values.put("usePersistentMapping", true);
                values.put("useComputeShaders", false);
                values.put("useSPIRV", false);
                values.put("batchSize", 512);
            }
            case ULTRA -> {
                values.put("useVBO", true);
                values.put("useVAO", true);
                values.put("useInstancing", true);
                values.put("useDSA", true);
                values.put("usePersistentMapping", true);
                values.put("useComputeShaders", true);
                values.put("useSPIRV", true);
                values.put("batchSize", 1024);
            }
            case CUSTOM -> {
                // Don't modify - user settings
            }
        }
        validateAgainstCapabilities();
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 15: CHANGE LISTENERS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Register a change listener for a specific key
     */
    public static void addChangeListener(String key, Runnable listener) {
        changeListeners.put(key, listener);
    }

    /**
     * Remove a change listener
     */
    public static void removeChangeListener(String key) {
        changeListeners.remove(key);
    }

    /**
     * Notify listeners of a change
     */
    private static void notifyChange(String key) {
        Runnable listener = changeListeners.get(key);
        if (listener != null) {
            try {
                listener.run();
            } catch (Throwable t) {
                System.err.println("[Astralis Config] Listener error for " + key + ": " + t.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 16: UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Reset all settings to defaults
     */
    public static void resetToDefaults() {
        values.clear();
        values.putAll(defaults);
        validateAgainstCapabilities();
        save();
    }

    /**
     * Get all current values as a map
     */
    public static Map<String, Object> getAllValues() {
        return new LinkedHashMap<>(values);
    }

    /**
     * Check if a key exists
     */
    public static boolean hasKey(String key) {
        return values.containsKey(key) || defaults.containsKey(key);
    }

    /**
     * Get config file path
     */
    public static Path getConfigPath() {
        if (!initialized.get()) initialize();
        return configPath;
    }

    /**
     * Reload configuration from file
     */
    public static void reload() {
        load();
        validateAgainstCapabilities();
    }
}
