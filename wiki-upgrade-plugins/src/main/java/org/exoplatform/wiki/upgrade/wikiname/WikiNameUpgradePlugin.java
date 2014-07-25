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

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
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
  
  private Log log = ExoLogger.getLogger(this.getClass().getName());
  
  private SpaceService spaceService;
  
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
      this.spaceService = Utils.getService(SpaceService.class);
      
      ListAccess<Space> spaces = spaceService.getAllSpacesWithListAccess();
      for (Space space : spaces.load(0, spaces.getSize())) {
        String apps = space.getApp();
        if (apps != null && apps.contains("WikiPortlet:Wiki:")) {
          space.setApp(apps.replace("WikiPortlet:Wiki:", "WikiPortlet:wiki:"));
          spaceService.updateSpace(space);
          if (log.isInfoEnabled()) {
            log.info("-------------- Rename WikiPortlet from space " + space.getPrettyName() + "--------------");
          }
        }
      }
      
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
  
  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return VersionComparator.isAfter(newVersion, previousVersion);
  }
}