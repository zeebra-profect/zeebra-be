package com.zeebra.domain.chat.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeebra.ZeebraApplication;
import com.zeebra.domain.chat.dto.ChatRoomRequestDto;
import com.zeebra.domain.chat.entity.ChatRoom;
import com.zeebra.domain.chat.entity.ChatRoomMember;
import com.zeebra.domain.chat.entity.ChatRoomType;
import com.zeebra.domain.chat.repository.ChatRoomMemberRepository;
import com.zeebra.domain.chat.repository.ChatRoomRepository;
import com.zeebra.domain.member.entity.Gender;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.entity.Role;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.zeebra.domain.chat.entity.ChatRoomType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ZeebraApplication.class)
@AutoConfigureMockMvc
@Transactional
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    //테스트 데이터
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired
    private JwtProvider jwtProvider;

    private Member testUser;
    private String accessToken;

    //테스트용 유저 및 토큰 미리 생성
    @BeforeEach
    public void setup() {
        testUser = Member.builder()
                .userLoginId("testUser")
                .nickname("테스트유저")
                .gender(Gender.MAN)
                .birth(LocalDate.now())
                .memberEmail("dd@dd.dd")
                .passwordHash("Test1234@")
                .memberName("test")
                .role(Role.USER).build();

        memberRepository.save(testUser);

        accessToken = jwtProvider.createAccessToken(
                testUser.getId(),
                testUser.getUserLoginId(),
                "USER", 10);
    }

    @Test
    @DisplayName("시나리오 1: 로그인 안 한 유저가 그룹 채팅방 입장")
    void scenario1_Anonymous_EnterGroupChat() throws Exception {
        //Given
        Long productId = 123L;
        Long memberCountBefore = chatRoomMemberRepository.count(); // 입장 전 채팅방 멤버 수

        ChatRoomRequestDto chatRoomRequestDto = new ChatRoomRequestDto();
        chatRoomRequestDto.setProductId(productId);
        chatRoomRequestDto.setChatRoomType(ChatRoomType.GROUP);

        //When
        mockMvc.perform(post("/api/chat/group/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRoomRequestDto)))
        //Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.chatRoomType").value(ChatRoomType.GROUP.name()));

        long memberCountAfter = chatRoomMemberRepository.count(); // 입장 후 채팅방 멤버 수
        assertThat(memberCountAfter).isEqualTo(memberCountBefore);
    }

//    @Test
//    @DisplayName("시나리오 1 (인증): 로그인 한 유저가 그룹 채팅방 입장")
//    void scenario1_Authenticated_EnterGroupChat() throws Exception {
//        Long productId = 123L;
//        ChatRoomRequestDto chatRoomRequestDto = new ChatRoomRequestDto(productId, null, GROUP);
//
//    }


}
