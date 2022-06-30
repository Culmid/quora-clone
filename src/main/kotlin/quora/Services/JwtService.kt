package quora.Services

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
@PropertySource("classpath:application.properties")
class JwtService {
    @Autowired
    private val env: Environment? = null

    fun generateJWT(id: Int): String {
        val secretKey = env?.getProperty("jwt-secret-key") ?: "12345789"
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))

        return Jwts.builder()
            .setIssuer("quora")
            .setSubject("userAuth")
            .setId("$id")
            .setIssuedAt(Date.from(Instant.now())) // Current Time
            .setExpiration(Date.from(Instant.now().plusSeconds(env?.get("jwt-expire-period")?.toLong() ?: 86400L))) // One Day Later
            .signWith(key)
            .compact()
    }

    fun parseJWT(authHeader: String): Jws<Claims>? {
        val bearerToken = authHeader.split(" ")[1] // Assume Correct Format -> Bearer <Token>
        val secretKey = env?.getProperty("jwt-secret-key") ?: "12345789"
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))

        return  try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(bearerToken);
        } catch (e: JwtException) {
            null
        }
    }
}