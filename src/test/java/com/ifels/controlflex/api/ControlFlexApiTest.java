package com.ifels.controlflex.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ControlFlexApi} static hub behavior.
 * Each test resets state in @BeforeEach to ensure isolation.
 */
class ControlFlexApiTest {

    @BeforeEach
    void setUp() {
        ControlFlexApi.resetForTesting();
    }

    @AfterEach
    void tearDown() {
        ControlFlexApi.resetForTesting();
    }

    // ===== isAvailable() =====

    @Test
    void isAvailable_shouldReturnFalse_whenNoProviderSet() {
        assertFalse(ControlFlexApi.isAvailable());
    }

    @Test
    void isAvailable_shouldReturnTrue_whenAllProvidersSet() {
        ControlFlexApi.setActionStateProvider(new StubActionStateProvider());
        ControlFlexApi.setInputProvider(new StubInputProvider());
        ControlFlexApi.setPlayerStateRegistry(new StubPlayerStateRegistry());

        assertTrue(ControlFlexApi.isAvailable());
    }

    @Test
    void isAvailable_shouldReturnFalse_whenOnlySomeProvidersSet() {
        ControlFlexApi.setActionStateProvider(new StubActionStateProvider());
        // inputProvider and playerStateRegistry not set

        assertFalse(ControlFlexApi.isAvailable());
    }

    // ===== isControllerConnected() =====

    @Test
    void isControllerConnected_shouldReturnFalse_whenNoInputProvider() {
        assertFalse(ControlFlexApi.isControllerConnected());
    }

    @Test
    void isControllerConnected_shouldDelegateToProvider_whenSet() {
        ControlFlexApi.setInputProvider(new StubInputProvider());
        assertTrue(ControlFlexApi.isControllerConnected());
    }

    // ===== getApiVersion() =====

    @Test
    void getApiVersion_shouldReturnNull_whenNotSet() {
        assertNull(ControlFlexApi.getApiVersion());
    }

    @Test
    void getApiVersion_shouldReturnSetValue() {
        ControlFlexApi.setApiVersion("1.2.3");
        assertEquals("1.2.3", ControlFlexApi.getApiVersion());
    }

    // ===== Duplicate setter protection =====

    @Test
    void setActionStateProvider_shouldThrow_whenAlreadySet() {
        ControlFlexApi.setActionStateProvider(new StubActionStateProvider());
        assertThrows(IllegalStateException.class, () ->
            ControlFlexApi.setActionStateProvider(new StubActionStateProvider()));
    }

    @Test
    void setInputProvider_shouldThrow_whenAlreadySet() {
        ControlFlexApi.setInputProvider(new StubInputProvider());
        assertThrows(IllegalStateException.class, () ->
            ControlFlexApi.setInputProvider(new StubInputProvider()));
    }

    @Test
    void setPlayerStateRegistry_shouldThrow_whenAlreadySet() {
        ControlFlexApi.setPlayerStateRegistry(new StubPlayerStateRegistry());
        assertThrows(IllegalStateException.class, () ->
            ControlFlexApi.setPlayerStateRegistry(new StubPlayerStateRegistry()));
    }

    // ===== Null protection =====

    @Test
    void setActionStateProvider_shouldThrow_whenNull() {
        assertThrows(NullPointerException.class, () ->
            ControlFlexApi.setActionStateProvider(null));
    }

    @Test
    void setInputProvider_shouldThrow_whenNull() {
        assertThrows(NullPointerException.class, () ->
            ControlFlexApi.setInputProvider(null));
    }

    @Test
    void setPlayerStateRegistry_shouldThrow_whenNull() {
        assertThrows(NullPointerException.class, () ->
            ControlFlexApi.setPlayerStateRegistry(null));
    }

    // ===== reloadGuides() no-op =====

    @Test
    void reloadGuides_shouldNotThrow_whenCallbackNotSet() {
        assertDoesNotThrow(ControlFlexApi::reloadGuides);
    }

    // ===== Package validation =====

    @Test
    void setActionStateProvider_shouldThrow_whenForeignImpl() {
        com.example.ForeignActionStateProvider foreign = new com.example.ForeignActionStateProvider();
        assertThrows(SecurityException.class, () ->
            ControlFlexApi.setActionStateProvider(foreign));
    }

    // ===== Plugin registration =====

    @Test
    void registerPlugin_shouldAddToRegisteredList() {
        StubPlugin plugin = new StubPlugin();
        ControlFlexApi.registerPlugin(plugin);

        assertTrue(ControlFlexApi.getRegisteredPlugins().contains(plugin));
    }

    @Test
    void registerPlugin_shouldNotifyImmediately_whenApiAvailable() {
        // Make API available first
        ControlFlexApi.setActionStateProvider(new StubActionStateProvider());
        ControlFlexApi.setInputProvider(new StubInputProvider());
        ControlFlexApi.setPlayerStateRegistry(new StubPlayerStateRegistry());

        StubPlugin plugin = new StubPlugin();
        assertFalse(plugin.readyCalled);
        ControlFlexApi.registerPlugin(plugin);
        assertTrue(plugin.readyCalled);
    }

    @Test
    void registerPlugin_shouldNotNotifyImmediately_whenApiNotAvailable() {
        StubPlugin plugin = new StubPlugin();
        ControlFlexApi.registerPlugin(plugin);
        assertFalse(plugin.readyCalled);
    }

    @Test
    void registerPlugin_shouldThrow_whenForeignPlugin() {
        com.example.ForeignPlugin foreign = new com.example.ForeignPlugin();
        assertThrows(SecurityException.class, () ->
            ControlFlexApi.registerPlugin(foreign));
    }

    @Test
    void registerPlugin_shouldThrow_whenNull() {
        assertThrows(NullPointerException.class, () ->
            ControlFlexApi.registerPlugin(null));
    }

    @Test
    void getRegisteredPlugins_shouldReturnUnmodifiableSnapshot() {
        ControlFlexApi.registerPlugin(new StubPlugin());
        List<IControlFlexPlugin> snapshot = ControlFlexApi.getRegisteredPlugins();
        assertEquals(1, snapshot.size());
        // Returned list is unmodifiable — mutating it should throw
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
    }

    // ===== Stubs =====

    static class StubActionStateProvider implements IActionStateProvider {
        @Override public boolean isGameActionActive(String actionId) { return false; }
        @Override public boolean isGuiActionActive(String actionId) { return false; }
        @Override public java.util.Set<String> getActiveGameActions() { return java.util.Set.of(); }
        @Override public java.util.Set<String> getActiveGuiActions() { return java.util.Set.of(); }
    }

    static class StubInputProvider implements IInputProvider {
        @Override public boolean isConnected() { return true; }
        @Override public IControllerState getControllerState() { return null; }
        @Override public String getGamepadName() { return "test"; }
        @Override public int getGamepadIndex() { return 0; }
        @Override public IControllerCapabilities getCapabilities() { return null; }
    }

    static class StubPlayerStateRegistry implements IPlayerStateRegistry {
        @Override public void setState(String stateKey, boolean active) {}
        @Override public boolean getState(String stateKey) { return false; }
        @Override public void clearState(String stateKey) {}
    }

    static class StubPlugin implements IControlFlexPlugin {
        boolean readyCalled;

        @Override
        public String getModId() { return "test_mod"; }

        @Override
        public void onControlFlexReady() { readyCalled = true; }
    }
}
