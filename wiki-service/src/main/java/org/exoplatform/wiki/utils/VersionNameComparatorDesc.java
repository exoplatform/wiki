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
package org.exoplatform.wiki.utils;

import java.io.Serializable;
import java.util.Comparator;

import org.exoplatform.wiki.mow.api.PageVersion;

public class VersionNameComparatorDesc implements Comparator<PageVersion>,Serializable {

  public int compare(PageVersion version1, PageVersion version2) {
    if (version1.getName().length() == version2.getName().length()) {
      return version2.getName().compareTo(version1.getName());
    } else {
      return version2.getName().length() > version1.getName().length() ? 1 : -1;
    }
  }
}
