package stellar.snow.astralis.engine.render.texture;
import java.util.concurrent.*;
public final class TextureStreaming {
    private final BlockingQueue<Long> streamQueue = new LinkedBlockingQueue<>();
    private Thread streamThread;
    public void requestLoad(long textureId, int mipLevel) {
        streamQueue.offer(textureId);
    }
    private void streamWorker() {
        while (!Thread.interrupted()) {
            try {
                Long textureId = streamQueue.take();
                // Load texture data from disk
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
