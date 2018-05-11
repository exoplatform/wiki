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

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.Attachment;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.utils.WikiNameValidator;
import org.exoplatform.wiki.webui.control.UIAttachmentContainer;
import org.exoplatform.wiki.webui.control.filter.EditPagesPermissionFilter;
import org.exoplatform.wiki.webui.control.listener.UIWikiPortletActionListener;
import org.exoplatform.wiki.webui.core.UIWikiForm;
import org.exoplatform.wiki.webui.form.UIWikiFormUploadInput;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "app:/templates/wiki/webui/UIWikiUploadAttachment.gtmpl",
  events = {
    @EventConfig(listeners = UIWikiUploadAttachment.UploadAttachmentActionListener.class)
  }
)
public class UIWikiUploadAttachment extends UIWikiForm {
  public static int SIZE_LIMIT = -1;
  
  public static String FIELD_UPLOAD = UIWikiFormUploadInput.UPLOAD_ACTION;

  private static final Log log = ExoLogger.getLogger(UIWikiUploadAttachment.class.getName());
  
  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] { new EditPagesPermissionFilter() });

  private static WikiService wikiService;

  public UIWikiUploadAttachment() throws Exception {
    wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    this.accept_Modes = Arrays.asList(new WikiMode[] { WikiMode.VIEW,WikiMode.EDITPAGE,WikiMode.ADDPAGE});   
    SIZE_LIMIT = Utils.getLimitUploadSize();
    UIWikiFormUploadInput uiInput = new UIWikiFormUploadInput(FIELD_UPLOAD, FIELD_UPLOAD, SIZE_LIMIT);
    addUIFormInput(uiInput);
  }

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }
  
  private Page getCurrentWikiPage() throws Exception {
    UIWikiPortlet wikiPortlet = this.getAncestorOfType(UIWikiPortlet.class);
    if (wikiPortlet.getWikiMode() == WikiMode.ADDPAGE) {
      return Utils.getCurrentNewDraftWikiPage();
    }
    return Utils.getCurrentWikiPage();
  }

  static public class UploadAttachmentActionListener extends UIWikiPortletActionListener<UIWikiUploadAttachment> {
    @Override
    public void processEvent(Event<UIWikiUploadAttachment> event) throws Exception {                 
      UIWikiUploadAttachment wikiAttachmentArea = event.getSource();
      String id = event.getRequestContext().getRequestParameter(OBJECTID);
      UIWikiFormUploadInput input = wikiAttachmentArea.getUIInput(FIELD_UPLOAD);
      UploadResource uploadResource = input.getUploadResource(id);
      
      String fileName = null;
      try {
        if (uploadResource != null) {
          fileName = uploadResource.getFileName();
          if (fileName != null) {  
            fileName = org.exoplatform.wiki.utils.Utils.escapeIllegalCharacterInName(fileName);
            WikiNameValidator.validateFileName(fileName);
          }
        }
      } catch (IllegalArgumentException ex) {
        event.getRequestContext()
             .getUIApplication()
             .addMessage(new ApplicationMessage("AttachmentNameValidator.msg.Invalid-char", null, ApplicationMessage.WARNING));        
        event.getRequestContext().setProcessRender(true);
      }
      
      if (event.getRequestContext().getProcessRender()) {        
        resetUploadInput(event);
        return;
      }
      
      byte[] imageBytes;
      Attachment attachment = null;
      if (uploadResource != null) {
        long fileSize = ((long) uploadResource.getUploadedSize());
        if (SIZE_LIMIT > 0 && fileSize >= SIZE_LIMIT * 1024 * 1024) {
          event.getRequestContext()
               .getUIApplication()
               .addMessage(new ApplicationMessage("UIFormUploadInput.msg.attachment-limit",
                                                  new String[] { String.valueOf(SIZE_LIMIT) },
                                                  ApplicationMessage.WARNING));
          resetUploadInput(event);
          return;
        }
        
        InputStream is = null;
        
        try {
          is = input.getUploadDataAsStream(id);
          if (is == null) {
            throw new FileNotFoundException();
          }
        } catch (FileNotFoundException ex) {
          event.getRequestContext()
               .getUIApplication()
               .addMessage(new ApplicationMessage("UIWikiUploadAttachment.msg.file-not-exist", null, ApplicationMessage.WARNING));
          resetUploadInput(event);
          return;
        }
        
        imageBytes = new byte[is.available()];
        is.read(imageBytes);
        attachment = new Attachment();
        attachment.setName(fileName);
        if (uploadResource.getFileName().lastIndexOf(".") > 0) {
          attachment.setTitle(uploadResource.getFileName().substring(0, uploadResource.getFileName().lastIndexOf(".")));
        }
        attachment.setMimeType(uploadResource.getMimeType());
        attachment.setContent(imageBytes);
        attachment.setCreator(event.getRequestContext().getRemoteUser());
      }
      
      if (attachment != null) {
        try {          
          Page page = wikiAttachmentArea.getCurrentWikiPage();
          if (wikiService.getAttachmentOfPageByName(fileName, page) != null) {
            event.getRequestContext()
                 .getUIApplication()
                 .addMessage(new ApplicationMessage("UIWikiUploadAttachment.msg.file-already-exists",
                                                    null,
                                                    ApplicationMessage.WARNING));
            resetUploadInput(event);
            return;
          }
          wikiService.addAttachmentToPage(attachment, page);
          input.removeUploadId(id);
        } catch (Exception e) {
          log.error("An exception happens when saving attach file:" + attachment.getName(), e);
          event.getRequestContext().getUIApplication().addMessage(new ApplicationMessage("UIApplication.msg.unknown-error",
                                                                                         null,
                                                                                         ApplicationMessage.ERROR));
        } finally {
          resetUploadInput(event);        
        }
      }      
    }

    private void resetUploadInput(Event<UIWikiUploadAttachment> event) {
      UIWikiPortlet wikiPortlet = event.getSource().getAncestorOfType(UIWikiPortlet.class);
      UIWikiUploadAttachment wikiAttachmentArea = event.getSource();
      UIWikiBottomArea bottomArea= wikiPortlet.findFirstComponentOfType(UIWikiBottomArea.class);
      wikiAttachmentArea.removeChildById(FIELD_UPLOAD);
      UIWikiFormUploadInput uiInput = new UIWikiFormUploadInput(FIELD_UPLOAD, FIELD_UPLOAD, SIZE_LIMIT);
      wikiAttachmentArea.addChild(uiInput);
      UIWikiPageInfoArea inforArea= wikiPortlet.findFirstComponentOfType(UIWikiPageInfoArea.class);
      if (bottomArea != null && bottomArea.isRendered() && bottomArea.getAccept_Modes().contains(bottomArea.getCurrentMode())) {
        event.getRequestContext().addUIComponentToUpdateByAjax(bottomArea);
      }
      if (inforArea != null && inforArea.isRendered() && inforArea.getAccept_Modes().contains(inforArea.getCurrentMode())) {
        event.getRequestContext().addUIComponentToUpdateByAjax(inforArea);
      }
    }

    @Override
    protected String getExtensionType() {
      return UIAttachmentContainer.EXTENSION_TYPE;
    }
  }
}
