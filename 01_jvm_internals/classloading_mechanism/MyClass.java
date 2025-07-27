package classloading_mechanism;

class MyClass {
    public static final String STATIC_FIELD = "Hello from MyClass!";

    static {
        System.out.println("MyClass: Static initializer block executed.");
    }

    public MyClass() {
        System.out.println("MyClass: Constructor executed.");
    }
} 