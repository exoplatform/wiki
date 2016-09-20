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

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 1/21/16
 */
public class WikiMigrationContext {

  public static final String WIKI_MIGRATION_SETTING_GLOBAL_KEY = "WIKI_MIGRATION_SETTING_GLOBAL";
  public static final String WIKI_RDBMS_MIGRATION_KEY = "WIKI_RDBMS_MIGRATION_DONE";
  public static final String WIKI_RDBMS_MIGRATION_PORTAL_WIKI_KEY = "WIKI_RDBMS_MIGRATION_PORTAL_WIKI_DONE";
  public static final String WIKI_RDBMS_MIGRATION_SPACE_WIKI_KEY = "WIKI_RDBMS_MIGRATION_SPACE_WIKI_DONE";
  public static final String WIKI_RDBMS_MIGRATION_USER_WIKI_KEY = "WIKI_RDBMS_MIGRATION_USER_WIKI_DONE";
  public static final String WIKI_RDBMS_MIGRATION_DRAFT_PAGE_KEY = "WIKI_RDBMS_MIGRATION_DRAFT_PAGE_DONE";
  public static final String WIKI_RDBMS_MIGRATION_RELATED_PAGE_KEY = "WIKI_RDBMS_MIGRATION_RELATED_PAGE_DONE";

  public static final String WIKI_RDBMS_MIGRATION_REINDEX_KEY = "WIKI_RDBMS_MIGRATION_REINDEX_DONE";

  public static final String WIKI_RDBMS_DELETION_KEY = "WIKI_RDBMS_DELETION_DONE";
  public static final String WIKI_RDBMS_CLEANUP_PORTAL_WIKI_KEY = "WIKI_RDBMS_CLEANUP_PORTAL_WIKI_DONE";
  public static final String WIKI_RDBMS_CLEANUP_SPACE_WIKI_KEY = "WIKI_RDBMS_CLEANUP_SPACE_WIKI_DONE";
  public static final String WIKI_RDBMS_CLEANUP_USER_WIKI_KEY = "WIKI_RDBMS_CLEANUP_USER_WIKI_DONE";
  public static final String WIKI_RDBMS_CLEANUP_EMOTICON_KEY = "WIKI_RDBMS_CLEANUP_DRAFT_PAGE_DONE";

  public static final String WIKI_RDBMS_MIGRATION_ERROR_WIKI_LIST_SETTING = "WIKI_RDBMS_MIGRATION_ERROR_WIKI_LIST";
  public static final String WIKI_RDBMS_MIGRATION_ERROR_PAGE_LIST_SETTING = "WIKI_RDBMS_MIGRATION_ERROR_PAGE_LIST";
  public static final String WIKI_RDBMS_DELETION_ERROR_WIKI_LIST_SETTING = "WIKI_RDBMS_DELETION_ERROR_WIKI_LIST";

  public static final String WIKI_RDBMS_MIGRATION_RELATED_PAGE_LIST_SETTING = "WIKI_RDBMS_MIGRATION_RELATED_PAGE_LIST";

  public static final String WIKI_RDBMS_MIGRATION_FORCE_DELETION_PROPERTY_NAME = "exo.wiki.migration.forceJCRDeletion";
  public static final String WIKI_RDBMS_MIGRATION_FORCE_MIGRATION_PROPERTY_NAME = "exo.wiki.migration.forceRunMigration";

  private static boolean migrationDone = false;
  private static boolean portalWikiMigrationDone = false;
  private static boolean spaceWikiMigrationDone = false;
  private static boolean userWikiMigrationDone = false;
  private static boolean draftPageMigrationDone = false;
  private static boolean relatedPageMigrationDone = false;

  private static boolean reindexDone = false;

  private static boolean deletionDone = false;
  private static boolean portalWikiCleanupDone = false;
  private static boolean spaceWikiCleanupDone = false;
  private static boolean userWikiCleanupDone = false;
  private static boolean emoticonCleanupDone = false;

  public static boolean isDeletionDone() {
    return deletionDone;
  }

  public static void setDeletionDone(boolean deletionDone) {
    WikiMigrationContext.deletionDone = deletionDone;
  }

  public static boolean isReindexDone() {
    return reindexDone;
  }

  public static void setReindexDone(boolean reindexDone) {
    WikiMigrationContext.reindexDone = reindexDone;
  }

  public static boolean isMigrationDone() {
    return migrationDone;
  }

  public static void setMigrationDone(boolean migrationDone) {
    WikiMigrationContext.migrationDone = migrationDone;
  }

  public static boolean isPortalWikiMigrationDone() {
    return portalWikiMigrationDone;
  }

  public static void setPortalWikiMigrationDone(boolean portalWikiMigrationDone) {
    WikiMigrationContext.portalWikiMigrationDone = portalWikiMigrationDone;
  }

  public static boolean isSpaceWikiMigrationDone() {
    return spaceWikiMigrationDone;
  }

  public static void setSpaceWikiMigrationDone(boolean spaceWikiMigrationDone) {
    WikiMigrationContext.spaceWikiMigrationDone = spaceWikiMigrationDone;
  }

  public static boolean isUserWikiMigrationDone() {
    return userWikiMigrationDone;
  }

  public static void setUserWikiMigrationDone(boolean userWikiMigrationDone) {
    WikiMigrationContext.userWikiMigrationDone = userWikiMigrationDone;
  }

  public static boolean isDraftPageMigrationDone() {
    return draftPageMigrationDone;
  }

  public static void setDraftPageMigrationDone(boolean draftPageMigrationDone) {
    WikiMigrationContext.draftPageMigrationDone = draftPageMigrationDone;
  }

  public static boolean isRelatedPageMigrationDone() {
    return relatedPageMigrationDone;
  }

  public static void setRelatedPageMigrationDone(boolean relatedPageMigrationDone) {
    WikiMigrationContext.relatedPageMigrationDone = relatedPageMigrationDone;
  }

  public static boolean isPortalWikiCleanupDone() {
    return portalWikiCleanupDone;
  }

  public static void setPortalWikiCleanupDone(boolean portalWikiCleanupDone) {
    WikiMigrationContext.portalWikiCleanupDone = portalWikiCleanupDone;
  }

  public static boolean isSpaceWikiCleanupDone() {
    return spaceWikiCleanupDone;
  }

  public static void setSpaceWikiCleanupDone(boolean spaceWikiCleanupDone) {
    WikiMigrationContext.spaceWikiCleanupDone = spaceWikiCleanupDone;
  }

  public static boolean isUserWikiCleanupDone() {
    return userWikiCleanupDone;
  }

  public static void setUserWikiCleanupDone(boolean userWikiCleanupDone) {
    WikiMigrationContext.userWikiCleanupDone = userWikiCleanupDone;
  }

  public static boolean isEmoticonCleanupDone() {
    return emoticonCleanupDone;
  }

  public static void setEmoticonCleanupDone(boolean emoticonCleanupDone) {
    WikiMigrationContext.emoticonCleanupDone = emoticonCleanupDone;
  }
}

