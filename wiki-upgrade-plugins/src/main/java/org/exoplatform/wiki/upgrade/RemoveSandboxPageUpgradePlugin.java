package org.exoplatform.wiki.upgrade;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * User: dongpd Date: 12/11/13 Time: 1:56 PM
 */
public class RemoveSandboxPageUpgradePlugin extends UpgradeProductPlugin {

  private static final String SANDBOX_PATH = "/Groups/sandbox";

  private static Log          LOG          = ExoLogger.getLogger(RemoveSandboxPageUpgradePlugin.class.getName());

  private RepositoryService   repositoryService;

  public RemoveSandboxPageUpgradePlugin(RepositoryService repositoryService, InitParams initParams) {
    super(initParams);
    this.repositoryService = repositoryService;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    LOG.info("Start RemoveSandboxPageUpgradePlugin starting ...");

    SessionProvider sessionProvider = null;
    try {
      // Get system session
      sessionProvider = SessionProvider.createSystemProvider();
      ManageableRepository currentRepository = repositoryService.getCurrentRepository();
      Session systemSession = sessionProvider.getSession(currentRepository.getConfiguration()
                                                                          .getDefaultWorkspaceName(),
                                                         currentRepository);

      // Remove sandbox group and related data
      try {
        Node sandBoxGroup = (Node) systemSession.getItem(SANDBOX_PATH);
        sandBoxGroup.remove();
        systemSession.save();
      } catch (PathNotFoundException e) {
        LOG.info("SandBox Group does not exist...");
      }

      LOG.info("Finish RemoveSandboxPageUpgradePlugin ...");
    } catch (Exception e) {
      LOG.error("RemoveSandboxPageUpgradePlugin fail: ", e);
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return VersionComparator.isAfter(newVersion, previousVersion);
  }
}
