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

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.service.BreadcrumbData;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.webui.tree.EventUIComponent;
import org.exoplatform.wiki.webui.tree.EventUIComponent.EVENTTYPE;

/**
 * Created by The eXo Platform SAS
 * Author : viet nguyen
 *          viet.nguyen@exoplatform.com
 * Apr 26, 2010  
 */
@ComponentConfig(
  lifecycle = Lifecycle.class,
  template = "app:/templates/wiki/webui/UIWikiBreadCrumb.gtmpl",
  events = {@EventConfig(listeners = UIWikiBreadCrumb.SwitchSpaceActionListener.class)}
)
public class UIWikiBreadCrumb extends UIContainer {
  public static final String  SPACE_SWITCHER = "UIWikiSpaceSwitcher_BreadCrumb";
  
  private static final String  SWITCH_SPACE_ACTION = "SwitchSpace";
  
  private static final String  BREAD_CRUMB_CONTAINER = "UIWikiBreadCrumb";

  private List<BreadcrumbData> breadCumbs = new ArrayList<BreadcrumbData>();

  private String               actionLabel;

  private boolean              isLink     = true;
  
  private boolean              isAllowChooseSpace = false;
  
  private static final Log     log               = ExoLogger.getLogger(UIWikiBreadCrumb.class);
  
  public UIWikiBreadCrumb() throws Exception {
    UIWikiSpaceSwitcher uiWikiSpaceSwitcher = addChild(UIWikiSpaceSwitcher.class, null, SPACE_SWITCHER);
    EventUIComponent eventComponent = new EventUIComponent(BREAD_CRUMB_CONTAINER, SWITCH_SPACE_ACTION, EVENTTYPE.EVENT);
    uiWikiSpaceSwitcher.init(eventComponent);
  }

  private boolean              isShowWikiType = true;
  
  private boolean              isDisplayFullSpaceName = true;

  public List<BreadcrumbData> getBreadCumbs() {
    return breadCumbs;
  }

  public void setBreadCumbs(List<BreadcrumbData> breadCumbs) {
    this.breadCumbs = breadCumbs;
  }
  
  public String getActionLabel() {
    return actionLabel;
  }

  public void setActionLabel(String actionLabel) {
    this.actionLabel = actionLabel;
  }  

  public String getParentURL() throws Exception {
    if(breadCumbs.size() > 1) {
      return createActionLink(breadCumbs.get(breadCumbs.size() - 2)) ;
    }else {
      return createActionLink(breadCumbs.get(0)) ;
    }     
  }
  
  public boolean isLink() {
    return isLink;
  }

  public void setLink(boolean isLink) {
    this.isLink = isLink;
  }
  
  public boolean isAllowChooseSpace() {
    return isAllowChooseSpace;
  }
  
  public void setAllowChooseSpace(boolean isAlowChooseSpace) {
    this.isAllowChooseSpace = isAlowChooseSpace;
  }

  public WikiPageParams getPageParam() throws Exception {
    if (this.breadCumbs != null && this.breadCumbs.size() > 0) {
      WikiService wservice = (WikiService) PortalContainer.getComponent(WikiService.class);
      return wservice.getWikiPageParams(breadCumbs.get(breadCumbs.size() - 1));
    }
    return null;
  }

  public String getWikiType() throws Exception {
    WikiPageParams params = getPageParam();
    if (params != null) {
      return params.getType();
    }
    return null;
  }

  public String getWikiName() throws Exception {
    if (getPageParam() != null) {
      String wikiName = getPageParam().getOwner();
      if (!isDisplayFullSpaceName && wikiName.indexOf('/') > -1) {
        wikiName = wikiName.substring(wikiName.lastIndexOf('/') + 1);
      }
      return wikiName;
    }
    return null;
  }

  public boolean isShowWikiType() {
    return isShowWikiType;
  }

  public void setShowWikiType(boolean isShowWikiType) {
    this.isShowWikiType = isShowWikiType;
  }
  
  public boolean isDisplayFullSpaceName() {
    return isDisplayFullSpaceName;
  }

  public void setDisplayFullSpaceName(boolean isDisplayFullSpaceName) {
    this.isDisplayFullSpaceName = isDisplayFullSpaceName;
  }

  public String createActionLink(BreadcrumbData breadCumbData) throws Exception {  
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    StringBuilder sb = new StringBuilder(portalRequestContext.getPortalURI());
    UIPortal uiPortal = Util.getUIPortal();
    String pageNodeSelected = uiPortal.getSelectedUserNode().getURI();
    sb.append(pageNodeSelected);
    sb.append("/");
    if (!PortalConfig.PORTAL_TYPE.equalsIgnoreCase(breadCumbData.getWikiType())) {
      sb.append(breadCumbData.getWikiType());
      sb.append("/");
      sb.append(Utils.validateWikiOwner(breadCumbData.getWikiType(), breadCumbData.getWikiOwner()));
      sb.append("/");
    }
    sb.append(breadCumbData.getId());
    return sb.toString();
  }
  
  public static class SwitchSpaceActionListener extends EventListener<UIWikiBreadCrumb> {
    public void execute(Event<UIWikiBreadCrumb> event) throws Exception {
      String wikiId = event.getRequestContext().getRequestParameter(UIWikiSpaceSwitcher.SPACE_ID_PARAMETER);
      UIWikiBreadCrumb uiWikiBreadCrumb = event.getSource();
      UIWikiSpaceSwitcher uiWikiSpaceSwitcher = uiWikiBreadCrumb.getChildById(SPACE_SWITCHER);
      Wiki wiki = uiWikiSpaceSwitcher.getWikiById(wikiId);
      if (wiki != null) {
        PageImpl wikiHome = (PageImpl) wiki.getWikiHome();
        org.exoplatform.wiki.commons.Utils.ajaxRedirect(event, Utils.getWikiPageParams(wikiHome), WikiMode.VIEW, null);
      } else {
        log.warn(String.format("Wrong wiki id: [%s], can not change space", wikiId));
      }
    }
  }
}
