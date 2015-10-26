package org.exoplatform.wiki.service;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.service.search.*;

import java.util.List;
import java.util.Map;

public interface DataStorage {

  public Wiki getWikiByTypeAndOwner(String wikiType, String owner) throws WikiException;

  public List<Wiki> getWikisByType(String wikiType) throws WikiException;

  public Wiki createWiki(Wiki wiki) throws WikiException;

  public Page createPage(Wiki wiki, Page parentPage, Page page) throws WikiException;

  /**
   * Get a wiki page by its unique name in the wiki
   *
   * @param pageName The unique name of the page in the wiki
   * @return The wiki page
   * @throws WikiException
   */
  public Page getPageOfWikiByName(String wikiType, String wikiOwner, String pageName) throws WikiException;

  /**
   * Get a wiki page by its unique id
   *
   * @param id The unique id of wiki page
   * @return The wiki page
   * @throws WikiException
   */
  public Page getPageById(String id) throws WikiException;

  public Page getParentPageOf(Page page) throws WikiException;

  public List<Page> getChildrenPageOf(Page page) throws WikiException;

  public void createTemplatePage(Wiki wiki, Template template) throws WikiException;

  public void updateTemplatePage(Template template) throws WikiException;

  public void deleteTemplatePage(String wikiType, String wikiOwner, String templateName) throws WikiException;

  public void deletePage(String wikiType, String wikiOwner, String pageId) throws WikiException;

  public Template getTemplatePage(WikiPageParams params, String templateId) throws WikiException;

  public Map<String, Template> getTemplates(WikiPageParams params) throws WikiException;

  public void deleteDraftOfPage(Page page, String username) throws WikiException;

  public void deleteDraftByName(String newDraftPageName, String username) throws WikiException;

  public void renamePage(String wikiType, String wikiOwner, String pageName, String newName, String newTitle) throws WikiException;

  public void movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws WikiException;

  public List<PermissionEntry> getWikiPermission(String wikiType, String wikiOwner) throws WikiException;

  public void updateWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws WikiException;

  public List<Page> getRelatedPagesOfPage(Page page) throws WikiException;

  public Page getRelatedPage(String wikiType, String wikiOwner, String pageId) throws WikiException;

  public void addRelatedPage(Page page, Page relatedPage) throws WikiException;

  public void removeRelatedPage(Page page, Page relatedPage) throws WikiException;

  public Page getExsitedOrNewDraftPageById(String wikiType, String wikiOwner, String pageId, String username) throws WikiException;

  public DraftPage getDraft(WikiPageParams param, String username) throws WikiException;

  public DraftPage getLastestDraft(String username) throws WikiException;

  public DraftPage getDraft(String draftName, String username) throws WikiException;

  public List<DraftPage> getDraftPagesOfUser(String username) throws WikiException;

  public void createDraftPageForUser(DraftPage draftPage, String username) throws WikiException;

  public PageList<SearchResult> search(WikiSearchData data) throws WikiException;

  public List<TemplateSearchResult> searchTemplate(TemplateSearchData data) throws WikiException;

  public List<Attachment> getAttachmentsOfPage(Page page) throws WikiException;

  public void addAttachmentToPage(Attachment attachment, Page page) throws WikiException;

  public void deleteAttachmentOfPage(String attachmentId, Page page) throws WikiException;

  public Page getHelpSyntaxPage(String syntaxId, boolean fullContent, List<ValuesParam> syntaxHelpParams, ConfigurationManager configurationManager) throws WikiException;

  public void createEmotionIcon(EmotionIcon emotionIcon) throws WikiException;

  public List<EmotionIcon> getEmotionIcons() throws WikiException;

  public EmotionIcon getEmotionIconByName(String name) throws WikiException;

  public boolean hasPermissionOnPage(Page page, PermissionType permissionType, Identity user) throws WikiException;

  public boolean hasAdminSpacePermission(String wikiType, String owner, Identity user) throws WikiException;

  public boolean hasAdminPagePermission(String wikiType, String owner, Identity user) throws WikiException;

  public List<PageVersion> getVersionsOfPage(Page page) throws WikiException;

  public void addPageVersion(Page page) throws WikiException;

  public void restoreVersionOfPage(String versionName, Page page) throws WikiException;

  public void updatePage(Page page) throws WikiException;

  public List<String> getPreviousNamesOfPage(Page page) throws WikiException;

  public List<String> getWatchersOfPage(Page page) throws WikiException;

  public void addWatcherToPage(String username, Page page) throws WikiException;

  public void deleteWatcherOfPage(String username, Page page) throws WikiException;
}
