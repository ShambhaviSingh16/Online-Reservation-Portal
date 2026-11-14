package com.trainticket.api;

import com.trainticket.integration.RailwayProvider;
import org.springframework.web.bind.annotation.*;

@RestController
public class RailwayProviderApi {
    private final RailwayProvider provider;
    public RailwayProviderApi(RailwayProvider provider){this.provider=provider;}
    @GetMapping("/api/journeys/provider") public RailwayProvider.Capabilities status(){return provider.capabilities();}
}
