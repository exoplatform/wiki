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
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import junit.framework.TestCase;

import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.core.api.wiki.Model;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.wiki.GroupWiki;
import org.exoplatform.wiki.mow.core.api.wiki.PortalWiki;
import org.exoplatform.wiki.mow.core.api.wiki.UserWiki;
import org.exoplatform.wiki.mow.core.api.wiki.WikiContainer;
import org.exoplatform.wiki.mow.core.api.wiki.WikiHome;
import org.exoplatform.wiki.mow.core.api.wiki.WikiImpl;

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
    try {
      String containerConf = Thread.currentThread().getContextClassLoader().getResource("conf/standalone/configuration.xml").toString();
      StandaloneContainer.addConfigurationURL(containerConf);
      //
      String loginConf = Thread.currentThread().getContextClassLoader().getResource("conf/standalone/login.conf").toString();
      System.setProperty("java.security.auth.login.config", loginConf);
      //System.setProperty("gatein.data.dir", Files.createTempDirectory("wiki-data", null).getFileName().toString());
      //
      container = StandaloneContainer.getInstance();

      mowService = container.getComponentInstanceOfType(MOWService.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize standalone container: " + e.getMessage(), e);
    }
  }

  private void stopContainer() {
    try {
      container = StandaloneContainer.getInstance();
      container.dispose();
    } catch (Exception e) {
      throw new RuntimeException("Failed to stop standalone container: " + e.getMessage(), e);
    }
  }

  private static void initJCR() {
    try {
      repositoryService = (RepositoryService) container.getComponentInstanceOfType(RepositoryService.class);
      // Initialize datas
      Session session = repositoryService.getCurrentRepository().getSystemSession(WIKI_WS);
      root_ = session.getRootNode();
      // Remove old data before to starting test case.
      StringBuffer stringBuffer = new StringBuffer();
      stringBuffer.append("/jcr:root").append("//*[fn:name() = 'eXoWiki' or fn:name() = 'ApplicationData']");
      QueryManager qm = session.getWorkspace().getQueryManager();
      Query query = qm.createQuery(stringBuffer.toString(), Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      while (iter.hasNext()) {
        Node node = iter.nextNode();
        try {
          removeNodes(node);
        } catch (Exception e) {}
      }
      session.save();
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize JCR: ", e);
    }
  }
  
  private static void removeNodes(Node node) throws Exception {
    NodeIterator iter = node.getNodes();
    while (iter.hasNext()) {
      iter.nextNode().remove();
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
