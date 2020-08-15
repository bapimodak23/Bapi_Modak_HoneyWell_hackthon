package jetty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import java.util.Map;

final class JettyServerModule extends AbstractModule {

    private final Map<String, String> props;

    JettyServerModule(Map<String, String> props) {
        this.props = props;
    }

    @Override
    protected void configure() {
        Names.bindProperties(binder(), props);

        ObjectMapper objectMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JacksonJsonProvider jsonProvider = new JacksonJsonProvider(objectMapper);

        bind(ObjectMapper.class).toInstance(objectMapper);
        bind(JacksonJsonProvider.class).toInstance(jsonProvider);
    }

}
