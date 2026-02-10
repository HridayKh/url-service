# URL Service

A lightweight, high-performance URL management utility and SVG QR engine. Hosted at `urls.HridayKh.in`, it serves as a professional portfolio piece demonstrating clean architecture, custom SVG manipulation, and efficient routing without the bloat of traditional SaaS platforms.

---

## 1. Features

### Core Functionality

- **URL Shortening**: Generate a unique short URL for a given long URL and ensure that the same long URL always generates the same short URL unless different expiry type required.
- **URL Redirection**: Redirect users from the short URL to the original long URL with sub-100ms latency (via Redis caching).
- **Expiration**: Auto-purge links that receive zero scans for 365 consecutive days as a default expiry type.

### Themed SVG QR Engine

- **Custom Styling**: Toggle between square, rounded, and "dot" pixel styles.
- **Eye Styling**: Independent customization of the three corner "finder" patterns.
- **Brand Integration**: Ability to center-align custom logos with high-error correction (Level H) to maintain scan-ability.
- **Vector Output**: Native SVG generation for infinite scalability (perfect for print/resumes).

### Security & Lifecycle

- **Password Gating**: Optional password protection for any link (accessible via `/p/` prefix) for registered users.
- **Dynamic Targets**: Destination URLs can be updated at any time without changing the QR/Short Link.
- **Account-Free Generation**: Guests can generate links instantly without signing up.

### Analytics

- **Simplified Tracking**: No invasive tracking or complex dashboards.
- **Total Scan Count**: Each link displays a simple, transparent "Total Scans" counter, updated asynchronously.

---

## 2. System Architecture & Routing Logic

The service uses a clear, prefix-based routing system to handle different link types efficiently.

### URL Structure

- `/[id]` (**Standard**): Randomly generated 8-character collision-resistant IDs (e.g., `urls.HridayKh.in/xJ92_zP1`). Uses a custom URL-friendly alphabet to avoid ambiguous characters (e.g., `l` vs `1`, `O` vs `0`).
- `/[id]` (**Protected**): Secure links that require a password before the redirect triggers.
- `/[vanity]` (**Custom**): User-defined slugs for personal branding (e.g., `urls.HridayKh.in/github`) (Registered users only).

### Redirection Strategy

- **High-Speed Path**: Uses a Key-Value cache (Redis) for the ID → Destination mapping.
- **Total Count Sync**: Increments a global "Total Scans" counter asynchronously to ensure zero latency for the user.

---

## 3. User Access Levels

| Feature | Guest (No Signup) | Registered (Free) |
| :--- | :--- | :--- |
| **Link Types** | `/u/` (Random) | `/u/`, `/p/`, and Custom Vanity |
| **Password Protection** | No | Yes |
| **Total Scan Counter** | Yes | Yes |
| **Management** | Session-based | Full Dashboard (Edit/Delete) |
| **Expiry Policy** | 1 Year Inactivity | Permanent |

---

## 4. UX & UI Design

### Visual Identity

- **Host Domain**: `urls.HridayKh.in`
- **Theme**: Clean, developer-centric "Modern Minimalist."
- **Focus**: High-contrast accessibility, fast load times, and mobile-first responsiveness.

### Redirection Page (Password Gate)

- A simple, centered input box.
- Minimalist "Unlock" button.
- No ads or external branding; focus entirely on the secure transition.

---

## 5. Technical Success Metrics

- **Sub-100ms Redirects**: Optimized routing via Redis.
- **Zero-Conflict IDs**: Custom alphabet avoids ambiguity.
- **SVG Performance**: Direct DOM manipulation for real-time QR previews without server round-trips.

## 6. Offered domains

- urls.hridaykh.in
- .pp.ua
- .eu.org
- .of.to
- .work.gd
- .publicvm.com
- .run.place
- .2bd.net
- .linkpc.net
