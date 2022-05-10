package com.github.hhhzzzsss.epsilonbot;

import java.util.concurrent.TimeUnit;

public class Main {
    private static EpsilonBot bot = new EpsilonBot();

    public static void main( String[] args )
    {
        bot.start();
    }

    public static void restartBot() {
        try {
            bot.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static void stopBot() {
        try {
            bot.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(42);
    }
}
