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

public abstract class StateDefinition {

  protected final StateMachineDefinition stateMachine;
  private final StateName name;
  private final StateType type;
  private final StateName next;
  private final boolean end;

  protected StateDefinition(
      StateMachineDefinition stateMachine,
      StateName name,
      StateType type,
      StateName next,
      boolean end) {
    this.stateMachine = stateMachine;
    this.name = name;
    this.type = type;
    this.next = next;
    this.end = end;
  }

  public StateName getName() {
    return name;
  }

  public StateType getType() {
    return type;
  }

  public StateName getNext() {
    return next;
  }

  public boolean isEnd() {
    return end;
  }

  public void complete(StateMachineEvents events, StateMachineCommands commands) {
    if (isEnd()) {
      StateDefinition parent = stateMachine.getParentDefinition(name);
      parent.completeChild(events, commands, name);
    } else {
      stateMachine.getStateDefinition(getNext()).addCommands(events, commands);
    }
  }

  void completeChild(StateMachineEvents events, StateMachineCommands commands, StateName name) {
    throw new UnsupportedOperationException();
  }

  protected abstract void addCommands(StateMachineEvents events, StateMachineCommands commands);

  protected final StateDefinition getStateDefinition(StateName branch) {
    return stateMachine.getStateDefinition(branch);
  }
}
