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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Runs a workflow with two durable timers using the standalone SDK Java POC. */
public final class HelloPocTimer {
  private static final String TASK_QUEUE = "sdk-java-poc-timer";

  @WorkflowInterface
  public interface TimerWorkflow {
    @WorkflowMethod
    String run(String name, long firstTimerMillis);
  }

  public static class TimerWorkflowImpl implements TimerWorkflow {
    @Override
    public String run(String name, long firstTimerMillis) {
      Workflow.sleep(Duration.ofMillis(firstTimerMillis));
      Workflow.sleep(100);
      return "Hello " + name;
    }
  }

  public static void main(String[] args) throws InterruptedException {
    String target = args.length == 0 ? "127.0.0.1:7233" : args[0];
    String workflowId = args.length > 1 ? args[1] : "poc-timer-" + UUID.randomUUID();
    String mode = args.length > 2 ? args[2] : "combined";
    long firstTimerMillis = args.length > 3 ? Long.parseLong(args[3]) : 350;
    if (!mode.equals("combined") && !mode.equals("worker") && !mode.equals("client")) {
      throw new IllegalArgumentException("Expected combined, worker, or client mode");
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = mode.equals("client") ? null : WorkerFactory.newInstance(client);
    try {
      if (factory != null) {
        factory.newWorker(TASK_QUEUE).registerWorkflowImplementationTypes(TimerWorkflowImpl.class);
        factory.start();
      }
      if (mode.equals("worker")) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> close(factory, service)));
        new CountDownLatch(1).await();
      } else {
        runWorkflow(client, workflowId, firstTimerMillis);
      }
    } finally {
      close(factory, service);
    }
  }

  private static void runWorkflow(WorkflowClient client, String workflowId, long firstTimerMillis) {
    TimerWorkflow workflow =
        client.newWorkflowStub(
            TimerWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(TASK_QUEUE)
                .build());
    String input = "Temporal \"Core\" ✓";
    String result = workflow.run(input, firstTimerMillis);
    if (!("Hello " + input).equals(result)) {
      throw new IllegalStateException("Unexpected workflow result: " + result);
    }
    System.out.println("Workflow " + workflowId + " completed with " + result);
  }

  private static void close(WorkerFactory factory, WorkflowServiceStubs service) {
    try {
      if (factory != null) {
        factory.shutdown();
        factory.awaitTermination(15, TimeUnit.SECONDS);
        if (!factory.isTerminated()) {
          throw new IllegalStateException("Worker factory did not terminate");
        }
      }
    } finally {
      service.shutdownNow();
      service.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  private HelloPocTimer() {}
}
