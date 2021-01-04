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

import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.stepfunctions.definition.StateMachineCommands;
import io.temporal.samples.stepfunctions.definition.StateMachineDefinition;
import io.temporal.samples.stepfunctions.definition.StateMachineEvents;
import io.temporal.samples.stepfunctions.definition.StateMachineLoader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.parser.ParseException;

public class AmazonStatesLanguageActivitiesImpl implements AmazonStatesLanguageActivities {

  private final String pathPrefix;

  /**
   * Read only cache of the workflow definitions. Consider using LRU cache if the list of potential
   * definitions is very large.
   */
  private final Map<String, StateMachineDefinition> definitions = new ConcurrentHashMap<>();

  public AmazonStatesLanguageActivitiesImpl(String pathPrefix) {
    this.pathPrefix = pathPrefix;
  }

  @Override
  public StateMachineCommands evaluate(StateMachineEvents events) {
    StateMachineDefinition definition =
        definitions.computeIfAbsent(events.getStateMachineName(), this::loadDefinition);
    return definition.getCommandsToExecute(events);
  }

  private StateMachineDefinition loadDefinition(String name) {
    String resourceName = pathPrefix + "/" + name + ".json";
    InputStream resource = this.getClass().getResourceAsStream(resourceName);
    try {
      return StateMachineLoader.load(new InputStreamReader(resource, StandardCharsets.UTF_8));
    } catch (IOException | ParseException e) {
      ApplicationFailure failure =
          ApplicationFailure.newNonRetryableFailure(
              "Failure loading definition: " + name, "definition");
      failure.initCause(e);
      throw failure;
    }
  }
}
