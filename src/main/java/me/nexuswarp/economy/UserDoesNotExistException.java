package me.nexuswarp.economy;

public class UserDoesNotExistException extends Exception {
    public UserDoesNotExistException(String target) {
        super("User " + target + " does not exist in economy.");
    }
}
