package org.aerial_dad.bedwars_plugin.bedwars.game.Constants;


public class GameConfig {
//    Team count and team size depends on what mode you set the plug-in to
    private int teamCount = 2;
    private int sizePerTeam = 1;

    private GameConfig(int teamCount, int sizePerTeam) {
        this.teamCount = teamCount;
        this.sizePerTeam = sizePerTeam;
    }

    public int getSizePerTeam(){
        return this.sizePerTeam;
    }

    public int getTeamCount() {
        return this.teamCount;
    }

    public static Builder toBuilder() {
        return new Builder();
    }

    public static class Builder {
//        private GameConfig gameConfig;
        private int teamCount;
        private int sizePerTeam;

        public Builder teamCount(int teamCount) {
            this.teamCount = teamCount;
            return this;
        }

        public Builder sizePerTeam(int sizePerTeam) {
            this.sizePerTeam = sizePerTeam;
            return this;
        }
        public GameConfig build() {
            return new GameConfig(teamCount, sizePerTeam);
        }
    }

    public static void main(String[] args) {
        GameConfig config = GameConfig.toBuilder()
                .teamCount(5)
                .sizePerTeam(17)
                .build();
        System.out.println("Team Count: " + config.getTeamCount());
        System.out.println("Size per Team: " + config.getSizePerTeam());
    }
}
