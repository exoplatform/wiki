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
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.utils.JCRUtils;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * @version $Revision$
 */
@PrimaryType(name = WikiNodeType.GROUP_WIKI_CONTAINER)
public abstract class GroupWikiContainer extends WikiContainer<GroupWiki> {
  
  @OneToOne
  @MappedBy(WikiNodeType.Definition.GROUP_WIKI_CONTAINER_NAME)
  public abstract WikiStoreImpl getMultiWiki();

  @Override
  public GroupWiki addWiki(Wiki wiki) throws WikiException {
    GroupWiki groupWiki = getWikiObject(wiki.getOwner());
    if(groupWiki == null) {
      groupWiki = createWiki(wiki);
    }
    return groupWiki;
  }
  
  /**
   * Gets the group wiki in current GroupWikiContainer by specified wiki owner
   * @param wikiOwner the wiki owner
   * @return the wiki object
   */
  @Override
  protected GroupWiki getWikiObject(String wikiOwner) {
    //Group wikis is stored in /Groups/$wikiOwner/ApplicationData/eXoWiki/WikiHome
    wikiOwner = validateWikiOwner(wikiOwner);
    if(wikiOwner == null){
      return null;
    }
    OrganizationService organizationService = ExoContainerContext.getCurrentContainer()
            .getComponentInstanceOfType(OrganizationService.class);
    try {
      if (organizationService.getGroupHandler().findGroupById(wikiOwner) == null) {
        return null;
      }
    } catch (Exception ex) {
      return null;
    }
    ChromatticSession session = mowService.getSession();
    Node wikiNode;
    try {
      Node rootNode = session.getJCRSession().getRootNode();
      Node groupDataNode = rootNode.getNode("Groups" + wikiOwner + "/" + "ApplicationData");
      try {
        wikiNode = groupDataNode.getNode(WikiNodeType.Definition.WIKI_APPLICATION);
      } catch (PathNotFoundException e) {
        wikiNode = groupDataNode.addNode(WikiNodeType.Definition.WIKI_APPLICATION, WikiNodeType.GROUP_WIKI);
        groupDataNode.save();
      }
    } catch (RepositoryException e) {
      throw new UndeclaredRepositoryException(e);
    }
    GroupWiki gwiki = session.findByNode(GroupWiki.class, wikiNode);
    gwiki.setGroupWikis(this);
    if (gwiki.getOwner() == null) gwiki.setOwner(wikiOwner);
    return gwiki;
  }

  @Override
  public GroupWiki createWiki(Wiki wiki) throws WikiException {
    OrganizationService organizationService = ExoContainerContext.getCurrentContainer()
            .getComponentInstanceOfType(OrganizationService.class);

    try {
      String wikiOwner = validateWikiOwner(wiki.getOwner());
      if(wikiOwner == null){
        return null;
      }

      if (organizationService.getGroupHandler().findGroupById(wikiOwner) == null) {
        throw new WikiException("Cannot create wiki " + wiki.getType() + ":" + wikiOwner + " because group " + wikiOwner + " does not exist.");
      }

      ChromatticSession session = mowService.getSession();
      Node rootNode = session.getJCRSession().getRootNode();
      Node groupDataNode = rootNode.getNode("Groups" + wiki.getOwner() + "/" + "ApplicationData");
      Node wikiNode = groupDataNode.addNode(WikiNodeType.Definition.WIKI_APPLICATION, WikiNodeType.GROUP_WIKI);
      groupDataNode.save();
      GroupWiki gwiki = session.findByNode(GroupWiki.class, wikiNode);
      gwiki.setGroupWikis(this);
      gwiki.setOwner(wiki.getOwner());
      gwiki.getPreferences();
      if(wiki.getPermissions() != null) {
        gwiki.setWikiPermissions(JCRUtils.convertPermissionEntryListToWikiPermissions(wiki.getPermissions()));
        gwiki.setDefaultPermissionsInited(true);
      }
      session.save();
      return gwiki;
    } catch (Exception e) {
      throw new WikiException("Cannot create wiki " + wiki.getType() + ":" + wiki.getOwner(), e);
    }
  }

  protected String validateWikiOwner(String wikiOwner){
    if(wikiOwner == null || wikiOwner.length() == 0){
      return null;
    }
    if(!wikiOwner.startsWith("/")){
      wikiOwner = "/" + wikiOwner;
    }
    if(wikiOwner.endsWith("/")){
      wikiOwner = wikiOwner.substring(0,wikiOwner.length()-1);
    }
    return wikiOwner;
  }
}
