package org.exoplatform.wiki.liquibase;

import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.ModifyDataTypeStatement;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Custom tag Liquibase implements CustomSqlChange
 * This service will be used to be able to set correctly the UTC time  (use '1970-01-01 00:00:01' as default TIMESTAMP).
 */
public class TimestampChange implements CustomSqlChange {

  private String tableName;

  private String columnName;

  @Override
  public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
      SqlStatement[]  sqlStatements = new SqlStatement[1];

      sqlStatements[0] = new ModifyDataTypeStatement(database.getDefaultCatalogName(),database.getDefaultSchemaName(),
              tableName,columnName,"TIMESTAMP NOT NULL DEFAULT '" +getLocalToUtcDelta() +"'");
    return sqlStatements;
  }

  @Override
  public String getConfirmationMessage() {
    return null;
  }

  @Override
  public void setUp() throws SetupException {

  }

  @Override
  public void setFileOpener(ResourceAccessor resourceAccessor) {
    try {
      resourceAccessor.getResourcesAsStream("tableName");
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Override
  public ValidationErrors validate(Database database) {
    return null;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public static String getLocalToUtcDelta() {
    Calendar utcDate = Calendar.getInstance();
    utcDate.set(1970, Calendar.JANUARY, 1, 0, 0, 1);

    String format = "yyyy/MM/dd HH:mm:ss";
    SimpleDateFormat sdf = new SimpleDateFormat(format);

    TimeZone tz = TimeZone.getDefault();
    Date local = utcDate.getTime();
    if (tz.getRawOffset() > 0) {
      local = new Date(utcDate.getTime().getTime() + tz.getRawOffset());
    }

    return sdf.format(local);
  }
}
