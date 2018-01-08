/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

import java.util.HashMap;
import java.util.ResourceBundle;

import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.wiki.WikiPortletPreference;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.commons.WikiConstants;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.service.WikiContext;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.webui.UIWikiPermissionForm.Scope;
import org.exoplatform.wiki.webui.control.UIAttachmentContainer;
import org.exoplatform.wiki.webui.control.action.AddPageActionComponent;

@ComponentConfig(
  lifecycle = UIApplicationLifecycle.class,
  template = "app:/templates/wiki/webui/UIWikiPortlet.gtmpl",
  events = {
    @EventConfig(listeners = UIWikiPortlet.ViewPageActionListener.class),
    @EventConfig(listeners = UIWikiPortlet.ChangeModeActionListener.class),
    @EventConfig(listeners = UIWikiPortlet.RedirectActionListener.class)
  }
)
public class UIWikiPortlet extends UIPortletApplication {
  
  private WikiMode              mode                       = WikiMode.VIEW;

  private EditMode              editmode                   = EditMode.ALL;
  
  private EditorMode editorMode_ = EditorMode.RICHTEXT;

  private String                sectionIndex               = "";

  private WikiMode              previousMode;

  private WikiPortletPreference portletPreferences         = new WikiPortletPreference();

  public static String          VIEW_PAGE_ACTION           = "ViewPage";

  public static String          CHANGE_MODE_ACTION         = "ChangeMode";

  public static String          REDIRECT_ACTION            = "Redirect";

  public static String          WIKI_PORTLET_ACTION_PREFIX = "UIWikiPortlet_";

  private String                redirectURL                = "";
  
  private ResourceBundle resourceBundle;

  private WikiService wikiService;

  private PortletMode portletMode;

  public static enum PopupLevel {
    L1,
    L2
  }

  
  public UIWikiPortlet() throws Exception {
    super();
    try {
      wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
      addChild(UIWikiEmptyAjaxBlock.class, null, null);
      addChild(UIWikiPortletPreferences.class, null, null);
      addChild(UIWikiUpperArea.class, null, null);
      addChild(UIWikiMiddleArea.class, null, null);
      addChild(UIWikiMaskWorkspace.class, null, "UIWikiMaskWorkspace");
      UIPopupContainer uiPopupContainer = addChild(UIPopupContainer.class, null, "UIWikiPopupContainer" + PopupLevel.L1);
      uiPopupContainer.getChild(UIPopupWindow.class).setId("UIWikiPopupWindow" + PopupLevel.L1);
      uiPopupContainer = uiPopupContainer.addChild(UIPopupContainer.class, null, "UIWikiPopupContainer" + PopupLevel.L2);
      uiPopupContainer.getChild(UIPopupWindow.class).setId("UIWikiPopupWindow" + PopupLevel.L2);
      loadPreferences();
    } catch (Exception e) {
      log.error("An exception happens when init WikiPortlet", e);
    }
  }

  public WikiPortletPreference getPortletPreferences() {
    return portletPreferences;
  }

  public void setPortletPreferences(WikiPortletPreference portletPreferences) {
    this.portletPreferences = portletPreferences;
  }
  
  public ResourceBundle getResourceBundle() {
    if (resourceBundle == null) {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      resourceBundle = context.getApplicationResourceBundle();
    }
    return resourceBundle;
  }

  public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
    PortletRequestContext portletReqContext = (PortletRequestContext) context;
    redirectURL = this.url(this.REDIRECT_ACTION);
    loadPreferences();
    portletMode = portletReqContext.getApplicationMode();
    if (portletMode == PortletMode.VIEW) {
      if (mode.equals(WikiMode.PORTLETPREFERENCES)) {        
        changeMode(WikiMode.VIEW);
      }
      getChild(UIWikiUpperArea.class).getChild(UIWikiApplicationControlArea.class)
                                     .getChild(UIWikiBreadCrumb.class)
                                     .setRendered(portletPreferences.isShowBreadcrumb());     
      String requestURL = Utils.getCurrentRequestURL();

      WikiPageParams wikiPageParams = Utils.getCurrentWikiPageParams();
      Wiki wiki = Utils.getCurrentWiki();
      if(wiki == null) {
        wiki = wikiService.createWiki(wikiPageParams.getType(), wikiPageParams.getOwner());
      }

      if (!context.useAjax()) {
        UIWikiPermissionForm wikiPermissionForm = this.findComponentById("UIWikiPagePermissionForm");
        if (wikiPermissionForm != null) {
          this.getPopupContainer(wikiPermissionForm.getPopupLevel()).deActivate();
        }
      }

      Identity currentIdentity = ConversationState.getCurrent().getIdentity();

      if(!wikiService.hasPermissionOnWiki(wiki, PermissionType.VIEWPAGE, currentIdentity)) {
        changeMode(WikiMode.PAGE_NOT_FOUND);
        super.processRender(app, context);
        return;
      }

      Page page = Utils.getCurrentWikiPage();

      if (page == null) {
        changeMode(WikiMode.PAGE_NOT_FOUND);
        super.processRender(app, context);
        return;
      } else {
        if (mode.equals(WikiMode.PAGE_NOT_FOUND)) {
          changeMode(WikiMode.VIEW);
        }

        if((WikiMode.EDITPAGE.equals(this.getWikiMode()) || WikiMode.ADDPAGE.equals(this.getWikiMode()))
                && !wikiService.hasPermissionOnPage(page, PermissionType.EDITPAGE, currentIdentity)) {
          changeMode(WikiMode.VIEW);
        }
      }
      
      // Check if page url is null then create url for it
      if (StringUtils.isEmpty(page.getUrl())) {
        page.setUrl(Utils.getURLFromParams(new WikiPageParams(wiki.getType(), wiki.getOwner(), page.getName())));
      }
      
      if (WikiContext.ADDPAGE.equalsIgnoreCase(wikiPageParams.getParameter(WikiContext.ACTION))) {
        AddPageActionComponent addPageComponent = this.findFirstComponentOfType(AddPageActionComponent.class);
        if (addPageComponent != null) {
          Event<UIComponent> xEvent = addPageComponent.createEvent(AddPageActionComponent.ACTION, Event.Phase.PROCESS, context);
          if (xEvent != null) {
            xEvent.broadcast();
          }
        }
      }else if (org.exoplatform.wiki.utils.Utils.COMPARE_REVISION.equalsIgnoreCase(wikiPageParams.getParameter(WikiContext.ACTION))) {
        //UIWikiPageInfoArea.COMPARE_REVISION
        UIWikiPageInfoArea pageInfoArea = this.findFirstComponentOfType(UIWikiPageInfoArea.class);
        if (pageInfoArea != null) {
          Event<UIComponent> xEvent = pageInfoArea.createEvent(org.exoplatform.wiki.utils.Utils.COMPARE_REVISION, Event.Phase.PROCESS, context);
          if (xEvent != null) {
            xEvent.broadcast();
          }
        }
      }
      try {
        // TODO: ignore request URL of resources
        context.setAttribute("wikiPage", page);
        ((UIWikiPageTitleControlArea) findComponentById(UIWikiPageControlArea.TITLE_CONTROL)).getUIFormInputInfo()
                                                                                             .setValue(HTMLSanitizer.sanitize(page.getTitle()));      
      } catch (Exception e) {
        context.setAttribute("wikiPage", null);
        UIWikiPageContentArea wikiPageContentArea = findFirstComponentOfType(UIWikiPageContentArea.class);
        if (wikiPageContentArea != null) {
          UIWikiContentDisplay contentDisplay = wikiPageContentArea.getChildById(UIWikiPageContentArea.VIEW_DISPLAY);
          contentDisplay.setHtmlOutput(("Exceptions occur when rendering content!"));
        }
        if (log.isWarnEnabled()) {
          log.warn("An exception happens when resolving URL: " + requestURL, e);
        }
      }

      // Check to close wikiPagePermissionForm
      if (!WikiMode.VIEW.equals(this.getWikiMode())) {
        UIWikiPermissionForm wikiPermissionForm = this.findComponentById("UIWikiPagePermissionForm");
        if (wikiPermissionForm != null && Scope.PAGE.equals(wikiPermissionForm.getScope())) {
          this.getPopupContainer(wikiPermissionForm.getPopupLevel()).deActivate();
        }
      }
      
      super.processRender(app, context);

      if (getWikiMode() == WikiMode.HELP) {
        changeMode(previousMode);
      }
    } else if (portletMode == PortletMode.EDIT) {
      changeMode(WikiMode.PORTLETPREFERENCES);
      super.processRender(app, context);
    } else {
      super.processRender(app, context);
    }
  }
  
  public UIPopupContainer getPopupContainer(PopupLevel level) {
    UIPopupContainer popupContainer = getChildById("UIWikiPopupContainer" + PopupLevel.L1);
    if (level == PopupLevel.L2) {
      popupContainer = popupContainer.getChildById("UIWikiPopupContainer" + PopupLevel.L2);
    }
    return popupContainer;
  }
  
  public WikiMode getWikiMode() {
    return mode;
  }
  
  public EditMode getEditMode() {
    return editmode;
  }
  
  /**
   * gets the mode of wiki editor
   * @return the editor mode
   */
  public EditorMode getEditorMode() {
    return editorMode_;
  }
  
  public String getSectionIndex() {
    return sectionIndex;
  }

  public void setSectionIndex(String sectionIndex) {
    this.sectionIndex = sectionIndex;
  }

  public String getRedirectURL() {
    return redirectURL;
  }

  public void setRedirectURL(String redirectURL) {
    this.redirectURL = redirectURL;
  }

  public void changeMode(WikiMode newMode) {
    if (newMode== WikiMode.HELP)
        this.previousMode = mode;
    if (newMode.equals(WikiMode.VIEW)) {
      findFirstComponentOfType(UIWikiPageTitleControlArea.class).toInfoMode();
      UIWikiBottomArea bottomArea = findFirstComponentOfType(UIWikiBottomArea.class).setRendered(true);
      bottomArea.getChild(UIAttachmentContainer.class).setRendered(false);
      bottomArea.getChild(UIWikiPageVersionsList.class).setRendered(false);
    }
    if (newMode.equals(WikiMode.EDITPAGE)||newMode.equals(WikiMode.ADDPAGE)) {
      findFirstComponentOfType(UIAttachmentContainer.class).setRendered(true);
      
      findFirstComponentOfType(UIWikiSidePanelArea.class).setRendered(EditorMode.SOURCE.equals(editorMode_));
      findFirstComponentOfType(UIWikiBottomArea.class).setRendered(EditorMode.SOURCE.equals(editorMode_));
      findFirstComponentOfType(UIWikiPageEditForm.class).getUIFormTextAreaInput(UIWikiPageEditForm.FIELD_CONTENT)
                                                        .setRendered(EditorMode.SOURCE.equals(editorMode_));
      findFirstComponentOfType(UIWikiRichTextArea.class).setRendered(EditorMode.RICHTEXT.equals(editorMode_));
    }
    mode = newMode;
  }
  
  public void changeEditMode(EditMode newEditMode) {
    editmode = newEditMode;
  }
  
  /**
   * changes the mode of wiki editor
   * @param newMode the new mode
   */
  public void changeEditorMode(EditorMode newMode) {
    editorMode_ = newMode;
  }

  private void loadPreferences() {
    PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    try {
      portletPreferences.setShowBreadcrumb(Boolean.parseBoolean(portletPref.getValue(WikiPortletPreference.SHOW_BREADCRUMB, "true")));
      portletPreferences.setShowNavigationTree(Boolean.parseBoolean(portletPref.getValue(WikiPortletPreference.SHOW_NAVIGATIONTREE, "true")));
    } catch (Exception e) {
      log.error("Fail to load wiki portlet's preference: ", e);
    }
  }
  
  public HashMap<String, Object> getUIExtContext() throws Exception {
    HashMap<String, Object> context = new HashMap<String, Object>();
    WikiPageParams params = Utils.getCurrentWikiPageParams();
    context.put(WikiConstants.WIKI_MODE, this.mode);
    context.put(WikiConstants.CURRENT_PAGE, params.getPageName());
    context.put(WikiConstants.CURRENT_WIKI_OWNER, params.getOwner());
    context.put(WikiConstants.CURRENT_WIKI_TYPE, params.getType());
    UIWikiPageArea wikiPageArea = this.findFirstComponentOfType(UIWikiPageArea.class);
    UIWikiPageEditForm wikiPageEditForm = wikiPageArea.findFirstComponentOfType(UIWikiPageEditForm.class);
    UIWikiRichTextArea wikiRichTextArea = wikiPageEditForm.findFirstComponentOfType(UIWikiRichTextArea.class);
    context.put(WikiConstants.IS_MARKUP, Boolean.valueOf(!wikiRichTextArea.isRendered()));
    return context;
  }
  
  protected boolean isKeepSessionAlive() {
    return (this.mode == WikiMode.EDITPAGE) || (this.mode == WikiMode.EDITTEMPLATE) 
      || (this.mode == WikiMode.ADDPAGE) || (this.mode == WikiMode.ADDTEMPLATE);
  }
 
  public static class ViewPageActionListener extends EventListener<UIWikiPortlet> {
    @Override
    public void execute(Event<UIWikiPortlet> event) throws Exception {
      UIWikiPortlet wikiPortlet = event.getSource();
      WikiMode currentMode = wikiPortlet.getWikiMode();
      if (currentMode.equals(WikiMode.VIEW)) {
        event.getRequestContext()
             .addUIComponentToUpdateByAjax(wikiPortlet.findFirstComponentOfType(UIWikiEmptyAjaxBlock.class));
      } else {
        event.getSource().changeMode(WikiMode.VIEW);
      }

    }
  }
  
  public static class ChangeModeActionListener extends EventListener<UIWikiPortlet> {
    @Override
    public void execute(Event<UIWikiPortlet> event) throws Exception {
      UIWikiPortlet wikiPortlet = event.getSource();
      String mode = event.getRequestContext().getRequestParameter("mode");
      String currentModeName = (mode.equals("")) ? WikiMode.VIEW.toString() : mode;
      WikiMode currentMode = Utils.getModeFromAction(currentModeName);
      if (!wikiPortlet.mode.equals(currentMode)) {
        if (currentMode == null)
          currentMode = WikiMode.VIEW;
        event.getSource().changeMode(currentMode);
      }
      event.getRequestContext()
           .addUIComponentToUpdateByAjax(wikiPortlet.findFirstComponentOfType(UIWikiEmptyAjaxBlock.class));
    }
  }
  
  public static class RedirectActionListener extends EventListener<UIWikiPortlet> {
    @Override
    public void execute(Event<UIWikiPortlet> event) throws Exception {      
      String value = event.getRequestContext().getRequestParameter(OBJECTID);
      value = TitleResolver.getId(value, false);
      WikiPageParams params = TreeUtils.getPageParamsFromPath(value);
      Utils.redirect(params, WikiMode.VIEW);
    }
  }
  
}
