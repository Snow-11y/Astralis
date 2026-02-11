package org.spongepowered.include.com.google.common.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import org.spongepowered.include.com.google.common.util.concurrent.ListenableFuture;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

@CanIgnoreReturnValue
public interface ListeningExecutorService
extends ExecutorService {
    public <T> ListenableFuture<T> submit(Callable<T> var1);

    public ListenableFuture<?> submit(Runnable var1);

    public <T> ListenableFuture<T> submit(Runnable var1, T var2);
}

