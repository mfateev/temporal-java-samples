#!/usr/bin/env python3
"""Run the installed timer sample against a disposable server and restart its worker JVM."""

import argparse
from collections import Counter
from contextlib import ExitStack
import json
from pathlib import Path
import socket
import subprocess
import time
from uuid import uuid4


def wait_for_server(server, port):
    deadline = time.monotonic() + 15
    while time.monotonic() < deadline:
        if server.poll() is not None:
            raise RuntimeError("Temporal dev server exited")
        try:
            with socket.create_connection(("127.0.0.1", port), timeout=1):
                return
        except OSError:
            time.sleep(0.1)
    raise TimeoutError("Temporal dev server did not become ready")


def stop(process):
    if process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=20)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)
            raise


def verify(sample, logs):
    logs.mkdir(parents=True, exist_ok=True)
    with socket.socket() as reservation:
        reservation.bind(("127.0.0.1", 0))
        port = reservation.getsockname()[1]
    target = f"127.0.0.1:{port}"
    workflow_id = f"poc-timer-restart-{uuid4()}"
    cleanup = ExitStack()

    def launch(label, command):
        with (logs / f"{label}.log").open("w") as output:
            process = subprocess.Popen(command, stdout=output, stderr=subprocess.STDOUT)
        cleanup.callback(stop, process)
        return process

    def history():
        result = subprocess.run(
            ["temporal", "workflow", "show", "--address", target, "--workflow-id", workflow_id,
             "--output", "json"],
            capture_output=True, text=True, timeout=10,
        )
        return json.loads(result.stdout) if result.returncode == 0 else None

    def counts(record):
        return Counter(event["eventType"] for event in record["events"])

    try:
        server = launch("server", [
            "temporal", "server", "start-dev", "--headless", "--ip", "127.0.0.1",
            "--port", str(port),
        ])
        wait_for_server(server, port)
        worker = launch("worker-before", [sample, target, workflow_id, "worker"])
        client = launch("client", [sample, target, workflow_id, "client", "10000"])

        deadline = time.monotonic() + 30
        while time.monotonic() < deadline:
            if worker.poll() is not None or client.poll() is not None:
                raise RuntimeError("Worker or client exited before the first timer was recorded")
            record = history()
            if record and counts(record)["EVENT_TYPE_TIMER_STARTED"] == 1:
                break
            time.sleep(0.1)
        else:
            raise TimeoutError("First timer was never recorded")

        worker.kill()
        worker.wait(timeout=5)
        pending = history()
        pending_events = counts(pending) if pending is not None else Counter()
        if pending_events["EVENT_TYPE_TIMER_STARTED"] != 1 or pending_events["EVENT_TYPE_TIMER_FIRED"]:
            raise RuntimeError("The first timer must still be pending after the original JVM exits")
        (logs / "pending-history.json").write_text(json.dumps(pending, indent=2))

        replacement = launch("worker-after", [sample, target, workflow_id, "worker"])
        if client.wait(timeout=90) != 0:
            raise RuntimeError("Client failed to obtain the workflow result")
        if replacement.poll() is not None:
            raise RuntimeError("Replacement worker exited unexpectedly")
        client_output = (logs / "client.log").read_text()
        if 'completed with Hello Temporal "Core" ✓' not in client_output:
            raise RuntimeError("Workflow did not return the expected result")

        finished = history()
        if finished is None:
            raise RuntimeError("Workflow history is unavailable")
        (logs / "history.json").write_text(json.dumps(finished, indent=2))
        events = counts(finished)
        expected = {
            "EVENT_TYPE_WORKFLOW_EXECUTION_STARTED": 1,
            "EVENT_TYPE_TIMER_STARTED": 2,
            "EVENT_TYPE_TIMER_FIRED": 2,
            "EVENT_TYPE_WORKFLOW_EXECUTION_COMPLETED": 1,
            "EVENT_TYPE_WORKFLOW_TASK_FAILED": 0,
        }
        if any(events[event] != count for event, count in expected.items()):
            raise RuntimeError(f"Unexpected workflow history {events}")
        print(client_output.strip())
        print(f"Replaced worker JVM {worker.pid} with {replacement.pid} while the first timer was pending.")
        print("History confirms two timers started and fired, with no workflow task failures.")
    finally:
        cleanup.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    sample_root = Path(__file__).resolve().parent
    application = "sdk-java-poc-timer-sample"
    distribution = sample_root / "build" / "install" / application
    parser.add_argument("--sample", type=Path, default=distribution / "bin" / application)
    parser.add_argument("--logs", type=Path, default=sample_root / "build/restart-verification")
    args = parser.parse_args()
    verify(str(args.sample.resolve()), args.logs)
