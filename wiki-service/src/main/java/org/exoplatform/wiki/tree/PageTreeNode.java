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
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.utils.Utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class PageTreeNode extends TreeNode {
  private Page page;

  private WikiService wikiService;

  public PageTreeNode(Page page) throws Exception {
    super(page.getTitle(), TreeNodeType.PAGE);

    this.wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);

    this.page = page;
    this.path = buildPath();
  }

  public Page getPage() {
    return page;
  }

  public void setPage(Page page) {
    this.page = page;
  }

  @Override
  protected void addChildren(HashMap<String, Object> context) throws Exception {
    // TODO need getChildrenByRootPermission ?
    //Collection<Page> pages = page.getChildrenByRootPermission().values();
    Collection<Page> pages = wikiService.getChildrenPageOf(page);
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

  public PageTreeNode getChildByName(String name) throws Exception {
    for (TreeNode child : children) {
      if (child.getName().equals(name))
        return (PageTreeNode) child;
    }
    return null;
  }
  
  @Override
  public String buildPath() {
    try {
      WikiPageParams params = new WikiPageParams(page.getWikiType(), page.getWikiOwner(), page.getName());
      return TreeUtils.getPathFromPageParams(params);
    } catch (Exception e) {
      // TODO log
      e.printStackTrace();
      return null;
    }
  }

}
