package org.exoplatform.wiki.webui.control.filter;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;

import java.util.Map;

public class RemoveAttachmentPermissionFilter extends UIExtensionAbstractFilter {
  public static final String ATTACHMENT_KEY = "attachmentName";

  private WikiService wikiService;

  public RemoveAttachmentPermissionFilter() {
    this(null);
  }

  public RemoveAttachmentPermissionFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
    this.wikiService = CommonsUtils.getService(WikiService.class);

  }

  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
    if (pageParams == null) {
      return false;
    }

    if (pageParams.getOwner() == null || pageParams.getPageName() == null || pageParams.getType() == null) {
      return false;
    }
    Page page = wikiService.getPageOfWikiByName(pageParams.getType(), pageParams.getOwner(), pageParams.getPageName());
    if (page == null) {
      return false;
    }

    if (ConversationState.getCurrent() == null) {
      throw new IllegalStateException("No Identity found: ConversationState.getCurrent() is null");
    }
    if (ConversationState.getCurrent().getIdentity()==null) {
      throw new IllegalStateException("No Identity found: ConversationState.getCurrent().getIdentity() is null");
    }
    return wikiService.hasPermissionOnPage(page, PermissionType.EDITPAGE, ConversationState.getCurrent().getIdentity());

  }

  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
  }
}
