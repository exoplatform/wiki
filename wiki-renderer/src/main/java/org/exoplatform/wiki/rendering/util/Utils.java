/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.wiki.rendering.util;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.definition.PortalContainerConfig;
import org.exoplatform.container.xml.PortalContainerInfo;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 14, 2013  
 */
public class Utils {

  
  /**
   * Gets the service.
   * @param clazz the clazz
   * @return the service
   */
  public static <T> T getService(Class<T> clazz) {
    return getService(clazz, null);
  }
  
  /**
   * Gets the service.
   * @param clazz the class
   * @param containerName the container's name
   * @return the service
   */
  public static <T> T getService(Class<T> clazz, String containerName) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    if (containerName != null) {
      container = RootContainer.getInstance().getPortalContainer(containerName);
    }
    if (container.getComponentInstanceOfType(clazz)==null) {
      containerName = PortalContainer.getCurrentPortalContainerName();
      container = RootContainer.getInstance().getPortalContainer(containerName);
    }
    return clazz.cast(container.getComponentInstanceOfType(clazz));
  }
  
  /**
   * gets rest context name
   * @return rest context name
   */
  public static String getRestContextName() {
    return getService(PortalContainerConfig.class).
              getRestContextName(getService(PortalContainerInfo.class).getContainerName());
  }  

  /**
   * gets portal name
   * @return portal name
   */
  public static String getPortalName() {
    return getService(PortalContainerInfo.class).getContainerName();
  }
  

}

