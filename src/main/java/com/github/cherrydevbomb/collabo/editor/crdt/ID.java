package com.github.cherrydevbomb.collabo.editor.crdt;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class ID {

    private String userId;
    private long timestamp;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ID id = (ID) o;
        return timestamp == id.timestamp && Objects.equals(userId, id.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, timestamp);
    }

    public boolean isPredecessorOf(ID other) {
        boolean isUserPredecessor = this.userId.compareToIgnoreCase(other.getUserId()) < 0;
        return isUserPredecessor || (this.userId.equalsIgnoreCase(other.getUserId()) && timestamp < other.getTimestamp());
    }
}