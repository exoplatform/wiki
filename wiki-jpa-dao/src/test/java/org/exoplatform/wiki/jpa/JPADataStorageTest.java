/*
 *
 *  * Copyright (C) 2003-2015 eXo Platform SAS.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 */

package org.exoplatform.wiki.jpa;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.service.IDType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.WikiConstants;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 9/8/15
 */
public class JPADataStorageTest extends BaseWikiJPAIntegrationTest {

  protected JPADataStorage storage;
  protected SessionProvider sessionProvider;
  private static SessionProviderService sessionProviderService;
  
  public void setUp() {
    super.setUp();

    // Init services
    storage = PortalContainer.getInstance().getComponentInstanceOfType(JPADataStorage.class);
    sessionProviderService = PortalContainer.getInstance().getComponentInstanceOfType(SessionProviderService.class);
  }

  @Test
  public void testCreateWiki() throws Exception {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");

    // When
    storage.createWiki(wiki);
    Wiki createdWiki = storage.getWikiByTypeAndOwner("portal", "wiki1");
    Page wikiHomePage = createdWiki.getWikiHome();

    // Then
    assertNotNull(createdWiki);
    assertEquals("portal", createdWiki.getType());
    assertEquals("wiki1", createdWiki.getOwner());
    assertNotNull(wikiHomePage);
    assertEquals(WikiConstants.WIKI_HOME_NAME, wikiHomePage.getName());
    assertEquals(WikiConstants.WIKI_HOME_TITLE, wikiHomePage.getTitle());
    assertNotNull(wikiHomePage.getCreatedDate());
    assertNotNull(wikiHomePage.getUpdatedDate());
    assertTrue(StringUtils.isNotEmpty(wikiHomePage.getContent()));
  }

  @Test
  public void testWikiPermissions() throws Exception {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");

    Identity userIdentity = new Identity("user", Arrays.asList(
            new MembershipEntry("/platform/users", "*")));
    Identity adminIdentity = new Identity("admin", Arrays.asList(
            new MembershipEntry("/platform/users", "*"),
            new MembershipEntry("/platform/administrators", "*")));

    List<PermissionEntry> wikiPermissions = new ArrayList<>();
    wikiPermissions.add(new PermissionEntry("user", null, IDType.USER, new Permission[] {
            new Permission(PermissionType.VIEWPAGE, true) }));
    wikiPermissions.add(new PermissionEntry("admin", null, IDType.USER, new Permission[]{
            new Permission(PermissionType.VIEWPAGE, true),
            new Permission(PermissionType.EDITPAGE, true),
            new Permission(PermissionType.ADMINPAGE, true),
            new Permission(PermissionType.ADMINSPACE, true)}));
    wiki.setPermissions(wikiPermissions);

    // When
    storage.createWiki(wiki);

    // Then
    assertTrue(storage.hasPermissionOnWiki(wiki, PermissionType.VIEWPAGE, userIdentity));
    assertTrue(storage.hasPermissionOnWiki(wiki, PermissionType.VIEWPAGE, adminIdentity));
    assertFalse(storage.hasPermissionOnWiki(wiki, PermissionType.EDITPAGE, userIdentity));
    assertTrue(storage.hasPermissionOnWiki(wiki, PermissionType.EDITPAGE, adminIdentity));
    assertFalse(storage.hasAdminPagePermission(wiki.getType(), wiki.getOwner(), userIdentity));
    assertTrue(storage.hasAdminPagePermission(wiki.getType(), wiki.getOwner(), adminIdentity));
    assertFalse(storage.hasAdminSpacePermission(wiki.getType(), wiki.getOwner(), userIdentity));
    assertTrue(storage.hasAdminSpacePermission(wiki.getType(), wiki.getOwner(), adminIdentity));
  }

  @Test
  public void testUpdateWikiPermissions() throws Exception {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");

    List<PermissionEntry> initialPermissions = new ArrayList<>();
    initialPermissions.add(new PermissionEntry("user", null, IDType.USER, new Permission[] {
            new Permission(PermissionType.VIEWPAGE, true) }));
    List<PermissionEntry> updatedPermissions = new ArrayList<>();
    updatedPermissions.add(new PermissionEntry("admin", null, IDType.USER, new Permission[]{
            new Permission(PermissionType.VIEWPAGE, true),
            new Permission(PermissionType.EDITPAGE, true)}));

    // When
    storage.createWiki(wiki);
    storage.updateWikiPermission("portal", "wiki1", initialPermissions);
    List<PermissionEntry> fetchedInitialPermissions = storage.getWikiPermission("portal", "wiki1");
    storage.updateWikiPermission("portal", "wiki1", updatedPermissions);
    List<PermissionEntry> fetchedUpdatedPermissions = storage.getWikiPermission("portal", "wiki1");

    // Then
    assertNotNull(fetchedInitialPermissions);
    assertEquals(1, fetchedInitialPermissions.size());
    assertTrue(ArrayUtils.contains(fetchedInitialPermissions.get(0).getPermissions(), new Permission(PermissionType.VIEWPAGE, true)));
    assertTrue(ArrayUtils.contains(fetchedInitialPermissions.get(0).getPermissions(), new Permission(PermissionType.EDITPAGE, false)));
    assertNotNull(fetchedUpdatedPermissions);
    assertEquals(1, fetchedUpdatedPermissions.size());
    assertTrue(ArrayUtils.contains(fetchedUpdatedPermissions.get(0).getPermissions(), new Permission(PermissionType.VIEWPAGE, true)));
    assertTrue(ArrayUtils.contains(fetchedUpdatedPermissions.get(0).getPermissions(), new Permission(PermissionType.EDITPAGE, true)));
  }

  @Test
  public void testParentPageOfPage() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page parentPage = new Page();
    parentPage.setWikiId(wiki.getId());
    parentPage.setWikiType(wiki.getType());
    parentPage.setWikiOwner(wiki.getOwner());
    parentPage.setName("page0");
    parentPage.setTitle("Page 0");

    Page page = new Page();
    page.setWikiId(wiki.getId());
    page.setWikiType(wiki.getType());
    page.setWikiOwner(wiki.getOwner());
    page.setName("page1");
    page.setTitle("Page 1");

    // When
    storage.createPage(wiki, wiki.getWikiHome(), parentPage);
    storage.createPage(wiki, parentPage, page);
    Page pageOfWikiByName = storage.getPageOfWikiByName("portal", "wiki1", "page1");

    // Then
    assertEquals(3, pageDAO.findAll().size());
    assertNotNull(pageOfWikiByName);
    assertEquals("portal", pageOfWikiByName.getWikiType());
    assertEquals("wiki1", pageOfWikiByName.getWikiOwner());
    assertEquals("page1", pageOfWikiByName.getName());
    assertEquals("Page 1", pageOfWikiByName.getTitle());
  }

  @Test
  public void testGetAllWikiPages() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wikiTest4");
    wiki = storage.createWiki(wiki);

    Page parentPage = new Page();
    parentPage.setWikiId(wiki.getId());
    parentPage.setWikiType(wiki.getType());
    parentPage.setWikiOwner(wiki.getType());
    parentPage.setName("page0");
    parentPage.setTitle("Page 0");

    Page page = new Page();
    page.setWikiId(wiki.getId());
    page.setWikiType(wiki.getType());
    page.setWikiOwner(wiki.getOwner());
    page.setName("page1");
    page.setTitle("Page 1");

    // When
    storage.createPage(wiki, wiki.getWikiHome(), parentPage);
    storage.createPage(wiki, parentPage, page);

    List<Page> pagesOfWiki = storage.getPagesOfWiki(wiki.getType(), wiki.getOwner());

    assertNotNull(pagesOfWiki);
    assertEquals(3, pagesOfWiki.size());
  }

  @Test
  public void testChildrenPagesOfPage() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page parentPage = new Page();
    parentPage.setWikiId(wiki.getId());
    parentPage.setWikiType(wiki.getType());
    parentPage.setWikiOwner(wiki.getOwner());
    parentPage.setName("page0");
    parentPage.setTitle("Page 0");

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setName("page1");
    page1.setTitle("Page 1");

    Page page2 = new Page();
    page2.setWikiId(wiki.getId());
    page2.setWikiType(wiki.getType());
    page2.setWikiOwner(wiki.getOwner());
    page2.setName("page2");
    page2.setTitle("Page 2");

    // When
    storage.createPage(wiki, wiki.getWikiHome(), parentPage);
    storage.createPage(wiki, parentPage, page1);
    storage.createPage(wiki, parentPage, page2);
    List<Page> childrenPages = storage.getChildrenPageOf(parentPage);

    // Then
    assertEquals(4, pageDAO.findAll().size());
    assertNotNull(childrenPages);
    assertEquals(2, childrenPages.size());
  }

  @Test
  public void testDeletePage() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setName("page1");
    page1.setTitle("Page 1");

    // When
    storage.createPage(wiki, wiki.getWikiHome(), page1);
    assertEquals(2, pageDAO.findAll().size());
    storage.deletePage(wiki.getType(), wiki.getOwner(), page1.getName());

    // Then
    assertEquals(1, pageDAO.findAllIds(0, 0).size());
  }

  @Test
  public void testDeletePageTree() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setName("page1");
    page1.setTitle("Page 1");

    Page page2 = new Page();
    page2.setWikiId(wiki.getId());
    page2.setWikiType(wiki.getType());
    page2.setWikiOwner(wiki.getOwner());
    page2.setName("page2");
    page2.setTitle("Page 2");

    // When
    storage.createPage(wiki, wiki.getWikiHome(), page1);
    storage.createPage(wiki, page1, page2);
    assertEquals(3, pageDAO.findAll().size());
    storage.deletePage(wiki.getType(), wiki.getOwner(), page1.getName());

    // Then
    assertEquals(1, pageDAO.findAllIds(0, 0).size());
  }

  @Test
  public void testMovePage() throws WikiException {
    // Given
    Wiki wiki1 = new Wiki();
    wiki1.setType("portal");
    wiki1.setOwner("wiki1");
    wiki1 = storage.createWiki(wiki1);
    Wiki wiki2 = new Wiki();
    wiki2.setType("portal");
    wiki2.setOwner("wiki2");
    wiki2 = storage.createWiki(wiki2);

    Page page1 = new Page();
    page1.setName("page1");
    page1.setTitle("Page 1");
    Page page11 = new Page();
    page11.setName("page11");
    page11.setTitle("Page 11");

    Page page2 = new Page();
    page2.setName("page2");
    page2.setTitle("Page 2");

    // When
    storage.createPage(wiki1, wiki1.getWikiHome(), page1);
    storage.createPage(wiki1, page1, page11);
    storage.createPage(wiki1, wiki1.getWikiHome(), page2);
    assertEquals(5, pageDAO.findAll().size());
    assertEquals(2, storage.getChildrenPageOf(wiki1.getWikiHome()).size());
    storage.movePage(new WikiPageParams(wiki1.getType(), wiki1.getOwner(), page1.getName()), new WikiPageParams(wiki2.getType(),
            wiki2.getOwner(),
            wiki2.getWikiHome().getName()));

    // Then
    assertEquals(5, pageDAO.findAll().size());
    assertEquals(1, storage.getChildrenPageOf(wiki1.getWikiHome()).size());
    List<Page> wiki2HomeChildrenPages = storage.getChildrenPageOf(wiki2.getWikiHome());
    assertEquals(1, wiki2HomeChildrenPages.size());
    Page movedPage1 = wiki2HomeChildrenPages.get(0);
    assertEquals("page1", movedPage1.getName());
    assertEquals("Page 1", movedPage1.getTitle());
    assertEquals(1, storage.getChildrenPageOf(movedPage1).size());
    Page fetchedPage11 = storage.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "wiki2", "page11");
    assertNotNull(fetchedPage11);
    assertEquals("page11", fetchedPage11.getName());
  }

  @Test
  public void testUpdatePage() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setName("page1");
    page1.setTitle("Page 1");

    // When
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page1);
    assertEquals(2, pageDAO.findAll().size());
    createdPage.setTitle("Page 1 updated");
    storage.updatePage(createdPage);

    // Then
    assertEquals(2, pageDAO.findAll().size());
    Page updatedPage = storage.getPageById(createdPage.getId());
    assertNotNull(updatedPage);
    assertEquals("page1", updatedPage.getName());
    assertEquals("Page 1 updated", updatedPage.getTitle());
  }

  @Test
  public void testRenamePage() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setName("page1");
    page1.setTitle("Page 1");

    // When
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page1);
    assertEquals(2, pageDAO.findAll().size());
    storage.renamePage(wiki.getType(), wiki.getOwner(), page1.getName(), "newName", "New Title");

    // Then
    assertEquals(2, pageDAO.findAll().size());
    Page renamedPage = storage.getPageById(createdPage.getId());
    assertNotNull(renamedPage);
    assertEquals("newName", renamedPage.getName());
    assertEquals("New Title", renamedPage.getTitle());
  }

  @Test
  public void testPermissionsOnPage() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Identity userIdentity = new Identity("user", Arrays.asList(
            new MembershipEntry("/platform/users", "*")));
    Identity adminIdentity = new Identity("admin", Arrays.asList(
            new MembershipEntry("/platform/users", "*"),
            new MembershipEntry("/platform/administrators", "*")));

    Page noPermissionPage = new Page();
    noPermissionPage.setWikiId(wiki.getId());
    noPermissionPage.setWikiType(wiki.getType());
    noPermissionPage.setWikiOwner(wiki.getOwner());
    noPermissionPage.setName("page1");
    noPermissionPage.setTitle("Page 1");
    noPermissionPage.setPermissions(new ArrayList<PermissionEntry>());
    noPermissionPage = storage.createPage(wiki, wiki.getWikiHome(), noPermissionPage);

    Page publicPage = new Page();
    publicPage.setWikiId(wiki.getId());
    publicPage.setWikiType(wiki.getType());
    publicPage.setWikiOwner(wiki.getOwner());
    publicPage.setName("page1");
    publicPage.setTitle("Page 1");
    publicPage.setPermissions(Arrays.asList(new PermissionEntry(IdentityConstants.ANY, null, IDType.USER, new Permission[] {(
            new Permission(PermissionType.VIEWPAGE, true)
    )})));
    publicPage = storage.createPage(wiki, wiki.getWikiHome(), publicPage);

    Page authenticatedPage = new Page();
    authenticatedPage.setWikiId(wiki.getId());
    authenticatedPage.setWikiType(wiki.getType());
    authenticatedPage.setWikiOwner(wiki.getOwner());
    authenticatedPage.setName("page2");
    authenticatedPage.setTitle("Page 2");
    authenticatedPage.setPermissions(Arrays.asList(new PermissionEntry("/platform/users", null, IDType.GROUP, new Permission[] {(
            new Permission(PermissionType.VIEWPAGE, true)
    )}), new PermissionEntry("/platform/administrators", null, IDType.GROUP, new Permission[] {(
            new Permission(PermissionType.EDITPAGE, true)
    )})));
    authenticatedPage = storage.createPage(wiki, wiki.getWikiHome(), authenticatedPage);

    Page adminPage = new Page();
    adminPage.setWikiId(wiki.getId());
    adminPage.setWikiType(wiki.getType());
    adminPage.setWikiOwner(wiki.getOwner());
    adminPage.setName("page3");
    adminPage.setTitle("Page 3");
    adminPage.setPermissions(Arrays.asList(new PermissionEntry("*:/platform/administrators", null, IDType.MEMBERSHIP, new Permission[] {(
            new Permission(PermissionType.VIEWPAGE, true)
    )})));
    adminPage = storage.createPage(wiki, wiki.getWikiHome(), adminPage);

    //Then
    assertTrue(storage.hasPermissionOnPage(noPermissionPage, PermissionType.VIEWPAGE, userIdentity));
    assertTrue(storage.hasPermissionOnPage(noPermissionPage, PermissionType.VIEWPAGE, adminIdentity));
    assertTrue(storage.hasPermissionOnPage(publicPage, PermissionType.VIEWPAGE, userIdentity));
    assertTrue(storage.hasPermissionOnPage(publicPage, PermissionType.VIEWPAGE, adminIdentity));
    assertTrue(storage.hasPermissionOnPage(authenticatedPage, PermissionType.VIEWPAGE, userIdentity));
    assertTrue(storage.hasPermissionOnPage(authenticatedPage, PermissionType.VIEWPAGE, adminIdentity));
    assertFalse(storage.hasPermissionOnPage(authenticatedPage, PermissionType.EDITPAGE, userIdentity));
    assertTrue(storage.hasPermissionOnPage(authenticatedPage, PermissionType.EDITPAGE, adminIdentity));
    assertFalse(storage.hasPermissionOnPage(adminPage, PermissionType.VIEWPAGE, userIdentity));
    assertTrue(storage.hasPermissionOnPage(adminPage, PermissionType.VIEWPAGE, adminIdentity));
  }

  @Test
  public void testAttachmentsOfPage() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setCreatedDate(new Date());
    page1.setUpdatedDate(new Date());
    page1.setName("page1");
    page1.setTitle("Page 1");

    Page page2 = new Page();
    page2.setWikiId(wiki.getId());
    page2.setWikiType(wiki.getType());
    page2.setWikiOwner(wiki.getOwner());
    page2.setCreatedDate(new Date());
    page2.setUpdatedDate(new Date());
    page2.setName("page2");
    page2.setTitle("Page 2");

    Attachment attachment1 = new Attachment();
    attachment1.setCreatedDate(Calendar.getInstance());
    attachment1.setUpdatedDate(Calendar.getInstance());
    attachment1.setName("attachment1");
    attachment1.setContent("content attachment1".getBytes());

    Attachment attachment2 = new Attachment();
    attachment2.setCreatedDate(Calendar.getInstance());
    attachment2.setUpdatedDate(Calendar.getInstance());
    attachment2.setName("attachment2");
    attachment2.setContent("content attachment2".getBytes());

    // When
    storage.createPage(wiki, wiki.getWikiHome(), page1);
    storage.createPage(wiki, wiki.getWikiHome(), page2);
    storage.addAttachmentToPage(attachment1, page1);
    storage.addAttachmentToPage(attachment2, page1);
    List<Attachment> attachmentsOfPage1 = storage.getAttachmentsOfPage(page1);
    List<Attachment> attachmentsOfPage2 = storage.getAttachmentsOfPage(page2);

    // Then
    assertEquals(2, pageAttachmentDAO.findAll().size());
    assertNotNull(attachmentsOfPage1);
    assertEquals(2, attachmentsOfPage1.size());
    assertNotNull(attachmentsOfPage2);
    assertEquals(0, attachmentsOfPage2.size());
  }

  @Test
  public void testDeleteAttachmentOfPage() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setCreatedDate(new Date());
    page1.setUpdatedDate(new Date());
    page1.setName("page1");
    page1.setTitle("Page 1");

    Attachment attachment1 = new Attachment();
    attachment1.setCreatedDate(GregorianCalendar.getInstance());
    attachment1.setUpdatedDate(GregorianCalendar.getInstance());
    attachment1.setName("attachment1");
    attachment1.setContent("content attachment2".getBytes());

    Attachment attachment2 = new Attachment();
    attachment2.setName("attachment2");
    attachment2.setCreatedDate(GregorianCalendar.getInstance());
    attachment2.setUpdatedDate(GregorianCalendar.getInstance());
    attachment2.setContent("content attachment2".getBytes());

    // When
    storage.createPage(wiki, wiki.getWikiHome(), page1);
    storage.addAttachmentToPage(attachment1, page1);
    storage.addAttachmentToPage(attachment2, page1);
    List<Attachment> attachmentsOfPage = storage.getAttachmentsOfPage(page1);
    storage.deleteAttachmentOfPage("attachment1", page1);
    List<Attachment> attachmentsOfPageAfterDeletion = storage.getAttachmentsOfPage(page1);

    // Then
    assertNotNull(attachmentsOfPage);
    assertEquals(2, attachmentsOfPage.size());
    assertNotNull(attachmentsOfPageAfterDeletion);
    assertEquals(1, attachmentsOfPageAfterDeletion.size());
  }
  
  @Test
  public void testDeleteAttachmentOfDraftPage() throws WikiException {
    // Given
    startSessionAs("Jhon");
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki); 
    
    DraftPage draftPage = new DraftPage();
    draftPage.setAuthor("Jhon");
    draftPage.setName("DraftPage1");
    draftPage.setTitle("DraftPage 1");
    draftPage.setWikiId(wiki.getId());
    draftPage.setWikiType(wiki.getType());
    draftPage.setWikiOwner(wiki.getOwner());
    draftPage.setNewPage(true);
    draftPage.setTargetPageRevision("1");
    draftPage.setCreatedDate(new Date());
    draftPage.setUpdatedDate(new Date());
    
    Attachment attachment1 = new Attachment();
    attachment1.setName("attachment1");
    attachment1.setContent("content attachment2".getBytes());
    attachment1.setCreatedDate(GregorianCalendar.getInstance());
    attachment1.setUpdatedDate(GregorianCalendar.getInstance());

    Attachment attachment2 = new Attachment();
    attachment2.setName("attachment2");
    attachment2.setContent("content attachment2".getBytes());
    attachment2.setCreatedDate(GregorianCalendar.getInstance());
    attachment2.setUpdatedDate(GregorianCalendar.getInstance());
    
    //When
    storage.createDraftPageForUser(draftPage, "Jhon");
    storage.addAttachmentToPage(attachment1, draftPage);
    storage.addAttachmentToPage(attachment2, draftPage);
    List<Attachment> attachmentsOfdraftPage = storage.getAttachmentsOfPage(draftPage);
    storage.deleteAttachmentOfPage("attachment1", draftPage);
    List<Attachment> attachmentsOfdraftPageAfterDeletion = storage.getAttachmentsOfPage(draftPage);
    
    //then
    assertNotNull(attachmentsOfdraftPage);
    assertEquals(2, attachmentsOfdraftPage.size());
    assertNotNull(attachmentsOfdraftPageAfterDeletion);
    assertEquals(1, attachmentsOfdraftPageAfterDeletion.size());
  }

  @Test
  public void testRelatedPagesOfPage() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page = new Page();
    page.setWikiId(wiki.getId());
    page.setWikiType(wiki.getType());
    page.setWikiOwner(wiki.getOwner());
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setName("page0");
    page.setTitle("Page 0");

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setCreatedDate(new Date());
    page1.setUpdatedDate(new Date());
    page1.setName("page1");
    page1.setTitle("Page 1");

    Page page2 = new Page();
    page2.setWikiId(wiki.getId());
    page2.setWikiType(wiki.getType());
    page2.setWikiOwner(wiki.getOwner());
    page2.setCreatedDate(new Date());
    page2.setUpdatedDate(new Date());
    page2.setName("page2");
    page2.setTitle("Page 2");

    // When
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page);
    Page createdPage1 = storage.createPage(wiki, wiki.getWikiHome(), page1);
    Page createdPage2 = storage.createPage(wiki, wiki.getWikiHome(), page2);
    storage.addRelatedPage(createdPage, page1);
    storage.addRelatedPage(createdPage, page2);

    // Then
    assertEquals(4, pageDAO.findAll().size());
    assertNotNull(createdPage);
    assertNotNull(storage.getRelatedPagesOfPage(createdPage));
    assertEquals(2, storage.getRelatedPagesOfPage(createdPage).size());
  }

  @Test
  public void testRemoveRelatedPagesOfPage() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page = new Page();
    page.setWikiId(wiki.getId());
    page.setWikiType(wiki.getType());
    page.setWikiOwner(wiki.getOwner());
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setName("page0");
    page.setTitle("Page 0");

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setCreatedDate(new Date());
    page1.setUpdatedDate(new Date());
    page1.setName("page1");
    page1.setTitle("Page 1");

    Page page2 = new Page();
    page2.setWikiId(wiki.getId());
    page2.setWikiType(wiki.getType());
    page2.setWikiOwner(wiki.getOwner());
    page2.setCreatedDate(new Date());
    page2.setUpdatedDate(new Date());
    page2.setName("page2");
    page2.setTitle("Page 2");

    // When
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page);
    Page createdPage1 = storage.createPage(wiki, wiki.getWikiHome(), page1);
    Page createdPage2 = storage.createPage(wiki, wiki.getWikiHome(), page2);
    storage.addRelatedPage(createdPage, page1);
    storage.addRelatedPage(createdPage, page2);
    List<Page> relatedPagesBeforeDeletion = storage.getRelatedPagesOfPage(page);
    storage.removeRelatedPage(createdPage, createdPage1);
    List<Page> relatedPagesAfterDeletion = storage.getRelatedPagesOfPage(page);

    // Then
    assertNotNull(relatedPagesBeforeDeletion);
    assertEquals(2, relatedPagesBeforeDeletion.size());
    assertNotNull(relatedPagesAfterDeletion);
    assertEquals(1, relatedPagesAfterDeletion.size());
  }

  @Test
  public void testDraftPagesOfUser() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page = new Page();
    page.setWikiId(wiki.getId());
    page.setWikiType(wiki.getType());
    page.setWikiOwner(wiki.getOwner());
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setName("page1");
    page.setTitle("Page 1");
    page.setContent("Content Page 1");
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page);

    DraftPage draftPage = new DraftPage();
    draftPage.setAuthor("user1");
    draftPage.setName("DraftPage1");
    draftPage.setTitle("DraftPage 1");
    draftPage.setContent("Content Page 1 Updated");
    draftPage.setTargetPageId(createdPage.getId());
    draftPage.setTargetPageRevision("1");
    draftPage.setCreatedDate(new Date());
    draftPage.setUpdatedDate(new Date());

    // When
    storage.createDraftPageForUser(draftPage, "user1");
    List<DraftPage> draftPagesOfUser1 = storage.getDraftPagesOfUser("user1");
    List<DraftPage> draftPagesOfUser2 = storage.getDraftPagesOfUser("user2");

    // Then
    assertNotNull(draftPagesOfUser1);
    assertEquals(1, draftPagesOfUser1.size());
    assertEquals("DraftPage1", draftPagesOfUser1.get(0).getName());
    assertEquals(createdPage.getId(), draftPagesOfUser1.get(0).getTargetPageId());
    assertEquals(WikiType.USER.toString(), draftPagesOfUser1.get(0).getWikiType());
    assertEquals("user1", draftPagesOfUser1.get(0).getWikiOwner());
    assertEquals("Content Page 1 Updated", draftPagesOfUser1.get(0).getContent());
    assertNotNull(draftPagesOfUser2);
    assertEquals(0, draftPagesOfUser2.size());
  }

  @Test
  public void testDraftPageOfUserByName() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page = new Page();
    page.setWikiId(wiki.getId());
    page.setWikiType(wiki.getType());
    page.setWikiOwner(wiki.getOwner());
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setName("page1");
    page.setTitle("Page 1");
    page.setContent("Content Page 1");
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page);

    DraftPage draftPage = new DraftPage();
    draftPage.setAuthor("user1");
    draftPage.setName("DraftPage1");
    draftPage.setTitle("DraftPage 1");
    draftPage.setContent("Content Page 1 Updated");
    draftPage.setTargetPageId(createdPage.getId());
    draftPage.setTargetPageRevision("1");
    draftPage.setCreatedDate(new Date());
    draftPage.setUpdatedDate(new Date());

    // When
    storage.createDraftPageForUser(draftPage, "user1");
    DraftPage draftPage1OfUser1 = storage.getDraft("DraftPage1", "user1");
    DraftPage draftPage2OfUser1 = storage.getDraft("DraftPage2", "user1");
    DraftPage draftPage1OfUser2 = storage.getDraft("DraftPage1", "user2");

    // Then
    assertNotNull(draftPage1OfUser1);
    assertEquals(createdPage.getId(), draftPage1OfUser1.getTargetPageId());
    assertEquals("Content Page 1 Updated", draftPage1OfUser1.getContent());
    assertNull(draftPage2OfUser1);
    assertNull(draftPage1OfUser2);
  }

  @Test
  public void testLatestDraftPageOfUser() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page = new Page();
    page.setWikiId(wiki.getId());
    page.setWikiType(wiki.getType());
    page.setWikiOwner(wiki.getOwner());
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setName("page1");
    page.setTitle("Page 1");
    page.setContent("Content Page 1");
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page);

    Calendar calendar = Calendar.getInstance();
    Date now = calendar.getTime();
    calendar.add(Calendar.YEAR, -1);
    Date oneYearAgo = calendar.getTime();

    DraftPage draftPage1 = new DraftPage();
    draftPage1.setAuthor("user1");
    draftPage1.setName("DraftPage1");
    draftPage1.setTitle("DraftPage 1");
    draftPage1.setContent("Content Page 1 Updated");
    draftPage1.setTargetPageId(createdPage.getId());
    draftPage1.setTargetPageRevision("1");
    draftPage1.setUpdatedDate(oneYearAgo);
    draftPage1.setCreatedDate(oneYearAgo);

    DraftPage draftPage2 = new DraftPage();
    draftPage2.setAuthor("user1");
    draftPage2.setName("DraftPage1");
    draftPage2.setTitle("DraftPage 1");
    draftPage2.setContent("Content Page 1 Updated Again");
    draftPage2.setTargetPageId(createdPage.getId());
    draftPage2.setTargetPageRevision("2");
    draftPage2.setUpdatedDate(now);
    draftPage2.setCreatedDate(now);

    // When
    storage.createDraftPageForUser(draftPage1, "user1");
    storage.createDraftPageForUser(draftPage2, "user1");
    DraftPage fetchedDraftPage1 = storage.getLastestDraft("user1");
    DraftPage fetchedDraftPage2 = storage.getLastestDraft("user2");

    // Then
    assertNotNull(fetchedDraftPage1);
    assertEquals(createdPage.getId(), fetchedDraftPage1.getTargetPageId());
    assertEquals("Content Page 1 Updated Again", fetchedDraftPage1.getContent());
    assertNull(fetchedDraftPage2);
  }

  @Test
  public void testDraftPageOfUserByNameAndTargetPage() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page = new Page();
    page.setWikiId(wiki.getId());
    page.setWikiType(wiki.getType());
    page.setWikiOwner(wiki.getOwner());
    page.setName("page1");
    page.setTitle("Page 1");
    page.setContent("Content Page 1");
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page);

    Calendar calendar = Calendar.getInstance();
    Date now = calendar.getTime();
    calendar.roll(Calendar.YEAR, -1);
    Date oneYearAgo = calendar.getTime();

    DraftPage draftPage1 = new DraftPage();
    draftPage1.setAuthor("user1");
    draftPage1.setName("DraftPage1");
    draftPage1.setTitle("DraftPage 1");
    draftPage1.setContent("Content Page 1 Updated");
    draftPage1.setTargetPageId(createdPage.getId());
    draftPage1.setTargetPageRevision("1");
    draftPage1.setUpdatedDate(oneYearAgo);
    draftPage1.setCreatedDate(oneYearAgo);

    DraftPage draftPage2 = new DraftPage();
    draftPage2.setAuthor("user1");
    draftPage2.setName("DraftPage2");
    draftPage2.setTitle("DraftPage 2");
    draftPage2.setContent("Content Page 2 Updated");
    draftPage2.setTargetPageId(createdPage.getId());
    draftPage2.setTargetPageRevision("1");
    draftPage2.setUpdatedDate(now);
    draftPage2.setCreatedDate(now);

    // When
    storage.createDraftPageForUser(draftPage1, "user1");
    storage.createDraftPageForUser(draftPage2, "user1");
    DraftPage fetchedDraftPage1 = storage.getDraft(new WikiPageParams("portal", "wiki1", "page1"), "user1");
    DraftPage fetchedDraftPage2 = storage.getDraft(new WikiPageParams("portal", "wiki1", "page1"), "user2");

    // Then
    assertNotNull(fetchedDraftPage1);
    assertEquals("DraftPage2", fetchedDraftPage1.getName());
    assertNull(fetchedDraftPage2);
  }

  @Test
  public void testGetExistingOrNewDraftPage() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page = new Page();
    page.setWikiId(wiki.getId());
    page.setWikiType(wiki.getType());
    page.setWikiOwner(wiki.getOwner());
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setName("page1");
    page.setTitle("Page 1");
    page.setContent("Content Page 1");
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page);

    // When
    Page page1 = storage.getExsitedOrNewDraftPageById("portal", "wiki1", "page1", "user1");

    // Then
    assertNotNull(page1);
    assertTrue(page1 instanceof DraftPage);
    assertEquals(PortalConfig.USER_TYPE, page1.getWikiType());
    assertEquals("user1", page1.getWikiOwner());
    assertEquals("page1", page1.getName());
  }

  @Test
  public void testDeleteDraftPageOfUserByNameAndTargetPage() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page = new Page();
    page.setWikiId(wiki.getId());
    page.setWikiType(wiki.getType());
    page.setWikiOwner(wiki.getOwner());
    page.setName("page1");
    page.setTitle("Page 1");
    page.setContent("Content Page 1");
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page);

    Calendar calendar = Calendar.getInstance();
    Date now = calendar.getTime();
    calendar.roll(Calendar.YEAR, -1);
    Date oneYearAgo = calendar.getTime();

    DraftPage draftPage1 = new DraftPage();
    draftPage1.setAuthor("user1");
    draftPage1.setName("DraftPage1");
    draftPage1.setTitle("DraftPage 1");
    draftPage1.setContent("Content Page 1 User1");
    draftPage1.setTargetPageId(createdPage.getId());
    draftPage1.setTargetPageRevision("1");
    draftPage1.setCreatedDate(oneYearAgo);
    draftPage1.setUpdatedDate(oneYearAgo);

    DraftPage draftPage2 = new DraftPage();
    draftPage2.setAuthor("user2");
    draftPage2.setName("DraftPage2");
    draftPage2.setTitle("DraftPage 2");
    draftPage2.setContent("Content Page 1 User 2");
    draftPage2.setTargetPageId(createdPage.getId());
    draftPage2.setTargetPageRevision("1");
    draftPage2.setCreatedDate(now);
    draftPage2.setUpdatedDate(now);

    // When
    storage.createDraftPageForUser(draftPage1, "user1");
    storage.createDraftPageForUser(draftPage2, "user2");
    DraftPage initialDraftPageUser1 = storage.getDraft(new WikiPageParams("portal", "wiki1", "page1"), "user1");
    DraftPage initialDraftPageUser2 = storage.getDraft(new WikiPageParams("portal", "wiki1", "page1"), "user2");
    storage.deleteDraftOfPage(createdPage, "user1");
    DraftPage updatedDraftPageUser1 = storage.getDraft(new WikiPageParams("portal", "wiki1", "page1"), "user1");
    DraftPage updatedDraftPageUser2 = storage.getDraft(new WikiPageParams("portal", "wiki1", "page1"), "user2");

    // Then
    assertNotNull(initialDraftPageUser1);
    assertNotNull(initialDraftPageUser2);
    assertNull(updatedDraftPageUser1);
    assertNotNull(updatedDraftPageUser2);
  }

  @Test
  public void testDeleteDraftPageOfUserByName() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page = new Page();
    page.setWikiId(wiki.getId());
    page.setWikiType(wiki.getType());
    page.setWikiOwner(wiki.getOwner());
    page.setName("page1");
    page.setTitle("Page 1");
    page.setContent("Content Page 1");
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page);

    Calendar calendar = Calendar.getInstance();
    Date now = calendar.getTime();
    calendar.roll(Calendar.YEAR, -1);
    Date oneYearAgo = calendar.getTime();

    DraftPage draftPage1 = new DraftPage();
    draftPage1.setAuthor("user1");
    draftPage1.setName("DraftPage1");
    draftPage1.setTitle("DraftPage 1");
    draftPage1.setContent("Content Page 1 User1");
    draftPage1.setTargetPageId(createdPage.getId());
    draftPage1.setTargetPageRevision("1");
    draftPage1.setUpdatedDate(oneYearAgo);
    draftPage1.setCreatedDate(oneYearAgo);

    DraftPage draftPage2 = new DraftPage();
    draftPage2.setAuthor("user2");
    draftPage2.setName("DraftPage2");
    draftPage2.setTitle("DraftPage 2");
    draftPage2.setContent("Content Page 1 User 2");
    draftPage2.setTargetPageId(createdPage.getId());
    draftPage2.setTargetPageRevision("1");
    draftPage2.setUpdatedDate(now);
    draftPage2.setCreatedDate(now);

    // When
    storage.createDraftPageForUser(draftPage1, "user1");
    storage.createDraftPageForUser(draftPage2, "user2");
    DraftPage initialDraftPageUser1 = storage.getDraft(new WikiPageParams("portal", "wiki1", "page1"), "user1");
    DraftPage initialDraftPageUser2 = storage.getDraft(new WikiPageParams("portal", "wiki1", "page1"), "user2");
    storage.deleteDraftByName("DraftPage1", "user1");
    DraftPage updatedDraftPageUser1 = storage.getDraft(new WikiPageParams("portal", "wiki1", "page1"), "user1");
    DraftPage updatedDraftPageUser2 = storage.getDraft(new WikiPageParams("portal", "wiki1", "page1"), "user2");

    // Then
    assertNotNull(initialDraftPageUser1);
    assertNotNull(initialDraftPageUser2);
    assertNull(updatedDraftPageUser1);
    assertNotNull(updatedDraftPageUser2);
  }

  @Test
  public void testPageVersions() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page = new Page();
    page.setWikiId(wiki.getId());
    page.setWikiType(wiki.getType());
    page.setWikiOwner(wiki.getOwner());
    page.setName("page1");
    page.setTitle("Page 1");
    page.setContent("Content Page 1");
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page);

    // When
    storage.addPageVersion(createdPage);
    List<PageVersion> pageVersions1 = storage.getVersionsOfPage(createdPage);
    storage.addPageVersion(createdPage);
    List<PageVersion> pageVersions2 = storage.getVersionsOfPage(createdPage);

    // Then
    assertNotNull(pageVersions1);
    assertEquals(1, pageVersions1.size());
    assertEquals("1", pageVersions1.get(0).getName());
    assertNotNull(pageVersions2);
    assertEquals(2, pageVersions2.size());
    assertEquals("2", pageVersions2.get(0).getName());
    assertEquals("1", pageVersions2.get(1).getName());
  }

  @Test
  public void testRestorePageVersions() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page = new Page();
    page.setWikiId(wiki.getId());
    page.setWikiType(wiki.getType());
    page.setWikiOwner(wiki.getOwner());
    page.setName("page1");
    page.setTitle("Page 1");
    page.setContent("Content Page Version 1");
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page);
    storage.addPageVersion(createdPage);
    createdPage.setContent("Content Page Version 2");
    storage.updatePage(createdPage);
    storage.addPageVersion(createdPage);

    // When
    Page pageBeforeRestore = storage.getPageById(createdPage.getId());
    storage.restoreVersionOfPage("1", createdPage);
    Page pageAfterRestore = storage.getPageById(createdPage.getId());

    // Then
    assertNotNull(pageBeforeRestore);
    assertEquals("Content Page Version 2", pageBeforeRestore.getContent());
    assertNotNull(pageAfterRestore);
    assertEquals("Content Page Version 1", pageAfterRestore.getContent());
  }

  @Test
  public void testPageNames() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page = new Page();
    page.setName("page1");
    page.setTitle("Page 1");
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page);

    // When
    storage.renamePage(wiki.getType(), wiki.getOwner(), createdPage.getName(), "page2", "Page 1");
    storage.renamePage(wiki.getType(), wiki.getOwner(), "page2", "page3", "Page 1");

    // Then
    List<String> previousNames = storage.getPreviousNamesOfPage(createdPage);
    assertNotNull(previousNames);
    assertEquals(2, previousNames.size());
    assertTrue(previousNames.contains("page1"));
    assertTrue(previousNames.contains("page2"));
  }

  @Test
  public void testPageMoves() throws WikiException {
    // Given
    Wiki wiki1 = new Wiki();
    wiki1.setType("portal");
    wiki1.setOwner("wiki1");
    wiki1 = storage.createWiki(wiki1);
    Wiki wiki2 = new Wiki();
    wiki2.setType("portal");
    wiki2.setOwner("wiki2");
    wiki2 = storage.createWiki(wiki2);

    Page page1 = new Page();
    page1.setName("page1");
    page1.setTitle("Page 1");
    Page createdPage1 = storage.createPage(wiki1, wiki1.getWikiHome(), page1);

    // When
    // rename the page (so we keep the page in the same wiki)
    storage.renamePage(wiki1.getType(), wiki1.getOwner(), createdPage1.getName(), "page2", "Page 1");
    // move the page to another wiki
    storage.movePage(new WikiPageParams(wiki1.getType(), wiki1.getOwner(), "page2"), new WikiPageParams(wiki2.getType(), wiki2.getOwner(), wiki2.getWikiHome().getName()));

    Page relatedPage1 = storage.getRelatedPage(wiki1.getType(), wiki1.getOwner(), "page1");
    Page relatedPage2 = storage.getRelatedPage(wiki1.getType(), wiki1.getOwner(), "page2");
    Page relatedPage3 = storage.getRelatedPage(wiki1.getType(), wiki1.getOwner(), "page3");

    // Then
    assertNotNull(relatedPage1);
    assertEquals("portal", relatedPage1.getWikiType());
    assertEquals("wiki2", relatedPage1.getWikiOwner());
    assertEquals("page2", relatedPage1.getName());
    assertNotNull(relatedPage2);
    assertEquals("portal", relatedPage2.getWikiType());
    assertEquals("wiki2", relatedPage2.getWikiOwner());
    assertEquals("page2", relatedPage2.getName());
    assertNull(relatedPage3);
  }

  @Test
  public void testGetEmotionIcons() throws WikiException {
    // Given
    EmotionIcon emotionIcon1 = new EmotionIcon();
    emotionIcon1.setName("emotionIcon1");
    emotionIcon1.setImage("image1".getBytes());
    storage.createEmotionIcon(emotionIcon1);

    EmotionIcon emotionIcon2 = new EmotionIcon();
    emotionIcon2.setName("emotionIcon2");
    emotionIcon2.setImage("image2".getBytes());
    storage.createEmotionIcon(emotionIcon2);

    // When
    List<EmotionIcon> emotionIcons = storage.getEmotionIcons();

    // Then
    assertNotNull(emotionIcons);
    assertEquals(2, emotionIcons.size());
  }

  @Test
  public void testGetEmotionIconByName() throws WikiException {
    // Given
    EmotionIcon emotionIcon1 = new EmotionIcon();
    emotionIcon1.setName("emotionIcon1");
    emotionIcon1.setImage("image1".getBytes());
    storage.createEmotionIcon(emotionIcon1);

    EmotionIcon emotionIcon2 = new EmotionIcon();
    emotionIcon2.setName("emotionIcon2");
    emotionIcon2.setImage("image2".getBytes());
    storage.createEmotionIcon(emotionIcon2);

    // When
    EmotionIcon fetchedEmotionIcon1 = storage.getEmotionIconByName("emotionIcon1");
    EmotionIcon fetchedEmotionIcon2 = storage.getEmotionIconByName("emotionIcon2");
    EmotionIcon fetchedEmotionIcon3 = storage.getEmotionIconByName("emotionIcon3");

    // Then
    assertNotNull(fetchedEmotionIcon1);
    assertEquals("emotionIcon1", fetchedEmotionIcon1.getName());
    assertTrue(Arrays.equals("image1".getBytes(), fetchedEmotionIcon1.getImage()));
    assertNotNull(fetchedEmotionIcon2);
    assertEquals("emotionIcon2", fetchedEmotionIcon2.getName());
    assertTrue(Arrays.equals("image2".getBytes(), fetchedEmotionIcon2.getImage()));
    assertNull(fetchedEmotionIcon3);
  }

  @Test
  public void testGetTemplate() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Template template1 = new Template();
    template1.setName("template1");
    template1.setTitle("Template 1");
    template1.setContent("Template 1 Content");
    storage.createTemplatePage(wiki, template1);

    // When
    Template fetchedTemplate1 = storage.getTemplatePage(new WikiPageParams("portal", "wiki1", null), "template1");
    Template fetchedTemplate2 = storage.getTemplatePage(new WikiPageParams("portal", "wiki1", null), "template2");
    Template fetchedTemplate1OfWiki2 = storage.getTemplatePage(new WikiPageParams("portal", "wiki2", null), "template1");

    // Then
    assertNotNull(fetchedTemplate1);
    assertEquals("template1", fetchedTemplate1.getName());
    assertNull(fetchedTemplate2);
    assertNull(fetchedTemplate1OfWiki2);
  }

  @Test
  public void testGetTemplates() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Template template1 = new Template();
    template1.setName("template1");
    template1.setTitle("Template 1");
    template1.setContent("Template 1 Content");
    storage.createTemplatePage(wiki, template1);

    Template template2 = new Template();
    template2.setName("template2");
    template2.setTitle("Template 2");
    template2.setContent("Template 2 Content");
    storage.createTemplatePage(wiki, template2);

    // When
    Map<String, Template> fetchedTemplateWiki1 = storage.getTemplates(new WikiPageParams("portal", "wiki1", null));
    Map<String, Template> fetchedTemplateWiki2 = storage.getTemplates(new WikiPageParams("portal", "wiki2", null));

    // Then
    assertNotNull(fetchedTemplateWiki1);
    assertEquals(2, fetchedTemplateWiki1.size());
    assertNotNull(fetchedTemplateWiki2);
    assertEquals(0, fetchedTemplateWiki2.size());
  }

  @Test
  public void testUpdateTemplate() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Template template1 = new Template();
    template1.setName("template1");
    template1.setTitle("Template 1");
    template1.setContent("Template 1 Content");
    storage.createTemplatePage(wiki, template1);

    Template template2 = new Template();
    template2.setName("template2");
    template2.setTitle("Template 2");
    template2.setContent("Template 2 Content");
    storage.createTemplatePage(wiki, template2);

    // When
    Template fetchedTemplate1 = storage.getTemplatePage(new WikiPageParams("portal", "wiki1", null), "template1");
    fetchedTemplate1.setTitle("Template 1 Updated");
    fetchedTemplate1.setContent("Template 1 Content Updated");
    storage.updateTemplatePage(fetchedTemplate1);
    Template fetchedTemplate1AfterUpdate = storage.getTemplatePage(new WikiPageParams("portal", "wiki1", null), "template1");
    end();
    begin();

    // Then
    assertNotNull(fetchedTemplate1AfterUpdate);
    assertEquals("template1", fetchedTemplate1AfterUpdate.getName());
    assertEquals("Template 1 Updated", fetchedTemplate1AfterUpdate.getTitle());
    assertEquals("Template 1 Content Updated", fetchedTemplate1AfterUpdate.getContent());
  }

  @Test
  public void testDeleteTemplate() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Template template1 = new Template();
    template1.setName("template1");
    template1.setTitle("Template 1");
    template1.setContent("Template 1 Content");
    storage.createTemplatePage(wiki, template1);

    Template template2 = new Template();
    template2.setName("template2");
    template2.setTitle("Template 2");
    template2.setContent("Template 2 Content");
    storage.createTemplatePage(wiki, template2);

    // When
    Map<String, Template> fetchedTemplateWiki1 = storage.getTemplates(new WikiPageParams("portal", "wiki1", null));
    storage.deleteTemplatePage("portal", "wiki1", "template2");
    Map<String, Template> fetchedTemplateWiki1AfterDeletion = storage.getTemplates(new WikiPageParams("portal", "wiki1", null));

    // Then
    assertNotNull(fetchedTemplateWiki1);
    assertEquals(2, fetchedTemplateWiki1.size());
    assertNotNull(fetchedTemplateWiki1AfterDeletion);
    assertEquals(1, fetchedTemplateWiki1AfterDeletion.size());
  }

  @Test
  public void testSearchTemplates() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Template template1 = new Template();
    template1.setName("template1");
    template1.setTitle("Template with Title 1");
    template1.setContent("Template 1 Content");
    storage.createTemplatePage(wiki, template1);

    Template template2 = new Template();
    template2.setName("template2");
    template2.setTitle("Template with Title 2");
    template2.setContent("Template 2 Content");
    storage.createTemplatePage(wiki, template2);

    // When
    List<TemplateSearchResult> searchResults1 = storage.searchTemplate(new TemplateSearchData("Template",
            wiki.getType(),
            wiki.getOwner()));
    List<TemplateSearchResult> searchResults2 = storage.searchTemplate(new TemplateSearchData("Title 1",
                                                                                              wiki.getType(),
                                                                                              wiki.getOwner()));
    List<TemplateSearchResult> searchResults3 = storage.searchTemplate(new TemplateSearchData("No Result",
                                                                                              wiki.getType(),
                                                                                              wiki.getOwner()));

    // Then
    assertNotNull(searchResults1);
    assertEquals(2, searchResults1.size());
    assertNotNull(searchResults2);
    assertEquals(1, searchResults2.size());
    assertNotNull(searchResults3);
    assertEquals(0, searchResults3.size());
  }

  @Test
  public void testGetWatchers() throws WikiException {
    // Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setName("page1");
    page1.setTitle("Page 1");

    // When
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page1);
    List<String> initialWatchers = storage.getWatchersOfPage(page1);
    storage.addWatcherToPage("user1", page1);
    List<String> step1Watchers = storage.getWatchersOfPage(page1);
    storage.addWatcherToPage("user2", page1);
    List<String> step2Watchers = storage.getWatchersOfPage(page1);
    storage.deleteWatcherOfPage("user1", page1);
    List<String> step3Watchers = storage.getWatchersOfPage(page1);

    // Then
    assertNotNull(initialWatchers);
    assertEquals(0, initialWatchers.size());
    assertNotNull(step1Watchers);
    assertEquals(1, step1Watchers.size());
    assertTrue(step1Watchers.contains("user1"));
    assertNotNull(step2Watchers);
    assertEquals(2, step2Watchers.size());
    assertTrue(step2Watchers.contains("user1"));
    assertTrue(step2Watchers.contains("user2"));
    assertEquals(1, step3Watchers.size());
    assertTrue(step3Watchers.contains("user2"));
  }

  @Test
  public void testHelpPages() throws WikiException {
    // Given
    List<ValuesParam> syntaxHelpParams = new ArrayList<>();
    ValuesParam valuesParam = new ValuesParam();
    valuesParam.setName("xwiki/2.0");
    valuesParam.setValues(Arrays.asList("jar:/wikisyntax/help/xWiki2.0_Short.txt", "jar:/wikisyntax/help/xWiki2.0_Full.txt"));
    syntaxHelpParams.add(valuesParam);

    ConfigurationManager configurationManager = PortalContainer.getInstance().getComponentInstanceOfType(ConfigurationManager.class);

    // When
    Page shortHelpPage = storage.getHelpSyntaxPage("xwiki/2.0", false, syntaxHelpParams, configurationManager);
    Page fullHelpPage = storage.getHelpSyntaxPage("xwiki/2.0", true, syntaxHelpParams, configurationManager);

    // Then
    assertNotNull(shortHelpPage);
    assertNotNull(fullHelpPage);
  }
  
  protected void startSessionAs(String user) {
    startSessionAs(user, new HashSet<MembershipEntry>());
  }

  protected void startSessionAs(String user, Collection<MembershipEntry> memberships) {
    Identity identity = new Identity(user, memberships);
    ConversationState state = new ConversationState(identity);
    ConversationState.setCurrent(state);
    sessionProviderService.setSessionProvider(null, new SessionProvider(state));
    sessionProvider = sessionProviderService.getSessionProvider(null);
  }
}
