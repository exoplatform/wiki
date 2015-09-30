/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.rendering.cache;

import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.WikiPageParams;

import java.util.Map;
import java.util.Set;

/**
 * Manages the Rendering Cache of wiki pages.
 * The cache stores all HTML markups from the wiki page markups. Therefore, it saves much time in rendering markups, especially with a long content page.
 * Generally, its workflow is as follows:
 * <ul>
 * <li>Open a page, does the page HTML markup exist in the cache?
 * <ul>
 * <li>Yes = Reuses this one. </li>
 * <li> No = Renders the page and adds the page HTML markup to the cache.</li>
 * </ul>
 * </li>
 * <li>Editing a page  = Invalidates the page HTML markup in the cache.</li>
 * </ul>
 *
 * @LevelAPI Experimental
 */
public interface PageRenderingCacheService {
  
  /**
   * Gets the rendered content of a wiki page.
   * @param page The wiki page.
   * @param targetSyntax The syntax to be displayed.
   * @return The rendered content.
   */
  public String getRenderedContent(Page page, String targetSyntax);
  
  /**
   * Gets the wiki page object by params
   * @param param the wiki page param
   * @return the wiki page
   */
  public Page getPageByParams(WikiPageParams param);
  
  /**
   * Get number of attachment of a wiki page
   * @param param the parameter to specify the wiki page
   * @param targetSyntax the syntax to be display
   * @return the attachment size
   */
  public int getAttachmentCount(Page page);
  
  /**
   * Gets the rendering cache.
   * @return The rendering cache.
   */
  public ExoCache<Integer, MarkupData> getRenderingCache();
  
  /**
   * Returns a collection of connections of a wiki page. In details, a connection is
   * built if there is a link to another page in the page content.
   * 
   * @return The map of connections.
   */
  public Map<WikiPageParams, java.util.List<WikiPageParams>> getPageLinksMap();
  
  /**
   * Adds a link between two pages.
   * @param param The identity parameter of the wiki page to add.
   * @param entity The identity parameter of the wiki page to be added.
   */
  public void addPageLink(WikiPageParams param, WikiPageParams entity);
  
  /**
   * Invalidates all cache entries linking to a page in case this page is removed, changed or renamed.
   * @param param The parameter which specifies the wiki page identity.
   */
  public void invalidateCache(WikiPageParams param);
  
  /**
   * adds plugin containing uncached macro list
   * @param plugin the plugin
   */
  public void addUnCachedMacro(UnCachedMacroPlugin plugin);
  
  /**
   * gets the list of uncached macroes
   * @return the uncached macroes
   */
  public Set<String> getUncachedMacroes();
  
  /**
   * Invalidates the cache entry containing given wiki page param
   * @param param
   */
  public void invalidateUUIDCache(WikiPageParams param);
  
  /**
   * Invalidate cache storing attachment size of a wiki page
   * @param param specify identity of a page
   */
  public void invalidateAttachmentCache(WikiPageParams param);
  
}
