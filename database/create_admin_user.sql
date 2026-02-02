-- ========================================
-- Admin User Creation Script (With BCrypt Hashing)
-- ========================================
-- This script creates an admin user for the EJ2 Timetable Management System
-- Database: appdb (MariaDB 10.6)
--
-- IMPORTANT: Passwords are hashed using BCrypt (12 rounds)
--
-- Usage Method 1 (Recommended - Generate fresh hash):
--   1. cd backend
--   2. mvn compile
--   3. Run: mvn exec:java -Dexec.mainClass="com.ej2.util.PasswordUtil"
--      Or: java -cp target/classes:~/.m2/repository/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar com.ej2.util.PasswordUtil
--   4. Copy the generated hash and replace HASH_HERE below
--   5. Execute this SQL script
--
-- Usage Method 2 (Quick - Use pre-generated hash):
--   1. Use the default hash provided below (password: admin123)
--   2. docker exec -it <mariadb_container_name> mysql -u appuser -p
--   3. source /path/to/create_admin_user.sql
-- ========================================

USE appdb;

-- Check if admin user already exists
SELECT 'Checking for existing admin user...' AS status;
SELECT id, username, email, role FROM users WHERE username = 'admin' OR role = 'ADMIN';

-- ========================================
-- Create Admin User with BCrypt Password
-- ========================================
-- Default credentials:
--   Username: admin
--   Password: admin123
--   Email: admin@ej2.com
--
-- BCrypt Hash (12 rounds): $2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIpqBu4dKu
--
-- To generate a new hash for a different password:
--   Run: java -cp backend/target/classes:~/.m2/repository/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar com.ej2.util.PasswordUtil
-- ========================================

INSERT INTO users (
    username,
    name,
    email,
    password,
    role,
    created_at,
    updated_at
) VALUES (
    'admin',
    'System Administrator',
    'admin@ej2.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIpqBu4dKu',  -- BCrypt hash for 'admin123'
    'ADMIN',
    NOW(),
    NOW()
);

-- Verify admin user creation
SELECT 'Admin user created successfully!' AS status;
SELECT id, username, name, email, role, created_at FROM users WHERE username = 'admin';

-- ========================================
-- Optional: Create Multiple Admin Users
-- ========================================
-- Pre-generated BCrypt hashes (all use password: admin123):

/*
INSERT INTO users (username, name, email, password, role, created_at, updated_at)
VALUES
    ('superadmin', 'Super Admin', 'superadmin@ej2.com',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIpqBu4dKu',
     'ADMIN', NOW(), NOW()),
    ('moderator', 'Moderator', 'mod@ej2.com',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIpqBu4dKu',
     'ADMIN', NOW(), NOW());
*/

-- ========================================
-- Generate New Password Hash
-- ========================================
-- To create a hash for a custom password, you have 3 options:
--
-- Option 1: Use the existing GenerateHash.java utility
--   1. Edit backend/GenerateHash.java and change the password
--   2. Compile: cd backend && javac -cp ~/.m2/repository/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar GenerateHash.java
--   3. Run: java -cp .:~/.m2/repository/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar GenerateHash
--
-- Option 2: Use PasswordUtil main method
--   1. Edit backend/src/main/java/com/ej2/util/PasswordUtil.java line 43
--   2. Run: mvn exec:java -Dexec.mainClass="com.ej2.util.PasswordUtil"
--
-- Option 3: Use online BCrypt generator (NOT recommended for production)
--   - Visit: https://bcrypt-generator.com/
--   - Set rounds to 12
--   - Generate hash and copy here
--
-- ========================================

-- ========================================
-- Useful Admin Management Queries
-- ========================================

-- List all admin users
-- SELECT id, username, name, email, role, created_at FROM users WHERE role = 'ADMIN';

-- List all users with their roles
-- SELECT id, username, name, email, role, created_at FROM users ORDER BY role DESC, created_at DESC;

-- Promote existing user to admin
-- UPDATE users SET role = 'ADMIN', updated_at = NOW() WHERE username = 'existing_username';

-- Demote admin to regular user
-- UPDATE users SET role = 'USER', updated_at = NOW() WHERE username = 'admin_username';

-- Change admin password (replace HASH_HERE with new BCrypt hash)
-- UPDATE users SET password = 'HASH_HERE', updated_at = NOW() WHERE username = 'admin';

-- Delete admin user (use with extreme caution!)
-- DELETE FROM users WHERE username = 'admin';

-- Count users by role
-- SELECT role, COUNT(*) as count FROM users GROUP BY role;

-- ========================================
-- Security Best Practices
-- ========================================
-- ✓ Passwords are hashed with BCrypt (12 rounds)
-- ✓ Change default password after first login
-- ⚠ TODO: Implement password complexity requirements
-- ⚠ TODO: Add multi-factor authentication (MFA)
-- ⚠ TODO: Implement session management with JWT
-- ⚠ TODO: Add rate limiting for login attempts
-- ⚠ TODO: Log all admin actions for audit trail
-- ⚠ TODO: Implement password expiration policy
-- ⚠ TODO: Add account lockout after failed login attempts
-- ========================================

-- ========================================
-- Test Admin Login
-- ========================================
-- After creating the admin user, test login with:
--   Username: admin
--   Password: admin123
--
-- The AuthService.java should use PasswordUtil.verifyPassword()
-- to validate credentials
-- ========================================
