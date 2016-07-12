package org.exoplatform.wiki.service.search;

import java.util.Calendar;

import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class SearchResult {
  private static Log         log = ExoLogger.getLogger(SearchResult.class);

  protected String           wikiType;

  protected String           wikiOwner;

  protected String           pageName;

  protected String           attachmentName;

  protected String           excerpt;

  protected String           title;

  protected SearchResultType type;

  protected String           url;

  protected long             score;

  protected Calendar         updatedDate;

  protected Calendar         createdDate;

  public SearchResult() {
  }

  public SearchResult(String wikiType,
                      String wikiOwner,
                      String pageName,
                      String attachmentName,
                      String excerpt,
                      String title,
                      SearchResultType type,
                      Calendar updatedDate,
                      Calendar createdDate) {
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

  public void setWikiOwner(String wikiOwner) {
    this.wikiOwner = wikiOwner;
  }

  public String getPageName() {
    return pageName;
  }

  public void setPageName(String pageName) {
    this.pageName = pageName;
  }

  public String getAttachmentName() {
    return attachmentName;
  }

  public void setAttachmentName(String attachmentName) {
    this.attachmentName = attachmentName;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getExcerpt() {
    try {
      return HTMLSanitizer.sanitize(excerpt);
    } catch (Exception e) {

      log.error("Fail to sanitize input [" + excerpt + "], " + e.getMessage(), e);

    }
    return "";
  }

  public void setExcerpt(String text) {
    this.excerpt = text;
  }

  public SearchResultType getType() {
    return type;
  }

  public void setType(SearchResultType type) {
    this.type = type;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public long getScore() {
    return score;
  }

  public void setScore(long score) {
    this.score = score;
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
