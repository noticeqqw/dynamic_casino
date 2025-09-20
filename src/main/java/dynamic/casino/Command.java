package dynamic.casino;

public interface Command {
    void execute();
    boolean canExecute(GameState currentState);
}
