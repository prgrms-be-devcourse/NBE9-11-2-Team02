package com.back.together02be.asset.controller;

import com.back.together02be.asset.enitity.UserAccount;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.users.enitity.Users;
import com.back.together02be.users.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
public class AssetControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private UsersRepository usersRepository;

    @BeforeEach
    void setUp(){
        Users testUser = new Users("user1","1234","my_nick");
        usersRepository.save(testUser);
        UserAccount testUserAccount = new UserAccount(testUser,10000L,1000000L);
        userAccountRepository.save(testUserAccount);
    }

    @Test
    @DisplayName("총매수금 출력")
    void test1() throws Exception{
        mockMvc.perform(get("/api/asset/accounts/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(10000L));
    }
}
