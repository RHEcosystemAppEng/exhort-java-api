package com.redhat.exhort.utils;

import com.redhat.exhort.tools.Operations;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class PythonControllerRealEnv extends PythonControllerBase{
  public PythonControllerRealEnv(String pathToPythonBin,String pathToPip) {
    Path pipPath = Path.of(pathToPip);
    this.pipBinaryDir = pipPath.getParent();
    if(this.pipBinaryDir == null)
    {
      this.pipBinaryDir = pipPath;
    }
    this.pythonEnvironmentDir = Path.of(System.getProperty("user.dir"));
    this.pathToPythonBin = pathToPythonBin;
  }

  @Override
  public void prepareEnvironment(String pathToPythonBin)
  {
    String envBinDir = pipBinaryDir.toString();
    if(envBinDir.contains(FileSystems.getDefault().getSeparator())) {
      if (pathToPythonBin.contains("python3")) {
        this.pipBinaryLocation = Path.of(envBinDir, "pip3").toString();
      } else {
        this.pipBinaryLocation = Path.of(envBinDir, "pip").toString();
      }
    }
    else
    {
      this.pipBinaryLocation = envBinDir;
    }
  }

  @Override
  public boolean automaticallyInstallPackageOnEnvironment() {
    return false;
  }
  @Override
  public boolean isVirtualEnv()
  {
    return false;
  }

  @Override
  public void cleanEnvironment() {

    //noop as this is real environment,
  }

}
