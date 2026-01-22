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
import java.util.concurrent.TimeUnit

/**
 * Unit test for [HelloDynamic] sample.
 *
 * Tests dynamic workflow and activity execution using Kotlin-only testing APIs.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class HelloDynamicTest {

    companion object {
        @JvmField
        @RegisterExtension
        val testExtension = kTestWorkflowExtension {
            workflowImplementationTypes = listOf(HelloDynamic.DynamicGreetingWorkflow::class)
            activityImplementations = listOf(HelloDynamic.DynamicGreetingActivity())
        }
    }

    @Test
    fun `dynamic workflow with signal and activity returns formatted greeting`(
        client: KClient,
        options: KWorkflowOptions,
    ) = runTest {
        // Start workflow with signal using untyped API
        val handle = client.signalWithStart(
            workflowType = "DynamicWF",
            workflowArgs = arrayOf("Hello"),
            signalName = "greetingSignal",
            signalArgs = arrayOf("John"),
            options = options,
        )

        // Wait for workflow to finish and get result
        val result = handle.getResult<String>()

        assertEquals("DynamicACT: Hello John from: DynamicWF", result)
    }
}
