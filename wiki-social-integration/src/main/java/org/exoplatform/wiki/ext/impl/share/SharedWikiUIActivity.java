package org.exoplatform.wiki.ext.impl.share;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.StringCommonUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.social.core.space.SpaceApplication;
import org.exoplatform.social.core.space.SpaceTemplate;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.space.spi.SpaceTemplateService;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.social.webui.activity.share.UISharedActivity;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.lifecycle.WebuiBindingContext;
import org.exoplatform.wiki.ext.impl.WikiSpaceActivityPublisher;
import org.exoplatform.wiki.ext.impl.WikiUIActivity;
import org.exoplatform.wiki.utils.Utils;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "war:/groovy/wiki/social-integration/plugin/space/share/SharedWikiUIActivity.gtmpl",
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
    })
public class SharedWikiUIActivity extends UISharedActivity {

  private static final Log           LOG           = ExoLogger.getLogger(WikiUIActivity.class);

  public static final String         ACTIVITY_TYPE = "SHARED_WIKI_ACTIVITY";

  public final ResourceBundleService resourceBundleService;

  public SharedWikiUIActivity() {
    resourceBundleService = CommonsUtils.getService(ResourceBundleService.class);
  }

  public String getActivityParamValue(String key) {
    String value = null;
    Map<String, String> params = getOriginalActivity().getTemplateParams();
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
    pageUrl = pageUrl.contains(":spaces") ? getPageURLFromSpace() : pageUrl;
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

  private static String getWikiAppNameInSpace(String spaceId) {
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    Space space = spaceService.getSpaceByGroupId(spaceId);
    SpaceTemplateService spaceTemplateService = CommonsUtils.getService(SpaceTemplateService.class);
    SpaceTemplate spaceTemplate = spaceTemplateService.getSpaceTemplateByName(space.getTemplate());
    List<SpaceApplication> spaceTemplateApplications = spaceTemplate.getSpaceApplicationList();
    if (spaceTemplateApplications != null) {
      for (SpaceApplication spaceApplication : spaceTemplateApplications) {
        if ("WikiPortlet".equals(spaceApplication.getPortletName())) {
          return spaceApplication.getUri();
        }
      }
    }
    return "WikiPortlet";
  }

  private String getPageURLFromSpace() {
    String spaceGroupId = getActivityParamValue(WikiSpaceActivityPublisher.PAGE_OWNER_KEY);
    String pageId = getActivityParamValue(WikiSpaceActivityPublisher.PAGE_ID_KEY);
    SpaceService spaceService = (SpaceService) PortalContainer.getInstance().getComponentInstanceOfType(SpaceService.class);
    Space space = spaceService.getSpaceByGroupId(spaceGroupId);

    StringBuffer sb = new StringBuffer("");
    if (space != null) {
      sb.append(org.exoplatform.social.webui.Utils.getSpaceHomeURL(space))
        .append("/" + getWikiAppNameInSpace(space.getGroupId()) + "/")
        .append(pageId);
    }

    return sb.toString();
  }

  String getViewChangeURL() {
    return getActivityParamValue(WikiSpaceActivityPublisher.VIEW_CHANGE_URL_KEY);
  }

  String getVerName() {
    String url = getViewChangeURL();
    StringBuilder sb = new StringBuilder();
    for (int i = url.length() - 1; i >= 0; i--) {
      char c = url.charAt(i);
      if (Character.isDigit(c)) {
        sb.insert(0, c);
      } else {
        break;
      }
    }
    return sb.toString();
  }

  String getPageExcerpt() {
    return getActivityParamValue(WikiSpaceActivityPublisher.PAGE_EXCERPT);
  }

  String getPageVersion() {
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
  
  String getWikiActivityType(){
    return getActivityParamValue(WikiSpaceActivityPublisher.ACTIVITY_TYPE_KEY);
  }
}
