package stellar.snow.astralis.engine.render.quantization;
public final class PositionQuantizer {
    public static short[] quantizeToInt16(float[] positions, float min, float max) {
        short[] quantized = new short[positions.length];
        float scale = 65535.0f / (max - min);
        for (int i = 0; i < positions.length; i++) {
            quantized[i] = (short)((positions[i] - min) * scale);
        }
        return quantized;
    }
}
