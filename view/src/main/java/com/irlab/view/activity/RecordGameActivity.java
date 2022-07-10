package com.irlab.view.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.irlab.base.utils.ToastUtil;
import com.irlab.view.MainView;
import com.irlab.view.R;
import com.irlab.view.models.Board;
import com.irlab.view.models.Game;
import com.irlab.view.models.Move;
import com.irlab.view.processing.Drawer;
import com.irlab.view.processing.FileHelper;
import com.irlab.view.processing.ImageUtils;
import com.irlab.view.processing.Logger;
import com.irlab.view.processing.LoggingConfiguration;
import com.irlab.view.processing.boardDetector.BoardDetector;
import com.irlab.view.processing.cornerDetector.Corner;
import com.irlab.view.processing.cornerDetector.CornerDetector;
import com.irlab.view.processing.cornerDetector.Ponto;
import com.irlab.view.processing.similarityCalculator.FingerprintMatching;
import com.irlab.view.processing.similarityCalculator.SimilarityCalculatorInterface;
import com.irlab.view.processing.stoneDetector.StoneDetector;
import com.rosefinches.smiledialog.SmileDialog;
import com.rosefinches.smiledialog.SmileDialogBuilder;
import com.rosefinches.smiledialog.enums.SmileDialogType;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

public class RecordGameActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    public static final String TAG = "Recorder";

    public static int STATE_RUNNING = 1;
    public static int STATE_LOOKING_FOR_BOARD = 2;
    private static int MOVEMENT_THRESHOLD = 10;
    private int state;

    Logger logger;
    FileHelper fileHelper;

    BoardDetector boardDetector = new BoardDetector();
    StoneDetector stoneDetector = new StoneDetector();
    CornerDetector[] cornerDetector;
    Game game;
    Board lastDetectedBoard;

    int moveCounter = 0;
    int curX = -1;
    int curY = -1;
    int curPlayer = -1;
    // 检测到的棋盘必须保持等于检测器注册的时间
    long timeLimit = 1000;
    long timeOfLastBoardDetection;
    long timeSinceLastBoardChange;
    boolean paused = false;
    // TODO: Check if this can be replaced by timeSinceLastBoardChange
    long timeOfLastImageProcessing;
    boolean isCornerTrackingActive = true;
    Mat lastValidOrthogonalBoardImage = null;
    SimilarityCalculatorInterface fingerprintMatching = new FingerprintMatching();

    // 该数组存储每个角落没有保留的帧数, 在它们上方检测到椭圆。这用于误报检查
    int[] numberOfFramesWithoutStone = {0, 0, 0, 0};
    // 用于在探测器偏离太远时将其带回来
    int numberOfFramesWithDissimilarOrthogonalImages = 0;

    // Domain objects
    // 棋盘可以是 9x9 13x13 或者 19x19
    int boardDimension;
    Corner[] boardCorners;
    Corner[] originalBoardCorners;
    Mat cameraFrame;
    // 一个包括棋盘角的图像
    Mat boardPositionInImage;

    // 棋盘转换为正交透视
    Mat orthogonalBoard;
    MatOfPoint boardContour;

    private ImageButton btnUndoLastMove;
    private ImageButton btnRotateCounterClockwise;
    private ImageButton btnRotateClockwise;
    private ImageButton btnResetBoardPosition;
    private Button btnFinish;
    private SoundPool soundPool;
    private int beepId;
    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_game);
        getSupportActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        state = STATE_RUNNING;

        initializeCamera();
        initializeProcessing();
        initializeUserInterface();
        initializeLogging();

        Log.i(TAG, "onCreate() finished");
    }

    private void initializeCamera() {
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        // mOpenCvCameraView.setMaxFrameSize(1000, 1000);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    private void initializeProcessing() {
        Intent i = getIntent();
        String blackPlayer = i.getStringExtra("blackPlayer");
        String whitePlayer = i.getStringExtra("whitePlayer");
        String komi = i.getStringExtra("komi");
        boardDimension = i.getIntExtra("boardDimension", -1);
        int[] foundBoardCorners = i.getIntArrayExtra("boardPositionInImage");
        stoneDetector.setBoardDimension(boardDimension);

        game = new Game(boardDimension, blackPlayer, whitePlayer, komi);
        lastDetectedBoard = new Board(boardDimension);
        timeOfLastBoardDetection = SystemClock.elapsedRealtime();
        timeSinceLastBoardChange = 0;
        timeOfLastImageProcessing = SystemClock.elapsedRealtime();

        originalBoardCorners = new Corner[4];
        originalBoardCorners[0] = new Corner(foundBoardCorners[0], foundBoardCorners[1]);
        originalBoardCorners[1] = new Corner(foundBoardCorners[2], foundBoardCorners[3]);
        originalBoardCorners[2] = new Corner(foundBoardCorners[4], foundBoardCorners[5]);
        originalBoardCorners[3] = new Corner(foundBoardCorners[6], foundBoardCorners[7]);
        boardCorners = originalBoardCorners;
        cornerDetector = new CornerDetector[4];
        for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
            cornerDetector[cornerIndex] = new CornerDetector();
            cornerDetector[cornerIndex].setCorner(boardCorners[cornerIndex]);
            cornerDetector[cornerIndex].setCornerIndex(cornerIndex + 1);
        }
        processBoardCorners();
    }

    private void initializeUserInterface() {
        btnUndoLastMove = findViewById(R.id.btnUndoLastMove);
        btnUndoLastMove.setOnClickListener(this);
        btnUndoLastMove.setEnabled(false);
        btnRotateCounterClockwise = findViewById(R.id.btnRotateCounterClockwise);
        btnRotateCounterClockwise.setOnClickListener(this);
        btnRotateClockwise = findViewById(R.id.btnRotateClockwise);
        btnRotateClockwise.setOnClickListener(this);

        btnFinish = findViewById(R.id.btnFinish);
        btnFinish.setOnClickListener(this);
        btnResetBoardPosition = findViewById(R.id.btnResetBoardPosition);
        btnResetBoardPosition.setOnClickListener(this);

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        beepId = soundPool.load(this, R.raw.beep, 1);
    }

    private void initializeLogging() {
        LoggingConfiguration.activateLogging();
        LoggingConfiguration.activateLogging(LoggingConfiguration.RAW_CAMERA_IMAGE);
        LoggingConfiguration.activateLogging(LoggingConfiguration.CAMERA_IMAGE_WITH_BOARD_CONTOUR);
        LoggingConfiguration.activateLogging(LoggingConfiguration.ORTHOGONAL_BOARD_IMAGE);
        LoggingConfiguration.activateLogging(LoggingConfiguration.CORNER_POSITIONS);
        LoggingConfiguration.activateLogging(LoggingConfiguration.NUMBER_OF_QUADRILATERALS_FOUND_BY_BOARD_DETECTOR);

        fileHelper = new FileHelper(game);
        logger = new Logger(game, fileHelper);

        for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
            cornerDetector[cornerIndex].setFileHelper(fileHelper);
        }
    }

    private void processBoardCorners() {
        Point[] cornerPoints = new Point[4];
        for (int i = 0; i < 4; i++) {
            Ponto ponto = boardCorners[i].getRealCornerPosition();
            cornerPoints[i] = new Point(ponto.x, ponto.y);
        }

        boardPositionInImage = new Mat(4, 1, CvType.CV_32FC2);
        boardPositionInImage.put(0, 0,
                cornerPoints[0].x, cornerPoints[0].y,
                cornerPoints[1].x, cornerPoints[1].y,
                cornerPoints[2].x, cornerPoints[2].y,
                cornerPoints[3].x, cornerPoints[3].y);

        boardContour = new MatOfPoint(cornerPoints);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstaceState) {
        super.onRestoreInstanceState(savedInstaceState);
        fileHelper.restoreGameStoredTemporarily(game, boardCorners);
        processBoardCorners();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "RecordGameActivity.onPause");
        fileHelper.storeGameTemporarily(game, boardCorners);
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    public void onCameraViewStarted(int width, int height) {

    }

    public void onCameraViewStopped() {

    }

    /**
     * This method is called every time a camera frame is ready to be processed
     */
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        logger.startLoggingFrame();
        logger.setCameraFrame(inputFrame.rgba().clone());

        cameraFrame = inputFrame.rgba();
        Mat originalImage = inputFrame.rgba();

        if (isCornerTrackingActive) {
            for (int i = 0; i < 4; i++) {
                cornerDetector[i].setImageIndex((int) logger.getFrameNumber());
            }
            updateCornerPositions(originalImage.clone());
        }

        if (state == STATE_LOOKING_FOR_BOARD) {
            logger.addToLog("棋盘不在轮廓内");
            logger.addToLog("");
            if (orthogonalBoard != null) {
                orthogonalBoard.copyTo(originalImage.rowRange(0, 500).colRange(0, 500));
            }
            Drawer.drawLostBoardContour(originalImage, boardContour);
            Drawer.drawBoard(originalImage, game.getLastBoard(), 0, 500, 400, game.getLastMove());
            logger.log();
            return originalImage;
        }

        // 每秒处理两次
        if (System.currentTimeMillis() - timeOfLastImageProcessing < 500) {
            if (orthogonalBoard != null) {
                orthogonalBoard.copyTo(originalImage.rowRange(0, 500).colRange(0, 500));
            }
            Drawer.drawBoardContour(originalImage, boardContour);
            Drawer.drawBoard(originalImage, game.getLastBoard(), 0, 500, 400, game.getLastMove());
            logger.log();
            return originalImage;
        } else timeOfLastImageProcessing = System.currentTimeMillis();

        orthogonalBoard = ImageUtils.transformOrthogonally(originalImage, boardPositionInImage);
        logger.setOrtogonalBoardImage(orthogonalBoard.clone());

        // TODO: 在此处检查正交棋盘图像的大小 会在计算交叉点周围的平均颜色时影响半径大小

        stoneDetector.setBoardImage(orthogonalBoard);

        Board board = stoneDetector.detect(
                game.getLastBoard(),
                game.canNextMoveBe(Board.BLACK_STONE),
                game.canNextMoveBe(Board.WHITE_STONE)
        );

        // snapshotAtual = stoneDetector.snapshot.toString();
        logger.logCurrentBoardState();

        if (!paused) {

            if (lastDetectedBoard.equals(board)) {
                timeSinceLastBoardChange += SystemClock.elapsedRealtime() - timeOfLastBoardDetection;
                timeOfLastBoardDetection = SystemClock.elapsedRealtime();
                if (timeSinceLastBoardChange > timeLimit && game.addMoveIfItIsValid(board)) {
                    newMoveWasAdded();
                }
            } else {
                timeSinceLastBoardChange = 0;
                timeOfLastBoardDetection = SystemClock.elapsedRealtime();
            }

        }

        lastDetectedBoard = board;

        Drawer.drawBoardContour(originalImage, boardContour);
        logger.setCameraImageWithBoardContour(originalImage.clone());

        // 在屏幕上绘制正交棋盘
        if (orthogonalBoard != null) {
            orthogonalBoard.copyTo(originalImage.rowRange(0, 500).colRange(0, 500));
        }

        if (paused) {
            // 当它暂停时，绘制棋子探测器的当前输出（用于调试）
            Drawer.drawBoard(originalImage, board, 0, 500, 400, null);
        } else {
            Drawer.drawBoard(originalImage, game.getLastBoard(), 0, 500, 400, game.getLastMove());
        }

        logger.log();

        // 获得当前落子位置 由于同一图像可能识别多次才能识别出正确位置 所以要和上一次的已经确定的落子位置判重
        if (game.getLastMove() != null) {
            int moveX = game.getLastMove().column;
            int moveY = game.getLastMove().row;
            int movePlayer = game.getLastMove().color;
            if (moveX != curX && moveY != curY && curPlayer != movePlayer) {
                Log.d("testD", game.getLastMove().column + " " + game.getLastMove().row + " " + game.getLastMove().color);
                curX = moveX;
                curY = moveY;
                curPlayer = movePlayer;
                String movement = getAlphaIndex(curX, curY);
                Log.d("testD", "gtp command: " + (movePlayer == 1 ? "B" : "W") + " play " + movement);
            }

        }
        return originalImage;
    }

    private void updateCornerPositions(Mat image) {
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2BGR);

        Corner[] possibleNewCorners = new Corner[4];
        boolean wereAllCornersFound = true;
        for (int i = 0; i < 4; i++) {
            long start = System.currentTimeMillis();
            possibleNewCorners[i] = cornerDetector[i].detectCornerIn(image);
            long duration = System.currentTimeMillis() - start;
            if (possibleNewCorners[i] != null) {
                logger.addToLog(possibleNewCorners[i].toString());
            } else {
                logger.addToLog("Corner " + i + " was not found");
            }
            logger.addToLog("Time to process corner " + i + " = " + duration + "ms");
            if (possibleNewCorners[i] == null) {
                wereAllCornersFound = false;
            }
        }

        Mat orthogonalBoardImage = wereAllCornersFound
                ? ImageUtils.generateOrthogonalBoardImage(image, possibleNewCorners)
                : null;

        if (wereAllCornersFound && boardDetector.isBoardContainedIn(orthogonalBoardImage)) {
            logger.addToLog("Board is inside contour");
            int numberOfCornersThatMoved = getNumberOfCornersThatMoved(possibleNewCorners, boardCorners);
            logger.addToLog("Number of corners that moved: " + numberOfCornersThatMoved);
            int numberOfEmptyCornersThatMoved = getNumberOfEmptyCornersThatMoved(possibleNewCorners, boardCorners);
            logger.addToLog("Number of empty corners that moved: " + numberOfEmptyCornersThatMoved);
            double[] distanceToNewPoint = new double[4];
            for (int i = 0; i < 4; i++) {
                distanceToNewPoint[i] = possibleNewCorners[i].distanceTo(boardCorners[i]);
                logger.addToLog("Distance to old corner point " + (i + 1) + " = " + distanceToNewPoint[i]);
            }

            for (int i = 0; i < 4; i++) {
                if (numberOfCornersThatMoved < 4) {
                    // 不是所有的角落都移动了，所以这可能是一个角落调整用棋子更新可能角落的相对角落位置
                    if (possibleNewCorners[i].isStone) {
                        if (!boardCorners[i].isStone) {
                            possibleNewCorners[i].updateDisplacementVectorRelativeTo(boardCorners[i].position);
                        } else {
                            possibleNewCorners[i].updateDisplacementVectorRelativeTo(boardCorners[i].getRealCornerPosition());
                        }
                    }
                } else if (possibleNewCorners[i].isStone) {
                    // 所有角一起移动, 所以这可能是棋盘位移, 我但不会更新角与真实角的相对位置
                    possibleNewCorners[i].displacementToRealCorner = boardCorners[i].displacementToRealCorner;
                }
            }

            Mat orthogonalBoardImage2 = ImageUtils.generateOrthogonalBoardImage(image, possibleNewCorners);
            double similarity = lastValidOrthogonalBoardImage != null ? fingerprintMatching.calculateSimilatiryBetween(lastValidOrthogonalBoardImage, orthogonalBoardImage2) : -1;
            logger.addToLog("Similarity between new orthogonal board image to last valid one = " + similarity);
            logger.setOrtogonalBoardImage2(orthogonalBoardImage2);

            if (logger.getFrameNumber() <= 3 || numberOfFramesWithDissimilarOrthogonalImages >= 5 || fingerprintMatching.areImagesSimilar(lastValidOrthogonalBoardImage, orthogonalBoardImage2)) {
                // 此条件应基于时间而不是基于帧
                if (numberOfFramesWithDissimilarOrthogonalImages >= 5) {
                    logger.addToLog("Forcing orthogonal image to be similar");
                } else {
                    logger.addToLog("New orthogonal board image is similar to last valid one");
                }
                for (int i = 0; i < 4; i++) {
                    if (!possibleNewCorners[i].isStone) {
                        numberOfFramesWithoutStone[i]++;
                    } else {
                        numberOfFramesWithoutStone[i] = 0;
                    }

                    if (!boardCorners[i].isStone && !possibleNewCorners[i].isStone && numberOfCornersThatMoved < 3 && numberOfEmptyCornersThatMoved == 1) {
                        // 这意味着一个单独的空角会自行移动, 这是不可能的
                        logger.addToLog("Corner " + i + " - This empty corner moved by itself");
                        continue;
                    }
                    if (!possibleNewCorners[i].isStone && boardCorners[i].isStone && possibleNewCorners[i].distanceTo(boardCorners[i].getRealCornerPosition()) > MOVEMENT_THRESHOLD
                            // 这个条件应该是基于时间而不是基于帧的, 比如 2 或 3 秒左右
                            && numberOfFramesWithoutStone[i] < 5
                    ) {
                        // 如果一个角是原本棋子而被提掉, 则新的空角应该与棋子所在的真实角位置相匹配。
                        logger.addToLog("Corner " + i + " - This now empty corner is in a wrong position");
                        logger.addToLog("Number of frames without stone = " + numberOfFramesWithoutStone[i]);
                        continue;
                    }
                    boardCorners[i] = possibleNewCorners[i];
                    cornerDetector[i].setCorner(possibleNewCorners[i]);
                }
                numberOfFramesWithDissimilarOrthogonalImages = 0;
                lastValidOrthogonalBoardImage = orthogonalBoardImage2.clone();
                logger.setLastValidOrtogonalBoardImage(lastValidOrthogonalBoardImage);
            } else {
                logger.addToLog("New orthogonal board image is NOT similar to last valid one");
                numberOfFramesWithDissimilarOrthogonalImages++;
            }

            processBoardCorners();
            state = STATE_RUNNING;
        } else {
            state = STATE_LOOKING_FOR_BOARD;
            logger.addToLog("Board is NOT inside contour");
            logger.addToLog("were all corners found = " + wereAllCornersFound);
        }

        logger.logCornerPositions(boardCorners);
    }

    private int getNumberOfCornersThatMoved(Corner[] possibleNewCorners, Corner[] corners) {
        int numberOfCornersThatMoved = 0;
        for (int i = 0; i < 4; i++) {
            if (possibleNewCorners[i].distanceTo(corners[i]) > MOVEMENT_THRESHOLD) {
                numberOfCornersThatMoved++;
            }
        }
        return numberOfCornersThatMoved;
    }

    private int getNumberOfEmptyCornersThatMoved(Corner[] possibleNewCorners, Corner[] corners) {
        int numberOfEmptyCornersThatMoved = 0;
        for (int i = 0; i < 4; i++) {
            if (!possibleNewCorners[i].isStone
                    // && !corners[i].isStone
                    && possibleNewCorners[i].distanceTo(corners[i]) > MOVEMENT_THRESHOLD) {
                numberOfEmptyCornersThatMoved++;
            }
        }
        return numberOfEmptyCornersThatMoved;
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnUndoLastMove) {
            areYouSureYouWantToUndoTheLastMove(getString(R.string.btn_undo_last_move));
        } else if (id == R.id.btnRotateCounterClockwise) {
            rotate(-1);
        } else if (id == R.id.btnRotateClockwise) {
            rotate(1);
        } else if (id == R.id.btnFinish) {
            areYouSureYouWantToFinishRecording();
        } else if (id == R.id.btnResetBoardPosition) {
            resetCornersToTheirOriginalPositions();
        }
    }

    private void areYouSureYouWantToUndoTheLastMove(String message) {
        SmileDialog dialog = new SmileDialogBuilder(this, SmileDialogType.WARNING)
                .hideTitle(true)
                .setContentText(message)
                .setConformBgResColor(com.irlab.base.R.color.warning)
                .setConformTextColor(Color.WHITE)
                .setCancelTextColor(Color.BLACK)
                .setCancelButton("取消")
                .setCancelBgResColor(R.color.whiteSmoke)
                .setWindowAnimations(R.style.dialog_style)
                .setConformButton("确定", () -> {
                    Move move = game.undoLastMove();
                    timeSinceLastBoardChange = 0;
                    timeOfLastBoardDetection = SystemClock.elapsedRealtime();
                    updateUndoButton();
                    logger.addToLog("Undoing last move" + move);
                })
                .build();
        dialog.show();
        // 更新当前步
        if (game.getLastMove() != null) {
            int moveX = game.getLastMove().column;
            int moveY = game.getLastMove().row;
            int movePlayer = game.getLastMove().color;
            curX = moveX;
            curY = moveY;
            curPlayer = movePlayer;
        }
    }

    private void rotate(int direction) {
        logger.addToLog("Rotated board in direction " + direction);

        Corner[] rotatedBoardCorners = new Corner[4];
        for (int i = 0; i < 4; i++) {
            rotatedBoardCorners[i] = new Corner();
        }

        // 逆时针旋转
        if (direction == -1) {
            rotatedBoardCorners[0] = boardCorners[1];
            rotatedBoardCorners[1] = boardCorners[2];
            rotatedBoardCorners[2] = boardCorners[3];
            rotatedBoardCorners[3] = boardCorners[0];
        }
        // 顺时针旋转
        else if (direction == 1) {
            rotatedBoardCorners[0] = boardCorners[3];
            rotatedBoardCorners[1] = boardCorners[0];
            rotatedBoardCorners[2] = boardCorners[1];
            rotatedBoardCorners[3] = boardCorners[2];
        }

        boardCorners = rotatedBoardCorners;
        for (int i = 0; i < 4; i++) {
            cornerDetector[i].setCorner(boardCorners[i]);
        }
        processBoardCorners();

        lastValidOrthogonalBoardImage = ImageUtils.rotateImage(lastValidOrthogonalBoardImage, direction);
        logger.setLastValidOrtogonalBoardImage(lastValidOrthogonalBoardImage);
        game.rotate(direction);
    }

    private void areYouSureYouWantToFinishRecording() {
        SmileDialog dialog = new SmileDialogBuilder((AppCompatActivity) this, SmileDialogType.WARNING)
                .hideTitle(true)
                .setContentText(getString(R.string.dialog_finish_recording))
                .setConformBgResColor(com.irlab.base.R.color.warning)
                .setConformTextColor(Color.WHITE)
                .setCancelTextColor(Color.BLACK)
                .setCancelButton("取消")
                .setCancelBgResColor(R.color.whiteSmoke)
                .setWindowAnimations(R.style.dialog_style)
                .setConformButton("确定", () -> {
                    saveGameRecordOnDiskAndExit();
                })
                .build();
        dialog.show();
    }

    /**
     * Saves the game record and log in secondary storage.
     */
    private void saveGameRecordOnDiskAndExit() {
        saveGameRecordOnDisk();
        Intent intent = new Intent(getApplicationContext(), MainView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void saveGameRecordOnDisk() {
        if (fileHelper.saveGameFile(game)) {
            // 必须在 UI 线程中运行，因为在显示 toast 后可能会关闭活动
            runOnUiThread(new Runnable() {
                public void run() {
                }
            });
        }
    }

    // TODO: Check if this is being called
    @Override
    public void onBackPressed() {
        Log.d(TAG, "RecordGameActivity.onBackPressed()");
        areYouSureYouWantToFinishRecording();
    }

    private Move processManualMove(String moveString) {
        moveString = moveString.trim();
        if (moveString.length() != 3) return null;
        moveString = moveString.toLowerCase();
        int color = moveString.charAt(0) == 'b' ? Board.BLACK_STONE : Board.WHITE_STONE;
        int row = moveString.charAt(1) - 'a';
        int column = moveString.charAt(2) - 'a';
        return new Move(row, column, color);
    }

    private void newMoveWasAdded() {
        moveCounter++;
        soundPool.play(beepId, 1, 1, 0, 0, 1);
        saveGameRecordOnDisk();
        updateUndoButton();
    }

    private void updateUndoButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnUndoLastMove.setEnabled(game.getNumberOfMoves() > 0);
            }
        });
    }

    private void resetCornersToTheirOriginalPositions() {
        logger.addToLog("Resetting corners to their original positions");
        for (int i = 0; i < 4; i++) {
            boardCorners[i].copy(originalBoardCorners[i]);
        }
        Toast.makeText(
                RecordGameActivity.this,
                R.string.toast_restore_original_corner_positions,
                Toast.LENGTH_LONG
        ).show();
        processBoardCorners();
    }

    private String getAlphaIndex(int x, int y) {
        String res = "";
        int j = 0;
        for (char i = 'A'; i <= 'S'; i ++, j ++ ) {
            if (x == j) {
                res += i + " " + (y + 1);
                break;
            }
        }
        return res;
    }
}