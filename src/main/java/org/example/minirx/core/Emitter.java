package org.example.minirx.core;

/**
 * Sends events from an {@link Observable} source to an {@link Observer}.
 *
 * <p>An emitter is used inside {@code Observable.create(...)} to push items,
 * signal errors, or complete the stream.
 *
 * <p>This interface also extends {@link Disposable} so the source can check
 * whether the subscription is still active.
 *
 * @param <T> the type of emitted items
 */
public interface Emitter<T> extends Disposable {

    /**
     * Emits the next item to the observer.
     *
     * @param item the item to emit
     */
    void onNext(T item);

    /**
     * Signals an error and terminates the stream.
     *
     * @param throwable the error to signal
     */
    void onError(Throwable throwable);

    /**
     * Signals successful completion and terminates the stream.
     */
    void onComplete();
}