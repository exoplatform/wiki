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
package org.exoplatform.wiki.resolver;


import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.wiki.mock.MockDataStorage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;


public class TestPageResolver extends AbstractResolverTestcase {

  private WikiService wikiService;

  private PageResolver resolver ;
  
  public void setUp() throws Exception{
    super.setUp() ;
    wikiService = getContainer().getComponentInstanceOfType(WikiService.class);
    resolver = getContainer().getComponentInstanceOfType(PageResolver.class);
  }
  
  public void testPageResolver() throws Exception{
    assertNotNull(resolver) ;
  }
  
  public void testExtractParams() throws Exception {
    UserNode usernode = createUserNode(MockDataStorage.PORTAL_CLASSIC__WIKI[0], "wiki");
    WikiPageParams params = resolver.extractWikiPageParams("http://hostname/$CONTAINER/$ACCESS/classic/wiki", SiteKey.portal("classic"), usernode);
    assertNotNull(params);
  }
  
  public void testGetPage() throws Exception{
    getOrCreateWiki(wikiService, PortalConfig.PORTAL_TYPE, "classic");
    UserNode usernode = createUserNode(MockDataStorage.PORTAL_CLASSIC__WIKI[0], "wiki");
    Page page = resolver.resolve("http://hostname/$CONTAINER/$ACCESS/classic/wiki", null, usernode);
    assertNotNull(page) ;

    // Resolve wiki pages on another portal which is specified in user node
    usernode = createUserNode(MockDataStorage.PORTAL_CLASSIC__WIKI[0], "wiki");
    page = resolver.resolve("http://hostname/$CONTAINER/g/:spaces:cca_community_space/cca_community_space/jeeneegrlobalportal", null, usernode);
    assertNotNull(page);
    
    // Resolve wiki pages on a portal which is specified in URL
    usernode = createUserNode(MockDataStorage.SPACE_EXO_WIKI[0], "exo/wiki");
    page = resolver.resolve("http://hostname/$CONTAINER/g/:spaces:exo/exo/wiki/portal/classic", null, usernode);
    assertNotNull(page);

    // Resolve wiki pages on another space
    getOrCreateWiki(wikiService, PortalConfig.GROUP_TYPE, "/platform/users");
    usernode = createUserNode(MockDataStorage.GROUP_USER_WIKI[0], "wiki");
    page = resolver.resolve("http://hostname/$CONTAINER/g/:spaces:cca_community_space/cca_community_space/platformuserspace", null, usernode);
    assertNotNull(page);

    // Resolve wiki pages on another user
    getOrCreateWiki(wikiService, PortalConfig.USER_TYPE, "mary");
    usernode = createUserNode(MockDataStorage.USER_MARY_WIKI[0], "wiki");
    page = resolver.resolve("http://hostname/$CONTAINER/g/:spaces:cca_community_space/cca_community_space/maryspace", null, usernode);
    assertNotNull(page);    
  }
}
