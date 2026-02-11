package org.spongepowered.asm.mixin.injection.struct;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorConstructor;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;
import org.spongepowered.asm.mixin.injection.struct.InvalidMemberDescriptorException;
import org.spongepowered.asm.mixin.injection.struct.TargetNotSupportedException;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Quantifier;
import org.spongepowered.asm.util.SignaturePrinter;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.include.com.google.common.base.Objects;
import org.spongepowered.include.com.google.common.base.Strings;

public final class MemberInfo
implements ITargetSelectorConstructor,
ITargetSelectorRemappable {
    private final String owner;
    private final String name;
    private final String desc;
    private final Quantifier matches;
    private final boolean forceField;
    private final String input;
    private final String tail;

    public MemberInfo(String name, Quantifier matches) {
        this(name, null, null, matches, null, null);
    }

    public MemberInfo(String name, String owner, Quantifier matches) {
        this(name, owner, null, matches, null, null);
    }

    public MemberInfo(String name, String owner, String desc) {
        this(name, owner, desc, Quantifier.DEFAULT, null, null);
    }

    public MemberInfo(String name, String owner, String desc, Quantifier matches) {
        this(name, owner, desc, matches, null, null);
    }

    public MemberInfo(String name, String owner, String desc, Quantifier matches, String tail) {
        this(name, owner, desc, matches, tail, null);
    }

    public MemberInfo(String name, String owner, String desc, Quantifier matches, String tail, String input) {
        if (owner != null && owner.contains(".")) {
            throw new IllegalArgumentException("Attempt to instance a MemberInfo with an invalid owner format");
        }
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.matches = matches;
        this.forceField = false;
        this.tail = tail;
        this.input = input;
    }

    public MemberInfo(AbstractInsnNode insn) {
        this.matches = Quantifier.DEFAULT;
        this.forceField = false;
        this.input = null;
        this.tail = null;
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode methodNode = (MethodInsnNode)insn;
            this.owner = methodNode.owner;
            this.name = methodNode.name;
            this.desc = methodNode.desc;
        } else if (insn instanceof FieldInsnNode) {
            FieldInsnNode fieldNode = (FieldInsnNode)insn;
            this.owner = fieldNode.owner;
            this.name = fieldNode.name;
            this.desc = fieldNode.desc;
        } else {
            throw new IllegalArgumentException("insn must be an instance of MethodInsnNode or FieldInsnNode");
        }
    }

    public MemberInfo(IMapping<?> mapping) {
        this.owner = mapping.getOwner();
        this.name = mapping.getSimpleName();
        this.desc = mapping.getDesc();
        this.matches = Quantifier.SINGLE;
        this.forceField = mapping.getType() == IMapping.Type.FIELD;
        this.tail = null;
        this.input = null;
    }

    private MemberInfo(MemberInfo remapped, MappingMethod method, boolean setOwner) {
        this.owner = setOwner ? method.getOwner() : remapped.owner;
        this.name = method.getSimpleName();
        this.desc = method.getDesc();
        this.matches = remapped.matches;
        this.forceField = false;
        this.tail = null;
        this.input = null;
    }

    private MemberInfo(MemberInfo original, String owner) {
        this.owner = owner;
        this.name = original.name;
        this.desc = original.desc;
        this.matches = original.matches;
        this.forceField = original.forceField;
        this.tail = original.tail;
        this.input = null;
    }

    @Override
    public ITargetSelector next() {
        return Strings.isNullOrEmpty(this.tail) ? null : MemberInfo.parse(this.tail, null);
    }

    @Override
    public String getOwner() {
        return this.owner;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDesc() {
        return this.desc;
    }

    @Override
    public int getMinMatchCount() {
        return this.matches.getClampedMin();
    }

    @Override
    public int getMaxMatchCount() {
        return this.matches.getClampedMax();
    }

    public String toString() {
        String desc;
        String owner = this.owner != null ? "L" + this.owner + ";" : "";
        String name = this.name != null ? this.name : "";
        String quantifier = this.matches.toString();
        String string = desc = this.desc != null ? this.desc : "";
        String separator = desc.startsWith("(") ? "" : (this.desc != null ? ":" : "");
        String tail = this.tail != null ? " -> " + this.tail : "";
        return owner + name + quantifier + separator + desc + tail;
    }

    @Deprecated
    public String toSrg() {
        if (!this.isFullyQualified()) {
            throw new MixinException("Cannot convert unqualified reference to SRG mapping");
        }
        if (this.desc.startsWith("(")) {
            return this.owner + "/" + this.name + " " + this.desc;
        }
        return this.owner + "/" + this.name;
    }

    @Override
    public String toDescriptor() {
        if (this.desc == null) {
            return "";
        }
        return new SignaturePrinter(this).setFullyQualified(true).toDescriptor();
    }

    @Override
    public String toCtorType() {
        if (this.input == null) {
            return null;
        }
        String returnType = this.getReturnType();
        if (returnType != null) {
            return returnType;
        }
        if (this.owner != null) {
            return this.owner;
        }
        if (this.name != null && this.desc == null) {
            return this.name;
        }
        return this.desc != null ? this.desc : this.input;
    }

    @Override
    public String toCtorDesc() {
        return Bytecode.changeDescriptorReturnType(this.desc, "V");
    }

    private String getReturnType() {
        if (this.desc == null || this.desc.indexOf(41) == -1 || this.desc.indexOf(40) != 0) {
            return null;
        }
        String returnType = this.desc.substring(this.desc.indexOf(41) + 1);
        if (returnType.startsWith("L") && returnType.endsWith(";")) {
            return returnType.substring(1, returnType.length() - 1);
        }
        return returnType;
    }

    @Override
    public IMapping<?> asMapping() {
        return this.isField() ? this.asFieldMapping() : this.asMethodMapping();
    }

    @Override
    public MappingMethod asMethodMapping() {
        if (!this.isFullyQualified()) {
            throw new MixinException("Cannot convert unqualified reference " + this + " to MethodMapping");
        }
        if (this.isField()) {
            throw new MixinException("Cannot convert a non-method reference " + this + " to MethodMapping");
        }
        return new MappingMethod(this.owner, this.name, this.desc);
    }

    @Override
    public MappingField asFieldMapping() {
        if (!this.isField()) {
            throw new MixinException("Cannot convert non-field reference " + this + " to FieldMapping");
        }
        return new MappingField(this.owner, this.name, this.desc);
    }

    @Override
    public boolean isFullyQualified() {
        return this.owner != null && this.name != null && this.desc != null;
    }

    @Override
    public boolean isField() {
        return this.forceField || this.desc != null && !this.desc.startsWith("(");
    }

    @Override
    public boolean isConstructor() {
        return "<init>".equals(this.name);
    }

    @Override
    public boolean isClassInitialiser() {
        return "<clinit>".equals(this.name);
    }

    @Override
    public boolean isInitialiser() {
        return this.isConstructor() || this.isClassInitialiser();
    }

    @Override
    public MemberInfo validate() throws InvalidSelectorException {
        if (this.getMaxMatchCount() == 0) {
            throw new InvalidMemberDescriptorException(this.input, "Malformed quantifier in selector: " + this.input);
        }
        if (this.owner != null) {
            if (!this.owner.matches("(?i)^[\\w\\p{Sc}/]+$")) {
                throw new InvalidMemberDescriptorException(this.input, "Invalid owner: " + this.owner);
            }
            if (this.input != null && this.input.lastIndexOf(46) > 0 && this.owner.startsWith("L")) {
                throw new InvalidMemberDescriptorException(this.input, "Malformed owner: " + this.owner + " If you are seeing this messageunexpectedly and the owner appears to be correct, replace the owner descriptor with formal type L" + this.owner + "; to suppress this error");
            }
        }
        if (this.name != null && !this.name.matches("(?i)^<?[\\w\\p{Sc}]+>?$")) {
            throw new InvalidMemberDescriptorException(this.input, "Invalid name: " + this.name);
        }
        if (this.desc != null) {
            if (!this.desc.matches("^(\\([\\w\\p{Sc}\\[/;]*\\))?\\[*[\\w\\p{Sc}/;]+$")) {
                throw new InvalidMemberDescriptorException(this.input, "Invalid descriptor: " + this.desc);
            }
            if (this.isField()) {
                if (!this.desc.equals(Type.getType((String)this.desc).getDescriptor())) {
                    throw new InvalidMemberDescriptorException(this.input, "Invalid field type in descriptor: " + this.desc);
                }
            } else {
                try {
                    Type[] argTypes = Type.getArgumentTypes((String)this.desc);
                    if (ASM.isAtLeastVersion(6)) {
                        for (Type argType : argTypes) {
                            argType.getInternalName();
                        }
                    }
                }
                catch (Exception ex) {
                    throw new InvalidMemberDescriptorException(this.input, "Invalid descriptor: " + this.desc);
                }
                String retString = this.desc.substring(this.desc.indexOf(41) + 1);
                try {
                    Type retType = Type.getType((String)retString);
                    int sort = retType.getSort();
                    if (sort >= 9) {
                        retType.getInternalName();
                    }
                    if (!retString.equals(retType.getDescriptor())) {
                        throw new InvalidMemberDescriptorException(this.input, "Invalid return type \"" + retString + "\" in descriptor: " + this.desc);
                    }
                }
                catch (Exception ex) {
                    throw new InvalidMemberDescriptorException(this.input, "Invalid return type \"" + retString + "\" in descriptor: " + this.desc);
                }
            }
        }
        return this;
    }

    @Override
    public <TNode> MatchResult match(ElementNode<TNode> node) {
        return node == null ? MatchResult.NONE : this.matches(node.getOwner(), node.getName(), node.getDesc());
    }

    @Override
    public MatchResult matches(String owner, String name, String desc) {
        if (this.desc != null && desc != null && !this.desc.equals(desc)) {
            return MatchResult.NONE;
        }
        if (this.owner != null && owner != null && !this.owner.equals(owner)) {
            return MatchResult.NONE;
        }
        if (this.name != null && name != null) {
            if (this.name.equals(name)) {
                return MatchResult.EXACT_MATCH;
            }
            if (this.name.equalsIgnoreCase(name)) {
                return MatchResult.MATCH;
            }
            return MatchResult.NONE;
        }
        return MatchResult.EXACT_MATCH;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ITargetSelectorByName)) {
            return false;
        }
        ITargetSelectorByName other = (ITargetSelectorByName)obj;
        boolean otherForceField = other instanceof MemberInfo ? ((MemberInfo)other).forceField : (other instanceof ITargetSelectorRemappable ? ((ITargetSelectorRemappable)other).isField() : false);
        return this.compareMatches(other) && this.forceField == otherForceField && Objects.equal(this.owner, other.getOwner()) && Objects.equal(this.name, other.getName()) && Objects.equal(this.desc, other.getDesc());
    }

    private boolean compareMatches(ITargetSelectorByName other) {
        if (other instanceof MemberInfo) {
            return ((MemberInfo)other).matches.equals(this.matches);
        }
        return this.getMinMatchCount() == other.getMinMatchCount() && this.getMaxMatchCount() == other.getMaxMatchCount();
    }

    public int hashCode() {
        return Objects.hashCode(this.matches, this.owner, this.name, this.desc);
    }

    @Override
    public ITargetSelector configure(ITargetSelector.Configure request, String ... args) {
        request.checkArgs(args);
        switch (request) {
            case SELECT_MEMBER: {
                if (!this.matches.isDefault()) break;
                return new MemberInfo(this.name, this.owner, this.desc, Quantifier.SINGLE, this.tail);
            }
            case SELECT_INSTRUCTION: {
                if (!this.matches.isDefault()) break;
                return new MemberInfo(this.name, this.owner, this.desc, Quantifier.ANY, this.tail);
            }
            case MOVE: {
                return this.move(Strings.emptyToNull(args[0]));
            }
            case ORPHAN: {
                return this.move(null);
            }
            case TRANSFORM: {
                return this.transform(Strings.emptyToNull(args[0]));
            }
            case PERMISSIVE: {
                return this.transform(null);
            }
            case CLEAR_LIMITS: {
                if (this.matches.getMin() == 0 && this.matches.getMax() >= Integer.MAX_VALUE) break;
                return new MemberInfo(this.name, this.owner, this.desc, Quantifier.ANY, this.tail);
            }
        }
        return this;
    }

    @Override
    public ITargetSelector attach(ISelectorContext context) throws InvalidSelectorException {
        if (this.owner != null && !this.owner.equals(context.getMixin().getTargetClassRef())) {
            throw new TargetNotSupportedException(this.owner);
        }
        return this;
    }

    @Override
    public ITargetSelectorRemappable move(String newOwner) {
        if (newOwner == null && this.owner == null || newOwner != null && newOwner.equals(this.owner)) {
            return this;
        }
        return new MemberInfo(this, newOwner);
    }

    @Override
    public ITargetSelectorRemappable transform(String newDesc) {
        if (newDesc == null && this.desc == null || newDesc != null && newDesc.equals(this.desc)) {
            return this;
        }
        return new MemberInfo(this.name, this.owner, newDesc, this.matches);
    }

    @Override
    public ITargetSelectorRemappable remapUsing(MappingMethod srgMethod, boolean setOwner) {
        return new MemberInfo(this, srgMethod, setOwner);
    }

    public static MemberInfo parse(String input, ISelectorContext context) {
        String desc = null;
        String owner = null;
        String name = Strings.nullToEmpty(input).replaceAll("\\s", "");
        String tail = null;
        int arrowPos = name.indexOf("->");
        if (arrowPos > -1) {
            tail = name.substring(arrowPos + 2);
            name = name.substring(0, arrowPos);
        }
        if (context != null) {
            name = context.remap(name);
        }
        int lastDotPos = name.lastIndexOf(46);
        int semiColonPos = name.indexOf(59);
        if (lastDotPos > -1) {
            owner = name.substring(0, lastDotPos).replace('.', '/');
            name = name.substring(lastDotPos + 1);
        } else if (semiColonPos > -1 && name.startsWith("L")) {
            owner = name.substring(1, semiColonPos).replace('.', '/');
            name = name.substring(semiColonPos + 1);
        }
        int parenPos = name.indexOf(40);
        int colonPos = name.indexOf(58);
        if (parenPos > -1) {
            desc = name.substring(parenPos);
            name = name.substring(0, parenPos);
        } else if (colonPos > -1) {
            desc = name.substring(colonPos + 1);
            name = name.substring(0, colonPos);
        }
        if ((name.indexOf(47) > -1 || name.indexOf(46) > -1) && owner == null) {
            owner = name;
            name = "";
        }
        Quantifier quantifier = Quantifier.DEFAULT;
        if (name.endsWith("*")) {
            quantifier = Quantifier.ANY;
            name = name.substring(0, name.length() - 1);
        } else if (name.endsWith("+")) {
            quantifier = Quantifier.PLUS;
            name = name.substring(0, name.length() - 1);
        } else if (name.endsWith("}")) {
            quantifier = Quantifier.NONE;
            int bracePos = name.indexOf("{");
            if (bracePos >= 0) {
                try {
                    quantifier = Quantifier.parse(name.substring(bracePos, name.length()));
                    name = name.substring(0, bracePos);
                }
                catch (Exception exception) {}
            }
        } else if (name.indexOf("{") >= 0) {
            quantifier = Quantifier.NONE;
        }
        if (name.isEmpty()) {
            name = null;
        }
        return new MemberInfo(name, owner, desc, quantifier, tail, input);
    }

    public static MemberInfo fromMapping(IMapping<?> mapping) {
        return new MemberInfo(mapping);
    }
}

