package org.exoplatform.wiki.mow.api;

import org.exoplatform.wiki.service.diff.DiffResult;

public interface DraftPage extends Page {
  String getTargetPage();
  void setTargetPage(String targetPage);
  
  String getTargetRevision();
  void setTargetRevision(String targetRevision);
  
  boolean isNewPage();
  void setNewPage(boolean isNewPage);
  
  public boolean isOutDate() throws Exception;
  
  public DiffResult getChanges() throws Exception;
}
