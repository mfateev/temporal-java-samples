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

package io.temporal.samples.stepfunctions;

import static org.junit.Assert.assertNull;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;

public class AmazonStatesLanguageWorkflowTest {

  /** Prints a history of the workflow under test in case of a test failure. */
  @Rule
  public TestWatcher watchman =
      new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
          if (testEnv != null) {
            System.err.println(testEnv.getDiagnostics());
            testEnv.close();
          }
        }
      };

  @Rule public Timeout timeout = new Timeout(5, TimeUnit.SECONDS);

  private static final String TASK_QUEUE = "TestTaskQueue";

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  private WorkflowClient workflowClient;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(AmazonStatesLanguageWorkflow.class);
    worker.registerActivitiesImplementations(
        new AmazonStatesLanguageActivitiesImpl("/stepfunctions"));
    worker.registerActivitiesImplementations();
    workflowClient = testEnv.getWorkflowClient();
    testEnv.start();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testSingleTask() {
    WorkflowStub stub =
        workflowClient.newUntypedWorkflowStub(
            "SPWorkflow1", WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    stub.start(new AmazonStatesLanguageWorkflow.Input());
    String result = stub.getResult(String.class);
    assertNull(result);
  }

  @Test
  public void testParallel() {
    WorkflowStub stub =
        workflowClient.newUntypedWorkflowStub(
            "SPParallel", WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    stub.start(new AmazonStatesLanguageWorkflow.Input());
    String result = stub.getResult(String.class);
    assertNull(result);
  }
}
