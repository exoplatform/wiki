/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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
package org.exoplatform.wiki.jpa.dao;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.wiki.jpa.entity.DraftPageEntity;

import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 24, 2015  
 */
public class DraftPageDAO extends WikiBaseDAO<DraftPageEntity, Long> {

  public List<DraftPageEntity> findDraftPagesByUser(String username) {
    TypedQuery<DraftPageEntity> query = getEntityManager().createNamedQuery("wikiDraftPage.findDraftPagesByUser", DraftPageEntity.class)
            .setParameter("username", username);
    return query.getResultList();
  }

  public DraftPageEntity findLatestDraftPageByUserAndName(String username, String draftPageName) {
    TypedQuery<DraftPageEntity> query = getEntityManager().createNamedQuery("wikiDraftPage.findDraftPageByUserAndName", DraftPageEntity.class)
            .setParameter("username", username).setMaxResults(1)
            .setParameter("draftPageName", draftPageName);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public DraftPageEntity findLatestDraftPageByUser(String username) {
    TypedQuery<DraftPageEntity> query = getEntityManager().createNamedQuery("wikiDraftPage.findDraftPagesByUser", DraftPageEntity.class)
            .setParameter("username", username).setMaxResults(1);
    List<DraftPageEntity> draftPages = query.getResultList();
    return draftPages.size() > 0 ? draftPages.get(0) : null;
  }

  public List<DraftPageEntity> findDraftPagesByUserAndTargetPage(String username, long targetPageId) {
    TypedQuery<DraftPageEntity> query = getEntityManager().createNamedQuery("wikiDraftPage.findDraftPageByUserAndTargetPage", DraftPageEntity.class)
            .setParameter("username", username)
            .setParameter("targetPageId", targetPageId);
    return query.getResultList();
  }

  @ExoTransactional
  public void deleteDraftPagesByUserAndTargetPage(String username, long targetPageId) {

    List<DraftPageEntity> draftPages = findDraftPagesByUserAndTargetPage(username, targetPageId);
    for (DraftPageEntity draftPage: draftPages) {
      delete(draftPage);
    }

  }

  @ExoTransactional
  public void deleteDraftPagesByUserAndName(String draftName, String username) {
    DraftPageEntity draftPage = findLatestDraftPageByUserAndName(username, draftName);
    if(draftPage != null) {
      delete(draftPage);
    }
  }
}
