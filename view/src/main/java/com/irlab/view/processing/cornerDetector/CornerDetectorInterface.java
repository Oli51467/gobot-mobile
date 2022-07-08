package com.irlab.view.processing.cornerDetector;

import org.opencv.core.Mat;

import java.util.List;

public interface CornerDetectorInterface {
    public void setImageIndex(int imageIndex);
    public void setCornerIndex(int cornerIndex);
    public List<Corner> detectCandidateCornersIn(Mat image);
}