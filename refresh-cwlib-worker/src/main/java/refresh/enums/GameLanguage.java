package refresh.enums;

import cwlib.io.ValueEnum;

// based on the game's ELanguage
public enum GameLanguage implements ValueEnum<Integer> {
    // idk the actual values, too lazy to look up rn
    English(18),
    German(100);

    private final int value;

    GameLanguage(int value) {
        this.value = value;
    }

    public Integer getValue()
    {
        return this.value;
    }
}
