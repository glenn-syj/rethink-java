package memory_model_and_management.heap_and_stack_anatomy;

/*
    A demo class for checking stack memory
    Intentionally throw StackOverFlowError to check stack memory
*/
public class StackMemoryDemo {

    public static void recursiveMethod(int i) {
        System.out.println("Stack depth: " + i);
        recursiveMethod(i + 1);
    }

    public static void main(String[] args) {
        try {
            recursiveMethod(1);
        } catch (StackOverflowError e) {
            System.err.println("StackOverflowError caught: " + e.toString());
            System.err.println("This indicates that the stack memory has been exhausted.");
        }
    }
} 