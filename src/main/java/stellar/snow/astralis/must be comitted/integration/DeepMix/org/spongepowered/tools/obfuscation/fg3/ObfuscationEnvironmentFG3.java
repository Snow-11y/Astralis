package org.spongepowered.tools.obfuscation.fg3;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import org.spongepowered.tools.obfuscation.ObfuscationEnvironment;
import org.spongepowered.tools.obfuscation.ObfuscationType;
import org.spongepowered.tools.obfuscation.mapping.IMappingProvider;
import org.spongepowered.tools.obfuscation.mapping.IMappingWriter;
import org.spongepowered.tools.obfuscation.mapping.fg3.MappingProviderTSrg;
import org.spongepowered.tools.obfuscation.mapping.fg3.MappingWriterTSrg;

public class ObfuscationEnvironmentFG3
extends ObfuscationEnvironment {
    private MappingProviderTSrg provider;

    protected ObfuscationEnvironmentFG3(ObfuscationType type) {
        super(type);
    }

    @Override
    protected IMappingProvider getMappingProvider(Messager messager, Filer filer) {
        this.provider = new MappingProviderTSrg(messager, filer);
        return this.provider;
    }

    @Override
    protected IMappingWriter getMappingWriter(Messager messager, Filer filer) {
        String outputBehaviour = this.ap.getOption("mergeBehaviour");
        return new MappingWriterTSrg(messager, filer, this.provider, outputBehaviour != null && outputBehaviour.equalsIgnoreCase("merge"));
    }
}

