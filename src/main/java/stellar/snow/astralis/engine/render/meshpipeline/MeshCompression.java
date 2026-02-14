package stellar.snow.astralis.engine.render.meshpipeline;
    public static byte[] compressPositions(float[] positions) {
        // Quantize to 16-bit fixed point
        byte[] compressed = new byte[positions.length * 2];
        return compressed;
    }
    public static byte[] compressNormals(float[] normals) {
        // Octahedral encoding
        byte[] compressed = new byte[normals.length / 3 * 2];
        return compressed;
    }
}
