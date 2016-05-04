
#include "SyncMsgCrypt.h"

#include <string.h>
#include <stdlib.h>
#ifndef _WIN32
#include <arpa/inet.h>
#else
#include <Windows.h>
#endif
#include <string>
#include <vector>
#include <algorithm>
#include <iostream>

#include "openssl/aes.h"
#include "openssl/sha.h"
#include "openssl/evp.h"


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
    
namespace anyrtc{

int SyncMsgCrypt::DecryptMsg(const std::string &sMsgSignature,
                const std::string &sTimeStamp,
                const std::string &sNonce,
				const std::string &sDecryptData,
                std::string &sMsg)
{
    //1.validate xml format
	std::string sEncryptMsg = sDecryptData;
	if (sEncryptMsg.length() == 0)
    {
		return SyncMsgCrypt_NULL_Error;
    }

    //2.validate signature
    if(0 != ValidateSignature(sMsgSignature,sTimeStamp,sNonce,sEncryptMsg) ) 
    {
        return SyncMsgCrypt_ValidateSignature_Error;
    }

    //3.decode base64
    std::string sAesData;
    if(0 != DecodeBase64(sEncryptMsg,sAesData))
    {
        return SyncMsgCrypt_DecodeBase64_Error;
    }
    
    //4.decode aes
    std::string sAesKey;
    std::string sNoEncryptData;
    if(0 != GenAesKeyFromEncodingKey(m_sEncodingAESKey,sAesKey)) 
    {
        return SyncMsgCrypt_IllegalAesKey;
    }
    if(0 != AES_CBCDecrypt(sAesData, sAesKey, &sNoEncryptData))
    {
        return SyncMsgCrypt_DecryptAES_Error;
    }

    // 5. remove kRandEncryptStrLen str 
    if(sNoEncryptData.size() <= (kRandEncryptStrLen + kMsgLen))
    {
        return SyncMsgCrypt_IllegalBuffer;
    }  
    uint32_t iNetLen = *((const uint32_t *)(sNoEncryptData.c_str() + kRandEncryptStrLen));
    uint32_t iMsgLen = ntohl(iNetLen);
    if(sNoEncryptData.size() <= (kRandEncryptStrLen + kMsgLen + iMsgLen))
    {
        return SyncMsgCrypt_IllegalBuffer;
    }
    sMsg = sNoEncryptData.substr(kRandEncryptStrLen+kMsgLen,iMsgLen );

    //6. validate corpid
    std::string sAppid = sNoEncryptData.substr(kRandEncryptStrLen+kMsgLen+iMsgLen);
    if(sAppid != m_sAppid)
    {
        return SyncMsgCrypt_ValidateCorpid_Error;
    }
   
    return SyncMsgCrypt_OK;
}

int SyncMsgCrypt::EncryptMsg(const std::string &sReplyMsg,
	const std::string &sTimeStamp,
	const std::string &sNonce,
	std::string &sSignature,
	std::string &sEncryptMsg)
{
	if (0 == sReplyMsg.size())
	{
		return SyncMsgCrypt_NULL_Error;
	}

	//1.add rand str ,len, corpid
	std::string sNeedEncrypt;
	GenNeedEncryptData(sReplyMsg, sNeedEncrypt);

	//2. AES Encrypt
	std::string sAesData;
	std::string sAesKey;
	if (0 != GenAesKeyFromEncodingKey(m_sEncodingAESKey, sAesKey))
	{
		return SyncMsgCrypt_IllegalAesKey;
	}
	if (0 != AES_CBCEncrypt(sNeedEncrypt, sAesKey, &sAesData))
	{
		return SyncMsgCrypt_EncryptAES_Error;
	}

	//3. base64Encode
	std::string sBase64Data;
	if (0 != EncodeBase64(sAesData, sBase64Data))
	{
		return SyncMsgCrypt_EncodeBase64_Error;
	}

	//4. compute signature
	if (0 != ComputeSignature(m_sToken, sTimeStamp, sNonce, sBase64Data, sSignature))
	{
		return SyncMsgCrypt_ComputeSignature_Error;
	}

	sEncryptMsg = sBase64Data;

	return SyncMsgCrypt_OK;
}

int SyncMsgCrypt::AES_CBCEncrypt( const std::string & objSource,
        const std::string & objKey, std::string * poResult )
{
    return AES_CBCEncrypt( objSource.data(), objSource.size(),
            objKey.data(), objKey.size(), poResult );
}

int SyncMsgCrypt::AES_CBCEncrypt( const char * sSource, const uint32_t iSize,
        const char * sKey,  uint32_t iKeySize, std::string * poResult )
{
    if ( !sSource || !sKey || !poResult || iSize <= 0)
    {
        return -1;
    }
    
    poResult->clear();

    int padding = kAesKeySize - iSize % kAesKeySize;

    char * tmp = (char*)malloc( iSize + padding );
    if(NULL == tmp)
    {
        return -1;
    }
    memcpy( tmp, sSource, iSize );
    memset( tmp + iSize, padding, padding );
    
    unsigned char * out = (unsigned char*)malloc( iSize + padding );
    if(NULL == out)
    {
        FREE_PTR(tmp);
        return -1;
    }

    unsigned char key[ kAesKeySize ] = { 0 };
    unsigned char iv[ kAesIVSize ] = { 0 };
    memcpy( key, sKey, iKeySize > kAesKeySize ? kAesKeySize : iKeySize );
    memcpy(iv, key, sizeof(iv) < sizeof(key) ? sizeof(iv) : sizeof(key));

    AES_KEY aesKey;
    AES_set_encrypt_key( key, 8 * kAesKeySize, &aesKey );
    AES_cbc_encrypt((unsigned char *)tmp, out,iSize + padding,  &aesKey, iv, AES_ENCRYPT);
    poResult->append((char*)out, iSize + padding);
    
    FREE_PTR(tmp);
    FREE_PTR(out);
    return 0;
}

int SyncMsgCrypt::AES_CBCDecrypt( const std::string & objSource,
        const std::string & objKey, std::string * poResult )
{
    return AES_CBCDecrypt( objSource.data(), objSource.size(),
            objKey.data(), objKey.size(), poResult );
}

int SyncMsgCrypt::AES_CBCDecrypt( const char * sSource, const uint32_t iSize,
        const char * sKey, uint32_t iKeySize, std::string * poResult )
{
    if ( !sSource || !sKey || iSize < kAesKeySize || iSize % kAesKeySize != 0 || !poResult)
    {
        return -1;
    }
    
    poResult->clear();

    unsigned char * out = (unsigned char*)malloc( iSize );
    if(NULL == out)
    {
        return -1;
    }

    unsigned char key[ kAesKeySize ] = { 0 };
    unsigned char iv[ kAesIVSize ] = {0} ;
    memcpy( key, sKey, iKeySize > kAesKeySize ? kAesKeySize : iKeySize );
    memcpy(iv, key, sizeof(iv) < sizeof(key) ? sizeof(iv) : sizeof(key));

    int iReturnValue = 0;
    AES_KEY aesKey;
    AES_set_decrypt_key( key, 8 * kAesKeySize, &aesKey );
    AES_cbc_encrypt( (unsigned char *)sSource, out, iSize, &aesKey, iv ,AES_DECRYPT);
    if( out[iSize-1] > 0 && out[iSize-1] <= kAesKeySize && (iSize - out[iSize-1]) > 0 )
    {
        poResult->append( (char *)out , iSize - out[iSize-1] );
    } else {
        iReturnValue = -1;
    }

    FREE_PTR(out);
    return iReturnValue;
}

int SyncMsgCrypt::EncodeBase64(const std::string sSrc, std::string & sTarget)
{
    if(0 == sSrc.size() || kMaxBase64Size < sSrc.size())
    {
        return -1;
    }
    
    uint32_t iBlockNum = sSrc.size() / 3;
    if (iBlockNum * 3 != sSrc.size())
    {
        iBlockNum++;
    }
    uint32_t iOutBufSize = iBlockNum * 4 + 1;
    
    char * pcOutBuf = (char*)malloc( iOutBufSize);
    if(NULL == pcOutBuf)
    {
        return -1;
    }
    int iReturn = 0;
    int ret = EVP_EncodeBlock((unsigned char*)pcOutBuf, (const unsigned char*)sSrc.c_str(), sSrc.size());
    if (ret > 0 && ret < (int)iOutBufSize)
    {
        sTarget.assign(pcOutBuf,ret);
    }
    else
    {
        iReturn = -1;
    }
    
    FREE_PTR(pcOutBuf);
    return iReturn;
}

int SyncMsgCrypt::DecodeBase64(const std::string sSrc, std::string & sTarget)
{
    if(0 == sSrc.size() || kMaxBase64Size < sSrc.size())
    {
        return -1;
    }
    
    //¼ÆËãÄ©Î²=ºÅ¸öÊý
    int iEqualNum = 0;
    for(int n= sSrc.size() - 1; n>=0; --n)
    {
        if(sSrc.c_str()[n] == '=')
        {
            iEqualNum++;
        }
        else
        {
            break;
        }
    }
    
    int iOutBufSize = sSrc.size();
    char * pcOutBuf = (char*)malloc( iOutBufSize);
    if(NULL == pcOutBuf)
    {
        return -1;
    }
    
    int iRet = 0;
    int iTargetSize = 0;
    iTargetSize =  EVP_DecodeBlock((unsigned char*)pcOutBuf, (const unsigned char*)sSrc.c_str(), sSrc.size());
    if(iTargetSize > iEqualNum && iTargetSize < iOutBufSize)
    {
        sTarget.assign(pcOutBuf, iTargetSize - iEqualNum);
    }
    else
    {
        iRet = -1;
    }
    
    FREE_PTR(pcOutBuf);
    return iRet;
}

int SyncMsgCrypt::ComputeSignature(const std::string sToken, const std::string sTimeStamp, const std::string & sNonce,
    const std::string & sMessage, std::string & sSignature)
{
    if( 0 == sToken.size() || 0 == sNonce.size() || 0 == sMessage.size() || 0 == sTimeStamp.size())
    {
        return -1;
    }

    //sort
    std::vector< std::string >  vecStr;
    vecStr.push_back( sToken );
    vecStr.push_back( sTimeStamp );
    vecStr.push_back( sNonce );
    vecStr.push_back( sMessage );
    std::sort( vecStr.begin(), vecStr.end() );
    std::string sStr = vecStr[0] + vecStr[1] + vecStr[2] + vecStr[3] ;

    //compute
    unsigned char output[SHA_DIGEST_LENGTH] = { 0 };
    if( NULL == SHA1( (const unsigned char *)sStr.c_str(), sStr.size(), output ) )
    {
        return -1;
    }

    // to hex
    sSignature.clear();
    char tmpChar[ 8 ] = { 0 };
    for( int i = 0; i < SHA_DIGEST_LENGTH; i++ )
    {
#ifdef WIN32
        _snprintf_s( tmpChar, sizeof( tmpChar ), "%02x", 0xff & output[i] );
#else
		snprintf( tmpChar, sizeof( tmpChar ), "%02x", 0xff & output[i] );
#endif
        sSignature.append( tmpChar );
    }
    return 0;    
}


int SyncMsgCrypt::ValidateSignature(const std::string &sMsgSignature, const std::string &sTimeStamp, 
    const std::string &sNonce, const std::string & sEncryptMsg)
{
    std::string sSignature;
    if(0 != ComputeSignature(m_sToken, sTimeStamp, sNonce, sEncryptMsg, sSignature))
    {
        return -1;
    }
    
    if( sMsgSignature != sSignature)
    {
        return -1;
    }
    
    return 0;
}

int SyncMsgCrypt::GenAesKeyFromEncodingKey( const std::string & sEncodingKey, std::string & sAesKey)
{
    if(kEncodingKeySize != sEncodingKey.size())
    {
        return -1;
    }
    
    std::string sBase64 = sEncodingKey + "=";
    int ret = DecodeBase64(sBase64, sAesKey);
    if(0 != ret || kAesKeySize != sAesKey.size())
    {
        return -1;
    }
    
    return 0;
}

void SyncMsgCrypt::GenRandStr(std::string & sRandStr, uint32_t len)
{
    uint32_t idx = 0;
    srand((unsigned)time(NULL));
    char tempChar = 0;
    sRandStr.clear();
    
    while(idx < len)
    {
        tempChar = rand()%128;
        if(isprint(tempChar))
        {
            sRandStr.append(1, tempChar);
            ++idx;
        }
    }
}

void SyncMsgCrypt::GenNeedEncryptData(const std::string &sReplyMsg,std::string & sNeedEncrypt )
{
    //random(16B)+ msg_len(4B) + msg + $corpid
    std::string sRandStr;
    GenRandStr(sRandStr,kRandEncryptStrLen);
    uint32_t iXmlSize = sReplyMsg.size();
    uint32_t iNSize  = htonl(iXmlSize);
    std::string sSize ;
    sSize.assign((const char *)&iNSize,sizeof(iNSize));
    
    sNeedEncrypt.erase();
    sNeedEncrypt = sRandStr;
    sNeedEncrypt += sSize;
    sNeedEncrypt += sReplyMsg;
    sNeedEncrypt += m_sAppid;
}

}

