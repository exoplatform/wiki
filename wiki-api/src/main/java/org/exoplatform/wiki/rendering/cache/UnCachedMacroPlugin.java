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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValuesParam;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 24, 2013  
 */
public class UnCachedMacroPlugin extends BaseComponentPlugin {
  private Set<String> uncachedMacroes = new HashSet<String>();
  
  public UnCachedMacroPlugin(InitParams params) {
    if (params != null) {
      ValuesParam values = params.getValuesParam("uncachedMacroes");
      List<String> names = values.getValues();
      for (String name : names) {
        uncachedMacroes.add(name);
      }
    }
  }
  
  public Set<String> getUncachedMacroes() {
    return new HashSet<String>(uncachedMacroes);
  }
}
