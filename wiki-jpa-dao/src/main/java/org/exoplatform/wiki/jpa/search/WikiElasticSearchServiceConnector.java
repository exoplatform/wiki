/* 
* Copyright (C) 2003-2015 eXo Platform SAS.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see http://www.gnu.org/licenses/ .
*/
package org.exoplatform.wiki.jpa.search;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.search.es.ElasticSearchException;
import org.exoplatform.commons.search.es.ElasticSearchFilter;
import org.exoplatform.commons.search.es.ElasticSearchFilterType;
import org.exoplatform.commons.search.es.ElasticSearchServiceConnector;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.SearchResultType;
import org.exoplatform.wiki.utils.Utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.*;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 11/24/15
 */
public class WikiElasticSearchServiceConnector extends ElasticSearchServiceConnector {

  private static final Log LOG = ExoLogger.getLogger(WikiElasticSearchServiceConnector.class);

  public WikiElasticSearchServiceConnector(InitParams initParams, ElasticSearchingClient client) {
    super(initParams, client);
  }

  @Override
  protected String getSourceFields() {

    List<String> fields = new ArrayList<>();
    fields.add("title");
    fields.add("url");
    fields.add("wikiType");
    fields.add("wikiOwner");
    fields.add("createdDate");
    fields.add("updatedDate");
    fields.add("name");
    fields.add("pageName");

    List<String> sourceFields = new ArrayList<>();
    for (String sourceField: fields) {
      sourceFields.add("\"" + sourceField + "\"");
    }

    return StringUtils.join(sourceFields, ",");
  }

  public List<SearchResult> searchWiki(String searchedText, String wikiType, String wikiOwner, int offset, int limit, String sort, String order) {
    List<ElasticSearchFilter> filters = new ArrayList<>();
    filters.add(new ElasticSearchFilter(ElasticSearchFilterType.FILTER_BY_TERM, "wikiType", wikiType));
    filters.add(new ElasticSearchFilter(ElasticSearchFilterType.FILTER_BY_TERM, "wikiOwner", wikiOwner));
    List<SearchResult> searchResults = filteredWikiSearch(null, searchedText, filters, null, offset, limit, sort, order);
    return searchResults;
  }

  protected List<SearchResult> filteredWikiSearch(SearchContext context, String query, List<ElasticSearchFilter> filters, Collection<String> sites,
                                                  int offset, int limit, String sort, String order) {
    String esQuery = buildFilteredQuery(query, sites, filters, offset, limit, sort, order);
    String jsonResponse = getClient().sendRequest(esQuery, getIndex(), getType());
    return buildWikiResult(jsonResponse);

  }

  protected List<SearchResult> buildWikiResult(String jsonResponse) {

    List<SearchResult> wikiResults = new ArrayList<>();

    JSONParser parser = new JSONParser();

    Map json;
    try {
      json = (Map) parser.parse(jsonResponse);
    } catch (ParseException e) {
      throw new ElasticSearchException("Unable to parse JSON response", e);
    }

    JSONObject jsonResult = (JSONObject) json.get("hits");
    JSONArray jsonHits = (JSONArray) jsonResult.get("hits");

    for (Object jsonHit : jsonHits) {

      long score = ((Double) ((JSONObject) jsonHit).get("_score")).longValue();

      JSONObject hitSource = (JSONObject) ((JSONObject) jsonHit).get("_source");

      String title = (String) hitSource.get("title");
      String url = (String) hitSource.get("url");

      String wikiType = (String) hitSource.get("wikiType");
      String wikiOwner = (String) hitSource.get("wikiOwner");

      Calendar createdDate = Calendar.getInstance();
      createdDate.setTimeInMillis(Long.parseLong((String) hitSource.get("createdDate")));
      Calendar updatedDate = Calendar.getInstance();
      updatedDate.setTimeInMillis(Long.parseLong((String) hitSource.get("updatedDate")));

      SearchResultType type = SearchResultType.PAGE;
      String pageName = (String) hitSource.get("name");
      String attachmentName = null;

      //Result can be an attachment
      if (((JSONObject) jsonHit).get("_type").equals("wiki-attachment")) {
        pageName = (String) hitSource.get("pageName");
        attachmentName = (String) hitSource.get("name");
      }

      //Get the excerpt
      JSONObject hitHighlight = (JSONObject) ((JSONObject) jsonHit).get("highlight");
      StringBuilder excerpt = new StringBuilder();
      if(hitHighlight != null) {
        Iterator<?> keys = hitHighlight.keySet().iterator();
        while (keys.hasNext()) {
          String key = (String) keys.next();
          JSONArray highlights = (JSONArray) hitHighlight.get(key);
          for (Object highlight : highlights) {
            excerpt.append("... ").append(highlight);
          }
        }
      }

      //Create the wiki serch result
      SearchResult wikiSearchResult = new SearchResult();
      wikiSearchResult.setWikiType(wikiType);
      wikiSearchResult.setWikiOwner(wikiOwner);
      wikiSearchResult.setPageName(pageName);
      wikiSearchResult.setAttachmentName(attachmentName);
      wikiSearchResult.setExcerpt(excerpt.toString());
      wikiSearchResult.setTitle(title);
      wikiSearchResult.setType(type);
      wikiSearchResult.setCreatedDate(createdDate);
      wikiSearchResult.setUpdatedDate(updatedDate);
      wikiSearchResult.setUrl(url);
      wikiSearchResult.setScore(score);

      //Add the wiki search result to the list of search results
      wikiResults.add(wikiSearchResult);

    }

    return wikiResults;

  }

}

