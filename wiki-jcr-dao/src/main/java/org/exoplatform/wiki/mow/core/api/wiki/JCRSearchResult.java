package org.exoplatform.wiki.mow.core.api.wiki;

import org.exoplatform.wiki.service.search.SearchResult;

/**
 * SearchResult for JCR
 */
public class JCRSearchResult extends SearchResult {

  protected String path;

  public void setPath(String path) {
    this.path = path;
  }
  public String getPath() {
    return path;
  }

}
