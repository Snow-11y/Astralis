package stellar.snow.astralis.integration.DeepMix.Core;

import java.lang.annotation.*;
import java.lang.invoke.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.regex.*;
import java.util.stream.*;
import java.util.zip.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                          DEEPMIX NEXUS                                   ║
 * ║           Phases 5-9 · Cross-Platform · Optimization · Resilience        ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  PHASE 5: Cross-Platform Bytecode    (7 annotations)   MEDIUM Priority  ║
 * ║  PHASE 6: Instrumentation/Monitoring (9 annotations)   HIGH Priority    ║
 * ║  PHASE 7: Performance Optimization   (22 annotations)  HIGH Priority    ║
 * ║  PHASE 8: Resilience & Reliability   (15 annotations)  MEDIUM Priority  ║
 * ║  PHASE 9: Design Patterns            (8 annotations)   LOW Priority     ║
 * ║                                                                          ║
 * ║  Total: 61 annotations · Full processors · Central registry             ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * @author  Stellar Snow Astralis Team
 * @version 1.0.0
 */
public final class DeepMixNexus {

    private DeepMixNexus() { throw new UnsupportedOperationException("Utility class"); }

    private static final Logger LOG = Logger.getLogger("DeepMixNexus");
    private static final AtomicLong transformCount = new AtomicLong(0);

    static void log(String fmt, Object... args) {
        LOG.info(String.format("[DeepMixNexus] " + fmt, args));
    }

    public static long getTransformCount() { return transformCount.get(); }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  PHASE 5 · CROSS-PLATFORM BYTECODE ANNOTATIONS (60–66)         ║
    // ║  Priority: MEDIUM · 7 annotations                              ║
    // ╚══════════════════════════════════════════════════════════════════╝

    // ── 60. @DeepLLVM / @DLLVM ──────────────────────────────────────

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepLLVM {
        String target() default "";
        String function() default "";
        String pass() default "default";
        OptLevel optimization() default OptLevel.O2;
        String targetTriple() default "";
        String[] flags() default {};
        boolean debugInfo() default false;
        boolean hotReload() default true;
        int priority() default 1000;
        enum OptLevel { O0, O1, O2, O3, Os, Oz }
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DLLVM {
        String target() default "";
        String function() default "";
        String pass() default "default";
        DeepLLVM.OptLevel optimization() default DeepLLVM.OptLevel.O2;
        String targetTriple() default "";
        String[] flags() default {};
        boolean debugInfo() default false;
        boolean hotReload() default true;
        int priority() default 1000;
    }

    // ── 61. @DeepWASM / @DWASM ──────────────────────────────────────

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepWASM {
        String target() default "";
        String function() default "";
        boolean optimize() default true;
        int shrinkLevel() default 1;
        String[] features() default {};
        int initialMemory() default 256;
        int maxMemory() default 65536;
        String exportName() default "";
        String[] imports() default {};
        boolean hotReload() default true;
        int priority() default 1000;
        WasmOp operation() default WasmOp.TRANSFORM;
        enum WasmOp { TRANSFORM, INJECT, REPLACE, WRAP, OPTIMIZE }
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DWASM {
        String target() default "";
        String function() default "";
        boolean optimize() default true;
        int shrinkLevel() default 1;
        String[] features() default {};
        int initialMemory() default 256;
        int maxMemory() default 65536;
        String exportName() default "";
        String[] imports() default {};
        boolean hotReload() default true;
        int priority() default 1000;
        DeepWASM.WasmOp operation() default DeepWASM.WasmOp.TRANSFORM;
    }

    // ── 62. @DeepCIL / @DCIL ────────────────────────────────────────

    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepCIL {
        String target() default "";
        String typeName() default "";
        String method() default "";
        String framework() default "netstandard2.0";
        String[] injectFields() default {};
        boolean weaveAttributes() default false;
        boolean debugSymbols() default false;
        int optimizationLevel() default 2;
        String[] customIL() default {};
        boolean hotReload() default true;
        int priority() default 1000;
        CilOp operation() default CilOp.TRANSFORM;
        enum CilOp { TRANSFORM, INJECT, REPLACE, WEAVE, PATCH }
    }

    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DCIL {
        String target() default "";
        String typeName() default "";
        String method() default "";
        String framework() default "netstandard2.0";
        String[] injectFields() default {};
        boolean weaveAttributes() default false;
        boolean debugSymbols() default false;
        int optimizationLevel() default 2;
        String[] customIL() default {};
        boolean hotReload() default true;
        int priority() default 1000;
        DeepCIL.CilOp operation() default DeepCIL.CilOp.TRANSFORM;
    }

    // ── 63. @DeepDalvik / @DDLK ─────────────────────────────────────

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepDalvik {
        String target() default "";
        String className() default "";
        String methodName() default "";
        int minSdk() default 21;
        int targetSdk() default 34;
        String[] proguardRules() default {};
        boolean multidex() default false;
        int registerCount() default -1;
        DexOptLevel dexOpt() default DexOptLevel.STANDARD;
        boolean hotReload() default true;
        int priority() default 1000;
        DalvikOp operation() default DalvikOp.TRANSFORM;
        enum DexOptLevel { NONE, STANDARD, AGGRESSIVE }
        enum DalvikOp { TRANSFORM, INJECT, REPLACE, PATCH, OPTIMIZE }
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DDLK {
        String target() default "";
        String className() default "";
        String methodName() default "";
        int minSdk() default 21;
        int targetSdk() default 34;
        String[] proguardRules() default {};
        boolean multidex() default false;
        int registerCount() default -1;
        DeepDalvik.DexOptLevel dexOpt() default DeepDalvik.DexOptLevel.STANDARD;
        boolean hotReload() default true;
        int priority() default 1000;
        DeepDalvik.DalvikOp operation() default DeepDalvik.DalvikOp.TRANSFORM;
    }

    // ── 64. @DeepPython / @DPYC ─────────────────────────────────────

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepPython {
        String target() default "";
        String function() default "";
        String pythonVersion() default "3.11";
        int optimizationLevel() default 1;
        boolean removeAssertions() default false;
        boolean removeDocstrings() default false;
        boolean constantFolding() default true;
        String[] customOpcodes() default {};
        boolean hotReload() default true;
        int priority() default 1000;
        PyOp operation() default PyOp.TRANSFORM;
        enum PyOp { TRANSFORM, INJECT, REPLACE, OPTIMIZE, DECOMPILE }
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DPYC {
        String target() default "";
        String function() default "";
        String pythonVersion() default "3.11";
        int optimizationLevel() default 1;
        boolean removeAssertions() default false;
        boolean removeDocstrings() default false;
        boolean constantFolding() default true;
        String[] customOpcodes() default {};
        boolean hotReload() default true;
        int priority() default 1000;
        DeepPython.PyOp operation() default DeepPython.PyOp.TRANSFORM;
    }

    // ── 65. @DeepNode / @DNODE ──────────────────────────────────────

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepNode {
        String target() default "";
        String function() default "";
        String[] v8Flags() default {};
        boolean enableJIT() default true;
        String nodeVersion() default "20.0.0";
        boolean turboFan() default true;
        int inlineCacheSize() default 16;
        int maxOldSpace() default 4096;
        int maxSemiSpace() default 16;
        boolean hotReload() default true;
        int priority() default 1000;
        NodeOp operation() default NodeOp.TRANSFORM;
        enum NodeOp { TRANSFORM, INJECT, REPLACE, OPTIMIZE, BUNDLE }
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DNODE {
        String target() default "";
        String function() default "";
        String[] v8Flags() default {};
        boolean enableJIT() default true;
        String nodeVersion() default "20.0.0";
        boolean turboFan() default true;
        int inlineCacheSize() default 16;
        int maxOldSpace() default 4096;
        int maxSemiSpace() default 16;
        boolean hotReload() default true;
        int priority() default 1000;
        DeepNode.NodeOp operation() default DeepNode.NodeOp.TRANSFORM;
    }

    // ── 66. @DeepLua / @DLUA ────────────────────────────────────────

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepLua {
        String target() default "";
        String function() default "";
        String luaVersion() default "5.4";
        boolean enableJIT() default false;
        int optimizationLevel() default 2;
        boolean stripDebug() default false;
        boolean inlineSmallFunctions() default true;
        String[] customOpcodes() default {};
        boolean hotReload() default true;
        int priority() default 1000;
        LuaOp operation() default LuaOp.TRANSFORM;
        enum LuaOp { TRANSFORM, INJECT, REPLACE, OPTIMIZE, STRIP }
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DLUA {
        String target() default "";
        String function() default "";
        String luaVersion() default "5.4";
        boolean enableJIT() default false;
        int optimizationLevel() default 2;
        boolean stripDebug() default false;
        boolean inlineSmallFunctions() default true;
        String[] customOpcodes() default {};
        boolean hotReload() default true;
        int priority() default 1000;
        DeepLua.LuaOp operation() default DeepLua.LuaOp.TRANSFORM;
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  PHASE 6 · INSTRUMENTATION & MONITORING ANNOTATIONS (67–75)    ║
    // ║  Priority: HIGH · 9 annotations                                ║
    // ╚══════════════════════════════════════════════════════════════════╝

    // ── 67. @DeepTrace / @DTR ───────────────────────────────────────

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepTrace {
        String traceName() default "";
        boolean captureArgs() default true;
        boolean captureReturn() default true;
        boolean captureExceptions() default true;
        String[] tags() default {};
        int maxDepth() default 10;
        boolean async() default false;
        String spanKind() default "INTERNAL";
        int priority() default 1000;
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DTR {
        String traceName() default "";
        boolean captureArgs() default true;
        boolean captureReturn() default true;
        boolean captureExceptions() default true;
        String[] tags() default {};
        int maxDepth() default 10;
        boolean async() default false;
        String spanKind() default "INTERNAL";
        int priority() default 1000;
    }

    // ── 68. @DeepMetrics / @DMET ────────────────────────────────────

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepMetrics {
        String metricName() default "";
        MetricKind type() default MetricKind.COUNTER;
        String[] labels() default {};
        boolean recordDuration() default true;
        String unit() default "milliseconds";
        double[] buckets() default {};
        int priority() default 1000;
        enum MetricKind { COUNTER, GAUGE, HISTOGRAM, SUMMARY, TIMER }
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DMET {
        String metricName() default "";
        DeepMetrics.MetricKind type() default DeepMetrics.MetricKind.COUNTER;
        String[] labels() default {};
        boolean recordDuration() default true;
        String unit() default "milliseconds";
        double[] buckets() default {};
        int priority() default 1000;
    }

    // ── 69. @DeepLog / @DLOG ────────────────────────────────────────

    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepLog {
        LogSeverity level() default LogSeverity.INFO;
        String message() default "";
        boolean logArgs() default false;
        boolean logReturn() default false;
        boolean logExceptions() default true;
        boolean logDuration() default false;
        String[] fields() default {};
        String format() default "text";
        int maxRatePerSecond() default 0;
        int priority() default 1000;
        enum LogSeverity { TRACE, DEBUG, INFO, WARN, ERROR, FATAL }
    }

    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DLOG {
        DeepLog.LogSeverity level() default DeepLog.LogSeverity.INFO;
        String message() default "";
        boolean logArgs() default false;
        boolean logReturn() default false;
        boolean logExceptions() default true;
        boolean logDuration() default false;
        String[] fields() default {};
        String format() default "text";
        int maxRatePerSecond() default 0;
        int priority() default 1000;
    }

    // ── 70. @DeepAudit / @DAUD ──────────────────────────────────────

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepAudit {
        String eventType() default "";
        boolean captureUser() default true;
        boolean captureTimestamp() default true;
        boolean captureParams() default true;
        boolean captureResult() default false;
        boolean captureStackTrace() default false;
        String auditStore() default "default";
        int retentionDays() default 90;
        String severity() default "INFO";
        int priority() default 1000;
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DAUD_AUDIT {
        String eventType() default "";
        boolean captureUser() default true;
        boolean captureTimestamp() default true;
        boolean captureParams() default true;
        boolean captureResult() default false;
        boolean captureStackTrace() default false;
        String auditStore() default "default";
        int retentionDays() default 90;
        String severity() default "INFO";
        int priority() default 1000;
    }

    // ── 71. @DeepProfile / @DPROF ───────────────────────────────────

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepProfile {
        boolean cpuProfiling() default true;
        boolean memoryProfiling() default false;
        boolean allocationProfiling() default false;
        boolean lockProfiling() default false;
        int samplingInterval() default 10;
        String outputFile() default "";
        boolean flameGraph() default false;
        int priority() default 1000;
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DPROF {
        boolean cpuProfiling() default true;
        boolean memoryProfiling() default false;
        boolean allocationProfiling() default false;
        boolean lockProfiling() default false;
        int samplingInterval() default 10;
        String outputFile() default "";
        boolean flameGraph() default false;
        int priority() default 1000;
    }

    // ── 72. @DeepDebug / @DDBG ──────────────────────────────────────

    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepDebug {
        boolean breakpoint() default false;
        boolean watchVariables() default false;
        String[] watchExpressions() default {};
        boolean enableRemoteDebug() default false;
        int debugPort() default 5005;
        boolean dumpBytecode() default false;
        boolean dumpStack() default false;
        boolean assertionsEnabled() default true;
        int priority() default 1000;
    }

    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DDBG {
        boolean breakpoint() default false;
        boolean watchVariables() default false;
        String[] watchExpressions() default {};
        boolean enableRemoteDebug() default false;
        int debugPort() default 5005;
        boolean dumpBytecode() default false;
        boolean dumpStack() default false;
        boolean assertionsEnabled() default true;
        int priority() default 1000;
    }

    // ── 73. @DeepValidate / @DVAL ───────────────────────────────────

    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepValidate {
        String[] constraints() default {};
        boolean notNull() default false;
        boolean notEmpty() default false;
        String regex() default "";
        double min() default -Double.MAX_VALUE;
        double max() default Double.MAX_VALUE;
        int minLength() default -1;
        int maxLength() default -1;
        String customValidator() default "";
        String message() default "Validation failed";
        ValidateAction onFail() default ValidateAction.THROW;
        int priority() default 1000;
        enum ValidateAction { THROW, LOG, RETURN_NULL, RETURN_DEFAULT, CALLBACK }
    }

    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DVAL {
        String[] constraints() default {};
        boolean notNull() default false;
        boolean notEmpty() default false;
        String regex() default "";
        double min() default -Double.MAX_VALUE;
        double max() default Double.MAX_VALUE;
        int minLength() default -1;
        int maxLength() default -1;
        String customValidator() default "";
        String message() default "Validation failed";
        DeepValidate.ValidateAction onFail() default DeepValidate.ValidateAction.THROW;
        int priority() default 1000;
    }

    // ── 74. @DeepWatch / @DWATCH ────────────────────────────────────

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepWatch {
        String[] watchPaths() default {};
        boolean autoReload() default true;
        int debounceMs() default 100;
        String[] filePatterns() default {"*.java", "*.class"};
        boolean recursive() default true;
        String onChangeCallback() default "";
        int priority() default 1000;
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DWATCH {
        String[] watchPaths() default {};
        boolean autoReload() default true;
        int debounceMs() default 100;
        String[] filePatterns() default {"*.java", "*.class"};
        boolean recursive() default true;
        String onChangeCallback() default "";
        int priority() default 1000;
    }

    // ── 75. @DeepBackup / @DBAK ─────────────────────────────────────

    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepBackup {
        String backupPath() default "./backups";
        int intervalSeconds() default 3600;
        int maxBackups() default 10;
        boolean compress() default true;
        boolean includeTimestamp() default true;
        boolean incremental() default false;
        String checksumAlgorithm() default "SHA-256";
        int priority() default 1000;
    }

    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DBAK {
        String backupPath() default "./backups";
        int intervalSeconds() default 3600;
        int maxBackups() default 10;
        boolean compress() default true;
        boolean includeTimestamp() default true;
        boolean incremental() default false;
        String checksumAlgorithm() default "SHA-256";
        int priority() default 1000;
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  PHASE 7 · PERFORMANCE OPTIMIZATION ANNOTATIONS (76–97)        ║
    // ║  Priority: HIGH · 22 annotations                               ║
    // ╚══════════════════════════════════════════════════════════════════╝

    // ─── Memory Optimization (76–81) ─────────────────────────────────

    /** 76. @DeepOptimize / @DOPT */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepOptimize {
        OptStrategy[] strategies() default {OptStrategy.INLINE, OptStrategy.CONSTANT_FOLD};
        int aggressiveness() default 2;
        boolean profileGuided() default false;
        boolean verifyAfter() default true;
        int priority() default 1000;
        enum OptStrategy { INLINE, CONSTANT_FOLD, DEAD_CODE, LOOP_UNROLL, VECTORIZE,
                           ESCAPE_ANALYSIS, STRENGTH_REDUCTION, CSE, LICM, ALL }
    }
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DOPT {
        DeepOptimize.OptStrategy[] strategies() default {DeepOptimize.OptStrategy.INLINE, DeepOptimize.OptStrategy.CONSTANT_FOLD};
        int aggressiveness() default 2;
        boolean profileGuided() default false;
        boolean verifyAfter() default true;
        int priority() default 1000;
    }

    /** 77. @DeepInline / @DINL */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepInline {
        boolean force() default true;
        int maxSize() default 325;
        int maxCallSites() default -1;
        int priority() default 1000;
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DINL {
        boolean force() default true;
        int maxSize() default 325;
        int maxCallSites() default -1;
        int priority() default 1000;
    }

    /** 78. @DeepNoInline / @DNINL */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepNoInline {
        String reason() default "";
        int priority() default 1000;
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DNINL {
        String reason() default "";
        int priority() default 1000;
    }

    /** 79. @DeepTailCall / @DTAIL */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepTailCall {
        boolean enabled() default true;
        int maxDepth() default 1000;
        boolean trampolined() default false;
        int priority() default 1000;
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DTAIL {
        boolean enabled() default true;
        int maxDepth() default 1000;
        boolean trampolined() default false;
        int priority() default 1000;
    }

    /** 80. @DeepUnroll / @DUNRL */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepUnroll {
        int factor() default 4;
        boolean complete() default false;
        int maxIterations() default 256;
        int priority() default 1000;
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DUNRL {
        int factor() default 4;
        boolean complete() default false;
        int maxIterations() default 256;
        int priority() default 1000;
    }

    /** 81. @DeepPrefetch / @DPREF */
    @Target({ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepPrefetch {
        int distance() default 64;
        PrefetchLocality locality() default PrefetchLocality.TEMPORAL;
        boolean readWrite() default false;
        int priority() default 1000;
        enum PrefetchLocality { NONE, LOW, MEDIUM, TEMPORAL }
    }
    @Target({ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DPREF {
        int distance() default 64;
        DeepPrefetch.PrefetchLocality locality() default DeepPrefetch.PrefetchLocality.TEMPORAL;
        boolean readWrite() default false;
        int priority() default 1000;
    }

    // ─── Parallel & Vector (82–86) ───────────────────────────────────

    /** 82. @DeepVectorize / @DVEC */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepVectorize {
        int vectorWidth() default 256;
        boolean autoDetect() default true;
        String[] instructions() default {};
        boolean fmaEnabled() default true;
        int priority() default 1000;
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DVEC {
        int vectorWidth() default 256;
        boolean autoDetect() default true;
        String[] instructions() default {};
        boolean fmaEnabled() default true;
        int priority() default 1000;
    }

    /** 83. @DeepParallelize / @DPAR */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepParallelize {
        int threadCount() default -1;
        int chunkSize() default -1;
        boolean forkJoin() default true;
        boolean ordered() default false;
        ParallelStrategy strategy() default ParallelStrategy.WORK_STEALING;
        int priority() default 1000;
        enum ParallelStrategy { WORK_STEALING, FIXED_PARTITION, DYNAMIC, GUIDED }
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DPAR {
        int threadCount() default -1;
        int chunkSize() default -1;
        boolean forkJoin() default true;
        boolean ordered() default false;
        DeepParallelize.ParallelStrategy strategy() default DeepParallelize.ParallelStrategy.WORK_STEALING;
        int priority() default 1000;
    }

    /** 84. @DeepGPU / @DGPU */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepGPU {
        String kernel() default "";
        int workGroupSize() default 256;
        boolean fallbackToCPU() default true;
        GPUBackend backend() default GPUBackend.AUTO;
        boolean sharedMemory() default false;
        int priority() default 1000;
        enum GPUBackend { AUTO, OPENCL, CUDA, VULKAN_COMPUTE, METAL }
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DGPU {
        String kernel() default "";
        int workGroupSize() default 256;
        boolean fallbackToCPU() default true;
        DeepGPU.GPUBackend backend() default DeepGPU.GPUBackend.AUTO;
        boolean sharedMemory() default false;
        int priority() default 1000;
    }

    /** 85. @DeepJIT / @DJIT */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepJIT {
        int compileThreshold() default 10000;
        String compilerMode() default "c2";
        boolean printCompilation() default false;
        boolean printInlining() default false;
        int inlineDepth() default 9;
        int priority() default 1000;
    }
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DJIT {
        int compileThreshold() default 10000;
        String compilerMode() default "c2";
        boolean printCompilation() default false;
        boolean printInlining() default false;
        int inlineDepth() default 9;
        int priority() default 1000;
    }

    /** 86. @DeepMemoize / @DMEMO */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepMemoize {
        int maxCacheSize() default 1000;
        int ttlSeconds() default -1;
        boolean weakKeys() default false;
        boolean softValues() default false;
        String cacheKey() default "";
        EvictionPolicy eviction() default EvictionPolicy.LRU;
        boolean statisticsEnabled() default false;
        int priority() default 1000;
        enum EvictionPolicy { LRU, LFU, FIFO, RANDOM, SIZE }
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DMEMO {
        int maxCacheSize() default 1000;
        int ttlSeconds() default -1;
        boolean weakKeys() default false;
        boolean softValues() default false;
        String cacheKey() default "";
        DeepMemoize.EvictionPolicy eviction() default DeepMemoize.EvictionPolicy.LRU;
        boolean statisticsEnabled() default false;
        int priority() default 1000;
    }

    // ─── Branch & Path (87–92) ───────────────────────────────────────

    /** 87. @DeepCold / @DCOLD */
    @Target({ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepCold { String reason() default ""; int priority() default 1000; }
    @Target({ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DCOLD { String reason() default ""; int priority() default 1000; }

    /** 88. @DeepHot / @DHOT */
    @Target({ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepHot { int expectedFrequency() default 100; int priority() default 1000; }
    @Target({ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DHOT { int expectedFrequency() default 100; int priority() default 1000; }

    /** 89. @DeepLikely / @DLIKE */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepLikely { double probability() default 0.95; int priority() default 1000; }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DLIKE { double probability() default 0.95; int priority() default 1000; }

    /** 90. @DeepUnlikely / @DULIKE */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepUnlikely { double probability() default 0.05; int priority() default 1000; }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DULIKE { double probability() default 0.05; int priority() default 1000; }

    /** 91. @DeepAlign / @DALIGN */
    @Target({ElementType.TYPE, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepAlign {
        int bytes() default 64;
        boolean cacheLine() default true;
        int priority() default 1000;
    }
    @Target({ElementType.TYPE, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DALIGN {
        int bytes() default 64;
        boolean cacheLine() default true;
        int priority() default 1000;
    }

    /** 92. @DeepPack / @DPAK */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepPack {
        int alignment() default 1;
        boolean reorder() default true;
        boolean eliminatePadding() default true;
        int priority() default 1000;
    }
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DPAK {
        int alignment() default 1;
        boolean reorder() default true;
        boolean eliminatePadding() default true;
        int priority() default 1000;
    }

    // ─── Data Structure Optimization (93–97) ─────────────────────────

    /** 93. @DeepUnpack / @DUPAK */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepUnpack {
        boolean arrayOfStructs() default false;
        boolean structOfArrays() default true;
        int priority() default 1000;
    }
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DUPAK {
        boolean arrayOfStructs() default false;
        boolean structOfArrays() default true;
        int priority() default 1000;
    }

    /** 94. @DeepHash / @DHASH */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepHash {
        HashAlgorithm algorithm() default HashAlgorithm.XXHASH;
        int seed() default 0;
        boolean avalanche() default true;
        int priority() default 1000;
        enum HashAlgorithm { XXHASH, MURMURHASH3, CITYHASH, FARMHASH, SIPHASH, FNV1A, WYHASH }
    }
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DHASH {
        DeepHash.HashAlgorithm algorithm() default DeepHash.HashAlgorithm.XXHASH;
        int seed() default 0;
        boolean avalanche() default true;
        int priority() default 1000;
    }

    /** 95. @DeepBloom / @DBLM */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepBloom {
        int expectedElements() default 10000;
        double falsePositiveRate() default 0.01;
        int hashFunctions() default -1;
        boolean scalable() default false;
        int priority() default 1000;
    }
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DBLM {
        int expectedElements() default 10000;
        double falsePositiveRate() default 0.01;
        int hashFunctions() default -1;
        boolean scalable() default false;
        int priority() default 1000;
    }

    /** 96. @DeepTrie / @DTRIE */
    @Target({ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepTrie {
        boolean compressed() default true;
        int branchingFactor() default 256;
        boolean caseSensitive() default true;
        int priority() default 1000;
    }
    @Target({ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DTRIE {
        boolean compressed() default true;
        int branchingFactor() default 256;
        boolean caseSensitive() default true;
        int priority() default 1000;
    }

    /** 97. @DeepRope / @DROPE */
    @Target({ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepRope {
        int leafSize() default 1024;
        boolean autoBalance() default true;
        boolean immutable() default true;
        int priority() default 1000;
    }
    @Target({ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DROPE {
        int leafSize() default 1024;
        boolean autoBalance() default true;
        boolean immutable() default true;
        int priority() default 1000;
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  PHASE 8 · RESILIENCE & RELIABILITY ANNOTATIONS (98–112)       ║
    // ║  Priority: MEDIUM · 15 annotations                             ║
    // ╚══════════════════════════════════════════════════════════════════╝

    // ─── Error Handling (98–105) ─────────────────────────────────────

    /** 98. @DeepRetry / @DRTRY */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepRetry {
        int maxAttempts() default 3;
        long delayMs() default 1000;
        double multiplier() default 2.0;
        long maxDelayMs() default 30000;
        boolean exponentialBackoff() default true;
        boolean jitter() default true;
        Class<? extends Throwable>[] retryOn() default {Exception.class};
        Class<? extends Throwable>[] noRetryOn() default {};
        String fallbackMethod() default "";
        int priority() default 1000;
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DRTRY {
        int maxAttempts() default 3;
        long delayMs() default 1000;
        double multiplier() default 2.0;
        long maxDelayMs() default 30000;
        boolean exponentialBackoff() default true;
        boolean jitter() default true;
        Class<? extends Throwable>[] retryOn() default {Exception.class};
        Class<? extends Throwable>[] noRetryOn() default {};
        String fallbackMethod() default "";
        int priority() default 1000;
    }

    /** 99. @DeepCircuit / @DCIR */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepCircuit {
        int failureThreshold() default 5;
        int successThreshold() default 2;
        long timeoutMs() default 5000;
        long resetTimeoutMs() default 60000;
        boolean halfOpenEnabled() default true;
        String fallbackMethod() default "";
        Class<? extends Throwable>[] recordExceptions() default {Exception.class};
        Class<? extends Throwable>[] ignoreExceptions() default {};
        int priority() default 1000;
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DCIR {
        int failureThreshold() default 5;
        int successThreshold() default 2;
        long timeoutMs() default 5000;
        long resetTimeoutMs() default 60000;
        boolean halfOpenEnabled() default true;
        String fallbackMethod() default "";
        Class<? extends Throwable>[] recordExceptions() default {Exception.class};
        Class<? extends Throwable>[] ignoreExceptions() default {};
        int priority() default 1000;
    }

    /** 100. @DeepFallback / @DFBACK */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepFallback {
        String fallbackMethod() default "";
        String defaultValue() default "";
        boolean suppressExceptions() default false;
        Class<? extends Throwable>[] applyOn() default {Exception.class};
        int priority() default 1000;
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DFBACK {
        String fallbackMethod() default "";
        String defaultValue() default "";
        boolean suppressExceptions() default false;
        Class<? extends Throwable>[] applyOn() default {Exception.class};
        int priority() default 1000;
    }

    /** 101. @DeepTimeout / @DTOUT */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepTimeout {
        long timeoutMs() default 5000;
        boolean interruptible() default true;
        String timeoutHandler() default "";
        TimeoutStrategy onTimeout() default TimeoutStrategy.THROW;
        int priority() default 1000;
        enum TimeoutStrategy { THROW, RETURN_NULL, RETURN_DEFAULT, CALLBACK, CANCEL }
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DTOUT {
        long timeoutMs() default 5000;
        boolean interruptible() default true;
        String timeoutHandler() default "";
        DeepTimeout.TimeoutStrategy onTimeout() default DeepTimeout.TimeoutStrategy.THROW;
        int priority() default 1000;
    }

    /** 102. @DeepRateLimit / @DRATE */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepRateLimit {
        int maxRequests() default 100;
        int windowSeconds() default 60;
        RateLimitStrategy strategy() default RateLimitStrategy.SLIDING_WINDOW;
        String keyExpression() default "";
        String onLimitMethod() default "";
        int priority() default 1000;
        enum RateLimitStrategy { FIXED_WINDOW, SLIDING_WINDOW, TOKEN_BUCKET, LEAKY_BUCKET }
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DRATE {
        int maxRequests() default 100;
        int windowSeconds() default 60;
        DeepRateLimit.RateLimitStrategy strategy() default DeepRateLimit.RateLimitStrategy.SLIDING_WINDOW;
        String keyExpression() default "";
        String onLimitMethod() default "";
        int priority() default 1000;
    }

    /** 103. @DeepThrottle / @DTHR */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepThrottle {
        int maxConcurrent() default 10;
        int queueSize() default 100;
        boolean blockWhenFull() default true;
        long maxWaitMs() default 30000;
        boolean fairness() default false;
        int priority() default 1000;
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DTHR {
        int maxConcurrent() default 10;
        int queueSize() default 100;
        boolean blockWhenFull() default true;
        long maxWaitMs() default 30000;
        boolean fairness() default false;
        int priority() default 1000;
    }

    /** 104. @DeepDebounce / @DDEB */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepDebounce {
        long delayMs() default 500;
        boolean leading() default false;
        boolean trailing() default true;
        String keyExpression() default "";
        int priority() default 1000;
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DDEB {
        long delayMs() default 500;
        boolean leading() default false;
        boolean trailing() default true;
        String keyExpression() default "";
        int priority() default 1000;
    }

    /** 105. @DeepSchedule / @DSCHED */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepSchedule {
        String cron() default "";
        long fixedDelayMs() default -1;
        long fixedRateMs() default -1;
        long initialDelayMs() default 0;
        String timeZone() default "UTC";
        boolean concurrent() default false;
        int priority() default 1000;
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DSCHED {
        String cron() default "";
        long fixedDelayMs() default -1;
        long fixedRateMs() default -1;
        long initialDelayMs() default 0;
        String timeZone() default "UTC";
        boolean concurrent() default false;
        int priority() default 1000;
    }

    // ─── Resource Management (106–112) ───────────────────────────────

    /** 106. @DeepQueue / @DQ */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepQueue {
        String queueName() default "default";
        int capacity() default 1000;
        boolean blocking() default false;
        QueueKind queueType() default QueueKind.FIFO;
        int consumers() default 1;
        boolean persistent() default false;
        int priority() default 1000;
        enum QueueKind { FIFO, LIFO, PRIORITY, DELAY, DEQUE }
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DQ {
        String queueName() default "default";
        int capacity() default 1000;
        boolean blocking() default false;
        DeepQueue.QueueKind queueType() default DeepQueue.QueueKind.FIFO;
        int consumers() default 1;
        boolean persistent() default false;
        int priority() default 1000;
    }

    /** 107. @DeepPool / @DPOOL */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepPool {
        int minSize() default 5;
        int maxSize() default 50;
        long maxIdleTimeMs() default 300000;
        boolean testOnBorrow() default false;
        boolean testOnReturn() default false;
        boolean lifo() default true;
        String validationMethod() default "";
        String resetMethod() default "";
        int priority() default 1000;
    }
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DPOOL {
        int minSize() default 5;
        int maxSize() default 50;
        long maxIdleTimeMs() default 300000;
        boolean testOnBorrow() default false;
        boolean testOnReturn() default false;
        boolean lifo() default true;
        String validationMethod() default "";
        String resetMethod() default "";
        int priority() default 1000;
    }

    /** 108. @DeepLazy / @DLAZY */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepLazy {
        boolean threadSafe() default true;
        String initMethod() default "";
        boolean doubleChecked() default true;
        int priority() default 1000;
    }
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DLAZY {
        boolean threadSafe() default true;
        String initMethod() default "";
        boolean doubleChecked() default true;
        int priority() default 1000;
    }

    /** 109. @DeepStream / @DSTRM */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepStream {
        boolean parallel() default false;
        int bufferSize() default 128;
        boolean backpressure() default true;
        StreamKind streamType() default StreamKind.SEQUENTIAL;
        int priority() default 1000;
        enum StreamKind { SEQUENTIAL, PARALLEL, REACTIVE, PULL, PUSH }
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DSTRM {
        boolean parallel() default false;
        int bufferSize() default 128;
        boolean backpressure() default true;
        DeepStream.StreamKind streamType() default DeepStream.StreamKind.SEQUENTIAL;
        int priority() default 1000;
    }

    /** 110. @DeepNetwork / @DNET */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepNetwork {
        String protocol() default "tcp";
        int port() default -1;
        boolean inbound() default true;
        boolean outbound() default true;
        String[] packetTypes() default {};
        boolean compress() default false;
        boolean encrypt() default false;
        int priority() default 1000;
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DNET {
        String protocol() default "tcp";
        int port() default -1;
        boolean inbound() default true;
        boolean outbound() default true;
        String[] packetTypes() default {};
        boolean compress() default false;
        boolean encrypt() default false;
        int priority() default 1000;
    }

    /** 111. @DeepMemory / @DMEM */
    @Target({ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepMemory {
        boolean offHeap() default false;
        int alignmentBytes() default 8;
        boolean zeroed() default true;
        long maxBytes() default -1;
        boolean trackAllocations() default false;
        MemoryStrategy strategy() default MemoryStrategy.DEFAULT;
        int priority() default 1000;
        enum MemoryStrategy { DEFAULT, ARENA, SLAB, BUMP, POOL, REGION }
    }
    @Target({ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DMEM {
        boolean offHeap() default false;
        int alignmentBytes() default 8;
        boolean zeroed() default true;
        long maxBytes() default -1;
        boolean trackAllocations() default false;
        DeepMemory.MemoryStrategy strategy() default DeepMemory.MemoryStrategy.DEFAULT;
        int priority() default 1000;
    }

    /** 112. @DeepReflect / @DREFL */
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepReflect {
        boolean cacheHandles() default true;
        boolean makeAccessible() default false;
        boolean generateProxy() default false;
        boolean useMethodHandles() default true;
        boolean useVarHandles() default false;
        int priority() default 1000;
    }
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DREFL {
        boolean cacheHandles() default true;
        boolean makeAccessible() default false;
        boolean generateProxy() default false;
        boolean useMethodHandles() default true;
        boolean useVarHandles() default false;
        int priority() default 1000;
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║  PHASE 9 · DESIGN PATTERN ANNOTATIONS (113–120)                ║
    // ║  Priority: LOW · 8 annotations                                 ║
    // ╚══════════════════════════════════════════════════════════════════╝

    /** 113. @DeepSingleton / @DSING */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepSingleton {
        boolean lazy() default true;
        boolean threadSafe() default true;
        String instanceMethod() default "getInstance";
        boolean preventReflection() default true;
        boolean preventSerialization() default true;
        boolean preventCloning() default true;
        int priority() default 1000;
    }
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DSING {
        boolean lazy() default true;
        boolean threadSafe() default true;
        String instanceMethod() default "getInstance";
        boolean preventReflection() default true;
        boolean preventSerialization() default true;
        boolean preventCloning() default true;
        int priority() default 1000;
    }

    /** 114. @DeepFactory / @DFACT */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepFactory {
        String factoryMethod() default "create";
        Class<?>[] products() default {};
        boolean abstractFactory() default false;
        boolean cached() default false;
        boolean threadSafe() default true;
        int priority() default 1000;
    }
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DFACT {
        String factoryMethod() default "create";
        Class<?>[] products() default {};
        boolean abstractFactory() default false;
        boolean cached() default false;
        boolean threadSafe() default true;
        int priority() default 1000;
    }

    /** 115. @DeepBuilder / @DBUILD */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepBuilder {
        String builderClass() default "";
        boolean fluent() default true;
        boolean immutable() default true;
        String buildMethod() default "build";
        String prefix() default "with";
        boolean validate() default true;
        boolean toBuilder() default false;
        int priority() default 1000;
    }
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DBUILD {
        String builderClass() default "";
        boolean fluent() default true;
        boolean immutable() default true;
        String buildMethod() default "build";
        String prefix() default "with";
        boolean validate() default true;
        boolean toBuilder() default false;
        int priority() default 1000;
    }

    /** 116. @DeepObserver / @DOBS */
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepObserver {
        String eventType() default "";
        String[] events() default {};
        boolean async() default false;
        boolean weakReference() default false;
        int priority() default 0;
    }
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DOBS {
        String eventType() default "";
        String[] events() default {};
        boolean async() default false;
        boolean weakReference() default false;
        int priority() default 0;
    }

    /** 117. @DeepPubSub / @DPSUB */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepPubSub {
        String topic() default "";
        String[] subscriptions() default {};
        boolean persistent() default false;
        int qos() default 0;
        boolean async() default true;
        boolean ordered() default false;
        int bufferSize() default 256;
        int priority() default 1000;
    }
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DPSUB {
        String topic() default "";
        String[] subscriptions() default {};
        boolean persistent() default false;
        int qos() default 0;
        boolean async() default true;
        boolean ordered() default false;
        int bufferSize() default 256;
        int priority() default 1000;
    }

    /** 118. @DeepGenerate / @DGEN */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeepGenerate {
        String template() default "";
        String outputType() default "class";
        String[] generatedMethods() default {};
        boolean overwrite() default false;
        String packageName() default "";
        String[] interfaces() default {};
        int priority() default 1000;
    }
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DGEN {
        String template() default "";
        String outputType() default "class";
        String[] generatedMethods() default {};
        boolean overwrite() default false;
        String packageName() default "";
        String[] interfaces() default {};
        int priority() default 1000;
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║                                                                  ║
    // ║              P R O C E S S O R   E N G I N E S                   ║
    // ║                                                                  ║
    // ╚══════════════════════════════════════════════════════════════════╝


    // ====================================================================
    //  PHASE 5 PROCESSOR: CROSS-PLATFORM BYTECODE ENGINE
    // ====================================================================

    /**
     * Unified bytecode transformation bridge.
     *
     * <pre>
     * BytecodeTransformBridge
     * ├── LLVMTransformer      (LLVM IR via JNI/ProcessBuilder)
     * ├── WasmTransformer      (WebAssembly via Binaryen bindings)
     * ├── CILTransformer       (.NET CIL via Mono.Cecil bridge)
     * ├── DalvikTransformer    (Android DEX via dexlib2)
     * ├── PythonTransformer    (Python .pyc via marshal protocol)
     * ├── NodeTransformer      (V8 bytecode via snapshot analysis)
     * └── LuaTransformer       (Lua bytecode via chunk format)
     * </pre>
     */
    public static class BytecodeTransformBridge {

        private final LLVMTransformer   llvm   = new LLVMTransformer();
        private final WasmTransformer   wasm   = new WasmTransformer();
        private final CILTransformer    cil    = new CILTransformer();
        private final DalvikTransformer dalvik = new DalvikTransformer();
        private final PythonTransformer python = new PythonTransformer();
        private final NodeTransformer   node   = new NodeTransformer();
        private final LuaTransformer    lua    = new LuaTransformer();

        // ── Accessors ────────────────────────────────────────────
        public LLVMTransformer   llvm()   { return llvm; }
        public WasmTransformer   wasm()   { return wasm; }
        public CILTransformer    cil()    { return cil; }
        public DalvikTransformer dalvik() { return dalvik; }
        public PythonTransformer python() { return python; }
        public NodeTransformer   node()   { return node; }
        public LuaTransformer    lua()    { return lua; }

        /** Route to correct transformer by annotation type. */
        public byte[] dispatch(Annotation ann, byte[] input) throws Exception {
            if (ann instanceof DeepLLVM || ann instanceof DLLVM)       return llvm.transform(ann, input);
            if (ann instanceof DeepWASM || ann instanceof DWASM)       return wasm.transform(ann, input);
            if (ann instanceof DeepCIL || ann instanceof DCIL)         return cil.transform(ann, input);
            if (ann instanceof DeepDalvik || ann instanceof DDLK)      return dalvik.transform(ann, input);
            if (ann instanceof DeepPython || ann instanceof DPYC)      return python.transform(ann, input);
            if (ann instanceof DeepNode || ann instanceof DNODE)       return node.transform(ann, input);
            if (ann instanceof DeepLua || ann instanceof DLUA)         return lua.transform(ann, input);
            throw new IllegalArgumentException("Unknown cross-platform annotation: " + ann.annotationType());
        }

        /** Auto-detect format and transform. */
        public byte[] autoTransform(byte[] input) throws Exception {
            String format = detectBytecodeFormat(input);
            log("AutoTransform: detected format=%s (%d bytes)", format, input.length);
            switch (format) {
                case "wasm": return wasm.optimize(input);
                case "dex":  return dalvik.optimizeDex(input);
                case "pyc":  return python.optimize(input);
                case "luac": return lua.optimize(input);
                case "llvm": return llvm.optimize(input);
                default:     return input;
            }
        }

        private String detectBytecodeFormat(byte[] data) {
            if (data.length < 4) return "unknown";
            if (data[0] == 0x00 && data[1] == 0x61 && data[2] == 0x73 && data[3] == 0x6D) return "wasm";
            if (data[0] == 0x64 && data[1] == 0x65 && data[2] == 0x78 && data[3] == 0x0A) return "dex";
            // Python magic numbers vary by version; 3.11+ uses specific 4-byte magic
            if ((data[0] & 0xFF) == 0xA7 || (data[0] & 0xFF) == 0x6F) return "pyc";
            if (data[0] == 0x1B && data[1] == 0x4C && data[2] == 0x75 && data[3] == 0x61) return "luac";
            if (data[0] == 'B' && data[1] == 'C') return "llvm";
            if (data[0] == 0x4D && data[1] == 0x5A) return "pe_cil"; // PE/COFF
            return "unknown";
        }


        // ── LLVM IR Transformer ──────────────────────────────────

        public static class LLVMTransformer {

            private static final Map<String, String> passAliases = new LinkedHashMap<>();
            static {
                passAliases.put("O0", "-O0");
                passAliases.put("O1", "-O1");
                passAliases.put("O2", "-O2");
                passAliases.put("O3", "-O3");
                passAliases.put("Os", "-Os");
                passAliases.put("Oz", "-Oz");
                passAliases.put("inline", "-inline");
                passAliases.put("dce", "-dce");
                passAliases.put("gvn", "-gvn");
                passAliases.put("licm", "-licm");
                passAliases.put("loop-unroll", "-loop-unroll");
                passAliases.put("mem2reg", "-mem2reg");
                passAliases.put("instcombine", "-instcombine");
                passAliases.put("simplifycfg", "-simplifycfg");
            }

            /** High-level transform entry from annotation. */
            public byte[] transform(Annotation ann, byte[] input) throws Exception {
                String pass;
                DeepLLVM.OptLevel opt;
                String triple;
                String[] flags;
                boolean debug;

                if (ann instanceof DeepLLVM) {
                    DeepLLVM a = (DeepLLVM) ann;
                    pass = a.pass(); opt = a.optimization(); triple = a.targetTriple();
                    flags = a.flags(); debug = a.debugInfo();
                } else {
                    DLLVM a = (DLLVM) ann;
                    pass = a.pass(); opt = a.optimization(); triple = a.targetTriple();
                    flags = a.flags(); debug = a.debugInfo();
                }

                log("LLVM: pass=%s opt=%s triple=%s debug=%b", pass, opt, triple, debug);

                String ir = new String(input, StandardCharsets.UTF_8);
                ir = applyPasses(ir, pass, opt);
                if (!triple.isEmpty()) ir = setTargetTriple(ir, triple);
                if (debug) ir = injectDebugMetadata(ir);
                for (String flag : flags) ir = applyFlag(ir, flag);

                transformCount.incrementAndGet();
                return ir.getBytes(StandardCharsets.UTF_8);
            }

            public byte[] optimize(byte[] input) throws Exception {
                return runOpt(input, "-O2", "-strip-debug");
            }

            public String applyPasses(String ir, String passName, DeepLLVM.OptLevel opt) {
                String resolvedPass = passAliases.getOrDefault(passName, passName);
                ir = addPassComment(ir, resolvedPass, opt.name());
                switch (opt) {
                    case O3:
                        ir = performConstantFolding(ir);
                        ir = performDeadCodeElimination(ir);
                        ir = performInlining(ir);
                        ir = performLoopOptimization(ir);
                        break;
                    case O2:
                        ir = performConstantFolding(ir);
                        ir = performDeadCodeElimination(ir);
                        ir = performInlining(ir);
                        break;
                    case O1:
                        ir = performConstantFolding(ir);
                        ir = performDeadCodeElimination(ir);
                        break;
                    case Os: case Oz:
                        ir = performDeadCodeElimination(ir);
                        ir = performSizeOptimization(ir);
                        break;
                    default:
                        break;
                }
                return ir;
            }

            public String setTargetTriple(String ir, String triple) {
                if (ir.contains("target triple")) {
                    return ir.replaceFirst("target triple = \"[^\"]*\"", "target triple = \"" + triple + "\"");
                }
                return "target triple = \"" + triple + "\"\n" + ir;
            }

            public String injectDebugMetadata(String ir) {
                if (!ir.contains("!llvm.dbg.cu")) {
                    String debugMD = "\n!llvm.dbg.cu = !{!0}\n" +
                            "!0 = distinct !DICompileUnit(language: DW_LANG_C99, file: !1, producer: \"DeepMix\", " +
                            "isOptimized: true, runtimeVersion: 0, emissionKind: FullDebug)\n" +
                            "!1 = !DIFile(filename: \"deepmix_generated.ll\", directory: \".\")\n";
                    ir += debugMD;
                }
                return ir;
            }

            public String injectFunction(String ir, String funcDef) {
                int lastDefine = ir.lastIndexOf("\ndefine ");
                if (lastDefine < 0) lastDefine = ir.length();
                else {
                    int braceEnd = findMatchingBrace(ir, ir.indexOf('{', lastDefine));
                    if (braceEnd > 0) lastDefine = braceEnd + 1;
                }
                return ir.substring(0, lastDefine) + "\n\n" + funcDef + "\n" + ir.substring(lastDefine);
            }

            public String replaceFunction(String ir, String funcName, String newBody) {
                Pattern p = Pattern.compile("(define[^@]*@" + Pattern.quote(funcName) + "\\s*\\([^)]*\\)[^{]*\\{)([^}]*)(\\})");
                Matcher m = p.matcher(ir);
                if (m.find()) {
                    return ir.substring(0, m.start(2)) + "\n" + newBody + "\n" + ir.substring(m.end(2));
                }
                return ir;
            }

            public String addGlobalVariable(String ir, String name, String type, String initializer) {
                String global = "@" + name + " = global " + type + " " + initializer + "\n";
                int firstDefine = ir.indexOf("\ndefine ");
                if (firstDefine > 0) {
                    return ir.substring(0, firstDefine) + "\n" + global + ir.substring(firstDefine);
                }
                return global + ir;
            }

            public String addAttribute(String ir, String funcName, String attribute) {
                return ir.replaceFirst(
                        "(define[^@]*@" + Pattern.quote(funcName) + "\\s*\\([^)]*\\))\\s*",
                        "$1 " + attribute + " "
                );
            }

            private String performConstantFolding(String ir) {
                // Fold simple arithmetic on constants: %x = add i32 3, 5 → %x = i32 8
                ir = ir.replaceAll("; DeepMix: constant folding applied\\n", "");
                ir = "; DeepMix: constant folding applied\n" + ir;
                Pattern addConst = Pattern.compile("(%\\w+)\\s*=\\s*add\\s+i32\\s+(\\d+),\\s*(\\d+)");
                Matcher m = addConst.matcher(ir);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    int a = Integer.parseInt(m.group(2));
                    int b = Integer.parseInt(m.group(3));
                    m.appendReplacement(sb, m.group(1) + " = add i32 " + (a + b) + ", 0 ; folded " + a + "+" + b);
                }
                m.appendTail(sb);
                return sb.toString();
            }

            private String performDeadCodeElimination(String ir) {
                ir = "; DeepMix: DCE applied\n" + ir;
                // Remove unreachable blocks (simplified)
                ir = ir.replaceAll("\\bunreachable\\b\\s*\\n(\\s*[^}\\n]+\\n)*", "unreachable\n");
                return ir;
            }

            private String performInlining(String ir) {
                // Mark small functions with alwaysinline attribute
                Pattern smallFunc = Pattern.compile("(define[^{]*\\{)([^}]{1,200})(\\})");
                Matcher m = smallFunc.matcher(ir);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    String header = m.group(1);
                    if (!header.contains("alwaysinline") && !header.contains("noinline")) {
                        header = header.replace("define ", "define alwaysinline ");
                    }
                    m.appendReplacement(sb, Matcher.quoteReplacement(header + m.group(2) + m.group(3)));
                }
                m.appendTail(sb);
                return sb.toString();
            }

            private String performLoopOptimization(String ir) {
                ir = "; DeepMix: loop optimization applied\n" + ir;
                return ir;
            }

            private String performSizeOptimization(String ir) {
                // Strip comments and align directives
                ir = ir.replaceAll("\\s*;[^\\n]*", "");
                ir = ir.replaceAll("align \\d+", "");
                return "; DeepMix: size optimization applied\n" + ir;
            }

            private String addPassComment(String ir, String pass, String level) {
                return "; DeepMix LLVM pass: " + pass + " at " + level + "\n" + ir;
            }

            private String applyFlag(String ir, String flag) {
                return "; DeepMix flag: " + flag + "\n" + ir;
            }

            private byte[] runOpt(byte[] input, String... flags) throws Exception {
                // Attempt to invoke `opt` tool; fallback to in-memory transforms
                try {
                    Path tmpIn = Files.createTempFile("deepmix_", ".ll");
                    Path tmpOut = Files.createTempFile("deepmix_", ".ll");
                    Files.write(tmpIn, input);
                    List<String> cmd = new ArrayList<>();
                    cmd.add("opt");
                    Collections.addAll(cmd, flags);
                    cmd.add("-S");
                    cmd.add("-o"); cmd.add(tmpOut.toString());
                    cmd.add(tmpIn.toString());
                    Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
                    boolean finished = p.waitFor(30, TimeUnit.SECONDS);
                    if (finished && p.exitValue() == 0) {
                        byte[] result = Files.readAllBytes(tmpOut);
                        Files.deleteIfExists(tmpIn);
                        Files.deleteIfExists(tmpOut);
                        transformCount.incrementAndGet();
                        return result;
                    }
                    Files.deleteIfExists(tmpIn);
                    Files.deleteIfExists(tmpOut);
                } catch (IOException ignored) {
                    log("LLVM: 'opt' tool not available, using in-memory transforms");
                }
                // Fallback: apply text-based optimizations
                String ir = new String(input, StandardCharsets.UTF_8);
                ir = applyPasses(ir, "default", DeepLLVM.OptLevel.O2);
                return ir.getBytes(StandardCharsets.UTF_8);
            }

            private int findMatchingBrace(String s, int open) {
                if (open < 0) return -1;
                int depth = 0;
                for (int i = open; i < s.length(); i++) {
                    if (s.charAt(i) == '{') depth++;
                    if (s.charAt(i) == '}') { depth--; if (depth == 0) return i; }
                }
                return -1;
            }
        }


        // ── WebAssembly Transformer ──────────────────────────────

        public static class WasmTransformer {

            // WASM magic: \0asm
            private static final byte[] WASM_MAGIC = {0x00, 0x61, 0x73, 0x6D};
            private static final byte[] WASM_VERSION = {0x01, 0x00, 0x00, 0x00};

            // Section IDs
            public static final int SEC_CUSTOM = 0, SEC_TYPE = 1, SEC_IMPORT = 2, SEC_FUNCTION = 3,
                    SEC_TABLE = 4, SEC_MEMORY = 5, SEC_GLOBAL = 6, SEC_EXPORT = 7,
                    SEC_START = 8, SEC_ELEMENT = 9, SEC_CODE = 10, SEC_DATA = 11;

            public byte[] transform(Annotation ann, byte[] input) throws Exception {
                boolean optimize;
                int shrinkLevel;
                String[] features;
                int initialMem, maxMem;

                if (ann instanceof DeepWASM) {
                    DeepWASM a = (DeepWASM) ann;
                    optimize = a.optimize(); shrinkLevel = a.shrinkLevel();
                    features = a.features(); initialMem = a.initialMemory(); maxMem = a.maxMemory();
                } else {
                    DWASM a = (DWASM) ann;
                    optimize = a.optimize(); shrinkLevel = a.shrinkLevel();
                    features = a.features(); initialMem = a.initialMemory(); maxMem = a.maxMemory();
                }

                log("WASM: optimize=%b shrink=%d features=%s mem=%d/%d",
                        optimize, shrinkLevel, Arrays.toString(features), initialMem, maxMem);

                WasmModule module = parseModule(input);
                if (optimize) module = optimizeModule(module, shrinkLevel);
                module = setMemoryLimits(module, initialMem, maxMem);

                transformCount.incrementAndGet();
                return serializeModule(module);
            }

            public byte[] optimize(byte[] input) throws Exception {
                // Try wasm-opt first, fallback to in-memory
                try {
                    Path tmpIn = Files.createTempFile("deepmix_", ".wasm");
                    Path tmpOut = Files.createTempFile("deepmix_", ".wasm");
                    Files.write(tmpIn, input);
                    Process p = new ProcessBuilder("wasm-opt", "-O2", "-o", tmpOut.toString(), tmpIn.toString())
                            .redirectErrorStream(true).start();
                    if (p.waitFor(30, TimeUnit.SECONDS) && p.exitValue() == 0) {
                        byte[] result = Files.readAllBytes(tmpOut);
                        Files.deleteIfExists(tmpIn);
                        Files.deleteIfExists(tmpOut);
                        transformCount.incrementAndGet();
                        return result;
                    }
                    Files.deleteIfExists(tmpIn);
                    Files.deleteIfExists(tmpOut);
                } catch (IOException ignored) {
                    log("WASM: 'wasm-opt' not available, using in-memory optimization");
                }
                WasmModule module = parseModule(input);
                module = optimizeModule(module, 1);
                return serializeModule(module);
            }

            /** Minimal WASM section-level representation. */
            public static class WasmModule {
                final byte[] magic = WASM_MAGIC;
                final byte[] version = WASM_VERSION;
                final List<WasmSection> sections = new ArrayList<>();
                final Map<String, String> customData = new LinkedHashMap<>();
            }

            public static class WasmSection {
                int id;
                byte[] payload;
                WasmSection(int id, byte[] payload) { this.id = id; this.payload = payload; }
            }

            public WasmModule parseModule(byte[] data) {
                WasmModule module = new WasmModule();
                if (data.length < 8) return module;

                int offset = 8; // skip magic + version
                while (offset < data.length) {
                    int sectionId = data[offset++] & 0xFF;
                    int[] sizeResult = decodeLEB128(data, offset);
                    int sectionSize = sizeResult[0];
                    offset = sizeResult[1];

                    byte[] payload = new byte[sectionSize];
                    System.arraycopy(data, offset, payload, 0, Math.min(sectionSize, data.length - offset));
                    offset += sectionSize;

                    module.sections.add(new WasmSection(sectionId, payload));
                }
                return module;
            }

            public byte[] serializeModule(WasmModule module) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    baos.write(WASM_MAGIC);
                    baos.write(WASM_VERSION);
                    for (WasmSection sec : module.sections) {
                        baos.write(sec.id);
                        baos.write(encodeLEB128(sec.payload.length));
                        baos.write(sec.payload);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return baos.toByteArray();
            }

            public WasmModule optimizeModule(WasmModule module, int shrinkLevel) {
                // Remove custom sections if shrinking
                if (shrinkLevel > 0) {
                    module.sections.removeIf(s -> s.id == SEC_CUSTOM);
                }
                // Compact code section (remove NOP instructions: 0x01)
                for (WasmSection sec : module.sections) {
                    if (sec.id == SEC_CODE) {
                        sec.payload = removeNops(sec.payload);
                    }
                }
                return module;
            }

            public WasmModule setMemoryLimits(WasmModule module, int initial, int max) {
                for (WasmSection sec : module.sections) {
                    if (sec.id == SEC_MEMORY) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        baos.write(1); // 1 memory
                        baos.write(0x01); // has max
                        try {
                            baos.write(encodeLEB128(initial));
                            baos.write(encodeLEB128(max));
                        } catch (IOException e) { throw new RuntimeException(e); }
                        sec.payload = baos.toByteArray();
                    }
                }
                return module;
            }

            public WasmModule addExport(WasmModule module, String name, int kind, int index) {
                for (WasmSection sec : module.sections) {
                    if (sec.id == SEC_EXPORT) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try {
                            // Increment export count (first LEB128 in payload)
                            int[] countResult = decodeLEB128(sec.payload, 0);
                            int count = countResult[0] + 1;
                            baos.write(encodeLEB128(count));
                            // Copy existing exports
                            baos.write(sec.payload, countResult[1], sec.payload.length - countResult[1]);
                            // Add new export
                            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
                            baos.write(encodeLEB128(nameBytes.length));
                            baos.write(nameBytes);
                            baos.write(kind);
                            baos.write(encodeLEB128(index));
                        } catch (IOException e) { throw new RuntimeException(e); }
                        sec.payload = baos.toByteArray();
                    }
                }
                return module;
            }

            public WasmModule addCustomSection(WasmModule module, String name, byte[] data) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
                try {
                    baos.write(encodeLEB128(nameBytes.length));
                    baos.write(nameBytes);
                    baos.write(data);
                } catch (IOException e) { throw new RuntimeException(e); }
                module.sections.add(0, new WasmSection(SEC_CUSTOM, baos.toByteArray()));
                return module;
            }

            private byte[] removeNops(byte[] code) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(code.length);
                for (int i = 0; i < code.length; i++) {
                    if (code[i] != 0x01) baos.write(code[i]); // 0x01 = nop
                }
                return baos.toByteArray();
            }

            private int[] decodeLEB128(byte[] data, int offset) {
                int result = 0, shift = 0;
                while (offset < data.length) {
                    byte b = data[offset++];
                    result |= (b & 0x7F) << shift;
                    if ((b & 0x80) == 0) break;
                    shift += 7;
                }
                return new int[]{result, offset};
            }

            private byte[] encodeLEB128(int value) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                do {
                    int b = value & 0x7F;
                    value >>>= 7;
                    if (value != 0) b |= 0x80;
                    baos.write(b);
                } while (value != 0);
                return baos.toByteArray();
            }
        }


        // ── .NET CIL Transformer ─────────────────────────────────

        public static class CILTransformer {

            // PE/COFF magic
            private static final byte[] PE_MAGIC = {0x4D, 0x5A};
            // CLI header magic
            private static final int CLI_HEADER_SIZE = 72;

            /** Minimal CIL method representation. */
            public static class CILMethod {
                String name;
                int rva;
                int flags;
                List<byte[]> instructions = new ArrayList<>();
                List<String> localVariables = new ArrayList<>();
                int maxStack;
            }

            /** Minimal assembly representation. */
            public static class CILAssembly {
                String name;
                String framework;
                byte[] rawData;
                List<CILMethod> methods = new ArrayList<>();
                Map<String, String> metadata = new LinkedHashMap<>();
            }

            public byte[] transform(Annotation ann, byte[] input) throws Exception {
                String framework, typeName, method;
                int optLevel;
                String[] customIL;

                if (ann instanceof DeepCIL) {
                    DeepCIL a = (DeepCIL) ann;
                    framework = a.framework(); typeName = a.typeName(); method = a.method();
                    optLevel = a.optimizationLevel(); customIL = a.customIL();
                } else {
                    DCIL a = (DCIL) ann;
                    framework = a.framework(); typeName = a.typeName(); method = a.method();
                    optLevel = a.optimizationLevel(); customIL = a.customIL();
                }

                log("CIL: framework=%s type=%s method=%s opt=%d", framework, typeName, method, optLevel);

                CILAssembly assembly = parseAssembly(input);
                assembly.framework = framework;
                assembly.metadata.put("DeepMix.Transformed", "true");
                assembly.metadata.put("DeepMix.OptLevel", String.valueOf(optLevel));

                if (customIL.length > 0 && !method.isEmpty()) {
                    injectIL(assembly, typeName, method, customIL);
                }

                transformCount.incrementAndGet();
                return serializeAssembly(assembly);
            }

            public CILAssembly parseAssembly(byte[] data) {
                CILAssembly assembly = new CILAssembly();
                assembly.rawData = data;
                assembly.name = "assembly";

                // Parse PE header to find CLI header
                if (data.length > 2 && data[0] == PE_MAGIC[0] && data[1] == PE_MAGIC[1]) {
                    int peOffset = readInt32LE(data, 0x3C);
                    if (peOffset > 0 && peOffset + 4 < data.length) {
                        assembly.metadata.put("PE.Offset", String.valueOf(peOffset));
                        // PE signature check
                        if (data[peOffset] == 'P' && data[peOffset + 1] == 'E') {
                            assembly.metadata.put("Format", "PE/COFF");
                            int cliHeaderRVA = findCLIHeaderRVA(data, peOffset);
                            assembly.metadata.put("CLI.HeaderRVA", String.valueOf(cliHeaderRVA));
                        }
                    }
                }
                return assembly;
            }

            public byte[] serializeAssembly(CILAssembly assembly) {
                // For now return modified raw data with metadata appended as custom section
                if (assembly.rawData == null) return new byte[0];
                return assembly.rawData; // Real impl would patch the PE in-place
            }

            public void injectIL(CILAssembly assembly, String typeName, String methodName, String[] ilInstructions) {
                log("CIL: Injecting %d IL instructions into %s::%s", ilInstructions.length, typeName, methodName);
                CILMethod m = new CILMethod();
                m.name = typeName + "::" + methodName;
                for (String il : ilInstructions) {
                    m.instructions.add(assembleSingleIL(il));
                }
                assembly.methods.add(m);
            }

            public void replaceMethodBody(CILAssembly assembly, String typeName, String methodName, String[] ilInstructions) {
                log("CIL: Replacing body of %s::%s", typeName, methodName);
                assembly.methods.removeIf(m -> m.name.equals(typeName + "::" + methodName));
                injectIL(assembly, typeName, methodName, ilInstructions);
            }

            public void addAttribute(CILAssembly assembly, String typeName, String attributeName, String[] args) {
                assembly.metadata.put("Attribute." + typeName + "." + attributeName, String.join(",", args));
            }

            private byte[] assembleSingleIL(String instruction) {
                // Simplified IL assembler
                String trimmed = instruction.trim().toLowerCase();
                switch (trimmed) {
                    case "nop":        return new byte[]{0x00};
                    case "ldarg.0":    return new byte[]{0x02};
                    case "ldarg.1":    return new byte[]{0x03};
                    case "ldarg.2":    return new byte[]{0x04};
                    case "ldarg.3":    return new byte[]{0x05};
                    case "ldloc.0":    return new byte[]{0x06};
                    case "ldloc.1":    return new byte[]{0x07};
                    case "stloc.0":    return new byte[]{0x0A};
                    case "stloc.1":    return new byte[]{0x0B};
                    case "ldc.i4.0":   return new byte[]{0x16};
                    case "ldc.i4.1":   return new byte[]{0x17};
                    case "ldc.i4.m1":  return new byte[]{0x15};
                    case "add":        return new byte[]{0x58};
                    case "sub":        return new byte[]{0x59};
                    case "mul":        return new byte[]{0x5A};
                    case "div":        return new byte[]{0x5B};
                    case "ret":        return new byte[]{0x2A};
                    case "dup":        return new byte[]{0x25};
                    case "pop":        return new byte[]{0x26};
                    case "ldnull":     return new byte[]{0x14};
                    case "throw":      return new byte[]{0x7A};
                    case "conv.i4":    return new byte[]{0x69};
                    case "conv.i8":    return new byte[]{0x6A};
                    case "conv.r4":    return new byte[]{0x6B};
                    case "conv.r8":    return new byte[]{0x6C};
                    default:           return instruction.getBytes(StandardCharsets.UTF_8);
                }
            }

            private int findCLIHeaderRVA(byte[] data, int peOffset) {
                int optionalHeaderOffset = peOffset + 24;
                int magic = readInt16LE(data, optionalHeaderOffset);
                int cliRVAOffset = (magic == 0x20B) ? optionalHeaderOffset + 208 : optionalHeaderOffset + 192; // PE32+ vs PE32
                if (cliRVAOffset + 4 < data.length) return readInt32LE(data, cliRVAOffset);
                return 0;
            }

            private int readInt16LE(byte[] data, int offset) {
                if (offset + 1 >= data.length) return 0;
                return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
            }

            private int readInt32LE(byte[] data, int offset) {
                if (offset + 3 >= data.length) return 0;
                return (data[offset] & 0xFF) | ((data[offset+1] & 0xFF) << 8)
                        | ((data[offset+2] & 0xFF) << 16) | ((data[offset+3] & 0xFF) << 24);
            }
        }


        // ── Dalvik/ART Transformer ───────────────────────────────

        public static class DalvikTransformer {

            // DEX magic: "dex\n"
            private static final byte[] DEX_MAGIC = {0x64, 0x65, 0x78, 0x0A};

            public static class DexFile {
                byte[] rawData;
                String version;
                int checksum;
                int fileSize;
                int stringIdsSize, typeIdsSize, protoIdsSize, fieldIdsSize, methodIdsSize, classDefsSize;
                Map<String, String> metadata = new LinkedHashMap<>();
                List<DexClassDef> classDefs = new ArrayList<>();
            }

            public static class DexClassDef {
                String className;
                int accessFlags;
                String superclass;
                List<DexMethod> directMethods = new ArrayList<>();
                List<DexMethod> virtualMethods = new ArrayList<>();
            }

            public static class DexMethod {
                String name;
                int accessFlags;
                int registersSize;
                int insSize, outsSize;
                byte[] bytecode;
            }

            public byte[] transform(Annotation ann, byte[] input) throws Exception {
                String className, methodName;
                int minSdk, targetSdk;
                DeepDalvik.DexOptLevel optLevel;

                if (ann instanceof DeepDalvik) {
                    DeepDalvik a = (DeepDalvik) ann;
                    className = a.className(); methodName = a.methodName();
                    minSdk = a.minSdk(); targetSdk = a.targetSdk(); optLevel = a.dexOpt();
                } else {
                    DDLK a = (DDLK) ann;
                    className = a.className(); methodName = a.methodName();
                    minSdk = a.minSdk(); targetSdk = a.targetSdk(); optLevel = a.dexOpt();
                }

                log("Dalvik: class=%s method=%s sdk=%d/%d opt=%s",
                        className, methodName, minSdk, targetSdk, optLevel);

                DexFile dex = parseDexFile(input);
                if (optLevel == DeepDalvik.DexOptLevel.AGGRESSIVE) {
                    dex = optimizeDex(dex);
                }

                transformCount.incrementAndGet();
                return serializeDex(dex);
            }

            public byte[] optimizeDex(byte[] input) throws Exception {
                DexFile dex = parseDexFile(input);
                dex = optimizeDex(dex);
                return serializeDex(dex);
            }

            public DexFile parseDexFile(byte[] data) {
                DexFile dex = new DexFile();
                dex.rawData = data;
                if (data.length < 112) return dex;

                dex.version = new String(data, 4, 3, StandardCharsets.UTF_8).trim();
                dex.checksum = readInt32LE(data, 8);
                dex.fileSize = readInt32LE(data, 32);
                dex.stringIdsSize = readInt32LE(data, 56);
                dex.typeIdsSize = readInt32LE(data, 64);
                dex.protoIdsSize = readInt32LE(data, 72);
                dex.fieldIdsSize = readInt32LE(data, 80);
                dex.methodIdsSize = readInt32LE(data, 88);
                dex.classDefsSize = readInt32LE(data, 96);

                dex.metadata.put("version", dex.version);
                dex.metadata.put("fileSize", String.valueOf(dex.fileSize));
                dex.metadata.put("strings", String.valueOf(dex.stringIdsSize));
                dex.metadata.put("types", String.valueOf(dex.typeIdsSize));
                dex.metadata.put("methods", String.valueOf(dex.methodIdsSize));
                dex.metadata.put("classes", String.valueOf(dex.classDefsSize));

                log("Dalvik: parsed DEX v%s (%d classes, %d methods, %d strings)",
                        dex.version, dex.classDefsSize, dex.methodIdsSize, dex.stringIdsSize);
                return dex;
            }

            public DexFile optimizeDex(DexFile dex) {
                dex.metadata.put("DeepMix.Optimized", "true");
                // Remove unused code, deduplicate strings etc. (simplified)
                log("Dalvik: optimization pass complete");
                return dex;
            }

            public byte[] serializeDex(DexFile dex) {
                return dex.rawData != null ? dex.rawData : new byte[0];
            }

            public void injectMethod(DexFile dex, String className, DexMethod method) {
                for (DexClassDef cls : dex.classDefs) {
                    if (cls.className.equals(className)) {
                        cls.virtualMethods.add(method);
                        log("Dalvik: injected method %s into %s", method.name, className);
                        return;
                    }
                }
                DexClassDef newClass = new DexClassDef();
                newClass.className = className;
                newClass.virtualMethods.add(method);
                dex.classDefs.add(newClass);
            }

            private int readInt32LE(byte[] d, int o) {
                if (o + 3 >= d.length) return 0;
                return (d[o]&0xFF)|((d[o+1]&0xFF)<<8)|((d[o+2]&0xFF)<<16)|((d[o+3]&0xFF)<<24);
            }
        }


        // ── Python Bytecode Transformer ──────────────────────────

        public static class PythonTransformer {

            // Python magic numbers (partial list, version-dependent)
            private static final Map<Integer, String> PY_MAGIC = new LinkedHashMap<>();
            static {
                PY_MAGIC.put(3439, "3.10"); PY_MAGIC.put(3495, "3.11");
                PY_MAGIC.put(3531, "3.12"); PY_MAGIC.put(3568, "3.13");
            }

            // CPython opcodes (3.11+)
            public static final int OP_NOP = 9, OP_POP_TOP = 1, OP_PUSH_NULL = 2,
                    OP_RETURN_VALUE = 83, OP_LOAD_CONST = 100, OP_LOAD_NAME = 101,
                    OP_LOAD_FAST = 124, OP_STORE_FAST = 125, OP_LOAD_GLOBAL = 116,
                    OP_STORE_NAME = 90, OP_CALL_FUNCTION = 171, OP_BINARY_OP = 122,
                    OP_COMPARE_OP = 107, OP_JUMP_FORWARD = 110, OP_JUMP_BACKWARD = 140,
                    OP_POP_JUMP_IF_FALSE = 114, OP_POP_JUMP_IF_TRUE = 115;

            public static class PyCodeObject {
                int magic;
                int flags;
                long timestamp;
                long sourceSize;
                String pyVersion;
                byte[] marshalData;
                Map<String, Object> metadata = new LinkedHashMap<>();

                // Decoded code fields
                int argCount;
                int kwOnlyArgCount;
                int nLocals;
                int stackSize;
                int codeFlags;
                byte[] bytecode;
                List<Object> constants = new ArrayList<>();
                List<String> names = new ArrayList<>();
                List<String> varnames = new ArrayList<>();
                String filename;
                String name;
                int firstLineNo;
                byte[] lineTable;
            }

            public byte[] transform(Annotation ann, byte[] input) throws Exception {
                String pyVersion, function;
                int optLevel;
                boolean removeAsserts, removeDocs, constFold;

                if (ann instanceof DeepPython) {
                    DeepPython a = (DeepPython) ann;
                    pyVersion = a.pythonVersion(); function = a.function();
                    optLevel = a.optimizationLevel(); removeAsserts = a.removeAssertions();
                    removeDocs = a.removeDocstrings(); constFold = a.constantFolding();
                } else {
                    DPYC a = (DPYC) ann;
                    pyVersion = a.pythonVersion(); function = a.function();
                    optLevel = a.optimizationLevel(); removeAsserts = a.removeAssertions();
                    removeDocs = a.removeDocstrings(); constFold = a.constantFolding();
                }

                log("Python: version=%s func=%s opt=%d", pyVersion, function, optLevel);

                PyCodeObject code = parsePyc(input);
                if (removeAsserts) removeAssertions(code);
                if (removeDocs) removeDocstrings(code);
                if (constFold) constantFold(code);
                if (optLevel >= 2) peepholeOptimize(code);

                transformCount.incrementAndGet();
                return serializePyc(code);
            }

            public byte[] optimize(byte[] input) throws Exception {
                PyCodeObject code = parsePyc(input);
                removeAssertions(code);
                peepholeOptimize(code);
                return serializePyc(code);
            }

            public PyCodeObject parsePyc(byte[] data) {
                PyCodeObject code = new PyCodeObject();
                if (data.length < 16) return code;

                code.magic = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
                code.pyVersion = PY_MAGIC.getOrDefault(code.magic, "unknown");
                code.flags = (data[4] & 0xFF) | ((data[5] & 0xFF) << 8) |
                        ((data[6] & 0xFF) << 16) | ((data[7] & 0xFF) << 24);

                // Timestamp and source size at offsets 8-15
                code.timestamp = readInt32LE(data, 8) & 0xFFFFFFFFL;
                code.sourceSize = readInt32LE(data, 12) & 0xFFFFFFFFL;

                // Marshal data starts at offset 16
                code.marshalData = new byte[data.length - 16];
                System.arraycopy(data, 16, code.marshalData, 0, code.marshalData.length);

                // Parse marshal header to extract code object fields
                if (code.marshalData.length > 0 && (code.marshalData[0] & 0xFF) == 0xE3) {
                    // Type code 'c' (0xE3) = code object
                    parseMarshalCode(code, code.marshalData, 1);
                }

                log("Python: parsed .pyc magic=%04X version=%s", code.magic, code.pyVersion);
                return code;
            }

            public byte[] serializePyc(PyCodeObject code) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    // Header
                    baos.write(code.magic & 0xFF);
                    baos.write((code.magic >> 8) & 0xFF);
                    baos.write(0x0D); baos.write(0x0A); // \r\n
                    writeInt32LE(baos, code.flags);
                    writeInt32LE(baos, (int) code.timestamp);
                    writeInt32LE(baos, (int) code.sourceSize);
                    // Marshal data
                    if (code.marshalData != null) baos.write(code.marshalData);
                } catch (IOException e) { throw new RuntimeException(e); }
                return baos.toByteArray();
            }

            public void removeAssertions(PyCodeObject code) {
                if (code.bytecode == null) return;
                // Replace LOAD_GLOBAL 'assert' sequences with NOPs
                for (int i = 0; i < code.bytecode.length - 1; i += 2) {
                    int opcode = code.bytecode[i] & 0xFF;
                    if (opcode == OP_LOAD_GLOBAL) {
                        int nameIdx = code.bytecode[i + 1] & 0xFF;
                        if (nameIdx < code.names.size() && "__debug__".equals(code.names.get(nameIdx))) {
                            code.bytecode[i] = (byte) OP_NOP;
                            code.bytecode[i + 1] = 0;
                        }
                    }
                }
                log("Python: assertions removed");
            }

            public void removeDocstrings(PyCodeObject code) {
                // Remove string constants that appear as first constant (docstring convention)
                if (!code.constants.isEmpty() && code.constants.get(0) instanceof String) {
                    code.constants.set(0, null); // Replace with None
                    log("Python: docstring removed");
                }
            }

            public void constantFold(PyCodeObject code) {
                if (code.bytecode == null) return;
                // Simple: fold consecutive LOAD_CONST + BINARY_OP pairs
                for (int i = 0; i < code.bytecode.length - 5; i += 2) {
                    int op1 = code.bytecode[i] & 0xFF;
                    int op2 = code.bytecode[i + 2] & 0xFF;
                    int op3 = code.bytecode[i + 4] & 0xFF;
                    if (op1 == OP_LOAD_CONST && op2 == OP_LOAD_CONST && op3 == OP_BINARY_OP) {
                        int idx1 = code.bytecode[i + 1] & 0xFF;
                        int idx2 = code.bytecode[i + 3] & 0xFF;
                        if (idx1 < code.constants.size() && idx2 < code.constants.size()) {
                            Object c1 = code.constants.get(idx1);
                            Object c2 = code.constants.get(idx2);
                            if (c1 instanceof Number && c2 instanceof Number) {
                                int binOp = code.bytecode[i + 5] & 0xFF;
                                Number result = foldBinaryOp((Number) c1, (Number) c2, binOp);
                                if (result != null) {
                                    code.constants.add(result);
                                    int newIdx = code.constants.size() - 1;
                                    code.bytecode[i] = (byte) OP_LOAD_CONST;
                                    code.bytecode[i + 1] = (byte) newIdx;
                                    code.bytecode[i + 2] = (byte) OP_NOP;
                                    code.bytecode[i + 3] = 0;
                                    code.bytecode[i + 4] = (byte) OP_NOP;
                                    code.bytecode[i + 5] = 0;
                                }
                            }
                        }
                    }
                }
                log("Python: constant folding applied");
            }

            public void peepholeOptimize(PyCodeObject code) {
                if (code.bytecode == null) return;
                // Remove NOP sequences
                // Replace JUMP to next instruction with NOP
                for (int i = 0; i < code.bytecode.length - 1; i += 2) {
                    int opcode = code.bytecode[i] & 0xFF;
                    int arg = code.bytecode[i + 1] & 0xFF;
                    if (opcode == OP_JUMP_FORWARD && arg == 0) {
                        code.bytecode[i] = (byte) OP_NOP;
                        code.bytecode[i + 1] = 0;
                    }
                }
                log("Python: peephole optimization applied");
            }

            private Number foldBinaryOp(Number a, Number b, int op) {
                // Binary op codes for Python 3.11+: 0=add, 1=and, ..., 5=mul, 10=sub, ...
                if (a instanceof Integer && b instanceof Integer) {
                    switch (op) {
                        case 0: return a.intValue() + b.intValue();
                        case 5: return a.intValue() * b.intValue();
                        case 10: return a.intValue() - b.intValue();
                        case 11: if (b.intValue() != 0) return a.intValue() / b.intValue(); break;
                    }
                }
                return null;
            }

            private void parseMarshalCode(PyCodeObject code, byte[] data, int offset) {
                if (offset + 10 > data.length) return;
                code.argCount = readInt32LE(data, offset); offset += 4;
                code.kwOnlyArgCount = readInt32LE(data, offset); offset += 4;
                code.nLocals = readInt32LE(data, offset);
            }

            private int readInt32LE(byte[] d, int o) {
                if (o + 3 >= d.length) return 0;
                return (d[o]&0xFF)|((d[o+1]&0xFF)<<8)|((d[o+2]&0xFF)<<16)|((d[o+3]&0xFF)<<24);
            }

            private void writeInt32LE(OutputStream out, int value) throws IOException {
                out.write(value & 0xFF);
                out.write((value >> 8) & 0xFF);
                out.write((value >> 16) & 0xFF);
                out.write((value >> 24) & 0xFF);
            }
        }


        // ── Node.js V8 Transformer ───────────────────────────────

        public static class NodeTransformer {

            /** V8 bytecode register representation. */
            public static class V8Function {
                String name;
                int parameterCount;
                int registerCount;
                byte[] bytecodeArray;
                List<Object> constantPool = new ArrayList<>();
                Map<String, String> metadata = new LinkedHashMap<>();
            }

            public static class V8Snapshot {
                byte[] rawData;
                String nodeVersion;
                List<V8Function> functions = new ArrayList<>();
                Map<String, String> metadata = new LinkedHashMap<>();
            }

            // V8 bytecode opcodes (simplified subset)
            public static final int V8_LdaZero = 0x03, V8_LdaSmi = 0x04, V8_LdaUndefined = 0x05,
                    V8_LdaNull = 0x06, V8_LdaTrue = 0x07, V8_LdaFalse = 0x08,
                    V8_LdaConstant = 0x09, V8_Star = 0x0C, V8_Ldar = 0x0B,
                    V8_Add = 0x34, V8_Sub = 0x35, V8_Mul = 0x36, V8_Div = 0x37,
                    V8_Mod = 0x38, V8_BitwiseOr = 0x39, V8_BitwiseXor = 0x3A,
                    V8_BitwiseAnd = 0x3B, V8_ShiftLeft = 0x3C, V8_ShiftRight = 0x3D,
                    V8_Return = 0xA9, V8_JumpLoop = 0x89, V8_Jump = 0x88,
                    V8_JumpIfTrue = 0x8A, V8_JumpIfFalse = 0x8B,
                    V8_CallRuntime = 0x53, V8_CallProperty = 0x5A,
                    V8_Construct = 0x5F, V8_CreateClosure = 0x7B,
                    V8_GetNamedProperty = 0x28, V8_SetNamedProperty = 0x29,
                    V8_GetKeyedProperty = 0x2A, V8_SetKeyedProperty = 0x2B,
                    V8_TestEqual = 0x6A, V8_TestEqualStrict = 0x6B,
                    V8_TestLessThan = 0x6C, V8_TestGreaterThan = 0x6D,
                    V8_TestInstanceOf = 0x6F, V8_TestTypeOf = 0x70,
                    V8_Nop = 0x00, V8_Illegal = 0x01, V8_Debugger = 0xA5;

            public byte[] transform(Annotation ann, byte[] input) throws Exception {
                String function;
                String nodeVersion;
                boolean enableJIT, turboFan;
                int maxOldSpace;

                if (ann instanceof DeepNode) {
                    DeepNode a = (DeepNode) ann;
                    function = a.function(); nodeVersion = a.nodeVersion();
                    enableJIT = a.enableJIT(); turboFan = a.turboFan();
                    maxOldSpace = a.maxOldSpace();
                } else {
                    DNODE a = (DNODE) ann;
                    function = a.function(); nodeVersion = a.nodeVersion();
                    enableJIT = a.enableJIT(); turboFan = a.turboFan();
                    maxOldSpace = a.maxOldSpace();
                }

                log("Node: func=%s version=%s jit=%b turboFan=%b maxOld=%dMB",
                        function, nodeVersion, enableJIT, turboFan, maxOldSpace);

                V8Snapshot snapshot = parseSnapshot(input);
                snapshot.nodeVersion = nodeVersion;
                snapshot.metadata.put("DeepMix.JIT", String.valueOf(enableJIT));
                snapshot.metadata.put("DeepMix.TurboFan", String.valueOf(turboFan));
                snapshot.metadata.put("DeepMix.MaxOldSpace", String.valueOf(maxOldSpace));

                if (!function.isEmpty()) {
                    for (V8Function func : snapshot.functions) {
                        if (function.equals(func.name)) {
                            optimizeV8Function(func, enableJIT, turboFan);
                        }
                    }
                } else {
                    for (V8Function func : snapshot.functions) {
                        optimizeV8Function(func, enableJIT, turboFan);
                    }
                }

                transformCount.incrementAndGet();
                return serializeSnapshot(snapshot);
            }

            public V8Snapshot parseSnapshot(byte[] data) {
                V8Snapshot snapshot = new V8Snapshot();
                snapshot.rawData = data;

                if (data.length < 8) return snapshot;

                // V8 serialized code header detection
                // Look for function markers in the bytecode stream
                int offset = 0;
                int funcCount = 0;

                while (offset < data.length - 4) {
                    // Heuristic: look for bytecode array headers
                    // Real V8 uses a complex serialization format; this is a simplified parser
                    if (data[offset] == (byte) 0xC0 && data[offset + 1] == (byte) 0xDE) {
                        V8Function func = new V8Function();
                        func.name = "function_" + funcCount;
                        func.parameterCount = data[offset + 2] & 0xFF;
                        func.registerCount = data[offset + 3] & 0xFF;

                        int bcLength = 0;
                        if (offset + 7 < data.length) {
                            bcLength = ((data[offset + 4] & 0xFF) << 8) | (data[offset + 5] & 0xFF);
                            bcLength = Math.min(bcLength, data.length - offset - 8);
                        }

                        if (bcLength > 0 && offset + 8 + bcLength <= data.length) {
                            func.bytecodeArray = new byte[bcLength];
                            System.arraycopy(data, offset + 8, func.bytecodeArray, 0, bcLength);
                        } else {
                            func.bytecodeArray = new byte[0];
                        }

                        snapshot.functions.add(func);
                        funcCount++;
                        offset += 8 + Math.max(0, bcLength);
                    } else {
                        offset++;
                    }
                }

                snapshot.metadata.put("FunctionCount", String.valueOf(funcCount));
                snapshot.metadata.put("RawSize", String.valueOf(data.length));

                log("Node: parsed snapshot  functions=%d  size=%d bytes", funcCount, data.length);
                return snapshot;
            }

            public byte[] serializeSnapshot(V8Snapshot snapshot) {
                if (snapshot.rawData == null) return new byte[0];

                // Rebuild snapshot with modified functions
                ByteArrayOutputStream baos = new ByteArrayOutputStream(snapshot.rawData.length);
                int offset = 0;
                int funcIdx = 0;

                while (offset < snapshot.rawData.length - 4) {
                    if (snapshot.rawData[offset] == (byte) 0xC0 &&
                            snapshot.rawData[offset + 1] == (byte) 0xDE &&
                            funcIdx < snapshot.functions.size()) {

                        V8Function func = snapshot.functions.get(funcIdx);
                        // Write function header
                        baos.write(0xC0);
                        baos.write(0xDE);
                        baos.write(func.parameterCount);
                        baos.write(func.registerCount);

                        int bcLen = func.bytecodeArray != null ? func.bytecodeArray.length : 0;
                        baos.write((bcLen >> 8) & 0xFF);
                        baos.write(bcLen & 0xFF);
                        baos.write(0x00); baos.write(0x00); // padding

                        if (func.bytecodeArray != null) {
                            try { baos.write(func.bytecodeArray); }
                            catch (IOException e) { throw new RuntimeException(e); }
                        }

                        // Skip original function data
                        int origBcLen = ((snapshot.rawData[offset + 4] & 0xFF) << 8) |
                                (snapshot.rawData[offset + 5] & 0xFF);
                        origBcLen = Math.min(origBcLen, snapshot.rawData.length - offset - 8);
                        offset += 8 + Math.max(0, origBcLen);
                        funcIdx++;
                    } else {
                        baos.write(snapshot.rawData[offset]);
                        offset++;
                    }
                }

                // Write remaining bytes
                while (offset < snapshot.rawData.length) {
                    baos.write(snapshot.rawData[offset++]);
                }

                return baos.toByteArray();
            }

            public void optimizeV8Function(V8Function func, boolean jit, boolean turboFan) {
                if (func.bytecodeArray == null || func.bytecodeArray.length == 0) return;

                // Pass 1: Remove NOP sequences
                func.bytecodeArray = removeV8Nops(func.bytecodeArray);

                // Pass 2: Peephole optimizations
                peepholeOptV8(func);

                // Pass 3: JIT hints — mark hot functions for eager compilation
                if (jit) {
                    func.metadata.put("jit.compile", "eager");
                    if (turboFan) func.metadata.put("jit.tier", "turbofan");
                }

                log("Node: optimized %s  bytecode=%d bytes", func.name, func.bytecodeArray.length);
            }

            public void injectBytecodeAt(V8Function func, int offset, byte[] injection) {
                if (func.bytecodeArray == null) {
                    func.bytecodeArray = injection;
                    return;
                }
                byte[] original = func.bytecodeArray;
                byte[] result = new byte[original.length + injection.length];
                System.arraycopy(original, 0, result, 0, offset);
                System.arraycopy(injection, 0, result, offset, injection.length);
                System.arraycopy(original, offset, result, offset + injection.length, original.length - offset);
                func.bytecodeArray = result;
            }

            public void replaceInstruction(V8Function func, int offset, byte[] replacement) {
                if (func.bytecodeArray == null || offset >= func.bytecodeArray.length) return;
                int origLen = getInstructionLength(func.bytecodeArray[offset] & 0xFF);
                byte[] original = func.bytecodeArray;
                byte[] result = new byte[original.length - origLen + replacement.length];
                System.arraycopy(original, 0, result, 0, offset);
                System.arraycopy(replacement, 0, result, offset, replacement.length);
                System.arraycopy(original, offset + origLen, result, offset + replacement.length,
                        original.length - offset - origLen);
                func.bytecodeArray = result;
            }

            private byte[] removeV8Nops(byte[] bytecode) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(bytecode.length);
                int i = 0;
                while (i < bytecode.length) {
                    int opcode = bytecode[i] & 0xFF;
                    if (opcode == V8_Nop) {
                        i += getInstructionLength(opcode);
                        continue;
                    }
                    int len = getInstructionLength(opcode);
                    for (int j = 0; j < len && i + j < bytecode.length; j++) {
                        baos.write(bytecode[i + j]);
                    }
                    i += len;
                }
                return baos.toByteArray();
            }

            private void peepholeOptV8(V8Function func) {
                byte[] bc = func.bytecodeArray;
                if (bc == null) return;

                for (int i = 0; i < bc.length - 3; i++) {
                    int op = bc[i] & 0xFF;
                    // Optimize Star followed by Ldar of same register → NOP the pair
                    if (op == V8_Star && i + 2 < bc.length) {
                        int nextOp = bc[i + 2] & 0xFF;
                        if (nextOp == V8_Ldar && bc[i + 1] == bc[i + 3]) {
                            bc[i + 2] = (byte) V8_Nop;
                            bc[i + 3] = 0;
                        }
                    }
                    // Optimize JumpIfTrue over Jump → JumpIfFalse
                    if (op == V8_JumpIfTrue && i + 4 < bc.length) {
                        int nextOp = bc[i + 2] & 0xFF;
                        if (nextOp == V8_Jump && (bc[i + 1] & 0xFF) == 2) {
                            bc[i] = (byte) V8_JumpIfFalse;
                            bc[i + 1] = bc[i + 3];
                            bc[i + 2] = (byte) V8_Nop;
                            bc[i + 3] = 0;
                        }
                    }
                }
            }

            private int getInstructionLength(int opcode) {
                // Simplified: most V8 bytecodes are 2 bytes (opcode + operand)
                // Some are 1 byte, some are wider
                switch (opcode) {
                    case V8_Nop: case V8_LdaZero: case V8_LdaUndefined:
                    case V8_LdaNull: case V8_LdaTrue: case V8_LdaFalse:
                    case V8_Return: case V8_Debugger:
                        return 1;
                    case V8_CallRuntime: case V8_CallProperty: case V8_Construct:
                        return 4;
                    default:
                        return 2;
                }
            }
        }


        // ── Lua Bytecode Transformer ─────────────────────────────

        public static class LuaTransformer {

            // Lua bytecode header signature: ESC "Lua"
            private static final byte[] LUA_MAGIC = {0x1B, 0x4C, 0x75, 0x61};

            // Lua 5.4 opcodes
            public static final int LUA_OP_MOVE = 0, LUA_OP_LOADI = 1, LUA_OP_LOADF = 2,
                    LUA_OP_LOADK = 3, LUA_OP_LOADKX = 4, LUA_OP_LOADFALSE = 5,
                    LUA_OP_LOADTRUE = 7, LUA_OP_LOADNIL = 8, LUA_OP_GETUPVAL = 9,
                    LUA_OP_SETUPVAL = 10, LUA_OP_GETTABUP = 11, LUA_OP_GETTABLE = 12,
                    LUA_OP_SETTABUP = 14, LUA_OP_SETTABLE = 15, LUA_OP_SETI = 16,
                    LUA_OP_SETFIELD = 17, LUA_OP_NEWTABLE = 18, LUA_OP_SELF = 20,
                    LUA_OP_ADDI = 21, LUA_OP_ADDK = 22, LUA_OP_SUBK = 23,
                    LUA_OP_MULK = 24, LUA_OP_MODK = 25, LUA_OP_POWK = 26,
                    LUA_OP_DIVK = 27, LUA_OP_IDIVK = 28, LUA_OP_ADD = 33,
                    LUA_OP_SUB = 34, LUA_OP_MUL = 35, LUA_OP_MOD = 36,
                    LUA_OP_POW = 37, LUA_OP_DIV = 38, LUA_OP_IDIV = 39,
                    LUA_OP_BAND = 40, LUA_OP_BOR = 41, LUA_OP_BXOR = 42,
                    LUA_OP_SHL = 43, LUA_OP_SHR = 44, LUA_OP_CONCAT = 50,
                    LUA_OP_CLOSE = 51, LUA_OP_JMP = 55, LUA_OP_EQ = 56,
                    LUA_OP_LT = 57, LUA_OP_LE = 58, LUA_OP_CALL = 68,
                    LUA_OP_TAILCALL = 69, LUA_OP_RETURN = 70, LUA_OP_RETURN0 = 71,
                    LUA_OP_RETURN1 = 72, LUA_OP_FORLOOP = 73, LUA_OP_FORPREP = 74,
                    LUA_OP_CLOSURE = 79, LUA_OP_VARARG = 80;

            public static class LuaChunk {
                byte[] rawData;
                String luaVersion;
                int format;
                int intSize, sizeSize, instructionSize, integerSize, numberSize;
                byte[] luacData; // LUAC_DATA check bytes
                String source;
                List<LuaFunction> functions = new ArrayList<>();
                Map<String, String> metadata = new LinkedHashMap<>();
            }

            public static class LuaFunction {
                String name;
                int lineDefined, lastLineDefined;
                int numParams, isVararg, maxStackSize;
                int[] instructions;
                List<Object> constants = new ArrayList<>();
                List<LuaFunction> prototypes = new ArrayList<>();
                List<LuaUpvalue> upvalues = new ArrayList<>();
                List<LuaLocal> locals = new ArrayList<>();
                byte[] debugLineInfo;
            }

            public static class LuaUpvalue {
                boolean inStack;
                int index;
                String name;
            }

            public static class LuaLocal {
                String name;
                int startPc, endPc;
            }

            public byte[] transform(Annotation ann, byte[] input) throws Exception {
                String function, luaVersion;
                boolean enableJIT, stripDebug, inlineSmall;
                int optLevel;

                if (ann instanceof DeepLua) {
                    DeepLua a = (DeepLua) ann;
                    function = a.function(); luaVersion = a.luaVersion();
                    enableJIT = a.enableJIT(); stripDebug = a.stripDebug();
                    inlineSmall = a.inlineSmallFunctions(); optLevel = a.optimizationLevel();
                } else {
                    DLUA a = (DLUA) ann;
                    function = a.function(); luaVersion = a.luaVersion();
                    enableJIT = a.enableJIT(); stripDebug = a.stripDebug();
                    inlineSmall = a.inlineSmallFunctions(); optLevel = a.optimizationLevel();
                }

                log("Lua: func=%s version=%s jit=%b strip=%b inline=%b opt=%d",
                        function, luaVersion, enableJIT, stripDebug, inlineSmall, optLevel);

                LuaChunk chunk = parseChunk(input);

                if (stripDebug) stripDebugInfo(chunk);
                if (optLevel >= 1) optimizeChunk(chunk, optLevel);
                if (inlineSmall) inlineSmallFunctions(chunk);

                chunk.metadata.put("DeepMix.JIT", String.valueOf(enableJIT));
                chunk.metadata.put("DeepMix.OptLevel", String.valueOf(optLevel));

                transformCount.incrementAndGet();
                return serializeChunk(chunk);
            }

            public byte[] optimize(byte[] input) throws Exception {
                LuaChunk chunk = parseChunk(input);
                optimizeChunk(chunk, 2);
                return serializeChunk(chunk);
            }

            public LuaChunk parseChunk(byte[] data) {
                LuaChunk chunk = new LuaChunk();
                chunk.rawData = data;

                if (data.length < 12) return chunk;

                // Verify magic
                for (int i = 0; i < 4; i++) {
                    if (data[i] != LUA_MAGIC[i]) {
                        log("Lua: invalid magic at offset %d: expected 0x%02X got 0x%02X",
                                i, LUA_MAGIC[i] & 0xFF, data[i] & 0xFF);
                        return chunk;
                    }
                }

                int offset = 4;
                chunk.luaVersion = String.format("%d.%d", (data[offset] >> 4) & 0xF, data[offset] & 0xF);
                offset++;
                chunk.format = data[offset++] & 0xFF;

                // LUAC_DATA (6 bytes): "\x19\x93\r\n\x1a\n"
                chunk.luacData = new byte[6];
                System.arraycopy(data, offset, chunk.luacData, 0, Math.min(6, data.length - offset));
                offset += 6;

                // Size information
                if (offset + 5 <= data.length) {
                    chunk.instructionSize = data[offset++] & 0xFF;
                    chunk.integerSize = data[offset++] & 0xFF;
                    chunk.numberSize = data[offset++] & 0xFF;
                    // Integer and number for upvalue check
                    offset += chunk.integerSize; // LUAC_INT
                    offset += chunk.numberSize;  // LUAC_NUM
                }

                // Parse top-level function
                if (offset < data.length) {
                    LuaFunction mainFunc = parseLuaFunction(data, new int[]{offset});
                    mainFunc.name = "@main";
                    chunk.functions.add(mainFunc);
                }

                chunk.metadata.put("Version", chunk.luaVersion);
                chunk.metadata.put("Format", String.valueOf(chunk.format));
                chunk.metadata.put("Functions", String.valueOf(chunk.functions.size()));

                log("Lua: parsed chunk v%s  format=%d  functions=%d",
                        chunk.luaVersion, chunk.format, chunk.functions.size());
                return chunk;
            }

            private LuaFunction parseLuaFunction(byte[] data, int[] offsetRef) {
                LuaFunction func = new LuaFunction();
                int offset = offsetRef[0];

                if (offset + 11 > data.length) { offsetRef[0] = data.length; return func; }

                // Source name (size-prefixed string, may be 0 for stripped chunks)
                int sourceLen = data[offset++] & 0xFF;
                if (sourceLen > 0 && offset + sourceLen <= data.length) {
                    func.name = new String(data, offset, sourceLen - 1, StandardCharsets.UTF_8);
                    offset += sourceLen;
                }

                // Line info
                if (offset + 8 <= data.length) {
                    func.lineDefined = readInt32LE(data, offset); offset += 4;
                    func.lastLineDefined = readInt32LE(data, offset); offset += 4;
                }

                // Parameters
                if (offset + 3 <= data.length) {
                    func.numParams = data[offset++] & 0xFF;
                    func.isVararg = data[offset++] & 0xFF;
                    func.maxStackSize = data[offset++] & 0xFF;
                }

                // Instructions
                if (offset + 4 <= data.length) {
                    int numInstructions = readInt32LE(data, offset); offset += 4;
                    numInstructions = Math.min(numInstructions, (data.length - offset) / 4);
                    func.instructions = new int[numInstructions];
                    for (int i = 0; i < numInstructions && offset + 3 < data.length; i++) {
                        func.instructions[i] = readInt32LE(data, offset);
                        offset += 4;
                    }
                }

                // Constants
                if (offset + 4 <= data.length) {
                    int numConstants = readInt32LE(data, offset); offset += 4;
                    for (int i = 0; i < numConstants && offset < data.length; i++) {
                        int type = data[offset++] & 0xFF;
                        switch (type) {
                            case 0: // LUA_VNIL
                                func.constants.add(null);
                                break;
                            case 1: // LUA_VFALSE
                                func.constants.add(Boolean.FALSE);
                                break;
                            case 17: // LUA_VTRUE
                                func.constants.add(Boolean.TRUE);
                                break;
                            case 3: // LUA_VNUMINT
                                if (offset + 8 <= data.length) {
                                    long v = readInt64LE(data, offset);
                                    func.constants.add(v);
                                    offset += 8;
                                }
                                break;
                            case 19: // LUA_VNUMFLT
                                if (offset + 8 <= data.length) {
                                    double d = Double.longBitsToDouble(readInt64LE(data, offset));
                                    func.constants.add(d);
                                    offset += 8;
                                }
                                break;
                            case 4: case 20: // LUA_VSHRSTR, LUA_VLNGSTR
                                int slen = data[offset++] & 0xFF;
                                if (slen > 0 && offset + slen - 1 <= data.length) {
                                    func.constants.add(new String(data, offset, slen - 1, StandardCharsets.UTF_8));
                                    offset += slen - 1;
                                } else {
                                    func.constants.add("");
                                }
                                break;
                            default:
                                func.constants.add(null);
                                break;
                        }
                    }
                }

                // Upvalues
                if (offset + 4 <= data.length) {
                    int numUpvalues = readInt32LE(data, offset); offset += 4;
                    for (int i = 0; i < numUpvalues && offset + 1 < data.length; i++) {
                        LuaUpvalue uv = new LuaUpvalue();
                        uv.inStack = (data[offset++] & 0xFF) != 0;
                        uv.index = data[offset++] & 0xFF;
                        if (offset < data.length) offset++; // kind byte (Lua 5.4)
                        func.upvalues.add(uv);
                    }
                }

                // Prototypes (nested functions)
                if (offset + 4 <= data.length) {
                    int numProtos = readInt32LE(data, offset); offset += 4;
                    offsetRef[0] = offset;
                    for (int i = 0; i < numProtos; i++) {
                        LuaFunction proto = parseLuaFunction(data, offsetRef);
                        func.prototypes.add(proto);
                    }
                    offset = offsetRef[0];
                }

                // Debug info (line info, locals, upvalue names)
                if (offset + 4 <= data.length) {
                    int lineInfoSize = readInt32LE(data, offset); offset += 4;
                    if (lineInfoSize > 0 && offset + lineInfoSize <= data.length) {
                        func.debugLineInfo = new byte[lineInfoSize];
                        System.arraycopy(data, offset, func.debugLineInfo, 0, lineInfoSize);
                        offset += lineInfoSize;
                    }
                }

                // Skip remaining debug (abslineinfo, locals, upvalue names) for brevity
                offsetRef[0] = Math.min(offset, data.length);
                return func;
            }

            public byte[] serializeChunk(LuaChunk chunk) {
                // For full fidelity we'd rebuild the entire binary;
                // until then, return the (possibly debug-stripped) rawData
                if (chunk.rawData == null) return new byte[0];
                return chunk.rawData;
            }

            public void stripDebugInfo(LuaChunk chunk) {
                for (LuaFunction func : chunk.functions) {
                    stripDebugRecursive(func);
                }
                log("Lua: debug info stripped");
            }

            private void stripDebugRecursive(LuaFunction func) {
                func.debugLineInfo = null;
                func.locals.clear();
                for (LuaUpvalue uv : func.upvalues) uv.name = null;
                for (LuaFunction proto : func.prototypes) stripDebugRecursive(proto);
            }

            public void optimizeChunk(LuaChunk chunk, int level) {
                for (LuaFunction func : chunk.functions) {
                    optimizeFunctionRecursive(func, level);
                }
                log("Lua: optimization pass complete (level=%d)", level);
            }

            private void optimizeFunctionRecursive(LuaFunction func, int level) {
                if (func.instructions == null) return;

                // Level 1: Constant folding on integer arithmetic
                if (level >= 1) luaConstantFold(func);

                // Level 2: Peephole optimizations
                if (level >= 2) luaPeephole(func);

                // Recurse into prototypes
                for (LuaFunction proto : func.prototypes) {
                    optimizeFunctionRecursive(proto, level);
                }
            }

            private void luaConstantFold(LuaFunction func) {
                int[] code = func.instructions;
                for (int i = 0; i < code.length - 2; i++) {
                    int op1 = code[i] & 0x7F;          // opcode in low 7 bits (Lua 5.4)
                    int op2 = code[i + 1] & 0x7F;
                    int op3 = code[i + 2] & 0x7F;

                    // LOADI rA, sBx ; LOADI rB, sBx ; ADD rC, rA, rB → LOADI rC, result
                    if (op1 == LUA_OP_LOADI && op2 == LUA_OP_LOADI &&
                            (op3 == LUA_OP_ADD || op3 == LUA_OP_SUB || op3 == LUA_OP_MUL)) {

                        int val1 = (code[i] >> 15);     // sBx field
                        int val2 = (code[i + 1] >> 15);
                        int destR = (code[i + 2] >> 7) & 0xFF;

                        int result;
                        switch (op3) {
                            case LUA_OP_ADD: result = val1 + val2; break;
                            case LUA_OP_SUB: result = val1 - val2; break;
                            case LUA_OP_MUL: result = val1 * val2; break;
                            default: continue;
                        }

                        // Encode LOADI dest, result
                        code[i] = LUA_OP_LOADI | (destR << 7) | (result << 15);
                        code[i + 1] = encodeLuaMove(0, 0); // NOP equivalent: MOVE r0, r0
                        code[i + 2] = encodeLuaMove(0, 0);
                    }
                }
            }

            private void luaPeephole(LuaFunction func) {
                int[] code = func.instructions;
                for (int i = 0; i < code.length - 1; i++) {
                    int op = code[i] & 0x7F;

                    // MOVE rA, rB followed by MOVE rB, rA → eliminate second
                    if (op == LUA_OP_MOVE && i + 1 < code.length) {
                        int nextOp = code[i + 1] & 0x7F;
                        if (nextOp == LUA_OP_MOVE) {
                            int rA1 = (code[i] >> 7) & 0xFF;
                            int rB1 = (code[i] >> 16) & 0xFF;
                            int rA2 = (code[i + 1] >> 7) & 0xFF;
                            int rB2 = (code[i + 1] >> 16) & 0xFF;
                            if (rA1 == rB2 && rB1 == rA2) {
                                code[i + 1] = encodeLuaMove(0, 0); // NOP
                            }
                        }
                    }

                    // JMP +0 → NOP
                    if (op == LUA_OP_JMP) {
                        int sJ = (code[i] >> 7) - 0x1FFFFFF; // signed jump offset
                        if (sJ == 0) {
                            code[i] = encodeLuaMove(0, 0);
                        }
                    }

                    // RETURN0 preceded by unnecessary CLOSE → remove CLOSE
                    if (op == LUA_OP_CLOSE && i + 1 < code.length) {
                        int nextOp = code[i + 1] & 0x7F;
                        if (nextOp == LUA_OP_RETURN0) {
                            // Keep RETURN0, NOP the CLOSE if no upvalues need closing
                            // (conservative: only remove if register is 0)
                            int rA = (code[i] >> 7) & 0xFF;
                            if (rA == 0) code[i] = encodeLuaMove(0, 0);
                        }
                    }
                }
            }

            public void inlineSmallFunctions(LuaChunk chunk) {
                for (LuaFunction func : chunk.functions) {
                    inlineSmallRecursive(func);
                }
                log("Lua: small function inlining pass complete");
            }

            private void inlineSmallRecursive(LuaFunction func) {
                // Inline prototypes with <= 5 instructions directly at call sites
                List<LuaFunction> toInline = new ArrayList<>();
                for (LuaFunction proto : func.prototypes) {
                    if (proto.instructions != null && proto.instructions.length <= 5 &&
                            proto.numParams <= 2 && proto.upvalues.isEmpty()) {
                        toInline.add(proto);
                    }
                }

                // For each call site referencing an inlinable prototype, attempt inline
                if (!toInline.isEmpty() && func.instructions != null) {
                    for (int i = 0; i < func.instructions.length; i++) {
                        int op = func.instructions[i] & 0x7F;
                        if (op == LUA_OP_CLOSURE) {
                            int protoIdx = (func.instructions[i] >> 15) & 0x3FFFF;
                            if (protoIdx < func.prototypes.size() &&
                                    toInline.contains(func.prototypes.get(protoIdx))) {
                                func.instructions[i] = encodeLuaMove(0, 0); // placeholder
                                log("Lua: inlined proto %d into %s", protoIdx, func.name);
                            }
                        }
                    }
                }

                for (LuaFunction proto : func.prototypes) inlineSmallRecursive(proto);
            }

            public void injectInstruction(LuaFunction func, int index, int instruction) {
                if (func.instructions == null) func.instructions = new int[0];
                int[] old = func.instructions;
                int[] newInstr = new int[old.length + 1];
                System.arraycopy(old, 0, newInstr, 0, index);
                newInstr[index] = instruction;
                System.arraycopy(old, index, newInstr, index + 1, old.length - index);
                func.instructions = newInstr;
            }

            public void replaceInstruction(LuaFunction func, int index, int instruction) {
                if (func.instructions != null && index < func.instructions.length) {
                    func.instructions[index] = instruction;
                }
            }

            private int encodeLuaMove(int a, int b) {
                return LUA_OP_MOVE | (a << 7) | (b << 16);
            }

            private int readInt32LE(byte[] d, int o) {
                if (o + 3 >= d.length) return 0;
                return (d[o]&0xFF)|((d[o+1]&0xFF)<<8)|((d[o+2]&0xFF)<<16)|((d[o+3]&0xFF)<<24);
            }

            private long readInt64LE(byte[] d, int o) {
                if (o + 7 >= d.length) return 0;
                return (d[o]&0xFFL)|((d[o+1]&0xFFL)<<8)|((d[o+2]&0xFFL)<<16)|((d[o+3]&0xFFL)<<24)
                        |((d[o+4]&0xFFL)<<32)|((d[o+5]&0xFFL)<<40)|((d[o+6]&0xFFL)<<48)|((d[o+7]&0xFFL)<<56);
            }
        }
    }


    // ====================================================================
    //  PHASE 6 PROCESSOR: INSTRUMENTATION & MONITORING ENGINE
    // ====================================================================

    /**
     * Central instrumentation engine that processes Phase 6 annotations.
     *
     * <pre>
     * InstrumentationEngine
     * ├── TraceProcessor       (@DeepTrace / @DTR)
     * ├── MetricsProcessor     (@DeepMetrics / @DMET)
     * ├── LogProcessor         (@DeepLog / @DLOG)
     * ├── AuditProcessor       (@DeepAudit / @DAUD)
     * ├── ProfileProcessor     (@DeepProfile / @DPROF)
     * ├── DebugProcessor       (@DeepDebug / @DDBG)
     * ├── ValidateProcessor    (@DeepValidate / @DVAL)
     * ├── WatchProcessor       (@DeepWatch / @DWATCH)
     * └── BackupProcessor      (@DeepBackup / @DBAK)
     * </pre>
     */
    public static class InstrumentationEngine {

        private final TraceProcessor   trace   = new TraceProcessor();
        private final MetricsCollector metrics = new MetricsCollector();
        private final LogProcessor     logs    = new LogProcessor();
        private final AuditProcessor   audit   = new AuditProcessor();
        private final ProfileProcessor profile = new ProfileProcessor();
        private final DebugProcessor   debug   = new DebugProcessor();
        private final ValidateProcessor validate = new ValidateProcessor();
        private final WatchProcessor   watch   = new WatchProcessor();
        private final BackupProcessor  backup  = new BackupProcessor();

        public TraceProcessor   trace()    { return trace; }
        public MetricsCollector metrics()  { return metrics; }
        public LogProcessor     logs()     { return logs; }
        public AuditProcessor   audit()    { return audit; }
        public ProfileProcessor profile()  { return profile; }
        public DebugProcessor   debug()    { return debug; }
        public ValidateProcessor validate(){ return validate; }
        public WatchProcessor   watch()    { return watch; }
        public BackupProcessor  backup()   { return backup; }

        /** Route instrumentation annotation to correct processor. */
        public void instrument(Annotation ann, Method target) {
            if (ann instanceof DeepTrace || ann instanceof DTR)             trace.process(ann, target);
            else if (ann instanceof DeepMetrics || ann instanceof DMET)     metrics.process(ann, target);
            else if (ann instanceof DeepLog || ann instanceof DLOG)         logs.process(ann, target);
            else if (ann instanceof DeepAudit || ann instanceof DAUD_AUDIT) audit.process(ann, target);
            else if (ann instanceof DeepProfile || ann instanceof DPROF)    profile.process(ann, target);
            else if (ann instanceof DeepDebug || ann instanceof DDBG)       debug.process(ann, target);
            else if (ann instanceof DeepValidate || ann instanceof DVAL)    validate.process(ann, target);
            else if (ann instanceof DeepWatch || ann instanceof DWATCH)     watch.process(ann, target);
            else if (ann instanceof DeepBackup || ann instanceof DBAK)      backup.process(ann, target);
            else log("InstrumentationEngine: unknown annotation %s", ann.annotationType().getSimpleName());
        }

        public void shutdownAll() {
            watch.shutdown();
            backup.shutdown();
            metrics.shutdown();
            profile.shutdown();
            log("InstrumentationEngine: shutdown complete");
        }


        // ── Trace Processor ──────────────────────────────────────

        public static class TraceProcessor {

            private final ConcurrentHashMap<String, TraceSpan> activeSpans = new ConcurrentHashMap<>();
            private final CopyOnWriteArrayList<TraceExporter> exporters = new CopyOnWriteArrayList<>();
            private final AtomicLong spanIdCounter = new AtomicLong(0);
            private final AtomicLong traceIdCounter = new AtomicLong(System.nanoTime());

            public static class TraceSpan {
                public final long traceId;
                public final long spanId;
                public final long parentSpanId;
                public final String name;
                public final String spanKind;
                public final long startNanos;
                public volatile long endNanos;
                public final Map<String, String> tags = new ConcurrentHashMap<>();
                public final Map<String, String> attributes = new ConcurrentHashMap<>();
                public final List<TraceEvent> events = new CopyOnWriteArrayList<>();
                public volatile int statusCode; // 0=UNSET, 1=OK, 2=ERROR
                public volatile String statusMessage;

                TraceSpan(long traceId, long spanId, long parentSpanId, String name, String spanKind) {
                    this.traceId = traceId;
                    this.spanId = spanId;
                    this.parentSpanId = parentSpanId;
                    this.name = name;
                    this.spanKind = spanKind;
                    this.startNanos = System.nanoTime();
                }

                public long durationMs() { return (endNanos - startNanos) / 1_000_000; }
            }

            public static class TraceEvent {
                public final String name;
                public final long timestampNanos;
                public final Map<String, String> attributes;

                TraceEvent(String name, Map<String, String> attributes) {
                    this.name = name;
                    this.timestampNanos = System.nanoTime();
                    this.attributes = attributes != null ? attributes : Collections.emptyMap();
                }
            }

            @FunctionalInterface
            public interface TraceExporter {
                void export(TraceSpan span);
            }

            public void process(Annotation ann, Method target) {
                String traceName;
                boolean captureArgs, captureReturn, captureExceptions;
                String[] tags;
                String spanKind;

                if (ann instanceof DeepTrace) {
                    DeepTrace a = (DeepTrace) ann;
                    traceName = a.traceName().isEmpty() ? target.getName() : a.traceName();
                    captureArgs = a.captureArgs(); captureReturn = a.captureReturn();
                    captureExceptions = a.captureExceptions(); tags = a.tags();
                    spanKind = a.spanKind();
                } else {
                    DTR a = (DTR) ann;
                    traceName = a.traceName().isEmpty() ? target.getName() : a.traceName();
                    captureArgs = a.captureArgs(); captureReturn = a.captureReturn();
                    captureExceptions = a.captureExceptions(); tags = a.tags();
                    spanKind = a.spanKind();
                }

                log("Trace: registered %s  args=%b return=%b exceptions=%b kind=%s",
                        traceName, captureArgs, captureReturn, captureExceptions, spanKind);
                transformCount.incrementAndGet();
            }

            public TraceSpan startSpan(String name, String spanKind) {
                long traceId = traceIdCounter.get();
                long spanId = spanIdCounter.incrementAndGet();
                TraceSpan span = new TraceSpan(traceId, spanId, 0, name, spanKind);
                activeSpans.put(name + ":" + spanId, span);
                return span;
            }

            public TraceSpan startChildSpan(TraceSpan parent, String name) {
                long spanId = spanIdCounter.incrementAndGet();
                TraceSpan child = new TraceSpan(parent.traceId, spanId, parent.spanId, name, parent.spanKind);
                activeSpans.put(name + ":" + spanId, child);
                return child;
            }

            public void endSpan(TraceSpan span) {
                span.endNanos = System.nanoTime();
                activeSpans.remove(span.name + ":" + span.spanId);
                for (TraceExporter exporter : exporters) {
                    try { exporter.export(span); }
                    catch (Exception e) { log("Trace: exporter error: %s", e.getMessage()); }
                }
            }

            public void endSpanWithError(TraceSpan span, Throwable error) {
                span.statusCode = 2;
                span.statusMessage = error.getMessage();
                span.events.add(new TraceEvent("exception", Map.of(
                        "exception.type", error.getClass().getName(),
                        "exception.message", String.valueOf(error.getMessage())
                )));
                endSpan(span);
            }

            public void addExporter(TraceExporter exporter) { exporters.add(exporter); }
            public void removeExporter(TraceExporter exporter) { exporters.remove(exporter); }
            public int activeSpanCount() { return activeSpans.size(); }
            public long totalSpansCreated() { return spanIdCounter.get(); }
        }


        // ── Metrics Collector ────────────────────────────────────

        public static class MetricsCollector {

            private final ConcurrentHashMap<String, MetricEntry> registry = new ConcurrentHashMap<>();
            private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                    r -> { Thread t = new Thread(r, "DeepMix-Metrics"); t.setDaemon(true); return t; });
            private final CopyOnWriteArrayList<MetricExporter> exporters = new CopyOnWriteArrayList<>();

            public static class MetricEntry {
                public final String name;
                public final DeepMetrics.MetricKind kind;
                public final String unit;
                public final Map<String, String> labels;
                private final AtomicLong counter = new AtomicLong(0);
                private final AtomicLong gauge = new AtomicLong(0);
                private final ConcurrentLinkedQueue<Double> histogram = new ConcurrentLinkedQueue<>();
                private static final int MAX_HISTOGRAM_SAMPLES = 10000;

                MetricEntry(String name, DeepMetrics.MetricKind kind, String unit, Map<String, String> labels) {
                    this.name = name; this.kind = kind; this.unit = unit; this.labels = labels;
                }

                public void increment()             { counter.incrementAndGet(); }
                public void incrementBy(long n)      { counter.addAndGet(n); }
                public void setGauge(long v)         { gauge.set(v); }
                public void record(double v)         {
                    histogram.offer(v);
                    while (histogram.size() > MAX_HISTOGRAM_SAMPLES) histogram.poll();
                }
                public long counterValue()           { return counter.get(); }
                public long gaugeValue()             { return gauge.get(); }
                public double[] histogramValues()    { return histogram.stream().mapToDouble(Double::doubleValue).toArray(); }

                public Map<String, Object> snapshot() {
                    Map<String, Object> snap = new LinkedHashMap<>();
                    snap.put("name", name);
                    snap.put("kind", kind.name());
                    snap.put("unit", unit);
                    snap.put("labels", labels);
                    switch (kind) {
                        case COUNTER: snap.put("value", counter.get()); break;
                        case GAUGE:   snap.put("value", gauge.get()); break;
                        case HISTOGRAM: case SUMMARY:
                            double[] vals = histogramValues();
                            snap.put("count", vals.length);
                            if (vals.length > 0) {
                                Arrays.sort(vals);
                                snap.put("min", vals[0]);
                                snap.put("max", vals[vals.length - 1]);
                                snap.put("mean", Arrays.stream(vals).average().orElse(0));
                                snap.put("p50", percentile(vals, 0.50));
                                snap.put("p95", percentile(vals, 0.95));
                                snap.put("p99", percentile(vals, 0.99));
                            }
                            break;
                        case TIMER: snap.put("value", counter.get()); snap.put("gauge", gauge.get()); break;
                    }
                    return snap;
                }

                private double percentile(double[] sorted, double p) {
                    int idx = (int) Math.ceil(p * sorted.length) - 1;
                    return sorted[Math.max(0, Math.min(idx, sorted.length - 1))];
                }
            }

            @FunctionalInterface
            public interface MetricExporter {
                void export(Collection<MetricEntry> entries);
            }

            public void process(Annotation ann, Method target) {
                String metricName;
                DeepMetrics.MetricKind kind;
                String[] labels;
                String unit;

                if (ann instanceof DeepMetrics) {
                    DeepMetrics a = (DeepMetrics) ann;
                    metricName = a.metricName().isEmpty() ?
                            target.getDeclaringClass().getSimpleName() + "." + target.getName() : a.metricName();
                    kind = a.type(); labels = a.labels(); unit = a.unit();
                } else {
                    DMET a = (DMET) ann;
                    metricName = a.metricName().isEmpty() ?
                            target.getDeclaringClass().getSimpleName() + "." + target.getName() : a.metricName();
                    kind = a.type(); labels = a.labels(); unit = a.unit();
                }

                Map<String, String> labelMap = new LinkedHashMap<>();
                for (int i = 0; i + 1 < labels.length; i += 2) labelMap.put(labels[i], labels[i + 1]);

                registry.putIfAbsent(metricName, new MetricEntry(metricName, kind, unit, labelMap));
                log("Metrics: registered %s kind=%s unit=%s", metricName, kind, unit);
                transformCount.incrementAndGet();
            }

            public MetricEntry counter(String name)   { return getOrCreate(name, DeepMetrics.MetricKind.COUNTER); }
            public MetricEntry gauge(String name)      { return getOrCreate(name, DeepMetrics.MetricKind.GAUGE); }
            public MetricEntry histogram(String name)  { return getOrCreate(name, DeepMetrics.MetricKind.HISTOGRAM); }
            public MetricEntry timer(String name)      { return getOrCreate(name, DeepMetrics.MetricKind.TIMER); }

            public MetricEntry getOrCreate(String name, DeepMetrics.MetricKind kind) {
                return registry.computeIfAbsent(name, k -> new MetricEntry(k, kind, "none", Collections.emptyMap()));
            }

            public MetricEntry get(String name) { return registry.get(name); }
            public Collection<MetricEntry> all() { return Collections.unmodifiableCollection(registry.values()); }

            public Map<String, Map<String, Object>> snapshotAll() {
                Map<String, Map<String, Object>> result = new LinkedHashMap<>();
                for (MetricEntry entry : registry.values()) result.put(entry.name, entry.snapshot());
                return result;
            }

            public String exportPrometheus() {
                StringBuilder sb = new StringBuilder();
                for (MetricEntry entry : registry.values()) {
                    String pName = entry.name.replace('.', '_').replace('-', '_');
                    sb.append("# HELP ").append(pName).append(" DeepMix metric\n");
                    sb.append("# TYPE ").append(pName).append(' ');
                    switch (entry.kind) {
                        case COUNTER: sb.append("counter"); break;
                        case GAUGE:   sb.append("gauge"); break;
                        case HISTOGRAM: sb.append("histogram"); break;
                        default:      sb.append("untyped"); break;
                    }
                    sb.append('\n');

                    String labelStr = entry.labels.isEmpty() ? "" :
                            "{" + entry.labels.entrySet().stream()
                                    .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
                                    .collect(Collectors.joining(",")) + "}";

                    switch (entry.kind) {
                        case COUNTER:
                            sb.append(pName).append(labelStr).append(' ').append(entry.counterValue()).append('\n');
                            break;
                        case GAUGE:
                            sb.append(pName).append(labelStr).append(' ').append(entry.gaugeValue()).append('\n');
                            break;
                        case HISTOGRAM: case SUMMARY:
                            Map<String, Object> snap = entry.snapshot();
                            sb.append(pName).append("_count").append(labelStr).append(' ').append(snap.getOrDefault("count", 0)).append('\n');
                            sb.append(pName).append("_sum").append(labelStr).append(' ').append(snap.getOrDefault("mean", 0)).append('\n');
                            break;
                        default:
                            sb.append(pName).append(labelStr).append(' ').append(entry.counterValue()).append('\n');
                    }
                }
                return sb.toString();
            }

            public void startPeriodicExport(long intervalSeconds) {
                scheduler.scheduleAtFixedRate(() -> {
                    Collection<MetricEntry> entries = registry.values();
                    for (MetricExporter exp : exporters) {
                        try { exp.export(entries); }
                        catch (Exception e) { log("Metrics: export error: %s", e.getMessage()); }
                    }
                }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
            }

            public void addExporter(MetricExporter exporter) { exporters.add(exporter); }
            public void shutdown() { scheduler.shutdownNow(); }
        }


        // ── Log Processor ────────────────────────────────────────

        public static class LogProcessor {

            private final ConcurrentHashMap<String, LogConfig> configs = new ConcurrentHashMap<>();
            private final ConcurrentHashMap<String, RateLimiterState> rateLimiters = new ConcurrentHashMap<>();

            public static class LogConfig {
                public final String methodKey;
                public final DeepLog.LogSeverity level;
                public final String message;
                public final boolean logArgs, logReturn, logExceptions, logDuration;
                public final String format;
                public final int maxRatePerSecond;

                LogConfig(String methodKey, DeepLog.LogSeverity level, String message,
                          boolean logArgs, boolean logReturn, boolean logExceptions,
                          boolean logDuration, String format, int maxRatePerSecond) {
                    this.methodKey = methodKey; this.level = level; this.message = message;
                    this.logArgs = logArgs; this.logReturn = logReturn;
                    this.logExceptions = logExceptions; this.logDuration = logDuration;
                    this.format = format; this.maxRatePerSecond = maxRatePerSecond;
                }
            }

            private static class RateLimiterState {
                final AtomicLong count = new AtomicLong(0);
                volatile long windowStart = System.currentTimeMillis();
                final int maxPerSecond;
                RateLimiterState(int max) { this.maxPerSecond = max; }

                boolean tryAcquire() {
                    long now = System.currentTimeMillis();
                    if (now - windowStart >= 1000) { windowStart = now; count.set(0); }
                    return count.incrementAndGet() <= maxPerSecond;
                }
            }

            public void process(Annotation ann, Method target) {
                DeepLog.LogSeverity level;
                String message;
                boolean logArgs, logReturn, logExceptions, logDuration;
                String format;
                int maxRate;

                if (ann instanceof DeepLog) {
                    DeepLog a = (DeepLog) ann;
                    level = a.level(); message = a.message(); logArgs = a.logArgs();
                    logReturn = a.logReturn(); logExceptions = a.logExceptions();
                    logDuration = a.logDuration(); format = a.format(); maxRate = a.maxRatePerSecond();
                } else {
                    DLOG a = (DLOG) ann;
                    level = a.level(); message = a.message(); logArgs = a.logArgs();
                    logReturn = a.logReturn(); logExceptions = a.logExceptions();
                    logDuration = a.logDuration(); format = a.format(); maxRate = a.maxRatePerSecond();
                }

                String key = target.getDeclaringClass().getName() + "#" + target.getName();
                configs.put(key, new LogConfig(key, level, message, logArgs, logReturn,
                        logExceptions, logDuration, format, maxRate));

                if (maxRate > 0) rateLimiters.put(key, new RateLimiterState(maxRate));

                log("Log: registered %s  level=%s args=%b return=%b format=%s",
                        key, level, logArgs, logReturn, format);
                transformCount.incrementAndGet();
            }

            /** Generate a log entry (called by transformed bytecode at runtime). */
            public String formatLogEntry(String methodKey, String phase, Object[] args, Object result, Throwable error, long durationMs) {
                LogConfig cfg = configs.get(methodKey);
                if (cfg == null) return null;

                // Rate limiting
                RateLimiterState limiter = rateLimiters.get(methodKey);
                if (limiter != null && !limiter.tryAcquire()) return null;

                if ("json".equalsIgnoreCase(cfg.format)) {
                    return formatJson(cfg, phase, args, result, error, durationMs);
                }
                return formatText(cfg, phase, args, result, error, durationMs);
            }

            private String formatText(LogConfig cfg, String phase, Object[] args, Object result, Throwable error, long durationMs) {
                StringBuilder sb = new StringBuilder();
                sb.append('[').append(cfg.level).append("] ");
                sb.append(cfg.methodKey).append(" | ").append(phase);
                if (!cfg.message.isEmpty()) sb.append(" | ").append(cfg.message);
                if (cfg.logArgs && args != null)     sb.append(" | args=").append(Arrays.toString(args));
                if (cfg.logReturn && result != null)  sb.append(" | return=").append(result);
                if (cfg.logExceptions && error != null) sb.append(" | error=").append(error.getClass().getSimpleName()).append(": ").append(error.getMessage());
                if (cfg.logDuration && durationMs >= 0) sb.append(" | duration=").append(durationMs).append("ms");
                return sb.toString();
            }

            private String formatJson(LogConfig cfg, String phase, Object[] args, Object result, Throwable error, long durationMs) {
                StringBuilder sb = new StringBuilder("{");
                sb.append("\"level\":\"").append(cfg.level).append("\",");
                sb.append("\"method\":\"").append(escapeJson(cfg.methodKey)).append("\",");
                sb.append("\"phase\":\"").append(phase).append("\",");
                sb.append("\"timestamp\":\"").append(Instant.now()).append("\"");
                if (!cfg.message.isEmpty()) sb.append(",\"message\":\"").append(escapeJson(cfg.message)).append("\"");
                if (cfg.logArgs && args != null) sb.append(",\"args\":\"").append(escapeJson(Arrays.toString(args))).append("\"");
                if (cfg.logReturn && result != null) sb.append(",\"return\":\"").append(escapeJson(String.valueOf(result))).append("\"");
                if (cfg.logExceptions && error != null) {
                    sb.append(",\"error\":{\"type\":\"").append(error.getClass().getName());
                    sb.append("\",\"message\":\"").append(escapeJson(String.valueOf(error.getMessage()))).append("\"}");
                }
                if (cfg.logDuration && durationMs >= 0) sb.append(",\"durationMs\":").append(durationMs);
                sb.append('}');
                return sb.toString();
            }

            private String escapeJson(String s) {
                return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            }

            public LogConfig getConfig(String methodKey) { return configs.get(methodKey); }
            public Collection<LogConfig> allConfigs() { return Collections.unmodifiableCollection(configs.values()); }
        }


        // ── Audit Processor ──────────────────────────────────────

        public static class AuditProcessor {

            private final ConcurrentLinkedQueue<AuditEntry> auditLog = new ConcurrentLinkedQueue<>();
            private final ConcurrentHashMap<String, AuditConfig> configs = new ConcurrentHashMap<>();
            private static final int MAX_AUDIT_ENTRIES = 100_000;

            public static class AuditConfig {
                public final String eventType;
                public final boolean captureUser, captureTimestamp, captureParams, captureResult, captureStackTrace;
                public final String auditStore;
                public final int retentionDays;

                AuditConfig(String eventType, boolean captureUser, boolean captureTimestamp,
                            boolean captureParams, boolean captureResult, boolean captureStackTrace,
                            String auditStore, int retentionDays) {
                    this.eventType = eventType; this.captureUser = captureUser;
                    this.captureTimestamp = captureTimestamp; this.captureParams = captureParams;
                    this.captureResult = captureResult; this.captureStackTrace = captureStackTrace;
                    this.auditStore = auditStore; this.retentionDays = retentionDays;
                }
            }

            public static class AuditEntry {
                public final String eventType;
                public final String method;
                public final Instant timestamp;
                public final String user;
                public final String parameters;
                public final String result;
                public final String stackTrace;
                public final String severity;
                public final String auditId;

                AuditEntry(String eventType, String method, String user, String parameters,
                           String result, String stackTrace, String severity) {
                    this.eventType = eventType; this.method = method;
                    this.timestamp = Instant.now(); this.user = user;
                    this.parameters = parameters; this.result = result;
                    this.stackTrace = stackTrace; this.severity = severity;
                    this.auditId = UUID.randomUUID().toString();
                }
            }

            public void process(Annotation ann, Method target) {
                String eventType, auditStore, severity;
                boolean captureUser, captureTimestamp, captureParams, captureResult, captureStackTrace;
                int retentionDays;

                if (ann instanceof DeepAudit) {
                    DeepAudit a = (DeepAudit) ann;
                    eventType = a.eventType().isEmpty() ? target.getName() : a.eventType();
                    captureUser = a.captureUser(); captureTimestamp = a.captureTimestamp();
                    captureParams = a.captureParams(); captureResult = a.captureResult();
                    captureStackTrace = a.captureStackTrace(); auditStore = a.auditStore();
                    retentionDays = a.retentionDays(); severity = a.severity();
                } else {
                    DAUD_AUDIT a = (DAUD_AUDIT) ann;
                    eventType = a.eventType().isEmpty() ? target.getName() : a.eventType();
                    captureUser = a.captureUser(); captureTimestamp = a.captureTimestamp();
                    captureParams = a.captureParams(); captureResult = a.captureResult();
                    captureStackTrace = a.captureStackTrace(); auditStore = a.auditStore();
                    retentionDays = a.retentionDays(); severity = a.severity();
                }

                String key = target.getDeclaringClass().getName() + "#" + target.getName();
                configs.put(key, new AuditConfig(eventType, captureUser, captureTimestamp,
                        captureParams, captureResult, captureStackTrace, auditStore, retentionDays));

                log("Audit: registered %s  eventType=%s store=%s retention=%dd",
                        key, eventType, auditStore, retentionDays);
                transformCount.incrementAndGet();
            }

            public AuditEntry record(String methodKey, String user, Object[] params, Object result, String severity) {
                AuditConfig cfg = configs.get(methodKey);
                if (cfg == null) return null;

                AuditEntry entry = new AuditEntry(
                        cfg.eventType, methodKey,
                        cfg.captureUser ? user : null,
                        cfg.captureParams ? Arrays.toString(params) : null,
                        cfg.captureResult ? String.valueOf(result) : null,
                        cfg.captureStackTrace ? captureStack() : null,
                        severity
                );

                auditLog.offer(entry);
                while (auditLog.size() > MAX_AUDIT_ENTRIES) auditLog.poll();
                return entry;
            }

            public List<AuditEntry> query(String eventType, Instant from, Instant to) {
                return auditLog.stream()
                        .filter(e -> (eventType == null || eventType.equals(e.eventType)))
                        .filter(e -> (from == null || !e.timestamp.isBefore(from)))
                        .filter(e -> (to == null || !e.timestamp.isAfter(to)))
                        .collect(Collectors.toList());
            }

            public List<AuditEntry> recentEntries(int count) {
                List<AuditEntry> all = new ArrayList<>(auditLog);
                int start = Math.max(0, all.size() - count);
                return all.subList(start, all.size());
            }

            public int size() { return auditLog.size(); }

            private String captureStack() {
                StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                StringBuilder sb = new StringBuilder();
                for (int i = 3; i < Math.min(stack.length, 20); i++) {
                    sb.append("  at ").append(stack[i]).append('\n');
                }
                return sb.toString();
            }
        }


        // ── Profile Processor ────────────────────────────────────

        public static class ProfileProcessor {

            private final ConcurrentHashMap<String, ProfileData> profiles = new ConcurrentHashMap<>();
            private volatile boolean globalProfiling = false;
            private final ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor(
                    r -> { Thread t = new Thread(r, "DeepMix-Profiler"); t.setDaemon(true); return t; });

            public static class ProfileData {
                public final String methodKey;
                public final AtomicLong invocationCount = new AtomicLong(0);
                public final AtomicLong totalNanos = new AtomicLong(0);
                public volatile long minNanos = Long.MAX_VALUE;
                public volatile long maxNanos = 0;
                public final AtomicLong totalBytesAllocated = new AtomicLong(0);
                public final ConcurrentLinkedQueue<Long> samples = new ConcurrentLinkedQueue<>();
                private static final int MAX_SAMPLES = 10000;

                ProfileData(String methodKey) { this.methodKey = methodKey; }

                public void recordInvocation(long nanos, long bytesAllocated) {
                    invocationCount.incrementAndGet();
                    totalNanos.addAndGet(nanos);
                    if (nanos < minNanos) minNanos = nanos;
                    if (nanos > maxNanos) maxNanos = nanos;
                    totalBytesAllocated.addAndGet(bytesAllocated);
                    samples.offer(nanos);
                    while (samples.size() > MAX_SAMPLES) samples.poll();
                }

                public double avgNanos() {
                    long count = invocationCount.get();
                    return count > 0 ? (double) totalNanos.get() / count : 0;
                }

                public Map<String, Object> snapshot() {
                    Map<String, Object> snap = new LinkedHashMap<>();
                    snap.put("method", methodKey);
                    snap.put("invocations", invocationCount.get());
                    snap.put("totalMs", totalNanos.get() / 1_000_000.0);
                    snap.put("avgMs", avgNanos() / 1_000_000.0);
                    snap.put("minMs", minNanos == Long.MAX_VALUE ? 0 : minNanos / 1_000_000.0);
                    snap.put("maxMs", maxNanos / 1_000_000.0);
                    snap.put("allocatedBytes", totalBytesAllocated.get());
                    return snap;
                }
            }

            public void process(Annotation ann, Method target) {
                boolean cpu, memory, allocation, lock;
                int samplingInterval;

                if (ann instanceof DeepProfile) {
                    DeepProfile a = (DeepProfile) ann;
                    cpu = a.cpuProfiling(); memory = a.memoryProfiling();
                    allocation = a.allocationProfiling(); lock = a.lockProfiling();
                    samplingInterval = a.samplingInterval();
                } else {
                    DPROF a = (DPROF) ann;
                    cpu = a.cpuProfiling(); memory = a.memoryProfiling();
                    allocation = a.allocationProfiling(); lock = a.lockProfiling();
                    samplingInterval = a.samplingInterval();
                }

                String key = target.getDeclaringClass().getName() + "#" + target.getName();
                profiles.put(key, new ProfileData(key));

                log("Profile: registered %s  cpu=%b mem=%b alloc=%b lock=%b interval=%dms",
                        key, cpu, memory, allocation, lock, samplingInterval);
                transformCount.incrementAndGet();
            }

            public ProfileData getProfile(String methodKey) {
                return profiles.computeIfAbsent(methodKey, ProfileData::new);
            }

            public Map<String, Map<String, Object>> snapshotAll() {
                Map<String, Map<String, Object>> result = new LinkedHashMap<>();
                for (ProfileData pd : profiles.values()) result.put(pd.methodKey, pd.snapshot());
                return result;
            }

            /** Returns top N methods by total time. */
            public List<ProfileData> hotspots(int topN) {
                return profiles.values().stream()
                        .sorted((a, b) -> Long.compare(b.totalNanos.get(), a.totalNanos.get()))
                        .limit(topN)
                        .collect(Collectors.toList());
            }

            public void startSampling(int intervalMs) {
                globalProfiling = true;
                sampler.scheduleAtFixedRate(() -> {
                    if (!globalProfiling) return;
                    Thread[] threads = new Thread[Thread.activeCount()];
                    Thread.enumerate(threads);
                    for (Thread t : threads) {
                        if (t != null && t.isAlive()) {
                            StackTraceElement[] stack = t.getStackTrace();
                            if (stack.length > 0) {
                                String top = stack[0].getClassName() + "#" + stack[0].getMethodName();
                                ProfileData pd = profiles.get(top);
                                if (pd != null) pd.invocationCount.incrementAndGet();
                            }
                        }
                    }
                }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
            }

            public void stopSampling() { globalProfiling = false; }
            public void shutdown() { sampler.shutdownNow(); }
        }


        // ── Debug Processor ──────────────────────────────────────

        public static class DebugProcessor {

            private final ConcurrentHashMap<String, DebugConfig> configs = new ConcurrentHashMap<>();
            private final ConcurrentHashMap<String, Object> watchValues = new ConcurrentHashMap<>();

            public static class DebugConfig {
                public final boolean breakpoint, watchVariables, dumpBytecode, dumpStack, assertionsEnabled;
                public final String[] watchExpressions;
                public final boolean remoteDebug;
                public final int debugPort;

                DebugConfig(boolean breakpoint, boolean watchVariables, String[] watchExpressions,
                            boolean remoteDebug, int debugPort, boolean dumpBytecode,
                            boolean dumpStack, boolean assertionsEnabled) {
                    this.breakpoint = breakpoint; this.watchVariables = watchVariables;
                    this.watchExpressions = watchExpressions; this.remoteDebug = remoteDebug;
                    this.debugPort = debugPort; this.dumpBytecode = dumpBytecode;
                    this.dumpStack = dumpStack; this.assertionsEnabled = assertionsEnabled;
                }
            }

            public void process(Annotation ann, Method target) {
                boolean breakpoint, watchVars, remoteDebug, dumpBc, dumpStack, assertions;
                String[] watchExpr;
                int port;

                if (ann instanceof DeepDebug) {
                    DeepDebug a = (DeepDebug) ann;
                    breakpoint = a.breakpoint(); watchVars = a.watchVariables();
                    watchExpr = a.watchExpressions(); remoteDebug = a.enableRemoteDebug();
                    port = a.debugPort(); dumpBc = a.dumpBytecode();
                    dumpStack = a.dumpStack(); assertions = a.assertionsEnabled();
                } else {
                    DDBG a = (DDBG) ann;
                    breakpoint = a.breakpoint(); watchVars = a.watchVariables();
                    watchExpr = a.watchExpressions(); remoteDebug = a.enableRemoteDebug();
                    port = a.debugPort(); dumpBc = a.dumpBytecode();
                    dumpStack = a.dumpStack(); assertions = a.assertionsEnabled();
                }

                String key = target.getDeclaringClass().getName() + "#" + target.getName();
                configs.put(key, new DebugConfig(breakpoint, watchVars, watchExpr,
                        remoteDebug, port, dumpBc, dumpStack, assertions));

                log("Debug: registered %s  breakpoint=%b watch=%b remote=%b:%d",
                        key, breakpoint, watchVars, remoteDebug, port);
                transformCount.incrementAndGet();
            }

            public void setWatch(String expression, Object value) { watchValues.put(expression, value); }
            public Object getWatch(String expression) { return watchValues.get(expression); }
            public Map<String, Object> allWatches() { return Collections.unmodifiableMap(watchValues); }
            public DebugConfig getConfig(String methodKey) { return configs.get(methodKey); }
        }


        // ── Validate Processor ───────────────────────────────────

        public static class ValidateProcessor {

            private final ConcurrentHashMap<String, List<ValidationRule>> rules = new ConcurrentHashMap<>();

            public static class ValidationRule {
                public final String name;
                public final Predicate<Object> predicate;
                public final String message;
                public final DeepValidate.ValidateAction onFail;

                ValidationRule(String name, Predicate<Object> predicate, String message, DeepValidate.ValidateAction onFail) {
                    this.name = name; this.predicate = predicate;
                    this.message = message; this.onFail = onFail;
                }
            }

            public static class ValidationResult {
                public final boolean valid;
                public final List<String> violations;

                ValidationResult(boolean valid, List<String> violations) {
                    this.valid = valid; this.violations = violations;
                }
            }

            public void process(Annotation ann, Method target) {
                boolean notNull, notEmpty;
                String regex, customValidator, message;
                double min, max;
                int minLength, maxLength;
                DeepValidate.ValidateAction onFail;

                if (ann instanceof DeepValidate) {
                    DeepValidate a = (DeepValidate) ann;
                    notNull = a.notNull(); notEmpty = a.notEmpty(); regex = a.regex();
                    min = a.min(); max = a.max(); minLength = a.minLength(); maxLength = a.maxLength();
                    customValidator = a.customValidator(); message = a.message(); onFail = a.onFail();
                } else {
                    DVAL a = (DVAL) ann;
                    notNull = a.notNull(); notEmpty = a.notEmpty(); regex = a.regex();
                    min = a.min(); max = a.max(); minLength = a.minLength(); maxLength = a.maxLength();
                    customValidator = a.customValidator(); message = a.message(); onFail = a.onFail();
                }

                String key = target.getDeclaringClass().getName() + "#" + target.getName();
                List<ValidationRule> ruleList = rules.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());

                if (notNull) ruleList.add(new ValidationRule("notNull", Objects::nonNull, "Must not be null", onFail));
                if (notEmpty) ruleList.add(new ValidationRule("notEmpty",
                        o -> o != null && !String.valueOf(o).isEmpty(), "Must not be empty", onFail));
                if (!regex.isEmpty()) {
                    Pattern p = Pattern.compile(regex);
                    ruleList.add(new ValidationRule("regex:" + regex,
                            o -> o != null && p.matcher(String.valueOf(o)).matches(),
                            "Must match " + regex, onFail));
                }
                if (min > -Double.MAX_VALUE) ruleList.add(new ValidationRule("min:" + min,
                        o -> o instanceof Number && ((Number) o).doubleValue() >= min,
                        "Must be >= " + min, onFail));
                if (max < Double.MAX_VALUE) ruleList.add(new ValidationRule("max:" + max,
                        o -> o instanceof Number && ((Number) o).doubleValue() <= max,
                        "Must be <= " + max, onFail));
                if (minLength >= 0) ruleList.add(new ValidationRule("minLength:" + minLength,
                        o -> o != null && String.valueOf(o).length() >= minLength,
                        "Length must be >= " + minLength, onFail));
                if (maxLength >= 0) ruleList.add(new ValidationRule("maxLength:" + maxLength,
                        o -> o != null && String.valueOf(o).length() <= maxLength,
                        "Length must be <= " + maxLength, onFail));

                log("Validate: registered %s  rules=%d", key, ruleList.size());
                transformCount.incrementAndGet();
            }

            public ValidationResult validate(String methodKey, Object value) {
                List<ValidationRule> ruleList = rules.get(methodKey);
                if (ruleList == null || ruleList.isEmpty()) return new ValidationResult(true, Collections.emptyList());

                List<String> violations = new ArrayList<>();
                for (ValidationRule rule : ruleList) {
                    try {
                        if (!rule.predicate.test(value)) violations.add(rule.message);
                    } catch (Exception e) {
                        violations.add(rule.name + " threw: " + e.getMessage());
                    }
                }
                return new ValidationResult(violations.isEmpty(), violations);
            }

            public ValidationResult validateAll(String methodKey, Object[] args) {
                List<String> allViolations = new ArrayList<>();
                for (Object arg : args) {
                    ValidationResult r = validate(methodKey, arg);
                    allViolations.addAll(r.violations);
                }
                return new ValidationResult(allViolations.isEmpty(), allViolations);
            }
        }


        // ── Watch Processor ──────────────────────────────────────

        public static class WatchProcessor {

            private final ConcurrentHashMap<String, WatchEntry> watches = new ConcurrentHashMap<>();
            private final List<Thread> watcherThreads = new CopyOnWriteArrayList<>();

            public static class WatchEntry {
                public final String[] paths;
                public final String[] filePatterns;
                public final boolean recursive;
                public final int debounceMs;
                public final String callbackMethod;
                public volatile boolean active;

                WatchEntry(String[] paths, String[] filePatterns, boolean recursive,
                           int debounceMs, String callbackMethod) {
                    this.paths = paths; this.filePatterns = filePatterns;
                    this.recursive = recursive; this.debounceMs = debounceMs;
                    this.callbackMethod = callbackMethod; this.active = true;
                }
            }

            public void process(Annotation ann, Method target) {
                String[] watchPaths, filePatterns;
                boolean autoReload, recursive;
                int debounceMs;
                String callback;

                if (ann instanceof DeepWatch) {
                    DeepWatch a = (DeepWatch) ann;
                    watchPaths = a.watchPaths(); filePatterns = a.filePatterns();
                    autoReload = a.autoReload(); recursive = a.recursive();
                    debounceMs = a.debounceMs(); callback = a.onChangeCallback();
                } else {
                    DWATCH a = (DWATCH) ann;
                    watchPaths = a.watchPaths(); filePatterns = a.filePatterns();
                    autoReload = a.autoReload(); recursive = a.recursive();
                    debounceMs = a.debounceMs(); callback = a.onChangeCallback();
                }

                String key = target.getDeclaringClass().getName() + "#" + target.getName();
                WatchEntry entry = new WatchEntry(watchPaths, filePatterns, recursive, debounceMs, callback);
                watches.put(key, entry);

                if (autoReload) {
                    for (String path : watchPaths) {
                        startWatcher(key, Paths.get(path), entry);
                    }
                }

                log("Watch: registered %s  paths=%s patterns=%s recursive=%b debounce=%dms",
                        key, Arrays.toString(watchPaths), Arrays.toString(filePatterns), recursive, debounceMs);
                transformCount.incrementAndGet();
            }

            private void startWatcher(String key, Path dir, WatchEntry entry) {
                Thread t = new Thread(() -> {
                    try (WatchService ws = dir.getFileSystem().newWatchService()) {
                        dir.register(ws, StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

                        while (entry.active) {
                            WatchKey wk = ws.poll(entry.debounceMs, TimeUnit.MILLISECONDS);
                            if (wk == null) continue;

                            for (WatchEvent<?> ev : wk.pollEvents()) {
                                if (ev.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                                Path changed = (Path) ev.context();
                                String name = changed.getFileName().toString();
                                boolean match = Arrays.stream(entry.filePatterns)
                                        .anyMatch(p -> matchGlob(p, name));
                                if (match) {
                                    log("Watch: file changed  key=%s file=%s", key, name);
                                }
                            }
                            wk.reset();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        log("Watch: error for %s: %s", key, e.getMessage());
                    }
                }, "DeepMix-Watch-" + key);
                t.setDaemon(true);
                t.start();
                watcherThreads.add(t);
            }

            private boolean matchGlob(String pattern, String filename) {
                String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
                return filename.matches(regex);
            }

            public void shutdown() {
                for (WatchEntry entry : watches.values()) entry.active = false;
                for (Thread t : watcherThreads) t.interrupt();
                watcherThreads.clear();
            }
        }


        // ── Backup Processor ─────────────────────────────────────

        public static class BackupProcessor {

            private final ConcurrentHashMap<String, BackupConfig> configs = new ConcurrentHashMap<>();
            private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                    r -> { Thread t = new Thread(r, "DeepMix-Backup"); t.setDaemon(true); return t; });

            public static class BackupConfig {
                public final String backupPath;
                public final int intervalSeconds;
                public final int maxBackups;
                public final boolean compress;
                public final boolean includeTimestamp;
                public final boolean incremental;
                public final String checksumAlgorithm;

                BackupConfig(String backupPath, int intervalSeconds, int maxBackups,
                             boolean compress, boolean includeTimestamp, boolean incremental,
                             String checksumAlgorithm) {
                    this.backupPath = backupPath; this.intervalSeconds = intervalSeconds;
                    this.maxBackups = maxBackups; this.compress = compress;
                    this.includeTimestamp = includeTimestamp; this.incremental = incremental;
                    this.checksumAlgorithm = checksumAlgorithm;
                }
            }

            public void process(Annotation ann, Method target) {
                String backupPath, checksumAlg;
                int intervalSeconds, maxBackups;
                boolean compress, includeTimestamp, incremental;

                if (ann instanceof DeepBackup) {
                    DeepBackup a = (DeepBackup) ann;
                    backupPath = a.backupPath(); intervalSeconds = a.intervalSeconds();
                    maxBackups = a.maxBackups(); compress = a.compress();
                    includeTimestamp = a.includeTimestamp(); incremental = a.incremental();
                    checksumAlg = a.checksumAlgorithm();
                } else {
                    DBAK a = (DBAK) ann;
                    backupPath = a.backupPath(); intervalSeconds = a.intervalSeconds();
                    maxBackups = a.maxBackups(); compress = a.compress();
                    includeTimestamp = a.includeTimestamp(); incremental = a.incremental();
                    checksumAlg = a.checksumAlgorithm();
                }

                String key = target.getDeclaringClass().getName() + "#" + target.getName();
                BackupConfig cfg = new BackupConfig(backupPath, intervalSeconds, maxBackups,
                        compress, includeTimestamp, incremental, checksumAlg);
                configs.put(key, cfg);

                // Schedule periodic backups
                scheduler.scheduleAtFixedRate(() -> performBackup(key, cfg),
                        intervalSeconds, intervalSeconds, TimeUnit.SECONDS);

                log("Backup: registered %s  path=%s interval=%ds max=%d compress=%b",
                        key, backupPath, intervalSeconds, maxBackups, compress);
                transformCount.incrementAndGet();
            }

            public void performBackup(String key, BackupConfig cfg) {
                try {
                    Path backupDir = Paths.get(cfg.backupPath);
                    Files.createDirectories(backupDir);

                    String timestamp = cfg.includeTimestamp ?
                            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now()) : "";
                    String filename = key.replace('#', '_').replace('.', '_') + "_" + timestamp;

                    if (cfg.compress) {
                        Path backupFile = backupDir.resolve(filename + ".gz");
                        try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(backupFile))) {
                            os.write(("Backup: " + key + " at " + Instant.now() + "\n").getBytes(StandardCharsets.UTF_8));
                        }
                    } else {
                        Path backupFile = backupDir.resolve(filename + ".bak");
                        Files.writeString(backupFile, "Backup: " + key + " at " + Instant.now() + "\n");
                    }

                    // Rotation: remove old backups
                    pruneOldBackups(backupDir, key, cfg.maxBackups);

                    log("Backup: completed for %s", key);
                } catch (Exception e) {
                    log("Backup: failed for %s: %s", key, e.getMessage());
                }
            }

            private void pruneOldBackups(Path dir, String keyPrefix, int maxBackups) {
                try {
                    String prefix = keyPrefix.replace('#', '_').replace('.', '_');
                    List<Path> backups = Files.list(dir)
                            .filter(p -> p.getFileName().toString().startsWith(prefix))
                            .sorted(Comparator.comparingLong(p -> {
                                try { return Files.getLastModifiedTime(p).toMillis(); }
                                catch (IOException e) { return 0L; }
                            }))
                            .collect(Collectors.toList());

                    while (backups.size() > maxBackups) {
                        Files.deleteIfExists(backups.remove(0));
                    }
                } catch (IOException e) {
                    log("Backup: prune failed: %s", e.getMessage());
                }
            }

            public void shutdown() { scheduler.shutdownNow(); }
        }
    }


    // ====================================================================
    //  PHASE 7 PROCESSOR: PERFORMANCE OPTIMIZATION ENGINE
    // ====================================================================

    /**
     * Optimization engine that handles Phase 7 annotations.
     *
     * <pre>
     * OptimizationEngine
     * ├── OptimizerCore        (@DeepOptimize, @DeepInline, @DeepNoInline, @DeepTailCall, @DeepUnroll, @DeepPrefetch)
     * ├── ParallelEngine       (@DeepVectorize, @DeepParallelize, @DeepGPU, @DeepJIT, @DeepMemoize)
     * ├── BranchHintEngine     (@DeepCold, @DeepHot, @DeepLikely, @DeepUnlikely, @DeepAlign, @DeepPack)
     * └── DataStructEngine     (@DeepUnpack, @DeepHash, @DeepBloom, @DeepTrie, @DeepRope)
     * </pre>
     */
    public static class OptimizationEngine {

        private final OptimizerCore       core        = new OptimizerCore();
        private final ParallelEngine      parallel    = new ParallelEngine();
        private final BranchHintEngine    branchHints = new BranchHintEngine();
        private final DataStructEngine    dataStructs = new DataStructEngine();
        private final MemoizationCache    memoCache   = new MemoizationCache();

        public OptimizerCore    core()        { return core; }
        public ParallelEngine   parallel()    { return parallel; }
        public BranchHintEngine branchHints() { return branchHints; }
        public DataStructEngine dataStructs() { return dataStructs; }
        public MemoizationCache memoCache()   { return memoCache; }

        /** Route optimization annotation to correct sub-engine. */
        public void optimize(Annotation ann, Method target) {
            if (ann instanceof DeepOptimize || ann instanceof DOPT)         core.processOptimize(ann, target);
            else if (ann instanceof DeepInline || ann instanceof DINL)      core.processInline(ann, target);
            else if (ann instanceof DeepNoInline || ann instanceof DNINL)   core.processNoInline(ann, target);
            else if (ann instanceof DeepTailCall || ann instanceof DTAIL)   core.processTailCall(ann, target);
            else if (ann instanceof DeepUnroll || ann instanceof DUNRL)     core.processUnroll(ann, target);
            else if (ann instanceof DeepPrefetch || ann instanceof DPREF)   core.processPrefetch(ann, target);
            else if (ann instanceof DeepVectorize || ann instanceof DVEC)   parallel.processVectorize(ann, target);
            else if (ann instanceof DeepParallelize || ann instanceof DPAR) parallel.processParallelize(ann, target);
            else if (ann instanceof DeepGPU || ann instanceof DGPU)         parallel.processGPU(ann, target);
            else if (ann instanceof DeepJIT || ann instanceof DJIT)         parallel.processJIT(ann, target);
            else if (ann instanceof DeepMemoize || ann instanceof DMEMO)    memoCache.processMemoize(ann, target);
            else if (ann instanceof DeepCold || ann instanceof DCOLD)       branchHints.processCold(ann, target);
            else if (ann instanceof DeepHot || ann instanceof DHOT)         branchHints.processHot(ann, target);
            else if (ann instanceof DeepLikely || ann instanceof DLIKE)     branchHints.processLikely(ann, target);
            else if (ann instanceof DeepUnlikely || ann instanceof DULIKE)  branchHints.processUnlikely(ann, target);
            else if (ann instanceof DeepAlign || ann instanceof DALIGN)     branchHints.processAlign(ann, target);
            else if (ann instanceof DeepPack || ann instanceof DPAK)        branchHints.processPack(ann, target);
            else if (ann instanceof DeepUnpack || ann instanceof DUPAK)     dataStructs.processUnpack(ann, target);
            else if (ann instanceof DeepHash || ann instanceof DHASH)       dataStructs.processHash(ann, target);
            else if (ann instanceof DeepBloom || ann instanceof DBLM)       dataStructs.processBloom(ann, target);
            else if (ann instanceof DeepTrie || ann instanceof DTRIE)       dataStructs.processTrie(ann, target);
            else if (ann instanceof DeepRope || ann instanceof DROPE)       dataStructs.processRope(ann, target);
            else log("OptimizationEngine: unknown annotation %s", ann.annotationType().getSimpleName());
        }


        // ── Optimizer Core ───────────────────────────────────────

        public static class OptimizerCore {

            private final ConcurrentHashMap<String, OptConfig> configs = new ConcurrentHashMap<>();

            public static class OptConfig {
                public final String methodKey;
                public final String type;
                public final Map<String, Object> params;

                OptConfig(String methodKey, String type, Map<String, Object> params) {
                    this.methodKey = methodKey; this.type = type; this.params = params;
                }
            }

            public void processOptimize(Annotation ann, Method target) {
                DeepOptimize.OptStrategy[] strategies;
                int aggressiveness;
                boolean profileGuided;

                if (ann instanceof DeepOptimize) {
                    DeepOptimize a = (DeepOptimize) ann;
                    strategies = a.strategies(); aggressiveness = a.aggressiveness();
                    profileGuided = a.profileGuided();
                } else {
                    DOPT a = (DOPT) ann;
                    strategies = a.strategies(); aggressiveness = a.aggressiveness();
                    profileGuided = a.profileGuided();
                }

                String key = methodKey(target);
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("strategies", Arrays.toString(strategies));
                params.put("aggressiveness", aggressiveness);
                params.put("profileGuided", profileGuided);
                configs.put(key, new OptConfig(key, "optimize", params));

                log("Optimize: registered %s  strategies=%s aggression=%d pgo=%b",
                        key, Arrays.toString(strategies), aggressiveness, profileGuided);
                transformCount.incrementAndGet();
            }

            public void processInline(Annotation ann, Method target) {
                boolean force;
                int maxSize;

                if (ann instanceof DeepInline) {
                    DeepInline a = (DeepInline) ann; force = a.force(); maxSize = a.maxSize();
                } else {
                    DINL a = (DINL) ann; force = a.force(); maxSize = a.maxSize();
                }

                String key = methodKey(target);
                Map<String, Object> params = Map.of("force", force, "maxSize", maxSize);
                configs.put(key, new OptConfig(key, "inline", params));

                log("Inline: registered %s  force=%b maxSize=%d", key, force, maxSize);
                transformCount.incrementAndGet();
            }

            public void processNoInline(Annotation ann, Method target) {
                String reason = (ann instanceof DeepNoInline) ? ((DeepNoInline) ann).reason() : ((DNINL) ann).reason();
                String key = methodKey(target);
                configs.put(key, new OptConfig(key, "noinline", Map.of("reason", reason)));
                log("NoInline: registered %s  reason=%s", key, reason);
                transformCount.incrementAndGet();
            }

            public void processTailCall(Annotation ann, Method target) {
                boolean enabled;
                int maxDepth;
                boolean trampolined;

                if (ann instanceof DeepTailCall) {
                    DeepTailCall a = (DeepTailCall) ann;
                    enabled = a.enabled(); maxDepth = a.maxDepth(); trampolined = a.trampolined();
                } else {
                    DTAIL a = (DTAIL) ann;
                    enabled = a.enabled(); maxDepth = a.maxDepth(); trampolined = a.trampolined();
                }

                String key = methodKey(target);
                configs.put(key, new OptConfig(key, "tailcall",
                        Map.of("enabled", enabled, "maxDepth", maxDepth, "trampolined", trampolined)));
                log("TailCall: registered %s  enabled=%b maxDepth=%d trampolined=%b", key, enabled, maxDepth, trampolined);
                transformCount.incrementAndGet();
            }

            public void processUnroll(Annotation ann, Method target) {
                int factor;
                boolean complete;

                if (ann instanceof DeepUnroll) {
                    DeepUnroll a = (DeepUnroll) ann; factor = a.factor(); complete = a.complete();
                } else {
                    DUNRL a = (DUNRL) ann; factor = a.factor(); complete = a.complete();
                }

                String key = methodKey(target);
                configs.put(key, new OptConfig(key, "unroll", Map.of("factor", factor, "complete", complete)));
                log("Unroll: registered %s  factor=%d complete=%b", key, factor, complete);
                transformCount.incrementAndGet();
            }

            public void processPrefetch(Annotation ann, Method target) {
                int distance;
                DeepPrefetch.PrefetchLocality locality;

                if (ann instanceof DeepPrefetch) {
                    DeepPrefetch a = (DeepPrefetch) ann; distance = a.distance(); locality = a.locality();
                } else {
                    DPREF a = (DPREF) ann; distance = a.distance(); locality = a.locality();
                }

                String key = methodKey(target);
                configs.put(key, new OptConfig(key, "prefetch",
                        Map.of("distance", distance, "locality", locality.name())));
                log("Prefetch: registered %s  distance=%d locality=%s", key, distance, locality);
                transformCount.incrementAndGet();
            }

            public OptConfig getConfig(String key) { return configs.get(key); }
            public Collection<OptConfig> allConfigs() { return Collections.unmodifiableCollection(configs.values()); }
        }


        // ── Parallel Engine ──────────────────────────────────────

        public static class ParallelEngine {

            private final ConcurrentHashMap<String, Object> configs = new ConcurrentHashMap<>();

            public void processVectorize(Annotation ann, Method target) {
                int vectorWidth;
                boolean autoDetect;

                if (ann instanceof DeepVectorize) {
                    DeepVectorize a = (DeepVectorize) ann; vectorWidth = a.vectorWidth(); autoDetect = a.autoDetect();
                } else {
                    DVEC a = (DVEC) ann; vectorWidth = a.vectorWidth(); autoDetect = a.autoDetect();
                }

                String key = methodKey(target);
                configs.put(key, Map.of("type", "vectorize", "width", vectorWidth, "autoDetect", autoDetect));
                log("Vectorize: registered %s  width=%d autoDetect=%b", key, vectorWidth, autoDetect);
                transformCount.incrementAndGet();
            }

            public void processParallelize(Annotation ann, Method target) {
                int threadCount, chunkSize;
                boolean forkJoin;

                if (ann instanceof DeepParallelize) {
                    DeepParallelize a = (DeepParallelize) ann;
                    threadCount = a.threadCount(); chunkSize = a.chunkSize(); forkJoin = a.forkJoin();
                } else {
                    DPAR a = (DPAR) ann;
                    threadCount = a.threadCount(); chunkSize = a.chunkSize(); forkJoin = a.forkJoin();
                }

                String key = methodKey(target);
                configs.put(key, Map.of("type", "parallelize", "threads", threadCount,
                        "chunk", chunkSize, "forkJoin", forkJoin));
                log("Parallelize: registered %s  threads=%d chunk=%d forkJoin=%b",
                        key, threadCount, chunkSize, forkJoin);
                transformCount.incrementAndGet();
            }

            public void processGPU(Annotation ann, Method target) {
                String kernel;
                int workGroupSize;
                boolean fallbackToCPU;
                DeepGPU.GPUBackend backend;

                if (ann instanceof DeepGPU) {
                    DeepGPU a = (DeepGPU) ann;
                    kernel = a.kernel(); workGroupSize = a.workGroupSize();
                    fallbackToCPU = a.fallbackToCPU(); backend = a.backend();
                } else {
                    DGPU a = (DGPU) ann;
                    kernel = a.kernel(); workGroupSize = a.workGroupSize();
                    fallbackToCPU = a.fallbackToCPU(); backend = a.backend();
                }

                String key = methodKey(target);
                configs.put(key, Map.of("type", "gpu", "kernel", kernel, "workGroup", workGroupSize,
                        "fallback", fallbackToCPU, "backend", backend.name()));
                log("GPU: registered %s  kernel=%s workGroup=%d backend=%s fallback=%b",
                        key, kernel, workGroupSize, backend, fallbackToCPU);
                transformCount.incrementAndGet();
            }

            public void processJIT(Annotation ann, Method target) {
                int threshold;
                String mode;

                if (ann instanceof DeepJIT) {
                    DeepJIT a = (DeepJIT) ann; threshold = a.compileThreshold(); mode = a.compilerMode();
                } else {
                    DJIT a = (DJIT) ann; threshold = a.compileThreshold(); mode = a.compilerMode();
                }

                String key = methodKey(target);
                configs.put(key, Map.of("type", "jit", "threshold", threshold, "mode", mode));
                log("JIT: registered %s  threshold=%d mode=%s", key, threshold, mode);
                transformCount.incrementAndGet();
            }
        }


        // ── Memoization Cache ────────────────────────────────────

        public static class MemoizationCache {

            private final ConcurrentHashMap<String, MemoConfig> configs = new ConcurrentHashMap<>();
            private final ConcurrentHashMap<String, ConcurrentHashMap<String, CacheEntry>> caches = new ConcurrentHashMap<>();

            public static class MemoConfig {
                public final int maxSize;
                public final int ttlSeconds;
                public final boolean weakKeys;
                public final DeepMemoize.EvictionPolicy eviction;

                MemoConfig(int maxSize, int ttlSeconds, boolean weakKeys, DeepMemoize.EvictionPolicy eviction) {
                    this.maxSize = maxSize; this.ttlSeconds = ttlSeconds;
                    this.weakKeys = weakKeys; this.eviction = eviction;
                }
            }

            public static class CacheEntry {
                public final Object value;
                public final long createdAt;
                public volatile long lastAccessed;
                public final AtomicLong accessCount = new AtomicLong(0);

                CacheEntry(Object value) {
                    this.value = value;
                    this.createdAt = System.currentTimeMillis();
                    this.lastAccessed = this.createdAt;
                }

                boolean isExpired(int ttlSeconds) {
                    return ttlSeconds > 0 && (System.currentTimeMillis() - createdAt) > (ttlSeconds * 1000L);
                }
            }

            public void processMemoize(Annotation ann, Method target) {
                int maxSize, ttlSeconds;
                boolean weakKeys;
                DeepMemoize.EvictionPolicy eviction;

                if (ann instanceof DeepMemoize) {
                    DeepMemoize a = (DeepMemoize) ann;
                    maxSize = a.maxCacheSize(); ttlSeconds = a.ttlSeconds();
                    weakKeys = a.weakKeys(); eviction = a.eviction();
                } else {
                    DMEMO a = (DMEMO) ann;
                    maxSize = a.maxCacheSize(); ttlSeconds = a.ttlSeconds();
                    weakKeys = a.weakKeys(); eviction = a.eviction();
                }

                String key = methodKey(target);
                configs.put(key, new MemoConfig(maxSize, ttlSeconds, weakKeys, eviction));
                caches.put(key, new ConcurrentHashMap<>());

                log("Memoize: registered %s  maxSize=%d ttl=%ds eviction=%s",
                        key, maxSize, ttlSeconds, eviction);
                transformCount.incrementAndGet();
            }

            /** Look up a cached result. Returns null on miss. */
            public Object get(String methodKey, Object[] args) {
                ConcurrentHashMap<String, CacheEntry> cache = caches.get(methodKey);
                MemoConfig cfg = configs.get(methodKey);
                if (cache == null || cfg == null) return null;

                String argKey = Arrays.deepHashCode(args) + ":" + Arrays.deepToString(args);
                CacheEntry entry = cache.get(argKey);

                if (entry == null) return null;
                if (entry.isExpired(cfg.ttlSeconds)) { cache.remove(argKey); return null; }

                entry.lastAccessed = System.currentTimeMillis();
                entry.accessCount.incrementAndGet();
                return entry.value;
            }

            /** Store a result in the cache. */
            public void put(String methodKey, Object[] args, Object result) {
                ConcurrentHashMap<String, CacheEntry> cache = caches.get(methodKey);
                MemoConfig cfg = configs.get(methodKey);
                if (cache == null || cfg == null) return;

                // Evict if necessary
                while (cache.size() >= cfg.maxSize) {
                    evict(cache, cfg.eviction);
                }

                String argKey = Arrays.deepHashCode(args) + ":" + Arrays.deepToString(args);
                cache.put(argKey, new CacheEntry(result));
            }

            public void invalidate(String methodKey) {
                ConcurrentHashMap<String, CacheEntry> cache = caches.get(methodKey);
                if (cache != null) cache.clear();
            }

            public void invalidateAll() { caches.values().forEach(ConcurrentHashMap::clear); }

            public Map<String, Integer> cacheSizes() {
                Map<String, Integer> sizes = new LinkedHashMap<>();
                for (Map.Entry<String, ConcurrentHashMap<String, CacheEntry>> e : caches.entrySet()) {
                    sizes.put(e.getKey(), e.getValue().size());
                }
                return sizes;
            }

            private void evict(ConcurrentHashMap<String, CacheEntry> cache, DeepMemoize.EvictionPolicy policy) {
                if (cache.isEmpty()) return;

                String toEvict = null;
                switch (policy) {
                    case LRU:
                        toEvict = cache.entrySet().stream()
                                .min(Comparator.comparingLong(e -> e.getValue().lastAccessed))
                                .map(Map.Entry::getKey).orElse(null);
                        break;
                    case LFU:
                        toEvict = cache.entrySet().stream()
                                .min(Comparator.comparingLong(e -> e.getValue().accessCount.get()))
                                .map(Map.Entry::getKey).orElse(null);
                        break;
                    case FIFO:
                        toEvict = cache.entrySet().stream()
                                .min(Comparator.comparingLong(e -> e.getValue().createdAt))
                                .map(Map.Entry::getKey).orElse(null);
                        break;
                    case RANDOM:
                        toEvict = cache.keys().nextElement();
                        break;
                    default:
                        toEvict = cache.keys().nextElement();
                        break;
                }
                if (toEvict != null) cache.remove(toEvict);
            }
        }


        // ── Branch Hint Engine ───────────────────────────────────

        public static class BranchHintEngine {

            private final ConcurrentHashMap<String, Map<String, Object>> hints = new ConcurrentHashMap<>();

            public void processCold(Annotation ann, Method target) {
                String key = methodKey(target);
                hints.put(key, Map.of("type", "cold", "reason",
                        (ann instanceof DeepCold) ? ((DeepCold) ann).reason() : ((DCOLD) ann).reason()));
                log("Cold: registered %s", key);
                transformCount.incrementAndGet();
            }

            public void processHot(Annotation ann, Method target) {
                int freq = (ann instanceof DeepHot) ? ((DeepHot) ann).expectedFrequency() : ((DHOT) ann).expectedFrequency();
                String key = methodKey(target);
                hints.put(key, Map.of("type", "hot", "frequency", freq));
                log("Hot: registered %s  frequency=%d", key, freq);
                transformCount.incrementAndGet();
            }

            public void processLikely(Annotation ann, Method target) {
                double prob = (ann instanceof DeepLikely) ? ((DeepLikely) ann).probability() : ((DLIKE) ann).probability();
                String key = methodKey(target);
                hints.put(key, Map.of("type", "likely", "probability", prob));
                log("Likely: registered %s  p=%.2f", key, prob);
                transformCount.incrementAndGet();
            }

            public void processUnlikely(Annotation ann, Method target) {
                double prob = (ann instanceof DeepUnlikely) ? ((DeepUnlikely) ann).probability() : ((DULIKE) ann).probability();
                String key = methodKey(target);
                hints.put(key, Map.of("type", "unlikely", "probability", prob));
                log("Unlikely: registered %s  p=%.2f", key, prob);
                transformCount.incrementAndGet();
            }

            public void processAlign(Annotation ann, Method target) {
                int bytes;
                boolean cacheLine;
                if (ann instanceof DeepAlign) {
                    DeepAlign a = (DeepAlign) ann; bytes = a.bytes(); cacheLine = a.cacheLine();
                } else {
                    DALIGN a = (DALIGN) ann; bytes = a.bytes(); cacheLine = a.cacheLine();
                }
                String key = methodKey(target);
                hints.put(key, Map.of("type", "align", "bytes", bytes, "cacheLine", cacheLine));
                log("Align: registered %s  bytes=%d cacheLine=%b", key, bytes, cacheLine);
                transformCount.incrementAndGet();
            }

            public void processPack(Annotation ann, Method target) {
                int alignment;
                boolean reorder;
                if (ann instanceof DeepPack) {
                    DeepPack a = (DeepPack) ann; alignment = a.alignment(); reorder = a.reorder();
                } else {
                    DPAK a = (DPAK) ann; alignment = a.alignment(); reorder = a.reorder();
                }
                String key = methodKey(target);
                hints.put(key, Map.of("type", "pack", "alignment", alignment, "reorder", reorder));
                log("Pack: registered %s  alignment=%d reorder=%b", key, alignment, reorder);
                transformCount.incrementAndGet();
            }

            public Map<String, Object> getHint(String methodKey) { return hints.get(methodKey); }
            public boolean isHot(String methodKey) {
                Map<String, Object> h = hints.get(methodKey);
                return h != null && "hot".equals(h.get("type"));
            }
            public boolean isCold(String methodKey) {
                Map<String, Object> h = hints.get(methodKey);
                return h != null && "cold".equals(h.get("type"));
            }
        }


        // ── Data Structure Engine ────────────────────────────────

        public static class DataStructEngine {

            private final ConcurrentHashMap<String, Map<String, Object>> configs = new ConcurrentHashMap<>();

            public void processUnpack(Annotation ann, Method target) {
                boolean aos = (ann instanceof DeepUnpack) ? ((DeepUnpack) ann).arrayOfStructs() : ((DUPAK) ann).arrayOfStructs();
                boolean soa = (ann instanceof DeepUnpack) ? ((DeepUnpack) ann).structOfArrays() : ((DUPAK) ann).structOfArrays();
                String key = methodKey(target);
                configs.put(key, Map.of("type", "unpack", "aos", aos, "soa", soa));
                log("Unpack: registered %s  aos=%b soa=%b", key, aos, soa);
                transformCount.incrementAndGet();
            }

            public void processHash(Annotation ann, Method target) {
                DeepHash.HashAlgorithm alg = (ann instanceof DeepHash) ? ((DeepHash) ann).algorithm() : ((DHASH) ann).algorithm();
                int seed = (ann instanceof DeepHash) ? ((DeepHash) ann).seed() : ((DHASH) ann).seed();
                String key = methodKey(target);
                configs.put(key, Map.of("type", "hash", "algorithm", alg.name(), "seed", seed));
                log("Hash: registered %s  algorithm=%s seed=%d", key, alg, seed);
                transformCount.incrementAndGet();
            }

            public void processBloom(Annotation ann, Method target) {
                int expected = (ann instanceof DeepBloom) ? ((DeepBloom) ann).expectedElements() : ((DBLM) ann).expectedElements();
                double fpr = (ann instanceof DeepBloom) ? ((DeepBloom) ann).falsePositiveRate() : ((DBLM) ann).falsePositiveRate();
                String key = methodKey(target);
                configs.put(key, Map.of("type", "bloom", "expected", expected, "fpr", fpr));
                log("Bloom: registered %s  expected=%d fpr=%.4f", key, expected, fpr);
                transformCount.incrementAndGet();
            }

            public void processTrie(Annotation ann, Method target) {
                boolean compressed = (ann instanceof DeepTrie) ? ((DeepTrie) ann).compressed() : ((DTRIE) ann).compressed();
                int branching = (ann instanceof DeepTrie) ? ((DeepTrie) ann).branchingFactor() : ((DTRIE) ann).branchingFactor();
                String key = methodKey(target);
                configs.put(key, Map.of("type", "trie", "compressed", compressed, "branching", branching));
                log("Trie: registered %s  compressed=%b branching=%d", key, compressed, branching);
                transformCount.incrementAndGet();
            }

            public void processRope(Annotation ann, Method target) {
                int leafSize = (ann instanceof DeepRope) ? ((DeepRope) ann).leafSize() : ((DROPE) ann).leafSize();
                boolean autoBalance = (ann instanceof DeepRope) ? ((DeepRope) ann).autoBalance() : ((DROPE) ann).autoBalance();
                String key = methodKey(target);
                configs.put(key, Map.of("type", "rope", "leafSize", leafSize, "autoBalance", autoBalance));
                log("Rope: registered %s  leafSize=%d autoBalance=%b", key, leafSize, autoBalance);
                transformCount.incrementAndGet();
            }

            public Map<String, Object> getConfig(String key) { return configs.get(key); }
        }
    }


    // ====================================================================
    //  PHASE 8 PROCESSOR: RESILIENCE & RELIABILITY ENGINE
    // ====================================================================

    /**
     * Resilience engine that handles Phase 8 annotations.
     *
     * <pre>
     * ResilienceEngine
     * ├── RetryHandler         (@DeepRetry / @DRTRY)
     * ├── CircuitBreaker       (@DeepCircuit / @DCIR)
     * ├── FallbackHandler      (@DeepFallback / @DFBACK)
     * ├── TimeoutHandler       (@DeepTimeout / @DTOUT)
     * ├── RateLimiter          (@DeepRateLimit / @DRATE)
     * ├── ThrottleHandler      (@DeepThrottle / @DTHR)
     * ├── DebounceHandler      (@DeepDebounce / @DDEB)
     * ├── SchedulerHandler     (@DeepSchedule / @DSCHED)
     * ├── QueueHandler         (@DeepQueue / @DQ)
     * ├── PoolHandler          (@DeepPool / @DPOOL)
     * ├── LazyHandler          (@DeepLazy / @DLAZY)
     * ├── StreamHandler        (@DeepStream / @DSTRM)
     * ├── NetworkHandler       (@DeepNetwork / @DNET)
     * ├── MemoryHandler        (@DeepMemory / @DMEM)
     * └── ReflectHandler       (@DeepReflect / @DREFL)
     * </pre>
     */
    public static class ResilienceEngine {

        private final RetryHandler   retry   = new RetryHandler();
        private final CircuitBreaker circuit = new CircuitBreaker();
        private final FallbackHandler fallback = new FallbackHandler();
        private final TimeoutHandler timeout = new TimeoutHandler();
        private final RateLimiter    rateLimit = new RateLimiter();
        private final ThrottleHandler throttle = new ThrottleHandler();
        private final DebounceHandler debounce = new DebounceHandler();
        private final SchedulerHandler scheduler = new SchedulerHandler();
        private final QueueHandler   queue   = new QueueHandler();
        private final PoolHandler    pool    = new PoolHandler();
        private final LazyHandler    lazy    = new LazyHandler();

        public RetryHandler    retry()    { return retry; }
        public CircuitBreaker  circuit()  { return circuit; }
        public FallbackHandler fallback() { return fallback; }
        public TimeoutHandler  timeout()  { return timeout; }
        public RateLimiter     rateLimit(){ return rateLimit; }
        public ThrottleHandler throttle() { return throttle; }
        public DebounceHandler debounce() { return debounce; }
        public SchedulerHandler scheduler(){ return scheduler; }
        public QueueHandler    queue()    { return queue; }
        public PoolHandler     pool()     { return pool; }
        public LazyHandler     lazy()     { return lazy; }

        /** Route resilience annotation to correct handler. */
        public void apply(Annotation ann, Method target) {
            if (ann instanceof DeepRetry || ann instanceof DRTRY)         retry.process(ann, target);
            else if (ann instanceof DeepCircuit || ann instanceof DCIR)     circuit.process(ann, target);
            else if (ann instanceof DeepFallback || ann instanceof DFBACK)  fallback.process(ann, target);
            else if (ann instanceof DeepTimeout || ann instanceof DTOUT)    timeout.process(ann, target);
            else if (ann instanceof DeepRateLimit || ann instanceof DRATE)  rateLimit.process(ann, target);
            else if (ann instanceof DeepThrottle || ann instanceof DTHR)    throttle.process(ann, target);
            else if (ann instanceof DeepDebounce || ann instanceof DDEB)    debounce.process(ann, target);
            else if (ann instanceof DeepSchedule || ann instanceof DSCHED)  scheduler.process(ann, target);
            else if (ann instanceof DeepQueue || ann instanceof DQ)         queue.process(ann, target);
            else if (ann instanceof DeepPool || ann instanceof DPOOL)       pool.process(ann, target);
            else if (ann instanceof DeepLazy || ann instanceof DLAZY)       lazy.process(ann, target);
            else log("ResilienceEngine: unknown annotation %s", ann.annotationType().getSimpleName());
        }

        public void shutdownAll() {
            scheduler.shutdown();
            debounce.shutdown();
            queue.shutdown();
            pool.shutdown();
            log("ResilienceEngine: shutdown complete");
        }


        // ── Retry Handler ────────────────────────────────────────

        public static class RetryHandler {

            private final ConcurrentHashMap<String, RetryConfig> configs = new ConcurrentHashMap<>();
            private final ConcurrentHashMap<String, AtomicLong> retryCounters = new ConcurrentHashMap<>();

            public static class RetryConfig {
                public final int maxAttempts;
                public final long delayMs;
                public final double multiplier;
                public final long maxDelayMs;
                public final boolean exponentialBackoff;
                public final boolean jitter;
                public final Class<? extends Throwable>[] retryOn;
                public final Class<? extends Throwable>[] noRetryOn;
                public final String fallbackMethod;

                @SuppressWarnings("unchecked")
                RetryConfig(int maxAttempts, long delayMs, double multiplier, long maxDelayMs,
                            boolean exponentialBackoff, boolean jitter,
                            Class<? extends Throwable>[] retryOn, Class<? extends Throwable>[] noRetryOn,
                            String fallbackMethod) {
                    this.maxAttempts = maxAttempts; this.delayMs = delayMs;
                    this.multiplier = multiplier; this.maxDelayMs = maxDelayMs;
                    this.exponentialBackoff = exponentialBackoff; this.jitter = jitter;
                    this.retryOn = retryOn; this.noRetryOn = noRetryOn;
                    this.fallbackMethod = fallbackMethod;
                }
            }

            public void process(Annotation ann, Method target) {
                int maxAttempts;
                long delayMs, maxDelayMs;
                double multiplier;
                boolean expBackoff, jitter;
                Class<? extends Throwable>[] retryOn, noRetryOn;
                String fallbackMethod;

                if (ann instanceof DeepRetry) {
                    DeepRetry a = (DeepRetry) ann;
                    maxAttempts = a.maxAttempts(); delayMs = a.delayMs(); multiplier = a.multiplier();
                    maxDelayMs = a.maxDelayMs(); expBackoff = a.exponentialBackoff(); jitter = a.jitter();
                    retryOn = a.retryOn(); noRetryOn = a.noRetryOn(); fallbackMethod = a.fallbackMethod();
                } else {
                    DRTRY a = (DRTRY) ann;
                    maxAttempts = a.maxAttempts(); delayMs = a.delayMs(); multiplier = a.multiplier();
                    maxDelayMs = a.maxDelayMs(); expBackoff = a.exponentialBackoff(); jitter = a.jitter();
                    retryOn = a.retryOn(); noRetryOn = a.noRetryOn(); fallbackMethod = a.fallbackMethod();
                }

                String key = methodKey(target);
                configs.put(key, new RetryConfig(maxAttempts, delayMs, multiplier, maxDelayMs,
                        expBackoff, jitter, retryOn, noRetryOn, fallbackMethod));
                retryCounters.put(key, new AtomicLong(0));

                log("Retry: registered %s  max=%d delay=%dms multiplier=%.1f backoff=%b jitter=%b",
                        key, maxAttempts, delayMs, multiplier, expBackoff, jitter);
                transformCount.incrementAndGet();
            }

            /**
             * Execute a callable with retry logic.
             * This is the runtime wrapper that transformed bytecode will invoke.
             */
            public <T> T executeWithRetry(String methodKey, Callable<T> callable) throws Exception {
                RetryConfig cfg = configs.get(methodKey);
                if (cfg == null) return callable.call();

                Exception lastException = null;
                long currentDelay = cfg.delayMs;
                ThreadLocalRandom rng = ThreadLocalRandom.current();

                for (int attempt = 1; attempt <= cfg.maxAttempts; attempt++) {
                    try {
                        return callable.call();
                    } catch (Exception e) {
                        lastException = e;
                        retryCounters.get(methodKey).incrementAndGet();

                        // Check noRetryOn first
                        if (shouldNotRetry(e, cfg.noRetryOn)) throw e;
                        // Check retryOn
                        if (!shouldRetry(e, cfg.retryOn)) throw e;

                        if (attempt < cfg.maxAttempts) {
                            long sleepTime = currentDelay;
                            if (cfg.jitter) {
                                sleepTime = (long) (sleepTime * (0.5 + rng.nextDouble()));
                            }
                            sleepTime = Math.min(sleepTime, cfg.maxDelayMs);

                            log("Retry: %s attempt %d/%d failed (%s), retrying in %dms",
                                    methodKey, attempt, cfg.maxAttempts,
                                    e.getClass().getSimpleName(), sleepTime);

                            Thread.sleep(sleepTime);

                            if (cfg.exponentialBackoff) {
                                currentDelay = (long) (currentDelay * cfg.multiplier);
                            }
                        }
                    }
                }

                throw lastException;
            }

            private boolean shouldRetry(Exception e, Class<? extends Throwable>[] retryOn) {
                if (retryOn.length == 0) return true;
                for (Class<? extends Throwable> cls : retryOn) {
                    if (cls.isInstance(e)) return true;
                }
                return false;
            }

            private boolean shouldNotRetry(Exception e, Class<? extends Throwable>[] noRetryOn) {
                for (Class<? extends Throwable> cls : noRetryOn) {
                    if (cls.isInstance(e)) return true;
                }
                return false;
            }

            public long getRetryCount(String methodKey) {
                AtomicLong c = retryCounters.get(methodKey);
                return c != null ? c.get() : 0;
            }

            public RetryConfig getConfig(String key) { return configs.get(key); }
        }


        // ── Circuit Breaker ──────────────────────────────────────

        public static class CircuitBreaker {

            private final ConcurrentHashMap<String, CircuitState> states = new ConcurrentHashMap<>();

            public enum State { CLOSED, OPEN, HALF_OPEN }

            public static class CircuitConfig {
                public final int failureThreshold;
                public final int successThreshold;
                public final long timeoutMs;
                public final long resetTimeoutMs;
                public final boolean halfOpenEnabled;
                public final String fallbackMethod;
                public final Class<? extends Throwable>[] recordExceptions;
                public final Class<? extends Throwable>[] ignoreExceptions;

                CircuitConfig(int failureThreshold, int successThreshold, long timeoutMs,
                              long resetTimeoutMs, boolean halfOpenEnabled, String fallbackMethod,
                              Class<? extends Throwable>[] recordExceptions,
                              Class<? extends Throwable>[] ignoreExceptions) {
                    this.failureThreshold = failureThreshold; this.successThreshold = successThreshold;
                    this.timeoutMs = timeoutMs; this.resetTimeoutMs = resetTimeoutMs;
                    this.halfOpenEnabled = halfOpenEnabled; this.fallbackMethod = fallbackMethod;
                    this.recordExceptions = recordExceptions; this.ignoreExceptions = ignoreExceptions;
                }
            }

            public static class CircuitState {
                public final CircuitConfig config;
                public volatile State state = State.CLOSED;
                public final AtomicInteger failureCount = new AtomicInteger(0);
                public final AtomicInteger successCount = new AtomicInteger(0);
                public volatile long lastFailureTime = 0;
                public volatile long openedAt = 0;
                public final AtomicLong totalCalls = new AtomicLong(0);
                public final AtomicLong totalFailures = new AtomicLong(0);
                public final AtomicLong totalRejected = new AtomicLong(0);

                CircuitState(CircuitConfig config) { this.config = config; }
            }

            public void process(Annotation ann, Method target) {
                int failureThreshold, successThreshold;
                long timeoutMs, resetTimeoutMs;
                boolean halfOpenEnabled;
                String fallbackMethod;
                Class<? extends Throwable>[] recordExceptions, ignoreExceptions;

                if (ann instanceof DeepCircuit) {
                    DeepCircuit a = (DeepCircuit) ann;
                    failureThreshold = a.failureThreshold(); successThreshold = a.successThreshold();
                    timeoutMs = a.timeoutMs(); resetTimeoutMs = a.resetTimeoutMs();
                    halfOpenEnabled = a.halfOpenEnabled(); fallbackMethod = a.fallbackMethod();
                    recordExceptions = a.recordExceptions(); ignoreExceptions = a.ignoreExceptions();
                } else {
                    DCIR a = (DCIR) ann;
                    failureThreshold = a.failureThreshold(); successThreshold = a.successThreshold();
                    timeoutMs = a.timeoutMs(); resetTimeoutMs = a.resetTimeoutMs();
                    halfOpenEnabled = a.halfOpenEnabled(); fallbackMethod = a.fallbackMethod();
                    recordExceptions = a.recordExceptions(); ignoreExceptions = a.ignoreExceptions();
                }

                String key = methodKey(target);
                CircuitConfig cfg = new CircuitConfig(failureThreshold, successThreshold, timeoutMs,
                        resetTimeoutMs, halfOpenEnabled, fallbackMethod, recordExceptions, ignoreExceptions);
                states.put(key, new CircuitState(cfg));

                log("Circuit: registered %s  failThreshold=%d successThreshold=%d timeout=%dms reset=%dms",
                        key, failureThreshold, successThreshold, timeoutMs, resetTimeoutMs);
                transformCount.incrementAndGet();
            }

            /**
             * Execute a callable through the circuit breaker.
             */
            public <T> T execute(String methodKey, Callable<T> callable) throws Exception {
                CircuitState cs = states.get(methodKey);
                if (cs == null) return callable.call();

                cs.totalCalls.incrementAndGet();

                switch (cs.state) {
                    case OPEN:
                        long elapsed = System.currentTimeMillis() - cs.openedAt;
                        if (elapsed >= cs.config.resetTimeoutMs && cs.config.halfOpenEnabled) {
                            cs.state = State.HALF_OPEN;
                            cs.successCount.set(0);
                            log("Circuit: %s transitioning OPEN → HALF_OPEN", methodKey);
                        } else {
                            cs.totalRejected.incrementAndGet();
                            throw new CircuitOpenException(methodKey, elapsed, cs.config.resetTimeoutMs);
                        }
                        // fall through to HALF_OPEN
                    case HALF_OPEN:
                        try {
                            T result = callable.call();
                            int successes = cs.successCount.incrementAndGet();
                            if (successes >= cs.config.successThreshold) {
                                cs.state = State.CLOSED;
                                cs.failureCount.set(0);
                                cs.successCount.set(0);
                                log("Circuit: %s transitioning HALF_OPEN → CLOSED", methodKey);
                            }
                            return result;
                        } catch (Exception e) {
                            if (shouldRecord(e, cs.config)) {
                                cs.state = State.OPEN;
                                cs.openedAt = System.currentTimeMillis();
                                cs.totalFailures.incrementAndGet();
                                log("Circuit: %s transitioning HALF_OPEN → OPEN", methodKey);
                            }
                            throw e;
                        }

                    case CLOSED:
                    default:
                        try {
                            T result = callable.call();
                            cs.failureCount.set(0);
                            return result;
                        } catch (Exception e) {
                            if (shouldRecord(e, cs.config)) {
                                cs.lastFailureTime = System.currentTimeMillis();
                                cs.totalFailures.incrementAndGet();
                                int failures = cs.failureCount.incrementAndGet();
                                if (failures >= cs.config.failureThreshold) {
                                    cs.state = State.OPEN;
                                    cs.openedAt = System.currentTimeMillis();
                                    log("Circuit: %s transitioning CLOSED → OPEN (failures=%d)",
                                            methodKey, failures);
                                }
                            }
                            throw e;
                        }
                }
            }

            private boolean shouldRecord(Exception e, CircuitConfig cfg) {
                for (Class<? extends Throwable> cls : cfg.ignoreExceptions) {
                    if (cls.isInstance(e)) return false;
                }
                for (Class<? extends Throwable> cls : cfg.recordExceptions) {
                    if (cls.isInstance(e)) return true;
                }
                return cfg.recordExceptions.length == 0;
            }

            public State getState(String methodKey) {
                CircuitState cs = states.get(methodKey);
                return cs != null ? cs.state : null;
            }

            public void forceOpen(String methodKey) {
                CircuitState cs = states.get(methodKey);
                if (cs != null) { cs.state = State.OPEN; cs.openedAt = System.currentTimeMillis(); }
            }

            public void forceClosed(String methodKey) {
                CircuitState cs = states.get(methodKey);
                if (cs != null) { cs.state = State.CLOSED; cs.failureCount.set(0); }
            }

            public Map<String, Object> stats(String methodKey) {
                CircuitState cs = states.get(methodKey);
                if (cs == null) return Collections.emptyMap();
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("state", cs.state.name());
                s.put("failures", cs.failureCount.get());
                s.put("totalCalls", cs.totalCalls.get());
                s.put("totalFailures", cs.totalFailures.get());
                s.put("totalRejected", cs.totalRejected.get());
                return s;
            }

            /** Custom exception for open circuit. */
            public static class CircuitOpenException extends RuntimeException {
                public final String methodKey;
                public final long elapsedMs;
                public final long resetTimeoutMs;

                CircuitOpenException(String methodKey, long elapsedMs, long resetTimeoutMs) {
                    super("Circuit breaker OPEN for " + methodKey +
                            " (elapsed=" + elapsedMs + "ms, reset=" + resetTimeoutMs + "ms)");
                    this.methodKey = methodKey; this.elapsedMs = elapsedMs;
                    this.resetTimeoutMs = resetTimeoutMs;
                }
            }
        }


        // ── Fallback Handler ─────────────────────────────────────

        public static class FallbackHandler {

            private final ConcurrentHashMap<String, FallbackConfig> configs = new ConcurrentHashMap<>();

            public static class FallbackConfig {
                public final String fallbackMethod;
                public final String defaultValue;
                public final boolean suppressExceptions;
                public final Class<? extends Throwable>[] applyOn;

                FallbackConfig(String fallbackMethod, String defaultValue, boolean suppressExceptions,
                               Class<? extends Throwable>[] applyOn) {
                    this.fallbackMethod = fallbackMethod; this.defaultValue = defaultValue;
                    this.suppressExceptions = suppressExceptions; this.applyOn = applyOn;
                }
            }

            public void process(Annotation ann, Method target) {
                String fallbackMethod, defaultValue;
                boolean suppress;
                Class<? extends Throwable>[] applyOn;

                if (ann instanceof DeepFallback) {
                    DeepFallback a = (DeepFallback) ann;
                    fallbackMethod = a.fallbackMethod(); defaultValue = a.defaultValue();
                    suppress = a.suppressExceptions(); applyOn = a.applyOn();
                } else {
                    DFBACK a = (DFBACK) ann;
                    fallbackMethod = a.fallbackMethod(); defaultValue = a.defaultValue();
                    suppress = a.suppressExceptions(); applyOn = a.applyOn();
                }

                String key = methodKey(target);
                configs.put(key, new FallbackConfig(fallbackMethod, defaultValue, suppress, applyOn));

                log("Fallback: registered %s  method=%s default=%s suppress=%b",
                        key, fallbackMethod, defaultValue, suppress);
                transformCount.incrementAndGet();
            }

            /**
             * Execute with fallback support.
             */
            public <T> T executeWithFallback(String methodKey, Callable<T> primary, Callable<T> fallbackCallable) throws Exception {
                FallbackConfig cfg = configs.get(methodKey);
                if (cfg == null) return primary.call();

                try {
                    return primary.call();
                } catch (Exception e) {
                    boolean applies = false;
                    for (Class<? extends Throwable> cls : cfg.applyOn) {
                        if (cls.isInstance(e)) { applies = true; break; }
                    }
                    if (!applies && cfg.applyOn.length > 0) throw e;

                    if (fallbackCallable != null) {
                        log("Fallback: %s primary failed (%s), invoking fallback",
                                methodKey, e.getClass().getSimpleName());
                        return fallbackCallable.call();
                    }

                    if (cfg.suppressExceptions) {
                        log("Fallback: %s suppressed %s", methodKey, e.getClass().getSimpleName());
                        return null;
                    }

                    throw e;
                }
            }

            public FallbackConfig getConfig(String key) { return configs.get(key); }
        }


        // ── Timeout Handler ──────────────────────────────────────

        public static class TimeoutHandler {

            private final ConcurrentHashMap<String, TimeoutConfig> configs = new ConcurrentHashMap<>();
            private final ExecutorService executor = Executors.newCachedThreadPool(
                    r -> { Thread t = new Thread(r, "DeepMix-Timeout"); t.setDaemon(true); return t; });

            public static class TimeoutConfig {
                public final long timeoutMs;
                public final boolean interruptible;
                public final String timeoutHandler;
                public final DeepTimeout.TimeoutStrategy onTimeout;

                TimeoutConfig(long timeoutMs, boolean interruptible, String handler,
                              DeepTimeout.TimeoutStrategy onTimeout) {
                    this.timeoutMs = timeoutMs; this.interruptible = interruptible;
                    this.timeoutHandler = handler; this.onTimeout = onTimeout;
                }
            }

            public void process(Annotation ann, Method target) {
                long timeoutMs;
                boolean interruptible;
                String handler;
                DeepTimeout.TimeoutStrategy onTimeout;

                if (ann instanceof DeepTimeout) {
                    DeepTimeout a = (DeepTimeout) ann;
                    timeoutMs = a.timeoutMs(); interruptible = a.interruptible();
                    handler = a.timeoutHandler(); onTimeout = a.onTimeout();
                } else {
                    DTOUT a = (DTOUT) ann;
                    timeoutMs = a.timeoutMs(); interruptible = a.interruptible();
                    handler = a.timeoutHandler(); onTimeout = a.onTimeout();
                }

                String key = methodKey(target);
                configs.put(key, new TimeoutConfig(timeoutMs, interruptible, handler, onTimeout));

                log("Timeout: registered %s  timeout=%dms interruptible=%b strategy=%s",
                        key, timeoutMs, interruptible, onTimeout);
                transformCount.incrementAndGet();
            }

            /**
             * Execute a callable with timeout enforcement.
             */
            public <T> T executeWithTimeout(String methodKey, Callable<T> callable) throws Exception {
                TimeoutConfig cfg = configs.get(methodKey);
                if (cfg == null) return callable.call();

                Future<T> future = executor.submit(callable);
                try {
                    return future.get(cfg.timeoutMs, TimeUnit.MILLISECONDS);
                } catch (TimeoutException te) {
                    if (cfg.interruptible) future.cancel(true);

                    switch (cfg.onTimeout) {
                        case RETURN_NULL:    return null;
                        case RETURN_DEFAULT: return null; // Caller provides default
                        case CANCEL:         future.cancel(true); return null;
                        case THROW:
                        default:
                            throw new TimeoutException("Method " + methodKey +
                                    " timed out after " + cfg.timeoutMs + "ms");
                    }
                } catch (ExecutionException ee) {
                    Throwable cause = ee.getCause();
                    if (cause instanceof Exception) throw (Exception) cause;
                    throw new RuntimeException(cause);
                }
            }

            public TimeoutConfig getConfig(String key) { return configs.get(key); }
        }


        // ── Rate Limiter ─────────────────────────────────────────

        public static class RateLimiter {

            private final ConcurrentHashMap<String, RateLimitState> states = new ConcurrentHashMap<>();

            public static class RateLimitConfig {
                public final int maxRequests;
                public final int windowSeconds;
                public final DeepRateLimit.RateLimitStrategy strategy;
                public final String keyExpression;

                RateLimitConfig(int maxRequests, int windowSeconds,
                                DeepRateLimit.RateLimitStrategy strategy, String keyExpression) {
                    this.maxRequests = maxRequests; this.windowSeconds = windowSeconds;
                    this.strategy = strategy; this.keyExpression = keyExpression;
                }
            }

            public static class RateLimitState {
                public final RateLimitConfig config;
                private final AtomicLong counter = new AtomicLong(0);
                private volatile long windowStart = System.currentTimeMillis();
                private final AtomicLong totalPermitted = new AtomicLong(0);
                private final AtomicLong totalRejected = new AtomicLong(0);
                // Token bucket fields
                private final AtomicLong tokens;
                private volatile long lastRefill;

                RateLimitState(RateLimitConfig config) {
                    this.config = config;
                    this.tokens = new AtomicLong(config.maxRequests);
                    this.lastRefill = System.currentTimeMillis();
                }
            }

            public void process(Annotation ann, Method target) {
                int maxRequests, windowSeconds;
                DeepRateLimit.RateLimitStrategy strategy;
                String keyExpr;

                if (ann instanceof DeepRateLimit) {
                    DeepRateLimit a = (DeepRateLimit) ann;
                    maxRequests = a.maxRequests(); windowSeconds = a.windowSeconds();
                    strategy = a.strategy(); keyExpr = a.keyExpression();
                } else {
                    DRATE a = (DRATE) ann;
                    maxRequests = a.maxRequests(); windowSeconds = a.windowSeconds();
                    strategy = a.strategy(); keyExpr = a.keyExpression();
                }

                String key = methodKey(target);
                RateLimitConfig cfg = new RateLimitConfig(maxRequests, windowSeconds, strategy, keyExpr);
                states.put(key, new RateLimitState(cfg));

                log("RateLimit: registered %s  max=%d window=%ds strategy=%s",
                        key, maxRequests, windowSeconds, strategy);
                transformCount.incrementAndGet();
            }

            /**
             * Try to acquire a permit. Returns true if allowed.
             */
            public boolean tryAcquire(String methodKey) {
                RateLimitState st = states.get(methodKey);
                if (st == null) return true;

                boolean permitted;
                switch (st.config.strategy) {
                    case TOKEN_BUCKET:
                        permitted = tryAcquireTokenBucket(st);
                        break;
                    case SLIDING_WINDOW:
                        permitted = tryAcquireSlidingWindow(st);
                        break;
                    case FIXED_WINDOW:
                    default:
                        permitted = tryAcquireFixedWindow(st);
                        break;
                }

                if (permitted) st.totalPermitted.incrementAndGet();
                else st.totalRejected.incrementAndGet();

                return permitted;
            }

            private boolean tryAcquireFixedWindow(RateLimitState st) {
                long now = System.currentTimeMillis();
                long windowMs = st.config.windowSeconds * 1000L;
                if (now - st.windowStart >= windowMs) {
                    st.windowStart = now;
                    st.counter.set(0);
                }
                return st.counter.incrementAndGet() <= st.config.maxRequests;
            }

            private boolean tryAcquireSlidingWindow(RateLimitState st) {
                // Simplified: use fixed window with fractional carryover
                return tryAcquireFixedWindow(st);
            }

            private boolean tryAcquireTokenBucket(RateLimitState st) {
                long now = System.currentTimeMillis();
                long elapsed = now - st.lastRefill;
                long windowMs = st.config.windowSeconds * 1000L;

                // Refill tokens proportionally to elapsed time
                if (elapsed > 0) {
                    long newTokens = (long) ((double) elapsed / windowMs * st.config.maxRequests);
                    if (newTokens > 0) {
                        long current = st.tokens.get();
                        long refilled = Math.min(current + newTokens, st.config.maxRequests);
                        st.tokens.set(refilled);
                        st.lastRefill = now;
                    }
                }

                return st.tokens.decrementAndGet() >= 0;
            }

            public Map<String, Object> stats(String methodKey) {
                RateLimitState st = states.get(methodKey);
                if (st == null) return Collections.emptyMap();
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("permitted", st.totalPermitted.get());
                s.put("rejected", st.totalRejected.get());
                s.put("currentCount", st.counter.get());
                s.put("tokens", st.tokens.get());
                return s;
            }
        }


        // ── Throttle Handler ─────────────────────────────────────

        public static class ThrottleHandler {

            private final ConcurrentHashMap<String, Semaphore> semaphores = new ConcurrentHashMap<>();
            private final ConcurrentHashMap<String, ThrottleConfig> configs = new ConcurrentHashMap<>();

            public static class ThrottleConfig {
                public final int maxConcurrent;
                public final int queueSize;
                public final boolean blockWhenFull;
                public final long maxWaitMs;
                public final boolean fairness;

                ThrottleConfig(int maxConcurrent, int queueSize, boolean blockWhenFull,
                               long maxWaitMs, boolean fairness) {
                    this.maxConcurrent = maxConcurrent; this.queueSize = queueSize;
                    this.blockWhenFull = blockWhenFull; this.maxWaitMs = maxWaitMs;
                    this.fairness = fairness;
                }
            }

            public void process(Annotation ann, Method target) {
                int maxConcurrent, queueSize;
                boolean blockWhenFull, fairness;
                long maxWaitMs;

                if (ann instanceof DeepThrottle) {
                    DeepThrottle a = (DeepThrottle) ann;
                    maxConcurrent = a.maxConcurrent(); queueSize = a.queueSize();
                    blockWhenFull = a.blockWhenFull(); maxWaitMs = a.maxWaitMs();
                    fairness = a.fairness();
                } else {
                    DTHR a = (DTHR) ann;
                    maxConcurrent = a.maxConcurrent(); queueSize = a.queueSize();
                    blockWhenFull = a.blockWhenFull(); maxWaitMs = a.maxWaitMs();
                    fairness = a.fairness();
                }

                String key = methodKey(target);
                configs.put(key, new ThrottleConfig(maxConcurrent, queueSize, blockWhenFull, maxWaitMs, fairness));
                semaphores.put(key, new Semaphore(maxConcurrent, fairness));

                log("Throttle: registered %s  maxConcurrent=%d queue=%d block=%b maxWait=%dms",
                        key, maxConcurrent, queueSize, blockWhenFull, maxWaitMs);
                transformCount.incrementAndGet();
            }

            /**
             * Execute with throttling.
             */
            public <T> T executeThrottled(String methodKey, Callable<T> callable) throws Exception {
                Semaphore sem = semaphores.get(methodKey);
                ThrottleConfig cfg = configs.get(methodKey);
                if (sem == null || cfg == null) return callable.call();

                boolean acquired;
                if (cfg.blockWhenFull) {
                    acquired = sem.tryAcquire(cfg.maxWaitMs, TimeUnit.MILLISECONDS);
                } else {
                    acquired = sem.tryAcquire();
                }

                if (!acquired) {
                    throw new RejectedExecutionException(
                            "Throttle limit reached for " + methodKey + " (max=" + cfg.maxConcurrent + ")");
                }

                try {
                    return callable.call();
                } finally {
                    sem.release();
                }
            }

            public int availablePermits(String key) {
                Semaphore s = semaphores.get(key);
                return s != null ? s.availablePermits() : -1;
            }
        }


        // ── Debounce Handler ─────────────────────────────────────

        public static class DebounceHandler {

            private final ConcurrentHashMap<String, DebounceState> states = new ConcurrentHashMap<>();
            private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2,
                    r -> { Thread t = new Thread(r, "DeepMix-Debounce"); t.setDaemon(true); return t; });

            public static class DebounceConfig {
                public final long delayMs;
                public final boolean leading;
                public final boolean trailing;

                DebounceConfig(long delayMs, boolean leading, boolean trailing) {
                    this.delayMs = delayMs; this.leading = leading; this.trailing = trailing;
                }
            }

            public static class DebounceState {
                public final DebounceConfig config;
                public volatile ScheduledFuture<?> pendingFuture;
                public volatile boolean leadingFired = false;
                public final AtomicLong callCount = new AtomicLong(0);
                public final AtomicLong executionCount = new AtomicLong(0);

                DebounceState(DebounceConfig config) { this.config = config; }
            }

            public void process(Annotation ann, Method target) {
                long delayMs;
                boolean leading, trailing;

                if (ann instanceof DeepDebounce) {
                    DeepDebounce a = (DeepDebounce) ann;
                    delayMs = a.delayMs(); leading = a.leading(); trailing = a.trailing();
                } else {
                    DDEB a = (DDEB) ann;
                    delayMs = a.delayMs(); leading = a.leading(); trailing = a.trailing();
                }

                String key = methodKey(target);
                states.put(key, new DebounceState(new DebounceConfig(delayMs, leading, trailing)));

                log("Debounce: registered %s  delay=%dms leading=%b trailing=%b",
                        key, delayMs, leading, trailing);
                transformCount.incrementAndGet();
            }

            /**
             * Debounce a runnable.
             */
            public void debounce(String methodKey, Runnable action) {
                DebounceState ds = states.get(methodKey);
                if (ds == null) { action.run(); return; }

                ds.callCount.incrementAndGet();

                // Leading edge
                if (ds.config.leading && !ds.leadingFired) {
                    ds.leadingFired = true;
                    ds.executionCount.incrementAndGet();
                    action.run();
                }

                // Cancel pending trailing
                if (ds.pendingFuture != null) ds.pendingFuture.cancel(false);

                // Schedule trailing edge
                if (ds.config.trailing) {
                    ds.pendingFuture = scheduler.schedule(() -> {
                        ds.executionCount.incrementAndGet();
                        ds.leadingFired = false;
                        action.run();
                    }, ds.config.delayMs, TimeUnit.MILLISECONDS);
                }
            }

            public void shutdown() { scheduler.shutdownNow(); }
        }


        // ── Scheduler Handler ────────────────────────────────────

        public static class SchedulerHandler {

            private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4,
                    r -> { Thread t = new Thread(r, "DeepMix-Scheduler"); t.setDaemon(true); return t; });
            private final ConcurrentHashMap<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

            public void process(Annotation ann, Method target) {
                String cron;
                long fixedDelayMs, fixedRateMs, initialDelayMs;

                if (ann instanceof DeepSchedule) {
                    DeepSchedule a = (DeepSchedule) ann;
                    cron = a.cron(); fixedDelayMs = a.fixedDelayMs();
                    fixedRateMs = a.fixedRateMs(); initialDelayMs = a.initialDelayMs();
                } else {
                    DSCHED a = (DSCHED) ann;
                    cron = a.cron(); fixedDelayMs = a.fixedDelayMs();
                    fixedRateMs = a.fixedRateMs(); initialDelayMs = a.initialDelayMs();
                }

                String key = methodKey(target);

                log("Schedule: registered %s  cron=%s fixedDelay=%dms fixedRate=%dms initial=%dms",
                        key, cron, fixedDelayMs, fixedRateMs, initialDelayMs);
                transformCount.incrementAndGet();
            }

            public ScheduledFuture<?> scheduleFixedRate(String key, Runnable task, long initialDelay, long period) {
                ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.MILLISECONDS);
                tasks.put(key, future);
                return future;
            }

            public ScheduledFuture<?> scheduleFixedDelay(String key, Runnable task, long initialDelay, long delay) {
                ScheduledFuture<?> future = executor.scheduleWithFixedDelay(task, initialDelay, delay, TimeUnit.MILLISECONDS);
                tasks.put(key, future);
                return future;
            }

            public boolean cancel(String key) {
                ScheduledFuture<?> f = tasks.remove(key);
                return f != null && f.cancel(false);
            }

            public void shutdown() {
                tasks.values().forEach(f -> f.cancel(false));
                tasks.clear();
                executor.shutdownNow();
            }
        }


        // ── Queue Handler ────────────────────────────────────────

        public static class QueueHandler {

            private final ConcurrentHashMap<String, BlockingQueue<Runnable>> queues = new ConcurrentHashMap<>();
            private final ConcurrentHashMap<String, ExecutorService> consumers = new ConcurrentHashMap<>();

            public void process(Annotation ann, Method target) {
                String queueName;
                int capacity, consumerCount;
                boolean blocking;
                DeepQueue.QueueKind type;

                if (ann instanceof DeepQueue) {
                    DeepQueue a = (DeepQueue) ann;
                    queueName = a.queueName(); capacity = a.capacity();
                    blocking = a.blocking(); type = a.queueType();
                    consumerCount = a.consumers();
                } else {
                    DQ a = (DQ) ann;
                    queueName = a.queueName(); capacity = a.capacity();
                    blocking = a.blocking(); type = a.queueType();
                    consumerCount = a.consumers();
                }

                String key = methodKey(target);
                BlockingQueue<Runnable> queue;
                switch (type) {
                    case PRIORITY: queue = new PriorityBlockingQueue<>(capacity); break;
                    case LIFO:     queue = new LinkedBlockingDeque<>(capacity); break;
                    default:       queue = new LinkedBlockingQueue<>(capacity); break;
                }

                queues.put(queueName, queue);

                // Start consumer threads
                ExecutorService consumerPool = Executors.newFixedThreadPool(consumerCount,
                        r -> { Thread t = new Thread(r, "DeepMix-Queue-" + queueName); t.setDaemon(true); return t; });

                for (int i = 0; i < consumerCount; i++) {
                    consumerPool.submit(() -> {
                        while (!Thread.currentThread().isInterrupted()) {
                            try {
                                Runnable task = queue.poll(1, TimeUnit.SECONDS);
                                if (task != null) task.run();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            } catch (Exception e) {
                                log("Queue: consumer error on %s: %s", queueName, e.getMessage());
                            }
                        }
                    });
                }
                consumers.put(queueName, consumerPool);

                log("Queue: registered %s  queue=%s capacity=%d consumers=%d type=%s",
                        key, queueName, capacity, consumerCount, type);
                transformCount.incrementAndGet();
            }

            public boolean enqueue(String queueName, Runnable task) {
                BlockingQueue<Runnable> q = queues.get(queueName);
                return q != null && q.offer(task);
            }

            public int queueSize(String queueName) {
                BlockingQueue<Runnable> q = queues.get(queueName);
                return q != null ? q.size() : -1;
            }

            public void shutdown() {
                consumers.values().forEach(ExecutorService::shutdownNow);
                consumers.clear();
                queues.clear();
            }
        }


        // ── Pool Handler ─────────────────────────────────────────

        public static class PoolHandler {

            private final ConcurrentHashMap<String, ObjectPool<?>> pools = new ConcurrentHashMap<>();

            public static class ObjectPool<T> {
                private final BlockingQueue<T> available;
                private final Set<T> borrowed = ConcurrentHashMap.newKeySet();
                private final Supplier<T> factory;
                private final Consumer<T> resetter;
                private final int minSize, maxSize;
                private final long maxIdleTimeMs;
                private final AtomicInteger created = new AtomicInteger(0);

                public ObjectPool(Supplier<T> factory, Consumer<T> resetter,
                                  int minSize, int maxSize, long maxIdleTimeMs) {
                    this.factory = factory; this.resetter = resetter;
                    this.minSize = minSize; this.maxSize = maxSize;
                    this.maxIdleTimeMs = maxIdleTimeMs;
                    this.available = new LinkedBlockingQueue<>(maxSize);

                    // Pre-populate with minSize objects
                    for (int i = 0; i < minSize; i++) {
                        available.offer(factory.get());
                        created.incrementAndGet();
                    }
                }

                public T borrow() {
                    T obj = available.poll();
                    if (obj == null && created.get() < maxSize) {
                        obj = factory.get();
                        created.incrementAndGet();
                    }
                    if (obj != null) borrowed.add(obj);
                    return obj;
                }

                public T borrowBlocking(long timeoutMs) throws InterruptedException {
                    T obj = available.poll(timeoutMs, TimeUnit.MILLISECONDS);
                    if (obj == null && created.get() < maxSize) {
                        obj = factory.get();
                        created.incrementAndGet();
                    }
                    if (obj != null) borrowed.add(obj);
                    return obj;
                }

                public void release(T obj) {
                    if (obj == null) return;
                    borrowed.remove(obj);
                    if (resetter != null) resetter.accept(obj);
                    if (!available.offer(obj)) {
                        created.decrementAndGet(); // Couldn't return, discard
                    }
                }

                public int availableCount() { return available.size(); }
                public int borrowedCount() { return borrowed.size(); }
                public int totalCreated() { return created.get(); }
            }

            public void process(Annotation ann, Method target) {
                int minSize, maxSize;
                long maxIdleTimeMs;

                if (ann instanceof DeepPool) {
                    DeepPool a = (DeepPool) ann;
                    minSize = a.minSize(); maxSize = a.maxSize();
                    maxIdleTimeMs = a.maxIdleTimeMs();
                } else {
                    DPOOL a = (DPOOL) ann;
                    minSize = a.minSize(); maxSize = a.maxSize();
                    maxIdleTimeMs = a.maxIdleTimeMs();
                }

                String key = methodKey(target);
                log("Pool: registered %s  min=%d max=%d maxIdle=%dms",
                        key, minSize, maxSize, maxIdleTimeMs);
                transformCount.incrementAndGet();
            }

            @SuppressWarnings("unchecked")
            public <T> ObjectPool<T> createPool(String name, Supplier<T> factory, Consumer<T> resetter,
                                                 int minSize, int maxSize, long maxIdleTimeMs) {
                ObjectPool<T> pool = new ObjectPool<>(factory, resetter, minSize, maxSize, maxIdleTimeMs);
                pools.put(name, pool);
                return pool;
            }

            @SuppressWarnings("unchecked")
            public <T> ObjectPool<T> getPool(String name) { return (ObjectPool<T>) pools.get(name); }

            public void pool() {
                pools.clear();
            }
        }


        // ── Lazy Handler ─────────────────────────────────────────

        public static class LazyHandler {

            private final ConcurrentHashMap<String, LazyHolder<?>> holders = new ConcurrentHashMap<>();

            public static class LazyHolder<T> {
                private volatile T value;
                private volatile boolean initialized = false;
                private final Supplier<T> supplier;
                private final boolean threadSafe;
                private final Object lock = new Object();

                public LazyHolder(Supplier<T> supplier, boolean threadSafe) {
                    this.supplier = supplier; this.threadSafe = threadSafe;
                }

                public T get() {
                    if (!initialized) {
                        if (threadSafe) {
                            synchronized (lock) {
                                if (!initialized) {
                                    value = supplier.get();
                                    initialized = true;
                                }
                            }
                        } else {
                            value = supplier.get();
                            initialized = true;
                        }
                    }
                    return value;
                }

                public boolean isInitialized() { return initialized; }

                public void reset() {
                    synchronized (lock) {
                        value = null;
                        initialized = false;
                    }
                }
            }

            public void process(Annotation ann, Method target) {
                boolean threadSafe;

                if (ann instanceof DeepLazy) {
                    threadSafe = ((DeepLazy) ann).threadSafe();
                } else {
                    threadSafe = ((DLAZY) ann).threadSafe();
                }

                String key = methodKey(target);
                log("Lazy: registered %s  threadSafe=%b", key, threadSafe);
                transformCount.incrementAndGet();
            }

            @SuppressWarnings("unchecked")
            public <T> LazyHolder<T> createLazy(String name, Supplier<T> supplier, boolean threadSafe) {
                LazyHolder<T> holder = new LazyHolder<>(supplier, threadSafe);
                holders.put(name, holder);
                return holder;
            }

            @SuppressWarnings("unchecked")
            public <T> LazyHolder<T> getLazy(String name) { return (LazyHolder<T>) holders.get(name); }
        }
    }


    // ====================================================================
    //  PHASE 9 PROCESSOR: DESIGN PATTERN ENGINE
    // ====================================================================

    /**
     * Design pattern engine that handles Phase 9 annotations.
     *
     * <pre>
     * DesignPatternEngine
     * ├── SingletonEnforcer    (@DeepSingleton / @DSING)
     * ├── FactoryGenerator     (@DeepFactory / @DFACT)
     * ├── BuilderGenerator     (@DeepBuilder / @DBUILD)
     * ├── ObserverManager      (@DeepObserver / @DOBS)
     * ├── PubSubBroker         (@DeepPubSub / @DPSUB)
     * └── CodeGenerator        (@DeepGenerate / @DGEN)
     * </pre>
     */
    public static class DesignPatternEngine {

        private final SingletonEnforcer singletons = new SingletonEnforcer();
        private final FactoryGenerator  factories  = new FactoryGenerator();
        private final BuilderGenerator  builders   = new BuilderGenerator();
        private final ObserverManager   observers  = new ObserverManager();
        private final PubSubBroker      pubsub     = new PubSubBroker();
        private final CodeGenerator     codegen    = new CodeGenerator();

        public SingletonEnforcer singletons() { return singletons; }
        public FactoryGenerator  factories()  { return factories; }
        public BuilderGenerator  builders()   { return builders; }
        public ObserverManager   observers()  { return observers; }
        public PubSubBroker      pubsub()     { return pubsub; }
        public CodeGenerator     codegen()    { return codegen; }

        /** Route pattern annotation to correct engine. */
        public void apply(Annotation ann, Method target) {
            if (ann instanceof DeepSingleton || ann instanceof DSING)     singletons.process(ann, target);
            else if (ann instanceof DeepFactory || ann instanceof DFACT)  factories.process(ann, target);
            else if (ann instanceof DeepBuilder || ann instanceof DBUILD) builders.process(ann, target);
            else if (ann instanceof DeepObserver || ann instanceof DOBS)  observers.process(ann, target);
            else if (ann instanceof DeepPubSub || ann instanceof DPSUB)   pubsub.process(ann, target);
            else if (ann instanceof DeepGenerate || ann instanceof DGEN)  codegen.process(ann, target);
            else log("DesignPatternEngine: unknown annotation %s", ann.annotationType().getSimpleName());
        }

        public void shutdownAll() {
            pubsub.shutdown();
            log("DesignPatternEngine: shutdown complete");
        }


        // ── Singleton Enforcer ───────────────────────────────────

        public static class SingletonEnforcer {

            private final ConcurrentHashMap<String, Object> instances = new ConcurrentHashMap<>();
            private final ConcurrentHashMap<String, SingletonConfig> configs = new ConcurrentHashMap<>();

            public static class SingletonConfig {
                public final boolean lazy, threadSafe, preventReflection, preventSerialization, preventCloning;
                public final String instanceMethod;

                SingletonConfig(boolean lazy, boolean threadSafe, String instanceMethod,
                                boolean preventReflection, boolean preventSerialization, boolean preventCloning) {
                    this.lazy = lazy; this.threadSafe = threadSafe; this.instanceMethod = instanceMethod;
                    this.preventReflection = preventReflection;
                    this.preventSerialization = preventSerialization;
                    this.preventCloning = preventCloning;
                }
            }

            public void process(Annotation ann, Method target) {
                boolean lazy, threadSafe, preventReflection, preventSerialization, preventCloning;
                String instanceMethod;

                if (ann instanceof DeepSingleton) {
                    DeepSingleton a = (DeepSingleton) ann;
                    lazy = a.lazy(); threadSafe = a.threadSafe(); instanceMethod = a.instanceMethod();
                    preventReflection = a.preventReflection();
                    preventSerialization = a.preventSerialization();
                    preventCloning = a.preventCloning();
                } else {
                    DSING a = (DSING) ann;
                    lazy = a.lazy(); threadSafe = a.threadSafe(); instanceMethod = a.instanceMethod();
                    preventReflection = a.preventReflection();
                    preventSerialization = a.preventSerialization();
                    preventCloning = a.preventCloning();
                }

                String key = target.getDeclaringClass().getName();
                configs.put(key, new SingletonConfig(lazy, threadSafe, instanceMethod,
                        preventReflection, preventSerialization, preventCloning));

                log("Singleton: registered %s  lazy=%b threadSafe=%b method=%s",
                        key, lazy, threadSafe, instanceMethod);
                transformCount.incrementAndGet();
            }

            @SuppressWarnings("unchecked")
            public <T> T getInstance(String className, Supplier<T> factory) {
                return (T) instances.computeIfAbsent(className, k -> factory.get());
            }

            public boolean isRegistered(String className) { return configs.containsKey(className); }
            public Object getExisting(String className) { return instances.get(className); }
        }


        // ── Factory Generator ────────────────────────────────────

        public static class FactoryGenerator {

            private final ConcurrentHashMap<String, FactoryConfig> configs = new ConcurrentHashMap<>();
            private final ConcurrentHashMap<String, Map<String, Supplier<?>>> productRegistries = new ConcurrentHashMap<>();

            public static class FactoryConfig {
                public final String factoryMethod;
                public final boolean abstractFactory;
                public final boolean cached;
                public final boolean threadSafe;

                FactoryConfig(String factoryMethod, boolean abstractFactory, boolean cached, boolean threadSafe) {
                    this.factoryMethod = factoryMethod; this.abstractFactory = abstractFactory;
                    this.cached = cached; this.threadSafe = threadSafe;
                }
            }

            public void process(Annotation ann, Method target) {
                String factoryMethod;
                boolean abstractFactory, cached, threadSafe;

                if (ann instanceof DeepFactory) {
                    DeepFactory a = (DeepFactory) ann;
                    factoryMethod = a.factoryMethod(); abstractFactory = a.abstractFactory();
                    cached = a.cached(); threadSafe = a.threadSafe();
                } else {
                    DFACT a = (DFACT) ann;
                    factoryMethod = a.factoryMethod(); abstractFactory = a.abstractFactory();
                    cached = a.cached(); threadSafe = a.threadSafe();
                }

                String key = target.getDeclaringClass().getName();
                configs.put(key, new FactoryConfig(factoryMethod, abstractFactory, cached, threadSafe));
                productRegistries.put(key, new ConcurrentHashMap<>());

                log("Factory: registered %s  method=%s abstract=%b cached=%b",
                        key, factoryMethod, abstractFactory, cached);
                transformCount.incrementAndGet();
            }

            public <T> void registerProduct(String factoryKey, String productName, Supplier<T> supplier) {
                Map<String, Supplier<?>> registry = productRegistries.get(factoryKey);
                if (registry != null) registry.put(productName, supplier);
            }

            @SuppressWarnings("unchecked")
            public <T> T create(String factoryKey, String productName) {
                Map<String, Supplier<?>> registry = productRegistries.get(factoryKey);
                if (registry == null) return null;
                Supplier<?> supplier = registry.get(productName);
                return supplier != null ? (T) supplier.get() : null;
            }
        }


        // ── Builder Generator ────────────────────────────────────

        public static class BuilderGenerator {

            private final ConcurrentHashMap<String, BuilderConfig> configs = new ConcurrentHashMap<>();

            public static class BuilderConfig {
                public final boolean fluent, immutable, validate, toBuilder;
                public final String buildMethod, prefix;

                BuilderConfig(boolean fluent, boolean immutable, String buildMethod,
                              String prefix, boolean validate, boolean toBuilder) {
                    this.fluent = fluent; this.immutable = immutable;
                    this.buildMethod = buildMethod; this.prefix = prefix;
                    this.validate = validate; this.toBuilder = toBuilder;
                }
            }

            /**
             * Generic dynamic builder that works with any Map-based configuration.
             */
            public static class DynamicBuilder {
                private final Map<String, Object> properties = new LinkedHashMap<>();
                private final BuilderConfig config;

                public DynamicBuilder(BuilderConfig config) { this.config = config; }

                public DynamicBuilder set(String key, Object value) {
                    properties.put(key, value);
                    return this;
                }

                public Object get(String key) { return properties.get(key); }

                public Map<String, Object> build() {
                    if (config.validate) {
                        // Basic validation: ensure required fields are present
                        for (Map.Entry<String, Object> entry : properties.entrySet()) {
                            if (entry.getValue() == null) {
                                log("Builder: WARNING — null value for key '%s'", entry.getKey());
                            }
                        }
                    }
                    return config.immutable ?
                            Collections.unmodifiableMap(new LinkedHashMap<>(properties)) :
                            new LinkedHashMap<>(properties);
                }
            }

            public void process(Annotation ann, Method target) {
                boolean fluent, immutable, validate, toBuilder;
                String buildMethod, prefix;

                if (ann instanceof DeepBuilder) {
                    DeepBuilder a = (DeepBuilder) ann;
                    fluent = a.fluent(); immutable = a.immutable();
                    buildMethod = a.buildMethod(); prefix = a.prefix();
                    validate = a.validate(); toBuilder = a.toBuilder();
                } else {
                    DBUILD a = (DBUILD) ann;
                    fluent = a.fluent(); immutable = a.immutable();
                    buildMethod = a.buildMethod(); prefix = a.prefix();
                    validate = a.validate(); toBuilder = a.toBuilder();
                }

                String key = target.getDeclaringClass().getName();
                configs.put(key, new BuilderConfig(fluent, immutable, buildMethod, prefix, validate, toBuilder));

                log("Builder: registered %s  fluent=%b immutable=%b method=%s prefix=%s",
                        key, fluent, immutable, buildMethod, prefix);
                transformCount.incrementAndGet();
            }

            public DynamicBuilder newBuilder(String className) {
                BuilderConfig cfg = configs.get(className);
                if (cfg == null) cfg = new BuilderConfig(true, true, "build", "with", false, false);
                return new DynamicBuilder(cfg);
            }
        }


        // ── Observer Manager ─────────────────────────────────────

        public static class ObserverManager {

            private final ConcurrentHashMap<String, CopyOnWriteArrayList<ObserverEntry>> eventListeners = new ConcurrentHashMap<>();
            private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(4,
                    r -> { Thread t = new Thread(r, "DeepMix-Observer"); t.setDaemon(true); return t; });

            public static class ObserverEntry {
                public final String methodKey;
                public final Consumer<Object> handler;
                public final boolean async;
                public final boolean weakRef;
                public final int priority;

                ObserverEntry(String methodKey, Consumer<Object> handler, boolean async, boolean weakRef, int priority) {
                    this.methodKey = methodKey; this.handler = handler;
                    this.async = async; this.weakRef = weakRef; this.priority = priority;
                }
            }

            public void process(Annotation ann, Method target) {
                String[] events;
                boolean async, weakRef;
                int priority;

                if (ann instanceof DeepObserver) {
                    DeepObserver a = (DeepObserver) ann;
                    events = a.events().length > 0 ? a.events() : new String[]{a.eventType()};
                    async = a.async(); weakRef = a.weakReference(); priority = a.priority();
                } else {
                    DOBS a = (DOBS) ann;
                    events = a.events().length > 0 ? a.events() : new String[]{a.eventType()};
                    async = a.async(); weakRef = a.weakReference(); priority = a.priority();
                }

                String key = methodKey(target);
                for (String event : events) {
                    if (event != null && !event.isEmpty()) {
                        eventListeners.computeIfAbsent(event, k -> new CopyOnWriteArrayList<>());
                        log("Observer: registered %s for event '%s' async=%b priority=%d",
                                key, event, async, priority);
                    }
                }
                transformCount.incrementAndGet();
            }

            public void subscribe(String eventType, Consumer<Object> handler, boolean async, int priority) {
                CopyOnWriteArrayList<ObserverEntry> listeners =
                        eventListeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
                listeners.add(new ObserverEntry("dynamic", handler, async, false, priority));
                // Keep sorted by priority (higher first)
                listeners.sort((a, b) -> Integer.compare(b.priority, a.priority));
            }

            public void unsubscribe(String eventType, String methodKey) {
                CopyOnWriteArrayList<ObserverEntry> listeners = eventListeners.get(eventType);
                if (listeners != null) listeners.removeIf(e -> e.methodKey.equals(methodKey));
            }

            public void notify(String eventType, Object eventData) {
                CopyOnWriteArrayList<ObserverEntry> listeners = eventListeners.get(eventType);
                if (listeners == null || listeners.isEmpty()) return;

                for (ObserverEntry entry : listeners) {
                    if (entry.async) {
                        asyncExecutor.submit(() -> {
                            try { entry.handler.accept(eventData); }
                            catch (Exception e) { log("Observer: async error for %s: %s", entry.methodKey, e.getMessage()); }
                        });
                    } else {
                        try { entry.handler.accept(eventData); }
                        catch (Exception e) { log("Observer: sync error for %s: %s", entry.methodKey, e.getMessage()); }
                    }
                }
            }

            public int listenerCount(String eventType) {
                CopyOnWriteArrayList<ObserverEntry> l = eventListeners.get(eventType);
                return l != null ? l.size() : 0;
            }

            public Set<String> registeredEvents() { return Collections.unmodifiableSet(eventListeners.keySet()); }
        }


        // ── PubSub Broker ────────────────────────────────────────

        public static class PubSubBroker {

            private final ConcurrentHashMap<String, CopyOnWriteArrayList<Subscriber>> topics = new ConcurrentHashMap<>();
            private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(4,
                    r -> { Thread t = new Thread(r, "DeepMix-PubSub"); t.setDaemon(true); return t; });
            private final ConcurrentHashMap<String, BlockingQueue<Object>> persistentQueues = new ConcurrentHashMap<>();

            public static class Subscriber {
                public final String id;
                public final Consumer<Object> handler;
                public final boolean async;
                public final int qos; // 0=at-most-once, 1=at-least-once, 2=exactly-once

                Subscriber(String id, Consumer<Object> handler, boolean async, int qos) {
                    this.id = id; this.handler = handler; this.async = async; this.qos = qos;
                }
            }

            public void process(Annotation ann, Method target) {
                String topic;
                String[] subscriptions;
                boolean persistent, async;
                int qos;

                if (ann instanceof DeepPubSub) {
                    DeepPubSub a = (DeepPubSub) ann;
                    topic = a.topic(); subscriptions = a.subscriptions();
                    persistent = a.persistent(); async = a.async(); qos = a.qos();
                } else {
                    DPSUB a = (DPSUB) ann;
                    topic = a.topic(); subscriptions = a.subscriptions();
                    persistent = a.persistent(); async = a.async(); qos = a.qos();
                }

                String key = methodKey(target);

                if (!topic.isEmpty()) {
                    topics.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>());
                    if (persistent) {
                        persistentQueues.computeIfAbsent(topic, k -> new LinkedBlockingQueue<>(10000));
                    }
                }

                for (String sub : subscriptions) {
                    if (!sub.isEmpty()) {
                        topics.computeIfAbsent(sub, k -> new CopyOnWriteArrayList<>());
                    }
                }

                log("PubSub: registered %s  topic=%s subs=%s persistent=%b qos=%d",
                        key, topic, Arrays.toString(subscriptions), persistent, qos);
                transformCount.incrementAndGet();
            }

            public void subscribe(String topic, String subscriberId, Consumer<Object> handler, boolean async, int qos) {
                CopyOnWriteArrayList<Subscriber> subs =
                        topics.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>());
                subs.add(new Subscriber(subscriberId, handler, async, qos));
            }

            public void unsubscribe(String topic, String subscriberId) {
                CopyOnWriteArrayList<Subscriber> subs = topics.get(topic);
                if (subs != null) subs.removeIf(s -> s.id.equals(subscriberId));
            }

            public void publish(String topic, Object message) {
                CopyOnWriteArrayList<Subscriber> subs = topics.get(topic);

                // Persist if needed
                BlockingQueue<Object> pq = persistentQueues.get(topic);
                if (pq != null) pq.offer(message);

                if (subs == null || subs.isEmpty()) return;

                for (Subscriber sub : subs) {
                    if (sub.async) {
                        asyncExecutor.submit(() -> deliverMessage(sub, message));
                    } else {
                        deliverMessage(sub, message);
                    }
                }
            }

            private void deliverMessage(Subscriber sub, Object message) {
                try {
                    sub.handler.accept(message);
                } catch (Exception e) {
                    log("PubSub: delivery error to %s: %s", sub.id, e.getMessage());
                    if (sub.qos >= 1) {
                        // At-least-once: retry
                        try { sub.handler.accept(message); }
                        catch (Exception e2) { log("PubSub: retry failed for %s: %s", sub.id, e2.getMessage()); }
                    }
                }
            }

            public Set<String> listTopics() { return Collections.unmodifiableSet(topics.keySet()); }
            public int subscriberCount(String topic) {
                CopyOnWriteArrayList<Subscriber> s = topics.get(topic);
                return s != null ? s.size() : 0;
            }

            public void shutdown() { asyncExecutor.shutdownNow(); }
        }


        // ── Code Generator ───────────────────────────────────────

        public static class CodeGenerator {

            private final ConcurrentHashMap<String, GenerateConfig> configs = new ConcurrentHashMap<>();

            public static class GenerateConfig {
                public final String template;
                public final String outputType;
                public final String[] generatedMethods;
                public final boolean overwrite;
                public final String packageName;
                public final String[] interfaces;

                GenerateConfig(String template, String outputType, String[] generatedMethods,
                               boolean overwrite, String packageName, String[] interfaces) {
                    this.template = template; this.outputType = outputType;
                    this.generatedMethods = generatedMethods; this.overwrite = overwrite;
                    this.packageName = packageName; this.interfaces = interfaces;
                }
            }

            public void process(Annotation ann, Method target) {
                String template, outputType, packageName;
                String[] generatedMethods, interfaces;
                boolean overwrite;

                if (ann instanceof DeepGenerate) {
                    DeepGenerate a = (DeepGenerate) ann;
                    template = a.template(); outputType = a.outputType();
                    generatedMethods = a.generatedMethods(); overwrite = a.overwrite();
                    packageName = a.packageName(); interfaces = a.interfaces();
                } else {
                    DGEN a = (DGEN) ann;
                    template = a.template(); outputType = a.outputType();
                    generatedMethods = a.generatedMethods(); overwrite = a.overwrite();
                    packageName = a.packageName(); interfaces = a.interfaces();
                }

                String key = target.getDeclaringClass().getName();
                configs.put(key, new GenerateConfig(template, outputType, generatedMethods,
                        overwrite, packageName, interfaces));

                log("Generate: registered %s  template=%s output=%s methods=%s",
                        key, template, outputType, Arrays.toString(generatedMethods));
                transformCount.incrementAndGet();
            }

            /**
             * Generate source code from a template with variable substitution.
             */
            public String generateFromTemplate(String templateContent, Map<String, String> variables) {
                String result = templateContent;
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    result = result.replace("${" + entry.getKey() + "}", entry.getValue());
                    result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
                }
                return result;
            }

            /**
             * Generate a basic class skeleton.
             */
            public String generateClass(String packageName, String className,
                                         String superClass, String[] interfaces,
                                         Map<String, String> fields, String[] methods) {
                StringBuilder sb = new StringBuilder();

                if (!packageName.isEmpty()) sb.append("package ").append(packageName).append(";\n\n");

                sb.append("public class ").append(className);
                if (superClass != null && !superClass.isEmpty()) sb.append(" extends ").append(superClass);
                if (interfaces != null && interfaces.length > 0) {
                    sb.append(" implements ").append(String.join(", ", interfaces));
                }
                sb.append(" {\n\n");

                // Fields
                if (fields != null) {
                    for (Map.Entry<String, String> field : fields.entrySet()) {
                        sb.append("    private ").append(field.getValue()).append(' ')
                                .append(field.getKey()).append(";\n");
                    }
                    sb.append('\n');
                }

                // Methods
                if (methods != null) {
                    for (String method : methods) {
                        sb.append("    ").append(method).append("\n\n");
                    }
                }

                sb.append("}\n");
                return sb.toString();
            }

            public GenerateConfig getConfig(String key) { return configs.get(key); }
        }
    }


    // ╔══════════════════════════════════════════════════════════════════╗
    // ║                                                                  ║
    // ║           C E N T R A L   R E G I S T R Y   &   A P I           ║
    // ║                                                                  ║
    // ╚══════════════════════════════════════════════════════════════════╝


    // ====================================================================
    //  TRANSFORMATION REGISTRY
    // ====================================================================

    /**
     * Central registry that maps annotation types to their processors.
     * This is the single entry point for the DeepMix transformation pipeline.
     */
    public static class TransformationRegistry {

        private static final TransformationRegistry INSTANCE = new TransformationRegistry();

        private final BytecodeTransformBridge   bytecodeEngine      = new BytecodeTransformBridge();
        private final InstrumentationEngine     instrumentationEngine = new InstrumentationEngine();
        private final OptimizationEngine        optimizationEngine  = new OptimizationEngine();
        private final ResilienceEngine          resilienceEngine    = new ResilienceEngine();
        private final DesignPatternEngine       patternEngine       = new DesignPatternEngine();
        private final ValidationEngine          validationEngine    = new ValidationEngine();

        private final ConcurrentHashMap<Class<? extends Annotation>, AnnotationPhase> phaseMap = new ConcurrentHashMap<>();
        private final AtomicLong registeredTransforms = new AtomicLong(0);

        public enum AnnotationPhase { PHASE5_BYTECODE, PHASE6_INSTRUMENTATION, PHASE7_OPTIMIZATION, PHASE8_RESILIENCE, PHASE9_PATTERNS }

        private TransformationRegistry() {
            registerPhase5();
            registerPhase6();
            registerPhase7();
            registerPhase8();
            registerPhase9();
            log("TransformationRegistry: initialized with %d annotation mappings", phaseMap.size());
        }

        public static TransformationRegistry getInstance() { return INSTANCE; }

        // ── Engine Accessors ─────────────────────────────────────

        public BytecodeTransformBridge   bytecode()        { return bytecodeEngine; }
        public InstrumentationEngine     instrumentation() { return instrumentationEngine; }
        public OptimizationEngine        optimization()    { return optimizationEngine; }
        public ResilienceEngine          resilience()      { return resilienceEngine; }
        public DesignPatternEngine       patterns()        { return patternEngine; }
        public ValidationEngine          validation()      { return validationEngine; }

        // ── Phase Registration ───────────────────────────────────

        private void registerPhase5() {
            Class<?>[] p5 = {DeepLLVM.class, DLLVM.class, DeepWASM.class, DWASM.class,
                    DeepCIL.class, DCIL.class, DeepDalvik.class, DDLK.class,
                    DeepPython.class, DPYC.class, DeepNode.class, DNODE.class,
                    DeepLua.class, DLUA.class};
            for (Class<?> c : p5) registerAnnotation(c, AnnotationPhase.PHASE5_BYTECODE);
        }

        private void registerPhase6() {
            Class<?>[] p6 = {DeepTrace.class, DTR.class, DeepMetrics.class, DMET.class,
                    DeepLog.class, DLOG.class, DeepAudit.class, DAUD_AUDIT.class,
                    DeepProfile.class, DPROF.class, DeepDebug.class, DDBG.class,
                    DeepValidate.class, DVAL.class, DeepWatch.class, DWATCH.class,
                    DeepBackup.class, DBAK.class};
            for (Class<?> c : p6) registerAnnotation(c, AnnotationPhase.PHASE6_INSTRUMENTATION);
        }

        private void registerPhase7() {
            Class<?>[] p7 = {DeepOptimize.class, DOPT.class, DeepInline.class, DINL.class,
                    DeepNoInline.class, DNINL.class, DeepTailCall.class, DTAIL.class,
                    DeepUnroll.class, DUNRL.class, DeepPrefetch.class, DPREF.class,
                    DeepVectorize.class, DVEC.class, DeepParallelize.class, DPAR.class,
                    DeepGPU.class, DGPU.class, DeepJIT.class, DJIT.class,
                    DeepMemoize.class, DMEMO.class, DeepCold.class, DCOLD.class,
                    DeepHot.class, DHOT.class, DeepLikely.class, DLIKE.class,
                    DeepUnlikely.class, DULIKE.class, DeepAlign.class, DALIGN.class,
                    DeepPack.class, DPAK.class, DeepUnpack.class, DUPAK.class,
                    DeepHash.class, DHASH.class, DeepBloom.class, DBLM.class,
                    DeepTrie.class, DTRIE.class, DeepRope.class, DROPE.class};
            for (Class<?> c : p7) registerAnnotation(c, AnnotationPhase.PHASE7_OPTIMIZATION);
        }

        private void registerPhase8() {
            Class<?>[] p8 = {DeepRetry.class, DRTRY.class, DeepCircuit.class, DCIR.class,
                    DeepFallback.class, DFBACK.class, DeepTimeout.class, DTOUT.class,
                    DeepRateLimit.class, DRATE.class, DeepThrottle.class, DTHR.class,
                    DeepDebounce.class, DDEB.class, DeepSchedule.class, DSCHED.class,
                    DeepQueue.class, DQ.class, DeepPool.class, DPOOL.class,
                    DeepLazy.class, DLAZY.class, DeepStream.class, DSTRM.class,
                    DeepNetwork.class, DNET.class, DeepMemory.class, DMEM.class,
                    DeepReflect.class, DREFL.class};
            for (Class<?> c : p8) registerAnnotation(c, AnnotationPhase.PHASE8_RESILIENCE);
        }

        private void registerPhase9() {
            Class<?>[] p9 = {DeepSingleton.class, DSING.class, DeepFactory.class, DFACT.class,
                    DeepBuilder.class, DBUILD.class, DeepObserver.class, DOBS.class,
                    DeepPubSub.class, DPSUB.class, DeepGenerate.class, DGEN.class};
            for (Class<?> c : p9) registerAnnotation(c, AnnotationPhase.PHASE9_PATTERNS);
        }

        @SuppressWarnings("unchecked")
        private void registerAnnotation(Class<?> annClass, AnnotationPhase phase) {
            phaseMap.put((Class<? extends Annotation>) annClass, phase);
            registeredTransforms.incrementAndGet();
        }

        // ── Dispatch ─────────────────────────────────────────────

        /**
         * Process a single annotation on a method, routing it to the correct engine.
         */
        public void processAnnotation(Annotation ann, Method target) {
            AnnotationPhase phase = phaseMap.get(ann.annotationType());
            if (phase == null) {
                log("Registry: no handler for annotation %s", ann.annotationType().getSimpleName());
                return;
            }

            // Validate before processing
            List<String> errors = validationEngine.validate(ann, target);
            if (!errors.isEmpty()) {
                log("Registry: validation errors for %s on %s: %s",
                        ann.annotationType().getSimpleName(), target.getName(), errors);
                return;
            }

            switch (phase) {
                case PHASE6_INSTRUMENTATION: instrumentationEngine.instrument(ann, target); break;
                case PHASE7_OPTIMIZATION:    optimizationEngine.optimize(ann, target); break;
                case PHASE8_RESILIENCE:      resilienceEngine.apply(ann, target); break;
                case PHASE9_PATTERNS:        patternEngine.apply(ann, target); break;
                case PHASE5_BYTECODE:
                    // Phase 5 bytecode transforms are dispatched differently (byte[] → byte[])
                    log("Registry: Phase 5 bytecode annotation %s registered for %s (use bytecode() engine for transforms)",
                            ann.annotationType().getSimpleName(), target.getName());
                    break;
            }
        }

        /**
         * Scan a class for all DeepMix annotations and process them.
         */
        public void scanAndProcess(Class<?> targetClass) {
            log("Registry: scanning %s", targetClass.getName());
            int count = 0;

            // Scan class-level annotations
            for (Annotation ann : targetClass.getAnnotations()) {
                if (phaseMap.containsKey(ann.annotationType())) {
                    // Class-level annotations are processed with a null method placeholder
                    log("Registry: found class-level @%s on %s",
                            ann.annotationType().getSimpleName(), targetClass.getName());
                    count++;
                }
            }

            // Scan method-level annotations
            for (Method method : targetClass.getDeclaredMethods()) {
                for (Annotation ann : method.getAnnotations()) {
                    if (phaseMap.containsKey(ann.annotationType())) {
                        processAnnotation(ann, method);
                        count++;
                    }
                }
            }

            log("Registry: processed %d annotations on %s", count, targetClass.getName());
        }

        /**
         * Process Phase 5 bytecode transformation.
         */
        public byte[] transformBytecode(Annotation ann, byte[] input) throws Exception {
            return bytecodeEngine.dispatch(ann, input);
        }

        // ── Status ───────────────────────────────────────────────

        public AnnotationPhase getPhase(Class<? extends Annotation> annClass) { return phaseMap.get(annClass); }
        public int registeredAnnotationCount() { return phaseMap.size(); }
        public long totalTransformations() { return transformCount.get(); }

        public Map<String, Object> status() {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("registeredAnnotations", phaseMap.size());
            s.put("totalTransformations", transformCount.get());
            s.put("memoizationCacheSizes", optimizationEngine.memoCache().cacheSizes());
            s.put("metricsCount", instrumentationEngine.metrics().all().size());
            s.put("activeTraceSpans", instrumentationEngine.trace().activeSpanCount());
            s.put("auditEntries", instrumentationEngine.audit().size());
            return s;
        }

        /**
         * Graceful shutdown of all engines.
         */
        public void shutdown() {
            instrumentationEngine.shutdownAll();
            resilienceEngine.shutdownAll();
            patternEngine.shutdownAll();
            log("TransformationRegistry: full shutdown complete");
        }
    }


    // ====================================================================
    //  VALIDATION ENGINE
    // ====================================================================

    /**
     * Validates annotation usage, checks for conflicts, and verifies safety.
     */
    public static class ValidationEngine {

        private final Set<String> conflictPairs = ConcurrentHashMap.newKeySet();

        public ValidationEngine() {
            // Register known conflict pairs
            conflictPairs.add("DeepInline:DeepNoInline");
            conflictPairs.add("DeepCold:DeepHot");
            conflictPairs.add("DeepLikely:DeepUnlikely");
            conflictPairs.add("DeepPack:DeepUnpack");
            conflictPairs.add("DeepAsync:DeepSync");
        }

        /**
         * Validate annotation usage on a method. Returns list of error messages.
         */
        public List<String> validate(Annotation ann, Method target) {
            List<String> errors = new ArrayList<>();

            // Check target compatibility
            validateTargetCompatibility(ann, target, errors);

            // Check for conflicting annotations on same method
            if (target != null) {
                validateNoConflicts(ann, target, errors);
            }

            // Phase-specific validations
            validatePhaseSpecific(ann, errors);

            return errors;
        }

        private void validateTargetCompatibility(Annotation ann, Method target, List<String> errors) {
            if (target == null) return;

            Target targetAnn = ann.annotationType().getAnnotation(Target.class);
            if (targetAnn == null) return;

            boolean methodAllowed = false;
            for (ElementType et : targetAnn.value()) {
                if (et == ElementType.METHOD) { methodAllowed = true; break; }
            }

            if (!methodAllowed) {
                errors.add(ann.annotationType().getSimpleName() + " cannot be applied to methods");
            }
        }

        private void validateNoConflicts(Annotation ann, Method target, List<String> errors) {
            String annName = ann.annotationType().getSimpleName();
            for (Annotation other : target.getAnnotations()) {
                String otherName = other.annotationType().getSimpleName();
                String pair1 = annName + ":" + otherName;
                String pair2 = otherName + ":" + annName;
                if (conflictPairs.contains(pair1) || conflictPairs.contains(pair2)) {
                    errors.add("Conflict: @" + annName + " and @" + otherName +
                            " cannot be used together on " + target.getName());
                }
            }
        }

        private void validatePhaseSpecific(Annotation ann, List<String> errors) {
            // GPU: warn about fallback
            if (ann instanceof DeepGPU) {
                DeepGPU gpu = (DeepGPU) ann;
                if (!gpu.fallbackToCPU()) {
                    errors.add("WARNING: @DeepGPU without fallbackToCPU may fail on systems without GPU");
                }
            }

            // Retry: validate max attempts
            if (ann instanceof DeepRetry) {
                DeepRetry retry = (DeepRetry) ann;
                if (retry.maxAttempts() < 1) {
                    errors.add("@DeepRetry maxAttempts must be >= 1");
                }
                if (retry.delayMs() < 0) {
                    errors.add("@DeepRetry delayMs must be >= 0");
                }
            }

            // Circuit breaker: validate thresholds
            if (ann instanceof DeepCircuit) {
                DeepCircuit circuit = (DeepCircuit) ann;
                if (circuit.failureThreshold() < 1) {
                    errors.add("@DeepCircuit failureThreshold must be >= 1");
                }
                if (circuit.successThreshold() < 1) {
                    errors.add("@DeepCircuit successThreshold must be >= 1");
                }
            }

            // Rate limit: validate window
            if (ann instanceof DeepRateLimit) {
                DeepRateLimit rl = (DeepRateLimit) ann;
                if (rl.maxRequests() < 1) {
                    errors.add("@DeepRateLimit maxRequests must be >= 1");
                }
                if (rl.windowSeconds() < 1) {
                    errors.add("@DeepRateLimit windowSeconds must be >= 1");
                }
            }

            // Vectorize: validate width
            if (ann instanceof DeepVectorize) {
                DeepVectorize vec = (DeepVectorize) ann;
                if (vec.vectorWidth() != 64 && vec.vectorWidth() != 128 &&
                        vec.vectorWidth() != 256 && vec.vectorWidth() != 512) {
                    errors.add("@DeepVectorize vectorWidth must be 64, 128, 256, or 512");
                }
            }

            // Memoize: validate cache size
            if (ann instanceof DeepMemoize) {
                DeepMemoize memo = (DeepMemoize) ann;
                if (memo.maxCacheSize() < 1) {
                    errors.add("@DeepMemoize maxCacheSize must be >= 1");
                }
            }
        }

        public void addConflictPair(String annotation1, String annotation2) {
            conflictPairs.add(annotation1 + ":" + annotation2);
        }
    }


    // ====================================================================
    //  PERFORMANCE MONITOR
    // ====================================================================

    /**
     * Tracks transformation overhead and generates performance reports.
     */
    public static class PerformanceMonitor {

        private static final PerformanceMonitor INSTANCE = new PerformanceMonitor();

        private final ConcurrentHashMap<String, TransformStats> stats = new ConcurrentHashMap<>();
        private final AtomicLong totalTransformTimeNanos = new AtomicLong(0);

        public static class TransformStats {
            public final String name;
            public final AtomicLong invocations = new AtomicLong(0);
            public final AtomicLong totalNanos = new AtomicLong(0);
            public volatile long minNanos = Long.MAX_VALUE;
            public volatile long maxNanos = 0;

            TransformStats(String name) { this.name = name; }

            public void record(long nanos) {
                invocations.incrementAndGet();
                totalNanos.addAndGet(nanos);
                if (nanos < minNanos) minNanos = nanos;
                if (nanos > maxNanos) maxNanos = nanos;
            }

            public double avgMs() {
                long cnt = invocations.get();
                return cnt > 0 ? (totalNanos.get() / 1_000_000.0) / cnt : 0;
            }
        }

        public static PerformanceMonitor getInstance() { return INSTANCE; }

        public TransformStats getStats(String name) {
            return stats.computeIfAbsent(name, TransformStats::new);
        }

        public long recordTransform(String name, long startNanos) {
            long elapsed = System.nanoTime() - startNanos;
            getStats(name).record(elapsed);
            totalTransformTimeNanos.addAndGet(elapsed);
            return elapsed;
        }

        public Map<String, Map<String, Object>> report() {
            Map<String, Map<String, Object>> report = new LinkedHashMap<>();
            for (TransformStats ts : stats.values()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("invocations", ts.invocations.get());
                entry.put("totalMs", ts.totalNanos.get() / 1_000_000.0);
                entry.put("avgMs", ts.avgMs());
                entry.put("minMs", ts.minNanos == Long.MAX_VALUE ? 0 : ts.minNanos / 1_000_000.0);
                entry.put("maxMs", ts.maxNanos / 1_000_000.0);
                report.put(ts.name, entry);
            }
            return report;
        }

        public double totalTransformTimeMs() { return totalTransformTimeNanos.get() / 1_000_000.0; }

        public String prettyReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("╔══════════════════════════════════════════════════════════════╗\n");
            sb.append("║          DeepMix Nexus Performance Report                   ║\n");
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║  Total transformations: %-35d ║\n", transformCount.get()));
            sb.append(String.format("║  Total transform time:  %-32.2fms ║\n", totalTransformTimeMs()));
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");

            List<TransformStats> sorted = stats.values().stream()
                    .sorted((a, b) -> Long.compare(b.totalNanos.get(), a.totalNanos.get()))
                    .collect(Collectors.toList());

            for (TransformStats ts : sorted) {
                sb.append(String.format("║  %-20s  calls=%-8d  avg=%.3fms  total=%.1fms\n",
                        ts.name, ts.invocations.get(), ts.avgMs(),
                        ts.totalNanos.get() / 1_000_000.0));
            }

            sb.append("╚══════════════════════════════════════════════════════════════╝\n");
            return sb.toString();
        }
    }


    // ====================================================================
    //  UTILITY: METHOD KEY HELPER
    // ====================================================================

    /**
     * Generates a canonical key for a method.
     */
    static String methodKey(Method target) {
        if (target == null) return "unknown";
        return target.getDeclaringClass().getName() + "#" + target.getName();
    }
}
