/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.wiki.service.image;

import java.io.InputStream;

/**
 * Provides different methods which handle images 
 * with the right ratio and optimize them.
 *
 * @LevelAPI Platform
 */
public interface ResizeImageService {

  /**
   * Resizes a given image to the specified dimensions.
   * 
   * @param imageName Name of the image to be resized.
   * @param is The input stream of the image.
   * @param requestWidth The new width.
   * @param requestHeight The new height.
   * @param keepAspectRatio Keeps the aspect ratio or not.
   * @return The input stream of the resized image.
   */
  public InputStream resizeImage(String imageName,
                                 InputStream is,
                                 int requestWidth,
                                 int requestHeight,
                                 boolean keepAspectRatio);

  /**
   * Resizes a given image to adapt with the desired width and keep
   * the aspect ratio.
   * 
   * @param imageName Name of the image to be resized.
   * @param is The input stream of the image.
   * @param requestWidth The desired width.
   * @return The input stream of the resized image.
   */
  public InputStream resizeImageByWidth(String imageName, InputStream is, int requestWidth);

  /**
   * Resizes a given image input stream to adapt with the desired height and keep
   * the aspect ratio.
   * 
   * @param imageName Name of the resized image.
   * @param is The input stream of the image.
   * @param requestHeight The desired height.
   * @return The input stream of the resized image.
   */
  public InputStream resizeImageByHeight(String imageName, InputStream is, int requestHeight);
}
