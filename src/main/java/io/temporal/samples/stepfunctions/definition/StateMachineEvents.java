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

import java.util.List;
import java.util.Map;

public class StateMachineEvents {
  private String stateMachineName;
  private List<StateName> stateCompletions;
  private Map<String, String> variables;

  /** Needed for Jackson serialization */
  public StateMachineEvents() {}

  public StateMachineEvents(
      String stateMachineName, List<StateName> stateCompletions, Map<String, String> variables) {
    this.stateMachineName = stateMachineName;
    this.stateCompletions = stateCompletions;
    this.variables = variables;
  }

  public String getStateMachineName() {
    return stateMachineName;
  }

  public List<StateName> getStateCompletions() {
    return stateCompletions;
  }

  public Map<String, String> getVariables() {
    return variables;
  }
}
