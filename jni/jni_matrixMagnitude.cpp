#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <jni.h>


extern "C" {


JNIEXPORT void Java_app_ai_imgproc_Helpers_matrixMagnitude(JNIEnv*, jobject, jlong, jlong, jlong);
JNIEXPORT void Java_app_ai_imgproc_Helpers_matrixMagnitude(JNIEnv*, jobject, jlong addrX, jlong addrY, jlong addrM) {

	cv::Mat gradX = *(cv::Mat*)addrX;
	cv::Mat gradY = *(cv::Mat*)addrY;
	cv::Mat mags = *(cv::Mat*)addrM;

	for (int y = 0; y < gradX.rows; y++) {
		const double *Xr = gradX.ptr<double>(y), *Yr = gradY.ptr<double>(y);
		double *Mr = mags.ptr<double>(y);

		for (int x = 0; x < gradX.cols; x++) Mr[x] = sqrt(Xr[x]*Xr[x] +  Yr[x]* Yr[x]);
	}
}
}
