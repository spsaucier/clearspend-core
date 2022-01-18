package com.clearspend.capital.service;

import com.clearspend.capital.data.model.User;
import java.util.Collection;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Profile("test")
@Component
@RequiredArgsConstructor
public class TestUserDetailsService implements UserDetailsService {

  private final UserService userService;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Optional<User> u = userService.retrieveUserBySubjectRef(username);
    UserDetails ud =
        new UserDetails() {

          @Override
          public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
          }

          @Override
          public String getPassword() {
            return null;
          }

          @Override
          public String getUsername() {
            return null;
          }

          @Override
          public boolean isAccountNonExpired() {
            return false;
          }

          @Override
          public boolean isAccountNonLocked() {
            return false;
          }

          @Override
          public boolean isCredentialsNonExpired() {
            return false;
          }

          @Override
          public boolean isEnabled() {
            return false;
          }
        };

    return null;
  }
}
