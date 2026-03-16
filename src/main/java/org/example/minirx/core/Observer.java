package org.example.minirx.core;

/**
 * Receives events from an {@link Observable}.
 *
 * <p>An observer is the final consumer in the reactive chain.
 * It receives three kinds of signals:
 * <ul>
 *     <li>{@code onNext(T item)} - a new item was emitted</li>
 *     <li>{@code onError(Throwable throwable)} - an error happened</li>
 *     <li>{@code onComplete()} - the stream finished successfully</li>
 * </ul>
 *
 * <p>After {@code onError(...)} or {@code onComplete()} is called,
 * no more events should be delivered to the observer.
 *
 * @param <T> the type of items received by this observer
 */
public interface Observer<T> {

    /**
     * Receives the next item from the stream.
     *
     * @param item the emitted item
     */
    void onNext(T item);

    /**
     * Receives an error from the stream.
     *
     * <p>After this method is called, the stream is considered terminated.
     *
     * @param throwable the error that happened during stream execution
     */
    void onError(Throwable throwable);

    /**
     * Receives a completion signal from the stream.
     *
     * <p>After this method is called, the stream is considered terminated.
     */
    void onComplete();
}