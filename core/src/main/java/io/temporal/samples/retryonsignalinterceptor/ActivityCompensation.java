package io.temporal.samples.retryonsignalinterceptor;

import io.temporal.workflow.Functions;

public class ActivityCompensation {
  public static AutoCloseable newCompensation(Functions.Proc1<Exception> compensation) {
    RetryOnSignalWorkflowOutboundCallsInterceptor.setCompensation(compensation);
    return () -> {
      RetryOnSignalWorkflowOutboundCallsInterceptor.setCompensation(null);
    };
  }
}
