package org.exoplatform.wiki.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.compress.utils.IOUtils;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.rest.api.EntityBuilder;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.service.search.SearchResult;
import org.junit.Test;

import com.ibm.icu.util.Calendar;

import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mock.MockResourceBundleService;
import org.exoplatform.wiki.service.impl.WikiRestServiceImpl;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 *
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class TestWikiRestService {

  @Test
  public void shouldGetEmotionIcon() throws WikiException, IOException {
    // Given
    WikiService wikiService = mock(WikiService.class);
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, new MockResourceBundleService());

    EmotionIcon emotionIcon = new EmotionIcon();
    emotionIcon.setName("test.gif");
    emotionIcon.setImage("image".getBytes());

    when(wikiService.getEmotionIconByName("test.gif")).thenReturn(emotionIcon);

    // When
    Response emotionIconResponse = wikiRestService.getEmotionIcon(null, "test.gif");

    // Then
    assertNotNull(emotionIconResponse);
    assertEquals(200, emotionIconResponse.getStatus());
    Object responseEntity = emotionIconResponse.getEntity();
    assertNotNull(responseEntity);
    assertArrayEquals(emotionIcon.getImage(), IOUtils.toByteArray((ByteArrayInputStream) responseEntity));
  }

  @Test
  public void shouldGetNotFoundResponseWhenEmotionIconDoesNotExist() throws WikiException {
    // Given
    WikiService wikiService = mock(WikiService.class);
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, new MockResourceBundleService());

    when(wikiService.getEmotionIconByName("test.gif")).thenReturn(null);

    // When
    Response emotionIconResponse = wikiRestService.getEmotionIcon(null, "test.gif");

    // Then
    assertNotNull(emotionIconResponse);
    assertEquals(404, emotionIconResponse.getStatus());
  }

  @PrepareForTest({EntityBuilder.class})
  @Test
  public void testSearchData() throws Exception {
    // Given
    WikiService wikiService = mock(WikiService.class);
    EntityBuilder entityBuilder = mock(EntityBuilder.class);
    java.util.Calendar cDate1 = java.util.Calendar.getInstance();
    UriInfo uriInfo = mock(UriInfo.class);
    org.exoplatform.social.core.identity.model.Identity identityResult = new org.exoplatform.social.core.identity.model.Identity();
    identityResult.setProviderId("organization");
    identityResult.setRemoteId("root");
    identityResult.setId("1");
    Page page = new Page();
    page.setWikiId("1");
    page.setWikiType("Page");
    page.setWikiOwner("alioua");
    page.setName("Wiki_one");
    page.setTitle("Wiki one");
    page.setUrl("/exo/wiki");
    org.exoplatform.wiki.service.search.SearchResult result1 = new SearchResult();
    org.exoplatform.wiki.service.search.SearchResult result2 = new SearchResult();
    result1.setPageName("wiki");
    result1.setExcerpt("admin");
    result1.setPoster(null);
    result2.setExcerpt("admin");
    result2.setPageName("wik");
    result2.setPoster(identityResult);
    result2.setTitle("wiki test");
    result2.setCreatedDate(cDate1);
    org.exoplatform.social.rest.entity.IdentityEntity entity = new org.exoplatform.social.rest.entity.IdentityEntity();
    entity.setProviderId("organization");
    entity.setRemoteId("root");
    entity.setId("1");
    entity.setDeleted(false);
    when(wikiService.getPageOfWikiByName(any(), any(), any())).thenReturn(page);
    List<org.exoplatform.wiki.service.search.SearchResult> results = new ArrayList<org.exoplatform.wiki.service.search.SearchResult>();
    results.add(result1);
    results.add(result2);
    PowerMockito.mockStatic(EntityBuilder.class);
    PageList<org.exoplatform.wiki.service.search.SearchResult> pageList = new ObjectPageList<>(results, 2);
    when(wikiService.search(any())).thenReturn(pageList);
    when(uriInfo.getPath()).thenReturn("/wiki/contextsearch");
    when(EntityBuilder.buildEntityIdentity((Identity) any(), any(), any())).thenReturn(entity);
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, new MockResourceBundleService());

    // When
    Response response = wikiRestService.searchData(uriInfo, "wiki", "page", "alioua");

    // Then
    verify(entityBuilder, times(1)).buildEntityIdentity((Identity) any(), any(), any());
    assertEquals(200, response.getStatus());
  }
  
  @Test
  public void testGetPageAttachmentResponseHeader() throws WikiException {
    //Given
    WikiService wikiService = mock(WikiService.class);
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, new MockResourceBundleService());
    
    Wiki wiki = new Wiki("user", "root");
    when(wikiService.createWiki("portal", "wikiAttachement1")).thenReturn(wiki);
    String wikiType = wiki.getType();
    String wikiOwner = wiki.getOwner();
    
    Page wikiHomePage = new Page("wikiHomePage", "wikiHomePage");
    wikiHomePage.setId("wikiHomePageId");
    String pageId = wikiHomePage.getId();
    
    Attachment attachment1 = new Attachment();
    attachment1.setName("attachment1.png");
    attachment1.setTitle("attachment1.png");
    attachment1.setContent("logo".getBytes());
    attachment1.setMimeType("multipart/mixed");
    attachment1.setCreator("root");
    
    Attachment attachment2 = new Attachment();
    attachment2.setName("attachment2.png");
    attachment2.setTitle("attachment2.png");
    attachment2.setContent("logo".getBytes());
    attachment2.setMimeType("application/octet-stream");
    attachment2.setCreator("root");
    
    Attachment attachment3 = new Attachment();
    attachment3.setName("attachment3.png");
    attachment3.setTitle("attachment3.png");
    attachment3.setContent("logo".getBytes());
    attachment3.setMimeType("text/xhtml");
    attachment3.setCreator("root");

    when(wikiService.getPageOfWikiByName(wikiType, wikiOwner, "wikiHomePageId")).thenReturn(wikiHomePage);
    when(wikiService.getAttachmentOfPageByName("attachment1.png", wikiHomePage, true)).thenReturn(attachment1);
    when(wikiService.getAttachmentOfPageByName("attachment2.png", wikiHomePage, true)).thenReturn(attachment2);
    when(wikiService.getAttachmentOfPageByName("attachment3.png", wikiHomePage, true)).thenReturn(attachment3);
  
    // When
    Response response = wikiRestService.getAttachment(null, wikiType , wikiOwner, pageId, "attachment1.png", null);
    // Then
    assertNotNull(response);
    assertEquals(200, response.getStatus());
    assertEquals(2, response.getMetadata().size());
    assertNotNull(response.getMetadata().containsKey("cache-control"));
    assertNotNull(response.getMetadata().containsKey("Content-Disposition"));
    assertTrue(response.getMetadata().get("Content-Disposition").contains("attachment; filename="+attachment1.getTitle()));
    //when
    Response response1 = wikiRestService.getAttachment(null, wikiType , wikiOwner, pageId, "attachment2.png", null);
    // Then
    assertNotNull(response1);
    assertEquals(200, response1.getStatus());
    assertEquals(2, response1.getMetadata().size());
    assertNotNull(response1.getMetadata().containsKey("cache-control"));
    assertNotNull(response1.getMetadata().containsKey("Content-Disposition"));
    assertTrue(response1.getMetadata().get("Content-Disposition").contains("attachment; filename="+attachment2.getTitle()));
  
    //when
    Response response2 = wikiRestService.getAttachment(null, wikiType , wikiOwner, pageId, "attachment3.png", null);
    // Then
    assertNotNull(response2);
    assertEquals(200, response2.getStatus());
    assertEquals(2, response2.getMetadata().size());
    assertNotNull(response2.getMetadata().containsKey("cache-control"));
    assertNotNull(response2.getMetadata().containsKey("Content-Disposition"));
    assertTrue(response2.getMetadata().get("Content-Disposition").contains("attachment; filename="+attachment3.getTitle()));
  }
  

  @Test
  public void testSanitizePageTitle() throws Exception {
    //Given
    WikiService wikiService = mock(WikiService.class);
    UriInfo uriInfo = mock(UriInfo.class);
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, new MockResourceBundleService());
    
    Wiki wiki = new Wiki("user", "root");
    when(wikiService.createWiki("portal", "wikiAttachement1")).thenReturn(wiki);
    String wikiType = wiki.getType();
    String wikiOwner = wiki.getOwner();
    
    Page wikiHomePage = new Page("wikiHomePage", "wikiHomePage<script>alert();</script>");
    wikiHomePage.setId("wikiHomePageId");
    wikiHomePage.setWikiType(wikiType);
    wikiHomePage.setWikiOwner(wikiOwner);
    wikiHomePage.setUpdatedDate(Calendar.getInstance().getTime());
    String pageId = wikiHomePage.getId();

    when(wikiService.getWikiByTypeAndOwner(wikiType, wikiOwner)).thenReturn(wiki);
    when(wikiService.getPageOfWikiByName(wikiType, wikiOwner, pageId)).thenReturn(wikiHomePage);
    when(uriInfo.getAbsolutePath()).thenReturn(new URI("/"));
    when(uriInfo.getBaseUri()).thenReturn(new URI("/"));

    // When
    org.exoplatform.wiki.service.rest.model.Page page = wikiRestService.getPage(uriInfo, wikiType, wikiOwner, pageId);
    // Then
    verify(wikiService, times(1)).getPageOfWikiByName(wikiType, wikiOwner, pageId);
    assertEquals("wikiHomePage", page.getTitle());
  }

  @Test
  public void testSanitizePageContent() throws Exception {
    //Given
    WikiService wikiService = mock(WikiService.class);
    UriInfo uriInfo = mock(UriInfo.class);
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, new MockResourceBundleService());
    EnvironmentContext.setCurrent(new EnvironmentContext());
    ServletContext servletContext = mock(ServletContext.class);
    when(servletContext.getResourceAsStream(anyString())).thenReturn(new ByteArrayInputStream("<div>$content</div>".getBytes()));

    Wiki wiki = new Wiki("user", "root");
    when(wikiService.createWiki("portal", "wikiSanitizePageContent1")).thenReturn(wiki);
    String wikiType = wiki.getType();
    String wikiOwner = wiki.getOwner();

    Page wikiHomePage = new Page("wikiHomePage", "wikiHomePage");
    wikiHomePage.setId("wikiHomePageId");
    wikiHomePage.setWikiType(wikiType);
    wikiHomePage.setWikiOwner(wikiOwner);
    wikiHomePage.setUpdatedDate(Calendar.getInstance().getTime());
    wikiHomePage.setContent("<div>my<script>alert();</script> page</div>");
    String pageId = wikiHomePage.getId();

    when(wikiService.getWikiByTypeAndOwner(wikiType, wikiOwner)).thenReturn(wiki);
    when(wikiService.getPageOfWikiByName(wikiType, wikiOwner, pageId)).thenReturn(wikiHomePage);
    when(uriInfo.getAbsolutePath()).thenReturn(new URI("/"));
    when(uriInfo.getBaseUri()).thenReturn(new URI("/"));

    // When
    Response response = wikiRestService.getWikiPageContent(wikiHomePage.getContent());

    // Then
    assertNotNull(response);
    assertEquals(200, response.getStatus());
    assertEquals("<div>my page</div>", response.getEntity());
  }

  @Test
  public void testSaveDraftName() throws Exception {
    //Given
    WikiService wikiService = mock(WikiService.class);
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, new MockResourceBundleService());

    DraftPage newDraftPage = new DraftPage();
    newDraftPage.setTitle("newDraft");
    newDraftPage.setName("new_Draft");
    newDraftPage.setId("1");
    newDraftPage.setNewPage(true);
    newDraftPage.setTargetPageId("1");
    newDraftPage.setTargetPageRevision("1");
    newDraftPage.setContent("new content");

    Wiki wiki = new Wiki("portal", "global");
    String wikiType = wiki.getType();
    String wikiOwner = wiki.getOwner();

    Page wikiHomePage = new Page("WikiHome", "Wiki Home");
    wikiHomePage.setId("1");
    wikiHomePage.setWikiId("1");
    wikiHomePage.setSyntax("xhtml/1.0");
    wikiHomePage.setWikiType(wikiType);
    wikiHomePage.setWikiOwner(wikiOwner);
    String pageId = wikiHomePage.getId();


    when(wikiService.getWikiByTypeAndOwner(wikiType, wikiOwner)).thenReturn(wiki);
    when(wikiService.getPageOfWikiByName(wikiType, wikiOwner, pageId)).thenReturn(wikiHomePage);
    when(wikiService.createDraftForNewPage(any(DraftPage.class),any(Page.class),anyLong())).thenReturn(newDraftPage);

    // When
    Response response = wikiRestService.saveDraft(wikiType, wikiOwner, pageId,"undefined","",true, 1594394768847L,"newDraft","new content","false");
    // Then
    assertEquals(200, response.getStatus());
  }
}
