/*
 * Copyright 2007-2008 the original author or authors.
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

package org.gradle.api;

import org.gradle.util.ConfigureUtil
import org.gradle.api.internal.AbstractTask
import org.gradle.api.internal.NoConventionMapping;

/**
 * @author Hans Dockter
 */
@NoConventionMapping
class DefaultTask extends AbstractTask {
    DefaultTask() {
        super();
        setMetaClass(GroovySystem.getMetaClassRegistry().getMetaClass(getClass()))
    }

    Task doFirst(Closure action) {
        if (action == null) {
            throw new InvalidUserDataException("Action must not be null!");
        }
        doFirst(convertClosureToAction(action));
        return this;
    }

    Task doLast(Closure action) {
        if (action == null) {
            throw new InvalidUserDataException("Action must not be null!");
        }
        doLast(convertClosureToAction(action));
        return this;
    }

    public Task leftShift(Closure action) {
        return doLast(action)
    }

    Task configure(Closure closure) {
        return (Task) ConfigureUtil.configure(closure, this);
    }

    private TaskAction convertClosureToAction(Closure actionClosure) {
        actionClosure.setDelegate(getProject());
        actionClosure.setResolveStrategy(Closure.OWNER_FIRST);
        return new ClosureTaskAction(actionClosure);
    }
}

class ClosureTaskAction implements TaskAction {
    private final Closure closure;

    def ClosureTaskAction(Closure closure) {
        this.closure = closure;
    }

    public void execute(Task task) {
        if (closure.maximumNumberOfParameters == 0) {
            closure.call()
        }
        else {
            closure.call(task);
        }
    }

}
