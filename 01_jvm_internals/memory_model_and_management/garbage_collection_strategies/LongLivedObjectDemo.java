package memory_model_and_management.garbage_collection_strategies;

import java.util.ArrayList;
import java.util.List;

public class LongLivedObjectDemo {
    // The instances of this list will likely be promoted to Old Generation
    private static List<byte[]> permanentList = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting LongLivedObjectDemo. Observe promotion and Full GC.");

        // 1. Create objects that will likely be promoted to Old Generation (initial allocation)
        for (int i = 0; i < 5000; i++) {
            permanentList.add(new byte[1024 * 10]); // 10KB object
        }
        System.out.println("Initial long-lived objects created. Size: " + permanentList.size() * 10 + "KB");

        // 2. Create short-lived objects to induce Young GC
        List<Object> tempList = new ArrayList<>();
        long counter = 0;
        while (true) {
            for (int i = 0; i < 1000; i++) {
                byte[] data = new byte[1024]; // 1KB object
                tempList.add(data);
                counter++;
                if (tempList.size() > 10000) {
                    tempList = new ArrayList<>();
                }
            }
            if (counter % 1_000_000 == 0) {
                System.out.println("Generated " + counter + " short-lived objects.");
            }
            // Add new objects to permanentList to increase Old Gen usage
            if (counter % 5_000_000 == 0) {
                permanentList.add(new byte[1024 * 500]); // 500KB object added
                System.out.println("Added more long-lived objects. PermanentList size: " + permanentList.size() * 10 + "KB");
            }
            Thread.sleep(10);
        }
    }
}
