/*
 *
 *  * Copyright (C) 2003-2015 eXo Platform SAS.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 */

package org.exoplatform.wiki.jpa.dao;


import org.exoplatform.wiki.jpa.BaseWikiJPAIntegrationTest;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.PageVersionEntity;
import org.exoplatform.wiki.jpa.entity.WikiEntity;
import org.junit.Test;

import java.util.Date;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 7/31/15
 */
public class PageVersionDAOTest extends BaseWikiJPAIntegrationTest {

  @Test
  public void testLastversionNumberOfPage() {
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);

    PageEntity parentPage = new PageEntity();
    parentPage.setWiki(wiki);
    parentPage.setName("parentPage1");
    parentPage.setCreatedDate(new Date());
    parentPage.setUpdatedDate(new Date());

    PageEntity page = new PageEntity();
    page.setWiki(wiki);
    page.setParentPage(parentPage);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setName("page1");
    page.setTitle("Page 1");

    pageDAO.create(parentPage);
    pageDAO.create(page);

    PageVersionEntity pageVersion1 = new PageVersionEntity();
    pageVersion1.setPage(page);
    pageVersion1.setName("1");
    pageVersion1.setCreatedDate(new Date());
    pageVersion1.setUpdatedDate(new Date());
    pageVersion1.setVersionNumber(1);
    pageVersionDAO.create(pageVersion1);

    PageVersionEntity pageVersion2 = new PageVersionEntity();
    pageVersion2.setPage(page);
    pageVersion2.setName("2");
    pageVersion2.setCreatedDate(new Date());
    pageVersion2.setUpdatedDate(new Date());
    pageVersion2.setVersionNumber(2);
    pageVersionDAO.create(pageVersion2);

    assertEquals(2, pageVersionDAO.getLastversionNumberOfPage(page.getId()).longValue());
  }

  @Test
  public void testPageversionByPageIdAndVersion() {
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);

    PageEntity parentPage = new PageEntity();
    parentPage.setWiki(wiki);
    parentPage.setName("parentPage1");
    parentPage.setCreatedDate(new Date());
    parentPage.setUpdatedDate(new Date());

    PageEntity page = new PageEntity();
    page.setWiki(wiki);
    page.setParentPage(parentPage);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setName("page1");
    page.setTitle("Page 1");

    pageDAO.create(parentPage);
    pageDAO.create(page);

    PageVersionEntity pageVersion1 = new PageVersionEntity();
    pageVersion1.setPage(page);
    pageVersion1.setName("1");
    pageVersion1.setCreatedDate(new Date());
    pageVersion1.setUpdatedDate(new Date());
    pageVersion1.setVersionNumber(1);
    pageVersion1.setContent("Content Version 1");
    pageVersionDAO.create(pageVersion1);

    PageVersionEntity pageVersion2 = new PageVersionEntity();
    pageVersion2.setPage(page);
    pageVersion2.setName("2");
    pageVersion2.setCreatedDate(new Date());
    pageVersion2.setUpdatedDate(new Date());
    pageVersion2.setVersionNumber(2);
    pageVersion2.setContent("Content Version 2");
    pageVersionDAO.create(pageVersion2);

    PageVersionEntity fetchedPageVersion1 = pageVersionDAO.getPageversionByPageIdAndVersion(page.getId(), 1L);
    PageVersionEntity fetchedPageVersion2 = pageVersionDAO.getPageversionByPageIdAndVersion(page.getId(), 2L);

    assertNotNull(fetchedPageVersion1);
    assertEquals("Content Version 1", fetchedPageVersion1.getContent());
    assertNotNull(fetchedPageVersion2);
    assertEquals("Content Version 2", fetchedPageVersion2.getContent());
  }

}
