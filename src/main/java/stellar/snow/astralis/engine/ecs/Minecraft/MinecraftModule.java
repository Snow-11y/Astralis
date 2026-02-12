package ecs.Minecraft;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark the Minecraft integration module as optional.
 * 
 * This allows the core ECS system to function without the Minecraft-specific
 * implementations. If Minecraft APIs are not available, the annotated classes
 * will gracefully degrade or be skipped.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface MinecraftModule {
    
    /**
     * Module version
     */
    String version() default "1.0.0";
    
    /**
     * Module description
     */
    String description() default "Minecraft ECS integration";
    
    /**
     * Whether this module is required for the ECS to function
     * Set to false for optional modules
     */
    boolean required() default false;
    
    /**
     * Dependencies on other modules
     */
    String[] dependencies() default {};
    
    /**
     * Minecraft version this module targets
     */
    String minecraftVersion() default "1.20+";
}
