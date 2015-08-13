/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.rendering.render.xwiki;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.wiki.rendering.render.xwiki.image.BaseImageWriterImpl;
import org.exoplatform.wiki.rendering.util.Utils;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.formula.FormulaRenderer;
import org.xwiki.formula.FormulaRenderer.FontSize;
import org.xwiki.formula.FormulaRenderer.Type;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.internal.macro.formula.FormulaMacro;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.formula.FormulaMacroConfiguration;
import org.xwiki.rendering.macro.formula.FormulaMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 14, 2013  
 */
@Component
@Named("formula")
@Singleton
public class WikiFormulaMacro extends FormulaMacro {
  
  /** Component manager, needed for retrieving the selected formula renderer. */
  @Inject
  private ComponentManager manager;
  
  /** Defines from where to read the rendering configuration data. */
  @Inject
  private FormulaMacroConfiguration configuration;
  
  /**
   * The logger to log.
   */
  @Inject
  private Logger logger;  
  
  @Override
  public List<Block> execute(FormulaMacroParameters parameters, String content, MacroTransformationContext context)
      throws MacroExecutionException
  {
      if (StringUtils.isEmpty(content)) {
          throw new MacroExecutionException(CONTENT_MISSING_ERROR);
      }

      String rendererHint = this.configuration.getRenderer();
      FontSize size = parameters.getFontSize();
      Type type = parameters.getImageType();
      Block result = null;
      try {
          result = render(content, context.isInline(), size, type, rendererHint);
      } catch (ComponentLookupException ex) {
          this.logger.warn("Invalid renderer: [" + rendererHint + "]. Falling back to the safe renderer.");
          try {
              result = render(content, context.isInline(), size, type, this.configuration.getSafeRenderer());
          } catch (ComponentLookupException ex2) {
              this.logger.error("Safe renderer not found. No image generated. Returning plain text.", ex);
          } catch (IllegalArgumentException ex2) {
              throw new MacroExecutionException(WRONG_CONTENT_ERROR);
          }
      } catch (IllegalArgumentException ex) {
          throw new MacroExecutionException(WRONG_CONTENT_ERROR);
      }

      // If no image was generated, just return the original text
      if (result == null) {
          result = new WordBlock(content);
      }
      // Block level formulae should be wrapped in a paragraph element
      if (!context.isInline()) {
          result = new ParagraphBlock(Collections.<Block> singletonList(result));
      }
      return Collections.singletonList(result);
  }  
  
  /**
   * Renders the formula using the specified renderer.
   * 
   * @param formula the formula text
   * @param inline is the formula supposed to be used inline or as a block-level element
   * @param fontSize the specified font size
   * @param imageType the specified resulting image type
   * @param rendererHint the hint for the renderer to use
   * @return the resulting block holding the generated image, or {@code null} in case of an error.
   * @throws ComponentLookupException if no component with the specified hint can be retrieved
   * @throws IllegalArgumentException if the formula is not valid, according to the LaTeX syntax
   */
  private Block render(String formula, boolean inline, FontSize fontSize, Type imageType, String rendererHint)
      throws ComponentLookupException, IllegalArgumentException
  {
      try {
          FormulaRenderer renderer = this.manager.getInstance(FormulaRenderer.class, rendererHint);
          String imageName = renderer.process(formula, inline, fontSize, imageType);
          BaseImageWriterImpl imageWriter = Utils.getService(BaseImageWriterImpl.class);
          imageWriter.writeImage(imageName, renderer.getImage(imageName).getData());
          String url = imageWriter.getURL(imageName);
          
          ResourceReference imageReference = new ResourceReference(url, ResourceType.URL);
          ImageBlock result = new ImageBlock(imageReference, false);
          // Set the alternative text for the image to be the original formula
          result.setParameter("alt", formula);
          return result;
      } catch (IOException ex) {
          throw new ComponentLookupException("Failed to render formula using [" + rendererHint + "] renderer");
      } catch (Exception e) {
          throw new ComponentLookupException("Failed to render formula using [" + rendererHint + "] renderer");
      }
  }  
}
