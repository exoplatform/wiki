package org.exoplatform.wiki;

public class WikiException extends Exception {
  public WikiException() {
  }

  public WikiException(String message) {
    super(message);
  }

  public WikiException(String message, Throwable cause) {
    super(message, cause);
  }

  public WikiException(Throwable cause) {
    super(cause);
  }

  public WikiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
