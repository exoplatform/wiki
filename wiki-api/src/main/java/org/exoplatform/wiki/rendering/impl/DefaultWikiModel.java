/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wiki.rendering.impl;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.mow.api.Attachment;
import org.exoplatform.wiki.mow.api.EmotionIcon;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.rendering.context.MarkupContextManager;
import org.exoplatform.wiki.service.WikiContext;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.WikiNameValidator;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.wiki.WikiModel;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;

@Component
public class DefaultWikiModel implements WikiModel {
  
  private static final Log    LOG           = ExoLogger.getLogger(DefaultWikiModel.class);
  
  /**
   * Used to get the current context
   */
  @SuppressWarnings("unused")
  @Inject
  private Execution execution;
  
  /**
   * Used to get the build context for document
   */
  @Inject
  private MarkupContextManager markupContextManager;
  
  @Override
  public String getDocumentEditURL(ResourceReference documentReference) {
    WikiContext wikiContext = getWikiContext();
    WikiContext wikiMarkupContext = markupContextManager.getMarkupContext(documentReference.getReference(),ResourceType.DOCUMENT);
    if (wikiContext != null) {
      StringBuilder sb = new StringBuilder();
      String pageTitle = wikiMarkupContext.getPageTitle();
      String wikiType = wikiMarkupContext.getType();
      String wiki = wikiMarkupContext.getOwner();
      try {
        WikiNameValidator.validate(pageTitle);
        sb.append(getDocumentViewURL(wikiContext));
        sb.append("?")
          .append(WikiContext.ACTION)
          .append("=")
          .append(WikiContext.ADDPAGE)
          .append("&")
          .append(WikiContext.PAGETITLE)
          .append("=")
          .append(pageTitle)
          .append("&")
          .append(WikiContext.WIKI)
          .append("=")
          .append(wiki)
          .append("&")
          .append(WikiContext.WIKITYPE)
          .append("=")
          .append(wikiType);
      } catch (IllegalArgumentException ex) {
        sb.append(String.format("javascript:void(0);"));
      }
      return sb.toString();
    }
    return "";
  }

  @Override
  public String getDocumentViewURL(ResourceReference documentReference) {
    WikiContext wikiMarkupContext = markupContextManager.getMarkupContext(documentReference.getReference(),ResourceType.DOCUMENT);
    return getDocumentViewURL(wikiMarkupContext);
  }

  @Override
  public String getImageURL(ResourceReference imageReference, Map<String, String> parameters) {
    String imageName = imageReference.getReference();
    StringBuilder sb = new StringBuilder();
    try {
      WikiService wikiService = ExoContainerContext.getCurrentContainer()
              .getComponentInstanceOfType(WikiService.class);

      ResourceType resourceType = ResourceType.ICON.equals(imageReference.getType()) ? ResourceType.ICON : ResourceType.ATTACHMENT;
      WikiContext wikiMarkupContext = markupContextManager.getMarkupContext(imageName, resourceType);
      String portalContainerName = PortalContainer.getCurrentPortalContainerName();
      String portalURL = wikiMarkupContext.getPortalURL();
      String domainURL = portalURL.substring(0, portalURL.indexOf("/"+portalContainerName));
      sb.append(domainURL);
      WikiContext context = getWikiContext();
      wikiService.addPageLink(new WikiPageParams(context.getType(), context.getOwner(), context.getPageName()),
                                        new WikiPageParams(wikiMarkupContext.getType(),
                                                           wikiMarkupContext.getOwner(),
                                                           wikiMarkupContext.getPageName()));
      wikiService.addPageLink(new WikiPageParams(context.getType(), context.getOwner(), context.getPageName()),
                                        new WikiPageParams(wikiMarkupContext.getType(),
                                                           wikiMarkupContext.getOwner(),
                                                           wikiMarkupContext.getPageName(),
                                                           wikiMarkupContext.getAttachmentName()));
      Page page;
      String attachmentName = wikiMarkupContext.getAttachmentName();
      if (ResourceType.ATTACHMENT.equals(resourceType)) {
        page = wikiService.getExsitedOrNewDraftPageById(wikiMarkupContext.getType(), wikiMarkupContext.getOwner(), wikiMarkupContext.getPageName());

        Attachment att = wikiService.getAttachmentOfPageByName(attachmentName, page);
        Boolean exportToPdf = (parameters == null)? false : Boolean.valueOf(parameters.get(RenderingService.EXPORT_TO_PDF));
        if (att != null) {
          if ((exportToPdf) && att.getMimeType().startsWith("image/")) {
            // PDF rendering is done on server side from the HTML content, when it comes getting restricted by permissions images, the server will call a rest service
            // anonymously and we cannot check the permissions. So for the PDF export images will be alternatively created and fetched from a temporary file
            File image = Files.createTempFile(attachmentName + Long.toString(System.currentTimeMillis()), null).toFile();
            try (OutputStream outStream = new FileOutputStream(image)) {
              outStream.write(att.getContent());
            }
            sb.setLength(0);
            sb.append(image.toURI().toURL().toString());
            image.deleteOnExit();
          } else {
            sb.append(att.getDownloadURL());
          }
        }
      } else {
        EmotionIcon emotionIcon = wikiService.getEmotionIconByName(attachmentName);
        if(emotionIcon != null) {
          sb.append("/rest/wiki/emoticons/" + emotionIcon.getName());
        }
      }
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Couldn't get attachment URL for attachment: " + imageName, e);
      }
    }
    return sb.toString();
  }

  @Override
  public String getLinkURL(ResourceReference linkReference) {
    return getImageURL(linkReference, null);
  }

  @Override
  public boolean isDocumentAvailable(ResourceReference documentReference) {
    // Should look for pages in the model with the given title
    // (Page.findPageByTitle())
    WikiService wikiService = ExoContainerContext.getCurrentContainer()
                                                               .getComponentInstanceOfType(WikiService.class);
    Page page = null;
    String documentName = documentReference.getReference();
    ResourceType type = documentReference.getType();
    WikiContext wikiMarkupContext = markupContextManager.getMarkupContext(documentName, type);
    WikiContext wikiContext = getWikiContext();
    try {
      wikiService.addPageLink(new WikiPageParams(wikiContext.getType(), wikiContext.getOwner(), wikiContext.getPageName()),
                                        new WikiPageParams(wikiMarkupContext.getType(),
                                                           wikiMarkupContext.getOwner(),
                                                           wikiMarkupContext.getPageName()));
    } catch (Exception e) {
      LOG.warn(String.format("Failed to link incoming pages for page %s", documentReference.toString()), e);
    }
    try {
      if (wikiService.getWikiByTypeAndOwner(wikiMarkupContext.getType(), wikiMarkupContext.getOwner()) == null) {
        return false;
      } else {
        page = wikiService.getPageOfWikiByName(wikiMarkupContext.getType(),
                wikiMarkupContext.getOwner(),
                wikiMarkupContext.getPageName());
        if (page == null) {
          page = wikiService.getRelatedPage(wikiMarkupContext.getType(), wikiMarkupContext.getOwner(), wikiMarkupContext.getPageName());
          if (page != null) {
            wikiService.addPageLink(new WikiPageParams(wikiContext.getType(),
                                                                 wikiContext.getOwner(),
                                                                 wikiContext.getPageName()),
                                              new WikiPageParams(wikiMarkupContext.getType(),
                                                                 wikiMarkupContext.getOwner(),
                                                                 page.getName()));
          }
          return page != null;
        } 
      }

    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("An exception happened when checking available status of document: "
            + documentName, e);
      }
      return false;
    }
    return true;
  }

  private String getDocumentViewURL(WikiContext context) {
    try {
      WikiService wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
      Page page = wikiService.getPageOfWikiByName(context.getType(), context.getOwner(), context.getPageName());
      if (page == null) {
        page = wikiService.getRelatedPage(context.getType(), context.getOwner(), context.getPageName());
      }
      if (page != null) {
        Wiki wiki = wikiService.getWikiByTypeAndOwner(page.getWikiType(), page.getWikiOwner());
        context.setType(wiki.getType());
        context.setOwner(wiki.getOwner());
        context.setPageName(page.getName());
      }
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("An exception happened when process broken link.", e);
      }
    }
    return Utils.getDocumentURL(context);
  }
  
  private WikiContext getWikiContext() {
    try {
      RenderingService renderingService = (RenderingService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(RenderingService.class);
      Execution execution = ((RenderingServiceImpl) renderingService).getExecution();
      ExecutionContext ec = execution.getContext();
      
      if (ec != null) {
        WikiContext wikiContext = (WikiContext) ec.getProperty(WikiContext.WIKICONTEXT);
        return wikiContext;
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }
  
}
