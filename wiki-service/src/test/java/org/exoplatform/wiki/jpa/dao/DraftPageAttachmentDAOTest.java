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

import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileStorageException;
import org.exoplatform.wiki.jpa.BaseWikiJPAIntegrationTest;
import org.exoplatform.wiki.jpa.JPADataStorage;
import org.exoplatform.wiki.jpa.entity.*;
import org.exoplatform.wiki.mow.api.PermissionType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 25, 2015  
 */
public class DraftPageAttachmentDAOTest extends BaseWikiJPAIntegrationTest {

  public void testInsertDelete() throws IOException, URISyntaxException {
    //Given
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);
    PageEntity page = new PageEntity();
    page.setName("name");
    page.setWiki(wiki);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page = pageDAO.create(page);
    DraftPageEntity dp = new DraftPageEntity();
    dp.setName("draft1");
    dp.setTargetPage(page);
    dp.setCreatedDate(new Date());
    dp.setUpdatedDate(new Date());
    dp = draftPageDAO.create(dp);
    URL fileResource = this.getClass().getClassLoader().getResource("AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf");
    DraftPageAttachmentEntity att = new DraftPageAttachmentEntity();
    FileItem fileItem = null;
    try {
      fileItem = new FileItem(null,
              "AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf",
              null,
              JPADataStorage.WIKI_FILES_NAMESPACE_NAME,
              Files.readAllBytes(Paths.get(fileResource.toURI())).length,
              new Date(),
              "marry",
              false,
              new ByteArrayInputStream(Files.readAllBytes(Paths.get(fileResource.toURI()))));
      fileItem = fileService.writeFile(fileItem);
    } catch (Exception e) {
      fail(e);
    }
    att.setAttachmentFileID(fileItem.getFileInfo().getId());
    att.setCreatedDate(new Date());
    att.setDraftPage(dp);
    //When
    draftPageAttachmentDAO.create(att);
    Long id = att.getId();
    //Then
    AttachmentEntity got = draftPageAttachmentDAO.find(id);

    try {
      assertNotNull(fileService.getFile(got.getAttachmentFileID()).getAsByte());
      assertEquals(new File(fileResource.toURI()).length(), fileService.getFile(got.getAttachmentFileID()).getFileInfo().getSize());
    } catch (FileStorageException e) {
      fail(e);
    }
    //Delete
    draftPageAttachmentDAO.delete(att);
    assertNull(draftPageAttachmentDAO.find(id));
  }

  public void testUpdate() throws IOException, URISyntaxException {
    //Given
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);
    PageEntity page = new PageEntity();
    page.setName("name");
    page.setWiki(wiki);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page = pageDAO.create(page);
    DraftPageEntity dp = new DraftPageEntity();
    dp.setName("draft1");
    dp.setTargetPage(page);
    dp.setCreatedDate(new Date());
    dp.setUpdatedDate(new Date());
    dp = draftPageDAO.create(dp);
    URL fileResource = this.getClass().getClassLoader().getResource("AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf");
    DraftPageAttachmentEntity att = new DraftPageAttachmentEntity();
    att.setCreatedDate(new Date());
    FileItem fileItem = null;
    try {
      fileItem = new FileItem(null,
              "AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf",
              null,
              JPADataStorage.WIKI_FILES_NAMESPACE_NAME,
              Files.readAllBytes(Paths.get(fileResource.toURI())).length,
              new Date(),
              "marry",
              false,
              new ByteArrayInputStream(Files.readAllBytes(Paths.get(fileResource.toURI()))));
      fileItem = fileService.writeFile(fileItem);
    } catch (Exception e) {
      fail(e);
    }
    att.setAttachmentFileID(fileItem.getFileInfo().getId());
    att.setDraftPage(dp);
    //When
    draftPageAttachmentDAO.create(att);
    Long id = att.getId();
    PermissionEntity per = new PermissionEntity();
    per.setIdentity("user");
    per.setIdentityType("User");
    per.setPermissionType(PermissionType.ADMINPAGE);
    List<PermissionEntity> permissions = new ArrayList<>();
    permissions.add(per);
    Date date = new Date();
    fileItem.getFileInfo().setUpdater("creator");
    fileItem.getFileInfo().setUpdatedDate(date);

    try {
      fileService.updateFile(fileItem);
    } catch (FileStorageException e) {
      fail(e);
    }
    //Then
    draftPageAttachmentDAO.update(att);
    AttachmentEntity got = draftPageAttachmentDAO.find(id);
    FileInfo fileInfo=fileService.getFileInfo(got.getAttachmentFileID());
    assertEquals("creator", fileInfo.getUpdater());
    assertEquals(date, fileInfo.getUpdatedDate());
  }
}
