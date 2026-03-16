package org.example.minirx.internal;

import org.example.minirx.core.Disposable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple {@link Disposable} implementation based on {@link AtomicBoolean}.
 *
 * <p>This class stores the disposal state and allows it to be checked safely.
 * It is useful as a base class for emitters and other subscription-related
 * components.
 *
 * <p>Once disposed, the state cannot become active again.
 */
public class BooleanDisposable implements Disposable {

    /**
     * Stores the current disposal state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Marks this disposable as disposed.
     *
     * <p>This method is idempotent, which means calling it multiple times
     * has the same effect as calling it once.
     */
    @Override
    public void dispose() {
        disposed.set(true);
    }

    /**
     * Returns whether this disposable has already been disposed.
     *
     * @return {@code true} if disposed, otherwise {@code false}
     */
    @Override
    public boolean isDisposed() {
        return disposed.get();
    }
}