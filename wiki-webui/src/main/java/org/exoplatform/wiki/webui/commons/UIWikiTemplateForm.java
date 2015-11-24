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
package org.exoplatform.wiki.webui.commons;

import java.util.*;
import java.util.Map.Entry;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.Template;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.webui.bean.TemplateBean;
import org.exoplatform.wiki.webui.bean.WikiTemplateListAccess;

@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "app:/templates/wiki/webui/commons/UIWikiTemplateForm.gtmpl",
  events = {
    @EventConfig(listeners = UIWikiTemplateForm.SearchTemplateActionListener.class)
  })
public class UIWikiTemplateForm extends UIForm{
 
  public static final String    TEMPLATE_SEARCHBOX = "TemplateSeachBox";

  public static final String    TEMPLATE_GRID      = "UIWikiTemplateGrid";

  public static final String    TEMPLATE_ITER      = "TemplateIter";

  public static final int       ITEMS_PER_PAGE     = 8;

  public static final String    TEMPLATE_ID        = TemplateBean.ID;

  public static final String    TEMPLATE_NAME      = TemplateBean.TITLE;

  public static final String    DESCRIPTION        = TemplateBean.DESCRIPTION;

  public static final String[] TEMPLATE_FIELD     = { TEMPLATE_NAME, DESCRIPTION };

  public UIGrid                grid;

  public WikiService           wService;
  
  public ResourceBundle        res;
  
  public UIWikiTemplateForm() throws Exception {

    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    res = context.getApplicationResourceBundle();
    UIFormStringInput inputSearchBox = new UIFormStringInput(TEMPLATE_SEARCHBOX, null, null);
    inputSearchBox.setHTMLAttribute("placeholder", res.getString("UIWikiSearchBox.label.Search"));
    addChild(inputSearchBox);
    grid = addChild(UIWikiGrid.class, null, TEMPLATE_GRID);
    grid.getUIPageIterator().setId(TEMPLATE_ITER);
    grid.getUIPageIterator().setParent(this);
    wService = (WikiService) PortalContainer.getComponent(WikiService.class);
    ((UIWikiGrid)grid.configure(TEMPLATE_ID, TEMPLATE_FIELD, null)).setUIGridMode(UIWikiGrid.TEMPLATE);
    initGrid();
  }
  

  public void initGrid() throws Exception {
    List<TemplateBean> listBean = new ArrayList<>();

    WikiPageParams params = Utils.getCurrentWikiPageParams();
    Map<String, Template> templatesMap = wService.getTemplates(params);
    if(templatesMap != null) {
      for(Template template : templatesMap.values()) {
        listBean.add(new TemplateBean(template.getName(),
                template.getTitle(),
                template.getDescription()));
      }
    }
    LazyPageList<TemplateBean> lazylist = new LazyPageList<TemplateBean>(new WikiTemplateListAccess(listBean),
                                                                         ITEMS_PER_PAGE);
    grid.getUIPageIterator().setPageList(lazylist);
  }
  
  static public class SearchTemplateActionListener extends EventListener<UIWikiTemplateForm> {
    public void execute(Event<UIWikiTemplateForm> event) throws Exception {
      UIWikiTemplateForm form = event.getSource();
      WikiPageParams params = Utils.getCurrentWikiPageParams();
      UIFormStringInput searchbox = form.findComponentById(UIWikiTemplateForm.TEMPLATE_SEARCHBOX);
      String searchKeyword = searchbox.getValue();
      if (searchKeyword == null || (searchKeyword != null && searchKeyword.trim().length() == 0)) {
        form.initGrid();
      } else {
        TemplateSearchData data = new TemplateSearchData(searchKeyword, params.getType(), params.getOwner());
        List<TemplateSearchResult> results = form.wService.searchTemplate(data);
        List<TemplateBean> listBean = new ArrayList<TemplateBean>();
        for (int i = 0; i < results.size(); i++) {
          TemplateSearchResult result = results.get(i);
          listBean.add(new TemplateBean(result.getName(), result.getTitle(), result.getDescription()));
        }
        LazyPageList<TemplateBean> lazylist = new LazyPageList<TemplateBean>(new WikiTemplateListAccess(listBean), ITEMS_PER_PAGE);
        form.grid.getUIPageIterator().setPageList(lazylist);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(form);
    }
  }
}
