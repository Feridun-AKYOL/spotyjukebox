package org.bithub.model;

/**
 * Represents the number of votes a specific Spotify track has received
 * within a Jukebox session.
 *
 * @param trackId the unique Spotify track ID
 * @param votes   the total number of votes for this track
 */
public record TrackVote(String trackId, Long votes) {}
