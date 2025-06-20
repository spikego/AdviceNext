package cn.advicenext.features.module.impl.render;

import cn.advicenext.features.module.Module;
import cn.advicenext.features.module.Category;
public class NoHurtCam extends Module{
    public static NoHurtCam INSTANCE;
    public NoHurtCam() {
        super("NoHurtCam", "NoHurtCam", Category.RENDER);
        INSTANCE = this;
    }
}
