# Implementation Roadmap - University Community Platform

## Overview
This document provides a phased approach to implementing all 25 features of the EJ2 platform.

## Current Status
✅ **Completed**
- Basic User model (id, name, email)
- Timetable system (timetables, timetable_courses)
- Basic CRUD operations for users
- Docker containerization
- Spring Framework + React setup

## Implementation Phases

### Phase 1: Foundation & Authentication (Priority: CRITICAL)
**Estimated effort**: 2-3 weeks

#### 1.1 Enhanced User Model & Authentication
- [ ] Extend User entity with new fields (password, nickname, university_id, etc.)
- [ ] Create University entity and repository
- [ ] Implement BCrypt password hashing
- [ ] Create EmailVerification entity and service
- [ ] Create PasswordResetToken entity and service
- [ ] Build registration API with email validation
- [ ] Build login API with JWT/Session management
- [ ] Implement password reset flow
- [ ] Implement profile management (bio, password change)

**API Endpoints to create**:
- `POST /api/auth/register` - Register with university email
- `POST /api/auth/verify-email` - Verify email code
- `POST /api/auth/login` - Login
- `POST /api/auth/forgot-password` - Request password reset
- `POST /api/auth/reset-password` - Reset password with token
- `GET /api/auth/me` - Get current user info
- `PUT /api/auth/profile` - Update profile
- `PUT /api/auth/change-password` - Change password

**Frontend Pages**:
- Registration page
- Login page
- Email verification page
- Password reset page
- Profile management page

---

### Phase 2: Core Board System (Priority: HIGH)
**Estimated effort**: 3-4 weeks

#### 2.1 Board Infrastructure
- [ ] Create Board entity (general, anonymous, event, best, market)
- [ ] Create Post entity with all fields
- [ ] Create PostImage entity
- [ ] Create PostViewLog entity for view tracking
- [ ] Implement board service and repository
- [ ] Implement post service with IP-based view counting
- [ ] Build anonymous ID generation logic

#### 2.2 Board APIs
- [ ] List boards API
- [ ] Create post API (with image upload)
- [ ] List posts API (pagination, sorting)
- [ ] View post API (increment view count)
- [ ] Update post API (owner only)
- [ ] Delete post API (owner/admin)
- [ ] Search posts API (title + content)

**API Endpoints**:
- `GET /api/boards` - List all boards
- `GET /api/boards/{id}/posts` - List posts in board
- `POST /api/posts` - Create post
- `GET /api/posts/{id}` - View post details
- `PUT /api/posts/{id}` - Update post
- `DELETE /api/posts/{id}` - Delete post
- `POST /api/posts/{id}/images` - Upload images
- `GET /api/search?q={query}&sort={field}` - Search posts

**Frontend Pages**:
- Board list page
- Post list page (with filters)
- Post detail page
- Post create/edit form
- Search results page

#### 2.3 Anonymous Board
- [ ] Anonymous ID generation per post
- [ ] Anonymous ID consistency within post
- [ ] Store real user_id for admin

#### 2.4 Event Board
- [ ] Create Event entity
- [ ] Add thumbnail upload
- [ ] Date range validation
- [ ] Auto-archiving for expired events
- [ ] Event listing with active/archived filter

#### 2.5 Best Board
- [ ] Scheduled job to find best posts (20+ likes OR 500+ views in 24h)
- [ ] Auto-collection to best board
- [ ] Real-time best post calculation

---

### Phase 3: Engagement Features (Priority: HIGH)
**Estimated effort**: 2-3 weeks

#### 3.1 Comment System
- [ ] Create Comment entity (with parent_comment_id for replies)
- [ ] Implement 2-level depth validation
- [ ] Comment CRUD APIs
- [ ] Soft delete implementation ("삭제된 댓글")
- [ ] Anonymous comment support
- [ ] Comment count auto-update on posts

**API Endpoints**:
- `GET /api/posts/{postId}/comments` - List comments
- `POST /api/posts/{postId}/comments` - Create comment
- `POST /api/comments/{id}/replies` - Create reply
- `PUT /api/comments/{id}` - Update comment
- `DELETE /api/comments/{id}` - Delete comment

#### 3.2 Reaction System
- [ ] Create PostReaction entity
- [ ] Create CommentReaction entity
- [ ] Like/dislike toggle logic
- [ ] Prevent self-reaction
- [ ] Real-time count updates
- [ ] Cancellation support

**API Endpoints**:
- `POST /api/posts/{id}/react` - Like/dislike post
- `DELETE /api/posts/{id}/react` - Cancel reaction
- `POST /api/comments/{id}/react` - Like/dislike comment
- `DELETE /api/comments/{id}/react` - Cancel reaction

#### 3.3 Scrap System
- [ ] Create Scrap entity
- [ ] Create ScrapFolder entity
- [ ] Folder management APIs
- [ ] Scrap CRUD APIs
- [ ] My scraps page

**API Endpoints**:
- `POST /api/posts/{id}/scrap` - Scrap post
- `DELETE /api/scraps/{id}` - Remove scrap
- `GET /api/my/scraps` - List my scraps
- `POST /api/scrap-folders` - Create folder
- `PUT /api/scraps/{id}/folder` - Move to folder

**Frontend Pages**:
- My scraps page with folder navigation

---

### Phase 4: Marketplace (Priority: MEDIUM)
**Estimated effort**: 1-2 weeks

#### 4.1 Marketplace Feature
- [ ] Create MarketplaceItem entity
- [ ] Price, condition, trade method fields
- [ ] Trade status management (selling, reserved, sold)
- [ ] Image requirement validation
- [ ] Marketplace-specific listing page

**API Endpoints**:
- `POST /api/marketplace/items` - Create item
- `PUT /api/marketplace/items/{id}` - Update item
- `PATCH /api/marketplace/items/{id}/status` - Update trade status
- `GET /api/marketplace/items` - List items (with filters)

**Frontend Pages**:
- Marketplace listing page
- Item create/edit form
- Item detail with trade status

---

### Phase 5: Study Groups & Clubs (Priority: MEDIUM)
**Estimated effort**: 2 weeks

#### 5.1 Study Groups
- [ ] Create StudyGroup entity
- [ ] Create StudyGroupMember entity
- [ ] Category management
- [ ] Member limit validation
- [ ] Join/leave APIs
- [ ] Status management (recruiting, closed, in_progress, completed)

**API Endpoints**:
- `POST /api/study-groups` - Create study group
- `POST /api/study-groups/{id}/join` - Join group
- `DELETE /api/study-groups/{id}/leave` - Leave group
- `GET /api/study-groups` - List groups (filter by category, status)
- `PATCH /api/study-groups/{id}/status` - Update status

#### 5.2 Club Promotions
- [ ] Create ClubPromotion entity
- [ ] Club type (university, alliance)
- [ ] Contact info management

**API Endpoints**:
- `POST /api/clubs` - Create club promotion
- `GET /api/clubs` - List clubs (filter by type)

**Frontend Pages**:
- Study group listing
- Study group detail with member list
- Club promotion listing

---

### Phase 6: Messaging (Priority: MEDIUM)
**Estimated effort**: 2 weeks

#### 6.1 Direct Messages
- [ ] Create DirectMessage entity
- [ ] Send message API
- [ ] Read/unread tracking
- [ ] Message list (inbox/sent)
- [ ] Real-time updates (WebSocket or polling)

**API Endpoints**:
- `POST /api/messages` - Send message
- `GET /api/messages/inbox` - Get inbox
- `GET /api/messages/sent` - Get sent messages
- `PATCH /api/messages/{id}/read` - Mark as read
- `GET /api/messages/conversation/{userId}` - Get conversation

#### 6.2 Real-time Chat
- [ ] Create ChatRoom entity
- [ ] Create ChatMessage entity
- [ ] University-specific chat rooms
- [ ] Anonymous chat support
- [ ] WebSocket integration (or SSE)

**API Endpoints**:
- `GET /api/chat/rooms` - List chat rooms
- `GET /api/chat/rooms/{id}/messages` - Get messages
- `POST /api/chat/rooms/{id}/messages` - Send message (WebSocket)

**Frontend Pages**:
- Direct message inbox
- Chat interface
- Message thread view

---

### Phase 7: Course Reviews (Priority: MEDIUM)
**Estimated effort**: 1-2 weeks

#### 7.1 Course Management
- [ ] Create Course entity
- [ ] Course search API
- [ ] Professor name search

#### 7.2 Review System
- [ ] Create CourseReview entity
- [ ] Create CourseReviewReaction entity
- [ ] Rating system (difficulty, workload, satisfaction)
- [ ] One review per user per course
- [ ] Helpful/not helpful reactions

**API Endpoints**:
- `GET /api/courses/search?q={query}` - Search courses
- `POST /api/courses/{id}/reviews` - Write review
- `GET /api/courses/{id}/reviews` - List reviews
- `PUT /api/reviews/{id}` - Update review
- `DELETE /api/reviews/{id}` - Delete review
- `POST /api/reviews/{id}/helpful` - Mark helpful

**Frontend Pages**:
- Course search page
- Course detail with reviews
- Review write/edit form

---

### Phase 8: Admin & Moderation (Priority: HIGH)
**Estimated effort**: 2-3 weeks

#### 8.1 Report System
- [ ] Create Report entity
- [ ] Report submission for posts/comments/messages/users
- [ ] Report status workflow
- [ ] Admin review interface

**API Endpoints**:
- `POST /api/reports` - Submit report
- `GET /api/admin/reports` - List reports (admin only)
- `PATCH /api/admin/reports/{id}` - Review report

#### 8.2 User Management
- [ ] Create UserWarning entity
- [ ] User list with search/filter
- [ ] Account suspension (temporary)
- [ ] Account ban (permanent)
- [ ] Warning system

**API Endpoints**:
- `GET /api/admin/users` - List users
- `GET /api/admin/users/{id}` - User details
- `POST /api/admin/users/{id}/warn` - Issue warning
- `POST /api/admin/users/{id}/suspend` - Suspend account
- `POST /api/admin/users/{id}/ban` - Ban account

#### 8.3 Content Moderation
- [ ] Post blind/unblind
- [ ] Comment deletion
- [ ] AdminActionLog entity for audit trail

**API Endpoints**:
- `PATCH /api/admin/posts/{id}/blind` - Blind post
- `PATCH /api/admin/posts/{id}/unblind` - Unblind post
- `DELETE /api/admin/comments/{id}` - Delete comment
- `GET /api/admin/action-logs` - View admin actions

**Frontend Pages**:
- Admin dashboard
- User management page
- Report queue page
- Content moderation page
- Action log viewer

---

## Technical Considerations

### Security
- [ ] Implement JWT or Session-based authentication
- [ ] Add Spring Security configuration
- [ ] CORS configuration for production
- [ ] Input validation and sanitization
- [ ] SQL injection prevention (use JPA properly)
- [ ] XSS prevention (sanitize HTML in posts/comments)
- [ ] Rate limiting for APIs
- [ ] File upload validation (size, type)

### Performance
- [ ] Database indexing (see DATABASE_SCHEMA.md)
- [ ] Pagination for all list APIs
- [ ] Caching for frequently accessed data (best posts, popular courses)
- [ ] Image optimization and CDN integration
- [ ] Lazy loading for comments
- [ ] Connection pooling (HikariCP already configured)

### DevOps
- [ ] Logging framework (SLF4J + Logback)
- [ ] Error handling and exception mapping
- [ ] API documentation (Swagger/OpenAPI)
- [ ] Environment-specific configurations
- [ ] Database migrations (Flyway or Liquibase)
- [ ] Scheduled tasks for best posts, event archiving
- [ ] File storage solution (local/S3)

### Testing
- [ ] Unit tests for services
- [ ] Integration tests for APIs
- [ ] Security testing
- [ ] Load testing for chat and real-time features

---

## Implementation Priority Summary

### Must-Have (MVP)
1. **Authentication** (Phase 1) - Foundation
2. **Board System** (Phase 2) - Core feature
3. **Comments & Reactions** (Phase 3) - Engagement
4. **Admin Tools** (Phase 8) - Content safety

### Should-Have
5. **Marketplace** (Phase 4)
6. **Study Groups** (Phase 5)
7. **Direct Messages** (Phase 6)
8. **Course Reviews** (Phase 7)

### Nice-to-Have (Future)
- Mobile app
- Push notifications
- Advanced analytics
- AI-powered moderation
- Gamification (badges, points)

---

## Next Steps

### Immediate Actions
1. **Review and approve** this roadmap
2. **Choose starting phase** (recommend Phase 1)
3. **Set up development workflow**:
   - Git branching strategy (feature branches)
   - Code review process
   - Testing requirements
4. **Begin Phase 1 implementation**:
   - Start with User entity enhancement
   - Then authentication APIs
   - Then frontend pages

### Questions to Decide
1. **Authentication method**: JWT vs Session?
2. **File storage**: Local filesystem vs AWS S3?
3. **Real-time features**: WebSocket vs SSE vs polling?
4. **Email service**: Which provider? (SendGrid, AWS SES, etc.)
5. **Image hosting**: Local vs CDN?
6. **Database migrations**: Manual SQL vs Flyway?

Would you like me to start implementing Phase 1, or would you prefer to discuss any specific decisions first?
