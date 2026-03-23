package org.example.minirx.internal;

import java.util.Objects;
import org.example.minirx.core.Emitter;
import org.example.minirx.core.Observer;

/**
 * Default {@link Emitter} used by {@code Observable.create(...)}.
 *
 * <p>The emitter forwards signals to the target observer and tracks whether the stream has already
 * reached a terminal state or has been disposed.
 *
 * @param <T> type of emitted items
 */
public final class CreateEmitter<T> extends BooleanDisposable implements Emitter<T> {

  /** Observer that receives emitted signals. */
  private final Observer<? super T> observer;

  /** Shows whether a terminal signal has already been sent. */
  private boolean terminated;

  /**
   * Creates an emitter for the given observer.
   *
   * @param observer target observer
   */
  public CreateEmitter(Observer<? super T> observer) {
    this.observer = Objects.requireNonNull(observer, "observer must not be null");
  }

  /**
   * Sends the next item if the subscription is still active.
   *
   * @param item item to emit
   */
  @Override
  public void onNext(T item) {
    if (isDisposed() || terminated) {
      return;
    }

    observer.onNext(item);
  }

  /**
   * Sends an error and closes the stream.
   *
   * <p>If {@code throwable} is {@code null}, a {@link NullPointerException} is sent instead.
   *
   * @param throwable error to deliver
   */
  @Override
  public void onError(Throwable throwable) {
    if (isDisposed() || terminated) {
      return;
    }

    terminated = true;
    observer.onError(
        throwable != null ? throwable : new NullPointerException("throwable must not be null"));
    dispose();
  }

  /** Completes the stream if it is still active. */
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
