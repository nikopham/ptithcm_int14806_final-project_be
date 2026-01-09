package com.ptithcm.movie.movie.repository;


import com.ptithcm.movie.movie.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PersonRepository extends
        JpaRepository<Person, UUID>,
        JpaSpecificationExecutor<Person> {
    @Query(value = "SELECT person_id, COUNT(*) FROM movie_actors WHERE person_id IN :ids GROUP BY person_id", nativeQuery = true)
    List<Object[]> countMoviesByActorIds(@Param("ids") List<UUID> ids);

        @Query(value = "SELECT person_id, COUNT(*) FROM movie_directors WHERE person_id IN :ids GROUP BY person_id", nativeQuery = true)
    List<Object[]> countMoviesByDirectorIds(@Param("ids") List<UUID> ids);

    @Modifying
    @Query(value = "DELETE FROM movie_actors WHERE person_id = :personId", nativeQuery = true)
    void deleteMovieActorRelations(@Param("personId") UUID personId);

    @Modifying
    @Query(value = "DELETE FROM movie_directors WHERE person_id = :personId", nativeQuery = true)
    void deleteMovieDirectorRelations(@Param("personId") UUID personId);
}