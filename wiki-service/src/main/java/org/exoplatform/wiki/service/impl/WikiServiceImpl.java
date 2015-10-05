package org.exoplatform.wiki.service.impl;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.IOUtils;
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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.plugin.WikiEmotionIconsPlugin;
import org.exoplatform.wiki.rendering.cache.PageRenderingCacheService;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.*;
import org.exoplatform.wiki.service.diff.DiffResult;
import org.exoplatform.wiki.service.diff.DiffService;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.service.search.*;
import org.exoplatform.wiki.template.plugin.WikiTemplatePagePlugin;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.WikiConstants;
import org.picocontainer.Startable;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.xwiki.rendering.syntax.Syntax;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
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

  private List<WikiEmotionIconsPlugin> emotionIconsPlugins = new ArrayList<>();

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
  public void start() {
    initEmotionIcons();
    // TODO Why do we need to remove help pages at each startup ?
    //removeHelpPages();
  }

  @Override
  public void stop() {
  }

  /******* Configuration *******/

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
  public void addEmotionIconsPlugin(WikiEmotionIconsPlugin plugin) {
    if (plugin != null) {
      emotionIconsPlugins.add(plugin);
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
  public String getWikiWebappUri() {
    return wikiWebappUri;
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

  /******* Wiki *******/

  @Override
  public Wiki getWikiByTypeAndOwner(String wikiType, String owner) throws WikiException {
    boolean hasAdminPermission = false;
    try {
      hasAdminPermission = hasAdminPagePermission(wikiType, owner);
    } catch (WikiException e) {
      LOG.error("Cannot check permissions of connected user when getting wiki " + wikiType + ":" + owner, e);
    }
    return dataStorage.getWikiByTypeAndOwner(wikiType, owner, hasAdminPermission);
  }

  @Override
  public Wiki getOrCreateUserWiki(String username) throws WikiException {
    return getWikiByTypeAndOwner(PortalConfig.USER_TYPE, username);
  }

  @Override
  public List<PermissionEntry> getWikiPermission(String wikiType, String wikiOwner) throws WikiException {
    return dataStorage.getWikiPermission(wikiType, wikiOwner);
  }

  @Override
  public void setWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws WikiException {
    dataStorage.setWikiPermission(wikiType, wikiOwner, permissionEntries);
  }

  @Override
  public List<String> getWikiDefaultPermissions(String wikiType, String wikiOwner) throws WikiException {
    return dataStorage.getWikiDefaultPermissions(wikiType, wikiOwner);
  }

  @Override
  public Wiki getWikiById(String wikiId) throws WikiException {
    Wiki wiki;
    if (wikiId.startsWith("/spaces/")) {
      wiki = getWikiByTypeAndOwner(PortalConfig.GROUP_TYPE, wikiId);
    } else if (wikiId.startsWith("/user/")) {
      wikiId = wikiId.substring(wikiId.lastIndexOf('/') + 1);
      wiki = getWikiByTypeAndOwner(PortalConfig.USER_TYPE, wikiId);
    } else {
      if (wikiId.startsWith("/")) {
        wikiId = wikiId.substring(wikiId.lastIndexOf('/') + 1);
      }
      wiki = getWikiByTypeAndOwner(PortalConfig.PORTAL_TYPE, wikiId);
    }
    return wiki;
  }

  @Override
  public String getWikiNameById(String wikiId) throws WikiException {
    Wiki wiki = getWikiById(wikiId);
    if (WikiType.PORTAL.equals(wiki.getType())) {
      String displayName = wiki.getId();
      int slashIndex = displayName.lastIndexOf('/');
      if (slashIndex > -1) {
        displayName = displayName.substring(slashIndex + 1);
      }
      return displayName;
    }

    if (WikiType.USER.equals(wiki.getType())) {
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


  @Override
  public Wiki createWiki(String wikiType, String owner) throws WikiException {
    Wiki wiki = dataStorage.createWiki(wikiType, owner);

    // init templates
    for(WikiTemplatePagePlugin templatePlugin : templatePagePlugins_) {
      if (templatePlugin != null && templatePlugin.getSourcePaths() != null) {
        for (String templateSourcePath : templatePlugin.getSourcePaths()) {
          // TODO change the WikiTemplatePagePlugin plugin to not rely on JCR imports
          //InputStream templateContent = configManager.getInputStream(templateSourcePath);
          Template template = new Template();
          template.setName("MyTemplate");
          template.setTitle("My Great Template !");
          template.setContent("My Great Template !");
          dataStorage.createTemplatePage(wiki, template);
        }
      }
    }

    return wiki;
  }

  /******* Page *******/

  @Override
  public Page createPage(Wiki wiki, String parentPageName, Page page) throws WikiException {
    String pageName = TitleResolver.getId(page.getTitle(), false);
    page.setName(pageName);

    if (isExisting(wiki.getType(), wiki.getOwner(), pageName)) {
      throw new WikiException("Page " + wiki.getType() + ":" + wiki.getOwner() + ":" + pageName + " already exists, cannot create it.");
    }

    PageRenderingCacheService pageRenderingCacheService = ExoContainerContext.getCurrentContainer()
            .getComponentInstanceOfType(PageRenderingCacheService.class);

    Page parentPage = getPageOfWikiByName(wiki.getType(), wiki.getOwner(), parentPageName);

    Page createdPage = dataStorage.createPage(wiki, parentPage, page);

    pageRenderingCacheService.invalidateCache(new WikiPageParams(wiki.getType(), wiki.getOwner(), parentPage.getName()));
    pageRenderingCacheService.invalidateCache(new WikiPageParams(wiki.getType(), wiki.getOwner(), page.getName()));

    return createdPage;
  }

  @Override
  public Page getPageOfWikiByName(String wikiType, String wikiOwner, String pageName) throws WikiException {
    // check in the cache first
    PageRenderingCacheService pageRenderingCacheService = org.exoplatform.wiki.rendering.util.Utils.getService(PageRenderingCacheService.class);
    // TODO re-activate cache
    //Page page = pageRenderingCacheService.getPageByParams(new WikiPageParams(wikiType, wikiOwner, pageName));
    Page page = null;

    Identity user = ConversationState.getCurrent().getIdentity();

    if (page != null) {
      if(!hasPermissionOnPage(page, PermissionType.VIEWPAGE, user)) {
        page = null;
      }
    } else {
      page = dataStorage.getPageOfWikiByName(wikiType, wikiOwner, pageName);
    }

    // Check to remove the domain in page url
    checkToRemoveDomainInUrl(page);

    return page;
  }

  @Override
  public Page getPageById(String id) throws WikiException {
    if (id == null) {
      return null;
    }

    return dataStorage.getPageById(id);
  }

  @Override
  public Page getPageByRootPermission(String wikiType, String wikiOwner, String pageId) throws WikiException {
    return dataStorage.getPageOfWikiByName(wikiType, wikiOwner, pageId);
  }

  @Override
  public Page getParentPageOf(Page page) throws WikiException {
    return dataStorage.getParentPageOf(page);
  }

  @Override
  public List<Page> getChildrenPageOf(Page page) throws WikiException {
    return dataStorage.getChildrenPageOf(page);
  }

  @Override
  public boolean deletePage(String wikiType, String wikiOwner, String pageName) throws WikiException {
    if (WikiConstants.WIKI_HOME_NAME.equals(pageName) || pageName == null) {
      return false;
    }

    try {
      Page page = getPageOfWikiByName(wikiType, wikiOwner, pageName);
      invalidateUUIDCache(wikiType, wikiOwner, pageName);

      if(page != null) {

        // Store all children to launch post deletion listeners
        List<Page> allChrildrenPages = new ArrayList<>();
        Queue<Page> queue = new LinkedList<>();
        queue.add(page);
        Page tempPage;
        while (!queue.isEmpty()) {
          tempPage = queue.poll();
          List<Page> childrenPages = getChildrenPageOf(tempPage);
          for(Page childPage : childrenPages) {
            queue.add(childPage);
            allChrildrenPages.add(childPage);
          }
        }

        dataStorage.deletePage(wikiType, wikiOwner, pageName);

        // Post delete activity for all children pages
        for(Page childPage : allChrildrenPages) {
          postDeletePage(childPage.getWikiType(), childPage.getWikiOwner(), childPage.getName(), childPage);
        }

      } else {
        log.error("Can't delete page '" + pageName + "'. This page does not exist.");
        return false;
      }
    } catch (WikiException e) {
      log.error("Can't delete page '" + pageName + "' ", e);
      return false;
    }
    return true;
  }

  @Override
  public boolean renamePage(String wikiType,
                            String wikiOwner,
                            String pageName,
                            String newName,
                            String newTitle) throws WikiException {
    if (WikiConstants.WIKI_HOME_NAME.equals(pageName) || pageName == null) {
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
  public boolean movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws WikiException {
    try {
      Page movePage = getPageOfWikiByName(currentLocationParams.getType(),
              currentLocationParams.getOwner(),
              currentLocationParams.getPageId());

      dataStorage.movePage(currentLocationParams, newLocationParams);

      PageRenderingCacheService pageRenderingCacheService = ExoContainerContext.getCurrentContainer()
              .getComponentInstanceOfType(PageRenderingCacheService.class);
      pageRenderingCacheService.invalidateCache(currentLocationParams);

      postUpdatePage(newLocationParams.getType(), newLocationParams.getOwner(), movePage.getName(), movePage, PageWikiListener.MOVE_PAGE_TYPE);
    } catch (WikiException e) {
      log.error("Can't move page '" + currentLocationParams.getPageId() + "' ", e);
      return false;
    }
    return true;
  }


  private void invalidateUUIDCache(String wikiType, String wikiOwner, String pageId) throws WikiException {
    Page page = getPageOfWikiByName(wikiType, wikiOwner, pageId);

    Queue<Page> queue = new LinkedList<>();
    queue.add(page);
    while (!queue.isEmpty()) {
      Page currentPage = queue.poll();
      PageRenderingCacheService pageRenderingCacheService = ExoContainerContext.getCurrentContainer()
              .getComponentInstanceOfType(PageRenderingCacheService.class);
      pageRenderingCacheService.invalidateUUIDCache(new WikiPageParams(wikiType, wikiOwner, currentPage.getName()));
      List<Page> childrenPages = getChildrenPageOf(currentPage);
      for (Page child : childrenPages) {
        queue.add(child);
      }
    }
  }

  @Override
  public Page getRelatedPage(String wikiType, String wikiOwner, String pageId) throws WikiException {
    return dataStorage.getRelatedPage(wikiType, wikiOwner, pageId);
  }

  @Override
  public boolean isExisting(String wikiType, String wikiOwner, String pageId) throws WikiException {
    return getPageByRootPermission(wikiType, wikiOwner, pageId) != null;
  }

  @Override
  public PageList<SearchResult> search(WikiSearchData data) throws WikiException {
    try {
      PageList<SearchResult> result = dataStorage.search(data);

      if ((data.getTitle() != null) && (data.getWikiType() != null) && (data.getWikiOwner() != null) && (result.getPageSize() > 0)) {
        Page homePage = getWikiByTypeAndOwner(data.getWikiType(), data.getWikiOwner()).getWikiHome();
        if (data.getTitle().equals("") || homePage != null && homePage.getTitle().contains(data.getTitle())) {
          Calendar wikiHomeCreateDate = Calendar.getInstance();
          wikiHomeCreateDate.setTime(homePage.getCreatedDate());

          Calendar wikiHomeUpdateDate = Calendar.getInstance();
          wikiHomeUpdateDate.setTime(homePage.getUpdatedDate());

          SearchResult wikiHomeResult = new SearchResult(data.getWikiType(), data.getWikiOwner(), data.getPageId(),
                  null, null, homePage.getTitle(), homePage.getPath(), SearchResultType.PAGE, wikiHomeUpdateDate, wikiHomeCreateDate);
          List<SearchResult> tempSearchResult = result.getAll();
          tempSearchResult.add(wikiHomeResult);
          result = new ObjectPageList<>(tempSearchResult, result.getPageSize());
        }
      }
      return result;
    } catch (Exception e) {
      log.error("Cannot search on wiki " + data.getWikiType() + ":" + data.getWikiOwner() + " - Cause : " + e.getMessage(), e);
    }
    return new ObjectPageList<>(new ArrayList<SearchResult>(), 0);
  }

  @Override
  public List<SearchResult> searchRenamedPage(String wikiType, String wikiOwner, String pageId) throws WikiException {
    WikiSearchData data = new WikiSearchData(wikiType, wikiOwner, pageId);
    return dataStorage.searchRenamedPage(data);
  }

  @Override
  public Page getPageOfAttachment(Attachment attachment) throws WikiException {
    return dataStorage.getPageOfAttachment(attachment);
  }

  @Override
  public Page getHelpSyntaxPage(String syntaxId) throws WikiException {
    return dataStorage.getHelpSyntaxPage(syntaxId, syntaxHelpParams, configManager);
  }

  @Override
  public List<EmotionIcon> getEmotionIcons() throws WikiException {
    return dataStorage.getEmotionIcons();
  }

  @Override
  public EmotionIcon getEmotionIconByName(String name) throws WikiException {
    return dataStorage.getEmotionIconByName(name);
  }

  @Override
  public List<Page> getDuplicatePages(Page parentPage, Wiki targetWiki, List<Page> resultList) throws WikiException {
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
    List<Page> childrenPages = getChildrenPageOf(parentPage);
    for (Page page : childrenPages) {
      getDuplicatePages(page, targetWiki, resultList);
    }
    return resultList;
  }

  @Override
  public boolean hasPermissionOnPage(Page page, PermissionType permissionType, Identity user) throws WikiException {
    return dataStorage.hasPermissionOnPage(page, permissionType, user);
  }

  @Override
  public boolean canModifyPagePermission(Page currentPage, String currentUser) throws WikiException{
    String owner = currentPage.getOwner();
    boolean isPageOwner = owner != null && owner.equals(currentUser);
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

    Wiki wiki = getWikiByTypeAndOwner(currentPage.getWikiType(), currentPage.getWikiOwner());
    return ((isPageOwner && hasEditPagePermissionOnPage) || hasAdminSpacePermission(wiki.getType(), wiki.getOwner()))
            || hasAdminPagePermission(wiki.getType(), wiki.getOwner());
  }

  @Override
  public boolean canPublicAndRetrictPage(Page currentPage, String currentUser) throws WikiException {
    Wiki wiki = getWikiByTypeAndOwner(currentPage.getWikiType(), currentPage.getWikiOwner());

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
  public List<PageVersion> getVersionsOfPage(Page page) throws WikiException {
    List<PageVersion> versions = dataStorage.getVersionsOfPage(page);
    if(versions == null || versions.isEmpty()) {
      dataStorage.addPageVersion(page);
      versions = dataStorage.getVersionsOfPage(page);
    }
    return versions;
  }

  @Override
  public void createVersionOfPage(Page page) throws WikiException {
    dataStorage.addPageVersion(page);
  }

  @Override
  public void updatePage(Page page) throws WikiException {
    dataStorage.updatePage(page);

    // TODO implement watch page here (should send an email only if there is a new version of the page content)
    // Utils.sendMailOnChangeContent(content);

    postUpdatePage(page.getWikiType(), page.getOwner(), page.getOwner(), page, PageWikiListener.EDIT_PAGE_CONTENT_AND_TITLE_TYPE);
  }

  /******* Template *******/

  @Override
  public void createTemplatePage(Wiki wiki, Template template) throws WikiException {
    dataStorage.createTemplatePage(wiki, template);
  }

  @Override
  public Template getTemplatePage(WikiPageParams params, String templateId) throws WikiException {
    return dataStorage.getTemplatePage(params, templateId);
  }

  @Override
  public Map<String, Template> getTemplates(WikiPageParams params) throws WikiException {
    return dataStorage.getTemplates(params);
  }

  @Override
  public List<TemplateSearchResult> searchTemplate(TemplateSearchData data) throws WikiException {
    return dataStorage.searchTemplate(data);
  }

  @Override
  public void modifyTemplate(WikiPageParams params,
                             Template template,
                             String newTitle,
                             String newDescription,
                             String newContent,
                             String newSyntaxId) throws WikiException {
    if (newTitle != null) {
      // TODO need updateTemplate
      //template = dataStorage.getTemplatesContainer(params).addPage(TitleResolver.getId(newTitle, false), template);
      template.setDescription(StringEscapeUtils.escapeHtml(newDescription));
      template.setTitle(newTitle);
      template.setContent(newContent);
      template.setSyntax(newSyntaxId);
    }
  }

  @Override
  public void deleteTemplatePage(String wikiType, String wikiOwner, String templateName) throws WikiException {
    dataStorage.deleteTemplatePage(wikiType, wikiOwner, templateName);
  }

  @Override
  public void addRelatedPage(WikiPageParams orginaryPageParams, WikiPageParams relatedPageParams) throws WikiException {
    Page orginary = getPageOfWikiByName(orginaryPageParams.getType(), orginaryPageParams.getOwner(), orginaryPageParams.getPageId());
    Page related = getPageOfWikiByName(relatedPageParams.getType(), relatedPageParams.getOwner(), relatedPageParams.getPageId());
    dataStorage.addRelatedPage(orginary, related);
  }

  @Override
  public List<Page> getRelatedPagesOfPage(Page page) throws WikiException {
    return dataStorage.getRelatedPagesOfPage(page);
  }

  @Override
  public void removeRelatedPage(WikiPageParams orginaryPageParams, WikiPageParams relatedPageParams) throws WikiException {
    Page originary = getPageOfWikiByName(orginaryPageParams.getType(), orginaryPageParams.getOwner(), orginaryPageParams.getPageId());
    Page related = getPageOfWikiByName(relatedPageParams.getType(), relatedPageParams.getOwner(), relatedPageParams.getPageId());
    dataStorage.removeRelatedPage(originary, related);
  }

  /******* Draft *******/

  @Override
  public Page getExsitedOrNewDraftPageById(String wikiType, String wikiOwner, String pageId) throws WikiException {
    Identity user = ConversationState.getCurrent().getIdentity();
    Page existedPage = getPageByRootPermission(wikiType, wikiOwner, pageId);
    if (existedPage != null) {
      if (user == null || hasPermissionOnPage(existedPage, PermissionType.EDITPAGE, user) || hasPermissionOnPage(existedPage, PermissionType.VIEW_ATTACHMENT, user)) {
        return existedPage;
      }
    }

    return dataStorage.getExsitedOrNewDraftPageById(wikiType, wikiOwner, pageId, user.getUserId());
  }

  @Override
  public DraftPage createDraftForNewPage(WikiPageParams targetPageParam, long clientTime) throws WikiException {
    // Create suffix for draft name
    String draftSuffix = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date(clientTime));

    // Get targetPage
    Page targetPage = getPageOfWikiByName(targetPageParam.getType(), targetPageParam.getOwner(), targetPageParam.getPageId());

    DraftPage draftPage = new DraftPage();
    draftPage.setName(UNTITLED_PREFIX + draftSuffix);
    draftPage.setNewPage(true);
    draftPage.setTargetPage(targetPage.getId());
    draftPage.setTargetRevision("1");

    Wiki wiki = getWikiByTypeAndOwner(PortalConfig.USER_TYPE, Utils.getCurrentUser());
    if(wiki == null) {
      // create user wiki to store draft pages
      createWiki(PortalConfig.USER_TYPE, Utils.getCurrentUser());
    }

    dataStorage.createDraftPageForUser(draftPage, Utils.getCurrentUser());

    return draftPage;
  }

  @Override
  public DraftPage createDraftForExistPage(WikiPageParams targetPageParam, String revision, long clientTime) throws WikiException {
    // Create suffix for draft name
    String draftSuffix = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date(clientTime));

    // Get targetPage
    Page targetPage = getPageOfWikiByName(targetPageParam.getType(), targetPageParam.getOwner(), targetPageParam.getPageId());

    DraftPage draftPage = new DraftPage();
    draftPage.setName(targetPage.getName() + "_" + draftSuffix);
    draftPage.setNewPage(false);
    draftPage.setTargetPage(targetPage.getId());
    if (StringUtils.isEmpty(revision)) {
      List<PageVersion> versions = getVersionsOfPage(targetPage);
      if(versions != null && !versions.isEmpty()) {
        draftPage.setTargetRevision(versions.get(0).getName());
      } else {
        draftPage.setTargetRevision("1");
      }
    } else {
      draftPage.setTargetRevision(revision);
    }

    Wiki wiki = getWikiByTypeAndOwner(PortalConfig.USER_TYPE, Utils.getCurrentUser());
    if(wiki == null) {
      // create user wiki to store draft pages
      createWiki(PortalConfig.USER_TYPE, Utils.getCurrentUser());
    }

    dataStorage.createDraftPageForUser(draftPage, Utils.getCurrentUser());

    return draftPage;
  }

  @Override
  public DraftPage getDraftOfPage(Page page) throws WikiException {
    List<DraftPage> draftPages = getDraftsOfUser(Utils.getCurrentUser());
    for(DraftPage draftPage : draftPages) {
      if(draftPage.getTargetPage() != null && draftPage.getTargetPage().equals(page.getId())) {
        return draftPage;
      }
    }
    return null;
  }

  @Override
  public DraftPage getLastestDraft() throws WikiException {
    String currentUser = Utils.getCurrentUser();
    if (IdentityConstants.ANONIM.equals(currentUser)) {
      return null;
    }

    return dataStorage.getLastestDraft(currentUser);
  }

  @Override
  public DraftPage getDraft(String draftName) throws WikiException {
    if (draftName == null) {
      return null;
    }

    String currentUser = Utils.getCurrentUser();
    if (currentUser == null || IdentityConstants.ANONIM.equals(currentUser)) {
      return null;
    }

    return dataStorage.getDraft(draftName, currentUser);
  }

  @Override
  public void removeDraftOfPage(WikiPageParams param) throws WikiException {
    Page page = getPageOfWikiByName(param.getType(), param.getOwner(), param.getPageId());
    dataStorage.deleteDraftOfPage(page, Utils.getCurrentUser());
  }

  @Override
  public void removeDraft(String draftName) throws WikiException {
    dataStorage.deleteDraftById(draftName, Utils.getCurrentUser());
  }

  @Override
  public List<DraftPage> getDraftsOfUser(String username) throws WikiException {
    List<DraftPage> draftPages = new ArrayList<>();
    if (!IdentityConstants.ANONIM.equals(username)) {
      draftPages = dataStorage.getDraftPagesOfUser(username);
    }
    return draftPages;
  }

  @Override
  public boolean isDraftOutDated(DraftPage draftPage) throws WikiException {
    String targetRevision = draftPage.getTargetRevision();
    if (targetRevision == null) {
      return false;
    }

    if (targetRevision.equals("rootVersion")) {
      targetRevision = "1";
    }

    Wiki wiki = getWikiByTypeAndOwner(draftPage.getWikiType(), draftPage.getWikiOwner());
    Page targetPage = getPageOfWikiByName(wiki.getType(), wiki.getOwner(), draftPage.getTargetPage());
    if (targetPage == null) {
      return true;
    }

    String lastestRevision = null;
    List<PageVersion> versions = getVersionsOfPage(targetPage);
    if(versions != null && !versions.isEmpty()) {
      lastestRevision = versions.get(0).getName();
    }
    if (lastestRevision == null) {
      return true;
    }

    if (lastestRevision.equals("rootVersion")) {
      lastestRevision = "1";
    }

    return lastestRevision.compareTo(targetRevision) > 0;
  }

  @Override
  public DiffResult getDraftChanges(DraftPage draftPage) throws WikiException {
    String targetContent = null;

    if (!draftPage.isNewPage()) {
      Wiki wiki = getWikiByTypeAndOwner(draftPage.getWikiType(), draftPage.getWikiOwner());
      Page targetPage = getPageOfWikiByName(wiki.getType(), wiki.getOwner(), draftPage.getTargetPage());
      if (targetPage != null) {
        List<PageVersion> versions = getVersionsOfPage(targetPage);
        if(versions != null && !versions.isEmpty()) {
          PageVersion lastestRevision = versions.get(0);
          targetContent = lastestRevision.getContent();
        }
        if (targetContent == null) {
          targetContent = StringUtils.EMPTY;
        }
      }
    }
    DiffService diffService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(DiffService.class);
    DiffResult diffResult;
    try {
      diffResult = diffService.getDifferencesAsHTML(targetContent, draftPage.getContent(), true);
    } catch (DifferentiationFailedException e) {
      throw new WikiException("Cannot get changes of draft " + draftPage.getWikiType() + ":"
              + draftPage.getWikiOwner() + ":" + draftPage.getName() + " - Cause : " + e.getMessage(), e);
    }

    return diffResult;
  }


  /******* Attachment *******/

  @Override
  public List<Attachment> getAttachmentsOfPage(Page page) throws WikiException {
    return dataStorage.getAttachmentsOfPage(page);
  }

  @Override
  public Attachment getAttachmentOfPageByName(String attachmentName, Page page) throws WikiException {
    Attachment attachment = null;
    List<Attachment> attachments = dataStorage.getAttachmentsOfPage(page);
    for(Attachment att : attachments) {
      if(att.getName().equals(attachmentName)) {
        attachment = att;
        break;
      }
    }
    return attachment;
  }

  @Override
  public void addAttachmentToPage(Attachment attachment, Page page) throws WikiException {
    dataStorage.addAttachmentToPage(attachment, page);
  }

  @Override
  public void deleteAttachmentOfPage(String attachmentId, Page page) throws WikiException {
    dataStorage.deleteAttachmentOfPage(attachmentId, page);
  }

  /******* Spaces *******/

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public List<SpaceBean> searchSpaces(String keyword) throws WikiException {
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
          if (wiki.getId().contains(keyword)) {
            spaceBeans.add(new SpaceBean(wiki.getOwner(), wiki.getId(), PortalConfig.GROUP_TYPE, ""));
          }
        }
      }
    } catch (Exception e) {
      throw new WikiException("Error while searching in wikis for user " + currentUser + " - Cause : " + e.getMessage(), e);
    }
    return spaceBeans;
  }

  @SuppressWarnings("rawtypes")
  private String getDefaultSpaceAvatarUrl() {
    try {
      Class linkProviderClass = Class.forName("org.exoplatform.social.core.service.LinkProvider");
      return linkProviderClass.getDeclaredField("SPACE_DEFAULT_AVATAR_URL").get(null).toString();
    } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
      return "";
    }
  }

  @Override
  public boolean hasAdminSpacePermission(String wikiType, String owner) throws WikiException {
    ConversationState conversationState = ConversationState.getCurrent();
    Identity user;
    UserACL acl;
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
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      log.error("Can not check if user " + userId + " is a member of the space " + spaceId + " - Cause : "
              + e.getMessage(), e);
      return false;
    }
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public boolean isHiddenSpace(String groupId) {
    try {
      Class spaceServiceClass = Class.forName("org.exoplatform.social.core.space.spi.SpaceService");
      Object spaceService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(spaceServiceClass);
      Class spaceClass = Class.forName("org.exoplatform.social.core.space.model.Space");
      Object space = spaceServiceClass.getDeclaredMethod("getSpaceByGroupId", String.class).invoke(spaceService, groupId);
      String visibility = String.valueOf(spaceClass.getDeclaredMethod("getVisibility").invoke(space));
      String hiddenValue = String.valueOf(spaceClass.getDeclaredField("HIDDEN").get(space));
      return hiddenValue.equals(visibility);
    } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
      log.error("Can not check if space " + groupId + " is a hidden space - Cause : " + e.getMessage(), e);
      return true;
    }
  }


  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public String getSpaceNameByGroupId(String groupId) {
    try {
      Class spaceServiceClass = Class.forName("org.exoplatform.social.core.space.spi.SpaceService");
      Object spaceService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(spaceServiceClass);

      Class spaceClass = Class.forName("org.exoplatform.social.core.space.model.Space");
      Object space = spaceServiceClass.getDeclaredMethod("getSpaceByGroupId", String.class).invoke(spaceService, groupId);
      if(space != null) {
        return String.valueOf(spaceClass.getDeclaredMethod("getDisplayName").invoke(space));
      } else {
        return groupId.substring(groupId.lastIndexOf('/') + 1);
      }
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      log.error("Can not find space of group " + groupId + " - Cause : " + e.getMessage(), e);
      return groupId.substring(groupId.lastIndexOf('/') + 1);
    }
  }



  /******* Listeners *******/
  // TODO should not be in the interface
  @Override
  public void postUpdatePage(final String wikiType, final String wikiOwner, final String pageId, Page page, String wikiUpdateType) throws WikiException {
    List<PageWikiListener> listeners = getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.postUpdatePage(wikiType, wikiOwner, pageId, page, wikiUpdateType);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on [%s] failed", l.toString(), page.getPath()), e);
        }
      }
    }
  }

  @Override
  public void postAddPage(final String wikiType, final String wikiOwner, final String pageId, Page page) throws WikiException {
    List<PageWikiListener> listeners = getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.postAddPage(wikiType, wikiOwner, pageId, page);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on [%s] failed", l.toString(), page.getPath()), e);
        }
      }
    }
  }

  @Override
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    List<PageWikiListener> listeners = getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.postDeletePage(wikiType, wikiOwner, pageId, page);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on [%s] failed", l.toString(), page.getPath()), e);
        }
      }
    }
  }


  @Override
  public List<BreadcrumbData> getBreadcumb(String wikiType, String wikiOwner, String pageId) throws WikiException {
    return getBreadcumb(null, wikiType, wikiOwner, pageId);
  }



  @Override
  public WikiPageParams getWikiPageParams(BreadcrumbData data) {
    if (data != null) {
      return new WikiPageParams(data.getWikiType(), data.getWikiOwner(), data.getId());
    }
    return null;
  }

  @Override
  public String getPortalOwner() throws WikiException {
    return dataStorage.getPortalOwner();
  }



  @Override
  public boolean hasAdminPagePermission(String wikiType, String owner) throws WikiException {
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


  @Override
  public void createEmotionIcon(EmotionIcon emotionIcon) throws WikiException {
    dataStorage.createEmotionIcon(emotionIcon);
  }

  /******* Private methods *******/

  private void checkToRemoveDomainInUrl(Page page) {
    if (page == null) {
      return;
    }

    String url = page.getUrl();
    if (url != null && url.contains("://")) {
      try {
        URL oldURL = new URL(url);
        page.setUrl(oldURL.getPath());
      } catch (MalformedURLException ex) {
        if (log.isWarnEnabled()) {
          log.warn("Malformed url " + url, ex);
        }
      }
    }
  }

  /**
   * Recursive method to build the breadcump of a wiki page
   * @param list
   * @param wikiType
   * @param wikiOwner
   * @param pageId
   * @return
   * @throws WikiException
   */
  private List<BreadcrumbData> getBreadcumb(List<BreadcrumbData> list,
                                            String wikiType,
                                            String wikiOwner,
                                            String pageId) throws WikiException {
    if (list == null) {
      list = new ArrayList<>(5);
    }
    if (pageId == null) {
      return list;
    }
    Page page = getPageOfWikiByName(wikiType, wikiOwner, pageId);
    if (page == null) {
      return list;
    }
    list.add(0, new BreadcrumbData(page.getName(), page.getPath(), page.getTitle(), wikiType, wikiOwner));
    Page parentPage = getParentPageOf(page);
    if (parentPage != null) {
      getBreadcumb(list, wikiType, wikiOwner, parentPage.getName());
    }

    return list;
  }

  /**
   * Creates all the emoticons
   */
  private void initEmotionIcons() {
    try {
      List<EmotionIcon> emotionIcons = getEmotionIcons();
      if (emotionIcons == null || emotionIcons.isEmpty()) {
        for (WikiEmotionIconsPlugin emotionIconsPlugin : emotionIconsPlugins) {
          for (EmotionIcon emotionIcon : emotionIconsPlugin.getEmotionIcons()) {
            try {
              InputStream imageInputStream = configManager.getInputStream(emotionIcon.getImageFilePath());
              emotionIcon.setImage(IOUtils.toByteArray(imageInputStream));
              createEmotionIcon(emotionIcon);
            } catch (Exception e) {
              log.error("Cannot create emoticon " + emotionIcon.getName() + " - Cause : " + e.getMessage(), e);
            }
          }
        }
      }
    } catch(WikiException e) {
      log.error("Cannot init emotion icons - Cause : " + e.getMessage(), e);
    }
  }
}
