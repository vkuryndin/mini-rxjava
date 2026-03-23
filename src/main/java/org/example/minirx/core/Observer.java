package org.example.minirx.core;

/**
 * Receives signals from an {@link Observable}.
 *
 * <p>An observer handles three events: the next item, an error, or normal completion. After {@code
 * onError(...)} or {@code onComplete()} is called, no further signals should be delivered.
 *
 * @param <T> type of items passed to the observer
 */
public interface Observer<T> {

  /**
   * Handles the next item from the stream.
   *
   * @param item emitted item
   */
  void onNext(T item);

  /**
   * Handles a terminal error.
   *
   * @param throwable stream error
   */
  void onError(Throwable throwable);

  /** Handles normal stream completion. */
  void onComplete();
}
