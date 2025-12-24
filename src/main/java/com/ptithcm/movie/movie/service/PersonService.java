package com.ptithcm.movie.movie.service;

import com.ptithcm.movie.common.constant.ErrorCode;
import com.ptithcm.movie.common.dto.ServiceResult;
import com.ptithcm.movie.external.cloudinary.CloudinaryService;
import com.ptithcm.movie.external.meili.SearchService;
import com.ptithcm.movie.movie.dto.request.PersonRequest;
import com.ptithcm.movie.movie.dto.request.PersonSearchRequest;
import com.ptithcm.movie.movie.dto.response.MovieSearchResponse;
import com.ptithcm.movie.movie.dto.response.PersonDetailResponse;
import com.ptithcm.movie.movie.dto.response.PersonResponse;
import com.ptithcm.movie.movie.entity.Movie;
import com.ptithcm.movie.movie.entity.Person;
import com.ptithcm.movie.movie.repository.MovieRepository;
import com.ptithcm.movie.movie.repository.PersonRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final CloudinaryService cloudinaryService;
    private final SearchService searchService;
    private final MovieRepository movieRepository;

    @Transactional(readOnly = true)
    public ServiceResult getPersonDetail(UUID personId, Pageable pageable) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Person not found"));


        Page<Movie> moviePage = movieRepository.findByPersonId(personId, pageable);

        Page<MovieSearchResponse> movieDtos = moviePage.map(movie -> MovieSearchResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .originalTitle(movie.getOriginalTitle())
                .description(movie.getDescription())
                .slug(movie.getSlug())

                .posterUrl(movie.getPosterUrl())
                .backdropUrl(movie.getBackdropUrl())

                .releaseDate(movie.getReleaseDate())
                .releaseYear(movie.getReleaseDate() != null ? movie.getReleaseDate().getYear() : null)
                .durationMin(movie.getDurationMin())
                .ageRating(String.valueOf(movie.getAgeRating()))
                .quality(movie.getQuality())
                .status(String.valueOf(movie.getStatus()))
                .isSeries(movie.isSeries())
                .build());

        PersonDetailResponse response = PersonDetailResponse.builder()
                .id(person.getId())
                .fullName(person.getFullName())
                .biography(person.getBiography())
                .birthDate(person.getBirthDate())
                .placeOfBirth(person.getPlaceOfBirth())
                .profilePath(person.getProfilePath())
                .job(person.getJob())
                .movies(movieDtos)
                .build();

        return ServiceResult.Success().data(response);
    }

    public ServiceResult searchPeople(PersonSearchRequest request, Pageable pageable) {
        Specification<Person> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(request.getQuery())) {
                String searchKey = "%" + request.getQuery().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("fullName")), searchKey));
            }

            if (request.getJob() != null) {
                Expression<Boolean> jsonContains = cb.function(
                        "jsonb_exists",
                        Boolean.class,
                        root.get("job"),
                        cb.literal(request.getJob().name())
                );
                predicates.add(cb.isTrue(jsonContains));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Person> personPage = personRepository.findAll(spec, pageable);

        List<UUID> personIds = personPage.getContent().stream()
                .map(Person::getId)
                .toList();

        Map<UUID, Long> movieCountMap = new HashMap<>();

        if (!personIds.isEmpty()) {
            List<Object[]> actorCounts = personRepository.countMoviesByActorIds(personIds);
            for (Object[] row : actorCounts) {
                UUID id = (UUID) row[0];
                Long count = ((Number) row[1]).longValue();
                movieCountMap.merge(id, count, Long::sum);
            }

            List<Object[]> directorCounts = personRepository.countMoviesByDirectorIds(personIds);
            for (Object[] row : directorCounts) {
                UUID id = (UUID) row[0];
                Long count = ((Number) row[1]).longValue();
                movieCountMap.merge(id, count, Long::sum);
            }
        }

        Page<PersonResponse> responsePage = personPage.map(p -> PersonResponse.builder()
                .id(p.getId())
                .fullName(p.getFullName())
                .job(p.getJob())
                .profilePath(p.getProfilePath())
                .movieCount(movieCountMap.getOrDefault(p.getId(), 0L))
                .build());

        return ServiceResult.Success()
                .code(ErrorCode.SUCCESS)
                .data(responsePage);
    }


    @Transactional
    public ServiceResult createPerson(PersonRequest request) {
        try {
            String avatarUrl = null;
            if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
                avatarUrl = cloudinaryService.uploadImage(request.getAvatar());
            }

            Person person = Person.builder()
                    .fullName(request.getFullName())
                    .job(request.getJob())
                    .profilePath(avatarUrl)
                    .build();

            Person savedPerson = personRepository.save(person);
            searchService.indexPerson(savedPerson);

            return ServiceResult.Success().code(ErrorCode.SUCCESS)
                    .message("Tạo nhân vật thành công")
                    .data(savedPerson);

        } catch (IOException e) {
            return ServiceResult.Failure().code(ErrorCode.FAILED).message("Lỗi khi tải ảnh lên: " + e.getMessage());
        }
    }

    @Transactional
    public ServiceResult updatePerson(UUID id, PersonRequest request) {
        try {
            Person person = personRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân vật"));

            person.setFullName(request.getFullName());
            person.setJob(request.getJob());

            if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
                String newAvatarUrl = cloudinaryService.uploadImage(request.getAvatar());
                person.setProfilePath(newAvatarUrl);
            }

            Person updatedPerson = personRepository.save(person);

            searchService.indexPerson(updatedPerson);

            return ServiceResult.Success().code(ErrorCode.SUCCESS)
                    .message("Cập nhật nhân vật thành công")
                    .data(updatedPerson);

        } catch (IOException e) {
            return ServiceResult.Failure().code(ErrorCode.FAILED).message("Lỗi khi tải ảnh lên: " + e.getMessage());
        }
    }

    @Transactional
    public ServiceResult deletePerson(UUID id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân vật"));

        personRepository.deleteMovieActorRelations(id);
        personRepository.deleteMovieDirectorRelations(id);

        if (person.getProfilePath() != null) {
            cloudinaryService.deleteImage(person.getProfilePath());
        }

        personRepository.delete(person);

        searchService.removePerson(id);

        return ServiceResult.Success()
                .message("Xóa nhân vật thành công");
    }
}
