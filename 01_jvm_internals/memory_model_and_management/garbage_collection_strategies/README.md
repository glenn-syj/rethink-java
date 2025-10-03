# Garbage Collection Strategies

## Overview

This module provides an in-depth review of major Java Virtual Machine (JVM) Garbage Collection (GC) algorithms. It covers their working principles, key characteristics, Stop-The-World (STW) issues, and effective tuning techniques. Through hands-on demonstrations and guided explanations, users will gain a comprehensive understanding of how different GC strategies impact application performance and memory management.

## 1. Major GC Algorithms Explained

### 1.1 Serial GC

- **Working Principle**: A "stop-the-world" collector that uses a single thread for all GC work. It marks live objects and then sweeps away dead ones. For the young generation, it uses a copying algorithm; for the old generation, it uses a mark-sweep-compact algorithm.
- **Key Characteristics & Goal**: Simple, efficient for small heap sizes (up to 100MB). Focuses on maximizing throughput by dedicating all CPU resources to GC.
- **STW Issues**: High STW pauses, as the entire application halts during both minor and major collections.
- **Suitable Workloads**: Client-side applications, single-processor machines, or applications with very small heaps where GC pauses are acceptable.

### 1.2 Parallel GC (Throughput Collector)

- **Working Principle**: Uses multiple threads to perform young and old generation collections. Similar to Serial GC in its algorithms (copying for young, mark-sweep-compact for old) but parallelizes the work.
- **Key Characteristics & Goal**: Designed for high throughput. It can utilize multiple CPU cores to speed up GC, making it suitable for applications that can tolerate occasional pauses for GC.
- **STW Issues**: While collections are parallelized, they are still "stop-the-world" events, leading to potentially long pauses on large heaps or with complex old generation collections.
- **Suitable Workloads**: Batch processing, applications with large data sets, and server-side applications where throughput is more critical than low latency.

### 1.3 CMS GC (Concurrent Mark-Sweep Collector)

- **Working Principle**: Aims to minimize STW pauses by performing most of its work concurrently with the application threads. It uses a mark-sweep algorithm for the old generation. The "concurrent" phases involve marking reachable objects while the application is running.
- **Key Characteristics & Goal**: Low latency collector, designed for applications requiring minimal GC pauses.
- **STW Issues**: Significantly shorter STW pauses compared to Serial and Parallel GC, primarily during initial mark and remark phases. However, it can suffer from "concurrent mode failure" if the old generation fills up before concurrent collection finishes, leading to a full STW collection.
- **Suitable Workloads**: Web servers, interactive applications, and other systems where responsiveness is a high priority.

### 1.4 G1 GC (Garbage-First Collector)

- **Working Principle**: A generational, concurrent, compacting collector. It divides the heap into a set of equal-sized regions. It prioritizes collecting regions with the most garbage first (hence "Garbage-First"). It operates in phases, some concurrent, some STW.
- **Key Characteristics & Goal**: Aims to balance throughput and latency. It allows users to specify a maximum pause time goal. It's designed to be a "server-style" collector for multiprocessor machines with large memories.
- **STW Issues**: Offers more predictable pause times than CMS, as it collects small regions rather than the entire heap. Most of its work is concurrent, but marking and evacuation still incur STW pauses.
- **Suitable Workloads**: Large heaps (4GB+), applications that need more predictable GC pauses, and a good default choice for general-purpose servers.

### 1.5 ZGC (Z Garbage Collector)

- **Working Principle**: A concurrent, single-generation, region-based, compacting collector. ZGC performs all expensive GC operations concurrently without stopping application threads for more than 10ms (typically much less). It uses colored pointers and load barriers to achieve extremely low pause times.
- **Key Characteristics & Goal**: Extremely low latency collector, designed for very large heaps (terabytes) and applications requiring consistent, short GC pauses.
- **STW Issues**: Designed for maximum pause times of less than 10ms, regardless of heap size. Achieves this by doing almost all work concurrently.
- **Suitable Workloads**: High-performance computing, databases, real-time trading systems, and other latency-sensitive applications with very large heaps.

### 1.6 Shenandoah GC

- **Working Principle**: Another concurrent, single-generation, region-based, compacting collector aiming for ultra-low pause times, similar to ZGC. It uses a different mechanism involving Brooks pointers and forwarding pointers to achieve concurrency.
- **Key Characteristics & Goal**: Similar to ZGC, Shenandoah's primary goal is to minimize GC pause times, often achieving pauses under 10ms, even on large heaps.
- **STW Issues**: Like ZGC, it significantly reduces STW pauses by performing most work concurrently with application threads.
- **Suitable Workloads**: Latency-sensitive applications with large heaps, often seen as an alternative to ZGC.

## 2. Major JVM GC Options and Tuning Techniques

JVM options are crucial for selecting and tuning GC algorithms to suit specific application needs.

### 2.1 GC Activation Options

- `-XX:+UseSerialGC`: Enables the Serial GC.
- `-XX:+UseParallelGC`: Enables the Parallel GC.
- `-XX:+UseParallelOldGC`: Use with `-XX:+UseParallelGC` for parallel old generation collection.
- `-XX:+UseConcMarkSweepGC`: Enables the CMS GC (deprecated in JDK 9, removed in JDK 14).
- `-XX:+UseG1GC`: Enables the G1 GC (default since JDK 9).
- `-XX:+UseZGC`: Enables the ZGC (JDK 11+).
- `-XX:+UseShenandoahGC`: Enables the Shenandoah GC (requires specific OpenJDK builds).

### 2.2 General Memory Tuning Options

- `-Xms<size>`: Sets the initial Java heap size (e.g., `-Xms256m`).
- `-Xmx<size>`: Sets the maximum Java heap size (e.g., `-Xmx1g`).
- `-Xmn<size>`: Sets the initial and maximum size of the young generation (e.g., `-Xmn128m`). (Less relevant for G1/ZGC/Shenandoah as they manage regions).
- `-XX:MaxMetaspaceSize=<size>`: Sets the maximum Metaspace size (e.g., `-XX:MaxMetaspaceSize=256m`).
- `-Xss<size>`: Sets the thread stack size (e.g., `-Xss256k`).

### 2.3 GC-Specific Tuning Options

- **Parallel GC**:
  - `-XX:ParallelGCThreads=<N>`: Sets the number of GC threads for parallel collection.
- **G1 GC**:
  - `-XX:MaxGCPauseMillis=<N>`: Sets a target for the maximum GC pause time (G1 will try to meet this, but it's not a strict guarantee).
  - `-XX:G1HeapRegionSize=<N>`: Sets the size of the G1 regions (e.g., `1M`, `2M`, `4M` etc.).
- **CMS GC**:
  - `-XX:CMSInitiatingOccupancyFraction=<percent>`: Sets the percentage of the old generation occupancy at which CMS GC should start.
  - `-XX:+UseCMSCompactAtFullCollection`: Enables compaction of the old generation during a full GC (can cause longer pauses).
- **ZGC/Shenandoah**:
  - These collectors are designed to be largely self-tuning, with minimal user-facing options beyond heap size.

## 3. GC Log Analysis Methods

Understanding GC behavior is crucial for performance tuning. GC logs provide detailed insights into memory management.

### 3.1 Enabling GC Logs

- **JDK 9 and later**: Use `-Xlog:gc*` for detailed GC logging. You can specify verbosity and output file:
  - `-Xlog:gc=info`: Basic GC information.
  - `-Xlog:gc*=debug`: Very detailed GC information.
  - `-Xlog:gc*=info:file=gc_log.txt`: Output GC logs to a file.
- **Prior to JDK 9**:
  - `-XX:+PrintGCDetails`: Prints detailed information about GC events.
  - `-XX:+PrintGCDateStamps`: Adds timestamps with dates to GC log entries.
  - `-XX:+PrintGCTimeStamps`: Adds elapsed time from JVM start to GC log entries.
  - `-Xloggc:<file_path>`: Redirects GC output to a specified file.

### 3.2 Interpreting Key GC Log Entries

GC logs typically show:

- **Heap Usage Before/After GC**: `[PSYoungGen: 7288K->1024K(9216K)] 7288K->1024K(20480K)` (Young Gen usage change, total heap usage change)
- **GC Duration**: `0.0053420 secs` (Time taken for the GC event).
- **GC Type**: `[GC (Allocation Failure)]` (Minor GC due to failed allocation), `[Full GC (System.gc())]` (Full GC triggered by `System.gc()`).
- **Memory Pool Changes**: `Eden space`, `From space`, `To space`, `Old space`, `Metaspace` usage.
- **Pause Times**: Often denoted by `user`, `sys`, `real` times. `real` time indicates the actual STW pause duration.

### 3.3 GC Log Analysis Tools

- **GCViewer**: A free, open-source tool that visualizes GC logs, making it easier to spot trends, pause times, and memory usage patterns.
- **GCEasy**: An online tool that uploads your GC logs and provides a comprehensive report with recommendations.

## 4. Provided Demo Codes Explanation

This module includes two java files to help observe different GC scenarios. The `run_gc_demos.sh` script, located in this directory, can be used to compile and run these demos with various GC algorithms and generate log files for analysis.

Before running, ensure you have executable permissions (`chmod +x run_gc_demos.sh`) and execute it from the project root directory (`rethink-java/`). If the file is not processed, check `DEMO_DIR` varaible and the execution path.

To select which GC algorithms to run, modify the control variables (`RUN_SERIAL_GC`, `RUN_PARALLEL_GC`, `RUN_G1_GC`, `RUN_ZGC`) at the top of the `run_gc_demos.sh` script.

### 4.1 `ShortLivedObjectDemo.java`

- **Simulated Memory Allocation Pattern**: This demo continuously allocates a large number of small, short-lived objects (byte arrays) in a loop. It aims to fill up the Young Generation (Eden space) quickly.
- **Observable GC Scenarios**: Primarily designed to observe frequent **Minor GCs (Young GCs)**. You will see the Eden space rapidly filling up and then being cleared by the GC. Objects that survive multiple Minor GCs might be promoted to the Survivor spaces or eventually to the Old Generation.
- **Recommended Execution**: Execute via `run_gc_demos.sh` by setting `RUN_<GC_ALGORITHM>_GC="true"` for the desired GC(s).

### 4.2 `LongLivedObjectDemo.java`

- **Simulated Memory Allocation Pattern**: This demo first allocates a set of objects that are held by a static reference, ensuring they live for a long time and are likely to be promoted to the Old Generation. Subsequently, it also generates short-lived objects to induce Minor GCs, similar to `ShortLivedObjectDemo`. Additionally, it periodically adds more long-lived objects to further stress the Old Generation.
- **Observable GC Scenarios**: Designed to observe **object promotion** from Young to Old Generation, and eventually **Major GCs (Full GCs)** when the Old Generation becomes full. This demo is excellent for understanding STW pauses associated with Full GCs (especially with Serial/Parallel GC).
- **Recommended Execution**: Execute via `run_gc_demos.sh` by setting `RUN_<GC_ALGORITHM>_GC="true"` for the desired GC(s).

### 4.3 `run_gc_demo.sh`

- `shortelived_g1_gc.log`

```shell
[0.018s][info][gc] Using G1
[0.023s][info][gc,init] Version: 17.0.10+13-LTS (release)
[0.023s][info][gc,init] CPUs: 12 total, 12 available
[0.023s][info][gc,init] Memory: 7505M
[0.023s][info][gc,init] Large Page Support: Disabled
[0.023s][info][gc,init] NUMA Support: Disabled
[0.023s][info][gc,init] Compressed Oops: Enabled (32-bit)
[0.023s][info][gc,init] Heap Region Size: 1M
[0.023s][info][gc,init] Heap Min Capacity: 256M
[0.023s][info][gc,init] Heap Initial Capacity: 256M
[0.023s][info][gc,init] Heap Max Capacity: 256M
[0.023s][info][gc,init] Pre-touch: Disabled
[0.023s][info][gc,init] Parallel Workers: 10
[0.024s][info][gc,init] Concurrent Workers: 3
[0.024s][info][gc,init] Concurrent Refinement Workers: 10
[0.024s][info][gc,init] Periodic GC: Disabled
[0.045s][info][gc,metaspace] CDS archive(s) mapped at: [0x0000022c1d000000-0x0000022c1dbb0000-0x0000022c1dbb0000), size 12255232, SharedBaseAddress: 0x0000022c1d000000, ArchiveRelocationMode: 1.
[0.045s][info][gc,metaspace] Compressed class space mapped at: 0x0000022c1e000000-0x0000022c5e000000, reserved size: 1073741824
[0.045s][info][gc,metaspace] Narrow klass base: 0x0000022c1d000000, Narrow klass shift: 0, Narrow klass range: 0x100000000
[0.138s][info][gc,start    ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[0.138s][info][gc,task     ] GC(0) Using 6 workers of 10 for evacuation
[0.159s][info][gc,phases   ] GC(0)   Pre Evacuate Collection Set: 0.2ms
[0.160s][info][gc,phases   ] GC(0)   Merge Heap Roots: 0.1ms
[0.160s][info][gc,phases   ] GC(0)   Evacuate Collection Set: 20.0ms
[0.160s][info][gc,phases   ] GC(0)   Post Evacuate Collection Set: 0.5ms
[0.160s][info][gc,phases   ] GC(0)   Other: 1.0ms
[0.160s][info][gc,heap     ] GC(0) Eden regions: 23->0(14)
[0.160s][info][gc,heap     ] GC(0) Survivor regions: 0->3(3)
[0.160s][info][gc,heap     ] GC(0) Old regions: 0->19
[0.160s][info][gc,heap     ] GC(0) Archive regions: 0->0
[0.160s][info][gc,heap     ] GC(0) Humongous regions: 0->0
[0.160s][info][gc,metaspace] GC(0) Metaspace: 185K(384K)->185K(384K) NonClass: 175K(256K)->175K(256K) Class: 9K(128K)->9K(128K)
[0.160s][info][gc          ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 22M->21M(256M) 22.079ms
[0.160s][info][gc,cpu      ] GC(0) User=0.00s Sys=0.09s Real=0.02s
[0.168s][info][gc,start    ] GC(1) Pause Young (Normal) (G1 Evacuation Pause)
[0.169s][info][gc,task     ] GC(1) Using 6 workers of 10 for evacuation
[0.182s][info][gc,phases   ] GC(1)   Pre Evacuate Collection Set: 0.1ms
[0.182s][info][gc,phases   ] GC(1)   Merge Heap Roots: 0.1ms
[0.182s][info][gc,phases   ] GC(1)   Evacuate Collection Set: 12.9ms
[0.182s][info][gc,phases   ] GC(1)   Post Evacuate Collection Set: 0.5ms
[0.183s][info][gc,phases   ] GC(1)   Other: 0.4ms
[0.183s][info][gc,heap     ] GC(1) Eden regions: 14->0(21)
[0.183s][info][gc,heap     ] GC(1) Survivor regions: 3->3(3)
[0.183s][info][gc,heap     ] GC(1) Old regions: 19->33
[0.183s][info][gc,heap     ] GC(1) Archive regions: 0->0
[0.183s][info][gc,heap     ] GC(1) Humongous regions: 0->0
[0.183s][info][gc,metaspace] GC(1) Metaspace: 185K(384K)->185K(384K) NonClass: 175K(256K)->175K(256K) Class: 9K(128K)->9K(128K)
[0.183s][info][gc          ] GC(1) Pause Young (Normal) (G1 Evacuation Pause) 35M->35M(256M) 14.349ms
[0.183s][info][gc,cpu      ] GC(1) User=0.02s Sys=0.00s Real=0.01s
[0.300s][info][gc,start    ] GC(2) Pause Young (Normal) (G1 Evacuation Pause)
[0.300s][info][gc,task     ] GC(2) Using 6 workers of 10 for evacuation
[0.321s][info][gc,phases   ] GC(2)   Pre Evacuate Collection Set: 0.1ms
[0.321s][info][gc,phases   ] GC(2)   Merge Heap Roots: 0.1ms
[0.321s][info][gc,phases   ] GC(2)   Evacuate Collection Set: 20.3ms
[0.322s][info][gc,phases   ] GC(2)   Post Evacuate Collection Set: 0.5ms
[0.322s][info][gc,phases   ] GC(2)   Other: 0.3ms
[0.322s][info][gc,heap     ] GC(2) Eden regions: 21->0(29)
[0.322s][info][gc,heap     ] GC(2) Survivor regions: 3->3(3)
[0.322s][info][gc,heap     ] GC(2) Old regions: 33->54
[0.322s][info][gc,heap     ] GC(2) Archive regions: 0->0
[0.322s][info][gc,heap     ] GC(2) Humongous regions: 0->0
[0.322s][info][gc,metaspace] GC(2) Metaspace: 278K(448K)->278K(448K) NonClass: 265K(320K)->265K(320K) Class: 13K(128K)->13K(128K)
[0.322s][info][gc          ] GC(2) Pause Young (Normal) (G1 Evacuation Pause) 56M->56M(256M) 21.766ms
[0.322s][info][gc,cpu      ] GC(2) User=0.02s Sys=0.08s Real=0.02s
[0.760s][info][gc,start    ] GC(3) Pause Young (Normal) (G1 Evacuation Pause)
[0.760s][info][gc,task     ] GC(3) Using 6 workers of 10 for evacuation
[0.768s][info][gc,phases   ] GC(3)   Pre Evacuate Collection Set: 0.1ms
[0.768s][info][gc,phases   ] GC(3)   Merge Heap Roots: 0.1ms
[0.768s][info][gc,phases   ] GC(3)   Evacuate Collection Set: 6.8ms
[0.768s][info][gc,phases   ] GC(3)   Post Evacuate Collection Set: 0.4ms
[0.768s][info][gc,phases   ] GC(3)   Other: 0.5ms
[0.768s][info][gc,heap     ] GC(3) Eden regions: 29->0(123)
[0.768s][info][gc,heap     ] GC(3) Survivor regions: 3->4(4)
[0.768s][info][gc,heap     ] GC(3) Old regions: 54->56
[0.768s][info][gc,heap     ] GC(3) Archive regions: 0->0
[0.768s][info][gc,heap     ] GC(3) Humongous regions: 0->0
[0.768s][info][gc,metaspace] GC(3) Metaspace: 278K(448K)->278K(448K) NonClass: 265K(320K)->265K(320K) Class: 13K(128K)->13K(128K)
[0.768s][info][gc          ] GC(3) Pause Young (Normal) (G1 Evacuation Pause) 85M->59M(256M) 8.261ms
[0.769s][info][gc,cpu      ] GC(3) User=0.03s Sys=0.06s Real=0.01s
[2.732s][info][gc,start    ] GC(4) Pause Young (Normal) (G1 Evacuation Pause)
[2.732s][info][gc,task     ] GC(4) Using 6 workers of 10 for evacuation
[2.738s][info][gc,phases   ] GC(4)   Pre Evacuate Collection Set: 0.1ms
[2.738s][info][gc,phases   ] GC(4)   Merge Heap Roots: 0.1ms
[2.738s][info][gc,phases   ] GC(4)   Evacuate Collection Set: 5.0ms
[2.738s][info][gc,phases   ] GC(4)   Post Evacuate Collection Set: 0.4ms
[2.738s][info][gc,phases   ] GC(4)   Other: 0.3ms
[2.738s][info][gc,heap     ] GC(4) Eden regions: 123->0(127)
[2.738s][info][gc,heap     ] GC(4) Survivor regions: 4->7(16)
[2.738s][info][gc,heap     ] GC(4) Old regions: 56->56
[2.738s][info][gc,heap     ] GC(4) Archive regions: 0->0
[2.738s][info][gc,heap     ] GC(4) Humongous regions: 0->0
[2.738s][info][gc,metaspace] GC(4) Metaspace: 293K(512K)->293K(512K) NonClass: 280K(384K)->280K(384K) Class: 13K(128K)->13K(128K)
[2.738s][info][gc          ] GC(4) Pause Young (Normal) (G1 Evacuation Pause) 182M->61M(256M) 6.033ms
[2.738s][info][gc,cpu      ] GC(4) User=0.00s Sys=0.00s Real=0.01s
[4.712s][info][gc,start    ] GC(5) Pause Young (Normal) (G1 Evacuation Pause)
[4.712s][info][gc,task     ] GC(5) Using 6 workers of 10 for evacuation
[4.716s][info][gc,phases   ] GC(5)   Pre Evacuate Collection Set: 0.1ms
[4.716s][info][gc,phases   ] GC(5)   Merge Heap Roots: 0.1ms
[4.716s][info][gc,phases   ] GC(5)   Evacuate Collection Set: 2.7ms
[4.716s][info][gc,phases   ] GC(5)   Post Evacuate Collection Set: 0.8ms
[4.717s][info][gc,phases   ] GC(5)   Other: 0.4ms
[4.717s][info][gc,heap     ] GC(5) Eden regions: 127->0(139)
[4.717s][info][gc,heap     ] GC(5) Survivor regions: 7->2(17)
[4.717s][info][gc,heap     ] GC(5) Old regions: 56->56
[4.717s][info][gc,heap     ] GC(5) Archive regions: 0->0
[4.717s][info][gc,heap     ] GC(5) Humongous regions: 0->0
[4.717s][info][gc,metaspace] GC(5) Metaspace: 293K(512K)->293K(512K) NonClass: 280K(384K)->280K(384K) Class: 13K(128K)->13K(128K)
[4.717s][info][gc          ] GC(5) Pause Young (Normal) (G1 Evacuation Pause) 188M->56M(256M) 4.431ms
[4.717s][info][gc,cpu      ] GC(5) User=0.03s Sys=0.00s Real=0.01s
[6.892s][info][gc,start    ] GC(6) Pause Young (Normal) (G1 Evacuation Pause)
[6.892s][info][gc,task     ] GC(6) Using 6 workers of 10 for evacuation
[6.903s][info][gc,phases   ] GC(6)   Pre Evacuate Collection Set: 0.2ms
[6.903s][info][gc,phases   ] GC(6)   Merge Heap Roots: 0.1ms
[6.903s][info][gc,phases   ] GC(6)   Evacuate Collection Set: 9.8ms
[6.903s][info][gc,phases   ] GC(6)   Post Evacuate Collection Set: 0.5ms
[6.903s][info][gc,phases   ] GC(6)   Other: 0.5ms
[6.903s][info][gc,heap     ] GC(6) Eden regions: 139->0(132)
[6.903s][info][gc,heap     ] GC(6) Survivor regions: 2->10(18)
[6.903s][info][gc,heap     ] GC(6) Old regions: 56->56
[6.903s][info][gc,heap     ] GC(6) Archive regions: 0->0
[6.903s][info][gc,heap     ] GC(6) Humongous regions: 0->0
[6.903s][info][gc,metaspace] GC(6) Metaspace: 293K(512K)->293K(512K) NonClass: 280K(384K)->280K(384K) Class: 13K(128K)->13K(128K)
[6.904s][info][gc          ] GC(6) Pause Young (Normal) (G1 Evacuation Pause) 195M->64M(256M) 11.482ms
[6.904s][info][gc,cpu      ] GC(6) User=0.03s Sys=0.02s Real=0.01s
[8.964s][info][gc,start    ] GC(7) Pause Young (Normal) (G1 Evacuation Pause)
[8.964s][info][gc,task     ] GC(7) Using 6 workers of 10 for evacuation
[8.969s][info][gc,phases   ] GC(7)   Pre Evacuate Collection Set: 0.1ms
[8.969s][info][gc,phases   ] GC(7)   Merge Heap Roots: 0.1ms
[8.969s][info][gc,phases   ] GC(7)   Evacuate Collection Set: 4.1ms
[8.969s][info][gc,phases   ] GC(7)   Post Evacuate Collection Set: 0.5ms
[8.969s][info][gc,phases   ] GC(7)   Other: 0.3ms
[8.969s][info][gc,heap     ] GC(7) Eden regions: 132->0(133)
[8.969s][info][gc,heap     ] GC(7) Survivor regions: 10->10(18)
[8.969s][info][gc,heap     ] GC(7) Old regions: 56->56
[8.969s][info][gc,heap     ] GC(7) Archive regions: 0->0
[8.969s][info][gc,heap     ] GC(7) Humongous regions: 0->0
[8.969s][info][gc,metaspace] GC(7) Metaspace: 293K(512K)->293K(512K) NonClass: 280K(384K)->280K(384K) Class: 13K(128K)->13K(128K)
[8.969s][info][gc          ] GC(7) Pause Young (Normal) (G1 Evacuation Pause) 196M->64M(256M) 5.424ms
[8.969s][info][gc,cpu      ] GC(7) User=0.02s Sys=0.08s Real=0.00s
[11.056s][info][gc,start    ] GC(8) Pause Young (Normal) (G1 Evacuation Pause)
[11.057s][info][gc,task     ] GC(8) Using 6 workers of 10 for evacuation
[11.058s][info][gc,phases   ] GC(8)   Pre Evacuate Collection Set: 0.1ms
[11.058s][info][gc,phases   ] GC(8)   Merge Heap Roots: 0.1ms
[11.058s][info][gc,phases   ] GC(8)   Evacuate Collection Set: 0.6ms
[11.058s][info][gc,phases   ] GC(8)   Post Evacuate Collection Set: 0.5ms
[11.058s][info][gc,phases   ] GC(8)   Other: 0.4ms
[11.058s][info][gc,heap     ] GC(8) Eden regions: 133->0(147)
[11.058s][info][gc,heap     ] GC(8) Survivor regions: 10->2(18)
[11.058s][info][gc,heap     ] GC(8) Old regions: 56->56
[11.058s][info][gc,heap     ] GC(8) Archive regions: 0->0
[11.058s][info][gc,heap     ] GC(8) Humongous regions: 0->0
[11.058s][info][gc,metaspace] GC(8) Metaspace: 293K(512K)->293K(512K) NonClass: 280K(384K)->280K(384K) Class: 13K(128K)->13K(128K)
[11.058s][info][gc          ] GC(8) Pause Young (Normal) (G1 Evacuation Pause) 197M->56M(256M) 2.030ms
[11.058s][info][gc,cpu      ] GC(8) User=0.00s Sys=0.00s Real=0.00s
[13.341s][info][gc,start    ] GC(9) Pause Young (Normal) (G1 Evacuation Pause)
[13.341s][info][gc,task     ] GC(9) Using 6 workers of 10 for evacuation
[13.353s][info][gc,phases   ] GC(9)   Pre Evacuate Collection Set: 0.1ms
[13.353s][info][gc,phases   ] GC(9)   Merge Heap Roots: 0.1ms
[13.353s][info][gc,phases   ] GC(9)   Evacuate Collection Set: 10.6ms
[13.353s][info][gc,phases   ] GC(9)   Post Evacuate Collection Set: 0.9ms
[13.354s][info][gc,phases   ] GC(9)   Other: 0.6ms
[13.354s][info][gc,heap     ] GC(9) Eden regions: 147->0(143)
[13.354s][info][gc,heap     ] GC(9) Survivor regions: 2->7(19)
[13.354s][info][gc,heap     ] GC(9) Old regions: 56->56
[13.354s][info][gc,heap     ] GC(9) Archive regions: 0->0
[13.354s][info][gc,heap     ] GC(9) Humongous regions: 0->0
[13.354s][info][gc,metaspace] GC(9) Metaspace: 293K(512K)->293K(512K) NonClass: 280K(384K)->280K(384K) Class: 13K(128K)->13K(128K)
[13.354s][info][gc          ] GC(9) Pause Young (Normal) (G1 Evacuation Pause) 203M->61M(256M) 12.747ms
[13.354s][info][gc,cpu      ] GC(9) User=0.00s Sys=0.00s Real=0.01s
```

- longlived_g1_gc.log

```shell
[0.018s][info][gc] Using G1
[0.023s][info][gc,init] Version: 17.0.10+13-LTS (release)
[0.023s][info][gc,init] CPUs: 12 total, 12 available
[0.023s][info][gc,init] Memory: 7505M
[0.023s][info][gc,init] Large Page Support: Disabled
[0.023s][info][gc,init] NUMA Support: Disabled
[0.023s][info][gc,init] Compressed Oops: Enabled (32-bit)
[0.023s][info][gc,init] Heap Region Size: 1M
[0.023s][info][gc,init] Heap Min Capacity: 256M
[0.023s][info][gc,init] Heap Initial Capacity: 256M
[0.023s][info][gc,init] Heap Max Capacity: 256M
[0.023s][info][gc,init] Pre-touch: Disabled
[0.023s][info][gc,init] Parallel Workers: 10
[0.024s][info][gc,init] Concurrent Workers: 3
[0.024s][info][gc,init] Concurrent Refinement Workers: 10
[0.024s][info][gc,init] Periodic GC: Disabled
[0.045s][info][gc,metaspace] CDS archive(s) mapped at: [0x0000022c1d000000-0x0000022c1dbb0000-0x0000022c1dbb0000), size 12255232, SharedBaseAddress: 0x0000022c1d000000, ArchiveRelocationMode: 1.
[0.045s][info][gc,metaspace] Compressed class space mapped at: 0x0000022c1e000000-0x0000022c5e000000, reserved size: 1073741824
[0.045s][info][gc,metaspace] Narrow klass base: 0x0000022c1d000000, Narrow klass shift: 0, Narrow klass range: 0x100000000
[0.138s][info][gc,start    ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[0.138s][info][gc,task     ] GC(0) Using 6 workers of 10 for evacuation
[0.159s][info][gc,phases   ] GC(0)   Pre Evacuate Collection Set: 0.2ms
[0.160s][info][gc,phases   ] GC(0)   Merge Heap Roots: 0.1ms
[0.160s][info][gc,phases   ] GC(0)   Evacuate Collection Set: 20.0ms
[0.160s][info][gc,phases   ] GC(0)   Post Evacuate Collection Set: 0.5ms
[0.160s][info][gc,phases   ] GC(0)   Other: 1.0ms
[0.160s][info][gc,heap     ] GC(0) Eden regions: 23->0(14)
[0.160s][info][gc,heap     ] GC(0) Survivor regions: 0->3(3)
[0.160s][info][gc,heap     ] GC(0) Old regions: 0->19
[0.160s][info][gc,heap     ] GC(0) Archive regions: 0->0
[0.160s][info][gc,heap     ] GC(0) Humongous regions: 0->0
[0.160s][info][gc,metaspace] GC(0) Metaspace: 185K(384K)->185K(384K) NonClass: 175K(256K)->175K(256K) Class: 9K(128K)->9K(128K)
[0.160s][info][gc          ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 22M->21M(256M) 22.079ms
[0.160s][info][gc,cpu      ] GC(0) User=0.00s Sys=0.09s Real=0.02s
[0.168s][info][gc,start    ] GC(1) Pause Young (Normal) (G1 Evacuation Pause)
[0.169s][info][gc,task     ] GC(1) Using 6 workers of 10 for evacuation
[0.182s][info][gc,phases   ] GC(1)   Pre Evacuate Collection Set: 0.1ms
[0.182s][info][gc,phases   ] GC(1)   Merge Heap Roots: 0.1ms
[0.182s][info][gc,phases   ] GC(1)   Evacuate Collection Set: 12.9ms
[0.182s][info][gc,phases   ] GC(1)   Post Evacuate Collection Set: 0.5ms
[0.183s][info][gc,phases   ] GC(1)   Other: 0.4ms
[0.183s][info][gc,heap     ] GC(1) Eden regions: 14->0(21)
[0.183s][info][gc,heap     ] GC(1) Survivor regions: 3->3(3)
[0.183s][info][gc,heap     ] GC(1) Old regions: 19->33
[0.183s][info][gc,heap     ] GC(1) Archive regions: 0->0
[0.183s][info][gc,heap     ] GC(1) Humongous regions: 0->0
[0.183s][info][gc,metaspace] GC(1) Metaspace: 185K(384K)->185K(384K) NonClass: 175K(256K)->175K(256K) Class: 9K(128K)->9K(128K)
[0.183s][info][gc          ] GC(1) Pause Young (Normal) (G1 Evacuation Pause) 35M->35M(256M) 14.349ms
[0.183s][info][gc,cpu      ] GC(1) User=0.02s Sys=0.00s Real=0.01s
[0.300s][info][gc,start    ] GC(2) Pause Young (Normal) (G1 Evacuation Pause)
[0.300s][info][gc,task     ] GC(2) Using 6 workers of 10 for evacuation
[0.321s][info][gc,phases   ] GC(2)   Pre Evacuate Collection Set: 0.1ms
[0.321s][info][gc,phases   ] GC(2)   Merge Heap Roots: 0.1ms
[0.321s][info][gc,phases   ] GC(2)   Evacuate Collection Set: 20.3ms
[0.322s][info][gc,phases   ] GC(2)   Post Evacuate Collection Set: 0.5ms
[0.322s][info][gc,phases   ] GC(2)   Other: 0.3ms
[0.322s][info][gc,heap     ] GC(2) Eden regions: 21->0(29)
[0.322s][info][gc,heap     ] GC(2) Survivor regions: 3->3(3)
[0.322s][info][gc,heap     ] GC(2) Old regions: 33->54
[0.322s][info][gc,heap     ] GC(2) Archive regions: 0->0
[0.322s][info][gc,heap     ] GC(2) Humongous regions: 0->0
[0.322s][info][gc,metaspace] GC(2) Metaspace: 278K(448K)->278K(448K) NonClass: 265K(320K)->265K(320K) Class: 13K(128K)->13K(128K)
[0.322s][info][gc          ] GC(2) Pause Young (Normal) (G1 Evacuation Pause) 56M->56M(256M) 21.766ms
[0.322s][info][gc,cpu      ] GC(2) User=0.02s Sys=0.08s Real=0.02s
[0.760s][info][gc,start    ] GC(3) Pause Young (Normal) (G1 Evacuation Pause)
[0.760s][info][gc,task     ] GC(3) Using 6 workers of 10 for evacuation
[0.768s][info][gc,phases   ] GC(3)   Pre Evacuate Collection Set: 0.1ms
[0.768s][info][gc,phases   ] GC(3)   Merge Heap Roots: 0.1ms
[0.768s][info][gc,phases   ] GC(3)   Evacuate Collection Set: 6.8ms
[0.768s][info][gc,phases   ] GC(3)   Post Evacuate Collection Set: 0.4ms
[0.768s][info][gc,phases   ] GC(3)   Other: 0.5ms
[0.768s][info][gc,heap     ] GC(3) Eden regions: 29->0(123)
[0.768s][info][gc,heap     ] GC(3) Survivor regions: 3->4(4)
[0.768s][info][gc,heap     ] GC(3) Old regions: 54->56
[0.768s][info][gc,heap     ] GC(3) Archive regions: 0->0
[0.768s][info][gc,heap     ] GC(3) Humongous regions: 0->0
[0.768s][info][gc,metaspace] GC(3) Metaspace: 278K(448K)->278K(448K) NonClass: 265K(320K)->265K(320K) Class: 13K(128K)->13K(128K)
[0.768s][info][gc          ] GC(3) Pause Young (Normal) (G1 Evacuation Pause) 85M->59M(256M) 8.261ms
[0.769s][info][gc,cpu      ] GC(3) User=0.03s Sys=0.06s Real=0.01s
[2.732s][info][gc,start    ] GC(4) Pause Young (Normal) (G1 Evacuation Pause)
[2.732s][info][gc,task     ] GC(4) Using 6 workers of 10 for evacuation
[2.738s][info][gc,phases   ] GC(4)   Pre Evacuate Collection Set: 0.1ms
[2.738s][info][gc,phases   ] GC(4)   Merge Heap Roots: 0.1ms
[2.738s][info][gc,phases   ] GC(4)   Evacuate Collection Set: 5.0ms
[2.738s][info][gc,phases   ] GC(4)   Post Evacuate Collection Set: 0.4ms
[2.738s][info][gc,phases   ] GC(4)   Other: 0.3ms
[2.738s][info][gc,heap     ] GC(4) Eden regions: 123->0(127)
[2.738s][info][gc,heap     ] GC(4) Survivor regions: 4->7(16)
[2.738s][info][gc,heap     ] GC(4) Old regions: 56->56
[2.738s][info][gc,heap     ] GC(4) Archive regions: 0->0
[2.738s][info][gc,heap     ] GC(4) Humongous regions: 0->0
[2.738s][info][gc,metaspace] GC(4) Metaspace: 293K(512K)->293K(512K) NonClass: 280K(384K)->280K(384K) Class: 13K(128K)->13K(128K)
[2.738s][info][gc          ] GC(4) Pause Young (Normal) (G1 Evacuation Pause) 182M->61M(256M) 6.033ms
[2.738s][info][gc,cpu      ] GC(4) User=0.00s Sys=0.00s Real=0.01s
[4.712s][info][gc,start    ] GC(5) Pause Young (Normal) (G1 Evacuation Pause)
[4.712s][info][gc,task     ] GC(5) Using 6 workers of 10 for evacuation
[4.716s][info][gc,phases   ] GC(5)   Pre Evacuate Collection Set: 0.1ms
[4.716s][info][gc,phases   ] GC(5)   Merge Heap Roots: 0.1ms
[4.716s][info][gc,phases   ] GC(5)   Evacuate Collection Set: 2.7ms
[4.716s][info][gc,phases   ] GC(5)   Post Evacuate Collection Set: 0.8ms
[4.717s][info][gc,phases   ] GC(5)   Other: 0.4ms
[4.717s][info][gc,heap     ] GC(5) Eden regions: 127->0(139)
[4.717s][info][gc,heap     ] GC(5) Survivor regions: 7->2(17)
[4.717s][info][gc,heap     ] GC(5) Old regions: 56->56
[4.717s][info][gc,heap     ] GC(5) Archive regions: 0->0
[4.717s][info][gc,heap     ] GC(5) Humongous regions: 0->0
[4.717s][info][gc,metaspace] GC(5) Metaspace: 293K(512K)->293K(512K) NonClass: 280K(384K)->280K(384K) Class: 13K(128K)->13K(128K)
[4.717s][info][gc          ] GC(5) Pause Young (Normal) (G1 Evacuation Pause) 188M->56M(256M) 4.431ms
[4.717s][info][gc,cpu      ] GC(5) User=0.03s Sys=0.00s Real=0.01s
[6.892s][info][gc,start    ] GC(6) Pause Young (Normal) (G1 Evacuation Pause)
[6.892s][info][gc,task     ] GC(6) Using 6 workers of 10 for evacuation
[6.903s][info][gc,phases   ] GC(6)   Pre Evacuate Collection Set: 0.2ms
[6.903s][info][gc,phases   ] GC(6)   Merge Heap Roots: 0.1ms
[6.903s][info][gc,phases   ] GC(6)   Evacuate Collection Set: 9.8ms
[6.903s][info][gc,phases   ] GC(6)   Post Evacuate Collection Set: 0.5ms
[6.903s][info][gc,phases   ] GC(6)   Other: 0.5ms
[6.903s][info][gc,heap     ] GC(6) Eden regions: 139->0(132)
[6.903s][info][gc,heap     ] GC(6) Survivor regions: 2->10(18)
[6.903s][info][gc,heap     ] GC(6) Old regions: 56->56
[6.903s][info][gc,heap     ] GC(6) Archive regions: 0->0
[6.903s][info][gc,heap     ] GC(6) Humongous regions: 0->0
[6.903s][info][gc,metaspace] GC(6) Metaspace: 293K(512K)->293K(512K) NonClass: 280K(384K)->280K(384K) Class: 13K(128K)->13K(128K)
[6.904s][info][gc          ] GC(6) Pause Young (Normal) (G1 Evacuation Pause) 195M->64M(256M) 11.482ms
[6.904s][info][gc,cpu      ] GC(6) User=0.03s Sys=0.02s Real=0.01s
[8.964s][info][gc,start    ] GC(7) Pause Young (Normal) (G1 Evacuation Pause)
[8.964s][info][gc,task     ] GC(7) Using 6 workers of 10 for evacuation
[8.969s][info][gc,phases   ] GC(7)   Pre Evacuate Collection Set: 0.1ms
[8.969s][info][gc,phases   ] GC(7)   Merge Heap Roots: 0.1ms
[8.969s][info][gc,phases   ] GC(7)   Evacuate Collection Set: 4.1ms
[8.969s][info][gc,phases   ] GC(7)   Post Evacuate Collection Set: 0.5ms
[8.969s][info][gc,phases   ] GC(7)   Other: 0.3ms
[8.969s][info][gc,heap     ] GC(7) Eden regions: 132->0(133)
[8.969s][info][gc,heap     ] GC(7) Survivor regions: 10->10(18)
[8.969s][info][gc,heap     ] GC(7) Old regions: 56->56
[8.969s][info][gc,heap     ] GC(7) Archive regions: 0->0
[8.969s][info][gc,heap     ] GC(7) Humongous regions: 0->0
[8.969s][info][gc,metaspace] GC(7) Metaspace: 293K(512K)->293K(512K) NonClass: 280K(384K)->280K(384K) Class: 13K(128K)->13K(128K)
[8.969s][info][gc          ] GC(7) Pause Young (Normal) (G1 Evacuation Pause) 196M->64M(256M) 5.424ms
[8.969s][info][gc,cpu      ] GC(7) User=0.02s Sys=0.08s Real=0.00s
[11.056s][info][gc,start    ] GC(8) Pause Young (Normal) (G1 Evacuation Pause)
[11.057s][info][gc,task     ] GC(8) Using 6 workers of 10 for evacuation
[11.058s][info][gc,phases   ] GC(8)   Pre Evacuate Collection Set: 0.1ms
[11.058s][info][gc,phases   ] GC(8)   Merge Heap Roots: 0.1ms
[11.058s][info][gc,phases   ] GC(8)   Evacuate Collection Set: 0.6ms
[11.058s][info][gc,phases   ] GC(8)   Post Evacuate Collection Set: 0.5ms
[11.058s][info][gc,phases   ] GC(8)   Other: 0.4ms
[11.058s][info][gc,heap     ] GC(8) Eden regions: 133->0(147)
[11.058s][info][gc,heap     ] GC(8) Survivor regions: 10->2(18)
[11.058s][info][gc,heap     ] GC(8) Old regions: 56->56
[11.058s][info][gc,heap     ] GC(8) Archive regions: 0->0
[11.058s][info][gc,heap     ] GC(8) Humongous regions: 0->0
[11.058s][info][gc,metaspace] GC(8) Metaspace: 293K(512K)->293K(512K) NonClass: 280K(384K)->280K(384K) Class: 13K(128K)->13K(128K)
[11.058s][info][gc          ] GC(8) Pause Young (Normal) (G1 Evacuation Pause) 197M->56M(256M) 2.030ms
[11.058s][info][gc,cpu      ] GC(8) User=0.00s Sys=0.00s Real=0.00s
[13.341s][info][gc,start    ] GC(9) Pause Young (Normal) (G1 Evacuation Pause)
[13.341s][info][gc,task     ] GC(9) Using 6 workers of 10 for evacuation
[13.353s][info][gc,phases   ] GC(9)   Pre Evacuate Collection Set: 0.1ms
[13.353s][info][gc,phases   ] GC(9)   Merge Heap Roots: 0.1ms
[13.353s][info][gc,phases   ] GC(9)   Evacuate Collection Set: 10.6ms
[13.353s][info][gc,phases   ] GC(9)   Post Evacuate Collection Set: 0.9ms
[13.354s][info][gc,phases   ] GC(9)   Other: 0.6ms
[13.354s][info][gc,heap     ] GC(9) Eden regions: 147->0(143)
[13.354s][info][gc,heap     ] GC(9) Survivor regions: 2->7(19)
[13.354s][info][gc,heap     ] GC(9) Old regions: 56->56
[13.354s][info][gc,heap     ] GC(9) Archive regions: 0->0
[13.354s][info][gc,heap     ] GC(9) Humongous regions: 0->0
[13.354s][info][gc,metaspace] GC(9) Metaspace: 293K(512K)->293K(512K) NonClass: 280K(384K)->280K(384K) Class: 13K(128K)->13K(128K)
[13.354s][info][gc          ] GC(9) Pause Young (Normal) (G1 Evacuation Pause) 203M->61M(256M) 12.747ms
[13.354s][info][gc,cpu      ] GC(9) User=0.00s Sys=0.00s Real=0.01s
```
