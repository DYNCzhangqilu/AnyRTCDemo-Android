#ifndef __DYNC_TYPES_H__
#define __DYNC_TYPES_H__

enum DYNC_CMD
{
	//-------------------
	DC_MESSAGE = 1001,
	DC_PUBLISH,
	DC_UNPUBLISH ,
	DC_SUBSCRIBE,
	DC_UNSUBSCRIBE,
	DC_SDP_INFO,
	//-------------------
	// P2P
	DC_MAKE_CALL = 2001,
	DC_NOTIFY_CALL,
	DC_END_CALL,
	//-------------------
	// Meeting
	DC_JOIN_MEET = 3001,
	DC_NOTIFY_MEET,
	DC_LEAVE_MEET,
	//-------------------
	// Liveing
	DC_JOIN_LIVE = 4001,
	DC_NOTIFY_LIVE,
	DC_LEAVE_LIVE,
	//-------------------
	// Lineing
	DC_JOIN_LINE = 5001,
	DC_NOTIFY_LINE,
	DC_LEAVE_LINE,
	//-------------------
	// 
	DC_SWITCH_V_BITS = 6001,
};

enum DYNC_CODE
{
	DE_CONNECT_OK = 200,	// 连接成功
	DE_CONNECT_BAD_REQ,		// 服务不支持的错误请求
	DE_CONNECT_AUTH_FAIL,	// 认证失败
	DE_CONNECT_NO_USER,		// 此开发者不存在
	DE_CONNECT_SVR_ERR,		// 服务器内部错误
	DE_CONNECT_SQL_ERR,		// 服务器内部数据库错误
	DE_CONNECT_ARREARS,		// 账号欠费
	DE_CONNECT_LOCKED,		// 账号被锁定
	//-------------------
	// P2P
	DE_P2P_CALL_OK = 300,	// 呼叫建立成功
	DE_P2P_NOT_FOUND,		// 系统没有这个呼叫
	DE_P2P_MEM_FULL,		// 人员已满
	DE_P2P_LIMITED,			// 账号(欠费,非法操作)受限

};

typedef enum UserOption
{
	OPT_CALL = 0,
	OPT_MEET,
	OPT_LIVE,
	OPT_LINE
}UserOption;

#endif	// __DYNC_TYPES_H__