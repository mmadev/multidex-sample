package com.github.mmadev.multidexsample.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexFile;

public class MultiDexUtils {
    private static final String EXTRACTED_NAME_EXT = ".classes";
    private static final String EXTRACTED_SUFFIX = ".zip";

    private static final String SECONDARY_FOLDER_NAME = "code_cache" + File.separator +
            "secondary-dexes";

    private static final String PREFS_FILE = "multidex.version";
    private static final String KEY_DEX_NUMBER = "dex.number";

    private SharedPreferences getMultiDexPreferences(Context context) {
        return context.getSharedPreferences(PREFS_FILE,
                Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                        ? Context.MODE_PRIVATE
                        : Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
    }

    /**
     * get all the dex path
     *
     * @param context the application context
     * @return all the dex path
     * @throws PackageManager.NameNotFoundException
     * @throws IOException
     */
    public List<String> getSourcePaths(Context context) throws PackageManager.NameNotFoundException, IOException {
        final ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
        final File sourceApk = new File(applicationInfo.sourceDir);
        final File dexDir = new File(applicationInfo.dataDir, SECONDARY_FOLDER_NAME);

        final List<String> sourcePaths = new ArrayList<>();
        sourcePaths.add(applicationInfo.sourceDir); //add the default apk path

        //the prefix of extracted file, ie: test.classes
        final String extractedFilePrefix = sourceApk.getName() + EXTRACTED_NAME_EXT;
        //the total dex numbers
        final int totalDexNumber = getMultiDexPreferences(context).getInt(KEY_DEX_NUMBER, 1);

        for (int secondaryNumber = 2; secondaryNumber <= totalDexNumber; secondaryNumber++) {
            //for each dex file, ie: test.classes2.zip, test.classes3.zip...
            final String fileName = extractedFilePrefix + secondaryNumber + EXTRACTED_SUFFIX;
            final File extractedFile = new File(dexDir, fileName);
            if (extractedFile.isFile()) {
                sourcePaths.add(extractedFile.getAbsolutePath());
                //we ignore the verify zip part
            } else {
                throw new IOException("Missing extracted secondary dex file '" +
                        extractedFile.getPath() + "'");
            }
        }

        return sourcePaths;
    }

    /**
     * get all the external classes name in "classes2.dex", "classes3.dex" ....
     *
     * @param context the application context
     * @return all the classes name in the external dex
     * @throws PackageManager.NameNotFoundException
     * @throws IOException
     */
    public List<String> getExternalDexClasses(Context context) throws PackageManager.NameNotFoundException, IOException {
        final List<String> paths = getSourcePaths(context);
        if(paths.size() <= 1) {
            // no external dex
            return null;
        }
        // the first element is the main dex, remove it.
        paths.remove(0);
        final List<String> classNames = new ArrayList<>();
        for (String path : paths) {
            try {
                DexFile dexfile = null;
                if (path.endsWith(EXTRACTED_SUFFIX)) {
                    //NOT use new DexFile(path), because it will throw "permission error in /data/dalvik-cache"
                    dexfile = DexFile.loadDex(path, path + ".tmp", 0);
                } else {
                    dexfile = new DexFile(path);
                }
                final Enumeration<String> dexEntries = dexfile.entries();
                while (dexEntries.hasMoreElements()) {
                    classNames.add(dexEntries.nextElement());
                }
            } catch (IOException e) {
                throw new IOException("Error at loading dex file '" +
                        path + "'");
            }
        }
        return classNames;
    }

    /**
     * Get all loaded external classes name in "classes2.dex", "classes3.dex" ....
     * @param context
     * @return get all loaded external classes
     */
    public List<String> getLoadedExternalDexClasses(Context context) {
        try {
            final List<String> externalDexClasses = getExternalDexClasses(context);
            if (externalDexClasses != null && !externalDexClasses.isEmpty()) {
                final ArrayList<String> classList = new ArrayList<>();
                final java.lang.reflect.Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[]{String.class});
                m.setAccessible(true);
                final ClassLoader cl = context.getClassLoader();
                for (String clazz : externalDexClasses) {
                    if (m.invoke(cl, clazz) != null) {
                        classList.add(clazz.replaceAll("\\.", "/").replaceAll("$", ".class"));
                    }
                }
                return classList;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}