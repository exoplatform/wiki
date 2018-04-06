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
import org.exoplatform.wiki.jpa.entity.PermissionEntity;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.service.search.WikiSearchData;

import java.util.Arrays;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 9/14/15
 */
public class WikiPagePermissionsTest extends BaseWikiESIntegrationTest {
  public void testSearchPage_byOwner_found() throws NoSuchFieldException, IllegalAccessException {
    // Given
    SecurityUtils.setCurrentUser("BCH", "*:/admin");
    // When
    indexPage("RDBMS Guidelines", "RDBMS Guidelines", "RDBMS Guidelines", "RDBMS Guidelines", "BCH", null);
    // Then
    assertEquals(1, storage.search(new WikiSearchData("RDBMS", null, "test", "BCH")).getPageSize());
  }

  public void testSearchPage_byOwner_notFound() throws NoSuchFieldException, IllegalAccessException {
    // Given
    SecurityUtils.setCurrentUser("BCH", "*:/admin");
    // When
    indexPage("RDBMS Guidelines", "RDBMS Guidelines", "RDBMS Guidelines", "RDBMS Guidelines", "JOHN", null);
    // Then
    assertEquals(0, storage.search(new WikiSearchData("RDBMS", null, "test", "BCH")).getPageSize());
  }

  public void testSearchPage_byUserPermission_notFound() throws NoSuchFieldException, IllegalAccessException {
    // Given
    SecurityUtils.setCurrentUser("BCH", "*:/admin");
    PermissionEntity permission1 = new PermissionEntity("JOHN", "User", PermissionType.VIEWPAGE);
    PermissionEntity permission2 = new PermissionEntity("MARY", "User", PermissionType.VIEWPAGE);
    // When
    indexPage("RDBMS Guidelines", "RDBMS Guidelines", "RDBMS Guidelines", "RDBMS Guidelines", "JOHN", Arrays.asList(permission1, permission2));
    // Then
    assertEquals(0, storage.search(new WikiSearchData("RDBMS", null, "test", "BCH")).getPageSize());
  }

  public void testSearchPage_byUserPermission_found() throws NoSuchFieldException, IllegalAccessException {
    // Given
    SecurityUtils.setCurrentUser("BCH", "*:/admin");
    PermissionEntity permission1 = new PermissionEntity("JOHN", "User", PermissionType.VIEWPAGE);
    PermissionEntity permission2 = new PermissionEntity("BCH", "User", PermissionType.VIEWPAGE);
    // When
    indexPage("RDBMS Guidelines", "RDBMS Guidelines", "RDBMS Guidelines", "RDBMS Guidelines", "JOHN", Arrays.asList(permission1, permission2));
    // Then
    assertEquals(1, storage.search(new WikiSearchData("RDBMS", null, "test", "BCH")).getPageSize());
  }

  public void testSearchPage_byGroupPermission_found() throws NoSuchFieldException, IllegalAccessException {
    // Given
    SecurityUtils.setCurrentUser("BCH", "administrator:/admin");
    PermissionEntity permission1 = new PermissionEntity("JOHN", "User", PermissionType.VIEWPAGE);
    PermissionEntity permission2 = new PermissionEntity("administrator:/admin", "Group", PermissionType.VIEWPAGE);
    // When
    indexPage("RDBMS Guidelines", "RDBMS Guidelines", "RDBMS Guidelines", "RDBMS Guidelines", "JOHN", Arrays.asList(permission1, permission2));
    // Then
    assertEquals(1, storage.search(new WikiSearchData("RDBMS", null, "test", "BCH")).getPageSize());
  }

  public void testSearchPage_byWildcardGroupPermission_found() throws NoSuchFieldException, IllegalAccessException {
    // Given
    SecurityUtils.setCurrentUser("BCH", "administrator:/admin");
    PermissionEntity permission1 = new PermissionEntity("JOHN", "User", PermissionType.VIEWPAGE);
    PermissionEntity permission2 = new PermissionEntity("*:/admin", "Group", PermissionType.VIEWPAGE);
    // When
    indexPage("RDBMS Guidelines", "RDBMS Guidelines", "RDBMS Guidelines", "RDBMS Guidelines", "JOHN", Arrays.asList(permission1, permission2));
    // Then
    assertEquals(1, storage.search(new WikiSearchData("RDBMS", null, "test", "BCH")).getPageSize());
  }

  public void testSearchPage_byGroupPermission_notFound() throws NoSuchFieldException, IllegalAccessException {
    // Given
    SecurityUtils.setCurrentUser("BCH", "administrator:/admin");
    PermissionEntity permission1 = new PermissionEntity("JOHN", "User", PermissionType.VIEWPAGE);
    PermissionEntity permission2 = new PermissionEntity("indexer:/admin", "Group", PermissionType.VIEWPAGE);
    // When
    indexPage("RDBMS Guidelines", "RDBMS Guidelines", "RDBMS Guidelines", "RDBMS Guidelines", "JOHN", Arrays.asList(permission1, permission2));
    // Then
    assertEquals(0, storage.search(new WikiSearchData("RDBMS", null, "test", "BCH")).getPageSize());
  }
}
