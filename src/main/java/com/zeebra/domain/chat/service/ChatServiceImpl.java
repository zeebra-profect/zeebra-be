package com.zeebra.domain.chat.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.zeebra.domain.chat.dto.*;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.product.entity.Sales;
import com.zeebra.domain.product.repository.SalesRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import com.zeebra.domain.chat.entity.ChatMessage;
import com.zeebra.domain.chat.entity.ChatRoom;
import com.zeebra.domain.chat.entity.ChatRoomMember;
import com.zeebra.domain.chat.entity.ChatRoomType;
import com.zeebra.domain.chat.entity.Trade;

import com.zeebra.domain.chat.repository.ChatMessageRepository;
import com.zeebra.domain.chat.repository.ChatRoomMemberRepository;
import com.zeebra.domain.chat.repository.ChatRoomRepository;
import com.zeebra.domain.chat.repository.TradeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import static com.zeebra.domain.chat.entity.ChatRoomType.DM;
import static com.zeebra.domain.chat.entity.ChatRoomType.GROUP;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TradeRepository tradeRepository;

    private final MemberRepository memberRepository;
    private final SalesRepository salesRepository;


    @Override
    @Transactional
    public ChatRoomResponseDto createOrGetChatRoom(ChatRoomRequestDto chatRoomRequestDto, Long currentMemberId) {

        ChatRoom chatRoom;
        ChatRoomType type = chatRoomRequestDto.getChatRoomType();

        if (type == null) {
            throw new IllegalArgumentException("chatRoomType을 받으십시오");
        }

        switch (type) {
            case GROUP: {
                Long productId = chatRoomRequestDto.getProductId();
                if (productId == null) {
                throw new IllegalArgumentException("Group 채팅방을 위해선 productId가 필요합니다.");
                }

                Optional<ChatRoom> existingRoom = chatRoomRepository.findByProductIdAndChatRoomType(productId, ChatRoomType.GROUP);

                if (existingRoom.isPresent()) {
                    chatRoom = existingRoom.get();
                }else{
                    try {
                        ChatRoom newRoom = ChatRoom.builder()
                                .productId(productId)
                                .chatRoomType(ChatRoomType.GROUP)
                                .build();

                        chatRoom = chatRoomRepository.save(newRoom);

                    } catch (DataIntegrityViolationException e){ // 동시성 이슈 (다른 스레드가 먼저 방 만들었을경우 생성 실패 -> 다시 조회해서 가져오기)
                        log.info("기존 방 조회. ProductId={}", productId);
                                    chatRoom = chatRoomRepository.findTopByProductIdOrderByIdAsc(productId)
                                            .orElseThrow(() -> new EntityNotFoundException("채팅방 생성 중 동시성 오류 발생했으나 방을 찾을 수 없습니다."));
                    }
                }

                if (currentMemberId != null) {
                    ensureUserIsChatMember(chatRoom, currentMemberId);
                }
                break;
            }
            case DM: {
                Long saleId = chatRoomRequestDto.getSaleId();

                Sales sales = salesRepository.findById(saleId)
                        .orElseThrow(() -> new EntityNotFoundException("판매 글을 찾을 수 없습니다"));

                Long user1 = sales.getMemberId();
                Long user2 = currentMemberId;

                if (Objects.equals(user1, user2)) {
                    throw new IllegalArgumentException("자신과 1:1 채팅을 할 수 없습니다.");
                }

                String dmPairKey = createDmPairKey(saleId, user1, user2);

                chatRoom = chatRoomRepository.findBySaleIdAndDmPairKeyAndChatRoomType(saleId, dmPairKey, ChatRoomType.DM)
                        .orElseGet(() -> {
                            ChatRoom newRoom = ChatRoom.builder()
                                    .saleId(saleId)
                                    .chatRoomType(ChatRoomType.DM)
                                    .dmPairKey(dmPairKey).build();
                            ChatRoom savedRoom = chatRoomRepository.save(newRoom);

                            addDmMembers(savedRoom, user1, user2);

                            return savedRoom;
                        });
                break;
            }
            default:
                throw new IllegalArgumentException("지원하지 않는 ChatRoomType 입니다.: " + type);
            }
            return ChatRoomResponseDto.from(chatRoom);
        }



        ///TODO: 쿼리 실행계획 분석 및 슬로우 쿼리 확인하기  -> 인덱스 설계(튜닝) -> DB 공부
    @Override
    @Transactional
    public ChatMessageResponseDto saveMessage(ChatMessageRequestDto chatMessageRequestDto, Long currentMemberId) {
        Long roomId = chatMessageRequestDto.getChatRoomId();

        // 개선하기 1. ChatRoom 정보 Redis 캐시에서 조회 (없을 때만 DB에서 조회)
        // ChatRoom chatRoom = chatRoomCacheService.getRoom(roomId);
        // 구현 전이니 일단 주석처리 12.02

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다.")); //DB 호출 1

        ChatRoomMember sender;

        if (chatRoom.getChatRoomType() == GROUP) {
            sender = ensureUserIsChatMember(chatRoom, currentMemberId);
        }else {
            sender = chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, currentMemberId)
                    .orElseThrow(() -> new SecurityException("해당 채팅방의 멤버가 아닙니다")); //DB 호출 2
        }
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoomMember(sender)
                .messageType(chatMessageRequestDto.getMessageType())
                .messageContent(chatMessageRequestDto.getContent())
                .imageUrl(chatMessageRequestDto.getImageUrl())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        chatRoom.updateLastMessageId(savedMessage.getId());

        return ChatMessageResponseDto.from(savedMessage, null);
    }

    @Override
    @Transactional
    public void saveFromResponse(ChatMessageResponseDto responseDto) {
        ChatMessageRequestDto req = new ChatMessageRequestDto();

        req.setChatRoomId(responseDto.roomId());
        req.setMessageType(responseDto.messageType());
        req.setContent(responseDto.content());
        req.setImageUrl(responseDto.imageUrl());

        Long senderId = responseDto.senderMemberId();

        saveMessage(req, senderId);
    }



    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageResponseDto> getChatHistory(Long roomId, Long currentMemberId, Pageable pageable){
        //권한 검사 로직 추가
        if (currentMemberId != null) {
            chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, currentMemberId)
                    .orElseThrow(() -> new SecurityException("채팅방 접근 권한이 없습니다."));
        }

        Page<ChatMessage> messagePage = chatMessageRepository.findByChatRoomMember_ChatRoomId(roomId, pageable);

        Set<Long> MemberIds = messagePage.getContent().stream().map(msg -> msg.getChatRoomMember()
                .getMemberId()).collect(Collectors.toSet());

        List<Member> members = memberRepository.findAllById(MemberIds);

        Map<Long, Member> memberMap = members.stream().collect(Collectors.toMap(Member::getId, Function.identity()));


        return messagePage.map(message -> {
           Long senderId = message.getChatRoomMember().getMemberId();
           Member member = memberMap.get(senderId);
           return ChatMessageResponseDto.from(message, member);
        });
    }

    @Override
    @Transactional(readOnly = true) //Bulk 방식
    public List<ChatRoomList> getMyChatRooms(Long currentMemberId) {
        List<ChatRoomMember> myMemberships = chatRoomMemberRepository.findByMemberIdAndDeletedAtIsNull(currentMemberId);

        if (myMemberships.isEmpty()) {
            return List.of();
        }

        List<Long> dmRoomIds = myMemberships.stream()
                .filter(m -> m.getChatRoom().getChatRoomType() == DM)
                .map(m -> m.getChatRoom().getId())
                .toList();

        if (dmRoomIds.isEmpty()) {
            return List.of();
        }

        // Map<ChatRoomId, ChatRoomMember(상대방)>
        List<ChatRoomMember> opponents = chatRoomMemberRepository.findAllOpponents(dmRoomIds, currentMemberId);
        Map<Long, ChatRoomMember> opponentMap = opponents.stream()
                .collect(Collectors.toMap(crm -> crm.getChatRoom().getId(), Function.identity()));

        List<Object[]> unreadCounts = chatMessageRepository.countUnreadMessagesByRoomIds(dmRoomIds, currentMemberId);
        Map<Long, Long> unreadCountMap = unreadCounts.stream()
                .collect(Collectors.toMap(obj -> (Long) obj[0], obj -> (Long) obj[1]));

        List<Long> lastMessageIds = myMemberships.stream().map(crm -> crm.getChatRoom()
                .getLastMessageId()).filter(Objects::nonNull).toList();

        Map<Long, ChatMessage> lastMessageMap = new HashMap<>();
        if(!lastMessageIds.isEmpty()) {
            List<ChatMessage> lastMessages = chatMessageRepository.findAllById(lastMessageIds);
            lastMessageMap = lastMessages.stream()
                    .collect(Collectors.toMap(ChatMessage::getId, Function.identity()));
        }

        Map<Long, ChatMessage> finalLastMessageMap = lastMessageMap;
        return myMemberships.stream()
                .filter(member -> member.getChatRoom().getChatRoomType() == DM)
                .map(member -> {
                    Long roomId = member.getChatRoom().getId();
                    Long lastMsgId = member.getChatRoom().getLastMessageId();

                    ChatRoomMember opponent = opponentMap.get(roomId);
                    String roomName = (opponent != null) ? opponent.getMemberName() : "알 수 없음.";
                    String roomProfileImageUrl = null;

                    long unreadCount = unreadCountMap.getOrDefault(roomId, 0L);

                    String lastMessageContent = "";
                    LocalDateTime lastMessageTime = null;
                    if (lastMsgId != null) {
                        ChatMessage lastMsg = finalLastMessageMap.get(lastMsgId);
                        if (lastMsg != null) {
                            lastMessageContent = lastMsg.getMessageContent();
                            lastMessageTime = lastMsg.getCreatedAt();
                        }
                    }

                    return ChatRoomList.builder()
                            .chatRoomId(roomId)
                            .unreadCount(unreadCount)
                            .roomName(roomName)
                            .roomProfileImageUrl(roomProfileImageUrl)
                            .lastMessageContent(lastMessageContent)
                            .lastMessageTime(lastMessageTime)
                            .build();
                }).toList();
    }

    @Override
    @Transactional
    public void leaveChatRoom(Long chatRoomId, Long currentMemberId) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoomId, currentMemberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 채팅방의 멤버가 아닙니다"));

        if (member.getChatRoom().getChatRoomType() == GROUP) {
            throw new IllegalArgumentException("그룹 채팅방은 나갈 수 없습니다.");
        }
        member.leave();
    }

    @Async("chatExecutor")
    @Transactional
    public void saveMessageAsync(ChatMessageRequestDto chatMessageRequestDto, Long currentMemberId) {
        Long roomId = chatMessageRequestDto.getChatRoomId();

        // 개선하기 1. ChatRoom 정보 Redis 캐시에서 조회 (없을 때만 DB에서 조회)
        // ChatRoom chatRoom = chatRoomCacheService.getRoom(roomId);
        // 구현 전이니 일단 주석처리 12.02
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다.")); //DB 호출 1

        ChatRoomMember sender;

        if (chatRoom.getChatRoomType() == GROUP) {
            sender = ensureUserIsChatMember(chatRoom, currentMemberId);
        }else {
            sender = chatRoomMemberRepository.findByChatRoomIdAndMemberId(roomId, currentMemberId)
                    .orElseThrow(() -> new SecurityException("해당 채팅방의 멤버가 아닙니다")); //DB 호출 2
        }
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoomMember(sender)
                .messageType(chatMessageRequestDto.getMessageType())
                .messageContent(chatMessageRequestDto.getContent())
                .imageUrl(chatMessageRequestDto.getImageUrl())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        chatRoom.updateLastMessageId(savedMessage.getId());

    }


    @Override
    @Transactional
    public TradeResponseDto proposeTrade(Long chatRoomId, TradeRequestDto tradeRequestDto, Long currentMemberId) {
        BigDecimal price = tradeRequestDto.price();

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다"));

        Trade trade = tradeRepository.findByChatRoomId(chatRoomId)
                .map(existingTrade -> {
                    existingTrade.updatePrice(price);
                    return existingTrade;
                })
                .orElseGet(() -> Trade.builder()
                        .chatRoom(chatRoom)
                        .price(price)
                        .build());
        Trade savedTrade = tradeRepository.save(trade);
        return new TradeResponseDto(savedTrade.getId(), savedTrade.getChatRoom().getId(), savedTrade.getPrice());
    }

    private ChatRoomMember ensureUserIsChatMember(ChatRoom chatRoom, Long currentMemberId) {
        return chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoom.getId(), currentMemberId)
                .orElseGet(() -> {
                   try { // Member 서비스에서 닉네임 조회
                       // String memberName = memberService.Api.getMemberName(memberId);
                       Member member = memberRepository.findByIdAndDeletedAtIsNull(currentMemberId)
                               .orElseThrow(() -> new EntityNotFoundException("멤버 정보를 찾을 수 없습니다."));

                       ChatRoomMember newMember = ChatRoomMember.builder()
                               .chatRoom(chatRoom)
                               .memberId(currentMemberId)
                               .memberName(member.getNickname())
                               .build();
                       return chatRoomMemberRepository.saveAndFlush(newMember);
                   } catch(DataIntegrityViolationException e) {
                       return chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoom.getId(), currentMemberId)
                               .orElseThrow(() -> new RuntimeException("멤버 추가 동시성 오류"));
                   }
                });
    }

    private void addDmMembers(ChatRoom room, Long user1Id, Long user2Id) {
        Member m1 = memberRepository.findByIdAndDeletedAtIsNull(user1Id).orElseThrow();
        Member m2 = memberRepository.findByIdAndDeletedAtIsNull(user2Id).orElseThrow();

        ChatRoomMember member1 = ChatRoomMember.builder().chatRoom(room).memberId(user1Id).memberName(m1.getNickname()).build();
        ChatRoomMember member2 = ChatRoomMember.builder().chatRoom(room).memberId(user2Id).memberName(m2.getNickname()).build();

        chatRoomMemberRepository.saveAll(List.of(member1, member2));
    }

    private String createDmPairKey(Long saleId, Long dmUser1, Long dmUser2) {
        Long user1 = Math.min(dmUser1, dmUser2);
        Long user2 = Math.max(dmUser1, dmUser2);
        return String.format("sale:%d-user:%d-user:%d", saleId, user1, user2);
    }
}
