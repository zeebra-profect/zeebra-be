INSERT INTO category (parent_id, name)
VALUES (NULL, '뷰티'),
       (NULL, '신발'),
       (NULL, '상의'),
       (NULL, '아우터'),
       (NULL, '바지'),
       (NULL, '원피스/스커트'),
       (NULL, '가방'),
       (NULL, '패션소품'),
       (NULL, '속옷/홈웨어'),
       (NULL, '스포츠/레저'),
       (NULL, '디지털/라이프'),
       (NULL, '아울렛'),
       (NULL, '부티크'),
       (NULL, '키즈'),
       (1, '스킨케어');

INSERT INTO brand (id, name)
VALUES (1, '감성가먼트');

INSERT
INTO product(id, name, model_number, description, brand_id,
             category_id, review_count, favorite_product_count)
VALUES (18, '감성가먼트 스킨케어 코튼 레귤러핏 베이직', NULL, NULL, 1, 15, 0, 0),
       (19, '감성가먼트 스킨케어 코튼 레귤러핏 로고', NULL, NULL, 1, 15, 0, 0),
       (20, '감성가먼트 스킨케어 코튼 레귤러핏 포켓', NULL, NULL, 1, 15, 0, 0),
       (21, '감성가먼트 스킨케어 코튼 오버핏 베이직', NULL, NULL, 1, 15, 0, 0),
       (22, '감성가먼트 스킨케어 코튼 오버핏 로고', NULL, NULL, 1, 15, 0, 0),
       (23, '감성가먼트 스킨케어 코튼 오버핏 포켓', NULL, NULL, 1, 15, 0, 0),
       (24, '감성가먼트 스킨케어 코튼 슬림핏 베이직', NULL, NULL, 1, 15, 0, 0),
       (25, '감성가먼트 스킨케어 코튼 슬림핏 로고', NULL, NULL, 1, 15, 0, 0),
       (26, '감성가먼트 스킨케어 코튼 슬림핏 포켓', NULL, NULL, 1, 15, 0, 0),
       (27, '감성가먼트 스킨케어 폴리 레귤러핏 베이직', NULL, NULL, 1, 15, 0, 0);

INSERT INTO product_search_document (product_id, product_name, model_number, product_description, brand_id, brand_name,
                                     category_id, category_name, review_count, favorite_count, search_vector,
                                     updated_at)
VALUES (18, '감성가먼트 스킨케어 코튼 레귤러핏 베이직', NULL, NULL, 1, '감성가먼트', 15, '스킨케어', 0, 0,
        '''감성가먼트'':1A,6B ''레귤러핏'':4A ''베이직'':5A ''스킨케어'':2A,7C ''코튼'':3A'::tsvector, '2025-11-18 18:49:24.456195'),
       (19, '감성가먼트 스킨케어 코튼 레귤러핏 로고', NULL, NULL, 1, '감성가먼트', 15, '스킨케어', 0, 0,
        '''감성가먼트'':1A,6B ''레귤러핏'':4A ''로고'':5A ''스킨케어'':2A,7C ''코튼'':3A'::tsvector, '2025-11-18 18:49:24.456195'),
       (20, '감성가먼트 스킨케어 코튼 레귤러핏 포켓', NULL, NULL, 1, '감성가먼트', 15, '스킨케어', 0, 0,
        '''감성가먼트'':1A,6B ''레귤러핏'':4A ''스킨케어'':2A,7C ''코튼'':3A ''포켓'':5A'::tsvector, '2025-11-18 18:49:24.456195'),
       (21, '감성가먼트 스킨케어 코튼 오버핏 베이직', NULL, NULL, 1, '감성가먼트', 15, '스킨케어', 0, 0,
        '''감성가먼트'':1A,6B ''베이직'':5A ''스킨케어'':2A,7C ''오버핏'':4A ''코튼'':3A'::tsvector, '2025-11-18 18:49:24.456195'),
       (22, '감성가먼트 스킨케어 코튼 오버핏 로고', NULL, NULL, 1, '감성가먼트', 15, '스킨케어', 0, 0,
        '''감성가먼트'':1A,6B ''로고'':5A ''스킨케어'':2A,7C ''오버핏'':4A ''코튼'':3A'::tsvector, '2025-11-18 18:49:24.456195'),
       (23, '감성가먼트 스킨케어 코튼 오버핏 포켓', NULL, NULL, 1, '감성가먼트', 15, '스킨케어', 0, 0,
        '''감성가먼트'':1A,6B ''스킨케어'':2A,7C ''오버핏'':4A ''코튼'':3A ''포켓'':5A'::tsvector, '2025-11-18 18:49:24.456195'),
       (24, '감성가먼트 스킨케어 코튼 슬림핏 베이직', NULL, NULL, 1, '감성가먼트', 15, '스킨케어', 0, 0,
        '''감성가먼트'':1A,6B ''베이직'':5A ''스킨케어'':2A,7C ''슬림핏'':4A ''코튼'':3A'::tsvector, '2025-11-18 18:49:24.456195'),
       (25, '감성가먼트 스킨케어 코튼 슬림핏 로고', NULL, NULL, 1, '감성가먼트', 15, '스킨케어', 0, 0,
        '''감성가먼트'':1A,6B ''로고'':5A ''스킨케어'':2A,7C ''슬림핏'':4A ''코튼'':3A'::tsvector, '2025-11-18 18:49:24.456195'),
       (26, '감성가먼트 스킨케어 코튼 슬림핏 포켓', NULL, NULL, 1, '감성가먼트', 15, '스킨케어', 0, 0,
        '''감성가먼트'':1A,6B ''스킨케어'':2A,7C ''슬림핏'':4A ''코튼'':3A ''포켓'':5A'::tsvector, '2025-11-18 18:49:24.456195'),
       (27, '감성가먼트 스킨케어 폴리 레귤러핏 베이직', NULL, NULL, 1, '감성가먼트', 15, '스킨케어', 0, 0,
        '''감성가먼트'':1A,6B ''레귤러핏'':4A ''베이직'':5A ''스킨케어'':2A,7C ''폴리'':3A'::tsvector, '2025-11-18 18:49:24.456195');

-- 5. Sequence 업데이트 (ID 충돌 방지)
SELECT setval('brand_id_seq', (SELECT COALESCE(MAX(id), 0) FROM brand) + 1, false);
SELECT setval('category_id_seq', (SELECT COALESCE(MAX(id), 0) FROM category) + 1, false);
SELECT setval('product_id_seq', (SELECT COALESCE(MAX(id), 0) FROM product) + 1, false);

