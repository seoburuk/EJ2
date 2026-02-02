package com.ej2.dto;

public class DashboardStatsDTO {
    private Long totalUsers;
    private Long adminCount;
    private Long totalBoards;
    private Long totalPosts;
    private Long totalComments;
    private Long newUsersThisWeek;
    private Long newPostsThisWeek;

    public DashboardStatsDTO() {
    }

    // Getters and Setters
    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Long getAdminCount() {
        return adminCount;
    }

    public void setAdminCount(Long adminCount) {
        this.adminCount = adminCount;
    }

    public Long getTotalBoards() {
        return totalBoards;
    }

    public void setTotalBoards(Long totalBoards) {
        this.totalBoards = totalBoards;
    }

    public Long getTotalPosts() {
        return totalPosts;
    }

    public void setTotalPosts(Long totalPosts) {
        this.totalPosts = totalPosts;
    }

    public Long getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(Long totalComments) {
        this.totalComments = totalComments;
    }

    public Long getNewUsersThisWeek() {
        return newUsersThisWeek;
    }

    public void setNewUsersThisWeek(Long newUsersThisWeek) {
        this.newUsersThisWeek = newUsersThisWeek;
    }

    public Long getNewPostsThisWeek() {
        return newPostsThisWeek;
    }

    public void setNewPostsThisWeek(Long newPostsThisWeek) {
        this.newPostsThisWeek = newPostsThisWeek;
    }
}
