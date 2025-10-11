package memory_model_and_management.memory_leaks_and_profiling;

import java.lang.ref.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ReferenceTypeDemo demonstrates different reference types and their GC behavior.
 */
public class ReferenceTypeDemo {
    
    // Large data object to demonstrate memory pressure effects
    private static class LargeDataObject {
        private final byte[] data;
        private final String id;
        private final long timestamp;
        
        public LargeDataObject(String id) {
            this.id = id;
            this.data = new byte[1024 * 100]; // 100KB per object
            this.timestamp = System.currentTimeMillis();
            
            // Fill with some data
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (i % 256);
            }
        }
        
        @Override
        public String toString() {
            return "LargeDataObject{id='" + id + "', timestamp=" + timestamp + ", size=" + data.length + "}";
        }
        
        @Override
        @SuppressWarnings("deprecation")
        protected void finalize() throws Throwable {
            // NOTE: finalize() is deprecated since Java 9 and should NOT be used in production code!
            // We use it here ONLY for educational purposes to visualize GC behavior and object collection timing.
            // In real applications, use try-with-resources, PhantomReference, or Cleaner API instead.
            System.out.println("LargeDataObject '" + id + "' is being garbage collected");
            super.finalize();
        }
    }
    
    // Collections to hold different reference types
    private static final List<LargeDataObject> strongReferences = new ArrayList<>();
    private static final List<WeakReference<LargeDataObject>> weakReferences = new ArrayList<>();
    private static final List<SoftReference<LargeDataObject>> softReferences = new ArrayList<>();
    private static final List<PhantomReference<LargeDataObject>> phantomReferences = new ArrayList<>();
    private static final ReferenceQueue<LargeDataObject> phantomQueue = new ReferenceQueue<>();
    
    private static int objectCounter = 0;
    
    /**
     * Demonstrates Strong Reference behavior
     */
    private static void demonstrateStrongReferences() {
        System.out.println("=== Demonstrating Strong References ===");
        System.out.println("Strong references prevent GC - objects are never collected while referenced");
        
        // Create objects with strong references
        for (int i = 0; i < 50; i++) {
            LargeDataObject obj = new LargeDataObject("strong_" + i);
            strongReferences.add(obj); // Strong reference - prevents GC
            objectCounter++;
            
            if (i % 10 == 0) {
                System.out.println("Created " + (i + 1) + " strong references. Objects alive: " + 
                                 strongReferences.size());
                printMemoryUsage();
            }
        }
        
        System.out.println("Strong references created. These objects will NEVER be garbage collected");
        System.out.println("while the strongReferences list exists (memory leak simulation)");
    }
    
    /**
     * Demonstrates Weak Reference behavior
     */
    private static void demonstrateWeakReferences() {
        System.out.println("\n=== Demonstrating Weak References ===");
        System.out.println("Weak references allow immediate GC collection in next GC cycle");
        
        // Create objects with weak references
        for (int i = 0; i < 30; i++) {
            LargeDataObject obj = new LargeDataObject("weak_" + i);
            WeakReference<LargeDataObject> weakRef = new WeakReference<>(obj);
            weakReferences.add(weakRef);
            objectCounter++;
            
            // Object goes out of scope, only weak reference remains
            obj = null; // Remove strong reference
            
            if (i % 5 == 0) {
                System.out.println("Created " + (i + 1) + " weak references");
                checkWeakReferenceStatus();
                printMemoryUsage();
            }
        }
        
        System.out.println("Weak references created. Objects should be collected in next GC cycle");
        forceGarbageCollection();
        checkWeakReferenceStatus();
    }
    
    /**
     * Demonstrates Soft Reference behavior
     */
    private static void demonstrateSoftReferences() {
        System.out.println("\n=== Demonstrating Soft References ===");
        System.out.println("Soft references allow GC only when memory pressure exists");
        
        // Create objects with soft references
        for (int i = 0; i < 40; i++) {
            LargeDataObject obj = new LargeDataObject("soft_" + i);
            SoftReference<LargeDataObject> softRef = new SoftReference<>(obj);
            softReferences.add(softRef);
            objectCounter++;
            
            // Object goes out of scope, only soft reference remains
            obj = null; // Remove strong reference
            
            if (i % 8 == 0) {
                System.out.println("Created " + (i + 1) + " soft references");
                checkSoftReferenceStatus();
                printMemoryUsage();
            }
        }
        
        System.out.println("Soft references created. Objects will be collected only under memory pressure");
        
        // Try to create memory pressure to trigger soft reference collection
        System.out.println("Creating memory pressure to test soft reference behavior...");
        createMemoryPressure();
        
        checkSoftReferenceStatus();
    }
    
    /**
     * Demonstrates Phantom Reference behavior
     */
    private static void demonstratePhantomReferences() {
        System.out.println("\n=== Demonstrating Phantom References ===");
        System.out.println("Phantom references provide cleanup notifications - objects always collected");
        
        // Create objects with phantom references
        for (int i = 0; i < 20; i++) {
            LargeDataObject obj = new LargeDataObject("phantom_" + i);
            PhantomReference<LargeDataObject> phantomRef = new PhantomReference<>(obj, phantomQueue);
            phantomReferences.add(phantomRef);
            objectCounter++;
            
            // Object goes out of scope, only phantom reference remains
            obj = null; // Remove strong reference
            
            if (i % 4 == 0) {
                System.out.println("Created " + (i + 1) + " phantom references");
                printMemoryUsage();
            }
        }
        
        System.out.println("Phantom references created. Objects will be collected and notifications queued");
        
        // Force GC and check phantom queue
        forceGarbageCollection();
        checkPhantomQueue();
    }
    
    /**
     * Creates memory pressure to test soft reference behavior
     */
    private static void createMemoryPressure() {
        System.out.println("Creating memory pressure with large object allocation...");
        
        List<byte[]> pressureList = new ArrayList<>();
        try {
            for (int i = 0; i < 100; i++) {
                // Allocate large objects to create memory pressure
                pressureList.add(new byte[1024 * 1024]); // 1MB each
                
                if (i % 20 == 0) {
                    System.out.println("Allocated " + (i + 1) + " MB of memory pressure objects");
                    printMemoryUsage();
                    
                    // Check if soft references are being collected
                    checkSoftReferenceStatus();
                }
            }
        } catch (OutOfMemoryError e) {
            System.out.println("OutOfMemoryError occurred - memory pressure achieved");
            System.out.println("This should trigger soft reference collection");
        } finally {
            // Clear pressure objects
            pressureList.clear();
            System.gc(); // Suggest GC
        }
    }
    
    /**
     * Forces garbage collection and waits
     */
    private static void forceGarbageCollection() {
        System.out.println("Forcing garbage collection...");
        
        for (int i = 0; i < 3; i++) {
            System.gc();
            try {
                Thread.sleep(100); // Give GC time to work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        printMemoryUsage();
    }
    
    /**
     * Checks status of weak references
     */
    private static void checkWeakReferenceStatus() {
        int collected = 0;
        int alive = 0;
        
        for (WeakReference<LargeDataObject> ref : weakReferences) {
            if (ref.get() == null) {
                collected++;
            } else {
                alive++;
            }
        }
        
        System.out.println("Weak References - Collected: " + collected + ", Alive: " + alive);
    }
    
    /**
     * Checks status of soft references
     */
    private static void checkSoftReferenceStatus() {
        int collected = 0;
        int alive = 0;
        
        for (SoftReference<LargeDataObject> ref : softReferences) {
            if (ref.get() == null) {
                collected++;
            } else {
                alive++;
            }
        }
        
        System.out.println("Soft References - Collected: " + collected + ", Alive: " + alive);
    }
    
    /**
     * Checks phantom reference queue for cleanup notifications
     */
    private static void checkPhantomQueue() {
        System.out.println("Checking phantom reference queue for cleanup notifications...");
        
        int notifications = 0;
        Reference<? extends LargeDataObject> ref;
        
        while ((ref = phantomQueue.poll()) != null) {
            notifications++;
            System.out.println("Phantom reference notification #" + notifications + " received");
        }
        
        System.out.println("Total phantom reference notifications: " + notifications);
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
        
        System.out.println("--- Memory Usage ---");
        System.out.println("Used Memory: " + formatBytes(usedMemory));
        System.out.println("Free Memory: " + formatBytes(freeMemory));
        System.out.println("Total Memory: " + formatBytes(totalMemory));
        System.out.println("Max Memory: " + formatBytes(maxMemory));
        System.out.println("Memory Usage: " + String.format("%.2f%%", (double) usedMemory / maxMemory * 100));
        System.out.println("Object Counter: " + objectCounter);
        System.out.println("-------------------");
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
    
    /**
     * Demonstrates reference type comparison under different scenarios
     */
    private static void demonstrateReferenceComparison() {
        System.out.println("\n=== Reference Type Comparison Summary ===");
        
        System.out.println("\n1. STRONG REFERENCES:");
        System.out.println("   - Objects: " + strongReferences.size());
        System.out.println("   - Behavior: NEVER garbage collected while referenced");
        System.out.println("   - Use Case: Normal object references");
        System.out.println("   - Memory Leak Risk: HIGHEST");
        
        System.out.println("\n2. WEAK REFERENCES:");
        checkWeakReferenceStatus();
        System.out.println("   - Behavior: Collected immediately in next GC cycle");
        System.out.println("   - Use Case: Caches, metadata, WeakHashMap");
        System.out.println("   - Memory Leak Risk: VERY LOW");
        
        System.out.println("\n3. SOFT REFERENCES:");
        checkSoftReferenceStatus();
        System.out.println("   - Behavior: Collected only under memory pressure");
        System.out.println("   - Use Case: Large object caches, image caches");
        System.out.println("   - Memory Leak Risk: LOW");
        
        System.out.println("\n4. PHANTOM REFERENCES:");
        System.out.println("   - Objects: " + phantomReferences.size());
        System.out.println("   - Behavior: Always collected, provides cleanup notifications");
        System.out.println("   - Use Case: Resource cleanup, finalizer alternative");
        System.out.println("   - Memory Leak Risk: NONE");
        
        checkPhantomQueue();
    }
    
    public static void main(String[] args) {
        System.out.println("=== Reference Type Demo Started ===");
        System.out.println("This demo demonstrates different reference types and their GC behavior.");
        System.out.println("Monitor with JVisualVM to observe memory allocation patterns.");
        System.out.println();
        
        try {
            // Initial memory state
            printMemoryUsage();
            
            // Demonstrate each reference type
            demonstrateStrongReferences();
            demonstrateWeakReferences();
            demonstrateSoftReferences();
            demonstratePhantomReferences();
            
            // Compare all reference types
            demonstrateReferenceComparison();
            
            System.out.println("\n=== Demo completed ===");
            System.out.println("Reference type behaviors demonstrated successfully.");
            
            System.out.println("\n=== Analysis Recommendations ===");
            System.out.println("1. Use JVisualVM to analyze heap allocation patterns");
            System.out.println("2. Generate heap dump to see object retention differences");
            System.out.println("3. Observe finalizer output for object collection timing");
            System.out.println("4. Compare memory usage patterns between reference types");
            System.out.println("5. Use appropriate reference types based on use case:");
            System.out.println("   - Strong: Normal references");
            System.out.println("   - Weak: Metadata caches, temporary associations");
            System.out.println("   - Soft: Large caches that can survive minor pressure");
            System.out.println("   - Phantom: Resource cleanup, finalizer replacement");
            
        } catch (Exception e) {
            System.err.println("Demo error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
