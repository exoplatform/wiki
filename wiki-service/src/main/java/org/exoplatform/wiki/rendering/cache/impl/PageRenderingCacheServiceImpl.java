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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.rendering.cache.AttachmentCountData;
import org.exoplatform.wiki.rendering.cache.MarkupData;
import org.exoplatform.wiki.rendering.cache.MarkupKey;
import org.exoplatform.wiki.rendering.cache.PageRenderingCacheService;
import org.exoplatform.wiki.service.PermissionType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.xwiki.rendering.syntax.Syntax;

public class PageRenderingCacheServiceImpl implements PageRenderingCacheService {
  
  public static final String              CACHE_NAME = "wiki.PageRenderingCache";
  public static final String              ATT_CACHE_NAME = "wiki.PageRenderingCache.attachment";
  
  private static final Log                LOG = ExoLogger.getLogger(PageRenderingCacheService.class);

  private RenderingService                renderingService;

  private WikiService                     wikiService;

  private ExoCache<Integer, MarkupData> renderingCache;
  private ExoCache<Integer, AttachmentCountData> attachmentCountCache;
  
  private Map<WikiPageParams, List<WikiPageParams>> pageLinksMap = new ConcurrentHashMap<WikiPageParams, List<WikiPageParams>>();

  /**
   * Initialize rendering cache service 
   * @param renderingService the rendering service
   */
  public PageRenderingCacheServiceImpl(RenderingService renderingService, WikiService wikiService, CacheService cacheService) {
    this.renderingService = renderingService;
    this.wikiService = wikiService;
    this.renderingCache = cacheService.getCacheInstance(CACHE_NAME);
    this.attachmentCountCache = cacheService.getCacheInstance(ATT_CACHE_NAME);
        
  }
  
  @Override
  public String getRenderedContent(WikiPageParams param, String targetSyntax) {
    String renderedContent = StringUtils.EMPTY;
    try {
      PageImpl page = (PageImpl) wikiService.getPageById(param.getType(), param.getOwner(), param.getPageId());
      boolean supportSectionEdit = page.hasPermission(PermissionType.EDITPAGE);
      MarkupKey key = new MarkupKey(new WikiPageParams(param.getType(), param.getOwner(), param.getPageId()), page.getSyntax(), targetSyntax, supportSectionEdit);
      MarkupData cachedData = renderingCache.get(new Integer(key.hashCode()));
      if (cachedData != null) {
        return cachedData.build();
      }
      String markup = page.getContent().getText();
      renderedContent = renderingService.render(markup, page.getSyntax(), targetSyntax, supportSectionEdit);
      renderingCache.put(new Integer(key.hashCode()), new MarkupData(renderedContent));
    } catch (Exception e) {
      LOG.error(String.format("Failed to get rendered content of page [%s:%s:%s] in syntax %s", param.getType(), param.getOwner(), param.getPageId(), targetSyntax), e);
    }
    return renderedContent;
  }

  @Override
  public int getAttachmentCount(PageImpl page) {
    int attachmentCount = 0;
    Wiki wiki = page.getWiki();
    try {
      boolean supportSectionEdit = page.hasPermission(PermissionType.EDITPAGE);
//      PageImpl page = (PageImpl) wikiService.getPageById(param.getType(), param.getOwner(), param.getPageId());
      MarkupKey key = new MarkupKey(new WikiPageParams(wiki.getType(), wiki.getOwner(), page.getName()), 
                                    page.getSyntax(), Syntax.XHTML_1_0.toIdString(), supportSectionEdit);
      AttachmentCountData cachedData = attachmentCountCache.get(new Integer(key.hashCode()));
      if (cachedData != null) {
        System.out.println("Cache hit!");
        return cachedData.build();
      }
      attachmentCount = page.getAttachmentsExcludeContent().size();
      System.out.println("Put to cache!");
      attachmentCountCache.put(new Integer(key.hashCode()), new AttachmentCountData(attachmentCount));
    } catch (Exception e) {
      LOG.error(String.format("Failed to get attachment count of page [%s:%s:%s]", 
                              wiki.getType(), wiki.getOwner(), page.getName()), e);
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
