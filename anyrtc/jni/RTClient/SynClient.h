#ifndef __SYNC_CLIENT_H__
#define __SYNC_CLIENT_H__
#include "SyncTypes.h"
#include "SyncMsgCrypt.h"

class SynClient
{
public:
	SynClient(int maxEv = 5);
	virtual ~SynClient();

	void	DoTick();

	void	SetEncrypt(const std::string &sToken, const std::string &sEncodingAESKey, const std::string &sAppid);

	void	Clear();

public:
	virtual void OnConnected(int code, const std::string&strUserData) = 0;
	virtual void OnMessageSend(const char*pMsg, int nLen) = 0;
	virtual void OnMessageRecved(int cmd, MapParams& params, const std::string& strContent) = 0;
	virtual void OnDisconnect() = 0;

protected:
	void	Connect(const std::string&strAuthTag, const std::string&strUserData);
	void	SendMessageX(int cmd, bool encrypt, const MapParams& params, const std::string& strContent);
	void	RecvMessageX(const char*pData, int nLen);
	void	Disconnect();

private:
	void	DoAck(int seqn);
	void	DoSync();
	void	DoPackMessage();
	void	RePackMessage();

private:
	bool		m_bEncrypt;
	bool		m_bConnected;
	int			m_nMaxEv;
	int			m_nLocalSeqn;
	int			m_nRemoteSeqn;
	uint32_t	m_nTimestamp;

	anyrtc::SyncMsgCrypt	*m_pEncrypt;

	ListMessage				m_lstMsgWait2Send;
	ListMessage				m_lstMsgWait4Ack;
};

#endif	// __SYNC_CLIENT_H__