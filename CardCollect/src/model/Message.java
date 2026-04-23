package model;
//message
public class Message {
    public final String id;
    public final String fromUserId;
    public final String toUserId;
    public final String body;
    public final long timestamp;

    public Message(String id, String fromUserId, String toUserId, String body, long timestamp) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.body = body;
        this.timestamp = timestamp;
    }
}
