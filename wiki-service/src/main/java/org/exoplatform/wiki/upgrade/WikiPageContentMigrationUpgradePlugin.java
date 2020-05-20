package org.exoplatform.wiki.upgrade;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.MembershipEntry;
import org.picocontainer.Startable;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class WikiPageContentMigrationUpgradePlugin implements Startable {
//public class WikiPageContentMigrationUpgradePlugin extends UpgradeProductPlugin {

  private static final Log LOG = ExoLogger.getLogger(WikiPageContentMigrationUpgradePlugin.class);

  private PageContentMigrationService migrationService;

  public WikiPageContentMigrationUpgradePlugin(SettingService settingService, InitParams initParams) {
    //super(settingService, initParams);
    migrationService = ExoContainerContext.getService(PageContentMigrationService.class);
  }

  /*
  @Override
  public void processUpgrade(String newVersion, String previousVersion) {
    migrationService.migrateAllPages();
  }
  */

  @Override
  public void start() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    ExecutorService executorService = Executors.newSingleThreadExecutor(r -> new Thread(r, "Wiki-PageSyntaxMigration"));
    executorService.execute(() -> {
      ExoContainerContext.setCurrentContainer(container);

      try {
        ConversationState.setCurrent(new ConversationState(new Identity(IdentityConstants.SYSTEM)));

        LOG.info("== Starting Wiki page syntax migration");

        migrationService.migrateAllPages();
        migrationService.migrateAllPagesVersions();
        migrationService.migrateAllPagesTemplates();

        LOG.info("== Wiki pages syntax migration - Migration finished");
      } catch (Exception e) {
        LOG.error("Error while migrating wiki pages fom XWiki syntax to HTML", e);
      } finally {
        ConversationState.setCurrent(null);
      }
    });
  }

  @Override
  public void stop() {

  }
}
