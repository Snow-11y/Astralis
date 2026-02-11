package org.spongepowered.include.com.google.common.io;

import java.io.IOException;
import java.nio.CharBuffer;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.io.LineProcessor;
import org.spongepowered.include.com.google.common.io.LineReader;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public final class CharStreams {
    static CharBuffer createBuffer() {
        return CharBuffer.allocate(2048);
    }

    @CanIgnoreReturnValue
    public static <T> T readLines(Readable readable, LineProcessor<T> processor) throws IOException {
        String line;
        Preconditions.checkNotNull(readable);
        Preconditions.checkNotNull(processor);
        LineReader lineReader = new LineReader(readable);
        while ((line = lineReader.readLine()) != null && processor.processLine(line)) {
        }
        return processor.getResult();
    }
}

