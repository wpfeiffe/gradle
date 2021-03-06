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
package org.gradle.api.testing.execution.control.server.messagehandlers;

import org.gradle.api.testing.execution.Pipeline;
import org.gradle.api.testing.execution.PipelineDispatcher;
import org.gradle.api.testing.execution.control.messages.TestControlMessageHandler;

/**
 * @author Tom Eyckmans
 */
public abstract class AbstractTestServerControlMessageHandler implements TestControlMessageHandler {
    protected final PipelineDispatcher pipelineDispatcher;
    protected final Pipeline pipeline;

    protected AbstractTestServerControlMessageHandler(PipelineDispatcher pipelineDispatcher) {
        if (pipelineDispatcher == null) throw new IllegalArgumentException("pipelineDispatcher == null!");

        this.pipelineDispatcher = pipelineDispatcher;
        this.pipeline = pipelineDispatcher.getPipeline();
    }
}
