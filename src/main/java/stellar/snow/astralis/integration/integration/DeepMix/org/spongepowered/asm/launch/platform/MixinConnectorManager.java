package org.spongepowered.asm.launch.platform;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.connect.IMixinConnector;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.MixinService;

public class MixinConnectorManager {
    private static final ILogger logger = MixinService.getService().getLogger("mixin");
    private final Set<String> connectorClasses = new LinkedHashSet<String>();
    private final List<IMixinConnector> connectors = new ArrayList<IMixinConnector>();

    MixinConnectorManager() {
    }

    void addConnector(String connectorClass) {
        this.connectorClasses.add(connectorClass);
    }

    void inject() {
        this.loadConnectors();
        this.initConnectors();
    }

    void loadConnectors() {
        IClassProvider classProvider = MixinService.getService().getClassProvider();
        for (String connectorClassName : this.connectorClasses) {
            Class<?> connectorClass = null;
            try {
                Class<?> clazz = classProvider.findClass(connectorClassName);
                if (!IMixinConnector.class.isAssignableFrom(clazz)) {
                    logger.error("Mixin Connector [" + connectorClassName + "] does not implement IMixinConnector", new Object[0]);
                    continue;
                }
                connectorClass = clazz;
            }
            catch (ClassNotFoundException ex) {
                logger.catching(ex);
                continue;
            }
            try {
                IMixinConnector connector = (IMixinConnector)connectorClass.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
                this.connectors.add(connector);
                logger.info("Successfully loaded Mixin Connector [{}]", connectorClassName);
            }
            catch (ReflectiveOperationException ex) {
                logger.warn("Error loading Mixin Connector [{}]", connectorClassName, ex);
            }
        }
        this.connectorClasses.clear();
    }

    void initConnectors() {
        for (IMixinConnector connector : this.connectors) {
            try {
                connector.connect();
            }
            catch (Exception ex) {
                logger.warn("Error initialising Mixin Connector [" + connector.getClass().getName() + "]", ex);
            }
        }
    }
}

