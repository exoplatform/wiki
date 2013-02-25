package org.exoplatform.wiki.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.exoplatform.commons.api.search.SearchServiceConnector;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiNodeType;
import org.exoplatform.wiki.mow.core.api.wiki.AttachmentImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.utils.Utils;
import org.xwiki.rendering.syntax.Syntax;

public class WikiSearchServiceConnector extends SearchServiceConnector {
  
  private static final Log LOG = ExoLogger.getLogger("org.exoplatform.wiki.service.impl.WikiSearchServiceConnector");
  
  public static final String WIKI_PAGE_ICON = "/wiki/skin/DefaultSkin/webui/background/PageIcon.png";
  
  public static String  DATE_TIME_FORMAT = "EEEEE, MMMMMMMM d, yyyy K:mm a";
  
  private static final int   EXCERPT_LENGTH    = 140;
  
  private WikiService wikiService;
  
  private RenderingService renderingService;
  
  public WikiSearchServiceConnector(InitParams initParams) {
    super(initParams);
    wikiService = (WikiService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    renderingService = (RenderingService) PortalContainer.getInstance().getComponentInstanceOfType(RenderingService.class);
  }

  @Override
  public Collection<SearchResult> search(SearchContext context, String query, Collection<String> sites, int offset, int limit, String sort, String order) {
    WikiSearchData searchData = new WikiSearchData(query, null, null, null, null, null);
    searchData.setOffset(offset);
    searchData.setLimit(limit);
    searchData.setSort(sort);
    searchData.setOrder(order);
    
    List<SearchResult> searchResults = new ArrayList<SearchResult>();
    try {
      PageList<org.exoplatform.wiki.service.search.SearchResult> wikiSearchPageList = wikiService.search(searchData);
      if (wikiSearchPageList != null) {
        List<org.exoplatform.wiki.service.search.SearchResult> wikiSearchResults = wikiSearchPageList.getAll();
        for (org.exoplatform.wiki.service.search.SearchResult wikiSearchResult : wikiSearchResults) {
          SearchResult searchResult = buildResult(wikiSearchResult);
          if (searchResult != null) {
            searchResults.add(searchResult);
          }
        }
      }
    } catch (Exception e) {
      LOG.info("Could not execute unified seach on wiki" , e) ; 
    }
    return searchResults;
  }
  
  private String getResultIcon(org.exoplatform.wiki.service.search.SearchResult wikiSearchResult) {
    return WIKI_PAGE_ICON;
  }
  
  private PageImpl getPage(org.exoplatform.wiki.service.search.SearchResult result) throws Exception {
    PageImpl page = null;
    if (WikiNodeType.WIKI_PAGE_CONTENT.equals(result.getType()) || WikiNodeType.WIKI_ATTACHMENT.equals(result.getType())) {
      AttachmentImpl searchContent = (AttachmentImpl) org.exoplatform.wiki.utils.Utils.getObject(result.getPath(), WikiNodeType.WIKI_ATTACHMENT);
      page = searchContent.getParentPage();
    } else if (WikiNodeType.WIKI_PAGE.equals(result.getType()) || WikiNodeType.WIKI_HOME.equals(result.getType())) {
      page = (PageImpl) org.exoplatform.wiki.utils.Utils.getObject(result.getPath(), WikiNodeType.WIKI_PAGE);
    }
    return page;
  }
  
  private String getExcerptOfPage(org.exoplatform.wiki.service.search.SearchResult wikiSearchResult) {
    return wikiSearchResult.getExcerpt();
  }
  
  private String getPageDetail(org.exoplatform.wiki.service.search.SearchResult wikiSearchResult) {
    StringBuffer pageDetail = new StringBuffer();
    try {
      
      // Get space name
      PageImpl page = getPage(wikiSearchResult);
      String spaceName = "";
      Wiki wiki = page.getWiki();
      if (wiki.getType().equals(PortalConfig.GROUP_TYPE)) {
        String wikiOwner = wiki.getOwner();
        if (wikiOwner.indexOf('/') == -1) {
          spaceName = wikiService.getSpaceNameByGroupId("/spaces/" + wiki.getOwner());
        } else {
          spaceName = wikiService.getSpaceNameByGroupId(wiki.getOwner());
        }
      } else {
        spaceName = wiki.getOwner();
      }
      
      // Get update date
      Calendar updateDate = wikiSearchResult.getUpdatedDate();
      SimpleDateFormat format = new SimpleDateFormat(DATE_TIME_FORMAT);
      
      // Build page detail
      pageDetail.append(spaceName);
      pageDetail.append(" - ");
      pageDetail.append(format.format(updateDate.getTime()));
    } catch (Exception e) {
      LOG.info("Can not get page detail ", e);
    }
    return pageDetail.toString();
  }
  
  private SearchResult buildResult(org.exoplatform.wiki.service.search.SearchResult wikiSearchResult) {
    try {
      String title = wikiSearchResult.getTitle();
      String url = wikiSearchResult.getUrl();
      String excerpt = getExcerptOfPage(wikiSearchResult);
      String detail = getPageDetail(wikiSearchResult);
      long relevancy = wikiSearchResult.getJcrScore();
      long date = wikiSearchResult.getUpdatedDate().getTime().getTime();
      String imageUrl = getResultIcon(wikiSearchResult);
      return new SearchResult(url, title, excerpt, detail, imageUrl, date, relevancy);
    } catch (Exception e) {
      LOG.info("Error when getting property from node ", e);
      return null;
    }
  }
}
