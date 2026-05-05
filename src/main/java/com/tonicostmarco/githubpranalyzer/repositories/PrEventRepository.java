package com.tonicostmarco.githubpranalyzer.repositories;

import com.tonicostmarco.githubpranalyzer.entities.PrEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PrEventRepository extends MongoRepository<PrEvent, String> {

    boolean existsByDeliveryId(String s);
}
