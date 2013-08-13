/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.service.search;

import java.util.List;
import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;

import org.apache.commons.lang.StringUtils;
import org.chromattic.api.NoSuchNodeException;
import org.chromattic.api.UndeclaredRepositoryException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.wiki.mow.api.WikiNodeType;
import org.exoplatform.wiki.utils.Utils;

public class SearchData {
  public String title;

  public String content;

  public String wikiType;

  public String wikiOwner;

  public String pageId;

  public String jcrQueryPath;
  
  private long offset = 0;
  
  protected String sort;
  
  protected String order;
  
  protected List<String> propertyConstraints = new ArrayList<String>();;
    
  public int limit = Integer.MAX_VALUE;
  
  public static final String ALL_PATH    = "%/";

  protected static String    PORTAL_PATH = "/exo:applications/"
                                             + WikiNodeType.Definition.WIKI_APPLICATION + "/"
                                             + WikiNodeType.Definition.WIKIS + "/%/";

  protected static String    GROUP_PATH  = "/Groups/%/ApplicationData/"
                                             + WikiNodeType.Definition.WIKI_APPLICATION + "/";

  protected String           USER_PATH   = "/Users/%/ApplicationData/" + WikiNodeType.Definition.WIKI_APPLICATION + "/";

  public SearchData(String title, String content, String wikiType, String wikiOwner, String pageId, List<String> constraints) {
    this.title = org.exoplatform.wiki.utils.Utils.escapeIllegalCharacterInQuery(title);
    this.content = org.exoplatform.wiki.utils.Utils.escapeIllegalCharacterInQuery(content);
    this.wikiType = wikiType;
    this.wikiOwner = Utils.validateWikiOwner(wikiType, wikiOwner);
    this.pageId = pageId;
    if (PortalConfig.USER_TYPE.equals(wikiType)) {
      NodeHierarchyCreator nodeHierachyCreator = (NodeHierarchyCreator) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(NodeHierarchyCreator.class);
      try {
        if (wikiOwner != null && wikiOwner.length() > 0) {
          Node userNode = nodeHierachyCreator.getUserApplicationNode(Utils.createSystemProvider(), wikiOwner);
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
    this.propertyConstraints = new ArrayList<String>();
    if (constraints != null) {
      this.propertyConstraints.addAll(constraints);
    }
  }
  
  public SearchData(String title, String content, String wikiType, String wikiOwner, String pageId) {
    this(title, content, wikiType, wikiOwner, pageId, null);
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = org.exoplatform.wiki.utils.Utils.escapeIllegalCharacterInQuery(title);
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = org.exoplatform.wiki.utils.Utils.escapeIllegalCharacterInQuery(content);
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

  public String getPageId() {
    return pageId;
  }

  public void setPageId(String pageId) {
    this.pageId = pageId;
  }

  public String getJcrQueryPath() {
    return jcrQueryPath;
  }

  public void setJcrQueryPath(String jcrQueryPath) {
    this.jcrQueryPath = jcrQueryPath;
  }
  
  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public String getStatementForSearchingTitle() {
    return null;
  }
  
  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }

  public String getSort() {
    return sort;
  }

  public void setSort(String sort) {
    this.sort = sort;
  }

  public String getOrder() {
    return order;
  }

  public void setOrder(String order) {
    this.order = order;
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
  
}
