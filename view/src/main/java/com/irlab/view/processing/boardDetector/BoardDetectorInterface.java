package com.irlab.view.processing.boardDetector;

import org.opencv.core.Mat;

public interface BoardDetectorInterface {

    int STATE_BOARD_IS_INSIDE = 1;
    int STATE_LOOKING_FOR_BOARD = 2;

    public boolean isBoardContainedIn(Mat orthogonalBoardImage);

    void setState(int state);
    void setImageIndex(int imageIndex);
}
