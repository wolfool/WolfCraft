package kr.wolfool.wolfcraft.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * A crafting task in a player's queue.
 */
public class CraftTask {

    private final UUID playerId;
    private final String recipeId;
    private final String recipeDisplay;
    private final ItemStack result;
    private final long startTime;
    private final int duration; // seconds
    private boolean completed;

    public CraftTask(UUID playerId, String recipeId, String recipeDisplay,
                     ItemStack result, int duration) {
        this.playerId = playerId;
        this.recipeId = recipeId;
        this.recipeDisplay = recipeDisplay;
        this.result = result;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
        this.completed = false;
    }

    /**
     * Check if crafting is finished.
     */
    public boolean isFinished() {
        return getElapsedSeconds() >= duration;
    }

    /**
     * Get elapsed seconds since start.
     */
    public long getElapsedSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    /**
     * Get remaining seconds.
     */
    public int getRemainingSeconds() {
        return Math.max(0, duration - (int) getElapsedSeconds());
    }

    /**
     * Get progress (0.0 - 1.0).
     */
    public double getProgress() {
        return Math.min(1.0, (double) getElapsedSeconds() / duration);
    }

    // Getters
    public UUID getPlayerId() { return playerId; }
    public String getRecipeId() { return recipeId; }
    public String getRecipeDisplay() { return recipeDisplay; }
    public ItemStack getResult() { return result; }
    public int getDuration() { return duration; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
