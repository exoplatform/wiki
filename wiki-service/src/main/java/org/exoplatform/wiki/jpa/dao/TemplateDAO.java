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

import org.exoplatform.wiki.jpa.entity.TemplateEntity;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

public class TemplateDAO extends WikiBaseDAO<TemplateEntity, Long> {

  public List<TemplateEntity> getTemplatesOfWiki(String wikiType, String wikiOwner) {
    TypedQuery<TemplateEntity> query = getEntityManager().createNamedQuery("template.getTemplatesOfWiki", TemplateEntity.class)
            .setParameter("type", wikiType)
            .setParameter("owner", wikiOwner);
    return query.getResultList();
  }

  public TemplateEntity getTemplateOfWikiByName(String wikiType, String wikiOwner, String templateName) {
    TypedQuery<TemplateEntity> query = getEntityManager().createNamedQuery("template.getTemplateOfWikiByName", TemplateEntity.class)
            .setParameter("name", templateName)
            .setParameter("type", wikiType)
            .setParameter("owner", wikiOwner);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<TemplateEntity> searchTemplatesByTitle(String wikiType, String wikiOwner, String searchText) {
    TypedQuery<TemplateEntity> query = getEntityManager().createNamedQuery("template.searchTemplatesByTitle", TemplateEntity.class)
            .setParameter("type", wikiType)
            .setParameter("owner", wikiOwner)
            .setParameter("searchText", "%" + searchText + "%");
    return query.getResultList();
  }

  public List<TemplateEntity> findAllBySyntax(String syntax, int offset, int limit) {
    return getEntityManager().createNamedQuery("template.getAllTemplatesBySyntax")
            .setParameter("syntax", syntax)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
  }

  public Long countTemplatesBySyntax(String syntax) {
    return (Long) getEntityManager().createNamedQuery("template.countAllTemplatesBySyntax")
            .setParameter("syntax", syntax)
            .getSingleResult();
  }

}
