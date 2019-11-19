/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.PageVersion;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.webui.UIWikiPortlet.PopupLevel;
import org.exoplatform.wiki.webui.control.UIAttachmentContainer;
import org.exoplatform.wiki.webui.core.UIWikiContainer;

import java.util.Arrays;
import java.util.List;

@ComponentConfig(
  lifecycle = Lifecycle.class,
  template = "app:/templates/wiki/webui/UIWikiPageInfoArea.gtmpl",
  events = {
    @EventConfig(listeners = UIWikiPageInfoArea.PermalinkActionListener.class, csrfCheck = false),
    @EventConfig(listeners = UIWikiPageInfoArea.CompareRevisionActionListener.class, csrfCheck = false),
    @EventConfig(listeners = UIWikiPageInfoArea.ShowRevisionActionListener.class, csrfCheck = false),
    @EventConfig(listeners = UIWikiPageInfoArea.ToggleAttachmentsActionListener.class, csrfCheck = false)
  }
)
public class UIWikiPageInfoArea extends UIWikiContainer {

  private static final Log log = ExoLogger.getLogger("wiki:UIWikiPageInfoArea");

  public static String TOGGLE_ATTACHMENTS_ACTION = "ToggleAttachments";
  
  public static String SHOW_REVISION = "ShowRevision";
  
  public static String COMPARE_REVISION = "CompareRevision";
  
  public static final String PERMALINK_ACTION = "Permalink";

  private WikiService wikiService;
  
  public UIWikiPageInfoArea() {
    wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);

    this.accept_Modes = Arrays.asList(new WikiMode[] { WikiMode.VIEW });
  }

  protected Page getCurrentWikiPage() {
    Page currentPage = null;
    try {
      currentPage = Utils.getCurrentWikiPage();
    } catch (Exception e) {
      log.warn("An error happened when getting current wiki page", e);
    }
    return currentPage;
  }

  protected int getNumberOfAttachments(Page page) {
    int nbOfAttachments = 0;
    try {
      nbOfAttachments = wikiService.getNbOfAttachmentsOfPage(page);
    } catch (WikiException e) {
      log.error("Cannot get number of attachments of " + page.getWikiType() + ":" + page.getWikiOwner()
              + ":" + page.getName() + " - Cause : " + e.getMessage(), e);
    }
    return nbOfAttachments;
  }

  protected int getNumberOfVersions(Page page) {
    int nbOfversions = 0;
    try {
      List<PageVersion> versions = wikiService.getVersionsOfPage(page);
      if (versions != null && !versions.isEmpty()) {
        nbOfversions = versions.size();
      }
    } catch (Exception e) {
      log.error("Cannot get versions of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName()
              + " - Cause : " + e.getMessage(), e);
    }

    return nbOfversions;
  }
  
  protected boolean isPagePublic(Page page) throws Exception {
    return Utils.isPagePublic(page);
  }
  
  public static class PermalinkActionListener extends EventListener<UIWikiPageInfoArea> {
    @Override
    public void execute(Event<UIWikiPageInfoArea> event) throws Exception {
      UIWikiPortlet uiWikiPortlet = event.getSource().getAncestorOfType(UIWikiPortlet.class);
      UIPopupContainer uiPopupContainer = uiWikiPortlet.getPopupContainer(PopupLevel.L1);
      uiPopupContainer.activate(UIWikiPermalinkForm.class, 800);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

  public static class ToggleAttachmentsActionListener extends EventListener<UIWikiPageInfoArea> {
    @Override
    public void execute(Event<UIWikiPageInfoArea> event) throws Exception {
      UIWikiPortlet uiWikiPortlet = event.getSource().getAncestorOfType(UIWikiPortlet.class);
      UIWikiBottomArea bottomArea = uiWikiPortlet.findFirstComponentOfType(UIWikiBottomArea.class);
      UIAttachmentContainer attachform = bottomArea.findFirstComponentOfType(UIAttachmentContainer.class);
      if (attachform.isRendered()) {
        attachform.setRendered(false);
      } else {
        attachform.setRendered(true);
        UIWikiPageVersionsList pageVersions = bottomArea.findFirstComponentOfType(UIWikiPageVersionsList.class);
        if (pageVersions.isRendered()) {
          pageVersions.setRendered(false);
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(bottomArea.getParent());
    }
  }

  public static class ShowRevisionActionListener extends EventListener<UIWikiPageInfoArea> {
    public void execute(Event<UIWikiPageInfoArea> event) throws Exception {
      UIWikiPortlet uiWikiPortlet = event.getSource().getAncestorOfType(UIWikiPortlet.class);
      UIWikiBottomArea bottomArea = uiWikiPortlet.findFirstComponentOfType(UIWikiBottomArea.class);
      UIWikiPageVersionsList pageVersions = bottomArea.getChild(UIWikiPageVersionsList.class);
      if (pageVersions.isRendered()) {
        pageVersions.setRendered(false);
      } else {
        UIAttachmentContainer attachform = bottomArea.getChild(UIAttachmentContainer.class);
        if (attachform.isRendered()) {
          attachform.setRendered(false);
        }
        pageVersions.setRendered(true);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(bottomArea.getParent());
    }
  }

  public static class CompareRevisionActionListener extends
                                                   org.exoplatform.wiki.webui.control.action.CompareRevisionActionListener {
    public void execute(Event<UIComponent> event) throws Exception {
      WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
      List<PageVersion> lstVersion = wikiService.getVersionsOfPage(Utils.getCurrentWikiPage());
      this.setVersionToCompare(lstVersion);
      WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
      String verName = pageParams.getParameter(org.exoplatform.wiki.utils.Utils.VER_NAME);
      if (!StringUtils.isEmpty(verName)) {
        for (int i = 0; i < lstVersion.size(); i++) {
          PageVersion ver = lstVersion.get(i);
          if (ver.getName().equals(verName) && i < lstVersion.size() + 1) {
            this.setFrom(i);
            this.setTo(i + 1);
            break;
          }
        }
      }
      super.execute(event);
    }
  }

}
