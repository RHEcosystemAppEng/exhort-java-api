package com.redhat.exhort.utils;

import com.redhat.exhort.ExhortTest;
import com.redhat.exhort.tools.Operations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class PythonControllerRealEnvTest extends ExhortTest {

  private static PythonControllerRealEnv pythonControllerRealEnv;
  private final String PIP_FREEZE_LINES_CYCLIC = getFileIntoString("msc","python","pip_freeze_lines_cyclic.txt");
  private final String PIP_SHOW_LINES_CYCLIC = getFileIntoString("msc","python","pip_show_lines_cyclic.txt");



  @BeforeEach
   void setUp() {
    pythonControllerRealEnv = new PythonControllerRealEnv("python3","pip3");
  }

  @AfterEach
  void tearDown() {
  }


  @Test
  void getDependencies() {
    MockedStatic<Operations> operationsMockedStatic = Mockito.mockStatic(Operations.class);
//    ArgumentMatcher<String[]> matchCommandPipFreeze = command -> Arrays.stream(command).anyMatch(word -> word.contains("freeze"));
    ArgumentMatcher<String[]> matchCommandPipFreeze = new ArgumentMatcher<String[]>() {
      @Override
      public boolean matches(String[] command) {
        return Arrays.stream(command).anyMatch(word -> word.contains("freeze"));
      }

      @Override
      public Class type()
      {
        return String[].class;
      }

    };

    ArgumentMatcher<String[]> matchCommandPipShow = new ArgumentMatcher<String[]>() {
      @Override
      public boolean matches(String[] command) {
        return Arrays.stream(command).anyMatch(word -> word.contains("show"));
      }

      @Override
      public Class type()
      {
        return String[].class;
      }

    };
    operationsMockedStatic.when(() -> Operations.runProcessGetOutput(any(Path.class),argThat(matchCommandPipFreeze))).thenReturn(PIP_FREEZE_LINES_CYCLIC);
//    operationsMockedStatic.when(() -> Operations.runProcessGetOutput(any(Path.class),any(String[].class))).thenReturn(PIP_FREEZE_LINES_CYCLIC);
    operationsMockedStatic.when(() -> Operations.runProcessGetOutput(any(Path.class),argThat(matchCommandPipShow))).thenReturn(PIP_SHOW_LINES_CYCLIC);
    String requirementsTxt = getFileFromResource("requirements.txt", "msc", "python", "requirements-cyclic-test.txt");
    System.setProperty("MATCH_MANIFEST_VERSIONS","false");
    List<Map<String, Object>> dependencies = pythonControllerRealEnv.getDependencies(requirementsTxt, true);
    assertEquals(104,dependencies.size());

    operationsMockedStatic.close();

  }

  @Test
  void get_Dependency_Name_requirements() {

    assertEquals("something",PythonControllerRealEnv.getDependencyName("something==2.0.5"));
    assertEquals("something",PythonControllerRealEnv.getDependencyName("something == 2.0.5"));
    assertEquals("something",PythonControllerRealEnv.getDependencyName("something>=2.0.5"));

  }



  @Test
  void automaticallyInstallPackageOnEnvironment() {
    assertFalse(this.pythonControllerRealEnv.automaticallyInstallPackageOnEnvironment());
  }

  @Test
  void isRealEnv() {

    assertTrue(this.pythonControllerRealEnv.isRealEnv());
  }

  @Test
  void isVirtualEnv() {
    assertFalse(this.pythonControllerRealEnv.isVirtualEnv());
  }

}
