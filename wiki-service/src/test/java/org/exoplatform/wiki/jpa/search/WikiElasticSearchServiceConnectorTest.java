package org.exoplatform.wiki.jpa.search;

import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wiki.service.search.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class WikiElasticSearchServiceConnectorTest {

  private WikiElasticSearchServiceConnector searchServiceConnector;

  @Mock
  private ElasticSearchingClient elasticSearchingClient;

  @Mock
  private IdentityManager identityManager;

  @Test
  public void shouldReturnResultsWithoutExcerptWhenNoHighlight() {
    // Given
    Mockito.when(elasticSearchingClient.sendRequest(Matchers.any(), Matchers.any(), Matchers.any()))
            .thenReturn("{\n" +
                    "  \"took\": 939,\n" +
                    "  \"timed_out\": false,\n" +
                    "  \"_shards\": {\n" +
                    "    \"total\": 5,\n" +
                    "    \"successful\": 5,\n" +
                    "    \"failed\": 0\n" +
                    "  },\n" +
                    "  \"hits\": {\n" +
                    "    \"total\": 4,\n" +
                    "    \"max_score\": 1.0,\n" +
                    "    \"hits\": [{\n" +
                    "      \"_index\": \"wiki\",\n" +
                    "      \"_type\": \"wiki-page\",\n" +
                    "      \"_id\": \"2\",\n" +
                    "      \"_score\": 1.0,\n" +
                    "      \"_source\": {\n" +
                    "        \"wikiOwner\": \"intranet\",\n" +
                    "        \"createdDate\": \"1494833363955\",\n" +
                    "        \"name\": \"Page_1\",\n" +
                    "        \"wikiType\": \"portal\",\n" +
                    "        \"updatedDate\": \"1494833363955\",\n" +
                    "        \"title\": \"Page 1\",\n" +
                    "        \"url\": \"/portal/intranet/wiki/Page_1\"\n" +
                    "      }\n" +
                    "    }, {\n" +
                    "      \"_index\": \"wiki\",\n" +
                    "      \"_type\": \"wiki-page\",\n" +
                    "      \"_id\": \"3\",\n" +
                    "      \"_score\": 1.0,\n" +
                    "      \"_source\": {\n" +
                    "        \"wikiOwner\": \"intranet\",\n" +
                    "        \"createdDate\": \"1494833380251\",\n" +
                    "        \"name\": \"Page_2\",\n" +
                    "        \"wikiType\": \"portal\",\n" +
                    "        \"updatedDate\": \"1494833380251\",\n" +
                    "        \"title\": \"Page 2\",\n" +
                    "        \"url\": \"/portal/intranet/wiki/Page_2\"\n" +
                    "      }\n" +
                    "    }]\n" +
                    "  }\n" +
                    "}");

    InitParams initParams = new InitParams();
    PropertiesParam properties = new PropertiesParam();
    properties.setProperty("searchType", "wiki-es");
    properties.setProperty("displayName", "wiki-es");
    properties.setProperty("index", "wiki");
    properties.setProperty("type", "wiki,wiki-page,wiki-attachment");
    properties.setProperty("titleField", "title");
    properties.setProperty("searchFields", "name,title,content,comment,file");
    initParams.put("constructor.params", properties);

    this.searchServiceConnector = new WikiElasticSearchServiceConnector(initParams, elasticSearchingClient, identityManager) {
      @Override
      protected String getPermissionFilter() {
        return "";
      }
    };

    // when
    List<SearchResult> searchResults = searchServiceConnector.searchWiki("*", "portal", "intranet", 0, 20, "exo:lastModifiedDate", "DESC");

    // Then
    assertNotNull(searchResults);
    assertEquals(2, searchResults.size());
    assertEquals("", searchResults.get(0).getExcerpt());
    assertEquals("", searchResults.get(1).getExcerpt());
  }
}