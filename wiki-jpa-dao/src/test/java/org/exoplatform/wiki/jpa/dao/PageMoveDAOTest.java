/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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
package org.exoplatform.wiki.jpa.dao;

import org.exoplatform.wiki.jpa.BaseWikiJPAIntegrationTest;
import org.exoplatform.wiki.jpa.entity.*;
import org.exoplatform.wiki.mow.api.PermissionType;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 25, 2015  
 */
public class PageMoveDAOTest extends BaseWikiJPAIntegrationTest {

  public void testInsertDelete() throws IOException, URISyntaxException {
    //Given
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);

    PageEntity page = new PageEntity();
    page.setWiki(wiki);
    page.setParentPage(null);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setName("page1");
    page.setTitle("Page 1");

    pageDAO.create(page);

    PageMoveEntity pageMoveEntity1 = new PageMoveEntity();
    pageMoveEntity1.setPage(page);
    pageMoveEntity1.setWikiType("portal");
    pageMoveEntity1.setWikiOwner("wiki1");
    pageMoveEntity1.setPageName("previousName1");
    pageMoveEntity1.setCreatedDate(Calendar.getInstance().getTime());
    pageMoveDAO.create(pageMoveEntity1);

    PageMoveEntity pageMoveEntity2 = new PageMoveEntity();
    pageMoveEntity2.setPage(page);
    pageMoveEntity2.setWikiType("portal");
    pageMoveEntity2.setWikiOwner("wiki1");
    pageMoveEntity2.setPageName("previousName2");
    pageMoveEntity2.setCreatedDate(Calendar.getInstance().getTime());
    pageMoveDAO.create(pageMoveEntity2);

    //When
    List<PageMoveEntity> pageMoves1 = pageMoveDAO.findInPageMoves("portal", "wiki1", "previousName1");
    List<PageMoveEntity> pageMoves2 = pageMoveDAO.findInPageMoves("portal", "wiki1", "previousName2");
    List<PageMoveEntity> pageMoves3 = pageMoveDAO.findInPageMoves("portal", "wiki1", "previousName3");

    //Then
    assertNotNull(pageMoves1);
    assertEquals(1, pageMoves1.size());
    assertEquals("page1", pageMoves1.get(0).getPage().getName());
    assertNotNull(pageMoves2);
    assertEquals(1, pageMoves2.size());
    assertEquals("page1", pageMoves2.get(0).getPage().getName());
    assertNotNull(pageMoves3);
    assertEquals(0, pageMoves3.size());
  }

}
