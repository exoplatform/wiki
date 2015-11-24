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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.plugin;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.wiki.mow.api.EmotionIcon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WikiEmotionIconsPlugin extends BaseComponentPlugin {
  List<EmotionIcon> emotionIcons = new ArrayList<EmotionIcon>();

  @SuppressWarnings("unchecked")
  public WikiEmotionIconsPlugin(InitParams params) throws Exception {
    emotionIcons.addAll(params.getObjectParamValues(EmotionIcon.class));
  }

  public List<EmotionIcon> getEmotionIcons() {
    return this.emotionIcons;
  }
}
