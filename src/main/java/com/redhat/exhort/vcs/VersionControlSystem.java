package com.redhat.exhort.vcs;

import java.nio.file.Path;

public interface VersionControlSystem {
  /**
   * This method gets the latest tag in a repo,  information whether current commit pointed by
   * a tag or not, and a short hash digest of the current commit
   * @param repoLocation - the repo directory path with inner .git directory
   * @return {@link TagInfo} containing the latest tag
   */
  TagInfo getLatestTag(Path repoLocation);

  /**
   *
   * @param repoLocation - the directory path to be checked whether it's a git repo or not
   * @return {boolean} - returns true if the directory is a repo.
   */
  boolean isDirectoryRepo(Path repoLocation);

  /**
   *
   * @param tagInfo - object that contains the tag info in order to calculate next version
   * @return A String containing the next version
   */
  String getNextTagVersion(TagInfo tagInfo);

  /**
   *
   * @param tagInfo - object that contains the tag info and current commit data
   * @param newTagVersion
   * @return
   */
  String getPseudoVersion(TagInfo tagInfo , String newTagVersion);

}
