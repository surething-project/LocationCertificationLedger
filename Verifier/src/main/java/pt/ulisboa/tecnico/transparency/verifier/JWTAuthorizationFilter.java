package pt.ulisboa.tecnico.transparency.verifier;

import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.crypto.MacProvider;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.security.RolesAllowed;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JWTAuthorizationFilter extends OncePerRequestFilter {
  public static Logger logger = LoggerFactory.getLogger(JWTAuthorizationFilter.class);
  public static final String ROLES_CLAIM = "roles";
  private static final long TTL_MILLIS = 60 * 60 * 1000;
  private static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;
  private static byte[] signingKey = new byte[0];

  @Autowired
  public JWTAuthorizationFilter() {
    SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    signingKey = DatatypeConverter.parseBase64Binary(Encoders.BASE64.encode(key.getEncoded()));
  }

  public static String createJwt(List<String> roles) {

    Map<String, Object> claims = new HashMap<>();
    claims.put(ROLES_CLAIM, roles);
    claims.put(Claims.EXPIRATION, new Date(System.currentTimeMillis() + TTL_MILLIS));
    return Jwts.builder()
        .setClaims(claims)
        .signWith(new SecretKeySpec(signingKey, SIGNATURE_ALGORITHM.getJcaName()))
        .compact();
  }

  public static Jws<Claims> decodeJwt(String jwt) {
    return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(jwt);
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    try {
      String HEADER = "Authorization";
      String jwt = request.getHeader(HEADER);
      if (jwt == null) {
        SecurityContextHolder.clearContext();
      }
      else {
        Claims claims = decodeJwt(jwt).getBody();
        System.out.println(claims);
        if (claims == null || !claims.containsKey(ROLES_CLAIM)) {
          SecurityContextHolder.clearContext();
        } else {
          setUpSpringAuthentication(claims);
        }
      }
      chain.doFilter(request, response);
    } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException e) {
      logger.error(e.getMessage());
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
    }
  }

  private void setUpSpringAuthentication(Claims claims) {
    @SuppressWarnings("unchecked")
    List<String> authorities = (List<String>) claims.get(ROLES_CLAIM);
    System.out.println(claims.getSubject());
    System.out.println(authorities);
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null,
            authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
    SecurityContextHolder.getContext().setAuthentication(auth);

  }
}
