#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <jni.h>


extern "C" {

	JNIEXPORT void Java_app_ai_imgproc_Helpers_invert(JNIEnv*, jobject, jlong);
	JNIEXPORT void Java_app_ai_imgproc_Helpers_invert(JNIEnv*, jobject, jlong addrSRC) {

		cv::Mat img = *(cv::Mat*)addrSRC;

		for (int y = 0; y < img.rows;y++) {
			unsigned char *row = img.ptr<unsigned char>(y);
			for  (int x = 0; x < img.cols; x++)
				row[x]  = (255 - row[x]);
		}
	}
}
