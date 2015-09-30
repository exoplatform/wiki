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


import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.mow.core.api.wiki.Model;
import org.exoplatform.wiki.mow.core.api.AbstractMOWTestcase;
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.mow.core.api.wiki.GroupWiki;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PortalWiki;
import org.exoplatform.wiki.mow.core.api.wiki.UserWiki;
import org.exoplatform.wiki.mow.core.api.wiki.WikiContainer;
import org.exoplatform.wiki.mow.core.api.wiki.WikiHome;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.xwiki.rendering.syntax.Syntax;

@SuppressWarnings("deprecation")
public class TestWikiService extends AbstractMOWTestcase {
  private WikiService wService ; 
  public void setUp() throws Exception{
    super.setUp() ;
    wService = container.getComponentInstanceOfType(WikiService.class) ;
  }
  
  public void testWikiService() throws Exception{
    assertNotNull(wService) ;
  }

  public void testCreateWiki() throws Exception {
    Wiki wiki = wService.getWikiByTypeAndOwner(PortalConfig.PORTAL_TYPE, "wiki1");
    assertNotNull(wiki);

    //wService.createWiki(WikiType.PORTAL.toString(), "wiki1");

    //wiki = wService.getWikiByTypeAndOwner(WikiType.PORTAL.toString(), "wiki1");
    //assertNotNull(wiki);

  }

  public void testGetPortalPageById() throws Exception {
    Model model = mowService.getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
    PortalWiki wiki = portalWikiContainer.addWiki("classic");
    PortalWiki wikiACME = portalWikiContainer.addWiki("acme");
    wikiACME.getWikiHome() ;
    WikiHome wikiHomePage = wiki.getWikiHome();
    model.save() ;
    
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "WikiHome")) ;
        
    PageImpl wikipage = wiki.createWikiPage();
    wikipage.setName("testGetPortalPageById-001");
    wikiHomePage.addWikiPage(wikipage);
    model.save() ;
    
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "testGetPortalPageById-001")) ;
    
  }

  public void testGetGroupPageById() throws Exception{

    Model model = mowService.getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    WikiContainer<GroupWiki> groupWikiContainer = wStore.getWikiContainer(WikiType.GROUP);
    GroupWiki wiki = groupWikiContainer.addWiki("platform/users");
    WikiHome wikiHomePage = wiki.getWikiHome();
    model.save() ;

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.GROUP_TYPE, "platform/users", "WikiHome")) ;

    PageImpl wikipage = wiki.createWikiPage();
    wikipage.setName("testGetGroupPageById-001");
    wikiHomePage.addWikiPage(wikipage);
    model.save() ;

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.GROUP_TYPE, "platform/users", "testGetGroupPageById-001")) ;
    assertNull(wService.getPageOfWikiByName(PortalConfig.GROUP_TYPE, "unknown", "WikiHome"));
  }

  public void testGetUserPageById() throws Exception{

    Model model = mowService.getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    WikiContainer<UserWiki> userWikiContainer = wStore.getWikiContainer(WikiType.USER);
    UserWiki wiki = userWikiContainer.addWiki("john");
    WikiHome wikiHomePage = wiki.getWikiHome();
    model.save() ;

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.USER_TYPE, "john", "WikiHome")) ;

    PageImpl wikipage = wiki.createWikiPage();
    wikipage.setName("testGetUserPageById-001");
    wikiHomePage.addWikiPage(wikipage);
    model.save() ;

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.USER_TYPE, "john", "testGetUserPageById-001")) ;
    assertNull(wService.getPageOfWikiByName(PortalConfig.USER_TYPE, "unknown", "WikiHome"));
  }

  public void testCreatePageAndSubPage() throws Exception{
    Wiki wiki = new Wiki(PortalConfig.PORTAL_TYPE, "classic");
    wService.createPage(wiki, "WikiHome", new Page("parentPage", "parentPage")) ;
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "parentPage")) ;
    wService.createPage(wiki, "parentPage", new Page("childPage", "childPage")) ;
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "childPage")) ;
  }

  public void testCreateTemplatePage() throws Exception {
    Model model = mowService.getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
    PortalWiki wiki = portalWikiContainer.addWiki("classic");
    model.save();

    WikiPageParams params = new WikiPageParams(PortalConfig.PORTAL_TYPE, "classic", null);
    wService.createTemplatePage("SampleChart", params);
    assertNotNull(wService.getTemplatePage(params, "SampleChart"));
  }

  public void testGetBreadcumb() throws Exception {
    Wiki portalWiki = new Wiki(PortalConfig.PORTAL_TYPE, "classic");
    wService.createPage(portalWiki, "WikiHome", new Page("Breadcumb1", "Breadcumb1")) ;
    wService.createPage(portalWiki, "Breadcumb1", new Page("Breadcumb2", "Breadcumb2")) ;
    wService.createPage(portalWiki, "Breadcumb2", new Page("Breadcumb3", "Breadcumb3")) ;
    List<BreadcrumbData> breadCumbs = wService.getBreadcumb(PortalConfig.PORTAL_TYPE, "classic", "Breadcumb3");
    assertEquals(4, breadCumbs.size());
    assertEquals("WikiHome", breadCumbs.get(0).getId());
    assertEquals("Breadcumb1", breadCumbs.get(1).getId());
    assertEquals("Breadcumb2", breadCumbs.get(2).getId());
    assertEquals("Breadcumb3", breadCumbs.get(3).getId());
    Wiki groupWiki = new Wiki(PortalConfig.GROUP_TYPE, "platform/users");
    wService.createPage(groupWiki, "WikiHome", new Page("GroupBreadcumb1", "GroupBreadcumb1")) ;
    wService.createPage(groupWiki, "GroupBreadcumb1", new Page("GroupBreadcumb2", "GroupBreadcumb2")) ;
    wService.createPage(groupWiki, "GroupBreadcumb2", new Page("GroupBreadcumb3", "GroupBreadcumb3")) ;
    breadCumbs = wService.getBreadcumb(PortalConfig.GROUP_TYPE, "platform/users", "GroupBreadcumb3");
    assertEquals(4, breadCumbs.size());
    assertEquals("WikiHome", breadCumbs.get(0).getId());
    assertEquals("GroupBreadcumb1", breadCumbs.get(1).getId());
    assertEquals("GroupBreadcumb2", breadCumbs.get(2).getId());
    assertEquals("GroupBreadcumb3", breadCumbs.get(3).getId());
    Wiki userWiki = new Wiki(PortalConfig.USER_TYPE, "john");
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

  public void testMovePage() throws Exception{
    //moving page in same space
    Wiki portalWiki = new Wiki(PortalConfig.PORTAL_TYPE, "classic");
    wService.createPage(portalWiki, "WikiHome", new Page("oldParent", "oldParent")) ;
    wService.createPage(portalWiki, "oldParent", new Page("child", "child")) ;
    wService.createPage(portalWiki, "WikiHome", new Page("newParent", "newParent")) ;

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "oldParent")) ;
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "child")) ;
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "newParent")) ;

    WikiPageParams currentLocationParams= new WikiPageParams();
    WikiPageParams newLocationParams= new WikiPageParams();
    currentLocationParams.setPageId("child");
    currentLocationParams.setType(PortalConfig.PORTAL_TYPE);
    currentLocationParams.setOwner("classic");
    newLocationParams.setPageId("newParent");
    newLocationParams.setType(PortalConfig.PORTAL_TYPE);
    newLocationParams.setOwner("classic");

    assertTrue(wService.movePage(currentLocationParams,newLocationParams)) ;

    //moving page from different spaces
    Wiki userWiki = new Wiki(PortalConfig.USER_TYPE, "demo");
    wService.createPage(userWiki, "WikiHome", new Page("acmePage", "acmePage")) ;
    wService.createPage(portalWiki, "WikiHome", new Page("classicPage", "classicPage")) ;

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.USER_TYPE, "demo", "acmePage")) ;
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "classicPage")) ;

    currentLocationParams.setPageId("acmePage");
    currentLocationParams.setType(PortalConfig.USER_TYPE);
    currentLocationParams.setOwner("demo");
    newLocationParams.setPageId("classicPage");
    newLocationParams.setType(PortalConfig.PORTAL_TYPE);
    newLocationParams.setOwner("classic");
    assertTrue(wService.movePage(currentLocationParams,newLocationParams)) ;

    // moving a page to another read-only page
    wService.createPage(new Wiki(PortalConfig.PORTAL_TYPE, "demo"), "WikiHome", new Page("toMovedPage", "toMovedPage"));
    Page page = wService.createPage(userWiki, "WikiHome", new Page("privatePage", "privatePage"));
    HashMap<String, String[]> permissionMap = new HashMap<>();
    permissionMap.put("any", new String[] {PermissionType.VIEWPAGE.toString(), PermissionType.EDITPAGE.toString()});
    page.setPermission(permissionMap);

    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "demo", "toMovedPage"));
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.USER_TYPE, "demo", "privatePage"));

    currentLocationParams.setPageId("toMovedPage");
    currentLocationParams.setType(PortalConfig.PORTAL_TYPE);
    currentLocationParams.setOwner("demo");
    newLocationParams.setPageId("privatePage");
    newLocationParams.setType(PortalConfig.USER_TYPE);
    newLocationParams.setOwner("demo");

    startSessionAs("mary");

    assertFalse(wService.movePage(currentLocationParams, newLocationParams));
  }

  public void testDeletePage() throws Exception{
    Wiki portalWiki = new Wiki(PortalConfig.PORTAL_TYPE, "classic");
    wService.createPage(portalWiki, "WikiHome", new Page("deletePage", "deletePage")) ;
    assertTrue(wService.deletePage(PortalConfig.PORTAL_TYPE, "classic", "deletePage")) ;
    //wait(10) ;
    wService.createPage(portalWiki, "WikiHome", new Page("deletePage", "deletePage")) ;
    assertTrue(wService.deletePage(PortalConfig.PORTAL_TYPE, "classic", "deletePage")) ;    
    assertNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "deletePage")) ;
    assertFalse(wService.deletePage(PortalConfig.PORTAL_TYPE, "classic", "WikiHome")) ;
  }
  
  public void testDeleteTemplatePage() throws Exception{
    WikiPageParams params = new WikiPageParams(PortalConfig.PORTAL_TYPE, "classic", null);
    wService.createTemplatePage("SampleExcel", params);
    assertNotNull(wService.getTemplatePage(params, "SampleExcel"));
    wService.deleteTemplatePage(PortalConfig.PORTAL_TYPE, "classic", "SampleExcel");
    assertNull(wService.getTemplatePage(params, "SampleExcel"));
  }

  public void testRenamePage() throws Exception{
    Wiki portalWiki = new Wiki(PortalConfig.PORTAL_TYPE, "classic");
    wService.createPage(portalWiki, "WikiHome", new Page("currentPage", "currentPage")) ;
    assertTrue(wService.renamePage(PortalConfig.PORTAL_TYPE, "classic", "currentPage", "renamedPage", "renamedPage")) ;
    assertNotNull(wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "renamedPage")) ;
  }

  public void testSearchRenamedPage() throws Exception{
    Wiki portalWiki = new Wiki(PortalConfig.PORTAL_TYPE, "classic");
    Page page = wService.createPage(portalWiki, "WikiHome", new Page("Page", "Page"));
    page.getContent().setText("This is a rename page test");
    RequestLifeCycle.end();
    RequestLifeCycle.begin(container);
    assertTrue(wService.renamePage(PortalConfig.PORTAL_TYPE, "classic", "Page", "Page01", "Page01"));
    RequestLifeCycle.end();
    RequestLifeCycle.begin(container);
    assertEquals(1, wService.searchRenamedPage(PortalConfig.PORTAL_TYPE, "classic", "Page").size());

    Wiki groupWiki = new Wiki(PortalConfig.GROUP_TYPE, "/platform/guests");
    Page guestPage = wService.createPage(groupWiki, "WikiHome", new Page("Page", "Page"));
    guestPage.getContent().setText("This is a rename guest page test");
    RequestLifeCycle.end();
    RequestLifeCycle.begin(container);
    assertTrue(wService.renamePage(PortalConfig.GROUP_TYPE, "/platform/guests", "Page", "Page01", "Page01"));
    assertEquals(1, wService.searchRenamedPage(PortalConfig.GROUP_TYPE, "/platform/guests", "Page").size());
    RequestLifeCycle.end();
    RequestLifeCycle.begin(container);

    Wiki userWiki = new Wiki(PortalConfig.USER_TYPE, "demo");
    Page demoPage = wService.createPage(userWiki, "WikiHome", new Page("Page", "Page"));
    demoPage.getContent().setText("This is a rename demo page test");
    RequestLifeCycle.end();
    RequestLifeCycle.begin(container);
    assertTrue(wService.renamePage(PortalConfig.USER_TYPE, "demo", "Page", "Page01", "Page01"));
    RequestLifeCycle.end();
    RequestLifeCycle.begin(container);
    assertEquals(1, wService.searchRenamedPage(PortalConfig.USER_TYPE, "demo", "Page").size());
  }

  public void testSearchContent() throws Exception {
    Wiki classicWiki = new Wiki(PortalConfig.PORTAL_TYPE, "classic");
    Page kspage = new Page("knowledge suite", "knowledge suite");
    Attachment ksContent = new Attachment();
    ksContent.setText("forum faq wiki");
    kspage.setContent(ksContent);
    wService.createPage(classicWiki, "WikiHome", kspage);

    Wiki extWiki = new Wiki(PortalConfig.PORTAL_TYPE, "ext");
    Page ksExtPage = new Page("knowledge suite", "knowledge suite");
    Attachment content = new Attachment();
    content.setText("forum faq wiki");
    ksExtPage.setContent(content);
    wService.createPage(extWiki, "WikiHome", ksExtPage);

    Wiki demoWiki = new Wiki(PortalConfig.USER_TYPE, "demo");
    Page ksSocialPage = new Page("knowledge suite", "knowledge suite");
    Attachment ksSocialContent = new Attachment();
    ksSocialContent.setText("forum faq wiki");
    ksSocialPage.setContent(ksSocialContent);
    wService.createPage(demoWiki, "WikiHome", ksSocialPage);

    Page csPage = new Page("collaboration suite", "collaboration suite");
    Attachment csContent = new Attachment();
    csContent.setText("calendar mail contact chat");
    csPage.setContent(csContent);
    wService.createPage(classicWiki, "WikiHome", csPage);

    Wiki guestWiki = new Wiki(PortalConfig.GROUP_TYPE, "/platform/guests");
    Page guestPage = new Page("Guest page", "Guest page");
    Attachment guestContent = new Attachment();
    guestContent.setText("Playground");
    guestPage.setContent(guestContent);
    wService.createPage(guestWiki, "WikiHome", guestPage);

    // fulltext search
    WikiSearchData data = new WikiSearchData(null, "suite", "portal", "classic");
    PageList<SearchResult> result = wService.search(data);
    assertEquals(0, result.getAll().size());

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
    Wiki wiki = new Wiki(PortalConfig.PORTAL_TYPE, "classic");
    Page kspage = new Page("test search service", "test search service");
    Attachment content = new Attachment();
    content.setText("forum faq wiki exoplatform");
    kspage.setContent(content);
    wService.createPage(wiki, "WikiHome", kspage) ;

    Wiki wikiExt = new Wiki(PortalConfig.PORTAL_TYPE, "ext");
    Page extPage = new Page("test search service ext", "test search service ext");
    Attachment extPageContent = new Attachment();
    extPageContent.setText("forum faq wiki exoplatform");
    extPage.setContent(extPageContent);
    wService.createPage(wikiExt, "WikiHome", extPage) ;

    /*
    AttachmentImpl attachment1 = wService.createAttachment("attachment1.txt", Resource.createPlainText("foo")) ;
    attachment1.setCreator("you") ;
    assertEquals(attachment1.getName(), "attachment1.txt") ;
    assertNotNull(attachment1.getContentResource()) ;
    attachment1.setContentResource(Resource.createPlainText("exoplatform content mamagement")) ;
    */

    Wiki groupWiki = new Wiki(PortalConfig.GROUP_TYPE, "/platform/guests");
    Page guestPage = new Page("guest platform", "guest platform");
    Attachment guestPageContent = new Attachment();
    guestPageContent.setText("exoplatform");
    guestPage.setContent(guestPageContent);
    wService.createPage(groupWiki, "WikiHome", guestPage);

    Wiki userWiki = new Wiki(PortalConfig.USER_TYPE, "demo");
    Page userPage = new Page("demo", "demo");
    Attachment userPageContent = new Attachment();
    userPageContent.setText("exoplatform");
    userPage.setContent(userPageContent);
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
    wService.createPage(new Wiki(PortalConfig.PORTAL_TYPE, "classic"), "WikiHome", new Page("dumpPage", "dumpPage"));
    wService.createPage(new Wiki(PortalConfig.GROUP_TYPE, "/platform/guests"), "WikiHome", new Page("Dump guest Page", "Dump guest Page"));
    wService.createPage(new Wiki(PortalConfig.USER_TYPE, "demo"), "WikiHome", new Page("Dump demo Page", "Dump demo Page"));

    /*
    AttachmentImpl attachment1 = kspage.createAttachment("dumpFile.txt", Resource.createPlainText("foo"));
    assertEquals(attachment1.getName(), "dumpFile.txt");
    assertNotNull(attachment1.getContentResource());
    kspage.getChromatticSession().save();
    */

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

    data = new WikiSearchData("dump", null,PortalConfig.GROUP_TYPE, "/platform/guests");
    result = wService.search(data).getAll();
    assertEquals(1, result.size());

    data = new WikiSearchData("dump", null, PortalConfig.USER_TYPE, null);
    result = wService.search(data).getAll();
    assertEquals(1, result.size());

    data = new WikiSearchData("dump", null, PortalConfig.USER_TYPE, "demo");
    result = wService.search(data).getAll();
    assertEquals(1, result.size());
  }

  public void testSearchTemplate() throws Exception {
    WikiPageParams params = new WikiPageParams(PortalConfig.PORTAL_TYPE,  "classic", null);
    wService.createTemplatePage("Sample Search Template", params);
    assertNotNull(wService.getTemplatePage(params, "Sample_Search_Template"));

    params= new WikiPageParams(PortalConfig.GROUP_TYPE,  "/platform/guests", null);
    wService.createTemplatePage("Sample Group Search Template", params);
    assertNotNull(wService.getTemplatePage(params, "Sample_Group_Search_Template"));

    params= new WikiPageParams(PortalConfig.USER_TYPE,  "demo", null);
    wService.createTemplatePage("Sample User Search Template", params);
    assertNotNull(wService.getTemplatePage(params, "Sample_User_Search_Template"));

    TemplateSearchData data = new TemplateSearchData("Template", PortalConfig.PORTAL_TYPE, "classic");
    List<TemplateSearchResult> result = wService.searchTemplate(data);
    assertEquals(1, result.size());

    data = new TemplateSearchData("Template", PortalConfig.GROUP_TYPE, "/platform/guests");
    result = wService.searchTemplate(data);
    assertEquals(1, result.size());

    data = new TemplateSearchData("Template", PortalConfig.USER_TYPE, "demo");
    result = wService.searchTemplate(data);
    assertEquals(1, result.size());
  }
  /*
  */

  // TODO ???
  /*
  public void testGetPageTitleOfAttachment() throws Exception {
    Page kspage = wService.createPage(PortalConfig.PORTAL_TYPE, "classic", "GetPageTitleOfAttachment", "WikiHome") ;
    kspage.getContent().setText("forum faq wiki platform") ;
    AttachmentImpl attachment1 = kspage.createAttachment("attachment1.txt", Resource.createPlainText("foo")) ;
    attachment1.setCreator("you") ;    
    assertEquals(attachment1.getName(), "attachment1.txt") ;
    assertNotNull(attachment1.getContentResource()) ;
    attachment1.setContentResource(Resource.createPlainText("exo platform content mamagement")) ;
    
    assertEquals("GetPageTitleOfAttachment", wService.getPageTitleOfAttachment(attachment1.getJCRContentPath())) ;
    
  }
  
  public void testGetAttachmentAsStream() throws Exception {
    Page kspage = wService.createPage(PortalConfig.PORTAL_TYPE, "classic", "GetAttachmentAsStream", "WikiHome") ;
    kspage.getContent().setText("forum faq wiki platform") ;
    AttachmentImpl attachment1 = kspage.createAttachment("attachment.txt", Resource.createPlainText("this is a text attachment")) ;
    attachment1.setCreator("john") ;    
    assertEquals(attachment1.getName(), "attachment.txt") ;
    assertNotNull(attachment1.getContentResource()) ;    
    assertNotNull(wService.getAttachmentAsStream(attachment1.getPath()+"/jcr:content")) ;    
  }
  */

  public void testGetSyntaxPage() throws Exception {
    Page syntaxPage = wService.getHelpSyntaxPage(Syntax.XWIKI_2_0.toIdString());
    assertNotNull(syntaxPage);
  }

  public void testBrokenLink() throws Exception {
    Wiki wiki = new Wiki(PortalConfig.PORTAL_TYPE, "classic");
    wService.createPage(wiki, "WikiHome", new Page("OriginalParentPage", "OriginalParentPage"));
    wService.createPage(wiki, "OriginalParentPage", new Page("OriginalPage", "OriginalPage"));
    Page relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage");
    assertEquals("OriginalPage", relatedPage.getName());
    wService.renamePage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage", "RenamedOriginalPage", "RenamedOriginalPage");
    relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage");
    assertEquals("RenamedOriginalPage", relatedPage.getName());
    wService.renamePage(PortalConfig.PORTAL_TYPE, "classic", "RenamedOriginalPage", "RenamedOriginalPage2", "RenamedOriginalPage2");
    relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage");
    assertEquals("RenamedOriginalPage2", relatedPage.getName());
    WikiPageParams currentPageParams= new WikiPageParams();
    currentPageParams.setPageId("RenamedOriginalPage2");
    currentPageParams.setOwner("classic");
    currentPageParams.setType(PortalConfig.PORTAL_TYPE);
    WikiPageParams newPageParams= new WikiPageParams();
    newPageParams.setPageId("WikiHome");
    newPageParams.setOwner("classic");
    newPageParams.setType(PortalConfig.PORTAL_TYPE);
    wService.movePage(currentPageParams,newPageParams);
    relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage");
    assertEquals("RenamedOriginalPage2", relatedPage.getName());
    wService.renamePage(PortalConfig.PORTAL_TYPE, "classic", "RenamedOriginalPage2", "RenamedOriginalPage3", "RenamedOriginalPage3");
    relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage");
    assertEquals("RenamedOriginalPage3", relatedPage.getName());
    wService.createPage(new Wiki(PortalConfig.GROUP_TYPE, "platform/users"), "WikiHome", new Page("OriginalParentPage", "OriginalParentPage"));
    // Move RenamedOriginalPage3 from portal type to group type
    currentPageParams.setPageId("RenamedOriginalPage3");
    currentPageParams.setOwner("classic");
    currentPageParams.setType(PortalConfig.PORTAL_TYPE);
    newPageParams.setPageId("OriginalParentPage");
    newPageParams.setOwner("platform/users");
    newPageParams.setType(PortalConfig.GROUP_TYPE);
    //
    wService.movePage(currentPageParams,newPageParams);
    relatedPage = wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage");
    assertEquals("RenamedOriginalPage3", relatedPage.getName());
    wService.deletePage(PortalConfig.GROUP_TYPE, "platform/users", "RenamedOriginalPage3");
    assertNull(wService.getRelatedPage(PortalConfig.PORTAL_TYPE, "classic", "OriginalPage"));
  }

  public void testCircularRename() throws Exception {
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

  public void testDraftPage() throws Exception {
    startSessionAs("mary");
    
    // Get wiki home
    Page wikiHome = wService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "classic", "WikiHome");
    
    // Test create draft for new page
    DraftPage draftPage = wService.createDraftForNewPage(new WikiPageParams(PortalConfig.PORTAL_TYPE, "classic", "WikiHome"), new Date().getTime());
    assertNotNull(draftPage);
    String draftNameForNewPage = draftPage.getName();
    assertTrue(draftPage.isNewPage());
    assertEquals(wikiHome.getId(), draftPage.getTargetPage());
    assertEquals("1", draftPage.getTargetRevision());
    
    // Test get draft by draft name
    DraftPage draftPage1 = wService.getDraft(draftNameForNewPage);
    assertNotNull(draftPage1);
    assertEquals(draftPage.isNewPage(), draftPage1.isNewPage());
    assertEquals(draftPage.getTargetPage(), draftPage1.getTargetPage());
    assertEquals(draftPage.getTargetRevision(), draftPage1.getTargetRevision());
    
    // Create a wiki page for test
    Page page = new Page("new page", "new page");
    Attachment content = new Attachment();
    content.setText("Page content");
    page.setContent(content);
    page = wService.createPage(new Wiki(PortalConfig.PORTAL_TYPE, "classic"), "WikiHome", page);

    // update it and create a version
    page.getContent().setText("Page content updated");
    wService.updatePage(page);
    wService.createVersionOfPage(page);

    // Test create draft for exist wiki page
    WikiPageParams param = new WikiPageParams(PortalConfig.PORTAL_TYPE, "classic", page.getName());
    DraftPage draftPage2 = wService.createDraftForExistPage(param, null, new Date().getTime());
    assertNotNull(draftPage2);
    assertFalse(draftPage2.isNewPage());
    assertEquals(page.getId(), draftPage2.getTargetPage());
    assertEquals("2", draftPage2.getTargetRevision());
    
    // Test get draft for exist wiki page
    DraftPage draftPage3 = wService.getDraftOfPage(page);
    assertNotNull(draftPage3);
    assertFalse(draftPage3.isNewPage());
    assertEquals(page.getId(), draftPage3.getTargetPage());
    assertEquals("2", draftPage3.getTargetRevision());
    
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
