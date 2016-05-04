#ifndef __SYNC_TYPES_H__
#define __SYNC_TYPES_H__
#include <list>
#include <map>
#include <string>
#include "webrtc/base/refcount.h"
#include "webrtc/base/scoped_ref_ptr.h"

typedef std::map < std::string, std::string > MapParams;
typedef struct SyncMessage_ : public rtc::RefCountInterface{
	int				cmd;
	int				seqn;
	bool			encrypt;
	MapParams		params;
	std::string		strContent;
	std::string		strPacked;
}SyncMessage;
typedef std::list<rtc::scoped_refptr<SyncMessage>> ListMessage;

typedef enum {
	SYC_Connect = 0,
	SYC_Sync,
	SYC_Message,
	SYC_Disconnect
} SYCMsgType;

#endif	// __SYNC_TYPES_H__