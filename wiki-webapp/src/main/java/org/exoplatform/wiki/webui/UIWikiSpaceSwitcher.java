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
package org.exoplatform.wiki.webui;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.core.api.wiki.PortalWiki;
import org.exoplatform.wiki.mow.core.api.wiki.UserWiki;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.impl.SpaceBean;
import org.exoplatform.wiki.webui.tree.EventUIComponent;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Hung Phong
 *          phongth@exoplatform.com
 * Oct 23, 2012
 */
@ComponentConfig(
  lifecycle = Lifecycle.class, 
  template = "app:/templates/wiki/webui/UIWikiSpaceSwitcher.gtmpl",
  events = {@EventConfig(listeners = UIWikiSpaceSwitcher.SelectSpaceActionListener.class)}
)
public class UIWikiSpaceSwitcher extends UIContainer {
  public static final String SPACE_ID_PARAMETER = "spaceId";
  
  private static final String MY_SPACE_REST_URL = "/wiki/spaces/mySpaces/";
  
  public static final String SELECT_SPACE_ACTION = "SelectSpace";
  
  private EventUIComponent eventComponent;
  
  private Wiki currentSpace;
  
  public UIWikiSpaceSwitcher() throws Exception {
    currentSpace = Utils.getCurrentWiki();
  }
  
  public void init(EventUIComponent eventComponent) {
    this.eventComponent = eventComponent;
  }
  
  public EventUIComponent getEventComponent() {
    return eventComponent;
  }
  
  private String getPortalName() {
    ExoContainer container = ExoContainerContext.getCurrentContainer() ; 
    PortalContainerInfo containerInfo = (PortalContainerInfo)container.getComponentInstanceOfType(PortalContainerInfo.class);
    return containerInfo.getContainerName();
  }
  
  public Wiki getWikiById(String wikiId) {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    Wiki wiki = null;
    if (wikiId.startsWith("/spaces/")) {
      wiki = wikiService.getWiki(PortalConfig.GROUP_TYPE, wikiId);
    } else if (wikiId.startsWith("/user/")) {
      wikiId = wikiId.substring(wikiId.lastIndexOf('/') + 1);
      wiki = wikiService.getWiki(PortalConfig.USER_TYPE, wikiId);
    } else if (wikiId.startsWith("/" + getPortalName())) {
      wikiId = wikiId.substring(wikiId.lastIndexOf('/') + 1);
      wiki = wikiService.getWiki(PortalConfig.PORTAL_TYPE, wikiId);
    }
    return wiki;
  }
  
  public Wiki getCurrentSpace() {
    return currentSpace;
  }

  public void setCurrentSpace(Wiki currentSpace) {
    this.currentSpace = currentSpace;
  }

  public String getCurrentSpaceName() throws Exception {
    if (currentSpace instanceof PortalWiki) {
      String displayName = currentSpace.getName();
      int slashIndex = displayName.lastIndexOf('/');
      if (slashIndex > -1) {
        displayName = displayName.substring(slashIndex + 1); 
      }
      return displayName;
    }
    
    if (currentSpace instanceof UserWiki) {
      String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
      if (currentSpace.getOwner().equals(currentUser)) {
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        ResourceBundle res = context.getApplicationResourceBundle();
        String mySpaceLabel = res.getString("UIWikiSpaceSwitcher.title.my-space");
        return mySpaceLabel;
      }
      return currentSpace.getOwner();
    }
    
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    return wikiService.getSpaceNameByGroupId(currentSpace.getOwner());
  }
  
  public List<SpaceBean> getSpaces() throws Exception {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    List<SpaceBean> spaceBeans = new ArrayList<SpaceBean>();
    
    // Get portal wiki
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    String portalOwner = portalRequestContext.getPortalOwner();
    String portalName = getPortalName();
    StringBuilder spaceId = new StringBuilder();
    spaceId.append("/");
    spaceId.append(portalName);
    spaceId.append("/");
    spaceId.append(portalOwner);
    spaceBeans.add(new SpaceBean(spaceId.toString(), portalOwner, PortalConfig.PORTAL_TYPE));
    
    // Get user wiki
    String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
    if (!StringUtils.isEmpty(currentUser) && !currentUser.equals(IdentityConstants.ANONIM)) {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      ResourceBundle res = context.getApplicationResourceBundle();
      String mySpaceLabel = res.getString("UIWikiSpaceSwitcher.title.my-space");
      spaceBeans.add(new SpaceBean("/user/" + currentUser, mySpaceLabel, PortalConfig.USER_TYPE));
    }
    
    // Get group wiki
    spaceBeans.addAll(wikiService.searchSpaces(""));
    return spaceBeans;
  }

  protected String getRestUrl() {
    return Utils.getCurrentRestURL() + MY_SPACE_REST_URL;
  }
  
  protected String createSelectSpaceEvent(String spaceId) throws Exception {
    Parameter parameter = new Parameter(SPACE_ID_PARAMETER, spaceId);
    return event(SELECT_SPACE_ACTION, null, new Parameter[] {parameter});
  }
  
  public static class SelectSpaceActionListener extends EventListener<UIWikiSpaceSwitcher> {
    public void execute(Event<UIWikiSpaceSwitcher> event) throws Exception {
      WebuiRequestContext context = event.getRequestContext();
      UIWikiSpaceSwitcher spaceSwitcher = event.getSource();
      UIPortletApplication root = spaceSwitcher.getAncestorOfType(UIPortletApplication.class);
      EventUIComponent eventComponent = spaceSwitcher.getEventComponent();
      UIComponent uiComponent = null;
      if (eventComponent.getId() != null) {
        uiComponent = (UIComponent) root.findComponentById(eventComponent.getId());
      } else {
        uiComponent = root;
      }
      String eventName = eventComponent.getEventName();
      Event<UIComponent> xEvent = uiComponent.createEvent(eventName, Event.Phase.PROCESS, context);
      if (xEvent != null) {
        xEvent.broadcast();
      }
    }
  }
}
