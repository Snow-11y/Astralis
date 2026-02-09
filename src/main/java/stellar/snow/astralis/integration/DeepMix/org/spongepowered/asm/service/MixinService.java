package org.spongepowered.asm.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterConsole;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.IMixinServiceBootstrap;
import org.spongepowered.asm.service.ServiceInitialisationException;
import org.spongepowered.asm.service.ServiceNotAvailableError;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.collect.ObjectArrays;

public final class MixinService {
    private static LogBuffer logBuffer = new LogBuffer();
    private static MixinService instance;
    private ServiceLoader<IMixinServiceBootstrap> bootstrapServiceLoader;
    private final Set<String> bootedServices = new HashSet<String>();
    private ServiceLoader<IMixinService> serviceLoader;
    private IMixinService service = null;
    private IGlobalPropertyService propertyService;

    private MixinService() {
        this.runBootServices();
    }

    private void runBootServices() {
        String serviceCls = System.getProperty("mixin.bootstrapService");
        if (serviceCls != null) {
            try {
                IMixinServiceBootstrap bootService = (IMixinServiceBootstrap)Class.forName(serviceCls).getConstructor(new Class[0]).newInstance(new Object[0]);
                bootService.bootstrap();
                this.bootedServices.add(bootService.getServiceClassName());
                return;
            }
            catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        this.bootstrapServiceLoader = ServiceLoader.load(IMixinServiceBootstrap.class, this.getClass().getClassLoader());
        Iterator<IMixinServiceBootstrap> iter = this.bootstrapServiceLoader.iterator();
        while (iter.hasNext()) {
            try {
                IMixinServiceBootstrap bootService = iter.next();
                bootService.bootstrap();
                this.bootedServices.add(bootService.getServiceClassName());
            }
            catch (ServiceInitialisationException ex) {
                logBuffer.debug("Mixin bootstrap service {} is not available: {}", ex.getStackTrace()[0].getClassName(), ex.getMessage());
            }
            catch (Throwable th) {
                logBuffer.debug("Catching {}:{} initialising service", th.getClass().getName(), th.getMessage(), th);
            }
        }
    }

    private static MixinService getInstance() {
        if (instance == null) {
            instance = new MixinService();
        }
        return instance;
    }

    public static void boot() {
        MixinService.getInstance();
    }

    public static IMixinService getService() {
        return MixinService.getInstance().getServiceInstance();
    }

    private synchronized IMixinService getServiceInstance() {
        if (this.service == null) {
            try {
                this.service = this.initService();
                ILogger serviceLogger = this.service.getLogger("mixin");
                logBuffer.flush(serviceLogger);
            }
            catch (Error err) {
                ILogger defaultLogger = (ILogger)MixinService.getDefaultLogger();
                logBuffer.flush(defaultLogger);
                defaultLogger.error(err.getMessage(), err);
                throw err;
            }
        }
        return this.service;
    }

    private IMixinService initService() {
        String serviceCls = System.getProperty("mixin.service");
        if (serviceCls != null) {
            try {
                IMixinService service = (IMixinService)Class.forName(serviceCls).getConstructor(new Class[0]).newInstance(new Object[0]);
                if (!service.isValid()) {
                    throw new RuntimeException("invalid service " + serviceCls + " configured via system property");
                }
                return service;
            }
            catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        this.serviceLoader = ServiceLoader.load(IMixinService.class, this.getClass().getClassLoader());
        Iterator<IMixinService> iter = this.serviceLoader.iterator();
        ArrayList<String> badServices = new ArrayList<String>();
        int brokenServiceCount = 0;
        while (iter.hasNext()) {
            try {
                IMixinService service = iter.next();
                if (this.bootedServices.contains(service.getClass().getName())) {
                    logBuffer.debug("MixinService [{}] was successfully booted in {}", service.getName(), this.getClass().getClassLoader());
                }
                if (service.isValid()) {
                    return service;
                }
                logBuffer.debug("MixinService [{}] is not valid", service.getName());
                badServices.add(String.format("INVALID[%s]", service.getName()));
            }
            catch (ServiceConfigurationError sce) {
                ++brokenServiceCount;
            }
            catch (Throwable th) {
                String faultingClassName = th.getStackTrace()[0].getClassName();
                logBuffer.debug("MixinService [{}] failed initialisation: {}", faultingClassName, th.getMessage());
                int pos = faultingClassName.lastIndexOf(46);
                badServices.add(String.format("ERROR[%s]", pos < 0 ? faultingClassName : faultingClassName.substring(pos + 1)));
            }
        }
        String brokenServiceNote = brokenServiceCount == 0 ? "" : " and " + brokenServiceCount + " other invalid services.";
        throw new ServiceNotAvailableError("No mixin host service is available. Services: " + Joiner.on(", ").join(badServices) + brokenServiceNote);
    }

    public static IGlobalPropertyService getGlobalPropertyService() {
        return MixinService.getInstance().getGlobalPropertyServiceInstance();
    }

    private IGlobalPropertyService getGlobalPropertyServiceInstance() {
        if (this.propertyService == null) {
            this.propertyService = this.initPropertyService();
        }
        return this.propertyService;
    }

    private IGlobalPropertyService initPropertyService() {
        ServiceLoader<IGlobalPropertyService> serviceLoader = ServiceLoader.load(IGlobalPropertyService.class, this.getClass().getClassLoader());
        Iterator<IGlobalPropertyService> iter = serviceLoader.iterator();
        while (iter.hasNext()) {
            try {
                IGlobalPropertyService service = iter.next();
                return service;
            }
            catch (ServiceConfigurationError serviceConfigurationError) {}
            finally {
            }
        }
        throw new ServiceNotAvailableError("No mixin global property service is available");
    }

    private static <T> T getDefaultLogger() {
        return (T)new LoggerAdapterConsole("mixin").setDebugStream(System.err);
    }

    static class LogBuffer {
        private final List<LogEntry> buffer = new ArrayList<LogEntry>();
        private ILogger logger;

        LogBuffer() {
        }

        synchronized void debug(String message, Object ... params) {
            if (this.logger != null) {
                this.logger.debug(message, params);
                return;
            }
            this.buffer.add(new LogEntry(message, params, null));
        }

        synchronized void flush(ILogger logger) {
            for (LogEntry buffered : this.buffer) {
                if (buffered.t != null) {
                    logger.debug(buffered.message, ObjectArrays.concat(buffered.params, buffered.t));
                    continue;
                }
                logger.debug(buffered.message, buffered.params);
            }
            this.buffer.clear();
            this.logger = logger;
        }

        public static class LogEntry {
            public String message;
            public Object[] params;
            public Throwable t;

            public LogEntry(String message, Object[] params, Throwable t) {
                this.message = message;
                this.params = params;
                this.t = t;
            }
        }
    }
}

