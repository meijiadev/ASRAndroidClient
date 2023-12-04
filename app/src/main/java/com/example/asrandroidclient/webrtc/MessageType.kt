package com.example.asrandroidclient.webrtc

/**
 * Create by MJ on 2023/12/4.
 * Describe :
 */

enum class MessageType {
    KeywordAdd,            // 对象 Data
    KeywordEdit,          // 对象  Data
    KeywordDel,          // String (下发一个keywordId)
    KeywordList,         // list<Data>

    VoiceAdd,
    VoiceEdit,
    VoiceDel,
    VoiceList,
}