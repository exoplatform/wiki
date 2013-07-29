package org.exoplatform.wiki.webui.bean;

import java.util.Date;

import org.exoplatform.wiki.utils.Utils;

public class DraftBean {
  public static final String ID = "id";
 
  public static final String PAGE_TITLE = "pageTitle";
      
  public static final String PLACE = "place";
  
  public static final String LAST_EDITION = "lastEdition";
  
  private String id;
  
  private String pageTitle;
  
  private String place;
  
  private Date lastEdition;
  
  public DraftBean(String id, String pageTitle, String place, Date lastEdition) {
    this.id = id;
    this.pageTitle = pageTitle;
    this.place = place;
    this.lastEdition = lastEdition;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPageTitle() {
    return pageTitle;
  }

  public void setPageTitle(String pageTitle) {
    this.pageTitle = pageTitle;
  }

  public String getPlace() {
    return place;
  }

  public void setPlace(String place) {
    this.place = place;
  }

  public String getLastEdition() {
    return getEditTimeInString(lastEdition);
  }
  
  public Date getLastEditionInDate() {
    return lastEdition;
  }

  public void setLastEdition(Date lastEdition) {
    this.lastEdition = lastEdition;
  }
  
  private String getEditTimeInString(Date date) {
    long asecond = 1000;
    long aminute = asecond * 60;
    long ahour = aminute * 60;
    long aday = ahour * 24;
    
    long timeDistance = System.currentTimeMillis() - date.getTime();
    long days = timeDistance / aday;
    if (days > 0) {
      return appendTime(Utils.getWikiResourceBundle("DraftPage.label.day-ago", this.getClass().getClassLoader()), days);
    }
    
    long hours = timeDistance / ahour;
    if (hours > 0) {
      return appendTime(Utils.getWikiResourceBundle("DraftPage.label.hour-ago", this.getClass().getClassLoader()), hours);
    }
    
    long minutes = timeDistance / aminute;
    if (minutes > 0) {
      return appendTime(Utils.getWikiResourceBundle("DraftPage.label.minute-ago", this.getClass().getClassLoader()), minutes);
    }
    
    long seconds = timeDistance / asecond;
    if (seconds > 0) {
      return appendTime(Utils.getWikiResourceBundle("DraftPage.label.second-ago", this.getClass().getClassLoader()), seconds);
    }
    return  appendTime(Utils.getWikiResourceBundle("DraftPage.label.miliseconds-ago", this.getClass().getClassLoader()), timeDistance);
  }
  
  private String appendTime(String message, long time) {
    return message.replace("{0}", String.valueOf(time));
  }
}
