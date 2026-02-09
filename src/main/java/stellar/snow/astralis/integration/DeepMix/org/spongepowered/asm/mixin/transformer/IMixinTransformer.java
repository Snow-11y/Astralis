package org.spongepowered.asm.mixin.transformer;

import java.util.List;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinProcessor;
import org.spongepowered.asm.mixin.transformer.ext.IExtensionRegistry;

public interface IMixinTransformer {
    public void audit(MixinEnvironment var1);

    public List<String> reload(String var1, ClassNode var2);

    public boolean computeFramesForClass(MixinEnvironment var1, String var2, ClassNode var3);

    public byte[] transformClassBytes(String var1, String var2, byte[] var3);

    public byte[] transformClass(MixinEnvironment var1, String var2, byte[] var3);

    public boolean transformClass(MixinEnvironment var1, String var2, ClassNode var3);

    public byte[] generateClass(MixinEnvironment var1, String var2);

    public boolean generateClass(MixinEnvironment var1, String var2, ClassNode var3);

    public IExtensionRegistry getExtensions();

    public IMixinProcessor getProcessor();
}

