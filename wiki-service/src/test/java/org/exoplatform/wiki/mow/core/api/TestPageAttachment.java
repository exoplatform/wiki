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

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.service.IDType;
import org.exoplatform.wiki.service.WikiService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestPageAttachment extends AbstractMOWTestcase {

  private WikiService wikiService;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    wikiService = container.getComponentInstanceOfType(WikiService.class);
  }

  public void testAddPageAttachment() throws Exception {
    Wiki wiki = wikiService.createWiki(PortalConfig.PORTAL_TYPE, "wikiAttachement1");
    Page wikiHome = wiki.getWikiHome();

    Attachment attachment = new Attachment();
    attachment.setName("attachment1.png");
    attachment.setContent("logo".getBytes());
    attachment.setMimeType("image/png");
    attachment.setCreator("root");
    wikiService.addAttachmentToPage(attachment, wikiHome);

    Attachment storedAttachment = wikiService.getAttachmentOfPageByName("attachment1.png", wikiHome, true);

    assertNotNull(storedAttachment);
    assertNotNull(storedAttachment.getName());
    assertEquals(attachment.getName(), storedAttachment.getName());
    assertNotNull(storedAttachment.getContent());
    assertTrue(Arrays.equals(attachment.getContent(), storedAttachment.getContent()));
  }

  public void testAttachmentPermission() throws Exception {
    startSessionAs("demo");

    Wiki wiki = wikiService.createWiki(PortalConfig.PORTAL_TYPE, "wikiAttachement2");
    Page wikiHome = wiki.getWikiHome();

    List<PermissionEntry> expectedPermissions = new ArrayList<>();
    // Create new wiki page
    Page page = new Page("testAttachmentPermissionPage", "testAttachmentPermissionPage");
    page.setOwner("demo");
    page = wikiService.createPage(wiki, wikiHome.getName(), page);
    expectedPermissions.addAll(page.getPermissions());
    // Create attachment
    Attachment attachment1 = new Attachment();
    attachment1.setName("AttachmentPermission.jpg");
    attachment1.setContent("logo".getBytes());
    attachment1.setMimeType("image/png");
    attachment1.setCreator("demo");
    wikiService.addAttachmentToPage(attachment1, page);

    attachment1 = wikiService.getAttachmentOfPageByName("AttachmentPermission.jpg", page, true);
    
    List<PermissionEntry> actualPermissions = attachment1.getPermissions();
    assertNotNull(actualPermissions);
    assertEquals(expectedPermissions.size(), actualPermissions.size());
  }

  public void testGetPageAttachment() throws Exception{
    Wiki wiki = wikiService.createWiki(PortalConfig.PORTAL_TYPE, "wikiAttachement3");
    Page wikiHome = wiki.getWikiHome();

    Attachment attachment1 = new Attachment();
    attachment1.setName("attachment1.png");
    attachment1.setContent("logo".getBytes());
    attachment1.setMimeType("image/png");
    attachment1.setCreator("root");
    wikiService.addAttachmentToPage(attachment1, wikiHome);

    Page page1 = wikiService.createPage(wiki, wikiHome.getName(), new Page("testGetPageAttachment1", "testGetPageAttachment1"));

    Attachment attachment2 = new Attachment();
    attachment2.setName("attachment2.png");
    attachment2.setContent("foo - Updated".getBytes());
    attachment2.setMimeType("image/png");
    attachment2.setCreator("you");
    wikiService.addAttachmentToPage(attachment2, page1);

    Attachment attachment3 = new Attachment();
    attachment3.setName("attachment3.png");
    attachment3.setContent("bar - Updated".getBytes());
    attachment3.setMimeType("image/png");
    attachment3.setCreator("me");
    wikiService.addAttachmentToPage(attachment3, page1);
	  
    Page page = wikiService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "wikiAttachement3", "WikiHome");
    List<Attachment> attachments = wikiService.getAttachmentsOfPage(page);
    assertNotNull(attachments);
    assertEquals(attachments.size(), 1);
    Attachment att0 = attachments.get(0);
    assertNotNull(att0.getContent());
    assertTrue(Arrays.equals(att0.getContent(), "logo".getBytes()));
    assertEquals(att0.getWeightInBytes(), "logo".getBytes().length);
    assertEquals(att0.getCreator(), "root");

    page = wikiService.getPageOfWikiByName(PortalConfig.PORTAL_TYPE, "wikiAttachement3", "testGetPageAttachment1");
    attachments = wikiService.getAttachmentsOfPage(page);
    assertEquals(attachments.size(), 2);
    Attachment att1 = attachments.get(0);
    assertNotNull(att1.getContent());
    assertTrue(Arrays.equals(att1.getContent(), "foo - Updated".getBytes()));
    assertEquals(att1.getWeightInBytes(), "foo - Updated".getBytes().length) ;
    assertEquals(att1.getCreator(), "you") ;
    
    Attachment att2 = attachments.get(1);
    assertNotNull(att2.getContent()) ;
    assertTrue(Arrays.equals(att2.getContent(), "bar - Updated".getBytes()));
    assertEquals(att2.getWeightInBytes(), "bar - Updated".getBytes().length) ;
    assertEquals(att2.getCreator(), "me") ;
  }
}
