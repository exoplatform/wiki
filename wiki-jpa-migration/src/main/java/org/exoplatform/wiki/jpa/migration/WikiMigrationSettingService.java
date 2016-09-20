/* 
* Copyright (C) 2003-2016 eXo Platform SAS.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see http://www.gnu.org/licenses/ .
*/
package org.exoplatform.wiki.jpa.migration;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.settings.impl.SettingServiceImpl;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.core.api.wiki.WikiImpl;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 1/26/16
 */
public class WikiMigrationSettingService {

  //Log
  private static final Log LOG = ExoLogger.getLogger(WikiMigrationSettingService.class);

  //Service
  private SettingService settingService;

  //eXo Properties
  private boolean forceJCRDeletion = false;
  private boolean forceRunMigration = false;

  private final static String INNER_OBJECT_SPLIT = ":";
  private final static String OUTER_OBJECT_SPLIT = ";";

  public WikiMigrationSettingService(SettingService settingService) {

    //Init Service
    this.settingService = settingService;

    //Init eXo Properties
    if (StringUtils.isNotBlank(PropertyManager.getProperty(WikiMigrationContext.WIKI_RDBMS_MIGRATION_FORCE_DELETION_PROPERTY_NAME))) {
      this.forceJCRDeletion = Boolean.valueOf(PropertyManager.getProperty(WikiMigrationContext.WIKI_RDBMS_MIGRATION_FORCE_DELETION_PROPERTY_NAME));
    }
    if (StringUtils.isNotBlank(PropertyManager.getProperty(WikiMigrationContext.WIKI_RDBMS_MIGRATION_FORCE_MIGRATION_PROPERTY_NAME))) {
      this.forceRunMigration = Boolean.valueOf(PropertyManager.getProperty(WikiMigrationContext.WIKI_RDBMS_MIGRATION_FORCE_MIGRATION_PROPERTY_NAME));
    }
  }

  /**
   * Get value (an eXo Property) that force the deletion of wiki and page even if they encounter error during migration
   *
   * @return
   */
  public boolean isForceJCRDeletion() {
    return forceJCRDeletion;
  }

  public void setForceJCRDeletion(boolean forceJCRDeletion) {
    this.forceJCRDeletion = forceJCRDeletion;
  }

  /**
   * Get value (an eXo Property) that force the migration to restart from the beginning
   *
   * @return
   */
  public boolean isForceRunMigration() {
    return forceRunMigration;
  }

  public void setForceRunMigration(boolean forceRunMigration) {
    this.forceRunMigration = forceRunMigration;
  }

  /**
   * Use the settingService to get all wiki migration setting as PLF startup.
   *
   * The settings allow to start the migration where it has been stopped before
   * and do not re start operation already finished.
   *
   * If the setting doesn't exist yet (first PLF start after installation of the add-on)
   * it is configure as false by default
   */
  public void initMigrationSetting() {

    if (forceRunMigration) {
      initMigrationSettingToDefault();
      return;
    }

    settingService = CommonsUtils.getService(SettingService.class);

    //Init migration state
    WikiMigrationContext.setMigrationDone(getOrCreateOperationState(WikiMigrationContext.WIKI_RDBMS_MIGRATION_KEY));
    WikiMigrationContext.setPortalWikiMigrationDone(getOrCreateOperationState(WikiMigrationContext.WIKI_RDBMS_MIGRATION_PORTAL_WIKI_KEY));
    WikiMigrationContext.setSpaceWikiMigrationDone(getOrCreateOperationState(WikiMigrationContext.WIKI_RDBMS_MIGRATION_SPACE_WIKI_KEY));
    WikiMigrationContext.setUserWikiMigrationDone(getOrCreateOperationState(WikiMigrationContext.WIKI_RDBMS_MIGRATION_USER_WIKI_KEY));
    WikiMigrationContext.setDraftPageMigrationDone(getOrCreateOperationState(WikiMigrationContext.WIKI_RDBMS_MIGRATION_DRAFT_PAGE_KEY));
    WikiMigrationContext.setRelatedPageMigrationDone(getOrCreateOperationState(WikiMigrationContext.WIKI_RDBMS_MIGRATION_RELATED_PAGE_KEY));

    //Init reindex state
    WikiMigrationContext.setReindexDone(getOrCreateOperationState(WikiMigrationContext.WIKI_RDBMS_MIGRATION_REINDEX_KEY));

    //Init deletion state
    WikiMigrationContext.setDeletionDone(getOrCreateOperationState(WikiMigrationContext.WIKI_RDBMS_DELETION_KEY));
    WikiMigrationContext.setPortalWikiCleanupDone(getOrCreateOperationState(WikiMigrationContext.WIKI_RDBMS_CLEANUP_PORTAL_WIKI_KEY));
    WikiMigrationContext.setSpaceWikiCleanupDone(getOrCreateOperationState(WikiMigrationContext.WIKI_RDBMS_CLEANUP_SPACE_WIKI_KEY));
    WikiMigrationContext.setUserWikiCleanupDone(getOrCreateOperationState(WikiMigrationContext.WIKI_RDBMS_CLEANUP_USER_WIKI_KEY));
    WikiMigrationContext.setEmoticonCleanupDone(getOrCreateOperationState(WikiMigrationContext.WIKI_RDBMS_CLEANUP_EMOTICON_KEY));
  }

  /**
   * Set all setting value to their default value (false) whatever their current value.
   *
   * Use to force a migration to be restart from the beginning
   */
  private void initMigrationSettingToDefault() {
    settingService = CommonsUtils.getService(SettingService.class);

    //Init migration state
    WikiMigrationContext.setMigrationDone(setOperationStatusToDefault(WikiMigrationContext.WIKI_RDBMS_MIGRATION_KEY));
    WikiMigrationContext.setPortalWikiMigrationDone(setOperationStatusToDefault(WikiMigrationContext.WIKI_RDBMS_MIGRATION_PORTAL_WIKI_KEY));
    WikiMigrationContext.setSpaceWikiMigrationDone(setOperationStatusToDefault(WikiMigrationContext.WIKI_RDBMS_MIGRATION_SPACE_WIKI_KEY));
    WikiMigrationContext.setUserWikiMigrationDone(setOperationStatusToDefault(WikiMigrationContext.WIKI_RDBMS_MIGRATION_USER_WIKI_KEY));
    WikiMigrationContext.setDraftPageMigrationDone(setOperationStatusToDefault(WikiMigrationContext.WIKI_RDBMS_MIGRATION_DRAFT_PAGE_KEY));
    WikiMigrationContext.setRelatedPageMigrationDone(setOperationStatusToDefault(WikiMigrationContext.WIKI_RDBMS_MIGRATION_RELATED_PAGE_KEY));

    //Init reindex state
    WikiMigrationContext.setReindexDone(setOperationStatusToDefault(WikiMigrationContext.WIKI_RDBMS_MIGRATION_REINDEX_KEY));

    //Init deletion state
    WikiMigrationContext.setDeletionDone(setOperationStatusToDefault(WikiMigrationContext.WIKI_RDBMS_DELETION_KEY));
    WikiMigrationContext.setPortalWikiCleanupDone(setOperationStatusToDefault(WikiMigrationContext.WIKI_RDBMS_CLEANUP_PORTAL_WIKI_KEY));
    WikiMigrationContext.setSpaceWikiCleanupDone(setOperationStatusToDefault(WikiMigrationContext.WIKI_RDBMS_CLEANUP_SPACE_WIKI_KEY));
    WikiMigrationContext.setUserWikiCleanupDone(setOperationStatusToDefault(WikiMigrationContext.WIKI_RDBMS_CLEANUP_USER_WIKI_KEY));
    WikiMigrationContext.setEmoticonCleanupDone(setOperationStatusToDefault(WikiMigrationContext.WIKI_RDBMS_CLEANUP_EMOTICON_KEY));
  }

  /**
   * Call settingService to get the state of an operation (Migration of User wiki for instance)
   *
   * @param operation operation
   * @return true if the operation already done, false if not
   */
  public boolean getOrCreateOperationState(String operation) {
    try {
      if (settingService == null) LOG.info("settingService is null");
      SettingValue<?> migrationValue =  settingService.get(Context.GLOBAL, Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY), operation);
      if (migrationValue != null) {
        return Boolean.parseBoolean(migrationValue.getValue().toString());
      } else {
        updateOperationStatus(operation, Boolean.FALSE);
        return false;
      }
    } finally {
      Scope.APPLICATION.id(null);
    }
  }

  /**
   * Set the value of the operation state to default (false)
   *
   * @param operation
   * @return the default value of the operation state
   */
  public boolean setOperationStatusToDefault(String operation) {
    updateOperationStatus(operation, Boolean.FALSE);
    return false;
  }

  public void setWikiMigrationOfTypeDone(String wikiType) {
    if (wikiType.equals(PortalConfig.PORTAL_TYPE)) {
      updateOperationStatus(WikiMigrationContext.WIKI_RDBMS_MIGRATION_PORTAL_WIKI_KEY, true);
    } else if (wikiType.equals(PortalConfig.GROUP_TYPE)) {
      updateOperationStatus(WikiMigrationContext.WIKI_RDBMS_MIGRATION_SPACE_WIKI_KEY, true);
    }
  }

  public void setWikiCleanupOfTypeDone(String wikiType) {
    if (wikiType.equals(PortalConfig.PORTAL_TYPE)) {
      updateOperationStatus(WikiMigrationContext.WIKI_RDBMS_CLEANUP_PORTAL_WIKI_KEY, true);
    } else if (wikiType.equals(PortalConfig.GROUP_TYPE)) {
      updateOperationStatus(WikiMigrationContext.WIKI_RDBMS_CLEANUP_SPACE_WIKI_KEY, true);
    }
  }

  /**
   * Update in the settingService the status of an operation
   *
   * @param key operation status
   * @param status
   */
  public void updateOperationStatus(String key, Boolean status) {
    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();
    try {
      settingService.set(Context.GLOBAL, Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY), key, SettingValue.create(status));
      try {
        CommonsUtils.getService(ChromatticManager.class).getLifeCycle("setting").getContext().getSession().save();
      } catch (Exception e) {
        LOG.warn(e);
      }
    } finally {
      Scope.APPLICATION.id(null);
      settingServiceImpl.stopSynchronization(created);
    }
  }

  /**
   * Remove from the SettingService the setting with the given key
   */
  public void removeSettingValue(String settingKey) {
    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();
    try {
      settingService.remove(Context.GLOBAL, Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY), settingKey);
      try {
        CommonsUtils.getService(ChromatticManager.class).getLifeCycle("setting").getContext().getSession().save();
      } catch (Exception e) {
        LOG.warn(e);
      }
    } finally {
      Scope.APPLICATION.id(null);
      settingServiceImpl.stopSynchronization(created);
    }
  }

  /**
   * Remove from the SettingService all settings related to the wiki migration
   */
  public void removeAllSettingValues() {
    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();
    try {
      settingService.remove(Context.GLOBAL, Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY));
      try {
        CommonsUtils.getService(ChromatticManager.class).getLifeCycle("setting").getContext().getSession().save();
      } catch (Exception e) {
        LOG.warn(e);
      }
    } finally {
      Scope.APPLICATION.id(null);
      settingServiceImpl.stopSynchronization(created);
    }
  }

  /**
   * Add a wiki to the "wiki errors" list stored in SettingService
   * The list is stored as a unique string where all wiki are separated by comma
   * See wikiToString() to check the format of a wiki in the string list
   *
   * @param wikiMigrationError the wiki in error during migration
   */
  public void addWikiErrorToSetting(Wiki wikiMigrationError) {
    String wiki = wikiToString(wikiMigrationError);
    addErrorToSetting(WikiMigrationContext.WIKI_RDBMS_MIGRATION_ERROR_WIKI_LIST_SETTING, wiki);
  }

  /**
   * Add a page to the "page errors" list stored in SettingService
   * The list is stored as a unique string where all page are separated by comma
   * See pageToString() to check the format of a page in the string list
   *
   * @param pageMigrationError the page in error during migration
   */
  public void addPageErrorToSetting(Page pageMigrationError) {
    String page = pageToString(pageMigrationError);
    addErrorToSetting(WikiMigrationContext.WIKI_RDBMS_MIGRATION_ERROR_PAGE_LIST_SETTING, page);
  }

  /**
   * Add a wiki to the "wiki deletion errors" list stored in SettingService
   * The list is stored as a unique string where all wiki are separated by comma
   * See wikiToString() to check the format of a wiki in the string list
   *
   * @param wikiDeletionError the wiki in error during deletion
   */
  public void addWikiDeletionErrorToSetting(Wiki wikiDeletionError) {
    String wiki = wikiToString(wikiDeletionError);
    addErrorToSetting(WikiMigrationContext.WIKI_RDBMS_DELETION_ERROR_WIKI_LIST_SETTING, wiki);
  }

  private void addErrorToSetting(String settingErrorKey, String settingErrorValue) {
    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();
    try {
      String migrationErrors = getErrorsSetting(settingErrorKey);
      //Add the error to the migrationErrors String list
      if (migrationErrors == null) {
        migrationErrors = settingErrorValue;
      } else {
        migrationErrors += OUTER_OBJECT_SPLIT+settingErrorValue;
      }
      SettingValue<String> errorsSetting = new SettingValue<>(migrationErrors);
      settingService.set(Context.GLOBAL, Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY), settingErrorKey, errorsSetting);
      try {
        CommonsUtils.getService(ChromatticManager.class).getLifeCycle("setting").getContext().getSession().save();
      } catch (Exception e) {
        LOG.warn(e);
      }
    } finally {
      Scope.APPLICATION.id(null);
      settingServiceImpl.stopSynchronization(created);
    }
  }

  /**
   * Get the "wiki error" list (a unique string with wiki separated by comma)
   * See wikiToString() to check the format of a wiki in the string list
   *
   * @return a unique string containing all wiki in error separated by a comma
   */
  public String getWikiErrorsSetting() {
    return getErrorsSetting(WikiMigrationContext.WIKI_RDBMS_MIGRATION_ERROR_WIKI_LIST_SETTING);
  }

  /**
   * Get the "page error" list (a unique string with wiki separated by comma)
   * See pageToString() to check the format of a page in the string list
   *
   * @return a unique string containing all page in error separated by a comma
   */
  public String getPageErrorsSetting() {
    return getErrorsSetting(WikiMigrationContext.WIKI_RDBMS_MIGRATION_ERROR_PAGE_LIST_SETTING);
  }

  /**
   * Get the "wiki deletion error" list (a unique string with wiki separated by comma)
   * See wikiToString() to check the format of a wiki in the string list
   *
   * @return a unique string containing all wiki in deletion error separated by a comma
   */
  public String getWikiDeletionErrorsSetting() {
    return getErrorsSetting(WikiMigrationContext.WIKI_RDBMS_DELETION_ERROR_WIKI_LIST_SETTING);
  }

  private String getErrorsSetting(String settingErrorKey) {

    String migrationErrors = null;

    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();

    try {
      SettingValue settingValue = settingService.get(
          Context.GLOBAL,
          Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY),
          settingErrorKey);
      if (settingValue != null) {
        migrationErrors = (String) settingValue.getValue();
      }
    } finally {
      Scope.APPLICATION.id(null);
      settingServiceImpl.stopSynchronization(created);
    }

    return migrationErrors;
  }

  /**
   * Add a page to the list of "page with related pages" stored in SettingService
   * The list is stored as a unique string where all page are separated by comma
   * See pageToString() to check the format of a page in the string list
   *
   * @param relatedPage
   */
  public void addRelatedPagesToSetting(Page relatedPage) {
    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();
    try {
      String relatedPages = getRelatedPagesSetting();
      //Add the page to the relatedPages String list
      if (relatedPages == null) {
        relatedPages = pageToString(relatedPage);
      } else {
        relatedPages += OUTER_OBJECT_SPLIT+pageToString(relatedPage);
      }
      SettingValue<String> relatedPageSetting = new SettingValue<>(relatedPages);
      settingService.set(Context.GLOBAL, Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY), WikiMigrationContext.WIKI_RDBMS_MIGRATION_RELATED_PAGE_LIST_SETTING, relatedPageSetting);
      try {
        CommonsUtils.getService(ChromatticManager.class).getLifeCycle("setting").getContext().getSession().save();
      } catch (Exception e) {
        LOG.warn(e);
      }
    } finally {
      Scope.APPLICATION.id(null);
      settingServiceImpl.stopSynchronization(created);
    }
  }

  /**
   * Get the "page with related pages" list (a unique string with wiki separated by comma)
   * The migration of link between page is done at the end of migration and use this list
   * See pageToString() to check the format of a page in the string list
   *
   * @return a unique string containing all page in error separated by a comma
   */
  public String getRelatedPagesSetting() {

    String relatedPage = null;

    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();

    try {
      SettingValue settingValue = settingService.get(
          Context.GLOBAL,
          Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY),
          WikiMigrationContext.WIKI_RDBMS_MIGRATION_RELATED_PAGE_LIST_SETTING);
      if (settingValue != null) {
        relatedPage = (String) settingValue.getValue();
      }
    } finally {
      Scope.APPLICATION.id(null);
      settingServiceImpl.stopSynchronization(created);
    }

    return relatedPage;
  }

  /**
   * Get the "page with related pages" list in a String Array format
   *
   * @return an array of String representing a page
   */
  public String[] getPagesWithRelatedPages() {
    String pageWithRelatedPages = getRelatedPagesSetting();
    if (pageWithRelatedPages != null) return pageWithRelatedPages.split(OUTER_OBJECT_SPLIT);
    return null;
  }

  /**
   *
   * @return the number of wiki in error during the migration
   */
  public Integer getWikiMigrationErrorsNumber() {
    String wikiMigrationErrors = getWikiErrorsSetting();
    return (wikiMigrationErrors != null ? wikiMigrationErrors.split(OUTER_OBJECT_SPLIT).length : 0);
  }

  /**
   *
   * @return the number of wiki in error during the deletion
   */
  public Integer getWikiDeletionErrorsNumber() {
    String wikiDeletionErrors = getWikiDeletionErrorsSetting();
    return (wikiDeletionErrors != null ? wikiDeletionErrors.split(OUTER_OBJECT_SPLIT).length : 0);
  }

  /**
   *
   * @return the number of pages in error during the migration
   */
  public Integer getPageErrorsNumber() {
    String pageErrors = getPageErrorsSetting();
    if (pageErrors != null) return pageErrors.split(OUTER_OBJECT_SPLIT).length;
    return 0;
  }

  /**
   * Transform a wiki to string to store it to the "wiki error" String list
   *
   * @param wiki wiki to transform to string format
   * @return wiki in String format: WikiType:WikiOwner
   */
  public String wikiToString(Wiki wiki) {
    return wiki.getType()+INNER_OBJECT_SPLIT+wiki.getOwner();
  }

  /**
   * Transform a wikiImpl to string to store it to the "wiki error" String list
   *
   * @param wiki wiki to transform to string format
   * @return wiki in String format: WikiType:WikiOwner
   */
  public String wikiToString(WikiImpl wiki) {
    return wiki.getType()+INNER_OBJECT_SPLIT+wiki.getOwner();
  }

  /**
   *
   * @param pageString String to transform Page object
   * @return
   */
  public Page stringToPage(String pageString) {
    String[] pageAttribute = pageString.split(INNER_OBJECT_SPLIT);
    Page page = new Page();
    page.setWikiType(pageAttribute[0]);
    page.setWikiOwner(pageAttribute[1]);
    page.setId(pageAttribute[2]);
    page.setName(pageAttribute[3]);
    return page;
  }

  /**
   * Transform a page to string to store it to the "page error" or "page with related page" String list
   *
   * @param page page to transform to string format
   * @return page in String format: WikiType:WikiOwner:PageId:PageName
   */
  public String pageToString(Page page) {
    return page.getWikiType()+INNER_OBJECT_SPLIT+page.getWikiOwner()+INNER_OBJECT_SPLIT+page.getId()+INNER_OBJECT_SPLIT+page.getName();
  }

  /**
   * Set the list of "page with related pages" stored in SettingService
   * The list is stored as a unique string where all page are separated by comma
   * See pageToString() to check the format of a page in the string list
   *
   * @param pagesWithRelatedPagesString the list of "page with related pages"
   */
  public void setRelatedPagesToSetting(String pagesWithRelatedPagesString) {
    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();
    try {
      SettingValue<String> relatedPageSetting = new SettingValue<>(pagesWithRelatedPagesString);
      settingService.set(Context.GLOBAL, Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY), WikiMigrationContext.WIKI_RDBMS_MIGRATION_RELATED_PAGE_LIST_SETTING, relatedPageSetting);
      try {
        CommonsUtils.getService(ChromatticManager.class).getLifeCycle("setting").getContext().getSession().save();
      } catch (Exception e) {
        LOG.warn(e);
      }
    } finally {
      Scope.APPLICATION.id(null);
      settingServiceImpl.stopSynchronization(created);
    }
  }
}

