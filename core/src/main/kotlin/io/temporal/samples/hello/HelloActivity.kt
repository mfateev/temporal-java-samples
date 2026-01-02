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
import io.temporal.activity.ActivityMethod
import io.temporal.kotlin.activity.KActivityOptions
import io.temporal.kotlin.client.KWorkflowClient
import io.temporal.kotlin.client.KWorkflowOptions
import io.temporal.kotlin.worker.KWorkerFactory
import io.temporal.kotlin.workflow.KWorkflow
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.registerWorkflowImplementationType
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

/**
 * Sample Temporal Workflow Definition that executes a single Activity.
 *
 * This is the Kotlin equivalent of the Java HelloActivity sample, demonstrating:
 * - KWorkflowClient for type-safe workflow execution
 * - KWorkerFactory for Kotlin coroutine support in workflows
 * - KWorkflow.executeActivity for activity invocation
 * - Suspend workflow methods for natural coroutine integration
 */
object HelloActivity {

    // Define the task queue name
    const val TASK_QUEUE = "HelloActivityTaskQueue"

    // Define our workflow unique id
    const val WORKFLOW_ID = "HelloActivityWorkflow"

    private val log = LoggerFactory.getLogger(HelloActivity::class.java)

    /**
     * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
     *
     * For Kotlin workflows using the Kotlin SDK, the workflow method should be a suspend function.
     * Workflow Definitions should not contain any heavyweight computations, non-deterministic
     * code, network calls, database operations, etc. Those things should be handled by Activities.
     */
    @WorkflowInterface
    interface GreetingWorkflow {

        /**
         * This is the method that is executed when the Workflow Execution is started.
         * The Workflow Execution completes when this method finishes execution.
         */
        @WorkflowMethod
        suspend fun getGreeting(name: String): String
    }

    /**
     * This is the Activity Definition's Interface. Activities are building blocks of any Temporal
     * Workflow and contain any business logic that could perform long running computation, network
     * calls, etc.
     *
     * Annotating Activity Definition methods with @ActivityMethod is optional.
     */
    @ActivityInterface
    interface GreetingActivities {

        // Define your activity method which can be called during workflow execution
        @ActivityMethod(name = "greet")
        fun composeGreeting(greeting: String, name: String): String
    }

    /**
     * Define the workflow implementation which implements our getGreeting workflow method.
     */
    class GreetingWorkflowImpl : GreetingWorkflow {

        override suspend fun getGreeting(name: String): String {
            // Execute the activity using type-safe method reference
            // In Kotlin SDK, this is a suspending call that returns when activity completes
            return KWorkflow.executeActivity(
                GreetingActivities::composeGreeting,
                KActivityOptions(
                    // The "startToCloseTimeout" option sets the overall timeout that our workflow
                    // is willing to wait for the activity to complete.
                    startToCloseTimeout = 2.seconds
                ),
                "Hello",
                name
            )
        }
    }

    /**
     * Simple activity implementation, that concatenates two strings.
     */
    class GreetingActivitiesImpl : GreetingActivities {

        override fun composeGreeting(greeting: String, name: String): String {
            log.info("Composing greeting...")
            return "$greeting $name!"
        }
    }

    /**
     * With our Workflow and Activities defined, we can now start execution.
     * The main method starts the worker and then the workflow.
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // Get a Workflow service stub.
        val service = WorkflowServiceStubs.newLocalServiceStubs()

        // Create a Kotlin workflow client - provides suspend functions and type-safe APIs
        val client = KWorkflowClient(service)

        // Create a Kotlin worker factory - enables Kotlin coroutine support for workflows
        val factory = KWorkerFactory(client)

        // Create a worker for the task queue
        val worker = factory.newWorker(TASK_QUEUE)

        // Register our Kotlin workflow implementation with the worker
        worker.registerWorkflowImplementationType<GreetingWorkflowImpl>()

        // Register our Activity Types with the Worker
        worker.registerActivitiesImplementations(GreetingActivitiesImpl())

        // Start all the workers
        factory.start()

        // Define workflow options
        val options = KWorkflowOptions(
            workflowId = WORKFLOW_ID,
            taskQueue = TASK_QUEUE
        )

        // Execute the workflow using the type-safe Kotlin API
        // This is a suspend function that starts the workflow and waits for the result
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
