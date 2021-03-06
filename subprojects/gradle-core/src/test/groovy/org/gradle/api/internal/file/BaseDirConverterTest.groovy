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

package org.gradle.api.internal.file

import org.gradle.api.InvalidUserDataException
import org.gradle.api.PathValidation
import org.gradle.api.file.FileCollection
import org.gradle.util.TemporaryFolder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import static org.hamcrest.Matchers.*
import static org.junit.Assert.*
import java.util.concurrent.Callable

/**
 * @author Hans Dockter
 */
class BaseDirConverterTest {
    static final String TEST_PATH = 'testpath'

    File baseDir
    File testFile
    File testDir

    BaseDirConverter baseDirConverter
    @Rule public TemporaryFolder rootDir = new TemporaryFolder();

    @Before public void setUp() {
        baseDir = rootDir.dir
        baseDirConverter = new BaseDirConverter(baseDir)
        testFile = new File(baseDir, 'testfile')
        testDir = new File(baseDir, 'testdir')
    }

    @Test (expected = IllegalArgumentException) public void testWithNullPath() {
        baseDirConverter.resolve(null)
    }

    @Test public void testWithNoPathValidation() {
        // No exceptions means test has passed
        baseDirConverter.resolve(TEST_PATH)
        baseDirConverter.resolve(TEST_PATH, PathValidation.NONE)
    }

    @Test public void testPathValidationWithNonExistingFile() {
        try {
            baseDirConverter.resolve(testFile.name, PathValidation.FILE)
            fail()
        } catch (InvalidUserDataException e) {
            assertThat(e.message, equalTo("File '$testFile.canonicalFile' does not exist.".toString()))
        }
    }

    @Test public void testPathValidationForFileWithDirectory() {
        testDir.mkdir()
        try {
            baseDirConverter.resolve(testDir.name, PathValidation.FILE)
            fail()
        } catch (InvalidUserDataException e) {
            assertThat(e.message, equalTo("File '$testDir.canonicalFile' is not a file.".toString()))
        }
    }

    @Test public void testWithValidFile() {
        testFile.createNewFile()
        baseDirConverter.resolve(testFile.name, PathValidation.FILE)
    }

    @Test public void testPathValidationWithNonExistingDirectory() {
        try {
            baseDirConverter.resolve(testDir.name, PathValidation.DIRECTORY)
            fail()
        } catch (InvalidUserDataException e) {
            assertThat(e.message, equalTo("Directory '$testDir.canonicalFile' does not exist.".toString()))
        }
    }

    @Test public void testPathValidationWithValidDirectory() {
        testDir.mkdir()
        baseDirConverter.resolve(testDir.name, PathValidation.DIRECTORY)
    }

    @Test public void testPathValidationForDirectoryWithFile() {
        testFile.createNewFile()
        try {
            baseDirConverter.resolve(testFile.name, PathValidation.DIRECTORY)
            fail()
        } catch (InvalidUserDataException e) {
            assertThat(e.message, equalTo("Directory '$testFile.canonicalFile' is not a directory.".toString()))
        }
    }

    @Test public void testPathValidationForExistingDirAndFile() {
        testDir.mkdir()
        testFile.createNewFile()
        baseDirConverter.resolve(testDir.name, PathValidation.EXISTS)
        baseDirConverter.resolve(testFile.name, PathValidation.EXISTS)
    }

    @Test public void testExistsPathValidationWithNonExistingDir() {
        try {
            baseDirConverter.resolve(testDir.name, PathValidation.EXISTS)
            fail()
        } catch (InvalidUserDataException e) {
            assertThat(e.message, equalTo("File '$testDir.canonicalFile' does not exist.".toString()))
        }
    }

    @Test public void testExistsPathValidationWithNonExistingFile() {
        try {
            baseDirConverter.resolve(testFile.name, PathValidation.EXISTS)
            fail()
        } catch (InvalidUserDataException e) {
            assertThat(e.message, equalTo("File '$testFile.canonicalFile' does not exist.".toString()))
        }
    }

    @Test public void testResolveAbsolutePath() {
        File absoluteFile = new File('nonRelative').absoluteFile
        assertEquals(absoluteFile, baseDirConverter.resolve(absoluteFile.path))
    }

    @Test public void testResolveRelativePath() {
        String relativeFileName = "relative"
        assertEquals(new File(baseDir, relativeFileName), baseDirConverter.resolve(relativeFileName))
    }

    @Test public void testResolveAbsoluteFile() {
        File absoluteFile = new File('nonRelative').absoluteFile
        assertEquals(absoluteFile, baseDirConverter.resolve(absoluteFile))
    }

    @Test public void testResolveRelativeFile() {
        File relativeFile = new File('relative')
        assertEquals(new File(baseDir, 'relative'), baseDirConverter.resolve(relativeFile))
    }

    @Test public void testResolveClosure() {
        assertEquals(new File(baseDir, 'relative'), baseDirConverter.resolve({'relative'}))
    }

    @Test public void testResolveCallable() {
        assertEquals(new File(baseDir, 'relative'), baseDirConverter.resolve({'relative'} as Callable))
    }

    @Test public void testResolveNestedClosuresAndCallables() {
        Callable callable = {'relative'} as Callable
        Closure closure = {callable}
        assertEquals(new File(baseDir, 'relative'), baseDirConverter.resolve(closure))
    }
    
    @Test public void testFiles() {
        FileCollection collection = baseDirConverter.resolveFiles('a', 'b')
        assertThat(collection, instanceOf(PathResolvingFileCollection))
        assertThat(collection.sources, equalTo(['a', 'b']))
    }

    @Test public void testFilesReturnsSourceFileCollection() {
        FileCollection source = baseDirConverter.resolveFiles('a')
        FileCollection collection = baseDirConverter.resolveFiles(source)
        assertThat(collection, sameInstance(source))
    }
}
