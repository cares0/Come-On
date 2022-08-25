package com.comeon.meetingservice.web.meetingdate;

import com.comeon.meetingservice.common.exception.ErrorCode;
import com.comeon.meetingservice.web.ControllerTest;
import com.comeon.meetingservice.web.meetingdate.request.MeetingDateAddRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.beneathPath;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MeetingDateControllerTest extends ControllerTest {

    String duplicatedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjIsIm5hbWUiOiJKb2huIERvZ" +
            "SIsImlhdCI6MTUxNjIzOTAyMn0.RPxUhKwz-RU-s0qmttmh2QoP3j1pU-EUnAX74B94nD8";

    String notJoinedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjEwMCwibmFtZSI6IkpvaG4gRG" +
            "9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.XdpobbYZskeuceElG_LNbbstM9w-N2SYNSZdvQa7a-c";

    @Nested
    @DisplayName("모임날짜 저장")
    class 모임날짜저장 {

        @Test
        @DisplayName("모든 필수 데이터가 넘어온 경우 Created코드와 저장된, 혹은 영향을 받은 ID를 응답한다.")
        @Sql(value = "classpath:static/test-dml/meeting-insert.sql", executionPhase = BEFORE_TEST_METHOD)
        @Sql(value = "classpath:static/test-dml/meeting-delete.sql", executionPhase = AFTER_TEST_METHOD)
        public void 정상_흐름() throws Exception {

            MeetingDateAddRequest meetingDateAddRequest =
                    MeetingDateAddRequest.builder()
                            .meetingId(10L)
                            .date(LocalDate.of(2022, 07, 15))
                            .build();

            mockMvc.perform(post("/meeting-dates")
                            .header("Authorization", sampleToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson(meetingDateAddRequest))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andDo(document("date-create-normal",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("meetingId").description("날짜를 추가할 모임의 ID"),
                                    fieldWithPath("date").description("추가할 날짜").attributes(new Attributes.Attribute("format", "yyyy-MM-dd"))
                            ))
                    )
            ;
        }

        @Test
        @DisplayName("날짜가 모임 기간내에 없다면 예외가 발생한다.")
        @Sql(value = "classpath:static/test-dml/meeting-insert.sql", executionPhase = BEFORE_TEST_METHOD)
        @Sql(value = "classpath:static/test-dml/meeting-delete.sql", executionPhase = AFTER_TEST_METHOD)
        public void 날짜_예외() throws Exception {

            MeetingDateAddRequest meetingDateAddRequest =
                    MeetingDateAddRequest.builder()
                            .meetingId(10L)
                            .date(LocalDate.of(2022, 8, 15))
                            .build();

            mockMvc.perform(post("/meeting-dates")
                            .header("Authorization", sampleToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson(meetingDateAddRequest))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.code", equalTo(ErrorCode.DATE_NOT_WITHIN_PERIOD.getCode())))
                    .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.DATE_NOT_WITHIN_PERIOD.getMessage())))
                    .andDo(document("date-create-error-date",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("meetingId").description("날짜를 추가할 모임의 ID"),
                                    fieldWithPath("date").description("추가할 날짜").attributes(new Attributes.Attribute("format", "yyyy-MM-dd"))
                            ),
                            responseFields(beneathPath("data").withSubsectionId("data"),
                                    fieldWithPath("code").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("예외 메시지")
                            ))
                    )
            ;
        }

        @Test
        @DisplayName("날짜를 등록하려는 모임이 없는 경우 예외가 발생한다.")
        @Sql(value = "classpath:static/test-dml/meeting-insert.sql", executionPhase = BEFORE_TEST_METHOD)
        @Sql(value = "classpath:static/test-dml/meeting-delete.sql", executionPhase = AFTER_TEST_METHOD)
        public void 모임_예외() throws Exception {

            MeetingDateAddRequest meetingDateAddRequest =
                    MeetingDateAddRequest.builder()
                            .meetingId(5L)
                            .date(LocalDate.of(2022, 07, 15))
                            .build();

            mockMvc.perform(post("/meeting-dates")
                            .header("Authorization", sampleToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson(meetingDateAddRequest))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.code", equalTo(ErrorCode.ENTITY_NOT_FOUND.getCode())))
                    .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.ENTITY_NOT_FOUND.getMessage())))
                    .andDo(document("date-create-error-meetingId",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("meetingId").description("날짜를 추가할 모임의 ID"),
                                    fieldWithPath("date").description("추가할 날짜").attributes(new Attributes.Attribute("format", "yyyy-MM-dd"))
                            ),
                            responseFields(beneathPath("data").withSubsectionId("data"),
                                    fieldWithPath("code").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("예외 메시지")
                            ))
                    )
            ;
        }

        @Test
        @DisplayName("이미 해당 회원이 해당 날짜를 선택했다면 예외가 발생한다.")
        @Sql(value = "classpath:static/test-dml/meeting-insert.sql", executionPhase = BEFORE_TEST_METHOD)
        @Sql(value = "classpath:static/test-dml/meeting-delete.sql", executionPhase = AFTER_TEST_METHOD)
        public void 중복_예외() throws Exception {

            MeetingDateAddRequest meetingDateAddRequest =
                    MeetingDateAddRequest.builder()
                            .meetingId(10L)
                            .date(LocalDate.of(2022, 07, 20))
                            .build();

            mockMvc.perform(post("/meeting-dates")
                            .header("Authorization", duplicatedToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson(meetingDateAddRequest))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.code", equalTo(ErrorCode.USER_ALREADY_SELECT.getCode())))
                    .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.USER_ALREADY_SELECT.getMessage())))
                    .andDo(document("date-create-error-duplicate",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("meetingId").description("날짜를 추가할 모임의 ID"),
                                    fieldWithPath("date").description("추가할 날짜").attributes(new Attributes.Attribute("format", "yyyy-MM-dd"))
                            ),
                            responseFields(beneathPath("data").withSubsectionId("data"),
                                    fieldWithPath("code").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("예외 메시지")
                            ))
                    )
            ;
        }

        @Test
        @DisplayName("해당 회원이 모임에 가입되어있지 않다면 예외가 발생한다.")
        @Sql(value = "classpath:static/test-dml/meeting-insert.sql", executionPhase = BEFORE_TEST_METHOD)
        @Sql(value = "classpath:static/test-dml/meeting-delete.sql", executionPhase = AFTER_TEST_METHOD)
        public void 미가입_예외() throws Exception {

            MeetingDateAddRequest meetingDateAddRequest =
                    MeetingDateAddRequest.builder()
                            .meetingId(10L)
                            .date(LocalDate.of(2022, 07, 15))
                            .build();

            mockMvc.perform(post("/meeting-dates")
                            .header("Authorization", notJoinedToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson(meetingDateAddRequest))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.code", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getCode())))
                    .andExpect(jsonPath("$.data.message", equalTo(ErrorCode.MEETING_USER_NOT_INCLUDE.getMessage())))
                    .andDo(document("date-create-error-notjoined",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("meetingId").description("날짜를 추가할 모임의 ID"),
                                    fieldWithPath("date").description("추가할 날짜").attributes(new Attributes.Attribute("format", "yyyy-MM-dd"))
                            ),
                            responseFields(beneathPath("data").withSubsectionId("data"),
                                    fieldWithPath("code").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("예외 메시지")
                            ))
                    )
            ;
        }

        @Test
        @DisplayName("필수 데이터가 없다면 예외가 발생한다.")
        @Sql(value = "classpath:static/test-dml/meeting-insert.sql", executionPhase = BEFORE_TEST_METHOD)
        @Sql(value = "classpath:static/test-dml/meeting-delete.sql", executionPhase = AFTER_TEST_METHOD)
        public void 필수값_예외() throws Exception {

            MeetingDateAddRequest meetingDateAddRequest =
                    MeetingDateAddRequest.builder()
                            .meetingId(10L)
                            .build();

            mockMvc.perform(post("/meeting-dates")
                            .header("Authorization", notJoinedToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson(meetingDateAddRequest))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.code", equalTo(ErrorCode.VALIDATION_FAIL.getCode())))
                    .andDo(document("date-create-error-param",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("meetingId").description("날짜를 추가할 모임의 ID"),
                                    fieldWithPath("date").description("추가할 날짜").attributes(new Attributes.Attribute("format", "yyyy-MM-dd"))
                            ),
                            responseFields(beneathPath("data").withSubsectionId("data"),
                                    fieldWithPath("code").type(JsonFieldType.NUMBER).description(errorCodeLink),
                                    fieldWithPath("message").type(JsonFieldType.OBJECT).description("예외 메시지"),
                                    fieldWithPath("message.date").type(JsonFieldType.ARRAY).description("검증에 실패한 이유")
                            ))
                    )
            ;
        }
    }
}