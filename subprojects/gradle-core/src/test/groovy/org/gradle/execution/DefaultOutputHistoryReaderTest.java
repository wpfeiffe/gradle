/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.execution;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.util.GFileUtils;
import org.gradle.util.TemporaryFolder;
import static org.hamcrest.Matchers.*;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;

import java.io.File;

/**
 * @author Hans Dockter
 */
public class DefaultOutputHistoryReaderTest {
    private JUnit4Mockery context = new JUnit4Mockery();
    private Task taskStub = context.mock(Task.class);
    
    private DefaultOutputHistoryReader outputHistoryReader = new DefaultOutputHistoryReader();
    private Project projectStub = context.mock(Project.class);
    private static final String TASK_PATH = ":someProjectPath:someTaskName";
    private static final String CONVERTED_TASK_PATH = TASK_PATH.replace(":", "/");
    private static final String HISTORY_FILE_PATH = OutputHistoryWriter.HISTORY_DIR_NAME + "/" + CONVERTED_TASK_PATH;
    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();
    private File historyDir = testDir.getDir();

    @Before
    public void setUp() {
        context.checking(new Expectations() {{
            allowing(taskStub).getPath();
            will(returnValue(TASK_PATH));
            allowing(taskStub).getProject();
            will(returnValue(projectStub));
            allowing(projectStub).getBuildDir();
            will(returnValue(historyDir));
        }});
    }

    @org.junit.Test
    public void testReadHistoryWithExistingHistoryFile() {
        File historyFile = new File(historyDir, HISTORY_FILE_PATH);
        long timestamp = System.currentTimeMillis();
        GFileUtils.writeStringToFile(historyFile, "" + timestamp);
        OutputHistory outputHistory = outputHistoryReader.readHistory(taskStub);
        assertThat(outputHistory.wasCreatedSuccessfully(), equalTo(true));
        assertThat(outputHistory.getLastModified(), equalTo(timestamp));
    }

    @org.junit.Test
    public void testReadHistoryWithNonExistingHistoryFile() {
        OutputHistory outputHistory = outputHistoryReader.readHistory(taskStub);
        assertThat(outputHistory.wasCreatedSuccessfully(), equalTo(false));
        assertThat(outputHistory.getLastModified(), equalTo(null));
    }
}
