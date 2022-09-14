from Support import *
import numpy as np
import cv2
import math
import sys
import os


def main(byte_array):
    img = np.asarray(byte_array, dtype="uint8")
    img = cv2.imdecode(img, -1)  # 从指定的内存缓存中读取数据, 并把数据转换(解码)成图像格式, 主要用于从网络传输数据中恢复出图像。

    code_table = []  # 存放解码结果的list
    n, r, color = 12, 0.85, 'white'

    '''
    image.shape[0], 图片垂直尺寸
    image.shape[1], 图片水平尺寸
    image.shape[2], 图片通道数
    '''
    img_shape = img.shape
    img_height = img_shape[0]
    img_width = img_shape[1]

    # 将输入图像转换为灰度图
    img_gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    # 使用Otsu对图像进行自适应二值化
    ret_val, img_binary = cv2.threshold(img_gray, 0, 1, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

    # 使用findcontours函数对二值化后的图像进行轮廓提取,第三个参数为轮廓点的存储方式，这里选返回所有轮廓点，方便后面做筛选
    contours, hierarchy = cv2.findContours(img_binary, cv2.RETR_LIST, cv2.CHAIN_APPROX_NONE)
    # 遍历提取出来的轮廓，筛选其中的椭圆轮廓
    for contour in contours:
        area = cv2.contourArea(contour, False)
        length = cv2.arcLength(contour, True)
        # 计算轮廓的圆度
        r0 = 2 * math.sqrt(math.pi * area) / (length + 1)
        if r0 < r:
            continue
        if len(contour) < 20:
            continue
        # 将轮廓点集转换为numpy数组
        e_points = np.array(contour)
        # 得到拟合的椭圆参数：中心点坐标，尺寸，旋转角
        box1 = cv2.fitEllipse(e_points)
        box2 = tuple([box1[0], tuple([box1[1][0] * 2, box1[1][1] * 2]), box1[2]])
        box3 = tuple([box1[0], tuple([box1[1][0] * 3, box1[1][1] * 3]), box1[2]])
        # 求得最外层椭圆的最小外接矩形的四个顶点，顺时针方向
        min_rect = cv2.boxPoints(box3)
        # 计算椭圆的长轴
        a = max(box3[1][0], box3[1][1])
        s = a
        # 在原图像中裁剪CCT所在的区域
        cct_roi = None
        row_min = round(box1[0][1] - s / 2)
        row_max = round(box1[0][1] + s / 2)
        col_min = round(box1[0][0] - s / 2)
        col_max = round(box1[0][0] + s / 2)
        # 判断cct_roi是否超出原图像边界
        if row_min >= 0 and row_max <= img_height and col_min >= 0 and col_max <= img_width:
            # 从原图像中将cct_roi截取出来
            cct_roi = img[row_min:row_max, col_min:col_max]
            # cct_roi相对于原始影像的偏移量
            dx = box1[0][0] - s / 2
            dy = box1[0][1] - s / 2
            # 对CCT椭圆区域进行仿射变换将其变为正圆
            src = np.float32(
                [[min_rect[0][0] - dx, min_rect[0][1] - dy],
                 [min_rect[1][0] - dx, min_rect[1][1] - dy],
                 [min_rect[2][0] - dx, min_rect[2][1] - dy],
                 [min_rect[3][0] - dx, min_rect[3][1] - dy],
                 [box1[0][0] - dx, box1[0][1] - dy]])
            dst = np.float32(
                [[box1[0][0] - a / 2 - dx, box1[0][1] - a / 2 - dy],
                 [box1[0][0] + a / 2 - dx, box1[0][1] - a / 2 - dy],
                 [box1[0][0] + a / 2 - dx, box1[0][1] + a / 2 - dy],
                 [box1[0][0] - a / 2 - dx, box1[0][1] + a / 2 - dy],
                 [box1[0][0] - dx, box1[0][1] - dy]])
            # 得到仿射变换矩阵
            m = my_getAffineTransform(src, dst)
            if isinstance(m, int):
                continue
            # 计算仿射变换后的中心点坐标
            x0, y0 = PointAffineTransform(m, [box1[0][0] - dx, box1[0][1] - dy])
            cct_img = None
            # 对cct_roi进行仿射变换
            cct_roi_size = np.shape(cct_roi)
            if cct_roi_size[0] > 0 and cct_roi_size[1] > 0:
                cct_img = cv2.warpAffine(cct_roi, m, (round(s), round(s)))
            # 对仿射变换后的CCT进行缩放
            cct_large = cv2.resize(cct_img, (0, 0), fx=200.0 / s, fy=200.0 / s,
                                   interpolation=cv2.INTER_LANCZOS4)
            # 将放大后的CCT转换为灰度图
            cct_gray = cv2.cvtColor(cct_large, cv2.COLOR_BGR2GRAY)
            # 对该灰度图进行自适应二值化
            ret_val, cct_binary = cv2.threshold(cct_gray, 0, 1, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
            kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3))
            # 执行腐蚀
            cct_eroded = cv2.erode(cct_binary, kernel)
            # 判断这个区域里是不是CCT
            if cct_or_not(cct_eroded):
                # 调用解码函数进行解码
                code = cct_decode(cct_eroded, n, color)
                if code < 500 and code not in code_table:
                    code_table.append([code, box1[0][0], box1[0][1]])
                # 将编码在原图像中绘制出来.各参数依次是：图片，添加的文字，左上角坐标，字体，字体大小，颜色，字体粗细
                cv2.putText(img, str(code), (int(box3[0][0] - 0.25 * s), int(box1[0][1] + 0.5 * s)),
                            cv2.FONT_HERSHEY_COMPLEX, 1, (0, 0, 255), 2)
                # 绘制拟合出的椭圆
                cv2.ellipse(img, box1, (0, 255, 0), 1)
                cv2.ellipse(img, box2, (0, 255, 0), 1)
                cv2.ellipse(img, box3, (0, 255, 0), 1)
    return code_table


# 判断给定图像中是否有CCT
def cct_or_not(cct_img):
    sample_num = 36
    # 得到cct图像的尺寸
    shape = np.shape(cct_img)
    height = shape[0]
    width = shape[1]
    # 得到中心点坐标，该坐标可以视为CCT中心坐标
    x0 = width / 2
    y0 = height / 2
    # 得到CCT中心的半径
    r1 = x0 / 3.0
    # 存放三个圆周上采样点像素值的列表
    pixels_0_5_r = []
    pixels_1_5_r = []
    pixels_2_5_r = []
    # 遍历三个圆周处的采样点像素
    for j in range(sample_num):
        xi = math.cos(10.0 * j / 180 * math.pi)
        yi = math.sin(10.0 * j / 180 * math.pi)

        x_0_5_r = 0.5 * r1 * xi + x0
        y_0_5_r = 0.5 * r1 * yi + y0

        x_1_5_r = 1.5 * r1 * xi + x0
        y_1_5_r = 1.5 * r1 * yi + y0

        x_2_5_r = 2.5 * r1 * xi + x0
        y_2_5_r = 2.5 * r1 * yi + y0

        # 访问像素【rows,cols】先行后列
        pixel_value_0_5_r = cct_img[round(y_0_5_r)][round(x_0_5_r)]
        pixel_value_1_5_r = cct_img[round(y_1_5_r)][round(x_1_5_r)]
        pixel_value_2_5_r = cct_img[round(y_2_5_r)][round(x_2_5_r)]

        pixels_0_5_r.append(pixel_value_0_5_r)
        pixels_1_5_r.append(pixel_value_1_5_r)
        pixels_2_5_r.append(pixel_value_2_5_r)

    if sum(pixels_0_5_r) == sample_num and sum(pixels_1_5_r) == 0 and sum(pixels_2_5_r) >= 2:
        return True
    elif sum(pixels_0_5_r) == 0 and sum(pixels_1_5_r) == sample_num and sum(
            pixels_2_5_r) <= sample_num - 2:
        return True
    else:
        return False


def cct_decode(cct_img, n, color):
    # 得到cct图像的尺寸
    shape = np.shape(cct_img)
    height = shape[0]
    width = shape[1]
    # 得到中心点坐标，该坐标可以视为CCT中心坐标
    x0 = width / 2
    y0 = height / 2
    # 得到CCT的三个半径
    r1 = x0 * 0.333333
    # 存放所有码值的list
    code_all = []
    # 如果是12位编码，那就转30圈，每圈加一度
    for j in range(int(360 / n)):
        code_j = []  # 存放单圈码值的list
        # 以N为标准在2.5倍r1的圆环上均匀采样
        for k in range(n):
            x = 2.5 * r1 * math.cos((360.0 / n * k + j) / 180 * math.pi) + x0
            y = 2.5 * r1 * math.sin((360.0 / n * k + j) / 180 * math.pi) + y0
            # 访问像素【rows,cols】先行后列
            pixel_value = cct_img[round(y)][round(x)]
            code_j.append(pixel_value)
        # 将每一圈得到的编码都转换为最小编码
        temp1 = B2I(code_j, n)
        temp2 = I2B(temp1, n)
        code_all.append(temp2)
    code = np.asarray(code_all)
    # 对列求平均
    code = np.mean(code, 0)
    # 对cct编码进行二值化
    result = []
    for i in code:
        if i > 0.5:
            result.append(1)
        if i <= 0.5:
            result.append(0)
    if color == 'black':
        result = swap0and1(result)
    # 调用DrawCCT中的解码函数进行解码
    return B2I(result, len(result))
