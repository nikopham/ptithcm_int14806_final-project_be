package com.ptithcm.movie.movie.repository;


import com.ptithcm.movie.common.constant.PersonJob;
import com.ptithcm.movie.movie.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PeopleRepository extends JpaRepository<Person, UUID> {

}