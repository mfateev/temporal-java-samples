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

import com.google.common.base.Throwables
import io.temporal.client.WorkflowUpdateException
import io.temporal.failure.ApplicationFailure
import io.temporal.kotlin.activity.KActivityOptions
import io.temporal.kotlin.client.KWorkflowClient
import io.temporal.kotlin.client.KWorkflowOptions
import io.temporal.kotlin.worker.KWorkerFactory
import io.temporal.kotlin.workflow.KWorkflow
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.UpdateMethod
import io.temporal.workflow.UpdateValidatorMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

/**
 * Sample Temporal workflow that demonstrates how to use workflow update methods.
 *
 * Workflow update is another way to interact with a running workflow along with signals and queries.
 * Workflow update combines aspects of signals and queries. Like signals, workflow update can mutate
 * workflow state. Like queries, workflow update can return a value.
 *
 * In Kotlin workflows, update handlers should be suspend functions. This allows them to call
 * activities, child workflows, and other workflow operations using KWorkflow APIs.
 * Update validators, however, are NOT suspend functions - they must return synchronously.
 *
 * Note: Make sure to set `frontend.enableUpdateWorkflowExecution=true` in your Temporal
 * config to enable update.
 *
 * This is the Kotlin equivalent of the Java HelloUpdate sample.
 */
object HelloUpdate {

    // Define the task queue name
    const val TASK_QUEUE = "HelloUpdateTaskQueue"

    // Define the workflow unique id
    const val WORKFLOW_ID = "HelloUpdateWorkflow"

    /**
     * The Workflow Definition's Interface.
     */
    @WorkflowInterface
    interface GreetingWorkflow {

        @WorkflowMethod
        suspend fun getGreetings(): List<String>

        /**
         * Workflow update method. This method is executed when the workflow receives an update request.
         *
         * In Kotlin workflows, update methods should be suspend functions to allow calling
         * activities, child workflows, and other workflow operations.
         */
        @UpdateMethod
        suspend fun addGreeting(name: String): Int

        /**
         * Optional workflow update validator. The validator must take the same parameters as the update handler.
         * If the validator fails by throwing any exception, the update request will be rejected.
         *
         * Note: Validators are NOT suspend functions - they must return synchronously.
         */
        @UpdateValidatorMethod(updateName = "addGreeting")
        fun addGreetingValidator(name: String)

        /**
         * Signal method to exit the workflow.
         */
        @SignalMethod
        fun exit()
    }

    /**
     * Define the workflow implementation.
     */
    class GreetingWorkflowImpl : GreetingWorkflow {

        // messageQueue holds up to 10 messages (received from updates)
        private val messageQueue = mutableListOf<String>()
        private val receivedMessages = mutableListOf<String>()
        private var exit = false

        override suspend fun getGreetings(): List<String> {
            while (true) {
                // Block until messages are available or exit is signaled
                KWorkflow.awaitCondition { messageQueue.isNotEmpty() || exit }

                if (messageQueue.isEmpty() && exit) {
                    return receivedMessages.toList()
                }

                val message = messageQueue.removeAt(0)
                receivedMessages.add(message)
            }
        }

        override suspend fun addGreeting(name: String): Int {
            if (name.isEmpty()) {
                // Updates can fail by throwing a TemporalFailure
                throw ApplicationFailure.newFailure("Cannot greet someone with an empty name", "Failure")
            }

            // Update handlers are suspend functions, so we can use KWorkflow.executeActivity
            val greeting = KWorkflow.executeActivity(
                HelloActivity.GreetingActivities::composeGreeting,
                KActivityOptions(startToCloseTimeout = 2.seconds),
                "Hello",
                name
            )
            messageQueue.add(greeting)

            // Updates can return data back to the client
            return receivedMessages.size + messageQueue.size
        }

        override fun addGreetingValidator(name: String) {
            // Update validators have the same restrictions as Queries
            // Workflow state cannot be mutated inside a validator
            if (receivedMessages.size >= 10) {
                throw IllegalStateException("Only 10 greetings may be added")
            }
        }

        override fun exit() {
            exit = true
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
        worker.registerActivitiesImplementations(HelloActivity.GreetingActivitiesImpl())

        // Start all workers
        factory.start()

        // Create the workflow options
        val workflowOptions = KWorkflowOptions(
            workflowId = WORKFLOW_ID,
            taskQueue = TASK_QUEUE
        )

        // Start workflow and get a handle for interaction
        val handle = client.startWorkflow(GreetingWorkflow::getGreetings, workflowOptions)

        // Get Java client for update operations
        // Note: Updates are sent synchronously from the client side
        val javaClient = client.workflowClient
        val workflow = javaClient.newWorkflowStub(GreetingWorkflow::class.java, WORKFLOW_ID)

        // Send the first workflow update
        workflow.addGreeting("World")

        // Send the second update
        workflow.addGreeting("Universe")

        // Demonstrate untyped stub update
        val greetingStub = javaClient.newUntypedWorkflowStub(WORKFLOW_ID)
        greetingStub.update("addGreeting", Int::class.java, "Temporal")

        // Demonstrate update failure handling
        try {
            workflow.addGreeting("")
            System.exit(-1)
        } catch (e: WorkflowUpdateException) {
            val cause = Throwables.getRootCause(e)
            println("\n Update failed, root cause: ${cause.message}")
        }

        // Send updates up to the validator limit of 10
        var sentUpdates = workflow.addGreeting("Update")
        while (sentUpdates < 10) {
            sentUpdates = workflow.addGreeting("Again")
        }

        // Demonstrate update rejection by validator
        try {
            workflow.addGreeting("Will be rejected")
            System.exit(-1)
        } catch (e: WorkflowUpdateException) {
            val cause = Throwables.getRootCause(e)
            println("\n Update rejected: ${cause.message}")
        }

        // Send exit signal using the Kotlin handle
        handle.signal(GreetingWorkflow::exit)

        // Get the workflow result using the Kotlin handle
        val greetings = handle.result()

        println(greetings.joinToString("\n"))
        System.exit(0)
    }
}
