/*
 * Copyright 2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.initialization

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.JavaPlugin
import org.gradle.util.JUnit4GroovyMockery
import org.gradle.util.TemporaryFolder
import org.jmock.lib.legacy.ClassImposteriser
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.gradle.*
import static org.junit.Assert.*

/**
 * @author Hans Dockter
 */
@RunWith (org.jmock.integration.junit4.JMock)
class BuildSourceBuilderTest {
    BuildSourceBuilder buildSourceBuilder
    GradleLauncherFactory gradleFactoryMock
    GradleLauncher gradleMock
    Project rootProjectMock
    Configuration configurationMock
    ConfigurationContainer configurationContainerStub
    CacheInvalidationStrategy cacheInvalidationStrategyMock
    File rootDir
    File testBuildSrcDir
    Set testDependencies
    StartParameter expectedStartParameter
    JUnit4GroovyMockery context = new JUnit4GroovyMockery()
    String expectedArtifactPath
    BuildResult expectedBuildResult
    @Rule public TemporaryFolder tmpDir = new TemporaryFolder();

    @Before public void setUp() {
        context.setImposteriser(ClassImposteriser.INSTANCE)
        File testDir = tmpDir.dir
        (rootDir = new File(testDir, 'root')).mkdir()
        (testBuildSrcDir = new File(rootDir, 'buildSrc')).mkdir()
        gradleFactoryMock = context.mock(GradleLauncherFactory)
        gradleMock = context.mock(GradleLauncher)
        rootProjectMock = context.mock(Project)
        configurationContainerStub = context.mock(ConfigurationContainer)
        configurationMock = context.mock(Configuration)
        cacheInvalidationStrategyMock = context.mock(CacheInvalidationStrategy)
        buildSourceBuilder = new BuildSourceBuilder(gradleFactoryMock, cacheInvalidationStrategyMock)
        expectedStartParameter = new StartParameter(
                searchUpwards: false,
                currentDir: testBuildSrcDir,
                taskNames: ['clean', 'build'],
                gradleUserHomeDir: new File('gradleUserHome'),
                projectProperties: dependencyProjectProps
        )
        testDependencies = ['dep1' as File, 'dep2' as File]
        expectedArtifactPath = "$testBuildSrcDir.absolutePath/build/libs/${BuildSourceBuilder.BUILD_SRC_MODULE}-${BuildSourceBuilder.BUILD_SRC_REVISION}.jar"
        Gradle build = context.mock(Gradle)
        context.checking {
            allowing(rootProjectMock).getConfigurations(); will(returnValue(configurationContainerStub))
            allowing(configurationContainerStub).getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME); will(returnValue(configurationMock))
            allowing(build).getRootProject(); will(returnValue(rootProjectMock))
        }
        expectedBuildResult = new BuildResult(build, null)
    }

    @Test public void testBuildSourceBuilder() {
        assert buildSourceBuilder.gradleLauncherFactory.is(gradleFactoryMock)
    }

    @Test public void testBuildArtifactFile() {
        assertEquals(new File(expectedArtifactPath), buildSourceBuilder.buildArtifactFile(testBuildSrcDir))
    }

    @Test public void testCreateDependencyWithExistingBuildSources() {
        StartParameter modifiedStartParameter = expectedStartParameter.newInstance()
        context.checking {
            allowing(cacheInvalidationStrategyMock).isValid(expectedArtifactPath as File, testBuildSrcDir); will(returnValue(false))
            one(gradleFactoryMock).newInstance(modifiedStartParameter); will(returnValue(gradleMock))
            one(gradleMock).run(); will(returnValue(expectedBuildResult))
            one(configurationMock).getFiles(); will(returnValue(testDependencies))
        }
        createArtifact()
        createBuildFile()
        Set<File> actualClasspath = buildSourceBuilder.createBuildSourceClasspath(expectedStartParameter)
        assertEquals(new LinkedHashSet([expectedArtifactPath as File] + testDependencies), actualClasspath)
    }

    @Test public void testCreateDependencyWithCachedArtifactAndValidCache() {
        expectedStartParameter.setCacheUsage(CacheUsage.ON)
        StartParameter modifiedStartParameter = expectedStartParameter.newInstance()
        context.checking {
            allowing(cacheInvalidationStrategyMock).isValid(expectedArtifactPath as File, testBuildSrcDir); will(returnValue(true))
            one(gradleFactoryMock).newInstance(modifiedStartParameter); will(returnValue(gradleMock))
            one(gradleMock).getBuildAnalysis(); will(returnValue(expectedBuildResult))
            one(configurationMock).getFiles(); will(returnValue(testDependencies))
        }
        createArtifact()
        createBuildFile()
        Set actualClasspath = buildSourceBuilder.createBuildSourceClasspath(expectedStartParameter)
        assertEquals(new LinkedHashSet([expectedArtifactPath as File] + testDependencies), actualClasspath)
    }

    @Test public void testCreateDependencyWithCachedArtifactAndValidCacheWithCacheOff() {
        expectedStartParameter.setCacheUsage(CacheUsage.OFF)
        StartParameter modifiedStartParameter = expectedStartParameter.newInstance()
        context.checking {
            allowing(cacheInvalidationStrategyMock).isValid(expectedArtifactPath as File, testBuildSrcDir); will(returnValue(true))
            one(gradleFactoryMock).newInstance(modifiedStartParameter); will(returnValue(gradleMock))
            one(gradleMock).run(); will(returnValue(expectedBuildResult))
            one(configurationMock).getFiles(); will(returnValue(testDependencies))
        }
        createArtifact()
        createBuildFile()
        Set actualClasspath = buildSourceBuilder.createBuildSourceClasspath(expectedStartParameter)
        assertEquals(new LinkedHashSet([expectedArtifactPath as File] + testDependencies), actualClasspath)
    }

    @Test public void testCreateDependencyWithNonExistingBuildScript() {
        StartParameter modifiedStartParameter = this.expectedStartParameter.newInstance()
        modifiedStartParameter.useEmbeddedBuildFile(BuildSourceBuilder.getDefaultScript())
        context.checking {
            allowing(cacheInvalidationStrategyMock).isValid(expectedArtifactPath as File, testBuildSrcDir); will(returnValue(false))
            one(gradleFactoryMock).newInstance(modifiedStartParameter); will(returnValue(gradleMock))
            one(gradleMock).run(); will(returnValue(expectedBuildResult))
            one(configurationMock).getFiles(); will(returnValue(testDependencies))
        }
        createArtifact()
        Set actualClasspath = buildSourceBuilder.createBuildSourceClasspath(expectedStartParameter)
        assertEquals(new LinkedHashSet([expectedArtifactPath as File] + testDependencies), actualClasspath)
    }

    @Test public void testCreateDependencyWithNonExistingBuildSrcDir() {
        expectedStartParameter = expectedStartParameter.newInstance()
        expectedStartParameter.setCurrentDir(new File('nonexisting'));
        assertEquals([] as Set, buildSourceBuilder.createBuildSourceClasspath(expectedStartParameter))
    }

    @Test public void testCreateDependencyWithNoArtifactProducingBuild() {
        StartParameter modifiedStartParameter = this.expectedStartParameter.newInstance()
        context.checking {
            allowing(cacheInvalidationStrategyMock).isValid(expectedArtifactPath as File, testBuildSrcDir); will(returnValue(false))
            one(gradleFactoryMock).newInstance(modifiedStartParameter); will(returnValue(gradleMock))
            one(gradleMock).run()
        }
        createBuildFile()
        assertEquals([] as Set, buildSourceBuilder.createBuildSourceClasspath(expectedStartParameter))
    }

    private createBuildFile() {
        new File(testBuildSrcDir, Project.DEFAULT_BUILD_FILE).createNewFile()
    }

    private createArtifact() {
        buildSourceBuilder.buildArtifactFile(testBuildSrcDir).parentFile.mkdirs()
        buildSourceBuilder.buildArtifactFile(testBuildSrcDir).createNewFile()
    }

    private Map getDependencyProjectProps() {
        [group: BuildSourceBuilder.BUILD_SRC_ORG,
                version: BuildSourceBuilder.BUILD_SRC_REVISION,
                type: 'jar']
    }
}
