package cn.advicenext.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;

public class RenderBufferBuilder {
    VertexFormat.DrawMode drawMode;
    //VertexInputType vertextFormat;
    Tessellator tessellator;

    //BufferBuilder buffer = Tessellator.getInstance().begin(drawMode,vertextFormat)

    public static final int FACE_DOWN  = (1 << 0) | (1 << 1) | (1 << 2) | (1 << 3);
    public static final int FACE_UP    = (1 << 4) | (1 << 5) | (1 << 6) | (1 << 7);
    public static final int FACE_NORTH = (1 << 8) | (1 << 9) | (1 << 10) | (1 << 11);
    public static final int FACE_EAST  = (1 << 12) | (1 << 13) | (1 << 14) | (1 << 15);
    public static final int FACE_SOUTH = (1 << 16) | (1 << 17) | (1 << 18) | (1 << 19);
    public static final int FACE_WEST  = (1 << 20) | (1 << 21) | (1 << 22) | (1 << 23);

    public static final int EDGE_NORTH_DOWN = (1 << 0) | (1 << 1);
    public static final int EDGE_EAST_DOWN  = (1 << 2) | (1 << 3);
    public static final int EDGE_SOUTH_DOWN = (1 << 4) | (1 << 5);
    public static final int EDGE_WEST_DOWN  = (1 << 6) | (1 << 7);

    public static final int EDGE_NORTH_WEST = (1 << 8) | (1 << 9);
    public static final int EDGE_NORTH_EAST = (1 << 10) | (1 << 11);
    public static final int EDGE_SOUTH_EAST = (1 << 12) | (1 << 13);
    public static final int EDGE_SOUTH_WEST = (1 << 14) | (1 << 15);

    public static final int EDGE_NORTH_UP = (1 << 16) | (1 << 17);
    public static final int EDGE_EAST_UP  = (1 << 18) | (1 << 19);
    public static final int EDGE_SOUTH_UP = (1 << 20) | (1 << 21);
    public static final int EDGE_WEST_UP  = (1 << 22) | (1 << 23);

}
