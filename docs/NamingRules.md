# Naming Rules and Conventions

## Cases

- **Database**: `snake_case` (plural for tables)
- **Java Class**: `PascalCase`
- **Java Variable**: `camelCase`
- **Java Method**: `camelCase`
- **Java Constant**: `UPPER_SNAKE_CASE`
- **packages**: `lowercase` (no separators)
  - the only exception is: in.hridaykh.url_service

## Html Fragments

- **File Name**: `kebab-case`
- **File name under ViewRegistry**: `camelCase`
- **Fragment names (`th:fragment` or `id`)**: `kebab-case`
- **Internal Template Fragments**: Use `th:fragment` on a `th:block`. (e.g., passing content to a layout).
- **Java/HTMX Handled Fragments**: Use `id` directly on the target element without a `th:block`. (e.g., returning `#short-url-result`).
- **Dual-Usage Fragments**: In cases where a fragment is used both by Java (via `id`) and by templates (via `th:fragment`):
  1. Decide on a case-by-case basis which identifier (th:block or html) is more prominent but use both `th:fragment` and `id`.
  2. Use HTML comments above the element to clarify that it serves both purposes.
  3. Note special exceptions below in a separate section. (doesn't exist yet due to not having a real example yet).

### Implementation Example

If you had a "URL Card" that you sometimes include in a page and sometimes refresh via Java:

```html
<div id="url-card" th:fragment="url-card" class="card">
    <span th:text="${url.code}">CODE</span>
</div>
```

## Data & Domain Logic

- **Encapsulation**: Create Getters and Setters **only** when they are explicitly needed by the application or a library.
- **Rich Domain Models**:
  - Move business-state logic (ex: `isExpired()`) directly into the Entity.
  - Use descriptive methods for state changes (ex: `markAsDeleted()`) instead of generic setters to preserve data integrity and intent.

## Markdown Files

- dont use `---` or similar.
- use the [David Anson/markdownlint](https://marketplace.visualstudio.com/items?itemName=DavidAnson.vscode-markdownlint) extension for Markdown linting and formatting in VSCode or similar in other editors.
