package com.github.hhhzzzsss.epsilonbot.util;

import lombok.Getter;

public enum ChatColor {
    DARK_RED('4', 11141120, "AA0000"),
    RED('c', 16733525, "FF5555"),
    GOLD('6', 16755200, "FFAA00"),
    YELLOW('e', 16777045, "FFFF55"),
    DARK_GREEN('2', 43520, "00AA00"),
    GREEN('a', 5635925, "55FF55"),
    AQUA('b', 5636095, "55FFFF"),
    DARK_AQUA('3', 43690,"00AAAA"),
    DARK_BLUE('1', 170, "0000AA"),
    BLUE('9', 5592575, "5555FF"),
    LIGHT_PURPLE('d', 16733695, "FF55FF"),
    DARK_PURPLE('5', 11141290, "AA00AA"),
    WHITE('f', 16777215, "FFFFFF"),
    GRAY('7', 11184810, "AAAAAA"),
    DARK_GRAY('8', 5592405, "555555"),
    BLACK('0', 0, "000000");

    @Getter private final char code;
    @Getter private final int decimal;
    @Getter private final String hexadecimal;

    ChatColor(char code, int decimal, String hexadecimal) {
        this.code = code;
        this.decimal = decimal;
        this.hexadecimal = hexadecimal;
    }
}
