package org.exoplatform.wiki.service.impl;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.wiki.upgrade.PageContentMigrationService;
import org.picocontainer.Startable;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.rendering.converter.ConversionException;
import org.xwiki.rendering.syntax.Syntax;

import org.exoplatform.commons.diff.DiffResult;
import org.exoplatform.commons.diff.DiffService;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.SpaceFilter;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Attachment;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.EmotionIcon;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.PageVersion;
import org.exoplatform.wiki.mow.api.Permission;
import org.exoplatform.wiki.mow.api.PermissionEntry;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.mow.api.Template;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiPreferences;
import org.exoplatform.wiki.mow.api.WikiPreferencesSyntax;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.plugin.WikiEmotionIconsPlugin;
import org.exoplatform.wiki.plugin.WikiTemplatePagePlugin;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.rendering.cache.AttachmentCountData;
import org.exoplatform.wiki.rendering.cache.MarkupData;
import org.exoplatform.wiki.rendering.cache.MarkupKey;
import org.exoplatform.wiki.rendering.cache.UnCachedMacroPlugin;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.BreadcrumbData;
import org.exoplatform.wiki.service.DataStorage;
import org.exoplatform.wiki.service.IDType;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.listener.AttachmentWikiListener;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.SearchResultType;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.WikiConstants;

public class WikiServiceImpl implements WikiService, Startable {

  private static final Log LOG = ExoLogger.getLogger(WikiServiceImpl.class);

  public static final String WIKI_TYPE_DRAFT = "draft";

  private static final String UNTITLED_PREFIX = "Untitled_";

  final static private String PREFERENCES = "preferences";

  final static private String DEFAULT_SYNTAX = "defaultSyntax";

  private static final String DEFAULT_WIKI_NAME = "wiki";

  private static final int CIRCULAR_RENAME_FLAG = 1000;

  private static final long DEFAULT_SAVE_DRAFT_SEQUENCE_TIME = 30000;

  public static final String CACHE_NAME = "wiki.PageRenderingCache";

  public static final String ATT_CACHE_NAME = "wiki.PageAttachmentCache";

  public static String UPLOAD_LIMIT_PARAMETER_NAME = "attachment.upload.limit";

  private ConfigurationManager configManager;

  private OrganizationService orgService;

  private UserACL userACL;

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

  private ExoCache<Integer, MarkupData> renderingCache;
  private ExoCache<Integer, AttachmentCountData> attachmentCountCache;

  private Map<WikiPageParams, List<WikiPageParams>> pageLinksMap = new ConcurrentHashMap<>();

  private Set<String> uncachedMacroes = new HashSet<>();

  private int uploadLimit = 200;

  public WikiServiceImpl(ConfigurationManager configManager,
                         UserACL userACL,
                         DataStorage dataStorage,
                         RenderingService renderingService,
                         CacheService cacheService,
                         OrganizationService orgService,
                         InitParams initParams) {
    String autoSaveIntervalProperty = System.getProperty("wiki.autosave.interval");
    if ((autoSaveIntervalProperty == null) || autoSaveIntervalProperty.isEmpty()) {
      autoSaveInterval = DEFAULT_SAVE_DRAFT_SEQUENCE_TIME;
    } else {
      autoSaveInterval = Long.parseLong(autoSaveIntervalProperty);
    }

    this.configManager = configManager;
    this.userACL = userACL;
    this.dataStorage = dataStorage;
    this.orgService = orgService;

    this.renderingCache = cacheService.getCacheInstance(CACHE_NAME);
    this.attachmentCountCache = cacheService.getCacheInstance(ATT_CACHE_NAME);

    if (initParams != null) {
      Iterator<ValuesParam> helps = initParams.getValuesParamIterator();
      if (helps != null)
        syntaxHelpParams = (List<ValuesParam>) IteratorUtils.toList(initParams.getValuesParamIterator());
      else
        syntaxHelpParams = new ArrayList<>();
      preferencesParams = initParams.getPropertiesParam(PREFERENCES);
      if (initParams.containsKey(UPLOAD_LIMIT_PARAMETER_NAME)) {
        String uploadLimitParamValue = initParams.getValueParam(UPLOAD_LIMIT_PARAMETER_NAME).getValue();
        if (StringUtils.isNotBlank(uploadLimitParamValue)) {
          uploadLimit = Integer.parseInt(uploadLimitParamValue);
        }
      }
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
  }

  @Override
  public void stop() {
  }

  public ExoCache<Integer, MarkupData> getRenderingCache() {
    return renderingCache;
  }

  public Map<WikiPageParams, List<WikiPageParams>> getPageLinksMap() {
    return pageLinksMap;
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
  public List<AttachmentWikiListener> getAttachmentListeners() {
    List<AttachmentWikiListener> attachmentListeners = new ArrayList<>();
    for (ComponentPlugin c : plugins_) {
      if (c instanceof AttachmentWikiListener) {
        attachmentListeners.add((AttachmentWikiListener) c);
      }
    }
    return attachmentListeners;
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
    return Syntax.XHTML_1_0.toIdString();
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
    return dataStorage.getWikiByTypeAndOwner(wikiType, owner);
  }

  @Override
  public List<Wiki> getWikisByType(String wikiType) throws WikiException {
    return dataStorage.getWikisByType(wikiType);
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
  public void updateWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws WikiException {
    dataStorage.updateWikiPermission(wikiType, wikiOwner, permissionEntries);
  }

  @Override
  public List<PermissionEntry> getWikiDefaultPermissions(String wikiType, String wikiOwner) throws WikiException {
    Permission[] viewPermissions = new Permission[] {
            new Permission(PermissionType.VIEWPAGE, true)
    };
    Permission[] viewEditPermissions = new Permission[] {
            new Permission(PermissionType.VIEWPAGE, true),
            new Permission(PermissionType.EDITPAGE, true)
    };
    Permission[] allPermissions = new Permission[] {
            new Permission(PermissionType.VIEWPAGE, true),
            new Permission(PermissionType.EDITPAGE, true),
            new Permission(PermissionType.ADMINPAGE, true),
            new Permission(PermissionType.ADMINSPACE, true)
    };
    List<PermissionEntry> permissions = new ArrayList<>();
    Iterator<Map.Entry<String, IDType>> iter = Utils.getACLForAdmins().entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<String, IDType> entry = iter.next();
      PermissionEntry permissionEntry = new PermissionEntry(entry.getKey(), "", entry.getValue(), allPermissions);
      permissions.add(permissionEntry);
    }
    if (PortalConfig.PORTAL_TYPE.equals(wikiType)) {
      UserPortalConfigService userPortalConfigService = ExoContainerContext.getCurrentContainer()
              .getComponentInstanceOfType(UserPortalConfigService.class);
      try {
        if(userPortalConfigService != null) {
          UserPortalConfig userPortalConfig = userPortalConfigService.getUserPortalConfig(wikiOwner, null);
          if (userPortalConfig != null) {
            PortalConfig portalConfig = userPortalConfig.getPortalConfig();
            PermissionEntry portalPermissionEntry = new PermissionEntry(portalConfig.getEditPermission(),
                    "", IDType.MEMBERSHIP, allPermissions);
            permissions.add(portalPermissionEntry);
          }
        }
        PermissionEntry userPermissionEntry = new PermissionEntry("any", "", IDType.USER, viewPermissions);
        permissions.add(userPermissionEntry);
      } catch (Exception e) {
        throw new WikiException("Cannot get user portal config for wiki " + wikiType + ":" + wikiOwner
                + " - Cause : " + e.getMessage(), e);
      }
    } else if (PortalConfig.GROUP_TYPE.equals(wikiType)) {
      PermissionEntry groupPermissionEntry = new PermissionEntry(userACL.getMakableMT() + ":" + wikiOwner, "", IDType.MEMBERSHIP, allPermissions);
      permissions.add(groupPermissionEntry);
      PermissionEntry ownerPermissionEntry = new PermissionEntry("*:" + wikiOwner, "", IDType.MEMBERSHIP, viewEditPermissions);
      permissions.add(ownerPermissionEntry);
    } else if (PortalConfig.USER_TYPE.equals(wikiType)) {
      PermissionEntry ownerPermissionEntry = new PermissionEntry(wikiOwner, "", IDType.USER, allPermissions);
      permissions.add(ownerPermissionEntry);
    }

    return permissions;
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
    Wiki wiki = getWikiByTypeAndOwner(wikiType, owner);
    if (wiki != null) {
      throw new WikiException("Wiki with type '" + wikiType + "' and owner = '" + owner + "' already exists");
    }
    wiki = new Wiki(wikiType, owner);
    wiki.setPermissions(getWikiDefaultPermissions(wikiType, owner));
    // set wiki syntax
    WikiPreferences wikiPreferences = new WikiPreferences();
    WikiPreferencesSyntax wikiPreferencesSyntax = new WikiPreferencesSyntax();
    wikiPreferencesSyntax.setDefaultSyntax(getDefaultWikiSyntaxId());
    wikiPreferences.setWikiPreferencesSyntax(wikiPreferencesSyntax);
    wiki.setPreferences(wikiPreferences);

    Wiki createdWiki = dataStorage.createWiki(wiki);
    StringBuilder sb = new StringBuilder("<h1> Welcome to ");
    String wikiLabel = owner;
    if(wikiType.equals(PortalConfig.GROUP_TYPE)) {
      sb.append("Space ");
      wikiLabel = getSpaceNameByGroupId(owner);
    } else if (wikiType.equals(PortalConfig.USER_TYPE)) {
      wikiLabel = this.getUserDisplayName(wiki.getOwner());
    }
    sb.append(wikiLabel).append(" Wiki </h1>");
    createdWiki.getWikiHome().setContent(sb.toString());
    updatePage(createdWiki.getWikiHome(), null);
    // init templates
    for(WikiTemplatePagePlugin templatePlugin : templatePagePlugins_) {
      if (templatePlugin != null && templatePlugin.getTemplates() != null) {
        for (Template template : templatePlugin.getTemplates()) {
          try {
            InputStream templateInputStream = configManager.getInputStream(template.getSourceFilePath());
            template.setContent(IOUtils.toString(templateInputStream));
            dataStorage.createTemplatePage(createdWiki, template);
          } catch(Exception e) {
            log.error("Cannot init template " + template.getName() + " - Cause : " + e.getMessage(), e);
          }
        }
      }
    }

    return createdWiki;
  }

  private String getUserDisplayName(String username) {
    try {
      User user = orgService.getUserHandler().findUserByName(username, UserStatus.ANY);
      StringBuilder nameBuilder = new StringBuilder(user.getFirstName());
      nameBuilder.append(" ").append(user.getLastName());
      return nameBuilder.toString();
    } catch (Exception e) {
      return username;
    }
  }

  /******* Page *******/

  @Override
  public Page createPage(Wiki wiki, String parentPageName, Page page) throws WikiException {
    String pageName = TitleResolver.getId(page.getTitle(), false);
    page.setName(pageName);

    if (isExisting(wiki.getType(), wiki.getOwner(), pageName)) {
      throw new WikiException("Page " + wiki.getType() + ":" + wiki.getOwner() + ":" + pageName + " already exists, cannot create it.");
    }

    Page parentPage = getPageOfWikiByName(wiki.getType(), wiki.getOwner(), parentPageName);
    List<PermissionEntry> permissions = page.getPermissions();
    // if permissions are not set, init with default permissions
    if(permissions == null) {
      permissions = this.getWikiDefaultPermissions(wiki.getType(), wiki.getOwner());
      page.setPermissions(permissions);
    }
    // init permission for page's creator
    String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
    PermissionEntry currentUserPermEnty = null;
    for (PermissionEntry permEntry : page.getPermissions()) {
      // get user permission entry  
      if (permEntry.getId().equals(currentUser)) {
        currentUserPermEnty = permEntry;
        Boolean hasAdminPerm = false;
        for (Permission permission : permEntry.getPermissions()) {
          if (permission.getPermissionType().equals(PermissionType.ADMINPAGE)) {
            if (!permission.isAllowed()) {
              permission.setAllowed(true);
            } 
            hasAdminPerm = true;
            break;
          }
        }
        if (!hasAdminPerm) {
          // if user has no ADMINPAGE permission, add and update user PermissionEntry
          List<Permission> userPermissions = new ArrayList<Permission>(Arrays.asList(permEntry.getPermissions()));
          userPermissions.add(new Permission(PermissionType.ADMINPAGE, true));
          permEntry.setPermissions(userPermissions.toArray(new Permission[userPermissions.size()]));
        }
        break;
      }
    }
    // if page has no permission entry for current user, init and add user permission entry
    if (currentUserPermEnty == null) {
      currentUserPermEnty = new PermissionEntry(currentUser, "", IDType.USER, new Permission[] {
          new Permission(PermissionType.VIEWPAGE, true),
          new Permission(PermissionType.EDITPAGE, true),
          new Permission(PermissionType.ADMINPAGE, true) });
      page.getPermissions().add(currentUserPermEnty);
    }
    Page createdPage = dataStorage.createPage(wiki, parentPage, page);

    invalidateCache(parentPage);
    invalidateCache(page);

    // call listeners
    postAddPage(wiki.getType(), wiki.getOwner(), page.getName(), createdPage);

    return createdPage;
  }
  @Override
  public Page getPageOfWikiByName(String wikiType, String wikiOwner, String pageName) throws WikiException {
    Page page = null;

    // check in the cache first
    page = dataStorage.getPageOfWikiByName(wikiType, wikiOwner, pageName);

    if(page != null) {
      Identity user = ConversationState.getCurrent().getIdentity();
      if (!hasPermissionOnPage(page, PermissionType.VIEWPAGE, user)) {
        page = null;
      }
    }

    // Check to remove the domain in page url
    checkToRemoveDomainInUrl(page);

    return page;
  }

  @Override
  public void addUnCachedMacro(UnCachedMacroPlugin plugin) {
    uncachedMacroes.addAll(plugin.getUncachedMacroes());
  }

  @Override
  public Page getPageById(String id) throws WikiException {
    if (id == null) {
      return null;
    }

    return dataStorage.getPageById(id);
  }

  @Override
  public DraftPage getDraftPageById(String id) throws WikiException {
    if (id == null) {
      return null;
    }

    return dataStorage.getDraftPageById(id);
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

      if(page != null) {
        invalidateCachesOfPageTree(page);
        invalidateAttachmentCache(page);

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

        postDeletePage(wikiType, wikiOwner, pageName, page);

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

    if (!pageName.equals(newName) && isExisting(wikiType, wikiOwner, newName)) {
      throw new WikiException("Page " + wikiType + ":" + wikiOwner + ":" + newName + " already exists, cannot rename it.");
    }

    dataStorage.renamePage(wikiType, wikiOwner, pageName, newName, newTitle);

    // Invaliding cache
    Page page = new Page(pageName);
    page.setWikiType(wikiType);
    page.setWikiOwner(wikiOwner);
    invalidateCache(page);

    return true;
  }

  @Override
  public boolean movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws WikiException {
    try {
      Page movePage = getPageOfWikiByName(currentLocationParams.getType(),
          currentLocationParams.getOwner(),
          currentLocationParams.getPageName());

      dataStorage.movePage(currentLocationParams, newLocationParams);

      Page page = new Page(currentLocationParams.getPageName());
      page.setWikiType(currentLocationParams.getType());
      page.setWikiOwner(currentLocationParams.getOwner());
      invalidateCache(page);
      invalidateAttachmentCache(page);

      postUpdatePage(newLocationParams.getType(), newLocationParams.getOwner(), movePage.getName(), movePage, PageUpdateType.MOVE_PAGE);
    } catch (WikiException e) {
      log.error("Can't move page '" + currentLocationParams.getPageName() + "' ", e);
      return false;
    }
    return true;
  }

  @Override
  public String getPageRenderedContent(Page page, String targetSyntax) {
    String renderedContent = StringUtils.EMPTY;
    try {
      String markup = page.getContent();

      boolean isUseCache = isCachePage(markup);
      MarkupKey key = null;
      if (isUseCache) {
        key = new MarkupKey(new WikiPageParams(page.getWikiType(), page.getWikiOwner(), page.getName()), page.getSyntax(), targetSyntax, false);
        //get content from cache only when page is not uncached mixin
        MarkupData cachedData = renderingCache.get(new Integer(key.hashCode()));
        if (cachedData != null) {
          return cachedData.build();
        }
      }

      // migrate page from XWiki syntax to HTML on the fly
      ExoContainerContext.getService(PageContentMigrationService.class).migratePage(page);

      renderedContent = markup;
      if (isUseCache) {
        renderingCache.put(new Integer(key.hashCode()), new MarkupData(renderedContent));
      }
    } catch (Exception e) {
      LOG.error(String.format("Failed to get rendered content of page [%s:%s:%s] in syntax %s", page.getWikiType(), page.getWikiOwner(), page.getName(), targetSyntax), e);
    }
    return renderedContent;
  }

  @Override
  public void addPageLink(WikiPageParams param, WikiPageParams entity) {
    List<WikiPageParams> linkParams = this.pageLinksMap.get(entity);
    if (linkParams == null) {
      linkParams = new ArrayList<>();
      this.pageLinksMap.put(entity, linkParams);
    }
    linkParams.add(param);
  }

  public Set<String> getUncachedMacroes() {
    return uncachedMacroes;
  }

  protected void invalidateCache(Page page) {
    WikiPageParams params = new WikiPageParams(page.getWikiType(), page.getWikiOwner(), page.getName());
    List<WikiPageParams> linkedPages = pageLinksMap.get(params);
    if (linkedPages == null) {
      linkedPages = new ArrayList<>();
    } else {
      linkedPages = new ArrayList<>(linkedPages);
    }
    linkedPages.add(params);

    for (WikiPageParams wikiPageParams : linkedPages) {
      try {
        MarkupKey key = new MarkupKey(wikiPageParams, Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false);
        renderingCache.remove(new Integer(key.hashCode()));
        key.setSupportSectionEdit(true);
        renderingCache.remove(new Integer(key.hashCode()));

        key = new MarkupKey(wikiPageParams,Syntax.XHTML_1_0.toIdString(), Syntax.XWIKI_2_0.toIdString(), false);
        renderingCache.remove(new Integer(key.hashCode()));
        key.setSupportSectionEdit(true);
        renderingCache.remove(new Integer(key.hashCode()));

        key = new MarkupKey(wikiPageParams,Syntax.XHTML_1_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false);
        renderingCache.remove(new Integer(key.hashCode()));
        key.setSupportSectionEdit(true);
        renderingCache.remove(new Integer(key.hashCode()));
      } catch (Exception e) {
        LOG.warn(String.format("Failed to invalidate cache of page [%s:%s:%s]", wikiPageParams.getType(), wikiPageParams.getOwner(), wikiPageParams.getPageName()));
      }
    }
  }

  protected void invalidateAttachmentCache(Page page) {
    WikiPageParams wikiPageParams = new WikiPageParams(page.getWikiType(), page.getWikiOwner(), page.getName());

    List<WikiPageParams> linkedPages = pageLinksMap.get(wikiPageParams);
    if (linkedPages == null) {
      linkedPages = new ArrayList<>();
    } else {
      linkedPages = new ArrayList<>(linkedPages);
    }
    linkedPages.add(wikiPageParams);

    for (WikiPageParams linkedWikiPageParams : linkedPages) {
      try {
        MarkupKey key = new MarkupKey(linkedWikiPageParams, Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false);
        attachmentCountCache.remove(new Integer(key.hashCode()));
        key.setSupportSectionEdit(true);
        attachmentCountCache.remove(new Integer(key.hashCode()));

        key = new MarkupKey(linkedWikiPageParams,Syntax.XHTML_1_0.toIdString(), Syntax.XWIKI_2_0.toIdString(), false);
        attachmentCountCache.remove(new Integer(key.hashCode()));
        key.setSupportSectionEdit(true);
        attachmentCountCache.remove(new Integer(key.hashCode()));
      } catch (Exception e) {
        LOG.warn(String.format("Failed to invalidate cache of page [%s:%s:%s]", linkedWikiPageParams.getType(),
                linkedWikiPageParams.getOwner(), linkedWikiPageParams.getPageName()));
      }
    }
  }


  /**
   * Invalidate all caches of a page and all its descendants
   * @param page root page
   * @throws WikiException
   */
  protected void invalidateCachesOfPageTree(Page page) throws WikiException {
    Queue<Page> queue = new LinkedList<>();
    queue.add(page);
    while (!queue.isEmpty()) {
      Page currentPage = queue.poll();
      invalidateCache(currentPage);
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

          SearchResult wikiHomeResult = new SearchResult(data.getWikiType(), data.getWikiOwner(), homePage.getName(),
                  null, null, homePage.getTitle(), SearchResultType.PAGE, wikiHomeUpdateDate, wikiHomeCreateDate);
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
  public Page getHelpSyntaxPage(String syntaxId, boolean fullContent) throws WikiException {
    return null;
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
  public boolean hasPermissionOnWiki(Wiki wiki, PermissionType permissionType, Identity user) throws WikiException {
    return dataStorage.hasPermissionOnWiki(wiki, permissionType, user);
  }

  @Override
  public boolean hasPermissionOnPage(Page page, PermissionType permissionType, Identity user) throws WikiException {
    return dataStorage.hasPermissionOnPage(page, permissionType, user);
  }

  @Override
  public boolean canModifyPagePermission(Page currentPage, String currentUser) throws WikiException{
    boolean canModifyPage = false;

    String owner = currentPage.getOwner();
    boolean isPageOwner = owner != null && owner.equals(currentUser);
    boolean hasEditPagePermissionOnPage = false;
    if (currentPage.getPermissions() != null) {
      for (PermissionEntry permissionEntry : currentPage.getPermissions()) {
        if(permissionEntry.getId().equals(currentUser)) {
          for(Permission permission : permissionEntry.getPermissions()) {
            if(permission.getPermissionType().equals(PermissionType.EDITPAGE) && permission.isAllowed()) {
              hasEditPagePermissionOnPage = true;
              break;
            }
          }
        }
        if(hasEditPagePermissionOnPage) {
          break;
        }
      }
    }

    if(isPageOwner && hasEditPagePermissionOnPage) {
      canModifyPage = true;
    } else {
      Wiki wiki = getWikiByTypeAndOwner(currentPage.getWikiType(), currentPage.getWikiOwner());
      canModifyPage = (hasAdminSpacePermission(wiki.getType(), wiki.getOwner()))
              || hasAdminPagePermission(wiki.getType(), wiki.getOwner());
    }

    return canModifyPage;
  }
  
  @Override
  public boolean canPublicAndRetrictPage(Page currentPage, String currentUser) throws WikiException {
    if (currentPage.getPermissions() != null) {
      for (PermissionEntry permissionEntry : currentPage.getPermissions()) {
        if(permissionEntry.getId().equals(currentUser)) {
          for(Permission permission : permissionEntry.getPermissions()) {
            if(permission.getPermissionType().equals(PermissionType.EDITPAGE) && permission.isAllowed()) {
              return true;
            }
          }
        }
      }
    }
    Wiki wiki = getWikiByTypeAndOwner(currentPage.getWikiType(), currentPage.getWikiOwner());
    return hasAdminPagePermission(wiki.getType(), wiki.getOwner())
        || hasAdminSpacePermission(wiki.getType(), wiki.getOwner());
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
  public PageVersion getVersionOfPageByName(String versionName, Page page) throws WikiException {
    List<PageVersion> versions = getVersionsOfPage(page);
    PageVersion pageVersion = null;
    if(versions != null) {
      for(PageVersion version : versions) {
        if(version.getName().equals(versionName)) {
          pageVersion = version;
          break;
        }
      }
    }
    return pageVersion;
  }

  @Override
  public void createVersionOfPage(Page page) throws WikiException {
    dataStorage.addPageVersion(page);
  }

  @Override
  public void restoreVersionOfPage(String versionName, Page page) throws WikiException {
    dataStorage.restoreVersionOfPage(versionName, page);
    createVersionOfPage(page);

    invalidateCache(page);
  }

  @Override
  public void updatePage(Page page, PageUpdateType updateType) throws WikiException {
    // update updated date if the page content has been updated
    if(PageUpdateType.EDIT_PAGE_CONTENT.equals(updateType) || PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE.equals(updateType)) {
      page.setUpdatedDate(Calendar.getInstance().getTime());
    }

    dataStorage.updatePage(page);

    invalidateCache(page);

    if(PageUpdateType.EDIT_PAGE_CONTENT.equals(updateType) || PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE.equals(updateType)) {
      try {
        Utils.sendMailOnChangeContent(page);
      } catch (WikiException | DifferentiationFailedException | ComponentLookupException | ConversionException e) {
        log.error("Cannot send notification email on page change - Cause : " + e.getMessage(), e);
      }
    }


    postUpdatePage(page.getWikiType(), page.getWikiOwner(), page.getName(), page, updateType);
  }

  @Override
  public List<String> getPreviousNamesOfPage(Page page) throws WikiException {
    return dataStorage.getPreviousNamesOfPage(page);
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
  public void updateTemplate(Template template) throws WikiException {
    dataStorage.updateTemplatePage(template);
  }

  @Override
  public void deleteTemplatePage(String wikiType, String wikiOwner, String templateName) throws WikiException {
    dataStorage.deleteTemplatePage(wikiType, wikiOwner, templateName);
  }

  @Override
  public void addRelatedPage(WikiPageParams orginaryPageParams, WikiPageParams relatedPageParams) throws WikiException {
    if (relatedPageParams == null) {
      throw new IllegalArgumentException("relatedPageParams shouldn't be null");
    }
    Page orginary = getPageOfWikiByName(orginaryPageParams.getType(), orginaryPageParams.getOwner(), orginaryPageParams.getPageName());
    Page related = getPageOfWikiByName(relatedPageParams.getType(), relatedPageParams.getOwner(), relatedPageParams.getPageName());
    if (related == null) {
      throw new IllegalStateException("Cannot find related page with parameters type = " + relatedPageParams.getType()
          + " owner = " + relatedPageParams.getOwner() + " pageName = " + relatedPageParams.getPageName());
    }
    dataStorage.addRelatedPage(orginary, related);
  }

  @Override
  public List<Page> getRelatedPagesOfPage(Page page) throws WikiException {
    return dataStorage.getRelatedPagesOfPage(page);
  }

  @Override
  public void removeRelatedPage(WikiPageParams orginaryPageParams, WikiPageParams relatedPageParams) throws WikiException {
    Page originary = getPageOfWikiByName(orginaryPageParams.getType(), orginaryPageParams.getOwner(), orginaryPageParams.getPageName());
    Page related = getPageOfWikiByName(relatedPageParams.getType(), relatedPageParams.getOwner(), relatedPageParams.getPageName());
    dataStorage.removeRelatedPage(originary, related);
  }

  /******* Draft *******/

  @Override
  public Page getExsitedOrNewDraftPageById(String wikiType, String wikiOwner, String pageId) throws WikiException {
    Identity user = ConversationState.getCurrent().getIdentity();
    Page existedPage = getPageByRootPermission(wikiType, wikiOwner, pageId);
    if (existedPage != null) {
      if (user == null || hasPermissionOnPage(existedPage, PermissionType.VIEWPAGE, user) || hasPermissionOnPage(existedPage, PermissionType.VIEW_ATTACHMENT, user)) {
        return existedPage;
      }
    }

    return dataStorage.getExsitedOrNewDraftPageById(wikiType, wikiOwner, pageId, user.getUserId());
  }

  @Override
  public DraftPage createDraftForNewPage(DraftPage draftPage, Page parentPage,  long clientTime) throws WikiException {
    // Create suffix for draft name
    String draftSuffix = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date(clientTime));
    
    DraftPage newDraftPage = new DraftPage();
    newDraftPage.setName(UNTITLED_PREFIX + draftSuffix);
    newDraftPage.setNewPage(true);
    newDraftPage.setTitle(draftPage.getTitle());
    newDraftPage.setTargetPageId(parentPage.getId());
    newDraftPage.setTargetPageRevision("1");
    newDraftPage.setContent(draftPage.getContent());
    newDraftPage.setCreatedDate(new Date(clientTime));
    newDraftPage.setUpdatedDate(new Date(clientTime));

    Wiki wiki = getWikiByTypeAndOwner(PortalConfig.USER_TYPE, Utils.getCurrentUser());
    if(wiki == null) {
      // create user wiki to store draft pages
      createWiki(PortalConfig.USER_TYPE, Utils.getCurrentUser());
    }

    dataStorage.createDraftPageForUser(newDraftPage, Utils.getCurrentUser());

    return newDraftPage;
  }

  @Override
  public DraftPage createDraftForExistPage(DraftPage draftPage, Page targetPage, String revision, long clientTime) throws WikiException {
    // Create suffix for draft name
    String draftSuffix = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date(clientTime));

    DraftPage newDraftPage = new DraftPage();
    newDraftPage.setName(targetPage.getName() + "_" + draftSuffix);
    newDraftPage.setNewPage(false);
    newDraftPage.setTitle(draftPage.getTitle());
    newDraftPage.setTargetPageId(targetPage.getId());
    newDraftPage.setContent(draftPage.getContent());
    newDraftPage.setCreatedDate(new Date(clientTime));
    newDraftPage.setUpdatedDate(new Date(clientTime));
    if (StringUtils.isEmpty(revision)) {
      List<PageVersion> versions = getVersionsOfPage(targetPage);
      if(versions != null && !versions.isEmpty()) {
        newDraftPage.setTargetPageRevision(versions.get(0).getName());
      } else {
        newDraftPage.setTargetPageRevision("1");
      }
    } else {
      newDraftPage.setTargetPageRevision(revision);
    }

    Wiki wiki = getWikiByTypeAndOwner(PortalConfig.USER_TYPE, Utils.getCurrentUser());
    if(wiki == null) {
      // create user wiki to store draft pages
      createWiki(PortalConfig.USER_TYPE, Utils.getCurrentUser());
    }

    dataStorage.createDraftPageForUser(newDraftPage, Utils.getCurrentUser());

    return newDraftPage;
  }

  @Override
  public DraftPage getDraftOfPage(Page page) throws WikiException {
    List<DraftPage> draftPages = getDraftsOfUser(Utils.getCurrentUser());
    for(DraftPage draftPage : draftPages) {
      if(draftPage.getTargetPageId() != null && draftPage.getTargetPageId().equals(page.getId())) {
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
    Page page = getPageOfWikiByName(param.getType(), param.getOwner(), param.getPageName());
    dataStorage.deleteDraftOfPage(page, Utils.getCurrentUser());
  }

  @Override
  public void removeDraft(String draftName) throws WikiException {
    dataStorage.deleteDraftByName(draftName, Utils.getCurrentUser());
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
    String targetRevision = draftPage.getTargetPageRevision();
    if (targetRevision == null) {
      return false;
    }

    if (targetRevision.equals("rootVersion")) {
      targetRevision = "1";
    }

    Page targetPage = getPageById(draftPage.getTargetPageId());
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
    String targetContent = StringUtils.EMPTY;

    if (!draftPage.isNewPage()) {
      Wiki wiki = getWikiByTypeAndOwner(draftPage.getWikiType(), draftPage.getWikiOwner());
      Page targetPage = getPageById(draftPage.getTargetPageId());
      if (targetPage != null) {
        List<PageVersion> versions = getVersionsOfPage(targetPage);
        if(versions != null && !versions.isEmpty()) {
          PageVersion lastestRevision = versions.get(0);
          targetContent = lastestRevision.getContent();
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
  public List<Attachment> getAttachmentsOfPage(Page page, boolean loadContent) throws WikiException {
    return dataStorage.getAttachmentsOfPage(page, loadContent);
  }

  @Override
  public int getNbOfAttachmentsOfPage(Page page) throws WikiException {
    int nbOfAttachments = 0;

    WikiPageParams wikiPageParams = new WikiPageParams(page.getWikiType(), page.getWikiOwner(), page.getName());
    MarkupKey key = new MarkupKey(wikiPageParams, Syntax.XWIKI_2_0.toIdString(), Syntax.XHTML_1_0.toIdString(), false);
    Integer cacheKey = new Integer(key.hashCode());
    AttachmentCountData cachedNbOfAttachments = attachmentCountCache.get(cacheKey);
    if(cachedNbOfAttachments != null) {
      nbOfAttachments = cachedNbOfAttachments.build();
    } else {
      try {
        List<Attachment> attachments = dataStorage.getAttachmentsOfPage(page,false);
        nbOfAttachments = attachments == null ? 0 : attachments.size();
        attachmentCountCache.put(cacheKey, new AttachmentCountData(nbOfAttachments));
      } catch (WikiException e) {
        log.error("Cannot get number of attachments of " + page.getWikiType() + ":" + page.getWikiOwner()
                + ":" + page.getName() + " - Cause : " + e.getMessage(), e);
      }
    }
    return nbOfAttachments;
  }

  @Override
  public Attachment getAttachmentOfPageByName(String attachmentName, Page page) throws WikiException {
    return getAttachmentOfPageByName(attachmentName, page, false);
  }

  @Override
  public Attachment getAttachmentOfPageByName(String attachmentName, Page page, boolean loadContent) throws WikiException {
    Attachment attachment = null;
    List<Attachment> attachments = dataStorage.getAttachmentsOfPage(page, loadContent);
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

    invalidateAttachmentCache(page);

    //Call listener
    addAttachment(attachment, page);
  }

  @Override
  public void deleteAttachmentOfPage(String attachmentId, Page page) throws WikiException {

    //Call listener
    deleteAttachment(attachmentId, page);

    dataStorage.deleteAttachmentOfPage(attachmentId, page);

    invalidateAttachmentCache(page);
  }

  /******* Watch *******/

  @Override
  public List<String> getWatchersOfPage(Page page) throws WikiException {
    return dataStorage.getWatchersOfPage(page);
  }

  @Override
  public void addWatcherToPage(String username, Page page) throws WikiException {
    dataStorage.addWatcherToPage(username, page);
  }

  @Override
  public void deleteWatcherOfPage(String username, Page page) throws WikiException {
    dataStorage.deleteWatcherOfPage(username, page);
  }

  /******* Spaces *******/

  @Override
  public List<SpaceBean> searchSpaces(String keyword) throws WikiException {
    List<SpaceBean> spaceBeans = new ArrayList<>();

    // Get group wiki
    String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
    try {
      if (StringUtils.isEmpty(keyword)) {
        keyword = "*";
      }
      keyword = keyword.trim();
      //search by keyword
      SpaceFilter spaceFilter = new SpaceFilter(keyword);
      spaceFilter.setRemoteId(currentUser);
      spaceFilter.setAppId("Wiki");

      SpaceService spaceService = (SpaceService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SpaceService.class);
      //search by appId(wiki)
      ListAccess<Space> spaces = spaceService.getAccessibleSpacesByFilter(currentUser, spaceFilter);

      for (Space space : spaces.load(0, spaces.getSize())) {
        String groupId = space.getGroupId();
        String spaceName = space.getDisplayName();
        String avatarUrl = space.getAvatarUrl();
        if (StringUtils.isBlank(avatarUrl)) {
          avatarUrl = getDefaultSpaceAvatarUrl();
        }
        spaceBeans.add(new SpaceBean(groupId, spaceName, PortalConfig.GROUP_TYPE, avatarUrl));
      }
    } catch (ClassNotFoundException e) {
      Collection<Wiki> wikis = getWikisByType(WikiType.GROUP.toString());
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

  private String getDefaultSpaceAvatarUrl() {
    return LinkProvider.SPACE_DEFAULT_AVATAR_URL;
  }

  @Override
  public boolean hasAdminSpacePermission(String wikiType, String owner) throws WikiException {
    ConversationState conversationState = ConversationState.getCurrent();
    Identity user;
    if (conversationState != null) {
      user = conversationState.getIdentity();
      if (userACL != null && userACL.getSuperUser().equals(user.getUserId())) {
        return true;
      }
    } else {
      user = new Identity(IdentityConstants.ANONIM);
    }

    return dataStorage.hasAdminSpacePermission(wikiType, owner, user);
  }

  @Override
  public String getSpaceNameByGroupId(String groupId) {
    SpaceService spaceService = (SpaceService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SpaceService.class);
    Space space = spaceService.getSpaceByGroupId(groupId);
    if (space == null) {
      LOG.warn("Can't find space with group id " + groupId);
      return groupId.substring(groupId.lastIndexOf('/') + 1);
    } else {
      return space.getDisplayName();
    }
  }

  /******* Listeners *******/

  public void postUpdatePage(final String wikiType, final String wikiOwner, final String pageId, Page page, PageUpdateType wikiUpdateType) throws WikiException {
    List<PageWikiListener> listeners = getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.postUpdatePage(wikiType, wikiOwner, pageId, page, wikiUpdateType);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on [%s] failed", l.toString(), page.getName()), e);
        }
      }
    }
  }

  public void postAddPage(final String wikiType, final String wikiOwner, final String pageId, Page page) throws WikiException {
    List<PageWikiListener> listeners = getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.postAddPage(wikiType, wikiOwner, pageId, page);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on [%s] failed", l.toString(), page.getName()), e);
        }
      }
    }
  }

  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    List<PageWikiListener> listeners = getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.postDeletePage(wikiType, wikiOwner, pageId, page);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on [%s] failed", l.toString(), page.getName()), e);
        }
      }
    }
  }

  public void addAttachment(Attachment attachment, Page page) throws WikiException {
    List<AttachmentWikiListener> listeners = getAttachmentListeners();
    for (AttachmentWikiListener l : listeners) {
      try {
        l.addAttachment(attachment, page);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on attachment with name = [%s] failed", l.toString(), attachment.getName()), e);
        }
      }
    }
  }

  public void deleteAttachment(String attachmentId, Page page) throws WikiException {
    List<AttachmentWikiListener> listeners = getAttachmentListeners();
    for (AttachmentWikiListener l : listeners) {
      try {
        l.deleteAttachment(attachmentId, page);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on attachment with name = [%s] failed", l.toString(), attachmentId), e);
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
  public boolean hasAdminPagePermission(String wikiType, String owner) throws WikiException {
    ConversationState conversationState = ConversationState.getCurrent();
    Identity user;
    if (conversationState != null) {
      user = conversationState.getIdentity();
      if (userACL != null && userACL.getSuperUser().equals(user.getUserId())) {
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

  @Override
  public int getUploadLimit() {
    return uploadLimit;
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
    list.add(0, new BreadcrumbData(page.getName(), page.getTitle(), wikiType, wikiOwner));
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

  private boolean isCachePage(String renderedContent) {
    if (uncachedMacroes == null || uncachedMacroes.isEmpty()) {
      return true;
    }
    boolean useCachePage = true;
    for (String macro : uncachedMacroes) {
      String m1 = new StringBuilder().append("{{").append(macro).append("}}").toString();
      String m2 = new StringBuilder().append("{{").append(macro).append("/}}").toString();
      String m3 = new StringBuilder().append("{{").append(macro).append(" ").toString();
      if (renderedContent.contains(m1) || renderedContent.contains(m2) || renderedContent.contains(m3)) {
        useCachePage = false;
        break;
      }
    }
    return useCachePage;
  }


  @Override
  public List<Page> getPagesOfWiki(String wikiType, String wikiOwner) {
    return dataStorage.getPagesOfWiki(wikiType, wikiOwner);
  }

}
