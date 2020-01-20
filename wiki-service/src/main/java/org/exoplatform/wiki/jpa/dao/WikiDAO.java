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

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.wiki.jpa.entity.WikiEntity;
import org.exoplatform.wiki.mow.api.WikiType;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Jun
 * 24, 2015
 */
public class WikiDAO extends WikiBaseDAO<WikiEntity, Long> {

  public List<Long> findAllIds(int offset, int limit) {
    return getEntityManager().createNamedQuery("wiki.getAllIds").setFirstResult(offset).setMaxResults(limit).getResultList();
  }

  public WikiEntity getWikiByTypeAndOwner(String wikiType, String wikiOwner) {

    //We need to add the first "/" on the wiki owner if it's  wiki group
    if (StringUtils.equalsIgnoreCase(WikiType.GROUP.name(), wikiType)) {
      wikiOwner = validateGroupWikiOwner(wikiOwner);
    }

    TypedQuery<WikiEntity> query = getEntityManager().createNamedQuery("wiki.getWikiByTypeAndOwner", WikiEntity.class)
                                               .setParameter("type", wikiType)
                                               .setParameter("owner", wikiOwner);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<WikiEntity> getWikisByType(String wikiType) {
    TypedQuery<WikiEntity> query = getEntityManager().createNamedQuery("wiki.getWikisByType", WikiEntity.class)
                                               .setParameter("type", wikiType);

    return query.getResultList();
  }

}
