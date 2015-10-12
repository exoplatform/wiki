package org.exoplatform.wiki.service.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.ResourceBundle;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.chromattic.api.ChromatticSession;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.deployment.plugins.XMLDeploymentPlugin;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.Model;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiNodeType;
import org.exoplatform.wiki.mow.api.WikiStore;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.mow.core.api.wiki.AttachmentImpl;
import org.exoplatform.wiki.mow.core.api.wiki.DraftPageImpl;
import org.exoplatform.wiki.mow.core.api.wiki.GroupWiki;
import org.exoplatform.wiki.mow.core.api.wiki.HelpPage;
import org.exoplatform.wiki.mow.core.api.wiki.LinkEntry;
import org.exoplatform.wiki.mow.core.api.wiki.LinkRegistry;
import org.exoplatform.wiki.mow.core.api.wiki.MovedMixin;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PortalWiki;
import org.exoplatform.wiki.mow.core.api.wiki.RemovedMixin;
import org.exoplatform.wiki.mow.core.api.wiki.RenamedMixin;
import org.exoplatform.wiki.mow.core.api.wiki.Template;
import org.exoplatform.wiki.mow.core.api.wiki.TemplateContainer;
import org.exoplatform.wiki.mow.core.api.wiki.Trash;
import org.exoplatform.wiki.mow.core.api.wiki.UserWiki;
import org.exoplatform.wiki.mow.core.api.wiki.WikiContainer;
import org.exoplatform.wiki.mow.core.api.wiki.WikiHome;
import org.exoplatform.wiki.mow.core.api.wiki.WikiImpl;
import org.exoplatform.wiki.rendering.cache.PageRenderingCacheService;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.BreadcrumbData;
import org.exoplatform.wiki.service.IDType;
import org.exoplatform.wiki.service.MetaDataPage;
import org.exoplatform.wiki.service.Permission;
import org.exoplatform.wiki.service.PermissionEntry;
import org.exoplatform.wiki.service.PermissionType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.template.plugin.WikiTemplatePagePlugin;
import org.exoplatform.wiki.utils.Utils;
import org.picocontainer.Startable;
import org.xwiki.rendering.syntax.Syntax;

public class WikiServiceImpl implements WikiService, Startable {

  private static final String UNTITLED_PREFIX = "Untitled_";

  final static private String          PREFERENCES          = "preferences";

  final static private String          DEFAULT_SYNTAX       = "defaultSyntax";
  
  private static final String          DEFAULT_WIKI_NAME    = "wiki";

  final static private int             CIRCULAR_RENAME_FLAG   = 1000;

  private static final long            DEFAULT_SAVE_DRAFT_SEQUENCE_TIME = 30000;

  private ConfigurationManager  configManager;

  private JCRDataStorage        jcrDataStorage;

  private List<ValuesParam> syntaxHelpParams;

  private PropertiesParam           preferencesParams;
  
  private List<ComponentPlugin> plugins_ = new ArrayList<ComponentPlugin>();
  
  private List<WikiTemplatePagePlugin> templatePagePlugins_ = new ArrayList<WikiTemplatePagePlugin>();

  private static final Log      log               = ExoLogger.getLogger(WikiServiceImpl.class);

  private long                  autoSaveInterval;
  
  private long editPageLivingTime_;
  
  private String wikiWebappUri;

  public WikiServiceImpl(ConfigurationManager configManager,
                         JCRDataStorage jcrDataStorage,
                         InitParams initParams) {

    String autoSaveIntervalProperty = System.getProperty("wiki.autosave.interval");
    if ((autoSaveIntervalProperty == null) || autoSaveIntervalProperty.isEmpty()) {
      autoSaveInterval = DEFAULT_SAVE_DRAFT_SEQUENCE_TIME;
    } else {
      autoSaveInterval = Long.parseLong(autoSaveIntervalProperty);
    }

    this.configManager = configManager;
    this.jcrDataStorage = jcrDataStorage;
    if (initParams != null) {
      Iterator<ValuesParam> helps = initParams.getValuesParamIterator();
      if (helps != null)
        syntaxHelpParams = (List<ValuesParam>) IteratorUtils.toList(initParams.getValuesParamIterator());
      else
        syntaxHelpParams = new ArrayList<ValuesParam>();
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
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    ChromatticSession session = wStore.getSession();
    jcrDataStorage.initDefaultTemplatePage(session, configManager, path);
  }

  @Override
  public Page createPage(String wikiType, String wikiOwner, String title, String parentId) throws Exception {
    String pageId = Utils.escapeIllegalJcrChars(title);
    if(isExisting(wikiType, wikiOwner, pageId)) throw new Exception();
    Model model = getModel();
    WikiImpl wiki = (WikiImpl) getWiki(wikiType, wikiOwner, model);
    PageImpl page = wiki.createWikiPage();
    PageImpl parentPage = null;
    parentPage = (PageImpl) getPageById(wikiType, wikiOwner, parentId);
    if (parentPage == null)
      throw new IllegalArgumentException(String.format("[%s]:[%s]:[%s] is not [wikiType]:[wikiOwner]:[pageId] of an existed page!", wikiType, wikiOwner, parentId));    
    page.setName(pageId);
    parentPage.addWikiPage(page);
    ConversationState conversationState = ConversationState.getCurrent();
    String creator = null;
    if (conversationState != null && conversationState.getIdentity() != null) {
      creator = conversationState.getIdentity().getUserId();
    }
    page.setOwner(creator);
    setFullPermissionForOwner(page, creator);
    page.setTitle(title);
    page.getContent().setText("");
    page.makeVersionable();
    
    //update LinkRegistry
    LinkRegistry linkRegistry = wiki.getLinkRegistry();
    String newEntryName = getLinkEntryName(wikiType, wikiOwner, pageId);
    String newEntryAlias = getLinkEntryAlias(wikiType, wikiOwner, pageId);
    LinkEntry newEntry = linkRegistry.getLinkEntries().get(newEntryName);
    if (newEntry == null) {
      newEntry = linkRegistry.createLinkEntry();
      linkRegistry.getLinkEntries().put(newEntryName, newEntry);
      newEntry.setAlias(newEntryAlias);
      newEntry.setTitle(title);
    }
    //This line must be outside if statement to break chaining list when add new page with name that was used in list.
    newEntry.setNewLink(newEntry);
    
    model.save();
    
    return page;
  }
  
  private void setFullPermissionForOwner(PageImpl page, String owner) throws Exception {
    ConversationState conversationState = ConversationState.getCurrent();

    if (conversationState != null) {
      HashMap<String, String[]> permissions = page.getPermission();
      permissions.put(conversationState.getIdentity().getUserId(), org.exoplatform.services.jcr.access.PermissionType.ALL);
      page.setPermission(permissions);
    }
  }
  
  @Override
  public boolean isExisting(String wikiType, String wikiOwner, String pageId) throws Exception {
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    String statement = new WikiSearchData(wikiType, wikiOwner, pageId).getPageConstraint();
    if (statement != null) {
      Iterator<PageImpl> result = wStore.getSession().createQueryBuilder(PageImpl.class)
      .where(statement).get().objects();
      boolean isExisted = result.hasNext();
      if (!isExisted) {
        Page page = getWikiHome(wikiType, wikiOwner);
        if (page != null) {
          String wikiHomeId = Utils.escapeIllegalJcrChars(page.getTitle());
          if (wikiHomeId.equals(pageId)) {
            isExisted = true;
          }
        }
      }
      return isExisted;
    }
    return false;
  }
  
  @Override
  public boolean deletePage(String wikiType, String wikiOwner, String pageId) throws Exception {
    if (WikiNodeType.Definition.WIKI_HOME_NAME.equals(pageId) || pageId == null)
      return false;
    try {
      PageImpl page = (PageImpl) getPageById(wikiType, wikiOwner, pageId);
      invalidateUUIDCache(wikiType, wikiOwner, pageId);
      Model model = getModel();
      WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
      ChromatticSession session = wStore.getSession();
      RemovedMixin mix = session.create(RemovedMixin.class);
      session.setEmbedded(page, RemovedMixin.class, mix);
      mix.setRemovedBy(Utils.getCurrentUser());
      Calendar calendar = GregorianCalendar.getInstance();
      calendar.setTimeInMillis(new Date().getTime()) ;
      mix.setRemovedDate(calendar.getTime());
      mix.setParentPath(page.getParentPage().getPath());
      WikiImpl wiki = (WikiImpl) getWiki(wikiType, wikiOwner, model);
      Trash trash = wiki.getTrash();
      if(trash.isHasPage(page.getName())) {
        PageImpl oldDeleted = trash.getPage(page.getName()) ;
        String removedDate = oldDeleted.getRemovedMixin().getRemovedDate().toGMTString() ;
        String newName = page.getName()+ "_" + removedDate.replaceAll(" ", "-").replaceAll(":", "-");
        trash.addChild(newName, oldDeleted) ;        
      }      
      trash.addRemovedWikiPage(page);      
      
      //update LinkRegistry
      LinkRegistry linkRegistry = wiki.getLinkRegistry();
      if (linkRegistry.getLinkEntries().get(getLinkEntryName(wikiType, wikiOwner, pageId)) != null) {
        linkRegistry.getLinkEntries().get(getLinkEntryName(wikiType, wikiOwner, pageId)).setNewLink(null);
      }     
      // Post delete activity for all children pages
      Queue<PageImpl> queue = new LinkedList<PageImpl>();
      queue.add(page);
      PageImpl tempPage = null;
      while(!queue.isEmpty()) {
        tempPage = queue.poll();
        postDeletePage(wikiType, wikiOwner, tempPage.getName(), tempPage);
        Iterator<PageImpl> iter = tempPage.getChildPages().values().iterator();
        while(iter.hasNext()) {
          queue.add(iter.next());
        }
      }
      session.save();

    } catch (Exception e) {
      log.error("Can't delete page '" + pageId + "' ", e) ;
      return false;
    }
    return true;    
  }
  
  private void invalidateUUIDCache(String wikiType, String wikiOwner, String pageId) throws Exception {
    PageImpl page = (PageImpl) getPageById(wikiType, wikiOwner, pageId);
    
    Queue<PageImpl> queue = new LinkedList<PageImpl>();
    queue.add(page);
    while (!queue.isEmpty()) {
      PageImpl currentPage = queue.poll();
      org.exoplatform.wiki.rendering.util.Utils.getService(PageRenderingCacheService.class)
         .invalidateUUIDCache(new WikiPageParams(wikiType, wikiOwner, currentPage.getName()));
      for (PageImpl child : currentPage.getChildPages().values()) {
        queue.add(child);
      }
    }
  }

  @Override
  public void deleteTemplatePage(String wikiType, String wikiOwner, String templateId) throws Exception {
    WikiPageParams params = new WikiPageParams(wikiType, wikiOwner, templateId);
    getTemplatePage(params, templateId).remove();
  }  
  
  @Override
  public void deleteDraftNewPage(String newDraftPageId) throws Exception {
    RepositoryService repoService = (RepositoryService) PortalContainer.getInstance()
                                                                       .getComponentInstanceOfType(RepositoryService.class);
    try {
      repoService.getCurrentRepository();
    } catch (Exception e) {
      log.info("Can not get current repository. Drap page will removed in next starting service");
      return;
    }
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    PageImpl draftNewPagesContainer = wStore.getDraftNewPagesContainer();
    PageImpl draftPage = (PageImpl) draftNewPagesContainer.getChild(newDraftPageId);
    if (draftPage != null){
      draftPage.remove();
    }
  }

  @Override
  public boolean renamePage(String wikiType,
                            String wikiOwner,
                            String pageName,
                            String newName,
                            String newTitle) throws Exception {
    if (WikiNodeType.Definition.WIKI_HOME_NAME.equals(pageName) || pageName == null)
      return false;
    PageImpl currentPage = (PageImpl) getPageById(wikiType, wikiOwner, pageName);
    PageImpl parentPage = currentPage.getParentPage();
    RenamedMixin mix = currentPage.getRenamedMixin();
    if (mix == null) {        
      mix = parentPage.getChromatticSession().create(RenamedMixin.class);
      currentPage.setRenamedMixin(mix);
      List<String> ids = new ArrayList<String>() ;
      ids.add(pageName);
      mix.setOldPageIds(ids.toArray(new String[]{}));
    }
    List<String> ids = new ArrayList<String>();
    for (String id : mix.getOldPageIds()) {
      ids.add(id);
    }
    mix.setOldPageIds(ids.toArray(new String[] {}));    
    currentPage.setName(newName);
    getModel().save();
    currentPage.setTitle(newTitle) ;
    getModel().save();
    
    //update LinkRegistry
    WikiImpl wiki = (WikiImpl) parentPage.getWiki();
    LinkRegistry linkRegistry = wiki.getLinkRegistry();
    String newEntryName = getLinkEntryName(wikiType, wikiOwner, newName);
    String newEntryAlias = getLinkEntryAlias(wikiType, wikiOwner, newName);
    LinkEntry newEntry = linkRegistry.getLinkEntries().get(newEntryName);
    LinkEntry entry = linkRegistry.getLinkEntries().get(getLinkEntryName(wikiType, wikiOwner, pageName));
    if (newEntry == null) {
      newEntry = linkRegistry.createLinkEntry();
      linkRegistry.getLinkEntries().put(newEntryName, newEntry);
      newEntry.setAlias(newEntryAlias);
      newEntry.setNewLink(newEntry);
      newEntry.setTitle(newTitle);
      if (entry != null) {
        entry.setNewLink(newEntry);
      }
    } else if (entry == null) {
      newEntry.setNewLink(newEntry);
    } else {
      processCircularRename(entry, newEntry);
    }
    parentPage.getChromatticSession().save();
    
    // Invaliding cache
    org.exoplatform.wiki.rendering.util.Utils.getService(PageRenderingCacheService.class)
      .invalidateCache(new WikiPageParams(wikiType, wikiOwner, pageName));
    org.exoplatform.wiki.rendering.util.Utils.getService(PageRenderingCacheService.class)
      .invalidateUUIDCache(new WikiPageParams(wikiType, wikiOwner, pageName));
    return true ;    
  }

  @Override
  public boolean movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws Exception {
    try {
      PageImpl destPage = (PageImpl) getPageById(newLocationParams.getType(),
                                                 newLocationParams.getOwner(),
                                                 newLocationParams.getPageId());
      if (destPage == null || !destPage.hasPermission(PermissionType.EDITPAGE))
        return false;
      Model model = getModel();
      WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
      ChromatticSession session = wStore.getSession();
      PageImpl movePage = (PageImpl) getPageById(currentLocationParams.getType(),
                                                 currentLocationParams.getOwner(),
                                                 currentLocationParams.getPageId());
      WikiImpl sourceWiki = (WikiImpl) movePage.getWiki();
      MovedMixin mix = movePage.getMovedMixin();
      if (mix == null) {
        movePage.setMovedMixin(session.create(MovedMixin.class));
        mix = movePage.getMovedMixin();
        mix.setTargetPage(movePage.getParentPage());
      }
      mix.setTargetPage(destPage);      
      WikiImpl destWiki = (WikiImpl) destPage.getWiki();
      movePage.setParentPage(destPage);
      movePage.setMinorEdit(false);
      
      // Update permission if moving page to other space or other wiki
      Collection<AttachmentImpl> attachments = ((PageImpl) movePage).getAttachmentsExcludeContentByRootPermisison();
      HashMap<String, String[]> pagePermission = (HashMap<String, String[]>)movePage.getPermission();
      if (PortalConfig.GROUP_TYPE.equals(currentLocationParams.getType()) 
          && (!currentLocationParams.getOwner().equals(newLocationParams.getOwner())
              || !PortalConfig.GROUP_TYPE.equals(newLocationParams.getType()))) {
        // Remove old space permission first
        Iterator<Entry<String, String[]>> pagePermissionIterator = pagePermission.entrySet().iterator();
        while (pagePermissionIterator.hasNext()) {
          Entry<String, String[]> permissionEntry = pagePermissionIterator.next();
          if (StringUtils.substringAfter(permissionEntry.getKey(), ":").equals(currentLocationParams.getOwner())) {
            pagePermissionIterator.remove();
          }
        }
        for (AttachmentImpl attachment : attachments) {
          HashMap<String, String[]> attachmentPermission = (HashMap<String, String[]>)attachment.getPermission();
          Iterator<Entry<String, String[]>> attachmentPermissionIterator = attachmentPermission.entrySet().iterator();
          while (attachmentPermissionIterator.hasNext()) {
            Entry<String, String[]> permissionEntry = attachmentPermissionIterator.next();
            if (StringUtils.substringAfter(permissionEntry.getKey(), ":").equals(currentLocationParams.getOwner())) {
              attachmentPermissionIterator.remove();
            }
          }
          attachment.setPermission(attachmentPermission);
        }
      }
      
      // Update permission by inherit from parent
      HashMap<String, String[]> parentPermissions = (HashMap<String, String[]>)destPage.getPermission();
      pagePermission.putAll(parentPermissions);
      
      // Set permission to page
      movePage.setPermission(pagePermission);
      
      for (AttachmentImpl attachment : attachments) {
        HashMap<String, String[]> attachmentPermission = (HashMap<String, String[]>)attachment.getPermission();
        attachmentPermission.putAll(parentPermissions);
        attachment.setPermission(attachmentPermission);
      }
      
      
      //update LinkRegistry
      if (!newLocationParams.getType().equals(currentLocationParams.getType())
          || (PortalConfig.GROUP_TYPE.equals(currentLocationParams.getType())
              && !currentLocationParams.getOwner().equals(newLocationParams.getOwner()))) {
        LinkRegistry sourceLinkRegistry = sourceWiki.getLinkRegistry();
        LinkRegistry destLinkRegistry = destWiki.getLinkRegistry();
        String newEntryName = getLinkEntryName(newLocationParams.getType(),
                newLocationParams.getOwner(),
                currentLocationParams.getPageId());
        String newEntryAlias = getLinkEntryAlias(newLocationParams.getType(),
                newLocationParams.getOwner(),
                currentLocationParams.getPageId());
        LinkEntry newEntry = destLinkRegistry.getLinkEntries().get(newEntryName);
        LinkEntry entry =
                sourceLinkRegistry.getLinkEntries().get(
                        getLinkEntryName(currentLocationParams.getType(),
                                currentLocationParams.getOwner(),
                                currentLocationParams.getPageId()));
        if (newEntry == null) {
          newEntry = destLinkRegistry.createLinkEntry();
          destLinkRegistry.getLinkEntries().put(newEntryName, newEntry);
          newEntry.setAlias(newEntryAlias);
          newEntry.setNewLink(newEntry);
          newEntry.setTitle(destPage.getTitle());
          if (entry != null) {
            entry.setNewLink(newEntry);
          }
        } else if (entry == null) {
          newEntry.setNewLink(newEntry);
        } else {
          processCircularRename(entry, newEntry);
        }
      }
      session.save();
      
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
    List<PermissionEntry> permissionEntries = new ArrayList<PermissionEntry>();
    Model model = getModel();
    Wiki wiki = getWikiWithoutPermission(wikiType, wikiOwner, model);
    if (wiki == null) {
      return permissionEntries;
    }
    if (!wiki.getDefaultPermissionsInited()) {
      List<String> permissions = getWikiDefaultPermissions(wikiType, wikiOwner);
      wiki.setWikiPermissions(permissions);
      wiki.setDefaultPermissionsInited(true);
      HashMap<String, String[]> permMap = new HashMap<String, String[]>();
      for (String perm : permissions) {
        String[] actions = perm.substring(0, perm.indexOf(":")).split(",");
        perm = perm.substring(perm.indexOf(":") + 1);
        String id = perm.substring(perm.indexOf(":") + 1);
        List<String> jcrActions = new ArrayList<String>();
        for (String action : actions) {
          if (PermissionType.VIEWPAGE.toString().equals(action)) {
            jcrActions.add(org.exoplatform.services.jcr.access.PermissionType.READ);
          } else if (PermissionType.EDITPAGE.toString().equals(action)) {
            jcrActions.add(org.exoplatform.services.jcr.access.PermissionType.ADD_NODE);
            jcrActions.add(org.exoplatform.services.jcr.access.PermissionType.REMOVE);
            jcrActions.add(org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY);
          }
        }
        permMap.put(id, jcrActions.toArray(new String[jcrActions.size()]));
      }
      updateAllPagesPermissions(wikiType, wikiOwner, permMap);
    }
    List<String> permissions = wiki.getWikiPermissions();
    for (String perm : permissions) {
      String[] actions = perm.substring(0, perm.indexOf(":")).split(",");
      perm = perm.substring(perm.indexOf(":") + 1);
      String idType = perm.substring(0, perm.indexOf(":"));
      String id = perm.substring(perm.indexOf(":") + 1);

      PermissionEntry entry = new PermissionEntry();
      if (IDType.USER.toString().equals(idType)) {
        entry.setIdType(IDType.USER);
      } else if (IDType.GROUP.toString().equals(idType)) {
        entry.setIdType(IDType.GROUP);
      } else if (IDType.MEMBERSHIP.toString().equals(idType)) {
        entry.setIdType(IDType.MEMBERSHIP);
      }
      entry.setId(id);
      Permission[] perms = new Permission[4];
      perms[0] = new Permission();
      perms[0].setPermissionType(PermissionType.VIEWPAGE);
      perms[1] = new Permission();
      perms[1].setPermissionType(PermissionType.EDITPAGE);
      perms[2] = new Permission();
      perms[2].setPermissionType(PermissionType.ADMINPAGE);
      perms[3] = new Permission();
      perms[3].setPermissionType(PermissionType.ADMINSPACE);
      for (String action : actions) {
        if (PermissionType.VIEWPAGE.toString().equals(action)) {
          perms[0].setAllowed(true);
        } else if (PermissionType.EDITPAGE.toString().equals(action)) {
          perms[1].setAllowed(true);
        } else if (PermissionType.ADMINPAGE.toString().equals(action)) {
          perms[2].setAllowed(true);
        } else if (PermissionType.ADMINSPACE.toString().equals(action)) {
          perms[3].setAllowed(true);
        }
      }
      entry.setPermissions(perms);

      permissionEntries.add(entry);
    }
    return permissionEntries;
  }
  
  @Override
  public void setWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws Exception {
    Model model = getModel();
    WikiImpl wiki = (WikiImpl) getWiki(wikiType, wikiOwner, model);
    List<String> permissions = new ArrayList<String>();
    HashMap<String, String[]> permMap = new HashMap<String, String[]>();
    for (PermissionEntry entry : permissionEntries) {
      StringBuilder actions = new StringBuilder();
      Permission[] pers = entry.getPermissions();
      List<String> permlist = new ArrayList<String>();
      // Permission strings has the format:
      // VIEWPAGE,EDITPAGE,ADMINPAGE,ADMINSPACE:USER:john
      // VIEWPAGE:GROUP:/platform/users
      // VIEWPAGE,EDITPAGE,ADMINPAGE,ADMINSPACE:MEMBERSHIP:manager:/platform/administrators
      for (int i = 0; i < pers.length; i++) {
        Permission perm = pers[i];
        if (perm.isAllowed()) {
          actions.append(perm.getPermissionType().toString());
          if (i < pers.length - 1) {
            actions.append(",");
          }
          
          if (perm.getPermissionType().equals(PermissionType.VIEWPAGE)) {
            permlist.add(org.exoplatform.services.jcr.access.PermissionType.READ);
          } else if (perm.getPermissionType().equals(PermissionType.EDITPAGE)) {
            permlist.add(org.exoplatform.services.jcr.access.PermissionType.ADD_NODE);
            permlist.add(org.exoplatform.services.jcr.access.PermissionType.REMOVE);
            permlist.add(org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY);
          }
        }
      }
      if (actions.toString().length() > 0) {
        actions.append(":").append(entry.getIdType()).append(":").append(entry.getId());
        permissions.add(actions.toString());
      }
      if (permlist.size() > 0) {
        permMap.put(entry.getId(), permlist.toArray(new String[permlist.size()]));
      }
    }
    wiki.setWikiPermissions(permissions);
    // TODO: study performance
    updateAllPagesPermissions(wikiType, wikiOwner, permMap);
  }

  
  @Override
  public Page getPageById(String wikiType, String wikiOwner, String pageId) throws Exception {
    if(pageId.equals(Utils.unescapeIllegalJcrChars(pageId))) {
      pageId = Utils.escapeIllegalJcrChars(pageId);
    }
    Page page = org.exoplatform.wiki.rendering.util.Utils.getService(PageRenderingCacheService.class)
       .getPageByParams(new WikiPageParams(wikiType, wikiOwner, pageId));
    if (page != null && !page.hasPermission(PermissionType.VIEWPAGE)) {
      page = null;
    }
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
    PageImpl page = null;
    if (WikiNodeType.Definition.WIKI_HOME_NAME.equals(pageId) || pageId == null) {
      page = getWikiHome(wikiType, wikiOwner);
    } else {
      String statement = new WikiSearchData(wikiType, wikiOwner, pageId).getPageConstraint();
      if (statement != null) {
        Model model = getModel();
        WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
        ChromatticSession session = wStore.getSession();
        if (session != null) {
          page = searchPage(statement, session);
          if (page == null && (page = getWikiHome(wikiType, wikiOwner)) != null) {
            String wikiHomeId = TitleResolver.getId(page.getTitle(), true);
            if (!wikiHomeId.equals(pageId)) {
              page = null;
            }
          }
        }
      }
    }
    // Check to remove the domain in page url
    checkToRemoveDomainInUrl(page);
    return page;
  }
  
  private void checkToRemoveDomainInUrl(PageImpl page) {
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
    Model model = getModel();
    WikiImpl wiki = (WikiImpl) getWiki(wikiType, wikiOwner, model);
    LinkRegistry linkRegistry = wiki.getLinkRegistry();
    LinkEntry oldLinkEntry = linkRegistry.getLinkEntries().get(getLinkEntryName(wikiType, wikiOwner, pageId));
    LinkEntry newLinkEntry = null;
    if (oldLinkEntry != null) {
      newLinkEntry = oldLinkEntry.getNewLink();
    }
    int circularFlag = CIRCULAR_RENAME_FLAG;// To deal with old circular data if it is existed
    while (newLinkEntry != null && !newLinkEntry.equals(oldLinkEntry) && circularFlag > 0) {
      oldLinkEntry = newLinkEntry;
      newLinkEntry = oldLinkEntry.getNewLink();
      circularFlag--;
    }
    if (newLinkEntry == null) {
      return null;
    }
    if (circularFlag == 0) {
      // Find link entry mapped with an existed page in old circular data
      circularFlag = CIRCULAR_RENAME_FLAG;
      while (circularFlag > 0) {
        if (getPageWithLinkEntry(newLinkEntry) != null) {
          break;
        }
        newLinkEntry = newLinkEntry.getNewLink();
        circularFlag--;
      }
      // Break old circular data
      if (circularFlag > 0) {
        newLinkEntry.setNewLink(newLinkEntry);
      }
    }
    return getPageWithLinkEntry(newLinkEntry);
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
    
    // if this is ANONIM then use draft in DraftNewPagesContainer 
    if (IdentityConstants.ANONIM.equals(username)) {
      Model model = getModel();
      WikiStore wStore = model.getWikiStore();
      PageImpl draftNewPagesContainer = wStore.getDraftNewPagesContainer();
      Page draftPage = draftNewPagesContainer.getChildPages().get(pageId);
      if (draftPage == null) {
        draftPage = wStore.createPage();
        draftPage.setName(pageId);
        draftNewPagesContainer.addPublicPage(draftPage);
      }
      return draftPage;
    }
    
    // check to get draft if exist
    Model model = getModel();
    UserWiki userWiki = null;
    DraftPageImpl draftPage = null;
    
    // Check if in the case that access to wiki page by rest service of xwiki
    if ((username == null) && (pageId.indexOf(Utils.SPLIT_TEXT_OF_DRAFT_FOR_NEW_PAGE) > -1)) {
      String[] texts = pageId.split(Utils.SPLIT_TEXT_OF_DRAFT_FOR_NEW_PAGE);
      username = texts[0];
      WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
      WikiContainer<UserWiki> userWikiContainer = wStore.getWikiContainer(WikiType.USER);
      userWiki = userWikiContainer.getWiki(username, true);
      Collection<PageImpl> childPages = userWiki.getDraftPagesContainer().getChildrenByRootPermission().values();
      
      // Change collection to List
      for (PageImpl pageImpl : childPages) {
        if (pageImpl.getName().equals(pageId)) {
          return pageImpl;
        }
      }
    } else {
      // Get draft page
      draftPage = (DraftPageImpl) getDraft(pageId);
      if (draftPage != null) {
        return draftPage;
      }
    }
    
    // Get draft page container
    if (userWiki == null) {
      userWiki = (UserWiki) getWiki(PortalConfig.USER_TYPE, username, model);
    }
    PageImpl draftPagesContainer = userWiki.getDraftPagesContainer();
    
    // Create new draft
    draftPage = userWiki.createDraftPage();
    draftPage.setName(pageId);
    draftPagesContainer.addWikiPage(draftPage);
    draftPage.setNewPage(true);
    draftPage.setTargetPage(null);
    draftPage.setTargetRevision("1");
    
    // Put any permisison to access by xwiki rest service
    HashMap<String, String[]> permissions = draftPage.getPermission();
    permissions.put(IdentityConstants.ANY, new String[] { org.exoplatform.services.jcr.access.PermissionType.READ });
    draftPage.setPermission(permissions);
    return draftPage;
  }
  
  @Override
  public Page getPageByUUID(String uuid) throws Exception {
    if (uuid == null) {
      return null;
    }
    
    Model model = getModel();
    WikiStore wStore = model.getWikiStore();
    return jcrDataStorage.getWikiPageByUUID(wStore.getSession(), uuid);
  }
  
  @Override
  public Template getTemplatePage(WikiPageParams params, String templateId) throws Exception {
    return getTemplatesContainer(params).getTemplate(templateId);
  }

  @Override
  public Map<String,Template> getTemplates(WikiPageParams params) throws Exception {
    return getTemplatesContainer(params).getTemplates();
  }
  
  @Override
  public TemplateContainer getTemplatesContainer(WikiPageParams params) throws Exception {
    Model model = getModel();
    WikiImpl wiki = (WikiImpl) getWiki(params.getType(), params.getOwner(), model);
    return wiki.getPreferences().getTemplateContainer();
  }
  
  @Override
  public void modifyTemplate(WikiPageParams params,
                             Template template,
                             String newTitle,
                             String newDescription,
                             String newContent,
                             String newSyntaxId) throws Exception {
    if (newTitle != null) {
      template = getTemplatesContainer(params).addPage(Utils.escapeIllegalJcrChars(newTitle), template);
      template.setDescription(StringEscapeUtils.escapeHtml(newDescription));
      template.setTitle(newTitle);
      template.getContent().setText(newContent);
      template.setSyntax(newSyntaxId);
    }
  }

  @Override
  public PageList<SearchResult> search(WikiSearchData data) throws Exception {
    Model model = getModel();
    try {
      WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
      PageList<SearchResult> result = jcrDataStorage.search(wStore.getSession(), data);
      
      if ((data.getTitle() != null) && (data.getWikiType() != null) && (data.getWikiOwner() != null) && (result.getPageSize() > 0)) {
        PageImpl homePage = getWikiHome(data.getWikiType(), data.getWikiOwner());
        if (data.getTitle().equals("") || homePage != null && homePage.getTitle().contains(data.getTitle())) {
          Calendar wikiHomeCreateDate = Calendar.getInstance();
          wikiHomeCreateDate.setTime(homePage.getCreatedDate());
          
          Calendar wikiHomeUpdateDate = Calendar.getInstance();
          wikiHomeUpdateDate.setTime(homePage.getUpdatedDate());
          
          SearchResult wikiHomeResult = new SearchResult(null, homePage.getTitle(), homePage.getPath(), WikiNodeType.WIKI_HOME.toString(), wikiHomeUpdateDate, wikiHomeCreateDate);
          wikiHomeResult.setPageName(homePage.getName());          
          List<SearchResult> tempSearchResult = result.getAll();
          tempSearchResult.add(wikiHomeResult);
          result = new ObjectPageList<SearchResult>(tempSearchResult, result.getPageSize());
        }
      }
      return result;
    } catch (Exception e) {
      log.error("Can't search", e);
    }
    return new ObjectPageList<SearchResult>(new ArrayList<SearchResult>(), 0);
  }
  
  @Override
  public List<TemplateSearchResult> searchTemplate(TemplateSearchData data) throws Exception {

    Model model = getModel();
    try {
      WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
      List<TemplateSearchResult> result = jcrDataStorage.searchTemplate(wStore.getSession(),
                                                                            data);
      return result;
    } catch (Exception e) {
      log.error("Can't search", e);
    }
    return null;
  }

  @Override
  public List<SearchResult> searchRenamedPage(String wikiType, String wikiOwner, String pageId) throws Exception {
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    WikiSearchData data = new WikiSearchData(wikiType, wikiOwner, pageId);
    return jcrDataStorage.searchRenamedPage(wStore.getSession(), data);
  }

  @Override
  public Object findByPath(String path, String objectNodeType) {    
    String relPath = path;
    if (relPath.startsWith("/"))
      relPath = relPath.substring(1);
    try {
      Model model = getModel();
      WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
      if (WikiNodeType.WIKI_PAGE.equals(objectNodeType)) {
        return wStore.getSession().findByPath(PageImpl.class, relPath);
      } else if (WikiNodeType.WIKI_ATTACHMENT.equals(objectNodeType)) {
        return wStore.getSession().findByPath(AttachmentImpl.class, relPath);
      } else if (WikiNodeType.WIKI_TEMPLATE.equals(objectNodeType)) {   
        return wStore.getSession().findByPath(Template.class, relPath);
      }
    } catch (Exception e) {
      log.error("Can't find Object", e);
    }  
    return null;
  }

  @Override
  public String getPageTitleOfAttachment(String path) throws Exception {
    try {
      String relPath = path;
      if (relPath.startsWith("/"))
        relPath = relPath.substring(1);
      String temp = relPath.substring(0, relPath.lastIndexOf("/"));
      relPath = temp.substring(0, temp.lastIndexOf("/"));
      Model model = getModel();
      WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
      PageImpl page = wStore.getSession().findByPath(PageImpl.class, relPath);
      return page.getTitle();
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public InputStream getAttachmentAsStream(String path) throws Exception {
    Model model = getModel();
    try {
      WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
      return jcrDataStorage.getAttachmentAsStream(path, wStore.getSession());
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
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    if (wStore.getHelpPageByChromattic() == null ||
        wStore.getHelpPagesContainer().getChildPages().size() == 0) {
      createHelpPages(wStore);
    }
    Iterator<PageImpl> syntaxPageIterator = wStore.getHelpPagesContainer()
                                                  .getChildPages()
                                                  .values()
                                                  .iterator();
    while (syntaxPageIterator.hasNext()) {
      PageImpl syntaxPage = syntaxPageIterator.next();
      if (syntaxPage.getSyntax().equals(syntaxId)) {
        return syntaxPage;
      }
    }
    return null;
  }
  
  @Override
  public Page getMetaDataPage(MetaDataPage metaPage) throws Exception {
    if (MetaDataPage.EMOTION_ICONS_PAGE.equals(metaPage)) {
      Model model = getModel();
      WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
      return wStore.getEmotionIconsPage();
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
      resultList = new ArrayList<PageImpl>();
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
  
  private Model getModel() {
    MOWService mowService = (MOWService) ExoContainerContext.getCurrentContainer()
                                                            .getComponentInstanceOfType(MOWService.class);
    return mowService.getModel();
  }

  private PageImpl searchPage(String statement, ChromatticSession session) throws Exception {
    PageImpl wikiPage = null;
    if (statement != null) {
      Iterator<PageImpl> result = session.createQueryBuilder(PageImpl.class)
                                         .where(statement)
                                         .get()
                                         .objects();
      if (result.hasNext())
        wikiPage = result.next();
    }
    // TODO: still don't know reason but following code is necessary.
    if (wikiPage != null) {
      String path = wikiPage.getPath();
      if (path.startsWith("/")) {
        path = path.substring(1, path.length());
      }
      wikiPage = session.findByPath(PageImpl.class, path);
    }
    if (wikiPage != null) {
    }
    return wikiPage;
  }
  
  @Override
  public Wiki getWiki(String wikiType, String owner) {
    return getWiki(wikiType, owner, getModel());
  }
  
  @Override
  public String getPortalOwner() {
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    List<Wiki> portalWikis = new ArrayList<Wiki>(wStore.getWikiContainer(WikiType.PORTAL).getAllWikis());
    if (portalWikis.size() > 0) {
      return portalWikis.get(0).getOwner();
    }
    return null;
  }
  
  private Wiki getWiki(String wikiType, String owner, Model model) {
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    WikiImpl wiki = null;
    try {
      if (PortalConfig.PORTAL_TYPE.equals(wikiType)) {
        WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
        wiki = portalWikiContainer.getWiki(owner, true);
      } else if (PortalConfig.GROUP_TYPE.equals(wikiType)) {
        WikiContainer<GroupWiki> groupWikiContainer = wStore.getWikiContainer(WikiType.GROUP);
        boolean hasPermission = hasAdminSpacePermission(wikiType, owner);
        wiki = groupWikiContainer.getWiki(owner, hasPermission);
      } else if (PortalConfig.USER_TYPE.equals(wikiType)) {
        boolean hasEditWiki = hasAdminSpacePermission(wikiType, owner);
        WikiContainer<UserWiki> userWikiContainer = wStore.getWikiContainer(WikiType.USER);
        wiki = userWikiContainer.getWiki(owner, hasEditWiki);
      }
      model.save();
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("[WikiService] Cannot get wiki " + wikiType + ":" + owner, e);
      }
    }
    return wiki;
  }
  
  private Wiki getWikiWithoutPermission(String wikiType, String owner, Model model) {
    WikiStore wStore = model.getWikiStore();
    Wiki wiki = null;
    try {
      if (PortalConfig.PORTAL_TYPE.equals(wikiType)) {
        WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
        wiki = portalWikiContainer.getWiki(owner, true);
      } else if (PortalConfig.GROUP_TYPE.equals(wikiType)) {
        WikiContainer<GroupWiki> groupWikiContainer = wStore.getWikiContainer(WikiType.GROUP);
        wiki = groupWikiContainer.getWiki(owner, true);
      } else if (PortalConfig.USER_TYPE.equals(wikiType)) {
        WikiContainer<UserWiki> userWikiContainer = wStore.getWikiContainer(WikiType.USER);
        wiki = userWikiContainer.getWiki(owner, true);
      }
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("[WikiService] Cannot get wiki " + wikiType + ":" + owner, e);
      }
    }
    return wiki;
  }
  
  private List<AccessControlEntry> getAccessControls(String wikiType, String wikiOwner) throws Exception{
    List<AccessControlEntry> aces = new ArrayList<AccessControlEntry>();
    try {
      List<PermissionEntry> permissionEntries = getWikiPermission(wikiType, wikiOwner);
      for (PermissionEntry perm : permissionEntries) {
        Permission[] permissions = perm.getPermissions();
        List<String> actions = new ArrayList<String>();
        for (Permission permission : permissions) {
          if (permission.isAllowed()) {
            actions.add(permission.getPermissionType().toString());
          }
        }
        
        for (String action : actions) {
          aces.add(new AccessControlEntry(perm.getId(), action));
        }
      }
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("failed in method getAccessControls:", e);
      }
    }
    return aces;
  }

  @Override
  public boolean hasAdminSpacePermission(String wikiType, String owner) throws Exception {
    ConversationState conversationState = ConversationState.getCurrent();
    Identity user = null;
    if (conversationState != null) {
      user = conversationState.getIdentity();
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      UserACL acl = (UserACL)container.getComponentInstanceOfType(UserACL.class);
      if(acl != null && acl.getSuperUser().equals(user.getUserId())){
        return true;
      }
    } else {
      user = new Identity(IdentityConstants.ANONIM);
    }
    
    List<AccessControlEntry> aces = getAccessControls(wikiType, owner);
    AccessControlList acl = new AccessControlList(owner, aces);
    String []permission = new String[]{PermissionType.ADMINSPACE.toString()};
    return Utils.hasPermission(acl, permission, user);
  }

  @Override
  public boolean hasAdminPagePermission(String wikiType, String owner) throws Exception {
    ConversationState conversationState = ConversationState.getCurrent();
    Identity user = null;
    if (conversationState != null) {
      user = conversationState.getIdentity();
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      UserACL acl = (UserACL)container.getComponentInstanceOfType(UserACL.class);
      if(acl != null && acl.getSuperUser().equals(user.getUserId())){
        return true;
      }
    } else {
      user = new Identity(IdentityConstants.ANONIM);
    }
    List<AccessControlEntry> aces = getAccessControls(wikiType, owner);
    AccessControlList acl = new AccessControlList(owner, aces);
    String []permission = new String[]{PermissionType.ADMINPAGE.toString()};
    return Utils.hasPermission(acl, permission, user);
  }

  private WikiHome getWikiHome(String wikiType, String owner) throws Exception {
    Model model = getModel();
    WikiImpl wiki = (WikiImpl) getWiki(wikiType, owner, model);
    if (wiki != null) {
      WikiHome wikiHome = wiki.getWikiHome();
      return wikiHome;
    } else {
      return null;
    }

  }

  private List<BreadcrumbData> getBreadcumb(List<BreadcrumbData> list,
                                           String wikiType,
                                           String wikiOwner,
                                           String pageId) throws Exception {
    if (list == null) {
      list = new ArrayList<BreadcrumbData>(5);
    }
    if (pageId == null) {
      return list;
    }
    PageImpl page = (PageImpl) getPageById(wikiType, wikiOwner, pageId);
    if (page == null) {
      return list;
    }
    list.add(0, new BreadcrumbData(page.getName(), page.getPath(), page.getTitle(), wikiType, wikiOwner));
    PageImpl parentPage = page.getParentPage();
    if (parentPage != null) {
      getBreadcumb(list, wikiType, wikiOwner, parentPage.getName());
    }

    return list;
  }

  private synchronized void createHelpPages(WikiStoreImpl wStore) throws Exception {
      PageImpl helpPage = wStore.getHelpPagesContainer();
      if (helpPage.getChildPages().size() == 0) {
        for(ValuesParam syntaxhelpParam : syntaxHelpParams)  {
          try {
            String syntaxName = syntaxhelpParam.getName();
            ArrayList<String> syntaxValues = syntaxhelpParam.getValues();
            String shortFile = syntaxValues.get(0);
            String fullFile = syntaxValues.get(1);
            HelpPage syntaxPage = addSyntaxPage(wStore, helpPage, syntaxName, shortFile, " Short help Page");
            addSyntaxPage(wStore, syntaxPage, syntaxName, fullFile, " Full help Page");
            wStore.getSession().save();
          } catch (Exception e) {
            log.error("Can not create Help page", e);
          }
        }
      }
  }
  
  @Override
  public Template createTemplatePage(String title, WikiPageParams params) throws Exception {
    Model model = getModel();
    TemplateContainer templContainer = getTemplatesContainer(params);
    ConversationState conversationState = ConversationState.getCurrent();
    try {
      Template template = templContainer.createTemplatePage();
      String pageId = Utils.escapeIllegalJcrChars(title);
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

  private HelpPage addSyntaxPage(WikiStoreImpl wStore,
                                 PageImpl parentPage,
                                 String name,
                                 String path,
                                 String type) throws Exception {
    StringBuffer stringContent = new StringBuffer();
    InputStream inputContent = null;
    BufferedReader bufferReader = null;
    String tempLine;
    inputContent = configManager.getInputStream(path);
    bufferReader = new BufferedReader(new InputStreamReader(inputContent));
    while ((tempLine = bufferReader.readLine()) != null) {
      stringContent.append(tempLine + "\n");
    }

    HelpPage syntaxPage = wStore.createHelpPage();
    String realName = name.replace("/", "");
    syntaxPage.setName(realName + type);
    parentPage.addPublicPage(syntaxPage);
    AttachmentImpl content = syntaxPage.getContent();
    syntaxPage.setTitle(realName + type);
    content.setText(stringContent.toString());
    syntaxPage.setSyntax(name);
    syntaxPage.setNonePermission();
    inputContent.close();
    bufferReader.close();
    return syntaxPage;
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

  private String getLinkEntryName(String wikiType, String wikiOwner, String pageId) {
    if (PortalConfig.GROUP_TYPE.equals(wikiType)) {
      wikiOwner = wikiOwner.replace("/", "-");
    }
    return wikiType + "@" + wikiOwner + "@" + pageId;
  }
  
  private String getLinkEntryAlias(String wikiType, String wikiOwner, String pageId) {
    return wikiType + "@" + wikiOwner + "@" + pageId;
  }
  
  private void processCircularRename(LinkEntry entry, LinkEntry newEntry) {
    // Check circular rename
    boolean isCircular = true;
    int circularFlag = CIRCULAR_RENAME_FLAG;// To deal with old circular data if it is existed
    LinkEntry checkEntry = newEntry;
    while (!checkEntry.equals(entry) && circularFlag > 0) {
      checkEntry = checkEntry.getNewLink();
      if (checkEntry == null || (checkEntry.equals(checkEntry.getNewLink()) && !checkEntry.equals(entry))) {
        isCircular = false;
        break;
      }
      circularFlag--;
    }
    if (!isCircular || circularFlag == 0) {
      entry.setNewLink(newEntry);
    } else {
      LinkEntry nextEntry = newEntry.getNewLink();
      while (!nextEntry.equals(newEntry)) {
        LinkEntry deletedEntry = nextEntry;
        nextEntry = nextEntry.getNewLink();
        if (!nextEntry.equals(deletedEntry)) {
          deletedEntry.remove();
        } else {
          deletedEntry.remove();
          break;
        }
      }
    }
    newEntry.setNewLink(newEntry);
  }
  
  private Page getPageWithLinkEntry(LinkEntry entry) throws Exception {
    String linkEntryAlias = entry.getAlias();
    String[] splits = linkEntryAlias.split("@");
    String wikiType = splits[0];
    String wikiOwner = splits[1];
    String pageId = linkEntryAlias.substring((wikiType + "@" + wikiOwner + "@").length());
    return getPageById(wikiType, wikiOwner, pageId);
  }
  
  private void updateAllPagesPermissions(String wikiType, String wikiOwner, HashMap<String, String[]> permMap) throws Exception {    
    PageImpl page = getWikiHome(wikiType, wikiOwner);
    Queue<PageImpl> queue = new LinkedList<PageImpl>();
    queue.add(page);
    while (queue.peek() != null) {
      PageImpl p = (PageImpl) queue.poll();
      if (!p.getOverridePermission()) {
        p.setPermission(permMap);
        p.setUpdateAttachmentMixin(null);
      }
      Iterator<PageImpl> iter = p.getChildPages().values().iterator();
      while (iter.hasNext()) {
        queue.add(iter.next());
      }
    }
  }
  
  @Override
  public List<String> getWikiDefaultPermissions(String wikiType, String wikiOwner) throws Exception {
    String view = new StringBuilder().append(PermissionType.VIEWPAGE).toString();
    String viewEdit = new StringBuilder().append(PermissionType.VIEWPAGE).append(",").append(PermissionType.EDITPAGE).toString();
    String all = new StringBuilder().append(PermissionType.VIEWPAGE)
                                    .append(",")
                                    .append(PermissionType.EDITPAGE)
                                    .append(",")
                                    .append(PermissionType.ADMINPAGE)
                                    .append(",")
                                    .append(PermissionType.ADMINSPACE)
                                    .toString();
    List<String> permissions = new ArrayList<String>();
    Iterator<Entry<String, IDType>> iter = Utils.getACLForAdmins().entrySet().iterator();
    while (iter.hasNext()) {
      Entry<String, IDType> entry = iter.next();
      permissions.add(new StringBuilder(all).append(":").append(entry.getValue()).append(":").append(entry.getKey()).toString());
    }
    if (PortalConfig.PORTAL_TYPE.equals(wikiType)) {
      UserPortalConfigService service = (UserPortalConfigService) ExoContainerContext.getCurrentContainer()
                                                                                     .getComponentInstanceOfType(UserPortalConfigService.class);
      PortalConfig portalConfig = service.getUserPortalConfig(wikiOwner, null).getPortalConfig();
      String portalEditClause = new StringBuilder(all).append(":")
                                                      .append(IDType.MEMBERSHIP)
                                                      .append(":")
                                                      .append(portalConfig.getEditPermission())
                                                      .toString();
      if (!permissions.contains(portalEditClause)) {
        permissions.add(portalEditClause);
      }
      permissions.add(new StringBuilder(view).append(":").append(IDType.USER).append(":any").toString());
    } else if (PortalConfig.GROUP_TYPE.equals(wikiType)) {
      UserACL userACL = (UserACL) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(UserACL.class);
      String makableMTClause = new StringBuilder(all).append(":")
                                                     .append(IDType.MEMBERSHIP)
                                                     .append(":")
                                                     .append(userACL.getMakableMT())
                                                     .append(":")
                                                     .append(wikiOwner)
                                                     .toString();
      if (!permissions.contains(makableMTClause)) {
        permissions.add(makableMTClause);
      }
      String ownerClause = new StringBuilder(viewEdit).append(":")
                                                      .append(IDType.MEMBERSHIP)
                                                      .append(":*:")
                                                      .append(wikiOwner)
                                                      .toString();
      if (!permissions.contains(ownerClause)) {
        permissions.add(ownerClause);
      }
    } else if (PortalConfig.USER_TYPE.equals(wikiType)) {
      String ownerClause = new StringBuilder(all).append(":").append(IDType.USER).append(":").append(wikiOwner).toString();
      if (!permissions.contains(ownerClause)) {
        permissions.add(ownerClause);
      }
    }
    return permissions;
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
    List<PageWikiListener> pageListeners = new ArrayList<PageWikiListener>();
    for (ComponentPlugin c : plugins_) {
      if (c instanceof PageWikiListener) {
        pageListeners.add((PageWikiListener) c);
      }
    }
    return pageListeners;
  }

  public void setTemplatePagePlugin() {
    for (WikiTemplatePagePlugin plugin : templatePagePlugins_) {
      jcrDataStorage.setTemplatePagePlugin(plugin);
    }
  }
  
  @Override
  public UserWiki getOrCreateUserWiki(String username) {
    Model model = getModel();
    return (UserWiki) getWiki(PortalConfig.USER_TYPE, username, model);
  }
  
  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public List<SpaceBean> searchSpaces(String keyword) throws Exception {
    List<SpaceBean> spaceBeans = new ArrayList<SpaceBean>();
    
    // Get group wiki
    String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
    try {
      Class spaceServiceClass = Class.forName("org.exoplatform.social.core.space.spi.SpaceService");
      Object spaceService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(spaceServiceClass);
      
      Class spaceClass = Class.forName("org.exoplatform.social.core.space.model.Space");
      ListAccess spaces = null;
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
      spaces = (ListAccess) spaceServiceClass.getDeclaredMethod("getAccessibleSpacesByFilter", String.class, spaceFilterClass).invoke(spaceService, currentUser, spaceFilter);
      
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
      
      for (Wiki wiki : wikis) {
        if (wiki.getName().contains(keyword)) {
          spaceBeans.add(new SpaceBean(wiki.getOwner(), wiki.getName(), PortalConfig.GROUP_TYPE, ""));
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
      Boolean bool = Boolean.valueOf(String.valueOf(spaceServiceClass.getDeclaredMethod("isMember", spaceClass, String.class).invoke(spaceService, space, userId)));
      return bool.booleanValue();
    } catch (Exception e) {
      log.debug("Can not check if user is space member", e);
      return false;
    }
  }
  
  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
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
    Model model = getModel();
    UserWiki userWiki = (UserWiki) getWiki(PortalConfig.USER_TYPE, Utils.getCurrentUser(), model);
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
    Model model = getModel();
    UserWiki userWiki = (UserWiki) getWiki(PortalConfig.USER_TYPE, Utils.getCurrentUser(), model);
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
    if ((param.getPageId() == null) || (param.getOwner() == null) || (param.getType() == null)) {
      return null;
    }
    
    Page targetPage = getPageById(param.getType(), param.getOwner(), param.getPageId());
    if ((param.getPageId() == null) || (targetPage == null)) {
      return null;
    }
    return getDraftOfWikiPage(targetPage);
  }
  
  @Override
  public DraftPage getLastestDraft() throws Exception {
    if (IdentityConstants.ANONIM.equals(org.exoplatform.wiki.utils.Utils.getCurrentUser())) {
      return null;
    }
    
    // Get all draft pages
    Collection<PageImpl> childPages = getDraftContainerOfCurrentUser().getChildPages().values();
    
    // Find the lastest draft
    DraftPageImpl lastestDraft = null;
    for (PageImpl draft : childPages) {
      DraftPageImpl draftPage = (DraftPageImpl) draft;
      // Compare and get the lastest draft
      if ((lastestDraft == null) || (lastestDraft.getUpdatedDate().getTime() < draftPage.getUpdatedDate().getTime())) {
        lastestDraft = draftPage;
      }
    }
    return lastestDraft;
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
    
    List<DraftPage> drafts = getDrafts(org.exoplatform.wiki.utils.Utils.getCurrentUser());
    for (DraftPage draftPage : drafts) {
      if (draftPage.getName().equals(draftName)) {
        return draftPage;
      }
    }
    return null;
  }
  
  @Override
  public void removeDraft(WikiPageParams param) throws Exception {
    DraftPageImpl draftPage = (DraftPageImpl) getDraft(param);
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
    List<DraftPage> draftPages = new ArrayList<DraftPage>();
    if (!IdentityConstants.ANONIM.equals(org.exoplatform.wiki.utils.Utils.getCurrentUser())) {
      // Get all draft of user
      Collection<PageImpl> childPages = getDraftContainerOfCurrentUser().getChildPages().values();
      
      // Change collection to List
      for (PageImpl pageImpl : childPages) {
        draftPages.add((DraftPageImpl) pageImpl);
      }
    }
    return draftPages;
  }
  
  @Override
  public Page getWikiPageByUUID(String uuid) throws Exception {
    Model model = getModel();
    WikiStore wStore = model.getWikiStore();
    return jcrDataStorage.getWikiPageByUUID(wStore.getSession(), uuid);
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
  
  private DraftPage getDraftOfWikiPage(Page targetPage) throws Exception {
    // If target page is null then return null
    if (targetPage == null) {
      return null;
    }
    
    if (IdentityConstants.ANONIM.equals(org.exoplatform.wiki.utils.Utils.getCurrentUser())) {
      return null;
    }
    
    // Get all draft pages
    Collection<PageImpl> childPages = getDraftContainerOfCurrentUser().getChildPages().values();
    
    // Find the lastest draft of target page 
    DraftPageImpl lastestDraft = null;
    for (PageImpl draft : childPages) {
      DraftPageImpl draftPage = (DraftPageImpl) draft;
      // If this draft is use for target page
      if (draftPage.getTargetPage() != null && !draftPage.isNewPage() && draftPage.getTargetPage().equals(targetPage.getJCRPageNode().getUUID())) {
        // Compare and get the lastest draft
        if ((lastestDraft == null) || (lastestDraft.getUpdatedDate().getTime() < draftPage.getUpdatedDate().getTime())) {
          lastestDraft = draftPage;
        }
      }
    }
    return lastestDraft;
  }
  
  private PageImpl getDraftContainerOfCurrentUser() {
    Model model = getModel();
    UserWiki userWiki = (UserWiki) getWiki(PortalConfig.USER_TYPE, Utils.getCurrentUser(), model);
    return userWiki.getDraftPagesContainer();
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
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    Wiki wiki = null;
    if (wikiId.startsWith("/spaces/")) {
      wiki = wikiService.getWiki(PortalConfig.GROUP_TYPE, wikiId);
    } else if (wikiId.startsWith("/user/")) {
      wikiId = wikiId.substring(wikiId.lastIndexOf('/') + 1);
      wiki = wikiService.getWiki(PortalConfig.USER_TYPE, wikiId);
    } else if (wikiId.startsWith("/" + Utils.getPortalName())) {
      wikiId = wikiId.substring(wikiId.lastIndexOf('/') + 1);
      wiki = wikiService.getWiki(PortalConfig.PORTAL_TYPE, wikiId);
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
        String mySpaceLabel = res.getString("UISpaceSwitcher.title.my-space");
        return mySpaceLabel;
      }
      return wiki.getOwner();
    }
    
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    return wikiService.getSpaceNameByGroupId(wiki.getOwner());
  }
  
  @Override
  public void start() {
    try {
      ChromatticManager chromatticManager = (ChromatticManager) ExoContainerContext.getCurrentContainer()
                                                                                   .getComponentInstanceOfType(ChromatticManager.class);
      RequestLifeCycle.begin(chromatticManager);
      try {
        setTemplatePagePlugin();
      } catch (Exception e) {
        log.warn("Can not init page templates ...");
      }
      addEmotionIcons();
      removeHelpPages();
      try {
        getWikiHome(PortalConfig.GROUP_TYPE, "sandbox");
      } catch (Exception e) {
        log.warn("Can not init sandbox wiki ...");
      }
    } catch (Exception e) {
      log.warn("Can not start WikiService ...", e);
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  public void stop() {
  }
}
