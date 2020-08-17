#include "avif/avif.h"

#include <stdio.h>
#include <string.h>

#include <stdlib.h>
static FILE * pFile;
static long lSize;
static uint8_t * buffer;
static size_t result;
static FILE * wFile;
static uint8_t * writeb;

int main(int argc, char * argv[]) {
	
    (void)argc;
    (void)argv;
	
	if(argc != 3) return -1;
	
	
    pFile = fopen(argv[1], "rb");
	
    if (!pFile){
     	printf("File not found\n");
		return 1;
	}
	fseek (pFile , 0 , SEEK_END);
	lSize = ftell (pFile);
	rewind (pFile);

	avifROData raw;

	buffer = (uint8_t*) malloc (sizeof(uint8_t)*lSize);
	if (buffer == NULL) {fputs ("Memory error",stderr); exit (2);}

	result = fread (buffer,1,lSize,pFile);
	
	raw.data = buffer;
	raw.size = lSize;	
    fclose(pFile);

	avifImage * image = avifImageCreateEmpty();
	avifDecoder * decoder = avifDecoderCreate();
	
	avifResult decodeResult = avifDecoderRead(decoder, image, &raw);
	if (decodeResult == AVIF_RESULT_OK) {
		avifRGBImage rgb;

		avifRGBImageSetDefaults(&rgb, image);
		rgb.format = AVIF_RGB_FORMAT_ARGB;                 
		rgb.depth = 8; 
		avifRGBImageAllocatePixels(&rgb);
		avifImageYUVToRGB(image, &rgb);
		writeb = rgb.pixels;
		wFile = fopen (argv[2], "wb+");
		fwrite(rgb.pixels, sizeof(uint8_t), 8*image->width * image->height, wFile);
		
		
	}
	printf("decodeResult:%s\n", avifResultToString(decodeResult));
	printf("imageSize:%dx%d\n", image->width, image->height);
		
	
    return 0;
}
