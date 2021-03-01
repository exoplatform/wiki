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
import java.util.*;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.core.space.SpaceApplication;
import org.exoplatform.social.core.space.SpaceTemplate;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.space.spi.SpaceTemplateService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.resolver.PageResolver;
import org.exoplatform.wiki.service.WikiContext;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.impl.SessionManager;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.webui.UIWikiPageEditForm;
import org.exoplatform.wiki.webui.UIWikiPortlet;
import org.exoplatform.wiki.webui.UIWikiRichTextArea;
import org.exoplatform.wiki.webui.WikiMode;

public class Utils {

  public static final int    DEFAULT_VALUE_UPLOAD_PORTAL = -1;

  public static final String SLASH                       = "/";

  public static final String DRAFT_ID                    = "draftId";

  public static final String WIKI_MODE                    = "wikiMode";

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

  public static String getSpaceName(Wiki wiki) throws Exception {
    WikiType wikiType = WikiType.valueOf(wiki.getType().toUpperCase());
    if (WikiType.PORTAL.equals(wikiType)) {
      String displayName = wiki.getOwner();
      int slashIndex = displayName.lastIndexOf('/');
      if (slashIndex > -1) {
        displayName = displayName.substring(slashIndex + 1);
      }
      return Utils.upperFirstCharacter(displayName);
    }

    if (WikiType.USER.equals(wikiType)) {
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

  public static String getCurrentRequestURL() throws Exception {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    HttpServletRequest request = portalRequestContext.getRequest();
    String requestURL = java.net.URLDecoder.decode(request.getRequestURL().toString(), "UTF-8");
    UIPortal uiPortal = Util.getUIPortal();
    String pageNodeSelected = uiPortal.getSelectedUserNode().getURI();
    if (!requestURL.contains(pageNodeSelected)) {
      // Happens at the first time processRender() called when add wiki portlet
      // manually
      requestURL = portalRequestContext.getPortalURI() + pageNodeSelected;
    }
    return requestURL;
  }

  public static WikiPageParams getCurrentWikiPageParams() throws Exception {
    String requestURL = getCurrentRequestURL();
    PageResolver pageResolver = (PageResolver) PortalContainer.getComponent(PageResolver.class);
    UIPortal uiPortal = Util.getUIPortal();
    WikiPageParams params = pageResolver.extractWikiPageParams(requestURL, uiPortal.getSiteKey(), uiPortal.getSelectedUserNode());
    HttpServletRequest request = Util.getPortalRequestContext().getRequest();
    Map<String, String[]> paramsMap = request.getParameterMap();
    params.setParameters(paramsMap);
    return params;
  }

  /**
   * Gets current wiki page directly from data base
   * 
   * @return current wiki page
   * @throws Exception
   */
  public static Page getCurrentWikiPage() throws Exception {
    String requestURL = Utils.getCurrentRequestURL();
    PageResolver pageResolver = (PageResolver) PortalContainer.getComponent(PageResolver.class);
    UIPortal uiPortal = Util.getUIPortal();
    return pageResolver.resolve(requestURL, uiPortal.getSiteKey(), uiPortal.getSelectedUserNode());
  }

  public static boolean canModifyPagePermission() throws Exception {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
    Page currentPage = Utils.getCurrentWikiPage();
    if (currentPage == null) {
      return false;
    }
    return wikiService.canModifyPagePermission(currentPage, currentUser);
  }

  public static boolean isPagePublic(Page page) throws Exception {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    return (page != null)
        && wikiService.hasPermissionOnPage(page, PermissionType.VIEWPAGE, new Identity(IdentityConstants.ANONIM));
  }

  public static boolean isCurrentPagePublic() throws Exception {
    Page currentPage = Utils.getCurrentWikiPage();
    return isPagePublic(currentPage);
  }

  public static String getSpaceHomeURL(String spaceGroupId) {
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    Space space = spaceService.getSpaceByGroupId(spaceGroupId);
    String spaceLink = org.exoplatform.social.webui.Utils.getSpaceHomeURL(space);
    return spaceLink;
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
      spaceUrl.append(getWikiAppNameInSpace(params.getOwner())).append("/");
      if (!StringUtils.isEmpty(params.getPageName())) {
        spaceUrl.append(params.getPageName());
      }
      return spaceUrl.toString();
    }
    return org.exoplatform.wiki.utils.Utils.getPermanlink(params, false);
  }

  private static String getWikiAppNameInSpace(String spaceId) {
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    Space space = spaceService.getSpaceByGroupId(spaceId);
    SpaceTemplateService spaceTemplateService = CommonsUtils.getService(SpaceTemplateService.class);
    SpaceTemplate spaceTemplate = spaceTemplateService.getSpaceTemplateByName(space.getTemplate());
    List<SpaceApplication> spaceTemplateApplications = spaceTemplate.getSpaceApplicationList();
    if (spaceTemplateApplications != null) {
      for (SpaceApplication spaceApplication : spaceTemplateApplications) {
        if("WikiPortlet".equals(spaceApplication.getPortletName())){
          return spaceApplication.getUri();
        }
      }
    }
    return "WikiPortlet";
  }

  public static Page getCurrentNewDraftWikiPage() throws Exception {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    return wikiService.getExsitedOrNewDraftPageById(null, null, org.exoplatform.wiki.utils.Utils.getPageNameForAddingPage());
  }

  public static String getExtension(String filename) throws Exception {
    MimeTypeResolver mimeResolver = new MimeTypeResolver();
    try {
      return mimeResolver.getExtension(mimeResolver.getMimeType(filename));
    } catch (Exception e) {
      return mimeResolver.getDefaultMimeType();
    }
  }

  public static Wiki getCurrentWiki() throws Exception {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    WikiPageParams params = Utils.getCurrentWikiPageParams();
    return wikiService.getWikiByTypeAndOwner(params.getType(), params.getOwner());
  }

  public static void feedDataForWYSIWYGEditor(UIWikiPageEditForm pageEditForm, String markup) throws Exception {
    UIWikiPortlet wikiPortlet = pageEditForm.getAncestorOfType(UIWikiPortlet.class);
    UIWikiRichTextArea richTextArea = pageEditForm.getChild(UIWikiRichTextArea.class);
    HttpSession session = Util.getPortalRequestContext().getRequest().getSession(false);
    UIFormTextAreaInput markupInput = pageEditForm.getUIFormTextAreaInput(UIWikiPageEditForm.FIELD_CONTENT);
    if (markup == null) {
      markup = (markupInput.getValue() == null) ? "" : markupInput.getValue();
    }
    String xhtmlContent = markup;
    richTextArea.getUIFormTextAreaInput().setValue(xhtmlContent);
    session.setAttribute(UIWikiRichTextArea.SESSION_KEY, xhtmlContent);
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    SessionManager sessionManager = (SessionManager) container.getComponentInstanceOfType(SessionManager.class);
    sessionManager.addSessionContext(session.getId(), Utils.createWikiContext(wikiPortlet));

    sessionManager.addSessionContext(ConversationState.getCurrent().getIdentity().getUserId(),
                                     Utils.createWikiContext(wikiPortlet));
    if (sessionManager.getSessionContainer(session.getId()) != null) {
      sessionManager.addSessionContainer(ConversationState.getCurrent().getIdentity().getUserId(),
                                         sessionManager.getSessionContainer(session.getId()));
    }

  }

  public static String getCurrentWikiPagePath() throws Exception {
    return TreeUtils.getPathFromPageParams(getCurrentWikiPageParams());
  }

  public static String getDefaultSyntax() {
    WikiService wservice = ExoContainerContext.getService(WikiService.class);
    return wservice.getDefaultWikiSyntaxId();
  }

  public static WikiPreferences getCurrentPreferences() throws Exception {
    Wiki currentWiki = getCurrentWiki();
    return currentWiki.getPreferences();
  }

  public static WikiContext createWikiContext(UIWikiPortlet wikiPortlet) throws Exception {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    WikiMode currentMode = getWikiMode();
    List<WikiMode> editModes = Arrays.asList(WikiMode.EDITPAGE, WikiMode.ADDPAGE, WikiMode.EDITTEMPLATE,
            WikiMode.ADDTEMPLATE);
    UIPortal uiPortal = Util.getUIPortal();
    String portalURI = portalRequestContext.getPortalURI();
    URL requestURL = new URL(portalRequestContext.getRequest().getRequestURL().toString());
    String domainURL = requestURL.getProtocol() + "://" + requestURL.getAuthority();
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
      Page currentPage = service.getPageOfWikiByName(params.getType(), params.getOwner(), params.getPageName());
      if (currentPage != null) {
        wikiContext.setSyntax(currentPage.getSyntax());
      }
    }
    if (currentMode == WikiMode.ADDPAGE) {
      wikiContext.setPageName(org.exoplatform.wiki.utils.Utils.getPageNameForAddingPage());
    } else {
      wikiContext.setPageName(params.getPageName());
    }
    wikiContext.setBaseUrl(getBaseUrl());
    return wikiContext;
  }

  public static String getBaseUrl() throws Exception {
    WikiPageParams params = getCurrentWikiPageParams();
    params.setPageName(null);
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

  /**
   * Get the full path for current wiki page
   */
  public static String getPageLink() throws Exception {
    WikiPageParams params = getCurrentWikiPageParams();
    params.setPageName(null);
    if (PortalConfig.PORTAL_TYPE.equals(params.getType())) {
      String navigationURI = Util.getUIPortal().getNavPath().getURI();
      String requestURI = Util.getPortalRequestContext().getRequestURI();
      if (requestURI.indexOf(navigationURI) < 0) {
        navigationURI = "wiki";
      }
      return requestURI.substring(0, requestURI.indexOf(navigationURI) + navigationURI.length()) + "/";
    }
    return getURLFromParams(params);
  }

  public static void redirect(WikiPageParams pageParams, WikiMode mode, Map<String, String[]> params) throws Exception {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    portalRequestContext.setResponseComplete(true);
    if (PortalConfig.GROUP_TYPE.equals(Utils.getCurrentWiki().getType())) {
      pageParams.setPageName(URLEncoder.encode(pageParams.getPageName(), "UTF-8"));
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
    event.getRequestContext()
         .getJavascriptManager()
         .addCustomizedOnLoadScript("eXo.wiki.UIWikiPortlet.ajaxRedirect('" + redirectLink + "');");
  }

  public static String createURLWithMode(WikiPageParams pageParams, WikiMode mode, Map<String, String[]> params) throws Exception {
    StringBuffer sb = new StringBuffer();
    sb.append(getURLFromParams(pageParams));
    // sb.append(getPageLink());
    // if(!StringUtils.isEmpty(pageParams.getPageName())){
    // sb.append(URLEncoder.encode(pageParams.getPageName(), "UTF-8"));
    // }
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

  public static String createFormActionLink(UIComponent uiComponent, String action, String beanId) throws Exception {
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
        b.append("&amp;").append(WIKI_MODE).append("=").append(getWikiMode());
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

  public static int getLimitUploadSize() {
    WikiService wikiService = CommonsUtils.getService(WikiService.class);
    return wikiService.getUploadLimit();
  }

  public static String getFullName(String userId) {
    try {
      OrganizationService organizationService = (OrganizationService) PortalContainer.getComponent(OrganizationService.class);
      User user = organizationService.getUserHandler().findUserByName(userId, UserStatus.ANY);
      return user.getFullName();
    } catch (Exception e) {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      ResourceBundle res = context.getApplicationResourceBundle();
      return res.getString("UIWikiPortlet.label.Anonymous");
    }
  }

  public static String getDraftIdSessionKey() {
    return ConversationState.getCurrent().getIdentity().getUserId() + DRAFT_ID;
  }

  public static String getWikiTypeFromWikiId(String wikiId) {
    String wikiType = "";
    if (wikiId.startsWith("/spaces/")) {
      wikiType = PortalConfig.GROUP_TYPE;
    } else if (wikiId.startsWith("/user/")) {
      wikiType = PortalConfig.USER_TYPE;
    } else {
      if (wikiId.startsWith("/")) {
        wikiType = PortalConfig.PORTAL_TYPE;
      }
    }
    return wikiType;
  }

  public static String getWikiOwnerFromWikiId(String wikiId) {
    String wikiType = getWikiTypeFromWikiId(wikiId);
    if (PortalConfig.USER_TYPE.equals(wikiType) || PortalConfig.PORTAL_TYPE.equals(wikiType)) {
      wikiId = wikiId.substring(wikiId.lastIndexOf('/') + 1);
    }
    return wikiId;
  }

  public static WikiMode getWikiMode() {
    String wikiMode = Util.getPortalRequestContext().getRequestParameter(WIKI_MODE);
    return StringUtils.isBlank(wikiMode) ? WikiMode.VIEW : WikiMode.valueOf(wikiMode.toUpperCase());
  }
}
