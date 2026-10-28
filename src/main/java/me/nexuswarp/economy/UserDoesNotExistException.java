package me.nexuswarp.economy;

import java.util.UUID;

public class UserDoesNotExistException extends Exception {

    public UserDoesNotExistException(String target) {
        super("User " + target + " does not exist in economy.");
    }

    public UserDoesNotExistException(UUID uuid) {
        super("User with UUID " + uuid + " does not exist in economy.");
    }
}
