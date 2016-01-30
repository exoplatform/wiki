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
package org.exoplatform.wiki.webui;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.webui.commons.EventUIComponent;
import org.exoplatform.webui.commons.EventUIComponent.EVENTTYPE;
import org.exoplatform.webui.commons.UISpacesSwitcher;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectOption;
import org.exoplatform.webui.core.model.SelectOptionGroup;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.wiki.commons.WikiConstants;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.webui.core.UIAdvancePageIterator;

@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "app:/templates/wiki/webui/UIWikiAdvanceSearchForm.gtmpl",
  events = {
      @EventConfig(listeners = UIWikiAdvanceSearchForm.SearchActionListener.class),
      @EventConfig(listeners = UIWikiAdvanceSearchForm.SwitchSpaceActionListener.class)
    }
)

public class UIWikiAdvanceSearchForm extends UIForm {
  final static String TEXT  = "text".intern();

  final static String WIKIS = "wikis".intern();
  
  public static final String UI_WIKI_ADVANCE_SEARCH_FORM = "UIWikiAdvanceSearchForm";
  
  public static final String SWITCH_SPACE_ACTION = "SwitchSpace";
  
  private int itemPerPage = 10;
   
  private long numberOfSearchResult;
  
  private String currentWiki_;
  
  public UIWikiAdvanceSearchForm() throws Exception {
    addChild(new UIFormStringInput(TEXT, TEXT, null));
    UISpacesSwitcher selectSpaces = addChild(UISpacesSwitcher.class, null, WIKIS);
    selectSpaces.setCurrentSpaceName(org.exoplatform.wiki.commons.Utils.upperFirstCharacter(
           org.exoplatform.wiki.commons.Utils.getCurrentSpaceName()));
    selectSpaces.setAutoResize(true);
    selectSpaces.setAppId(WikiConstants.SPACES_SWITCHER_WIKI_APP_ID);
    EventUIComponent eventComponent1 = new EventUIComponent(UI_WIKI_ADVANCE_SEARCH_FORM, SWITCH_SPACE_ACTION, EVENTTYPE.EVENT);
    selectSpaces.init(eventComponent1);
    addChild(selectSpaces);
    
    resetWikiSpaces();
    this.setActions(new String[] { "Search" });
  }
  
  public void resetWikiSpaces() throws Exception {
    getChild(UISpacesSwitcher.class).setCurrentSpaceName(
           org.exoplatform.wiki.commons.Utils.upperFirstCharacter(org.exoplatform.wiki.commons.Utils.getCurrentSpaceName()));
    currentWiki_ = getDefaultSelectWikiValue();
  }
  
  public SelectOptionGroup getAllWikiOptions() throws Exception {
    SelectOptionGroup allSpaceOptions = new SelectOptionGroup("");
    allSpaceOptions.addOption(new SelectOption(getLabel("AllWikis"), ""));
    allSpaceOptions.addOption(new SelectOption(getLabel("AllPortalWikis"), PortalConfig.PORTAL_TYPE));
    allSpaceOptions.addOption(new SelectOption(getLabel("AllGroupWikis"), PortalConfig.GROUP_TYPE));
    allSpaceOptions.addOption(new SelectOption(getLabel("AllUserWikis"), PortalConfig.USER_TYPE));
    return allSpaceOptions;
  }
  
  public void processSearchAction() throws Exception {
    WikiSearchData data = createSearchData();
    numberOfSearchResult = Utils.countSearchResult(data);
    gotoSearchPage(1);
  }

  public long getNumberOfSearchResult() {
    return numberOfSearchResult;
  }
  
  public void setItemsPerPage(int value) {
    itemPerPage = value;
  }
  
  /**
   * Get number of items per page
   * 
   * @return the itemPerPage
   */
  public int getItemPerPage() {
    return itemPerPage;
  }

  public void gotoSearchPage(int pageIndex) throws Exception {
    if(numberOfSearchResult > 0) {
      pageIndex = Math.min(pageIndex, getPageAvailable());
    }
    WikiSearchData data = createSearchData();
    data.setOffset((pageIndex - 1) * itemPerPage);
    data.setLimit(itemPerPage);

    WikiService wikiservice = (WikiService) PortalContainer.getComponent(WikiService.class);
    UIWikiAdvanceSearchResult uiSearchResults = getParent().findFirstComponentOfType(UIWikiAdvanceSearchResult.class);
    uiSearchResults.setResults(wikiservice.search(data));

    UIAdvancePageIterator uiAdvancePageIterator = getParent().findFirstComponentOfType(UIAdvancePageIterator.class);
    uiAdvancePageIterator.setCurrentPage(pageIndex);
  }

  public String getKeyword() {
    String text = getUIStringInput(TEXT).getValue();
    return (text == null) ? StringUtils.EMPTY : text.trim();
  }

  public int getPageAvailable() {
    double pageAvailabe = numberOfSearchResult * 1.0 / itemPerPage;
    if (pageAvailabe > (int) pageAvailabe) {
      pageAvailabe = (int) pageAvailabe + 1;
    }
    return (int) pageAvailabe;
  }

  private WikiSearchData createSearchData() {
    String text = getKeyword();
    String path = this.currentWiki_;
    if (path.startsWith(org.exoplatform.wiki.commons.Utils.SLASH)) {
      path = path.substring(1);
    }
    String wikiType = null;
    String wikiOwner = null;
    if (!StringUtils.isEmpty(path)) {
      String[] arrayParams = path.split(org.exoplatform.wiki.commons.Utils.SLASH);
      if (arrayParams.length >= 1) {
        wikiType = arrayParams[0];
        if (arrayParams.length >= 2)
          wikiOwner = StringUtils.replace(path, wikiType + org.exoplatform.wiki.commons.Utils.SLASH, StringUtils.EMPTY);
      }
    }
    WikiSearchData searchData = new WikiSearchData(text, text, wikiType, wikiOwner);
    return searchData;
  }
  
  private String getDefaultSelectWikiValue() throws Exception {
    WikiPageParams currentParams = org.exoplatform.wiki.commons.Utils.getCurrentWikiPageParams();
    String wikiType = currentParams.getType();
    String owner = currentParams.getOwner();
    return wikiType + "/" + Utils.validateWikiOwner(wikiType, owner);
  }
  
  static public class SearchActionListener extends EventListener<UIWikiAdvanceSearchForm> {
    public void execute(Event<UIWikiAdvanceSearchForm> event) throws Exception {
      UIWikiAdvanceSearchForm uiSearch = event.getSource() ;
      uiSearch.processSearchAction();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearch.getAncestorOfType(UIWikiSearchSpaceArea.class));
    }
  }

  /**
   * Switches to another space, searches for wiki pages in that space
   * @author vuna
   *
   */
  static public class SwitchSpaceActionListener extends EventListener<UIWikiAdvanceSearchForm> {
    public void execute(Event<UIWikiAdvanceSearchForm> event) throws Exception {
      String wikiId = event.getRequestContext().getRequestParameter(UISpacesSwitcher.SPACE_ID_PARAMETER);      
      UIWikiAdvanceSearchForm uiSearch = event.getSource() ;
      UISpacesSwitcher spaceSwitcher = uiSearch.getChild(UISpacesSwitcher.class);
      
      WikiService wikiService = org.exoplatform.wiki.rendering.util.Utils.getService(WikiService.class);
      spaceSwitcher.setCurrentSpaceName(
                    org.exoplatform.wiki.commons.Utils.upperFirstCharacter(wikiService.getWikiNameById(wikiId)));
      Wiki wiki = wikiService.getWikiById(wikiId);
      String wikiType = wiki.getType();
      String wikiOwner = wiki.getOwner();
      uiSearch.currentWiki_ = new StringBuffer(wikiType).append("/").
                                    append(Utils.validateWikiOwner(wikiType, wikiOwner)).toString();
      uiSearch.processSearchAction();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearch.getAncestorOfType(UIWikiSearchSpaceArea.class));
    }
  }

}
