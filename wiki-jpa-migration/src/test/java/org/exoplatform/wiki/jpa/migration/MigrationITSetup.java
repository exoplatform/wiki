package org.exoplatform.wiki.jpa.migration;

import org.exoplatform.commons.api.persistence.DataInitializer;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.jpa.JPADataStorage;
import org.exoplatform.wiki.jpa.dao.*;
import org.exoplatform.wiki.jpa.entity.WikiEntity;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.service.impl.JCRDataStorage;

import java.io.File;
import java.io.IOException;

/**
 *
 */
@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/components-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/migration-components-configuration.xml")
})
public class MigrationITSetup extends BaseExoTestCase {

  protected JCRDataStorage jcrDataStorage;
  protected JPADataStorage jpaDataStorage;
  protected OrganizationService organizationService;
  protected MOWService mowService;
  private IndexingService indexingService;
  protected MigrationService migrationService;
  protected SettingService settingService;
  protected WikiMigrationSettingService wikiMigrationSettingService;

  protected WikiDAO        wikiDAO;
  protected PageDAO        pageDAO;
  protected PageAttachmentDAO  pageAttachmentDAO;
  protected DraftPageAttachmentDAO  draftPageAttachmentDAO;
  protected DraftPageDAO   draftPageDAO;
  protected PageVersionDAO pageVersionDAO;
  protected PageMoveDAO    pageMoveDAO;
  protected TemplateDAO    templateDAO;
  protected EmotionIconDAO emotionIconDAO;

  protected File tempFolder;

  @Override
  protected void beforeRunBare() {
    try {
      tempFolder = File.createTempFile("wiki-rdbms-addon-", "");
      tempFolder.delete();
      tempFolder.mkdir();
    } catch (IOException e) {
      tempFolder = new File("target/temp");
    }

    System.setProperty("gatein.test.output.path", tempFolder.getPath());
    System.setProperty("jcr.collaboration.index.dir", tempFolder.getPath() + "/jcr/index/repository/collaboration");
    System.setProperty("jcr.collaboration.values.dir", tempFolder.getPath() + "/jcr/values/collaboration");
    System.setProperty("jcr.collaboration.swap.dir", tempFolder.getPath() + "/jcr/swap/collaboration");

    super.beforeRunBare();
  }

  @Override
  public void setUp() {

    RequestLifeCycle.begin(this.getContainer());

    // make sure data are well initialized for each test
    DataInitializer dataInitializer = PortalContainer.getInstance().getComponentInstanceOfType(DataInitializer.class);
    dataInitializer.initData();

    jcrDataStorage = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(JCRDataStorage.class);
    jpaDataStorage = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(JPADataStorage.class);
    organizationService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(OrganizationService.class);
    mowService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(MOWService.class);
    indexingService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(IndexingService.class);
    settingService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SettingService.class);

    wikiMigrationSettingService = new WikiMigrationSettingService(settingService);
    migrationService = new MigrationService(jcrDataStorage, jpaDataStorage, organizationService, mowService, indexingService, wikiMigrationSettingService);

    // Init DAO
    wikiDAO = PortalContainer.getInstance().getComponentInstanceOfType(WikiDAO.class);
    pageDAO = PortalContainer.getInstance().getComponentInstanceOfType(PageDAO.class);
    pageAttachmentDAO = PortalContainer.getInstance().getComponentInstanceOfType(PageAttachmentDAO.class);
    draftPageAttachmentDAO = PortalContainer.getInstance().getComponentInstanceOfType(DraftPageAttachmentDAO.class);
    draftPageDAO = PortalContainer.getInstance().getComponentInstanceOfType(DraftPageDAO.class);
    pageVersionDAO = PortalContainer.getInstance().getComponentInstanceOfType(PageVersionDAO.class);
    pageMoveDAO = PortalContainer.getInstance().getComponentInstanceOfType(PageMoveDAO.class);
    templateDAO = PortalContainer.getInstance().getComponentInstanceOfType(TemplateDAO.class);
    emotionIconDAO = PortalContainer.getInstance().getComponentInstanceOfType(EmotionIconDAO.class);
    // Clean Data
    cleanDB();

    RequestLifeCycle.end();
  }

  public void tearDown() {
    RequestLifeCycle.begin(this.getContainer());

    // Clean Data
    cleanDB();

    RequestLifeCycle.end();
  }

  @Override
  protected void afterRunBare() {
    super.afterRunBare();

    tempFolder.delete();
  }

  private void cleanDB() {
    emotionIconDAO.deleteAll();
    templateDAO.deleteAll();
    pageMoveDAO.deleteAll();
    pageVersionDAO.deleteAll();
    draftPageAttachmentDAO.deleteAll();
    draftPageDAO.deleteAll();
    pageAttachmentDAO.deleteAll();
    // remove foreign keys to pages
    for (WikiEntity wikiEntity : wikiDAO.findAll()) {
      wikiEntity.setWikiHome(null);
      wikiDAO.update(wikiEntity);
    }
    pageDAO.deleteAll();
    wikiDAO.deleteAll();
  }

  protected void startSessionAs(String user) {
    Identity userIdentity = new Identity(user);
    ConversationState.setCurrent(new ConversationState(userIdentity));
  }
}
