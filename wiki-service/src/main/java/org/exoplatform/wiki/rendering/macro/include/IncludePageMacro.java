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

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.rendering.context.MarkupContextManager;
import org.exoplatform.wiki.service.WikiContext;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.wiki.WikiModel;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@Component("includepage")
public class IncludePageMacro extends AbstractMacro<IncludePageMacroParameters> {
  /**
   * The description of the macro
   */
  private static final String DESCRIPTION = "Includes the contents of a page within current";
  
  /**
   * Used to get the current syntax parser.
   */
  @Inject
  private ComponentManager    componentManager;

  @Inject
  private Execution           execution;
  
  /**
   * Used to get the build context for document
   */
  @Inject
  private MarkupContextManager markupContextManager;
  
  public IncludePageMacro() {
    super("Include Page", DESCRIPTION, IncludePageMacroParameters.class);
    setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
  }

  @Override
  public List<Block> execute(IncludePageMacroParameters parameters,
                             String content,
                             MacroTransformationContext context) throws MacroExecutionException {
    StringBuilder includeContent = new StringBuilder();
    ExecutionContext ec = execution.getContext();
    WikiContext currentCtx = (WikiContext) ec.getProperty(WikiContext.WIKICONTEXT);
    WikiContext includeCtx = currentCtx.clone();
    WikiPageParams includeParams = markupContextManager.getMarkupContext(parameters.getPage(), ResourceType.DOCUMENT);
    Page page = null;
    try {
      page = getWikiService().getPageOfWikiByName(includeParams.getType(), includeParams.getOwner(), includeParams.getPageName());
      WikiService wikiService = ExoContainerContext.getCurrentContainer()
              .getComponentInstanceOfType(WikiService.class);
      wikiService.addPageLink(new WikiPageParams(currentCtx.getType(), currentCtx.getOwner(), currentCtx.getPageName()),
              new WikiPageParams(includeParams.getType(),
                      includeParams.getOwner(),
                      includeParams.getPageName()));
      if (page == null) {
        return Collections.emptyList();
      }
      includeParams = new WikiPageParams(includeParams.getType(), includeParams.getOwner(), page.getName());
    } catch (Exception e) {
      throw new MacroExecutionException(String.format("Failed to resolve page [%s.%s:%s]",
                                                      currentCtx.getType(),
                                                      currentCtx.getOwner(),
                                                      currentCtx.getPageTitle()));
    }
    
    if (isRecursiveInclude(currentCtx, includeParams)) {      
      throw new MacroExecutionException(String.format("Found recursive inclusion of page [%s.%s:%s]",
                                                      currentCtx.getType(),
                                                      currentCtx.getOwner(),
                                                      currentCtx.getPageTitle()));
    }
    try {
      includeCtx.includePageCtx.add(includeParams);
      // Set page context as current context
      ec.setProperty(WikiContext.WIKICONTEXT, includeCtx);      
      if (page != null) {
        includeContent.append("<div class=\"IncludePage \" >");
        includeContent.append(getRenderingService().render(page.getContent(),
                                                      page.getSyntax(),
                                                      Syntax.XHTML_1_0.toIdString(),
                                                      false));
        includeContent.append("</div>");
      }      
      Block result = new RawBlock(includeContent.toString(), Syntax.XHTML_1_0);
      return Collections.singletonList(result);
    } catch (Exception e) {
      throw new MacroExecutionException(String.format("Failed to render include page's content [%s.%s:%s]",
                                                      currentCtx.getType(),
                                                      currentCtx.getOwner(),
                                                      currentCtx.getPageTitle()));
    } finally {
      // Restore current context
      ec.setProperty(WikiContext.WIKICONTEXT, currentCtx);
    }
  }

  @Override
  public boolean supportsInlineMode() {

    return true;
  }
  
  /**
   * @return the component manager.
   */
  public ComponentManager getComponentManager() {
    return this.componentManager;
  }

  protected WikiModel getWikiModel(MacroTransformationContext context) throws MacroExecutionException {
    try {
      return getComponentManager().getInstance(WikiModel.class);
    } catch (ComponentLookupException e) {
      throw new MacroExecutionException("Failed to find wiki model", e);
    }
  }
  
  protected RenderingService getRenderingService() {
    return (RenderingService) ExoContainerContext.getCurrentContainer()
                                                 .getComponentInstanceOfType(RenderingService.class);
  }
  
  protected WikiService getWikiService() {
    return (WikiService) ExoContainerContext.getCurrentContainer()
                                                 .getComponentInstanceOfType(WikiService.class);
  }
  
  private boolean isRecursiveInclude(WikiContext context, WikiPageParams params) {
    return context.includePageCtx.contains(params);
  }
  
}
