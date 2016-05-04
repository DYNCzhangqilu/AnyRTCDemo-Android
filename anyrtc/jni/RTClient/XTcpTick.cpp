#include "XTcpTick.h"
#include <map>

#ifdef WIN32
#include "webrtc/base/win32socketserver.h"
#else
#include <unistd.h>
#endif
#define EN_THREAD 0

namespace rtc {
class XTcpMgr
		: public Thread
	{
	public:
		XTcpMgr() 
			: m_bRunning(false) {
#if !EN_THREAD
			m_bRunning = true;
			this->Start();
#endif
		};
		virtual ~XTcpMgr(){
#if !EN_THREAD
			m_bRunning = false;
			this->Stop();
#endif
		};
        
        static XTcpMgr&Inst() {
            static rtc::XTcpMgr g_XTcpMgr;
            return g_XTcpMgr;
        }

	public:
		virtual void AddTick(XTcpTick*pTick){
			if (pTick == NULL)
				return;

			CritScope cs(&m_csTick);
			m_mapTick[pTick] = pTick;
#if EN_THREAD
			if (m_mapTick.size() == 1 && !m_bRunning)
			{
				m_bRunning = true;
				this->Start();
			}
#endif

		};

		virtual void RemoveTick(XTcpTick*pTick){
			if (pTick == NULL)
				return;

			int size = 0;
			{
				CritScope cs(&m_csTick);
				m_mapTick.erase(pTick);
				pTick->OnFinish();
				size = m_mapTick.size();
			}
#if EN_THREAD
			if (size == 0 && m_bRunning)
			{
				m_bRunning = false;
				this->Stop();
			}
#endif
		};

	public:
		//* For Thread
		virtual void Run(){
#if WIN32
			// Need to pump messages on our main thread on Windows.
			Win32Thread w32_thread;
#endif
			while (m_bRunning)
			{
				{// ProcessMessages
					this->ProcessMessages(10);
#if WIN32
					w32_thread.ProcessMessages(1);
#endif
				}
				
				{// OnTick
					CritScope cs(&m_csTick);
					MapTick::iterator iter = m_mapTick.begin();
					while (iter != m_mapTick.end())
					{
						iter->second->OnTick();
						iter++;
					}
				}
			}
		};

	private:
		bool				m_bRunning;
		CriticalSection		m_csTick;
		typedef std::map<void*, XTcpTick*> MapTick;
		MapTick				m_mapTick;
	};
};


XTcpTick::XTcpTick(void)
{

}
XTcpTick::~XTcpTick(void)
{

}

rtc::Thread* XTcpTick::CurThread()
{
	return &rtc::XTcpMgr::Inst();
}

int XTcpTick::Open(void)
{
	rtc::XTcpMgr::Inst().AddTick(this);
	return 0;
}

void XTcpTick::Close(void)
{
	rtc::XTcpMgr::Inst().RemoveTick(this);
}



