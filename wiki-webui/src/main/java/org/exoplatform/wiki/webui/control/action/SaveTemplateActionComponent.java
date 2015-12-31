/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.commons.WikiConstants;
import org.exoplatform.wiki.mow.api.Template;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.utils.WikiNameValidator;
import org.exoplatform.wiki.webui.*;
import org.exoplatform.wiki.webui.control.filter.IsEditAddTemplateModeFilter;
import org.exoplatform.wiki.webui.control.listener.UISubmitToolBarActionListener;
import org.exoplatform.wiki.webui.extension.UITemplateSettingForm;

import java.util.Arrays;
import java.util.List;

@ComponentConfig(
  template = "app:/templates/wiki/webui/control/action/SaveTemplateActionComponent.gtmpl",                   
  events = {
    @EventConfig(listeners = SaveTemplateActionComponent.SaveTemplateActionListener.class, phase = Phase.DECODE)
  }
)
public class SaveTemplateActionComponent extends UIComponent {

  public static final String                   ACTION   = "SaveTemplate";
  
  private static final Log log = ExoLogger.getLogger("wiki:SaveTemplateActionComponent");
  
  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] { new IsEditAddTemplateModeFilter() });

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }
  
  protected boolean isNewMode() {
    return (WikiMode.ADDPAGE.equals(getAncestorOfType(UIWikiPortlet.class).getWikiMode()));
  }  

  protected String getActionLink() throws Exception {
    return Utils.createFormActionLink(this, ACTION, ACTION);
  }
  
  protected String getPageTitleInputId() {
    return UIWikiPageTitleControlArea.FIELD_TITLEINPUT;
  }
  
  public static class SaveTemplateActionListener extends
                                                UISubmitToolBarActionListener<SaveTemplateActionComponent> {
    @Override
    protected void processEvent(Event<SaveTemplateActionComponent> event) throws Exception {
      boolean isError = false;
      ApplicationMessage appMsg = null;
      WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
      UIWikiPortlet wikiPortlet = event.getSource().getAncestorOfType(UIWikiPortlet.class);
      WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
      UIWikiPageEditForm pageEditForm = wikiPortlet.findFirstComponentOfType(UIWikiPageEditForm.class);
      UIFormStringInput titleInput = pageEditForm.getChild(UIWikiPageTitleControlArea.class)
                                                 .getUIStringInput();
      UIFormStringInput descriptionInput = pageEditForm.findComponentById(UIWikiTemplateDescriptionContainer.FIELD_DESCRIPTION);
      UIFormTextAreaInput markupInput = pageEditForm.findComponentById(UIWikiPageEditForm.FIELD_CONTENT);
      if (titleInput.getValue() == null || titleInput.getValue().trim().length() == 0) {
        isError = true;
        appMsg = new ApplicationMessage("WikiPageNameValidator.msg.EmptyTitle",
                  null,
                  ApplicationMessage.WARNING);
      } else if (titleInput.getValue().trim().length() > WikiConstants.MAX_LENGTH_TITLE) {
      	isError = true;
      	appMsg = new ApplicationMessage("WikiPageNameValidator.msg.TooLongTitle", new Object[] {WikiConstants.MAX_LENGTH_TITLE} , ApplicationMessage.WARNING);
      }
      try {
        WikiNameValidator.validate(titleInput.getValue());
      } catch (IllegalArgumentException ex) {
        isError = true;
        Object[] arg = { ex.getMessage() };
        appMsg = new ApplicationMessage("WikiPageNameValidator.msg.Invalid-char",
                                            arg,
                                            ApplicationMessage.WARNING);
      }
      if (isError) {
        event.getRequestContext().getUIApplication().addMessage(appMsg);
        event.getRequestContext().setProcessRender(true);
      }
      if (event.getRequestContext().getProcessRender()) {
        Utils.redirect(pageParams, wikiPortlet.getWikiMode());
        return;
      }
      String title = titleInput.getValue().trim();
      String markup = (markupInput.getValue() == null) ? "" : markupInput.getValue();
      markup = markup.trim();
      String description = descriptionInput.getValue();
      String syntaxId = Utils.getDefaultSyntax();
      String[] msgArg = { title };
      boolean isExist = false; 
      try {
        String idTemp = TitleResolver.getId(title, false);
        if (wikiPortlet.getWikiMode() == WikiMode.ADDTEMPLATE
            || (wikiPortlet.getWikiMode() == WikiMode.EDITTEMPLATE && !idTemp.equals(pageEditForm.getTemplateId()))) {
          isExist = (wikiService.getTemplatePage(pageParams, idTemp) != null);
          if (isExist) {
            event.getRequestContext()
                 .getUIApplication()
                 .addMessage(new ApplicationMessage("SavePageAction.msg.warning-page-title-already-exist",
                                                    null,
                                                    ApplicationMessage.WARNING));
            Utils.redirect(pageParams, wikiPortlet.getWikiMode());
            return;
          }

        }
        if (wikiPortlet.getWikiMode() == WikiMode.EDITTEMPLATE) {
          Template template = wikiService.getTemplatePage(pageParams, pageEditForm.getTemplateId());
          template.setTitle(title);
          template.setDescription(StringEscapeUtils.escapeHtml(description));
          template.setContent(markup);
          template.setSyntax(syntaxId);
          wikiService.updateTemplate(template);
        } else if (wikiPortlet.getWikiMode() == WikiMode.ADDTEMPLATE) {
          Template template = new Template();
          template.setName(idTemp);
          template.setTitle(title);
          template.setDescription(StringEscapeUtils.escapeHtml(description));
          template.setContent(markup);
          template.setSyntax(syntaxId);
          template.setPermissions(null);
          wikiService.createTemplatePage(Utils.getCurrentWiki(), template);
          ApplicationMessage message = new ApplicationMessage("SaveTemplateAction.msg.Create-template-successfully", msgArg, ApplicationMessage.INFO);
          message.setArgsLocalized(false);
          event.getRequestContext().getUIApplication().addMessage(message);
        }
      } catch (Exception e) {
        log.error("An exception happens when saving the page with title:" + title, e);
        event.getRequestContext().getUIApplication().addMessage(new ApplicationMessage("UIPageToolBar.msg.Exception",
                                                                                       null,
                                                                                       ApplicationMessage.ERROR));
      } finally {
        if (!isExist) {
          UITemplateSettingForm uiTemplateSettingForm = wikiPortlet.findFirstComponentOfType(UITemplateSettingForm.class);
          if (uiTemplateSettingForm != null) {
            // Update template list
            uiTemplateSettingForm.initGrid();
          }
          Utils.redirect(pageParams, WikiMode.SPACESETTING);
        }
        super.processEvent(event);
      }
    }
  }
}
