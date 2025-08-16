package org.byeautumn.chuachua.generate.world.pipeline.tree;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class TreeGenerationOrganizer {

    public static TreeConfigure getTreeConfigure(String treeType) {
        TreeConfigure config = null;
        if (treeType.equals("oak")) {
            Map<Character, String> oakRules = new HashMap<>();
            oakRules.put('F', "FF-[-F+F+F]+[+F-F-F]");

            config = new TreeConfigure.Builder()
                    .iterations(5)
                    .segmentLength(25)
                    .angle(22.5)
                    .angleVariation(0.7)
                    .axiom("F")
                    .rules(oakRules)
                    .forwardSymbol('F')
                    .logMaterial(Material.OAK_LOG)
                    .leafMaterial(Material.OAK_LEAVES)
                    .build();
        } else if (treeType.equals("birch")) {
            Map<Character, String> birchRules = new HashMap<>();
            birchRules.put('F', "F[-F][+F]");

            config = new TreeConfigure.Builder()
                    .iterations(6)
                    .segmentLength(20)
                    .angle(25)
                    .angleVariation(1.0)
                    .axiom("F")
                    .rules(birchRules)
                    .logMaterial(Material.BIRCH_LOG)
                    .leafMaterial(Material.BIRCH_LEAVES)
                    .build();
        }
        return config;
    }
}
