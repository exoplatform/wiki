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

import java.util.Date;
import java.util.List;

public class Page {

  private String id;

  private String name;

  private String owner;

  private String author;

  private Date createdDate;

  private Date updatedDate;

  private String content;

  private String syntax;

  private String title;

  private String comment;

  private List<PermissionEntry> permissions;

  private String url;

  private String activityId;

  private String wikiId;

  private String wikiType;

  private String wikiOwner;

  private boolean isMinorEdit;

  public Page() {
  }

  public Page(String name) {
    this.name = name;
  }

  public Page(String name, String title) {
    this();
    this.name = name;
    this.title = title;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Date getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getSyntax() {
    return syntax;
  }

  public void setSyntax(String syntax) {
    this.syntax = syntax;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public List<PermissionEntry> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<PermissionEntry> permissions) {
    this.permissions = permissions;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public String getWikiId() {
    return wikiId;
  }

  public void setWikiId(String wikiId) {
    this.wikiId = wikiId;
  }

  public String getWikiType() {
    return wikiType;
  }

  public void setWikiType(String wikiType) {
    this.wikiType = wikiType;
  }

  public String getWikiOwner() {
    return wikiOwner;
  }

  public void setWikiOwner(String wikiOwner) {
    this.wikiOwner = wikiOwner;
  }

  public boolean isMinorEdit() {
    return isMinorEdit;
  }

  public void setMinorEdit(boolean isMinorEdit) {
    this.isMinorEdit = isMinorEdit;
  }
}
