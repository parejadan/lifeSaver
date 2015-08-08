#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <jni.h>


extern "C" {

	JNIEXPORT void Java_app_ai_imgproc_Helpers_circle(JNIEnv*, jobject, jlong, jdouble, jdouble, jint, jint);
	JNIEXPORT void Java_app_ai_imgproc_Helpers_circle(JNIEnv*, jobject, jlong addrSRC, jdouble x, jdouble y, jint size, jint color) {

		circle( *(cv::Mat*)addrSRC, cv::Point(x, y), (int)size, (int) color );
	}
}
