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
package org.exoplatform.wiki.mow.core.api;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

import org.chromattic.api.ChromatticSession;
import org.chromattic.api.annotations.Create;
import org.chromattic.api.annotations.MappedBy;
import org.chromattic.api.annotations.OneToOne;
import org.chromattic.api.annotations.Owner;
import org.chromattic.api.annotations.PrimaryType;
import org.exoplatform.wiki.mow.core.api.wiki.WikiNodeType;
import org.exoplatform.wiki.mow.core.api.wiki.WikiStore;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.wiki.*;

/**
 * A Wiki store for portal, group and user wikis
 *
 * @version $Revision$
 */
@PrimaryType(name = WikiNodeType.WIKI_STORE)
public abstract class WikiStoreImpl implements WikiStore {

  private MOWService mowService;

  public void setMOWService(MOWService mowService) {
    this.mowService = mowService;
  }

  public WikiImpl addWiki(WikiType wikiType, String name) {
    return getWikiContainer(wikiType).addWiki(name);
  }

  public WikiImpl getWiki(WikiType wikiType, String name) {
    return getWikiContainer(wikiType).getWiki(name, true);
  }

  public Collection<WikiImpl> getWikis() {
    Collection<WikiImpl> col = new CopyOnWriteArraySet<>();
    col.addAll(getPortalWikiContainer().getAllWikis());
    col.addAll(getGroupWikiContainer().getAllWikis());
    col.addAll(getUserWikiContainer().getAllWikis());
    return col;
  }

  @SuppressWarnings("unchecked")
  public  <W extends WikiImpl>WikiContainer<W> getWikiContainer(WikiType wikiType) {
    boolean created = mowService.startSynchronization();

    WikiContainer wikiContainer;
    if (wikiType == WikiType.PORTAL) {
      wikiContainer = getPortalWikiContainer();
    } else if (wikiType == WikiType.GROUP) {
      wikiContainer = getGroupWikiContainer();
    } else if (wikiType == WikiType.USER) {
      wikiContainer = getUserWikiContainer();
    } else {
      throw new UnsupportedOperationException();
    }

    mowService.stopSynchronization(created);

    return wikiContainer;
  }

  @Create
  public abstract PageImpl createPage();
  
  @Create
  public abstract HelpPage createHelpPage();
  
  public HelpPage getHelpPagesContainer() {
    boolean created = mowService.startSynchronization();

    HelpPage page = getHelpPageByChromattic();
    if (page == null) {
      page = createHelpPage();
      setHelpPageByChromattic(page);
    }

    mowService.stopSynchronization(created);

    return page;
  }
  
  public PageImpl getDraftNewPagesContainer() {
    boolean created = mowService.startSynchronization();

    PageImpl page = getDraftNewPagesContainerByChromattic();
    if (page == null) {
      page = createPage();
      setDraftNewPagesContainerByChromattic(page);
    }

    mowService.stopSynchronization(created);

    return page;
  }

  public PageImpl getEmotionIconsContainer() {
    boolean created = mowService.startSynchronization();

    PageImpl page = getEmotionIconsPageByChromattic();
    if (page == null) {
      page = createEmotionIconsPage();
      setEmotionIconsPageByChromattic(page);
    }

    mowService.stopSynchronization(created);

    return page;
  }

  @OneToOne
  @Owner
  @MappedBy(WikiNodeType.Definition.EMOTION_ICONS_PAGE)
  protected abstract PageImpl getEmotionIconsPageByChromattic();

  protected abstract void setEmotionIconsPageByChromattic(PageImpl page);

  @Create
  protected abstract PageImpl createEmotionIconsPage();

  @OneToOne
  @Owner
  @MappedBy(WikiNodeType.Definition.PORTAL_WIKI_CONTAINER_NAME)
  protected abstract PortalWikiContainer getPortalWikiContainerByChromattic();

  protected abstract void setPortalWikiContainerByChromattic(PortalWikiContainer portalWikiContainer);

  @Create
  protected abstract PortalWikiContainer createPortalWikiContainer();

  @OneToOne
  @Owner
  @MappedBy(WikiNodeType.Definition.GROUP_WIKI_CONTAINER_NAME)
  protected abstract GroupWikiContainer getGroupWikiContainerByChromattic();

  protected abstract void setGroupWikiContainerByChromattic(GroupWikiContainer groupWikiContainer);

  @Create
  protected abstract GroupWikiContainer createGroupWikiContainer();

  @OneToOne
  @Owner
  @MappedBy(WikiNodeType.Definition.USER_WIKI_CONTAINER_NAME)
  protected abstract UserWikiContainer getUserWikiContainerByChromattic();

  protected abstract void setUserWikiContainerByChromattic(UserWikiContainer userWikiContainer);

  @Create
  protected abstract UserWikiContainer createUserWikiContainer();
  
  @OneToOne
  @Owner
  @MappedBy(WikiNodeType.Definition.HELP_PAGES)
  public abstract HelpPage getHelpPageByChromattic();
  
  protected abstract void setHelpPageByChromattic(HelpPage page);
  
  @OneToOne
  @Owner
  @MappedBy(WikiNodeType.Definition.DRAFT_NEW_PAGES)
  protected abstract PageImpl getDraftNewPagesContainerByChromattic();

  protected abstract void setDraftNewPagesContainerByChromattic(PageImpl page);

  private PortalWikiContainer getPortalWikiContainer() {
    boolean created = mowService.startSynchronization();

    PortalWikiContainer portalWikiContainer = getPortalWikiContainerByChromattic();
    if (portalWikiContainer == null) {
      portalWikiContainer = createPortalWikiContainer();
      setPortalWikiContainerByChromattic(portalWikiContainer);
      mowService.persist();
    }

    mowService.stopSynchronization(created);

    return portalWikiContainer;
  }

  private GroupWikiContainer getGroupWikiContainer() {
    GroupWikiContainer groupWikiContainer = getGroupWikiContainerByChromattic();
    if (groupWikiContainer == null) {
      groupWikiContainer = createGroupWikiContainer();
      setGroupWikiContainerByChromattic(groupWikiContainer);
      mowService.persist();
    }
    return groupWikiContainer;
  }

  private UserWikiContainer getUserWikiContainer() {
    UserWikiContainer userWikiContainer = getUserWikiContainerByChromattic();
    if (userWikiContainer == null) {
      userWikiContainer = createUserWikiContainer();
      setUserWikiContainerByChromattic(userWikiContainer);
      mowService.persist();
    }
    return userWikiContainer;
  }
  
}
