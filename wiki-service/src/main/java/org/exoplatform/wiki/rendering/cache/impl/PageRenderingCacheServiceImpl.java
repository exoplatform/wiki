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
package org.exoplatform.wiki.rendering.cache.impl;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.rendering.cache.*;
import org.exoplatform.wiki.service.PermissionType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.xwiki.rendering.syntax.Syntax;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PageRenderingCacheServiceImpl implements PageRenderingCacheService {
  
  public static final String              CACHE_NAME = "wiki.PageRenderingCache";

  public static final String              ATT_CACHE_NAME = "wiki.PageRenderingCache.attachment";
  
  public static final String              UUID_CACHE_NAME = "wiki.PageRenderingCache.pageUuid";
  
  private static final Log                LOG = ExoLogger.getLogger(PageRenderingCacheService.class);

  private RenderingService                renderingService;

  private WikiService                     wikiService;

  private ExoCache<Integer, MarkupData> renderingCache;
  private ExoCache<Integer, AttachmentCountData> attachmentCountCache;
  private ExoCache<Integer, String> uuidCache;
  
  private Map<WikiPageParams, List<WikiPageParams>> pageLinksMap = new ConcurrentHashMap<WikiPageParams, List<WikiPageParams>>();
  
  private Set<String> uncachedMacroes = new HashSet<String>();

  /**
   * Initialize rendering cache service 
   * @param renderingService the rendering service
   */
  public PageRenderingCacheServiceImpl(RenderingService renderingService, WikiService wikiService, CacheService cacheService) {
    this.renderingService = renderingService;
    this.wikiService = wikiService;
    this.renderingCache = cacheService.getCacheInstance(CACHE_NAME);
    this.attachmentCountCache = cacheService.getCacheInstance(ATT_CACHE_NAME);
    this.uuidCache = cacheService.getCacheInstance(UUID_CACHE_NAME);
  }
  
  @Override
  public String getRenderedContent(Page page, String targetSyntax) {
    String renderedContent = StringUtils.EMPTY;
    try {
      boolean supportSectionEdit = wikiService.hasPermissionOnPage(page, PermissionType.EDITPAGE, ConversationState.getCurrent().getIdentity());
      MarkupKey key = new MarkupKey(new WikiPageParams(page.getWikiType(), page.getWikiOwner(), page.getName()), page.getSyntax(), targetSyntax, supportSectionEdit);
      //get content from cache only when page is not uncached mixin
      // TODO add uncached property to page object
      /*
      if (page.getUncachedMixin() == null) {
        MarkupData cachedData = renderingCache.get(new Integer(key.hashCode()));
        if (cachedData != null) {
          return cachedData.build();
        }
      }
      */
      String markup = page.getContent().getText();
      renderedContent = renderingService.render(markup, page.getSyntax(), targetSyntax, supportSectionEdit);
      renderingCache.put(new Integer(key.hashCode()), new MarkupData(renderedContent));
    } catch (Exception e) {
      LOG.error(String.format("Failed to get rendered content of page [%s:%s:%s] in syntax %s", page.getWikiType(), page.getWikiOwner(), page.getName(), targetSyntax), e);
    }
    return renderedContent;
  }
  
  @Override
  public Page getPageByParams(WikiPageParams param) {
    Page page = null;
    try {
      MarkupKey key = new MarkupKey(param, 
                                    "", Syntax.XHTML_1_0.toIdString(), true);
      String uuid  = uuidCache.get(new Integer(key.hashCode()));
      if (uuid != null) {
        page = wikiService.getPageById(uuid);
        if (page != null) {
          return page;
        }
      }
      
      page = wikiService.getPageOfWikiByName(param.getType(), param.getOwner(), param.getPageId());
      if (page != null) {
        uuid = page.getId();
        uuidCache.put(new Integer(key.hashCode()), uuid);
      }
    } catch (Exception e) {
      LOG.error(String.format("Failed to get page [%s:%s:%s]", 
                              param.getType(), param.getOwner(), param.getPageId()), e);
    }
    return page;
  }

  @Override
  public int getAttachmentCount(Page page) {
    int attachmentCount = 0;
    Wiki wiki;
    try {
      wiki = wikiService.getWikiById(page.getWikiId());
      boolean supportSectionEdit = wikiService.hasPermissionOnPage(page, PermissionType.EDITPAGE, ConversationState.getCurrent().getIdentity());
//      PageImpl page = (PageImpl) wikiService.getPageOfWikiByName(param.getType(), param.getOwner(), param.getPageId());
      MarkupKey key = new MarkupKey(new WikiPageParams(wiki.getType(), wiki.getOwner(), page.getName()), 
                                    page.getSyntax(), Syntax.XHTML_1_0.toIdString(), supportSectionEdit);
      AttachmentCountData cachedData = attachmentCountCache.get(new Integer(key.hashCode()));
      if (cachedData != null) {
        return cachedData.build();
      }
      // TODO ???
      //attachmentCount = page.getAttachmentsExcludeContent().size();
      attachmentCount = 0;
      attachmentCountCache.put(new Integer(key.hashCode()), new AttachmentCountData(attachmentCount));
    } catch (Exception e) {
      LOG.error(String.format("Failed to get attachment count of page [%s]", page.getName()), e);
    }
    return attachmentCount;
  }

  @Override
  public final ExoCache<Integer, MarkupData> getRenderingCache() {
    return renderingCache;
  }
  
  @Override
  public Map<WikiPageParams, List<WikiPageParams>> getPageLinksMap() {
    return pageLinksMap;
  }

  @Override
  public void addPageLink(WikiPageParams param, WikiPageParams entity) {
    List<WikiPageParams> linkParams = this.pageLinksMap.get(entity);
    if (linkParams == null) {
      linkParams = new ArrayList<WikiPageParams>();
      this.pageLinksMap.put(entity, linkParams);
    }
    linkParams.add(param);
  }

  @Override
  public void invalidateCache(WikiPageParams param) {
    List<WikiPageParams> linkedPages = pageLinksMap.get(param);
    if (linkedPages == null) {
      linkedPages = new ArrayList<WikiPageParams>();
    } else {
      linkedPages = new ArrayList<WikiPageParams>(linkedPages);
    }
    linkedPages.add(param);
    
    for (WikiPageParams wikiPageParams : linkedPages) {
      try {
        MarkupKey key = new MarkupKey(wikiPageParams, Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false);
        renderingCache.remove(new Integer(key.hashCode()));
        key.setSupportSectionEdit(true);
        renderingCache.remove(new Integer(key.hashCode()));
        
        key = new MarkupKey(wikiPageParams,Syntax.XHTML_1_0.toIdString(), Syntax.XWIKI_2_0.toIdString(), false);
        renderingCache.remove(new Integer(key.hashCode()));
        key.setSupportSectionEdit(true);
        renderingCache.remove(new Integer(key.hashCode()));
      } catch (Exception e) {
        LOG.warn(String.format("Failed to invalidate cache of page [%s:%s:%s]", wikiPageParams.getType(), wikiPageParams.getOwner(), wikiPageParams.getPageId()));
      }
    }
  }

  @Override
  public void addUnCachedMacro(UnCachedMacroPlugin plugin) {
    for (String name : plugin.getUncachedMacroes())
      uncachedMacroes.add(name);
  }

  @Override
  public Set<String> getUncachedMacroes() {
    return new HashSet<String>(uncachedMacroes);
  }
  
  public void invalidateUUIDCache(WikiPageParams param) {
    MarkupKey key = new MarkupKey(param, 
                                  "", Syntax.XHTML_1_0.toIdString(), true);
    uuidCache.remove(new Integer(key.hashCode()));
  }

  @Override
  public void invalidateAttachmentCache(WikiPageParams param) {
    List<WikiPageParams> linkedPages = pageLinksMap.get(param);
    if (linkedPages == null) {
      linkedPages = new ArrayList<WikiPageParams>();
    } else {
      linkedPages = new ArrayList<WikiPageParams>(linkedPages);
    }
    linkedPages.add(param);
    
    for (WikiPageParams wikiPageParams : linkedPages) {
      try {
        MarkupKey key = new MarkupKey(wikiPageParams, Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false);
        attachmentCountCache.remove(new Integer(key.hashCode()));
        key.setSupportSectionEdit(true);
        attachmentCountCache.remove(new Integer(key.hashCode()));
        
        key = new MarkupKey(wikiPageParams,Syntax.XHTML_1_0.toIdString(), Syntax.XWIKI_2_0.toIdString(), false);
        attachmentCountCache.remove(new Integer(key.hashCode()));
        key.setSupportSectionEdit(true);
        attachmentCountCache.remove(new Integer(key.hashCode()));
      } catch (Exception e) {
        LOG.warn(String.format("Failed to invalidate cache of page [%s:%s:%s]", wikiPageParams.getType(), wikiPageParams.getOwner(), wikiPageParams.getPageId()));
      }
    }
  }
}
