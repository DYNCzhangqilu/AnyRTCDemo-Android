package org.anyrtc;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * Created by Eric on 2016/1/2.
 */
public class AnyRTC {
    public static String gSvrAddr = "139.129.11.100";//"cloud.anyrtc.io";//"192.168.199.110";//
    public static int    gSvrPort = 9060;
    public static int    gScrnWidth = 0;
    public static int    gScrnHeight = 0;
    public static Boolean gDebug = false;
    public static Boolean gAutoBitrate = false;
    public static String gStrDeveloperId;
    public static String gStrToken;
    public static String gStrAESKey;
    public static String gStrAppId;

    /** 初始化AnyRTC
     * @param strDeveloperId	开发者ID
     * @param strToken			APP Token
     * @param strAESKey			APP Key
     * @param strAppId			APP ID
     */
    public static void InitAnyRTCWithAppKey(String strDeveloperId, String strToken,
                                            String strAESKey, String strAppId) {
        gStrDeveloperId = strDeveloperId;
        gStrToken = strToken;
        gStrAESKey = strAESKey;
        gStrAppId = strAppId;
    }

    public static String GetAnyRTCSdkVersion() {
        return "v0.0.2.20160428";
    }

    public enum AnyRTCErrorCode {
        AnyRTC_UNKNOW(0),       // 未知错误
        AnyRTC_NET_ERR(100),    // 网络错误
        AnyRTC_LIVE_ERR(101),   // 直播出错
        AnyRTC_BAD_REQ(201),    // 服务不支持的错误请求
        AnyRTC_AUTH_FAIL(202),	// 认证失败
        AnyRTC_NO_USER(203),	// 此开发者信息不存在
        AnyRTC_SQL_ERR(204),	// 服务器内部数据库错误
        AnyRTC_ARREARS(205),	// 账号欠费
        AnyRTC_LOCKED(206),		// 账号被锁定
        AnyRTC_FORCE_EXIT(207); // 强制离开
        private int value = 0;
        private AnyRTCErrorCode(int value) {
            //*必须是private的，否则编译错误
            this.value = value;
        }

        public int Value(){return value;};

        public static AnyRTCErrorCode valueOf(int value) {    //    手写的从int到enum的转换函数
            switch (value) {
                case 0:
                    return AnyRTC_UNKNOW;
                case 100:
                    return AnyRTC_NET_ERR;
                case 101:
                    return AnyRTC_LIVE_ERR;
                case 201:
                    return AnyRTC_BAD_REQ;
                case 202:
                    return AnyRTC_AUTH_FAIL;
                case 203:
                    return AnyRTC_NO_USER;
                case 204:
                    return AnyRTC_SQL_ERR;
                case 205:
                    return AnyRTC_ARREARS;
                case 206:
                    return AnyRTC_LOCKED;
                case 207:
                    return AnyRTC_FORCE_EXIT;
                default:
                    return null;
            }
        }
    }
    public static String GetErrString(int code) {
        if(!gDebug){
            return "如需具体错误信息请至平台开启开发者模式!";
        }
        String errInfo = "系统未知错误,请更新SDK或查阅官方文档!";
        switch(code) {
            case 100:
                errInfo = "网络异常错误";
                break;
            case 101:
                errInfo = "直播发生错误";
                break;
            case 201:
                errInfo = "服务不支持的错误请求";
                break;
            case 202:
                errInfo = "认证失败";
                break;
            case 203:
                errInfo = "此开发者信息不存在";
                break;
            case 204:
                errInfo = "服务器内部数据库错误";
                break;
            case 205:
                errInfo = "账号欠费";
                break;
            case 206:
                errInfo = "账号被锁定";
                break;
            case 207:
                errInfo = "用户被强制离开，可能使用的是测试账号，请至www.anyrtc.io注册开发者账号";
                break;
        }
        return errInfo;
    }

    /** Helper method for byteBuffer to String. */
    public static String byteBufferToString(ByteBuffer buffer) {
        CharBuffer charBuffer = null;
        try {
            Charset charset = Charset.forName("UTF-8");
            CharsetDecoder decoder = charset.newDecoder();
            charBuffer = decoder.decode(buffer);
            buffer.flip();
            return charBuffer.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }
}
