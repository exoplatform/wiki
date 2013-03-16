package org.exoplatform.wiki.upgrade.nodetype;

import java.io.InputStream;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class WikiNodeTypeUpgradePlugin extends UpgradeProductPlugin {
  private Log log = ExoLogger.getLogger(this.getClass());
  
  public WikiNodeTypeUpgradePlugin(InitParams initParams){
    super(initParams);
  }
  
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    checkToRegisterNodetype();
  }
  
  private void registerNodeTypes(String nodeTypeFilesName, int alreadyExistsBehaviour) throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    ConfigurationManager configurationService = (ConfigurationManager) container.getComponentInstanceOfType(ConfigurationManager.class);
    InputStream nodetypeDefinition = configurationService.getInputStream(nodeTypeFilesName);
    RepositoryService repositoryService = (RepositoryService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(RepositoryService.class);
    ExtendedNodeTypeManager ntManager =   repositoryService.getCurrentRepository().getNodeTypeManager();
    log.info("\nTrying register node types from xml-file " + nodeTypeFilesName);
    ntManager.registerNodeTypes(nodetypeDefinition, alreadyExistsBehaviour, NodeTypeDataManager.TEXT_XML);
    log.info("\nNode types were registered from xml-file " + nodeTypeFilesName);
  }
  
  private void checkToRegisterNodetype() {
    try {
      registerNodeTypes("jar:/conf/portal/wiki-nodetypes.xml", ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
    } catch (Exception e) {
      log.warn("Can not check and register wiki's nodetype", e);
    }
  }
  
  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return VersionComparator.isAfter(newVersion, previousVersion);
  }
}
