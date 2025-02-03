package org.byeautumn.chuachua.generate;

import org.byeautumn.chuachua.undo.ActionRecord;
import org.byeautumn.chuachua.undo.ActionRecorder;

public interface Generable {
    ActionRecord generate();
}
