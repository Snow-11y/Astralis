package stellar.snow.astralis.engine.render.material;
public final class PBRMaterialBuilder {
    private long albedoMap, normalMap, metallicMap, roughnessMap, aoMap;
    private float metallicFactor = 0.0f, roughnessFactor = 0.5f;
    public PBRMaterialBuilder albedo(long texture) { albedoMap = texture; return this; }
    public PBRMaterialBuilder normal(long texture) { normalMap = texture; return this; }
    public PBRMaterialBuilder metallic(float value) { metallicFactor = value; return this; }
    public Object build() {
        // Create material
        return new Object();
    }
}
