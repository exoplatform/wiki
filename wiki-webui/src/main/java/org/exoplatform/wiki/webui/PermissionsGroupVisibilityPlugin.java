package org.exoplatform.wiki.webui;

import org.exoplatform.portal.config.GroupVisibilityPlugin;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

import java.util.Collection;

public class PermissionsGroupVisibilityPlugin extends GroupVisibilityPlugin {
  private UserACL userACL;

  public PermissionsGroupVisibilityPlugin(UserACL userACL) {
    this.userACL = userACL;
  }

  public boolean hasPermission(Identity userIdentity, Group group) {
    Collection<MembershipEntry> userMemberships = userIdentity.getMemberships();
    return userACL.getSuperUser().equals(userIdentity.getUserId())
        || userMemberships.stream()
                          .anyMatch(userMembership -> userMembership.getGroup().equals(userACL.getAdminGroups())
                              || ((userMembership.getGroup().equals(group.getId())
                                  || userMembership.getGroup().startsWith(group.getId() + "/"))
                                  && (group.getId().startsWith("/spaces/")
                                      || userMembership.getMembershipType().equals("*")
                                      || userMembership.getMembershipType().equals("manager"))));
  }
}
