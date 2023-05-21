package com.redhat.crda.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.redhat.crda.providers.JavaMavenProvider;
import org.junit.jupiter.api.Test;
import java.nio.file.Paths;

class Ecosystem_Test {

  @Test
  void get_a_manifest_for_an_unknown_package_file_should_throw_an_exception() {
    var manifestPath = Paths.get("/not/a/supported/mani.fest");
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> Ecosystem.getManifest(manifestPath));
  }

  @Test
  void get_a_manifest_for_a_pom_xml_file_should_return_java_maven_manifest() {
    var manifestPath = Paths.get("/supported/manifest/pom.xml");
    var manifest = Ecosystem.getManifest(manifestPath);

    assertThat(manifest.manifestPath()).isEqualTo(manifestPath);
    assertThat(manifest.packageManager()).isEqualTo(Ecosystem.PackageManager.MAVEN);
    assertThat(manifest.provider()).isInstanceOf(JavaMavenProvider.class);
  }

}
