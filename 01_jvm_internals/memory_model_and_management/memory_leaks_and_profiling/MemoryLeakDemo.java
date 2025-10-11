package memory_model_and_management.memory_leaks_and_profiling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MemoryLeakDemo demonstrates a classic collection-based memory leak pattern.
 */
public class MemoryLeakDemo {
    
    // Static field - exists for entire application lifecycle
    // This is the root cause of the memory leak as it prevents GC
    private static final Map<String, List<byte[]>> globalCache = new HashMap<>();
    private static final List<LargeDataObject> permanentList = new ArrayList<>();
    
    // Counter to track memory allocation
    private static long objectCount = 0;
    
    /**
     * Simulates a large data object that consumes significant memory
     */
    private static class LargeDataObject {
        private final byte[] data;
        private final String id;
        private final long timestamp;
        
        public LargeDataObject(String id) {
            this.id = id;
            this.data = new byte[1024 * 100]; // 100KB per object
            this.timestamp = System.currentTimeMillis();
            
            // Fill the array with some data to make it realistic
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (i % 256);
            }
        }
        
        @Override
        public String toString() {
            return "LargeDataObject{id='" + id + "', timestamp=" + timestamp + ", size=" + data.length + "}";
        }
    }
    
    /**
     * Simulates cache operations that cause memory leak
     */
    private static void simulateCacheLeak() {
        System.out.println("Starting cache leak simulation...");
        
        for (int i = 0; i < 1000; i++) {
            String key = "cache_key_" + i;
            List<byte[]> dataList = new ArrayList<>();
            
            // Add multiple data chunks to simulate real cache usage
            for (int j = 0; j < 10; j++) {
                dataList.add(new byte[1024 * 10]); // 10KB chunks
            }
            
            // Add to global cache - objects never removed (memory leak!)
            globalCache.put(key, dataList);
            objectCount += dataList.size();
            
            if (i % 100 == 0) {
                System.out.println("Added " + i + " cache entries. Total objects: " + objectCount);
                printMemoryUsage();
            }
        }
    }
    
    /**
     * Simulates permanent data accumulation
     */
    private static void simulatePermanentDataLeak() {
        System.out.println("Starting permanent data leak simulation...");
        
        for (int i = 0; i < 500; i++) {
            // Create large objects that will never be garbage collected
            LargeDataObject largeObj = new LargeDataObject("permanent_" + i);
            permanentList.add(largeObj);
            objectCount++;
            
            if (i % 50 == 0) {
                System.out.println("Added " + i + " permanent objects. Total objects: " + objectCount);
                printMemoryUsage();
            }
        }
    }
    
    /**
     * Simulates continuous memory allocation without cleanup
     */
    private static void simulateContinuousLeak() throws InterruptedException {
        System.out.println("Starting continuous memory leak simulation...");
        System.out.println("This will continue until OutOfMemoryError occurs.");
        System.out.println("Monitor with JVisualVM to observe memory growth pattern.");
        
        int iteration = 0;
        while (true) {
            // Simulate different types of memory leaks
            
            // 1. Growing list without cleanup
            List<String> tempList = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                tempList.add("String data " + i + " with some additional content to make it larger");
            }
            // tempList goes out of scope but objects may not be immediately GC'd
            
            // 2. Static reference accumulation
            String staticKey = "continuous_" + iteration;
            List<byte[]> continuousData = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                continuousData.add(new byte[1024 * 50]); // 50KB chunks
            }
            globalCache.put(staticKey, continuousData);
            objectCount += continuousData.size();
            
            // 3. Large object creation
            if (iteration % 10 == 0) {
                LargeDataObject largeObj = new LargeDataObject("continuous_large_" + iteration);
                permanentList.add(largeObj);
                objectCount++;
            }
            
            iteration++;
            
            if (iteration % 50 == 0) {
                System.out.println("Iteration " + iteration + " completed. Total objects: " + objectCount);
                printMemoryUsage();
                
                // Suggest taking heap dump at this point
                if (iteration % 200 == 0) {
                    System.out.println("=== SUGGESTION: Take heap dump now for analysis ===");
                    System.out.println("Use: jmap -dump:format=b,file=memory_leak_" + iteration + ".hprof <pid>");
                }
            }
            
            // Small delay to allow monitoring tools to track changes
            Thread.sleep(100);
        }
    }
    
    /**
     * Prints current memory usage statistics
     */
    private static void printMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        System.out.println("=== Memory Usage ===");
        System.out.println("Used Memory: " + formatBytes(usedMemory));
        System.out.println("Free Memory: " + formatBytes(freeMemory));
        System.out.println("Total Memory: " + formatBytes(totalMemory));
        System.out.println("Max Memory: " + formatBytes(maxMemory));
        System.out.println("Memory Usage: " + String.format("%.2f%%", (double) usedMemory / maxMemory * 100));
        System.out.println("Cache Entries: " + globalCache.size());
        System.out.println("Permanent Objects: " + permanentList.size());
        System.out.println("Total Object Count: " + objectCount);
        System.out.println("==================");
    }
    
    /**
     * Formats bytes into human-readable format
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    public static void main(String[] args) {
        System.out.println("=== Memory Leak Demo Started ===");
        System.out.println("This demo simulates classic collection-based memory leaks.");
        System.out.println("Connect JVisualVM to monitor heap growth in real-time.");
        System.out.println("Expected outcome: OutOfMemoryError due to unbounded collection growth.");
        System.out.println();
        
        try {
            // Initial memory state
            printMemoryUsage();
            
            // Phase 1: Cache leak simulation
            simulateCacheLeak();
            System.out.println("\nPhase 1 completed. Memory should have grown significantly.");
            printMemoryUsage();
            
            // Phase 2: Permanent data leak
            simulatePermanentDataLeak();
            System.out.println("\nPhase 2 completed. More memory consumed by permanent objects.");
            printMemoryUsage();
            
            // Phase 3: Continuous leak until OOM
            System.out.println("\nStarting Phase 3: Continuous memory leak...");
            simulateContinuousLeak();
            
        } catch (OutOfMemoryError e) {
            System.err.println("=== OutOfMemoryError occurred as expected ===");
            System.err.println("This demonstrates the memory leak successfully!");
            System.err.println("Error: " + e.getMessage());
            
            // Print final statistics
            System.err.println("\nFinal Memory Statistics:");
            printMemoryUsage();
            
            System.err.println("\n=== Analysis Recommendations ===");
            System.err.println("1. Use JVisualVM to analyze heap growth pattern");
            System.err.println("2. Generate heap dump before crash for MAT analysis");
            System.err.println("3. Look for static field 'globalCache' and 'permanentList' in MAT");
            System.err.println("4. Trace 'Path to GC Roots' to understand retention chain");
            
        } catch (InterruptedException e) {
            System.err.println("Demo interrupted: " + e.getMessage());
        }
        
        System.out.println("\n=== Demo completed ===");
        System.out.println("Memory leak pattern demonstrated successfully.");
    }
}
