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
package org.exoplatform.wiki.jpa.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 23, 2015
 */
@MappedSuperclass
@ExoEntity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class AttachmentEntity {

  @Id
  @Column(name = "ATTACHMENT_ID")
  @SequenceGenerator(name="SEQ_WIKI_PAGE_ATTACH_ATTACH_ID", sequenceName="SEQ_WIKI_PAGE_ATTACH_ATTACH_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_WIKI_PAGE_ATTACH_ATTACH_ID")
  private Long id;

  @Column(name = "CREATED_DATE")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  @Column(name = "FULL_TITLE")
  private String fullTitle;

  @Column(name = "ATTACHMENT_FILE_ID")
  private Long attachmentFileID;

  public long getId(){return this.id;}

  public void setId(Long id) {
    this.id = id;
  }

  public Date getCreatedDate(){
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public String getFullTitle() {
    return fullTitle;
  }

  public void setFullTitle(String fullTitle) {
    this.fullTitle = fullTitle;
  }

  public Long getAttachmentFileID() {
    return attachmentFileID;
  }

  public void setAttachmentFileID(Long attachmentFileID) {
    this.attachmentFileID = attachmentFileID;
  }

}
