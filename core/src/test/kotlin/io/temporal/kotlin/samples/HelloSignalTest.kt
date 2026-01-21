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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Unit test for [HelloSignal] sample.
 *
 * Tests workflow signal functionality using KTestWorkflowExtension.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class HelloSignalTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = kTestWorkflowExtension {
            registerWorkflowImplementationTypes<HelloSignal.GreetingWorkflowImpl>()
        }
    }

    @Test
    fun `workflow receives signals and returns greetings`(
        client: KClient,
        options: KWorkflowOptions,
    ) = runTest {
        val workflowOptions = options.copy(workflowId = "hello-signal-${UUID.randomUUID()}")

        // Start workflow
        val handle = client.startWorkflow(
            HelloSignal.GreetingWorkflow::getGreetings,
            workflowOptions
        )

        // Send signals
        handle.signal(HelloSignal.GreetingWorkflow::waitForName, "World")
        handle.signal(HelloSignal.GreetingWorkflow::waitForName, "Universe")
        handle.signal(HelloSignal.GreetingWorkflow::exit)

        // Get result
        val greetings = handle.result()

        assertEquals(2, greetings.size)
        assertTrue(greetings.contains("Hello World!"))
        assertTrue(greetings.contains("Hello Universe!"))
    }

    @Test
    fun `workflow returns empty list when exit signaled immediately`(
        client: KClient,
        options: KWorkflowOptions,
    ) = runTest {
        val workflowOptions = options.copy(workflowId = "hello-signal-empty-${UUID.randomUUID()}")

        // Start workflow
        val handle = client.startWorkflow(
            HelloSignal.GreetingWorkflow::getGreetings,
            workflowOptions
        )

        // Signal exit immediately
        handle.signal(HelloSignal.GreetingWorkflow::exit)

        // Get result
        val greetings = handle.result()

        assertTrue(greetings.isEmpty())
    }

    @Test
    fun `workflow accumulates multiple signals`(
        client: KClient,
        options: KWorkflowOptions,
    ) = runTest {
        val workflowOptions = options.copy(workflowId = "hello-signal-multi-${UUID.randomUUID()}")

        // Start workflow
        val handle = client.startWorkflow(
            HelloSignal.GreetingWorkflow::getGreetings,
            workflowOptions
        )

        // Send multiple signals
        for (i in 1..5) {
            handle.signal(HelloSignal.GreetingWorkflow::waitForName, "User$i")
        }
        handle.signal(HelloSignal.GreetingWorkflow::exit)

        // Get result
        val greetings = handle.result()

        assertEquals(5, greetings.size)
        for (i in 1..5) {
            assertTrue(greetings.contains("Hello User$i!"))
        }
    }
}
