package org.exoplatform.wiki.service;

import java.io.InputStream;
import java.util.List;

import org.chromattic.api.ChromatticSession;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

public interface DataStorage {
  public PageList<SearchResult> search(ChromatticSession session, WikiSearchData data) throws Exception ;
  public InputStream getAttachmentAsStream(String path, ChromatticSession session) throws Exception ;
  public List<SearchResult> searchRenamedPage(ChromatticSession session, WikiSearchData data) throws Exception ;
  public List<TemplateSearchResult> searchTemplate(ChromatticSession session, TemplateSearchData data) throws Exception ;

  /**
   * Get a wiki page by UUID
   * 
   * @param session The chromattics session to get wiki page
   * @param uuid The node UUID of wiki page 
   * @return The wiki page
   * @throws Exception
   */
  public Page getWikiPageByUUID(ChromatticSession session, String uuid) throws Exception;
}
