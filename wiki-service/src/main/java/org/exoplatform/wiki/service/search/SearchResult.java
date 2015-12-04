package org.exoplatform.wiki.service.search;

import org.exoplatform.services.deployment.Utils;

import java.util.Calendar;

public class SearchResult {
  protected String wikiType;
  protected String wikiOwner;
  protected String pageName;
  protected String attachmentName;
  protected String excerpt;
  protected String title;
  protected SearchResultType type;
  protected String url;
  protected long score;
  protected Calendar updatedDate;  
  protected Calendar createdDate;

  public SearchResult() {}
  
  public SearchResult(String wikiType, String wikiOwner, String pageName, String attachmentName, String excerpt,
                      String title, SearchResultType type, Calendar updatedDate, Calendar createdDate) {
    this.wikiType = wikiType;
    this.wikiOwner = wikiOwner;
    this.pageName = pageName;
    this.attachmentName = attachmentName;
    this.excerpt = excerpt;
    this.title = title;
    this.type = type;
    this.updatedDate = updatedDate;
    this.createdDate = createdDate;
  }

  public String getWikiType() {
    return wikiType;
  }

  public void setWikiType(String wikiType) {
    this.wikiType = wikiType;
  }

  public String getWikiOwner() {
    return wikiOwner;
  }

  public void setPageName(String pageName) {
    this.pageName = pageName;
  }

  public String getPageName() {
    return pageName;
  }

  public void setWikiOwner(String wikiOwner) {
    this.wikiOwner = wikiOwner;
  }

  public String getAttachmentName() {
    return attachmentName;
  }

  public void setAttachmentName(String attachmentName) {
    this.attachmentName = attachmentName;
  }

  public void setTitle(String title) {
    this.title = title;
  }
  public String getTitle() {
    return title;
  }
  
  public void setExcerpt(String text) {
    this.excerpt = text;
  }

  public String getExcerpt() {
    return Utils.sanitize(excerpt);
  }

  public void setType(SearchResultType type) {
    this.type = type;
  }

  public SearchResultType getType() {
    return type;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }
  
  public String getUrl() {
    return url;
  }
  
  public void setScore(long score) {
    this.score = score;
  }
  
  public long getScore() {
    return score;
  }

  public Calendar getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Calendar updatedDate) {
    this.updatedDate = updatedDate;
  }

  /**
   * @return the createdDate
   */
  public Calendar getCreatedDate() {
    return createdDate;
  }

  /**
   * @param createdDate the createdDate to set
   */
  public void setCreatedDate(Calendar createdDate) {
    this.createdDate = createdDate;
  }
}
