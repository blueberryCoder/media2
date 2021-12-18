#ifndef MP3_ENCODER
#define MP3_ENCODER

#include <stdio.h>
#include <lame/lame.h>

class Mp3Encoder
{
private:
    FILE* pcmFile;
    FILE* mp3File;
    lame_t lameClient;

public:
    Mp3Encoder(/* args */) {}
    virtual ~Mp3Encoder(){}
    int Init(const char* pcmFilePath,const char* mp3FilePath,int sampleRate,
    int channels,int bitRate);
    void Encode();
    void Destroy();
};

#endif /* MP3_ENCODER */