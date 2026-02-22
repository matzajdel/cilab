package org.example.commands;

public interface Command {
    void execute(String[] args) throws Exception;
    String getName();
    String getDescription();
}
