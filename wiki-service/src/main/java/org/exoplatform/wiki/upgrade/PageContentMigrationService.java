package org.exoplatform.wiki.upgrade;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiService;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.rendering.converter.ConversionException;
import org.xwiki.rendering.syntax.Syntax;

public class PageContentMigrationService {

  private static final Log LOG = ExoLogger.getLogger(PageContentMigrationService.class);

  private RenderingService renderingService;

  private WikiService wikiService;

  public PageContentMigrationService(RenderingService renderingService, WikiService wikiService) {
    this.renderingService = renderingService;
    this.wikiService = wikiService;
  }

  /**
   * Migrate page from XWiki 2.0 syntax to HTML
   * @param page Page to migrate
   * @throws ComponentLookupException
   * @throws ConversionException
   * @throws WikiException
   */
  public void migratePage(Page page) throws ComponentLookupException, ConversionException, WikiException {
    if(Syntax.XWIKI_2_0.toIdString().equals(page.getSyntax())) {
      LOG.info("Convert wiki page " + page.getId() + " to HTML");
      String markup = page.getContent();
      markup = renderingService.render(markup, page.getSyntax(), Syntax.XHTML_1_0.toIdString(), false);
      page.setContent(markup);
      page.setSyntax(Syntax.XHTML_1_0.toIdString());
      wikiService.updatePage(page, PageUpdateType.EDIT_PAGE_CONTENT);
    }
  }
}
