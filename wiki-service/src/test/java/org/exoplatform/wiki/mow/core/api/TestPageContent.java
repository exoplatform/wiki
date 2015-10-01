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
package org.exoplatform.wiki.mow.core.api;

import org.exoplatform.wiki.mow.api.Attachment;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.service.WikiService;


public class TestPageContent extends AbstractMOWTestcase {

  private WikiService wikiService;

  public void setUp() throws Exception {
    super.setUp();
    wikiService = container.getComponentInstanceOfType(WikiService.class);
  }

  public void testGetPageContent() throws Exception {
    Wiki wiki = wikiService.createWiki(WikiType.PORTAL.toString(), "classic");
    Page page = new Page("AddPageContent-001", "AddPageContent-001");
    page.setSyntax("xwiki_2.0");
    page.getContent().setText("This is a content of page");
    wikiService.createPage(wiki, "WikiHome", page);

    page = wikiService.getPageOfWikiByName(wiki.getType(), wiki.getOwner(), "AddPageContent-001");
    assertNotNull(page);
    assertEquals("xwiki_2.0", page.getSyntax());
    assertEquals("This is a content of page", page.getContent().getText());
  }

  public void testUpdatePageContent() throws Exception {
    Wiki wiki = wikiService.createWiki(WikiType.PORTAL.toString(), "classic");
    Page page = new Page("UpdatePageContent-001", "UpdatePageContent-001");
    page.setSyntax("xwiki_2.0");
    Attachment content = new Attachment();
    content.setText("This is a content of page");
    page.setContent(content);
    wikiService.createPage(wiki, "WikiHome", page);

    content.setText("This is a content of page - edited");
    page.setSyntax("xwiki_2.1");
    wikiService.updatePage(page);

    page = wikiService.getPageOfWikiByName(wiki.getType(), wiki.getOwner(), "UpdatePageContent-001");
    assertNotNull(page);
    assertEquals(page.getSyntax(), "xwiki_2.1");
    assertEquals(page.getContent().getText(), "This is a content of page - edited");
  }

}
