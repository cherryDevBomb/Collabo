package com.github.cherrydevbomb.collabo.editor.crdt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ID {

    private String userId;
    private long timestamp;

    public boolean isPredecessorOf(ID other) {
        boolean isUserPredecessor = this.userId.compareToIgnoreCase(other.getUserId()) < 0;
        return isUserPredecessor || (this.userId.equalsIgnoreCase(other.getUserId()) && timestamp < other.getTimestamp());
    }

    @Override
    public String toString() {
        return userId + "-" + timestamp;
    }
}