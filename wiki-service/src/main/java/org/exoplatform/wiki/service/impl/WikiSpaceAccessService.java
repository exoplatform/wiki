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

import java.util.List;

import javax.servlet.ServletContext;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalApplication;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.gatein.wci.WebApp;
import org.gatein.wci.WebAppEvent;
import org.gatein.wci.WebAppLifeCycleEvent;
import org.gatein.wci.WebAppListener;
import org.gatein.wci.impl.DefaultServletContainerFactory;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS
 * Author : phongth
 *          phongth@exoplatform.com
 * Nov 16, 2012 
 */
public class WikiSpaceAccessService implements WebAppListener, Startable {
  private static final Log log = ExoLogger.getLogger(WikiSpaceAccessService.class);

  @Override
  public void start() {
    DefaultServletContainerFactory.getInstance().getServletContainer().addWebAppListener(this);
  }

  @Override
  public void stop() {
    DefaultServletContainerFactory.getInstance().getServletContainer().removeWebAppListener(this);
  }

  @Override
  public void onEvent(WebAppEvent webAppEvent) {
    if (webAppEvent instanceof WebAppLifeCycleEvent) {
      WebAppLifeCycleEvent lfEvent = (WebAppLifeCycleEvent) webAppEvent;
      
      // Check if the event type is ADDED
      if (lfEvent.getType() == WebAppLifeCycleEvent.ADDED) {
        WebApp webApp = webAppEvent.getWebApp();
        ServletContext scontext = webApp.getServletContext();

        final String contextPath = scontext.getContextPath();
        // final to initialize Root WebApp
        if ("".equals(contextPath)) {
          handle();
        }
      }
    }
  }

  private void handle() {
    try {
      WebAppController controller = (WebAppController) PortalContainer.getInstance().getComponentInstanceOfType(WebAppController.class);
      PortalApplication app = controller.getApplication(PortalApplication.PORTAL_APPLICATION_ID);
      List<ApplicationLifecycle> lifecyces = app.getApplicationLifecycle();
      
      // Add SpaceAccessLifecycle to ApplicationLifecycle list
      lifecyces.add(new SpaceAccessLifecycle());
      app.setApplicationLifecycle(lifecyces);
    } catch (Exception e) {
      log.error("Could not inject SpaceAccessLifecycle class.", e);
    }
  }
}