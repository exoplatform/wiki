package org.exoplatform.wiki.ext.impl;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

/**
 * Test class for WikiSpaceActivityPublisher
 */
@RunWith(MockitoJUnitRunner.class)
public class WikiSpaceActivityPublisherTest {

  @Mock
  private WikiService      wikiService;

  @Mock
  private IdentityManager  identityManager;

  @Mock
  private RenderingService renderingService;

  @Mock
  private ActivityManager  activityManager;

  @Mock
  private SpaceService     spaceService;

  @Test
  public void shouldNotCreateActivityWhenUpdateTypeIsNull() throws Exception {
    // Given
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(wikiService,
                                                                                           identityManager,
                                                                                           renderingService,
                                                                                           activityManager,
                                                                                           spaceService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = spy(wikiSpaceActivityPublisher);
    Page page = new Page();

    // When
    wikiSpaceActivityPublisher.postUpdatePage("portal", "portal1", "page1", page, null);

    // Then
    verify(wikiSpaceActivityPublisherSpy, never()).saveActivity("portal", "portal1", "page1", page, null);
  }

  @Test
  public void shouldNotCreateActivityWhenUpdateTypeIsPermissionsChange() throws Exception {
    // Given
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(wikiService,
                                                                                           identityManager,
                                                                                           renderingService,
                                                                                           activityManager,
                                                                                           spaceService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = spy(wikiSpaceActivityPublisher);
    Page page = new Page();

    // When
    wikiSpaceActivityPublisher.postUpdatePage("portal", "portal1", "page1", page, PageUpdateType.EDIT_PAGE_PERMISSIONS);

    // Then
    verify(wikiSpaceActivityPublisherSpy, never()).saveActivity("portal", "portal1", "page1", page, null);
  }

  @Test
  public void shouldSaveActivityWith4LinesAndEllipsisWhenPageContainsMoreThan4Lines() throws Exception {
    Page page = new Page();
    page.setContent("line1\n\nline2\n\nline3\n\nline4\n\nline5");

    prepareMockServices(page);

    // save activity
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(wikiService,
                                                                                           identityManager,
                                                                                           renderingService,
                                                                                           activityManager,
                                                                                           spaceService);
    wikiSpaceActivityPublisher.saveActivity("portal", "root", "page1", page, PageUpdateType.ADD_PAGE);
    // check if activity manager saveActivityNoReturn is called
    ArgumentCaptor<ExoSocialActivity> activityCaptor = ArgumentCaptor.forClass(ExoSocialActivity.class);
    verify(activityManager).saveActivityNoReturn(any(org.exoplatform.social.core.identity.model.Identity.class),
                                                 activityCaptor.capture());
    ExoSocialActivity activity = activityCaptor.getValue();

    // check the exceprt is processed correctly
    // only get first 4 lines of wiki page WIKI-1290
    String exceprt = activity.getTemplateParams().get("page_exceprt");
    Assert.assertEquals("<p>line1</p><p>line2</p><p>line3</p><p>line4</p>...", exceprt);
  }

  @Test
  public void shouldSaveActivityWith4LinesAndNoEllipsisWhenPageContains4Lines() throws Exception {
    Page page = new Page();
    page.setContent("line1\n\nline2\n\n  \n\nline3\n\nline4");

    prepareMockServices(page);

    // save activity
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(wikiService,
                                                                                           identityManager,
                                                                                           renderingService,
                                                                                           activityManager,
                                                                                           spaceService);
    // case 2: there are only 4 not empty lines, the ellipsis should not be added
    page.setContent("line1\n\nline2\n\n  \n\nline3\n\nline4");
    wikiSpaceActivityPublisher.saveActivity("portal", "root", "page1", page, PageUpdateType.ADD_PAGE);
    // capture new activity
    ArgumentCaptor<ExoSocialActivity> activityCaptor = ArgumentCaptor.forClass(ExoSocialActivity.class);
    verify(activityManager).saveActivityNoReturn(any(org.exoplatform.social.core.identity.model.Identity.class),
                                                 activityCaptor.capture());
    ExoSocialActivity activity = activityCaptor.getValue();
    //
    String exceprt = activity.getTemplateParams().get("page_exceprt");
    Assert.assertEquals("<p>line1</p><p>line2</p><p>line3</p><p>line4</p>", exceprt);
  }

  private void prepareMockServices(Page page) throws Exception {
    ConversationState state = mock(ConversationState.class);
    Identity owner = new Identity("root");
    when(state.getIdentity()).thenReturn(owner);
    ConversationState.setCurrent(state);

    when(activityManager.getActivity(anyString())).thenReturn(null);

    when(identityManager.getOrCreateIdentity(anyString(),
                                             anyString(),
                                             anyBoolean())).thenReturn(mock(org.exoplatform.social.core.identity.model.Identity.class));

    when(renderingService.render(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(page.getContent());
  }
}
