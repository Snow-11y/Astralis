package stellar.snow.astralis.engine.render.decals;
import org.joml.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/**
 * DecalAtlas - Texture Atlas Management for Decals
 * 
 * Packs multiple decal textures into a single atlas for efficient rendering.
 * Features:
 * - Dynamic atlas allocation
 * - Rectangle packing algorithm
 * - UV coordinate mapping
 * - Runtime updates
 */
    
    private final int atlasWidth;
    private final int atlasHeight;
    private long atlasTexture;
    
    private static class AtlasEntry {
        int x, y, width, height;
        String name;
        
        Vector4f getUVBounds() {
            return new Vector4f(
                (float)x / 4096,
                (float)y / 4096,
                (float)(x + width) / 4096,
                (float)(y + height) / 4096
            );
        }
    }
    
    private final ConcurrentHashMap<String, AtlasEntry> entries = new ConcurrentHashMap<>();
    private final AtomicInteger nextY = new AtomicInteger(0);
    private int currentRowHeight = 0;
    private int currentX = 0;
    
    public DecalAtlas(int width, int height) {
        this.atlasWidth = width;
        this.atlasHeight = height;
        createAtlasTexture();
    }
    
    private void createAtlasTexture() {
        // Create 4096x4096 RGBA texture
    }
    
    public AtlasEntry allocate(String name, int width, int height) {
        if (currentX + width > atlasWidth) {
            currentX = 0;
            nextY.addAndGet(currentRowHeight);
            currentRowHeight = 0;
        }
        
        if (nextY.get() + height > atlasHeight) {
            throw new IllegalStateException("Atlas full");
        }
        
        AtlasEntry entry = new AtlasEntry();
        entry.x = currentX;
        entry.y = nextY.get();
        entry.width = width;
        entry.height = height;
        entry.name = name;
        
        entries.put(name, entry);
        
        currentX += width;
        currentRowHeight = Math.max(currentRowHeight, height);
        
        return entry;
    }
    
    public Vector4f getUVBounds(String name) {
        AtlasEntry entry = entries.get(name);
        return entry != null ? entry.getUVBounds() : new Vector4f(0, 0, 1, 1);
    }
    
    public long getAtlasTexture() {
        return atlasTexture;
    }
}
