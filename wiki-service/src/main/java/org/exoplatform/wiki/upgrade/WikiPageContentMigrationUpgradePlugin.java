package org.exoplatform.wiki.upgrade;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.picocontainer.Startable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class WikiPageContentMigrationUpgradePlugin implements Startable {
//public class WikiPageContentMigrationUpgradePlugin extends UpgradeProductPlugin {

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
      migrationService.migrateAllPages();
    });
  }

  @Override
  public void stop() {

  }
}
