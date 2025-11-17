package com.zeebra.domain.chat.service;


import com.zeebra.domain.chat.dto.*;
import com.zeebra.domain.chat.entity.*;
import com.zeebra.domain.chat.repository.ChatMessageRepository;
import com.zeebra.domain.chat.repository.ChatRoomMemberRepository;
import com.zeebra.domain.chat.repository.ChatRoomRepository;
import com.zeebra.domain.chat.repository.TradeRepository;
import com.zeebra.domain.member.entity.Gender;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.entity.Role;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.product.entity.Sales;
import com.zeebra.domain.product.entity.SalesStatus;
import com.zeebra.domain.product.repository.SalesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class ChatServiceImplTest {
    @InjectMocks
    private ChatServiceImpl chatService;

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private SalesRepository salesRepository;

    private Member testUser;
    private Member testUser2;
    private Member testUser3;
    private ChatRoomRequestDto groupRequestDto;
    private ChatRoomRequestDto dmRequestDto;
    private ChatMessageRequestDto messageDto;
    private TradeRequestDto tradeRequestDto;

    @BeforeEach
    public void setUp() {
        testUser = Member.builder()
                .userLoginId("testuser1")
                .nickname("test1")
                .memberEmail("dd@dd.dd")
                .birth(LocalDate.now())
                .gender(Gender.MAN)
                .passwordHash("Testuser!234")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testUser2 = Member.builder()
                .userLoginId("testuser2")
                .nickname("test2")
                .memberEmail("dd@dd.dd")
                .birth(LocalDate.now())
                .gender(Gender.MAN)
                .passwordHash("Testuser!234")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(testUser2, "id", 2L);

        testUser3 = Member.builder()
                .userLoginId("testuser3").nickname("test3").memberEmail("dd@dd.dd")
                .birth(LocalDate.now()).gender(Gender.MAN).passwordHash("Testuser!234")
                .role(Role.USER).build();
        ReflectionTestUtils.setField(testUser3, "id", 3L);

        groupRequestDto = new ChatRoomRequestDto();
        groupRequestDto.setProductId(100L);
        groupRequestDto.setChatRoomType(ChatRoomType.GROUP);

        dmRequestDto = new ChatRoomRequestDto();
        dmRequestDto.setSaleId(200L);
        dmRequestDto.setChatRoomType(ChatRoomType.DM);

        messageDto = new ChatMessageRequestDto();
        messageDto.setMessageType(MessageType.TEXT);
        messageDto.setContent("hi");

        tradeRequestDto = new TradeRequestDto(new BigDecimal("15000"));


    }

    @Test
    @DisplayName("시나리오 1 (로그인X): 기존 그룹 채팅방 입장시, 방은 생성X, 멤버추가 X")
    void scenario1_Anonymous_EnterExistingGroupChat() {
        //Given
        Long productId = groupRequestDto.getProductId();
        Long currentMemberId = null;

        ChatRoom existingRoom = ChatRoom.builder().productId(productId).chatRoomType(ChatRoomType.GROUP).build();
        ReflectionTestUtils.setField(existingRoom, "id", 1L);
        given(chatRoomRepository.findByProductId(productId))
                .willReturn(Optional.of(existingRoom));


        //When
        ChatRoomResponseDto response = chatService.createOrGetChatRoom(groupRequestDto, currentMemberId);

        //Then
        assertThat(response.getChatRoomId()).isEqualTo(1L);
        assertThat(response.getProductId()).isEqualTo(productId);
        assertThat(response.getChatRoomType()).isEqualTo(ChatRoomType.GROUP);

        verify(chatRoomMemberRepository, never()).findByChatRoomIdAndMemberId(anyLong(), anyLong());

        //레포지토리 상호작용 검증
        verify(chatRoomRepository).findByProductId(productId);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        verifyNoMoreInteractions(chatRoomRepository, chatRoomMemberRepository);
    }

    @Test
    @DisplayName("시나리오 2 (로그인X): 새 그룹 채팅방 입장시, 방은 생성O, 멤버추가 X")
    void scenario2_Anonymous_EnterNewGroupChat() {
        //Given
        Long productId = groupRequestDto.getProductId();
        Long currentMemberId = null;

        // 1. Mocking 방 없음 설정
        given(chatRoomRepository.findByProductId(productId))
                .willReturn(Optional.empty());

        ChatRoom newRoom = ChatRoom.builder().productId(productId).chatRoomType(ChatRoomType.GROUP).build();
        ReflectionTestUtils.setField(newRoom, "id", 1L);
        given(chatRoomRepository.save(any(ChatRoom.class)))
                .willReturn(newRoom); // save 호출시 새 . 반환

        //When
        ChatRoomResponseDto response = chatService.createOrGetChatRoom(groupRequestDto, currentMemberId);

        //Then
        assertThat(response.getChatRoomId()).isEqualTo(1L);
        assertThat(response.getProductId()).isEqualTo(productId);
        assertThat(response.getChatRoomType()).isEqualTo(ChatRoomType.GROUP);

        verify(chatRoomMemberRepository, never()).findByChatRoomIdAndMemberId(anyLong(), anyLong()); // 멤버 추가 X (eunsureUserIsChatMember 호출X)

        //레포지토리 상호작용 검증
        verify(chatRoomRepository).findByProductId(productId);
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verifyNoMoreInteractions(chatRoomRepository, chatRoomMemberRepository);
    }

    @Test
    @DisplayName("시나리오 3 (로그인): '기존' 그룹 채팅방 입장 시, 방 생성x, 멤버 추가 O")
    void scenario3_Authenticated_EnterExistingGroupChat() {
        //Givne
        Long productId = groupRequestDto.getProductId();
        Long currentMemberId = testUser.getId();

        // 1. Mocking 방 있음 설정
        ChatRoom existingRoom = ChatRoom.builder().productId(productId).chatRoomType(ChatRoomType.GROUP).build();
        ReflectionTestUtils.setField(existingRoom, "id", 1L);
        given(chatRoomRepository.findByProductId(productId))
                .willReturn(Optional.of(existingRoom));

        // 2. Mocking ensureUserIsChatMember 실행
        given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(existingRoom.getId(), currentMemberId))
                .willReturn(Optional.empty()); //멤버 없음 설정
        given(memberRepository.findByIdAndDeletedAtIsNull(currentMemberId))
                .willReturn(Optional.of(testUser)); // 멤버 찾음

        //When
        ChatRoomResponseDto response = chatService.createOrGetChatRoom(groupRequestDto, currentMemberId);

        //Then
        assertThat(response.getProductId()).isEqualTo(productId);

        verify(chatRoomRepository, never()).save(any(ChatRoom.class)); // 기존 . 사용하여 save 호출 되지 않았는지 검증
        verify(chatRoomMemberRepository, times(1)).save(any(ChatRoomMember.class));
        // 로그인 유저 ensureUserIsChatMember 호출 확인 -> 1번 호출
    }

    @Test
    @DisplayName("시나리오 4 (로그인): 새 1:1 채팅방 생성")
    void scenario4_Authenticated_CreateNewDmChat() {
        //Given
        Long saleId = dmRequestDto.getSaleId();
        Long buyerUser = testUser.getId();
        Long sellerUser = testUser2.getId();

        //Mocking
        // 1. 판매글 찾기 (판매자 ID 반환)
        Sales mockSale = new Sales(1L, sellerUser, BigDecimal.TEN, 1, SalesStatus.ON_SALE);
        given(salesRepository.findById(saleId)).willReturn(Optional.of(mockSale));
        // 2. DM방 없음 설정
        String dmPairKey = "sale:" + saleId + "-user:" + buyerUser + "-user:" + sellerUser;
        given(chatRoomRepository.findByDmPairKey(dmPairKey)).willReturn(Optional.empty());
        // 3. 멤버 정보 찾기
        given(memberRepository.findByIdAndDeletedAtIsNull(sellerUser)).willReturn(Optional.of(testUser2));
        given(memberRepository.findByIdAndDeletedAtIsNull(buyerUser)).willReturn(Optional.of(testUser));
        // 4. save 호출되면 새 방 반환
        ChatRoom newRoom = ChatRoom.builder().saleId(saleId).chatRoomType(ChatRoomType.DM).build();
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(newRoom);

        //When
        ChatRoomResponseDto response = chatService.createOrGetChatRoom(dmRequestDto, buyerUser);

        //Then
        assertThat(response.getSaleId()).isEqualTo(saleId);
        assertThat(response.getChatRoomType()).isEqualTo(ChatRoomType.DM);

        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class)); // save 1번 호출 확인
        verify(chatRoomMemberRepository, times(1)).saveAll(any(List.class)); // 멤버 추가 확인
    }

    @Test
    @DisplayName("시나리오 5: 메세지 저장")
    void scenario5_Save_Message() {
        Long currentMemberId = testUser.getId();
        ChatMessageRequestDto requestDto = new ChatMessageRequestDto();
        requestDto.setChatRoomId(1L);
        requestDto.setMessageType(MessageType.TEXT);
        requestDto.setContent("hi");

        // 1. Mocking 그룹채팅방 있음 설정
        ChatRoom mockRoom = mock(ChatRoom.class);

        ChatRoomMember mockSender = mock(ChatRoomMember.class);
        given(mockSender.getChatRoom()).willReturn(mockRoom);
        given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(1L, currentMemberId))
                .willReturn(Optional.of(mockSender));

        ChatMessage mockSavedMessage = ChatMessage.builder()
                .chatRoomMember(mockSender).messageContent("mock message").messageType(MessageType.TEXT).build();
        ReflectionTestUtils.setField(mockSavedMessage, "id", 1L);
        ReflectionTestUtils.setField(mockSavedMessage, "createdAt", LocalDateTime.now());

        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockSavedMessage);


        //When
        ChatMessageResponseDto response = chatService.saveMessage(requestDto, currentMemberId);

        //Then
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        verify(mockRoom, times(1)).updateLastMessageId(1L);

        assertThat(response.messageType()).isEqualTo(MessageType.TEXT);
        assertThat(response.content()).isEqualTo("mock message");
        assertThat(response.senderName()).isEqualTo(testUser.getMemberName());
    }

    @Test
    @DisplayName("시나리오 6: 채팅방 메세지 내역 조회")
    void scenario6_Get_GroupMessage() {
        Long currentMemberId = testUser.getId();
        Long roomId = 1L;

        Pageable pageable = PageRequest.of(0, 30);

        // 1. Mocking 그룹채팅방 있음 설정
        ChatRoom mockRoom = mock(ChatRoom.class);
        given(mockRoom.getId()).willReturn(roomId);

        ChatRoomMember mockMembers = mock(ChatRoomMember.class);
        given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, currentMemberId))
                .willReturn(Optional.of(mockMembers));


        ChatRoomMember sender1 = mock(ChatRoomMember.class);
        given(sender1.getMemberId()).willReturn(currentMemberId);
        given(sender1.getMemberName()).willReturn(testUser.getNickname());
        given(sender1.getChatRoom()).willReturn(mockRoom);
        ChatMessage message1 = ChatMessage.builder().chatRoomMember(sender1).messageContent("message1").build();
        ReflectionTestUtils.setField(message1, "createdAt", LocalDateTime.now().minusMinutes(1));

        ChatRoomMember sender2 = mock(ChatRoomMember.class);
        given(sender2.getMemberId()).willReturn(testUser2.getId());
        given(sender2.getMemberName()).willReturn(testUser2.getNickname());
        given(sender2.getChatRoom()).willReturn(mockRoom);
        ChatMessage message2 = ChatMessage.builder().chatRoomMember(sender2).messageContent("message2").build();
        ReflectionTestUtils.setField(message2, "createdAt", LocalDateTime.now());

        //메세지 2개 page 객체로 감싸기
        List<ChatMessage> messageList = List.of(message1, message2);
        Page<ChatMessage> messagePage = new PageImpl<>(messageList, pageable, messageList.size());

        given(chatMessageRepository.findByChatRoomMember_ChatRoomId(roomId, pageable)).willReturn(messagePage);

        // 멤버 프로필 조회 설정
        given(memberRepository.findByIdAndDeletedAtIsNull(currentMemberId)).willReturn(Optional.of(testUser));
        given(memberRepository.findByIdAndDeletedAtIsNull(testUser2.getId())).willReturn(Optional.of(testUser2));

        //When
        Page<ChatMessageResponseDto> responsePage = chatService.getChatHistory(roomId, currentMemberId, pageable);

        //Then
        assertThat(responsePage).isNotNull();
        assertThat(responsePage.getTotalElements()).isEqualTo(2);

        assertThat(responsePage.getContent().get(0).content()).isEqualTo("message1");
        assertThat(responsePage.getContent().get(0).senderName().contains(testUser.getNickname()));

        assertThat(responsePage.getContent().get(1).content()).isEqualTo("message2");
        assertThat(responsePage.getContent().get(1).senderName().contains(testUser2.getNickname()));

        verify(chatRoomMemberRepository, times(1)).findByChatRoomIdAndMemberId(roomId, currentMemberId);
        verify(chatMessageRepository, times(1)).findByChatRoomMember_ChatRoomId(roomId, pageable);

    }


    @Test
    @DisplayName("시나리오 8: 1:1 채팅방 메세지 목록 조회")
    void scenario8_Get_DmList() {
        Long currentMemberId = testUser.getId();
        Long opponentMember1Id = testUser2.getId();
        Long opponentMember2Id = testUser3.getId();
        Long dmRoom1Id = 1L;
        Long dmRoom2Id = 2L;
        Long lastMessageId1 = 50L;
        Long lastMessageId2 = 80L;
        Long myLastReadId1 = 40L;
        Long myLastReadId2 = 80L;

        // 1. Mocking 나의 멤버쉽 정보 (DM방 2개 + 그룹방 1개)
        ChatRoom mockDMRoom1 = mock(ChatRoom.class);
        given(mockDMRoom1.getChatRoomType()).willReturn(ChatRoomType.DM);
        given(mockDMRoom1.getId()).willReturn(dmRoom1Id);
        given(mockDMRoom1.getLastMessageId()).willReturn(lastMessageId1);

        ChatRoom mockDMRoom2 = mock(ChatRoom.class);
        given(mockDMRoom2.getChatRoomType()).willReturn(ChatRoomType.DM);
        given(mockDMRoom2.getId()).willReturn(dmRoom2Id);
        given(mockDMRoom2.getLastMessageId()).willReturn(lastMessageId2);

        ChatRoom mockGroupRoom = mock(ChatRoom.class);
        given(mockGroupRoom.getChatRoomType()).willReturn(ChatRoomType.GROUP);

        ChatRoomMember myDmMembership1 = mock(ChatRoomMember.class);
        given(myDmMembership1.getChatRoom()).willReturn(mockDMRoom1);
        given(myDmMembership1.getLastReadMessageId()).willReturn(myLastReadId1);

        ChatRoomMember myDMMembership2 = mock(ChatRoomMember.class);
        given(myDMMembership2.getChatRoom()).willReturn(mockDMRoom2);
        given(myDMMembership2.getLastReadMessageId()).willReturn(myLastReadId2);

        ChatRoomMember myGroupMembership = mock(ChatRoomMember.class);
        given(myGroupMembership.getChatRoom()).willReturn(mockGroupRoom);

        // 호출 시 DM방 2개 그룹방 1개 반환
        given(chatRoomMemberRepository.findByMemberIdAndDeletedAtIsNull(currentMemberId))
                .willReturn(List.of(myDmMembership1, myDMMembership2, myGroupMembership));

        // 2. Mocking 상대방 정보
        ChatRoomMember opponentMembership1 = mock(ChatRoomMember.class);
        given(opponentMembership1.getMemberId()).willReturn(opponentMember1Id);
        given(chatRoomMemberRepository.findByChatRoomIdAndDeletedAtIsNull(dmRoom1Id))
                .willReturn(List.of(myDmMembership1, opponentMembership1));

        ChatRoomMember opponentMembership2 = mock(ChatRoomMember.class);
        given(opponentMembership2.getMemberId()).willReturn(opponentMember2Id);
        given(chatRoomMemberRepository.findByChatRoomIdAndDeletedAtIsNull(dmRoom2Id))
                .willReturn(List.of(myDMMembership2, opponentMembership2));

        // 3. 상대방 프로필(Member조회)

        given(memberRepository.findByIdAndDeletedAtIsNull(opponentMember1Id)).willReturn(Optional.of(testUser2));
        given(memberRepository.findByIdAndDeletedAtIsNull(opponentMember2Id)).willReturn(Optional.of(testUser3));

        //안읽은 개수
        given(chatMessageRepository.countByChatRoomMember_ChatRoomIdAndIdGreaterThan(dmRoom1Id, 40L))
                .willReturn(10L);
        given(chatMessageRepository.countByChatRoomMember_ChatRoomIdAndIdGreaterThan(dmRoom2Id, 80L))
                .willReturn(0L);

        ChatMessage lastMsg1 = mock(ChatMessage.class);
        given(lastMsg1.getMessageContent()).willReturn("last message1");
        given(lastMsg1.getCreatedAt()).willReturn(LocalDateTime.now());
        given(chatMessageRepository.findById(lastMessageId1)).willReturn(Optional.of(lastMsg1));

        ChatMessage lastMsg2 = mock(ChatMessage.class);
        given(lastMsg2.getMessageContent()).willReturn("last message2");
        given(lastMsg2.getCreatedAt()).willReturn(LocalDateTime.now());
        given(chatMessageRepository.findById(lastMessageId2)).willReturn(Optional.of(lastMsg2));

        //When
        List<ChatRoomList> responseList = chatService.getMyChatRooms(currentMemberId);

        //Then
        assertThat(responseList.size()).isEqualTo(2);

        ChatRoomList resultDto1 = responseList.stream().filter(r -> r.getChatRoomId() == dmRoom1Id).findFirst().get();
        ChatRoomList resultDto2 = responseList.stream().filter(r -> r.getChatRoomId() == dmRoom2Id).findFirst().get();

        assertThat(resultDto1.getRoomName()).isEqualTo(testUser2.getMemberName());
        assertThat(resultDto2.getRoomName()).isEqualTo(testUser3.getMemberName());

        assertThat(resultDto1.getUnreadCount()).isEqualTo(10L);
        assertThat(resultDto2.getUnreadCount()).isEqualTo(0L);

        assertThat(resultDto1.getLastMessageContent()).isEqualTo("last message1");
        assertThat(resultDto2.getLastMessageContent()).isEqualTo("last message2");
    }

    @Test
    @DisplayName("시나리오 10: 1:1 채팅방 나가기")
    void scenario9_Leave_DmChatRoom() {
        Long currentMemberId = testUser.getId();
        Long dmRoomId = 1L;

        // Mock 채팅방, 멤버 설정
        ChatRoom mockRoom = mock(ChatRoom.class);
        ChatRoomMember mockMember = mock(ChatRoomMember.class);

        // findByChatRooIdANdMemberId mockMember반환하도록 설정
        given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(dmRoomId, currentMemberId))
                .willReturn(Optional.of(mockMember));

        // getChatRoom-> mockDmRoom / getChatRoomType -> DM 나오도록 설정
        given(mockMember.getChatRoom()).willReturn(mockRoom);
        given(mockRoom.getChatRoomType()).willReturn(ChatRoomType.DM);

        //when
        chatService.leaveChatRoom(dmRoomId, currentMemberId);

        //then
        verify(chatRoomMemberRepository, times(1)).findByChatRoomIdAndMemberId(dmRoomId, currentMemberId);
        verify(mockMember, times(1)).leave();

        assertThat(chatRoomMemberRepository.findByChatRoomIdAndDeletedAtIsNull(dmRoomId).size()).isEqualTo(0);
    }

    @Test
    @DisplayName("시나리오 11: 채팅창 새 거래 제안")
    void scenario11_Propose_NewTrade() {
        Long chatRoomId = 1L;
        Long currentMemberId = testUser.getId();
        BigDecimal price = tradeRequestDto.price();

        // Mocking 채팅방 조회
        ChatRoom mockRoom = mock(ChatRoom.class);
        given(mockRoom.getId()).willReturn(chatRoomId);
        given(chatRoomRepository.findById(chatRoomId))
                .willReturn(Optional.of(mockRoom));

        // Mocking 기존 거래 없음? 으로 설정
        given(tradeRepository.findByChatRoomId(chatRoomId))
                .willReturn(Optional.empty());

        // 새 거래가 저장된 객체 Mocking
        Trade savedTrade = mock(Trade.class);
        given(savedTrade.getId()).willReturn(1L);
        given(savedTrade.getPrice()).willReturn(price);
        given(savedTrade.getChatRoom()).willReturn(mockRoom);

        // save 호출되면 저장된 객체 반환
        given(tradeRepository.save(any(Trade.class))).willReturn(savedTrade);

        // When
        TradeResponseDto response = chatService.proposeTrade(chatRoomId, tradeRequestDto, currentMemberId);

        verify(tradeRepository, times(1)).save(any(Trade.class));

        assertThat(response.getTradeId()).isEqualTo(1L);
        assertThat(response.getPrice()).isEqualTo(price);
        assertThat(response.getChatRoomId()).isEqualTo(chatRoomId);
    }

    @Test
    @DisplayName("[실패] 자신에게 1:1 채팅 걸기")
    void scenario_Fail_CreateDmChatWithSelf() {
        //Given
        Long saleId = dmRequestDto.getSaleId();
        Long currentMemberId = testUser.getId();

        //Mocking 1. 판매자 글 나로 설정
        Sales mySale = new Sales(1L, currentMemberId, BigDecimal.TEN, 1, SalesStatus.ON_SALE);
        given(salesRepository.findById(saleId)).willReturn(Optional.of(mySale));

        //When Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatService.createOrGetChatRoom(dmRequestDto, currentMemberId)
        );

        assertThat(exception.getMessage()).isEqualTo("자신과 1:1 채팅을 할 수 없습니다.");
    }

    @Test
    @DisplayName("[실패] 멤버가 아닌 사람 메세지 전송")
    void scenario_Fail_Send_Message() {
        //Given
        Long currentMemberId = testUser.getId();
        Long groupId = groupRequestDto.getProductId();

        ChatMessageRequestDto requestDto = new ChatMessageRequestDto();
        requestDto.setChatRoomId(groupId);
        requestDto.setMessageType(MessageType.TEXT);
        requestDto.setContent("test message");

        // Mocking
        // findByChatRoomIdAndMemberId 호출했을 때 -> empty 반환
        given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(groupId, currentMemberId))
                .willReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(SecurityException.class, () -> {
            chatService.saveMessage(requestDto, currentMemberId);
        });

        assertThat(exception.getMessage()).isEqualTo("해당 채팅방의 멤버가 아닙니다");
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("[실패] 그룹 채팅방 나가기")
    void scenario_Fail_Leave_GroupChatRoom() {
        Long currentMemberId = testUser.getId();
        Long groupId = 1L;

        // Mocking 가짜 방 가짜 멤버 객체 생성
        ChatRoom mockRoom = mock(ChatRoom.class);
        ChatRoomMember mockGroupMember = mock(ChatRoomMember.class);

        //findByChatRoomAndMemberId -> 가짜 채팅방 멤버 반환
        given(chatRoomMemberRepository.findByChatRoomIdAndMemberId(groupId, currentMemberId))
                .willReturn(Optional.of(mockGroupMember));

        // if member.getChatRoom() 검사 위해 설정
        given(mockGroupMember.getChatRoom()).willReturn(mockRoom);
        given(mockRoom.getChatRoomType()).willReturn(ChatRoomType.GROUP);

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    chatService.leaveChatRoom(groupId, currentMemberId);
                });

        assertThat(exception.getMessage()).isEqualTo("그룹 채팅방은 나갈 수 없습니다.");

        verify(mockGroupMember, never()).leave();
    }

    @Test
    @DisplayName("[실패] 존재하지 않은 채팅방에 거래 제안")
    void scenario_Fail_Trade_NoExistChatRoom() {
        //Given
        Long chatRoomId = 99L;
        Long currentMemberId = testUser.getId();

        //Mocking 채팅방 조회 실패
        given(chatRoomRepository.findById(chatRoomId))
                .willReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            chatService.proposeTrade(chatRoomId, tradeRequestDto, currentMemberId);
        });
        assertThat(exception.getMessage()).isEqualTo("채팅방을 찾을 수 없습니다");
        verify(tradeRepository, never()).save(any(Trade.class)); // save 호출 안됨 검증
    }
}
