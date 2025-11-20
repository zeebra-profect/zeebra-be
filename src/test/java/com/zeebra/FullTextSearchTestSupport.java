package com.zeebra;

import com.zeebra.config.TestContainerConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
@Transactional
@Sql(
        scripts = "/sql/fulltext-search-setup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
@Sql(
        scripts = "/sql/fulltext-search-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD  // ← 각 테스트 전에 실행
)
public abstract class FullTextSearchTestSupport {
}