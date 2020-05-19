package org.exoplatform.wiki.migration;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.upgrade.PageContentMigrationService;
import org.xwiki.rendering.syntax.Syntax;

public class TestMigrationService extends BaseTest {

  private WikiService wikiService;

  private PageContentMigrationService migrationService;

  public void setUp() throws Exception {
    super.setUp() ;

    wikiService = getContainer().getComponentInstanceOfType(WikiService.class);
    migrationService = getContainer().getComponentInstanceOfType(PageContentMigrationService.class);

    getOrCreateWiki(wikiService, PortalConfig.PORTAL_TYPE, "classic");
  }

  public void testShouldMigratePageWithSimpleText() throws Exception {
    Wiki wikiClassic = getOrCreateWiki(wikiService, PortalConfig.PORTAL_TYPE, "classic");

    Page page = new Page("testMigration-001", "testMigration-001");
    page.setId("testMigration-001");
    page.setWikiType("portal");
    page.setWikiOwner("classic");
    page.setSyntax(Syntax.XWIKI_2_0.toIdString());
    page.setContent("simple text");
    page.setUrl("/portal/classic/wiki/testMigration-001");
    wikiService.createPage(wikiClassic, "WikiHome", page);

    migrationService.migratePage(page);

    Page migratedPage = wikiService.getPageOfWikiByName("portal", "classic", "testMigration-001");
    assertNotNull(migratedPage);
    assertEquals(Syntax.XHTML_1_0.toIdString(), migratedPage.getSyntax());
    assertEquals(page.getTitle(), migratedPage.getTitle());
    assertEquals("<p>simple text</p>", migratedPage.getContent());
  }

  public void testShouldMigratePortalPageWithWikiPageLink() throws Exception {
    Wiki wikiClassic = getOrCreateWiki(wikiService, PortalConfig.PORTAL_TYPE, "classic");

    Page page1 = new Page("testMigrationPageLink-001", "testMigrationPageLink-001");
    page1.setId("testMigrationPageLink-001");
    page1.setWikiType("portal");
    page1.setWikiOwner("classic");
    page1.setSyntax(Syntax.XWIKI_2_0.toIdString());
    page1.setContent("Target page");
    page1.setUrl("/portal/classic/wiki/testMigrationPageLink-001");
    wikiService.createPage(wikiClassic, "WikiHome", page1);

    Page page2 = new Page("testMigrationPageLink-002", "testMigrationPageLink-002");
    page2.setId("testMigrationPageLink-002");
    page2.setWikiType("portal");
    page2.setWikiOwner("classic");
    page2.setSyntax(Syntax.XWIKI_2_0.toIdString());
    page2.setContent("[[link>>testMigrationPageLink-001||rel=\"nofollow\"]]");
    page2.setUrl("/portal/classic/wiki/testMigrationPageLink-002");
    wikiService.createPage(wikiClassic, "WikiHome", page2);

    migrationService.migratePage(page1);
    migrationService.migratePage(page2);

    Page migratedPage1 = wikiService.getPageOfWikiByName("portal", "classic", "testMigrationPageLink-001");
    assertNotNull(migratedPage1);
    assertEquals(Syntax.XHTML_1_0.toIdString(), migratedPage1.getSyntax());
    assertEquals(page1.getTitle(), migratedPage1.getTitle());
    assertEquals("<p>Target page</p>", migratedPage1.getContent());
    Page migratedPage2 = wikiService.getPageOfWikiByName("portal", "classic", "testMigrationPageLink-002");
    assertNotNull(migratedPage2);
    assertEquals(Syntax.XHTML_1_0.toIdString(), migratedPage2.getSyntax());
    assertEquals(page2.getTitle(), migratedPage2.getTitle());
    assertEquals("<p><span class=\"wikilink\"><a rel=\"nofollow\" href=\"/portal/classic/wiki/testMigrationPageLink-001\">link</a></span></p>", migratedPage2.getContent());
  }

  public void testShouldMigrateSpacePageWithWikiPageLinkInSameSpaceAndNoURL() throws Exception {
    Wiki wikiSpace1 = getOrCreateWiki(wikiService, PortalConfig.GROUP_TYPE, "/spaces/space1nourl");

    Page page1 = new Page("testMigrationSpacePageLinkNoURL-001", "testMigrationSpacePageLinkNoURL-001");
    page1.setId("testMigrationSpacePageLinkNoURL-001");
    page1.setWikiType("group");
    page1.setWikiOwner("/spaces/space1nourl");
    page1.setSyntax(Syntax.XWIKI_2_0.toIdString());
    page1.setContent("Target page");
    page1.setUrl("/portal/g/:spaces:space1nourl/space1nourl/wiki/group/spaces/testMigrationSpacePageLinkNoURL-001");
    wikiService.createPage(wikiSpace1, "WikiHome", page1);

    Page page2 = new Page("testMigrationSpacePageLink-002", "testMigrationSpacePageLinkNoURL-002");
    page2.setId("testMigrationSpacePageLinkNoURL-002");
    page2.setWikiType("group");
    page2.setWikiOwner("/spaces/space1nourl");
    page2.setSyntax(Syntax.XWIKI_2_0.toIdString());
    page2.setContent("[[link>>testMigrationSpacePageLinkNoURL-001||rel=\"nofollow\"]]");
    page2.setUrl(null);
    wikiService.createPage(wikiSpace1, "WikiHome", page2);

    migrationService.migratePage(page1);
    migrationService.migratePage(page2);

    Page migratedPage1 = wikiService.getPageOfWikiByName("group", "/spaces/space1nourl", "testMigrationSpacePageLinkNoURL-001");
    assertNotNull(migratedPage1);
    assertEquals(Syntax.XHTML_1_0.toIdString(), migratedPage1.getSyntax());
    assertEquals(page1.getTitle(), migratedPage1.getTitle());
    assertEquals("<p>Target page</p>", migratedPage1.getContent());
    Page migratedPage2 = wikiService.getPageOfWikiByName("group", "/spaces/space1nourl", "testMigrationSpacePageLinkNoURL-002");
    assertNotNull(migratedPage2);
    assertEquals(Syntax.XHTML_1_0.toIdString(), migratedPage2.getSyntax());
    assertEquals(page2.getTitle(), migratedPage2.getTitle());
    assertEquals("<p><span class=\"wikilink\"><a rel=\"nofollow\" href=\"/portal/g/:spaces:space1nourl/space1nourl/wiki/group/spaces/space1nourl/testMigrationSpacePageLinkNoURL-001\">link</a></span></p>", migratedPage2.getContent());
  }

  public void testShouldMigrateSpacePageWithWikiPageLinkInSameSpace() throws Exception {
    Wiki wikiSpace1 = getOrCreateWiki(wikiService, PortalConfig.GROUP_TYPE, "/spaces/space1");

    Page page1 = new Page("testMigrationSpacePageLink-001", "testMigrationSpacePageLink-001");
    page1.setId("testMigrationSpacePageLink-001");
    page1.setWikiType("group");
    page1.setWikiOwner("/spaces/space1");
    page1.setSyntax(Syntax.XWIKI_2_0.toIdString());
    page1.setContent("Target page");
    page1.setUrl("/portal/g/:spaces:space1/space1/wiki/group/spaces/testMigrationSpacePageLink-001");
    wikiService.createPage(wikiSpace1, "WikiHome", page1);

    Page page2 = new Page("testMigrationSpacePageLink-002", "testMigrationSpacePageLink-002");
    page2.setId("testMigrationSpacePageLink-002");
    page2.setWikiType("group");
    page2.setWikiOwner("/spaces/space1");
    page2.setSyntax(Syntax.XWIKI_2_0.toIdString());
    page2.setContent("[[link>>testMigrationSpacePageLink-001||rel=\"nofollow\"]]");
    page2.setUrl("/portal/g/:spaces:space1/space1/wiki/group/spaces/testMigrationSpacePageLink-002");
    wikiService.createPage(wikiSpace1, "WikiHome", page2);

    migrationService.migratePage(page1);
    migrationService.migratePage(page2);

    Page migratedPage1 = wikiService.getPageOfWikiByName("group", "/spaces/space1", "testMigrationSpacePageLink-001");
    assertNotNull(migratedPage1);
    assertEquals(Syntax.XHTML_1_0.toIdString(), migratedPage1.getSyntax());
    assertEquals(page1.getTitle(), migratedPage1.getTitle());
    assertEquals("<p>Target page</p>", migratedPage1.getContent());
    Page migratedPage2 = wikiService.getPageOfWikiByName("group", "/spaces/space1", "testMigrationSpacePageLink-002");
    assertNotNull(migratedPage2);
    assertEquals(Syntax.XHTML_1_0.toIdString(), migratedPage2.getSyntax());
    assertEquals(page2.getTitle(), migratedPage2.getTitle());
    assertEquals("<p><span class=\"wikilink\"><a rel=\"nofollow\" href=\"/portal/g/:spaces:space1/space1/wiki/group/spaces/space1/testMigrationSpacePageLink-001\">link</a></span></p>", migratedPage2.getContent());
  }

  public void testShouldMigrateSpacePageWithWikiPageLinkInOtherSpace() throws Exception {
    // Given
    Wiki wikiSpace1 = getOrCreateWiki(wikiService, PortalConfig.GROUP_TYPE, "/spaces/otherspace1");
    Wiki wikiSpace2 = getOrCreateWiki(wikiService, PortalConfig.GROUP_TYPE, "/spaces/otherspace2");

    Page page1 = new Page("testMigrationOtherSpacePageLink-001", "testMigrationOtherSpacePageLink-001");
    page1.setId("testMigrationOtherSpacePageLink-001");
    page1.setWikiType("group");
    page1.setWikiOwner("/spaces/otherspace1");
    page1.setSyntax(Syntax.XWIKI_2_0.toIdString());
    page1.setContent("Target page");
    page1.setUrl("/portal/g/:spaces:otherspace1/otherspace1/wiki/group/spaces/testMigrationOtherSpacePageLink-001");
    wikiService.createPage(wikiSpace1, "WikiHome", page1);

    Page page2 = new Page("testMigrationOtherSpacePageLink-002", "testMigrationOtherSpacePageLink-002");
    page2.setId("testMigrationOtherSpacePageLink-002");
    page2.setWikiType("group");
    page2.setWikiOwner("/spaces/otherspace2");
    page2.setSyntax(Syntax.XWIKI_2_0.toIdString());
    page2.setContent("[[link>>/spaces/otherspace1.testMigrationOtherSpacePageLink-001||rel=\"nofollow\"]]");
    page2.setUrl("/portal/g/:spaces:otherspace2/otherspace2/wiki/group/spaces/testMigrationOtherSpacePageLink-002");
    wikiService.createPage(wikiSpace2, "WikiHome", page2);

    // When
    migrationService.migratePage(page1);
    migrationService.migratePage(page2);

    // Then
    Page migratedPage1 = wikiService.getPageOfWikiByName("group", "/spaces/otherspace1", "testMigrationOtherSpacePageLink-001");
    assertNotNull(migratedPage1);
    assertEquals(Syntax.XHTML_1_0.toIdString(), migratedPage1.getSyntax());
    assertEquals(page1.getTitle(), migratedPage1.getTitle());
    assertEquals("<p>Target page</p>", migratedPage1.getContent());
    Page migratedPage2 = wikiService.getPageOfWikiByName("group", "/spaces/otherspace2", "testMigrationOtherSpacePageLink-002");
    assertNotNull(migratedPage2);
    assertEquals(Syntax.XHTML_1_0.toIdString(), migratedPage2.getSyntax());
    assertEquals(page2.getTitle(), migratedPage2.getTitle());
    assertEquals("<p><span class=\"wikilink\"><a rel=\"nofollow\" href=\"/portal/g/:spaces:otherspace2/otherspace2/wiki/group/spaces/otherspace1/testMigrationOtherSpacePageLink-001\">link</a></span></p>", migratedPage2.getContent());
  }

  public void testShouldMigratePortalPageWithChildrenPageMacro() throws Exception {
    Wiki wikiClassic = getOrCreateWiki(wikiService, PortalConfig.PORTAL_TYPE, "classic");

    Page page1 = new Page("testMigrationPageMacroChildren-001", "testMigrationPageMacroChildren-001");
    page1.setId("testMigrationPageMacroChildren-001");
    page1.setWikiType("portal");
    page1.setWikiOwner("classic");
    page1.setSyntax(Syntax.XWIKI_2_0.toIdString());
    page1.setContent("Page 1\n\n{{children depth=\"1\"/}}");
    page1.setUrl("/portal/classic/wiki/testMigrationPageMacroChildren-001");
    wikiService.createPage(wikiClassic, "WikiHome", page1);

    Page page2 = new Page("testMigrationPageMacroChildren-002", "testMigrationPageMacroChildren-002");
    page2.setId("testMigrationPageMacroChildren-002");
    page2.setWikiType("portal");
    page2.setWikiOwner("classic");
    page2.setSyntax(Syntax.XWIKI_2_0.toIdString());
    page2.setContent("Page 2");
    page2.setUrl("/portal/classic/wiki/testMigrationPageMacroChildren-002");
    wikiService.createPage(wikiClassic, "testMigrationPageMacroChildren-001", page2);

    migrationService.migratePage(page1);
    migrationService.migratePage(page2);

    Page migratedPage1 = wikiService.getPageOfWikiByName("portal", "classic", "testMigrationPageMacroChildren-001");
    assertNotNull(migratedPage1);
    assertEquals(Syntax.XHTML_1_0.toIdString(), migratedPage1.getSyntax());
    assertEquals(page1.getTitle(), migratedPage1.getTitle());
    assertEquals("<p>Page 1</p><exo-wiki-children-pages depth=\"1\"></exo-wiki-children-pages>", migratedPage1.getContent());
  }

  public void testShouldMigratePortalPageWithToCMacro() throws Exception {
    Wiki wikiClassic = getOrCreateWiki(wikiService, PortalConfig.PORTAL_TYPE, "classic");

    Page page1 = new Page("testMigrationPageMacroToC-001", "testMigrationPageMacroToC-001");
    page1.setId("testMigrationPageMacroToC-001");
    page1.setWikiType("portal");
    page1.setWikiOwner("classic");
    page1.setSyntax(Syntax.XWIKI_2_0.toIdString());
    page1.setContent("{{toc /}}\n\n== Page 1 ==\n\n");
    page1.setUrl("/portal/classic/wiki/testMigrationPageMacroToC-001");
    wikiService.createPage(wikiClassic, "WikiHome", page1);

    migrationService.migratePage(page1);

    Page migratedPage1 = wikiService.getPageOfWikiByName("portal", "classic", "testMigrationPageMacroToC-001");
    assertNotNull(migratedPage1);
    assertEquals(Syntax.XHTML_1_0.toIdString(), migratedPage1.getSyntax());
    assertEquals(page1.getTitle(), migratedPage1.getTitle());
    assertEquals("<div class=\"toc\"><ul><li><ul><li><span class=\"wikilink\"><a href=\"#HPage1\">Page 1</a></span></li></ul></li></ul></div><h2 id=\"HPage1\"><span>Page 1</span></h2>", migratedPage1.getContent());
  }
}
