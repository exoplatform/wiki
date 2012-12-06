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
package org.exoplatform.wiki.webui.commons;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.util.ReflectionUtil;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.wiki.service.BreadcrumbData;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.utils.Utils;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Hung Phong
 *          phongth@exoplatform.com
 * 11 Sep 2012  
 */
@ComponentConfig(template = "app:/templates/wiki/webui/UIWikiDraftGrid.gtmpl")
@Serialized
public class UIWikiDraftGrid extends UIComponent {
  public static final String    SORT_ASC        = "ASC";
  
  public static final String    SORT_DESC       = "DESC";
  
  private Map<String, String> actionForField = new HashMap<String, String>();
  
  private String fieldToDisplayBreadCrumb;
  
  private String sortField;
  
  private boolean isASC;
  
  /** The page iterator */
  protected UIPageIterator uiIterator_;

  /** The bean field that holds the id of this bean */
  protected String beanIdField_;

  /** An array of String representing the fields in each bean */
  protected String[] beanField_;

  /** An array of String representing the actions on each bean */
  protected String[] action_;

  protected String label_;

  protected boolean useAjax = true;

  protected int displayedChars_ = 30;
  
  private HashMap<String, List<BreadcrumbData>> breadCrumbs = new HashMap<String, List<BreadcrumbData>>();
  
  public UIWikiDraftGrid() throws Exception {
    uiIterator_ = createUIComponent(UIPageIterator.class, null, null);
    uiIterator_.setParent(this);
  }
  
  public void clearBreadcrum() {
    breadCrumbs.clear();
  }
  
  public UIPageIterator getUIPageIterator() {
    return uiIterator_;
  }
  
  public void setActionForField(String field, String action) {
    actionForField.put(field, action);
  }
  
  public String getActionForField(String field) {
    return actionForField.get(field);
  }
  
  public void putBreadCrumbDatas(String key, List<BreadcrumbData> breakCrumbDatas) throws Exception {
    breadCrumbs.put(key, breakCrumbDatas);
  }
  
  public String getBreadCrumb(String key) {
    List<BreadcrumbData> breadcrumbDatas = breadCrumbs.get(key);
    if (breadcrumbDatas == null) {
      return null;
    }
    
    StringBuilder breadCrum = new StringBuilder();
    for (int i = 0; i < breadcrumbDatas.size(); i++) {
      breadCrum.append(breadcrumbDatas.get(i).getTitle());
      breadCrum.append(" >> ");
    }
    
    breadCrum.delete(breadCrum.length() - 4, breadCrum.length());
    return breadCrum.toString();
  }
  
  public WikiPageParams getPageParam(String key) throws Exception {
    List<BreadcrumbData> breadcrumbDatas = breadCrumbs.get(key);
    if (breadcrumbDatas != null && breadcrumbDatas.size() > 0) {
      WikiService wservice = (WikiService) PortalContainer.getComponent(WikiService.class);
      return wservice.getWikiPageParams(breadcrumbDatas.get(breadcrumbDatas.size() - 1));
    }
    return null;
  }
  
  public String getFieldToDisplayBreadCrumb() {
    return fieldToDisplayBreadCrumb;
  }
  
  public void setFieldToDisplayBreadCrumb(String fieldToDisplayBreadCrumb) {
    this.fieldToDisplayBreadCrumb = fieldToDisplayBreadCrumb;
  }

  public String getWikiName(String key) throws Exception {
    WikiPageParams params = getPageParam(key);
    if (params != null) {
      String wikiName = params.getOwner();
      int index = wikiName.lastIndexOf('/');
      if (index > -1) {
        wikiName = wikiName.substring(index + 1);
      }
      return wikiName;
    }
    return null;
  }

  public String getSortField() {
    return sortField;
  }

  public void setSortField(String sortField) {
    this.sortField = sortField;
  }

  public boolean isASC() {
    return isASC;
  }

  public void setASC(boolean isASC) {
    this.isASC = isASC;
  }

  public String createActionLink(BreadcrumbData breadCumbData) throws Exception {  
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    StringBuilder sb = new StringBuilder(portalRequestContext.getPortalURI());
    UIPortal uiPortal = Util.getUIPortal();
    String pageNodeSelected = uiPortal.getSelectedUserNode().getURI();
    sb.append(pageNodeSelected);
    sb.append("/");
    if (!PortalConfig.PORTAL_TYPE.equalsIgnoreCase(breadCumbData.getWikiType())) {
      sb.append(breadCumbData.getWikiType());
      sb.append("/");
      sb.append(Utils.validateWikiOwner(breadCumbData.getWikiType(), breadCumbData.getWikiOwner()));
      sb.append("/");
    }
    sb.append(breadCumbData.getId());
    return sb.toString();
  }
  
  public UIWikiDraftGrid configure(String beanIdField, String[] beanField, String[] action) {
    this.beanIdField_ = beanIdField;
    this.beanField_ = beanField;
    this.action_ = action;
    return this;
  }

  public String getBeanIdField() {
    return beanIdField_;
  }

  public String[] getBeanFields() {
    return beanField_;
  }

  public String[] getBeanActions() {
    return action_;
  }

  public List<?> getBeans() throws Exception {
    return uiIterator_.getCurrentPageData();
  }

  public String getLabel() {
    return label_;
  }

  public void setLabel(String label) {
    label_ = label;
  }

  public Object getFieldValue(Object bean, String field) throws Exception {
    Method method = ReflectionUtil.getGetBindingMethod(bean, field);
    return method.invoke(bean, ReflectionUtil.EMPTY_ARGS);
  }

  public String getBeanIdFor(Object bean) throws Exception {
    return getFieldValue(bean, beanIdField_).toString();
  }

  @SuppressWarnings("unchecked")
  public UIComponent findComponentById(String lookupId) {
    if (uiIterator_.getId().equals(lookupId)) {
      return uiIterator_;
    }
    return super.findComponentById(lookupId);
  }

  public boolean isUseAjax() {
    return useAjax;
  }

  public void setUseAjax(boolean value) {
    useAjax = value;
  }

  public int getDisplayedChars() {
    return displayedChars_;
  }

  public void setDisplayedChars(int displayedChars) {
    this.displayedChars_ = displayedChars;
  }
}
