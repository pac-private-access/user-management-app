package com.pac.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pac.repository.SmartphoneRepository;
import com.pac.repository.UserRepository;
import com.pac.service.EmployeeDetailsService;
import com.pac.service.JwtService;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean EmployeeDetailsService employeeDetailsService;
    @MockitoBean JwtService jwtService;

    // GlobalModelAttributeAdvice dependencies
    @MockitoBean SmartphoneRepository smartphoneRepository;
    @MockitoBean UserRepository userRepository;

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        UserDetails userDetails = new User("admin@pac.ro", "hash", List.of());
        given(authenticationManager.authenticate(any()))
                .willReturn(new UsernamePasswordAuthenticationToken("admin@pac.ro", null, List.of()));
        given(employeeDetailsService.loadUserByUsername("admin@pac.ro")).willReturn(userDetails);
        given(jwtService.generateToken(userDetails)).willReturn("signed.jwt.token");

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@pac.ro\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed.jwt.token"));
    }

    @Test
    void login_invalidCredentials_returns4xx() throws Exception {
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"hacker@pac.ro\",\"password\":\"wrong\"}"))
                .andExpect(status().is4xxClientError());
    }

}
