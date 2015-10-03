package org.exoplatform.wiki.utils;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.mow.core.api.wiki.WikiNodeType;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.service.*;
import org.exoplatform.wiki.service.Permission;
import org.exoplatform.wiki.service.diff.DiffResult;
import org.exoplatform.wiki.service.diff.DiffService;
import org.exoplatform.wiki.service.impl.WikiPageHistory;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.xwiki.rendering.syntax.Syntax;

import javax.jcr.RepositoryException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.*;
import java.util.Map.Entry;

public class Utils {
  public static final String SLASH = "SLASH";
  
  public static final String DOT = "DOT";
  
  public static final String  SPACE                       = "space";

  public static final String  PAGE                        = "page";
  
  private static final Log      log_               = ExoLogger.getLogger(Utils.class);
  
  private static final String JCR_WEBDAV_SERVICE_BASE_URI = "/jcr";
  
  public static final String COMPARE_REVISION = "CompareRevision";
  
  public static final String VER_NAME = "verName";

  final private static String MIMETYPE_TEXTHTML = "text/html";

  private static Map<String, Map<String, WikiPageHistory>> editPageLogs = new HashMap<String, Map<String, WikiPageHistory>>();
  
  public static final String WIKI_RESOUCE_BUNDLE_NAME = "locale.wiki.service.WikiService";
  
  private static final String ILLEGAL_SEARCH_CHARACTERS= "\\!^()+{}[]:-\"";
  
  private static final String ILLEGAL_JCR_NAME_CHARACTERS = "*|\":[]/'"; 

  public static final String SPLIT_TEXT_OF_DRAFT_FOR_NEW_PAGE = "_A_A_";
  
  public static String escapeIllegalCharacterInQuery(String query) {
    String ret = query;
    if (ret != null) {
      for (char c : ILLEGAL_SEARCH_CHARACTERS.toCharArray()) {
        ret = ret.replace(c + "", "\\" + c);
      }
      ret = ret.replace("'", "''");
    }
    return ret;
  }
  
  public static String escapeIllegalCharacterInName(String name) {
    if (name == null) return null;
    else if (".".equals(name)) return "_";
    else {
      int first = name.indexOf('.');
      int last = name.lastIndexOf('.');
      //if only 1 dot character
      if (first != -1 && first == last && ( first == 0 || last == name.length() - 1)) {
        name = name.replace('.', '_');
      } 
      for (char c : ILLEGAL_JCR_NAME_CHARACTERS.toCharArray())
        name = name.replace(c, '_');
      name = name.replace("%20", "_");
      return name;
    }
  }
  
  public static String getPortalName() {
    return org.exoplatform.wiki.rendering.util.Utils.getPortalName();
  }
  
  /**
   * Get resource bundle from given resource file
   *
   * @param key key
   * @param cl ClassLoader to load resource file
   * @return The value of key in resource bundle
   */
  public static String getWikiResourceBundle(String key, ClassLoader cl) {
    Locale locale = WebuiRequestContext.getCurrentInstance().getLocale();
    ResourceBundle resourceBundle = ResourceBundle.getBundle(WIKI_RESOUCE_BUNDLE_NAME, locale,cl);
    return resourceBundle.getString(key);
  }
  
  /**
   * Log the edit page action of user
   * 
   * @param pageParams The page that has been editing
   * @param username The name of user that editing wiki page
   * @param updateTime The time that this page is edited
   * @param draftName The name of draft for this edit
   * @param isNewPage Is the wiki page a draft or not
   */
  public static void logEditPageTime(WikiPageParams pageParams, String username, long updateTime, String draftName, boolean isNewPage) {
    String pageId = pageParams.getPageId();
    Map<String, WikiPageHistory> logByPage = editPageLogs.get(pageId);
    if (logByPage == null) {
      logByPage = new HashMap<String, WikiPageHistory>();
      editPageLogs.put(pageId, logByPage);
    }
    WikiPageHistory logByUsername = logByPage.get(username);
    if (logByUsername == null) {
      logByUsername = new WikiPageHistory(pageParams, username, draftName, isNewPage);
      logByPage.put(username, logByUsername);
    }
    logByUsername.setEditTime(updateTime);
  }
  
  /**
   * removes the log of user editing page.
   * @param pageParams
   * @param user
   */
  public static void removeLogEditPage(WikiPageParams pageParams, String user) {
    String pageId = pageParams.getPageId();
    Map<String, WikiPageHistory> logByPage = editPageLogs.get(pageId);
    if (logByPage != null) {
      logByPage.remove(user);
    }
  }
  
  /**
   * Get the list of user that're editing the wiki page
   * 
   * @param pageId The id of wiki page
   * @return The list of user that're editing this wiki page 
   */
  public static List<String> getListOfUserEditingPage(String pageId) {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    List<String> edittingUsers = new ArrayList<String>();
    List<String> outdateEdittingUser = new ArrayList<String>();
    String currentUser = getCurrentUser();
    
    Map<String, WikiPageHistory> logByPage = editPageLogs.get(pageId);
    if (logByPage != null) {
      // Find all the user that editting this page
      for (String username : logByPage.keySet()) {
        WikiPageHistory log = logByPage.get(username);
        if (System.currentTimeMillis() - log.getEditTime() < wikiService.getEditPageLivingTime()) {
          if (!username.equals(currentUser) && !log.isNewPage()) {
            edittingUsers.add(username);
          }
        } else {
          outdateEdittingUser.add(username);
        }
      }
      
      // Remove all outdate editting user
      for (String username : outdateEdittingUser) {
        logByPage.remove(username);
      }
    }
    return edittingUsers;
  }
  
  /**
   * Get the permalink of current wiki page <br>
   * 
   * <ul>With the current page param:</ul>
   *   <li>type = "group"</li>
   *   <li>owner = "spaces/test_space"</li>
   *   <li>pageId = "test_page"</li>
   * <br>
   *  
   * <ul>The permalink will be: </ul>
   * <li>http://int.exoplatform.org/portal/intranet/wiki/group/spaces/test_space/test_page</li>
   * <br>
   * 
   * @return The permalink of current wiki page
   * @throws Exception
   */
  public static String getPermanlink(WikiPageParams params, boolean hasDowmainUrl) throws Exception {
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    
    // get wiki webapp name
    String wikiWebappUri = wikiService.getWikiWebappUri();
    
    // Create permalink
    StringBuilder sb = new StringBuilder(wikiWebappUri);
    sb.append("/");
    if (!params.getType().equalsIgnoreCase(WikiType.PORTAL.toString())) {
      sb.append(params.getType().toLowerCase());
      sb.append("/");
      sb.append(org.exoplatform.wiki.utils.Utils.validateWikiOwner(params.getType(), params.getOwner()));
      sb.append("/");
    }
    
    if (params.getPageId() != null) {
      sb.append(params.getPageId());
    }
    
    if (hasDowmainUrl) {
      return getDomainUrl() + fillPortalName(sb.toString());
    }
    return fillPortalName(sb.toString());
  }
  
  public static String getPageNameForAddingPage() {
    String sessionId = Util.getPortalRequestContext().getRequest().getSession(false).getId();
    String username = org.exoplatform.wiki.utils.Utils.getCurrentUser();
    return username + SPLIT_TEXT_OF_DRAFT_FOR_NEW_PAGE + sessionId;
  }
  
  private static String getDomainUrl() {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    StringBuilder domainUrl = new StringBuilder();
    domainUrl.append(portalRequestContext.getRequest().getScheme());
    domainUrl.append("://");
    domainUrl.append(portalRequestContext.getRequest().getServerName());
    int port = portalRequestContext.getRequest().getServerPort();
    if (port != 80) {
      domainUrl.append(":");
      domainUrl.append(port);
    }
    return domainUrl.toString();
  }
  
  private static String fillPortalName(String url) {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL =  ctx.createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.PORTAL, Util.getPortalRequestContext().getPortalOwner(), url);
    return nodeURL.setResource(resource).toString(); 
  }

  /**
   * Get the editting log of wiki page
   * 
   * @param pageId The id of wiki page to get log
   * @return The editting log of wiki pgae
   */
  public static Map<String, WikiPageHistory> getLogOfPage(String pageId) {
    Map<String, WikiPageHistory> logByPage = editPageLogs.get(pageId);
    if (logByPage == null) {
      logByPage = new HashMap<String, WikiPageHistory>();
    }
    return logByPage;
  }
   

  //The path should get from NodeHierarchyCreator 
  public static String getPortalWikisPath() {    
    String path = "/exo:applications/" 
    + WikiNodeType.Definition.WIKI_APPLICATION + "/"
    + WikiNodeType.Definition.WIKIS ; 
    return path ;
  }
  /**
   * @return 
   *      <li> portal name if wiki is portal type</li>
   *      <li> groupid if wiki is group type</li>
   *      <li> userid if wiki is personal type</li>
   * @throws IllegalArgumentException if jcr path is not of a wiki page node.
   */
  public static String getSpaceIdByJcrPath(String jcrPath) throws IllegalArgumentException {
    String wikiType = getWikiType(jcrPath);
    if (PortalConfig.PORTAL_TYPE.equals(wikiType)) {
      return getPortalIdByJcrPath(jcrPath);
    } else if (PortalConfig.GROUP_TYPE.equals(wikiType)) {
      return getGroupIdByJcrPath(jcrPath);
    } else if (PortalConfig.USER_TYPE.equals(wikiType)) {
      return getUserIdByJcrPath(jcrPath);
    } else {
      throw new IllegalArgumentException(jcrPath + " is not jcr path of a wiki page node!");
    }
  }
  
  /**
   * @param jcrPath follows the format /Groups/$GROUP/ApplicationData/eXoWiki/[wikipage]
   * @return $GROUP of jcrPath
   * @throws IllegalArgumentException if jcrPath is not as expected.
   */
  public static String getGroupIdByJcrPath(String jcrPath) throws IllegalArgumentException {
    int pos1 = jcrPath.indexOf("/Groups/");
    int pos2 = jcrPath.indexOf("/ApplicationData");
    if (pos1 >= 0 && pos2 > 0) {
      return jcrPath.substring(pos1 + "/Groups/".length(), pos2);
    } else {
      throw new IllegalArgumentException(jcrPath + " is not jcr path of a group wiki page node!");
    }
  }
  
  public static String getRepositoryName(){
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    try {
      return ((RepositoryService)container.getComponentInstanceOfType(RepositoryService.class))
          .getCurrentRepository().getConfiguration().getName();
    } catch (RepositoryException e) {
      if (log_.isDebugEnabled()) {
        log_.debug(String.format("Failed to get Repository name"), e);
      }
    }
    return "";
  }
  
  /**
   * @param jcrPath follows the format /Users/$USERNAME/ApplicationData/eXoWiki/...
   * @return $USERNAME of jcrPath
   * @throws IllegalArgumentException if jcrPath is not as expected.
   */
  public static String getUserIdByJcrPath(String jcrPath) throws IllegalArgumentException {
    int pos1 = jcrPath.indexOf("/Users/");
    int pos2 = jcrPath.indexOf("/ApplicationData");
    if (pos1 >= 0 && pos2 > 0) {
      return jcrPath.substring(pos1 + "/Users/".length(), pos2);
    } else {
      throw new IllegalArgumentException(jcrPath + " is not jcr path of a personal wiki page node!");
    }
  }
  
  /**
   * @param jcrPath follows the format /exo:applications/eXoWiki/wikis/$PORTAL/...
   * @return $PORTAL of jcrPath
   * @throws IllegalArgumentException if jcrPath is not as expected.
   */
  public static String getPortalIdByJcrPath(String jcrPath) throws IllegalArgumentException {
    String portalPath = getPortalWikisPath();
    int pos1 = jcrPath.indexOf(portalPath);
    
    if (pos1 >= 0) {
      String restPath = jcrPath.substring(pos1 + portalPath.length() + 1);
      return restPath.substring(0, restPath.indexOf("/"));
    } else {
      throw new IllegalArgumentException(jcrPath + " is not jcr path of a portal wiki page node!");
    }
  }
  
  /**
   * @param jcrPath absolute jcr path of page node.
   * @return type of wiki page. 
   */
  public static String getWikiType(String jcrPath) throws IllegalArgumentException {
    if (jcrPath.startsWith("/exo:applications/")) {
      return PortalConfig.PORTAL_TYPE;
    } else if (jcrPath.startsWith("/Groups/")) {
      return PortalConfig.GROUP_TYPE;
    } else if (jcrPath.startsWith("/Users/")) {
      return PortalConfig.USER_TYPE;
    } else {
      throw new IllegalArgumentException(jcrPath + " is not jcr path of a wiki page node!");
    }
  }
  
  /**
   * Validate {@code wikiOwner} depending on {@code wikiType}. <br>
   * If wikiType is {@link PortalConfig#GROUP_TYPE}, {@code wikiOwner} is checked to removed slashes at the begin and the end point of it.
   * @param wikiType
   * @param wikiOwner
   * @return wikiOwner after validated.
   */ 
  public static String validateWikiOwner(String wikiType, String wikiOwner){
    if(wikiType != null && wikiType.equals(PortalConfig.GROUP_TYPE) && StringUtils.isNotEmpty(wikiOwner)) {
      if(wikiOwner.startsWith("/")){
        wikiOwner = wikiOwner.substring(1,wikiOwner.length());
      }
      if(wikiOwner.endsWith("/")){
        wikiOwner = wikiOwner.substring(0,wikiOwner.length()-1);
      }
    }
    return wikiOwner;
  }
  
  public static String getDefaultRestBaseURI() {
    StringBuilder sb = new StringBuilder();
    sb.append("/");
    sb.append(PortalContainer.getCurrentPortalContainerName());
    sb.append("/");
    sb.append(PortalContainer.getCurrentRestContextName());
    return sb.toString();
  }

  public static String getCurrentRepositoryWebDavUri() {
    StringBuilder sb = new StringBuilder();
    sb.append(getDefaultRestBaseURI());
    sb.append(JCR_WEBDAV_SERVICE_BASE_URI);
    sb.append("/");
    RepositoryService repositoryService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(RepositoryService.class);
    try {
      sb.append(repositoryService.getCurrentRepository().getConfiguration().getName());
    } catch (RepositoryException e) {
      sb.append(repositoryService.getConfig().getDefaultRepositoryName());
    }
    sb.append("/");
    return sb.toString();
  }
  
  public static String getDocumentURL(WikiContext wikiContext) {
    if (wikiContext.getPortalURL() == null && wikiContext.getPortletURI() == null) {
      return wikiContext.getPageId();
    }
    StringBuilder sb = new StringBuilder();
    sb.append(wikiContext.getPortalURL());
    sb.append(wikiContext.getPortletURI());
    sb.append("/");
    if (!PortalConfig.PORTAL_TYPE.equalsIgnoreCase(wikiContext.getType())) {
      sb.append(wikiContext.getType().toLowerCase());
      sb.append("/");
      sb.append(Utils.validateWikiOwner(wikiContext.getType(), wikiContext.getOwner()));
      sb.append("/");
    }
    sb.append(wikiContext.getPageId());
    return sb.toString();
  }
  
  public static String getCurrentUser() {
    ConversationState conversationState = ConversationState.getCurrent();
    if (conversationState != null) {
      return ConversationState.getCurrent().getIdentity().getUserId();
    }
    return null; 
  }
  
  public static Collection<Wiki> getWikisByType(WikiType wikiType) {
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    // TODO need getAllWikis
    //return store.getWikiContainer(wikiType).getAllWikis();
    return Collections.EMPTY_LIST;
  }
  
  public static Wiki getWiki(WikiPageParams params) {
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    if (params != null) {
      String wikiType = params.getType();
      String owner = params.getOwner();
      if (!StringUtils.isEmpty(wikiType) && !StringUtils.isEmpty(owner)) {
        try {
          return wikiService.getWikiByTypeAndOwner(wikiType, owner);
        } catch (Exception e) {
          // TODO Log and Exception
        }
      }
    }
    return null;
  }
  
  public static Wiki[] getAllWikiSpace() throws WikiException {
    MOWService mowService = (MOWService) PortalContainer.getComponent(MOWService.class);
    WikiStoreImpl store = (WikiStoreImpl) mowService.getModel().getWikiStore();
    return store.getWikis().toArray(new Wiki[]{}) ;
  } 
  
  public static boolean isDescendantPage(Page page, Page parentPage) {
    return page.getPath().startsWith(parentPage.getPath());
  }

  public static Object getObject(String path, String type) throws WikiException {
    WikiService wservice = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    return wservice.findByPath(path, type) ;
  }
  
  public static Object getObjectFromParams(WikiPageParams param) throws WikiException {
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    String wikiType = param.getType();
    String wikiOwner = param.getOwner();
    String wikiPageId = param.getPageId();

    if (wikiOwner != null && wikiPageId != null) {
      if (!wikiPageId.equals(WikiConstants.WIKI_HOME_NAME)) {
        // Object is a page
        Page expandPage = wikiService.getPageByRootPermission(wikiType, wikiOwner, wikiPageId);
        return expandPage;
      } else {
        // Object is a wiki home page
        Wiki wiki = wikiService.getWikiByTypeAndOwner(wikiType.toUpperCase(), wikiOwner);
        Page wikiHome = wiki.getWikiHome();
        return wikiHome;
      }
    } else if (wikiOwner != null) {
      // Object is a wiki
      Wiki wiki =  wikiService.getWikiByTypeAndOwner(wikiType.toUpperCase(), wikiOwner);
      return wiki;
    } else if (wikiType != null) {
      // Object is a space
      return wikiType;
    } else {
      return null;
    }
  }
  
  public static Stack<WikiPageParams> getStackParams(Page page) throws WikiException {
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    Stack<WikiPageParams> stack = new Stack<>();
    Wiki wiki = wikiService.getWikiByTypeAndOwner(page.getWikiType(), page.getWikiOwner());
    if (wiki != null) {
      while (page != null) {
        stack.push(new WikiPageParams(wiki.getType(), wiki.getOwner(), page.getName()));
        page = wikiService.getParentPageOf(page);
      }      
    }
    return stack;
  }
  
  
  public static WikiPageParams getWikiPageParams(Page page) {
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    try {
      Wiki wiki = wikiService.getWikiByTypeAndOwner(page.getWikiType(), page.getWikiOwner());
      String wikiType = wiki.getType();
      WikiPageParams params = new WikiPageParams(wikiType, wiki.getOwner(), page.getName());
      return params;
    } catch(Exception e) {
      // TODO Log exception
      return null;
    }
  }
  
  public static void sendMailOnChangeContent(Attachment content) throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    WikiService wikiService = container.getComponentInstanceOfType(WikiService.class);
    DiffService diffService = container.getComponentInstanceOfType(DiffService.class);
    RenderingService renderingService = container.getComponentInstanceOfType(RenderingService.class);
    Message message = new Message();
    ConversationState conversationState = ConversationState.getCurrent();
    // Get author
    String author = conversationState.getIdentity().getUserId();

    // Get watchers' mails
    Page page = wikiService.getPageOfAttachment(content);
    // TODO need page watchers
    //List<String> list = page.getWatchedMixin().getWatchers();
    List<String> list = Collections.EMPTY_LIST;
            List<String> emailList = new ArrayList<String>();
    for (int i = 0; i < list.size(); i++) {
      if (isEnabledUser(list.get(i))) {
        emailList.add(getEmailUser(list.get(i)));
      }
    }   
    
    // Get differences
    String pageTitle = page.getTitle();
    String currentVersionContent = new String(content.getContent());
    List<PageVersion> versions = wikiService.getVersionsOfPage(page);
    String previousVersionContent = StringUtils.EMPTY;
    if(versions != null && !versions.isEmpty()) {
      PageVersion previousVersion = versions.get(0);
      previousVersionContent = previousVersion.getContent();
    }
    DiffResult diffResult = diffService.getDifferencesAsHTML(previousVersionContent,
                                                             currentVersionContent,
                                                             false);
    String fullContent = renderingService.render(currentVersionContent,
                                                 page.getSyntax(),
                                                 Syntax.XHTML_1_0.toIdString(),
                                                 false);
    
    if (diffResult.getChanges() == 0) {
      diffResult.setDiffHTML("No changes, new revision is created.");
    } 
    
    StringBuilder sbt = new StringBuilder();
    sbt.append("<html>")
       .append("  <head>")
       .append("     <link rel=\"stylesheet\" href=\""+renderingService.getCssURL() +"\" type=\"text/css\">")
       .append("  </head>")
       .append("  <body>")
       .append("    Page <a href=\""+CommonsUtils.getCurrentDomain()+page.getUrl()+"\">" + page.getTitle() +"</a> is modified by " +page.getAuthor())
       .append("    <br/><br/>")
       .append("    Changes("+ diffResult.getChanges()+")")
       .append("    <br/><br/>")
       .append(     insertStyle(diffResult.getDiffHTML()))
       .append("    Full content: ")
       .append("    <br/><br/>")
       .append(     fullContent)
       .append("  </body>")
       .append("</html>");
    // Create message
    message.setFrom(makeNotificationSender(author));
    message.setSubject("\"" + pageTitle + "\" page was modified");
    message.setMimeType(MIMETYPE_TEXTHTML);
    message.setBody(sbt.toString());
    MailService mailService = (MailService) container.getComponentInstanceOfType(MailService.class);
    for (String address : emailList) {
      message.setTo(address);
      try {
        mailService.sendMessage(message);
      } catch (Exception e) {
        if (log_.isDebugEnabled()) {
          log_.debug(String.format("Failed to send notification email to user: %s", address), e);
        }
      }
    }
  }
  
  private static boolean isEnabledUser(String userName) throws Exception {
    OrganizationService orgService = org.exoplatform.wiki.rendering.util.Utils.getService(OrganizationService.class);
    return orgService.getUserHandler().findUserByName(userName) != null;
  }
  
  public static String getEmailUser(String userName) throws Exception {
    OrganizationService organizationService = (OrganizationService) ExoContainerContext.getCurrentContainer()
                                                                                       .getComponentInstanceOfType(OrganizationService.class);
    User user = organizationService.getUserHandler().findUserByName(userName);
    String email = user.getEmail();
    return email;
  }
  
  public static boolean isWikiAvailable(String wikiType, String wikiOwner) {
    WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);

    Wiki wiki;
    try {
      wiki = wikiService.getWikiByTypeAndOwner(wikiType, wikiOwner);
    } catch (Exception e) {
      return false;
    }

    return (wiki != null);
  }
  
  public static HashMap<String, IDType> getACLForAdmins() {
    HashMap<String, IDType> permissionMap = new HashMap<String, IDType>();
    UserACL userACL = (UserACL) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(UserACL.class);
    permissionMap.put(userACL.getSuperUser(), IDType.USER);
    for (String group : userACL.getPortalCreatorGroups()) {
      if (!StringUtils.isEmpty(group)) {
        permissionMap.put(group, IDType.MEMBERSHIP);
      }
    }
    return permissionMap;
  }
  /**
   * Has permission.
   * 
   * @param acl
   *          access control list 
   * @param permission
   *          permissions array
   * @param user
   *          user Identity
   * @return boolean
   */
  public static boolean hasPermission( AccessControlList acl,String[] permission, Identity user) {
    
    String userId = user.getUserId();
    if (userId.equals(IdentityConstants.SYSTEM)) {
      // SYSTEM has permission everywhere
      return true;
    } else if (userId.equals(acl.getOwner())) {
      // Current user is owner of node so has all privileges
      return true;
    } else if (userId.equals(IdentityConstants.ANONIM)) {
      List<String> anyPermissions = acl.getPermissions(IdentityConstants.ANY);

      if (anyPermissions.size() < permission.length)
        return false;

      for (int i = 0; i < permission.length; i++) {
        if (!anyPermissions.contains(permission[i]))
          return false;
      }
      return true;
    } else {
      if (acl.getPermissionsSize() > 0 && permission.length > 0) {
        // check permission to perform all of the listed actions
        for (int i = 0; i < permission.length; i++) {
          // check specific actions
          if (!isPermissionMatch(acl.getPermissionEntries(), permission[i], user))
            return false;
        }
        return true;
      }
      return false;
    }
  }
  /**
   * Has permission.
   * 
   * @param permission
   *          permissions array
   * @param user
   *          user Identity
   * @param pageParams
   *          wikiPage parameter 
   * @return boolean
   */
  public static boolean hasPermission( String[] permission, Identity user,WikiPageParams pageParams) {
    UserACL userACL = Util.getUIPortalApplication().getApplicationComponent(UserACL.class);
    WikiService wikiService = (WikiService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    List<PermissionEntry> permissionEntries = new ArrayList<PermissionEntry>();
    try {
      permissionEntries = wikiService.getWikiPermission(pageParams.getType(), pageParams.getOwner());
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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
    return hasPermission(acl,permission,user);
  }
  
  private static boolean isPermissionMatch(List<AccessControlEntry> existedPermission, String testPermission, Identity user) {
    for (int i = 0, length = existedPermission.size(); i < length; i++) {
      AccessControlEntry ace = existedPermission.get(i);
      // match action
      if (testPermission.equals(ace.getPermission())) {
        // match any
        if (IdentityConstants.ANY.equals(ace.getIdentity()))
          return true;
        else if (ace.getIdentity().indexOf(":") == -1) {
          // just user
          if (ace.getIdentity().equals(user.getUserId()))
            return true;

        } else if (user.isMemberOf(ace.getMembershipEntry()))
          return true;
      }
    }
    return false;
  }
  
  private static String makeNotificationSender(String from) {
    InternetAddress addr = null;
    if (from == null) return null;
    try {
      addr = new InternetAddress(from);
    } catch (AddressException e) {
      if (log_.isDebugEnabled()) { log_.debug("value of 'from' field in message made by forum notification feature is not in format of mail address", e); }
      return null;
    }
    Properties props = new Properties(System.getProperties());
    String mailAddr = props.getProperty("gatein.email.smtp.from");
    if (mailAddr == null || mailAddr.length() == 0) mailAddr = props.getProperty("mail.from");
    if (mailAddr != null) {
      try {
        InternetAddress serMailAddr = new InternetAddress(mailAddr);
        addr.setAddress(serMailAddr.getAddress());
        return addr.toUnicodeString();
      } catch (AddressException e) {
        if (log_.isDebugEnabled()) { log_.debug("value of 'gatein.email.smtp.from' or 'mail.from' in configuration file is not in format of mail address", e); }
        return null;
      }
    } else {
      return null;
    }
  }
  

  private static String insertStyle(String rawHTML) {
    String result = rawHTML;
    result = result.replaceAll("class=\"diffaddword\"", "style=\"background: #b5ffbf;\"");
    result = result.replaceAll("<span class=\"diffremoveword\">",
                               "<span style=\" background: #ffd8da;text-decoration: line-through;\">");
    result = result.replaceAll("<pre class=\"diffremoveword\">",
                               "<pre style=\" background: #ffd8da;\">");
    return result;
  }
  
  /*
   * get URL to public on social activity
   */
  public static String getURL(String url, String verName){
    StringBuffer strBuffer = new StringBuffer();
    strBuffer.append(url).append("?").append(WikiContext.ACTION).append("=").append(COMPARE_REVISION).append("&").append(VER_NAME).append("=").append(verName);
    return strBuffer.toString();
  }
  
  public static SessionProvider createSystemProvider() {
    SessionProviderService sessionProviderService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SessionProviderService.class);
    return sessionProviderService.getSystemSessionProvider(null);
  }
  
  public static long countSearchResult(WikiSearchData data) throws Exception {
    data.setOffset(0);
    data.setLimit(Integer.MAX_VALUE);
    WikiService wikiservice = (WikiService) PortalContainer.getComponent(WikiService.class);
    PageList<SearchResult> results = wikiservice.search(data);
    return results.getAll().size();

  }
  
  public static String getNodeTypeCssClass(Attachment attachment, String append) throws Exception {
    Class<?> dmsMimeTypeResolverClass = Class.forName("org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver");
    Object dmsMimeTypeResolverObject =
        dmsMimeTypeResolverClass.getDeclaredMethod("getInstance", null).invoke(null, null);
    Object mimeType = dmsMimeTypeResolverClass
      .getMethod("getMimeType", new Class[] { String.class})
      .invoke(dmsMimeTypeResolverObject, new Object[]{new String(attachment.getFullTitle().toLowerCase())});

    StringBuilder cssClass = new StringBuilder();
    cssClass.append(append);
    cssClass.append("FileDefault");
    cssClass.append(" ");
    cssClass.append(append);
    cssClass.append("nt_file");
    cssClass.append(" ");
    cssClass.append(append);
    cssClass.append(((String)mimeType).replaceAll("/|\\.", ""));
    return cssClass.toString();
  }
  
  /**
   * gets rest context name
   * @return rest context name
   */
  public static String getRestContextName() {
    return org.exoplatform.wiki.rendering.util.Utils.getRestContextName();
  }
  
  public static String[] getAllPermissionText(){
    return new String[] {
        org.exoplatform.services.jcr.access.PermissionType.READ, 
        org.exoplatform.services.jcr.access.PermissionType.ADD_NODE,
        org.exoplatform.services.jcr.access.PermissionType.REMOVE,
        org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY};
  }
  
  public static String getReadPermissionText(){
    return org.exoplatform.services.jcr.access.PermissionType.READ;
  }
  
  private static String getAddNodePermissionText(){
    return org.exoplatform.services.jcr.access.PermissionType.ADD_NODE;
  }
  
  private static String getRemovePermissionText(){
    return org.exoplatform.services.jcr.access.PermissionType.REMOVE;
  }
  
  private static String getSetPropertyPermissionText(){
    return org.exoplatform.services.jcr.access.PermissionType.SET_PROPERTY;
  }

  public static List<PermissionEntry> convertToPermissionEntryList(HashMap<String, String[]> permissions) {
    List<PermissionEntry> permissionEntries = new ArrayList<PermissionEntry>();
    Set<Entry<String, String[]>> entries = permissions.entrySet();
    for (Entry<String, String[]> entry : entries) {
      PermissionEntry permissionEntry = new PermissionEntry();
      String key = entry.getKey();
      IDType idType = IDType.USER;
      if (key.indexOf(":") > 0) {
        idType = IDType.MEMBERSHIP;
      } else if (key.indexOf("/") == 0) {
        idType = IDType.GROUP;
      }
      permissionEntry.setIdType(idType);
      permissionEntry.setId(key);
      Permission[] perms = new Permission[2];
      perms[0] = new Permission();
      perms[0].setPermissionType(PermissionType.VIEWPAGE);
      perms[1] = new Permission();
      perms[1].setPermissionType(PermissionType.EDITPAGE);
      for (String action : entry.getValue()) {
        if (Utils.getReadPermissionText().equals(action)) {
          perms[0].setAllowed(true);
        } else if (Utils.getAddNodePermissionText().equals(action)
            || Utils.getRemovePermissionText().equals(action)
            || Utils.getSetPropertyPermissionText().equals(action)) {
          perms[1].setAllowed(true);
        }
      }
      permissionEntry.setPermissions(perms);

      permissionEntries.add(permissionEntry);
    }
    return permissionEntries;
  }
  
  public static HashMap<String, String[]> convertToPermissionMap(List<PermissionEntry> permissionEntries) {
    HashMap<String, String[]> permissionMap = new HashMap<String, String[]>();
    for (PermissionEntry permissionEntry : permissionEntries) {
      Permission[] permissions = permissionEntry.getPermissions();
      List<String> permlist = new ArrayList<String>();
      for (int i = 0; i < permissions.length; i++) {
        Permission permission = permissions[i];
        if (permission.isAllowed()) {
          if (permission.getPermissionType().equals(PermissionType.VIEWPAGE)) {
            permlist.add(Utils.getReadPermissionText());
          } else if (permission.getPermissionType().equals(PermissionType.EDITPAGE)) {
            permlist.add(Utils.getAddNodePermissionText());
            permlist.add(Utils.getRemovePermissionText());
            permlist.add(Utils.getSetPropertyPermissionText());
          }
        }
      }
      if (permlist.size() > 0) {
        permissionMap.put(permissionEntry.getId(), permlist.toArray(new String[permlist.size()]));
      }
    }
    return permissionMap;
  }
}
