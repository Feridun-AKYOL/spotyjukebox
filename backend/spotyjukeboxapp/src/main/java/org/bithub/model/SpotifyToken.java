package org.bithub.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
public class SpotifyToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;

    @OneToOne
    @JoinColumn(name = "user_id")
    private UserInfo user;


}
