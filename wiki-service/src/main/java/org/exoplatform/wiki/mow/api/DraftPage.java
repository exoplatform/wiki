package org.exoplatform.wiki.mow.api;

import org.exoplatform.wiki.service.diff.DiffResult;

public class DraftPage extends Page {
  private String targetPage;

  private String targetRevision;

  private boolean newPage;

  private DiffResult changes;

  public String getTargetPage() {
    return targetPage;
  }

  public void setTargetPage(String targetPage) {
    this.targetPage = targetPage;
  }

  public String getTargetRevision() {
    return targetRevision;
  }

  public void setTargetRevision(String targetRevision) {
    this.targetRevision = targetRevision;
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
