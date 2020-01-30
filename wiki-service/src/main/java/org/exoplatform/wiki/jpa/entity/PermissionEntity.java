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
package org.exoplatform.wiki.jpa.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.wiki.mow.api.PermissionType;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 26, 2015  
 */
@Embeddable
@ExoEntity
public class PermissionEntity {
  /**
   * User or Group
   */
  @Column(name = "WIKI_IDENTITY")
  private String identity;

  @Column(name = "IDENTITY_TYPE")
  private String identityType;

  @Column(name="PERMISSION")
  @Enumerated(EnumType.STRING)
  private PermissionType permissionType;


  public PermissionEntity() {
    //Default constructor
  }

  public PermissionEntity(String identity, String identityType, PermissionType permissionType) {
    this.identity = identity;
    this.identityType = identityType;
    this.permissionType = permissionType;
  }

  public String getIdentity() {
    return identity;
  }

  public void setIdentity(String user) {
    this.identity = user;
  }

  public String getIdentityType() {
    return identityType;
  }

  public void setIdentityType(String identityType) {
    this.identityType = identityType;
  }

  public PermissionType getPermissionType() {
    return permissionType;
  }

  public void setPermissionType(PermissionType permissionType) {
    this.permissionType = permissionType;
  }
}
