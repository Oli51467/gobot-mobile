package com.irlab.base.response;

public enum ResponseCode {
    SUCCESS(200, "成功"),

    SERVER_FAILED(400, "服务器异常"),
    FIND_MARKER(1000, "已确定棋盘位置"),
    NOT_FIND_MARKER(1001, "未确定棋盘位置，请调整后重试"),

    SAVE_SGF_SUCCESSFULLY(20001, "保存棋谱成功"),
    ENGINE_CONNECT_SUCCESSFULLY(20002, "引擎连接成功"),
    PLAY_PASS_TO_ENGINE_SUCCESSFULLY(20003, "落子已传递给引擎"),
    ENGINE_PLAY_SUCCESSFULLY(20004, "引擎已落子"),
    GET_PLAY_CONFIG_SUCCESSFULLY(20005, null),
    SHOW_BOARD_SUCCESSFULLY(20006, "展示棋盘"),
    ENGINE_RESIGN(20007, "引擎认输"),
    ENGINE_PASS(20008, "引擎停一手"),
    LOAD_CONFIG_SUCCESSFULLY(20009, "加载配置数据成功"),

    ENGINE_CONNECT_FAILED(40001, "引擎连接失败"),
    PLAY_PASS_TO_ENGINE_FAILED(40002, "落子传递引擎失败"),
    ENGINE_PLAY_FAILED(40003, "引擎落子失败"),
    SHOW_BOARD_FAILED(40004, "展示棋盘失败"),
    CANNOT_PLAY(40005, "这里不能落子"),

    BLUETOOTH_SERVICE_FAILED(50001, "蓝牙服务获取失败"),

    ;

    /**
     * 状态码
     */
    private int code;
    /**
     * 返回信息
     */
    private String msg;

    ResponseCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
