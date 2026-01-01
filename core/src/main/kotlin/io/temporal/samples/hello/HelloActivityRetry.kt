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
import io.temporal.kotlin.common.KRetryOptions
import io.temporal.kotlin.worker.KWorkerFactory
import io.temporal.worker.registerWorkflowImplementationType
import io.temporal.kotlin.workflow.KWorkflow
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

/**
 * Sample Temporal workflow that demonstrates workflow activity retries.
 *
 * This is the Kotlin equivalent of the Java HelloActivityRetry sample, demonstrating:
 * - KWorkflowClient for type-safe workflow execution
 * - KWorkerFactory for automatic Kotlin coroutine support
 * - Activity retry configuration with RetryOptions
 */
object HelloActivityRetry {

    // Define the task queue name
    const val TASK_QUEUE = "HelloActivityWithRetriesTaskQueue"

    // Define our workflow unique id
    const val WORKFLOW_ID = "HelloActivityWithRetriesWorkflow"

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
            // Execute activity with retry options.
            // The "startToCloseTimeout" sets the maximum time of a single Activity execution attempt.
            // The "initialInterval" sets the interval of the first retry.
            // The "doNotRetry" option is a list of application failures for which retries should not be performed.
            return KWorkflow.executeActivity(
                GreetingActivities::composeGreeting,
                KActivityOptions(
                    startToCloseTimeout = 10.seconds,
                    retryOptions = KRetryOptions(
                        initialInterval = 1.seconds,
                        doNotRetry = listOf(IllegalArgumentException::class.java.name)
                    )
                ),
                "Hello",
                name
            )
        }
    }

    /**
     * Activity implementation that simulates failures for retry demonstration.
     */
    class GreetingActivitiesImpl : GreetingActivities {

        private var callCount = 0
        private var lastInvocationTime = 0L

        @Synchronized
        override fun composeGreeting(greeting: String, name: String): String {
            if (lastInvocationTime != 0L) {
                val timeSinceLastInvocation = System.currentTimeMillis() - lastInvocationTime
                print("$timeSinceLastInvocation milliseconds since last invocation. ")
            }
            lastInvocationTime = System.currentTimeMillis()

            if (++callCount < 4) {
                println("composeGreeting activity is going to fail")
                // IllegalStateException is not in "do not retry" list, so retry will happen
                throw IllegalStateException("not yet")
            }

            // After 3 unsuccessful retries we finally complete
            println("composeGreeting activity is going to complete")
            return "$greeting $name!"
        }
    }

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
