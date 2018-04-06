package org.exoplatform.wiki.mow.api;

import java.util.Date;

public class PageVersion {
  private String name;

  private String author;

  private Date createdDate;

  private Date updatedDate;

  private String content;

  private String comment;

  private String[] predecessors;

  private String[] successors;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Date getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

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
