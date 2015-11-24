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
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.core.api.wiki.*;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.listener.PageWikiListener;


public class TestWikiPage extends AbstractMOWTestcase {

  private WikiService wService;

  public void setUp() throws Exception {
    super.setUp();
    wService = container.getComponentInstanceOfType(WikiService.class) ;
  }

  public void testAddWikiHome() throws WikiException {
    boolean created = mowService.startSynchronization();
    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
    PortalWiki wiki = portalWikiContainer.addWiki(new Wiki(PortalConfig.PORTAL_TYPE, "classic"));
    WikiHome wikiHomePage = wiki.getWikiHome();
    assertNotNull(wikiHomePage) ;
    mowService.stopSynchronization(created);
  }

  public void testAddWikiPage() throws WikiException {
    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
    PortalWiki wiki = portalWikiContainer.addWiki(new Wiki(PortalConfig.PORTAL_TYPE, "classic"));
    WikiHome wikiHomePage = wiki.getWikiHome();
    
    PageImpl wikipage = wiki.createWikiPage();
    wikipage.setName("AddWikiPage");
    
    wikiHomePage.addWikiPage(wikipage);
    assertSame(wikipage, wikiHomePage.getChildPages().get(wikipage.getName()));
  }
  
  public void testGetWikiPageById() throws Exception {
    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
    PortalWiki wiki = portalWikiContainer.addWiki(new Wiki(PortalConfig.PORTAL_TYPE, "classic"));
    WikiHome wikiHomePage = wiki.getWikiHome();
    
    PageImpl wikipage = wikiHomePage.getWikiPage("CreateWikiPage-001");
    if(wikipage == null) {
      wikipage = wiki.createWikiPage();
      wikipage.setName("CreateWikiPage-001");
      wikiHomePage.addWikiPage(wikipage);
    }
    
    assertNotNull(wikiHomePage.getWikiPage("CreateWikiPage-001")) ;
    
    PageImpl subpage = wiki.createWikiPage();
    subpage.setName("SubWikiPage-001") ;
    wikipage.addWikiPage(subpage) ;
        
    assertNotNull(wikipage.getWikiPage("SubWikiPage-001")) ;

    mowService.persist();
    
    WikiService wService = container.getComponentInstanceOfType(WikiService.class) ;
    Page page = wService.getPageOfWikiByName("portal", "classic", "SubWikiPage-001") ;
    assertNotNull(page) ;
  }
  
  public void testUpdateWikiPage() throws Exception {
    Wiki wiki = wService.createWiki(PortalConfig.PORTAL_TYPE, "intranet");
    Page page = new Page("UpdateWikiPage-001", "UpdateWikiPage-001");
    page.setOwner("Root");
    wService.createPage(wiki, "WikiHome", page);

    page.setOwner("Demo");
    page.setAuthor("Demo");
    wService.updatePage(page, null);
    
    Page updatedPage = wService.getPageOfWikiByName(wiki.getType(), wiki.getOwner(), "UpdateWikiPage-001") ;
    assertNotNull(updatedPage) ;
    assertEquals("Demo", updatedPage.getOwner()) ;
    assertEquals("Demo", updatedPage.getAuthor()) ;
    assertNotNull(updatedPage.getUpdatedDate()) ;
  }
  
  public void testDeleteWikiPage() throws Exception {
    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
    PortalWiki wiki = portalWikiContainer.addWiki(new Wiki(PortalConfig.PORTAL_TYPE, "classic"));
    WikiHome wikiHomePage = wiki.getWikiHome();
    
    PageImpl wikipage = wiki.createWikiPage();
    wikipage.setName("DeleteWikiPage");
    wikiHomePage.addWikiPage(wikipage);
    PageImpl deletePage = wikiHomePage.getWikiPage("DeleteWikiPage") ;
    assertNotNull(deletePage) ;
    
    deletePage.remove() ;
    assertNull(wikiHomePage.getWikiPage("DeleteWikiPage")) ;    
  }  
  
  public void testGetWiki() throws WikiException {
    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
    PortalWiki wiki = portalWikiContainer.addWiki(new Wiki(PortalConfig.PORTAL_TYPE, "classic"));
    
    WikiHome wikiHomePage = wiki.getWikiHome();
    PageImpl parrentpage = wiki.createWikiPage();
    parrentpage.setName("ParentPage");
    wikiHomePage.addWikiPage(parrentpage);    
    PageImpl childpage = wiki.createWikiPage();
    childpage.setName("ChildPage");
    parrentpage.addWikiPage(childpage);
    WikiImpl childPageWiki = childpage.getWiki();
    
    assertEquals(childPageWiki.getOwner(), "classic");    
  }
}
