package stellar.snow.astralis.engine.render.minecraft.common;
    private static AstralisRenderEngine INSTANCE;
    private boolean initialized = false;
    
    private AstralisRenderEngine() {
        System.out.println("[AstralisEngine] Initializing for Minecraft");
    }
    
    public static AstralisRenderEngine getInstance() {
        if (INSTANCE == null) INSTANCE = new AstralisRenderEngine();
        return INSTANCE;
    }
    
    public void initialize() {
        if (!initialized) {
            System.out.println("[AstralisEngine] Initialized successfully");
            initialized = true;
        }
    }
    
    public boolean isInitialized() {
        return initialized;
    }
}
