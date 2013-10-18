package org.exoplatform.wiki.service.cache.impl;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.cache.GetPageByIdCacheService;

import java.util.HashMap;
import java.util.Map;

public class GetPageByIdCacheServiceImpl implements GetPageByIdCacheService {

  private static ThreadLocal<Map<String, Page>> pageKeeper = new ThreadLocal<Map<String, Page>>() {
    @Override
    protected Map<String, Page> initialValue() {
      return new HashMap<String, Page>();
    }
  };

  @Override
  public Page get(String wikiType, String wikiOwner, String pageId) throws Exception {
    Map<String, Page> pageCache = pageKeeper.get();
    String pageKey = createKeyMap(wikiType, wikiOwner, pageId);
    return pageCache.get(pageKey);
  }

  @Override
  public Page putInCache(String wikiType, String wikiOwner, String pageId, Page page) {
    if (WebuiRequestContext.getCurrentInstance() != null ) {
      Map<String, Page> pageCache = pageKeeper.get();
      String pageKey = createKeyMap(wikiType, wikiOwner, pageId);
      pageCache.put(pageKey, page);
    }
    return page;
  }

  @Override
  public void clearCache() {
    Map<String, Page> pageCache = pageKeeper.get();
    pageCache.clear();
  }

  /**
   * Create key for map cache.
   *
   * @param wikiType Wiki Type
   * @param wikiOwner Wiki Owner
   * @param pageId Page ID
   * @return key
   */
  private String createKeyMap(String wikiType, String wikiOwner, String pageId) {
    StringBuilder keyBuilder = new StringBuilder();
    keyBuilder.append(wikiType);
    keyBuilder.append(wikiOwner);
    keyBuilder.append(pageId);
    return keyBuilder.toString();
  }
}
