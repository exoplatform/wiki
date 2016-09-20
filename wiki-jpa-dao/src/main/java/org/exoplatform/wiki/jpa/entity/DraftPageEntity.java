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
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * Jun 23, 2015
 */
@Entity(name = "WikiDraftPageEntity")
@ExoEntity
@Table(name = "WIKI_DRAFT_PAGES")
@NamedQueries({
        @NamedQuery(name = "wikiDraftPage.findDraftPagesByUser", query = "SELECT d FROM WikiDraftPageEntity d WHERE d.author = :username ORDER BY d.updatedDate DESC"),
        @NamedQuery(name = "wikiDraftPage.findDraftPageByUserAndName", query = "SELECT d FROM WikiDraftPageEntity d WHERE d.author = :username AND d.name = :draftPageName ORDER BY d.updatedDate DESC"),
        @NamedQuery(name = "wikiDraftPage.findDraftPageByUserAndTargetPage", query = "SELECT d FROM WikiDraftPageEntity d WHERE d.author = :username AND d.targetPage.id = :targetPageId")
})
public class DraftPageEntity extends BasePageEntity {

  @Id
  @SequenceGenerator(name="SEQ_WIKI_DRAFT_PAGES_DRAFT_ID", sequenceName="SEQ_WIKI_DRAFT_PAGES_DRAFT_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_WIKI_DRAFT_PAGES_DRAFT_ID")
  @Column(name = "DRAFT_PAGE_ID")
  private long id;

  @ManyToOne
  @JoinColumn(name = "TARGET_PAGE_ID")
  private PageEntity targetPage;

  @Column(name = "TARGET_PAGE_REVISION")
  private String targetRevision;

  @Column(name = "NEW_PAGE")
  private boolean newPage;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "draftPage")
  private List<DraftPageAttachmentEntity> attachments;

  public PageEntity getTargetPage() {
    return targetPage;
  }

  public void setTargetPage(PageEntity targetPage) {
    this.targetPage = targetPage;
  }

  public String getTargetRevision() {
    return targetRevision;
  }

  public void setTargetRevision(String targetRevision) {
    this.targetRevision = targetRevision;
  }

  public boolean isNewPage() {
    return newPage;
  }

  public void setNewPage(boolean newPage) {
    this.newPage = newPage;
  }

  public long getId() {
    return id;
  }

  public List<DraftPageAttachmentEntity> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<DraftPageAttachmentEntity> attachments) {
    this.attachments = attachments;
  }
}
