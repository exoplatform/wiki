package org.exoplatform.wiki.service;

import org.apache.commons.compress.utils.IOUtils;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Attachment;
import org.exoplatform.wiki.mow.api.EmotionIcon;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.service.impl.WikiRestServiceImpl;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 *
 */
public class TestWikiRestService {

  @Test
  public void shouldGetEmotionIcon() throws WikiException, IOException {
    // Given
    WikiService wikiService = mock(WikiService.class);
    RenderingService renderingService = mock(RenderingService.class);
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, renderingService);

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
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, renderingService);

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
    WikiRestServiceImpl wikiRestService = new WikiRestServiceImpl(wikiService, renderingService);
    
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
    when(wikiService.getAttachmentOfPageByName("attachment1.png",wikiHomePage)).thenReturn(attachment1);
    when(wikiService.getAttachmentOfPageByName("attachment2.png",wikiHomePage)).thenReturn(attachment2);
    when(wikiService.getAttachmentOfPageByName("attachment3.png",wikiHomePage)).thenReturn(attachment3);
  
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
  

}
