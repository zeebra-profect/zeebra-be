package com.zeebra.domain.chat.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.zeebra.domain.chat.dto.*;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.product.entity.Sales;
import com.zeebra.domain.product.repository.SalesRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

                chatRoom = chatRoomRepository.findTopByProductIdOrderByIdAsc(productId).orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.builder()
                            .productId(productId)
                            .chatRoomType(ChatRoomType.GROUP)
                            .build();

                    ChatRoom savedRoom = chatRoomRepository.save(newRoom);
                    return savedRoom;
                });

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

                chatRoom = chatRoomRepository.findByDmPairKey(dmPairKey)
                        .orElseGet(() -> {
                            ChatRoom newRoom = ChatRoom.builder()
                                    .saleId(saleId)
                                    .chatRoomType(ChatRoomType.DM)
                                    .dmPairKey(dmPairKey).build();
                            ChatRoom savedRoom = chatRoomRepository.save(newRoom);

                            Member memberUser1 = memberRepository.findByIdAndDeletedAtIsNull(user1)
                                    .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));
                            Member memberUser2 = memberRepository.findByIdAndDeletedAtIsNull(user2)
                                    .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

                            ChatRoomMember dmUser1 = ChatRoomMember.builder()
                                    .chatRoom(savedRoom)
                                    .memberId(user1)
                                    .memberName(memberUser1.getNickname()).build();

                            ChatRoomMember dmUser2 = ChatRoomMember.builder()
                                    .chatRoom(savedRoom)
                                    .memberId(user2)
                                    .memberName(memberUser2.getNickname()).build();
                            chatRoomMemberRepository.saveAll(List.of(dmUser1, dmUser2));

                            return savedRoom;
                        });
                break;
            }
            default:
                throw new IllegalArgumentException("지원하지 않는 ChatRoomType 입니다.: " + type);
            }
            return ChatRoomResponseDto.from(chatRoom);
        }


    @Override
    @Transactional
    public ChatMessageResponseDto saveMessage(ChatMessageRequestDto chatMessageRequestDto, Long currentMemberId) {

        ChatRoomMember sender = chatRoomMemberRepository.findByChatRoomIdAndMemberId(
                chatMessageRequestDto.getChatRoomId(), currentMemberId)
                .orElseThrow(() -> new SecurityException("해당 채팅방의 멤버가 아닙니다"));

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoomMember(sender)
                .messageType(chatMessageRequestDto.getMessageType())
                .messageContent(chatMessageRequestDto.getContent())
                .imageUrl(chatMessageRequestDto.getImageUrl())
                .build();
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        sender.getChatRoom().updateLastMessageId(savedMessage.getId());

        Member member = memberRepository.findByIdAndDeletedAtIsNull(currentMemberId)
                .orElse(null);

        return ChatMessageResponseDto.from(savedMessage, member);
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

        return messagePage.map(message -> {
           Long senderMemberId = message.getChatRoomMember().getMemberId();
           Member member = memberRepository.findByIdAndDeletedAtIsNull(senderMemberId)
                   .orElse(null);
           return ChatMessageResponseDto.from(message, member);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomList> getMyChatRooms(Long currentMemberId) {
        List<ChatRoomMember> myMemberships = chatRoomMemberRepository.findByMemberIdAndDeletedAtIsNull(currentMemberId);

        return myMemberships.stream()
                .filter(member -> member.getChatRoom().getChatRoomType() == DM)
                .map(member -> {
                    ChatRoom room = member.getChatRoom();
                    Long roomLastMessageId = room.getLastMessageId();
                    Long myLastReadId = member.getLastReadMessageId();

                    String roomName = member.getMemberName();
                    String roomProfileImageUrl = null;


                    List<ChatRoomMember> membersInRoom = chatRoomMemberRepository.findByChatRoomIdAndDeletedAtIsNull(room.getId());

                    Optional<ChatRoomMember> opponent = membersInRoom.stream()
                            .filter(m -> !m.getMemberId().equals(currentMemberId))
                            .findFirst();

                    if (opponent.isPresent()) {
                        Optional<Member> opponentMember = memberRepository.findByIdAndDeletedAtIsNull(opponent.get().getMemberId());
                        if (opponentMember.isPresent()) {
                            roomName = opponentMember.get().getNickname();
                            roomProfileImageUrl = opponentMember.get().getMemberImage();
                        }
                    }

                    long unreadCount = 0;
                    if (roomLastMessageId != null) {
                        if (myLastReadId == null) {
                            unreadCount = chatMessageRepository.countByChatRoomMember_ChatRoomIdAndIdGreaterThan(room.getId(), 0L);
                        } else if (roomLastMessageId > myLastReadId) {
                            unreadCount = chatMessageRepository.countByChatRoomMember_ChatRoomIdAndIdGreaterThan(room.getId(), myLastReadId);
                        }
                    }

                    String lastMessageContent = "";
                    LocalDateTime lastMessageTime = null;
                    if (room.getLastMessageId() != null) {
                        Optional<ChatMessage> lastMsg = chatMessageRepository.findById(room.getLastMessageId());
                        if (lastMsg.isPresent()) {
                            lastMessageContent = lastMsg.get().getMessageContent();
                            lastMessageTime = lastMsg.get().getCreatedAt();
                        }
                    }

                    return ChatRoomList.builder()
                            .chatRoomId(room.getId())
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

    private void ensureUserIsChatMember(ChatRoom chatRoom, Long currentMemberId) {
        chatRoomMemberRepository.findByChatRoomIdAndMemberId(chatRoom.getId(), currentMemberId)
                .orElseGet(() -> {
                    // Member 서비스에서 닉네임 조회
                    // String memberName = memberService.Api.getMemberName(memberId);
                    Member member = memberRepository.findByIdAndDeletedAtIsNull(currentMemberId)
                            .orElseThrow(() -> new EntityNotFoundException("멤버 정보를 찾을 수 없습니다."));

                    ChatRoomMember newMember   = ChatRoomMember.builder()
                            .chatRoom(chatRoom)
                            .memberId(currentMemberId)
                            .memberName(member.getNickname())
                            .build();
                    return chatRoomMemberRepository.save(newMember);
                });
    }

    private String createDmPairKey(Long saleId, Long dmUser1, Long dmUser2) {
        Long user1 = Math.min(dmUser1, dmUser2);
        Long user2 = Math.max(dmUser1, dmUser2);
        return String.format("sale:%d-user:%d-user:%d", saleId, user1, user2);
    }
}