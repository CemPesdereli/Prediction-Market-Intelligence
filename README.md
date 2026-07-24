# Polymarket Common Bets

Seçilen bir Polymarket kategorisinin **aylık leaderboard**'undaki ilk 20 kullanıcının,
aynı piyasada (aynı `conditionId`) ortak girdiği bahisleri gösteren Spring Boot
uygulaması. Hem hâlâ **aktif** (sonuçlanmamış) hem de son birkaç gün içinde
**kapanmış** (sonuçlanmış — kim kazanmış/kaybetmiş, kar/zarar ne olmuş) ortak
bahisleri kapsar.

## Kullanılan Polymarket API'leri (resmi, public, auth gerektirmiyor)

| Endpoint | Amaç |
|---|---|
| `GET https://data-api.polymarket.com/v1/leaderboard?category=&timePeriod=MONTH&orderBy=PNL&limit=20` | Kategoriye göre aylık top-20 kullanıcı |
| `GET https://data-api.polymarket.com/positions?user=&redeemable=false` | Bir kullanıcının aktif (açık) pozisyonları |
| `GET https://data-api.polymarket.com/positions?user=&redeemable=true` | Bir kullanıcının sonuçlanmış (redeem edilebilir) pozisyonları |

Kaynak: https://docs.polymarket.com/api-reference

## Mimari

```
PolymarketDataApiClient   -> WebClient ile yukaridaki uc endpoint'i cagirir
        |
LeaderboardSyncService    -> @Scheduled (varsayilan: 30 dakikada bir) + @PostConstruct
        |                    (LeaderboardSyncScheduler uzerinden) ile her kategori
        |                    icin top-20 + aktif + kapanmis pozisyonlari cekip
        |                    H2 veritabanina yazar (onceki kaydin yerine)
        v
   H2 Database  (leaderboard_entry, position_snapshot, closed_position_snapshot)
        |
CommonBetsService         -> position_snapshot kayitlarini conditionId'ye gore
        |                    gruplar, >= min-common-holders (varsayilan 2) farkli
        |                    kullanicinin oldugu marketleri "ortak bahis" sayar
        |
ClosedCommonBetsService   -> ayni gruplamayi closed_position_snapshot uzerinde yapar;
        |                    piyasanin sonucunu (resolvedOutcome) ve her kullanicinin
        |                    kazanip kazanmadigini (curPrice >= 0.5) ve kar/zararini
        |                    (cashPnl) hesaplar
        v
CommonBetsApiController   -> GET /api/common-bets?category=POLITICS         (JSON)
                              GET /api/closed-common-bets?category=POLITICS (JSON)
CommonBetsViewController  -> GET /common-bets?category=POLITICS      (Thymeleaf HTML,
                              aktif + kapanmis bahisler ayni sayfada)
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
- http://localhost:8080/api/common-bets?category=CRYPTO  (JSON, aktif)
- http://localhost:8080/api/closed-common-bets?category=CRYPTO  (JSON, kapanmış)
- `POST http://localhost:8080/api/sync?category=POLITICS` → 30 dakikayı beklemeden
  manuel yeniden senkronizasyon (demo/test için)
- http://localhost:8080/h2-console  → H2 konsolu (JDBC URL: `jdbc:h2:file:./data/polybets`)

## Yapılandırma (`application.yml`)

| Alan | Açıklama |
|---|---|
| `polymarket.top-n` | Leaderboard'dan kaç kişi alınacak (varsayılan 20) |
| `polymarket.time-period` | `DAY` / `WEEK` / `MONTH` / `ALL` (varsayılan MONTH) |
| `polymarket.min-common-holders` | "Ortak bahis" sayılması için gereken min. farklı kullanıcı sayısı (varsayılan 2) |
| `polymarket.closed-window-days` | Kapanmış bahisler için geriye dönük gün penceresi (varsayılan 3) |
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
- **Kapanmış bahisler için veri sınırı**: Polymarket'in `/positions?redeemable=true`
  endpoint'i kazanan pozisyonları genelde hızlıca redeem edilip listeden düştüğü için
  esas olarak henüz redeem edilmemiş (ağırlıklı kaybeden) pozisyonları döndürür. Bir
  piyasada aynı anda hem Yes hem No tutan kullanıcılar varsa iki tarafın da sonucu
  görünür (`resolvedOutcome`, hangi tarafın kazandığını her holder'ın kendi
  `curPrice`/`oppositeOutcome` alanından bağımsız olarak çıkarır), ama tek taraflı
  kazanan pozisyonlar zaten redeem edilmişse görünmeyebilir. Bu, kendi indeksleyicimiz
  olmadan (sadece resmi API'ye dayanarak) kabul ettiğimiz bilinçli bir sınırlama.
