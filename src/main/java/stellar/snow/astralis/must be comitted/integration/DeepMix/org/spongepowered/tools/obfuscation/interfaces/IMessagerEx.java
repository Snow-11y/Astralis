package org.spongepowered.tools.obfuscation.interfaces;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IOptionProvider;

public interface IMessagerEx
extends Messager {
    public void printMessage(MessageType var1, CharSequence var2);

    public void printMessage(MessageType var1, CharSequence var2, Element var3);

    public void printMessage(MessageType var1, CharSequence var2, Element var3, AnnotationMirror var4);

    public void printMessage(MessageType var1, CharSequence var2, Element var3, AnnotationMirror var4, AnnotationValue var5);

    public static enum MessageType {
        INFO(Diagnostic.Kind.NOTE),
        NOTE(Diagnostic.Kind.NOTE),
        ERROR(Diagnostic.Kind.ERROR),
        WARNING(Diagnostic.Kind.WARNING),
        MIXIN_ON_INVALID_TYPE(Diagnostic.Kind.ERROR),
        MIXIN_SOFT_TARGET_NOT_FOUND(Diagnostic.Kind.ERROR),
        MIXIN_SOFT_TARGET_NOT_RESOLVED(Diagnostic.Kind.WARNING),
        MIXIN_SOFT_TARGET_IS_PUBLIC(Diagnostic.Kind.WARNING),
        MIXIN_NO_TARGETS(Diagnostic.Kind.ERROR),
        PARENT_VALIDATOR(Diagnostic.Kind.ERROR),
        TARGET_VALIDATOR(Diagnostic.Kind.ERROR),
        ACCESSOR_ATTACH_ERROR(Diagnostic.Kind.ERROR),
        ACCESSOR_TARGET_NOT_FOUND(Diagnostic.Kind.ERROR),
        ACCESSOR_TYPE_UNSUPPORTED(Diagnostic.Kind.WARNING),
        ACCESSOR_NAME_UNRESOLVED(Diagnostic.Kind.WARNING),
        INVOKER_RAW_RETURN_TYPE(Diagnostic.Kind.WARNING),
        FACTORY_INVOKER_GENERIC_ARGS(Diagnostic.Kind.ERROR),
        FACTORY_INVOKER_RETURN_TYPE(Diagnostic.Kind.ERROR),
        FACTORY_INVOKER_NONSTATIC(Diagnostic.Kind.ERROR),
        CONSTRAINT_VIOLATION(Diagnostic.Kind.ERROR),
        INVALID_CONSTRAINT(Diagnostic.Kind.WARNING),
        ACCESSOR_MAPPING_CONFLICT(Diagnostic.Kind.ERROR),
        INJECTOR_MAPPING_CONFLICT(Diagnostic.Kind.ERROR),
        OVERWRITE_MAPPING_CONFLICT(Diagnostic.Kind.ERROR),
        SHADOW_MAPPING_CONFLICT(Diagnostic.Kind.ERROR),
        INJECTOR_IN_INTERFACE(Diagnostic.Kind.ERROR),
        INJECTOR_ON_NON_METHOD_ELEMENT(Diagnostic.Kind.WARNING),
        OVERWRITE_ON_NON_METHOD_ELEMENT(Diagnostic.Kind.ERROR),
        ACCESSOR_ON_NON_METHOD_ELEMENT(Diagnostic.Kind.ERROR),
        SHADOW_ON_INVALID_ELEMENT(Diagnostic.Kind.ERROR),
        INJECTOR_ON_NON_MIXIN_METHOD(Diagnostic.Kind.ERROR),
        OVERWRITE_ON_NON_MIXIN_METHOD(Diagnostic.Kind.ERROR),
        ACCESSOR_ON_NON_MIXIN_METHOD(Diagnostic.Kind.ERROR),
        SHADOW_ON_NON_MIXIN_ELEMENT(Diagnostic.Kind.ERROR),
        SOFT_IMPLEMENTS_ON_INVALID_TYPE(Diagnostic.Kind.ERROR),
        SOFT_IMPLEMENTS_ON_NON_MIXIN(Diagnostic.Kind.ERROR),
        SOFT_IMPLEMENTS_EMPTY(Diagnostic.Kind.WARNING),
        TARGET_SELECTOR_VALIDATION(Diagnostic.Kind.ERROR),
        INJECTOR_TARGET_NOT_FULLY_QUALIFIED(Diagnostic.Kind.ERROR),
        MISSING_INJECTOR_DESC_MULTITARGET(Diagnostic.Kind.ERROR),
        MISSING_INJECTOR_DESC_SINGLETARGET(Diagnostic.Kind.WARNING),
        MISSING_INJECTOR_DESC_SIMULATED(Diagnostic.Kind.OTHER),
        TARGET_ELEMENT_NOT_FOUND(Diagnostic.Kind.WARNING),
        METHOD_VISIBILITY(Diagnostic.Kind.WARNING),
        NO_OBFDATA_FOR_ACCESSOR(Diagnostic.Kind.WARNING),
        NO_OBFDATA_FOR_CLASS(Diagnostic.Kind.WARNING),
        NO_OBFDATA_FOR_TARGET(Diagnostic.Kind.ERROR),
        NO_OBFDATA_FOR_CTOR(Diagnostic.Kind.WARNING),
        NO_OBFDATA_FOR_OVERWRITE(Diagnostic.Kind.ERROR),
        NO_OBFDATA_FOR_STATIC_OVERWRITE(Diagnostic.Kind.WARNING),
        NO_OBFDATA_FOR_FIELD(Diagnostic.Kind.WARNING),
        NO_OBFDATA_FOR_METHOD(Diagnostic.Kind.WARNING),
        NO_OBFDATA_FOR_SHADOW(Diagnostic.Kind.WARNING),
        NO_OBFDATA_FOR_SIMULATED_SHADOW(Diagnostic.Kind.WARNING),
        NO_OBFDATA_FOR_SOFT_IMPLEMENTS(Diagnostic.Kind.ERROR),
        BARE_REFERENCE(Diagnostic.Kind.WARNING),
        OVERWRITE_DOCS(Diagnostic.Kind.WARNING);

        private static boolean decorate;
        private static String prefix;
        private final Diagnostic.Kind originalKind;
        private Diagnostic.Kind kind;
        private boolean enabled = true;
        private boolean setByUser = false;

        private MessageType(Diagnostic.Kind kind) {
            this.originalKind = this.kind = kind;
        }

        public boolean isError() {
            return this.kind == Diagnostic.Kind.ERROR;
        }

        public Diagnostic.Kind getKind() {
            return this.kind;
        }

        public void setKind(Diagnostic.Kind kind) {
            this.kind = kind;
            this.setByUser = true;
        }

        public void quench(Diagnostic.Kind kind) {
            if (!this.setByUser && kind.ordinal() > this.kind.ordinal()) {
                this.kind = kind;
            }
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            this.setByUser = true;
        }

        public void reset() {
            this.kind = this.originalKind;
            this.enabled = true;
            this.setByUser = false;
        }

        public CharSequence decorate(CharSequence msg) {
            return decorate ? String.format("%s[%s] %s", prefix, this.name(), msg) : prefix + msg;
        }

        public static void setDecoration(boolean enabled) {
            decorate = enabled;
        }

        public static void setPrefix(String prefix) {
            MessageType.prefix = prefix;
        }

        public static Set<String> getSupportedOptions() {
            HashSet<String> supportedOptions = new HashSet<String>();
            for (MessageType type : MessageType.values()) {
                supportedOptions.add("MSG_" + type.name());
            }
            return supportedOptions;
        }

        public static void applyOptions(IMixinAnnotationProcessor.CompilerEnvironment env, IOptionProvider options) {
            MessageType.setDecoration("true".equalsIgnoreCase(options.getOption("showMessageTypes")));
            INFO.setEnabled(!env.isDevelopmentEnvironment() && !"true".equalsIgnoreCase(options.getOption("quiet")));
            INJECTOR_IN_INTERFACE.setEnabled(options.getOption("disableInterfaceMixins", false));
            if ("error".equalsIgnoreCase(options.getOption("overwriteErrorLevel"))) {
                OVERWRITE_DOCS.setKind(Diagnostic.Kind.ERROR);
            }
            for (MessageType type : MessageType.values()) {
                String option = options.getOption("MSG_" + type.name());
                if (option == null) continue;
                if ("note".equalsIgnoreCase(option)) {
                    type.setKind(Diagnostic.Kind.NOTE);
                    continue;
                }
                if ("warning".equalsIgnoreCase(option)) {
                    type.setKind(Diagnostic.Kind.WARNING);
                    continue;
                }
                if ("error".equalsIgnoreCase(option)) {
                    type.setKind(Diagnostic.Kind.ERROR);
                    continue;
                }
                if (!"disabled".equalsIgnoreCase(option)) continue;
                type.setEnabled(false);
            }
        }

        static {
            decorate = false;
            prefix = "";
        }
    }
}

