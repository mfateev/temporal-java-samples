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

import io.temporal.kotlin.client.KClient
import io.temporal.kotlin.client.KWorkflowOptions
import io.temporal.kotlin.common.kargs
import io.temporal.kotlin.worker.KWorker
import io.temporal.kotlin.worker.KWorkerOptions
import io.temporal.kotlin.workflow.KWorkflow
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import kotlinx.coroutines.runBlocking

/**
 * Sample Temporal Workflow Definition that demonstrates the execution of a Child Workflow.
 *
 * Child workflows allow you to group your Workflow logic into small logical and reusable
 * units that solve a particular problem. They can be typically reused by multiple other Workflows.
 *
 * This is the Kotlin equivalent of the Java HelloChild sample, demonstrating:
 * - KWorkflowClient for type-safe workflow execution
 * - KWorkerFactory for automatic Kotlin coroutine support
 * - KWorkflow.async for parallel child workflow execution
 */
object HelloChild {

    // Define the task queue name
    const val TASK_QUEUE = "HelloChildTaskQueue"

    // Define the workflow unique id
    const val WORKFLOW_ID = "HelloChildWorkflow"

    /**
     * Define the parent workflow interface.
     */
    @WorkflowInterface
    interface GreetingWorkflow {

        /**
         * Define the parent workflow method. This method is executed when the workflow is started.
         */
        @WorkflowMethod
        suspend fun getGreeting(name: String): String
    }

    /**
     * Define the child workflow Interface.
     */
    @WorkflowInterface
    interface GreetingChild {

        /**
         * Define the child workflow method.
         */
        @WorkflowMethod
        suspend fun composeGreeting(greeting: String, name: String): String
    }

    /**
     * Define the parent workflow implementation.
     */
    class GreetingWorkflowImpl : GreetingWorkflow {

        override suspend fun getGreeting(name: String): String {
            // Execute the child workflow using direct method reference
            // The Kotlin SDK extracts the workflow type from the interface automatically
            // Options are optional - use default options when not specified
            return KWorkflow.executeChildWorkflow(
                GreetingChild::composeGreeting,
                kargs("Hello", name)
            )
        }
    }

    /**
     * Define the child workflow implementation.
     */
    class GreetingChildImpl : GreetingChild {

        override suspend fun composeGreeting(greeting: String, name: String): String {
            return "$greeting $name!"
        }
    }

    /**
     * With the workflow and child workflow defined, we can now start execution.
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val client = KClient.connect()

        val worker = KWorker(
            client,
            KWorkerOptions(
                taskQueue = TASK_QUEUE,
                workflows = listOf(GreetingWorkflowImpl::class, GreetingChildImpl::class)
            )
        )
        worker.start()

        // Define workflow options
        val options = KWorkflowOptions(
            workflowId = WORKFLOW_ID,
            taskQueue = TASK_QUEUE
        )

        // Execute our parent workflow using the type-safe Kotlin API
        val greeting = client.executeWorkflow(
            GreetingWorkflow::getGreeting,
            "World",
            options
        )

        // Display the parent workflow execution results
        println(greeting)
        System.exit(0)
    }
}
