# SQLNext 0.2.0 release checklist

This is a desktop beta, not the SRS production release acceptance.

- [x] Scoped HTTP diagnostics exercised against a local HTTP fixture, including no redirect following.
- [x] Queue pause/resume, failure, persistence and interrupted-task recovery tested.
- [x] Local SQL/file/script fixtures tested without external targets.
- [x] Real sqlmap 1.10.10 adapter exercised against an isolated in-memory vulnerable endpoint.
- [x] SQLite read-only metadata and redacted sample tested.
- [x] Vault encryption, wrong-password and tamper rejection tested.
- [x] JSON/HTML/CSV/SARIF/PDF export tested; overwrite and HTML/formula injection protection checked.
- [x] JavaFX view construction and UI-driven task execution tested headlessly on Linux.
- [x] Existing version-1 workspace migration tested.
- [x] Linux native app-image created; bundled-runtime self-test passed.
- [ ] Windows/macOS workflow results reviewed and native UI manually tested.
- [ ] External AI verified with a live configured HTTPS service (transport/approval/redaction tests exist).
- [ ] HTTP/SOCKS5/Tor routing verified against configured real proxy infrastructure.
- [ ] Team authentication, role enforcement and OS keychain integration.
- [ ] Signed, isolated plugin execution and signed updater/rollback.
- [ ] Production evidence encryption, retention/deletion policies and authenticated audit integrity.
- [ ] Remote database drivers and server-version compatibility tests.
- [ ] Signed installers, full transitive dependency lock and complete production release security review.
- [ ] Original v5.5 executable/source behavior verified.

Default test runs skip the optional real sqlmap integration unless explicitly configured. Headless UI is enabled only by the headless Maven profile. Distinguish those skipped checks from the additional configured integration run.
