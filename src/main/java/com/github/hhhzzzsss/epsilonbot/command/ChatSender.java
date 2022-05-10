package com.github.hhhzzzsss.epsilonbot.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class ChatSender {
    private final UUID uuid;
    private final int permission;
}
