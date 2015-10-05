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
package org.exoplatform.wiki.service.search;

import org.exoplatform.wiki.utils.Utils;

public class SearchData {
  public String title;

  public String content;

  public String wikiType;

  public String wikiOwner;

  public String pageId;
  
  private long offset = 0;
  
  protected String sort;
  
  protected String order;
  
  public int limit = Integer.MAX_VALUE;

  public SearchData(String title, String content, String wikiType, String wikiOwner, String pageId) {
    this.title = org.exoplatform.wiki.utils.Utils.escapeIllegalCharacterInQuery(title);
    this.content = org.exoplatform.wiki.utils.Utils.escapeIllegalCharacterInQuery(content);
    this.wikiType = wikiType;
    this.wikiOwner = Utils.validateWikiOwner(wikiType, wikiOwner);
    this.pageId = pageId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = org.exoplatform.wiki.utils.Utils.escapeIllegalCharacterInQuery(title);
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = org.exoplatform.wiki.utils.Utils.escapeIllegalCharacterInQuery(content);
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

  public String getPageId() {
    return pageId;
  }

  public void setPageId(String pageId) {
    this.pageId = pageId;
  }
  
  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }
  
  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }

  public String getSort() {
    return sort;
  }

  public void setSort(String sort) {
    this.sort = sort;
  }

  public String getOrder() {
    return order;
  }

  public void setOrder(String order) {
    this.order = order;
  }
  
}
