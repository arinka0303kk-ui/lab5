# Laboratory work 6 — variant 9

Form: http://localhost:8080/books/new
Catalogue: http://localhost:8080/books

Language links: `?lang=en`, `?lang=ru`, `?lang=kk`.
The locale is stored in the HTTP session. The initial locale is English.
`messages.properties` is the default English bundle. If a key or a language-specific bundle is missing, lookup falls back to this default bundle. System-locale fallback is disabled.

## Checked responses

| Request | Status | Content type | Handler |
| --- | --- | --- | --- |
| GET /books/999 | 404 | text/html;charset=UTF-8 | PageExceptionHandler |
| GET /api/books/999 | 404 | application/problem+json | ApiExceptionHandler |

The page advice is scoped to BookPageController. The API advice is scoped to @RestController classes, so the same exception produces different responses for pages and APIs.

The Book record is unchanged. Genre and availability belong to BookForm, as in the assignment; the POST handler saves title, author and year using the laboratory 5 model.
