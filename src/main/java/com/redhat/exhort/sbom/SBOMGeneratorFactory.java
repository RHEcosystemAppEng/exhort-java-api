package com.redhat.exhort.sbom;

public class SBOMGeneratorFactory {

  public static CycloneDxSBOMGenerator getSbomGenerator(String ecosystem)
  {
    CycloneDxSBOMGenerator generator;
    switch (ecosystem)
    {
      case "npm":
        generator = new NpmSBomGenerator();
        break;
      default:
        throw new IllegalArgumentException(String.format("Ecosystem %s is either Still not implemented or wrong"));
    }
    return generator;
  }




}
