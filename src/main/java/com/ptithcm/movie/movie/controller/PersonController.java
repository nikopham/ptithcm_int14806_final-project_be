package com.ptithcm.movie.movie.controller;

import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.movie.dto.request.PersonRequest;
import com.ptithcm.movie.movie.dto.request.PersonSearchRequest;
import com.ptithcm.movie.movie.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/people")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @GetMapping("/search")
    public ResponseEntity<ServiceResult> searchPeople(
            @ModelAttribute PersonSearchRequest request,
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(personService.searchPeople(request, pageable));
    }

    @PostMapping(path = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceResult> createPerson(
            @ModelAttribute @Valid PersonRequest request
    ) {
        return ResponseEntity.ok(personService.createPerson(request));
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceResult> updatePerson(
            @PathVariable UUID id,
            @ModelAttribute @Valid PersonRequest request
    ) {
        return ResponseEntity.ok(personService.updatePerson(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ServiceResult> deletePerson(@PathVariable UUID id) {
        return ResponseEntity.ok(personService.deletePerson(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResult> getPersonDetail(
            @PathVariable UUID id,
            @PageableDefault(size = 12, sort = "releaseDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(personService.getPersonDetail(id, pageable));
    }
}