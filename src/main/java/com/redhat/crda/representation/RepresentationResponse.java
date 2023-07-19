package com.redhat.crda.representation;


public class RepresentationResponse {

  private Representation representation;
  private String contentType;

  private String actualContent;


  public Representation getRepresentation() {
    return representation;
  }

  public void setRepresentation(Representation representation) {
    this.representation = representation;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getActualContent() {
    return actualContent;
  }

  public void setActualContent(String actualContent) {
    this.actualContent = actualContent;
  }
}
