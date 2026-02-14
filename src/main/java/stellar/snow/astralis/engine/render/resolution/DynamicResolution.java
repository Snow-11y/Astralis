package stellar.snow.astralis.engine.render.resolution;
    private float currentScale = 1.0f;
    private final float targetFrameTime = 16.666f; // 60 FPS
    public void update(float actualFrameTime) {
        if (actualFrameTime > targetFrameTime) {
            currentScale = Math.max(0.5f, currentScale - 0.05f);
        } else {
            currentScale = Math.min(1.0f, currentScale + 0.05f);
        }
    }
    public float getScale() { return currentScale; }
}
