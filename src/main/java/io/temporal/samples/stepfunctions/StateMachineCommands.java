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

import io.temporal.samples.stepfunctions.command.StateCommand;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StateMachineCommands {
  private final List<StateCommand> commands;
  private final Map<String, String> variables;

  public StateMachineCommands(List<StateCommand> commands, Map<String, String> variables) {
    this.commands = commands;
    this.variables = variables;
  }

  public List<StateCommand> getCommands() {
    return new ArrayList<>(commands);
  }

  public Map<String, String> getVariables() {
    return new HashMap<>(variables);
  }

  public void addCommand(StateCommand command) {
    this.commands.add(command);
  }

  public void setVariable(String name, String value) {
    variables.put(name, value);
  }
}
