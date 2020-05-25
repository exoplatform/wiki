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
package org.exoplatform.wiki.rendering.macro.include;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component("includepage")
public class IncludePageMacro extends AbstractMacro<IncludePageMacroParameters> {
  /**
   * The description of the macro
   */
  private static final String DESCRIPTION = "Includes the contents of a page within current";
  
  public IncludePageMacro() {
    super("Include Page", DESCRIPTION, IncludePageMacroParameters.class);
    setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
  }

  @Override
  public List<Block> execute(IncludePageMacroParameters parameters,
                             String content,
                             MacroTransformationContext context) throws MacroExecutionException {
    Block includePageBlock = new RawBlock("<exo-wiki-include-page page-name=\"" + parameters.getPage() + "\"></exo-wiki-include-page>", Syntax.XHTML_1_0);

    Block container = new GroupBlock(Arrays.asList(includePageBlock));
    container.setParameter("class", "wiki-include-page");

    return Collections.singletonList(container);  }

  @Override
  public boolean supportsInlineMode() {

    return true;
  }
  
}
