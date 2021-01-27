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

package org.exoplatform.wiki.jpa;
import org.exoplatform.component.test.*;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.*;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.service.WikiService;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 25, 2015  
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/test-configuration.xml"),
})
public abstract class BaseTest extends AbstractKernelTest {

  protected void setUp() throws Exception {
    super.setUp();
    begin();
    Identity systemIdentity = new Identity(IdentityConstants.SYSTEM);
    ConversationState.setCurrent(new ConversationState(systemIdentity));
    System.setProperty("gatein.email.domain.url", "localhost");
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    end();
  }

  protected void startSessionAs(String user) {
    try {
      Authenticator authenticator = getContainer().getComponentInstanceOfType(Authenticator.class);
      Identity userIdentity = authenticator.createIdentity(user);
      ConversationState.setCurrent(new ConversationState(userIdentity));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
  public <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }

  protected Wiki getOrCreateWiki(WikiService wikiService, String type, String owner) throws WikiException {
    Wiki wiki = wikiService.getWikiByTypeAndOwner(type, owner);
    if (wiki == null) {
      return wikiService.createWiki(type, owner);
    }
    return wiki;
  }
}
