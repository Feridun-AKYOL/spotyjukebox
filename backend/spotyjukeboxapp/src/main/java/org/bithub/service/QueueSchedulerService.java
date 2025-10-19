package org.bithub.service;

import lombok.RequiredArgsConstructor;
import org.bithub.model.UserInfo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueSchedulerService {

    private final UserService userService;
    private final SpotifyService spotifyService;

    /**
     * 🔄 Her 10 saniyede bir (test) / 2 dakikada bir (prod)
     * Aktif Jukebox kullanıcılarının playlist'ini oylamaya göre günceller
     */
    @Scheduled(fixedRate = 10000) // 10 saniye (test)
    // @Scheduled(fixedRate = 120000) // 2 dakika (production)
    public void updateJukeboxPlaylists() {
        try {
            List<UserInfo> activeUsers = userService.findAllActiveJukeboxUsers();

            if (activeUsers.isEmpty()) {
                System.out.println("⏸️ No active jukebox sessions.");
                return;
            }

            System.out.println("🔄 Updating jukebox playlists for " + activeUsers.size() + " users...");

            for (UserInfo user : activeUsers) {
                try {
                    spotifyService.updateJukeboxPlaylist(user);
                } catch (Exception e) {
                    System.err.println("❌ Failed to update playlist for: " + user.getSpotifyUserId());
                    e.printStackTrace();
                }
            }

            System.out.println("✅ Jukebox playlist update cycle completed.");

        } catch (Exception e) {
            System.err.println("❌ Jukebox scheduler error:");
            e.printStackTrace();
        }
    }
}