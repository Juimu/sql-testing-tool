# SQLNext — বাংলায় ব্যবহার নির্দেশিকা

SQLNext একটি JavaFX ডেস্কটপ অ্যাপ: অনুমোদিত নিরাপত্তা পরীক্ষা চালানো, ফলাফল গুছিয়ে রাখা এবং রিপোর্ট তৈরি করার জন্য। তথ্য স্থানীয় SQLite ডাটাবেসে থাকে।

**বর্তমান সংস্করণ: ০.২.০ Desktop Beta।** আগের foundation সংস্করণে বাস্তব assessment queue, HTTP diagnostics, optional sqlmap integration, database inspection, encrypted credential vault এবং আরও রিপোর্ট যোগ হয়েছে। এটি এখনো মূল SRS-এর সব enterprise সুবিধাসহ production release নয়। বাকি কাজ নিচে পরিষ্কারভাবে দেওয়া আছে।

## ১. ডাউনলোড

👉 **[সোর্স কোডের ZIP ডাউনলোড](https://github.com/Juimu/sql-testing-tool/archive/refs/heads/main.zip)**

অথবা [রিপোজিটরি](https://github.com/Juimu/sql-testing-tool) খুলে **Code → Download ZIP → Extract All** করুন। `pom.xml` থাকা ফোল্ডারটি প্রজেক্টের মূল ফোল্ডার।

### Windows-এর চালানোর package কোথায় পাবেন?

[GitHub Actions](https://github.com/Juimu/sql-testing-tool/actions) খুলে সর্বশেষ সফল **Build, test and package** run দেখুন। সবুজ চিহ্ন থাকলে নিচের **Artifacts** অংশ থেকে `SQLNext-Windows-X64` বা প্রদর্শিত Windows artifact ডাউনলোড করুন। GitHub-এ login লাগতে পারে।

Artifact Extract করার পরে ভেতরের `SQLNext-0.2.0-windows-native.zip`-ও Extract করুন। সেখানে `SQLNext/SQLNext.exe` পাবেন; folder layout ভিন্ন হলে `SQLNext.exe` সার্চ করুন। **পুরো SQLNext ফোল্ডার একসঙ্গে রাখবেন**—শুধু `.exe` কপি করবেন না, কারণ পাশে runtime ও library থাকে। Bundled-runtime image হলে আলাদা Java/Maven লাগে না।

Workflow এখনো চললে অপেক্ষা করুন; লাল চিহ্ন থাকলে package প্রস্তুত হয়েছে ধরে নেবেন না। এই cloud পরিবেশে Linux package পরীক্ষা করা হয়েছে; Windows/macOS-এর ফল সংশ্লিষ্ট workflow-তে দেখতে হবে। Package-গুলো code-signed installer নয়।

## ২. কী কী সুবিধা আছে?

| পাতা | কাজ |
| --- | --- |
| Overview | প্রজেক্ট, findings, verified finding এবং task-এর আসল সংখ্যা দেখা |
| Projects & scope | অনুমোদনের reference ও অনুমোদিত URL সীমা সংরক্ষণ |
| Single / batch assessment | এক বা একাধিক অনুমোদিত URL-এর task queue বা schedule করা |
| Tasks & session history | অবস্থা, ফল, event history; pause/resume/retry/cancel |
| Scope validator | কোনো request না পাঠিয়ে URL-এর অনুমোদিত সীমা যাচাই |
| Findings | manual finding ও HTTP observation রাখা, search, reviewer/owner/CWE/CVSS ও status |
| Database inspection | স্থানীয় SQLite file read-only খুলে table/column ও redacted sample দেখা |
| Results & evidence | redacted sample group স্থায়ীভাবে রাখা, দেখা, export ও delete |
| Discovery / endpoints | owned-site query তৈরি ও manually reviewed endpoint URL যাচাই |
| Controlled laboratories | synthetic SQL, harmless file marker ও fixed script fixture চালানো |
| Proxy & Tor diagnostics | explicit HTTP/SOCKS5 route ও সীমিত route health check |
| Analytics / comparison | task-এর সংখ্যা ও একই target-এর দুটি HTTP ফল তুলনা |
| Reports | JSON, HTML, CSV, SARIF এবং PDF summary |
| Remediation assistant | local নিয়মভিত্তিক পরামর্শ ও optional external AI draft |
| Credential vault | master password দিয়ে আলাদা encrypted secret store |
| Audit & diagnostics | স্থানীয় পরিবর্তনের event দেখা ও JSON export |
| Compatibility | তৈরি, আংশিক তৈরি ও বাকি সুবিধার তালিকা |
| Settings | theme, data location, sqlmap ও optional AI configuration |

## ৩. সোর্স থেকে চালাতে যা লাগবে

- পূর্ণ **JDK 21 বা নতুন সংস্করণ**; শুধু Java Runtime/JRE নয়। Cloud-এ JDK 25 দিয়ে যাচাই হয়েছে।
- **Apache Maven 3.9.11** এবং প্রথমবার library ডাউনলোডের ইন্টারনেট।
- JavaFX চালানোর উপযুক্ত desktop; Linux-এ GTK/graphical libraries দরকার।

Terminal বা PowerShell-এ দেখুন:

```sh
java -version
javac -version
mvn -version
```

কোনো কমান্ড না পাওয়া গেলে সংশ্লিষ্ট software ইনস্টল করে `PATH` ঠিক করুন এবং নতুন Terminal খুলুন। Maven ভুল Java ব্যবহার করলে `JAVA_HOME` পূর্ণ JDK ফোল্ডারে দিন।

`pom.xml` থাকা ফোল্ডারে Terminal খুলে চালান:

```sh
mvn verify
mvn javafx:run
```

প্রথম কমান্ড build ও মূল পরীক্ষা করে; সফল হলে `BUILD SUCCESS` দেখাবে। দ্বিতীয়টি অ্যাপের উইন্ডো খোলে। UI-সহ headless পরীক্ষা করতে:

```sh
mvn -Pheadless verify
```

`target/sqlnext-0.2.0.jar` একা চলার fat JAR নয়; `java -jar` ব্যবহার করবেন না।

## ৪. প্রথম assessment চালান

### প্রজেক্ট তৈরি

**Projects & scope** পাতায় উদাহরণের মতো তথ্য দিন:

| ঘর | উদাহরণ |
| --- | --- |
| Project name | আমার অনুমোদিত প্রজেক্ট |
| Approved scope | `https://example.org/app` |
| Authorization reference | অনুমোদনের টিকিট `AUTH-001` |

**Create project** চাপুন। এখানে নিজের বা লিখিতভাবে অনুমোদিত target ব্যবহার করুন। Reference লিখলেই অ্যাপ নিজে থেকে মালিকানা বা অনুমতি প্রমাণ করে না। Password/token reference ঘরে লিখবেন না।

### Profile নির্বাচন

**Single / batch assessment** খুলে project ও profile নির্বাচন করুন:

| Profile | আসলে কী করে |
| --- | --- |
| HTTP baseline | সীমিত GET request; response status, security headers, TLS ও database error marker পর্যবেক্ষণ |
| Endpoint exposure | আপনার দেওয়া exact endpoint-গুলোর একই HTTP diagnostics; কোনো unrestricted wordlist enumeration নয় |
| sqlmap diagnostic | আলাদা installed sqlmap দিয়ে risk-1/level-1 boolean/error diagnostics; dumping বা file access flag নেই |
| SQL lab | in-memory synthetic SQLite-তে unsafe query ও prepared statement-এর পার্থক্য পরীক্ষা |
| File lab | অ্যাপের ভেতরে থাকা harmless marker file পড়া; remote server file পড়া নয় |
| Script lab | নির্দিষ্ট স্থানীয় text-transformation fixture ও identifier পরীক্ষা; WAF bypass নয় |

প্রতি লাইনে একটি target URL দিন; সর্বোচ্চ ১০০টি আলাদা URL। URL text file import করা যায়। Labs-এ URL শুধু project attribution-এর জন্য; কোনো network request হয় না।

Route দিন: `DIRECT`, `http://host:port` বা `socks5://host:port`। Priority ও কত মিনিট পরে চালাতে হবে নির্বাচন করুন। অনুমোদন নিশ্চিত করার checkbox টিক দিয়ে **Validate & queue** চাপুন।

সব URL আগে scope-এর সঙ্গে মেলে। Invalid/out-of-scope URL থাকলে queue তৈরি হয় না। একই project/URL/profile-এর unfinished task আবার দেওয়া হলে সেটি বাদ যায়।

### Scope কীভাবে মেলে?

Scope `https://example.org/app` হলে:

| URL | ফল |
| --- | --- |
| `https://example.org/app` | অনুমোদিত |
| `https://example.org/app/orders?id=1` | অনুমোদিত |
| `https://example.org/application` | অনুমোদিত পথের বাইরে |
| `http://example.org/app` | ভিন্ন protocol |
| `https://other.org/app` | ভিন্ন host |

Scheme, host, port ও path boundary মিলতে হয়। Credentials, fragments, encoded paths ও traversal path গ্রহণ করা হয় না। HTTP diagnostic redirects অনুসরণ করে না; proxy ব্যর্থ হলে চুপিসারে direct route নেয় না।

## ৫. Task ও ফলাফল

**Tasks & session history**-তে প্রতি সেকেন্ডে অবস্থা হালনাগাদ হয়:

- `QUEUED`: চালানোর অপেক্ষায়; pause করা যায়।
- `PAUSED`: resume করা যায়।
- `RUNNING`: চলছে; cancel করা যায়।
- `COMPLETED`: engine শেষ করেছে; এর অর্থ target নিরাপদ বা verified vulnerable নয়।
- `FAILED`: network/dependency/engine failure; কারণ দেখে manual retry করুন।
- `CANCELLED`: বাতিল হয়েছে।
- `INTERRUPTED`: আগের session হঠাৎ বন্ধ হয়েছিল; নিজে retry করতে হবে।

একটি worker কাজ চালায়; task-এর মাঝে অন্তত ১.১ সেকেন্ড বিরতি থাকে। HTTP response সর্বোচ্চ ৬৪ KiB পড়ে। Connect/read ও overall read limits আছে। Running request বাতিল হতে timeout পর্যন্ত সামান্য সময় লাগতে পারে। sqlmap-এর deadline তিন মিনিট; cancel হলে তার owned child process-ও বন্ধ করার চেষ্টা করা হয়।

**View result**-এ curated ফল, **Event history**-তে state পরিবর্তন দেখুন। HTTP task নির্বাচন করে **Import observations** চাপলে unverified findings যোগ হয়; আগের একই finding-এর evidence overwrite হয় না। sqlmap candidate এখনই auto-import করা হয় না—ফল review করে Findings-এ entry করুন।

## ৬. Optional sqlmap সেটআপ

Python 3 এবং trusted sqlmap distribution প্রয়োজন। Cloud/developer machine-এ helper চালাতে পারেন:

```sh
python scripts/install-sqlmap.py
```

Helper PyPI-এর নির্দিষ্ট `1.10.10` source release নামায়, SHA-256 যাচাই করে user tool directory-তে extract করে এবং paths দেখায়। এটি app task configuration বদলায় না।

**Settings → Configure sqlmap paths**-এ Python executable এবং `sqlmap.py`-এর পূর্ণ path দিন। Windows-এ Python executable খুঁজতে `where.exe python` ব্যবহার করতে পারেন।

Adapter শুধু নির্দিষ্ট flags ব্যবহার করে: `--risk=1 --level=1 --technique=BE --threads=1 --timeout=5 --retries=0 --delay=1 --ignore-redirects`। Arbitrary shell command/extra flags, database dumping, server-file read বা tamper execution নেয় না।

Engine-এর নিজস্ব output data directory-র `engine-output/task-ID`-তে থাকে; এতে sensitive target information থাকতে পারে এবং এটি encrypted নয়। প্রয়োজনমতো retention ও filesystem access নিয়ন্ত্রণ করুন।

## ৭. Findings review ও রিপোর্ট

**Findings → Record a finding** দিয়ে title, target, severity ও redacted evidence লিখুন। **Review selected** দিয়ে reviewer, owner, CWE যেমন `CWE-89`, নিজের assessed CVSS score `0–10` এবং verification দিন। CVSS vector calculator নেই।

`Verified` হলো সংরক্ষিত মানুষের সিদ্ধান্ত; অ্যাপের স্বয়ংক্রিয় সত্যায়ন নয়। Remediation status আলাদা: `Open`, `In Progress`, `Fixed`, `Accepted Risk`, `Reopened`। `Fixed` দিলেই পুনরায় পরীক্ষা হয় না। **Status history**-তে পরিবর্তন দেখা যায়।

**Reports** থেকে নতুন ফাইলে export করুন:

- JSON/HTML: Unicode title-সহ project, findings ও task summary।
- CSV: finding table; spreadsheet formula-এর ঝুঁকি কমাতে formula prefix neutralize করা হয়।
- SARIF: findings; unverified ফল `note`, false-positive review বাদ যায়।
- PDF: portable English/ASCII summary by IDs; পূর্ণ বাংলা title-এর জন্য HTML/JSON ব্যবহার করুন।

Evidence, finding target URL, authorization note ও raw engine output report-এ বাদ থাকে। Project scope ও title থাকে—শেয়ার করার আগে পড়ুন। আগের ফাইল overwrite হয় না; নতুন নাম দিন।

## ৮. Database, vault ও AI

**Database inspection:** অনুমোদিত local SQLite file নির্বাচন করুন; table/column দেখুন, Select all/none ব্যবহার করুন এবং redacted preview append করুন। সর্বোচ্চ ২০টি row; মান `[REDACTED]` থাকে। Project নির্বাচন করলে sample আলাদা persistent group হিসেবে Results & evidence-এ জমা হয়। Remote MySQL/PostgreSQL/SQL Server/Oracle connections বা driver bundle নেই; assistant-এ DBMS-specific guidance আছে।

**Credential vault:** অন্তত ১২ অক্ষরের master password দিয়ে named secret save/update/list-name/remove করা যায়। AES-256-GCM এবং PBKDF2-HMAC-SHA256, ৬০০,০০০ iteration ব্যবহৃত হয়। Master password disk-এ রাখা হয় না; হারালে উদ্ধার করা যায় না। OS keychain integration নেই। Workspace-এর অন্য তথ্য এই vault দিয়ে encrypted হয় না।

**Remediation assistant:** local advice কোনো AI model নয়। External AI ব্যবহার করতে vault-এ API key রাখুন, Settings-এ HTTPS OpenAI-compatible chat-completions endpoint/model/vault-entry-name দিন। Verified finding নির্বাচন করে **External AI (review & approve)** চাপুন। Displayed title/severity/CWE পর্যালোচনা করে approval দিন এবং vault password দিন। সংরক্ষিত default route ব্যবহার হয়; approval dialog-এ সেটি দেখুন। শুধু এই তিনটি finding field পাঠানো হয়; title-এও sensitive তথ্য থাকতে পারে। Evidence/target URL স্বয়ংক্রিয়ভাবে পাঠানো হয় না। Output human-reviewed draft, verification result নয়। Live AI service এই cloud-এ পরীক্ষা করা হয়নি; credential/configuration আপনার প্রয়োজন।

## ৯. তথ্যের অবস্থান ও ব্যাকআপ

সাধারণ database `~/.sqlnext/workspace.db`; Settings-এ আসল path দেখুন। Vault পাশের `credentials.vault` ফাইলে থাকে। Cloud helper সাধারণভাবে `/workspace/sqlnext-data` ব্যবহার করে।

অন্য data directory দিতে:

```sh
mvn javafx:run -Djavafx.options="-Dsqlnext.data=/path/to/workspace"
```

অথবা `SQLNEXT_DATA` environment variable ব্যবহার করুন। ব্যাকআপ নিতে অ্যাপ বন্ধ করে **পুরো data directory** কপি করুন। পুরোনো ০.১ workspace-এর tables রাখা হয় এবং নতুন schema যোগ হয়; upgrade-এর আগে ব্যাকআপ রাখুন। Workspace/audit plaintext; vault ছাড়া কোথাও password বা unredacted sensitive evidence রাখবেন না। Audit স্থানীয় এবং tamper-proof নয়।

## ১০. Cloud, package ও পরীক্ষা

```sh
cd /workspace/sql-testing-tool
bash scripts/cloud-setup.sh
bash scripts/cloud-mvn.sh -Pheadless verify
```

Setup official checksum, managed CA trust ও proxy configuration ব্যবহার করে। Verification বন্ধ করবেন না। Graphical display থাকলে `bash scripts/cloud-mvn.sh javafx:run`। Headless test কোনো web preview বা remote desktop তৈরি করে না।

নিজের OS-এর dependency-included ZIP তৈরি করতে Maven verify-এর পরে:

```sh
python scripts/package.py
```

পূর্ণ JDK-এর `JAVA_HOME` ঠিক থাকলে bundled-runtime native app image:

```sh
python scripts/package.py --native
```

Package-এ সেই OS-এর JavaFX native libraries থাকে; Linux ZIP Windows-এ ব্যবহার করবেন না। Runtime-বিহীন dependency ZIP-এ `SQLNext.bat`/`SQLNext.sh` চালাতে Java 21+ দরকার। Native image-এ আলাদা Java লাগে না। Native launcher যাচাই:

```sh
# Linux উদাহরণ
./target/native/SQLNext/bin/SQLNext --self-test
```

Build-এর `target/bom.json`/`bom.xml` হলো CycloneDX SBOM; distribution-এ SHA-256 file manifest থাকে। এটি code signing বা complete dependency lock-এর বিকল্প নয়।

HTTP fixture, scope, state persistence/recovery, reviewer history, vault tampering/wrong-password, report formats, SQLite redaction, process cancellation এবং UI থেকে lab queue করে completion পরীক্ষা করা হয়। Optional real sqlmap test শুধু local in-memory vulnerable fixture-এর বিরুদ্ধে চলে; public target ব্যবহার করে না। Live AI, real Tor/proxy end-to-end routing এবং সব OS-এর UI coverage দাবি করা হচ্ছে না।

## ১১. সমস্যা হলে

| সমস্যা | কী করবেন |
| --- | --- |
| `mvn`/`javac` নেই | Maven/পূর্ণ JDK ইনস্টল, PATH ও JAVA_HOME ঠিক করে নতুন Terminal খুলুন |
| `pom.xml` পাওয়া যাচ্ছে না | সঠিক project root-এ যান |
| Java release error | Maven-এর Java version 21+ কি না দেখুন |
| `Unable to open DISPLAY` | UI-এর জন্য graphical desktop; test-এর জন্য headless profile |
| Task FAILED | result diagnostic, target scope, route ও dependency paths দেখুন |
| Finding/report save হয় না | duplicate entry বা আগে থেকে থাকা filename পরীক্ষা করুন |
| Vault খোলে না | সঠিক master password ও অক্ষত vault file দরকার; password recover করা যায় না |
| Library download ব্যর্থ | connection/proxy/CA trust ঠিক করুন; TLS verification বন্ধ করবেন না |

## ১২. যা এখনো পূর্ণ হয়নি

মূল SRS-এর team login/RBAC, signed ও sandboxed plugin execution, OS keychain, remote multi-DB drivers, automatic Tor lifecycle/exit-IP verification, advanced report branding/retention, signed updater/rollback এবং signed native installers এখনো নেই। Arbitrary external search-result exploitation, production data dumping বা unrestricted tamper/file-read workflow যোগ করা হয়নি; সংশ্লিষ্ট SRS-এর controlled alternatives রাখা হয়েছে।

এই সংস্করণকে সম্পূর্ণ production/enterprise edition বলা হচ্ছে না। [Compatibility register](src/main/resources/compatibility.tsv), [requirement status](docs/requirements-status.csv), [মূল SRS](docs/SRS-v1.0.md) এবং [release checklist](docs/RELEASE-CHECKLIST.md) দেখুন। মূল v5.5 executable/source না থাকায় legacy behavior compatibility নিশ্চিত করা হয়নি।
