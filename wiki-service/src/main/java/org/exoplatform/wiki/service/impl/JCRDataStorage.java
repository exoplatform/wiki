package org.exoplatform.wiki.service.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.chromattic.api.ChromatticSession;
import org.chromattic.common.IO;
import org.chromattic.core.api.ChromatticSessionImpl;
import org.chromattic.ext.ntdef.Resource;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.chromattic.ext.ntdef.NTVersion;
import org.exoplatform.wiki.chromattic.ext.ntdef.VersionableMixin;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.mow.core.api.wiki.*;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.DataStorage;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.search.*;
import org.exoplatform.wiki.service.search.jcr.JCRTemplateSearchQueryBuilder;
import org.exoplatform.wiki.service.search.jcr.JCRWikiSearchQueryBuilder;
import org.exoplatform.wiki.utils.JCRUtils;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.VersionNameComparatorDesc;
import org.exoplatform.wiki.utils.WikiConstants;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.io.*;
import java.net.URLConnection;
import java.util.*;

public class JCRDataStorage implements DataStorage {
  private static final Log log = ExoLogger.getLogger(JCRDataStorage.class);

  private static final int MAX_EXCERPT_LENGTH = 430;

  private static final int CIRCULAR_RENAME_FLAG = 1000;

  private MOWService mowService;

  public JCRDataStorage(MOWService mowService) {
    this.mowService = mowService;
  }

  @Override
  public Wiki getWikiByTypeAndOwner(String wikiType, String wikiOwner) throws WikiException {
    WikiImpl wikiImpl = fetchWikiImpl(wikiType, wikiOwner);
    return convertWikiImplToWiki(wikiImpl);
  }

  @Override
  public List<Wiki> getWikisByType(String wikiType) throws WikiException {
    boolean created = mowService.startSynchronization();

    List wikis = new ArrayList();

    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    if(wStore != null) {
      WikiContainer<WikiImpl> wikiContainer = wStore.getWikiContainer(WikiType.valueOf(wikiType.toUpperCase()));
      if(wikiContainer != null) {
        Collection<WikiImpl> allWikis = wikiContainer.getAllWikis();

        if(allWikis != null) {
          for (WikiImpl wikiImpl : allWikis) {
            wikis.add(convertWikiImplToWiki(wikiImpl));
          }
        }
      }
    }

    mowService.stopSynchronization(created);

    return wikis;
  }

  @Override
  public Wiki createWiki(Wiki wiki) throws WikiException {
    boolean created = mowService.startSynchronization();
    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();

    WikiContainer wikiContainer = wStore.getWikiContainer(WikiType.valueOf(wiki.getType().toUpperCase()));
    WikiImpl wikiImpl = wikiContainer.addWiki(wiki);
    // create wiki home page
    wikiImpl.getWikiHome();

    mowService.persist();

    Wiki createdWiki = convertWikiImplToWiki(wikiImpl);

    mowService.stopSynchronization(created);

    return createdWiki;
  }

  /**
   * Create a wiki page with the given pageId, under the node of the parentPage node.
   * @param wiki
   * @param parentPage
   * @param page
   * @return
   * @throws WikiException
   */
  @Override
  public Page createPage(Wiki wiki, Page parentPage, Page page) throws WikiException {
    if (parentPage == null) {
      throw new IllegalArgumentException("Parent page cannot be null when creating the new page " + wiki.getType() + ":" + wiki.getOwner() + ":" + page.getName());
    }

    boolean created = mowService.startSynchronization();

    WikiImpl wikiImpl = fetchWikiImpl(wiki.getType(), wiki.getOwner());
    PageImpl parentPageImpl = fetchPageImpl(parentPage.getWikiType(), parentPage.getWikiOwner(), parentPage.getName());
    PageImpl pageImpl = wikiImpl.createWikiPage();
    pageImpl.setName(page.getName());
    parentPageImpl.addWikiPage(pageImpl);
    pageImpl.setOwner(page.getOwner());
    pageImpl.setPermission(JCRUtils.convertToPermissionMap(page.getPermissions()));
    pageImpl.setTitle(page.getTitle());
    String text = "";
    if(page.getContent() != null) {
      text = page.getContent();
    }
    Date now = GregorianCalendar.getInstance().getTime();
    pageImpl.setCreatedDate(now);
    pageImpl.setUpdatedDate(now);
    pageImpl.setAuthor(page.getAuthor());
    pageImpl.getContent().setText(text);
    pageImpl.setSyntax(page.getSyntax());
    pageImpl.setURL(page.getUrl());

    if(page.getActivityId() != null) {
      pageImpl.setActivityId(page.getActivityId());
    }

    try {
      // create a first version
      pageImpl.makeVersionable();
      pageImpl.checkin();
      pageImpl.checkout();
    } catch (Exception e) {
      log.error("Cannot create first version of page " + wiki.getType() + ":" + wiki.getOwner() + ":" + page.getName()
              + " - Cause : " + e.getMessage(), e);
    }

    //update LinkRegistry
    LinkRegistry linkRegistry = wikiImpl.getLinkRegistry();
    String newEntryName = getLinkEntryName(wiki.getType(), wiki.getOwner(), page.getName());
    String newEntryAlias = getLinkEntryAlias(wiki.getType(), wiki.getOwner(), page.getName());
    LinkEntry newEntry = linkRegistry.getLinkEntries().get(newEntryName);
    if (newEntry == null) {
      newEntry = linkRegistry.createLinkEntry();
      linkRegistry.getLinkEntries().put(newEntryName, newEntry);
      newEntry.setAlias(newEntryAlias);
      newEntry.setTitle(page.getTitle());
    }
    //This line must be outside if statement to break chaining list when add new page with name that was used in list.
    newEntry.setNewLink(newEntry);

    mowService.persist();

    Page createdPage = convertPageImplToPage(pageImpl);

    mowService.stopSynchronization(created);

    return createdPage;
  }

  @Override
  public Page getPageOfWikiByName(String wikiType, String wikiOwner, String pageName) throws WikiException {
    PageImpl pageImpl = null;

    boolean created = mowService.startSynchronization();

    WikiImpl wiki = fetchWikiImpl(wikiType, wikiOwner);

    if(wiki != null) {
      if (WikiConstants.WIKI_HOME_NAME.equals(pageName) || pageName == null) {
        pageImpl = wiki.getWikiHome();
      } else {
        pageImpl = fetchPageImpl(wikiType, wikiOwner, pageName);
        if (pageImpl == null && (pageImpl = wiki.getWikiHome()) != null) {
          String wikiHomeId = TitleResolver.getId(pageImpl.getTitle(), true);
          if (!wikiHomeId.equals(pageName)) {
            pageImpl = null;
          }
        }
      }
    }

    Page page = null;
    if(pageImpl != null) {
      try {
        pageImpl.migrateLegacyData();
        pageImpl.migrateAttachmentPermission();
      } catch(WikiException | RepositoryException e) {
        log.error("Cannot migrate page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
                + page.getName() + " - Cause : " + e.getMessage(), e);
      }
      page = convertPageImplToPage(pageImpl);
      page.setWikiId(wiki.getName());
      page.setWikiType(wiki.getType());
      page.setWikiOwner(wiki.getOwner());
    }

    mowService.stopSynchronization(created);

    return page;
  }

  @Override
  public Page getPageById(String id) throws WikiException {
    boolean created = mowService.startSynchronization();

    ChromatticSession session = mowService.getSession();

    Page page = convertPageImplToPage(session.findById(PageImpl.class, id));

    mowService.stopSynchronization(created);

    return page;
  }

  @Override
  public Page getParentPageOf(Page page) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    PageImpl parentPageImpl = pageImpl.getParentPage();

    Page parentPage = convertPageImplToPage(parentPageImpl);
    if(parentPage != null) {
      parentPage.setWikiId(page.getWikiId());
      parentPage.setWikiType(page.getWikiType());
      parentPage.setWikiOwner(page.getWikiOwner());
    }

    mowService.stopSynchronization(created);

    return parentPage;
  }

  @Override
  public List<Page> getChildrenPageOf(Page page) throws WikiException {
    List<Page> childrenPages = new ArrayList<>();

    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    if(pageImpl == null) {
      throw new WikiException("Page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName() + " does not exist, cannot get its children.");
    }

    try {
      Map<String, PageImpl> childrenPageImpls = pageImpl.getChildPages();
      for (PageImpl childPageImpl : childrenPageImpls.values()) {
        Page childPage = convertPageImplToPage(childPageImpl);
        childPage.setWikiType(page.getWikiType());
        childPage.setWikiOwner(page.getWikiOwner());
        childrenPages.add(childPage);
      }
    } catch(Exception e) {
      log.error("Cannot get children pages of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName()
              + " - Cause : " + e.getMessage(), e);
    }

    mowService.stopSynchronization(created);

    return childrenPages;
  }

  @Override
  public void createTemplatePage(Wiki wiki, Template template) throws WikiException {
    boolean created = mowService.startSynchronization();

    TemplateContainer templatesContainer = getTemplatesContainer(wiki.getType(), wiki.getOwner());

    TemplateImpl templatePage = templatesContainer.createTemplatePage();
    templatePage = templatesContainer.addPage(template.getName(), templatePage);

    templatePage.setTitle(template.getTitle());
    templatePage.setDescription(template.getDescription());
    templatePage.getContent().setText(template.getContent());

    mowService.stopSynchronization(created);
  }

  @Override
  public void updateTemplatePage(Template template) throws WikiException {
    boolean created = mowService.startSynchronization();

    TemplateContainer templatesContainer = getTemplatesContainer(template.getWikiType(), template.getWikiOwner());
    TemplateImpl templateImpl = templatesContainer.getTemplate(template.getName());

    templateImpl.setTitle(template.getTitle());
    templateImpl.setDescription(template.getDescription());
    templateImpl.getContent().setText(template.getContent());
    templateImpl.setSyntax(template.getSyntax());

    mowService.persist();

    mowService.stopSynchronization(created);
  }

  @Override
  public void deleteTemplatePage(String wikiType, String wikiOwner, String templateName) throws WikiException {
    boolean created = mowService.startSynchronization();

    TemplateContainer templatesContainer = getTemplatesContainer(wikiType, wikiOwner);
    TemplateImpl templateImpl = templatesContainer.getTemplate(templateName);
    templateImpl.remove();
    mowService.persist();

    mowService.stopSynchronization(created);
  }

  @Override
  public void deletePage(String wikiType, String wikiOwner, String pageId) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl page = fetchPageImpl(wikiType, wikiOwner, pageId);
    if(page == null) {
      throw new WikiException("Page " + wikiType + ":" + wikiOwner + ":" + pageId + " does not exist, cannot delete it.");
    }

    ChromatticSession session = mowService.getSession();
    RemovedMixin mix = session.create(RemovedMixin.class);
    session.setEmbedded(page, RemovedMixin.class, mix);
    mix.setRemovedBy(Utils.getCurrentUser());
    Calendar calendar = GregorianCalendar.getInstance();
    calendar.setTimeInMillis(new Date().getTime());
    mix.setRemovedDate(calendar.getTime());
    mix.setParentPath(page.getParentPage().getPath());
    WikiImpl wiki = fetchWikiImpl(wikiType, wikiOwner);
    Trash trash = wiki.getTrash();
    if (trash.isHasPage(page.getName())) {
      PageImpl oldDeleted = trash.getPage(page.getName());
      String removedDate = oldDeleted.getRemovedMixin().getRemovedDate().toGMTString();
      String newName = page.getName() + "_" + removedDate.replaceAll(" ", "-").replaceAll(":", "-");
      trash.addChild(newName, oldDeleted);
    }
    trash.addRemovedWikiPage(page);

    //update LinkRegistry
    LinkRegistry linkRegistry = wiki.getLinkRegistry();
    if (linkRegistry.getLinkEntries().get(getLinkEntryName(wikiType, wikiOwner, pageId)) != null) {
      linkRegistry.getLinkEntries().get(getLinkEntryName(wikiType, wikiOwner, pageId)).setNewLink(null);
    }

    session.save();

    mowService.stopSynchronization(created);
  }

  @Override
  public Template getTemplatePage(WikiPageParams params, String templateId) throws WikiException {
    boolean created = mowService.startSynchronization();

    TemplateContainer templatesContainer = getTemplatesContainer(params.getType(), params.getOwner());
    Template template = convertTemplateImplToTemplate(templatesContainer.getTemplate(templateId));
    if(template != null) {
      template.setWikiType(params.getType());
      template.setWikiOwner(params.getOwner());
    }

    mowService.stopSynchronization(created);

    return template;
  }

  @Override
  public Map<String, Template> getTemplates(WikiPageParams params) throws WikiException {
    boolean created = mowService.startSynchronization();

    Map<String, Template> templates = new HashMap<>();
    Map<String, TemplateImpl> templatesImpl = getTemplatesContainer(params.getType(), params.getOwner()).getTemplates();
    for(String templateImplKey : templatesImpl.keySet()) {
      Template template = convertTemplateImplToTemplate(templatesImpl.get(templateImplKey));
      template.setWikiType(params.getType());
      template.setWikiOwner(params.getOwner());
      templates.put(templateImplKey, template);
    }

    mowService.stopSynchronization(created);

    return templates;
  }

  private TemplateContainer getTemplatesContainer(String wikiType, String wikiOwner) throws WikiException {
    boolean created = mowService.startSynchronization();

    WikiImpl wiki = fetchWikiImpl(wikiType, wikiOwner);
    TemplateContainer templateContainer = wiki.getPreferences().getTemplateContainer();

    mowService.stopSynchronization(created);

    return templateContainer;
  }

  @Override
  public void deleteDraftOfPage(Page page, String username) throws WikiException {
    boolean created = mowService.startSynchronization();

    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    UserWiki userWiki = (UserWiki) wStore.getWiki(WikiType.USER, username);
    if(userWiki != null) {
      PageImpl draftPagesContainer = userWiki.getDraftPagesContainer();
      try {
        Map<String, PageImpl> childPages = draftPagesContainer.getChildPages();
        for (PageImpl childPage : childPages.values()) {
          String targetPageId = ((DraftPageImpl) childPage).getTargetPage();
          if (targetPageId != null && targetPageId.equals(page.getId())) {
            childPage.remove();
            return;
          }
        }
      } catch(Exception e) {
        log.error("Cannot get drafts of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName()
                + " - Cause : " + e.getMessage(), e);
      }
    }

    if(log.isDebugEnabled()) {
      log.debug("No draft page of page " + page.getWikiType() + ":" + page.getWikiOwner()
              + ":" + page.getName() + " for user " + username + ", so nothing to delete.");
    }

    mowService.stopSynchronization(created);
  }

  @Override
  public void deleteDraftByName(String newDraftPageName, String username) throws WikiException {
    boolean created = mowService.startSynchronization();

    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    UserWiki userWiki = (UserWiki) wStore.getWiki(WikiType.USER, username);
    if(userWiki == null) {
      mowService.stopSynchronization(created);
      throw new WikiException("Cannot delete draft page with name " + newDraftPageName + " of user " + username + " because no user wiki has been found.");
    }

    PageImpl draftPagesContainer = userWiki.getDraftPagesContainer();
    try {
      Map<String, PageImpl> childPages = draftPagesContainer.getChildPages();
      for (PageImpl childPage : childPages.values()) {
        if (newDraftPageName.equals(childPage.getName())) {
          childPage.remove();
          return;
        }
      }
    } catch(Exception e) {
      throw new WikiException("Cannot delete draft page of with name " + newDraftPageName + " of user " + username, e);
    } finally {
      mowService.stopSynchronization(created);
    }

    throw new WikiException("Cannot delete draft page with name " + newDraftPageName + " of user " + username + " because it does not exist.");
  }

  @Override
  public void renamePage(String wikiType, String wikiOwner, String pageName, String newName, String newTitle) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl currentPage = fetchPageImpl(wikiType, wikiOwner, pageName);
    PageImpl parentPage = currentPage.getParentPage();
    RenamedMixin mix = currentPage.getRenamedMixin();
    if (mix == null) {
      mix = parentPage.getChromatticSession().create(RenamedMixin.class);
      currentPage.setRenamedMixin(mix);
      List<String> ids = new ArrayList<>();
      ids.add(pageName);
      mix.setOldPageIds(ids.toArray(new String[]{}));
    }
    List<String> ids = new ArrayList<>();
    for (String id : mix.getOldPageIds()) {
      ids.add(id);
    }
    mix.setOldPageIds(ids.toArray(new String[]{}));
    currentPage.setName(newName);
    mowService.persist();
    currentPage.setTitle(newTitle);
    mowService.persist();

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

    mowService.stopSynchronization(created);
  }

  @Override
  public void movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl destPage = fetchPageImpl(newLocationParams.getType(),
            newLocationParams.getOwner(),
            newLocationParams.getPageName());
    if (destPage == null || !destPage.hasPermission(PermissionType.EDITPAGE)) {
      throw new WikiException("Destination page " + newLocationParams.getType() + ":" +
              newLocationParams.getOwner() + ":" + newLocationParams.getPageName() + " does not exist");
    }
    ChromatticSession session = mowService.getSession();
    PageImpl movePage = fetchPageImpl(currentLocationParams.getType(),
            currentLocationParams.getOwner(),
            currentLocationParams.getPageName());
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
    Collection<AttachmentImpl> attachments = movePage.getAttachmentsExcludeContentByRootPermisison();
    HashMap<String, String[]> pagePermission = movePage.getPermission();
    if (PortalConfig.GROUP_TYPE.equals(currentLocationParams.getType())
            && (!currentLocationParams.getOwner().equals(newLocationParams.getOwner())
            || !PortalConfig.GROUP_TYPE.equals(newLocationParams.getType()))) {
      // Remove old space permission first
      Iterator<Map.Entry<String, String[]>> pagePermissionIterator = pagePermission.entrySet().iterator();
      while (pagePermissionIterator.hasNext()) {
        Map.Entry<String, String[]> permissionEntry = pagePermissionIterator.next();
        if (StringUtils.substringAfter(permissionEntry.getKey(), ":").equals(currentLocationParams.getOwner())) {
          pagePermissionIterator.remove();
        }
      }
      for (AttachmentImpl attachment : attachments) {
        HashMap<String, String[]> attachmentPermission = attachment.getPermission();
        Iterator<Map.Entry<String, String[]>> attachmentPermissionIterator = attachmentPermission.entrySet().iterator();
        while (attachmentPermissionIterator.hasNext()) {
          Map.Entry<String, String[]> permissionEntry = attachmentPermissionIterator.next();
          if (StringUtils.substringAfter(permissionEntry.getKey(), ":").equals(currentLocationParams.getOwner())) {
            attachmentPermissionIterator.remove();
          }
        }
        attachment.setPermission(attachmentPermission);
      }
    }

    // Update permission by inherit from parent
    HashMap<String, String[]> parentPermissions = destPage.getPermission();
    pagePermission.putAll(parentPermissions);

    // Set permission to page
    movePage.setPermission(pagePermission);

    for (AttachmentImpl attachment : attachments) {
      HashMap<String, String[]> attachmentPermission = attachment.getPermission();
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
              currentLocationParams.getPageName());
      String newEntryAlias = getLinkEntryAlias(newLocationParams.getType(),
              newLocationParams.getOwner(),
              currentLocationParams.getPageName());
      LinkEntry newEntry = destLinkRegistry.getLinkEntries().get(newEntryName);
      LinkEntry entry =
              sourceLinkRegistry.getLinkEntries().get(
                      getLinkEntryName(currentLocationParams.getType(),
                              currentLocationParams.getOwner(),
                              currentLocationParams.getPageName()));
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

    mowService.stopSynchronization(created);
  }

  @Override
  public List<PermissionEntry> getWikiPermission(String wikiType, String wikiOwner) throws WikiException {
    List<PermissionEntry> permissionEntries = new ArrayList<>();

    boolean created = mowService.startSynchronization();

    WikiImpl wikiImpl = fetchWikiImpl(wikiType, wikiOwner);
    if (wikiImpl == null) {
      return permissionEntries;
    }

    List<String> permissions = wikiImpl.getWikiPermissions();
    permissionEntries = JCRUtils.convertWikiPermissionsToPermissionEntryList(permissions);

    mowService.stopSynchronization(created);

    return permissionEntries;
  }

  @Override
  public void updateWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws WikiException {
    boolean created = mowService.startSynchronization();

    WikiImpl wiki = fetchWikiImpl(wikiType, wikiOwner);
    List<String> permissions = new ArrayList<>();
    HashMap<String, String[]> permMap = new HashMap<>();
    for (PermissionEntry entry : permissionEntries) {
      StringBuilder actions = new StringBuilder();
      Permission[] pers = entry.getPermissions();
      List<String> permlist = new ArrayList<>();
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
    mowService.persist();

    mowService.stopSynchronization(created);
  }

  @Override
  public List<Page> getRelatedPagesOfPage(Page page) throws WikiException {
    List<Page> relatedPages = new ArrayList<>();

    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    try {
      List<PageImpl> relatedPageImpls = pageImpl.getRelatedPages();
      for (PageImpl relatedPageImpl : relatedPageImpls) {
        relatedPages.add(convertPageImplToPage(relatedPageImpl));
      }
    } catch(RepositoryException e) {
      throw new WikiException("Cannot get related pages of page " + page.getWikiType()
              + ":" + page.getWikiOwner() + ":" + page.getName() + " - Cause : " + e.getMessage(), e);
    }

    mowService.stopSynchronization(created);

    return relatedPages;
  }

  @Override
  public Page getRelatedPage(String wikiType, String wikiOwner, String pageId) throws WikiException {
    boolean created = mowService.startSynchronization();

    WikiImpl wiki = fetchWikiImpl(wikiType, wikiOwner);
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
    Page pageWithLinkEntry = getPageWithLinkEntry(newLinkEntry);

    mowService.stopSynchronization(created);

    return pageWithLinkEntry;
  }

  @Override
  public void addRelatedPage(Page page, Page relatedPage) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    PageImpl relatedPageImpl = fetchPageImpl(relatedPage.getWikiType(), relatedPage.getWikiOwner(), relatedPage.getName());

    try {
      pageImpl.addRelatedPage(relatedPageImpl);
    } catch(RepositoryException e) {
      throw new WikiException("Cannot add related page "
              + relatedPage.getWikiType() + ":" + relatedPage.getWikiOwner() + ":" + relatedPage.getName()
              + " for page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName()
              + " - Cause : " + e.getMessage(), e);
    } finally {
      mowService.stopSynchronization(created);
    }
  }

  @Override
  public void removeRelatedPage(Page page, Page relatedPage) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    PageImpl relatedPageImpl = fetchPageImpl(relatedPage.getWikiType(), relatedPage.getWikiOwner(), relatedPage.getName());

    try {
      pageImpl.removeRelatedPage(relatedPageImpl);
    } catch(RepositoryException e) {
      throw new WikiException("Cannot remove related page "
              + relatedPage.getWikiType() + ":" + relatedPage.getWikiOwner() + ":" + relatedPage.getName()
              + " of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName()
              + " - Cause : " + e.getMessage(), e);
    } finally {
      mowService.stopSynchronization(created);
    }
  }

  @Override
  public Page getExsitedOrNewDraftPageById(String wikiType, String wikiOwner, String pageId, String username) throws WikiException {
    boolean created = mowService.startSynchronization();

    // if this is ANONIM then use draft in DraftNewPagesContainer
    if (IdentityConstants.ANONIM.equals(username)) {
      WikiStore wStore = mowService.getWikiStore();
      PageImpl draftNewPagesContainer = wStore.getDraftNewPagesContainer();
      PageImpl draftPage = draftNewPagesContainer.getChildPages().get(pageId);
      if (draftPage == null) {
        draftPage = wStore.createPage();
        draftPage.setName(pageId);
        draftNewPagesContainer.addPublicPage(draftPage);
      }
      return convertPageImplToPage(draftPage);
    }

    // check to get draft if exist
    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();

    UserWiki userWiki;

    // Check if in the case that access to wiki page by rest service of xwiki
    if ((username == null) && (pageId.contains(Utils.SPLIT_TEXT_OF_DRAFT_FOR_NEW_PAGE))) {
      String[] texts = pageId.split(Utils.SPLIT_TEXT_OF_DRAFT_FOR_NEW_PAGE);
      username = texts[0];
      WikiContainer<UserWiki> userWikiContainer = wStore.getWikiContainer(WikiType.USER);
      userWiki = userWikiContainer.getWiki(username);
      Collection<PageImpl> childPages = userWiki.getDraftPagesContainer().getChildrenByRootPermission().values();

      // Change collection to List
      for (PageImpl pageImpl : childPages) {
        if (pageImpl.getName().equals(pageId)) {
          return convertPageImplToPage(pageImpl);
        }
      }
    } else {
      userWiki = (UserWiki) fetchWikiImpl(PortalConfig.USER_TYPE, username);
      if(userWiki == null) {
        userWiki = (UserWiki) wStore.addWiki(WikiType.USER, username);
      }
      // Get draft page
      DraftPage draftPage = getDraft(pageId, username);
      if (draftPage != null) {
        return draftPage;
      }
    }

    // Get draft page container
    PageImpl draftPagesContainer = userWiki.getDraftPagesContainer();

    // Create new draft
    DraftPageImpl draftPageImpl = userWiki.createDraftPage();
    draftPageImpl.setName(pageId);
    draftPagesContainer.addWikiPage(draftPageImpl);
    draftPageImpl.setNewPage(true);
    draftPageImpl.setTargetPage(null);
    draftPageImpl.setTargetRevision("1");

    // Put any permisison to access by xwiki rest service
    HashMap<String, String[]> permissions = draftPageImpl.getPermission();
    permissions.put(IdentityConstants.ANY, new String[]{org.exoplatform.services.jcr.access.PermissionType.READ});
    draftPageImpl.setPermission(permissions);
    Page draftPage = convertPageImplToPage(draftPageImpl);

    mowService.stopSynchronization(created);

    return draftPage;
  }

  @Override
  public DraftPage getDraft(WikiPageParams param, String username) throws WikiException {
    if (IdentityConstants.ANONIM.equals(username)) {
      return null;
    }

    if ((param.getPageName() == null) || (param.getOwner() == null) || (param.getType() == null)) {
      return null;
    }

    PageImpl targetPage = fetchPageImpl(param.getType(), param.getOwner(), param.getPageName());
    if ((param.getPageName() == null) || (targetPage == null)) {
      return null;
    }

    boolean created = mowService.startSynchronization();

    // Get all draft pages
    UserWiki userWiki = (UserWiki) fetchWikiImpl(PortalConfig.USER_TYPE, username);
    Collection<PageImpl> childPages = userWiki.getDraftPagesContainer().getChildPages().values();

    // Find the lastest draft of target page
    DraftPageImpl lastestDraft = null;
    for (PageImpl draft : childPages) {
      DraftPageImpl draftPage = (DraftPageImpl) draft;
      // If this draft is use for target page
      try {
        if (draftPage.getTargetPage() != null && !draftPage.isNewPage() && draftPage.getTargetPage().equals(targetPage.getJCRPageNode().getUUID())) {
          // Compare and get the lastest draft
          if ((lastestDraft == null) || (lastestDraft.getUpdatedDate().getTime() < draftPage.getUpdatedDate().getTime())) {
            lastestDraft = draftPage;
          }
        }
      } catch(RepositoryException e) {
        log.error("Cannot get JCR node of page " + param.getType() + ":" + param.getOwner() + ":" + param.getPageName()
          + " for user " + username + " - Cause : " + e.getMessage(), e);
      }
    }

    DraftPage draftPage = convertDraftPageImplToDraftPage(lastestDraft);

    mowService.stopSynchronization(created);

    return draftPage;
  }


  @Override
  public DraftPage getLastestDraft(String username) throws WikiException {
    boolean created = mowService.startSynchronization();

    // Get all draft pages
    UserWiki userWiki = (UserWiki) fetchWikiImpl(PortalConfig.USER_TYPE, username);
    Collection<PageImpl> childPages = userWiki.getDraftPagesContainer().getChildPages().values();

    // Find the lastest draft
    DraftPageImpl lastestDraft = null;
    for (PageImpl draft : childPages) {
      DraftPageImpl draftPage = (DraftPageImpl) draft;
      // Compare and get the lastest draft
      if ((lastestDraft == null) || (lastestDraft.getUpdatedDate().getTime() < draftPage.getUpdatedDate().getTime())) {
        lastestDraft = draftPage;
      }
    }
    DraftPage draftPage = convertDraftPageImplToDraftPage(lastestDraft);

    mowService.stopSynchronization(created);

    return draftPage;
  }

  @Override
  public DraftPage getDraft(String draftName, String username) throws WikiException {
    boolean created = mowService.startSynchronization();

    DraftPage page = null;
    List<DraftPage> drafts = getDraftPagesOfUser(username);
    for (DraftPage draftPage : drafts) {
      if (draftPage.getName().equals(draftName)) {
        page = draftPage;
        break;
      }
    }

    mowService.stopSynchronization(created);

    return page;
  }

  @Override
  public List<DraftPage> getDraftPagesOfUser(String username) throws WikiException {
    List<DraftPage> draftPages = new ArrayList<>();

    boolean created = mowService.startSynchronization();

    // Get all draft of user
    UserWiki userWiki = (UserWiki) mowService.getWikiStore().getWiki(WikiType.USER, username);
    if(userWiki != null) {
      Collection<PageImpl> childPages = userWiki.getDraftPagesContainer().getChildPages().values();

      // Change collection to List
      for (PageImpl page : childPages) {
        DraftPage draftPage = convertDraftPageImplToDraftPage((DraftPageImpl) page);
        draftPage.setWikiType(userWiki.getType());
        draftPage.setWikiOwner(userWiki.getOwner());
        draftPages.add(draftPage);
      }
    }

    mowService.stopSynchronization(created);

    return draftPages;
  }

  @Override
  public void createDraftPageForUser(DraftPage draftPage, String username) throws WikiException {
    boolean created = mowService.startSynchronization();

    WikiStore wikiStore = mowService.getWikiStore();

    UserWiki userWiki = (UserWiki) wikiStore.getWiki(WikiType.USER, username);
    if(userWiki == null) {
      userWiki = (UserWiki) wikiStore.addWiki(WikiType.USER, username);
    }
    PageImpl draftPagesContainer = userWiki.getDraftPagesContainer();

    // Create draft page
    DraftPageImpl draftPageImpl = userWiki.createDraftPage();
    draftPageImpl.setName(draftPage.getName());
    draftPagesContainer.addWikiPage(draftPageImpl);
    draftPageImpl.setNewPage(draftPage.isNewPage());
    draftPageImpl.setTitle(draftPage.getTitle());
    draftPageImpl.getContent().setText(draftPage.getContent());
    draftPageImpl.setTargetPage(draftPage.getTargetPageId());
    draftPageImpl.setTargetRevision(draftPage.getTargetPageRevision());
    draftPageImpl.setCreatedDate(draftPage.getCreatedDate());
    draftPageImpl.setUpdatedDate(draftPage.getUpdatedDate());

    mowService.persist();

    mowService.stopSynchronization(created);
  }

  @Override
  public PageList<SearchResult> search(WikiSearchData data) throws WikiException {
    List<SearchResult> resultList = new ArrayList<>();
    long numberOfSearchForTitleResult = 0;

    boolean created = mowService.startSynchronization();

    ChromatticSession session = mowService.getSession();

    try {
      if (!StringUtils.isEmpty(data.getTitle())) {
        // Search for title
        String statement = new JCRWikiSearchQueryBuilder(data).getStatementForSearchingTitle();
        QueryImpl q = (QueryImpl) ((ChromatticSessionImpl) session).getDomainSession().getSessionWrapper().createQuery(statement);
        if (data.getOffset() > 0) {
          q.setOffset(data.getOffset());
        }
        if (data.getLimit() > 0) {
          q.setLimit(data.getLimit());
        }
        QueryResult result = q.execute();
        RowIterator iter = result.getRows();
        numberOfSearchForTitleResult = iter.getSize();
        if (numberOfSearchForTitleResult > 0) {
          while (iter.hasNext()) {
            SearchResult tempResult = getResult(iter.nextRow(), data);
            // If contains, merges with the exist
            if (tempResult != null && !isContains(resultList, tempResult)) {
              resultList.add(tempResult);
            }
          }
        }
      }

      // if we have enough result then return
      if ((resultList.size() >= data.getLimit()) || StringUtils.isEmpty(data.getContent())) {
        return new ObjectPageList<>(resultList, resultList.size());
      }
      // Search for wiki content
      long searchForContentOffset = data.getOffset();
      long searchForContentLimit = data.getLimit() - numberOfSearchForTitleResult;
      if (data.getLimit() == Integer.MAX_VALUE) {
        searchForContentLimit = Integer.MAX_VALUE;
      }

      if (searchForContentOffset >= 0 && searchForContentLimit > 0) {
        JCRWikiSearchQueryBuilder queryBuilder = new JCRWikiSearchQueryBuilder(data);
        String statement = queryBuilder.getStatementForSearchingContent();
        QueryImpl q = (QueryImpl) ((ChromatticSessionImpl) session).getDomainSession().getSessionWrapper().createQuery(statement);
        q.setOffset(searchForContentOffset);
        q.setLimit(searchForContentLimit);
        QueryResult result = q.execute();
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
          SearchResult tempResult = getResult(iter.nextRow(), data);
          // If contains, merges with the exist
          if (tempResult != null && !isContains(resultList, tempResult) && !isDuplicateTitle(resultList, tempResult)) {
            resultList.add(tempResult);
          }
        }
      }
    } catch (RepositoryException e) {
      throw new WikiException("Cannot search in wiki " + data.getWikiType() + ":" + data.getWikiOwner(), e);
    }

    mowService.stopSynchronization(created);

    // Return all the result
    return new ObjectPageList<>(resultList, resultList.size());
  }

  @Override
  public List<SearchResult> searchRenamedPage(WikiSearchData data) throws WikiException {
    boolean created = mowService.startSynchronization();

    ChromatticSession session = mowService.getSession();

    List<SearchResult> resultList = new ArrayList<>();
    JCRWikiSearchQueryBuilder queryBuilder = new JCRWikiSearchQueryBuilder(data);
    String statement = queryBuilder.getStatementForRenamedPage();
    try {
      Query q = ((ChromatticSessionImpl)session).getDomainSession().getSessionWrapper().createQuery(statement);
      QueryResult result = q.execute();
      NodeIterator iter = result.getNodes() ;
      while(iter.hasNext()) {
        try {
          resultList.add(getResult(iter.nextNode()));
        } catch (RepositoryException | IOException e) {
          log.debug("Failed to add item search result", e);
        }
      }
    } catch (RepositoryException e) {
      throw new WikiException("Cannot search in wiki " + data.getWikiType() + ":" + data.getWikiOwner(), e);
    }

    mowService.stopSynchronization(created);

    return resultList ;
  }

  @Override
  public List<Attachment> getAttachmentsOfPage(Page page) throws WikiException {
    List<Attachment> attachments = new ArrayList<>();

    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    if(pageImpl != null) {
      try {
        Collection<AttachmentImpl> attachmentsExcludeContent = pageImpl.getAttachmentsExcludeContent();
        if(attachmentsExcludeContent != null) {
          for (AttachmentImpl attachmentImpl : attachmentsExcludeContent) {
            attachments.add(convertAttachmentImplToAttachment(attachmentImpl));
          }
        }
      } catch (RepositoryException e) {
        throw new WikiException("Cannot get attachments of page "
                + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName(), e);
      }
    } else {
      throw new WikiException("Cannot get attachments of page "
              + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName() + " because the page does not exist.");
    }

    mowService.stopSynchronization(created);

    return attachments;
  }

  @Override
  public void addAttachmentToPage(Attachment attachment, Page page) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    AttachmentImpl attachmentImpl = pageImpl.createAttachment(attachment.getName(), new Resource(attachment.getMimeType(), "UTF-8", attachment.getContent()));
    attachmentImpl.setTitle(attachment.getTitle());
    attachmentImpl.setCreator(attachment.getCreator());

    mowService.stopSynchronization(created);
  }

  @Override
  public void deleteAttachmentOfPage(String attachmentId, Page page) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    pageImpl.removeAttachment(attachmentId);

    mowService.stopSynchronization(created);
  }

  @Override
  public Page getHelpSyntaxPage(String syntaxId, List<ValuesParam> syntaxHelpParams, ConfigurationManager configurationManager) throws WikiException {
    boolean created = mowService.startSynchronization();

    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    HelpPage helpPageByChromattic = wStore.getHelpPageByChromattic();

    if(helpPageByChromattic == null || wStore.getHelpPagesContainer().getChildPages().size() == 0) {
      createHelpPages(syntaxHelpParams, configurationManager);
    }

    Page page = null;
    Iterator<PageImpl> syntaxPageIterator = wStore.getHelpPagesContainer()
            .getChildPages()
            .values()
            .iterator();
    while (syntaxPageIterator.hasNext()) {
      PageImpl syntaxPage = syntaxPageIterator.next();
      if (syntaxPage.getSyntax().equals(syntaxId)) {
        page = convertPageImplToPage(syntaxPage);
        break;
      }
    }

    mowService.stopSynchronization(created);

    return page;
  }

  @Override
  public void createEmotionIcon(EmotionIcon emotionIcon) throws WikiException {
    boolean created = mowService.startSynchronization();

    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    PageImpl emotionIconsPage = wStore.getEmotionIconsContainer();

    String mimetype;
    try {
      mimetype = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(emotionIcon.getImage()));
    } catch (IOException e) {
      log.warn("Cannot guess mimetype from inputstream for emotion icon " + emotionIcon.getName() + " - Cause : " + e.getMessage());
      mimetype = URLConnection.guessContentTypeFromName(emotionIcon.getName());
      if(mimetype == null) {
        mimetype = "image/*";
      }
    }

    AttachmentImpl emotionIconAttachment = emotionIconsPage.createAttachment(emotionIcon.getName(),
            new Resource(mimetype, "UTF-8", emotionIcon.getImage()));
    emotionIconsPage.addAttachment(emotionIconAttachment);

    mowService.stopSynchronization(created);
  }

  @Override
  public List<EmotionIcon> getEmotionIcons() throws WikiException {
    List<EmotionIcon> emotionIcons = null;

    boolean created = mowService.startSynchronization();

    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();

    PageImpl emotionIconsPage = wStore.getEmotionIconsContainer();
    if(emotionIconsPage != null) {
      emotionIcons = new ArrayList<>();

      String baseUrl = JCRUtils.getCurrentRepositoryWebDavUri();

      Collection<AttachmentImpl> emotionIconsAttachments = emotionIconsPage.getAttachments();
      for(AttachmentImpl emotionIconAttachment : emotionIconsAttachments) {
        EmotionIcon emotionIcon = new EmotionIcon();
        emotionIcon.setName(emotionIconAttachment.getName());
        StringBuilder sbUrl = new StringBuilder(baseUrl)
                .append(mowService.getSession().getJCRSession().getWorkspace().getName())
                .append(emotionIconsPage.getPath())
                .append("/")
                .append(emotionIconAttachment.getName());
        emotionIcon.setUrl(sbUrl.toString());
        emotionIcons.add(emotionIcon);
      }
    }

    mowService.stopSynchronization(created);

    return emotionIcons;
  }

  @Override
  public EmotionIcon getEmotionIconByName(String name) throws WikiException {
    EmotionIcon emotionIcon = null;

    boolean created = mowService.startSynchronization();

    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();

    String baseUrl = JCRUtils.getCurrentRepositoryWebDavUri();

    PageImpl emotionIconsPage = wStore.getEmotionIconsContainer();
    if(emotionIconsPage != null) {
      AttachmentImpl emotionIconAttachment = emotionIconsPage.getAttachment(name);
      if(emotionIconAttachment != null) {
        emotionIcon = new EmotionIcon();
        emotionIcon.setName(name);
        StringBuilder sbUrl = new StringBuilder(baseUrl)
                .append(mowService.getSession().getJCRSession().getWorkspace().getName())
                .append(emotionIconsPage.getPath())
                .append("/")
                .append(emotionIconAttachment.getName());
        emotionIcon.setUrl(sbUrl.toString());
      }
    }

    mowService.stopSynchronization(created);

    return emotionIcon;
  }

  private synchronized void createHelpPages(List<ValuesParam> syntaxHelpParams, ConfigurationManager configurationManager) throws WikiException {
    boolean created = mowService.startSynchronization();

    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    PageImpl helpPage = wStore.getHelpPagesContainer();
    if (helpPage.getChildPages().size() == 0) {
      for (ValuesParam syntaxhelpParam : syntaxHelpParams) {
        try {
          String syntaxName = syntaxhelpParam.getName();
          List<String> syntaxValues = syntaxhelpParam.getValues();
          String shortFilePath = syntaxValues.get(0);
          String fullFilePath = syntaxValues.get(1);
          InputStream shortFile = configurationManager.getInputStream(shortFilePath);
          InputStream fullFile = configurationManager.getInputStream(fullFilePath);
          HelpPage syntaxPage = addSyntaxPage(wStore, helpPage, syntaxName, shortFile, " Short help Page");
          addSyntaxPage(wStore, syntaxPage, syntaxName, fullFile, " Full help Page");
          mowService.persist();
        } catch (Exception e) {
          log.error("Can not create Help page " + syntaxhelpParam.getName() + " - Cause : " + e.getMessage(), e);
        }
      }
    }

    mowService.stopSynchronization(created);
  }

  @Override
  public boolean hasAdminSpacePermission(String wikiType, String owner, Identity user) throws WikiException {
    boolean created = mowService.startSynchronization();

    List<AccessControlEntry> aces = getAccessControls(wikiType, owner);
    AccessControlList acl = new AccessControlList(owner, aces);
    String[] permission = new String[]{PermissionType.ADMINSPACE.toString()};
    boolean hasPermission = JCRUtils.hasPermission(acl, permission, user);

    mowService.stopSynchronization(created);

    return hasPermission;
  }

  @Override
  public boolean hasAdminPagePermission(String wikiType, String owner, Identity user) throws WikiException {
    boolean created = mowService.startSynchronization();

    List<AccessControlEntry> aces = getAccessControls(wikiType, owner);
    AccessControlList acl = new AccessControlList(owner, aces);
    String[] permission = new String[]{PermissionType.ADMINPAGE.toString()};
    boolean hasPermission = JCRUtils.hasPermission(acl, permission, user);

    mowService.stopSynchronization(created);

    return hasPermission;
  }

  @Override
  public boolean hasPermissionOnPage(Page page, PermissionType permissionType, Identity user) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    boolean hasPermission = pageImpl.hasPermission(permissionType, user);

    mowService.stopSynchronization(created);

    return hasPermission;
  }

  private List<AccessControlEntry> getAccessControls(String wikiType, String wikiOwner) throws WikiException {
    boolean created = mowService.startSynchronization();

    List<AccessControlEntry> aces = new ArrayList<>();
    try {
      List<PermissionEntry> permissionEntries = getWikiPermission(wikiType, wikiOwner);
      for (PermissionEntry perm : permissionEntries) {
        Permission[] permissions = perm.getPermissions();
        List<String> actions = new ArrayList<>();
        for (Permission permission : permissions) {
          if (permission.isAllowed()) {
            actions.add(permission.getPermissionType().toString());
          }
        }

        for (String action : actions) {
          aces.add(new AccessControlEntry(perm.getId(), action));
        }
      }
    } catch (WikiException e) {
      if (log.isDebugEnabled()) {
        log.debug("failed in method getAccessControls:", e);
      }
    }

    mowService.stopSynchronization(created);

    return aces;
  }

  @Override
  public List<PageVersion> getVersionsOfPage(Page page) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());

    List<PageVersion> versions = new ArrayList<>();
    VersionableMixin versionableMixin = pageImpl.getVersionableMixin();
    if(versionableMixin != null) {
      for (NTVersion version : versionableMixin.getVersionHistory()) {
        if (!(WikiNodeType.Definition.ROOT_VERSION.equals(version.getName()))) {
          try {
            PageVersion pageVersion = new PageVersion();
            pageVersion.setName(version.getName());
            pageVersion.setAuthor(version.getNTFrozenNode().getAuthor());
            pageVersion.setCreatedDate(version.getCreated());
            pageVersion.setUpdatedDate(version.getNTFrozenNode().getUpdatedDate());
            //pageVersion.setPredecessors(version.getPredecessors());
            //pageVersion.setSuccessors(version.getSuccessors());
            pageVersion.setContent(version.getNTFrozenNode().getContentString());
            pageVersion.setComment(version.getNTFrozenNode().getComment());
            versions.add(pageVersion);
          } catch(RepositoryException e) {
            log.error("Cannot get version " + version.getName() + " of page "
                    + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName()
                    + " - Cause : " + e.getMessage(), e);
          }
        }
      }
    }
    Collections.sort(versions, new VersionNameComparatorDesc());

    mowService.stopSynchronization(created);

    return versions;
  }

  @Override
  public void addPageVersion(Page page) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    if(pageImpl.getVersionableMixin() == null) {
      pageImpl.makeVersionable();
    }
    try {
      pageImpl.checkin();
      pageImpl.checkout();
    } catch(RepositoryException e) {
      throw new WikiException("Cannot create new version of page "
              + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName(), e);
    }

    mowService.stopSynchronization(created);
  }

  @Override
  public void restoreVersionOfPage(String versionName, Page page) throws WikiException {
    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    pageImpl.restore(versionName, false);
  }

  @Override
  public void updatePage(Page page) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    pageImpl.setTitle(page.getTitle());
    pageImpl.setOwner(page.getOwner());
    pageImpl.setAuthor(page.getAuthor());
    pageImpl.setSyntax(page.getSyntax());

    List<PermissionEntry> currentPermissions = JCRUtils.convertToPermissionEntryList(pageImpl.getPermission());
    if(!CollectionUtils.isEqualCollection(currentPermissions, page.getPermissions())) {
      pageImpl.setPermission(JCRUtils.convertToPermissionMap(page.getPermissions()));
      pageImpl.setOverridePermission(true);
    }
    pageImpl.setURL(page.getUrl());
    pageImpl.getContent().setText(page.getContent());
    pageImpl.setComment(page.getComment());
    pageImpl.setUpdatedDate(GregorianCalendar.getInstance().getTime());

    if(page.getActivityId() != null) {
      pageImpl.setActivityId(page.getActivityId());
    }

    mowService.persist();

    mowService.stopSynchronization(created);
  }

  @Override
  public List<String> getPreviousNamesOfPage(Page page) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    if(pageImpl != null) {
      List<String> previousNames = new ArrayList<>();
      RenamedMixin renamedMixin = pageImpl.getRenamedMixin();
      if(renamedMixin != null) {
        previousNames = Arrays.asList(renamedMixin.getOldPageIds());
      }

      mowService.stopSynchronization(created);

      return previousNames;
    } else {
      mowService.stopSynchronization(created);

      throw new WikiException("Cannot get previous names of page " + page.getWikiType() + ":"
              + page.getWikiOwner() + ":" + page.getName() + " because the page does not exist.");
    }
  }

  @Override
  public List<String> getWatchersOfPage(Page page) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    if(pageImpl != null) {
      pageImpl.makeWatched();
      List<String> watchers = pageImpl.getWatchedMixin().getWatchers();

      mowService.stopSynchronization(created);

      return watchers;
    } else {
      mowService.stopSynchronization(created);

      throw new WikiException("Cannot get watchers of page " + page.getWikiType() + ":"
              + page.getWikiOwner() + ":" + page.getName() + " because the page does not exist.");
    }
  }

  @Override
  public void addWatcherToPage(String username, Page page) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    if(pageImpl != null) {
      pageImpl.makeWatched();
      List<String> watchers = pageImpl.getWatchedMixin().getWatchers();
      if (watchers == null) {
        watchers = new ArrayList<>();
      }
      if (!watchers.contains(username)) {
        watchers.add(username);
        pageImpl.getWatchedMixin().setWatchers(watchers);
      }

      mowService.persist();

      mowService.stopSynchronization(created);
    } else {
      mowService.stopSynchronization(created);

      throw new WikiException("Cannot add watcher " + username + " to page " + page.getWikiType() + ":"
              + page.getWikiOwner() + ":" + page.getName() + " because the page does not exist.");
    }
  }

  @Override
  public void deleteWatcherOfPage(String username, Page page) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl pageImpl = fetchPageImpl(page.getWikiType(), page.getWikiOwner(), page.getName());
    if(pageImpl != null) {
      pageImpl.makeWatched();
      List<String> watchers = pageImpl.getWatchedMixin().getWatchers();
      if (watchers != null && watchers.contains(username)) {
        watchers.remove(username);
        pageImpl.getWatchedMixin().setWatchers(watchers);
      }

      mowService.persist();

      mowService.stopSynchronization(created);
    } else {
      mowService.stopSynchronization(created);

      throw new WikiException("Cannot delete watcher " + username + " of page " + page.getWikiType() + ":"
              + page.getWikiOwner() + ":" + page.getName() + " because the page does not exist.");
    }
  }

  private HelpPage addSyntaxPage(WikiStoreImpl wStore,
                                 PageImpl parentPage,
                                 String name,
                                 InputStream content,
                                 String type) throws WikiException {
    boolean created = mowService.startSynchronization();

    StringBuilder stringContent = new StringBuilder();
    BufferedReader bufferReader = null;
    String tempLine;
    try {
      bufferReader = new BufferedReader(new InputStreamReader(content));
      while ((tempLine = bufferReader.readLine()) != null) {
        stringContent.append(tempLine).append("\n");
      }

      HelpPage syntaxPage = wStore.createHelpPage();
      String realName = name.replace("/", "");
      syntaxPage.setName(realName + type);
      parentPage.addPublicPage(syntaxPage);
      AttachmentImpl pageContent = syntaxPage.getContent();
      syntaxPage.setTitle(realName + type);
      pageContent.setText(stringContent.toString());
      syntaxPage.setSyntax(name);
      syntaxPage.setNonePermission();
      return syntaxPage;
    } catch (IOException e) {
      throw new WikiException("Cannot create help page " + type, e);
    } finally {
      if (content != null) {
        try {
          content.close();
        } catch (IOException e) {
          log.error("Cannot close input stream of help page " + type + " - Cause : " + e.getMessage(), e);
        }
      }
      if (bufferReader != null) {
        try {
          bufferReader.close();
        } catch (IOException e) {
          log.error("Cannot close buffer reader of help page " + type + " - Cause : " + e.getMessage(), e);
        }
      }
      mowService.stopSynchronization(created);
    }

  }

  private boolean isDuplicateTitle(List<SearchResult> list, SearchResult result) {
    for (SearchResult searchResult : list) {
      if (result.getTitle().equals(searchResult.getTitle())) {
        return true;
      }
    } 
    return false;
  }

  private void updateAllPagesPermissions(String wikiType, String wikiOwner, HashMap<String, String[]> permMap) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl page = fetchWikiImpl(wikiType, wikiOwner).getWikiHome();
    Queue<PageImpl> queue = new LinkedList<>();
    queue.add(page);
    while (queue.peek() != null) {
      PageImpl p = queue.poll();
      if (!p.getOverridePermission()) {
        p.setPermission(permMap);
        p.setUpdateAttachmentMixin(null);
      }
      Iterator<PageImpl> iter = p.getChildPages().values().iterator();
      while (iter.hasNext()) {
        queue.add(iter.next());
      }
    }

    mowService.stopSynchronization(created);
  }

  private Page getPageWithLinkEntry(LinkEntry entry) throws WikiException {
    boolean created = mowService.startSynchronization();

    String linkEntryAlias = entry.getAlias();
    String[] splits = linkEntryAlias.split("@");
    String wikiType = splits[0];
    String wikiOwner = splits[1];
    String pageId = linkEntryAlias.substring((wikiType + "@" + wikiOwner + "@").length());
    Page page = getPageOfWikiByName(wikiType, wikiOwner, pageId);

    mowService.stopSynchronization(created);

    return page;
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

  private Object findByPath(String path, String objectNodeType) throws WikiException {
    String relPath = path;
    if (relPath.startsWith("/")) {
      relPath = relPath.substring(1);
    }

    boolean created = mowService.startSynchronization();

    Object object = null;
    if (WikiNodeType.WIKI_PAGE.equals(objectNodeType)) {
      object = mowService.getSession().findByPath(PageImpl.class, relPath);
    } else if (WikiNodeType.WIKI_ATTACHMENT.equals(objectNodeType)) {
      object = mowService.getSession().findByPath(AttachmentImpl.class, relPath);
    } else if (WikiNodeType.WIKI_TEMPLATE.equals(objectNodeType)) {
      object = mowService.getSession().findByPath(Template.class, relPath);
    }

    mowService.stopSynchronization(created);

    return object;
  }
  
  private SearchResult getResult(Row row, WikiSearchData data) throws WikiException {
    boolean created = mowService.startSynchronization();

    try {
      String type = row.getValue(WikiNodeType.Definition.PRIMARY_TYPE).getString();
      String path = row.getValue(WikiNodeType.Definition.PATH).getString();

      SearchResult result = new SearchResult();

      long score = row.getValue("jcr:score").getLong();
      Calendar createdDate = GregorianCalendar.getInstance();
      Calendar updatedDate = GregorianCalendar.getInstance();
      PageImpl page;
      if (WikiNodeType.WIKI_ATTACHMENT.equals(type) || WikiNodeType.WIKI_ATTACHMENT_CONTENT.equals(type)) {
        // Transform to Attachment result
        result.setType(SearchResultType.ATTACHMENT);
        if (!path.endsWith(WikiNodeType.Definition.CONTENT)) {
          result.setType(SearchResultType.PAGE_CONTENT);
          AttachmentImpl searchAtt = (AttachmentImpl) findByPath(path, WikiNodeType.WIKI_ATTACHMENT);
          updatedDate = searchAtt.getUpdatedDate();
          page = searchAtt.getParentPage();
          createdDate.setTime(page.getCreatedDate());
          result.setAttachmentName(searchAtt.getName());
        } else {
          result.setType(SearchResultType.ATTACHMENT);
          String pagePath = path.substring(0, path.lastIndexOf("/" + WikiNodeType.Definition.CONTENT));
          result.setType(SearchResultType.PAGE_CONTENT);
          page = (PageImpl) findByPath(pagePath, WikiNodeType.WIKI_PAGE);
          updatedDate.setTime(page.getUpdatedDate());
          createdDate.setTime(page.getCreatedDate());
        }
      } else if (WikiNodeType.WIKI_PAGE.equals(type)) {
        result.setType(SearchResultType.PAGE);
        page = (PageImpl) findByPath(path, type);
        updatedDate.setTime(page.getUpdatedDate());
        createdDate.setTime(page.getCreatedDate());
      } else {
        return null;
      }

      if (page == null || !page.hasPermission(PermissionType.VIEWPAGE)) {
        return null;
      }

      result.setWikiType(page.getWiki().getType());
      result.setWikiOwner(page.getWiki().getOwner());
      result.setPageName(page.getName());
      result.setTitle(page.getTitle());
      result.setPath(path);
      result.setCreatedDate(createdDate);
      result.setUpdatedDate(updatedDate);
      result.setUrl(page.getURL());
      result.setScore(score);

      //get the excerpt from row result
      result.setExcerpt(getExcerpt(row, type));

      return result;
    } catch(RepositoryException e) {
      throw new WikiException("Cannot get search result", e);
    } finally {
      mowService.stopSynchronization(created);
    }
  }

  private void processCircularRename(LinkEntry entry, LinkEntry newEntry) {
    boolean created = mowService.startSynchronization();

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

    mowService.stopSynchronization(created);
  }

  /**
   * gets except of row result based on specific properties, but all to get nice excerpt
   * @param row the result row
   * @param type the result type
   * @return the excerpt
   * @throws ItemNotFoundException
   * @throws RepositoryException
   */
  private String getExcerpt(Row row, String type) throws RepositoryException {
    boolean created = mowService.startSynchronization();

    StringBuilder ret = new StringBuilder();
    String[] properties = (WikiNodeType.WIKI_PAGE_CONTENT.equals(type) || WikiNodeType.WIKI_ATTACHMENT.equals(type)) ? 
                          new String[]{"."} :
                          new String[]{"title", "url"};
    for (String prop : properties) {
      Value excerptValue = row.getValue("rep:excerpt(" + prop + ")");
      if (excerptValue != null) {
        ret.append(excerptValue.getString()).append("...");
      }
    }
    if (ret.length() > MAX_EXCERPT_LENGTH) {
      return ret.substring(0, MAX_EXCERPT_LENGTH) + "...";
    }

    mowService.stopSynchronization(created);

    return ret.toString();
  }
  
  private SearchResult getResult(Node node)throws RepositoryException, IOException {
    boolean created = mowService.startSynchronization();

    SearchResult result = new SearchResult() ;
    result.setPageName(node.getName()) ;
    String title = node.getProperty(WikiNodeType.Definition.TITLE).getString();
    InputStream data = node.getNode(WikiNodeType.Definition.CONTENT).getNode(WikiNodeType.Definition.ATTACHMENT_CONTENT).getProperty(WikiNodeType.Definition.DATA).getStream();
    byte[] bytes = IO.getBytes(data);
    String content = new String(bytes, "UTF-8");
    if(content.length() > 100) content = content.substring(0, 100) + "...";
    result.setExcerpt(content) ;
    result.setTitle(title) ;

    mowService.stopSynchronization(created);

    return result ;
  }
  
  private boolean isContains(List<SearchResult> list, SearchResult result) throws WikiException {
    boolean created = mowService.startSynchronization();

    boolean contains = false;
    AttachmentImpl att = null;
    PageImpl page = null;
    if (WikiNodeType.WIKI_ATTACHMENT.equals(result.getType())) {
      att = (AttachmentImpl) findByPath(result.getPath(), WikiNodeType.WIKI_ATTACHMENT);
    } else if (WikiNodeType.WIKI_ATTACHMENT_CONTENT.equals(result.getType())) {
      String attPath = result.getPath().substring(0, result.getPath().lastIndexOf("/"));
      att = (AttachmentImpl) findByPath(attPath, WikiNodeType.WIKI_ATTACHMENT);
    } else if(WikiNodeType.WIKI_PAGE.equals(result.getType()) || WikiNodeType.WIKI_HOME.equals(result.getType())){
      page = (PageImpl) findByPath(result.getPath(), WikiNodeType.WIKI_PAGE);
    } else if (WikiNodeType.WIKI_PAGE_CONTENT.equals(result.getType())) {
      att = (AttachmentImpl) findByPath(result.getPath(), WikiNodeType.WIKI_ATTACHMENT);
      page = att.getParentPage();
    }
    if (att != null || page != null) {
      Iterator<SearchResult> iter = list.iterator();
      while (iter.hasNext()) {
        SearchResult child = iter.next();
        if (WikiNodeType.WIKI_ATTACHMENT.equals(child.getType()) || WikiNodeType.WIKI_PAGE_CONTENT.equals(child.getType())) {
          AttachmentImpl tempAtt = (AttachmentImpl) findByPath(child.getPath(), WikiNodeType.WIKI_ATTACHMENT);
          if (att != null && att.equals(tempAtt)) {
            // Merge data
            if (child.getExcerpt()==null && result.getExcerpt()!=null ){
              child.setExcerpt(result.getExcerpt());
            }
            contains = true;
          }               
          if (page != null && page.getName().equals(tempAtt.getParentPage())) {
            contains = true;
          }     
        } else if (WikiNodeType.WIKI_PAGE.equals(child.getType())) {
          if (page != null && page.getPath().equals(child.getPath())) {
            iter.remove();
            contains = false;
          }
        }
      }
    }

    mowService.stopSynchronization(created);

    return contains;
  }

  @Override
  public List<TemplateSearchResult> searchTemplate(TemplateSearchData data) throws WikiException {
    boolean created = mowService.startSynchronization();

    ChromatticSession session = mowService.getSession();

    List<TemplateSearchResult> resultList = new ArrayList<>();
    String statement = new JCRTemplateSearchQueryBuilder(data).getStatementForSearchingTitle();

    try {
      Query q = ((ChromatticSessionImpl) session).getDomainSession().getSessionWrapper().createQuery(statement);
      QueryResult result = q.execute();
      RowIterator iter = result.getRows();
      while (iter.hasNext()) {
        TemplateSearchResult tempResult = getTemplateResult(iter.nextRow(), data);
        resultList.add(tempResult);
      }
    } catch(RepositoryException e) {
      throw new WikiException("Cannot search templates in wiki " + data.getWikiType() + ":" + data.getWikiOwner(), e);
    }

    mowService.stopSynchronization(created);

    return resultList;
  }

  private TemplateSearchResult getTemplateResult(Row row, TemplateSearchData data) throws WikiException {
    boolean created = mowService.startSynchronization();

    try {
      String path = row.getValue(WikiNodeType.Definition.PATH).getString();
      String title = (row.getValue(WikiNodeType.Definition.TITLE) == null ? null : row.getValue(WikiNodeType.Definition.TITLE).getString());

      TemplateImpl templateImpl = (TemplateImpl) findByPath(path, WikiNodeType.WIKI_PAGE);
      String description = templateImpl.getDescription();
      TemplateSearchResult result = new TemplateSearchResult(data.getWikiType(),
              data.getWikiOwner(),
              templateImpl.getName(),
              title,
              path,
              SearchResultType.TEMPLATE,
              null,
              null,
              description);
      return result;
    } catch(RepositoryException e) {
      throw new WikiException("Cannot get template", e);
    } finally {
      mowService.stopSynchronization(created);
    }
  }

  /**
   * Fetch a WikiImpl object with Chrommatic
   * @param wikiType
   * @param wikiOwner
   * @return
   */
  private WikiImpl fetchWikiImpl(String wikiType, String wikiOwner) throws WikiException {
    boolean created = mowService.startSynchronization();

    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    WikiImpl wiki = null;
    if (PortalConfig.PORTAL_TYPE.equals(wikiType)) {
      WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
      wiki = portalWikiContainer.getWiki(wikiOwner);
    } else if (PortalConfig.GROUP_TYPE.equals(wikiType)) {
      WikiContainer<GroupWiki> groupWikiContainer = wStore.getWikiContainer(WikiType.GROUP);
      wiki = groupWikiContainer.getWiki(wikiOwner);
    } else if (PortalConfig.USER_TYPE.equals(wikiType)) {
      WikiContainer<UserWiki> userWikiContainer = wStore.getWikiContainer(WikiType.USER);
      wiki = userWikiContainer.getWiki(wikiOwner);
    }
    mowService.persist();

    mowService.stopSynchronization(created);

    return wiki;
  }

  /**
   * Fetch a PageImpl object with Chrommatic
   * @return
   */
  private PageImpl fetchPageImpl(String wikiType, String wikiOwner, String pageName) throws WikiException {
    boolean created = mowService.startSynchronization();

    PageImpl wikiPage = null;
    if(pageName.equals(WikiConstants.WIKI_HOME_NAME)) {
      WikiImpl wikiImpl = fetchWikiImpl(wikiType, wikiOwner);
      wikiPage = wikiImpl.getWikiHome();
    } else {
      ChromatticSession session = mowService.getSession();

      WikiSearchData searchData = new WikiSearchData(wikiType, wikiOwner, pageName);
      JCRWikiSearchQueryBuilder queryBuilder = new JCRWikiSearchQueryBuilder(searchData);
      String statement = queryBuilder.getPageConstraint();

      if (statement != null) {
        Iterator<PageImpl> result = session.createQueryBuilder(PageImpl.class)
                .where(statement)
                .get()
                .objects();
        if (result.hasNext()) {
          wikiPage = result.next();
        }
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
    }

    mowService.stopSynchronization(created);

    return wikiPage;
  }

  private Wiki convertWikiImplToWiki(WikiImpl wikiImpl) throws WikiException {
    Wiki wiki = null;
    if(wikiImpl != null) {
      boolean created = mowService.startSynchronization();

      wiki = new Wiki();
      wiki.setId(wikiImpl.getName());
      wiki.setType(wikiImpl.getType());
      wiki.setOwner(wikiImpl.getOwner());
      Page wikiHome = convertPageImplToPage(wikiImpl.getWikiHome());
      wikiHome.setWikiId(wikiImpl.getName());
      wikiHome.setWikiType(wikiImpl.getType());
      wikiHome.setWikiOwner(wikiImpl.getOwner());
      wiki.setWikiHome(wikiHome);
      wiki.setPermissions(JCRUtils.convertWikiPermissionsToPermissionEntryList(wikiImpl.getWikiPermissions()));
      wiki.setDefaultPermissionsInited(wikiImpl.getDefaultPermissionsInited());
      PreferencesImpl preferencesImpl = wikiImpl.getPreferences();
      if (preferencesImpl != null) {
        WikiPreferencesSyntax wikiPreferencesSyntax = new WikiPreferencesSyntax();
        PreferencesSyntax preferencesSyntax = preferencesImpl.getPreferencesSyntax();
        if (preferencesSyntax != null) {
          wikiPreferencesSyntax.setDefaultSyntax(preferencesSyntax.getDefaultSyntax());
          wikiPreferencesSyntax.setAllowMultipleSyntaxes(preferencesSyntax.getAllowMutipleSyntaxes());
        }
        WikiPreferences wikiPreferences = new WikiPreferences();
        wikiPreferences.setPath(preferencesImpl.getPath());
        wikiPreferences.setWikiPreferencesSyntax(wikiPreferencesSyntax);
        wiki.setPreferences(wikiPreferences);
      }

      mowService.stopSynchronization(created);
    }
    return wiki;
  }

  /**
   * Utility method to convert PageImpl object to Page object
   * @param pageImpl PageImpl object to convert
   * @return
   * @throws WikiException
   */
  private Page convertPageImplToPage(PageImpl pageImpl) throws WikiException {
    Page page = null;
    if(pageImpl != null) {
      boolean created = mowService.startSynchronization();

      page = new Page();
      try {
        page.setId(pageImpl.getID());
      } catch(RepositoryException e) {
        throw new WikiException("Cannot get page id", e);
      }
      WikiImpl wiki = pageImpl.getWiki();
      if(wiki != null) {
        page.setWikiId(wiki.getName());
        page.setWikiType(wiki.getWikiType().toString().toLowerCase());
        page.setWikiOwner(wiki.getOwner());
      }
      page.setOwner(pageImpl.getOwner());
      page.setName(pageImpl.getName());
      page.setTitle(pageImpl.getTitle());
      page.setAuthor(pageImpl.getAuthor());
      page.setUrl(pageImpl.getURL());
      page.setCreatedDate(pageImpl.getCreatedDate());
      page.setUpdatedDate(pageImpl.getUpdatedDate());
      page.setPath(pageImpl.getPath());
      page.setComment(pageImpl.getComment());
      page.setContent(pageImpl.getContent().getText());
      page.setSyntax(pageImpl.getSyntax());
      page.setPermissions(JCRUtils.convertToPermissionEntryList(pageImpl.getPermission()));
      page.setActivityId(pageImpl.getActivityId());

      mowService.stopSynchronization(created);
    }
    return page;
  }

  /**
   * Utility method to convert DraftPageImpl object to DraftPage object
   * @param draftPageImpl DraftPageImpl object to convert
   * @return
   * @throws WikiException
   */
  private DraftPage convertDraftPageImplToDraftPage(DraftPageImpl draftPageImpl) throws WikiException {
    DraftPage draftPage = null;
    if(draftPageImpl != null) {
      boolean created = mowService.startSynchronization();

      draftPage = new DraftPage();
      try {
        draftPage.setId(draftPageImpl.getID());
      } catch(RepositoryException e) {
        throw new WikiException("Cannot get draft page id", e);
      }
      draftPage.setOwner(draftPageImpl.getOwner());
      draftPage.setName(draftPageImpl.getName());
      draftPage.setAuthor(draftPageImpl.getAuthor());
      draftPage.setTitle(draftPageImpl.getTitle());
      draftPage.setUrl(draftPageImpl.getURL());
      draftPage.setCreatedDate(draftPageImpl.getCreatedDate());
      draftPage.setUpdatedDate(draftPageImpl.getUpdatedDate());
      draftPage.setPath(draftPageImpl.getPath());
      draftPage.setComment(draftPageImpl.getComment());
      draftPage.setContent(draftPageImpl.getContent().getText());
      draftPage.setSyntax(draftPageImpl.getSyntax());
      draftPage.setPermissions(JCRUtils.convertToPermissionEntryList(draftPageImpl.getPermission()));

      draftPage.setTargetPageId(draftPageImpl.getTargetPage());
      draftPage.setTargetPageRevision(draftPageImpl.getTargetRevision());
      draftPage.setNewPage(draftPageImpl.isNewPage());

      mowService.stopSynchronization(created);
    }
    return draftPage;
  }

  private Attachment convertAttachmentImplToAttachment(AttachmentImpl attachmentImpl) throws WikiException {
    Attachment attachment = null;
    if(attachmentImpl != null) {
      boolean created = mowService.startSynchronization();

      attachment = new Attachment();
      attachment.setName(attachmentImpl.getName());
      attachment.setTitle(attachmentImpl.getTitle());
      attachment.setFullTitle(attachmentImpl.getFullTitle());
      attachment.setCreator(attachmentImpl.getCreator());
      attachment.setCreatedDate(attachmentImpl.getCreatedDate());
      attachment.setUpdatedDate(attachmentImpl.getUpdatedDate());
      attachment.setContent(attachmentImpl.getContentResource().getData());
      attachment.setMimeType(attachmentImpl.getContentResource().getMimeType());
      attachment.setPermissions(JCRUtils.convertToPermissionEntryList(attachmentImpl.getPermission()));
      attachment.setDownloadURL(attachmentImpl.getDownloadURL());
      attachment.setWeightInBytes(attachmentImpl.getWeightInBytes());

      mowService.stopSynchronization(created);
    }
    return attachment;
  }

  private Template convertTemplateImplToTemplate(TemplateImpl templateImpl) throws WikiException {
    Template template = null;
    if(templateImpl != null) {
      boolean created = mowService.startSynchronization();

      template = new Template();
      template.setName(templateImpl.getName());
      template.setTitle(templateImpl.getTitle());
      template.setDescription(templateImpl.getDescription());
      template.setContent(templateImpl.getContent().getText());

      mowService.stopSynchronization(created);
    }
    return template;
  }
}
