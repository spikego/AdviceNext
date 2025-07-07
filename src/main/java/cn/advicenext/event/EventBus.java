package cn.advicenext.event;

import java.lang.reflect.Method;
import java.util.*;

public class EventBus {
    private static final EventBus INSTANCE = new EventBus();

    public static EventBus getInstance() {
        return INSTANCE;
    }

    public record ListenerMethod(Object target, Method method, int priority) {
    }

    private static final Map<Class<?>, List<ListenerMethod>> listeners = new HashMap<>();

    public static void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Listener.class) && method.getParameterCount() == 1) {
                Listener annotation = method.getAnnotation(Listener.class);
                Class<?> eventType = method.getParameterTypes()[0];
                method.setAccessible(true);
                listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                        .add(new ListenerMethod(listener, method, annotation.p()));
            }
        }
    }
    
    public static void unregister(Object listener) {
        listeners.values().forEach(list -> 
            list.removeIf(lm -> lm.target == listener)
        );
    }

    public static void post(Object event) {
        List<ListenerMethod> methods = listeners.get(event.getClass());
        if (methods == null) return;
        methods.stream()
                .sorted(Comparator.comparingInt((ListenerMethod lm) -> lm.priority).reversed())
                .forEach(lm -> {
                    try {
                        lm.method.invoke(lm.target, event);
                        if (event instanceof Event && ((Event) event).cancelled) return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }
}