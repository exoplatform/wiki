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

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.chromattic.api.ChromatticSession;
import org.chromattic.api.UndeclaredRepositoryException;
import org.chromattic.api.annotations.MappedBy;
import org.chromattic.api.annotations.OneToOne;
import org.chromattic.api.annotations.PrimaryType;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.wiki.mow.api.WikiNodeType;
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.utils.Utils;

/**
 * @version $Revision$
 */
@PrimaryType(name = WikiNodeType.GROUP_WIKI_CONTAINER)
public abstract class GroupWikiContainer extends WikiContainer<GroupWiki> {
  
  @OneToOne
  @MappedBy(WikiNodeType.Definition.GROUP_WIKI_CONTAINER_NAME)
  public abstract WikiStoreImpl getMultiWiki();
  
  public GroupWiki addWiki(String wikiOwner) {
    return getWikiObject(wikiOwner, true);
  }
  
  /**
   * Gets the group wiki in current GroupWikiContainer by specified wiki owner
   * @param wikiOwner the wiki owner
   * @param createIfNonExist if true, create the wiki when it does not exist
   * @return the wiki object
   */
  protected GroupWiki getWikiObject(String wikiOwner, boolean createIfNonExist) {
    //check if wiki object is created
    //Group wikis is stored in /Groups/$wikiOwner/ApplicationData/eXoWiki/WikiHome
    boolean isCreatedWikiObject = false;
    wikiOwner = validateWikiOwner(wikiOwner);
    if(wikiOwner == null){
      return null;
    }
    OrganizationService organizationService = (OrganizationService) ExoContainerContext.getCurrentContainer()
    		                                                                            .getComponentInstanceOfType(OrganizationService.class);
    CommonsUtils.startRequest(organizationService);
    try {
      if (organizationService.getGroupHandler().findGroupById(wikiOwner) == null) {
        return null;
      }
    } catch (Exception ex) {
      return null;
    } finally {
      CommonsUtils.endRequest(organizationService);
    }
    ChromatticSession session = getMultiWiki().getSession();
    Node wikiNode = null;
    try {
      Node rootNode = session.getJCRSession().getRootNode();
      Node groupDataNode = rootNode.getNode("Groups" + wikiOwner + "/" + "ApplicationData");
      try {
        wikiNode = groupDataNode.getNode(WikiNodeType.Definition.WIKI_APPLICATION);
      } catch (PathNotFoundException e) {
        if (createIfNonExist) {
          wikiNode = groupDataNode.addNode(WikiNodeType.Definition.WIKI_APPLICATION, WikiNodeType.GROUP_WIKI);
          groupDataNode.save();
          isCreatedWikiObject = true;
        } else {
          return null;
        }
      }
    } catch (RepositoryException e) {
      throw new UndeclaredRepositoryException(e);
    }
    GroupWiki gwiki = session.findByNode(GroupWiki.class, wikiNode);
    gwiki.setWikiService(getwService());
    gwiki.setGroupWikis(this);
    if (isCreatedWikiObject) {
      gwiki.setOwner(wikiOwner);
      gwiki.getPreferences();
      initDefaultPermisisonForWiki(gwiki);
      session.save();
    }
    return gwiki;
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
