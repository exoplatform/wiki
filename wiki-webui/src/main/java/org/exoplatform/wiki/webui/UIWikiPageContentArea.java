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

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.PageVersion;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.webui.core.UIWikiContainer;
import org.xwiki.rendering.syntax.Syntax;

import java.util.Arrays;

@ComponentConfig(
  lifecycle = Lifecycle.class,
  template = "app:/templates/wiki/webui/UIWikiPageContentArea.gtmpl"
)
public class UIWikiPageContentArea extends UIWikiContainer {

  public static final String VIEW_DISPLAY = "UIViewContentDisplay";

  private WikiService wikiService;

  public UIWikiPageContentArea() throws Exception{
    wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);

    this.accept_Modes = Arrays.asList(new WikiMode[] { WikiMode.VIEW, WikiMode.VIEWREVISION });
    this.addChild(UIWikiPageControlArea.class, null, null);
    this.addChild(UIWikiVersionSelect.class, null, null);
    this.addChild(UIWikiContentDisplay.class, null, VIEW_DISPLAY);    
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    renderVersion();
    super.processRender(context);
  }

  private void renderVersion() throws Exception {
    String currentVersionName= this.getChild(UIWikiVersionSelect.class).getVersionName();
    UIWikiPortlet wikiPortlet = getAncestorOfType(UIWikiPortlet.class);
    WikiMode currentMode= wikiPortlet.getWikiMode();
    RenderingService renderingService = (RenderingService) PortalContainer.getComponent(RenderingService.class);
    UIWikiContentDisplay contentDisplay = this.getChildById(VIEW_DISPLAY);

    Page wikipage = Utils.getCurrentWikiPage();

    //Setup wiki context
    Utils.setUpWikiContext(wikiPortlet);
    try{
      // Render current content
      if (currentMode.equals(WikiMode.VIEW)) {
          contentDisplay.setHtmlOutput(wikiService.getPageRenderedContent(wikipage, Syntax.XHTML_1_0.toIdString()));
      }
      // Render select version content
      if (currentMode.equals(WikiMode.VIEWREVISION) && currentVersionName != null) {
        PageVersion version = wikiService.getVersionOfPageByName(currentVersionName, wikipage);
        if (version != null) {
          String pageContent = version.getContent();
          contentDisplay.setHtmlOutput(pageContent);
        }
      }
    } catch(Exception e) {
      contentDisplay.setHtmlOutput("Bad syntax in content! Cannot generate HTML content!");
    }
    Utils.removeWikiContext();
  }
}
