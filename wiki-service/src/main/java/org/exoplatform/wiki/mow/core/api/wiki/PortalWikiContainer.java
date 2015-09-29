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
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.utils.Utils;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * @version $Revision$
 */
@PrimaryType(name = WikiNodeType.PORTAL_WIKI_CONTAINER)
public abstract class PortalWikiContainer extends WikiContainer<PortalWiki> {

  @OneToOne
  @MappedBy(WikiNodeType.Definition.PORTAL_WIKI_CONTAINER_NAME)
  public abstract WikiStoreImpl getMultiWiki();
  
  public PortalWiki addWiki(String wikiOwner) {
    return getWikiObject(wikiOwner, true);
  }
  
  /**
   * Gets the portal wiki in current PortalWikiContainer by specified wiki owner
   * @param wikiOwner the wiki owner
   * @param createIfNonExist if true, create the wiki when it does not exist
   * @return the wiki object
   */
  protected PortalWiki getWikiObject(String wikiOwner, boolean createIfNonExist) {
    //check if wiki object is created
    boolean isCreatedWikiObject = false;
    //Portal wikis is stored in /exo:applications/eXoWiki/wikis/$wikiOwner/WikiHome
    wikiOwner = validateWikiOwner(wikiOwner);
    if(wikiOwner == null){
      return null;
    }
    ChromatticSession session = getMultiWiki().getSession();
    Node wikiNode = null;
    try {
      Node wikisNode = (Node)session.getJCRSession().getItem(Utils.getPortalWikisPath()) ;
      try {
        wikiNode = wikisNode.getNode(wikiOwner);
      } catch (PathNotFoundException e) {
        if (createIfNonExist) {
          wikiNode = wikisNode.addNode(wikiOwner, WikiNodeType.PORTAL_WIKI);
          //wikiNode.addNode(WikiNodeType.Definition.TRASH_NAME, WikiNodeType.WIKI_TRASH) ;
          wikisNode.save();
          isCreatedWikiObject = true;
        } else {
          return null;
        }
      }
    } catch (RepositoryException e) {
      throw new UndeclaredRepositoryException(e);
    }
    PortalWiki pwiki = session.findByNode(PortalWiki.class, wikiNode);
    pwiki.setPortalWikis(this);
    if (isCreatedWikiObject) {
      pwiki.setOwner(wikiOwner);
      pwiki.getPreferences();
      session.save();
    }
    return pwiki;
  }
}
