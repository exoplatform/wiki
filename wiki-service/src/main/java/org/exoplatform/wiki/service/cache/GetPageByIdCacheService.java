package org.exoplatform.wiki.service.cache;

import org.exoplatform.wiki.mow.api.Page;

/**
 * Get wiki page by Id from cache.
 *
 * User: dongpd
 * Date: 10/17/13
 * Time: 11:07 AM
 */
public interface GetPageByIdCacheService {

  /**
   * Gets a wiki page by a given Id from cache.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page.
   * @return The wiki page if cache contains it. Otherwise, it is "null".
   * @throws Exception
   */
  public Page get(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Put page item to cache
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page.
   * @param page wiki page
   * @return  wiki page
   */
  public Page putInCache(String wikiType, String wikiOwner, String pageId, Page page) throws Exception;

  /**
   * Clear cache.
   */
  public void clearCache();
}
