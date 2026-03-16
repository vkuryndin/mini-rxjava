package org.example.minirx.core;

/**
 * Represents a disposable resource, usually a subscription.
 *
 * <p>A disposable allows stopping the stream and releasing related work.
 * After disposal, the stream should not deliver new items to the observer.
 */
public interface Disposable {

    /**
     * Cancels the current subscription or resource.
     *
     * <p>This method should be safe to call more than once.
     */
    void dispose();

    /**
     * Returns whether this disposable has already been disposed.
     *
     * @return {@code true} if disposed, otherwise {@code false}
     */
    boolean isDisposed();
}