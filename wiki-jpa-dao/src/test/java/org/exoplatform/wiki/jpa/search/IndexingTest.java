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
import org.exoplatform.wiki.jpa.entity.PageAttachmentEntity;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.service.search.WikiSearchData;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 9/14/15
 */
public class IndexingTest extends BaseWikiESIntegrationTest {

  public void testReindexingWikiPagesAndSearch() throws NoSuchFieldException, IllegalAccessException, IOException {
    // Given
    PageEntity page = indexPage("RDBMS Guidelines",
                                "RDBMS Guidelines",
                                "All the guidelines you need",
                                "Draft version",
                                "BCH",
                                null);
    page.setName("Liquibase Guidelines");
    page.setTitle("Liquibase Guidelines");
    pageDAO.update(page);
    page = indexPage("RDBMS Stats", "RDBMS Stats", "All the stats you need", "Draft version", "BCH", null);
    page.setName("Liquibase Stats");
    page.setTitle("Liquibase Stats");
    pageDAO.update(page);
    assertEquals(2, pageDAO.findAll().size());
    // When
    indexingService.reindexAll(WikiPageIndexingServiceConnector.TYPE);
    indexingOperationProcessor.process();
    // Second time because operations were reinjected in the queue
    indexingOperationProcessor.process();
    node.client().admin().indices().prepareRefresh().execute().actionGet();
    // Then
    assertEquals(2, storage.search(new WikiSearchData("Liquibase", null, "test", "BCH")).getPageSize());
  }

  public void testReindexingAttachmentAndSearch() throws NoSuchFieldException, IllegalAccessException, IOException {
    // Given
    PageAttachmentEntity attachment1 = indexAttachment("Scrum @eXo - Collector",
        fileResource.getPath(),
        "www.exo.com",
        "BCH");
    pageAttachmentDAO.create(attachment1);
    PageAttachmentEntity attachment2 = indexAttachment("Scrum @eXo - Collector",
        fileResource.getPath(),
        "www.exo.com",
        "BCH");
    pageAttachmentDAO.create(attachment2);
    assertEquals(2, pageAttachmentDAO.findAll().size());
    // When
    indexingService.reindexAll(AttachmentIndexingServiceConnector.TYPE);
    indexingOperationProcessor.process();
    // Second time because operations were reinjected in the queue
    indexingOperationProcessor.process();
    node.client().admin().indices().prepareRefresh().execute().actionGet();
    // Then
    assertEquals(2, storage.search(new WikiSearchData("Agile", null, "test", "BCH")).getPageSize());
  }

}
