# دليل مشروع aias — خدمة التحقق من الطلبات

> دليل تعليمي مبسّط لمن يبدأ مع هذا المشروع ومع Spring AI. يشرح: ما الذي يفعله النظام، ممّ يتكوّن، كيف يمرّ الطلب فيه، كيف تشغّله على جهازك، وكيف تهيّئه للاستخدام الحقيقي.
>
> المصطلحات الإنجليزية تُترك كما هي (Check, Module, Port…) لأنها هي الأسماء الموجودة في الكود والوثائق.

---

## 1. ما هو النظام بجملة واحدة

موظف يستلم طلباً (مثلاً طلب منحة دراسية). النظام **يقرأ بيانات الطلب من قاعدة بيانات الجهة**، **يقرأ المستندات المرفقة** (PDF، صور، جداول Excel)، ثم **يقارن كل ذلك بشروط الخدمة المكتوبة بلغة طبيعية** مستعيناً بنموذج ذكاء اصطناعي (LLM)، ويُخرج **تقريراً** بالنتيجة: مطابق (COMPLIANT)، غير مطابق (NOT_COMPLIANT)، أو يحتاج مراجعة يدوية (NEEDS_MANUAL_REVIEW). الموظف يقرأ التقرير ويسجّل **قراره**، وإن وافق قد يستدعي النظام واجهة الاعتماد (Approval API) عند الجهة.

العملية الواحدة من البداية إلى التقرير تُسمّى **Check**.

### المبدأ الأهم: الذكاء الاصطناعي يحلّل فقط، ولا ينفّذ
هذه قاعدة تصميم ثابتة في المشروع (تُسمّى guardrails في الوثائق):
- النموذج **لا يملك أدوات** (tools): لا ينفّذ SQL، لا يفتح ملفات، لا يستدعي أي واجهة.
- الاستعلامات تُكتب مسبقاً من مدير الخدمة وتُنفَّذ **كما هي** مع ربط رقم الطلب كقيمة (bind parameter) — لا يُبنى SQL من نص حر أبداً.
- محتوى المستندات يُرسل للنموذج **كبيانات** داخل علامات تحديد، لا كتعليمات.
- لا يُحتفظ بأي بيانات بين Check وأخرى.

---

## 2. المكوّنات (الموديولات)

النظام تطبيق Spring Boot واحد (deployable واحد) مقسّم إلى خمسة موديولات، كلّ منها حزمة Java تحت `io.agenticai`:

| الموديول | الحزمة | دوره بكلمات بسيطة |
|---|---|---|
| **REG** — Service Registry | `io.agenticai.reg` | "دليل الخدمات". يحمّل عند بدء التشغيل مجلدات الخدمات (كل خدمة = مجلد فيه ملفان) ويسجّل إصداراتها، ويسجّل اتصالات قواعد بيانات الجهة. |
| **DOC** — Document Access | `io.agenticai.doc` | "قارئ المستندات". يجلب المستندات (من مجلد تخزين، أو من عمود BLOB في قاعدة بيانات، أو من رفع يدوي)، يتعرّف على نوعها من محتواها، يستخرج نصّها أو جداولها، ويرسل الممسوح ضوئياً لنموذج قراءة. |
| **CHK** — Check Engine | `io.agenticai.chk` | "محرّك الفحص". يشغّل خطوات الـ Check بالترتيب الثابت: استعلامات → مستندات → فحوص حتمية → مقارنة بالنموذج → تحقق من إجابة النموذج → الحالة العامة. |
| **RPT** — Report Store | `io.agenticai.rpt` | "مخزن التقارير". يحفظ كل Check ونتيجتها وقرار الموظف، ويقدّم القراءة والتنظيف الدوري. |
| **INT** — Host Integration | `io.agenticai.integration` | "الواجهة الخارجية". واجهة REST الوحيدة التي تستخدمها شاشة الموظف: بدء Check، رفع المستندات، تأكيد الرفع، تسجيل القرار، قراءة التقارير. وهي التي تستدعي Approval API عند الجهة. |

> ملاحظة: حزمة INT اسمها `integration` لأن `int` كلمة محجوزة في Java.

ويوجد جزء مشترك **`io.agenticai.platform`** يحمل ما يخص المنصة كلها:
- `config/CheckLimitsProperties` — حدود كل Check (المهلة، أقصى عدد صفوف، أقصى حجم ملف…).
- `mcp/McpQueryChannel` + `SpringAiMcpQueryChannel` — قناة تنفيذ الاستعلامات عبر MCP (انظر §4).
- `tx/JdbcSavepoints` — نقاط حفظ داخل المعاملة لعزل فشل عنصر واحد عن الباقي.
- `web/Problems` — بناء ردود الأخطاء بصيغة ProblemDetail الموحّدة.

### كيف تتواصل الموديولات؟
كل موديول له حزمة `contract` فقط هي المسموح للآخرين استيرادها (مثل `io.agenticai.reg.contract.ServiceRegistry`). لا موديول يصل إلى جداول أو خدمات موديول آخر مباشرة. الاتصال داخل نفس العملية (in-process)، لا HTTP بين الموديولات.

### الطبقات داخل كل موديول
```
controller  →  service  →  domain
                  ↓
         port ⇄ adapter        (كل شيء خارجي خلف port: ملفات، JDBC، MCP، النموذج)
                  ↓
        repository / entity    (جداول الموديول نفسه فقط)
```
- **port**: واجهة Java عادية تصف "ما نحتاجه" (مثل `DocumentReadingModelPort.read(bytes, mediaType, deadline)`).
- **adapter**: التنفيذ الفعلي الذي يلمس العالم الخارجي (مثل `SpringAiDocumentReadingAdapter`). يمكن استبداله دون لمس باقي الكود.

---

## 3. رحلة Check كاملة (خطوة بخطوة)

```
شاشة الموظف ──POST /api/v1/checks──▶ INT ──▶ CHK.startCheck
                                               │  يسأل REG: هل الخدمة متاحة؟ ما إصدارها الحالي؟
                                               │  يسجّل في RPT صف Check Run
                                               ▼
                        (خدمة manual)  AWAITING_DOCUMENTS   ◀── الموظف يرفع المستندات عبر INT → DOC يخزّنها
                                               │  POST .../upload-confirmation
                                               ▼
                        (خدمة path/blob) RUNNING  ← يبدأ خط الأنابيب في الخلفية:
                           1. استعلامات الخدمة عبر MCP (قراءة فقط، رقم الطلب كقيمة مربوطة)
                           2. جلب المستندات عبر DOC (ملفات / BLOB / المرفوع يدوياً) وقراءتها
                           3. فحوص حتمية (1): لكل نوع مستند مطلوب → نتيجة
                           4. مقارنة بالنموذج: معرفة الخدمة + البيانات → نتائج findings
                           5. فحوص حتمية (2): التحقق من إجابة النموذج (الدليل موجود؟ الأرقام والتواريخ محسوبة في الكود)
                           6. الحالة العامة: COMPLIANT / NOT_COMPLIANT / NEEDS_MANUAL_REVIEW
                           7. الإنهاء: حفظ التقرير في RPT، حذف المستندات المرفوعة في DOC
                                               ▼
الموظف ──GET /api/v1/check-reports/{id}──▶ التقرير
الموظف ──POST .../decision (APPROVED/REJECTED)──▶ RPT يسجّل القرار؛ وإن كانت الخدمة تفعّل Approval API → INT يستدعيه مرة واحدة
```

**أنماط جلب المستندات (fetch mode)** — تُحدَّد في تعريف الخدمة:
| النمط | معناه |
|---|---|
| `manual` | الموظف يرفع المستندات بنفسه. الـ Check تنتظر (AWAITING_DOCUMENTS) حتى يؤكد الرفع. |
| `path` | استعلام يعيد مسارات ملفات داخل "مجلد التخزين" المسموح. أي مسار خارجه يُرفض. |
| `blob` | استعلام يعيد المحتوى من عمود BLOB عبر اتصال JDBC للقراءة فقط. |

**لماذا تنتهي Check بـ FAILED؟** سبب واحد دائماً من قائمة مغلقة: `TIMED_OUT`، `MODEL_UNAVAILABLE`، `MODEL_OUTPUT_INVALID`، `MODEL_NOT_PERMITTED`، `UPLOAD_WINDOW_EXPIRED`، `INTERRUPTED` (إعادة تشغيل)، `INTERNAL_ERROR`.

---

## 4. أين يظهر Spring AI في المشروع؟ (الجزء التعليمي)

Spring AI مكتبة تجعل التعامل مع نماذج اللغة في Spring Boot مثل التعامل مع أي خدمة أخرى. المشروع يستخدم منها **أقل ما يمكن وبشكل محايد للمزوّد**. إليك المفاهيم كما تظهر فعلاً في الكود:

### 4.1 `ChatModel` — "الهاتف الذي نتصل به بالنموذج"
واجهة واحدة: تعطيها `Prompt` وتعيد `ChatResponse`. المشروع لا يستخدم `ChatClient` (الواجهة الأعلى التي تضيف ذاكرة وأدوات) عمداً، لأن القاعدة "لا أدوات ولا ذاكرة".

يوجد **بينان (beans) منفصلان** من `ChatModel`:
- `comparisonModel` — نموذج المقارنة (في CHK، يُبنى في `chk/config/ComparisonModelConfiguration`).
- `documentReadingModel` — نموذج قراءة المستندات الممسوحة والصور (في DOC، `doc/config/DocumentReadingModelConfiguration`).

كل منهما يُضبط من مفاتيحه الخاصة، ولا يُحقن أحدهما في موديول الآخر.

### 4.2 `Prompt` و`Message` — "ما نرسله"
كل استدعاء هو `Prompt` جديد فيه **رسالتان بالضبط** ولا تاريخ محادثة:
- رسالة نظام (`SystemMessage`): التعليمات الثابتة + معرفة الخدمة (knowledge.md) كما هي.
- رسالة مستخدم (`UserMessage`): البيانات فقط، داخل علامات `<check-data source="…">…</check-data>` مع تهريب أي علامة مشابهة داخل المحتوى.

انظر `chk/adapter/SpringAiComparisonAdapter.java`.

### 4.3 `Media` — "إرسال صورة أو ملف"
في DOC، الصورة أو صفحة الـ PDF تُرسل كـ `Media(MimeType, bytes)` داخل رسالة المستخدم. الـ PDF الممسوح ضوئياً يُحوَّل أولاً إلى صور صفحات (PDFBox) لأن بعض المزوّدين المتوافقين مع OpenAI لا يقبلون ملف PDF مباشرة. انظر `doc/adapter/SpringAiDocumentReadingAdapter.java` و`PdfPageRenderer.java`.

### 4.4 `BeanOutputConverter` — "إجبار النموذج على JSON بشكل معيّن"
بدل تحليل نص حر، يطلب CHK من النموذج إجابة بصيغة JSON مطابقة للسجل `ComparisonOutput` (قائمة findings). المحوّل:
1. يولّد مخطط JSON Schema من الكلاس ويضعه في التعليمات.
2. يحوّل الإجابة إلى كائن Java. إن فشل → `MODEL_OUTPUT_INVALID`.

**مهم:** إجابة النموذج **لا تُصدَّق كما هي**. الكود يعيد التحقق من كل نتيجة: هل الدليل موجود فعلاً في البيانات؟ هل الحدّ مذكور في معرفة الخدمة؟ ويعيد حساب المقارنات الرقمية والتاريخية بنفسه (`chk/domain/FindingVerifier`).

### 4.5 "نقطة الربط بالمزوّد" (provider seam)
الكود كله يرى `ChatModel` المحايد. الموضع **الوحيد** الذي يعرف مزوّداً بعينه هو كلاس الإعداد الذي يبني الـ bean (`OpenAiChatModel` عبر starter OpenAI). تبديل المزوّد = تغيير إعدادات وربما starter، بلا تعديل في منطق العمل. أي مزوّد له واجهة متوافقة مع OpenAI (Gemini، Groq، Ollama، OpenRouter…) يعمل عبر `spring.ai.openai.base-url`.

### 4.6 MCP — "قناة الاستعلامات"، وليست أداة للنموذج
MCP (Model Context Protocol) بروتوكول لربط أدوات بالنماذج. المشروع يستخدم **عميل MCP** من Spring AI لكن **بالاتجاه المعاكس للمعتاد**: التطبيق نفسه (لا النموذج) هو من يستدعي أداة `query` على خادم MCP صغير يتصل بقاعدة بيانات الجهة للقراءة فقط. ولضمان ألا تصل أدوات MCP للنموذج أبداً:
```properties
spring.ai.mcp.client.toolcallback.enabled=false
```
والتطبيق يرفض الإقلاع لو وجد أي `ToolCallbackProvider`. الخادم المحلي للتجربة: `governance/mcp-servers/oracle/index.js` (Node).

### 4.7 بوابة الأمان: المستوى المجاني والبيانات الحقيقية
```
tier = FREE  و  data-class = REAL   ⇒  لا يُستدعى النموذج أبداً (MODEL_NOT_PERMITTED)
```
بيانات حقيقية لا تُرسل لمزوّد مجاني. في الإنتاج تضع `tier=APPROVED` بعد اعتماد المزوّد رسمياً.

---

## 5. تشغيل المشروع على جهازك

### المتطلبات
- JDK 25 و Maven 3.9+.
- Oracle (الاختبار المحلي يستخدم حاوية `gvenzl/oracle-free` في Docker).
- Node.js (لخادم MCP المحلي): `cd governance/mcp-servers/oracle && npm install`.
- Python 3 (لسكربتات الاختبار).

### الخطوات
1. **ملف الإعداد المحلي** (غير مرفوع إلى git):
   ```bash
   cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
   ```
   ثم عدّل بيانات اتصال Oracle (`spring.datasource.*` و`ORACLE_USER/ORACLE_PASSWORD` لخادم MCP).
2. **المفاتيح السرية** في ملف `local/secrets.properties` (المجلد `local/` كله مستبعد من git):
   ```properties
   spring.ai.openai.api-key=...        # مفتاح المزوّد (مثلاً Gemini من aistudio.google.com)
   LOCAL_JDBC_CREDENTIAL=user:password # لاتصال blob عبر JDBC إن استخدمته
   ```
3. **مجلد الخدمات** `local/package-directory/` — انظر §6 لشكل الخدمة. أول تشغيل بمجلد فارغ طبيعي (لا خدمات).
4. **البناء والتشغيل**:
   ```bash
   mvn -q -DskipTests package
   java -jar target/agentic-ai-library-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
   ```
   عند البدء: Flyway ينشئ الجداول (`src/main/resources/db/migration/V1..V4`)، ثم REG يحمّل مجلد الخدمات ويفعّل الاتصالات، ثم CHK ينهي أي Check كانت معلّقة قبل إعادة التشغيل بحالة INTERRUPTED.
5. **تحقق**:
   ```bash
   curl localhost:7271/api/v1/services        # قائمة الخدمات
   curl localhost:7271/api/v1/load-results    # تقرير التحميل (ما سُجّل وما رُفض ولماذا)
   ```

> الطريق الأسهل للتجربة الكاملة: `python3 scripts/e2e/setup_fixtures.py` ينشئ خدمات تجريبية واصطناعية ويعيد التشغيل، ثم `python3 scripts/e2e/simulate.py --only manual` يشغّل Check كاملة.

---

## 6. كيف تُعرَّف خدمة؟ (مجلد واحد = خدمة)

```
package-directory/
└── scholarship-request/        ← اسم المجلد = رمز الخدمة (أحرف صغيرة، أرقام، شرطات)
    ├── knowledge.md            ← "معرفة الخدمة": الشروط بلغة طبيعية، تُعطى للنموذج كما هي
    └── service.yaml            ← "تعريف الخدمة": ينفّذه الكود حرفياً، ولا يراه النموذج أبداً
```

مثال `service.yaml`:
```yaml
service: scholarship-request
version: 1
input: requestId                      # اسم المعامل الوحيد المسموح في كل الاستعلامات
queries:
  applicant:
    connection: main-db               # اسم اتصال مسجّل في إعدادات البيئة
    sql: SELECT gpa, credits FROM applicants WHERE request_id = :requestId
documents:
  fetch: manual                       # path | blob | manual
  required: [TRANSCRIPT, ID_CARD]
approval:
  enabled: false
  # api: POST /requests/{requestId}/approve   ← عند التفعيل
```

قواعد يرفض REG مخالفتها عند التحميل (وتظهر في `/api/v1/load-results` مع سبب إنجليزي واضح): استعلام ليس SELECT واحداً، معامل غير `:requestId`، اتصال غير مفعّل، نمط جلب مجهول، إصدار معدّل في مكانه (يجب رفع رقم الإصدار)، ملف دخيل في المجلد، أي مفتاح خارج البنية المغلقة (مثل `timeout` أو `max_rows` — هذه حدود المنصة لا الخدمة).

**الإصدارات لا تُحذف ولا تُعدَّل أبداً**: كل Check تُثبَّت على الإصدار الذي بدأت به حتى لو ظهر إصدار أحدث أثناءها.

---

## 7. الإعداد للاستخدام الحقيقي (قائمة مراجعة)

### 7.1 مفاتيح الإعداد الأساسية
| المفتاح | المعنى | ملاحظة |
|---|---|---|
| `spring.datasource.*` | قاعدة بيانات الخدمة نفسها (Oracle 19c+) | الجداول تبدأ بـ `REG_/DOC_/CHK_/RPT_`؛ Flyway يديرها |
| `aias.registry.package-directory` | مجلد الخدمات | يقرأه مدير الخدمة فقط؛ نشر خدمة = إضافة مجلد + إعادة تشغيل |
| `aias.registry.environment-name` | اسم البيئة | يُسجَّل على كل اتصال |
| `aias.registry.connections[n].*` | اتصالات قواعد بيانات الجهة | `type` = `mcp` أو `jdbc`؛ `read-only=true` إلزامي؛ `credential-reference` اسم مرجع فقط، لا كلمة مرور |
| `aias.check.timeout` | مهلة الـ Check من لحظة RUNNING | افتراضي PT2M |
| `aias.check.upload-window` | كم ينتظر النظام رفع المستندات | **مطلوب، لا افتراضي** |
| `aias.check.max-rows` / `max-file-size` / `max-uploads` | حدود كل Check | افتراضي 100 / 10MB / 20 |
| `aias.check.comparison-model.{provider,model,tier}` | نموذج المقارنة | `tier`: FREE أو APPROVED |
| `aias.documents.reading-model.{provider,model,tier,instruction}` | نموذج قراءة المستندات | `instruction` هي التعليمة الوحيدة التي يراها |
| `aias.documents.storage-root` | مجلد التخزين المسموح لنمط `path` | غيابه = كل مستند path يُعدّ UNREADABLE |
| `aias.documents.data-class` | `SYNTHETIC` أو `REAL` | مع `REAL` يجب أن تكون النماذج `APPROVED` وإلا لا تُستدعى |
| `aias.integration.approval.base-address` | عنوان Approval API عند الجهة | التعريف في الخدمة يعطي الطريقة والمسار فقط |
| `aias.integration.approval.timeout` | مهلة استدعاء الاعتماد | افتراضي 10 ثوانٍ، محاولة واحدة بلا إعادة |
| `aias.integration.upload.request-limit` | أقصى حجم طلب رفع | يجب ألا يقل عن `max-file-size` |
| `aias.reports.retention-days` | مدة الاحتفاظ بالتقارير | غيابه = لا تنظيف (مع رسالة في السجل) |
| `aias.reports.purge-schedule` / `list-limit` | جدول التنظيف الدوري (cron) وأقصى عدد في قوائم التقارير | — |
| `aias.check.deadline-check-interval` / `aias.registry.load-lock-timeout` | فترة فحص المهلات، ومهلة قفل التحميل عند بدء التشغيل | قيم تشغيلية؛ الافتراضيات كافية غالباً |
| `spring.ai.openai.base-url` / `api-key` | عنوان ومفتاح المزوّد المتوافق مع OpenAI | المفتاح من متغيّر بيئة أو ملف سري، أبداً في الريبو |

### 7.2 قائمة ما قبل الإنتاج
1. **المزوّد**: اعتمد مزوّداً رسمياً (شروط عدم التدريب على البيانات) ثم اضبط `tier=APPROVED` و`data-class=REAL`. قبل ذلك: بيانات اصطناعية فقط.
2. **الأسرار**: مفتاح النموذج، كلمات مرور قواعد البيانات، `LOCAL_JDBC_CREDENTIAL` → متغيرات بيئة أو خزنة أسرار. لا تضعها في `application-*.properties`.
3. **خادم MCP**: الخادم المحلي في `governance/mcp-servers/oracle` للتجربة. في الإنتاج شغّله كخدمة بمستخدم قاعدة بيانات **للقراءة فقط** و`ORACLE_ALLOW_WRITE=false`، أو استبدله بخادم MCP الجهة (يجب أن يقدّم أداة `query` تأخذ `{sql, binds, maxRows}`).
4. **مجلد التخزين** (`path`): نقطة تحميل للقراءة فقط؛ النظام يرفض أي مسار يخرج منه (بما في ذلك الروابط الرمزية).
5. **اتصال JDBC** (`blob`): مستخدم للقراءة فقط؛ الاتصال يُفتح بـ `setReadOnly(true)`.
6. **Approval API**: يجب أن تقبل الجهة الاعتماد نفسه مرتين بأمان (idempotent)، لأن النظام قد ينتهي بمهلة بينما وصل الطلب فعلاً.
7. **المصادقة**: غير موجودة في هذا الإصدار بقرار صريح (amendment A2). ضع الخدمة خلف بوابة/شبكة موثوقة حتى تُضاف.
8. **الحجم**: `aias.check.pipeline-threads` (افتراضي 4) عدد الـ Checks المتزامنة؛ ارفعه بحسب حصة المزوّد والعتاد.
9. **المراقبة**: السجلات لا تحتوي أبداً محتوى مستندات أو نتائج استعلامات أو إجابات النموذج (قاعدة مطبّقة)؛ راقب معدلات FAILED حسب السبب.

---

## 8. واجهة الـ API (ملخص)

كل الأخطاء بصيغة RFC 9457 ProblemDetail مع حقل `code` ثابت بالشكل `{MODULE}-{http}[-{SLUG}]` (مثل `CHK-422-SERVICE-NOT-AVAILABLE`). الواجهات تعتمد على `code` لا على نص الرسالة.

| العملية | الطريقة والمسار | الموديول |
|---|---|---|
| بدء Check | `POST /api/v1/checks` → 202 | INT |
| رفع مستند | `POST /api/v1/checks/{id}/documents` (multipart) → 201 | INT |
| تأكيد الرفع | `POST /api/v1/checks/{id}/upload-confirmation` → 202 | INT |
| قراءة التقرير | `GET /api/v1/check-reports/{id}` | INT |
| تسجيل القرار | `POST /api/v1/checks/{id}/decision` → 201 | INT |
| الـ Checks لطلب | `GET /api/v1/check-reports?serviceCode=&requestNumber=` | INT |
| الخدمات المتاحة | `GET /api/v1/services` | REG |
| تقرير التحميل | `GET /api/v1/load-results` | REG |
| حالة Check جارية | `GET /api/v1/active-checks/{id}` | CHK |

التوثيق الكامل يولَّد من التطبيق نفسه: OpenAPI على `/v3/api-docs/{reg|doc|chk|rpt|int}`، ووثائق Markdown في `governance/shared/backend/modules/<MOD>/api-docs/`.

---

## 9. الاختبار (بدون JUnit)

المشروع لا يستخدم JUnit بقرار المالك. الاختبار يتم ضد التطبيق الحقيقي:
| الأداة | ماذا تفعل | التشغيل |
|---|---|---|
| **api-verify** | يحمل وثائق الـ API المولَّدة على عقد الـ API الرسمي ويستدعي كل عملية | `python3 governance/governance-tools/api-verify-generator/build.py REG DOC CHK RPT INT` ثم تشغيل السكربتات في `governance/shared/backend/modules/<MOD>/test-api/` |
| **المحاكاة الشاملة** `scripts/e2e/` | ~170 سيناريو تغطي الرحلة كاملة وكل حالات الرفض والحدود والمهلات | `python3 scripts/e2e/setup_fixtures.py && python3 scripts/e2e/simulate.py` |

أدوات مساعدة في `scripts/e2e/`: `model_stub.py` (نموذج محلي بإجابات محددة سلفاً — لا يستهلك حصة المزوّد)، `model_tap.py` (يسجّل ما يرسله التطبيق للنموذج فعلاً)، `mcp_tap.py` (يسجّل SQL المرسل عبر MCP). التقارير في `governance/project-artifacts/`.

> نصيحة للمبتدئ: شغّل `python3 scripts/e2e/simulate.py --list` لترى كل السيناريوهات، ثم `--only manual` لتتابع Check واحدة في السجل `logs/aias-local.log` خطوة بخطوة.

---

## 10. الحوكمة (لماذا توجد مجلدات governance؟)

الكود هنا مولَّد ومراجَع وفق "مصنع حوكمة": كل موديول له وثائق تحليل (متطلبات SRS، سكربت قاعدة بيانات، خطة تنفيذ، خطة اختبار) تحت `governance/shared/` (submodule من مستودع `aias`). القاعدة: **الكود يطابق الوثيقة، لا العكس**؛ وأي شيء وجدناه ناقصاً في الوثائق سُجّل كـ "فجوة" (gap) في `execution-state.json` ليجيب عنها المصنع. الأداة الوحيدة التي تكتب هذه الحالة هي `scripts/gov-module.py`.

للمبتدئ: لا تحتاج هذه المجلدات لتشغيل التطبيق، لكنها المرجع لفهم "لماذا" وراء كل قاعدة.

---

## 11. مسرد سريع

| المصطلح | المعنى |
|---|---|
| **Check** | عملية تحقق واحدة لطلب واحد مقابل إصدار خدمة واحد |
| **Service package** | مجلد الخدمة: `knowledge.md` + `service.yaml` |
| **Finding** | نتيجة شرط واحد: SATISFIED / NOT_SATISFIED / UNDETERMINED |
| **Overall Status** | الحكم العام على الـ Check |
| **Port / Adapter** | واجهة الاحتياج / تنفيذها الخارجي القابل للاستبدال |
| **Contract** | حزمة `contract` التي تنشرها كل موديول للآخرين |
| **MCP** | بروتوكول يستخدمه التطبيق لتنفيذ استعلامات القراءة عند الجهة |
| **ProblemDetail** | صيغة رد الخطأ الموحّدة (RFC 9457) مع `code` |
| **Tier / Data class** | تصنيف النموذج (FREE/APPROVED) وتصنيف البيانات (SYNTHETIC/REAL) — بوابة الأمان |
| **Flyway** | أداة ترحيل قاعدة البيانات؛ ملفات `V<N>__*.sql` لا تُعدَّل بعد تطبيقها |

---

## 12. من أين أبدأ القراءة في الكود؟

1. `src/main/java/io/agenticai/integration/controller/*` — نقاط الدخول الثماني.
2. `src/main/java/io/agenticai/chk/service/CheckPipeline.java` — الخطوات السبع للـ Check.
3. `src/main/java/io/agenticai/chk/adapter/SpringAiComparisonAdapter.java` — أول احتكاك بـ Spring AI.
4. `src/main/java/io/agenticai/chk/domain/FindingVerifier.java` — كيف لا نثق بالنموذج.
5. `src/main/java/io/agenticai/reg/service/RegistryLoadRun.java` — ماذا يحدث عند بدء التشغيل.
6. `src/main/java/io/agenticai/platform/mcp/SpringAiMcpQueryChannel.java` — قناة الاستعلامات.
