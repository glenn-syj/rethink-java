# Memory Leaks and Profiling

## Overview

This module provides comprehensive coverage of memory leaks in Java applications, their root causes, and practical approaches to diagnose and resolve them using industry-standard tools like JVisualVM and Eclipse Memory Analyzer (MAT). Understanding memory leaks is crucial for building robust, scalable Java applications that maintain optimal performance over time.

## Table of Contents

1. [Memory Leak Definition](#memory-leak-definition)
2. [Common Memory Leak Patterns](#common-memory-leak-patterns)
3. [Reference Types and GC Behavior](#reference-types-and-gc-behavior)
4. [Diagnostic Tools and Techniques](#diagnostic-tools-and-techniques)
5. [Memory Profiling Best Practices](#memory-profiling-best-practices)
6. [Demo Code Explanations](#demo-code-explanations)

## Memory Leak Definition

### What is a Memory Leak in Java?

A **memory leak** in Java occurs when objects that are no longer needed by the application continue to occupy heap memory and cannot be garbage collected by the JVM. Unlike memory leaks in languages with manual memory management (C/C++), Java memory leaks are logical leaks where objects remain reachable but unused.

### Why Do Memory Leaks Occur Despite GC?

Java's Garbage Collector (GC) only reclaims objects that are **unreachable** from any root reference. Memory leaks happen when:

1. **Unexpected Reachability**: Objects that should be garbage collected remain reachable through unintended references
2. **Static Field Retention**: Static fields maintain references throughout the application lifecycle
3. **Collection Accumulation**: Collections grow indefinitely without proper cleanup mechanisms
4. **Resource Management**: External resources aren't properly released, keeping related Java objects alive
5. **ClassLoader Issues**: Outdated class loaders prevent proper cleanup in application servers

**Key Point**: GC is not a magic solution - it only reclaims truly unreachable objects. Memory leaks are essentially "logical bugs" where reachability is maintained unintentionally.

## Common Memory Leak Patterns

### 1. Collection-Based Leaks

**Pattern**: Objects added to collections (ArrayList, HashMap, etc.) are never removed, causing indefinite accumulation.

**Common Scenarios**:

- **Unbounded Caches**: Cache implementations without size limits or expiration policies
- **Event Listeners**: Listeners registered but never unregistered
- **Session Management**: User sessions stored in collections but not cleaned up
- **Configuration Storage**: Configuration objects accumulated without cleanup

**Example**:

```java
public class BadCache {
    private static Map<String, MyObject> cache = new HashMap<>();

    public static void put(String key, MyObject value) {
        cache.put(key, value); // Objects added but never removed
    }

    // Missing: remove() method and cache size management
}
```

**Diagnosis**: MAT shows specific collection objects consuming unexpectedly large memory. Path to GC Roots reveals collection as the retention point.

**Solution**: Implement size limits, expiration policies, or use WeakHashMap for automatic cleanup.

### 2. Static Field References

**Pattern**: Static fields holding references to objects prevent GC, as static fields exist for the application's entire lifecycle.

**Common Scenarios**:

- **Global Caches**: Static collections used as application-wide caches
- **Singleton Implementations**: Improper singleton patterns holding large object graphs
- **Utility Classes**: Static fields storing temporary or session-specific data
- **Logger References**: Static logger references to large objects

**Example**:

```java
public class GlobalDataManager {
    private static List<LargeObject> permanentList = new ArrayList<>(); // Static retention

    public static void addData(LargeObject obj) {
        permanentList.add(obj); // Objects never released
    }
}
```

**Diagnosis**: MAT's Path to GC Roots shows static field as the retention root.

**Solution**: Use WeakReference, implement cleanup methods, or avoid static fields for temporary data.

### 3. External Resource Leaks

**Pattern**: External resources (files, database connections, network sockets) not properly closed keep related Java objects alive.

**Common Scenarios**:

- **Database Connections**: JDBC connections, statements, result sets not closed
- **File Handles**: InputStream/OutputStream not properly closed
- **Network Resources**: Socket connections not terminated
- **Native Resources**: JNI resources not released

**Example**:

```java
public void processFile(String filename) {
    FileInputStream fis = new FileInputStream(filename);
    // Process file...
    // Missing: fis.close(); - Resource leak!
}
```

**Diagnosis**: OS-level monitoring shows increasing file handles or connections. Heap dumps reveal resource objects accumulating.

**Solution**: Use try-with-resources or ensure proper cleanup in finally blocks.

### 4. Anonymous/Inner Class References

**Pattern**: Anonymous classes and non-static inner classes implicitly hold references to their enclosing class instances.

**Common Scenarios**:

- **Event Handlers**: UI event listeners holding outer class references
- **Thread Implementations**: Inner classes in long-running threads
- **Callback Implementations**: Callbacks holding unnecessary outer references
- **Observer Patterns**: Observer implementations with implicit outer references

**Example**:

```java
public class DataProcessor {
    private LargeObject data = new LargeObject();

    public void startProcessing() {
        // Anonymous class holds implicit reference to DataProcessor instance
        new Thread(new Runnable() {
            public void run() {
                // This thread keeps DataProcessor alive even when not needed
                processData(data);
            }
        }).start();
    }
}
```

**Diagnosis**: MAT shows inner class instances unexpectedly retaining outer class objects.

**Solution**: Use static inner classes or WeakReference to break implicit references.

### 5. ClassLoader Leaks (Advanced)

**Pattern**: Outdated class loaders in application servers prevent proper cleanup during redeployment.

**Common Scenarios**:

- **Web Application Redeployment**: Old class loaders not properly cleaned up
- **Plugin Systems**: Plugin class loaders retaining references
- **ThreadLocal Variables**: ThreadLocal not cleaned up during application shutdown
- **Static Initialization**: Static blocks holding class loader references

**Example**:

```java
public class ProblematicClass {
    private static final ThreadLocal<SomeObject> threadLocal = new ThreadLocal<>();

    // If not properly cleaned up during application shutdown,
    // this can prevent class loader cleanup
}
```

**Diagnosis**: MAT's classloader_leaks query or multiple class loaders for same classes.

**Solution**: Proper cleanup of ThreadLocal, static fields, and shutdown hooks during application lifecycle.

## Reference Types and GC Behavior

Understanding Java's reference types is crucial for preventing memory leaks and optimizing memory usage.

### Strong References (Default)

**Definition**: Standard object references that prevent GC as long as they exist.

```java
Object obj = new Object(); // Strong reference
```

**GC Behavior**: Objects with strong references are **never** garbage collected.

**Memory Leak Risk**: **Highest** - most memory leaks are caused by unintended strong references.

### Soft References

**Definition**: References that allow GC when memory is low but retain objects when memory is sufficient.

```java
SoftReference<MyObject> softRef = new SoftReference<>(new MyObject());
```

**GC Behavior**: Objects are collected only when memory pressure exists.

**Use Cases**:

- Cache implementations that should survive minor memory pressure
- Large object caches that can be sacrificed under memory stress

**Memory Leak Risk**: **Low** - objects are automatically collected under memory pressure.

### Weak References

**Definition**: References that allow immediate GC collection in the next GC cycle.

```java
WeakReference<MyObject> weakRef = new WeakReference<>(new MyObject());
```

**GC Behavior**: Objects are collected immediately if no strong references exist.

**Use Cases**:

- Metadata caches
- Normalized string maps
- Listener/callback implementations
- `WeakHashMap` for automatic cleanup

**Memory Leak Risk**: **Very Low** - objects are collected as soon as strong references are removed.

### Phantom References

**Definition**: The weakest reference type, used for cleanup notifications.

```java
PhantomReference<MyObject> phantomRef = new PhantomReference<>(new MyObject(), referenceQueue);
```

**GC Behavior**: Objects are collected, but phantom references are queued before final memory cleanup.

**Use Cases**:

- Safe alternative to finalizers
- External resource cleanup
- Object death notifications

**Memory Leak Risk**: **None** - objects are always collected, references only provide notifications.

## Diagnostic Tools and Techniques

### 1. JVisualVM

**Purpose**: Real-time monitoring and profiling of running Java applications.

**Key Features**:

- **Memory Tab**: Real-time heap usage monitoring
- **Profiler**: CPU and memory profiling capabilities
- **Threads**: Thread analysis and deadlock detection
- **Sampler**: Memory allocation sampling

**Usage for Memory Leak Detection**:

1. Connect to running application
2. Monitor heap usage over time
3. Use Profiler to identify memory allocation patterns
4. Analyze object retention in heap dumps

### 2. Eclipse Memory Analyzer (MAT)

**Purpose**: Deep analysis of heap dumps for memory leak identification.

**Key Features**:

- **Leak Suspects Report**: Automated leak detection
- **Dominator Tree**: Object retention hierarchy
- **Path to GC Roots**: Reference chain analysis
- **Histogram**: Object count and size analysis

**MAT Analysis Workflow**:

1. Generate heap dump using `jmap` or JVisualVM
2. Open dump in MAT
3. Review Leak Suspects report
4. Use Dominator Tree to identify large object retainers
5. Follow Path to GC Roots to find retention sources

### 3. Command-Line Tools

**jmap**: Generate heap dumps and memory statistics

```bash
jmap -dump:format=b,file=heap.hprof <pid>
jmap -histo <pid>  # Object histogram
```

**jstat**: Monitor GC activity and memory usage

```bash
jstat -gc <pid> 250ms  # GC statistics every 250ms
jstat -gcutil <pid>    # GC utilization percentages
```

**jstack**: Thread analysis and deadlock detection

```bash
jstack <pid>  # Thread dump
```

### 4. Heap Dump Analysis Techniques

**Step-by-Step Process**:

1. **Baseline**: Take initial heap dump when application starts
2. **Monitor**: Track heap growth over time using JVisualVM
3. **Capture**: Take heap dump when memory usage is high
4. **Compare**: Use MAT to compare dumps and identify growing objects
5. **Analyze**: Trace Path to GC Roots to find retention sources
6. **Validate**: Fix suspected leaks and verify with new dumps

## Memory Profiling Best Practices

### 1. Proactive Monitoring

- **Set up continuous monitoring** for heap usage trends
- **Establish memory baselines** for normal application behavior
- **Monitor GC frequency and duration** for performance impact
- **Use alerting** for unusual memory growth patterns

### 2. Regular Analysis

- **Schedule periodic heap dump analysis** for production applications
- **Profile during different load conditions** to identify load-specific leaks
- **Analyze memory usage after application updates** to detect new leaks
- **Document memory usage patterns** for future reference

### 3. Development Practices

- **Use try-with-resources** for all Closeable resources
- **Implement proper cleanup methods** for long-lived objects
- **Avoid static fields for temporary data**
- **Use appropriate reference types** (WeakReference for caches)
- **Test with realistic data volumes** to identify scaling issues

### 4. Testing Strategies

- **Load testing**: Run applications under sustained load to identify gradual leaks
- **Memory stress testing**: Use tools to force memory pressure and test cleanup
- **Long-running tests**: Execute applications for extended periods to detect slow leaks
- **Resource exhaustion testing**: Test behavior under resource constraints

## Demo Code Explanations

### MemoryLeakDemo.java

**Purpose**: Demonstrates a classic collection-based memory leak.

**Pattern Simulated**: Unbounded collection growth without cleanup mechanisms.

**Key Learning Points**:

- How collections can grow indefinitely
- Impact of static references on object retention
- Memory growth patterns over time

**Monitoring Approach**:

1. Run with JVisualVM connected
2. Observe heap growth in Memory tab
3. Generate heap dump when memory usage is high
4. Analyze with MAT to identify collection as retention source

### ResourceLeakDemo.java

**Purpose**: Shows external resource leaks and proper cleanup techniques.

**Pattern Simulated**: File handles and database connections not properly closed.

**Key Learning Points**:

- Importance of resource cleanup
- try-with-resources vs manual cleanup
- OS-level resource monitoring

**Monitoring Approach**:

1. Monitor file handle count using OS tools
2. Use JVisualVM to track resource object creation
3. Compare memory usage with and without proper cleanup

### ReferenceTypeDemo.java

**Purpose**: Demonstrates different reference types and their GC behavior.

**Pattern Simulated**: Strong, weak, and soft reference behavior under memory pressure.

**Key Learning Points**:

- Reference type impact on GC
- When to use different reference types
- Memory pressure effects on reference behavior

**Monitoring Approach**:

1. Force memory pressure using large object allocation
2. Observe GC behavior for different reference types
3. Analyze object retention patterns in heap dumps

## Learning Path Recommendations

### Beginner Level

1. **Understand basic memory leak concepts** using MemoryLeakDemo
2. **Learn JVisualVM basics** for real-time monitoring
3. **Practice heap dump generation** using jmap
4. **Study reference types** with ReferenceTypeDemo

- Use `-Xmx16m` option with `java ...` to observe `SoftReference` objects get garbage collected.

### Intermediate Level

1. **Master MAT analysis** for complex leak detection
2. **Implement proper resource management** patterns
3. **Study classloader leaks** in application server environments
4. **Practice performance profiling** under different load conditions

### Advanced Level

1. **Design leak-resistant architectures** using appropriate reference types
2. **Implement custom memory monitoring** solutions
3. **Optimize GC behavior** based on memory profiling results
4. **Develop memory leak detection tools** for specific application domains

## Conclusion

Memory leaks in Java are preventable with proper understanding of reference types, careful resource management, and systematic monitoring practices. This module provides the foundation for identifying, diagnosing, and resolving memory leaks using industry-standard tools and techniques. Regular profiling and analysis are essential for maintaining optimal application performance and preventing production issues.

Remember: **Prevention is better than cure** - design applications with memory management in mind from the beginning, and establish monitoring practices that catch leaks early in the development lifecycle.
