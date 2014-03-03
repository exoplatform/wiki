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
package org.exoplatform.wiki.commons;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import javax.portlet.PortletPreferences;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.wiki.chromattic.ext.ntdef.NTVersion;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PortalWiki;
import org.exoplatform.wiki.mow.core.api.wiki.Preferences;
import org.exoplatform.wiki.mow.core.api.wiki.UserWiki;
import org.exoplatform.wiki.mow.core.api.wiki.WikiImpl;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.rendering.impl.RenderingServiceImpl;
import org.exoplatform.wiki.resolver.PageResolver;
import org.exoplatform.wiki.service.Permission;
import org.exoplatform.wiki.service.PermissionEntry;
import org.exoplatform.wiki.service.PermissionType;
import org.exoplatform.wiki.service.WikiContext;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.impl.SessionManager;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.webui.UIWikiPageEditForm;
import org.exoplatform.wiki.webui.UIWikiPortlet;
import org.exoplatform.wiki.webui.UIWikiRichTextArea;
import org.exoplatform.wiki.webui.WikiMode;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.syntax.Syntax;

public class Utils {  
 
  public static final int DEFAULT_VALUE_UPLOAD_PORTAL = -1;
  
  public static final String SLASH = "/";
  
  public static String upperFirstCharacter(String str) {
    if (StringUtils.isEmpty(str)) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }
  
  public static String getCurrentSpaceName() throws Exception {
    Wiki currentSpace = Utils.getCurrentWiki();
    if (currentSpace == null) {
      return StringUtils.EMPTY;
    }
    return getSpaceName(currentSpace);
  }
  
  public static String getSpaceName(Wiki space) throws Exception {
    if (space instanceof PortalWiki) {
      String displayName = space.getName();
      int slashIndex = displayName.lastIndexOf('/');
      if (slashIndex > -1) {
        displayName = displayName.substring(slashIndex + 1);
      }
      return Utils.upperFirstCharacter(displayName);
    }

    if (space instanceof UserWiki) {
      String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
      if (space.getOwner().equals(currentUser)) {
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        ResourceBundle res = context.getApplicationResourceBundle();
        String mySpaceLabel = res.getString("UISpaceSwitcher.title.my-space");
        return mySpaceLabel;
      }
      return space.getOwner();
    }

    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    return wikiService.getSpaceNameByGroupId(space.getOwner());
  }
  
  public static String getCurrentRequestURL() throws Exception {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    HttpServletRequest request = portalRequestContext.getRequest();
    String requestURL = request.getRequestURL().toString();
    UIPortal uiPortal = Util.getUIPortal();
    String pageNodeSelected = uiPortal.getSelectedUserNode().getURI();
    if (!requestURL.contains(pageNodeSelected)) {
      // Happens at the first time processRender() called when add wiki portlet manually
      requestURL = portalRequestContext.getPortalURI() + pageNodeSelected;
    }      
    return requestURL;
  }

  public static WikiPageParams getCurrentWikiPageParams() throws Exception {
    String requestURL = getCurrentRequestURL();
    PageResolver pageResolver = (PageResolver) PortalContainer.getComponent(PageResolver.class);
    WikiPageParams params = pageResolver.extractWikiPageParams(requestURL, Util.getUIPortal().getSelectedUserNode());
    HttpServletRequest request = Util.getPortalRequestContext().getRequest();
    Map<String, String[]> paramsMap = request.getParameterMap();
    params.setParameters(paramsMap);
    return params;
  }
  
  /**
   * Gets current wiki page directly from data base
   * @return current wiki page
   * @throws Exception
   */
  public static Page getCurrentWikiPage() throws Exception {
    String requestURL = Utils.getCurrentRequestURL();
    PageResolver pageResolver = (PageResolver) PortalContainer.getComponent(PageResolver.class);
    Page page = pageResolver.resolve(requestURL, Util.getUIPortal().getSelectedUserNode());
    return page;
  }
  
  public static boolean canModifyPagePermission() throws Exception {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
    PageImpl currentPage = (PageImpl) Utils.getCurrentWikiPage();
    if (currentPage == null) {
      return false;
    }
    
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
    return ((isPageOwner && hasEditPagePermissionOnPage) || wikiService.hasAdminSpacePermission(wiki.getType(), wiki.getOwner()))
        || wikiService.hasAdminPagePermission(wiki.getType(), wiki.getOwner());
  }
  
  public static boolean canPublicAndRetrictPage() throws Exception {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    PageImpl currentPage = (PageImpl) Utils.getCurrentWikiPage();
    if (currentPage == null) {
      return false;
    }
    Wiki wiki = currentPage.getWiki();
    
    boolean hasEditPagePermissionOnPage = false;
    String[] permissionOfCurrentUser = currentPage.getPermission().get(org.exoplatform.wiki.utils.Utils.getCurrentUser());
    if (permissionOfCurrentUser != null) {
      for (int i = 0; i < permissionOfCurrentUser.length; i++) {
        if (org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY.equals(permissionOfCurrentUser[i])) {
          hasEditPagePermissionOnPage = true;
          break;
        }
      }
    }
    return wikiService.hasAdminSpacePermission(wiki.getType(), wiki.getOwner()) || hasEditPagePermissionOnPage;
  }
  
  public static boolean isCurrentPagePublic() throws Exception {
    Page currentPage = Utils.getCurrentWikiPage();
    return (currentPage != null) && currentPage.hasPermission(PermissionType.EDITPAGE, new Identity(IdentityConstants.ANONIM));
  }
  
  public static String getSpaceHomeURL(String spaceGroupId) {
    String permanentSpaceName = spaceGroupId.split("/")[2];
    NodeURL nodeURL = RequestContext.getCurrentInstance().createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.GROUP, spaceGroupId, permanentSpaceName);
    return nodeURL.setResource(resource).toString();
  }
  
  public static String getURLFromParams(WikiPageParams params) throws Exception {
    if (StringUtils.isEmpty(params.getType()) || StringUtils.isEmpty(params.getOwner())) {
      return StringUtils.EMPTY;
    }
    
    if (params.getType().equals(PortalConfig.GROUP_TYPE)) {
      StringBuilder spaceUrl = new StringBuilder(getSpaceHomeURL(params.getOwner()));
      if (!spaceUrl.toString().endsWith("/")) {
        spaceUrl.append("/");
      }
      spaceUrl.append("wiki/");
      if (!StringUtils.isEmpty(params.getPageId())) {
        spaceUrl.append(params.getPageId());
      }
      return spaceUrl.toString();
    }
    return org.exoplatform.wiki.utils.Utils.getPermanlink(params, false);
  }
  
  public static Page getCurrentNewDraftWikiPage() throws Exception {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    return wikiService.getExsitedOrNewDraftPageById(null, null, org.exoplatform.wiki.utils.Utils.getPageNameForAddingPage());
  }
  
  public static String getExtension(String filename)throws Exception {
    MimeTypeResolver mimeResolver = new MimeTypeResolver() ;
    try{
      return mimeResolver.getExtension(mimeResolver.getMimeType(filename)) ;
    }catch(Exception e) {
      return mimeResolver.getDefaultMimeType() ;
    }
  }
  
  public static Wiki getCurrentWiki() throws Exception {
    MOWService mowService = (MOWService) PortalContainer.getComponent(MOWService.class);
    WikiStoreImpl store = (WikiStoreImpl) mowService.getModel().getWikiStore();
    WikiPageParams params = Utils.getCurrentWikiPageParams();
    if (params != null) {
      String wikiType = params.getType();
      String owner = params.getOwner();
      if (!StringUtils.isEmpty(wikiType) && !StringUtils.isEmpty(owner)) {
        return store.getWiki(WikiType.valueOf(wikiType.toUpperCase()), owner);
      }
    }
    return null;
  }

  public static WikiContext setUpWikiContext(UIWikiPortlet wikiPortlet) throws Exception {
    RenderingService renderingService = (RenderingService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(RenderingService.class);
    Execution ec = ((RenderingServiceImpl) renderingService).getExecution();
    if (ec.getContext() == null) {
      ec.setContext(new ExecutionContext());
    }
    WikiContext wikiContext = createWikiContext(wikiPortlet);
    ec.getContext().setProperty(WikiContext.WIKICONTEXT, wikiContext);
    return wikiContext;
  }
  
  public static void feedDataForWYSIWYGEditor(UIWikiPageEditForm pageEditForm, String markup) throws Exception {
    UIWikiPortlet wikiPortlet = pageEditForm.getAncestorOfType(UIWikiPortlet.class);
    UIWikiRichTextArea richTextArea = pageEditForm.getChild(UIWikiRichTextArea.class);
    RenderingService renderingService = (RenderingService) PortalContainer.getComponent(RenderingService.class);
    HttpSession session = Util.getPortalRequestContext().getRequest().getSession(false);
    UIFormTextAreaInput markupInput = pageEditForm.getUIFormTextAreaInput(UIWikiPageEditForm.FIELD_CONTENT);
    String markupSyntax = getDefaultSyntax();
    WikiContext wikiContext= Utils.setUpWikiContext(wikiPortlet);
    if (markup == null) {
      markup = (markupInput.getValue() == null) ? "" : markupInput.getValue();
    }
    String xhtmlContent = renderingService.render(markup, markupSyntax, Syntax.ANNOTATED_XHTML_1_0.toIdString(), false);
    richTextArea.getUIFormTextAreaInput().setValue(xhtmlContent);
    session.setAttribute(UIWikiRichTextArea.SESSION_KEY, xhtmlContent);
    session.setAttribute(UIWikiRichTextArea.WIKI_CONTEXT, wikiContext);
    SessionManager sessionManager = (SessionManager) ExoContainerContext.getCurrentContainer()
                                                                        .getComponentInstanceOfType(SessionManager.class);
    sessionManager.addSessionContext(ConversationState.getCurrent().getIdentity().getUserId(), 
                                     Utils.createWikiContext(wikiPortlet));
    sessionManager.addSessionContainer(ConversationState.getCurrent().getIdentity().getUserId(), 
                                     sessionManager.getSessionContainer(session.getId()));

  }

  public static String getCurrentWikiPagePath() throws Exception {
    return TreeUtils.getPathFromPageParams(getCurrentWikiPageParams());
  }
  
  public static String getDefaultSyntax() throws Exception {
    String currentDefaultSyntaxt = Utils.getCurrentPreferences().getPreferencesSyntax().getDefaultSyntax();
    if (currentDefaultSyntaxt == null) {
      WikiService wservice = (WikiService) PortalContainer.getComponent(WikiService.class);
      currentDefaultSyntaxt = wservice.getDefaultWikiSyntaxId();
    }
    return currentDefaultSyntaxt;
  }
  
  public static Preferences getCurrentPreferences() throws Exception {
    WikiImpl currentWiki = (WikiImpl) getCurrentWiki();
    return currentWiki.getPreferences();
  }
 
  public static WikiContext createWikiContext(UIWikiPortlet wikiPortlet) throws Exception {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    WikiMode currentMode = wikiPortlet.getWikiMode();
    List<WikiMode> editModes = Arrays.asList(new WikiMode[] { WikiMode.EDITPAGE, WikiMode.ADDPAGE, WikiMode.EDITTEMPLATE,
        WikiMode.ADDTEMPLATE });
    UIPortal uiPortal = Util.getUIPortal();
    String portalURI = portalRequestContext.getPortalURI();
    URL requestURL = new URL(portalRequestContext.getRequest().getRequestURL().toString());
    String domainURL = requestURL.getPath();
    String portalURL = domainURL + portalURI;
    String pageNodeSelected = uiPortal.getSelectedUserNode().getURI();
    String treeRestURL = getCurrentRestURL().concat("/wiki/tree/children/");
    
    WikiContext wikiContext = new WikiContext();
    wikiContext.setPortalURL(portalURL);
    wikiContext.setTreeRestURI(treeRestURL);
    wikiContext.setRestURI(getCurrentRestURL());
    wikiContext.setRedirectURI(wikiPortlet.getRedirectURL());
    wikiContext.setPortletURI(pageNodeSelected);
    WikiPageParams params = Utils.getCurrentWikiPageParams();    
    wikiContext.setType(params.getType());
    wikiContext.setOwner(params.getOwner());
    if (editModes.contains(currentMode)) {
      wikiContext.setSyntax(getDefaultSyntax());
    } else {
      WikiService service = (WikiService) PortalContainer.getComponent(WikiService.class);
      Page currentPage = service.getPageById(params.getType(), params.getOwner(), params.getPageId());
      if (currentPage != null) {
        wikiContext.setSyntax(currentPage.getSyntax());
      }
    }
    if (wikiPortlet.getWikiMode() == WikiMode.ADDPAGE) {
      wikiContext.setPageId(org.exoplatform.wiki.utils.Utils.getPageNameForAddingPage());
    } else {
      wikiContext.setPageId(params.getPageId());
    }
    wikiContext.setBaseUrl(getBaseUrl());
    return wikiContext;
  }
  
  public static String getBaseUrl() throws Exception {
    WikiPageParams params = getCurrentWikiPageParams();
    params.setPageId(null);
    return getURLFromParams(params);
  }
  
  public static String getCurrentWikiNodeUri() throws Exception {    
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    StringBuilder sb = new StringBuilder(portalRequestContext.getPortalURI());
    UIPortal uiPortal = Util.getUIPortal();
    String pageNodeSelected = uiPortal.getSelectedUserNode().getURI();
    sb.append(pageNodeSelected);   
    return sb.toString();
  }

  public static void redirect(WikiPageParams pageParams, WikiMode mode) throws Exception {
    redirect(pageParams, mode, null);
  }

  public static void redirect(WikiPageParams pageParams, WikiMode mode, Map<String, String[]> params) throws Exception {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    portalRequestContext.setResponseComplete(true);
    if (PortalConfig.GROUP_TYPE.equals(Utils.getCurrentWiki().getType())) {
      pageParams.setPageId(URLEncoder.encode(pageParams.getPageId(), "UTF-8"));
    }
    
    portalRequestContext.sendRedirect(createURLWithMode(pageParams, mode, params));
  }
  
  public static void redirect(String url) throws Exception {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    portalRequestContext.setResponseComplete(true);
    portalRequestContext.sendRedirect(url);
  }
  
  public static void ajaxRedirect(Event<? extends UIComponent> event,
                                  WikiPageParams pageParams,
                                  WikiMode mode,
                                  Map<String, String[]> params) throws Exception {
    String redirectLink = Utils.createURLWithMode(pageParams, mode, params);
    ajaxRedirect(event, redirectLink);
  }
  
  public static void ajaxRedirect(Event<? extends UIComponent> event, String redirectLink) throws Exception {
    event.getRequestContext().getJavascriptManager().addCustomizedOnLoadScript("eXo.wiki.UIWikiPortlet.ajaxRedirect('" + redirectLink + "');");
  }
  
  public static String createURLWithMode(WikiPageParams pageParams,
                                         WikiMode mode,
                                         Map<String, String[]> params) throws Exception {
    StringBuffer sb = new StringBuffer();
    sb.append(getPageLink());
    if(!StringUtils.isEmpty(pageParams.getPageId())){
      sb.append(pageParams.getPageId());
    }
    if (!mode.equals(WikiMode.VIEW)) {
      sb.append("#").append(Utils.getActionFromWikiMode(mode));
    }
    if (params != null) {
      Iterator<Entry<String, String[]>> iter = params.entrySet().iterator();
      while (iter.hasNext()) {
        Entry<String, String[]> entry = iter.next();
        sb.append("&");
        sb.append(entry.getKey()).append("=").append(entry.getValue()[0]);
      }
    }
    return sb.toString();
  }

  
  public static String createFormActionLink(UIComponent uiComponent,
                                          String action,
                                          String beanId) throws Exception {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    boolean isForm = UIForm.class.isInstance(uiComponent);
    UIForm form = isForm ? (UIForm) uiComponent : uiComponent.getAncestorOfType(UIForm.class);
    if (form != null) {
      String formId = form.getId();
      if (context instanceof PortletRequestContext) {
        formId = ((PortletRequestContext) context).getWindowId() + "#" + formId;
      }
      StringBuilder b = new StringBuilder();

      b.append("javascript:eXo.wiki.UIForm.submitPageEvent('").append(formId).append("','");
      b.append(action).append("','");
      if (!isForm) {
        b.append("&amp;").append(UIForm.SUBCOMPONENT_ID).append("=").append(uiComponent.getId());
        if (beanId != null) {
          b.append("&amp;").append(UIComponent.OBJECTID).append("=").append(beanId);
        }
      }
      b.append("')");
      return b.toString();
    } else {
      return form.event(action, uiComponent.getId(), action);
    }
  }

  public static String getActionFromWikiMode(WikiMode mode) {
    switch (mode) {
    case EDITPAGE:
      return "EditPage";
    case ADDPAGE:
      return "AddPage";
    case ADDTEMPLATE:
      return "AddTemplate";
    case EDITTEMPLATE:
      return "EditTemplate";
    case SPACESETTING:
      return "SpaceSetting";
    case MYDRAFTS:
      return "MyDrafts";
    default:
      return "";
    }
  }

  public static String getCurrentRestURL() {
    StringBuilder sb = new StringBuilder();
    sb.append("/").append(PortalContainer.getCurrentPortalContainerName()).append("/");
    sb.append(PortalContainer.getCurrentRestContextName());
    return sb.toString();
  }
  
  public static boolean hasPermission(String[] permissions) throws Exception {
    UserACL userACL = Util.getUIPortalApplication().getApplicationComponent(UserACL.class);
    WikiService wikiService = (WikiService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
    List<PermissionEntry> permissionEntries = wikiService.getWikiPermission(pageParams.getType(), pageParams.getOwner());
    ConversationState conversationState = ConversationState.getCurrent();
    Identity user = null;
    if (conversationState != null) {
      user = conversationState.getIdentity();
    } else {
      user = new Identity(IdentityConstants.ANONIM);
    }
    List<AccessControlEntry> aces = new ArrayList<AccessControlEntry>();
    for (PermissionEntry permissionEntry : permissionEntries) {
      Permission[] perms = permissionEntry.getPermissions();
      for (Permission perm : perms) {
        if (perm.isAllowed()) {
          AccessControlEntry ace = new AccessControlEntry(permissionEntry.getId(), perm.getPermissionType().toString());
          aces.add(ace);
        }
      }
    }
    AccessControlList acl = new AccessControlList(userACL.getSuperUser(), aces);
    return org.exoplatform.wiki.utils.Utils.hasPermission(acl, permissions, user);
  }
  
  public static WikiMode getModeFromAction(String actionParam) {
    String[] params = actionParam.split(WikiConstants.WITH);
    String name = params[0];
    if (name != null) {
      try {
        WikiMode mode = WikiMode.valueOf(name.toUpperCase());
        if (mode != null)
          return mode;
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
    return null;
  }
  
  /**
   * render macro to XHtml string.
   * @param uiComponent - component that contain the macro.
   * @param macroName - name of macro
   * @param wikiSyntax - wiki syntax referred from {@link Syntax}
   * @return String in format {@link Syntax#XHTML_1_0}
   */
  public static String renderMacroToXHtml(UIComponent uiComponent, String macroName, String wikiSyntax) {
    try {
      RenderingService renderingService = (RenderingService) PortalContainer.getComponent(RenderingService.class);
      setUpWikiContext(uiComponent.getAncestorOfType(UIWikiPortlet.class));
      String content= renderingService.render(macroName,
                                     wikiSyntax,
                                     Syntax.XHTML_1_0.toIdString(),
                                     false);      
      return content;
    } catch (Exception e) {
      return "";
    }
  }

  public static void removeWikiContext() throws Exception {
    RenderingService renderingService = (RenderingService) PortalContainer.getComponent(RenderingService.class);
    Execution ec = ((RenderingServiceImpl) renderingService).getExecution();
    if (ec != null) {
      ec.removeContext();
    }
  }
  
  public static List<NTVersion> getCurrentPageRevisions() throws Exception {
    return org.exoplatform.wiki.utils.Utils.getCurrentPageRevisions((PageImpl) getCurrentWikiPage());
  }
  
  public static int getLimitUploadSize() {
    PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    int limitMB = DEFAULT_VALUE_UPLOAD_PORTAL;
    try {
      limitMB = Integer.parseInt(portletPref.getValue("uploadFileSizeLimitMB", "").trim());
    } catch (Exception e) {
      limitMB = 10;
    }
    return limitMB;
  }
  
  public static String getFullName(String userId) {
    try {
      OrganizationService organizationService = (OrganizationService) PortalContainer.getComponent(OrganizationService.class);
      User user = organizationService.getUserHandler().findUserByName(userId);
      return user.getFullName();
    } catch (Exception e) {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      ResourceBundle res = context.getApplicationResourceBundle();
      return res.getString("UIWikiPortlet.label.Anonymous");
    }
  }
  
  public static String getPageLink() throws Exception {    
    StringBuilder sb = new StringBuilder();    
    sb.append(Utils.getBaseUrl());
     
    String pageURI = Util.getUIPortal().getSelectedUserNode().getURI();    
    String pageName = Util.getUIPortal().getSelectedUserNode().getName();
    if(!WikiContext.WIKI.equals(pageName)) {
      if(pageURI.contains(WikiContext.WIKI)) {
        pageURI = pageURI.substring(pageURI.indexOf(WikiContext.WIKI) + WikiContext.WIKI.length() + 1, pageURI.length());
      }
      sb.append(pageURI).append("/");
    } 
    return sb.toString();
  }
}
