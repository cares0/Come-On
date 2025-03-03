package com.comeon.courseservice.web.course.query;

import com.comeon.courseservice.common.exception.CustomException;
import com.comeon.courseservice.common.exception.ErrorCode;
import com.comeon.courseservice.domain.common.exception.EntityNotFoundException;
import com.comeon.courseservice.domain.course.entity.Course;
import com.comeon.courseservice.domain.course.entity.CourseStatus;
import com.comeon.courseservice.web.common.file.FileManager;
import com.comeon.courseservice.web.common.response.SliceResponse;
import com.comeon.courseservice.web.course.query.repository.CourseLikeQueryRepository;
import com.comeon.courseservice.web.course.query.repository.cond.CourseCondition;
import com.comeon.courseservice.web.course.query.repository.cond.MyCourseCondition;
import com.comeon.courseservice.web.course.query.repository.dto.CourseListData;
import com.comeon.courseservice.web.course.query.repository.CourseQueryRepository;
import com.comeon.courseservice.web.course.query.repository.dto.MyPageCourseListData;
import com.comeon.courseservice.web.course.response.CourseDetailResponse;
import com.comeon.courseservice.web.course.response.CourseListResponse;
import com.comeon.courseservice.web.course.response.MyPageCourseListResponse;
import com.comeon.courseservice.web.course.response.UserDetailInfo;
import com.comeon.courseservice.web.feign.userservice.UserFeignService;
import com.comeon.courseservice.web.feign.userservice.response.UserDetailsResponse;
import com.comeon.courseservice.web.feign.userservice.response.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryService {

    @Value("${s3.folder-name.course}")
    private String dirName;

    private final FileManager fileManager;

    private final UserFeignService userFeignService;

    private final CourseQueryRepository courseQueryRepository;
    private final CourseLikeQueryRepository courseLikeQueryRepository;

    public CourseStatus getCourseStatus(Long courseId) {
        return courseQueryRepository.findById(courseId)
                .map(Course::getCourseStatus)
                .orElseThrow(
                        () -> new EntityNotFoundException("해당 식별값의 코스가 존재하지 않습니다. 요청한 코스 식별값 : " + courseId)
                );
    }

    public CourseDetailResponse getCourseDetails(Long courseId, Long userId) {
        Course course = courseQueryRepository.findByIdFetchAll(courseId)
                .orElseThrow(
                        () -> new EntityNotFoundException("해당 식별값의 코스가 존재하지 않습니다. 요청한 코스 식별값 : " + courseId)
                );

        // 해당 코스 작성자가 아니라면, 작성 완료되지 않은 코스는 조회 X
        if (!(course.getUserId().equals(userId) || course.isWritingComplete())) {
            throw new CustomException("작성 완료되지 않은 코스입니다. 요청한 코스 식별값 : " + courseId, ErrorCode.CAN_NOT_ACCESS_RESOURCE);
        }

        // 코스 작성자 닉네임 가져오기
        UserDetailInfo userDetailInfo = getUserDetailInfo(course.getUserId());

        // 코스 이미지 처리
        String fileUrl = getCourseImageUrl(course.getCourseImage().getStoredName());

        // 코스 좋아요 조회
        boolean userLiked = doesUserLikeCourse(userId, course);

        return new CourseDetailResponse(course, userDetailInfo, fileUrl, userLiked);
    }

    // 코스 리스트 조회
    public SliceResponse<CourseListResponse> getCourseList(Long userId,
                                                           CourseCondition courseCondition,
                                                           Pageable pageable) {
        Slice<CourseListData> courseSlice = courseQueryRepository.findCourseSlice(userId, courseCondition, pageable);

        // 조회 결과에서 작성자 id 리스트 추출
        List<Long> writerIds = courseSlice.getContent().stream()
                .map(courseListData -> courseListData.getCourse().getUserId())
                .distinct()
                .collect(Collectors.toList());

        // 작성자 id 리스트로 유저들 정보 조회
        Map<Long, UserDetailInfo> userDetailInfoMap = getUserDetailInfoMap(writerIds);

        Slice<CourseListResponse> courseListResponseSlice = courseSlice.map(
                courseListData -> CourseListResponse.builder()
                        .course(courseListData.getCourse())
                        .coursePlace(courseListData.getCoursePlace())
                        .firstPlaceDistance(courseListData.getDistance())
                        .imageUrl(getCourseImageUrl(courseListData.getCourse().getCourseImage().getStoredName()))
                        .writer(
                                userDetailInfoMap.getOrDefault(
                                        courseListData.getCourse().getUserId(),
                                        new UserDetailInfo(courseListData.getCourse().getUserId(), null)
                                )
                        )
                        .userLiked(Objects.nonNull(courseListData.getUserLikeId()))
                        .build()
        );

        return SliceResponse.toSliceResponse(courseListResponseSlice);
    }

    // 유저가 등록한 코스 리스트 조회
    public SliceResponse<MyPageCourseListResponse> getMyRegisteredCourseList(Long userId,
                                                                             MyCourseCondition condition,
                                                                             Pageable pageable) {
        Slice<MyPageCourseListData> myCourseSlice = courseQueryRepository.findMyCourseSlice(userId, condition, pageable);

        // 유저 닉네임 조회
        UserDetailInfo userDetailInfo = getUserDetailInfo(userId);

        Slice<MyPageCourseListResponse> myCourseListResponseSlice = myCourseSlice.map(
                myPageCourseListData -> MyPageCourseListResponse.builder()
                        .course(myPageCourseListData.getCourse())
                        .imageUrl(getCourseImageUrl(myPageCourseListData.getCourse().getCourseImage().getStoredName()))
                        .writer(userDetailInfo)
                        .userLiked(Objects.nonNull(myPageCourseListData.getUserLikeId()))
                        .build()
        );

        return SliceResponse.toSliceResponse(myCourseListResponseSlice);
    }

    // 유저가 좋아요한 코스 리스트 조회
    public SliceResponse<MyPageCourseListResponse> getMyLikedCourseList(Long userId, Pageable pageable) {
        // 코스 리스트 조회
        Slice<MyPageCourseListData> myLikedCourseSlice = courseQueryRepository.findMyLikedCourseSlice(userId, pageable);

        // 조회 결과에서 작성자 id 리스트 추출
        List<Long> writerIds = myLikedCourseSlice.getContent().stream()
                .map(courseListData -> courseListData.getCourse().getUserId())
                .distinct()
                .collect(Collectors.toList());

        // 작성자 id 리스토로 유저 닉네임 조회
        Map<Long, UserDetailInfo> userDetailInfoMap = getUserDetailInfoMap(writerIds);

        // 응답값 변환
        Slice<MyPageCourseListResponse> myLikedCourseListResponseSlice = myLikedCourseSlice.map(
                courseListData -> MyPageCourseListResponse.builder()
                        .course(courseListData.getCourse())
                        .imageUrl(getCourseImageUrl(courseListData.getCourse().getCourseImage().getStoredName()))
                        .writer(
                                userDetailInfoMap.getOrDefault(
                                        courseListData.getCourse().getUserId(),
                                        new UserDetailInfo(courseListData.getCourse().getUserId(), null)
                                )
                        )
                        .userLiked(Objects.nonNull(courseListData.getUserLikeId()))
                        .build()
        );

        return SliceResponse.toSliceResponse(myLikedCourseListResponseSlice);
    }

    public String getStoredFileName(Long courseId) {
        return courseQueryRepository.findByIdFetchCourseImg(courseId)
                .orElseThrow(
                        () -> new EntityNotFoundException("해당 식별값의 코스가 존재하지 않습니다. 요청한 코스 식별값 : " + courseId)
                )
                .getCourseImage()
                .getStoredName();
    }

    private UserDetailInfo getUserDetailInfo(Long userId) {
        UserDetailsResponse detailsResponse = userFeignService.getUserDetails(userId).orElse(null);

        String userNickname = null;
        if (Objects.nonNull(detailsResponse)) {
            if (detailsResponse.getStatus().equals(UserStatus.WITHDRAWN)) {
                userNickname = "탈퇴한 회원입니다.";
            } else {
                userNickname = detailsResponse.getNickname();
            }
        }

        return new UserDetailInfo(userId, userNickname);
    }

    private Map<Long, UserDetailInfo> getUserDetailInfoMap(List<Long> userIds) {
        // userStatus == WITHDRAWN 이면 탈퇴한 회원 처리
        return userFeignService.getUserDetailsMap(userIds)
                .entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry ->
                                        new UserDetailInfo(
                                                entry.getValue().getUserId(),
                                                entry.getValue().getStatus().equals(UserStatus.WITHDRAWN)
                                                        ? "탈퇴한 회원입니다."
                                                        : entry.getValue().getNickname()
                                        )
                        )
                );

    }

    private boolean doesUserLikeCourse(Long userId, Course course) {
        if (userId != null) {
            return courseLikeQueryRepository.findByCourseAndUserId(course, userId).isPresent();
        }
        return false;
    }

    private String getCourseImageUrl(String storedFileName) {
        return fileManager.getFileUrl(storedFileName, dirName);
    }
}
