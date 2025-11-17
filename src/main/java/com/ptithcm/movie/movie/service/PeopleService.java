package com.ptithcm.movie.movie.service;

import com.ptithcm.movie.common.constant.PersonJob;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.entity.Person;
import com.ptithcm.movie.movie.repository.PeopleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PeopleService {

    private PeopleRepository peopleRepository;

    /**
     * Logic tìm kiếm People theo tên và công việc (job)
     */
    public ServiceResult searchPeople(String query, PersonJob job) {
        try {
            // Gọi phương thức repository đã định nghĩa
            List<Person> people = peopleRepository.findByFullNameContainingIgnoreCaseAndJob(query, job);

            return ServiceResult.Success()
                    .message("Found " + people.size() + " people.")
                    .data(people);
        } catch (Exception e) {
            return ServiceResult.Failure()
                    .message("Error searching people: " + e.getMessage());
        }
    }
}