/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.wiki.resolver;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.*;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.user.UserPortalContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.wiki.mock.MockDataStorage;

public class MockUserPortalConfigService extends UserPortalConfigService {

  private static final Log log = ExoLogger.getLogger(MockUserPortalConfigService.class);

  /**
   * @param userACL
   * @param storage
   * @param orgService
   * @param navService
   * @param descriptionService
   * @param params
   * @throws Exception
   */
  public MockUserPortalConfigService(UserACL userACL,
                                     DataStorage storage,
                                     OrganizationService orgService,
                                     NavigationService navService,
                                     DescriptionService descriptionService,
                                     SettingService settingService,
                                     InitParams params) throws Exception {
    super(userACL, storage, orgService, navService, descriptionService, null, settingService, params);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.portal.config.UserPortalConfigService#getPage(java.lang.String, java.lang.String)
   */
  public PageContext getPage(PageKey pageRef) {
  	MockDataStorage mockData = new MockDataStorage();
  	Page page = null;
		try {
			page = mockData.getPage(pageRef.format());
		} catch (Exception e) {
			log.error("Cannot get page " + pageRef.getName() + " - Cause : " + e.getMessage(), e);
		}
  	List<String> accessPermissions = null;
  	if(page.getAccessPermissions() !=null)
  		accessPermissions = Arrays.asList(page.getAccessPermissions());
  	PageState pageState = new PageState(page.getTitle(), page.getDescription(), page.isShowMaxWindow(), page.getFactoryId(),
  	accessPermissions, page.getEditPermission(), Arrays.asList(page.getMoveAppsPermissions()), Arrays.asList(page.getMoveContainersPermissions()));


  	PageContext pageContext = new PageContext(pageRef,pageState);  	
  	
      return pageContext;
  }

  @Override
  public UserPortalConfig getUserPortalConfig(String portalName, String accessUser) throws Exception {
    return null;
  }

  @Override
  public UserPortalConfig getUserPortalConfig(String portalName,
                                              String accessUser,
                                              UserPortalContext userPortalContext) throws Exception {
    return null;
  }
}
