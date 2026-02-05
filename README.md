# URL Service

A lightweight, high-performance URL management utility and SVG QR code engine, built with **Java 25** and **Spring Boot 4**.

---

## Features

### Core Functionality

- **Smart URL Shortening**: Generates collision-resistant 8-character IDs using a custom URL-friendly alphabet.
- **High-Speed Redirection**: Designed for sub-100ms latency.
- **Lifecycle Management**: Supports auto-expiration based on time, usage count, or inactivity.
- **Password Protection**: Secure links with password gating (prefixed via `/p/`).

### Themed SVG QR Engine

- **Vector Output**: Native SVG generation for infinite scalability.
- **Customization**: Support for different pixel styles (square, rounded, dot) and custom "eye" patterns.
- **Brand Integration**: Center-align custom logos with high-error correction (Level H).

### Analytics & Security

- **Privacy-First Tracking**: Simple "Total Scans" counter without invasive tracking.
- **Dynamic Targets**: Destination URLs can be updated without changing the generated short link or QR code.

---

## Tech Stack

- **Language**: Java 25
- **Framework**: Spring Boot 4
- **Frontend**: HTMX, Hyperscript
- **Build Tool**: Maven
- **Template Engine**: Thymeleaf
- **Native Support**: GraalVM Native Image compatible

---

## Documentation

Detailed documentation for the system design and API specifications can be found in the `docs/` directory:

- [**System Design**](docs/DESIGN.md): Architecture, routing strategy, and feature deep-dive.
- [**API Specification**](docs/Api.md): Database schema and object definitions.

---
