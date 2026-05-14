package com.alumniconnect.repository;

import com.alumniconnect.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
        select m from Message m
        where (m.sender.id = :userA and m.receiver.id = :userB)
           or (m.sender.id = :userB and m.receiver.id = :userA)
        order by m.sentAt
        """)
    List<Message> findConversation(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("""
        select m from Message m
        where m.id in (
            select max(m2.id) from Message m2
            where m2.sender.id = :userId or m2.receiver.id = :userId
            group by case
                when m2.sender.id = :userId then m2.receiver.id
                else m2.sender.id
            end
        )
        order by m.sentAt desc
        """)
    List<Message> findLatestMessagesForUser(@Param("userId") Long userId);

    List<Message> findByReceiverIdAndIsReadFalse(Long receiverId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    List<Message> findByReceiverIdAndSenderIdAndIsReadFalse(Long receiverId, Long senderId);
}
