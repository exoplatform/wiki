package org.exoplatform.wiki.indexing.listener;

import org.exoplatform.commons.api.indexing.IndexingService;
import org.exoplatform.commons.api.indexing.data.SearchEntry;
import org.exoplatform.commons.api.indexing.data.SearchEntryId;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.listener.PageWikiListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Indexing with :
 * - collection : "wiki"
 * - type : wiki type
 * - name : page id
 */
public class UnifiedSearchPageWikiListener extends PageWikiListener {

  private static Log log = ExoLogger.getLogger(UnifiedSearchPageWikiListener.class);

  private final IndexingService indexingService;

  public UnifiedSearchPageWikiListener(IndexingService indexingService) {
    this.indexingService = indexingService;
  }

  @Override
  public void postAddPage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    if(indexingService != null) {
      Map<String, Object> content = new HashMap<String, Object>();
      content.put("page", page);
      SearchEntry searchEntry = new SearchEntry("wiki", wikiType, pageId, content);
      indexingService.add(searchEntry);
    }
  }

  @Override
  public void postUpdatePage(String wikiType, String wikiOwner, String pageId, Page page, PageUpdateType wikiUpdateType) throws WikiException {
    if(indexingService != null) {
      Map<String, Object> content = new HashMap<String, Object>();
      content.put("page", page);
      SearchEntryId searchEntryId = new SearchEntryId("wiki", wikiType, pageId);
      indexingService.update(searchEntryId, content);
    }
  }

  @Override
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    if(indexingService != null) {
      SearchEntryId searchEntryId = new SearchEntryId("wiki", wikiType, pageId);
      indexingService.delete(searchEntryId);
    }
  }
}
