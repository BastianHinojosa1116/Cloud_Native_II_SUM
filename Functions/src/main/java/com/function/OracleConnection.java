package com.function;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Base64;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class OracleConnection {

    private static String extractedWalletPath;

    public static Connection getConnection() throws Exception {
        String walletPath = getOrCreateWalletFolder();

        String alias = System.getenv("ORACLE_DB_ALIAS");
        String username = System.getenv("ORACLE_USERNAME");
        String password = System.getenv("ORACLE_PASSWORD");

        if (alias == null || username == null || password == null) {
            throw new RuntimeException("Faltan variables ORACLE_DB_ALIAS, ORACLE_USERNAME o ORACLE_PASSWORD");
        }

        String normalizedWalletPath = walletPath.replace("\\", "/");
        String url = "jdbc:oracle:thin:@" + alias + "?TNS_ADMIN=" + normalizedWalletPath;

        Properties props = new Properties();
        props.put("user", username);
        props.put("password", password);

        return DriverManager.getConnection(url, props);
    }

    private static synchronized String getOrCreateWalletFolder() throws Exception {
        if (extractedWalletPath != null) {
            return extractedWalletPath;
        }

        String walletBase64 = System.getenv("ORACLE_WALLET_BASE64");
        if (walletBase64 == null || walletBase64.isBlank()) {
            throw new RuntimeException("No existe la variable ORACLE_WALLET_BASE64");
        }

        Path tempDir = Files.createTempDirectory("oracle_wallet");
        Path zipPath = tempDir.resolve("wallet.zip");

        byte[] zipBytes = Base64.getDecoder().decode(walletBase64);
        Files.write(zipPath, zipBytes);

        unzip(zipPath.toString(), tempDir.toString());

        Path walletDir = tempDir.resolve("wallet");
        extractedWalletPath = walletDir.toAbsolutePath().toString();

        return extractedWalletPath;
    }

    private static void unzip(String zipFilePath, String destDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                Path newPath = Path.of(destDir, entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    if (newPath.getParent() != null) {
                        Files.createDirectories(newPath.getParent());
                    }

                    try (FileOutputStream fos = new FileOutputStream(newPath.toFile())) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }

                zis.closeEntry();
            }
        }
    }
}