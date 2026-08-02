# گزارش فاز ۳ — تولید کد (Code Generation با jasmin)

> **این زیپ حالا هر سه فاز را با هم دارد.** کدهای فاز ۲ که فرستادی
> (`NameAnalyzer` / `TypeAnalyzer` / `TypeUtils` / `symbolTable/` / `samples/` /
> `REPORT_phase2.md` / `run_phase2_tests.sh`) داخل پروژه‌ی فاز ۳ ادغام شدند.
> بقیه‌ی فایل‌های AST و گرامر در دو زیپ **کاملاً یکسان** بودند، پس ادغام بدون
> هیچ تداخلی انجام شد؛ تنها فایل مشترکی که فرق داشت `SimpleLang.java` بود که
> بازنویسی شد تا هر سه فاز را پشت سر هم اجرا کند.

## وضعیت
پیاده‌سازی کامل است و **واقعاً کامپایل، اسمبل و اجرا شده**. برخلاف فاز ۲، این بار
توانستم در محیط خودم JDK نصب کنم، پس:

- کل پروژه (فاز ۲ + فاز ۳) با `javac` کامپایل شد؛ بدون خطا و بدون warning.
- `run_phase2_tests.sh` خودت اجرا شد → **۱۱ از ۱۱ PASS** (شامل چک فایل
  `unreachable_optimized.mol`). یعنی فاز ۲ که قبلاً فقط دستی ردگیری شده بود،
  حالا واقعاً تست شده است.
- `Sample/sample.mol` پردازش شد و فایل‌های `.j` تولیدشده با `jasmin.jar` اسمبل و
  با `java Main` اجرا شدند → خروجی `20` و `20` (درست).
- خروجی تولیدشده با فایل‌های مرجع `Sample/Main.j` و `Sample/BaseModule.j`
  **دیف** شد؛ تنها اختلاف‌ها: `iload 1` به‌جای `iload_1` و یک newline انتهایی
  (هر دو معادل و مجازند — خودِ فایل مرجع هم ناسازگار است: در `Main.j` از فرم
  بلند `iload 1` و در `BaseModule.j` از فرم کوتاه `iload_1` استفاده کرده).
- یک برنامه‌ی تست سنگین (`Sample/stress.mol`) نوشتم که struct، ارث‌بری،
  while/for، break/continue، bool، float/double/char، فیلد شیء و `this.field` را
  پوشش می‌دهد؛ اسمبل شد و خروجی‌اش کاملاً درست بود.

## خط لوله‌ی جدید در `SimpleLang.java`
```
parse → NameAnalyzer → TypeAnalyzer → چاپ خطاها → writeOptimizedSource
      → اگر هیچ خطایی نبود: CodeGenerator
```
یعنی برنامه‌ی معیوب همان خروجی فاز ۲ را می‌دهد و کدی تولید نمی‌شود، و برنامه‌ی
سالم چیزی چاپ نمی‌کند و فقط `.j` تولید می‌شود. (تست شد: روی
`samples/Type/typeMismatch.mol` هیچ فایلی ساخته نمی‌شود.)

## دو اصلاح لازم در `NameAnalyzer` (فاز ۲)
موقع اجرای کامل معلوم شد فاز ۲ روی سمپل رسمی خودِ فاز ۳ خطای اشتباه می‌داد:
`Line 14 : BaseModule not declared`. علتش این است که گرامر در قاعده‌ی `expr`
آلترناتیو `methodcall` را قبل از `cons` گذاشته، پس
`BaseModule base = BaseModule();` یک `MethodCall` بدون instance به نام
`BaseModule` می‌سازد، نه `ConstructorCall`. دو جای `NameAnalyzer` اصلاح شد:

1. `handleMethodCall` — اگر فراخوانی بدون پیشوند باشد و نامش یک ماژول/struct
   اعلام‌شده باشد، نمونه‌سازی است و نباید `not declared` بدهد.
2. جمع‌کننده‌ی ارجاعات در تحلیل reachability — همان الگو حالا به‌عنوان ارجاع به
   آن تایپ شمرده می‌شود، وگرنه `Point` در تست من به‌اشتباه unreachable و از AST
   حذف می‌شد.

هر ۱۱ نمونه‌ی فاز ۲ بعد از این تغییر هم PASS می‌دهند (هیچ‌کدام این الگو را
نداشتند). بقیه‌ی کد فاز ۲ دست‌نخورده است.



## چه چیزی تغییر کرد
- `src/main/visitor/CodeGenerator.java` — پیاده‌سازی کامل (جایگزین تمپلیت خالی).
- `src/SimpleLang.java` — خط لوله‌ی هر سه فاز.
- `src/main/visitor/NameAnalyzer.java` — دو اصلاح توضیح‌داده‌شده‌ی بالا.

فایل‌های افزوده‌شده:

- `run_phase3.sh` — کامپایل + تولید کد + اسمبل + اجرا.
- `Sample/stress.mol` — برنامه‌ی تست پوشش‌دهنده‌ی امکانات زبان.
- `REPORT_phase3.md` — همین فایل.

از زیپ فاز ۲ منتقل شدند (بدون تغییر): `src/main/symbolTable/`،
`TypeAnalyzer.java`، `TypeUtils.java`، `samples/`، `REPORT_phase2.md`،
`run_phase2_tests.sh`.

## رعایت قوانین پروژه
- ✅ فقط Visitor؛ هیچ Listenerی استفاده نشده.
- ✅ تشخیص نوع نود فقط با `instanceof` است. تنها enumهای استفاده‌شده
  `BinaryOperator`، `UnaryOperator`، `PrimitiveType.Primitive` و
  `UnaryOpExpr.Operator` هستند که enum عملیاتی‌اند نه enum تشخیص نوع نود.
- ✅ `import main.ast.declarations.Module;` به‌صورت صریح زده شده (تله‌ی
  `java.lang.Module`).
- ✅ `.limit stack 128` و `.limit locals 128` در همه‌ی متدها.
- ✅ هر دستور دقیقاً روی یک خط (متد `emit` همیشه `\n` می‌گذارد).

## معماری تولیدشده

### ۱. یک فایل `.j` به ازای هر ماژول/struct
خروجی در `./codeGenOutput/` نوشته می‌شود: `Main.j`، `BaseModule.j`، `Point.j`، …
- `module A includes B` → `.class public A` / `.super B`
- ماژول/struct بدون include → `.super java/lang/Object`
- ماژول‌های include‌شده **قبل از** ماژولی که آن‌ها را include کرده تولید می‌شوند
  (مرتب‌سازی توپولوژیک در `orderedClasses`).
- JVM چند-ارث‌بری ندارد؛ اگر یک ماژول بیش از یک include داشته باشد، اولی
  superclass می‌شود و برای بقیه یک خط کامنت `; note: ...` در فایل درج می‌شود
  (کامنت jasmin، روی اسمبل اثری ندارد).

### ۲. اعضا
- `VarDecl` → `.field public <name> <descriptor>`
- `MethodDecl` → `.method public <name>(<args>)<ret>`
- برای هر کلاس یک `<init>()V` تولید می‌شود که `super.<init>()` را صدا می‌زند.
- کلاسی که متد `main` بدون پارامتر دارد، علاوه بر متد نمونه‌ای `main()V`، یک
  `public static main([Ljava/lang/String;)V` هم می‌گیرد که یک instance می‌سازد و
  `main()` را صدا می‌زند (دقیقاً مثل تمپلیت مرجع).

### ۳. `slotOf`
```java
public int slotOf(String varName)
```
slot صفر همیشه متعلق به خودِ کلاس (`this`) است، بعد آرگومان‌های متد به‌ترتیب
اضافه می‌شوند، و بعد متغیرهای محلی به‌ترتیب اولین برخورد. اگر نامی موجود نباشد
اضافه و slot جدیدش برگردانده می‌شود. در ابتدای هر `visit(Method)` جدول
`slots`/`localTypes` خالی و `nextSlot` صفر می‌شود.
نکته: `double` دو slot می‌گیرد، پس شمارنده برای `D` دوتا جلو می‌رود.

### ۴. انتخاب دستور بر اساس نوع
هر `visit` روی یک expression، **descriptor مقداری که روی stack گذاشته** را
برمی‌گرداند (`I`, `Z`, `C`, `F`, `D`, `LFoo;`). از روی آن انتخاب می‌شود:

| نوع | load | store | حساب | return |
|---|---|---|---|---|
| int/bool/char | `iload` | `istore` | `iadd`… | `ireturn` |
| float | `fload` | `fstore` | `fadd`… | `freturn` |
| double | `dload` | `dstore` | `dadd`… | `dreturn` |
| شیء | `aload` | `astore` | — | `areturn` |

یک `inferType` جدا (بدون تولید کد) هم هست، چون برای BinaryExpression باید *قبل*
از visit کردن عملوندها نوع مشترک را بدانیم تا در جای درست `i2d`/`f2d`/… بگذاریم.

### ۵. دستورات
- **BinaryExpression**: اول عملوند چپ، بعد راست روی stack، سپس عملگر. برای
  مقایسه‌ها با label نتیجه‌ی `0/1` ساخته می‌شود (`if_icmplt` برای int،
  `dcmpl`/`fcmpl` + `iflt` برای اعشاری، `if_acmpeq` برای مرجع).
- **and/or**: کوتاه‌مدار (short-circuit) با پرش.
- **UnaryExpression**: `not` → `ldc 1` + `ixor`؛ `-` → `ineg`/`fneg`/`dneg`.
- **if**: `ifeq` به `else`/`endif`، `goto endif` بعد از then.
- **while**: `Lstart: cond; ifeq Lend; body; goto Lstart; Lend:`
- **for**: initializer‌ها، `Lcond`، شرط، بدنه، `Lupdate` (updaterها)، `goto Lcond`.
  `break` → `Lend`، `continue` → `Lupdate` (با دو استک از labelها، پس حلقه‌های
  تودرتو درست کار می‌کنند).
- **output** → `getstatic java/lang/System/out ...` + `println(<desc>)V` (برای
  شیء `println(Ljava/lang/Object;)V`).
- **input** → ساخت `java/util/Scanner` و `nextInt/nextFloat/nextDouble/nextBoolean`
  یا `next().charAt(0)` بسته به نوع مقصد.
- **مقداردهی به فیلد** (`this.x = e` یا `obj.x = e`): اول ارجاع شیء، بعد مقدار،
  بعد `putfield` — ترتیب استک درست رعایت شده (`prepareStore` / `finishStore`).

### ۶. فراخوانی‌ها
- بدون پیشوند یا `this.foo(...)` → `aload_0` + `invokevirtual <CurrentClass>/...`
  (مثل تمپلیت مرجع که برای متدِ به‌ارث‌رسیده هم از نام کلاس فرزند استفاده کرده؛
  JVM خودش در superclass دنبالش می‌گردد).
- `obj.foo(...)` → بارگذاری `obj`، آرگومان‌ها، `invokevirtual <StaticType>/...`
- امضای فراخوانی از روی امضای متدِ resolve‌شده ساخته می‌شود (نه از روی نوع
  آرگومان‌ها) تا با `.method` مقصد دقیقاً یکی باشد.

## دو نکته‌ی ظریف که موقع تست واقعی پیدا شد
۱. **`BaseModule base = BaseModule();`** توسط گرامر به `ConstructorCall` تبدیل
   *نمی‌شود*؛ چون در قاعده‌ی `expr` آلترناتیو `methodcall` قبل از `cons` آمده،
   این یک `MethodCall` بدون instance با نام `BaseModule` است. پس در
   `visit(MethodCall)` اگر فراخوانی بدون پیشوند باشد و نامش با یک ماژول/struct
   اعلام‌شده یکی باشد، به‌عنوان **نمونه‌سازی** تولید می‌شود
   (`new`/`dup`/`invokespecial <init>()V`) — دقیقاً همان چیزی که فایل مرجع
   انتظار دارد.

۲. **label انتهای متد**: اگر بدنه با `if/else`ی تمام شود که هر دو شاخه‌اش
   `return` دارند، آخرین خط تولیدشده یک label است و JVM خطای
   `Illegal target of jump or branch` می‌دهد. راه‌حل: قبل از `.end method` نگاه
   می‌کنیم آخرین خطِ تولیدشده واقعاً یک دستور return هست یا نه؛ اگر نه، یک return
   پیش‌فرض اضافه می‌شود. این هم دقیقاً همان رفتار فایل مرجع را می‌دهد (در
   `BaseModule.process` بعد از `ireturn` چیزی اضافه نمی‌شود) و هم کد را معتبر
   نگه می‌دارد.

## نکات و فرض‌ها
- MOL نحو اعلان سازنده ندارد. اگر یک `ConstructorCall` آرگومان داشته باشد و آن
  تایپ متدی به نام `init` داشته باشد، بعد از `<init>()V` آن `init` صدا زده
  می‌شود (سازگار با الگوی `myPacket.init()` که در فاز ۲ دیده بودیم). در غیر این
  صورت آرگومان‌ها نادیده گرفته می‌شوند.
- `mut` و ownership طبق صورت‌پروژه در این فاز هیچ اثری در خروجی ندارند.
- این `CodeGenerator` ایندکس کلاس/فیلد/متد خودش را از AST می‌سازد و مستقیم به
  symbol table فاز ۲ وابسته نیست. دلیلش این بود که ابتدا فقط تمپلیت خام فاز ۳ را
  داشتم. حالا که کد فاز ۲ کنارش است، هر دو در یک اجرا کار می‌کنند و هیچ تداخلی
  ندارند. اگر استاد صریحاً بخواهد که کدجن از خودِ symbol table فاز ۲ بخواند،
  بگو تا `memberIndex`/`superIndex` را با `nameAnalyzer.modules`/`structs` و
  `lookupMember` جایگزین کنم (تغییر کوچکی است و رفتار عوض نمی‌شود).
- **`break`/`continue` در MOL بدون `;` هستند** (طبق گرامر، `jumpStmt` سمی‌کالن
  نمی‌گیرد) — موقع نوشتن تست خودم به این برخوردم.

## نحوه‌ی تست
```bash
# فاز ۲ (تحلیل نام و تایپ) — ۱۱ نمونه
bash run_phase2_tests.sh

# فاز ۳ (تولید کد)
chmod +x run_phase3.sh
./run_phase3.sh Sample/sample.mol
./run_phase3.sh Sample/stress.mol
```
اگر `jasmin.jar` را کنار پروژه بگذاری، اسکریپت خودش اسمبل و اجرا هم می‌کند.
دستی:
```bash
javac -cp utilities/antlr-4.13.1-complete.jar -d build_classes $(find src gen -name "*.java")
java  -cp build_classes:utilities/antlr-4.13.1-complete.jar SimpleLang Sample/sample.mol
cd codeGenOutput && java -jar jasmin.jar *.j && java Main
```

## خروجی مورد انتظار تست‌ها
- `Sample/sample.mol` → `20` و `20`
- `Sample/stress.mol` → `10, 0, 1, 2, 4, 5, 12, 9, true, 5.0, 1.5, A, 2, -10, true`
