/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.wiki.mow.core.api.wiki;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

import org.chromattic.api.ChromatticSession;
import org.chromattic.api.DuplicateNameException;
import org.chromattic.api.RelationshipType;
import org.chromattic.api.annotations.Create;
import org.chromattic.api.annotations.DefaultValue;
import org.chromattic.api.annotations.Destroy;
import org.chromattic.api.annotations.ManyToOne;
import org.chromattic.api.annotations.MappedBy;
import org.chromattic.api.annotations.Name;
import org.chromattic.api.annotations.OneToMany;
import org.chromattic.api.annotations.OneToOne;
import org.chromattic.api.annotations.Owner;
import org.chromattic.api.annotations.Path;
import org.chromattic.api.annotations.PrimaryType;
import org.chromattic.api.annotations.Property;
import org.chromattic.api.annotations.WorkspaceName;
import org.chromattic.ext.ntdef.NTFolder;
import org.chromattic.ext.ntdef.Resource;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.chromattic.ext.ntdef.NTVersion;
import org.exoplatform.wiki.chromattic.ext.ntdef.VersionableMixin;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Permission;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiNodeType;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.rendering.converter.ConfluenceToXWiki2Transformer;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.PermissionType;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.utils.Utils;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.syntax.Syntax;

@PrimaryType(name = WikiNodeType.WIKI_PAGE)
public abstract class PageImpl extends NTFolder implements Page {
  
  private static final Log      LOG               = ExoLogger.getLogger(PageImpl.class.getName());
  
  private MOWService mowService;
  
  private WikiService wService;
  
  private Permission permission = new PermissionImpl();
  
  private ComponentManager componentManager;
  
  /**
   * caching related pages for performance
   */
  private List<PageImpl> relatedPages = null;
  
  private boolean isMinorEdit = false;
  
  public void setMOWService(MOWService mowService) {
    this.mowService = mowService;
    permission.setMOWService(mowService);
  }
  
  public MOWService getMOWService() {
    return mowService;
  }
  
  public void setWikiService(WikiService wService) {
    this.wService = wService;
  }

  public ChromatticSession getChromatticSession() {
    return mowService.getSession();
  }
  
  public Session getJCRSession() {
    return getChromatticSession().getJCRSession();
  }
  
  public WikiService getWikiService(){
    return wService;
  }
  
  public void setComponentManager(ComponentManager componentManager) {
    this.componentManager = componentManager;
  }

  public Node getJCRPageNode() throws Exception {
    return (Node) getChromatticSession().getJCRSession().getItem(getPath());
  }
  
  @Name
  @Override
  public abstract String getName();
  public abstract void setName(String name);
  
  @Path
  public abstract String getPath();

  @WorkspaceName
  public abstract String getWorkspace();
  
  @OneToOne
  @Owner
  @MappedBy(WikiNodeType.Definition.CONTENT)
  protected abstract AttachmentImpl getContentByChromattic();

  protected abstract void setContentByChromattic(AttachmentImpl content);

  @Create
  protected abstract AttachmentImpl createContent();

  @Override
  public AttachmentImpl getContent() {
    AttachmentImpl content = getContentByChromattic();
    if (content == null) {
      content = createContent();
      setContentByChromattic(content);
      content.setText("");
    } else {
      String syntax = getSyntax();
      if (Syntax.CONFLUENCE_1_0.toIdString().equals(syntax)) {
        content.setText(ConfluenceToXWiki2Transformer.transformContent(content.getText(), componentManager));
        setSyntax(Syntax.XWIKI_2_0.toIdString());
        setContentByChromattic(content);
      }
    }
    return content;
  }
  
  @Property(name = WikiNodeType.Definition.TITLE)
  public abstract String getTitleByChromattic();
  public abstract void setTitleByChromattic(String title);
  
  @Override
  public String getTitle() {
    String title = getTitleByChromattic();
    return (title != null) ? title : getName();
  }
  
  @Override
  public void setTitle(String title) {
    setTitleByChromattic(title);
  }
  
  @Property(name = WikiNodeType.Definition.SYNTAX)
  public abstract String getSyntax();
  public abstract void setSyntax(String syntax);
  
  @Property(name = WikiNodeType.Definition.COMMENT)
  @DefaultValue({""})
  public abstract String getComment();
  public abstract void setComment(String comment);
  
  @Property(name = WikiNodeType.Definition.OWNER)
  public abstract String getOwner();
  public abstract void setOwner(String owner);
  
  @Property(name = WikiNodeType.Definition.AUTHOR)
  public abstract String getAuthor();

  @Property(name = WikiNodeType.Definition.CREATED_DATE)
  public abstract Date getCreatedDate();
  public abstract void setCreatedDate(Date date);
  
  @Property(name = WikiNodeType.Definition.UPDATED_DATE)
  public abstract Date getUpdatedDate();
  
  @Property(name = WikiNodeType.Definition.URL)
  public abstract String getURL();
  public abstract void setURL(String url);
  
  @OneToOne(type = RelationshipType.EMBEDDED)
  @Owner
  public abstract MovedMixin getMovedMixin();
  public abstract void setMovedMixin(MovedMixin move);
  
  @OneToOne(type = RelationshipType.EMBEDDED)
  @Owner
  public abstract RemovedMixin getRemovedMixin();
  public abstract void setRemovedMixin(RemovedMixin remove);
  
  @OneToOne(type = RelationshipType.EMBEDDED)
  @Owner
  public abstract RenamedMixin getRenamedMixin();
  public abstract void setRenamedMixin(RenamedMixin mix);
  
  @OneToOne(type = RelationshipType.EMBEDDED)
  @Owner
  public abstract WatchedMixin getWatchedMixin();
  public abstract void setWatchedMixin(WatchedMixin mix);
  
  @Create
  protected abstract WatchedMixin createWatchedMixin();
  
  public void makeWatched() {
    WatchedMixin watchedMixin = getWatchedMixin();
    if (watchedMixin == null) {
      watchedMixin = createWatchedMixin();
      setWatchedMixin(watchedMixin);
    }
  }
  
  public VersionableMixin getVersionableMixin() {
    return getContent().getVersionableMixin();
  }

  public void makeVersionable() {
    this.getContent().makeVersionable();
  }
  
  
  //TODO: replace by @Checkin when Chromattic support
  public NTVersion checkin() throws Exception {
    PageDescriptionMixin description = getContent().getPageDescriptionMixin();
    description.setAuthor(ConversationState.getCurrent().getIdentity().getUserId());
    description.setUpdatedDate(GregorianCalendar.getInstance().getTime());
    description.setComment(this.getComment());
    //create new version only for the page content node, but whole wiki page to improve performance.
    NTVersion ret = getContent().checkin();
    return ret;
  }

  //TODO: replace by @Checkout when Chromattic support
  public void checkout() throws Exception {
    getContent().checkout();
  }

  //TODO: replace by @Restore when Chromattic support
  public void restore(String versionName, boolean removeExisting) throws Exception {
    getContent().restore(versionName, removeExisting);
  }
  
  @Create
  public abstract AttachmentImpl createAttachment();
  
  public AttachmentImpl createAttachment(String fileName, Resource contentResource) throws Exception {
    if (fileName == null) {
      throw new NullPointerException();
    }
    Iterator<AttachmentImpl> attIter= getAttachments().iterator();
    while (attIter.hasNext()) {
      AttachmentImpl att = attIter.next();
      if (att.getName().equals(fileName)) {
        att.remove();
      }
    }
    
    AttachmentImpl file = createAttachment();
    file.setName(TitleResolver.getId(fileName, false));
    addAttachment(file);
    if (fileName.lastIndexOf(".") > 0) {
      file.setTitle(fileName.substring(0, fileName.lastIndexOf(".")));
      file.setFileType(fileName.substring(fileName.lastIndexOf(".")));
    } else {
      file.setTitle(fileName);
    }
    
    if (contentResource != null) {
      file.setContentResource(contentResource);
    }
    getChromatticSession().save();
    setFullPermissionForOwner(file);
    return file;
  }
  
  private void setFullPermissionForOwner(AttachmentImpl file) throws Exception {
    ConversationState conversationState = ConversationState.getCurrent();

    if (conversationState != null) {
      HashMap<String, String[]> permissions = file.getPermission();
      permissions.put(conversationState.getIdentity().getUserId(), org.exoplatform.services.jcr.access.PermissionType.ALL);
      file.setPermission(permissions);
    }
  }
  
  @OneToMany
  public abstract Collection<AttachmentImpl> getAttachmentsByChromattic();

  @Override
  public Collection<AttachmentImpl> getAttachments() {
    return getAttachmentsByChromattic();
  }
  
  public Collection<AttachmentImpl> getAttachmentsExcludeContent() throws Exception {
    Collection<AttachmentImpl> attachments = getAttachmentsByChromattic();
    List<AttachmentImpl> atts = new ArrayList<AttachmentImpl>();
    for (AttachmentImpl attachment : attachments) {
      if ((attachment.hasPermission(PermissionType.VIEW_ATTACHMENT)
          || attachment.hasPermission(PermissionType.EDIT_ATTACHMENT))
          && !WikiNodeType.Definition.CONTENT.equals(attachment.getName())) {
        atts.add(attachment);
      }
    }
    Collections.sort(atts);
    return atts;
  }
  
  public Collection<AttachmentImpl> getAttachmentsExcludeContentByRootPermisison() throws Exception {
    Collection<AttachmentImpl> attachments = getAttachmentsByChromattic();
    List<AttachmentImpl> atts = new ArrayList<AttachmentImpl>(attachments.size());
    for (AttachmentImpl attachment : attachments) {
      if (!WikiNodeType.Definition.CONTENT.equals(attachment.getName())) {
        atts.add(attachment);
      }
    }
    Collections.sort(atts);
    return atts;
  }
  
  public AttachmentImpl getAttachment(String attachmentId) throws Exception {
    for (AttachmentImpl attachment : getAttachments()) {
      if (attachment.getName().equals(attachmentId)
          && (attachment.hasPermission(PermissionType.VIEW_ATTACHMENT)
          || attachment.hasPermission(PermissionType.EDIT_ATTACHMENT))) {
        return attachment;
      }
    }
    return null;
  }
  
  public AttachmentImpl getAttachmentByRootPermisison(String attachmentId) throws Exception {
    for (AttachmentImpl attachment : getAttachments()) {
      if (attachment.getName().equals(attachmentId)) {
        return attachment;
      }
    }
    return null;
  }
  
  public void addAttachment(AttachmentImpl attachment) throws DuplicateNameException {
    getAttachments().add(attachment);
  }  
  
  public void removeAttachment(String attachmentId) throws Exception {
    AttachmentImpl attachment = getAttachment(attachmentId);
    if(attachment != null){
      attachment.remove();
    }
  }
  
  @ManyToOne
  public abstract PageImpl getParentPage();
  public abstract void setParentPage(PageImpl page);

  @ManyToOne
  public abstract Trash getTrash();
  public abstract void setTrash(Trash trash);
  
  @OneToMany
  protected abstract Map<String, PageImpl> getChildrenContainer();
  
  public Map<String, PageImpl> getChildPages() throws Exception {
    TreeMap<String, PageImpl> result = new TreeMap<String, PageImpl>(new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return o1.compareTo(o2);
      }
    });
    List<PageImpl> pages = new ArrayList<PageImpl>(getChildrenContainer().values());
    
    for (int i = 0; i < pages.size(); i++) {
      PageImpl page = pages.get(i);
      if (page != null && page.hasPermission(PermissionType.VIEWPAGE)) {
        result.put(page.getName(), page);
      }
    }
    return result;
  }
  
  public Map<String, PageImpl> getChildrenByRootPermission() throws Exception {
    TreeMap<String, PageImpl> result = new TreeMap<String, PageImpl>(new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return o1.compareTo(o2);
      }
    });
    List<PageImpl> pages = new ArrayList<PageImpl>(getChildrenContainer().values());
    
    for (int i = 0; i < pages.size(); i++) {
      PageImpl page = pages.get(i);
      if (page != null) {
        result.put(page.getTitle(), page);
      }
    }
    return result;
  }
  
  @Property(name = WikiNodeType.Definition.OVERRIDEPERMISSION)
  public abstract boolean getOverridePermission();
  public abstract void setOverridePermission(boolean isOverridePermission);
  
  @Override
  public boolean hasPermission(PermissionType permissionType) throws Exception {
    return permission.hasPermission(permissionType, getPath());
  }
  
  @Override
  public boolean hasPermission(PermissionType permissionType, Identity user) throws Exception {
    return permission.hasPermission(permissionType, getPath(), user);
  }
  
  @Override
  public HashMap<String, String[]> getPermission() throws Exception {
    return permission.getPermission(getPath());
  }
  
  @Override
  public void setPermission(HashMap<String, String[]> permissions) throws Exception {
    permission.setPermission(permissions, getPath());
  }
  
  public void setNonePermission() throws Exception {
    setPermission(null);
  }
  
  protected void addPage(String pageName, Page page) {
    if (pageName == null) {
      throw new NullPointerException();
    }
    if (page == null) {
      throw new NullPointerException();
    }
    Map<String, PageImpl> children = getChildrenContainer();
    if (children.containsKey(pageName)) {
      throw new IllegalStateException();
    }
    children.put(pageName, (PageImpl) page);
  }
  
  public void addWikiPage(Page page) {
    if (page == null) {
      throw new NullPointerException();
    }
    addPage(page.getName(), page);
  }
  
  public void addPublicPage(Page page) throws Exception {
    addWikiPage(page);
    page.setNonePermission();
  }
  
  public PageImpl getWikiPage(String pageId) throws Exception{
    if(WikiNodeType.Definition.WIKI_HOME_NAME.equalsIgnoreCase(pageId)){
      return this;
    }
    Iterator<PageImpl> iter = getChildPages().values().iterator();
    while(iter.hasNext()) {
      PageImpl page = (PageImpl)iter.next() ;
      if (pageId.equals(page.getName()))  return page ;         
    }
    return null ;
  }
  
  public Wiki getWiki() {
    WikiHome wikiHome = getWikiHome();
    if (wikiHome != null) {
      PortalWiki portalWiki = wikiHome.getPortalWiki();
      GroupWiki groupWiki = wikiHome.getGroupWiki();
      UserWiki userWiki = wikiHome.getUserWiki();
      if (portalWiki != null) {
        return portalWiki;
      } else if (groupWiki != null) {
        return groupWiki;
      } else {
        return userWiki;
      }
    }
    return null;
  }

  public WikiHome getWikiHome() {
    PageImpl parent = this.getParentPage();
    if (this instanceof WikiHome) {
      parent = this;
    } else
      while (parent != null && !(parent instanceof WikiHome)) {
        parent = parent.getParentPage();
      }
    return (WikiHome) parent;
  }
  
  public boolean isMinorEdit() {
    return isMinorEdit;
  }

  public void setMinorEdit(boolean isMinorEdit) {
    this.isMinorEdit = isMinorEdit;
  }

  @Destroy
  public abstract void remove();
  
  /**
   * add a related page
   * @param page
   * @return uuid of node of related page if add successfully. <br>
   *         null if add failed.
   * @throws NullPointerException if the param is null
   * @throws Exception when any error occurs.
   */
  public synchronized String addRelatedPage(PageImpl page) throws Exception {
    Map<String, Value> referredUUIDs = getReferredUUIDs();
    Session jcrSession = getJCRSession();
    Node myJcrNode = (Node) jcrSession.getItem(getPath());
    Node referredJcrNode = (Node) jcrSession.getItem(page.getPath());
    String referedUUID = referredJcrNode.getUUID();
    if (referredUUIDs.containsKey(referedUUID)) {
      return null;
    }
    Value value2Add = jcrSession.getValueFactory().createValue(referredJcrNode);
    referredUUIDs.put(referedUUID, value2Add);

    myJcrNode.setProperty(WikiNodeType.Definition.RELATION,
                          referredUUIDs.values().toArray(new Value[referredUUIDs.size()]));
    myJcrNode.save();
    // cache a related page.
    if (relatedPages != null) relatedPages.add(page);
    return referedUUID;
  }
  
  public List<PageImpl> getRelatedPages() throws Exception {
    if (relatedPages == null) {
      relatedPages = new ArrayList<PageImpl>();
      Iterator<Entry<String, Value>> refferedIter = getReferredUUIDs().entrySet().iterator();
      ChromatticSession chSession = getChromatticSession();
      while (refferedIter.hasNext()) {
        Entry<String, Value> entry = refferedIter.next();
        PageImpl page = chSession.findById(PageImpl.class, entry.getValue().getString());
        if(page != null && page.hasPermission(PermissionType.VIEWPAGE)){
          relatedPages.add(page);
        }
      }
    }
    return new ArrayList<PageImpl>(relatedPages);
  }
  
  /**
   * remove a specified related page.
   * @param page
   * @return uuid of node if related page is removed successfully <br>
   *         null if removing failed.
   * @throws Exception when an error is thrown.
   */
  public synchronized String removeRelatedPage(PageImpl page) throws Exception {
    Map<String, Value> referedUUIDs = getReferredUUIDs();
    Session jcrSession = getJCRSession();
    Node referredJcrNode = (Node) jcrSession.getItem(page.getPath());
    Node myJcrNode = (Node) jcrSession.getItem(getPath());
    String referredUUID = referredJcrNode.getUUID();
    if (!referedUUIDs.containsKey(referredUUID)) {
      return null;
    }
    referedUUIDs.remove(referredUUID);
    myJcrNode.setProperty(WikiNodeType.Definition.RELATION,
                          referedUUIDs.values().toArray(new Value[referedUUIDs.size()]));
    myJcrNode.save();
    // remove page from cache
    if (relatedPages != null) relatedPages.remove(page);
    return referredUUID;
  }

  
  /**
   * get reference uuids of current page
   * @return Map<String, Value> map of referred uuids of current page 
   * @throws Exception when an error is thrown.
   */
  public Map<String, Value> getReferredUUIDs() throws Exception {   
    Session jcrSession = getJCRSession();
    Node myJcrNode = (Node) jcrSession.getItem(getPath());
    Map<String, Value> referedUUIDs = new HashMap<String, Value>();
    if (myJcrNode.hasProperty(WikiNodeType.Definition.RELATION)) {
      Value[] values = myJcrNode.getProperty(WikiNodeType.Definition.RELATION).getValues();
      if (values != null && values.length > 0) {
        for (Value value : values) {
          referedUUIDs.put(value.getString(), value);
        }
      }
    }
    return referedUUIDs;
  }
  
  public synchronized void removeAllRelatedPages() throws Exception {
    Session jcrSession = getJCRSession();
    Node myJcrNode = (Node) jcrSession.getItem(getPath());
    myJcrNode.setProperty(WikiNodeType.Definition.RELATION, (Value[]) null);
    myJcrNode.save();
    // clear related pages in cache.
    if (relatedPages != null) relatedPages.clear();
  }
  
  /**
   * Migrates old page history data on the fly: 1.create version history of content child node <br/>
   * based on the history of page node, 2.remove the mix:versionable from page node. 
   * @throws Exception
   */
  public void migrateLegacyData() throws Exception {
    //migrate only when the current Page Node is mix:versionable
    if (this.getJCRPageNode().isNodeType(WikiNodeType.MIX_VERSIONABLE)) {
      Node pageNode = this.getJCRPageNode();
      if (LOG.isInfoEnabled()) {
        LOG.info("Migrating history for wiki page: " + pageNode.getPath());
      }
      //get history: author list, content list and updatedDate list
      List<String> authors = new ArrayList<String>();
      List<Calendar> calendars = new ArrayList<Calendar>();
      List<String> contents = new ArrayList<String>();
      List<String> comments = new ArrayList<String>();
      VersionIterator iter = pageNode.getVersionHistory().getAllVersions();
      while (iter.hasNext()) {
        Version v = iter.nextVersion();
        if (v.hasNode(WikiNodeType.JCR_FROZEN_NODE))  {
          Node frozenNode = v.getNode(WikiNodeType.JCR_FROZEN_NODE);
          authors.add(frozenNode.hasProperty(WikiNodeType.Definition.AUTHOR) ?
                      frozenNode.getProperty(WikiNodeType.Definition.AUTHOR).getString() : "");
          calendars.add(frozenNode.hasProperty(WikiNodeType.Definition.UPDATED_DATE) ? 
                        frozenNode.getProperty(WikiNodeType.Definition.UPDATED_DATE).getDate() : 
                        GregorianCalendar.getInstance());
          contents.add(frozenNode
                        .getNode(WikiNodeType.Definition.CONTENT)
                        .getNode(WikiNodeType.Definition.ATTACHMENT_CONTENT)
                        .getProperty(WikiNodeType.Definition.DATA).getString());
          comments.add(frozenNode.hasProperty(WikiNodeType.Definition.COMMENT) ?
                       frozenNode.getProperty(WikiNodeType.Definition.COMMENT).getString() : "");
        }
      }
      //remove mix:versionable of the page itself
      pageNode.removeMixin(WikiNodeType.MIX_VERSIONABLE);
      pageNode.save();
      this.makeVersionable();
      AttachmentImpl content = this.getContent();
      //save the current content
      String currentContent = content.getText();
      //create version history for content node
      for (int i = 0; i < authors.size(); i++) {
        PageDescriptionMixin description = content.getPageDescriptionMixin();
        description.setAuthor(authors.get(i));
        description.setUpdatedDate(calendars.get(i).getTime());
        content.setText(contents.get(i));
        description.setComment(comments.get(i));
        content.checkin();
        content.checkout();
      }
      //restore the current content
      content.setText(currentContent);
    }
  }
}
