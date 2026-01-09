/*
 * Copyright (C) 2022 Temporal Technologies, Inc. All Rights Reserved.
 *
 * Copyright (C) 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this material except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.temporal.kotlin.samples

import io.temporal.kotlin.client.KWorkflowClient
import io.temporal.kotlin.client.KWorkflowOptions
import io.temporal.kotlin.worker.KWorkerFactory
import io.temporal.kotlin.workflow.KWorkflow
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import kotlinx.coroutines.runBlocking

/**
 * Sample Temporal workflow that demonstrates how to use workflow signal methods
 * to signal from external sources.
 *
 * This is the Kotlin equivalent of the Java HelloSignal sample, demonstrating:
 * - KWorkflowClient for type-safe workflow execution and signaling
 * - KWorkerFactory for automatic Kotlin coroutine support
 * - KWorkflowHandle for signaling running workflows
 * - KWorkflow.awaitCondition for workflow-safe blocking
 */
object HelloSignal {

    // Define the task queue name
    const val TASK_QUEUE = "HelloSignalTaskQueue"

    // Define the workflow unique id
    const val WORKFLOW_ID = "HelloSignalWorkflow"

    /**
     * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
     */
    @WorkflowInterface
    interface GreetingWorkflow {

        /**
         * This is the method that is executed when the Workflow Execution is started.
         */
        @WorkflowMethod
        suspend fun getGreetings(): List<String>

        /**
         * Define the workflow waitForName signal method.
         * Signal methods are called when the workflow receives a signal.
         */
        @SignalMethod
        fun waitForName(name: String)

        /**
         * Define the workflow exit signal method.
         */
        @SignalMethod
        fun exit()
    }

    /**
     * Define the workflow implementation which implements the getGreetings workflow method.
     */
    class GreetingWorkflowImpl : GreetingWorkflow {

        // messageQueue holds up to 10 messages (received from signals)
        private val messageQueue = mutableListOf<String>()
        private var exit = false

        override suspend fun getGreetings(): List<String> {
            val receivedMessages = mutableListOf<String>()

            while (true) {
                // Block current coroutine until the condition is evaluated to true
                // This is the Kotlin equivalent of Workflow.await() in Java
                KWorkflow.awaitCondition { messageQueue.isNotEmpty() || exit }

                if (messageQueue.isEmpty() && exit) {
                    // No messages in queue and exit signal was sent, return the received messages
                    return receivedMessages
                }

                val message = messageQueue.removeAt(0)
                receivedMessages.add(message)
            }
        }

        override fun waitForName(name: String) {
            messageQueue.add("Hello $name!")
        }

        override fun exit() {
            exit = true
        }
    }

    /**
     * With the Workflow defined, we can now start execution.
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // Get a Workflow service stub
        val service = WorkflowServiceStubs.newLocalServiceStubs()

        // Create a Kotlin workflow client
        val client = KWorkflowClient(service)

        // Create a Kotlin worker factory - automatically enables Kotlin coroutine support
        val factory = KWorkerFactory(client)

        // Create a worker for the task queue
        val worker = factory.newWorker(TASK_QUEUE)

        // Register the workflow implementation
        worker.registerWorkflowImplementationTypes<GreetingWorkflowImpl>()

        // Start all workers
        factory.start()

        // Define workflow options
        val options = KWorkflowOptions(
            workflowId = WORKFLOW_ID,
            taskQueue = TASK_QUEUE
        )

        // Start workflow and get a typed handle for interaction
        val handle = client.startWorkflow(
            GreetingWorkflow::getGreetings,
            options
        )

        // Send signals using the type-safe handle API
        handle.signal(GreetingWorkflow::waitForName, "World")
        handle.signal(GreetingWorkflow::waitForName, "Universe")

        // Send exit signal
        handle.signal(GreetingWorkflow::exit)

        // Wait for workflow result using the handle
        val greetings = handle.result()

        // Print our greetings
        println(greetings)
        System.exit(0)
    }
}
