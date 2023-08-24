package com.redhat.exhort.vcs;

import java.time.LocalDateTime;

public class TagInfo {

  private String tagName;
  private boolean currentCommitPointedByTag;
  private String currentCommitDigest;

  public LocalDateTime getCommitTimestamp() {
    return commitTimestamp;
  }

  public void setCommitTimestamp(LocalDateTime commitTimestamp) {
    this.commitTimestamp = commitTimestamp;
  }

  private LocalDateTime commitTimestamp;

  public String getTagName() {
    return tagName;
  }

  public void setTagName(String tagName) {
    this.tagName = tagName;
  }

  public boolean isCurrentCommitPointedByTag() {
    return currentCommitPointedByTag;
  }

  public void setCurrentCommitPointedByTag(boolean currentCommitPointedByTag) {
    this.currentCommitPointedByTag = currentCommitPointedByTag;
  }

  public String getCurrentCommitDigest() {
    return currentCommitDigest;
  }

  public void setCurrentCommitDigest(String currentCommitDigest) {
    this.currentCommitDigest = currentCommitDigest;
  }
}
