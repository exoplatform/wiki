package org.exoplatform.wiki.notification.listener;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.command.NotificationCommand;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.notification.Utils.NotificationsUtils;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.utils.Utils;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EditWikiListener extends Listener<WikiService, Page> {

  private static final Log LOG = ExoLogger.getLogger(EditWikiListener.class);

  @Override
  public void onEvent(Event<WikiService, Page> event) {

    WikiService wikiService = event.getSource();
    Page page = event.getData();

    // How to send notification
    NotificationContext ctx = buildContext(wikiService, page);
    dispatch(ctx, NotificationsUtils.EDIT_WIKI_NOTIFICATION_ID);
  }

  private NotificationContext buildContext(WikiService wikiService, Page page) {
    String creatorId = Utils.getCurrentUser();
    String changes = null;
    try {
      changes = Utils.getWikiOnChangeContent(page);
    } catch (WikiException | DifferentiationFailedException e) {
      LOG.error("Cannot send notification email on page change - Cause : " + e.getMessage(), e);
    }

    NotificationContext ctx = NotificationContextImpl.cloneInstance()
                                                     .append(NotificationsUtils.PAGE, page)
                                                     .append(NotificationsUtils.WIKI_URL, page.getUrl())
                                                     .append(NotificationsUtils.WIKI_PAGE_NAME, page.getTitle())
                                                     .append(NotificationsUtils.WIKI_EDITOR, Utils.getIdentityUser(creatorId))
                                                     .append(NotificationsUtils.CONTENT_CHANGE, changes);

    //. Receiver
    Set<String> receivers = new HashSet<String>();

    // Task creator
    try {
      List<String> watchers = wikiService.getWatchersOfPage(page);
      if (watchers != null && watchers.size() > 0) {
        for (String watcher : watchers) {
          receivers.add(watcher);
        }
      }

    } catch (Exception e) {
      LOG.error("Cannot have list of watchers for wiki page {} ", page.getName(), e);
    }

    // Remove the user who create this comment, he should not receive the notification
    receivers.remove(creatorId);
    ctx.append(NotificationsUtils.WATCHERS, receivers);
    return ctx;
  }

  private void dispatch(NotificationContext ctx, String... pluginId) {
    List<NotificationCommand> commands = new ArrayList<>(pluginId.length);
    for (String p : pluginId) {
      commands.add(ctx.makeCommand(PluginKey.key(p)));
    }

    ctx.getNotificationExecutor().with(commands).execute(ctx);
  }
}
