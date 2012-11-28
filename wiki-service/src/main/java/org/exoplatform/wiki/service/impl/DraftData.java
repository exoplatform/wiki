package org.exoplatform.wiki.service.impl;

public class DraftData {
  private String draftName;
  
  public DraftData(String draftName) {
    this.draftName = draftName;
  }

  public String getDraftName() {
    return draftName;
  }

  public void setDraftName(String draftName) {
    this.draftName = draftName;
  }
}
