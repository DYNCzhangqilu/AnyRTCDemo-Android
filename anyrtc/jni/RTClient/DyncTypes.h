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
	DE_CONNECT_OK = 200,	// ���ӳɹ�
	DE_CONNECT_BAD_REQ,		// ����֧�ֵĴ�������
	DE_CONNECT_AUTH_FAIL,	// ��֤ʧ��
	DE_CONNECT_NO_USER,		// �˿����߲�����
	DE_CONNECT_SVR_ERR,		// �������ڲ�����
	DE_CONNECT_SQL_ERR,		// �������ڲ����ݿ����
	DE_CONNECT_ARREARS,		// �˺�Ƿ��
	DE_CONNECT_LOCKED,		// �˺ű�����
	//-------------------
	// P2P
	DE_P2P_CALL_OK = 300,	// ���н����ɹ�
	DE_P2P_NOT_FOUND,		// ϵͳû���������
	DE_P2P_MEM_FULL,		// ��Ա����
	DE_P2P_LIMITED,			// �˺�(Ƿ��,�Ƿ�����)����

};

typedef enum UserOption
{
	OPT_CALL = 0,
	OPT_MEET,
	OPT_LIVE,
	OPT_LINE
}UserOption;

#endif	// __DYNC_TYPES_H__