package org.exoplatform.wiki.service;

import java.util.ArrayList;
import java.util.Collection;

import org.chromattic.ext.ntdef.Resource;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.wiki.mow.core.api.AbstractMOWTestcase;
import org.exoplatform.wiki.mow.core.api.wiki.AttachmentImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.service.impl.WikiSearchServiceConnector;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

public class TestWikiSearchServiceConnector extends AbstractMOWTestcase {

  private WikiService                wService;

  private WikiSearchServiceConnector searchServiceConnector;

  public void setUp() throws Exception {
    super.setUp();
    wService = (WikiService) container.getComponentInstanceOfType(WikiService.class);
    searchServiceConnector = (WikiSearchServiceConnector) container.getComponentInstanceOfType(WikiSearchServiceConnector.class);
  }

  public void testWikiService() throws Exception {
    assertNotNull(wService);
  }

  public void testWikiSearchService() throws Exception {
    assertNotNull(searchServiceConnector);
  }
    
  public void testUnifieldSearch() throws Exception {
    PageImpl page1 = (PageImpl) wService.createPage(PortalConfig.PORTAL_TYPE,
                                                    "intranet",
                                                    "Bella vita",
                                                    "WikiHome");
    page1.getContent().setText("Bella vita");
    page1.setURL("/portal/intranet/wiki/Bella_vita");
    assertNotNull(wService.getPageById(PortalConfig.PORTAL_TYPE, "intranet", "Bella_vita"));

    AttachmentImpl attachment1 = page1.createAttachment("Bella.txt",
                                                        Resource.createPlainText("Bella vita in content of attachment"));
    attachment1.setCreator("you");
    assertEquals(attachment1.getName(), "Bella.txt");
    assertNotNull(attachment1.getContentResource());
    attachment1.setContentResource(Resource.createPlainText("Bella vita in content of attachment"));
    // Advance search in content
    WikiSearchData data = new WikiSearchData(null, "Bella", null, null);
    PageList<SearchResult> result = wService.search(data);
    assertEquals(2, result.getAll().size());
    // Unified Search
    Collection<String> sites = new ArrayList<String>();
    sites.add("intranet");
    org.exoplatform.commons.api.search.data.SearchContext context = new org.exoplatform.commons.api.search.data.SearchContext(new Router(new ControllerDescriptor()),
                                                                                                                              "intranet");
    Collection<org.exoplatform.commons.api.search.data.SearchResult> searchResult = searchServiceConnector.search(context,
                                                                                                                  "bella~0.5",
                                                                                                                  sites,
                                                                                                                  0,
                                                                                                                  5,
                                                                                                                  "relevancy",
                                                                                                                  "desc");
    assertEquals(searchResult.size(), 1);
  }
}
