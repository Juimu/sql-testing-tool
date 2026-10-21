# SQLNext — বাংলায় ব্যবহার নির্দেশিকা

SQLNext হলো নিরাপত্তা পরীক্ষার প্রজেক্ট, অনুমোদিত ওয়েব ঠিকানা এবং পাওয়া সমস্যার তথ্য গুছিয়ে রাখার একটি ডেস্কটপ অ্যাপ। এর ইন্টারফেস JavaFX দিয়ে তৈরি এবং তথ্য SQLite ডাটাবেসে সংরক্ষিত থাকে।

**সংস্করণ ০.১.০ — প্রাথমিক কার্যকর সংস্করণ।** এটি আপনার দেওয়া SqliDumper Next পরিকল্পনার প্রথম বাস্তবায়ন। সব সুবিধা এখনো তৈরি হয়নি। **বর্তমানে অ্যাপ নিজে থেকে SQL Injection স্ক্যান করে না বা ডাটাবেস থেকে তথ্য বের করে না।**

## এখন কী কী করা যায়?

| পাতার নাম | কী কাজে লাগবে |
| --- | --- |
| Overview | প্রজেক্ট, নথিভুক্ত সমস্যা ও কাজের রেকর্ডের সংখ্যা এবং গুরুত্বের চার্ট দেখা |
| Projects & scope | প্রজেক্ট তৈরি ও পরীক্ষার অনুমোদিত ওয়েব ঠিকানা নির্ধারণ করা |
| Scope validator | এক বা একাধিক URL অনুমোদিত সীমায় আছে কি না যাচাই করা |
| Findings | নিজে পাওয়া সমস্যার বিবরণ, গুরুত্ব, প্রমাণ ও সমাধানের অবস্থা লিখে রাখা |
| Reports | JSON বা HTML রিপোর্ট তৈরি করা |
| Compatibility | কোন সুবিধা তৈরি হয়েছে ও কোনটি বাকি আছে দেখা |
| Settings | Dark/Light থিম নির্বাচন ও ডাটাবেসের অবস্থান দেখা |

অ্যাপ বন্ধ করে আবার খুললেও সংরক্ষিত প্রজেক্ট, সমস্যা ও থিম থেকে যায়।

## ১. কোড ডাউনলোড করুন

👉 **[সরাসরি ZIP ডাউনলোড করুন](https://github.com/Juimu/sql-testing-tool/archive/refs/heads/main.zip)**

অথবা [GitHub রিপোজিটরি](https://github.com/Juimu/sql-testing-tool) খুলে **Code → Download ZIP** চাপুন। ডাউনলোড হলে ZIP ফাইলটি **Extract All** দিয়ে খুলুন।

**এটি সোর্স কোড।** এখনো ডাবল-ক্লিক করে চালানোর `.exe` বা ইনস্টলার তৈরি হয়নি। ZIP Extract করলেই অ্যাপ চালু হবে না।

## ২. চালাতে যা লাগবে

- পূর্ণ **JDK 21 বা নতুন সংস্করণ**। শুধু Java Runtime/JRE যথেষ্ট নয়। প্রস্তুত cloud পরিবেশে JDK 25 দিয়ে পরীক্ষা করা হয়েছে।
- **Apache Maven 3.9.11**।
- প্রথমবার প্রয়োজনীয় লাইব্রেরি ডাউনলোডের জন্য ইন্টারনেট।
- অ্যাপের উইন্ডো দেখানোর উপযুক্ত ডেস্কটপ পরিবেশ। Linux-এ প্রয়োজনীয় GTK ও গ্রাফিক্যাল লাইব্রেরি থাকতে হবে।

JavaFX 21.0.8 এবং অন্য Java লাইব্রেরি Maven ডাউনলোড করবে। Windows/macOS এবং native installer এখনো যাচাই করা হয়নি।

Terminal বা PowerShell-এ পরীক্ষা করুন:

```sh
java -version
javac -version
mvn -version
```

তিনটি কমান্ডেই সংস্করণের তথ্য আসা উচিত। `javac` না পাওয়া গেলে পূর্ণ JDK ইনস্টল করুন। `mvn` না পাওয়া গেলে Maven ইনস্টল করে তার `bin` ফোল্ডার `PATH`-এ যোগ করুন। এরপর নতুন Terminal খুলুন। Maven ভুল Java ব্যবহার করলে `JAVA_HOME` আপনার JDK ফোল্ডারে সেট করুন।

## ৩. অ্যাপ চালু করুন

ZIP Extract করার পরে **যে ফোল্ডারে `pom.xml` আছে**, সেখানে Terminal খুলুন। Windows-এ ফোল্ডারের **Open in Terminal** ব্যবহার করতে পারেন। অথবা লিখুন:

```sh
cd sql-testing-tool-main
```

আপনার ফোল্ডারের নাম আলাদা হলে সেই নাম দিন। এরপর বিল্ড ও মূল পরীক্ষা চালান:

```sh
mvn verify
```

প্রথমবার লাইব্রেরি ডাউনলোড হতে সময় লাগতে পারে। সফল হলে শেষে `BUILD SUCCESS` দেখাবে। এই সাধারণ কমান্ডে মূল পরীক্ষাগুলো চলে; নিচের headless প্রোফাইলে UI পরীক্ষাও চলে।

অ্যাপের উইন্ডো খুলতে:

```sh
mvn javafx:run
```

`target` ফোল্ডারের JAR ডাবল-ক্লিক করে বা `java -jar` দিয়ে চালাবেন না। এটি একা চলার মতো প্যাকেজ নয়; Maven প্রয়োজনীয় লাইব্রেরিসহ অ্যাপ চালায়।

## ৪. প্রথমবার ব্যবহার করুন

### প্রজেক্ট তৈরি

**Projects & scope** খুলে নিচের মতো তথ্য দিন:

| ঘর | উদাহরণ |
| --- | --- |
| Project name | আমার অনুমোদিত টেস্ট প্রজেক্ট |
| Approved scope | `https://example.org/app` |
| Authorization reference | অনুমোদনের টিকিট নম্বর, যেমন `AUTH-001` |

**Create project** চাপুন। অনুমোদনের রেফারেন্স লিখবেন; পাসওয়ার্ড বা গোপন টোকেন লিখবেন না। এই রেফারেন্স নিজে থেকে মালিকানা বা অনুমতি যাচাই করে না। উদাহরণের ঠিকানাগুলো বোঝানোর জন্য; বাস্তবে নিজের বা অনুমোদিত প্রজেক্টের ঠিকানা ব্যবহার করুন।

### URL-এর সীমা যাচাই

**Scope validator** খুলুন, প্রজেক্ট নির্বাচন করুন এবং প্রতি লাইনে একটি URL লিখুন। তারপর **Validate URLs** চাপুন। একবারে সর্বোচ্চ ১,০০০ URL দেওয়া যায়।

Scope যদি `https://example.org/app` হয়:

| URL | ফলাফল |
| --- | --- |
| `https://example.org/app` | IN SCOPE — অনুমোদিত সীমায় আছে |
| `https://example.org/app/orders?id=1` | IN SCOPE — অনুমোদিত সীমায় আছে |
| `https://example.org/application` | OUT OF SCOPE — অনুমোদিত পথের বাইরে |
| `https://other.org/app` | OUT OF SCOPE — ভিন্ন হোস্ট |
| `http://example.org/app` | OUT OF SCOPE — ভিন্ন প্রোটোকল |

হোস্ট, প্রোটোকল, পোর্ট ও পথ মিলিয়ে যাচাই হয়। একই URL আবার দিলে `DUPLICATE` দেখায়। ভুল URL, URL-এ লগইন তথ্য, fragment, encoded path এবং `..` দিয়ে পথ বদলানোর চেষ্টা গ্রহণ করা হয় না।

**এটি আপনার কম্পিউটারে হওয়া URL যাচাই। ওয়েবসাইটে অনুরোধ পাঠানো বা দুর্বলতা পরীক্ষা করা হয় না।**

### সমস্যার তথ্য সংরক্ষণ

1. **Findings → Record a finding** খুলুন।
2. প্রজেক্ট নির্বাচন করুন এবং তার অনুমোদিত সীমার মধ্যে থাকা Target URL দিন।
3. সমস্যার শিরোনাম লিখুন।
4. গুরুত্ব নির্বাচন করুন: `Critical`, `High`, `Medium`, `Low` বা `Info`।
5. গোপন তথ্য বাদ দিয়ে প্রমাণ ও সমাধানের নোট লিখুন।
6. **OK** চাপুন।

এগুলো আপনার দেওয়া **যাচাই হয়নি এমন manual entry**। অ্যাপ এগুলোকে স্বয়ংক্রিয়ভাবে প্রমাণিত দুর্বলতা বলে ধরে নেয় না। একই project, URL ও title দিয়ে দ্বিতীয়বার সমস্যা যোগ করা যায় না।

Search দিয়ে খুঁজুন এবং **View evidence** দিয়ে নোট দেখুন। একটি সমস্যা নির্বাচন করে অবস্থা বদলান, তারপর **Update selected** চাপুন:

| অবস্থা | অর্থ |
| --- | --- |
| Open | সমস্যা নথিভুক্ত আছে |
| In Progress | সমাধানের কাজ চলছে |
| Fixed | ব্যবহারকারী সমাধান হয়েছে বলে চিহ্নিত করেছেন |
| Accepted Risk | ঝুঁকি মেনে নেওয়া হয়েছে |
| Reopened | সমস্যাটি আবার খোলা হয়েছে |

`Fixed` নির্বাচন করলে অ্যাপ আবার পরীক্ষা করে সমাধান নিশ্চিত করে না। Dashboard-এর open গণনায় `Fixed` ছাড়া সব অবস্থা অন্তর্ভুক্ত হয়।

### রিপোর্ট তৈরি

**Reports → Export JSON** অথবা **Export HTML** চাপুন। নতুন ফাইলের নাম ও অবস্থান নির্বাচন করুন। HTML রিপোর্ট ব্রাউজারে খোলা যায়।

রিপোর্টে পুরো workspace-এর প্রজেক্ট ও সমস্যার সারাংশ থাকে। বিস্তারিত evidence, অনুমোদনের নোট এবং সমস্যার target URL বাদ দেওয়া হয়। প্রজেক্টের scope URL ও শিরোনাম থাকে—শেয়ার করার আগে পড়ে দেখুন। একই নামের ফাইল আগে থেকে থাকলে অন্য নাম দিন; অ্যাপ আগের ফাইল মুছে লেখে না।

## ৫. তথ্য ও ব্যাকআপ

সাধারণভাবে তথ্য থাকে `~/.sqlnext/workspace.db` ফাইলে। এখানে `~` আপনার ব্যবহারকারীর হোম ফোল্ডার। **Settings** পাতায় ব্যবহৃত অবস্থান দেখা যায়।

অন্য ফোল্ডারে রাখতে:

```sh
mvn javafx:run -Djavafx.options="-Dsqlnext.data=/path/to/workspace"
```

`/path/to/workspace`-এর জায়গায় লেখার অনুমতি আছে এমন ফোল্ডার দিন। `SQLNEXT_DATA` পরিবেশ ভেরিয়েবল দিয়েও অবস্থান নির্ধারণ করা যায়; `sqlnext.data` দিলে সেটি অগ্রাধিকার পায়।

ব্যাকআপ নিতে অ্যাপ বন্ধ করে `workspace.db` ফাইলের কপি নিরাপদ স্থানে রাখুন। বর্তমান ডাটাবেস ও audit রেকর্ড **এনক্রিপ্ট করা নয়** এবং একজন স্থানীয় ব্যবহারকারীর জন্য তৈরি। পাসওয়ার্ড, API key বা সংবেদনশীল বাস্তব তথ্য রাখবেন না।

## ৬. প্রস্তুত cloud পরিবেশের নির্দেশনা

এই অংশ প্রস্তুত Linux cloud workspace-এর জন্য। নিজের Windows কম্পিউটারে আগের সাধারণ Maven নির্দেশনা ব্যবহার করুন।

```sh
cd /workspace/sql-testing-tool
bash scripts/cloud-setup.sh
```

Setup script প্রয়োজন হলে official checksum যাচাই করে JDK 25 ও Maven প্রস্তুত করে, cloud proxy ও বিশ্বস্ত CA ব্যবহার করে এবং পরীক্ষা চালায়। প্রয়োজনীয় টুল থাকলে আবার ডাউনলোড করে না।

আগে থেকে প্রস্তুত থাকলে পরীক্ষা চালাতে:

```sh
bash scripts/cloud-mvn.sh -Pheadless verify
```

নিজের উপযুক্ত Linux পরিবেশে পর্দা ছাড়া পরীক্ষা চালাতে:

```sh
mvn -Pheadless verify
```

এই headless পরীক্ষা পর্দা ছাড়াই JavaFX toolkit চালিয়ে অ্যাপের শুরু ও view তৈরি যাচাই করে। এটি দূরবর্তী ডেস্কটপ বা ওয়েব প্রিভিউ তৈরি করে না। গ্রাফিক্যাল display থাকলে:

```sh
bash scripts/cloud-mvn.sh javafx:run
```

Cloud helper তথ্যের জন্য সাধারণভাবে `/workspace/sqlnext-data` ব্যবহার করে। Cloud-এ interactive desktop ব্যবহার যাচাই করা হয়নি।

## ৭. সমস্যা হলে কী করবেন?

| সমস্যা | করণীয় |
| --- | --- |
| `mvn` পাওয়া যাচ্ছে না | Maven ইনস্টল করুন, `PATH` ঠিক করুন ও নতুন Terminal খুলুন |
| `javac` পাওয়া যাচ্ছে না | শুধু JRE নয়, পূর্ণ JDK ইনস্টল করুন |
| `release version 21 not supported` | JDK 21 বা নতুন সংস্করণ এবং Maven-এর ব্যবহৃত Java যাচাই করুন |
| `pom.xml` পাওয়া যাচ্ছে না | `pom.xml` থাকা মূল ফোল্ডারে যান |
| লাইব্রেরি ডাউনলোড ব্যর্থ | ইন্টারনেট, proxy ও certificate trust configuration দেখুন; TLS যাচাই বন্ধ করবেন না |
| `Unable to open DISPLAY` | অ্যাপের উইন্ডোর জন্য গ্রাফিক্যাল desktop দরকার; শুধু পরীক্ষার জন্য headless কমান্ড ব্যবহার করুন |
| Target scope-এর বাইরে | অনুমোদিত ঠিকানার scheme, host, port ও path মিলিয়ে দেখুন |
| Finding যোগ হচ্ছে না | সব ঘর পূরণ করুন এবং একই project/URL/title আগে আছে কি না দেখুন |
| রিপোর্ট সেভ হচ্ছে না | লেখার অনুমতি আছে এমন ফোল্ডার ও নতুন ফাইলের নাম দিন |

## ৮. কোন সুবিধাগুলো এখনো বাকি?

- স্বয়ংক্রিয় SQL Injection পরীক্ষা ও sqlmap integration।
- প্রকৃত batch assessment, task scheduling এবং task recovery।
- ডাটাবেস, টেবিল ও কলাম পরিদর্শন এবং নিয়ন্ত্রিত sample-data workflow।
- Asset discovery, endpoint পরীক্ষা এবং নিয়ন্ত্রিত file/script laboratory।
- Tor এবং HTTP/SOCKS5 proxy ব্যবস্থাপনা।
- AI assistant, plugins ও external integration।
- Credential vault, encryption, user role ও team permission।
- PDF, CSV, SARIF রিপোর্ট ও পুরোনো পরীক্ষার সঙ্গে তুলনা।
- Native installer, স্বয়ংক্রিয় update এবং Windows/macOS-এর পূর্ণ যাচাই।

সরাসরি dependency ও build plugin-এর সংস্করণ নির্দিষ্ট আছে। সম্পূর্ণ transitive dependency lock, SBOM এবং production release যাচাই এখনো হয়নি।

## ৯. কী পরীক্ষা করা হয়েছে?

প্রস্তুত Linux cloud পরিবেশে **৪টি automated test পাস করেছে** এবং Maven build সফল হয়েছে।

- `WorkspaceTest`: scope-এর সীমা, ভুল তথ্য প্রত্যাখ্যান, duplicate প্রতিরোধ, transaction rollback, audit-এর সামঞ্জস্য, ডাটাবেস আবার খুললে তথ্য থাকা, settings এবং report export যাচাই করে।
- `AppSmokeTest`: headless JavaFX toolkit চালিয়ে সাতটি view তৈরি ও dashboard layout যাচাই করে।

এটি সব বাটনের পূর্ণ interactive পরীক্ষা, Windows/macOS যাচাই বা production release certification নয়।

## ১০. গুরুত্বপূর্ণ ফাইল

| ফাইল বা ফোল্ডার | কাজ |
| --- | --- |
| `pom.xml` | Maven build ও dependency configuration |
| `src/main/java/dev/sqlnext/App.java` | ডেস্কটপ অ্যাপ ও পাতাগুলো |
| `src/main/java/dev/sqlnext/Store.java` | SQLite-এ তথ্য সংরক্ষণ |
| `src/main/java/dev/sqlnext/Scope.java` | URL-এর অনুমোদিত সীমা যাচাই |
| `src/main/java/dev/sqlnext/Reports.java` | JSON/HTML রিপোর্ট তৈরি |
| `src/main/resources/theme.css` | রং ও সাজসজ্জা |
| `src/test/` | Automated test |
| [মূল SRS](docs/SRS-v1.0.md) | আপনার দেওয়া বিস্তারিত পরিকল্পনা |
| [Compatibility register](src/main/resources/compatibility.tsv) | কোন সুবিধা কতটুকু তৈরি হয়েছে |

মূল SqliDumper v5.5-এর সোর্স বা executable দেওয়া হয়নি। তাই তার সঙ্গে আচরণগত সামঞ্জস্য নিশ্চিত করা হয়নি। এই README বর্তমান কোডের বাস্তব সুবিধা ও সীমাবদ্ধতা বর্ণনা করে।
