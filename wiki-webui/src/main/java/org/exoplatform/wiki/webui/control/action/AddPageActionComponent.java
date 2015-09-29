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

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.service.WikiContext;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.webui.UIWikiPageEditForm;
import org.exoplatform.wiki.webui.UIWikiPageTitleControlArea;
import org.exoplatform.wiki.webui.UIWikiPortlet;
import org.exoplatform.wiki.webui.UIWikiRichTextArea;
import org.exoplatform.wiki.webui.WikiMode;
import org.exoplatform.wiki.webui.control.action.core.AbstractEventActionComponent;
import org.exoplatform.wiki.webui.control.filter.EditPagesPermissionFilter;
import org.exoplatform.wiki.webui.control.filter.IsViewModeFilter;
import org.exoplatform.wiki.webui.control.listener.AddContainerActionListener;

@ComponentConfig(
  template = "app:/templates/wiki/webui/control/action/AbstractActionComponent.gtmpl",
  events = {
     @EventConfig(listeners = AddPageActionComponent.AddPageActionListener.class)
  }
)
public class AddPageActionComponent extends AbstractEventActionComponent {  

  public static final String                   ACTION = "AddPage";
  
  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      new IsViewModeFilter(), new EditPagesPermissionFilter() });

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
  
  public static class AddPageActionListener extends AddContainerActionListener<AddPageActionComponent> {
    @Override
    protected void processEvent(Event<AddPageActionComponent> event) throws Exception {
      WikiService wservice = (WikiService) PortalContainer.getComponent(WikiService.class);
      HttpSession session = Util.getPortalRequestContext().getRequest().getSession(false);
      String draftId = (String) session.getAttribute(Utils.getDraftIdSessionKey());
      
      DraftPage draftPage = draftId == null ? null : wservice.getDraft(draftId);
 
      UIWikiPortlet wikiPortlet = event.getSource().getAncestorOfType(UIWikiPortlet.class);
      wikiPortlet.changeMode(WikiMode.ADDPAGE);
      WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
      String pageTitle = draftPage == null ? pageParams.getParameter(WikiContext.PAGETITLE) : draftPage.getTitle();
      UIWikiPageEditForm pageEditForm = wikiPortlet.findFirstComponentOfType(UIWikiPageEditForm.class);
      UIFormStringInput titleInput = pageEditForm.getChild(UIWikiPageTitleControlArea.class).getUIStringInput();
      UIFormTextAreaInput markupInput = pageEditForm.findComponentById(UIWikiPageEditForm.FIELD_CONTENT);
      UIFormStringInput commentInput = pageEditForm.findComponentById(UIWikiPageEditForm.FIELD_COMMENT);
      titleInput.setValue("");

      titleInput.setEditable(true);
      markupInput.setValue(draftPage == null ? "" : draftPage.getContent().getText());
      commentInput.setRendered(false);

      if (pageTitle != null && pageTitle.length() > 0) {
        titleInput.setValue(pageTitle);
        titleInput.setEditable(false);
      }

      pageEditForm.setInitDraftName(draftPage == null ? StringUtils.EMPTY : draftPage.getName());
      UIWikiRichTextArea wikiRichTextArea = pageEditForm.getChild(UIWikiRichTextArea.class);
      if (wikiRichTextArea.isRendered()) {
        Utils.feedDataForWYSIWYGEditor(pageEditForm, null);
      }

      session.setAttribute(Utils.getDraftIdSessionKey(), null);
      super.processEvent(event);
    }
  }
}
