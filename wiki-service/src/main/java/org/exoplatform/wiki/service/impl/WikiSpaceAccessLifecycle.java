/*
 * Copyright (C) 2012 eXo Platform SAS.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.wiki.service.impl;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.application.RequestNavigationData;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.web.application.Application;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.exoplatform.web.application.RequestFailure;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.utils.Utils;

public class WikiSpaceAccessLifecycle extends BaseComponentPlugin implements ApplicationLifecycle<WebuiRequestContext> {
  private static final String WIKI_PORTLET_NAME = "wiki";
  
  private static final Log      LOG               = ExoLogger.getLogger(WikiSpaceAccessLifecycle.class.toString());
  
  public void onInit(Application app) {
  }

  public void onStartRequest(final Application app, final WebuiRequestContext context) throws Exception {
    PortalRequestContext pcontext = (PortalRequestContext) context;
    String requestPath = pcontext.getControllerContext().getParameter(RequestNavigationData.REQUEST_PATH);
    String siteName = pcontext.getSiteName();
    try {
      if (pcontext.getSiteType().equals(SiteType.GROUP) && siteName.startsWith("/spaces")  && (requestPath != null) && (requestPath.length() > 0)) {
        
        // Check if user want to access to wiki application
        String currentUser = Utils.getCurrentUser();
        String[] params = requestPath.split("/");
        if ((params.length > 1) && params[1].equals(WIKI_PORTLET_NAME)) {
          String spaceId = params[0];
          String owner = siteName;
          String pageId = "WikiHome";
          if (params.length > 2) {
            pageId = params[2];
          }
          
          // Get page
          SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
          Space space = spaceService.getSpaceById(spaceId);
          if (space == null) {
            space = spaceService.getSpaceByPrettyName(spaceId);
            if (space == null) {
              space = spaceService.getSpaceByGroupId(spaceId);
            }
          }

          // If user is not member of space but has view permission
          if (space != null && !Space.HIDDEN.equals(space.getVisibility()) && !spaceService.isSuperManager(currentUser)
              && !spaceService.isMember(space, currentUser)) {
            WikiPageParams wikiPageParams = new WikiPageParams(PortalConfig.GROUP_TYPE, owner, pageId);
            String permalink = Utils.getPermanlink(wikiPageParams, false);
            redirect(permalink);
          }
        }
      }
    } catch(Exception ex) {
      LOG.warn(String.format("Can not process url for request: site name = [%s], request path = [%s]", siteName, requestPath), ex);
    }
  }
  
  private static void redirect(String url) throws Exception {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    portalRequestContext.sendRedirect(url);
  }
  
  public void onFailRequest(Application app, WebuiRequestContext context, RequestFailure failureType) {
  }

  public void onEndRequest(Application app, WebuiRequestContext context) throws Exception {
  }

  public void onDestroy(Application app) {
  }
}
