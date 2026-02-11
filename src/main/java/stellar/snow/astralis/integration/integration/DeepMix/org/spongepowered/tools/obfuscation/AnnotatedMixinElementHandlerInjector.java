package org.spongepowered.tools.obfuscation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorConstructor;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.tools.obfuscation.AnnotatedMixin;
import org.spongepowered.tools.obfuscation.AnnotatedMixinElementHandler;
import org.spongepowered.tools.obfuscation.ObfuscationData;
import org.spongepowered.tools.obfuscation.ObfuscationType;
import org.spongepowered.tools.obfuscation.ReferenceManager;
import org.spongepowered.tools.obfuscation.SuppressedBy;
import org.spongepowered.tools.obfuscation.ext.SpecialPackages;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IReferenceManager;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.struct.InjectorRemap;

class AnnotatedMixinElementHandlerInjector
extends AnnotatedMixinElementHandler {
    AnnotatedMixinElementHandlerInjector(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
        super(ap, mixin);
    }

    public void registerInjector(AnnotatedElementInjector elem) {
        for (String reference : ((AnnotationHandle)elem.getAnnotation()).getList("method")) {
            this.registerInjectorTarget(elem, reference, TargetSelector.parse(reference, (ISelectorContext)elem), elem + ".method=\"" + reference + "\"");
        }
        for (IAnnotationHandle desc : ((AnnotationHandle)elem.getAnnotation()).getAnnotationList("target")) {
            String subject = String.format("%s.target=@Desc(id = \"%s\")", elem, desc.getValue("id", ""));
            this.registerInjectorTarget(elem, null, TargetSelector.parse(desc, (ISelectorContext)elem), subject);
        }
    }

    private void registerInjectorTarget(AnnotatedElementInjector elem, String reference, ITargetSelector targetSelector, String subject) {
        block6: {
            try {
                targetSelector.validate();
            }
            catch (InvalidSelectorException ex) {
                elem.printMessage(this.ap, IMessagerEx.MessageType.TARGET_SELECTOR_VALIDATION, ex.getMessage());
            }
            if (!(targetSelector instanceof ITargetSelectorByName)) {
                return;
            }
            ITargetSelectorByName targetMember = (ITargetSelectorByName)targetSelector;
            if (targetMember.getName() == null) {
                return;
            }
            if (targetMember.getDesc() != null) {
                this.validateReferencedTarget(elem, reference, targetMember, subject);
            }
            if (!(targetSelector instanceof ITargetSelectorRemappable) || !elem.shouldRemap()) break block6;
            for (TypeHandle target : this.mixin.getTargets()) {
                if (!this.registerInjector(elem, reference, (ITargetSelectorRemappable)targetMember, target)) break;
            }
        }
    }

    private boolean registerInjector(AnnotatedElementInjector elem, String reference, ITargetSelectorRemappable targetMember, TypeHandle target) {
        String desc = target.findDescriptor(targetMember);
        if (desc == null) {
            IMessagerEx.MessageType messageType;
            IMessagerEx.MessageType messageType2 = messageType = this.mixin.isMultiTarget() ? IMessagerEx.MessageType.MISSING_INJECTOR_DESC_MULTITARGET : IMessagerEx.MessageType.MISSING_INJECTOR_DESC_SINGLETARGET;
            if (target.isSimulated()) {
                elem.printMessage(this.ap, IMessagerEx.MessageType.MISSING_INJECTOR_DESC_SIMULATED, elem + " target '" + reference + "' in @Pseudo mixin will not be obfuscated");
            } else if (target.isImaginary()) {
                elem.printMessage(this.ap, messageType, elem + " target requires method signature because enclosing type information for " + target + " is unavailable");
            } else if (!targetMember.isInitialiser()) {
                elem.printMessage(this.ap, messageType, "Unable to determine descriptor for " + elem + " target method");
            }
            return true;
        }
        String targetName = elem + " target " + targetMember.getName();
        MappingMethod targetMethod = target.getMappingMethod(targetMember.getName(), desc);
        ObfuscationData<MappingMethod> obfData = this.obf.getDataProvider().getObfMethod(targetMethod);
        if (obfData.isEmpty()) {
            if (target.isSimulated()) {
                obfData = this.obf.getDataProvider().getRemappedMethod(targetMethod);
            } else {
                if (targetMember.isClassInitialiser()) {
                    return true;
                }
                elem.addMessage(targetMember.isConstructor() ? IMessagerEx.MessageType.NO_OBFDATA_FOR_CTOR : IMessagerEx.MessageType.NO_OBFDATA_FOR_TARGET, "Unable to locate obfuscation mapping for " + targetName, (Element)elem.getElement(), (AnnotationHandle)elem.getAnnotation());
                return false;
            }
        }
        IReferenceManager refMaps = this.obf.getReferenceManager();
        try {
            if (targetMember.getOwner() == null && this.mixin.isMultiTarget() || target.isSimulated()) {
                obfData = AnnotatedMixinElementHandler.stripOwnerData(obfData);
            }
            refMaps.addMethodMapping(this.classRef, reference, obfData);
        }
        catch (ReferenceManager.ReferenceConflictException ex) {
            String conflictType;
            String string = conflictType = this.mixin.isMultiTarget() ? "Multi-target" : "Target";
            if (elem.hasCoerceArgument() && targetMember.getOwner() == null && targetMember.getDesc() == null) {
                String newName;
                ITargetSelector oldMember = TargetSelector.parse(ex.getOld(), (ISelectorContext)elem);
                ITargetSelector newMember = TargetSelector.parse(ex.getNew(), (ISelectorContext)elem);
                String oldName = oldMember instanceof ITargetSelectorByName ? ((ITargetSelectorByName)oldMember).getName() : oldMember.toString();
                String string2 = newName = newMember instanceof ITargetSelectorByName ? ((ITargetSelectorByName)newMember).getName() : newMember.toString();
                if (oldName != null && oldName.equals(newName)) {
                    obfData = AnnotatedMixinElementHandler.stripDescriptors(obfData);
                    refMaps.setAllowConflicts(true);
                    refMaps.addMethodMapping(this.classRef, reference, obfData);
                    refMaps.setAllowConflicts(false);
                    elem.printMessage(this.ap, IMessagerEx.MessageType.BARE_REFERENCE, "Coerced " + conflictType + " reference has conflicting descriptors for " + targetName + ": Storing bare references " + obfData.values() + " in refMap");
                    return true;
                }
            }
            elem.printMessage(this.ap, IMessagerEx.MessageType.INJECTOR_MAPPING_CONFLICT, conflictType + " reference conflict for " + targetName + ": " + reference + " -> " + ex.getNew() + " previously defined as " + ex.getOld());
        }
        return true;
    }

    public void registerInjectionPoint(AnnotatedElementInjectionPoint elem, String format) {
        ITargetSelector targetSelector = null;
        String targetReference = (String)elem.getAt().getValue("target");
        if (targetReference != null) {
            targetSelector = TargetSelector.parse(targetReference, (ISelectorContext)elem);
            try {
                targetSelector.validate();
            }
            catch (InvalidSelectorException ex) {
                this.ap.printMessage(IMessagerEx.MessageType.TARGET_SELECTOR_VALIDATION, (CharSequence)ex.getMessage(), (Element)elem.getElement(), elem.getAtErrorElement(this.ap.getCompilerEnvironment()));
            }
        }
        String type = InjectionPointData.parseType(elem.getAt().getValue("value", ""));
        ITargetSelector classSelector = null;
        String classReference = elem.getAtArg("class");
        if ("NEW".equals(type) && classReference != null) {
            classSelector = TargetSelector.parse(classReference, (ISelectorContext)elem);
            try {
                classSelector.validate();
            }
            catch (InvalidSelectorException ex) {
                this.ap.printMessage(IMessagerEx.MessageType.TARGET_SELECTOR_VALIDATION, (CharSequence)ex.getMessage(), (Element)elem.getElement(), elem.getAtErrorElement(this.ap.getCompilerEnvironment()));
            }
        }
        if (elem.shouldRemap()) {
            if ("NEW".equals(type)) {
                this.remapNewTarget(String.format(format, type + ".<target>"), targetReference, targetSelector, elem);
                this.remapNewTarget(String.format(format, type + ".args[class]"), classReference, classSelector, elem);
            } else {
                this.remapReference(String.format(format, type + ".<target>"), targetReference, targetSelector, elem);
            }
        }
    }

    protected final void remapNewTarget(String subject, String reference, ITargetSelector selector, AnnotatedElementInjectionPoint elem) {
        if (!(selector instanceof ITargetSelectorConstructor)) {
            return;
        }
        ITargetSelectorConstructor member = (ITargetSelectorConstructor)selector;
        String target = member.toCtorType();
        if (target != null) {
            String desc = member.toCtorDesc();
            MappingMethod m = new MappingMethod(target, ".", desc != null ? desc : "()V");
            ObfuscationData<MappingMethod> remapped = this.obf.getDataProvider().getRemappedMethod(m);
            if (remapped.isEmpty() && !SpecialPackages.isExcludedPackage(member.toCtorType())) {
                this.ap.printMessage(IMessagerEx.MessageType.NO_OBFDATA_FOR_CLASS, (CharSequence)("Unable to locate class mapping for " + subject + " '" + target + "'"), (Element)elem.getElement(), ((AnnotationHandle)elem.getAnnotation()).asMirror(), SuppressedBy.MAPPING);
                return;
            }
            ObfuscationData<String> mappings = new ObfuscationData<String>();
            for (ObfuscationType type : remapped) {
                MappingMethod mapping = remapped.get(type);
                if (desc == null) {
                    mappings.put(type, mapping.getOwner());
                    continue;
                }
                mappings.put(type, mapping.getDesc().replace(")V", ")L" + mapping.getOwner() + ";"));
            }
            this.obf.getReferenceManager().addClassMapping(this.classRef, reference, mappings);
        }
        elem.notifyRemapped();
    }

    protected final void remapReference(String subject, String reference, ITargetSelector selector, AnnotatedElementInjectionPoint elem) {
        if (!(selector instanceof ITargetSelectorRemappable)) {
            return;
        }
        ITargetSelectorRemappable targetMember = (ITargetSelectorRemappable)selector;
        AnnotationMirror errorElement = elem.getAtErrorElement(this.ap.getCompilerEnvironment());
        if (!targetMember.isFullyQualified()) {
            String missing = targetMember.getOwner() == null ? (targetMember.getDesc() == null ? "owner and descriptor" : "owner") : "descriptor";
            this.ap.printMessage(IMessagerEx.MessageType.INJECTOR_TARGET_NOT_FULLY_QUALIFIED, (CharSequence)(subject + " is not fully qualified, missing " + missing), (Element)elem.getElement(), errorElement);
            return;
        }
        try {
            if (targetMember.isField()) {
                ObfuscationData<MappingField> obfFieldData = this.obf.getDataProvider().getObfFieldRecursive(targetMember);
                if (obfFieldData.isEmpty()) {
                    if (targetMember.getOwner() == null || !SpecialPackages.isExcludedPackage(targetMember.getOwner())) {
                        this.ap.printMessage(IMessagerEx.MessageType.NO_OBFDATA_FOR_FIELD, (CharSequence)("Unable to locate field mapping for " + subject + " '" + reference + "'"), (Element)elem.getElement(), errorElement, SuppressedBy.MAPPING);
                    }
                    return;
                }
                this.obf.getReferenceManager().addFieldMapping(this.classRef, reference, targetMember, obfFieldData);
            } else {
                ObfuscationData<MappingMethod> obfMethodData = this.obf.getDataProvider().getObfMethodRecursive(targetMember);
                if (obfMethodData.isEmpty()) {
                    if (targetMember.getOwner() == null || !SpecialPackages.isExcludedPackage(targetMember.getOwner())) {
                        this.ap.printMessage(IMessagerEx.MessageType.NO_OBFDATA_FOR_METHOD, (CharSequence)("Unable to locate method mapping for " + subject + " '" + reference + "'"), (Element)elem.getElement(), errorElement, SuppressedBy.MAPPING);
                    }
                    return;
                }
                this.obf.getReferenceManager().addMethodMapping(this.classRef, reference, targetMember, obfMethodData);
            }
        }
        catch (ReferenceManager.ReferenceConflictException ex) {
            this.ap.printMessage(IMessagerEx.MessageType.INJECTOR_MAPPING_CONFLICT, (CharSequence)("Unexpected reference conflict for " + subject + ": " + reference + " -> " + ex.getNew() + " previously defined as " + ex.getOld()), (Element)elem.getElement(), errorElement);
            return;
        }
        elem.notifyRemapped();
    }

    static class AnnotatedElementSliceInjectionPoint
    extends AnnotatedElementInjectionPoint {
        private final ISelectorContext parentContext;

        public AnnotatedElementSliceInjectionPoint(ExecutableElement element, AnnotationHandle inject, IMixinContext context, String selectorCoordinate, AnnotationHandle at, InjectorRemap state, ISelectorContext parentContext) {
            super(element, inject, context, selectorCoordinate, at, state);
            this.parentContext = parentContext;
        }

        @Override
        public ISelectorContext getParent() {
            return this.parentContext;
        }
    }

    static class AnnotatedElementInjectionPoint
    extends AnnotatedMixinElementHandler.AnnotatedElementExecutable {
        private final AnnotationHandle at;
        private Map<String, String> args;
        private final InjectorRemap state;

        public AnnotatedElementInjectionPoint(ExecutableElement element, AnnotationHandle inject, IMixinContext context, String selectorCoordinate, AnnotationHandle at, InjectorRemap state) {
            super(element, inject, context, selectorCoordinate);
            this.at = at;
            this.state = state;
        }

        public boolean shouldRemap() {
            return this.at.getBoolean("remap", this.state.shouldRemap());
        }

        public AnnotationHandle getAt() {
            return this.at;
        }

        public AnnotationMirror getAtErrorElement(IMixinAnnotationProcessor.CompilerEnvironment compilerEnvironment) {
            return ((AnnotationHandle)(compilerEnvironment.isDevelopmentEnvironment() ? this.getAt() : this.getAnnotation())).asMirror();
        }

        @Override
        public IAnnotationHandle getSelectorAnnotation() {
            return this.getAt();
        }

        public String getAtArg(String key) {
            if (this.args == null) {
                this.args = new HashMap<String, String>();
                for (String arg : this.at.getList("args")) {
                    if (arg == null) continue;
                    int eqPos = arg.indexOf(61);
                    if (eqPos > -1) {
                        this.args.put(arg.substring(0, eqPos), arg.substring(eqPos + 1));
                        continue;
                    }
                    this.args.put(arg, "");
                }
            }
            return this.args.get(key);
        }

        public void notifyRemapped() {
            this.state.notifyRemapped();
        }
    }

    static class AnnotatedElementInjector
    extends AnnotatedMixinElementHandler.AnnotatedElementExecutable {
        private final InjectorRemap state;

        public AnnotatedElementInjector(ExecutableElement element, AnnotationHandle annotation, IMixinContext context, InjectorRemap shouldRemap) {
            super(element, annotation, context, "method");
            this.state = shouldRemap;
        }

        public boolean shouldRemap() {
            return this.state.shouldRemap();
        }

        public boolean hasCoerceArgument() {
            if (!this.annotation.toString().equals("@Inject")) {
                return false;
            }
            Iterator<? extends VariableElement> iterator = ((ExecutableElement)this.element).getParameters().iterator();
            if (iterator.hasNext()) {
                VariableElement param = iterator.next();
                return AnnotationHandle.of(param, Coerce.class).exists();
            }
            return false;
        }

        public void addMessage(IMessagerEx.MessageType type, CharSequence msg, Element element, AnnotationHandle annotation) {
            this.state.addMessage(type, msg, element, annotation);
        }

        @Override
        public String toString() {
            return ((AnnotationHandle)this.getAnnotation()).toString();
        }
    }
}

