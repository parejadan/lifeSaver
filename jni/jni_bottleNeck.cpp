#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <jni.h>

void testPossibleCentersFormula(int x, int y, double gx, double gy, cv::Mat &out) {

	// Precompute values
	int xy_sum = x*x + y*y, y_double = 2*y, x_double = 2*x;
	double gxy = gx*x + gy*y, dot, top;

	for (int j = 0; j < out.rows; j++) {
		double *Or = out.ptr<double>(j);

		for (int k = 0; k < out.cols; k++) {
			//avoid a specific location
			if (j == y && k == x) continue;


			// see if the result will be negative
			top = gxy - gx*k - gy*j;
			if (top <= 0) dot = 0;
			else dot = top / sqrt(xy_sum + k*(k - x_double) + j*(j - y_double));

            Or[k] += dot*dot;
		}
	}
}

extern "C" {

	JNIEXPORT void Java_app_ai_imgproc_FindEyes_bottleNeck(JNIEnv*, jobject, jlong, jlong, jlong);
	JNIEXPORT void Java_app_ai_imgproc_FindEyes_bottleNeck(JNIEnv*, jobject, jlong addrX, jlong addrY, jlong addrO) {
		void testPossibleCentersFormula(int, int, double, double, cv::Mat&);

		cv::Mat gradX = *(cv::Mat*)addrX;
		cv::Mat gradY = *(cv::Mat*)addrY;
		cv::Mat out = *(cv::Mat*)addrO;

		for (int y = 0; y < out.rows; y++) {
			const double *Xr = gradX.ptr<double>(y), *Yr = gradY.ptr<double>(y);
			for (int x = 0; x < out.cols; x++) {
				if (Xr[x] == 0.0 && Yr[x] == 0.0) continue;

				testPossibleCentersFormula(x, y, Xr[x], Yr[x], out);
			}
		}
	}

}
