#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <jni.h>


extern "C" {


JNIEXPORT void Java_app_ai_imgproc_FindEyes_normalize(JNIEnv*, jobject, jdouble, jlong, jlong, jlong);
JNIEXPORT void Java_app_ai_imgproc_FindEyes_normalize(JNIEnv*, jobject, jdouble addrT, jlong addrX, jlong addrY, jlong addrM) {

	double gradientThresh = (double) addrT;
	cv::Mat gradX = *(cv::Mat*)addrX;
	cv::Mat gradY = *(cv::Mat*)addrY;
	cv::Mat mags = *(cv::Mat*)addrM;

	for (int y = 0; y < gradX.rows; y++) {
		double *Xr = gradX.ptr<double>(y), *Yr = gradY.ptr<double>(y);
		const double *Mr = mags.ptr<double>(y);

		for (int x = 0; x < gradX.cols; x++) {
			if (Mr[x] > gradientThresh) {
				Xr[x] = Xr[x]/Mr[x]; Yr[x] = Yr[x]/Mr[x];
			} else {
				Xr[x] = 0.0; Yr[x] = 0.0;
			}

		}
	}
}
}
