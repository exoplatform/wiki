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

import org.chromattic.api.ChromatticSession;
import org.exoplatform.commons.chromattic.ChromatticLifeCycle;
import org.exoplatform.commons.chromattic.SessionContext;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;

public class WikiChromatticLifeCycle extends ChromatticLifeCycle {

  public static final String WIKI_LIFECYCLE_NAME = "wiki";

  private static final ThreadLocal<ChromatticSession> session = new ThreadLocal<>();

  public WikiChromatticLifeCycle(InitParams params) {
    super(params);
  }

  public ChromatticSession getSession() {
    if (invalidSession()) {
      reCreateSession();
    }

    return session.get();
  }

  private boolean invalidSession() {
    boolean invalid = (session.get() == null);
    if(invalid) return invalid;
    return session.get().getJCRSession().isLive() == false || session.get().isClosed();
  }

  private void reCreateSession() {
    try {
      onOpenSession(openContext());
    } catch (IllegalStateException e) {
      this.closeContext(false);
      if(this.getManager().getSynchronization() != null) {
        this.getManager().endRequest(false);
      }
      this.getManager().startRequest(ExoContainerContext.getCurrentContainer());
      session.set(this.getChromattic().openSession());
    }
  }

  @Override
  protected void onOpenSession(SessionContext context) {
    session.set(context.getSession());
    context.getSession().addEventListener(new Injector());
    super.onOpenSession(context);
  }

  @Override
  protected void onCloseSession(final SessionContext context) {
    super.onCloseSession(context);
    if (session.get() != null) {
      session.get().close();
    }
    session.remove();
  }

}
