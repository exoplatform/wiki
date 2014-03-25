/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.wiki.upgrade.wikiname;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.gadget.core.ExoContainerConfig;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.rendering.util.Utils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 24, 2014  
 */
public class WikiNameUpgradePlugin extends UpgradeProductPlugin {
  
  private static final String WIKI_PORTLET = "WikiPortlet";
  private static final String WIKI = "wiki";
  private static final String PORTAL_SYSTEM = "portal-system";
  
  private Log log = ExoLogger.getLogger(this.getClass().getName());
  
  private SpaceService spaceService;
  private RepositoryService repositoryService;
  
  public WikiNameUpgradePlugin(InitParams initParams){
    super(initParams);
  }
  
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("-------------- Starting " + this.getName() + " --------------");
    }
    try {
      RequestLifeCycle.begin(ExoContainerContext.getCurrentContainer());
      this.repositoryService = Utils.getService(RepositoryService.class);
      this.spaceService = Utils.getService(SpaceService.class);
      
      List<String> spaceIds = removeWikiPortletFromSpaces();
      upgradePortletName();
      addWikiPortletToSpaces(spaceIds);
      if (log.isInfoEnabled()) {
        log.info("-------------- " + this.getName() + " finish successfully --------------");
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error when upgrading wiki portlet name.", e);
      }
    } finally {
      RequestLifeCycle.end();
    }
  }
  
  /**
   * Removes wiki portlet from all spaces
   * @return the list of spaces from which wiki portlet is removed
   * @throws Exception 
   * @throws IllegalArgumentException 
   */
  private List<String> removeWikiPortletFromSpaces() throws IllegalArgumentException, Exception {
    ListAccess<Space> spaces = spaceService.getAllSpacesWithListAccess();
    List<String> ret = new ArrayList<String>();
    for (Space space : spaces.load(0, spaces.getSize())) {
      if (space.getApp() != null && space.getApp().contains("Wiki")) {
        spaceService.removeApplication(space.getId(), WIKI_PORTLET, "Wiki");
        ret.add(space.getId());
        if (log.isInfoEnabled()) {
          log.info("-------------- Remove WikiPortlet from space " + space.getPrettyName() + "--------------");
        }
      }
    }
    return ret;
  }
  
  /**
   * Upgrades name: WikiPortlet -> wiki
   * @throws Exception
   */
  private void upgradePortletName() throws Exception {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      Session session = sessionProvider.getSession(PORTAL_SYSTEM, repositoryService.getCurrentRepository());
      //app:WikiPortlet
      Node appWikiPortlet = (Node)session.getItem("/production/app:applications/app:Collaboration/app:WikiPortlet");
      appWikiPortlet.setProperty("exo:name", "app:wiki");
      appWikiPortlet.save();
      session.move(appWikiPortlet.getPath(), "/production/app:applications/app:Collaboration/app:wiki");
      session.save();
      //mop:WikiPortlet
      Node mopWikiPortlet = (Node)session.getItem("/production/mop:workspace/mop:customizations/mop:WikiPortlet");
      mopWikiPortlet.setProperty("mop:contentid", "wiki/wiki");
      mopWikiPortlet.save();
      session.move(mopWikiPortlet.getPath(), "/production/mop:workspace/mop:customizations/mop:wiki");
      session.save();
      // /production/mop:workspace/mop:portalsites/mop:intranet/mop:rootpage/mop:children/
      //  mop:pages/mop:children/mop:wiki/mop:rootcomponent/mop:1aa6c3f1-89a8-4846-9a80-3bd053cb2ba8/
      //  mop:34eabdd0-abc0-41fd-97ab-e332d6aaa5b7/mop:customization
      NodeIterator iter = session.getWorkspace().getQueryManager().createQuery(
        "SELECT * FROM mop:workspaceclone where mop:contentid='wiki/WikiPortlet'", Query.SQL).
      execute().getNodes();
      while (iter.hasNext()) {
        Node current = iter.nextNode();
        current.setProperty("mop:contentid","wiki/wiki");
        current.save();
      }
    } finally {
      sessionProvider.close();
    }
  }
  
  private void addWikiPortletToSpaces(List<String> spaceIds) throws Exception {
    for (String id : spaceIds) {
      spaceService.activateApplication(id, "wiki");
      if (log.isInfoEnabled()) {
        log.info("-------------- Add wiki to space " +
                 spaceService.getSpaceById(id).getPrettyName() + 
                 "--------------");
      }
    }
  }
  
  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return VersionComparator.isAfter(newVersion, previousVersion);
  }
}
