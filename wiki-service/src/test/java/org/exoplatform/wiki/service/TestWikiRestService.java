package org.exoplatform.wiki.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.compress.utils.IOUtils;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.junit.Test;
import org.mockito.Mockito;

import com.ibm.icu.util.Calendar;

import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mock.MockResourceBundleService;
import org.exoplatform.wiki.mow.api.Attachment;
import org.exoplatform.wiki.mow.api.EmotionIcon;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.service.impl.WikiRestServiceImpl;
import org.xwiki.context.internal.DefaultExecution;

/**
 *
 */
public class TestWikiRestService {

  @Test
  public void shouldGetEmotionIcon() throws WikiException, IOException {
    // Given
    WikiService wikiService = mock(WikiService.class);
    RenderingService renderingService = mock(RenderingService.class);
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, renderingService, new MockResourceBundleService());

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
    RenderingService renderingService = mock(RenderingService.class);
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, renderingService, new MockResourceBundleService());

    when(wikiService.getEmotionIconByName("test.gif")).thenReturn(null);

    // When
    Response emotionIconResponse = wikiRestService.getEmotionIcon(null, "test.gif");

    // Then
    assertNotNull(emotionIconResponse);
    assertEquals(404, emotionIconResponse.getStatus());
  }
  
  @Test
  public void testGetPageAttachmentResponseHeader() throws WikiException {
    //Given
    WikiService wikiService = mock(WikiService.class);
    RenderingService renderingService = mock(RenderingService.class);
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, renderingService, new MockResourceBundleService());
    
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
    RenderingService renderingService = mock(RenderingService.class);
    UriInfo uriInfo = mock(UriInfo.class);
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, renderingService, new MockResourceBundleService());
    
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
    RenderingService renderingService = mock(RenderingService.class);
    when(renderingService.getExecution()).thenReturn(new DefaultExecution());
    when(renderingService.render(anyString(), anyString(), anyString(), anyBoolean())).thenAnswer(i -> i.getArguments()[0]);
    UriInfo uriInfo = mock(UriInfo.class);
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, renderingService, new MockResourceBundleService());
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
}
