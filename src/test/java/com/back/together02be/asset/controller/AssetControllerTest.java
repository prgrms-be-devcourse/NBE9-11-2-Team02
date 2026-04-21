package com.back.together02be.asset.controller;

import com.back.together02be.asset.entity.UserAccount;
import com.back.together02be.asset.entity.UserStock;
import com.back.together02be.asset.repository.UserAccountRepository;
import com.back.together02be.asset.repository.UserStockRepository;
import com.back.together02be.stock.enitity.Stock;
import com.back.together02be.stock.repository.StockRepository;
import com.back.together02be.users.enitity.Users;
import com.back.together02be.users.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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
    @Autowired
    private StockRepository StockRepository;
    @Autowired
    private UserStockRepository userStockRepository;
    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void setUp(){
        Users testUser = new Users("user1","1234","my_nick");
        usersRepository.save(testUser);

        UserAccount testUserAccount = new UserAccount(testUser,10000L,1000000L);
        userAccountRepository.save(testUserAccount);

        Stock samsung = new Stock("005930", "삼성전자","kospi");
        Stock hynix = new Stock("000660", "SK하이닉스","kospi");
        stockRepository.saveAll(List.of(samsung, hynix));

        UserStock us1 = new UserStock(testUser, samsung, 10L, 5000L);
        UserStock us2 = new UserStock(testUser, hynix, 2L, 10000L);
        userStockRepository.saveAll(List.of(us1,us2));
    }

    @Test
    @DisplayName("총매수금 및 보유 주식 목록 조회 테스트")
    void test1() throws Exception{
        mockMvc.perform(get("/api/asset/accounts/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(10000L))
                // 2. 리스트 크기 검증
                .andExpect(jsonPath("$.data.stocks.length()").value(2))
                // 3. 구체적인 데이터 검증 (삼성전자)
                .andExpect(jsonPath("$.data.stocks[0].stockCode").value("005930"))
                .andExpect(jsonPath("$.data.stocks[0].quantity").value(10))
                // 4. 구체적인 데이터 검증 (SK하이닉스)
                .andExpect(jsonPath("$.data.stocks[1].stockCode").value("000660"))
                .andExpect(jsonPath("$.data.stocks[1].quantity").value(2));
    }
}
