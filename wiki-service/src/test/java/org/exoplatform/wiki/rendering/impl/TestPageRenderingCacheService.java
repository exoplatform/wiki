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
package org.exoplatform.wiki.rendering.impl;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.service.WikiContext;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.impl.WikiServiceImpl;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.syntax.Syntax;

public final class TestPageRenderingCacheService extends AbstractRenderingTestCase {

  private WikiService               wikiService;
  
  /* (non-Javadoc)
   * @see org.exoplatform.wiki.rendering.impl.AbstractRenderingTestCase#setUp()
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    wikiService = getContainer().getComponentInstanceOfType(WikiService.class);
  }

  public void testShouldCacheSizeIncreaseWhenPagesRendered() throws Exception {
    // Given
    Wiki wiki = getOrCreateWiki(wikiService, PortalConfig.PORTAL_TYPE, "rendering1");
    Page homePage = wiki.getWikiHome();
    homePage.setContent("Sample content");
    homePage.setSyntax(Syntax.XHTML_1_0.toIdString());
    wikiService.updatePage(homePage, null);
    Page childrenPage = new Page("Page1", "Page 1");
    childrenPage.setContent("Sample content");
    childrenPage.setSyntax(Syntax.XHTML_1_0.toIdString());
    childrenPage = wikiService.createPage(wiki, "WikiHome", childrenPage);
    int initialCacheHit = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheHit();

    // When
    wikiService.getPageRenderedContent(homePage, Syntax.XHTML_1_0.toIdString());
    int cacheSize1 = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheSize();
    int cacheHit1 = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheHit();
    wikiService.getPageRenderedContent(childrenPage, Syntax.XHTML_1_0.toIdString());
    int cacheSize2 = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheSize();
    int cacheHit2 = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheHit();

    // Then
    assertEquals(1, cacheSize1);
    assertEquals(0, cacheHit1 - initialCacheHit);
    assertEquals(2, cacheSize2);
    assertEquals(0, cacheHit2 - initialCacheHit);
  }


  public void testShouldHitCacheWhenSamePageRenderedTwice() throws Exception {
    // Given
    Wiki wiki = getOrCreateWiki(wikiService, PortalConfig.PORTAL_TYPE, "rendering2");
    Page homePage = wiki.getWikiHome();
    homePage.setContent("Sample content");
    homePage.setSyntax(Syntax.XHTML_1_0.toIdString());
    wikiService.updatePage(homePage, null);
    int initialCacheHit = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheHit();

    // When
    wikiService.getPageRenderedContent(homePage, Syntax.XHTML_1_0.toIdString());
    int cacheSize1 = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheSize();
    int cacheHit1 = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheHit();
    wikiService.getPageRenderedContent(homePage, Syntax.XHTML_1_0.toIdString());
    int cacheSize2 = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheSize();
    int cacheHit2 = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheHit();

    // Then
    assertEquals(1, cacheSize1);
    assertEquals(0, cacheHit1 - initialCacheHit);
    assertEquals(1, cacheSize2);
    assertEquals(1, cacheHit2 - initialCacheHit);
  }

  public void testShouldNotHitCacheWhenPageUpdated() throws Exception {
    // Given
    Wiki wiki = getOrCreateWiki(wikiService, PortalConfig.PORTAL_TYPE, "rendering3");
    Page homePage = wiki.getWikiHome();
    homePage.setContent("Sample content");
    homePage.setSyntax(Syntax.XHTML_1_0.toIdString());
    wikiService.updatePage(homePage, null);
    int initialCacheHit = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheHit();

    // When
    wikiService.getPageRenderedContent(homePage, Syntax.XHTML_1_0.toIdString());
    int cacheSize1 = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheSize();
    int cacheHit1 = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheHit();
    homePage.setContent("Another text");
    wikiService.updatePage(homePage, null);
    wikiService.getPageRenderedContent(homePage, Syntax.XHTML_1_0.toIdString());
    int cacheSize2 = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheSize();
    int cacheHit2 = ((WikiServiceImpl) wikiService).getRenderingCache().getCacheHit();

    // Then
    assertEquals(1, cacheSize1);
    assertEquals(0, cacheHit1 - initialCacheHit);
    assertEquals(1, cacheSize2);
    assertEquals(0, cacheHit2 - initialCacheHit);
  }

  public void testRenderingWithUncachedMacro() throws Exception {
    WikiServiceImpl wikiServiceImpl = (WikiServiceImpl) wikiService;
    wikiServiceImpl.getUncachedMacroes().add("warning");
    try {
      Wiki wiki = getOrCreateWiki(wikiServiceImpl, PortalConfig.PORTAL_TYPE, "testcache");
      Page testcacheHome = wiki.getWikiHome();
      testcacheHome.setContent("{{warning}}Sample content{{/warning}}");
      wikiService.updatePage(testcacheHome, null);
      wikiService.getPageRenderedContent(testcacheHome, Syntax.XHTML_1_0.toIdString());
      assertEquals(0, wikiServiceImpl.getRenderingCache().getCacheSize());
    } finally {
      wikiServiceImpl.getUncachedMacroes().remove("warning");
    }
  }

  @Override
  protected void tearDown() throws Exception {
    ((WikiServiceImpl)wikiService).getRenderingCache().clearCache();
    super.tearDown();
  }
  
  private void setupWikiContext(WikiPageParams params) throws ComponentLookupException, ComponentRepositoryException {
    Execution ec = renderingService.getExecution();
    ec.setContext(new ExecutionContext());
    WikiContext wikiContext = new WikiContext();
    wikiContext.setType(params.getType());
    wikiContext.setOwner(params.getOwner());
    wikiContext.setPageName(params.getPageName());
    ec.getContext().setProperty(WikiContext.WIKICONTEXT, wikiContext);
  }

}
