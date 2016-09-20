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
import org.exoplatform.wiki.jpa.SecurityUtils;
import org.exoplatform.wiki.service.search.WikiSearchData;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 9/14/15
 */
public class AttachmentPermissionsTest extends BaseWikiESIntegrationTest {

  public void testSearchAttachment_byOwner_found() throws NoSuchFieldException, IllegalAccessException, IOException {
    // Given
    SecurityUtils.setCurrentUser("BCH", "*:/admin");
    // When
    indexAttachment("Scrum @eXo - Collector", fileResource.getPath(), "www.exo.com", "BCH");
    // Then
    assertEquals(1, storage.search(new WikiSearchData("Agile", null, "test", "BCH")).getPageSize());
  }

  public void testSearchAttachment_byOwner_notFound() throws NoSuchFieldException, IllegalAccessException, IOException {
    // Given
    SecurityUtils.setCurrentUser("BCH", "*:/admin");
    // When
    indexAttachment("Scrum @eXo - Collector", fileResource.getPath(), "www.exo.com", "JOHN");
    // Then
    assertEquals(0, storage.search(new WikiSearchData("Agile", null, "test", "BCH")).getPageSize());
  }

}
