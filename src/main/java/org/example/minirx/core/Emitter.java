package org.example.minirx.core;

/**
 * Pushes signals from a source to an {@link Observer}.
 *
 * <p>An emitter is available inside {@code Observable.create(...)}. It can send items, report an
 * error, complete the stream, and expose the current subscription state through {@link Disposable}.
 *
 * @param <T> type of emitted items
 */
public interface Emitter<T> extends Disposable {

  /**
   * Sends the next item downstream.
   *
   * @param item item to emit
   */
  void onNext(T item);

  /**
   * Terminates the stream with an error.
   *
   * @param throwable error to deliver
   */
  void onError(Throwable throwable);

  /** Completes the stream normally. */
  void onComplete();
}
