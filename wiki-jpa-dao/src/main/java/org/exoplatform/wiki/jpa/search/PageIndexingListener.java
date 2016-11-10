package org.exoplatform.wiki.jpa.search;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.dao.PageAttachmentDAO;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.listener.PageWikiListener;

import java.util.List;

/**
 * Listener on pages creation/update/deletion to index them
 */
public class PageIndexingListener extends PageWikiListener {

  private PageAttachmentDAO pageAttachmentDAO;
  private IndexingService indexingService;

  public PageIndexingListener(PageAttachmentDAO pageAttachmentDAO, IndexingService indexingService) {
    this.pageAttachmentDAO = pageAttachmentDAO;
    this.indexingService = indexingService;
  }

  @Override
  public void postAddPage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    indexingService.index(WikiPageIndexingServiceConnector.TYPE, page.getId());
  }

  @Override
  public void postUpdatePage(String wikiType, String wikiOwner, String pageId, Page page, PageUpdateType wikiUpdateType) throws WikiException {
    indexingService.reindex(WikiPageIndexingServiceConnector.TYPE, page.getId());
  }

  @Override
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    indexingService.unindex(WikiPageIndexingServiceConnector.TYPE, page.getId());
    //We need also to unindex the attachments of the page
    List<Long> attachmentIds = pageAttachmentDAO.getAttachmentIdByPageId(Long.valueOf(page.getId()));
    for (Long attachmentId : attachmentIds) {
      indexingService.unindex(AttachmentIndexingServiceConnector.TYPE, String.valueOf(attachmentId));
    }
  }


}
