package org.spongepowered.asm.mixin.transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.MixinCoprocessor;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError;
import org.spongepowered.asm.util.asm.ClassNodeAdapter;

class MixinCoprocessorNestHost
extends MixinCoprocessor {
    private final Map<String, Set<String>> nestHosts = new HashMap<String, Set<String>>();

    MixinCoprocessorNestHost() {
    }

    void registerNestMember(String hostName, String memberName) {
        Set<String> nestMembers = this.nestHosts.get(hostName);
        if (nestMembers == null) {
            nestMembers = new HashSet<String>();
            this.nestHosts.put(hostName, nestMembers);
        }
        nestMembers.add(memberName);
    }

    @Override
    boolean postProcess(String className, ClassNode classNode) {
        if (!this.nestHosts.containsKey(className)) {
            return false;
        }
        Set<String> newMembers = this.nestHosts.get(className);
        if (!MixinEnvironment.getCompatibilityLevel().supports(8) || newMembers.isEmpty()) {
            return false;
        }
        String nestHost = ClassNodeAdapter.getNestHostClass(classNode);
        if (nestHost != null) {
            throw new MixinTransformerError(String.format("Nest host candidate %s is a nest member", classNode.name));
        }
        List<String> nestMembers = ClassNodeAdapter.getNestMembers(classNode);
        if (nestMembers == null) {
            nestMembers = new ArrayList<String>(newMembers);
        } else {
            LinkedHashSet<String> combinedMembers = new LinkedHashSet<String>(nestMembers);
            combinedMembers.addAll(newMembers);
            nestMembers.clear();
            nestMembers.addAll(combinedMembers);
        }
        ClassNodeAdapter.setNestMembers(classNode, nestMembers);
        return true;
    }
}

