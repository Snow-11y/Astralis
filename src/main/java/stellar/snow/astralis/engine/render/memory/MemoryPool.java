package stellar.snow.astralis.engine.render.memory;
import java.util.concurrent.*;
    private final long blockSize;
    private final ConcurrentLinkedQueue<Long> freeBlocks = new ConcurrentLinkedQueue<>();
    public MemoryPool(long blockSize, int initialBlocks) {
        this.blockSize = blockSize;
        for (int i = 0; i < initialBlocks; i++) {
            freeBlocks.offer(allocateBlock());
        }
    }
    private long allocateBlock() {
        // Allocate from GPU
        return 1L;
    }
    public long acquire() {
        Long block = freeBlocks.poll();
        return block != null ? block : allocateBlock();
    }
    public void release(long block) {
        freeBlocks.offer(block);
    }
}
