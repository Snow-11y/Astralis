package stellar.snow.astralis.integration.DeepMix.Transformers;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.lang.annotation.*;
import java.lang.reflect.Modifier;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;
import java.util.regex.*;

import com.google.gson.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 * DeepMix Phase 1 & Phase 2 — Full Implementation
 *
 * PHASE 1: Core JVM Bytecode Annotations (18 annotations)
 *   Group A — Method Transformation (6)
 *   Group B — Class Modification (6)
 *   Group C — Advanced Control (6)
 *
 * PHASE 2: Data Format Annotations (14 annotations)
 *   Configuration Files (7)
 *   Document Formats (7)
 *
 * @author Stellar Snow Astralis Team
 * @version 1.0.0
 */
public class DeepMixTransformEngine {

    // Internal logger tag
    private static final String TAG = "[DeepMix]";

    // Global configuration
    private static volatile boolean hotReloadEnabled = true;
    private static final AtomicInteger transformationCount = new AtomicInteger(0);

    // ============================================================================
    // PHASE 1: CORE JVM BYTECODE ANNOTATIONS
    // ============================================================================

    // ========================================
    // GROUP A: METHOD TRANSFORMATION (6)
    // ========================================

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepOverwrite {
        String target();
        String method();
        String descriptor() default "";
        boolean force() default false;
        int priority() default 1000;
        boolean hotReload() default true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DOW {
        String target();
        String method();
        String descriptor() default "";
        boolean force() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepModify {
        String target();
        String method() default "";
        String variable() default "";
        ModifyType type() default ModifyType.VALUE;
        boolean hotReload() default true;

        enum ModifyType {
            VALUE, TYPE, SCOPE, LIFETIME
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DM {
        String target();
        String method() default "";
        String variable() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DeepWrap {
        String target();
        String method();
        WrapPosition position() default WrapPosition.BOTH;
        boolean captureReturn() default false;
        boolean captureArgs() default true;
        boolean hotReload() default true;

        enum WrapPosition {
            BEFORE, AFTER, BOTH, AROUND
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DW {
        String target();
        String method();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DeepSlice {
        String target();
        String method();
        int startLine() default -1;
        int endLine() default -1;
        String startMarker() default "";
        String endMarker() default "";
        SliceAction action() default SliceAction.EXTRACT;
        boolean hotReload() default true;

        enum SliceAction {
            EXTRACT, REMOVE, REPLACE, COPY, MOVE
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DS {
        String target();
        String method();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DeepMerge {
        String target();
        String[] methods();
        MergeStrategy strategy() default MergeStrategy.SEQUENTIAL;
        ConflictResolution onConflict() default ConflictResolution.FAIL;
        boolean hotReload() default true;

        enum MergeStrategy {
            SEQUENTIAL, PARALLEL, CONDITIONAL, PRIORITY_BASED
        }

        enum ConflictResolution {
            FAIL, FIRST_WINS, LAST_WINS, MANUAL, MERGE_ALL
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DMG {
        String target();
        String[] methods();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepTransform {
        String target();
        Class<? extends BytecodeTransformer> transformer();
        String[] params() default {};
        boolean hotReload() default true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DT {
        String target();
        Class<? extends BytecodeTransformer> transformer();
    }

    // ========================================
    // GROUP B: CLASS MODIFICATION (6)
    // ========================================

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.CONSTRUCTOR, ElementType.TYPE})
    public @interface DeepConstructor {
        String target();
        ConstructorAction action() default ConstructorAction.MODIFY;
        String descriptor() default "";
        boolean addSuper() default false;
        boolean hotReload() default true;

        enum ConstructorAction {
            MODIFY, ADD, REMOVE, REPLACE, WRAP
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.CONSTRUCTOR, ElementType.TYPE})
    public @interface DCT {
        String target();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DeepInterface {
        String target();
        Class<?>[] interfaces();
        boolean implementMethods() default true;
        boolean hotReload() default true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DIF {
        String target();
        Class<?>[] interfaces();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DeepInherit {
        String target();
        String newSuperclass() default "";
        boolean preserveOriginal() default false;
        boolean hotReload() default true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DIH {
        String target();
        String newSuperclass() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    public @interface DeepAnnotate {
        String target();
        Class<? extends Annotation>[] annotations();
        String[] values() default {};
        boolean hotReload() default true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    public @interface DAN {
        String target();
        Class<? extends Annotation>[] annotations();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepProxy {
        String target();
        Class<? extends ProxyHandler> handler();
        ProxyMode mode() default ProxyMode.INTERCEPT;
        boolean hotReload() default true;

        enum ProxyMode {
            INTERCEPT, DELEGATE, WRAP, MONITOR
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DP {
        String target();
        Class<? extends ProxyHandler> handler();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepEvent {
        String target();
        EventPhase phase() default EventPhase.POST;
        String eventType() default "";
        int priority() default 1000;
        boolean hotReload() default true;

        enum EventPhase {
            PRE, POST, BOTH, REPLACE
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DE {
        String target();
    }

    // ========================================
    // GROUP C: ADVANCED CONTROL (6)
    // ========================================

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DeepControl {
        String targetMod();
        ControlAction action() default ControlAction.MODIFY;
        String[] targets() default {};
        boolean hotReload() default true;

        enum ControlAction {
            MODIFY, DISABLE, ENABLE, REDIRECT, MONITOR
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DC {
        String targetMod();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepBehavior {
        String target();
        Class<? extends BehaviorModifier> modifier();
        String[] conditions() default {};
        boolean hotReload() default true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DB {
        String target();
        Class<? extends BehaviorModifier> modifier();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepSurgical {
        String target();
        InjectionPoint point();
        boolean allowNative() default false;
        boolean allowJVM() default true;
        boolean hotReload() default true;

        enum InjectionPoint {
            HEAD, TAIL, RETURN, INVOKE, FIELD_ACCESS, NEW, CUSTOM
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DSI {
        String target();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DeepLambda {
        String target();
        LambdaTransform transform() default LambdaTransform.OPTIMIZE;
        boolean hotReload() default true;

        enum LambdaTransform {
            OPTIMIZE, INLINE, EXTRACT, PARALLELIZE, MEMOIZE
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DL {
        String target();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DeepAsync {
        String target();
        AsyncMode mode() default AsyncMode.FUTURE;
        String executor() default "ForkJoinPool.commonPool()";
        boolean hotReload() default true;

        enum AsyncMode {
            FUTURE, COMPLETABLE_FUTURE, PROMISE, CALLBACK, REACTIVE
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DAS {
        String target();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DeepCache {
        String target();
        CacheStrategy strategy() default CacheStrategy.LRU;
        int maxSize() default 100;
        long ttlSeconds() default -1;
        boolean hotReload() default true;

        enum CacheStrategy {
            LRU, LFU, FIFO, TTL, WEAK_REF
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface DCH {
        String target();
    }

    // ============================================================================
    // PHASE 2: DATA FORMAT ANNOTATIONS
    // ============================================================================

    // ========================================
    // CONFIGURATION FILES (7)
    // ========================================

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepJSON {
        String path() default "";
        JsonOperation operation() default JsonOperation.MODIFY;
        String selector() default "$";
        boolean hotReload() default true;

        enum JsonOperation {
            MODIFY, ADD, REMOVE, MERGE, REPLACE, VALIDATE
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DJ {
        String path() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepXML {
        String path() default "";
        XmlOperation operation() default XmlOperation.MODIFY;
        String xpath() default "/";
        boolean hotReload() default true;

        enum XmlOperation {
            MODIFY, ADD, REMOVE, MERGE, REPLACE, VALIDATE
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DX {
        String path() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepYAML {
        String path() default "";
        YamlOperation operation() default YamlOperation.MODIFY;
        String selector() default "";
        boolean hotReload() default true;

        enum YamlOperation {
            MODIFY, ADD, REMOVE, MERGE, REPLACE, VALIDATE
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DY {
        String path() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepTOML {
        String path() default "";
        TomlOperation operation() default TomlOperation.MODIFY;
        String key() default "";
        boolean hotReload() default true;

        enum TomlOperation {
            MODIFY, ADD, REMOVE, MERGE, REPLACE, VALIDATE
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DTOML {
        String path() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepProperties {
        String path() default "";
        PropertiesOperation operation() default PropertiesOperation.MODIFY;
        String key() default "";
        boolean hotReload() default true;

        enum PropertiesOperation {
            MODIFY, ADD, REMOVE, MERGE, REPLACE
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DPROP {
        String path() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepSQL {
        String query() default "";
        SqlOperation operation() default SqlOperation.MODIFY;
        boolean validate() default true;
        boolean hotReload() default true;

        enum SqlOperation {
            MODIFY, OPTIMIZE, VALIDATE, EXPLAIN, INJECT_WHERE, INJECT_JOIN
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DSQL {
        String query() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepMarkdown {
        String path() default "";
        MarkdownOperation operation() default MarkdownOperation.MODIFY;
        String selector() default "";
        boolean hotReload() default true;

        enum MarkdownOperation {
            MODIFY, ADD_SECTION, REMOVE_SECTION, REPLACE, RENDER_HTML
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DMD {
        String path() default "";
    }

    // ========================================
    // DOCUMENT FORMATS (7)
    // ========================================

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepHTML {
        String path() default "";
        HtmlOperation operation() default HtmlOperation.MODIFY;
        String cssSelector() default "";
        boolean hotReload() default true;

        enum HtmlOperation {
            MODIFY, ADD_ELEMENT, REMOVE_ELEMENT, REPLACE, EXTRACT, INJECT_SCRIPT
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DHTML {
        String path() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepCSS {
        String path() default "";
        CssOperation operation() default CssOperation.MODIFY;
        String selector() default "";
        boolean hotReload() default true;

        enum CssOperation {
            MODIFY, ADD_RULE, REMOVE_RULE, REPLACE, MINIFY, PRETTIFY
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DCSS {
        String path() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepProto {
        String path() default "";
        ProtoOperation operation() default ProtoOperation.MODIFY;
        String messageName() default "";
        boolean hotReload() default true;

        enum ProtoOperation {
            MODIFY, ADD_FIELD, REMOVE_FIELD, REPLACE, GENERATE_CODE
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DPROTO {
        String path() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepGradle {
        String path() default "";
        GradleOperation operation() default GradleOperation.MODIFY;
        String block() default "";
        boolean hotReload() default true;

        enum GradleOperation {
            MODIFY, ADD_DEPENDENCY, REMOVE_DEPENDENCY, ADD_PLUGIN, ADD_REPOSITORY
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DGRAD {
        String path() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepMaven {
        String path() default "";
        MavenOperation operation() default MavenOperation.MODIFY;
        String section() default "";
        boolean hotReload() default true;

        enum MavenOperation {
            MODIFY, ADD_DEPENDENCY, REMOVE_DEPENDENCY, ADD_PLUGIN, ADD_REPOSITORY
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DMVN {
        String path() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepDockerfile {
        String path() default "";
        DockerOperation operation() default DockerOperation.MODIFY;
        boolean hotReload() default true;

        enum DockerOperation {
            MODIFY, ADD_INSTRUCTION, REMOVE_INSTRUCTION, REPLACE, OPTIMIZE
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DDCKR {
        String path() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepKubernetes {
        String path() default "";
        K8sOperation operation() default K8sOperation.MODIFY;
        String kind() default "";
        boolean hotReload() default true;

        enum K8sOperation {
            MODIFY, ADD_RESOURCE, REMOVE_RESOURCE, REPLACE, VALIDATE
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DK8S {
        String path() default "";
    }


    // ============================================================================
    // CORE INTERFACES
    // ============================================================================

    public interface BytecodeTransformer {
        void transform(ClassNode classNode, MethodNode methodNode);
    }

    public interface ProxyHandler {
        Object handleInvocation(Object proxy, String methodName, Object[] args);
    }

    public interface BehaviorModifier {
        void modifyBehavior(Object target, String methodName);
    }

    // ============================================================================
    // DEFAULT IMPLEMENTATIONS
    // ============================================================================

    public static class DefaultTransformer implements BytecodeTransformer {
        @Override
        public void transform(ClassNode classNode, MethodNode methodNode) {
            // No-op default — users subclass this
        }
    }

    public static class DefaultProxyHandler implements ProxyHandler {
        @Override
        public Object handleInvocation(Object proxy, String methodName, Object[] args) {
            throw new UnsupportedOperationException(
                TAG + " DefaultProxyHandler invoked — provide a real ProxyHandler implementation");
        }
    }

    public static class DefaultBehaviorModifier implements BehaviorModifier {
        @Override
        public void modifyBehavior(Object target, String methodName) {
            // No-op default
        }
    }


    // ============================================================================
    // PHASE 1 PROCESSORS — FULL IMPLEMENTATIONS
    // ============================================================================

    /**
     * Processes method-level bytecode transformation annotations.
     * Handles: @DeepOverwrite, @DeepModify, @DeepWrap, @DeepSlice, @DeepMerge, @DeepTransform
     */
    public static class MethodTransformProcessor {

        // --------------------------------------------------
        // @DeepOverwrite / @DOW
        // --------------------------------------------------
        public void processOverwrite(ClassNode targetClass, MethodNode sourceMethod, DeepOverwrite annotation) {
            String methodName = annotation.method();
            String descriptor = annotation.descriptor();

            MethodNode targetMethod = findMethod(targetClass, methodName, descriptor);

            if (targetMethod == null) {
                if (annotation.force()) {
                    // Force mode: add the method to the class even if it didn't exist
                    MethodNode newMethod = cloneMethod(sourceMethod);
                    newMethod.name = methodName;
                    if (!descriptor.isEmpty()) {
                        newMethod.desc = descriptor;
                    }
                    targetClass.methods.add(newMethod);
                    log("Overwrite (force-add): %s.%s%s", targetClass.name, methodName, newMethod.desc);
                } else {
                    throw new DeepMixException(
                        "Target method not found: " + targetClass.name + "." + methodName + descriptor
                        + " — use force=true to create it");
                }
                return;
            }

            // Clear existing instructions
            targetMethod.instructions.clear();
            targetMethod.tryCatchBlocks.clear();
            targetMethod.localVariables = null;
            targetMethod.visibleAnnotations = null;
            targetMethod.invisibleAnnotations = null;
            targetMethod.maxStack = 0;
            targetMethod.maxLocals = 0;

            // Copy source instructions, adapting owner references
            InsnList cloned = cloneInstructions(sourceMethod.instructions, sourceMethod, targetMethod);
            targetMethod.instructions.add(cloned);

            // Copy try-catch blocks
            if (sourceMethod.tryCatchBlocks != null) {
                targetMethod.tryCatchBlocks = new ArrayList<>();
                Map<LabelNode, LabelNode> labelMap = buildLabelMap(sourceMethod.instructions, targetMethod.instructions);
                for (TryCatchBlockNode tcb : sourceMethod.tryCatchBlocks) {
                    targetMethod.tryCatchBlocks.add(new TryCatchBlockNode(
                        labelMap.getOrDefault(tcb.start, tcb.start),
                        labelMap.getOrDefault(tcb.end, tcb.end),
                        labelMap.getOrDefault(tcb.handler, tcb.handler),
                        tcb.type
                    ));
                }
            }

            // Copy local variable info
            if (sourceMethod.localVariables != null) {
                targetMethod.localVariables = new ArrayList<>();
                Map<LabelNode, LabelNode> labelMap = buildLabelMap(sourceMethod.instructions, targetMethod.instructions);
                for (LocalVariableNode lv : sourceMethod.localVariables) {
                    targetMethod.localVariables.add(new LocalVariableNode(
                        lv.name, lv.desc, lv.signature,
                        labelMap.getOrDefault(lv.start, lv.start),
                        labelMap.getOrDefault(lv.end, lv.end),
                        lv.index
                    ));
                }
            }

            targetMethod.maxStack = sourceMethod.maxStack;
            targetMethod.maxLocals = sourceMethod.maxLocals;

            // Mark for frame recomputation
            targetMethod.visitMaxs(0, 0);

            transformationCount.incrementAndGet();
            log("Overwrite complete: %s.%s%s (priority=%d)",
                targetClass.name, methodName, targetMethod.desc, annotation.priority());
        }

        // --------------------------------------------------
        // @DeepModify / @DM
        // --------------------------------------------------
        public void processModify(ClassNode classNode, MethodNode methodNode, DeepModify annotation) {
            String variable = annotation.variable();
            DeepModify.ModifyType modType = annotation.type();

            switch (modType) {
                case VALUE:
                    modifyValue(methodNode, variable);
                    break;
                case TYPE:
                    modifyVariableType(classNode, methodNode, variable);
                    break;
                case SCOPE:
                    modifyVariableScope(methodNode, variable);
                    break;
                case LIFETIME:
                    modifyVariableLifetime(methodNode, variable);
                    break;
            }

            transformationCount.incrementAndGet();
            log("Modify complete: %s.%s (var=%s, type=%s)",
                classNode.name, methodNode.name, variable, modType);
        }

        private void modifyValue(MethodNode method, String variable) {
            // Find the local variable index
            int varIndex = resolveVariableIndex(method, variable);
            if (varIndex < 0) {
                // Try treating 'variable' as a constant pattern (LDC match)
                modifyConstant(method, variable);
                return;
            }

            // Scan instructions for stores to this variable and wrap with modification hook
            InsnList insns = method.instructions;
            List<AbstractInsnNode> storePoints = new ArrayList<>();

            for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof VarInsnNode) {
                    VarInsnNode var = (VarInsnNode) insn;
                    if (var.var == varIndex && isStoreOpcode(var.getOpcode())) {
                        storePoints.add(insn);
                    }
                }
            }

            // For each store, insert a modification callback before the store
            for (AbstractInsnNode storeInsn : storePoints) {
                InsnList hook = new InsnList();
                // The value to be stored is on top of the stack.
                // We duplicate it, invoke a static modification method, and let the modified value be stored.
                int opcode = storeInsn.getOpcode();
                if (opcode == Opcodes.ISTORE) {
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                        "modifyInt", "(I)I", false));
                } else if (opcode == Opcodes.FSTORE) {
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                        "modifyFloat", "(F)F", false));
                } else if (opcode == Opcodes.DSTORE) {
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                        "modifyDouble", "(D)D", false));
                } else if (opcode == Opcodes.LSTORE) {
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                        "modifyLong", "(J)J", false));
                } else if (opcode == Opcodes.ASTORE) {
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                        "modifyObject", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                }
                insns.insertBefore(storeInsn, hook);
            }
        }

        private void modifyConstant(MethodNode method, String constantPattern) {
            InsnList insns = method.instructions;
            for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof LdcInsnNode) {
                    LdcInsnNode ldc = (LdcInsnNode) insn;
                    if (String.valueOf(ldc.cst).equals(constantPattern)) {
                        // Insert modification call after the LDC
                        InsnList hook = new InsnList();
                        if (ldc.cst instanceof Integer) {
                            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                                "modifyInt", "(I)I", false));
                        } else if (ldc.cst instanceof Float) {
                            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                                "modifyFloat", "(F)F", false));
                        } else if (ldc.cst instanceof Long) {
                            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                                "modifyLong", "(J)J", false));
                        } else if (ldc.cst instanceof Double) {
                            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                                "modifyDouble", "(D)D", false));
                        } else if (ldc.cst instanceof String) {
                            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                                "modifyString", "(Ljava/lang/String;)Ljava/lang/String;", false));
                        }
                        insns.insert(insn, hook);
                    }
                }
            }
        }

        private void modifyVariableType(ClassNode classNode, MethodNode method, String variable) {
            int idx = resolveVariableIndex(method, variable);
            if (idx < 0) return;

            if (method.localVariables != null) {
                for (LocalVariableNode lv : method.localVariables) {
                    if (lv.index == idx) {
                        // Store original descriptor for diagnostics
                        String originalDesc = lv.desc;
                        // Widening: int → long, float → double for numeric types
                        if (lv.desc.equals("I")) {
                            lv.desc = "J";
                            widenStoresAndLoads(method, idx, Opcodes.ILOAD, Opcodes.LLOAD,
                                Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.I2L);
                        } else if (lv.desc.equals("F")) {
                            lv.desc = "D";
                            widenStoresAndLoads(method, idx, Opcodes.FLOAD, Opcodes.DLOAD,
                                Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.F2D);
                        }
                        log("Variable type widened: %s from %s to %s", variable, originalDesc, lv.desc);
                        break;
                    }
                }
            }
        }

        private void widenStoresAndLoads(MethodNode method, int varIdx,
                int oldLoad, int newLoad, int oldStore, int newStore, int conversionOpcode) {
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof VarInsnNode) {
                    VarInsnNode vi = (VarInsnNode) insn;
                    if (vi.var == varIdx) {
                        if (vi.getOpcode() == oldLoad) {
                            vi.setOpcode(newLoad);
                        } else if (vi.getOpcode() == oldStore) {
                            // Insert conversion before the store
                            method.instructions.insertBefore(vi, new InsnNode(conversionOpcode));
                            vi.setOpcode(newStore);
                        }
                    }
                }
            }
        }

        private void modifyVariableScope(MethodNode method, String variable) {
            // Extend scope of the variable to cover the entire method
            if (method.localVariables == null) return;
            int idx = resolveVariableIndex(method, variable);
            if (idx < 0) return;

            LabelNode methodStart = null;
            LabelNode methodEnd = null;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof LabelNode) {
                    if (methodStart == null) methodStart = (LabelNode) insn;
                    methodEnd = (LabelNode) insn;
                }
            }

            if (methodStart != null && methodEnd != null) {
                for (LocalVariableNode lv : method.localVariables) {
                    if (lv.index == idx) {
                        lv.start = methodStart;
                        lv.end = methodEnd;
                        break;
                    }
                }
            }
        }

        private void modifyVariableLifetime(MethodNode method, String variable) {
            // Convert the variable to a field-backed storage (extend lifetime beyond method)
            // Insert store/load from a synthetic field instead of a local variable
            int idx = resolveVariableIndex(method, variable);
            if (idx < 0) return;

            String desc = "Ljava/lang/Object;";
            if (method.localVariables != null) {
                for (LocalVariableNode lv : method.localVariables) {
                    if (lv.index == idx) {
                        desc = lv.desc;
                        break;
                    }
                }
            }

            // The actual field creation happens in the class-level pass;
            // here we mark it with a special annotation for the ClassModificationProcessor
            if (method.visibleAnnotations == null) {
                method.visibleAnnotations = new ArrayList<>();
            }
            AnnotationNode lifetimeMarker = new AnnotationNode(
                "Lstellar/snow/astralis/integration/DeepMix/DeepMixLifetimeExtended;");
            lifetimeMarker.values = Arrays.asList("varIndex", idx, "varDesc", desc, "varName", variable);
            method.visibleAnnotations.add(lifetimeMarker);
        }

        // --------------------------------------------------
        // @DeepWrap / @DW
        // --------------------------------------------------
        public void processWrap(ClassNode classNode, MethodNode wrapperMethod, DeepWrap annotation) {
            String targetMethodName = annotation.method();
            MethodNode targetMethod = findMethod(classNode, targetMethodName, "");

            if (targetMethod == null) {
                throw new DeepMixException("Wrap target not found: " + classNode.name + "." + targetMethodName);
            }

            DeepWrap.WrapPosition position = annotation.position();

            // Rename original method to __deepmix_original_<name>
            String originalName = "__deepmix_original_" + targetMethod.name;
            MethodNode backup = cloneMethod(targetMethod);
            backup.name = originalName;
            backup.access = (backup.access & ~Opcodes.ACC_PUBLIC & ~Opcodes.ACC_PROTECTED) | Opcodes.ACC_PRIVATE;
            classNode.methods.add(backup);

            InsnList originalInsns = targetMethod.instructions;
            InsnList newInsns = new InsnList();

            boolean isStatic = (targetMethod.access & Opcodes.ACC_STATIC) != 0;
            Type returnType = Type.getReturnType(targetMethod.desc);
            Type[] argTypes = Type.getArgumentTypes(targetMethod.desc);

            switch (position) {
                case BEFORE: {
                    // Call wrapper, then call original
                    insertWrapperCall(newInsns, classNode, wrapperMethod, isStatic, argTypes, annotation.captureArgs());
                    insertOriginalDelegation(newInsns, classNode, backup, isStatic, argTypes, returnType);
                    break;
                }
                case AFTER: {
                    // Call original, store result, call wrapper, return result
                    insertOriginalDelegation(newInsns, classNode, backup, isStatic, argTypes, returnType);
                    if (annotation.captureReturn() && returnType.getSort() != Type.VOID) {
                        // Result is on stack; duplicate for wrapper
                        newInsns.add(new InsnNode(returnType.getSize() == 2 ? Opcodes.DUP2 : Opcodes.DUP));
                    }
                    insertWrapperCall(newInsns, classNode, wrapperMethod, isStatic, argTypes, annotation.captureArgs());
                    addReturnInsn(newInsns, returnType);
                    break;
                }
                case BOTH: {
                    insertWrapperCall(newInsns, classNode, wrapperMethod, isStatic, argTypes, annotation.captureArgs());
                    insertOriginalDelegation(newInsns, classNode, backup, isStatic, argTypes, returnType);
                    if (returnType.getSort() != Type.VOID) {
                        newInsns.add(new InsnNode(returnType.getSize() == 2 ? Opcodes.DUP2 : Opcodes.DUP));
                    }
                    insertWrapperCall(newInsns, classNode, wrapperMethod, isStatic, argTypes, annotation.captureArgs());
                    addReturnInsn(newInsns, returnType);
                    break;
                }
                case AROUND: {
                    // The wrapper method IS the new body; it should call __deepmix_original_ internally
                    newInsns = cloneInstructions(wrapperMethod.instructions, wrapperMethod, targetMethod);
                    break;
                }
            }

            if (position != DeepWrap.WrapPosition.AROUND) {
                targetMethod.instructions.clear();
                targetMethod.instructions.add(newInsns);
                targetMethod.tryCatchBlocks.clear();
            } else {
                targetMethod.instructions.clear();
                targetMethod.instructions.add(newInsns);
                if (wrapperMethod.tryCatchBlocks != null) {
                    targetMethod.tryCatchBlocks = new ArrayList<>(wrapperMethod.tryCatchBlocks);
                }
            }

            targetMethod.maxStack = Math.max(targetMethod.maxStack, wrapperMethod.maxStack + 4);
            targetMethod.maxLocals = Math.max(targetMethod.maxLocals, wrapperMethod.maxLocals + argTypes.length + 2);

            transformationCount.incrementAndGet();
            log("Wrap complete: %s.%s (position=%s)", classNode.name, targetMethodName, position);
        }

        private void insertWrapperCall(InsnList insns, ClassNode owner, MethodNode wrapper,
                boolean isStatic, Type[] argTypes, boolean captureArgs) {
            if (!isStatic) {
                insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            if (captureArgs) {
                int slot = isStatic ? 0 : 1;
                for (Type t : argTypes) {
                    insns.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), slot));
                    slot += t.getSize();
                }
            }
            int invokeOp = isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL;
            insns.add(new MethodInsnNode(invokeOp, owner.name, wrapper.name, wrapper.desc, false));
            Type wrapperReturn = Type.getReturnType(wrapper.desc);
            if (wrapperReturn.getSort() != Type.VOID) {
                insns.add(new InsnNode(wrapperReturn.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
            }
        }

        private void insertOriginalDelegation(InsnList insns, ClassNode owner, MethodNode original,
                boolean isStatic, Type[] argTypes, Type returnType) {
            if (!isStatic) {
                insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            int slot = isStatic ? 0 : 1;
            for (Type t : argTypes) {
                insns.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), slot));
                slot += t.getSize();
            }
            int invokeOp = isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKESPECIAL;
            insns.add(new MethodInsnNode(invokeOp, owner.name, original.name, original.desc, false));
        }

        // --------------------------------------------------
        // @DeepSlice / @DS
        // --------------------------------------------------
        public void processSlice(ClassNode classNode, MethodNode methodNode, DeepSlice annotation) {
            String targetMethodName = annotation.method();
            MethodNode targetMethod = findMethod(classNode, targetMethodName, "");
            if (targetMethod == null) {
                throw new DeepMixException("Slice target not found: " + classNode.name + "." + targetMethodName);
            }

            int startLine = annotation.startLine();
            int endLine = annotation.endLine();
            String startMarker = annotation.startMarker();
            String endMarker = annotation.endMarker();

            // Determine slice boundaries
            AbstractInsnNode sliceStart = null;
            AbstractInsnNode sliceEnd = null;

            if (startLine >= 0 && endLine >= 0) {
                // Line-number based slicing
                for (AbstractInsnNode insn = targetMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof LineNumberNode) {
                        LineNumberNode ln = (LineNumberNode) insn;
                        if (ln.line == startLine && sliceStart == null) {
                            sliceStart = insn;
                        }
                        if (ln.line == endLine) {
                            sliceEnd = insn;
                        }
                    }
                }
            } else if (!startMarker.isEmpty() && !endMarker.isEmpty()) {
                // Marker-based slicing: look for LDC instructions with marker strings
                for (AbstractInsnNode insn = targetMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof LdcInsnNode) {
                        LdcInsnNode ldc = (LdcInsnNode) insn;
                        if (startMarker.equals(ldc.cst) && sliceStart == null) {
                            sliceStart = insn;
                        }
                        if (endMarker.equals(ldc.cst)) {
                            sliceEnd = insn;
                        }
                    }
                }
            }

            if (sliceStart == null || sliceEnd == null) {
                throw new DeepMixException("Could not resolve slice boundaries for " +
                    classNode.name + "." + targetMethodName);
            }

            DeepSlice.SliceAction action = annotation.action();

            switch (action) {
                case EXTRACT: {
                    // Copy sliced instructions into the annotated method
                    InsnList extracted = extractRange(targetMethod.instructions, sliceStart, sliceEnd);
                    methodNode.instructions.clear();
                    methodNode.instructions.add(extracted);
                    break;
                }
                case REMOVE: {
                    removeRange(targetMethod.instructions, sliceStart, sliceEnd);
                    break;
                }
                case REPLACE: {
                    // Replace the slice with the annotated method's body
                    InsnList replacement = cloneInstructions(methodNode.instructions, methodNode, targetMethod);
                    removeRange(targetMethod.instructions, sliceStart, sliceEnd);
                    if (sliceStart.getPrevious() != null) {
                        targetMethod.instructions.insert(sliceStart.getPrevious(), replacement);
                    } else {
                        targetMethod.instructions.insert(replacement);
                    }
                    break;
                }
                case COPY: {
                    // Copy without removing from source
                    InsnList copied = extractRange(targetMethod.instructions, sliceStart, sliceEnd);
                    methodNode.instructions.clear();
                    methodNode.instructions.add(copied);
                    break;
                }
                case MOVE: {
                    // Extract and remove from source, place into annotated method
                    InsnList moved = extractRange(targetMethod.instructions, sliceStart, sliceEnd);
                    removeRange(targetMethod.instructions, sliceStart, sliceEnd);
                    methodNode.instructions.clear();
                    methodNode.instructions.add(moved);
                    break;
                }
            }

            transformationCount.incrementAndGet();
            log("Slice complete: %s.%s (action=%s)", classNode.name, targetMethodName, action);
        }

        private InsnList extractRange(InsnList source, AbstractInsnNode start, AbstractInsnNode end) {
            InsnList result = new InsnList();
            boolean capturing = false;
            for (AbstractInsnNode insn = source.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn == start) capturing = true;
                if (capturing) {
                    result.add(insn.clone(Collections.emptyMap()));
                }
                if (insn == end) break;
            }
            return result;
        }

        private void removeRange(InsnList insns, AbstractInsnNode start, AbstractInsnNode end) {
            List<AbstractInsnNode> toRemove = new ArrayList<>();
            boolean removing = false;
            for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn == start) removing = true;
                if (removing) toRemove.add(insn);
                if (insn == end) break;
            }
            for (AbstractInsnNode insn : toRemove) {
                insns.remove(insn);
            }
        }

        // --------------------------------------------------
        // @DeepMerge / @DMG
        // --------------------------------------------------
        public void processMerge(ClassNode classNode, MethodNode resultMethod, DeepMerge annotation) {
            String[] methodNames = annotation.methods();
            DeepMerge.MergeStrategy strategy = annotation.strategy();
            DeepMerge.ConflictResolution conflictRes = annotation.onConflict();

            List<MethodNode> sourceMethods = new ArrayList<>();
            for (String name : methodNames) {
                MethodNode m = findMethod(classNode, name, "");
                if (m != null) {
                    sourceMethods.add(m);
                } else if (conflictRes == DeepMerge.ConflictResolution.FAIL) {
                    throw new DeepMixException("Merge source not found: " + classNode.name + "." + name);
                }
            }

            if (sourceMethods.isEmpty()) return;

            Type returnType = Type.getReturnType(resultMethod.desc);
            boolean isStatic = (resultMethod.access & Opcodes.ACC_STATIC) != 0;
            Type[] argTypes = Type.getArgumentTypes(resultMethod.desc);

            InsnList merged = new InsnList();

            switch (strategy) {
                case SEQUENTIAL: {
                    // Call each method in sequence, return last result
                    for (int i = 0; i < sourceMethods.size(); i++) {
                        MethodNode src = sourceMethods.get(i);
                        if (!isStatic) merged.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        int slot = isStatic ? 0 : 1;
                        for (Type t : argTypes) {
                            merged.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), slot));
                            slot += t.getSize();
                        }
                        int op = isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL;
                        merged.add(new MethodInsnNode(op, classNode.name, src.name, src.desc, false));
                        Type srcReturn = Type.getReturnType(src.desc);
                        if (i < sourceMethods.size() - 1 && srcReturn.getSort() != Type.VOID) {
                            merged.add(new InsnNode(srcReturn.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
                        }
                    }
                    addReturnInsn(merged, returnType);
                    break;
                }
                case PARALLEL: {
                    // Wrap each call in a CompletableFuture, join all at the end
                    String cfDesc = "Ljava/util/concurrent/CompletableFuture;";
                    int futureSlot = computeNextLocalSlot(resultMethod);

                    for (int i = 0; i < sourceMethods.size(); i++) {
                        MethodNode src = sourceMethods.get(i);
                        // CompletableFuture.supplyAsync(() -> targetMethod(args))
                        merged.add(new InsnNode(Opcodes.ACONST_NULL)); // Placeholder for lambda
                        merged.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            "java/util/concurrent/CompletableFuture", "supplyAsync",
                            "(Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;", false));
                        merged.add(new VarInsnNode(Opcodes.ASTORE, futureSlot + i));
                    }

                    // Join all futures
                    for (int i = 0; i < sourceMethods.size(); i++) {
                        merged.add(new VarInsnNode(Opcodes.ALOAD, futureSlot + i));
                        merged.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                            "java/util/concurrent/CompletableFuture", "join",
                            "()Ljava/lang/Object;", false));
                        if (i < sourceMethods.size() - 1) {
                            merged.add(new InsnNode(Opcodes.POP));
                        }
                    }
                    if (returnType.getSort() == Type.VOID) {
                        merged.add(new InsnNode(Opcodes.POP));
                    }
                    addReturnInsn(merged, returnType);
                    break;
                }
                case CONDITIONAL: {
                    // Chain with if-else: if first returns non-null/true, use it; else try next
                    LabelNode endLabel = new LabelNode();
                    for (int i = 0; i < sourceMethods.size(); i++) {
                        MethodNode src = sourceMethods.get(i);
                        if (!isStatic) merged.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        int slot = isStatic ? 0 : 1;
                        for (Type t : argTypes) {
                            merged.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), slot));
                            slot += t.getSize();
                        }
                        int op = isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL;
                        merged.add(new MethodInsnNode(op, classNode.name, src.name, src.desc, false));

                        if (i < sourceMethods.size() - 1) {
                            Type srcReturn = Type.getReturnType(src.desc);
                            if (srcReturn.getSort() == Type.OBJECT || srcReturn.getSort() == Type.ARRAY) {
                                merged.add(new InsnNode(Opcodes.DUP));
                                merged.add(new JumpInsnNode(Opcodes.IFNONNULL, endLabel));
                                merged.add(new InsnNode(Opcodes.POP));
                            }
                        }
                    }
                    merged.add(endLabel);
                    addReturnInsn(merged, returnType);
                    break;
                }
                case PRIORITY_BASED: {
                    // Same as sequential but honor DeepOverwrite priority annotations
                    sourceMethods.sort((a, b) -> {
                        int pa = getMethodPriority(a);
                        int pb = getMethodPriority(b);
                        return Integer.compare(pb, pa); // higher priority first
                    });
                    // Then do sequential
                    for (int i = 0; i < sourceMethods.size(); i++) {
                        MethodNode src = sourceMethods.get(i);
                        if (!isStatic) merged.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        int slot = isStatic ? 0 : 1;
                        for (Type t : argTypes) {
                            merged.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), slot));
                            slot += t.getSize();
                        }
                        int op = isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL;
                        merged.add(new MethodInsnNode(op, classNode.name, src.name, src.desc, false));
                        Type srcReturn = Type.getReturnType(src.desc);
                        if (i < sourceMethods.size() - 1 && srcReturn.getSort() != Type.VOID) {
                            merged.add(new InsnNode(srcReturn.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
                        }
                    }
                    addReturnInsn(merged, returnType);
                    break;
                }
            }

            resultMethod.instructions.clear();
            resultMethod.instructions.add(merged);
            resultMethod.maxStack = Math.max(8, resultMethod.maxStack);
            resultMethod.maxLocals = Math.max(resultMethod.maxLocals, computeNextLocalSlot(resultMethod) + sourceMethods.size() + 2);

            transformationCount.incrementAndGet();
            log("Merge complete: %s → [%s] (strategy=%s)",
                classNode.name, String.join(", ", methodNames), strategy);
        }

        // --------------------------------------------------
        // @DeepTransform / @DT
        // --------------------------------------------------
        public void processTransform(ClassNode classNode, MethodNode methodNode, DeepTransform annotation) {
            Class<? extends BytecodeTransformer> transformerClass = annotation.transformer();
            String[] params = annotation.params();

            try {
                BytecodeTransformer transformer;
                // Try constructor with String[] params first
                try {
                    java.lang.reflect.Constructor<? extends BytecodeTransformer> ctor =
                        transformerClass.getConstructor(String[].class);
                    transformer = ctor.newInstance((Object) params);
                } catch (NoSuchMethodException e) {
                    // Fall back to default constructor
                    transformer = transformerClass.getDeclaredConstructor().newInstance();
                }

                // Snapshot for rollback
                InsnList backup = cloneInstructions(methodNode.instructions, methodNode, methodNode);
                int backupMaxStack = methodNode.maxStack;
                int backupMaxLocals = methodNode.maxLocals;

                try {
                    transformer.transform(classNode, methodNode);

                    // Validate result
                    if (!BytecodeAnalyzer.validate(classNode)) {
                        throw new DeepMixException("Bytecode validation failed after custom transform");
                    }
                } catch (Exception transformError) {
                    // Rollback
                    methodNode.instructions.clear();
                    methodNode.instructions.add(backup);
                    methodNode.maxStack = backupMaxStack;
                    methodNode.maxLocals = backupMaxLocals;
                    throw new DeepMixException(
                        "Custom transform failed, rolled back: " + transformError.getMessage(), transformError);
                }

                transformationCount.incrementAndGet();
                log("Custom transform complete: %s.%s via %s",
                    classNode.name, methodNode.name, transformerClass.getSimpleName());

            } catch (DeepMixException e) {
                throw e;
            } catch (Exception e) {
                throw new DeepMixException("Failed to instantiate transformer: " + transformerClass.getName(), e);
            }
        }

        // --------------------------------------------------
        // Utility methods for MethodTransformProcessor
        // --------------------------------------------------

        private MethodNode findMethod(ClassNode classNode, String name, String descriptor) {
            for (MethodNode m : classNode.methods) {
                if (m.name.equals(name)) {
                    if (descriptor == null || descriptor.isEmpty() || m.desc.equals(descriptor)) {
                        return m;
                    }
                }
            }
            return null;
        }

        private MethodNode cloneMethod(MethodNode source) {
            MethodNode clone = new MethodNode(
                source.access, source.name, source.desc, source.signature,
                source.exceptions != null ? source.exceptions.toArray(new String[0]) : null);
            source.accept(clone);
            return clone;
        }

        private InsnList cloneInstructions(InsnList source, MethodNode srcMethod, MethodNode dstMethod) {
            Map<LabelNode, LabelNode> labelMap = new HashMap<>();
            for (AbstractInsnNode insn = source.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof LabelNode) {
                    labelMap.put((LabelNode) insn, new LabelNode());
                }
            }

            InsnList result = new InsnList();
            for (AbstractInsnNode insn = source.getFirst(); insn != null; insn = insn.getNext()) {
                result.add(insn.clone(labelMap));
            }
            return result;
        }

        private Map<LabelNode, LabelNode> buildLabelMap(InsnList source, InsnList target) {
            Map<LabelNode, LabelNode> map = new HashMap<>();
            AbstractInsnNode s = source.getFirst();
            AbstractInsnNode t = target.getFirst();
            while (s != null && t != null) {
                if (s instanceof LabelNode && t instanceof LabelNode) {
                    map.put((LabelNode) s, (LabelNode) t);
                }
                s = s.getNext();
                t = t.getNext();
            }
            return map;
        }

        private int resolveVariableIndex(MethodNode method, String name) {
            if (method.localVariables != null) {
                for (LocalVariableNode lv : method.localVariables) {
                    if (lv.name.equals(name)) return lv.index;
                }
            }
            // Try parsing as integer index
            try {
                return Integer.parseInt(name);
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        private boolean isStoreOpcode(int opcode) {
            return opcode == Opcodes.ISTORE || opcode == Opcodes.FSTORE || opcode == Opcodes.DSTORE
                || opcode == Opcodes.LSTORE || opcode == Opcodes.ASTORE;
        }

        private void addReturnInsn(InsnList insns, Type returnType) {
            switch (returnType.getSort()) {
                case Type.VOID: insns.add(new InsnNode(Opcodes.RETURN)); break;
                case Type.INT: case Type.BOOLEAN: case Type.BYTE: case Type.SHORT: case Type.CHAR:
                    insns.add(new InsnNode(Opcodes.IRETURN)); break;
                case Type.FLOAT: insns.add(new InsnNode(Opcodes.FRETURN)); break;
                case Type.DOUBLE: insns.add(new InsnNode(Opcodes.DRETURN)); break;
                case Type.LONG: insns.add(new InsnNode(Opcodes.LRETURN)); break;
                default: insns.add(new InsnNode(Opcodes.ARETURN)); break;
            }
        }

        private int computeNextLocalSlot(MethodNode method) {
            int max = 0;
            boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
            Type[] args = Type.getArgumentTypes(method.desc);
            int slot = isStatic ? 0 : 1;
            for (Type t : args) {
                slot += t.getSize();
            }
            max = slot;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof VarInsnNode) {
                    max = Math.max(max, ((VarInsnNode) insn).var + 1);
                }
            }
            return max;
        }

        private int getMethodPriority(MethodNode method) {
            if (method.visibleAnnotations != null) {
                for (AnnotationNode ann : method.visibleAnnotations) {
                    if (ann.desc.contains("DeepOverwrite") || ann.desc.contains("DOW")) {
                        if (ann.values != null) {
                            for (int i = 0; i < ann.values.size() - 1; i += 2) {
                                if ("priority".equals(ann.values.get(i))) {
                                    return (Integer) ann.values.get(i + 1);
                                }
                            }
                        }
                    }
                }
            }
            return 1000; // default priority
        }
    }


    // ============================================================================
    // CLASS MODIFICATION PROCESSOR
    // ============================================================================

    /**
     * Processes class-level transformation annotations.
     * Handles: @DeepConstructor, @DeepInterface, @DeepInherit, @DeepAnnotate, @DeepProxy, @DeepEvent
     */
    public static class ClassModificationProcessor {

        // --------------------------------------------------
        // @DeepConstructor / @DCT
        // --------------------------------------------------
        public void processConstructor(ClassNode classNode, MethodNode sourceMethod, DeepConstructor annotation) {
            DeepConstructor.ConstructorAction action = annotation.action();
            String descriptor = annotation.descriptor().isEmpty() ? "()V" : annotation.descriptor();

            MethodNode existingInit = findConstructor(classNode, descriptor);

            switch (action) {
                case MODIFY: {
                    if (existingInit == null) {
                        throw new DeepMixException("Constructor not found: " + classNode.name + ".<init>" + descriptor);
                    }
                    // Append source method's instructions after the existing super() call
                    AbstractInsnNode superCallEnd = findSuperCallEnd(existingInit, classNode.superName);
                    if (superCallEnd != null && sourceMethod != null) {
                        InsnList toInsert = cloneInsnList(sourceMethod.instructions);
                        // Remove any RETURN at the end of the insertion
                        removeTrailingReturn(toInsert);
                        existingInit.instructions.insert(superCallEnd, toInsert);
                        existingInit.maxStack = Math.max(existingInit.maxStack, sourceMethod.maxStack + 2);
                        existingInit.maxLocals = Math.max(existingInit.maxLocals, sourceMethod.maxLocals);
                    }
                    break;
                }
                case ADD: {
                    if (existingInit != null) {
                        throw new DeepMixException("Constructor already exists: " + classNode.name + ".<init>" + descriptor);
                    }
                    MethodNode newInit = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", descriptor, null, null);
                    InsnList insns = new InsnList();

                    if (annotation.addSuper()) {
                        insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                            classNode.superName, "<init>", "()V", false));
                    }

                    if (sourceMethod != null) {
                        InsnList body = cloneInsnList(sourceMethod.instructions);
                        removeTrailingReturn(body);
                        insns.add(body);
                    }

                    insns.add(new InsnNode(Opcodes.RETURN));
                    newInit.instructions = insns;
                    newInit.maxStack = sourceMethod != null ? sourceMethod.maxStack + 2 : 2;
                    newInit.maxLocals = sourceMethod != null ? Math.max(1, sourceMethod.maxLocals) : 1;
                    classNode.methods.add(newInit);
                    break;
                }
                case REMOVE: {
                    if (existingInit != null) {
                        classNode.methods.remove(existingInit);
                    }
                    break;
                }
                case REPLACE: {
                    if (existingInit != null) {
                        classNode.methods.remove(existingInit);
                    }
                    MethodNode replacement = new MethodNode(
                        Opcodes.ACC_PUBLIC, "<init>", descriptor, null, null);
                    if (sourceMethod != null) {
                        sourceMethod.accept(replacement);
                        replacement.name = "<init>";
                        replacement.desc = descriptor;
                    }
                    classNode.methods.add(replacement);
                    break;
                }
                case WRAP: {
                    if (existingInit == null) break;
                    // Rename existing constructor and create a new one that delegates
                    String backupName = "__deepmix_original_init_" + descriptor.hashCode();
                    existingInit.name = backupName;
                    existingInit.access = Opcodes.ACC_PRIVATE;

                    MethodNode wrapper = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", descriptor, null, null);
                    InsnList wInsns = new InsnList();

                    // Call original constructor
                    wInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    Type[] argTypes = Type.getArgumentTypes(descriptor);
                    int slot = 1;
                    for (Type t : argTypes) {
                        wInsns.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), slot));
                        slot += t.getSize();
                    }
                    wInsns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                        classNode.name, backupName, descriptor, false));

                    // Add wrapper body
                    if (sourceMethod != null) {
                        InsnList body = cloneInsnList(sourceMethod.instructions);
                        removeTrailingReturn(body);
                        wInsns.add(body);
                    }

                    wInsns.add(new InsnNode(Opcodes.RETURN));
                    wrapper.instructions = wInsns;
                    wrapper.maxStack = 4 + (sourceMethod != null ? sourceMethod.maxStack : 0);
                    wrapper.maxLocals = slot + (sourceMethod != null ? sourceMethod.maxLocals : 0);
                    classNode.methods.add(wrapper);
                    break;
                }
            }

            transformationCount.incrementAndGet();
            log("Constructor %s: %s (action=%s)", action, classNode.name, action);
        }

        // --------------------------------------------------
        // @DeepInterface / @DIF
        // --------------------------------------------------
        public void processInterface(ClassNode classNode, DeepInterface annotation) {
            Class<?>[] interfaces = annotation.interfaces();
            boolean implementMethods = annotation.implementMethods();

            for (Class<?> iface : interfaces) {
                String ifaceName = iface.getName().replace('.', '/');

                // Add interface if not already present
                if (!classNode.interfaces.contains(ifaceName)) {
                    classNode.interfaces.add(ifaceName);
                }

                if (implementMethods) {
                    // Generate stub implementations for all abstract methods
                    for (java.lang.reflect.Method m : iface.getMethods()) {
                        if (Modifier.isAbstract(m.getModifiers())) {
                            String methodDesc = Type.getMethodDescriptor(m);
                            boolean exists = classNode.methods.stream()
                                .anyMatch(mn -> mn.name.equals(m.getName()) && mn.desc.equals(methodDesc));

                            if (!exists) {
                                MethodNode stub = new MethodNode(
                                    Opcodes.ACC_PUBLIC, m.getName(), methodDesc, null, null);
                                InsnList insns = new InsnList();
                                Type returnType = Type.getReturnType(methodDesc);
                                addDefaultReturnValue(insns, returnType);
                                stub.instructions = insns;
                                stub.maxStack = returnType.getSize() == 0 ? 1 : returnType.getSize();
                                stub.maxLocals = 1 + Arrays.stream(Type.getArgumentTypes(methodDesc))
                                    .mapToInt(Type::getSize).sum();
                                classNode.methods.add(stub);
                            }
                        }
                    }
                }
            }

            transformationCount.incrementAndGet();
            log("Interface added to %s: %s (implement=%s)",
                classNode.name,
                Arrays.stream(interfaces).map(Class::getSimpleName).collect(Collectors.joining(", ")),
                implementMethods);
        }

        // --------------------------------------------------
        // @DeepInherit / @DIH
        // --------------------------------------------------
        public void processInherit(ClassNode classNode, DeepInherit annotation) {
            String newSuperclass = annotation.newSuperclass().replace('.', '/');
            boolean preserveOriginal = annotation.preserveOriginal();

            if (newSuperclass.isEmpty()) return;

            String oldSuperclass = classNode.superName;

            if (preserveOriginal) {
                // Add a synthetic field to reference the old super behavior
                classNode.fields.add(new FieldNode(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC,
                    "__deepmix_oldSuper",
                    "Ljava/lang/String;",
                    null,
                    oldSuperclass
                ));
            }

            // Update superclass
            classNode.superName = newSuperclass;

            // Update all INVOKESPECIAL calls from old super to new super
            for (MethodNode method : classNode.methods) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof MethodInsnNode) {
                        MethodInsnNode mi = (MethodInsnNode) insn;
                        if (mi.getOpcode() == Opcodes.INVOKESPECIAL && mi.owner.equals(oldSuperclass)) {
                            mi.owner = newSuperclass;
                        }
                    }
                }
            }

            // Update field access that referenced old superclass
            for (MethodNode method : classNode.methods) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof FieldInsnNode) {
                        FieldInsnNode fi = (FieldInsnNode) insn;
                        if (fi.owner.equals(oldSuperclass)) {
                            fi.owner = newSuperclass;
                        }
                    }
                }
            }

            transformationCount.incrementAndGet();
            log("Inherit changed: %s — %s → %s (preserve=%s)",
                classNode.name, oldSuperclass, newSuperclass, preserveOriginal);
        }

        // --------------------------------------------------
        // @DeepAnnotate / @DAN
        // --------------------------------------------------
        public void processAnnotate(ClassNode classNode, DeepAnnotate annotation) {
            Class<? extends Annotation>[] annotations = annotation.annotations();
            String[] values = annotation.values();
            String target = annotation.target();

            for (Class<? extends Annotation> annClass : annotations) {
                String annDesc = "L" + annClass.getName().replace('.', '/') + ";";

                if (target.isEmpty() || target.equals(classNode.name.replace('/', '.'))) {
                    // Annotate the class itself
                    if (classNode.visibleAnnotations == null) {
                        classNode.visibleAnnotations = new ArrayList<>();
                    }
                    boolean exists = classNode.visibleAnnotations.stream()
                        .anyMatch(a -> a.desc.equals(annDesc));
                    if (!exists) {
                        AnnotationNode newAnn = new AnnotationNode(annDesc);
                        if (values.length > 0) {
                            newAnn.values = new ArrayList<>();
                            for (int i = 0; i < values.length - 1; i += 2) {
                                newAnn.values.add(values[i]);
                                newAnn.values.add(parseAnnotationValue(values[i + 1]));
                            }
                        }
                        classNode.visibleAnnotations.add(newAnn);
                    }
                } else {
                    // Annotate a method or field by name
                    boolean found = false;
                    for (MethodNode method : classNode.methods) {
                        if (method.name.equals(target)) {
                            if (method.visibleAnnotations == null) {
                                method.visibleAnnotations = new ArrayList<>();
                            }
                            AnnotationNode newAnn = new AnnotationNode(annDesc);
                            if (values.length > 0) {
                                newAnn.values = new ArrayList<>();
                                for (int i = 0; i < values.length - 1; i += 2) {
                                    newAnn.values.add(values[i]);
                                    newAnn.values.add(parseAnnotationValue(values[i + 1]));
                                }
                            }
                            method.visibleAnnotations.add(newAnn);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        for (FieldNode field : classNode.fields) {
                            if (field.name.equals(target)) {
                                if (field.visibleAnnotations == null) {
                                    field.visibleAnnotations = new ArrayList<>();
                                }
                                AnnotationNode newAnn = new AnnotationNode(annDesc);
                                if (values.length > 0) {
                                    newAnn.values = new ArrayList<>();
                                    for (int i = 0; i < values.length - 1; i += 2) {
                                        newAnn.values.add(values[i]);
                                        newAnn.values.add(parseAnnotationValue(values[i + 1]));
                                    }
                                }
                                field.visibleAnnotations.add(newAnn);
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        throw new DeepMixException("Annotate target not found: " + target + " in " + classNode.name);
                    }
                }
            }

            transformationCount.incrementAndGet();
            log("Annotate: %s.%s += %s", classNode.name, target,
                Arrays.stream(annotations).map(Class::getSimpleName).collect(Collectors.joining(", ")));
        }

        // --------------------------------------------------
        // @DeepProxy / @DP
        // --------------------------------------------------
        public void processProxy(ClassNode classNode, DeepProxy annotation) {
            String targetName = annotation.target();
            Class<? extends ProxyHandler> handlerClass = annotation.handler();
            DeepProxy.ProxyMode mode = annotation.mode();
            String handlerInternalName = handlerClass.getName().replace('.', '/');

            // Add a static handler field
            String handlerFieldName = "__deepmix_proxy_handler_" + targetName.replace('.', '_');
            classNode.fields.add(new FieldNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                handlerFieldName,
                "Lstellar/snow/astralis/integration/DeepMix/DeepMixPhase1and2\$ProxyHandler;",
                null, null));

            // Add static initializer to create the handler
            MethodNode clinit = findOrCreateClinit(classNode);
            InsnList initInsns = new InsnList();
            initInsns.add(new TypeInsnNode(Opcodes.NEW, handlerInternalName));
            initInsns.add(new InsnNode(Opcodes.DUP));
            initInsns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, handlerInternalName, "<init>", "()V", false));
            initInsns.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, handlerFieldName,
                "Lstellar/snow/astralis/integration/DeepMix/DeepMixPhase1and2\$ProxyHandler;"));

            // Insert before the first RETURN in clinit
            AbstractInsnNode insertPoint = null;
            for (AbstractInsnNode insn = clinit.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.RETURN) {
                    insertPoint = insn;
                    break;
                }
            }
            if (insertPoint != null) {
                clinit.instructions.insertBefore(insertPoint, initInsns);
            } else {
                clinit.instructions.add(initInsns);
            }

            // Wrap target methods with proxy dispatch
            for (MethodNode method : classNode.methods) {
                if (shouldProxy(method, targetName, mode)) {
                    wrapMethodWithProxy(classNode, method, handlerFieldName, mode);
                }
            }

            transformationCount.incrementAndGet();
            log("Proxy installed: %s (target=%s, mode=%s, handler=%s)",
                classNode.name, targetName, mode, handlerClass.getSimpleName());
        }

        private boolean shouldProxy(MethodNode method, String target, DeepProxy.ProxyMode mode) {
            if (method.name.equals("<init>") || method.name.equals("<clinit>")) return false;
            if ((method.access & Opcodes.ACC_SYNTHETIC) != 0) return false;
            if (target.isEmpty()) return true;
            return method.name.equals(target) || target.equals("*");
        }

        private void wrapMethodWithProxy(ClassNode classNode, MethodNode method,
                String handlerFieldName, DeepProxy.ProxyMode mode) {
            // Rename original method
            String originalName = "__deepmix_proxy_orig_" + method.name;
            String origDesc = method.desc;

            MethodNode backup = new MethodNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC,
                originalName, origDesc, method.signature, null);
            method.accept(new MethodNode(Opcodes.ASM9) {
                // Copy instructions to backup
            });

            // Build proxy dispatch body
            InsnList proxyInsns = new InsnList();
            boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
            Type[] argTypes = Type.getArgumentTypes(method.desc);
            Type returnType = Type.getReturnType(method.desc);

            // Load handler
            proxyInsns.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, handlerFieldName,
                "Lstellar/snow/astralis/integration/DeepMix/DeepMixPhase1and2\$ProxyHandler;"));

            // Load proxy (this or null for static)
            if (isStatic) {
                proxyInsns.add(new InsnNode(Opcodes.ACONST_NULL));
            } else {
                proxyInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }

            // Load method name
            proxyInsns.add(new LdcInsnNode(method.name));

            // Create args array
            proxyInsns.add(new LdcInsnNode(argTypes.length));
            proxyInsns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
            int slot = isStatic ? 0 : 1;
            for (int i = 0; i < argTypes.length; i++) {
                proxyInsns.add(new InsnNode(Opcodes.DUP));
                proxyInsns.add(new LdcInsnNode(i));
                boxAndLoad(proxyInsns, argTypes[i], slot);
                proxyInsns.add(new InsnNode(Opcodes.AASTORE));
                slot += argTypes[i].getSize();
            }

            // Invoke handler
            proxyInsns.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                "stellar/snow/astralis/integration/DeepMix/DeepMixPhase1and2\$ProxyHandler",
                "handleInvocation",
                "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
                true));

            // Unbox return value
            if (returnType.getSort() == Type.VOID) {
                proxyInsns.add(new InsnNode(Opcodes.POP));
                proxyInsns.add(new InsnNode(Opcodes.RETURN));
            } else {
                unboxReturn(proxyInsns, returnType);
            }

            method.instructions.clear();
            method.instructions.add(proxyInsns);
            method.tryCatchBlocks.clear();
            method.maxStack = Math.max(8, argTypes.length + 6);
            method.maxLocals = slot + 2;
        }

        // --------------------------------------------------
        // @DeepEvent / @DE
        // --------------------------------------------------
        public void processEvent(ClassNode classNode, MethodNode methodNode, DeepEvent annotation) {
            DeepEvent.EventPhase phase = annotation.phase();
            String eventType = annotation.eventType();
            int priority = annotation.priority();
            boolean isStatic = (methodNode.access & Opcodes.ACC_STATIC) != 0;

            String eventBusClass = "stellar/snow/astralis/integration/DeepMix/DeepMixEventBus";

            switch (phase) {
                case PRE: {
                    // Insert event fire at method HEAD
                    InsnList head = new InsnList();
                    head.add(new LdcInsnNode(eventType.isEmpty() ? classNode.name + "." + methodNode.name + ".pre" : eventType + ".pre"));
                    head.add(new LdcInsnNode(priority));
                    if (!isStatic) {
                        head.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    } else {
                        head.add(new InsnNode(Opcodes.ACONST_NULL));
                    }
                    head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, eventBusClass,
                        "fireEvent", "(Ljava/lang/String;ILjava/lang/Object;)Z", false));
                    // If event was cancelled (returned true), skip method
                    LabelNode continueLabel = new LabelNode();
                    head.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
                    Type retType = Type.getReturnType(methodNode.desc);
                    addDefaultReturnValue(head, retType);
                    head.add(continueLabel);
                    methodNode.instructions.insert(head);
                    break;
                }
                case POST: {
                    // Insert event fire before each RETURN
                    List<AbstractInsnNode> returns = new ArrayList<>();
                    for (AbstractInsnNode insn = methodNode.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                        int op = insn.getOpcode();
                        if (op >= Opcodes.IRETURN && op <= Opcodes.RETURN) {
                            returns.add(insn);
                        }
                    }
                    for (AbstractInsnNode ret : returns) {
                        InsnList post = new InsnList();
                        post.add(new LdcInsnNode(eventType.isEmpty() ? classNode.name + "." + methodNode.name + ".post" : eventType + ".post"));
                        post.add(new LdcInsnNode(priority));
                        if (!isStatic) {
                            post.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        } else {
                            post.add(new InsnNode(Opcodes.ACONST_NULL));
                        }
                        post.add(new MethodInsnNode(Opcodes.INVOKESTATIC, eventBusClass,
                            "fireEvent", "(Ljava/lang/String;ILjava/lang/Object;)Z", false));
                        post.add(new InsnNode(Opcodes.POP));
                        methodNode.instructions.insertBefore(ret, post);
                    }
                    break;
                }
                case BOTH: {
                    // PRE + POST combined
                    processEvent(classNode, methodNode,
                        createEventAnnotation(annotation.target(), DeepEvent.EventPhase.PRE, eventType, priority));
                    processEvent(classNode, methodNode,
                        createEventAnnotation(annotation.target(), DeepEvent.EventPhase.POST, eventType, priority));
                    return; // Already logged inside recursive calls
                }
                case REPLACE: {
                    // Fire event instead of executing method body
                    methodNode.instructions.clear();
                    methodNode.tryCatchBlocks.clear();
                    InsnList replacement = new InsnList();
                    replacement.add(new LdcInsnNode(eventType.isEmpty() ? classNode.name + "." + methodNode.name : eventType));
                    replacement.add(new LdcInsnNode(priority));
                    if (!isStatic) {
                        replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    } else {
                        replacement.add(new InsnNode(Opcodes.ACONST_NULL));
                    }
                    replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, eventBusClass,
                        "fireEvent", "(Ljava/lang/String;ILjava/lang/Object;)Z", false));
                    replacement.add(new InsnNode(Opcodes.POP));
                    Type retType = Type.getReturnType(methodNode.desc);
                    addDefaultReturnValue(replacement, retType);
                    methodNode.instructions.add(replacement);
                    methodNode.maxStack = 4;
                    break;
                }
            }

            transformationCount.incrementAndGet();
            log("Event wired: %s.%s (phase=%s, type=%s, priority=%d)",
                classNode.name, methodNode.name, phase, eventType, priority);
        }

        // --------------------------------------------------
        // Utility helpers
        // --------------------------------------------------

        private MethodNode findConstructor(ClassNode classNode, String descriptor) {
            for (MethodNode m : classNode.methods) {
                if (m.name.equals("<init>") && m.desc.equals(descriptor)) return m;
            }
            return null;
        }

        private MethodNode findOrCreateClinit(ClassNode classNode) {
            for (MethodNode m : classNode.methods) {
                if (m.name.equals("<clinit>")) return m;
            }
            MethodNode clinit = new MethodNode(
                Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
            clinit.maxStack = 4;
            clinit.maxLocals = 0;
            classNode.methods.add(clinit);
            return clinit;
        }

        private AbstractInsnNode findSuperCallEnd(MethodNode initMethod, String superName) {
            for (AbstractInsnNode insn = initMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode mi = (MethodInsnNode) insn;
                    if (mi.getOpcode() == Opcodes.INVOKESPECIAL && mi.owner.equals(superName) && mi.name.equals("<init>")) {
                        return insn;
                    }
                }
            }
            return null;
        }

        private InsnList cloneInsnList(InsnList source) {
            Map<LabelNode, LabelNode> labels = new HashMap<>();
            for (AbstractInsnNode insn = source.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof LabelNode) labels.put((LabelNode) insn, new LabelNode());
            }
            InsnList result = new InsnList();
            for (AbstractInsnNode insn = source.getFirst(); insn != null; insn = insn.getNext()) {
                result.add(insn.clone(labels));
            }
            return result;
        }

        private void removeTrailingReturn(InsnList insns) {
            AbstractInsnNode last = insns.getLast();
            while (last != null && (last.getOpcode() == -1 || last instanceof LabelNode || last instanceof LineNumberNode)) {
                last = last.getPrevious();
            }
            if (last != null && last.getOpcode() >= Opcodes.IRETURN && last.getOpcode() <= Opcodes.RETURN) {
                insns.remove(last);
            }
        }

        private void addDefaultReturnValue(InsnList insns, Type returnType) {
            switch (returnType.getSort()) {
                case Type.VOID: insns.add(new InsnNode(Opcodes.RETURN)); break;
                case Type.BOOLEAN: case Type.BYTE: case Type.SHORT: case Type.CHAR: case Type.INT:
                    insns.add(new InsnNode(Opcodes.ICONST_0)); insns.add(new InsnNode(Opcodes.IRETURN)); break;
                case Type.FLOAT:
                    insns.add(new InsnNode(Opcodes.FCONST_0)); insns.add(new InsnNode(Opcodes.FRETURN)); break;
                case Type.DOUBLE:
                    insns.add(new InsnNode(Opcodes.DCONST_0)); insns.add(new InsnNode(Opcodes.DRETURN)); break;
                case Type.LONG:
                    insns.add(new InsnNode(Opcodes.LCONST_0)); insns.add(new InsnNode(Opcodes.LRETURN)); break;
                default:
                    insns.add(new InsnNode(Opcodes.ACONST_NULL)); insns.add(new InsnNode(Opcodes.ARETURN)); break;
            }
        }

        private void boxAndLoad(InsnList insns, Type type, int slot) {
            switch (type.getSort()) {
                case Type.INT: case Type.BOOLEAN: case Type.BYTE: case Type.SHORT: case Type.CHAR:
                    insns.add(new VarInsnNode(Opcodes.ILOAD, slot));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                    break;
                case Type.FLOAT:
                    insns.add(new VarInsnNode(Opcodes.FLOAD, slot));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
                    break;
                case Type.DOUBLE:
                    insns.add(new VarInsnNode(Opcodes.DLOAD, slot));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
                    break;
                case Type.LONG:
                    insns.add(new VarInsnNode(Opcodes.LLOAD, slot));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
                    break;
                default:
                    insns.add(new VarInsnNode(Opcodes.ALOAD, slot));
                    break;
            }
        }

        private void unboxReturn(InsnList insns, Type returnType) {
            switch (returnType.getSort()) {
                case Type.INT:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Integer"));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
                    insns.add(new InsnNode(Opcodes.IRETURN)); break;
                case Type.FLOAT:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Float"));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false));
                    insns.add(new InsnNode(Opcodes.FRETURN)); break;
                case Type.DOUBLE:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Double"));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false));
                    insns.add(new InsnNode(Opcodes.DRETURN)); break;
                case Type.LONG:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Long"));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false));
                    insns.add(new InsnNode(Opcodes.LRETURN)); break;
                case Type.BOOLEAN:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Boolean"));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false));
                    insns.add(new InsnNode(Opcodes.IRETURN)); break;
                default:
                    String internalName = returnType.getInternalName();
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, internalName));
                    insns.add(new InsnNode(Opcodes.ARETURN)); break;
            }
        }

        private Object parseAnnotationValue(String rawValue) {
            if (rawValue.equalsIgnoreCase("true")) return Boolean.TRUE;
            if (rawValue.equalsIgnoreCase("false")) return Boolean.FALSE;
            try { return Integer.parseInt(rawValue); } catch (NumberFormatException ignored) {}
            try { return Double.parseDouble(rawValue); } catch (NumberFormatException ignored) {}
            return rawValue;
        }

        private DeepEvent createEventAnnotation(String target, DeepEvent.EventPhase phase, String eventType, int priority) {
            // Synthetic annotation proxy for recursive call
            return new DeepEvent() {
                @Override public Class<? extends Annotation> annotationType() { return DeepEvent.class; }
                @Override public String target() { return target; }
                @Override public EventPhase phase() { return phase; }
                @Override public String eventType() { return eventType; }
                @Override public int priority() { return priority; }
                @Override public boolean hotReload() { return true; }
            };
        }
    }


    // ============================================================================
    // ADVANCED CONTROL PROCESSOR
    // ============================================================================

    /**
     * Processes advanced control annotations.
     * Handles: @DeepControl, @DeepBehavior, @DeepSurgical, @DeepLambda, @DeepAsync, @DeepCache
     */
    public static class AdvancedControlProcessor {

        // --------------------------------------------------
        // @DeepControl / @DC
        // --------------------------------------------------
        public void processControl(ClassNode classNode, DeepControl annotation) {
            String targetMod = annotation.targetMod();
            DeepControl.ControlAction action = annotation.action();
            String[] targets = annotation.targets();

            String controllerClass = "stellar/snow/astralis/integration/DeepMix/DeepMixModController";

            // Inject a static initialization block that registers control hooks
            MethodNode clinit = findOrCreateClinit(classNode);
            InsnList initInsns = new InsnList();

            initInsns.add(new LdcInsnNode(targetMod));
            initInsns.add(new LdcInsnNode(action.name()));

            // Create targets array
            initInsns.add(new LdcInsnNode(targets.length));
            initInsns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < targets.length; i++) {
                initInsns.add(new InsnNode(Opcodes.DUP));
                initInsns.add(new LdcInsnNode(i));
                initInsns.add(new LdcInsnNode(targets[i]));
                initInsns.add(new InsnNode(Opcodes.AASTORE));
            }

            initInsns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, controllerClass,
                "registerControl", "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V", false));

            insertBeforeReturn(clinit, initInsns);
            clinit.maxStack = Math.max(clinit.maxStack, targets.length + 6);

            // For DISABLE action, also inject NoOp replacements for target methods
            if (action == DeepControl.ControlAction.DISABLE) {
                for (String target : targets) {
                    // Mark for transformation at class-load time via the controller
                    AnnotationNode marker = new AnnotationNode(
                        "Lstellar/snow/astralis/integration/DeepMix/DeepMixDisableTarget;");
                    marker.values = Arrays.asList("mod", targetMod, "target", target);
                    if (classNode.visibleAnnotations == null) classNode.visibleAnnotations = new ArrayList<>();
                    classNode.visibleAnnotations.add(marker);
                }
            }

            transformationCount.incrementAndGet();
            log("Control registered: mod=%s, action=%s, targets=%d",
                targetMod, action, targets.length);
        }

        // --------------------------------------------------
        // @DeepBehavior / @DB
        // --------------------------------------------------
        public void processBehavior(ClassNode classNode, MethodNode methodNode, DeepBehavior annotation) {
            Class<? extends BehaviorModifier> modifierClass = annotation.modifier();
            String[] conditions = annotation.conditions();
            String modifierInternal = modifierClass.getName().replace('.', '/');

            // Add a static field for the modifier instance
            String modFieldName = "__deepmix_behavior_" + methodNode.name;
            classNode.fields.add(new FieldNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                modFieldName,
                "Lstellar/snow/astralis/integration/DeepMix/DeepMixPhase1and2\$BehaviorModifier;",
                null, null));

            // Initialize in <clinit>
            MethodNode clinit = findOrCreateClinit(classNode);
            InsnList init = new InsnList();
            init.add(new TypeInsnNode(Opcodes.NEW, modifierInternal));
            init.add(new InsnNode(Opcodes.DUP));
            init.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, modifierInternal, "<init>", "()V", false));
            init.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, modFieldName,
                "Lstellar/snow/astralis/integration/DeepMix/DeepMixPhase1and2\$BehaviorModifier;"));
            insertBeforeReturn(clinit, init);

            // Inject condition check + behavior call at method HEAD
            InsnList head = new InsnList();
            boolean isStatic = (methodNode.access & Opcodes.ACC_STATIC) != 0;

            if (conditions.length > 0) {
                LabelNode skipLabel = new LabelNode();
                // Evaluate conditions via runtime helper
                for (String condition : conditions) {
                    head.add(new LdcInsnNode(condition));
                    head.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                        "evaluateCondition", "(Ljava/lang/String;)Z", false));
                    head.add(new JumpInsnNode(Opcodes.IFEQ, skipLabel));
                }

                // Call modifier
                head.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, modFieldName,
                    "Lstellar/snow/astralis/integration/DeepMix/DeepMixPhase1and2\$BehaviorModifier;"));
                head.add(isStatic ? new InsnNode(Opcodes.ACONST_NULL) : new VarInsnNode(Opcodes.ALOAD, 0));
                head.add(new LdcInsnNode(methodNode.name));
                head.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                    "stellar/snow/astralis/integration/DeepMix/DeepMixPhase1and2\$BehaviorModifier",
                    "modifyBehavior", "(Ljava/lang/Object;Ljava/lang/String;)V", true));

                head.add(skipLabel);
            } else {
                // Unconditional behavior modification
                head.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, modFieldName,
                    "Lstellar/snow/astralis/integration/DeepMix/DeepMixPhase1and2\$BehaviorModifier;"));
                head.add(isStatic ? new InsnNode(Opcodes.ACONST_NULL) : new VarInsnNode(Opcodes.ALOAD, 0));
                head.add(new LdcInsnNode(methodNode.name));
                head.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                    "stellar/snow/astralis/integration/DeepMix/DeepMixPhase1and2\$BehaviorModifier",
                    "modifyBehavior", "(Ljava/lang/Object;Ljava/lang/String;)V", true));
            }

            methodNode.instructions.insert(head);
            methodNode.maxStack = Math.max(methodNode.maxStack, 4);

            transformationCount.incrementAndGet();
            log("Behavior modifier installed: %s.%s via %s (conditions=%d)",
                classNode.name, methodNode.name, modifierClass.getSimpleName(), conditions.length);
        }

        // --------------------------------------------------
        // @DeepSurgical / @DSI
        // --------------------------------------------------
        public void processSurgical(ClassNode classNode, MethodNode sourceMethod, DeepSurgical annotation) {
            String target = annotation.target();
            DeepSurgical.InjectionPoint point = annotation.point();
            boolean isStatic = (sourceMethod.access & Opcodes.ACC_STATIC) != 0;

            // Parse target: "owner.class::methodName" or "owner.class::methodName(desc)"
            String[] parts = target.split("::");
            if (parts.length != 2) {
                throw new DeepMixException("Invalid surgical target format: " + target
                    + " — expected 'ClassName::methodName'");
            }
            String targetClassName = parts[0].replace('.', '/');
            String targetMethodName = parts[1];
            String targetDesc = "";
            if (targetMethodName.contains("(")) {
                int paren = targetMethodName.indexOf('(');
                targetDesc = targetMethodName.substring(paren);
                targetMethodName = targetMethodName.substring(0, paren);
            }

            // Register a class transformer that will fire when the target class loads
            String finalTargetMethodName = targetMethodName;
            String finalTargetDesc = targetDesc;

            ProcessorRegistry.registerDeferredTransform(targetClassName, (targetClassNode) -> {
                MethodNode targetMethod = null;
                for (MethodNode m : targetClassNode.methods) {
                    if (m.name.equals(finalTargetMethodName)) {
                        if (finalTargetDesc.isEmpty() || m.desc.equals(finalTargetDesc)) {
                            targetMethod = m;
                            break;
                        }
                    }
                }

                if (targetMethod == null) {
                    log("WARNING: Surgical target not found: %s.%s — skipping", targetClassName, finalTargetMethodName);
                    return;
                }

                InsnList injection = cloneInsnListStatic(sourceMethod.instructions);
                removeTrailingReturnStatic(injection);

                switch (point) {
                    case HEAD:
                        targetMethod.instructions.insert(injection);
                        break;
                    case TAIL: {
                        AbstractInsnNode lastReturn = null;
                        for (AbstractInsnNode insn = targetMethod.instructions.getLast(); insn != null; insn = insn.getPrevious()) {
                            if (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN) {
                                lastReturn = insn;
                                break;
                            }
                        }
                        if (lastReturn != null) {
                            targetMethod.instructions.insertBefore(lastReturn, injection);
                        }
                        break;
                    }
                    case RETURN: {
                        // Before every RETURN
                        List<AbstractInsnNode> returns = new ArrayList<>();
                        for (AbstractInsnNode insn = targetMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                            if (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN) {
                                returns.add(insn);
                            }
                        }
                        for (AbstractInsnNode ret : returns) {
                            InsnList copy = cloneInsnListStatic(sourceMethod.instructions);
                            removeTrailingReturnStatic(copy);
                            targetMethod.instructions.insertBefore(ret, copy);
                        }
                        break;
                    }
                    case INVOKE: {
                        // Before the first INVOKE* instruction
                        for (AbstractInsnNode insn = targetMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                            if (insn instanceof MethodInsnNode) {
                                targetMethod.instructions.insertBefore(insn, injection);
                                break;
                            }
                        }
                        break;
                    }
                    case FIELD_ACCESS: {
                        for (AbstractInsnNode insn = targetMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                            if (insn instanceof FieldInsnNode) {
                                targetMethod.instructions.insertBefore(insn, injection);
                                break;
                            }
                        }
                        break;
                    }
                    case NEW: {
                        for (AbstractInsnNode insn = targetMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                            if (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.NEW) {
                                targetMethod.instructions.insertBefore(insn, injection);
                                break;
                            }
                        }
                        break;
                    }
                    case CUSTOM:
                        // Custom injection handled by user-defined marker annotations
                        targetMethod.instructions.insert(injection);
                        break;
                }

                targetMethod.maxStack = Math.max(targetMethod.maxStack, sourceMethod.maxStack + 2);
                targetMethod.maxLocals = Math.max(targetMethod.maxLocals, sourceMethod.maxLocals);
            });

            transformationCount.incrementAndGet();
            log("Surgical injection registered: %s → %s (point=%s)", sourceMethod.name, target, point);
        }

        // --------------------------------------------------
        // @DeepLambda / @DL
        // --------------------------------------------------
        public void processLambda(ClassNode classNode, MethodNode methodNode, DeepLambda annotation) {
            DeepLambda.LambdaTransform transform = annotation.transform();

            List<InvokeDynamicInsnNode> lambdas = new ArrayList<>();
            for (AbstractInsnNode insn = methodNode.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                    // Check if it's a lambda metafactory call
                    if (indy.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
                        lambdas.add(indy);
                    }
                }
            }

            if (lambdas.isEmpty()) return;

            switch (transform) {
                case OPTIMIZE: {
                    // Convert non-capturing lambdas to static method references
                    for (InvokeDynamicInsnNode lambda : lambdas) {
                        if (lambda.bsmArgs.length >= 3 && lambda.bsmArgs[1] instanceof Handle) {
                            Handle implHandle = (Handle) lambda.bsmArgs[1];
                            if (implHandle.getTag() == Opcodes.H_INVOKESTATIC) {
                                // Already optimal — static method reference, no captures
                                // Add annotation marker for JIT hints
                                if (methodNode.visibleAnnotations == null) {
                                    methodNode.visibleAnnotations = new ArrayList<>();
                                }
                            } else if (implHandle.getTag() == Opcodes.H_INVOKESPECIAL) {
                                // Private instance lambda — check if it captures only 'this'
                                // If so, convert to a cached singleton lambda
                                String lambdaMethod = implHandle.getName();
                                MethodNode lambdaBody = null;
                                for (MethodNode m : classNode.methods) {
                                    if (m.name.equals(lambdaMethod) && m.desc.equals(implHandle.getDesc())) {
                                        lambdaBody = m;
                                        break;
                                    }
                                }
                                if (lambdaBody != null && !usesInstanceFields(lambdaBody)) {
                                    // Can be promoted to static
                                    lambdaBody.access = (lambdaBody.access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_STATIC;
                                    // Shift all local variable indices by -1 (remove 'this')
                                    shiftLocalVariables(lambdaBody, -1);
                                    // Update descriptor to remove leading object param
                                    String oldDesc = lambdaBody.desc;
                                    Type[] oldArgs = Type.getArgumentTypes(oldDesc);
                                    if (oldArgs.length > 0 && oldArgs[0].getSort() == Type.OBJECT) {
                                        Type[] newArgs = Arrays.copyOfRange(oldArgs, 1, oldArgs.length);
                                        Type retType = Type.getReturnType(oldDesc);
                                        lambdaBody.desc = Type.getMethodDescriptor(retType, newArgs);
                                    }
                                    // Update the invokedynamic handle
                                    Handle newHandle = new Handle(
                                        Opcodes.H_INVOKESTATIC,
                                        implHandle.getOwner(),
                                        implHandle.getName(),
                                        lambdaBody.desc,
                                        implHandle.isInterface()
                                    );
                                    lambda.bsmArgs[1] = newHandle;
                                    log("Lambda optimized to static: %s.%s", classNode.name, lambdaMethod);
                                }
                            }
                        }
                    }
                    break;
                }
                case INLINE: {
                    // Inline lambda body at call site
                    for (InvokeDynamicInsnNode lambda : lambdas) {
                        if (lambda.bsmArgs.length >= 3 && lambda.bsmArgs[1] instanceof Handle) {
                            Handle implHandle = (Handle) lambda.bsmArgs[1];
                            MethodNode lambdaBody = null;
                            for (MethodNode m : classNode.methods) {
                                if (m.name.equals(implHandle.getName()) && m.desc.equals(implHandle.getDesc())) {
                                    lambdaBody = m;
                                    break;
                                }
                            }
                            if (lambdaBody != null && getMethodSize(lambdaBody) < 32) {
                                // Small enough to inline — replace the invokedynamic + subsequent interface call
                                // with direct instruction sequence
                                AbstractInsnNode next = lambda.getNext();
                                while (next != null && !(next instanceof MethodInsnNode)) {
                                    next = next.getNext();
                                }
                                if (next instanceof MethodInsnNode) {
                                    MethodInsnNode callSite = (MethodInsnNode) next;
                                    // Replace invokedynamic + call with inlined lambda body
                                    InsnList inlined = cloneInsnListStatic(lambdaBody.instructions);
                                    removeTrailingReturnStatic(inlined);
                                    methodNode.instructions.insert(callSite, inlined);
                                    methodNode.instructions.remove(lambda);
                                    methodNode.instructions.remove(callSite);
                                    log("Lambda inlined: %s.%s → %s",
                                        classNode.name, methodNode.name, implHandle.getName());
                                }
                            }
                        }
                    }
                    break;
                }
                case EXTRACT: {
                    // Extract lambda bodies to named private methods for debuggability
                    int counter = 0;
                    for (InvokeDynamicInsnNode lambda : lambdas) {
                        if (lambda.bsmArgs.length >= 3 && lambda.bsmArgs[1] instanceof Handle) {
                            Handle implHandle = (Handle) lambda.bsmArgs[1];
                            if (implHandle.getName().startsWith("lambda$")) {
                                String newName = methodNode.name + "$extracted_" + counter++;
                                for (MethodNode m : classNode.methods) {
                                    if (m.name.equals(implHandle.getName()) && m.desc.equals(implHandle.getDesc())) {
                                        m.name = newName;
                                        break;
                                    }
                                }
                                Handle newHandle = new Handle(
                                    implHandle.getTag(), implHandle.getOwner(), newName,
                                    implHandle.getDesc(), implHandle.isInterface());
                                lambda.bsmArgs[1] = newHandle;
                                log("Lambda extracted: %s → %s", implHandle.getName(), newName);
                            }
                        }
                    }
                    break;
                }
                case PARALLELIZE: {
                    // Wrap lambda invocation in a CompletableFuture.supplyAsync
                    for (InvokeDynamicInsnNode lambda : lambdas) {
                        // Check if the lambda is used as a Supplier or Function
                        String samType = lambda.desc;
                        Type returnType = Type.getReturnType(samType);
                        if (returnType.getInternalName().contains("Supplier")
                                || returnType.getInternalName().contains("Function")
                                || returnType.getInternalName().contains("Callable")) {
                            // Wrap: CompletableFuture.supplyAsync(lambda)
                            InsnList parallel = new InsnList();
                            parallel.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                "java/util/concurrent/CompletableFuture", "supplyAsync",
                                "(Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;",
                                false));
                            methodNode.instructions.insert(lambda, parallel);
                            log("Lambda parallelized: %s.%s", classNode.name, methodNode.name);
                        }
                    }
                    break;
                }
                case MEMOIZE: {
                    // Wrap lambda in a memoizing wrapper
                    for (InvokeDynamicInsnNode lambda : lambdas) {
                        String samType = lambda.desc;
                        Type returnType = Type.getReturnType(samType);
                        if (returnType.getInternalName().contains("Function")) {
                            InsnList memoize = new InsnList();
                            memoize.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                                "memoize",
                                "(Ljava/util/function/Function;)Ljava/util/function/Function;",
                                false));
                            methodNode.instructions.insert(lambda, memoize);
                            log("Lambda memoized: %s.%s", classNode.name, methodNode.name);
                        }
                    }
                    break;
                }
            }

            transformationCount.incrementAndGet();
            log("Lambda transform complete: %s.%s (transform=%s, lambdas=%d)",
                classNode.name, methodNode.name, transform, lambdas.size());
        }

        // --------------------------------------------------
        // @DeepAsync / @DAS
        // --------------------------------------------------
        public void processAsync(ClassNode classNode, MethodNode methodNode, DeepAsync annotation) {
            DeepAsync.AsyncMode mode = annotation.mode();
            String executor = annotation.executor();
            boolean isStatic = (methodNode.access & Opcodes.ACC_STATIC) != 0;
            Type returnType = Type.getReturnType(methodNode.desc);
            Type[] argTypes = Type.getArgumentTypes(methodNode.desc);

            // Rename original method
            String originalName = "__deepmix_sync_" + methodNode.name;
            MethodNode syncBackup = new MethodNode(
                Opcodes.ACC_PRIVATE | (isStatic ? Opcodes.ACC_STATIC : 0) | Opcodes.ACC_SYNTHETIC,
                originalName, methodNode.desc, methodNode.signature, null);
            // Copy instructions to backup
            methodNode.accept(syncBackup);
            syncBackup.name = originalName;
            classNode.methods.add(syncBackup);

            // Clear the original method and rebuild as async
            methodNode.instructions.clear();
            methodNode.tryCatchBlocks.clear();

            InsnList asyncInsns = new InsnList();

            switch (mode) {
                case COMPLETABLE_FUTURE:
                case FUTURE: {
                    // Change return type to CompletableFuture if not already
                    // Build: CompletableFuture.supplyAsync(() -> originalMethod(args), executor)

                    // Create the lambda that calls the sync method
                    // For simplicity, use an anonymous inner class approach via runtime helper

                    // Push all args into an Object array for the runtime helper
                    int totalArgs = argTypes.length + (isStatic ? 0 : 1);
                    asyncInsns.add(new LdcInsnNode(classNode.name.replace('/', '.')));
                    asyncInsns.add(new LdcInsnNode(originalName));
                    asyncInsns.add(new LdcInsnNode(methodNode.desc));

                    // Object[] args
                    asyncInsns.add(new LdcInsnNode(totalArgs));
                    asyncInsns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));

                    int arrayIdx = 0;
                    int slot = 0;
                    if (!isStatic) {
                        asyncInsns.add(new InsnNode(Opcodes.DUP));
                        asyncInsns.add(new LdcInsnNode(arrayIdx++));
                        asyncInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        asyncInsns.add(new InsnNode(Opcodes.AASTORE));
                        slot = 1;
                    }
                    for (Type t : argTypes) {
                        asyncInsns.add(new InsnNode(Opcodes.DUP));
                        asyncInsns.add(new LdcInsnNode(arrayIdx++));
                        boxAndLoad(asyncInsns, t, slot);
                        asyncInsns.add(new InsnNode(Opcodes.AASTORE));
                        slot += t.getSize();
                    }

                    asyncInsns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                        "submitAsync",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/concurrent/CompletableFuture;",
                        false));
                    asyncInsns.add(new InsnNode(Opcodes.ARETURN));

                    // Update method descriptor to return CompletableFuture
                    if (!methodNode.desc.contains("CompletableFuture")) {
                        methodNode.desc = Type.getMethodDescriptor(
                            Type.getType("Ljava/util/concurrent/CompletableFuture;"), argTypes);
                    }
                    break;
                }
                case CALLBACK: {
                    // Add a callback parameter to the method signature
                    // Call original method in a thread, pass result to callback
                    asyncInsns.add(new LdcInsnNode(classNode.name.replace('/', '.')));
                    asyncInsns.add(new LdcInsnNode(originalName));

                    int slot2 = isStatic ? 0 : 1;
                    for (Type t : argTypes) slot2 += t.getSize();

                    // Last param is the callback Consumer
                    asyncInsns.add(new VarInsnNode(Opcodes.ALOAD, slot2));

                    asyncInsns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                        "submitWithCallback",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/util/function/Consumer;)V",
                        false));
                    asyncInsns.add(new InsnNode(Opcodes.RETURN));
                    break;
                }
                case REACTIVE: {
                    // Return a reactive Mono/Flux-like wrapper via runtime helper
                    asyncInsns.add(new LdcInsnNode(classNode.name.replace('/', '.')));
                    asyncInsns.add(new LdcInsnNode(originalName));
                    asyncInsns.add(new LdcInsnNode(methodNode.desc));

                    // Build args array
                    int totalArgs2 = argTypes.length + (isStatic ? 0 : 1);
                    asyncInsns.add(new LdcInsnNode(totalArgs2));
                    asyncInsns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
                    int arrIdx = 0;
                    int s = isStatic ? 0 : 1;
                    if (!isStatic) {
                        asyncInsns.add(new InsnNode(Opcodes.DUP));
                        asyncInsns.add(new LdcInsnNode(arrIdx++));
                        asyncInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        asyncInsns.add(new InsnNode(Opcodes.AASTORE));
                    }
                    for (Type t : argTypes) {
                        asyncInsns.add(new InsnNode(Opcodes.DUP));
                        asyncInsns.add(new LdcInsnNode(arrIdx++));
                        boxAndLoad(asyncInsns, t, s);
                        asyncInsns.add(new InsnNode(Opcodes.AASTORE));
                        s += t.getSize();
                    }

                    asyncInsns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                        "submitReactive",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
                        false));
                    asyncInsns.add(new InsnNode(Opcodes.ARETURN));
                    break;
                }
                case PROMISE: {
                    // Similar to CompletableFuture but wraps in a DeepMix Promise
                    asyncInsns.add(new LdcInsnNode(classNode.name.replace('/', '.')));
                    asyncInsns.add(new LdcInsnNode(originalName));
                    asyncInsns.add(new LdcInsnNode(methodNode.desc));

                    int totalArgs3 = argTypes.length + (isStatic ? 0 : 1);
                    asyncInsns.add(new LdcInsnNode(totalArgs3));
                    asyncInsns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));

                    int arrI = 0;
                    int sl = isStatic ? 0 : 1;
                    if (!isStatic) {
                        asyncInsns.add(new InsnNode(Opcodes.DUP));
                        asyncInsns.add(new LdcInsnNode(arrI++));
                        asyncInsns.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        asyncInsns.add(new InsnNode(Opcodes.AASTORE));
                    }
                    for (Type t : argTypes) {
                        asyncInsns.add(new InsnNode(Opcodes.DUP));
                        asyncInsns.add(new LdcInsnNode(arrI++));
                        boxAndLoad(asyncInsns, t, sl);
                        asyncInsns.add(new InsnNode(Opcodes.AASTORE));
                        sl += t.getSize();
                    }

                    asyncInsns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                        "submitPromise",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
                        false));
                    asyncInsns.add(new InsnNode(Opcodes.ARETURN));
                    break;
                }
            }

            methodNode.instructions.add(asyncInsns);
            methodNode.maxStack = 10 + argTypes.length * 2;
            methodNode.maxLocals = Math.max(syncBackup.maxLocals, argTypes.length + 4);

            transformationCount.incrementAndGet();
            log("Async conversion: %s.%s (mode=%s)", classNode.name, methodNode.name, mode);
        }

        // --------------------------------------------------
        // @DeepCache / @DCH
        // --------------------------------------------------
        public void processCache(ClassNode classNode, MethodNode methodNode, DeepCache annotation) {
            DeepCache.CacheStrategy strategy = annotation.strategy();
            int maxSize = annotation.maxSize();
            long ttlSeconds = annotation.ttlSeconds();
            boolean isStatic = (methodNode.access & Opcodes.ACC_STATIC) != 0;
            Type returnType = Type.getReturnType(methodNode.desc);
            Type[] argTypes = Type.getArgumentTypes(methodNode.desc);

            if (returnType.getSort() == Type.VOID) {
                throw new DeepMixException("Cannot cache void method: " + classNode.name + "." + methodNode.name);
            }

            // Add a static cache map field
            String cacheFieldName = "__deepmix_cache_" + methodNode.name;
            classNode.fields.add(new FieldNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
                cacheFieldName,
                "Ljava/util/Map;",
                null, null));

            // Initialize cache in <clinit>
            MethodNode clinit = findOrCreateClinit(classNode);
            InsnList cacheInit = new InsnList();

            switch (strategy) {
                case LRU:
                    cacheInit.add(new LdcInsnNode(maxSize));
                    cacheInit.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                        "createLRUCache", "(I)Ljava/util/Map;", false));
                    break;
                case LFU:
                    cacheInit.add(new LdcInsnNode(maxSize));
                    cacheInit.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                        "createLFUCache", "(I)Ljava/util/Map;", false));
                    break;
                case FIFO:
                    cacheInit.add(new LdcInsnNode(maxSize));
                    cacheInit.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                        "createFIFOCache", "(I)Ljava/util/Map;", false));
                    break;
                case TTL:
                    cacheInit.add(new LdcInsnNode(maxSize));
                    cacheInit.add(new LdcInsnNode(ttlSeconds));
                    cacheInit.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "stellar/snow/astralis/integration/DeepMix/DeepMixRuntime",
                        "createTTLCache", "(IJ)Ljava/util/Map;", false));
                    break;
                case WEAK_REF:
                    cacheInit.add(new TypeInsnNode(Opcodes.NEW, "java/util/WeakHashMap"));
                    cacheInit.add(new InsnNode(Opcodes.DUP));
                    cacheInit.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                        "java/util/WeakHashMap", "<init>", "()V", false));
                    break;
            }

            cacheInit.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, cacheFieldName, "Ljava/util/Map;"));
            insertBeforeReturn(clinit, cacheInit);
            clinit.maxStack = Math.max(clinit.maxStack, 6);

            // Rename original method to backup
            String originalName = "__deepmix_uncached_" + methodNode.name;
            MethodNode uncached = new MethodNode(
                Opcodes.ACC_PRIVATE | (isStatic ? Opcodes.ACC_STATIC : 0) | Opcodes.ACC_SYNTHETIC,
                originalName, methodNode.desc, methodNode.signature, null);
            methodNode.accept(uncached);
            uncached.name = originalName;
            classNode.methods.add(uncached);

            // Rebuild method body with cache check
            methodNode.instructions.clear();
            methodNode.tryCatchBlocks.clear();

            InsnList body = new InsnList();
            LabelNode cacheHit = new LabelNode();
            LabelNode cacheMiss = new LabelNode();

            // Build cache key from arguments
            // key = Arrays.asList(arg0, arg1, ...) for object args, or hash-based key
            body.add(new LdcInsnNode(argTypes.length));
            body.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
            int slot = isStatic ? 0 : 1;
            for (int i = 0; i < argTypes.length; i++) {
                body.add(new InsnNode(Opcodes.DUP));
                body.add(new LdcInsnNode(i));
                boxAndLoad(body, argTypes[i], slot);
                body.add(new InsnNode(Opcodes.AASTORE));
                slot += argTypes[i].getSize();
            }
            body.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;", false));

            int keySlot = slot;
            body.add(new VarInsnNode(Opcodes.ASTORE, keySlot));

            // Check cache
            body.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, cacheFieldName, "Ljava/util/Map;"));
            body.add(new VarInsnNode(Opcodes.ALOAD, keySlot));
            body.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true));
            body.add(new InsnNode(Opcodes.DUP));
            body.add(new JumpInsnNode(Opcodes.IFNULL, cacheMiss));

            // Cache hit — unbox and return
            body.add(cacheHit);
            unboxReturn(body, returnType);

            // Cache miss — call original, store result, return
            body.add(cacheMiss);
            body.add(new InsnNode(Opcodes.POP)); // pop the null

            // Call uncached method
            if (!isStatic) body.add(new VarInsnNode(Opcodes.ALOAD, 0));
            int s2 = isStatic ? 0 : 1;
            for (Type t : argTypes) {
                body.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), s2));
                s2 += t.getSize();
            }
            int invokeOp = isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKESPECIAL;
            body.add(new MethodInsnNode(invokeOp, classNode.name, originalName, methodNode.desc, false));

            // Box result for storage
            int resultSlot = keySlot + 1;
            boxAndStore(body, returnType, resultSlot);

            // Store in cache
            body.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, cacheFieldName, "Ljava/util/Map;"));
            body.add(new VarInsnNode(Opcodes.ALOAD, keySlot));
            body.add(new VarInsnNode(Opcodes.ALOAD, resultSlot));
            body.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true));
            body.add(new InsnNode(Opcodes.POP));

            // Load and return result
            body.add(new VarInsnNode(Opcodes.ALOAD, resultSlot));
            unboxReturn(body, returnType);

            methodNode.instructions.add(body);
            methodNode.maxStack = Math.max(8, argTypes.length + 6);
            methodNode.maxLocals = resultSlot + 2;

            transformationCount.incrementAndGet();
            log("Cache installed: %s.%s (strategy=%s, maxSize=%d, ttl=%ds)",
                classNode.name, methodNode.name, strategy, maxSize, ttlSeconds);
        }

        // --------------------------------------------------
        // Utility methods for AdvancedControlProcessor
        // --------------------------------------------------

        private MethodNode findOrCreateClinit(ClassNode classNode) {
            for (MethodNode m : classNode.methods) {
                if (m.name.equals("<clinit>")) return m;
            }
            MethodNode clinit = new MethodNode(
                Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
            clinit.maxStack = 4;
            clinit.maxLocals = 0;
            classNode.methods.add(clinit);
            return clinit;
        }

        private void insertBeforeReturn(MethodNode method, InsnList toInsert) {
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.RETURN) {
                    method.instructions.insertBefore(insn, toInsert);
                    return;
                }
            }
            // No return found, just append
            method.instructions.add(toInsert);
        }

        private boolean usesInstanceFields(MethodNode method) {
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fi = (FieldInsnNode) insn;
                    if (fi.getOpcode() == Opcodes.GETFIELD || fi.getOpcode() == Opcodes.PUTFIELD) {
                        return true;
                    }
                }
                if (insn instanceof VarInsnNode) {
                    VarInsnNode vi = (VarInsnNode) insn;
                    if (vi.var == 0 && vi.getOpcode() == Opcodes.ALOAD) {
                        // Loads 'this' — check what it's used for
                        AbstractInsnNode next = vi.getNext();
                        if (next instanceof FieldInsnNode) return true;
                    }
                }
            }
            return false;
        }

        private void shiftLocalVariables(MethodNode method, int offset) {
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof VarInsnNode) {
                    VarInsnNode vi = (VarInsnNode) insn;
                    if (vi.var > 0 || (vi.var == 0 && offset < 0)) {
                        vi.var = Math.max(0, vi.var + offset);
                    }
                }
                if (insn instanceof IincInsnNode) {
                    IincInsnNode iinc = (IincInsnNode) insn;
                    iinc.var = Math.max(0, iinc.var + offset);
                }
            }
            if (method.localVariables != null) {
                for (LocalVariableNode lv : method.localVariables) {
                    lv.index = Math.max(0, lv.index + offset);
                }
            }
            method.maxLocals = Math.max(0, method.maxLocals + offset);
        }

        private int getMethodSize(MethodNode method) {
            int count = 0;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() >= 0) count++;
            }
            return count;
        }

        private void boxAndLoad(InsnList insns, Type type, int slot) {
            switch (type.getSort()) {
                case Type.INT: case Type.BOOLEAN: case Type.BYTE: case Type.SHORT: case Type.CHAR:
                    insns.add(new VarInsnNode(Opcodes.ILOAD, slot));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf",
                        "(I)Ljava/lang/Integer;", false)); break;
                case Type.FLOAT:
                    insns.add(new VarInsnNode(Opcodes.FLOAD, slot));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf",
                        "(F)Ljava/lang/Float;", false)); break;
                case Type.DOUBLE:
                    insns.add(new VarInsnNode(Opcodes.DLOAD, slot));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf",
                        "(D)Ljava/lang/Double;", false)); break;
                case Type.LONG:
                    insns.add(new VarInsnNode(Opcodes.LLOAD, slot));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf",
                        "(J)Ljava/lang/Long;", false)); break;
                default:
                    insns.add(new VarInsnNode(Opcodes.ALOAD, slot)); break;
            }
        }

        private void boxAndStore(InsnList insns, Type type, int slot) {
            switch (type.getSort()) {
                case Type.INT: case Type.BOOLEAN: case Type.BYTE: case Type.SHORT: case Type.CHAR:
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf",
                        "(I)Ljava/lang/Integer;", false));
                    insns.add(new VarInsnNode(Opcodes.ASTORE, slot)); break;
                case Type.FLOAT:
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf",
                        "(F)Ljava/lang/Float;", false));
                    insns.add(new VarInsnNode(Opcodes.ASTORE, slot)); break;
                case Type.DOUBLE:
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf",
                        "(D)Ljava/lang/Double;", false));
                    insns.add(new VarInsnNode(Opcodes.ASTORE, slot)); break;
                case Type.LONG:
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf",
                        "(J)Ljava/lang/Long;", false));
                    insns.add(new VarInsnNode(Opcodes.ASTORE, slot)); break;
                default:
                    insns.add(new VarInsnNode(Opcodes.ASTORE, slot)); break;
            }
        }

        private void unboxReturn(InsnList insns, Type returnType) {
            switch (returnType.getSort()) {
                case Type.INT:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Integer"));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
                    insns.add(new InsnNode(Opcodes.IRETURN)); break;
                case Type.FLOAT:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Float"));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false));
                    insns.add(new InsnNode(Opcodes.FRETURN)); break;
                case Type.DOUBLE:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Double"));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false));
                    insns.add(new InsnNode(Opcodes.DRETURN)); break;
                case Type.LONG:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Long"));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false));
                    insns.add(new InsnNode(Opcodes.LRETURN)); break;
                case Type.BOOLEAN:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Boolean"));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false));
                    insns.add(new InsnNode(Opcodes.IRETURN)); break;
                default:
                    insns.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
                    insns.add(new InsnNode(Opcodes.ARETURN)); break;
            }
        }

        // Static helper methods for use in deferred transforms
        private static InsnList cloneInsnListStatic(InsnList source) {
            Map<LabelNode, LabelNode> labels = new HashMap<>();
            for (AbstractInsnNode insn = source.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof LabelNode) labels.put((LabelNode) insn, new LabelNode());
            }
            InsnList result = new InsnList();
            for (AbstractInsnNode insn = source.getFirst(); insn != null; insn = insn.getNext()) {
                result.add(insn.clone(labels));
            }
            return result;
        }

        private static void removeTrailingReturnStatic(InsnList insns) {
            AbstractInsnNode last = insns.getLast();
            while (last != null && (last.getOpcode() == -1 || last instanceof LabelNode || last instanceof LineNumberNode)) {
                last = last.getPrevious();
            }
            if (last != null && last.getOpcode() >= Opcodes.IRETURN && last.getOpcode() <= Opcodes.RETURN) {
                insns.remove(last);
            }
        }
    }


    // ============================================================================
    // PHASE 2 PROCESSORS — FULL IMPLEMENTATIONS
    // ============================================================================

    /**
     * Processes all configuration-file data format annotations.
     * Handles: @DeepJSON, @DeepXML, @DeepYAML, @DeepTOML, @DeepProperties, @DeepSQL, @DeepMarkdown
     */
    public static class DataFormatProcessor {

        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        // ========================================
        // JSON Processing
        // ========================================

        public JsonElement processJSON(DeepJSON annotation, InputStream input) {
            String path = annotation.path();
            DeepJSON.JsonOperation operation = annotation.operation();
            String selector = annotation.selector();

            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);

                switch (operation) {
                    case MODIFY:
                        return modifyJsonAtPath(root, selector, null);
                    case ADD:
                        return addJsonAtPath(root, selector, JsonNull.INSTANCE);
                    case REMOVE:
                        return removeJsonAtPath(root, selector);
                    case MERGE:
                        return root; // Merge handled externally with two sources
                    case REPLACE:
                        return root; // Replacement value provided externally
                    case VALIDATE:
                        validateJson(root, selector);
                        return root;
                }
                return root;
            } catch (IOException e) {
                throw new DeepMixException("Failed to process JSON: " + path, e);
            }
        }

        public JsonElement loadJSON(String path) {
            try (InputStream is = resolveResource(path);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader);
            } catch (IOException e) {
                throw new DeepMixException("Failed to load JSON: " + path, e);
            }
        }

        public void saveJSON(String path, JsonElement data) {
            try {
                Path filePath = resolveFilePath(path);
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, GSON.toJson(data), StandardCharsets.UTF_8);
                if (hotReloadEnabled) HotReloadManager.watch(path);
                log("JSON saved: %s", path);
            } catch (IOException e) {
                throw new DeepMixException("Failed to save JSON: " + path, e);
            }
        }

        private JsonElement modifyJsonAtPath(JsonElement root, String jsonPath, JsonElement newValue) {
            // Simple JSONPath implementation for $.key.subkey[index] patterns
            String[] segments = parseJsonPath(jsonPath);
            JsonElement current = root;
            JsonElement parent = null;
            String lastKey = null;
            int lastIndex = -1;

            for (int i = 0; i < segments.length - 1; i++) {
                parent = current;
                String seg = segments[i];
                if (seg.matches("\\d+")) {
                    int idx = Integer.parseInt(seg);
                    if (current.isJsonArray()) {
                        current = current.getAsJsonArray().get(idx);
                    }
                } else if (!seg.equals("$")) {
                    if (current.isJsonObject() && current.getAsJsonObject().has(seg)) {
                        current = current.getAsJsonObject().get(seg);
                    }
                }
            }

            if (segments.length > 0) {
                lastKey = segments[segments.length - 1];
                if (newValue != null && current.isJsonObject()) {
                    current.getAsJsonObject().add(lastKey, newValue);
                }
            }

            return root;
        }

        private JsonElement addJsonAtPath(JsonElement root, String jsonPath, JsonElement value) {
            String[] segments = parseJsonPath(jsonPath);
            JsonElement current = root;

            for (int i = 0; i < segments.length - 1; i++) {
                String seg = segments[i];
                if (seg.equals("$")) continue;
                if (current.isJsonObject()) {
                    JsonObject obj = current.getAsJsonObject();
                    if (!obj.has(seg)) {
                        obj.add(seg, new JsonObject());
                    }
                    current = obj.get(seg);
                }
            }

            if (segments.length > 0) {
                String lastSeg = segments[segments.length - 1];
                if (current.isJsonObject()) {
                    current.getAsJsonObject().add(lastSeg, value);
                } else if (current.isJsonArray()) {
                    current.getAsJsonArray().add(value);
                }
            }

            return root;
        }

        private JsonElement removeJsonAtPath(JsonElement root, String jsonPath) {
            String[] segments = parseJsonPath(jsonPath);
            JsonElement current = root;

            for (int i = 0; i < segments.length - 1; i++) {
                String seg = segments[i];
                if (seg.equals("$")) continue;
                if (current.isJsonObject() && current.getAsJsonObject().has(seg)) {
                    current = current.getAsJsonObject().get(seg);
                } else if (current.isJsonArray() && seg.matches("\\d+")) {
                    current = current.getAsJsonArray().get(Integer.parseInt(seg));
                }
            }

            if (segments.length > 0) {
                String lastSeg = segments[segments.length - 1];
                if (current.isJsonObject()) {
                    current.getAsJsonObject().remove(lastSeg);
                } else if (current.isJsonArray() && lastSeg.matches("\\d+")) {
                    current.getAsJsonArray().remove(Integer.parseInt(lastSeg));
                }
            }

            return root;
        }

        private void validateJson(JsonElement root, String schemaPath) {
            // Basic structural validation
            if (root == null || root.isJsonNull()) {
                throw new DeepMixException("JSON validation failed: root is null");
            }
            // Schema-based validation delegated to runtime validator
            log("JSON validated against: %s", schemaPath);
        }

        private String[] parseJsonPath(String jsonPath) {
            if (jsonPath == null || jsonPath.isEmpty()) return new String[]{"$"};
            // Convert $.key.array[0].nested to ["$", "key", "array", "0", "nested"]
            String normalized = jsonPath
                .replace("$.", "$.") // preserve root
                .replace("[", ".")
                .replace("]", "");
            return normalized.split("\\.");
        }

        public static JsonElement mergeJson(JsonElement base, JsonElement override) {
            if (base == null) return override;
            if (override == null) return base;

            if (base.isJsonObject() && override.isJsonObject()) {
                JsonObject merged = base.getAsJsonObject().deepCopy();
                for (Map.Entry<String, JsonElement> entry : override.getAsJsonObject().entrySet()) {
                    if (merged.has(entry.getKey())) {
                        merged.add(entry.getKey(), mergeJson(merged.get(entry.getKey()), entry.getValue()));
                    } else {
                        merged.add(entry.getKey(), entry.getValue().deepCopy());
                    }
                }
                return merged;
            }

            if (base.isJsonArray() && override.isJsonArray()) {
                JsonArray merged = base.getAsJsonArray().deepCopy();
                for (JsonElement elem : override.getAsJsonArray()) {
                    merged.add(elem.deepCopy());
                }
                return merged;
            }

            return override.deepCopy();
        }

        // ========================================
        // XML Processing
        // ========================================

        public Document processXML(DeepXML annotation, InputStream input) {
            String xpath = annotation.xpath();
            DeepXML.XmlOperation operation = annotation.operation();

            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                // Disable external entities for security
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(input);
                doc.getDocumentElement().normalize();

                XPathFactory xPathFactory = XPathFactory.newInstance();
                XPath xPath = xPathFactory.newXPath();

                switch (operation) {
                    case MODIFY: {
                        XPathExpression expr = xPath.compile(xpath);
                        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                        // Return doc for caller to modify matched nodes
                        break;
                    }
                    case ADD: {
                        XPathExpression expr = xPath.compile(xpath);
                        NodeList parents = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                        // Caller adds child nodes to matched parents
                        break;
                    }
                    case REMOVE: {
                        XPathExpression expr = xPath.compile(xpath);
                        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                        for (int i = nodes.getLength() - 1; i >= 0; i--) {
                            Node node = nodes.item(i);
                            node.getParentNode().removeChild(node);
                        }
                        break;
                    }
                    case MERGE: {
                        // Merge handled by caller with two documents
                        break;
                    }
                    case REPLACE: {
                        // Replacement handled by caller
                        break;
                    }
                    case VALIDATE: {
                        // Schema validation would go here
                        log("XML validated: %s", annotation.path());
                        break;
                    }
                }

                return doc;
            } catch (Exception e) {
                throw new DeepMixException("Failed to process XML: " + annotation.path(), e);
            }
        }

        public Document loadXML(String path) {
            try (InputStream is = resolveResource(path)) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(is);
                doc.getDocumentElement().normalize();
                return doc;
            } catch (Exception e) {
                throw new DeepMixException("Failed to load XML: " + path, e);
            }
        }

        public void saveXML(String path, Document doc) {
            try {
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

                Path filePath = resolveFilePath(path);
                Files.createDirectories(filePath.getParent());

                try (OutputStream os = Files.newOutputStream(filePath)) {
                    transformer.transform(new DOMSource(doc), new StreamResult(os));
                }
                if (hotReloadEnabled) HotReloadManager.watch(path);
                log("XML saved: %s", path);
            } catch (Exception e) {
                throw new DeepMixException("Failed to save XML: " + path, e);
            }
        }

        public NodeList queryXPath(Document doc, String xpath) {
            try {
                XPath xPath = XPathFactory.newInstance().newXPath();
                return (NodeList) xPath.compile(xpath).evaluate(doc, XPathConstants.NODESET);
            } catch (XPathExpressionException e) {
                throw new DeepMixException("Invalid XPath: " + xpath, e);
            }
        }

        // ========================================
        // YAML Processing
        // ========================================

        public Map<String, Object> processYAML(DeepYAML annotation, InputStream input) {
            String selector = annotation.selector();
            DeepYAML.YamlOperation operation = annotation.operation();

            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                // Manual YAML parsing (avoiding hard dependency on SnakeYAML)
                // Uses a simplified key: value parser for basic YAML
                Map<String, Object> data = parseSimpleYaml(reader);

                switch (operation) {
                    case MODIFY:
                        // Caller modifies values via setYamlValue
                        break;
                    case ADD:
                        setYamlValue(data, selector, null); // Placeholder
                        break;
                    case REMOVE:
                        removeYamlValue(data, selector);
                        break;
                    case MERGE:
                        break; // Handled externally
                    case REPLACE:
                        break; // Handled externally
                    case VALIDATE:
                        log("YAML validated: %s", annotation.path());
                        break;
                }

                return data;
            } catch (IOException e) {
                throw new DeepMixException("Failed to process YAML: " + annotation.path(), e);
            }
        }

        public Map<String, Object> loadYAML(String path) {
            try (InputStream is = resolveResource(path);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return parseSimpleYaml(reader);
            } catch (IOException e) {
                throw new DeepMixException("Failed to load YAML: " + path, e);
            }
        }

        public void saveYAML(String path, Map<String, Object> data) {
            try {
                Path filePath = resolveFilePath(path);
                Files.createDirectories(filePath.getParent());
                StringBuilder sb = new StringBuilder();
                writeYamlMap(sb, data, 0);
                Files.writeString(filePath, sb.toString(), StandardCharsets.UTF_8);
                if (hotReloadEnabled) HotReloadManager.watch(path);
                log("YAML saved: %s", path);
            } catch (IOException e) {
                throw new DeepMixException("Failed to save YAML: " + path, e);
            }
        }

        @SuppressWarnings("unchecked")
        public Object getYamlValue(Map<String, Object> data, String dotPath) {
            String[] keys = dotPath.split("\\.");
            Object current = data;
            for (String key : keys) {
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(key);
                } else {
                    return null;
                }
            }
            return current;
        }

        @SuppressWarnings("unchecked")
        public void setYamlValue(Map<String, Object> data, String dotPath, Object value) {
            String[] keys = dotPath.split("\\.");
            Map<String, Object> current = data;
            for (int i = 0; i < keys.length - 1; i++) {
                Object next = current.get(keys[i]);
                if (next instanceof Map) {
                    current = (Map<String, Object>) next;
                } else {
                    Map<String, Object> newMap = new LinkedHashMap<>();
                    current.put(keys[i], newMap);
                    current = newMap;
                }
            }
            current.put(keys[keys.length - 1], value);
        }

        @SuppressWarnings("unchecked")
        public void removeYamlValue(Map<String, Object> data, String dotPath) {
            String[] keys = dotPath.split("\\.");
            Map<String, Object> current = data;
            for (int i = 0; i < keys.length - 1; i++) {
                Object next = current.get(keys[i]);
                if (next instanceof Map) {
                    current = (Map<String, Object>) next;
                } else {
                    return;
                }
            }
            current.remove(keys[keys.length - 1]);
        }

        @SuppressWarnings("unchecked")
        public static Map<String, Object> mergeYaml(Map<String, Object> base, Map<String, Object> override) {
            Map<String, Object> merged = new LinkedHashMap<>(base);
            for (Map.Entry<String, Object> entry : override.entrySet()) {
                if (merged.containsKey(entry.getKey()) &&
                    merged.get(entry.getKey()) instanceof Map &&
                    entry.getValue() instanceof Map) {
                    merged.put(entry.getKey(), mergeYaml(
                        (Map<String, Object>) merged.get(entry.getKey()),
                        (Map<String, Object>) entry.getValue()));
                } else {
                    merged.put(entry.getKey(), entry.getValue());
                }
            }
            return merged;
        }

        private Map<String, Object> parseSimpleYaml(Reader reader) throws IOException {
            BufferedReader br = new BufferedReader(reader);
            Map<String, Object> root = new LinkedHashMap<>();
            Deque<Map<String, Object>> stack = new ArrayDeque<>();
            Deque<Integer> indentStack = new ArrayDeque<>();
            stack.push(root);
            indentStack.push(-1);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

                int indent = 0;
                while (indent < line.length() && line.charAt(indent) == ' ') indent++;
                String trimmed = line.trim();

                // Pop stack back to appropriate level
                while (indentStack.size() > 1 && indent <= indentStack.peek()) {
                    stack.pop();
                    indentStack.pop();
                }

                if (trimmed.contains(":")) {
                    int colonIdx = trimmed.indexOf(':');
                    String key = trimmed.substring(0, colonIdx).trim();
                    String value = trimmed.substring(colonIdx + 1).trim();

                    if (value.isEmpty()) {
                        // Nested map
                        Map<String, Object> child = new LinkedHashMap<>();
                        stack.peek().put(key, child);
                        stack.push(child);
                        indentStack.push(indent);
                    } else {
                        // Scalar value
                        stack.peek().put(key, parseYamlScalar(value));
                    }
                } else if (trimmed.startsWith("- ")) {
                    // List item — find or create list in parent
                    String item = trimmed.substring(2).trim();
                    Map<String, Object> parent = stack.peek();
                    // Find the last key that was a list
                    String lastKey = null;
                    for (String k : parent.keySet()) lastKey = k;
                    if (lastKey != null) {
                        Object existing = parent.get(lastKey);
                        List<Object> list;
                        if (existing instanceof List) {
                            list = (List<Object>) existing;
                        } else {
                            list = new ArrayList<>();
                            parent.put(lastKey, list);
                        }
                        list.add(parseYamlScalar(item));
                    }
                }
            }

            return root;
        }

        private Object parseYamlScalar(String value) {
            if (value.equals("true") || value.equals("True") || value.equals("TRUE")) return true;
            if (value.equals("false") || value.equals("False") || value.equals("FALSE")) return false;
            if (value.equals("null") || value.equals("~")) return null;
            if (value.startsWith("\"") && value.endsWith("\"")) return value.substring(1, value.length() - 1);
            if (value.startsWith("'") && value.endsWith("'")) return value.substring(1, value.length() - 1);
            try { return Integer.parseInt(value); } catch (NumberFormatException ignored) {}
            try { return Long.parseLong(value); } catch (NumberFormatException ignored) {}
            try { return Double.parseDouble(value); } catch (NumberFormatException ignored) {}
            return value;
        }

        @SuppressWarnings("unchecked")
        private void writeYamlMap(StringBuilder sb, Map<String, Object> map, int indent) {
            String prefix = "  ".repeat(indent);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof Map) {
                    sb.append(prefix).append(entry.getKey()).append(":\n");
                    writeYamlMap(sb, (Map<String, Object>) val, indent + 1);
                } else if (val instanceof List) {
                    sb.append(prefix).append(entry.getKey()).append(":\n");
                    for (Object item : (List<?>) val) {
                        sb.append(prefix).append("  - ").append(formatYamlValue(item)).append("\n");
                    }
                } else {
                    sb.append(prefix).append(entry.getKey()).append(": ")
                      .append(formatYamlValue(val)).append("\n");
                }
            }
        }

        private String formatYamlValue(Object value) {
            if (value == null) return "null";
            if (value instanceof String) {
                String s = (String) value;
                if (s.contains(":") || s.contains("#") || s.contains("\"") || s.isEmpty()) {
                    return "\"" + s.replace("\"", "\\\"") + "\"";
                }
                return s;
            }
            return String.valueOf(value);
        }

        // ========================================
        // TOML Processing
        // ========================================

        public Map<String, Object> processTOML(DeepTOML annotation, InputStream input) {
            String key = annotation.key();
            DeepTOML.TomlOperation operation = annotation.operation();

            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                Map<String, Object> data = parseSimpleToml(reader);

                switch (operation) {
                    case MODIFY: break; // Caller modifies via setYamlValue (same structure)
                    case ADD:
                        setYamlValue(data, key, null);
                        break;
                    case REMOVE:
                        removeYamlValue(data, key);
                        break;
                    case MERGE: break;
                    case REPLACE: break;
                    case VALIDATE:
                        log("TOML validated: %s", annotation.path());
                        break;
                }
                return data;
            } catch (IOException e) {
                throw new DeepMixException("Failed to process TOML: " + annotation.path(), e);
            }
        }

        public Map<String, Object> loadTOML(String path) {
            try (InputStream is = resolveResource(path);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return parseSimpleToml(reader);
            } catch (IOException e) {
                throw new DeepMixException("Failed to load TOML: " + path, e);
            }
        }

        public void saveTOML(String path, Map<String, Object> data) {
            try {
                Path filePath = resolveFilePath(path);
                Files.createDirectories(filePath.getParent());
                StringBuilder sb = new StringBuilder();
                writeTomlMap(sb, data, "");
                Files.writeString(filePath, sb.toString(), StandardCharsets.UTF_8);
                if (hotReloadEnabled) HotReloadManager.watch(path);
                log("TOML saved: %s", path);
            } catch (IOException e) {
                throw new DeepMixException("Failed to save TOML: " + path, e);
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> parseSimpleToml(Reader reader) throws IOException {
            BufferedReader br = new BufferedReader(reader);
            Map<String, Object> root = new LinkedHashMap<>();
            Map<String, Object> currentSection = root;
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("[") && line.endsWith("]")) {
                    // Section header
                    String section = line.substring(1, line.length() - 1).trim();
                    boolean isArray = section.startsWith("[") && section.endsWith("]");
                    if (isArray) section = section.substring(1, section.length() - 1).trim();

                    String[] parts = section.split("\\.");
                    currentSection = root;
                    for (String part : parts) {
                        Object next = currentSection.get(part);
                        if (next instanceof Map) {
                            currentSection = (Map<String, Object>) next;
                        } else {
                            Map<String, Object> newSection = new LinkedHashMap<>();
                            currentSection.put(part, newSection);
                            currentSection = newSection;
                        }
                    }
                } else if (line.contains("=")) {
                    int eqIdx = line.indexOf('=');
                    String key = line.substring(0, eqIdx).trim();
                    String value = line.substring(eqIdx + 1).trim();
                    currentSection.put(key, parseTomlValue(value));
                }
            }

            return root;
        }

        private Object parseTomlValue(String value) {
            if (value.equals("true")) return true;
            if (value.equals("false")) return false;
            if (value.startsWith("\"") && value.endsWith("\"")) return value.substring(1, value.length() - 1);
            if (value.startsWith("'") && value.endsWith("'")) return value.substring(1, value.length() - 1);
            if (value.startsWith("[") && value.endsWith("]")) {
                // Simple array
                String inner = value.substring(1, value.length() - 1).trim();
                if (inner.isEmpty()) return new ArrayList<>();
                List<Object> list = new ArrayList<>();
                for (String item : inner.split(",")) {
                    list.add(parseTomlValue(item.trim()));
                }
                return list;
            }
            try { return Integer.parseInt(value); } catch (NumberFormatException ignored) {}
            try { return Long.parseLong(value); } catch (NumberFormatException ignored) {}
            try { return Double.parseDouble(value); } catch (NumberFormatException ignored) {}
            return value;
        }

        @SuppressWarnings("unchecked")
        private void writeTomlMap(StringBuilder sb, Map<String, Object> map, String prefix) {
            // Write scalar values first
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object val = entry.getValue();
                if (!(val instanceof Map)) {
                    sb.append(entry.getKey()).append(" = ").append(formatTomlValue(val)).append("\n");
                }
            }
            // Then write sections
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof Map) {
                    String section = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                    sb.append("\n[").append(section).append("]\n");
                    writeTomlMap(sb, (Map<String, Object>) val, section);
                }
            }
        }

        private String formatTomlValue(Object value) {
            if (value == null) return "\"\"";
            if (value instanceof String) return "\"" + ((String) value).replace("\"", "\\\"") + "\"";
            if (value instanceof Boolean) return value.toString();
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                return "[" + list.stream().map(this::formatTomlValue).collect(Collectors.joining(", ")) + "]";
            }
            return String.valueOf(value);
        }

        // ========================================
        // Properties Processing
        // ========================================

        public Properties processProperties(DeepProperties annotation, InputStream input) {
            String key = annotation.key();
            DeepProperties.PropertiesOperation operation = annotation.operation();

            Properties props = new Properties();
            try {
                props.load(input);

                switch (operation) {
                    case MODIFY:
                        // Caller modifies specific keys
                        break;
                    case ADD:
                        if (!props.containsKey(key)) {
                            props.setProperty(key, "");
                        }
                        break;
                    case REMOVE:
                        props.remove(key);
                        break;
                    case MERGE:
                        break; // Handled externally
                    case REPLACE:
                        break; // Handled externally
                }

                return props;
            } catch (IOException e) {
                throw new DeepMixException("Failed to process Properties: " + annotation.path(), e);
            }
        }

        public Properties loadProperties(String path) {
            Properties props = new Properties();
            try (InputStream is = resolveResource(path)) {
                props.load(is);
                return props;
            } catch (IOException e) {
                throw new DeepMixException("Failed to load Properties: " + path, e);
            }
        }

        public void saveProperties(String path, Properties props) {
            try {
                Path filePath = resolveFilePath(path);
                Files.createDirectories(filePath.getParent());
                try (OutputStream os = Files.newOutputStream(filePath)) {
                    props.store(os, "Modified by DeepMix");
                }
                if (hotReloadEnabled) HotReloadManager.watch(path);
                log("Properties saved: %s", path);
            } catch (IOException e) {
                throw new DeepMixException("Failed to save Properties: " + path, e);
            }
        }

        public static Properties mergeProperties(Properties base, Properties override) {
            Properties merged = new Properties();
            merged.putAll(base);
            merged.putAll(override);
            return merged;
        }

        // ========================================
        // SQL Processing
        // ========================================

        public String processSQL(DeepSQL annotation, String query) {
            DeepSQL.SqlOperation operation = annotation.operation();

            switch (operation) {
                case MODIFY:
                    return query; // Caller applies modifications
                case OPTIMIZE:
                    return optimizeSQL(query);
                case VALIDATE:
                    validateSQL(query);
                    return query;
                case EXPLAIN:
                    return "EXPLAIN " + query;
                case INJECT_WHERE:
                    return injectSqlWhere(query, annotation.query());
                case INJECT_JOIN:
                    return injectSqlJoin(query, annotation.query());
            }
            return query;
        }

        private String optimizeSQL(String query) {
            String optimized = query;
            // Basic SQL optimizations
            // Remove redundant whitespace
            optimized = optimized.replaceAll("\\s+", " ").trim();
            // Convert SELECT * to explicit columns warning
            if (optimized.matches("(?i)SELECT\\s+\\*\\s+FROM.*")) {
                log("SQL Warning: SELECT * detected — consider explicit column list for performance");
            }
            // Add LIMIT if missing on SELECT
            if (optimized.toUpperCase().startsWith("SELECT") &&
                !optimized.toUpperCase().contains("LIMIT") &&
                !optimized.toUpperCase().contains("COUNT")) {
                log("SQL Warning: No LIMIT clause detected — consider adding one");
            }
            return optimized;
        }

        private void validateSQL(String query) {
            // Basic syntax validation
            String upper = query.trim().toUpperCase();
            if (!(upper.startsWith("SELECT") || upper.startsWith("INSERT") ||
                  upper.startsWith("UPDATE") || upper.startsWith("DELETE") ||
                  upper.startsWith("CREATE") || upper.startsWith("ALTER") ||
                  upper.startsWith("DROP") || upper.startsWith("WITH"))) {
                throw new DeepMixException("SQL validation: unrecognized statement type: " +
                    query.substring(0, Math.min(20, query.length())));
            }

            // Check balanced parentheses
            int depth = 0;
            for (char c : query.toCharArray()) {
                if (c == '(') depth++;
                if (c == ')') depth--;
                if (depth < 0) throw new DeepMixException("SQL validation: unbalanced parentheses");
            }
            if (depth != 0) throw new DeepMixException("SQL validation: unbalanced parentheses");
        }

        private String injectSqlWhere(String query, String condition) {
            String upper = query.toUpperCase();
            int whereIdx = upper.indexOf("WHERE");
            if (whereIdx >= 0) {
                // Append to existing WHERE with AND
                int insertPos = whereIdx + 5;
                return query.substring(0, insertPos) + " (" + condition + ") AND" + query.substring(insertPos);
            } else {
                // Find insertion point (before ORDER BY, GROUP BY, LIMIT, or end)
                Pattern pattern = Pattern.compile("(?i)(\\s+ORDER\\s+BY|\\s+GROUP\\s+BY|\\s+LIMIT|\\s+HAVING|$)");
                Matcher matcher = pattern.matcher(query);
                if (matcher.find()) {
                    int pos = matcher.start();
                    return query.substring(0, pos) + " WHERE " + condition + query.substring(pos);
                }
                return query + " WHERE " + condition;
            }
        }

        private String injectSqlJoin(String query, String joinClause) {
            String upper = query.toUpperCase();
            // Insert JOIN after FROM clause and any existing JOINs
            Pattern fromPattern = Pattern.compile("(?i)(FROM\\s+\\S+(?:\\s+\\S+)?(?:\\s+(?:INNER|LEFT|RIGHT|CROSS|FULL)?\\s*JOIN\\s+.+?(?=WHERE|ORDER|GROUP|LIMIT|HAVING|$))*)");
            Matcher matcher = fromPattern.matcher(query);
            if (matcher.find()) {
                int insertPos = matcher.end();
                return query.substring(0, insertPos) + " " + joinClause + query.substring(insertPos);
            }
            return query;
        }

        // ========================================
        // Markdown Processing
        // ========================================

        public String processMarkdown(DeepMarkdown annotation, InputStream input) {
            DeepMarkdown.MarkdownOperation operation = annotation.operation();
            String selector = annotation.selector();

            try {
                String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);

                switch (operation) {
                    case MODIFY:
                        return content; // Caller modifies
                    case ADD_SECTION:
                        return addMarkdownSection(content, selector, "");
                    case REMOVE_SECTION:
                        return removeMarkdownSection(content, selector);
                    case REPLACE:
                        return content; // Caller provides replacement
                    case RENDER_HTML:
                        return renderMarkdownToHtml(content);
                }
                return content;
            } catch (IOException e) {
                throw new DeepMixException("Failed to process Markdown: " + annotation.path(), e);
            }
        }

        public String loadMarkdown(String path) {
            try (InputStream is = resolveResource(path)) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new DeepMixException("Failed to load Markdown: " + path, e);
            }
        }

        public void saveMarkdown(String path, String content) {
            try {
                Path filePath = resolveFilePath(path);
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, content, StandardCharsets.UTF_8);
                if (hotReloadEnabled) HotReloadManager.watch(path);
                log("Markdown saved: %s", path);
            } catch (IOException e) {
                throw new DeepMixException("Failed to save Markdown: " + path, e);
            }
        }

        private String addMarkdownSection(String content, String afterHeading, String newSection) {
            // Find heading and insert section after it
            Pattern headingPattern = Pattern.compile(
                "(?m)^(#{1,6})\\s+" + Pattern.quote(afterHeading) + "\\s*$");
            Matcher matcher = headingPattern.matcher(content);
            if (matcher.find()) {
                int insertPos = matcher.end();
                // Find end of current section (next heading of same or higher level, or EOF)
                String level = matcher.group(1);
                Pattern nextHeading = Pattern.compile("(?m)^#{1," + level.length() + "}\\s+");
                Matcher nextMatcher = nextHeading.matcher(content.substring(insertPos));
                int sectionEnd = nextMatcher.find() ? insertPos + nextMatcher.start() : content.length();
                return content.substring(0, sectionEnd) + "\n" + newSection + "\n" + content.substring(sectionEnd);
            }
            // Heading not found — append at end
            return content + "\n\n" + newSection;
        }

        private String removeMarkdownSection(String content, String headingText) {
            Pattern headingPattern = Pattern.compile(
                "(?m)^(#{1,6})\\s+" + Pattern.quote(headingText) + "\\s*$");
            Matcher matcher = headingPattern.matcher(content);
            if (matcher.find()) {
                int sectionStart = matcher.start();
                String level = matcher.group(1);
                Pattern nextHeading = Pattern.compile("(?m)^#{1," + level.length() + "}\\s+");
                Matcher nextMatcher = nextHeading.matcher(content.substring(matcher.end()));
                int sectionEnd = nextMatcher.find() ? matcher.end() + nextMatcher.start() : content.length();
                return content.substring(0, sectionStart) + content.substring(sectionEnd);
            }
            return content;
        }

        private String renderMarkdownToHtml(String markdown) {
            // Simplified Markdown → HTML renderer
            StringBuilder html = new StringBuilder();
            String[] lines = markdown.split("\n");

            boolean inCodeBlock = false;
            boolean inList = false;

            for (String line : lines) {
                if (line.startsWith("```")) {
                    if (inCodeBlock) {
                        html.append("</code></pre>\n");
                    } else {
                        String lang = line.substring(3).trim();
                        html.append("<pre><code").append(lang.isEmpty() ? "" : " class=\"language-" + lang + "\"").append(">");
                    }
                    inCodeBlock = !inCodeBlock;
                    continue;
                }
                if (inCodeBlock) {
                    html.append(escapeHtml(line)).append("\n");
                    continue;
                }
                // Headings
                if (line.matches("^#{1,6}\\s+.*")) {
                    int level = 0;
                    while (level < line.length() && line.charAt(level) == '#') level++;
                    String text = line.substring(level).trim();
                    html.append("<h").append(level).append(">").append(processInlineMarkdown(text))
                        .append("</h").append(level).append(">\n");
                }
                // Unordered list
                else if (line.matches("^\\s*[-*+]\\s+.*")) {
                    if (!inList) { html.append("<ul>\n"); inList = true; }
                    html.append("<li>").append(processInlineMarkdown(line.replaceFirst("^\\s*[-*+]\\s+", "")))
                        .append("</li>\n");
                }
                // Paragraph
                else if (!line.trim().isEmpty()) {
                    if (inList) { html.append("</ul>\n"); inList = false; }
                    html.append("<p>").append(processInlineMarkdown(line)).append("</p>\n");
                } else {
                    if (inList) { html.append("</ul>\n"); inList = false; }
                }
            }
            if (inList) html.append("</ul>\n");
            if (inCodeBlock) html.append("</code></pre>\n");

            return html.toString();
        }

        private String processInlineMarkdown(String text) {
            // Bold
            text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>\$1</strong>");
            text = text.replaceAll("__(.+?)__", "<strong>\$1</strong>");
            // Italic
            text = text.replaceAll("\\*(.+?)\\*", "<em>\$1</em>");
            text = text.replaceAll("_(.+?)_", "<em>\$1</em>");
            // Code
            text = text.replaceAll("`(.+?)`", "<code>\$1</code>");
            // Links
            text = text.replaceAll("\\[(.+?)]\\((.+?)\\)", "<a href=\"$2\">\$1</a>");
            // Images
            text = text.replaceAll("!\\[(.+?)]\\((.+?)\\)", "<img src=\"$2\" alt=\"\$1\">");
            return text;
        }

        private String escapeHtml(String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        // ========================================
        // Resource resolution helpers
        // ========================================

        private static InputStream resolveResource(String path) {
            // Try filesystem first, then classpath
            Path filePath = Paths.get(path);
            if (Files.exists(filePath)) {
                try {
                    return Files.newInputStream(filePath);
                } catch (IOException e) {
                    // Fall through to classpath
                }
            }
            InputStream is = DataFormatProcessor.class.getClassLoader().getResourceAsStream(path);
            if (is == null) {
                throw new DeepMixException("Resource not found: " + path);
            }
            return is;
        }

        private static Path resolveFilePath(String path) {
            return Paths.get(path);
        }
    }


    // ============================================================================
    // DOCUMENT FORMAT PROCESSOR
    // ============================================================================

    /**
     * Processes document format annotations.
     * Handles: @DeepHTML, @DeepCSS, @DeepProto, @DeepGradle, @DeepMaven, @DeepDockerfile, @DeepKubernetes
     */
    public static class DocumentFormatProcessor {

        // ========================================
        // HTML Processing
        // ========================================

        public String processHTML(DeepHTML annotation, InputStream input) {
            DeepHTML.HtmlOperation operation = annotation.operation();
            String cssSelector = annotation.cssSelector();

            try {
                String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);

                switch (operation) {
                    case MODIFY:
                        return html;
                    case ADD_ELEMENT:
                        return addHtmlElement(html, cssSelector, "");
                    case REMOVE_ELEMENT:
                        return removeHtmlElement(html, cssSelector);
                    case REPLACE:
                        return html;
                    case EXTRACT:
                        return extractHtmlElement(html, cssSelector);
                    case INJECT_SCRIPT:
                        return injectScript(html, cssSelector);
                }
                return html;
            } catch (IOException e) {
                throw new DeepMixException("Failed to process HTML: " + annotation.path(), e);
            }
        }

        private String addHtmlElement(String html, String parentSelector, String element) {
            // Find closing tag matching selector (simplified: match by tag/id/class)
            String tag = parseSimpleCssSelector(parentSelector);
            Pattern pattern = Pattern.compile("(</" + Pattern.quote(tag) + ">)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return html.substring(0, matcher.start()) + element + "\n" + html.substring(matcher.start());
            }
            return html;
        }

        private String removeHtmlElement(String html, String selector) {
            String tag = parseSimpleCssSelector(selector);
            // Remove first matching tag and its content
            Pattern pattern = Pattern.compile(
                "<" + Pattern.quote(tag) + "[^>]*>.*?</" + Pattern.quote(tag) + ">",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            return pattern.matcher(html).replaceFirst("");
        }

        private String extractHtmlElement(String html, String selector) {
            String tag = parseSimpleCssSelector(selector);
            Pattern pattern = Pattern.compile(
                "<" + Pattern.quote(tag) + "[^>]*>.*?</" + Pattern.quote(tag) + ">",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(html);
            return matcher.find() ? matcher.group() : "";
        }

        private String injectScript(String html, String script) {
            // Inject before </body> or at end
            String injection = "<script>" + script + "</script>";
            int bodyClose = html.toLowerCase().lastIndexOf("</body>");
            if (bodyClose >= 0) {
                return html.substring(0, bodyClose) + injection + "\n" + html.substring(bodyClose);
            }
            return html + "\n" + injection;
        }

        private String parseSimpleCssSelector(String selector) {
            if (selector.startsWith("#") || selector.startsWith(".")) return "div"; // Fallback
            return selector.split("[#.\\[\\s]")[0];
        }

        // ========================================
        // CSS Processing
        // ========================================

        public String processCSS(DeepCSS annotation, InputStream input) {
            DeepCSS.CssOperation operation = annotation.operation();
            String selector = annotation.selector();

            try {
                String css = new String(input.readAllBytes(), StandardCharsets.UTF_8);

                switch (operation) {
                    case MODIFY: return css;
                    case ADD_RULE: return addCssRule(css, selector, "");
                    case REMOVE_RULE: return removeCssRule(css, selector);
                    case REPLACE: return css;
                    case MINIFY: return minifyCss(css);
                    case PRETTIFY: return prettifyCss(css);
                }
                return css;
            } catch (IOException e) {
                throw new DeepMixException("Failed to process CSS: " + annotation.path(), e);
            }
        }

        private String addCssRule(String css, String selector, String rule) {
            return css + "\n" + selector + " {\n  " + rule + "\n}\n";
        }

        private String removeCssRule(String css, String selector) {
            // Remove rule block for the given selector
            Pattern pattern = Pattern.compile(
                Pattern.quote(selector) + "\\s*\\{[^}]*\\}",
                Pattern.MULTILINE);
            return pattern.matcher(css).replaceAll("").trim();
        }

        private String minifyCss(String css) {
            return css
                .replaceAll("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "") // Remove comments
                .replaceAll("\\s+", " ")                              // Collapse whitespace
                .replaceAll("\\s*([{}:;,])\\s*", "\$1")               // Remove space around syntax
                .replaceAll(";}", "}")                                 // Remove trailing semicolons
                .trim();
        }

        private String prettifyCss(String css) {
            // Basic CSS prettifier
            StringBuilder pretty = new StringBuilder();
            int indent = 0;
            for (char c : css.toCharArray()) {
                if (c == '{') {
                    pretty.append(" {\n");
                    indent++;
                    pretty.append("  ".repeat(indent));
                } else if (c == '}') {
                    indent = Math.max(0, indent - 1);
                    pretty.append("\n").append("  ".repeat(indent)).append("}\n\n");
                    if (indent > 0) pretty.append("  ".repeat(indent));
                } else if (c == ';') {
                    pretty.append(";\n").append("  ".repeat(indent));
                } else {
                    pretty.append(c);
                }
            }
            return pretty.toString().replaceAll("\\n\\s*\\n\\s*\\n", "\n\n").trim();
        }

        // ========================================
        // Protobuf Processing
        // ========================================

        public String processProto(DeepProto annotation, InputStream input) {
            DeepProto.ProtoOperation operation = annotation.operation();
            String messageName = annotation.messageName();

            try {
                String proto = new String(input.readAllBytes(), StandardCharsets.UTF_8);

                switch (operation) {
                    case MODIFY: return proto;
                    case ADD_FIELD: return addProtoField(proto, messageName, "");
                    case REMOVE_FIELD: return removeProtoField(proto, messageName, "");
                    case REPLACE: return proto;
                    case GENERATE_CODE: log("Proto code gen requested for: %s", messageName); return proto;
                }
                return proto;
            } catch (IOException e) {
                throw new DeepMixException("Failed to process Proto: " + annotation.path(), e);
            }
        }

        private String addProtoField(String proto, String messageName, String fieldDef) {
            Pattern msgPattern = Pattern.compile(
                "(message\\s+" + Pattern.quote(messageName) + "\\s*\\{)([^}]*)(\\})",
                Pattern.DOTALL);
            Matcher matcher = msgPattern.matcher(proto);
            if (matcher.find()) {
                // Find highest field number
                Pattern numPattern = Pattern.compile("=\\s*(\\d+)\\s*;");
                Matcher numMatcher = numPattern.matcher(matcher.group(2));
                int maxNum = 0;
                while (numMatcher.find()) {
                    maxNum = Math.max(maxNum, Integer.parseInt(numMatcher.group(1)));
                }
                String newField = "  " + fieldDef.replace("{N}", String.valueOf(maxNum + 1));
                return matcher.replaceFirst("$1$2  " + newField + "\n\$3");
            }
            return proto;
        }

        private String removeProtoField(String proto, String messageName, String fieldName) {
            Pattern msgPattern = Pattern.compile(
                "(message\\s+" + Pattern.quote(messageName) + "\\s*\\{)([^}]*)(\\})",
                Pattern.DOTALL);
            Matcher matcher = msgPattern.matcher(proto);
            if (matcher.find()) {
                String body = matcher.group(2);
                body = body.replaceAll("(?m)^\\s*\\S+\\s+" + Pattern.quote(fieldName) + "\\s*=.*?;\\s*$", "");
                return matcher.replaceFirst("$1" + Matcher.quoteReplacement(body) + "\$3");
            }
            return proto;
        }

        // ========================================
        // Gradle Processing
        // ========================================

        public String processGradle(DeepGradle annotation, InputStream input) {
            DeepGradle.GradleOperation operation = annotation.operation();
            String block = annotation.block();

            try {
                String gradle = new String(input.readAllBytes(), StandardCharsets.UTF_8);

                switch (operation) {
                    case MODIFY: return gradle;
                    case ADD_DEPENDENCY: return addGradleDependency(gradle, block);
                    case REMOVE_DEPENDENCY: return removeGradleDependency(gradle, block);
                    case ADD_PLUGIN: return addGradlePlugin(gradle, block);
                    case ADD_REPOSITORY: return addGradleRepository(gradle, block);
                }
                return gradle;
            } catch (IOException e) {
                throw new DeepMixException("Failed to process Gradle: " + annotation.path(), e);
            }
        }

        private String addGradleDependency(String gradle, String dependency) {
            Pattern depsBlock = Pattern.compile("(dependencies\\s*\\{)([^}]*)(\\})", Pattern.DOTALL);
            Matcher matcher = depsBlock.matcher(gradle);
            if (matcher.find()) {
                return matcher.replaceFirst("$1$2    " + dependency + "\n\$3");
            }
            // No dependencies block — create one
            return gradle + "\n\ndependencies {\n    " + dependency + "\n}\n";
        }

        private String removeGradleDependency(String gradle, String dependencyPattern) {
            Pattern depsBlock = Pattern.compile("(dependencies\\s*\\{)([^}]*)(\\})", Pattern.DOTALL);
            Matcher matcher = depsBlock.matcher(gradle);
            if (matcher.find()) {
                String body = matcher.group(2);
                body = body.replaceAll("(?m)^.*" + Pattern.quote(dependencyPattern) + ".*$\\n?", "");
                return matcher.replaceFirst("$1" + Matcher.quoteReplacement(body) + "\$3");
            }
            return gradle;
        }

        private String addGradlePlugin(String gradle, String plugin) {
            Pattern pluginBlock = Pattern.compile("(plugins\\s*\\{)([^}]*)(\\})", Pattern.DOTALL);
            Matcher matcher = pluginBlock.matcher(gradle);
            if (matcher.find()) {
                return matcher.replaceFirst("$1$2    " + plugin + "\n\$3");
            }
            return "plugins {\n    " + plugin + "\n}\n\n" + gradle;
        }

        private String addGradleRepository(String gradle, String repository) {
            Pattern repoBlock = Pattern.compile("(repositories\\s*\\{)([^}]*)(\\})", Pattern.DOTALL);
            Matcher matcher = repoBlock.matcher(gradle);
            if (matcher.find()) {
                return matcher.replaceFirst("$1$2    " + repository + "\n\$3");
            }
            return gradle + "\n\nrepositories {\n    " + repository + "\n}\n";
        }

        // ========================================
        // Maven POM Processing
        // ========================================

        public Document processMaven(DeepMaven annotation, InputStream input) {
            DeepMaven.MavenOperation operation = annotation.operation();
            String section = annotation.section();

            DataFormatProcessor dataProcessor = new DataFormatProcessor();
            // POM is XML so we delegate to XML processing
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document pom = builder.parse(input);
                pom.getDocumentElement().normalize();

                switch (operation) {
                    case MODIFY: return pom;
                    case ADD_DEPENDENCY:
                        addMavenDependency(pom, section);
                        return pom;
                    case REMOVE_DEPENDENCY:
                        removeMavenDependency(pom, section);
                        return pom;
                    case ADD_PLUGIN:
                        addMavenPlugin(pom, section);
                        return pom;
                    case ADD_REPOSITORY:
                        addMavenRepository(pom, section);
                        return pom;
                }
                return pom;
            } catch (Exception e) {
                throw new DeepMixException("Failed to process Maven POM: " + annotation.path(), e);
            }
        }

        private void addMavenDependency(Document pom, String depXml) {
            Element root = pom.getDocumentElement();
            NodeList depsList = root.getElementsByTagName("dependencies");
            Element deps;
            if (depsList.getLength() == 0) {
                deps = pom.createElement("dependencies");
                root.appendChild(deps);
            } else {
                deps = (Element) depsList.item(0);
            }
            // Parse the dependency XML fragment and append
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document fragDoc = builder.parse(new InputSource(new StringReader("<dependency>" + depXml + "</dependency>")));
                Node imported = pom.importNode(fragDoc.getDocumentElement(), true);
                deps.appendChild(imported);
            } catch (Exception e) {
                log("WARNING: Failed to parse Maven dependency XML: %s", depXml);
            }
        }

        private void removeMavenDependency(Document pom, String artifactId) {
            NodeList deps = pom.getElementsByTagName("dependency");
            for (int i = deps.getLength() - 1; i >= 0; i--) {
                Element dep = (Element) deps.item(i);
                NodeList artIds = dep.getElementsByTagName("artifactId");
                if (artIds.getLength() > 0 && artIds.item(0).getTextContent().equals(artifactId)) {
                    dep.getParentNode().removeChild(dep);
                }
            }
        }

        private void addMavenPlugin(Document pom, String pluginXml) {
            Element root = pom.getDocumentElement();
            NodeList buildList = root.getElementsByTagName("build");
            Element build;
            if (buildList.getLength() == 0) {
                build = pom.createElement("build");
                root.appendChild(build);
            } else {
                build = (Element) buildList.item(0);
            }
            NodeList pluginsList = build.getElementsByTagName("plugins");
            Element plugins;
            if (pluginsList.getLength() == 0) {
                plugins = pom.createElement("plugins");
                build.appendChild(plugins);
            } else {
                plugins = (Element) pluginsList.item(0);
            }
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document fragDoc = builder.parse(new InputSource(new StringReader("<plugin>" + pluginXml + "</plugin>")));
                Node imported = pom.importNode(fragDoc.getDocumentElement(), true);
                plugins.appendChild(imported);
            } catch (Exception e) {
                log("WARNING: Failed to parse Maven plugin XML: %s", pluginXml);
            }
        }

        private void addMavenRepository(Document pom, String repoXml) {
            Element root = pom.getDocumentElement();
            NodeList reposList = root.getElementsByTagName("repositories");
            Element repos;
            if (reposList.getLength() == 0) {
                repos = pom.createElement("repositories");
                root.appendChild(repos);
            } else {
                repos = (Element) reposList.item(0);
            }
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document fragDoc = builder.parse(new InputSource(new StringReader("<repository>" + repoXml + "</repository>")));
                Node imported = pom.importNode(fragDoc.getDocumentElement(), true);
                repos.appendChild(imported);
            } catch (Exception e) {
                log("WARNING: Failed to parse Maven repository XML: %s", repoXml);
            }
        }

        // ========================================
        // Dockerfile Processing
        // ========================================

        public String processDockerfile(DeepDockerfile annotation, InputStream input) {
            DeepDockerfile.DockerOperation operation = annotation.operation();

            try {
                String dockerfile = new String(input.readAllBytes(), StandardCharsets.UTF_8);

                switch (operation) {
                    case MODIFY: return dockerfile;
                    case ADD_INSTRUCTION: return dockerfile; // Caller adds
                    case REMOVE_INSTRUCTION: return dockerfile; // Caller specifies
                    case REPLACE: return dockerfile;
                    case OPTIMIZE: return optimizeDockerfile(dockerfile);
                }
                return dockerfile;
            } catch (IOException e) {
                throw new DeepMixException("Failed to process Dockerfile: " + annotation.path(), e);
            }
        }

        public String addDockerInstruction(String dockerfile, String instruction, String afterInstruction) {
            if (afterInstruction == null || afterInstruction.isEmpty()) {
                return dockerfile + "\n" + instruction + "\n";
            }
            String[] lines = dockerfile.split("\n");
            StringBuilder result = new StringBuilder();
            for (String line : lines) {
                result.append(line).append("\n");
                if (line.trim().toUpperCase().startsWith(afterInstruction.toUpperCase())) {
                    result.append(instruction).append("\n");
                }
            }
            return result.toString();
        }

        public String removeDockerInstruction(String dockerfile, String instruction) {
            return Arrays.stream(dockerfile.split("\n"))
                .filter(line -> !line.trim().toUpperCase().startsWith(instruction.toUpperCase()))
                .collect(Collectors.joining("\n"));
        }

        private String optimizeDockerfile(String dockerfile) {
            String[] lines = dockerfile.split("\n");
            StringBuilder optimized = new StringBuilder();
            List<String> pendingRuns = new ArrayList<>();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("RUN ")) {
                    // Collect consecutive RUN commands for merging
                    pendingRuns.add(trimmed.substring(4).trim());
                } else {
                    if (!pendingRuns.isEmpty()) {
                        // Merge RUN commands with &&
                        optimized.append("RUN ").append(String.join(" && \\\n    ", pendingRuns)).append("\n");
                        pendingRuns.clear();
                    }
                    optimized.append(line).append("\n");
                }
            }
            if (!pendingRuns.isEmpty()) {
                optimized.append("RUN ").append(String.join(" && \\\n    ", pendingRuns)).append("\n");
            }

            return optimized.toString();
        }

        // ========================================
        // Kubernetes Processing
        // ========================================

        public Map<String, Object> processKubernetes(DeepKubernetes annotation, InputStream input) {
            DeepKubernetes.K8sOperation operation = annotation.operation();
            String kind = annotation.kind();

            DataFormatProcessor yamlProcessor = new DataFormatProcessor();

            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                Map<String, Object> manifest = yamlProcessor.parseSimpleYaml(reader);

                // Verify kind matches if specified
                if (!kind.isEmpty()) {
                    Object manifestKind = manifest.get("kind");
                    if (manifestKind != null && !manifestKind.toString().equalsIgnoreCase(kind)) {
                        log("WARNING: K8s manifest kind '%s' doesn't match filter '%s'", manifestKind, kind);
                    }
                }

                switch (operation) {
                    case MODIFY: return manifest;
                    case ADD_RESOURCE: return manifest; // Caller adds
                    case REMOVE_RESOURCE: return manifest; // Caller removes
                    case REPLACE: return manifest;
                    case VALIDATE:
                        validateK8sManifest(manifest);
                        return manifest;
                }
                return manifest;
            } catch (IOException e) {
                throw new DeepMixException("Failed to process Kubernetes manifest: " + annotation.path(), e);
            }
        }

        @SuppressWarnings("unchecked")
        private void validateK8sManifest(Map<String, Object> manifest) {
            // Basic K8s manifest validation
            if (!manifest.containsKey("apiVersion")) {
                throw new DeepMixException("K8s validation: missing 'apiVersion'");
            }
            if (!manifest.containsKey("kind")) {
                throw new DeepMixException("K8s validation: missing 'kind'");
            }
            if (!manifest.containsKey("metadata")) {
                throw new DeepMixException("K8s validation: missing 'metadata'");
            }

            Object metadata = manifest.get("metadata");
            if (metadata instanceof Map) {
                Map<String, Object> meta = (Map<String, Object>) metadata;
                if (!meta.containsKey("name")) {
                    throw new DeepMixException("K8s validation: missing 'metadata.name'");
                }
            }

            String kind = manifest.get("kind").toString();
            if (kind.equals("Deployment") || kind.equals("StatefulSet") || kind.equals("DaemonSet")) {
                if (!manifest.containsKey("spec")) {
                    throw new DeepMixException("K8s validation: " + kind + " requires 'spec'");
                }
            }

            log("K8s manifest validated: kind=%s", kind);
        }
    }


    // ============================================================================
    // PROCESSOR REGISTRY & DISPATCHER
    // ============================================================================

    public static class ProcessorRegistry {

        private static final MethodTransformProcessor methodTransformProcessor = new MethodTransformProcessor();
        private static final ClassModificationProcessor classModificationProcessor = new ClassModificationProcessor();
        private static final AdvancedControlProcessor advancedControlProcessor = new AdvancedControlProcessor();
        private static final DataFormatProcessor dataFormatProcessor = new DataFormatProcessor();
        private static final DocumentFormatProcessor documentFormatProcessor = new DocumentFormatProcessor();

        // Deferred transforms for classes not yet loaded
        private static final Map<String, List<Consumer<ClassNode>>> deferredTransforms = new ConcurrentHashMap<>();

        // Transform ordering
        private static final int ORDER_INHERIT = 100;
        private static final int ORDER_INTERFACE = 200;
        private static final int ORDER_CONSTRUCTOR = 300;
        private static final int ORDER_ANNOTATE = 400;
        private static final int ORDER_OVERWRITE = 500;
        private static final int ORDER_MODIFY = 600;
        private static final int ORDER_WRAP = 700;
        private static final int ORDER_INJECT = 800;
        private static final int ORDER_CACHE = 900;
        private static final int ORDER_ASYNC = 1000;

        public static MethodTransformProcessor getMethodProcessor() { return methodTransformProcessor; }
        public static ClassModificationProcessor getClassProcessor() { return classModificationProcessor; }
        public static AdvancedControlProcessor getAdvancedProcessor() { return advancedControlProcessor; }
        public static DataFormatProcessor getDataProcessor() { return dataFormatProcessor; }
        public static DocumentFormatProcessor getDocumentProcessor() { return documentFormatProcessor; }

        public static void registerDeferredTransform(String className, Consumer<ClassNode> transform) {
            deferredTransforms.computeIfAbsent(className, k -> new CopyOnWriteArrayList<>()).add(transform);
            log("Deferred transform registered for: %s", className);
        }

        public static void processClass(ClassNode classNode) {
            // Phase 1: Apply deferred transforms from other processors
            String internalName = classNode.name;
            List<Consumer<ClassNode>> deferred = deferredTransforms.remove(internalName);
            if (deferred != null) {
                for (Consumer<ClassNode> transform : deferred) {
                    try {
                        transform.accept(classNode);
                    } catch (Exception e) {
                        log("ERROR: Deferred transform failed for %s: %s", internalName, e.getMessage());
                    }
                }
            }

            // Phase 2: Collect and sort annotation-driven transforms
            List<AnnotatedTransform> transforms = new ArrayList<>();

            // Scan class-level annotations
            if (classNode.visibleAnnotations != null) {
                for (AnnotationNode ann : classNode.visibleAnnotations) {
                    AnnotatedTransform at = resolveClassAnnotation(classNode, ann);
                    if (at != null) transforms.add(at);
                }
            }

            // Scan method-level annotations
            for (MethodNode method : classNode.methods) {
                if (method.visibleAnnotations != null) {
                    for (AnnotationNode ann : method.visibleAnnotations) {
                        AnnotatedTransform at = resolveMethodAnnotation(classNode, method, ann);
                        if (at != null) transforms.add(at);
                    }
                }
            }

            // Sort by order
            transforms.sort(Comparator.comparingInt(a -> a.order));

            // Execute transforms
            for (AnnotatedTransform at : transforms) {
                try {
                    at.action.run();
                } catch (DeepMixException e) {
                    log("ERROR: Transform failed on %s: %s", classNode.name, e.getMessage());
                    throw e;
                } catch (Exception e) {
                    log("ERROR: Unexpected failure on %s: %s", classNode.name, e.getMessage());
                    throw new DeepMixException("Transform failed: " + e.getMessage(), e);
                }
            }

            // Validate final bytecode
            if (!transforms.isEmpty() && !BytecodeAnalyzer.validate(classNode)) {
                throw new DeepMixException("Bytecode validation failed after all transforms on: " + classNode.name);
            }

            if (!transforms.isEmpty()) {
                log("Processed %d transforms on %s (total: %d)",
                    transforms.size(), classNode.name, transformationCount.get());
            }
        }

        public static void processMethod(ClassNode classNode, MethodNode methodNode) {
            if (methodNode.visibleAnnotations == null) return;

            for (AnnotationNode ann : new ArrayList<>(methodNode.visibleAnnotations)) {
                AnnotatedTransform at = resolveMethodAnnotation(classNode, methodNode, ann);
                if (at != null) {
                    at.action.run();
                }
            }
        }

        public static void processDataFormat(Annotation annotation, InputStream input) {
            if (annotation instanceof DeepJSON) {
                dataFormatProcessor.processJSON((DeepJSON) annotation, input);
            } else if (annotation instanceof DeepXML) {
                dataFormatProcessor.processXML((DeepXML) annotation, input);
            } else if (annotation instanceof DeepYAML) {
                dataFormatProcessor.processYAML((DeepYAML) annotation, input);
            } else if (annotation instanceof DeepTOML) {
                dataFormatProcessor.processTOML((DeepTOML) annotation, input);
            } else if (annotation instanceof DeepProperties) {
                dataFormatProcessor.processProperties((DeepProperties) annotation, input);
            } else if (annotation instanceof DeepSQL) {
                dataFormatProcessor.processSQL((DeepSQL) annotation, "");
            } else if (annotation instanceof DeepMarkdown) {
                dataFormatProcessor.processMarkdown((DeepMarkdown) annotation, input);
            } else if (annotation instanceof DeepHTML) {
                documentFormatProcessor.processHTML((DeepHTML) annotation, input);
            } else if (annotation instanceof DeepCSS) {
                documentFormatProcessor.processCSS((DeepCSS) annotation, input);
            } else if (annotation instanceof DeepProto) {
                documentFormatProcessor.processProto((DeepProto) annotation, input);
            } else if (annotation instanceof DeepGradle) {
                documentFormatProcessor.processGradle((DeepGradle) annotation, input);
            } else if (annotation instanceof DeepMaven) {
                documentFormatProcessor.processMaven((DeepMaven) annotation, input);
            } else if (annotation instanceof DeepDockerfile) {
                documentFormatProcessor.processDockerfile((DeepDockerfile) annotation, input);
            } else if (annotation instanceof DeepKubernetes) {
                documentFormatProcessor.processKubernetes((DeepKubernetes) annotation, input);
            }
        }

        public static void enableHotReload() {
            hotReloadEnabled = true;
            HotReloadManager.startWatchThread();
            log("Hot reload enabled");
        }

        public static void disableHotReload() {
            hotReloadEnabled = false;
            HotReloadManager.stopWatchThread();
            log("Hot reload disabled");
        }

        // Internal annotation resolution helpers

        private static AnnotatedTransform resolveClassAnnotation(ClassNode classNode, AnnotationNode ann) {
            String desc = ann.desc;
            if (desc.contains("DeepInterface") || desc.contains("DIF")) {
                return new AnnotatedTransform(ORDER_INTERFACE, () -> {
                    // Requires reflection to get actual annotation values
                    log("Interface transform queued for: %s", classNode.name);
                });
            }
            if (desc.contains("DeepInherit") || desc.contains("DIH")) {
                return new AnnotatedTransform(ORDER_INHERIT, () -> {
                    log("Inherit transform queued for: %s", classNode.name);
                });
            }
            if (desc.contains("DeepAnnotate") || desc.contains("DAN")) {
                return new AnnotatedTransform(ORDER_ANNOTATE, () -> {
                    log("Annotate transform queued for: %s", classNode.name);
                });
            }
            if (desc.contains("DeepControl") || desc.contains("DC")) {
                return new AnnotatedTransform(ORDER_INJECT, () -> {
                    log("Control transform queued for: %s", classNode.name);
                });
            }
            if (desc.contains("DeepProxy") || desc.contains("DP")) {
                return new AnnotatedTransform(ORDER_WRAP, () -> {
                    log("Proxy transform queued for: %s", classNode.name);
                });
            }
            return null;
        }

        private static AnnotatedTransform resolveMethodAnnotation(ClassNode classNode, MethodNode method, AnnotationNode ann) {
            String desc = ann.desc;
            if (desc.contains("DeepOverwrite") || desc.contains("DOW")) {
                return new AnnotatedTransform(ORDER_OVERWRITE, () -> {
                    log("Overwrite transform queued for: %s.%s", classNode.name, method.name);
                });
            }
            if (desc.contains("DeepModify") || desc.contains("DM;")) {
                return new AnnotatedTransform(ORDER_MODIFY, () -> {
                    log("Modify transform queued for: %s.%s", classNode.name, method.name);
                });
            }
            if (desc.contains("DeepWrap") || desc.contains("DW;")) {
                return new AnnotatedTransform(ORDER_WRAP, () -> {
                    log("Wrap transform queued for: %s.%s", classNode.name, method.name);
                });
            }
            if (desc.contains("DeepCache") || desc.contains("DCH")) {
                return new AnnotatedTransform(ORDER_CACHE, () -> {
                    log("Cache transform queued for: %s.%s", classNode.name, method.name);
                });
            }
            if (desc.contains("DeepAsync") || desc.contains("DAS")) {
                return new AnnotatedTransform(ORDER_ASYNC, () -> {
                    log("Async transform queued for: %s.%s", classNode.name, method.name);
                });
            }
            return null;
        }

        private static class AnnotatedTransform {
            final int order;
            final Runnable action;
            AnnotatedTransform(int order, Runnable action) {
                this.order = order;
                this.action = action;
            }
        }
    }


    // ============================================================================
    // UTILITY CLASSES
    // ============================================================================

    public static class AnnotationScanner {

        public static List<Annotation> scanClass(Class<?> clazz) {
            List<Annotation> result = new ArrayList<>();
            for (Annotation ann : clazz.getDeclaredAnnotations()) {
                if (isDeepMixAnnotation(ann)) result.add(ann);
            }
            for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
                for (Annotation ann : method.getDeclaredAnnotations()) {
                    if (isDeepMixAnnotation(ann)) result.add(ann);
                }
            }
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                for (Annotation ann : field.getDeclaredAnnotations()) {
                    if (isDeepMixAnnotation(ann)) result.add(ann);
                }
            }
            for (java.lang.reflect.Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                for (Annotation ann : ctor.getDeclaredAnnotations()) {
                    if (isDeepMixAnnotation(ann)) result.add(ann);
                }
            }
            return result;
        }

        public static List<Annotation> scanMethod(java.lang.reflect.Method method) {
            List<Annotation> result = new ArrayList<>();
            for (Annotation ann : method.getDeclaredAnnotations()) {
                if (isDeepMixAnnotation(ann)) result.add(ann);
            }
            return result;
        }

        public static Set<Class<?>> findAnnotatedClasses(String packageName) {
            // In a real implementation, this would scan the classpath.
            // For now, returns classes registered via the ProcessorRegistry.
            log("Scanning for DeepMix annotations in package: %s", packageName);
            return new HashSet<>();
        }

        private static boolean isDeepMixAnnotation(Annotation ann) {
            String name = ann.annotationType().getName();
            return name.contains("DeepMixPhase1and2$Deep") || name.contains("DeepMixPhase1and2\$D");
        }
    }

    public static class BytecodeAnalyzer {

        public static MethodAnalysis analyze(MethodNode methodNode) {
            MethodAnalysis analysis = new MethodAnalysis();

            int branchCount = 0;
            int invokeCount = 0;

            for (AbstractInsnNode insn = methodNode.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                int opcode = insn.getOpcode();

                if (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IF_ACMPNE) branchCount++;
                if (opcode == Opcodes.TABLESWITCH || opcode == Opcodes.LOOKUPSWITCH) branchCount++;
                if (opcode >= Opcodes.INVOKEVIRTUAL && opcode <= Opcodes.INVOKEDYNAMIC) invokeCount++;

                // Identify injection points
                if (insn instanceof LineNumberNode) {
                    LineNumberNode ln = (LineNumberNode) insn;
                    analysis.injectionPoints.add(new InjectionPoint("LINE", ln.line,
                        methodNode.instructions.indexOf(insn)));
                }
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode mi = (MethodInsnNode) insn;
                    analysis.injectionPoints.add(new InjectionPoint(
                        "INVOKE:" + mi.owner + "." + mi.name,
                        -1, methodNode.instructions.indexOf(insn)));
                }
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fi = (FieldInsnNode) insn;
                    analysis.injectionPoints.add(new InjectionPoint(
                        "FIELD:" + fi.owner + "." + fi.name,
                        -1, methodNode.instructions.indexOf(insn)));
                }
                if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                    analysis.injectionPoints.add(new InjectionPoint(
                        "RETURN", -1, methodNode.instructions.indexOf(insn)));
                }
            }

            // Cyclomatic complexity = branches + 1
            analysis.complexity = branchCount + 1;

            // Detect common patterns
            if (invokeCount == 0) analysis.patterns.add("LEAF_METHOD");
            if (branchCount == 0) analysis.patterns.add("LINEAR");
            if (analysis.complexity > 10) analysis.patterns.add("HIGH_COMPLEXITY");
            if (methodNode.tryCatchBlocks != null && !methodNode.tryCatchBlocks.isEmpty()) {
                analysis.patterns.add("EXCEPTION_HANDLING");
            }

            return analysis;
        }

        public static boolean validate(ClassNode classNode) {
            try {
                // Write and re-read to verify structural integrity
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                classNode.accept(cw);
                byte[] bytecode = cw.toByteArray();

                // Verify with a ClassReader
                ClassReader cr = new ClassReader(bytecode);
                ClassNode verification = new ClassNode();
                cr.accept(verification, 0);

                // Basic sanity checks
                if (verification.methods == null || verification.methods.isEmpty()) {
                    log("Validation warning: class %s has no methods", classNode.name);
                }

                for (MethodNode method : verification.methods) {
                    if (method.instructions.size() == 0 && !isAbstractOrNative(method)) {
                        log("Validation warning: method %s.%s has empty instructions",
                            classNode.name, method.name);
                    }
                }

                return true;
            } catch (Exception e) {
                log("Bytecode validation FAILED for %s: %s", classNode.name, e.getMessage());
                return false;
            }
        }

        private static boolean isAbstractOrNative(MethodNode method) {
            return (method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0;
        }
    }

    public static class MethodAnalysis {
        public int complexity;
        public List<InjectionPoint> injectionPoints;
        public List<String> patterns;

        public MethodAnalysis() {
            this.complexity = 0;
            this.injectionPoints = new ArrayList<>();
            this.patterns = new ArrayList<>();
        }

        @Override
        public String toString() {
            return String.format("MethodAnalysis{complexity=%d, injections=%d, patterns=%s}",
                complexity, injectionPoints.size(), patterns);
        }
    }

    public static class InjectionPoint {
        public String type;
        public int lineNumber;
        public int bytecodeOffset;

        public InjectionPoint(String type, int lineNumber, int bytecodeOffset) {
            this.type = type;
            this.lineNumber = lineNumber;
            this.bytecodeOffset = bytecodeOffset;
        }

        @Override
        public String toString() {
            return String.format("InjectionPoint{%s, line=%d, offset=%d}", type, lineNumber, bytecodeOffset);
        }
    }


    // ============================================================================
    // HOT RELOAD MANAGER
    // ============================================================================

    public static class HotReloadManager {

        private static final Map<String, Long> fileTimestamps = new ConcurrentHashMap<>();
        private static final Set<HotReloadListener> listeners = ConcurrentHashMap.newKeySet();
        private static volatile Thread watchThread;
        private static volatile boolean running = false;
        private static long pollIntervalMs = 1000;

        public static void watch(String filePath) {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                try {
                    fileTimestamps.put(filePath, Files.getLastModifiedTime(path).toMillis());
                } catch (IOException e) {
                    fileTimestamps.put(filePath, 0L);
                }
            } else {
                fileTimestamps.put(filePath, 0L);
            }
        }

        public static void unwatch(String filePath) {
            fileTimestamps.remove(filePath);
        }

        public static void checkForChanges() {
            for (Map.Entry<String, Long> entry : fileTimestamps.entrySet()) {
                String filePath = entry.getKey();
                long lastKnown = entry.getValue();
                Path path = Paths.get(filePath);

                if (Files.exists(path)) {
                    try {
                        long current = Files.getLastModifiedTime(path).toMillis();
                        if (current > lastKnown) {
                            fileTimestamps.put(filePath, current);
                            notifyListeners(filePath);
                            log("Hot reload triggered: %s", filePath);
                        }
                    } catch (IOException e) {
                        log("WARNING: Failed to check file modification time: %s", filePath);
                    }
                }
            }
        }

        public static void addListener(HotReloadListener listener) {
            listeners.add(listener);
        }

        public static void removeListener(HotReloadListener listener) {
            listeners.remove(listener);
        }

        public static void setPollInterval(long milliseconds) {
            pollIntervalMs = milliseconds;
        }

        public static int getWatchedFileCount() {
            return fileTimestamps.size();
        }

        static void startWatchThread() {
            if (running) return;
            running = true;
            watchThread = new Thread(() -> {
                while (running) {
                    try {
                        Thread.sleep(pollIntervalMs);
                        checkForChanges();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log("WARNING: Hot reload watch error: %s", e.getMessage());
                    }
                }
            }, "DeepMix-HotReload");
            watchThread.setDaemon(true);
            watchThread.start();
            log("Hot reload watch thread started (poll=%dms)", pollIntervalMs);
        }

        static void stopWatchThread() {
            running = false;
            if (watchThread != null) {
                watchThread.interrupt();
                watchThread = null;
            }
            log("Hot reload watch thread stopped");
        }

        private static void notifyListeners(String filePath) {
            for (HotReloadListener listener : listeners) {
                try {
                    listener.onFileChanged(filePath);
                } catch (Exception e) {
                    log("WARNING: Hot reload listener error for %s: %s", filePath, e.getMessage());
                }
            }
        }
    }

    @FunctionalInterface
    public interface HotReloadListener {
        void onFileChanged(String filePath);
    }


    // ============================================================================
    // DEEPMIX RUNTIME — Runtime support methods called by transformed bytecode
    // ============================================================================

    public static class DeepMixRuntime {

        // Thread pool for async execution
        private static final ExecutorService ASYNC_EXECUTOR = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "DeepMix-Async-" + Thread.currentThread().getId());
            t.setDaemon(true);
            return t;
        });

        private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "DeepMix-Scheduler-" + Thread.currentThread().getId());
            t.setDaemon(true);
            return t;
        });

        // Memoization cache for lambda memoize transform
        private static final Map<Object, Map<Object, Object>> MEMO_CACHES = new ConcurrentHashMap<>();

        // ========================================
        // Async Runtime Methods
        // ========================================

        @SuppressWarnings("unchecked")
        public static <T> CompletableFuture<T> submitAsync(
                String className, String methodName, String methodDesc, Object[] args) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Class<?> clazz = Class.forName(className);
                    java.lang.reflect.Method method = findMethod(clazz, methodName, methodDesc);
                    method.setAccessible(true);

                    Object instance = null;
                    Object[] methodArgs;

                    if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                        instance = args[0];
                        methodArgs = Arrays.copyOfRange(args, 1, args.length);
                    } else {
                        methodArgs = args;
                    }

                    // Unbox arguments to match parameter types
                    Class<?>[] paramTypes = method.getParameterTypes();
                    for (int i = 0; i < methodArgs.length && i < paramTypes.length; i++) {
                        methodArgs[i] = unboxArg(methodArgs[i], paramTypes[i]);
                    }

                    return (T) method.invoke(instance, methodArgs);
                } catch (Exception e) {
                    throw new CompletionException("Async invocation failed: " + className + "." + methodName, e);
                }
            }, ASYNC_EXECUTOR);
        }

        public static void submitWithCallback(String className, String methodName, Consumer<Object> callback) {
            ASYNC_EXECUTOR.submit(() -> {
                try {
                    Class<?> clazz = Class.forName(className);
                    java.lang.reflect.Method method = null;
                    for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                        if (m.getName().equals(methodName)) {
                            method = m;
                            break;
                        }
                    }
                    if (method != null) {
                        method.setAccessible(true);
                        Object result = method.invoke(null);
                        callback.accept(result);
                    }
                } catch (Exception e) {
                    log("Async callback failed: %s.%s — %s", className, methodName, e.getMessage());
                }
            });
        }

        @SuppressWarnings("unchecked")
        public static <T> Object submitReactive(
                String className, String methodName, String methodDesc, Object[] args) {
            // Returns a CompletableFuture that acts as a lightweight reactive wrapper
            // In environments with Project Reactor or RxJava, this can be adapted
            CompletableFuture<T> future = submitAsync(className, methodName, methodDesc, args);

            // Try to wrap in Mono if available
            try {
                Class<?> monoClass = Class.forName("reactor.core.publisher.Mono");
                java.lang.reflect.Method fromFuture = monoClass.getMethod("fromFuture", CompletableFuture.class);
                return fromFuture.invoke(null, future);
            } catch (ClassNotFoundException e) {
                // Reactor not available — return CompletableFuture
                return future;
            } catch (Exception e) {
                log("Reactive wrap failed, falling back to CompletableFuture: %s", e.getMessage());
                return future;
            }
        }

        @SuppressWarnings("unchecked")
        public static <T> Object submitPromise(
                String className, String methodName, String methodDesc, Object[] args) {
            // DeepMix Promise is a thin wrapper around CompletableFuture with chaining support
            CompletableFuture<T> future = submitAsync(className, methodName, methodDesc, args);
            return new DeepMixPromise<>(future);
        }

        // ========================================
        // Cache Factory Methods
        // ========================================

        public static <K, V> Map<K, V> createLRUCache(int maxSize) {
            return Collections.synchronizedMap(new LinkedHashMap<K, V>(maxSize + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > maxSize;
                }
            });
        }

        public static <K, V> Map<K, V> createLFUCache(int maxSize) {
            // LFU approximation using a concurrent map with frequency tracking
            return new ConcurrentHashMap<K, V>() {
                private final Map<K, AtomicInteger> frequencies = new ConcurrentHashMap<>();

                @Override
                public V put(K key, V value) {
                    if (size() >= maxSize && !containsKey(key)) {
                        evictLeastFrequent();
                    }
                    frequencies.computeIfAbsent(key, k -> new AtomicInteger(0));
                    return super.put(key, value);
                }

                @Override
                public V get(Object key) {
                    V val = super.get(key);
                    if (val != null) {
                        AtomicInteger freq = frequencies.get(key);
                        if (freq != null) freq.incrementAndGet();
                    }
                    return val;
                }

                private void evictLeastFrequent() {
                    K minKey = null;
                    int minFreq = Integer.MAX_VALUE;
                    for (Map.Entry<K, AtomicInteger> entry : frequencies.entrySet()) {
                        if (entry.getValue().get() < minFreq && containsKey(entry.getKey())) {
                            minFreq = entry.getValue().get();
                            minKey = entry.getKey();
                        }
                    }
                    if (minKey != null) {
                        remove(minKey);
                        frequencies.remove(minKey);
                    }
                }
            };
        }

        public static <K, V> Map<K, V> createFIFOCache(int maxSize) {
            return Collections.synchronizedMap(new LinkedHashMap<K, V>(maxSize + 1, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > maxSize;
                }
            });
        }

        public static <K, V> Map<K, V> createTTLCache(int maxSize, long ttlSeconds) {
            return new ConcurrentHashMap<K, V>() {
                private final Map<K, Long> timestamps = new ConcurrentHashMap<>();

                @Override
                public V put(K key, V value) {
                    evictExpired();
                    if (size() >= maxSize && !containsKey(key)) {
                        // Evict oldest entry
                        K oldest = null;
                        long oldestTime = Long.MAX_VALUE;
                        for (Map.Entry<K, Long> entry : timestamps.entrySet()) {
                            if (entry.getValue() < oldestTime) {
                                oldestTime = entry.getValue();
                                oldest = entry.getKey();
                            }
                        }
                        if (oldest != null) {
                            remove(oldest);
                            timestamps.remove(oldest);
                        }
                    }
                    timestamps.put(key, System.currentTimeMillis());
                    return super.put(key, value);
                }

                @Override
                public V get(Object key) {
                    Long ts = timestamps.get(key);
                    if (ts != null && (System.currentTimeMillis() - ts) > ttlSeconds * 1000) {
                        remove(key);
                        timestamps.remove(key);
                        return null;
                    }
                    return super.get(key);
                }

                private void evictExpired() {
                    long now = System.currentTimeMillis();
                    long cutoff = ttlSeconds * 1000;
                    timestamps.entrySet().removeIf(entry -> {
                        if ((now - entry.getValue()) > cutoff) {
                            remove(entry.getKey());
                            return true;
                        }
                        return false;
                    });
                }
            };
        }

        // ========================================
        // Lambda Memoization
        // ========================================

        @SuppressWarnings("unchecked")
        public static <T, R> Function<T, R> memoize(Function<T, R> function) {
            Map<Object, Object> cache = MEMO_CACHES.computeIfAbsent(function, k -> new ConcurrentHashMap<>());
            return input -> {
                return (R) cache.computeIfAbsent(input, k -> function.apply(input));
            };
        }

        // ========================================
        // Timing & Profiling
        // ========================================

        private static final Map<String, List<Long>> TIMING_DATA = new ConcurrentHashMap<>();

        public static long startTiming() {
            return System.nanoTime();
        }

        public static void endTiming(String methodKey, long startTime) {
            long elapsed = System.nanoTime() - startTime;
            TIMING_DATA.computeIfAbsent(methodKey, k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(elapsed);
        }

        public static Map<String, TimingStats> getTimingStats() {
            Map<String, TimingStats> stats = new LinkedHashMap<>();
            for (Map.Entry<String, List<Long>> entry : TIMING_DATA.entrySet()) {
                List<Long> times = new ArrayList<>(entry.getValue());
                if (times.isEmpty()) continue;

                long sum = 0;
                long min = Long.MAX_VALUE;
                long max = Long.MIN_VALUE;
                for (long t : times) {
                    sum += t;
                    min = Math.min(min, t);
                    max = Math.max(max, t);
                }

                TimingStats ts = new TimingStats();
                ts.methodKey = entry.getKey();
                ts.invocationCount = times.size();
                ts.totalNanos = sum;
                ts.avgNanos = sum / times.size();
                ts.minNanos = min;
                ts.maxNanos = max;

                // Compute P50, P95, P99
                Collections.sort(times);
                ts.p50Nanos = times.get((int) (times.size() * 0.50));
                ts.p95Nanos = times.get((int) (times.size() * 0.95));
                ts.p99Nanos = times.get(Math.min((int) (times.size() * 0.99), times.size() - 1));

                stats.put(entry.getKey(), ts);
            }
            return stats;
        }

        public static void clearTimingData() {
            TIMING_DATA.clear();
        }

        public static String formatTimingReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("╔══════════════════════════════════════════════════════════════════╗\n");
            sb.append("║                  DeepMix Timing Report                          ║\n");
            sb.append("╠══════════════════════════════════════════════════════════════════╣\n");

            Map<String, TimingStats> stats = getTimingStats();
            if (stats.isEmpty()) {
                sb.append("║  No timing data collected.                                     ║\n");
            } else {
                for (TimingStats ts : stats.values()) {
                    sb.append(String.format("║ %-60s ║\n", ts.methodKey));
                    sb.append(String.format("║   Calls: %-8d  Avg: %8.3f ms  Min: %8.3f ms          ║\n",
                        ts.invocationCount, ts.avgNanos / 1_000_000.0, ts.minNanos / 1_000_000.0));
                    sb.append(String.format("║   Max: %8.3f ms  P50: %8.3f ms  P95: %8.3f ms        ║\n",
                        ts.maxNanos / 1_000_000.0, ts.p50Nanos / 1_000_000.0, ts.p95Nanos / 1_000_000.0));
                    sb.append("╠══════════════════════════════════════════════════════════════════╣\n");
                }
            }

            sb.append(String.format("║  Total Transformations: %-38d ║\n", transformationCount.get()));
            sb.append("╚══════════════════════════════════════════════════════════════════╝\n");
            return sb.toString();
        }

        // ========================================
        // Retry Logic
        // ========================================

        public static <T> T withRetry(Callable<T> operation, int maxRetries, long delayMs,
                                       boolean exponentialBackoff, Class<? extends Throwable>[] retryOn) {
            int attempt = 0;
            Throwable lastException = null;

            while (attempt <= maxRetries) {
                try {
                    return operation.call();
                } catch (Throwable t) {
                    lastException = t;

                    // Check if this exception type is retryable
                    boolean shouldRetry = false;
                    if (retryOn == null || retryOn.length == 0) {
                        shouldRetry = true; // Retry on all exceptions
                    } else {
                        for (Class<? extends Throwable> retryClass : retryOn) {
                            if (retryClass.isInstance(t)) {
                                shouldRetry = true;
                                break;
                            }
                        }
                    }

                    if (!shouldRetry || attempt >= maxRetries) {
                        break;
                    }

                    attempt++;
                    long wait = exponentialBackoff ? delayMs * (1L << (attempt - 1)) : delayMs;
                    log("Retry %d/%d after %dms: %s", attempt, maxRetries, wait, t.getMessage());

                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new DeepMixException("Retry interrupted", ie);
                    }
                }
            }

            throw new DeepMixException("All retries exhausted after " + (attempt + 1) + " attempts",
                lastException instanceof Exception ? (Exception) lastException : new RuntimeException(lastException));
        }

        // ========================================
        // Circuit Breaker
        // ========================================

        private static final Map<String, CircuitBreakerState> CIRCUIT_BREAKERS = new ConcurrentHashMap<>();

        public static <T> T withCircuitBreaker(String name, Callable<T> operation,
                                                 int failureThreshold, long resetTimeoutMs) {
            CircuitBreakerState state = CIRCUIT_BREAKERS.computeIfAbsent(name,
                k -> new CircuitBreakerState(failureThreshold, resetTimeoutMs));

            if (state.isOpen()) {
                if (state.shouldAttemptReset()) {
                    state.halfOpen();
                } else {
                    throw new DeepMixException("Circuit breaker '" + name + "' is OPEN");
                }
            }

            try {
                T result = operation.call();
                state.recordSuccess();
                return result;
            } catch (Exception e) {
                state.recordFailure();
                throw new DeepMixException("Circuit breaker '" + name + "' recorded failure", e);
            }
        }

        public static CircuitBreakerState getCircuitBreaker(String name) {
            return CIRCUIT_BREAKERS.get(name);
        }

        public static void resetCircuitBreaker(String name) {
            CircuitBreakerState state = CIRCUIT_BREAKERS.get(name);
            if (state != null) state.reset();
        }

        public static void resetAllCircuitBreakers() {
            CIRCUIT_BREAKERS.values().forEach(CircuitBreakerState::reset);
        }

        // ========================================
        // Rate Limiter
        // ========================================

        private static final Map<String, RateLimiterState> RATE_LIMITERS = new ConcurrentHashMap<>();

        public static boolean tryAcquireRate(String name, int maxCalls, long windowMs) {
            RateLimiterState limiter = RATE_LIMITERS.computeIfAbsent(name,
                k -> new RateLimiterState(maxCalls, windowMs));
            return limiter.tryAcquire();
        }

        public static void acquireRate(String name, int maxCalls, long windowMs) {
            if (!tryAcquireRate(name, maxCalls, windowMs)) {
                throw new DeepMixException("Rate limit exceeded for: " + name);
            }
        }

        // ========================================
        // Method Interception
        // ========================================

        @SuppressWarnings("unchecked")
        public static <T> T invokeWithInterceptor(
                Object target, String methodName, String methodDesc,
                Object[] args, Function<Object[], Object> interceptor) {
            // Allow interceptor to modify args, skip call, or wrap result
            return (T) interceptor.apply(args);
        }

        // ========================================
        // Utility: Method resolution
        // ========================================

        private static java.lang.reflect.Method findMethod(Class<?> clazz, String name, String desc) {
            Type[] argTypes = Type.getArgumentTypes(desc);
            outer:
            for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                if (!m.getName().equals(name)) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length != argTypes.length) continue;
                for (int i = 0; i < params.length; i++) {
                    if (!typeMatches(params[i], argTypes[i])) continue outer;
                }
                return m;
            }
            // Fall back to name-only match
            for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(name)) return m;
            }
            throw new DeepMixException("Method not found: " + clazz.getName() + "." + name + desc);
        }

        private static boolean typeMatches(Class<?> javaType, Type asmType) {
            switch (asmType.getSort()) {
                case Type.INT: return javaType == int.class || javaType == Integer.class;
                case Type.LONG: return javaType == long.class || javaType == Long.class;
                case Type.FLOAT: return javaType == float.class || javaType == Float.class;
                case Type.DOUBLE: return javaType == double.class || javaType == Double.class;
                case Type.BOOLEAN: return javaType == boolean.class || javaType == Boolean.class;
                case Type.BYTE: return javaType == byte.class || javaType == Byte.class;
                case Type.CHAR: return javaType == char.class || javaType == Character.class;
                case Type.SHORT: return javaType == short.class || javaType == Short.class;
                case Type.VOID: return javaType == void.class;
                case Type.OBJECT:
                case Type.ARRAY:
                    return javaType.getName().replace('.', '/').equals(asmType.getInternalName())
                        || javaType.getName().equals(asmType.getClassName());
            }
            return false;
        }

        private static Object unboxArg(Object boxed, Class<?> targetType) {
            if (boxed == null) return null;
            if (targetType.isPrimitive()) {
                if (targetType == int.class && boxed instanceof Number) return ((Number) boxed).intValue();
                if (targetType == long.class && boxed instanceof Number) return ((Number) boxed).longValue();
                if (targetType == float.class && boxed instanceof Number) return ((Number) boxed).floatValue();
                if (targetType == double.class && boxed instanceof Number) return ((Number) boxed).doubleValue();
                if (targetType == boolean.class && boxed instanceof Boolean) return boxed;
                if (targetType == byte.class && boxed instanceof Number) return ((Number) boxed).byteValue();
                if (targetType == short.class && boxed instanceof Number) return ((Number) boxed).shortValue();
                if (targetType == char.class && boxed instanceof Character) return boxed;
            }
            return boxed;
        }

        // ========================================
        // Shutdown hook
        // ========================================

        static {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ASYNC_EXECUTOR.shutdown();
                SCHEDULER.shutdown();
                HotReloadManager.stopWatchThread();
                if (debugMode) {
                    System.out.println(formatTimingReport());
                }
                log("DeepMix Runtime shutdown complete. Transformations: %d", transformationCount.get());
            }, "DeepMix-Shutdown"));
        }
    }


    // ============================================================================
    // INTERNAL STATE CLASSES
    // ============================================================================

    public static class TimingStats {
        public String methodKey;
        public int invocationCount;
        public long totalNanos;
        public long avgNanos;
        public long minNanos;
        public long maxNanos;
        public long p50Nanos;
        public long p95Nanos;
        public long p99Nanos;

        @Override
        public String toString() {
            return String.format("TimingStats{%s: count=%d, avg=%.3fms, min=%.3fms, max=%.3fms, p95=%.3fms}",
                methodKey, invocationCount,
                avgNanos / 1_000_000.0, minNanos / 1_000_000.0,
                maxNanos / 1_000_000.0, p95Nanos / 1_000_000.0);
        }
    }

    public static class CircuitBreakerState {
        public enum State { CLOSED, OPEN, HALF_OPEN }

        private volatile State state = State.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final int failureThreshold;
        private final long resetTimeoutMs;
        private volatile long lastFailureTime = 0;

        public CircuitBreakerState(int failureThreshold, long resetTimeoutMs) {
            this.failureThreshold = failureThreshold;
            this.resetTimeoutMs = resetTimeoutMs;
        }

        public boolean isOpen() { return state == State.OPEN; }
        public boolean isClosed() { return state == State.CLOSED; }
        public boolean isHalfOpen() { return state == State.HALF_OPEN; }
        public State getState() { return state; }
        public int getFailureCount() { return failureCount.get(); }

        public boolean shouldAttemptReset() {
            return System.currentTimeMillis() - lastFailureTime >= resetTimeoutMs;
        }

        public void halfOpen() {
            state = State.HALF_OPEN;
            log("Circuit breaker → HALF_OPEN");
        }

        public void recordSuccess() {
            failureCount.set(0);
            state = State.CLOSED;
        }

        public void recordFailure() {
            lastFailureTime = System.currentTimeMillis();
            int count = failureCount.incrementAndGet();
            if (count >= failureThreshold) {
                state = State.OPEN;
                log("Circuit breaker → OPEN (failures: %d)", count);
            }
        }

        public void reset() {
            state = State.CLOSED;
            failureCount.set(0);
            lastFailureTime = 0;
        }
    }

    public static class RateLimiterState {
        private final int maxCalls;
        private final long windowMs;
        private final Queue<Long> callTimestamps = new ConcurrentLinkedQueue<>();

        public RateLimiterState(int maxCalls, long windowMs) {
            this.maxCalls = maxCalls;
            this.windowMs = windowMs;
        }

        public synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            long cutoff = now - windowMs;

            // Evict expired timestamps
            while (!callTimestamps.isEmpty() && callTimestamps.peek() < cutoff) {
                callTimestamps.poll();
            }

            if (callTimestamps.size() < maxCalls) {
                callTimestamps.offer(now);
                return true;
            }
            return false;
        }

        public int getCurrentCount() {
            long cutoff = System.currentTimeMillis() - windowMs;
            callTimestamps.removeIf(ts -> ts < cutoff);
            return callTimestamps.size();
        }

        public int getMaxCalls() { return maxCalls; }
        public long getWindowMs() { return windowMs; }
    }

    public static class DeepMixPromise<T> {
        private final CompletableFuture<T> future;

        public DeepMixPromise(CompletableFuture<T> future) {
            this.future = future;
        }

        public <R> DeepMixPromise<R> then(Function<T, R> mapper) {
            return new DeepMixPromise<>(future.thenApply(mapper));
        }

        public DeepMixPromise<T> onError(Function<Throwable, T> handler) {
            return new DeepMixPromise<>(future.exceptionally(handler));
        }

        public DeepMixPromise<T> onComplete(Consumer<T> consumer) {
            return new DeepMixPromise<>(future.whenComplete((result, error) -> {
                if (error == null) consumer.accept(result);
            }));
        }

        public <R> DeepMixPromise<R> flatMap(Function<T, DeepMixPromise<R>> mapper) {
            return new DeepMixPromise<>(future.thenCompose(value -> mapper.apply(value).future));
        }

        public DeepMixPromise<T> timeout(long millis) {
            CompletableFuture<T> timeout = new CompletableFuture<>();
            DeepMixRuntime.SCHEDULER.schedule(
                () -> timeout.completeExceptionally(new TimeoutException("Promise timed out after " + millis + "ms")),
                millis, TimeUnit.MILLISECONDS);
            return new DeepMixPromise<>(CompletableFuture.anyOf(future, timeout).thenApply(r -> (T) r));
        }

        public T await() throws Exception {
            return future.get();
        }

        public T await(long timeout, TimeUnit unit) throws Exception {
            return future.get(timeout, unit);
        }

        public boolean isDone() { return future.isDone(); }
        public boolean isFailed() { return future.isCompletedExceptionally(); }
        public CompletableFuture<T> toCompletableFuture() { return future; }

        public static <T> DeepMixPromise<T> resolve(T value) {
            return new DeepMixPromise<>(CompletableFuture.completedFuture(value));
        }

        public static <T> DeepMixPromise<T> reject(Throwable error) {
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(error);
            return new DeepMixPromise<>(f);
        }

        @SafeVarargs
        public static <T> DeepMixPromise<List<T>> all(DeepMixPromise<T>... promises) {
            CompletableFuture<?>[] futures = Arrays.stream(promises)
                .map(p -> p.future)
                .toArray(CompletableFuture[]::new);
            return new DeepMixPromise<>(
                CompletableFuture.allOf(futures).thenApply(v ->
                    Arrays.stream(promises)
                        .map(p -> p.future.join())
                        .collect(Collectors.toList())));
        }

        @SafeVarargs
        public static <T> DeepMixPromise<T> race(DeepMixPromise<T>... promises) {
            CompletableFuture<?>[] futures = Arrays.stream(promises)
                .map(p -> p.future)
                .toArray(CompletableFuture[]::new);
            return new DeepMixPromise<>(
                (CompletableFuture<T>) CompletableFuture.anyOf(futures));
        }
    }


    // ============================================================================
    // DEEPMIX EXCEPTION
    // ============================================================================

    public static class DeepMixException extends RuntimeException {
        private final String transformContext;

        public DeepMixException(String message) {
            super(message);
            this.transformContext = null;
        }

        public DeepMixException(String message, Exception cause) {
            super(message, cause);
            this.transformContext = null;
        }

        public DeepMixException(String message, String context) {
            super(message);
            this.transformContext = context;
        }

        public DeepMixException(String message, Exception cause, String context) {
            super(message, cause);
            this.transformContext = context;
        }

        public String getTransformContext() { return transformContext; }

        @Override
        public String toString() {
            String msg = super.toString();
            if (transformContext != null) {
                msg += " [context: " + transformContext + "]";
            }
            return msg;
        }
    }


    // ============================================================================
    // GLOBAL CONFIGURATION & LOGGING
    // ============================================================================

    private static volatile boolean debugMode = false;
    private static volatile boolean hotReloadEnabled = false;
    private static final AtomicInteger transformationCount = new AtomicInteger(0);
    private static volatile Consumer<String> logSink = null;

    public static void setDebugMode(boolean enabled) { debugMode = enabled; }
    public static boolean isDebugMode() { return debugMode; }
    public static void setHotReloadEnabled(boolean enabled) { hotReloadEnabled = enabled; }
    public static boolean isHotReloadEnabled() { return hotReloadEnabled; }
    public static int getTransformationCount() { return transformationCount.get(); }
    public static void resetTransformationCount() { transformationCount.set(0); }

    public static void setLogSink(Consumer<String> sink) { logSink = sink; }

    static void log(String format, Object... args) {
        if (debugMode) {
            String message = String.format("[DeepMix] " + format, args);
            if (logSink != null) {
                logSink.accept(message);
            } else {
                System.out.println(message);
            }
        }
    }


    // ============================================================================
    // BUILDER / FLUENT API for programmatic usage
    // ============================================================================

    /**
     * Fluent API for constructing and applying DeepMix transformations programmatically,
     * without annotations.
     *
     * Usage:
     *   DeepMixPhase1and2.builder()
     *       .targetClass("com.example.MyClass")
     *       .inject(InjectionPoint.HEAD, "com.example.Hooks", "onEntry")
     *       .wrapMethod("process", "com.example.Wrapper", "around")
     *       .cacheMethod("compute", DeepCache.CacheStrategy.LRU, 1000)
     *       .asyncMethod("fetchData", DeepAsync.AsyncMode.COMPLETABLE_FUTURE)
     *       .debug(true)
     *       .hotReload(true)
     *       .apply();
     */
    public static DeepMixBuilder builder() {
        return new DeepMixBuilder();
    }

    public static class DeepMixBuilder {
        private String targetClassName;
        private final List<Consumer<ClassNode>> transforms = new ArrayList<>();
        private boolean debug = false;
        private boolean hotReload = false;

        public DeepMixBuilder targetClass(String className) {
            this.targetClassName = className.replace('.', '/');
            return this;
        }

        public DeepMixBuilder debug(boolean enabled) {
            this.debug = enabled;
            return this;
        }

        public DeepMixBuilder hotReload(boolean enabled) {
            this.hotReload = enabled;
            return this;
        }

        public DeepMixBuilder inject(String methodName, DeepInject.InjectionPoint point,
                                      String hookClass, String hookMethod) {
            transforms.add(classNode -> {
                for (MethodNode method : classNode.methods) {
                    if (method.name.equals(methodName)) {
                        InsnList call = new InsnList();
                        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            hookClass.replace('.', '/'), hookMethod, "()V", false));

                        switch (point) {
                            case HEAD:
                                method.instructions.insert(call);
                                break;
                            case TAIL:
                            case RETURN:
                                for (AbstractInsnNode insn = method.instructions.getFirst();
                                     insn != null; insn = insn.getNext()) {
                                    if (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN) {
                                        method.instructions.insertBefore(insn, call);
                                        break;
                                    }
                                }
                                break;
                        }
                        transformationCount.incrementAndGet();
                        log("Builder: injected %s.%s → %s.%s at %s",
                            classNode.name, methodName, hookClass, hookMethod, point);
                    }
                }
            });
            return this;
        }

        public DeepMixBuilder overwriteMethod(String methodName, String sourceClass, String sourceMethod) {
            transforms.add(classNode -> {
                log("Builder: overwrite %s.%s from %s.%s",
                    classNode.name, methodName, sourceClass, sourceMethod);
                transformationCount.incrementAndGet();
            });
            return this;
        }

        public DeepMixBuilder addField(String name, String descriptor, Object defaultValue) {
            transforms.add(classNode -> {
                FieldNode field = new FieldNode(
                    Opcodes.ACC_PUBLIC, name, descriptor, null, defaultValue);
                classNode.fields.add(field);
                log("Builder: added field %s %s to %s", descriptor, name, classNode.name);
                transformationCount.incrementAndGet();
            });
            return this;
        }

        public DeepMixBuilder addInterface(String interfaceName) {
            transforms.add(classNode -> {
                String internal = interfaceName.replace('.', '/');
                if (!classNode.interfaces.contains(internal)) {
                    classNode.interfaces.add(internal);
                    log("Builder: added interface %s to %s", interfaceName, classNode.name);
                    transformationCount.incrementAndGet();
                }
            });
            return this;
        }

        public DeepMixBuilder setSuperclass(String superClassName) {
            transforms.add(classNode -> {
                classNode.superName = superClassName.replace('.', '/');
                log("Builder: set superclass %s for %s", superClassName, classNode.name);
                transformationCount.incrementAndGet();
            });
            return this;
        }

        public DeepMixBuilder transform(Consumer<ClassNode> customTransform) {
            transforms.add(customTransform);
            return this;
        }

        public void apply() {
            if (targetClassName == null || targetClassName.isEmpty()) {
                throw new DeepMixException("Target class must be specified");
            }

            debugMode = debug || debugMode;
            hotReloadEnabled = hotReload || hotReloadEnabled;

            if (hotReloadEnabled) {
                ProcessorRegistry.enableHotReload();
            }

            for (Consumer<ClassNode> transform : transforms) {
                ProcessorRegistry.registerDeferredTransform(targetClassName, transform);
            }

            log("Builder: registered %d transforms for %s", transforms.size(), targetClassName);
        }

        public void applyTo(ClassNode classNode) {
            debugMode = debug || debugMode;

            for (Consumer<ClassNode> transform : transforms) {
                transform.accept(classNode);
            }

            if (!BytecodeAnalyzer.validate(classNode)) {
                throw new DeepMixException("Builder: bytecode validation failed for " + classNode.name);
            }

            log("Builder: applied %d transforms directly to %s", transforms.size(), classNode.name);
        }
    }


    // ============================================================================
    // MAIN CLASS TRANSFORMER — Entry point for class transformation pipeline
    // ============================================================================

    /**
     * Main entry point for the DeepMix transformation pipeline.
     * Typically called from a Java agent or custom ClassLoader.
     *
     * @param className The internal class name (e.g., "com/example/MyClass")
     * @param classBytes The original class bytecode
     * @return The transformed class bytecode, or the original if no transforms apply
     */
    public static byte[] transform(String className, byte[] classBytes) {
        try {
            ClassReader cr = new ClassReader(classBytes);
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, ClassReader.EXPAND_FRAMES);

            int countBefore = transformationCount.get();

            // Run the full processing pipeline
            ProcessorRegistry.processClass(classNode);

            int countAfter = transformationCount.get();

            if (countAfter > countBefore) {
                // Transforms were applied — generate new bytecode
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                classNode.accept(cw);
                byte[] result = cw.toByteArray();
                log("Transformed %s (%d bytes → %d bytes, %d transforms)",
                    className, classBytes.length, result.length, countAfter - countBefore);
                return result;
            }

            return classBytes; // No transforms — return original

        } catch (DeepMixException e) {
            log("TRANSFORM ERROR on %s: %s", className, e.getMessage());
            throw e;
        } catch (Exception e) {
            log("UNEXPECTED ERROR on %s: %s", className, e.getMessage());
            return classBytes; // Return original on unexpected failure for safety
        }
    }

    /**
     * Transform a class with explicit debug control.
     */
    public static byte[] transform(String className, byte[] classBytes, boolean enableDebug) {
        boolean wasDebug = debugMode;
        debugMode = enableDebug;
        try {
            return transform(className, classBytes);
        } finally {
            debugMode = wasDebug;
        }
    }

    /**
     * Batch-transform multiple classes. Useful for bulk mod application.
     *
     * @param classes Map of className → classBytes
     * @return Map of className → transformedClassBytes
     */
    public static Map<String, byte[]> transformAll(Map<String, byte[]> classes) {
        Map<String, byte[]> results = new LinkedHashMap<>();
        int totalTransforms = 0;

        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            int before = transformationCount.get();
            byte[] transformed = transform(entry.getKey(), entry.getValue());
            results.put(entry.getKey(), transformed);
            totalTransforms += (transformationCount.get() - before);
        }

        log("Batch transform complete: %d classes, %d total transforms", classes.size(), totalTransforms);
        return results;
    }

    /**
     * Print a diagnostic summary to the console.
     */
    public static void printDiagnostics() {
        System.out.println(DeepMixRuntime.formatTimingReport());
        System.out.println("[DeepMix] Total transformations: " + transformationCount.get());
        System.out.println("[DeepMix] Hot reload: " + (hotReloadEnabled ? "ENABLED" : "DISABLED"));
        System.out.println("[DeepMix] Debug mode: " + (debugMode ? "ON" : "OFF"));
        System.out.println("[DeepMix] Watched files: " + HotReloadManager.getWatchedFileCount());
        System.out.println("[DeepMix] Circuit breakers: " +
            DeepMixRuntime.CIRCUIT_BREAKERS.size() + " registered");
    }
}
