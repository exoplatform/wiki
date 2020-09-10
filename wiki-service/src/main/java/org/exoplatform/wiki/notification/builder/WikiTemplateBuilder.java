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
package org.exoplatform.wiki.notification.builder;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessageUtils;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.webui.utils.TimeConvertUtils;
import org.exoplatform.wiki.notification.Utils.NotificationsUtils;
import org.exoplatform.wiki.utils.Utils;

import java.io.Writer;
import java.util.Calendar;
import java.util.Locale;

public class WikiTemplateBuilder extends AbstractTemplateBuilder {

  private TemplateProvider templateProvider;

  private boolean          pushNotification;

  public WikiTemplateBuilder(TemplateProvider templateProvider, boolean pushNotification) {
    this.templateProvider = templateProvider;
    this.pushNotification = pushNotification;
  }

  @Override
  public MessageInfo makeMessage(NotificationContext ctx) {
    NotificationInfo notification = ctx.getNotificationInfo();
    String pluginId = notification.getKey().getId();

    String language = getLanguage(notification);
    TemplateContext templateContext =
        TemplateContext.newChannelInstance(this.templateProvider.getChannelKey(), pluginId, language);

    String creatorId = notification.getValueOwnerParameter(NotificationsUtils.WIKI_EDITOR.getKey());
    String wikiUrl = notification.getValueOwnerParameter(NotificationsUtils.WIKI_URL.getKey());
    String wikiPageName = notification.getValueOwnerParameter(NotificationsUtils.WIKI_PAGE_NAME.getKey());
    String wikiContentChange = notification.getValueOwnerParameter(NotificationsUtils.CONTENT_CHANGE.getKey());
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(notification.getLastModifiedDate());
    templateContext.put("READ",
                        Boolean.parseBoolean(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read"
                                                                                                                                   : "unread");
    templateContext.put("LAST_UPDATED_TIME",
                        TimeConvertUtils.convertXTimeAgoByTimeServer(cal.getTime(),
                                                                     "EE, dd yyyy",
                                                                     new Locale(language),
                                                                     TimeConvertUtils.YEAR));
    templateContext.put("WIKI_EDITOR", creatorId);
    templateContext.put("USER", notification.getTo());
    String notificationURL = CommonsUtils.getCurrentDomain();
    templateContext.put("WIKI_URL", notificationURL + wikiUrl);
    templateContext.put("WIKI_PAGE_NAME", wikiPageName);
    templateContext.put("NOTIFICATION_ID", notification.getId());
    templateContext.put("CONTENT_CHANGE", wikiContentChange);
    //binding the exception throws by processing template
    MessageInfo messageInfo = new MessageInfo();
    if (pushNotification) {
      messageInfo.subject(wikiUrl);
    } else {
      messageInfo.subject(TemplateUtils.processSubject(templateContext));
    }
    String body = TemplateUtils.processGroovy(templateContext);
    //binding the exception throws by processing template
    ctx.setException(templateContext.getException());
    return messageInfo.body(body).end();
  }

  @Override
  protected boolean makeDigest(NotificationContext ctx, Writer writer) {
    return false;
  }
}
