package com.github.cherrydevbomb.collabo.editor.crdt;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(of = {"id"})
public class Element {

    private ID id;
    private ID originLeft;
    private ID originRight;
    private String value;
    private boolean deleted;
}
