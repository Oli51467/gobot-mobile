import numpy as np
import math


# 最小二乘仿射变换函数
def my_getAffineTransform(src, dst):
    m = len(src)
    A = np.zeros((2 * m, 6))
    b = np.zeros((2 * m, 1))
    for i in range(m):
        for j in range(2):
            if j == 0:
                A[2 * i + j][0] = src[i][0]
                A[2 * i + j][1] = src[i][1]
                A[2 * i + j][4] = 1
                b[2 * i + j][0] = dst[i][0]
            if j == 1:
                A[2 * i + j][2] = src[i][0]
                A[2 * i + j][3] = src[i][1]
                A[2 * i + j][5] = 1
                b[2 * i + j][0] = dst[i][1]
    Naa = np.linalg.det((A.T).dot(A))
    if Naa < 0.1:
        return 0
    Nbb = np.linalg.inv((A.T).dot(A))
    x = (Nbb.dot(A.T)).dot(b)

    M = np.zeros((2, 3))
    M[0][0] = x[0][0]
    M[0][1] = x[1][0]
    M[0][2] = x[4][0]
    M[1][0] = x[2][0]
    M[1][1] = x[3][0]
    M[1][2] = x[5][0]

    return M


# 计算仿射变换矩阵与单点相乘的结果
def PointAffineTransform(M, P):
    x = M[0][0] * P[0] + M[0][1] * P[1] + M[0][2]
    y = M[1][0] * P[0] + M[1][1] * P[1] + M[1][2]
    return x, y


# 将二进制list转换为最小整数的函数
def B2I(array, N):
    min_value = 1000000
    temp = 0
    for i in range(0, N):
        temp = 0
        for j in range(0, N):
            if array[j] == 1:
                temp = temp + 2 ** j
        if temp < min_value:
            min_value = temp
        array = MoveBit(array, 1)

    for i in range(0, N):
        temp = 0
        for j in range(0, N):
            if array[j] == 1:
                temp = temp + 2 ** j
        if temp == min_value:
            break
        array = MoveBit(array, 1)
    return min_value


# 将整数转换为2进制list的函数
def I2B(value, N):
    # 指定list的长度
    array = [' '] * N
    for i in range(0, N):
        if value > 0:
            array[i] = value % 2
            # python中的整除，向下取整
            value = value // 2
        else:
            array[i] = 0
    return array


# 将list向左移位函数
def MoveBit(lst, k):
    temp = lst[:]
    for i in range(k):
        temp.append(temp.pop(0))
    return temp


# 反转编码的函数：0->1,1->0,为黑色码带设计
def swap0and1(code_list):
    result = []
    for i in code_list:
        if i == 0:
            result.append(1)
        if i == 1:
            result.append(0)
    return result
