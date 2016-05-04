
#ifndef __SYNC_MSG_CRYPT_H__
#define __SYNC_MSG_CRYPT_H__

#include <string>
#include <stdint.h>

namespace anyrtc {

static const unsigned int kAesKeySize = 32;
static const unsigned int kAesIVSize = 16;
static const unsigned int kEncodingKeySize = 43;
static const unsigned int kRandEncryptStrLen = 16;
static const unsigned int kMsgLen = 4;
static const unsigned int kMaxBase64Size = 1000000000;
enum  SyncMsgCryptErrorCode
{
    SyncMsgCrypt_OK = 0,
    SyncMsgCrypt_ValidateSignature_Error = -40001,
	SyncMsgCrypt_NULL_Error = -40002,
    SyncMsgCrypt_ComputeSignature_Error = -40003,
    SyncMsgCrypt_IllegalAesKey = -40004,
    SyncMsgCrypt_ValidateCorpid_Error = -40005,
    SyncMsgCrypt_EncryptAES_Error = -40006,
    SyncMsgCrypt_DecryptAES_Error = -40007,
    SyncMsgCrypt_IllegalBuffer = -40008,
    SyncMsgCrypt_EncodeBase64_Error = -40009,
    SyncMsgCrypt_DecodeBase64_Error = -40010,
};

class SyncMsgCrypt
{
public:
    //���캯��
    // @param sToken: ����ƽ̨�ϣ����������õ�Token
    // @param sEncodingAESKey: ����ƽ̨�ϣ����������õ�EncodingAESKey
    // @param sAppid: �������˺ŵ�appid
	SyncMsgCrypt(const std::string &sToken,
		const std::string &sEncodingAESKey,
		const std::string &sAppid)
		:m_sToken(sToken), m_sEncodingAESKey(sEncodingAESKey), m_sAppid(sAppid)
	{   }
    
    // ������Ϣ����ʵ�ԣ����һ�ȡ���ܺ������
    // @param sMsgSignature: ǩ��������ӦURL������msg_signature
    // @param sTimeStamp: ʱ�������ӦURL������timestamp
    // @param sNonce: ���������ӦURL������nonce
    // @param sDecryptData: ���ģ���Ӧ���ܵ�����
    // @param sMsg: ���ܺ��ԭ�ģ���return����0ʱ��Ч
    // @return: �ɹ�0��ʧ�ܷ��ض�Ӧ�Ĵ�����
    int DecryptMsg(const std::string &sMsgSignature,
                    const std::string &sTimeStamp,
                    const std::string &sNonce,
					const std::string &sDecryptData,
                    std::string &sMsg);
            
	//���������˺ŷ��͵���Ϣ���ܴ��
	// @param sReplyMsg:�������˺ŷ��͵���Ϣ��xml��ʽ���ַ���
	// @param sTimeStamp: ʱ����������Լ����ɣ�Ҳ������URL������timestamp
	// @param sNonce: ������������Լ����ɣ�Ҳ������URL������nonce
	// @param sEncryptMsg: ���ܺ�Ŀ���ֱ�ӻظ��û������ģ�����msg_signature, timestamp, nonce, encrypt��xml��ʽ���ַ���,
	//                      ��return����0ʱ��Ч
	// return���ɹ�0��ʧ�ܷ��ض�Ӧ�Ĵ�����
	int EncryptMsg(const std::string &sReplyMsg,
		const std::string &sTimeStamp,
		const std::string &sNonce,
		std::string &sSignature,
		std::string &sEncryptMsg);

private:
    std::string m_sToken;
    std::string m_sEncodingAESKey;
    std::string m_sAppid;

private:
    // AES CBC
    int AES_CBCEncrypt( const char * sSource, const uint32_t iSize,
            const char * sKey, unsigned int iKeySize, std::string * poResult );
    
    int AES_CBCEncrypt( const std::string & objSource,
            const std::string & objKey, std::string * poResult );
    
    int AES_CBCDecrypt( const char * sSource, const uint32_t iSize,
            const char * sKey, uint32_t iKeySize, std::string * poResult );
    
    int AES_CBCDecrypt( const std::string & objSource,
            const std::string & objKey, std::string * poResult );
    
    //base64
    int EncodeBase64(const std::string sSrc, std::string & sTarget);
    
    int DecodeBase64(const std::string sSrc, std::string & sTarget);
    
    //genkey
    int GenAesKeyFromEncodingKey( const std::string & sEncodingKey, std::string & sAesKey);
    
    //signature
    int ComputeSignature(const std::string sToken, const std::string sTimeStamp, const std::string & sNonce,
        const std::string & sMessage, std::string & sSignature);
    
    int ValidateSignature(const std::string &sMsgSignature, const std::string &sTimeStamp, 
        const std::string &sNonce, const std::string & sEncryptMsg);  

    //get , set data
    void GenRandStr(std::string & sRandStr, uint32_t len);

    void GenNeedEncryptData(const std::string &sReplyMsg,std::string & sNeedEncrypt );

};

}

#endif	// __SYNC_MSG_CRYPT_H__
