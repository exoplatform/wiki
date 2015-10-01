package org.exoplatform.wiki.service;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface DataStorage {

  public Wiki getWikiByTypeAndOwner(String wikiType, String owner, boolean hasAdminPermission) throws Exception;

  public Wiki createWiki(String wikiType, String owner) throws Exception;

  public Page createPage(Wiki wiki, Page parentPage, Page page) throws Exception;

  /**
   * Get a wiki page by its unique name in the wiki
   *
   * @param pageName The unique name of the page in the wiki
   * @return The wiki page
   * @throws Exception
   */
  public Page getPageOfWikiByName(String wikiType, String wikiOwner, String pageName) throws Exception;

  /**
   * Get a wiki page by its unique id
   *
   * @param id The unique id of wiki page
   * @return The wiki page
   * @throws Exception
   */
  public Page getPageById(String id) throws Exception;

  public Page getParentPageOf(Page page) throws Exception;

  public List<Page> getChildrenPageOf(Page page) throws Exception;

  public void createTemplatePage(Wiki wiki, Template template) throws Exception;

  public void deleteTemplatePage(String wikiType, String wikiOwner, String templateName) throws Exception;

  public void deletePage(String wikiType, String wikiOwner, String pageId) throws Exception;

  public Template getTemplatePage(WikiPageParams params, String templateId) throws Exception;

  public Map<String, Template> getTemplates(WikiPageParams params) throws Exception;

  public void deleteDraftOfPage(Page page, String username) throws Exception;

  public void deleteDraftById(String newDraftPageId, String username) throws Exception;

  public void renamePage(String wikiType, String wikiOwner, String pageName, String newName, String newTitle) throws Exception;

  public void movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws Exception;

  public List<PermissionEntry> getWikiPermission(String wikiType, String wikiOwner) throws Exception;

  public List<String> getWikiDefaultPermissions(String wikiType, String wikiOwner) throws Exception;

  public void setWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws Exception;

  public List<Page> getRelatedPagesOfPage(Page page) throws Exception;

  public Page getRelatedPage(String wikiType, String wikiOwner, String pageId) throws Exception;

  public void addRelatedPage(Page page, Page relatedPage) throws Exception;

  public void removeRelatedPage(Page page, Page relatedPage) throws Exception;

  public Page getExsitedOrNewDraftPageById(String wikiType, String wikiOwner, String pageId, String username) throws Exception;

  public DraftPage getDraft(WikiPageParams param, String username) throws Exception;

  public DraftPage getLastestDraft(String username) throws Exception;

  public DraftPage getDraft(String draftName, String username) throws Exception;

  public List<DraftPage> getDraftPagesOfUser(String username) throws Exception;

  public void createDraftPageForUser(DraftPage draftPage, String username) throws Exception;

  public PageList<SearchResult> search(WikiSearchData data) throws Exception;

  public List<TemplateSearchResult> searchTemplate(TemplateSearchData data) throws Exception;

  public List<SearchResult> searchRenamedPage(WikiSearchData data) throws Exception;

  public Page getPageOfAttachment(Attachment attachment) throws Exception;

  public InputStream getAttachmentAsStream(String path) throws Exception;

  public Object findByPath(String path, String objectNodeType);

  public Page getHelpSyntaxPage(String syntaxId, List<ValuesParam> syntaxHelpParams, ConfigurationManager configurationManager) throws Exception;

  public Page getEmotionIconsPage() throws Exception;

  public String getPortalOwner();

  public boolean hasPermissionOnPage(Page page, PermissionType permissionType, Identity user) throws Exception;

  public boolean hasAdminSpacePermission(String wikiType, String owner, Identity user) throws Exception;

  public boolean hasAdminPagePermission(String wikiType, String owner, Identity user) throws Exception;

  public List<PageVersion> getVersionsOfPage(Page page) throws Exception;

  public void addPageVersion(Page page) throws Exception;

  public void updatePage(Page page) throws Exception;
}
