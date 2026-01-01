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

import io.temporal.activity.Activity
import io.temporal.activity.ActivityInterface
import io.temporal.client.ActivityCompletionException
import io.temporal.kotlin.activity.KActivityOptions
import io.temporal.kotlin.client.KWorkflowClient
import io.temporal.kotlin.client.KWorkflowOptions
import io.temporal.kotlin.worker.KWorkerFactory
import io.temporal.kotlin.workflow.KChildWorkflowOptions
import io.temporal.worker.registerWorkflowImplementationType
import io.temporal.kotlin.workflow.KWorkflow
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

/**
 * Sample shows how to use workflow timer instead of WorkflowOptions->Run/ExecutionTimeout.
 *
 * This is the Kotlin equivalent of the Java HelloWorkflowTimer sample, demonstrating:
 * - KWorkflowClient for type-safe workflow execution
 * - KWorkerFactory for automatic Kotlin coroutine support
 * - KWorkflow.withTimeoutOrNull for workflow-safe timeouts (Kotlin coroutines pattern)
 * - KWorkflow.delay for workflow-safe delays
 *
 * NOTE: This sample uses a simplified approach compared to the Java version.
 * The Java version uses CancellationScope for fine-grained cancellation control.
 * In Kotlin, we use structured concurrency with withTimeoutOrNull which provides
 * similar functionality in a more idiomatic way.
 */
object HelloWorkflowTimer {

    private const val WORKFLOW_ID = "HelloWorkflowWithTimer"
    private const val TASK_QUEUE = "HelloWorkflowWithTimerTaskQueue"
    // Change time to 12 to 20 seconds to handle cancellation while child workflow is running
    private const val TIME_SECS = 8

    // Workflow
    @WorkflowInterface
    interface WorkflowWithTimer {

        @WorkflowMethod
        suspend fun execute(input: String): String
    }

    class WorkflowWithTimerImpl : WorkflowWithTimer {

        override suspend fun execute(input: String): String {
            // Use withTimeoutOrNull for workflow timer functionality
            // This allows us to timeout the business logic while still being able to
            // react and return a result when the timer fires
            val result = KWorkflow.withTimeoutOrNull(TIME_SECS.seconds) {
                // Execute our activity using direct method reference
                val activityResult = KWorkflow.executeActivity(
                    WorkflowWithTimerActivities::sayHello,
                    KActivityOptions(
                        startToCloseTimeout = 12.seconds,
                        heartbeatTimeout = 2.seconds
                    ),
                    input
                )

                // Then execute child workflow using direct method reference
                KWorkflow.executeChildWorkflow(
                    WorkflowWithTimerChildWorkflow::executeChild,
                    KChildWorkflowOptions(),
                    input
                )

                activityResult
            }

            return result ?: "Workflow timer fired - operation timed out"
        }
    }

    // Activities
    @ActivityInterface
    interface WorkflowWithTimerActivities {

        fun sayHello(input: String): String
    }

    class WorkflowWithTimerActivitiesImpl : WorkflowWithTimerActivities {

        override fun sayHello(input: String): String {
            // Here we just heartbeat then sleep for 1s
            for (i in 0 until 10) {
                try {
                    Activity.getExecutionContext().heartbeat("heartbeating: $i")
                } catch (e: ActivityCompletionException) {
                    // Do some cleanup if needed, then re-throw
                    throw e
                }
                Thread.sleep(1000)
            }
            return "Hello $input"
        }
    }

    // Child Workflows
    @WorkflowInterface
    interface WorkflowWithTimerChildWorkflow {

        @WorkflowMethod
        suspend fun executeChild(input: String): String
    }

    class WorkflowWithTimerChildWorkflowImpl : WorkflowWithTimerChildWorkflow {

        override suspend fun executeChild(input: String): String {
            // For sample we just sleep for 5 seconds and return some result
            try {
                KWorkflow.delay(5.seconds)
                return "From executeChild - $input"
            } catch (e: CancellationException) {
                // Can do cleanup if needed
                throw e
            }
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

        // Create worker
        val worker = factory.newWorker(TASK_QUEUE)

        // Register workflow and child workflow
        worker.registerWorkflowImplementationType<WorkflowWithTimerImpl>()
        worker.registerWorkflowImplementationType<WorkflowWithTimerChildWorkflowImpl>()

        // Register activities
        worker.registerActivitiesImplementations(WorkflowWithTimerActivitiesImpl())

        // Start factory (and worker)
        factory.start()

        // Define workflow options
        val options = KWorkflowOptions(
            workflowId = WORKFLOW_ID,
            taskQueue = TASK_QUEUE
        )

        // Execute workflow using the type-safe Kotlin API
        val result = client.executeWorkflow(
            WorkflowWithTimer::execute,
            options,
            "Some Name Here"
        )

        println("Workflow result: $result")
        System.exit(0)
    }
}
