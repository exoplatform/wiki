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

import org.apache.commons.lang.StringUtils;

import org.exoplatform.wiki.jpa.entity.PageMoveEntity;
import org.exoplatform.wiki.mow.api.WikiType;

import javax.persistence.TypedQuery;
import java.util.List;

public class PageMoveDAO extends WikiBaseDAO<PageMoveEntity,Long> {

  public List<PageMoveEntity> findInPageMoves(String wikiType, String wikiOwner, String pageName) {

    //We need to add the first "/" on the wiki owner if it's  wiki group
    if (StringUtils.equalsIgnoreCase(WikiType.GROUP.name(), wikiType)) {
      wikiOwner = validateGroupWikiOwner(wikiOwner);
    }

    TypedQuery<PageMoveEntity> query = getEntityManager().createNamedQuery("wikiPageMove.getPreviousPage", PageMoveEntity.class)
            .setParameter("wikiType", wikiType)
            .setParameter("wikiOwner", wikiOwner)
            .setParameter("pageName", pageName);
    return query.getResultList();
  }

}
