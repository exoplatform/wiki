package org.exoplatform.wiki.ext.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.commons.utils.StringCommonUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.SpaceStorageException;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.lifecycle.WebuiBindingContext;
import org.exoplatform.wiki.utils.Utils;

@ComponentConfig (
    lifecycle = UIFormLifecycle.class,
    template = "war:/groovy/wiki/social-integration/plugin/space/WikiUIActivity.gtmpl",
    events = {
        @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
        @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
        @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LikeCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.EditActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.EditCommentActionListener.class)
      }
)
public class WikiUIActivity extends BaseUIActivity {
  
  private static final Log LOG = ExoLogger.getLogger(WikiUIActivity.class);
  
  public enum CommentType { USER, SYSTEM, SYSTEM_GROUP };

  public static final String COMMENT_MESSAGE_KEY            = "commentMessageKey";
  
  public static final String COMMENT_MESSAGE_KEY1            = "commentMessageKey1";
  
  public static final String COMMENT_MESSAGE_KEY2            = "commentMessageKey2";
  
  public static final String COMMENT_MESSAGE_ARGS = "messageArgs";
  
  public static final String COMMENT_MESSAGE_ARGS1 = "messageArgs1";
  
  public static final String COMMENT_MESSAGE_ARGS2 = "messageArgs2";
  
  public static final String COMMENT_MESSAGE_ARGS_ELEMENT_SAPERATOR = "\n";
  
  public static final String COMMENT_TYPE     = "commentType";
  
  public static final String SPACE_TYPE = "/spaces/";

  public final ResourceBundleService resourceBundleService;

  public WikiUIActivity() {
    resourceBundleService = CommonsUtils.getService(ResourceBundleService.class);
  }

  public String getUriOfAuthor() {   
    if (getOwnerIdentity() == null){
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to get Url of user, author isn't set");
      }       
      return "";
    }        
    return new StringBuilder().append("<a href='").append(getOwnerIdentity().getProfile().getUrl()).append("'>")
                                .append(StringEscapeUtils.escapeHtml(getOwnerIdentity().getProfile().getFullName())).append("</a>").toString();
  }

  public String getUserFullName(String userId) {
    return getOwnerIdentity().getProfile().getFullName();
  }

  public String getUserProfileUri(String userId) {
    return getOwnerIdentity().getProfile().getUrl();
  }

  public String getUserAvatarImageSource(String userId) {
    return getOwnerIdentity().getProfile().getAvatarUrl();
  }

  public String getSpaceAvatarImageSource(String spaceIdentityId) {    
    try {
      if (getOwnerIdentity() == null){
        LOG.error("Failed to get Space Avatar Source, unknow owner identity.");
        return null;
      }
      String spaceId = getOwnerIdentity().getRemoteId();
      SpaceService spaceService = getApplicationComponent(SpaceService.class);
      Space space = spaceService.getSpaceById(spaceId);
      if (space != null) {
        return space.getAvatarUrl();
      }
    } catch (SpaceStorageException e) { // SpaceService
      LOG.error(String.format("Failed to getSpaceById: %s. \n Cause by: ", spaceIdentityId), e);
    }

    return null;
  }
  
  public String getActivityParamValue(String key) {
    String value = null;
    Map<String, String> params = getActivity().getTemplateParams();
    if (params != null) {
      value = params.get(key);
    }
    return value != null ? value : "";
  }

  String getActivityMessage(WebuiBindingContext _ctx) throws Exception {
    return _ctx.appRes("WikiUIActivity.label.page-create");
  }

  String getPageName() throws Exception {
    String pageName = getActivityParamValue(WikiSpaceActivityPublisher.PAGE_TITLE_KEY);

    if (StringUtils.isBlank(pageName)) {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      ResourceBundle res = resourceBundleService.getResourceBundle(Utils.WIKI_RESOUCE_BUNDLE_NAME, context.getLocale());
      return res.getString("Page.Untitled");
    }
    return StringCommonUtils.encodeSpecialCharForSimpleInput(pageName);
  }

  String getPageURL() {
    String pageUrl = getActivityParamValue(WikiSpaceActivityPublisher.URL_KEY);
    if (pageUrl != null && pageUrl.contains("://")) {
    // pageURL might be a full URL, keeps only its path
      try {
        URL oldURL = new URL(pageUrl);
        pageUrl = oldURL.getPath();
      } catch (MalformedURLException ex) {
        LOG.info("Failed to create URL object.", ex);
      }
    }
    return pageUrl;
  }
  
  private String getPageURLFromSpace(){
    String spaceGroupId = getActivityParamValue(WikiSpaceActivityPublisher.PAGE_OWNER_KEY);
    String pageId = getActivityParamValue(WikiSpaceActivityPublisher.PAGE_ID_KEY);
    SpaceService spaceService = (SpaceService) PortalContainer.getInstance().getComponentInstanceOfType(SpaceService.class);
    Space space = spaceService.getSpaceByGroupId(spaceGroupId);
    
    StringBuffer sb = new StringBuffer("");
    if (space != null) {
      sb.append(org.exoplatform.social.webui.Utils.getSpaceHomeURL(space))
        .append("/" + WikiSpaceActivityPublisher.WIKI_PAGE_NAME + "/")
        .append(pageId);
    }
    
    return sb.toString();
  }
  
  public String getSpaceGroupId(){
    String spaceGroupId = "";
    String pageOwnerKey = getActivityParamValue(WikiSpaceActivityPublisher.PAGE_OWNER_KEY);
    boolean isASpace = pageOwnerKey.contains(SPACE_TYPE);
    if(isASpace) {
      spaceGroupId = pageOwnerKey;
    }
    return spaceGroupId;
  }
  
  String getViewChangeURL(){
    return getActivityParamValue(WikiSpaceActivityPublisher.VIEW_CHANGE_URL_KEY);
  }
  String getVerName(){
  	String url = getViewChangeURL();
  	StringBuilder sb = new StringBuilder();
  	for (int i = url.length() - 1; i >= 0; i --) {
  	    char c = url.charAt(i);
  	    if (Character.isDigit(c)) {
  	        sb.insert(0, c);
  	    } else {
  	        break;
  	    }
  	}
  	return sb.toString();
  }
  
  String getPageExcerpt(){
    return getActivityParamValue(WikiSpaceActivityPublisher.PAGE_EXCERPT);
  }
  
  String getPageVersion(){
    String version = getActivityParamValue(WikiSpaceActivityPublisher.WIKI_PAGE_VERSION);
    if (StringUtils.isEmpty(version)) {
      version = "1";
      String pageUrl = getPageURL();
      if (pageUrl == null) {
        return version;
      }
    }
    return version;
  }
  /**
   * Get comment message arguments by param Key.
   * 
   * @param activityParams templates params
   * @param paramKey key to get message arguments from activity params
   * @return message bundle
   */
  private String[] getActivityCommentBundleArguments(Map<String, String> activityParams, String paramKey) {
    if (activityParams != null) {
      String commentMessageArgs  = activityParams.get(paramKey);
      if (StringUtils.isNotEmpty(commentMessageArgs)) {
        String[] args = commentMessageArgs.split(COMMENT_MESSAGE_ARGS_ELEMENT_SAPERATOR);
        return args;
      }
    }
    return null;
  }
  
  /**
   * Format message from arguments.
   * 
   * @param pattern
   * @param args
   * @return
   */
  private String formatMessage(String msgKey, Object[] args) {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    String message = StringUtils.EMPTY;
    try {
      message = res.getString(msgKey);
    } catch (MissingResourceException e) {
      message = msgKey;
    }
    
    return MessageFormat.format(message.replace("'", "''"), args);
  }
  
  /**
   * Get system comment message.
   * 
   * @param messageKeyParam param to get message key from activity params
   * @param messageArgumentsParam param to get message arguments from activity params
   * @param activityParams activity parameters
   * @return
   */
  private String getSystemCommentMessage(String messageKeyParam,
                            String messageArgumentsParam,
                            Map<String, String> activityParams) {
    String msgKey = activityParams.get(messageKeyParam);
    String[] args = getActivityCommentBundleArguments(activityParams, messageArgumentsParam);
    return formatMessage(msgKey, args);
  }
  
  /**
   * Get system comment message.
   * 
   * @param activityParams
   * @param title
   * @return
   * @throws Exception 
   */
  public String getSystemCommentMessage(Map<String, String> activityParams, String title) throws Exception {
    // Get Comment Type
    CommentType commentType = CommentType.USER;
    if (activityParams != null && activityParams.containsKey(COMMENT_TYPE)) {
      commentType = CommentType.valueOf(activityParams.get(COMMENT_TYPE));
    }
    
    // Get system comment message
    String commentMessage = StringUtils.EMPTY;
    switch (commentType) {
      case USER:
        commentMessage = title;
        break;
      case SYSTEM:
        commentMessage = getSystemCommentMessage(COMMENT_MESSAGE_KEY, COMMENT_MESSAGE_ARGS, activityParams);
        break;
      case SYSTEM_GROUP:
        String commentMessage1 = getSystemCommentMessage(COMMENT_MESSAGE_KEY1, COMMENT_MESSAGE_ARGS1, activityParams);
        String commentMessage2 = getSystemCommentMessage(COMMENT_MESSAGE_KEY2, COMMENT_MESSAGE_ARGS2, activityParams);
        commentMessage = commentMessage1.concat("<br>").concat(commentMessage2);
        break;
    }
    
    return HTMLSanitizer.sanitize(commentMessage);
  }
  
  String getWikiActivityType(){
  	return getActivityParamValue(WikiSpaceActivityPublisher.ACTIVITY_TYPE_KEY);
  }
}
