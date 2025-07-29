import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class CustomClassLoader extends ClassLoader {

    private String classPath;

    public CustomClassLoader(String classPath) {
        // Follow delegation model of the classloader hierarchy
        super(ClassLoader.getSystemClassLoader());
        this.classPath = classPath;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            byte[] classData = loadClassData(name);
            Class<?> clazz = defineClass(name, classData, 0, classData.length);
            System.out.println("The loaded class: " + clazz.getName());
            return clazz;
        } catch (IOException e) {
            throw new ClassNotFoundException("Class " + name + " not found.", e);
        }
    }

    private byte[] loadClassData(String name) throws IOException {
        String fileName = name.replace('.', '/') + ".class";
        Path path = Paths.get(classPath, fileName);
        return Files.readAllBytes(path);
    }
}
