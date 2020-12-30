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

import io.temporal.activity.LocalActivityOptions;
import io.temporal.common.converter.EncodedValues;
import io.temporal.samples.stepfunctions.command.StateCommand;
import io.temporal.samples.stepfunctions.definition.StateName;
import io.temporal.workflow.CompletablePromise;
import io.temporal.workflow.DynamicWorkflow;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** TODO: Untyped way to call continue as new */
public class AmazonStatesLanguageWorkflow implements DynamicWorkflow {

  public static class Input {
    private final String stateMachineName;
    private final String stateMachineVersion;

    public Input(String stateMachineName, String stateMachineVersion) {
      this.stateMachineName = stateMachineName;
      this.stateMachineVersion = stateMachineVersion;
    }

    public String getStateMachineName() {
      return stateMachineName;
    }

    public String getStateMachineVersion() {
      return stateMachineVersion;
    }
  }

  private final AmazonStatesLanguageActivities asl =
      Workflow.newLocalActivityStub(
          AmazonStatesLanguageActivities.class,
          LocalActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

  private final Map<StateName, StateCommand> outstandingCommands = new HashMap<>();
  private final Map<String, String> variables = new HashMap<>();
  private final List<StateName> completions = new ArrayList<>();

  private final CompletablePromise<Object> workflowResult = Workflow.newPromise();

  private Input input;

  @Override
  public Object execute(EncodedValues encodedValues) {
    this.input = encodedValues.get(0, Input.class);
    while (!workflowResult.isCompleted()) {
      StateMachineEvents events =
          new StateMachineEvents(
              input.getStateMachineName(), input.getStateMachineVersion(), completions, variables);
      StateMachineCommands commands = asl.evaluate(events);
      updateVariables(commands.getVariables());
      executeCommands(commands.getCommands());
      Workflow.await(() -> completions.size() > 0 || workflowResult.isCompleted());
    }
    return workflowResult.get();
  }

  private void executeCommands(List<StateCommand> commands) {
    completions.clear();
    for (StateCommand command : commands) {
      outstandingCommands.put(command.getName(), command);
      Promise<Void> completion = command.execute();
      completion.handle(
          (r, e) -> {
            // TODO: Error handling
            if (e != null) {
              workflowResult.completeExceptionally(e);
              return null;
            }
            outstandingCommands.remove(command.getName());
            completions.add(command.getName());
            return null;
          });
    }
  }

  private void updateVariables(Map<String, String> variableUpdates) {
    for (Map.Entry<String, String> update : variableUpdates.entrySet()) {
      String variableName = update.getKey();
      String value = update.getValue();
      if (value == null) {
        variables.remove(variableName);
      } else {
        variables.put(variableName, value);
      }
    }
  }
}
