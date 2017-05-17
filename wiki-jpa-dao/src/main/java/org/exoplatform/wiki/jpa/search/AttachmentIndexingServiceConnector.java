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

package org.exoplatform.wiki.jpa.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.simple.JSONObject;

import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.file.services.FileStorageException;
import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.commons.search.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.jpa.dao.PageAttachmentDAO;
import org.exoplatform.wiki.jpa.entity.PageAttachmentEntity;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.PermissionEntity;
import org.exoplatform.wiki.mow.api.PermissionType;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 10/2/15
 */
public class AttachmentIndexingServiceConnector  extends ElasticIndexingServiceConnector {
  private static final long serialVersionUID = -1229795181507933039L;

  private static final Log LOGGER = ExoLogger.getExoLogger(AttachmentIndexingServiceConnector.class);
  public static final String TYPE = "wiki-attachment";
  private final PageAttachmentDAO attachmentDAO;
  private final FileService fileService;

  public AttachmentIndexingServiceConnector(InitParams initParams, FileService fileService, PageAttachmentDAO attachmentDAO) {
    super(initParams);
    this.attachmentDAO = attachmentDAO;
    this.fileService = fileService;
  }

  @Override
  public Document create(String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("Id is null");
    }
    //Get the wiki object from BD
    PageAttachmentEntity attachment = attachmentDAO.find(Long.parseLong(id));
    if (attachment==null) {
      LOGGER.info("The attachment entity with id {} doesn't exist.", id);
      return null;
    }

    FileItem fileItem = null;
    try {
      fileItem = fileService.getFile(attachment.getAttachmentFileID());
    } catch (FileStorageException e) {
      LOGGER.error("Cannot get attachment file ID {}, cause {}",attachment.getAttachmentFileID(),e.getCause());
    }
    if (fileItem==null) {
      LOGGER.info("The attachment entity with id {} doesn't exist.", id);
      return null;
    }


    Map<String,String> fields = new HashMap<>();
    FileInfo fileInfo = fileItem.getFileInfo();
    Document doc = new Document(TYPE, id, getUrl(attachment), fileInfo.getUpdatedDate(),
        computePermissions(fileInfo.getUpdater(),attachment), fields);

    doc.addField("title", attachment.getFullTitle());
    doc.addField("file", fileItem.getAsByte());
    doc.addField("name", fileInfo.getName());
    doc.addField("createdDate", String.valueOf(attachment.getCreatedDate().getTime()));
    doc.addField("updatedDate", String.valueOf(fileInfo.getUpdatedDate().getTime()));
    PageEntity page = attachment.getPage();
    doc.addField("pageName", page.getName());
    fields.put("wikiType", page.getWiki().getType());
    fields.put("wikiOwner", page.getWiki().getOwner());

    return doc;
  }

  @Override
  public Document update(String id) {
    return create(id);
  }

  private Set<String> computePermissions(String creator, PageAttachmentEntity attachment) {
    Set<String> permissions = new HashSet<>();
    permissions.add(creator);
    //Add permissions from the wiki page
    List<PermissionEntity> pagePermission = attachment.getPage().getPermissions();
    if (pagePermission != null) {
      for(PermissionEntity permission : pagePermission) {
        if (permission.getPermissionType().equals(PermissionType.VIEWPAGE)
            || permission.getPermissionType().equals(PermissionType.VIEW_ATTACHMENT)) {
          permissions.add(permission.getIdentity());
        }
      }
    }
    return permissions;
  }

  private String getUrl(PageAttachmentEntity attachment) {
    return attachment.getPage().getUrl();
  }

  @SuppressWarnings("unchecked")
  @Override
  public String getMapping() {

    JSONObject notAnalyzedField = new JSONObject();
    notAnalyzedField.put("type", "text");
    notAnalyzedField.put("index", false);

    //Use Fast Vector Highlighter
    JSONObject fastVectorHighlighterField = new JSONObject();
    fastVectorHighlighterField.put("type", "text");
    fastVectorHighlighterField.put("term_vector", "with_positions_offsets");
    fastVectorHighlighterField.put("store", true);
    //Use Posting Highlighter
    JSONObject postingHighlighterField = new JSONObject();
    postingHighlighterField.put("type", "text");
    postingHighlighterField.put("index_options", "offsets");

    JSONObject properties = new JSONObject();

    //Construct attachment field
    JSONObject attachmentField = new JSONObject();
    JSONObject attachmentFields = new JSONObject();
    attachmentFields.put("content", fastVectorHighlighterField);
    attachmentField.put("properties", attachmentFields);
    properties.put("attachment", attachmentField);

    JSONObject keywordTypeMapping = new JSONObject();
    keywordTypeMapping.put("type", "keyword");

    //Add all field mapping
    properties.put("file", notAnalyzedField);
    properties.put("permissions", keywordTypeMapping);
    properties.put("wikiType", keywordTypeMapping);
    properties.put("wikiOwner", keywordTypeMapping);
    properties.put("name", postingHighlighterField);
    properties.put("title", postingHighlighterField);

    JSONObject mappingProperties = new JSONObject();
    mappingProperties.put("properties", properties);

    JSONObject mappingJSON = new JSONObject();
    mappingJSON.put(this.getType(), mappingProperties);

    return mappingJSON.toJSONString();
  }

  @Override
  public List<String> getAllIds(int offset, int limit) {

    List<String> result;

    List<Long> ids = this.attachmentDAO.findAllIds(offset, limit);
    if (ids==null) {
      result = new ArrayList<>(0);
    } else {
      result = new ArrayList<>(ids.size());
      for (Long id : ids) {
        result.add(String.valueOf(id));
      }
    }
    return result;
  }

  @Override
  public boolean isNeedIngestPipeline() {
    return true;
  }

  @Override
  public String getPipelineName() {
    return "wiki-attachment-file";
  }

  @SuppressWarnings("unchecked")
  @Override
  public String getAttachmentProcessor() {
    JSONObject fieldJSON = new JSONObject();
    fieldJSON.put("field", "file");
    fieldJSON.put("indexed_chars", -1);
    fieldJSON.put("properties", new JSONArray(Collections.singleton("content")));

    JSONObject attachmentJSON = new JSONObject();
    attachmentJSON.put("attachment", fieldJSON);

    JSONObject fileBinaryRemoveActionJSON = new JSONObject();
    JSONObject fileBinaryRemoveJSON = new JSONObject();
    fileBinaryRemoveJSON.put("field", "file");
    fileBinaryRemoveActionJSON.put("remove", fileBinaryRemoveJSON);

    JSONObject processorJSON = new JSONObject();
    processorJSON.put("description", "Wiki Attachment processor");
    processorJSON.put("processors", new JSONArray(Arrays.asList(attachmentJSON, fileBinaryRemoveActionJSON)));
    return processorJSON.toJSONString();
  }
}