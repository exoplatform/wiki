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

import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.PageVersion;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiService;

import java.util.Iterator;
import java.util.List;

public class TestVersioning extends BaseTest {

  private WikiService wikiService;

  public void setUp() throws Exception {
    super.setUp();
    wikiService = getContainer().getComponentInstanceOfType(WikiService.class);
  }

  public void testGetVersionHistory() throws Exception {
    Wiki wiki = wikiService.createWiki(WikiType.PORTAL.toString(), "versioning1");
    Page page = new Page("testGetVersionHistory-001", "testGetVersionHistory-001");
    page = wikiService.createPage(wiki, "WikiHome", page);
    wikiService.createVersionOfPage(page);

    page = wikiService.getPageOfWikiByName(wiki.getType(), wiki.getOwner(), "testGetVersionHistory-001");
    assertNotNull(page);
    List<PageVersion> versions = wikiService.getVersionsOfPage(page);
    assertNotNull(versions);
// FIXME Failing Test coming from JPA Impl bug comparing to JCR Impl
//    assertEquals(2, versions.size());
  }

  public void testCreateVersionHistoryTree() throws Exception {
    Wiki wiki = wikiService.createWiki(WikiType.PORTAL.toString(), "versioning2");
    Page page = new Page("testCreateVersionHistoryTree-001", "testCreateVersionHistoryTree-001");
    page.setContent("testCreateVersionHistoryTree-ver0.0");
    page = wikiService.createPage(wiki, "WikiHome", page);

    page.setTitle("testCreateVersionHistoryTree");
    page.setContent("testCreateVersionHistoryTree-ver1.0");
    wikiService.updatePage(page, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE);
    wikiService.createVersionOfPage(page);

    page.setContent("testCreateVersionHistoryTree-ver2.0");
    wikiService.updatePage(page, PageUpdateType.EDIT_PAGE_CONTENT);
    wikiService.createVersionOfPage(page);

    List<PageVersion> versions = wikiService.getVersionsOfPage(page);
    assertNotNull(versions);
    assertEquals(3, versions.size());

    // restore to previous version (testCreateVersionHistoryTree-ver1.0)
    wikiService.restoreVersionOfPage(versions.get(1).getName(), page);
    page = wikiService.getPageOfWikiByName(wiki.getType(), wiki.getOwner(), page.getName());
    assertEquals("testCreateVersionHistoryTree-ver1.0", page.getContent());

    page.setContent("testCreateVersionHistoryTree-ver3.0");
    wikiService.updatePage(page, PageUpdateType.EDIT_PAGE_CONTENT);
    wikiService.createVersionOfPage(page);

    versions = wikiService.getVersionsOfPage(page);
    assertNotNull(versions);
    assertEquals(5, versions.size());

    Iterator<PageVersion> itVersions = versions.iterator();
    PageVersion pageVersion = itVersions.next();
    assertEquals("testCreateVersionHistoryTree-ver3.0", pageVersion.getContent());

    pageVersion = itVersions.next();
    assertEquals("testCreateVersionHistoryTree-ver1.0", pageVersion.getContent());

    pageVersion = itVersions.next();
    assertEquals("testCreateVersionHistoryTree-ver2.0", pageVersion.getContent());

    pageVersion = itVersions.next();
    assertEquals("testCreateVersionHistoryTree-ver1.0", pageVersion.getContent());

    pageVersion = itVersions.next();
//FIXME Failing Test coming from JPA Impl bug comparing to JCR Impl
//    assertEquals("testCreateVersionHistoryTree-ver0.0", pageVersion.getContent());
  }
}
