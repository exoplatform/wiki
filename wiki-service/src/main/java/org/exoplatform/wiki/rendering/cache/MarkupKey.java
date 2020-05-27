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

import java.io.Serializable;

import org.exoplatform.wiki.service.WikiPageParams;

public class MarkupKey implements Serializable {

  private WikiPageParams pageParams;

  private boolean        supportSectionEdit;
  
  /**
   * Instance new markup key
   *
   * @param pageParams the identity params of page
   * @param supportSectionEdit the content supports section editing or not
   */
  public MarkupKey(WikiPageParams pageParams, boolean supportSectionEdit) {
    this.pageParams = pageParams;
    this.supportSectionEdit = supportSectionEdit;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((pageParams == null) ? 0 : pageParams.hashCode());
    result = prime * result + (supportSectionEdit ? 1231 : 1237);
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MarkupKey other = (MarkupKey) obj;
    if (pageParams == null) {
      if (other.pageParams != null)
        return false;
    } else if (!pageParams.equals(other.pageParams))
      return false;
    if (supportSectionEdit != other.supportSectionEdit)
      return false;
    return true;
  }

  /**
   * @return the supportSectionEdit
   */
  public boolean isSupportSectionEdit() {
    return supportSectionEdit;
  }

  /**
   * @param supportSectionEdit the supportSectionEdit to set
   */
  public void setSupportSectionEdit(boolean supportSectionEdit) {
    this.supportSectionEdit = supportSectionEdit;
  }
}
