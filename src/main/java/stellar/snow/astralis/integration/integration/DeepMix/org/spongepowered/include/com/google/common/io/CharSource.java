package org.spongepowered.include.com.google.common.io;

import java.io.IOException;
import java.io.Reader;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.io.CharStreams;
import org.spongepowered.include.com.google.common.io.Closer;
import org.spongepowered.include.com.google.common.io.LineProcessor;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public abstract class CharSource {
    protected CharSource() {
    }

    public abstract Reader openStream() throws IOException;

    @CanIgnoreReturnValue
    public <T> T readLines(LineProcessor<T> processor) throws IOException {
        Preconditions.checkNotNull(processor);
        try (Closer closer = Closer.create();){
            Reader reader = closer.register(this.openStream());
            T t = CharStreams.readLines(reader, processor);
            return t;
        }
    }
}

