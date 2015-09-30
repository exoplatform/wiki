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

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.service.diff.DiffResult;
import org.exoplatform.wiki.service.impl.SpaceBean;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.template.plugin.WikiTemplatePagePlugin;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Provides functions for processing database
 * with wikis and pages, including: adding, editing, removing and searching for data.
 *
 * @LevelAPI Provisional
 */
public interface WikiService {

  /**
   * Create a new wiki page in the given wiki, under the given parent page.
   *
   * @param wiki It can be Portal, Group, or User.
   * @param parentPageName Name of the parent wiki page.
   * @return The new wiki page.
   * @throws Exception
   */
  public Page createPage(Wiki wiki, String parentPageName, Page page) throws Exception;

  /**
   * Creates a new Wiki template.
   *
   * @param title Title of the Wiki template.
   * @param params The params object which is used for creating the new Wiki template.
   * @return The new Wiki template.
   * @throws Exception
   */
  public void createTemplatePage(String title, WikiPageParams params) throws Exception;

  /**
   * Initializes a default Wiki template.
   *
   * @param path The path in which the default Wiki template is initialized.
   */
  public void initDefaultTemplatePage(String path) ;

  /**
   * Deletes a wiki page.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page.
   * @return "True" if deleting the wiki page is successful, or "false" if not.
   * @throws Exception
   */
  public boolean deletePage(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Deletes a Wiki template.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param templateName Name of the Wiki template.
   * @throws Exception
   */
  public void deleteTemplatePage(String wikiType, String wikiOwner, String templateName) throws Exception;

  /**
   * Renames a wiki page.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageName Old name of the wiki page.
   * @param newName New name of the wiki page.
   * @param newTitle New title of the wiki page.
   * @return "True" if renaming the wiki page is successful, or "false" if not.
   * @throws Exception
   */
  public boolean renamePage(String wikiType, String wikiOwner, String pageName, String newName, String newTitle) throws Exception;

  /**
   * Move a wiki Page
   *
   * @param currentLocationParams The current location of the wiki page.
   * @param newLocationParams The new location of the wiki page.
   * @return "True" if moving the wiki page is successful, or "false" if not.
   * @throws Exception
   */
  public boolean movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws Exception;

  /**
   * Gets a list of Wiki permissions based on its type and owner.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @return The list of Wiki permissions.
   * @throws Exception
   */
  public List<PermissionEntry> getWikiPermission(String wikiType, String wikiOwner) throws Exception;

  /**
   * Adds a list of permissions to Wiki.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param permissionEntries The list of permissions.
   * @throws Exception
   */
  public void setWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws Exception;

  /**
   * Gets a wiki page by its unique name in the wiki.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageName Id of the wiki page.
   * @return The wiki page if the current user has the read permission. Otherwise, it is "null".
   * @throws Exception
   */
  public Page getPageOfWikiByName(String wikiType, String wikiOwner, String pageName) throws Exception;

  /**
   * Gets a wiki page regardless of the current user's permission.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page.
   * @return The wiki page.
   * @throws Exception
   */
  public Page getPageByRootPermission(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Gets a related page of a wiki page which is specified by a given Id.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page.
   * @return The related wiki page.
   * @throws Exception
   */
  public Page getRelatedPage(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Gets a wiki page or its draft if existing by its Id.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page.
   * @return The wiki page or its draft.
   * @throws Exception
   */
  public Page getExsitedOrNewDraftPageById(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Gets a wiki page based on its unique id.
   *
   * @param id Unique id of the wiki page.
   * @return The wiki page.
   * @throws Exception
   */
  public Page getPageById(String id) throws Exception;

  /**
   * Get parent page of a wiki page
   * @param page Wiki page.
   * @return The list of children pages
   */
  public Page getParentPageOf(Page page) throws Exception;

  /**
   * Get all the children pages of a wiki page
   * @param page Wiki page.
   * @return The list of children pages
   */
  public List<Page> getChildrenPageOf(Page page) throws Exception;

  /**
   * Gets a Wiki template.
   * @param params The params object which is used for creating the Wiki template.
   * @param templateId Id of the wiki template.
   * @return The wiki template.
   * @throws Exception
   */
  public Template getTemplatePage(WikiPageParams params, String templateId) throws Exception;

  /**
   * Gets a list of data which is used for composing the breadcrumb.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page to which the breadcrumb points.
   * @return The list of data.
   * @throws Exception
   */
  public List<BreadcrumbData> getBreadcumb(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Gets parameters of a wiki page based on the data stored in the breadcrumb.
   *
   * @param data The data in the breadcrumb that identifies the wiki page.
   * @return The parameters identifying the wiki page.
   * @throws Exception
   */
  public WikiPageParams getWikiPageParams(BreadcrumbData data) throws Exception;

  /**
   * Searches in all wiki pages.
   *
   * @param data The data to search.
   * @return Search results.
   * @throws Exception
   */
  public PageList<SearchResult> search(WikiSearchData data) throws Exception;

  /**
   * Searches in all templates.
   *
   * @param data The data to search.
   * @return Search results.
   * @throws Exception
   */
  public List<TemplateSearchResult> searchTemplate(TemplateSearchData data) throws Exception;

  /**
   * Searches from a list of renamed pages to find the pages whose old Ids are equal to the given page Id.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the page before it is renamed.
   * @return The pages whose old Ids are equal to 'pageId'.
   * @throws Exception
   */
  public List<SearchResult> searchRenamedPage(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Checks if a page and its children are duplicated with ones in the target Wiki or not,
   * then gets a list of duplicated pages if any.
   * 
   * @param parentPage The page to check.
   * @param targetWiki The target Wiki to check.
   * @param resultList The list of duplicated wiki pages.
   * @return The list of duplicated wiki pages.
   * @throws Exception
   */
  public List<Page> getDuplicatePages(Page parentPage, Wiki targetWiki, List<Page> resultList) throws Exception;

  /**
   * Finds a wiki page based on its type and relative path.
   *
   * @param path The relative path to find.
   * @param objectNodeType The node type can be page, attachment or template.
   * @return A wiki object that can be page, attachement or template.
   * @throws Exception
   */
  public Object findByPath(String path, String objectNodeType) throws Exception;

  /**
   * Gets Id of a default Wiki syntax.
   *
   * @return The Id.
   */
  public String getDefaultWikiSyntaxId();

  /**
   * Gets an interval which specifies the periodical auto-saving for pages in Wiki.
   * 
   * @return The interval. Its default value is 30 seconds.
   */
  public long getSaveDraftSequenceTime();
  
  /**
   * Get the living time of edited page
   * 
   * @return The living time of edited page
   */
  public long getEditPageLivingTime();

  /**
   * Gets title of a wiki attachment.
   *
   * @param path Path of the attachment.
   * @return Title of the attachment.
   * @throws Exception
   */
  public String getPageTitleOfAttachment(String path) throws Exception;

  /**
   * Gets parent page of a wiki attachment.
   *
   * @param attachment Attachment.
   * @return Page of the attachment.
   * @throws Exception
   */
  public Page getPageOfAttachment(Attachment attachment) throws Exception;

  /**
   * Gets a stream of a wiki attachment.
   *
   * @param path Path of the wiki attachment.
   * @return The stream of the wiki attachment.
   * @throws Exception
   */
  public InputStream getAttachmentAsStream(String path) throws Exception;

  /**
   * Gets a Help wiki page based on a given syntax Id.
   *
   * @param syntaxId Id of the syntax.
   * @return The Help wiki page.
   * @throws Exception
   */
  public Page getHelpSyntaxPage(String syntaxId) throws Exception;

  /**
   * Gets a wiki page of metadata.
   *
   * @param metaPage The metadata to use, mainly emoticons.
   * @return The wiki page of metadata.
   * @throws Exception
   */
  public Page getMetaDataPage(MetaDataPage metaPage) throws Exception;

  /**
   * Gets a map of wiki templates based on a given params object.
   * 
   * @param params The params object which is used for getting the wiki templates.
   * @return The map of wiki templates.
   * @throws Exception
   */
  public Map<String, Template> getTemplates(WikiPageParams params) throws Exception;

  /**
   * Modifies an existing wiki template.
   *
   * @param params The params object which is used for getting the wiki template.
   * @param template The wiki template to be modified.
   * @param newName New name of the wiki template.
   * @param newDescription New description of the wiki template.
   * @param newContent New content of the wiki template.
   * @param newSyntaxId New syntax Id of the wiki template.
   * @throws Exception
   */
  public void modifyTemplate(WikiPageParams params, Template template, String newName, String newDescription, String newContent, String newSyntaxId) throws Exception;

  /**
   * Checks if a wiki page exists or not.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page.
   * @return The returned value is "true" if the page exists, or "false" if not.
   * @throws Exception
   */
  public boolean isExisting(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Gets a list of Wiki default permissions.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @return The list of Wiki default permissions.
   * @throws Exception
   */
  public List<String> getWikiDefaultPermissions(String wikiType, String wikiOwner) throws Exception;
  
  /**
   * Registers a component plugin into the Wiki service.
   * @param plugin The component plugin to be registered.
   */
  public void addComponentPlugin(ComponentPlugin plugin);

  /**
   * Adds a Wiki template as plugin.
   *
   * @param templatePlugin The wiki template plugin to be added.
   */
  public void addWikiTemplatePagePlugin(WikiTemplatePagePlugin templatePlugin);

  /**
   * Gets listeners of all wiki pages that are registered into the Wiki service.
   * @return The list of listeners.
   */
  public List<PageWikiListener> getPageListeners();

  /**
   * Adds a related page to the current wiki page.
   *
   * @param orginaryPageParams The params object of the current wiki page.
   * @param relatedPageParams The params object of the related page.
   * @throws Exception
   */
  public void addRelatedPage(WikiPageParams orginaryPageParams, WikiPageParams relatedPageParams) throws Exception;

  /**
   * Gets a list of related pages based on a given param.
   * @param page The wiki page.
   * @return The list of related pages.
   * @throws Exception
   */
  public List<Page> getRelatedPagesOfPage(Page page) throws Exception;

  /**
   * Removes a related page of the current wiki page.
   * @param orginaryPageParams The params object of the current wiki page.
   * @param relatedPageParams The params object of the related page.
   * @return "True" if removing the related page is successful, or "false" if not.
   * @throws Exception
   */
  public void removeRelatedPage(WikiPageParams orginaryPageParams, WikiPageParams relatedPageParams) throws Exception;
  
  /**
   * Creates a draft page for a wiki page which is specified by a given param object.
   * 
   * @param param The param object of the wiki page.
   * @param revision The revision which is used for creating the draft page. If "null", this will be the last revision.
   * @param clientTime The time of client when the draft page is saved.
   * @return The draft page.
   * @throws Exception if the draft page cannot be created.
   */
  public DraftPage createDraftForExistPage(WikiPageParams param, String revision, long clientTime) throws Exception;
  
  /**
   * Creates a draft page for a new wiki page whose parent is specified by a given param object.
   * 
   * @param parentPageParam The param object of the parent wiki page.
   * @param clientTime The time of client when the draft page is saved.
   * @return The draft page.
   * @throws Exception if the draft page cannot be created.
   */
  public DraftPage createDraftForNewPage(WikiPageParams parentPageParam, long clientTime) throws Exception;
  
  /**
   * Gets a draft page of a wiki page which is specified by a given param object.
   * 
   * @param param The param object of the wiki page.
   * @return The draft page, or "null" if the draft page does not exist.
   * @throws Exception
   */
  public DraftPage getDraftOfPage(Page page) throws Exception;
  
  /**
   * Gets a draft page by its name.
   * 
   * @param draftName Name of the draft page.
   * @return The draft page, or "null" if it does not exist.
   * @throws Exception
   */
   public DraftPage getDraft(String draftName) throws Exception;
  
  /**
    * Removes a draft page of a wiki page which is specified by the wiki page param.
    * 
    * @param param The param object of the wiki page param.
    * @throws Exception
    */
  public void removeDraftOfPage(WikiPageParams param) throws Exception;
  
  /**
   * Removes a draft page by its name.
   * 
   * @param draftName Name of the draft page.
   * @throws Exception
   */
  public void removeDraft(String draftName) throws Exception;
  
  /**
   * Gets a list of draft pages belonging to a given user.
   * 
   * @param username Name of the user.
   * @return The list of draft pages.
   * @throws Exception
   */
  public List<DraftPage> getDraftsOfUser(String username) throws Exception;

  /**
   * Check if a draft page is outdated
   * @param draftPage
   * @return
   * @throws Exception
   */
  public boolean isDraftOutDated(DraftPage draftPage) throws Exception;
  
  /**
   * Gets the last created draft of a wiki page.
   * 
   * @return The last draft.
   * @throws Exception
   */
  public DraftPage getLastestDraft() throws Exception;

  /**
   * Gets the changes between the draft page and the target page
   * @return
   * @throws Exception
   */
  public DiffResult getDraftChanges(DraftPage draftPage) throws Exception;

  /**
   * Gets a user Wiki. If it does not exist, the new one will be created.
   * 
   * @param username Name of the user.
   * @return The user Wiki.
   */
  public Wiki getOrCreateUserWiki(String username) throws Exception;
 
  /**
   * Gets a space name by a given group Id.
   * 
   * @param groupId The group Id.
   * @return The space name.
   * @throws Exception
   */
  public String getSpaceNameByGroupId(String groupId) throws Exception;
  
  /**
   * Searches for spaces by a given keyword.
   * 
   * @param keyword The keyword to search for spaces.
   * @return The list of spaces matching with the keyword.
   * @throws Exception
   */
  public List<SpaceBean> searchSpaces(String keyword) throws Exception;
  
  /**
   * Gets a Wiki which is defined by its type and owner.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param owner The Wiki owner.
   * @return The Wiki.
   */
  public Wiki getWikiByTypeAndOwner(String wikiType, String owner) throws Exception;

  /**
   * Creates a wiki with the given type and owner
   * @param wikiType It can be Portal, Group, or User.
   * @param owner The Wiki owner.
   * @throws Exception
   */
  public Wiki createWiki(String wikiType, String owner) throws Exception;
  
  /**
   * Gets a portal owner.
   * 
   * @return The portal owner.
   */
  public String getPortalOwner();
  
  /**
   * Gets a Wiki webapp URI.
   * 
   * @return The Wiki webapp URI.
   */
  public String getWikiWebappUri();
  
  /**
   * Checks if a given user is member of space or not.
   *
   * @param spaceId Id of the space.
   * @param userId The username.
   * @return "True" if the user is member, or "false" if not.
   */
  public boolean isSpaceMember(String spaceId, String userId);

  /**
   * Checks if a space is hidden or not.
   * 
   * @param groupId Id of the group.
   * @return The returned value is "true" if the space is hidden, or "false" if not.
   * @throws Exception
   */
  public boolean isHiddenSpace(String groupId) throws Exception;

  /**
   * Checks if the given user has the permission on a page
   * @param user
   * @param page
   * @param permissionType
   * @return
   * @throws Exception
   */
  public boolean hasPermissionOnPage(Page page, PermissionType permissionType, Identity user) throws Exception;

  /** 
   * Checks if the current user has the admin permission on a space or not.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param owner Owner of the space.
   * @return The returned value is "true" if the current user has the admin permission on the space, or "false" if not.
   * @throws Exception
   */
  public boolean hasAdminSpacePermission(String wikiType, String owner) throws Exception;
  
  /**
   * Checks if the current user has the admin permission on a wiki page.
   * 
   * @param wikiType It can be Portal, Group, or User.
   * @param owner Owner of the wiki page.
   * @return "True" if the current user has the admin permission on the wiki page, or "false" if not.
   * @throws Exception
   */
  public boolean hasAdminPagePermission(String wikiType, String owner) throws Exception;
  
  /**
   * Creates an activity once a wiki page is updated.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page.
   * @param page The wiki page.
   * @param wikiUpdateType The update type (edit title, edit content, or edit both).
   * @throws Exception
   */
  public void postUpdatePage(String wikiType, String wikiOwner, String pageId, Page page, String wikiUpdateType) throws Exception;
  
  /**
   * Creates an activity of a newly added wiki page.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the new wiki page.
   * @param page The wiki page.
   * @throws Exception
   */
  public void postAddPage(final String wikiType, final String wikiOwner, final String pageId, Page page) throws Exception;
  
  /**
   * Removes all activities related to a deleted wiki page.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the deleted wiki page.
   * @param page The deleted wiki page.
   * @throws Exception
   */
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws Exception;

  /**
   * Gets a Wiki by its Id.
   * 
   * @param wikiId The Wiki Id.
   * @return The Wiki.
   */
  public Wiki getWikiById(String wikiId) throws Exception;
  
  /**
   * Gets a Wiki name by its Id.
   *
   * @param wikiId The Wiki Id.
   * @return The Wiki name.
   * @throws Exception
   */
  public String getWikiNameById(String wikiId) throws Exception;

  public boolean canModifyPagePermission(Page currentPage, String currentUser) throws Exception;

  public boolean canPublicAndRetrictPage(Page currentPage, String currentUser) throws Exception;

  public List<PageVersion> getVersionsOfPage(Page page) throws Exception;

  public void createVersionOfPage(Page page) throws Exception;

  /**
   * Update the given page. This does not automatically create a new version.
   * If a new version must be created it should be explicitly done by calling createVersionOfPage().
   * @param page
   * @throws Exception
   */
  public void updatePage(Page page) throws Exception;
}
