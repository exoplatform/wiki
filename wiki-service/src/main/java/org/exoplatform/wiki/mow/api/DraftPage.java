package org.exoplatform.wiki.mow.api;

import org.exoplatform.wiki.service.diff.DiffResult;

public class DraftPage extends Page {
  private String targetPageId;

  private String targetPageRevision;

  private boolean newPage;

  private DiffResult changes;

  public String getTargetPageId() {
    return targetPageId;
  }

  public void setTargetPageId(String targetPageId) {
    this.targetPageId = targetPageId;
  }

  public String getTargetPageRevision() {
    return targetPageRevision;
  }

  public void setTargetPageRevision(String targetPageRevision) {
    this.targetPageRevision = targetPageRevision;
  }

  public boolean isNewPage() {
    return newPage;
  }

  public void setNewPage(boolean newPage) {
    this.newPage = newPage;
  }

  public DiffResult getChanges() {
    return changes;
  }

  public void setChanges(DiffResult changes) {
    this.changes = changes;
  }
}
