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

package io.temporal.samples.moneytransfer;

import static io.temporal.samples.moneytransfer.AccountActivityWorker.TASK_QUEUE;

import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransferRequester {

  @SuppressWarnings("CatchAndPrintStackTrace")
  public static void main(String[] args) throws ExecutionException, InterruptedException {
    String reference;
    int amountCents;
    if (args.length == 0) {
      reference = UUID.randomUUID().toString();
      amountCents = new Random().nextInt(5000);
    } else {
      reference = args[0];
      amountCents = Integer.parseInt(args[1]);
    }
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    // client that can be used to start and signal workflows
    WorkflowClient workflowClient = WorkflowClient.newInstance(service);

    // now we can start running instances of the saga - its state will be persisted
    WorkflowOptions options =
        WorkflowOptions.newBuilder()
            .setTaskQueue(TASK_QUEUE)
            .setWorkflowId(UUID.randomUUID().toString())
            .setWorkflowIdReusePolicy(
                WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
            .build();
    AccountTransferWorkflow transferWorkflow1 =
        workflowClient.newWorkflowStub(AccountTransferWorkflow.class, options);
    String from = "account1";
    String to = "account2";

    AccountTransferWorkflow transferWorkflow2 =
        workflowClient.newWorkflowStub(AccountTransferWorkflow.class, options);

    ExecutorService executorService = Executors.newFixedThreadPool(5);
    CompletableFuture<Integer> result1 = new CompletableFuture<>();
    CompletableFuture<Integer> result2 = new CompletableFuture<>();
    executorService.execute(
        () -> {
          try {
            int result = transferWorkflow1.transfer(from, to, reference, amountCents);
            System.out.println("Result1=" + result);
            result1.complete(result);
          } catch (Throwable e) {
            result1.completeExceptionally(e);
          }
        });
    executorService.execute(
        () -> {
          try {
            int result = transferWorkflow2.transfer(from, to, reference, amountCents);
            System.out.println("Result2=" + result);
            result2.complete(result);
          } catch (Throwable e) {
            result2.completeExceptionally(e);
          }
        });
    if (!result1.get().equals(result2.get())) {
      throw new RuntimeException(
          String.format("Result1=%d, Result2=%d\n", result1.get(), result2.get()));
    }
    //    System.out.printf("Result1=%d, Result2=%d\n", result1, result2);
    //    WorkflowClient.start(transferWorkflow::transfer, from, to, reference, amountCents);
    System.out.printf("Transfer of %d cents from %s to %s completed", amountCents, from, to);
    System.exit(0);
  }
}
