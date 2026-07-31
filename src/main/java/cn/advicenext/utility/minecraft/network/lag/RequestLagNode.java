package cn.advicenext.utility.minecraft.network.lag;

public class RequestLagNode extends LagNode {
    private final LagRequest request;

    public RequestLagNode(LagRequest request) {
        this.request = request;
    }

    public LagRequest getRequest() {
        return request;
    }
}