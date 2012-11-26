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

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
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
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.service.PermissionEntry;
import org.exoplatform.wiki.service.PermissionType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.webui.UIWikiPermissionForm.Scope;
import org.exoplatform.wiki.webui.UIWikiPortlet.PopupLevel;

/**
 * Created by The eXo Platform SAS
 * Author : phongth
 *          phongth@exoplatform.com
 * Oct 11, 2012
 */
@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "app:/templates/wiki/webui/UIWikiPermalinkForm.gtmpl",
  events = {
    @EventConfig(listeners = UIWikiPermalinkForm.RestrictActionListener.class),
    @EventConfig(listeners = UIWikiPermalinkForm.MakePublicActionListener.class),
    @EventConfig(listeners = UIWikiPermalinkForm.ManagePermisisonsActionListener.class)
  }
)
public class UIWikiPermalinkForm extends UIForm implements UIPopupComponent {
  
  public static final String RESTRICT_ACTION = "Restrict";
  
  public static final String MAKE_PUBLIC_ACTION = "MakePublic";
  
  public static final String MANAGE_PERMISSION_ACTION = "ManagePermisisons";
  
  public UIWikiPermalinkForm() throws Exception {
    setActions(new String[] { RESTRICT_ACTION, MAKE_PUBLIC_ACTION, MANAGE_PERMISSION_ACTION });
  }
  
  @Override
  public void activate() throws Exception {
  }

  @Override
  public void deActivate() throws Exception {
  }
  
  protected boolean canModifyPagePermission() throws Exception {
    return Utils.canModifyPagePermission();
  }
  
  protected boolean isCurrentPagePublic() throws Exception {
    return Utils.isCurrentPagePublic();
  }
  
  protected boolean canPublicAndRetrictPage() throws Exception {
    return Utils.canPublicAndRetrictPage();
  }
  
  /**
   * Get the permalink of current wiki page <br>
   * 
   * <ul>With the current page param:</ul>
   *   <li>type = "group"</li>
   *   <li>owner = "spaces/test_space"</li>
   *   <li>pageId = "test_page"</li>
   * <br>
   *  
   * <ul>The permalink will be: </ul>
   * <li>http://int.exoplatform.org/portal/intranet/wiki/group/spaces/test_space/test_page</li>
   * <br>
   * 
   * @return The permalink of current wiki page
   * @throws Exception
   */
  protected static String getPermanlink() throws Exception {
    WikiPageParams params = Utils.getCurrentWikiPageParams();
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    
    // get wiki webapp name
    String wikiWebappUri = wikiService.getWikiWebappUri();
    
    // Create permalink
    StringBuilder sb = new StringBuilder(wikiWebappUri);
    sb.append("/");
    
    if (!params.getType().equalsIgnoreCase(WikiType.PORTAL.toString())) {
      sb.append(params.getType().toLowerCase());
      sb.append("/");
      sb.append(org.exoplatform.wiki.utils.Utils.validateWikiOwner(params.getType(), params.getOwner()));
      sb.append("/");
    }
    
    if (params.getPageId() != null) {
      sb.append(URLEncoder.encode(params.getPageId(), "UTF-8"));
    }
    
    return getDomainUrl() + fillPortalName(sb.toString());
  }
  
  private static String getDomainUrl() {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    StringBuilder domainUrl = new StringBuilder();
    domainUrl.append(portalRequestContext.getRequest().getScheme());
    domainUrl.append("://");
    
    domainUrl.append(portalRequestContext.getRequest().getLocalName());
    int port = portalRequestContext.getRequest().getLocalPort();
    if (port != 80) {
      domainUrl.append(":");
      domainUrl.append(port);
    }
    return domainUrl.toString();
  }
  
  private static String fillPortalName(String url) {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL =  ctx.createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.PORTAL, Util.getPortalRequestContext().getPortalOwner(), url);
    return nodeURL.setResource(resource).toString(); 
  }
  
  public static class RestrictActionListener extends EventListener<UIWikiPermalinkForm> {
    @Override
    public void execute(Event<UIWikiPermalinkForm> event) throws Exception {
      UIWikiPermalinkForm uiWikiPermalinkForm = event.getSource();
      if (uiWikiPermalinkForm.canPublicAndRetrictPage()) {
        Page currentPage = Utils.getCurrentWikiPage();
        HashMap<String, String[]> permissions = currentPage.getPermission();
        permissions.remove(IdentityConstants.ANY);
        currentPage.setPermission(permissions);
        
        UIWikiPortlet uiWikiPortlet = uiWikiPermalinkForm.getAncestorOfType(UIWikiPortlet.class);
        if (currentPage.hasPermission(PermissionType.VIEWPAGE)) {
          UIWikiPageInfoArea uiWikiPageInfoArea = uiWikiPortlet.findFirstComponentOfType(UIWikiPageInfoArea.class);
          UIWikiPageControlArea uiWikiPageControlArea = uiWikiPortlet.findFirstComponentOfType(UIWikiPageControlArea.class);
          event.getRequestContext().addUIComponentToUpdateByAjax(uiWikiPermalinkForm);
          event.getRequestContext().addUIComponentToUpdateByAjax(uiWikiPageInfoArea);
          event.getRequestContext().addUIComponentToUpdateByAjax(uiWikiPageControlArea);
        } else {
          uiWikiPortlet.changeMode(WikiMode.PAGE_NOT_FOUND);
          event.getRequestContext().getJavascriptManager().addCustomizedOnLoadScript("eXo.wiki.UIWikiPageNotFound.hidePopup();");
          Utils.ajaxRedirect(event, Utils.getCurrentWikiPageParams(), WikiMode.PAGE_NOT_FOUND, null);
        }
      }
    }
  }

  public static class MakePublicActionListener extends EventListener<UIWikiPermalinkForm> {
    @Override
    public void execute(Event<UIWikiPermalinkForm> event) throws Exception {
      UIWikiPermalinkForm uiWikiPermalinkForm = event.getSource();
      if (uiWikiPermalinkForm.canPublicAndRetrictPage()) {
        Page currentPage = Utils.getCurrentWikiPage();
        HashMap<String, String[]> permissions = currentPage.getPermission();
        if (permissions.get(IdentityConstants.ANY) == null) {
          permissions.put(IdentityConstants.ANY, new String[] {
              org.exoplatform.services.jcr.access.PermissionType.READ, 
              org.exoplatform.services.jcr.access.PermissionType.ADD_NODE,
              org.exoplatform.services.jcr.access.PermissionType.REMOVE,
              org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY});
        }
        currentPage.setPermission(permissions);
        
        UIWikiPortlet uiWikiPortlet = uiWikiPermalinkForm.getAncestorOfType(UIWikiPortlet.class);
        UIWikiPageInfoArea uiWikiPageInfoArea = uiWikiPortlet.findFirstComponentOfType(UIWikiPageInfoArea.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiWikiPermalinkForm);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiWikiPageInfoArea);
      }
    }
  }
  
  public static class ManagePermisisonsActionListener extends EventListener<UIWikiPermalinkForm> {
    @Override
    public void execute(Event<UIWikiPermalinkForm> event) throws Exception {
      UIWikiPermalinkForm uiWikiPermalinkForm = event.getSource();
      UIWikiPortlet uiWikiPortlet = uiWikiPermalinkForm.getAncestorOfType(UIWikiPortlet.class);
      
      if (uiWikiPermalinkForm.canModifyPagePermission()) {
        UIPopupContainer uiPopupContainer = uiWikiPortlet.getPopupContainer(PopupLevel.L1);
        uiPopupContainer.cancelPopupAction();
        
        UIWikiPermissionForm uiWikiPermissionForm = uiPopupContainer.createUIComponent(UIWikiPermissionForm.class, null, "UIWikiPagePermissionForm");
        uiPopupContainer.activate(uiWikiPermissionForm, 800, 0);
        uiWikiPermissionForm.setPopupLevel(PopupLevel.L1);
        uiWikiPermissionForm.setScope(Scope.PAGE);
        PageImpl page = (PageImpl) Utils.getCurrentWikiPage();
        HashMap<String, String[]> permissionMap = page.getPermission();
        List<PermissionEntry> permissionEntries = uiWikiPermissionForm.convertToPermissionEntryList(permissionMap);
        uiWikiPermissionForm.setPermission(permissionEntries);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
      }
    }
  }
}
