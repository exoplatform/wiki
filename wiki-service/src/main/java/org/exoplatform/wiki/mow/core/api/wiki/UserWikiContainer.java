/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wiki.mow.core.api.wiki;

import org.chromattic.api.ChromatticSession;
import org.chromattic.api.UndeclaredRepositoryException;
import org.chromattic.api.annotations.MappedBy;
import org.chromattic.api.annotations.OneToOne;
import org.chromattic.api.annotations.PrimaryType;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.utils.Utils;

import javax.jcr.Node;

/**
 * @version $Revision$
 */
@PrimaryType(name = WikiNodeType.USER_WIKI_CONTAINER)
public abstract class UserWikiContainer extends WikiContainer<UserWiki> {

  @OneToOne
  @MappedBy(WikiNodeType.Definition.USER_WIKI_CONTAINER_NAME )
  public abstract WikiStoreImpl getMultiWiki();

  @Override
  public UserWiki addWiki(Wiki wiki) throws WikiException {
    UserWiki userWiki = getWikiObject(wiki.getOwner());
    if(userWiki == null) {
      userWiki = createWiki(wiki);
    }
    return userWiki;
  }
  
  /**
   * Gets the user wiki in current UserWikiContainer by specified wiki owner
   * @param wikiOwner the wiki owner
   * @return the wiki object
   */
  @Override
  protected UserWiki getWikiObject(String wikiOwner) {
    NodeHierarchyCreator nodeHierachyCreator = ExoContainerContext.getCurrentContainer()
            .getComponentInstanceOfType(NodeHierarchyCreator.class);
    OrganizationService organizationService = ExoContainerContext.getCurrentContainer()
            .getComponentInstanceOfType(OrganizationService.class);

    wikiOwner = validateWikiOwner(wikiOwner);
    if(wikiOwner == null){
      return null;
    }

    try {
      if (organizationService.getUserHandler().findUserByName(wikiOwner) == null) {
        return null;
      }
    } catch (Exception ex) {
      return null;
    }
    ChromatticSession session = mowService.getSession();
    Node wikiNode;
    try {
      Node tempNode = nodeHierachyCreator.getUserApplicationNode(Utils.createSystemProvider(), wikiOwner);
      Node userDataNode = (Node) session.getJCRSession().getItem(tempNode.getPath());
      wikiNode = userDataNode.getNode(WikiNodeType.Definition.WIKI_APPLICATION);
    } catch (Exception e) {
      return null;
    }

    UserWiki uwiki = session.findByNode(UserWiki.class, wikiNode);
    uwiki.setUserWikis(this);

    return uwiki;
  }


  @Override
  public UserWiki createWiki(Wiki wiki) throws WikiException {
    ChromatticSession session = mowService.getSession();
    try {
      NodeHierarchyCreator nodeHierachyCreator = ExoContainerContext.getCurrentContainer()
              .getComponentInstanceOfType(NodeHierarchyCreator.class);
      Node tempNode = nodeHierachyCreator.getUserApplicationNode(Utils.createSystemProvider(), wiki.getOwner());
      Node userDataNode = (Node) session.getJCRSession().getItem(tempNode.getPath());
      Node wikiNode = userDataNode.addNode(WikiNodeType.Definition.WIKI_APPLICATION, WikiNodeType.USER_WIKI);
      userDataNode.save();
      UserWiki uwiki = session.findByNode(UserWiki.class, wikiNode);
      uwiki.setUserWikis(this);
      uwiki.setOwner(wiki.getOwner());
      uwiki.getPreferences();
      if(wiki.getPermissions() != null) {
        uwiki.setWikiPermissions(wiki.getPermissions());
        uwiki.setDefaultPermissionsInited(true);
      }
      session.save();

      return uwiki;
    } catch (Exception e) {
      throw new WikiException("Cannot create wiki " + wiki.getType() + ":" + wiki.getOwner(), e);
    }
  }
}