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

import org.chromattic.api.ChromatticSession;
import org.chromattic.api.UndeclaredRepositoryException;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.wiki.mow.core.api.wiki.WikiNodeType;
import org.exoplatform.wiki.mow.core.api.wiki.WikiStore;
import org.exoplatform.wiki.service.impl.WikiChromatticLifeCycle;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * @version $Revision$
 */
public class MOWService {

  private WikiChromatticLifeCycle chromatticLifeCycle;

  public MOWService(ChromatticManager chromatticManager) {
    this.chromatticLifeCycle = (WikiChromatticLifeCycle) chromatticManager.getLifeCycle(WikiChromatticLifeCycle.WIKI_LIFECYCLE_NAME);
  }

  public ChromatticSession getSession() {
    return chromatticLifeCycle.getSession();
  }

  public boolean startSynchronization() {
    if (chromatticLifeCycle.getManager().getSynchronization() == null) {
      chromatticLifeCycle.getManager().beginRequest();
      return true;
    }
    return false;
  }

  public void stopSynchronization(boolean requestClose) {
    if (requestClose) {
      chromatticLifeCycle.getManager().endRequest(true);
    }
  }

  public boolean persist() {
    return persist(false);
  }

  /**
   * Make the decision to persist JCR Storage and refresh session or not
   *
   * @return
   */
  public boolean persist(boolean isRefresh) {
    try {
      ChromatticSession chromatticSession = chromatticLifeCycle.getSession();
      if (chromatticSession.getJCRSession().hasPendingChanges()) {
        chromatticSession.getJCRSession().save();
        if (isRefresh) {
          chromatticSession.getJCRSession().refresh(true);
        }

      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  public WikiStore getWikiStore() {
    boolean created = this.startSynchronization();

    ChromatticSession session = chromatticLifeCycle.getSession();
    WikiStoreImpl store = session.findByPath(WikiStoreImpl.class, "exo:applications" + "/"
            + WikiNodeType.Definition.WIKI_APPLICATION + "/"
            + WikiNodeType.Definition.WIKI_STORE_NAME);
    if (store == null) {
      try {
        Node rootNode = session.getJCRSession().getRootNode();
        Node publicApplicationNode = rootNode.getNode("exo:applications");
        Node eXoWiki = null;
        try {
          eXoWiki = publicApplicationNode.getNode(WikiNodeType.Definition.WIKI_APPLICATION);
        } catch (PathNotFoundException e) {
          eXoWiki = publicApplicationNode.addNode(WikiNodeType.Definition.WIKI_APPLICATION);
          publicApplicationNode.save();
        }
        Node wikiMetadata = eXoWiki.addNode(WikiNodeType.Definition.WIKI_STORE_NAME,
                WikiNodeType.WIKI_STORE);
        Node wikis = eXoWiki.addNode("wikis");
        session.save();
        store = session.findByNode(WikiStoreImpl.class, wikiMetadata);

      } catch (RepositoryException e) {
        throw new UndeclaredRepositoryException(e);
      }
    }

    this.stopSynchronization(created);

    return store;
  }
}
