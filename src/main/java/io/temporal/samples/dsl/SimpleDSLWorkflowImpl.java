package io.temporal.samples.dsl;

import io.temporal.samples.dsl.models.DslWorkflow;
import io.temporal.workflow.CancellationScope;
import java.util.HashMap;
import java.util.Map;

public class SimpleDSLWorkflowImpl implements SimpleDSLWorkflow {
  private Map<String, String> bindings;
  private Map<String, CancellationScope> activityToCancelableScopes;

  public SimpleDSLWorkflowImpl() {
    activityToCancelableScopes = new HashMap<>();
  }

  @Override
  public void execute(DslWorkflow dsl) {
    this.bindings = dsl.variables;
    dsl.root.execute(this.bindings, activityToCancelableScopes);
  }

  @Override
  public void callback(String activityName) {
    System.out.println("Callback for " + activityName + " gets called");
    if (activityToCancelableScopes.containsKey(activityName)) {
      System.out.println("Callback for " + activityName + " gets found and calling cancel");

      CancellationScope scope = activityToCancelableScopes.get(activityName);
      scope.cancel();
    }
  }
}
