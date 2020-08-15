package server;

import api.healthcheck.HealthCheckApi;
import api.healthcheck.HealthCheckApiImpl;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import jetty.GuiceApplication;
import jetty.JettyLauncher;

import java.util.Set;

public class Launcher extends JettyLauncher {

    private static class DemoApplicationServiceJettyModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(HealthCheckApi.class).to(HealthCheckApiImpl.class);
        }
    }

    private static class DemoApplicationServiceApplication extends GuiceApplication {

        @Override
        protected Set<Module> modules() {
            return Sets.newHashSet(new DemoApplicationServiceJettyModule());
        }

        @Override
        protected Set<Object> serviceInstances(Injector injector) {

            return Sets.newHashSet(
                    injector.getInstance(HealthCheckApi.class)
            );
        }
    }

    public static void main(String[] args) throws Exception {
        contextPath("/");
        servletPath("/*");
        application(new DemoApplicationServiceApplication());
        port(8080);
        serve();
    }


}
