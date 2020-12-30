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

package io.temporal.samples.common;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.serviceclient.WorkflowServiceStubs;

/**
 * Calls Temporal DescribeWorkflowExecution gRPC API.
 *
 * <p>The service API is defined in <a
 * href="https://github.com/temporalio/api/blob/master/temporal/api/workflowservice/v1/service.proto">service.proto</a>
 *
 * @author fateev
 */
public class DescribeWorkflowExecution {

  public static void main(String[] args) {
    if (args.length < 1 || args.length > 2) {
      System.err.println(
          "Usage: java " + DescribeWorkflowExecution.class.getName() + " <workflowId> [<runId>]");
      System.exit(1);
    }
    String workflowId = args[0];
    String runId = args.length > 1 ? args[1] : "";

    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();

    WorkflowExecution workflowExecution =
        WorkflowExecution.newBuilder().setWorkflowId(workflowId).setRunId(runId).build();
    DescribeWorkflowExecutionRequest request =
        DescribeWorkflowExecutionRequest.newBuilder().setExecution(workflowExecution).build();

    DescribeWorkflowExecutionResponse response =
        service.blockingStub().describeWorkflowExecution(request);

    System.out.println("DescribeWorkflowExecution response for " + workflowExecution + ":");
    System.out.println(response);
  }
}
