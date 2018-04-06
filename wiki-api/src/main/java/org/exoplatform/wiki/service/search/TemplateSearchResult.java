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

import java.util.Calendar;

public class TemplateSearchResult extends SearchResult {
 
  protected String description;
  
  protected String name;

  public TemplateSearchResult() {
    super();
  }

  public TemplateSearchResult(String wikiType,
                              String wikiOwner,
                              String name,
                              String title,
                              SearchResultType type,
                              Calendar updatedDate,
                              Calendar createdDate,
                              String description) {
    super(wikiType, wikiOwner, null, null, null, title, type, updatedDate, createdDate);
    this.description = description;
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
