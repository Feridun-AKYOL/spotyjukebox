package org.bithub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a Spotify device associated with a user's account.
 * Used to display available playback devices and manage where the music is playing.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpotifyDevice {

    /** The unique Spotify device ID. */
    private String id;

    /** The display name of the device (e.g., "John’s iPhone", "Web Player"). */
    private String name;

    /** The type of device (e.g., "Computer", "Smartphone", "Speaker"). */
    private String type;

    /** Indicates whether this device is currently active. */
    private boolean isActive;
}
