package org.exoplatform.wiki.service;

import org.exoplatform.services.jcr.util.Text;
import java.util.HashMap;
import java.util.Map;

public class WikiPageParams {

  public static final String  WIKI_HOME  = "WikiHome";

  private String              type;

  private String              owner;

  private String              pageName;

  private String              attachmentName;

  private Map<String, String[]> parameters = new HashMap<String, String[]>();

  public WikiPageParams() {    
  }

  public WikiPageParams(String type, String owner, String pageName) {
    this.type = type;
    this.owner = (owner==null)?owner:Text.unescapeIllegalJcrChars(owner);
    this.pageName = pageName;
  }

  /**
   * @param type
   * @param owner
   * @param pageName
   * @param attachmentName
   */
  public WikiPageParams(String type, String owner, String pageName, String attachmentName) {
    super();
    this.type = type;
    this.owner = (owner==null)?owner:Text.unescapeIllegalJcrChars(owner);
    this.pageName = pageName;
    this.attachmentName = attachmentName;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public void setOwner(String owner) {
    this.owner = (owner==null)?owner:Text.unescapeIllegalJcrChars(owner);
  }

  public String getOwner() {
    return owner;
  }

  public void setPageName(String pageName) {
    this.pageName = pageName;
  }

  public String getPageName() {
    return pageName;
  }

  public String getAttachmentName() {
    return attachmentName;
  }

  public void setAttachmentName(String attachmentName) {
    this.attachmentName = attachmentName;
  }

  public void setParameter(String key, String[] values) {
    parameters.put(key, values);
  }

  public String getParameter(String name) {
    String[] values = parameters.get(name);
    return (values == null) ? null : values[0];
  }

  public Map<String, String[]> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String[]> parameters) {
    this.parameters = parameters;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((attachmentName == null) ? 0 : attachmentName.hashCode());
    result = prime * result + ((owner == null) ? 0 : owner.hashCode());
    result = prime * result + ((pageName == null) ? 0 : pageName.hashCode());
    result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    WikiPageParams other = (WikiPageParams) obj;
    if (attachmentName == null) {
      if (other.attachmentName != null)
        return false;
    } else if (!attachmentName.equals(other.attachmentName))
      return false;
    if (owner == null) {
      if (other.owner != null)
        return false;
    } else if (!owner.equals(other.owner))
      return false;
    if (pageName == null) {
      if (other.pageName != null)
        return false;
    } else if (!pageName.equals(other.pageName))
      return false;
    if (parameters == null) {
      if (other.parameters != null)
        return false;
    } else if (!parameters.equals(other.parameters))
      return false;
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    return true;
  }

}
