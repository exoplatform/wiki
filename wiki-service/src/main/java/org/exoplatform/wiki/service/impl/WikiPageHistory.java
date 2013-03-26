/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.wiki.service.impl;

import org.exoplatform.wiki.service.WikiPageParams;

public class WikiPageHistory {
  private WikiPageParams pageParams;
  
  private String username;
  
  private long editTime;
 
  private String draftName;
  
  private boolean isNewPage;

  public WikiPageHistory(WikiPageParams pageParams, String username, String draftName, boolean isNewPage) {
    this.pageParams = pageParams;
    this.username = username;
    this.draftName = draftName;
    this.isNewPage = isNewPage;
  }

  public WikiPageParams getPageParams() {
    return pageParams;
  }

  public String getUsername() {
    return username;
  }

  public long getEditTime() {
    return editTime;
  }
  
  public void setEditTime(long updateTime) {
    this.editTime = updateTime;
  }

  public String getDraftName() {
    return draftName;
  }

  public boolean isNewPage() {
    return isNewPage;
  }
}
