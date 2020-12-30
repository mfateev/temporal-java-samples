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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StateMachineDefinition {
  private final Map<StateName, StateDefinition> states = new HashMap<>();
  private final RootStateDefinition root;

  public StateMachineDefinition(StateName startAt) {
    this.root = new RootStateDefinition(this, startAt);
  }

  public StateMachineCommands getCommandsToExecute(StateMachineEvents events) {
    StateMachineCommands commands =
        new StateMachineCommands(new ArrayList<>(), events.getVariables());
    if (events.getVariables().get("started") == null) {
      root.addCommands(events, commands);
      commands.setVariable("started", "true");
      return commands;
    }
    List<StateName> completions = events.getStateCompletions();
    for (StateName completedName : completions) {
      StateDefinition completed = getStateDefinition(completedName);
      completed.complete(events, commands);
    }
    return commands;
  }

  public StateDefinition getStateDefinition(StateName name) {
    StateDefinition result = states.get(name);
    if (result == null) {
      throw new IllegalArgumentException("Unknown state: " + name);
    }
    return result;
  }

  public StateDefinition getParentDefinition(StateName child) {
    Optional<StateName> parentName = child.getParent();
    return parentName.isPresent() ? getStateDefinition(parentName.get()) : root;
  }
}
