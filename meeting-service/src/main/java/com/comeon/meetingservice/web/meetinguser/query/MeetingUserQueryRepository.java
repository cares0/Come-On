package com.comeon.meetingservice.web.meetinguser.query;

import com.comeon.meetingservice.domain.meetinguser.entity.MeetingUserEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.comeon.meetingservice.domain.meetinguser.entity.QMeetingUserEntity.*;

@Repository
@RequiredArgsConstructor
public class MeetingUserQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<MeetingUserEntity> findAllByMeetingId(Long meetingId) {
        return queryFactory
                .selectFrom(meetingUserEntity)
                .where(meetingUserEntity.meetingEntity.id.eq(meetingId))
                .fetch();
    }

}
