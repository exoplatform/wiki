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

/**
 * The RenderingService is used to render a wiki page from its markup to something readable such as html
 * It used a specific syntax to be able to read the markup
 *
 * @LevelAPI Experimental
 */
public interface RenderingService {

  /**
   * Get the current execution instance
   *
   * @return The execution instance
   * @throws ComponentLookupException
   * @throws ComponentRepositoryException
   */
  public Execution getExecution() throws ComponentLookupException, ComponentRepositoryException;

  /**
   * Get the Component Manager
   *
   * @return The Component Manager
   */
  public ComponentManager getComponentManager();

  /**
   * Render a wiki page from its markup to html by using the right syntax
   *
   * @param markup The markup of the page
   * @param sourceSyntax The syntax used by the wiki page
   * @param targetSyntax The target syntax to apply
   * @param supportSectionEdit True if we support the section edit or false if not
   * @return The result rendered
   * @throws Exception
   */
  public String render(String markup, String sourceSyntax, String targetSyntax, boolean supportSectionEdit) throws Exception;

  /**
   * Get the content of a section
   *
   * @param markup The markup of the page
   * @param sourceSyntax The syntax used by the wiki page
   * @param sectionIndex The index of the section
   * @return The content as string
   * @throws Exception
   */
  public String getContentOfSection(String markup, String sourceSyntax, String sectionIndex) throws Exception;

  /**
   * Update the content of the selected section
   *
   * @param markup The markup of the page
   * @param sourceSyntax The syntax used by the wiki page
   * @param sectionIndex The index of the section
   * @param newSectionContent The new content of the section
   * @return The content as string
   * @throws Exception
   */
  public String updateContentOfSection(String markup, String sourceSyntax, String sectionIndex, String newSectionContent) throws Exception;

  /**
   * Parse the markup of the page based on the syntax
   *
   * @param markup The markup to parse
   * @param sourceSyntax The syntax to use
   * @return The XDOM to display
   * @throws Exception
   */
  public XDOM parse(String markup, String sourceSyntax) throws Exception;

    /**
     * Get the CSS url
     *
     * @return the CSS url
     */
  public String getCssURL();

    /**
     * Set the CSS url
     *
     * @param cssURL CSS url to set
     */
  public void setCssURL(String cssURL);

}
