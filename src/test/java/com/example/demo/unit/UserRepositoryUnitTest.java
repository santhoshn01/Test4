package com.example.demo.unit;

import com.example.demo.domain.Users;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import java.util.Optional;

public class UserRepositoryUnitTest {

    @Test
    void testFindByIdMocked() {
        UserRepository mockRepo = Mockito.mock(UserRepository.class);
        Users user = new Users();
        user.setId(1L);
        user.setName("Mock User");
        user.setEmail("mock@example.com");

        when(mockRepo.findById(1L)).thenReturn(Optional.of(user));

        Optional<Users> result = mockRepo.findById(1L);

        assertTrue(result.isPresent(), "User should be present");
        assertEquals("Mock User", result.get().getName());
        assertEquals("mock@example.com", result.get().getEmail());
    }
}
