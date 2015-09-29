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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.ext.UIExtensionManager;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.Attachment;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.webui.control.action.RemoveAttachmentActionComponent;
import org.exoplatform.wiki.webui.control.filter.RemoveAttachmentPermissionFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/wiki/webui/UIWikiAttachmentUploadListForm.gtmpl"
)
public class UIWikiAttachmentUploadListForm extends UIForm {
  private static final Log log = ExoLogger.getLogger("org.exoplatform.wiki.webui.control.UIAttachmentUploadContainer");
  
  public static final String DOWNLOAD_ACTION = "DownloadAttachment";
  
  public static final String EXTENSION_TYPE = "org.exoplatform.wiki.webui.UIWikiAttachmentUploadListForm";
  
  public UIWikiAttachmentUploadListForm() throws Exception {
    addChild(RemoveAttachmentActionComponent.class, null, null);
  }
  
  protected Collection<Attachment> getAttachmentsList() {
    Collection<Attachment> attachments = new ArrayList<>();
    try {
      Page page = getCurrentWikiPage();
      if (page != null) {
        // TODO attachements
        //attachments = page.getAttachments();
      }
    } catch (Exception e) {
      log.warn("An error happened when get attachments list", e);
    }
    return attachments;
  }
  
  public Page getCurrentWikiPage() throws Exception {
    UIWikiPortlet wikiPortlet = this.getAncestorOfType(UIWikiPortlet.class);
    if (wikiPortlet.getWikiMode() == WikiMode.ADDPAGE) {
      return Utils.getCurrentNewDraftWikiPage();
    } else {
      return Utils.getCurrentWikiPage();
    }
  }
  
  protected void renderActions(String attName) throws Exception {
    if (attName == null) {
      return;
    }
    
    RemoveAttachmentActionComponent component = getChild(RemoveAttachmentActionComponent.class);
    component.setAttachmentName(attName);
    UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
    
    Page page = getCurrentWikiPage();
    // TODO need page.getAttachment(attName)
    //Attachment attachment = page.getAttachment(attName);
    Attachment attachment = null;

    // Create context
    Map<String, Object> context = new HashMap<String, Object>();
    context.put(RemoveAttachmentPermissionFilter.ATTACHMENT_KEY, attachment);
    
    // Accept permission
    if (manager.accept(EXTENSION_TYPE, RemoveAttachmentActionComponent.DELETE_ACTION, context)) {
      renderChild(RemoveAttachmentActionComponent.class);
    }
  }
}
