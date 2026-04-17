package tvgameboy.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tvgameboy.games.template.TemplateGame;

public final class GameRegistry {
    private static final List<GameEntry> GAMES;

    static {
        List<GameEntry> games = new ArrayList<>();

        games.add(0, new GameEntry(
            "template-game",
            "Cast",
            TemplateGame::new
        ));

        GAMES = Collections.unmodifiableList(games);
    }

    private GameRegistry() {
    }

    public static List<GameEntry> getGames() {
        return GAMES;
    }
}
