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

import java.util.Map;

import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.service.WikiPageParams;

/**
 * This service Manage the Rendering Cache of wiki pages.
 * The cache storage store all html markup from page wiki markup. Therefore, it saves much time in rendering markup, especially with long content page.
 * Generally, its workflow as:
 * Open a page, does this page's html markup exist in the cache ?
 * - Yes = reuse this one.
 * - No = Render the page and add to the cache.
 * Editing a page  = invalidating the cache page.
 *
 * @LevelAPI Experimental
 */
public interface PageRenderingCacheService {
  
  /**
   * Get rendered content of a wiki page
   * @param param the parameter to specify the wiki page
   * @param targetSyntax the syntax to be display
   * @return the rendered content
   */
  public String getRenderedContent(WikiPageParams param, String targetSyntax);
  
  /**
   * Get the rendering cache
   * @return the rendering cache
   */
  public ExoCache<Integer, MarkupData> getRenderingCache();
  
  /**
   * Return the collection of connections of page. In details, a connections is
   * built if in content of a page, there is a link to another page
   * 
   * @return the map of connection
   */
  public Map<WikiPageParams, java.util.List<WikiPageParams>> getPageLinksMap();
  
  /**
   * Record a link between two pages
   * @param param identity parameter of a page to add
   * @param entity identity parameter of page to be added
   */
  public void addPageLink(WikiPageParams param, WikiPageParams entity);
  
  /**
   * Invalidate all cache entries link to a page in case this page is removed, changed or renamed...
   * @param param specify identity of a page
   */
  public void invalidateCache(WikiPageParams param);
  
  /**
   * Invalidate cache storing attachment size of a wiki page
   * @param param specify identity of a page
   */
  public void invalidateAttachmentCache(WikiPageParams param);
  
  /**
   * Get number of attachment of a wiki page
   * @param param the parameter to specify the wiki page
   * @param targetSyntax the syntax to be display
   * @return the attachment size
   */
  public int getAttachmentCount(PageImpl page);
  
}
