package com.github.cherrydevbomb.collabo.editor.crdt;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Element {

    private ID id;
    private ID originLeft;
    private ID originRight;
    private String value;
    private boolean deleted;
}
