# Polymarket Common Bets

Seçilen bir Polymarket kategorisinin **aylık leaderboard**'undaki ilk 20 kullanıcının,
aynı piyasada (aynı `conditionId`) aktif olarak (henüz sonuçlanmamış) bahis yaptığı
**ortak marketleri** gösteren Spring Boot uygulaması.

## Kullanılan Polymarket API'leri (resmi, public, auth gerektirmiyor)

| Endpoint | Amaç |
|---|---|
| `GET https://data-api.polymarket.com/v1/leaderboard?category=&timePeriod=MONTH&orderBy=PNL&limit=20` | Kategoriye göre aylık top-20 kullanıcı |
| `GET https://data-api.polymarket.com/positions?user=&redeemable=false` | Bir kullanıcının aktif (açık) pozisyonları |

Kaynak: https://docs.polymarket.com/api-reference

## Mimari

```
PolymarketDataApiClient   -> WebClient ile yukaridaki iki endpoint'i cagirir
        |
LeaderboardSyncService    -> @Scheduled (varsayilan: 30 dakikada bir) + @PostConstruct
        |                    ile her kategori icin top-20 + pozisyonlari cekip
        |                    H2 veritabanina yazar (onceki kaydin yerine)
        v
   H2 Database  (leaderboard_entry, position_snapshot tablolari)
        |
CommonBetsService         -> position_snapshot kayitlarini conditionId'ye gore
        |                    gruplar, >= min-common-holders (varsayilan 2) farkli
        |                    kullanicinin oldugu marketleri "ortak bahis" sayar
        v
CommonBetsApiController   -> GET /api/common-bets?category=POLITICS  (JSON)
CommonBetsViewController  -> GET /common-bets?category=POLITICS      (Thymeleaf HTML)
```

## Çalıştırma

```bash
mvn spring-boot:run
```

Uygulama açılınca `@PostConstruct` ile tüm kategoriler için ilk senkronizasyon otomatik
tetiklenir (Polymarket'e ~11 kategori x ~21 istek gider, birkaç saniye sürebilir).

Sonra tarayıcıdan:

- http://localhost:8080/  → varsayılan kategori (POLITICS) ile ana sayfa
- http://localhost:8080/common-bets?category=SPORTS
- http://localhost:8080/api/common-bets?category=CRYPTO  (JSON)
- `POST http://localhost:8080/api/sync?category=POLITICS` → 30 dakikayı beklemeden
  manuel yeniden senkronizasyon (demo/test için)
- http://localhost:8080/h2-console  → H2 konsolu (JDBC URL: `jdbc:h2:file:./data/polybets`)

## Yapılandırma (`application.yml`)

| Alan | Açıklama |
|---|---|
| `polymarket.top-n` | Leaderboard'dan kaç kişi alınacak (varsayılan 20) |
| `polymarket.time-period` | `DAY` / `WEEK` / `MONTH` / `ALL` (varsayılan MONTH) |
| `polymarket.min-common-holders` | "Ortak bahis" sayılması için gereken min. farklı kullanıcı sayısı (varsayılan 2) |
| `polymarket.sync.cron` | Senkronizasyon periyodu (varsayılan 30 dk) |

## Notlar / Bilinçli Tasarım Kararları

- **"Ortak bahis" tanımı**: Aynı `conditionId`'ye (yani aynı markete) sahip olmak yeterli
  sayıldı; kullanıcıların Yes/No gibi farklı taraflarda olması da "ortak bahis" kabul
  edilir, ama hangi tarafta oldukları holder listesinde `outcome` alanıyla gösterilir.
  İstersen bunu "sadece aynı tarafta olanlar" şeklinde daraltabiliriz.
- Pozisyonlar kategoriye göre filtrelenmiyor — bir kullanıcının o kategorideki
  leaderboard'da olması yeterli, gösterilen ortak bahisler onun **tüm aktif
  pozisyonları** arasından çıkarılıyor (yani bir siyaset lideri aslında spor
  marketinde ortaklaşabilir). Bunu istemiyorsan pozisyonları event/tag bazında
  kategoriye göre de filtreleyebiliriz (Gamma API'den event->tag eşlemesi gerekir).
- Rate limit'e karşı senkronizasyon sıralı (sequential) yapılıyor; performans sorun
  olursa `WebClient` çağrılarını paralel/async hale getirebiliriz.
