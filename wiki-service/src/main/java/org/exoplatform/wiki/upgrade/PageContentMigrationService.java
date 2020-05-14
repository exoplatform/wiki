package org.exoplatform.wiki.upgrade;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.EntityConverter;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.dao.PageVersionDAO;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.PageVersionEntity;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.PageVersion;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.service.WikiContext;
import org.exoplatform.wiki.service.WikiService;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.converter.ConversionException;
import org.xwiki.rendering.syntax.Syntax;

import java.util.*;

public class PageContentMigrationService {

  private static final Log LOG = ExoLogger.getLogger(PageContentMigrationService.class);

  private RenderingService renderingService;

  private WikiService wikiService;

  private EntityManagerService entityManagerService;

  private PageDAO pageDAO;

  private PageVersionDAO pageVersionDAO;

  private int nbMigratedPages = 0;

  private int nbMigratedPagesVersions = 0;

  private List<Page> pagesInError = new ArrayList<>();

  private List<Page> pagesVersionsInError = new ArrayList<>();

  public PageContentMigrationService(RenderingService renderingService, WikiService wikiService,
                                     EntityManagerService entityManagerService, PageDAO pageDAO,
                                     PageVersionDAO pageVersionDAO) {
    this.renderingService = renderingService;
    this.wikiService = wikiService;
    this.entityManagerService = entityManagerService;
    this.pageDAO = pageDAO;
    this.pageVersionDAO = pageVersionDAO;
  }

  /**
   * Migrate page from XWiki 2.0 syntax to HTML
   * @param page Page to migrate
   * @throws ComponentLookupException
   * @throws ConversionException
   * @throws WikiException
   */
  public void migratePage(Page page) throws Exception {
    if(page.getSyntax() == null || page.getSyntax().equals(Syntax.XWIKI_2_0.toIdString())) {
      LOG.info("Convert wiki page " + page.getId() + " to HTML");

      setWikiContext(page);

      String markup = convertContent(page.getContent());
      page.setContent(markup);
      page.setSyntax(Syntax.XHTML_1_0.toIdString());

      wikiService.updatePage(page, null);
    }
  }

  public String convertContent(String xwikiContent) throws Exception {
    return renderingService.render(xwikiContent, Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false);
  }

    /**
     * Migrate all pages from XWiki 2.0 syntax to HTML
     */
  public void migrateAllPages() {
    int batchSize = 10;

    try {
      entityManagerService.startRequest(ExoContainerContext.getCurrentContainer());

      Long nbPagesToMigrate = pageDAO.countPagesBySyntax(Syntax.XWIKI_2_0.toIdString());
      if (nbPagesToMigrate == null || nbPagesToMigrate == 0) {
        LOG.info("Wiki pages syntax migration - No Wiki page to migrate from XWiki syntax to HTML");
        return;
      }

      LOG.info("==== Starting Wiki pages syntax migration");
      LOG.info("Wiki pages syntax migration - Number of pages = " + nbPagesToMigrate);

      List<PageEntity> pagesToMigrate;
      do {
        entityManagerService.endRequest(ExoContainerContext.getCurrentContainer());
        entityManagerService.startRequest(ExoContainerContext.getCurrentContainer());

        pagesToMigrate = pageDAO.findAllBySyntax(Syntax.XWIKI_2_0.toIdString(), 0, batchSize);
        for (PageEntity pageEntity : pagesToMigrate) {
          Page page = null;
          try {
            page = EntityConverter.convertPageEntityToPage(pageEntity);
            migratePage(page);
            nbMigratedPages++;
          } catch (Exception e) {
            LOG.error("Error while migrating wiki page " + (page != null ? page.getId() : "") + " to HTML", e);
            pagesInError.add(page);

            pageEntity.setSyntax("ERROR");
            pageDAO.update(pageEntity);
          }
        }

        LOG.info("Wiki pages syntax migration - Progress : " + nbMigratedPages + "/" + nbPagesToMigrate);
      } while (pagesToMigrate != null && pagesToMigrate.size() == batchSize);

      LOG.info("==== Wiki pages syntax migration - Migration finished - Number of migrated pages = " + nbMigratedPages + ", number of errors = " + pagesInError.size());
    } catch (Exception e) {
      LOG.error("Error while migrating wiki pages fom XWiki syntax to HTML", e);
    } finally {
      entityManagerService.endRequest(ExoContainerContext.getCurrentContainer());
    }
  }

  /**
   * Migrate all versions of all pages from XWiki 2.0 syntax to HTML
   */
  public void migrateAllPagesVersions() {
    int batchSize = 10;

    try {
      entityManagerService.startRequest(ExoContainerContext.getCurrentContainer());

      Long nbPagesVersionsToMigrate = pageVersionDAO.countPagesVersionsBySyntax(Syntax.XWIKI_2_0.toIdString());
      if (nbPagesVersionsToMigrate == null || nbPagesVersionsToMigrate == 0) {
        LOG.info("Wiki pages versions syntax migration - No Wiki page version to migrate from XWiki syntax to HTML");
        return;
      }

      LOG.info("==== Starting Wiki pages versions syntax migration");
      LOG.info("Wiki pages versions syntax migration - Number of pages versions = " + nbPagesVersionsToMigrate);

      List<PageVersionEntity> pagesVersionsToMigrate;
      do {
        entityManagerService.endRequest(ExoContainerContext.getCurrentContainer());
        entityManagerService.startRequest(ExoContainerContext.getCurrentContainer());

        pagesVersionsToMigrate = pageVersionDAO.findAllVersionsBySyntax(Syntax.XWIKI_2_0.toIdString(), 0, batchSize);
        for (PageVersionEntity pageVersionEntity : pagesVersionsToMigrate) {
          try {
            LOG.info("Convert wiki page version " + pageVersionEntity.getId() + " to HTML");
            String markup = convertContent(pageVersionEntity.getContent());
            pageVersionEntity.setContent(markup);
            pageVersionEntity.setSyntax(Syntax.XHTML_1_0.toIdString());
            pageVersionDAO.update(pageVersionEntity);
            nbMigratedPagesVersions++;
          } catch (Exception e) {
            LOG.error("Error while migrating wiki page version " + (pageVersionEntity != null ? pageVersionEntity.getId() : "") + " to HTML", e);
            pagesVersionsInError.add(EntityConverter.convertPageVersionEntityToPageVersion(pageVersionEntity));

            pageVersionEntity.setSyntax("ERROR");
            pageVersionDAO.update(pageVersionEntity);
          }
        }

        LOG.info("Wiki pages versions syntax migration - Progress : " + nbMigratedPagesVersions + "/" + nbPagesVersionsToMigrate);
      } while (pagesVersionsToMigrate != null && pagesVersionsToMigrate.size() == batchSize);

      LOG.info("==== Wiki pages versions syntax migration - Migration finished - Number of migrated pages = " + nbMigratedPagesVersions + ", number of errors = " + pagesVersionsInError.size());
    } catch (Exception e) {
      LOG.error("Error while migrating wiki pages versions fom XWiki syntax to HTML", e);
    } finally {
      entityManagerService.endRequest(ExoContainerContext.getCurrentContainer());
    }
  }

  /**
   * Set the wiki context in the execution environment to let know XWiki the context (wiki type, wiki owner, ...)
   * @param page
   * @throws ComponentLookupException
   * @throws ComponentRepositoryException
   */
  protected void setWikiContext(Page page) throws ComponentLookupException, ComponentRepositoryException {
    RenderingService renderingService = ExoContainerContext.getService(RenderingService.class);
    Execution ec = renderingService.getExecution();

    if (ec.getContext() == null) {
      ec.setContext(new ExecutionContext());
    }

    WikiContext wikiContext = new WikiContext();
    wikiContext.setType(page.getWikiType());
    wikiContext.setOwner(page.getWikiOwner());
    wikiContext.setPageName(page.getName());
    wikiContext.setPageTitle(page.getTitle());
    String portalURL = page.getUrl();
    if(portalURL == null) {
      portalURL = PortalContainer.getInstance().getPortalContext().getContextPath();
      if("group".equals(page.getWikiType())) {
        // FIXME does not handle renamed spaces
        portalURL += "/g/" + page.getWikiOwner().replaceAll("/", ":") + "/" + page.getWikiOwner().substring(page.getWikiOwner().lastIndexOf("/") + 1);
      } else {
        portalURL += "/" + page.getWikiOwner();
      }
    } else {
      portalURL = page.getUrl().substring(0, page.getUrl().lastIndexOf("/wiki/"));
    }
    wikiContext.setPortalURL(portalURL);
    wikiContext.setPortletURI("/wiki");
    wikiContext.setRestURI("/" + PortalContainer.getCurrentPortalContainerName() + "/" + PortalContainer.getCurrentRestContextName() + "/wiki/tree/children/");
    wikiContext.setSyntax(Syntax.XWIKI_2_0.toIdString());

    ec.getContext().setProperty(WikiContext.WIKICONTEXT, wikiContext);
  }
}
