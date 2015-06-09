package org.exoplatform.wiki.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.Model;
import org.exoplatform.wiki.mow.core.api.AbstractMOWTestcase;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.utils.Utils;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class TestWikiServiceWithSpecialCharacters extends AbstractMOWTestcase {
  private WikiService wService;

  public void setUp() throws Exception {
    super.setUp();
    wService = (WikiService) container.getComponentInstanceOfType(WikiService.class);
  }
  
  public void testWikiService() throws Exception{
    assertNotNull(wService) ;
  }
  
  public static final String[] INVALID_CHARACTERS  = { "~", "!", "@", "#", "$", "%", "^", "&", "*",
      "(", ")", "+", "=", "?", "<", ">", "'", "\"" };

  private String pageTitle = "";
  private String templatePageTitle = "";
  private String subPageTitle = "";
  
  public void testCreatePage() throws Exception {
    for(int i =0; i< INVALID_CHARACTERS.length; i++) {
      pageTitle = "test"+INVALID_CHARACTERS[i]+"character";
      subPageTitle = "testSub"+INVALID_CHARACTERS[i]+"character";
      templatePageTitle = "testTemplate"+INVALID_CHARACTERS[i]+"character";
      // IN PORTAL
      wService.createPage(PortalConfig.PORTAL_TYPE, "classic", pageTitle, "WikiHome") ;
      assertNotNull(wService.getPageById(PortalConfig.PORTAL_TYPE, "classic", Utils.escapeIllegalJcrChars(pageTitle))) ;
      wService.createPage(PortalConfig.PORTAL_TYPE, "classic", subPageTitle, pageTitle) ;
      assertNotNull(wService.getPageById(PortalConfig.PORTAL_TYPE, "classic", Utils.escapeIllegalJcrChars(subPageTitle))) ;
      WikiPageParams paramPortal = new WikiPageParams(PortalConfig.PORTAL_TYPE, "classic", null) ;
      wService.createTemplatePage(templatePageTitle, paramPortal);
      assertNotNull(wService.getTemplatePage(paramPortal, Utils.escapeIllegalJcrChars(templatePageTitle)));
      
      // IN SPACE
      wService.createPage(PortalConfig.GROUP_TYPE, "platform/users", pageTitle, "WikiHome") ;
      assertNotNull(wService.getPageById(PortalConfig.GROUP_TYPE, "platform/users", Utils.escapeIllegalJcrChars(pageTitle))) ;
      wService.createPage(PortalConfig.GROUP_TYPE, "platform/users", subPageTitle, pageTitle) ;
      assertNotNull(wService.getPageById(PortalConfig.GROUP_TYPE, "platform/users", Utils.escapeIllegalJcrChars(subPageTitle))) ;
      WikiPageParams paramSpace = new WikiPageParams(PortalConfig.GROUP_TYPE, "platform/users", null) ; 
      wService.createTemplatePage(templatePageTitle, paramSpace);
      assertNotNull(wService.getTemplatePage(paramPortal, Utils.escapeIllegalJcrChars(templatePageTitle)));
      
      // IN USER
      wService.createPage(PortalConfig.USER_TYPE, "john", pageTitle, "WikiHome") ;
      assertNotNull(wService.getPageById(PortalConfig.USER_TYPE, "john", Utils.escapeIllegalJcrChars(pageTitle))) ;
      wService.createPage(PortalConfig.USER_TYPE, "john", subPageTitle, pageTitle) ;
      assertNotNull(wService.getPageById(PortalConfig.USER_TYPE, "john", Utils.escapeIllegalJcrChars(subPageTitle))) ;
      WikiPageParams paramUser = new WikiPageParams(PortalConfig.USER_TYPE, "john", null) ; 
      wService.createTemplatePage(templatePageTitle, paramUser);
      assertNotNull(wService.getTemplatePage(paramUser, Utils.escapeIllegalJcrChars(templatePageTitle)));
    }
  }
  public void testRelatedPage() throws Exception {
    for(int i =0; i< INVALID_CHARACTERS.length; i++) {
      pageTitle = "testRelated"+INVALID_CHARACTERS[i]+"character";
      wService.createPage(PortalConfig.GROUP_TYPE, "platform/users", pageTitle, "WikiHome") ;
      wService.createPage(PortalConfig.USER_TYPE, "john", pageTitle, "WikiHome") ;
      wService.createPage(PortalConfig.PORTAL_TYPE, "classic", pageTitle, "WikiHome") ;
      //Add related pages
      WikiPageParams orginaryPageParams = new WikiPageParams(PortalConfig.GROUP_TYPE, "platform/users", pageTitle);
      WikiPageParams relatedPageParams_1 = new WikiPageParams(PortalConfig.PORTAL_TYPE, "classic", pageTitle);
      WikiPageParams relatedPageParams_2 = new WikiPageParams(PortalConfig.USER_TYPE, "john", pageTitle);
      wService.addRelatedPage(orginaryPageParams, relatedPageParams_1);
      ArrayList<PageImpl> relatedPages_1 = new ArrayList<PageImpl>();
      relatedPages_1.add((PageImpl) wService.getPageById(PortalConfig.PORTAL_TYPE, "classic", pageTitle));
      assertEquals(relatedPages_1, wService.getRelatedPage(orginaryPageParams));
      wService.removeRelatedPage(orginaryPageParams, relatedPageParams_1);
      assertEquals(new ArrayList<PageImpl>(), wService.getRelatedPage(orginaryPageParams));
      wService.addRelatedPage(orginaryPageParams, relatedPageParams_2);
      ArrayList<PageImpl> relatedPages_2 = new ArrayList<PageImpl>();
      relatedPages_2.add((PageImpl) wService.getPageById(PortalConfig.USER_TYPE, "john", pageTitle));
      assertEquals(relatedPages_2, wService.getRelatedPage(orginaryPageParams));
      wService.removeRelatedPage(orginaryPageParams, relatedPageParams_2);
      assertEquals(new ArrayList<PageImpl>(), wService.getRelatedPage(orginaryPageParams));
    }
  }
  public void testBreadCumb() throws Exception {
    for(int i =0; i< INVALID_CHARACTERS.length; i++) {
      pageTitle = "testBreadCumb1"+INVALID_CHARACTERS[i]+"character";
      subPageTitle = "testBreadCumb2"+INVALID_CHARACTERS[i]+"character";
      String subPageTitle2 = "testBreadCumb3"+INVALID_CHARACTERS[i]+"character";
      //IN PORTAL
      wService.createPage(PortalConfig.PORTAL_TYPE, "classic", pageTitle, "WikiHome") ;
      assertNotNull(wService.getPageById(PortalConfig.PORTAL_TYPE, "classic", Utils.escapeIllegalJcrChars(pageTitle))) ;
      wService.createPage(PortalConfig.PORTAL_TYPE, "classic", subPageTitle, pageTitle) ;
      assertNotNull(wService.getPageById(PortalConfig.PORTAL_TYPE, "classic", Utils.escapeIllegalJcrChars(subPageTitle))) ;
      wService.createPage(PortalConfig.PORTAL_TYPE, "classic", subPageTitle2, subPageTitle) ;
      assertNotNull(wService.getPageById(PortalConfig.PORTAL_TYPE, "classic", Utils.escapeIllegalJcrChars(subPageTitle2))) ;
      ArrayList<BreadcrumbData> breadCumbs_1 = (ArrayList<BreadcrumbData>) wService.getBreadcumb(PortalConfig.PORTAL_TYPE, "classic", subPageTitle2);
      assertEquals(4, breadCumbs_1.size());
      assertEquals("WikiHome", breadCumbs_1.get(0).getId());
      assertEquals(Utils.escapeIllegalJcrChars(pageTitle), breadCumbs_1.get(1).getId());
      assertEquals(Utils.escapeIllegalJcrChars(subPageTitle), breadCumbs_1.get(2).getId());
      assertEquals(Utils.escapeIllegalJcrChars(subPageTitle2), breadCumbs_1.get(3).getId());
      //IN SPACE
      wService.createPage(PortalConfig.GROUP_TYPE, "platform/users", pageTitle, "WikiHome") ;
      assertNotNull(wService.getPageById(PortalConfig.GROUP_TYPE, "platform/users", Utils.escapeIllegalJcrChars(pageTitle))) ;
      wService.createPage(PortalConfig.GROUP_TYPE, "platform/users", subPageTitle, pageTitle) ;
      assertNotNull(wService.getPageById(PortalConfig.GROUP_TYPE, "platform/users", Utils.escapeIllegalJcrChars(subPageTitle))) ;
      wService.createPage(PortalConfig.GROUP_TYPE, "platform/users", subPageTitle2, subPageTitle) ;
      assertNotNull(wService.getPageById(PortalConfig.GROUP_TYPE, "platform/users", Utils.escapeIllegalJcrChars(subPageTitle2))) ;
      ArrayList<BreadcrumbData> breadCumbs_2 = (ArrayList<BreadcrumbData>) wService.getBreadcumb(PortalConfig.GROUP_TYPE, "platform/users", subPageTitle2);
      assertEquals(4, breadCumbs_2.size());
      assertEquals("WikiHome", breadCumbs_2.get(0).getId());
      assertEquals(Utils.escapeIllegalJcrChars(pageTitle), breadCumbs_2.get(1).getId());
      assertEquals(Utils.escapeIllegalJcrChars(subPageTitle), breadCumbs_2.get(2).getId());
      assertEquals(Utils.escapeIllegalJcrChars(subPageTitle2), breadCumbs_2.get(3).getId());
      //IN PERSONAL WIKI
      wService.createPage(PortalConfig.USER_TYPE, "john", pageTitle, "WikiHome") ;
      assertNotNull(wService.getPageById(PortalConfig.USER_TYPE, "john", Utils.escapeIllegalJcrChars(pageTitle))) ;
      wService.createPage(PortalConfig.USER_TYPE, "john", subPageTitle, pageTitle) ;
      assertNotNull(wService.getPageById(PortalConfig.USER_TYPE, "john", Utils.escapeIllegalJcrChars(subPageTitle))) ;
      wService.createPage(PortalConfig.USER_TYPE, "john", subPageTitle2, subPageTitle) ;
      assertNotNull(wService.getPageById(PortalConfig.USER_TYPE, "john", Utils.escapeIllegalJcrChars(subPageTitle2))) ;
      ArrayList<BreadcrumbData> breadCumbs_3 = (ArrayList<BreadcrumbData>) wService.getBreadcumb(PortalConfig.USER_TYPE, "john", subPageTitle2);
      assertEquals(4, breadCumbs_3.size());
      assertEquals("WikiHome", breadCumbs_3.get(0).getId());
      assertEquals(Utils.escapeIllegalJcrChars(pageTitle), breadCumbs_3.get(1).getId());
      assertEquals(Utils.escapeIllegalJcrChars(subPageTitle), breadCumbs_3.get(2).getId());
      assertEquals(Utils.escapeIllegalJcrChars(subPageTitle2), breadCumbs_3.get(3).getId());
    }
  }
  public void testRenamePage() throws Exception {
    for(int i =0; i< INVALID_CHARACTERS.length; i++) {
      pageTitle = "oldPage"+INVALID_CHARACTERS[i]+"character";
      String renamedTitle = "renamedPage"+INVALID_CHARACTERS[i]+"character";
      wService.createPage(PortalConfig.PORTAL_TYPE, "classic", pageTitle, "WikiHome") ;
      assertNotNull(wService.getPageById(PortalConfig.PORTAL_TYPE, "classic", Utils.escapeIllegalJcrChars(pageTitle))) ;
      wService.renamePage(PortalConfig.PORTAL_TYPE, "classic", Utils.escapeIllegalJcrChars(pageTitle), Utils.escapeIllegalJcrChars(renamedTitle), renamedTitle);
      assertNotNull(wService.getPageById(PortalConfig.PORTAL_TYPE, "classic", Utils.escapeIllegalJcrChars(renamedTitle)));
      wService.createPage(PortalConfig.GROUP_TYPE, "platform/users", pageTitle, "WikiHome") ;
      assertNotNull(wService.getPageById(PortalConfig.GROUP_TYPE, "platform/users", Utils.escapeIllegalJcrChars(pageTitle))) ;
      wService.renamePage(PortalConfig.GROUP_TYPE, "platform/users", Utils.escapeIllegalJcrChars(pageTitle), Utils.escapeIllegalJcrChars(renamedTitle), renamedTitle);
      assertNotNull(wService.getPageById(PortalConfig.GROUP_TYPE, "platform/users", Utils.escapeIllegalJcrChars(renamedTitle)));
      wService.createPage(PortalConfig.USER_TYPE, "john", pageTitle, "WikiHome") ;
      assertNotNull(wService.getPageById(PortalConfig.USER_TYPE, "john", Utils.escapeIllegalJcrChars(pageTitle))) ;
      wService.renamePage(PortalConfig.USER_TYPE, "john", Utils.escapeIllegalJcrChars(pageTitle), Utils.escapeIllegalJcrChars(renamedTitle), renamedTitle);
      assertNotNull(wService.getPageById(PortalConfig.USER_TYPE, "john", Utils.escapeIllegalJcrChars(renamedTitle)));
    }
  }
  public void testSearchPage() throws Exception {
    for(int i =0; i< INVALID_CHARACTERS.length; i++) {
      pageTitle = "searchPage"+INVALID_CHARACTERS[i]+"character";
      wService.createPage(PortalConfig.PORTAL_TYPE, "classic", pageTitle, "WikiHome") ;
      assertNotNull(wService.getPageById(PortalConfig.PORTAL_TYPE, "classic", Utils.escapeIllegalJcrChars(pageTitle))) ;
      WikiSearchData data = new WikiSearchData(INVALID_CHARACTERS[i], null, PortalConfig.PORTAL_TYPE, null);
      int result = wService.search(data).getAvailablePage();
      assertEquals(1, result);
      wService.createPage(PortalConfig.USER_TYPE, "john", pageTitle, "WikiHome") ;
      WikiSearchData data1 = new WikiSearchData(INVALID_CHARACTERS[i], null, PortalConfig.USER_TYPE, null);
      int result1 = wService.search(data1).getAvailablePage();
      assertEquals(1, result1);
      wService.createPage(PortalConfig.GROUP_TYPE, "platform/users", pageTitle, "WikiHome") ;
      WikiSearchData data2 = new WikiSearchData(INVALID_CHARACTERS[i], null, PortalConfig.USER_TYPE, null);
      int result2 = wService.search(data2).getAvailablePage();
      assertEquals(1, result2);
    }
  }
  public void testMovePage() throws Exception {
    for(int i =0; i< INVALID_CHARACTERS.length; i++) {
      String currentPageTitle = "currentPage"+INVALID_CHARACTERS[i]+"character";
      String destinationPageTitle = "destPage"+INVALID_CHARACTERS[i]+"character";
      wService.createPage(PortalConfig.PORTAL_TYPE, "classic", currentPageTitle, "WikiHome") ;
      wService.createPage(PortalConfig.GROUP_TYPE, "platform/users", destinationPageTitle, "WikiHome") ;
      WikiPageParams currentLocationParams= new WikiPageParams();
      WikiPageParams newLocationParams= new WikiPageParams();
      currentLocationParams.setPageId(Utils.escapeIllegalJcrChars(currentPageTitle));
      currentLocationParams.setType(PortalConfig.PORTAL_TYPE);
      currentLocationParams.setOwner("classic");
      newLocationParams.setPageId(Utils.escapeIllegalJcrChars(destinationPageTitle));
      newLocationParams.setType(PortalConfig.GROUP_TYPE);
      newLocationParams.setOwner("platform/users");    
      assertTrue(wService.movePage(currentLocationParams,newLocationParams)) ;
    }
  }
  public void testDeletePage() throws Exception {
    for(int i =0; i< INVALID_CHARACTERS.length; i++) {
      pageTitle = "deletePage"+INVALID_CHARACTERS[i]+"character";
      wService.createPage(PortalConfig.PORTAL_TYPE, "classic", pageTitle, "WikiHome") ;
      assertTrue(wService.deletePage(PortalConfig.PORTAL_TYPE, "classic", Utils.escapeIllegalJcrChars(pageTitle)));
      wService.createPage(PortalConfig.GROUP_TYPE, "platform/users", pageTitle, "WikiHome") ;
      assertTrue(wService.deletePage(PortalConfig.GROUP_TYPE, "platform/users", Utils.escapeIllegalJcrChars(pageTitle)));
      wService.createPage(PortalConfig.USER_TYPE, "john", pageTitle, "WikiHome") ;
      assertTrue(wService.deletePage(PortalConfig.USER_TYPE, "john", Utils.escapeIllegalJcrChars(pageTitle)));
    }
  }
  public void testDeleteTemplatePage() throws Exception{
    Model model = mowService.getModel();
    for(int i=0; i < INVALID_CHARACTERS[i].length(); i++) {
      templatePageTitle = "deletePage"+INVALID_CHARACTERS[i]+"character";
      WikiPageParams params = new WikiPageParams(PortalConfig.PORTAL_TYPE, "classic", null);
      wService.createTemplatePage(templatePageTitle, params);
      model.save();
      assertNotNull(wService.getTemplatePage(params, Utils.escapeIllegalJcrChars(templatePageTitle)));
      wService.deleteTemplatePage(PortalConfig.PORTAL_TYPE, "classic", Utils.escapeIllegalJcrChars(templatePageTitle));
      model.save();
      assertNull(wService.getTemplatePage(params, Utils.escapeIllegalJcrChars(templatePageTitle)));
    }
  }
  public void testDraftPage() throws Exception {
    startSessionAs("john");
    for(int i = 0 ; i< INVALID_CHARACTERS[i].length(); i++) {
      pageTitle = "draftPage"+INVALID_CHARACTERS[i]+"character";
      PageImpl page = (PageImpl) wService.createPage(PortalConfig.PORTAL_TYPE, "classic", pageTitle, "WikiHome");
      page.checkin();
      page.checkout();
      page.getContent().setText("Page content");
      page.checkin();
      page.checkout();
      // Test create draft for exist wiki page
      WikiPageParams param = new WikiPageParams(PortalConfig.PORTAL_TYPE, "classic", page.getName());
      DraftPage draftPage = wService.createDraftForExistPage(param, null, new Date().getTime());
      assertNotNull(draftPage);
      assertFalse(draftPage.isNewPage());
      assertEquals(page.getJCRPageNode().getUUID(), draftPage.getTargetPage());
      assertEquals("2", draftPage.getTargetRevision());
      wService.removeDraft(param);
      assertNull(wService.getDraft(param));
    }
  }
}
