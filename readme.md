# Mini RxJava

Educational implementation of a simplified reactive streams library in Java.

## Project Goal

The goal of this course project is to implement a simplified version of RxJava and to understand the main ideas of reactive programming in practice.

In this project I focused on the core concepts:
- the Observer pattern,
- reactive data flow with `Observable` and `Observer`,
- transformation operators such as `map`, `filter`, and `flatMap`,
- asynchronous execution with `Scheduler`,
- thread switching with `subscribeOn(...)` and `observeOn(...)`,
- subscription cancellation with `Disposable`,
- error handling through `onError(...)`.

I did not try to reproduce the full complexity of real RxJava.  
Instead, I implemented a smaller educational version that is easier to read, test, and explain.

---

## Task Coverage

This project covers the required blocks from the assignment.

### 1. Basic reactive components
Implemented:
- `Observer<T>`
- `Observable<T>`
- `Observable.create(...)`
- `subscribe(...)`

### 2. Data transformation operators
Implemented:
- `map(...)`
- `filter(...)`

### 3. Execution flow control
Implemented:
- `Scheduler`
- `IOThreadScheduler`
- `ComputationScheduler`
- `SingleThreadScheduler`
- `subscribeOn(...)`
- `observeOn(...)`

### 4. Additional operators and subscription management
Implemented:
- `flatMap(...)`
- `Disposable`
- error propagation to `onError(...)`

### 5. Testing
Implemented:
- unit tests for core behavior
- operator tests
- scheduler tests
- robustness tests
- stress/comparison tests with real RxJava

### 6. Report
This `README.md` serves as the project report.

---

## Project Structure

```text
mini-rxjava/
├── build.gradle.kts
├── settings.gradle.kts
├── README.md
└── src/
    ├── main/
    │   └── java/
    │       └── org/
    │           └── example/
    │               └── minirx/
    │                   ├── core/
    │                   │   ├── Observer.java
    │                   │   ├── Observable.java
    │                   │   ├── ObservableOnSubscribe.java
    │                   │   ├── Emitter.java
    │                   │   └── Disposable.java
    │                   ├── scheduler/
    │                   │   ├── Scheduler.java
    │                   │   ├── IOThreadScheduler.java
    │                   │   ├── ComputationScheduler.java
    │                   │   └── SingleThreadScheduler.java
    │                   ├── internal/
    │                   │   ├── BooleanDisposable.java
    │                   │   └── CreateEmitter.java
    │                   └── demo/
    │                       └── DemoMain.java
    └── test/
        └── java/
            └── org/
                └── example/
                    └── minirx/
                        ├── core/
                        │    ├── DisposableTest.java
                        │    └── ObservableBasicTest.java
                        ├── internal/
                        │   ├── ErrorHandlingTest.java
                        │   ├── FilterOperatorTest.java
                        │   ├── FlatMapOperatorTest.java
                        │   └── MapOperatorTest.java
                        ├── scheduler/
                        │   └── SchedulerTest.java
                        ├── integration/ 
                        │   ├── BoundaryAndRobustnessTest.java
                        │   ├── RaceConditionTest.java
                        │   ├── RxJavaComparisonTest.java
                        │   └── StressComparisonTest.java
                        └── metrics/
                            └── MetricsTest.java
```
---
## Architecture Overview

The main design idea of this project is that `Observable<T>` does not store data directly.
Instead, it stores subscription logic, that is, code that should run when an observer subscribes.

I chose this approach because it is close to the idea used in reactive libraries and at the same time simple enough for a course project. It also makes operators such as `map`, `filter`, and `flatMap` easier to implement as wrappers over the previous observable.

### Main flow
`Observable.create(...) -> operators -> subscribe(observer) `

More specifically:

`source logic -> Emitter -> Observer`

### Main roles

- **Observable** — stores source logic and exposes operators

- **Observer** — receives onNext, onError, and onComplete

- **Emitter** — sends events from source logic to the observer

- **Disposable** — allows subscription cancellation

- **Scheduler** — executes tasks on specific threads or thread pools

## Core Interfaces and Classes

### `Observer<T>`
`Observer<T>` is the consumer of events in the reactive chain.
It defines three methods:
- `onNext(T item)` — receives the next item
- `onError(Throwable throwable)` — receives an error and terminates the stream
- `onComplete()` — receives a successful completion signal

### `Disposable`
`Disposable` is used to cancel a subscription.
It defines:
- `dispose()`
- `isDisposed()`

### `Emitter<T>`
`Emitter<T>` is the object used inside `Observable.create(...)` to push events.
It allows the source to:
- emit items with `onNext(...)`
- signal errors with `onError(...)`
- signal completion with `onComplete()`
- check disposal state through `isDisposed()`

### `ObservableOnSubscribe<T>`
`ObservableOnSubscribe<T>` is a functional interface that contains source logic:
```text
void subscribe(Emitter<T> emitter);
```

This is the behavior passed into Observable.create(...).

### Observable<T>

Observable<T> is the main class of the library.

Implemented methods:

- `create(...)`
- `subscribe(...)`
- `map(...)`
- `filter(...)` 
- `flatMap(...)`
- `subscribeOn(...)`
- `observeOn(...)` 

## Internal Implementation Details

### BooleanDisposable
`BooleanDisposable` is a simple implementation of `Disposable` based on `AtomicBoolean`.
It is used to track whether a subscription has already been cancelled.

### CreateEmitter
`CreateEmitter<T>` is the concrete implementation of `Emitter<T>` used by `Observable.create(...)`.
Its responsibilities:
- deliver `onNext(...)` to the observer
- deliver `onError(...)`
- deliver `onComplete()`
- ignore signals after terminal state
- ignore signals after disposal

---
## Implemented Operators

### map(...)
`map(...)` is the simplest transformation operator in the project.
It takes one incoming item and converts it into another item.

Example:
```text
Observable.<Integer>create(emitter -> {
emitter.onNext(1);
emitter.onNext(2);
emitter.onNext(3);
emitter.onComplete();
})
.map(number -> number * 10)
```
    10, 20, 30

**Implementation idea**
`map(...)` creates a new `Observable<R>` that:
- subscribes to the current observable,
- receives each item,
- applies the mapper,
- emits the transformed item downstream.

If the mapper throws an exception, it is forwarded to `onError(...)`.

### filter(...)
`filter(...)`  does not change the item type. It only decides whether an item should be passed further or skipped.

```text
.filter(number -> number % 2 == 0)
```
    Input:
    1, 2, 3, 4, 5
    
    Output:
    2, 4

**Implementation idea**
`filter(...)` creates a new observable that:
- subscribes to the upstream observable,
- checks each item with a predicate,
- emits only matching items.

If the predicate throws an exception, the stream terminates with `onError(...)`.

### flatMap(...)
`flatMap(...)` converts each item into an inner observable and merges all inner observables into one resulting stream.

Example:

```text
.flatMap(number -> Observable.<Integer>create(innerEmitter -> {
    innerEmitter.onNext(number);
    innerEmitter.onNext(number * 10);
    innerEmitter.onComplete();
}))
```
    Input:
    1, 2, 3

    Output:
    1, 10, 2, 20, 3, 30

**Implementation idea**
For each outer item:
- apply mapper,
- create an inner observable,
- subscribe to it,
- forward inner items downstream.

The implementation uses an `AtomicInteger` counter to track active sources:
- `1` for the outer observable,
- `+1` for each active inner observable,
- `-1` when outer or inner observable completes.

The resulting observable calls `onComplete()` only when:
- the outer observable completes,
- and all inner observables have also completed.
- 
---
## Schedulers

### Scheduler
`Scheduler` is an abstraction for task execution.
It defines one method: `void execute(Runnable task);`
The idea is simple: the scheduler decides where the task runs.

### IOThreadScheduler
`IOThreadScheduler` uses a cached thread pool.
It is intended for:
- blocking work,
- waiting for external resources,
- file/network/database style tasks.

In this project it is implemented with daemon threads and readable thread names such as `mini-rx-io-1`.

### ComputationScheduler
`ComputationScheduler` uses a fixed thread pool.
It is intended for:
- CPU-bound tasks,
- calculations,
- work that does not mostly wait for I/O.

In this project the pool size is based on available processors.

### SingleThreadScheduler
`SingleThreadScheduler` uses one dedicated thread.
It is useful when:
- tasks must be executed sequentially,
- event order should remain predictable,
- parallel execution is not required.

This scheduler is especially useful for `observeOn(...)` in the educational implementation because it preserves a clear event order.

### subscribeOn(...) and observeOn(...)
These two operators are similar in appearance but have different purposes.

### subscribeOn(scheduler)
`subscribeOn(...)` moves source execution to the given scheduler.
This means the source logic inside `Observable.create(...)` starts running on the scheduler thread.

### observeOn(scheduler)
`observeOn(...)` moves observer notifications to the given scheduler.
In the educational implementation, each signal is submitted as a separate scheduler task.
This means:
- source may run on one thread,
- observer may receive `onNext(...)` and `onComplete()` on another thread.

**Important note**
In my educational implementation, `observeOn(...)` is most predictable with `SingleThreadScheduler`, because events are delivered sequentially on a single thread.

---
## Error Handling
Error handling is based on the reactive rule:
- an error is delivered through `onError(...)`
- the stream is terminated after the error
- after `onError(...)`, no more `onNext(...)` or `onComplete()` should be delivered

**Implemented error-related behavior**
- source exceptions are forwarded to `onError(...)`
- mapper and predicate exceptions are forwarded to `onError(...)`
- inner observable errors in `flatMap(...)` are forwarded to `onError(...)`
- null errors are replaced with `NullPointerException`
- signals after terminal state are ignored

---
## Disposable and Subscription Cancellation
`Disposable` allows a subscription to be cancelled.
This is important when:
- the stream is long-running,
- the observer is no longer interested in new items,
- work should stop early.

**Current behavior**
In the project:
- `CreateEmitter` tracks disposal state,
- source logic can check `emitter.isDisposed()`,
- after disposal, new signals are ignored.

**Educational simplification**
The current implementation does not include a full composite-disposable mechanism for all nested inner subscriptions. For the purposes of this course project, the implemented cancellation behavior is sufficient to demonstrate the concept.

---
## Demo Scenarios
`DemoMain.java` contains demonstration examples for:
- simple successful stream
- source error handling
- subscription cancellation with `Disposable`
- `map(...)`
- `filter(...)`
- `map(...)` + `filter(...)`
- `flatMap(...)`
- `subscribeOn(...)`
- `observeOn(...)`

These demos were used for manual validation before and during test development.

---
## Testing Strategy
Testing was divided into several groups.

### 1. Core functionality
- `ObservableBasicTest`
- `MapOperatorTest`
- `FilterOperatorTest`
- `FlatMapOperatorTest`

These tests cover the basic behavior of the observable pipeline:
- item delivery,
- successful completion,
- error propagation,
- operator correctness.

### 2. Scheduler behavior
- `SchedulerTest`

This group focuses on thread-related behavior:
- direct scheduler execution,
- `subscribeOn(...)`,
- `observeOn(...)`,
- combined thread switching.

### 3. Disposable and error handling
- `DisposableTest`
- `ErrorHandlingTest`

These tests verify:
- cancellation,
- terminal-state handling,
- correct error propagation,
- no completion after error.

### 4. Boundary and robustness tests
- `BoundaryAndRobustnessTest`

These tests verify:
- empty streams,
- filters removing all items,
- empty inner observables,
- null validation,
- robustness against observer callback failures.

### 5. Race-condition-oriented tests
- `RaceConditionTest`

These tests verify:
- concurrent `flatMap(...)` scenarios,
- no obvious item loss under load,
- completion signaled only once,
- order preservation with `observeOn(new SingleThreadScheduler())`,
- cancellation under concurrent emission.

These tests do not mathematically prove the absence of race conditions, but they are useful for detecting typical concurrency problems.

### 6. Comparison with real RxJava
- `RxJavaComparisonTest`

These tests compare:
- emitted items,
- completion behavior,
- error behavior
  between Mini RxJava and the real RxJava library.

### 7. Metrics and stress scenarios
- `MetricsTest`
- `StressComparisonTest`

These tests collect:
- functional metrics,
- timing data,
- scheduler behavior data,
- stress/comparison information for report analysis.
- 
---
## Comparison with RxJava

To validate the behavior of Mini RxJava, I compared several scenarios with the real RxJava library.

The compared scenarios were:
- simple `create(...)`,
- `map(...)`,
- `filter(...)`,
- `flatMap(...)`,
- source error,
- mapper error.

For each scenario I compared:
- emitted items,
- completion status,
- error type,
- error message.

In the tested scenarios, Mini RxJava produced the same observable behavior as RxJava. This was important for the project, because the main goal was not to build a faster library, but to reproduce the core semantics of reactive streams correctly.

---
## Metrics and Evaluation
The project includes both normal metrics and extended stress/comparison metrics.

**What was measured**
- functional correctness
- completion correctness
- error propagation correctness
- scheduler behavior correctness
- basic execution time
- stress behavior on larger scenarios

**Important note**
Timing measurements were collected on the local machine during project development. They are useful for comparative discussion, but they are not a full benchmark methodology.

They depend on:
- JVM warm-up,
- JIT optimization,
- machine load,
- execution order,
- thread scheduling.

Therefore, the project report uses timing data carefully and does not make exaggerated performance claims.

---
## Measured Results

The following metrics were collected during test execution.

### Metrics: Map + Filter scenario
```text
Mini RxJava items count: 3333
Mini RxJava completed: true
Mini RxJava error type: null
Mini RxJava error message: null
Mini RxJava duration (ns): 7625300

RxJava items count: 3333
RxJava completed: true
RxJava error type: null
RxJava error message: null
RxJava duration (ns): 18957700
```

### Metrics: FlatMap scenario
```text
Mini RxJava items count:`
Mini RxJava items count: 8
Mini RxJava completed: true
Mini RxJava error type: null
Mini RxJava error message: null
Mini RxJava duration (ns): 2566400

RxJava items count: 8
RxJava completed: true
RxJava error type: null
RxJava error message: null
RxJava duration (ns): 4470500
```

### Metrics: Scheduler scenario
```text
Mini RxJava items: [1, 2, 3]
Mini RxJava completed: true
Mini RxJava error type: null
Mini RxJava error message: null
Mini RxJava source thread: mini-rx-io-1
Mini RxJava observer thread: mini-rx-single-1
Mini RxJava duration (ns): 10366700
```

### Metrics: Error scenario

```text
Mini RxJava items count: 1
Mini RxJava completed: false
Mini RxJava error type: IllegalStateException
Mini RxJava error message: Map failure
Mini RxJava duration (ns): 127100

RxJava items count: 1
RxJava completed: false
RxJava error type: IllegalStateException
RxJava error message: Map failure
RxJava duration (ns): 743800
```

### Stress Metrics: FlatMap expansion stress
```text
Mini RxJava items count: 15000
Mini RxJava completed: true
Mini RxJava error type: null
Mini RxJava error message: null
Mini RxJava duration (ns): 10039100

RxJava items count: 15000
RxJava completed: true
RxJava error type: null
RxJava error message: null
RxJava duration (ns): 35576800
```

### Stress Metrics: Large map + filter + map pipeline
```text
Mini RxJava items count: 20000
Mini RxJava completed: true
Mini RxJava error type: null
Mini RxJava error message: null
Mini RxJava duration (ns): 9993900

RxJava items count: 20000
RxJava completed: true
RxJava error type: null
RxJava error message: null
RxJava duration (ns): 10576700
```
### Stress Metrics: Scheduler stress scenario

```text
Mini RxJava items count:
Mini RxJava items count: 20000
Mini RxJava completed: true
Mini RxJava error type: null
Mini RxJava error message: null
Mini RxJava first observed thread: mini-rx-single-1
Mini RxJava duration (ns): 139482800

RxJava items count: 20000
RxJava completed: true
RxJava error type: null
RxJava error message: null
RxJava first observed thread: RxSingleScheduler-1
RxJava duration (ns): 93582900
```
---
## Conclusions from Metrics

### Functional correctness
The most important result is that Mini RxJava matched RxJava in all measured scenarios with respect to:
- emitted item count,
- completion behavior,
- error behavior,
- scheduler semantics in the tested cases.

This confirms that the implementation reproduces the intended behavior of RxJava for the implemented feature set.

### Operators
The `map(...)`, `filter(...)`, and `flatMap(...)` operators worked correctly both in basic tests and in larger stress scenarios.

**Examples:**
- `Map + Filter scenario` produced exactly 3333 items in both implementations
- `Large map + filter + map pipeline` produced exactly 20000 items in both implementations
- `FlatMap expansion stress` produced exactly 15000 items in both implementations

### Error handling
The Error scenario showed that Mini RxJava behaves like RxJava when an exception occurs inside a mapper:
- one item was emitted before failure,
- completion did not happen,
- the error type and message matched,
- the stream terminated correctly.

### Scheduler behavior
The Scheduler scenario and Scheduler stress scenario demonstrated that:
- source execution can be moved to an IO thread,
- observer notifications can be moved to a single-thread scheduler,
- thread-switching semantics work as intended in the educational implementation.

### Performance discussion
The timing results show that Mini RxJava had lower execution time than RxJava in several synthetic synchronous scenarios.
However, this should not be interpreted as a general claim that Mini RxJava is faster than RxJava.

A more correct interpretation is:
- Mini RxJava is much simpler,
- it supports a smaller feature set,
- it has fewer internal layers and fewer production-level safeguards,
- therefore it may have lower overhead in small controlled synchronous scenarios.

In contrast, the Scheduler stress scenario showed RxJava performing better in a heavier asynchronous context. This is expected, because RxJava has a more mature and optimized internal implementation for multi-threaded event delivery.

### Main interpretation
The main achievement of the project is semantic correctness, not raw performance superiority.
Mini RxJava successfully reproduces the core behavior of RxJava in the selected educational scenarios.

### Boundary, Robustness, and Concurrency Findings
Additional robustness and race-condition-oriented tests were added.

**Boundary and robustness checks confirmed:**
- empty streams complete correctly
- filters may remove all items without breaking completion
- `flatMap(...)` works with empty inner observables
- null arguments are rejected properly
- expected failure scenarios do not unnecessarily crash the subscription flow outward

**Race-condition-oriented checks confirmed:**
- concurrent inner streams in `flatMap(...)` did not lose items in tested scenarios
- completion was not signaled multiple times in the tested concurrent scenarios
- order was preserved with `observeOn(new SingleThreadScheduler())`
- disposal during concurrent emission stopped the stream without obvious observer corruption

**Important limitation of interpretation:**
These tests increase confidence, but they do not mathematically prove the absence of all race conditions. They are practical stress checks, not a formal proof of thread safety.

### Limitations
This project intentionally remains a simplified educational implementation.

**Current limitations:**
- not all RxJava operators are implemented
- no backpressure support
- no advanced resource management
- no composite-disposable structure for all nested subscriptions
- `observeOn(...)` uses a simple per-signal scheduling model
- concurrency behavior is educational and tested, but not production-grade
- timing results are illustrative and not a formal benchmark

These limitations are acceptable for a course project whose goal is to understand the fundamentals of reactive programming and to build a working educational model.

---

## Final Conclusion

As a result of this course project, I created a working educational Mini RxJava library that implements the main ideas of reactive programming:
- Observer pattern
- Observable creation
- stream transformation with `map(...)`, `filter(...)`, and `flatMap(...)`
- subscription cancellation with `Disposable`
- asynchronous execution with schedulers
- execution context switching with `subscribeOn(...)` and `observeOn(...)`
- error propagation through `onError(...)`

The implementation was validated through unit tests, scheduler tests, robustness tests, race-condition-oriented tests, direct comparison with real RxJava, metrics and stress scenarios.

The final conclusion is that Mini RxJava successfully reproduces the core semantics of RxJava in the implemented scenarios and serves as a clear, readable, and correct educational reactive library.