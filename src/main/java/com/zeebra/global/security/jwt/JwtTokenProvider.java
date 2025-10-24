//@Component
//public class JwtTokenProvider {
//    private final Key key;
//    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
//        this.key = Keys.hmacShaKeyFor(secret.getBytes());
//    }
//
//    // ✅ sub에 "memberId" (PK)를 문자열로 저장
//    public String generateTokenByMemberId(Long memberId, Collection<String> roles, long expMs) {
//        Date now = new Date();
//        Date exp = new Date(now.getTime() + expMs);
//        return Jwts.builder()
//                .setSubject(String.valueOf(memberId)) // <-- PK를 subject로
//                .claim("roles", roles)
//                .setIssuedAt(now)
//                .setExpiration(exp)
//                .signWith(key, SignatureAlgorithm.HS256)
//                .compact();
//    }
//
//    public boolean validateToken(String token) {
//        try {
//            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
//            return true;
//        } catch (JwtException | IllegalArgumentException e) {
//            return false;
//        }
//    }

//    // ✅ 토큰에서 memberId(int)로 꺼내기
//    public int getMemberId(String token) {
//        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
//                .parseClaimsJws(token).getBody();
//        return Long.parseLong(claims.getSubject());
//    }
//
//    public List<String> getRoles(String token) {
//        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
//                .parseClaimsJws(token).getBody();
//        Object rolesObj = claims.get("roles");
//        if (rolesObj instanceof List<?> list) {
//            return list.stream().map(Object::toString).toList();
//        }
//        return List.of();
//    }
//}
