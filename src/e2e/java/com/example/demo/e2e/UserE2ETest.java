package com.example.demo.e2e;

import com.example.demo.domain.Users;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Transactional
public class UserE2ETest {


    @Autowired
    private UserRepository userRepository;

    @Test
    void testFullCrudFlow() {
        Users user = new Users();
        user.setName("Santhosh");
        user.setEmail("test@example.com");

        user = userRepository.save(user);
        assertNotNull(user.getId());

        Users updated = userRepository.findById(user.getId()).get();
        updated.setEmail("updated@example.com");
        userRepository.save(updated);

        assertEquals("updated@example.com", userRepository.findById(user.getId()).get().getEmail());

        userRepository.deleteById(user.getId());
        assertFalse(userRepository.findById(user.getId()).isPresent());
    }
}
