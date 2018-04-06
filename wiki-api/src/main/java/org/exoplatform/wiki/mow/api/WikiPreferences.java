package org.exoplatform.wiki.mow.api;

/**
 * @author Thomas Delhoménie
 */
public class WikiPreferences {
  private WikiPreferencesSyntax wikiPreferencesSyntax;

  private String path;

  public WikiPreferencesSyntax getWikiPreferencesSyntax() {
    return wikiPreferencesSyntax;
  }

  public void setWikiPreferencesSyntax(WikiPreferencesSyntax wikiPreferencesSyntax) {
    this.wikiPreferencesSyntax = wikiPreferencesSyntax;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
