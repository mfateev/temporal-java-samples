package io.temporal.samples.poc;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Runs a workflow with two durable timers using the standalone SDK Java POC. */
public final class HelloPocTimer {
  private static final String TASK_QUEUE = "sdk-java-poc-timer";

  @WorkflowInterface
  public interface TimerWorkflow {
    @WorkflowMethod
    String run(String name);
  }

  public static class TimerWorkflowImpl implements TimerWorkflow {
    @Override
    public String run(String name) {
      Workflow.sleep(Duration.ofMillis(350));
      Workflow.sleep(100);
      return "Hello " + name;
    }
  }

  public static void main(String[] args) {
    WorkflowServiceStubs service =
        args.length == 0
            ? WorkflowServiceStubs.newLocalServiceStubs()
            : WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder().setTarget(args[0]).build());
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    try {
      factory.newWorker(TASK_QUEUE).registerWorkflowImplementationTypes(TimerWorkflowImpl.class);
      factory.start();
      String workflowId = args.length > 1 ? args[1] : "poc-timer-" + UUID.randomUUID();
      TimerWorkflow workflow =
          client.newWorkflowStub(
              TimerWorkflow.class,
              WorkflowOptions.newBuilder().setWorkflowId(workflowId).setTaskQueue(TASK_QUEUE).build());
      String input = "Temporal \"Core\" ✓";
      String result = workflow.run(input);
      if (!("Hello " + input).equals(result)) {
        throw new IllegalStateException("Unexpected workflow result: " + result);
      }
      System.out.println("Workflow " + workflowId + " completed with " + result);
    } finally {
      try {
        factory.shutdown();
        factory.awaitTermination(15, TimeUnit.SECONDS);
        if (!factory.isTerminated()) {
          throw new IllegalStateException("Worker factory did not terminate");
        }
      } finally {
        service.shutdownNow();
        service.awaitTermination(5, TimeUnit.SECONDS);
      }
    }
  }

  private HelloPocTimer() {}
}
