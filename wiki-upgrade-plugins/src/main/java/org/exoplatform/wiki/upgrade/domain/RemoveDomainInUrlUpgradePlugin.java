/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.wiki.upgrade.domain;

import java.net.URL;

import org.chromattic.api.ChromatticSession;
import org.chromattic.api.query.QueryResult;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;

public class RemoveDomainInUrlUpgradePlugin extends UpgradeProductPlugin {
  private static final Log LOG = ExoLogger.getLogger(RemoveDomainInUrlUpgradePlugin.class);

  public RemoveDomainInUrlUpgradePlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    LOG.info("\n\nStart check url wiki page...\n");
    try {
      removeDomainNameInURL();
    } catch (Exception e) {
      LOG.warn("[WikiRemoveDomainNameInURLPlugin] Exception when fix null url for wiki page:", e);
    }
    LOG.info("\n\nFinish ...\n");
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return VersionComparator.isAfter(newVersion, previousVersion);
  }
  
  public void removeDomainNameInURL() {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    MOWService mowService = (MOWService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(MOWService.class);
    ChromatticSession session = mowService.getSession();
    
    // Select all the wiki pages
    QueryResult<PageImpl> pageIterator = session.createQueryBuilder(PageImpl.class).where("jcr:path LIKE '/%'").get().objects();
    LOG.info("\nTotal pages found: {}\n", pageIterator.size());
    
    int checkedPage=0;
    while (pageIterator.hasNext()) {
      PageImpl page = pageIterator.next();
      try {
        String oldPageURL = page.getURL();
        LOG.info("\nOld wiki Page URL:", oldPageURL);
        if(oldPageURL !=null){
          URL oldURL = new URL(oldPageURL);
          //remove all domain name in page url
          String newURL = oldURL.getPath();
         page.setURL(newURL);
          LOG.info("\nNew URL:", newURL);
        }
       checkedPage++;
        LOG.info("\nFixed page: {}/{}\n", checkedPage, pageIterator.size());
      } catch (Exception e) {
        LOG.warn(String.format("cannot repair the hostname for page's url %s", page.getName()), e);
      }
    }
    RequestLifeCycle.end();
  }
}