package ch.bbw.climategame.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Prüft für Admin-Endpunkte den Header X-Admin-Key (oder Authorization: Bearer)
 * mittels zeitkonstantem Vergleich (MessageDigest.isEqual) gegen APP_ADMIN_SECRET.
 */
@Component
public class AdminAuthenticationFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;

    public AdminAuthenticationFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String configuredSecret = appProperties.getAdminSecret();
        String providedSecret = extractSecretFromRequest(request);

        if (configuredSecret != null && !configuredSecret.isBlank()
                && providedSecret != null && !providedSecret.isBlank()) {
            byte[] expectedBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
            byte[] actualBytes = providedSecret.getBytes(StandardCharsets.UTF_8);

            if (MessageDigest.isEqual(expectedBytes, actualBytes)) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractSecretFromRequest(HttpServletRequest request) {
        String headerKey = request.getHeader("X-Admin-Key");
        if (headerKey != null && !headerKey.isBlank()) {
            return headerKey.trim();
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        return null;
    }
}
