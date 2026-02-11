package org.spongepowered.asm.launch;

import java.util.HashMap;
import java.util.Map;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;
import org.spongepowered.asm.service.MixinService;

public final class GlobalProperties {
    private static IGlobalPropertyService service;

    private GlobalProperties() {
    }

    private static IGlobalPropertyService getService() {
        if (service == null) {
            service = MixinService.getGlobalPropertyService();
        }
        return service;
    }

    public static <T> T get(Keys key) {
        IGlobalPropertyService service = GlobalProperties.getService();
        return service.getProperty(key.resolve(service));
    }

    public static <T> T get(String key) {
        return GlobalProperties.get(Keys.of(key));
    }

    public static void put(Keys key, Object value) {
        IGlobalPropertyService service = GlobalProperties.getService();
        service.setProperty(key.resolve(service), value);
    }

    public static void put(String key, Object value) {
        GlobalProperties.put(Keys.of(key), value);
    }

    public static <T> T get(Keys key, T defaultValue) {
        IGlobalPropertyService service = GlobalProperties.getService();
        return service.getProperty(key.resolve(service), defaultValue);
    }

    public static <T> T get(String key, T defaultValue) {
        return GlobalProperties.get(Keys.of(key), defaultValue);
    }

    public static String getString(Keys key, String defaultValue) {
        IGlobalPropertyService service = GlobalProperties.getService();
        return service.getPropertyString(key.resolve(service), defaultValue);
    }

    public static String getString(String key, String defaultValue) {
        return GlobalProperties.getString(Keys.of(key), defaultValue);
    }

    public static final class Keys {
        public static final Keys INIT = Keys.of("mixin.initialised");
        public static final Keys AGENTS = Keys.of("mixin.agents");
        public static final Keys CONFIGS = Keys.of("mixin.configs");
        public static final Keys PLATFORM_MANAGER = Keys.of("mixin.platform");
        public static final Keys FML_LOAD_CORE_MOD = Keys.of("mixin.launch.fml.loadcoremodmethod");
        public static final Keys FML_GET_REPARSEABLE_COREMODS = Keys.of("mixin.launch.fml.reparseablecoremodsmethod");
        public static final Keys FML_CORE_MOD_MANAGER = Keys.of("mixin.launch.fml.coremodmanagerclass");
        public static final Keys FML_GET_IGNORED_MODS = Keys.of("mixin.launch.fml.ignoredmodsmethod");
        public static final Keys CLEANROOM_DISABLE_MIXIN_CONFIGS = Keys.of("mixin.cleanroom.disablemixinconfigs");
        private static Map<String, Keys> keys;
        private final String name;
        private IPropertyKey key;

        private Keys(String name) {
            this.name = name;
        }

        IPropertyKey resolve(IGlobalPropertyService service) {
            if (this.key != null) {
                return this.key;
            }
            if (service == null) {
                return null;
            }
            this.key = service.resolveKey(this.name);
            return this.key;
        }

        public String toString() {
            return this.name;
        }

        public static Keys of(String name) {
            Keys key;
            if (keys == null) {
                keys = new HashMap<String, Keys>();
            }
            if ((key = keys.get(name)) == null) {
                key = new Keys(name);
                keys.put(name, key);
            }
            return key;
        }
    }
}

