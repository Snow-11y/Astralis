package stellar.snow.astralis.engine.ecs.integration;

/*
 * ════════════════════════════════════════════════════════════════════════════════════════════════
 * Astralis ECS Interoperability Bridge
 * 
 * Copyright © 2026 Astralis ECS Project
 * Licensed under PolyForm Shield License 1.0.0
 * 
 * LEGAL: This file implements interoperability interfaces based on publicly documented APIs.
 * See LegalCompliance.java for full legal documentation including Oracle v. Google precedent.
 * ════════════════════════════════════════════════════════════════════════════════════════════════
 */

import stellar.snow.astralis.engine.ecs.core.*;
import stellar.snow.astralis.engine.ecs.storage.*;
import stellar.snow.astralis.engine.ecs.config.ECSConfig;
import stellar.snow.astralis.engine.ecs.events.EventBus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Enhanced interoperability bridge for cross-system ECS integration.
 * 
 * <h2>Purpose</h2>
 * <p>Provides advanced bridging capabilities between Astralis ECS and other entity-component
 * systems, enabling seamless migration, hybrid architectures, and system interoperation.</p>
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Bidirectional Entity Mapping:</b> Synchronize entities between systems</li>
 *   <li><b>Component Translation:</b> Convert component data between formats</li>
 *   <li><b>System Bridging:</b> Execute systems across different ECS implementations</li>
 *   <li><b>Event Forwarding:</b> Route events between event buses</li>
 *   <li><b>Migration Tools:</b> Utilities for gradual system migration</li>
 * </ul>
 * 
 * @author Astralis ECS Project
 * @version 1.0.0
 * @since Java 21
 */
public final class InteroperabilityBridge {

    private static final InteroperabilityBridge INSTANCE = new InteroperabilityBridge();
    
    private final Map<String, EntityMapper> entityMappers = new ConcurrentHashMap<>();
    private final Map<String, ComponentTranslator<?>> componentTranslators = new ConcurrentHashMap<>();
    private final Map<String, SystemBridge> systemBridges = new ConcurrentHashMap<>();
    private final List<EventForwarder> eventForwarders = new ArrayList<>();

    private InteroperabilityBridge() {}

    /**
     * Get singleton instance.
     */
    public static InteroperabilityBridge getInstance() {
        return INSTANCE;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ENTITY MAPPING
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Entity mapper for bidirectional entity synchronization.
     */
    public static class EntityMapper {
        private final Map<Object, Entity> externalToInternal = new ConcurrentHashMap<>();
        private final Map<Entity, Object> internalToExternal = new ConcurrentHashMap<>();
        private final String systemName;

        public EntityMapper(String systemName) {
            this.systemName = systemName;
        }

        /**
         * Map external entity to Astralis entity.
         */
        public void mapEntity(Object externalEntity, Entity internalEntity) {
            externalToInternal.put(externalEntity, internalEntity);
            internalToExternal.put(internalEntity, externalEntity);
        }

        /**
         * Get Astralis entity for external entity.
         */
        public Entity getInternalEntity(Object externalEntity) {
            return externalToInternal.get(externalEntity);
        }

        /**
         * Get external entity for Astralis entity.
         */
        public Object getExternalEntity(Entity internalEntity) {
            return internalToExternal.get(internalEntity);
        }

        /**
         * Remove mapping.
         */
        public void removeMapping(Object externalEntity) {
            Entity internal = externalToInternal.remove(externalEntity);
            if (internal != null) {
                internalToExternal.remove(internal);
            }
        }

        /**
         * Get system name.
         */
        public String getSystemName() {
            return systemName;
        }
    }

    /**
     * Register entity mapper for external system.
     */
    public void registerEntityMapper(String systemName, EntityMapper mapper) {
        entityMappers.put(systemName, mapper);
        if (ECSConfig.enableDetailedLogging()) {
            System.out.println("[Interoperability] Registered entity mapper: " + systemName);
        }
    }

    /**
     * Get entity mapper for system.
     */
    public EntityMapper getEntityMapper(String systemName) {
        return entityMappers.computeIfAbsent(systemName, EntityMapper::new);
    }

    // ════════════════════════════════════════════════════════════════════════
    // COMPONENT TRANSLATION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Component translator for converting between component formats.
     * 
     * @param <T> Target component type
     */
    public interface ComponentTranslator<T> {
        /**
         * Translate external component to Astralis component.
         */
        T translateToAstralis(Object externalComponent);

        /**
         * Translate Astralis component to external format.
         */
        Object translateFromAstralis(T astralisComponent);

        /**
         * Get Astralis component class.
         */
        Class<T> getAstralisComponentClass();

        /**
         * Get external component class name.
         */
        String getExternalComponentClassName();
    }

    /**
     * Register component translator.
     */
    public <T> void registerComponentTranslator(String componentName, ComponentTranslator<T> translator) {
        componentTranslators.put(componentName, translator);
        if (ECSConfig.enableDetailedLogging()) {
            System.out.println("[Interoperability] Registered component translator: " + componentName);
        }
    }

    /**
     * Get component translator.
     */
    @SuppressWarnings("unchecked")
    public <T> ComponentTranslator<T> getComponentTranslator(String componentName) {
        return (ComponentTranslator<T>) componentTranslators.get(componentName);
    }

    /**
     * Translate external component to Astralis format.
     */
    public <T> T translateToAstralis(String componentName, Object externalComponent) {
        ComponentTranslator<T> translator = getComponentTranslator(componentName);
        if (translator == null) {
            throw new IllegalArgumentException("No translator for component: " + componentName);
        }
        return translator.translateToAstralis(externalComponent);
    }

    /**
     * Translate Astralis component to external format.
     */
    public <T> Object translateFromAstralis(String componentName, T astralisComponent) {
        ComponentTranslator<T> translator = getComponentTranslator(componentName);
        if (translator == null) {
            throw new IllegalArgumentException("No translator for component: " + componentName);
        }
        return translator.translateFromAstralis(astralisComponent);
    }

    // ════════════════════════════════════════════════════════════════════════
    // SYSTEM BRIDGING
    // ════════════════════════════════════════════════════════════════════════

    /**
     * System bridge for executing external systems in Astralis context.
     */
    public static class SystemBridge {
        private final String systemName;
        private final Runnable updateCallback;

        public SystemBridge(String systemName, Runnable updateCallback) {
            this.systemName = systemName;
            this.updateCallback = updateCallback;
        }

        /**
         * Execute bridged system.
         */
        public void execute() {
            updateCallback.run();
        }

        /**
         * Get system name.
         */
        public String getSystemName() {
            return systemName;
        }
    }

    /**
     * Register system bridge.
     */
    public void registerSystemBridge(String systemName, SystemBridge bridge) {
        systemBridges.put(systemName, bridge);
        if (ECSConfig.enableDetailedLogging()) {
            System.out.println("[Interoperability] Registered system bridge: " + systemName);
        }
    }

    /**
     * Get system bridge.
     */
    public SystemBridge getSystemBridge(String systemName) {
        return systemBridges.get(systemName);
    }

    /**
     * Execute all bridged systems.
     */
    public void executeAllBridgedSystems() {
        systemBridges.values().forEach(SystemBridge::execute);
    }

    // ════════════════════════════════════════════════════════════════════════
    // EVENT FORWARDING
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Event forwarder for routing events between systems.
     */
    public static class EventForwarder {
        private final String sourceName;
        private final String targetName;
        private final Consumer<Object> forwardCallback;

        public EventForwarder(String sourceName, String targetName, Consumer<Object> forwardCallback) {
            this.sourceName = sourceName;
            this.targetName = targetName;
            this.forwardCallback = forwardCallback;
        }

        /**
         * Forward event.
         */
        public void forward(Object event) {
            forwardCallback.accept(event);
        }

        /**
         * Get source name.
         */
        public String getSourceName() {
            return sourceName;
        }

        /**
         * Get target name.
         */
        public String getTargetName() {
            return targetName;
        }
    }

    /**
     * Register event forwarder.
     */
    public void registerEventForwarder(EventForwarder forwarder) {
        eventForwarders.add(forwarder);
        if (ECSConfig.enableDetailedLogging()) {
            System.out.println("[Interoperability] Registered event forwarder: " 
                + forwarder.getSourceName() + " -> " + forwarder.getTargetName());
        }
    }

    /**
     * Forward event to all registered forwarders.
     */
    public void forwardEvent(Object event) {
        eventForwarders.forEach(forwarder -> forwarder.forward(event));
    }

    // ════════════════════════════════════════════════════════════════════════
    // MIGRATION UTILITIES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Migration assistant for gradual system transition.
     */
    public static class MigrationAssistant {
        private final World astralisWorld;
        private final EntityMapper entityMapper;
        private final Map<String, ComponentTranslator<?>> translators = new HashMap<>();

        public MigrationAssistant(World astralisWorld, EntityMapper entityMapper) {
            this.astralisWorld = astralisWorld;
            this.entityMapper = entityMapper;
        }

        /**
         * Register component translator for migration.
         */
        public <T> void registerTranslator(String componentName, ComponentTranslator<T> translator) {
            translators.put(componentName, translator);
        }

        /**
         * Migrate entity from external system to Astralis.
         */
        public Entity migrateEntity(Object externalEntity, List<Object> externalComponents) {
            Entity astralisEntity = astralisWorld.createEntity();
            entityMapper.mapEntity(externalEntity, astralisEntity);

            for (Object externalComponent : externalComponents) {
                // Find appropriate translator
                // This is simplified - real implementation would detect component type
                String componentName = externalComponent.getClass().getSimpleName();
                ComponentTranslator<?> translator = translators.get(componentName);
                
                if (translator != null) {
                    Object astralisComponent = translator.translateToAstralis(externalComponent);
                    // Add component to entity
                    // (Actual component addition would use World API)
                }
            }

            return astralisEntity;
        }

        /**
         * Migrate entity from Astralis to external system.
         */
        public Object exportEntity(Entity astralisEntity) {
            // Placeholder for entity export
            // Real implementation would gather components and translate
            return entityMapper.getExternalEntity(astralisEntity);
        }
    }

    /**
     * Create migration assistant.
     */
    public MigrationAssistant createMigrationAssistant(World world, String systemName) {
        EntityMapper mapper = getEntityMapper(systemName);
        MigrationAssistant assistant = new MigrationAssistant(world, mapper);
        
        // Register all known translators
        componentTranslators.forEach(assistant::registerTranslator);
        
        return assistant;
    }

    // ════════════════════════════════════════════════════════════════════════
    // DIAGNOSTICS AND MONITORING
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Get interoperability statistics.
     */
    public InteroperabilityStats getStats() {
        return new InteroperabilityStats(
            entityMappers.size(),
            componentTranslators.size(),
            systemBridges.size(),
            eventForwarders.size()
        );
    }

    /**
     * Interoperability statistics record.
     */
    public record InteroperabilityStats(
        int entityMapperCount,
        int componentTranslatorCount,
        int systemBridgeCount,
        int eventForwarderCount
    ) {
        @Override
        public String toString() {
            return String.format("""
                Interoperability Statistics:
                ════════════════════════════════════════════
                Entity Mappers:        %d
                Component Translators: %d
                System Bridges:        %d
                Event Forwarders:      %d
                ════════════════════════════════════════════
                """,
                entityMapperCount,
                componentTranslatorCount,
                systemBridgeCount,
                eventForwarderCount
            );
        }
    }

    /**
     * Clear all interoperability registrations.
     */
    public void clearAll() {
        entityMappers.clear();
        componentTranslators.clear();
        systemBridges.clear();
        eventForwarders.clear();
        
        if (ECSConfig.enableDetailedLogging()) {
            System.out.println("[Interoperability] All registrations cleared");
        }
    }

    /**
     * Validate interoperability configuration.
     */
    public List<String> validateConfiguration() {
        List<String> issues = new ArrayList<>();

        // Check for orphaned translators
        Set<String> mappedSystems = entityMappers.keySet();
        Set<String> bridgedSystems = systemBridges.keySet();
        
        for (String system : bridgedSystems) {
            if (!mappedSystems.contains(system)) {
                issues.add("System bridge registered without entity mapper: " + system);
            }
        }

        // Validate component translators
        for (Map.Entry<String, ComponentTranslator<?>> entry : componentTranslators.entrySet()) {
            ComponentTranslator<?> translator = entry.getValue();
            if (translator.getAstralisComponentClass() == null) {
                issues.add("Component translator missing Astralis class: " + entry.getKey());
            }
        }

        return issues;
    }

    /**
     * Generate interoperability report.
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("═══════════════════════════════════════════════════════════════\n");
        report.append("ASTRALIS ECS INTEROPERABILITY REPORT\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");
        
        report.append(getStats().toString()).append("\n");
        
        report.append("REGISTERED SYSTEMS:\n");
        report.append("───────────────────────────────────────────────────────────────\n");
        entityMappers.keySet().forEach(system -> 
            report.append("  • ").append(system).append("\n")
        );
        
        report.append("\nCOMPONENT TRANSLATORS:\n");
        report.append("───────────────────────────────────────────────────────────────\n");
        componentTranslators.forEach((name, translator) ->
            report.append("  • ").append(name)
                  .append(" (").append(translator.getAstralisComponentClass().getSimpleName()).append(")\n")
        );
        
        report.append("\nVALIDATION ISSUES:\n");
        report.append("───────────────────────────────────────────────────────────────\n");
        List<String> issues = validateConfiguration();
        if (issues.isEmpty()) {
            report.append("  ✓ No issues detected\n");
        } else {
            issues.forEach(issue -> report.append("  ✗ ").append(issue).append("\n"));
        }
        
        report.append("\n═══════════════════════════════════════════════════════════════\n");
        
        return report.toString();
    }
}
