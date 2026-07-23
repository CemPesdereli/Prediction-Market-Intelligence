# Prediction Market Intelligence Platform (Polymarket Consensus Engine)

Bu dosya, bu proje üzerinde çalışan her Claude Code oturumunun başlangıç bağlamıdır.
Yeni bir oturuma başlarken bu dosyayı oku, güncel durumu ve kararları buradan al.

## Proje Ne Yapıyor

Polymarket'te seçilen bir kategorinin (örn. WEATHER) **aylık leaderboard**'undaki ilk 20
trader'ın, aynı piyasada (`conditionId`) hâlâ **aktif** (sonuçlanmamış) pozisyon tuttuğu
ortak marketleri tespit edip bir "consensus" raporu üretiyor. Amaç: "en başarılı 20 kişi
şu an ortaklaşa nereye bahis oynuyor" sorusuna cevap vermek.

CV amacıyla, kurumsal seviyede (temiz mimari + test + CI/CD) bir backend projesi olarak
tasarlanıyor. Bu bir "Polymarket parser" değil, ileride başka platformlar (Kalshi,
Manifold, PredictIt) da eklenebilecek şekilde tasarlanan bir **Prediction Market
Intelligence Platform**'un ilk adımı.

## Kesinleşmiş Mimari Kararlar

- **"Hafif" (pragmatic) Hexagonal / Ports & Adapters** — tam DDD ciddiyetinde değil,
  tek Maven modülü içinde katman bazlı paket ayrımı:
  - `domain` — düz POJO'lar (Trader, Position, Market, ConsensusResult). Spring/JPA/
    Jackson annotation'ı **yok**.
  - `application` — use-case servisleri (LeaderboardService, ConsensusService, ReportService).
    Sadece port interface'lerine bağımlı, implementasyon detayı bilmez.
  - `adapter` — dış dünyaya bakan her şey: Polymarket WebClient adapter'ı, JPA repository
    adapter'ı, REST controller'lar, (ileride) Telegram adapter'ı.
  - Port örnekleri: `MarketDataPort`, `LeaderboardPort`, `ConsensusRepositoryPort`.
- **ArchUnit** ile bu katman sınırları otomatik test edilecek (örn. "domain paketi
  Spring'e bağımlı olamaz", "adapter'dan application'a bağımlılık olamaz").
- Neden hexagonal: Consensus mantığı Polymarket'e bağımlı olmasın; ileride başka platform
  eklemek yeni bir adapter yazmaktan ibaret olsun. "API kırılırsa diye" savunmacı bir
  gerekçe değil, test edilebilirlik ve genişletilebilirlik gerekçesi.
- **Veritabanı**: PostgreSQL + Flyway (migration yönetimi baştan var, H2'ye geri
  dönülmedi — CV'de daha iyi durduğu için bilinçli tercih).
- **Test stratejisi**: JUnit 5 + Mockito (unit) + WireMock (Polymarket API mock'lu
  entegrasyon testleri) + **Testcontainers** (gerçek Postgres container'ına karşı
  repository/entegrasyon testleri).
- **Docker Compose**: yerel Postgres + (ileride) uygulamanın kendisi için.
- **CI**: GitHub Actions (Faz 2'de eklenecek).

## Doğrulanmış Polymarket API Detayları (ÖNEMLİ — varsayım değil, test edildi)

Bazı kaynaklar "Polymarket resmi API'si aktif pozisyonları vermiyor" diyor —
**bu yanlış**, aşağıdaki iki endpoint gerçekten çalışıyor ve auth gerektirmiyor:

```
GET https://data-api.polymarket.com/v1/leaderboard
    ?category=WEATHER          (POLITICS, SPORTS, ESPORTS, CRYPTO, CULTURE,
                                 MENTIONS, WEATHER, ECONOMICS, TECH, FINANCE, OVERALL)
    &timePeriod=MONTH           (DAY, WEEK, MONTH, ALL)
    &orderBy=PNL
    &limit=20
→ [{ rank, proxyWallet, userName, vol, pnl, profileImage, xUsername, verifiedBadge }]

GET https://data-api.polymarket.com/positions
    ?user=0x...
    &redeemable=false           (= henüz sonuçlanmamış / aktif pozisyon)
    &limit=500
→ [{ proxyWallet, asset, conditionId, size, avgPrice, currentValue, cashPnl,
     curPrice, redeemable, title, slug, eventSlug, outcome, outcomeIndex, endDate }]
```

"Ortak bahis" tanımı: aynı `conditionId`'ye sahip olmak yeterli sayılıyor; Yes/No
farklı taraflarda olmaları da ortaklık sayılır ama `outcome` alanıyla ayrıca gösterilir.
Eşik: `min-common-holders` (varsayılan 2) farklı cüzdan.

## Önceki Sürüm (referans / yeniden kullanılacak kod)

`polymarket-common-bets` adında, düz katmanlı (hexagonal olmayan), H2 + Thymeleaf
kullanan çalışan bir MVP zaten var: `PolymarketDataApiClient` (WebClient ile yukarıdaki
iki endpoint'i çağırıyor), `LeaderboardSyncService` (@Scheduled ile periyodik senkron),
`CommonBetsService` (conditionId bazlı gruplama). Bu projeyi **sıfırdan yazmıyoruz**,
API client ve iş mantığını yeni paket yapısına (`domain`/`application`/`adapter`)
taşıyarak (refactor ederek) evrimleştiriyoruz.

## Yol Haritası (Faz Planı)

- **Faz 1 (şu an burdayız)**: Hexagonal iskelet, domain modeli, portlar, Polymarket
  adapter'ı (önceki koddan taşınmış), Consensus Engine, PostgreSQL + Flyway,
  JUnit5 + Mockito + WireMock + Testcontainers testleri, Docker Compose.
- **Faz 2**: GitHub Actions CI, ROI'ye göre ağırlıklı consensus (başarılı trader'ların
  oyu daha ağır basar), historical tracking (consensus'un zaman içindeki değişimi).
- **Faz 3**: Telegram bot (günlük consensus bildirimi), basit dashboard.
- **Faz 4 (nice-to-have)**: Micrometer + Prometheus + Grafana, SonarQube, ikinci bir
  platform adapter'ı (Kalshi/Manifold) ile mimarinin gerçekten platform-agnostik
  olduğunu kanıtlamak.

## Konvansiyonlar

- Kod: İngilizce (sınıf/metot/değişken adları), yorumlar ve commit mesajları Türkçe
  olabilir (önceki projede bu şekildeydi, tutarlılık için devam).
- Paket kökü: `com.example.polybets` (önceki projeden devam, değişmedi).
- Java 21, Spring Boot 3.3+/3.5.
- README.md'yi güncel tutmayı unutma — her faz sonunda "şu ana kadar ne yapıldı"
  bölümü CV'de referans verilecek şekilde net olmalı.

## Şu Anki Durum / Sıradaki Adım

Faz 1'e yeni başlıyoruz. Sıradaki somut adım: proje iskeletini (paket yapısı, pom.xml
güncellemesi — Postgres/Flyway/Testcontainers/ArchUnit/WireMock bağımlılıkları,
docker-compose.yml) kurmak.
