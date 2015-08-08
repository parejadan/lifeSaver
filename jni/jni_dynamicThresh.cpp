#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <jni.h>


extern "C" {

JNIEXPORT jdouble Java_app_ai_imgproc_Helpers_dynamicThresh(JNIEnv*, jobject, jlong);
    JNIEXPORT jdouble Java_app_ai_imgproc_Helpers_dynamicThresh(JNIEnv*, jobject, jlong addrSRC, jdouble stdDF) {
        double stdDevFactor = (double) stdDF;
        cv::Mat img = *(cv::Mat*)addrSRC;
        cv::Scalar stdMagnGrad, meanMagnGrad;
        cv::meanStdDEv(mat, meanMagnGrad, stdMagnGrad);
        double stdDev = stdMagnGrad[0] / sqrt(mat.rows*mat.cols);
        return (jdouble) stdDevFactor * stdDev + meanMagnGrad[0];
    }
}
