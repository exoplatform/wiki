package org.exoplatform.wiki.jpa.migration.mock;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Synchronous executor to run the task in the same thread than the caller.
 * Necessary for unit tests.
 */
public class SynchronousExecutorService extends AbstractExecutorService {
  private boolean shutdown;

  @Override
  public void shutdown() {
    shutdown = true;
  }

  @Override
  public List<Runnable> shutdownNow() {
    shutdown = true;
    return Collections.emptyList();
  }

  @Override
  public boolean isShutdown() {
    shutdown = true;
    return shutdown;
  }

  @Override
  public boolean isTerminated() {
    return shutdown;
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return true;
  }

  @Override
  public void execute(Runnable command) {
    command.run();
  }
}
