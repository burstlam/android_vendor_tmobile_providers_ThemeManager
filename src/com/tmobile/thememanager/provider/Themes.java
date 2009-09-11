package com.tmobile.thememanager.provider;

import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.utils.FileUtilities;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ThemeInfo;
import android.database.Cursor;
import android.net.Uri;

public class Themes {
    public static final String AUTHORITY = "com.tmobile.thememanager.themes";

    public static final Uri CONTENT_URI =
        Uri.parse("content://" + AUTHORITY);

    private Themes() {}

    public static Uri getThemeUri(Context context, String packageName, String themeId) {
        return ThemeColumns.CONTENT_URI.buildUpon()
            .appendPath(packageName)
            .appendPath(themeId).build();
    }

    public static Cursor listThemes(Context context) {
        return context.getContentResolver().query(ThemeColumns.CONTENT_PLURAL_URI,
                null, null, null, null);
    }

    public static Cursor listThemesByPackage(Context context, String packageName) {
        return context.getContentResolver().query(ThemeColumns.CONTENT_PLURAL_URI,
                null, ThemeColumns.THEME_PACKAGE + " = ?",
                new String[] { packageName }, null);
    }

    public static Cursor getAppliedTheme(Context context) {
        return context.getContentResolver().query(ThemeColumns.CONTENT_PLURAL_URI,
                null, ThemeColumns.IS_APPLIED + "=1", null, null);
    }

    private static void populateContentValues(Context context, ContentValues outValues,
            PackageInfo pi, ThemeInfo ti) {
        outValues.put(ThemeColumns.THEME_ID, ti.themeId);
        outValues.put(ThemeColumns.THEME_PACKAGE, pi.packageName);
        outValues.put(ThemeColumns.NAME, ti.name);
        outValues.put(ThemeColumns.STYLE_NAME,
                ti.themeStyleName != null ? ti.themeStyleName : ti.name);
        outValues.put(ThemeColumns.AUTHOR, ti.author);
        outValues.put(ThemeColumns.IS_DRM, ti.isDrmProtected);
        outValues.put(ThemeColumns.IS_SYSTEM,
                (pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
        if (ti.wallpaperImageName != null) {
            Uri wallpaperUri = PackageResources.getImageUri(context,
                    pi.packageName, ti.wallpaperImageName);
            if (wallpaperUri != null) {
                String filename = FileUtilities.basename(ti.wallpaperImageName);
                outValues.put(ThemeColumns.WALLPAPER_NAME,
                        FileUtilities.removeExtension(filename));
                outValues.put(ThemeColumns.WALLPAPER_URI, wallpaperUri.toString());
            }
        }
        if (ti.ringtoneFileName != null) {
            Uri ringtoneUri = PackageResources.getRingtoneUri(context,
                    pi.packageName, ti.ringtoneFileName);
            if (ringtoneUri != null) {
                outValues.put(ThemeColumns.RINGTONE_NAME, ti.ringtoneName);
                outValues.put(ThemeColumns.RINGTONE_URI, ringtoneUri.toString());
            }
        }
        if (ti.notificationRingtoneFileName != null) {
            Uri notifRingtoneUri = PackageResources.getRingtoneUri(context,
                    pi.packageName, ti.notificationRingtoneFileName);
            if (notifRingtoneUri != null) {
                outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_NAME, ti.notificationRingtoneName);
                outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_URI, notifRingtoneUri.toString());
            }
        }
        if (ti.thumbnail != null) {
            Uri thumbnailUri = PackageResources.getImageUri(context,
                    pi.packageName, ti.thumbnail);
            if (thumbnailUri != null) {
                outValues.put(ThemeColumns.THUMBNAIL_URI, thumbnailUri.toString());
            }
        }
    }

    public static Uri insertTheme(Context context, PackageInfo pi, ThemeInfo ti,
            boolean overwrite) {
        return insertTheme(context, pi, ti, overwrite, false);
    }

    public static Uri insertTheme(Context context, PackageInfo pi, ThemeInfo ti,
            boolean overwrite, boolean isCurrentTheme) {
        Uri existingUri = getThemeUri(context, pi.packageName, ti.themeId);
        Cursor c = context.getContentResolver().query(existingUri,
                new String[] { ThemeColumns._ID }, null, null, null);
        if (c == null) {
            return null;
        }
        int count;
        try {
            count = c.getCount();
        } finally {
            c.close();
        }
        ContentValues values = new ContentValues();
        populateContentValues(context, values, pi, ti);
        values.put(ThemeColumns.IS_APPLIED, isCurrentTheme ? 1 : 0);
        if (count == 0) {
            return context.getContentResolver().insert(ThemeColumns.CONTENT_PLURAL_URI, values);
        } else if (overwrite && count == 1) {
            context.getContentResolver().update(existingUri, values, null, null);
            return existingUri;
        }
        return null;
    }

    public static void deleteTheme(Context context, String packageName,
            String themeId) {
        context.getContentResolver().delete(
                ThemeColumns.CONTENT_PLURAL_URI, ThemeColumns.THEME_PACKAGE + " = ? AND " +
                    ThemeColumns.THEME_ID + " = ?",
                new String[] { packageName, themeId });
    }

    public static void deleteThemesByPackage(Context context, String packageName) {
        context.getContentResolver().delete(
                ThemeColumns.CONTENT_PLURAL_URI, ThemeColumns.THEME_PACKAGE + " = ?",
                new String[] { packageName });
    }

    public static void markAppliedTheme(Context context, String packageName, String themeId) {
        ContentValues values = new ContentValues();
        values.put(ThemeColumns.IS_APPLIED, 0);
        context.getContentResolver().update(ThemeColumns.CONTENT_PLURAL_URI, values, null, null);
        values.put(ThemeColumns.IS_APPLIED, 1);
        context.getContentResolver().update(ThemeColumns.CONTENT_PLURAL_URI, values, 
                ThemeColumns.THEME_PACKAGE + " = ? AND " +
                    ThemeColumns.THEME_ID + " = ?",
                new String[] { packageName, themeId });
    }

    /**
     * Request a theme change by broadcasting to the ThemeManager. Must hold
     * permission {@link ThemeManager#PERMISSION_CHANGE_THEME}.
     */
    public static void changeTheme(Context context, Uri themeUri) {
        context.sendOrderedBroadcast(new Intent(ThemeManager.ACTION_CHANGE_THEME, themeUri), 
                Manifest.permission.CHANGE_CONFIGURATION);
    }

    public interface ThemeColumns {
        public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/theme");

        public static final Uri CONTENT_PLURAL_URI =
            Uri.parse("content://" + AUTHORITY + "/themes");
        
        public static final String CONTENT_TYPE = "vnd.tmobile.cursor.dir/theme";
        public static final String CONTENT_ITEM_TYPE = "vnd.tmobile.cursor.item/theme";
        
        public static final String STYLE_CONTENT_TYPE = "vnd.tmobile.cursor.dir/style";
        public static final String STYLE_CONTENT_ITEM_TYPE = "vnd.tmobile.cursor.item/style";

        public static final String _ID = "_id";
        public static final String THEME_ID = "theme_id";
        public static final String THEME_PACKAGE = "theme_package";

        public static final String IS_APPLIED = "is_applied";

        public static final String NAME = "name";
        public static final String STYLE_NAME = "style_name";
        public static final String AUTHOR = "author";
        public static final String IS_DRM = "is_drm";

        public static final String WALLPAPER_NAME = "wallpaper_name";
        public static final String WALLPAPER_URI = "wallpaper_uri";

        public static final String RINGTONE_NAME = "ringtone_name";
        public static final String RINGTONE_URI = "ringtone_uri";
        public static final String NOTIFICATION_RINGTONE_NAME = "notif_ringtone_name";
        public static final String NOTIFICATION_RINGTONE_URI = "notif_ringtone_uri";

        public static final String THUMBNAIL_URI = "thumbnail_uri";

        public static final String IS_SYSTEM = "system";
    }
}