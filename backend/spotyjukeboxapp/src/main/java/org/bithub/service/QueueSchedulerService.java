package org.bithub.service;

import lombok.RequiredArgsConstructor;
import org.bithub.model.UserInfo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Scheduled service responsible for periodically updating
 * Jukebox playlists based on active user votes.
 *
 * <p>This scheduler runs at fixed intervals and ensures that
 * active sessions reflect the latest vote results in their
 * Spotify playlists.</p>
 */
@Service
@RequiredArgsConstructor
public class QueueSchedulerService {

    private final UserService userService;
    private final SpotifyService spotifyService;

    /**
     * Periodically updates Jukebox playlists for all active users.
     * <ul>
     *     <li>Runs every 10 seconds in test environments.</li>
     *     <li>In production, it can be adjusted to every 2 minutes.</li>
     * </ul>
     */
    @Scheduled(fixedRate = 10000) // 10 seconds (testing)
    // @Scheduled(fixedRate = 120000) // 2 minutes (production)
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
                    System.err.println("❌ Failed to update playlist for user: " + user.getSpotifyUserId());
                    e.printStackTrace();
                }
            }

            System.out.println("✅ Jukebox playlist update cycle completed successfully.");

        } catch (Exception e) {
            System.err.println("❌ Jukebox scheduler encountered an unexpected error:");
            e.printStackTrace();
        }
    }
}
