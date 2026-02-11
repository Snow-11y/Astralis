package org.spongepowered.asm.mixin.injection.selectors.dynamic;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Descriptors;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.dynamic.IResolvedDescriptor;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.Quantifier;
import org.spongepowered.asm.util.asm.IAnnotatedElement;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.base.Strings;

public final class DescriptorResolver {
    public static String PRINT_ID = "?";

    private DescriptorResolver() {
    }

    public static IResolvedDescriptor resolve(IAnnotationHandle desc, ISelectorContext context) {
        return new Descriptor(Collections.emptySet(), desc, context);
    }

    public static IResolvedDescriptor resolve(String id, ISelectorContext context) {
        boolean debug = false;
        ResolverObserverBasic observer = new ResolverObserverBasic();
        if (!Strings.isNullOrEmpty(id)) {
            if (PRINT_ID.equals(id)) {
                observer = new ResolverObserverDebug(context);
                id = "";
                debug = true;
            } else {
                observer.visit(id, "", "");
            }
        }
        IAnnotationHandle desc = DescriptorResolver.resolve(id, context, observer, context.getSelectorCoordinate(true));
        observer.postResolve();
        return new Descriptor(observer.getSearched(), desc, context, debug);
    }

    private static IAnnotationHandle resolve(String id, ISelectorContext context, IResolverObserver observer, String coordinate) {
        IAnnotationHandle annotation = Annotations.handleOf(context.getSelectorAnnotation());
        observer.visit(coordinate, annotation, annotation.toString() + ".desc");
        IAnnotationHandle resolved = DescriptorResolver.resolve(id, context, observer, coordinate, annotation.getAnnotationList("desc"));
        if (resolved != null) {
            return resolved;
        }
        resolved = DescriptorResolver.resolve(id, context, observer, coordinate, context.getMethod(), "method");
        if (resolved != null) {
            return resolved;
        }
        ISelectorContext root = DescriptorResolver.getRoot(context);
        String rootCoordinate = root.getSelectorCoordinate(false);
        String mixinCoordinate = (root != context || !coordinate.contains(".")) && !rootCoordinate.equals(coordinate) ? rootCoordinate + "." + coordinate : coordinate;
        resolved = DescriptorResolver.resolve(id, context, observer, mixinCoordinate, context.getMixin(), "mixin");
        if (resolved != null) {
            return resolved;
        }
        ISelectorContext parent = context.getParent();
        if (parent != null) {
            String parentCoordinate = parent.getSelectorCoordinate(false) + "." + coordinate;
            return DescriptorResolver.resolve(id, parent, observer, parentCoordinate);
        }
        return null;
    }

    private static IAnnotationHandle resolve(String id, ISelectorContext context, IResolverObserver observer, String coordinate, Object element, String detail) {
        IAnnotationHandle resolved;
        IAnnotationHandle resolved2;
        observer.visit(coordinate, element, detail);
        IAnnotationHandle descriptors = DescriptorResolver.getVisibleAnnotation(element, Descriptors.class);
        if (descriptors != null && (resolved2 = DescriptorResolver.resolve(id, context, observer, coordinate, descriptors.getAnnotationList("value"))) != null) {
            return resolved2;
        }
        IAnnotationHandle descriptor = DescriptorResolver.getVisibleAnnotation(element, Desc.class);
        if (descriptor != null && (resolved = DescriptorResolver.resolve(id, context, observer, coordinate, descriptor)) != null) {
            return resolved;
        }
        return null;
    }

    private static IAnnotationHandle resolve(String id, ISelectorContext context, IResolverObserver observer, String coordinate, List<IAnnotationHandle> availableDescriptors) {
        if (availableDescriptors != null) {
            for (IAnnotationHandle desc : availableDescriptors) {
                IAnnotationHandle resolved = DescriptorResolver.resolve(id, context, observer, coordinate, desc);
                if (resolved == null) continue;
                return resolved;
            }
        }
        return null;
    }

    private static IAnnotationHandle resolve(String id, ISelectorContext context, IResolverObserver observer, String coordinate, IAnnotationHandle desc) {
        if (desc != null) {
            String descriptorId = desc.getValue("id", coordinate);
            boolean implicit = Strings.isNullOrEmpty(id);
            if (implicit && descriptorId.equalsIgnoreCase(coordinate) || !implicit && descriptorId.equalsIgnoreCase(id)) {
                return desc;
            }
        }
        return null;
    }

    private static IAnnotationHandle getVisibleAnnotation(Object element, Class<? extends Annotation> annotationClass) {
        if (element instanceof MethodNode) {
            return Annotations.handleOf(Annotations.getVisible((MethodNode)element, annotationClass));
        }
        if (element instanceof ClassNode) {
            return Annotations.handleOf(Annotations.getVisible((ClassNode)element, annotationClass));
        }
        if (element instanceof MixinTargetContext) {
            return Annotations.handleOf(Annotations.getVisible(((MixinTargetContext)element).getClassNode(), annotationClass));
        }
        if (element instanceof IAnnotatedElement) {
            IAnnotationHandle annotation = ((IAnnotatedElement)element).getAnnotation(annotationClass);
            return annotation != null && annotation.exists() ? annotation : null;
        }
        if (element == null) {
            return null;
        }
        throw new IllegalStateException("Cannot read visible annotations from element with unknown type: " + element.getClass().getName());
    }

    private static ISelectorContext getRoot(ISelectorContext context) {
        ISelectorContext parent = context.getParent();
        while (parent != null) {
            context = parent;
            parent = context.getParent();
        }
        return context;
    }

    static class ResolverObserverDebug
    extends ResolverObserverBasic {
        private final PrettyPrinter printer = new PrettyPrinter();

        ResolverObserverDebug(ISelectorContext context) {
            this.printer.add("Searching for implicit descriptor").add(context).hr().table();
            this.printer.tr("Context Coordinate:", context.getSelectorCoordinate(true) + " (" + context.getSelectorCoordinate(false) + ")");
            this.printer.tr("Selector Annotation:", context.getSelectorAnnotation());
            this.printer.tr("Root Annotation:", context.getAnnotation());
            this.printer.tr("Method:", context.getMethod()).hr();
            this.printer.table("Search Coordinate", "Search Element", "Detail").th().hr();
        }

        @Override
        public void visit(String coordinate, Object element, String detail) {
            super.visit(coordinate, element, detail);
            this.printer.tr(coordinate, element, detail);
        }

        @Override
        public void postResolve() {
            this.printer.print();
        }
    }

    static class ResolverObserverBasic
    implements IResolverObserver {
        private final Set<String> searched = new LinkedHashSet<String>();

        ResolverObserverBasic() {
        }

        @Override
        public void visit(String coordinate, Object element, String detail) {
            this.searched.add(coordinate);
        }

        @Override
        public Set<String> getSearched() {
            return this.searched;
        }

        @Override
        public void postResolve() {
        }
    }

    static interface IResolverObserver {
        public void visit(String var1, Object var2, String var3);

        public Set<String> getSearched();

        public void postResolve();
    }

    static final class Descriptor
    implements IResolvedDescriptor {
        private final Set<String> searched;
        private final IAnnotationHandle desc;
        private final ISelectorContext context;
        private final boolean debug;

        Descriptor(Set<String> searched, IAnnotationHandle desc, ISelectorContext context) {
            this(searched, desc, context, false);
        }

        Descriptor(Set<String> searched, IAnnotationHandle desc, ISelectorContext context, boolean debug) {
            this.searched = searched;
            this.desc = desc;
            this.context = context;
            this.debug = debug;
        }

        @Override
        public boolean isResolved() {
            return this.desc != null;
        }

        @Override
        public boolean isDebug() {
            return this.debug;
        }

        @Override
        public String getResolutionInfo() {
            if (this.searched == null) {
                return "";
            }
            return String.format("Searched coordinates [ \"%s\" ]", Joiner.on("\", \"").join(this.searched));
        }

        @Override
        public IAnnotationHandle getAnnotation() {
            return this.desc;
        }

        @Override
        public String getId() {
            return this.desc != null ? this.desc.getValue("id", "") : "";
        }

        @Override
        public Type getOwner() {
            if (this.desc == null) {
                return Type.VOID_TYPE;
            }
            Type ownerClass = this.desc.getTypeValue("owner");
            if (ownerClass != Type.VOID_TYPE) {
                return ownerClass;
            }
            return this.context != null ? Type.getObjectType((String)this.context.getMixin().getTargetClassRef()) : ownerClass;
        }

        @Override
        public String getName() {
            if (this.desc == null) {
                return "";
            }
            String value = this.desc.getValue("value", "");
            if (!value.isEmpty()) {
                return value;
            }
            return this.desc.getValue("name", "");
        }

        @Override
        public Type[] getArgs() {
            if (this.desc == null) {
                return new Type[0];
            }
            List<Type> args = this.desc.getTypeList("args");
            return args.toArray(new Type[args.size()]);
        }

        @Override
        public Type getReturnType() {
            if (this.desc == null) {
                return Type.VOID_TYPE;
            }
            return this.desc.getTypeValue("ret");
        }

        @Override
        public Quantifier getMatches() {
            Integer max;
            if (this.desc == null) {
                return Quantifier.DEFAULT;
            }
            int min = Math.max(0, this.desc != null ? this.desc.getValue("min", 0) : 0);
            Integer n = max = this.desc != null ? (Integer)this.desc.getValue("max", null) : null;
            return new Quantifier(min, max != null ? (max > 0 ? max : Integer.MAX_VALUE) : -1);
        }

        @Override
        public List<IAnnotationHandle> getNext() {
            return this.desc != null ? this.desc.getAnnotationList("next") : Collections.emptyList();
        }
    }
}

