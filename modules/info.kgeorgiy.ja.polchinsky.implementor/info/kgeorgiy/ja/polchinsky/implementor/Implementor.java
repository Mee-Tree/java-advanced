package info.kgeorgiy.ja.polchinsky.implementor;

import info.kgeorgiy.java.advanced.implementor.BaseImplementorTest;
import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * The {@code Implementor} class generates basic implementation of provided class or interface.
 * <br>
 * Capable of producing {@code .java} and {@code .jar} files using {@code Reflection API}.
 * <p>
 * All the generated class names end with the {@code Impl} suffix.
 * Overridden methods return default value for their return type, which is
 * {@code 0} for numeric primitives, {@code false} for {@code boolean}, and {@code null} for non-primitive types.
 *
 * @author Dmitry Polchinsky
 * @see Impler
 * @see JarImpler
 * @see java.lang.reflect
 */
public class Implementor implements JarImpler {
    /**
     * Command line option for running implementor in {@code jar} mode.
     */
    private static final String JAR_OPTION = "-jar";

    /**
     * Usage message.
     */
    private static final String USAGE = String.format("Usage: java %s [%s] class_name output_path",
            Implementor.class.getCanonicalName(), JAR_OPTION);

    /**
     * Extension for generated source files.
     */
    private static final String JAVA_EXT = ".java";

    /**
     * Extension for generated compiled files.
     */
    private static final String CLASS_EXT = ".class";

    /**
     * Suffix for generated class name.
     */
    private static final String IMPL_SUFFIX = "Impl";

    /**
     * Class for grouping placeholders.
     */
    private final static class Placeholder {
        /**
         * Placeholder for package declaration.
         */
        static final String PACKAGE = "package %s;";

        /**
         * Placeholder for class declaration.
         */
        static final String CLASS = "public class %s%s%s";

        /**
         * Placeholder for super call.
         */
        static final String SUPER = "super(%s);";

        /**
         * Placeholder for return statement.
         */
        static final String RETURN = "return %s;";

        /**
         * Placeholder for executable declaration.
         */
        static final String EXEC_DECL = "%s(%s)%s";

        /**
         * Placeholder for declaration and body together.
         */
        static final String DECL_AND_BODY = String.join(StringUtils.LINE_SEPARATOR,
                "%s {", "%s", "}");

        /**
         * Default constructor. Made private to prevent instantiation of utility class.
         */
        private Placeholder() {
        }
    }

    /**
     * Keyword for implementing interfaces.
     */
    private static final String IMPLEMENTS = " implements ";

    /**
     * Keyword for extending a class.
     */
    private static final String EXTENDS = " extends ";

    /**
     * Keyword for throwing exceptions.
     */
    private static final String THROWS = " throws ";

    /**
     * Checks that all elements of {@code objects} are not null.
     * <br>
     * Calls {@link Objects#requireNonNull(Object)} for each of {@code objects}.
     *
     * @param objects the objects to check for nullity
     * @throws NullPointerException if any of {@code objects} is null
     */
    private static void requireNonNull(final Object... objects) {
        Arrays.stream(objects).forEach(Objects::requireNonNull);
    }

    /**
     * The application's main entry point. Supports two modes:
     * <ol>
     *     <li>{@code class_name output_path}.
     *         <br>
     *         Creates a {@code .java} file by calling {@link #implement(Class, Path)}.</li>
     *     <li>{@code --jar class_name output_path}.
     *         <br>
     *         Creates a {@code .jar} file by calling {@link #implementJar(Class, Path)}.</li>
     * </ol>
     * If any of provided arguments are invalid or an error occurs,
     * message describing the problem is printed in standard error.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        try {
            requireNonNull((Object[]) args);

            if (args.length == 2) {
                new Implementor().implement(Class.forName(args[0]), Path.of(args[1]));
            } else if (args.length == 3 && JAR_OPTION.equals(args[0])) {
                new Implementor().implementJar(Class.forName(args[1]), Path.of(args[2]));
            } else {
                System.err.println(USAGE);
            }
        } catch (final ClassNotFoundException e) {
            System.err.println("No class was found by name: " + e.getMessage());
        } catch (final InvalidPathException e) {
            System.err.println("Invalid path: " + e.getMessage());
        } catch (final ImplerException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Default constructor.
     */
    public Implementor() {
    }

    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        requireNonNull(token, root);

        final ImplerException cause = getCauseIfApplicable(token);
        if (cause != null) {
            throw new ImplerException("Cannot implement given token: " + token.getCanonicalName(), cause);
        }

        final Path path;
        try {
            path = getImplPath(token, root);
        } catch (final InvalidPathException e) {
            throw new ImplerException("Invalid path", e);
        }

        FileUtils.createDirectories(path);
        try (final BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(StringUtils.toAscii(generateImpl(token)));
        } catch (final IOException e) {
            throw new ImplerException("Couldn't write to output file", e);
        }
    }

    @Override
    public void implementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        requireNonNull(token, jarFile);

        FileUtils.createDirectories(jarFile);
        final Path temp = FileUtils.createTempDirectory(jarFile.toAbsolutePath().getParent());
        try {
            implement(token, temp);
            compile(token, temp);
            buildJar(token, jarFile, temp);
        } finally {
            FileUtils.deleteDirectory(temp);
        }
    }

    /**
     * Compiles the implementation of {@code token}
     * and stores resulting {@code .class} files inside {@code temp}.
     *
     * @param token the type token implementation of which to compile
     * @param temp  the directory where source files are located
     * @throws ImplerException if an error occurs during compilation
     * @see JavaCompiler
     */
    private void compile(final Class<?> token, final Path temp) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not find java compiler");
        }

        final String classpath = getClassPath(token);
        final List<String> args = new ArrayList<>(
                List.of("-encoding", "utf8"));

        if (classpath != null) {
            args.add("-cp");
            args.add(temp + File.pathSeparator + classpath);
        } else {
            args.add("--patch-module");
            args.add(token.getModule().getName() + "=" + temp);
        }

        args.add(getImplPath(token, temp).toString());
        final int returnCode = compiler.run(null, null, null,
                args.toArray(String[]::new));
        if (returnCode != 0) {
            throw new ImplerException("Compilation returned nonzero code: " + returnCode);
        }
    }

    /**
     * Returns the classpath of the specified token.
     *
     * @param token the type token to get classpath of
     * @return the classpath of the provided token
     * @throws ImplerException if the classpath is incorrect
     */
    private static String getClassPath(final Class<?> token) throws ImplerException {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final NullPointerException e) {
            return null;
        } catch (final URISyntaxException e) {
            throw new ImplerException(e);
        }
    }

    /**
     * Builds a {@code .jar} file containing the implementation of {@code token}.
     * <p>
     * Takes compiled files from {@code temp} directory
     * and produces the resulting {@code .jar} file at the location specified by {@code jarFile}.
     *
     * @param token   the type token implementation of which to build {@code .jar} for
     * @param jarFile the path to the file where the resulting {@code .jar} should be created
     * @param temp    the directory where compiled files are located
     * @throws ImplerException if an error occurs during build process
     */
    private void buildJar(final Class<?> token, final Path jarFile, final Path temp) throws ImplerException {
        try (final JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile))) {
            final String localName = getPackageDirectory(token, "/") + "/" + getImplName(token) + CLASS_EXT;
            out.putNextEntry(new ZipEntry(localName));
            Files.copy(temp.resolve(localName), out);
        } catch (final IOException e) {
            throw new ImplerException("Could not build a jar file", e);
        }
    }

    /**
     * Returns the cause why the specified {@code token} cannot be implemented.
     *
     * @param token the type token to check the possibility of implementation for
     * @return the cause if it cannot be implemented, {@code null otherwise}
     */
    private ImplerException getCauseIfApplicable(final Class<?> token) {
        final int modifiers = token.getModifiers();
        if (token.isPrimitive()) {
            return new ImplerException("is primitive");
        } else if (token.isArray()) {
            return new ImplerException("is array");
        } else if (token.equals(Enum.class)) {
            return new ImplerException("is " + Enum.class.getCanonicalName());
        } else if (Modifier.isPrivate(modifiers)) {
            return new ImplerException("has private modifier");
        } else if (Modifier.isFinal(modifiers)) {
            return new ImplerException("has final modifier");
        }
        return null;
    }

    /**
     * Returns the path to the {@code .java} file implementing the given {@code token}.
     *
     * @param token the type token to implement
     * @param root  the directory where the implementation should be located
     * @return the path to {@code token} implementation
     */
    private Path getImplPath(final Class<?> token, final Path root) {
        return root.resolve(Path.of(getPackageDirectory(token, File.separator), getImplName(token) + JAVA_EXT));
    }

    /**
     * Returns the directory representing the package of the provided {@code token}.
     *
     * @param token     the type token to get the package from
     * @param separator the file separator
     * @return a {@code String} representing the resulting directory
     */
    private String getPackageDirectory(final Class<?> token, final String separator) {
        return token.getPackageName().replace(".", separator);
    }

    /**
     * Returns the name of {@code token} implementation.
     *
     * @param token the type token to implement
     * @return the name of the class
     */
    private String getImplName(final Class<?> token) {
        return token.getSimpleName() + IMPL_SUFFIX;
    }

    /**
     * Generates {@code token} implementation class.
     *
     * @param token the type token to be implemented
     * @return a {@code String} representation of {@code token} implementation class
     * @throws ImplerException if the content cannot be generated,
     *                         due to absence of non-private constructors
     */
    private String generateImpl(final Class<?> token) throws ImplerException {
        return StringUtils.joinBlocks(generatePackageDecl(token),
                generateDeclAndBody(generateClassDecl(token), generateClassBody(token)));
    }

    /**
     * Generates the package declaration for the specified {@code token}.
     *
     * @param token the type token to get package from
     * @return a {@code String} representing the package declaration of {@code token},
     * or an empty {@code String} if the package is default.
     */
    private String generatePackageDecl(final Class<?> token) {
        final String packageName = token.getPackageName();
        return packageName.isBlank() ? StringUtils.EMPTY : String.format(Placeholder.PACKAGE, packageName);
    }

    /**
     * Generates the class declaration of {@code token} implementation.
     * Determines whether the {@code token} is an {@code interface} or a {@code class},
     * and generates a correct declaration of the class, implementing {@code token}.
     *
     * @param token the type token to generate implementation for
     * @return a {@code String} representation of class declaration
     */
    private String generateClassDecl(final Class<?> token) {
        final String keyword = token.isInterface() ? IMPLEMENTS : EXTENDS;
        return String.format(Placeholder.CLASS, getImplName(token), keyword, token.getCanonicalName());
    }

    /**
     * Generates the class body of {@code token} implementation.
     *
     * @param token the type token to generate implementation for
     * @return a {@code String} representation of generated class body
     * @throws ImplerException if the body cannot be generated,
     *                         due to absence of non-private constructors
     */
    private String generateClassBody(final Class<?> token) throws ImplerException {
        return StringUtils.joinBlocks(generateAnyConstructor(token), generateMethods(token));
    }

    /**
     * Generates the provided {@code token} constructor implementation
     * using {@link #constructorToString(Constructor)}.
     *
     * @param token the type token to generate implementation for
     * @return an empty string if {@code token} is an interface,
     * a {@code String} representation of generated constructor otherwise
     * @throws ImplerException if a constructor is required, but no non-private constructors were found
     */
    private String generateAnyConstructor(final Class<?> token) throws ImplerException {
        if (token.isInterface()) {
            return StringUtils.EMPTY;
        }

        return Arrays.stream(token.getDeclaredConstructors())
                .filter(ctor -> !Modifier.isPrivate(ctor.getModifiers()))
                .findAny()
                .map(this::constructorToString)
                .orElseThrow(() -> new ImplerException("Cannot implement class with only private constructors"));
    }

    /**
     * Generates the provided {@code token} {@code abstract} method implementations
     * using {@link #methodToString(Method)}.
     *
     * @param token the type token to generate implementation for
     * @return a {@code String} representation of generated {@code abstract} methods
     */
    private String generateMethods(final Class<?> token) {
        final Set<MethodSignature> signatures = new HashSet<>();
        collectSignatures(token.getMethods(), signatures);

        for (Class<?> tk = token; tk != null; tk = tk.getSuperclass()) {
            collectSignatures(tk.getDeclaredMethods(), signatures);
        }

        return StringUtils.joinBlocks(signatures.stream()
                .map(MethodSignature::getMethod)
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(this::methodToString)
                .toArray(String[]::new));
    }

    /**
     * Returns a {@code String} representing the provided {@code constructor}.
     *
     * @param constructor the {@link Constructor} to represent
     * @return a {@code String} representation of a resulting code block
     * @see #executableToString(Executable, String)
     */
    private String constructorToString(final Constructor<?> constructor) {
        final Parameter[] parameters = constructor.getParameters();
        final String body = String.format(Placeholder.SUPER, generateParameters(parameters, false));
        return executableToString(constructor, body);
    }

    /**
     * Returns a {@code String} representing the provided {@code method}.
     *
     * @param method the {@link Method} to represent
     * @return a {@code String} representation of a resulting code block
     * @see #executableToString(Executable, String)
     */
    private String methodToString(final Method method) {
        final Class<?> returnType = method.getReturnType();
        final String body = String.format(Placeholder.RETURN, getDefaultValue(returnType));
        return executableToString(method, body);
    }

    /**
     * Returns a {@code String} representing a code block
     * with the specified {@code executable} declaration and the given {@code body}.
     *
     * @param executable the {@link Executable} to get declaration of
     * @param body       the {@code String} representing executable body
     * @return a {@code String} representation of a resulting code block
     * @see #generateDeclAndBody(String, String)
     */
    private String executableToString(final Executable executable, final String body) {
        return generateDeclAndBody(generateExecutableDecl(executable), body);
    }

    /**
     * Generates the declaration of the given {@code executable}.
     *
     * @param executable an {@link Executable} to generate declaration for
     * @return a {@code String} representation of declaration
     * @see #generateParameters(Parameter[], boolean)
     * @see #generateExceptions(Class[])
     */
    private String generateExecutableDecl(final Executable executable) {
        final String name;
        final String returnType;

        if (executable instanceof Method) {
            name = executable.getName();
            returnType = ((Method) executable).getReturnType().getCanonicalName();
        } else {
            name = getImplName(executable.getDeclaringClass());
            returnType = StringUtils.EMPTY;
        }

        return StringUtils.join(StringUtils.SPACE,
                Modifier.toString(getAccessModifiers(executable.getModifiers())),
                returnType,
                String.format(Placeholder.EXEC_DECL,
                        name,
                        generateParameters(executable.getParameters(), true),
                        generateExceptions(executable.getExceptionTypes())));
    }

    /**
     * Generates a part of method declaration
     * that throws the specified {@code exceptions}.
     *
     * @param exceptions an array of exception types
     * @return a {@code String} representation of comma separated {@code exceptions}
     * preceded by {@code throws} keyword if not empty
     */
    private String generateExceptions(final Class<?>[] exceptions) {
        return StringUtils.join(StringUtils.COMMA, THROWS, Class::getCanonicalName, exceptions);
    }

    /**
     * Generates a comma separated list of {@code parameters}.
     *
     * @param parameters an array of {@link Parameter}
     * @param typed      whether parameters should be typed
     * @return a {@code String} representation of comma separated
     */
    private String generateParameters(final Parameter[] parameters, final boolean typed) {
        return StringUtils.join(StringUtils.COMMA, typed ? this::getTypedName : Parameter::getName, parameters);
    }

    /**
     * Generates a code block combining provided {@code declaration} and {@code body}.
     * A code block looks like this:
     * <pre>{@code declaration {
     *     body
     * }}
     * </pre>
     *
     * @param declaration the declaration used in a block
     * @param body        the body used in a block
     * @return a {@code String} representation of code block
     * @see Placeholder#DECL_AND_BODY
     */
    private String generateDeclAndBody(final String declaration, final String body) {
        return String.format(Placeholder.DECL_AND_BODY, declaration, StringUtils.tabbed(body));
    }

    /**
     * Filters given modifiers to only access modifiers.
     *
     * @param modifiers an int representing modifiers
     * @return an int value representing some access modifier
     */
    private int getAccessModifiers(final int modifiers) {
        return modifiers & Modifier.constructorModifiers();
    }

    /**
     * Returns the parameter name preceded by the type represented by {@code parameter}.
     *
     * @param parameter the parameter to get typed name for
     * @return a {@code String} representing type and parameter name
     */
    private String getTypedName(final Parameter parameter) {
        return String.join(StringUtils.SPACE, parameter.getType().getCanonicalName(), parameter.getName());
    }

    /**
     * Returns the default value of an object of type {@code token}, which is:
     * <ul>
     *     <li>{@code 0} for numeric primitive types</li>
     *     <li>{@code false} for {@code boolean}</li>
     *     <li>{@code null} for non-primitive types</li>
     *     <li>nothing for {@code void}</li>
     * </ul>
     *
     * @param token the type token of needed object
     * @return the {@code String} representation of default value
     */
    private String getDefaultValue(final Class<?> token) {
        if (!token.isPrimitive()) {
            return "null";
        } else if (boolean.class.equals(token)) {
            return "false";
        } else if (void.class.equals(token)) {
            return "";
        }
        return "0";
    }

    /**
     * Populates the specified {@code signatures}
     * with the {@link MethodSignature} of given {@code methods}.
     *
     * @param methods    an array of methods to create signatures of
     * @param signatures a {@link Set} of method signatures to be populated
     */
    private void collectSignatures(final Method[] methods, final Set<MethodSignature> signatures) {
        Arrays.stream(methods)
                .map(MethodSignature::new)
                .collect(Collectors.toCollection(() -> signatures));
    }

    /**
     * The {@code MethodSignature} class represents the signature of {@link Method}.
     * Contains the method the signature of which it represents.
     * <p>
     * The difference from {@code Method} class is that
     * this class {@link #equals(Object)} and {@link #hashCode()} methods
     * do not consider {@link Method#getDeclaringClass()}.
     */
    private static class MethodSignature {

        /**
         * The method of which this signature is of.
         */
        private final Method method;

        /**
         * Created a new signature of the given {@code method}.
         *
         * @param method to create the signature of
         */
        private MethodSignature(final Method method) {
            this.method = method;
        }

        /**
         * Getter for the {@link #method} .
         *
         * @return the method of which this signature is of
         */
        public Method getMethod() {
            return method;
        }

        /**
         * Compares this {@code MethodSignature} against the specified object.
         * <br>
         * Two method signatures are the same if they have the same name
         * and formal parameter types and return type.
         *
         * @param o the object to compare with
         * @return {@code true} if the objects are the same, {@code false} otherwise
         */
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final MethodSignature that = (MethodSignature) o;
            return method.getName().equals(that.method.getName())
                    && Arrays.equals(method.getParameterTypes(), that.method.getParameterTypes());
        }

        /**
         * Returns a hashcode for this {@code MethodSignature}.
         * The hashcode is computed as {@link Objects#hash(Object...)}
         * of the method name and {@link Arrays#hashCode(Object[])} of parameter types.
         *
         * @return a hash code value for this object
         */
        @Override
        public int hashCode() {
            return Objects.hash(method.getName(), Arrays.hashCode(method.getParameterTypes()));
        }
    }
}
