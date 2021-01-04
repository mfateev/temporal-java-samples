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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.temporal.samples.stepfunctions.command.CompleteWorkflowCommand;
import io.temporal.samples.stepfunctions.command.StateCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.json.simple.parser.ParseException;
import org.junit.Test;

public class StateMachineTest {

  @Test
  public void testLoad() throws IOException, ParseException {
    String stateMachineName = "SPWorkflow1";
    InputStream resource = this.getClass().getResourceAsStream("/stepfunctions/SPWorkflow1.json");
    StateMachineDefinition stateMachine =
        StateMachineLoader.load(new InputStreamReader(resource, StandardCharsets.UTF_8));
    assertNotNull(stateMachine);
    StateMachineEvents events =
        new StateMachineEvents(stateMachineName, new ArrayList<>(), new HashMap<>());
    StateMachineCommands commands = stateMachine.getCommandsToExecute(events);
    assertNotNull(commands);
    List<StateCommand> commandList = commands.getCommands();
    assertEquals(1, commandList.size());
    StateName hello_world = new StateName("Hello World");
    assertEquals(hello_world, commandList.get(0).getName());

    events =
        new StateMachineEvents(
            stateMachineName, Collections.singletonList(hello_world), commands.getVariables());
    commands = stateMachine.getCommandsToExecute(events);
    commandList = commands.getCommands();
    assertEquals(1, commandList.size());
    assertTrue(commandList.get(0) instanceof CompleteWorkflowCommand);

    System.out.println(commands);
  }
}
