package org.spongepowered.asm.mixin.transformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.MixinConfig;
import org.spongepowered.asm.mixin.transformer.MixinInfo;
import org.spongepowered.asm.util.Bytecode;

public final class MixinInheritanceTracker
extends Enum<MixinInheritanceTracker>
implements MixinConfig.IListener {
    public static final /* enum */ MixinInheritanceTracker INSTANCE = new MixinInheritanceTracker();
    private final Map<String, List<MixinInfo>> parentMixins = new HashMap<String, List<MixinInfo>>();
    private static final /* synthetic */ MixinInheritanceTracker[] $VALUES;

    public static MixinInheritanceTracker[] values() {
        return (MixinInheritanceTracker[])$VALUES.clone();
    }

    public static MixinInheritanceTracker valueOf(String name) {
        return Enum.valueOf(MixinInheritanceTracker.class, name);
    }

    @Override
    public void onPrepare(MixinInfo mixin) {
    }

    @Override
    public void onInit(MixinInfo mixin) {
        ClassInfo mixinInfo = mixin.getClassInfo();
        assert (mixinInfo.isMixin());
        for (ClassInfo superType = mixinInfo.getSuperClass(); superType != null && superType.isMixin(); superType = superType.getSuperClass()) {
            List<MixinInfo> children = this.parentMixins.get(superType.getName());
            if (children == null) {
                children = new ArrayList<MixinInfo>();
                this.parentMixins.put(superType.getName(), children);
            }
            children.add(mixin);
        }
    }

    public List<MethodNode> findOverrides(ClassInfo owner, String name, String desc) {
        return this.findOverrides(owner.getName(), name, desc);
    }

    public List<MethodNode> findOverrides(String owner, String name, String desc) {
        List<MixinInfo> children = this.parentMixins.get(owner);
        if (children == null) {
            return Collections.emptyList();
        }
        ArrayList out = new ArrayList(children.size());
        block4: for (MixinInfo child : children) {
            MixinInfo.MixinClassNode node = child.getClassNode(6);
            MethodNode method = Bytecode.findMethod(node, name, desc);
            if (method == null || Bytecode.isStatic(method)) continue;
            switch (Bytecode.getVisibility(method)) {
                case PRIVATE: {
                    break;
                }
                case PACKAGE: {
                    int childSplit;
                    int ownerSplit = owner.lastIndexOf(47);
                    if (ownerSplit != (childSplit = node.name.lastIndexOf(47))) continue block4;
                    if (ownerSplit > 0 && !owner.regionMatches(0, node.name, 0, ownerSplit + 1)) break;
                    out.add(method);
                    break;
                }
                default: {
                    out.add(method);
                }
            }
        }
        return out.isEmpty() ? Collections.emptyList() : out;
    }

    static {
        $VALUES = new MixinInheritanceTracker[]{INSTANCE};
    }
}

