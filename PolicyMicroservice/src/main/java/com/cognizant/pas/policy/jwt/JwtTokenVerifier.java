package com.cognizant.pas.policy.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JwtTokenVerifier extends OncePerRequestFilter {

    private final JwtUtility jwtUtility;

    public JwtTokenVerifier(JwtUtility jwtUtility) {
        this.jwtUtility = jwtUtility;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = httpServletRequest.getHeader(jwtUtility.getTokenHeader());
        if(header == null || header.isEmpty() || !header.startsWith(jwtUtility.getTokenPrefix() + " ")) {
            filterChain.doFilter(httpServletRequest,httpServletResponse);
            return;
        }

        try {
            String token = header.replace(jwtUtility.getTokenPrefix() + " ", "");
            Jws<Claims> claimsJws = Jwts.parser()
                    .setSigningKey(jwtUtility.getSecretKey().getBytes())
                    .parseClaimsJws(token);
            Claims body = claimsJws.getBody();
            String username = body.getSubject();
            List<Map<String,String>> authorities = (List<Map<String,String>>) body.get(jwtUtility.getClaimsName());
            Set<SimpleGrantedAuthority> authority = authorities.stream().map(i -> new SimpleGrantedAuthority(i.get(jwtUtility.getAuthority()))).collect(Collectors.toSet());
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    "",
                    authority
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }catch (JwtException e) {
            throw new IllegalStateException("Invalid Token");
        }
        filterChain.doFilter(httpServletRequest, httpServletResponse);

    }
}
