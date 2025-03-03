package com.comeon.courseservice.domain.courseplace.service;

import com.comeon.courseservice.common.exception.CustomException;
import com.comeon.courseservice.common.exception.ErrorCode;
import com.comeon.courseservice.domain.common.exception.EntityNotFoundException;
import com.comeon.courseservice.domain.course.entity.Course;
import com.comeon.courseservice.domain.course.repository.CourseRepository;
import com.comeon.courseservice.domain.courseplace.entity.CoursePlace;
import com.comeon.courseservice.domain.courseplace.service.dto.CoursePlaceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class CoursePlaceService {

    private final CourseRepository courseRepository;

    public void batchUpdateCoursePlace(Long courseId, Long userId,
                                       List<CoursePlaceDto> dtosToSave,
                                       List<CoursePlaceDto> dtosToModify,
                                       List<Long> coursePlaceIdsToDelete) {

        Course course = getCourse(courseId);

        checkWriter(userId, course);

        List<CoursePlace> coursePlaces = course.getCoursePlaces();

        // 삭제
        coursePlaceIdsToDelete.forEach(
                coursePlaceId -> coursePlaces.removeIf(coursePlace -> coursePlace.getId().equals(coursePlaceId))
        );

        // 수정
        dtosToModify.forEach(
                coursePlaceDto -> coursePlaces.stream()
                        .filter(coursePlace -> coursePlace.getId().equals(coursePlaceDto.getCoursePlaceId()))
                        .findFirst()
                        .ifPresent(coursePlace -> modify(coursePlace, coursePlaceDto))
        );

        // 등록
        dtosToSave.forEach(coursePlaceDto -> coursePlaceDto.toEntity(course));

        course.updateCourseState();
    }


    /* === private method === */
    private Course getCourse(Long courseId) {
        return courseRepository.findByIdFetchCoursePlaces(courseId)
                .orElseThrow(
                        () -> new EntityNotFoundException("해당 식별자를 가진 Course가 없습니다. 요청한 Course 식별값 : " + courseId)
                );
    }

    private void checkWriter(Long userId, Course course) {
        if (!Objects.equals(course.getUserId(), userId)) {
            throw new CustomException("해당 코스에 장소를 등록 할 권한이 없습니다. 요청한 유저 식별값 : " + userId, ErrorCode.NO_AUTHORITIES);
        }
    }

    private void modify(CoursePlace coursePlace, CoursePlaceDto coursePlaceDto) {
        if (Objects.nonNull(coursePlaceDto.getName())) {
            coursePlace.updateName(coursePlaceDto.getName());
        }

        if (Objects.nonNull(coursePlaceDto.getDescription())) {
            coursePlace.updateDescription(coursePlaceDto.getDescription());
        }

        if (Objects.nonNull(coursePlaceDto.getLat())) {
            coursePlace.updateLat(coursePlaceDto.getLat());
        }

        if (Objects.nonNull(coursePlaceDto.getLng())) {
            coursePlace.updateLng(coursePlaceDto.getLng());
        }

        if (Objects.nonNull(coursePlaceDto.getOrder())) {
            coursePlace.updateOrder(coursePlaceDto.getOrder());
        }

        if (Objects.nonNull(coursePlaceDto.getKakaoPlaceId())) {
            coursePlace.updateKakaoPlaceId(coursePlaceDto.getKakaoPlaceId());
        }

        if (Objects.nonNull(coursePlaceDto.getPlaceCategory())) {
            coursePlace.updatePlaceCategory(coursePlaceDto.getPlaceCategory());
        }
    }
}
