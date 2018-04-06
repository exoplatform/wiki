/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

import javax.jcr.Node;

import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.security.*;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.wiki.*;

import junit.framework.TestCase;

/**
 * @version $Revision$
 */
public abstract class AbstractMOWTestcase extends TestCase {

  protected static RepositoryService    repositoryService;

  protected static StandaloneContainer  container;

  protected final static String         WIKI_WS           = "collaboration".intern();

  protected static Node                 root_                  = null;

  protected static MOWService          mowService;

  boolean syncStarted;

  protected void begin() {
    initContainer();

    RequestLifeCycle.begin(container);
  }

  protected void end() {
    RequestLifeCycle.end();

    // TODO stopping or disposing the container does not delete data. We should find a way to do it after each test to make sure tests are really independent.
    //stopContainer();
  }

  protected void setUp() throws Exception {
    begin();
    Identity systemIdentity = new Identity(IdentityConstants.SYSTEM);
    ConversationState.setCurrent(new ConversationState(systemIdentity));
    System.setProperty("gatein.email.domain.url", "localhost");
  }

  protected void tearDown() throws Exception {
    end();
  }

  private void initContainer() {
    if (container != null) {
      return;
    }
    try {
      String containerConf = Thread.currentThread().getContextClassLoader().getResource("conf/standalone/wiki-jcr-configuration.xml").toString();
      StandaloneContainer.addConfigurationURL(containerConf);
      //
      String loginConf = Thread.currentThread().getContextClassLoader().getResource("conf/standalone/login.conf").toString();
      System.setProperty("java.security.auth.login.config", loginConf);
      //System.setProperty("gatein.data.dir", Files.createTempDirectory("wiki-data", null).getFileName().toString());
      //
      container = StandaloneContainer.getInstance();

      mowService = container.getComponentInstanceOfType(MOWService.class);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to initialize standalone container: " + e.getMessage(), e);
    }
  }

  protected WikiImpl getWiki(WikiType wikiType, String wikiName, Model model) throws WikiException {
    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    WikiImpl wiki = null;
    switch (wikiType) {
      case PORTAL:
        WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
        wiki = portalWikiContainer.getWiki(wikiName);
        break;
      case GROUP:
        WikiContainer<GroupWiki> groupWikiContainer = wStore.getWikiContainer(WikiType.GROUP);
        wiki = groupWikiContainer.getWiki(wikiName);
        break;
      case USER:
        WikiContainer<UserWiki> userWikiContainer = wStore.getWikiContainer(WikiType.USER);
        wiki = userWikiContainer.getWiki(wikiName);
        break;
    }
    mowService.persist();
    return wiki;
  }
  
  protected WikiHome getWikiHomeOfWiki(WikiType wikiType, String wikiName, Model model) throws WikiException {
    WikiHome wikiHomePage = getWiki(wikiType, wikiName, model).getWikiHome();
    return wikiHomePage;
  }
  
  protected void startSessionAs(String user) {
    Identity userIdentity = new Identity(user);
    ConversationState.setCurrent(new ConversationState(userIdentity));
  }
  
}
