/* 
* Copyright (C) 2003-2016 eXo Platform SAS.
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

import org.exoplatform.addons.es.client.ElasticSearchingClient;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.utils.Utils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 2/22/16
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Utils.class)
public class WikiElasticUnifiedSearchServiceConnectorTest {

  private WikiElasticUnifiedSearchServiceConnector searchServiceConnector;

  SearchContext context = new SearchContext(null, "siteName");

  @Mock
  private WikiService wService;

  @Mock
  private ElasticSearchingClient elasticSearchingClient;

  @Before
  public void initMocks() throws Exception {

    //MockitoAnnotations.initMocks(this);

    //Init wikiSearchServiceConnector
    InitParams params = new InitParams();
    PropertiesParam properties = new PropertiesParam();
    properties.setProperty("searchType", "wiki-es");
    properties.setProperty("displayName", "wiki-es");
    properties.setProperty("index", "wiki");
    properties.setProperty("type", "wiki,wiki-page,wiki-attachment");
    properties.setProperty("titleField", "title");
    properties.setProperty("searchFields", "name,title,content,comment,file");
    params.put("constructor.params", properties);
    this.searchServiceConnector = new WikiElasticUnifiedSearchServiceConnector(params, elasticSearchingClient, wService);

    //Mock the wiki Utils static class
    PowerMockito.mockStatic(Utils.class);
    when(Utils.getPortalName()).thenReturn("portal");

    //Mock uri of the wikiWebapp
    when(wService.getWikiWebappUri()).thenReturn("wiki");

  }

  @Test
  public void getUrlFromJsonResult_portalWiki_returnWellFormedPortalWikiURL() {

    //Given
    JSONObject hitsource = (JSONObject) JSONValue.parse("{\"wikiType\":\"PORTAL\",\"wikiOwner\":\"intranet\",\"name\":\"pageName\"}");

    //When
    String url = searchServiceConnector.getUrlFromJsonResult(hitsource, context);

    //Then
    assertEquals("/portal/siteName/wiki/pageName", url);
  }

  @Test
  public void getUrlFromJsonResult_userWiki_returnWellFormedUserWikiURL() {

    //Given
    JSONObject hitsource = (JSONObject) JSONValue.parse("{\"wikiType\":\"USER\",\"wikiOwner\":\"thib\",\"name\":\"pageName\"}");

    //When
    String url = searchServiceConnector.getUrlFromJsonResult(hitsource, context);

    //Then
    assertEquals("/portal/siteName/wiki/user/thib/pageName", url);
  }

  @Test
  public void getUrlFromJsonResult_spaceWiki_returnWellFormedSpaceWikiURL() {

    //Given
    JSONObject hitsource = (JSONObject) JSONValue.parse("{\"wikiType\":\"GROUP\",\"wikiOwner\":\"/spaces/spaceId\",\"name\":\"pageName\"}");

    //When
    String url = searchServiceConnector.getUrlFromJsonResult(hitsource, context);

    //Then
    assertEquals("/portal/siteName/wiki/group/spaces/spaceId/pageName", url);
  }

}

