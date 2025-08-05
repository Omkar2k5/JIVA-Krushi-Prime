package com.example.jiva

import com.example.jiva.utils.SecurityUtils
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for SecurityUtils
 * Critical for production security with 100+ users
 */
class SecurityUtilsTest {

    @Test
    fun `isValidUsername should validate correct usernames`() {
        assertTrue(SecurityUtils.isValidUsername("testuser"))
        assertTrue(SecurityUtils.isValidUsername("test_user"))
        assertTrue(SecurityUtils.isValidUsername("test.user"))
        assertTrue(SecurityUtils.isValidUsername("test-user"))
        assertTrue(SecurityUtils.isValidUsername("user123"))
        assertTrue(SecurityUtils.isValidUsername("123user"))
    }

    @Test
    fun `isValidUsername should reject invalid usernames`() {
        assertFalse(SecurityUtils.isValidUsername("")) // Empty
        assertFalse(SecurityUtils.isValidUsername("ab")) // Too short
        assertFalse(SecurityUtils.isValidUsername("a".repeat(21))) // Too long
        assertFalse(SecurityUtils.isValidUsername("test user")) // Space
        assertFalse(SecurityUtils.isValidUsername("test@user")) // Invalid character
        assertFalse(SecurityUtils.isValidUsername("test#user")) // Invalid character
    }

    @Test
    fun `isValidEmail should validate correct emails`() {
        assertTrue(SecurityUtils.isValidEmail("test@example.com"))
        assertTrue(SecurityUtils.isValidEmail("user.name@domain.co.uk"))
        assertTrue(SecurityUtils.isValidEmail("test+tag@example.org"))
        assertTrue(SecurityUtils.isValidEmail("user123@test-domain.com"))
    }

    @Test
    fun `isValidEmail should reject invalid emails`() {
        assertFalse(SecurityUtils.isValidEmail("")) // Empty
        assertFalse(SecurityUtils.isValidEmail("invalid")) // No @
        assertFalse(SecurityUtils.isValidEmail("@example.com")) // No local part
        assertFalse(SecurityUtils.isValidEmail("test@")) // No domain
        assertFalse(SecurityUtils.isValidEmail("test@.com")) // Invalid domain
        assertFalse(SecurityUtils.isValidEmail("test..test@example.com")) // Double dot
    }

    @Test
    fun `isValidPassword should validate passwords`() {
        assertTrue(SecurityUtils.isValidPassword("password123"))
        assertTrue(SecurityUtils.isValidPassword("123456"))
        assertTrue(SecurityUtils.isValidPassword("abcdef"))
        assertTrue(SecurityUtils.isValidPassword("P@ssw0rd!"))
    }

    @Test
    fun `isValidPassword should reject invalid passwords`() {
        assertFalse(SecurityUtils.isValidPassword("")) // Empty
        assertFalse(SecurityUtils.isValidPassword("12345")) // Too short
        assertFalse(SecurityUtils.isValidPassword("     ")) // Only spaces
    }

    @Test
    fun `isStrongPassword should validate strong passwords`() {
        assertTrue(SecurityUtils.isStrongPassword("Password123"))
        assertTrue(SecurityUtils.isStrongPassword("MyP@ssw0rd"))
        assertTrue(SecurityUtils.isStrongPassword("Str0ng!Pass"))
    }

    @Test
    fun `isStrongPassword should reject weak passwords`() {
        assertFalse(SecurityUtils.isStrongPassword("password")) // No uppercase, no digit
        assertFalse(SecurityUtils.isStrongPassword("PASSWORD")) // No lowercase, no digit
        assertFalse(SecurityUtils.isStrongPassword("Password")) // No digit
        assertFalse(SecurityUtils.isStrongPassword("password123")) // No uppercase
        assertFalse(SecurityUtils.isStrongPassword("Pass123")) // Too short
    }

    @Test
    fun `sanitizeInput should escape dangerous characters`() {
        assertEquals("&lt;script&gt;", SecurityUtils.sanitizeInput("<script>"))
        assertEquals("&quot;test&quot;", SecurityUtils.sanitizeInput("\"test\""))
        assertEquals("&#x27;test&#x27;", SecurityUtils.sanitizeInput("'test'"))
        assertEquals("test&#x2F;path", SecurityUtils.sanitizeInput("test/path"))
        assertEquals("normal text", SecurityUtils.sanitizeInput("normal text"))
        assertEquals("test", SecurityUtils.sanitizeInput("  test  ")) // Trim spaces
    }

    @Test
    fun `hashPassword should generate consistent hashes`() {
        val password = "testpassword"
        val hash1 = SecurityUtils.hashPassword(password)
        val hash2 = SecurityUtils.hashPassword(password)
        
        assertEquals(hash1, hash2) // Same password should produce same hash
        assertNotEquals(password, hash1) // Hash should be different from password
        assertTrue(hash1.isNotEmpty()) // Hash should not be empty
    }

    @Test
    fun `generateSecureToken should generate unique tokens`() {
        val token1 = SecurityUtils.generateSecureToken()
        val token2 = SecurityUtils.generateSecureToken()
        
        assertNotEquals(token1, token2) // Tokens should be unique
        assertTrue(token1.isNotEmpty()) // Token should not be empty
        assertTrue(token2.isNotEmpty()) // Token should not be empty
        assertTrue(token1.length >= 32) // Token should be reasonably long
    }

    @Test
    fun `isValidSessionToken should validate tokens`() {
        assertTrue(SecurityUtils.isValidSessionToken("a".repeat(32)))
        assertTrue(SecurityUtils.isValidSessionToken("valid-session-token-12345678901234567890"))
        assertFalse(SecurityUtils.isValidSessionToken("")) // Empty
        assertFalse(SecurityUtils.isValidSessionToken("short")) // Too short
        assertFalse(SecurityUtils.isValidSessionToken("   ")) // Only spaces
    }

    @Test
    fun `isSessionExpired should check expiration correctly`() {
        val now = System.currentTimeMillis()
        val futureTime = now + 60000 // 1 minute in future
        val pastTime = now - 60000 // 1 minute in past
        
        assertFalse(SecurityUtils.isSessionExpired(futureTime)) // Not expired
        assertTrue(SecurityUtils.isSessionExpired(pastTime)) // Expired
        assertTrue(SecurityUtils.isSessionExpired(now - 1)) // Just expired
    }

    @Test
    fun `RateLimiter should allow requests within limit`() {
        val rateLimiter = SecurityUtils.RateLimiter(maxAttempts = 3, timeWindowMs = 60000)
        val identifier = "test-user"
        
        // First 3 attempts should be allowed
        assertTrue(rateLimiter.isAllowed(identifier))
        rateLimiter.recordAttempt(identifier)
        
        assertTrue(rateLimiter.isAllowed(identifier))
        rateLimiter.recordAttempt(identifier)
        
        assertTrue(rateLimiter.isAllowed(identifier))
        rateLimiter.recordAttempt(identifier)
        
        // 4th attempt should be blocked
        assertFalse(rateLimiter.isAllowed(identifier))
    }

    @Test
    fun `RateLimiter should track different identifiers separately`() {
        val rateLimiter = SecurityUtils.RateLimiter(maxAttempts = 2, timeWindowMs = 60000)
        
        // User 1 makes 2 attempts
        assertTrue(rateLimiter.isAllowed("user1"))
        rateLimiter.recordAttempt("user1")
        assertTrue(rateLimiter.isAllowed("user1"))
        rateLimiter.recordAttempt("user1")
        assertFalse(rateLimiter.isAllowed("user1")) // Blocked
        
        // User 2 should still be allowed
        assertTrue(rateLimiter.isAllowed("user2"))
        rateLimiter.recordAttempt("user2")
        assertTrue(rateLimiter.isAllowed("user2"))
    }
}
