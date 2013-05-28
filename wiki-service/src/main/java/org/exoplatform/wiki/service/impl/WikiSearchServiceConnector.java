package org.exoplatform.wiki.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.search.SearchServiceConnector;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiNodeType;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.wiki.AttachmentImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.utils.Utils;


/**
 *  The WikiSearchServiceConnector provide a connector service for the common unified search.
 *  It implements a direct search in the jcr based on several criteria
 *
 * @LevelAPI Experimental
 */
public class WikiSearchServiceConnector extends SearchServiceConnector {
  
  private static final Log LOG = ExoLogger.getLogger("org.exoplatform.wiki.service.impl.WikiSearchServiceConnector");

  /**
   * URL of the icon used in the unified search to represent the results
   */
  public static final String WIKI_PAGE_ICON = "/wiki/skin/images/unified-search/PageIcon.png";

  /**
   * Date format expected by the unified search
   */
  public static String  DATE_TIME_FORMAT = "EEEEE, MMMMMMMM d, yyyy K:mm a";
  
  private WikiService wikiService;

  /**
   * Initialise the wiki service connector
   *
   * @param initParams Parameters
   */
  public WikiSearchServiceConnector(InitParams initParams) {
    super(initParams);
    wikiService = (WikiService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
  }

    /**
     * The connectors must implement this search method, with the following parameters and return a collection of SearchResult
     *
     * @param context Search context
     * @param query The user-input query to search for
     * @param sites Search on these specified sites only (e.g acme, intranet...)
     * @param offset Start offset of the result set
     * @param limit Maximum size of the result set
     * @param sort The field to sort the result set
     * @param order Sort order (ASC, DESC)
     * @return a collection of SearchResult
     */
  @Override
  public Collection<SearchResult> search(SearchContext context, String query, Collection<String> sites, int offset, int limit, String sort, String order) {
    // When limit is 0 then return all search result
    if (limit == 0) {
      limit = Integer.MAX_VALUE;
    }

    // Prepare search data
    WikiSearchData searchData = new WikiSearchData(query, query, null, null);
    searchData.setOffset(offset);
    searchData.setLimit(limit);
    
    // Execute the search
    List<SearchResult> searchResults = new ArrayList<SearchResult>();
    try {
      PageList<org.exoplatform.wiki.service.search.SearchResult> wikiSearchPageList = wikiService.search(searchData);
      if (wikiSearchPageList != null) {
        List<org.exoplatform.wiki.service.search.SearchResult> wikiSearchResults = wikiSearchPageList.getAll();
        for (org.exoplatform.wiki.service.search.SearchResult wikiSearchResult : wikiSearchResults) {
          SearchResult searchResult = buildResult(context, wikiSearchResult);
          if (searchResult != null) {
            searchResults.add(searchResult);
          }
        }
      }
    } catch (Exception e) {
      LOG.info("Could not execute unified seach on wiki" , e) ; 
    }
    
    // Sort search result
    sortSearchResult(searchResults, sort, order);
    
    // Return the result
    return searchResults;
  }

  /**
   * Sort the results based on the order and labels
   *
   * @param searchResults The list of results
   * @param sort Can be orderred by title, relevancy or date
   * @param order ASC or DESC
   */
  private void sortSearchResult(List<SearchResult> searchResults, String sort, String order) {
    if (StringUtils.isEmpty(sort)) {
      return;
    }
    
    if (StringUtils.isEmpty(order)) {
      order = "ASC";
    }
    final String orderValue = order;
    
    if ("title".equalsIgnoreCase(sort)) {
      Collections.sort(searchResults, new Comparator<SearchResult>() {
        @Override
        public int compare(SearchResult o1, SearchResult o2) {
          if ("ASC".equalsIgnoreCase(orderValue)) {
            o1.getTitle().compareTo(o2.getTitle());
          }
          return o2.getTitle().compareTo(o1.getTitle());
        }
      });
    } else if ("relevancy".equalsIgnoreCase(sort)) {
      Collections.sort(searchResults, new Comparator<SearchResult>() {
        @Override
        public int compare(SearchResult o1, SearchResult o2) {
          if ("ASC".equalsIgnoreCase(orderValue)) {
            return (int) (o1.getRelevancy() - o2.getRelevancy());
          }
          return (int) (o2.getRelevancy() - o1.getRelevancy());
        }
      });
      
    } else if ("date".equalsIgnoreCase(sort)) {
      Collections.sort(searchResults, new Comparator<SearchResult>() {
        @Override
        public int compare(SearchResult o1, SearchResult o2) {
          if ("ASC".equalsIgnoreCase(orderValue)) {
            return (int) (o1.getDate() - o2.getDate());
          }
          return (int) (o2.getDate() - o1.getDate());
        }
      });
    } 
  }

  /**
   * Get the icon for the wiki pages
   *
   * @param wikiSearchResult A simple result from the search
   * @return The url of the icon
   */
  private String getResultIcon(org.exoplatform.wiki.service.search.SearchResult wikiSearchResult) {
    return WIKI_PAGE_ICON;
  }

  /**
   * Get all the information of the page result
   *
   * @param result A simple result from the search
   * @return The page
   * @throws Exception
   */
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

  /**
   * Return the detail of the page based on the result
   *
   * @param wikiSearchResult A simple result from the search
   * @return The detail of the result
   */
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

  /**
   * Return the permalink of the result from the search
   *
   * @param context Not Used
   * @param wikiSearchResult A simple result from the search
   * @return The permalink of the result
   */
  private String getPagePermalink(SearchContext context, org.exoplatform.wiki.service.search.SearchResult wikiSearchResult) {
    StringBuffer permalink = new StringBuffer();
    try {
      PageImpl page = getPage(wikiSearchResult);
      if (page.getWiki().getType().equalsIgnoreCase(WikiType.GROUP.toString())) {
        String portalContainerName = Utils.getPortalName();
        String portalOwner = context.getSiteName();
        String wikiWebappUri = wikiService.getWikiWebappUri();
        String spaceGroupId = page.getWiki().getOwner();
        
        permalink.append("/");
        permalink.append(portalContainerName);
        permalink.append("/");
        permalink.append(portalOwner);
        permalink.append("/");
        permalink.append(wikiWebappUri);
        permalink.append("/");
        permalink.append(PortalConfig.GROUP_TYPE);
        permalink.append(spaceGroupId);
        permalink.append("/");
        permalink.append(page.getName());
      } else {
        String portalContainerName = Utils.getPortalName();
        String url = page.getURL();
        if (url != null) {
          url = url.substring(url.indexOf("/" + portalContainerName + "/"));
          permalink.append(url);
        }
      }
    } catch (Exception ex) {
      LOG.info("Can not build the permalink for wiki page ", ex);
    }
    return permalink.toString();
  }

  /**
   * Format the result as expected by the unified search
   *
   * @param context The context url
   * @param wikiSearchResult A simple result from the search
   * @return A result formated
   */
  private SearchResult buildResult(SearchContext context, org.exoplatform.wiki.service.search.SearchResult wikiSearchResult) {
    try {
      String title = wikiSearchResult.getTitle();
      String url = getPagePermalink(context, wikiSearchResult);
      String excerpt = wikiSearchResult.getExcerpt();
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
