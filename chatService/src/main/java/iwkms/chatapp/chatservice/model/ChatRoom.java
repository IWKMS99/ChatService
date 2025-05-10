package iwkms.chatapp.chatservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "chat_rooms")
@Data
@NoArgsConstructor
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean isPrivate = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private String ownerUsername;

    @ElementCollection
    @CollectionTable(name = "chat_room_members", 
                    joinColumns = @JoinColumn(name = "chat_room_id"))
    @Column(name = "username")
    private Set<String> members = new HashSet<>();

    public ChatRoom(String roomId, String name, String description, boolean isPrivate, String creatorUsername) {
        this.roomId = roomId;
        this.name = name;
        this.description = description;
        this.isPrivate = isPrivate;
        this.createdAt = LocalDateTime.now();
        this.ownerUsername = creatorUsername;
        this.members.add(creatorUsername); // Создатель автоматически является участником
    }

    // Для совместимости со старым кодом
    public ChatRoom(String roomId, String name, String description, boolean isPrivate) {
        this(roomId, name, description, isPrivate, "system");
    }

    public void addMember(String username) {
        this.members.add(username);
    }

    public void removeMember(String username) {
        // Владельца нельзя удалить из комнаты
        if (!username.equals(ownerUsername)) {
            this.members.remove(username);
        }
    }

    public boolean hasMember(String username) {
        return this.members.contains(username);
    }
    
    public boolean isOwner(String username) {
        return username != null && username.equals(ownerUsername);
    }
} 