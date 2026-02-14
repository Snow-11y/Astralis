package stellar.snow.astralis.engine.render.terrain;
    public static float[][] generatePerlin(int width, int height, float scale, int octaves) {
        float[][] heightmap = new float[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                heightmap[x][y] = perlinNoise(x * scale, y * scale, octaves);
            }
        }
        return heightmap;
    }
    private static float perlinNoise(float x, float y, int octaves) {
        float total = 0;
        float frequency = 1;
        float amplitude = 1;
        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency) * amplitude;
            frequency *= 2;
            amplitude *= 0.5f;
        }
        return total;
    }
    private static float noise(float x, float y) {
        return (float) Math.sin(x * 12.9898 + y * 78.233) * 43758.5453f % 1.0f;
    }
}
