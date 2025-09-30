package org.example.schemaflow.shared.config.webSocket;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.example.schemaflow.shared.config.TokenJWTConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class JwtTokenProvider {

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(TokenJWTConfig.SECRET_KEY)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(TokenJWTConfig.SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Asumiendo que el userId esta en el subject o como un claim personalizado
            return  claims.get("userId", String.class) != null ?
                    claims.get("userId", String.class) :
                    claims.getSubject();
        } catch (Exception e) {
            log.error("Error extracting userId from JWT token: {}", e.getMessage());
            return null;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(TokenJWTConfig.SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Asumiendo que el username esta en el subject o como un claim personalizado
            return  claims.get("username", String.class) != null ?
                    claims.get("username", String.class) :
                    claims.get("sub", String.class);

        } catch (Exception e) {
            log.error("Error extracting username from JWT token", e);
            return null;
        }
    }

    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(TokenJWTConfig.SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Extraer roles si estan presentes
            Object roles = claims.get("roles");
            if (roles instanceof List) {
                return (List<String>) roles;
            }

            // Fallback: buscar authorities
            Object authorities = claims.get("authorities");
            if ( authorities instanceof List) {
                return (List<String>) authorities;
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error extracting roles from JWT token", e);
            return new ArrayList<>();
        }
    }







































}
