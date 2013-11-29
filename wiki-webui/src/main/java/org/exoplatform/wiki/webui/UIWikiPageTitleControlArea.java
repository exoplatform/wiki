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
import java.util.List;
import java.util.ResourceBundle;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.WikiNodeType;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.webui.control.UIWikiExtensionContainer;

@ComponentConfig(
  lifecycle = Lifecycle.class,
  template = "app:/templates/wiki/webui/UIWikiPageTitleControlArea.gtmpl"
)
public class UIWikiPageTitleControlArea extends UIWikiExtensionContainer {
  
  public static final String EXTENSION_TYPE = "org.exoplatform.wiki.webui.UIWikiPageTitleControlArea";

  public static final String FIELD_TITLEINFO  = "titleInfo";

  public static final String FIELD_TITLEINPUT = "titleInput";

  public static final String FIELD_EDITABLE   = "editable";

  public static final String CHANGE_TITLEMODE = "changeTitleMode";

  public UIWikiPageTitleControlArea() throws Exception {
    UIFormInputInfo titleInfo = new UIFormInputInfo(FIELD_TITLEINFO, FIELD_TITLEINFO, FIELD_TITLEINFO);
    titleInfo.setRendered(true);
    addChild(titleInfo);
    UIFormStringInput titleInput = new UIFormStringInput(FIELD_TITLEINPUT, FIELD_TITLEINPUT, FIELD_TITLEINPUT);
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    titleInput.setHTMLAttribute("title", res.getString("UIWikiPageTitleControlArea.label.title"));
    titleInput.setRendered(false);
    addChild(titleInput);
  }
  
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    if (!context.useAjax()) {
      UIFormInputInfo titleInfo = getChild(UIFormInputInfo.class);
      List<WikiMode> acceptEdiableModes = Arrays.asList(new WikiMode[] { WikiMode.VIEW, WikiMode.HELP, WikiMode.VIEWREVISION });
      WikiMode currentMode = getAncestorOfType(UIWikiPortlet.class).getWikiMode();
      if (acceptEdiableModes.contains(currentMode)) {
        titleInfo.setRendered(true);
      }

      UIFieldEditableForm fieldEditableForm = getChild(UIFieldEditableForm.class);
      if (fieldEditableForm != null) {
        fieldEditableForm.hideTitleInputBox();
      }
    }
    super.processRender(context);
  }

  @Override
  public String getExtensionType() {
    return EXTENSION_TYPE;
  }
  
  public UIFormInputInfo getUIFormInputInfo(){
    return findComponentById(FIELD_TITLEINFO);
  }
  
  public UIFormStringInput getUIStringInput(){
    return findComponentById(FIELD_TITLEINPUT);
  }
  
  public void toInfoMode(){
    findComponentById(FIELD_TITLEINFO).setRendered(true);
    findComponentById(FIELD_TITLEINPUT).setRendered(false);
  }
  
  public void toInputMode(){
    findComponentById(FIELD_TITLEINFO).setRendered(false);
    findComponentById(FIELD_TITLEINPUT).setRendered(true);
  }
  
  public boolean isInfoMode() {
    return getChildById(FIELD_TITLEINFO).isRendered();
  }
    
  public void saveTitle(String newTitle, Event event) throws Exception {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);    
    WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
    String newName = TitleResolver.getId(newTitle, true);
    Page page = Utils.getCurrentWikiPage();
    boolean isRenameHome = WikiNodeType.Definition.WIKI_HOME_NAME.equals(page.getName())
        && !newName.equals(pageParams.getPageId());
    page.setMinorEdit(false);
    if (isRenameHome) {
      page.setTitle(newTitle);
      
      // Post activity
      wikiService.postUpdatePage(pageParams.getType(), pageParams.getOwner(), page.getName(), page, PageWikiListener.EDIT_PAGE_TITLE_TYPE);
    } else {
      wikiService.renamePage(pageParams.getType(),
                             pageParams.getOwner(),
                             pageParams.getPageId(),
                             newName,
                             newTitle);
      
      // Post activity
      Page renamedPage = wikiService.getPageById(pageParams.getType(), pageParams.getOwner(), newName);
      wikiService.postUpdatePage(pageParams.getType(), pageParams.getOwner(), newName, renamedPage, PageWikiListener.EDIT_PAGE_TITLE_TYPE);
    }
    pageParams.setPageId(newName);
    Utils.redirect(pageParams, WikiMode.VIEW);
  }
  
  protected boolean isAddMode() {
    WikiMode currentMode = (WikiMode) this.getAncestorOfType(UIWikiPortlet.class).getWikiMode();
    return currentMode.equals(WikiMode.ADDPAGE);
  }
  
  public String getTitle() {
    return this.getChild(UIFormStringInput.class).getValue();
  }
}
