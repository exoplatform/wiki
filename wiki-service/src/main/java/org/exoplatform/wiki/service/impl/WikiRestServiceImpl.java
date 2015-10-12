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
package org.exoplatform.wiki.service.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiNodeType;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.wiki.AttachmentImpl;
import org.exoplatform.wiki.mow.core.api.wiki.DraftPageImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.rendering.impl.RenderingServiceImpl;
import org.exoplatform.wiki.service.Relations;
import org.exoplatform.wiki.service.WikiContext;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiResource;
import org.exoplatform.wiki.service.WikiRestService;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.image.ResizeImageService;
import org.exoplatform.wiki.service.related.JsonRelatedData;
import org.exoplatform.wiki.service.related.RelatedUtil;
import org.exoplatform.wiki.service.rest.model.Attachment;
import org.exoplatform.wiki.service.rest.model.Attachments;
import org.exoplatform.wiki.service.rest.model.Link;
import org.exoplatform.wiki.service.rest.model.ObjectFactory;
import org.exoplatform.wiki.service.rest.model.PageSummary;
import org.exoplatform.wiki.service.rest.model.Pages;
import org.exoplatform.wiki.service.rest.model.Space;
import org.exoplatform.wiki.service.rest.model.Spaces;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TitleSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.tree.JsonNodeData;
import org.exoplatform.wiki.tree.TreeNode;
import org.exoplatform.wiki.tree.TreeNode.TREETYPE;
import org.exoplatform.wiki.tree.WikiTreeNode;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.WikiNameValidator;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.syntax.Syntax;

/**
 * {@inheritDoc}
 */
@SuppressWarnings("deprecation")
@Path("/wiki")
public class WikiRestServiceImpl implements WikiRestService, ResourceContainer {

  private final WikiService      wikiService;

  private final RenderingService renderingService;

  private static Log             log = ExoLogger.getLogger("wiki:WikiRestService");

  private static final String DASH = "-";

  private final CacheControl     cc;
  
  private ObjectFactory  objectFactory = new ObjectFactory();
  
  public WikiRestServiceImpl(WikiService wikiService, RenderingService renderingService) {
    this.wikiService = wikiService;
    this.renderingService = renderingService;
    cc = new CacheControl();
    cc.setNoCache(true);
    cc.setNoStore(true);
  }

  /**
   * {@inheritDoc}
   */
  @POST
  @Path("/content/")
  @Produces(MediaType.TEXT_HTML)
  @RolesAllowed("users")
  public Response getWikiPageContent(@QueryParam("sessionKey") String sessionKey,
                                     @QueryParam("wikiContext") String wikiContextKey,
                                     @QueryParam("markup") boolean isMarkup,
                                     @FormParam("html") String data) {
    EnvironmentContext env = EnvironmentContext.getCurrent();
    WikiContext wikiContext = new WikiContext();
    String currentSyntax = Syntax.XWIKI_2_0.toIdString();
    HttpServletRequest request = (HttpServletRequest) env.get(HttpServletRequest.class);
    try {
      if (data == null) {
        if (sessionKey != null && sessionKey.length() > 0) {
          data = (String) request.getSession().getAttribute(sessionKey);
        }
      }
      if (wikiContextKey != null && wikiContextKey.length() > 0) {
        wikiContext = (WikiContext) request.getSession().getAttribute(wikiContextKey);
        if (wikiContext != null)
          currentSyntax = wikiContext.getSyntax();
      }
      Execution ec = ((RenderingServiceImpl) renderingService).getExecution();
      if (ec.getContext() == null) {
        ec.setContext(new ExecutionContext());
      }
      ec.getContext().setProperty(WikiContext.WIKICONTEXT, wikiContext);
      ServletContext wikiServletContext = PortalContainer.getInstance()
                                                         .getPortalContext()
                                                         .getContext("/wiki");
      InputStream is = wikiServletContext.getResourceAsStream("/templates/wiki/webui/xwiki/wysiwyginput.html");
      byte[] b = new byte[is.available()];
      is.read(b);
      is.close();
     
      data = renderingService.render(data,
                                     Syntax.XHTML_1_0.toIdString(),
                                     currentSyntax,
                                     false);
      data = renderingService.render(data,
                                     currentSyntax,
                                     Syntax.ANNOTATED_XHTML_1_0.toIdString(),
                                     false);
      data = new String(b).replace("$content", data);

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
    return Response.ok(data, MediaType.TEXT_HTML).cacheControl(cc).build();
  }

  /**
   * Upload an attachment to a wiki page
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @param pageId Is the pageId used by the system
   * @return the instance of javax.ws.rs.core.Response
   */
  @POST
  @Path("/upload/{wikiType}/{wikiOwner:.+}/{pageId}/")
  @RolesAllowed("users")
  public Response upload(@PathParam("wikiType") String wikiType,
                         @PathParam("wikiOwner") String wikiOwner,
                         @PathParam("pageId") String pageId) {
    EnvironmentContext env = EnvironmentContext.getCurrent();
    HttpServletRequest req = (HttpServletRequest) env.get(HttpServletRequest.class);
    boolean isMultipart = FileUploadBase.isMultipartContent(req);
    if (isMultipart) {
      DiskFileUpload upload = new DiskFileUpload();
      // Parse the request
      try {
        List<FileItem> items = upload.parseRequest(req);
        for (FileItem fileItem : items) {
          InputStream inputStream = fileItem.getInputStream();
          byte[] imageBytes;
          if (inputStream != null) {
            imageBytes = new byte[inputStream.available()];
            inputStream.read(imageBytes);
          } else {
            imageBytes = null;
          }
          String fileName = Utils.escapeIllegalCharacterInName(fileItem.getName());
          if (fileName != null)
            // It's necessary because IE posts full path of uploaded files
            fileName = FilenameUtils.getName(fileName);          
          String mimeType = new MimeTypeResolver().getMimeType(StringUtils.lowerCase(fileName));
          WikiResource attachfile = new WikiResource(mimeType, "UTF-8", imageBytes);
          attachfile.setName(fileName);
          if (attachfile != null) {
            WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
            Page page = wikiService.getExsitedOrNewDraftPageById(wikiType, wikiOwner, Utils.escapeIllegalJcrChars(pageId));
            AttachmentImpl att = ((PageImpl) page).createAttachment(attachfile.getName(), attachfile);
            ConversationState conversationState = ConversationState.getCurrent();
            String creator = null;
            if (conversationState != null && conversationState.getIdentity() != null) {
              creator = conversationState.getIdentity().getUserId();
            }
            att.setCreator(creator);
          }
        }
      } catch (IllegalArgumentException e) {
        log.error("Special characters are not allowed in the name of an attachment.");
        return Response.status(HTTPStatus.BAD_REQUEST).entity(e.getMessage()).build();
      } catch (Exception e) {
        log.error(e.getMessage());
        return Response.status(HTTPStatus.BAD_REQUEST).entity(e.getMessage()).build();
      }
    }
    return Response.ok().build();
  }

  /**
   * Display the current tree of a wiki based on is path
   * @param type It can be a Portal, Group, User type of wiki
   * @param path Contains the path of the wiki page
   * @param currentPath Contains the path of the current wiki page
   * @param showExcerpt Boolean to display or not the excerpt
   * @param depth Defined the depth of the children we want to display
   * @return List of descendants including the page itself.
   */
  @GET
  @Path("/tree/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTreeData(@PathParam("type") String type,
                              @QueryParam(TreeNode.PATH) String path,
                              @QueryParam(TreeNode.CURRENT_PATH) String currentPath,
                              @QueryParam(TreeNode.SHOW_EXCERPT) Boolean showExcerpt,
                              @QueryParam(TreeNode.DEPTH) String depth) {
    try {
      List<JsonNodeData> responseData = new ArrayList<JsonNodeData>();
      HashMap<String, Object> context = new HashMap<String, Object>();
      
      if (currentPath != null){
        context.put(TreeNode.CURRENT_PATH, Text.unescapeIllegalJcrChars(currentPath));
        WikiPageParams currentPageParam = TreeUtils.getPageParamsFromPath(currentPath);
        PageImpl currentPage = (PageImpl) wikiService.getPageById(currentPageParam.getType(), currentPageParam.getOwner(), currentPageParam.getPageId());
        context.put(TreeNode.CURRENT_PAGE, currentPage);
      }
      
      // Put select page to context
      context.put(TreeNode.PATH, Text.unescapeIllegalJcrChars(path));
      WikiPageParams pageParam = TreeUtils.getPageParamsFromPath(path);
      PageImpl page = (PageImpl) wikiService.getPageById(pageParam.getType(), pageParam.getOwner(), pageParam.getPageId());
      if (page == null) {
        log.warn("User [{}] can not get wiki path [{}]. Wiki Home is used instead",
                 ConversationState.getCurrent().getIdentity().getUserId(), path);
        page = (PageImpl) wikiService.getPageById(pageParam.getType(), pageParam.getOwner(), pageParam.WIKI_HOME);
      }
      
      context.put(TreeNode.SELECTED_PAGE, page);
      
      context.put(TreeNode.SHOW_EXCERPT, showExcerpt);
      if (type.equalsIgnoreCase(TREETYPE.ALL.toString())) {
        Stack<WikiPageParams> stk = Utils.getStackParams(page);
        context.put(TreeNode.STACK_PARAMS, stk);
        responseData = getJsonTree(pageParam, context);
      } else if (type.equalsIgnoreCase(TREETYPE.CHILDREN.toString())) {
        // Get children only
        if (depth == null)
          depth = "1";
        context.put(TreeNode.DEPTH, depth);
        responseData = getJsonDescendants(pageParam, context);
      }
      return Response.ok(new BeanToJsons(responseData), MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Failed for get tree data by rest service.", e.getMessage());
      }
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }

  /**
   * Return the related pages of a Wiki page
   * @param path Contains the path of the wiki page
   * @return List of related pages
   */
  @GET
  @Path("/related/")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getRelated(@QueryParam(TreeNode.PATH) String path) {
    if (path == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    try {
      WikiPageParams params = TreeUtils.getPageParamsFromPath(path);
      PageImpl page = (PageImpl) wikiService.getPageById(params.getType(), params.getOwner(), params.getPageId());
      if (page != null) {
        List<PageImpl> relatedPages = page.getRelatedPages();
        List<JsonRelatedData> relatedData = RelatedUtil.pageImplToJson(relatedPages);
        return Response.ok(new BeanToJsons<JsonRelatedData>(relatedData)).cacheControl(cc).build();
      }
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      if (log.isErrorEnabled()) log.error(String.format("can not get related pages of [%s]", path), e);
      return Response.serverError().cacheControl(cc).build();
    }
  }

  /**
   * Return a list of wiki based on their type.
   * @param uriInfo Uri of the wiki
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param start Not used
   * @param number Not used
   * @return List of wikis by type
   */
  @GET
  @Path("/{wikiType}/spaces")
  @Produces("application/xml")
  @RolesAllowed("users")
  public Spaces getSpaces(@Context UriInfo uriInfo,
                          @PathParam("wikiType") String wikiType,
                          @QueryParam("start") Integer start,
                          @QueryParam("number") Integer number) {
    Spaces spaces = objectFactory.createSpaces();
    List<String> spaceNames = new ArrayList<String>();
    Collection<Wiki> wikis = Utils.getWikisByType(WikiType.valueOf(wikiType.toUpperCase()));
    for (Wiki wiki : wikis) {
      spaceNames.add(wiki.getOwner());
    }
    for (String spaceName : spaceNames) {
      try {
        Page page = wikiService.getPageById(wikiType, spaceName, WikiNodeType.Definition.WIKI_HOME_NAME);
        spaces.getSpaces().add(createSpace(objectFactory, uriInfo.getBaseUri(), wikiType, spaceName, page));
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    return spaces;
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private List getLastAccessedSpace(String userId, String appId, int offset, int limit) throws Exception {
    List spaces = new ArrayList();
    Class spaceServiceClass = Class.forName("org.exoplatform.social.core.space.spi.SpaceService");
    Object spaceService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(spaceServiceClass);
    spaces = (List) spaceServiceClass.getDeclaredMethod("getLastAccessedSpace", String.class, String.class, Integer.class, Integer.class)
      .invoke(spaceService, userId, appId, new Integer(offset), new Integer(limit));
    return spaces;
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked"})
  private <T> T getValueFromSpace(Object space, String getterMethod, Class<T> propertyClass) throws Exception {
    Class spaceClass = Class.forName("org.exoplatform.social.core.space.model.Space");
    T propertyValue = (T) spaceClass.getMethod(getterMethod).invoke(space);
    return propertyValue;
  }

  /**
   * Return a list of last visited spaces by the user.
   * @param uriInfo Uri of the wiki
   * @param offset The offset to search
   * @param limit Limit number to search
   * @return List of spaces
   */
  @GET
  @Path("/lastVisited/spaces")
  @Produces("application/xml")
  @SuppressWarnings("rawtypes")
  @RolesAllowed("users")
  public Spaces getLastVisitedSpaces(@Context UriInfo uriInfo,
                                     @QueryParam("offset") Integer offset,
                                     @QueryParam("limit") Integer limit) {
    Spaces spaces = objectFactory.createSpaces();
    String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
    try {
      List lastVisitedSpaces = getLastAccessedSpace(currentUser, "Wiki", offset, limit);
      for (Object space : lastVisitedSpaces) {
        String groupId = getValueFromSpace(space, "getGroupId", String.class);
        String displayName = getValueFromSpace(space, "getDisplayName", String.class);
        Wiki wiki = wikiService.getWikiById(groupId);
        Page page = wikiService.getPageById(wiki.getType(), wiki.getOwner(), WikiNodeType.Definition.WIKI_HOME_NAME);
        spaces.getSpaces().add(createSpace(objectFactory, uriInfo.getBaseUri(), wiki.getType(), displayName, page));
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return spaces;
  }

  /**
   * Return the space based on the uri
   * @param uriInfo Uri of the wiki
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @return Space related to the uri
   */
  @GET
  @Path("/{wikiType}/spaces/{wikiOwner:.+}/")
  @Produces("application/xml")
  @RolesAllowed("users")
  public Space getSpace(@Context UriInfo uriInfo,
                        @PathParam("wikiType") String wikiType,
                        @PathParam("wikiOwner") String wikiOwner) {
    Page page;
    try {
      page = wikiService.getPageById(wikiType, wikiOwner, WikiNodeType.Definition.WIKI_HOME_NAME);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return objectFactory.createSpace();
    }
    return createSpace(objectFactory, uriInfo.getBaseUri(), wikiType, wikiOwner, page);
  }

  /**
   * Return a list of pages related to the space and uri
   * @param uriInfo Uri of the wiki
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @param start Not used
   * @param number Not used
   * @param parentFilterExpression
   * @return List of pages
   */
  @GET
  @Path("/{wikiType}/spaces/{wikiOwner:.+}/pages")
  @Produces("application/xml")
  @RolesAllowed("users")
  public Pages getPages(@Context UriInfo uriInfo,
                        @PathParam("wikiType") String wikiType,
                        @PathParam("wikiOwner") String wikiOwner,
                        @QueryParam("start") Integer start,
                        @QueryParam("number") Integer number,
                        @QueryParam("parentId") String parentFilterExpression) {
    Pages pages = objectFactory.createPages();
    PageImpl page = null;
    boolean isWikiHome = true;
    try {
      String parentId = WikiNodeType.Definition.WIKI_HOME_NAME;
      if (parentFilterExpression != null && parentFilterExpression.length() > 0
          && !parentFilterExpression.startsWith("^(?!")) {
        parentId = parentFilterExpression;
        if (parentId.indexOf(".") >= 0) {
          parentId = parentId.substring(parentId.indexOf(".") + 1);
        }
        isWikiHome = false;
      }
      page = (PageImpl) wikiService.getPageById(wikiType, wikiOwner, parentId);
      if (isWikiHome) {
        pages.getPageSummaries().add(createPageSummary(objectFactory, uriInfo.getBaseUri(), page));
      } else {
        for (PageImpl childPage : page.getChildPages().values()) {
          pages.getPageSummaries().add(createPageSummary(objectFactory,
                                                       uriInfo.getBaseUri(),
                                                       childPage));
        }
      }
    } catch (Exception e) {
      log.error("Can't get children pages of:" + parentFilterExpression, e);
    }

    return pages;
  }

  /**
   * Return a wiki page based on is uri and id
   * @param uriInfo Uri of the wiki
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @param pageId Id of the wiki page
   * @return A wiki page
   */
  @GET
  @Path("/{wikiType}/spaces/{wikiOwner:.+}/pages/{pageId}")
  @Produces("application/xml")
  @RolesAllowed("users")
  public org.exoplatform.wiki.service.rest.model.Page getPage(@Context UriInfo uriInfo,
                                                              @PathParam("wikiType") String wikiType,
                                                              @PathParam("wikiOwner") String wikiOwner,
                                                              @PathParam("pageId") String pageId) {
    PageImpl page;
    try {
      page = (PageImpl) wikiService.getPageById(wikiType, wikiOwner, pageId);
      if (page != null) {
        return createPage(objectFactory, uriInfo.getBaseUri(), uriInfo.getAbsolutePath(), page);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return objectFactory.createPage();
  }

    /**
     * Return a list of attachments attached to a wiki page
     * @param uriInfo Uri of the wiki
     * @param wikiType It can be a Portal, Group, User type of wiki
     * @param wikiOwner Is the owner of the wiki
     * @param pageId Id of the wiki page
     * @param start Not used
     * @param number Not used
     * @return List of attachments
     */
  @GET
  @Path("/{wikiType}/spaces/{wikiOwner:.+}/pages/{pageId}/attachments")
  @Produces("application/xml")
  @RolesAllowed("users")
  public Attachments getAttachments(@Context UriInfo uriInfo,
                                    @PathParam("wikiType") String wikiType,
                                    @PathParam("wikiOwner") String wikiOwner,
                                    @PathParam("pageId") String pageId,
                                    @QueryParam("start") Integer start,
                                    @QueryParam("number") Integer number) {
    Attachments attachments = objectFactory.createAttachments();
    PageImpl page;
    try {
      page = (PageImpl) wikiService.getPageById(wikiType, wikiOwner, pageId);
      Collection<AttachmentImpl> pageAttachments = page.getAttachmentsExcludeContent();
      for (AttachmentImpl pageAttachment : pageAttachments) {
        attachments.getAttachments().add(createAttachment(objectFactory, uriInfo.getBaseUri(), pageAttachment, "attachment", "attachment"));
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return attachments;
  }

  /**
   * Return a list of title based on a searched words.
   * @param keyword Word to search
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @return List of title
   * @throws Exception
   */
  @GET
  @Path("contextsearch/")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response searchData(@QueryParam("keyword") String keyword,
                             @QueryParam("wikiType") String wikiType,
                             @QueryParam("wikiOwner") String wikiOwner) throws Exception {
    try {
      WikiSearchData data = new WikiSearchData(keyword.toLowerCase(), null, wikiType, wikiOwner);
      data.setNodeType("nt:base");
      data.setLimit(10);
      List<SearchResult> results = wikiService.search(data).getAll();
      List<TitleSearchResult> titleSearchResults = new ArrayList<TitleSearchResult>();
      for (SearchResult searchResult : results) {
        String url = null;
        if (WikiNodeType.WIKI_ATTACHMENT.equals(searchResult.getType())) {
          url = ((AttachmentImpl)Utils.getObject(searchResult.getPath(), searchResult.getType())).getDownloadURL();
          String attachmentName = searchResult.getPath().substring(searchResult.getPath().lastIndexOf("/")+1);
          titleSearchResults.add(new TitleSearchResult(attachmentName, searchResult.getPath(), searchResult.getType(), url));
        } else {
          url = searchResult.getUrl();
          titleSearchResults.add(new TitleSearchResult(searchResult.getTitle(), searchResult.getPath(), searchResult.getType(), url));
        }
      }
      return Response.ok(new BeanToJsons(titleSearchResults), MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception e) {
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  /**
   * Return an image attach to the wiki page and keep the size ratio of it.
   * @param uriInfo Uri of the wiki
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @param pageId Id of the wiki page
   * @param imageId Id of the image attached to the wiki page
   * @param width expected width of the image, it will keep the ratio
   * @return The response with the image
   */
  @GET
  @Path("/images/{wikiType}/space/{wikiOwner:.+}/page/{pageId}/{imageId}")
  @Produces("image")
  public Response getImage(@Context UriInfo uriInfo,
                           @PathParam("wikiType") String wikiType,
                           @PathParam("wikiOwner") String wikiOwner,
                           @PathParam("pageId") String pageId,
                           @PathParam("imageId") String imageId,
                           @QueryParam("width") Integer width) {
    InputStream result = null;
    try {
      ResizeImageService resizeImgService = (ResizeImageService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ResizeImageService.class);
      PageImpl page = (PageImpl) wikiService.getPageByRootPermission(wikiType, wikiOwner, Utils.escapeIllegalJcrChars(pageId));
      if (page == null) {
        return Response.status(HTTPStatus.NOT_FOUND).entity("There is no resource matching to request path " + uriInfo.getPath()).type(MediaType.TEXT_PLAIN).build();
      }
      AttachmentImpl att = page.getAttachment(imageId);
      if (att == null) {
        return Response.status(HTTPStatus.NOT_FOUND).entity("There is no resource matching to request path " + uriInfo.getPath()).type(MediaType.TEXT_PLAIN).build();
      }
      ByteArrayInputStream bis = new ByteArrayInputStream(att.getContentResource().getData());
      if (width != null) {
        result = resizeImgService.resizeImageByWidth(imageId, bis, width);
      } else {
        result = bis;
      }
      return Response.ok(result, "image").cacheControl(cc).build();
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug(String.format("Can't get image name: %s of page %s", imageId, pageId), e);
      }
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  public Space createSpace(ObjectFactory objectFactory,
                           URI baseUri,
                           String wikiName,
                           String spaceName,
                           Page home) {
    Space space = objectFactory.createSpace();
    space.setId(String.format("%s:%s", wikiName, spaceName));
    space.setWiki(wikiName);
    space.setName(spaceName);
    if (home != null) {
      space.setHome("home");
      space.setXwikiRelativeUrl("home");
      space.setXwikiAbsoluteUrl("home");
    }

    String pagesUri = UriBuilder.fromUri(baseUri)
                                .path("/wiki/{wikiName}/spaces/{spaceName}/pages")
                                .build(wikiName, spaceName)
                                .toString();
    Link pagesLink = objectFactory.createLink();
    pagesLink.setHref(pagesUri);
    pagesLink.setRel(Relations.PAGES);
    space.getLinks().add(pagesLink);

    if (home != null) {
      String homeUri = UriBuilder.fromUri(baseUri)
                                 .path("/wiki/{wikiName}/spaces/{spaceName}/pages/{pageName}")
                                 .build(wikiName, spaceName, home.getName())
                                 .toString();
      Link homeLink = objectFactory.createLink();
      homeLink.setHref(homeUri);
      homeLink.setRel(Relations.HOME);
      space.getLinks().add(homeLink);
    }

    String searchUri = UriBuilder.fromUri(baseUri)
                                 .path("/wiki/{wikiName}/spaces/{spaceName}/search")
                                 .build(wikiName, spaceName)
                                 .toString();
    Link searchLink = objectFactory.createLink();
    searchLink.setHref(searchUri);
    searchLink.setRel(Relations.SEARCH);
    space.getLinks().add(searchLink);

    return space;

  }

  public org.exoplatform.wiki.service.rest.model.Page createPage(ObjectFactory objectFactory,
                                                                 URI baseUri,
                                                                 URI self,
                                                                 PageImpl doc) throws Exception {
    org.exoplatform.wiki.service.rest.model.Page page = objectFactory.createPage();
    fillPageSummary(page, objectFactory, baseUri, doc);

    page.setVersion("current");
    page.setMajorVersion(1);
    page.setMinorVersion(0);
    page.setLanguage(doc.getSyntax());
    page.setCreator(doc.getOwner());

    GregorianCalendar calendar = new GregorianCalendar();
    page.setCreated(calendar);

    page.setModifier(doc.getAuthor());

    calendar = new GregorianCalendar();
    calendar.setTime(doc.getUpdatedDate());
    page.setModified(calendar);

    page.setContent(doc.getContent().getText());

    if (self != null) {
      Link pageLink = objectFactory.createLink();
      pageLink.setHref(self.toString());
      pageLink.setRel(Relations.SELF);
      page.getLinks().add(pageLink);
    }
    return page;
  }

  public PageSummary createPageSummary(ObjectFactory objectFactory, URI baseUri, PageImpl doc) throws IllegalArgumentException, UriBuilderException, Exception {
    PageSummary pageSummary = objectFactory.createPageSummary();
    fillPageSummary(pageSummary, objectFactory, baseUri, doc);
    String wikiName = doc.getWiki().getType();
    String spaceName = doc.getWiki().getOwner();
    String pageUri = UriBuilder.fromUri(baseUri)
                               .path("/wiki/{wikiName}/spaces/{spaceName}/pages/{pageName}")
                               .build(wikiName, spaceName, doc.getName())
                               .toString();
    Link pageLink = objectFactory.createLink();
    pageLink.setHref(pageUri);
    pageLink.setRel(Relations.PAGE);
    pageSummary.getLinks().add(pageLink);

    return pageSummary;
  }
  
  public Attachment createAttachment(ObjectFactory objectFactory,
                                     URI baseUri,
                                     AttachmentImpl pageAttachment,
                                     String xwikiRelativeUrl,
                                     String xwikiAbsoluteUrl) throws Exception {
    Attachment attachment = objectFactory.createAttachment();

    fillAttachment(attachment, objectFactory, baseUri, pageAttachment, xwikiRelativeUrl, xwikiAbsoluteUrl);

    PageImpl page = pageAttachment.getParentPage();

    String attachmentUri = UriBuilder.fromUri(baseUri)
                                     .path("/wiki/{wikiName}/spaces/{spaceName}/pages/{pageName}/attachments/{attachmentName}")
                                     .build(page.getWiki().getType(), page.getWiki().getOwner(), page.getName(), pageAttachment.getName())
                                     .toString();
    Link attachmentLink = objectFactory.createLink();
    attachmentLink.setHref(attachmentUri);
    attachmentLink.setRel(Relations.ATTACHMENT_DATA);
    attachment.getLinks().add(attachmentLink);

    return attachment;
  }  
 
  private List<JsonNodeData> getJsonTree(WikiPageParams params,HashMap<String, Object> context) throws Exception {
    List<JsonNodeData> responseData = new ArrayList<JsonNodeData>();
    Wiki wiki = Utils.getWiki(params);
    WikiTreeNode wikiNode = new WikiTreeNode(wiki);
    wikiNode.pushDescendants(context);
    responseData = TreeUtils.tranformToJson(wikiNode, context);
    return responseData;
  }

  private List<JsonNodeData> getJsonDescendants(WikiPageParams params,
                                                HashMap<String, Object> context) throws Exception {
    TreeNode treeNode = TreeUtils.getDescendants(params, context);
    return TreeUtils.tranformToJson(treeNode, context);
  }

  private static void fillPageSummary(PageSummary pageSummary,
                                      ObjectFactory objectFactory,
                                      URI baseUri,
                                      PageImpl doc) throws IllegalArgumentException, UriBuilderException, Exception {
    String wikiType = doc.getWiki().getType();
    pageSummary.setWiki(wikiType);
    pageSummary.setFullName(doc.getTitle());
    pageSummary.setId(wikiType + ":" + doc.getWiki().getOwner() + "." + doc.getName());
    pageSummary.setSpace(doc.getWiki().getOwner());
    pageSummary.setName(doc.getName());
    pageSummary.setTitle(doc.getTitle());
    pageSummary.setTranslations(objectFactory.createTranslations());
    pageSummary.setSyntax(doc.getSyntax());

    PageImpl parent = doc.getParentPage();
    // parentId must not be set if the parent document does not exist.
    if (parent != null) {
      pageSummary.setParent(parent.getName());
      pageSummary.setParentId(parent.getName());
    } else {
      pageSummary.setParent("");
      pageSummary.setParentId("");
    }

    String spaceUri = UriBuilder.fromUri(baseUri)
                                .path("/wiki/{wikiName}/spaces/{spaceName}")
                                .build(wikiType, doc.getWiki().getOwner())
                                .toString();
    Link spaceLink = objectFactory.createLink();
    spaceLink.setHref(spaceUri);
    spaceLink.setRel(Relations.SPACE);
    pageSummary.getLinks().add(spaceLink);

    if (parent != null) {
      String parentUri = UriBuilder.fromUri(baseUri)
                                   .path("/wiki/{wikiName}/spaces/{spaceName}/pages/{pageName}")
                                   .build(parent.getWiki().getType(),
                                          parent.getWiki().getOwner(),
                                          parent.getName())
                                   .toString();
      Link parentLink = objectFactory.createLink();
      parentLink.setHref(parentUri);
      parentLink.setRel(Relations.PARENT);
      pageSummary.getLinks().add(parentLink);
    }

    if (!doc.getChildPages().isEmpty()) {
      String pageChildrenUri = UriBuilder.fromUri(baseUri)
                                         .path("/wiki/{wikiName}/spaces/{spaceName}/pages/{pageName}/children")
                                         .build(wikiType,
                                                doc.getWiki().getOwner(),
                                                doc.getName())
                                         .toString();
      Link pageChildrenLink = objectFactory.createLink();
      pageChildrenLink.setHref(pageChildrenUri);
      pageChildrenLink.setRel(Relations.CHILDREN);
      pageSummary.getLinks().add(pageChildrenLink);
    }

    if (!doc.getAttachmentsExcludeContent().isEmpty()) {
      String attachmentsUri;
      attachmentsUri = UriBuilder.fromUri(baseUri)
                                 .path("/wiki/{wikiName}/spaces/{spaceName}/pages/{pageName}/attachments")
                                 .build(wikiType,
                                        doc.getWiki().getOwner(),
                                        doc.getName())
                                 .toString();

      Link attachmentsLink = objectFactory.createLink();
      attachmentsLink.setHref(attachmentsUri);
      attachmentsLink.setRel(Relations.ATTACHMENTS);
      pageSummary.getLinks().add(attachmentsLink);
    }

  }
  
  private void fillAttachment(Attachment attachment,
                              ObjectFactory objectFactory,
                              URI baseUri,
                              AttachmentImpl pageAttachment,
                              String xwikiRelativeUrl,
                              String xwikiAbsoluteUrl) throws Exception {
    PageImpl page = pageAttachment.getParentPage();

    attachment.setId(String.format("%s@%s", page.getName(), pageAttachment.getName()));
    attachment.setName(pageAttachment.getName());
    attachment.setSize((int) pageAttachment.getWeightInBytes());
    attachment.setVersion("current");
    attachment.setPageId(page.getName());
    attachment.setPageVersion("current");
    attachment.setMimeType(pageAttachment.getContentResource().getMimeType());
    attachment.setAuthor(pageAttachment.getCreator());

    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTime(pageAttachment.getCreated());
    attachment.setDate(calendar);

    attachment.setXwikiRelativeUrl(xwikiRelativeUrl);
    attachment.setXwikiAbsoluteUrl(xwikiAbsoluteUrl);

    String pageUri = UriBuilder.fromUri(baseUri)
                               .path("/wiki/{wikiName}/spaces/{spaceName}/pages/{pageName}")
                               .build(page.getWiki().getType(), page.getWiki().getOwner(), page.getName())
                               .toString();
    Link pageLink = objectFactory.createLink();
    pageLink.setHref(pageUri);
    pageLink.setRel(Relations.PAGE);
    attachment.getLinks().add(pageLink);
  }
  
  /**
   * Return the help syntax page.
   * The syntax id have to replaced all special characters: 
   *  Character '/' have to replace to "SLASH"
   *  Character '.' have to replace to "DOT"
   *
   * Sample:
   * "confluence/1.0" will be replaced to "confluenceSLASH1DOT0"
   *  
   * @param syntaxId The id of syntax to show in help page
   * @param portalUrl The current portal url
   * @return The response that contains help page
   */
  @GET
  @Path("/help/{syntaxId}")
  @Produces(MediaType.TEXT_HTML)
  @RolesAllowed("users")
  public Response getHelpSyntaxPage(@PathParam("syntaxId") String syntaxId, @QueryParam("portalUrl") String portalUrl) {
    CacheControl cacheControl = new CacheControl();
    
    syntaxId = syntaxId.replace(Utils.SLASH, "/").replace(Utils.DOT, ".");
    try {
      PageImpl page = wikiService.getHelpSyntaxPage(syntaxId);
      if (page == null) {
        return Response.status(HTTPStatus.NOT_FOUND).cacheControl(cc).build();
      }
      Page fullHelpPage = (Page) page.getChildPages().values().iterator().next();
      
      // Build wiki context
      if (!StringUtils.isEmpty(portalUrl)) {
        RenderingService renderingService = (RenderingService) ExoContainerContext.getCurrentContainer()
            .getComponentInstanceOfType(RenderingService.class);
        Execution ec = ((RenderingServiceImpl) renderingService).getExecution();
        if (ec.getContext() == null) {
          ec.setContext(new ExecutionContext());
        }
        WikiContext wikiContext = new WikiContext();
        wikiContext.setPortalURL(portalUrl);
        wikiContext.setType(PortalConfig.PORTAL_TYPE);
        ec.getContext().setProperty(WikiContext.WIKICONTEXT, wikiContext);
      }
      
      // Get help page body
      String body = renderingService.render(fullHelpPage.getContent().getText(), fullHelpPage.getSyntax(), Syntax.XHTML_1_0.toIdString(), false);
      
      // Create javascript to load css
      StringBuilder script = new StringBuilder("<script type=\"text/javascript\">")
      .append("var local = String(window.location);")
      .append("var i = local.indexOf('/', local.indexOf('//') + 2);")
      .append("local = (i <= 0) ? local : local.substring(0, i);")
      .append("local = local + '/wiki/skin/DefaultSkin/webui/Stylesheet.css';")
      .append("var link = document.createElement('link');")
      .append("link.rel = 'stylesheet';")
      .append("link.type = 'text/css';")
      .append("link.href = local;")
      .append("document.head = document.head || document.getElementsByTagName(\"head\")[0] || document.documentElement;")
      .append("document.head.appendChild(link);")
      .append("</script>");
      
      // Create help html page
      StringBuilder htmlOutput = new StringBuilder();
      htmlOutput.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">")
      .append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\" dir=\"ltr\">")
      .append("<head id=\"head\">")
      .append("<title>Wiki help page</title>")
      .append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>")
      .append(script)
      .append("</head>")
      .append("<body>")
      .append("<div class=\"UIWikiPageContentArea\">")
      .append(body)
      .append("</div>")
      .append("</body>")
      .append("</html>");
      
      return Response.ok(htmlOutput.toString(), MediaType.TEXT_HTML).cacheControl(cacheControl).build();
    } catch (Exception e) {
      if (log.isWarnEnabled()) {
        log.warn("An exception happens when getHelpSyntaxPage", e);
      }
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }
  
  @GET
  @Path("/spaces/accessibleSpaces/")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response searchAccessibleSpaces(@QueryParam("keyword") String keyword) {
    try {
      List<SpaceBean> spaceBeans = wikiService.searchSpaces(keyword);
      return Response.ok(new BeanToJsons(spaceBeans), MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception ex) {
      if (log.isWarnEnabled()) {
        log.warn("An exception happens when searchAccessibleSpaces", ex);
      }
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }
  
  /**
   * Save draft title and content for a page specified by the given page params
   * 
   * @param wikiType type of wiki to save draft
   * @param wikiOwner owner of wiki to save draft
   * @param rawPageId name of page to save draft in encoded format
   * @param pageRevision the target revision of target page
   * @param lastDraftName name of the draft page of last saved draft request
   * @param isNewPage The draft for new page or not
   * @param title draft title
   * @param content draft content
   * @param isMarkup content is markup or html. True if is markup.
   * @return {@link Response} with status HTTPStatus.ACCEPTED if saving process is performed successfully
   *                          with status HTTPStatus.INTERNAL_ERROR if there is any unknown error in the saving process
   */                          
  @POST
  @Path("/saveDraft/")
  @RolesAllowed("users")
  public Response saveDraft(@QueryParam("wikiType") String wikiType,
                            @QueryParam("wikiOwner") String wikiOwner,
                            @QueryParam("pageId") String rawPageId,
                            @QueryParam("pageRevision") String pageRevision,
                            @QueryParam("lastDraftName") String lastDraftName,
                            @QueryParam("isNewPage") boolean isNewPage,
                            @QueryParam("clientTime") long clientTime,
                            @FormParam("title") String title,
                            @FormParam("content") String content,
                            @FormParam("isMarkup") String isMarkup) {
    String pageId = null;
    try {
      if ("__anonim".equals(org.exoplatform.wiki.utils.Utils.getCurrentUser())) {
        return Response.status(HTTPStatus.BAD_REQUEST).cacheControl(cc).build();
      } 
      pageId = URLDecoder.decode(rawPageId,"utf-8");
      WikiPageParams param = new WikiPageParams(wikiType, wikiOwner, pageId);
      PageImpl pageImpl = (PageImpl) wikiService.getPageById(wikiType, wikiOwner, pageId);
      if (StringUtils.isEmpty(pageId) || (pageImpl == null)) {
        throw new IllegalArgumentException("Can not find the target page");
      }
      
      DraftPage draftPage = null;
      if (!isNewPage) {
        draftPage = wikiService.getDraft(param);
        if ((draftPage != null) && !draftPage.getName().equals(lastDraftName)) {
          draftPage = null;
        }
      } else {
        if (!StringUtils.isEmpty(lastDraftName)) {
          draftPage = (DraftPageImpl) wikiService.getDraft(lastDraftName);
        }
      }
      
      // If draft page is not exist then create draft page
      if (draftPage == null) {
        // if create draft for exist page, we need synchronized when create draft 
        if (!isNewPage) {
          synchronized (pageImpl.getJCRPageNode().getUUID()) {
            draftPage = (DraftPageImpl) wikiService.createDraftForExistPage(param, pageRevision, clientTime);
          }
        } else {
          draftPage = (DraftPageImpl) wikiService.createDraftForNewPage(param, clientTime);
        }
      }
      
      // Convert conent to markup if need
      if (StringUtils.isEmpty(isMarkup) || !isMarkup.toLowerCase().equals("true")) {
        content = renderingService.render(content, Syntax.XHTML_1_0.toIdString(), wikiService.getDefaultWikiSyntaxId(), false);
      }
      
      // Store page content and page title in draft
      title = replaceSpecialCharacter(title);
      if ("".equals(title)) {
        draftPage.setTitle(draftPage.getName());
      } else {
        draftPage.setTitle(title);
      }
      draftPage.getContent().setText(content);
      ((DraftPageImpl) draftPage).getChromatticSession().save();
      
      // Log the editting time for current user
      Utils.logEditPageTime(param, Utils.getCurrentUser(), System.currentTimeMillis(), draftPage.getName(), isNewPage);
      
      // Notify to client that saved draft success
      return Response.ok(new DraftData(draftPage.getName()), MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (UnsupportedEncodingException uee) {
        log.warn("Cannot decode page name");
        return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    } 
    catch (Exception ex) {
      if(StringUtils.isEmpty(pageId)) pageId = rawPageId;
      log.warn(String.format("Failed to perform auto save wiki page %s:%s:%s", wikiType,wikiOwner,pageId), ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }
  
  private String replaceSpecialCharacter(String s) {
    StringTokenizer tokens = new StringTokenizer(WikiNameValidator.INVALID_CHARACTERS);
    while (tokens.hasMoreTokens()) {
      s = s.replace(tokens.nextToken(), DASH);
    }
    return s;
  }
  
  /**
   * Remove the draft
   * 
   * @param draftName The name of draft to remove
   * @return Status.OK if remove draft success
   *         HTTPStatus.INTERNAL_ERROR if there's error occur when remove draft
   */
  @GET
  @Path("/removeDraft/")
  @RolesAllowed("users")
  public Response removeDraft(@QueryParam("draftName") String draftName) {
    if (StringUtils.isEmpty(draftName)) {
      return Response.status(HTTPStatus.BAD_REQUEST).cacheControl(cc).build();
    }
    
    try {
      wikiService.removeDraft(draftName);
      return Response.ok().cacheControl(cc).build();
    } catch (Exception e) {
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }
}
