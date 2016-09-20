/* 
* Copyright (C) 2003-2015 eXo Platform SAS.
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
package org.exoplatform.wiki.jpa.search;

import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.JPADataStorage;
import org.exoplatform.wiki.jpa.entity.AttachmentEntity;
import org.exoplatform.wiki.jpa.entity.PageAttachmentEntity;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.mow.api.Attachment;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.listener.AttachmentWikiListener;

import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 11/19/15
 */
public class AttachmentIndexingListener extends AttachmentWikiListener {

  private static final Log LOG = ExoLogger.getLogger(AttachmentIndexingListener.class);

  private JPADataStorage jpaDataStorage;

  private FileService fileService;

  public AttachmentIndexingListener(FileService fileService, JPADataStorage jpaDataStorage) {
    this.jpaDataStorage = jpaDataStorage;
    this.fileService = fileService;
  }

  @Override
  public void addAttachment(Attachment attachment, Page page) throws WikiException {
    String attachmentId = getAttachmentId(attachment.getName(), page);
    if (attachmentId != null) {
      IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
      indexingService.index(AttachmentIndexingServiceConnector.TYPE, attachmentId);
      LOG.debug("Index attachment {} with name {} to ES", attachmentId, attachment.getName());
    }
  }

  @Override
  public void deleteAttachment(String attachmentName, Page page) throws WikiException {
    String attachmentId = getAttachmentId(attachmentName, page);
    if (attachmentId != null) {
      IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
      indexingService.unindex(AttachmentIndexingServiceConnector.TYPE, attachmentId);
      LOG.debug("Unindex attachment {} with name {} from ES", attachmentId, attachmentName);
    }
  }

  private String getAttachmentId(String attachmentName, Page page) throws WikiException {

    if (page instanceof DraftPage) return null;

    PageEntity pageEntity = jpaDataStorage.fetchPageEntity(page);

    if (pageEntity != null) {

      List<PageAttachmentEntity> attachmentsEntities = pageEntity.getAttachments();
      if (attachmentsEntities != null) {
        for (int i = 0; i < attachmentsEntities.size(); i++) {
          AttachmentEntity attachmentEntity = attachmentsEntities.get(i);
          long fileId = attachmentEntity.getAttachmentFileID();
          FileInfo fileInfo = fileService.getFileInfo(fileId);
          if (fileInfo.getName() != null && fileInfo.getName().equals(attachmentName)) {
            return String.valueOf(attachmentEntity.getId());
          }
        }
      }
    }
    return null;
  }

}

