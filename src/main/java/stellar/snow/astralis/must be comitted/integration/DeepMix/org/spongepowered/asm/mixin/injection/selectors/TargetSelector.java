package org.spongepowered.asm.mixin.injection.selectors;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.Diagnostic;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelector;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;
import org.spongepowered.asm.mixin.injection.selectors.MemberMatcher;
import org.spongepowered.asm.mixin.injection.selectors.dynamic.DynamicSelectorDesc;
import org.spongepowered.asm.mixin.injection.selectors.throwables.SelectorConstraintException;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.throwables.MixinError;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.asm.util.logging.MessageRouter;
import org.spongepowered.include.com.google.common.base.Strings;

public final class TargetSelector {
    private static final Pattern PATTERN_DYNAMIC = Pattern.compile("(?i)^\\x40([a-z]+(:[a-z]+)?)(\\((.*)\\))?$");
    private static Map<String, DynamicSelectorEntry> dynamicSelectors = new LinkedHashMap<String, DynamicSelectorEntry>();

    private TargetSelector() {
    }

    public static void register(Class<? extends ITargetSelectorDynamic> type, String namespace) {
        DynamicSelectorEntry entry;
        ITargetSelectorDynamic.SelectorId selectorId = type.getAnnotation(ITargetSelectorDynamic.SelectorId.class);
        if (selectorId == null) {
            throw new IllegalArgumentException("Dynamic target selector class " + type + " is not annotated with @SelectorId");
        }
        String annotationNamespace = selectorId.namespace();
        if (!Strings.isNullOrEmpty(annotationNamespace)) {
            namespace = annotationNamespace;
        }
        if (Strings.isNullOrEmpty(namespace)) {
            throw new IllegalArgumentException("Dynamic target selector class " + type + " has no namespace. Please specify namespace in SelectorId annotation or declaring configuration");
        }
        try {
            entry = new DynamicSelectorEntry(namespace.toLowerCase(Locale.ROOT), selectorId.value().toLowerCase(Locale.ROOT), type);
        }
        catch (NoSuchMethodException ex) {
            throw new MixinError("Dynamic target selector class " + type.getName() + " does not contain a valid parse method");
        }
        String code = entry.getCode();
        if (!Pattern.matches("[a-z]+(:[a-z]+)?", code)) {
            throw new IllegalArgumentException("Dynamic target selector class " + type + " has an invalid id. Only alpha characters can be used in selector ids and namespaces");
        }
        DynamicSelectorEntry existing = dynamicSelectors.get(code);
        if (existing != null) {
            MessageRouter.getMessager().printMessage(Diagnostic.Kind.WARNING, String.format("Overriding target selector for @%s with %s (previously %s)", code, type.getName(), existing.type.getName()));
        } else {
            MessageRouter.getMessager().printMessage(Diagnostic.Kind.OTHER, String.format("Registering new target selector for @%s with %s", code, type.getName()));
        }
        dynamicSelectors.put(code, entry);
    }

    private static void registerBuiltIn(Class<? extends ITargetSelectorDynamic> type) {
        DynamicSelectorEntry entry;
        ITargetSelectorDynamic.SelectorId selectorId = type.getAnnotation(ITargetSelectorDynamic.SelectorId.class);
        try {
            entry = new DynamicSelectorEntry(null, selectorId.value().toLowerCase(Locale.ROOT), type);
        }
        catch (NoSuchMethodException ex) {
            throw new MixinError("Dynamic target selector class " + type.getName() + " does not contain a valid parse method");
        }
        dynamicSelectors.put(entry.id, entry);
        dynamicSelectors.put("mixin:" + entry.id, entry);
    }

    public static ITargetSelector parseAndValidate(IAnnotationHandle annotation, ISelectorContext context) throws InvalidSelectorException {
        return TargetSelector.parse(annotation, context).validate();
    }

    public static ITargetSelector parseAndValidate(String string, ISelectorContext context) throws InvalidSelectorException {
        return TargetSelector.parse(string, context).validate();
    }

    public static Set<ITargetSelector> parseAndValidate(Iterable<?> selectors, ISelectorContext context) throws InvalidSelectorException {
        Set<ITargetSelector> parsed = TargetSelector.parse(selectors, context, new LinkedHashSet<ITargetSelector>());
        for (ITargetSelector selector : parsed) {
            selector.validate();
        }
        return parsed;
    }

    public static Set<ITargetSelector> parse(Iterable<?> selectors, ISelectorContext context) {
        return TargetSelector.parse(selectors, context, new LinkedHashSet<ITargetSelector>());
    }

    public static Set<ITargetSelector> parse(Iterable<?> selectors, ISelectorContext context, Set<ITargetSelector> parsed) {
        if (parsed == null) {
            parsed = new LinkedHashSet<ITargetSelector>();
        }
        if (selectors != null) {
            for (Object selector : selectors) {
                if (selector instanceof IAnnotationHandle) {
                    parsed.add(TargetSelector.parse((IAnnotationHandle)selector, context));
                    continue;
                }
                if (selector instanceof AnnotationNode) {
                    parsed.add(TargetSelector.parse(Annotations.handleOf(selector), context));
                    continue;
                }
                if (selector instanceof String) {
                    parsed.add(TargetSelector.parse((String)selector, context));
                    continue;
                }
                if (selector instanceof Class) {
                    String desc = Type.getType((Class)((Class)selector)).getDescriptor();
                    parsed.add(TargetSelector.parse(desc, context));
                    continue;
                }
                if (selector == null) continue;
                parsed.add(TargetSelector.parse(selector.toString(), context));
            }
        }
        return parsed;
    }

    public static ITargetSelector parse(IAnnotationHandle annotation, ISelectorContext context) {
        for (DynamicSelectorEntry entry : dynamicSelectors.values()) {
            if (entry.annotation == null || !Annotations.getDesc(entry.annotation).equals(annotation.getDesc())) continue;
            try {
                return entry.parse(annotation, context);
            }
            catch (ReflectiveOperationException ex) {
                return new InvalidSelector(ex.getCause());
            }
            catch (Exception ex) {
                return new InvalidSelector(ex);
            }
        }
        return new InvalidSelector(new InvalidSelectorException("Dynamic selector for annotation " + annotation + " is not registered."));
    }

    public static ITargetSelector parse(String string, ISelectorContext context) {
        MemberMatcher regexMatcher;
        if ((string = string.trim()).endsWith("/") && (regexMatcher = MemberMatcher.parse(string, context)) != null) {
            return regexMatcher;
        }
        if (!string.startsWith("@")) {
            return MemberInfo.parse(string, context);
        }
        Matcher dynamic = PATTERN_DYNAMIC.matcher(string);
        if (!dynamic.matches()) {
            return new InvalidSelector(new InvalidSelectorException("Dynamic selector was in an unrecognised format. Parsing selector: " + string));
        }
        String selectorId = dynamic.group(1).toLowerCase(Locale.ROOT);
        if (!dynamicSelectors.containsKey(selectorId)) {
            return new InvalidSelector(new InvalidSelectorException("Dynamic selector with id '@" + dynamic.group(1) + "' is not registered. Parsing selector: " + string));
        }
        try {
            return dynamicSelectors.get(selectorId).parse(Strings.nullToEmpty(dynamic.group(4)).trim(), context);
        }
        catch (ReflectiveOperationException ex) {
            return new InvalidSelector(ex.getCause(), string);
        }
        catch (Exception ex) {
            return new InvalidSelector(ex);
        }
    }

    public static String parseName(String name, ISelectorContext context) {
        ITargetSelector selector = TargetSelector.parse(name, context);
        if (!(selector instanceof ITargetSelectorByName)) {
            return name;
        }
        String mappedName = ((ITargetSelectorByName)selector).getName();
        return mappedName != null ? mappedName : name;
    }

    public static <TNode> Result<TNode> run(ITargetSelector selector, Iterable<ElementNode<TNode>> nodes) {
        ArrayList<ElementNode<TNode>> candidates = new ArrayList<ElementNode<TNode>>();
        ElementNode<TNode> exactMatch = TargetSelector.runSelector(selector, nodes, candidates);
        return new Result<TNode>(exactMatch, candidates);
    }

    public static <TNode> Result<TNode> run(Iterable<ITargetSelector> selector, Iterable<ElementNode<TNode>> nodes) {
        ElementNode<TNode> exactMatch = null;
        ArrayList candidates = new ArrayList();
        for (ITargetSelector target : selector) {
            ElementNode<TNode> selectorExactMatch = TargetSelector.runSelector(target, nodes, candidates);
            if (exactMatch != null) continue;
            exactMatch = selectorExactMatch;
        }
        return new Result(exactMatch, candidates);
    }

    private static <TNode> ElementNode<TNode> runSelector(ITargetSelector selector, Iterable<ElementNode<TNode>> nodes, List<ElementNode<TNode>> candidates) {
        int matchCount = 0;
        ElementNode<TNode> exactMatch = null;
        for (ElementNode<TNode> element : nodes) {
            MatchResult match = selector.match(element);
            if (!match.isMatch()) continue;
            if (++matchCount > selector.getMaxMatchCount()) break;
            if (!candidates.contains(element)) {
                candidates.add(element);
            }
            if (exactMatch != null || !match.isExactMatch()) continue;
            exactMatch = element;
        }
        if (matchCount < selector.getMinMatchCount()) {
            throw new SelectorConstraintException(selector, String.format("%s did not match the required number of targets (required=%d, matched=%d)", selector, selector.getMinMatchCount(), matchCount));
        }
        return exactMatch;
    }

    static {
        TargetSelector.registerBuiltIn(DynamicSelectorDesc.class);
    }

    static class DynamicSelectorEntry {
        final String namespace;
        final String id;
        final Class<? extends ITargetSelectorDynamic> type;
        final Class<? extends Annotation> annotation;
        final Method mdParseString;
        final Method mdParseAnnotation;

        DynamicSelectorEntry(String namespace, String id, Class<? extends ITargetSelectorDynamic> type) throws NoSuchMethodException {
            this.namespace = namespace;
            this.id = id;
            this.type = type;
            this.mdParseString = type.getDeclaredMethod("parse", String.class, ISelectorContext.class);
            if (!Modifier.isStatic(this.mdParseString.getModifiers())) {
                throw new MixinError("parse method for dynamic target selector [" + this.type.getName() + "] must be static");
            }
            if (!ITargetSelectorDynamic.class.isAssignableFrom(this.mdParseString.getReturnType())) {
                throw new MixinError("parse(String) method for dynamic target selector [" + this.type.getName() + "] must return an ITargetSelectorDynamic subtype");
            }
            Class<? extends Annotation> annotation = null;
            Method mdParseAnnotation = null;
            ITargetSelectorDynamic.SelectorAnnotation selectorAnnotation = type.getAnnotation(ITargetSelectorDynamic.SelectorAnnotation.class);
            if (selectorAnnotation != null) {
                annotation = selectorAnnotation.value();
                mdParseAnnotation = type.getDeclaredMethod("parse", IAnnotationHandle.class, ISelectorContext.class);
                if (!Modifier.isStatic(mdParseAnnotation.getModifiers())) {
                    throw new MixinError("parse method for dynamic target selector [" + this.type.getName() + "] must be static");
                }
                if (!ITargetSelectorDynamic.class.isAssignableFrom(mdParseAnnotation.getReturnType())) {
                    throw new MixinError("parse(Annotation) method for dynamic target selector [" + this.type.getName() + "] must return an ITargetSelectorDynamic subtype");
                }
            }
            this.annotation = annotation;
            this.mdParseAnnotation = mdParseAnnotation;
        }

        String getCode() {
            return (this.namespace != null ? this.namespace + ":" : "") + this.id;
        }

        ITargetSelectorDynamic parse(String input, ISelectorContext context) throws ReflectiveOperationException {
            return this.parse(input, context, this.mdParseString);
        }

        ITargetSelectorDynamic parse(IAnnotationHandle input, ISelectorContext context) throws ReflectiveOperationException {
            return this.parse(input, context, this.mdParseAnnotation);
        }

        ITargetSelectorDynamic parse(Object input, ISelectorContext context, Method parseMethod) throws ReflectiveOperationException {
            try {
                return (ITargetSelectorDynamic)parseMethod.invoke(null, input, context);
            }
            catch (InvocationTargetException itex) {
                Throwable cause = itex.getCause();
                if (cause instanceof MixinException) {
                    throw (MixinException)cause;
                }
                Throwable ex = cause != null ? cause : itex;
                throw new MixinError("Error parsing dynamic target selector [" + this.type.getName() + "] for " + context, ex);
            }
        }
    }

    public static class Result<TNode> {
        public final ElementNode<TNode> exactMatch;
        public final List<ElementNode<TNode>> candidates;

        Result(ElementNode<TNode> exactMatch, List<ElementNode<TNode>> candidates) {
            this.exactMatch = exactMatch;
            this.candidates = candidates;
        }

        public TNode getSingleResult(boolean strict) {
            int resultCount = this.candidates.size();
            if (this.exactMatch != null) {
                return this.exactMatch.get();
            }
            if (resultCount == 1 || !strict) {
                return this.candidates.get(0).get();
            }
            throw new IllegalStateException((resultCount == 0 ? "No" : "Multiple") + " candidates were found");
        }
    }
}

