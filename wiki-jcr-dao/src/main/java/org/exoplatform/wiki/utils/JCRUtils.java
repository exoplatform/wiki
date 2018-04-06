package org.exoplatform.wiki.utils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.mow.api.Permission;
import org.exoplatform.wiki.mow.api.PermissionEntry;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.service.IDType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;

import javax.jcr.RepositoryException;
import java.util.*;

/**
 * Utility methods for JCR
 */
public class JCRUtils {

  private static final Log log = ExoLogger.getLogger(JCRUtils.class);

  private static final String JCR_WEBDAV_SERVICE_BASE_URI = "/jcr";

  public static String getCurrentRepositoryWebDavUri() {
    StringBuilder sb = new StringBuilder();
    sb.append(Utils.getDefaultRestBaseURI());
    sb.append(JCR_WEBDAV_SERVICE_BASE_URI);
    sb.append("/");
    RepositoryService repositoryService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(RepositoryService.class);
    try {
      sb.append(repositoryService.getCurrentRepository().getConfiguration().getName());
    } catch (RepositoryException e) {
      sb.append(repositoryService.getConfig().getDefaultRepositoryName());
    }
    sb.append("/");
    return sb.toString();
  }

  public static SessionProvider createSystemProvider() {
    SessionProviderService sessionProviderService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SessionProviderService.class);
    return sessionProviderService.getSystemSessionProvider(null);
  }

  /**
   * Get the wiki type by its JCR path
   * @param jcrPath absolute jcr path of page node.
   * @return type of wiki page.
   */
  public static String getWikiType(String jcrPath) throws IllegalArgumentException {
    if (jcrPath.startsWith("/exo:applications/")) {
      return PortalConfig.PORTAL_TYPE;
    } else if (jcrPath.startsWith("/Groups/")) {
      return PortalConfig.GROUP_TYPE;
    } else if (jcrPath.startsWith("/Users/")) {
      return PortalConfig.USER_TYPE;
    } else {
      throw new IllegalArgumentException(jcrPath + " is not jcr path of a wiki page node!");
    }
  }

  /**
   * Has permission.
   *
   * @param acl
   *          access control list
   * @param permission
   *          permissions array
   * @param user
   *          user Identity
   * @return boolean
   */
  public static boolean hasPermission(AccessControlList acl,String[] permission, Identity user) {

    String userId = user.getUserId();
    if (userId.equals(IdentityConstants.SYSTEM)) {
      // SYSTEM has permission everywhere
      return true;
    } else if (userId.equals(acl.getOwner())) {
      // Current user is owner of node so has all privileges
      return true;
    } else if (userId.equals(IdentityConstants.ANONIM)) {
      List<String> anyPermissions = acl.getPermissions(IdentityConstants.ANY);

      if (anyPermissions.size() < permission.length)
        return false;

      for (int i = 0; i < permission.length; i++) {
        if (!anyPermissions.contains(permission[i]))
          return false;
      }
      return true;
    } else {
      if (acl.getPermissionsSize() > 0 && permission.length > 0) {
        // check permission to perform all of the listed actions
        for (int i = 0; i < permission.length; i++) {
          // check specific actions
          if (!isPermissionMatch(acl.getPermissionEntries(), permission[i], user))
            return false;
        }
        return true;
      }
      return false;
    }
  }
  /**
   * Has permission.
   *
   * @param permission
   *          permissions array
   * @param user
   *          user Identity
   * @param pageParams
   *          wikiPage parameter
   * @return boolean
   */
  public static boolean hasPermission(String[] permission, Identity user, WikiPageParams pageParams) {
    UserACL userACL = Util.getUIPortalApplication().getApplicationComponent(UserACL.class);
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    List<PermissionEntry> permissionEntries = new ArrayList<>();
    try {
      permissionEntries = wikiService.getWikiPermission(pageParams.getType(), pageParams.getOwner());
    } catch (Exception e) {
      log.error("Cannot get permissions of wiki " + pageParams.getType() + ":" + pageParams.getOwner() + " - Cause : " + e.getMessage(), e);
    }
    List<AccessControlEntry> aces = new ArrayList<>();
    for (PermissionEntry permissionEntry : permissionEntries) {
      Permission[] perms = permissionEntry.getPermissions();
      for (Permission perm : perms) {
        if (perm.isAllowed()) {
          AccessControlEntry ace = new AccessControlEntry(permissionEntry.getId(), perm.getPermissionType().toString());
          aces.add(ace);
        }
      }
    }
    AccessControlList acl = new AccessControlList(userACL.getSuperUser(), aces);
    return hasPermission(acl,permission,user);
  }

  private static boolean isPermissionMatch(List<AccessControlEntry> existedPermission, String testPermission, Identity user) {
    for (int i = 0, length = existedPermission.size(); i < length; i++) {
      AccessControlEntry ace = existedPermission.get(i);
      // match action
      if (testPermission.equals(ace.getPermission())) {
        // match any
        if (IdentityConstants.ANY.equals(ace.getIdentity()))
          return true;
        else if (ace.getIdentity().indexOf(":") == -1) {
          // just user
          if (ace.getIdentity().equals(user.getUserId()))
            return true;

        } else if (user.isMemberOf(ace.getMembershipEntry()))
          return true;
      }
    }
    return false;
  }

  public static String[] getAllPermissionText(){
    return new String[] {
            org.exoplatform.services.jcr.access.PermissionType.READ,
            org.exoplatform.services.jcr.access.PermissionType.ADD_NODE,
            org.exoplatform.services.jcr.access.PermissionType.REMOVE,
            org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY};
  }

  public static String getReadPermissionText(){
    return org.exoplatform.services.jcr.access.PermissionType.READ;
  }

  private static String getAddNodePermissionText(){
    return org.exoplatform.services.jcr.access.PermissionType.ADD_NODE;
  }

  private static String getRemovePermissionText(){
    return org.exoplatform.services.jcr.access.PermissionType.REMOVE;
  }

  private static String getSetPropertyPermissionText(){
    return org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY;
  }

  public static List<PermissionEntry> convertToPermissionEntryList(HashMap<String, String[]> permissions) {
    List<PermissionEntry> permissionEntries = new ArrayList<PermissionEntry>();
    Set<Map.Entry<String, String[]>> entries = permissions.entrySet();
    for (Map.Entry<String, String[]> entry : entries) {
      PermissionEntry permissionEntry = new PermissionEntry();
      String key = entry.getKey();
      IDType idType = IDType.USER;
      if (key.indexOf(":") > 0) {
        idType = IDType.MEMBERSHIP;
      } else if (key.indexOf("/") == 0) {
        idType = IDType.GROUP;
      }
      permissionEntry.setIdType(idType);
      permissionEntry.setId(key);
      Permission[] perms = new Permission[2];
      perms[0] = new Permission();
      perms[0].setPermissionType(PermissionType.VIEWPAGE);
      perms[1] = new Permission();
      perms[1].setPermissionType(PermissionType.EDITPAGE);
      for (String action : entry.getValue()) {
        if (getReadPermissionText().equals(action)) {
          perms[0].setAllowed(true);
        } else if (getAddNodePermissionText().equals(action)
                || getRemovePermissionText().equals(action)
                || getSetPropertyPermissionText().equals(action)) {
          perms[1].setAllowed(true);
        }
      }
      permissionEntry.setPermissions(perms);

      permissionEntries.add(permissionEntry);
    }
    return permissionEntries;
  }

  public static HashMap<String, String[]> convertToPermissionMap(List<PermissionEntry> permissionEntries) {
    HashMap<String, String[]> permissionMap = new HashMap<String, String[]>();
    for (PermissionEntry permissionEntry : permissionEntries) {
      Permission[] permissions = permissionEntry.getPermissions();
      List<String> permlist = new ArrayList<String>();
      for (int i = 0; i < permissions.length; i++) {
        Permission permission = permissions[i];
        if (permission.isAllowed()) {
          if (permission.getPermissionType().equals(PermissionType.VIEWPAGE)) {
            permlist.add(getReadPermissionText());
          } else if (permission.getPermissionType().equals(PermissionType.EDITPAGE)) {
            permlist.add(getAddNodePermissionText());
            permlist.add(getRemovePermissionText());
            permlist.add(getSetPropertyPermissionText());
          }
        }
      }
      if (permlist.size() > 0) {
        permissionMap.put(permissionEntry.getId(), permlist.toArray(new String[permlist.size()]));
      }
    }
    return permissionMap;
  }

  public static List<PermissionEntry> convertWikiPermissionsToPermissionEntryList(List<String> permissions) {
    List<PermissionEntry> permissionEntries = new ArrayList<>();
    if(permissions != null) {
      for (String perm : permissions) {
        String[] actions = perm.substring(0, perm.indexOf(":")).split(",");
        perm = perm.substring(perm.indexOf(":") + 1);
        String idType = perm.substring(0, perm.indexOf(":"));
        String id = perm.substring(perm.indexOf(":") + 1);

        PermissionEntry entry = new PermissionEntry();
        if (IDType.USER.toString().equals(idType)) {
          entry.setIdType(IDType.USER);
        } else if (IDType.GROUP.toString().equals(idType)) {
          entry.setIdType(IDType.GROUP);
        } else if (IDType.MEMBERSHIP.toString().equals(idType)) {
          entry.setIdType(IDType.MEMBERSHIP);
        }
        entry.setId(id);
        Permission[] perms = new Permission[4];
        perms[0] = new Permission();
        perms[0].setPermissionType(PermissionType.VIEWPAGE);
        perms[1] = new Permission();
        perms[1].setPermissionType(PermissionType.EDITPAGE);
        perms[2] = new Permission();
        perms[2].setPermissionType(PermissionType.ADMINPAGE);
        perms[3] = new Permission();
        perms[3].setPermissionType(PermissionType.ADMINSPACE);
        for (String action : actions) {
          if (PermissionType.VIEWPAGE.toString().equals(action)) {
            perms[0].setAllowed(true);
          } else if (PermissionType.EDITPAGE.toString().equals(action)) {
            perms[1].setAllowed(true);
          } else if (PermissionType.ADMINPAGE.toString().equals(action)) {
            perms[2].setAllowed(true);
          } else if (PermissionType.ADMINSPACE.toString().equals(action)) {
            perms[3].setAllowed(true);
          }
        }
        entry.setPermissions(perms);

        permissionEntries.add(entry);
      }
    }

    return permissionEntries;
  }

  /**
   * Convert permission entries list to wiki permissions (VIEWPAGE,EDITPAGE,:USER:mary)
   * @param permissionEntries List of permissions entries
   * @return List of wiki permissions
   */
  public static List<String> convertPermissionEntryListToWikiPermissions(List<PermissionEntry> permissionEntries) {
    List<String> wikiPermissions = new ArrayList<>();
    if(permissionEntries != null) {
      for(PermissionEntry permissionEntry : permissionEntries) {
        StringBuilder wikiPermission = new StringBuilder();
        for(Permission permission : permissionEntry.getPermissions()) {
          if(permission.isAllowed()) {
            wikiPermission.append(permission.getPermissionType().toString()).append(",");
          }
        }
        wikiPermission.append(":")
                .append(permissionEntry.getIdType().toString())
                .append(":")
                .append(permissionEntry.getId());
        wikiPermissions.add(wikiPermission.toString());
      }
    }

    return wikiPermissions;
  }

}
