package org.exoplatform.wiki.migration;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.file.services.NameSpaceService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.jpa.JPADataStorage;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Attachment Migration service :  Custom tag Liquibase implements CustomTaskChange
 * This service will be used to migrate attachments wiki page and draft page from WIKI_PAGE_ATTACHMENTS and WIKI_DRAFT_ATTACHMENTS
 * To File Rdbms storage.
 */
public class AttachmentsMigration implements CustomTaskChange {
  private static final Log  LOG                            = ExoLogger.getLogger(AttachmentsMigration.class);

  private static String     PAGE_ATTACHMENTS_COUNT         = "select count(*) from WIKI_PAGE_ATTACHMENTS";

  private static String     DRAFT_ATTACHMENTS_COUNT        = "select count(*) from WIKI_DRAFT_ATTACHMENTS";

  private static String     PAGE_ATTACHMENTS_SELECT_QUERY  =
                                                          "select P.ATTACHMENT_ID, P.NAME, P.CREATOR, P.CREATED_DATE, P.UPDATED_DATE, P.MIMETYPE, P.CONTENT from WIKI_PAGE_ATTACHMENTS P where P.ATTACHMENT_ID>= ? and P.ATTACHMENT_ID <?";

  private static String     PAGE_ATTACHMENTS_UPDATE_QUERY  =
                                                          "update WIKI_PAGE_ATTACHMENTS set ATTACHMENT_FILE_ID= ? , CREATED_DATE= ?  where ATTACHMENT_ID = ?";

  private static String     DRAFT_ATTACHMENTS_SELECT_QUERY =
                                                           "select P.ATTACHMENT_ID, P.NAME, P.CREATOR, P.CREATED_DATE, P.UPDATED_DATE, P.MIMETYPE, P.CONTENT from WIKI_DRAFT_ATTACHMENTS P  where P.ATTACHMENT_ID>= ? and P.ATTACHMENT_ID <?";

  private static String     DRAFT_ATTACHMENTS_UPDATE_QUERY =
                                                           "update WIKI_DRAFT_ATTACHMENTS set ATTACHMENT_FILE_ID= ? , CREATED_DATE= ? where ATTACHMENT_ID = ?";

  private PreparedStatement findPageAttachment;

  private PreparedStatement findDraftAttachment;

  private PreparedStatement updatePageAttachment;

  private PreparedStatement updateDraftAttachment;

  private PreparedStatement findPageAttachmentCount;

  private PreparedStatement findDraftAttachmentCount;

  @Override
  public void execute(Database database) throws CustomChangeException {
    JdbcConnection dbConn = (JdbcConnection) database.getConnection();
    FileService fileService = CommonsUtils.getService(FileService.class);
    NameSpaceService nameService = CommonsUtils.getService(NameSpaceService.class);
    ResultSet attachmentSet = null;
    int errorNumber = 0;
    int pageSize = 100;
    int fromId;
    int toId = 0;
    long startTime = System.currentTimeMillis();
    boolean autoCommit =  false;

    try (PreparedStatement findPageAttachment_ = dbConn.prepareStatement(PAGE_ATTACHMENTS_SELECT_QUERY);
        PreparedStatement findDraftAttachment_ = dbConn.prepareStatement(DRAFT_ATTACHMENTS_SELECT_QUERY);
        PreparedStatement updatePageAttachment_ = dbConn.prepareStatement(PAGE_ATTACHMENTS_UPDATE_QUERY);
        PreparedStatement updateDraftAttachment_ = dbConn.prepareStatement(DRAFT_ATTACHMENTS_UPDATE_QUERY);
        PreparedStatement findPageAttachmentCount_ = dbConn.prepareStatement(PAGE_ATTACHMENTS_COUNT);
        PreparedStatement findDraftAttachmentCount_ = dbConn.prepareStatement(DRAFT_ATTACHMENTS_COUNT);) {

      findPageAttachment = findPageAttachment_;
      findDraftAttachment = findDraftAttachment_;
      updatePageAttachment = updatePageAttachment_;
      updateDraftAttachment = updateDraftAttachment_;
      findPageAttachmentCount = findPageAttachmentCount_;
      findDraftAttachmentCount = findDraftAttachmentCount_;

      autoCommit = dbConn.getAutoCommit();
      dbConn.setAutoCommit(false);
      
      // create wiki namespace if not exist
      nameService.createNameSpace(JPADataStorage.WIKI_FILES_NAMESPACE_NAME, JPADataStorage.WIKI_FILES_NAMESPACE_DESCRIPTION);

      int attachmentSize = findPageAttachmentCount() + findDraftAttachmentCount();
      int count = 0;
      int start;

      // start the migration
      LOG.info("=== Start Wiki attachments migration to FILES RDBMS");
      startTime = System.currentTimeMillis();

      // start the migration of Page Attachment
      boolean hasNext = true;
      while (hasNext) {
        start = count;
        fromId = toId;
        toId = fromId + pageSize;
        attachmentSet = findPageAttachment(fromId, toId);

        while (attachmentSet.next()) {
          count++;
          long id = attachmentSet.getLong("ATTACHMENT_ID");
          LOG.info("Start Migration page attachment id {}", id);

          byte[] content = attachmentSet.getBytes("CONTENT");
          long contentSize = 0;
          Date updatedDate = null;
          if (content != null) {
            contentSize = content.length;
          }
          try {
            if (attachmentSet.getTimestamp("UPDATED_DATE") != null) {
              updatedDate = new Date(attachmentSet.getTimestamp("UPDATED_DATE").getTime());
            }
            FileItem fileItem = new FileItem(null,
                                             attachmentSet.getString("NAME"),
                                             attachmentSet.getString("MIMETYPE"),
                                             JPADataStorage.WIKI_FILES_NAMESPACE_NAME,
                                             contentSize,
                                             updatedDate,
                                             attachmentSet.getString("CREATOR"),
                                             false,
                                             new ByteArrayInputStream(content));
            fileItem = fileService.writeFile(fileItem);
            if (fileItem.getFileInfo().getId() != null) {
              Timestamp createdDate = attachmentSet.getTimestamp("CREATED_DATE");
              updatePageAttachment(id, fileItem.getFileInfo().getId(), createdDate);
              dbConn.commit();
            }
            LOG.info("Migration page attachment id {} Done, progress {}/{}", id, count, attachmentSize);
          } catch (Exception e) {
            errorNumber++;
            LOG.error("Error while migrate Wiki Page Attachment  ID = " + id + "  data to File RDBMS - Cause : " + e.getMessage(),
                      e);
          }
        }
        if (count == start) {
          hasNext = false;
        }
      }

      // start the migration of Draft Attachment
      hasNext = true;
      toId = 0;
      while (hasNext) {
        start = count;
        fromId = toId;
        toId = fromId + pageSize;
        attachmentSet = findDraftAttachment(fromId, toId);

        while (attachmentSet.next()) {
          count++;
          long id = attachmentSet.getLong("ATTACHMENT_ID");
          LOG.info("Start Migration draft attachment id {}", id);

          byte[] content = attachmentSet.getBytes("CONTENT");
          long contentSize = 0;
          Date updatedDate = null;
          if (content != null) {
            contentSize = content.length;
          }
          try {
            if (attachmentSet.getTimestamp("UPDATED_DATE") != null) {
              updatedDate = new Date(attachmentSet.getTimestamp("UPDATED_DATE").getTime());
            }
            FileItem fileItem = new FileItem(null,
                                             attachmentSet.getString("NAME"),
                                             attachmentSet.getString("MIMETYPE"),
                                             JPADataStorage.WIKI_FILES_NAMESPACE_NAME,
                                             contentSize,
                                             updatedDate,
                                             attachmentSet.getString("CREATOR"),
                                             false,
                                             new ByteArrayInputStream(content));
            fileItem = fileService.writeFile(fileItem);
            if (fileItem.getFileInfo().getId() != null) {
              Timestamp createdDate = attachmentSet.getTimestamp("CREATED_DATE");
              updateDraftAttachment(id, fileItem.getFileInfo().getId(), createdDate);
              dbConn.commit();
            }
            LOG.info("Migration draft attachment id {} Done, progress {}/{}", id, count, attachmentSize);
          } catch (Exception e) {
            errorNumber++;
            LOG.error("Error while migrate Wiki Draft Attachment ID = " + id + " data to File RDBMS - Cause : " + e.getMessage(),
                      e);
          }
        }
        if (count == start) {
          hasNext = false;
        }
      }

    } catch (Exception e) {
      LOG.error("cannot start Attachments migration  - Cause : " + e.getMessage(), e);
    } finally {

      long endTime = System.currentTimeMillis();
      if (errorNumber == 0) {
        LOG.info("No error during migration");
      } else {
        LOG.info("Numbers of wiki attachments in error during migration attachment to File Rdbms = " + errorNumber);
      }
      LOG.info("===  Wiki attachments migration to File RDBMS done in " + (endTime - startTime) + " ms");
      try {
        attachmentSet.close();
      } catch (SQLException e) {
        LOG.error("Error during close ResultSet  - Cause : " + e.getMessage(), e);
      }

      try {
        dbConn.setAutoCommit(autoCommit);
      } catch (DatabaseException e) {
        LOG.error("Error during set AutoCommit  - Cause : " + e.getMessage(), e);
      }
    }

  }

  @Override
  public String getConfirmationMessage() {
    return "Wiki attachments migrated";
  }

  @Override
  public void setUp() throws SetupException {

  }

  @Override
  public void setFileOpener(ResourceAccessor resourceAccessor) {

  }

  @Override
  public ValidationErrors validate(Database database) {
    return null;
  }

  private ResultSet findPageAttachment(long from, long to) throws SQLException, DatabaseException {
    findPageAttachment.clearParameters();
    findPageAttachment.setLong(1, from);
    findPageAttachment.setLong(2, to);
    return findPageAttachment.executeQuery();
  }

  private ResultSet findDraftAttachment(long from, long to) throws SQLException, DatabaseException {
    findDraftAttachment.clearParameters();
    findDraftAttachment.setLong(1, from);
    findDraftAttachment.setLong(2, to);
    return findDraftAttachment.executeQuery();
  }

  private int updatePageAttachment(long id, long fileID, Timestamp created) throws SQLException {
    updatePageAttachment.clearParameters();

    updatePageAttachment.setLong(1, fileID);
    updatePageAttachment.setTimestamp(2, created);
    updatePageAttachment.setLong(3, id);

    return updatePageAttachment.executeUpdate();
  }

  private int updateDraftAttachment(long id, long fileID, Timestamp created) throws SQLException {
    updateDraftAttachment.clearParameters();

    updateDraftAttachment.setLong(1, fileID);
    updateDraftAttachment.setTimestamp(2, created);
    updateDraftAttachment.setLong(3, id);

    return updateDraftAttachment.executeUpdate();
  }

  private int findPageAttachmentCount() throws SQLException {
    try (ResultSet count = findPageAttachmentCount.executeQuery()) {
      if (count.next()) {
        return count.getInt(1);
      } else {
        return 0;
      }
    }

  }

  private int findDraftAttachmentCount() throws SQLException {
    try (ResultSet count = findDraftAttachmentCount.executeQuery()) {
      if (count.next()) {
        return count.getInt(1);
      } else {
        return 0;
      }
    }

  }
}
