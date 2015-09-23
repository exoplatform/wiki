package org.exoplatform.wiki.service.impl;

import org.apache.commons.lang.StringUtils;
import org.chromattic.api.ChromatticSession;
import org.chromattic.common.IO;
import org.chromattic.core.api.ChromatticSessionImpl;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.mow.core.api.wiki.*;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.*;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.utils.Utils;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class JCRDataStorage implements DataStorage {
  private static final Log log = ExoLogger.getLogger(JCRDataStorage.class);

  private static final int MAX_EXCERPT_LENGTH = 430;

  private static final int CIRCULAR_RENAME_FLAG = 1000;

  @Override
  // TODO check who has admin permission ?
  public Wiki getWiki(String wikiType, String owner, boolean hasAdminPermission) {
    WikiStoreImpl wStore = (WikiStoreImpl) getModel().getWikiStore();
    WikiImpl wiki = null;
    try {
      if (PortalConfig.PORTAL_TYPE.equals(wikiType)) {
        WikiContainer<PortalWiki> portalWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
        wiki = portalWikiContainer.getWiki(owner, true);
      } else if (PortalConfig.GROUP_TYPE.equals(wikiType)) {
        WikiContainer<GroupWiki> groupWikiContainer = wStore.getWikiContainer(WikiType.GROUP);
        wiki = groupWikiContainer.getWiki(owner, hasAdminPermission);
      } else if (PortalConfig.USER_TYPE.equals(wikiType)) {
        WikiContainer<UserWiki> userWikiContainer = wStore.getWikiContainer(WikiType.USER);
        wiki = userWikiContainer.getWiki(owner, hasAdminPermission);
      }
      getModel().save();
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("[WikiService] Cannot get wiki " + wikiType + ":" + owner, e);
      }
    }
    return wiki;
  }

  /**
   * Create a wiki page with the given pageId, under the node of the parentPage node
   * @param wiki
   * @param pageId
   * @param parentPage
   * @param title
   * @return
   * @throws Exception
   */
  @Override
  public Page createPage(Wiki wiki, String pageId, Page parentPage, String title) throws Exception {
    if (parentPage == null) {
      throw new IllegalArgumentException("Parent page cannot be null when creating the new page " + wiki.getType() + ":" + wiki.getOwner() + ":" + pageId);
    }

    Model model = getModel();
    PageImpl page = ((WikiImpl)wiki).createWikiPage();
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
    LinkRegistry linkRegistry = ((WikiImpl)wiki).getLinkRegistry();
    String newEntryName = getLinkEntryName(wiki.getType(), wiki.getOwner(), pageId);
    String newEntryAlias = getLinkEntryAlias(wiki.getType(), wiki.getOwner(), pageId);
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

  @Override
  public Page getPageById(String wikiType, String wikiOwner, String pageId) throws Exception {
    Page page = null;

    Wiki wiki = getWiki(wikiType, wikiOwner, true);

    if(wiki != null) {
      if (WikiNodeType.Definition.WIKI_HOME_NAME.equals(pageId) || pageId == null) {
        page = wiki.getWikiHome();
      } else {
        String statement = new WikiSearchData(wikiType, wikiOwner, pageId).getPageConstraint();
        if (statement != null) {
          Model model = getModel();
          WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
          ChromatticSession session = wStore.getSession();
          if (session != null) {
            page = searchPage(statement, session);
            if (page == null && (page = wiki.getWikiHome()) != null) {
              String wikiHomeId = TitleResolver.getId(page.getTitle(), true);
              if (!wikiHomeId.equals(pageId)) {
                page = null;
              }
            }
          }
        }
      }
    }

    return page;
  }

  @Override
  public Page getWikiPageByUUID(String uuid) throws Exception {
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    ChromatticSession session = wStore.getSession();

    return session.findById(Page.class, uuid);
  }

  @Override
  public void createTemplatePage(ConfigurationManager configurationManager, String templateSourcePath, String targetPath) {
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    ChromatticSession session = wStore.getSession();
    if (templateSourcePath != null) {
      InputStream is = null;
      try {
        is = configurationManager.getInputStream(templateSourcePath);
        int type = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
        if(((Node)session.getJCRSession().getItem(targetPath)).hasNode(WikiNodeType.WIKI_TEMPLATE_CONTAINER)) {
          type = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
        }
        session.getJCRSession().importXML(targetPath, is, type);
        session.save();
      } catch(Exception e) {
        // TODO
        e.printStackTrace();
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (IOException e) {
            // TODO
            e.printStackTrace();
          }
        }
      }
    }
  }

  @Override
  public void deletePage(String wikiType, String wikiOwner, String pageId) throws Exception {
    Model model = getModel();
    Page page = getPageById(wikiType, wikiOwner, pageId);
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    ChromatticSession session = wStore.getSession();
    RemovedMixin mix = session.create(RemovedMixin.class);
    session.setEmbedded(page, RemovedMixin.class, mix);
    mix.setRemovedBy(Utils.getCurrentUser());
    Calendar calendar = GregorianCalendar.getInstance();
    calendar.setTimeInMillis(new Date().getTime());
    mix.setRemovedDate(calendar.getTime());
    mix.setParentPath(page.getParentPage().getPath());
    WikiImpl wiki = (WikiImpl) getWiki(wikiType, wikiOwner, false);
    Trash trash = wiki.getTrash();
    if (trash.isHasPage(page.getName())) {
      PageImpl oldDeleted = trash.getPage(page.getName());
      String removedDate = oldDeleted.getRemovedMixin().getRemovedDate().toGMTString();
      String newName = page.getName() + "_" + removedDate.replaceAll(" ", "-").replaceAll(":", "-");
      trash.addChild(newName, oldDeleted);
    }
    trash.addRemovedWikiPage((PageImpl) page);

    //update LinkRegistry
    LinkRegistry linkRegistry = wiki.getLinkRegistry();
    if (linkRegistry.getLinkEntries().get(getLinkEntryName(wikiType, wikiOwner, pageId)) != null) {
      linkRegistry.getLinkEntries().get(getLinkEntryName(wikiType, wikiOwner, pageId)).setNewLink(null);
    }

    session.save();
  }

  @Override
  public void deleteTemplatePage(String wikiType, String wikiOwner, String templateId) throws Exception {
    WikiPageParams params = new WikiPageParams(wikiType, wikiOwner, templateId);
    getTemplatePage(params, templateId).remove();
  }

  @Override
  public Template getTemplatePage(WikiPageParams params, String templateId) throws Exception {
    return getTemplatesContainer(params).getTemplate(templateId);
  }

  @Override
  public Map<String, Template> getTemplates(WikiPageParams params) throws Exception {
    return getTemplatesContainer(params).getTemplates();
  }

  @Override
  public TemplateContainer getTemplatesContainer(WikiPageParams params) throws Exception {
    WikiImpl wiki = (WikiImpl) getWiki(params.getType(), params.getOwner(), false);
    return wiki.getPreferences().getTemplateContainer();
  }

  @Override
  public void deleteDraftNewPage(String newDraftPageId) throws Exception {
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    PageImpl draftNewPagesContainer = wStore.getDraftNewPagesContainer();
    PageImpl draftPage = (PageImpl) draftNewPagesContainer.getChild(newDraftPageId);
    if (draftPage != null) {
      draftPage.remove();
    }
  }

  @Override
  public void renamePage(String wikiType, String wikiOwner, String pageName, String newName, String newTitle) throws Exception {
    PageImpl currentPage = (PageImpl) getPageById(wikiType, wikiOwner, pageName);
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
    getModel().save();
    currentPage.setTitle(newTitle);
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
  }

  @Override
  public void movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws Exception {
    PageImpl destPage = (PageImpl) getPageById(newLocationParams.getType(),
            newLocationParams.getOwner(),
            newLocationParams.getPageId());
    if (destPage == null || !destPage.hasPermission(PermissionType.EDITPAGE)) {
      throw new Exception("Destination page " + newLocationParams.getType() + ":" +
              newLocationParams.getOwner() + ":" + newLocationParams.getPageId() + " does not exist");
    }
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
  }

  @Override
  public List<PermissionEntry> getWikiPermission(String wikiType, String wikiOwner) throws Exception {
    List<PermissionEntry> permissionEntries = new ArrayList<>();

    Model model = getModel();
    Wiki wiki = getWikiWithoutPermission(wikiType, wikiOwner, model);
    if (wiki == null) {
      return permissionEntries;
    }
    if (!wiki.getDefaultPermissionsInited()) {
      List<String> permissions = getWikiDefaultPermissions(wikiType, wikiOwner);
      wiki.setWikiPermissions(permissions);
      wiki.setDefaultPermissionsInited(true);
      HashMap<String, String[]> permMap = new HashMap<>();
      for (String perm : permissions) {
        String[] actions = perm.substring(0, perm.indexOf(":")).split(",");
        perm = perm.substring(perm.indexOf(":") + 1);
        String id = perm.substring(perm.indexOf(":") + 1);
        List<String> jcrActions = new ArrayList<>();
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
      org.exoplatform.wiki.service.Permission[] perms = new org.exoplatform.wiki.service.Permission[4];
      perms[0] = new org.exoplatform.wiki.service.Permission();
      perms[0].setPermissionType(PermissionType.VIEWPAGE);
      perms[1] = new org.exoplatform.wiki.service.Permission();
      perms[1].setPermissionType(PermissionType.EDITPAGE);
      perms[2] = new org.exoplatform.wiki.service.Permission();
      perms[2].setPermissionType(PermissionType.ADMINPAGE);
      perms[3] = new org.exoplatform.wiki.service.Permission();
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
    List<String> permissions = new ArrayList<>();
    Iterator<Map.Entry<String, IDType>> iter = Utils.getACLForAdmins().entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<String, IDType> entry = iter.next();
      permissions.add(new StringBuilder(all).append(":").append(entry.getValue()).append(":").append(entry.getKey()).toString());
    }
    if (PortalConfig.PORTAL_TYPE.equals(wikiType)) {
      UserPortalConfigService service = ExoContainerContext.getCurrentContainer()
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
      UserACL userACL = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(UserACL.class);
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
  public void setWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws Exception {
    WikiImpl wiki = (WikiImpl) getWiki(wikiType, wikiOwner, false);
    List<String> permissions = new ArrayList<>();
    HashMap<String, String[]> permMap = new HashMap<>();
    for (PermissionEntry entry : permissionEntries) {
      StringBuilder actions = new StringBuilder();
      org.exoplatform.wiki.service.Permission[] pers = entry.getPermissions();
      List<String> permlist = new ArrayList<>();
      // Permission strings has the format:
      // VIEWPAGE,EDITPAGE,ADMINPAGE,ADMINSPACE:USER:john
      // VIEWPAGE:GROUP:/platform/users
      // VIEWPAGE,EDITPAGE,ADMINPAGE,ADMINSPACE:MEMBERSHIP:manager:/platform/administrators
      for (int i = 0; i < pers.length; i++) {
        org.exoplatform.wiki.service.Permission perm = pers[i];
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
  public Page getRelatedPage(String wikiType, String wikiOwner, String pageId) throws Exception {
    WikiImpl wiki = (WikiImpl) getWiki(wikiType, wikiOwner, false);
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
  public Page getExsitedOrNewDraftPageById(String wikiType, String wikiOwner, String pageId, String username) throws Exception {
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
    DraftPageImpl draftPage;

    // Check if in the case that access to wiki page by rest service of xwiki
    if ((username == null) && (pageId.contains(Utils.SPLIT_TEXT_OF_DRAFT_FOR_NEW_PAGE))) {
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
      draftPage = (DraftPageImpl) getDraft(pageId, username);
      if (draftPage != null) {
        return draftPage;
      }
    }

    // Get draft page container
    if (userWiki == null) {
      userWiki = (UserWiki) getWiki(PortalConfig.USER_TYPE, username, false);
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
    permissions.put(IdentityConstants.ANY, new String[]{org.exoplatform.services.jcr.access.PermissionType.READ});
    draftPage.setPermission(permissions);
    return draftPage;
  }

  @Override
  public DraftPage getDraft(WikiPageParams param, String username) throws Exception {
    if ((param.getPageId() == null) || (param.getOwner() == null) || (param.getType() == null)) {
      return null;
    }

    Page targetPage = getPageById(param.getType(), param.getOwner(), param.getPageId());
    if ((param.getPageId() == null) || (targetPage == null)) {
      return null;
    }
    return getDraftOfWikiPage(targetPage, username);
  }


  @Override
  public DraftPage getLastestDraft(String username) throws Exception {
    // Get all draft pages
    UserWiki userWiki = (UserWiki) getWiki(PortalConfig.USER_TYPE, username, true);
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
    return lastestDraft;
  }

  @Override
  public DraftPage getDraft(String draftName, String username) throws Exception {
    List<DraftPage> drafts = getDrafts(username);
    for (DraftPage draftPage : drafts) {
      if (draftPage.getName().equals(draftName)) {
        return draftPage;
      }
    }

    return null;
  }

  @Override
  public List<DraftPage> getDrafts(String username) throws Exception {
    List<DraftPage> draftPages = new ArrayList<>();

    // Get all draft of user
    UserWiki userWiki = (UserWiki) getWiki(PortalConfig.USER_TYPE, username, true);
    Collection<PageImpl> childPages = userWiki.getDraftPagesContainer().getChildPages().values();

    // Change collection to List
    for (PageImpl pageImpl : childPages) {
      draftPages.add((DraftPageImpl) pageImpl);
    }

    return draftPages;
  }


  @Override
  public PageList<SearchResult> search(WikiSearchData data) throws Exception {
    List<SearchResult> resultList = new ArrayList<>();
    long numberOfSearchForTitleResult = 0;

    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    ChromatticSession session = wStore.getSession();

    if (!StringUtils.isEmpty(data.getTitle())) {
      // Search for title
      String statement = data.getStatementForSearchingTitle();
      QueryImpl q = (QueryImpl) ((ChromatticSessionImpl) session).getDomainSession().getSessionWrapper().createQuery(statement);
      if(data.getOffset() > 0) {
        q.setOffset(data.getOffset());
      }
      if(data.getLimit() > 0) {
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
      String statement = data.getStatementForSearchingContent();
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
    // Return all the result
    return new ObjectPageList<>(resultList, resultList.size());
  }

  @Override
  public List<SearchResult> searchRenamedPage(WikiSearchData data) throws Exception {
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    ChromatticSession session = wStore.getSession();

    List<SearchResult> resultList = new ArrayList<>() ;
    String statement = data.getStatementForRenamedPage() ;
    Query q = ((ChromatticSessionImpl)session).getDomainSession().getSessionWrapper().createQuery(statement);
    QueryResult result = q.execute();
    NodeIterator iter = result.getNodes() ;
    while(iter.hasNext()) {
      try {
        resultList.add(getResult(iter.nextNode()));
      } catch (Exception e) {
        log.debug("Failed to add item search result", e);
      }
    }
    return resultList ;
  }

  @Override
  public InputStream getAttachmentAsStream(String path) throws Exception {
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    ChromatticSession session = wStore.getSession();

    Node attContent = (Node)session.getJCRSession().getItem(path) ;
    return attContent.getProperty(WikiNodeType.Definition.DATA).getStream() ;
  }

  @Override
  public Object findByPath(String path, String objectNodeType) {
    try {
      Model model = getModel();
      WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
      if (WikiNodeType.WIKI_PAGE.equals(objectNodeType)) {
        return wStore.getSession().findByPath(PageImpl.class, path);
      } else if (WikiNodeType.WIKI_ATTACHMENT.equals(objectNodeType)) {
        return wStore.getSession().findByPath(AttachmentImpl.class, path);
      } else if (WikiNodeType.WIKI_TEMPLATE.equals(objectNodeType)) {
        return wStore.getSession().findByPath(Template.class, path);
      }
    } catch (Exception e) {
      log.error("Can't find Object", e);
    }
    return null;
  }

  @Override
  public Page getHelpSyntaxPage(String syntaxId, List<ValuesParam> syntaxHelpParams, ConfigurationManager configurationManager) throws Exception {
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    HelpPage helpPageByChromattic = wStore.getHelpPageByChromattic();

    if(helpPageByChromattic == null || wStore.getHelpPagesContainer().getChildPages().size() == 0) {
      createHelpPages(syntaxHelpParams, configurationManager);
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
  public Page getEmotionIconsPage(MetaDataPage metaPage) throws Exception {
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    return wStore.getEmotionIconsPage();
  }

  private synchronized void createHelpPages(List<ValuesParam> syntaxHelpParams, ConfigurationManager configurationManager) throws Exception {
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
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
          wStore.getSession().save();
        } catch (Exception e) {
          log.error("Can not create Help page", e);
        }
      }
    }
  }

  @Override
  public String getPortalOwner() {
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    List<Wiki> portalWikis = new ArrayList<>(wStore.getWikiContainer(WikiType.PORTAL).getAllWikis());
    if (portalWikis.size() > 0) {
      return portalWikis.get(0).getOwner();
    }
    return null;
  }

  @Override
  public boolean hasAdminSpacePermission(String wikiType, String owner, Identity user) throws Exception {
    List<AccessControlEntry> aces = getAccessControls(wikiType, owner);
    AccessControlList acl = new AccessControlList(owner, aces);
    String[] permission = new String[]{PermissionType.ADMINSPACE.toString()};
    return Utils.hasPermission(acl, permission, user);
  }

  @Override
  public boolean hasAdminPagePermission(String wikiType, String owner, Identity user) throws Exception {
    List<AccessControlEntry> aces = getAccessControls(wikiType, owner);
    AccessControlList acl = new AccessControlList(owner, aces);
    String[] permission = new String[]{PermissionType.ADMINPAGE.toString()};
    return Utils.hasPermission(acl, permission, user);
  }


  private List<AccessControlEntry> getAccessControls(String wikiType, String wikiOwner) throws Exception {
    List<AccessControlEntry> aces = new ArrayList<>();
    try {
      List<PermissionEntry> permissionEntries = getWikiPermission(wikiType, wikiOwner);
      for (PermissionEntry perm : permissionEntries) {
        org.exoplatform.wiki.service.Permission[] permissions = perm.getPermissions();
        List<String> actions = new ArrayList<>();
        for (org.exoplatform.wiki.service.Permission permission : permissions) {
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

  private HelpPage addSyntaxPage(WikiStoreImpl wStore,
                                 PageImpl parentPage,
                                 String name,
                                 InputStream content,
                                 String type) throws Exception {
    StringBuilder stringContent = new StringBuilder();
    BufferedReader bufferReader;
    String tempLine;
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
    content.close();
    bufferReader.close();
    return syntaxPage;
  }

  private boolean isDuplicateTitle(List<SearchResult> list, SearchResult result) {
    for (SearchResult searchResult : list) {
      if (result.getTitle().equals(searchResult.getTitle())) {
        return true;
      }
    } 
    return false;
  }

  private void updateAllPagesPermissions(String wikiType, String wikiOwner, HashMap<String, String[]> permMap) throws Exception {
    Page page = getWiki(wikiType, wikiOwner, false).getWikiHome();
    Queue<PageImpl> queue = new LinkedList<>();
    queue.add((PageImpl)page);
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
  }

  private Page getPageWithLinkEntry(LinkEntry entry) throws Exception {
    String linkEntryAlias = entry.getAlias();
    String[] splits = linkEntryAlias.split("@");
    String wikiType = splits[0];
    String wikiOwner = splits[1];
    String pageId = linkEntryAlias.substring((wikiType + "@" + wikiOwner + "@").length());
    return getPageById(wikiType, wikiOwner, pageId);
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

  private void setFullPermissionForOwner(PageImpl page, String owner) throws Exception {
    ConversationState conversationState = ConversationState.getCurrent();

    if (conversationState != null) {
      HashMap<String, String[]> permissions = page.getPermission();
      permissions.put(conversationState.getIdentity().getUserId(), org.exoplatform.services.jcr.access.PermissionType.ALL);
      page.setPermission(permissions);
    }
  }

  private Model getModel() {
    MOWService mowService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(MOWService.class);
    return mowService.getModel();
  }
  
  private SearchResult getResult(Row row, WikiSearchData data) throws Exception {
    String type = row.getValue(WikiNodeType.Definition.PRIMARY_TYPE).getString();
    String path = row.getValue(WikiNodeType.Definition.PATH).getString();
    
    String title = StringUtils.EMPTY;
    String excerpt = StringUtils.EMPTY;
    long jcrScore = row.getValue("jcr:score").getLong();
    Calendar updateDate = GregorianCalendar.getInstance();
    Calendar createdDate = GregorianCalendar.getInstance();
    PageImpl page = null;
    if (WikiNodeType.WIKI_ATTACHMENT.equals(type)) {
      // Transform to Attachment result
      type = WikiNodeType.WIKI_ATTACHMENT.toString();
      if(!path.endsWith(WikiNodeType.Definition.CONTENT)){
        AttachmentImpl searchAtt = (AttachmentImpl) Utils.getObject(path, WikiNodeType.WIKI_ATTACHMENT);
        updateDate = searchAtt.getUpdatedDate();
        page = searchAtt.getParentPage();
        createdDate.setTime(page.getCreatedDate());
        title = page.getTitle();
      } else {
        String pagePath = path.substring(0, path.lastIndexOf("/" + WikiNodeType.Definition.CONTENT));
        type = WikiNodeType.WIKI_PAGE_CONTENT.toString();
        page = (PageImpl) Utils.getObject(pagePath, WikiNodeType.WIKI_PAGE);
        title = page.getTitle();
        updateDate.setTime(page.getUpdatedDate());
        createdDate.setTime(page.getCreatedDate());
      }
    } else if (WikiNodeType.WIKI_PAGE.equals(type)) {
      page = (PageImpl) Utils.getObject(path, type);
      updateDate.setTime(page.getUpdatedDate());
      createdDate.setTime(page.getCreatedDate());
      title = page.getTitle();
    } else {
      return null;
    }
    
    //get the excerpt from row result
    excerpt = getExcerpt(row, type);

    if (page == null || !page.hasPermission(PermissionType.VIEWPAGE)) {
      return null;
    }
    
    SearchResult result = new SearchResult(excerpt, title, path, type, updateDate, createdDate);
    result.setUrl(page.getURL());
    result.setJcrScore(jcrScore);
    return result;
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

  /**
   * gets except of row result based on specific properties, but all to get nice excerpt
   * @param row the result row
   * @param type the result type
   * @return the excerpt
   * @throws ItemNotFoundException
   * @throws RepositoryException
   */
  private String getExcerpt(Row row, String type) throws ItemNotFoundException, RepositoryException {
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
    return ret.toString();
  }
  
  private SearchResult getResult(Node node)throws Exception {
    SearchResult result = new SearchResult() ;
    result.setPageName(node.getName()) ;
    String title = node.getProperty(WikiNodeType.Definition.TITLE).getString();
    InputStream data = node.getNode(WikiNodeType.Definition.CONTENT).getNode(WikiNodeType.Definition.ATTACHMENT_CONTENT).getProperty(WikiNodeType.Definition.DATA).getStream();
    byte[] bytes = IO.getBytes(data);
    String content = new String(bytes, "UTF-8");
    if(content.length() > 100) content = content.substring(0, 100) + "...";
    result.setExcerpt(content) ;
    result.setTitle(title) ;
    return result ;
  }
  
  private boolean isContains(List<SearchResult> list, SearchResult result) throws Exception {
    AttachmentImpl att = null;
    PageImpl page = null;
    if (WikiNodeType.WIKI_ATTACHMENT.equals(result.getType())) {
      att = (AttachmentImpl) Utils.getObject(result.getPath(), WikiNodeType.WIKI_ATTACHMENT);
    } else if (WikiNodeType.WIKI_ATTACHMENT_CONTENT.equals(result.getType())) {
      String attPath = result.getPath().substring(0, result.getPath().lastIndexOf("/"));
      att = (AttachmentImpl) Utils.getObject(attPath, WikiNodeType.WIKI_ATTACHMENT);
    } else if(WikiNodeType.WIKI_PAGE.equals(result.getType()) || WikiNodeType.WIKI_HOME.equals(result.getType())){
      page = (PageImpl) Utils.getObject(result.getPath(), WikiNodeType.WIKI_PAGE);
    } else if (WikiNodeType.WIKI_PAGE_CONTENT.equals(result.getType())) {
      att = (AttachmentImpl) Utils.getObject(result.getPath(), WikiNodeType.WIKI_ATTACHMENT);
      page = att.getParentPage();
    }
    if (att != null || page != null) {
      Iterator<SearchResult> iter = list.iterator();
      while (iter.hasNext()) {
        SearchResult child = iter.next();
        if (WikiNodeType.WIKI_ATTACHMENT.equals(child.getType()) || WikiNodeType.WIKI_PAGE_CONTENT.equals(child.getType())) {
          AttachmentImpl tempAtt = (AttachmentImpl) Utils.getObject(child.getPath(), WikiNodeType.WIKI_ATTACHMENT);
          if (att != null && att.equals(tempAtt)) {
            // Merge data
            if (child.getExcerpt()==null && result.getExcerpt()!=null ){
              child.setExcerpt(result.getExcerpt());
            }
            return true;
          }               
          if (page != null && page.getName().equals(tempAtt.getParentPage())) {
            return true;
          }     
        } else if (WikiNodeType.WIKI_PAGE.equals(child.getType())) {
          if (page != null && page.getPath().equals(child.getPath())) {
            iter.remove();
            return false;
          }
        }
      }
    }
    return false;
  }

  private DraftPage getDraftOfWikiPage(Page targetPage, String username) throws Exception {
    // If target page is null then return null
    if (targetPage == null) {
      return null;
    }

    if (IdentityConstants.ANONIM.equals(username)) {
      return null;
    }

    // Get all draft pages
    UserWiki userWiki = (UserWiki) getWiki(PortalConfig.USER_TYPE, username, true);
    Collection<PageImpl> childPages = userWiki.getDraftPagesContainer().getChildPages().values();

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

  @Override
  public List<TemplateSearchResult> searchTemplate(TemplateSearchData data) throws Exception {
    Model model = getModel();
    WikiStoreImpl wStore = (WikiStoreImpl) model.getWikiStore();
    ChromatticSession session = wStore.getSession();

    List<TemplateSearchResult> resultList = new ArrayList<>();
    String statement = data.getStatementForSearchingTitle();
    Query q = ((ChromatticSessionImpl)session).getDomainSession().getSessionWrapper().createQuery(statement);
    QueryResult result = q.execute();
    RowIterator iter = result.getRows();
    while (iter.hasNext()) {
      TemplateSearchResult tempResult = getTemplateResult(iter.nextRow());
      resultList.add(tempResult);
    }
   return resultList;
  }

  private TemplateSearchResult getTemplateResult(Row row) throws Exception {
    String type = row.getValue(WikiNodeType.Definition.PRIMARY_TYPE).getString();

    String path = row.getValue(WikiNodeType.Definition.PATH).getString();
    String title = (row.getValue(WikiNodeType.Definition.TITLE) == null ? null : row.getValue(WikiNodeType.Definition.TITLE).getString());
    
    Template template = (Template) Utils.getObject(path, WikiNodeType.WIKI_PAGE);
    String description = template.getDescription();
    TemplateSearchResult result = new TemplateSearchResult(template.getName(),
                                                           title,
                                                           path,
                                                           type,
                                                           null,
                                                           null,
                                                           description);
    return result;
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
}
