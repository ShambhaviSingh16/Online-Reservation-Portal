package com.trainticket.integration;

import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;

@Configuration
public class UnconfiguredRailwayProvider {
    @Bean @ConditionalOnMissingBean(RailwayProvider.class)
    RailwayProvider railwayProvider(){return new RailwayProvider(){
        public Capabilities capabilities(){return new Capabilities("No external provider",false,false,false,
            "TrackEase uses its own demonstration schedules. Live integration requires an authorized provider, its documented API, and server-side credentials.");}
        public List<Timetable> search(String departure,String arrival,LocalDate date){throw new IllegalStateException("An authorized railway provider has not been configured");}
    };}
}
