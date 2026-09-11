# HONK Limousine Service — Project Status

> **Purpose of this file**: a complete handoff summary for any other AI agent or developer picking up this project, so they don't need to re-read the entire chat history. Last updated: 2026-08-24.

---

## 1. What this project is

A Chicago limo/car service booking website (inspired by eminentlimo.com, royalcarriagelimo.com, americancoachlimousine.com). Customers fill out a ride booking form; the site automatically notifies the business owner (and customer) via WhatsApp/email, persists the booking to a database, calculates live fare estimates via Mapbox, and gives customers a way to track/cancel their booking without logging in. There's also a full admin panel for managing bookings, fleet, and pricing.

**Brand name:** HONK Limousine Service (originally prototyped as "Chicago Elite Limo" — Java package name is still `com.chicagoelitelimo`, this was never renamed and is a known cosmetic inconsistency, not a bug).

**Business model reference document**: the user provided a detailed blueprint PDF early on (phases, DB schema, pricing logic, features list) which has driven most of the build decisions. Payment is intentionally NOT integrated (Phase 2 feature) — the owner contacts the customer personally to finalize price/payment via a payment link (Stripe/Square/PayPal) after booking.

---

## 2. Tech stack

| Layer | Choice |
|---|---|
| Frontend | Thymeleaf + Bootstrap 5 + vanilla JS, server-rendered, all inside the Spring Boot app (single deployable JAR) |
| Backend | Spring Boot 3.3.4, Java 17, Maven |
| Database | H2 file-based (`./data/honklimo`) for dev — designed to swap to AWS RDS PostgreSQL for production (just change `spring.datasource.*`, no code changes needed since it's plain JPA/Hibernate) |
| Auth | Spring Security, single in-memory admin user (BCrypt-hashed), session-based form login |
| Messaging | Twilio WhatsApp Business API (Content Templates) |
| Email | Spring Mail / SMTP (Gmail App Password compatible) |
| Maps | Mapbox (Geocoding API for autocomplete, Directions API for distance/fare) |
| Analytics | Google Analytics (GA4), optional/conditional |

---

## 3. Project structure

```
Chicago Travels/
├── pom.xml
├── src/main/java/com/chicagoelitelimo/
│   ├── ChicagoEliteLimoApplication.java
│   ├── config/
│   │   ├── AdminProperties.java        (admin.username / admin.password)
│   │   ├── DataSeeder.java             (seeds Vehicle + PricingRate tables on first run only)
│   │   ├── GlobalModelAttributes.java  (@ControllerAdvice — injects baseUrl + gaTrackingId into all page models)
│   │   ├── MailProperties.java         (mail.owner-email / mail.from-address)
│   │   ├── MapboxProperties.java       (mapbox.access-token)
│   │   ├── SecurityConfig.java         (Spring Security filter chain, in-memory admin user)
│   │   └── TwilioProperties.java       (twilio.* — account sid, auth token, sender, content SIDs)
│   ├── controller/
│   │   ├── AdminController.java        (/admin/** — login, bookings CRUD+CSV, fleet CRUD, pricing CRUD)
│   │   ├── ApiExceptionHandler.java    (validation error → clean JSON response)
│   │   ├── BookingApiController.java   (POST /api/bookings — creates booking, triggers WhatsApp + email)
│   │   ├── BookingTrackController.java (POST /api/bookings/track, /api/bookings/track/cancel — guest lookup)
│   │   ├── EstimateController.java     (POST /api/estimate — Mapbox distance + PricingService fare calc)
│   │   └── PageController.java         (all public page routes + dynamic /sitemap.xml)
│   ├── dto/
│   │   ├── BookingRequest.java, EstimateRequest.java, TrackRequest.java
│   ├── entity/
│   │   ├── Booking.java, BookingStatus.java (enum: PENDING/CONFIRMED/COMPLETED/CANCELLED)
│   │   ├── Customer.java, PricingRate.java, Vehicle.java
│   ├── repository/  (Spring Data JPA repos for each entity)
│   └── service/
│       ├── BookingService.java   (create booking, generate reference, tracking lookup, cancel, admin search)
│       ├── EmailService.java     (SMTP booking confirmation/alert, gracefully no-ops if unconfigured)
│       ├── FareEstimate.java, RouteResult.java (small result records)
│       ├── MapboxService.java    (calls Mapbox Directions API)
│       ├── PricingService.java   (fare calculation logic, reads rates from DB)
│       ├── RateLimiterService.java (simple in-memory limiter for guest tracking endpoints)
│       └── WhatsAppService.java  (Twilio WhatsApp send, gracefully no-ops if unconfigured)
├── src/main/resources/
│   ├── application.yml   (all config, env-var driven with dev-safe defaults)
│   ├── templates/        (index, booking, fleet, services, pricing, faq, about, contact, track + admin/*)
│   └── static/
│       ├── css/style.css (black/gold luxury theme)
│       ├── js/main.js    (all client-side logic — forms, autocomplete, fare estimate, toasts, tracking)
│       └── robots.txt
└── PROJECT-GUIDE.html   (older static HTML doc — superseded by this file for planning purposes, still useful for env var reference)
```

Root also still contains the **original static HTML prototype files** (index.html, booking.html, css/, js/ at the workspace root) from before the Spring Boot conversion — these are dead/unused now that everything lives under `src/main/resources`. Safe to delete, never cleaned up.

---

## 4. Features built (chronological, all tested live during development)

### Frontend (all 9 public pages + 3 admin pages)
- Home (`/`) — hero with quick-booking widget (One Way + Hourly tabs, both now fully functional with autocomplete), services/fleet preview, testimonials, CTA.
- Booking (`/booking`) — full ride booking form with live Mapbox address autocomplete, live fare estimate card with "View Details" breakdown toggle, WhatsApp consent checkbox, toast notifications on submit.
- Fleet (`/fleet`) — dynamically rendered from the `vehicles` DB table.
- Pricing (`/pricing`) — dynamically rendered from the `pricing_rates` DB table.
- Services, About, Contact, FAQ — static content pages.
- Track (`/track`) — guest booking status lookup + cancel (reference + phone two-factor verification, no login required).
- Admin (`/admin/login`, `/admin/bookings`, `/admin/fleet`, `/admin/pricing`) — full CRUD dashboards.

### Backend modules (in build order)
1. **Static site → Spring Boot + Thymeleaf conversion**
2. **WhatsApp integration** (Twilio) — sends booking alert to owner + confirmation to customer via Content Templates. Bug found & fixed: `Message.creator()`'s 3rd positional arg is `body`, not `contentSid` — must use `.setContentSid()` builder method instead.
3. **H2 database + JPA** — `Customer`/`Booking` entities, booking reference generation (`HONK-yyyyMMdd-000001` format).
4. **Pricing + FAQ pages** (static content matching blueprint).
5. **Guest booking tracking module** — `/track` page, reference+phone lookup, 24-hour cancellation policy window, rate limiting. Bug found & fixed: `findForTracking()` needed `@Transactional(readOnly = true)` or lazy-loaded `Customer` association threw `LazyInitializationException` (since `open-in-view: false`).
6. **Mapbox integration** — Geocoding API (client-side autocomplete, public token) + Directions API (server-side, separate token) + `PricingService` (flat airport rate / per-mile / call-for-pricing logic based on distance + airport keyword detection).
7. **Admin panel** — Spring Security login, bookings dashboard (search/status update/delete/CSV export).
8. **Fleet/Pricing content management** — converted from static to DB-backed (`Vehicle`, `PricingRate` entities), admin CRUD UI. Bug found & fixed: a `<form>` can't validly wrap multiple `<td>` elements in a table row — fixed using the `form="id"` HTML attribute technique so inputs associate with a form declared elsewhere in the DOM.
9. **Email notifications** (SMTP) — booking confirmation to customer + alert to owner, same graceful-degradation pattern as WhatsApp.
10. **SEO basics** — meta descriptions, canonical URLs, Open Graph/Twitter Card tags (all built from configurable `app.base-url`), conditional Google Analytics snippet, `robots.txt`, dynamic `/sitemap.xml`.
11. **Professional UI polish** — replaced plain Bootstrap alert divs with a custom toast notification system (color-coded, auto-dismiss, dark themed), redesigned fare estimate into a card with a collapsible "View Details" breakdown panel.
12. **Hero quick-booking widget bug fixes** — the "Hourly" tab had zero JS wired to it (completely non-functional); both tabs lacked address autocomplete. Both fixed and verified.

---

## 5. Database schema (current)

```
customers (id, name, phone [unique], email)
bookings  (id, booking_reference [unique], customer_id [FK], service_type, vehicle_type,
           pickup_location, dropoff_location, pickup_date, pickup_time, return_date, return_time,
           passengers, luggage, flight_number, special_requests, status [enum], estimated_fare,
           created_at)
vehicles  (id, vehicle_key [unique], name, capacity, luggage, price_per_hour, icon_class,
           description, display_order)
pricing_rates (id, vehicle_key [unique], label, flat_airport_rate, per_mile_rate, display_order)
```

`vehicle_key` values used throughout (booking form, fleet, pricing): `sedan`, `suv`, `sprinter`, `stretch`, `partybus`, `motorcoach`. Note: `pricing_rates` only has rows for `sedan`, `suv`, `stretch`, `sprinter` — `partybus`/`motorcoach` intentionally have no pricing row, so they always fall back to "Call for Pricing" (no fabricated numbers).

---

## 6. Environment variables (all optional — app degrades gracefully if unset, with a logged warning)

| Variable | Used for | Dev default if unset |
|---|---|---|
| `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` | Twilio auth | WhatsApp sending disabled |
| `TWILIO_WHATSAPP_FROM` | Sender number | `whatsapp:+14155238886` (sandbox) |
| `OWNER_WHATSAPP_NUMBER` | Owner's WhatsApp | none — owner alert skipped |
| `TWILIO_CONTENT_SID_OWNER`, `TWILIO_CONTENT_SID_CUSTOMER` | Approved Content Template SIDs | none — skipped |
| `MAPBOX_ACCESS_TOKEN` | Server-side Directions API | none — `/api/estimate` returns 503 |
| (client-side Mapbox public token) | Autocomplete | Hardcoded in `main.js` `SITE_CONFIG.mapboxPublicToken` — **not an env var**, embedded directly since it's meant to be public |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Admin login | Falls back to `admin`/`admin123` — **must** be set before any real deployment |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP | Email disabled |
| `OWNER_EMAIL`, `MAIL_FROM_ADDRESS` | Email recipients | `HonkLimoservice@gmail.com` default from |
| `APP_BASE_URL` | Canonical/OG URLs, sitemap | `http://localhost:8080` |
| `GOOGLE_ANALYTICS_ID` | GA4 tracking | Unset — no script rendered at all |

---

## 7. How to run locally

```powershell
cd "Chicago Travels"
mvn clean compile
$env:MAPBOX_ACCESS_TOKEN="pk...."   # only if testing fare estimate
mvn spring-boot:run
```
App runs on `http://localhost:8080`. H2 console at `/h2-console` (JDBC URL `jdbc:h2:file:./data/honklimo`, user `sa`, blank password).

**Common gotcha**: `mvn spring-boot:run` fails with "port 8080 already in use" if a previous run wasn't killed. Fix:
```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object -ExpandProperty OwningProcess | ForEach-Object { Stop-Process -Id $_ -Force }
```

---

## 8. Known issues / limitations (honest list)

1. **Mapbox classic Geocoding API has no real POI/landmark search** — typing a landmark name (e.g., "Willis Tower") won't resolve to the actual building, only to streets with matching words. Real addresses and airport names work fine. Fixing this properly would require migrating to Mapbox's newer Search Box API — not done, flagged as a possible future improvement.
2. **This dev machine sits behind a corporate Zscaler proxy** that was intermittently blocking/resetting outbound HTTPS to Twilio's API during earlier testing (unrelated to Mapbox, which works fine). If WhatsApp sending mysteriously fails with SSL/connection errors on this specific machine/network, it's very likely this proxy, not the code. Confirmed via direct `curl` outside this network that credentials/code are correct.
3. **`Message.creator()` Twilio SDK gotcha**: don't pass `contentSid` as the 3rd positional constructor arg — it's actually `body`. Use `.setContentSid()`.
4. **Lazy-loading + `open-in-view: false`**: any new repository method that returns an entity with a lazy `@ManyToOne`/`@OneToMany` association accessed later (e.g., in a Thymeleaf template, or after the method returns) needs `@Transactional` on the service method, or a `JOIN FETCH` in the query. Bit us twice already (`findForTracking`, and would bite again in new code without care).
5. **HTML tables + forms**: don't wrap a `<form>` around multiple `<td>` elements in the same `<tr>` — browsers foster-parent it out of the table, breaking the association. Use the `form="formId"` attribute on inputs/buttons instead, with an empty `<form id="formId">` declared anywhere in the DOM.
6. **Admin credentials default to `admin`/`admin123`** if env vars aren't set — fine for local dev, must never ship to production unset.
7. **Root-level duplicate static files** (from the pre-Spring-Boot prototype) are still sitting in the workspace root, unused. Not cleaned up.
8. **No automated tests** (unit/integration) exist yet — all verification so far has been manual (curl/PowerShell/Playwright browser testing during development).
9. **Security note for future agents**: earlier in this project, real Twilio credentials were accidentally pasted in plaintext into chat by the user on two occasions. They were told to rotate the token each time. If you see any real-looking API keys/tokens in old conversation history, treat them as already rotated/invalid — never reuse them.

---

## 9. Not yet built (from the original blueprint / roadmap)

1. **Migration to AWS RDS PostgreSQL** for production (currently H2 file-based for dev only).
2. **Production deployment** (hosting choice — Lightsail/EC2 discussed, domain, SSL).
3. **Multi-user admin accounts / roles** (currently a single hardcoded in-memory admin user — fine for a single owner-operator business, would need real user management for multiple staff logins).
4. **Payment integration** — intentionally deferred (Phase 2), owner sends a manual payment link (Stripe/Square/PayPal) after confirming price by phone/WhatsApp.
5. **Mapbox Search Box API migration** for proper landmark search (optional polish).
6. Full systematic bug-fixing pass across every remaining page/flow — was in progress when this file was requested (home page hero widget bugs already found and fixed; services/about/contact/FAQ/admin pages not yet specifically audited).

---

## 10. Suggested next steps (in priority order)

1. Finish the systematic UI/bug-fixing pass across remaining pages.
2. Set real `ADMIN_USERNAME`/`ADMIN_PASSWORD` and rotate/confirm all other credentials before any deployment.
3. Migrate to AWS RDS Postgres.
4. Deploy (Lightsail/EC2 + domain + SSL).
5. Consider Mapbox Search Box API if landmark search quality matters for the business.
