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
@PrimaryType(name = WikiNodeType.PORTAL_WIKI_CONTAINER)
public abstract class PortalWikiContainer extends WikiContainer<PortalWiki> {

  @OneToOne
  @MappedBy(WikiNodeType.Definition.PORTAL_WIKI_CONTAINER_NAME)
  public abstract WikiStoreImpl getMultiWiki();

  @Override
  public PortalWiki addWiki(Wiki wiki) throws WikiException {
    PortalWiki portalWiki = getWikiObject(wiki.getOwner());
    if(portalWiki == null) {
      portalWiki = createWiki(wiki);
    }
    return portalWiki;
  }
  
  /**
   * Gets the portal wiki in current PortalWikiContainer by specified wiki owner
   * @param wikiOwner the wiki owner
   * @return the wiki object
   */
  @Override
  protected PortalWiki getWikiObject(String wikiOwner) {
    //Portal wikis is stored in /exo:applications/eXoWiki/wikis/$wikiOwner/WikiHome
    wikiOwner = validateWikiOwner(wikiOwner);
    if(wikiOwner == null){
      return null;
    }
    ChromatticSession session = mowService.getSession();
    Node wikiNode;
    try {
      Node wikisNode = (Node)session.getJCRSession().getItem(getPortalWikisPath()) ;
      try {
        wikiNode = wikisNode.getNode(wikiOwner);
      } catch (PathNotFoundException e) {
        wikiNode = wikisNode.addNode(wikiOwner, WikiNodeType.PORTAL_WIKI);
        wikisNode.save();
      }
    } catch (RepositoryException e) {
      throw new UndeclaredRepositoryException(e);
    }

    PortalWiki pwiki = session.findByNode(PortalWiki.class, wikiNode);
    pwiki.setPortalWikis(this);
    if (pwiki.getOwner() == null) pwiki.setOwner(wikiOwner);

    return pwiki;
  }

  @Override
  public PortalWiki createWiki(Wiki wiki) throws WikiException {
    try {
      String wikiOwner = validateWikiOwner(wiki.getOwner());
      if(wikiOwner == null){
        return null;
      }

      ChromatticSession session = mowService.getSession();
      Node wikisNode = (Node)session.getJCRSession().getItem(getPortalWikisPath()) ;
      Node wikiNode = wikisNode.addNode(wikiOwner, WikiNodeType.PORTAL_WIKI);
      wikisNode.save();

      PortalWiki pwiki = session.findByNode(PortalWiki.class, wikiNode);
      pwiki.setPortalWikis(this);
      pwiki.setOwner(wikiOwner);
      if(wiki.getPermissions() != null) {
        pwiki.setWikiPermissions(JCRUtils.convertPermissionEntryListToWikiPermissions(wiki.getPermissions()));
        pwiki.setDefaultPermissionsInited(true);
      }
      pwiki.getPreferences();
      session.save();

      return pwiki;
    } catch (Exception e) {
      throw new WikiException("Cannot create wiki " + wiki.getType() + ":" + wiki.getOwner(), e);
    }
  }

  //The path should get from NodeHierarchyCreator
  public static String getPortalWikisPath() {
    String path = "/exo:applications/"
            + WikiNodeType.Definition.WIKI_APPLICATION + "/"
            + WikiNodeType.Definition.WIKIS ;
    return path ;
  }
}
