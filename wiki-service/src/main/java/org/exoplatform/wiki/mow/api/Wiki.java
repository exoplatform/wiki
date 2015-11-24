/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wiki.mow.api;

import java.util.List;

public class Wiki {

  /**
   * Wiki unique id
   */
  private String id;

  /**
   * Name of the owner of this wiki. May be a portal name, a group name or a
   * user name depending on the type of the wiki
   */
  private String owner;

  /**
   * Type of this wiki. May be a portal type, a group type or a user type
   */
  private String type;

  /**
   * Home page of the wiki
   */
  private Page wikiHome;

  private List<PermissionEntry> permissions;
  
  private WikiPreferences preferences;
  
  private boolean defaultPermissionsInited;

  public Wiki() {
  }

  public Wiki(String type, String owner) {
    this.type = type;
    this.owner = owner;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Page getWikiHome() {
    return wikiHome;
  }

  public void setWikiHome(Page wikiHome) {
    this.wikiHome = wikiHome;
  }

  public List<PermissionEntry> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<PermissionEntry> permissions) {
    this.permissions = permissions;
  }

  public WikiPreferences getPreferences() {
    return preferences;
  }

  public void setPreferences(WikiPreferences preferences) {
    this.preferences = preferences;
  }

  public boolean isDefaultPermissionsInited() {
    return defaultPermissionsInited;
  }

  public void setDefaultPermissionsInited(boolean defaultPermissionsInited) {
    this.defaultPermissionsInited = defaultPermissionsInited;
  }
}
