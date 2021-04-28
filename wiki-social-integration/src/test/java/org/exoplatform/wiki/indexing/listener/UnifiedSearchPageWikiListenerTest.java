package org.exoplatform.wiki.indexing.listener;

import org.exoplatform.commons.api.indexing.IndexingService;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class UnifiedSearchPageWikiListenerTest {
  @Mock
  private WikiService     wikiService;

  @Mock
  private IdentityManager identityManager;

  @Mock
  private ActivityManager activityManager;

  @Mock
  private SpaceService    spaceService;

  @Mock
  private IndexingService indexingService;

  @Test
  public void testPostAddPage() throws Exception {
    // Given
    UnifiedSearchPageWikiListener unifiedSearchPageWikiListener = new UnifiedSearchPageWikiListener(indexingService);

    Page page = new Page();
    page.setTitle("title");
    page.setAuthor("root");
    page.setId("id123");

    // When
    unifiedSearchPageWikiListener.postAddPage("wikiType", "root", "id123", page);

    // Then
    verify(indexingService, times(1)).add(any());
  }

  @Test
  public void testPostUpdatePage() throws Exception {
    // Given
    UnifiedSearchPageWikiListener unifiedSearchPageWikiListener = new UnifiedSearchPageWikiListener(indexingService);

    Page page = new Page();
    page.setTitle("title");
    page.setAuthor("root");
    page.setId("id1234");

    // When
    unifiedSearchPageWikiListener.postUpdatePage("wikiType", "root", "id123", page, PageUpdateType.EDIT_PAGE_CONTENT);

    // Then
    verify(indexingService, times(1)).update(any(), any());
  }

  @Test
  public void testPostDeletePage() throws Exception {
    // Given
    UnifiedSearchPageWikiListener unifiedSearchPageWikiListener = new UnifiedSearchPageWikiListener(indexingService);

    Page page = new Page();
    page.setTitle("title");
    page.setAuthor("root");
    page.setId("id123");

    // When
    unifiedSearchPageWikiListener.postDeletePage("wikiType", "root", "id123", page);

    // Then
    verify(indexingService, times(1)).delete(any());
  }

}
