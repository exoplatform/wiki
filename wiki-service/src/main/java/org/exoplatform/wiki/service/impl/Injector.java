/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.wiki.service.impl;

import org.chromattic.api.event.LifeCycleListener;
import org.chromattic.api.event.StateChangeListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.chromattic.ext.ntdef.NTFrozenNode;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;

public class Injector implements LifeCycleListener, StateChangeListener {
  
  private final MOWService mowService;

  //private final WikiService wService;
  
  //private final RenderingService renderingService;
  
  private static final Log log = ExoLogger.getLogger("org.exoplatform.wiki.service.impl.Injector");

  public Injector(MOWService mowService) {
    this.mowService = mowService;
    //this.wService = wService;
    //this.renderingService = renderingService;
  }

  @Override
  public void added(String id, String path, String name, Object o) {
    if (o instanceof PageImpl) {
      ((PageImpl) o).setMOWService(mowService);
      //((PageImpl) o).setComponentManager(renderingService.getComponentManager());
    }
    /*
    if(o instanceof WikiContainer<?>) {
      ((WikiContainer<?>) o).setwService(wService);
    }
    */
    if (o instanceof NTFrozenNode) {
      ((NTFrozenNode) o).setMOWService(mowService);
    }
  }

  @Override
  public void created(Object o) {
    if (o instanceof PageImpl) {
      ((PageImpl) o).setMOWService(mowService);
      //((PageImpl) o).setComponentManager(renderingService.getComponentManager());
    }
    /*
    if(o instanceof WikiContainer<?>) {
      ((WikiContainer<?>) o).setwService(wService);
    }
    */
    if (o instanceof NTFrozenNode) {
      ((NTFrozenNode) o).setMOWService(mowService);
    }
  }

  @Override
  public void loaded(String id, String path, String name, Object o) {
    if (o instanceof PageImpl) {
      ((PageImpl) o).setMOWService(mowService);
      //((PageImpl) o).setComponentManager(renderingService.getComponentManager());
    }
    /*
    if(o instanceof WikiContainer<?>) {
      ((WikiContainer<?>) o).setwService(wService);
    }
    */
    if (o instanceof NTFrozenNode) {
      ((NTFrozenNode) o).setMOWService(mowService);
    }
  }

  @Override
  public void removed(String id, String path, String name, Object o) {
    if (o instanceof PageImpl) {
      ((PageImpl) o).setMOWService(mowService);
      //((PageImpl) o).setComponentManager(renderingService.getComponentManager());
    }
    if (o instanceof NTFrozenNode) {
      ((NTFrozenNode) o).setMOWService(mowService);
    }
  }

  @Override
  public void propertyChanged(String id, Object o, String propertyName, Object propertyValue) {
  }
}
