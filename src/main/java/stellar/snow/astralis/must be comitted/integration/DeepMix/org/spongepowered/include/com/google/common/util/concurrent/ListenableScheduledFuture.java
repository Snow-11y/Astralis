package org.spongepowered.include.com.google.common.util.concurrent;

import java.util.concurrent.ScheduledFuture;
import org.spongepowered.include.com.google.common.util.concurrent.ListenableFuture;

public interface ListenableScheduledFuture<V>
extends ScheduledFuture<V>,
ListenableFuture<V> {
}

