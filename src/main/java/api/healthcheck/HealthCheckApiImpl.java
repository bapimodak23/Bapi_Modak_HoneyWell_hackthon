

package api.healthcheck;

public class HealthCheckApiImpl implements HealthCheckApi {

    @Override
    public String checkHealth() {
        return "OK";
    }

}
