package org.spongepowered.tools.obfuscation.interfaces;

import java.util.List;

public interface IOptionProvider {
    public String getOption(String var1);

    public String getOption(String var1, String var2);

    public boolean getOption(String var1, boolean var2);

    public List<String> getOptions(String var1);
}

