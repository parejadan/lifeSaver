#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <jni.h>


extern "C" {

	JNIEXPORT void Java_app_ai_imgproc_Helpers_nativeGaussBlur(JNIEnv*, jobject, jlong, jlong, jint, jint, jdouble, jdouble);
	JNIEXPORT void Java_app_ai_imgproc_Helpers_nativeGaussBlur(JNIEnv*, jobject, jlong addrSRC, jlong addrDST,
				jint blrX, jint blrY, jdouble sigX, jdouble sigY) {

		cv::Mat eyeROI = *(cv::Mat*)addrSRC;
		cv::Mat weight = *(cv::Mat*)addrDST;
		cv::Size ksize(blrX, blrY);
		double sigmaX = (double)sigX, sigmaY = (double)sigY;

		GaussianBlur(eyeROI, weight, ksize, sigmaX, sigmaY);
	}
}
