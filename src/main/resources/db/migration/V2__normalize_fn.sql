CREATE OR REPLACE FUNCTION public.normalize_search(input_text text)
RETURNS text AS $$
BEGIN
RETURN lower(
        translate(
                regexp_replace(input_text, '\s+', '', 'g'),
                '[](){}.,!?''"', ''
        )
       );
END;
$$ LANGUAGE plpgsql IMMUTABLE;
