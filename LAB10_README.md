# Laboratory 10 — Diara Kegenbay — IT-2409 — variant 9

The original Desktop project was preserved. This directory is a complete working copy on branch `lab10`.

## Run

Requires Java 25, PostgreSQL 18 and Maven. Run `Start-Lab10.ps1` in PowerShell. Open http://localhost:8080/books.

Demo accounts: `user / user123`, `admin / admin123`. These are deliberately fake laboratory passwords.

The lab profile uses a separate local PostgreSQL database `springlab` at `127.0.0.1:55432`, role `lab10`. Its local-only trust authentication is for the isolated teaching cluster. Existing databases on port 5432 are untouched.

For an already running database use `java -jar spring-lab-01/target/spring-lab-01-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev,lab10` from the spring-lab-01 directory.

## Verify

`mvn test` in `spring-lab-01`: 20 tests, all passed. A test-only H2 setting allows the existing `year` column name. Lab08 write tests now use an ADMIN test principal; migration-history assertions now expect seven versions.

The browser and HTTP experiments are stored separately in the supplied evidence directory and report. For web POST requests obtain `_csrf` from a page in the same cookie session. The handout's POST /login without a token is rejected (403), so obtain a login-page token first. Supply the existing BookForm's mandatory author and use genre SCIENCE rather than the handout's nonexistent NOVEL.

## Changes

Spring Security, database UserDetailsService, BCrypt(10), USER/ADMIN URL rules, @PreAuthorize on all catalogue writes, a user-list admin page, conditional controls and POST logout. CSRF remains enabled for the browser chain.

V1-V5 were already present and were not changed. V6 creates the catalogue schema missing from the old Flyway history; V7 adds users and roles. The instruction's V5__security.sql is therefore represented by V7__security.sql.

Variant 9: first SecurityFilterChain matches /api/** and uses HTTP Basic with STATELESS and no cookie SecurityContext. API writes require ADMIN. This exercise assumes non-browser clients explicitly send Authorization on every call. Stateless HTTP Basic alone does not eliminate browser CSRF risks: do not use cached browser Basic authentication for this API; browser use requires CSRF or an appropriate non-ambient credential design. Use HTTPS outside localhost.

The package preserves old .git, source, tests and local data. Generated target files and local databases should not be newly committed. SQL backup contains the isolated lab database and demo hashes only.
