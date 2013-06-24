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

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 17, 2013  
 */
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.w3c.dom.Document;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;
import org.xwiki.xml.html.filter.HTMLFilter;

/**
 * {@link HTMLCleaner} for cleaning HTML generated from an office server.
 * 
 * @version $Id: 6a1d4314372915b173afb416ecc6156ae9b87dc0 $
 * @since 1.8M1
 */
@Component
@Named("openoffice")
@Singleton
public class WikiOfficeHTMLCleaner implements HTMLCleaner
{
    /**
     * Default html cleaner component used internally.
     */
    @Inject
    private HTMLCleaner defaultHtmlCleaner;


    @Override
    public Document clean(Reader originalHtmlContent)
    {
        // Add special parameters used in filters
        HTMLCleanerConfiguration configuration = getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap("filterStyles", "strict"));

        return clean(originalHtmlContent, configuration);
    }

    @Override
    public Document clean(Reader originalHtmlContent, HTMLCleanerConfiguration configuration)
    {
        return this.defaultHtmlCleaner.clean(originalHtmlContent, configuration);
    }

    @Override
    public HTMLCleanerConfiguration getDefaultConfiguration()
    {
        HTMLCleanerConfiguration configuration = this.defaultHtmlCleaner.getDefaultConfiguration();

        // Add office cleaning filters after the default filters.
        List<HTMLFilter> filters = new ArrayList<HTMLFilter>(configuration.getFilters());
        filters.addAll(new ArrayList<HTMLFilter>());
        configuration.setFilters(filters);

        return configuration;
    }
}
