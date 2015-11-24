/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.rendering.render.xwiki;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 14, 2013  
 */
@Component
@Singleton
public class DummyFormularDocumentAccessBridge implements DocumentAccessBridge {

  @Override
  @Deprecated
  public DocumentModelBridge getDocument(String documentReference) throws Exception {
    return null;
  }

  @Override
  public DocumentModelBridge getDocument(DocumentReference documentReference) throws Exception {
    return null;
  }

  @Override
  public DocumentReference getCurrentDocumentReference() {
    return null;
  }

  @Override
  public boolean exists(DocumentReference documentReference) {
    return false;
  }

  @Override
  @Deprecated
  public boolean exists(String documentReference) {
    return false;
  }

  @Override
  public void setDocumentContent(DocumentReference documentReference,
                                 String content,
                                 String editComment,
                                 boolean isMinorEdit) throws Exception {
  }

  @Override
  @Deprecated
  public void setDocumentContent(String documentReference,
                          String content,
                          String editComment,
                          boolean isMinorEdit) throws Exception {
  }

  @Override
  @Deprecated
  public String getDocumentContent(String documentReference) throws Exception {
    return null;
  }

  @Override
  @Deprecated
  public String getDocumentSyntaxId(String documentReference) throws Exception {
    return null;
  }

  @Override
  public void setDocumentSyntaxId(DocumentReference documentReference, String syntaxId) throws Exception {
  }

  @Override
  @Deprecated
  public void setDocumentSyntaxId(String documentReference, String syntaxId) throws Exception {
  }

  @Override
  public void setDocumentParentReference(DocumentReference documentReference,
                                         DocumentReference parentReference) throws Exception {
  }

  @Override
  public void setDocumentTitle(DocumentReference documentReference, String title) throws Exception {
  }

  @Override
  public String getDocumentContentForDefaultLanguage(DocumentReference documentReference) throws Exception {
    return null;
  }

  @Override
  @Deprecated
  public String getDocumentContentForDefaultLanguage(String documentReference) throws Exception {
    return null;
  }

  @Override
  public String getDocumentContent(DocumentReference documentReference, String language) throws Exception {
    return null;
  }

  @Override
  @Deprecated
  public String getDocumentContent(String documentReference, String language) throws Exception {
    return null;
  }

  @Override
  public int getObjectNumber(DocumentReference documentReference,
                             DocumentReference classReference,
                             String parameterName,
                             String valueToMatch) {
    return 0;
  }

  @Override
  public Object getProperty(String documentReference,
                            String className,
                            int objectNumber,
                            String propertyName) {
    return null;
  }

  @Override
  @Deprecated
  public Object getProperty(String documentReference, String className, String propertyName) {
    return null;
  }

  @Override
  public Object getProperty(ObjectReference objectReference, String propertyName) {
    return null;
  }

  @Override
  public Object getProperty(ObjectPropertyReference objectPropertyReference) {
    return null;
  }

  @Override
  public Object getProperty(DocumentReference documentReference,
                            DocumentReference classReference,
                            String propertyName) {
    return null;
  }

  @Override
  public Object getProperty(DocumentReference documentReference,
                            DocumentReference classReference,
                            int objectNumber,
                            String propertyName) {
    return null;
  }

  @Override
  public Object getProperty(String documentReference, String propertyName) {
    return null;
  }

  @Override
  public List<Object> getProperties(String documentReference, String className) {
    return null;
  }

  @Override
  public String getPropertyType(String className, String propertyName) throws Exception {
    return null;
  }

  @Override
  public boolean isPropertyCustomMapped(String className, String propertyName) throws Exception {
    return false;
  }

  @Override
  @Deprecated
  public void setProperty(String documentReference,
                   String className,
                   String propertyName,
                   Object propertyValue) throws Exception {
  }

  @Override
  public void setProperty(DocumentReference documentReference,
                          DocumentReference classReference,
                          String propertyName,
                          Object propertyValue) throws Exception {
  }

  @Override
  @Deprecated
  public byte[] getAttachmentContent(String documentReference, String attachmentName) throws Exception {
    return null;
  }

  @Override
  public InputStream getAttachmentContent(AttachmentReference attachmentReference) throws Exception {
    return null;
  }

  @Override
  public void setAttachmentContent(AttachmentReference attachmentReference, byte[] attachmentData) throws Exception {
  }

  @Override
  @Deprecated
  public void setAttachmentContent(String documentReference,
                            String attachmentFilename,
                            byte[] attachmentData) throws Exception {
  }

  @Override
  public String getAttachmentVersion(AttachmentReference attachmentReference) throws Exception {
    return null;
  }

  @Override
  public String getDocumentURL(DocumentReference documentReference,
                               String action,
                               String queryString,
                               String anchor) {
    return null;
  }

  @Override
  public String getDocumentURL(DocumentReference documentReference,
                               String action,
                               String queryString,
                               String anchor,
                               boolean isFullURL) {
    return null;
  }

  @Override
  @Deprecated
  public String getURL(String documentReference, String action, String queryString, String anchor) {
    return null;
  }

  @Override
  public List<AttachmentReference> getAttachmentReferences(DocumentReference documentReference) throws Exception {
    return null;
  }

  @Override
  @Deprecated
  public String getAttachmentURL(String documentReference, String attachmentFilename) {
    return null;
  }

  @Override
  public String getAttachmentURL(AttachmentReference attachmentReference, boolean isFullURL) {
    return null;
  }

  @Override
  public String getAttachmentURL(AttachmentReference attachmentReference,
                                 String queryString,
                                 boolean isFullURL) {
    return null;
  }

  @Override
  @Deprecated
  public List<String> getAttachmentURLs(DocumentReference documentReference, boolean isFullURL) throws Exception {
    return null;
  }

  @Override
  public boolean isDocumentViewable(DocumentReference documentReference) {
    return false;
  }

  @Override
  @Deprecated
  public boolean isDocumentViewable(String documentReference) {
    return false;
  }

  @Override
  @Deprecated
  public boolean isDocumentEditable(String documentReference) {
    return false;
  }

  @Override
  public boolean isDocumentEditable(DocumentReference documentReference) {
    return false;
  }

  @Override
  public boolean hasProgrammingRights() {
    return false;
  }

  @Override
  @Deprecated
  public String getCurrentUser() {
    return null;
  }

  @Override
  public DocumentReference getCurrentUserReference() {
    return null;
  }

  @Override
  public void setCurrentUser(String userName) {
    
  }

  @Override
  public String getDefaultEncoding() {
    return null;
  }

  @Override
  @Deprecated
  public void pushDocumentInContext(Map<String, Object> backupObjects, String documentReference) throws Exception {
    
  }

  @Override
  public void pushDocumentInContext(Map<String, Object> backupObjects,
                                    DocumentReference documentReference) throws Exception {
  }

  @Override
  public void popDocumentFromContext(Map<String, Object> backupObjects) {
  }

  @Override
  @Deprecated
  public String getCurrentWiki() {
    return null;
  }

}
