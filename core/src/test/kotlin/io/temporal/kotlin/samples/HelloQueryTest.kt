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
import io.temporal.kotlin.testing.kTestWorkflowExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Unit test for [HelloQuery] sample.
 *
 * Tests workflow query functionality using KTestWorkflowExtension.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class HelloQueryTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = kTestWorkflowExtension {
            registerWorkflowImplementationTypes<HelloQuery.GreetingWorkflowImpl>()
        }
    }

    @Test
    fun `query returns initial greeting before sleep`(
        client: KClient,
        options: KWorkflowOptions,
    ) = runTest {
        val workflowOptions = options.copy(workflowId = "hello-query-${UUID.randomUUID()}")

        // Start workflow and get handle
        val handle = client.startWorkflow(
            HelloQuery.GreetingWorkflow::createGreeting,
            "World",
            workflowOptions
        )

        // Query should return "Hello World!" initially
        val greeting = handle.query(HelloQuery.GreetingWorkflow::queryGreeting)
        assertEquals("Hello World!", greeting)

        // Wait for workflow to complete (time skipping will speed this up)
        handle.result()
    }

    @Test
    fun `query returns bye greeting after workflow completes`(
        client: KClient,
        options: KWorkflowOptions,
    ) = runTest {
        val workflowOptions = options.copy(workflowId = "hello-query-complete-${UUID.randomUUID()}")

        // Start and wait for completion
        val handle = client.startWorkflow(
            HelloQuery.GreetingWorkflow::createGreeting,
            "Kotlin",
            workflowOptions
        )

        // Wait for workflow to complete
        handle.result()

        // Query after completion should return "Bye Kotlin!"
        val greeting = handle.query(HelloQuery.GreetingWorkflow::queryGreeting)
        assertEquals("Bye Kotlin!", greeting)
    }
}
