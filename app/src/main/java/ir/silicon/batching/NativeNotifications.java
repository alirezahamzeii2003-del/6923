package ir.silicon.batching;

import android.app.*;
import android.content.*;
import android.os.Build;

public final class NativeNotifications {
    public static final String CHANNEL = "silicon_reports";
    private NativeNotifications(){}

    public static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm.getNotificationChannel(CHANNEL) == null)
                nm.createNotificationChannel(new NotificationChannel(CHANNEL, "گزارش‌های بچینگ", NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    public static void showDraft(Context c) {
        if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) return;
        ensureChannel(c);
        Intent i = new Intent(c, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 41, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_menu_edit)
         .setContentTitle("گزارش بچینگ هنوز تایید نهایی نشده")
         .setContentText("اطلاعات ذخیره شده‌اند؛ قبل از تایید نهایی حذف نمی‌شوند.")
         .setContentIntent(pi).setAutoCancel(true);
        c.getSystemService(NotificationManager.class).notify(4101, b.build());
    }

    public static void showError(Context c, String msg) {
        ensureChannel(c);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("Beaching").setContentText(msg).setAutoCancel(true);
        c.getSystemService(NotificationManager.class).notify(4102, b.build());
    }
}
