package stellar.snow.astralis.engine.render.texture;
public final class TextureCompression {
    public enum Format { BC1, BC3, BC7, ASTC }
    public static byte[] compress(byte[] rgba, int width, int height, Format format) {
        // Compress using selected format
        return new byte[width * height];
    }
}
