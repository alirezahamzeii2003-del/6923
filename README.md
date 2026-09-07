# بچینگ آجر سیلیکان — نسخه Android / GitHub

این مخزن نسخه Android برنامه را از فایل HTML اصلی داخل یک WebView بومی Android بسته‌بندی می‌کند.

## قابلیت‌های اصلی
- ذخیره محلی اطلاعات کاری.
- ذخیره خودکار هنگام خروج از صفحه، رفتن برنامه به پس‌زمینه و `pagehide`.
- گزارش PDF و پیش‌نمایش چندصفحه‌ای.
- اشتراک‌گذاری واقعی PDF و فایل پشتیبان JSON از طریق Android Share Sheet.
- پشتیبان‌گیری قابل ویرایش و بازیابی با تأیید کاربر.
- تاریخچه گزارش‌های نهایی.
- تأیید نهایی قبل از صفر شدن فرم گزارش جاری.
- اگر ذخیره نهایی یا راستی‌آزمایی ذخیره شکست بخورد، اطلاعات گزارش جاری پاک نمی‌شود.
- پس از تأیید نهایی، فرم کاری صفر می‌شود و گزارش در تاریخچه باقی می‌ماند.
- اعلان Android برای گزارش نیمه‌کاره پس از راه‌اندازی مجدد گوشی، تا زمانی که گزارش تأیید نهایی نشده باشد.
- GitHub Actions برای ساخت خودکار APK.

## ساخت APK در GitHub
Workflow زیر بدون نیاز به قرار دادن فایل APK داخل مخزن، APK را به‌صورت Artifact می‌سازد:

`.github/workflows/android-apk.yml`

از مسیر **Actions → Android APK → Run workflow** اجرا کنید. خروجی در بخش Artifacts قابل دریافت است.

## نکته مهم
هیچ اپلیکیشنی نمی‌تواند جلوی حذف خود برنامه توسط کاربر یا سیستم‌عامل را بگیرد. این پروژه تضمین می‌کند که خود برنامه قبل از «تأیید نهایی گزارش» داده کاری را با دستور داخلی صفر/حذف نکند و ذخیره نهایی ابتدا راستی‌آزمایی شود.

## ساختار
- `app/src/main/assets/index.html` — رابط و منطق برنامه.
- `app/src/main/java/.../MainActivity.java` — WebView و اشتراک‌گذاری native.
- `app/src/main/java/.../BootReceiver.java` — اعلان پس از روشن شدن گوشی.
- `app/src/main/java/.../NativeNotifications.java` — کانال اعلان‌ها.
- `app/src/main/res/xml/file_paths.xml` — FileProvider برای اشتراک فایل.

## Beaching – GitHub acceptance rules

- **Application name:** `Beaching`
- **Application icon:** the supplied final control-room/factory icon with operator and yellow accents must be used as the Android launcher icon and the in-app brand icon. Do not replace it with a generic icon.
- A disabled reactor must not be selectable for new batch registration; re-enabling it restores registration.
- Manual batch/sample times stay exactly as entered. The current-time button is the only action that inserts the current time. Typing `1240` or Persian digits produces `12:40`; an empty field remains empty.
- Handover status uses one green check only; pending status is yellow and says `در انتظار تایید اپراتور`.
- Page refreshes/re-renders must preserve the current scroll position when staying on the same page.
- JSON editable files use the shift/date filename format `Sifte A | 1405/06/01.json` (with filesystem-invalid characters except `|` sanitized only when required).
- Opening a JSON editable file from the Android file manager must offer **Beaching** as an app choice and, after selection, ask whether to load it for editing. Cancel must leave current data unchanged.
- Editable-file share text contains shift, date and shift turn.
- Handover description is optional; when present in PDF sharing text it is on a new line after `توضیحات :`, while it is not printed inside the PDF.
- Long-press text selection/context menus are disabled on app chrome; form fields remain editable.
- Pro can be activated from the logo only and accepts both license codes `AlirezaHamzeii6923` and `Hamzeii6923`.
- Exit confirms that saved information remains and then closes the Android task without clearing saved data.

## UI / identity rules for this release
- **Application name:** Beaching
- **Launcher icon:** `app/src/main/res/drawable/ic_beaching_launcher.png` (the new control-room icon).
- **In-app logo and PDF logo:** MUST remain the original factory logo at `app/src/main/assets/original_logo.png`; the launcher icon must never replace this logo.
- **Quick access:** the bottom navigation includes a dedicated **ثبت بچ** button/icon for fast batch entry.
- **Digits:** Persian/Arabic-Indic digits entered by the operator are immediately converted to Latin/English digits. Rendered application text, form values, stored numeric values, dates/times, and PDF numeric output use English digits.
