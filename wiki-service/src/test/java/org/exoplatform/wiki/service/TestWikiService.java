/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.service;


import org.apache.commons.io.IOUtils;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.mow.core.api.AbstractMOWTestcase;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.xwiki.rendering.syntax.Syntax;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@SuppressWarnings("deprecation")
public class TestWikiService extends AbstractMOWTestcase {
  private WikiService wService;
  public void setUp() throws Exception {
    super.setUp() ;
    wService = (WikiService) container.getComponentInstanceOfType(WikiService.class);
  }
  
  public void testWikiService() {
    assertNotNull(wService) ;
  }

  public void testCreateWiki() throws WikiException {
    Wiki wiki = wService.getWikiByTypeAndOwner(PortalConfig.PORTAL_TYPE, "wiki1");
    assertNull(wiki);

    wService.createWiki(PortalConfig.PORTAL_TYPE, "wiki1");

    wiki = wService.getWikiByTypeAndOwner(PortalConfig.PORTAL_TYPE, "wiki1");
    assertNotNull(wiki);

  }

  public void testGetPortalPageById() throws WikiException {
    Wiki wikiClassic = wService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    Wiki wikiAcme = wService.createWiki(PortalConfig.PORTAL_TYPE, "acme");

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "WikiHome")) ;

    wService.createPage(wikiClassic, "WikiHome", new Page("testGetPortalPageById-001", "testGetPortalPageById-001"));

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "testGetPortalPageById-001")) ;
    
  }

  public void testGetGroupPageById() throws WikiException {
    Wiki wiki = wService.createWiki(PortalConfig.GROUP_TYPE, "/platform/users");

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.GROUP_TYPE, "platform/users", "WikiHome")) ;

    wService.createPage(wiki, "WikiHome", new Page("testGetGroupPageById-001", "testGetGroupPageById-001"));

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.GROUP_TYPE, "platform/users", "testGetGroupPageById-001")) ;
    assertNull(wService.getPageOfWikiByName(PortalConfig.GROUP_TYPE, "unknown", "WikiHome"));
  }

  public void testGetUserPageById() throws WikiException {
    Wiki wiki = wService.createWiki(PortalConfig.USER_TYPE, "john");

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.USER_TYPE, "john", "WikiHome")) ;

    wService.createPage(wiki, "WikiHome", new Page("testGetUserPageById-001", "testGetUserPageById-001"));

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.USER_TYPE, "john", "testGetUserPageById-001")) ;
    assertNull(wService.getPageOfWikiByName(PortalConfig.USER_TYPE, "unknown", "WikiHome"));
  }

  public void testCreatePageAndSubPage() throws WikiException {
    Wiki wiki = new Wiki(PortalConfig.PORTAL_TYPE, "classic");
    wService.createPage(wiki, "WikiHome", new Page("parentPage", "parentPage")) ;
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "parentPage")) ;
    wService.createPage(wiki, "parentPage", new Page("childPage", "childPage")) ;
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "childPage")) ;
  }

  public void testCreateTemplatePage() throws WikiException {
    Wiki wiki = wService.createWiki(PortalConfig.PORTAL_TYPE, "wikiForTemplate1");

    Template template = new Template();
    template.setName("MyTemplate");
    template.setTitle("My Template");
    template.setContent("My Great Template !");
    wService.createTemplatePage(wiki, template);

    WikiPageParams params = new WikiPageParams(PortalConfig.PORTAL_TYPE, "wikiForTemplate1", null);
    Template myTemplate = wService.getTemplatePage(params, "MyTemplate");
    assertNotNull(myTemplate);
    assertEquals("MyTemplate", myTemplate.getName());
    assertEquals("My Template", myTemplate.getTitle());
    assertEquals("My Great Template !", myTemplate.getContent());
  }

  public void testDeleteTemplatePage() throws WikiException {
    Wiki wiki = wService.createWiki(PortalConfig.PORTAL_TYPE, "wikiForTemplate2");

    Template template = new Template();
    template.setName("MyTemplate");
    template.setTitle("My Template");
    template.setContent("My Great Template !");
    wService.createTemplatePage(wiki, template);

    WikiPageParams params = new WikiPageParams(PortalConfig.PORTAL_TYPE, "wikiForTemplate2", null);
    assertNotNull(wService.getTemplatePage(params, "MyTemplate"));

    wService.deleteTemplatePage(PortalConfig.PORTAL_TYPE, "wikiForTemplate2", "MyTemplate");
    assertNull(wService.getTemplatePage(params, "MyTemplate"));
  }

  public void testSearchTemplate() throws WikiException {
    Wiki wiki1 = wService.createWiki(PortalConfig.PORTAL_TYPE, "wikiForTemplateSearch");
    WikiPageParams params = new WikiPageParams(PortalConfig.PORTAL_TYPE,  "wikiForTemplateSearch", null);
    Template template1 = new Template();
    template1.setName("MyTemplate");
    template1.setTitle("My Template");
    wService.createTemplatePage(wiki1, template1);
    assertNotNull(wService.getTemplatePage(params, "MyTemplate"));

    Wiki wiki2 = wService.createWiki(PortalConfig.GROUP_TYPE, "/platform/guests");
    params = new WikiPageParams(PortalConfig.GROUP_TYPE,  "/platform/guests", null);
    Template template2 = new Template();
    template2.setName("Sample_Group_Search_Template");
    template2.setTitle("Sample Group Search Template");
    wService.createTemplatePage(wiki2, template2);
    assertNotNull(wService.getTemplatePage(params, "Sample_Group_Search_Template"));

    Wiki wiki3 = wService.createWiki(PortalConfig.USER_TYPE, "demo");
    params = new WikiPageParams(PortalConfig.USER_TYPE,  "demo", null);
    Template template3 = new Template();
    template3.setName("Sample_User_Search_Template");
    template3.setTitle("Sample User Search Template");
    wService.createTemplatePage(wiki3, template3);
    assertNotNull(wService.getTemplatePage(params, "Sample_User_Search_Template"));

    TemplateSearchData data = new TemplateSearchData("Template", PortalConfig.PORTAL_TYPE, "wikiForTemplateSearch");
    List<TemplateSearchResult> result = wService.searchTemplate(data);
    assertEquals(1, result.size());

    data = new TemplateSearchData("Template", PortalConfig.GROUP_TYPE, "/platform/guests");
    result = wService.searchTemplate(data);
    assertEquals(1, result.size());

    //data = new TemplateSearchData("Template", PortalConfig.USER_TYPE, "demo");
    //result = wService.searchTemplate(data);
    //assertEquals(1, result.size());
  }

  public void testGetBreadcumb() throws WikiException {
    Wiki portalWiki = wService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    wService.createPage(portalWiki, "WikiHome", new Page("Breadcumb1", "Breadcumb1")) ;
    wService.createPage(portalWiki, "Breadcumb1", new Page("Breadcumb2", "Breadcumb2")) ;
    wService.createPage(portalWiki, "Breadcumb2", new Page("Breadcumb3", "Breadcumb3")) ;
    List<BreadcrumbData> breadCumbs = wService.getBreadcumb(PortalConfig.PORTAL_TYPE, "classic", "Breadcumb3");
    assertEquals(4, breadCumbs.size());
    assertEquals("WikiHome", breadCumbs.get(0).getId());
    assertEquals("Breadcumb1", breadCumbs.get(1).getId());
    assertEquals("Breadcumb2", breadCumbs.get(2).getId());
    assertEquals("Breadcumb3", breadCumbs.get(3).getId());

    Wiki groupWiki = wService.createWiki(PortalConfig.GROUP_TYPE, "platform/users");
    wService.createPage(groupWiki, "WikiHome", new Page("GroupBreadcumb1", "GroupBreadcumb1")) ;
    wService.createPage(groupWiki, "GroupBreadcumb1", new Page("GroupBreadcumb2", "GroupBreadcumb2")) ;
    wService.createPage(groupWiki, "GroupBreadcumb2", new Page("GroupBreadcumb3", "GroupBreadcumb3")) ;
    breadCumbs = wService.getBreadcumb(PortalConfig.GROUP_TYPE, "platform/users", "GroupBreadcumb3");
    assertEquals(4, breadCumbs.size());
    assertEquals("WikiHome", breadCumbs.get(0).getId());
    assertEquals("GroupBreadcumb1", breadCumbs.get(1).getId());
    assertEquals("GroupBreadcumb2", breadCumbs.get(2).getId());
    assertEquals("GroupBreadcumb3", breadCumbs.get(3).getId());

    Wiki userWiki = wService.createWiki(PortalConfig.USER_TYPE, "john");
    wService.createPage(userWiki, "WikiHome", new Page("UserBreadcumb1", "UserBreadcumb1")) ;
    wService.createPage(userWiki, "UserBreadcumb1", new Page("UserBreadcumb2", "UserBreadcumb2")) ;
    wService.createPage(userWiki, "UserBreadcumb2", new Page("UserBreadcumb3", "UserBreadcumb3")) ;
    breadCumbs = wService.getBreadcumb(PortalConfig.USER_TYPE, "john", "UserBreadcumb3");
    assertEquals(4, breadCumbs.size());
    assertEquals("WikiHome", breadCumbs.get(0).getId());
    assertEquals("UserBreadcumb1", breadCumbs.get(1).getId());
    assertEquals("UserBreadcumb2", breadCumbs.get(2).getId());
    assertEquals("UserBreadcumb3", breadCumbs.get(3).getId());
  }

  public void testMovePage() throws WikiException {
    //moving page in same space
    Wiki portalWiki = wService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    wService.createPage(portalWiki, "WikiHome", new Page("oldParent", "oldParent")) ;
    wService.createPage(portalWiki, "oldParent", new Page("child", "child")) ;
    wService.createPage(portalWiki, "WikiHome", new Page("newParent", "newParent")) ;

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "oldParent")) ;
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "child")) ;
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "newParent")) ;

    WikiPageParams currentLocationParams= new WikiPageParams();
    WikiPageParams newLocationParams= new WikiPageParams();
    currentLocationParams.setPageName("child");
    currentLocationParams.setType(PortalConfig.PORTAL_TYPE);
    currentLocationParams.setOwner("classic");
    newLocationParams.setPageName("newParent");
    newLocationParams.setType(PortalConfig.PORTAL_TYPE);
    newLocationParams.setOwner("classic");

    assertTrue(wService.movePage(currentLocationParams,newLocationParams)) ;

    //moving page from different spaces
    Wiki userWiki = wService.createWiki(PortalConfig.USER_TYPE, "demo");
    wService.createPage(userWiki, "WikiHome", new Page("acmePage", "acmePage")) ;
    wService.createPage(portalWiki, "WikiHome", new Page("classicPage", "classicPage")) ;

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.USER_TYPE, "demo", "acmePage")) ;
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "classicPage")) ;

    currentLocationParams.setPageName("acmePage");
    currentLocationParams.setType(PortalConfig.USER_TYPE);
    currentLocationParams.setOwner("demo");
    newLocationParams.setPageName("classicPage");
    newLocationParams.setType(PortalConfig.PORTAL_TYPE);
    newLocationParams.setOwner("classic");
    assertTrue(wService.movePage(currentLocationParams,newLocationParams)) ;

    // moving a page to another read-only page
    Wiki demoWiki = wService.createWiki(PortalConfig.PORTAL_TYPE, "demo");
    wService.createPage(demoWiki, "WikiHome", new Page("toMovedPage", "toMovedPage"));
    Page page = wService.createPage(userWiki, "WikiHome", new Page("privatePage", "privatePage"));
    HashMap<String, String[]> permissionMap = new HashMap<>();
    permissionMap.put("any", new String[] {PermissionType.VIEWPAGE.toString(), PermissionType.EDITPAGE.toString()});
    List<PermissionEntry> permissionEntries = new ArrayList<>();
    PermissionEntry permissionEntry = new PermissionEntry(IdentityConstants.ANY.toString(), "", IDType.USER, new Permission[]{
            new Permission(PermissionType.VIEWPAGE, true),
            new Permission(PermissionType.EDITPAGE, true)
    });
    permissionEntries.add(permissionEntry);
    page.setPermissions(permissionEntries);

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "demo", "toMovedPage"));
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.USER_TYPE, "demo", "privatePage"));

    currentLocationParams.setPageName("toMovedPage");
    currentLocationParams.setType(PortalConfig.PORTAL_TYPE);
    currentLocationParams.setOwner("demo");
    newLocationParams.setPageName("privatePage");
    newLocationParams.setType(PortalConfig.USER_TYPE);
    newLocationParams.setOwner("demo");

    startSessionAs("mary");

    assertFalse(wService.movePage(currentLocationParams, newLocationParams));
  }

  public void testDeletePage() throws WikiException {
    Wiki portalWiki = wService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    wService.createPage(portalWiki, "WikiHome", new Page("deletePage", "deletePage")) ;
    assertTrue(wService.deletePage(PortalConfig.PORTAL_TYPE, "classic", "deletePage")) ;
    //wait(10) ;
    wService.createPage(portalWiki, "WikiHome", new Page("deletePage", "deletePage")) ;
    assertTrue(wService.deletePage(PortalConfig.PORTAL_TYPE, "classic", "deletePage")) ;
    assertNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "deletePage")) ;
    assertFalse(wService.deletePage(PortalConfig.PORTAL_TYPE, "classic", "WikiHome")) ;
  }


  public void testRenamePage() throws WikiException {
    Wiki portalWiki = wService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    wService.createPage(portalWiki, "WikiHome", new Page("currentPage", "currentPage")) ;
    assertTrue(wService.renamePage(PortalConfig.PORTAL_TYPE, "classic", "currentPage", "renamedPage", "renamedPage")) ;
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "renamedPage")) ;
  }

  public void testSearchContent() throws Exception {
    Wiki classicWiki = wService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    Page kspage = new Page("knowledge suite 1", "knowledge suite 1");
    kspage.setContent("forum faq wiki");
    wService.createPage(classicWiki, "WikiHome", kspage);

    Wiki extWiki = wService.createWiki(PortalConfig.PORTAL_TYPE, "ext");
    Page ksExtPage = new Page("knowledge suite 2", "knowledge suite 2");
    ksExtPage.setContent("forum faq wiki");
    wService.createPage(extWiki, "WikiHome", ksExtPage);

    Wiki demoWiki = wService.createWiki(PortalConfig.USER_TYPE, "demo");
    Page ksSocialPage = new Page("knowledge suite", "knowledge suite");
    ksSocialPage.setContent("forum faq wiki");
    wService.createPage(demoWiki, "WikiHome", ksSocialPage);

    Page csPage = new Page("collaboration suite", "collaboration suite");
    csPage.setContent("calendar mail contact chat");
    wService.createPage(classicWiki, "WikiHome", csPage);

    Wiki guestWiki = wService.createWiki(PortalConfig.GROUP_TYPE, "/platform/guests");
    Page guestPage = new Page("Guest page", "Guest page");
    guestPage.setContent("Playground");
    wService.createPage(guestWiki, "WikiHome", guestPage);

    // fulltext search
    WikiSearchData data = new WikiSearchData(null, "suite", "portal", "classic");
    PageList<SearchResult> result = wService.search(data);
    assertEquals(0, result.getAll().size());

    data = new WikiSearchData(null, "forum", "portal", "classic");
    result = wService.search(data);
    assertEquals(1, result.getAll().size());

    data = new WikiSearchData("suite", "suite", "portal", null);

    result = wService.search(data);
    assertEquals(3, result.getAll().size());

    data = new WikiSearchData("suite", "suite", null, null);
    result = wService.search(data);
    assertEquals(4, result.getAll().size());

    // title search
    data = new WikiSearchData("knowledge", null, "portal", "classic");
    result = wService.search(data);
    assertEquals(1, result.getAll().size());

    data = new WikiSearchData("collaboration", null, "portal", "classic");
    result = wService.search(data);
    assertEquals(1, result.getAll().size());

    data = new WikiSearchData("knowledge", null, "portal", null);
    result = wService.search(data);
    assertEquals(2, result.getAll().size());

    data = new WikiSearchData("knowledge", null, null, null);
    result = wService.search(data);
    assertEquals(3, result.getAll().size());

    data = new WikiSearchData("Playground", "Playground", PortalConfig.GROUP_TYPE, "/platform/guests");
    result = wService.search(data);
    assertEquals(1, result.getAll().size());

    data = new WikiSearchData("forum", "forum", PortalConfig.USER_TYPE, null);
    result = wService.search(data);
    assertEquals(1, result.getAll().size());

    data = new WikiSearchData("forum", "forum", PortalConfig.USER_TYPE, "demo");
    result = wService.search(data);
    assertEquals(1, result.getAll().size());
  }

  public void testSearch() throws Exception {
    Wiki wiki = wService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    Page kspage = new Page("test search service", "test search service");
    kspage.setContent("forum faq wiki exoplatform");
    wService.createPage(wiki, "WikiHome", kspage) ;

    Wiki wikiExt = wService.createWiki(PortalConfig.PORTAL_TYPE, "ext");
    Page extPage = new Page("test search service ext", "test search service ext");
    extPage.setContent("forum faq wiki exoplatform");
    wService.createPage(wikiExt, "WikiHome", extPage) ;

    Attachment attachment = new Attachment();
    attachment.setName("attachment1.txt");
    attachment.setContent("exoplatform content mamagement".getBytes());
    attachment.setCreator("you") ;
    attachment.setMimeType("text/plain"); ;
    wService.addAttachmentToPage(attachment, extPage);

    Wiki groupWiki = wService.createWiki(PortalConfig.GROUP_TYPE, "/platform/guests");
    Page guestPage = new Page("guest platform", "guest platform");
    guestPage.setContent("exoplatform");
    wService.createPage(groupWiki, "WikiHome", guestPage);

    Wiki userWiki = wService.createWiki(PortalConfig.USER_TYPE, "demo");
    Page userPage = new Page("demo", "demo");
    userPage.setContent("exoplatform");
    wService.createPage(userWiki, "WikiHome", userPage);

    WikiSearchData data = new WikiSearchData("exoplatform", "exoplatform", null, null);

    PageList<SearchResult> result = wService.search(data);
    assertEquals(4, result.getAll().size());

    data = new WikiSearchData("exoplatform", "exoplatform", "portal",null) ;
    result = wService.search(data) ;
    assertEquals(2, result.getAll().size()) ;

    data = new WikiSearchData("exoplatform", "exoplatform", "portal", "classic");

    result = wService.search(data);
    assertEquals(1, result.getAll().size());

    data = new WikiSearchData("exoplatform", "exoplatform", PortalConfig.GROUP_TYPE, null);
    result = wService.search(data);
    assertEquals(1, result.getAll().size());

    data = new WikiSearchData("exoplatform", "exoplatform", PortalConfig.GROUP_TYPE, "/platform/guests");
    result = wService.search(data);
    assertEquals(1, result.getAll().size());

    data = new WikiSearchData("exoplatform", "exoplatform", PortalConfig.USER_TYPE, null);
    result = wService.search(data);
    assertEquals(1, result.getAll().size());

    data = new WikiSearchData("exoplatform", "exoplatform", PortalConfig.USER_TYPE, "demo");
    result = wService.search(data);
    assertEquals(1, result.getAll().size());
  }

  public void testSearchTitle() throws Exception {
    wService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    wService.createWiki(PortalConfig.GROUP_TYPE, "/platform/users");
    wService.createWiki(PortalConfig.USER_TYPE, "demo");
    wService.createPage(new Wiki(PortalConfig.PORTAL_TYPE, "classic"), "WikiHome", new Page("dumpPage", "dumpPage"));
    wService.createPage(new Wiki(PortalConfig.GROUP_TYPE, "/platform/users"), "WikiHome", new Page("Dump guest Page", "Dump guest Page"));
    wService.createPage(new Wiki(PortalConfig.USER_TYPE, "demo"), "WikiHome", new Page("Dump demo Page", "Dump demo Page"));

    // limit size is 2
    WikiSearchData data = new WikiSearchData("dump", null, null, null);
    data.setLimit(2);
    List<SearchResult> result = wService.search(data).getAll();
    assertEquals(2, result.size());
    // limit size is 10
    data.setLimit(10);
    result = wService.search(data).getAll();
    assertEquals(2, result.size());
    // not limit size
    data= new WikiSearchData("dump", null, "portal", "classic");
    result = wService.search(data).getAll();
    assertEquals(0, result.size());

    data = new WikiSearchData("dump", null, PortalConfig.GROUP_TYPE, null);
    result = wService.search(data).getAll();
    assertEquals(1, result.size());

    data = new WikiSearchData("dump", null,PortalConfig.GROUP_TYPE, "/platform/users");
    result = wService.search(data).getAll();
    assertEquals(1, result.size());

    data = new WikiSearchData("dump", null, PortalConfig.USER_TYPE, null);
    result = wService.search(data).getAll();
    assertEquals(1, result.size());

    data = new WikiSearchData("dump", null, PortalConfig.USER_TYPE, "demo");
    result = wService.search(data).getAll();
    assertEquals(1, result.size());
  }

  public void testAddAttachment() throws WikiException {
    Wiki wiki = wService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    Page page = new Page("AddAttachment", "AddAttachment");
    page = wService.createPage(wiki, "WikiHome", page);
    Attachment attachment = new Attachment();
    attachment.setName("attachment1.txt");
    attachment.setContent("foo".getBytes());
    attachment.setCreator("you");
    attachment.setMimeType("text/plain");
    wService.addAttachmentToPage(attachment, page);

    page = wService.getPageOfWikiByName(wiki.getType(), wiki.getOwner(), page.getName());
    assertNotNull(page);
    List<Attachment> attachments = wService.getAttachmentsOfPage(page);
    assertNotNull(attachments);
    assertEquals(1, attachments.size());
    assertEquals("foo", new String(attachments.get(0).getContent()));
    assertNotNull(attachments.get(0).getDownloadURL());
    assertEquals("/portal/rest/jcr/repository/collaboration/exo:applications/eXoWiki/wikis/classic/WikiHome/AddAttachment/attachment1.txt", attachments.get(0).getDownloadURL());
  }

  public void testAddImageAttachment() throws WikiException, IOException {
    Wiki wiki = wService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    Page page = new Page("AddImageAttachment", "AddImageAttachment");
    page = wService.createPage(wiki, "WikiHome", page);
    Attachment attachment = new Attachment();
    attachment.setName("John.png");
    InputStream imageInputStream = this.getClass().getClassLoader().getResourceAsStream("images/John.png");
    byte[] content = IOUtils.toByteArray(imageInputStream);
    attachment.setContent(content);
    attachment.setCreator("you");
    attachment.setMimeType("image/png");
    wService.addAttachmentToPage(attachment, page);

    page = wService.getPageOfWikiByName(wiki.getType(), wiki.getOwner(), page.getName());
    assertNotNull(page);
    List<Attachment> attachments = wService.getAttachmentsOfPage(page);
    assertNotNull(attachments);
    assertEquals(1, attachments.size());
    byte[] content1 = attachments.get(0).getContent();
    assertTrue(Arrays.equals(content, content1));
    assertNotNull(attachments.get(0).getDownloadURL());
  }

  public void testAddEmotionIcons() throws WikiException, IOException {
    EmotionIcon emotionIcon = new EmotionIcon();
    emotionIcon.setName("thumb_up.gif");
    InputStream emotionIconInputStream = this.getClass().getClassLoader().getResourceAsStream("images/thumb_up.gif");
    byte[] emotionIconImage = IOUtils.toByteArray(emotionIconInputStream);
    emotionIcon.setImage(emotionIconImage);
    wService.createEmotionIcon(emotionIcon);

    EmotionIcon emotionIconThumbUp = wService.getEmotionIconByName("thumb_up.gif");
    assertNotNull(emotionIconThumbUp);
    assertEquals("thumb_up.gif", emotionIconThumbUp.getName());
    assertEquals("/portal/rest/jcr/repository/collaboration/exo:applications/eXoWiki/wikimetadata/EmotionIconsPage/thumb_up.gif", emotionIconThumbUp.getUrl());
  }

  public void testGetSyntaxPage() throws WikiException {
    Page syntaxSmallPage = wService.getHelpSyntaxPage(Syntax.XWIKI_2_0.toIdString(), false);
    assertNotNull(syntaxSmallPage);
    Page syntaxFullPage = wService.getHelpSyntaxPage(Syntax.XWIKI_2_0.toIdString(), true);
    assertNotNull(syntaxFullPage);
  }

  public void testBrokenLink() throws WikiException {
    Wiki wiki = new Wiki(PortalConfig.PORTAL_TYPE, "classic");
    wService.createPage(wiki, "WikiHome", new Page("OriginalParentPage1", "OriginalParentPage1"));
    wService.createPage(wiki, "OriginalParentPage1", new Page("OriginalPage", "OriginalPage"));
    Page relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage");
    assertEquals("OriginalPage", relatedPage.getName());
    wService.renamePage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage", "RenamedOriginalPage", "RenamedOriginalPage");
    relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage");
    assertEquals("RenamedOriginalPage", relatedPage.getName());
    wService.renamePage(PortalConfig.PORTAL_TYPE, "classic", "RenamedOriginalPage", "RenamedOriginalPage2", "RenamedOriginalPage2");
    relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage");
    assertEquals("RenamedOriginalPage2", relatedPage.getName());
    WikiPageParams currentPageParams= new WikiPageParams();
    currentPageParams.setPageName("RenamedOriginalPage2");
    currentPageParams.setOwner("classic");
    currentPageParams.setType(PortalConfig.PORTAL_TYPE);
    WikiPageParams newPageParams= new WikiPageParams();
    newPageParams.setPageName("WikiHome");
    newPageParams.setOwner("classic");
    newPageParams.setType(PortalConfig.PORTAL_TYPE);
    wService.movePage(currentPageParams,newPageParams);
    relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage");
    assertEquals("RenamedOriginalPage2", relatedPage.getName());
    wService.renamePage(PortalConfig.PORTAL_TYPE, "classic", "RenamedOriginalPage2", "RenamedOriginalPage3", "RenamedOriginalPage3");
    relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage");
    assertEquals("RenamedOriginalPage3", relatedPage.getName());
    wService.createPage(new Wiki(PortalConfig.GROUP_TYPE, "platform/users"), "WikiHome", new Page("OriginalParentPag2", "OriginalParentPage2"));
    // Move RenamedOriginalPage3 from portal type to group type
    currentPageParams.setPageName("RenamedOriginalPage3");
    currentPageParams.setOwner("classic");
    currentPageParams.setType(PortalConfig.PORTAL_TYPE);
    newPageParams.setPageName("OriginalParentPage2");
    newPageParams.setOwner("platform/users");
    newPageParams.setType(PortalConfig.GROUP_TYPE);
    //
    wService.movePage(currentPageParams,newPageParams);
    relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage");
    assertEquals("RenamedOriginalPage3", relatedPage.getName());
    wService.deletePage(PortalConfig.GROUP_TYPE, "platform/users", "RenamedOriginalPage3");
    assertNull(wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage"));
  }

  public void testCircularRename() throws WikiException {
    wService.createPage(new Wiki(PortalConfig.PORTAL_TYPE, "classic"), "WikiHome", new Page("CircularRename1", "CircularRename1"));
    Page relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "CircularRename1");
    assertEquals("CircularRename1", relatedPage.getName());
    wService.renamePage(PortalConfig.PORTAL_TYPE, "classic", "CircularRename1", "CircularRename2", "CircularRename2");
    relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "CircularRename1");
    assertEquals("CircularRename2", relatedPage.getName());
    // Do a circular rename
    wService.renamePage(PortalConfig.PORTAL_TYPE, "classic", "CircularRename2", "CircularRename1", "CircularRename1");
    relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "CircularRename1");
    assertEquals("CircularRename1", relatedPage.getName());
    relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "CircularRename2");
    assertNull(relatedPage);
  }

  public void testDraftPage() throws WikiException {
    startSessionAs("mary");
    
    // Get wiki home
    Page wikiHome = wService.createWiki(PortalConfig.PORTAL_TYPE, "testDraftPage").getWikiHome();

    // Test create draft for new page
    DraftPage draftPage = wService.createDraftForNewPage(new DraftPage(), wikiHome, new Date().getTime());
    assertNotNull(draftPage);
    String draftNameForNewPage = draftPage.getName();
    assertTrue(draftPage.isNewPage());
    assertEquals(wikiHome.getId(), draftPage.getTargetPageId());
    assertEquals("1", draftPage.getTargetPageRevision());
    
    // Test get draft by draft name
    DraftPage draftPage1 = wService.getDraft(draftNameForNewPage);
    assertNotNull(draftPage1);
    assertEquals(draftPage.isNewPage(), draftPage1.isNewPage());
    assertEquals(draftPage.getTargetPageId(), draftPage1.getTargetPageId());
    assertEquals(draftPage.getTargetPageRevision(), draftPage1.getTargetPageRevision());
    
    // Create a wiki page for test
    Page page = new Page("new page", "new page");
    page.setContent("Page content");
    page = wService.createPage(new Wiki(PortalConfig.PORTAL_TYPE, "testDraftPage"), "WikiHome", page);

    // update it and create a version
    page.setContent("Page content updated");
    wService.updatePage(page, PageUpdateType.EDIT_PAGE_CONTENT);
    wService.createVersionOfPage(page);

    // Test create draft for exist wiki page
    WikiPageParams param = new WikiPageParams(PortalConfig.PORTAL_TYPE, "testDraftPage", page.getName());
    DraftPage draftPage2 = wService.createDraftForExistPage(new DraftPage(), page, null, new Date().getTime());
    assertNotNull(draftPage2);
    assertFalse(draftPage2.isNewPage());
    assertEquals(page.getId(), draftPage2.getTargetPageId());
    assertEquals("2", draftPage2.getTargetPageRevision());
    
    // Test get draft for exist wiki page
    DraftPage draftPage3 = wService.getDraftOfPage(page);
    assertNotNull(draftPage3);
    assertFalse(draftPage3.isNewPage());
    assertEquals(page.getId(), draftPage3.getTargetPageId());
    assertEquals("2", draftPage3.getTargetPageRevision());
    
    // Test list draft by user
    List<DraftPage> drafts = wService.getDraftsOfUser("mary");
    assertNotNull(drafts);
    assertEquals(2, drafts.size());
    
    // Test remove draft of wiki page
    wService.removeDraftOfPage(param);
    assertNull(wService.getDraftOfPage(page));
    
    // Test remove draft by draft name
    wService.removeDraft(draftNameForNewPage);
    assertNull(wService.getDraft(draftNameForNewPage));
    
    // Test list draft by user
    drafts = wService.getDraftsOfUser("mary");
    assertNotNull(drafts);
    assertEquals(0, drafts.size());
  }
}
