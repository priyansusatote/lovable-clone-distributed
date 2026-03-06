package com.priyansu.distributed_lovable.api_gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityProperties {

    private List<String> publicRoutes;   //get all the public-routes from api-gateway.yml
}
