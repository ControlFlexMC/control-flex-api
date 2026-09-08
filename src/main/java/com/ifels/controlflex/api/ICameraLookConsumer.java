package com.ifels.controlflex.api;

/**
 * Camera look consumer: implemented by bridge mods (e.g., Epic Fight compat
 * mods) to consume ControlFlex's non-"simulated mouse movement" look input
 * when a third-party mod takes over the rendering camera.
 *
 * <p>Background: some mods (Epic Fight, etc.) take over the rendering camera
 * (e.g., {@code camera_mode=ALWAYS} / TPS combat camera / lock-on target), so
 * the player entity's yRot/xRot are no longer the data source for the rendered
 * view. Only "simulated mouse movement" (vanilla mouse pipeline) can be
 * intercepted by them. Once a bridge mod implements this interface, ControlFlex
 * asks this consumer before writing player rotation on direct / smooth-direct
 * look; returning true consumes the frame's look (typically forwarded to the
 * taking-over mod's camera API).</p>
 *
 * <p>Calling convention: client thread; at most once per frame; input is the
 * delta calibrated before "turn", in the same scale as what the vanilla mouse
 * pipeline hands to the taking-over mod.</p>
 *
 * @since 0.8.8
 */
public interface ICameraLookConsumer {

    /**
     * Consume one frame of look input.
     *
     * @param yaw   this frame's yaw delta (calibrated before turn)
     * @param pitch this frame's pitch delta (calibrated before turn)
     * @return true = this frame's look was consumed, ControlFlex must not
     *         write player rotation;
     *         false = not consumed, ControlFlex handles it as usual (original
     *         behavior unchanged)
     */
    boolean consumeLook(float yaw, float pitch);
}
