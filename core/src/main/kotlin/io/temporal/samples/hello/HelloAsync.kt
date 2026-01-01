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

import io.temporal.activity.ActivityInterface
import io.temporal.kotlin.activity.KActivityOptions
import io.temporal.kotlin.client.KWorkflowClient
import io.temporal.kotlin.client.KWorkflowOptions
import io.temporal.kotlin.worker.KWorkerFactory
import io.temporal.worker.registerWorkflowImplementationType
import io.temporal.kotlin.workflow.KWorkflow
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

/**
 * Sample Temporal Workflow Definition that demonstrates asynchronous Activity Execution.
 *
 * This is the Kotlin equivalent of the Java HelloAsync sample, demonstrating:
 * - KWorkflowClient for type-safe workflow execution
 * - KWorkerFactory for automatic Kotlin coroutine support
 * - KWorkflow.async for parallel activity execution
 */
object HelloAsync {

    // Define the task queue name
    const val TASK_QUEUE = "HelloAsyncActivityTaskQueue"

    // Define our workflow unique id
    const val WORKFLOW_ID = "HelloAsyncActivityWorkflow"

    /**
     * The Workflow Definition's Interface.
     */
    @WorkflowInterface
    interface GreetingWorkflow {

        @WorkflowMethod
        suspend fun getGreeting(name: String): String
    }

    /**
     * Activity Definition Interface.
     */
    @ActivityInterface
    interface GreetingActivities {

        fun composeGreeting(greeting: String, name: String): String
    }

    /**
     * Define the workflow implementation.
     */
    class GreetingWorkflowImpl : GreetingWorkflow {

        override suspend fun getGreeting(name: String): String {
            // Execute both activities in parallel using KWorkflow.async
            // This is the Kotlin coroutine equivalent of Async.function in Java
            val hello = KWorkflow.async {
                KWorkflow.executeActivity(
                    GreetingActivities::composeGreeting,
                    KActivityOptions(startToCloseTimeout = 10.seconds),
                    "Hello",
                    name
                )
            }
            val bye = KWorkflow.async {
                KWorkflow.executeActivity(
                    GreetingActivities::composeGreeting,
                    KActivityOptions(startToCloseTimeout = 10.seconds),
                    "Bye",
                    name
                )
            }

            // Wait for both activities to complete and combine results
            return "${hello.await()}\n${bye.await()}"
        }
    }

    /**
     * Simple activity implementation.
     */
    class GreetingActivitiesImpl : GreetingActivities {

        override fun composeGreeting(greeting: String, name: String): String {
            return "$greeting $name!"
        }
    }

    /**
     * Main method to start the workflow.
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
        worker.registerWorkflowImplementationType<GreetingWorkflowImpl>()

        // Register activities
        worker.registerActivitiesImplementations(GreetingActivitiesImpl())

        // Start all workers
        factory.start()

        // Define workflow options
        val options = KWorkflowOptions(
            workflowId = WORKFLOW_ID,
            taskQueue = TASK_QUEUE
        )

        // Execute our workflow using the type-safe Kotlin API
        val greeting = client.executeWorkflow(
            GreetingWorkflow::getGreeting,
            options,
            "World"
        )

        // Display workflow execution results
        println(greeting)
        System.exit(0)
    }
}
