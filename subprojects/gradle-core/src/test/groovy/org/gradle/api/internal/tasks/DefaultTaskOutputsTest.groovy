package org.gradle.api.internal.tasks

import org.junit.Test
import static org.junit.Assert.*
import static org.hamcrest.Matchers.*
import static org.gradle.util.Matchers.*
import org.gradle.api.internal.file.FileResolver;

class DefaultTaskOutputsTest {
    private final DefaultTaskOutputs outputs = new DefaultTaskOutputs({new File(it)} as FileResolver)

    @Test
    public void defaultValues() {
        assertThat(outputs.files.files, isEmpty())
        assertFalse(outputs.hasOutputFiles)
    }

    @Test
    public void canRegisterOutputFiles() {
        outputs.files('a')
        assertThat(outputs.files.files, equalTo([new File('a')] as Set))
    }

    @Test
    public void hasInputFilesWhenEmptyInputFilesRegistered() {
        outputs.files([])
        assertTrue(outputs.hasOutputFiles)
    }
    
    @Test
    public void hasInputFilesWhenNonEmptyInputFilesRegistered() {
        outputs.files('a')
        assertTrue(outputs.hasOutputFiles)
    }
}