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
package org.exoplatform.wiki.rendering.render.xwiki.image;

import java.io.File;
import java.io.FileOutputStream;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.exoplatform.wiki.rendering.util.Utils;
import org.picocontainer.Startable;
import org.xwiki.component.annotation.Component;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 14, 2013  
 */
@Component
@Named("base")
@Singleton
public class BaseImageWriterImpl implements Startable {

  public void writeImage(String imageId, byte[] imageData) throws Exception {
    File imageFile = getStorageLocation(imageId);

    FileOutputStream fos = null;
    try {
        fos = new FileOutputStream(imageFile);
        fos.write(imageData);
        fos.close();
    } finally {
        IOUtils.closeQuietly(fos);
    }
  }
  
  /**
   * Compute the location where to store the generated chart image.
   *
   * @param imageId the image id that we use to generate a unique storage location
   * @return the location where to store the generated chart image
   * @throws Exception if an error happened when computing the location
   */
  protected File getStorageLocation(String imageId) throws Exception
  {
      File directory;
      directory = new File("temp/wiki");
      directory.mkdirs();
      File locationFile = new File(directory, imageId + ".png");
      return locationFile;
  }  

  public String getURL(String imageId) {
    StringBuilder sb = new StringBuilder();
    sb.append("/").append(Utils.getPortalName()).append("/").append(Utils.getRestContextName()).
       append("/thumbnailImage/originImage/temp/wiki/").append(imageId).append(".png");
    return sb.toString();
  }

  @Override
  public void start() {
  }

  @Override
  public void stop() {
  }
  
}