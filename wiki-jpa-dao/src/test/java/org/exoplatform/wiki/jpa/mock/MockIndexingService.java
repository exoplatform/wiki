/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.wiki.jpa.mock;

import org.exoplatform.commons.search.index.IndexingService;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class MockIndexingService implements IndexingService {

  private Map<String, Integer> count = new HashMap<>();

  @Override
  public void init(String s) {
    increaseCount("init");
  }

  @Override
  public void index(String s, String s1) {
    increaseCount("index");
  }

  @Override
  public void reindex(String s, String s1) {
    increaseCount("reindex");
  }

  @Override
  public void unindex(String s, String s1) {
    increaseCount("unindex");
  }

  @Override
  public void reindexAll(String s) {
    increaseCount("reindexAll");
  }

  @Override
  public void unindexAll(String s) {
    increaseCount("unindexAll");
  }

  private synchronized void increaseCount(String name) {
    Integer v = count.get(name);
    if (v == null) {
      v = 0;
    } else {
      v ++;
    }
    count.put(name, v);
  }

  public int getCount(String name) {
    Integer v = count.get(name);
    if (v != null) {
      return v.intValue();
    } else {
      return 0;
    }
  }
}
