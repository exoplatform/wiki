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
import java.io.IOException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.exoplatform.wiki.rendering.util.Utils;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.internal.macro.chart.ChartImageWriter;
import org.xwiki.rendering.internal.macro.chart.ImageId;
import org.xwiki.rendering.macro.MacroExecutionException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 11, 2013  
 */
@Component
@Named("tmp")
@Singleton
public class ImageWriterImpl extends BaseImageWriterImpl implements ChartImageWriter {

  @Override
  public void writeImage(ImageId imageId, byte[] imageData) throws MacroExecutionException {
    try {
      super.writeImage(imageId.getId(), imageData);
    } catch (Exception e) {
        throw new MacroExecutionException("Failed to write the generated chart image", e);
    }
  }
  
  /**
   * Compute the location where to store the generated chart image.
   *
   * @param imageId the image id that we use to generate a unique storage location
   * @return the location where to store the generated chart image
   * @throws MacroExecutionException if an error happened when computing the location
   */
  protected File getStorageLocation(ImageId imageId) throws MacroExecutionException
  {
      try {
        return super.getStorageLocation(imageId.getId());
      } catch (Exception e) {
          // Should not happen since UTF8 encoding should always be present
          throw new MacroExecutionException("Failed to compute chart image location", e);
      }
  }  

  @Override
  public String getURL(ImageId imageId) throws MacroExecutionException {
    return super.getURL(imageId.getId());
  }

}
