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

import java.util.Calendar;
import java.util.List;

public class Attachment {

  private String name;

  /**
   * Get the weight of the attachment in bytes
   */
  private long weightInBytes;

  /**
   * Creator of the last version of the attachment
   */
  private String creator;

  /**
   * Date of the creation
   */
  private Calendar createdDate;

  /**
   * Date of last update of this attachment
   */
  private Calendar updatedDate;

  /**
   * URL to download the attachment
   */
  private String downloadURL;

  /**
   * Title of the attachment
   */
  private String title;

  /**
   * Full title of the attachment
   */
  private String fullTitle;

  /**
   * Content of the attachment
   */
  private byte[] content;

  /**
   * Mime type of the attachment
   */
  private String mimeType;

  /**
   * Permissions on the attachment
   */
  private List<PermissionEntry> permissions;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getWeightInBytes() {
    return weightInBytes;
  }

  public void setWeightInBytes(long weightInBytes) {
    this.weightInBytes = weightInBytes;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public Calendar getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Calendar createdDate) {
    this.createdDate = createdDate;
  }

  public Calendar getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Calendar updatedDate) {
    this.updatedDate = updatedDate;
  }

  public String getDownloadURL() {
    return downloadURL;
  }

  public void setDownloadURL(String downloadURL) {
    this.downloadURL = downloadURL;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getFullTitle() {
    return fullTitle;
  }

  public void setFullTitle(String fullTitle) {
    this.fullTitle = fullTitle;
  }

  public byte[] getContent() {
    return content;
  }

  public void setContent(byte[] content) {
    this.content = content;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public List<PermissionEntry> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<PermissionEntry> permissions) {
    this.permissions = permissions;
  }
}
