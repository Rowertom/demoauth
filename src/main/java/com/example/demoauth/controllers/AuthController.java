package com.example.demoauth.controllers;

import com.example.demoauth.dao.ERole;
import com.example.demoauth.dao.Role;
import com.example.demoauth.dao.User;
import com.example.demoauth.jwt.JwtUtils;
import com.example.demoauth.pojo.JwtResponse;
import com.example.demoauth.pojo.LoginRequest;
import com.example.demoauth.pojo.MessageResponse;
import com.example.demoauth.pojo.SingUpRequest;
import com.example.demoauth.repository.RoleRepository;
import com.example.demoauth.repository.UserRepository;
import com.example.demoauth.service.UserDet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/signing")
    public ResponseEntity<?> authUser(@RequestBody LoginRequest loginRequest){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDet userDet = (UserDet) authentication.getPrincipal();
        List<String> roles = userDet.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());
        return ResponseEntity.ok(new JwtResponse(jwt, userDet.getId(),
                userDet.getUsername(), userDet.getEmail(), userDet.getPhoneNumber(), roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SingUpRequest singUpRequest){
        if(userRepository.existsByUsername(singUpRequest.getUsername())){
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is exist"));
        }
        if(userRepository.existsByEmail(singUpRequest.getEmail())){
            return  ResponseEntity.badRequest().body((new MessageResponse("Error: Email is exist")));
        }
        if(userRepository.existsByPhoneNumber(singUpRequest.getPhoneNumber())){
            return ResponseEntity.badRequest().body(new MessageResponse("Error: PhoneNumber is exist"));
        }
        User user = new User(singUpRequest.getUsername(),
                singUpRequest.getEmail(), singUpRequest.getPhoneNumber(),
                passwordEncoder.encode(singUpRequest.getPassword()));
        Set<String> reqRoles = singUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if(reqRoles == null){
            Role userRole = roleRepository
                    .findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error, Role USER is not found"));
            roles.add(userRole);
        }else{
            reqRoles.forEach(r ->{
                switch(r){
                    case "admin":
                        Role adminRole = roleRepository
                                .findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(()-> new RuntimeException("Error, Role ADMIN is not found"));
                        roles.add(adminRole);
                        break;
                    case "mod":
                        Role modRole = roleRepository
                                .findByName(ERole.ROLE_MODERATOR)
                                .orElseThrow(()-> new RuntimeException("Error, Role MODERATOR is not found"));
                        roles.add(modRole);
                        break;
                    default:
                        Role userRole = roleRepository
                                .findByName(ERole.ROLE_USER)
                                .orElseThrow(()-> new RuntimeException("Error, Role USER is not found"));
                        roles.add(userRole);
                }
            });
        }
        user.setRoles(roles);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User CREATED"));
    }
}
