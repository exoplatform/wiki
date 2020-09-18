/*
 *
 *  * Copyright (C) 2003-2015 eXo Platform SAS.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 *  
 */

package org.exoplatform.wiki.jpa.search;

import java.util.Collection;

import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.wiki.jpa.BaseWikiESIntegrationTest;
import org.exoplatform.wiki.service.search.WikiSearchData;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 10/21/15
 */
public class ITWikiElasticsearchIntegrationTest extends BaseWikiESIntegrationTest {

    WikiElasticUnifiedSearchServiceConnector searchServiceConnector;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        searchServiceConnector = CommonsUtils.getService(WikiElasticUnifiedSearchServiceConnector.class);
    }

    public void testFindPageByTitle() throws Exception {
        // Given
        SearchContext searchContext = new SearchContext(CommonsUtils.getService(Router.class), "intranet");
        // When
        indexPage("My_name", "My title", "This is the content of my Page", "This is a comment", "BCH", null);
        // Then
        Collection<SearchResult> searchResults = searchServiceConnector.search(searchContext, "title", null, 0, 50, null, null);
        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());
        SearchResult foundPage = searchResults.iterator().next();
        assertEquals("My title", foundPage.getTitle());
        assertEquals("... My <strong>title</strong>", foundPage.getExcerpt());
        assertEquals("/portal/intranet/wiki/My_name", foundPage.getUrl());
    }

    public void testSearchWikiWithHTMLTag() throws Exception {
        // Given
        SearchContext searchContext = new SearchContext(CommonsUtils.getService(Router.class), "intranet");

        // search a wiki page containing an HTML tag
        indexPage("My_name", "My title", "<h1> test tag html <h1> <span> test 2 <span>", "This is a comment", "BCH", null);

        Collection<SearchResult> searchResults = searchServiceConnector.search(searchContext, "test tag", null, 0, 50, null, null);
        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());
        SearchResult foundPage = searchResults.iterator().next();
        assertEquals("My title", foundPage.getTitle());
        assertEquals("...  test tag  html    test  2", foundPage.getExcerpt());
    }
}
