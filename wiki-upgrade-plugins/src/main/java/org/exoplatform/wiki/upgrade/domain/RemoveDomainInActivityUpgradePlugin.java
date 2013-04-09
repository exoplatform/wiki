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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.chromattic.api.ChromatticSession;
import org.chromattic.api.query.QueryResult;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.common.lifecycle.SocialChromatticLifeCycle;
import org.exoplatform.social.core.chromattic.entity.ActivityParameters;

public class RemoveDomainInActivityUpgradePlugin extends UpgradeProductPlugin {
  public static final String URL_KEY = "page_url";
  private static final Log LOG = ExoLogger.getLogger(RemoveDomainInActivityUpgradePlugin.class);

  public RemoveDomainInActivityUpgradePlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    LOG.info("\n\nStart check wiki activities...\n");
    try {
      removeDomainNameInActivityParams();
    } catch (Exception e) {
      LOG.warn("[WikiRemoveDomainNameInURLPlugin] Exception when fix null url for wiki page:", e);
    }
    LOG.info("\n\nFinish ...\n");
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return VersionComparator.isAfter(newVersion, previousVersion);
  }

  private void removeDomainNameInActivityParams() {
    LOG.info("removing hostname of page url in activity param ....");
    RequestLifeCycle.begin(PortalContainer.getInstance());
    ChromatticSession session = getSocialChromatticLifeCycle().getSession();
    QueryResult<ActivityParameters> activityParamIterator = session.createQueryBuilder(ActivityParameters.class).where("page_url IS NOT NULL").get().objects();

    int count = 0;
    int nbOfParam = activityParamIterator.size();
    while (activityParamIterator.hasNext()) {
      ActivityParameters param = activityParamIterator.next();
      Map<String, String> templateParams = param.getParams();
      String oldActivityURL = templateParams.get(URL_KEY);
      LOG.info("Old wiki activity url:", oldActivityURL);
      if (oldActivityURL != null) {
        URL oldURL;
        try {
          oldURL = new URL(oldActivityURL);
          // remove all domain name in page url
          String newURL = oldURL.getPath();
          LOG.info("new wiki activity url:", newURL);
          templateParams.put(URL_KEY, newURL);
          count++;
          LOG.info("\nFixed activity: {}/{}\n", count, nbOfParam);
        } catch (MalformedURLException e) {
          LOG.info("exception when parsing url in removeDomainNameInActivityParams", e);
        }
      }
    }
    RequestLifeCycle.end();
  }

  private SocialChromatticLifeCycle getSocialChromatticLifeCycle() {
    PortalContainer container = PortalContainer.getInstance();
    ChromatticManager manager = (ChromatticManager) container.getComponentInstanceOfType(ChromatticManager.class);
    return (SocialChromatticLifeCycle) manager.getLifeCycle(SocialChromatticLifeCycle.SOCIAL_LIFECYCLE_NAME);
  }
}