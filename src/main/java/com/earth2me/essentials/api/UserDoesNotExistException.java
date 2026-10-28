package com.earth2me.essentials.api;

import java.util.UUID;

public class UserDoesNotExistException extends me.nexuswarp.economy.UserDoesNotExistException {
    public UserDoesNotExistException(String target) {
        super(target);
    }

    public UserDoesNotExistException(UUID uuid) {
        super(uuid);
    }
}
