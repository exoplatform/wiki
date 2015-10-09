/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.exoplatform.wiki.mow.api.Permission;
import org.exoplatform.wiki.mow.api.PermissionEntry;
import org.exoplatform.wiki.mow.api.PermissionType;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 8, 2015  
 */
public class TestUtils extends TestCase {
  //Only work with PermissionType.VIEWPAGE and PermissionType.EDITPAGE
  public void testConvertToPermissionMap(){
    List<PermissionEntry> permissionEntries = new ArrayList<PermissionEntry>();
    Permission permission = new Permission();
    permission.setAllowed(true);
    permission.setPermissionType(PermissionType.VIEWPAGE);
    PermissionEntry per = new PermissionEntry();
    per.setId("ID");
    per.setPermissions(new Permission[]{permission});
    permissionEntries.add(per);
    String[] pers = Utils.convertToPermissionMap(permissionEntries).get("ID");
    assertNotNull(pers);    
    assertEquals(1, pers.length);
  }
//Only work with PermissionType.VIEWPAGE and PermissionType.EDITPAGE
  public void testConvertToPermissionEntryList(){
    HashMap<String, String[]> permissions = new HashMap<String, String[]>();
    permissions.put("ID", new String[]{"read"});
    List<PermissionEntry> permissionEntries = Utils.convertToPermissionEntryList(permissions);
    assertEquals(1, permissionEntries.size());
    assertEquals("ID", permissionEntries.get(0).getId());
  }
}
