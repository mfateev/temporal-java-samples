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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

public class StateMachineLoader {

  public static StateMachineDefinition load(Reader document) throws IOException, ParseException {
    JSONObject definition = (JSONObject) JSONValue.parseWithException(document);
    Map<StateName, StateDefinition> states = new HashMap<>();
    StateName startAt = new StateName((String) definition.get("StartAt"), Optional.empty());
    StateMachineDefinition stateMachine = new StateMachineDefinition(states, startAt);
    addStates(stateMachine, (JSONObject) definition.get("States"), Optional.empty(), states);
    return stateMachine;
  }

  private static void addStates(
      StateMachineDefinition stateMachine,
      JSONObject jsonStates,
      Optional<StateName> parent,
      Map<StateName, StateDefinition> states) {
    Set<Map.Entry> entries = jsonStates.entrySet();
    for (Map.Entry entry : entries) {
      StateName name = new StateName((String) entry.getKey(), parent);
      JSONObject value = (JSONObject) entry.getValue();
      addStates(stateMachine, name, value, states);
    }
  }

  private static void addStates(
      StateMachineDefinition stateMachine,
      StateName name,
      JSONObject value,
      Map<StateName, StateDefinition> states) {
    String type = (String) value.get("Type");
    String next = (String) value.get("Next");
    Optional<StateName> nextName =
        next == null ? Optional.empty() : Optional.of(new StateName(next, name.getParent()));
    boolean end = (Boolean) value.getOrDefault("End", false);
    StateDefinition result;
    if (type.equals("Task")) {
      String resource = (String) value.get("Resource");
      result = new TaskStateDefinition(stateMachine, name, nextName, resource, end);
    } else if (type.equals("Wait")) {
      int seconds = Integer.parseInt((String) value.get("Seconds"));
      result = new WaitStateDefinition(stateMachine, name, nextName, seconds, end);
    } else if (type.equals("Parallel")) {
      List<StateName> branchHeads = new ArrayList<>();
      JSONArray jsonBranches = (JSONArray) value.get("Branches");
      for (Object branch : jsonBranches) {
        JSONObject jsonBranch = (JSONObject) branch;
        System.out.println(branch);
        StateName startAt = new StateName((String) jsonBranch.get("StartAt"), Optional.of(name));
        branchHeads.add(startAt);
        JSONObject branchStates = (JSONObject) jsonBranch.get("States");
        addStates(stateMachine, branchStates, Optional.of(name), states);
      }
      result = new ParallelStateDefinition(stateMachine, name, nextName, end, branchHeads);
    } else {
      throw new UnsupportedOperationException("Task " + type + " type is not yet supported");
    }
    states.put(name, result);
  }
}
