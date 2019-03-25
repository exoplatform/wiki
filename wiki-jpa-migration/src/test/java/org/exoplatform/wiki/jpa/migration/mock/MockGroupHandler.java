package org.exoplatform.wiki.jpa.migration.mock;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.impl.GroupImpl;
import org.exoplatform.services.organization.impl.UserImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class MockGroupHandler implements GroupHandler {
  Map<String, Group> groups = new HashMap<>();

  @Override
  public Group createGroupInstance() {
    return new GroupImpl();
  }

  @Override
  public void createGroup(Group group, boolean b) throws Exception {
    groups.put(group.getGroupName(), group);
  }

  @Override
  public void addChild(Group group, Group group1, boolean b) throws Exception {

  }

  @Override
  public void saveGroup(Group group, boolean b) throws Exception {
    groups.put(group.getGroupName(), group);
  }

  @Override
  public Group removeGroup(Group group, boolean b) throws Exception {
    return null;
  }

  @Override
  public Collection<Group> findGroupByMembership(String s, String s1) throws Exception {
    return null;
  }

  @Override
  public Collection<Group> resolveGroupByMembership(String s, String s1) throws Exception {
    return null;
  }

  @Override
  public Group findGroupById(String groupId) throws Exception {
    return groups.get(groupId);
  }

  @Override
  public Collection<Group> findGroups(Group group) throws Exception {
    return null;
  }

  @Override
  public Collection<Group> findGroupsOfUser(String s) throws Exception {
    return null;
  }

  @Override
  public ListAccess<Group> findGroupsByKeyword(String s) throws Exception {
    return null;
  }

  @Override
  public Collection<Group> getAllGroups() throws Exception {
    return groups.values();
  }

  @Override
  public void addGroupEventListener(GroupEventListener groupEventListener) {

  }

  @Override
  public void removeGroupEventListener(GroupEventListener groupEventListener) {

  }
}
