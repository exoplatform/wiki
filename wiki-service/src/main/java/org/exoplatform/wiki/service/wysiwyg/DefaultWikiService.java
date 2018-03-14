/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.service.wysiwyg;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.*;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.utils.Utils;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.gwt.wysiwyg.client.wiki.*;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiService;
import org.xwiki.model.reference.DocumentReference;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
public class DefaultWikiService implements WikiService {
  /**
   * Default Wiki logger to report errors correctly.
   */
  private static Log                     log                      = ExoLogger.getLogger("wiki:GWTWikiService");

  /** Execution context handler, needed for accessing the WikiContext. */
  @Inject
  private Execution                      execution;

  /**
   * The service used to create links.
   */
  @Inject
  private LinkService                    linkService;

  private org.exoplatform.wiki.service.WikiService wikiService;

  /**
   * The object used to convert between client and server entity reference.
   */
  private final EntityReferenceConverter entityReferenceConverter = new EntityReferenceConverter();

  private WikiContext getWikiContext() {
    return (WikiContext) execution.getContext().getProperty(WikiContext.WIKICONTEXT);
  }

  /**
   * {@inheritDoc}
   *
   * @see WikiService#isMultiWiki()
   */
  @Override
  public Boolean isMultiWiki() {
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @see WikiService#getVirtualWikiNames()
   */
  @Override
  public List<String> getVirtualWikiNames() {
    List<String> virtualWikiNamesList = new ArrayList<String>();
    return virtualWikiNamesList;
  }

  /**
   * {@inheritDoc}
   *
   * @see WikiService#getSpaceNames(String)
   */
  @Override
  public List<String> getSpaceNames(String wikiType) {
    List<String> spaceNames = new ArrayList<>();
    org.exoplatform.wiki.service.WikiService wikiService = ExoContainerContext.getCurrentContainer()
            .getComponentInstanceOfType(org.exoplatform.wiki.service.WikiService.class);
    try {
      Collection<Wiki> wikis = wikiService.getWikisByType(wikiType);
      for (Wiki wiki : wikis) {
        spaceNames.add(wiki.getOwner());
      }
    } catch(WikiException e) {
      log.error("Cannot get space names - Cause : " + e.getMessage(), e);
    }
    return spaceNames;
  }

  /**
   * {@inheritDoc}
   *
   * @see WikiService#getPageNames(String, String)
   */
  @Override
  public List<String> getPageNames(String wikiName, String spaceName) {
    org.exoplatform.wiki.service.WikiService wservice = (org.exoplatform.wiki.service.WikiService) PortalContainer.getComponent(org.exoplatform.wiki.service.WikiService.class);
    try {
      WikiContext wikiContext = getWikiContext();
      WikiSearchData data = new WikiSearchData("", null, wikiContext.getType(), wikiContext.getOwner());
      PageList<SearchResult> results = wservice.search(data);
      List<String> pagesNames = new ArrayList<String>();
      if(results != null) {
        List<DocumentReference> documentReferences = prepareDocumentReferenceList(results);
        List<WikiPage> wikiPages = getWikiPages(documentReferences);
        for (WikiPage page : wikiPages) {
          String pageName = page.getTitle();
          if (!pagesNames.contains(pageName)) {
            pagesNames.add(pageName);
          }
        }
      }
      return pagesNames;
    } catch (Exception e) {
      log.error("Exception happened when list pages name", e);
      throw new RuntimeException("Failed to list Wiki pages name.", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WikiService#getRecentlyModifiedPages(String, int, int)
   */
  public List<WikiPage> getRecentlyModifiedPages(String wikiName, int start, int count) {
    WikiContext wikiContext = getWikiContext();
    org.exoplatform.wiki.service.WikiService wservice =
        (org.exoplatform.wiki.service.WikiService) PortalContainer.getComponent(org.exoplatform.wiki.service.WikiService.class);

    // Create search condition by current user and recently
    WikiSearchData data = new WikiSearchData(StringUtils.EMPTY, "*", wikiContext.getType(), wikiContext.getOwner());
    // TODO add a property in the search data to get only recent pages
    //data.addPropertyConstraint(String.format(" AND (  ( exo:lastModifier='%s' ) )",
    //                           ConversationState.getCurrent().getIdentity().getUserId()));
    data.setSort("exo:lastModifiedDate");
    data.setOrder("DESC");
    data.setLimit(count);

    // Call wiki service to execute search
    try {
      PageList<SearchResult> results = wservice.search(data);
      List<DocumentReference> documentReferences = prepareDocumentReferenceList(results);
      return getWikiPages(documentReferences);
    } catch (Exception e) {
      log.error("Exception happened when searching pages", e);
      throw new RuntimeException("Failed to search Wiki pages.", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WikiService#getMatchingPages(String, String, int, int)
   */
  public List<WikiPage> getMatchingPages(String wikiName, String keyword, int start, int count) {
    String quote = "'";
    String doubleQuote = "''";
    String escapedKeyword = keyword.replaceAll(quote, doubleQuote).toLowerCase();
    org.exoplatform.wiki.service.WikiService wservice = (org.exoplatform.wiki.service.WikiService) PortalContainer.getComponent(org.exoplatform.wiki.service.WikiService.class);

    try {
      WikiContext wikiContext = getWikiContext();
      WikiSearchData data = new WikiSearchData(escapedKeyword, escapedKeyword, wikiContext.getType(), wikiContext.getOwner());
      PageList<SearchResult> results = wservice.search(data);
      List<DocumentReference> documentReferences = prepareDocumentReferenceList(results);
      return getWikiPages(documentReferences);
    } catch (Exception e) {
      log.error("Exception happened when searching pages", e);
      throw new RuntimeException("Failed to search Wiki pages.", e);
    }
  }

  /**
   * Helper function to create a list of {@link WikiPage}s from a list of
   * document references.
   *
   * @param documentReferences a list of document references
   * @return the list of {@link WikiPage}s corresponding to the given document
   *         references
   * @throws Exception if anything goes wrong while creating the list of
   *           {@link WikiPage}s
   */
  private List<WikiPage> getWikiPages(List<DocumentReference> documentReferences) throws Exception {
    org.exoplatform.wiki.service.WikiService wservice = (org.exoplatform.wiki.service.WikiService) PortalContainer.getComponent(org.exoplatform.wiki.service.WikiService.class);
    List<WikiPage> wikiPages = new ArrayList<>();
    for (DocumentReference documentReference : documentReferences) {
      WikiPage wikiPage = new WikiPage();
      wikiPage.setReference(entityReferenceConverter.convert(documentReference).getEntityReference());
      String pageId = documentReference.getName();
      String wikiOwner = documentReference.getParent().getName();
      String wikiType = documentReference.getParent().getParent().getName();
      Page page = wservice.getPageByRootPermission(wikiType, wikiOwner, pageId);
      wikiPage.setTitle(page.getTitle());
      wikiPage.setUrl(pageId);
      wikiPages.add(wikiPage);
    }
    return wikiPages;
  }

  /**
   * {@inheritDoc}
   *
   * @see WikiService#getEntityConfig(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference,
   *      ResourceReference)
   */
  public EntityConfig getEntityConfig(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference origin, ResourceReference destination) {
    return linkService.getEntityConfig(origin, destination);
  }

  /**
   * {@inheritDoc}
   *
   * @see WikiService#getAttachment(AttachmentReference)
   */
  @Override
  public Attachment getAttachment(AttachmentReference attachmentReference) {
    // Clean attachment filename to be synchronized with all attachment operations.
    String cleanedFileName = attachmentReference.getFileName();
    cleanedFileName = Utils.escapeIllegalCharacterInName(cleanedFileName);
    attachmentReference.setFileName(cleanedFileName);
    WikiPageReference pageReference = attachmentReference.getWikiPageReference();
    org.exoplatform.wiki.service.WikiService wservice = (org.exoplatform.wiki.service.WikiService) PortalContainer.getComponent(org.exoplatform.wiki.service.WikiService.class);
    Page page;
    try {
      page = wservice.getExsitedOrNewDraftPageById(pageReference.getWikiName(), pageReference.getSpaceName(), pageReference.getPageName());
      if (page == null) {
        return null;
      }

      org.exoplatform.wiki.mow.api.Attachment attachment = wservice.getAttachmentOfPageByName(cleanedFileName, page);
      if (attachment == null) {
        log.warn(String.format("Failed to get attachment: %s not found.", cleanedFileName));
        return null;
      }

      Attachment attach = new Attachment();
      attach.setReference(attachmentReference.getEntityReference());
      attach.setUrl(attachment.getDownloadURL());
      return attach;
    } catch (Exception e) {
      log.error("Failed to get attachment: there was a problem with getting the document on the server.", e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WikiService#getImageAttachments(WikiPageReference)
   */
  public List<Attachment> getImageAttachments(WikiPageReference reference) {
    List<Attachment> imageAttachments = new ArrayList<Attachment>();
    List<Attachment> allAttachments = getAttachments(reference);
    for (Attachment attachment : allAttachments) {
      if (attachment.getMimeType().startsWith("image/")) {
        imageAttachments.add(attachment);
      }
    }
    return imageAttachments;
  }

  /**
   * {@inheritDoc}
   *
   * @see WikiService#getAttachments(WikiPageReference)
   */
  @Override
  public List<Attachment> getAttachments(WikiPageReference documentReference) {
    try {
      String wikiName = documentReference.getWikiName();
      String spaceName = documentReference.getSpaceName();
      String pageName = documentReference.getPageName();

      if (log.isTraceEnabled()) {
        log.trace("Getting attachments of page : " + wikiName + "." + spaceName + "." + pageName);
      }
      List<Attachment> attachments = null;
      if (StringUtils.isNotBlank(pageName)) {
        Page page = getWikiService().getExsitedOrNewDraftPageById(wikiName,
                                                          spaceName,
                                                          StringUtils.isBlank(pageName) ? pageName
                                                                                        : TitleResolver.getId(pageName, false));
        attachments = getPageAttachments(page);
      } else {
        attachments = new ArrayList<>();
        List<Page> pages = getWikiService().getPagesOfWiki(wikiName, spaceName);
        for (Page page : pages) {
          attachments.addAll(getPageAttachments(page));
        }
      }
      if (attachments == null) {
        return Collections.emptyList();
      } else {
        return attachments;
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to retrieve the list of attachments.", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see WikiService#getUploadURL(org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference)
   */
  @Override
  public String getUploadURL(WikiPageReference documentReference) {
    StringBuilder sb = new StringBuilder();
    sb.append("/").append(PortalContainer.getCurrentPortalContainerName()).append("/");
    sb.append(PortalContainer.getCurrentRestContextName()).append("/wiki/upload/");
    sb.append(documentReference.getWikiName()).append("/").append(documentReference.getSpaceName());
    sb.append("/").append(documentReference.getPageName()).append("/");
    return sb.toString();
  }

  /**
   * {@inheritDoc}
   *
   * @see WikiService#parseLinkReference(String,
   *      org.xwiki.gwt.wysiwyg.client.wiki.EntityReference)
   */
  public ResourceReference parseLinkReference(String linkReference, org.xwiki.gwt.wysiwyg.client.wiki.EntityReference baseReference) {
    return linkService.parseLinkReference(linkReference, baseReference);
  }

  public org.exoplatform.wiki.service.WikiService getWikiService() {
    if (wikiService == null) {
      wikiService = CommonsUtils.getService(org.exoplatform.wiki.service.WikiService.class);
    }
    return wikiService;
  }

  /**
   * Helper function to prepare a list of {@link WikiPage} (with full name,
   * title, etc) from a list of search results.
   *
   * @param results the list of the search results
   * @return the list of {@link WikiPage}s corresponding to the passed names
   * @throws Exception if anything goes wrong retrieving the documents
   */
  private List<DocumentReference> prepareDocumentReferenceList(PageList<SearchResult> results) throws Exception {
    List<DocumentReference> documentReferences = new ArrayList<DocumentReference>();
    for (SearchResult result : results.getAll()) {
      String nodeName = result.getPageName();
      if (nodeName != null && nodeName.length() > 0 && nodeName.startsWith("/")) {
        nodeName = nodeName.substring(1);
      }
      WikiContext wikiContext = getWikiContext();
      log.info("Prepair DocumentReference : " + wikiContext.getType() + "@" + wikiContext.getOwner() + "@" + nodeName);
      documentReferences.add(new DocumentReference(wikiContext.getType(), wikiContext.getOwner(), nodeName));
    }
    return documentReferences;
  }

  private List<Attachment> getPageAttachments(Page page) throws WikiException {
    List<Attachment> attachments = new ArrayList<>();
    List<org.exoplatform.wiki.mow.api.Attachment> attachs = getWikiService().getAttachmentsOfPage(page);
    for (org.exoplatform.wiki.mow.api.Attachment attach : attachs) {
      AttachmentReference attachmentReference = new AttachmentReference(attach.getName(), new WikiPageReference(page.getWikiType(), page.getWikiOwner(), page.getName()));
      EntityReference entityReference = attachmentReference.getEntityReference();
      entityReference.setType(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.ATTACHMENT);
      Attachment currentAttach = new Attachment();
      currentAttach.setUrl(attach.getDownloadURL());
      currentAttach.setReference(entityReference);
      currentAttach.setMimeType(attach.getMimeType());
      attachments.add(currentAttach);
    }
    return attachments;
  }
}
