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
import io.temporal.kotlin.activity.KLocalActivityOptions
import io.temporal.kotlin.client.KWorkflowClient
import io.temporal.kotlin.client.KWorkflowOptions
import io.temporal.kotlin.worker.KWorkerFactory
import io.temporal.kotlin.workflow.KWorkflow
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

/**
 * Hello World Temporal workflow that executes a single local activity.
 *
 * Some Activities are very short lived and do not need the queuing semantic, flow
 * control, rate limiting and routing capabilities. For these Temporal supports so called local
 * Activity feature. Local Activities are executed in the same worker process as the Workflow that
 * invoked them. Consider using local Activities for functions that are:
 *
 * - no longer than a few seconds
 * - do not require global rate limiting
 * - do not require routing to specific workers or pools of workers
 * - can be implemented in the same binary as the Workflow that invokes them
 *
 * The main benefit of local Activities is that they are much more efficient in utilizing
 * Temporal service resources and have much lower latency overhead comparing to the usual Activity
 * invocation.
 *
 * This is the Kotlin equivalent of the Java HelloLocalActivity sample, demonstrating:
 * - KWorkflowClient for type-safe workflow execution
 * - KWorkerFactory for automatic Kotlin coroutine support
 * - KWorkflow.newLocalActivityStub for local activity stubs
 * - KWorkflow.executeLocalActivity for local activity execution
 */
object HelloLocalActivity {

    const val TASK_QUEUE = "HelloLocalActivity"

    /**
     * The Workflow Definition's Interface.
     */
    @WorkflowInterface
    interface GreetingWorkflow {

        @WorkflowMethod
        suspend fun getGreeting(name: String): String
    }

    /**
     * Activity interface.
     */
    @ActivityInterface
    interface GreetingActivities {

        @ActivityMethod
        fun composeGreeting(greeting: String, name: String): String
    }

    /**
     * GreetingWorkflow implementation that calls a local activity.
     */
    class GreetingWorkflowImpl : GreetingWorkflow {

        override suspend fun getGreeting(name: String): String {
            // Execute the local activity using direct method reference
            return KWorkflow.executeLocalActivity(
                GreetingActivities::composeGreeting,
                KLocalActivityOptions(startToCloseTimeout = 2.seconds),
                "Hello",
                name
            )
        }
    }

    /**
     * Local activity implementation.
     */
    class GreetingLocalActivityImpl : GreetingActivities {

        override fun composeGreeting(greeting: String, name: String): String {
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
        worker.registerWorkflowImplementationTypes<GreetingWorkflowImpl>()

        // Register activities
        worker.registerActivitiesImplementations(GreetingLocalActivityImpl())

        // Start all workers
        factory.start()

        // Define workflow options
        val options = KWorkflowOptions(
            taskQueue = TASK_QUEUE
        )

        // Execute our workflow using the type-safe Kotlin API
        val greeting = client.executeWorkflow(
            GreetingWorkflow::getGreeting,
            options,
            "World"
        )

        println(greeting)
        System.exit(0)
    }
}
