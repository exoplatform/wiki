package org.exoplatform.wiki.ext.impl;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.SpaceStorageException;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.ext.impl.WikiUIActivity.CommentType;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.service.BreadcrumbData;
import org.exoplatform.wiki.service.IDType;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.WikiConstants;
import org.xwiki.rendering.syntax.Syntax;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WikiSpaceActivityPublisher extends PageWikiListener {

  public static final String WIKI_APP_ID         = "ks-wiki:spaces";

  public static final String ACTIVITY_TYPE_KEY   = "act_key";

  public static final String PAGE_ID_KEY         = "page_id";

  public static final String PAGE_TYPE_KEY       = "page_type";

  public static final String PAGE_OWNER_KEY      = "page_owner";

  public static final String PAGE_TITLE_KEY      = "page_name";

  public static final String URL_KEY             = "page_url";

  public static final String PAGE_EXCERPT        = "page_exceprt";

  public static final String VIEW_CHANGE_URL_KEY = "view_change_url";

  public static final String VIEW_CHANGE_ANCHOR  = "#CompareRevision/changes";

  public static final String WIKI_PAGE_NAME      = "wiki";

  public static final String WIKI_PAGE_VERSION   = "version";

  private static final int   EXCERPT_LENGTH      = 140;

  private static final Log   LOG                 = ExoLogger.getExoLogger(WikiSpaceActivityPublisher.class);

  private WikiService        wikiService;

  private IdentityManager    identityManager;

  private RenderingService   renderingService;

  private ActivityManager    activityManager;

  private SpaceService       spaceService;

  public WikiSpaceActivityPublisher(WikiService wikiService,
                                    IdentityManager identityManager,
                                    RenderingService renderingService,
                                    ActivityManager activityManager,
                                    SpaceService spaceService) {
    this.wikiService = wikiService;
    this.identityManager = identityManager;
    this.renderingService = renderingService;
    this.activityManager = activityManager;
    this.spaceService = spaceService;
  }

  private ExoSocialActivityImpl createNewActivity(String ownerId) {
    ExoSocialActivityImpl activity = new ExoSocialActivityImpl();
    activity.setUserId(ownerId);
    activity.setBody("body");
    activity.setType(WIKI_APP_ID);
    return activity;
  }

  private ExoSocialActivity generateActivity(Identity ownerStream,
                                             Identity ownerIdentity,
                                             String wikiType,
                                             String wikiOwner,
                                             String pageId,
                                             Page page,
                                             String spaceUrl,
                                             String spaceName,
                                             PageUpdateType activityType) throws WikiException {
    // Get activity
    ExoSocialActivity activity = null;
    boolean isNewActivity = true;
    if (page.getActivityId() != null) {
      activity = activityManager.getActivity(page.getActivityId());
      isNewActivity = (activity == null);
    }

    if (isNewActivity) {
      if (page.isMinorEdit()) {
        return null;
      }
      activity = createNewActivity(ownerIdentity.getId());
    }
    activity.setTitle(page.getTitle());
    // Add UI params
    Map<String, String> templateParams = new HashMap<>();
    templateParams.put(PAGE_ID_KEY, pageId);
    templateParams.put(ACTIVITY_TYPE_KEY, activityType.toString());
    templateParams.put(PAGE_OWNER_KEY, wikiOwner);
    templateParams.put(PAGE_TYPE_KEY, wikiType);
    templateParams.put(PAGE_TITLE_KEY, page.getTitle());
    String pageURL = (page.getUrl() == null) ? (spaceUrl != null ? (spaceUrl + "/" + WIKI_PAGE_NAME) : "") : page.getUrl();
    templateParams.put(URL_KEY, pageURL);
    int versionsTotal = 0;
    List<PageVersion> versions = wikiService.getVersionsOfPage(page);
    if (versions != null && !versions.isEmpty()) {
      versionsTotal = versions.size();
    }
    templateParams.put(WIKI_PAGE_VERSION, String.valueOf(versionsTotal));

    // Create page excerpt
    StringBuilder excerpt = new StringBuilder();
    try {
      excerpt.append(renderingService.render(page.getContent(), page.getSyntax(), Syntax.PLAIN_1_0.toIdString(), false));
    } catch (Exception e) {
      throw new WikiException("Cannot render page " + page.getWikiType() + ":" + page.getWikiOwner() + page.getName(), e);
    }
    if (excerpt.length() > EXCERPT_LENGTH) {
      excerpt.replace(EXCERPT_LENGTH, excerpt.length(), "...");
    }
    templateParams.put(PAGE_EXCERPT, validateExcerpt(excerpt.toString()));
    templateParams.put(org.exoplatform.social.core.BaseActivityProcessorPlugin.TEMPLATE_PARAM_TO_PROCESS, PAGE_EXCERPT);
    if (!PageUpdateType.ADD_PAGE.equals(activityType)) {
      String verName = null;
      if (versions != null && !versions.isEmpty()) {
        verName = String.valueOf(versions.size() + 1);
      }
      templateParams.put(VIEW_CHANGE_URL_KEY, Utils.getURL(page.getUrl(), verName));
    }

    activity.setTemplateParams(templateParams);

    // Save activity
    if (isNewActivity) {
      activityManager.saveActivityNoReturn(ownerStream, activity);
    } else {
      if (PageUpdateType.MOVE_PAGE.equals(activityType)) {
        activity.setStreamOwner(ownerStream.getRemoteId());
      }
      activityManager.updateActivity(activity);
    }

    // Check to add comment to activity
    if (!PageUpdateType.ADD_PAGE.equals(activityType)) {
      if (PageUpdateType.EDIT_PAGE_TITLE.equals(activityType) && !page.isMinorEdit()) {
        createAndSaveSystemComment(activity, ownerIdentity.getId(), "WikiUIActivity.msg.update-page-title", page.getTitle());
      } else if (PageUpdateType.EDIT_PAGE_CONTENT.equals(activityType) && !page.isMinorEdit()) {
        String comment = page.getComment();
        if (StringUtils.isEmpty(comment)) {
          createAndSaveSystemComment(activity, ownerIdentity.getId(), "WikiUIActivity.msg.update-page-content");
        } else {
          createAndSaveUserComment(activity, ownerIdentity.getId(), comment);
        }
      } else if (PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE.equals(activityType) && !page.isMinorEdit()) {
        String comment = page.getComment();
        if (StringUtils.isEmpty(comment)) {
          createAndSaveComment(activity,
                               CommentType.SYSTEM_GROUP,
                               ownerIdentity.getId(),
                               null,
                               "WikiUIActivity.msg.update-page-title",
                               new String[] { page.getTitle() },
                               "WikiUIActivity.msg.update-page-content",
                               null);
        } else {
          createAndSaveComment(activity,
                               CommentType.SYSTEM_GROUP,
                               ownerIdentity.getId(),
                               null,
                               "WikiUIActivity.msg.update-page-title",
                               new String[] { page.getTitle() },
                               comment,
                               null);
        }
      } else if (PageUpdateType.MOVE_PAGE.equals(activityType)) {
        List<BreadcrumbData> breadcrumbDatas = wikiService.getBreadcumb(wikiType, wikiOwner, pageId);
        StringBuilder breadcrumText = new StringBuilder();
        breadcrumText.append((StringUtils.isEmpty(spaceName) ? wikiOwner : spaceName)).append(" > ");
        for (int i = 0; i < breadcrumbDatas.size(); i++) {
          breadcrumText.append(breadcrumbDatas.get(i).getTitle());
          if (i < breadcrumbDatas.size() - 1) {
            breadcrumText.append(" > ");
          }
        }
        createAndSaveSystemComment(activity, ownerIdentity.getId(), "WikiUIActivity.msg.move-page", breadcrumText.toString());
      }
    }
    return activity;
  }

  private String validateExcerpt(String excerpt) {
    List<String> lines = Stream.of(excerpt.split("\n")).filter(line -> !line.trim().isEmpty()).collect(Collectors.toList());

    Stream<String> sLines = lines.stream();
    StringBuilder result = new StringBuilder();

    //
    sLines.map(new Function<String, String>() {
      @Override
      public String apply(String line) {
        if (line.length() > EXCERPT_LENGTH) {
          line = line.substring(0, EXCERPT_LENGTH) + "...";
        }
        return line;
      }
    }).limit(4).forEach(line -> {
      result.append("<p>");
      result.append(line);
      result.append("</p>");
    });

    if (lines.size() > 4) {
      result.append("...");
    }
    return result.toString();
  }

  /**
   * Create and save System comment.
   * 
   * @param activity
   * @param userId
   * @param commentMsgKey
   * @param args
   */
  private void createAndSaveSystemComment(ExoSocialActivity activity, String userId, String commentMsgKey, String... args) {
    createAndSaveComment(activity, CommentType.SYSTEM, userId, null, commentMsgKey, args, null, null);
  }

  /**
   * Create and save User comment.
   * 
   * @param activity
   * @param userId
   * @param comment
   */
  private void createAndSaveUserComment(ExoSocialActivity activity, String userId, String comment) {
    createAndSaveComment(activity, CommentType.USER, userId, comment, null, null, null, null);
  }

  /**
   * Create and save comment.
   * 
   * @param activity
   * @param commentType USER: comment by user, SYSTEM: generated by system,
   *          GROUP_SYSTEM: block of 2 comments
   * @param userId
   * @param userComment
   * @param commentMsgKey1
   * @param args1
   * @param commentMsgKey2
   * @param args2
   */
  private void createAndSaveComment(ExoSocialActivity activity,
                                    CommentType commentType,
                                    String userId,
                                    String userComment,
                                    String commentMsgKey1,
                                    String[] args1,
                                    String commentMsgKey2,
                                    String[] args2) {
    // Activity manager
    ExoSocialActivity newComment = new ExoSocialActivityImpl();

    StringBuilder builder = new StringBuilder();
    //
    if (userComment == null) {
      userComment = getValueFromResourceBundle(commentMsgKey1, args1);
    }
    builder.append(userComment);
    //
    newComment.setUserId(userId);

    // Activity params
    Map<String, String> activityParams = new HashMap<>();

    activityParams.put(WikiUIActivity.COMMENT_TYPE, commentType.name());
    switch (commentType) {
    case USER:
      break;
    case SYSTEM:
      activityParams.put(WikiUIActivity.COMMENT_MESSAGE_KEY, commentMsgKey1);
      activityParams.put(WikiUIActivity.COMMENT_MESSAGE_ARGS,
                         StringUtils.join(args1, WikiUIActivity.COMMENT_MESSAGE_ARGS_ELEMENT_SAPERATOR));
      break;
    case SYSTEM_GROUP:
      activityParams.put(WikiUIActivity.COMMENT_MESSAGE_KEY1, commentMsgKey1);
      activityParams.put(WikiUIActivity.COMMENT_MESSAGE_ARGS1,
                         StringUtils.join(args1, WikiUIActivity.COMMENT_MESSAGE_ARGS_ELEMENT_SAPERATOR));
      activityParams.put(WikiUIActivity.COMMENT_MESSAGE_KEY2, commentMsgKey2);
      activityParams.put(WikiUIActivity.COMMENT_MESSAGE_ARGS2,
                         StringUtils.join(args2, WikiUIActivity.COMMENT_MESSAGE_ARGS_ELEMENT_SAPERATOR));
      if (args2 != null) {
        builder.append("<br/>").append(getValueFromResourceBundle(commentMsgKey2, args2));
      } else {
        builder.append("<br/>").append(commentMsgKey2);
      }
      break;
    }
    newComment.setTemplateParams(activityParams);
    newComment.setTitle(builder.toString());

    //
    activityManager.saveComment(activity, newComment);
  }

  /**
   * Get the string from resource bundle file by key and parameter
   * 
   * @param key the key in resource bundle file
   * @param params
   * @return
   */
  private String getValueFromResourceBundle(String key, String[] params) {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    if (res.getString(key) == null)
      return key;
    String value = res.getString(key);
    if (params != null) {
      for (int i = 0; i < params.length; i++) {
        value = value.replace("{" + i + "}", params[i]);
      }
    }
    return value;
  }

  private boolean isPublic(Page page) throws WikiException {
    List<PermissionEntry> permissions = page.getPermissions();
    // the page is public when it has permission: [any read]
    boolean isPublic = false;
    if (permissions != null) {
      for (PermissionEntry permissionEntry : permissions) {
        if (permissionEntry.getId().equals(IdentityConstants.ANY)) {
          for (Permission permission : permissionEntry.getPermissions()) {
            if (PermissionType.VIEWPAGE.equals(permission.getPermissionType())) {
              isPublic = true;
              break;
            }
          }
        }
      }
    }
    return isPublic;
  }

  /**
   * Check If a page can be read by all users of a space
   * 
   * @param page Page
   * @param space Space
   * @return true : can, false : not can;
   * @throws Exception
   */
  private boolean isPublicInSpace(Page page, Space space) throws WikiException {
    List<PermissionEntry> pagePermissions = page.getPermissions();
    String groupMemberShip = MembershipEntry.ANY_TYPE + ":" + space.getGroupId();
    boolean isPublic = false;
    if (pagePermissions != null) {
      for (PermissionEntry permissionEntry : pagePermissions) {
        if (permissionEntry.getIdType().equals(IDType.MEMBERSHIP) && permissionEntry.getId().equals(groupMemberShip)) {
          isPublic = true;
          break;
        }
      }
    }
    return isPublic;
  }

  protected void saveActivity(String wikiType,
                              String wikiOwner,
                              String pageId,
                              Page page,
                              PageUpdateType activityType) throws WikiException {
    try {
      Class.forName("org.exoplatform.social.core.space.spi.SpaceService");
    } catch (ClassNotFoundException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("eXo Social components not found!", e);
      }
      return;
    }

    // Not raise the activity in case of user space
    if (PortalConfig.USER_TYPE.equals(wikiType)) {
      return;
    }

    String username = ConversationState.getCurrent().getIdentity().getUserId();
    Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, username, false);

    Identity ownerStream = null, authorActivity = userIdentity;
    ExoSocialActivity activity = null;
    String spaceUrl = null;
    String spaceName = null;
    if (PortalConfig.GROUP_TYPE.equals(wikiType)) {
      /* checking whether the page is in a space */
      Space space;
      try {
        space = spaceService.getSpaceByGroupId(wikiOwner);
        if (space != null) {
          if (!isPublicInSpace(page, space))
            return;
          ownerStream = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
          spaceUrl = space.getUrl();
          spaceName = space.getDisplayName();
        }
      } catch (SpaceStorageException e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(String.format("Space %s not existed", wikiOwner), e);
        }
      }
    }

    if (ownerStream == null) {
      // if the page is public, publishing the activity in the user stream.
      ownerStream = userIdentity;
    }

    if (ownerStream != null) {
      activity = generateActivity(ownerStream,
                                  authorActivity,
                                  wikiType,
                                  wikiOwner,
                                  pageId,
                                  page,
                                  spaceUrl,
                                  spaceName,
                                  activityType);
      if (activity == null) {
        return;
      }

      // Attach activity id to wiki page
      String activityId = activity.getId();
      if (!StringUtils.isEmpty(activityId)) {
        page.setActivityId(activityId);
        wikiService.updatePage(page, null);
      }
    }
  }

  @Override
  public void postAddPage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    if (WikiConstants.WIKI_HOME_NAME.equals(pageId)) {
      // catch the case of the Wiki Home added as it's created by the system, not by
      // users.
      return;
    }
    saveActivity(wikiType, wikiOwner, pageId, page, PageUpdateType.ADD_PAGE);
  }

  @Override
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    if (page.getActivityId() != null && StringUtils.isNotEmpty(page.getActivityId())) {
      activityManager.deleteActivity(page.getActivityId());
    }
  }

  @Override
  public void postUpdatePage(String wikiType,
                             String wikiOwner,
                             String pageId,
                             Page page,
                             PageUpdateType wikiUpdateType) throws WikiException {
    // Generate an activity only in the following cases
    if (page != null && wikiUpdateType != null
        && (wikiUpdateType.equals(PageUpdateType.ADD_PAGE) || wikiUpdateType.equals(PageUpdateType.EDIT_PAGE_CONTENT)
            || wikiUpdateType.equals(PageUpdateType.EDIT_PAGE_TITLE)
            || wikiUpdateType.equals(PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE)
            || wikiUpdateType.equals(PageUpdateType.MOVE_PAGE))) {
      saveActivity(wikiType, wikiOwner, pageId, page, wikiUpdateType);
    }
  }
}
