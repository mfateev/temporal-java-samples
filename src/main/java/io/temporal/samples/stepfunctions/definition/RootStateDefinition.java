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

import io.temporal.samples.stepfunctions.command.CompleteWorkflowCommand;
import java.util.Optional;

public class RootStateDefinition extends StateDefinition {

  private final StateName startAt;

  protected RootStateDefinition(StateMachineDefinition stateMachine, StateName startAt) {
    super(stateMachine, null, null, Optional.empty(), true);
    this.startAt = startAt;
  }

  @Override
  protected void addCommands(StateMachineEvents events, StateMachineCommands commands) {
    getStateDefinition(startAt).addCommands(events, commands);
  }

  @Override
  void completeChild(StateMachineEvents events, StateMachineCommands commands, StateName name) {
    commands.addCommand(new CompleteWorkflowCommand(null));
  }

  @Override
  public void complete(StateMachineEvents events, StateMachineCommands commands) {
    throw new UnsupportedOperationException();
  }
}
