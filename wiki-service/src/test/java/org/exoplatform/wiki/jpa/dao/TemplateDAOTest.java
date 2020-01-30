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


import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import org.exoplatform.wiki.jpa.BaseWikiJPAIntegrationTest;
import org.exoplatform.wiki.jpa.entity.TemplateEntity;
import org.exoplatform.wiki.jpa.entity.WikiEntity;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 7/31/15
 */
public class TemplateDAOTest extends BaseWikiJPAIntegrationTest {

  @Test
  public void testGetTemplateOfWikiByName() {
    //Given
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);

    TemplateEntity template = new TemplateEntity();
    template.setWiki(wiki);
    template.setName("template1");
    template.setTitle("Template 1");
    template.setContent("Template 1 Content");
    Date now = Calendar.getInstance().getTime();
    template.setCreatedDate(now);
    template.setUpdatedDate(now);
    templateDAO.create(template);

    //When
    TemplateEntity fetchedTemplate1 = templateDAO.getTemplateOfWikiByName("portal", "wiki1", "template1");
    TemplateEntity fetchedTemplate2 = templateDAO.getTemplateOfWikiByName("portal", "wiki1", "template2");
    TemplateEntity fetchedTemplate1OfWiki2 = templateDAO.getTemplateOfWikiByName("portal", "wiki2", "template1");

    //Then
    assertEquals(1, templateDAO.findAll().size());
    assertNotNull(fetchedTemplate1);
    assertEquals("portal", fetchedTemplate1.getWiki().getType());
    assertEquals("wiki1", fetchedTemplate1.getWiki().getOwner());
    assertEquals("template1", fetchedTemplate1.getName());
    assertEquals("Template 1", fetchedTemplate1.getTitle());
    assertEquals("Template 1 Content", fetchedTemplate1.getContent());
    assertNull(fetchedTemplate2);
    assertNull(fetchedTemplate1OfWiki2);
  }


  @Test
  public void testGetTemplatesOfWiki() {
    //Given
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);

    Date now = Calendar.getInstance().getTime();

    TemplateEntity template1 = new TemplateEntity();
    template1.setWiki(wiki);
    template1.setName("template1");
    template1.setTitle("Template 1");
    template1.setContent("Template 1 Content");
    template1.setCreatedDate(now);
    template1.setUpdatedDate(now);
    templateDAO.create(template1);

    TemplateEntity template2 = new TemplateEntity();
    template2.setWiki(wiki);
    template2.setName("template2");
    template2.setTitle("Template 2");
    template2.setContent("Template 2 Content");
    template2.setCreatedDate(now);
    template2.setUpdatedDate(now);
    templateDAO.create(template2);

    //When
    List<TemplateEntity> templatesWiki1 = templateDAO.getTemplatesOfWiki("portal", "wiki1");
    List<TemplateEntity> templatesWiki2 = templateDAO.getTemplatesOfWiki("portal", "wiki2");

    //Then
    assertEquals(2, templateDAO.findAll().size());
    assertNotNull(templatesWiki1);
    assertEquals(2, templatesWiki1.size());
    assertNotNull(templatesWiki2);
    assertEquals(0, templatesWiki2.size());
  }

  @Test
  public void testSearchTemplates() {
    //Given
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);

    Date now = Calendar.getInstance().getTime();

    TemplateEntity template1 = new TemplateEntity();
    template1.setWiki(wiki);
    template1.setName("template1");
    template1.setTitle("Template with Title 1");
    template1.setContent("Template 1 Content");
    template1.setCreatedDate(now);
    template1.setUpdatedDate(now);
    templateDAO.create(template1);

    TemplateEntity template2 = new TemplateEntity();
    template2.setWiki(wiki);
    template2.setName("template2");
    template2.setTitle("Template with Title 2");
    template2.setContent("Template 2 Content");
    template2.setCreatedDate(now);
    template2.setUpdatedDate(now);
    templateDAO.create(template2);

    //When
    List<TemplateEntity> templatesWiki1 = templateDAO.searchTemplatesByTitle("portal", "wiki1", "with Title");
    List<TemplateEntity> templatesWiki2 = templateDAO.searchTemplatesByTitle("portal", "wiki1", "Title 1");
    List<TemplateEntity> templatesWiki3 = templateDAO.searchTemplatesByTitle("portal", "wiki1", "No Result");

    //Then
    assertEquals(2, templateDAO.findAll().size());
    assertNotNull(templatesWiki1);
    assertEquals(2, templatesWiki1.size());
    assertNotNull(templatesWiki2);
    assertEquals(1, templatesWiki2.size());
    assertNotNull(templatesWiki3);
    assertEquals(0, templatesWiki3.size());
  }

}
