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
package org.exoplatform.wiki.tree.utils;

import org.apache.commons.lang.BooleanUtils;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.rendering.macro.ExcerptUtils;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.tree.*;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.WikiConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TreeUtils {
  
  /**
   * Create a tree node with a given {@link WikiPageParams}
   * 
   * @param params is the wiki page parameters
   * @return <code>TreeNode</code>
   * @throws Exception
   */
  public static TreeNode getTreeNode(WikiPageParams params) throws Exception {
    Object wikiObject = Utils.getObjectFromParams(params);
    if (wikiObject instanceof Page) {
      Page page = (Page) wikiObject;
      if(params.getPageName().equals(WikiConstants.WIKI_HOME_NAME)) {
        WikiHomeTreeNode wikiHomeNode = new WikiHomeTreeNode(page);
        return wikiHomeNode;
      } else {
        PageTreeNode pageNode = new PageTreeNode(page);
        return pageNode;
      }
    } else if (wikiObject instanceof Wiki) {
      Wiki wiki = (Wiki) wikiObject;
      WikiTreeNode wikiNode = new WikiTreeNode(wiki);
      return wikiNode;
    } else if (wikiObject instanceof String) {
      SpaceTreeNode spaceNode = new SpaceTreeNode((String) wikiObject);
      return spaceNode;
    }
    return new TreeNode();
  }
  
  /**
   * Create a tree node contain all its descendant with a given {@link WikiPageParams} 
   * 
   * @param params is the wiki page parameters
   * @param context is the page tree context
   * @return <code>TreeNode</code>
   * @throws Exception
   */
  public static TreeNode getDescendants(WikiPageParams params, HashMap<String, Object> context) throws Exception {
    TreeNode treeNode = getTreeNode(params);
    treeNode.pushDescendants(context);
    return treeNode;
  }
  
  public static List<JsonNodeData> tranformToJson(TreeNode treeNode, HashMap<String, Object> context) throws Exception {
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);

    int counter = 1;
    Boolean showExcerpt = false;
    Page currentPage = null;
    String currentPath = null;
    Boolean canEdit      = false;
    if (context != null) {
      currentPath = (String) context.get(TreeNode.CURRENT_PATH);
      currentPage = (Page) context.get(TreeNode.CURRENT_PAGE);
      showExcerpt = (Boolean) context.get(TreeNode.SHOW_EXCERPT);
      canEdit     = (Boolean)context.get(TreeNode.CAN_EDIT);
    }
    
    List<JsonNodeData> children = new ArrayList<JsonNodeData>();
    for (TreeNode child : treeNode.getChildren()) {
      boolean isSelectable = true;
      boolean isLastNode = false;
      if (counter >= treeNode.getChildren().size()) {
        isLastNode = true;
      }
      
      if (child.getNodeType().equals(TreeNodeType.WIKI)) {
        isSelectable = false;
      } else if (child.getNodeType().equals(TreeNodeType.PAGE)) {
        Page page = ((PageTreeNode) child).getPage();
        if (((currentPage != null) && (currentPage.equals(page) || Utils.isDescendantPage(page, currentPage)))) {
          isSelectable = false;
        }
        
        if (!wikiService.hasPermissionOnPage(page, PermissionType.VIEWPAGE, ConversationState.getCurrent().getIdentity())) {
          isSelectable = false;
          child.setRetricted(true);
        }
        if(BooleanUtils.isTrue(canEdit) && !wikiService.hasPermissionOnPage(page, PermissionType.EDITPAGE, ConversationState.getCurrent().getIdentity())){
          isSelectable = false;
          child.setRetricted(true);
        }
      } else if (child.getNodeType().equals(TreeNodeType.WIKIHOME)) {
        Page page = ((WikiHomeTreeNode) child).getWikiHome();
        if (!wikiService.hasPermissionOnPage(page, PermissionType.VIEWPAGE, ConversationState.getCurrent().getIdentity())) {
          isSelectable = false;
          child.setRetricted(true);
        }

        if(BooleanUtils.isTrue(canEdit) && !wikiService.hasPermissionOnPage(page, PermissionType.EDITPAGE, ConversationState.getCurrent().getIdentity())){
          isSelectable = false;
          child.setRetricted(true);
        }

      }
      
      String excerpt = null;
      if (showExcerpt != null && showExcerpt) {
        WikiPageParams params = getPageParamsFromPath(child.getPath());
        excerpt = ExcerptUtils.getExcerpts(params);
      }
      
      children.add(new JsonNodeData(child, isLastNode, isSelectable, currentPath, excerpt, context));
      counter++;
    }
    return children;
  }
  
  public static WikiPageParams getPageParamsFromPath(String path) throws Exception {
    if (path == null) {
      return null;
    }
    WikiPageParams result = new WikiPageParams();
    path = path.trim();
    if (path.indexOf("/") < 0) {
      result.setType(path);
    } else {
      String[] array = path.split("/");
      result.setType(array[0]);
      if (array.length < 3) {
        result.setOwner(array[1]);
      } else if (array.length >= 3) {
        if (array[0].equals(PortalConfig.GROUP_TYPE)) {
          OrganizationService oService = (OrganizationService) ExoContainerContext.getCurrentContainer()
                                                                                  .getComponentInstanceOfType(OrganizationService.class);
          String groupId = path.substring(path.indexOf("/"));
          if (oService.getGroupHandler().findGroupById(groupId) != null) {
            result.setOwner(groupId);
          } else {
            String pageName = path.substring(path.lastIndexOf("/") + 1);
            if(StringUtils.isBlank(pageName)) {
              pageName = WikiPageParams.WIKI_HOME;
            }
            result.setPageName(pageName);
            String owner = path.substring(path.indexOf("/"), path.lastIndexOf("/"));
            while (oService.getGroupHandler().findGroupById(owner) == null) {
              owner = owner.substring(0,owner.lastIndexOf("/"));
            }
            result.setOwner(owner);
          }
        } else {
          // if (array[0].equals(PortalConfig.PORTAL_TYPE) || array[0].equals(PortalConfig.USER_TYPE))
          result.setOwner(array[1]);
          result.setPageName(array[array.length - 1]);
        }
      }
    }
    return result;
  }
 
  public static String getPathFromPageParams(WikiPageParams param) {
    StringBuilder sb = new StringBuilder();
    if (param.getType() != null) {
      sb.append(param.getType());
    }
    if (param.getOwner() != null) {
      sb.append("/").append(Utils.validateWikiOwner(param.getType(), param.getOwner()));
    }
    if (param.getPageName() != null) {
      sb.append("/").append(param.getPageName());
    }
    return sb.toString();
  }
}
