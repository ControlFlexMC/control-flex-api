package com.ifels.controlflex.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ControlFlexApiVersionTest {

    @Test
    void versionFieldMatchesGradleApiVersion() throws IOException {
        assertEquals(gradleApiVersion(), ControlFlexApi.VERSION);
        assertFalse(ControlFlexApi.VERSION.isBlank());
    }

    @Test
    void getApiVersionReturnsLibraryVersion() {
        assertEquals(ControlFlexApi.VERSION, ControlFlexApi.getApiVersion());
    }

    @Test
    void setApiVersionDoesNotOverrideLibraryVersion() {
        ControlFlexApi.setApiVersion("0.8.6");
        assertEquals(ControlFlexApi.VERSION, ControlFlexApi.getApiVersion());
    }

    private static String gradleApiVersion() throws IOException {
        Path propsFile = Path.of("gradle.properties");
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(propsFile)) {
            properties.load(in);
        }
        String version = properties.getProperty("api_version");
        if (version == null || version.isBlank()) {
            throw new IllegalStateException("api_version missing in gradle.properties");
        }
        return version.trim();
    }
}
