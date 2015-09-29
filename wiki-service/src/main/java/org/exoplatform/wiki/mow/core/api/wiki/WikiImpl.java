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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.chromattic.api.annotations.*;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.service.PermissionType;
import org.exoplatform.wiki.service.WikiService;
import org.xwiki.rendering.syntax.Syntax;

/**
 * @version $Revision$
 */
public abstract class WikiImpl {

  private static final Log LOG           = ExoLogger.getLogger(WikiImpl.class);
  
  @Create
  public abstract PageImpl createWikiPage();

  public abstract WikiType getWikiType();
  
  public void initTemplate() {
    // TODO launch the template init at service level
    /*
    String path = getPreferences().getPath();
    wService.initDefaultTemplatePage(path);
    */
  }
  
  public WikiHome getWikiHome() {
    WikiHome home = getHome();
    if (home == null) {
      home = createWikiHome();
      setHome(home);
      home.makeVersionable();
      home.setOwner(getOwner());
      AttachmentImpl content = home.getContent();
      home.setTitle(WikiNodeType.Definition.WIKI_HOME_TITLE);
      home.setSyntax(Syntax.XWIKI_2_0.toIdString());
      StringBuilder sb = new StringBuilder("= Welcome to ");
      String spaceName = getOwner();
      
      if (getType().equals(PortalConfig.GROUP_TYPE)) {
        try{
          // TODO launch the wiki home creation at service level
          //spaceName = wService.getSpaceNameByGroupId(getOwner());
        } catch (Exception e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Can't get Space name by group ID : " + getOwner(), e);
          }
        }
      }
      sb.append(spaceName).append(" =");
      content.setText(sb.toString());
      try {
        initPermisionForWikiHome(home);
        home.checkin();
        home.checkout();
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error(e);
        }
        return home;
      }
    }
    return home;
  }
  
  private void initPermisionForWikiHome(WikiHome home) throws Exception {
    List<String> wikiPermission = getWikiPermissions();
    if (wikiPermission == null) {
      home.setNonePermission();
      return;
    }
    
    HashMap<String, String[]> permMap = new HashMap<String, String[]>();
    for (String perm : wikiPermission) {
      String[] actions = perm.substring(0, perm.indexOf(":")).split(",");
      perm = perm.substring(perm.indexOf(":") + 1);
      String id = perm.substring(perm.indexOf(":") + 1);
      List<String> jcrActions = new ArrayList<String>();
      for (String action : actions) {
        if (PermissionType.VIEWPAGE.toString().equals(action)) {
          jcrActions.add(org.exoplatform.services.jcr.access.PermissionType.READ);
        } else if (PermissionType.EDITPAGE.toString().equals(action)) {
          jcrActions.add(org.exoplatform.services.jcr.access.PermissionType.ADD_NODE);
          jcrActions.add(org.exoplatform.services.jcr.access.PermissionType.REMOVE);
          jcrActions.add(org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY);
        }
      }
      permMap.put(id, jcrActions.toArray(new String[jcrActions.size()]));
    }
    home.setPermission(permMap);
  }

  public LinkRegistry getLinkRegistry() {
    LinkRegistry linkRegistry = getLinkRegistryByChromattic();
    if (linkRegistry == null) {
      linkRegistry = createLinkRegistry();
      setLinkRegistryByChromattic(linkRegistry);
    }
    return linkRegistry;
  }

  public Trash getTrash() {
    Trash trash = getTrashByChromattic();
    if (trash == null) {
      trash = createTrash();
      setTrashByChromattic(trash);
    }
    return trash;
  }
  
  public PreferencesImpl getPreferences()
  {
    PreferencesImpl preferences = getPreferencesByChromattic();
    if (preferences == null) {
      preferences = createPreferences();
      setPreferencesByChromattic(preferences);
    }
    return preferences;
  }

  @Name
  public abstract String getName();

  @Property(name = WikiNodeType.Definition.OWNER)
  public abstract String getOwner();

  public abstract void setOwner(String wikiOwner);

  @Path
  public abstract String getPath();
  
  @Property(name = WikiNodeType.Definition.WIKI_PERMISSIONS)
  public abstract List<String> getWikiPermissions();
  public abstract void setWikiPermissions(List<String> permissions);
  
  @Property(name = WikiNodeType.Definition.DEFAULT_PERMISSIONS_INITED)
  public abstract boolean getDefaultPermissionsInited();
  public abstract void setDefaultPermissionsInited(boolean isInited);

  public PageImpl getPageByID(String id) {
    throw new UnsupportedOperationException();
  }

  public PageImpl getPageByURI(String uri) {
    throw new UnsupportedOperationException();
  }
  
  public abstract String getType();
  
  @OneToOne
  @Owner
  @MappedBy(WikiNodeType.Definition.WIKI_HOME_NAME)
  protected abstract WikiHome getHome();
  protected abstract void setHome(WikiHome homePage);
  
  @Create
  protected abstract WikiHome createWikiHome();

  @OneToOne
  @Owner
  @MappedBy(WikiNodeType.Definition.LINK_REGISTRY)
  protected abstract LinkRegistry getLinkRegistryByChromattic();
  protected abstract void setLinkRegistryByChromattic(LinkRegistry linkRegistry);

  @Create
  protected abstract LinkRegistry createLinkRegistry();
  
  @OneToOne
  @Owner
  @MappedBy(WikiNodeType.Definition.TRASH_NAME)
  protected abstract Trash getTrashByChromattic();
  protected abstract void setTrashByChromattic(Trash trash);

  @Create
  protected abstract Trash createTrash();
  
  @OneToOne
  @Owner
  @MappedBy(WikiNodeType.Definition.PREFERENCES)
  protected abstract PreferencesImpl getPreferencesByChromattic();
  protected abstract void setPreferencesByChromattic(PreferencesImpl preferences);
  
  @Create
  protected abstract PreferencesImpl createPreferences();
  
}
