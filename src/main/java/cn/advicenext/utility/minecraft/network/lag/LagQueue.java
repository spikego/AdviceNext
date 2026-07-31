package cn.advicenext.utility.minecraft.network.lag;

import java.util.concurrent.ConcurrentLinkedQueue;

public class LagQueue {
    private final ConcurrentLinkedQueue<LagRequest> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(LagRequest request) {
        queue.add(request);
    }

    public LagRequest poll() {
        return queue.poll();
    }

    public LagRequest peek() {
        return queue.peek();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public void clear() {
        queue.clear();
    }

    public void remove(LagRequest request) {
        queue.remove(request);
    }

    public ConcurrentLinkedQueue<LagRequest> getRawQueue() {
        return queue;
    }
}