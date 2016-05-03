package org.exoplatform.wiki.service;

import org.apache.commons.compress.utils.IOUtils;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.EmotionIcon;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.service.impl.WikiRestServiceImpl;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.*;
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

}
