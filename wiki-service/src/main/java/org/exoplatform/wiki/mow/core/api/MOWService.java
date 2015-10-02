/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wiki.mow.core.api;

import org.chromattic.api.Chromattic;
import org.chromattic.api.ChromatticException;
import org.chromattic.api.ChromatticSession;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.impl.WikiChromatticLifeCycle;

/**
 * @version $Revision$
 */
public class MOWService {

  private WikiChromatticLifeCycle chromatticLifeCycle;

  private ChromatticManager chromatticManager;

  public MOWService(ChromatticManager chromatticManager) {
    this.chromatticManager = chromatticManager;
    this.chromatticLifeCycle = (WikiChromatticLifeCycle) chromatticManager.getLifeCycle("wiki");
    this.chromatticLifeCycle.setMOWService(this);
  }

  public ModelImpl getModel() throws WikiException {
    RequestLifeCycle.begin(chromatticManager);
    Chromattic chromattic = chromatticLifeCycle.getChromattic();
    try {
      ChromatticSession chromeSession = chromattic.openSession();
      return new ModelImpl(chromeSession);
    } catch(Exception e) {
      throw new WikiException("Cannot open chromattic session - Cause : " + e.getMessage(), e);
    }
  }

  public ChromatticSession getSession() {
    return chromatticLifeCycle.getContext().getSession();
  }
}
