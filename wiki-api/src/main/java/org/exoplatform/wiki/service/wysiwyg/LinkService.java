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

import org.xwiki.component.annotation.Role;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityReference;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference;

/**
 * Manages 2 different link types: EntityConfig and EntityReference.
 *
 * @LevelAPI Experimental
 */
@Role
public interface LinkService {

  /**
   * Creates an EntityConfig (URL and reference) for a link with the specified origin and
   * destination. The link reference in the returned EntityConfig is relative to the link origin.
   * 
   * @param origin Origin of the link.
   * @param destination Destination of the link.
   * @return The entity link that can be used to insert the link in the origin.
   */
  EntityConfig getEntityConfig(EntityReference origin, ResourceReference destination);

  /**
   * Parses a given link reference and extracts a reference to the linked entity. The returned entity reference is
   * resolved that is relative to the given base entity reference.
   * 
   * @param linkReference The link reference pointing to the entity.
   * @param baseReference The entity reference which is used for resolving the linked entity reference.
   * @return The link reference to the entity.
   */
  ResourceReference parseLinkReference(String linkReference, EntityReference baseReference);

}
