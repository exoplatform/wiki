/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.wiki.mow.core.api.wiki;

import org.apache.commons.lang.StringUtils;
import org.chromattic.api.annotations.PrimaryType;
import org.chromattic.api.annotations.Property;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.wiki.chromattic.ext.ntdef.NTVersion;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.WikiNodeType;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.diff.DiffResult;
import org.exoplatform.wiki.service.diff.DiffService;
import org.exoplatform.wiki.utils.Utils;

/**
 * Created by The eXo Platform SAS
 * Author : phongth
 *          phongth@exoplatform.com
 * Aug 20, 2012  
 */
@PrimaryType(name = WikiNodeType.WIKI_DRAFT_PAGE)
public abstract class DraftPageImpl extends PageImpl implements DraftPage {
  @Property(name = WikiNodeType.Definition.DRAFT_TARGET_PAGE)
  public abstract String getTargetPage();
  public abstract void setTargetPage(String targetPage);
  
  @Property(name = WikiNodeType.Definition.DRAFT_TARGET_REVISION)
  public abstract String getTargetRevision();
  public abstract void setTargetRevision(String targetRevision);
  
  @Property(name = WikiNodeType.Definition.DRAFT_IS_NEW_PAGE)
  public abstract boolean isNewPage();
  public abstract void setNewPage(boolean isNewPage);
  
  public boolean isOutDate() throws Exception {
    String targetRevision = getTargetRevision();
    if (targetRevision == null) {
      return false;
    }
    
    if (targetRevision.equals("rootVersion")) {
      targetRevision = "1";
    }
    
    PageImpl targetPage = (PageImpl) getTargetWikiPage();
    if (targetPage == null) {
      return true;
    }
    
    String lastestRevision = Utils.getLastRevisionOfPage(targetPage).getName();
    if (lastestRevision == null) {
      return true;
    }
    
    if (lastestRevision.equals("rootVersion")) {
      lastestRevision = "1";
    }
    
    return lastestRevision.compareTo(targetRevision) > 0;
  }
  
  private Page getTargetWikiPage() throws Exception {
    WikiService wservice = (WikiService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    return wservice.getWikiPageByUUID(getTargetPage());
  }
  
  public DiffResult getChanges() throws Exception {
    String targetContent = StringUtils.EMPTY;
    
    if (!isNewPage()) {
      PageImpl targetPage = (PageImpl) getTargetWikiPage();
      if (targetPage != null) {
        NTVersion lastestRevision = Utils.getLastRevisionOfPage(targetPage);
        targetContent = ((AttachmentImpl) lastestRevision.getNTFrozenNode().getChildren().get(WikiNodeType.Definition.CONTENT)).getText();
        if (targetContent == null) {
          targetContent = StringUtils.EMPTY;
        }
      }
    }
    DiffService diffService = (DiffService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(DiffService.class);
    return diffService.getDifferencesAsHTML(targetContent, getContent().getText(), true);
  }
}
