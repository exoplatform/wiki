package org.exoplatform.wiki.service.search.jcr;

import org.apache.commons.lang.StringUtils;
import org.chromattic.api.NoSuchNodeException;
import org.chromattic.api.UndeclaredRepositoryException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.wiki.mow.core.api.wiki.WikiNodeType;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.utils.JCRUtils;
import org.exoplatform.wiki.utils.WikiConstants;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class JCRWikiSearchQueryBuilder {

  public static final String ALL_PATH    = "%/";

  protected static String    PORTAL_PATH = "/exo:applications/"
          + WikiNodeType.Definition.WIKI_APPLICATION + "/"
          + WikiNodeType.Definition.WIKIS + "/%/";

  protected static String    GROUP_PATH  = "/Groups/%/ApplicationData/"
          + WikiNodeType.Definition.WIKI_APPLICATION + "/";

  protected String           USER_PATH   = "/Users/%/ApplicationData/" + WikiNodeType.Definition.WIKI_APPLICATION + "/";

  public static String ALL_PAGESPATH    = ALL_PATH + WikiConstants.WIKI_HOME_NAME;

  public static String PORTAL_PAGESPATH = PORTAL_PATH + WikiConstants.WIKI_HOME_NAME;

  public static String GROUP_PAGESPATH  = GROUP_PATH + WikiConstants.WIKI_HOME_NAME;

  public static String ASC_ORDER = "ASC";

  public static String DESC_ORDER = "DESC";

  private WikiSearchData wikiSearchData;

  private String pagePath = "";

  protected List<String> propertyConstraints = new ArrayList<>();

  public JCRWikiSearchQueryBuilder(WikiSearchData wikiSearchData) {
    this.wikiSearchData = wikiSearchData;

    if (PortalConfig.USER_TYPE.equals(wikiSearchData.getWikiType())) {
      NodeHierarchyCreator nodeHierachyCreator = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(NodeHierarchyCreator.class);
      try {
        if (wikiSearchData.getWikiOwner() != null && wikiSearchData.getWikiOwner().length() > 0) {
          Node userNode = nodeHierachyCreator.getUserApplicationNode(JCRUtils.createSystemProvider(), wikiSearchData.getWikiOwner());
          USER_PATH = userNode.getPath() + "/" + WikiNodeType.Definition.WIKI_APPLICATION + "/";
        }
      } catch (Exception e) {
        if (e instanceof PathNotFoundException) {
          throw new NoSuchNodeException(e);
        } else {
          throw new UndeclaredRepositoryException(e.getMessage());
        }
      }
    }
    this.propertyConstraints = new ArrayList<>();

    initJcrQueryPath();
  }

  public List<String> getPropertyConstraints() {
    return new ArrayList<String>(this.propertyConstraints);
  }

  public void addPropertyConstraints(List<String> value) {
    if (value != null) {
      propertyConstraints.addAll(value);
    }
  }

  public void addPropertyConstraint(String value) {
    if (StringUtils.isNotBlank(value)) {
      propertyConstraints.add(value);
    }
  }

  public void initJcrQueryPath() {
    if (wikiSearchData.getWikiType() == null && wikiSearchData.getWikiOwner() == null) {
      pagePath = ALL_PAGESPATH;
    } else if (wikiSearchData.getWikiType() != null) {
      if (wikiSearchData.getWikiType().equals(PortalConfig.USER_TYPE)){
        pagePath = USER_PATH + WikiConstants.WIKI_HOME_NAME;
      } else {
        if (wikiSearchData.getWikiType().equals(PortalConfig.PORTAL_TYPE)) {
          pagePath = PORTAL_PAGESPATH;
        } else if (wikiSearchData.getWikiType().equals(PortalConfig.GROUP_TYPE)) {
          pagePath = GROUP_PAGESPATH;
        }

        if (wikiSearchData.getWikiOwner() != null && wikiSearchData.getWikiOwner().length() > 0) {
          pagePath = pagePath.replaceFirst("%", wikiSearchData.getWikiOwner());
        }
      }
    }
  }

  public String getStatementForSearchingTitle() {
    StringBuilder statement = new StringBuilder();
    statement.append("SELECT title, jcr:primaryType, path, excerpt(.) FROM nt:base WHERE ");
    statement.append(createJcrQueryPathClause());
    statement.append(searchTitleCondition());
    statement.append(createOrderClause());
    return statement.toString();
  }

  public String getStatementForSearchingContent() {
    StringBuilder statement = new StringBuilder();
    statement.append("SELECT jcr:primaryType, path, excerpt(.) FROM wiki:attachment WHERE ");
    statement.append(createJcrQueryPathClause());
    statement.append(searchContentCondition());
    statement.append(createOrderClause());
    return statement.toString();
  }

  private String createJcrQueryPathClause() {
    return "(jcr:path LIKE '" + pagePath + "/%')";
  }

  private String createOrderClause() {
    StringBuffer clause = new StringBuffer();
    if (isOrderValid(wikiSearchData.getOrder()) && StringUtils.isNotEmpty(wikiSearchData.getSort())) {
      clause.append(" ORDER BY ");
      clause.append(wikiSearchData.getSort());
      clause.append(" ");
      clause.append(wikiSearchData.getOrder());
    }
    return clause.toString();
  }

  private boolean isOrderValid(String order) {
    return ASC_ORDER.equals(order) || DESC_ORDER.equals(order) || "".equals(order);
  }

  /**
   * get SQL constraint for searching available page (be a child of <code>WikiHome</code> page and not removed).
   * @return
   *        <ul>
   *          <li>returned string is in format:
   *              <code>((jcr:path like [path to page node likely] or jcr:path = [path to page node])
   *              AND (jcr:mixinTypes IS NULL OR NOT (jcr:mixinTypes = 'wiki:removed'))</code>
   *          </li>
   *          <li>
   *              if <code>wikiType</code> or <code>wikiOwner</code> is null,
   *              paths of the constraint are <code>/%/pageId</code> and <code>/pageId</code>.
   *              It means that pages of which id is <code>pageId</code> are searched from <code>root</code>.
   *          </li>
   *        </ul>
   */
  public String getPageConstraint() {
    StringBuilder constraint = new StringBuilder();

    String absPagePath = pagePath + "/" + wikiSearchData.getPageId();
    String pageLikePath = pagePath + "/%/" + wikiSearchData.getPageId();
    boolean isWikiHome = false;
    if (WikiConstants.WIKI_HOME_NAME.equals(wikiSearchData.getPageId())) {
      absPagePath = pagePath;
      isWikiHome = true;
    }
    if (wikiSearchData.getWikiType() == null || wikiSearchData.getWikiOwner() == null) {
      absPagePath = "/" + wikiSearchData.getPageId();
      pageLikePath = "/%/" + wikiSearchData.getPageId();
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
    if (wikiSearchData.getContent() != null && wikiSearchData.getContent().length() > 0 && !"*".equals(wikiSearchData.getContent())) {
      clause.append(" AND ");
      clause.append(" CONTAINS(*, '").append(wikiSearchData.getContent()).append("')");
    }
    return clause.toString();
  }

  private String searchTitleCondition() {
    StringBuilder clause = new StringBuilder();
    if (wikiSearchData.getTitle() != null && wikiSearchData.getTitle().length() > 0) {
      clause.append(" AND ");
      clause.append(" CONTAINS(title, '").append(wikiSearchData.getTitle()).append("')");
    }
    return clause.toString();
  }

}
