/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.notification.plugin;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.wiki.notification.Utils.NotificationsUtils;

import java.util.LinkedList;

public class EditWikiNotificationPlugin extends BaseNotificationPlugin {

  public EditWikiNotificationPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return NotificationsUtils.EDIT_WIKI_NOTIFICATION_ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return true;
  }

  @Override
  public NotificationInfo makeNotification(NotificationContext ctx) {
    String creatorId = ctx.value(NotificationsUtils.WIKI_EDITOR);
    String wiki_Url = ctx.value(NotificationsUtils.WIKI_URL);
    String wiki_page_name = ctx.value(NotificationsUtils.WIKI_PAGE_NAME);
    String wiki_content_change = ctx.value(NotificationsUtils.CONTENT_CHANGE);

    return NotificationInfo.instance()
                           .to(new LinkedList<String>(ctx.value(NotificationsUtils.WATCHERS)))
                           .with(NotificationsUtils.WIKI_EDITOR.getKey(), creatorId)
                           .with(NotificationsUtils.WIKI_URL.getKey(), wiki_Url)
                           .with(NotificationsUtils.WIKI_PAGE_NAME.getKey(), wiki_page_name)
                           .with(NotificationsUtils.CONTENT_CHANGE.getKey(), wiki_content_change)
                           .key(getId())
                           .end();
  }
}
