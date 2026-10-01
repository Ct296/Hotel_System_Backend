package com.hotel.system.security;
import com.hotel.system.entity.Account;
import com.hotel.system.entity.Users;
import com.hotel.system.repository.AccountRepository;
import com.hotel.system.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với email: " + email));
        Account account = accountRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy account"));
        return UserDetailsImpl.build(account);
    }
}