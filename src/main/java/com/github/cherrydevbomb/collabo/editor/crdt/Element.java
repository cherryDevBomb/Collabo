package com.github.cherrydevbomb.collabo.editor.crdt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Element {

    private ID id;
    private ID originLeft;
    private ID originRight;
    private String value;
    private boolean deleted;
}
