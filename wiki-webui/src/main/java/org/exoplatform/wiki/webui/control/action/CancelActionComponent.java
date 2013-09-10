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
package org.exoplatform.wiki.webui.control.action;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.webui.UIWikiPortlet;
import org.exoplatform.wiki.webui.WikiMode;
import org.exoplatform.wiki.webui.control.filter.IsEditAddModeFilter;
import org.exoplatform.wiki.webui.control.listener.UISubmitToolBarActionListener;


@ComponentConfig(
  template = "app:/templates/wiki/webui/control/action/CancelActionComponent.gtmpl",
  events = {
      @EventConfig(listeners = CancelActionComponent.CancelActionListener.class, phase = Phase.DECODE)
    }
)
public class CancelActionComponent extends UIComponent {

  public static final String                   ACTION   = "Cancel";
  
  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] { new IsEditAddModeFilter() });

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }
  
  protected boolean isNewMode() {
    return (WikiMode.ADDPAGE.equals(getAncestorOfType(UIWikiPortlet.class).getWikiMode()));
  }  

  protected String getCurrentPageURL() throws Exception {
    return Utils.getURLFromParams(Utils.getCurrentWikiPageParams());
  }
  
  protected String getActionLink() throws Exception {
    return Utils.createFormActionLink(this, ACTION, ACTION);
  }
  
  public static class CancelActionListener extends UISubmitToolBarActionListener<CancelActionComponent> {
    @Override
    protected void processEvent(Event<CancelActionComponent> event) throws Exception {
      PortalRequestContext context = Util.getPortalRequestContext();
      WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
      String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
      org.exoplatform.wiki.utils.Utils.removeLogEditPage(pageParams, currentUser);
      context.sendRedirect(Utils.getURLFromParams(Utils.getCurrentWikiPageParams()));
    }
  }
  
}
