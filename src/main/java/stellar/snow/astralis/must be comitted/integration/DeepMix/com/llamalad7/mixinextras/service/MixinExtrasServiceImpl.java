package com.llamalad7.mixinextras.service;

import com.llamalad7.mixinextras.expression.impl.point.ExpressionInjectionPoint;
import com.llamalad7.mixinextras.expression.impl.wrapper.ExpressionInjectorWrapperInjectionInfo;
import com.llamalad7.mixinextras.injector.LateInjectionApplicatorExtension;
import com.llamalad7.mixinextras.injector.ModifyExpressionValueInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReceiverInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReturnValueInjectionInfo;
import com.llamalad7.mixinextras.injector.WrapWithConditionV1InjectionInfo;
import com.llamalad7.mixinextras.injector.v2.WrapWithConditionInjectionInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethodApplicatorExtension;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethodInjectionInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationInjectionInfo;
import com.llamalad7.mixinextras.lib.apache.commons.StringUtils;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;
import com.llamalad7.mixinextras.service.ServiceInitializationExtension;
import com.llamalad7.mixinextras.service.Versioned;
import com.llamalad7.mixinextras.sugar.impl.SugarPostProcessingExtension;
import com.llamalad7.mixinextras.sugar.impl.SugarWrapperInjectionInfo;
import com.llamalad7.mixinextras.transformer.MixinTransformerExtension;
import com.llamalad7.mixinextras.utils.MixinExtrasLogger;
import com.llamalad7.mixinextras.utils.MixinInternals;
import com.llamalad7.mixinextras.wrapper.factory.FactoryRedirectWrapperInjectionInfo;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;

public class MixinExtrasServiceImpl
implements MixinExtrasService {
    private static final MixinExtrasLogger LOGGER = MixinExtrasLogger.get("Service");
    private final List<Versioned<String>> offeredPackages = new ArrayList<Versioned<String>>();
    private final List<Versioned<IExtension>> offeredExtensions = new ArrayList<Versioned<IExtension>>();
    private final List<Versioned<Class<? extends InjectionInfo>>> offeredInjectors = new ArrayList<Versioned<Class<? extends InjectionInfo>>>();
    private final List<Versioned<Class<? extends InjectionPoint>>> offeredPoints = new ArrayList<Versioned<Class<? extends InjectionPoint>>>();
    private final String ownPackage = StringUtils.substringBefore(this.getClass().getName(), ".service.");
    private final List<Versioned<String>> allPackages = new ArrayList<Versioned<String>>(Collections.singletonList(new Versioned<String>(this.getVersion(), this.ownPackage)));
    private final List<IExtension> ownExtensions = Arrays.asList(new MixinTransformerExtension(), new ServiceInitializationExtension(this), new LateInjectionApplicatorExtension(), new SugarPostProcessingExtension(), new WrapMethodApplicatorExtension());
    private final List<Class<? extends InjectionInfo>> ownInjectors = Arrays.asList(ModifyExpressionValueInjectionInfo.class, ModifyReceiverInjectionInfo.class, ModifyReturnValueInjectionInfo.class, WrapOperationInjectionInfo.class, WrapWithConditionV1InjectionInfo.class);
    private final List<Versioned<Class<? extends InjectionInfo>>> ownGatedInjectors = Arrays.asList(new Versioned<Class<WrapWithConditionInjectionInfo>>(MixinExtrasVersion.V0_3_4.getNumber(), WrapWithConditionInjectionInfo.class), new Versioned<Class<WrapMethodInjectionInfo>>(MixinExtrasVersion.V0_4_0_BETA_1.getNumber(), WrapMethodInjectionInfo.class));
    private final List<Class<? extends InjectionPoint>> ownPoints = Arrays.asList(ExpressionInjectionPoint.class);
    private final List<Class<? extends InjectionInfo>> internalInjectors = Arrays.asList(SugarWrapperInjectionInfo.class, FactoryRedirectWrapperInjectionInfo.class, ExpressionInjectorWrapperInjectionInfo.class);
    private final List<String> registeredInjectors = new ArrayList<String>();
    boolean initialized;

    @Override
    public int getVersion() {
        return MixinExtrasVersion.LATEST.getNumber();
    }

    @Override
    public boolean shouldReplace(Object otherService) {
        return this.getVersion() > MixinExtrasService.getFrom(otherService).getVersion();
    }

    @Override
    public void takeControlFrom(Object olderService) {
        LOGGER.debug("{} is taking over from {}", this, olderService);
        this.ownExtensions.forEach(it -> MixinInternals.registerExtension(it, it instanceof ServiceInitializationExtension || it instanceof MixinTransformerExtension));
        this.ownInjectors.forEach(it -> this.registerInjector((Class<? extends InjectionInfo>)it, this.ownPackage));
        this.ownGatedInjectors.forEach(it -> this.registerInjector((Class)it.value, this.ownPackage));
    }

    @Override
    public void concedeTo(Object newerService, boolean wasActive) {
        this.requireNotInitialized();
        LOGGER.debug("{} is conceding to {}", this, newerService);
        MixinExtrasService newService = MixinExtrasService.getFrom(newerService);
        if (wasActive) {
            this.deInitialize();
        }
        this.offeredPackages.forEach(packageName -> newService.offerPackage(packageName.version, (String)packageName.value));
        newService.offerPackage(this.getVersion(), this.ownPackage);
        this.offeredExtensions.forEach(extension -> newService.offerExtension(extension.version, (IExtension)extension.value));
        this.ownExtensions.forEach(extension -> newService.offerExtension(this.getVersion(), (IExtension)extension));
        this.offeredInjectors.forEach(injector -> newService.offerInjector(injector.version, (Class)injector.value));
        this.ownInjectors.forEach(injector -> newService.offerInjector(this.getVersion(), (Class<? extends InjectionInfo>)injector));
        this.offeredPoints.forEach(point -> newService.offerInjectionPoint(point.version, (Class)point.value));
        this.ownPoints.forEach(point -> newService.offerInjectionPoint(this.getVersion(), (Class<? extends InjectionPoint>)point));
    }

    @Override
    public void offerPackage(int version, String packageName) {
        this.requireNotInitialized();
        this.offeredPackages.add(new Versioned<String>(version, packageName));
        this.allPackages.add(new Versioned<String>(version, packageName));
        this.ownInjectors.forEach(it -> this.registerInjector((Class<? extends InjectionInfo>)it, packageName));
        for (Versioned<Class<? extends InjectionInfo>> gatedInjector : this.ownGatedInjectors) {
            if (version < gatedInjector.version) continue;
            this.registerInjector((Class)gatedInjector.value, packageName);
        }
    }

    @Override
    public void offerExtension(int version, IExtension extension) {
        this.requireNotInitialized();
        this.offeredExtensions.add(new Versioned<IExtension>(version, extension));
    }

    @Override
    public void offerInjector(int version, Class<? extends InjectionInfo> injector) {
        this.requireNotInitialized();
        this.offeredInjectors.add(new Versioned<Class<? extends InjectionInfo>>(version, injector));
    }

    @Override
    public void offerInjectionPoint(int version, Class<? extends InjectionPoint> point) {
        this.requireNotInitialized();
        this.offeredPoints.add(new Versioned<Class<? extends InjectionPoint>>(version, point));
    }

    public String toString() {
        return String.format("%s(version=%s)", new Object[]{this.getClass().getName(), MixinExtrasVersion.LATEST});
    }

    @Override
    public void initialize() {
        this.requireNotInitialized();
        LOGGER.info("Initializing MixinExtras via {}.", this);
        this.detectBetaPackages();
        this.internalInjectors.forEach(InjectionInfo::register);
        this.ownPoints.forEach(MixinInternals::registerInjectionPoint);
        this.initialized = true;
    }

    private void deInitialize() {
        for (IExtension extension : this.ownExtensions) {
            MixinInternals.unregisterExtension(extension);
        }
        this.registeredInjectors.forEach(MixinInternals::unregisterInjector);
    }

    private void registerInjector(Class<? extends InjectionInfo> injector, String packageName) {
        String name = injector.getAnnotation(InjectionInfo.AnnotationType.class).value().getName();
        String suffix = StringUtils.removeStart(name, this.ownPackage);
        this.registeredInjectors.add(packageName + suffix);
        MixinInternals.registerInjector(packageName + suffix, injector);
    }

    public Type changePackage(Class<?> ourType, Type theirReference, Class<?> ourReference) {
        String suffix = StringUtils.substringAfter(ourReference.getName(), this.ownPackage);
        String theirPackage = StringUtils.substringBefore(theirReference.getClassName(), suffix);
        return Type.getObjectType((String)(theirPackage + StringUtils.substringAfter(ourType.getName(), this.ownPackage)).replace('.', '/'));
    }

    public Set<String> getAllClassNames(String ourName) {
        return this.getAllClassNamesAtLeast(ourName, Integer.MIN_VALUE);
    }

    public Set<String> getAllClassNamesAtLeast(String ourName, MixinExtrasVersion minVersion) {
        return this.getAllClassNamesAtLeast(ourName, minVersion.getNumber());
    }

    private Set<String> getAllClassNamesAtLeast(String ourName, int minVersion) {
        String ourBinaryName = ourName.replace('/', '.');
        return this.allPackages.stream().filter(it -> it.version >= minVersion).map(it -> (String)it.value).map(it -> StringUtils.replaceOnce(ourBinaryName, this.ownPackage, it)).collect(Collectors.toSet());
    }

    public boolean isClassOwned(String name) {
        return this.allPackages.stream().map(it -> (String)it.value).anyMatch(name::startsWith);
    }

    private void requireNotInitialized() {
        if (this.initialized) {
            throw new IllegalStateException("The MixinExtras service has already been selected and is initialized!");
        }
    }

    private void detectBetaPackages() {
        for (IExtension extension : MixinInternals.getExtensions().getActiveExtensions()) {
            String suffix;
            String name = extension.getClass().getName();
            if (!name.endsWith(suffix = ".sugar.impl.SugarApplicatorExtension") || this.isClassOwned(name)) continue;
            String packageName = StringUtils.removeEnd(name, suffix);
            MixinExtrasVersion version = this.getBetaVersion(packageName);
            this.allPackages.add(new Versioned<String>(version.getNumber(), packageName));
            LOGGER.warn("Found problematic active MixinExtras instance at {} (version {})", new Object[]{packageName, version});
            LOGGER.warn("Versions from 0.2.0-beta.1 to 0.2.0-beta.9 have limited support and it is strongly recommended to update.", new Object[0]);
        }
    }

    private MixinExtrasVersion getBetaVersion(String packageName) {
        String bootstrapClassName = packageName + ".MixinExtrasBootstrap";
        try {
            String versionName;
            Class<?> bootstrapClass = Class.forName(bootstrapClassName);
            Field versionField = bootstrapClass.getDeclaredField("VERSION");
            versionField.setAccessible(true);
            switch (versionName = (String)versionField.get(null)) {
                case "0.2.0-beta.1": {
                    return MixinExtrasVersion.V0_2_0_BETA_1;
                }
                case "0.2.0-beta.2": {
                    return MixinExtrasVersion.V0_2_0_BETA_2;
                }
                case "0.2.0-beta.3": {
                    return MixinExtrasVersion.V0_2_0_BETA_3;
                }
                case "0.2.0-beta.4": {
                    return MixinExtrasVersion.V0_2_0_BETA_4;
                }
                case "0.2.0-beta.5": {
                    return MixinExtrasVersion.V0_2_0_BETA_5;
                }
                case "0.2.0-beta.6": {
                    return MixinExtrasVersion.V0_2_0_BETA_6;
                }
                case "0.2.0-beta.7": {
                    return MixinExtrasVersion.V0_2_0_BETA_7;
                }
                case "0.2.0-beta.8": {
                    return MixinExtrasVersion.V0_2_0_BETA_8;
                }
                case "0.2.0-beta.9": {
                    return MixinExtrasVersion.V0_2_0_BETA_9;
                }
            }
            throw new IllegalArgumentException("Unrecognized version " + versionName);
        }
        catch (Exception e) {
            LOGGER.error(String.format("Failed to determine version of MixinExtras instance at %s, assuming 0.2.0-beta.1", packageName), e);
            return MixinExtrasVersion.V0_2_0_BETA_1;
        }
    }
}

