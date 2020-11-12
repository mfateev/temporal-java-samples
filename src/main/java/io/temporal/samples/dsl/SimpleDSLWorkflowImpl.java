package io.temporal.samples.dsl;

import io.temporal.samples.dsl.models.DslWorkflow;
import java.util.Map;

public class SimpleDSLWorkflowImpl implements SimpleDSLWorkflow {
  private Map<String, String> bindings;

  @Override
  public void execute(DslWorkflow dsl) {
    this.bindings = dsl.variables;
    dsl.root.execute(this.bindings);
  }
}
