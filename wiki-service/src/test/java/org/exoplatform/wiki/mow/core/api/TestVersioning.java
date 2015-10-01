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

import java.util.Iterator;
import java.util.List;

import org.exoplatform.wiki.chromattic.ext.ntdef.NTFrozenNode;
import org.exoplatform.wiki.chromattic.ext.ntdef.NTVersion;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.mow.core.api.wiki.WikiNodeType;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.service.WikiService;

public class TestVersioning extends AbstractMOWTestcase {

  private WikiService wikiService;

  public void setUp() throws Exception {
    super.setUp();
    wikiService = container.getComponentInstanceOfType(WikiService.class);
  }

  public void testGetVersionHistory() throws Exception {
    Wiki wiki = wikiService.createWiki(WikiType.PORTAL.toString(), "versioning");
    Page page = new Page("testGetVersionHistory-001", "testGetVersionHistory-001");
    page = wikiService.createPage(wiki, "WikiHome", page);
    wikiService.createVersionOfPage(page);

    page = wikiService.getPageOfWikiByName(wiki.getType(), wiki.getOwner(), "testGetVersionHistory-001");
    assertNotNull(page);
    List<PageVersion> versions = wikiService.getVersionsOfPage(page);
    assertNotNull(versions);
    assertEquals(2, versions.size());
  }

  public void testCreateVersionHistoryTree() throws Exception {
    Wiki wiki = wikiService.createWiki(WikiType.PORTAL.toString(), "versioning");
    Page page = new Page("testCreateVersionHistoryTree-001", "testCreateVersionHistoryTree-001");
    page = wikiService.createPage(wiki, "WikiHome", page);

    page.setTitle("testCreateVersionHistoryTree");
    page.getContent().setText("testCreateVersionHistoryTree-ver1.0");
    wikiService.createVersionOfPage(page);

    page.getContent().setText("testCreateVersionHistoryTree-ver2.0");
    wikiService.createVersionOfPage(page);

    List<PageVersion> versions = wikiService.getVersionsOfPage(page);
    assertNotNull(versions);
    assertEquals(3, versions.size());

    // TODO
    /*
    wikipage.restore(ver1.getName(), false);
    assertEquals("testCreateVersionHistoryTree-ver1.0", wikipage.getContent().getText());
    wikipage.checkout();
    wikipage.getContent().setText("testCreateVersionHistoryTree-ver3.0");
    NTVersion ver3 = wikipage.checkin();
    wikipage.checkout();
    Iterator<NTVersion> iter = wikipage.getVersionableMixin().getVersionHistory().iterator();
    NTVersion version = iter.next();
    assertEquals(WikiNodeType.Definition.ROOT_VERSION, version.getName());
    version = iter.next();
    NTFrozenNode frozenNode = version.getNTFrozenNode();
    assertEquals("testCreateVersionHistoryTree-ver1.0",
                 frozenNode.getContentString());
    version = iter.next();
    frozenNode = version.getNTFrozenNode();
    assertEquals("testCreateVersionHistoryTree-ver2.0",
                 frozenNode.getContentString());
    version = iter.next();
    frozenNode = version.getNTFrozenNode();
    assertEquals("testCreateVersionHistoryTree-ver3.0",
                 frozenNode.getContentString());
    */
  }
}
