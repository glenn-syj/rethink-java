package memory_model_and_management.memory_leaks_and_profiling;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * ResourceLeakDemo demonstrates external resource leaks and proper cleanup techniques.
 */
public class ResourceLeakDemo {
    
    // Static collections to track resources (simulating real-world scenarios)
    private static final List<FileInputStream> openFileStreams = new ArrayList<>();
    private static final List<FileOutputStream> openOutputStreams = new ArrayList<>();
    
    private static int resourceCounter = 0;
    private static boolean demonstrateLeaks = true;
    
    /**
     * Demonstrates file input stream leaks
     */
    private static void demonstrateFileInputStreamLeak() {
        System.out.println("=== Demonstrating FileInputStream Leaks ===");
        
        try {
            // Create temporary files for demonstration
            Path tempDir = Files.createTempDirectory("resource_leak_demo");
            System.out.println("Created temp directory: " + tempDir);
            
            for (int i = 0; i < 100; i++) {
                Path tempFile = tempDir.resolve("temp_file_" + i + ".txt");
                Files.write(tempFile, ("Test data for file " + i).getBytes());
                
                if (demonstrateLeaks) {
                    // LEAKY VERSION: FileInputStream not closed
                    FileInputStream fis = new FileInputStream(tempFile.toFile());
                    openFileStreams.add(fis); // Simulate keeping reference
                    resourceCounter++;
                    
                    // Read some data but don't close
                    byte[] buffer = new byte[1024];
                    int bytesRead = fis.read(buffer);
                    System.out.println("Read " + bytesRead + " bytes from file " + i + " (LEAKED)");
                    
                } else {
                    // CORRECT VERSION: Using try-with-resources
                    try (FileInputStream fis = new FileInputStream(tempFile.toFile())) {
                        byte[] buffer = new byte[1024];
                        int bytesRead = fis.read(buffer);
                        System.out.println("Read " + bytesRead + " bytes from file " + i + " (PROPERLY CLOSED)");
                    }
                }
                
                if (i % 20 == 0) {
                    System.out.println("Processed " + i + " files. Open streams: " + openFileStreams.size());
                    printResourceStats();
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error in file stream demo: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates file output stream leaks
     */
    private static void demonstrateFileOutputStreamLeak() {
        System.out.println("\n=== Demonstrating FileOutputStream Leaks ===");
        
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "resource_leak_demo");
            Files.createDirectories(tempDir);
            
            for (int i = 0; i < 50; i++) {
                Path tempFile = tempDir.resolve("output_file_" + i + ".txt");
                
                if (demonstrateLeaks) {
                    // LEAKY VERSION: FileOutputStream not closed
                    FileOutputStream fos = new FileOutputStream(tempFile.toFile());
                    openOutputStreams.add(fos); // Simulate keeping reference
                    resourceCounter++;
                    
                    // Write some data but don't close
                    String data = "Output data for file " + i + " with some additional content\n";
                    fos.write(data.getBytes());
                    fos.flush();
                    System.out.println("Wrote data to file " + i + " (LEAKED)");
                    
                } else {
                    // CORRECT VERSION: Using try-with-resources
                    try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                        String data = "Output data for file " + i + " with some additional content\n";
                        fos.write(data.getBytes());
                        fos.flush();
                        System.out.println("Wrote data to file " + i + " (PROPERLY CLOSED)");
                    }
                }
                
                if (i % 10 == 0) {
                    System.out.println("Processed " + i + " output files. Open streams: " + openOutputStreams.size());
                    printResourceStats();
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error in file output stream demo: " + e.getMessage());
        }
    }
    
    
    /**
     * Demonstrates proper resource cleanup
     */
    private static void demonstrateProperCleanup() {
        System.out.println("\n=== Demonstrating Proper Resource Cleanup ===");
        
        // Close all leaked resources
        System.out.println("Cleaning up leaked file input streams...");
        for (FileInputStream fis : openFileStreams) {
            try {
                fis.close();
                resourceCounter--;
            } catch (IOException e) {
                System.err.println("Error closing file input stream: " + e.getMessage());
            }
        }
        openFileStreams.clear();
        
        System.out.println("Cleaning up leaked file output streams...");
        for (FileOutputStream fos : openOutputStreams) {
            try {
                fos.close();
                resourceCounter--;
            } catch (IOException e) {
                System.err.println("Error closing file output stream: " + e.getMessage());
            }
        }
        openOutputStreams.clear();
        
        System.out.println("Resource cleanup completed. Remaining resources: " + resourceCounter);
    }
    
    /**
     * Prints current resource statistics
     */
    private static void printResourceStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        System.out.println("--- Resource Statistics ---");
        System.out.println("Open FileInputStreams: " + openFileStreams.size());
        System.out.println("Open FileOutputStreams: " + openOutputStreams.size());
        System.out.println("Total Tracked Resources: " + resourceCounter);
        System.out.println("Used Memory: " + formatBytes(usedMemory));
        System.out.println("Free Memory: " + formatBytes(freeMemory));
        System.out.println("---------------------------");
        
        // OS-level monitoring suggestions
        if (resourceCounter > 0) {
            System.out.println("OS-Level Monitoring Commands:");
            System.out.println("  lsof -p <pid>  # List open files for this process");
            System.out.println("  netstat -an | grep <port>  # Check socket connections");
            System.out.println("  ps -o pid,rss,vsz <pid>  # Check memory usage");
        }
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
        System.out.println("=== Resource Leak Demo Started ===");
        System.out.println("This demo shows external resource leaks and proper cleanup techniques.");
        System.out.println("Monitor with JVisualVM and OS tools to observe resource consumption.");
        System.out.println();
        
        // Check command line arguments
        if (args.length > 0 && "clean".equals(args[0])) {
            demonstrateLeaks = false;
            System.out.println("Running in CLEAN mode - demonstrating proper resource management.");
        } else {
            System.out.println("Running in LEAK mode - demonstrating resource leaks.");
            System.out.println("Use 'clean' argument to see proper resource management: java ResourceLeakDemo clean");
        }
        
        try {
            // Initial resource state
            printResourceStats();
            
            // Demonstrate different types of resource leaks
            demonstrateFileInputStreamLeak();
            demonstrateFileOutputStreamLeak();
            
            System.out.println("\n=== Resource Leak Simulation Completed ===");
            printResourceStats();
            
            if (demonstrateLeaks) {
                System.out.println("\n=== Demonstrating Cleanup ===");
                demonstrateProperCleanup();
                
                System.out.println("\n=== Final Resource State ===");
                printResourceStats();
                
                System.out.println("\n=== Analysis Recommendations ===");
                System.out.println("1. Use JVisualVM to monitor memory usage patterns");
                System.out.println("2. Use OS tools to monitor file handles and socket connections:");
                System.out.println("   - lsof -p <pid>  # List open files");
                System.out.println("   - netstat -an   # List network connections");
                System.out.println("3. Compare memory usage with and without proper cleanup");
                System.out.println("4. Observe application behavior under resource exhaustion");
                System.out.println("5. Use try-with-resources for automatic resource management");
            }
            
        } catch (Exception e) {
            System.err.println("Demo error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== Demo completed ===");
        System.out.println("Resource leak patterns demonstrated successfully.");
    }
}
