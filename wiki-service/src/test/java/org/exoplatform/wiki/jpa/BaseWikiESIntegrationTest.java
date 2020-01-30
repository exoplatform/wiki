/*
 *
 *  * Copyright (C) 2003-2015 eXo Platform SAS.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 */

package org.exoplatform.wiki.jpa;

import static org.junit.Assert.assertNotEquals;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.search.dao.IndexingOperationDAO;
import org.exoplatform.commons.search.index.IndexingOperationProcessor;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.jpa.entity.*;
import org.exoplatform.wiki.jpa.search.AttachmentIndexingServiceConnector;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 10/1/15
 */
public abstract class BaseWikiESIntegrationTest extends BaseWikiJPAIntegrationTest {
  protected InputStream                      fileResource;

  private static final Log                   LOG               = ExoLogger.getExoLogger(BaseWikiESIntegrationTest.class);

  protected IndexingService                  indexingService;

  protected IndexingOperationProcessor       indexingProcessor;

  protected JPADataStorage                   storage;

  protected IndexingOperationDAO             indexingOperationDAO;

  private PoolingHttpClientConnectionManager connectionManager = null;

  private HttpClient                         client            = null;

  private String                             urlClient;

  @Override
  protected void beforeRunBare() {
    super.beforeRunBare();

    urlClient = PropertyManager.getProperty("exo.es.search.server.url");

    connectionManager = new PoolingHttpClientConnectionManager();
    // Used to allow multiple HTTP connections to same host
    String hostAndPort = urlClient.replaceAll("http(s)?://", "");
    String[] urlParts = hostAndPort.split(":");
    HttpHost localhost = new HttpHost(urlParts[0], Integer.parseInt(urlParts[1]));
    connectionManager.setMaxPerRoute(new HttpRoute(localhost), 50);
    connectionManager.closeIdleConnections(2, TimeUnit.SECONDS);

    client = HttpClients.custom().setConnectionManager(connectionManager).build();
  }

  @Override
  protected void afterRunBare() {
    connectionManager.closeExpiredConnections();
    connectionManager.shutdown();

    super.afterRunBare();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    fileResource = this.getClass().getClassLoader().getResourceAsStream("AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf");

    indexingOperationDAO = getService(IndexingOperationDAO.class);
    indexingService = getService(IndexingService.class);
    indexingProcessor = PortalContainer.getInstance().getComponentInstanceOfType(IndexingOperationProcessor.class);
    storage = PortalContainer.getInstance().getComponentInstanceOfType(JPADataStorage.class);

    // Init data
    deleteAllDocumentsInES();
    cleanIndexesFromDB();
    SecurityUtils.setCurrentUser("BCH", "*:/admin");
  }

  private void deleteAllDocumentsInES() {
    indexingService.unindexAll(WikiPageIndexingServiceConnector.TYPE);
    indexingService.unindexAll(AttachmentIndexingServiceConnector.TYPE);
    indexingProcessor.process();
    refreshWiki();
  }

  private void cleanIndexesFromDB() {
    indexingOperationDAO.deleteAll();
    indexingProcessor.process();
    refreshWiki();
  }

  protected void refreshWiki() {
    HttpGet request = new HttpGet(urlClient + "/wiki_alias/_refresh");
    LOG.info("Refreshing ES by calling {}", request.getURI());
    try {
      HttpResponse response = client.execute(request);
      assertEquals(200, response.getStatusLine().getStatusCode());
    } catch (Exception e) {
      LOG.warn("Error refreshing indices", e);
    }
  }

  protected PageEntity indexPage(String name,
                                 String title,
                                 String content,
                                 String comment,
                                 String owner,
                                 List<PermissionEntity> permissions) {
    WikiEntity wiki = getOrCreateWikiTestBCHEntity();
    PageEntity page = new PageEntity();
    page.setName(name);
    page.setTitle(title);
    page.setContent(content);
    page.setComment(comment);
    page.setOwner(owner);
    page.setPermissions(permissions);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setUrl("/url/to/my/wikiPage");
    page.setWiki(wiki);
    page = pageDAO.create(page);
    assertNotEquals(page.getId(), 0);
    indexingService.index(WikiPageIndexingServiceConnector.TYPE, Long.toString(page.getId()));
    indexingProcessor.process();
    refreshWiki();
    return page;
  }

  protected PageAttachmentEntity indexAttachment(String title, InputStream inputStream, String owner) throws IOException {
    WikiEntity wiki = getOrCreateWikiTestBCHEntity();
    PageEntity page = new PageEntity();
    page.setName("wikiPage");
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setUrl("/url/to/my/wikiPage");
    page.setWiki(wiki);
    pageDAO.create(page);
    PageAttachmentEntity attachment = new PageAttachmentEntity();
    FileItem fileItem = null;

    byte[] bytes = IOUtil.getStreamContentAsBytes(inputStream);
    try {
      fileItem = new FileItem(null,
                              title,
                              null,
                              JPADataStorage.WIKI_FILES_NAMESPACE_NAME,
                              bytes.length,
                              new Date(),
                              owner,
                              false,
                              new ByteArrayInputStream(bytes));
      fileItem = fileService.writeFile(fileItem);
    } catch (Exception e) {
      fail(e);
    }
    attachment.setAttachmentFileID(fileItem.getFileInfo().getId());
    attachment.setCreatedDate(new Date());
    attachment.setPage(page);
    attachment = pageAttachmentDAO.create(attachment);
    assertNotEquals(attachment.getId(), 0);
    indexingService.index(AttachmentIndexingServiceConnector.TYPE, Long.toString(attachment.getId()));
    indexingProcessor.process();
    refreshWiki();
    return attachment;
  }

  private WikiEntity getOrCreateWikiTestBCHEntity() {
    WikiEntity wiki = wikiDAO.getWikiByTypeAndOwner("test", "BCH");
    if (wiki == null) {
      wiki = new WikiEntity();
      wiki.setType("test");
      wiki.setOwner("BCH");
      wikiDAO.create(wiki);
    }
    return wiki;
  }

}
