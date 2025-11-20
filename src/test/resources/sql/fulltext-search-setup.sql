-- Full-Text Search 인덱스 및 데이터 초기화

-- 1. tsvector 컬럼 추가
ALTER TABLE product_search_document
    ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- 2. 기존 데이터에 대해 search_vector 값 생성
UPDATE product_search_document
SET search_vector = to_tsvector('simple',
                                COALESCE(product_name, '') || ' ' ||
                                COALESCE(brand_name, '') || ' ' ||
                                COALESCE(category_name, '') || ' ' ||
                                COALESCE(model_number, '') || ' ' ||
                                COALESCE(product_description, '')
                    );

-- 3. GIN 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_product_search_vector
    ON product_search_document
    USING GIN(search_vector);

-- 4. 필터용 일반 인덱스
CREATE INDEX IF NOT EXISTS idx_product_search_brand
    ON product_search_document (brand_id);

CREATE INDEX IF NOT EXISTS idx_product_search_category
    ON product_search_document (category_id);

-- 5. 복합 인덱스 (브랜드 + 카테고리 필터)
CREATE INDEX IF NOT EXISTS idx_product_search_brand_category
    ON product_search_document (brand_id, category_id);