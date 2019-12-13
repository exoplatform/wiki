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

package org.exoplatform.wiki.jpa.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 7/16/15
 */
@Entity(name = "WikiPageEntity")
@ExoEntity
@Table(name = "WIKI_PAGES")
@NamedQueries({
    @NamedQuery(name = "wikiPage.getAllIds", query = "SELECT p.id FROM WikiPageEntity p  WHERE p.deleted = false ORDER BY p.id"),
    @NamedQuery(name = "wikiPage.getPageOfWikiByName", query = "SELECT p FROM WikiPageEntity p JOIN p.wiki w WHERE p.name = :name AND w.type = :type AND w.owner = :owner AND p.deleted = false"),
    @NamedQuery(name = "wikiPage.getAllPagesOfWiki", query = "SELECT p FROM WikiPageEntity p JOIN p.wiki w WHERE w.type = :type AND w.owner = :owner"),
    @NamedQuery(name = "wikiPage.getPagesOfWiki", query = "SELECT p FROM WikiPageEntity p JOIN p.wiki w WHERE w.type = :type AND w.owner = :owner AND p.deleted = :deleted"),
    @NamedQuery(name = "wikiPage.getChildrenPages", query = "SELECT p FROM WikiPageEntity p WHERE p.parentPage.id = :id AND p.deleted = false ORDER BY p.name")
})
public class PageEntity extends BasePageEntity {

  @Id
  @Column(name = "PAGE_ID")
  @SequenceGenerator(name="SEQ_WIKI_PAGES_PAGE_ID", sequenceName="SEQ_WIKI_PAGES_PAGE_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_WIKI_PAGES_PAGE_ID")
  private long id;

  @ManyToOne
  @JoinColumn(name = "WIKI_ID")
  private WikiEntity wiki;

  @ManyToOne
  @JoinColumn(name = "PARENT_PAGE_ID")
  private PageEntity parentPage;

  @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
  private List<PageVersionEntity> versions;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "page")
  private List<PageAttachmentEntity> attachments;

  @ManyToMany
  @JoinTable(name = "WIKI_PAGES_RELATED_PAGES",
      joinColumns = {@JoinColumn(name = "PAGE_ID")},
      inverseJoinColumns = {@JoinColumn(name = "RELATED_PAGE_ID")}
  )
  private List<PageEntity> relatedPages;

  @Column(name = "OWNER")
  private String owner;

  @Column(name = "EDITION_COMMENT")
  private String comment;

  @Column(name = "URL")
  private String url;

  @Column(name = "MINOR_EDIT")
  private boolean minorEdit;

  @Column(name = "ACTIVITY_ID")
  private String activityId;

  @ElementCollection
  @CollectionTable(
      name = "WIKI_WATCHERS",
      joinColumns=@JoinColumn(name = "PAGE_ID")
  )
  @Column(name="USERNAME")
  private Set<String> watchers = new HashSet<>();

  @ElementCollection
  @CollectionTable(
      name = "WIKI_PAGE_PERMISSIONS",
      joinColumns=@JoinColumn(name = "PAGE_ID")
  )
  private List<PermissionEntity> permissions;

  @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
  private List<PageMoveEntity> moves = new ArrayList<>();

  @Column(name = "DELETED")
  private boolean deleted;

  public long getId() {
    return id;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public boolean isMinorEdit() {
    return minorEdit;
  }

  public void setMinorEdit(boolean minorEdit) {
    this.minorEdit = minorEdit;
  }

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public List<PermissionEntity> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<PermissionEntity> permission) {
    this.permissions = permission;
  }

  public WikiEntity getWiki() {
    return wiki;
  }

  public void setWiki(WikiEntity wiki) {
    this.wiki = wiki;
  }

  public PageEntity getParentPage() {
    return parentPage;
  }

  public void setParentPage(PageEntity parentPage) {
    this.parentPage = parentPage;
  }

  public List<PageVersionEntity> getVersions() {
    return versions;
  }

  public void setVersions(List<PageVersionEntity> versions) {
    this.versions = versions;
  }

  public List<PageAttachmentEntity> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<PageAttachmentEntity> attachments) {
    this.attachments = attachments;
  }

  public Set<String> getWatchers() {
    return watchers;
  }

  public void setWatchers(Set<String> watchers) {
    this.watchers = watchers;
  }

  public List<PageEntity> getRelatedPages() {
    return relatedPages;
  }

  public void setRelatedPages(List<PageEntity> relatedPages) {
    this.relatedPages = relatedPages;
  }

  public List<PageMoveEntity> getMoves() {
    return moves;
  }

  public void setMoves(List<PageMoveEntity> moves) {
    this.moves = moves;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }
}
