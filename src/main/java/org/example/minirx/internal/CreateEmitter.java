package org.example.minirx.internal;

import org.example.minirx.core.Emitter;
import org.example.minirx.core.Observer;

import java.util.Objects;

/**
 * Basic {@link Emitter} implementation used by {@code Observable.create(...)}.
 *
 * <p>This emitter delivers signals to the target observer and keeps track
 * of the subscription state. It prevents event delivery after:
 * <ul>
 *     <li>the stream has completed,</li>
 *     <li>the stream has failed with an error,</li>
 *     <li>the subscription has been disposed.</li>
 * </ul>
 *
 * @param <T> the type of emitted items
 */
public final class CreateEmitter<T> extends BooleanDisposable implements Emitter<T> {

    /**
     * The target observer that receives events.
     */
    private final Observer<? super T> observer;

    /**
     * Shows whether the stream has already reached a terminal state.
     *
     * <p>A terminal state means either {@code onError(...)} or
     * {@code onComplete()} has already been called.
     */
    private boolean terminated;

    /**
     * Creates a new emitter for the given observer.
     *
     * @param observer the observer that will receive events
     */
    public CreateEmitter(Observer<? super T> observer) {
        this.observer = Objects.requireNonNull(observer, "observer must not be null");
    }

    /**
     * Emits the next item to the observer if the stream is still active.
     *
     * <p>If the stream has already been disposed or terminated,
     * the item is ignored.
     *
     * @param item the item to emit
     */
    @Override
    public void onNext(T item) {
        if (isDisposed() || terminated) {
            return;
        }

        observer.onNext(item);
    }

    /**
     * Signals an error to the observer and terminates the stream.
     *
     * <p>If the stream has already been disposed or terminated,
     * the error is ignored.
     *
     * <p>If the provided throwable is {@code null}, it is replaced with
     * a {@link NullPointerException}.
     *
     * @param throwable the error to signal
     */
    @Override
    public void onError(Throwable throwable) {
        if (isDisposed() || terminated) {
            return;
        }

        terminated = true;
        observer.onError(throwable != null
                ? throwable
                : new NullPointerException("throwable must not be null"));
        dispose();
    }

    /**
     * Signals successful completion and terminates the stream.
     *
     * <p>If the stream has already been disposed or terminated,
     * the completion signal is ignored.
     */
    @Override
    public void onComplete() {
        if (isDisposed() || terminated) {
            return;
        }

        terminated = true;
        observer.onComplete();
        dispose();
    }
}