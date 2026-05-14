package com.ptit.chess.controller;

import com.ptit.chess.dto.JwtAuthenticationResponse;
import com.ptit.chess.dto.LoginRequest;
import com.ptit.chess.dto.RegisterRequest;
import com.ptit.chess.entity.Account;
import com.ptit.chess.entity.AccountStatus;
import com.ptit.chess.entity.Player;
import com.ptit.chess.entity.Role;
import com.ptit.chess.repository.AccountRepository;
import com.ptit.chess.repository.PlayerRepository;
import com.ptit.chess.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest signUpRequest) {
        if(accountRepository.existsByUsername(signUpRequest.getUsername())) {
            return new ResponseEntity<>("Username is already taken!", HttpStatus.BAD_REQUEST);
        }

        if(playerRepository.existsByDisplayName(signUpRequest.getDisplayName())) {
            return new ResponseEntity<>("Display name is already taken!", HttpStatus.BAD_REQUEST);
        }

        // Creating user's account
        Account account = Account.builder()
                .username(signUpRequest.getUsername())
                .passwordHash(passwordEncoder.encode(signUpRequest.getPassword()))
                .role(Role.PLAYER)
                .status(AccountStatus.ACTIVE)
                .build();

        Player player = Player.builder()
                .account(account)
                .displayName(signUpRequest.getDisplayName())
                .build();
                
        account.setPlayer(player);

        accountRepository.save(account);

        return ResponseEntity.ok("User registered successfully");
    }
}
