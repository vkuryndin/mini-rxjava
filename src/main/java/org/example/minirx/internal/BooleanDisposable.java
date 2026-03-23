package org.example.minirx.internal;

import java.util.concurrent.atomic.AtomicBoolean;
import org.example.minirx.core.Disposable;

/**
 * Basic {@link Disposable} implementation backed by {@link AtomicBoolean}.
 *
 * <p>Used as a small reusable base for components that need to store disposal state.
 */
public class BooleanDisposable implements Disposable {

  /** Current disposal state. */
  private final AtomicBoolean disposed = new AtomicBoolean(false);

  /**
   * Marks this instance as disposed.
   *
   * <p>This operation is idempotent.
   */
  @Override
  public void dispose() {
    disposed.set(true);
  }

  /**
   * Returns the current disposal state.
   *
   * @return {@code true} if already disposed
   */
  @Override
  public boolean isDisposed() {
    return disposed.get();
  }
}
