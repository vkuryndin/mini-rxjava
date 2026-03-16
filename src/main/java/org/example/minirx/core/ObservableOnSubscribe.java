package org.example.minirx.core;

/**
 * Contains the subscription logic for an {@link Observable}.
 *
 * <p>This functional interface is used by {@link Observable#create(ObservableOnSubscribe)}
 * to describe what should happen when an observer subscribes.
 *
 * @param <T> the type of emitted items
 */
@FunctionalInterface
public interface ObservableOnSubscribe<T> {

    /**
     * Runs the source logic and emits events through the given emitter.
     *
     * @param emitter the emitter used to send events to the observer
     */
    void subscribe(Emitter<T> emitter);
}