package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;
import java.util.Locale;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.mixin.injection.selectors.throwables.SelectorConstraintException;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.service.MixinService;

@InjectionPoint.AtCode(value="INVOKE")
public class BeforeInvoke
extends InjectionPoint {
    protected final ITargetSelector target;
    protected final boolean allowPermissive;
    protected final int ordinal;
    protected final String className;
    protected final IInjectionPointContext context;
    protected final IMixinContext mixin;
    protected final ILogger logger = MixinService.getService().getLogger("mixin");
    private boolean log = false;

    public BeforeInvoke(InjectionPointData data) {
        super(data);
        this.target = data.getTarget();
        this.ordinal = data.getOrdinal();
        this.log = data.get("log", false);
        this.className = this.getClassName();
        this.context = data.getContext();
        this.mixin = data.getMixin();
        this.allowPermissive = this.mixin.getOption(MixinEnvironment.Option.REFMAP_REMAP) && this.mixin.getOption(MixinEnvironment.Option.REFMAP_REMAP_ALLOW_PERMISSIVE) && !this.mixin.getReferenceMapper().isDefault();
    }

    private String getClassName() {
        InjectionPoint.AtCode atCode = this.getClass().getAnnotation(InjectionPoint.AtCode.class);
        return String.format("@At(%s)", atCode != null ? atCode.value() : this.getClass().getSimpleName().toUpperCase(Locale.ROOT));
    }

    public BeforeInvoke setLogging(boolean logging) {
        this.log = logging;
        return this;
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        this.log("{}->{} is searching for an injection point in method with descriptor {}", this.context, this.className, desc);
        boolean hasDescriptor = this.target instanceof ITargetSelectorByName && ((ITargetSelectorByName)this.target).getDesc() == null;
        boolean found = this.find(desc, insns, nodes, this.target, SearchType.STRICT);
        if (!found && hasDescriptor && this.allowPermissive) {
            this.logger.warn("STRICT match for {} using \"{}\" in {} returned 0 results, attempting permissive search. To inhibit permissive search set mixin.env.allowPermissiveMatch=false", this.className, this.target, this.mixin);
            found = this.find(desc, insns, nodes, this.target, SearchType.PERMISSIVE);
        }
        return found;
    }

    protected boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes, ITargetSelector selector, SearchType searchType) {
        if (selector == null) {
            return false;
        }
        ITargetSelector target = (searchType == SearchType.PERMISSIVE ? selector.configure(ITargetSelector.Configure.PERMISSIVE, new String[0]) : selector).configure(ITargetSelector.Configure.SELECT_INSTRUCTION, new String[0]);
        int ordinal = 0;
        int found = 0;
        int matchCount = 0;
        for (AbstractInsnNode insn : insns) {
            if (this.matchesInsn(insn)) {
                MemberInfo nodeInfo = new MemberInfo(insn);
                this.log("{}->{} is considering {}", this.context, this.className, nodeInfo);
                if (target.match(ElementNode.of(insn)).isExactMatch()) {
                    this.log("{}->{} > found a matching insn, checking preconditions...", this.context, this.className);
                    if (++matchCount > target.getMaxMatchCount()) break;
                    if (this.matchesOrdinal(ordinal)) {
                        this.log("{}->{} > > > found a matching insn at ordinal {}", this.context, this.className, ordinal);
                        if (this.addInsn(insns, nodes, insn)) {
                            ++found;
                        }
                    }
                    ++ordinal;
                }
            }
            this.inspectInsn(desc, insns, insn);
        }
        if (searchType == SearchType.PERMISSIVE && found > 1) {
            this.logger.warn("A permissive match for {} using \"{}\" in {} matched {} instructions, this may cause unexpected behaviour. To inhibit permissive search set mixin.env.allowPermissiveMatch=false", this.className, selector, this.mixin, found);
        }
        if (matchCount < target.getMinMatchCount()) {
            throw new SelectorConstraintException(target, String.format("%s did not match the required number of targets (required=%d, matched=%d)", target, selector.getMinMatchCount(), matchCount));
        }
        return found > 0;
    }

    protected boolean addInsn(InsnList insns, Collection<AbstractInsnNode> nodes, AbstractInsnNode insn) {
        nodes.add(insn);
        return true;
    }

    protected boolean matchesInsn(AbstractInsnNode insn) {
        return insn instanceof MethodInsnNode;
    }

    protected void inspectInsn(String desc, InsnList insns, AbstractInsnNode insn) {
    }

    protected boolean matchesOrdinal(int ordinal) {
        this.log("{}->{} > > comparing target ordinal {} with current ordinal {}", this.context, this.className, this.ordinal, ordinal);
        return this.ordinal == -1 || this.ordinal == ordinal;
    }

    protected void log(String message, Object ... params) {
        if (this.log) {
            this.logger.info(message, params);
        }
    }

    public static enum SearchType {
        STRICT,
        PERMISSIVE;

    }
}

