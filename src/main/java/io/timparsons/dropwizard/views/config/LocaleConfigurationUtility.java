package io.timparsons.dropwizard.views.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.Tables;

import io.timparsons.dropwizard.views.config.LocaleMap.LocaleMapValue;

public class LocaleConfigurationUtility {

    /**
     * Given a directory of locale property files, parse the documents, and hold
     * them in memory.
     * 
     * @param directory
     *            where the locale property files are located
     */
    public static Table<String, String, LocaleMap> getLocaleFiles(String directory) {
        Builder<String, String, LocaleMap> tableBuilder = new ImmutableTable.Builder<>();
        File localeDirectory = new File(directory);

        Table<String, String, LocaleMap> localeTable = null;
        if (localeDirectory.isDirectory()) {
            try {
                for (File file : localeDirectory.listFiles()) {
                    if (file.isFile()) {
                        tableBuilder.put(getLocaleFile(file));
                    } else if (file.isDirectory()) {
                        Locale locale = Locale.forLanguageTag(file.getName());
                        if (locale != null) {
                            for (File localeFile : file.listFiles()) {
                                tableBuilder.put(getLocaleFile(localeFile, locale));
                            }
                        }
                    }
                }

                localeTable = tableBuilder.build();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("locale must be a directory");
        }
        
        return localeTable;
    }

    private static Cell<String, String, LocaleMap> getLocaleFile(File localeFile)
            throws FileNotFoundException, IOException {

        Locale locale = Locale.getDefault();
        String fileName = localeFile.getName().substring(0, localeFile.getName().lastIndexOf("."));
        String[] fileParts = fileName.split("_");
        if (fileParts.length > 1) {
            locale = Locale.forLanguageTag(fileParts[fileParts.length - 1]);
        }

        if (locale == null) {
            locale = Locale.getDefault();
        }

        return getLocaleFile(localeFile, locale);
    }

    private static Cell<String, String, LocaleMap> getLocaleFile(File localeFile, Locale locale)
            throws FileNotFoundException, IOException {
        Properties props = new Properties();
        LocaleMap.Builder propMapBuilder = LocaleMap.builder();
        props.load(new FileInputStream(localeFile));

        String[] fileParts = localeFile.getName().split("_");
        StringBuilder fileKey = new StringBuilder();
        if (fileParts.length > 1) {
            for (int i = 0; i < fileParts.length - 1; i++) {
                fileKey.append(fileParts[i]);
                if ((i + 1) < (fileParts.length - 1)) {
                    fileKey.append("_");
                }
            }
        } else {
            fileKey.append(fileParts[0]);
        }

        for (Entry<Object, Object> prop : props.entrySet()) {
            propMapBuilder.put((String) prop.getKey(), new LocaleMapValue((String) prop.getValue()));
        }

        return Tables.immutableCell(locale.getLanguage(), fileKey.toString(), propMapBuilder.build());
    }
}
