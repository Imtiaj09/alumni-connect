package com.alumniconnect.service;

import com.alumniconnect.entity.Role;
import com.alumniconnect.entity.User;
import com.alumniconnect.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("ইউজার পাওয়া যায়নি: " + email));
        if (user.getRole() == null) {
            throw new UsernameNotFoundException("ইউজার রোল সেট করা নেই: " + email);
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        if (user.getRole() == Role.BATCH_CONTROLLER) {
            authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                !user.isSuspended(),
                authorities
        );
    }
}
