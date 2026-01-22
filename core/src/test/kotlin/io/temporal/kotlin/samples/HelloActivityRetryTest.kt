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
 * Unit test for [HelloActivityRetry] sample.
 *
 * Tests activity retry functionality using KTestWorkflowExtension.
 * The activity implementation fails 3 times before succeeding.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class HelloActivityRetryTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = kTestWorkflowExtension {
            workflowImplementationTypes = listOf(HelloActivityRetry.GreetingWorkflowImpl::class)
            activityImplementations = listOf(HelloActivityRetry.GreetingActivitiesImpl())
        }
    }

    @Test
    fun `activity retries on failure and eventually succeeds`(
        client: KClient,
        options: KWorkflowOptions,
    ) = runTest {
        val result = client.executeWorkflow(
            HelloActivityRetry.GreetingWorkflow::getGreeting,
            "World",
            options.copy(workflowId = "hello-retry-${UUID.randomUUID()}")
        )

        assertEquals("Hello World!", result)
    }
}
