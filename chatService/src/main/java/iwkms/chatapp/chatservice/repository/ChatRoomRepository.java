package iwkms.chatapp.chatservice.repository;

import iwkms.chatapp.chatservice.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByRoomId(String roomId);
    boolean existsByRoomId(String roomId);
    List<ChatRoom> findByIsPrivateFalse();
    List<ChatRoom> findByMembersContains(String username);
} 