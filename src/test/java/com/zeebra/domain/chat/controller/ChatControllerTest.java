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
import com.zeebra.domain.product.entity.Sales;
import com.zeebra.domain.product.entity.SalesStatus;
import com.zeebra.domain.product.repository.SalesRepository;
import com.zeebra.global.security.jwt.JwtProvider;
import com.zeebra.global.web.CookieUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.C;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static com.zeebra.domain.chat.entity.ChatRoomType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ZeebraApplication.class)
@AutoConfigureMockMvc(addFilters = true)
@Transactional
@ActiveProfiles("test")
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

    @Autowired
    private SalesRepository salesRepository;

    private Member testUser;
    private Member testUser2;
    private String accessToken;

    //테스트용 유저 및 토큰 미리 생성
    @BeforeEach
    public void setup() {
        chatRoomRepository.deleteAll();
        chatRoomMemberRepository.deleteAll();

        memberRepository.deleteAll();

        testUser = Member.builder()
                .userLoginId("testUser")
                .nickname("테스트유저")
                .gender(Gender.MAN)
                .birth(LocalDate.now())
                .memberEmail("dd@dd.dd")
                .passwordHash("Test1234@")
                .memberName("test")
                .role(Role.USER).build();

        testUser2 = Member.builder()
                .userLoginId("testUser2")
                .nickname("테스트유저")
                .gender(Gender.MAN)
                .birth(LocalDate.now())
                .memberEmail("dd@dd.dd")
                .passwordHash("Test1234@")
                .memberName("test")
                .role(Role.USER).build();

        memberRepository.save(testUser);
        memberRepository.save(testUser2);

        accessToken = jwtProvider.createAccessToken(
                testUser.getId(),
                testUser.getUserLoginId(),
                "USER", 10);
    }

    @Test
    @DisplayName("시나리오 1-1: 로그인 안 한 유저가 그룹 채팅방 입장")
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

    @Test
    @DisplayName("시나리오 1-2: 로그인 한 유저가 그룹 채팅방 입장")
    void scenario11_Authenticated_EnterGroupChat() throws Exception {
        //Given
        Long productId = 123L;

        ChatRoomRequestDto chatRoomRequestDto = new ChatRoomRequestDto();
        chatRoomRequestDto.setProductId(productId);
        chatRoomRequestDto.setChatRoomType(GROUP);

        String accessToken = jwtProvider.createAccessToken(testUser.getId(), "testUser", "USER", 10);
        Cookie authCookie = new Cookie("__Host-AT", accessToken);

        //When
        mockMvc.perform(post("/api/chat/group/rooms")
                        .cookie(authCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatRoomRequestDto)))

        //Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.chatRoomType").value(ChatRoomType.GROUP.name()));

        //검증 유저가 그룹채팅멤버에 포함되는지
        ChatRoom chatRoom = chatRoomRepository.findByProductId(productId).get();

        Optional<ChatRoomMember> member = chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoom.getId(), testUser.getId());
        assertThat(member).isPresent();
    }

    @Test
    @DisplayName("시나리오 1-2 (인증): 로그인 한 유저가 1:1 채팅방 입장")
    void scenario1_Authenticated_EnterGroupChat() throws Exception {
        //Given
        Member seller = testUser2; // @BeforeEach에서 이미 save 됨
        Member buyer  = testUser;

        Sales fakeSale = new Sales(
                1L, // productOptionId
                seller.getId(), //  판매자(testUser2)의 ID
                new BigDecimal("10000"),
                10,
                SalesStatus.ON_SALE
        );

        Sales savedSale = salesRepository.save(fakeSale);
        Long fakeSaleId = savedSale.getId();

        ChatRoomRequestDto chatRoomRequestDto = new ChatRoomRequestDto();
        chatRoomRequestDto.setSaleId(fakeSaleId);
        chatRoomRequestDto.setChatRoomType(ChatRoomType.DM);

        long memberCountBefore = chatRoomMemberRepository.count();

        Cookie authCookie = new Cookie("__Host-AT", accessToken);


        //When
        mockMvc.perform(post("/api/chat/dm/rooms")
                        .with(csrf())
                        .cookie(authCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatRoomRequestDto)))

        //Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saleId").value(fakeSaleId))
                .andExpect(jsonPath("$.data.chatRoomType").value(ChatRoomType.DM.name()));

        long memberCountAfter = chatRoomMemberRepository.count();
        assertThat(memberCountAfter).isEqualTo(memberCountBefore + 2);
    }

    @Test
    @DisplayName("시나리오 2: 로그인 안 한 유저가 그룹 채팅방 메세지 조회")
    void scenario2_Anonymous_GetGroupChatHistory() throws Exception {
        //Given
        ChatRoom chatRoom = chatRoomRepository.save(
                ChatRoom.builder().productId(123L).chatRoomType(ChatRoomType.GROUP).build());

                ChatRoomMember anonymousUser = chatRoomMemberRepository.save(
                        ChatRoomMember.builder().chatRoom(chatRoom).memberId(999L).memberName("").build());

        //When
        mockMvc.perform(get("/api/chat/group/rooms/"+chatRoom.getId() + "/messages")
                .param("page","0"))

        //Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());



    }
}
