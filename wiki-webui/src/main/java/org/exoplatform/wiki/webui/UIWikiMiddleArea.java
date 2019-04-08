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
package org.exoplatform.wiki.webui;

import java.util.Arrays;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.wiki.WikiPortletPreference;
import org.exoplatform.wiki.webui.core.UIWikiContainer;
import org.exoplatform.portal.webui.util.Util;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;

@ComponentConfig(
                 lifecycle = Lifecycle.class,
                 template = "app:/templates/wiki/webui/UIWikiMiddleArea.gtmpl",
                 events = {
                     @EventConfig(listeners = UIWikiMiddleArea.ShowHideActionListener.class)                     
                   }
               )
public class UIWikiMiddleArea extends UIWikiContainer {

  public static String SHOW_HIDE_ACTION = "ShowHide";
  public static boolean SHOW_LEFT_PANEL_DEFAULT = true;

  public UIWikiMiddleArea() throws Exception {
    super();
    this.accept_Modes = Arrays.asList(new WikiMode[] { WikiMode.VIEW, WikiMode.EDITPAGE,
        WikiMode.ADDPAGE, WikiMode.ADVANCEDSEARCH, WikiMode.SHOWHISTORY, WikiMode.PAGE_NOT_FOUND,
        WikiMode.VIEWREVISION, WikiMode.PAGEINFO,
        WikiMode.ADDTEMPLATE, WikiMode.EDITTEMPLATE, WikiMode.COMPAREREVISION, WikiMode.SPACESETTING, WikiMode.MYDRAFTS });
    addChild(UIWikiNavigationContainer.class, null, null);
    addChild(UIWikiPageContainer.class, null, null);
    addChild(UIWikiPageSettingContainer.class, null, null);
    addChild(UIWikiRelatedPages.class, null, null);
  }
  
  protected boolean isNavigationRender() {
    WikiPortletPreference preferences = this.getAncestorOfType(UIWikiPortlet.class).getPortletPreferences();
    UIWikiNavigationContainer navigation = getChild(UIWikiNavigationContainer.class);
    return (navigation.getAccept_Modes().contains(navigation.getCurrentMode())
        && navigation.isRendered() && preferences.isShowNavigationTree());
  }
  
  protected boolean isPageSettingContainerRender() {
    UIWikiPageSettingContainer settingContainer = getChild(UIWikiPageSettingContainer.class);
    return (settingContainer.getAccept_Modes().contains(settingContainer.getCurrentMode()) && settingContainer.isRendered());
  }

  public static class ShowHideActionListener extends EventListener<UIWikiMiddleArea> {
    @Override
    public void execute(Event<UIWikiMiddleArea> event) throws Exception {
      UIWikiMiddleArea middleArea = event.getSource();
      UIWikiNavigationContainer navigation = middleArea.getChild(UIWikiNavigationContainer.class);
      navigation.setRendered(!navigation.isRendered());
      event.getRequestContext().addUIComponentToUpdateByAjax(middleArea);
    }
  }
  
  public String getLeftPanelWidth() {
    HttpServletRequest req = Util.getPortalRequestContext().getRequest();
    Cookie[] cookies = req.getCookies();
    String width = "";
    
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if(cookie.getName().equals(req.getRemoteUser() + "_leftWidth")) {
          width = "width: " + cookie.getValue() + "px";
          break;
        }
      }
    }
    return width;
  }
  
  public boolean isShowLeftPanel() {
    HttpServletRequest req = Util.getPortalRequestContext().getRequest();
    Cookie[] cookies = req.getCookies();
    boolean showLeftPanel = SHOW_LEFT_PANEL_DEFAULT;
    
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if(cookie.getName().equals(req.getRemoteUser() + "_ShowLeftContainer")) {
          showLeftPanel = Boolean.parseBoolean(cookie.getValue());
          break;
        }
      }
    }
    return showLeftPanel;
  }
}
