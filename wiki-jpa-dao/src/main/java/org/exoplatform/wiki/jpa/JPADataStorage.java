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

import static org.exoplatform.wiki.jpa.EntityConverter.convertAttachmentEntityToAttachment;
import static org.exoplatform.wiki.jpa.EntityConverter.convertAttachmentToDraftPageAttachmentEntity;
import static org.exoplatform.wiki.jpa.EntityConverter.convertAttachmentToPageAttachmentEntity;
import static org.exoplatform.wiki.jpa.EntityConverter.convertDraftPageEntityToDraftPage;
import static org.exoplatform.wiki.jpa.EntityConverter.convertDraftPageToDraftPageEntity;
import static org.exoplatform.wiki.jpa.EntityConverter.convertEmotionIconEntityToEmotionIcon;
import static org.exoplatform.wiki.jpa.EntityConverter.convertPageEntityToPage;
import static org.exoplatform.wiki.jpa.EntityConverter.convertPageToPageEntity;
import static org.exoplatform.wiki.jpa.EntityConverter.convertPageVersionEntityToPageVersion;
import static org.exoplatform.wiki.jpa.EntityConverter.convertPermissionEntitiesToPermissionEntries;
import static org.exoplatform.wiki.jpa.EntityConverter.convertPermissionEntriesToPermissionEntities;
import static org.exoplatform.wiki.jpa.EntityConverter.convertTemplateEntityToTemplate;
import static org.exoplatform.wiki.jpa.EntityConverter.convertTemplateToTemplateEntity;
import static org.exoplatform.wiki.jpa.EntityConverter.convertWikiEntityToWiki;
import static org.exoplatform.wiki.jpa.EntityConverter.convertWikiToWikiEntity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.api.persistence.DataInitializer;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.dao.DraftPageAttachmentDAO;
import org.exoplatform.wiki.jpa.dao.DraftPageDAO;
import org.exoplatform.wiki.jpa.dao.EmotionIconDAO;
import org.exoplatform.wiki.jpa.dao.PageAttachmentDAO;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.dao.PageMoveDAO;
import org.exoplatform.wiki.jpa.dao.PageVersionDAO;
import org.exoplatform.wiki.jpa.dao.TemplateDAO;
import org.exoplatform.wiki.jpa.dao.WikiDAO;
import org.exoplatform.wiki.jpa.entity.AttachmentEntity;
import org.exoplatform.wiki.jpa.entity.DraftPageAttachmentEntity;
import org.exoplatform.wiki.jpa.entity.DraftPageEntity;
import org.exoplatform.wiki.jpa.entity.EmotionIconEntity;
import org.exoplatform.wiki.jpa.entity.PageAttachmentEntity;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.PageMoveEntity;
import org.exoplatform.wiki.jpa.entity.PageVersionEntity;
import org.exoplatform.wiki.jpa.entity.TemplateEntity;
import org.exoplatform.wiki.jpa.entity.WikiEntity;
import org.exoplatform.wiki.jpa.search.WikiElasticSearchServiceConnector;
import org.exoplatform.wiki.mow.api.Attachment;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.EmotionIcon;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.PageVersion;
import org.exoplatform.wiki.mow.api.Permission;
import org.exoplatform.wiki.mow.api.PermissionEntry;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.mow.api.Template;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.service.DataStorage;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.SearchResultType;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.VersionNameComparatorDesc;
import org.exoplatform.wiki.utils.WikiConstants;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 9/8/15
 */
public class JPADataStorage implements DataStorage {
  public static final String WIKI_TYPE_DRAFT = "draft";

  public static final String WIKI_FILES_NAMESPACE_NAME = "wiki";
  public static final String WIKI_FILES_NAMESPACE_DESCRIPTION = "wiki application files";

  private WikiDAO        wikiDAO;
  private PageDAO        pageDAO;
  private PageAttachmentDAO  pageAttachmentDAO;
  private DraftPageAttachmentDAO draftPageAttachmentDAO;
  private DraftPageDAO   draftPageDAO;
  private PageVersionDAO pageVersionDAO;
  private PageMoveDAO    pageMoveDAO;
  private TemplateDAO    templateDAO;
  private EmotionIconDAO emotionIconDAO;
  private FileService fileService;

  /**
   * JPADataStorage must depends on DataInitializer to make sure data structure is created before initializing it
   */
  public JPADataStorage(WikiDAO wikiDAO,
                        PageDAO pageDAO,
                        PageAttachmentDAO pageAttachmentDAO,
                        DraftPageAttachmentDAO draftPageAttachmentDAO,
                        DraftPageDAO draftPageDAO,
                        PageVersionDAO pageVersionDAO,
                        PageMoveDAO pageMoveDAO,
                        TemplateDAO templateDAO,
                        EmotionIconDAO emotionIconDAO,
                        DataInitializer dataInitializer,
                        FileService fileService) {
    this.wikiDAO = wikiDAO;
    this.pageDAO = pageDAO;
    this.pageAttachmentDAO = pageAttachmentDAO;
    this.draftPageAttachmentDAO = draftPageAttachmentDAO;
    this.draftPageDAO = draftPageDAO;
    this.pageVersionDAO = pageVersionDAO;
    this.pageMoveDAO = pageMoveDAO;
    this.templateDAO = templateDAO;
    this.emotionIconDAO = emotionIconDAO;
    this.fileService = fileService;
  }

  @Override
  public PageList<SearchResult> search(WikiSearchData wikiSearchData) {
    WikiElasticSearchServiceConnector searchService = PortalContainer.getInstance().getComponentInstanceOfType(WikiElasticSearchServiceConnector.class);

    //Trick to add the "/" at the beginning of the wiki owner
    String wikiOwner = wikiSearchData.getWikiOwner();
    if (wikiSearchData.getWikiType().toUpperCase().equals(WikiType.GROUP.name())) {
      wikiOwner = pageDAO.validateGroupWikiOwner(wikiOwner);
    }

    List<SearchResult> searchResults = searchService.searchWiki(getSearchedText(wikiSearchData),
        wikiSearchData.getWikiType(),
        wikiOwner,
        (int) wikiSearchData.getOffset(),
        wikiSearchData.getLimit(),
        wikiSearchData.getSort(),
        wikiSearchData.getOrder());

    return new ObjectPageList<>(searchResults, searchResults.size());
  }

  private String getSearchedText(WikiSearchData wikiSearchData) {
    String searchText = "";
    if (StringUtils.isNotBlank(wikiSearchData.getTitle())) {
      searchText = wikiSearchData.getTitle();
    } else if (StringUtils.isNotBlank(wikiSearchData.getContent())) {
      searchText = wikiSearchData.getContent();
    }
    return searchText;
  }

  private SearchResult toSearchResult(org.exoplatform.commons.api.search.data.SearchResult input) {
    SearchResult output = new SearchResult();
    output.setTitle(input.getTitle());
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(input.getDate());
    output.setCreatedDate(cal);
    output.setExcerpt(input.getExcerpt());
    output.setPageName(input.getTitle());
    output.setUrl(input.getUrl());
    return output;
  }

  @Override
  public Wiki getWikiByTypeAndOwner(String wikiType, String wikiOwner) throws WikiException {
    return convertWikiEntityToWiki(wikiDAO.getWikiByTypeAndOwner(wikiType, wikiOwner));
  }

  @Override
  public List<Wiki> getWikisByType(String wikiType) throws WikiException {
    List<Wiki> wikis = new ArrayList();
    for (WikiEntity wikiEntity : wikiDAO.getWikisByType(wikiType)) {
      wikis.add(convertWikiEntityToWiki(wikiEntity));
    }
    return wikis;
  }

  @Override
  @ExoTransactional
  public Wiki createWiki(Wiki wiki) throws WikiException {
    WikiEntity createdWikiEntity = wikiDAO.create(convertWikiToWikiEntity(wiki, wikiDAO));
    Wiki createdWiki = convertWikiEntityToWiki(createdWikiEntity);

    // create wiki home page
    Page wikiHomePage = new Page();
    wikiHomePage.setWikiType(wiki.getType());
    wikiHomePage.setWikiOwner(wiki.getOwner());
    wikiHomePage.setName(WikiConstants.WIKI_HOME_NAME);
    wikiHomePage.setTitle(WikiConstants.WIKI_HOME_TITLE);
    Date now = Calendar.getInstance().getTime();
    wikiHomePage.setCreatedDate(now);
    wikiHomePage.setUpdatedDate(now);
    wikiHomePage.setContent("= Welcome to " + wiki.getOwner() + " =");
    // inherit syntax from wiki
    wikiHomePage.setSyntax(createdWiki.getPreferences().getWikiPreferencesSyntax().getDefaultSyntax());
    // inherit home page permissions from wiki permissions
    List<PermissionEntry> homePagePermissions = new ArrayList<>();
    List<PermissionEntry> wikiPermissions = createdWiki.getPermissions();
    for(PermissionEntry wikiPermission : wikiPermissions) {
      PermissionEntry homePagePermission = new PermissionEntry(wikiPermission.getId(), wikiPermission.getFullName(), wikiPermission.getIdType(), null);
      List<Permission> newPermissions = new ArrayList<>();
      for(Permission permission : wikiPermission.getPermissions()) {
        if(permission.getPermissionType().equals(PermissionType.VIEWPAGE) || permission.getPermissionType().equals(PermissionType.EDITPAGE)) {
          newPermissions.add(permission);
        }
      }
      homePagePermission.setPermissions(newPermissions.toArray(new Permission[]{}));
      homePagePermissions.add(homePagePermission);
    }
    wikiHomePage.setPermissions(homePagePermissions);

    Page createdWikiHomePage = createPage(createdWiki, null, wikiHomePage);
    createdWiki.setWikiHome(createdWikiHomePage);

    return createdWiki;
  }

  @Override
  @ExoTransactional
  public Page createPage(Wiki wiki, Page parentPage, Page page) throws WikiException {
    WikiEntity wikiEntity = wikiDAO.getWikiByTypeAndOwner(wiki.getType(), wiki.getOwner());
    if (wikiEntity == null) {
      throw new WikiException("Cannot create page " + wiki.getType() + ":" + wiki.getOwner() + ":" + page.getName()
          + " because wiki does not exist.");
    }

    PageEntity parentPageEntity = null;
    if (parentPage != null) {
      parentPageEntity = pageDAO.getPageOfWikiByName(wiki.getType(), wiki.getOwner(), parentPage.getName());
      if (parentPageEntity == null) {
        throw new WikiException("Cannot create page " + wiki.getType() + ":" + wiki.getOwner() + ":" + page.getName()
            + " because parent page " + parentPage.getName() + " does not exist.");
      }
    }
    PageEntity pageEntity = convertPageToPageEntity(page, wikiDAO);
    pageEntity.setWiki(wikiEntity);
    pageEntity.setParentPage(parentPageEntity);

    Date now = GregorianCalendar.getInstance().getTime();
    if (pageEntity.getCreatedDate() == null) {
      pageEntity.setCreatedDate(now);
    }
    if (pageEntity.getUpdatedDate() == null) {
      pageEntity.setUpdatedDate(now);
    }

    pageEntity.setDeleted(false);

    PageEntity createdPageEntity = pageDAO.create(pageEntity);

    // if the page to create is the wiki home, update the wiki
    if (parentPage == null) {
      wikiEntity.setWikiHome(createdPageEntity);
      wikiDAO.update(wikiEntity);
    }

    return convertPageEntityToPage(createdPageEntity);
  }

  @Override
  public Page getPageOfWikiByName(String wikiType, String wikiOwner, String pageName) throws WikiException {
    //getCurrentNewDraftWikiPage from org.exoplatform.wiki.commons.Utils can call this method with wikiType
    // and wikiOwner null. This will cause an error in the pageDAO
    if(wikiType == null || wikiOwner == null) return null;
    if(WIKI_TYPE_DRAFT.equals(wikiType)) {
      return convertDraftPageEntityToDraftPage(draftPageDAO.findLatestDraftPageByUserAndName(wikiOwner, pageName));
    } else {
      return convertPageEntityToPage(pageDAO.getPageOfWikiByName(wikiType, wikiOwner, pageName));
    }
  }

  @Override
  public Page getPageById(String id) throws WikiException {
    return convertPageEntityToPage(pageDAO.find(Long.parseLong(id)));
  }

  @Override
  public DraftPage getDraftPageById(String id) throws WikiException {
    return convertDraftPageEntityToDraftPage(draftPageDAO.find(Long.parseLong(id)));
  }

  @Override
  public Page getParentPageOf(Page page) throws WikiException {
    Page parentPage = null;

    PageEntity childPageEntity = null;
    if (page.getId() != null && !page.getId().isEmpty()) {
      childPageEntity = pageDAO.find(Long.parseLong(page.getId()));
    } else {
      childPageEntity = pageDAO.getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());
    }

    if (childPageEntity != null) {
      parentPage = convertPageEntityToPage(childPageEntity.getParentPage());
    }

    return parentPage;
  }

  @Override
  public List<Page> getChildrenPageOf(Page page) throws WikiException {
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());
    if (pageEntity == null) {
      throw new WikiException("Cannot get children of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
          + page.getName() + " because page does not exist.");
    }

    List<Page> childrenPages = new ArrayList<>();
    List<PageEntity> childrenPagesEntities = pageDAO.getChildrenPages(pageEntity);
    if (childrenPagesEntities != null) {
      for (PageEntity childPageEntity : childrenPagesEntities) {
        childrenPages.add(convertPageEntityToPage(childPageEntity));
      }
    }

    return childrenPages;
  }

  @Override
  @ExoTransactional
  public void deletePage(String wikiType, String wikiOwner, String pageName) throws WikiException {
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(wikiType, wikiOwner, pageName);
    if (pageEntity == null) {
      throw new WikiException("Cannot delete page " + wikiType + ":" + wikiOwner + ":" + pageName
          + " because page does not exist.");
    }

    // delete the page and all its children pages (listeners call on delete page
    // event is done on service layer)
    deletePageEntity(pageEntity);
  }

  /**
   * Recursively deletes a page and all its children pages
   *
   * @param pageEntity the root page to delete
   */
  private void deletePageEntity(PageEntity pageEntity) {
    List<PageEntity> childrenPages = pageDAO.getChildrenPages(pageEntity);
    if (childrenPages != null) {
      for (PageEntity childPage : childrenPages) {
        deletePageEntity(childPage);
      }
    }

    pageEntity.setDeleted(true);
    pageDAO.update(pageEntity);
  }

  @Override
  public void createTemplatePage(Wiki wiki, Template template) throws WikiException {
    template.setWikiId(wiki.getId());
    template.setWikiType(wiki.getType());
    template.setWikiOwner(wiki.getOwner());
    Date createdDate = template.getCreatedDate();
    Date updatedDate = template.getUpdatedDate();
    if(createdDate == null || updatedDate == null) {
      Date now = Calendar.getInstance().getTime();
      if(createdDate == null) {
        template.setCreatedDate(now);
      }
      if(updatedDate == null) {
        template.setUpdatedDate(now);
      }
    }
    templateDAO.create(convertTemplateToTemplateEntity(template, wikiDAO));
  }

  @Override
  public void updateTemplatePage(Template template) throws WikiException {
    TemplateEntity templateEntity;
    if (template.getId() != null && !template.getId().isEmpty()) {
      templateEntity = templateDAO.find(Long.parseLong(template.getId()));
    } else {
      templateEntity = templateDAO.getTemplateOfWikiByName(template.getWikiType(), template.getWikiOwner(), template.getName());
    }

    if (templateEntity == null) {
      throw new WikiException("Cannot update template " + template.getWikiType() + ":" + template.getWikiOwner() + ":"
          + template.getName() + " because template does not exist.");
    }

    templateEntity.setName(template.getName());
    templateEntity.setTitle(template.getTitle());
    templateEntity.setDescription(template.getDescription());
    templateEntity.setContent(template.getContent());
    templateEntity.setSyntax(template.getSyntax());
    templateEntity.setUpdatedDate(Calendar.getInstance().getTime());

    templateDAO.update(templateEntity);
  }

  @Override
  public void deleteTemplatePage(String wikiType, String wikiOwner, String templateName) throws WikiException {
    TemplateEntity templateEntity = templateDAO.getTemplateOfWikiByName(wikiType, wikiOwner, templateName);
    if (templateEntity == null) {
      throw new WikiException("Cannot delete template " + wikiType + ":" + wikiOwner + ":" + templateName
          + " because template does not exist.");
    }

    templateDAO.delete(templateEntity);
  }

  @Override
  public Template getTemplatePage(WikiPageParams params, String templateName) throws WikiException {
    TemplateEntity templateEntity = templateDAO.getTemplateOfWikiByName(params.getType(), params.getOwner(), templateName);
    return convertTemplateEntityToTemplate(templateEntity);
  }

  @Override
  public Map<String, Template> getTemplates(WikiPageParams wikiPageParams) throws WikiException {
    Map<String, Template> templates = new HashMap<>();

    List<TemplateEntity> templatesEntities = templateDAO.getTemplatesOfWiki(wikiPageParams.getType(), wikiPageParams.getOwner());
    if (templatesEntities != null) {
      for (TemplateEntity templateEntity : templatesEntities) {
        templates.put(templateEntity.getName(), convertTemplateEntityToTemplate(templateEntity));
      }
    }

    return templates;
  }

  @Override
  public void deleteDraftOfPage(Page page, String username) throws WikiException {
    List<DraftPageEntity> draftPages = draftPageDAO.findDraftPagesByUserAndTargetPage(username, Long.valueOf(page.getId()));
    for (DraftPageEntity draftPage: draftPages) {
      if(draftPage != null){
        deleteAttachmentsOfDraftPage(draftPage);
      }
    }
    draftPageDAO.deleteDraftPagesByUserAndTargetPage(username, Long.valueOf(page.getId()));
  }

  @Override
  public void deleteDraftByName(String draftPageName, String username) throws WikiException {
    DraftPageEntity draftPage = draftPageDAO.findLatestDraftPageByUserAndName(username, draftPageName);
    if(draftPage != null){
      deleteAttachmentsOfDraftPage(draftPage);
    }
    draftPageDAO.deleteDraftPagesByUserAndName(draftPageName, username);
  }

  @Override
  @ExoTransactional
  public void renamePage(String wikiType, String wikiOwner, String pageName, String newName, String newTitle) throws WikiException {
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(wikiType, wikiOwner, pageName);
    if (pageEntity == null) {
      throw new WikiException("Cannot rename page " + wikiType + ":" + wikiOwner + ":" + pageName
          + " because page does not exist.");
    }

    // save the move in the page moves history
    List<PageMoveEntity> pageMoves = pageEntity.getMoves();
    if(pageMoves == null) {
      pageMoves = new ArrayList<>();
    }
    PageMoveEntity move = new PageMoveEntity(wikiType, wikiOwner, pageName, Calendar.getInstance().getTime());
    move.setPage(pageEntity);
    pageMoves.add(move);
    // move must be saved here because of Hibernate bug HHH-6776
    pageMoveDAO.create(move);

    pageEntity.setName(newName);
    pageEntity.setTitle(newTitle);
    pageEntity.setMoves(pageMoves);

    pageDAO.update(pageEntity);
  }

  @Override
  @ExoTransactional
  public void movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws WikiException {
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(currentLocationParams.getType(),
        currentLocationParams.getOwner(),
        currentLocationParams.getPageName());
    if (pageEntity == null) {
      throw new WikiException("Cannot move page " + currentLocationParams.getType() + ":" + currentLocationParams.getOwner()
          + ":" + currentLocationParams.getPageName() + " because page does not exist.");
    }

    PageEntity destinationPageEntity = pageDAO.getPageOfWikiByName(newLocationParams.getType(),
        newLocationParams.getOwner(),
        newLocationParams.getPageName());
    if (destinationPageEntity == null) {
      throw new WikiException("Cannot move page " + currentLocationParams.getType() + ":" + currentLocationParams.getOwner()
          + ":" + currentLocationParams.getPageName() + " to page " + newLocationParams.getType() + ":"
          + newLocationParams.getOwner() + ":" + newLocationParams.getPageName() + " because destination page does not exist.");
    }

    // save the move in the page moves history
    List<PageMoveEntity> pageMoves = pageEntity.getMoves();
    if(pageMoves == null) {
      pageMoves = new ArrayList<>();
    }
    PageMoveEntity move = new PageMoveEntity(currentLocationParams.getType(), currentLocationParams.getOwner(),
        currentLocationParams.getPageName(), Calendar.getInstance().getTime());
    move.setPage(pageEntity);
    // move must be saved here because of Hibernate bug HHH-6776
    pageMoveDAO.create(move);

    pageEntity.setParentPage(destinationPageEntity);
    updateWikiOfPageTree(destinationPageEntity.getWiki(), pageEntity);

    pageMoves.add(move);
    pageEntity.setMoves(pageMoves);

    pageDAO.update(pageEntity);
  }

  /**
   * Recursively update wiki of children pages
   * @param wikiEntity The new wiki
   * @param pageEntity The page to update
   */
  private void updateWikiOfPageTree(WikiEntity wikiEntity, PageEntity pageEntity) {
    pageEntity.setWiki(wikiEntity);

    List<PageEntity> childrenPages = pageDAO.getChildrenPages(pageEntity);
    if(childrenPages != null) {
      for (PageEntity childrenPageEntity : childrenPages) {
        updateWikiOfPageTree(wikiEntity, childrenPageEntity);
      }
    }
  }

  @Override
  public List<PermissionEntry> getWikiPermission(String wikiType, String wikiOwner) throws WikiException {
    WikiEntity wikiEntity = wikiDAO.getWikiByTypeAndOwner(wikiType, wikiOwner);

    if (wikiEntity == null) {
      throw new WikiException("Cannot get permissions of wiki " + wikiType + ":" + wikiOwner + " because wiki does not exist.");
    }

    return convertPermissionEntitiesToPermissionEntries(wikiEntity.getPermissions(),
        Arrays.asList(PermissionType.VIEWPAGE, PermissionType.EDITPAGE, PermissionType.ADMINPAGE, PermissionType.ADMINSPACE));
  }

  @Override
  public void updateWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws WikiException {
    WikiEntity wikiEntity = wikiDAO.getWikiByTypeAndOwner(wikiType, wikiOwner);

    if (wikiEntity == null) {
      throw new WikiException("Cannot update permissions of wiki " + wikiType + ":" + wikiOwner + " because wiki does not exist.");
    }

    wikiEntity.setPermissions(convertPermissionEntriesToPermissionEntities(permissionEntries));

    wikiDAO.update(wikiEntity);
  }

  @Override
  public List<Page> getRelatedPagesOfPage(Page page) throws WikiException {
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());

    if (pageEntity == null) {
      throw new WikiException("Cannot get related pages of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
          + page.getName() + " because page does not exist.");
    }

    List<Page> relatedPages = new ArrayList<>();
    List<PageEntity> relatedPagesEntities = pageEntity.getRelatedPages();
    if (relatedPagesEntities != null) {
      for (PageEntity relatedPageEntity : relatedPagesEntities) {
        relatedPages.add(convertPageEntityToPage(relatedPageEntity));
      }
    }

    return relatedPages;
  }

  @Override
  public Page getRelatedPage(String wikiType, String wikiOwner, String pageName) throws WikiException {
    Page relatedPage = null;
    List<PageMoveEntity> pageMoveEntities = pageMoveDAO.findInPageMoves(wikiType, wikiOwner, pageName);
    if(pageMoveEntities != null && !pageMoveEntities.isEmpty()) {
      // take first result
      relatedPage = convertPageEntityToPage(pageMoveEntities.get(0).getPage());
    }
    return relatedPage;
  }

  @Override
  public void addRelatedPage(Page page, Page relatedPage) throws WikiException {
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());

    if (pageEntity == null) {
      throw new WikiException("Cannot add related page to page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
          + page.getName() + " because page does not exist.");
    }

    PageEntity relatedPageEntity = pageDAO.getPageOfWikiByName(relatedPage.getWikiType(),
        relatedPage.getWikiOwner(),
        relatedPage.getName());

    if (relatedPageEntity == null) {
      throw new WikiException("Cannot add related page " + relatedPage.getWikiType() + ":" + relatedPage.getWikiOwner() + ":"
          + relatedPage.getName() + " of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName()
          + " because related page does not exist.");
    }

    List<PageEntity> relatedPages = pageEntity.getRelatedPages();
    if (relatedPages == null) {
      relatedPages = new ArrayList<>();
    }
    relatedPages.add(relatedPageEntity);
    pageEntity.setRelatedPages(relatedPages);

    pageDAO.update(pageEntity);
  }

  @Override
  public List<Page> getPagesOfWiki(String wikiType, String wikiOwner) {
    if (StringUtils.isBlank(wikiOwner)) {
      throw new IllegalArgumentException("wikiOwner is mandatory argument");
    }
    if (StringUtils.isBlank(wikiType)) {
      throw new IllegalArgumentException("wikiType is mandatory argument");
    }
    List<PageEntity> pagesOfWiki = pageDAO.getPagesOfWiki(wikiType, wikiOwner);
    List<Page> pages = new ArrayList<>();
    for (PageEntity pageEntity : pagesOfWiki) {
      pages.add(convertPageEntityToPage(pageEntity));
    }
    return pages;
  }

  @Override
  public void removeRelatedPage(Page page, Page relatedPage) throws WikiException {
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());

    if (pageEntity == null) {
      throw new WikiException("Cannot remove related page to page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
          + page.getName() + " because page does not exist.");
    }

    PageEntity relatedPageEntity = pageDAO.getPageOfWikiByName(relatedPage.getWikiType(),
        relatedPage.getWikiOwner(),
        relatedPage.getName());

    if (relatedPageEntity == null) {
      throw new WikiException("Cannot remove related page " + relatedPage.getWikiType() + ":" + relatedPage.getWikiOwner() + ":"
          + relatedPage.getName() + " of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName()
          + " because related page does not exist.");
    }

    List<PageEntity> relatedPages = pageEntity.getRelatedPages();
    if (relatedPages != null) {
      for (int i = 0; i < relatedPages.size(); i++) {
        if (relatedPages.get(i).getId() == relatedPageEntity.getId()) {
          relatedPages.remove(i);
          break;
        }
      }
      pageEntity.setRelatedPages(relatedPages);
      pageDAO.update(pageEntity);
    }
  }

  @Override
  public Page getExsitedOrNewDraftPageById(String wikiType, String wikiOwner, String pageName, String username) throws WikiException {
    DraftPage draftPage;

    if(pageName.contains(Utils.SPLIT_TEXT_OF_DRAFT_FOR_NEW_PAGE)) {
      String[] pageNameParts = pageName.split(Utils.SPLIT_TEXT_OF_DRAFT_FOR_NEW_PAGE);
      username = pageNameParts[0];
    }

    draftPage = getDraft(pageName, username);

    if (draftPage == null) {
      Date now = GregorianCalendar.getInstance().getTime();
      // create draft page for non existing draft page
      draftPage = new DraftPage();
      draftPage.setWikiType(PortalConfig.USER_TYPE);
      draftPage.setWikiOwner(username);
      draftPage.setName(pageName);
      draftPage.setAuthor(username);
      draftPage.setNewPage(true);
      draftPage.setCreatedDate(now);
      draftPage.setUpdatedDate(now);

      if(wikiType != null && wikiOwner != null) {
        Page targetPage = getPageOfWikiByName(wikiType, wikiOwner, pageName);
        if (targetPage != null) {
          draftPage.setTargetPageId(targetPage.getId());
          draftPage.setTargetPageRevision("1");
        }
      }

      createDraftPageForUser(draftPage, username);
    }

    return draftPage;
  }

  @Override
  public DraftPage getDraft(WikiPageParams wikiPageParams, String username) throws WikiException {
    DraftPage latestDraft = null;

    Page page = getPageOfWikiByName(wikiPageParams.getType(), wikiPageParams.getOwner(), wikiPageParams.getPageName());

    if (page != null) {
      List<DraftPageEntity> draftPagesOfUser = draftPageDAO.findDraftPagesByUserAndTargetPage(username,
          Long.valueOf(page.getId()));

      DraftPageEntity latestDraftEntity = null;
      for (DraftPageEntity draft : draftPagesOfUser) {
        // Compare and get the latest draft
        if ((latestDraftEntity == null) || (latestDraftEntity.getUpdatedDate().getTime() < draft.getUpdatedDate().getTime())) {
          latestDraftEntity = draft;
        }
      }
      latestDraft = convertDraftPageEntityToDraftPage(latestDraftEntity);
    } else {
      throw new WikiException("Cannot get draft of page " + wikiPageParams.getType() + ":" + wikiPageParams.getOwner() + ":"
          + wikiPageParams.getPageName() + " because page does not exist.");
    }

    return latestDraft;
  }

  @Override
  public DraftPage getLastestDraft(String username) throws WikiException {
    DraftPageEntity draftPagEntity = draftPageDAO.findLatestDraftPageByUser(username);
    return convertDraftPageEntityToDraftPage(draftPagEntity);
  }

  @Override
  public DraftPage getDraft(String draftName, String username) throws WikiException {
    DraftPage draftPageOfUser = null;
    List<DraftPage> draftPages = getDraftPagesOfUser(username);
    if (draftPages != null) {
      for (DraftPage draftPage : draftPages) {
        if (draftPage.getName() != null && draftPage.getName().equals(draftName)) {
          draftPageOfUser = draftPage;
          break;
        }
      }
    }
    return draftPageOfUser;
  }

  @Override
  public List<DraftPage> getDraftPagesOfUser(String username) throws WikiException {
    List<DraftPage> draftPages = new ArrayList<>();
    List<DraftPageEntity> draftPagesEntities = draftPageDAO.findDraftPagesByUser(username);
    if (draftPagesEntities != null) {
      for (DraftPageEntity draftPageEntity : draftPagesEntities) {
        draftPages.add(convertDraftPageEntityToDraftPage(draftPageEntity));
      }
    }
    return draftPages;
  }

  @Override
  public void createDraftPageForUser(DraftPage draftPage, String username) throws WikiException {
    DraftPageEntity draftPageEntity = convertDraftPageToDraftPageEntity(draftPage, pageDAO);
    draftPageEntity.setAuthor(username);
    draftPageDAO.create(draftPageEntity);
  }

  @Override
  public List<TemplateSearchResult> searchTemplate(TemplateSearchData templateSearchData) throws WikiException {

    String wikiOwner = templateSearchData.getWikiOwner();
    if(templateSearchData.getWikiType().toUpperCase().equals(WikiType.GROUP.toString())) {
      wikiOwner = templateDAO.validateGroupWikiOwner(wikiOwner);
    }

    List<TemplateEntity> templates = templateDAO.searchTemplatesByTitle(templateSearchData.getWikiType(),
        wikiOwner,
        templateSearchData.getTitle());

    List<TemplateSearchResult> searchResults = new ArrayList<>();
    if (templates != null) {
      for (TemplateEntity templateEntity : templates) {
        Calendar createdDateCalendar = null;
        Date createdDate = templateEntity.getCreatedDate();
        if(createdDate != null) {
          createdDateCalendar = Calendar.getInstance();
          createdDateCalendar.setTime(createdDate);
        }
        Calendar updatedDateCalendar = null;
        Date updatedDate = templateEntity.getUpdatedDate();
        if(updatedDate != null) {
          updatedDateCalendar = Calendar.getInstance();
          updatedDateCalendar.setTime(updatedDate);
        }
        TemplateSearchResult templateSearchResult = new TemplateSearchResult(templateEntity.getWiki().getType(),
            templateEntity.getWiki().getOwner(),
            templateEntity.getName(),
            templateEntity.getTitle(),
            SearchResultType.TEMPLATE,
            updatedDateCalendar,
            createdDateCalendar,
            null);
        searchResults.add(templateSearchResult);
      }
    }

    return searchResults;
  }

  @Override
  public List<Attachment> getAttachmentsOfPage(Page page) throws WikiException {
    List<AttachmentEntity> attachmentsEntities;
    String wikiType;
    String wikiOwner;
    String pageName;
    if (page instanceof DraftPage) {
      DraftPageEntity draftPageEntity = draftPageDAO.findLatestDraftPageByUserAndName(page.getAuthor(), page.getName());
      if (draftPageEntity == null) {
        throw new WikiException("Cannot get attachments of draft page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
            + page.getName() + " because draft page does not exist.");
      }
      attachmentsEntities = new ArrayList<>();
      List<DraftPageAttachmentEntity> draftPageAttachmentEntities = draftPageEntity.getAttachments();
      if (draftPageAttachmentEntities != null) attachmentsEntities.addAll(draftPageAttachmentEntities);
      if(draftPageEntity.isNewPage()) {
        wikiType = WIKI_TYPE_DRAFT;
        wikiOwner = draftPageEntity.getAuthor();
        pageName = draftPageEntity.getName();
      } else {
        PageEntity targetPage = draftPageEntity.getTargetPage();
        WikiEntity wiki = targetPage.getWiki();
        wikiType = wiki.getType();
        wikiOwner = wiki.getOwner();
        pageName = targetPage.getName();
      }
    } else {
      PageEntity pageEntity = fetchPageEntity(page);
      if (pageEntity == null) {
        throw new WikiException("Cannot get attachments of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
            + page.getName() + " because page does not exist.");
      }
      attachmentsEntities = new ArrayList<>();
      List<PageAttachmentEntity> pageAttachmentEntities = pageEntity.getAttachments();
      if (pageAttachmentEntities != null) attachmentsEntities.addAll(pageAttachmentEntities);
      WikiEntity wikiEntity = pageEntity.getWiki();
      wikiType = wikiEntity.getType();
      wikiOwner = wikiEntity.getOwner();
      pageName = pageEntity.getName();
    }

    List<Attachment> attachments = new ArrayList<>();
    if (attachmentsEntities != null) {
      for (AttachmentEntity attachmentEntity : attachmentsEntities) {
        Attachment attachment = convertAttachmentEntityToAttachment(fileService, attachmentEntity);
        // set title and full title if not there
        if (attachment.getTitle() == null || StringUtils.isEmpty(attachment.getTitle())) {
          int index = attachment.getName().lastIndexOf(".");
          if (index != -1) {
            attachment.setTitle(attachment.getName().substring(0, index));
          } else {
            attachment.setTitle(attachment.getName());
          }
        }
        if (attachment.getFullTitle() == null || StringUtils.isEmpty(attachment.getFullTitle())) {
          attachment.setFullTitle(attachment.getName());
        }
        // build download url
        attachment.setDownloadURL(getDownloadURL(wikiType, wikiOwner, pageName, attachment));
        attachments.add(attachment);
      }
    }

    return attachments;
  }

  @Override
  @ExoTransactional
  public void addAttachmentToPage(Attachment attachment, Page page) throws WikiException {

    if(page instanceof DraftPage) {

      DraftPageAttachmentEntity attachmentEntity = convertAttachmentToDraftPageAttachmentEntity(fileService, attachment);
      Date now = GregorianCalendar.getInstance().getTime();
      if (attachmentEntity.getCreatedDate() == null) {
        attachmentEntity.setCreatedDate(now);
      }

      DraftPageEntity draftPageEntity = draftPageDAO.findLatestDraftPageByUserAndName(Utils.getCurrentUser(), page.getName());

      if (draftPageEntity == null) {
        throw new WikiException("Cannot add an attachment to draft page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
            + page.getName() + " because draft page does not exist.");
      }

      attachmentEntity.setDraftPage(draftPageEntity);

      // attachment must be saved here because of Hibernate bug HHH-6776
      draftPageAttachmentDAO.create(attachmentEntity);

      List<DraftPageAttachmentEntity> attachmentsEntities = draftPageEntity.getAttachments();
      if (attachmentsEntities == null) {
        attachmentsEntities = new ArrayList<>();
      }
      DraftPageAttachmentEntity draftPageAttachmentEntity = attachmentEntity;
      draftPageAttachmentEntity.setDraftPage(draftPageEntity);
      attachmentsEntities.add(draftPageAttachmentEntity);
      draftPageEntity.setAttachments(attachmentsEntities);
      draftPageDAO.update(draftPageEntity);
    } else {

      PageAttachmentEntity attachmentEntity = convertAttachmentToPageAttachmentEntity(fileService, attachment);
      Date now = GregorianCalendar.getInstance().getTime();
      if (attachmentEntity.getCreatedDate() == null) {
        attachmentEntity.setCreatedDate(now);
      }

      PageEntity pageEntity = fetchPageEntity(page);

      if (pageEntity == null) {
        throw new WikiException("Cannot add an attachment to page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
            + page.getName() + " because page does not exist.");
      }
      attachmentEntity.setPage(pageEntity);

      // attachment must be saved here because of Hibernate bug HHH-6776
      pageAttachmentDAO.create(attachmentEntity);

      List<PageAttachmentEntity> attachmentsEntities = pageEntity.getAttachments();
      if (attachmentsEntities == null) {
        attachmentsEntities = new ArrayList<>();
      }

      PageAttachmentEntity pageAttachmentEntity = attachmentEntity;
      pageAttachmentEntity.setPage(pageEntity);
      attachmentsEntities.add(pageAttachmentEntity);
      pageEntity.setAttachments(attachmentsEntities);
      pageDAO.update(pageEntity);
    }
  }

  @Override
  @ExoTransactional
  public void deleteAttachmentOfPage(String attachmentName, Page page) throws WikiException {
    PageEntity pageEntity = fetchPageEntity(page);
    DraftPageEntity draftPageEntity = null;
    if(pageEntity == null) {
      draftPageEntity = draftPageDAO.findLatestDraftPageByUserAndName(Utils.getCurrentUser(),page.getName());
    }
    
    if (pageEntity == null && draftPageEntity == null) {
      throw new WikiException("Cannot delete an attachment of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
          + page.getName() + " because page does not exist.");
    }
    
    boolean attachmentFound = false;
    if (pageEntity == null && draftPageEntity != null) {
      attachmentFound = deleteDraftPageAttachementEntity(attachmentName, draftPageEntity, attachmentFound);
    }
    else {
      attachmentFound = deletePageAttachementEntity(attachmentName, pageEntity, attachmentFound);
    }

    if (!attachmentFound) {
      throw new WikiException("Cannot delete the attachment " + attachmentName + " of page " + page.getWikiType() + ":"
          + page.getWikiOwner() + ":" + page.getName() + " because attachment does not exist.");
    }
  }

  private boolean deletePageAttachementEntity(String attachmentName, PageEntity pageEntity, boolean attachmentFound) {
    List<PageAttachmentEntity> attachmentsEntities = pageEntity.getAttachments();
    if (attachmentsEntities != null) {
      for (int i = 0; i < attachmentsEntities.size(); i++) {
        AttachmentEntity attachmentEntity = attachmentsEntities.get(i);
        String name = null;
        if(attachmentEntity.getAttachmentFileID() != null){
           name = fileService.getFileInfo(attachmentEntity.getAttachmentFileID()).getName();
        }
        if (name != null && name.equals(attachmentName)) {
          attachmentFound = true;
          attachmentsEntities.remove(i);
          
          fileService.deleteFile(attachmentEntity.getAttachmentFileID());
          pageAttachmentDAO.delete((PageAttachmentEntity) attachmentEntity);
 
          pageEntity.setAttachments(attachmentsEntities);
          pageDAO.update(pageEntity);
          break;
        }
      }
    }
    return attachmentFound;
  }

  private boolean deleteDraftPageAttachementEntity(String attachmentName,
                                                   DraftPageEntity draftPageEntity,
                                                   boolean attachmentFound) {
    List<DraftPageAttachmentEntity> draftAttachmentsEntities = draftPageEntity.getAttachments();
    if (draftAttachmentsEntities != null) {
      for (int i = 0; i < draftAttachmentsEntities.size(); i++) {
        AttachmentEntity attachmentEntity = draftAttachmentsEntities.get(i);
        String name = null;
        if(attachmentEntity.getAttachmentFileID() != null){
           name = fileService.getFileInfo(attachmentEntity.getAttachmentFileID()).getName();
        }
        if (name != null && name.equals(attachmentName)) {
          attachmentFound = true;
          draftAttachmentsEntities.remove(i);

          fileService.deleteFile(attachmentEntity.getAttachmentFileID());
          draftPageAttachmentDAO.delete((DraftPageAttachmentEntity) attachmentEntity);

          draftPageEntity.setAttachments(draftAttachmentsEntities);
          draftPageDAO.update(draftPageEntity);
          break;
        }
      }
    }
    return attachmentFound;
  }

  @Override
  public Page getHelpSyntaxPage(String syntaxId, boolean fullContent, List<ValuesParam> syntaxHelpParams, ConfigurationManager configurationManager) throws WikiException {
    Page helpPage = null;
    if(syntaxHelpParams != null) {
      for(ValuesParam syntaxHelpParam : syntaxHelpParams) {
        String syntaxName = syntaxHelpParam.getName();
        if(syntaxName.equals(syntaxId)) {
          try {
            List<String> syntaxValues = syntaxHelpParam.getValues();
            String filePath;
            if(fullContent) {
              filePath = syntaxValues.get(1);
            } else {
              filePath = syntaxValues.get(0);
            }
            InputStream helpFile = configurationManager.getInputStream(filePath);
            helpPage = new Page();
            String realName = syntaxId.replace("/", "");
            helpPage.setName(realName + " Help Page");
            helpPage.setSyntax(syntaxId);

            StringBuilder stringContent = new StringBuilder();
            BufferedReader bufferReader;
            bufferReader = new BufferedReader(new InputStreamReader(helpFile));
            String tempLine;
            while ((tempLine = bufferReader.readLine()) != null) {
              stringContent.append(tempLine).append("\n");
            }
            helpPage.setContent(stringContent.toString());

          } catch(Exception e) {
            throw new WikiException("Cannot get help page " + syntaxId, e);
          }
        }
      }
    }
    return helpPage;
  }

  @Override
  public void createEmotionIcon(EmotionIcon emotionIcon) throws WikiException {
    EmotionIconEntity emotionIconEntity = new EmotionIconEntity();
    emotionIconEntity.setName(emotionIcon.getName());
    emotionIconEntity.setImage(emotionIcon.getImage());

    emotionIconDAO.create(emotionIconEntity);
  }

  @Override
  @ExoTransactional
  public List<EmotionIcon> getEmotionIcons() throws WikiException {
    List<EmotionIcon> emotionIcons = new ArrayList<>();
    List<EmotionIconEntity> emotionIconsEntities = emotionIconDAO.findAll();
    if (emotionIconsEntities != null) {
      for (EmotionIconEntity emotionIconEntity : emotionIconsEntities) {
        emotionIcons.add(convertEmotionIconEntityToEmotionIcon(emotionIconEntity));
      }
    }
    return emotionIcons;
  }

  @Override
  public EmotionIcon getEmotionIconByName(String emotionIconName) throws WikiException {
    return convertEmotionIconEntityToEmotionIcon(emotionIconDAO.getEmotionIconByName(emotionIconName));
  }

  @Override
  public boolean hasPermissionOnPage(Page page, PermissionType permissionType, Identity identity) throws WikiException {
    String userId = identity.getUserId();
    if (userId.equals(IdentityConstants.SYSTEM)) {
      // SYSTEM has permission everywhere
      return true;
    } else if (userId.equals(page.getOwner())) {
      // Current user is owner of the page so has all privileges
      return true;
    }

    List<PermissionEntry> pagePermissions = page.getPermissions();
    if(pagePermissions == null) {
      Page fetchedPage;
      if(page.getId() != null && !page.getId().isEmpty()) {
        fetchedPage = getPageById(page.getId());
      } else {
        fetchedPage = getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());
      }
      pagePermissions = fetchedPage.getPermissions();
    }

    if(pagePermissions == null || pagePermissions.isEmpty()) {
      // no permissions on the page
      return true;
    } else {
      return hasPermission(pagePermissions, identity, permissionType);
    }
  }

  @Override
  public boolean hasPermissionOnWiki(Wiki wiki, PermissionType permissionType, Identity identity) throws WikiException {
    if(wiki == null) {
      throw new WikiException("Wiki cannot be null");
    }

    String userId = identity.getUserId();
    if (userId.equals(IdentityConstants.SYSTEM)) {
      // SYSTEM has permission everywhere
      return true;
    } else if (wiki.getType().equals(PortalConfig.USER_TYPE) && userId.equals(wiki.getOwner())) {
      // Current user is owner of the wiki so has all privileges
      return true;
    }

    List<PermissionEntry> wikiPermissions = wiki.getPermissions();
    if (wikiPermissions == null || wikiPermissions.isEmpty()) {
      // no permissions on the wiki
      return true;
    } else {
      return hasPermission(wikiPermissions, identity, permissionType);
    }
  }

  @Override
  public boolean hasAdminSpacePermission(String wikiType, String owner, Identity identity) throws WikiException {
      Wiki wiki = getWikiByTypeAndOwner(wikiType, owner);
      return hasPermissionOnWiki(wiki, PermissionType.ADMINSPACE, identity);
  }

  @Override
  public boolean hasAdminPagePermission(String wikiType, String owner, Identity identity) throws WikiException {
    Wiki wiki = getWikiByTypeAndOwner(wikiType, owner);
    return hasPermissionOnWiki(wiki, PermissionType.ADMINPAGE, identity);
  }

  /**
   * Check if the identity has the permission of type permissionType in the resourcePermissions
   * @param resourcePermissions List of permissions of the resource (wiki, page, ...)
   * @param identity The identity of the user
   * @param permissionType The permission type to check
   * @return true of the user has the given permission type in the list of the given permission entries
   */
  private boolean hasPermission(List<PermissionEntry> resourcePermissions, Identity identity, PermissionType permissionType) {
    String userId = identity.getUserId();
    // for each permission set on the page
    for(PermissionEntry pagePermission : resourcePermissions) {
      // for each type of permission (VIEWPAGE, EDITPAGE, ...)
      for(Permission permission : pagePermission.getPermissions()) {
        // if the permission type equals the type we want to test
        if(permission.isAllowed() && permission.getPermissionType().equals(permissionType)) {
          // if the user belongs to this identity (user, membership or any)
          if(IdentityConstants.ANY.equals(pagePermission.getId())) {
            return true;
          } else {
            switch(pagePermission.getIdType()) {
              case USER:
                if(userId.equals(pagePermission.getId())) {
                  return true;
                }
              case GROUP:
                if(identity.isMemberOf(pagePermission.getId())) {
                  return true;
                }
              case MEMBERSHIP:
                UserACL.Permission membershipPermission = new UserACL.Permission();
                membershipPermission.setPermissionExpression(pagePermission.getId());
                if(identity.isMemberOf(membershipPermission.getGroupId(), membershipPermission.getMembership())) {
                  return true;
                }
            }
          }
        }
      }
    }
    return false;
  }

  @Override
  public List<PageVersion> getVersionsOfPage(Page page) throws WikiException {
    PageEntity pageEntity = fetchPageEntity(page);

    if (pageEntity == null) {
      throw new WikiException("Cannot get versions of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName()
          + " because page does not exist.");
    }

    List<PageVersion> pageVersions = new ArrayList<>();
    List<PageVersionEntity> pageVersionEntities = pageEntity.getVersions();
    if(pageVersionEntities != null) {
      for (PageVersionEntity pageVersionEntity : pageVersionEntities) {
        pageVersions.add(convertPageVersionEntityToPageVersion(pageVersionEntity));
      }
    }

    Collections.sort(pageVersions, new VersionNameComparatorDesc());

    return pageVersions;
  }

  @Override
  @ExoTransactional
  public void addPageVersion(Page page) throws WikiException {
    if(page != null) {
      PageEntity pageEntity = fetchPageEntity(page);

      if (pageEntity == null) {
        throw new WikiException("Cannot add version of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName()
            + " because page does not exist.");
      }

      PageVersionEntity pageVersionEntity = new PageVersionEntity();

      Long versionNumber = pageVersionDAO.getLastversionNumberOfPage(pageEntity.getId());
      if(versionNumber == null) {
        versionNumber = 1L;
      } else {
        versionNumber = versionNumber + 1;
      }

      pageVersionEntity.setPage(pageEntity);
      pageVersionEntity.setVersionNumber(versionNumber);
      pageVersionEntity.setName(pageEntity.getName());
      pageVersionEntity.setTitle(pageEntity.getTitle());
      pageVersionEntity.setAuthor(pageEntity.getAuthor());
      pageVersionEntity.setContent(pageEntity.getContent());
      pageVersionEntity.setMinorEdit(pageEntity.isMinorEdit());
      pageVersionEntity.setComment(pageEntity.getComment());
      Date now = Calendar.getInstance().getTime();
      pageVersionEntity.setCreatedDate(now);
      pageVersionEntity.setUpdatedDate(now);

      // attachment must be saved here because of Hibernate bug HHH-6776
      pageVersionDAO.create(pageVersionEntity);

      List<PageVersionEntity> pageVersionEntities = pageEntity.getVersions();
      if (pageVersionEntities == null) {
        pageVersionEntities = new ArrayList<>();
      }

      pageVersionEntities.add(pageVersionEntity);
      pageEntity.setVersions(pageVersionEntities);

      pageDAO.update(pageEntity);
    } else {
      throw new WikiException("Cannot create version of a page null");
    }
  }

  @Override
  public void restoreVersionOfPage(String versionName, Page page) throws WikiException {
    if(page != null) {
      PageEntity pageEntity = fetchPageEntity(page);

      if (pageEntity == null) {
        throw new WikiException("Cannot restore version of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName()
            + " because page does not exist.");
      }

      PageVersionEntity versionToRestore = pageVersionDAO.getPageversionByPageIdAndVersion(Long.parseLong(page.getId()), Long.parseLong(versionName));
      if(versionToRestore != null) {
        pageEntity.setContent(versionToRestore.getContent());
        pageEntity.setUpdatedDate(Calendar.getInstance().getTime());
        pageDAO.update(pageEntity);
      } else {
        throw new WikiException("Cannot restore version " + versionName + " of a page " + page.getWikiType() + ":"
            + page.getWikiOwner() + ":" + page.getName() + " because version does not exist.");
      }
    } else {
      throw new WikiException("Cannot restore version of a page null");
    }
  }

  @Override
  @ExoTransactional
  public void updatePage(Page page) throws WikiException {
    if(page instanceof DraftPage) {
      DraftPageEntity draftPageEntity = draftPageDAO.findLatestDraftPageByUserAndName(page.getAuthor(), page.getName());

      if (draftPageEntity == null) {
        throw new WikiException("Cannot add an attachment to draft page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
            + page.getName() + " because draft page does not exist.");
      }

      draftPageEntity.setTitle(page.getTitle());
      draftPageEntity.setContent(page.getContent());
      draftPageEntity.setUpdatedDate(page.getUpdatedDate());

      draftPageDAO.update(draftPageEntity);
    } else {
      PageEntity pageEntity = fetchPageEntity(page);

      if (pageEntity == null) {
        throw new WikiException("Cannot update page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName()
            + " because page does not exist.");
      }

      pageEntity.setName(page.getName());
      pageEntity.setTitle(page.getTitle());
      pageEntity.setAuthor(page.getAuthor());
      pageEntity.setContent(page.getContent());
      pageEntity.setSyntax(page.getSyntax());
      pageEntity.setCreatedDate(page.getCreatedDate());
      pageEntity.setUpdatedDate(page.getUpdatedDate());
      pageEntity.setMinorEdit(page.isMinorEdit());
      pageEntity.setComment(page.getComment());
      pageEntity.setUrl(page.getUrl());
      pageEntity.setPermissions(convertPermissionEntriesToPermissionEntities(page.getPermissions()));
      pageEntity.setActivityId(page.getActivityId());

      pageDAO.update(pageEntity);
    }
  }

  @Override
  public List<String> getPreviousNamesOfPage(Page page) throws WikiException {
    PageEntity pageEntity = fetchPageEntity(page);

    if (pageEntity == null) {
      throw new WikiException("Cannot get previous names of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
          + page.getName() + " because page does not exist.");
    }

    List<String> previousPageName = new ArrayList<>();
    List<PageMoveEntity> moves = pageEntity.getMoves();
    if(moves != null) {
      for(PageMoveEntity pageMoveEntity : moves) {
        previousPageName.add(pageMoveEntity.getPageName());
      }
    }

    return previousPageName;
  }

  @Override
  public List<String> getWatchersOfPage(Page page) throws WikiException {
    PageEntity pageEntity = fetchPageEntity(page);

    if (pageEntity == null) {
      throw new WikiException("Cannot get watchers of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
          + page.getName() + " because page does not exist.");
    }
    return pageEntity.getWatchers() == null ? null : new ArrayList<>(pageEntity.getWatchers());
  }

  @Override
  public void addWatcherToPage(String username, Page page) throws WikiException {
    PageEntity pageEntity = fetchPageEntity(page);

    if (pageEntity == null) {
      throw new WikiException("Cannot add a watcher on page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
          + page.getName() + " because page does not exist.");
    }
    if (pageEntity.getWatchers() == null) {
      throw new WikiException("Cannot add a watcher on page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
          + page.getName() + " because list of watchers is null.");
    }
    pageEntity.getWatchers().add(username);
    pageDAO.update(pageEntity);
  }

  @Override
  public void deleteWatcherOfPage(String username, Page page) throws WikiException {
    PageEntity pageEntity = fetchPageEntity(page);

    if (pageEntity == null) {
      throw new WikiException("Cannot delete a watcher of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
          + page.getName() + " because page does not exist.");
    }

    Set<String> watchers = pageEntity.getWatchers();
    if (watchers != null && watchers.contains(username)) {
      watchers.remove(username);
      pageEntity.setWatchers(watchers);
      pageDAO.update(pageEntity);
    } else {
      throw new WikiException("Cannot remove watcher " + username + " of page " + page.getWikiType() + ":" + page.getWikiOwner()
          + ":" + page.getName() + " because watcher does not exist.");
    }
  }

  @ExoTransactional
  public void deleteAttachmentsOfDraftPage(DraftPageEntity page) throws WikiException {
    List<DraftPageAttachmentEntity> attachmentsEntities = page.getAttachments();
    if (attachmentsEntities != null) {
      for (int i = 0; i < attachmentsEntities.size(); i++) {
        AttachmentEntity attachmentEntity = attachmentsEntities.get(i);
        attachmentsEntities.remove(i);
        fileService.deleteFile(attachmentEntity.getAttachmentFileID());
        draftPageAttachmentDAO.delete((DraftPageAttachmentEntity) attachmentEntity);
      }
      page.setAttachments(attachmentsEntities);
      draftPageDAO.update(page);
    }
  }

  /**
   * Fecth Page Entity from a Page domain object
   *
   * @param page The page domain object
   * @return The page entity
   */
  public PageEntity fetchPageEntity(Page page) {
    PageEntity pageEntity;
    Long pageId = null;
    if (page.getId() != null && !page.getId().isEmpty()) {
      try {
        pageId = Long.parseLong(page.getId());
      } catch(NumberFormatException e) {
        pageId = null;
      }
    }
    if(pageId != null) {
      pageEntity = pageDAO.find(Long.parseLong(page.getId()));
    } else {
      pageEntity = pageDAO.getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());
    }
    return pageEntity;
  }

  /**
   * Build the download URL of an attachment
   * @param attachment
   * @return
   */
  private String getDownloadURL(String wikiType, String wikiOwner, String pageName, Attachment attachment) {
    StringBuilder sb = new StringBuilder();

    sb.append(Utils.getDefaultRestBaseURI())
        .append("/wiki/attachments/")
        .append(wikiType)
        .append("/")
        .append(Utils.SPACE)
        .append("/")
        .append(wikiOwner)
        .append("/")
        .append(Utils.PAGE)
        .append("/")
        .append(pageName);
    try {
      sb.append("/").append(URLEncoder.encode(attachment.getName(), "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      sb.append("/").append(attachment.getName());
    }

    return sb.toString();
  }
}
