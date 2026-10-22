package demo;

import event.GameplayEvent;

/** Bridge for slow status: the local production PlayerStatus bus lacks slow event types. */
public final class DemoStatusEvent implements GameplayEvent {
    private final String description;
    public DemoStatusEvent(String description) { this.description = description; }
    public String getDescription() { return description; }
}
