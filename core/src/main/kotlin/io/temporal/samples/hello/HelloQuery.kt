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

package io.temporal.samples.hello

import io.temporal.kotlin.client.KWorkflowClient
import io.temporal.kotlin.client.KWorkflowOptions
import io.temporal.kotlin.worker.KWorkerFactory
import io.temporal.kotlin.workflow.KWorkflow
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.workflow.QueryMethod
import io.temporal.workflow.Workflow
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Sample Temporal Workflow Definition that demonstrates how to Query a Workflow.
 *
 * This is the Kotlin equivalent of the Java HelloQuery sample, demonstrating:
 * - KWorkflowClient for type-safe workflow execution and querying
 * - KWorkerFactory for automatic Kotlin coroutine support
 * - KWorkflowHandle for querying running workflows
 * - Workflow.sleep for workflow-safe time operations
 */
object HelloQuery {

    // Define the task queue name
    const val TASK_QUEUE = "HelloQueryTaskQueue"

    // Define our workflow unique id
    const val WORKFLOW_ID = "HelloQueryWorkflow"

    /**
     * The Workflow Definition's Interface.
     */
    @WorkflowInterface
    interface GreetingWorkflow {

        @WorkflowMethod
        suspend fun createGreeting(name: String)

        /**
         * Workflow query method. Used to return our greeting as a query value.
         * Note: Query methods should NOT be suspend functions.
         */
        @QueryMethod
        fun queryGreeting(): String
    }

    /**
     * Define the workflow implementation.
     */
    class GreetingWorkflowImpl : GreetingWorkflow {

        private var greeting: String = ""

        override suspend fun createGreeting(name: String) {
            // We set the value of greeting to "Hello" first
            greeting = "Hello $name!"

            // Sleep for 2 seconds using Workflow.sleep
            // Note: Inside a workflow method you should always use Workflow.sleep
            // rather than standard Kotlin delay to ensure determinism
            Workflow.sleep(Duration.ofSeconds(2))

            // After two seconds we change the value of our greeting to "Bye"
            greeting = "Bye $name!"
        }

        override fun queryGreeting(): String {
            return greeting
        }
    }

    /**
     * With our Workflow defined, we can now start execution.
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
            GreetingWorkflow::createGreeting,
            options,
            "World"
        )

        // Query our workflow to get the current value of greeting using type-safe API
        // We should get "Hello World!"
        val firstGreeting = handle.query(GreetingWorkflow::queryGreeting)
        println(firstGreeting)

        // Sleep for 2.5 seconds using Kotlin coroutines delay (outside workflow context)
        delay(2500.milliseconds)

        // Query our workflow again - now we should get "Bye World!"
        val secondGreeting = handle.query(GreetingWorkflow::queryGreeting)
        println(secondGreeting)
        System.exit(0)
    }
}
