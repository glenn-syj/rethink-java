public class ClassLoaderTest {

    public static void main(String[] args) throws Exception {
        System.out.println("\n--- Starting Custom ClassLoader Test for ClassWithNoPackage ---");

        String externalClassPath = "./external_classes";

        CustomClassLoader customClassLoader = new CustomClassLoader(externalClassPath);

        Class<?> noPackageClass = null;
        try {
            noPackageClass = customClassLoader.loadClass("ClassWithNoPackage");
            System.out.println("ClassWithNoPackage loaded successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("FAIL: ClassWithNoPackage should have been loaded by CustomClassLoader: " + e.getMessage());
            System.exit(1);
        }

        if (noPackageClass == null) {
            System.err.println("FAIL: ClassWithNoPackage should not be null after loading.");
            System.exit(1);
        }
        if (!"ClassWithNoPackage".equals(noPackageClass.getName())) {
            System.err.println("FAIL: Loaded class name should be ClassWithNoPackage, but was " + noPackageClass.getName());
            System.exit(1);
        }

        try {
            Object instance = noPackageClass.getDeclaredConstructor().newInstance();
            System.out.println("Instance of ClassWithNoPackage created.");
        } catch (Exception e) {
            System.err.println("FAIL: Error creating instance of ClassWithNoPackage: " + e.getMessage());
            System.exit(1);
        }

        if (noPackageClass.getClassLoader() == null) {
            System.err.println("FAIL: Class loader for ClassWithNoPackage should not be null.");
            System.exit(1);
        }
        if (!(noPackageClass.getClassLoader() instanceof CustomClassLoader)) {
            System.err.println("FAIL: ClassWithNoPackage should be loaded by an instance of CustomClassLoader, but was loaded by " + noPackageClass.getClassLoader());
            System.exit(1);
        }

        System.out.println("ClassWithNoPackage was loaded by: " + noPackageClass.getClassLoader());
        System.out.println("ClassLoaderTest class was loaded by: " + ClassLoaderTest.class.getClassLoader());
        System.out.println("--- Custom ClassLoader Test for ClassWithNoPackage Finished ---\n");

        System.out.println("--- Starting System ClassLoader Test for ClassWithNoPackageFromExternalPath ---");
        try {
            ClassLoader.getSystemClassLoader().loadClass("ClassWithNoPackage");
            System.err.println("FAIL: ClassWithNoPackage should NOT be found by System ClassLoader from external_classes.");
            System.exit(1);
        } catch (ClassNotFoundException e) {
            System.out.println("System ClassLoader correctly failed to load ClassWithNoPackage: " + e.getMessage());
        }
        System.out.println("--- System ClassLoader Test for ClassWithNoPackageFromExternalPath Finished ---\n");

        System.out.println("All tests passed successfully!");
    }
}