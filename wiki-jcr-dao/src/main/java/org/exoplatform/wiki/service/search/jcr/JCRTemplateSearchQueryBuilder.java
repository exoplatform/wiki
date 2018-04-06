package org.exoplatform.wiki.service.search.jcr;

import org.apache.commons.lang.StringUtils;
import org.chromattic.api.NoSuchNodeException;
import org.chromattic.api.UndeclaredRepositoryException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.mow.core.api.wiki.WikiNodeType;
import org.exoplatform.wiki.service.search.SearchData;
import org.exoplatform.wiki.utils.JCRUtils;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class JCRTemplateSearchQueryBuilder {

  private Log log = ExoLogger.getLogger(JCRTemplateSearchQueryBuilder.class);

  public static final String ALL_PATH    = "%/";

  protected static String    PORTAL_PATH = "/exo:applications/"
          + WikiNodeType.Definition.WIKI_APPLICATION + "/"
          + WikiNodeType.Definition.WIKIS + "/%/";

  protected static String    GROUP_PATH  = "/Groups/%/ApplicationData/"
          + WikiNodeType.Definition.WIKI_APPLICATION + "/";

  protected String           USER_PATH   = "/Users/%/ApplicationData/" + WikiNodeType.Definition.WIKI_APPLICATION + "/";

  public static String TEMPLATE_PATH    = WikiNodeType.Definition.PREFERENCES + "/"
          + WikiNodeType.Definition.TEMPLATE_CONTAINER + "/%";

  public static String ALL_TEMPLATESPATH    = ALL_PATH + TEMPLATE_PATH;

  public static String PORTAL_TEMPLATESPATH = PORTAL_PATH + TEMPLATE_PATH;

  public static String GROUP_TEMPLATESPATH  = GROUP_PATH + TEMPLATE_PATH;

  private SearchData searchData;

  private String nodeType = null;

  protected List<String> propertyConstraints = new ArrayList<>();

  public JCRTemplateSearchQueryBuilder(SearchData searchData) {
    this.searchData = searchData;

    if (PortalConfig.USER_TYPE.equals(searchData.getWikiType())) {
      NodeHierarchyCreator nodeHierachyCreator = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(NodeHierarchyCreator.class);
      try {
        if (searchData.getWikiOwner() != null && searchData.getWikiOwner().length() > 0) {
          Node userNode = nodeHierachyCreator.getUserApplicationNode(JCRUtils.createSystemProvider(), searchData.getWikiOwner());
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
  }

  public void setNodeType(String nodeType) {
    this.nodeType = nodeType;
  }

  public String getNodeType() {
    return this.nodeType;
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

  public String createJcrQueryPath() {
    String jcrQueryPath = StringUtils.EMPTY;
    if (searchData.getWikiType() == null && searchData.getWikiOwner() == null) {
      jcrQueryPath = "jcr:path LIKE '" + ALL_TEMPLATESPATH + "'";
    }
    if (searchData.getWikiType() != null) {
      if (searchData.getWikiType().equals(PortalConfig.USER_TYPE))
        jcrQueryPath = "jcr:path LIKE '" + USER_PATH + TEMPLATE_PATH + "'";
      else {
        if (searchData.getWikiType().equals(PortalConfig.PORTAL_TYPE)) {
          jcrQueryPath = "jcr:path LIKE '" + PORTAL_TEMPLATESPATH + "'";
        } else if (searchData.getWikiType().equals(PortalConfig.GROUP_TYPE))
          jcrQueryPath = "jcr:path LIKE '" + GROUP_TEMPLATESPATH + "'";

        if (searchData.getWikiOwner() != null) {
          jcrQueryPath = jcrQueryPath.replaceFirst("%", searchData.getWikiOwner());
        }
      }
    }
    return jcrQueryPath;
  }

  public String getStatementForSearchingTitle() {
    StringBuilder statement = new StringBuilder();
    try {
      String title = searchData.getTitle();
      statement.append("SELECT title, jcr:primaryType, path,"+WikiNodeType.Definition.DESCRIPTION)
              .append(" FROM ")
              .append(WikiNodeType.WIKI_PAGE)
              .append(" WHERE ");
      statement.append(createJcrQueryPath());
      if (title != null && title.length() > 0) {
        statement.append(" AND ")
                .append(" CONTAINS(" + WikiNodeType.Definition.TITLE + ", '")
                .append(title)
                .append("') ");
      }
    } catch (Exception e) {
      log.error("Failed to get statement ", e);
    }
    return statement.toString();
  }
}
