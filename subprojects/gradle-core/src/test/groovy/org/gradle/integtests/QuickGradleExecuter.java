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
package org.gradle.integtests;

import org.gradle.StartParameter;
import org.gradle.api.logging.LogLevel;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

public class QuickGradleExecuter extends AbstractGradleExecuter {
    private final GradleDistribution dist;
    private File directory;
    private String[] tasks = new String[0];
    private String[] args = new String[0];
    private boolean quiet;
    private StartParameterModifier inProcessStartParameterModifier;
    private Map<String, String> environmentVars = new HashMap<String, String>();
    private String script = null;

    public QuickGradleExecuter(GradleDistribution dist) {
        this.dist = dist;
        directory = dist.getTestDir();
    }

    @Override
    public GradleExecuter inDirectory(File directory) {
        this.directory = directory;
        return this;
    }

    @Override
    public GradleExecuter withArguments(String... args) {
        this.args = args;
        return this;
    }

    @Override
    public GradleExecuter withTasks(String... names) {
        this.tasks = names;
        return this;
    }

    @Override
    public GradleExecuter withQuietLogging() {
        quiet = true;
        return this;
    }

    @Override
    public GradleExecuter usingExecutable(String script) {
        this.script = script;
        return this;
    }

    public ExecutionResult run() {
        return configureExecuter().run();
    }

    public ExecutionFailure runWithFailure() {
        return configureExecuter().runWithFailure();
    }

    @Override
    public GradleExecuter withEnvironmentVars(Map<String, ?> environment) {
        environmentVars.clear();
        for (Map.Entry<String, ?> entry : environment.entrySet()) {
            environmentVars.put(entry.getKey(), entry.getValue().toString());
        }
        return this;
    }

    public void setInProcessStartParameterModifier(StartParameterModifier inProcessStartParameterModifier) {
        this.inProcessStartParameterModifier = inProcessStartParameterModifier;
    }

    private GradleExecuter configureExecuter() {
        StartParameter parameter = new StartParameter();
        parameter.setLogLevel(LogLevel.INFO);
        parameter.setGradleHomeDir(dist.getGradleHomeDir());
        parameter.setGradleUserHomeDir(dist.getUserHomeDir());

        InProcessGradleExecuter inProcessGradleExecuter = new InProcessGradleExecuter(parameter);
        inProcessGradleExecuter.inDirectory(directory);

        if (args.length > 0) {
            inProcessGradleExecuter.withArguments(args);
        }

        GradleExecuter returnedExecuter = inProcessGradleExecuter; 

        if (inProcessGradleExecuter.getParameter().isShowVersion() || !environmentVars.isEmpty() || script != null) {
            ForkingGradleExecuter forkingGradleExecuter = new ForkingGradleExecuter(dist);
            forkingGradleExecuter.inDirectory(directory);
            if (tasks.length > 0) {
                forkingGradleExecuter.withTasks(tasks);
            }
            if (args.length > 0) {
                forkingGradleExecuter.withArguments(args);
            }
            forkingGradleExecuter.withEnvironmentVars(environmentVars);
            if (quiet) {
                forkingGradleExecuter.withArguments("-q");
            }
            forkingGradleExecuter.usingExecutable(script);
            returnedExecuter = forkingGradleExecuter;
        } else {
            if (inProcessStartParameterModifier != null) {
                inProcessStartParameterModifier.modify(inProcessGradleExecuter.getParameter());
            }
        }

        if (tasks.length > 0) {
            returnedExecuter.withTasks(tasks);
        }
        if (quiet) {
            returnedExecuter.withArguments("-q");
        }

        return returnedExecuter;
    }

    public static interface StartParameterModifier {
        void modify(StartParameter startParameter);
    }
}
