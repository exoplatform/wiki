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
package org.exoplatform.wiki.rendering.macro.iframe;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Manage an iframe node
 */
@Component("iframe")
public class IFrameMacro extends AbstractMacro<IFrameMacroParameters> {
  public static final String MACRO_CATEGORY_OTHER = "Other";
  private static final String URL_INVALID = "/wiki/templates/wiki/webui/info/UrlInvalid.html";

  /**
   * The description of the macro.
   */
  private static final String DESCRIPTION = "Embraces a block of text within a fully customizable panel";

  /**
   * Create and initialize the descriptor of the macro.
   */
  public IFrameMacro() {
    super("IFrame", DESCRIPTION, IFrameMacroParameters.class);
    setDefaultCategory(MACRO_CATEGORY_OTHER);
  }

  public List<Block> execute(IFrameMacroParameters parameters, String content, MacroTransformationContext context)
      throws MacroExecutionException {
    StringBuilder rawContent = new StringBuilder("<iframe ");
    if (parameters.getWidth().length() > 0) {
      rawContent.append("width=\"").append(parameters.getWidth()).append("\" ");
    } else {
      rawContent.append("width=\"100%\"");
    }

    if (parameters.getHeight().length() > 0) {
      rawContent.append("height=\"").append(parameters.getHeight()).append("\" ");
    } else {
      rawContent.append("height=\"100%\"");
    }

    rawContent.append("src=\"").
    append(valid(parameters.getSrc()) ? parameters.getSrc() : URL_INVALID).
    append("\"");
    rawContent.append("></iframe>");
    RawBlock panelBlock = new RawBlock(rawContent.toString(), Syntax.XHTML_1_0);
    return Collections.singletonList((Block) panelBlock);
  }
  
  /**
   * checks if the url is valid.
   * @param urlValue the url
   * @return the validity of the url
   */
  private boolean valid(String urlValue) {
    try {
      URL url = new URL(urlValue);
      url.toURI();
    } catch ( MalformedURLException e ) {
      return false;
    } catch (URISyntaxException e) {
      return false;
    }
    return true;
  }

  public boolean supportsInlineMode() {
    return true;
  }
}
