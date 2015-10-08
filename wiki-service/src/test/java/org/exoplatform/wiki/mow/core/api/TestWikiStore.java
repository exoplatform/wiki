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
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.wiki.*;

public class TestWikiStore extends AbstractMOWTestcase {

  public void testGetWikiStore() throws WikiException {
    WikiStore wStore = mowService.getWikiStore();
    assertNotNull(wStore);
  }

  public void testGetWikiContainers() throws WikiException {
    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    WikiContainer<PortalWiki> pwikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
    PortalWikiContainer portalWikiContainer = (PortalWikiContainer) pwikiContainer;
    assertNotNull(portalWikiContainer);
    WikiContainer<GroupWiki> gwikiContainer = wStore.getWikiContainer(WikiType.GROUP);
    GroupWikiContainer groupWikiContainer = (GroupWikiContainer) gwikiContainer;
    assertNotNull(groupWikiContainer);
    WikiContainer<UserWiki> uwikiContainer = wStore.getWikiContainer(WikiType.USER);
    UserWikiContainer userWikiContainer = (UserWikiContainer) uwikiContainer;
    assertNotNull(userWikiContainer);
  }

  public void testAddAndGetPortalClassicWiki() throws WikiException {
    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
    PortalWiki wiki = portalWikiContainer.addWiki(new Wiki(PortalConfig.PORTAL_TYPE, "classic"));
    PortalWiki classicWiki = portalWikiContainer.getWiki("classic", true);
    assertSame(wiki, classicWiki);
  }

  public void testAddAndGetAdministratorsGroupWiki() throws WikiException {
    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    WikiContainer<GroupWiki> groupWikiContainer = wStore.getWikiContainer(WikiType.GROUP);
    GroupWiki wiki = groupWikiContainer.addWiki(new Wiki(PortalConfig.GROUP_TYPE, "/platform/administrators"));
    GroupWiki organizationWiki = groupWikiContainer.getWiki("/platform/administrators", true);
    assertSame(wiki, organizationWiki);
  }
  
  public void testAddAndGetDemoUserWiki() throws WikiException {
    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    WikiContainer<UserWiki> userWikiContainer = wStore.getWikiContainer(WikiType.USER);
    UserWiki wiki = userWikiContainer.addWiki(new Wiki(PortalConfig.USER_TYPE, "demo"));
    UserWiki rootWiki = userWikiContainer.getWiki("demo", true);
    assertSame(wiki, rootWiki);
  }
  
  public void testGetPortalClassicWikiHomePage() throws WikiException {
    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
    PortalWiki wiki = portalWikiContainer.addWiki(new Wiki(PortalConfig.PORTAL_TYPE, "classic"));
    PageImpl wikiHomePage = wiki.getWikiHome();
    assertNotNull(wikiHomePage);
  }

}
