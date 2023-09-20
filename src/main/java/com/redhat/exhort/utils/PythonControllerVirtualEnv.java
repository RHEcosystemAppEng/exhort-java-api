package com.redhat.exhort.utils;

import com.redhat.exhort.tools.Operations;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class PythonControllerVirtualEnv extends PythonControllerBase{
  public PythonControllerVirtualEnv(String pathToPythonBin) {
    this.pipBinaryDir = Path.of(FileSystems.getDefault().getSeparator(), "tmp","exhort_env","bin");
    this.pythonEnvironmentDir = Path.of(FileSystems.getDefault().getSeparator(),"tmp","exhort_env");
    this.pathToPythonBin = pathToPythonBin;
  }

  @Override
  public void prepareEnvironment(String pathToPythonBin)
  {
    try {
      if(!Files.exists(pythonEnvironmentDir)) {
        Files.createDirectory(pythonEnvironmentDir);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Operations.runProcess(new String[]{pathToPythonBin,"-m","venv",pythonEnvironmentDir.toString()});
    String envBinDir = pipBinaryDir.toString();
    if(pathToPythonBin.contains("python3"))
    {
      this.pipBinaryLocation = Path.of(envBinDir,"pip3").toString();
    }
    else
    {
      this.pipBinaryLocation = Path.of(envBinDir,"pip").toString();
    }

  }

  @Override
  public boolean automaticallyInstallPackageOnEnvironment() {
    return true;
  }
  @Override
  public boolean isVirtualEnv()
  {
    return true;
  }

  @Override
  public void cleanEnvironment()
  {
    Path envRequirements = Path.of(pythonEnvironmentDir.toString(), "requirements.txt");
    try {
      Files.deleteIfExists(envRequirements);
      String freezeOutput = Operations.runProcessGetOutput(pythonEnvironmentDir, pipBinaryLocation, "freeze");
      Files.createFile(envRequirements);
      Files.write(envRequirements,freezeOutput.getBytes());
      Operations.runProcessGetOutput(pythonEnvironmentDir, pipBinaryLocation, "uninstall","-y","-r","requirements.txt");

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
