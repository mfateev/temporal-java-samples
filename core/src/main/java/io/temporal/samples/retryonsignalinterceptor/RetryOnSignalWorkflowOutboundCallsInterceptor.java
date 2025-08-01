package io.temporal.samples.retryonsignalinterceptor;

import com.google.common.base.Throwables;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptorBase;
import io.temporal.workflow.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Most of the complexity of the implementation is due to the asynchronous nature of the activity
 * invocation at the interceptor level.
 */
public class RetryOnSignalWorkflowOutboundCallsInterceptor
    extends WorkflowOutboundCallsInterceptorBase {

  private enum Action {
    RETRY,
    FAIL
  }

  private class ActivityRetryState<R> {
    private final ActivityInput<R> input;
    private final CompletablePromise<R> asyncResult = Workflow.newPromise();
    private final Functions.Proc1<Exception> compensation;
    private CompletablePromise<Action> action;
    private Exception lastFailure;
    private int attempt;

    private ActivityRetryState(ActivityInput<R> input, Functions.Proc1<Exception> compensation) {
      this.input = input;
      this.compensation = compensation;
    }

    ActivityOutput<R> execute() {
      return executeWithAsyncRetry();
    }

    // Executes activity with retry based on signaled action asynchronously
    private ActivityOutput<R> executeWithAsyncRetry() {
      attempt++;
      lastFailure = null;
      action = null;
      ActivityOutput<R> result =
          RetryOnSignalWorkflowOutboundCallsInterceptor.super.executeActivity(input);
      result
          .getResult()
          .handle(
              (r, failure) -> {
                // No failure complete
                if (failure == null) {
                  pendingActivities.remove(this);
                  asyncResult.complete(r);
                  return null;
                }
                if (compensation != null) {
                  // If there is a compensation function, call it with the failure.
                  try {
                    compensation.apply(failure);
                  } catch (RuntimeException e) {
                    asyncResult.completeExceptionally(e);
                    return null;
                  }
                }
                // Asynchronously executes requested action when signal is received.
                lastFailure = failure;
                action = Workflow.newPromise();
                return action.thenApply(
                    a -> {
                      if (a == Action.FAIL) {
                        asyncResult.completeExceptionally(failure);
                      } else {
                        // Retries recursively.
                        executeWithAsyncRetry();
                      }
                      return null;
                    });
              });
      return new ActivityOutput<>(result.getActivityId(), asyncResult);
    }

    public void retry() {
      if (action == null) {
        return;
      }
      action.complete(Action.RETRY);
    }

    public void fail() {
      if (action == null) {
        return;
      }
      action.complete(Action.FAIL);
    }

    public String getStatus() {
      String activityName = input.getActivityName();
      if (lastFailure == null) {
        return "Executing activity \"" + activityName + "\". Attempt=" + attempt;
      }
      if (!action.isCompleted()) {
        return "Last \""
            + activityName
            + "\" activity failure:\n"
            + Throwables.getStackTraceAsString(lastFailure)
            + "\n\nretry or fail ?";
      }
      return (action.get() == Action.RETRY ? "Going to retry" : "Going to fail")
          + " activity \""
          + activityName
          + "\"";
    }
  }

  /**
   * For the example brevity the interceptor fails or retries all activities that are waiting for an
   * action. The production version might implement retry and failure of specific activities by
   * their type.
   */
  private final List<ActivityRetryState<?>> pendingActivities = new ArrayList<>();

  /**
   * Map of compensations for activities that are waiting for an action. The key is the activity
   * correlation ID.
   */
  private static final WorkflowThreadLocal<Functions.Proc1<Exception>> compensation =
      new WorkflowThreadLocal<>();

  public RetryOnSignalWorkflowOutboundCallsInterceptor(WorkflowOutboundCallsInterceptor next) {
    super(next);
    // Registers the listener for retry and fail signals as well as getPendingActivitiesStatus
    // query. Register in the constructor to do it once per workflow instance.
    Workflow.registerListener(
        new RetryOnSignalInterceptorListener() {
          @Override
          public void retry() {
            for (ActivityRetryState<?> pending : pendingActivities) {
              pending.retry();
            }
          }

          @Override
          public void fail() {
            for (ActivityRetryState<?> pending : pendingActivities) {
              pending.fail();
            }
          }

          @Override
          public String getPendingActivitiesStatus() {
            StringBuilder result = new StringBuilder();
            for (ActivityRetryState<?> pending : pendingActivities) {
              if (result.length() > 0) {
                result.append('\n');
              }
              result.append(pending.getStatus());
            }
            return result.toString();
          }
        });
  }

  @Override
  public <R> ActivityOutput<R> executeActivity(ActivityInput<R> input) {
    var c = compensation.get();
    ActivityRetryState<R> retryState = new ActivityRetryState<R>(input, c);
    pendingActivities.add(retryState);
    return retryState.execute();
  }

  static void setCompensation(Functions.Proc1<Exception> c) {
    compensation.set(c);
  }

  static void cleanCompensation() {
    compensation.set(null);
  }
}
