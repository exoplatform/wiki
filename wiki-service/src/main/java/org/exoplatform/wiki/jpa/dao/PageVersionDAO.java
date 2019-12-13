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

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.PageVersionEntity;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;

public class PageVersionDAO extends WikiBaseDAO<PageVersionEntity, Long> {
   public Long getLastversionNumberOfPage(Long pageId) {
     Query query = getEntityManager().createNamedQuery("wikiPageVersion.getLastversionNumberOfPage")
             .setParameter("pageId", pageId);

     try {
       return (Long) query.getSingleResult();
     } catch (NoResultException e) {
       return null;
     }
   }

  public PageVersionEntity getPageversionByPageIdAndVersion(Long pageId, Long versionNumber) {
    TypedQuery<PageVersionEntity> query = getEntityManager().createNamedQuery("wikiPageVersion.getPageversionByPageIdAndVersion", PageVersionEntity.class)
            .setParameter("pageId", pageId)
            .setParameter("versionNumber", versionNumber);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
