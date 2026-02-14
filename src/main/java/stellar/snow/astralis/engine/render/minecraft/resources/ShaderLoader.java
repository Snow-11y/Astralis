package stellar.snow.astralis.engine.render.minecraft.resources;
import java.nio.file.*;
import java.util.*;
/**
 * Loads shaders from Minecraft resource packs
 */
    
    private final Map<String, Path> shaderPaths = new HashMap<>();
    
    public void loadShadersFromResourcePack(Path resourcePackPath) {
        // Scan for .vsh, .fsh, .glsl files
        try {
            Files.walk(resourcePackPath)
                .filter(p -> isShaderFile(p))
                .forEach(p -> registerShader(p));
        } catch (Exception e) {
            System.err.println("Error loading shaders: " + e.getMessage());
        }
    }
    
    private boolean isShaderFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".vsh") || name.endsWith(".fsh") || 
               name.endsWith(".glsl") || name.endsWith(".comp");
    }
    
    private void registerShader(Path path) {
        String name = path.getFileName().toString();
        shaderPaths.put(name, path);
    }
    
    public Path getShaderPath(String name) {
        return shaderPaths.get(name);
    }
}
