package org.exoplatform.wiki.service;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.mow.core.api.wiki.Template;
import org.exoplatform.wiki.mow.core.api.wiki.TemplateContainer;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface DataStorage {

  public Wiki getWiki(String wikiType, String owner, boolean hasAdminPermission);

  public Page createPage(Wiki wiki, String pageId, Page parentPage, String title) throws Exception;

  public Page getPageById(String wikiType, String wikiOwner, String pageId) throws Exception;

  /**
   * Get a wiki page by UUID
   *
   * @param uuid The node UUID of wiki page
   * @return The wiki page
   * @throws Exception
   */
  public Page getWikiPageByUUID(String uuid) throws Exception;

  public void createTemplatePage(ConfigurationManager configurationManager, String templateSourcePath, String targetPath);

  public void deletePage(String wikiType, String wikiOwner, String pageId) throws Exception;

  public Template getTemplatePage(WikiPageParams params, String templateId) throws Exception;

  public Map<String, Template> getTemplates(WikiPageParams params) throws Exception;

  public TemplateContainer getTemplatesContainer(WikiPageParams params) throws Exception;

  public void deleteTemplatePage(String wikiType, String wikiOwner, String templateId) throws Exception;

  public void deleteDraftNewPage(String newDraftPageId) throws Exception;

  public void renamePage(String wikiType, String wikiOwner, String pageName, String newName, String newTitle) throws Exception;

  public void movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws Exception;

  public List<PermissionEntry> getWikiPermission(String wikiType, String wikiOwner) throws Exception;

  public List<String> getWikiDefaultPermissions(String wikiType, String wikiOwner) throws Exception;

  public void setWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws Exception;

  public Page getRelatedPage(String wikiType, String wikiOwner, String pageId) throws Exception;

  public Page getExsitedOrNewDraftPageById(String wikiType, String wikiOwner, String pageId, String username) throws Exception;

  public DraftPage getDraft(WikiPageParams param, String username) throws Exception;

  public DraftPage getLastestDraft(String username) throws Exception;

  public DraftPage getDraft(String draftName, String username) throws Exception;

  public List<DraftPage> getDrafts(String username) throws Exception;

  public PageList<SearchResult> search(WikiSearchData data) throws Exception;

  public List<TemplateSearchResult> searchTemplate(TemplateSearchData data) throws Exception;

  public List<SearchResult> searchRenamedPage(WikiSearchData data) throws Exception;

  public InputStream getAttachmentAsStream(String path) throws Exception;

  public Object findByPath(String path, String objectNodeType);

  public Page getHelpSyntaxPage(String syntaxId, List<ValuesParam> syntaxHelpParams, ConfigurationManager configurationManager) throws Exception;

  public Page getEmotionIconsPage(MetaDataPage metaPage) throws Exception;

  public String getPortalOwner();

  public boolean hasAdminSpacePermission(String wikiType, String owner, Identity user) throws Exception;

  public boolean hasAdminPagePermission(String wikiType, String owner, Identity user) throws Exception;
}
