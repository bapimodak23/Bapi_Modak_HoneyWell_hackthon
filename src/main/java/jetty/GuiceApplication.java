

package jetty;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import javax.ws.rs.core.Application;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public abstract class GuiceApplication extends Application {

    private static final String DEFAULT_CONFIG_PROPERTIES_FILE = "/config/config.properties";

    @Override
    public final Set<Class<?>> getClasses() {
        return Collections.emptySet();
    }

    @Override
    public final Set<Object> getSingletons() {
        Injector injector = Guice.createInjector(internalModules());
        return internalSingletons(injector);
    }

    @Override
    public final Map<String, Object> getProperties() {
        return Maps.transformValues(internalProperties(), value -> (Object) value);
    }

    /**
     * Returns instances of JAXRS providers.  Override to return micro-service-specific providers.
     * @param injector The Guice injector from which to obtain instances.
     * @return  A set of provider instances.
     */
    protected Set<Object> providers(Injector injector) {
        return Collections.emptySet();
    }

    /**
     * Returns the names of classpath-relative properties files from which to load configuration.  Note: all properties
     * are merged prior to use, in the order in which the files are given, and overriding the default file,
     * "/config/config.properties".
     * @return The property file names.
     */
    protected Set<String> propertyFiles() {
        return Collections.emptySet();
    }

    /**
     * Returns the set of Guice {@link Module}s used to define dependencies for this micro-service.
     * Note: an instance of {@link JettyServerModule} is automatically added to this set.
     * @return The set of {@link Module}s.
     */
    protected abstract Set<Module> modules();

    /**
     * Returns the set of service implementation instances for this micro-service.
     * @param injector The Guice injector from which to obtain instances.
     * @return  A set of service instances.
     */
    protected abstract Set<Object> serviceInstances(Injector injector);

    private Set<Module> internalModules() {
        Set<Module> modules = modules();
        requireNonNull(modules, "Modules may not be null");
        if (modules.isEmpty()) {
            throw new IllegalStateException("Modules may not be empty");
        }

        Set<Module> moduleses = new HashSet<>(modules);
        moduleses.add(new JettyServerModule(internalProperties()));
        return moduleses;
    }

    private Set<Object> internalSingletons(Injector injector) {
        Set<Object> serviceInstances = serviceInstances(injector);
        requireNonNull(serviceInstances, "Service Instances may not be null");
        if (serviceInstances.isEmpty()) {
            throw new IllegalStateException("Service Instances may not be empty");
        }

        Set<Object> singletons = new HashSet<>(serviceInstances);
        singletons.add(injector.getInstance(JacksonJsonProvider.class));
        singletons.addAll(providers(injector));

        return singletons;
    }

    private Map<String,String> internalProperties() {
        Map<String, String> allProperties = new HashMap<>();
        allProperties.putAll(properties(DEFAULT_CONFIG_PROPERTIES_FILE));

        Set<String> propertyFiles = propertyFiles();
        for (String propertyFile : propertyFiles) {
            allProperties.putAll(properties(propertyFile));
        }
        return allProperties;
    }

    private Map<String, String> properties(String configFile) {
        InputStream in = this.getClass().getResourceAsStream(configFile);
        Properties properties = new Properties();
        try {
            properties.load(in);
        } catch (Exception e) {
            //nop
        }
        return properties.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    }

}
