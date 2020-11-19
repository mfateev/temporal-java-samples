package io.temporal.samples.dsl.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.temporal.workflow.CancellationScope;
import java.util.Map;

public class Statement {
  public ActivityInvocation activity;
  public Sequence sequence;
  public Parallel parallel;

  @JsonCreator
  public Statement(
      @JsonProperty("sequence") Sequence sequence,
      @JsonProperty("activity") ActivityInvocation activity,
      @JsonProperty("parallel") Parallel parallel) {
    this.activity = activity;
    this.sequence = sequence;
    this.parallel = parallel;
  }

  public Void execute(Map<String, String> bindings, Map<String, CancellationScope> map) {
    if (this.parallel != null) {
      this.parallel.execute(bindings, map);
    }

    if (this.sequence != null) {
      this.sequence.execute(bindings, map);
    }

    if (this.activity != null) {
      this.activity.execute(bindings, map);
    }

    return null;
  }
}
