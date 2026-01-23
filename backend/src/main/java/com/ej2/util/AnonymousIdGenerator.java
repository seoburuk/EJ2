package com.ej2.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for generating consistent anonymous IDs within a post
 * Same user gets same anonymous ID within a post, but different IDs across posts
 */
public class AnonymousIdGenerator {

    /**
     * Generate anonymous ID for a user within a specific post
     * @param userId User ID
     * @param postId Post ID
     * @return Anonymous ID (e.g., "익명1", "익명2", etc.)
     */
    public static String generateAnonymousId(Long userId, Long postId) {
        if (userId == null || postId == null) {
            return "익명";
        }

        try {
            // Create a unique hash based on userId and postId
            String input = userId + ":" + postId;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert first 4 bytes to a positive integer
            int hashValue = Math.abs(
                ((hash[0] & 0xFF) << 24) |
                ((hash[1] & 0xFF) << 16) |
                ((hash[2] & 0xFF) << 8) |
                (hash[3] & 0xFF)
            );

            // Map to a number between 1 and 999
            int anonymousNumber = (hashValue % 999) + 1;

            return "익명" + anonymousNumber;
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash if SHA-256 is not available
            int simpleHash = (userId.toString() + postId.toString()).hashCode();
            int anonymousNumber = (Math.abs(simpleHash) % 999) + 1;
            return "익명" + anonymousNumber;
        }
    }

    /**
     * Assign sequential anonymous IDs to multiple users in a post
     * Used for displaying comments where order matters
     * @param userIds List of user IDs
     * @param postId Post ID
     * @return Map of userId to anonymous ID
     */
    public static Map<Long, String> generateAnonymousIdsForPost(Long[] userIds, Long postId) {
        Map<Long, String> idMap = new HashMap<>();
        Map<String, Integer> assignedNumbers = new HashMap<>();
        int nextSequentialNumber = 1;

        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }

            // Generate consistent hash-based ID
            String hashBasedId = generateAnonymousId(userId, postId);

            // If this hash-based ID hasn't been seen, assign sequential number
            if (!assignedNumbers.containsKey(hashBasedId)) {
                assignedNumbers.put(hashBasedId, nextSequentialNumber++);
            }

            // Map user to their sequential anonymous ID
            int sequentialNumber = assignedNumbers.get(hashBasedId);
            idMap.put(userId, "익명" + sequentialNumber);
        }

        return idMap;
    }

    /**
     * Generate anonymous ID for comments
     * Uses the same logic as posts to maintain consistency
     */
    public static String generateAnonymousIdForComment(Long userId, Long postId, Long commentId) {
        // Comments use post-based anonymous ID to maintain consistency within a post
        return generateAnonymousId(userId, postId);
    }
}
