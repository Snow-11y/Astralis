package stellar.snow.astralis.engine.render.memory;
    private long stagingBuffer;
    private long stagingMemory;
    private long mappedPtr;
    public void upload(byte[] data, long dstBuffer, long offset) {
        // Copy to staging, then GPU
    }
    public void flush() {
        // Submit pending uploads
    }
}
