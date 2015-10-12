/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

public class Permission {
  private PermissionType permissionType;

  private boolean isAllowed = false;

  public Permission() {
  }

  public Permission(PermissionType permissionType, boolean isAllowed) {
    this.permissionType = permissionType;
    this.isAllowed = isAllowed;
  }

  public PermissionType getPermissionType() {
    return permissionType;
  }

  public void setPermissionType(PermissionType permissionType) {
    this.permissionType = permissionType;
  }

  public boolean isAllowed() {
    return isAllowed;
  }

  public void setAllowed(boolean isAllowed) {
    this.isAllowed = isAllowed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Permission)) return false;

    Permission that = (Permission) o;

    if (isAllowed != that.isAllowed) return false;
    if (permissionType != that.permissionType) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = permissionType != null ? permissionType.hashCode() : 0;
    result = 31 * result + (isAllowed ? 1 : 0);
    return result;
  }
}
