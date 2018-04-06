/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.wiki.rendering.cache;

import org.apache.commons.lang.StringUtils;

public class MarkupData implements CacheData<String> {

  /**
   * 
   */
  private static final long serialVersionUID = 4024500794943104643L;
  private final String markup;
  
  public MarkupData(final String markup) {
    this.markup = markup;
  }
  
  @Override
  public String build() {
    return this.markup;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MarkupData)) return false;

    MarkupData that = (MarkupData) o;

    return StringUtils.equals(markup, that.markup);

  }

  @Override
  public int hashCode() {
    return markup != null ? markup.hashCode() : 0;
  }
}
