package org.exoplatform.wiki.mow.api;

/**
 * @author Thomas Delhoménie
 */
public class WikiPreferencesSyntax {
  private String defaultSyntax;

  private boolean allowMultipleSyntaxes;

  public String getDefaultSyntax() {
    return defaultSyntax;
  }

  public void setDefaultSyntax(String defaultSyntax) {
    this.defaultSyntax = defaultSyntax;
  }

  public boolean isAllowMultipleSyntaxes() {
    return allowMultipleSyntaxes;
  }

  public void setAllowMultipleSyntaxes(boolean allowMultipleSyntaxes) {
    this.allowMultipleSyntaxes = allowMultipleSyntaxes;
  }
}
