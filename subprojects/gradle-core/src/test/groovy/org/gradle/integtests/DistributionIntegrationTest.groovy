package org.gradle.integtests

import org.junit.runner.RunWith
import org.junit.Test
import org.gradle.util.GradleVersion
import org.apache.tools.ant.taskdefs.Chmod
import org.gradle.util.AntUtil
import org.apache.tools.ant.taskdefs.Expand

@RunWith(DistributionIntegrationTestRunner)
class DistributionIntegrationTest {
    // Injected by test runner
    private GradleDistribution dist;
    private GradleExecuter executer;
    private String version = new GradleVersion().version

    @Test
    public void binZipContents() {
        TestFile binZip = dist.distributionsDir.file("gradle-$version-bin.zip")
        binZip.assertIsFile()
        binZip.unzipTo(dist.testDir)
        TestFile contentsDir = dist.testDir.file("gradle-$version")

        checkMinimalContents(contentsDir)

        // Extra stuff
        contentsDir.file('src').assertDoesNotExist()
        contentsDir.file('samples').assertDoesNotExist()
        contentsDir.file('docs').assertDoesNotExist()
    }

    @Test
    public void allZipContents() {
        TestFile binZip = dist.distributionsDir.file("gradle-$version-all.zip")
        binZip.assertIsFile()
        binZip.unzipTo(dist.testDir)
        TestFile contentsDir = dist.testDir.file("gradle-$version")

        checkMinimalContents(contentsDir)

        // Source
        contentsDir.file('src/org/gradle/api/Project.java').assertIsFile()
        contentsDir.file('src/org/gradle/initialization/defaultBuildSourceScript.txt').assertIsFile()
        contentsDir.file('src/org/gradle/gradleplugin/userinterface/swing/standalone/BlockingApplication.java').assertIsFile()
        contentsDir.file('src/org/gradle/wrapper/Wrapper.java').assertIsFile()

        // Samples
        contentsDir.file('samples/java/quickstart/build.gradle').assertIsFile()

        // Docs
        contentsDir.file('docs/javadoc/index.html').assertIsFile()
        contentsDir.file('docs/javadoc/org/gradle/api/Project.html').assertIsFile()
        contentsDir.file('docs/groovydoc/index.html').assertIsFile()
        contentsDir.file('docs/groovydoc/org/gradle/api/Project.html').assertIsFile()
        contentsDir.file('docs/groovydoc/org/gradle/api/tasks/bundling/Zip.html').assertIsFile()
        contentsDir.file('docs/userguide/userguide.html').assertIsFile()
        contentsDir.file('docs/userguide/userguide_single.html').assertIsFile()
//        contentsDir.file('docs/userguide/userguide.pdf').assertIsFile()
    }

    private def checkMinimalContents(TestFile contentsDir) {
        // Scripts
        contentsDir.file('bin/gradle').assertIsFile()
        contentsDir.file('bin/gradle.bat').assertIsFile()

        // Top level files
        contentsDir.file('LICENSE').assertIsFile()

        // Libs
        contentsDir.file("lib/gradle-core-${version}.jar").assertIsFile()
        contentsDir.file("lib/gradle-ui-${version}.jar").assertIsFile()
        contentsDir.file("lib/gradle-wrapper-${version}.jar").assertIsFile()

        // Docs
        contentsDir.file('getting-started.html').assertIsFile()
    }

    @Test
    public void sourceZipContents() {
        TestFile srcZip = dist.distributionsDir.file("gradle-$version-src.zip")
        srcZip.assertIsFile()
        srcZip.unzipTo(dist.testDir)
        TestFile contentsDir = dist.testDir.file("gradle-$version")

        Chmod chmod = new Chmod()
        chmod.file = contentsDir.file('gradlew')
        chmod.perm = 'u+x'
        AntUtil.execute(chmod)

        // Build self using wrapper in source distribution
        executer.inDirectory(contentsDir).usingExecutable('gradlew').withTasks('binZip').run()

        File binZip = contentsDir.file('build/distributions').listFiles()[0]
        Expand unpack = new Expand()
        unpack.src = binZip
        unpack.dest = contentsDir.file('build/distributions/unzip')
        AntUtil.execute(unpack)
        TestFile unpackedRoot = new TestFile(contentsDir.file('build/distributions/unzip').listFiles()[0])

        // Make sure the build distribution does something useful
        unpackedRoot.file("bin/gradle").assertIsFile()
        // todo run something with the gradle build by the source dist
    }
}
