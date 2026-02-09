package org.spongepowered.include.com.google.common.io;

import java.io.IOException;
import java.io.Writer;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.io.Closer;

public abstract class CharSink {
    protected CharSink() {
    }

    public abstract Writer openStream() throws IOException;

    public void write(CharSequence charSequence) throws IOException {
        Preconditions.checkNotNull(charSequence);
        try (Closer closer = Closer.create();){
            Writer out = closer.register(this.openStream());
            out.append(charSequence);
            out.flush();
        }
    }
}

