package com.kroot.y1themes;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** Local, user-confirmed updates of this package only. */
final class ApkUpdates {
    static final String STAGED_NAME = "pending-player-update.apk";
    private static final long MAX_BYTES = 256L * 1024 * 1024;
    static final class Failure extends Exception {
        Failure(String message) { super(message); }
    }
    static final class Prepared {
        final File file;
        final String name, currentVersion, version;
        Prepared(File file, String name, String currentVersion, String version) {
            this.file = file; this.name = name;
            this.currentVersion = currentVersion; this.version = version;
        }
    }
    static List<File> list(File root, File directory) throws Exception {
        String boundary = root.getCanonicalPath();
        if (!inside(boundary, directory.getCanonicalPath())) throw new Failure("更新フォルダを開けません");
        File[] entries = directory.listFiles();
        if (entries == null) throw new Failure("音楽領域を確認してください");
        ArrayList<File> result = new ArrayList<File>();
        for (File file : entries) {
            if (file.getName().startsWith(".") || !inside(boundary, file.getCanonicalPath())) continue;
            if (file.isDirectory() || file.isFile() && file.getName().toLowerCase(java.util.Locale.US).endsWith(".apk"))
                result.add(file);
        }
        Collections.sort(result, new Comparator<File>() {
            public int compare(File a, File b) {
                if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        return result;
    }
    private static boolean inside(String root, String path) {
        return path.equals(root) || path.startsWith(root + File.separator);
    }
    private static HashSet<String> signatures(Signature[] values) {
        HashSet<String> set = new HashSet<String>();
        if (values != null) for (Signature value : values) set.add(value.toCharsString());
        return set;
    }
    static Prepared prepare(Context context, File root, File source) throws Exception {
        if (!inside(root.getCanonicalPath(), source.getCanonicalPath()) || !source.isFile() ||
                !source.getName().toLowerCase(java.util.Locale.US).endsWith(".apk"))
            throw new Failure("APKを読み込めません");
        long expected = source.length();
        if (expected <= 0 || expected > MAX_BYTES) throw new Failure("APKのサイズが不正です");
        File staged = context.getFileStreamPath(STAGED_NAME);
        try {
            // Android 4.2's Package Installer reads a file URI under this directory.
            FileInputStream input = new FileInputStream(source);
            try {
                FileOutputStream output = context.openFileOutput(STAGED_NAME, Context.MODE_WORLD_READABLE);
                try {
                    byte[] buffer = new byte[65536]; int count; long copied = 0;
                    while ((count = input.read(buffer)) != -1) {
                        copied += count;
                        if (copied > MAX_BYTES) throw new Failure("APKのサイズが不正です");
                        output.write(buffer, 0, count);
                    }
                    output.getFD().sync();
                    if (copied != expected) throw new Failure("APKを読み込めません");
                } finally { output.close(); }
            } finally { input.close(); }
            PackageManager pm = context.getPackageManager();
            PackageInfo candidate = pm.getPackageArchiveInfo(staged.getAbsolutePath(), PackageManager.GET_SIGNATURES);
            if (candidate == null) throw new Failure("APKを読み込めません");
            if (!context.getPackageName().equals(candidate.packageName)) throw new Failure("このアプリの更新APKではありません");
            PackageInfo installed = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            HashSet<String> trusted = signatures(installed.signatures);
            if (trusted.isEmpty() || !trusted.equals(signatures(candidate.signatures))) throw new Failure("APKの署名が一致しません");
            if (candidate.versionCode < installed.versionCode) throw new Failure("古いバージョンには更新できません");
            return new Prepared(staged, source.getName(), installed.versionName, candidate.versionName);
        } catch (Exception failure) {
            staged.delete(); throw failure;
        }
    }
    interface InstallResult { void finished(int result); }
    static Object install(Context context, File file, final InstallResult result) throws Exception {
        if (context.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") !=
                PackageManager.PERMISSION_GRANTED) throw new Failure("アプリ更新には対応FWが必要です");
        final String descriptor = "android.content.pm.IPackageInstallObserver";
        final android.os.Binder binder = new android.os.Binder() {
            @Override protected boolean onTransact(int code, android.os.Parcel data,
                    android.os.Parcel reply, int flags) throws android.os.RemoteException {
                if (code == android.os.IBinder.INTERFACE_TRANSACTION) {
                    if (reply != null) reply.writeString(descriptor);
                    return true;
                }
                if (code == android.os.IBinder.FIRST_CALL_TRANSACTION) {
                    data.enforceInterface(descriptor); data.readString();
                    result.finished(data.readInt());
                    if (reply != null) reply.writeNoException();
                    return true;
                }
                return super.onTransact(code, data, reply, flags);
            }
        };
        Class<?> observerType = Class.forName(descriptor);
        Object observer = java.lang.reflect.Proxy.newProxyInstance(observerType.getClassLoader(),
                new Class<?>[]{observerType}, new java.lang.reflect.InvocationHandler() {
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        if (method.getName().equals("asBinder")) return binder;
                        if (method.getName().equals("toString")) return "PlayerInstallObserver";
                        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                        if (method.getName().equals("equals")) return proxy == args[0];
                        throw new UnsupportedOperationException(method.getName());
                    }
                });
        // REPLACE_EXISTING | INTERNAL. Never let the vendor installer choose SD storage.
        context.getPackageManager().getClass().getMethod("installPackage", android.net.Uri.class,
                observerType, int.class, String.class).invoke(context.getPackageManager(),
                android.net.Uri.fromFile(file), observer, 2 | 16, context.getPackageName());
        return observer;
    }

}
