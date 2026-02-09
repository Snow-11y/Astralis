package org.spongepowered.tools.obfuscation.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import javax.tools.Diagnostic;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.tools.obfuscation.ObfuscationType;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.service.IObfuscationService;
import org.spongepowered.tools.obfuscation.service.ObfuscationTypeDescriptor;

public final class ObfuscationServices {
    private static ObfuscationServices instance;
    private final ServiceLoader<IObfuscationService> serviceLoader;
    private final Set<IObfuscationService> services = new HashSet<IObfuscationService>();
    private boolean providerInitDone = false;

    private ObfuscationServices() {
        this.serviceLoader = ServiceLoader.load(IObfuscationService.class, this.getClass().getClassLoader());
    }

    public static ObfuscationServices getInstance() {
        if (instance == null) {
            instance = new ObfuscationServices();
        }
        return instance;
    }

    public void initProviders(IMixinAnnotationProcessor ap) {
        if (this.providerInitDone) {
            return;
        }
        this.providerInitDone = true;
        boolean defaultIsPresent = false;
        LinkedHashMap<String, LinkedHashSet<String>> supportedTypes = new LinkedHashMap<String, LinkedHashSet<String>>();
        try {
            for (IObfuscationService service : this.serviceLoader) {
                if (this.services.contains(service)) continue;
                this.services.add(service);
                String serviceName = service.getClass().getSimpleName();
                Collection<ObfuscationTypeDescriptor> obfTypes = service.getObfuscationTypes(ap);
                if (obfTypes == null) continue;
                for (ObfuscationTypeDescriptor obfType : obfTypes) {
                    try {
                        ObfuscationType type = ObfuscationType.create(obfType, ap);
                        LinkedHashSet<String> types = (LinkedHashSet<String>)supportedTypes.get(serviceName);
                        if (types == null) {
                            types = new LinkedHashSet<String>();
                            supportedTypes.put(serviceName, types);
                        }
                        types.add(type.getKey());
                        defaultIsPresent |= type.isDefault();
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        catch (ServiceConfigurationError serviceError) {
            ap.printMessage(Diagnostic.Kind.ERROR, (CharSequence)(serviceError.getClass().getSimpleName() + ": " + serviceError.getMessage()));
            serviceError.printStackTrace();
        }
        if (supportedTypes.size() > 0) {
            StringBuilder sb = new StringBuilder("Supported obfuscation types:");
            for (Map.Entry supportedType : supportedTypes.entrySet()) {
                sb.append(' ').append((String)supportedType.getKey()).append(" supports [").append(Joiner.on(',').join((Iterable)supportedType.getValue())).append(']');
            }
            ap.printMessage(IMessagerEx.MessageType.INFO, (CharSequence)sb.toString());
        }
        if (!defaultIsPresent) {
            String defaultEnv = ap.getOption("defaultObfuscationEnv");
            if (defaultEnv == null) {
                ap.printMessage(Diagnostic.Kind.WARNING, (CharSequence)"No default obfuscation environment was specified and \"searge\" is not available. Please ensure defaultObfuscationEnv is specified in your build configuration");
            } else {
                ap.printMessage(Diagnostic.Kind.WARNING, (CharSequence)("Specified default obfuscation environment \"" + defaultEnv.toLowerCase(Locale.ROOT) + "\" was not defined. This probably means your build configuration is out of date or a required service is missing"));
            }
        }
    }

    public Set<String> getSupportedOptions() {
        HashSet<String> supportedOptions = new HashSet<String>();
        for (IObfuscationService provider : this.services) {
            Set<String> options = provider.getSupportedOptions();
            if (options == null) continue;
            supportedOptions.addAll(options);
        }
        return supportedOptions;
    }

    public IObfuscationService getService(Class<? extends IObfuscationService> serviceClass) {
        for (IObfuscationService service : this.services) {
            if (!serviceClass.getName().equals(service.getClass().getName())) continue;
            return service;
        }
        return null;
    }
}

