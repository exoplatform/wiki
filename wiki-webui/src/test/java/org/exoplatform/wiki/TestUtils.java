/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.wiki;

import junit.framework.TestCase;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.core.space.SpaceApplication;
import org.exoplatform.social.core.space.SpaceTemplate;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.space.spi.SpaceTemplateService;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.service.WikiPageParams;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CommonsUtils.class, org.exoplatform.social.webui.Utils.class})
public class TestUtils extends TestCase {

  public void testGetURLFromParams() throws Exception {
    // Mocks
    SpaceService spaceService = Mockito.mock(SpaceService.class);
    SpaceTemplateService spaceTemplateService = Mockito.mock(SpaceTemplateService.class);
    PowerMockito.mockStatic(CommonsUtils.class);
    PowerMockito.mockStatic(org.exoplatform.social.webui.Utils.class);

    // Returned Objects from mocks
    List<SpaceApplication> spaceApplicationList = new ArrayList<>();
    SpaceApplication spaceApplication1 = new SpaceApplication();
    spaceApplication1.setPortletName("WikiPortlet");
    spaceApplication1.setUri("wiki");
    spaceApplicationList.add(spaceApplication1);
    SpaceApplication spaceApplication2 = new SpaceApplication();
    spaceApplication2.setPortletName("SpaceSettingPortlet");
    spaceApplication2.setUri("settings");
    spaceApplicationList.add(spaceApplication2);
    SpaceTemplate spaceTemplate = new SpaceTemplate();
    spaceTemplate.setName("community");
    spaceTemplate.setSpaceApplicationList(spaceApplicationList);
    Space space = new Space();
    space.setGroupId("/spaces/spaceTest");
    space.setTemplate(spaceTemplate.getName());

    // When
    when(CommonsUtils.getService(SpaceService.class)).thenReturn(spaceService);
    when(CommonsUtils.getService(SpaceTemplateService.class)).thenReturn(spaceTemplateService);
    when(org.exoplatform.social.webui.Utils.getSpaceHomeURL(space)).thenReturn("/portal/g/:spaces:spaceTest/spaceTest");
    when(spaceService.getSpaceByGroupId(Matchers.eq("/spaces/spaceTest"))).thenReturn(space);
    when(spaceTemplateService.getSpaceTemplateByName(Matchers.eq("community"))).thenReturn(spaceTemplate);

    // Then
    WikiPageParams params = new WikiPageParams();
    params.setOwner("");
    assertEquals(Utils.getURLFromParams(params), StringUtils.EMPTY);
    params.setOwner("/spaces/spaceTest");
    params.setType("");
    assertEquals(Utils.getURLFromParams(params), StringUtils.EMPTY);
    params.setType("group");
    assertEquals(Utils.getURLFromParams(params), "/portal/g/:spaces:spaceTest/spaceTest/wiki/");
  }
}
