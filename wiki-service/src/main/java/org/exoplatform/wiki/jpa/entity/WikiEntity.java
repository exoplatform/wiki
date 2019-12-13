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
 *          exo@exoplatform.com
 * Jun 23, 2015
 */
@Entity(name = "WikiWikiEntity")
@ExoEntity
@Table(name = "WIKI_WIKIS")
@NamedQueries({
        @NamedQuery(name = "wiki.getAllIds", query = "SELECT w.id FROM WikiWikiEntity w ORDER BY w.id"),
        @NamedQuery(name = "wiki.getWikisByType", query = "SELECT w FROM WikiWikiEntity w WHERE w.type = :type"),
        @NamedQuery(name = "wiki.getWikiByTypeAndOwner", query = "SELECT w FROM WikiWikiEntity w WHERE w.type = :type AND w.owner = :owner")
})
public class WikiEntity {
  @Id
  @Column(name = "WIKI_ID")
  @SequenceGenerator(name="SEQ_WIKI_WIKIS_WIKI_ID", sequenceName="SEQ_WIKI_WIKIS_WIKI_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_WIKI_WIKIS_WIKI_ID")
  private long id;

  @Column(name = "NAME")
  private String name;

  @Column(name = "OWNER")
  private String owner;

  @Column(name = "TYPE")
  private String type;

  @OneToOne
  @JoinColumn(name = "WIKI_HOME")
  private PageEntity wikiHome;

  @ElementCollection
  @CollectionTable(
          name = "WIKI_WIKI_PERMISSIONS",
          joinColumns=@JoinColumn(name = "WIKI_ID")
  )
  private List<PermissionEntity> permissions;

  @Column(name = "SYNTAX")
  private String syntax;

  @Column(name = "ALLOW_MULTI_SYNTAX")
  private boolean allowMultipleSyntax;

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public WikiEntity setName(String name) {
    this.name = name;
    return this;
  }

  public String getOwner() {
    return owner;
  }

  public WikiEntity setOwner(String owner) {
    this.owner = owner;
    return this;
  }

  public String getType() {
    return type;
  }

  public WikiEntity setType(String type) {
    this.type = type;
    return this;
  }

  public PageEntity getWikiHome() {
    return wikiHome;
  }

  public WikiEntity setWikiHome(PageEntity wikiHome) {
    this.wikiHome = wikiHome;
    return this;
  }

  public List<PermissionEntity> getPermissions() {
    return permissions;
  }

  public WikiEntity setPermissions(List<PermissionEntity> permissions) {
    this.permissions = permissions;
    return this;
  }

  public String getSyntax() {
    return syntax;
  }

  public void setSyntax(String syntax) {
    this.syntax = syntax;
  }

  public boolean isAllowMultipleSyntax() {
    return allowMultipleSyntax;
  }

  public void setAllowMultipleSyntax(boolean allowMultipleSyntax) {
    this.allowMultipleSyntax = allowMultipleSyntax;
  }
}
