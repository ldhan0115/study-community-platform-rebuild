package com.study.studycommunity.global.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DatabaseMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsRequiredTables() {

        // 현재 데이터베이스에 생성된 핵심 테이블 개수 조회
        Long tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                      'member',
                      'study',
                      'application',
                      'comment'
                  )
                """,
                Long.class
        );

        assertThat(tableCount).isEqualTo(4L);
    }
}