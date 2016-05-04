#include "RTClient.h"
#include "DyncTypes.h"
#include "webrtc/base/base64.h"
#include "webrtc/base/md5digest.h"
#include "webrtc/base/timeutils.h"
#include "rapidjson/document.h"
#include "rapidjson/prettywriter.h"	
#include "rapidjson/stringbuffer.h"

const int		kRequestBufferSizeInBytes = 2048;
#define FREE_PTR(ptr) \
    if (NULL != (ptr)) {\
        free (ptr);\
        (ptr) = NULL;\
	    }

#define DELETE_PTR(ptr) \
    if (NULL != (ptr)) {\
        delete (ptr);\
        (ptr) = NULL;\
	    }
static void writeShort(char** pptr, unsigned short anInt)
{
	**pptr = (char)(anInt / 256);
	(*pptr)++;
	**pptr = (char)(anInt % 256);
	(*pptr)++;
}

static unsigned short readShort(char** pptr)
{
	char* ptr = *pptr;
	unsigned short len = 256 * ((unsigned char)(*ptr)) + (unsigned char)(*(ptr + 1));
	*pptr += 2;
	return len;
}

static void GetUserOptionType(UserOption opt, std::string&strType) {
	switch (opt) {
	case OPT_CALL:
		strType = "Calling";
		break;
	case OPT_MEET:
		strType = "Meeting";
		break;
	case OPT_LIVE:
		strType = "Liveing";
		break;
	case OPT_LINE:
		strType = "Lineing";
		break;
	};
}

static int GetUserJoinOption(UserOption opt, std::string&strType) {
	int cmd = 0;
	GetUserOptionType(opt, strType);
	switch (opt) {
	case OPT_CALL:
		cmd = DC_MAKE_CALL;
		break;
	case OPT_MEET:
		cmd = DC_JOIN_MEET;
		break;
	case OPT_LIVE: 
		cmd = DC_JOIN_LIVE;
		break;
	case OPT_LINE: 
		cmd = DC_JOIN_LINE;
		break;
	};
	return cmd;
}
static int GetUserNotifyOption(UserOption opt, std::string&strType) {
	int cmd = 0;
	GetUserOptionType(opt, strType);
	switch (opt) {
	case OPT_CALL:
		cmd = DC_NOTIFY_CALL;
		break;
	case OPT_MEET:
		cmd = DC_NOTIFY_MEET;
		break;
	case OPT_LIVE:
		cmd = DC_NOTIFY_LIVE;
		break;
	case OPT_LINE:
		cmd = DC_NOTIFY_LINE;
		break;
	};
	return cmd;
}
static int GetUserLeaveOption(UserOption opt, std::string&strType) {
	int cmd = 0;
	GetUserOptionType(opt, strType);
	switch (opt) {
	case OPT_CALL:
		cmd = DC_END_CALL;
		break;
	case OPT_MEET:
		cmd = DC_LEAVE_MEET;
		break;
	case OPT_LIVE:
		cmd = DC_LEAVE_LIVE;
		break;
	case OPT_LINE:
		cmd = DC_LEAVE_LINE;
		break;
	};
	return cmd;
}

RTClient::RTClient()
	: SynClient(10)
	, m_nTime2disconnect(0)
	, m_pTcpClient(NULL)
	, m_nBufOffset(0)
	, m_bSyncConnected(false)
{
	m_nBufLen = kRequestBufferSizeInBytes;
	m_pBuffer = new char[m_nBufLen];
	m_nParseBufLen = kRequestBufferSizeInBytes;
	m_pParseBuf = new char[m_nParseBufLen];
}

RTClient::~RTClient()
{
	if (m_pTcpClient)
		m_pTcpClient->Disconnect();
	DELETE_PTR(m_pTcpClient);

	delete[] m_pBuffer;
	delete[] m_pParseBuf;
}

int RTClient::ConnectStatus()
{
	if (m_pTcpClient == NULL)
		return 0;

	return m_pTcpClient->Status();
}

void RTClient::Connect(const std::string&strSvrAddr, int nSvrPort, const std::string&developerId, const std::string&strToken, const std::string&strAESKey, const std::string&strAppId)
{
	{// Init data.
		m_strSvrAddr = strSvrAddr;
		m_nSvrPort = nSvrPort;
		m_strDeveloperId = developerId;
		m_strAppId = strAppId;
		m_strToken = strToken;
	}
	
	SynClient::Clear();
	SynClient::SetEncrypt(strToken, strAESKey, strAppId);

	DELETE_PTR(m_pTcpClient);
	m_pTcpClient = XTcpClient::Create(*this);
	m_pTcpClient->Connect(strSvrAddr, nSvrPort, true);
}

void RTClient::Disconnect()
{
	if (m_bSyncConnected) {
		SynClient::Disconnect();
	}	
	else if (m_pTcpClient->Status() != NOT_CONNECTED) {
		m_pTcpClient->Disconnect();
	}

	if (m_nTime2disconnect == 0)
		m_nTime2disconnect = rtc::Time() + 500;
}

void RTClient::OnConnected(int code, const std::string&strUserData)
{
	if (code == 200)
	{
		m_bSyncConnected = true;
	}

	std::string strSysConf = "{}";
	if (strUserData.length() > 0) {
		rapidjson::Document		jsonReqDoc;
		if (!jsonReqDoc.ParseInsitu<0>((char*)strUserData.c_str()).HasParseError())
		{
			if (jsonReqDoc.HasMember("DyncId"))
				m_strDyncId = jsonReqDoc["DyncId"].GetString();
			if (jsonReqDoc.HasMember("ServerId"))
				m_strServerId = jsonReqDoc["ServerId"].GetString();
			if (jsonReqDoc.HasMember("SysConfigue"))
				strSysConf = jsonReqDoc["SysConfigue"].GetString();
		}
	}
	
	this->OnRtcConnect(code, m_strDyncId, m_strServerId, strSysConf);
}

void RTClient::OnMessageSend(const char*pMsg, int nLen)
{
	if (m_pTcpClient) {
		char buffer[3];
		char* pptr = buffer;
		pptr[0] = '$';
		pptr++;
		writeShort(&pptr, nLen + 3);
		m_pTcpClient->SendMessageX(buffer, 3);
		m_pTcpClient->SendMessageX(pMsg, nLen);
	}
}
void RTClient::OnMessageRecved(int cmd, MapParams& params, const std::string& strContent)
{
	switch (cmd) {
	case DC_MESSAGE: {
		std::string&strFrom = params["From"];
		OnRtcMessage(strFrom, strContent);
	}
		break;
	case DC_PUBLISH:
	{
		std::string&strResult = params["Result"];
		OnRtcPublish(strResult, params["ChanId"], params["DyncerId"], params["RtmpUrl"], params["HlsUrl"], params["Reason"]);
	}
		break;
	case DC_UNPUBLISH:
	{
		std::string&strResult = params["Result"];
		OnRtcUnpublish(strResult, params["ChanId"]);
	}
		break;
	case DC_SUBSCRIBE:
	{
		std::string&strResult = params["Result"];
		OnRtcSubscribe(strResult, params["ChanId"], params["PubId"], strContent);
	}
		break;
	case DC_UNSUBSCRIBE:
	{
		std::string&strResult = params["Result"];
		OnRtcUnsubscribe(strResult, params["ChanId"]);
	}
		break;
	case DC_SDP_INFO:
	{
		OnRtcSdpInfo(params["ChanId"], strContent);
	}
		break;
	case DC_MAKE_CALL:
	case DC_JOIN_MEET:
	case DC_JOIN_LIVE:
	case DC_JOIN_LINE:
	{
		OnRtcUserOptionJoin(params["AnyrtcID"], params["Result"], strContent);
	}
		break;
	case DC_NOTIFY_CALL:
	case DC_NOTIFY_MEET:
	case DC_NOTIFY_LIVE:
	case DC_NOTIFY_LINE:
	{
		OnRtcUserOptionNotify(params["AnyrtcID"], strContent);
	}
		break;
	case DC_END_CALL:
	case DC_LEAVE_MEET:
	case DC_LEAVE_LIVE:
	case DC_LEAVE_LINE:
	{
		OnRtcUserOptionLeave(params["AnyrtcID"]);
	}
		break;
	};
	
}
void RTClient::OnDisconnect()
{
	m_bSyncConnected = false;
	SynClient::Clear();
	this->OnRtcDisconnect();
}

void RTClient::Message(const std::string&strTo, const std::string&strToSvrId, const std::string&strContent)
{
	MapParams param;
	param["From"] = m_strDyncId;
	param["To"] = strTo;
	param["ToSvr"] = strToSvrId;

	SynClient::SendMessageX(DC_MESSAGE, true, param, strContent);
}

void RTClient::UserOptionJoin(UserOption opt, const std::string&strAnyrtcId, const std::string&strUserData)
{
	std::string strType;
	int cmd = GetUserJoinOption(opt, strType);
	MapParams param;
	rapidjson::Document		jsonDoc;
	rapidjson::StringBuffer jsonStr;
	rapidjson::Writer<rapidjson::StringBuffer> jsonWriter(jsonStr);
	jsonDoc.SetObject();
	jsonDoc.AddMember("Type", strType.c_str(), jsonDoc.GetAllocator());
	jsonDoc.AddMember("AnyrtcID", strAnyrtcId.c_str(), jsonDoc.GetAllocator());
	jsonDoc.AddMember("UserData", strUserData.c_str(), jsonDoc.GetAllocator());

	jsonDoc.Accept(jsonWriter);
	SynClient::SendMessageX(cmd, true, param, jsonStr.GetString());
}
void RTClient::UserOptionNotify(UserOption opt, const std::string&strAnyrtcId, const std::string&strMessage)
{
	std::string strType;
	int cmd = GetUserNotifyOption(opt, strType);
	MapParams param;
	rapidjson::Document		jsonDoc;
	rapidjson::StringBuffer jsonStr;
	rapidjson::Writer<rapidjson::StringBuffer> jsonWriter(jsonStr);
	jsonDoc.SetObject();
	jsonDoc.AddMember("Type", strType.c_str(), jsonDoc.GetAllocator());
	jsonDoc.AddMember("AnyrtcID", strAnyrtcId.c_str(), jsonDoc.GetAllocator());
	jsonDoc.AddMember("Message", strMessage.c_str(), jsonDoc.GetAllocator());

	jsonDoc.Accept(jsonWriter);
	SynClient::SendMessageX(cmd, true, param, jsonStr.GetString());
}
void RTClient::UserOptionLeave(UserOption opt, const std::string&strAnyrtcId)
{
	std::string strType;
	int cmd = GetUserLeaveOption(opt, strType);
	MapParams param;
	rapidjson::Document		jsonDoc;
	rapidjson::StringBuffer jsonStr;
	rapidjson::Writer<rapidjson::StringBuffer> jsonWriter(jsonStr);
	jsonDoc.SetObject();
	jsonDoc.AddMember("Type", strType.c_str(), jsonDoc.GetAllocator());
	jsonDoc.AddMember("AnyrtcID", strAnyrtcId.c_str(), jsonDoc.GetAllocator());

	jsonDoc.Accept(jsonWriter);
	SynClient::SendMessageX(cmd, true, param, jsonStr.GetString());
}

void RTClient::Publish(const std::string&strAnyrtId, int width, int height, int aBitrate, int vBitrate)
{
	MapParams param;
	rapidjson::Document		jsonDoc;
	rapidjson::StringBuffer jsonStr;
	rapidjson::Writer<rapidjson::StringBuffer> jsonWriter(jsonStr);
	jsonDoc.SetObject();
	jsonDoc.AddMember("Type", "M2M", jsonDoc.GetAllocator());
	jsonDoc.AddMember("AnyrtcID", strAnyrtId.c_str(), jsonDoc.GetAllocator());
	jsonDoc.AddMember("Width", width, jsonDoc.GetAllocator());
	jsonDoc.AddMember("Height", height, jsonDoc.GetAllocator());
	jsonDoc.AddMember("AudioBitrate", aBitrate, jsonDoc.GetAllocator());
	jsonDoc.AddMember("VideoBitrate", vBitrate, jsonDoc.GetAllocator());

	jsonDoc.Accept(jsonWriter);
	SynClient::SendMessageX(DC_PUBLISH, true, param, jsonStr.GetString());
}

void RTClient::Unpublish(const std::string&strPublishId)
{
	MapParams param;
	rapidjson::Document		jsonDoc;
	rapidjson::StringBuffer jsonStr;
	rapidjson::Writer<rapidjson::StringBuffer> jsonWriter(jsonStr);
	jsonDoc.SetObject();
	jsonDoc.AddMember("PublishId", strPublishId.c_str(), jsonDoc.GetAllocator());

	jsonDoc.Accept(jsonWriter);
	SynClient::SendMessageX(DC_UNPUBLISH, true, param, jsonStr.GetString());
}

void RTClient::Subscribe(const std::string&strPubDyncorId)
{
	MapParams param;
	rapidjson::Document		jsonDoc;
	rapidjson::StringBuffer jsonStr;
	rapidjson::Writer<rapidjson::StringBuffer> jsonWriter(jsonStr);
	jsonDoc.SetObject();
	jsonDoc.AddMember("SessionID", strPubDyncorId.c_str(), jsonDoc.GetAllocator());

	jsonDoc.Accept(jsonWriter);
	SynClient::SendMessageX(DC_SUBSCRIBE, true, param, jsonStr.GetString());
}

void RTClient::Unsubscribe(const std::string&strSubscribeId)
{
	MapParams param;
	rapidjson::Document		jsonDoc;
	rapidjson::StringBuffer jsonStr;
	rapidjson::Writer<rapidjson::StringBuffer> jsonWriter(jsonStr);
	jsonDoc.SetObject();
	jsonDoc.AddMember("ChanId", strSubscribeId.c_str(), jsonDoc.GetAllocator());

	jsonDoc.Accept(jsonWriter);
	SynClient::SendMessageX(DC_UNSUBSCRIBE, true, param, jsonStr.GetString());
}

void RTClient::SendSdpInfo(const std::string&strChannelId, const std::string&strJsep)
{
	MapParams param;
	param["ChanId"] = strChannelId;

	SynClient::SendMessageX(DC_SDP_INFO, true, param, strJsep);
}

void RTClient::SwitchVideoBits(const std::string&strSubscribeId, int bitLevel)
{
	MapParams param;
	rapidjson::Document		jsonDoc;
	rapidjson::StringBuffer jsonStr;
	rapidjson::Writer<rapidjson::StringBuffer> jsonWriter(jsonStr);
	jsonDoc.SetObject();
	jsonDoc.AddMember("ChanId", strSubscribeId.c_str(), jsonDoc.GetAllocator());
	jsonDoc.AddMember("BitLevel", bitLevel, jsonDoc.GetAllocator());

	jsonDoc.Accept(jsonWriter);
	SynClient::SendMessageX(DC_SWITCH_V_BITS, true, param, jsonStr.GetString());
}

void RTClient::OnServerConnected()
{
	rapidjson::Document		jsonDoc;
	rapidjson::StringBuffer jsonStr;
	rapidjson::Writer<rapidjson::StringBuffer> jsonWriter(jsonStr);
	jsonDoc.SetObject();
	jsonDoc.AddMember("DeveloperId", m_strDeveloperId.c_str(), jsonDoc.GetAllocator());
	jsonDoc.AddMember("AppId", m_strAppId.c_str(), jsonDoc.GetAllocator());
	jsonDoc.AddMember("DevType", "windows", jsonDoc.GetAllocator());

	jsonDoc.Accept(jsonWriter);

	SynClient::Connect(m_strToken, jsonStr.GetString());
}

void RTClient::OnServerDisconnect()
{
	SynClient::Clear();
}

void RTClient::OnServerConnectionFailure()
{
	this->OnRtcConnectFailed();
}

void RTClient::OnTick()
{
	SynClient::DoTick();

	if (m_nTime2disconnect > 0 && m_nTime2disconnect >= rtc::Time()) {
		if (m_pTcpClient)
		{
			m_pTcpClient->Disconnect();
		}
		m_nTime2disconnect = 0;
	}
}

void RTClient::OnMessageSent(int err)
{

}

void RTClient::OnMessageRecv(const char*data, int size)
{
	{//* 1,将接收到的数据放入缓存中
		while ((m_nBufOffset + size) > m_nBufLen)
		{
			int newLen = m_nBufLen + kRequestBufferSizeInBytes;
			if (size > kRequestBufferSizeInBytes)
				newLen = m_nBufLen + size;
			char* temp = new char[newLen];
			if (temp == NULL)
				continue;
			memcpy(temp, m_pBuffer, m_nBufLen);
			delete[] m_pBuffer;
			m_pBuffer = temp;
			m_nBufLen = newLen;
		}

		memcpy(m_pBuffer + m_nBufOffset, data, size);
		m_nBufOffset += size;
	}

	while (m_nBufOffset > 3)
	{//* 2,解压包
		int parsed = 0;
		if (m_pBuffer[0] != '$')
		{// Hase error!
			parsed = m_nBufOffset;
		}
		else
		{
			char*pptr = m_pBuffer + 1;
			int packLen = readShort(&pptr);
			if (packLen <= m_nBufOffset)
			{
				ParseMessage(pptr, packLen - 3);
				parsed = packLen;
			}
			else
			{
				break;
			}
		}

		if (parsed > 0)
		{
			m_nBufOffset -= parsed;
			if (m_nBufOffset == 0)
			{
				memset(m_pBuffer, 0, m_nBufLen);
			}
			else
			{
				memmove(m_pBuffer, m_pBuffer + parsed, m_nBufOffset);
			}
		}
	}
}

void RTClient::ParseMessage(const char*message, int nLen)
{
	if (nLen >= m_nParseBufLen)
	{
		m_nParseBufLen = nLen + 1;
		delete[] m_pParseBuf;
		m_pParseBuf = new char[m_nParseBufLen];
	}
	memcpy(m_pParseBuf, message, nLen);
	m_pParseBuf[nLen] = '\0';
	SynClient::RecvMessageX(m_pParseBuf, nLen);
}