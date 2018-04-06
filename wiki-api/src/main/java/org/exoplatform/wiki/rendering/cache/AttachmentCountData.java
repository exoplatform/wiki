/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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

/**
 * This class stores the number of attachment of a wiki page.
 * 
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 22, 2013  
 */
public class AttachmentCountData implements CacheData<Integer>{

  private int count_ = -1;
  /**
   * 
   */
  private static final long serialVersionUID = 7871302414513113604L;
  
  public AttachmentCountData(int count) {
    this.count_ = count;
  }

  @Override
  public Integer build() {
    return count_;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AttachmentCountData)) return false;

    AttachmentCountData that = (AttachmentCountData) o;

    return count_ == that.count_;

  }

  @Override
  public int hashCode() {
    return count_;
  }
}
