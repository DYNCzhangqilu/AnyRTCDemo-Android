#include "SynClient.h"
#include "webrtc/base/timeutils.h"
#include "rapidjson/document.h"
#include "rapidjson/prettywriter.h"	
#include "rapidjson/stringbuffer.h"

#define SYNC_TIME		60000
static void GenRandStr(std::string & sRandStr, uint32_t len)
{
	uint32_t idx = 0;
	srand((unsigned)time(NULL));
	char tempChar = 0;
	sRandStr.clear();

	while (idx < len)
	{
		tempChar = rand() % 128;
		if (isprint(tempChar))
		{
			sRandStr.append(1, tempChar);
			++idx;
		}
	}
}

SynClient::SynClient(int maxEv)
	: m_bEncrypt(false)
	, m_bConnected(false)
	, m_nMaxEv(maxEv)
	, m_nLocalSeqn(0)
	, m_nRemoteSeqn(0)
	, m_pEncrypt(NULL)
	, m_nTimestamp(0)
{
	if (m_nMaxEv <= 0)
		m_nMaxEv = 1;
}


SynClient::~SynClient()
{
	Clear();
	if (m_pEncrypt) {
		delete m_pEncrypt;
		m_pEncrypt = NULL;
	}
}

void SynClient::DoTick()
{
	if (m_bConnected) {
		if (m_lstMsgWait4Ack.size() == 0) {
			// No message is waitting for ack, so could send the message directly.
			DoPackMessage();
		}
		else {
			// Wait for ack.
		}

		if (m_nTimestamp < rtc::Time()) {
			DoSync();
		}
	}
}

void SynClient::SetEncrypt(const std::string &sToken, const std::string &sEncodingAESKey, const std::string &sAppid)
{
	m_bEncrypt = true;
	if (m_pEncrypt) {
		delete m_pEncrypt;
		m_pEncrypt = NULL;
	}
	m_pEncrypt = new anyrtc::SyncMsgCrypt(sToken, sEncodingAESKey, sAppid);
}

void SynClient::Clear()
{
	m_bConnected = false;
	m_nLocalSeqn = 0;
	m_nRemoteSeqn = 0;
	m_lstMsgWait2Send.clear();
	m_lstMsgWait4Ack.clear();
}

void SynClient::Connect(const std::string&strAuthTag, const std::string&strUserData)
{
	rapidjson::Document		jsonDoc;
	rapidjson::StringBuffer jsonStr;
	rapidjson::Writer<rapidjson::StringBuffer> jsonWriter(jsonStr);
	jsonDoc.SetObject();
	jsonDoc.AddMember("DYNC", "Connect", jsonDoc.GetAllocator());
	jsonDoc.AddMember("MaxEv", m_nMaxEv, jsonDoc.GetAllocator());

	std::string strTimestamp;
	std::string strNonce;
	std::string strResult;
	std::string strSignature;
	std::string strEncryptMsg;
	if (m_pEncrypt) {
		char buff[65] = { 0 };
		sprintf(buff, "%u", rtc::Time());
		strTimestamp = buff;
		GenRandStr(strNonce, 10);

		jsonDoc.AddMember("Encrypt", true, jsonDoc.GetAllocator());
		jsonDoc.AddMember("TimeStamp", strTimestamp.c_str(), jsonDoc.GetAllocator());
		jsonDoc.AddMember("Nonce", strNonce.c_str(), jsonDoc.GetAllocator());
		
		m_pEncrypt->EncryptMsg(strAuthTag, strTimestamp, strNonce, strSignature, strEncryptMsg);
		jsonDoc.AddMember("Signature", strSignature.c_str(), jsonDoc.GetAllocator());
		jsonDoc.AddMember("AuthTag", strEncryptMsg.c_str(), jsonDoc.GetAllocator());
	}
	else 
	{
		jsonDoc.AddMember("Encrypt", false, jsonDoc.GetAllocator());
		jsonDoc.AddMember("AuthTag", strAuthTag.c_str(), jsonDoc.GetAllocator());
	}
	jsonDoc.AddMember("UserData", strUserData.c_str(), jsonDoc.GetAllocator());

	jsonDoc.Accept(jsonWriter);
	OnMessageSend(jsonStr.GetString(), jsonStr.Size());
}

void SynClient::SendMessageX(int cmd, bool encrypt, const MapParams& params, const std::string& strContent)
{
	{//* Just save message in List.
		rtc::scoped_refptr<SyncMessage> syncMsg(new rtc::RefCountedObject<SyncMessage>());
		syncMsg->cmd = cmd;
		syncMsg->seqn = ++m_nLocalSeqn;
		syncMsg->encrypt = encrypt;
		syncMsg->params = params;
		syncMsg->strContent = strContent;

		m_lstMsgWait2Send.push_back(syncMsg);
	}
}

void SynClient::RecvMessageX(const char*pData, int nLen)
{
	rapidjson::Document		jsonReqDoc;
	if (!jsonReqDoc.ParseInsitu<0>((char*)pData).HasParseError())
	{
		if (jsonReqDoc.HasMember("DYNC") && jsonReqDoc["DYNC"].IsString())
		{
			const char* strDync = jsonReqDoc["DYNC"].GetString();
			if (strcmp("Connect", strDync) == 0)
			{
				int code = jsonReqDoc["Code"].GetInt();	
				std::string strUserData = jsonReqDoc["UserData"].GetString();;
				if (code == 200) {
					m_bConnected = true;
					m_nTimestamp = rtc::Time() + SYNC_TIME;
				}
				OnConnected(code, strUserData);
			}
			else if (strcmp("Message", strDync) == 0)
			{
				if (jsonReqDoc.HasMember("Encrypt") && jsonReqDoc["Encrypt"].IsBool()) {
					bool encrypt = jsonReqDoc["Encrypt"].GetBool();
					std::string strTimestamp;
					std::string strNonce;
					if (encrypt) {
						if (!(jsonReqDoc.HasMember("TimeStamp") && jsonReqDoc["TimeStamp"].IsString())
							|| !(jsonReqDoc.HasMember("Nonce") && jsonReqDoc["Nonce"].IsString()))
						{
							return;
						}
						strTimestamp = jsonReqDoc["TimeStamp"].GetString();
						strNonce = jsonReqDoc["Nonce"].GetString();

						if (jsonReqDoc.HasMember("JArr") && jsonReqDoc["JArr"].IsArray()) {
							rapidjson::Value& jarr = jsonReqDoc["JArr"];
							int seqn = 0;
							for (rapidjson::SizeType i = 0; i < jarr.Size(); i++)
							{
								const rapidjson::Value& object = jarr[i];
								rapidjson::Document		jsDoc;
								if (!jsDoc.ParseInsitu<0>((char*)object.GetString()).HasParseError())
								{
									if (jsDoc.HasMember("Cmd") && jsDoc.HasMember("Signature") && jsDoc.HasMember("Body")) {
										int cmd = jsDoc["Cmd"].GetInt();
										seqn = jsDoc["Seqn"].GetInt();
										std::string strSignature = jsDoc["Signature"].GetString();
										std::string strBody = jsDoc["Body"].GetString();
										MapParams params;
										rapidjson::Document::MemberIterator ite = jsDoc.MemberBegin();
										for (; ite != jsDoc.MemberEnd(); ++ite)
										{
											const char* name = ite->name.GetString();
											if (!(strcmp(name, "Cmd") == 0 || strcmp(name, "Signature") == 0
												|| strcmp(name, "Body") == 0 || strcmp(name, "Seqn") == 0)) {
												if (!ite->value.IsString())
													continue;
												params[name] = ite->value.GetString();
											}
										}
										std::string strMsg;
										if (m_pEncrypt) {
											if (strBody.length() == 0) {
												this->OnMessageRecved(cmd, params, strMsg);
											}
											else if (m_pEncrypt->DecryptMsg(strSignature, strTimestamp, strNonce, strBody, strMsg) == 0) {
												this->OnMessageRecved(cmd, params, strMsg);
											}
										}
									}
								}
							}
							if (seqn > m_nRemoteSeqn) {
								m_nRemoteSeqn = seqn;
								DoSync();
							}
						}
					}
					else {
						if (jsonReqDoc.HasMember("JArr") && jsonReqDoc["JArr"].IsArray()) {
							rapidjson::Value& jarr = jsonReqDoc["JArr"];
							int seqn = 0;
							for (rapidjson::SizeType i = 0; i < jarr.Size(); i++)
							{
								const rapidjson::Value& object = jarr[i];
								rapidjson::Document		jsDoc;
								if (!jsDoc.ParseInsitu<0>((char*)object.GetString()).HasParseError())
								{
									if (jsDoc.HasMember("Cmd") && jsDoc.HasMember("Body")) {
										int cmd = jsDoc["Cmd"].GetInt();
										seqn = jsDoc["Seqn"].GetInt();
										std::string strBody = jsDoc["Body"].GetString();
										MapParams params;
										rapidjson::Document::MemberIterator ite = jsDoc.MemberBegin();
										for (; ite != jsDoc.MemberEnd(); ++ite)
										{
											const char* name = ite->name.GetString();
											if (strcmp(name, "Cmd") != 0 && strcmp(name, "Body") != 0 && strcmp(name, "Seqn") != 0) {
												if (!ite->value.IsString())
													continue;
												params[name] = ite->value.GetString();
											}
										}
										this->OnMessageRecved(cmd, params, strBody);
									}
								}
							}
							if (seqn > m_nRemoteSeqn) {
								m_nRemoteSeqn = seqn;
								DoSync();
							}
						}
					}
				}
			}
			else if (strcmp("Ack", strDync) == 0)
			{
				if (jsonReqDoc.HasMember("Seqn") && jsonReqDoc["Seqn"].IsInt()) {
					int seqn = jsonReqDoc["Seqn"].GetInt();
					DoAck(seqn);
				}
			}
		}
	}
}

void SynClient::Disconnect()
{
	if (m_bConnected) {
		m_bConnected = false;

		rapidjson::Document		jsonDoc;
		rapidjson::StringBuffer jsonStr;
		rapidjson::Writer<rapidjson::StringBuffer> jsonWriter(jsonStr);
		jsonDoc.SetObject();
		jsonDoc.AddMember("DYNC", "Disconnect", jsonDoc.GetAllocator());
		jsonDoc.Accept(jsonWriter);

		OnMessageSend(jsonStr.GetString(), jsonStr.Size());
	}
}

void SynClient::DoAck(int seqn)
{
	ListMessage::iterator iter = m_lstMsgWait4Ack.begin();
	while (iter != m_lstMsgWait4Ack.end()) {
		if (iter->get()->seqn <= seqn) {
			m_lstMsgWait4Ack.erase(iter);
			iter = m_lstMsgWait4Ack.begin();
		}
		else {
			iter++;
		}
	}

	if (m_lstMsgWait4Ack.size() > 0) {
		// 
		RePackMessage();
	}
}

void SynClient::DoSync()
{
	m_nTimestamp = rtc::Time() + SYNC_TIME;

	rapidjson::Document		jsonDoc;
	rapidjson::StringBuffer jsonStr;
	rapidjson::Writer<rapidjson::StringBuffer> jsonWriter(jsonStr);
	jsonDoc.SetObject();
	jsonDoc.AddMember("DYNC", "Sync", jsonDoc.GetAllocator());
	jsonDoc.AddMember("LocalSeqn", m_nLocalSeqn, jsonDoc.GetAllocator());
	jsonDoc.AddMember("RemoteSeqn", m_nRemoteSeqn, jsonDoc.GetAllocator());
	jsonDoc.Accept(jsonWriter);

	OnMessageSend(jsonStr.GetString(), jsonStr.Size());
}

void SynClient::DoPackMessage()
{
	if (m_lstMsgWait2Send.size() > 0) {
		char buff[65] = { 0 };
		sprintf(buff, "%u", rtc::Time());
		std::string strTimestamp = buff;
		std::string strNonce;
		std::string strResult;
		GenRandStr(strNonce, 10);

		int ct = 0;
		rapidjson::Document		jsonDoc;
		rapidjson::StringBuffer jsonStr;
		rapidjson::Writer<rapidjson::StringBuffer> jsonWriter(jsonStr);
		jsonDoc.SetObject();
		rapidjson::Value jarr(rapidjson::kArrayType);
		jsonDoc.AddMember("DYNC", "Message", jsonDoc.GetAllocator());
		if (m_bEncrypt) {
			jsonDoc.AddMember("Encrypt", true, jsonDoc.GetAllocator());
			jsonDoc.AddMember("TimeStamp", strTimestamp.c_str(), jsonDoc.GetAllocator());
			jsonDoc.AddMember("Nonce", strNonce.c_str(), jsonDoc.GetAllocator());
		}
		else {
			jsonDoc.AddMember("Encrypt", false, jsonDoc.GetAllocator());
		}
		while (ct < m_nMaxEv) {
			rtc::scoped_refptr<SyncMessage> syncMsg = m_lstMsgWait2Send.front();
			rapidjson::Document		jsDoc;
			rapidjson::StringBuffer jsStr;
			rapidjson::Writer<rapidjson::StringBuffer> jsWriter(jsStr);
			jsDoc.SetObject();
			jsDoc.AddMember("Cmd", syncMsg->cmd, jsDoc.GetAllocator());
			jsDoc.AddMember("Seqn", syncMsg->seqn, jsDoc.GetAllocator());
			MapParams::iterator iter = syncMsg->params.begin();
			while (iter != syncMsg->params.end()) {
				jsDoc.AddMember(iter->first.c_str(), iter->second.c_str(), jsDoc.GetAllocator());
				iter++;
			}
			std::string strSignature;
			std::string strEncryptMsg;
			if (m_bEncrypt)
			{// Do Encrypt
				m_pEncrypt->EncryptMsg(syncMsg->strContent, strTimestamp, strNonce, strSignature, strEncryptMsg);
				jsDoc.AddMember("Signature", strSignature.c_str(), jsDoc.GetAllocator());
				std::string decStr;
				{
					m_pEncrypt->DecryptMsg(strSignature, strTimestamp, strNonce, strEncryptMsg, decStr);
				}
				jsDoc.AddMember("Body", strEncryptMsg.c_str(), jsDoc.GetAllocator());
			}
			else {
				jsDoc.AddMember("Body", syncMsg->strContent.c_str(), jsDoc.GetAllocator());
			}
			jsDoc.Accept(jsWriter);
			syncMsg->strPacked = jsStr.GetString();
			jarr.PushBack(syncMsg->strPacked.c_str(), jsonDoc.GetAllocator());

			// Swap message list.
			m_lstMsgWait4Ack.push_back(syncMsg);
			m_lstMsgWait2Send.pop_front();
			if (m_lstMsgWait2Send.size() == 0) {
				break;
			}
			ct++;
		}

		jsonDoc.AddMember("JArr", jarr, jsonDoc.GetAllocator());
		jsonDoc.Accept(jsonWriter);

		OnMessageSend(jsonStr.GetString(), jsonStr.Size());
	}
}

void SynClient::RePackMessage()
{
	if (m_lstMsgWait4Ack.size() > 0) {
		char buff[65] = { 0 };
		sprintf(buff, "%u", rtc::Time());
		std::string strTimestamp = buff;
		std::string strNonce;
		std::string strResult;
		GenRandStr(strNonce, 10);

		int ct = 0;
		rapidjson::Document		jsonDoc;
		rapidjson::StringBuffer jsonStr;
		rapidjson::Writer<rapidjson::StringBuffer> jsonWriter(jsonStr);
		jsonDoc.SetObject();
		rapidjson::Value jarr(rapidjson::kArrayType);
		jsonDoc.AddMember("DYNC", "Sync", jsonDoc.GetAllocator());
		if (m_bEncrypt) {
			jsonDoc.AddMember("Encrypt", true, jsonDoc.GetAllocator());
			jsonDoc.AddMember("TimeStamp", strTimestamp.c_str(), jsonDoc.GetAllocator());
			jsonDoc.AddMember("Nonce", strNonce.c_str(), jsonDoc.GetAllocator());
		}
		else {
			jsonDoc.AddMember("Encrypt", false, jsonDoc.GetAllocator());
		}

		ListMessage::iterator itor = m_lstMsgWait4Ack.begin();
		while (ct < m_nMaxEv) {

			rtc::scoped_refptr<SyncMessage> syncMsg = itor->get();

			rapidjson::Document		jsDoc;
			rapidjson::StringBuffer jsStr;
			rapidjson::Writer<rapidjson::StringBuffer> jsWriter(jsStr);
			jsDoc.SetObject();
			jsDoc.AddMember("Cmd", syncMsg->cmd, jsDoc.GetAllocator());
			jsDoc.AddMember("Seqn", syncMsg->seqn, jsDoc.GetAllocator());
			MapParams::iterator iter = syncMsg->params.begin();
			while (iter != syncMsg->params.end()) {
				jsDoc.AddMember(iter->first.c_str(), iter->second.c_str(), jsDoc.GetAllocator());
				iter++;
			}
			std::string strSignature;
			std::string strEncryptMsg;
			if (m_bEncrypt)
			{// Do Encrypt
				m_pEncrypt->EncryptMsg(syncMsg->strContent, strTimestamp, strNonce, strSignature, strEncryptMsg);
				jsDoc.AddMember("Signature", strSignature.c_str(), jsDoc.GetAllocator());
				std::string decStr;
				{
					m_pEncrypt->DecryptMsg(strSignature, strTimestamp, strNonce, strEncryptMsg, decStr);
				}
				jsDoc.AddMember("Body", strEncryptMsg.c_str(), jsDoc.GetAllocator());
			}
			else {
				jsDoc.AddMember("Body", syncMsg->strContent.c_str(), jsDoc.GetAllocator());
			}
			jsDoc.Accept(jsWriter);
			syncMsg->strPacked = jsStr.GetString();
			jarr.PushBack(syncMsg->strPacked.c_str(), jsonDoc.GetAllocator());

			ct++;
			itor++;
			if (itor == m_lstMsgWait4Ack.end()) {
				break;
			}
		}

		jsonDoc.AddMember("JArr", jarr, jsonDoc.GetAllocator());
		jsonDoc.Accept(jsonWriter);

		OnMessageSend(jsonStr.GetString(), jsonStr.Size());
	}
}