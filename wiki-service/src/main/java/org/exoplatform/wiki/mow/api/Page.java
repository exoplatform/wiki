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
package org.exoplatform.wiki.mow.api;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import javax.jcr.Node;

import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.chromattic.ext.ntdef.VersionableMixin;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.service.PermissionType;

/**
 * @version $Revision$
 */
public interface Page {

  /**
   * Get jcr node name
   * 
   * @return jcr node name
   */
  String getName();
  
  /**
   * set jcr node name
   * 
   * @param name Nodename
   */
  void setName(String name);
  
  /**
   * Get the owner of the page
   * 
   * @return
   */
  String getOwner();

  /**
   * The Author is changed when any part of the document changes (content, attachments).
   */
  String getAuthor();
  
  /**
   * The date when creating page.
   */
  Date getCreatedDate();
  
  /**
   * The date when any part of the document changes (content, attachments).
   */
  Date getUpdatedDate();
  
  /**
   * Get the actual content of the page
   * 
   * @return
   */
  Attachment getContent();
  
  /**
   * Get the syntax used in that page
   * 
   * @return
   */
  String getSyntax();

  void setSyntax(String syntax);

  String getTitle();

  void setTitle(String title);
  
  String getComment();
  
  void setComment(String comment);

  /**
   * Get the attachments of this page
   * 
   * @return
   * @throws Exception 
   */
  Collection<? extends Attachment> getAttachments() throws Exception;
  
  boolean hasPermission(PermissionType permissionType) throws Exception;
  
  /**
   * Check if user has permisison on page or not
   * 
   * @param permissionType The type of permisison to check {@link PermissionType}}
   * @param user The user to check
   * @return User has permisison on page or not
   * @throws Exception
   */
  boolean hasPermission(PermissionType permissionType, Identity user) throws Exception;
  
  /**
   * Get map of permission of page
   * 
   * @return
   * @throws Exception
   */
  HashMap<String, String[]> getPermission() throws Exception;
  
  /**
   * Set permission to page
   * 
   * @param permissions
   * @throws Exception
   */
  void setPermission(HashMap<String, String[]> permissions) throws Exception;
  
  /**
   * get URL of page. The domain part of link can be fixed.
   */
  String getURL();
  
  /**
   * Add a wiki page as child page
   * 
   * @param page 
   */
  void addWikiPage(Page page);
  
  /**
   * Get JCR node of wiki page
   * 
   * @return JCR node of wiki page
   * @throws Exception
   */
  Node getJCRPageNode() throws Exception;
  
  /**
   * get Versionable Mixin
   * 
   * @return Versionable Mixin
   */
  VersionableMixin getVersionableMixin();
  
  /**
   * Detroy wiki page
   */
  void remove();
  
  /**
   * get Wiki of page
   * 
   * @return Wiki of page
   */
  Wiki getWiki();
  
  /**
   * is page in minor edit or not
   * 
   * @param isMinorEdit
   */
  void setMinorEdit(boolean isMinorEdit);
  
  /**
   * is page in minor edit or not
   * 
   * @return
   */
  public boolean isMinorEdit();
  
  /**
   * set url
   * 
   * @param url
   */
  void setURL(String url);
  
  /**
   * get the parent page
   * 
   * @return the parent page
   */
  PageImpl getParentPage();
  
  /**
   * Add a public wiki page
   * 
   * @param page 
   * @throws Exception
   */
  void addPublicPage(Page page) throws Exception;
  
  /**
   * Reset page permisison
   * 
   * @throws Exception
   */
  void setNonePermission() throws Exception;
}
