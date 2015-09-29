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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;


import org.chromattic.ext.ntdef.Resource;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.core.api.wiki.Model;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.wiki.AttachmentImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PortalWiki;
import org.exoplatform.wiki.mow.core.api.wiki.WikiContainer;
import org.exoplatform.wiki.mow.core.api.wiki.WikiHome;
import org.exoplatform.wiki.service.WikiService;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

// TODO :
// * Fix tests to not have to specify the order of execution like this
// * The order of tests execution changed in Junit 4.11 (https://github.com/KentBeck/junit/blob/master/doc/ReleaseNotes4.11.md)
@FixMethodOrder(MethodSorters.JVM)
public class TestPageAttachment extends AbstractMOWTestcase {

  // TODO remove
  public void testDummy() {
    assertTrue(true);
  }

  // TODO ???
  /*
  public void testAddPageAttachment() throws Exception {
    Model model = mowService.getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
    PortalWiki wiki = portalWikiContainer.addWiki("classic");
    PageImpl wikiHomePage = (PageImpl) wiki.getWikiHome();
    
    AttachmentImpl attachment0 = addAttachment(wikiHomePage, "attachment0.jpg", "logo", "root");   
    assertEquals(attachment0.getName(), "attachment0.jpg");
    assertNotNull(attachment0.getContentResource());
    attachment0.setContentResource(Resource.createPlainText("logo - Updated"));
    
    PageImpl wikipage = addWikiPage(wiki, "testAddPageAttachment");
    wikiHomePage.addWikiPage(wikipage);
    wikipage.makeVersionable();
    
    AttachmentImpl attachment1 = addAttachment(wikipage, "attachment1.jpg", "foo", "you");
    assertEquals(attachment1.getName(), "attachment1.jpg");
    assertNotNull(attachment1.getContentResource());
    
    AttachmentImpl attachment2 = addAttachment(wikipage, "attachment2.jpg", "bar", "me");    
    assertEquals(attachment2.getName(), "attachment2.jpg");
    assertNotNull(attachment2.getContentResource());
  }
  
  public void testAttachmentPermission() throws Exception {
    startSessionAs("demo");
    Model model = mowService.getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
    PortalWiki wiki = portalWikiContainer.addWiki("classic");
    WikiHome wikiHomePage = wiki.getWikiHome();

    // Create permission entries
    HashMap<String, String[]> expectedPermissions = new HashMap<String, String[]>();
    String[] permission = new String[] { org.exoplatform.services.jcr.access.PermissionType.READ,
        org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY };
    expectedPermissions.put("demo", permission);

    // Create new wiki page
    PageImpl wikipage = addWikiPage(wiki, "testAttachmentPermissionPage");
    wikiHomePage.addWikiPage(wikipage);
    wikipage.setOwner("demo");
    wikipage.setPermission(expectedPermissions);

    // Create attachment
    AttachmentImpl attachment0 = wikipage.createAttachment("AttachmentPermission.jpg", Resource.createPlainText("logo"));
    attachment0.setCreator("demo");
    AttachmentImpl attachment1 = wikipage.getAttachment("AttachmentPermission.jpg");
    assertNotNull(attachment1);

    // Check if permission is correct
    expectedPermissions.put("demo", org.exoplatform.services.jcr.access.PermissionType.ALL);
    expectedPermissions.put(IdentityConstants.ANY, new String[] {
            org.exoplatform.services.jcr.access.PermissionType.READ});
    HashMap<String, String[]> altualPermissions = attachment1.getPermission();
    for (String key : altualPermissions.keySet()) {
      String[] expectPermission = expectedPermissions.get(key);
      String[] actualPermission = altualPermissions.get(key);
      for (int i = 0; i < actualPermission.length; i++) {
        assertEquals(expectPermission[i], actualPermission[i]);
      }
    }
  }
  
  public void testGetPageAttachment() throws Exception{
	  
    //Init data
    Model model = mowService.getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
    PortalWiki wiki = portalWikiContainer.addWiki("acme");
    Page wikiHomePage = wiki.getWikiHome();
    
    AttachmentImpl attachment0 = addAttachment(wikiHomePage, "attachment0.jpg", "logo", "root");
    attachment0.setContentResource(Resource.createPlainText("logo - Updated"));
    
    PageImpl wikiPage = addWikiPage(wiki, "testGetPageAttachment1");
    wikiHomePage.addWikiPage(wikiPage);
    wikiPage.makeVersionable();
    
    AttachmentImpl attachment1 = addAttachment(wikiPage, "attachment1.jpg", "foo", "you");
    attachment1.setContentResource(Resource.createPlainText("foo - Updated"));
    
    AttachmentImpl attachment2 = addAttachment(wikiPage, "attachment2.jpg", "bar", "me");
    attachment2.setContentResource(Resource.createPlainText("bar - Updated"));

    wikiPage = addWikiPage(wiki, "testGetPageAttachment2");
    wikiHomePage.addWikiPage(wikiPage);
    wikiPage.makeVersionable();
	  
    WikiService wService = (WikiService)container.getComponentInstanceOfType(WikiService.class) ;
    Page wikipage = wService.getPageOfWikiByName("portal", "acme", "WikiHome") ;
    Collection<AttachmentImpl> attachments = wikipage.getAttachmentsExcludeContent() ;
    assertEquals(attachments.size(), 1) ;
    Iterator<AttachmentImpl> iter = attachments.iterator() ;
    AttachmentImpl att0 = iter.next() ;
    assertNotNull(att0.getContentResource()) ;
    assertEquals(new String(att0.getContentResource().getData()), "logo - Updated") ;
    assertEquals(att0.getWeightInBytes(), "logo - Updated".getBytes().length) ;
    assertEquals(att0.getCreator(), "root") ;
    
    wikipage = wService.getPageOfWikiByName("portal", "acme", "testGetPageAttachment1") ;
    attachments = wikipage.getAttachmentsExcludeContent() ;
    assertEquals(attachments.size(), 2) ;
    iter = attachments.iterator() ;
    AttachmentImpl att1 = iter.next() ;
    assertNotNull(att1.getContentResource()) ;
    assertEquals(new String(att1.getContentResource().getData()), "foo - Updated") ;
    assertEquals(att1.getWeightInBytes(), "foo - Updated".getBytes().length) ;
    assertEquals(att1.getCreator(), "you") ;
    
    AttachmentImpl att2 = iter.next() ;
    assertNotNull(att2.getContentResource()) ;
    assertEquals(new String(att2.getContentResource().getData()), "bar - Updated") ;
    assertEquals(att2.getWeightInBytes(), "bar - Updated".getBytes().length) ;
    assertEquals(att2.getCreator(), "me") ;
    
    //Add new attachment for page that still don't have any attachment
    wikipage = wService.getPageOfWikiByName("portal", "acme", "testGetPageAttachment2");
    AttachmentImpl att = addAttachment(wikipage, "attachment3.jpg", "attachment3", "me");
    assertEquals(att.getName(), "attachment3.jpg") ;
    assertNotNull(att.getContentResource()) ;
  }
  
  public void testGetNewPageAttachment() throws Exception{
	  
    //Init data
    Model model = mowService.getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
    PortalWiki wiki = portalWikiContainer.addWiki("ecms");
    PageImpl wikiHomePage = (PageImpl) wiki.getWikiHome();
    
    PageImpl wikiPage = addWikiPage(wiki, "testGetNewPageAttachment");
    wikiHomePage.addWikiPage(wikiPage);
    wikiPage.makeVersionable();
    
    AttachmentImpl attachment = addAttachment(wikiPage, "attachment3.jpg", "attachment3", "me");
    attachment.setContentResource(Resource.createPlainText("attachment3 - Updated"));
	  
    WikiService wService = (WikiService)container.getComponentInstanceOfType(WikiService.class) ;
    PageImpl wikipage = (PageImpl)wService.getPageOfWikiByName("portal", "ecms", "testGetNewPageAttachment") ;
    Collection<AttachmentImpl> attachments = wikipage.getAttachmentsExcludeContent() ;
    assertEquals(attachments.size(), 1) ;
    Iterator<AttachmentImpl> iter = attachments.iterator() ;
    AttachmentImpl att = iter.next();
    assertNotNull(att.getContentResource()) ;
    assertEquals(new String(att.getContentResource().getData()), "attachment3 - Updated") ;
    assertEquals(att.getWeightInBytes(), "attachment3 - Updated".getBytes().length) ;
    assertEquals(att.getCreator(), "me") ;
  }
  
  private PageImpl addWikiPage(PortalWiki wiki, String pageName){
    PageImpl wikipage = wiki.createWikiPage();
    wikipage.setName(pageName);
    return wikipage;
  }
  
  private AttachmentImpl addAttachment(Page wikiPage, String filename, String plainText, String creator) throws Exception{
	AttachmentImpl attachment = wikiPage.createAttachment(filename, Resource.createPlainText(plainText));
	attachment.setCreator(creator);
	return attachment;
  }
  */
  
}
