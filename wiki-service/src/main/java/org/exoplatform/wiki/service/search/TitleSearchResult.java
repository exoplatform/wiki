/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.wiki.service.search;

/**
 * Created by The eXo Platform SAS
 * Author : Lai Trung Hieu
 *          hieu.lai@exoplatform.com
 * Sep 22, 2010  
 */
public class TitleSearchResult {
  private String title;

  private String type;

  private String path;

  private String url;
  
  public TitleSearchResult(String title, String path, String type, String url) {
    this.title = title;
    this.type = type;
    this.path = path;
    this.url = url;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
  
  public void setTitle(String title) {
    this.title = title;
  }
  
  public String getTitle() {
    return title;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }
  
  public String getUrl() {
    return url;
  }
}
