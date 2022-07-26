package com.irlab.view.processing.initialBoardDetector;

import org.opencv.core.CvType;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 定义四边形空间层次结构, 指示哪些在哪些内部
 */
public class QuadrilateralHierarchy {

    public Map<MatOfPoint, List<MatOfPoint>> hierarchy = new HashMap<>();
    public List<MatOfPoint> leaves = new ArrayList<>();
    public List<MatOfPoint> externals = new ArrayList<>();

    // 构建提供的四边形的层次结构。
    public QuadrilateralHierarchy(List<MatOfPoint> quadrilaterals) {
        for (MatOfPoint quadrilateral : quadrilaterals) {
            hierarchy.put(quadrilateral, new ArrayList<>());
            for (MatOfPoint otherQuadrilateral : quadrilaterals) {
                if (quadrilateral == otherQuadrilateral) continue;
                if (isInside(quadrilateral, otherQuadrilateral)) {
                    hierarchy.get(quadrilateral).add(otherQuadrilateral);
                }
            }
        }

        Iterator it = hierarchy.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            List<MatOfPoint> valor = (List<MatOfPoint>)pair.getValue();
            if (valor.size() == 0) {
                this.leaves.add((MatOfPoint) pair.getKey());
            }
            else {
                this.externals.add((MatOfPoint) pair.getKey());
            }
        }

    }

    // 检查一个四边形是否在另一个里面
    private boolean isInside(MatOfPoint externalQuadrilateral, MatOfPoint internalQuadrilateral) {
        final double IS_INSIDE_CONTOUR = 1;
        double result;
        MatOfPoint2f externalQuadrilateral2f = new MatOfPoint2f();
        externalQuadrilateral.convertTo(externalQuadrilateral2f, CvType.CV_32FC2);
        for (Point point : internalQuadrilateral.toList()) {
            result = Imgproc.pointPolygonTest(externalQuadrilateral2f, point, false);
            if (result != IS_INSIDE_CONTOUR) {
                return false;
            }
        }
        return true;
    }
}
