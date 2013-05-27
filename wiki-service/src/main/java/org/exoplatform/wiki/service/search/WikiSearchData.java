package org.exoplatform.wiki.service.search;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.mow.api.WikiNodeType;

public class WikiSearchData extends SearchData {

  public static String WIKIHOME_PATH    = WikiNodeType.Definition.WIKI_HOME_NAME;

  public static String ALL_PAGESPATH    = ALL_PATH + WIKIHOME_PATH;

  public static String PORTAL_PAGESPATH = PORTAL_PATH + WIKIHOME_PATH;

  public static String GROUP_PAGESPATH  = GROUP_PATH + WIKIHOME_PATH;
  
  public static String ASC_ORDER = "ASC";
  
  public static String DESC_ORDER = "DESC";

  private String pagePath = "";
  
  private String nodeType = null;
  
  public WikiSearchData(String title, String content, String wikiType, String wikiOwner, String pageId) {
    super(title, content, wikiType, wikiOwner, pageId);
    createJcrQueryPath();
  }

  public WikiSearchData(String wikiType, String wikiOwner, String pageId) {
    this(null, null, wikiType, wikiOwner, pageId);
  }

  public WikiSearchData(String title, String content, String wikiType, String wikiOwner) {
    this(title, content, wikiType, wikiOwner, null);
  }
  
  public void setNodeType(String nodeType) {
    this.nodeType = nodeType;
  }

  public void createJcrQueryPath() {
    if (wikiType == null && wikiOwner == null) {
      pagePath = ALL_PAGESPATH;
    } else if (wikiType != null) {
      if (wikiType.equals(PortalConfig.USER_TYPE)){
        pagePath = USER_PATH + WIKIHOME_PATH;
      } else {
        if (wikiType.equals(PortalConfig.PORTAL_TYPE)) {
          pagePath = PORTAL_PAGESPATH;
        } else if (wikiType.equals(PortalConfig.GROUP_TYPE)) {
          pagePath = GROUP_PAGESPATH;
        }  
         
        if (wikiOwner != null && wikiOwner.length() > 0) {
          pagePath = pagePath.replaceFirst("%", wikiOwner);
        }
      }
    }
    jcrQueryPath = "(jcr:path LIKE '" + pagePath + "/%')";
  }

  public String getStatementForSearchingTitle() {
    StringBuilder statement = new StringBuilder();
    statement.append("SELECT title, jcr:primaryType, path, excerpt(.) ");
    
    if (nodeType == null) {
      statement.append("FROM wiki:page ");
    } else {
      statement.append("FROM " + nodeType + " ");
    }
    statement.append("WHERE ");
    statement.append(searchTitleCondition());
    statement.append(createOrderClause());
    return statement.toString();
  }
  
  public String getStatementForSearchingContent() {
    StringBuilder statement = new StringBuilder();
    statement.append("SELECT jcr:primaryType, path, excerpt(.) ");
    statement.append("FROM nt:base ");
    statement.append("WHERE ");
    statement.append(searchContentCondition());
    statement.append(" AND NOT (jcr:primaryType = 'wiki:page') ");
    statement.append(createOrderClause());
    return statement.toString();
  }

  public String getStatementForRenamedPage() {
    StringBuilder statement = new StringBuilder();
    statement.append("SELECT * ").append("FROM wiki:renamed ").append("WHERE ");
    statement.append(jcrQueryPath);
    if (getPageId() != null && getPageId().length() > 0) {
      statement.append(" AND ");
      statement.append(" oldPageIds = '").append(getPageId()).append("'");
    }
    return statement.toString();
  }
  
  private String createOrderClause() {
    StringBuffer clause = new StringBuffer();
    if (isOrderValid(order) && StringUtils.isNotEmpty(sort)) {
      clause.append(" ORDER BY ");
      clause.append(sort);
      clause.append(" ");
      clause.append(order);
    }
    return clause.toString();
  }
  
  private boolean isOrderValid(String order) {
    return ASC_ORDER.equals(order) || DESC_ORDER.equals(order) || "".equals(order);
  }
  
  /**
   * get SQL constraint for searching available page (be a child of <code>WikiHome</code> page and not removed).
   * @return 
   *        <li>
   *         returned string is in format:
   *        <code>((jcr:path like [path to page node likely] or jcr:path = [path to page node]) 
   *        AND (jcr:mixinTypes IS NULL OR NOT (jcr:mixinTypes = 'wiki:removed'))</code>
   *        </li>
   *        <li>
   *        if <code>wikiType</code> or <code>wikiOwner</code> is null, 
   *        paths of the constraint are <code>/%/pageId</code> and <code>/pageId</code>. 
   *        It means that pages of which id is <code>pageId</code> are searched from <code>root</code>.  
   *        </li> 
   */
  public String getPageConstraint() {
    StringBuilder constraint = new StringBuilder();

    String absPagePath = pagePath + "/" + pageId;
    String pageLikePath = pagePath + "/%/" + pageId;
    boolean isWikiHome = false;
    if (WikiNodeType.Definition.WIKI_HOME_NAME.equals(pageId)) {
      absPagePath = pagePath;
      isWikiHome = true;
    }
    if (wikiType == null || wikiOwner == null) {
      absPagePath = "/" + pageId;
      pageLikePath = "/%/" + pageId;
    }
    constraint.append('(').append('(').append("jcr:path LIKE '").append(pageLikePath).append('\'');
    if (!isWikiHome)
      constraint.append(" or (jcr:path = '").append(absPagePath).append('\'').append(')');
    constraint.append(")")
              .append(" AND ")
              .append("(jcr:mixinTypes IS NULL OR NOT (jcr:mixinTypes = 'wiki:removed'))")
              .append(')');
    return constraint.toString();
  }
  
  private String searchContentCondition() {
    StringBuilder clause = new StringBuilder();
    clause.append(jcrQueryPath);
    if (content != null && content.length() > 0) {
      clause.append(" AND ");
      clause.append(" CONTAINS(*, '").append(content).append("')");
    }
    return clause.toString();
  }
  
  private String searchTitleCondition() {
    StringBuilder clause = new StringBuilder();
    clause.append(jcrQueryPath);
    if (title != null && title.length() > 0) {
      clause.append(" AND ");
      clause.append(" CONTAINS(title, '").append(title).append("')");
    }
    return clause.toString();
  }
}
