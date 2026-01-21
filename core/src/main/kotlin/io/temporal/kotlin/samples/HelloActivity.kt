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

import io.temporal.activity.ActivityInterface
import io.temporal.kotlin.activity.KActivity
import io.temporal.kotlin.activity.KActivityOptions
import io.temporal.kotlin.common.kargs
import io.temporal.kotlin.client.KClient
import io.temporal.kotlin.client.KWorkflowOptions
import io.temporal.kotlin.worker.KWorker
import io.temporal.kotlin.worker.KWorkerOptions
import io.temporal.kotlin.workflow.KWorkflow
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

/**
 * Sample Temporal Workflow Definition demonstrating mixed activity interfaces.
 *
 * This sample shows how the Kotlin SDK handles activity interfaces with both
 * synchronous and suspend methods:
 * - Synchronous activity: Regular function, runs on thread pool
 * - Suspend activity: Kotlin coroutine, supports non-blocking operations
 *
 * Use `registerActivitiesImplementations` to register implementations with suspend methods.
 */
object HelloActivity {

    const val TASK_QUEUE = "HelloActivityTaskQueue"
    const val WORKFLOW_ID = "HelloActivityWorkflow"

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
                GreetingActivities::composeGreeting, kargs("Hello", name), options
            )
            // Call suspend activity
            return KWorkflow.executeActivity(
                GreetingActivities::formatGreeting, greeting, options
            )
        }
    }

    class GreetingActivitiesImpl : GreetingActivities {
        override fun composeGreeting(greeting: String, name: String): String {
            KActivity.logger().info("Sync activity: composing greeting")
            return "$greeting $name"
        }

        override suspend fun formatGreeting(greeting: String): String {
            KActivity.logger().info("Suspend activity: formatting greeting")
            delay(10) // Simulate async operation
            return "$greeting!"
        }
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val client = KClient.connect()

        val worker = KWorker(
            client,
            KWorkerOptions(
                taskQueue = TASK_QUEUE,
                workflows = listOf(GreetingWorkflowImpl::class),
                activities = listOf(GreetingActivitiesImpl())
            )
        )
        worker.start()

        val options = KWorkflowOptions(workflowId = WORKFLOW_ID, taskQueue = TASK_QUEUE)
        val greeting = client.executeWorkflow(GreetingWorkflow::getGreeting, "Kotlin", options)

        println(greeting)
        System.exit(0)
    }
}
