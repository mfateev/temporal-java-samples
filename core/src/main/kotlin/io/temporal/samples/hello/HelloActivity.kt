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
import io.temporal.kotlin.activity.KActivity
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Sample Temporal Workflow Definition that executes a suspend Activity.
 *
 * This is the recommended approach for Kotlin activities, using suspend functions
 * to enable natural use of coroutines for I/O-bound operations like HTTP calls,
 * database queries, or any other async Kotlin libraries.
 *
 * Key features demonstrated:
 * - Defining suspend activity interfaces
 * - Non-blocking heartbeating with KActivity.suspendHeartbeat()
 * - Using coroutines (delay, async I/O) in activities
 * - Registering suspend activities with the worker
 *
 * For traditional blocking activities, see HelloSyncActivity.
 */
object HelloActivity {

    const val TASK_QUEUE = "HelloActivityTaskQueue"
    const val WORKFLOW_ID = "HelloActivityWorkflow"

    private val log = LoggerFactory.getLogger(HelloActivity::class.java)

    /**
     * Workflow interface that calls our suspend activity.
     */
    @WorkflowInterface
    interface DataProcessingWorkflow {

        @WorkflowMethod
        suspend fun processData(itemCount: Int): String
    }

    /**
     * Suspend Activity Interface.
     *
     * Activities defined as suspend functions can use Kotlin coroutines naturally:
     * - Use delay() instead of Thread.sleep()
     * - Call suspend functions from coroutine-based HTTP clients (Ktor, etc.)
     * - Use KActivity.suspendHeartbeat() for non-blocking heartbeats
     *
     * Note: The suspend keyword is part of the interface definition.
     */
    @ActivityInterface
    interface DataActivities {

        /**
         * A suspend activity that processes items with progress reporting.
         * Demonstrates non-blocking delays and heartbeating.
         */
        @ActivityMethod(name = "ProcessItems")
        suspend fun processItems(itemCount: Int): ProcessingResult
    }

    /**
     * Result of the data processing activity.
     */
    data class ProcessingResult(
        val itemsProcessed: Int,
        val duration: Long,
        val processorThread: String
    )

    /**
     * Workflow implementation that executes the suspend activity.
     */
    class DataProcessingWorkflowImpl : DataProcessingWorkflow {

        override suspend fun processData(itemCount: Int): String {
            // Use string-based API for suspend activities
            // Note: Method references for suspend activities use KSuspendFunction types
            // which require explicit API support
            val result = KWorkflow.executeActivity<ProcessingResult>(
                "ProcessItems",
                KActivityOptions(
                    startToCloseTimeout = 60.seconds,
                    // Enable heartbeat timeout so the server detects activity health
                    heartbeatTimeout = 10.seconds
                ),
                itemCount
            )

            return "Processed ${result.itemsProcessed} items in ${result.duration}ms " +
                "on thread: ${result.processorThread}"
        }
    }

    /**
     * Suspend Activity Implementation.
     *
     * This implementation uses Kotlin coroutines:
     * - delay() for non-blocking waits (simulating I/O operations)
     * - KActivity.suspendHeartbeat() for non-blocking progress reporting
     *
     * The activity runs on a coroutine dispatcher separate from the Java SDK's
     * activity executor, allowing efficient use of threads during I/O waits.
     */
    class DataActivitiesImpl : DataActivities {

        override suspend fun processItems(itemCount: Int): ProcessingResult {
            val startTime = System.currentTimeMillis()
            val info = KActivity.getInfo()
            log.info("Starting suspend activity ${info.activityId}, processing $itemCount items")

            // Process items with progress heartbeating
            for (i in 1..itemCount) {
                // Simulate async I/O operation (e.g., HTTP call, DB query)
                // In a real implementation, this could be:
                //   - httpClient.get(url).body()
                //   - database.query(sql)
                //   - fileChannel.read()
                delay(100.milliseconds)

                // Report progress using non-blocking heartbeat
                // The heartbeat details can be retrieved on retry for resumability
                val progress = Progress(
                    currentItem = i,
                    totalItems = itemCount,
                    percentComplete = (i * 100) / itemCount
                )
                KActivity.suspendHeartbeat(progress)

                log.info("Processed item $i/$itemCount (${progress.percentComplete}%)")
            }

            val duration = System.currentTimeMillis() - startTime
            log.info("Suspend activity completed in ${duration}ms")

            return ProcessingResult(
                itemsProcessed = itemCount,
                duration = duration,
                processorThread = Thread.currentThread().name
            )
        }
    }

    /**
     * Progress data for heartbeating.
     * This can be retrieved on retry using KActivity.getHeartbeatDetails<Progress>()
     */
    data class Progress(
        val currentItem: Int,
        val totalItems: Int,
        val percentComplete: Int
    )

    /**
     * Main entry point that starts the worker and executes the workflow.
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val service = WorkflowServiceStubs.newLocalServiceStubs()
        val client = KWorkflowClient(service)
        val factory = KWorkerFactory(client)

        val worker = factory.newWorker(TASK_QUEUE)

        // Register the workflow
        worker.registerWorkflowImplementationType<DataProcessingWorkflowImpl>()

        // Register suspend activities using the special registration method
        // This enables coroutine-based execution for suspend functions
        worker.registerSuspendActivities(DataActivitiesImpl())

        factory.start()
        log.info("Worker started on task queue: $TASK_QUEUE")

        // Execute the workflow
        val options = KWorkflowOptions(
            workflowId = WORKFLOW_ID,
            taskQueue = TASK_QUEUE
        )

        log.info("Starting workflow to process 5 items...")
        val result = client.executeWorkflow(
            DataProcessingWorkflow::processData,
            options,
            5  // Process 5 items
        )

        println("Result: $result")
        System.exit(0)
    }
}
