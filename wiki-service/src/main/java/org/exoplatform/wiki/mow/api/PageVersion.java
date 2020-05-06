package org.exoplatform.wiki.mow.api;

public class PageVersion extends Page {
  private String[] predecessors;

  private String[] successors;

  public String[] getPredecessors() {
    return predecessors;
  }

  public void setPredecessors(String[] predecessors) {
    this.predecessors = predecessors;
  }

  public String[] getSuccessors() {
    return successors;
  }

  public void setSuccessors(String[] successors) {
    this.successors = successors;
  }
}
