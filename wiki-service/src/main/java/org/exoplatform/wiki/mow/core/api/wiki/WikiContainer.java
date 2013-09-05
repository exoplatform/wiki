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

import java.util.Collection;
import java.util.List;

import org.chromattic.api.RelationshipType;
import org.chromattic.api.annotations.Create;
import org.chromattic.api.annotations.MappedBy;
import org.chromattic.api.annotations.OneToMany;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiNodeType;
import org.exoplatform.wiki.service.WikiService;

/**
 * @version $Revision$
 */
public abstract class WikiContainer<T extends Wiki> {
  
  private static final Log      log               = ExoLogger.getLogger(WikiContainer.class);

  private WikiService wService;
  
  @OneToMany(type = RelationshipType.REFERENCE)
  @MappedBy(WikiNodeType.Definition.WIKI_CONTAINER_REFERENCE)
  public abstract Collection<T> getWikis();

  /*
   * @OneToOne public abstract WikiStoreImpl getMultiWiki();
   */

  public abstract T addWiki(String wikiOwner);

  @Create
  public abstract T createWiki();  
  
  protected String validateWikiOwner(String wikiOwner){
    return wikiOwner;
  }

  public WikiService getwService() {
    return wService;
  }

  public void setwService(WikiService wService) {
    this.wService = wService;
  }

  public T getWiki(String wikiOwner, boolean hasAdminPermission) {
    T wiki = contains(wikiOwner);
    if (wiki != null)
      return wiki;
    else {
      if(hasAdminPermission){
        wiki = addWiki(wikiOwner);
        if(wiki != null) ((WikiImpl)wiki).initTemplate();
      }
      return wiki;
    }
  }

  public Collection<T> getAllWikis() {
    return getWikis();
  }
  
  /**
   * Checks if current WikiContainer contains the wiki with specified wiki owner
   * @param wikiOwner the wiki owner
   * @return the wiki if it exists, otherwise null
   */
  public T contains(String wikiOwner) {
    wikiOwner = validateWikiOwner(wikiOwner);
    if (wikiOwner == null) {
      return null;
    }
    return getWikiObject(wikiOwner, false);
  }
  
  /**
   * Gets the wiki in current WikiContainer by specified wiki owner
   * @param wikiOwner the wiki owner
   * @param createIfNonExist if true, create the wiki when it does not exist
   * @return the wiki object
   */
  abstract protected T getWikiObject(String wikiOwner, boolean createIfNonExist);
  
  public void initDefaultPermisisonForWiki(Wiki wiki) {
    WikiService wikiService = getwService(); 
    List<String> permissions;
    try {
      permissions = wikiService.getWikiDefaultPermissions(wiki.getType(), wiki.getOwner());
      wiki.setWikiPermissions(permissions);
      wiki.setDefaultPermissionsInited(true);
    } catch (Exception e) {
      log.warn(String.format("Can not initialize the permission for wiki [type: %s, owner: %s]", wiki.getType(), wiki.getOwner()), e);
    }
  }

}
