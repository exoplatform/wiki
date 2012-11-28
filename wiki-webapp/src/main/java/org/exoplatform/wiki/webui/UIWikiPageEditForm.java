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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.webui.control.UIEditorTabs;
import org.exoplatform.wiki.webui.control.UISubmitToolBar;
import org.exoplatform.wiki.webui.core.UIWikiForm;
import org.exoplatform.wiki.webui.popup.UIWikiPagePreview;

/**
 * Created by The eXo Platform SAS
 * Author : viet nguyen
 *          viet.nguyen@exoplatform.com
 * May 14, 2010  
 */
@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "app:/templates/wiki/webui/UIWikiPageEditForm.gtmpl",
  events = {
      @EventConfig(listeners = UIWikiPageEditForm.CloseActionListener.class),
      @EventConfig(listeners = UIWikiPageEditForm.DeleteDraftActionListener.class),
      @EventConfig(listeners = UIWikiPageEditForm.ResumeDraftActionListener.class),
      @EventConfig(listeners = UIWikiPageEditForm.ViewDraftChangeActionListener.class),
      @EventConfig(listeners = UIWikiPageEditForm.CancelDraftActionListener.class)
  }  
)
public class UIWikiPageEditForm extends UIWikiForm {
  
  public static final String UNTITLED                  = "Untitled";

  public static final String FIELD_CONTENT             = "Markup";

  public static final String FIELD_COMMENT             = "Comment";
  
  public static final String FIELD_PUBLISH_ACTIVITY_UPPER    = "PublishActivityUpper";
  
  public static final String FIELD_PUBLISH_ACTIVITY_BOTTOM    = "PublishActivityBottom";
  
  public static final String TITLE_CONTROL             = "UIWikiPageTitleControlForm_PageEditForm";

  public static final String EDITOR_TABS               = "UIEditorTabs";

  public static final String SUBMIT_TOOLBAR_UPPER      = "UISubmitToolBarUpper";

  public static final String SUBMIT_TOOLBAR_BOTTOM     = "UISubmitToolBarBottom";

  public static final String HELP_PANEL                = "UIWikiSidePanelArea";

  public static final String RICHTEXT_AREA             = "UIWikiRichTextArea";

  public static final String FIELD_TEMPLATEDESCTIPTION = "UIWikiTemplateDescriptionContainer";
  
  private boolean            isTemplate        = false;

  private String             templateId        = StringUtils.EMPTY;

  private String             title;
  
  private List<String> notificationMessages = new ArrayList<String>();

  private String initDraftName = StringUtils.EMPTY;

  private boolean isRunAutoSave = true;
  
  public static final String CLOSE = "Close";
  
  public UIWikiPageEditForm() throws Exception {
    this.accept_Modes = Arrays.asList(new WikiMode[] { WikiMode.EDITPAGE, WikiMode.ADDPAGE,
        WikiMode.EDITTEMPLATE, WikiMode.ADDTEMPLATE });
    addChild(UIWikiPageTitleControlArea.class, null, TITLE_CONTROL).toInputMode();
    addChild(UISubmitToolBar.class, null, SUBMIT_TOOLBAR_UPPER);
    addUIFormInput(new UICheckBoxInput(FIELD_PUBLISH_ACTIVITY_UPPER, FIELD_PUBLISH_ACTIVITY_UPPER, false));
    addChild(UIWikiTemplateDescriptionContainer.class, null, FIELD_TEMPLATEDESCTIPTION);
    addChild(UIEditorTabs.class, null, EDITOR_TABS);
    addChild(UISubmitToolBar.class, null, SUBMIT_TOOLBAR_BOTTOM);
    addChild(UIWikiSidePanelArea.class, null, HELP_PANEL);
    addChild(UIWikiRichTextArea.class, null, RICHTEXT_AREA).setRendered(false);
    UIFormTextAreaInput markupInput = new UIFormTextAreaInput(FIELD_CONTENT, FIELD_CONTENT, "");
    markupInput.setHTMLAttribute("title", getLabel(FIELD_CONTENT));
    addUIFormInput(markupInput);
    UIFormStringInput commentInput = new UIFormStringInput(FIELD_COMMENT, FIELD_COMMENT, "");
    addUIFormInput(commentInput);
    addUIFormInput(new UICheckBoxInput(FIELD_PUBLISH_ACTIVITY_BOTTOM, FIELD_PUBLISH_ACTIVITY_BOTTOM, false));
  }

  protected void checkToDissplayNotification() throws Exception {
    if ("__anonim".equals(org.exoplatform.wiki.utils.Utils.getCurrentUser())) {
      isRunAutoSave = false;
      return;
    }
    
    // Check to display info message if the draft for this page exist
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
    UIWikiPortlet wikiPortlet = getAncestorOfType(UIWikiPortlet.class);
    notificationMessages.clear();
    isRunAutoSave = true;
    if (wikiPortlet.getWikiMode() == WikiMode.EDITPAGE) {
      DraftPage draftPage = wikiService.getDraft(pageParams);
      if (draftPage != null) {
        if (!draftPage.getName().equals(initDraftName)) {
          isRunAutoSave = false;
          if (draftPage.isOutDate()) {
            notificationMessages.add(createDraftOutdateNotification());
          } else {
            notificationMessages.add(createDraftExistNotification(draftPage.getUpdatedDate()));
          }
        }
      }
      
      List<String> edittingUsers = org.exoplatform.wiki.utils.Utils.getListOfUserEditingPage(pageParams.getPageId());
      if (edittingUsers.size() > 0) {
        notificationMessages.add(createCocurrentEdittingNotification(edittingUsers));
      }
    }
  }
  
  private String createDraftOutdateNotification() throws Exception {
    ResourceBundle bundle = RequestContext.getCurrentInstance().getApplicationResourceBundle();
    
    // Build message markup
    String messageMarkup = bundle.getString("DraftPage.msg.draft-version-outdated");
    String messageHTML = "<div class='box notemessage'>" + messageMarkup + "</div>";
    
    // Add actions to message html
    String viewChangeDraftLabel = bundle.getString("DraftPage.label.view-your-change");
    String viewChangeActionLink = event("ViewDraftChange");
    String continueEdittingLabel = bundle.getString("DraftPage.label.continue-editing");
    String continueEdittingActionLink = event("ResumeDraft");
    String deleteDraftLabel = bundle.getString("DraftPage.label.delete");
    String deleteActionLink = event("DeleteDraft");
    messageHTML = messageHTML.replace("{0}", "<a title=\""+ viewChangeDraftLabel + "\" href=\"" + viewChangeActionLink + "\">" + viewChangeDraftLabel + "</a>");
    messageHTML = messageHTML.replace("{1}", "<a title=\""+ continueEdittingLabel + "\" href=\"" + continueEdittingActionLink + "\">" + continueEdittingLabel + "</a>");
    messageHTML = messageHTML.replace("{2}", "<a title=\""+ deleteDraftLabel + "\" href=\"" + deleteActionLink + "\">" + deleteDraftLabel + "</a>");
    return messageHTML;
  }
  
  private String createDraftExistNotification(Date draftUpdatedDate) throws Exception {
    ResourceBundle bundle = RequestContext.getCurrentInstance().getApplicationResourceBundle();
    
    // Build message markup
    String messageMarkup = bundle.getString("DraftPage.msg.draft-exist-notification");
    String dateString = new SimpleDateFormat("MMM dd, yyyy HH:mm").format(draftUpdatedDate);
    messageMarkup = messageMarkup.replace("{0}", dateString);
    String messageHTML = "<div class='box notemessage'>" + messageMarkup + "</div>";
    
    // Add actions to message html
    String viewChangeDraftLabel = bundle.getString("DraftPage.label.view-your-change");
    String viewChangeActionLink = event("ViewDraftChange");
    String resumeDraftLabel = bundle.getString("DraftPage.label.resume-the-draft");
    String resumeActionLink = event("ResumeDraft");
    String deleteDraftLabel = bundle.getString("DraftPage.label.delete");
    String deleteActionLink = event("DeleteDraft");
    
    messageHTML = messageHTML.replace("{1}", "<a title=\""+ viewChangeDraftLabel + "\" href=\"" + viewChangeActionLink + "\">" + viewChangeDraftLabel + "</a>");
    messageHTML = messageHTML.replace("{2}", "<a title=\""+ resumeDraftLabel + "\" href=\"" + resumeActionLink + "\">" + resumeDraftLabel + "</a>");
    messageHTML = messageHTML.replace("{3}", "<a title=\""+ deleteDraftLabel + "\" href=\"" + deleteActionLink + "\">" + deleteDraftLabel + "</a>");
    return messageHTML; 
  }
  
  private String createCocurrentEdittingNotification(List<String> users) throws Exception {
    // Concat all user name
    StringBuilder usernameList = new StringBuilder();
    OrganizationService organizationService = (OrganizationService) PortalContainer.getComponent(OrganizationService.class);
    for (String user : users) {
      usernameList.append("**");
      User userObject = organizationService.getUserHandler().findUserByName(user);
      usernameList.append(userObject.getFullName());
      usernameList.append("**, ");
    }
    
    // Remove 2 last chars
    usernameList.setLength(usernameList.length() - 2);
    
    // Build message markup
    ResourceBundle bundle = RequestContext.getCurrentInstance().getApplicationResourceBundle();
    String messageMarkup = bundle.getString("DraftPage.msg.concurrent-editing");
    messageMarkup = messageMarkup.replace("{0}", usernameList.toString());
    messageMarkup = "<div class='box warningmessage'>" + messageMarkup + "</div>";;
    
    // Render to message html and return
    return messageMarkup;
  }
 
  public void setTitle(String title){ this.title = title ;}
  public String getTitle(){ return title ;}

  public String getInitDraftName() {
    return initDraftName;
  }

  public void setInitDraftName(String initDraftName) {
    this.initDraftName = initDraftName;
  }
  
  public boolean isNewPage() {
    UIWikiPortlet wikiPortlet = getAncestorOfType(UIWikiPortlet.class);
    return wikiPortlet.getWikiMode() != WikiMode.EDITPAGE;
  }
  
  public long getAutoSaveSequenceTime() {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    return wikiService.getSaveDraftSequenceTime();
  }

  protected String getSaveDraftRestUrl() {
    StringBuilder childrenURLSb = new StringBuilder(Utils.getCurrentRestURL());
    childrenURLSb.append("/wiki/saveDraft/");
    return childrenURLSb.toString();
  }
  
  protected String getRemoveDraftRestUrl() {
    StringBuilder childrenURLSb = new StringBuilder(Utils.getCurrentRestURL());
    childrenURLSb.append("/wiki/removeDraft/");
    return childrenURLSb.toString();
  }
  
  protected String getWikiType() throws Exception {
    WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
    return pageParams.getType();
  }
  
  protected String getWikiOwner() throws Exception {
    WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
    return pageParams.getOwner();
  }
  
  protected String getCurrentPageId() throws Exception {
    WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
    return pageParams.getPageId();
  }
  
  protected String getCurrentPageRevision() throws Exception {
    UIWikiPortlet wikiPortlet = getAncestorOfType(UIWikiPortlet.class);
    if (wikiPortlet.getWikiMode() == WikiMode.EDITPAGE) {
      Page page = Utils.getCurrentWikiPage();
      return org.exoplatform.wiki.utils.Utils.getLastRevisionOfPage(page).getName();
    }
    return StringUtils.EMPTY;
  }
  
  public List getNotificationMessages() {
    return notificationMessages;
  }
 
  public boolean isTemplate() {
    return isTemplate;
  }

  public void setTemplate(boolean isTemplate) {
    this.isTemplate = isTemplate;
  }

  public String getTemplateId() {
    return templateId;
  }

  public void setTemplateId(String templateId) {
    this.templateId = templateId;
  }

  public boolean isSidePanelRendered(){
    return getChild(UIWikiSidePanelArea.class).isRendered();
  }

  public boolean isRunAutoSave() {
    return isRunAutoSave;
  }
  
  protected String getCancelDraftEvent() throws Exception {
    return org.exoplatform.wiki.commons.Utils.createFormActionLink(this, "CancelDraft", null);
  }
  
  public void synPublishActivityStatus(boolean isChecked) {
    UICheckBoxInput publishActivityUpperCheckBox = 
        this.findComponentById(UIWikiPageEditForm.FIELD_PUBLISH_ACTIVITY_UPPER);
    UICheckBoxInput publishActivityBottomCheckBox = 
        this.findComponentById(UIWikiPageEditForm.FIELD_PUBLISH_ACTIVITY_BOTTOM);
    publishActivityUpperCheckBox.setChecked(isChecked);
    publishActivityBottomCheckBox.setChecked(isChecked);
  }

  private void checkRenderOfPublishActivityCheckBoxes() {
    UICheckBoxInput publishActivityUpperCheckBox = 
        this.findComponentById(UIWikiPageEditForm.FIELD_PUBLISH_ACTIVITY_UPPER);
    UICheckBoxInput publishActivityBottomCheckBox = 
        this.findComponentById(UIWikiPageEditForm.FIELD_PUBLISH_ACTIVITY_BOTTOM);
    if (WikiMode.EDITPAGE == this.getCurrentMode()) {
      publishActivityUpperCheckBox.setRendered(true);
      publishActivityBottomCheckBox.setRendered(true);
    } else {
      publishActivityUpperCheckBox.setRendered(false);
      publishActivityBottomCheckBox.setRendered(false);
    }
  }
  
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    this.checkRenderOfPublishActivityCheckBoxes();
    super.processRender(context);
  }

  public static class DeleteDraftActionListener extends EventListener<UIWikiPageEditForm> {
    @Override
    public void execute(Event<UIWikiPageEditForm> event) throws Exception {
      WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
      WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
      wikiService.removeDraft(pageParams);
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource());
    }
  }
  
  public static class ResumeDraftActionListener extends EventListener<UIWikiPageEditForm> {
    @Override
    public void execute(Event<UIWikiPageEditForm> event) throws Exception {
      UIWikiPageEditForm pageEditForm = event.getSource();
      UIWikiPortlet wikiPortlet = pageEditForm.getAncestorOfType(UIWikiPortlet.class);
      
      WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
      WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
      if (wikiPortlet.getWikiMode() == WikiMode.EDITPAGE) {
        DraftPage draftPage = wikiService.getDraft(pageParams);
        if (draftPage != null) {
          UIFormStringInput titleInput = pageEditForm.getChild(UIWikiPageTitleControlArea.class).getUIStringInput();
          UIFormTextAreaInput markupInput = pageEditForm.findComponentById(UIWikiPageEditForm.FIELD_CONTENT);
          
          String title = draftPage.getTitle();
          String content = draftPage.getContent().getText();
          titleInput.setEditable(true);
          titleInput.setValue(title);
          pageEditForm.setTitle(title);
          markupInput.setValue(content);
          UIWikiRichTextArea wikiRichTextArea = pageEditForm.getChild(UIWikiRichTextArea.class);
          if (wikiRichTextArea.isRendered()) {
            Utils.feedDataForWYSIWYGEditor(pageEditForm, null);
          }
          pageEditForm.setInitDraftName(draftPage.getName());
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource());
    }
  }
  
  public static class ViewDraftChangeActionListener extends EventListener<UIWikiPageEditForm> {
    @Override
    public void execute(Event<UIWikiPageEditForm> event) throws Exception {
      UIWikiPageEditForm pageEditForm = event.getSource();
      UIWikiPortlet wikiPortlet = pageEditForm.getAncestorOfType(UIWikiPortlet.class);
      
      WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
      WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
      if (wikiPortlet.getWikiMode() == WikiMode.EDITPAGE) {
        DraftPage draftPage = wikiService.getDraft(pageParams);
        if (draftPage != null) {
          WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
          ResourceBundle res = context.getApplicationResourceBundle() ;
          UIWikiMaskWorkspace uiMaskWS = wikiPortlet.getChild(UIWikiMaskWorkspace.class);
          UIWikiPagePreview wikiPagePreview = uiMaskWS.createUIComponent(UIWikiPagePreview.class, null, null);
          wikiPagePreview.setPageTitle(draftPage.getTitle());
          wikiPagePreview.setContent(draftPage.getChanges().getDiffHTML());
          uiMaskWS.setUIComponent(wikiPagePreview);
          uiMaskWS.setShow(true);
          uiMaskWS.setPopupTitle(res.getString("DraftPage.title.draft-changes"));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiMaskWS);
        }
      }
    }
  }
  
  public static class CancelDraftActionListener extends EventListener<UIWikiPageEditForm> {
    @Override
    public void execute(Event<UIWikiPageEditForm> event) throws Exception {
      UIWikiPageEditForm pageEditForm = event.getSource();
      UIWikiPortlet wikiPortlet = pageEditForm.getAncestorOfType(UIWikiPortlet.class);
      WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
      WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
      
      if (wikiPortlet.getWikiMode() == WikiMode.EDITPAGE) {
        wikiService.removeDraft(pageParams);
      } else {
        DraftPage draftPage = wikiService.getLastestDraft();
        if (draftPage.isNewPage()) {
          draftPage.remove();
        }
      }
      Utils.redirect(pageParams, WikiMode.VIEW);
    }
  }

  static public class CloseActionListener extends EventListener<UIWikiPageEditForm> {
   @Override
    public void execute(Event<UIWikiPageEditForm> event) throws Exception {
      UIWikiSidePanelArea sidePanelForm = event.getSource().getChild(UIWikiSidePanelArea.class);
      sidePanelForm.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource());
    }
  }
}
