package com.tonicostmarco.githubpranalyzer.repositories;

import com.tonicostmarco.githubpranalyzer.entities.PrEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PrEventRepository extends MongoRepository<PrEvent, String> {

    boolean existsByDeliveryId(String s);


    Optional<PrEvent> findByDeliveryId(String s);
}
