package org.example.minirx.core;

/**
 * Represents a resource that can be cancelled or released.
 *
 * <p>In this project it is mainly used as a subscription handle. Once disposed, the stream should
 * stop sending new items.
 */
public interface Disposable {

  /**
   * Disposes the current resource.
   *
   * <p>Repeated calls are allowed.
   */
  void dispose();

  /**
   * Checks whether this resource has already been disposed.
   *
   * @return {@code true} if disposal has already happened
   */
  boolean isDisposed();
}
