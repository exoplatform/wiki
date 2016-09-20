package org.exoplatform.wiki.jpa.migration.mock;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.impl.UserImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class MockUserHandler implements UserHandler {
  Map<String, User> users = new HashMap<>();

  @Override
  public User createUserInstance() {
    return new UserImpl();
  }

  @Override
  public User createUserInstance(String username) {
    return new UserImpl(username);
  }

  @Override
  public void createUser(User user, boolean b) throws Exception {
    users.put(user.getUserName(), user);
  }

  @Override
  public void saveUser(User user, boolean b) throws Exception, DisabledUserException {

  }

  @Override
  public User removeUser(String s, boolean b) throws Exception {
    return null;
  }

  @Override
  public User setEnabled(String s, boolean b, boolean b1) throws Exception, UnsupportedOperationException {
    return null;
  }

  @Override
  public User findUserByName(String username) throws Exception {
    return users.get(username);
  }

  @Override
  public User findUserByName(String s, UserStatus userStatus) throws Exception {
    return null;
  }

  @Override
  public PageList<User> findUsersByGroup(String s) throws Exception {
    return null;
  }

  @Override
  public ListAccess<User> findUsersByGroupId(String s) throws Exception {
    return null;
  }

  @Override
  public ListAccess<User> findUsersByGroupId(String s, UserStatus userStatus) throws Exception {
    return null;
  }

  @Override
  public PageList<User> getUserPageList(int i) throws Exception {
    return null;
  }

  @Override
  public ListAccess<User> findAllUsers() throws Exception {
    return new ListAccessImpl<>(User.class, new ArrayList<>(users.values()));
  }

  @Override
  public ListAccess<User> findAllUsers(UserStatus userStatus) throws Exception {
    return null;
  }

  @Override
  public PageList<User> findUsers(Query query) throws Exception {
    return null;
  }

  @Override
  public ListAccess<User> findUsersByQuery(Query query) throws Exception {
    return null;
  }

  @Override
  public ListAccess<User> findUsersByQuery(Query query, UserStatus userStatus) throws Exception {
    return null;
  }

  @Override
  public boolean authenticate(String s, String s1) throws Exception, DisabledUserException {
    return false;
  }

  @Override
  public void addUserEventListener(UserEventListener userEventListener) {

  }

  @Override
  public void removeUserEventListener(UserEventListener userEventListener) {

  }
}
