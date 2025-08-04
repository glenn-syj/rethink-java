package memory_model_and_management.heap_and_stack_anatomy;

import java.util.ArrayList;
import java.util.List;

/*
    A demo class for checking heap memory
    Use JVM monitoring tool like jconsole
 */
public class HeapMemoryDemo {
    public static void main(String[] args) {
        List<byte[]> list = new ArrayList<>();

        // 1 kb (1024 bytes) * 1_000_000 = almost 1GB
        for (int i = 0; i < 1000000; i++) {
            list.add(new byte[1024]);
        }
        System.out.println("Objects created. Time to monitor heap memory.");
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}