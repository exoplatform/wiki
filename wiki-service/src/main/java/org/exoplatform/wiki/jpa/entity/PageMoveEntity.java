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
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 23, 2015
 */
@Entity(name = "WikiPageMoveEntity")
@ExoEntity
@Table(name = "WIKI_PAGE_MOVES")
@NamedQueries({
        @NamedQuery(name = "wikiPageMove.getPreviousPage", query = "SELECT p FROM WikiPageMoveEntity p WHERE p.wikiType = :wikiType AND p.wikiOwner = :wikiOwner AND p.pageName = :pageName")
})
public class PageMoveEntity {

  public PageMoveEntity() {
  }

  public PageMoveEntity(String wikiType, String wikiOwner, String pageName, Date createdDate) {
    this.wikiType = wikiType;
    this.wikiOwner = wikiOwner;
    this.pageName = pageName;
    this.createdDate = createdDate;
  }

  @Id
  @SequenceGenerator(name="SEQ_WIKI_PAGE_MOVES_MOVE_ID", sequenceName="SEQ_WIKI_PAGE_MOVES_MOVE_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_WIKI_PAGE_MOVES_MOVE_ID")
  @Column(name = "PAGE_MOVE_ID")
  private long id;

  @ManyToOne
  @JoinColumn(name = "PAGE_ID")
  private PageEntity page;

  @Column(name = "WIKI_TYPE")
  private String wikiType;

  @Column(name = "WIKI_OWNER")
  private String wikiOwner;

  @Column(name = "PAGE_NAME")
  private String pageName;

  @Column(name = "CREATED_DATE")
  private Date createdDate;

  public long getId() {
    return id;
  }

  public PageEntity getPage() {
    return page;
  }

  public void setPage(PageEntity page) {
    this.page = page;
  }

  public String getWikiType() {
    return wikiType;
  }

  public void setWikiType(String wikiType) {
    this.wikiType = wikiType;
  }

  public String getWikiOwner() {
    return wikiOwner;
  }

  public void setWikiOwner(String wikiOwner) {
    this.wikiOwner = wikiOwner;
  }

  public String getPageName() {
    return pageName;
  }

  public void setPageName(String pageName) {
    this.pageName = pageName;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }
}
