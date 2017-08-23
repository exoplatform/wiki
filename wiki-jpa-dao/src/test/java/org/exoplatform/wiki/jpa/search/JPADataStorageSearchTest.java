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

import org.exoplatform.wiki.jpa.BaseWikiESIntegrationTest;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 10/21/15
 */
public class JPADataStorageSearchTest extends BaseWikiESIntegrationTest {
  
    @Test
    public void testSearchPageByName() throws Exception {
        // Given
        // When
        indexPage("My name", "My title", "This is the content of my Page", "This is a comment", "BCH", null);
        // Then
        assertEquals(1, storage.search(new WikiSearchData(null, "name", "test", "BCH")).getPageSize());
        assertEquals(1, storage.search(new WikiSearchData("name", null, "test", "BCH")).getPageSize());
    }

    @Test
    public void testSearchPageByTitle() throws Exception {
        // Given
        // When
        indexPage("My name", "My title", "This is the content of my Page", "This is a comment", "BCH", null);
        // Then
        assertEquals(1, storage.search(new WikiSearchData(null, "Title", "test", "BCH")).getPageSize());
        assertEquals(1, storage.search(new WikiSearchData("Title", null, "test", "BCH")).getPageSize());
    }

    @Test
    public void testSearchPageByContent() throws Exception {
        // Given
        // When
        indexPage("My Page", "My Page", "This is the content of my Page", "This is a comment", "BCH", null);
        // Then
        assertEquals(1, storage.search(new WikiSearchData(null, "content", "test", "BCH")).getPageSize());
        assertEquals(1, storage.search(new WikiSearchData("content", null, "test", "BCH")).getPageSize());
    }

    @Test
    public void testSearchPageByComment() throws Exception {
        // Given
        // When
        indexPage("My Page", "My Page", "This is the content of my Page", "This is a comment", "BCH", null);
        // Then
        assertEquals(1, storage.search(new WikiSearchData("comment", null, "test", "BCH")).getPageSize());
        assertEquals(1, storage.search(new WikiSearchData(null, "comment", "test", "BCH")).getPageSize());
    }

    @Test
    public void testSearchAttachmentByTitle() throws NoSuchFieldException, IllegalAccessException, IOException {
        // Given
        InputStream fileResource = this.getClass().getClassLoader().getResourceAsStream("AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf");
        // When
        indexAttachment("Scrum @eXo - Collector", fileResource, "BCH");
        // Then
        assertEquals(1, storage.search(new WikiSearchData("Collector", null, "test", "BCH")).getPageSize());
        assertEquals(1, storage.search(new WikiSearchData(null, "Collector", "test", "BCH")).getPageSize());
    }

    @Test
    public void testSearchAttachmentByContent() throws NoSuchFieldException, IllegalAccessException, IOException {
        // Given
        InputStream fileResource = this.getClass().getClassLoader().getResourceAsStream("AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf");
        // When
        indexAttachment("Scrum @eXo - Collector", fileResource, "BCH");
        // Then
        assertEquals(1, storage.search(new WikiSearchData("Agile", null, "test", "BCH")).getPageSize());
        assertEquals(1, storage.search(new WikiSearchData(null, "Agile", "test", "BCH")).getPageSize());
    }
    
}
