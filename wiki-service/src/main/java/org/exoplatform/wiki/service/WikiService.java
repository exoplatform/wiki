/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.wiki.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.mow.core.api.wiki.Template;
import org.exoplatform.wiki.mow.core.api.wiki.TemplateContainer;
import org.exoplatform.wiki.mow.core.api.wiki.UserWiki;
import org.exoplatform.wiki.service.impl.SpaceBean;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.template.plugin.WikiTemplatePagePlugin;

/**
 * WikiService is interface provide functions for processing database
 * with wikis and pages include: add, edit, remove and searching data
 *
 * @LevelAPI Provisional
 */
public interface WikiService {

  /**
   * Create a new Wiki Page
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner The owner of the wiki
   * @param title The title of the page
   * @param parentId The parent Id of the new page
   * @return The new page
   * @throws Exception
   */
  public Page createPage(String wikiType, String wikiOwner, String title, String parentId) throws Exception;

  /**
   * Create a new template Wiki Page
   *
   * @param title The title of the template
   * @param params Parameters to create the new template
   * @return the new template
   * @throws Exception
   */
  public Template createTemplatePage(String title, WikiPageParams params) throws Exception;

  /**
   * Initialise the default template page
   *
   * @param path the page to initialize the default template page
   */
  public void initDefaultTemplatePage(String path) ;

  /**
   * Delete the Wiki Page
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @param pageId Id of the wiki page
   * @return True if the page is deleted, False if not
   * @throws Exception
   */
  public boolean deletePage(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Delete the template
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @param templateId Id of the template
   * @throws Exception
   */
  public void deleteTemplatePage(String wikiType, String wikiOwner, String templateId) throws Exception;

  /**
   * Delete the draft based on is Id
   *
   * @param draftNewPageId Id of the draft
   * @throws Exception
   */
  public void deleteDraftNewPage(String draftNewPageId) throws Exception;

  /**
   * Rename the wiki Page
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wik
   * @param pageName The old name of the page
   * @param newName The new name of the page
   * @param newTitle The new title of the page
   * @return True if the page is renamed, False if not
   * @throws Exception
   */
  public boolean renamePage(String wikiType, String wikiOwner, String pageName, String newName, String newTitle) throws Exception;

  /**
   * Rename the wiki Page
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wik
   * @param pageName The old name of the page
   * @param newName The new name of the page
   * @param newTitle The new title of the page
   * @param createNewVersion Indicate if new version is created 
   * @return True if the page is renamed, False if not
   * @throws Exception
   */
  public boolean renamePage(String wikiType, String wikiOwner, String pageName, String newName, String newTitle,
                            boolean createNewVersion) throws Exception;

  /**
   * Move a wiki Page
   *
   * @param currentLocationParams The current location of the page
   * @param newLocationParams The new location of the page
   * @return True if the page is moved, False if not
   * @throws Exception
   */
  public boolean movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws Exception;

  /**
   * Return a lists permissions for the wiki based on the type and the owner
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wik
   * @return  List of permissions
   * @throws Exception
   */
  public List<PermissionEntry> getWikiPermission(String wikiType, String wikiOwner) throws Exception;

  /**
   * Add new permissions to the wiki
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wik
   * @param permissionEntries list of permissions
   * @throws Exception
   */
  public void setWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws Exception;

  /**
   * Get a page based on is Id
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wik
   * @param pageId Is the pageId used by the system
   * @return The page
   * @throws Exception
   */
  public Page getPageById(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Return the wiki Home page with the root permissions
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wik
   * @param pageId Is the pageId used by the system
   * @return The page
   * @throws Exception
   */
  public Page getPageByRootPermission(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Return the related page based on is Id
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wik
   * @param pageId Is the pageId used by the system
   * @return The related page
   * @throws Exception
   */
  public Page getRelatedPage(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Return the page itself or is draft if one exist
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wik
   * @param pageId Is the pageId used by the system
   * @return wiki Page
   * @throws Exception
   */
  public Page getExsitedOrNewDraftPageById(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Retugn a wiki page based on is id
   *
   * @param uuid Id of the wiki page
   * @return Wiki page
   * @throws Exception
   */
  public Page getPageByUUID(String uuid) throws Exception;

  /**
   * Return the template to use for the wiki page
   * @param params
   * @param templateId
   * @return A wiki template
   * @throws Exception
   */
  public Template getTemplatePage(WikiPageParams params, String templateId) throws Exception;

  /**
   * Return a list of data to compose the breadcrumb
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wik
   * @param pageId Is the pageId used by the system
   * @return List of {@link BreadcrumbData}
   * @throws Exception
   */
  public List<BreadcrumbData> getBreadcumb(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Get a wiki page based on the data stored in the breadcrumb
   *
   * @param data A part of the breadcrumb
   * @return  Wiki Page params
   * @throws Exception
   */
  public WikiPageParams getWikiPageParams(BreadcrumbData data) throws Exception;

  /**
   * Search in all wiki pages
   *
   * @param data The data to search
   * @return List of results
   * @throws Exception
   */
  public PageList<SearchResult> search(WikiSearchData data) throws Exception;

  /**
   * Search in all template
   *
   * @param data The data to search
   * @return List of results
   * @throws Exception
   */
  public List<TemplateSearchResult> searchTemplate(TemplateSearchData data) throws Exception;

  /**
   * Search pages in the list of renamed pages
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wik
   * @param pageId Is the pageId used by the system
   * @return List of results
   * @throws Exception
   */
  public List<SearchResult> searchRenamedPage(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Get a list of duppilcated page between all children pages of parentPage and targetWiki before execute moving page
   * 
   * @param parentPage The page to check before execute moving
   * @param targetWiki The target wiki to move page to
   * @param resultList The list of duppicate wiki page
   * @return The list of duppicate wiki page
   * @throws Exception
   */
  public List<PageImpl> getDuplicatePages(PageImpl parentPage, Wiki targetWiki, List<PageImpl> resultList) throws Exception;

  /**
   * Find the fullpath based on the type and is relative path.
   *
   * @param path relative path to search
   * @param objectNodeType Can be a page, attachment or template
   * @return An object based on is path and type
   * @throws Exception
   */
  public Object findByPath(String path, String objectNodeType) throws Exception;

  /**
   * Get the default wiki syntax Id
   *
   * @return the default wiki syntax Id
   */
  public String getDefaultWikiSyntaxId();

  /**
   * Get the draft save sequence time from config file
   * 
   * @return The save draft sequence time
   */
  public long getSaveDraftSequenceTime();

  /**
   * Get the page title of an attachment
   *
   * @param path Path of the attachment
   * @return title of the page
   * @throws Exception
   */
  public String getPageTitleOfAttachment(String path) throws Exception;

  /**
   * Return an attachment as stream
   *
   * @param path Path to use to get the attachment
   * @return Stream of the attachment
   * @throws Exception
   */
  public InputStream getAttachmentAsStream(String path) throws Exception;

  /**
   * Return the helps syntax page based on the syntax id
   *
   * @param syntaxId Id of the syntax
   * @return Wiki Page
   * @throws Exception
   */
  public PageImpl getHelpSyntaxPage(String syntaxId) throws Exception;

  /**
   * Return the a page of metadata
   *
   * @param metaPage the metadata to use, mainly emoticons
   * @return page of metadata
   * @throws Exception
   */
  public Page getMetaDataPage(MetaDataPage metaPage) throws Exception;

  /**
   * Return a map of templates for the wiki page
   *
   * @param params The full params to get a page
   * @return Map of templates
   * @throws Exception
   */
  public Map<String, Template> getTemplates(WikiPageParams params) throws Exception;

  /**
   * Return the template container
   *
   * @param params The full params to get a page
   * @return  the template container
   * @throws Exception
   */
  public TemplateContainer getTemplatesContainer(WikiPageParams params) throws Exception;

  /**
   * Modify an existing template available
   *
   * @param params The full params to get a page
   * @param template the template
   * @param newName the new name for the template
   * @param newDescription the new description for the template
   * @param newContent the new description for the template
   * @param newSyntaxId the new syntax for the template
   * @throws Exception
   */
  public void modifyTemplate(WikiPageParams params, Template template, String newName, String newDescription, String newContent, String newSyntaxId) throws Exception;

  /**
   * Return true if the page exist
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @param pageId Is the pageId used by the system
   * @return true if the page exist, false if not
   * @throws Exception
   */
  public boolean isExisting(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Get wiki default permission
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @return The permisison list for wiki
   * @throws Exception
   */
  public List<String> getWikiDefaultPermissions(String wikiType, String wikiOwner) throws Exception;
  
  /**
   * register a {@link ComponentPlugin}
   * @param plugin
   */
  public void addComponentPlugin(ComponentPlugin plugin);

  /**
   * Add the wiki page as plugin
   *
   * @param templatePlugin The template plugin to use
   */
  public void addWikiTemplatePagePlugin(WikiTemplatePagePlugin templatePlugin);

  /**
   * @return list of {@link PageWikiListener}
   */
  public List<PageWikiListener> getPageListeners();

  /**
   * Add a related page of the current wiki page
   *
   * @param orginaryPageParams Current wiki page param
   * @param relatedPageParams Param of the related page
   * @return true if it has been added or false if it's not possible
   * @throws Exception
   */
  public boolean addRelatedPage(WikiPageParams orginaryPageParams, WikiPageParams relatedPageParams) throws Exception;

  /**
   * Return a list of related page based on its param
   * @param pageParams Param of the wiki page
   * @return List of related page
   * @throws Exception
   */
  public List<Page> getRelatedPage(WikiPageParams pageParams) throws Exception;

  /**
   * Remove a related page of the current wiki page
   * @param orginaryPageParams Current wiki page param
   * @param relatedPageParams Param of the related page
   * @return true if it has been removed or false if it's not possible
   * @throws Exception
   */
  public boolean removeRelatedPage(WikiPageParams orginaryPageParams, WikiPageParams relatedPageParams) throws Exception;
  
  /**
   * Create a draft page for a wiki page which is specified by the wiki page param
   * 
   * @param param wiki page param
   * @param revision the target revision, null if it's the lastest revision
   * @param clientTime The time of client when save draft
   * @return draft page
   * @throws Exception if create draft not success
   */
  public DraftPage createDraftForExistPage(WikiPageParams param, String revision, long clientTime) throws Exception;
  
  /**
   * Create a draft page for a new wiki page which parent is specified by the wiki page param
   * 
   * @param parentPageParam parent wiki page param
   * @param clientTime The time of client when save draft
   * @return draft page
   * @throws Exception if create draft not success
   */
  public DraftPage createDraftForNewPage(WikiPageParams parentPageParam, long clientTime) throws Exception;
  
  /**
   * Achieve a draft page for a wiki page which is specified by the wiki page param
   * 
   * @param param wiki page param
   * @return draft page or null if draft page doesn't exist.
   * @throws Exception
   */
  public DraftPage getDraft(WikiPageParams param) throws Exception;
  
  /**
   * Get draft by draft name
   * 
   * @param draftName draft name
   * @return draft page or null if draft page doesn't exist.
   * @throws Exception
   */
   public DraftPage getDraft(String draftName) throws Exception;
  
  /**
    * Remove a draft page for a wiki page which is specified by the wiki page param
    * 
    * @param param wiki page param
    * @throws Exception
    */
  public void removeDraft(WikiPageParams param) throws Exception;
  
  /**
   * Remove a draft page by draft name
   * 
   * @param draftName draft name
   * @throws Exception
   */
  public void removeDraft(String draftName) throws Exception;
  
  /**
   * Get collection of draft page belong to a user
   * 
   * @param username user name
   * @return draft list of user
   * @throws Exception
   */
  public List<DraftPage> getDrafts(String username) throws Exception;
  
  /**
   * Get wiki page by page UUID
   * 
   * @param uuid of wiki page
   * @return wiki page
   * @throws Exception
   */
  public Page getWikiPageByUUID(String uuid) throws Exception;
  
  /**
   * Get the draft that's created lastest
   * 
   * @return lastest draft
   * @throws Exception
   */
  public DraftPage getLastestDraft() throws Exception;

  /**
   * Get user wiki, and if it did not create yet then create new one
   * 
   * @param username The user name
   * @return The UserWiki for user
   */
  public UserWiki getOrCreateUserWiki(String username);
 
  /**
   * Get space name by group Id
   * 
   * @param groupId The group Id to get space name
   * @return The space name
   * @throws Exception
   */
  public String getSpaceNameByGroupId(String groupId) throws Exception;
  
  /**
   * Search for spaces by keyword
   * 
   * @param keyword The keyword to search spaces
   * @return The list of spaces that match wiki keyword
   * @throws Exception
   */
  public List<SpaceBean> searchSpaces(String keyword) throws Exception;
  
  /**
   * Get a wiki that definds by wikiType and owner
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param owner Wiki owner
   * @return The wiki
   */
  public Wiki getWiki(String wikiType, String owner);
  
  /**
   * Get portal owner
   * 
   * @return portal owner
   */
  public String getPortalOwner();
  
  /**
   * Get the uri of wiki webapp
   * 
   * @return wiki webapp uri
   */
  public String getWikiWebappUri();
  
  /**
   * Checks whether a user is a space's member or not.
   *
   * @param spaceId  the existing space id
   * @param userId the remote user id
   * @return true if that user is a member; otherwise, false
   */
  public boolean isSpaceMember(String spaceId, String userId);

  /**
   * Check if the space is hidden or not
   * 
   * @param groupId The group Id to check
   * @return the space is hidden or not
   * @throws Exception
   */
  public boolean isHiddenSpace(String groupId) throws Exception;
   
  /** 
   * Check if the current user has addmin permission on the space
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param owner The owner of the space
   * @return current user has addmin permisison on the space or not
   * @throws Exception
   */
  public boolean hasAdminSpacePermission(String wikiType, String owner) throws Exception;
  
  /**
   * Check if the current user has addmin permission on the page
   * 
   * @param wikiType The wiki type of the space
   * @param owner The owner of the space
   * @return current user has addmin permisison on the page or not
   * @throws Exception
   */
  public boolean hasAdminPagePermission(String wikiType, String owner) throws Exception;
  
  /**
   * Publish a update activity
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner The wiki owner
   * @param pageId The page id
   * @param page The wiki page
   * @param wikiUpdateType The update type
   * @throws Exception
   */
  public void postUpdatePage(String wikiType, String wikiOwner, String pageId, Page page, String wikiUpdateType) throws Exception;
  
  /**
   * Publish a add activity
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner The wiki owner
   * @param pageId The page id
   * @param page The wiki page
   * @throws Exception
   */
  public void postAddPage(final String wikiType, final String wikiOwner, final String pageId, Page page) throws Exception;
  
  /**
   * Publish a delete page activity
   *
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner The wiki owner
   * @param pageId The page id
   * @param page The wiki page
   * @throws Exception
   */
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws Exception;

  /**
   * Get wiki by Id
   * 
   * @param wikiId The wiki id
   * @return The wiki
   */
  public Wiki getWikiById(String wikiId);
  
  /**
   * Get wiki name by wiki id
   *
   * @param wikiId The wiki id
   * @return The wiki name
   * @throws Exception
   */
  public String getWikiNameById(String wikiId) throws Exception;
}
