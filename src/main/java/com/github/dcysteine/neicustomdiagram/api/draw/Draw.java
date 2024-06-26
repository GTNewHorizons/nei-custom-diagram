package com.github.dcysteine.neicustomdiagram.api.draw;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.Fluid;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.github.dcysteine.neicustomdiagram.api.Formatter;
import com.github.dcysteine.neicustomdiagram.api.diagram.layout.Grid;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.Tooltip;
import com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.TooltipLine;
import com.google.auto.value.AutoValue;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.guihook.GuiContainerManager;

/** GUI drawing library. */
public final class Draw {

    public static final int ICON_WIDTH = 16;
    public static final int TEXT_HEIGHT = 8;

    /**
     * Some pre-defined colours, for convenience.
     *
     * <p>
     * Colours are encoded as four {@code byte}s packed into an {@code int}:
     * <ul>
     * <li>{@code 0xFF000000}: Alpha channel ({@code 0xFF} is fully opaque)
     * <li>{@code 0x00FF0000}: Red channel
     * <li>{@code 0x0000FF00}: Green channel
     * <li>{@code 0x000000FF}: Blue channel
     * </ul>
     */
    public static final class Colour {

        public static final int BLACK = 0xFF000000;
        public static final int WHITE = 0xFFFFFFFF;
        public static final int GREY = 0xFF404040;
        public static final int RED = 0xFFFF0000;
        public static final int GREEN = 0xFF008000;
        public static final int BLUE = 0xFF0000FF;
        public static final int CYAN = 0xFF00FFFF;
        public static final int YELLOW = 0xFFFFFF00;
        public static final int MAGENTA = 0xFFFF00FF;

        public static final int GUI_BG = 0xFFC6C6C6;
        public static final int SLOT_BG = 0xFF8B8B8B;
        public static final int OVERLAY_WHITE = 0x80FFFFFF;
        public static final int OVERLAY_BLUE = 0x800000FF;

        // Static class.
        private Colour() {}
    }

    /** Struct class holding coordinates for mod textures. */
    @AutoValue
    public abstract static class TextureData {

        private static final String TEXTURE_PATH = "neicustomdiagram:textures/slots.png";

        public static final TextureData SLOT = create(0, 0, 18, 18);
        public static final TextureData BIG_SLOT = create(18, 0, 26, 26);
        public static final TextureData RAISED_SLOT = create(0, 18, 18, 18);

        private static TextureData create(int x, int y, int w, int h) {
            return new AutoValue_Draw_TextureData(x, y, w, h);
        }

        abstract int x();

        abstract int y();

        abstract int width();

        abstract int height();
    }

    // Static class.
    private Draw() {}

    /**
     * Draws a line of thickness 2 between the two points.
     *
     * <p>
     * <em>Horrible things</em> will happen if the two points aren't orthogonal. And by <em>horrible</em> I mean you'll
     * get a rectangle instead of a line.
     *
     * <p>
     * See {@link Draw.Colour} for colour encoding information.
     */
    public static void drawLine(Point a, Point b, int colour) {
        int x = Math.min(a.x(), b.x()) - 1;
        int y = Math.min(a.y(), b.y()) - 1;
        int w = Math.abs(a.x() - b.x()) + 2;
        int h = Math.abs(a.y() - b.y()) + 2;

        GL11.glDisable(GL11.GL_LIGHTING);
        GuiDraw.drawRect(x, y, w, h, colour);
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    /**
     * Draws an arrowhead pointing at the second point.
     *
     * <p>
     * <em>Horrible things</em> will happen if the two points aren't orthogonal. And by <em>horrible</em> I mean you'll
     * get a mess of rectangles.
     *
     * <p>
     * See {@link Draw.Colour} for colour encoding information.
     */
    public static void drawArrowhead(Point a, Point b, int colour) {
        // (diffX, diffY) is a unit vector pointing from b to a.
        int diffX = Integer.signum(a.x() - b.x());
        int diffY = Integer.signum(a.y() - b.y());

        GL11.glDisable(GL11.GL_LIGHTING);
        Point currPos = b;
        for (int i = 0; i < 3; i++) {
            // Draw a line perpendicular to (diffX, diffY) and of length 2 * i.
            drawLine(currPos.translate(i * diffY, i * diffX), currPos.translate(-i * diffY, -i * diffX), colour);

            currPos = currPos.translate(diffX, diffY);
        }
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    /**
     * Draws some text, with the given point being the top-left corner. This is different from most other draw methods,
     * which draw from the center, but doing this helps avoid left-justified text being off by one pixel due to integer
     * division.
     *
     * @param colour See {@link Draw.Colour} for colour encoding information.
     * @param small  Whether to draw half-scale text.
     * @param shadow Whether to draw a shadow for the text.
     */
    public static void drawText(String text, Point pos, int colour, boolean small, boolean shadow) {
        int x, y;
        if (small) {
            x = 2 * pos.x();
            y = 2 * pos.y();

            GL11.glPushMatrix();
            GL11.glScalef(0.5f, 0.5f, 0.5f);
        } else {
            x = pos.x();
            y = pos.y();
        }

        GL11.glDisable(GL11.GL_LIGHTING);
        GuiDraw.drawString(text, x, y, colour, shadow);
        GL11.glEnable(GL11.GL_LIGHTING);

        // Looks like drawString() leaves colour blending active, so reset it.
        GL11.glColor4f(1, 1, 1, 1);

        if (small) {
            GL11.glPopMatrix();
        }
    }

    /**
     * Draws stack size for a component centered on the given point.
     *
     * <p>
     * This will be drawn in the bottom-right corner of the component. Keep this short!
     */
    public static void drawStackSize(int stackSize, Point pos) {
        String text = Formatter.smartFormatInteger(stackSize);
        boolean small = stackSize >= 100;
        drawTextOverIcon(text, pos, Grid.Direction.SE, Colour.WHITE, small, true);
    }

    /**
     * Draws additional information for a component centered on the given point.
     *
     * <p>
     * This will be drawn in the top-left corner of the component. Keep this short!
     */
    public static void drawAdditionalInfo(String text, Point pos, boolean small) {
        drawTextOverIcon(text, pos, Grid.Direction.NW, Colour.YELLOW, small, true);
    }

    /** Draws text offset in the specified direction over an icon. */
    public static void drawTextOverIcon(String text, Point pos, Grid.Direction dir, int colour, boolean small,
            boolean shadow) {
        int textWidth = GuiDraw.getStringWidth(text);
        int textHeight = TEXT_HEIGHT;
        if (small) {
            textWidth /= 2;
            textHeight /= 2;
        }

        Point directionPos = pos.translate(dir.xFactor * ICON_WIDTH / 2, dir.yFactor * ICON_WIDTH / 2);
        Point textAnchor = directionPos
                .translate(-(dir.xFactor + 1) * textWidth / 2, -(dir.yFactor + 1) * textHeight / 2);
        drawText(text, textAnchor, colour, small, shadow);
    }

    /**
     * Unlike the other draw methods, tooltips are drawn with absolute mouse coordinates.
     *
     * <p>
     * This is due to how the handle tooltip method gets called. It ends up being quite convenient for us though, as
     * it's much easier to calculate screen boundaries with absolute coordinates.
     */
    public static void drawTooltip(Tooltip tooltip, Point mousePos) {
        if (tooltip.lines().isEmpty()) {
            return;
        }

        int width = tooltip.width();
        int height = tooltip.height();

        // Mimic the standard NEI tooltip behavior around screen boundaries.
        int x = mousePos.x() + 12;
        int y = mousePos.y() - 12;
        if (x + width > GuiDraw.displaySize().width - 8) {
            x -= tooltip.width() + 24;
        }
        if (y + height > GuiDraw.displaySize().height - 8) {
            y = GuiDraw.displaySize().height - (height + 8);
        }
        x = Math.max(8, x);
        y = Math.max(8, y);

        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glTranslatef(0, 0, 300);
        GuiDraw.gui.incZLevel(300);

        GuiDraw.drawTooltipBox(x - 4, y - 4, tooltip.width() + 8, tooltip.height() + 8);

        int currY = y;
        for (TooltipLine line : tooltip.lines()) {
            line.draw(x, currY);
            currY += Tooltip.LINE_SPACING + line.height();
        }

        GuiDraw.gui.incZLevel(-300);
        GL11.glTranslatef(0, 0, -300);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
    }

    /** Draws an item slot centered on the given point. */
    public static void drawSlot(Point pos) {
        drawTexture(TextureData.SLOT, pos);
    }

    /** Draws a big slot centered on the given point. */
    public static void drawBigSlot(Point pos) {
        drawTexture(TextureData.BIG_SLOT, pos);
    }

    /** Draws a raised slot centered on the given point. */
    public static void drawRaisedSlot(Point pos) {
        drawTexture(TextureData.RAISED_SLOT, pos);
    }

    /** Draws the specified texture centered on the given point. */
    public static void drawTexture(TextureData texture, Point pos) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GuiDraw.changeTexture(TextureData.TEXTURE_PATH);
        GuiDraw.drawTexturedModalRect(
                pos.x() - (texture.width() / 2),
                pos.y() - (texture.height() / 2),
                texture.x(),
                texture.y(),
                texture.width(),
                texture.height());
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    /** Draws an item centered on the given point. */
    public static void drawItem(ItemStack itemStack, Point pos) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GuiContainerManager.drawItem(pos.x() - ICON_WIDTH / 2, pos.y() - ICON_WIDTH / 2, itemStack);
        GL11.glEnable(GL11.GL_LIGHTING);

        // Looks like drawItem() leaves colour blending active, so reset it.
        GL11.glColor4f(1, 1, 1, 1);
    }

    /** Draws a fluid centered on the given point. */
    public static void drawFluid(Fluid fluid, Point pos) {
        IIcon icon = fluid.getIcon();
        if (icon == null) {
            return;
        }

        // Some fluids don't set their icon colour, so we have to blend in the colour ourselves.
        int colour = fluid.getColor();
        GL11.glColor3ub(
                (byte) ((colour & 0xFF0000) >> 16),
                (byte) ((colour & 0x00FF00) >> 8),
                (byte) (colour & 0x0000FF));

        GL11.glDisable(GL11.GL_LIGHTING);
        GuiDraw.changeTexture(TextureMap.locationBlocksTexture);
        GuiDraw.gui.drawTexturedModelRectFromIcon(
                pos.x() - ICON_WIDTH / 2,
                pos.y() - ICON_WIDTH / 2,
                icon,
                ICON_WIDTH,
                ICON_WIDTH);
        GL11.glEnable(GL11.GL_LIGHTING);

        // Reset colour blending.
        GL11.glColor4f(1, 1, 1, 1);
    }

    /**
     * Draws a coloured square centered on the given point.
     *
     * <p>
     * See {@link Draw.Colour} for colour encoding information. You probably want to use a semi-transparent value here.
     */
    public static void drawOverlay(Point pos, int colour) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GuiDraw.drawRect(pos.x() - ICON_WIDTH / 2, pos.y() - ICON_WIDTH / 2, ICON_WIDTH, ICON_WIDTH, colour);
        GL11.glEnable(GL11.GL_LIGHTING);
    }
}
