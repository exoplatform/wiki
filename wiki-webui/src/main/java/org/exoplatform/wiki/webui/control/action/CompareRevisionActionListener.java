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
package org.exoplatform.wiki.webui.control.action;

import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.wiki.mow.api.PageVersion;
import org.exoplatform.wiki.webui.UIWikiPageVersionsCompare;
import org.exoplatform.wiki.webui.UIWikiPortlet;
import org.exoplatform.wiki.webui.WikiMode;

import java.util.ArrayList;
import java.util.List;

public class CompareRevisionActionListener extends EventListener<UIComponent> {
  
  private int                  from             = 1;

  private int                  to               = 0;
  
  private List<PageVersion> versionToCompare = new ArrayList<>();
  
  public List<PageVersion> getVersionToCompare() {
    return versionToCompare;
  }

  public void setVersionToCompare(List<PageVersion> versionToCompare) {
    this.versionToCompare = versionToCompare;
  }

  public int getFrom() {
    return from;
  }

  public void setFrom(int from) {
    this.from = from;
  }

  public int getTo() {
    return to;
  }

  public void setTo(int to) {
    this.to = to;
  }

  @Override
  public void execute(Event<UIComponent> event) throws Exception {
    UIWikiPortlet wikiPortlet = event.getSource().getAncestorOfType(UIWikiPortlet.class);
    UIWikiPageVersionsCompare versionCompareArea = wikiPortlet.findFirstComponentOfType(UIWikiPageVersionsCompare.class);
    if (versionToCompare.size() > 1) {
      versionCompareArea.renderVersionsDifference(versionToCompare, from, to);
      wikiPortlet.changeMode(WikiMode.COMPAREREVISION);
    }
  }
}
