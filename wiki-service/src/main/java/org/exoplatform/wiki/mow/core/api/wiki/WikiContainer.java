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

import org.chromattic.api.RelationshipType;
import org.chromattic.api.annotations.Create;
import org.chromattic.api.annotations.MappedBy;
import org.chromattic.api.annotations.OneToMany;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.core.api.MOWService;

import java.util.Collection;
import java.util.List;

/**
 * @version $Revision$
 */
public abstract class WikiContainer<T extends WikiImpl> {
  
  private static final Log      log               = ExoLogger.getLogger(WikiContainer.class);

  protected MOWService mowService;

  public void setMOWService(MOWService mowService) {
    this.mowService = mowService;
  }

  @OneToMany(type = RelationshipType.REFERENCE)
  @MappedBy(WikiNodeType.Definition.WIKI_CONTAINER_REFERENCE)
  public abstract Collection<T> getWikis();

  /*
   * @OneToOne public abstract WikiStoreImpl getMultiWiki();
   */

  public abstract T addWiki(Wiki wiki) throws WikiException;

  public abstract T createWiki(Wiki wiki) throws WikiException;

  @Create
  public abstract T createWiki();  
  
  protected String validateWikiOwner(String wikiOwner){
    return wikiOwner;
  }

  public T getWiki(String wikiOwner, boolean hasAdminPermission) {
    return contains(wikiOwner);
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
    return getWikiObject(wikiOwner);
  }
  
  /**
   * Gets the wiki in current WikiContainer by specified wiki owner
   * @param wikiOwner the wiki owner
   * @return the wiki object
   */
  abstract protected T getWikiObject(String wikiOwner);

}
