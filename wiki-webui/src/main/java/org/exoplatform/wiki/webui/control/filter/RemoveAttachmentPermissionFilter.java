package org.exoplatform.wiki.webui.control.filter;

import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import java.util.Map;

public class RemoveAttachmentPermissionFilter extends UIExtensionAbstractFilter {
  public static final String ATTACHMENT_KEY = "attachmentName";

  public RemoveAttachmentPermissionFilter() {
    this(null);
  }

  public RemoveAttachmentPermissionFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    // TODO there is no permissions on attachments, why do we need a filter ?
    return true;
  }

  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
  }
}
