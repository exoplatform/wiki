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

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
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
import org.exoplatform.wiki.mow.api.Permission;
import org.exoplatform.wiki.mow.api.PermissionEntry;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.service.IDType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.webui.UIWikiPermissionForm.Scope;
import org.exoplatform.wiki.webui.UIWikiPortlet.PopupLevel;

import java.util.List;

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

  private static WikiService wikiService;
  
  public UIWikiPermalinkForm() throws Exception {
    setActions(new String[] { RESTRICT_ACTION, MAKE_PUBLIC_ACTION, MANAGE_PERMISSION_ACTION });

    wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
  }
  
  @Override
  public void activate() {
  }

  @Override
  public void deActivate() {
  }
  
  protected boolean canModifyPagePermission() throws Exception {
    return Utils.canModifyPagePermission();
  }
  
  protected boolean isCurrentPagePublic() throws Exception {
    return Utils.isCurrentPagePublic();
  }
  
  protected boolean canPublicAndRetrictPage() throws Exception {
    return wikiService.canPublicAndRetrictPage(Utils.getCurrentWikiPage(), org.exoplatform.wiki.utils.Utils.getCurrentUser());
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
    return org.exoplatform.wiki.utils.Utils.getPermanlink(params, true);
  }
  
  public static class RestrictActionListener extends EventListener<UIWikiPermalinkForm> {
    @Override
    public void execute(Event<UIWikiPermalinkForm> event) throws Exception {
      UIWikiPermalinkForm uiWikiPermalinkForm = event.getSource();
      if (uiWikiPermalinkForm.canPublicAndRetrictPage()) {
        Page currentPage = Utils.getCurrentWikiPage();
        List<PermissionEntry> permissions = currentPage.getPermissions();
        for(int i = 0; i < permissions.size(); i++) {
          PermissionEntry permissionEntry = permissions.get(i);
          if(permissionEntry.getId().equals(IdentityConstants.ANY.toString())) {
            permissions.remove(i);
            break;
          }
        }
        currentPage.setPermissions(permissions);

        wikiService.updatePage(currentPage, null);
        
        UIWikiPortlet uiWikiPortlet = uiWikiPermalinkForm.getAncestorOfType(UIWikiPortlet.class);
        if (wikiService.hasPermissionOnPage(currentPage, PermissionType.VIEWPAGE, ConversationState.getCurrent().getIdentity())) {
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
        List<PermissionEntry> permissions = currentPage.getPermissions();
        permissions.add(new PermissionEntry(IdentityConstants.ANY, "", IDType.MEMBERSHIP, new Permission[]{
                new Permission(PermissionType.VIEWPAGE, true),
                new Permission(PermissionType.EDITPAGE, true),
                new Permission(PermissionType.ADMINPAGE, true)
        }));
        currentPage.setPermissions(permissions);

        wikiService.updatePage(currentPage, null);
        
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
        Page page = Utils.getCurrentWikiPage();
        uiWikiPermissionForm.setPermission(page.getPermissions());
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
      }
    }
  }
}
