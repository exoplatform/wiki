package org.exoplatform.wiki.upgrade;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;

public class WikiPageContentMigrationUpgradePlugin extends UpgradeProductPlugin {

  public WikiPageContentMigrationUpgradePlugin(SettingService settingService, InitParams initParams) {
    super(settingService, initParams);
  }

  @Override
  public void processUpgrade(String newVersion, String previousVersion) {

  }
}
