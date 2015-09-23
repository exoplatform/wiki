package org.exoplatform.wiki.service.impl;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.deployment.plugins.XMLDeploymentPlugin;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.mow.core.api.wiki.*;
import org.exoplatform.wiki.rendering.cache.PageRenderingCacheService;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.*;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.template.plugin.WikiTemplatePagePlugin;
import org.exoplatform.wiki.utils.Utils;
import org.picocontainer.Startable;
import org.xwiki.rendering.syntax.Syntax;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class WikiServiceImpl implements WikiService, Startable {

  private static final Log LOG = ExoLogger.getLogger(WikiServiceImpl.class);

  private static final String UNTITLED_PREFIX = "Untitled_";

  final static private String PREFERENCES = "preferences";

  final static private String DEFAULT_SYNTAX = "defaultSyntax";

  private static final String DEFAULT_WIKI_NAME = "wiki";

  private static final int CIRCULAR_RENAME_FLAG = 1000;

  private static final long DEFAULT_SAVE_DRAFT_SEQUENCE_TIME = 30000;

  private ConfigurationManager configManager;

  private DataStorage dataStorage;

  private List<ValuesParam> syntaxHelpParams;

  private PropertiesParam preferencesParams;

  private List<ComponentPlugin> plugins_ = new ArrayList<>();

  private List<WikiTemplatePagePlugin> templatePagePlugins_ = new ArrayList<>();

  private static final Log log = ExoLogger.getLogger(WikiServiceImpl.class);

  private long autoSaveInterval;

  private long editPageLivingTime_;

  private String wikiWebappUri;

  public WikiServiceImpl(ConfigurationManager configManager,
                         DataStorage dataStorage,
                         InitParams initParams) {
    String autoSaveIntervalProperty = System.getProperty("wiki.autosave.interval");
    if ((autoSaveIntervalProperty == null) || autoSaveIntervalProperty.isEmpty()) {
      autoSaveInterval = DEFAULT_SAVE_DRAFT_SEQUENCE_TIME;
    } else {
      autoSaveInterval = Long.parseLong(autoSaveIntervalProperty);
    }

    this.configManager = configManager;
    this.dataStorage = dataStorage;
    if (initParams != null) {
      Iterator<ValuesParam> helps = initParams.getValuesParamIterator();
      if (helps != null)
        syntaxHelpParams = (List<ValuesParam>) IteratorUtils.toList(initParams.getValuesParamIterator());
      else
        syntaxHelpParams = new ArrayList<>();
      preferencesParams = initParams.getPropertiesParam(PREFERENCES);
    }

    wikiWebappUri = System.getProperty("wiki.permalink.appuri");
    if (StringUtils.isEmpty(wikiWebappUri)) {
      wikiWebappUri = DEFAULT_WIKI_NAME;
    }
    editPageLivingTime_ = Long.parseLong(initParams.getValueParam("wiki.editPage.livingTime").getValue());
  }

  @Override
  public void initDefaultTemplatePage(String path) {
    for(WikiTemplatePagePlugin templatePlugin : templatePagePlugins_) {
      if (templatePlugin != null && templatePlugin.getSourcePaths() != null) {
        for (String templateSourcePath : templatePlugin.getSourcePaths()) {
          dataStorage.createTemplatePage(configManager, templateSourcePath, path);
        }
      }
    }
  }

  @Override
  public Wiki getWiki(String wikiType, String owner) {
    boolean hasAdminPermission = false;
    try {
      hasAdminPermission = hasAdminPagePermission(wikiType, owner);
    } catch (Exception e) {
      LOG.error("Cannot check permissions of connected user when getting wiki " + wikiType + ":" + owner, e);
    }
    return dataStorage.getWiki(wikiType, owner, hasAdminPermission);
  }

  @Override
  public Page createPage(String wikiType, String wikiOwner, String title, String parentId) throws Exception {
    String pageId = TitleResolver.getId(title, false);

    if (isExisting(wikiType, wikiOwner, pageId)) {
      throw new Exception("Page " + wikiType + ":" + wikiOwner + ":" + pageId + " already exists, cannot create it.");
    }

    Wiki wiki = getWiki(wikiType, wikiOwner);

    Page parentPage = getPageById(wikiType, wikiOwner, parentId);


    Page page = dataStorage.createPage(wiki, pageId, parentPage, title);

    return page;
  }

  @Override
  public Page getPageById(String wikiType, String wikiOwner, String pageId) throws Exception {
    // check in the cache first
    PageRenderingCacheService pageRenderingCacheService = org.exoplatform.wiki.rendering.util.Utils.getService(PageRenderingCacheService.class);
    Page page = pageRenderingCacheService.getPageByParams(new WikiPageParams(wikiType, wikiOwner, pageId));

    if (page != null && !page.hasPermission(PermissionType.VIEWPAGE)) {
      page = null;
    }

    // Check to remove the domain in page url
    checkToRemoveDomainInUrl(page);

    return page;
  }

  @Override
  public Page getPageByIdJCRQuery(String wikiType, String wikiOwner, String pageId) throws Exception {
    Page page = getPageByRootPermission(wikiType, wikiOwner, pageId);
    if (page != null && page.hasPermission(PermissionType.VIEWPAGE)) {
      return page;
    }
    return null;
  }

  @Override
  public Page getPageByRootPermission(String wikiType, String wikiOwner, String pageId) throws Exception {
    return dataStorage.getPageById(wikiType, wikiOwner, pageId);
  }


  @Override
  public boolean deletePage(String wikiType, String wikiOwner, String pageId) throws Exception {
    if (WikiNodeType.Definition.WIKI_HOME_NAME.equals(pageId) || pageId == null) {
      return false;
    }

    try {
      Page page = getPageById(wikiType, wikiOwner, pageId);
      invalidateUUIDCache(wikiType, wikiOwner, pageId);

      if(page != null) {
        dataStorage.deletePage(wikiType, wikiOwner, pageId);

        // Post delete activity for all children pages
        Queue<Page> queue = new LinkedList<>();
        queue.add(page);
        Page tempPage;
        while (!queue.isEmpty()) {
          tempPage = queue.poll();
          postDeletePage(wikiType, wikiOwner, tempPage.getName(), tempPage);
          Iterator<PageImpl> iter = ((PageImpl)tempPage).getChildPages().values().iterator();
          while (iter.hasNext()) {
            queue.add(iter.next());
          }
        }
      } else {
        log.error("Can't delete page '" + pageId + "'. This page does not exist.");
        return false;
      }
    } catch (Exception e) {
      log.error("Can't delete page '" + pageId + "' ", e);
      return false;
    }
    return true;
  }

  private void invalidateUUIDCache(String wikiType, String wikiOwner, String pageId) throws Exception {
    Page page = getPageById(wikiType, wikiOwner, pageId);

    Queue<Page> queue = new LinkedList<>();
    queue.add(page);
    while (!queue.isEmpty()) {
      Page currentPage = queue.poll();
      org.exoplatform.wiki.rendering.util.Utils.getService(PageRenderingCacheService.class)
              .invalidateUUIDCache(new WikiPageParams(wikiType, wikiOwner, currentPage.getName()));
      for (PageImpl child : ((PageImpl)currentPage).getChildPages().values()) {
        queue.add(child);
      }
    }
  }

  @Override
  public void deleteTemplatePage(String wikiType, String wikiOwner, String templateId) throws Exception {
    dataStorage.deleteTemplatePage(wikiType, wikiOwner, templateId);
  }

  @Override
  public void deleteDraftNewPage(String newDraftPageId) throws Exception {
    dataStorage.deleteDraftNewPage(newDraftPageId);
  }

  @Override
  public boolean renamePage(String wikiType,
                            String wikiOwner,
                            String pageName,
                            String newName,
                            String newTitle) throws Exception {
    if (WikiNodeType.Definition.WIKI_HOME_NAME.equals(pageName) || pageName == null) {
      return false;
    }

    dataStorage.renamePage(wikiType, wikiOwner, pageName, newName, newTitle);

    // Invaliding cache
    org.exoplatform.wiki.rendering.util.Utils.getService(PageRenderingCacheService.class)
            .invalidateCache(new WikiPageParams(wikiType, wikiOwner, pageName));
    org.exoplatform.wiki.rendering.util.Utils.getService(PageRenderingCacheService.class)
            .invalidateUUIDCache(new WikiPageParams(wikiType, wikiOwner, pageName));

    return true;
  }

  @Override
  public boolean movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws Exception {
    try {
      Page movePage = getPageById(currentLocationParams.getType(),
              currentLocationParams.getOwner(),
              currentLocationParams.getPageId());

      dataStorage.movePage(currentLocationParams, newLocationParams);

      org.exoplatform.wiki.rendering.util.Utils.getService(PageRenderingCacheService.class)
              .invalidateUUIDCache(currentLocationParams);
      // Post activity
      postUpdatePage(newLocationParams.getType(), newLocationParams.getOwner(), movePage.getName(), movePage, PageWikiListener.MOVE_PAGE_TYPE);
    } catch (Exception e) {
      log.error("Can't move page '" + currentLocationParams.getPageId() + "' ", e);
      return false;
    }
    return true;
  }

  @Override
  public List<PermissionEntry> getWikiPermission(String wikiType, String wikiOwner) throws Exception {
    return dataStorage.getWikiPermission(wikiType, wikiOwner);
  }

  @Override
  public void setWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws Exception {
    dataStorage.setWikiPermission(wikiType, wikiOwner, permissionEntries);
  }



  private void checkToRemoveDomainInUrl(Page page) {
    if (page == null) {
      return;
    }

    String url = page.getURL();
    if (url != null && url.contains("://")) {
      try {
        URL oldURL = new URL(url);
        page.setURL(oldURL.getPath());
      } catch (MalformedURLException ex) {
        if (log.isWarnEnabled()) {
          log.warn("Malformed url " + url, ex);
        }
      }
    }
  }

  @Override
  public Page getRelatedPage(String wikiType, String wikiOwner, String pageId) throws Exception {
    return dataStorage.getRelatedPage(wikiType, wikiOwner, pageId);
  }

  @Override
  public Page getExsitedOrNewDraftPageById(String wikiType, String wikiOwner, String pageId) throws Exception {
    String username = Utils.getCurrentUser();
    Page existedPage = getPageByRootPermission(wikiType, wikiOwner, pageId);
    if (existedPage != null) {
      if (username == null || existedPage.hasPermission(PermissionType.EDITPAGE) || existedPage.hasPermission(PermissionType.VIEW_ATTACHMENT)) {
        return existedPage;
      }
    }

    return dataStorage.getExsitedOrNewDraftPageById(wikiType, wikiOwner, pageId, username);
  }

  @Override
  public Page getPageByUUID(String uuid) throws Exception {
    if (uuid == null) {
      return null;
    }

    Model model = getModel();
    WikiStore wStore = model.getWikiStore();
    return dataStorage.getWikiPageByUUID(uuid);
  }

  @Override
  public Template getTemplatePage(WikiPageParams params, String templateId) throws Exception {
    return dataStorage.getTemplatePage(params, templateId);
  }

  @Override
  public Map<String, Template> getTemplates(WikiPageParams params) throws Exception {
    return dataStorage.getTemplates(params);
  }

  @Override
  public TemplateContainer getTemplatesContainer(WikiPageParams params) throws Exception {
    return dataStorage.getTemplatesContainer(params);
  }

  @Override
  public void modifyTemplate(WikiPageParams params,
                             Template template,
                             String newTitle,
                             String newDescription,
                             String newContent,
                             String newSyntaxId) throws Exception {
    if (newTitle != null) {
      template = getTemplatesContainer(params).addPage(TitleResolver.getId(newTitle, false), template);
      template.setDescription(StringEscapeUtils.escapeHtml(newDescription));
      template.setTitle(newTitle);
      template.getContent().setText(newContent);
      template.setSyntax(newSyntaxId);
    }
  }

  @Override
  public boolean isExisting(String wikiType, String wikiOwner, String pageId) throws Exception {
    return getPageByRootPermission(wikiType, wikiOwner, pageId) != null;
  }

  @Override
  public PageList<SearchResult> search(WikiSearchData data) throws Exception {
    try {
      PageList<SearchResult> result = dataStorage.search(data);

      if ((data.getTitle() != null) && (data.getWikiType() != null) && (data.getWikiOwner() != null) && (result.getPageSize() > 0)) {
        Page homePage = getWiki(data.getWikiType(), data.getWikiOwner()).getWikiHome();
        if (data.getTitle().equals("") || homePage != null && homePage.getTitle().contains(data.getTitle())) {
          Calendar wikiHomeCreateDate = Calendar.getInstance();
          wikiHomeCreateDate.setTime(homePage.getCreatedDate());

          Calendar wikiHomeUpdateDate = Calendar.getInstance();
          wikiHomeUpdateDate.setTime(homePage.getUpdatedDate());

          SearchResult wikiHomeResult = new SearchResult(null, homePage.getTitle(), ((PageImpl)homePage).getPath(), WikiNodeType.WIKI_HOME, wikiHomeUpdateDate, wikiHomeCreateDate);
          wikiHomeResult.setPageName(homePage.getName());
          List<SearchResult> tempSearchResult = result.getAll();
          tempSearchResult.add(wikiHomeResult);
          result = new ObjectPageList<>(tempSearchResult, result.getPageSize());
        }
      }
      return result;
    } catch (Exception e) {
      log.error("Can't search", e);
    }
    return new ObjectPageList<>(new ArrayList<SearchResult>(), 0);
  }

  @Override
  public List<TemplateSearchResult> searchTemplate(TemplateSearchData data) throws Exception {
    try {
      return dataStorage.searchTemplate(data);
    } catch (Exception e) {
      log.error("Can't search", e);
    }
    return null;
  }

  @Override
  public List<SearchResult> searchRenamedPage(String wikiType, String wikiOwner, String pageId) throws Exception {
    WikiSearchData data = new WikiSearchData(wikiType, wikiOwner, pageId);
    return dataStorage.searchRenamedPage(data);
  }

  @Override
  public Object findByPath(String path, String objectNodeType) {
    String relPath = path;
    if (relPath.startsWith("/")) {
      relPath = relPath.substring(1);
    }

    return dataStorage.findByPath(relPath, objectNodeType);
  }

  @Override
  public String getPageTitleOfAttachment(String path) throws Exception {
    try {
      String relPath = path;
      if (relPath.startsWith("/")) {
        relPath = relPath.substring(1);
      }
      String temp = relPath.substring(0, relPath.lastIndexOf("/"));
      relPath = temp.substring(0, temp.lastIndexOf("/"));
      PageImpl page = (PageImpl) findByPath(relPath, WikiNodeType.WIKI_PAGE);
      return page.getTitle();
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public InputStream getAttachmentAsStream(String path) throws Exception {
    try {
      return dataStorage.getAttachmentAsStream(path);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public List<BreadcrumbData> getBreadcumb(String wikiType, String wikiOwner, String pageId) throws Exception {
    return getBreadcumb(null, wikiType, wikiOwner, pageId);
  }

  @Override
  public PageImpl getHelpSyntaxPage(String syntaxId) throws Exception {
    Page helpSyntaxPage = dataStorage.getHelpSyntaxPage(syntaxId, syntaxHelpParams, configManager);
    return (PageImpl) helpSyntaxPage;
  }

  @Override
  public Page getMetaDataPage(MetaDataPage metaPage) throws Exception {
    if (MetaDataPage.EMOTION_ICONS_PAGE.equals(metaPage)) {
      return dataStorage.getEmotionIconsPage(metaPage);
    }
    return null;
  }

  @Override
  public String getDefaultWikiSyntaxId() {
    if (preferencesParams != null) {
      return preferencesParams.getProperty(DEFAULT_SYNTAX);
    }
    return Syntax.XWIKI_2_0.toIdString();
  }

  @Override
  public long getSaveDraftSequenceTime() {
    return autoSaveInterval;
  }

  @Override
  public long getEditPageLivingTime() {
    return editPageLivingTime_;
  }

  @Override
  public WikiPageParams getWikiPageParams(BreadcrumbData data) {
    if (data != null) {
      return new WikiPageParams(data.getWikiType(), data.getWikiOwner(), data.getId());
    }
    return null;
  }

  @Override
  public List<PageImpl> getDuplicatePages(PageImpl parentPage, Wiki targetWiki, List<PageImpl> resultList) throws Exception {
    if (resultList == null) {
      resultList = new ArrayList<>();
    }

    // if the result list have more than 6 elements then return
    if (resultList.size() > 6) {
      return resultList;
    }

    // if parent page is duppicated then add to list
    if (isExisting(targetWiki.getType(), targetWiki.getOwner(), parentPage.getName())) {
      resultList.add(parentPage);
    }

    // Check the duplication of all childrent
    for (PageImpl page : parentPage.getChildPages().values()) {
      getDuplicatePages(page, targetWiki, resultList);
    }
    return resultList;
  }

  @Override
  public String getPortalOwner() {
    return dataStorage.getPortalOwner();
  }

  @Override
  public boolean hasAdminSpacePermission(String wikiType, String owner) throws Exception {
    ConversationState conversationState = ConversationState.getCurrent();
    Identity user;
    UserACL acl = null;
    if (conversationState != null) {
      user = conversationState.getIdentity();
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      acl = container.getComponentInstanceOfType(UserACL.class);
      if (acl != null && acl.getSuperUser().equals(user.getUserId())) {
        return true;
      }
    } else {
      user = new Identity(IdentityConstants.ANONIM);
    }

    return dataStorage.hasAdminSpacePermission(wikiType, owner, user);
  }

  @Override
  public boolean hasAdminPagePermission(String wikiType, String owner) throws Exception {
    ConversationState conversationState = ConversationState.getCurrent();
    Identity user;
    if (conversationState != null) {
      user = conversationState.getIdentity();
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      UserACL acl = container.getComponentInstanceOfType(UserACL.class);
      if (acl != null && acl.getSuperUser().equals(user.getUserId())) {
        return true;
      }
    } else {
      user = new Identity(IdentityConstants.ANONIM);
    }

    return dataStorage.hasAdminPagePermission(wikiType, owner, user);
  }

  /**
   * Recursive method to build the breadcump of a wiki page
   * @param list
   * @param wikiType
   * @param wikiOwner
   * @param pageId
   * @return
   * @throws Exception
   */
  private List<BreadcrumbData> getBreadcumb(List<BreadcrumbData> list,
                                            String wikiType,
                                            String wikiOwner,
                                            String pageId) throws Exception {
    if (list == null) {
      list = new ArrayList<>(5);
    }
    if (pageId == null) {
      return list;
    }
    Page page = getPageById(wikiType, wikiOwner, pageId);
    if (page == null) {
      return list;
    }
    list.add(0, new BreadcrumbData(page.getName(), ((PageImpl)page).getPath(), page.getTitle(), wikiType, wikiOwner));
    Page parentPage = page.getParentPage();
    if (parentPage != null) {
      getBreadcumb(list, wikiType, wikiOwner, parentPage.getName());
    }

    return list;
  }

  @Override
  public Template createTemplatePage(String title, WikiPageParams params) throws Exception {
    Model model = getModel();
    TemplateContainer templContainer = getTemplatesContainer(params);
    ConversationState conversationState = ConversationState.getCurrent();
    try {
      Template template = templContainer.createTemplatePage();
      String pageId = TitleResolver.getId(title, false);
      template.setName(pageId);
      templContainer.addPage(template.getName(), template);
      String creator = null;
      if (conversationState != null && conversationState.getIdentity() != null) {
        creator = conversationState.getIdentity().getUserId();
      }
      template.setOwner(creator);
      template.setTitle(title);
      template.getContent().setText("");
      model.save();
      return template;
    } catch (Exception e) {
      log.error("Can not create Template page", e);
    }
    return null;
  }

  private void addEmotionIcons() {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      Model model = getModel();
      WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
      if (wStore.getEmotionIconsPage() == null) {
        model.save();
        XMLDeploymentPlugin emotionIconsPlugin = getEmotionIconsPlugin();
        if (emotionIconsPlugin != null) {
          emotionIconsPlugin.deploy(sessionProvider);
        }
      }
    } catch (Exception e) {
      log.warn("Can not init emotion icons...");
    } finally {
      sessionProvider.close();
    }
  }

  private XMLDeploymentPlugin getEmotionIconsPlugin() {
    for (ComponentPlugin c : plugins_) {
      if (c instanceof XMLDeploymentPlugin) {
        return (XMLDeploymentPlugin) c;
      }
    }
    return null;
  }

  @Override
  public List<String> getWikiDefaultPermissions(String wikiType, String wikiOwner) throws Exception {
    return dataStorage.getWikiDefaultPermissions(wikiType, wikiOwner);
  }

  @Override
  public void addComponentPlugin(ComponentPlugin plugin) {
    if (plugin != null) {
      plugins_.add(plugin);
    }
  }

  @Override
  public void addWikiTemplatePagePlugin(WikiTemplatePagePlugin plugin) {
    if (plugin != null) {
      templatePagePlugins_.add(plugin);
    }
  }

  @Override
  public List<PageWikiListener> getPageListeners() {
    List<PageWikiListener> pageListeners = new ArrayList<>();
    for (ComponentPlugin c : plugins_) {
      if (c instanceof PageWikiListener) {
        pageListeners.add((PageWikiListener) c);
      }
    }
    return pageListeners;
  }

  @Override
  public UserWiki getOrCreateUserWiki(String username) {
    return (UserWiki) getWiki(PortalConfig.USER_TYPE, username);
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public List<SpaceBean> searchSpaces(String keyword) throws Exception {
    List<SpaceBean> spaceBeans = new ArrayList<>();

    // Get group wiki
    String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
    try {
      Class spaceServiceClass = Class.forName("org.exoplatform.social.core.space.spi.SpaceService");
      Object spaceService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(spaceServiceClass);

      Class spaceClass = Class.forName("org.exoplatform.social.core.space.model.Space");
      if (StringUtils.isEmpty(keyword)) {
        //spaces = (ListAccess) spaceServiceClass.getDeclaredMethod("getAccessibleSpacesWithListAccess", String.class).invoke(spaceService, currentUser);
        keyword = "*";
      }
      keyword = keyword.trim();
      //search by keyword
      Class spaceFilterClass = Class.forName("org.exoplatform.social.core.space.SpaceFilter");
      Object spaceFilter = spaceFilterClass.getConstructor(String.class).newInstance(keyword);
      //search by userId
      spaceFilterClass.getDeclaredMethod("setRemoteId", String.class).invoke(spaceFilter, currentUser);
      //search by appId(wiki)
      spaceFilterClass.getDeclaredMethod("setAppId", String.class).invoke(spaceFilter, "Wiki");
      ListAccess spaces = (ListAccess) spaceServiceClass.getDeclaredMethod("getAccessibleSpacesByFilter", String.class, spaceFilterClass).invoke(spaceService, currentUser, spaceFilter);

      for (Object space : spaces.load(0, spaces.getSize())) {
        String groupId = String.valueOf(spaceClass.getMethod("getGroupId").invoke(space));
        String spaceName = String.valueOf(spaceClass.getMethod("getDisplayName").invoke(space));
        String avatarUrl = String.valueOf(spaceClass.getMethod("getAvatarUrl").invoke(space));
        if (StringUtils.isEmpty(avatarUrl) || "null".equalsIgnoreCase(avatarUrl)) {
          avatarUrl = getDefaultSpaceAvatarUrl();
        }
        spaceBeans.add(new SpaceBean(groupId, spaceName, PortalConfig.GROUP_TYPE, avatarUrl));
      }
    } catch (ClassNotFoundException e) {
      Collection<Wiki> wikis = Utils.getWikisByType(WikiType.GROUP);
      if (keyword != null) {
        keyword = keyword.trim();
      }

      if(keyword != null) {
        for (Wiki wiki : wikis) {
          if (wiki.getName().contains(keyword)) {
            spaceBeans.add(new SpaceBean(wiki.getOwner(), wiki.getName(), PortalConfig.GROUP_TYPE, ""));
          }
        }
      }
    }
    return spaceBeans;
  }

  @SuppressWarnings("rawtypes")
  private String getDefaultSpaceAvatarUrl() {
    try {
      Class linkProviderClass = Class.forName("org.exoplatform.social.core.service.LinkProvider");
      return linkProviderClass.getDeclaredField("SPACE_DEFAULT_AVATAR_URL").get(null).toString();
    } catch (Exception e) {
      return "";
    }
  }

  @Override
  public boolean addRelatedPage(WikiPageParams orginaryPageParams, WikiPageParams relatedPageParams) throws Exception {
    PageImpl orginary = (PageImpl) getPageById(orginaryPageParams.getType(), orginaryPageParams.getOwner(), orginaryPageParams.getPageId());
    PageImpl related = (PageImpl) getPageById(relatedPageParams.getType(), relatedPageParams.getOwner(), relatedPageParams.getPageId());
    return orginary.addRelatedPage(related) != null;
  }

  @Override
  public List<Page> getRelatedPage(WikiPageParams pageParams) throws Exception {
    PageImpl page = (PageImpl) getPageById(pageParams.getType(), pageParams.getOwner(), pageParams.getPageId());
    List<PageImpl> pages = page.getRelatedPages();
    return new ArrayList<Page>(pages);
  }

  @Override
  public boolean removeRelatedPage(WikiPageParams orginaryPageParams, WikiPageParams relatedPageParams) throws Exception {
    PageImpl orginary = (PageImpl) getPageById(orginaryPageParams.getType(), orginaryPageParams.getOwner(), orginaryPageParams.getPageId());
    PageImpl related = (PageImpl) getPageById(relatedPageParams.getType(), relatedPageParams.getOwner(), relatedPageParams.getPageId());
    return orginary.removeRelatedPage(related) != null;
  }

  @Override
  public String getWikiWebappUri() {
    return wikiWebappUri;
  }

  @Override
  public boolean isSpaceMember(String spaceId, String userId) {
    try {
      // Get space service
      Class<?> spaceServiceClass = Class.forName("org.exoplatform.social.core.space.spi.SpaceService");
      Object spaceService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(spaceServiceClass);

      // Get space by Id
      Class<?> spaceClass = Class.forName("org.exoplatform.social.core.space.model.Space");
      Object space = spaceServiceClass.getDeclaredMethod("getSpaceByPrettyName", String.class).invoke(spaceService, spaceId);

      // Check if user is the member of space or not
      return Boolean.valueOf(String.valueOf(spaceServiceClass.getDeclaredMethod("isMember", spaceClass, String.class).invoke(spaceService, space, userId)));
    } catch (Exception e) {
      log.debug("Can not check if user is space member", e);
      return false;
    }
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public boolean isHiddenSpace(String groupId) throws Exception {
    try {
      Class spaceServiceClass = Class.forName("org.exoplatform.social.core.space.spi.SpaceService");
      Object spaceService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(spaceServiceClass);
      Class spaceClass = Class.forName("org.exoplatform.social.core.space.model.Space");
      Object space = spaceServiceClass.getDeclaredMethod("getSpaceByGroupId", String.class).invoke(spaceService, groupId);
      String visibility = String.valueOf(spaceClass.getDeclaredMethod("getVisibility").invoke(space));
      String hiddenValue = String.valueOf(spaceClass.getDeclaredField("HIDDEN").get(space));
      return hiddenValue.equals(visibility);
    } catch (ClassNotFoundException e) {
      return true;
    }
  }

  @Override
  public DraftPage createDraftForNewPage(WikiPageParams parentPageParam, long clientTime) throws Exception {
    // Create suffix for draft name
    String draftSuffix = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(clientTime));

    // Get targetPage
    Page parentPage = getPageById(parentPageParam.getType(), parentPageParam.getOwner(), parentPageParam.getPageId());
    String draftName = UNTITLED_PREFIX + draftSuffix;

    // Get draft page container
    UserWiki userWiki = (UserWiki) getWiki(PortalConfig.USER_TYPE, Utils.getCurrentUser());
    Page draftPagesContainer = userWiki.getDraftPagesContainer();

    // Create draft page
    DraftPage draftPage = userWiki.createDraftPage();
    draftPage.setName(draftName);
    draftPagesContainer.addWikiPage(draftPage);
    draftPage.setNewPage(true);
    draftPage.setTargetPage(parentPage.getJCRPageNode().getUUID());
    draftPage.setTargetRevision("1");
    return draftPage;
  }

  @Override
  public DraftPage createDraftForExistPage(WikiPageParams param, String revision, long clientTime) throws Exception {
    // Create suffix for draft name
    String draftSuffix = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(clientTime));

    // Get targetPage
    Page targetPage = getPageById(param.getType(), param.getOwner(), param.getPageId());
    String draftName = targetPage.getName() + "_" + draftSuffix;

    // Get draft page container
    UserWiki userWiki = (UserWiki) getWiki(PortalConfig.USER_TYPE, Utils.getCurrentUser());
    Page draftPagesContainer = userWiki.getDraftPagesContainer();

    // Create draft page
    DraftPageImpl draftPage = userWiki.createDraftPage();
    draftPage.setName(draftName);
    draftPagesContainer.addWikiPage(draftPage);
    draftPage.setNewPage(false);
    draftPage.setTargetPage(targetPage.getJCRPageNode().getUUID());
    if (StringUtils.isEmpty(revision)) {
      draftPage.setTargetRevision(Utils.getLastRevisionOfPage(targetPage).getName());
    } else {
      draftPage.setTargetRevision(revision);
    }
    return draftPage;
  }

  @Override
  public DraftPage getDraft(WikiPageParams param) throws Exception {
    return dataStorage.getDraft(param, org.exoplatform.wiki.utils.Utils.getCurrentUser());
  }

  @Override
  public DraftPage getLastestDraft() throws Exception {
    String currentUser = Utils.getCurrentUser();
    if (IdentityConstants.ANONIM.equals(currentUser)) {
      return null;
    }

    return dataStorage.getLastestDraft(currentUser);
  }

  @Override
  public DraftPage getDraft(String draftName) throws Exception {
    if (draftName == null) {
      return null;
    }

    String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
    if (currentUser == null || IdentityConstants.ANONIM.equals(currentUser)) {
      return null;
    }

    return dataStorage.getDraft(draftName, currentUser);
  }

  @Override
  public void removeDraft(WikiPageParams param) throws Exception {
    DraftPage draftPage = getDraft(param);
    if (draftPage != null) {
      draftPage.remove();
    }
  }

  @Override
  public void removeDraft(String draftName) throws Exception {
    DraftPage draftPage = getDraft(draftName);
    if (draftPage != null) {
      draftPage.remove();
    }
  }

  @Override
  public List<DraftPage> getDrafts(String username) throws Exception {
    List<DraftPage> draftPages = new ArrayList<>();
    if (!IdentityConstants.ANONIM.equals(username)) {
      draftPages = dataStorage.getDrafts(username);
    }
    return draftPages;
  }

  @Override
  public Page getWikiPageByUUID(String uuid) throws Exception {
    return getPageByUUID(uuid);
  }

  @Override
  public void postUpdatePage(final String wikiType, final String wikiOwner, final String pageId, Page page, String wikiUpdateType) throws Exception {
    List<PageWikiListener> listeners = getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.postUpdatePage(wikiType, wikiOwner, pageId, page, wikiUpdateType);
      } catch (Exception e) {
        if (log.isWarnEnabled()) {
          PageImpl pageImpl = (PageImpl) page;
          log.warn(String.format("executing listener [%s] on [%s] failed", l.toString(), pageImpl.getPath()), e);
        }
      }
    }
  }

  @Override
  public void postAddPage(final String wikiType, final String wikiOwner, final String pageId, Page page) throws Exception {
    List<PageWikiListener> listeners = getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.postAddPage(wikiType, wikiOwner, pageId, page);
      } catch (Exception e) {
        if (log.isWarnEnabled()) {
          PageImpl pageImpl = (PageImpl) page;
          log.warn(String.format("executing listener [%s] on [%s] failed", l.toString(), pageImpl.getPath()), e);
        }
      }
    }
  }

  @Override
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws Exception {
    List<PageWikiListener> listeners = getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.postDeletePage(wikiType, wikiOwner, pageId, page);
      } catch (Exception e) {
        if (log.isWarnEnabled()) {
          PageImpl pageImpl = (PageImpl) page;
          log.warn(String.format("executing listener [%s] on [%s] failed", l.toString(), pageImpl.getPath()), e);
        }
      }
    }
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public String getSpaceNameByGroupId(String groupId) throws Exception {
    try {
      Class spaceServiceClass = Class.forName("org.exoplatform.social.core.space.spi.SpaceService");
      Object spaceService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(spaceServiceClass);

      Class spaceClass = Class.forName("org.exoplatform.social.core.space.model.Space");
      Object space = spaceServiceClass.getDeclaredMethod("getSpaceByGroupId", String.class).invoke(spaceService, groupId);
      return String.valueOf(spaceClass.getDeclaredMethod("getDisplayName").invoke(space));
    } catch (ClassNotFoundException e) {
      return groupId.substring(groupId.lastIndexOf('/') + 1);
    }
  }

  private void removeHelpPages() {
    try {
      Model model = getModel();
      WikiStoreImpl wikiStore = (WikiStoreImpl) model.getWikiStore();
      PageImpl helpPages = wikiStore.getHelpPagesContainer();
      helpPages.remove();
    } catch (Exception e) {
      log.warn("Can not remove help pages ...");
    }
  }

  @Override
  public Wiki getWikiById(String wikiId) {
    Wiki wiki = null;
    if (wikiId.startsWith("/spaces/")) {
      wiki = getWiki(PortalConfig.GROUP_TYPE, wikiId);
    } else if (wikiId.startsWith("/user/")) {
      wikiId = wikiId.substring(wikiId.lastIndexOf('/') + 1);
      wiki = getWiki(PortalConfig.USER_TYPE, wikiId);
    } else if (wikiId.startsWith("/" + Utils.getPortalName())) {
      wikiId = wikiId.substring(wikiId.lastIndexOf('/') + 1);
      wiki = getWiki(PortalConfig.PORTAL_TYPE, wikiId);
    }
    return wiki;
  }

  @Override
  public String getWikiNameById(String wikiId) throws Exception {
    Wiki wiki = getWikiById(wikiId);
    if (wiki instanceof PortalWiki) {
      String displayName = wiki.getName();
      int slashIndex = displayName.lastIndexOf('/');
      if (slashIndex > -1) {
        displayName = displayName.substring(slashIndex + 1);
      }
      return displayName;
    }

    if (wiki instanceof UserWiki) {
      String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
      if (wiki.getOwner().equals(currentUser)) {
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        ResourceBundle res = context.getApplicationResourceBundle();
        return res.getString("UISpaceSwitcher.title.my-space");
      }
      return wiki.getOwner();
    }

    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    return wikiService.getSpaceNameByGroupId(wiki.getOwner());
  }

  private Model getModel() {
    MOWService mowService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(MOWService.class);
    return mowService.getModel();
  }

  @Override
  public boolean canModifyPagePermission(Page currentPage, String currentUser) throws Exception{
    boolean isPageOwner = currentPage.getOwner().equals(currentUser);
    String[] permissionOfCurrentUser = currentPage.getPermission().get(currentUser);
    boolean hasEditPagePermissionOnPage = false;
    if (permissionOfCurrentUser != null) {
      for (int i = 0; i < permissionOfCurrentUser.length; i++) {
        if (org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY.equals(permissionOfCurrentUser[i])) {
          hasEditPagePermissionOnPage = true;
          break;
        }
      }
    }

    Wiki wiki = currentPage.getWiki();
    return ((isPageOwner && hasEditPagePermissionOnPage) || hasAdminSpacePermission(wiki.getType(), wiki.getOwner()))
            || hasAdminPagePermission(wiki.getType(), wiki.getOwner());
  }

  @Override
  public boolean canPublicAndRetrictPage(Page currentPage, String currentUser) throws Exception {
    Wiki wiki = currentPage.getWiki();

    boolean hasEditPagePermissionOnPage = false;
    String[] permissionOfCurrentUser = currentPage.getPermission().get(currentUser);
    if (permissionOfCurrentUser != null) {
      for (int i = 0; i < permissionOfCurrentUser.length; i++) {
        if (org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY.equals(permissionOfCurrentUser[i])) {
          hasEditPagePermissionOnPage = true;
          break;
        }
      }
    }
    return hasAdminSpacePermission(wiki.getType(), wiki.getOwner()) || hasEditPagePermissionOnPage;
  }

  @Override
  public void start() {
    try {
      addEmotionIcons();
      removeHelpPages();
      try {
        getWiki(PortalConfig.GROUP_TYPE, "sandbox").getWikiHome();
      } catch (Exception e) {
        log.warn("Can not init sandbox wiki ...");
      }
    } catch (Exception e) {
      log.warn("Can not start WikiService ...", e);
    }
  }

  @Override
  public void stop() {
  }
}
