package classloading_mechanism;

public class ClassLoadingDemo {
    static {
        System.out.println("ClassLoadingDemo: Static initializer block executed.");
    }

    public static void main(String[] args) {
        System.out.println("ClassLoadingDemo: main method started.");

        // Class will be loaded and initialized when MyClass.class is accessed for the first time
        
        // But MyClass.STATIC_FIELD is a compile time constant, so it is not loaded
        System.out.println("Accessing MyClass.STATIC_FIELD...");
        System.out.println(MyClass.STATIC_FIELD);

        // MyClassNoInline.STATIC_FIELD is not a compile time constant, so it is loaded
        System.out.println("Accessing MyClassNoInline.STATIC_FIELD...");
        System.out.println(MyClassNoInline.STATIC_FIELD);

        System.out.println("ClassLoadingDemo: main method finished.");
    }
} 