package org.gradle.api.internal.project

import groovy.xml.MarkupBuilder
import org.junit.Test
import org.gradle.api.Project
import org.gradle.util.HelperUtil
import static org.junit.Assert.*
import static org.hamcrest.Matchers.*
import static org.gradle.util.Matchers.*
import org.gradle.api.tasks.ant.AntTarget
import java.lang.reflect.Field
import org.apache.tools.ant.Target

class DefaultAntBuilderTest {
    private final Project project = HelperUtil.createRootProject()
    private final def ant = new DefaultAntBuilder(project)

    @Test
    public void antPropertiesAreAvailableAsPropertiesOfBuilder() {
        ant.property(name: 'prop1', value: 'value1')
        assertThat(ant.prop1, equalTo('value1'))

        ant.prop2 = 'value2'
        assertThat(ant.antProject.properties.prop2, equalTo('value2'))
    }

    @Test
    public void throwsMissingPropertyExceptionForUnknownProperty() {
        try {
            ant.unknown
            fail()
        } catch (MissingPropertyException e) {
            // expected
        }
    }

    @Test
    public void antPropertiesAreAvailableAsMap() {
        ant.property(name: 'prop1', value: 'value1')
        assertThat(ant.properties.prop1, equalTo('value1'))

        ant.properties.prop2 = 'value2'
        assertThat(ant.antProject.properties.prop2, equalTo('value2'))
    }

    @Test
    public void antReferencesAreAvailableAsMap() {
        def path = ant.path(id: 'ref1', location: 'path')
        assertThat(ant.references.ref1, sameInstance(path))

        ant.references.prop2 = 'value2'
        assertThat(ant.antProject.references.prop2, equalTo('value2'))
    }

    @Test
    public void importAddsTaskForEachAntTarget() {
        File buildFile = new File(project.projectDir, 'build.xml')
        buildFile.withWriter {Writer writer ->
            def xml = new MarkupBuilder(writer)
            xml.project {
                target(name: 'target1', depends: 'target2, target3')
                target(name: 'target2')
                target(name: 'target3')
            }
        }

        ant.importBuild(buildFile)

        def task = project.tasks.target1
        assertThat(task, instanceOf(AntTarget))
        assertThat(task.target.name, equalTo('target1'))

        task = project.tasks.target2
        assertThat(task, instanceOf(AntTarget))
        assertThat(task.target.name, equalTo('target2'))

        task = project.tasks.target3
        assertThat(task, instanceOf(AntTarget))
        assertThat(task.target.name, equalTo('target3'))
    }

    @Test
    public void canNestElements() {
        ant.otherProp = 'true'
        ant.condition(property: 'prop', value: 'someValue') {
            or {
                and {
                    isSet(property: 'otherProp')
                    not { isSet(property: 'missing') }
                }
            }
        }
        assertThat(ant.prop, equalTo('someValue'))
    }
    
    @Test
    public void discardsTasksAfterExecution() {
        ant.echo(message: 'message')
        ant.echo(message: 'message')
        ant.echo(message: 'message')

        assertThat(ant.antProject.targets.size(), equalTo(0))

        Field field = AntBuilder.class.getDeclaredField('collectorTarget')
        field.accessible = true
        Target target = field.get(ant)
        field = target.class.getDeclaredField('children')
        field.accessible = true
        List children = field.get(target)
        assertThat(children, isEmpty())
    }
}