/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wiki.rendering;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.context.Execution;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.converter.ConversionException;

/**
 * Renders a wiki page from its markup to something readable, such as HTML.
 * It uses a specific syntax to read the markup.
 *
 * @LevelAPI Experimental
 */
public interface RenderingService {

  /**
   * Gets the current execution instance.
   *
   * @return The execution instance.
   * @throws ComponentLookupException
   * @throws ComponentRepositoryException
   */
  public Execution getExecution() throws ComponentLookupException, ComponentRepositoryException;

  /**
   * Gets the Component Manager which provides ways to access and modify components (service) in the system.
   *
   * @return The Component Manager.
   */
  public ComponentManager getComponentManager();

  /**
   * Renders a wiki page from its markup to HTML by using the right syntax.
   *
   * @param markup The wiki page markup.
   * @param sourceSyntax The syntax used by the wiki page.
   * @param targetSyntax The target syntax to apply.
   * @param supportSectionEdit If "true", the "Edit section" function is supported. Otherwise, this function is not supported.
   * @return The readable content of the rendered wiki page.
   * @throws Exception
   */
  public String render(String markup, String sourceSyntax, String targetSyntax, boolean supportSectionEdit) throws ConversionException, ComponentLookupException;

  /**
   * Gets content of a section.
   *
   * @param markup The wiki page markup.
   * @param sourceSyntax The syntax used by the wiki page.
   * @param sectionIndex The index of the section.
   * @return The section content.
   * @throws Exception
   */
  public String getContentOfSection(String markup, String sourceSyntax, String sectionIndex) throws Exception;

  /**
   * Updates content of the selected section.
   *
   * @param markup The wiki page markup.
   * @param sourceSyntax The syntax used by the wiki page.
   * @param sectionIndex The section index.
   * @param newSectionContent New content of the section.
   * @return Content of the page which includes the modified section.
   * @throws Exception
   */
  public String updateContentOfSection(String markup, String sourceSyntax, String sectionIndex, String newSectionContent) throws Exception;

  /**
   * Parses a wiki page markup based on the syntax.
   *
   * @param markup The markup to parse.
   * @param sourceSyntax The syntax to use.
   * @return The XDOM object.
   * @throws Exception
   */
  public XDOM parse(String markup, String sourceSyntax) throws Exception;

    /**
     * Gets a CSS URL.
     *
     * @return The CSS URL.
     */
  public String getCssURL();

    /**
     * Sets a CSS URL.
     *
     * @param cssURL The CSS URL.
     */
  public void setCssURL(String cssURL);

}
