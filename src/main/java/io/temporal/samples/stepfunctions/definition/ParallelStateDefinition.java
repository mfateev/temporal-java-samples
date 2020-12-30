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

package io.temporal.samples.stepfunctions.definition;

import io.temporal.samples.stepfunctions.StateMachineCommands;
import io.temporal.samples.stepfunctions.StateMachineEvents;
import java.util.List;

public class ParallelStateDefinition extends StateDefinition {

  private final List<StateName> branches;
  private final String variableName;

  public ParallelStateDefinition(
      StateMachineDefinition stateMachine,
      StateName name,
      StateType type,
      StateName next,
      boolean end,
      List<StateName> branches) {
    super(stateMachine, name, type, next, end);
    this.branches = branches;
    this.variableName = "parallel-" + getName();
  }

  @Override
  protected void addCommands(StateMachineEvents events, StateMachineCommands commands) {
    for (StateName branch : branches) {
      getStateDefinition(branch).addCommands(events, commands);
    }
    commands.setVariable(variableName, String.valueOf(branches.size()));
  }

  @Override
  void completeChild(StateMachineEvents events, StateMachineCommands commands, StateName name) {
    int count = Integer.parseInt(commands.getVariables().get(variableName));
    if (count == 0) {
      complete(events, commands);
    } else {
      commands.setVariable(variableName, String.valueOf(count - 1));
    }
  }
}
