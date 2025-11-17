package com.ptithcm.movie.movie.repository;


import com.ptithcm.movie.common.constant.PersonJob;
import com.ptithcm.movie.movie.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PeopleRepository extends JpaRepository<Person, UUID> {

    /**
     * Tự động tạo query:
     * SELECT * FROM people
     * WHERE full_name ILIKE %:query%  (ILIKE = không phân biệt hoa thường)
     * AND job = :job
     */
    List<Person> findByFullNameContainingIgnoreCaseAndJob(String query, PersonJob job);
}