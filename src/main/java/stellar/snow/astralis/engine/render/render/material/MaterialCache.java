package stellar.snow.astralis.engine.render.material;
import java.util.concurrent.*;
public final class MaterialCache {
    private final ConcurrentHashMap<String, Object> materials = new ConcurrentHashMap<>();
    public Object get(String name) { return materials.get(name); }
    public void put(String name, Object material) { materials.put(name, material); }
    public void clear() { materials.clear(); }
}
