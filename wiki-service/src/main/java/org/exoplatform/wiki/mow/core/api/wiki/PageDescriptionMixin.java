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
package org.exoplatform.wiki.mow.core.api.wiki;

import java.util.Date;

import org.chromattic.api.RelationshipType;
import org.chromattic.api.annotations.MixinType;
import org.chromattic.api.annotations.OneToOne;
import org.chromattic.api.annotations.Property;
import org.exoplatform.wiki.mow.api.WikiNodeType;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2013  
 */
@MixinType(name = WikiNodeType.WIKI_PAGE_DESCRIPTION)
public abstract class PageDescriptionMixin {

  @OneToOne(type = RelationshipType.EMBEDDED)
  public abstract AttachmentImpl getEntity();

  public abstract void setEntity(AttachmentImpl page);

  @Property(name = WikiNodeType.Definition.AUTHOR)
  public abstract String getAuthor();
  public abstract void setAuthor(String author);
  
  @Property(name = WikiNodeType.Definition.UPDATED_DATE)
  public abstract Date getUpdatedDate();
  public abstract void setUpdatedDate(Date date);
  
  @Property(name = WikiNodeType.Definition.COMMENT)
  public abstract String getComment();
  public abstract void setComment(String comments);
  

}
