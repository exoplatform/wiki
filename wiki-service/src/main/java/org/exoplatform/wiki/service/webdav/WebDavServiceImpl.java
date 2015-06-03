/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.wiki.service.webdav;

import javax.ws.rs.Path;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;

@Path("/jcrwiki")
public class WebDavServiceImpl extends org.exoplatform.services.jcr.webdav.WebDavServiceImpl implements WebDavService {

  public WebDavServiceImpl(InitParams params,
                           RepositoryService repositoryService,
                           SessionProviderService sessionProviderService) throws Exception {
    super(params, repositoryService, sessionProviderService);
  }

}
