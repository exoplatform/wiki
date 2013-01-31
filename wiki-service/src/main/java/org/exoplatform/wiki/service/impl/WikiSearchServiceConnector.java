package org.exoplatform.wiki.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.exoplatform.commons.api.search.SearchServiceConnector;
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
import org.xwiki.rendering.syntax.Syntax;

public class WikiSearchServiceConnector extends SearchServiceConnector {
  
  private static final Log LOG = ExoLogger.getLogger("org.exoplatform.wiki.service.impl.WikiSearchServiceConnector");
  
  public static final String WIKI_PAGE_ICON = "uiPageIcon";
  
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
  public Collection<SearchResult> search(String query, Collection<String> sites, int offset, int limit, String sort, String order) {
    WikiSearchData searchData = new WikiSearchData(query, null, null, null, null, null);
    searchData.setNodeType(WikiNodeType.WIKI_PAGE);
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
          searchResults.add(buildResult(wikiSearchResult));
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
    String excerpt = "";
    try {
      PageImpl page = getPage(wikiSearchResult);
      excerpt = renderingService.render(page.getContent().getText(), page.getSyntax(), Syntax.PLAIN_1_0.toIdString(), false);
      excerpt = excerpt.split("\n")[0];
      if (excerpt.length() > EXCERPT_LENGTH) {
        excerpt = excerpt.substring(0, EXCERPT_LENGTH) + "...";
      }
    } catch (Exception e) {
      LOG.info("Can not get page excerpt ", e);
    }
    return excerpt;
  }
  
  private String getPageDetail(org.exoplatform.wiki.service.search.SearchResult wikiSearchResult) {
    StringBuffer pageDetail = new StringBuffer();
    try {
      
      // Get space name
      PageImpl page = getPage(wikiSearchResult);
      String spaceName = "";
      Wiki wiki = page.getWiki();
      if (wiki.getType().equals(PortalConfig.GROUP_TYPE)) {
        spaceName = wikiService.getSpaceNameByGroupId("/spaces/" + wiki.getOwner());
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
    SearchResult result = new SearchResult() ;
    try {
      result.setTitle(wikiSearchResult.getTitle());
      result.setUrl(wikiSearchResult.getUrl());
      result.setExcerpt(getExcerptOfPage(wikiSearchResult));
      result.setDetail(getPageDetail(wikiSearchResult));
      result.setRelevancy(wikiSearchResult.getJcrScore());
      result.setDate(wikiSearchResult.getUpdatedDate().getTime().getTime());
      result.setImageUrl(getResultIcon(wikiSearchResult));
    }catch (Exception e) {
      LOG.info("Error when getting property from node ", e);
    }
    return result;
  }
}
