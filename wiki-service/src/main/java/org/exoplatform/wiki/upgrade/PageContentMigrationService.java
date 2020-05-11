package org.exoplatform.wiki.upgrade;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.EntityConverter;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiService;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.rendering.converter.ConversionException;
import org.xwiki.rendering.syntax.Syntax;

import javax.persistence.EntityManager;
import java.util.List;

public class PageContentMigrationService {

  private static final Log LOG = ExoLogger.getLogger(PageContentMigrationService.class);

  private RenderingService renderingService;

  private WikiService wikiService;

  private EntityManagerService entityManagerService;

  private PageDAO pageDAO;

  public PageContentMigrationService(RenderingService renderingService, WikiService wikiService, EntityManagerService entityManagerService, PageDAO pageDAO) {
    this.renderingService = renderingService;
    this.wikiService = wikiService;
    this.entityManagerService = entityManagerService;
    this.pageDAO = pageDAO;
  }

  /**
   * Migrate page from XWiki 2.0 syntax to HTML
   * @param page Page to migrate
   * @throws ComponentLookupException
   * @throws ConversionException
   * @throws WikiException
   */
  public void migratePage(Page page) throws Exception {
    if(Syntax.XWIKI_2_0.toIdString().equals(page.getSyntax())) {
      LOG.info("Convert wiki page " + page.getId() + " to HTML");
      String markup = page.getContent();
      markup = renderingService.render(markup, page.getSyntax(), Syntax.XHTML_1_0.toIdString(), false);
      page.setContent(markup);
      page.setSyntax(Syntax.XHTML_1_0.toIdString());
      wikiService.updatePage(page, null);
    }
  }

  /**
   * Migrate all pages from XWiki 2.0 syntax to HTML
   */
  public void migrateAllPages() {
    int offset = 0;
    int batchSize = 100;

    int nbMigratedPages = 0;
    int nbMigrationErrors = 0;

    try {
      entityManagerService.startRequest(ExoContainerContext.getCurrentContainer());

      Long nbPagesToMigrate = pageDAO.countPagesBySyntax(Syntax.XWIKI_2_0.toIdString());
      if (nbPagesToMigrate == null || nbPagesToMigrate == 0) {
        LOG.info("Wiki page syntax migration - No Wiki page to migrate from XWiki syntax to HTML");
        return;
      }

      LOG.info("== Starting Wiki page syntax migration");
      LOG.info("Wiki page syntax migration - Number of pages = " + nbPagesToMigrate);

      List<PageEntity> pagesToMigrate;
      do {
        entityManagerService.endRequest(ExoContainerContext.getCurrentContainer());
        entityManagerService.startRequest(ExoContainerContext.getCurrentContainer());

        pagesToMigrate = pageDAO.findAllBySyntax(Syntax.XWIKI_2_0.toIdString(), offset, batchSize);
        for (PageEntity pageEntity : pagesToMigrate) {
          Page page = null;
          try {
            page = EntityConverter.convertPageEntityToPage(pageEntity);
            migratePage(page);
            nbMigratedPages++;
          } catch (Exception e) {
            LOG.error("Error while migrating wiki page " + (page != null ? page.getId() : "") + " to HTML", e);
            nbMigrationErrors++;
            pageEntity.setSyntax("ERROR");
            pageDAO.update(pageEntity);
          }
        }

        LOG.info("Wiki page syntax migration - Progress : " + nbMigratedPages + "/" + nbPagesToMigrate);

        offset += batchSize;
      } while (pagesToMigrate != null && !pagesToMigrate.isEmpty());

      LOG.info("== Wiki page syntax migration - Migration finished - Number of migrated pages = " + nbMigratedPages + ", number of errors = " + nbMigrationErrors);
    } catch (Exception e) {
      LOG.error("Error while migrating wiki pages fom XWiki syntax to HTML", e);
    } finally {
      entityManagerService.endRequest(ExoContainerContext.getCurrentContainer());
    }
  }
}
