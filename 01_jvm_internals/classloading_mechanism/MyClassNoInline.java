package classloading_mechanism;

class MyClassNoInline {
    // Make a new String instead of a compile time constant
    public static final String STATIC_FIELD = new String("Hello from MyClassNoInline!");

    static {
        System.out.println("MyClassNoInline: Static initializer block executed.");
    }

    public MyClassNoInline() {
        System.out.println("MyClassNoInline: Constructor executed.");
    }
}
