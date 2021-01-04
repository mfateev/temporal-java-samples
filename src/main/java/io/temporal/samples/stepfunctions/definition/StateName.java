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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StateName {
  private List<String> ids;

  /** Needed for Jackson serialization */
  public StateName() {}

  public StateName(List<String> ids) {
    this.ids = new ArrayList<>(ids);
  }

  public StateName(String name, Optional<StateName> parent) {
    List<String> nameIds = parent.isPresent() ? parent.get().getIds() : new ArrayList<>();
    nameIds.add(name);
    ids = nameIds;
  }

  public StateName(String name) {
    this(name, Optional.empty());
  }

  public List<String> getIds() {
    return ids;
  }

  @JsonIgnore
  public Optional<StateName> getParent() {
    if (ids.size() == 1) {
      return Optional.empty();
    }
    return Optional.of(new StateName(ids.subList(0, ids.size() - 1)));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StateName)) return false;
    StateName stateName = (StateName) o;
    return Objects.equal(ids, stateName.ids);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(ids);
  }

  @Override
  public String toString() {
    return Joiner.on("/").join(ids);
  }
}
