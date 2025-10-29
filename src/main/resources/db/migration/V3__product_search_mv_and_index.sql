DROP MATERIALIZED VIEW IF EXISTS product_search_mv;

CREATE MATERIALIZED VIEW product_search_mv AS
SELECT
    p.id,
    p.name,
    p.description,
    p.model_number,
    p.thumbnail,
    c.name AS category_name,
    b.name AS brand_name,
    public.normalize_search(
            p.name || ' ' ||
            coalesce(p.description, '') || ' ' ||
            coalesce(p.model_number, '') || ' ' ||
            c.name || ' ' || b.name
    ) AS search_text_norm
FROM products p
         JOIN category c ON p.category_id = c.id
         JOIN brand b ON p.brand_id = b.id;

CREATE INDEX IF NOT EXISTS idx_product_search_mv_trgm
    ON product_search_mv USING gin (search_text_norm gin_trgm_ops);