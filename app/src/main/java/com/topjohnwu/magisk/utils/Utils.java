package com.topjohnwu.magisk.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.widget.Toast;

import com.topjohnwu.magisk.App;
import com.topjohnwu.magisk.Config;
import com.topjohnwu.magisk.Const;
import com.topjohnwu.magisk.container.Module;
import com.topjohnwu.magisk.container.ValueSortedMap;
import com.topjohnwu.net.Networking;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.internal.UiThreadHandler;
import com.topjohnwu.superuser.io.SuFile;

import java.util.Locale;
import java.util.Map;

public class Utils {

    public static void toast(CharSequence msg, int duration) {
        UiThreadHandler.run(() -> Toast.makeText(App.self, msg, duration).show());
    }

    public static void toast(int resId, int duration) {
        UiThreadHandler.run(() -> Toast.makeText(App.self, resId, duration).show());
    }

    public static String dlString(String url) {
        String s = Networking.get(url).execForString().getResult();
        return s == null ? "" : s;
    }

    public static int getPrefsInt(SharedPreferences prefs, String key, int def) {
        return Integer.parseInt(prefs.getString(key, String.valueOf(def)));
    }

    public static int getPrefsInt(SharedPreferences prefs, String key) {
        return getPrefsInt(prefs, key, 0);
    }

    public static String getNameFromUri(Context context, Uri uri) {
        String name = null;
        try (Cursor c = context.getContentResolver().query(uri, null, null, null, null)) {
            if (c != null) {
                int nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    c.moveToFirst();
                    name = c.getString(nameIndex);
                }
            }
        }
        if (name == null) {
            int idx = uri.getPath().lastIndexOf('/');
            name = uri.getPath().substring(idx + 1);
        }
        return name;
    }

    public static int dpInPx(int dp) {
        float scale = App.self.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5);
    }

    public static String fmt(String fmt, Object... args) {
        return String.format(Locale.US, fmt, args);
    }

    public static String getAppLabel(ApplicationInfo info, PackageManager pm) {
        try {
            if (info.labelRes > 0) {
                Resources res = pm.getResourcesForApplication(info);
                Configuration config = new Configuration();
                config.setLocale(LocaleManager.locale);
                res.updateConfiguration(config, res.getDisplayMetrics());
                return res.getString(info.labelRes);
            }
        } catch (Exception ignored) {}
        return info.loadLabel(pm).toString();
    }

    public static String getLegalFilename(CharSequence filename) {
        return filename.toString().replace(" ", "_").replace("'", "").replace("\"", "")
                .replace("$", "").replace("`", "").replace("*", "").replace("/", "_")
                .replace("#", "").replace("@", "").replace("\\", "_");
    }

    public static void loadModules() {
        Topic.reset(Topic.MODULE_LOAD_DONE);
        App.THREAD_POOL.execute(() -> {
            Map<String, Module> moduleMap = new ValueSortedMap<>();
            SuFile path = new SuFile(Const.MAGISK_PATH);
            SuFile[] modules = path.listFiles(
                    (file, name) -> !name.equals("lost+found") && !name.equals(".core"));
            for (SuFile file : modules) {
                if (file.isFile()) continue;
                Module module = new Module(Const.MAGISK_PATH + "/" + file.getName());
                moduleMap.put(module.getId(), module);
            }
            Topic.publish(Topic.MODULE_LOAD_DONE, moduleMap);
        });
    }

    public static boolean showSuperUser() {
        return Shell.rootAccess() && (Const.USER_ID == 0 ||
                (int) Config.get(Config.Key.SU_MULTIUSER_MODE) !=
                        Config.Value.MULTIUSER_MODE_OWNER_MANAGED);
    }

    public static void reboot() {
        Shell.su("/system/bin/reboot" + (Config.recovery ? " recovery" : "")).submit();
    }
}
