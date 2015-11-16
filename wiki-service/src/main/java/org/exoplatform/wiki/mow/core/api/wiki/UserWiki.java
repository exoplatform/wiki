/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.mow.core.api.wiki;

import org.chromattic.api.RelationshipType;
import org.chromattic.api.annotations.Create;
import org.chromattic.api.annotations.ManyToOne;
import org.chromattic.api.annotations.MappedBy;
import org.chromattic.api.annotations.OneToOne;
import org.chromattic.api.annotations.Owner;
import org.chromattic.api.annotations.PrimaryType;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.mow.api.WikiType;

/**
 * @version $Revision$
 */
@PrimaryType(name = WikiNodeType.USER_WIKI)
public abstract class UserWiki extends WikiImpl {
  
  public WikiType getWikiType() {
    return WikiType.USER;
  }

  @Override
  public String getType() {
    return PortalConfig.USER_TYPE;
  }
  
  public PageImpl getDraftPagesContainer() {
    PageImpl page = getDraftPagesContainerByChromattic();
    if (page == null) {
      page = createWikiPage();
      setDraftPagesContainerByChromattic(page);
    }
    return page;
  }
  
  @ManyToOne(type = RelationshipType.REFERENCE)
  @MappedBy(WikiNodeType.Definition.WIKI_CONTAINER_REFERENCE)
  public abstract UserWikiContainer getUserWikis();
  
  public abstract void setUserWikis(UserWikiContainer userWikiContainer);
  
  @OneToOne
  @Owner
  @MappedBy(WikiNodeType.Definition.DRAFT_PAGES)
  protected abstract PageImpl getDraftPagesContainerByChromattic();
  protected abstract void setDraftPagesContainerByChromattic(PageImpl page);
   
  @Create
  public abstract DraftPageImpl createDraftPage();
}
