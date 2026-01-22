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

import io.temporal.activity.Activity
import io.temporal.kotlin.activity.KDynamicActivity
import io.temporal.kotlin.client.KClient
import io.temporal.kotlin.client.KWorkflowOptions
import io.temporal.kotlin.common.KEncodedValues
import io.temporal.kotlin.worker.KWorker
import io.temporal.kotlin.worker.KWorkerOptions
import io.temporal.kotlin.workflow.KDynamicWorkflow
import io.temporal.kotlin.workflow.KWorkflow
import io.temporal.kotlin.activity.KActivityOptions
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

/**
 * Sample Temporal Workflow demonstrating dynamic workflows and activities.
 *
 * This sample shows:
 * - KDynamicWorkflow: Handles any workflow type at runtime with Kotlin coroutines
 * - KDynamicActivity: Handles any activity type at runtime
 * - Dynamic signal handlers: Receive signals by name
 * - Untyped workflow execution: Start workflows by type name
 * - Untyped activity execution: Execute activities by name
 *
 * Dynamic workflows and activities are useful for:
 * - Workflow/activity types determined at runtime
 * - Generic routing or dispatching systems
 * - Building workflow engines on top of Temporal
 *
 * Dynamic workflows support Kotlin coroutines and can use all KWorkflow APIs.
 */
object HelloDynamic {

    const val TASK_QUEUE = "HelloDynamicTaskQueue"
    const val WORKFLOW_ID = "HelloDynamicWorkflow"

    /**
     * Dynamic workflow that handles any workflow type.
     *
     * Uses Kotlin coroutines and KWorkflow APIs.
     * The workflow type is available via KWorkflow.info.workflowType.
     */
    class DynamicGreetingWorkflow : KDynamicWorkflow {
        private var name: String = ""

        override suspend fun execute(args: KEncodedValues): Any? {
            val greeting = args.get<String>(0)
            val workflowType = KWorkflow.info.workflowType

            // Register dynamic signal handler to receive the name
            KWorkflow.registerDynamicSignalHandler { signalName, signalArgs ->
                if (signalName == "greetingSignal") {
                    name = signalArgs.get<String>(0)
                }
            }

            // Wait for the name to be set via signal using coroutine
            KWorkflow.awaitCondition { name.isNotEmpty() }

            // Execute activity by name using KWorkflow API
            val result = KWorkflow.executeActivity<String>(
                "DynamicACT",
                listOf(greeting, name, workflowType),
                KActivityOptions(startToCloseTimeout = 10.seconds)
            )
            return result
        }
    }

    /**
     * Dynamic activity that handles any activity type.
     *
     * The activity type is available via Activity.getExecutionContext().info.activityType.
     */
    class DynamicGreetingActivity : KDynamicActivity {
        override fun execute(args: KEncodedValues): Any? {
            val activityType = Activity.getExecutionContext().info.activityType
            val greeting = args.get<String>(0)
            val name = args.get<String>(1)
            val fromType = args.get<String>(2)

            return "$activityType: $greeting $name from: $fromType"
        }
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val client = KClient.connect()

        // Create worker with dynamic workflow and activity
        val worker = KWorker(
            client,
            KWorkerOptions(
                taskQueue = TASK_QUEUE,
                dynamicWorkflow = DynamicGreetingWorkflow::class,
                dynamicActivity = DynamicGreetingActivity()
            )
        )
        worker.start()

        val options = KWorkflowOptions(
            workflowId = WORKFLOW_ID,
            taskQueue = TASK_QUEUE
        )

        // Start workflow with signal using untyped API
        // Note: The workflow type "DynamicWF" is not explicitly registered
        val handle = client.signalWithStart(
            workflowType = "DynamicWF",
            signalName = "greetingSignal",
            signalArgs = arrayOf("John"),
            workflowArgs = arrayOf("Hello"),
            options = options,
        )

        // Wait for workflow to finish and get result
        val result = handle.getResult<String>()

        println(result)
        // Expected output: "DynamicACT: Hello John from: DynamicWF"

        System.exit(0)
    }
}
