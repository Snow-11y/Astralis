package stellar.snow.astralis.engine.render.raytracing;
public final class RTShaderTable {
    private long shaderBindingTable;
    public void addRayGenShader(long shader) {}
    public void addMissShader(long shader) {}
    public void addHitGroup(long closestHit, long anyHit, long intersection) {}
    public long build() { return shaderBindingTable; }
}
