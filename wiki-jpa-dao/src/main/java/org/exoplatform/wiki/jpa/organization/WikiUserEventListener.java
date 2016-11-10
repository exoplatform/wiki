/* 
* Copyright (C) 2003-2016 eXo Platform SAS.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see http://www.gnu.org/licenses/ .
*/
package org.exoplatform.wiki.jpa.organization;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.dao.TemplateDAO;
import org.exoplatform.wiki.jpa.dao.WikiDAO;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.TemplateEntity;
import org.exoplatform.wiki.jpa.entity.WikiEntity;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;
import org.exoplatform.wiki.mow.api.WikiType;

import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 2/24/16
 */
public class WikiUserEventListener extends UserEventListener {

  private static final Log LOG = ExoLogger.getLogger(WikiUserEventListener.class);

  private WikiDAO wikiDAO;
  private PageDAO pageDAO;
  private TemplateDAO templateDAO;
  private IndexingService indexingService;

  public WikiUserEventListener(WikiDAO wikiDAO, PageDAO pageDAO, TemplateDAO templateDAO, IndexingService indexingService) {
    this.wikiDAO = wikiDAO;
    this.pageDAO = pageDAO;
    this.templateDAO = templateDAO;
    this.indexingService = indexingService;
  }

  /**
   * Deletes all wiki data of the deleted user.
   * WikiService does not have delete services, so we have to use DAOs directly and manage unindexation manually.
   * @param user Deleted user
   * @throws Exception
   */
  @Override
  public void postDelete(User user) throws Exception {

    LOG.info("Removing all wiki data of the user "+user.getUserName());

    //First remove and unindex all Wiki Pages (include wikiHome)
    List<PageEntity> pages = pageDAO.getPagesOfWiki(WikiType.USER.toString().toLowerCase(), user.getUserName());
    if (pages != null) {
      for (PageEntity page : pages) {
        indexingService.unindex(WikiPageIndexingServiceConnector.TYPE, String.valueOf(page.getId()));
      }
      pageDAO.deleteAll(pages);
    }

    //Then remove the template
    List<TemplateEntity> templates = templateDAO.getTemplatesOfWiki(WikiType.USER.toString().toLowerCase(), user.getUserName());
    if (templates != null) {
      templateDAO.deleteAll(templates);
    }

    //Finally remove the user wiki
    WikiEntity wikiUser = wikiDAO.getWikiByTypeAndOwner(WikiType.USER.toString().toLowerCase(), user.getUserName());
    if (wikiUser != null) {
      wikiDAO.delete(wikiUser);
    }
  }

}

