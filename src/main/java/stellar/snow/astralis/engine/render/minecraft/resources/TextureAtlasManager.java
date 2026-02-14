package stellar.snow.astralis.engine.render.minecraft.resources;
import java.util.*;
/**
 * Manages Minecraft's texture atlases (blocks, items, etc.)
 */
    
    private static class AtlasEntry {
        String name;
        int x, y, width, height;
        long textureId;
    }
    
    private final Map<String, AtlasEntry> atlasEntries = new HashMap<>();
    private long blockAtlasTexture;
    private long itemAtlasTexture;
    
    public void buildAtlas(List<String> textureNames) {
        // Pack textures into atlas
        int currentX = 0, currentY = 0, rowHeight = 0;
        
        for (String name : textureNames) {
            AtlasEntry entry = new AtlasEntry();
            entry.name = name;
            entry.x = currentX;
            entry.y = currentY;
            entry.width = 16;  // Minecraft default
            entry.height = 16;
            
            atlasEntries.put(name, entry);
            
            currentX += 16;
            if (currentX >= 1024) {
                currentX = 0;
                currentY += 16;
            }
        }
    }
    
    public float[] getUVCoords(String textureName) {
        AtlasEntry entry = atlasEntries.get(textureName);
        if (entry == null) return new float[]{0, 0, 1, 1};
        
        return new float[]{
            entry.x / 1024.0f,
            entry.y / 1024.0f,
            (entry.x + entry.width) / 1024.0f,
            (entry.y + entry.height) / 1024.0f
        };
    }
}
