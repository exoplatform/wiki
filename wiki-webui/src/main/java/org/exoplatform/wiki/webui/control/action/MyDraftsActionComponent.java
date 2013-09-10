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
package org.exoplatform.wiki.webui.control.action;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.wiki.webui.UIWikiMyDraftsForm;
import org.exoplatform.wiki.webui.UIWikiPortlet;
import org.exoplatform.wiki.webui.WikiMode;
import org.exoplatform.wiki.webui.control.action.core.AbstractEventActionComponent;
import org.exoplatform.wiki.webui.control.filter.IsUserFilter;
import org.exoplatform.wiki.webui.control.listener.BrowseContainerActionListener;

@ComponentConfig(
    template = "app:/templates/wiki/webui/control/action/MyDraftsActionComponent.gtmpl",                
    events = {
      @EventConfig(listeners = MyDraftsActionComponent.MyDraftsActionListener.class) 
    }
  )
public class MyDraftsActionComponent extends AbstractEventActionComponent {
  public static final String ACTION = "MyDrafts";
  
  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] { new IsUserFilter() });

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  } 

  @Override
  public String getActionName() {
    return ACTION;
  }

  @Override
  public boolean isAnchor() {
    return true;
  }
  
  public static class MyDraftsActionListener extends BrowseContainerActionListener<MyDraftsActionComponent> {
    @Override
    protected void processEvent(Event<MyDraftsActionComponent> event) throws Exception {
      UIWikiPortlet uiWikiPortlet = event.getSource().getAncestorOfType(UIWikiPortlet.class);
      UIWikiMyDraftsForm uiWikiMyDraftsForm = uiWikiPortlet.findFirstComponentOfType(UIWikiMyDraftsForm.class);
      uiWikiMyDraftsForm.initGrid();
      event.getSource().getAncestorOfType(UIWikiPortlet.class).changeMode(WikiMode.MYDRAFTS);
    }
  }
}
