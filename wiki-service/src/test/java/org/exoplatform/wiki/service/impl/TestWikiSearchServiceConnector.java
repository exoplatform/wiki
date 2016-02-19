/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.service.impl;


import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.utils.Utils;
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
 * 18/2/16
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Utils.class)
public class TestWikiSearchServiceConnector {

  private WikiSearchServiceConnector wikiSearchServiceConnector;

  SearchContext context = new SearchContext(null, "siteName");

  @Mock
  private WikiService wService;

  @Before
  public void initMocks() throws Exception {

    //MockitoAnnotations.initMocks(this);

    //Init wikiSearchServiceConnector
    InitParams params = new InitParams();
    PropertiesParam properties = new PropertiesParam();
    properties.setProperty("searchType", "wiki");
    properties.setProperty("displayName", "wiki");
    params.put("constructor.params", properties);
    this.wikiSearchServiceConnector = new WikiSearchServiceConnector(params, wService);

    //Mock the wiki Utils static class
    PowerMockito.mockStatic(Utils.class);
    when(Utils.getPortalName()).thenReturn("portal");

    //Mock uri of the wikiWebapp
    when(wService.getWikiWebappUri()).thenReturn("wiki");
    //Portal wikiType
    String portal = "intranet";
    Page portalPage = new Page("pageName");
    portalPage.setWikiOwner(portal);
    portalPage.setWikiType(WikiType.PORTAL.toString());
    when(wService.getPageOfWikiByName(WikiType.PORTAL.toString(), portal, "pageName")).thenReturn(portalPage);
    when(wService.getWikiByTypeAndOwner(WikiType.PORTAL.toString(), portal)).thenReturn(new Wiki(WikiType.PORTAL.toString(), portal));
    //User wikiType
    String user = "thib";
    Page userPage = new Page("pageName");
    userPage.setWikiOwner(user);
    userPage.setWikiType(WikiType.USER.toString());
    when(wService.getPageOfWikiByName(WikiType.USER.toString(), user, "pageName")).thenReturn(userPage);
    when(wService.getWikiByTypeAndOwner(WikiType.USER.toString(), user)).thenReturn(new Wiki(WikiType.USER.toString(), user));
    //Group wikiType
    String space = "/spaces/spaceId";
    Page groupPage = new Page("pageName");
    groupPage.setWikiOwner(space);
    groupPage.setWikiType(WikiType.GROUP.toString());
    when(wService.getPageOfWikiByName(WikiType.GROUP.toString(), space, "pageName")).thenReturn(groupPage);
    when(wService.getWikiByTypeAndOwner(WikiType.GROUP.toString(), space)).thenReturn(new Wiki(WikiType.GROUP.toString(), space));

  }

  @Test
  public void getPagePermalink_portalWiki_returnWellFormedPortalWikiURL() {

    //Given
    SearchResult searchResult = new SearchResult(WikiType.PORTAL.toString(), "intranet", "pageName", null, null, null, null, null, null);

    //When
    String url = wikiSearchServiceConnector.getPagePermalink(context, searchResult);

    //Then
    assertEquals("/portal/siteName/wiki/pageName", url);
  }

  @Test
  public void getPagePermalink_userWiki_returnWellFormedUserWikiURL() {

    //Given
    SearchResult searchResult = new SearchResult(WikiType.USER.toString(), "thib", "pageName", null, null, null, null, null, null);

    //When
    String url = wikiSearchServiceConnector.getPagePermalink(context, searchResult);

    //Then
    assertEquals("/portal/siteName/wiki/user/thib/pageName", url);
  }

  @Test
  public void getPagePermalink_spaceWiki_returnWellFormedSpaceWikiURL() {

    //Given
    SearchResult searchResult = new SearchResult(WikiType.GROUP.toString(), "/spaces/spaceId", "pageName", null, null, null, null, null, null);

    //When
    String url = wikiSearchServiceConnector.getPagePermalink(context, searchResult);

    //Then
    assertEquals("/portal/siteName/wiki/group/spaces/spaceId/pageName", url);
  }

}
