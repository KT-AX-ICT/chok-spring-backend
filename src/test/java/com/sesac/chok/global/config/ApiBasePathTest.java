package com.sesac.chok.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모든 도메인 REST 컨트롤러에 {@code /api/v1} base path가 적용되는지 검증한다.
 * 패키지 프리픽스 방식이라 {@code com.sesac.chok} 컨트롤러만 prefix가 붙고,
 * h2-console 같은 인프라 경로는 루트에 그대로 남는다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ApiBasePathTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void analysisEndpointIsServedUnderApiV1Prefix() throws Exception {
        mockMvc.perform(get("/api/v1/analysis"))
                .andExpect(status().isOk());
    }

    @Test
    void analysisEndpointIsNotMappedWithoutPrefix() throws Exception {
        mockMvc.perform(get("/analysis"))
                .andExpect(status().isNotFound());
    }
}
