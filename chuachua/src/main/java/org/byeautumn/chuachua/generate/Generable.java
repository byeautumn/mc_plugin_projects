package org.byeautumn.chuachua.generate;

import org.bukkit.entity.Player;
import org.byeautumn.chuachua.undo.ActionRecord;
import org.byeautumn.chuachua.undo.ActionRecorder;

public interface Generable {
    ActionRecord generate(Player player);
}
