/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.wiki.jpa;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.jpa.dao.PageAttachmentDAO;
import org.exoplatform.wiki.jpa.mock.MockIndexingService;
import org.exoplatform.wiki.jpa.search.PageIndexingListener;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.service.WikiService;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/components-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/page-indexing-configuration.xml"),
})
public class PageIndexingListenerTest extends BaseTest {
  private WikiService wikiService;
  private MockIndexingService indexingService;

  @Override
  public void setUp() {
    super.setUp();

    wikiService = getService(WikiService.class);
    indexingService = new MockIndexingService();
    wikiService.addComponentPlugin(new PageIndexingListener(getService(PageAttachmentDAO.class), indexingService));

  }

  public void testIndex() {

  }

  public void _testReindexingDraftPage() throws Exception {

    ConversationState state = new ConversationState(new Identity("root"));
    ConversationState.setCurrent(state);

    Wiki wiki = wikiService.createWiki(PortalConfig.USER_TYPE, "root");
    assertNotNull(wiki);
    Page page = new Page();
    page.setTitle("test wiki page");
    wikiService.createPage(wiki, wiki.getWikiHome().getName(), page);

    page = wikiService.getPageOfWikiByName(PortalConfig.USER_TYPE, "root", page.getName());
    page.setContent("Test wiki content");
    wikiService.updatePage(page, null);

    assertEquals(1, indexingService.getCount("reindex"));

    DraftPage draft = new DraftPage();
    draft.setContent("draft content");
    wikiService.createDraftForExistPage(draft, page, "1", System.currentTimeMillis());

    draft = wikiService.getDraftOfPage(page);
    draft.setContent("new draf content");
    wikiService.updatePage(draft, null);

    assertEquals(1, indexingService.getCount("reindex"));

    wikiService.removeDraft(draft.getName());
    wikiService.deletePage(PortalConfig.USER_TYPE, "root", page.getName());
  }
}
