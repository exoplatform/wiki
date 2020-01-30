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

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 23, 2015
 */
@Entity(name = "WikiPageAttachmentEntity")
@ExoEntity
@Table(name = "WIKI_PAGE_ATTACHMENTS")
@NamedQueries({
    @NamedQuery(name = "pageAttachment.getAllIds", query = "SELECT a.id FROM WikiPageAttachmentEntity a ORDER BY a.id"),
    @NamedQuery(name = "pageAttachment.getAllIdsByPageId", query = "SELECT a.id FROM WikiPageAttachmentEntity a WHERE a.page.id = :pageId")
})
public class PageAttachmentEntity extends AttachmentEntity {

  @ManyToOne(fetch=FetchType.LAZY)
  @JoinColumn(name="WIKI_PAGE_ID")
  private PageEntity page;

  public PageEntity getPage() {
    return page;
  }

  public void setPage(PageEntity page) {
    this.page = page;
  }
}
