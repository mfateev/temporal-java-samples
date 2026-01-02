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
import io.temporal.kotlin.activity.registerSuspendActivities
import io.temporal.kotlin.client.KWorkflowClient
import io.temporal.kotlin.client.KWorkflowOptions
import io.temporal.kotlin.worker.KWorkerFactory
import io.temporal.kotlin.workflow.KWorkflow
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.registerWorkflowImplementationType
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

/**
 * Sample Temporal Workflow Definition demonstrating mixed activity interfaces.
 *
 * This sample shows how the Kotlin SDK handles activity interfaces with both
 * synchronous and suspend methods:
 * - Synchronous activity: Regular function, runs on thread pool
 * - Suspend activity: Kotlin coroutine, supports non-blocking operations
 *
 * Use `registerSuspendActivities` to register implementations with suspend methods.
 */
object HelloActivity {

    // Define the task queue name
    const val TASK_QUEUE = "HelloActivityTaskQueue"

    // Define our workflow unique id
    const val WORKFLOW_ID = "HelloActivityWorkflow"

    private val log = LoggerFactory.getLogger(HelloActivity::class.java)

    @WorkflowInterface
    interface GreetingWorkflow {
        @WorkflowMethod
        suspend fun getGreeting(name: String): String
    }

    /**
     * Activity interface with both synchronous and suspend methods.
     */
    @ActivityInterface
    interface GreetingActivities {
        /** Synchronous activity - runs on thread pool */
        fun composeGreeting(greeting: String, name: String): String

        /** Suspend activity - runs as coroutine */
        suspend fun formatGreeting(greeting: String): String
    }

    class GreetingWorkflowImpl : GreetingWorkflow {
        private val options = KActivityOptions(startToCloseTimeout = 2.seconds)

        override suspend fun getGreeting(name: String): String {
            // Call synchronous activity
            val greeting = KWorkflow.executeActivity(
                GreetingActivities::composeGreeting, options, "Hello", name
            )
            // Call suspend activity
            return KWorkflow.executeActivity(
                GreetingActivities::formatGreeting, options, greeting
            )
        }
    }

    class GreetingActivitiesImpl : GreetingActivities {
        override fun composeGreeting(greeting: String, name: String): String {
            log.info("Sync activity: composing greeting")
            return "$greeting $name"
        }

        override suspend fun formatGreeting(greeting: String): String {
            log.info("Suspend activity: formatting greeting")
            delay(10) // Simulate async operation
            return "$greeting!"
        }
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val service = WorkflowServiceStubs.newLocalServiceStubs()
        val client = KWorkflowClient(service)
        val factory = KWorkerFactory(client)
        val worker = factory.newWorker(TASK_QUEUE)

        worker.registerWorkflowImplementationType<GreetingWorkflowImpl>()
        // Use registerSuspendActivities for interfaces with suspend methods
        worker.registerSuspendActivities(GreetingActivitiesImpl())

        factory.start()

        val options = KWorkflowOptions(workflowId = WORKFLOW_ID, taskQueue = TASK_QUEUE)
        val greeting = client.executeWorkflow(GreetingWorkflow::getGreeting, options, "World")

        println(greeting)
        System.exit(0)
    }
}
