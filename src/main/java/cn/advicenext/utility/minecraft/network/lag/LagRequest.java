package cn.advicenext.utility.minecraft.network.lag;

import java.util.Set;

public class LagRequest {
    private final Set<LagDirection> directions;
    private final LagTimeout timeout;

    public LagRequest(Set<LagDirection> directions, LagTimeout timeout) {
        this.directions = directions;
        this.timeout = timeout;
    }

    public Set<LagDirection> getDirections() {
        return directions;
    }

    public LagTimeout getTimeout() {
        return timeout;
    }
}