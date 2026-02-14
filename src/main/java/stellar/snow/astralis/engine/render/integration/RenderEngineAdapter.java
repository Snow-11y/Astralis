package stellar.snow.astralis.engine.render.integration;
public final class RenderEngineAdapter {
    public interface GraphicsAPI {
        void initialize();
        long createTexture(int width, int height, int format);
        void destroyTexture(long texture);
    }
    private GraphicsAPI api;
    public void setAPI(GraphicsAPI api) { this.api = api; }
    public void initialize() { api.initialize(); }
}
