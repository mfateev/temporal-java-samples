/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.dsl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.temporal.activity.Activity;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.samples.dsl.models.DslWorkflow;
import io.temporal.samples.hello.HelloActivity;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Unit test for {@link HelloActivity}. Doesn't use an external Temporal service. */
public class DslActivityTest {

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  private WorkflowClient client;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker("dsl");
    worker.registerWorkflowImplementationTypes(SimpleDSLWorkflowImpl.class);

    client = testEnv.getWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testMockedActivity() throws IOException {
    worker = testEnv.newWorker("dsl");
    worker.registerWorkflowImplementationTypes(SimpleDSLWorkflowImpl.class);

    SampleActivities.SampleActivities1 activities = mock(SampleActivities.SampleActivities1.class);
    when(activities.getInfo()).thenThrow(Activity.wrap(new IllegalArgumentException()));
    Worker newWorker = testEnv.newWorker("SampleActivities1");
    newWorker.registerActivitiesImplementations(activities);

    testEnv.start();

    // Get a workflow stub using the same task queue the worker uses.
    SimpleDSLWorkflow workflow =
        client.newWorkflowStub(
            SimpleDSLWorkflow.class,
            WorkflowOptions.newBuilder()
                .setRetryOptions(RetryOptions.newBuilder().setDoNotRetry().build())
                .setTaskQueue("dsl")
                .build());

    Path resourceDirectory = Paths.get("src", "test", "resources", "dsl", "workflow1.yaml");

    String absolutePath = resourceDirectory.toFile().getAbsolutePath();

    ObjectMapper objectMapper =
        new ObjectMapper(new YAMLFactory()).registerModule(new ParameterNamesModule());
    objectMapper.enable(MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES);
    objectMapper.findAndRegisterModules();

    DslWorkflow dslWorkflow = objectMapper.readValue(new File(absolutePath), DslWorkflow.class);
    workflow.execute(dslWorkflow);
  }
}
