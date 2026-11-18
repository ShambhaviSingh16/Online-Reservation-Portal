package com.trainticket.service;

import com.trainticket.model.*;
import com.trainticket.persistence.entity.*;
import com.trainticket.persistence.repository.*;
import jakarta.persistence.criteria.Expression;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** JPA handles catalog records; reservation transactions remain in the JDBC DAOs. */
@Service
@Transactional(readOnly = true)
public class CatalogService {
    private static final Set<String> FILTERS = Set.of("all", "active", "archived");
    private final TrainRepository trains;
    private final StationRepository stations;

    public CatalogService(TrainRepository trains, StationRepository stations) {
        this.trains = trains;
        this.stations = stations;
    }

    public List<Train> getAllTrains() {
        return trains.findByActiveTrueOrderByIdAsc().stream().map(CatalogService::trainDto).toList();
    }

    public List<Station> getAllStations() {
        return stations.findByActiveTrueOrderByIdAsc().stream().map(CatalogService::stationDto).toList();
    }

    public Station getStationById(int id) {
        return stations.findByIdAndActiveTrue(id).map(CatalogService::stationDto).orElse(null);
    }

    @Transactional
    public boolean addTrain(Train input, int adminId) {
        validateTrain(input);
        if (trains.existsByNumber(input.getTrainNumber())) return false;
        TrainEntity entity = new TrainEntity();
        copyTrain(input, entity);
        entity.setCreatedBy(adminId);
        trains.saveAndFlush(entity);
        return true;
    }

    @Transactional
    public boolean updateTrain(Train input) {
        validateTrain(input);
        TrainEntity entity = trains.lockById(input.getTrainId()).orElse(null);
        if (entity == null || !entity.isActive()) return false;
        if (trains.existsByNumberAndIdNot(input.getTrainNumber(), input.getTrainId())) return false;
        copyTrain(input, entity); // Hibernate dirty checking flushes at transaction commit.
        return true;
    }

    @Transactional
    public boolean deleteTrain(int id) {
        TrainEntity entity = trains.lockById(id).orElse(null);
        if (entity == null || !entity.isActive()) return false;
        entity.setActive(false); // Archive, preserving journey and ticket foreign keys.
        return true;
    }

    @Transactional
    public boolean addStation(Station input) {
        validateStation(input);
        if (stations.existsByCode(input.getStationCode())) return false;
        StationEntity entity = new StationEntity();
        copyStation(input, entity);
        stations.saveAndFlush(entity);
        return true;
    }

    @Transactional
    public boolean updateStation(Station input) {
        validateStation(input);
        StationEntity entity = stations.lockById(input.getStationId()).orElse(null);
        if (entity == null || !entity.isActive()) return false;
        if (stations.existsByCodeAndIdNot(input.getStationCode(), input.getStationId())) return false;
        copyStation(input, entity);
        return true;
    }

    @Transactional
    public boolean deleteStation(int id) {
        StationEntity entity = stations.lockById(id).orElse(null);
        if (entity == null || !entity.isActive()) return false;
        entity.setActive(false);
        return true;
    }

    public Map<String, Object> search(String type, String q, String filter, int page, int size) {
        if (!FILTERS.contains(filter) || q == null || q.length() > 100 || page < 0 || page > 100000 || size < 1 || size > 100)
            throw new IllegalArgumentException("Invalid catalog search");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Map<String, Object>> result;
        switch (type) {
            case "trains" -> result = trains.findAll(matches(q, filter, List.of("number", "name")), pageable)
                .map(t -> row("id", t.getId(), "train_number", t.getNumber(), "train_name", t.getName(),
                    "total_seats", t.getTotalSeats(), "is_active", t.isActive()));
            case "stations" -> result = stations.findAll(matches(q, filter, List.of("code", "name", "city", "state")), pageable)
                .map(s -> row("id", s.getId(), "station_code", s.getCode(), "station_name", s.getName(),
                    "city", s.getCity(), "state", s.getState(), "is_active", s.isActive()));
            default -> throw new IllegalArgumentException("Unsupported catalog resource");
        }
        return Map.of("items", result.getContent(), "total", result.getTotalElements(),
            "page", page, "size", size, "type", type);
    }

    /** Escape LIKE wildcards: searches for '%' and '_' remain literal. */
    private static <T> Specification<T> matches(String query, String filter, List<String> fields) {
        String term = "%" + query.strip().toLowerCase(Locale.ROOT)
            .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return (root, cq, cb) -> {
            Expression<String> text = cb.literal("");
            for (String field : fields) text = cb.concat(cb.concat(text, " "), cb.coalesce(root.<String>get(field), ""));
            var textMatch = cb.like(cb.lower(text), term, '!');
            return filter.equals("all") ? textMatch : cb.and(textMatch, cb.equal(root.get("active"), filter.equals("active")));
        };
    }

    /** LinkedHashMap preserves column order and permits nullable legacy city/state. */
    private static Map<String, Object> row(Object... values) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) row.put((String) values[i], values[i + 1]);
        return row;
    }

    private static Train trainDto(TrainEntity t) {
        return new Train(t.getId(), t.getNumber(), t.getName(), t.getTotalSeats());
    }
    private static Station stationDto(StationEntity s) {
        return new Station(s.getId(), s.getCode(), s.getName(), s.getCity(), s.getState());
    }
    private static void copyTrain(Train input, TrainEntity entity) {
        entity.setNumber(input.getTrainNumber()); entity.setName(input.getTrainName()); entity.setTotalSeats(input.getTotalSeats());
    }
    private static void copyStation(Station input, StationEntity entity) {
        entity.setCode(input.getStationCode()); entity.setName(input.getStationName()); entity.setCity(input.getCity()); entity.setState(input.getState());
    }
    private static void required(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max) throw new IllegalArgumentException("Invalid catalog fields");
    }
    private static void validateTrain(Train input) {
        required(input.getTrainNumber(), 30); required(input.getTrainName(), 200);
        if (input.getTotalSeats() < 1 || input.getTotalSeats() > 2000) throw new IllegalArgumentException("Train capacity must be 1-2000");
    }
    private static void validateStation(Station input) {
        required(input.getStationCode(), 20); required(input.getStationName(), 200);
        required(input.getCity(), 100); required(input.getState(), 100);
        if (!input.getStationCode().matches("[A-Za-z0-9]{2,20}")) throw new IllegalArgumentException("Invalid station code");
        input.setStationCode(input.getStationCode().toUpperCase(Locale.ROOT));
    }
}
