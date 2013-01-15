package org.exoplatform.wiki.service.listener;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.wiki.mow.api.Page;

public abstract class PageWikiListener extends BaseComponentPlugin {
  public static final String ADD_PAGE_TYPE     = "add_page";
  
  public static final String EDIT_PAGE_TITLE_TYPE = "editPageTitle";
  
  public static final String EDIT_PAGE_CONTENT_TYPE = "editPageContent";
  
  public static final String MOVE_PAGE_TYPE = "movePage";
  
  public abstract void postAddPage(final String wikiType, final String wikiOwner, final String pageId, Page page) throws Exception;

  public abstract void postUpdatePage(final String wikiType, final String wikiOwner, final String pageId, Page page, String wikiUpdateType) throws Exception;

  public abstract void postDeletePage(final String wikiType, final String wikiOwner, final String pageId, Page page) throws Exception;
}
