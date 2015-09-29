/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.wiki.tree;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.core.api.wiki.WikiNodeType;
import org.exoplatform.wiki.service.PermissionType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.utils.Utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class WikiHomeTreeNode extends TreeNode {
  private Page wikiHome;

  private WikiService wikiService;

  public WikiHomeTreeNode(Page wikiHome) throws Exception {
    super(wikiHome.getTitle(), TreeNodeType.WIKIHOME);

    wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);

    this.wikiHome = wikiHome;
    this.path = this.buildPath();
  }

  @Override
  protected void addChildren(HashMap<String, Object> context) throws Exception {
    Collection<Page> pages = wikiService.getChildrenPageOf(wikiHome);
    Iterator<Page> childPageIterator = pages.iterator();
    int count = 0;
    int size = getNumberOfChildren(context, pages.size());
    Page currentPage = (Page) context.get(TreeNode.SELECTED_PAGE);
    while (childPageIterator.hasNext() && count < size) {
      Page childPage = childPageIterator.next();
      if (wikiService.hasPermissionOnPage(childPage, PermissionType.VIEWPAGE, ConversationState.getCurrent().getIdentity())
              ||  (currentPage != null && Utils.isDescendantPage(currentPage, childPage))) {
        PageTreeNode child = new PageTreeNode(childPage);
        this.children.add(child);
      }
      count++;
    }
    super.addChildren(context);
  }

  public Page getWikiHome() {
    return wikiHome;
  }

  public PageTreeNode getChildByName(String name) throws Exception {
    for (TreeNode child : children) {
      if (child.getName().equals(name))
        return (PageTreeNode)child;
    }
    return null;
  }

  public PageTreeNode findDescendantNodeByName(List<TreeNode> listPageTreeNode, String name) throws Exception {
    for (TreeNode pageTreeNode : listPageTreeNode) {
      if (pageTreeNode.getName().equals(name)) {
        return (PageTreeNode)pageTreeNode;
      } else {
        List<TreeNode> listChildPageTreeNode =  pageTreeNode.getChildren();
        if (listChildPageTreeNode.size() > 0) {
          return (PageTreeNode)findDescendantNodeByName(listChildPageTreeNode, name);
        }
      }
    }
    return null;
  }
  
  @Override
  public String buildPath() {
    try {
      Wiki wiki = wikiService.getWikiByTypeAndOwner(wikiHome.getWikiType(), wikiHome.getWikiOwner());
      WikiPageParams params = new WikiPageParams(wiki.getType(), wiki.getOwner(),WikiNodeType.Definition.WIKI_HOME_NAME );
      return TreeUtils.getPathFromPageParams(params);
    } catch (Exception e) {
      // TODO log
      e.printStackTrace();
      return null;
    }
  }
}
