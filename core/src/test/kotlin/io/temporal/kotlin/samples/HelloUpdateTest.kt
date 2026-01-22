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

import io.temporal.client.WorkflowUpdateException
import io.temporal.kotlin.client.KClient
import io.temporal.kotlin.client.KWorkflowOptions
import io.temporal.kotlin.testing.KTestWorkflowEnvironment
import io.temporal.kotlin.testing.kTestWorkflowExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Unit test for [HelloUpdate] sample.
 *
 * Tests workflow update functionality using KTestWorkflowExtension.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class HelloUpdateTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = kTestWorkflowExtension {
            workflowImplementationTypes = listOf(HelloUpdate.GreetingWorkflowImpl::class)
            // Activities with suspend methods are registered via testEnv.registerActivitiesImplementations()
        }
    }

    @Test
    fun `update adds greeting and returns count`(
        testEnv: KTestWorkflowEnvironment,
        client: KClient,
        options: KWorkflowOptions,
    ) = runTest {
        // Register activities with suspend methods via the test environment
        testEnv.registerActivitiesImplementations(HelloActivity.GreetingActivitiesImpl())

        val workflowOptions = options.copy(workflowId = "hello-update-${UUID.randomUUID()}")

        // Start workflow
        val handle = client.startWorkflow(
            HelloUpdate.GreetingWorkflow::getGreetings,
            workflowOptions
        )

        // Send updates using Kotlin typed API
        val count1 = handle.executeUpdate(HelloUpdate.GreetingWorkflow::addGreeting, "World")
        assertEquals(1, count1)

        val count2 = handle.executeUpdate(HelloUpdate.GreetingWorkflow::addGreeting, "Universe")
        assertEquals(2, count2)

        // Signal exit
        handle.signal(HelloUpdate.GreetingWorkflow::exit)

        // Get result
        val greetings = handle.result()

        assertEquals(2, greetings.size)
        assertTrue(greetings.contains("Hello World"))
        assertTrue(greetings.contains("Hello Universe"))
    }

    @Test
    fun `update with empty name fails`(
        testEnv: KTestWorkflowEnvironment,
        client: KClient,
        options: KWorkflowOptions,
    ) = runTest {
        // Register activities with suspend methods via the test environment
        testEnv.registerActivitiesImplementations(HelloActivity.GreetingActivitiesImpl())

        val workflowOptions = options.copy(workflowId = "hello-update-fail-${UUID.randomUUID()}")

        // Start workflow
        val handle = client.startWorkflow(
            HelloUpdate.GreetingWorkflow::getGreetings,
            workflowOptions
        )

        // Update with empty name should fail - use Kotlin typed API
        assertThrows<WorkflowUpdateException> {
            handle.executeUpdate(HelloUpdate.GreetingWorkflow::addGreeting, "")
        }

        // Signal exit to complete workflow
        handle.signal(HelloUpdate.GreetingWorkflow::exit)
        handle.result()
    }

    @Test
    fun `validator rejects update when limit reached`(
        testEnv: KTestWorkflowEnvironment,
        client: KClient,
        options: KWorkflowOptions,
    ) = runTest {
        // Register activities with suspend methods via the test environment
        testEnv.registerActivitiesImplementations(HelloActivity.GreetingActivitiesImpl())

        val workflowOptions = options.copy(workflowId = "hello-update-limit-${UUID.randomUUID()}")

        // Start workflow
        val handle = client.startWorkflow(
            HelloUpdate.GreetingWorkflow::getGreetings,
            workflowOptions
        )

        // Send updates up to the limit of 10 using Kotlin typed API
        for (i in 1..10) {
            handle.executeUpdate(HelloUpdate.GreetingWorkflow::addGreeting, "User$i")
        }

        // 11th update should be rejected by validator
        assertThrows<WorkflowUpdateException> {
            handle.executeUpdate(HelloUpdate.GreetingWorkflow::addGreeting, "Rejected")
        }

        // Signal exit to complete workflow
        handle.signal(HelloUpdate.GreetingWorkflow::exit)
        val greetings = handle.result()

        assertEquals(10, greetings.size)
    }
}
