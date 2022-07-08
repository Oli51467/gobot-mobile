package com.irlab.view.processing.stoneDetector;

public class MoveHypothesis {
    public int row;
    public int column;
    public int color;
    public double confidence;
    
    public MoveHypothesis(int color, double confidence) {
        this.color = color;
        this.confidence = confidence;
    }
}
