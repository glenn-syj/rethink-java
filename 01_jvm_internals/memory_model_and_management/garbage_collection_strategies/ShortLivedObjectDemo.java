package memory_model_and_management.garbage_collection_strategies;

import java.util.ArrayList;
import java.util.List;

/*
    A demo class for checking short-lived objects
    The short-lived objects live a short time and are collected by Young GC.
*/
public class ShortLivedObjectDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting ShortLivedObjectDemo. Observe Young GC activities.");
        List<Object> tempList = new ArrayList<>();
        long counter = 0;

        while (true) {
            // create short-lived objects
            for (int i = 0; i < 1000; i++) {
                byte[] data = new byte[1024]; // 1KB
                // keep the object in the list to avoid being garbage-collected.
                // if the list gets too large, replace it with a new list 
                // to make the previous objects garbage-collectable.
                tempList.add(data);
                counter++;
                if (tempList.size() > 10000) { // almost 10MB
                    tempList = new ArrayList<>(); // make the previous objects garbage-collectable.
                }
            }
            if (counter % 1_000_000 == 0) {
                System.out.println("Generated " + counter + " objects.");
            }
            Thread.sleep(10); // wait for GC to happen
        }
    }
}