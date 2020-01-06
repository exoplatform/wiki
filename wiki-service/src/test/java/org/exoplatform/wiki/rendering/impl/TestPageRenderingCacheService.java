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

  public void testRenderingCache() throws Exception {
    Wiki wiki = wikiService.createWiki(PortalConfig.PORTAL_TYPE, "cladic");
    Page cladicHome = wiki.getWikiHome();
    cladicHome.setContent("Sample content");
    wikiService.updatePage(cladicHome, null);
    wikiService.getPageRenderedContent(cladicHome, Syntax.XHTML_1_0.toIdString());
    assertEquals(1, ((WikiServiceImpl)wikiService).getRenderingCache().getCacheSize());
    assertEquals(0, ((WikiServiceImpl)wikiService).getRenderingCache().getCacheHit());

    Wiki wikiAme = wikiService.createWiki(PortalConfig.PORTAL_TYPE, "ame");
    Page ameHome = wikiAme.getWikiHome();
    ameHome.setContent("Sample content");
    wikiService.updatePage(ameHome, null);
    wikiService.getPageRenderedContent(ameHome, Syntax.XHTML_1_0.toIdString());
    assertEquals(2, ((WikiServiceImpl)wikiService).getRenderingCache().getCacheSize());
    assertEquals(0, ((WikiServiceImpl)wikiService).getRenderingCache().getCacheHit());

    wikiService.getPageRenderedContent(cladicHome, Syntax.XHTML_1_0.toIdString());
    assertEquals(1, ((WikiServiceImpl)wikiService).getRenderingCache().getCacheHit());

    wikiService.getPageRenderedContent(ameHome, Syntax.XHTML_1_0.toIdString());
    assertEquals(2, ((WikiServiceImpl)wikiService).getRenderingCache().getCacheHit());

    // Change the content of page
    cladicHome.setContent("Another text");
    wikiService.updatePage(cladicHome, null);
    assertEquals(1, ((WikiServiceImpl)wikiService).getRenderingCache().getCacheSize());
    wikiService.getPageRenderedContent(cladicHome, Syntax.XHTML_1_0.toIdString());
    assertEquals(2, ((WikiServiceImpl)wikiService).getRenderingCache().getCacheHit());
  }

  public void testRenderingWithUncachedMacro() throws Exception {
    WikiServiceImpl wikiServiceImpl = (WikiServiceImpl) wikiService;
    wikiServiceImpl.getUncachedMacroes().add("warning");
    try {
      Wiki wiki = wikiServiceImpl.createWiki(PortalConfig.PORTAL_TYPE, "testcache");
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
