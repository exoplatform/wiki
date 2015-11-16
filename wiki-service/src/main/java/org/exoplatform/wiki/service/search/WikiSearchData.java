package org.exoplatform.wiki.service.search;

public class WikiSearchData extends SearchData {

  public WikiSearchData(String title, String content, String wikiType, String wikiOwner, String pageId) {
    super(title, content, wikiType, wikiOwner, pageId);
  }

  public WikiSearchData(String wikiType, String wikiOwner, String pageId) {
    this(null, null, wikiType, wikiOwner, pageId);
  }

  public WikiSearchData(String title, String content, String wikiType, String wikiOwner) {
    this(title, content, wikiType, wikiOwner, null);
  }

}
