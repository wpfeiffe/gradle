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
package org.gradle.api.testing.execution.control.client;

import org.gradle.api.testing.execution.control.client.transport.ExternalIoConnectorFactory;
import org.gradle.api.testing.execution.control.client.transport.IoConnectorFactory;
import org.gradle.api.testing.execution.control.messages.TestControlMessage;
import org.gradle.api.testing.execution.fork.ForkExecuter;
import org.gradle.util.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Tom Eyckmans
 */
public class TestForkExecuter implements ForkExecuter {
    private static final Logger logger = LoggerFactory.getLogger(TestForkExecuter.class);

    private ClassLoader sharedClassLoader;
    private ClassLoader controlClassLoader;
    private ClassLoader sandboxClassLoader;
    private List<String> arguments;

    private final BlockingQueue<TestControlMessage> testControlMessageQueue;

    public TestForkExecuter() {
        testControlMessageQueue = new ArrayBlockingQueue<TestControlMessage>(10);
    }

    public void execute() {
        try {
            final int pipelineId = Integer.parseInt(arguments.get(0));
            final int forkId = Integer.parseInt(arguments.get(1));
            final int testServerPort = Integer.parseInt(arguments.get(2));

            final IoConnectorFactory ioConnectorFactory = new ExternalIoConnectorFactory(testServerPort);
            final DefaultTestControlClient testControlClient = new DefaultTestControlClient(forkId, ioConnectorFactory, testControlMessageQueue);

            try {
                testControlClient.open();
                testControlClient.reportStarted();

                final TestControlMessageDispatcher testControlMessageDispatcher = new TestControlMessageDispatcher(testControlClient, sandboxClassLoader);
                final TestControlMessageQueueConsumer controlMessageConsumer = new TestControlMessageQueueConsumer(testControlMessageQueue, 100L, TimeUnit.MILLISECONDS, testControlMessageDispatcher);

                Thread controlMessageConsumerThread = new Thread(controlMessageConsumer);
                controlMessageConsumerThread.start();

                ThreadUtils.join(controlMessageConsumerThread);

                testControlClient.reportStopped();
            }
            finally {
                testControlClient.close();
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void setSharedClassLoader(ClassLoader sharedClassLoader) {
        this.sharedClassLoader = sharedClassLoader;
    }

    public void setControlClassLoader(ClassLoader controlClassLoader) {
        this.controlClassLoader = controlClassLoader;
    }

    public void setSandboxClassLoader(ClassLoader sandboxClassLoader) {
        this.sandboxClassLoader = sandboxClassLoader;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }
}
