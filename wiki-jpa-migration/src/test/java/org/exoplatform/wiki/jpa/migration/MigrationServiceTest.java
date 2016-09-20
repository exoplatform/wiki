package org.exoplatform.wiki.jpa.migration;

import org.apache.commons.io.IOUtils;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.service.IDType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.utils.JCRUtils;

import javax.jcr.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 *
 */
public class MigrationServiceTest extends MigrationITSetup {

  public void testWikiMigration() throws Exception {
    RequestLifeCycle.begin(this.getContainer());

    // Users
    UserHandler userHandler = organizationService.getUserHandler();
    User userJohn = userHandler.createUserInstance("john");
    userHandler.createUser(userJohn, false);
    User userMary = userHandler.createUserInstance("mary");
    userHandler.createUser(userMary, false);
    // Groups
    GroupHandler groupHandler = organizationService.getGroupHandler();
    Group groupSpace1 = groupHandler.createGroupInstance();
    groupSpace1.setGroupName("/spaces/space1");
    groupHandler.saveGroup(groupSpace1, false);

    startSessionAs("john");

    // Portal Wiki
    Wiki portalWiki = new Wiki(PortalConfig.PORTAL_TYPE, "intranet");
    portalWiki = jcrDataStorage.createWiki(portalWiki);
    List<PermissionEntry> wikiPermissions = new ArrayList<>();
    PermissionEntry rootWikiPermissionEntry = new PermissionEntry("root", null, IDType.USER, new Permission[]{
            new Permission(PermissionType.VIEWPAGE, true),
            new Permission(PermissionType.EDITPAGE, true),
            new Permission(PermissionType.ADMINPAGE, true),
            new Permission(PermissionType.ADMINSPACE, true)
    });
    PermissionEntry administratorsWikiPermissionEntry = new PermissionEntry("*:/platform/administrators", null, IDType.MEMBERSHIP, new Permission[]{
            new Permission(PermissionType.VIEWPAGE, true),
            new Permission(PermissionType.EDITPAGE, true),
            new Permission(PermissionType.ADMINPAGE, true),
            new Permission(PermissionType.ADMINSPACE, true)
    });
    PermissionEntry usersWikiPermissionEntry = new PermissionEntry("/platform/users", null, IDType.GROUP, new Permission[]{
            new Permission(PermissionType.VIEWPAGE, true),
            new Permission(PermissionType.EDITPAGE, false),
            new Permission(PermissionType.ADMINPAGE, false),
            new Permission(PermissionType.ADMINSPACE, false)
    });
    wikiPermissions.add(rootWikiPermissionEntry);
    wikiPermissions.add(administratorsWikiPermissionEntry);
    wikiPermissions.add(usersWikiPermissionEntry);
    jcrDataStorage.updateWikiPermission(PortalConfig.PORTAL_TYPE, "intranet", wikiPermissions);
    Page wikiHome = portalWiki.getWikiHome();
    wikiHome.setContent("Wiki Home Page updated");
    jcrDataStorage.updatePage(wikiHome);

    PermissionEntry rootPagePermissionEntry = new PermissionEntry("root", null, IDType.USER, new Permission[]{
            new Permission(PermissionType.VIEWPAGE, true),
            new Permission(PermissionType.EDITPAGE, true)
    });
    PermissionEntry administratorsPagePermissionEntry = new PermissionEntry("*:/platform/administrators", null, IDType.MEMBERSHIP, new Permission[]{
            new Permission(PermissionType.VIEWPAGE, true),
            new Permission(PermissionType.EDITPAGE, true)
    });
    PermissionEntry usersPagePermissionEntry = new PermissionEntry("/platform/users", null, IDType.GROUP, new Permission[]{
            new Permission(PermissionType.VIEWPAGE, true),
            new Permission(PermissionType.EDITPAGE, false)
    });
    // Pages
    Page page1 = new Page("Page1", "Page 1");
    page1.setAuthor("john");
    page1.setContent("Page 1 Content");
    Date createdDatePage1 = Calendar.getInstance().getTime();
    page1.setCreatedDate(createdDatePage1);
    page1.setUpdatedDate(createdDatePage1);
    page1.setPermissions(Arrays.asList(rootPagePermissionEntry, administratorsPagePermissionEntry));
    page1 = jcrDataStorage.createPage(portalWiki, wikiHome, page1);
    page1.setContent("Page 1 Content - Version 2");
    jcrDataStorage.updatePage(page1);
    jcrDataStorage.addPageVersion(page1);
    jcrDataStorage.addWatcherToPage("john", page1);
    jcrDataStorage.addWatcherToPage("mary", page1);

    startSessionAs("mary");
    Page page2 = new Page("Page2", "Page 2");
    page2.setAuthor("mary");
    page2.setContent("Page 2 Content");
    Date createdDatePage2 = Calendar.getInstance().getTime();
    page2.setCreatedDate(createdDatePage2);
    page2.setUpdatedDate(createdDatePage2);
    page2.setPermissions(Arrays.asList(rootPagePermissionEntry, usersPagePermissionEntry));
    page2 = jcrDataStorage.createPage(portalWiki, wikiHome, page2);
    Attachment attachment = new Attachment();
    attachment.setName("attachment2");
    attachment.setTitle("Attachment 2");
    attachment.setContent("attachment-content-2".getBytes());
    attachment.setCreator("mary");
    attachment.setCreatedDate(Calendar.getInstance());
    attachment.setUpdatedDate(Calendar.getInstance());
    attachment.setMimeType("text/plain");
    jcrDataStorage.addAttachmentToPage(attachment, page2);

    Page pageA = new Page("PageA", "Page A");
    pageA.setAuthor("john");
    pageA.setContent("Page A Content");
    Date createdDatePageA = Calendar.getInstance().getTime();
    pageA.setCreatedDate(createdDatePageA);
    pageA.setUpdatedDate(createdDatePageA);
    pageA.setPermissions(Arrays.asList(rootPagePermissionEntry, administratorsPagePermissionEntry));
    pageA = jcrDataStorage.createPage(portalWiki, wikiHome, pageA);

    Page pagea = new Page("Pagea", "Page a");
    pagea.setAuthor("john");
    pagea.setContent("Page a Content");
    Date createdDatePagea = Calendar.getInstance().getTime();
    pagea.setCreatedDate(createdDatePagea);
    pagea.setUpdatedDate(createdDatePagea);
    pagea.setPermissions(Arrays.asList(rootPagePermissionEntry, administratorsPagePermissionEntry));
    pagea = jcrDataStorage.createPage(portalWiki, wikiHome, pagea);

    Page pageaChild = new Page("ChildPagea", "Child Page a");
    pageaChild.setAuthor("john");
    pageaChild.setContent("Child Page a Content");
    Date createdDateChildPagea = Calendar.getInstance().getTime();
    pageaChild.setCreatedDate(createdDateChildPagea);
    pageaChild.setUpdatedDate(createdDateChildPagea);
    pageaChild.setPermissions(Arrays.asList(rootPagePermissionEntry, administratorsPagePermissionEntry));
    pageaChild = jcrDataStorage.createPage(portalWiki, pagea, pageaChild);

    // Portal Wiki 2
    Wiki portalWiki2 = new Wiki(PortalConfig.PORTAL_TYPE, "intranet2");
    portalWiki2 = jcrDataStorage.createWiki(portalWiki2);
    jcrDataStorage.updateWikiPermission(PortalConfig.PORTAL_TYPE, "intranet2", wikiPermissions);
    Page wikiHome2 = portalWiki2.getWikiHome();
    wikiHome2.setContent("Wiki Home Page updated");
    jcrDataStorage.updatePage(wikiHome2);

    // Draft Pages
    DraftPage draftPage1 = new DraftPage();
    draftPage1.setName("draftPage1");
    draftPage1.setNewPage(false);
    draftPage1.setContent("Draft Page 1 Content");
    draftPage1.setTargetPageId(page1.getId());
    Date createdDateDraftPage1 = Calendar.getInstance().getTime();
    draftPage1.setCreatedDate(createdDateDraftPage1);
    draftPage1.setUpdatedDate(createdDateDraftPage1);
    jcrDataStorage.createDraftPageForUser(draftPage1, "john");

    // Related pages between portal pages
    jcrDataStorage.addRelatedPage(page1, page2);
    jcrDataStorage.addRelatedPage(page2, pageaChild);

    // Templates
    Template template1 = new Template();
    template1.setName("Template1");
    template1.setTitle("Template 1 Title");
    template1.setDescription("Template 1 Description");
    template1.setContent("Template 1 Content");
    template1.setAuthor("john");
    Date createdDateTemplate1 = Calendar.getInstance().getTime();
    template1.setCreatedDate(createdDateTemplate1);
    template1.setUpdatedDate(createdDateTemplate1);
    jcrDataStorage.createTemplatePage(portalWiki, template1);

    // Group wiki
    Session jcrSession = mowService.getSession().getJCRSession();
    Node groupsNode = jcrSession.getRootNode().getNode("Groups");
    Node spacesNode = groupsNode.addNode("spaces");
    Node space1Node = spacesNode.addNode("space1");
    space1Node.addNode("ApplicationData");
    assertTrue(jcrSession.getRootNode().hasNode("Groups/spaces/space1/ApplicationData"));

    Wiki groupWiki = new Wiki(PortalConfig.GROUP_TYPE, "/spaces/space1");
    groupWiki = jcrDataStorage.createWiki(groupWiki);

    // Pages
    Page groupPage1 = new Page("PageGroup1", "Page Group 1");
    groupPage1.setAuthor("john");
    groupPage1.setContent("Page Group 1 Content");
    Date createdDateGroupPage1 = Calendar.getInstance().getTime();
    groupPage1.setCreatedDate(createdDateGroupPage1);
    groupPage1.setUpdatedDate(createdDateGroupPage1);
    groupPage1.setPermissions(Collections.EMPTY_LIST);
    groupPage1 = jcrDataStorage.createPage(groupWiki, groupWiki.getWikiHome(), groupPage1);
    groupPage1.setContent("Page Group 1 Content - Version 2");
    jcrDataStorage.updatePage(groupPage1);
    jcrDataStorage.addPageVersion(groupPage1);
    jcrDataStorage.addWatcherToPage("john", groupPage1);
    jcrDataStorage.addWatcherToPage("mary", groupPage1);
    Attachment groupPageAttachment = new Attachment();
    groupPageAttachment.setName("groupAttachment1");
    groupPageAttachment.setTitle("Group Attachment 1");
    groupPageAttachment.setContent("group-attachment-content-1".getBytes());
    groupPageAttachment.setCreator("john");
    groupPageAttachment.setCreatedDate(Calendar.getInstance());
    groupPageAttachment.setUpdatedDate(Calendar.getInstance());
    groupPageAttachment.setMimeType("text/plain");
    jcrDataStorage.addAttachmentToPage(groupPageAttachment, groupPage1);

    // related page from group to portal wiki
    jcrDataStorage.addRelatedPage(groupPage1, page1);

    // emotion icons
    InputStream emotionIconInputStream = this.getClass().getClassLoader().getResourceAsStream("images/thumb_up.gif");
    byte[] emotionIconImage = IOUtils.toByteArray(emotionIconInputStream);
    EmotionIcon emotionIcon1 = new EmotionIcon();
    emotionIcon1.setName("emotionIcon1");
    emotionIcon1.setImage(emotionIconImage);
    jcrDataStorage.createEmotionIcon(emotionIcon1);
    EmotionIcon emotionIcon2 = new EmotionIcon();
    emotionIcon2.setName("emotionIcon2");
    emotionIcon2.setImage(emotionIconImage);
    jcrDataStorage.createEmotionIcon(emotionIcon2);

    // checkout the emotion icon JCR root node to simulate the community state
    WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
    PageImpl emotionIconsPage = wStore.getEmotionIconsContainer();
    emotionIconsPage.makeVersionable();
    emotionIconsPage.checkout();

    // reset session
    startSessionAs(null);

    RequestLifeCycle.end();


    // DO MIGRATION
    migrationService.start();

    migrationService.getLatch().await();


    RequestLifeCycle.begin(this.getContainer());

    // check wiki
    List<Wiki> portalWikis = jpaDataStorage.getWikisByType(PortalConfig.PORTAL_TYPE);
    assertNotNull(portalWikis);
    assertEquals(2, portalWikis.size());
    Wiki wikiIntranet = portalWikis.get(0);
    assertEquals(PortalConfig.PORTAL_TYPE, wikiIntranet.getType());
    assertEquals("intranet", wikiIntranet.getOwner());
    // check wiki permissions
    List<PermissionEntry> fetchedWikiPermissions = wikiIntranet.getPermissions();
    assertNotNull(fetchedWikiPermissions);
    assertEquals(3, fetchedWikiPermissions.size());
    assertTrue(fetchedWikiPermissions.contains(rootWikiPermissionEntry));
    assertTrue(fetchedWikiPermissions.contains(administratorsWikiPermissionEntry));
    assertTrue(fetchedWikiPermissions.contains(usersWikiPermissionEntry));
    // check wiki home page
    Page fetchedWikiHome = wikiIntranet.getWikiHome();
    assertNotNull(fetchedWikiHome);
    assertEquals("Wiki Home Page updated", wikiHome.getContent());
    // check children pages
    List<Page> fetchedWikiHomeChildrenPages = jpaDataStorage.getChildrenPageOf(wikiHome);
    assertNotNull(fetchedWikiHomeChildrenPages);
    assertEquals(4, fetchedWikiHomeChildrenPages.size());
    // check page1
    Page fetchedPage1 = jpaDataStorage.getPageOfWikiByName(wikiIntranet.getType(), wikiIntranet.getOwner(), "Page1");
    assertNotNull(fetchedPage1);
    assertEquals("Page 1 Content - Version 2", fetchedPage1.getContent());
    assertEquals(2, jpaDataStorage.getVersionsOfPage(fetchedPage1).size());
    List<String> fetchedPage1Watchers = jpaDataStorage.getWatchersOfPage(fetchedPage1);
    assertNotNull(fetchedPage1Watchers);
    assertEquals(2, fetchedPage1Watchers.size());
    assertTrue(fetchedPage1Watchers.contains("john"));
    assertTrue(fetchedPage1Watchers.contains("mary"));
    List<PermissionEntry> fetchedPage1Permissions = fetchedPage1.getPermissions();
    assertNotNull(fetchedPage1Permissions);
    assertEquals(2, fetchedPage1Permissions.size());
    assertTrue(fetchedPage1Permissions.contains(rootPagePermissionEntry));
    assertTrue(fetchedPage1Permissions.contains(administratorsPagePermissionEntry));
    // check page2 and attachments
    Page fetchedPage2 = jpaDataStorage.getPageOfWikiByName(wikiIntranet.getType(), wikiIntranet.getOwner(), "Page2");
    assertNotNull(fetchedPage2);
    assertEquals("Page 2 Content", fetchedPage2.getContent());
    assertEquals(1, jpaDataStorage.getVersionsOfPage(fetchedPage2).size());
    List<Attachment> attachmentsOfPage2 = jpaDataStorage.getAttachmentsOfPage(fetchedPage2);
    assertNotNull(attachmentsOfPage2);
    assertEquals(1, attachmentsOfPage2.size());
    assertTrue(Arrays.equals("attachment-content-2".getBytes(), attachmentsOfPage2.get(0).getContent()));
    List<String> fetchedPage2Watchers = jpaDataStorage.getWatchersOfPage(fetchedPage2);
    assertNotNull(fetchedPage2Watchers);
    assertEquals(0, fetchedPage2Watchers.size());
    List<PermissionEntry> fetchedPage2Permissions = fetchedPage2.getPermissions();
    assertNotNull(fetchedPage2Permissions);
    assertEquals(2, fetchedPage2Permissions.size());
    assertTrue(fetchedPage2Permissions.contains(rootPagePermissionEntry));
    assertTrue(fetchedPage2Permissions.contains(usersPagePermissionEntry));
    // check draft pages
    List<DraftPage> johnDraftPages = jpaDataStorage.getDraftPagesOfUser("john");
    assertNotNull(johnDraftPages);
    assertEquals(1, johnDraftPages.size());
    List<DraftPage> maryDraftPages = jpaDataStorage.getDraftPagesOfUser("mary");
    assertNotNull(maryDraftPages);
    assertEquals(0, maryDraftPages.size());
    // check related pages
    List<Page> relatedPagesOfPage1 = jpaDataStorage.getRelatedPagesOfPage(fetchedPage1);
    assertNotNull(relatedPagesOfPage1);
    assertEquals(1, relatedPagesOfPage1.size());
    assertEquals("Page2", relatedPagesOfPage1.get(0).getName());
    // check template1
    Map<String, Template> fetchedTemplates = jpaDataStorage.getTemplates(new WikiPageParams(wikiIntranet.getType(), wikiIntranet.getOwner(), wikiIntranet.getId()));
    assertNotNull(fetchedTemplates);
    assertEquals(1, fetchedTemplates.size());
    Template fetchedTemplate1 = jpaDataStorage.getTemplatePage(new WikiPageParams(wikiIntranet.getType(), wikiIntranet.getOwner(), wikiIntranet.getId()), "Template1");
    assertNotNull(fetchedTemplate1);
    assertEquals("Template1", fetchedTemplate1.getName());
    assertEquals("Template 1 Title", fetchedTemplate1.getTitle());
    assertEquals("Template 1 Description", fetchedTemplate1.getDescription());
    assertEquals("Template 1 Content", fetchedTemplate1.getContent());

    // check group wiki
    List<Wiki> groupWikis = jpaDataStorage.getWikisByType(PortalConfig.GROUP_TYPE);
    assertNotNull(groupWikis);
    assertEquals(1, groupWikis.size());
    Wiki wikiSpace1 = groupWikis.get(0);
    assertEquals(PortalConfig.GROUP_TYPE, wikiSpace1.getType());
    assertEquals("/spaces/space1", wikiSpace1.getOwner());
    // check wiki home page
    Page fetchedGroupWikiHome = wikiSpace1.getWikiHome();
    assertNotNull(fetchedGroupWikiHome);
    // check children pages
    List<Page> fetchedGroupWikiHomeChildrenPages = jpaDataStorage.getChildrenPageOf(fetchedGroupWikiHome);
    assertNotNull(fetchedGroupWikiHomeChildrenPages);
    assertEquals(1, fetchedGroupWikiHomeChildrenPages.size());
    Page fetchedGroupPage1 = fetchedGroupWikiHomeChildrenPages.get(0);
    List<Attachment> attachmentsOfGroupPage1 = jpaDataStorage.getAttachmentsOfPage(fetchedGroupPage1);
    assertNotNull(attachmentsOfGroupPage1);
    assertEquals(1, attachmentsOfGroupPage1.size());
    assertTrue(Arrays.equals("group-attachment-content-1".getBytes(), attachmentsOfGroupPage1.get(0).getContent()));

    // check no more JCR data
    jcrSession = mowService.getSession().getJCRSession();
    assertFalse(jcrSession.getRootNode().hasNode("exo:applications/eXoWiki"));

    RequestLifeCycle.end();
  }
}
