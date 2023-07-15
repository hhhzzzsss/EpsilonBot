package com.github.hhhzzzsss.epsilonbot.command;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter public class ChatSender {
    private final EpsilonBot bot;
    private final UUID uuid;
    private final String msgSender;
    private final int permission;
}
