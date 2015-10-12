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

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.webui.UIWikiPortlet.PopupLevel;

import java.util.ArrayList;
import java.util.List;

@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "app:/templates/wiki/webui/UIWikiDeletePageConfirm.gtmpl",
  events = {
      @EventConfig(listeners = UIWikiDeletePageConfirm.OKActionListener.class),
      @EventConfig(listeners = UIWikiDeletePageConfirm.CancelActionListener.class)
    }
)
public class UIWikiDeletePageConfirm extends UIForm implements UIPopupComponent {
  public static final String OK_ACTION = "OK";
  public static final String CANCEL_ACTION = "Cancel";
  
  private static final String BREADCRUM_NAME = "uiWikiDeletePageConfirm_UIWikiBreadcrum"; 
  
  private String pageID;
  private String owner;

  public UIWikiDeletePageConfirm() throws Exception {
    addChild(UIWikiBreadCrumb.class, null, BREADCRUM_NAME).setLink(false).setShowWikiType(false);
  }

  protected List<SearchResult> getRelativePages() {
    try {
      WikiPageParams params = Utils.getCurrentWikiPageParams();
      WikiService wservice = (WikiService) PortalContainer.getComponent(WikiService.class);
      return wservice.searchRenamedPage(params.getType(), params.getOwner(), params.getPageName());
    } catch (Exception e) {
      return new ArrayList<SearchResult>();
    }
  }
  
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    UIWikiBreadCrumb breadCrumb = getChild(UIWikiBreadCrumb.class);
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    Page page = Utils.getCurrentWikiPage();
    Page parent = wikiService.getParentPageOf(page);
    WikiPageParams params = Utils.getCurrentWikiPageParams();
    if (params != null) {
      breadCrumb.setBreadCumbs(wikiService.getBreadcumb(params.getType(), params.getOwner(), parent.getName()));
    }
    super.processRender(context);
  }

  protected Page getCurrentPage() {
    try {
      WikiPageParams params = Utils.getCurrentWikiPageParams();
      pageID = params.getPageName();
      owner = params.getOwner();
      WikiService wservice = (WikiService) PortalContainer.getComponent(WikiService.class);
      return wservice.getPageOfWikiByName(params.getType(), params.getOwner(), params.getPageName());
    } catch (Exception e) {
      return null;
    }
  }

  protected String getCurrentPageId() {
    return pageID;
  }

  protected String getWiki() {
    return owner;
  }

  protected String getHomeURL() {
    return Util.getPortalRequestContext().getPortalURI() + "wiki";
  }
  
  protected void cancelPopup(Event<UIWikiDeletePageConfirm> event) throws Exception {
    UIWikiPortlet wikiPortlet = getAncestorOfType(UIWikiPortlet.class);
    UIPopupContainer popupContainer = wikiPortlet.getPopupContainer(PopupLevel.L1);
    popupContainer.cancelPopupAction();
    event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
  }
  
  static public class OKActionListener extends EventListener<UIWikiDeletePageConfirm> {
    public void execute(Event<UIWikiDeletePageConfirm> event) throws Exception {
      UIWikiDeletePageConfirm uiWikiDeletePageConfirm = event.getSource();
      WikiService wService = (WikiService) PortalContainer.getComponent(WikiService.class);
      WikiPageParams params = Utils.getCurrentWikiPageParams();
      wService.removeDraftOfPage(params);
      wService.deletePage(params.getType(), params.getOwner(), params.getPageName());
      UIWikiPortlet wikiPortlet = uiWikiDeletePageConfirm.getAncestorOfType(UIWikiPortlet.class);
      wikiPortlet.changeMode(WikiMode.VIEW);
      UIWikiBreadCrumb breadcumb = wikiPortlet.findFirstComponentOfType(UIWikiBreadCrumb.class);
      String parentURL = breadcumb.getParentURL();
      uiWikiDeletePageConfirm.cancelPopup(event);
      Utils.ajaxRedirect(event, parentURL);
    }
  }

  static public class CancelActionListener extends EventListener<UIWikiDeletePageConfirm> {
    public void execute(Event<UIWikiDeletePageConfirm> event) throws Exception {
      UIWikiDeletePageConfirm uiWikiDeletePageConfirm = event.getSource();
      uiWikiDeletePageConfirm.cancelPopup(event);
    }
  }

  @Override
  public void activate() {
  }

  @Override
  public void deActivate() {
  }
}
