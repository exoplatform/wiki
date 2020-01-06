/*
 *
 *  * Copyright (C) 2003-2015 eXo Platform SAS.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 *  
 */

package org.exoplatform.wiki.jpa;

import org.exoplatform.commons.api.persistence.DataInitializer;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.wiki.jpa.dao.*;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 10/20/15
 */
public abstract class BaseWikiJPAIntegrationTest extends BaseTest {
  protected WikiDAO        wikiDAO;
  protected PageDAO        pageDAO;
  protected PageAttachmentDAO  pageAttachmentDAO;
  protected DraftPageAttachmentDAO  draftPageAttachmentDAO;
  protected DraftPageDAO   draftPageDAO;
  protected PageVersionDAO pageVersionDAO;
  protected PageMoveDAO    pageMoveDAO;
  protected TemplateDAO    templateDAO;
  protected EmotionIconDAO emotionIconDAO;
  protected FileService fileService;

  public void setUp() throws Exception {
    super.setUp();

    // make sure data are well initialized for each test
    DataInitializer dataInitializer = PortalContainer.getInstance().getComponentInstanceOfType(DataInitializer.class);
    dataInitializer.initData();

    //Init fileService
    fileService= PortalContainer.getInstance().getComponentInstanceOfType(FileService.class);
    // Init DAO
    wikiDAO = PortalContainer.getInstance().getComponentInstanceOfType(WikiDAO.class);
    pageDAO = PortalContainer.getInstance().getComponentInstanceOfType(PageDAO.class);
    pageAttachmentDAO = PortalContainer.getInstance().getComponentInstanceOfType(PageAttachmentDAO.class);
    draftPageAttachmentDAO = PortalContainer.getInstance().getComponentInstanceOfType(DraftPageAttachmentDAO.class);
    draftPageDAO = PortalContainer.getInstance().getComponentInstanceOfType(DraftPageDAO.class);
    pageVersionDAO = PortalContainer.getInstance().getComponentInstanceOfType(PageVersionDAO.class);
    pageMoveDAO = PortalContainer.getInstance().getComponentInstanceOfType(PageMoveDAO.class);
    templateDAO = PortalContainer.getInstance().getComponentInstanceOfType(TemplateDAO.class);
    emotionIconDAO = PortalContainer.getInstance().getComponentInstanceOfType(EmotionIconDAO.class);
    // Clean Data
    cleanDB();
  }

  public void tearDown() throws Exception {
    // Clean Data
    cleanDB();
    super.tearDown();
  }

  private void cleanDB() {
    emotionIconDAO.deleteAll();
    templateDAO.deleteAll();
    pageMoveDAO.deleteAll();
    pageVersionDAO.deleteAll();
    draftPageAttachmentDAO.deleteAll();
    draftPageDAO.deleteAll();
    pageAttachmentDAO.deleteAll();
    pageDAO.deleteAll();
    wikiDAO.deleteAll();
  }
}
