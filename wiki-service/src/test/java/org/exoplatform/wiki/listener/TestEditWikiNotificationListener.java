/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.wiki.listener;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.NotificationCompletionService;
import org.exoplatform.commons.api.notification.service.storage.NotificationService;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.wiki.notification.Utils.NotificationsUtils;
import org.exoplatform.wiki.notification.plugin.EditWikiNotificationPlugin;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class TestEditWikiNotificationListener {

  @Mock
  private InitParams initParams;

  @PrepareForTest({ CommonsUtils.class, PluginKey.class })
  @Test
  public void testSendNotificationToWatchersOfWiki() {
    // Given
    PowerMockito.mockStatic(CommonsUtils.class);
    when(CommonsUtils.getService(NotificationService.class)).thenReturn(null);
    when(CommonsUtils.getService(NotificationCompletionService.class)).thenReturn(null);

    Set<String> recievers = new HashSet<>();
    recievers.add("Jean");
    recievers.add("John");

    EditWikiNotificationPlugin editWikiNotificationPlugin = new EditWikiNotificationPlugin(initParams);
    NotificationContext ctx = NotificationContextImpl.cloneInstance()
                                                     .append(NotificationsUtils.WIKI_PAGE_NAME, "title")
                                                     .append(NotificationsUtils.WIKI_EDITOR, "root")
                                                     .append(NotificationsUtils.WATCHERS, recievers);

    // When
    NotificationInfo notificationInfo = editWikiNotificationPlugin.makeNotification(ctx);

    // Then
    Assert.assertEquals(2, notificationInfo.getSendToUserIds().size());
    Assert.assertTrue(notificationInfo.getSendToUserIds().contains("Jean"));
    Assert.assertTrue(notificationInfo.getSendToUserIds().contains("John"));
    Assert.assertFalse(notificationInfo.getSendToUserIds().contains("root"));
  }

}
