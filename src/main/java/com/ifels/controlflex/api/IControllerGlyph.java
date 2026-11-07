package com.ifels.controlflex.api;

import org.jetbrains.annotations.Nullable;

/**
 * A drawable representation of one bound button.
 *
 * <p>Glyph artwork is packed in a texture atlas, so a glyph is a <b>rectangle</b> of a
 * texture rather than a whole file. This interface deliberately hides the atlas: if a
 * future version switches to one file per icon, {@link #getU()} / {@link #getV()} become
 * {@code 0} and {@link #getWidth()} / {@link #getHeight()} become the icon size — no
 * change here.</p>
 *
 * <p>Drawing is the caller's job because this API has no Minecraft dependency. See the
 * API reference for a {@code GuiGraphics#blit} snippet.</p>
 *
 * @since 0.9.2.1
 */
public interface IControllerGlyph {

    /**
     * Texture resource id, e.g.
     * {@code "controlflex:textures/gui/controller/controller_xbox.png"}.
     *
     * <p>Parse it with the host Minecraft version's {@code ResourceLocation} constructor:
     * {@code ResourceLocation.parse(...)} on 1.21+,
     * {@code new ResourceLocation(...)} on older versions.</p>
     */
    String getTextureId();

    /** Left edge of the sprite inside the texture. */
    int getU();

    /** Top edge of the sprite inside the texture. */
    int getV();

    /** Sprite width in texture pixels. */
    int getWidth();

    /** Sprite height in texture pixels. */
    int getHeight();

    /** Full texture width, needed for {@code GuiGraphics#blit}. */
    int getTextureWidth();

    /** Full texture height, needed for {@code GuiGraphics#blit}. */
    int getTextureHeight();

    /**
     * Text to draw instead of the sprite, for buttons that have no artwork — keyboard
     * keys ({@code "F1"}, {@code "HOME"}) and any button id the active skin has no cell
     * for (the raw id is used then, so a glyph is always drawable).
     *
     * <p>When non-null, ignore the texture rectangle and draw this string with the
     * vanilla font; the texture fields are then unspecified ({@link #getTextureId()}
     * is {@code ""}).</p>
     *
     * @return fallback label, or {@code null} when the sprite should be drawn
     */
    @Nullable String getTextFallback();
}
