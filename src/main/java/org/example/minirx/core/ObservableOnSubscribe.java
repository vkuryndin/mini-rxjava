package org.example.minirx.core;

/**
 * Describes what an {@link Observable} should do after subscription.
 *
 * <p>The implementation receives an {@link Emitter} and uses it to send
 * items, report an error, or finish the stream.
 *
 * @param <T> type of emitted items
 */
@FunctionalInterface
public interface ObservableOnSubscribe<T> {

    /**
     * Starts the source logic for a new subscription.
     *
     * @param emitter emitter used to send signals downstream
     */
    void subscribe(Emitter<T> emitter);
}
