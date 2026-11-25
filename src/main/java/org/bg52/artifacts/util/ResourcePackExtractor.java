package org.bg52.artifacts.util;

import org.bg52.artifacts.Artifacts;
import org.bg52.synthetix.api.SynthetixAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extracts bundled resource pack assets from the JAR into the plugin's data folder
 * and registers them seamlessly into Synthetix's ResourcePackManager.
 */
public class ResourcePackExtractor {

    public static void extractAndRegister(Artifacts plugin) {
        try {
            File targetDir = new File(plugin.getDataFolder(), "resources");
            File assetsDir = new File(targetDir, "assets");

            CodeSource src = plugin.getClass().getProtectionDomain().getCodeSource();
            if (src != null) {
                URL jar = src.getLocation();
                try (ZipInputStream zip = new ZipInputStream(jar.openStream())) {
                    ZipEntry entry;
                    byte[] buffer = new byte[8192];
                    while ((entry = zip.getNextEntry()) != null) {
                        String name = entry.getName();
                        // In JAR resources, assets are in resources/assets/
                        if (name.startsWith("resources/assets/")) {
                            String relativePath = name.substring("resources/".length());
                            File outFile = new File(targetDir, relativePath);
                            if (entry.isDirectory()) {
                                outFile.mkdirs();
                            } else {
                                outFile.getParentFile().mkdirs();
                                try (OutputStream out = new FileOutputStream(outFile)) {
                                    int len;
                                    while ((len = zip.read(buffer)) > 0) {
                                        out.write(buffer, 0, len);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Extract and deploy Geyser Bedrock assets
            extractAndDeployGeyser(plugin);

            // Register with Synthetix Core ResourcePack Engine
            if (SynthetixAPI.getOptional().isPresent() && assetsDir.exists()) {
                SynthetixAPI.get().registerResourcePack(plugin, targetDir);
                SynthetixAPI.get().rebuildResourcePack();
                plugin.getLogger().info("[Artifacts] Hooked 3D visual models & audio into Synthetix Resource Pack!");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[Artifacts] Failed to extract/register pack assets: " + t.getMessage());
        }
    }

    private static void extractAndDeployGeyser(Artifacts plugin) {
        try {
            File geyserDir = new File(plugin.getDataFolder(), "geyser");
            if (!geyserDir.exists()) {
                geyserDir.mkdirs();
            }

            File mappingsFile = new File(geyserDir, "Artifacts_mappings.json");
            try (InputStream in = plugin.getResource("Artifacts_mappings.json")) {
                if (in != null) {
                    java.nio.file.Files.copy(in, mappingsFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }

            File zipFile = new File(geyserDir, "Artifacts_Geyser.zip");
            try (InputStream in = plugin.getResource("Artifacts_Geyser.zip")) {
                if (in != null) {
                    java.nio.file.Files.copy(in, zipFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }

            File pluginsFolder = plugin.getDataFolder().getParentFile();
            if (pluginsFolder == null) return;

            File geyserFolder = new File(pluginsFolder, "Geyser-Spigot");
            if (!geyserFolder.exists()) {
                geyserFolder = new File(pluginsFolder, "Geyser");
            }

            if (geyserFolder.exists() && geyserFolder.isDirectory()) {
                File customMappingsDir = new File(geyserFolder, "custom_mappings");
                if (!customMappingsDir.exists()) customMappingsDir.mkdirs();
                if (mappingsFile.exists()) {
                    File destMappings = new File(customMappingsDir, mappingsFile.getName());
                    java.nio.file.Files.copy(mappingsFile.toPath(), destMappings.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("[Artifacts] Deployed " + mappingsFile.getName() + " to " + geyserFolder.getName() + "/custom_mappings");
                }

                File packsDir = new File(geyserFolder, "packs");
                if (!packsDir.exists()) packsDir.mkdirs();
                if (zipFile.exists()) {
                    File destPack = new File(packsDir, zipFile.getName());
                    java.nio.file.Files.copy(zipFile.toPath(), destPack.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("[Artifacts] Deployed " + zipFile.getName() + " to " + geyserFolder.getName() + "/packs");
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[Artifacts] Failed to extract/deploy Geyser assets: " + t.getMessage());
        }
    }
}
