package stellar.snow.astralis.engine.render.integration;
import java.util.*;
public final class ModLoader {
    private final List<Object> loadedMods = new ArrayList<>();
    public void loadMod(String modPath) {
        // Load mod from JAR/directory
    }
    public List<Object> getLoadedMods() {
        return Collections.unmodifiableList(loadedMods);
    }
}
