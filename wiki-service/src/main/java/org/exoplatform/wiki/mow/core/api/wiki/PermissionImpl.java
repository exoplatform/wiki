/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

import org.chromattic.api.ChromatticSession;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Permission;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.utils.Utils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class PermissionImpl {
  private static final Log log = ExoLogger.getLogger(PermissionImpl.class);

  protected MOWService mowService;

  public void setMOWService(MOWService mowService) {
    this.mowService = mowService;
  }

  public MOWService getMOWService() {
    return mowService;
  }

  public HashMap<String, String[]> getPermission(String jcrPath) throws WikiException {
    try {
      ExtendedNode extendedNode = (ExtendedNode) getJCRNode(jcrPath);
      HashMap<String, String[]> perm = new HashMap<>();
      AccessControlList acl = extendedNode.getACL();
      List<AccessControlEntry> aceList = acl.getPermissionEntries();
      for (int i = 0, length = aceList.size(); i < length; i++) {
        AccessControlEntry ace = aceList.get(i);
        String[] nodeActions = perm.get(ace.getIdentity());
        List<String> actions;
        if (nodeActions != null) {
          actions = new ArrayList<>(Arrays.asList(nodeActions));
        } else {
          actions = new ArrayList<>();
        }
        actions.add(ace.getPermission());
        perm.put(ace.getIdentity(), actions.toArray(new String[actions.size()]));
      }
      return perm;
    } catch (Exception e) {
      throw new WikiException("Cannot get permissions of node " + jcrPath, e);
    }
  }

  public boolean hasPermission(PermissionType permissionType, String jcrPath) {
    ConversationState conversationState = ConversationState.getCurrent();
    Identity user;
    if (conversationState != null) {
      user = conversationState.getIdentity();
    } else {
      user = new Identity(IdentityConstants.ANONIM);
    }
    return hasPermission(permissionType, jcrPath, user);
  }

  public boolean hasPermission(PermissionType permissionType, String jcrPath, Identity user) {
    // Convert permissionType to JCR permission
    String[] permission = new String[] {};
    if (PermissionType.VIEWPAGE.equals(permissionType) || PermissionType.VIEW_ATTACHMENT.equals(permissionType)) {
      permission = new String[] { org.exoplatform.services.jcr.access.PermissionType.READ };
    } else if (PermissionType.EDITPAGE.equals(permissionType) || PermissionType.EDIT_ATTACHMENT.equals(permissionType)) {
      permission = new String[] { org.exoplatform.services.jcr.access.PermissionType.ADD_NODE,
          org.exoplatform.services.jcr.access.PermissionType.REMOVE,
          org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY };
    }

    try {
      // Get ACL
      ExtendedNode extendedNode = (ExtendedNode) getJCRNode(jcrPath);
      AccessControlList acl = extendedNode.getACL();

      return Utils.hasPermission(acl, permission, user);
    } catch(RepositoryException e) {
      log.error("Cannot check permissions of user " + user.getUserId() + " on node " + jcrPath
              + " - Cause : " + e.getMessage(), e);
      return false;
    }
  }

  public void setPermission(HashMap<String, String[]> permissions, String jcrPath) throws WikiException {
    getChromatticSession().save();
    try {
      ExtendedNode extendedNode = (ExtendedNode) getJCRNode(jcrPath);
      if (extendedNode.canAddMixin("exo:privilegeable")) {
        extendedNode.addMixin("exo:privilegeable");
      }

      if (permissions != null && permissions.size() > 0) {
        extendedNode.setPermissions(permissions);
      } else {
        extendedNode.clearACL();
        extendedNode.setPermission(IdentityConstants.ANY, org.exoplatform.services.jcr.access.PermissionType.ALL);
      }
    } catch(RepositoryException e) {
      throw new WikiException("Cannot set permissions on node " + jcrPath, e);
    }
  }

  protected ChromatticSession getChromatticSession() {
    return mowService.getSession();
  }

  protected Node getJCRNode(String path) throws RepositoryException {
    return (Node) getChromatticSession().getJCRSession().getItem(path);
  }
}
